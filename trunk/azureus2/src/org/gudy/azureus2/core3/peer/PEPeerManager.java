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

import org.gudy.azureus2.core3.tracker.client.*;
 
public interface 
PEPeerManager 
{
	public static final int PS_INITIALISED	= 1;
	public static final int PS_DOWNLOADING	= 2;
	public static final int PS_SEEDING		= 3;
	public static final int PS_STOPPED		= 4;
	
	public static final int BLOCK_SIZE = 16384;

	public void
	start();
		
	public void
	stopAll();
		
	public byte[]
	getHash();
	
	public byte[]
	getPeerId();
	
	public void 
	blockWritten(
		int pieceNumber, 
		int offset,
    PEPeer peer);
	
	public void 
	pieceChecked(
		int pieceNumber, 
		boolean result );

	public int[] getAvailability();

	public int getAvailability(int pieceNumber);
	
	public boolean[] getPiecesStatus();

	public PEPiece[] getPieces();

	public PEPeerManagerStats
	getStats();

	public void
	processTrackerResponse(
		TRTrackerResponse	response );
		
	public void 
	checkTracker(
		boolean	force );

	public String getTrackerStatus();
	
	public int getNbPeers();

	public int getNbSeeds();
	
	public int getNbHashFails();
  
	public void setNbHashFails(int fails);

	public int getPiecesNumber();

	public int getPieceLength();
	
	public int getTrackerTime();
	
	public long getRemaining();

	public int getDownloadPriority();

	public long getETA();

	public String getElapsedTime();
	
	public void
	addListener(
		PEPeerManagerListener	l );
		
	public void
	removeListener(
		PEPeerManagerListener	l );
}
