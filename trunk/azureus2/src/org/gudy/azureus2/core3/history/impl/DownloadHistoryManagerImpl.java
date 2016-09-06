/*
 * Created on Sep 6, 2016
 * Created by Paul Gardner
 * 
 * Copyright 2016 Azureus Software, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package org.gudy.azureus2.core3.history.impl;

import java.util.ArrayList;
import java.util.List;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerAdapter;
import org.gudy.azureus2.core3.history.*;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.ListenerManager;
import org.gudy.azureus2.core3.util.ListenerManagerDispatcher;

import com.aelitis.azureus.core.util.CopyOnWriteList;

public class 
DownloadHistoryManagerImpl
	implements DownloadHistoryManager
{
	private ListenerManager<DownloadHistoryListener>	listeners = 
		ListenerManager.createAsyncManager(
			"DHM",
			new ListenerManagerDispatcher<DownloadHistoryListener>() {
				@Override
				public void 
				dispatch(
					DownloadHistoryListener listener,
					int 					type, 
					Object 					value ) 
				{
					listener.downloadHistoryEventOccurred((DownloadHistoryEvent)value );
				}
			});
	
	private Object	lock = new Object();
	
	private CopyOnWriteList<DownloadHistory>		history;
	
	public
	DownloadHistoryManagerImpl(
		GlobalManager		global_manager )
	{
		global_manager.addListener(
			new GlobalManagerAdapter()
			{
				public void
				downloadManagerAdded(
					DownloadManager	dm )
				{
					long uid = getUID( dm );
					
					System.out.println( uid );
					
					long	flags = dm.getDownloadState().getFlags();
					
					if (( flags & ( DownloadManagerState.FLAG_LOW_NOISE | DownloadManagerState.FLAG_METADATA_DOWNLOAD )) != 0 ){
						
						return;
					}
							
					synchronized( lock ){
						
						DownloadHistory gag = new  DownloadHistoryImpl();
						
						history.add( gag );
						
						List<DownloadHistory> list = new ArrayList<DownloadHistory>(1);
						
						list.add( gag );
						
						listeners.dispatch( 0, new DownloadHistoryEventImpl( DownloadHistoryEvent.DHE_HISTORY_ADDED, list ));
					}
				}
					
				public void
				downloadManagerRemoved( 
					DownloadManager	dm )
				{
				}	
				
				public void
				destroyed()
				{
				}
			}, false );
		
		history = new CopyOnWriteList<DownloadHistory>();
		
		history.add( new DownloadHistoryImpl());
	}
	
	public boolean
	isEnabled()
	{
		return( true );
	}
	
	public List<DownloadHistory>
	getHistory()
	{
		return( history.getList());
	}
	
	public int
	getHistoryCount()
	{
		return( history.size());
	}
	
	private long
	getUID(
		DownloadManager		dm )
	{
		TOTorrent torrent = dm.getTorrent();
		
		long	lhs;
		
		if ( torrent == null ){
			
			lhs = 0;
			
		}else{
			
			try{
				byte[] hash = torrent.getHash();
				
				lhs = (hash[0]<<24)&0xff000000 | (hash[1] << 16)&0x00ff0000 | (hash[2] << 8)&0x0000ff00 | hash[3]&0x000000ff;

			}catch( Throwable e ){
				
				lhs = 0;
			}
		}
		
		
		long date_added = dm.getDownloadState().getLongAttribute( DownloadManagerState.PARAM_DOWNLOAD_ADDED_TIME );

		long	rhs = date_added/1000;
		
		return( ( lhs << 32 ) | rhs );
	}
	
	public void
	addListener(
		DownloadHistoryListener		listener,
		boolean						fire_for_existing )
	{
		synchronized( lock ){
			
			listeners.addListener( listener );
			
			if ( fire_for_existing ){
			
				List<DownloadHistory> history = getHistory();
				
				listeners.dispatch( listener, 0, new DownloadHistoryEventImpl( DownloadHistoryEvent.DHE_HISTORY_ADDED, history ));
			}
		}
	}
	
	public void
	removeListener(
		DownloadHistoryListener		listener )
	{
		listeners.removeListener( listener );
	}
	
	private class
	DownloadHistoryEventImpl
		implements DownloadHistoryEvent
	{
		private int						type;
		private List<DownloadHistory>	history;
		
		private
		DownloadHistoryEventImpl(
			int						_type,
			List<DownloadHistory>	_history )
		{
			type	= _type;
			history	= _history;
		}
		
		public int
		getEventType()
		{
			return( type );
		}
		
		public List<DownloadHistory>
		getHistory()
		{
			return( history );
		}
	}
	
	private class
	DownloadHistoryImpl
		implements DownloadHistory
	{
		private final long 		uid		= 123;
		private final byte[]	hash	= {};
		private final String	name 			= "test test test";
		private final String	save_location	= "somewhere or other";
		private final long		add_time		= 1;
		private final long		complete_time	= 2;
		private final long		remove_time		= 3;

		public long
		getUID()
		{
			return( uid );
		}
		
		public byte[]
		getTorrentHash()
		{
			return( hash );
		}
		
		public String
		getName()
		{
			return( name );
		}
		
		public String
		getSaveLocation()
		{
			return( save_location );
		}
		
		public long
		getAddTime()
		{
			return( add_time );
		}
		
		public long
		getCompleteTime()
		{
			return( complete_time );
		}
		
		public long
		getRemoveTime()
		{
			return( remove_time );
		}
	}
}
