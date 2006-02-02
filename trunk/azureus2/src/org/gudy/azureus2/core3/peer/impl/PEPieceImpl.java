/*
 * File    : PEPieceImpl.java
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

package org.gudy.azureus2.core3.peer.impl;

/**
 * @author parg
 * @author MjrTom
 *			2005/Oct/08: numerous changes for new piece-picking
 *			2006/Jan/02: refactoring piece picking to elsewhere, and consolidations
 */

import java.util.*;

import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.util.*;

public class 
PEPieceImpl
implements PEPiece
{
	final private DiskManagerPiece	dm_piece;
	public PEPeerManager		manager;

	private long		creation_time;
	private int			nbBlocks;		// number of blocks in this piece

	private final String[]	requested;
	private final boolean[]	downloaded;
	private final String[] 	writers;
	private List 			writes;

	private String			reservedBy;	// using address for when they send bad/disconnect/reconnect

	//In end game mode, this limitation isn't used
	private int			speed;			//slower peers dont slow down fast pieces too much

	private long	resumePriority;
	
	// experimental class level lock
	protected static AEMonitor 	class_mon	= new AEMonitor( "PEPiece:class");

	public PEPieceImpl(
		PEPeerManager 		_manager, 
		DiskManagerPiece	_dm_piece,
		int					_pieceSpeed,
		boolean				_recovered )
	{
		manager =_manager;
		dm_piece =_dm_piece;

		nbBlocks =dm_piece.getNbBlocks();

		requested =new String[nbBlocks];

		if (dm_piece.getWritten() ==null)
			downloaded =new boolean[nbBlocks];
		else
			downloaded =(boolean[])dm_piece.getWritten().clone();
		writers =new String[nbBlocks];
		writes =new ArrayList(0);

		creation_time =SystemTime.getCurrentTime();
		if (!_recovered)
		{
			speed =_pieceSpeed;
			dm_piece.setLastWriteTime();
		} else
			speed =0;
	}

	/**
	 * @return int of availability in the swarm for this piece
	 * @see org.gudy.azureus2.core3.peer.PEPeerManager.getAvailability(int pieceNumber)
	 */
	public int getAvailability()
	{
		if (manager !=null)
			return manager.getAvailability(dm_piece.getPieceNumber());
		return 0;
	}

	/** This support method returns how many blocks have already been
	 * written from the dmPiece
	 * @return int from dmPiece.getNbWritten()
	 * @see org.gudy.azureus2.core3.disk.DiskManagerPiece.getNbWritten()
	 */
	public int getNbWritten()
	{
		return dm_piece.getNbWritten();
	}
	
	/** Tells if a block has been requested
	 * @param blockNumber the block in question
	 * @return true if the block is Requested already
	 */
	public boolean isRequested(int blockNumber)
	{
		return requested[blockNumber] !=null;
	}

	/** Tells if a block has been downloaded
	 * @param blockNumber the block in question
	 * @return true if the block is downloaded already
	 */
	public boolean isDownloaded(int blockNumber)
	{
		return downloaded[blockNumber];
	}

	/** This is a support method to return the dmPiece's written array
	 * @return boolean[] from the dmPiece
	 * @see com.aelitis.azureus.core.util.Piece.getWritten()
	 */
	public boolean[] getWritten()
	{
		return dm_piece.getWritten();
	}

	/** This flags the given block as having been downloaded
	 * @param blockNumber
	 */
	public void setBlockWritten(int blockNumber)
	{
		downloaded[blockNumber] =true;
	}

	/** This marks a given block as having been written by the given peer
	 * @param peer the PEPeerTransport that sent the data
	 * @param blockNumber the block we're operating on
	 */
	public void setWritten(PEPeerTransport peer, int blockNumber)
	{
		writers[blockNumber] =peer.getIp();
		dm_piece.setBlockWritten(blockNumber);
	}
	
	/** This method clear the requested information for the given block
	 */
	public void clearRequested(int blockNumber)
	{
		requested[blockNumber] =null;
	}

	/** This will scan each block looking for requested blocks. For each one, it'll verify
	 * if the PEPeer for it still exists and is still willing and able to upload data.
	 * If not, it'll unmark the block as requested. This should probably only be called
	 * after a piece has timed out, for performance
	 * @return int of how many were cleared (0 to nbBlocks)
	 */
	public int checkRequests()
	{
		int cleared =0;
		for (int i =0; i <nbBlocks; i++)
		{
			if (!downloaded[i] &&!dm_piece.isWritten(i))
			{
				final String			requester =requested[i];
				final PEPeerTransport	pt;
				if (requester !=null)
				{
					pt =manager.getTransportFromAddress(requester);
					if (pt !=null)
					{
						if (!pt.isSnubbed())
							pt.setSnubbed(true);
						if (!pt.isDownloadPossible())
						{
							requested[i] =null;
							cleared++;
						}
					} else
					{
						final LogIDs LOGID = LogIDs.PEER;
                        if (Logger.isEnabled())
                                Logger.log(new LogEvent(dm_piece.getManager().getTorrent(), LOGID, LogEvent.LT_WARNING,
                                        "Piece:"+getPieceNumber()+" Chunk:"+i+"; Peer doesn't exist:"+requested[i]));
						requested[i] =null;
						cleared++;
					}
				}
			}
		}
		if (cleared >0)
			dm_piece.clearRequested();
		return cleared;
	}

	/** This method presumes the caller believes the piece to probably have
	 * requestable blocks.  As such, this method marks it as Requested when
	 * no blocks are found.
	 *  @return true if the piece has any blocks that are not;
	 *  Downloaded, Requested, and Written
	 */
	public boolean hasUnrequestedBlock()
	{
		for (int i =0; i <nbBlocks; i++ )
		{
			if (!downloaded[i] &&requested[i] ==null &&!dm_piece.isWritten(i))
				return true;
		}
		// this should have only been called if piece was believed to have free blocks
		// mark it as not having any free blocks now
		dm_piece.setRequested();
		return false;
	}

	/**
	 * This method scans a piece for the first unrequested block.  Upon finding it,
	 * it counts how many are unrequested up to nbWanted.
	 * The blocks are marked as requested by the PEPeer
	 * Assumption - single threaded access to this
	 * TODO: this should return the largest span equal or smaller than nbWanted
	 * OR, probably a different method should do that, so this one can support 'more sequential' picking
	 */
	public int[] getAndMarkBlocks(PEPeerTransport peer, int nbWanted)
	{
		int blocksFound =0;
		// scan piece to find first free block
		for (int i =0; i <nbBlocks; i++)
		{
			while (blocksFound <=nbWanted &&(i +blocksFound) <nbBlocks &&!downloaded[i +blocksFound] &&requested[i +blocksFound] ==null &&!dm_piece.isWritten(i +blocksFound))
			{
				requested[i +blocksFound] =peer.getIp();
				blocksFound++;
			}
			if (blocksFound >0)
				return new int[] {i, blocksFound};
		}
		return new int[] {-1, 0};
	}

	/**
	 * This method is safe in a multi-threaded situation as the worst that it can do is mark a block as not requested even
	 * though its downloaded which may lead to it being downloaded again
	 */
	public void unmarkBlock(int blockNumber)
	{
		requested[blockNumber] =downloaded[blockNumber] ?writers[blockNumber] :null;
	}

	/**
	 * Assumption - single threaded with getAndMarkBlock
	 */
	public boolean markBlock(PEPeerTransport peer, int blockNumber)
	{
		if (!downloaded[blockNumber])
		{
			requested[blockNumber] =peer.getIp();
			return true;
		}
		return false;
	}
	
	public int 
	getBlockSize(
		int blockNumber) 
	{
		if ( blockNumber == (nbBlocks - 1)){
			
			int	length = dm_piece.getLength();
			
			if ((length % DiskManager.BLOCK_SIZE) != 0){
				
				return( length % DiskManager.BLOCK_SIZE );
			}
		}
		
		return DiskManager.BLOCK_SIZE;
	}
	
	public int getNbBlocks()
	{
		return nbBlocks;
	}

	public List getPieceWrites()
	{
		List result;
		try{
			class_mon.enter();
			
			result = new ArrayList(writes);
		}finally{
			
			class_mon.exit();
		}
		return result;
	}
	
	
	public List getPieceWrites(int blockNumber) {
		List result;
		try{
			class_mon.enter();
			
			result = new ArrayList(writes);
			
		}finally{
			
			class_mon.exit();
		}
		Iterator iter = result.iterator();
		while(iter.hasNext()) {
			PEPieceWriteImpl write = (PEPieceWriteImpl) iter.next();
			if(write.getBlockNumber() != blockNumber)
				iter.remove();
		}
		return result;
	}
	
	
	public List getPieceWrites(PEPeer peer) {
		List result;
		try{
			class_mon.enter();
			
			result = new ArrayList(writes);
		}finally{
			class_mon.exit();
		}
		Iterator iter = result.iterator();
		while(iter.hasNext()) {
			PEPieceWriteImpl write = (PEPieceWriteImpl) iter.next();
			if(peer == null || ! peer.equals(write.getSender()))
				iter.remove();
		}
		return result;
	}
	
	public List 
	getPieceWrites( 
		String	ip ) 
	{
		List result;
		
		try{
			class_mon.enter();
			
			result = new ArrayList(writes);
			
		}finally{
			
			class_mon.exit();
		}
		
		Iterator iter = result.iterator();
		
		while(iter.hasNext()) {
			
			PEPieceWriteImpl write = (PEPieceWriteImpl) iter.next();
			
			if ( !write.getSender().equals( ip )){
				
				iter.remove();
			}
		}
		
		return result;
	}

	public void reset()
	{
		dm_piece.reset();

		for (int i =0; i <nbBlocks; i++)
		{
			downloaded[i] =false;
			requested[i] =null;
			writers[i] =null;
		}

//		reservedBy =null;
	}

	protected void addWrite(PEPieceWriteImpl write) {
		try{
			class_mon.enter();
			
			writes.add(write);
			
		}finally{
			
			class_mon.exit();
		}
	}
	
	public void 
	addWrite(
		int blockNumber,
		String sender, 
		byte[] hash,
		boolean correct	)
	{
		addWrite( new PEPieceWriteImpl( blockNumber, sender, hash, correct ));
	}

	public String[] getWriters()
	{
		return writers;
	}

	public int getSpeed()
	{
		return speed;
	}

	public void setSpeed(int newSpeed)
	{
		speed =newSpeed;
	}

	/** @deprecated
	*/
	public void decSpeed()
	{
		if (speed >0)
			speed--;
	}

	public void incSpeed()
	{
		speed++;
	}

	/**
	 * @return Returns the manager.
	 */
	public PEPeerManager getManager()
	{
		return manager;
	}

	public void setReservedBy(String peer)
	{
		reservedBy =peer;
	}

	public String getReservedBy()
	{
		return reservedBy;
	}

	/** for a block that's already downloadedt, mark up the piece
	 * so that the block will get downloaded again.  This is used
	 * when the piece fails hash-checking.
	 */
	public void reDownloadBlock(int blockNumber)
	{
		downloaded[blockNumber] =false;
		requested[blockNumber] =null;
		dm_piece.reDownloadBlock(blockNumber);
	}

	/** finds all blocks downloaded by the given address
	 * and marks them up for re-downloading 
	 * @param address String
	 */
	public void reDownloadBlocks(String address)
	{
		for (int i =0; i <writers.length; i++ )
		{
			String writer =writers[i];

			if (writer !=null &&writer.equals(address))
				reDownloadBlock(i);
		}
	}

	public long getCreationTime()
	{
		long now =SystemTime.getCurrentTime();
		if (now >=creation_time &&creation_time >0)
			return creation_time;
		creation_time =now;
		return creation_time;
	}

	public int getNbRequests()
	{
		int result =0;
		for (int i =0; i <nbBlocks; i++)
		{
			if (!downloaded[i] &&requested[i] !=null)
				result++;
		}
		return result;
	}

	public int getNbUnrequested()
	{
		int result =0;
		for (int i =0; i <nbBlocks; i++)
		{
			if (!downloaded[i] &&requested[i] ==null)
				result++;
		}
		if (result ==0)
			dm_piece.setRequested();
		else
			dm_piece.clearRequested();
		return result;
	}

	public DiskManagerPiece getDMPiece()
	{
		return dm_piece;
	}

	public void setResumePriority(long p)
	{
		resumePriority =p;
	}

	public long getResumePriority()
	{
		return resumePriority;
	}

	public int getPieceNumber()
	{
		return dm_piece.getPieceNumber();
	}

	public int getLength()
	{
		return dm_piece.getLength();
	}

	public void clearChecking()
	{
		dm_piece.clearChecking();
	}

	public boolean isWritten()
	{
		return dm_piece.isWritten();
	}

	public void setRequestable()
	{
		dm_piece.setRequestable();
	}
	
	public boolean isChecking()
	{
		return dm_piece.isChecking();
	}

/*
	public int getNbWritten()
	{
		return dm_piece.getNbWritten();
	}
	
	public boolean isRequestable()
	{
		return dm_piece.isRequestable();
	}

	public void setChecking(boolean b)
	{
		dm_piece.setChecking(b);
	}

	public boolean isNeeded()
	{
		return dm_piece.isNeeded();
	}
*/
}