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

/**
 * @author parg
 *
 */
 
public interface 
PEPeerManager 
{
	public static final int BLOCK_SIZE = 16384;
 		
	public void peerAdded(PEPeer pc);

	public void peerRemoved(PEPeer pc);
	
	public void
	stopAll();
		
	public byte[]
	getHash();
	
	public byte[]
	getPeerId();
	
	public void blockWritten(int pieceNumber, int offset);
	
	public void pieceChecked(int pieceNumber, boolean result);

	public int[] getAvailability();
	
	public boolean[] getPiecesStatus();

	public PEPiece[] getPieces();

	public PEPeerStats
	getStats();

	public void 
	checkTracker();

	public String getTrackerStatus();
	
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
