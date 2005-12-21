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
 *			2005/Oct/08: priority/resumePriority handling and minor clock fixes
 */


import java.util.ArrayList;
import java.util.List;

import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerPiece;
import org.gudy.azureus2.core3.disk.impl.piecemapper.DMPieceList;
import org.gudy.azureus2.core3.util.SystemTime;

public class 
DiskManagerPieceImpl
implements DiskManagerPiece
{
	private static final long 	TIME_MIN_PRIORITY		=990;	// ms
	private static final long	PRIORITY_W_FILE			=1010; 	//user sets file as "High"
	private static final long	PRIORITY_W_1STLAST		=1009; 	//user select prioritize 1st/last

	private DiskManagerImpl		disk_manager;
	private int					piece_index =-1;
	private boolean				done;
	private boolean				needed =true;
	
	// to save memory the "written" field is only maintained for pieces that are
	// downloading. A value of "null" means that either the piece hasn't started 
	// download or that it is complete.
	// access to "written" is single-threaded (by the peer manager) apart from when
	// the disk manager is saving resume data.
	// actually this is not longer strictly true, as setDone is called asynchronously
	// however, this issue can be worked around by working on a reference to the written data
	// as problems only occur when switching from all-written to done=true, both of which signify
	// the same state of affairs.
	
	protected boolean[] 	written;
	protected long			last_write_time;
	
	private long			priority;
	private long			_time_last_priority;
	private long			ResumePriority;
	
	protected
	DiskManagerPieceImpl(
		DiskManagerImpl		_disk_manager,
		int					_piece_index)
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
			
		}
		return( disk_manager.getPieceLength());
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
		needed	=_needed;
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
	
	public long getLastWriteTime()
	{
		long now =SystemTime.getCurrentTime();
		if (last_write_time >now)
		{
			last_write_time =now;
		}
		return last_write_time;
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
	
	public long getPriority()
	{
		long now				=SystemTime.getCurrentTime();
		long time_since_last	=now -_time_last_priority;
		
		if ((0 <=time_since_last) &&(TIME_MIN_PRIORITY >time_since_last))
		{
			return priority;
		}
//		if (!done)
//		{
			long filesPriority					=Long.MIN_VALUE;
			long filePriority					=Long.MIN_VALUE;
			boolean firstPiecePriority			=disk_manager.getFirstPiecePriority();
			DiskManagerFileInfoImpl[] filesInfo	=getFiles();
			priority =0;
			for (int i =0; i <filesInfo.length; i++)
			{
				DiskManagerFileInfoImpl	fileInfo	=filesInfo[i];
				long fileLength					=fileInfo.getLength();
				if (!fileInfo.isSkipped() &&(0 <fileLength) &&(fileInfo.getDownloaded() <fileLength))
				{
					filePriority =0;
					// user option "prioritize more completed files"
//					if (completionPriority)
//					{
//						priority +=((1000 *fileInfo.getDownloaded()) /fileInfo.getLength());
//					}
					// user option "prioritize first and last piece"
					// TODO: maybe should prioritize according to how far from edges of file (ie middle = no priority boost)
					if (firstPiecePriority)
					{
						if (piece_index ==fileInfo.getFirstPieceNumber() || piece_index ==fileInfo.getLastPieceNumber())
						{
							filePriority +=PRIORITY_W_1STLAST;
						}
					}
					//if the file is high-priority
					//priority +=(1000 *fileInfo.getPriority()) /255;
					if (fileInfo.isPriority())
					{
						filePriority +=PRIORITY_W_FILE;
					}
					if (filesPriority <filePriority)
					{
						filesPriority =filePriority;
					}
				}
			}
			if (0 <=filesPriority)
			{
				priority +=filesPriority;
				needed =true;
			} else
			{
				priority =Long.MIN_VALUE;
				needed =false;
			}
//		} else
//		{
//			priority =Long.MIN_VALUE;
//			needed =false;
//		}
		_time_last_priority =now;
		return priority;
	}
	
	public void setResumePriority(long p)
	{
		ResumePriority =p;
	}
	
	public long getResumePriority()
	{
		return ResumePriority;
	}
	
	public DiskManager getManager()
	{
		return disk_manager;
	}

	// maps out the list of files this piece spans
  	public DiskManagerFileInfoImpl[] getFiles()
  	{
		List files =new ArrayList();
		DMPieceList	pieceList =disk_manager.getPieceList(piece_index);
		
		for (int i =0; i <pieceList.size(); i++)
		{
			DiskManagerFileInfoImpl		fileInfo =(pieceList.get(i)).getFile();
			files.add(fileInfo);
		}
  		
		DiskManagerFileInfoImpl[] filesArray =new DiskManagerFileInfoImpl[files.size()];
		files.toArray(filesArray);
		return filesArray;
	}

}
