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

public class PEPieceImpl
    implements PEPiece
{
	private static final LogIDs LOGID = LogIDs.PIECES;
	
	private final DiskManagerPiece	dmPiece;
	private final PEPeerManager		manager;
	
	private final int       nbBlocks;       // number of blocks in this piece
    private long            creationTime;
    
	private final String[]	requested;
	private final boolean[]	downloaded;
	private final String[] 	writers;
	private List 			writes;
	
	private String			reservedBy;	// using address for when they send bad/disconnect/reconnect
	
	//In end game mode, this limitation isn't used
    private int             speed;      //slower peers dont slow down fast pieces too much
    
    private int             resumePriority;
    
	// experimental class level lock
	protected static final AEMonitor 	class_mon	= new AEMonitor( "PEPiece:class");
	
    /** piece for tracking partially downloaded pieces
     * @param _manager the PEPeerManager
     * @param _dm_piece the backing dmPiece
     * @param _pieceSpeed the speed threshold for potential new requesters
     */
	public PEPieceImpl(
		PEPeerManager 		_manager, 
		DiskManagerPiece	_dm_piece,
        int                 _pieceSpeed)
	{
        creationTime =SystemTime.getCurrentTime();
		manager =_manager;
		dmPiece =_dm_piece;
        speed =_pieceSpeed;

		nbBlocks =dmPiece.getNbBlocks();

		requested =new String[nbBlocks];
        
        final boolean[] written =dmPiece.getWritten();
		if (written ==null)
			downloaded =new boolean[nbBlocks];
		else
			downloaded =(boolean[])written.clone();

        writers =new String[nbBlocks];
		writes =new ArrayList(0);
	}

    public DiskManagerPiece getDMPiece()
    {
        return dmPiece;
    }

    public long getCreationTime()
    {
        final long now =SystemTime.getCurrentTime();
        if (now >=creationTime &&creationTime >0)
            return creationTime;
        creationTime =now;
        return now;
    }
    
    public long getTimeSinceLastActivity()
    {
        final long now =SystemTime.getCurrentTime();
        final long lastWriteTime =dmPiece.getLastWriteTime(now);
        if (lastWriteTime >0 &&now >=lastWriteTime)
            return now -lastWriteTime;
        if (creationTime >0 &&now >=creationTime)
            return now -creationTime;
        creationTime =now;
        return 0;
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
	
	/** This flags the block at the given offset as having been downloaded
     * If all blocks are now downloaed, sets the dmPiece as downloaded
	 * @param blockNumber
	 */
	public void setDownloaded(int offset)
	{
		downloaded[offset /DiskManager.BLOCK_SIZE] =true;
        for (int i =0; i <nbBlocks; i++)
        {
            if (!downloaded[i])
                return;
        }
        dmPiece.setDownloaded();
        dmPiece.clearRequested();
	}
	
    /** This flags the block at the given offset as NOT having been downloaded
     * and the whole piece as not having been fully downloaded
     * @param blockNumber
     */
    public void clearDownloaded(int offset)
    {
        downloaded[offset /DiskManager.BLOCK_SIZE] =false;
        dmPiece.clearDownloaded();
    }
    
	/** This marks a given block as having been written by the given peer
	 * @param peer the PEPeer that sent the data
	 * @param blockNumber the block we're operating on
	 */
	public void setWritten(PEPeer peer, int blockNumber)
	{
		writers[blockNumber] =peer.getIp();
		dmPiece.setWritten(blockNumber);
	}
	
	/** This method clears the requested information for the given block
     * unless the block has already been downloaded, in which case the writer's
     * IP is recorded as a request for the block.
	 */
	public void clearRequested(int blockNumber)
	{
		requested[blockNumber] =downloaded[blockNumber] ?writers[blockNumber] :null;
	}
	
    /** @deprecated
     * This method is safe in a multi-threaded situation as the worst that it can do is mark a block as not requested even
     * though its downloaded which may lead to it being downloaded again
     */
    public void unmarkBlock(int blockNumber)
    {
        requested[blockNumber] =downloaded[blockNumber] ?writers[blockNumber] :null;
    }

	/** This will scan each block looking for requested blocks. For each one, it'll verify
	 * if the PEPeer for it still exists and is still willing and able to upload data.
	 * If not, it'll unmark the block as requested.
	 * @return int of how many were cleared (0 to nbBlocks)
	 */
	public int checkRequests()
	{
        if (getTimeSinceLastActivity() <30 *1000)
            return 0;
		int cleared =0;
		boolean nullPeer =false;
		for (int i =0; i <nbBlocks; i++)
		{
			if (!downloaded[i] &&!dmPiece.isWritten(i))
			{
				final String			requester =requested[i];
				final PEPeerTransport	pt;
				if (requester !=null)
				{
					pt =manager.getTransportFromAddress(requester);
					if (pt !=null)
					{
						pt.setSnubbed(true);
						if (!pt.isDownloadPossible())
						{
                            clearRequested(i);
							cleared++;
						}
					} else
					{
						nullPeer =true;
                        clearRequested(i);
						cleared++;
					}
				}
			}
		}
		if (cleared >0)
		{
			dmPiece.clearRequested();
            if (Logger.isEnabled())
                Logger.log(new LogEvent(dmPiece.getManager().getTorrent(), LOGID, LogEvent.LT_WARNING,
                        "checkRequests(): piece #" +getPieceNumber()+" cleared " +cleared +" requests."
                        + (nullPeer ?" Null peer was detected." :"")));
		}
		return cleared;
	}

	/** @return true if the piece has any blocks that are not;
	 *  Downloaded, Requested, or Written
	 */
	public boolean hasUnrequestedBlock()
	{
		final boolean[] written =dmPiece.getWritten();
		for (int i =0; i <nbBlocks; i++ )
		{
			if (!downloaded[i] &&requested[i] ==null &&(written ==null ||!written[i]))
				return true;
		}
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
	public int[] getAndMarkBlocks(PEPeer peer, int nbWanted)
	{
		final String ip =peer.getIp();
        final boolean[] written =dmPiece.getWritten();
		int blocksFound =0;
		// scan piece to find first free block
		for (int i =0; i <nbBlocks; i++)
		{
			while (blocksFound <nbWanted &&(i +blocksFound) <nbBlocks &&!downloaded[i +blocksFound]
			    &&requested[i +blocksFound] ==null &&(written ==null ||!written[i]))
			{
				requested[i +blocksFound] =ip;
				blocksFound++;
			}
			if (blocksFound >0)
				return new int[] {i, blocksFound};
		}
		return new int[] {-1, 0};
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
        final boolean[] written =dmPiece.getWritten();
        for (int i =0; i <nbBlocks; i++ )
        {
            if (!downloaded[i] &&requested[i] ==null &&(written ==null ||!written[i]))
                result++;
        }
        return result;
    }

	/**
	 * Assumption - single threaded with getAndMarkBlock
	 */
	public boolean setRequested(PEPeer peer, int blockNumber)
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
			
			int	length = dmPiece.getLength();
			
			if ((length % DiskManager.BLOCK_SIZE) != 0){
				
				return( length % DiskManager.BLOCK_SIZE );
			}
		}
		
		return DiskManager.BLOCK_SIZE;
	}
    
    public int getBlockNumber(int offset)
    {
        return offset /DiskManager.BLOCK_SIZE;
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
		final List result;
		try{
			class_mon.enter();
			
			result = new ArrayList(writes);
			
		}finally{
			
			class_mon.exit();
		}
		final Iterator iter = result.iterator();
		while(iter.hasNext()) {
			final PEPieceWriteImpl write = (PEPieceWriteImpl) iter.next();
			if(write.getBlockNumber() != blockNumber)
				iter.remove();
		}
		return result;
	}
	
	
	public List getPieceWrites(PEPeer peer) {
		final List result;
		try{
			class_mon.enter();
			
			result = new ArrayList(writes);
		}finally{
			class_mon.exit();
		}
		final Iterator iter = result.iterator();
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
		final List result;
		
		try{
			class_mon.enter();
			
			result = new ArrayList(writes);
			
		}finally{
			
			class_mon.exit();
		}
		
		final Iterator iter = result.iterator();
		
		while(iter.hasNext()) {
			
			final PEPieceWriteImpl write = (PEPieceWriteImpl) iter.next();
			
			if ( !write.getSender().equals( ip )){
				
				iter.remove();
			}
		}
		
		return result;
	}

	public void reset()
	{
		dmPiece.reset();
		for (int i =0; i <nbBlocks; i++)
		{
            requested[i] =null;
			downloaded[i] =false;
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
		dmPiece.reDownloadBlock(blockNumber);
	}

	/** finds all blocks downloaded by the given address
	 * and marks them up for re-downloading 
	 * @param address String
	 */
	public void reDownloadBlocks(String address)
	{
		for (int i =0; i <writers.length; i++ )
		{
			final String writer =writers[i];

			if (writer !=null &&writer.equals(address))
				reDownloadBlock(i);
		}
	}

	public void setResumePriority(int p)
	{
		resumePriority =p;
	}

	public int getResumePriority()
	{
		return resumePriority;
	}

    /**
     * @return int of availability in the swarm for this piece
     * @see org.gudy.azureus2.core3.peer.PEPeerManager.getAvailability(int pieceNumber)
     */
    public int getAvailability()
    {
        return manager.getAvailability(dmPiece.getPieceNumber());
    }

    /** This support method returns how many blocks have already been
     * written from the dmPiece
     * @return int from dmPiece.getNbWritten()
     * @see org.gudy.azureus2.core3.disk.DiskManagerPiece.getNbWritten()
     */
    public int getNbWritten()
    {
        return dmPiece.getNbWritten();
    }
    
    /** This support method returns the dmPiece's written array
     * @return boolean[] from the dmPiece
     * @see org.gudy.azureus2.core3.disk.DiskManagerPiece.getWritten()
     */
    public boolean[] getWritten()
    {
        return dmPiece.getWritten();
    }
    public boolean isWritten()
    {
        return dmPiece.isWritten();
    }
    
	public int getPieceNumber()
	{
		return dmPiece.getPieceNumber();
	}

	public int getLength()
	{
		return dmPiece.getLength();
	}

	public void setRequestable()
	{
		dmPiece.setRequestable();
	}
	
	public boolean isChecking()
	{
		return dmPiece.isChecking();
	}

/*
    public boolean isWritten()
    {
        return dmPiece.isWritten();
    }

    public void clearChecking()
    {
        dmPiece.clearChecking();
    }

	public int getNbWritten()
	{
		return dmPiece.getNbWritten();
	}
	
	public boolean isRequestable()
	{
		return dmPiece.isRequestable();
	}

	public void setChecking(boolean b)
	{
		dmPiece.setChecking(b);
	}

	public boolean isNeeded()
	{
		return dmPiece.isNeeded();
	}
*/
}