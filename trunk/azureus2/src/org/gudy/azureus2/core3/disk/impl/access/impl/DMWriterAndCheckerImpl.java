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

import java.util.ArrayList;
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


import com.aelitis.azureus.core.diskmanager.access.DiskAccessController;
import com.aelitis.azureus.core.diskmanager.access.DiskAccessRequest;
import com.aelitis.azureus.core.diskmanager.access.DiskAccessRequestListener;
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
    
	private static boolean 	friendly_hashing;
	private static boolean	flush_pieces;

    static{
    	
    	 ParameterListener param_listener = new ParameterListener() {
    	    public void 
			parameterChanged( 
				String  str ) 
    	    {
    	      friendly_hashing 	= COConfigurationManager.getBooleanParameter( "diskmanager.friendly.hashchecking" );
    	  	  
    	  	  flush_pieces		= COConfigurationManager.getBooleanParameter( "diskmanager.perf.cache.flushpieces" );

    	    }
    	 };

 		COConfigurationManager.addAndFireParameterListener( "diskmanager.friendly.hashchecking", param_listener );
		COConfigurationManager.addParameterListener( "diskmanager.perf.cache.flushpieces", param_listener );

    }

	private DiskManagerHelper		disk_manager;
	private DiskAccessController	disk_access;
		
	private int				async_checks;
	private AESemaphore		async_check_sem 	= new AESemaphore("DMW&C::asyncCheck");
	
	private int				async_reads;
	private AESemaphore		async_read_sem 		= new AESemaphore("DMW&C::asyncRead");

	private int				async_writes;
	private AESemaphore		async_write_sem 		= new AESemaphore("DMW&C::asyncWrite");

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
		disk_access		= disk_manager.getDiskAccessController();
		
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
		int	check_wait;
		int	read_wait;
		int write_wait;
		
		try{
			this_mon.enter();

			if ( stopped || !started ){
			
				return;
			}
					
				// when we exit here we guarantee that all file usage operations have completed
				// i.e. writes and checks (checks being doubly async)
			
			stopped	= true;
         			
			read_wait	= async_reads;
			check_wait	= async_checks;
			write_wait	= async_writes;
			
		}finally{
			
			this_mon.exit();
		}		
	
			// wait for reads
		
		for (int i=0;i<read_wait;i++){
			
			async_read_sem.reserve();
		}
		
			// wait for checks
		
		for (int i=0;i<check_wait;i++){
			
			async_check_sem.reserve();
		}
		
			// wait for writes
		
		for (int i=0;i<write_wait;i++){
			
			async_write_sem.reserve();
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

					while (written < length && !stopped ){
						
						int	write_size = buffer.capacity(DirectByteBuffer.SS_DW);
						
						if ((length - written) < write_size ){
            	
							write_size = (int)(length - written);
						}
            
						buffer.limit(DirectByteBuffer.SS_DW, write_size);
             
						final AESemaphore	sem = new AESemaphore( "DMW&C:zeroFile" );
						final Throwable[]	op_failed = {null};
						
						disk_access.queueWriteRequest(
								cache_file,
								written,
								buffer,
								false,
								new DiskAccessRequestListener()
								{
									public void
									requestComplete(
										DiskAccessRequest	request )
									{
										sem.release();
									}
									
									public void
									requestCancelled(
										DiskAccessRequest	request )
									{
										op_failed[0] = new Throwable( "Request cancelled" );
										
										sem.release();
									}
									
									public void
									requestFailed(
										DiskAccessRequest	request,
										Throwable			cause )
									{
										op_failed[0]	= cause;
										
										sem.release();
									}
								});
           
						sem.reserve();
						
						if ( op_failed[0] != null ){
							
							throw( op_failed[0] );
						}
						
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
		}catch ( Throwable e){ 
			
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
            
            
		            final AESemaphore	 run_sem = new AESemaphore( "DMW&C::checker:runsem", 2 );
		            
	  				for ( int i=0; i < nbPieces; i++ ){
	  					
	  					run_sem.reserve();
	  					
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
		  	       						
		  	       						run_sem.release();
		  	       						
		  	       						sem.release();
		  	       					}
		  	       				}
							},
							user_data,
							false );
	  					
	  					checks_submitted++;
	  					
	  					if( delay > 0 ){
	  						
	  						try{
	  						
	  							Thread.sleep( delay );
	  							
	  						}catch( Throwable e ){
	  							
	  						}
	  					}
	  				}
	  					  					
	  					// wait for all to complete
	  					
	  				for (int i=0;i<checks_submitted;i++){
	  						
	  					sem.reserve();
	  				}
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
		enqueueCheckRequest( pieceNumber, listener, user_data, flush_pieces );
	}
	
	protected void 
	enqueueCheckRequest(
		int 									pieceNumber,
		final DiskManagerCheckRequestListener 	listener,
		Object									user_data,
		boolean									read_flush ) 
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
				}, user_data, false, read_flush );
	}  
	  
	public void
	checkPiece(
		final int 						pieceNumber,
		final CheckPieceResultHandler	_result_handler,
		final Object					user_data,
		final boolean					low_priorty )
	{
		checkPiece( pieceNumber, _result_handler, user_data, low_priorty, false );
	}
	
	protected void
	checkPiece(
		final int 						pieceNumber,
		final CheckPieceResultHandler	_result_handler,
		final Object					user_data,
		final boolean					low_priorty,
		boolean							read_flush )
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

			try{
				for (int i = 0; i < pieceList.size(); i++) {
					
					PieceMapEntry piece_entry = pieceList.get(i);
						
					if ( piece_entry.getFile().getCacheFile().getLength() < piece_entry.getOffset()){
							
						result_handler.processResult( pieceNumber, CheckPieceResultHandler.OP_FAILURE, user_data );
						
						return;
					}
				}
			}catch( Throwable e ){
			
					// we can fail here if the disk manager has been stopped as the cache file length access may be being
					// performed on a "closed" (i.e. un-owned) file
				
				result_handler.processResult( pieceNumber, CheckPieceResultHandler.OP_CANCELLED, user_data );

				return;
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
		   	
		   	read_request.setFlush( read_flush );
		   	
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
								
								buffer.returnToPool();
								
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
	 
	public DiskManagerWriteRequest 
	createWriteRequest(
		int 									pieceNumber, 
		int 									offset, 
		DirectByteBuffer 						buffer,
		Object 									user_data )
	{
		return( new DiskManagerWriteRequestImpl( pieceNumber, offset, buffer, user_data ));
	}	
	
	
	public void 
	writeBlock(
		DiskManagerWriteRequest					request,				
		final DiskManagerWriteRequestListener	listener ) 

	{	
		try{
			int					pieceNumber	= request.getPieceNumber();
			DirectByteBuffer	buffer		= request.getBuffer();
			int					offset		= request.getOffset();
			
			//Do not allow to write in a piece marked as done.
					
			if ( disk_manager.getPieces()[pieceNumber].getDone()){
				
				buffer.returnToPool();

				listener.writeCompleted( request );
				
			}else{
				
				int	buffer_position = buffer.position(DirectByteBuffer.SS_DW);
				int buffer_limit	= buffer.limit(DirectByteBuffer.SS_DW);
				
				int previousFilesLength = 0;
				
				int currentFile = 0;
				
				PieceList pieceList = disk_manager.getPieceList(pieceNumber);
				
				PieceMapEntry current_piece = pieceList.get(currentFile);
				
				long fileOffset = current_piece.getOffset();
				
				while ((previousFilesLength + current_piece.getLength()) < offset) {
					
					previousFilesLength += current_piece.getLength();
					
					currentFile++;
					
					fileOffset = 0;
					
					current_piece = pieceList.get(currentFile);
				}
		
				List	chunks = new ArrayList();
				
					// Now current_piece points to the first file that contains data for this block
				
				while ( buffer_position < buffer_limit ){
						
					current_piece = pieceList.get(currentFile);
	
					long file_limit = buffer_position + 
										((current_piece.getFile().getLength() - current_piece.getOffset()) - 
											(offset - previousFilesLength));
		       
					if ( file_limit > buffer_limit ){
						
						file_limit	= buffer_limit;
					}
						
						// could be a zero-length file
					
					if ( file_limit > buffer_position ){
	
						long	file_pos = fileOffset + (offset - previousFilesLength);
						
						chunks.add( 
								new Object[]{ current_piece.getFile(),
								new Long( file_pos ),
								new Integer((int)file_limit )});
											
						buffer_position = (int)file_limit;
					}
					
					currentFile++;
					
					fileOffset = 0;
					
					previousFilesLength = offset;
				}
				
				
				DiskManagerWriteRequestListener	l = 
					new DiskManagerWriteRequestListener()
					{
						public void 
						writeCompleted( 
							DiskManagerWriteRequest 	request ) 
						{
							complete();
							 
							listener.writeCompleted( request );
						}
						  
						public void 
						writeFailed( 
							DiskManagerWriteRequest 	request, 
							Throwable		 			cause )
						{
							complete();
							  
							listener.writeFailed( request, cause );
						}
						  
						protected void
						complete()
						{
							try{
								this_mon.enter();
								
								async_writes--;
								  
								if ( stopped ){
									  
									async_write_sem.release();
								}
							}finally{
								  
								this_mon.exit();
							}
						}
					};
	
				try{
					this_mon.enter();
					
					if ( stopped ){
						
						buffer.returnToPool();
						
						listener.writeFailed( request, new Exception( "Disk writer has been stopped" ));
						
						return;
						
					}else{
					
						async_writes++;
					}
					
				}finally{
					
					this_mon.exit();
				}
									
				new requestDispatcher( request, l, buffer, chunks );
			}
		}catch( Throwable e ){
						
			request.getBuffer().returnToPool();
			
			disk_manager.setFailed( "Disk write error - " + Debug.getNestedExceptionMessage(e));
			
			Debug.printStackTrace( e );
			
			listener.writeFailed( request, e );
		}
	}
	
	protected class
	requestDispatcher
		implements DiskAccessRequestListener
	{
		private DiskManagerWriteRequest			request;
		private DiskManagerWriteRequestListener	listener;
		private DirectByteBuffer				buffer;
		private List							chunks;
				
		private int	chunk_index;
		
		protected
		requestDispatcher(
			DiskManagerWriteRequest			_request,
			DiskManagerWriteRequestListener	_listener,
			DirectByteBuffer				_buffer,
			List							_chunks )
		{	
			request		= _request;
			listener	= _listener;
			buffer		= _buffer;
			chunks		= _chunks;
			
			/*
			String	str = "Write: " + request.getPieceNumber() + "/" + request.getOffset() + ":";

			for (int i=0;i<chunks.size();i++){
			
				Object[]	entry = (Object[])chunks.get(i);
				
				String	str2 = entry[0] + "/" + entry[1] +"/" + entry[2];
				
				str += (i==0?"":",") + str2;
			}
			
			System.out.println( str );
			*/
						
			dispatch();
		}	
		
		protected void
		dispatch()
		{
			try{
				if ( chunk_index == chunks.size()){
					
					listener.writeCompleted( request );
					
				}else{
					
					DiskAccessRequestListener	l;
							
					if ( chunk_index == 1 && chunks.size() > 32 ){
						
							// for large numbers of chunks drop the recursion approach and
							// do it linearly (but on the async thread)
						
						for (int i=1;i<chunks.size();i++){
							
							final AESemaphore	sem 	= new AESemaphore( "DMW&C:dispatch:asyncReq" );
							final Throwable[]	error	= {null};
							
							doRequest( 
								new DiskAccessRequestListener()
								{
									public void
									requestComplete(
										DiskAccessRequest	request )
									{
										sem.release();
									}
									
									public void
									requestCancelled(
										DiskAccessRequest	request )
									{
										Debug.out( "shouldn't get here" );
									}
									
									public void
									requestFailed(
										DiskAccessRequest	request,
										Throwable			cause )
									{
										error[0]	= cause;
										
										sem.release();
									}
								});
							
							sem.reserve();
							
							if ( error[0] != null ){
								
								throw( error[0] );
							}
						}
						
						listener.writeCompleted( request );
						
					}else{
						
						doRequest( this );
					}
				}
			}catch( Throwable e ){
				
				failed( e );
			}
		}
		
		protected void
		doRequest(
			DiskAccessRequestListener	l )
		
			throws CacheFileManagerException
		{
			Object[]	stuff = (Object[])chunks.get( chunk_index++ );

			
			DiskManagerFileInfoImpl	file = (DiskManagerFileInfoImpl)stuff[0];
			
			buffer.limit( DirectByteBuffer.SS_DR, ((Integer)stuff[2]).intValue());
			
			if ( file.getAccessMode() == DiskManagerFileInfo.READ ){
				
				if (Logger.isEnabled())
					Logger.log(new LogEvent(disk_manager, LOGID, "Changing "
							+ file.getFile(true).getName()
							+ " to read/write"));
					
				file.setAccessMode( DiskManagerFileInfo.WRITE );
			}
			
			boolean	handover_buffer	= chunk_index == chunks.size(); 
			
			disk_access.queueWriteRequest(
				file.getCacheFile(),
				((Long)stuff[1]).longValue(),
				buffer,
				handover_buffer,
				l );
		}
		
		public void
		requestComplete(
			DiskAccessRequest	request )
		{
			dispatch();
		}
		
		public void
		requestCancelled(
			DiskAccessRequest	request )
		{
				// we never cancel so nothing to do here
			
			Debug.out( "shouldn't get here" );
		}
		
		public void
		requestFailed(
			DiskAccessRequest	request,
			Throwable			cause )
		{
			failed( cause );
		}
		
		protected void
		failed(
			Throwable			cause )
		{
			buffer.returnToPool();
			
			disk_manager.setFailed( "Disk write error - " + Debug.getNestedExceptionMessage(cause));
			
			Debug.printStackTrace( cause );
			
			listener.writeFailed( request, cause );
		}	
	}	
}
