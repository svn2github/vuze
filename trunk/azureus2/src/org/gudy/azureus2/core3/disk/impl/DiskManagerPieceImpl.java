/*
 * Created on 08-Oct-2004
 * Created by Paul Gardner
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
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
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.SystemTime;

public class DiskManagerPieceImpl
	implements DiskManagerPiece
{
    private static final LogIDs LOGID = LogIDs.PIECES;

    public static final int	PIECE_STATUS_NEEDED		=0x01;	//want to have the piece
	public static final int	PIECE_STATUS_REQUESTED	=0x04;	//piece fully requested
    public static final int	PIECE_STATUS_DOWNLOADED	=0x10;	//piece fully downloaded
    public static final int	PIECE_STATUS_WRITTEN	=0x20;	//piece fully written to storage
    public static final int	PIECE_STATUS_CHECKING	=0x40;	//piece is being hash checked
    public static final int	PIECE_STATUS_DONE		=0x80;	//everything completed - piece 100%
    
    private static final int PIECE_STATUS_MASK_REQUESTABLE=0x75;    // Needed IS once again included in this

	private static final int PIECE_STATUS_MASK_EGM_ACTIVE =0x05;    //requested and needed
	private static final int PIECE_STATUS_MASK_EGM_IGNORED=0x71;    //EGM ignores these pieces
	
	private static final int PIECE_STATUS_MASK_NEEDS_CHECK =0x60;

    //private static boolean statusTested =false;

    private final DiskManagerImpl	diskManager;
	private final int				pieceNumber;

	/** the number of bytes in this piece */
	private final int				length;
	/** the number of blocks in this piece */
	private final int				nbBlocks;
	// to save memory the "written" field is only maintained for pieces that are
	// downloading. A value of "null" means that either the piece hasn't started 
	// download or that it is complete.
	// access to "written" is single-threaded (by the peer manager) apart from when
	// the disk manager is saving resume data.
	// actually this is not longer strictly true, as setDone is called asynchronously
	// however, this issue can be worked around by working on a reference to the written data
	// as problems only occur when switching from all-written to done=true, both of which signify
	// the same state of affairs.
	protected volatile boolean[]	written;

	private long        time_last_write;

	/** it's *very* important to accurately maintain the "done" state of a piece. Currently the statusFlags
	 * are updated in a non-thread-safe manner so a 'done' field is maintained seperatly.  Synchronizing
	 * access to statusFlags or done would cause a tremendous performance hit.
	 */
    private int         statusFlags;
    private boolean		done;
    
	protected DiskManagerPieceImpl(final DiskManagerImpl _disk_manager, final int pieceIndex)
	{
		diskManager =_disk_manager;
		pieceNumber =pieceIndex;
		final int nbPieces =diskManager.getNbPieces();
		if (pieceNumber !=nbPieces -1)
			length =diskManager.getPieceLength();
		else
			length =diskManager.getLastPieceLength();
		nbBlocks =(length +DiskManager.BLOCK_SIZE -1) /DiskManager.BLOCK_SIZE;

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
		return length;
	}
	
	public int getNbBlocks()
	{
		return nbBlocks;
	}
	
    public int getBlockSize(final int blockNumber)
    {
        if (blockNumber ==nbBlocks -1)
        {
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
			final DiskManagerFileInfoImpl file =pieceList.get(i).getFile();
			final long fileLength =file.getLength();
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
			statusFlags |=PIECE_STATUS_NEEDED;
		else
			statusFlags &=~PIECE_STATUS_NEEDED;
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
			statusFlags |=PIECE_STATUS_REQUESTED;
		else
			statusFlags &=~PIECE_STATUS_REQUESTED;
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
		final boolean[] written_ref =written;
		
		if (written_ref ==null)
		{
            if ( done )
            {
                statusFlags |=PIECE_STATUS_WRITTEN;
                return true;
            }
            statusFlags &=~PIECE_STATUS_WRITTEN;
            return false;
		}
		
		for (int i =0; i <nbBlocks; i++ )
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

	/** written[] can be null, in which case if the piece is Done,
	*  all blocks are complete otherwise no blocks are complete
	*/
	public boolean[] getWritten()
	{
		return written;
	}

	public boolean isWritten(final int blockNumber)
	{
		if (done)
			return true;
		final boolean[] writtenRef =written;
		if (writtenRef ==null)
			return false;
		return writtenRef[blockNumber];
	}

	public int getNbWritten()
	{
		if (done)
			return nbBlocks;
		final boolean[] writtenRef =written;
		if (writtenRef ==null)
			return 0;
		int res =0;
		for (int i =0; i <nbBlocks; i++ )
		{
			if (writtenRef[i])
				res++;
		}
		return res;
	}

	public void setWritten(final int blockNumber)
	{
		if (written ==null)
			written =new boolean[nbBlocks];
		final boolean[] written_ref =written;
		
		written_ref[blockNumber] =true;
		time_last_write =SystemTime.getCurrentTime();
		for (int i =0; i <nbBlocks; i++)
		{
			if (!written_ref[i])
				return;
		}
		statusFlags |=PIECE_STATUS_WRITTEN;
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

    public boolean isNeedsCheck()
    {
    	return !done &&(statusFlags &PIECE_STATUS_MASK_NEEDS_CHECK) ==PIECE_STATUS_WRITTEN;
    }


    // this cannot be implemented the same as others could be
	// because the true state of Done is only determined by
	// having gone through setDoneSupport()
	public boolean calcDone()
	{
		return done;
	}

	public boolean isDone()
	{
		return done;
	}

	public void setDone(boolean b)
	{
		// we delegate this operation to the disk manager so it can synchronise the activity
        if (b !=done)
        {
            diskManager.setPieceDone(this, b);
        }
	}

	// this is ONLY used by the disk manager to update the done state while synchronized
	// i.e. don't use it else where!
	protected void setDoneSupport(final boolean b)
	{
        done =b;
        if (done)
            written =null;
	}

	/**
	 * @deprecated Use {@link #getLastWriteTime(final long now)} instead
	 */
	public long getLastWriteTime()
	{
		return getLastWriteTime(SystemTime.getCurrentTime());
	}

	public long getLastWriteTime(final long now)
	{
		if (time_last_write <=now)
			return time_last_write;
		return time_last_write =now;
	}

	/**
	 * Clears flags that show the piece doesn't need more downloading requested of it
	 * Including; Requested, Downloaded, Written, Checking, and Done.
	 */
	public void setRequestable()
	{		
		setDone(false);
		statusFlags &=~(PIECE_STATUS_MASK_REQUESTABLE);
		calcNeeded();	// Needed wouldn't have been calced before if couldn't download more
	}

	public boolean isRequestable()
	{
		return !done &&(statusFlags &PIECE_STATUS_MASK_REQUESTABLE) ==PIECE_STATUS_NEEDED;
	}

	/**
	 * @return true if the piece is Needed and not Done
	 */
	public boolean isInteresting()
	{
		return !done &&(statusFlags &PIECE_STATUS_NEEDED) != 0;
	}
    
    public boolean      isEGMActive()
    {
        return (statusFlags &PIECE_STATUS_MASK_EGM_ACTIVE) ==PIECE_STATUS_MASK_EGM_ACTIVE;
    }

    public boolean isEGMIgnored()
    {
        return done || (statusFlags &PIECE_STATUS_MASK_EGM_IGNORED) !=PIECE_STATUS_NEEDED;
    }
    
	public void reset()
	{
        setRequestable();
		written =null;
		time_last_write =0;
	}

	public void reDownloadBlock(int blockNumber)
	{
		final boolean[] written_ref = written;
		if (written_ref !=null)
		{
			written_ref[blockNumber] =false;
			setRequestable();
		}
	}
    
    public int getStatus()
    {
        if (done)
            return PIECE_STATUS_DONE;
        
        if ((statusFlags &PIECE_STATUS_CHECKING) !=0)
            return PIECE_STATUS_CHECKING;

        if ((statusFlags &PIECE_STATUS_WRITTEN) !=0)
            return PIECE_STATUS_WRITTEN;
        
        if ((statusFlags &PIECE_STATUS_DOWNLOADED) !=0)
            return PIECE_STATUS_DOWNLOADED;
        
        if ((statusFlags &PIECE_STATUS_REQUESTED) !=0)
            return PIECE_STATUS_REQUESTED;
        
        if ((statusFlags &PIECE_STATUS_NEEDED) !=0)
            return PIECE_STATUS_NEEDED;
        
        return 0;
    }

    /*
    public static final void testStatus()
    {
        if (statusTested)
            return;
        
        statusTested =true;
        int originalStatus =statusFlags;
        
        for (int i =0; i <0x100; i++)
        {
            statusFlags =i;
            Logger.log(new LogEvent(this, LOGID, LogEvent.LT_INFORMATION,
                "Done:" +isDone()
                +"  Checking:" +isChecking()
                +"  Written:" +isWritten()
                +"  Downloaded:" +isDownloaded()
                +"  Requested:" +isRequested()
                +"  Needed:" +isNeeded()
                +"  Interesting:" +isInteresting()
                +"  Requestable:" +isRequestable()
                +"  EGMActive:" +isEGMActive()
                +"  EGMIgnored:" +isEGMIgnored()
            ));
        }
        statusFlags =originalStatus;
    }
    */
}
