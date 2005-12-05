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
import java.util.List;

import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.disk.impl.*;
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
DMReaderImpl
	implements DMReader
{
	private static final LogIDs LOGID = LogIDs.DISK;

	private DiskManagerHelper		disk_manager;
	private DiskAccessController	disk_access;	

	private int						async_reads;
	private AESemaphore				async_read_sem = new AESemaphore("DMReader:asyncReads");
	
	private boolean					started;
	private boolean					stopped;
	
	protected AEMonitor	this_mon	= new AEMonitor( "DMReader" );
	
	public
	DMReaderImpl(
		DiskManagerHelper		_disk_manager )
	{
		disk_manager	= _disk_manager;
		disk_access		= disk_manager.getDiskAccessController();
	}
	
	public void
	start()
	{
		try{
			this_mon.enter();
		
			if ( started ){
				
				throw( new RuntimeException("can't start twice" ));
			}
			
			if ( stopped ){
				
				throw( new RuntimeException("already been stopped" ));
			}
		
			started	= true;
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	stop()
	{
		int	read_wait;
		
		try{
			this_mon.enter();
		
			if ( stopped || !started ){
				
				return;
			}
			
			stopped	= true;
			
			read_wait	= async_reads;
			
		}finally{
			
			this_mon.exit();
		}
		
		for (int i=0;i<read_wait;i++){
			
			async_read_sem.reserve();
		}
	}
	
	public DiskManagerReadRequest
	createRequest(
		int pieceNumber,
		int offset,
		int length )
	{
		return( new DiskManagerReadRequestImpl( pieceNumber, offset, length ));
	}
		  
		// returns null if the read can't be performed
	
	public DirectByteBuffer 
	readBlock(
		int pieceNumber, 
		int offset, 
		int length ) 
	{
		DiskManagerReadRequest	request = createRequest( pieceNumber, offset, length );
		
		final AESemaphore	sem = new AESemaphore( "DMReader:readBlock" );
		
		final DirectByteBuffer[]	result = {null};
		
		readBlock( 
				request,
				new DiskManagerReadRequestListener()
				{
					  public void 
					  readCompleted( 
					  		DiskManagerReadRequest 	request, 
							DirectByteBuffer 		data )
					  {
						  result[0]	= data;
						  
						  sem.release();
					  }
					  
					  public void 
					  readFailed( 
					  		DiskManagerReadRequest 	request, 
							Throwable		 		cause )
					  {
						  sem.release();
					  }
				});
		
		sem.reserve();
		
		return( result[0] );
	}
	
	public void 
	readBlock(
		final DiskManagerReadRequest			request,
		final DiskManagerReadRequestListener	listener )
	{

		DirectByteBuffer buffer	= null;
		
		try{
			int	length		= request.getLength();
	
			buffer = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_DM_READ,length );
	
			if ( buffer == null ) { // Fix for bug #804874
				
				Debug.out("DiskManager::readBlock:: ByteBufferPool returned null buffer");
				
				listener.readFailed( request, new Exception( "Out of memory" ));
				
				return;
			}
	
			int	pieceNumber	= request.getPieceNumber();
			int	offset		= request.getOffset();
			
			PieceList pieceList = disk_manager.getPieceList(pieceNumber);
	
				// temporary fix for bug 784306
			
			if ( pieceList.size() == 0 ){
				
				Debug.out("no pieceList entries for " + pieceNumber);
				
				listener.readCompleted( request, buffer );
				
				return;
			}
	
			long previousFilesLength = 0;
			
			int currentFile = 0;
			
			long fileOffset = pieceList.get(0).getOffset();
			
			while (currentFile < pieceList.size() && pieceList.getCumulativeLengthToPiece(currentFile) < offset) {
				
				previousFilesLength = pieceList.getCumulativeLengthToPiece(currentFile);
				
				currentFile++;
				
				fileOffset = 0;
			}
	
				// update the offset (we're in the middle of a file)
			
			fileOffset += offset - previousFilesLength;
			
			List	chunks = new ArrayList();
			
			int	buffer_position = 0;
			
			while ( buffer_position < length && currentFile < pieceList.size()) {
	     
				PieceMapEntry map_entry = pieceList.get( currentFile );
	      			
				int	length_available = map_entry.getLength() - (int)( fileOffset - map_entry.getOffset());
				
					//explicitly limit the read size to the proper length, rather than relying on the underlying file being correctly-sized
					//see long DMWriterAndCheckerImpl::checkPiece note
				
				int entry_read_limit = buffer_position + length_available;
				
					// now bring down to the required read length if this is shorter than this
					// chunk of data
				
				entry_read_limit = Math.min( length, entry_read_limit );
				
					// this chunk denotes a read up to buffer offset "entry_read_limit"
				
				chunks.add( new Object[]{ map_entry.getFile().getCacheFile(), new Long(fileOffset), new Integer( entry_read_limit )});
				
				buffer_position = entry_read_limit;
	      
				currentFile++;
				
				fileOffset = 0;
			}
	
			if ( chunks.size() == 0 ){
				
				Debug.out("no chunk reads for " + pieceNumber);
					
				listener.readCompleted( request, buffer );
				
				return;
			}
			
				// this is where we go async and need to start counting requests for the sake
				// of shutting down tidily
			
			DiskManagerReadRequestListener	l = 
				new DiskManagerReadRequestListener()
				{
					 public void 
					  readCompleted( 
					  		DiskManagerReadRequest 	request, 
							DirectByteBuffer 		data )
					 {
						 complete();
						 
						 listener.readCompleted( request, data );
					 }
					  
					  public void 
					  readFailed( 
					  		DiskManagerReadRequest 	request, 
							Throwable		 		cause )
					  {
						  complete();
						  
						  listener.readFailed( request, cause );
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
				};
			
			try{
				this_mon.enter();
				
				if ( stopped ){
					
					throw( new Exception( "Disk reader has been stopped" ));
				}
				
				async_reads++;
				
			}finally{
				
				this_mon.exit();
			}
			
			new requestDispatcher( request, l, buffer, chunks );

		}catch( Throwable e ){
			
			if ( buffer != null ){
				
				buffer.returnToPool();
			}
			
			disk_manager.setFailed( "Disk read error - " + Debug.getNestedExceptionMessage(e));
			
			Debug.printStackTrace( e );
			
			listener.readFailed( request, e );
		}
	}
	
	protected class
	requestDispatcher
		implements DiskAccessRequestListener
	{
		private DiskManagerReadRequest			dm_request;
		private DiskManagerReadRequestListener	listener;
		private DirectByteBuffer				buffer;
		private List							chunks;
		
		private int	buffer_length;
		
		private int	chunk_index;
		
		protected
		requestDispatcher(
			DiskManagerReadRequest			_request,
			DiskManagerReadRequestListener	_listener,
			DirectByteBuffer				_buffer,
			List							_chunks )
		{
			dm_request	= _request;
			listener	= _listener;
			buffer		= _buffer;
			chunks		= _chunks;
			
			/*
			String	str = "Read: " + dm_request.getPieceNumber()+"/"+dm_request.getOffset()+"/"+dm_request.getLength()+":";
			
			for (int i=0;i<chunks.size();i++){
			
				Object[]	entry = (Object[])chunks.get(i);
				
				String	str2 = entry[0] + "/" + entry[1] +"/" + entry[2];
				
				str += (i==0?"":",") + str2;
			}
			
			System.out.println( str );
			*/
			
			buffer_length = buffer.limit( DirectByteBuffer.SS_DR );
			
			dispatch();
		}	
		
		protected void
		dispatch()
		{
			try{
				if ( chunk_index == chunks.size()){
					
					buffer.limit( DirectByteBuffer.SS_DR, buffer_length );
					
					buffer.position(  DirectByteBuffer.SS_DR, 0 );
					
					listener.readCompleted( dm_request, buffer );
					
				}else{
					
					DiskAccessRequestListener	l;
					
					if ( chunk_index == 1 && chunks.size() > 32 ){
						
							// for large numbers of chunks drop the recursion approach and
							// do it linearly (but on the async thread)
						
						for (int i=1;i<chunks.size();i++){
							
							final AESemaphore	sem 	= new AESemaphore( "DMR:dispatch:asyncReq" );
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
						
						buffer.limit( DirectByteBuffer.SS_DR, buffer_length );
						
						buffer.position(  DirectByteBuffer.SS_DR, 0 );
						
						listener.readCompleted( dm_request, buffer );					
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
		{
			
			Object[]	stuff = (Object[])chunks.get( chunk_index++ );
			
			buffer.limit( DirectByteBuffer.SS_DR, ((Integer)stuff[2]).intValue());
			
			disk_access.queueReadRequest(
				(CacheFile)stuff[0],
				((Long)stuff[1]).longValue(),
				buffer,
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
			
			disk_manager.setFailed( "Disk read error - " + Debug.getNestedExceptionMessage(cause));
			
			Debug.printStackTrace( cause );
			
			listener.readFailed( dm_request, cause );
		}	
	}	
}
