/*
 * File    : PEPiece
 * Created : 15-Oct-2003
 * By      : Olivier
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

import org.gudy.azureus2.core3.disk.DiskManagerPiece;
import org.gudy.azureus2.core3.peer.impl.PEPeerTransport;

import com.aelitis.azureus.core.util.Piece;

/**
 * Represents a Peer Piece and the status of its different blocks (un-requested, requested, downloaded, written).
 * 
 * @author Olivier
 * @author MjrTom
 *			2005/Oct/08: various changes to support new piece-picking
 *			2006/Jan/2: refactoring, mostly to base Piece interface
 */

public interface PEPiece
	extends Piece
{  
	public PEPeerManager	getManager();

	/**
	 * record details of a piece's blocks that have been completed for bad peer detection purposes
	 * @param blockNumber
	 * @param sender
	 * @param hash
	 * @param correct
	 */
	public void 
	addWrite(
		int blockNumber,
		String sender, 
		byte[] hash,
		boolean correct	);

	public DiskManagerPiece	getDMPiece();
	public int			getNbWritten();

	public int			getAvailability();

	public boolean		hasUnrequestedBlock();
	public int[]		getAndMarkBlocks(PEPeerTransport peer, int wants);
	public boolean		markBlock(PEPeerTransport peer, int blockNumber);
	public void			unmarkBlock(int blocNumber);
	
	public int			getNbRequests();
	public int			getNbUnrequested();
	public int			checkRequests();

	public int			getBlockSize(int blockNumber);

	public long			getCreationTime();

	public boolean		isDownloaded(int blockNumber);   
	public boolean		isRequested(int blockNumber);

	public int			getPieceNumber();

	//A Piece can be reserved by a peer, so that only s/he can
	//contribute to it.
	public String		getReservedBy();
	public void			setReservedBy(String peer);

	/**
	 * @return long ResumePriority (startPriority + resuming adjustments)
	 */
	public long			getResumePriority();
	/**
	 * @param p the Resume Priority to set, for display purposes
	 */
	public void			setResumePriority(long p);

	public String[] 	getWriters();
	public void			setWritten(PEPeerTransport peer, int blockNumber);

	public int 			getSpeed();
	public void			setSpeed(int speed);
	public void			incSpeed();
	public void			decSpeed();
}