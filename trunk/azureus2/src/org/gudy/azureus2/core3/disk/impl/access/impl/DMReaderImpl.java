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

import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.disk.impl.*;
import org.gudy.azureus2.core3.disk.impl.access.*;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.diskmanager.cache.*;

/**
 * @author parg
 *
 */
public class 
DMReaderImpl
	implements DMReader
{
	protected static final int	QUEUE_REPORT_CHUNK	= 32;
	
	protected DiskManagerHelper	disk_manager;

	private boolean		bOverallContinue	= true;
	
	private List		readQueue		= new LinkedList();
	private AESemaphore	readQueueSem	= new AESemaphore("DMReader::readQ");
		
	private AEMonitor	this_mon		= new AEMonitor( "DMReader");
	
	private int			next_report_size	= QUEUE_REPORT_CHUNK;
	
	private boolean			started;
	private AESemaphore		stop_sem	= new AESemaphore( "DMReader::stop");

	private DiskReadThread readThread;

	public
	DMReaderImpl(
		DiskManagerHelper	_disk_manager )
	{
		disk_manager	= _disk_manager;
	}
	
	public void
	start()
	{
		try{
			this_mon.enter();

			if ( started ){
				
				throw( new RuntimeException( "DMReader: start while started"));
			}
			
			if ( !bOverallContinue ){
	
				throw( new RuntimeException( "DMReader: start after stopped"));
			}
			
			started	= true;
					
			readThread = new DiskReadThread();
			
			readThread.start();
			
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
		
			bOverallContinue	= false;
			
		}finally{
			
			this_mon.exit();
		}
		
		readThread.stopIt();
		
			// wait until the thread has stopped
		
		stop_sem.reserve();
	}
	
	public DiskManagerReadRequest
	createRequest(
		int pieceNumber,
		int offset,
		int length )
	{
		return( new DiskManagerRequestImpl( pieceNumber, offset, length ));
	}
	
	public void 
	enqueueReadRequest( 
	  	DiskManagerReadRequest request, 
		DiskManagerReadRequestListener listener ) 
	{
		DiskReadRequest drr = new DiskReadRequest( request, listener );
	    
	   try{
	   		this_mon.enter();
	   
	  		if ( !bOverallContinue ){
	  				  			
	  			throw( new RuntimeException( "Reader stopped" ));
	  		}
	  		
	   		readQueue.add( drr );
	   		
	    }finally{
	    	
	    	this_mon.exit();
	    }
	    
	    readQueueSem.release();
	    
	    int	queue_size = readQueueSem.getValue();
	    
	    if( queue_size > next_report_size ){
	    	
	    	LGLogger.log( "Disk Manager read queue size exceeds " + next_report_size );
	    	
	    	next_report_size += QUEUE_REPORT_CHUNK;
	    }
	    
		// System.out.println( "read queue size = " + queue_size );
	}
	  
		// returns null if the read can't be performed
	
	public DirectByteBuffer 
	readBlock(
		int pieceNumber, 
		int offset, 
		int length ) 
	{
		if ( !bOverallContinue ){
			
			Debug.out( "DMReader:readBlock: called when stopped" );
			
			return( null );
		}
		
		DirectByteBuffer buffer = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_DM_READ,length );

		if (buffer == null) { // Fix for bug #804874
			
			System.out.println("DiskManager::readBlock:: ByteBufferPool returned null buffer");
			
			return null;
		}

		long previousFilesLength = 0;
		int currentFile = 0;
		PieceList pieceList = disk_manager.getPieceList(pieceNumber);

			// temporary fix for bug 784306
		if (pieceList.size() == 0) {
			System.out.println("no pieceList entries for " + pieceNumber);
			return buffer;
		}

		long fileOffset = pieceList.get(0).getOffset();
		while (currentFile < pieceList.size() && pieceList.getCumulativeLengthToPiece(currentFile) < offset) {
			previousFilesLength = pieceList.getCumulativeLengthToPiece(currentFile);
			currentFile++;
			fileOffset = 0;
		}

			// update the offset (we're in the middle of a file)
		
		fileOffset += offset - previousFilesLength;
		
		while (buffer.hasRemaining(DirectByteBuffer.SS_DR) && currentFile < pieceList.size() ) {
     
			PieceMapEntry map_entry = pieceList.get( currentFile );
      			
			int	length_available = map_entry.getLength() - (int)( fileOffset - map_entry.getOffset());
			
				//explicitly limit the read size to the proper length, rather than relying on the underlying file being correctly-sized
				//see long DMWriterAndCheckerImpl::checkPiece note
			
			int entry_read_limit = buffer.position( DirectByteBuffer.SS_DR ) + length_available;
			
				// now bring down to the required read length if this is shorter than this
				// chunk of data
			
			entry_read_limit = Math.min( length, entry_read_limit );
			
			buffer.limit( DirectByteBuffer.SS_DR, entry_read_limit );
      
			boolean	ok = readFileInfoIntoBuffer( map_entry.getFile(), buffer, fileOffset );
      
			buffer.limit ( DirectByteBuffer.SS_DR, length );
      
			if( !ok ){
				
				buffer.returnToPool();
				
				return( null );
			}
      
			currentFile++;
			
			fileOffset = 0;
		}

		buffer.position(DirectByteBuffer.SS_DR,0);
		
		return buffer;
	}
	
		// reads a file into a buffer, returns true when no error, otherwise false.
	
	private boolean 
	readFileInfoIntoBuffer(
		DiskManagerFileInfoImpl file, 
		DirectByteBuffer buffer, 
		long offset) 
	{
		try{
			file.getCacheFile().read( buffer, offset );
				
			return( true );
				
		}catch( CacheFileManagerException e ){
				
			disk_manager.setFailed( Debug.getNestedExceptionMessage(e));
				
			return( false );
		}
	}

	public class 
	DiskReadThread 
		extends AEThread 
	{
		private boolean bReadContinue = true;

		public DiskReadThread() {
			super("Disk Reader");
			setDaemon(true);
		}

		public void 
		runSupport() 
		{
			try{
				while (bReadContinue){	
			
					try{
						int	entry_count = readQueueSem.reserveSet( 10 );
						
						for (int i=0;i<entry_count;i++){
							
							DiskReadRequest drr;
							
							try{
								this_mon.enter();
								
								if ( !bReadContinue){
															
									break;
								}
							
								drr = (DiskReadRequest)readQueue.remove(0);
								
							}finally{
								
								this_mon.exit();
							}
			
							DiskManagerReadRequest request = drr.getRequest();
			
							DirectByteBuffer buffer = readBlock(request.getPieceNumber(), request.getOffset(), request.getLength());
	            
							if (buffer != null) {
								
								drr.readCompleted( buffer );
								
							}else {
								
								String err_msg = "Failed loading piece " +request.getPieceNumber()+ ":" +request.getOffset()+ "->" +(request.getOffset() + request.getLength());
								
								LGLogger.log( LGLogger.ERROR, err_msg );
								
								System.out.println( err_msg );
							}
						}
					}catch( Throwable e ){
						
						disk_manager.setFailed( "DiskReadThread: error - " + Debug.getNestedExceptionMessage(e));
						
						Debug.printStackTrace( e );
						
						Debug.out( "DiskReadThread: error occurred during processing: " + e.toString());
					}
				}
			}finally{
				
				stop_sem.release();
			}
		}

		public void stopIt() {

			try{
				this_mon.enter();
				
				bReadContinue = false;
				
			}finally{
				
				this_mon.exit();
			}
			
			readQueueSem.releaseForever();
						
			while (readQueue.size() != 0){
				
				readQueue.remove(0);
			}
		}
	}
	 
	private static class 
	DiskReadRequest 
	{
	    private final DiskManagerReadRequest 	request;
	    private final DiskManagerReadRequestListener 	listener;
	    
	    //private long	queue_time	= SystemTime.getCurrentTime();
	    
	    private 
		DiskReadRequest( 
			DiskManagerReadRequest 	r, 
			DiskManagerReadRequestListener l ) 
	    {
	      request 	= r;
	      listener = l;
	    }
	    
	    protected DiskManagerReadRequest
		getRequest()
	    {
	    	return( request );
	    }
	    
	    protected void
		readCompleted(
			DirectByteBuffer	buffer )
	    {
	    	//long	now	= SystemTime.getCurrentTime();    	
	    	//long	processing_time = now - request.getTimeCreated();
	    	//long	queueung_time	= now - queue_time;
	    	
	    	//System.out.println( "DiskManager req time = " + processing_time + ", queue = " + queueung_time );
	    	
	    	listener.readCompleted( request, buffer );
	    }
	}

	  
}
