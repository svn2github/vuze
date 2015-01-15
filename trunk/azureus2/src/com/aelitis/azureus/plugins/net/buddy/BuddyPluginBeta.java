/*
 * Created on Oct 13, 2014
 * Created by Paul Gardner
 * 
 * Copyright 2014 Azureus Software, Inc.  All rights reserved.
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


package com.aelitis.azureus.plugins.net.buddy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.AENetworkClassifier;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.ByteArrayHashMap;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimeFormatter;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TimerEventPeriodic;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.core3.xml.util.XUXmlWriter;
import org.gudy.azureus2.plugins.PluginEvent;
import org.gudy.azureus2.plugins.PluginEventListener;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ipc.IPCException;
import org.gudy.azureus2.plugins.sharing.ShareManager;
import org.gudy.azureus2.plugins.sharing.ShareManagerListener;
import org.gudy.azureus2.plugins.sharing.ShareResource;
import org.gudy.azureus2.plugins.sharing.ShareResourceDir;
import org.gudy.azureus2.plugins.sharing.ShareResourceFile;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;

import com.aelitis.azureus.core.proxy.impl.AEPluginProxyHandler;
import com.aelitis.azureus.core.tag.Tag;
import com.aelitis.azureus.core.tag.TagManagerFactory;
import com.aelitis.azureus.core.tag.TagType;
import com.aelitis.azureus.core.util.CopyOnWriteList;

public class
BuddyPluginBeta 
{
	public static final boolean DEBUG_ENABLED			= System.getProperty( "az.chat.buddy.debug", "0" ).equals( "1" );
	public static final boolean BETA_CHAN_ENABLED		= System.getProperty( "az.chat.buddy.beta.chan", "1" ).equals( "1" );

	public static final String	BETA_CHAT_KEY = 	"test:beta:chat";
	
	public static final int PRIVATE_CHAT_DISABLED			= 1;
	public static final int PRIVATE_CHAT_PINNED_ONLY		= 2;
	public static final int PRIVATE_CHAT_ENABLED			= 3;
	
	private static final String	FLAGS_MSG_STATUS_KEY		= "s";
	private static final int 	FLAGS_MSG_STATUS_CHAT_NONE	= 0;		// def
	private static final int 	FLAGS_MSG_STATUS_CHAT_QUIT	= 1;
	
	public static final String 	FLAGS_MSG_ORIGIN_KEY 		= "o";
	public static final int 	FLAGS_MSG_ORIGIN_USER 		= 0;		// def
	public static final int 	FLAGS_MSG_ORIGIN_RATINGS 	= 1;
	
	
	private BuddyPlugin			plugin;
	private PluginInterface		plugin_interface;
	private BooleanParameter	enabled;
	
	private AsyncDispatcher		dispatcher = new AsyncDispatcher( "BuddyPluginBeta" );
	
	private Map<String,ChatInstance>		chat_instances_map 	= new HashMap<String, BuddyPluginBeta.ChatInstance>();
	private CopyOnWriteList<ChatInstance>	chat_instances_list	= new CopyOnWriteList<BuddyPluginBeta.ChatInstance>();
	
	private PluginInterface azmsgsync_pi;

	private TimerEventPeriodic		timer;
	
	private String					shared_public_nickname;
	private String					shared_anon_nickname;
	private int						private_chat_state;
	private boolean					shared_anon_endpoint;
	private boolean					sound_enabled;
	private String					sound_file;
	
	private Map<String,Long>		favourite_map;
	private Map<String,Long>		save_messages_map;
	private Map<String,byte[]>		lmi_map;
	
	private CopyOnWriteList<FTUXStateChangeListener>		ftux_listeners = new CopyOnWriteList<FTUXStateChangeListener>();
	
	private boolean	ftux_accepted = false;
	
	private CopyOnWriteList<ChatManagerListener>		listeners = new CopyOnWriteList<ChatManagerListener>();
	
	private AtomicInteger		private_chat_id = new AtomicInteger();
	
	private AESemaphore	init_complete = new AESemaphore( "bpb:init" );
	
	protected
	BuddyPluginBeta(
		PluginInterface		_pi,
		BuddyPlugin			_plugin,

		BooleanParameter	_enabled )
	{
		plugin_interface 	= _pi;
		plugin				= _plugin;
		enabled				= _enabled;
		
		ftux_accepted 	= COConfigurationManager.getBooleanParameter( "azbuddy.dchat.ftux.accepted", false );

		shared_public_nickname 	= COConfigurationManager.getStringParameter( "azbuddy.chat.shared_nick", "" );
		shared_anon_nickname 	= COConfigurationManager.getStringParameter( "azbuddy.chat.shared_anon_nick", "" );
		private_chat_state	 	= COConfigurationManager.getIntParameter( "azbuddy.chat.private_chat_state", PRIVATE_CHAT_ENABLED );
		
		shared_anon_endpoint	= COConfigurationManager.getBooleanParameter( "azbuddy.chat.share_i2p_endpoint", true );
		sound_enabled			= COConfigurationManager.getBooleanParameter( "azbuddy.chat.notif.sound.enable", true );
		sound_file			 	= COConfigurationManager.getStringParameter( "azbuddy.chat.notif.sound.file", "" );
	
		favourite_map			= COConfigurationManager.getMapParameter( "azbuddy.dchat.favemap", new HashMap<String,Long>());
		save_messages_map		= COConfigurationManager.getMapParameter( "azbuddy.dchat.savemsgmap", new HashMap<String,Long>());
		lmi_map					= COConfigurationManager.getMapParameter( "azbuddy.dchat.lmimap", new HashMap<String,byte[]>());
				
		SimpleTimer.addPeriodicEvent(
			"BPB:checkfave",
			30*1000,
			new TimerEventPerformer() 
			{		
				public void 
				perform(
					TimerEvent event ) 
				{
					checkFavourites();
				}
			});
	}
	
	public boolean 
	isAvailable()
	{
		return( plugin_interface.getPluginManager().getPluginInterfaceByID( "azmsgsync", true ) != null );
	}
	
	public boolean
	isInitialised()
	{
		return( init_complete.isReleasedForever());
	}
	
	public boolean
	getFavourite(
		String		net,
		String		key )
	{
		synchronized( favourite_map ){
			
			Long l = favourite_map.get( net + ":" + key );
			
			if ( l == null ){
				
				return( false );
			}
			
			return ( l == 1 );
		}
	}
	
	public void
	setFavourite(
		String		net,
		String		key,
		boolean		b )
	{
		synchronized( favourite_map ){
			
			String net_key = net + ":" + key;
			
			Long existing = favourite_map.get( net_key );
			
			if ( existing == null && !b ){
				
				return;
			}
			
			if ( existing != null && b == ( existing == 1 )){
				
				return;
			}
			
			if ( b ){
				
				favourite_map.put( net_key, 1L );
				
			}else{
				
				favourite_map.remove( net_key );
			}
			
			COConfigurationManager.setParameter( "azbuddy.dchat.favemap", favourite_map );	
		}
		
		COConfigurationManager.save();
		
		checkFavourites();
	}
	
	public List<String[]>
	getFavourites()
	{
		synchronized( favourite_map ){
			
			List<String[]>	result = new ArrayList<String[]>();
			
			for ( Map.Entry<String, Long> entry: favourite_map.entrySet()){
				
				String 	net_key = entry.getKey();
				Long	value	= entry.getValue();
				
				if ( value == 1 ){
					
					String[] bits = net_key.split( ":", 2 );
					
					String network 	= AENetworkClassifier.internalise( bits[0] );
					String key		= bits[1];
					
					result.add( new String[]{ network, key });
				}
			}
			
			return( result );
		}
	}
	
	private void
	checkFavourites()
	{
		dispatcher.dispatch(
			new AERunnable() 
			{	
				@Override
				public void 
				runSupport() 
				{
					try{
						List<String[]>	faves = getFavourites();
						
						Set<String>	set = new HashSet<String>();
						
						for ( String[] fave: faves ){
							
							String	net = fave[0];
							String	key	= fave[1];
							
							set.add( net + ":" + key );
							
							ChatInstance chat = peekChatInstance( net, key, false );
	
							if ( chat == null || !chat.getKeepAlive()){
							
									// get a reference to the chat
								
								try{
									chat = getChat( net, key );
								
									chat.setKeepAlive( true );
									
								}catch( Throwable e ){
									
								}
							}
						}
						
						for ( ChatInstance chat: chat_instances_list ){
							
							if ( chat.getKeepAlive()){
								
								String	net = chat.getNetwork();
								String	key = chat.getKey();
								
								if ( !set.contains( net + ":" + key )){
									
									if ( 	net == AENetworkClassifier.AT_PUBLIC &&
											key.equals(BETA_CHAT_KEY)){
										
										// leave
										
									}else{
										
											// release our reference
										
										chat.setKeepAlive( false );
										
										chat.destroy();
									}
								}
							}
						}
					}finally{
						
						init_complete.releaseForever();
					}
				}
			});
	}
	
	public boolean
	getSaveMessages(
		String		net,
		String		key )
	{
		synchronized( save_messages_map ){
			
			Long l = save_messages_map.get( net + ":" + key );
			
			if ( l == null ){
				
				return( false );
			}
			
			return ( l == 1 );
		}
	}
	
	public void
	setSaveMessages(
		String		net,
		String		key,
		boolean		b )
	{
		synchronized( save_messages_map ){
			
			String net_key = net + ":" + key;
			
			Long existing = save_messages_map.get( net_key );
			
			if ( existing == null && !b ){
				
				return;
			}
			
			if ( existing != null && b == ( existing == 1 )){
				
				return;
			}
			
			if ( b ){
				
				save_messages_map.put( net_key, 1L );
				
			}else{
				
				save_messages_map.remove( net_key );
			}
			
			COConfigurationManager.setParameter( "azbuddy.dchat.savemsgmap", save_messages_map );	
		}
		
		COConfigurationManager.save();
	}
	
	public String
	getLastMessageInfo(
		String		net,
		String		key )
	{
		synchronized( lmi_map ){
			
			byte[] info = lmi_map.get( net + ":" + key );
			
			if ( info != null ){
							
				try{
					return( new String( info, "UTF-8" ));
				
				}catch( Throwable e ){
				}
			}
			
			return( null );
		}
	}
	
	public void
	setLastMessageInfo(
		String		net,
		String		key,
		String		info )
	{
		synchronized( lmi_map ){
			
			String net_key = net + ":" + key;
			
			try{
				lmi_map.put( net_key, info.getBytes( "UTF-8" ));
			
				COConfigurationManager.setParameter( "azbuddy.dchat.lmimap", lmi_map );
				
			}catch( Throwable e ){
			}
		}
		
		COConfigurationManager.setDirty();
	}
	
	
	
	public String
	getSharedPublicNickname()
	{
		return( shared_public_nickname );
	}
	
	public void
	setSharedPublicNickname(
		String		_nick )
	{
		if ( !_nick.equals( shared_public_nickname )){
			
			shared_public_nickname	= _nick;
		
			COConfigurationManager.setParameter( "azbuddy.chat.shared_nick", _nick );
			
			allUpdated();		
		}	
	}
	
	public String
	getSharedAnonNickname()
	{
		return( shared_anon_nickname );
	}
	
	public void
	setSharedAnonNickname(
		String		_nick )
	{
		if ( !_nick.equals( shared_anon_nickname )){
			
			shared_anon_nickname	= _nick;
		
			COConfigurationManager.setParameter( "azbuddy.chat.shared_anon_nick", _nick );
			
			allUpdated();		
		}	
	}
	
	public int
	getPrivateChatState()
	{
		return( private_chat_state );
	}
	
	public void
	setPrivateChatState(
		int		state )
	{
		if ( state !=  private_chat_state ){
			
			private_chat_state	= state;
		
			COConfigurationManager.setParameter( "azbuddy.chat.private_chat_state", state );
			
			plugin.fireUpdated();
		}	
	}
	
	public boolean
	getSharedAnonEndpoint()
	{
		return( shared_anon_endpoint );
	}
	
	public void
	setSharedAnonEndpoint(
		boolean		b )
	{
		if ( b !=  shared_anon_endpoint ){
			
			shared_anon_endpoint	= b;
		
			COConfigurationManager.setParameter( "azbuddy.chat.share_i2p_endpoint", b );
			
			plugin.fireUpdated();
		}	
	}
	
	public void
	setSoundEnabled(
		boolean		b )
	{
		if ( b !=  sound_enabled ){
			
			sound_enabled	= b;
		
			COConfigurationManager.setParameter( "azbuddy.chat.notif.sound.enable", b );
			
			plugin.fireUpdated();
		}	
	}
	
	public boolean
	getSoundEnabled()
	{
		return( sound_enabled );
	}
	
	public String
	getSoundFile()
	{
		return( sound_file );
	}
	
	public void
	setSoundFile(
		String		_file )
	{
		if ( !_file.equals( sound_file )){
			
			sound_file	= _file;
		
			COConfigurationManager.setParameter( "azbuddy.chat.notif.sound.file", _file );
			
			plugin.fireUpdated();	
		}	
	}
	
	private void
	allUpdated()
	{
		for ( ChatInstance chat: chat_instances_list ){
			
			chat.updated();
		}
		
		plugin.fireUpdated();
	}
	
	protected void
	startup()
	{						
		plugin_interface.addEventListener(
			new PluginEventListener()
			{
				public void 
				handleEvent(
					PluginEvent ev )
				{
					int	type = ev.getType();
					
					if ( type == PluginEvent.PEV_INITIAL_SHARING_COMPLETE ){
						
						try{
							ShareManager share_manager = plugin_interface.getShareManager();
							
							share_manager.addListener(
								new ShareManagerListener() {
									
									public void 
									resourceModified(
										ShareResource old_resource,
										ShareResource new_resource ) 
									{
										checkTag( new_resource );
									}
									
									public void 
									resourceDeleted(
										ShareResource resource ) 
									{
									}
									
									public void 
									resourceAdded(
										ShareResource resource )
									{
										checkTag( resource );
									}
									
									public void 
									reportProgress(
										int percent_complete )
									{
									}
									
									public void 
									reportCurrentTask(
										String task_description ) 
									{				}
								});
							
							ShareResource[] existing = share_manager.getShares();
							
							for ( ShareResource sr: existing ){
								
								checkTag( sr );
							}
						}catch( Throwable e ){
							
							Debug.out( e );
						}
					}else if ( type == PluginEvent.PEV_PLUGIN_OPERATIONAL ){
						
						pluginAdded((PluginInterface)ev.getValue());
						
					}else  if ( type == PluginEvent.PEV_PLUGIN_NOT_OPERATIONAL ){
						
						pluginRemoved((PluginInterface)ev.getValue());
					}
				}
			});
		
		PluginInterface[] plugins = plugin_interface.getPluginManager().getPlugins( true );
		
		for ( PluginInterface pi: plugins ){
			
			if ( pi.getPluginState().isOperational()){
			
				pluginAdded( pi );
			}
		}
	}
	
	private void
	checkTag(
		ShareResource		resource )
	{
		Map<String,String>	properties = resource.getProperties();
		
		if ( properties != null ){
			
			String ud = properties.get( ShareManager.PR_USER_DATA );
			
			if ( ud.equals( "buddyplugin:share" )){
				
				try{

					Torrent torrent = null;
				
					if ( resource instanceof ShareResourceFile ){
						
						torrent = ((ShareResourceFile)resource).getItem().getTorrent();
						
					}else if ( resource instanceof ShareResourceDir ){
						
						torrent = ((ShareResourceDir)resource).getItem().getTorrent();
					}
					
					if ( torrent != null ){
						
						Download download = plugin_interface.getPluginManager().getDefaultPluginInterface().getShortCuts().getDownload( torrent.getHash());
	
						if ( download != null ){
							
							tagDownload( download );
						}
					}
				}catch( Throwable e ){
					
				}
			}
		}
	}
	
	public void
	tagDownload(
		Download	download )
	{
		try{
			TagType tt = TagManagerFactory.getTagManager().getTagType( TagType.TT_DOWNLOAD_MANUAL );
			
			Tag tag = tt.getTag( "tag.azbuddy.dchat.shares", false );
			
			if ( tag == null ){
				
				tag = tt.createTag( "tag.azbuddy.dchat.shares", true );
				
				tag.setCanBePublic( false );
				
				tag.setPublic( false );
			}
			
			tag.addTaggable( PluginCoreUtils.unwrap( download ));
			
		}catch( Throwable e ){
		
			Debug.out( e );
		}	
	}
	
	protected void
	closedown()
	{
		
	}
		
	private void
	pluginAdded(
		final PluginInterface	pi )
	{
		if ( pi.getPluginID().equals( "azmsgsync" )){
			
			synchronized( chat_instances_map ){

				azmsgsync_pi = pi;
				
				Iterator<ChatInstance>	it = chat_instances_map.values().iterator();
					
				while( it.hasNext()){
					
					ChatInstance inst = it.next();
					
					try{
						inst.bind( azmsgsync_pi, null );
						
					}catch( Throwable e ){
						
						Debug.out( e );
						
						it.remove();
					}
				}
			}
			
			dispatcher.dispatch(
				new AERunnable() {
					
					@Override
					public void 
					runSupport() 
					{
						try{
							if ( Constants.isCVSVersion() && enabled.getValue()){
								
								if ( BETA_CHAN_ENABLED ){
								
									ChatInstance chat = getChat( AENetworkClassifier.AT_PUBLIC, BETA_CHAT_KEY );
								
									chat.setKeepAlive( true );
								}
							}	
						}catch( Throwable e ){
							
							Debug.out( e );
						}
					}
				});

		}
	}
	
	private void
	pluginRemoved(
		PluginInterface	pi )
	{
		if ( pi.getPluginID().equals( "azmsgsync" )){
			
			synchronized( chat_instances_map ){

				azmsgsync_pi = null;
						
				Iterator<ChatInstance>	it = chat_instances_map.values().iterator();
				
				while( it.hasNext()){
					
					ChatInstance inst = it.next();
					
					inst.unbind();
					
					if ( inst.isPrivateChat()){
						
						it.remove();
					}	
				}
			}
		}
	}
		
	public boolean
	isI2PAvailable()
	{
		return( AEPluginProxyHandler.hasPluginProxyForNetwork( AENetworkClassifier.AT_I2P, false ));
	}
	
	public InputStream
	handleURI(
		String		url_str,
		boolean		open_only )
		
		throws Exception
	{
		
			// url_str will be something like chat:anon?Test%20Me[&a=b]...
			// should really be chat:anon?key=Test%20Me but we'll support the shorthand
		
			// azplug:?id=azbuddy&arg=chat%3A%3FTest%2520Me%26format%3Drss
			// azplug:?id=azbuddy&arg=chat%3A%3Fkey%3DTest%2520Me%26format%3Drss
				
		int	pos = url_str.indexOf( '?' );
		
		String protocol;
		String key 		= null;
		String format	= null;
		
		if ( pos != -1 ){
			
			protocol = url_str.substring( 0, pos ).toLowerCase( Locale.US );
			
			String args = url_str.substring( pos+1 );
			
			String[] bits = args.split( "&" );
			
			for ( String bit: bits ){
				
				String[] temp = bit.split( "=" );
				
				if ( temp.length == 1 ){
					
					key = UrlUtils.decode( temp[0] );
					
				}else{
					
					String lhs = temp[0].toLowerCase( Locale.US );
					String rhs = UrlUtils.decode( temp[1] );
					
					if ( lhs.equals( "key" )){
						
						key = rhs;
						
					}else if ( lhs.equals( "format" )){
						
						format	= rhs;
					}
				}
			}
					
		}else{
			
			throw( new Exception( "Malformed request" ));
		}
		
		if ( key == null ){
			
			throw( new Exception( "Key missing" ));
		}
		
		if ( open_only ){
			
			format = null;
		}
		
		String network;
		
		if ( protocol.startsWith( "chat:anon" )){
				
			if ( !isI2PAvailable()){
				
				throw( new Exception( "I2P unavailable" ));
			}
			
			network = AENetworkClassifier.AT_I2P;
			
		}else if ( protocol.startsWith( "chat" )){
			
			network = AENetworkClassifier.AT_PUBLIC;
			
		}else{
		
			throw( new Exception( "Invalid protocol: " + protocol ));
		}
		
		if ( format == null || !format.equalsIgnoreCase( "rss" )){
		
			BuddyPluginViewInterface ui = plugin.getSWTUI();

			if ( ui == null ){
				
				throw( new Exception( "UI unavailable" ));
			}
		
			ChatInstance chat = getChat( network, key);

			ui.openChat( chat );
		
			return( null );
			
		}else{
			
			ChatInstance chat = peekChatInstance( network, key, true );
			
			if ( chat == null ){
				
				throw( new Exception( "Chat unavailable" ));
			}
			
				// we need this chat to hang around
			
			if ( !chat.isFavourite()){
				
				chat.setFavourite( true );
				
				chat.setKeepAlive( true );
			}
			
			if ( !chat.getSaveMessages()){
				
				chat.setSaveMessages( true );
			}
			
			List<ChatMessage> messages = chat.getMessages();
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream( 10*1024 );
			
			PrintWriter pw = new PrintWriter( new OutputStreamWriter( baos, "UTF-8" ));
			
			pw.println( "<?xml version=\"1.0\" encoding=\"utf-8\"?>" );
			
			pw.println( "<rss version=\"2.0\" xmlns:vuze=\"http://www.vuze.com\">" );
			
			pw.println( "<channel>" );
			
			pw.println( "<title>" + escape( chat.getName()) + "</title>" );
					
			long	last_modified;
			
			if ( messages.size() == 0 ){
				
				last_modified = SystemTime.getCurrentTime();
				
			}else{
				
				last_modified = messages.get( messages.size()-1).getTimeStamp();
			}
										
			pw.println(	"<pubDate>" + TimeFormatter.getHTTPDate( last_modified ) + "</pubDate>" );
		
			for ( ChatMessage message: messages ){
								
				List<Map<String,Object>>	magnets = extractMagnets( message.getMessage());
				
				if ( magnets.size() == 0 ){
					
					continue;
				}
				
				String item_date = TimeFormatter.getHTTPDate( message.getTimeStamp());

				for ( Map<String,Object> magnet: magnets ){
					
					String	hash 	= (String)magnet.get( "hash" );
					
					if ( hash == null ){
						
						continue;
					}
					
					String	title 	= (String)magnet.get( "title" );
					
					if ( title == null ){
						
						title = hash;
					}
				
					String	link	= (String)magnet.get( "link" );
					
					if ( link == null ){
						
						link = (String)magnet.get( "magnet" );
					}
					
					pw.println( "<item>" );
				
					pw.println( "<title>" + escape( title ) + "</title>" );
				
					pw.println( "<guid>" + hash + "</guid>" );
				
					String	cdp	= (String)magnet.get( "cdp" );

					if ( cdp != null ){
						
						pw.println( "<link>" + escape( cdp ) + "</link>" );
					}
					
					Long	size 		= (Long)magnet.get( "size" );
					Long	seeds 		= (Long)magnet.get( "seeds" );
					Long	leechers 	= (Long)magnet.get( "leechers" );
					Long	date	 	= (Long)magnet.get( "date" );

					String enclosure = 
							"<enclosure " + 
								"type=\"application/x-bittorrent\" " +
								"url=\"" + escape( link ) + "\"";
					
					if ( size != null ){
						
						enclosure += " length=\"" + size + "\"";
					}
					
					enclosure += " />";
					
					pw.println( enclosure );
							
					String date_str = (date==null||date<=0)?item_date:TimeFormatter.getHTTPDate( date );
					
					pw.println(	"<pubDate>" + date_str + "</pubDate>" );
				
					
					if ( size != null ){
						
						pw.println(	"<vuze:size>" + size + "</vuze:size>" );
					}
					
					if ( seeds != null ){
						
						pw.println(	"<vuze:seeds>" + seeds + "</vuze:seeds>" );
					}
					
					if ( leechers != null ){
						
						pw.println(	"<vuze:peers>" + leechers + "</vuze:peers>" );
					}
					
					pw.println(	"<vuze:assethash>" + hash + "</vuze:assethash>" );
												
					pw.println( "<vuze:downloadurl>" + escape( link ) + "</vuze:downloadurl>" );
				
					pw.println( "</item>" );
				}
			}
			
			pw.println( "</channel>" );
			
			pw.println( "</rss>" );
			
			pw.flush();
			
			return( new ByteArrayInputStream( baos.toByteArray()));
		}
	}
	
	private List<Map<String,Object>>
	extractMagnets(
		String		str )
	{
		List<Map<String,Object>> result = new ArrayList<Map<String,Object>>();
		
		int	len = str.length();
		
		String	lc_str = str.toLowerCase( Locale.US );
		
		int	pos = 0;
		
		while( pos < len ){
			
			pos = lc_str.indexOf( "magnet:", pos );
			
			if ( pos == -1 ){
				
				break;
			}
			
			int	start = pos;
			
			while( pos < len ){
				
				if ( Character.isWhitespace( str.charAt( pos ))){
					
					break;
					
				}else{
					
					pos++;
				}
			}
			
			String magnet = str.substring( start, pos );
			
			int x = magnet.indexOf( '?' );
			
			if ( x != -1 ){
			
				Map<String,Object> map = new HashMap<String,Object>();
				
					// remove any trailing ui name hack
				
				int	p1 = magnet.lastIndexOf( "[[" );
				
				if ( p1 != -1 && magnet.endsWith( "]]" )){
					
					magnet = magnet.substring( 0, p1 );
				}
				
				map.put( "magnet", magnet );
				
				List<String>	trackers = new ArrayList<String>();
				
				map.put( "trackers", trackers );
				
				String[] bits = magnet.substring( x+1 ).split( "&" );
				
				for ( String bit: bits ){
					
					String[] temp = bit.split( "=" );
					
					if ( temp.length == 2 ){
						
						try{

							String	lhs = temp[0].toLowerCase( Locale.US );
							String	rhs = UrlUtils.decode( temp[1] );
							
							if ( lhs.equals( "xt" )){
								
								String lc_rhs = rhs.toLowerCase( Locale.US );
								
								int p = lc_rhs.indexOf( "btih:" );
								
								if ( p >= 0 ){
									
									map.put( "hash", lc_rhs.substring( p+5 ).toUpperCase( Locale.US ));
								}
								
							}else if ( lhs.equals( "dn" )){
								
								map.put( "title", rhs );
								
							}else if ( lhs.equals( "tr" )){
								
								trackers.add( rhs );
								
							}else if ( lhs.equals( "fl" )){
								
								map.put( "link", rhs );
								
							}else if ( lhs.equals( "xl" )){
								
								long size = Long.parseLong( rhs );
								
								map.put( "size", size );
								
							}else if ( lhs.equals( "_d" )){
								
								long date = Long.parseLong( rhs );
								
								map.put( "date", date );

							}else if ( lhs.equals( "_s" )){
								
								long seeds = Long.parseLong( rhs );
								
								map.put( "seeds", seeds );
								
							}else if ( lhs.equals( "_l" )){
								
								long leechers = Long.parseLong( rhs );
								
								map.put( "leechers", leechers );
								
							}else if ( lhs.equals( "_c" )){
							
								map.put( "cdp", rhs );
							}
						}catch( Throwable e ){
							
						}

					}
				}
			
				//System.out.println( magnet + " -> " + map );
				
				result.add( map );
			}
		}
		
		return( result );
	}
	
	private String
	escape(
		String	str )
	{
		return( XUXmlWriter.escapeXML(str));
	}
	
	public boolean
	getFTUXAccepted()
	{
		return( ftux_accepted );
	}
	
	public void
	setFTUXAccepted(
		boolean	accepted )
	{
		ftux_accepted = accepted;
		
		COConfigurationManager.setParameter( "azbuddy.dchat.ftux.accepted", true );
		
		COConfigurationManager.save();
		
		for ( FTUXStateChangeListener l: ftux_listeners ){
			
			l.stateChanged( accepted );
		}
	}
	
	public void
	addFTUXStateChangeListener(
		FTUXStateChangeListener		listener )
	{
		ftux_listeners.add( listener );
		
		listener.stateChanged( ftux_accepted );
	}
	
	public void
	removeFTUXStateChangeListener(
		FTUXStateChangeListener		listener )
	{
		ftux_listeners.remove( listener );
	}
	
	public void
	getAndShowChat(
		String		network,
		String		key )
		
		throws Exception
	{
		BuddyPluginViewInterface ui = plugin.getSWTUI();
		
		if ( ui == null ){
			
			throw( new Exception( "UI unavailable" ));
		}
				
		ChatInstance chat = getChat( network, key) ;
			
		ui.openChat( chat );
	}
	
	public void
	showChat(
		ChatInstance	inst )
		
		throws Exception
	{
		BuddyPluginViewInterface ui = plugin.getSWTUI();
		
		if ( ui == null ){
			
			throw( new Exception( "UI unavailable" ));
		}
							
		ui.openChat( inst );
	}
	
	private String
	pkToString(
		byte[]		pk )
	{		
		byte[] temp = new byte[3];
		
		if ( pk != null ){
			
			System.arraycopy( pk, 8, temp, 0, 3 );
		}
		
		return( ByteFormatter.encodeString( temp ));
	}
	
	public ChatInstance
	importChat(
		String		import_data )
		
		throws Exception
	{
		if ( azmsgsync_pi == null ){
			
			throw( new Exception( "Plugin unavailable " ));
		}
		
		Map<String,Object>		options = new HashMap<String, Object>();
				
		options.put( "import_data", import_data.getBytes( "UTF-8" ));
	
		Map<String,Object> reply = (Map<String,Object>)azmsgsync_pi.getIPC().invoke( "importMessageHandler", new Object[]{ options } );

		String	key			= new String((byte[])reply.get( "key" ), "UTF-8" );
		String	network	 	= (String)reply.get( "network" );
		Object	handler 	= reply.get( "handler" );
		
		return( getChat( network, key, null, handler, false ));
	}

	public String
	getDownloadKey(
		Download		download )
	{
		Torrent torrent = download.getTorrent();
		
		if ( torrent == null ){
			
			return( null );
		}
		
			// use torrent name here to canonicalize things in case user has renamed download display name
			// also it is more important to get a consistent string rather than something correctly localised
		
		String	torrent_name = null;
		
		try{
			TOTorrent to_torrent = PluginCoreUtils.unwrap( torrent );
			
			torrent_name = to_torrent.getUTF8Name();
			
			if ( torrent_name == null ){
				
				torrent_name = new String( to_torrent.getName(), "UTF-8" );
			}
		}catch( Throwable e ){
			
		}
		
		if ( torrent_name == null ){
			
			torrent_name = torrent.getName();
		}
		
		String key = "Download: " + torrent_name + " {" + ByteFormatter.encodeString( download.getTorrentHash()) + "}";

		return( key );
	}
	
	public ChatInstance
	getChat(
		Download		download )
	{
		String	key = getDownloadKey( download );

		if ( key != null ){
			
			String[] networks = PluginCoreUtils.unwrap( download ).getDownloadState().getNetworks();
			
			boolean	has_i2p = false;
			
			for ( String net: networks ){
				
				if ( net == AENetworkClassifier.AT_PUBLIC ){
					
					try{
						ChatInstance inst = getChat( net, key );
						
						return( inst );
						
					}catch( Throwable e ){
						
					}
				}else if ( net == AENetworkClassifier.AT_I2P ){
					
					has_i2p = true;
				}
			}
			
			if ( has_i2p ){
				
				try{
					ChatInstance inst = getChat( AENetworkClassifier.AT_I2P, key );
					
					return( inst );
										
				}catch( Throwable e ){
					
				}
			}
		}
		
		return( null );
	}
	
	public ChatInstance
	getChat(
		String			network,
		String			key )	
		
		throws Exception
	{
		return( getChat( network, key, null, null, false ));
	}
	
	public ChatInstance
	getChat(
		ChatParticipant		participant )
		
		throws Exception
	{
		String key = participant.getChat().getKey() + " - " + participant.getName() + " (outgoing)[" + private_chat_id.getAndIncrement() + "]";
		
		return( getChat( participant.getChat().getNetwork(), key, participant, null, true ));
	}
	
	public ChatInstance
	getChat(
		ChatParticipant	parent_participant,
		Object			handler )
		
		throws Exception
	{
		String key = parent_participant.getChat().getKey() + " - " + parent_participant.getName() + " (incoming)[" + private_chat_id.getAndIncrement() + "]";

		return( getChat( parent_participant.getChat().getNetwork(), key, null, handler, true ));
	}
	
	private ChatInstance
	getChat(
		String				network,
		String				key,
		ChatParticipant		private_target,
		Object				handler,
		boolean				is_private_chat )
		
		throws Exception
	{
		if ( !enabled.getValue()){
			
			throw( new Exception( "Plugin not enabled" ));
		}
		
		String meta_key = network + ":" + key;
	
		ChatInstance 	result;
		
		ChatInstance	added = null;
		
		synchronized( chat_instances_map ){
			
			result = chat_instances_map.get( meta_key );
			
			if ( result == null ){
							
				result = new ChatInstance( network, key, private_target, is_private_chat );
			
				chat_instances_map.put( meta_key, result );
				
				chat_instances_list.add( result );
				
				added = result;
				
				if ( azmsgsync_pi != null ){
					
					try{
						result.bind( azmsgsync_pi, handler );
						
					}catch( Throwable e ){
						
						chat_instances_map.remove( meta_key );
						
						chat_instances_list.remove( result );
						
						added = null;
						
						result.destroy();
						
						if ( e instanceof Exception ){
							
							throw((Exception)e);
						}
						
						throw( new Exception( e ));
					}
				}
			}else{
				
				result.addReference();
			}
			
			if ( timer == null ){
				
				timer = 
					SimpleTimer.addPeriodicEvent(
						"BPB:timer",
						2500,
						new TimerEventPerformer() {
							
							public void 
							perform(
								TimerEvent event ) 
							{									
								for ( ChatInstance inst: chat_instances_list ){
										
									inst.update();
								}
							}
						});
			}			
		}
		
		if ( added != null ){
			
			for ( ChatManagerListener l: BuddyPluginBeta.this.listeners ){
				
				try{
					l.chatAdded( added );
					
				}catch( Throwable e ){
					
					Debug.out( e );;
				}
			}
		}
		
		return( result );
	}
	
	/**
	 * returns existing chat if found without adding a reference to it. If create_if_missing supplied
	 * then this will create a new chat (and add a reference to it) so use this parameter with
	 * caution
	 */
	
	private ChatInstance
	peekChatInstance(
		String				network,
		String				key,
		boolean				create_if_missing )
	{
		String meta_key = network + ":" + key;
	
		synchronized( chat_instances_map ){
			
			ChatInstance inst = chat_instances_map.get( meta_key );
			
			if ( inst == null && create_if_missing ){
				
				try{
					inst = getChat( network, key );
					
				}catch( Throwable e ){
					
				}
			}
			
			return( inst );
		}
	}
	
	public Map<String,Object>
	peekChat(
		Download		download )
	{
		String	key = getDownloadKey( download );

		if ( key != null ){
			
			String[] networks = PluginCoreUtils.unwrap( download ).getDownloadState().getNetworks();
			
			boolean	has_i2p = false;
			
			for ( String net: networks ){
				
				if ( net == AENetworkClassifier.AT_PUBLIC ){
					
					try{
						return( peekChat( net, key ));
												
					}catch( Throwable e ){
						
					}
				}else if ( net == AENetworkClassifier.AT_I2P ){
					
					has_i2p = true;
				}
			}
			
			if ( has_i2p ){
				
				try{
					return( peekChat( AENetworkClassifier.AT_I2P, key ));
															
				}catch( Throwable e ){
					
				}
			}
		}
		
		return( null );
	}
	
	public Map<String,Object>
	peekChat(
		String				network,
		String				key )
	{
		Map<String,Object>		reply = new HashMap<String, Object>();
		
		try{
			PluginInterface pi;
		
			synchronized( chat_instances_map ){

				pi = azmsgsync_pi;
			}
			
			if ( pi != null ){
				
				Map<String,Object>		options = new HashMap<String, Object>();
				
				options.put( "network", network );
				options.put( "key", key.getBytes( "UTF-8" ));

				options.put( "timeout", 60*1000 );
				
				reply = (Map<String,Object>)pi.getIPC().invoke( "peekMessageHandler", new Object[]{ options } );
			}
				
		}catch( Throwable e ){
			
			Debug.out( e );
		}
		
		return( reply );
	}
	
	public List<ChatInstance>
	getChats()
	{
		return( chat_instances_list.getList());
	}
	
	public void
	addListener(
		ChatManagerListener		l,
		boolean					fire_for_existing )
	{
		listeners.add( l );
		
		if ( fire_for_existing ){
			
			for ( ChatInstance inst: chat_instances_list ){
				
				l.chatAdded( inst );
			}
		}
	}
	
	public void
	removeListener(
		ChatManagerListener		l )
	{
		listeners.remove( l );
	}
	
	public class
	ChatInstance
	{
		private static final int	MSG_HISTORY_MAX	= 512;
		
		private final String		network;
		private final String		key;
		
		private boolean				is_private_chat;
		
		private final ChatParticipant		private_target;
		
		private Object		binding_lock = new Object();
		private AESemaphore	binding_sem;
		
		private volatile PluginInterface		msgsync_pi;
		private volatile Object					handler;
		
		private byte[]							my_public_key;
		private byte[]							managing_public_key;
		private boolean							read_only;
		private int								ipc_version;
		
		private InetSocketAddress				my_address;
		
		private Object	chat_lock = this;
		
		private AtomicInteger						message_uid_next = new AtomicInteger();
		
		private List<ChatMessage>					messages	= new ArrayList<ChatMessage>();
		private ByteArrayHashMap<String>			message_ids = new ByteArrayHashMap<String>();
		private int									messages_not_mine_count;
		
		private ByteArrayHashMap<ChatParticipant>	participants = new ByteArrayHashMap<ChatParticipant>();
		
		private Map<String,List<ChatParticipant>>	nick_clash_map = new HashMap<String, List<ChatParticipant>>();
		
		private CopyOnWriteList<ChatListener>		listeners = new CopyOnWriteList<ChatListener>();
		
		private Map<Object,Object>					user_data = new HashMap<Object, Object>();
		
		private boolean		keep_alive;
		private boolean		have_interest;
		
		private Map<String,Object> 	status;
		
		private boolean		is_shared_nick;
		private String		instance_nick;
		
		private int			reference_count;
		
		private ChatMessage		last_message_not_mine;
		private boolean			message_outstanding;
		
		private boolean		is_favourite;
		private boolean		auto_notify;
		
		private boolean		save_messages;
		private boolean		destroyed;
		
		private
		ChatInstance(
			String				_network,
			String				_key,
			ChatParticipant		_private_target,
			boolean				_is_private_chat )
		{
			network 		= _network;
			key				= _key;
			
				// private chat args
			
			private_target	= _private_target;			
			is_private_chat = _is_private_chat;
			
			String chat_key_base = "azbuddy.chat." + getNetAndKey();
			
			String shared_key 	= chat_key_base + ".shared";
			String nick_key 	= chat_key_base + ".nick";

			is_shared_nick 	= COConfigurationManager.getBooleanParameter( shared_key, true );
			instance_nick 	= COConfigurationManager.getStringParameter( nick_key, "" );
			
			if ( !is_private_chat ){
			
				is_favourite 	= getFavourite( network, key );
				save_messages 	= BuddyPluginBeta.this.getSaveMessages( network, key );
			}
			
			addReference();
		}
		
		public ChatInstance
		getClone()
		
			throws Exception
		{
			if ( is_private_chat ){
				
				addReference();
				
				return( this );
				
			}else{
			
					// can probably just do the above...
				
				return( BuddyPluginBeta.this.getChat( network, key ));
			}
		}
		
		protected void
		addReference()
		{
			synchronized( chat_lock ){
				
				reference_count++;
			}
		}
		
		public String
		getName()
		{
			String str = key;
			
			int pos = str.lastIndexOf( '[' );
			
			if ( pos != -1 && str.endsWith( "]")){
				
				String temp = str.substring( pos+1, str.length()-1 );
				
				if ( temp.contains( "pk=" )){
					
					str = str.substring( 0, pos );
					
					if ( temp.contains( "ro=1" )){
				
						str += "[R]";
					}else{
						
						str += "[M]";
					}
				}else{
					
					str = str.substring( 0, pos );
				}
			}
			
			return( 
				MessageText.getString(
					network==AENetworkClassifier.AT_PUBLIC?"label.public":"label.anon") + 
					" - '" + str + "'" );
		}
		
		public String
		getShortName()
		{
			
			String	short_name = getName();
			
			if ( short_name.length() > 60 ){
				
				short_name = short_name.substring( 0, 60 ) + "...";
			}
			
			return( short_name );
		}
		
		public String
		getNetwork()
		{
			return( network );
		}
		
		public String
		getKey()
		{
			return( key );
		}
		
		public boolean
		isFavourite()
		{
			return( is_favourite );
		}
		
		public void
		setAutoNotify(
			boolean		b )
		{
			auto_notify	= b;
		}
		
		public boolean
		isAutoNotify()
		{
			return( auto_notify );
		}
		
		public boolean
		isInteresting()
		{
			return( have_interest );
		}
		
		public void
		setFavourite(
			boolean		b )
		{
			if ( !is_private_chat ){
				
				if ( b != is_favourite ){
					
					is_favourite = b;
					
					BuddyPluginBeta.this.setFavourite( network, key, b );
				}
			}
		}
		
		public boolean
		getSaveMessages()
		{
			return( save_messages );
		}
		
		public void
		setSaveMessages(
			boolean		b )
		{
			if ( !is_private_chat ){
				
				if ( b != save_messages ){
					
					save_messages = b;
					
					BuddyPluginBeta.this.setSaveMessages( network, key, b );
					
					Map<String,Object>	options = new HashMap<String, Object>();
					
					options.put( "save_messages", b );
					
					try{
						updateOptions( options );
						
					}catch( Throwable e ){
						
						Debug.out( e );
					}
				}
			}
		}
		
		private void
		setSpammer(
			ChatParticipant		participant,
			boolean				is_spammer )
		{
			Map<String,Object>	options = new HashMap<String, Object>();
			
			options.put( "pk", participant.getPublicKey());
			options.put( "spammer", is_spammer );
			
			try{
				updateOptions( options );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
		
		public boolean
		isManaged()
		{
			return( managing_public_key != null );
		}
		
		public boolean
		amManager()
		{
			return( managing_public_key != null && Arrays.equals( my_public_key, managing_public_key ));
		}
		
		public boolean
		isManagedFor(
			String		network,
			String		key )
		{
			if ( getNetwork() != network ){
				
				return( false );
			}
			
			return( getKey().equals( key + "[pk=" + Base32.encode( getPublicKey()) + "]" ));
		}
		
		public ChatInstance
		getManagedChannel()
		
			throws Exception
		{
			if ( isManaged()){
				
				throw( new Exception( "Channel is already managed" ));
			}
			
			String new_key = getKey() + "[pk=" + Base32.encode( getPublicKey()) + "]";
			
			ChatInstance inst = getChat( getNetwork(), new_key );
			
			return( inst );
		}
		
		public boolean
		isReadOnlyFor(
			String		network,
			String		key )
		{
			if ( getNetwork() != network ){
				
				return( false );
			}
			
			return( getKey().equals( key + "[pk=" + Base32.encode( getPublicKey()) + "&ro=1]" ));
		}
		
		public ChatInstance
		getReadOnlyChannel()
		
			throws Exception
		{
			if ( isManaged()){
				
				throw( new Exception( "Channel is already managed" ));
			}
			
			String new_key = getKey() + "[pk=" + Base32.encode( getPublicKey()) + "&ro=1]";
			
			ChatInstance inst = getChat( getNetwork(), new_key );
			
			return( inst );
		}
		
		public boolean
		isReadOnly()
		{
			return( read_only && !amManager());
		}
		
		public String
		getURL()
		{
			if ( network == AENetworkClassifier.AT_PUBLIC ){
				
				return( "chat:?" + UrlUtils.encode( key ));
				
			}else{
				
				return( "chat:anon:?" + UrlUtils.encode( key ));
			}
		}
		
		public byte[]
		getPublicKey()
		{
			return( my_public_key );
		}
		
		public boolean
		isPrivateChat()
		{
			return( is_private_chat );
		}
		
		public boolean
		isAnonymous()
		{
			return( network != AENetworkClassifier.AT_PUBLIC );
		}
		
		public String
		getNetAndKey()
		{
			return( network + ": " + key );
		}
		
		public void
		setKeepAlive(
			boolean		b )
		{
			keep_alive	= b;
		}
		
		public boolean
		getKeepAlive()
		{
			return( keep_alive );
		}
		
		public boolean
		isSharedNickname()
		{
			return( is_shared_nick );
		}
		
		public void
		setSharedNickname(
			boolean		_shared ) 
		{
			if ( _shared != is_shared_nick ){
			
				is_shared_nick	= _shared;
			
				String chat_key_base = "azbuddy.chat." + getNetAndKey();

				String shared_key 	= chat_key_base + ".shared";

				COConfigurationManager.setParameter( shared_key, _shared );
				
				updated();
			}
		}
		
		public String
		getInstanceNickname()
		{
			return( instance_nick );
		}
		
		public void
		setInstanceNickname(
			String		_nick )
		{
			if ( !_nick.equals( instance_nick )){
				
				instance_nick	= _nick;
			
				String chat_key_base = "azbuddy.chat." + getNetAndKey();

				String nick_key 	= chat_key_base + ".nick";

				COConfigurationManager.setParameter( nick_key, _nick );
				
				updated();
			}
		}
		
		public String
		getNickname()
		{
			if ( is_shared_nick ){
				
				return( network == AENetworkClassifier.AT_PUBLIC?shared_public_nickname:shared_anon_nickname );
				
			}else{
				
				return( instance_nick );
			}
		}
		
		private Object
		getHandler()
		{
			return( handler );
		}
		
		private void
		bind(
			PluginInterface		_msgsync_pi,
			Object				_handler )
		
			throws Exception
		{	
			boolean	inform_avail = false;
			
			synchronized( binding_lock ){
				
				binding_sem = new AESemaphore( "bpb:bind" );
				
				try{
			
					msgsync_pi = _msgsync_pi;
		
					if ( _handler != null ){
						
						handler		= _handler;
						
						try{
							Map<String,Object>		options = new HashMap<String, Object>();
									
							options.put( "handler", _handler );
							
							options.put( "addlistener", this );
									
							Map<String,Object> reply = (Map<String,Object>)msgsync_pi.getIPC().invoke( "updateMessageHandler", new Object[]{ options } );
								
							my_public_key 		= (byte[])reply.get( "pk" );
							managing_public_key = (byte[])reply.get( "mpk" );
							Boolean ro 			= (Boolean)reply.get( "ro" );
		
							read_only = ro != null && ro;
							
							Number ipc_v = (Number)reply.get( "ipc_version" );
							
							ipc_version = ipc_v ==null?1:ipc_v.intValue();
		
							inform_avail = true;

						}catch( Throwable e ){
							
							throw( new Exception( e ));
						}
					}else{
					
						try{
							Map<String,Object>		options = new HashMap<String, Object>();
							
							options.put( "network", network );
							options.put( "key", key.getBytes( "UTF-8" ));
								
							if ( private_target != null ){
							
								options.put( "parent_handler", private_target.getChat().getHandler());
								options.put( "target_pk", private_target.getPublicKey());
								options.put( "target_contact", private_target.getContact());
							}
							
							if ( network != AENetworkClassifier.AT_PUBLIC ){
								
								options.put( "server_id", getSharedAnonEndpoint()?"dchat_shared":"dchat" );
							}
							
							options.put( "listener", this );
									
							if ( getSaveMessages()){
								
								options.put( "save_messages", true );
							}
							
							Map<String,Object> reply = (Map<String,Object>)msgsync_pi.getIPC().invoke( "getMessageHandler", new Object[]{ options } );
							
							handler = reply.get( "handler" );
							
							my_public_key = (byte[])reply.get( "pk" );
							managing_public_key = (byte[])reply.get( "mpk" );
							Boolean ro 			= (Boolean)reply.get( "ro" );
		
							read_only = ro != null && ro;
							
							Number ipc_v = (Number)reply.get( "ipc_version" );
							
							ipc_version = ipc_v ==null?1:ipc_v.intValue();
							
							inform_avail = true;
							
						}catch( Throwable e ){
							
							throw( new Exception( e ));
						}
					}
				}finally{
					
					binding_sem.releaseForever();

					binding_sem = null;
				}
			}
			
			if ( inform_avail ){
					
				for ( ChatListener l: listeners ){
					
					try{
						l.stateChanged( true );
						
					}catch( Throwable e ){
						
						Debug.out( e );
					}
				}
			}
		}
		
		private void
		updateOptions(
			Map<String,Object>		options )
			
			throws Exception
		{	
			if ( handler == null || msgsync_pi == null ){
				
				Debug.out( "No handler!" );
				
			}else{
				
				options.put( "handler", handler );

				msgsync_pi.getIPC().invoke( "updateMessageHandler", new Object[]{ options });
			}
		}
		
		private void
		unbind()
		{
			for ( ChatListener l: listeners ){
				
				try{
					l.stateChanged( false );
					
				}catch( Throwable e ){
					
					Debug.out( e );
				}
			}
			
			handler 	= null;
			msgsync_pi	= null;
		}
		
		public boolean
		isAvailable()
		{
			return( handler != null );
		}
		
		public ChatMessage[]
		getHistory()
		{
			synchronized( chat_lock ){
				
				return( messages.toArray( new ChatMessage[ messages.size() ]));
			}
		}
		
		private void
		update()
		{
			PluginInterface		current_pi 			= msgsync_pi;
			Object 				current_handler 	= handler;
			
			if ( current_handler != null && current_pi != null ){
							
				try{
					Map<String,Object>		options = new HashMap<String, Object>();
					
					options.put( "handler", current_handler );
	
					status = (Map<String,Object>)current_pi.getIPC().invoke( "getStatus", new Object[]{ options } );
										
				}catch( Throwable e ){
					
					Debug.out( e );
				}
			}
			
			updated();
		}
		
		private void
		updated()
		{
			for ( ChatListener l: listeners ){
				
				try{	
					l.updated();
					
				}catch( Throwable e ){
				
					Debug.out( e );
				}
			}
		}
		
		public int
		getEstimatedNodes()
		{
			Map<String,Object> map = status;
			
			if ( map == null ){
				
				return( -1 );
			}
			
			return(((Number)map.get( "node_est" )).intValue());
		}
		
		public int
		getMessageCount(
			boolean	not_mine )
		{
			if ( not_mine ){
				
				return( messages_not_mine_count );
				
			}else{
			
				return( messages.size());
			}
		}
		
			/**
			 * -ve -> state unknown
			 * 0 - synced
			 * +ve - number of messages pending
			 * @return
			 */
		
		public int
		getIncomingSyncState()
		{
			Map<String,Object> map = status;
			
			if ( map == null ){
				
				return( -3 );
			}
			
			Number	in_pending = (Number)map.get( "msg_in_pending" );
			
			return( in_pending==null?-2:in_pending.intValue());
		}
		
		/**
		 * -ve -> state unknown
		 * 0 - synced
		 * +ve - number of messages pending
		 * @return
		 */
		
		public int
		getOutgoingSyncState()
		{
			Map<String,Object> map = status;
			
			if ( map == null ){
				
				return( -3 );
			}
			
			Number	out_pending = (Number)map.get( "msg_out_pending" );
			
			return( out_pending==null?-2:out_pending.intValue());
		}
		
		public String
		getStatus()
		{
			PluginInterface		current_pi 			= msgsync_pi;
			Object 				current_handler 	= handler;

			if ( current_pi == null ){
				
				return( MessageText.getString( "azbuddy.dchat.status.noplugin" ));
			}
			
			if ( current_handler == null ){
				
				return( MessageText.getString( "azbuddy.dchat.status.nohandler" ));
			}
			
			Map<String,Object> map = status;
			
			if ( map == null ){
				
				return( MessageText.getString( "azbuddy.dchat.status.notavail" ));
				
			}else{
				int status 			= ((Number)map.get( "status" )).intValue();
				int dht_count 		= ((Number)map.get( "dht_nodes" )).intValue();
				
				int nodes_local 	= ((Number)map.get( "nodes_local" )).intValue();
				int nodes_live 		= ((Number)map.get( "nodes_live" )).intValue();
				int nodes_dying 	= ((Number)map.get( "nodes_dying" )).intValue();
				
				int req_in 			= ((Number)map.get( "req_in" )).intValue();
				double req_in_rate 	= ((Number)map.get( "req_in_rate" )).doubleValue();
				int req_out_ok 		= ((Number)map.get( "req_out_ok" )).intValue();
				int req_out_fail 	= ((Number)map.get( "req_out_fail" )).intValue();
				double req_out_rate = ((Number)map.get( "req_out_rate" )).doubleValue();
									
				if ( status == 0 || status == 1 ){
					
					String	arg1;
					String	arg2;
					
					if ( isPrivateChat()){
						
						arg1 = MessageText.getString( "label.private.chat" ) + ": ";
						arg2 = "";
					}else{
						
						if ( status == 0 ){
						
							arg1 = MessageText.getString( "pairing.status.initialising" ) + ": ";
							arg2 = "DHT=" + (dht_count<0?"...":String.valueOf(dht_count)) + ", ";

						}else if ( status == 1 ){
							
							arg1 = "";
							arg2 = "DHT=" + dht_count + ", ";
					
						}else{
							arg1 = "";
							arg2 = "";
						}
					}
					
					String arg3 = nodes_local+"/"+nodes_live+"/"+nodes_dying;
					String arg4 = DisplayFormatters.formatDecimal(req_out_rate,1) + "/" +  DisplayFormatters.formatDecimal(req_in_rate,1);
					
					String str = 
						MessageText.getString(
							"azbuddy.dchat.node.status",
							new String[]{ arg1, arg2, arg3, arg4 });
					
					if ( isReadOnly()){
						
						str += ", R-";
						
					}else if ( amManager()){
						
						if ( read_only ){
							
							str += ", R+";
							
						}else{
						
							str += ", M+";
						}
					}else if ( isManaged()){
						
						str += ", M-";
					}
						
					
					return( str );
					
				}else{
					
					return( MessageText.getString( "azbuddy.dchat.status.destroyed" ));
				}
			}
		}
		
		private TimerEvent	sort_event;
		private boolean		sort_force_changed;
		
		private void
		sortMessages(
			boolean		force_change )
		{
			synchronized( chat_lock ){
				
				if ( force_change ){
					
					sort_force_changed = true;
				}
				
				if ( sort_event != null ){
					
					return;
				}
				
				sort_event = 
					SimpleTimer.addEvent(
						"msgsort",
						SystemTime.getOffsetTime( 500 ),
						new TimerEventPerformer()
						{
							public void 
							perform(
								TimerEvent event) 
							{
								boolean	changed = false;
								
								synchronized( chat_lock ){
									
									sort_event = null;
									
									changed = sortMessagesSupport();
									
									if ( sort_force_changed ){
										
										changed = true;
												
										sort_force_changed = false;
									}
								}
								
								if ( changed ){
									
									for ( ChatListener l: listeners ){
										
										l.messagesChanged();
									}
								}
							}
						});
			}
		}
		
		private boolean
		sortMessagesSupport()
		{
			int	num_messages = messages.size();
			
			ByteArrayHashMap<ChatMessage>	id_map 		= new ByteArrayHashMap<ChatMessage>( num_messages );
			Map<ChatMessage,ChatMessage>	prev_map 	= new HashMap<ChatMessage,ChatMessage>( num_messages );
			Map<ChatMessage,ChatMessage>	next_map 	= new HashMap<ChatMessage,ChatMessage>( num_messages );
			
				// build id map so we can lookup prev messages
			
			for ( ChatMessage msg: messages ){
				
				byte[]	id = msg.getID();
				
				id_map.put( id, msg );
			}
			
				// build sets of prev/next links 
			
			for ( ChatMessage msg: messages ){
				
				byte[]	prev_id 	= msg.getPreviousID();
				
				if ( prev_id != null ){
					
					ChatMessage prev_msg = id_map.get( prev_id );
					
					if ( prev_msg != null ){
						
						msg.setPreviousID( prev_msg.getID());	// save some mem
						
						// ordering prev_msg::msg
					
						prev_map.put( msg, prev_msg );
						next_map.put( prev_msg, msg );
					}
				}
			}
			
				// a comparator to consistently order messages to ensure sorting is determinstic
			
			Comparator<ChatMessage> message_comparator = 
					new Comparator<ChatMessage>()
					{					
						public int 
						compare(
							ChatMessage o1, 
							ChatMessage o2 ) 
						{
							return( o1.getUID() - o2.getUID());
						}
					};
					
				// break any loops arbitrarily
		
			Set<ChatMessage>	linked_messages = new TreeSet<ChatMessage>(	message_comparator );
			
			linked_messages.addAll( prev_map.keySet());
			
			while( linked_messages.size() > 0 ){
				
				ChatMessage start = linked_messages.iterator().next();
					
				linked_messages.remove( start );
				
				ChatMessage current = start;
				
				int	loops = 0;
				
				while( true ){
				
					loops++;
					
					if ( loops > num_messages ){
						
						Debug.out( "infinte loop" );
						
						break;
					}
					
					ChatMessage prev_msg = prev_map.get( current );
					
					if ( prev_msg == null ){
						
						break;
						
					}else{
						
						linked_messages.remove( prev_msg );
						
						if ( prev_msg == start ){
												
								// loopage
							
							prev_map.put( current, null );
							next_map.put( prev_msg, null );
							
							break;
							
						}else{
							
							current = prev_msg;
						}
					}
				}
				
			}
				// find the heads of the various chains
			
			Set<ChatMessage>		chain_heads = new TreeSet<ChatMessage>( message_comparator );
			
			for ( ChatMessage msg: messages ){
				
				ChatMessage prev_msg = prev_map.get( msg );
				
				if ( prev_msg != null ){
					
					int	 loops = 0;
					
					while( true ){
					
						loops++;
						
						if ( loops > num_messages ){
							
							Debug.out( "infinte loop" );
							
							break;
						}
						
						ChatMessage prev_prev = prev_map.get( prev_msg );
						
						if ( prev_prev == null ){
							
							chain_heads.add( prev_msg );
							
							break;
							
						}else{
							
							prev_msg = prev_prev;
						}
					}
				}
			}
			
			Set<ChatMessage>	remainder_set = new HashSet<BuddyPluginBeta.ChatMessage>( messages );
			
			List<ChatMessage> result = null;
			
			for ( ChatMessage head: chain_heads ){
				
				List<ChatMessage>	chain = new ArrayList<ChatMessage>( num_messages );

				ChatMessage msg = head;
						
				while( msg != null ){
					
					chain.add( msg );
					
					remainder_set.remove( msg );
					
					msg = next_map.get( msg );
				}
				
				if ( result == null ){
					
					result = chain;
					
				}else{
					
					result = merge( result, chain );
				}
			}
			
			if ( remainder_set.size() > 0 ){
				
					// these are messages not part of any chain so sort based on time
				
				List<ChatMessage>	remainder = new ArrayList<ChatMessage>( remainder_set );
				
				Collections.sort(
						remainder,
						new Comparator<ChatMessage>()
						{
							public int 
							compare(
								ChatMessage m1, 
								ChatMessage m2 ) 
							{
								long l = m1.getTimeStamp() - m2.getTimeStamp();
								
								if ( l < 0 ){
									return( -1 );
								}else if ( l > 0 ){
									return( 1 );
								}else{
									return( m1.getUID() - m2.getUID());
								}
							}
						});
				
				if ( result == null ){
					
					result = remainder;
					
				}else{
					
					result = merge( result, remainder );
				}
			}
						
			if ( result == null ){
						
				return( false );
			}
			
			boolean	changed = false;

			if ( messages.size() != result.size()){
					
				Debug.out( "Inconsistent: " + messages.size() + "/" + result.size());

				changed = true;
			}
					
			Set<ChatParticipant>	participants = new HashSet<ChatParticipant>();
			
			for ( int i=0;i<result.size();i++){
				
				ChatMessage msg = result.get(i);
				
				ChatParticipant p = msg.getParticipant();
					
				participants.add( p );
				
				if ( !changed ){
						
					if ( messages.get(i) != msg ){
					
						changed = true;
					}
				}
			}
				
			if ( changed ){
				
				messages = result;
				
				for ( ChatParticipant p: participants ){
					
					p.resetMessages();
				}
				
				Set<ChatParticipant>	updated = new HashSet<ChatParticipant>();
				
				for ( ChatMessage msg: messages ){
					
					ChatParticipant p = msg.getParticipant();
					
					if ( p.replayMessage( msg )){
						
						updated.add( p );
					}
				}
				
				for ( ChatParticipant p: updated ){
					
					updated( p );
				}
			}
			
			return( changed );
		}
		
		private List<ChatMessage>
		merge(
			List<ChatMessage>		list1,
			List<ChatMessage>		list2 )
		{
			int	size1 = list1.size();
			int size2 = list2.size();
			
			List<ChatMessage>	result = new ArrayList<ChatMessage>( size1 + size2 );
			
			int	pos1 = 0;
			int pos2 = 0;
			
			while( true ){
				
				if ( pos1 == size1 ){
					
					for ( int i=pos2;i<size2;i++){
						
						result.add( list2.get(i));
					}
					
					break;
					
				}else if ( pos2 == size2 ){
					
					for ( int i=pos1;i<size1;i++){
						
						result.add( list1.get(i));
					}
					
					break;
					
				}else{
					
					ChatMessage m1 = list1.get( pos1 );
					ChatMessage m2 = list2.get( pos2 );
				
					long	t1 = m1.getTimeStamp();
					long	t2 = m2.getTimeStamp();
					
					if ( t1 < t2 || ( t1 == t2 && m1.getUID() < m2.getUID())){
						
						result.add( m1 );
						
						pos1++;
						
					}else{
						
						result.add( m2 );
						
						pos2++;
					}
				}
			}
			
			return( result );
		}
		
		public void
		messageReceived(
			Map<String,Object>			message_map )
			
			throws IPCException
		{
			AESemaphore sem;
			
			synchronized( binding_lock ){
				
				sem = binding_sem;
			}
			
			if ( sem != null ){
				
				sem.reserve();
			}
			
			ChatMessage msg = new ChatMessage( message_uid_next.incrementAndGet(), message_map );
						
			ChatParticipant	new_participant = null;
				
			boolean	sort_outstanding = false;
			
			byte[]	prev_id 	= msg.getPreviousID();
						
			synchronized( chat_lock ){
				
				byte[] id = msg.getID();
				
				if ( message_ids.containsKey( id )){
					
						// duplicate, probably from plugin unload, reload and re-bind
					
					return;
				}
				
				message_ids.put( id, "" );
				
					// best case is that message belongs at the end
				
				int old_msgs = messages.size();

				messages.add( msg );
		
				if ( messages.size() > MSG_HISTORY_MAX ){
					
					ChatMessage removed = messages.remove(0);
					
					old_msgs--;
					
					message_ids.remove( removed.getID());
					
					ChatParticipant rem_part = removed.getParticipant();
					
					rem_part.removeMessage( removed );
					
					if ( !rem_part.isMe()){
						
						messages_not_mine_count--;
					}
				}
				

				byte[] pk = msg.getPublicKey();
				
				ChatParticipant participant = participants.get( pk );
								
				if ( participant == null ){
					
					new_participant = participant = new ChatParticipant( this, pk );
					
					participants.put( pk, participant );
										
					participant.addMessage( msg );
										
				}else{
										
					participant.addMessage( msg );										
				}
					
				if ( participant.isMe()){
					
					InetSocketAddress address = msg.getAddress();
					
					if ( address != null ){
						
						my_address = address;
					}
				}else{
					
					last_message_not_mine = msg;
					
					messages_not_mine_count++;
				}
				
				if ( sort_event != null ){
					
					sort_outstanding = true;
					
				}else{
					
					if ( old_msgs == 0 ){	
						
					}else if ( prev_id != null && Arrays.equals( prev_id, messages.get(old_msgs-1).getID())){

						// in right place already by linkage
						
					}else if ( msg.getMessageType() != ChatMessage.MT_NORMAL ){

						// info etc always go last
						
					}else{
						
						sortMessages( true );
						
						sort_outstanding = true;
					}
				}
			}
			
			if ( new_participant != null ){
				
				for ( ChatListener l: listeners ){
					
					l.participantAdded( new_participant );
				}
			}
			
			if ( !sort_outstanding ){
				
				for ( ChatListener l: listeners ){
					
					l.messageReceived( msg );
				}
			}
		}
		
		public Map<String,Object>
		chatRequested(
			Map<String,Object>			message_map )
			
			throws IPCException
		{
			AESemaphore sem;
			
			synchronized( binding_lock ){
				
				sem = binding_sem;
			}
			
			if ( sem != null ){
				
				sem.reserve();
			}
			
			if ( private_chat_state == PRIVATE_CHAT_DISABLED ){
				
				throw( new IPCException( "Private chat disabled by recipient" ));
			}
			
			try{
				Object	new_handler 	= message_map.get( "handler" );
				
				byte[]	remote_pk 		= (byte[])message_map.get( "pk" );
				
				ChatParticipant	participant;
				
				synchronized( chat_lock ){
	
					participant = participants.get( remote_pk );
				}
				
				if ( participant == null ){
					
					throw( new IPCException( "Private chat requires you send at least one message to the main chat first" ));
				}
					
				if ( private_chat_state == PRIVATE_CHAT_PINNED_ONLY && !participant.isPinned()){
					
					throw( new IPCException( "Recipient will only accept private chats from pinned participants" ));
				}
				
				BuddyPluginViewInterface ui = plugin.getSWTUI();
				
				if ( ui == null ){
					
					throw( new IPCException( "Chat unavailable" ));
				}
									
				ChatInstance inst = getChat( participant, new_handler );

				if ( !isSharedNickname()){
					
					inst.setSharedNickname( false );
					
					inst.setInstanceNickname( getInstanceNickname());
				}
				
				ui.openChat( inst );
				
				Map<String,Object>	reply = new HashMap<String, Object>();
				
				reply.put( "nickname", participant.getName());
				
				return( reply );
				
			}catch( IPCException e ){
				
				throw( e );
				
			}catch( Throwable e ){
				
				throw( new IPCException( e ));
			}
		}
		
		AsyncDispatcher	dispatcher = new AsyncDispatcher( "sendAsync" );
		
		public void 
		sendMessage(
			final String					message,
			final Map<String,Object>		options )
		{
			sendMessage( message, null, options );
		}
		
		public void 
		sendMessage(
			final String					message,
			final Map<String,Object>		flags,
			final Map<String,Object>		options )
		{
			dispatcher.dispatch(
				new AERunnable()
				{
					
					@Override
					public void 
					runSupport() 
					{
						sendMessageSupport( message, flags, options );
					}
				});
		}
		
		public void 
		sendLocalMessage(
			final String		message,
			final String[]		args,
			final int			message_type )
		{
			if ( ipc_version < 2 ){
				
				return;
			}
			
			dispatcher.dispatch(
				new AERunnable()
				{
					
					@Override
					public void 
					runSupport() 
					{
						Map<String,Object>		options = new HashMap<String, Object>();
						
						String raw_message;
						
						if ( message.startsWith( "!") && message.endsWith( "!" )){

							raw_message = message.substring( 1, message.length() - 1 );
			
						}else{
							
							raw_message = MessageText.getString( message, args );
						}
						options.put( "is_local", true );
						options.put( "message", raw_message );
						options.put( "message_type", message_type );
						
						sendMessageSupport( "", null, options );
					}
				});
		}
		
		public void 
		sendControlMessage(
			final String		cmd )
		{
			if ( ipc_version < 3 ){
				
				return;
			}
			
			dispatcher.dispatch(
				new AERunnable()
				{
					
					@Override
					public void 
					runSupport() 
					{
						Map<String,Object>		options = new HashMap<String, Object>();
						
						options.put( "is_control", true );
						options.put( "cmd", cmd );
						
						sendMessageSupport( "", null, options );
					}
				});
		}
		
		private void 
		sendMessageSupport(
			String					message,
			Map<String,Object>		flags,
			Map<String,Object>		options )
		{
			if ( handler == null || msgsync_pi == null ){
				
				Debug.out( "No handler/plugin" );
				
			}else{
				
				if ( message.equals( "!dump!" )){
					
					synchronized( chat_lock ){
						
						for ( ChatMessage msg: messages ){
							
							System.out.println( pkToString( msg.getID()) + ", " + pkToString( msg.getPreviousID()) + " - " + msg.getMessage());
						}
					}
					return;
					
				}else if ( message.equals( "!sort!" )){
					
					sortMessages( false );
					
					return;
					
				}else if ( message.equals( "!flood!" )){
					
					if ( DEBUG_ENABLED ){
					
						SimpleTimer.addPeriodicEvent(
							"flooder",
							1500,
							new TimerEventPerformer() {
								
								public void perform(TimerEvent event) {
								
									sendMessage( "flood - " + SystemTime.getCurrentTime(), null );
									
								}
							});
					}
					
					return;
					
	
				}else if ( message.equals( "!ftux!" )){
					
					plugin.getBeta().setFTUXAccepted( false );
					
					return;
				}
				
				if ( message.startsWith( "/" )){
										
					String[] bits = message.split( "[\\s]+", 3 );
					
					String command = bits[0].toLowerCase( Locale.US );
					
					boolean	ok = false;
					
					try{
						if ( command.equals( "/help" )){
							
							String link = MessageText.getString( "azbuddy.dchat.link.url" );
							
							sendLocalMessage( "label.see.x.for.help", new String[]{ link }, ChatMessage.MT_INFO );

							ok = true;
							
						}else if ( command.equals( "/join" )){
							
							if ( bits.length > 1 ){
								
								bits = message.split( "[\\s]+", 2 );
								
								String key = bits[1];
								
								if ( key.startsWith( "\"" ) && key.endsWith( "\"" )){
									key = key.substring(1,key.length()-1);
								}
								
								getAndShowChat( getNetwork(), key );
								
								ok = true;
							}
						}else if ( command.equals( "/pjoin" )){
							
							if ( bits.length > 1 ){
								
								bits = message.split( "[\\s]+", 2 );
								
								String key = bits[1];
								
								if ( key.startsWith( "\"" ) && key.endsWith( "\"" )){
									key = key.substring(1,key.length()-1);
								}
								
								getAndShowChat( AENetworkClassifier.AT_PUBLIC, key );
								
								ok = true;
							}
						}else if ( command.equals( "/ajoin" )){
							
							if ( bits.length > 1 && isI2PAvailable()){
								
								bits = message.split( "[\\s]+", 2 );
								
								String key = bits[1];
								
								if ( key.startsWith( "\"" ) && key.endsWith( "\"" )){
									key = key.substring(1,key.length()-1);
								}
								
								getAndShowChat( AENetworkClassifier.AT_I2P, key );
								
								ok = true;
							}
						}else if ( command.equals( "/msg" ) || command.equals( "/query" )){
							
							if ( bits.length > 1 ){
								
								String nick = bits[1];
								
								String	pm = bits.length ==2?"":bits[2].trim();
								
								ChatParticipant p = getParticipant( nick );
								
								if ( p == null ){
									
									throw( new Exception( "Nick not found: " + nick ));
									
								}else if ( p.isMe()){
									
									throw( new Exception( "Can't chat to yourself" ));
								}
								
								ChatInstance ci = p.createPrivateChat();
								
								if ( pm.length() > 0 ){
								
									ci.sendMessage( pm, new HashMap<String, Object>());
								}
								
								showChat( ci );
								
								ok = true;
							}
						}else if ( command.equals( "/ignore" )){
							
							if ( bits.length > 1 ){
								
								String nick = bits[1];
								
								boolean	ignore = true;
								
								if ( nick.equals( "-r" ) && bits.length > 2 ){
									
									nick = bits[2];
									
									ignore = false;
								}
								
								ChatParticipant p = getParticipant( nick );
								
								if ( p == null ){
									
									throw( new Exception( "Nick not found: " + nick ));
								}
								
								p.setIgnored( ignore );
								
									// obviously the setter should do this but whatever for the mo
								
								updated( p );
								
								ok = true;
							}
							
						}else if ( command.equals( "/control" )){
							
							if ( ipc_version >= 3 ){
								
								String[] bits2 = message.split( "[\\s]+", 2 );
	
								if ( bits2.length > 1 ){
									
									sendControlMessage( bits2[1] );
									
									ok = true;
									
								}else{
									
									throw( new Exception( "Invalid command: " + message ));
								}
							}
							
						}else if ( command.equals( "/peek" )){
							
							if ( bits.length > 1 ){
								
								Map<String,Object> result = peekChat( getNetwork(), bits[1] );
								
								sendLocalMessage( "!" + result + "!", null, ChatMessage.MT_INFO );
								
								ok = true;
							}
						}else if ( command.equals( "/clone" )){
							
							getAndShowChat( getNetwork(), getKey());
							
							ok = true;
						}
						
						if ( !ok ){
							
							throw( new Exception( "Unhandled command: " + message ));
						}
					}catch( Throwable e ){
						
						sendLocalMessage( "!" + Debug.getNestedExceptionMessage( e ) + "!", null, ChatMessage.MT_ERROR );
					}
					
					return;
				}
				
				try{
					ChatMessage		prev_message = null;
					
					synchronized( chat_lock ){
						
						if ( messages.size() > 0 ){
							
							prev_message = messages.get( messages.size()-1);
						}
					}
					
					if ( options == null ){
						
						options = new HashMap<String, Object>();
					}
					
					options.put( "handler", handler );
					
					Map<String,Object>	payload = new HashMap<String, Object>();
					
					payload.put( "msg", message.getBytes( "UTF-8" ));
					
					payload.put( "nick", getNickname().getBytes( "UTF-8" ));
					
					if ( prev_message != null ){
						
						payload.put( "pre", prev_message.getID());
					}
					
					if ( flags != null ){
						
						payload.put( "f", flags );
					}
					
					options.put( "content", BEncoder.encode( payload ));
												
					Map<String,Object> reply = (Map<String,Object>)msgsync_pi.getIPC().invoke( "sendMessage", new Object[]{ options } );
					
						// once we participate in a chat then we want to keep it around to ensure
						// or at least try and ensure message delivery
					
					have_interest = true;
					
				}catch( Throwable e ){
					
					Debug.out( e );
				}
			}
		}
		
		public String
		export()
		{
			if ( handler == null || msgsync_pi == null ){

				return( "" );
			}
			try{
				Map<String,Object>		options = new HashMap<String, Object>();
				
				options.put( "handler", handler );
	
				Map<String,Object> reply = (Map<String,Object>)msgsync_pi.getIPC().invoke( "exportMessageHandler", new Object[]{ options } );

				return((String)reply.get( "export_data" ));
				
			}catch( Throwable e ){
				
				Debug.out( e );
				
				return( "" );
			}
		}
		
		public List<ChatMessage>
		getMessages()
		{
			synchronized( chat_lock ){

				return( new ArrayList<ChatMessage>( messages ));
			}
		}
		
		public ChatParticipant[]
		getParticipants()
		{
			synchronized( chat_lock ){
				
				return( participants.values().toArray( new ChatParticipant[ participants.size()]));
			}
		}
		
		public ChatParticipant
		getParticipant(
			String	nick )
		{
			synchronized( chat_lock ){
				
				for ( ChatParticipant cp: participants.values()){
					
					if ( cp.getName().equals( nick )){
						
						return( cp );
					}
				}
			}
			
			return( null );
		}
		
		protected void
		updated(
			ChatParticipant		p )
		{
			for ( ChatListener l: listeners ){
				
				l.participantChanged( p );
			}
		}
		
		private void
		registerNick(
			ChatParticipant		p,
			String				old_nick,
			String				new_nick )
		{
			synchronized( chat_lock ){
				
				if ( old_nick != null ){
				
					List<ChatParticipant> list = nick_clash_map.get( old_nick );
					
					if ( list != null && list.remove( p )){
												
						if ( list.size() == 0 ){
							
							nick_clash_map.remove( old_nick );
							
						}else{
							
							if ( list.size() == 1 ){
							
								list.get(0).setNickClash( false );
							}
						}
					}else{
						
						Debug.out( "inconsistent" );
					}
				}
				
				List<ChatParticipant> list = nick_clash_map.get( new_nick );
	
				if ( list == null ){
					
					list = new ArrayList<BuddyPluginBeta.ChatParticipant>();
					
					nick_clash_map.put( new_nick, list );
				}
				
				if ( list.contains( p )){
					
					Debug.out( "inconsistent" );
					
				}else{
					
					list.add( p );
					
					if ( list.size() > 1 ){
						
						p.setNickClash( true );
						
						if ( list.size() == 2 ){
							
							list.get(0).setNickClash( true );
						}
					}else{
						
						p.setNickClash( false );
					}
				}
			}
		}
		
		public ChatMessage
		getLastMessageNotMine()
		{
			return( last_message_not_mine );
		}
		
		public void
		setUserData(
			Object		key,
			Object		value )
		{
			synchronized( user_data ){
				
				user_data.put( key, value );
			}
		}
		
		public Object
		getUserData(
			Object		key )
		{
			synchronized( user_data ){
				
				return( user_data.get( key ));
			}	
		}
		
		public boolean
		getMessageOutstanding()
		{
			synchronized( chat_lock ){
			
				return( message_outstanding );
			}
		}
		
		public void
		setMessageOutstanding( 
			boolean		b )
		{
			synchronized( chat_lock ){
			
				if ( message_outstanding == b ){
					
					return;
				}
				
				message_outstanding = b;
				
				if ( !b ){
					
					if ( messages.size() > 0 ){
						
						ChatMessage	last_read_msg = messages.get( messages.size()-1 );
						
						long last_read_time = last_read_msg.getTimeStamp();
						
						String last_info = (SystemTime.getCurrentTime()/1000) + "/" + (last_read_time/1000) + "/" + Base32.encode( last_read_msg.getID());
						
						BuddyPluginBeta.this.setLastMessageInfo( network, key, last_info );
					}
				}
			}
		}
		
		public boolean
		isOldOutstandingMessage(
			ChatMessage			msg )
		{
			synchronized( chat_lock ){
				
				String info = BuddyPluginBeta.this.getLastMessageInfo( network, key );
				
				if ( info != null ){
					
					String[] bits = info.split( "/" );
					
					try{
						long	old_time_secs 	= Long.parseLong( bits[0] );
						long	old_msg_secs 	= Long.parseLong( bits[1] );
						byte[]	old_id			= Base32.decode( bits[2] );
						
						long	msg_secs	= msg.getTimeStamp()/1000;
						byte[]	id			= msg.getID();
						
						if ( Arrays.equals( id, old_id )){
							
							return( true );
						}
						
						long	old_cuttoff = old_time_secs - 5*60;
						
						if ( old_msg_secs > old_cuttoff ){
							
							old_cuttoff = old_msg_secs;
						}
						
						if ( msg_secs <= old_cuttoff ){
							
							return( true );
						}
						
						if ( message_ids.containsKey( old_id ) && message_ids.containsKey( id )){
							
							int	msg_index 		= -1;
							int old_msg_index 	= -1;
							
							for ( int i=0;i<messages.size();i++){
								
								ChatMessage m = messages.get(i);
								
								if ( m == msg ){
									
									msg_index = i;
									
								}else if ( Arrays.equals( m.getID(), old_id )){
									
									old_msg_index = i;
								}
							}
							
							if ( msg_index <= old_msg_index ){
								
								return( true );
							}
						}
					}catch( Throwable e ){
						
					}
				}
			}
			
			return( false );
		}
		
		public InetSocketAddress
		getMyAddress()
		{
			return( my_address );
		}
		
		public void
		addListener(
			ChatListener		listener )
		{
			listeners.add( listener );
		}
		
		public void
		removeListener(
			ChatListener		listener )
		{
			listeners.remove( listener );
		}
		
		public boolean
		isDestroyed()
		{
			return( destroyed );
		}
		
		public void
		destroy()
		{
			synchronized( chat_lock ){
				
				reference_count--;
				
				if ( reference_count > 0 ){
					
					return;
				}
			}
			
			if ( !( keep_alive || (have_interest && !is_private_chat ))){
				
				destroyed = true;
				
				if ( handler != null ){
							
					if ( is_private_chat ){
						
						Map<String,Object>		flags = new HashMap<String, Object>();
						
						flags.put( FLAGS_MSG_STATUS_KEY, FLAGS_MSG_STATUS_CHAT_QUIT );
						
						sendMessageSupport( "", flags, new HashMap<String, Object>());
					}
					
					try{
						Map<String,Object>		options = new HashMap<String, Object>();
						
						options.put( "handler", handler );
													
						Map<String,Object> reply = (Map<String,Object>)msgsync_pi.getIPC().invoke( "removeMessageHandler", new Object[]{ options } );
						
					}catch( Throwable e ){
						
						Debug.out( e );
						
					}finally{
						
						String meta_key = network + ":" + key;

						ChatInstance	removed = null;
						
						synchronized( chat_instances_map ){
						
							ChatInstance inst = chat_instances_map.remove( meta_key );
							
							if ( inst != null ){
								
								removed = inst;
								
								chat_instances_list.remove( inst );
							}
							
							if ( chat_instances_map.size() == 0 ){
								
								if ( timer != null ){
									
									timer.cancel();
									
									timer = null;
								}
							}
						}
						
						if ( removed != null ){
							
							for ( ChatManagerListener l: BuddyPluginBeta.this.listeners ){
								
								try{
									l.chatRemoved( removed );
									
								}catch( Throwable e ){
									
									Debug.out( e );;
								}
							}
						}
					}
				}
			}
		}
	}
	
	public class
	ChatParticipant
	{
		private final ChatInstance		chat;
		private final byte[]			pk;
		
		private String				nickname;
		private boolean				is_ignored;
		private boolean				is_spammer;
		private boolean				is_pinned;
		private boolean				nick_clash;
		
		private List<ChatMessage>	participant_messages	= new ArrayList<ChatMessage>();
		
		private Boolean				is_me;
		
		private
		ChatParticipant(
			ChatInstance		_chat,
			byte[]				_pk )
		{
			chat	= _chat;
			pk		= _pk;
			
			nickname = pkToString( pk );
			
			is_pinned = COConfigurationManager.getBooleanParameter( getPinKey(), false );
			
			chat.registerNick( this, null, nickname );
		}
		
		public ChatInstance
		getChat()
		{
			return( chat );
		}
	
		public byte[]
		getPublicKey()
		{
			return( pk );
		}
		
		public Map<String,Object>
		getContact()
		{
			synchronized( chat.chat_lock ){
			
				return( participant_messages.get( participant_messages.size()-1).getContact());
			}
		}
		
		public InetSocketAddress
		getAddress()
		{
			synchronized( chat.chat_lock ){
			
				return( participant_messages.get( participant_messages.size()-1).getAddress());
			}
		}
		
		public boolean
		isMe()
		{
			if ( is_me != null ){
				
				return( is_me );
			}
			
			byte[] chat_key = chat.getPublicKey();
			
			if ( chat_key != null ){
			
				is_me = Arrays.equals( pk, chat_key );
			}
			
			return( is_me==null?false:is_me );
		}
		
		public String
		getName()
		{
			return( getName( true ));
		}
		
		public String
		getName(
			boolean	use_nick )
		{
			if ( use_nick ){
				
				return( nickname );
				
			}else{
				
				return( pkToString( pk ));
			}
		}
		
		public boolean
		hasNickname()
		{
			return( !nickname.equals( pkToString( pk )));
		}
		
		private void
		addMessage(
			ChatMessage		message )
		{
			participant_messages.add( message );
			
			message.setParticipant( this );
			
			message.setIgnored( is_ignored || is_spammer );
			
			String new_nickname = message.getNickName();
			
			if ( !nickname.equals( new_nickname )){
			
				chat.registerNick( this, nickname, new_nickname );

				message.setNickClash( isNickClash());
				
				nickname = new_nickname;
								
				chat.updated( this );
				
			}else{
				
				message.setNickClash( isNickClash());
			}
		}
		
		private boolean
		replayMessage(
			ChatMessage		message )
		{
			participant_messages.add( message );
						
			message.setIgnored( is_ignored || is_spammer );

			String new_nickname = message.getNickName();
			
			if ( !nickname.equals( new_nickname )){
			
				chat.registerNick( this, nickname, new_nickname );

				message.setNickClash( isNickClash());
				
				nickname = new_nickname;
								
				return( true );
				
			}else{
				
				message.setNickClash( isNickClash());
				
				return( false );
			}
		}
		
		private void
		removeMessage(
			ChatMessage		message )
		{
			participant_messages.remove( message );
		}
		
		private void
		resetMessages()
		{
			String new_nickname = pkToString( pk );
			
			if ( !nickname.equals( new_nickname )){
				
				chat.registerNick( this, nickname, new_nickname );

				nickname = new_nickname;
			}
			
			participant_messages.clear();
		}
		
		public List<ChatMessage>
		getMessages()
		{
			synchronized( chat.chat_lock ){
				
				return( new ArrayList<ChatMessage>( participant_messages ));
			}
		}
		
		public boolean
		isIgnored()
		{
			return( is_ignored );
		}
		
		public void
		setIgnored(
			boolean		b )
		{
			if ( b != is_ignored ){
				
				is_ignored = b;
				
				synchronized( chat.chat_lock ){

					for ( ChatMessage message: participant_messages ){
						
						message.setIgnored( b || is_spammer);
					}
				}
			}
		}
		
		public boolean
		isSpammer()
		{
			return( is_spammer );
		}
		
		public boolean
		canSpammer()
		{
			return( participant_messages.size() >= 5 && !is_spammer );
		}
		
		public void
		setSpammer(
			boolean		b )
		{
			if ( b != is_spammer ){
				
				is_spammer = b;
				
				chat.setSpammer( this, b );

				synchronized( chat.chat_lock ){

					for ( ChatMessage message: participant_messages ){
						
						message.setIgnored( b || is_ignored );
					}
				}
			}
		}
		
		public boolean
		isPinned()
		{
			return( is_pinned );
		}
		
		private String
		getPinKey()
		{
			return( "azbuddy.chat.pinned." + ByteFormatter.encodeString( pk, 0, 16 ));
		}
		
		public void
		setPinned(
			boolean		b )
		{
			if ( b != is_pinned ){
			
				is_pinned = b;
				
				String key = getPinKey();
				
				if ( is_pinned ){
				
					COConfigurationManager.setParameter( key, true );
					
				}else{
				
					COConfigurationManager.removeParameter( key );
				}
				
				COConfigurationManager.setDirty();
			}
		}
		
		public boolean
		isNickClash()
		{
			return( nick_clash );
		}
		
		private void
		setNickClash(
			boolean	b )
		{
			nick_clash = b;
		}
		
		public ChatInstance
		createPrivateChat()
		
			throws Exception
		{			
			ChatInstance inst = BuddyPluginBeta.this.getChat( this );
			
			ChatInstance	parent = getChat();
			
			if ( !parent.isSharedNickname()){
				
				inst.setSharedNickname( false );
				
				inst.setInstanceNickname( parent.getInstanceNickname());
			}

			return( inst );
		}
	}
	
	public class
	ChatMessage
	{
		public static final int MT_NORMAL	= 1;
		public static final int MT_INFO		= 2;
		public static final int MT_ERROR	= 3;
		
		private final int						uid;
		private final Map<String,Object>		map;
		
		private final byte[]					message_id;
		private final long						timestamp;

		private ChatParticipant					participant;

		private byte[]							previous_id;
				
		private boolean							is_ignored;
		private boolean							is_nick_clash;
		
		private
		ChatMessage(
			int						_uid,
			Map<String,Object>		_map )
		{
			uid		= _uid;
			map		= _map;
			
			message_id = (byte[])map.get( "id" );
			
			timestamp = SystemTime.getCurrentTime() - getAge()*1000L;

			Map<String,Object> payload = getPayload();
			
			previous_id = (byte[])payload.get( "pre" );
		}
		
		protected int
		getUID()
		{
			return( uid );
		}
		
		private void
		setParticipant(
			ChatParticipant		p )
		{
			participant	= p;
		}
		
		public ChatParticipant
		getParticipant()
		{
			return( participant );
		}
		
		private void
		setNickClash(
			boolean	clash )
		{
			is_nick_clash = clash;
		}
		
		public boolean
		isNickClash()
		{
			return( is_nick_clash );
		}
		
		private Map<String,Object>
		getPayload()
		{
			try{
				byte[] content_bytes = (byte[])map.get( "content" );
				
				if ( content_bytes != null && content_bytes.length > 0 ){
					
					return( BDecoder.decode( content_bytes ));
				}
			}catch( Throwable e){
			}
			
			return( new HashMap<String,Object>());
		}
		
		private int
		getMessageStatus()
		{
			Map<String,Object> payload = getPayload();

			if ( payload != null ){
				
				Map<String,Object>	flags = (Map<String,Object>)payload.get( "f" );
				
				if ( flags != null ){
					
					Number status = (Number)flags.get( "s" );
					
					if ( status != null ){
						
						return( status.intValue());
					}
				}
			}
			
			return( FLAGS_MSG_STATUS_CHAT_NONE );
		}
		
		public String
		getMessage()
		{
			try{
				String	report = (String)map.get( "error" );
				
				if ( report != null ){
					
					if ( report.length() > 2 && report.charAt(1) == ':' ){
						
						return( report.substring( 2 ));
					}
					
					return( report );
				}
				
				if ( getMessageStatus() == FLAGS_MSG_STATUS_CHAT_QUIT ){
					
					return( participant.getName() + " has quit" );
				}
				
					// was just a string for a while...
				
				Map<String,Object> payload = getPayload();
					
				if ( payload != null ){
					
					byte[] msg_bytes = (byte[])payload.get( "msg" );
					
					if ( msg_bytes != null ){
					
						return( new String( msg_bytes, "UTF-8" ));
					}
				}
				
				
				
				return( new String((byte[])map.get( "content" ), "UTF-8" ));
				
			}catch( Throwable e ){
				
				Debug.out( e );
					
				return( "" );
			}
		}
		
		public int
		getMessageType()
		{
			String	report = (String)map.get( "error" );
			
			if ( report == null ){
				
				if ( getMessageStatus() == FLAGS_MSG_STATUS_CHAT_QUIT ){
					
					return( MT_INFO );
				}
				
				return( MT_NORMAL );
				
			}else{
				
				if ( report.length() < 2 || report.charAt(1) != ':' ){
					
					return( MT_ERROR );
				}
								
				char type = report.charAt(0);
				
				if ( type == 'i' ){
					
					return( MT_INFO );
				}else{
					
					return( MT_ERROR );
				}
			}
		}
		
		public boolean
		isIgnored()
		{
			return( is_ignored );
		}
		
		public void
		setIgnored(
			boolean		b )
		{
			is_ignored = b;
		}
		
		public byte[]
		getID()
		{
			return( message_id );
		}
		
		public byte[]
		getPreviousID()
		{
			return( previous_id );
		}
		
		private void
		setPreviousID(
			byte[]		pid )
		{
			previous_id = pid;
		}
		
		public byte[]
		getPublicKey()
		{
			return((byte[])map.get( "pk" ));
		}
		
		public Map<String,Object>
		getContact()
		{
			return((Map<String,Object>)map.get( "contact" ));
		}
		
		public InetSocketAddress
		getAddress()
		{
			return((InetSocketAddress)map.get( "address" ));
		}
		
		private int
		getAge()
		{
			return(((Number)map.get( "age" )).intValue());
		}
		
		public long
		getTimeStamp()
		{
			return( timestamp );
		}
		
		public String
		getNickName()
		{		
				// always use payload if available (for real messages)
			
			Map<String,Object> payload = getPayload();
			
			if ( payload != null ){
				
				byte[] nick = (byte[])payload.get( "nick" );
				
				if ( nick != null ){
					
					try{
						String str = new String( nick, "UTF-8" );
						
						if ( str.length() > 0 ){
						
							return( str );
						}
					}catch( Throwable e ){
					}
				}
			}
			
				// otherwise assume it is internally generated for non-normal messages
			
			if ( getMessageType() != ChatMessage.MT_NORMAL ){
				
				String nick = participant.getChat().getNickname();
				
				if ( nick.length() > 0 ){
					
					return( nick );
				}
			}

				// default when no user specified one present
			
			return( pkToString( getPublicKey()));
		}
	}
	
	public interface
	ChatManagerListener
	{
		public void
		chatAdded(
			ChatInstance	inst );
		
		public void
		chatRemoved(
			ChatInstance	inst );
	}
	
	public interface
	ChatListener
	{
		public void
		messageReceived(
			ChatMessage				message );
		
		public void
		messagesChanged();
		
		public void
		participantAdded(
			ChatParticipant			participant );
		
		public void
		participantChanged(
			ChatParticipant			participant );
		
		public void
		participantRemoved(
			ChatParticipant			participant );
		
		public void
		stateChanged(
			boolean					avail );
		
		public void
		updated();
	}
	
	public interface
	FTUXStateChangeListener
	{
		public void
		stateChanged(
			boolean	accepted );
	}
}
