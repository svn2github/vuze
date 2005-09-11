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
	private DiskManagerImpl		disk_manager;
	private int					piece_index;
	private boolean				done;
	private boolean				needed;
	
		// to save memory the "written" field is only maintained for pieces that are
		// downloading. A value of "null" means that either the piece hasn't started 
		// download or that it is complete.
		// access to "written" is single-threaded (by the peer manager) apart from when
		// the disk manager is saving resume data.
		// actually this is not longer strictly true, as setDone is called asynchronously
		// however, this issue can be worked around by working on a reference to the written data
		// as problems only occur when switching from all-written to done=true, both of which signify
		// the same state of affairs.
	
	protected boolean[] written;

	protected long		last_write_time;

	protected
	DiskManagerPieceImpl(
		DiskManagerImpl		_disk_manager,
		int					_piece_index )
	{
		disk_manager	= _disk_manager;
		piece_index		= _piece_index;
		
		needed	= true;	// starting position is that all pieces are needed
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
		return((getLength() + DiskManager.BLOCK_SIZE - 1) / DiskManager.BLOCK_SIZE );
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
			// we delegate this operation to the disk manager so it can synchronise the activity
		
		disk_manager.setPieceDone( this, _done );
			
		if ( done ){
				
			written = null;
		}
	}
	
		// this is ONLY used by the disk manager to update the done state while synchronized
		// i.e. don't use it else where!
	
	protected void
	setDoneSupport(
		boolean		_done )
	{
		done	= _done;
	}
	
	public boolean
	isNeeded()
	{
		return( needed );
	}
	
	public void
	setNeeded(
		boolean	_needed )
	{
		needed	= _needed;
	}
	
	public void 
	setWritten(
		int blocNumber) 
	{
		boolean[]	written_ref = written;
		
		if ( written_ref == null ){
			
			written_ref = written = new boolean[getBlockCount()];
		}
		
	    written_ref[blocNumber] = true;
	    	    
	    last_write_time	= SystemTime.getCurrentTime();
	}

	public boolean
	getWritten(
	 	int		bn )
	{
		if ( done ){
			
			return( true );
		}
				
		boolean[]	written_ref = written;
		
		if ( written_ref == null ){
			
			return( false );		
		}
		
	  	return( written_ref[bn]);
	}
	  
	public long
	getLastWriteTime()
	{
	 	return( last_write_time );
	}
	  
	public int 
	getCompleteCount() 
	{
		if ( done ){
			
			return( getBlockCount());
		}
		
		boolean[]	written_ref	= written;
		
		if ( written_ref == null ){
			
			return( 0 );
		}
		
		int	res = 0;
		
	  	for (int i = 0; i < written_ref.length; i++) {
	  		
	  		if ( written_ref[i] ){
	  			
	  			res++;
	  		}
	  	}
	  	
	  	return( res );
	}
	  
	public boolean 
	getCompleted() 
	{
		boolean[]	written_ref	= written;
		
		if ( written_ref == null ){
			
			return( done );
		}
			  	
	  	for (int i = 0; i < written_ref.length; i++) {
	  		
	  		if ( !written_ref[i] ){
	  			
	  			return( false );
	  		}	  		
	  	}
	  	
		return( true );
	}
	  
	public boolean[]
	getWritten() 
	{
	 	return( written );
	}
	  
	public void
	reset()
	{
		written = null;
    
		setDone( false );
		
		last_write_time = SystemTime.getCurrentTime();
	}
	  
	public void
	setInitialWriteTime()
	{
		last_write_time = SystemTime.getCurrentTime();		
	}
  
  public void 
  reDownloadBlock(
	 int blockNumber ) 
  {
	  boolean[]	written_ref = written;

	  if ( written_ref != null ){
		  
		  written_ref[blockNumber] = false;
    
		  setDone(false);
	  }
  }
}
