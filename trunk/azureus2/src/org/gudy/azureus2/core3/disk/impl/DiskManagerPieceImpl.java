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
	
		// to save memory the "written" field is only maintained for pieces that are
		// downloading. A value of "null" means that either the piece hasn't started 
		// download or that it is complete.
		// access to "written" is single-threaded (by the peer manager) apart from when
		// the disk manager is saving resume data.
	
	protected boolean[] written;

	protected long		last_write_time;

	protected
	DiskManagerPieceImpl(
		DiskManagerImpl		_disk_manager,
		int					_piece_index )
	{
		disk_manager	= _disk_manager;
		piece_index		= _piece_index;
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
		if ( done != _done ){
	
			done	= _done;
		
			disk_manager.setPieceDone( this );
			
			if ( done ){
				
				written = null;
			}
		}
	}
	
	public void 
	setWritten(
		int blocNumber) 
	{
		if ( written == null ){
			
			written = new boolean[getBlockCount()];
		}
		
	    written[blocNumber] = true;
	    	    
	    last_write_time	= SystemTime.getCurrentTime();
	}

	public boolean
	getWritten(
	 	int		bn )
	{
		if ( done ){
			
			return( true );
		}
				
		if ( written == null ){
			
			return( false );		
		}
		
	  	return( written[bn]);
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
		
		if ( written == null ){
			
			return( 0 );
		}
		
		int	res = 0;
		
	  	for (int i = 0; i < written.length; i++) {
	  		
	  		if ( written[i] ){
	  			
	  			res++;
	  		}
	  	}
	  	
	  	return( res );
	}
	  
	public boolean 
	getCompleted() 
	{
		if ( written == null ){
			
			return( done );
		}
			  	
	  	for (int i = 0; i < written.length; i++) {
	  		
	  		if ( !written[i] ){
	  			
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
}
