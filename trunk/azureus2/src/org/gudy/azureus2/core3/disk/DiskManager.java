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
 
 import java.nio.ByteBuffer;
 
 import org.gudy.azureus2.core3.peer.*;
 
 public interface
 DiskManager
 {
	public static final int INITIALIZING = 1;
	public static final int ALLOCATING = 2;
	public static final int CHECKING = 3;
	public static final int READY = 4;
	public static final int FAULTY = 10;

	public void 
	setPeerManager(
		PEPeerManager manager );

	public void 
	writeBlock(
		int 		pieceNumber, 
		int 		offset, 
		ByteBuffer 	data,
    PEPeer sender);

	public boolean 
	checkBlock(
		int 		pieceNumber, 
		int 		offset, 
		ByteBuffer 	data );

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
		
	public DiskManagerDataQueueItem
	createDataQueueItem(
		DiskManagerRequest	request );
		
	public void 
	enqueueReadRequest(
		DiskManagerDataQueueItem item );

	public void
	stopIt();

	public void
	dumpResumeDataToDisk(
		boolean	savePartialPieces );

	public void
	computePriorityIndicator();
	
	public PEPiece[] 
	getPieces();

	public void
	aSyncCheckPiece(
		int	pieceNumber );
  
	public int 
	getPiecenumberToDownload(
		boolean[] 	_piecesRarest );
	
	public boolean[] 
	getPiecesStatus();

	public int 
	getPiecesNumber();

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
  
   public String moveCompletedFiles();
 }