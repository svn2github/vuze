/*
 * File    : DiskManagerImpl.java
 * Created : 18-Oct-2003
 * By      : stuff
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
 
package org.gudy.azureus2.core3.disk;
 
 
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.download.DownloadManager;

 
public interface
DiskManager
{
	public static final int INITIALIZING = 1;
	public static final int ALLOCATING = 2;
	public static final int CHECKING = 3;
	public static final int READY = 4;
	public static final int FAULTY = 10;
	
	
	public static final int BLOCK_SIZE = 16384;

	/**
	 * Start checking/allocating
	 */
	public void
	start();
	
	public void
	stop();


	/**
	  * @return whether all files exist and sizes match
	  */
	
	public boolean
	filesExist();

	public DirectByteBuffer 
	readBlock(
		int pieceNumber, 
		int offset, 
		int length );
	
		/**
		 * enqueue an async write request
		 * @param pieceNumber
		 * @param offset
		 * @param data
		 * @param user_data	this will be provided to the listener when called back
		 * @param listener
		 */
	
	public void 
	enqueueWriteRequest(
		int 							pieceNumber, 
		int 							offset, 
		DirectByteBuffer 				data,
		Object 							user_data,
		DiskManagerWriteRequestListener	listener );


	public DiskManagerReadRequest
	createReadRequest(
		int pieceNumber,
		int offset,
		int length );
	
		  /**
		   * Enqueue an async disk read request.
		   * @param request
		   * @param listener
		   */
	
	public void 
	enqueueReadRequest( 
		DiskManagerReadRequest 			request, 
		DiskManagerReadRequestListener listener );

		/**
		 * enqueue an asynchronous single piece check
		 * @param pieceNumber
		 * @param listener
		 * @param user_data
		 */
	
	public void
	enqueueCheckRequest(
		int								pieceNumber,
		DiskManagerCheckRequestListener	listener,
		Object							user_data );
  
	
		/**
		 * recheck the entire torrent asynchronously, reporting each piece to the listener
		 * @param listener
		 * @param user_data
		 */
	
	public void
	enqueueCompleteRecheckRequest(
		DiskManagerCheckRequestListener	listener,
		Object							user_data );
	
	public void
    dumpResumeDataToDisk(
    	boolean savePartialPieces, 
		boolean force_recheck )
		
		throws Exception;

	
	public void
	computePriorityIndicator();
	
	public DiskManagerPiece[] 
	getPieces();

	public int 
	getPieceNumberToDownload(
		boolean[] 	_piecesRarest );
	
	public int 
	getNumberOfPieces();

	public DiskManagerFileInfo[]
	getFiles();

	public int
	getState();
	
	public long
	getTotalLength();
	
	public int
	getPieceLength();
	
	public int 
	getLastPieceLength();

	public long
	getRemaining();
	
	public int
	getPercentDone();
	
	public String
	getErrorMessage();
  
	public String
	moveCompletedFiles();

		/**
		 * returns true if a complete recheck is in progress
		 * @return
		 */
	
	public boolean 
	isChecking();
  
		/**
		 * method for checking that the block details are sensible
		 * @param pieceNumber
		 * @param offset
		 * @param data
		 * @return
		 */
	
	public boolean 
	checkBlockConsistency(
		int 				pieceNumber, 
		int 				offset, 
		DirectByteBuffer 	data );

		/**
		 * method for checking that the block details are sensible
		 * @param pieceNumber
		 * @param offset
		 * @param length
		 * @return
		 */
	
	public boolean 
	checkBlockConsistency(
		int pieceNumber, 
		int offset, 
		int length );
		
   
	public void
	addListener(
		DiskManagerListener	l );
	
	public void
	removeListener(
		DiskManagerListener	l );
  
  
  /**
   * Save the individual file priorities map to
   * DownloadManager.getData( "file_priorities" ).
   */

	public void storeFilePriorities();
	
	public DownloadManager 
	getDownloadManager();
}