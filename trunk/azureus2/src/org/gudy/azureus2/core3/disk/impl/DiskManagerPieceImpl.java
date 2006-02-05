/*
 * Created on 08-Oct-2004
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.core3.disk.impl;

/**
 * @author parg
 * @author MjrTom
 *			2005/Oct/08: startPriority/resumePriority handling and minor clock fixes
 *			2006/Jan/02: refactoring, change booleans to statusFlags
 */

import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.disk.impl.piecemapper.DMPieceList;
import org.gudy.azureus2.core3.util.SystemTime;

public class DiskManagerPieceImpl
	implements DiskManagerPiece
{
	private final DiskManagerImpl	diskManager;
	private final int				pieceNumber;
	private volatile int			statusFlags;

	private volatile long			time_last_write;
	// to save memory the "written" field is only maintained for pieces that are
	// downloading. A value of "null" means that either the piece hasn't started 
	// download or that it is complete.
	// access to "written" is single-threaded (by the peer manager) apart from when
	// the disk manager is saving resume data.
	// actually this is not longer strictly true, as setDone is called asynchronously
	// however, this issue can be worked around by working on a reference to the written data
	// as problems only occur when switching from all-written to done=true, both of which signify
	// the same state of affairs.
	protected boolean[]	written;

	protected DiskManagerPieceImpl(DiskManagerImpl _disk_manager, int pieceIndex)
	{
		diskManager =_disk_manager;
		pieceNumber =pieceIndex;
        statusFlags =PIECE_STATUS_NEEDED;
	}

	public DiskManager getManager()
	{
		return diskManager;
	}

	public int getPieceNumber()
	{
		return pieceNumber;
	}

	/**
	 * @return int number of bytes in the piece
	 */
	public int getLength()
	{
		if (pieceNumber !=diskManager.getNbPieces() -1)
			return (diskManager.getPieceLength());
		return (diskManager.getLastPieceLength());
	}

	public int getNbBlocks()
	{
		return ((getLength() +DiskManager.BLOCK_SIZE -1) /DiskManager.BLOCK_SIZE);
	}

    public int getBlockSize(int blockNumber)
    {
        if (blockNumber ==(getNbBlocks() -1))
        {
            final int length =getLength();
            if ((length %DiskManager.BLOCK_SIZE) !=0)
                return (length %DiskManager.BLOCK_SIZE);
        }
        return DiskManager.BLOCK_SIZE;
    }
    
	public boolean isNeeded()
	{
		return (statusFlags &PIECE_STATUS_NEEDED) !=0;
	}

	public boolean calcNeeded()
	{
		boolean filesNeeded =false;
		final DMPieceList pieceList =diskManager.getPieceList(pieceNumber);
		for (int i =0; i <pieceList.size(); i++)
		{
			DiskManagerFileInfoImpl file =pieceList.get(i).getFile();
			long fileLength =file.getLength();
			filesNeeded |=fileLength >0 &&file.getDownloaded() <fileLength &&!file.isSkipped();
		}
		if (filesNeeded)
		{
			statusFlags |=PIECE_STATUS_NEEDED;
			return true;
		}
		statusFlags &=~PIECE_STATUS_NEEDED;
		return false;
	}

	public void clearNeeded()
	{
		statusFlags &=~PIECE_STATUS_NEEDED;
	}

	public void setNeeded()
	{
		statusFlags |=PIECE_STATUS_NEEDED;
	}

	public void setNeeded(boolean b)
	{
		if (b)
			setNeeded();
		else
			clearNeeded();
	}

	public boolean isAvail()
	{
		return (statusFlags &PIECE_STATUS_AVAIL) !=0;
	}

	//TODO: implement
	public boolean calcAvail()
	{
		return isAvail();
	}

	public void clearAvail()
	{
		statusFlags &=~PIECE_STATUS_AVAIL;
	}

	public void setAvail()
	{
		statusFlags |=PIECE_STATUS_AVAIL;
	}

	public void setAvail(boolean b)
	{
		if (b)
			setAvail();
		else
			clearAvail();
	}

	public boolean isRequested()
	{
		return (statusFlags &PIECE_STATUS_REQUESTED) !=0;
	}

	//TODO: implement
	public boolean calcRequested()
	{
		return isRequested();
	}

	public void clearRequested()
	{
		statusFlags &=~PIECE_STATUS_REQUESTED;
	}

	public void setRequested()
	{
		statusFlags |=PIECE_STATUS_REQUESTED;
	}

	public void setRequested(boolean b)
	{
		if (b)
			setRequested();
		else
			clearRequested();
	}

	public boolean isDownloaded()
	{
		return (statusFlags &PIECE_STATUS_DOWNLOADED) !=0;
	}

	//TODO: implement
	public boolean calcDownloaded()
	{
		return isDownloaded();
	}

	public void clearDownloaded()
	{
		statusFlags &=~PIECE_STATUS_DOWNLOADED;
	}

	public void setDownloaded()
	{
		statusFlags |=PIECE_STATUS_DOWNLOADED;
	}

	public boolean calcWritten()
	{
		if (written ==null)
		{
            if (isDone())
            {
                statusFlags |=PIECE_STATUS_WRITTEN;
                return true;
            }
            statusFlags &=~PIECE_STATUS_WRITTEN;
            return false;
		}

		boolean[] written_ref =written;
		for (int i =0; i <written_ref.length; i++ )
		{
			if (!written_ref[i])
			{
                statusFlags &=~PIECE_STATUS_WRITTEN;
				return false;
			}
		}
        statusFlags |=PIECE_STATUS_WRITTEN;
		return true;
	}

	public void clearWritten()
	{
		statusFlags &=~PIECE_STATUS_WRITTEN;
	}

	public boolean isWritten()
	{
		return (statusFlags &PIECE_STATUS_WRITTEN) !=0;
	}

	public void setWritten()
	{
		statusFlags |=PIECE_STATUS_WRITTEN;
	}

	/** written[] can be null, in which case if the piece is complete, all blocks are complete
	* otherwise no blocks are complete
	*/
	public boolean[] getWritten()
	{
		return written;
	}

	public boolean isWritten(int blockNumber)
	{
		if (isDone())
			return true;

		if (written ==null)
			return false;
		return written[blockNumber];
	}

	public int getNbWritten()
	{
		if (isDone())
			return getNbBlocks();

		if (written ==null)
			return 0;

		boolean[] written_ref =written;
		int res =0;

		for (int i =0; i <written_ref.length; i++ )
		{
			if (written_ref[i])
				res++ ;
		}
		return res;
	}

	public void setWritten(int blockNumber)
	{
		if (written ==null)
			written =new boolean[getNbBlocks()];

		written[blockNumber] =true;
		time_last_write =SystemTime.getCurrentTime();
	}

	public boolean isChecking()
	{
		return (statusFlags &PIECE_STATUS_CHECKING) !=0;
	}

	//TODO: implement
	public boolean calcChecking()
	{
		return isChecking();
	}

	public void clearChecking()
	{
		statusFlags &=~PIECE_STATUS_CHECKING;
	}

	public void setChecking()
	{
		statusFlags |=PIECE_STATUS_CHECKING;
	}

	public void setChecking(boolean b)
	{
		if (b)
			statusFlags |=PIECE_STATUS_CHECKING;
		else
			statusFlags &=~PIECE_STATUS_CHECKING;
	}

	public boolean calcDone()
	{
		return isDone();
	}

	public boolean isDone()
	{
		return (statusFlags &PIECE_STATUS_DONE) !=0;
	}

	public void setDone(boolean b)
	{
		// we delegate this operation to the disk manager so it can synchronise the activity
		diskManager.setPieceDone(this, b);

		if (isDone())
			written =null;
	}

	// this is ONLY used by the disk manager to update the done state while synchronized
	// i.e. don't use it else where!
	protected void setDoneSupport(final boolean b)
	{
		if (b)
        {
			statusFlags |=PIECE_STATUS_DONE;
            written =null;
        } else
        {
			statusFlags &=~PIECE_STATUS_DONE;
        }
	}

	public long getLastWriteTime()
	{
		long now =SystemTime.getCurrentTime();
		if (now >=time_last_write)
			return time_last_write;
		return time_last_write =now;
	}

	/**
	 * Clears flags that show the piece doesn't need more downloading requested of it
	 * Including; Requested, Downloaded, Written, Checking, and Done.
	 * Avail isn't affected by this.
	 */
	public void setRequestable()
	{
		statusFlags &=~(PIECE_STATUS_REQUESTABLE);
		calcNeeded();	// Needed wouldn't have been calced before if couldn't download more
	}

	public boolean isRequestable()
	{
		return (statusFlags &PIECE_STATUS_REQUESTABLE) ==PIECE_STATUS_NEEDED;
	}

	/**
	 * @return true if the piece is Needed and not Done
	 */
	public boolean isInteresting()
	{
		return (statusFlags &PIECE_STATUS_NEEDED_DONE) ==PIECE_STATUS_NEEDED;
	}
    
    public boolean      isEGMActive()
    {
        return (statusFlags &PIECE_STATUS_EGM_ACTIVE) ==PIECE_STATUS_EGM_ACTIVE;
    }

    public boolean isEGMIgnored()
    {
        return (statusFlags &PIECE_STATUS_EGM_IGNORED) !=PIECE_STATUS_NEEDED;
    }

	public void reset()
	{
        setRequestable();
		written =null;
		time_last_write =0;
	}

	public void reDownloadBlock(int blockNumber)
	{
		if (written !=null)
		{
			written[blockNumber] =false;
			setRequestable();
		}
	}

}
