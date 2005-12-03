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
import org.gudy.azureus2.core3.logging.*;
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
	private static final LogIDs LOGID = LogIDs.DISK;
  
	private static final long WRITE_THREAD_IDLE_LIMIT	= 60*1000;
		
	protected static final int	DEFAULT_WRITE_QUEUE_MAX	= 256;
	protected static final int	DEFAULT_CHECK_QUEUE_MAX	= 128;
	
	protected static final int	QUEUE_REPORT_CHUNK		= 32;
	
		// global limit on size of write queue
	
	private static int			global_write_queue_block_sem_size;
	private static AESemaphore	global_write_queue_block_sem;
	private static int			global_write_queue_block_sem_next_report_size;
	
		// global limit on size of check queue
	
	static{
		int	write_limit_blocks = COConfigurationManager.getIntParameter("DiskManager Write Queue Block Limit", 0);

		global_write_queue_block_sem_size	= write_limit_blocks==0?DEFAULT_WRITE_QUEUE_MAX:write_limit_blocks;
		
		global_write_queue_block_sem_next_report_size	= global_write_queue_block_sem_size - QUEUE_REPORT_CHUNK;
		
		global_write_queue_block_sem = new AESemaphore("DMW&C::writeQ", global_write_queue_block_sem_size);
		
		if ( global_write_queue_block_sem_size == 0 ){
			
			global_write_queue_block_sem.releaseForever();
		}
	}
  
	private static boolean 	friendly_hashing;
	private static int		max_read_block_size;

    static{
    	
    	 ParameterListener param_listener = new ParameterListener() {
    	    public void 
			parameterChanged( 
				String  str ) 
    	    {
    	      friendly_hashing = COConfigurationManager.getBooleanParameter( "diskmanager.friendly.hashchecking" );
    	      
    	  	  max_read_block_size	= COConfigurationManager.getIntParameter( "BT Request Max Block Size" );

    	    }
    	 };

 		COConfigurationManager.addParameterListener( "diskmanager.friendly.hashchecking", param_listener );
		COConfigurationManager.addParameterListener( "BT Request Max Block Size", param_listener );
		
		param_listener.parameterChanged("");	// pick up initial values
    }

  

	private DiskManagerHelper		disk_manager;
	
	private DiskWriteThread writeThread;
	private List 			writeQueue			= new LinkedList();
	private AESemaphore		writeCheckQueueSem	= new AESemaphore("writeCheckQ");				
	
	private int				async_checks;
	private AESemaphore		async_check_sem 	= new AESemaphore("DMW&C::asyncCheck");
	
	private int				async_reads;
	private AESemaphore		async_read_sem 		= new AESemaphore("DMW&C::asyncRead");

	private boolean	started;
	
	private volatile boolean	stopped;
	
	private int			pieceLength;
	private int			lastPieceLength;
	private long		totalLength;
	
	private int		nbPieces;
	
	private boolean	complete_recheck_in_progress;
	
	private AEMonitor		this_mon	= new AEMonitor( "DMW&C" );
		
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
			
			if ( stopped ){
				
				throw( new RuntimeException( "DMW&C: start after stopped"));
			}

			started	= true;
 			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	stop()
	{
		DiskWriteThread	current_write_thread;
		
		int	check_wait;
		int	read_wait;
		
		try{
			this_mon.enter();

			if ( stopped || !started ){
			
				return;
			}
					
				// when we exit here we guarantee that all file usage operations have completed
				// i.e. writes and checks (checks being doubly async)
			
			stopped	= true;
         
			current_write_thread	= writeThread;
			
			read_wait	= async_reads;
			check_wait	= async_checks;
			
		}finally{
			
			this_mon.exit();
		}
			
		if ( current_write_thread != null ){
			
			current_write_thread.stopWriteThread();
		}
				
	
			// wait for reads
		
		for (int i=0;i<read_wait;i++){
			
			async_read_sem.reserve();
		}
		
			// wait for checks
		
		for (int i=0;i<check_wait;i++){
			
			async_check_sem.reserve();
		}
		
			// TODO wait for writes
		
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

					while (written < length && !stopped ){
						
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
			
			if ( stopped ){
										   
				return false;
			}
		}catch (Exception e){ 
			
			Debug.printStackTrace( e );
			
			return( false );
		}
			
		return true;
	}
	  
	public void 
	enqueueCompleteRecheckRequest(
		final DiskManagerCheckRequestListener 	listener,
		final Object							user_data ) 
	{  	
	 	Thread t = new AEThread("DMW&C::checker")
		{
	  		public void
			runSupport()
	  		{
	  			try{
	  				complete_recheck_in_progress	= true;
	  					
	  				final AESemaphore	sem = new AESemaphore( "DMW&C::checker" );
	  				
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
	        
	  					if ( stopped ){
	  						
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
	  					  					
	  					// wait for all to complete
	  					
	  				for (int i=0;i<checks_submitted;i++){
	  						
	  					sem.reserve();
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
		int 									pieceNumber,
		final DiskManagerCheckRequestListener 	listener,
		Object									user_data ) 
	{  	
		checkPiece( 
				pieceNumber, 
				new CheckPieceResultHandler() 
				{
					public void 
					processResult(
						int 	pieceNumber, 
						int 	result,
						Object 	user_data ) 
					{
						if (result == CheckPieceResultHandler.OP_SUCCESS) {
							if (Logger.isEnabled())
								Logger.log(new LogEvent(disk_manager, LOGID, "Piece "
										+ pieceNumber + " passed hash check."));
		
						} else if (result == CheckPieceResultHandler.OP_FAILURE) {
							if (Logger.isEnabled())
								Logger.log(new LogEvent(disk_manager, LOGID,
										LogEvent.LT_ERROR, "Piece " + pieceNumber
												+ " failed hash check."));
		
						} else {
							if (Logger.isEnabled())
								Logger.log(new LogEvent(disk_manager, LOGID,
										LogEvent.LT_ERROR, "Piece " + pieceNumber
												+ " hash check cancelled."));
		
						}

						if ( listener != null){

							listener.pieceChecked(
									pieceNumber,
									result == CheckPieceResultHandler.OP_SUCCESS,
									user_data );
						}
					}
				}, user_data, false);
	}  
	  
	public void
	checkPiece(
		final int 						pieceNumber,
		final CheckPieceResultHandler	_result_handler,
		final Object					user_data,
		final boolean					low_priorty )
	{
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
					             		
		try{
			
			final byte[]	required_hash = disk_manager.getPieceHash(pieceNumber);
	        
				// quick check that the files that make up this piece are at least big enough
				// to warrant reading the data to check
			
			PieceList pieceList = disk_manager.getPieceList(pieceNumber);

			for (int i = 0; i < pieceList.size(); i++) {
				
				PieceMapEntry piece_entry = pieceList.get(i);
					
				if ( piece_entry.getFile().getCacheFile().getLength() < piece_entry.getOffset()){
						
					result_handler.processResult( pieceNumber, CheckPieceResultHandler.OP_FAILURE, user_data );
					
					return;
				}
			}
			
			int this_piece_length = pieceNumber < nbPieces - 1 ? pieceLength : lastPieceLength;

			DiskManagerReadRequest read_request = disk_manager.createReadRequest( pieceNumber, 0, this_piece_length );
			
		   	try{
		   		this_mon.enter();
		   	
				if ( stopped ){
					
					result_handler.processResult( pieceNumber, CheckPieceResultHandler.OP_CANCELLED, user_data );
					
					return;
				}
				
				async_reads++;
		   		
		   	}finally{
		   		
		   		this_mon.exit();
		   	}
		   	
			disk_manager.enqueueReadRequest( 
				read_request,
				new DiskManagerReadRequestListener()
				{
					public void 
					readCompleted( 
						DiskManagerReadRequest 	read_request, 
						DirectByteBuffer 		buffer )
					{
						complete();
						
					   	try{
					   		this_mon.enter();
					   	
							if ( stopped ){
								
								result_handler.processResult( pieceNumber, CheckPieceResultHandler.OP_CANCELLED, user_data );
								
								return;
							}
							
							async_checks++;
					   		
					   	}finally{
					   		
					   		this_mon.exit();
					   	}
						
						try{
					    	final	DirectByteBuffer	f_buffer	= buffer;
					    	
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
					    							
					    							try{
					    								this_mon.enter();
					    							
					    								async_checks--;
					    								
					    								  if ( stopped ){
					    									  
					    									  async_check_sem.release();
					    								  }
					    							}finally{
					    								
					    								this_mon.exit();
					    							}
					    						}
					    					}
					    				}
					    				
									},
									low_priorty );
						
					    	
						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
							
    						buffer.returnToPool();
    						
    						result_handler.processResult( 
    								pieceNumber, 
    								CheckPieceResultHandler.OP_FAILURE,
									user_data );
						}
					}
					  
					public void 
					readFailed( 
						DiskManagerReadRequest 	request, 
						Throwable		 		cause )
					{
						complete();
						
						result_handler.processResult( pieceNumber, CheckPieceResultHandler.OP_FAILURE, user_data );
					}
					
					  protected void
					  complete()
					  {
						  try{
							  this_mon.enter();
							
							  async_reads--;
							  
							  if ( stopped ){
								  
								  async_read_sem.release();
							  }
						  }finally{
							  
							  this_mon.exit();
						  }
					  }
				});
				
		}catch( Throwable e ){
			
			disk_manager.setFailed( "Piece check error - " + Debug.getNestedExceptionMessage(e));
			
			Debug.printStackTrace( e );
			
			result_handler.processResult( pieceNumber, CheckPieceResultHandler.OP_FAILURE, user_data );
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
		
					if (Logger.isEnabled())
						Logger.log(new LogEvent(disk_manager, LOGID, "Changing "
								+ current_piece.getFile().getFile(true).getName()
								+ " to read/write"));
						
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
			
			String file_name = current_piece==null?"<unknown>":current_piece.getFile().getFile(true).getName();
				
			disk_manager.setFailed( Debug.getNestedExceptionMessage(e) + " when processing file '" + file_name + "'" );
			
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
			
			if (Logger.isEnabled()) {
				int x = global_write_queue_block_sem_size;
				x -= global_write_queue_block_sem_next_report_size;
				Logger.log(new LogEvent(disk_manager, LOGID, LogEvent.LT_WARNING,
						"Disk Manager write queue size exceeds " + x));
			}

			global_write_queue_block_sem_next_report_size -= QUEUE_REPORT_CHUNK;
		}
		
		// System.out.println( "write queue size = " + ( global_write_queue_block_sem_size - global_write_queue_block_sem.getValue()));

		// System.out.println( "reserved global write slot (buffer = " + data.limit() + ")" );
		
		try{
			this_mon.enter();
			
	  		if ( stopped ){
	  				  			
	  			throw( new RuntimeException( "WriteChecker stopped" ));
	  		}
			
			writeQueue.add(new QueueElement(pieceNumber, offset, data, user_data, listener ));
			
			writeCheckQueueSem.release();
			
			if ( writeThread == null ){
				
				startDiskWriteThread();
			}

		}finally{
			
			this_mon.exit();
		}
	}

  
	public boolean 
	checkBlock(
		int pieceNumber, 
		int offset, 
		DirectByteBuffer data ) 
	{
		if (pieceNumber < 0) {
			if (Logger.isEnabled())
				Logger.log(new LogEvent(disk_manager, LOGID, LogEvent.LT_ERROR,
						"CHECKBLOCK1: pieceNumber=" + pieceNumber + " < 0"));
			return false;
		}
		if (pieceNumber >= this.nbPieces) {
			if (Logger.isEnabled())
				Logger.log(new LogEvent(disk_manager, LOGID, LogEvent.LT_ERROR,
						"CHECKBLOCK1: pieceNumber=" + pieceNumber + " >= this.nbPieces="
								+ this.nbPieces));
			return false;
		}
		int length = this.pieceLength;
		if (pieceNumber == nbPieces - 1) {
			length = this.lastPieceLength;
		}
		if (offset < 0) {
			if (Logger.isEnabled())
				Logger.log(new LogEvent(disk_manager, LOGID, LogEvent.LT_ERROR,
						"CHECKBLOCK1: offset=" + offset + " < 0"));
			return false;
		}
		if (offset > length) {
			if (Logger.isEnabled())
				Logger.log(new LogEvent(disk_manager, LOGID, LogEvent.LT_ERROR,
						"CHECKBLOCK1: offset=" + offset + " > length=" + length));
			return false;
		}
		int size = data.remaining(DirectByteBuffer.SS_DW);
		if (size <= 0) {
			if (Logger.isEnabled())
				Logger.log(new LogEvent(disk_manager, LOGID, LogEvent.LT_ERROR,
						"CHECKBLOCK1: size=" + size + " <= 0"));
			return false;
		}
		if (offset + size > length) {
			if (Logger.isEnabled())
				Logger.log(new LogEvent(disk_manager, LOGID, LogEvent.LT_ERROR,
						"CHECKBLOCK1: offset=" + offset + " + size=" + size + " > length="
								+ length));
			return false;
		}
		return true;
	}
  

	public boolean 
	checkBlock(
		int pieceNumber, 
		int offset, 
		int length) 
	{
		if (length > max_read_block_size) {
			if (Logger.isEnabled())
				Logger.log(new LogEvent(disk_manager, LOGID, LogEvent.LT_ERROR,
						"CHECKBLOCK2: length=" + length + " > " + max_read_block_size));
		  return false;
		}
		if (length <= 0 ) {
			if (Logger.isEnabled())
				Logger.log(new LogEvent(disk_manager, LOGID, LogEvent.LT_ERROR,
						"CHECKBLOCK2: length=" + length + " <= 0"));
		    return false;
		}	
		if (pieceNumber < 0) {
			if (Logger.isEnabled())
				Logger.log(new LogEvent(disk_manager, LOGID, LogEvent.LT_ERROR,
						"CHECKBLOCK2: pieceNumber=" + pieceNumber + " < 0"));
		  return false;
		}
		if (pieceNumber >= this.nbPieces) {
			if (Logger.isEnabled())
				Logger.log(new LogEvent(disk_manager, LOGID, LogEvent.LT_ERROR,
						"CHECKBLOCK2: pieceNumber=" + pieceNumber + " >= this.nbPieces="
								+ this.nbPieces));
		  return false;
		}
		int pLength = this.pieceLength;
		if (pieceNumber == this.nbPieces - 1)
			pLength = this.lastPieceLength;
		if (offset < 0) {
			if (Logger.isEnabled())
				Logger.log(new LogEvent(disk_manager, LOGID, LogEvent.LT_ERROR,
						"CHECKBLOCK2: offset=" + offset + " < 0"));
		  return false;
		}
		if (offset > pLength) {
			if (Logger.isEnabled())
				Logger.log(new LogEvent(disk_manager, LOGID, LogEvent.LT_ERROR,
						"CHECKBLOCK2: offset=" + offset + " > pLength=" + pLength));
		  return false;
		}
		if (offset + length > pLength) {
			if (Logger.isEnabled())
				Logger.log(new LogEvent(disk_manager, LOGID, LogEvent.LT_ERROR,
						"CHECKBLOCK2: offset=" + offset + " + length=" + length
								+ " > pLength=" + pLength));
		  return false;
		}
		if(!disk_manager.getPieces()[pieceNumber].getDone()) {
			if (Logger.isEnabled())
				Logger.log(new LogEvent(disk_manager, LOGID, LogEvent.LT_ERROR,
						"CHECKBLOCK2: pieceNumber=" + pieceNumber + " not done"));
		  return false;
		}
		return true;
	}

	protected void
	startDiskWriteThread()
	{	     
		writeThread = new DiskWriteThread();
		
		writeThread.start();
	}

	public class 
	DiskWriteThread 
		extends AEThread 
	{
		private volatile boolean bWriteContinue = true;
		
		private AESemaphore	stop_sem	= new AESemaphore( "DMW&C::stop");

		public DiskWriteThread() 
		{
			super("Disk Writer & Checker");
			
			setDaemon(true);
		}

		public void 
		runSupport() 
		{
			try{
				while( bWriteContinue ){
					
					try{
						int	entry_count = writeCheckQueueSem.reserveSet( 64, WRITE_THREAD_IDLE_LIMIT );
							
						if ( !bWriteContinue ){
							
							break;
						}

						if ( entry_count == 0 ){
						
							try{
								this_mon.enter();
								
								if ( writeCheckQueueSem.getValue() == 0 ){
																			
									writeThread	= null;
									
									break;
								}
							}finally{
								
								this_mon.exit();
							}
						}
						
						for (int i=0;i<entry_count;i++){
							
							final QueueElement	elt;
														
							try{
								this_mon.enter();
								
								if ( !bWriteContinue){
																
									break;
								}
																	
								elt	= (QueueElement)writeQueue.remove(0);
								
								// System.out.println( "releasing global write slot" );
	
								global_write_queue_block_sem.release();
																	
							}finally{
								
								this_mon.exit();
							}
										
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
							  }
							  
							}else{
		  
								elt.data.returnToPool();
								
								elt.data = null;
							}
						}
					}catch( Throwable e ){
						
						disk_manager.setFailed( "DiskWriteThread: error - " + Debug.getNestedExceptionMessage(e));

						Debug.printStackTrace( e );
						
						Debug.out( "DiskWriteThread: error occurred during processing: " + e.toString());
					}
				}
			}finally{
					
				stop_sem.releaseForever();
			}
		}

		protected void 
		stopWriteThread()
		{
			try{
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
				
			}finally{
				
				stop_sem.reserve();
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
