/*
 * Created on 03-Mar-2005
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

package com.aelitis.azureus.plugins.magnet;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.net.InetSocketAddress;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentAnnounceURLGroup;
import org.gudy.azureus2.core3.torrent.TOTorrentAnnounceURLSet;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.ByteArrayHashMap;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DelayedEvent;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.ddb.DistributedDatabase;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseContact;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseEvent;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseListener;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseProgressListener;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseTransferType;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseValue;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.sharing.ShareException;
import org.gudy.azureus2.plugins.sharing.ShareResourceDir;
import org.gudy.azureus2.plugins.sharing.ShareResourceFile;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.IntParameter;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderAdapter;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderException;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderFactory;

import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.core.util.FeatureAvailability;
import com.aelitis.net.magneturi.*;

/**
 * @author parg
 *
 */

public class 
MagnetPlugin
	implements Plugin
{	
	public static final int	FL_NONE					= 0x00000000;
	public static final int	FL_DISABLE_MD_LOOKUP	= 0x00000001;
	
	private static final String	SECONDARY_LOOKUP 			= "http://magnet.vuze.com/";
	private static final int	SECONDARY_LOOKUP_DELAY		= 20*1000;
	private static final int	SECONDARY_LOOKUP_MAX_TIME	= 2*60*1000;
	
	private static final int	MD_LOOKUP_DELAY_SECS_DEFAULT		= 20;


	private static final String	PLUGIN_NAME				= "Magnet URI Handler";
	private static final String PLUGIN_CONFIGSECTION_ID = "plugins.magnetplugin";

	private PluginInterface		plugin_interface;
		
	private CopyOnWriteList		listeners = new CopyOnWriteList();
	
	private boolean			first_download	= true;
	
	private BooleanParameter secondary_lookup;
	private BooleanParameter md_lookup;
	private IntParameter	 md_lookup_delay;
	
	public static void
	load(
		PluginInterface		plugin_interface )
	{
		plugin_interface.getPluginProperties().setProperty( "plugin.version", 	"1.0" );
		plugin_interface.getPluginProperties().setProperty( "plugin.name", PLUGIN_NAME );
	}
	
	public void
	initialize(
		PluginInterface	_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
		
		BasicPluginConfigModel	config = 
			plugin_interface.getUIManager().createBasicPluginConfigModel( ConfigSection.SECTION_PLUGINS, 
					PLUGIN_CONFIGSECTION_ID);
		
		secondary_lookup 	= config.addBooleanParameter2( "MagnetPlugin.use.lookup.service", "MagnetPlugin.use.lookup.service", true );
		md_lookup 			= config.addBooleanParameter2( "MagnetPlugin.use.md.download", "MagnetPlugin.use.md.download", true );
		md_lookup_delay		= config.addIntParameter2( "MagnetPlugin.use.md.download.delay", "MagnetPlugin.use.md.download.delay", MD_LOOKUP_DELAY_SECS_DEFAULT );
		
		md_lookup.addEnabledOnSelection( md_lookup_delay );
		
		MenuItemListener	listener = 
			new MenuItemListener()
			{
				public void
				selected(
					MenuItem		_menu,
					Object			_target )
				{
					Torrent torrent;
					String name;
					Object ds = ((TableRow)_target).getDataSource();
					if (ds instanceof ShareResourceFile) {
						try {
							torrent = ((ShareResourceFile) ds).getItem().getTorrent();
						} catch (ShareException e) {
							return;
						}
						name = ((ShareResourceFile) ds).getName();
					}else if (ds instanceof ShareResourceDir) {
							try {
								torrent = ((ShareResourceDir) ds).getItem().getTorrent();
							} catch (ShareException e) {
								return;
							}
							name = ((ShareResourceDir) ds).getName();
					} else if (ds instanceof Download) {
						Download download = (Download)((TableRow)_target).getDataSource();
						torrent = download.getTorrent();
						name = download.getName();
					} else {
						return;
					}
				  
					
					String cb_data = UrlUtils.getMagnetURI( name, torrent );
					
					// removed this as well - nothing wrong with allowing magnet copy
					// for private torrents - they still can't be tracked if you don't
					// have permission
					
					
					/*if ( torrent.isPrivate()){
						
						cb_data = getMessageText( "private_torrent" );
						
					}else if ( torrent.isDecentralised()){
					*/	
						// ok
						
						/* relaxed this as we allow such torrents to be downloaded via magnet links
						 * (as opposed to tracked in the DHT)
						 
					}else if ( torrent.isDecentralisedBackupEnabled()){
							
						TorrentAttribute ta_peer_sources 	= plugin_interface.getTorrentManager().getAttribute( TorrentAttribute.TA_PEER_SOURCES );

						String[]	sources = download.getListAttribute( ta_peer_sources );
		
						boolean	ok = false;
								
						for (int i=0;i<sources.length;i++){
									
							if ( sources[i].equalsIgnoreCase( "DHT")){
										
								ok	= true;
										
								break;
							}
						}
		
						if ( !ok ){
							
							cb_data = getMessageText( "decentral_disabled" );
						}
					}else{
						
						cb_data = getMessageText( "decentral_backup_disabled" );
						*/
					// }
					
					// System.out.println( "MagnetPlugin: export = " + url );
					
					try{
						plugin_interface.getUIManager().copyToClipBoard( cb_data );
						
					}catch( Throwable  e ){
						
						e.printStackTrace();
					}
				}
			};
		
		final TableContextMenuItem menu1 = plugin_interface.getUIManager().getTableManager().addContextMenuItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE, "MagnetPlugin.contextmenu.exporturi" );
		final TableContextMenuItem menu2 = plugin_interface.getUIManager().getTableManager().addContextMenuItem(TableManager.TABLE_MYTORRENTS_COMPLETE, 	"MagnetPlugin.contextmenu.exporturi" );
		final TableContextMenuItem menu3 = plugin_interface.getUIManager().getTableManager().addContextMenuItem(TableManager.TABLE_MYSHARES, 	"MagnetPlugin.contextmenu.exporturi" );
			
		menu1.addListener( listener );
		menu2.addListener( listener );
		menu3.addListener( listener );

		MagnetURIHandler.getSingleton().addListener(
			new MagnetURIHandlerListener()
			{
				public byte[]
				badge()
				{
					InputStream is = getClass().getClassLoader().getResourceAsStream( "com/aelitis/azureus/plugins/magnet/Magnet.gif" );
					
					if ( is == null ){
						
						return( null );
					}
					
					try{
						ByteArrayOutputStream	baos = new ByteArrayOutputStream();
						
						try{
							byte[]	buffer = new byte[8192];
							
							while( true ){
	
								int	len = is.read( buffer );
				
								if ( len <= 0 ){
									
									break;
								}
		
								baos.write( buffer, 0, len );
							}
						}finally{
							
							is.close();
						}
						
						return( baos.toByteArray());
						
					}catch( Throwable e ){
						
						Debug.printStackTrace(e);
						
						return( null );
					}
				}
							
				public byte[]
				download(
					final MagnetURIHandlerProgressListener		muh_listener,
					final byte[]								hash,
					final String								args,
					final InetSocketAddress[]					sources,
					final long									timeout )
				
					throws MagnetURIHandlerException
				{
						// see if we've already got it!
					
					try{
						Download	dl = plugin_interface.getDownloadManager().getDownload( hash );
					
						if ( dl != null ){
							
							Torrent	torrent = dl.getTorrent();
							
							if ( torrent != null ){
								
								return( torrent.writeToBEncodedData());
							}
						}
					}catch( Throwable e ){
					
						Debug.printStackTrace(e);
					}
					
					return( MagnetPlugin.this.download(
							new MagnetPluginProgressListener()
							{
								public void
								reportSize(
									long	size )
								{
									muh_listener.reportSize( size );
								}
								
								public void
								reportActivity(
									String	str )
								{
									muh_listener.reportActivity( str );
								}
								
								public void
								reportCompleteness(
									int		percent )
								{
									muh_listener.reportCompleteness( percent );
								}
								
								public void
								reportContributor(
									InetSocketAddress	address )
								{
								}
								
								public boolean 
								cancelled() 
								{
									return( muh_listener.cancelled());
								}
								
								public boolean 
								verbose() 
								{
									return( muh_listener.verbose());
								}
							},
							hash,
							args,
							sources,
							timeout,
							0 ));
				}
				
				public boolean
				download(
					URL		url )
				
					throws MagnetURIHandlerException
				{
					try{
						
						plugin_interface.getDownloadManager().addDownload( url, false );
						
						return( true );
						
					}catch( DownloadException e ){
						
						throw( new MagnetURIHandlerException( "Operation failed", e ));
					}
				}
				
				public boolean
				set(
					String		name,
					Map		values )
				{
					List	l = listeners.getList();
					
					for (int i=0;i<l.size();i++){
						
						if (((MagnetPluginListener)l.get(i)).set( name, values )){
							
							return( true );
						}
					}
					
					return( false );
				}
				
				public int
				get(
					String		name,
					Map			values )
				{
					List	l = listeners.getList();
					
					for (int i=0;i<l.size();i++){
						
						int res = ((MagnetPluginListener)l.get(i)).get( name, values );
						
						if ( res != Integer.MIN_VALUE ){
							
							return( res );
						}
					}
					
					return( Integer.MIN_VALUE );
				}
			});
		
		plugin_interface.addListener(
			new PluginListener()
			{
				public void
				initializationComplete()
				{
						// make sure DDB is initialised as we need it to register its
						// transfer types
					
					AEThread2 t = 
						new AEThread2( "MagnetPlugin:init", true )
						{
							public void
							run()
							{
								plugin_interface.getDistributedDatabase();
							}
						};
										
					t.start();
				}
				
				public void
				closedownInitiated(){}
				
				public void
				closedownComplete(){}			
			});
		
		plugin_interface.getUIManager().addUIListener(
				new UIManagerListener()
				{
					public void
					UIAttached(
						UIInstance		instance )
					{
						if ( instance.getUIType() == UIInstance.UIT_SWT ){
							
							try{
								Class.forName( "com.aelitis.azureus.plugins.magnet.swt.MagnetPluginUISWT" ).getConstructor(
									new Class[]{ UIInstance.class, TableContextMenuItem[].class }).newInstance(
										new Object[]{ instance, new TableContextMenuItem[]{ menu1, menu2, menu3 }} );
								
							}catch( Throwable e ){
								
								e.printStackTrace();
							}
						}
					}
					
					public void
					UIDetached(
						UIInstance		instance )
					{
						
					}
				});
		
		final List<Download>	to_delete = new ArrayList<Download>();
		
		Download[] downloads = plugin_interface.getDownloadManager().getDownloads();
		
		for ( Download download: downloads ){
			
			if ( download.getFlag( Download.FLAG_METADATA_DOWNLOAD )){
				
				to_delete.add( download );
			}
		}
		
		if ( to_delete.size() > 0 ){
			
			AEThread2 t = 
				new AEThread2( "MagnetPlugin:delmds", true )
				{
					public void
					run()
					{
						for ( Download download: to_delete ){
							
							try{
								download.stop();
								
							}catch( Throwable e ){
							}
							
							try{
								download.remove( true, true );
								
							}catch( Throwable e ){
								
								Debug.out( e );
							}
						}
					}
				};
								
			t.start();
		}
	}
	
	public URL
	getMagnetURL(
		Download		d )
	{
		Torrent	torrent = d.getTorrent();
		
		if ( torrent == null ){
			
			return( null );
		}
		
		return( getMagnetURL( torrent.getHash()));
	}
	
	public URL
	getMagnetURL(
		byte[]		hash )
	{
		try{
			return( new URL( "magnet:?xt=urn:btih:" + Base32.encode(hash)));
		
		}catch( Throwable e ){
		
			Debug.printStackTrace(e);
		
			return( null );
		}
	}
	
	public byte[]
	badge()
	{
		return( null );
	}
	
	public byte[]
	download(
		MagnetPluginProgressListener		listener,
		byte[]								hash,
		String								args,
		InetSocketAddress[]					sources,
		long								timeout,
		int									flags )
	
		throws MagnetURIHandlerException
	{
		byte[]	torrent_data = downloadSupport( listener, hash, args, sources, timeout, flags );
		
		if ( args != null ){
			
			String[] bits = args.split( "&" );
			
			List<String>	new_web_seeds 	= new ArrayList<String>();
			List<String>	new_trackers 	= new ArrayList<String>();

			for ( String bit: bits ){
				
				String[] x = bit.split( "=" );
				
				if ( x.length == 2 ){
					
					String	lhs = x[0].toLowerCase();
					
					if ( lhs.equals( "ws" )){
						
						try{
							new_web_seeds.add( new URL( UrlUtils.decode( x[1] )).toExternalForm());
							
						}catch( Throwable e ){							
						}
					}else if ( lhs.equals( "tr" )){
						
						try{
							new_trackers.add( new URL( UrlUtils.decode( x[1] )).toExternalForm());
							
						}catch( Throwable e ){							
						}
					}
				}
			}
			
			if ( new_web_seeds.size() > 0 || new_trackers.size() > 0 ){
				
				try{
					TOTorrent torrent = TOTorrentFactory.deserialiseFromBEncodedByteArray( torrent_data );
	
					boolean	update_torrent = false;
					
					if ( new_web_seeds.size() > 0 ){
						
						Object obj = torrent.getAdditionalProperty( "url-list" );
						
						List<String> existing = new ArrayList<String>();
						
						if ( obj instanceof byte[] ){
			                
							try{
								new_web_seeds.remove( new URL( new String((byte[])obj, "UTF-8" )).toExternalForm());
								
							}catch( Throwable e ){							
							}
						}else if ( obj instanceof List ){
							
							List<byte[]> l = (List<byte[]>)obj;
							
							for ( byte[] b: l ){
								
								try{
									existing.add( new URL( new String((byte[])b, "UTF-8" )).toExternalForm());
									
								}catch( Throwable e ){							
								}
							}
						}
						
						boolean update_ws = false;
						
						for ( String e: new_web_seeds ){
							
							if ( !existing.contains( e )){
								
								existing.add( e );
								
								update_ws = true;
							}
						}
						
						if ( update_ws ){
						
							List<byte[]>	l = new ArrayList<byte[]>();
							
							for ( String s: existing ){
								
								l.add( s.getBytes( "UTF-8" ));
							}
							
							torrent.setAdditionalProperty( "url-list", l );
							
							update_torrent = true;
						}
					}
					
					if ( new_trackers.size() > 0 ){
												
						URL announce_url = torrent.getAnnounceURL();
													
						new_trackers.remove( announce_url.toExternalForm());
						
						TOTorrentAnnounceURLGroup group = torrent.getAnnounceURLGroup();
						
						TOTorrentAnnounceURLSet[] sets = group.getAnnounceURLSets();
						
						for ( TOTorrentAnnounceURLSet set: sets ){
							
							URL[] set_urls = set.getAnnounceURLs();
							
							for( URL set_url: set_urls ){
																																		
								new_trackers.remove( set_url.toExternalForm());
							}
						}
						
						if ( new_trackers.size() > 0 ){
							
							TOTorrentAnnounceURLSet[]	new_sets = new TOTorrentAnnounceURLSet[ sets.length + new_trackers.size()];
							
							for ( int i=0;i<sets.length;i++){
								
								new_sets[i] = sets[i];
							}
							
							for ( int i=0;i<new_trackers.size();i++){
								
								TOTorrentAnnounceURLSet new_set = group.createAnnounceURLSet( new URL[]{ new URL( new_trackers.get(i))});
								
								new_sets[i+sets.length] = new_set;
							}
							
							group.setAnnounceURLSets( new_sets );
							
							update_torrent = true;
						}
					}
					
					if ( update_torrent ){
						
						torrent_data = BEncoder.encode( torrent.serialiseToMap());
					}
				}catch( Throwable e ){
				}
			}
		}
		
		return( torrent_data );
	}
	
	private static ByteArrayHashMap<DownloadActivity>	download_activities = new ByteArrayHashMap<DownloadActivity>();
	
	private static class
	DownloadActivity
	{
		private volatile byte[]						result;
		private volatile MagnetURIHandlerException	error;
		
		private AESemaphore		sem = new AESemaphore( "MP:DA" );
		
		public void
		setResult(
			byte[]	_result )
		{
			result	= _result;
			
			sem.releaseForever();
		}
		
		public void
		setResult(
			Throwable _error  )
		{
			if ( _error instanceof MagnetURIHandlerException ){
				
				error = (MagnetURIHandlerException)_error;
				
			}else{
				
				error = new MagnetURIHandlerException( "Download failed", _error );
			}
			
			sem.releaseForever();
		}
		
		public byte[]
		getResult()
		
			throws MagnetURIHandlerException
		{
			sem.reserve();
			
			if ( error != null ){
				
				throw( error );
			}
			
			return( result );
		}
	}
	
	private byte[]
 	downloadSupport(
 		MagnetPluginProgressListener	listener,
 		byte[]							hash,
 		String							args,
 		InetSocketAddress[]				sources,
 		long							timeout,
 		int								flags )
 	
 		throws MagnetURIHandlerException
 	{
		DownloadActivity	activity;
		boolean				new_activity = false;
		
 		synchronized( download_activities ){
 			
 				// single-thread per hash to avoid madness ensuing if we get multiple concurrent hits
 			
 			activity = download_activities.get( hash );
 			
 			if ( activity == null ){
 				
 				activity = new DownloadActivity();
 				
 				download_activities.put( hash, activity );
 				
 				new_activity = true;
 			}
 		}
 		 		
 		if ( new_activity ){
 		
	 		try{
	 			
	 			activity.setResult( _downloadSupport( listener, hash, args, sources, timeout, flags ));
	 			
	 		}catch( Throwable e ){
	 			
	 			activity.setResult( e );
	 			
	 		}finally{
	 			
	 			synchronized( download_activities ){
	 				
	 				download_activities.remove( hash );
	 			}
	 		}
 		}
 			
 		return( activity.getResult());

 	}
	
	private byte[]
	_downloadSupport(
		final MagnetPluginProgressListener		listener,
		final byte[]							hash,
		final String							args,
		final InetSocketAddress[]				sources,
		final long								timeout,
		int										flags )
	
		throws MagnetURIHandlerException
	{
		boolean	md_enabled;
		
		if ((flags & FL_DISABLE_MD_LOOKUP) != 0 ){
			
			md_enabled = false;
			
		}else{
			
			md_enabled = md_lookup.getValue() && FeatureAvailability.isMagnetMDEnabled();
		}

		TimerEvent							md_delay_event = null;
		final byte[][]						md_result = { null };
		final MagnetPluginMDDownloader[]	md_downloader = { null };
		
		final byte[][]						fl_result = { null };

		if ( md_enabled ){
			
			int	delay_millis = md_lookup_delay.getValue()*1000;
			
			md_delay_event = 
				SimpleTimer.addEvent(
					"MagnetPlugin:md_delay",
					delay_millis<=0?0:(SystemTime.getCurrentTime() + delay_millis ),
					new TimerEventPerformer()
					{
						public void 
						perform(
							TimerEvent event ) 
						{
							MagnetPluginMDDownloader mdd;
							
							synchronized( md_downloader ){
								
								if ( event.isCancelled()){
									
									return;
								}
								
								md_downloader[0] = mdd = new MagnetPluginMDDownloader( plugin_interface, hash, args );
							}
							
							listener.reportActivity( getMessageText( "report.md.starts" ));
							
							mdd.start(
								new MagnetPluginMDDownloader.DownloadListener()
								{
									public void
									reportProgress(
										int		downloaded,
										int		total_size )
									{
										listener.reportActivity( getMessageText( "report.md.progress", String.valueOf( downloaded + "/" + total_size ) ));
										
										listener.reportCompleteness( 100*downloaded/total_size );
									}
									
									public void
									complete(
										TOTorrent	torrent )
									{
										listener.reportActivity( getMessageText( "report.md.done" ));
										
										synchronized( md_result ){
										
											try{
												md_result[0] = BEncoder.encode( torrent.serialiseToMap());
												
											}catch( Throwable e ){
												
												Debug.out( e );
											}
										}
									}
									
									public void
									failed(
										Throwable e )
									{
										listener.reportActivity( getMessageText( "report.error", Debug.getNestedExceptionMessage(e)));
									}
								});
						}
					});
		}
		
		if ( args != null ){
			
			String[] bits = args.split( "&" );
			
			List<URL>	fl_args 	= new ArrayList<URL>();

			for ( String bit: bits ){
				
				String[] x = bit.split( "=" );
				
				if ( x.length == 2 ){
					
					String	lhs = x[0].toLowerCase();
					
					if ( lhs.equals( "fl" )){
						
						try{
							fl_args.add( new URL( UrlUtils.decode( x[1] )));
							
						}catch( Throwable e ){							
						}
					}
				}
			}
			
			if ( fl_args.size() > 0 ){
				
				for ( int i=0;i<fl_args.size() && i < 3; i++ ){
					
					final URL fl_url = fl_args.get( i );
					
					new AEThread2( "Magnet:fldl", true )
					{
						public void 
						run() 
						{
							try{
								TOTorrent torrent = TorrentUtils.download( fl_url );
								
								if ( torrent != null ){
									
									if ( Arrays.equals( torrent.getHash(), hash )){
										
										synchronized( fl_result ){
											
											fl_result[0] = BEncoder.encode( torrent.serialiseToMap());
										}
									}
								}
							}catch( Throwable e ){
								
								Debug.out( e );
							}
						}
					}.start();
				}
			}
		}
		
		try{
			try{
				long	remaining	= timeout;
								
				boolean	sl_enabled				= secondary_lookup.getValue() && FeatureAvailability.isMagnetSLEnabled();
	
				long secondary_lookup_time 	= -1;

				final Object[] secondary_result = { null };

				boolean	is_first_download = first_download;
				
				if ( is_first_download ){
				
					listener.reportActivity( getMessageText( "report.waiting_ddb" ));
					
					first_download = false;
				}
				
				final DistributedDatabase db = plugin_interface.getDistributedDatabase();
				
				if ( db.isAvailable()){
					
					final List			potential_contacts 		= new ArrayList();
					final AESemaphore	potential_contacts_sem 	= new AESemaphore( "MagnetPlugin:liveones" );
					final AEMonitor		potential_contacts_mon	= new AEMonitor( "MagnetPlugin:liveones" );
					
					final int[]			outstanding		= {0};
					final boolean[]		lookup_complete	= {false};
					
					listener.reportActivity(  getMessageText( "report.searching" ));
					
					DistributedDatabaseListener	ddb_listener = 
						new DistributedDatabaseListener()
						{
							private Set	found_set = new HashSet();
							
							public void
							event(
								DistributedDatabaseEvent 		event )
							{
								int	type = event.getType();
			
								if ( type == DistributedDatabaseEvent.ET_OPERATION_STARTS ){
		
										// give live results a chance before kicking in explicit ones
									
									if ( sources.length > 0 ){
										
										new DelayedEvent(
											"MP:sourceAdd",
											10*1000,
											new AERunnable()
											{
												public void
												runSupport()
												{
													addExplicitSources();
												}
											});
									}
									
								}else if ( type == DistributedDatabaseEvent.ET_VALUE_READ ){
															
									contactFound( event.getValue().getContact());
					
								}else if (	type == DistributedDatabaseEvent.ET_OPERATION_COMPLETE ||
											type == DistributedDatabaseEvent.ET_OPERATION_TIMEOUT ){
										
									listener.reportActivity( getMessageText( "report.found", String.valueOf( found_set.size())));
									
										// now inject any explicit sources
		
									addExplicitSources();
									
									try{
										potential_contacts_mon.enter();													
		
										lookup_complete[0] = true;
										
									}finally{
										
										potential_contacts_mon.exit();
									}
									
									potential_contacts_sem.release();
								}
							}
							
							protected void
							addExplicitSources()
							{	
								for (int i=0;i<sources.length;i++){
									
									try{
										contactFound( db.importContact(sources[i]));
										
									}catch( Throwable e ){
										
										Debug.printStackTrace(e);
									}
								}
							}
							
							public void
							contactFound(
								final DistributedDatabaseContact	contact )
							{
								String	key = contact.getAddress().toString();
								
								synchronized( found_set ){
									
									if ( found_set.contains( key )){
										
										return;
									}
									
									found_set.add( key );
								}
								
								if ( listener.verbose()){
								
									listener.reportActivity( getMessageText( "report.found", contact.getName()));
								}
								
								try{
									potential_contacts_mon.enter();													
		
									outstanding[0]++;
									
								}finally{
									
									potential_contacts_mon.exit();
								}
								
								contact.isAlive(
									20*1000,
									new DistributedDatabaseListener()
									{
										public void 
										event(
											DistributedDatabaseEvent event) 
										{
											try{
												boolean	alive = event.getType() == DistributedDatabaseEvent.ET_OPERATION_COMPLETE;
													
												if ( listener.verbose()){
												
													listener.reportActivity( 
														getMessageText( alive?"report.alive":"report.dead",	contact.getName()));
												}
												
												try{
													potential_contacts_mon.enter();
													
													Object[]	entry = new Object[]{ new Boolean( alive ), contact};
													
													boolean	added = false;
													
													if ( alive ){
														
															// try and place before first dead entry 
												
														for (int i=0;i<potential_contacts.size();i++){
															
															if (!((Boolean)((Object[])potential_contacts.get(i))[0]).booleanValue()){
																
																potential_contacts.add(i, entry );
																
																added = true;
																
																break;
															}
														}
													}
													
													if ( !added ){
														
														potential_contacts.add( entry );	// dead at end
													}
														
												}finally{
														
													potential_contacts_mon.exit();
												}
											}finally{
												
												try{
													potential_contacts_mon.enter();													
		
													outstanding[0]--;
													
												}finally{
													
													potential_contacts_mon.exit();
												}
												
												potential_contacts_sem.release();
											}
										}
									});
							}
						};
						
					db.read(
						ddb_listener,
						db.createKey( hash, "Torrent download lookup for '" + ByteFormatter.encodeString( hash ) + "'" ),
						timeout,
						DistributedDatabase.OP_EXHAUSTIVE_READ | DistributedDatabase.OP_PRIORITY_HIGH );
										
					long 	overall_start 			= SystemTime.getMonotonousTime();					
					long 	last_found 				= -1;
										
					AsyncDispatcher	dispatcher = new AsyncDispatcher();
					
					while( remaining > 0 ){
						
						try{
							potential_contacts_mon.enter();
		
							if ( 	lookup_complete[0] && 
									potential_contacts.size() == 0 &&
									outstanding[0] == 0 ){
								
								break;
							}
						}finally{
							
							potential_contacts_mon.exit();
						}
										
						
						while( remaining > 0 ){
						
							if ( listener.cancelled()){
								
								return( null );
							}
							
							synchronized( md_result ){
								
								if ( md_result[0] != null ){
									
									return( md_result[0] );
								}
							}
											
							synchronized( fl_result ){
								
								if ( fl_result[0] != null ){
									
									return( fl_result[0] );
								}
							}
							
							long wait_start = SystemTime.getMonotonousTime();
		
							boolean got_sem = potential_contacts_sem.reserve( 1000 );
				
							long now = SystemTime.getMonotonousTime();
							
							remaining -= ( now - wait_start );
						
							if ( got_sem ){
							
								last_found = now;
								
								break;
								
							}else{
								
								if ( sl_enabled ){
									
									if ( secondary_lookup_time == -1 ){
									
										long	base_time;
										
										if ( last_found == -1 || now - overall_start > 60*1000 ){
											
											base_time = overall_start;
											
										}else{
											
											base_time = last_found;
										}
										
										long	time_so_far = now - base_time;
										
										if ( time_so_far > SECONDARY_LOOKUP_DELAY ){
											
											secondary_lookup_time = SystemTime.getMonotonousTime();
											
											doSecondaryLookup( listener, secondary_result, hash, args );
										}
									}else{
										
										try{
											byte[] torrent = getSecondaryLookupResult( secondary_result );
											
											if ( torrent != null ){
												
												return( torrent );
											}
										}catch( ResourceDownloaderException e ){
											
											// ignore, we just continue processing
										}
									}
								}
		
								continue;
							}
						}
						
						final DistributedDatabaseContact	contact;
						final boolean						live_contact;
						
						try{
							potential_contacts_mon.enter();
							
							// System.out.println( "rem=" + remaining + ",pot=" + potential_contacts.size() + ",out=" + outstanding[0] );
							
							if ( potential_contacts.size() == 0 ){
								
								if ( outstanding[0] == 0 ){
								
									break;
									
								}else{
									
									continue;
								}
							}else{
							
								Object[]	entry = (Object[])potential_contacts.remove(0);
								
								live_contact 	= ((Boolean)entry[0]).booleanValue(); 
								contact 		= (DistributedDatabaseContact)entry[1];
							}
							
						}finally{
							
							potential_contacts_mon.exit();
						}
							
						// System.out.println( "magnetDownload: " + contact.getName() + ", live = " + live_contact );
						
						final AESemaphore	contact_sem 	= new AESemaphore( "MD:contact" );
						final byte[][]		contact_data	= { null };
						
						dispatcher.dispatch(
							new AERunnable()
							{
								public void
								runSupport()
								{
									try{
										if ( !live_contact ){
											
											listener.reportActivity( getMessageText( "report.tunnel", contact.getName()));
						
											contact.openTunnel();
										}
										
										try{
											listener.reportActivity( getMessageText( "report.downloading", contact.getName()));
											
											DistributedDatabaseValue	value = 
												contact.read( 
														new DistributedDatabaseProgressListener()
														{
															public void
															reportSize(
																long	size )
															{
																listener.reportSize( size );
															}
															public void
															reportActivity(
																String	str )
															{
																listener.reportActivity( str );
															}
															
															public void
															reportCompleteness(
																int		percent )
															{
																listener.reportCompleteness( percent );
															}
														},
														db.getStandardTransferType( DistributedDatabaseTransferType.ST_TORRENT ),
														db.createKey ( hash , "Torrent download content for '" + ByteFormatter.encodeString( hash ) + "'"),
														timeout );
																
											if ( value != null ){
												
													// let's verify the torrent
												
												byte[]	data = (byte[])value.getValue(byte[].class);
						
												try{
													TOTorrent torrent = TOTorrentFactory.deserialiseFromBEncodedByteArray( data );
													
													if ( Arrays.equals( hash, torrent.getHash())){
													
														listener.reportContributor( contact.getAddress());
												
														synchronized( contact_data ){
															
															contact_data[0] = data;
														}												
													}else{
														
														listener.reportActivity( getMessageText( "report.error", "torrent invalid (hash mismatch)" ));
													}
												}catch( Throwable e ){
													
													listener.reportActivity( getMessageText( "report.error", "torrent invalid (decode failed)" ));
												}
											}
										}catch( Throwable e ){
											
											listener.reportActivity( getMessageText( "report.error", Debug.getNestedExceptionMessage(e)));
											
											Debug.printStackTrace(e);
										}
									}finally{
										
										contact_sem.release();
									}
								}
							});
						
						while( true ){
							
							if ( listener.cancelled()){
								
								return( null );
							}
							
							boolean got_sem = contact_sem.reserve( 500 );
														
							synchronized( contact_data ){
									
								if ( contact_data[0] != null ){
										
									return( contact_data[0] );
								}
							}
							
							synchronized( md_result ){
								
								if ( md_result[0] != null ){
									
									return( md_result[0] );
								}
							}
							
							synchronized( fl_result ){
								
								if ( fl_result[0] != null ){
									
									return( fl_result[0] );
								}
							}
							
							if ( got_sem ){
								
								break;
							}
						}
					}
				}else{
					
					if ( is_first_download ){
					
						listener.reportActivity( getMessageText( "report.ddb_disabled" ));
					}
				}
				
				if ( sl_enabled ){
					
					if ( secondary_lookup_time == -1 ){
						
						secondary_lookup_time = SystemTime.getMonotonousTime();
						
						doSecondaryLookup(listener, secondary_result, hash, args );
					}
					
					while( SystemTime.getMonotonousTime() - secondary_lookup_time < SECONDARY_LOOKUP_MAX_TIME ){
						
						if ( listener.cancelled()){
							
							return( null );
						}
						
						try{
							byte[] torrent = getSecondaryLookupResult( secondary_result );
							
							if ( torrent != null ){
								
								return( torrent );
							}
							
							synchronized( md_result ){
								
								if ( md_result[0] != null ){
									
									return( md_result[0] );
								}
							}
							
							synchronized( fl_result ){
								
								if ( fl_result[0] != null ){
									
									return( fl_result[0] );
								}
							}
							
							Thread.sleep( 500 );
							
						}catch( ResourceDownloaderException e ){
							
							break;
						}
					}
				}
				
				if ( md_enabled ){
				
					while( remaining > 0 ){

						if ( listener.cancelled()){
							
							return( null );
						}
						
						Thread.sleep( 500 );
						
						remaining -= 500;
						
						byte[] torrent = getSecondaryLookupResult( secondary_result );
						
						if ( torrent != null ){
							
							return( torrent );
						}
						
						synchronized( md_result ){
							
							if ( md_result[0] != null ){
								
								return( md_result[0] );
							}
						}
						
						synchronized( fl_result ){
							
							if ( fl_result[0] != null ){
								
								return( fl_result[0] );
							}
						}
					}
				}
				
				return( null );		// nothing found
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
				
				listener.reportActivity( getMessageText( "report.error", Debug.getNestedExceptionMessage(e)));
	
				throw( new MagnetURIHandlerException( "MagnetURIHandler failed", e ));
			}
		}finally{
			
			synchronized( md_downloader ){

				if ( md_delay_event != null ){
					
					md_delay_event.cancel();
					
					if ( md_downloader[0] != null ){
						
						 md_downloader[0].cancel();
					}
				}
			}
		}
	}
	
	protected void
	doSecondaryLookup(
		final MagnetPluginProgressListener		listener,
		final Object[]							result,
		byte[]									hash,
		String									args )
	{
		listener.reportActivity( getMessageText( "report.secondarylookup", null ));
		
		try{
			ResourceDownloaderFactory rdf = plugin_interface.getUtilities().getResourceDownloaderFactory();
		
			URL sl_url = new URL( SECONDARY_LOOKUP + "magnetLookup?hash=" + Base32.encode( hash ) + (args.length()==0?"":("&args=" + UrlUtils.encode( args ))));
			
			ResourceDownloader rd = rdf.create( sl_url );
			
			rd.addListener(
				new ResourceDownloaderAdapter()
				{
					public boolean
					completed(
						ResourceDownloader	downloader,
						InputStream			data )
					{
						listener.reportActivity( getMessageText( "report.secondarylookup.ok", null ));

						synchronized( result ){
						
							result[0] = data;
						}
						
						return( true );
					}
					
					public void
					failed(
						ResourceDownloader			downloader,
						ResourceDownloaderException e )
					{
						synchronized( result ){
							
							result[0] = e;
						}
						
						listener.reportActivity( getMessageText( "report.secondarylookup.fail" ));
					}
				});
			
			rd.asyncDownload();
			
		}catch( Throwable e ){
			
			listener.reportActivity( getMessageText( "report.secondarylookup.fail", Debug.getNestedExceptionMessage( e ) ));
		}
	}
	
	protected byte[]
	getSecondaryLookupResult(
		final Object[]	result )
	
		throws ResourceDownloaderException
	{
		if ( result == null ){
			
			return( null );
		}
		
		Object x;
		
		synchronized( result ){
			
			x = result[0];
			
			result[0] = null;
		}
			
		if ( x instanceof InputStream ){
			
			InputStream is = (InputStream)x;
				
			try{
				TOTorrent t = TOTorrentFactory.deserialiseFromBEncodedInputStream( is );
				
				TorrentUtils.setPeerCacheValid( t );
		
				return( BEncoder.encode( t.serialiseToMap()));
				
			}catch( Throwable e ){							
			}
		}else if ( x instanceof ResourceDownloaderException ){
			
			throw((ResourceDownloaderException)x);
		}
		
		return( null );
	}
	
	protected String
	getMessageText(
		String	resource )
	{
		return( plugin_interface.getUtilities().getLocaleUtilities().getLocalisedMessageText( "MagnetPlugin." + resource ));
	}
	
	protected String
	getMessageText(
		String	resource,
		String	param )
	{
		return( plugin_interface.getUtilities().getLocaleUtilities().getLocalisedMessageText( 
				"MagnetPlugin." + resource, new String[]{ param }));
	}
	
	public void
	addListener(
		MagnetPluginListener		listener )
	{
		listeners.add( listener );
	}
	
	public void
	removeListener(
		MagnetPluginListener		listener )
	{
		listeners.remove( listener );
	}
}
