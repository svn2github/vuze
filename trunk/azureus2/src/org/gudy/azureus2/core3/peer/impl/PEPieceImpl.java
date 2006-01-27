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

import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerPiece;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.SystemTime;

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
		downloaded =new boolean[nbBlocks];
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

//	public int calcAvailability()
//	{
//		if (manager ==null)
//			return 0;
//		return manager.calcAvailability(dm_piece.getPieceNumber());
//	}

	public int getAvailability()
	{
		if (manager !=null)
			return manager.getAvailability(dm_piece.getPieceNumber());
		return 0;
	}

	public int getNbWritten()
	{
		return dm_piece.getNbWritten();
	}
	
	public boolean isRequested(int blockNumber)
	{
		return requested[blockNumber] !=null;
	}

	public boolean isDownloaded(int blockNumber)
	{
		return downloaded[blockNumber];
	}

	public boolean[] getWritten()
	{
		return dm_piece.getWritten();
	}

	public void setBlockWritten(int blockNumber)
	{
		downloaded[blockNumber] =true;
	}

	public void setWritten(PEPeerTransport peer,int blockNumber)
	{
		writers[blockNumber] =peer.getIp();
		dm_piece.setBlockWritten(blockNumber);
	}
	
	// This method is used to clear the requested information
	public void clearRequested(int blockNumber)
	{
		requested[blockNumber] =null;
	}

	// determines if a piece has any unrequested blocks
	public boolean hasUnrequestedBlock()
	{
		for (int i =0; i <nbBlocks; i++ )
		{
			if (!downloaded[i] &&requested[i] ==null &&!dm_piece.isWritten(i))
				return true;
		}
		// this should have only been called because the piece was believed to have free blocks
		// mark it as not having any free blocks now
		dm_piece.setRequested();
		return false;
	}

	/**
	 * This method scans a piece for the first unrequested block.  Upon finding it,
	 * it counts how many are unrequested up to the quantity of want.
	 * The blocks are marked as requested by the PEPeer
	 * will mark it as requested, unless it's been too long since
	 * anything was written to the piece, in which case it'll return
	 * the first non-written block so it can be re-requested
	 * Assumption - single threaded access to this
	 */
	public int[] getAndMarkBlocks(PEPeerTransport peer, int wants)
	{
		int blocksFound =0;
		// scan piece to find first free block
		for (int i =0; i <nbBlocks; i++)
		{
			while (blocksFound <=wants &&(i +blocksFound) <nbBlocks &&!downloaded[i +blocksFound] &&requested[i +blocksFound] ==null &&!dm_piece.isWritten(i +blocksFound))
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
		if ( blockNumber == (downloaded.length - 1)){
			
			int	length = dm_piece.getLength();
			
			if ((length % DiskManager.BLOCK_SIZE) != 0){
				
				return( length % DiskManager.BLOCK_SIZE );
			}
		}
		
		return DiskManager.BLOCK_SIZE;
	}
	
	public int getNbBlocks()
	{
		return downloaded.length;
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

		reservedBy =null;
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

	public void decSpeed()
	{
		if (speed >0)
			speed -=1;
	}

	public void incSpeed()
	{
		speed +=1;
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
		this.reservedBy =peer;
	}

	public String getReservedBy()
	{
		return this.reservedBy;
	}

	public void reDownloadBlock(int blockNumber)
	{
		downloaded[blockNumber] =false;
		requested[blockNumber] =null;
		dm_piece.reDownloadBlock(blockNumber);
	}

	public void reDownloadBlocks(String culprit)
	{
		for (int i =0; i <writers.length; i++ )
		{
			String writer =writers[i];

			if (writer !=null &&writer.equals(culprit))
				reDownloadBlock(i);
		}
	}

	public long getCreationTime()
	{
		long now =SystemTime.getCurrentTime();
		if (now >=creation_time)
		{
			return creation_time;
		}
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

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.core3.peer.PEPiece#getResumePriority()
	 */
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