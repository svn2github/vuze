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
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.disk.cache.*;
import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.impl.DiskManagerFileInfoImpl;
import org.gudy.azureus2.core3.disk.impl.DiskManagerHelper;
import org.gudy.azureus2.core3.disk.impl.PieceList;
import org.gudy.azureus2.core3.disk.impl.PieceMapEntry;
import org.gudy.azureus2.core3.disk.impl.access.*;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;
import org.gudy.azureus2.core3.util.Md5Hasher;
import org.gudy.azureus2.core3.util.SHA1Hasher;
import org.gudy.azureus2.core3.util.Semaphore;

/**
 * @author parg
 *
 */

public class 
DMWriterAndCheckerImpl 
	implements DMWriterAndChecker
{
	private static Semaphore	global_write_queue_block_sem;
	private static Semaphore	global_check_queue_block_sem;
	
	static{
		int	write_limit_blocks = COConfigurationManager.getIntParameter("DiskManager Write Queue Block Limit", 0);

		global_write_queue_block_sem = new Semaphore(write_limit_blocks);
		
		if ( write_limit_blocks == 0 ){
			
			global_write_queue_block_sem.releaseForever();
		}
		
		int	check_limit_pieces = COConfigurationManager.getIntParameter("DiskManager Check Queue Piece Limit", 0);

		global_check_queue_block_sem = new Semaphore(check_limit_pieces);
		
		if ( check_limit_pieces == 0 ){
			
			global_check_queue_block_sem.releaseForever();
		}
		
		// System.out.println( "global writes = " + write_limit_blocks + ", global checks = " + check_limit_pieces );
	}

	private DiskManagerHelper		disk_manager;
	private DMReader				reader;
	
	private DiskWriteThread writeThread;
	private List 			writeQueue;
	private List 			checkQueue;
	private Semaphore		writeCheckQueueSem;
	private Object			writeCheckQueueLock;
	
	private SHA1Hasher hasher;
	private Md5Hasher md5;
	private DirectByteBuffer md5Result;
	
	private DirectByteBuffer allocateAndTestBuffer;
		

	protected boolean	bOverallContinue		= true;
	
	protected int		pieceLength;
	protected int		lastPieceLength;
	protected long		totalLength;
	
	protected int		nbPieces;
	
	public
	DMWriterAndCheckerImpl(
		DiskManagerHelper	_disk_manager,
		DMReader			_reader )
	{
		disk_manager	= _disk_manager;
		reader			= _reader;
		
		pieceLength		= disk_manager.getPieceLength();
		lastPieceLength	= disk_manager.getLastPieceLength();
		totalLength		= disk_manager.getTotalLength();
		
		nbPieces		= disk_manager.getNumberOfPieces();
		
		md5 = new Md5Hasher();
	    md5Result = DirectByteBufferPool.getBuffer( 16 );
	    
		hasher = new SHA1Hasher();
	}
	
	public void
	start()
	{
		bOverallContinue	= true;
		
		writeQueue			= new LinkedList();
		checkQueue			= new LinkedList();
		writeCheckQueueSem	= new Semaphore();
		writeCheckQueueLock	= new Object();
		
		//Create the ByteBuffer for checking (size : pieceLength)
        allocateAndTestBuffer = DirectByteBufferPool.getBuffer(pieceLength);
    
		allocateAndTestBuffer.limit(pieceLength);
		for (int i = 0; i < allocateAndTestBuffer.limit(); i++) {
			allocateAndTestBuffer.put((byte)0);
		}
		allocateAndTestBuffer.position(0);
		
		writeThread = new DiskWriteThread();
		writeThread.start();
	}
	
	public void
	stop()
	{
		bOverallContinue	= false;
		
		if (writeThread != null)
			writeThread.stopIt();
		
		   
	    if (allocateAndTestBuffer != null) {
	    	allocateAndTestBuffer.returnToPool();
	      allocateAndTestBuffer = null;
	    }
	}
	
	 public boolean isChecking() {
	    return (checkQueue.size() != 0);
	  }

	  public boolean zeroFile( DiskManagerFileInfoImpl file, long length ) {
	  	CacheFile	fm_file = file.getCacheFile();
			long written = 0;
			synchronized (file){
			  try{
			    if( length == 0 ) { //create a zero-length file if it is listed in the torrent
	          fm_file.setLength( 0 );
	        }
	        else {
	          while (written < length && bOverallContinue) {
	            allocateAndTestBuffer.limit(allocateAndTestBuffer.capacity());
	            if ((length - written) < allocateAndTestBuffer.remaining())
	              allocateAndTestBuffer.limit((int) (length - written));
	             int deltaWriten = fm_file.write(allocateAndTestBuffer, written);
	             allocateAndTestBuffer.position(0);
	             written += deltaWriten;
	             disk_manager.setAllocated( disk_manager.getAllocated() + deltaWriten );
	             disk_manager.setPercentDone((int) ((disk_manager.getAllocated() * 1000) / totalLength));
	          }
	        }
					if (!bOverallContinue) {
					   fm_file.close();
					   return false;
					}
			  } catch (Exception e) {  e.printStackTrace();  }
			}
			return true;
		}
	  

	  public void 
	   aSyncCheckPiece(
	   		int pieceNumber) 
	   {  		
	   		global_check_queue_block_sem.reserve();
	   		
	  		synchronized( writeCheckQueueLock ){
	 		
	   			checkQueue.add(new QueueElement(pieceNumber, 0, null, null));
	    	}
	   		
	   		writeCheckQueueSem.release();
	   }
	  
	  
		public synchronized boolean checkPiece(int pieceNumber) {
		    
		    if( COConfigurationManager.getBooleanParameter( "diskmanager.friendly.hashchecking" ) ) {
		      try{  Thread.sleep( 100 );  }catch(Exception e) { e.printStackTrace(); }
		    }
		       
		    boolean[]	pieceDone	= disk_manager.getPiecesDone();
		    
		    if (bOverallContinue == false) return false;

				allocateAndTestBuffer.position(0);

				int length = pieceNumber < nbPieces - 1 ? pieceLength : lastPieceLength;

				allocateAndTestBuffer.limit(length);

				//get the piece list
				PieceList pieceList = disk_manager.getPieceList(pieceNumber);

				//for each piece
				for (int i = 0; i < pieceList.size(); i++) {
					//get the piece and the file 
					PieceMapEntry tempPiece = pieceList.get(i);
		            

					try {
		                    
							   //if the file is large enough
						if ( tempPiece.getFile().getCacheFile().getSize() >= tempPiece.getOffset()){
							
							tempPiece.getFile().getCacheFile().read(allocateAndTestBuffer, tempPiece.getOffset());
							
						}else{
								   //too small, can't be a complete piece
							
							allocateAndTestBuffer.clear();
							
							pieceDone[pieceNumber] = false;
							
							return false;
						}
					}catch (Exception e){
						
						e.printStackTrace();
					}
				}

				try {
		      
		      if (bOverallContinue == false) return false;
		      
					allocateAndTestBuffer.position(0);

					byte[] testHash = hasher.calculateHash(allocateAndTestBuffer.getBuffer());
					int i = 0;
					for (i = 0; i < 20; i++) {
						if (testHash[i] != disk_manager.getPieceHash(pieceNumber)[i])
							break;
					}
					if (i >= 20) {
						//mark the piece as done..
						if (!pieceDone[pieceNumber]) {
							
							pieceDone[pieceNumber] = true;
							
							disk_manager.setRemaining( disk_manager.getRemaining() - length );
							
							disk_manager.computeFilesDone(pieceNumber);
						}
						return true;
					}
					if(pieceDone[pieceNumber]) {
					  pieceDone[pieceNumber] = false;
						disk_manager.setRemaining( disk_manager.getRemaining() + length );
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				return false;
			}
		
		
		 private byte[] computeMd5Hash(DirectByteBuffer buffer) {
		    md5.reset();
		    int position = buffer.position();
		    md5.update(buffer.getBuffer());
		    buffer.position(position);
		    md5Result.position(0);
		    md5.finalDigest(md5Result.getBuffer());
		    byte[] result = new byte[16];
		    md5Result.position(0);
		    for(int i = 0 ; i < result.length ; i++) {
		      result[i] = md5Result.get();
		    }    
		    return result;    
		  }
		  
		  private void MD5CheckPiece(int pieceNumber,boolean correct) {
		    PEPiece piece = disk_manager.getPeerManager().getPieces()[pieceNumber];
		    if(piece == null) {
		      return;
		    }
		    PEPeer[] writers = piece.getWriters();
		    int offset = 0;
		    for(int i = 0 ; i < writers.length ; i++) {
		      int length = piece.getBlockSize(i);
		      PEPeer peer = writers[i];
		      if(peer != null) {
		        DirectByteBuffer buffer = reader.readBlock(pieceNumber,offset,length);
		        byte[] hash = computeMd5Hash(buffer);
		        buffer.returnToPool();
		        buffer = null;
		        piece.addWrite(i,peer,hash,correct);        
		      }
		      offset += length;
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
		int	initial_buffer_position = buffer.position();

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
	
			//Now tempPiece points to the first file that contains data for this block
			while (buffer.hasRemaining()) {
				current_piece = pieceList.get(currentFile);
	
				if (current_piece.getFile().getAccessMode() == DiskManagerFileInfo.READ){
		
					LGLogger.log(0, 0, LGLogger.INFORMATION, "Changing " + current_piece.getFile().getName() + " to read/write");
						
					current_piece.getFile().setAccessMode( DiskManagerFileInfo.WRITE );
				}
				
				int realLimit = buffer.limit();
					
				long limit = buffer.position() + ((current_piece.getFile().getLength() - current_piece.getOffset()) - (offset - previousFilesLength));
	       
				if (limit < realLimit) {
					buffer.limit((int)limit);
				}
	
				if ( buffer.hasRemaining() ){

					current_piece.getFile().getCacheFile().write( buffer, fileOffset + (offset - previousFilesLength));
				}
					
				buffer.limit(realLimit);
				
				currentFile++;
				fileOffset = 0;
				previousFilesLength = offset;
			}
			
			buffer.returnToPool();
			
			return( true );
			
		}catch( Throwable e ){
			
			e.printStackTrace();
			
			String file_name = current_piece==null?"<unknown>":current_piece.getFile().getName();
						
			disk_manager.setErrorMessage( (e.getCause()!=null?e.getCause().getMessage():e.getMessage()) + " (dumpBlockToDisk) when processing file '" + file_name + "'" );
			
			LGLogger.logAlert( LGLogger.AT_ERROR, disk_manager.getErrorMessage() );
			
			buffer.position(initial_buffer_position);
			
			return( false );
		}
	}
  
	
	public void 
	writeBlock(
		int pieceNumber, 
		int offset, 
		DirectByteBuffer data,
		PEPeer sender) 
	{		
		global_write_queue_block_sem.reserve();
		
		// System.out.println( "reserved global write slot (buffer = " + data.limit() + ")" );
		
		synchronized( writeCheckQueueLock ){
			
			writeQueue.add(new QueueElement(pieceNumber, offset, data,sender));
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
		int size = data.remaining();
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
		if(!disk_manager.getPiecesDone()[pieceNumber]) {
		  LGLogger.log(0, 0, LGLogger.ERROR, "CHECKBLOCK2: pieceNumber="+pieceNumber+" not done");
		  return false;
		}
		return true;
	}



	public class DiskWriteThread extends Thread {
		private boolean bWriteContinue = true;

		public DiskWriteThread() {
			super("Disk Writer & Checker");
			setDaemon(true);
		}

		public void run() 
		{
			while (bWriteContinue){
				
				try{
					int	entry_count = writeCheckQueueSem.reserveSet( 64 );
					
					for (int i=0;i<entry_count;i++){
						
						QueueElement	elt;
						boolean			elt_is_write;
						
						synchronized( writeCheckQueueLock ){
							
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
						}
		
						if ( elt_is_write ){
							
								//Do not allow to write in a piece marked as done.
							
							int pieceNumber = elt.getPieceNumber();
							
							if(!disk_manager.getPiecesDone()[pieceNumber]){
								
							  if ( dumpBlockToDisk(elt)){
							  
							  	disk_manager.getPeerManager().blockWritten(elt.getPieceNumber(), elt.getOffset(),elt.getSender());
							  	
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
							
						  boolean correct = checkPiece(elt.getPieceNumber());

						  if(!correct){
						  	
						    MD5CheckPiece(elt.getPieceNumber(),false);
						    
						    LGLogger.log(0, 0, LGLogger.ERROR, "Piece " + elt.getPieceNumber() + " failed hash check.");
						    
						  }else{
						  	
						    LGLogger.log(0, 0, LGLogger.INFORMATION, "Piece " + elt.getPieceNumber() + " passed hash check.");
						    
						    if( disk_manager.getPeerManager().needsMD5CheckOnCompletion(elt.getPieceNumber())){
						    	
						      MD5CheckPiece(elt.getPieceNumber(),true);
						    }
						  }
		
						  disk_manager.getPeerManager().asyncPieceChecked(elt.getPieceNumber(), correct);
					  }
					}
				}catch( Throwable e ){
					
					e.printStackTrace();
					
					Debug.out( "DiskWriteThread: error occurred during processing: " + e.toString());
				}
        
			}
		}

		public void stopIt(){
			
			synchronized( writeCheckQueueLock ){
				
				bWriteContinue = false;
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
				
				QueueElement elt = (QueueElement)checkQueue.remove(0);
			}
		}
	}
	public class QueueElement {
		private int pieceNumber;
		private int offset;
		private DirectByteBuffer data;
    private PEPeer sender; 

		public QueueElement(int pieceNumber, int offset, DirectByteBuffer data, PEPeer sender) {
			this.pieceNumber = pieceNumber;
			this.offset = offset;
			this.data = data;
      this.sender = sender;
		}  

		public int getPieceNumber() {
			return this.pieceNumber;
		}

		public int getOffset() {
			return this.offset;
		}

		public DirectByteBuffer getData() {
			return this.data;
		}
    
    public PEPeer getSender() {
      return this.sender;
	}
	}


}
