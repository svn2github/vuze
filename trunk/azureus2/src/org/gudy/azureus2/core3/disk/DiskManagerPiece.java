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

package org.gudy.azureus2.core3.disk;

import com.aelitis.azureus.core.util.Piece;

/**
 * Represents a DiskManager Piece
 *
 * @author parg
 * @author MjrTom
 *			2005/Oct/08: priority handling
 *			2006/Jan/2: refactoring, mostly to base Piece interface
 */

public interface DiskManagerPiece
	extends Piece
{
	public DiskManager	getManager();
	public DiskManagerFileInfo[] getFiles();
	
	public void		setLastWriteTime();
	public long		getLastWriteTime();

	// a piece is Needed if any file it covers is neither Do Not Download nor Delete, without concern for downloaded already or not
	public boolean		calcNeeded();
	public void			clearNeeded();
	public boolean		isNeeded();
	public void			setNeeded();
	public void			setNeeded(boolean b);

	// a piece is Avail if any other peer in the swarm makes the piece available, independant of if we have it or not
	public boolean		calcAvail();
	public void			clearAvail();
	public boolean		isAvail();
	public void			setAvail();
	public void			setAvail(boolean b);

	// a piece is Requested if there is at least 1 outstanding request on it AND there's no more blocks that need to be requested
	public boolean		calcRequested();
	public void			clearRequested();
	public boolean		isRequested();
	public void			setRequested();
	public void			setRequested(boolean b);

	// a piece is Downloaded if data has been received for every block (without concern for if it's written or checked)  
	public boolean		calcDownloaded();
	public void			clearDownloaded();
	public boolean		isDownloaded();
	public void			setDownloaded();
	public void			setDownloaded(boolean b);

	// a piece is Written if data has been written to storage for every block (without concern for if it's checked)  
	public boolean		calcWritten();
	public void			clearWritten();
	public void			setWritten();
	public void			setWritten(boolean b);
	public boolean		isWritten(int blockNumber);		//TODO: double check usage of this
	public void			setBlockWritten(int blockNumber);

	// a piece is Checking if a hash check has been setup and the hash check hasn't finalized the result yet
	// this flag is asynch, so be careful, and it's also transitory (comapared to most of the others being kinda sticky)
	public boolean		calcChecking();
	public boolean		isChecking();
	public void			setChecking();
	public void			setChecking(boolean b);

	// a piece is Done when the hash check has passed
	public boolean		calcDone();
	public void			clearDone();
	public boolean		isDone();
	public void			setDone();
	public void			setDone(boolean b);

	// a piece is Requestable if it's not; Downlaoded, Written, Checking, or Done
	// note that Needed, Avail, and Requested isn't considered
	public boolean isRequestable();
	// a piece is Interesting if it's Needed and Requestable
	// note that Avail and Requested isn't considered here
	public boolean isInteresting();

	public long getStartPriority();
	public void setStartPriority(long l);
}
