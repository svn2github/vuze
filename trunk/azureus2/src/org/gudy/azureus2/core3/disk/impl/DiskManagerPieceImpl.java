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

import java.util.ArrayList;
import java.util.List;

import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.disk.impl.piecemapper.DMPieceList;
import org.gudy.azureus2.core3.util.SystemTime;

public class DiskManagerPieceImpl
	implements DiskManagerPiece
{
	private DiskManagerImpl		disk_mgr;
	private int					piece_index	=-1;
	private int					statusFlags =PIECE_STATUS_NEEDED; // starting position is that all pieces are needed

	private long				time_last_write;
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

	protected DiskManagerPieceImpl(DiskManagerImpl _disk_manager, int _piece_index)
	{
		disk_mgr =_disk_manager;
		piece_index =_piece_index;
	}

	public DiskManager getManager()
	{
		return disk_mgr;
	}

	public int getPieceNumber()
	{
		return piece_index;
	}

	/**
	 * @return int number of bytes in the piece
	 */
	public int getLength()
	{
		if (piece_index !=disk_mgr.getNbPieces() -1)
			return (disk_mgr.getPieceLength());
		return (disk_mgr.getLastPieceLength());
	}

	public int getNbBlocks()
	{
		return ((getLength() +DiskManager.BLOCK_SIZE -1) /DiskManager.BLOCK_SIZE);
	}

	public boolean isNeeded()
	{
		return (statusFlags &PIECE_STATUS_NEEDED) !=0;
	}

	public boolean calcNeeded()
	{
		boolean filesNeeded =false;
		List filesInfo =getFiles();
		for (int i =0; i <filesInfo.size(); i++)
		{
			DiskManagerFileInfoImpl file =(DiskManagerFileInfoImpl)filesInfo.get(i);
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

	public void setDownloaded(boolean b)
	{
		if (b)
			setDownloaded();
		else
			clearDownloaded();
	}

	public boolean calcWritten()
	{
		if (written ==null)
		{
			setWritten(isDone());
			return isWritten();
		}

		boolean[] written_ref =written;
		for (int i =0; i <written_ref.length; i++ )
		{
			if (!written_ref[i])
			{
				clearWritten();
				return false;
			}
		}
		setWritten();
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

	public void setWritten(boolean b)
	{
		if (b)
			setWritten();
		else
			clearWritten();
	}

	// written[] can be null, in which case if the piece is complete, all blocks are complete
	// otherwise no blocks are complete
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

	public void setBlockWritten(int blockNumber)
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
			setChecking();
		else
			clearChecking();
	}

	public boolean calcDone()
	{
		return isDone();
	}

	public void clearDone()
	{
		setDone(false);
	}

	public boolean isDone()
	{
		return (statusFlags &PIECE_STATUS_DONE) !=0;
	}

	public void setDone()
	{
		setDone(true);
	}

	public void setDone(boolean b)
	{
		// we delegate this operation to the disk manager so it can synchronise the activity
		disk_mgr.setPieceDone(this, b);

		if (isDone())
			written =null;
	}

	// this is ONLY used by the disk manager to update the done state while synchronized
	// i.e. don't use it else where!
	protected void setDoneSupport(boolean b)
	{
		if (b)
			statusFlags |=PIECE_STATUS_DONE;
		else
			statusFlags &=~PIECE_STATUS_DONE;
	}

	public long getLastWriteTime()
	{
		long now =SystemTime.getCurrentTime();
		if (time_last_write <=now)
			return time_last_write;
		return time_last_write =now;
	}

	/**
	 * Clears [almost] all flags that say this piece has advanced to the
	 *  point where no more downloading is needed for it.
	 * Needed, and Avail flags are purposefully NOT affected by this!
	 */
	public void setRequestable()
	{
		statusFlags &=~(PIECE_STATUS_REQUESTED |PIECE_STATUS_REQUESTABLE);
	}

	/**
	 * @return true if no flag shows we need not download more of this piece
	 * Needed, Avail, and Requested flags are purposefully NOT considered by this!!
	 */
	public boolean isRequestable()
	{
		return (statusFlags &PIECE_STATUS_REQUESTABLE) ==0;
	}

	/**
	 * @return true if the piece is Needed and not Done
	 */
	public boolean isInteresting()
	{
		return (statusFlags &PIECE_STATUS_NEEDED) !=0 &&(statusFlags &PIECE_STATUS_DONE) ==0;
	}

	public void reset()
	{
		written =null;
		setRequestable();
		time_last_write =0;
	}

	public void setLastWriteTime()
	{
		time_last_write =SystemTime.getCurrentTime();
	}

	public void reDownloadBlock(int blockNumber)
	{
		if (written !=null)
		{
			written[blockNumber] =false;
			setRequestable();
		}
	}

	// maps out the list of files this piece spans
	public List getFiles()
	{
		List files =new ArrayList();
		DMPieceList pieceList =disk_mgr.getPieceList(piece_index);

		for (int i =0; i <pieceList.size(); i++ )
		{
			files.add((pieceList.get(i)).getFile());
		}
		return files;
	}
	
}
