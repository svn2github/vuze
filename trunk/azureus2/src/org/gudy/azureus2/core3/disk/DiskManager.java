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
 
 
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.download.DownloadManager;

import com.aelitis.azureus.core.diskmanager.ReadRequestListener;
 
 public interface
 DiskManager
 {
	public static final int INITIALIZING = 1;
	public static final int ALLOCATING = 2;
	public static final int CHECKING = 3;
	public static final int READY = 4;
	public static final int FAULTY = 10;

	/**
	 * Start checking/allocating
	 */
	public void
	start();
	
	/**
	  * @return whether all files exist and sizes match
	  */
	public boolean
	filesExist();

	public void 
	setPeerManager(
		PEPeerManager manager );

	public void 
	writeBlock(
		int 		pieceNumber, 
		int 		offset, 
		DirectByteBuffer 	data,
    PEPeer sender);

	public boolean 
	checkBlock(
		int 		pieceNumber, 
		int 		offset, 
		DirectByteBuffer 	data );

	public boolean 
	checkBlock(
		int pieceNumber, 
		int offset, 
		int length );
		
	public DiskManagerRequest
	createRequest(
		int pieceNumber,
		int offset,
		int length );
	
  /**
   * Enqueue an async disk read request.
   * @param request
   * @param listener
   */
	public void enqueueReadRequest( DiskManagerRequest request, ReadRequestListener listener );

	public void
	stopIt();

	public void
    dumpResumeDataToDisk(
    	boolean savePartialPieces, 
		boolean invalidate );

	public void
	computePriorityIndicator();
	
	public PEPiece[] 
	getRecoveredPieces();

	public void
	aSyncCheckPiece(
		int	pieceNumber );
  
	public int 
	getPieceNumberToDownload(
		boolean[] 	_piecesRarest );
	
	public boolean[] 
	getPiecesDone();

	public int 
	getNumberOfPieces();

	public DiskManagerFileInfo[]
	getFiles();

	public String[][] 
	getFilesStatus();
 
	public int
	getState();
	
	public String
	getPath();
	
	public String
	getFileName();
	
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

	public boolean isChecking();
  
   
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
  
  public DownloadManager getDownloadManager();
  
  public PEPeerManager getPeerManager();

 }