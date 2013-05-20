/*
 * Created on Mar 20, 2013
 * Created by Paul Gardner
 * 
 * Copyright 2013 Azureus Software, Inc.  All rights reserved.
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


package com.aelitis.azureus.core.tag.impl;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.FrequencyLimitedDispatcher;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimeFormatter;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.core3.xml.util.XMLEscapeWriter;
import org.gudy.azureus2.core3.xml.util.XUXmlWriter;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadScrapeResult;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageRequest;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageResponse;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreLifecycleAdapter;
import com.aelitis.azureus.core.rssgen.RSSGeneratorPlugin;
import com.aelitis.azureus.core.tag.*;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.util.MapUtils;

public class 
TagManagerImpl
	implements TagManager
{
	private static final String	CONFIG_FILE 				= "tag.config";
	
		// order is important as 'increases' in effects (see applyConfigUpdates)
	
	private static final int CU_TAG_CREATE		= 1;
	private static final int CU_TAG_CHANGE		= 2;
	private static final int CU_TAG_CONTENTS	= 3;
	private static final int CU_TAG_REMOVE		= 4;
	
	private static TagManagerImpl	singleton;
	
	public static synchronized TagManagerImpl
	getSingleton()
	{
		if ( singleton == null ){
			
			singleton = new TagManagerImpl();
			
			singleton.init();
		}
		
		return( singleton );
	}
	
	private CopyOnWriteList<TagType>	tag_types = new CopyOnWriteList<TagType>();
	
	private Map<Integer,TagType>	tag_type_map = new HashMap<Integer, TagType>();
	
	private static final String RSS_PROVIDER	= "tags";
	
	private Set<TagBase>	rss_tags = new HashSet<TagBase>();
	
	private RSSGeneratorPlugin.Provider rss_generator = 
		new RSSGeneratorPlugin.Provider()
		{
			public boolean 
			isEnabled() 
			{
				return( true );
			}
			
			public boolean
			generate(
				TrackerWebPageRequest		request,
				TrackerWebPageResponse		response )
			
				throws IOException
			{
				URL	url	= request.getAbsoluteURL();
				
				String path = url.getPath();
				
				int	pos = path.indexOf( '?' );
				
				if ( pos != -1 ){
					
					path = path.substring(0,pos);
				}
				
				path = path.substring( RSS_PROVIDER.length()+1);

				XMLEscapeWriter pw = new XMLEscapeWriter( new PrintWriter(new OutputStreamWriter( response.getOutputStream(), "UTF-8" )));

				pw.setEnabled( false );
				
				if ( path.length() <= 1 ){
					
					response.setContentType( "text/html; charset=UTF-8" );
					
					pw.println( "<HTML><HEAD><TITLE>Vuze Tag Feeds</TITLE></HEAD><BODY>" );
					
					Map<String,String>	lines = new TreeMap<String, String>();
					
					List<TagBase>	tags;
					
					synchronized( rss_tags ){

						tags = new ArrayList<TagBase>( rss_tags);
					}
					
					for ( TagBase t: tags ){
					
						if ( t instanceof TagDownload ){
							
							if ( ((TagFeatureRSSFeed)t).isTagRSSFeedEnabled()){
										
								String	name = t.getTagName( true );
								
								String	tag_url = RSS_PROVIDER + "/" + t.getTagType().getTagType()+"-" + t.getTagID();
							
								lines.put( name, "<LI><A href=\"" + tag_url + "\">" + name + "</A>&nbsp;&nbsp;-&nbsp;&nbsp;<font size=\"-1\"><a href=\"" + tag_url + "?format=html\">html</a></font></LI>" );
							}
						}
					}
					
					for ( String line: lines.values() ){
						
						pw.println( line );
					}
					
					pw.println( "</BODY></HTML>" );
					
				}else{
					
					String	tag_id = path.substring( 1 );
					
					String[] bits = tag_id.split( "-" );
					
					int	tt_id 	= Integer.parseInt( bits[0] );
					int	t_id 	= Integer.parseInt( bits[1] );
						
					TagDownload	tag = null;
					
					synchronized( rss_tags ){

						for ( TagBase t: rss_tags ){
							
							if ( t.getTagType().getTagType() == tt_id && t.getTagID() == t_id ){
								
								if ( t instanceof TagDownload ){
									
									tag = (TagDownload)t;
								}
							}
						}
					}
					
					if ( tag == null ){
						
						response.setReplyStatus( 404 );
						
						return( true );
					}
					
					Set<DownloadManager> dms = tag.getTaggedDownloads();
					
					List<Download> downloads = new ArrayList<Download>( dms.size());
					
					long	dl_marker = 0;
					
					for ( DownloadManager dm: dms ){
						
						TOTorrent torrent = dm.getTorrent();
						
						if ( torrent == null ){
							
							continue;
						}
						
						if ( !TorrentUtils.isReallyPrivate( torrent )){
						
							dl_marker += dm.getDownloadState().getLongParameter( DownloadManagerState.PARAM_DOWNLOAD_ADDED_TIME );
						
							downloads.add( PluginCoreUtils.wrap(dm));
						}
					}
					
					if ( url.toExternalForm().contains( "format=html" )){
						
						String	host = (String)request.getHeaders().get( "host" );
						
						if ( host != null ){
							
							int	c_pos = host.indexOf( ':' );
							
							if ( c_pos != -1 ){
								
								host = host.substring( 0, c_pos );
							}
						}else{
							
							host = "127.0.0.1";
						}
						
						response.setContentType( "text/html; charset=UTF-8" );
						
						pw.println( "<HTML><HEAD><TITLE>Tag: " + escape( tag.getTagName( true )) + "</TITLE></HEAD><BODY>" );

						PluginManager pm = AzureusCoreFactory.getSingleton().getPluginManager();
						
						PluginInterface pi = pm.getPluginInterfaceByID( "azupnpav", true );

						if ( pi == null ){
							
							pw.println( "UPnP Media Server plugin not found" );
							
						}else{
							
							for (int i=0;i<downloads.size();i++){
								
								Download download = downloads.get( i );
																
								DiskManagerFileInfo[] files = download.getDiskManagerFileInfo();
								
								for ( DiskManagerFileInfo file: files ){
									
									File target_file = file.getFile( true );
									
									if ( !target_file.exists()){
										
										continue;
									}
									
									try{
										URL stream_url = new URL((String)pi.getIPC().invoke("getContentURL", new Object[] { file }));
						  									  				
						  				if ( stream_url != null ){
						  						
						  					stream_url = UrlUtils.setHost( stream_url, host );
						  					
						  					String url_ext = stream_url.toExternalForm();
						  					
						  					pw.println( "<p>" );
						  					
						  					pw.println( "<a href=\"" + url_ext + "\">" + escape( target_file.getName()) + "</a>" );
						  						
						  					url_ext += url_ext.indexOf('?') == -1?"?":"&";
						  					
						  					url_ext += "action=download";
						  					
						  					pw.println( "&nbsp;&nbsp;-&nbsp;&nbsp;<font size=\"-1\"><a href=\"" + url_ext + "\">save</a></font>" );
						  				}
									}catch( Throwable e ){
										
										e.printStackTrace();
									}
								}
							}
						}
						
						pw.println( "</BODY></HTML>" );
					}else{
						
						String	config_key = "tag.rss.config." + tt_id + "." + t_id;
						
						long	old_marker = COConfigurationManager.getLongParameter( config_key + ".marker", 0 );
						
						long	last_modified = COConfigurationManager.getLongParameter( config_key + ".last_mod", 0 );
						
						long now = SystemTime.getCurrentTime();
						
						if ( old_marker == dl_marker ){
							
							if ( last_modified == 0 ){
								
								last_modified = now;
							}
						}else{
							
							COConfigurationManager.setParameter( config_key + ".marker", dl_marker );
							
							last_modified = now; 
						}
						
						if ( last_modified == now ){
							
							COConfigurationManager.setParameter( config_key + ".last_mod", last_modified );
						}
						
						pw.println( "<?xml version=\"1.0\" encoding=\"utf-8\"?>" );
						
						pw.println( "<rss version=\"2.0\" xmlns:vuze=\"http://www.vuze.com\">" );
						
						pw.println( "<channel>" );
						
						pw.println( "<title>" + escape( tag.getTagName( true )) + "</title>" );
						
						Collections.sort(
								downloads,
							new Comparator<Download>()
							{
								public int 
								compare(
									Download d1, 
									Download d2) 
								{
									long	added1 = getAddedTime( d1 )/1000;
									long	added2 = getAddedTime( d2 )/1000;
					
									return((int)(added2 - added1 ));
								}
							});
											
										
						pw.println(	"<pubDate>" + TimeFormatter.getHTTPDate( last_modified ) + "</pubDate>" );
					
						for (int i=0;i<downloads.size();i++){
							
							Download download = downloads.get( i );
							
							DownloadManager	core_download = PluginCoreUtils.unwrap( download );
							
							Torrent torrent = download.getTorrent();
							
							byte[] hash = torrent.getHash();
							
							String	hash_str = Base32.encode( hash );
							
							pw.println( "<item>" );
							
							pw.println( "<title>" + escape( download.getName()) + "</title>" );
							
							pw.println( "<guid>" + hash_str + "</guid>" );
							
							String magnet_url = escape( UrlUtils.getMagnetURI( download.getName(), torrent ));
	
							pw.println( "<link>" + magnet_url + "</link>" );
							
							long added = core_download.getDownloadState().getLongParameter(DownloadManagerState.PARAM_DOWNLOAD_ADDED_TIME);
							
							pw.println(	"<pubDate>" + TimeFormatter.getHTTPDate( added ) + "</pubDate>" );
							
							pw.println(	"<vuze:size>" + torrent.getSize()+ "</vuze:size>" );
							pw.println(	"<vuze:assethash>" + hash_str + "</vuze:assethash>" );
															
							pw.println( "<vuze:downloadurl>" + magnet_url + "</vuze:downloadurl>" );
					
							DownloadScrapeResult scrape = download.getLastScrapeResult();
							
							if ( scrape != null && scrape.getResponseType() == DownloadScrapeResult.RT_SUCCESS ){
								
								pw.println(	"<vuze:seeds>" + scrape.getSeedCount() + "</vuze:seeds>" );
								pw.println(	"<vuze:peers>" + scrape.getNonSeedCount() + "</vuze:peers>" );
							}
							
							pw.println( "</item>" );
						}
						
						pw.println( "</channel>" );
						
						pw.println( "</rss>" );
					}
				}
				 
				pw.flush();
				
				return( true );		
			}
			
			protected long
			getAddedTime(
				Download	download )
			{
				DownloadManager	core_download = PluginCoreUtils.unwrap( download );
				
				return( core_download.getDownloadState().getLongParameter(DownloadManagerState.PARAM_DOWNLOAD_ADDED_TIME));
			}
			
			protected String
			escape(
				String	str )
			{
				return( XUXmlWriter.escapeXML(str));
			}
		};
	
	private FrequencyLimitedDispatcher dirty_dispatcher = 
		new FrequencyLimitedDispatcher(
			new AERunnable()
			{
				public void
				runSupport()
				{
					try{
							// just in case there's a bunch of changes coming in together
						
						Thread.sleep( 1000 );
						
					}catch( Throwable e ){
						
					}
					
					writeConfig();
				}
			},
			30*1000 );
	
	
	private Map					config;
	private WeakReference<Map>	config_ref;
	
	private boolean				config_dirty;
	
	private List<Object[]>		config_change_queue = new ArrayList<Object[]>();
	
	
	private CopyOnWriteList<TagManagerListener>		listeners = new CopyOnWriteList<TagManagerListener>();
	
	private CopyOnWriteList<Object[]>				feature_listeners = new CopyOnWriteList<Object[]>();
	
	
	
	private
	TagManagerImpl()
	{
	}
	
	private void
	init()
	{
		AzureusCoreFactory.getSingleton().addLifecycleListener(
			new AzureusCoreLifecycleAdapter()
			{
				public void
				stopped(
					AzureusCore		core )
				{
					destroy();
				}
			});
		
		SimpleTimer.addPeriodicEvent(
			"TM:Sync",
			30*1000,
			new TimerEventPerformer()
			{
				public void 
				perform(
					TimerEvent event) 
				{
					for ( TagType tt: tag_types ){
						
						((TagTypeBase)tt).sync();
					}
				}
			});
	}
	
	private void
	resolverInitialized(
		TaggableResolver		resolver )
	{
		TagTypeDownloadManual ttdm = new TagTypeDownloadManual( resolver );
		
		List<Tag> tags = new ArrayList<Tag>();
		
		synchronized( this ){
			
			Map config = getConfig();
			
			Map<String,Object> tt = (Map<String,Object>)config.get( String.valueOf( ttdm.getTagType()));
			
			if ( tt != null ){
				
				for ( Map.Entry<String,Object> entry: tt.entrySet()){
					
					String key = entry.getKey();
					
					try{
						if ( Character.isDigit( key.charAt(0))){
						
							int	tag_id 	= Integer.parseInt( key );
							Map m		= (Map)entry.getValue();
							
							tags.add( ttdm.createTag( tag_id, m ));
						}
					}catch( Throwable e ){
						
						Debug.out( e );
					}
				}
			}
		}
		
		for ( Tag tag: tags ){
			
			ttdm.addTag( tag );
		}
	}
	
	private void
	removeTaggable(
		TaggableResolver	resolver,
		Taggable			taggable )
	{
		for ( TagType	tt: tag_types ){
			
			TagTypeBase	ttb = (TagTypeBase)tt;
			
			ttb.removeTaggable( resolver, taggable );
		}
	}
		
	public void
	addTagType(
		TagType		tag_type )
	{
		synchronized( tag_type_map ){
			
			if ( tag_type_map.put( tag_type.getTagType(), tag_type) != null ){
				
				Debug.out( "Duplicate tag type!" );
			}
		}
		
		tag_types.add( tag_type );

		for ( TagManagerListener l : listeners ){
			
			try{
				l.tagTypeAdded(this, tag_type);
				
			}catch ( Throwable t ){
				
				Debug.out(t);
			}
		}
	}
	
	public TagType 
	getTagType(
		int 	tag_type) 
	{
		synchronized( tag_type_map ){

			return( tag_type_map.get( tag_type ));
		}
	}
	
	protected void
	removeTagType(
		TagType		tag_type )
	{
		synchronized( tag_type_map ){
			
			tag_type_map.remove( tag_type.getTagType());
		}
		
		tag_types.remove( tag_type );
		
		for ( TagManagerListener l : listeners ){
			
			try{
				l.tagTypeRemoved(this, tag_type);
				
			}catch( Throwable t ){
				
				Debug.out(t);
			}
		}
		
		removeConfig( tag_type );
	}
	
	public List<TagType>
	getTagTypes()
	{
		return( tag_types.getList());
	}
	
	public List<Tag>
	getTagsForTaggable(
		Taggable	taggable )
	{
		Set<Tag>	result = new HashSet<Tag>();
		
		for ( TagType tt: tag_types ){
			
			result.addAll( tt.getTagsForTaggable( taggable ));
		}
		
		return( new ArrayList<Tag>( result ));
	}
	
	public Tag
	lookupTagByUID(
		long	tag_uid )
	{
		int	tag_type_id = (int)((tag_uid>>32)&0xffffffffL);
		
		TagType tt = tag_type_map.get( tag_type_id );
				
		if ( tt != null ){
				
			int	tag_id = (int)(tag_uid&0xffffffffL);
			
			return( tt.getTag( tag_id ));
		}
		
		return( null );
	}
	
	public TaggableLifecycleHandler
	registerTaggableResolver(
		final TaggableResolver	resolver )
	{
		return(
			new TaggableLifecycleHandler()
			{
				public void 
				initialized() 
				{				
					resolverInitialized( resolver );
				}
				
				public void
				taggableCreated(
					Taggable	t )
				{	
					// could do some initial tag allocations here
				}
				
				public void
				taggableDestroyed(
					Taggable	t )
				{
					removeTaggable( resolver, t );
				}
			});
	}
	
	public void
	setTagPublicDefault(
		boolean	pub )
	{
		COConfigurationManager.setParameter( "tag.manager.pub.default", pub );
	}
	
	public boolean
	getTagPublicDefault()
	{
		return( COConfigurationManager.getBooleanParameter( "tag.manager.pub.default", true ));
	}
	
	protected void
	checkRSSFeeds(
		TagBase		tag,
		boolean		enable )
	{
		synchronized( rss_tags ){
			
			if ( enable ){
				
				if ( rss_tags.contains( tag )){
					
					return;
				}
				
				rss_tags.add( tag );
				
				if ( rss_tags.size() > 1 ){
					
					return;
					
				}else{
					
					RSSGeneratorPlugin.registerProvider( RSS_PROVIDER, rss_generator  );
				}
			}else{

				rss_tags.remove( tag );
				
				if ( rss_tags.size() == 0 ){
					
					RSSGeneratorPlugin.unregisterProvider( RSS_PROVIDER  );
				}
			}
		}
	}
	
	public void
	addTagManagerListener(
		TagManagerListener		listener,
		boolean					fire_for_existing )
	{
		listeners.add( listener );
		
		if ( fire_for_existing ){
						
			for (TagType tt: tag_types ){
				
				listener.tagTypeAdded( this, tt );
			}
		}
	}
	
	public void
	removeTagManagerListener(
		TagManagerListener		listener )
	{
		listeners.remove( listener );
	}
	
	public void
	addTagFeatureListener(
		int						features,
		TagFeatureListener		listener )
	{
		feature_listeners.add( new Object[]{ features, listener });	
	}
	
	public void
	removeTagFeatureListener(
		TagFeatureListener		listener )
	{
		for ( Object[] entry: feature_listeners ){
			
			if ( entry[1] == listener ){
				
				feature_listeners.remove( entry );
			}
		}
	}
	
	protected void
	featureChanged(
		Tag			tag,
		int			feature )
	{
		for ( Object[] entry: feature_listeners ){
			
			if ((((Integer)entry[0]) & feature ) != 0 ){
		
				try{
					((TagFeatureListener)entry[1]).tagFeatureChanged( tag, feature );
					
				}catch( Throwable e ){
					
					Debug.out( e );
				}
			}
		}
	}
	
	protected void
	tagCreated(
		TagWithState	tag )
	{
		addConfigUpdate( CU_TAG_CREATE, tag );
	}
	
	protected void
	tagChanged(
		TagWithState	tag )
	{
		addConfigUpdate( CU_TAG_CHANGE, tag );

	}
	
	protected void
	tagRemoved(
		TagWithState	tag )
	{
		addConfigUpdate( CU_TAG_REMOVE, tag );
	}
	
	protected void
	tagContentsChanged(
		TagWithState	tag )
	{
		addConfigUpdate( CU_TAG_CONTENTS, tag );
	}
	
	private void
	addConfigUpdate(
		int				type,
		TagWithState	tag )
	{
		if ( !tag.getTagType().isTagTypePersistent()){
			
			return;
		}
		
		if ( tag.isRemoved() && type != CU_TAG_REMOVE ){
			
			return;
		}
		
		synchronized( this ){
			
			config_change_queue.add( new Object[]{ type, tag });
		}
		    
		setDirty();
	}
	
	private void
	applyConfigUpdates(
		Map			config )
	{
		Map<TagWithState,Integer>	updates = new HashMap<TagWithState, Integer>();
		
		for ( Object[] update: config_change_queue ){
			
			int				type	= (Integer)update[0];
			TagWithState	tag 	= (TagWithState)update[1];
			
			if ( tag.isRemoved()){
				
				type = CU_TAG_REMOVE;
			}
			
			Integer existing = updates.get( tag );
			
			if ( existing == null ){
				
				updates.put( tag, type );
				
			}else{
				
				if ( existing == CU_TAG_REMOVE ){
					
				}else if ( type > existing ){
					
					updates.put( tag, type );			
				}
			}
		}
		
		for ( Map.Entry<TagWithState,Integer> entry: updates.entrySet()){
			
			TagWithState 	tag = entry.getKey();
			int				type	= entry.getValue();
			
			TagType	tag_type = tag.getTagType();
			
			String tt_key = String.valueOf( tag_type.getTagType());
			
			Map tt = (Map)config.get( tt_key );
			
			if ( tt == null ){
				
				if ( type == CU_TAG_REMOVE ){
					
					continue;
				}
				
				tt = new HashMap();
					
				config.put( tt_key, tt );
			}
			
			String t_key = String.valueOf( tag.getTagID());
			
			if ( type == CU_TAG_REMOVE ){
				
				tt.remove( t_key );
				
				continue;
			}
			
			Map t = (Map)tt.get( t_key );
			
			if ( t == null ){
				
				t = new HashMap();
				
				tt.put( t_key, t );
			}
			
			tag.exportDetails( t, type == CU_TAG_CONTENTS );
		}

		config_change_queue.clear();
	}
	
	private void
	destroy()
	{
		writeConfig();
	}
	
	private void
	setDirty()
	{
		synchronized( this ){
			
			if ( !config_dirty ){
		
				config_dirty = true;
	
				dirty_dispatcher.dispatch();
			}
		}
	}
	
	private Map
	readConfig()
	{
		Map map;
		
		if ( FileUtil.resilientConfigFileExists( CONFIG_FILE )){
			
			map = FileUtil.readResilientConfigFile( CONFIG_FILE );
			
		}else{
			
			map = new HashMap();
		}
		
		return( map );
	}
	
	private Map
	getConfig()
	{
		synchronized( this ){
			
			if ( config != null ){
				
				return( config );
			}
			
			if ( config_ref != null ){
					
				config = config_ref.get();
				
				if ( config != null ){
					
					return( config );
				}
			}
							
			config = readConfig();
			
			return( config );
		}
	}
	
	private void
	writeConfig()
	{
		synchronized( this ){
			
			if ( !config_dirty ){
				
				return;
			}
	
			config_dirty = false;
			
			if ( config_change_queue.size() > 0 ){
			
				applyConfigUpdates( getConfig());
			}
			
			if ( config != null ){
							
				FileUtil.writeResilientConfigFile( CONFIG_FILE, config );
				
				config_ref = new WeakReference<Map>( config );
				
				config = null;
			}
		}
	}
	
	private Map
	getConf(
		TagTypeBase	tag_type,
		TagBase		tag,
		boolean		create )
	{
		Map m = getConfig();
		
		String tt_key = String.valueOf( tag_type.getTagType());

		Map tt = (Map)m.get( tt_key );
		
		if ( tt == null ){
			
			if ( create ){
				
				tt = new HashMap();
				
				m.put( tt_key, tt );
				
			}else{
				
				return( null );
			}
		}
		
		String t_key = String.valueOf( tag.getTagID());

		Map t = (Map)tt.get( t_key );
		
		if ( t == null ){
			
			if ( create ){
				
				t = new HashMap();
				
				tt.put( t_key, t );
				
			}else{
			
				return( null );
			}
		}
		
		Map conf = (Map)t.get( "c" );
		
		if ( conf == null && create ){
			
			conf = new HashMap();
			
			t.put( "c", conf );
		}
		
		return( conf );
	}
	
	protected Boolean
	readBooleanAttribute(
		TagTypeBase	tag_type,
		TagBase		tag,
		String		attr,
		Boolean		def )
	{
		Long result = readLongAttribute(tag_type, tag, attr, def==null?null:(def?1L:0L));
		
		if ( result == null ){
			
			return( null );
		}
		
		return( result == 1 );
	}
	
	protected void
	writeBooleanAttribute(
		TagTypeBase		tag_type,
		TagBase			tag,
		String			attr,
		boolean			value )
	{
		writeLongAttribute( tag_type, tag, attr, value?1:0 );
	}
	
	protected Long
	readLongAttribute(
		TagTypeBase	tag_type,
		TagBase		tag,
		String		attr,
		Long		def )
	{
		try{
			synchronized( this ){
				
				Map conf = getConf( tag_type, tag, false );
				
				if ( conf == null ){
					
					return( def );
				}
				
				Long value = (Long)conf.get( attr );
				
				if ( value == null ){
					
					return( def );
				}
				
				return( value );
			}
		}catch( Throwable e ){
			
			Debug.out( e );
			
			return( def );
		}
	}
	
	protected void
	writeLongAttribute(
		TagTypeBase		tag_type,
		TagBase			tag,
		String			attr,
		long			value )
	{
		try{
			synchronized( this ){
				
				Map conf = getConf( tag_type, tag, true );
				
				long old = MapUtils.getMapLong( conf, attr, 0 );
				
				if ( old == value && conf.containsKey( attr )){
					
					return;
				}
				
				conf.put( attr, value );
				
				setDirty();
			}
		}catch( Throwable e ){
			
			Debug.out( e );
		}
	}	
	
	protected String
	readStringAttribute(
		TagTypeBase	tag_type,
		TagBase		tag,
		String		attr,
		String		def )
	{
		try{
			synchronized( this ){
				
				Map conf = getConf( tag_type, tag, false );
				
				if ( conf == null ){
					
					return( def );
				}
				
				return( MapUtils.getMapString( conf, attr, def ));
			}
		}catch( Throwable e ){
			
			Debug.out( e );
			
			return( def );
		}
	}
	
	protected void
	writeStringAttribute(
		TagTypeBase		tag_type,
		TagBase			tag,
		String			attr,
		String			value )
	{
		try{
			synchronized( this ){
				
				Map conf = getConf( tag_type, tag, true );
				
				String old = MapUtils.getMapString( conf, attr, null );
				
				if ( old == value ){
					
					return;
					
				}else if ( old != null && value != null && old.equals( value )){
					
					return;
				}
				
				MapUtils.setMapString( conf, attr, value );
				
				setDirty();
			}
		}catch( Throwable e ){
			
			Debug.out( e );
		}
	}	
	
	protected void
	removeConfig(
		TagType	tag_type )
	{
		synchronized( this ){
			
			Map m = getConfig();
			
			String tt_key = String.valueOf( tag_type.getTagType());
			
			Map tt = (Map)m.remove( tt_key );

			if ( tt != null ){
				
				setDirty();
			}
		}
	}
	
	protected void
	removeConfig(
		Tag	tag )
	{
		TagType	tag_type = tag.getTagType();
		
		synchronized( this ){
			
			Map m = getConfig();
			
			String tt_key = String.valueOf( tag_type.getTagType());
			
			Map tt = (Map)m.get( tt_key );

			if ( tt == null ){
				
				return;
			}
			
			String t_key = String.valueOf( tag.getTagID());
			
			Map t = (Map)tt.remove( t_key );
			
			if ( t != null ){
				
				setDirty();
			}
		}
	}
}
