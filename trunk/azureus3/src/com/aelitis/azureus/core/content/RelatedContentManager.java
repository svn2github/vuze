/*
 * Created on Jul 8, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
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


package com.aelitis.azureus.core.content;

import java.io.IOException;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.ByteArrayHashMap;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.RandomUtils;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.StringInterner;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.plugins.dht.DHTPlugin;
import com.aelitis.azureus.plugins.dht.DHTPluginContact;
import com.aelitis.azureus.plugins.dht.DHTPluginOperationListener;
import com.aelitis.azureus.plugins.dht.DHTPluginValue;
import com.aelitis.azureus.util.ImportExportUtils;

public class 
RelatedContentManager 
{
	private static final int	MAX_HISTORY				= 16;
	private static final int	MAX_TITLE_LENGTH		= 64;
	private static final int	MAX_CONCURRENT_PUBLISH	= 2;
	
	
	private static RelatedContentManager	singleton;
	private static AzureusCore				core;
	
	public static synchronized void
	preInitialise(
		AzureusCore		_core )
	{
		core		= _core;
	}
	
	public static synchronized RelatedContentManager
	getSingleton()
	
		throws ContentException
	{
		if ( singleton == null ){
			
			singleton = new RelatedContentManager();
		}
		
		return( singleton );
	}
	
	
	private PluginInterface 				plugin_interface;
	private TorrentAttribute 				ta_networks;
	private DHTPlugin						dht_plugin;

	private long	global_random_id = -1;
	
	private LinkedList<DownloadInfo>		download_infos1 	= new LinkedList<DownloadInfo>();
	private LinkedList<DownloadInfo>		download_infos2 	= new LinkedList<DownloadInfo>();
	
	private ByteArrayHashMapEx<DownloadInfo>	download_info_map	= new ByteArrayHashMapEx<DownloadInfo>();
	private Set<String>							download_priv_set	= new HashSet<String>();
	
	private boolean	enabled;
	
	private int publishing_count = 0;
	
	private CopyOnWriteList<RelatedContentManagerListener>	listeners = new CopyOnWriteList<RelatedContentManagerListener>();
	
	
	protected
	RelatedContentManager()
	
		throws ContentException
	{
		if ( core == null ){
			
			throw( new ContentException( "getSingleton called before pre-initialisation" ));
		}
		
		while( global_random_id == -1 ){
			
			global_random_id = COConfigurationManager.getLongParameter( "rcm.random.id", -1 );
			
			if ( global_random_id == -1 ){
				
				global_random_id = RandomUtils.nextLong();
				
				COConfigurationManager.setParameter( "rcm.random.id", global_random_id );
			}
		}
			
		plugin_interface = core.getPluginManager().getDefaultPluginInterface();
		
		ta_networks 	= plugin_interface.getTorrentManager().getAttribute( TorrentAttribute.TA_NETWORKS );
		
		COConfigurationManager.addAndFireParameterListener(
			"rcm.enabled",
			new ParameterListener()
			{
				public void 
				parameterChanged(
					String name )
				{
					enabled = COConfigurationManager.getBooleanParameter( "rcm.enabled", true );
				}
			});
		
		SimpleTimer.addEvent(
			"rcm.delay.init",
			SystemTime.getOffsetTime( 15*1000 ),
			new TimerEventPerformer()
			{
				public void 
				perform(
					TimerEvent event )
				{
					plugin_interface.addListener(
						new PluginListener()
						{
							public void
							initializationComplete()
							{
								PluginInterface dht_pi = 
									plugin_interface.getPluginManager().getPluginInterfaceByClass(
												DHTPlugin.class );
					
								if ( dht_pi != null ){
						
									dht_plugin = (DHTPlugin)dht_pi.getPlugin();

									DownloadManager dm = plugin_interface.getDownloadManager();
									
									Download[] downloads = dm.getDownloads();
									
									addDownloads( downloads, true );
									
									dm.addListener(
										new DownloadManagerListener()
										{
											public void
											downloadAdded(
												Download	download )
											{
												addDownloads( new Download[]{ download }, false );
											}
											
											public void
											downloadRemoved(
												Download	download )
											{
											}
										},
										false );
									
									SimpleTimer.addPeriodicEvent(
										"RCM:publisher",
										30*1000,
										new TimerEventPerformer()
										{
											public void 
											perform(
												TimerEvent event ) 
											{
												if ( enabled ){
												
													publish();
												}
											}
										});
								}
							}
							
							public void
							closedownInitiated()
							{
							}
							
							public void
							closedownComplete()
							{
							}
						});
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
		boolean		_enabled )
	{
		COConfigurationManager.setParameter( "rcm.enabled", _enabled );
	}
	
	protected void
	addDownloads(
		Download[]		downloads,
		boolean			initialising )
	{
		synchronized( this ){
	
			List<DownloadInfo>	new_info = new ArrayList<DownloadInfo>( downloads.length );
			
			for ( Download download: downloads ){
				
				try{
					if ( !download.isPersistent()){
						
						continue;
					}
					
					Torrent	torrent = download.getTorrent();
	
					if ( torrent == null ){
						
						continue;
					}
					
					byte[]	hash = torrent.getHash();

					if ( download_info_map.containsKey( hash )){
						
						continue;
					}
					
					String[]	networks = download.getListAttribute( ta_networks );
					
					if ( networks == null ){
						
						continue;
					}
						
					boolean	public_net = false;
					
					for (int i=0;i<networks.length;i++){
						
						if ( networks[i].equalsIgnoreCase( "Public" )){
								
							public_net	= true;
							
							break;
						}
					}
					
					if ( public_net && !TorrentUtils.isReallyPrivate( PluginCoreUtils.unwrap( torrent ))){
						
						DownloadManagerState state = PluginCoreUtils.unwrap( download ).getDownloadState();

						if ( state.getFlag(DownloadManagerState.FLAG_LOW_NOISE )){
							
							continue;
						}
						
						long rand = global_random_id ^ state.getLongParameter( DownloadManagerState.PARAM_RANDOM_SEED );						
						
						DownloadInfo info = 
							new DownloadInfo(
								hash,
								download.getName(),
								(int)rand,
								torrent.isPrivate()?StringInterner.intern(torrent.getAnnounceURL().getHost()):null );
						
						new_info.add( info );
						
						if ( initialising || download_infos1.size() == 0 ){
							
							download_infos1.add( info );
							
						}else{
							
							download_infos1.add( RandomUtils.nextInt( download_infos1.size()), info );
						}
						
						download_infos2.add( info );
						
						download_info_map.put( hash, info );
						
						if ( info.getTracker() != null ){
							
							download_priv_set.add( info.getTitle() + ":" + info.getTracker());
						}
					}
				}catch( Throwable e ){
					
					Debug.out( e );
				}
			}
			
			List<Map<String,Object>> history = (List<Map<String,Object>>)COConfigurationManager.getListParameter( "rcm.dlinfo.history", new ArrayList<Map<String,Object>>());
			
			if ( initialising ){
		
				int padd = MAX_HISTORY - download_info_map.size();
				
				for ( int i=0;i<history.size() && padd > 0;i++ ){
					
					try{
						DownloadInfo info = deserialiseDI((Map<String,Object>)history.get(i));
						
						if ( !download_info_map.containsKey( info.getHash())){
							
							download_info_map.put( info.getHash(), info );
							
							if ( info.getTracker() != null ){
								
								download_priv_set.add( info.getTitle() + ":" + info.getTracker());
							}
							
							download_infos1.add( info );
							download_infos2.add( info );
							
							padd--;
						}
					}catch( Throwable e ){
						
					}
				}
				
				Collections.shuffle( download_infos1 );
				
			}else{
				
				if ( new_info.size() > 0 ){
					
					for ( DownloadInfo info: new_info ){
						
						try{
							history.add( serialiseDI( info ));
							
						}catch( Throwable e ){
						}
					}
					
					while( history.size() > MAX_HISTORY ){
						
						history.remove(0);
					}
					
					COConfigurationManager.setParameter( "rcm.dlinfo.history", history );
				}
			}
		}
	}
	
	protected void
	publish()
	{
		while( true ){
			
			DownloadInfo	info1 = null;
			DownloadInfo	info2 = null;

			synchronized( this ){
	
				if ( publishing_count >= MAX_CONCURRENT_PUBLISH ){
					
					return;
				}
				
				if ( download_infos1.isEmpty() || download_info_map.size() == 1 ){
					
					return;
				}
							
				info1 = download_infos1.removeFirst();
				
				Iterator<DownloadInfo> it = download_infos2.iterator();
				
				while( it.hasNext()){
					
					info2 = it.next();
					
					if ( info1 != info2 || download_infos2.size() == 1 ){
						
						it.remove();
						
						break;
					}
				}
				
				if ( info1 == info2 ){
									
					info2 = download_info_map.getRandomValueExcluding( info1 );
					
					if ( info2 == null || info1 == info2 ){
						
						Debug.out( "Inconsistent!" );
						
						return;
					}
				}
				
				publishing_count++;
			}
			
			try{
				publish( info1, info2 );
				
			}catch( Throwable e ){
				
				synchronized( this ){

					publishing_count--;
				}
				
				Debug.out( e );
			}
		}
	}
	
	protected void
	publishNext()
	{
		synchronized( this ){

			publishing_count--;
			
			if ( publishing_count < 0 ){
				
					// shouldn't happen but whatever
				
				publishing_count = 0;
			}
		}
		
		publish();
	}
	
	protected void
	publish(
		final DownloadInfo	from_info,
		final DownloadInfo	to_info )
	
		throws Exception
	{			
		final String from_hash	= ByteFormatter.encodeString( from_info.getHash());
		final String to_hash	= ByteFormatter.encodeString( to_info.getHash());
		
		final byte[] key_bytes	= ( "az:rcm:assoc:" + from_hash ).getBytes( "UTF-8" );
		
		String title = to_info.getTitle(); 
		
		if ( title.length() > MAX_TITLE_LENGTH ){
			
			title = title.substring( 0, MAX_TITLE_LENGTH );
		}
		
		Map<String,Object> map = new HashMap<String,Object>();
		
		map.put( "d", title );
		map.put( "r", new Long( Math.abs( to_info.getRand()%1000 )));
		
		String	tracker = to_info.getTracker();
		
		if ( tracker == null ){
			
			map.put( "h", to_info.getHash());
			
		}else{
			
			map.put( "t", tracker );
		}

		final byte[] map_bytes = BEncoder.encode( map );
		
		final int max_hits = 30;
		
		final Download download = getDownload( from_info.getHash());
		
		dht_plugin.get(
				key_bytes,
				"Content relationship read: " + from_hash,
				DHTPlugin.FLAG_SINGLE_VALUE,
				max_hits,
				30*1000,
				false,
				false,
				new DHTPluginOperationListener()
				{
					private boolean diversified;
					private int		hits;
					
					private Set<String>	entries = new HashSet<String>();
					
					public void
					starts(
						byte[]				key )
					{
						if ( download != null ){
						
							fireLookupStarts( download, null );
						}
					}
					
					public void
					diversified()
					{
						diversified = true;
					}
					
					public void
					valueRead(
						DHTPluginContact	originator,
						DHTPluginValue		value )
					{
						try{
							Map<String,Object> map = (Map<String,Object>)BDecoder.decode( value.getValue());
							
							String	title = new String((byte[])map.get( "d" ), "UTF-8" );
							
							String	tracker	= null;
							
							byte[]	hash 	= (byte[])map.get( "h" );
							
							if ( hash == null ){
								
								tracker = new String((byte[])map.get( "t" ), "UTF-8" );
							}
							
							int	rand = ((Long)map.get( "r" )).intValue();
							
							String	key = title + " % " + rand;
							
							synchronized( entries ){
							
								if ( entries.contains( key )){
									
									return;
								}
								
								entries.add( key );
							}
							
							analyseResponse( from_info, new DownloadInfo( hash, title, rand, tracker ), null );
							
						}catch( Throwable e ){							
						}
						
						hits++;
					}
					
					public void
					valueWritten(
						DHTPluginContact	target,
						DHTPluginValue		value )
					{
						
					}
					
					public void
					complete(
						byte[]				key,
						boolean				timeout_occurred )
					{
						try{
							boolean	do_it;
							
							if ( diversified ){
								
								do_it = RandomUtils.nextInt( 10 ) == 0;
								
							}else if ( hits <= 10 ){
								
								do_it = true;
								
							}else{
							
								int scaled = 10 * ( hits - 10 ) / ( max_hits - 10 );
								
								do_it = RandomUtils.nextInt( scaled ) == 0;
							}
								
							if ( do_it ){
								
								try{
									dht_plugin.put(
											key_bytes,
											"Content relationship: " +  from_hash + " -> " + to_hash,
											map_bytes,
											DHTPlugin.FLAG_ANON,
											new DHTPluginOperationListener()
											{
												public void
												diversified()
												{
												}
												
												public void 
												starts(
													byte[] 				key ) 
												{
												}
												
												public void
												valueRead(
													DHTPluginContact	originator,
													DHTPluginValue		value )
												{
												}
												
												public void
												valueWritten(
													DHTPluginContact	target,
													DHTPluginValue		value )
												{
												}
												
												public void
												complete(
													byte[]				key,
													boolean				timeout_occurred )
												{
													publishNext();
												}
											});
								}catch( Throwable e ){
									
									Debug.printStackTrace(e);
									
									publishNext();
								}
							}else{
								
								publishNext();
							}
						}finally{
							
							if ( download != null ){
								
								fireLookupComplete( download, null );
							}
						}
					}
				});
	}
		
	public void
	lookupContent(
		final Download						download,
		final RelatedContentManagerListener	listener )
	
		throws ContentException
	{
		final DownloadInfo	from_info;
	
		synchronized( this ){
			
			Torrent t = download.getTorrent();
			
			if ( t == null ){
				
				throw( new ContentException( "Torrent not available" ));
			}
			
			from_info = download_info_map.get( t.getHash());
			
			if ( from_info == null ){
				
				throw( new ContentException( "Unknown download" ));
			}
		}
		
		final String from_hash	= ByteFormatter.encodeString( from_info.getHash());
		
		try{
			final byte[] key_bytes	= ( "az:rcm:assoc:" + from_hash ).getBytes( "UTF-8" );
			
			
			final int max_hits = 30;
			
			dht_plugin.get(
					key_bytes,
					"Content relationship read: " + from_hash,
					DHTPlugin.FLAG_SINGLE_VALUE,
					max_hits,
					60*1000,
					false,
					false,
					new DHTPluginOperationListener()
					{
						private Set<String>	entries = new HashSet<String>();
						
						public void
						starts(
							byte[]				key )
						{
							fireLookupStarts( download, listener );
						}
						
						public void
						diversified()
						{
						}
						
						public void
						valueRead(
							DHTPluginContact	originator,
							DHTPluginValue		value )
						{
							try{
								Map<String,Object> map = (Map<String,Object>)BDecoder.decode( value.getValue());
								
								String	title = new String((byte[])map.get( "d" ), "UTF-8" );
								
								String	tracker	= null;
								
								byte[]	hash 	= (byte[])map.get( "h" );
								
								if ( hash == null ){
									
									tracker = new String((byte[])map.get( "t" ), "UTF-8" );
								}
								
								int	rand = ((Long)map.get( "r" )).intValue();
								
								String	key = title + " % " + rand;
								
								synchronized( entries ){
								
									if ( entries.contains( key )){
										
										return;
									}
									
									entries.add( key );
								}
								
								analyseResponse( from_info, new DownloadInfo( hash, title, rand, tracker ), listener );
								
							}catch( Throwable e ){							
							}
						}
						
						public void
						valueWritten(
							DHTPluginContact	target,
							DHTPluginValue		value )
						{
							
						}
						
						public void
						complete(
							byte[]				key,
							boolean				timeout_occurred )
						{
							fireLookupComplete( download, listener );
						}				
					});
		}catch( Throwable e ){
			
			throw( new ContentException( "Lookup failed", e ));
		}
	}
	
	protected void
	analyseResponse(
		DownloadInfo					from_info,
		DownloadInfo					to_info,
		RelatedContentManagerListener	extra_listener )
	{
		try{			
			synchronized( this ){
				
				byte[] target = to_info.getHash();
				
				if ( target != null ){
					
					if ( download_info_map.containsKey( target )){
						
							// already know about this
						
						return;
					}
				}else{
					
					if ( download_priv_set.contains( to_info.getTitle() + ":" + to_info.getTracker())){
						
							// already know about this
						
						return;
					}
				}
			}
			
			// TODO: stuff!
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
		
		if ( listeners.size() > 0 || extra_listener != null ){
			
			Download download = getDownload( from_info.getHash());
			
			if ( download != null ){
				
				for ( RelatedContentManagerListener l: listeners ){
					
					try{
						l.foundContent( download, to_info );
						
					}catch( Throwable e ){
						
						Debug.out( e );
					}
				}
				
				if ( extra_listener != null ){
					
					try{
						extra_listener.foundContent( download, to_info );
						
					}catch( Throwable e ){
						
						Debug.out( e );
					}
				}
			}
		}
	}
	
	protected Download
	getDownload(
		byte[]	hash )
	{
		try{
			return( plugin_interface.getDownloadManager().getDownload( hash ));
			
		}catch( Throwable e ){
			
			return( null );
		}
	}
	
	protected void
	fireLookupStarts(
		Download						download,
		RelatedContentManagerListener	extra_listener )
	{
		for ( RelatedContentManagerListener listener: listeners ){
			
			try{
				listener.lookupStarted( download );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
		
		if ( extra_listener != null ){
			
			try{
				extra_listener.lookupStarted( download );
				
			}catch( Throwable e ){	
				
				Debug.out( e );
			}
		}
	}
	
	protected void
	fireLookupComplete(
		Download						download,
		RelatedContentManagerListener	extra_listener )
	{
		for ( RelatedContentManagerListener listener: listeners ){
			
			try{
				listener.lookupCompleted( download );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
		
		if ( extra_listener != null ){
			
			try{
				extra_listener.lookupCompleted( download );
				
			}catch( Throwable e ){	
				
				Debug.out( e );
			}
		}
	}
	
	public void
	addListener(
		RelatedContentManagerListener		listener )
	{
		listeners.add( listener );
	}
	
	public void
	removeListener(
		RelatedContentManagerListener		listener )
	{
		listeners.remove( listener );
	}
	
	protected class
	ByteArrayHashMapEx<T>
		extends ByteArrayHashMap<T>
	{
	    public T
	    getRandomValueExcluding(
	    	T	excluded )
	    {
	    	int	num = RandomUtils.nextInt( size );
	    	
	    	T result = null;
	    	
	        for (int j = 0; j < table.length; j++) {
	        	
		         Entry<T> e = table[j];
		         
		         while( e != null ){
		        	 
	              	T value = e.value;
	               	
	              	if ( value != excluded ){
	              		
	              		result = value;
	              	}
	              	
	              	if ( num <= 0 && result != null ){
	              		
	              		return( result );
	              	}
	              	
	              	num--;
	              	
	              	e = e.next;
		        }
		    }
	    
	        return( result );
	    }
	}
	
	protected Map<String,Object>
	serialiseDI(
		DownloadInfo	info )
	
		throws IOException
	{
		Map<String,Object> m = new HashMap<String,Object>();
		
		m.put( "h", info.getHash());
		
		ImportExportUtils.exportString( m, "d", info.getTitle());
		ImportExportUtils.exportInt( m, "r", info.getRand());
		ImportExportUtils.exportString( m, "t", info.getTracker());
		
		return( m );
	}
	
	protected DownloadInfo
	deserialiseDI(
		Map<String,Object>				m )
	
		throws IOException
	{
		byte[]	hash 	= (byte[])m.get("h");
		String	title	= ImportExportUtils.importString( m, "d" );
		int		rand	= ImportExportUtils.importInt( m, "r" );
		String	tracker	= ImportExportUtils.importString( m, "t" );
		
		return( new DownloadInfo( hash, title, rand, tracker ));
	}
	
	protected static class
	DownloadInfo
		extends RelatedContent
	{
		final private int			rand;
		
		protected
		DownloadInfo(
			byte[]		_hash,
			String		_title,
			int			_rand,
			String		_tracker )
		{
			super( _title, _hash, _tracker );
			
			rand		= _rand;
		}
		
		protected int
		getRand()
		{
			return( rand );
		}
		
		public String
		getString()
		{
			return( super.getString() + ", " + rand );
		}
	}
}
