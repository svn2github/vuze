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
 *
 */


import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.util.SystemTime;

public class 
DiskManagerPieceImpl
	implements DiskManagerPiece
{
	protected DiskManagerImpl	disk_manager;
	protected int				piece_index;
	protected boolean			done;
	

	protected boolean[] written;

	protected long		last_write_time;
	protected short		completed;

	protected
	DiskManagerPieceImpl(
		DiskManagerImpl		_disk_manager,
		int					_piece_index,
		int					_length )
	{
		disk_manager	= _disk_manager;
		piece_index		= _piece_index;
		
		int	nbBlocs = (_length + DiskManager.BLOCK_SIZE - 1) / DiskManager.BLOCK_SIZE;

		written 	= new boolean[nbBlocs];
	}
	  
	public int
	getPieceNumber()
	{
		return( piece_index );
	}
	
	public int
	getLength()
	{
		if ( piece_index == disk_manager.getNumberOfPieces() - 1 ){
			
			return( disk_manager.getLastPieceLength());
			
		}else{
			
			return( disk_manager.getPieceLength());
		}
	}
	
	public int
	getBlockCount()
	{
		return( written.length );
	}
	
	public boolean
	getDone()
	{
		return( done );
	}
	
	public void
	setDone(
		boolean		_done )
	{
		if ( done != _done ){
	
			done	= _done;
		
			disk_manager.setPieceDone( this );
		}
	}
	
	  public void setWritten(int blocNumber) {
	    written[blocNumber] = true;
	    completed++;
	    
	    last_write_time	= SystemTime.getCurrentTime();
	  }

	  public boolean
	  getWritten(
	  	int		bn )
	  {
	  	return( written[bn]);
	  }
	  
	  public long
	  getLastWriteTime()
	  {
	  	return( last_write_time );
	  }
	  
	  public int getCompleteCount() {
		return completed;
	  }
	  
	  public boolean getCompleted() {
	  	boolean complete = true;
	  	for (int i = 0; i < written.length; i++) {
	  		complete = complete && written[i];
	  		if (!complete) return false;
	  	}
		  return complete;
	  }
	  
	public boolean[]
	getWritten() 
	{
	 	return( written );
	}
	  
	public void
	reset()
	{
		written = new boolean[written.length];
    
		completed = 0;

		last_write_time = SystemTime.getCurrentTime();
	}
	  
	public void
	setInitialWriteTime()
	{
		last_write_time = SystemTime.getCurrentTime();		
	}
}
