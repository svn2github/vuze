/*
 * File    : PEPeerManager
 * Created : 5 Oct. 2003
 * By      : parg 
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

package org.gudy.azureus2.core3.peer;

import java.util.List;

import java.nio.ByteBuffer;

import org.gudy.azureus2.core3.disk.*;

/**
 * @author stuff
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public interface 
PEPeerManager 
{
	public static final int BLOCK_SIZE = 16384;
 
		
	public void peerAdded(PEPeer pc);

	public void peerRemoved(PEPeer pc);
	
	public boolean validateHandshaking(PEPeer pc, byte[] peerId);

	public void
	stopAll();
	
	public PEPeerStats
	createPeerStats();
	
	public byte[]
	getHash();
	
	public byte[]
	getPeerId();
	
	public void blockWritten(int pieceNumber, int offset);

	public boolean checkBlock(int pieceNumber, int offset, int length);
	public boolean checkBlock(int pieceNumber, int offset, ByteBuffer data);
	public void enqueueReadRequest(DiskManagerDataQueueItem item);
	public void writeBlock(int pieceNumber, int offset, ByteBuffer data);
	public void requestCanceled(DiskManagerRequest request);
 	
	public void freeRequest(DiskManagerDataQueueItem item);

	public int[] getAvailability();
	
	public boolean[] getPiecesStatus();

	public void pieceChecked(int pieceNumber, boolean result);

	public PEPiece[] getPieces();
	public void havePiece(int pieceNumber, PEPeer pcOrigin);
	
	public boolean isOptimisticUnchoke(PEPeer pc);

	public PEPeerStats
	getStats();

	public void 
	checkTracker();

	public void discarded( int i );
	public void	received( int i );
	public void	sent( int i );
	
	public void haveNewPiece();

	public String getTrackerStatus();

	public List get_connections();
	
	public int getAvailability(int pieceNumber);

	public int getNbPeers();

	public int getNbSeeds();
	
	public int getNbHashFails();

	public int getPiecesNumber();

	public int getPieceLength();

	public String getUploaded();
	public String getTotalSpeed();
	public int getTrackerTime();

	public long downloaded();

	public long uploaded();
	
	public long getRemaining();

	public int getDownloadPriority();

	public String getETA();

	public String getElpased();
}
