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

import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerFactory;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.download.impl.DownloadManagerAdapter;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerAdapter;
import org.gudy.azureus2.core3.history.*;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.ListenerManager;
import org.gudy.azureus2.core3.util.ListenerManagerDispatcher;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreComponent;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreLifecycleAdapter;

public class 
DownloadHistoryManagerImpl
	implements DownloadHistoryManager
{
	private static final String CONFIG_ENABLED	= "Download History Enabled";
	
	private static final String	CONFIG_CURRENT_FILE	= "dlhistory1.config";
	private static final String	CONFIG_REMOVED_FILE = "dlhistory2.config";
	
	private final AzureusCore	azureus_core;
	
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
	
	private Map<Long,DownloadHistoryImpl>		history = new HashMap<Long,DownloadHistoryImpl>();
	
	private boolean	enabled;
	
	public
	DownloadHistoryManagerImpl()
	{
		azureus_core = AzureusCoreFactory.getSingleton();

		COConfigurationManager.addAndFireParameterListener(
				CONFIG_ENABLED,
				new ParameterListener() {
					
					private boolean	first_time = true;
					
					public void parameterChanged( String name ){
						
						setEnabledSupport( COConfigurationManager.getBooleanParameter( name ), first_time );
						
						first_time = false;
					}
				});
				
		azureus_core.addLifecycleListener(
			new AzureusCoreLifecycleAdapter()
			{
				public void
				componentCreated(
					AzureusCore				core,
					AzureusCoreComponent	component )
				{
					if ( component instanceof GlobalManager ){
				
						GlobalManager global_manager = (GlobalManager)component;
						
						global_manager.addListener(
							new GlobalManagerAdapter()
							{
								public void
								downloadManagerAdded(
									DownloadManager	dm )
								{
									synchronized( lock ){
										
										if ( !( enabled && isMonitored(dm))){
											
											return;
										}

										DownloadHistoryImpl new_dh = new  DownloadHistoryImpl( dm );
										
										DownloadHistoryImpl old_dh = history.put( new_dh.getUID(), new_dh );
										
										if ( old_dh != null ){
											
											List<DownloadHistory> old_list = new ArrayList<DownloadHistory>(1);
											
											old_list.add( old_dh );
											
											listeners.dispatch( 0, new DownloadHistoryEventImpl( DownloadHistoryEvent.DHE_HISTORY_REMOVED, old_list ));
										}
										
										List<DownloadHistory> new_list = new ArrayList<DownloadHistory>(1);
										
										new_list.add( new_dh );
										
										listeners.dispatch( 0, new DownloadHistoryEventImpl( DownloadHistoryEvent.DHE_HISTORY_ADDED, new_list ));
									}
								}
									
								public void
								downloadManagerRemoved( 
									DownloadManager	dm )
								{										
									synchronized( lock ){
										
										if ( !( enabled && isMonitored(dm))){
											
											return;
										}

										long uid = getUID( dm );

										DownloadHistoryImpl dh = history.get( uid );
										
										if ( dh != null ){
											
											dh.setRemoveTime( SystemTime.getCurrentTime());
											
											List<DownloadHistory> list = new ArrayList<DownloadHistory>(1);
											
											list.add( dh );
											
											listeners.dispatch( 0, new DownloadHistoryEventImpl( DownloadHistoryEvent.DHE_HISTORY_MODIFIED, list ));
										}

									}
								}	
							}, false );
						
						DownloadManagerFactory.addGlobalDownloadListener(
							new DownloadManagerAdapter()
							{
								@Override
								public void 
								completionChanged(
									DownloadManager 	dm,
									boolean 			bCompleted ) 
								{
									if ( !( enabled && isMonitored(dm))){
										
										return;
									}
																		
									System.out.println( "Comp Change: " + dm.getDisplayName() + " -> " + bCompleted );
								}
							});
						
						if ( enabled ){
						
							if ( !FileUtil.resilientConfigFileExists( CONFIG_CURRENT_FILE )){

								resetHistory();
							}
						}
					}
				}
				
				public void
				stopped(
					AzureusCore		core )
				{
				}
			});
	}
	
	public boolean
	isEnabled()
	{
		return( enabled );
	}
	
	public void
	setEnabled(
		boolean		enabled )
	{
		COConfigurationManager.setParameter( CONFIG_ENABLED, enabled );
	}
	
	private void
	setEnabledSupport(
		boolean		b,
		boolean		startup )
	{
		synchronized( lock ){
		
			if ( enabled == b ){
				
				return;
			}
			
			enabled	= b;
			
			if ( !startup ){
				
				if ( enabled ){
					
					resetHistory();
					
				}else{
					
					clearHistory();
				}
			}
		}
	}
	
	private boolean
	isMonitored(
		DownloadManager	dm )
	{
		if ( dm.isPersistent()){
		
			long	flags = dm.getDownloadState().getFlags();
		
			if (( flags & ( DownloadManagerState.FLAG_LOW_NOISE | DownloadManagerState.FLAG_METADATA_DOWNLOAD )) != 0 ){
			
				return( false );
			}else{
				
				return( true );
			}
		}
		
		return( false );
	}
	
	private void
	syncFromExisting(
		GlobalManager	global_manager )
	{
		if ( global_manager == null ){
			
			return;
		}
		
		synchronized( lock ){
			
			List<DownloadManager> dms = global_manager.getDownloadManagers();
			
			for ( DownloadManager dm: dms ){
				
				if ( isMonitored( dm )){
				
					DownloadHistoryImpl new_dh = new  DownloadHistoryImpl( dm );
				
					history.put( new_dh.getUID(), new_dh );
				}
			}
			
			listeners.dispatch( 
				0, 
				new DownloadHistoryEventImpl( 
					DownloadHistoryEvent.DHE_HISTORY_ADDED,  new ArrayList<DownloadHistory>( history.values())));
		}
	}
	
	public List<DownloadHistory>
	getHistory()
	{
		synchronized( lock ){
		
			return( new ArrayList<DownloadHistory>( history.values()));
		}
	}
	
	public int
	getHistoryCount()
	{
		return( history.size());
	}
	
	public void
	removeHistory(
		List<DownloadHistory>	to_remove )
	{
		synchronized( lock ){
			
			List<DownloadHistory> removed = new ArrayList<DownloadHistory>( to_remove.size());
			
			for ( DownloadHistory h: to_remove ){
				
				DownloadHistory r = history.remove( h.getUID());
					
				if ( r != null ){
					
					removed.add( r );
				}
			}
			
			if ( removed.size() > 0 ){
				
				listeners.dispatch( 
						0, 
						new DownloadHistoryEventImpl( 
							DownloadHistoryEvent.DHE_HISTORY_REMOVED,  removed ));
			}
		}
	}
	
	private void
	clearHistory()
	{
		synchronized( lock ){

			List<DownloadHistory> entries =  new ArrayList<DownloadHistory>( history.values());
			
			history.clear();
			
			listeners.dispatch( 
					0, 
					new DownloadHistoryEventImpl( 
						DownloadHistoryEvent.DHE_HISTORY_REMOVED,  entries ));
		}
	}
	
	public void
	resetHistory()
	{
		synchronized( lock ){

			clearHistory();

			syncFromExisting( azureus_core.getGlobalManager());
		}
	}
	
	private static long
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
		private final long 		uid;
		private final byte[]	hash;
		private String			name 			= "test test test";
		private String			save_location	= "somewhere or other";
		private long			add_time		= -1;
		private long			complete_time	= -1;
		private long			remove_time		= -1;

		private
		DownloadHistoryImpl(
			DownloadManager		dm )
		{
			uid		= DownloadHistoryManagerImpl.getUID( dm );
			
			byte[]	h = null;
			
			TOTorrent torrent = dm.getTorrent();
			
			if ( torrent != null ){
				
				try{
					h = torrent.getHash();
					
				}catch( Throwable e ){
				}
			}
			
			hash	= h;
			
			name	= dm.getDisplayName();
			
			save_location	= dm.getSaveLocation().getAbsolutePath();
			
			DownloadManagerState	dms = dm.getDownloadState();
			
			add_time 		= dms.getLongParameter( DownloadManagerState.PARAM_DOWNLOAD_ADDED_TIME );
			complete_time 	= dms.getLongParameter( DownloadManagerState.PARAM_DOWNLOAD_COMPLETED_TIME );
		}
		
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
		
		private void
		setRemoveTime(
			long		time )
		{
			remove_time	= time;
		}
		
		public long
		getRemoveTime()
		{
			return( remove_time );
		}
	}
}
