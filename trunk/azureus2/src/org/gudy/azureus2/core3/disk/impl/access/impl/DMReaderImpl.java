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

import com.aelitis.azureus.core.diskmanager.ReadRequestListener;
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

	private List		readQueue;
	private AESemaphore	readQueueSem;
	private AEMonitor	readQueue_mon		= new AEMonitor( "DMReader:RQ");
	
	private int			next_report_size	= 0;
	
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
		readQueue			= new LinkedList();
		readQueueSem		= new AESemaphore("readQ");
		next_report_size	= QUEUE_REPORT_CHUNK;
		
		readThread = new DiskReadThread();
		
		readThread.start();
	}
	
	public void
	stop()
	{
		if (readThread != null)
			readThread.stopIt();
 
	}
	
	public DiskManagerRequest
	createRequest(
		int pieceNumber,
		int offset,
		int length )
	{
		return( new DiskManagerRequestImpl( pieceNumber, offset, length ));
	}
	
	public void 
	enqueueReadRequest( 
	  	DiskManagerRequest request, 
		ReadRequestListener listener ) 
	{
		DiskReadRequest drr = new DiskReadRequest( request, listener );
	    
	   try{
	   		readQueue_mon.enter();
	   
	   		readQueue.add( drr );
	   		
	    }finally{
	    	
	    	readQueue_mon.exit();
	    }
	    
	    readQueueSem.release();
	    
	    int	queue_size = readQueueSem.getValue();
	    
	    if( queue_size > next_report_size ){
	    	
	    	LGLogger.log( "Disk Manager read queue size exceeds " + next_report_size );
	    	
	    	next_report_size += QUEUE_REPORT_CHUNK;
	    }
	    
		// System.out.println( "read queue size = " + queue_size );
	}
	  
	public DirectByteBuffer 
	readBlock(
		int pieceNumber, 
		int offset, 
		int length ) 
	{
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
		// noError is only used for error reporting, it could probably be removed
		boolean noError = true;
		while (buffer.hasRemaining(DirectByteBuffer.SS_DR)
			&& currentFile < pieceList.size()
			&& (noError = readFileInfoIntoBuffer(pieceList.get(currentFile).getFile(), buffer, fileOffset))) {

			currentFile++;
			fileOffset = 0;
		}

		if (!noError) {
			// continue the error report
			//PieceMapEntry tempPiece = pieceList.get(currentFile);
			//System.out.println("ERROR IN READ BLOCK (CONTINUATION FROM READ FILE INFO INTO BUFFER): *Debug Information*");
			//System.out.println("BufferLimit: " + buffer.limit());
			//System.out.println("BufferRemaining: " + buffer.remaining());
			//System.out.println("PieceNumber: " + pieceNumber);
			//System.out.println("Offset: " + fileOffset);
			//System.out.println("Length  " + length);
			//System.out.println("PieceLength: " + tempPiece.getLength());
			//System.out.println("PieceOffset: " + tempPiece.getOffset());
			//System.out.println("TotalNumPieces(this.nbPieces): " + this.nbPieces);


			// Stop, because if it happened once, it will probably happen everytime
			// Especially in the case of a CD being removed
	
			disk_manager.stopIt();
			
			disk_manager.setState( DiskManager.FAULTY );
		}

		buffer.position(DirectByteBuffer.SS_DR,0);
		return buffer;
	}

		// refactored out of readBlock() - Moti
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
				
			disk_manager.setErrorMessage((e.getCause()!=null?e.getCause().getMessage():e.getMessage()));
				
			return( false );
		}
	}

	public class DiskReadThread extends AEThread {
		private boolean bReadContinue = true;

		public DiskReadThread() {
			super("Disk Reader");
			setDaemon(true);
		}

		public void runSupport() {
			
			while (bReadContinue){	
		
				try{
					int	entry_count = readQueueSem.reserveSet( 10 );
					
					for (int i=0;i<entry_count;i++){
						
						DiskReadRequest drr;
						
						try{
							readQueue_mon.enter();
							
							if ( !bReadContinue){
														
								break;
							}
						
							drr = (DiskReadRequest)readQueue.remove(0);
						}finally{
							
							readQueue_mon.exit();
						}
		
						DiskManagerRequest request = drr.getRequest();
		
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
					
					Debug.printStackTrace( e );
					
					Debug.out( "DiskReadThread: error occurred during processing: " + e.toString());
				}
			}
		}

		public void stopIt() {

			try{
				readQueue_mon.enter();
				
				bReadContinue = false;
			}finally{
				
				readQueue_mon.exit();
			}
			
			readQueueSem.releaseForever();
						
			while (readQueue.size() != 0) {
				readQueue.remove(0);
			}
		}
	}
	 
	private static class 
	DiskReadRequest 
	{
	    private final DiskManagerRequest 	request;
	    private final ReadRequestListener 	listener;
	    
	    //private long	queue_time	= SystemTime.getCurrentTime();
	    
	    private 
		DiskReadRequest( 
			DiskManagerRequest 	r, 
			ReadRequestListener l ) 
	    {
	      request 	= r;
	      listener = l;
	    }
	    
	    protected DiskManagerRequest
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
