/*
 * Created on 31-Jul-2004
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.core3.disk.impl.access.impl;

import java.util.LinkedList;
import java.util.List;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.disk.impl.DiskManagerFileInfoImpl;
import org.gudy.azureus2.core3.disk.impl.DiskManagerHelper;
import org.gudy.azureus2.core3.disk.impl.PieceList;
import org.gudy.azureus2.core3.disk.impl.PieceMapEntry;
import org.gudy.azureus2.core3.disk.impl.access.*;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.util.*;


import com.aelitis.azureus.core.diskmanager.cache.*;

/**
 * @author parg
 *
 */

public class 
DMWriterAndCheckerImpl 
	implements DMWriterAndChecker
{
	protected static final boolean	CONCURRENT_CHECKING	= true;
	
	protected static final int	DEFAULT_WRITE_QUEUE_MAX	= 256;
	protected static final int	DEFAULT_CHECK_QUEUE_MAX	= 128;
	
	protected static final int	QUEUE_REPORT_CHUNK		= 32;
	
		// global limit on size of write queue
	
	private static int			global_write_queue_block_sem_size;
	private static AESemaphore	global_write_queue_block_sem;
	private static int			global_write_queue_block_sem_next_report_size;
	
		// global limit on size of check queue
	
	private static int			global_check_queue_block_sem_size;
	private static AESemaphore	global_check_queue_block_sem;
	private static int			global_check_queue_block_sem_next_report_size;
  
  private static boolean friendly_hashing = COConfigurationManager.getBooleanParameter( "diskmanager.friendly.hashchecking" );
  
	static{
		int	write_limit_blocks = COConfigurationManager.getIntParameter("DiskManager Write Queue Block Limit", 0);

		global_write_queue_block_sem_size	= write_limit_blocks==0?DEFAULT_WRITE_QUEUE_MAX:write_limit_blocks;
		
		global_write_queue_block_sem_next_report_size	= global_write_queue_block_sem_size - QUEUE_REPORT_CHUNK;
		
		global_write_queue_block_sem = new AESemaphore("DMW&C::writeQ", global_write_queue_block_sem_size);
		
		if ( global_write_queue_block_sem_size == 0 ){
			
			global_write_queue_block_sem.releaseForever();
		}
		
		int	check_limit_pieces = COConfigurationManager.getIntParameter("DiskManager Check Queue Piece Limit", 0);

		global_check_queue_block_sem_size	= check_limit_pieces==0?DEFAULT_CHECK_QUEUE_MAX:check_limit_pieces;
		
		global_check_queue_block_sem_next_report_size	= global_check_queue_block_sem_size - QUEUE_REPORT_CHUNK;
		
		global_check_queue_block_sem = new AESemaphore("DMW&C::checkQ", global_check_queue_block_sem_size);
		
		if ( global_check_queue_block_sem_size == 0 ){
			
			global_check_queue_block_sem.releaseForever();
		}
	}
  
  private static final ParameterListener param_listener = new ParameterListener() {
    public void parameterChanged( String  str ) {
      friendly_hashing = COConfigurationManager.getBooleanParameter( "diskmanager.friendly.hashchecking" );        
    }
  };
  

	private DiskManagerHelper		disk_manager;
	
	private DiskWriteThread writeThread;
	private List 			writeQueue			= new LinkedList();
	private List 			checkQueue			= new LinkedList();
	private AESemaphore		writeCheckQueueSem	= new AESemaphore("writeCheckQ");				

	protected volatile 		ConcurrentHasherRequest	current_hash_request;
	
	protected long			total_async_check_requests;
	protected AESemaphore	async_check_request_sem 	= new AESemaphore("DMW&C::asyncReq");
	
	protected boolean	started;
	protected boolean	bOverallContinue		= true;
	
	protected int		pieceLength;
	protected int		lastPieceLength;
	protected long		totalLength;
	
	protected int		nbPieces;
	
	protected boolean	complete_recheck_in_progress;
	
	protected AEMonitor		this_mon	= new AEMonitor( "DMW&C" );
	
	protected AESemaphore	stop_sem	= new AESemaphore( "DMW&C::stop");
	
	public
	DMWriterAndCheckerImpl(
		DiskManagerHelper	_disk_manager )
	{
		disk_manager	= _disk_manager;
		
		pieceLength		= disk_manager.getPieceLength();
		lastPieceLength	= disk_manager.getLastPieceLength();
		totalLength		= disk_manager.getTotalLength();
		
		nbPieces		= disk_manager.getNumberOfPieces();
		
		// System.out.println( "DMW&C: write sem = " + global_write_queue_block_sem.getValue() + ", check = " + global_check_queue_block_sem.getValue());
	}
	
	public void
	start()
	{
		try{
			this_mon.enter();

			if ( started ){
				
				throw( new RuntimeException( "DMW&C: start while started"));
			}
			
			if ( !bOverallContinue ){
				
				throw( new RuntimeException( "DMW&C: start after stopped"));
			}

			started	= true;
      
      
      COConfigurationManager.addParameterListener( "diskmanager.friendly.hashchecking", param_listener );
			
			writeThread = new DiskWriteThread();
			
			writeThread.start();
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	stop()
	{
		try{
			this_mon.enter();

			if ( !started ){
			
				return;
			}
		
				// when we exit here we guarantee that all file usage operations have completed
				// i.e. writes and checks (checks being doubly async)
			
			bOverallContinue	= false;
      
      COConfigurationManager.removeParameterListener( "diskmanager.friendly.hashchecking", param_listener );
      
			if ( current_hash_request != null ){
				
				current_hash_request.cancel();
			}
		}finally{
			
			this_mon.exit();
		}
					
		writeThread.stopIt();
		
			// wait until the thread has stopped
		
		stop_sem.reserve();
		
			// now wait until any outstanding piece checks have completed
		
		for (int i=0;i<total_async_check_requests;i++){
			
			async_check_request_sem.reserve();
		}
	}
	
	public boolean 
	isChecking() 
	{
	   return( complete_recheck_in_progress );
	}

	public boolean 
	zeroFile( 
		DiskManagerFileInfoImpl file, 
		long 					length ) 
	{
		CacheFile	cache_file = file.getCacheFile();
		
		long written = 0;
		
		try{
			if( length == 0 ){ //create a zero-length file if it is listed in the torrent
				
				cache_file.setLength( 0 );
				
			}else{
					
		        DirectByteBuffer	buffer = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_DM_ZERO,pieceLength);
		    
		        try{
			        buffer.limit(DirectByteBuffer.SS_DW, pieceLength);
			        
					for (int i = 0; i < buffer.limit(DirectByteBuffer.SS_DW); i++){
						
						buffer.put(DirectByteBuffer.SS_DW, (byte)0);
					}
					
					buffer.position(DirectByteBuffer.SS_DW, 0);

					while (written < length && bOverallContinue){
						
						int	write_size = buffer.capacity(DirectByteBuffer.SS_DW);
						
						if ((length - written) < write_size ){
            	
							write_size = (int)(length - written);
						}
            
						buffer.limit(DirectByteBuffer.SS_DW, write_size);
             
						cache_file.write( buffer, written );
            
						buffer.position(DirectByteBuffer.SS_DW, 0);
            
						written += write_size;
            
						disk_manager.setAllocated( disk_manager.getAllocated() + write_size );
            
						disk_manager.setPercentDone((int) ((disk_manager.getAllocated() * 1000) / totalLength));
					}
		        }finally{
		        	
		        	buffer.returnToPool();
		        }
				
			}
			
			if (!bOverallContinue){
						
				cache_file.close();
				   
				return false;
			}
		}catch (Exception e){ 
			
			Debug.printStackTrace( e );
      
      try{
        cache_file.close();
      }
      catch( Throwable t ) { /*ignore*/ }
			
			return( false );
		}
			
		return true;
	}
	  
	public void 
	enqueueCompleteRecheckRequest(
		final DiskManagerCheckRequestListener 	listener,
		final Object							user_data ) 
	{  	
	 	Thread t = new AEThread("PEPeerControl:checker")
		{
	  		public void
			runSupport()
	  		{
	  			try{
	  				complete_recheck_in_progress	= true;
	  					
	  				final AESemaphore	sem = new AESemaphore( "PEPeerControl:checker" );
	  				
	  				int	checks_submitted	= 0;
	  				
            int delay = 0;  //if friendly hashing is enabled, no need to delay even more here

            if( !friendly_hashing ) {
              //delay a bit normally anyway, as we don't want to kill the user's system
              //during the post-completion check (10k of piece = 1ms of sleep)
              delay = pieceLength /1024 /10;
              delay = Math.min( delay, 409 );
              delay = Math.max( delay, 12 );
            }
            
            
	  				for ( int i=0; i < nbPieces; i++ ){
	        
	  					if ( !bOverallContinue ){
	  						
	  						break;
	  					}
	  					
	  					enqueueCheckRequest( 
	  	       				i, 
	  	       				new DiskManagerCheckRequestListener()
							{
		  	       				public void
								pieceChecked( 
									int 		_pieceNumber, 
									boolean 	_result,
									Object		_user_data )
		  	       				{
		  	       					try{
		  	       						listener.pieceChecked( _pieceNumber, _result, _user_data );
		  	       						
		  	       					}finally{
		  	       						
		  	       						sem.release();
		  	       					}
		  	       				}
							},
							user_data );
	  					
	  					checks_submitted++;
	  					
	  					if( delay > 0 )  Thread.sleep( delay );
	  				}
	  				
	  				if ( bOverallContinue ){
	  					
	  						// wait for all to complete
	  					
	  					for (int i=0;i<checks_submitted;i++){
	  						
	  						sem.reserve();
	  					}
	  				}
	  	       }catch( Throwable e ){
	  	       	
	  	       			// we get here if the disk manager's stopped running
	  	       	
	  	       		Ignore.ignore(e);
	  	       }finally{
	  	       	
	  	       		complete_recheck_in_progress	= false;
	  	       }
	        }     			
	 	};
	
	 	t.setDaemon(true);
	 	
	 	t.start();
	}
	
	public void 
	enqueueCheckRequest(
		int 							pieceNumber,
		DiskManagerCheckRequestListener listener,
		Object							user_data ) 
	{  	
		global_check_queue_block_sem.reserve();	   	
		
		if ( global_check_queue_block_sem.getValue() < global_check_queue_block_sem_next_report_size ){
			
	    	// Debug.out( "Disk Manager check queue size exceeds " + ( global_check_queue_block_sem_size - global_check_queue_block_sem_next_report_size ));

			global_check_queue_block_sem_next_report_size -= QUEUE_REPORT_CHUNK;
		}
		
		// System.out.println( "check queue size = " + ( global_check_queue_block_sem_size - global_check_queue_block_sem.getValue()));
		
	  	try{
	  		this_mon.enter();
	 		
	  		if ( !bOverallContinue ){
	  			
	  			global_check_queue_block_sem.release();
	  			
	  			throw( new RuntimeException( "WriteChecker stopped" ));
	  		}
	  		
	   		checkQueue.add(new QueueElement(pieceNumber, 0, null, user_data, listener ));
	   		
	    }finally{
	    	
	    	this_mon.exit();
	    }
	   		
	   	writeCheckQueueSem.release();
	}  
	  
	public void
	checkPiece(
		final int 						pieceNumber,
		final CheckPieceResultHandler	_result_handler,
		final Object					user_data )
	{
		final int this_piece_length = pieceNumber < nbPieces - 1 ? pieceLength : lastPieceLength;

		final CheckPieceResultHandler	result_handler =
			new CheckPieceResultHandler()
			{
				public void
				processResult(
					int		piece_number,
					int		result,
					Object	_user_data )
				{
					try{						

						disk_manager.getPieces()[piece_number].setDone( result == CheckPieceResultHandler.OP_SUCCESS );
						
					}finally{
												
						if ( _result_handler != null ){
							
							_result_handler.processResult( pieceNumber, result, _user_data );
						}
					}
				}
			};
			
			
		int		check_result	= CheckPieceResultHandler.OP_CANCELLED;
		
		DirectByteBuffer	buffer	= null;
              
		boolean	async_request	= false;
        
		try{
			
	        buffer = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_DM_CHECK,this_piece_length);
	    
		    if ( !bOverallContinue ){
		    	
		    	return;
		    }
	
				//get the piece list
			
			PieceList pieceList = disk_manager.getPieceList(pieceNumber);

			//for each piece-part-to-file mapping entry
			for (int i = 0; i < pieceList.size(); i++) {
				
				PieceMapEntry tempPiece = pieceList.get(i);
	            
				try {
	                    
						//if the file is large enough
					
					if ( tempPiece.getFile().getCacheFile().getLength() >= tempPiece.getOffset()){
						
			            //Make sure we only read in this entry-length's worth of data from the file
			            //NOTE: Without this limit the read op will
			            // a) fill the entire buffer with file data if the file length is big enough,
			            //    i.e. the whole piece is contained within the file somewhere, or
			            // b) read the file into the buffer until it reaches EOF,
			            //    i.e. the piece overlaps two different files
			            //Under normal conditions this works ok, because the assumption is that if a piece
			            //is contained within a single file, then there will only be one PieceMapEntry for
			            //that piece, so we can just fill the buffer.  It also assumes that if a piece
			            //overlaps two (or more) files, then there will be multiple PieceMapEntrys, with
			            //each entry ending at the file EOF boundary.  However, if for some reason one of
			            //these files is at least one byte too large, then the read op will read in too
			            //many bytes before hitting EOF, and our piece buffer data will be misaligned,
			            //causing hash failure (and a 99.9% bug).  Better to set the buffer limit explicitly.
			            
			            int entry_read_limit = buffer.position( DirectByteBuffer.SS_DW ) + tempPiece.getLength();
			            
			            buffer.limit( DirectByteBuffer.SS_DW, entry_read_limit );
			
			            tempPiece.getFile().getCacheFile().read(buffer, tempPiece.getOffset());  //do read
			            
			            buffer.limit( DirectByteBuffer.SS_DW, this_piece_length );  //restore limit
						
					}else{
						// file is too small, therefore required data hasn't been 
						// written yet -> check fails
								
						return;
					}
				}catch (Exception e){
					
					Debug.printStackTrace( e );
					
					return;
				}
			}

			try {
	      
				if ( !bOverallContinue ){
											
					return;
				}
	      
	      		buffer.position(DirectByteBuffer.SS_DW, 0);

    		    if ( CONCURRENT_CHECKING ){

    		    	async_request	= true;
    		    	
    		    	total_async_check_requests++;
    		    	
    		    	final	DirectByteBuffer	f_buffer	= buffer;
    		    	
	    		    current_hash_request = 
	    		    	ConcurrentHasher.getSingleton().addRequest(
	    		    			buffer.getBuffer(DirectByteBuffer.SS_DW),
								new ConcurrentHasherRequestListener()
								{
	    		    				public void
									complete(
										ConcurrentHasherRequest	request )
	    		    				{
	    		    					int	async_result	= CheckPieceResultHandler.OP_CANCELLED;
	    		    						    		    					
	    		    					try{
	    		    						
		    								byte[] testHash = request.getResult();
		    										    								
		    								if ( testHash != null ){
		    											
			    								byte[]	required_hash = disk_manager.getPieceHash(pieceNumber);
			    								
			    								async_result = CheckPieceResultHandler.OP_SUCCESS;
			    								
			    								for (int i = 0; i < testHash.length; i++){
			    									
			    									if ( testHash[i] != required_hash[i]){
			    										
			    										async_result = CheckPieceResultHandler.OP_FAILURE;
			    										
			    										break;
			    									}
			    								}
		    								}
	    		    					}finally{
	    		    						
	    		    						try{
		    		    						f_buffer.returnToPool();
	
		    		    						result_handler.processResult( 
		    		    								pieceNumber, 
														async_result,
														user_data );
		    		    						
	    		    						}finally{
	    		    							
	    		    							async_check_request_sem.release();
	    		    						}
	    		    					}
	    		    				}
	    		    				
								});
	
    		    }else{

					byte[] testHash = new SHA1Hasher().calculateHash( buffer.getBuffer(DirectByteBuffer.SS_DW ));
										
					byte[]	required_hash = disk_manager.getPieceHash(pieceNumber);
										
					check_result	= CheckPieceResultHandler.OP_SUCCESS;
					
					for (int i = 0; i < testHash.length; i++){
						
						if ( testHash[i] != required_hash[i]){
							
							check_result	= CheckPieceResultHandler.OP_FAILURE;
							
							break;
						}
					}
    		    }
														
			}catch( Throwable  e){
				
				Debug.printStackTrace( e );
				
			}
		}finally{				
				
			if ( !async_request ){
				
				if ( buffer != null ){
					
					buffer.returnToPool();
				}

				result_handler.processResult( pieceNumber, check_result, user_data );
			}
		}
	}
	 
	/**
	 * @param e
	 * @return FALSE if the write failed for some reason. Error will have been reported
	 * and queue element set back to initial state to allow a re-write attempt later
	 */
	
	private boolean 
	dumpBlockToDisk(
		QueueElement queue_entry ) 
	{
		int pieceNumber 	= queue_entry.getPieceNumber();
		int offset		 	= queue_entry.getOffset();
		DirectByteBuffer buffer 	= queue_entry.getData();
		int	initial_buffer_position = buffer.position(DirectByteBuffer.SS_DW);

		PieceMapEntry current_piece = null;
		
		try{
			int previousFilesLength = 0;
			int currentFile = 0;
			PieceList pieceList = disk_manager.getPieceList(pieceNumber);
			current_piece = pieceList.get(currentFile);
			long fileOffset = current_piece.getOffset();
			while ((previousFilesLength + current_piece.getLength()) < offset) {
				previousFilesLength += current_piece.getLength();
				currentFile++;
				fileOffset = 0;
				current_piece = pieceList.get(currentFile);
			}
	
			boolean	buffer_handed_over	= false;
			
			//Now tempPiece points to the first file that contains data for this block
			while (buffer.hasRemaining(DirectByteBuffer.SS_DW)) {
				current_piece = pieceList.get(currentFile);
	
				if (current_piece.getFile().getAccessMode() == DiskManagerFileInfo.READ){
		
					LGLogger.log(0, 0, LGLogger.INFORMATION, "Changing " + current_piece.getFile().getName() + " to read/write");
						
					current_piece.getFile().setAccessMode( DiskManagerFileInfo.WRITE );
				}
				
				int realLimit = buffer.limit(DirectByteBuffer.SS_DW);
					
				long limit = buffer.position(DirectByteBuffer.SS_DW) + ((current_piece.getFile().getLength() - current_piece.getOffset()) - (offset - previousFilesLength));
	       
				if (limit < realLimit){
					
					buffer.limit(DirectByteBuffer.SS_DW, (int)limit);
				}
	
					// surely we always have remaining here?
				
				if ( buffer.hasRemaining(DirectByteBuffer.SS_DW) ){

					long	pos = fileOffset + (offset - previousFilesLength);
					
					if ( limit < realLimit ){
						
						current_piece.getFile().getCacheFile().write( buffer, pos );
						
					}else{
						
						current_piece.getFile().getCacheFile().writeAndHandoverBuffer( buffer, pos );
						
						buffer_handed_over	= true;
						
						break;
					}
				}
					
				buffer.limit(DirectByteBuffer.SS_DW, realLimit);
				
				currentFile++;
				fileOffset = 0;
				previousFilesLength = offset;
			}
			
			if ( !buffer_handed_over ){
			
					// the last write for a block should always be handed over, hence we
					// shouln't get here....
				
				Debug.out( "buffer not handed over to file cache!" );
				
				buffer.returnToPool();
			}
			
			return( true );
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
			
			String file_name = current_piece==null?"<unknown>":current_piece.getFile().getName();
						
			disk_manager.setErrorMessage( Debug.getNestedExceptionMessage(e) + " when processing file '" + file_name + "'" );
			
			LGLogger.logAlert( LGLogger.AT_ERROR, disk_manager.getErrorMessage() );
			
			buffer.position(DirectByteBuffer.SS_DW, initial_buffer_position);
			
			return( false );
		}
	}
  
	
	public void 
	writeBlock(
		int 							pieceNumber, 
		int 							offset, 
		DirectByteBuffer 				data,
		Object 							user_data,
		DiskManagerWriteRequestListener	listener ) 
	{		
		global_write_queue_block_sem.reserve();		
		
		if ( global_write_queue_block_sem.getValue() < global_write_queue_block_sem_next_report_size ){
			
			LGLogger.log( "Disk Manager write queue size exceeds " + ( global_write_queue_block_sem_size - global_write_queue_block_sem_next_report_size ));

			global_write_queue_block_sem_next_report_size -= QUEUE_REPORT_CHUNK;
		}
		
		// System.out.println( "write queue size = " + ( global_write_queue_block_sem_size - global_write_queue_block_sem.getValue()));

		// System.out.println( "reserved global write slot (buffer = " + data.limit() + ")" );
		
		try{
			this_mon.enter();
			
	  		if ( !bOverallContinue ){
	  			
	  			global_check_queue_block_sem.release();
	  			
	  			throw( new RuntimeException( "WriteChecker stopped" ));
	  		}
			
			writeQueue.add(new QueueElement(pieceNumber, offset, data, user_data, listener ));
		}finally{
			
			this_mon.exit();
		}
		
		writeCheckQueueSem.release();
	}

  
	public boolean checkBlock(int pieceNumber, int offset, DirectByteBuffer data) {
		if (pieceNumber < 0) {
      LGLogger.log(0, 0, LGLogger.ERROR, "CHECKBLOCK1: pieceNumber="+pieceNumber+" < 0");
			return false;
    }
		if (pieceNumber >= this.nbPieces) {
      LGLogger.log(0, 0, LGLogger.ERROR, "CHECKBLOCK1: pieceNumber="+pieceNumber+" >= this.nbPieces="+this.nbPieces);
			return false;
    }
		int length = this.pieceLength;
		if (pieceNumber == nbPieces - 1) {
			length = this.lastPieceLength;
    }
		if (offset < 0) {
      LGLogger.log(0, 0, LGLogger.ERROR, "CHECKBLOCK1: offset="+offset+" < 0");
			return false;
    }
		if (offset > length) {
      LGLogger.log(0, 0, LGLogger.ERROR, "CHECKBLOCK1: offset="+offset+" > length="+length);
			return false;
    }
		int size = data.remaining(DirectByteBuffer.SS_DW);
		if (offset + size > length) {
      LGLogger.log(0, 0, LGLogger.ERROR, "CHECKBLOCK1: offset="+offset+" + size="+size+" > length="+length);
			return false;
    }
		return true;
	}
  

	public boolean checkBlock(int pieceNumber, int offset, int length) {
		if (length > 65536) {
		  LGLogger.log(0, 0, LGLogger.ERROR, "CHECKBLOCK2: length="+length+" > 65536");
		  return false;
		}
		if (pieceNumber < 0) {
		  LGLogger.log(0, 0, LGLogger.ERROR, "CHECKBLOCK2: pieceNumber="+pieceNumber+" < 0");
		  return false;
      }
		if (pieceNumber >= this.nbPieces) {
		  LGLogger.log(0, 0, LGLogger.ERROR, "CHECKBLOCK2: pieceNumber="+pieceNumber+" >= this.nbPieces="+this.nbPieces);
		  return false;
      }
		int pLength = this.pieceLength;
		if (pieceNumber == this.nbPieces - 1)
			pLength = this.lastPieceLength;
		if (offset < 0) {
		  LGLogger.log(0, 0, LGLogger.ERROR, "CHECKBLOCK2: offset="+offset+" < 0");
		  return false;
		}
		if (offset > pLength) {
		  LGLogger.log(0, 0, LGLogger.ERROR, "CHECKBLOCK2: offset="+offset+" > pLength="+pLength);
		  return false;
		}
		if (offset + length > pLength) {
		  LGLogger.log(0, 0, LGLogger.ERROR, "CHECKBLOCK2: offset="+offset+" + length="+length+" > pLength="+pLength);
		  return false;
		}
		if(!disk_manager.getPieces()[pieceNumber].getDone()) {
		  LGLogger.log(0, 0, LGLogger.ERROR, "CHECKBLOCK2: pieceNumber="+pieceNumber+" not done");
		  return false;
		}
		return true;
	}



	public class 
	DiskWriteThread 
		extends AEThread 
	{
		private boolean bWriteContinue = true;

		public DiskWriteThread() 
		{
			super("Disk Writer & Checker");
			
			setDaemon(true);
		}

		public void 
		runSupport() 
		{
			try{
				while (bWriteContinue){
					
					try{
						int	entry_count = writeCheckQueueSem.reserveSet( 64 );
											
						for (int i=0;i<entry_count;i++){
							
							final QueueElement	elt;
							
							boolean			elt_is_write;
							
							try{
								this_mon.enter();
								
								if ( !bWriteContinue){
																
									break;
								}
								
								if ( writeQueue.size() > checkQueue.size()){
									
									elt	= (QueueElement)writeQueue.remove(0);
									
									// System.out.println( "releasing global write slot" );
		
									global_write_queue_block_sem.release();
									
									elt_is_write	= true;
									
								}else{
									
									elt	= (QueueElement)checkQueue.remove(0);
									
									global_check_queue_block_sem.release();
															
									elt_is_write	= false;
								}
							}finally{
								
								this_mon.exit();
							}
			
							if ( elt_is_write ){
								
									//Do not allow to write in a piece marked as done.
								
								int pieceNumber = elt.getPieceNumber();
								
								if(!disk_manager.getPieces()[pieceNumber].getDone()){
									
								  if ( dumpBlockToDisk(elt)){
								  
								  	DiskManagerWriteRequestListener	listener = (DiskManagerWriteRequestListener)elt.getListener();
								  	
								  	if ( listener != null ){
								  	
								  		listener.blockWritten( elt.getPieceNumber(), elt.getOffset(),elt.getUserData());
								  	}
								  }else{
								  	
								  		// could try and recover if, say, disk full. however, not really
								  		// worth the effort as user intervention is no doubt required to
								  		// fix the problem 
								  	
									elt.data.returnToPool();
											
									elt.data = null;
									  
									stopIt();
									
									disk_manager.setState( DiskManager.FAULTY );
									
								  }
								  
								}else{
			  
									elt.data.returnToPool();
									
								  elt.data = null;
								}
								
							}else{
								
							  checkPiece( 
							  		elt.getPieceNumber(),
									new CheckPieceResultHandler()
									{
							  			public void
										processResult(
											int			pieceNumber,
											int			result,
											Object		user_data )
							  			{
							  				if ( result == CheckPieceResultHandler.OP_SUCCESS ){
										  								  	
							  					LGLogger.log(0, 0, LGLogger.INFORMATION, "Piece " + pieceNumber + " passed hash check.");
										   
							  				}else if ( result == CheckPieceResultHandler.OP_FAILURE ){
	
								  				LGLogger.log(0, 0, LGLogger.ERROR, "Piece " + pieceNumber + " failed hash check.");
	
							  				}else{
							  					
								  				LGLogger.log(0, 0, LGLogger.ERROR, "Piece " + pieceNumber + " hash check cancelled.");
							  					
							  				}
			
										  	DiskManagerCheckRequestListener	listener = (DiskManagerCheckRequestListener)elt.getListener();
										  	
										  	if ( listener != null ){
	
										  		listener.pieceChecked(pieceNumber, result == CheckPieceResultHandler.OP_SUCCESS, user_data );
										  	}
							  			}
									},
									elt.getUserData());
						  }
						}
					}catch( Throwable e ){
						
						Debug.printStackTrace( e );
						
						Debug.out( "DiskWriteThread: error occurred during processing: " + e.toString());
					}
				}
			}finally{
					
				stop_sem.releaseForever();
			}
		}

		public void stopIt(){
			
			try{
				this_mon.enter();
				
				bWriteContinue = false;
			}finally{
				
				this_mon.exit();
			}
			
			writeCheckQueueSem.releaseForever();
			
			while (writeQueue.size() != 0){
				
				// System.out.println( "releasing global write slot (tidy up)" );

				global_write_queue_block_sem.release();
				
				QueueElement elt = (QueueElement)writeQueue.remove(0);
				
				elt.data.returnToPool();
				
				elt.data = null;
			}
			
			while (checkQueue.size() != 0){
				
				// System.out.println( "releasing global write slot (tidy up)" );

				global_check_queue_block_sem.release();
				
				checkQueue.remove(0);
			}
		}
	}
	
	public class 
	QueueElement 
	{
		private int 				pieceNumber;
		private int 				offset;
		private DirectByteBuffer 	data;
		private Object 				user_data; 
		private	Object				listener;

		public 
		QueueElement(
			int 				_pieceNumber, 
			int 				_offset, 
			DirectByteBuffer	_data, 
			Object 				_user_data,
			Object				_listener ) 
		{
			pieceNumber 	= _pieceNumber;
			offset 			= _offset;
			data 			= _data;
			user_data 		= _user_data;
			listener		= _listener;
		}  

		public int 
		getPieceNumber() 
		{
			return pieceNumber;
		}

		public int 
		getOffset() 
		{
			return offset;
		}

		public DirectByteBuffer 
		getData() 
		{
			return data;
		}
    
	    public Object 
		getUserData() 
	    {
	      return( user_data );
		}
	    
	    public Object
		getListener()
	    {
	    	return( listener );
	    }
	}
}
