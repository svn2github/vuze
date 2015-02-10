/*
 * Created on Mar 20, 2013
 * Created by Paul Gardner
 * 
 * Copyright 2013 Azureus Software, Inc.  All rights reserved.
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
import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerInitialisationAdapter;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.AENetworkClassifier;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.BDecoder;
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
import org.gudy.azureus2.plugins.download.DownloadCompletionListener;
import org.gudy.azureus2.plugins.download.DownloadScrapeResult;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageRequest;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageResponse;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreComponent;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreLifecycleAdapter;
import com.aelitis.azureus.core.rssgen.RSSGeneratorPlugin;
import com.aelitis.azureus.core.tag.*;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.core.util.IdentityHashSet;
import com.aelitis.azureus.util.MapUtils;

public class 
TagManagerImpl
	implements TagManager, DownloadCompletionListener
{
	private static final String	CONFIG_FILE 				= "tag.config";
	
		// order is important as 'increases' in effects (see applyConfigUpdates)
	
	private static final int CU_TAG_CREATE		= 1;
	private static final int CU_TAG_CHANGE		= 2;
	private static final int CU_TAG_CONTENTS	= 3;
	private static final int CU_TAG_REMOVE		= 4;
	
	private static final boolean	enabled = COConfigurationManager.getBooleanParameter( "tagmanager.enable", true );
	
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
	
	private Set<DownloadManager>	active_copy_on_complete = new IdentityHashSet<DownloadManager>();
	
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
				
				String	query = url.getQuery();
				
				if ( query != null ){
					
					path += "?" + query;
				}
				
				int	pos = path.indexOf( '?' );
				
				if ( pos != -1 ){
					
					String args = path.substring( pos+1 );

					path = path.substring(0,pos);
					
					if ( path.endsWith( "GetTorrent" )){
						
						String[] bits = args.split( "&" );
						
						for ( String bit: bits ){
							
							String[] temp = bit.split( "=" );
							
							if ( temp.length == 2 ){
								
								if ( temp[0].equals( "hash" )){
									
									try{
										Download download = AzureusCoreFactory.getSingleton().getPluginManager().getDefaultPluginInterface().getDownloadManager().getDownload( Base32.decode( temp[1] ));
										
										Torrent torrent = download.getTorrent();
										
										response.getOutputStream().write( torrent.writeToBEncodedData());
										
										response.setContentType( "application/x-bittorrent" );
										
										return( true );
										
									}catch( Throwable e ){
										
									}
								}
							}
						}
						
						response.setReplyStatus( 404 );
						
						return( true );	
					}
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
					
					boolean	enable_low_noise = RSSGeneratorPlugin.getSingleton().isLowNoiseEnabled();

					
					Set<DownloadManager> dms = tag.getTaggedDownloads();
					
					List<Download> downloads = new ArrayList<Download>( dms.size());
					
					long	dl_marker = 0;
					
					for ( DownloadManager dm: dms ){
						
						TOTorrent torrent = dm.getTorrent();
						
						if ( torrent == null ){
							
							continue;
						}
						
						DownloadManagerState state = dm.getDownloadState();
						
						if ( state.getFlag( DownloadManagerState.FLAG_METADATA_DOWNLOAD )){
							
							continue;
						}
						
						if ( !enable_low_noise ){
							
							if (  state.getFlag( DownloadManagerState.FLAG_LOW_NOISE )){
								
								continue;
							}
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
							
							String magnet_uri = UrlUtils.getMagnetURI( download );
							
							String obtained_from = TorrentUtils.getObtainedFrom( core_download.getTorrent());
							
							String[] dl_nets = core_download.getDownloadState().getNetworks();
							
							boolean	added_fl = false;
							
							if ( obtained_from != null ){
								
								try{
									URL ou = new URL( obtained_from );
									
									if ( ou.getProtocol().toLowerCase( Locale.US ).startsWith( "http" )){
										
										String host = ou.getHost();
										
											// make sure the originator network is compatible with the ones enabled
											// for the download
										
										String net = AENetworkClassifier.categoriseAddress( host );
										
										boolean	net_ok = false;
										
										if ( dl_nets == null || dl_nets.length == 0 ){
										
											net_ok = true;
											
										}else{
											
											for ( String dl_net: dl_nets ){
											
												if ( dl_net == net ){
													
													net_ok = true;
													
													break;
												}
											}
										}
										
										if ( net_ok ){
											
											magnet_uri += "&fl=" + UrlUtils.encode( ou.toExternalForm());
											
											added_fl = true;
										}
									}
								}catch( Throwable e ){
									
								}
							}
							
								// in theory we could add multiple &fls but it keeps things less confusing
								// and more efficient to just use one - if an external link is available and
								// the torrent file is a reasonable size and the rss feed is popular then this
								// can avoid quite a bit of load - plus it reduces the size of magnet URI
							
							if ( !added_fl ){
							
								String host = (String)request.getHeaders().get( "host" );
								
								if ( host != null ){
									
										// don't need to check network here as we are replying with the network
										// used to contact us
									
									String local_fl = url.getProtocol() + "://" + host + "/" + RSS_PROVIDER + "/GetTorrent?hash=" + Base32.encode( torrent.getHash());
																	
									magnet_uri += "&fl=" + UrlUtils.encode( local_fl );
								}
							}
																			
							magnet_uri = escape( magnet_uri );
	
							pw.println( "<link>" + magnet_uri + "</link>" );
							
							long added = core_download.getDownloadState().getLongParameter(DownloadManagerState.PARAM_DOWNLOAD_ADDED_TIME);
							
							pw.println(	"<pubDate>" + TimeFormatter.getHTTPDate( added ) + "</pubDate>" );
							
							pw.println(	"<vuze:size>" + torrent.getSize()+ "</vuze:size>" );
							pw.println(	"<vuze:assethash>" + hash_str + "</vuze:assethash>" );
															
							pw.println( "<vuze:downloadurl>" + magnet_uri + "</vuze:downloadurl>" );
					
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
	
	AsyncDispatcher async_dispatcher = new AsyncDispatcher(5000);
	
	
	private Map					config;
	private WeakReference<Map>	config_ref;
	
	private boolean				config_dirty;
	
	private List<Object[]>		config_change_queue = new ArrayList<Object[]>();
	
	
	private CopyOnWriteList<TagManagerListener>		listeners = new CopyOnWriteList<TagManagerListener>();
	
	private CopyOnWriteList<Object[]>				feature_listeners = new CopyOnWriteList<Object[]>();
	
	private Map<Long,LifecycleHandlerImpl>			lifecycle_handlers = new HashMap<Long,LifecycleHandlerImpl>();
	
	private
	TagManagerImpl()
	{
	}
	
	public boolean
	isEnabled()
	{
		return( enabled );
	}
	
	private void
	init()
	{
		if ( !enabled ){
			
			return;
		}
		
		AzureusCore azureus_core = AzureusCoreFactory.getSingleton();
		
		final TagPropertyTrackerHandler auto_tracker = new TagPropertyTrackerHandler( azureus_core, this );
		
		new TagPropertyUntaggedHandler( azureus_core, this );
		
		new TagPropertyTrackerTemplateHandler( azureus_core, this );
		
		new TagPropertyConstraintHandler( azureus_core, this );
		
		azureus_core.addLifecycleListener(
			new AzureusCoreLifecycleAdapter()
			{
				public void
				started(
					AzureusCore		core )
				{
					core.getPluginManager().getDefaultPluginInterface().getDownloadManager().getGlobalDownloadEventNotifier().addCompletionListener( TagManagerImpl.this);
				}
				
				public void 
				componentCreated(
					AzureusCore 			core,
					AzureusCoreComponent 	component )
				{
					if ( component instanceof GlobalManager ){
						
						GlobalManager global_manager = (GlobalManager)component;
					
						global_manager.addDownloadManagerInitialisationAdapter(
							new DownloadManagerInitialisationAdapter()
							{	
								public int 
								getActions() 
								{
									return( ACT_PROCESSES_TAGS );
								}
								
								public void 
								initialised(
									DownloadManager 	manager,
									boolean				for_seeding ) 
								{
									if ( for_seeding ){
										
										return;
									}
									
										// perform any auto-tagging - note that auto-tags aren't applied to the download
										// yet
									
									List<Tag> auto_tags = auto_tracker.getTagsForDownload( manager );
									
									Set<Tag> tags = new HashSet<Tag>( getTagsForTaggable( TagType.TT_DOWNLOAD_MANUAL, manager ));
									
									tags.addAll( auto_tags );
									
									if ( tags.size() > 0 ){
										
										List<Tag>	sl_tags = new ArrayList<Tag>();
										
										for ( Tag tag: tags ){
											
											TagFeatureFileLocation fl = (TagFeatureFileLocation)tag;

											if ( fl.supportsTagInitialSaveFolder()){
												
												File save_loc = fl.getTagInitialSaveFolder();
												
												if ( save_loc != null ){
													
													sl_tags.add( tag );
												}
											}
										}
										
										if ( sl_tags.size() > 0 ){
											
											if ( sl_tags.size() > 1 ){
												
												Collections.sort(
													sl_tags,
													new Comparator<Tag>()
													{
														public int 
														compare(
															Tag o1, Tag o2) 
														{
															return( o1.getTagID() - o2.getTagID());
														}
													});
											}
											
											File new_loc = ((TagFeatureFileLocation)sl_tags.get(0)).getTagInitialSaveFolder();
											
											File old_loc = manager.getSaveLocation();
											
											if ( !new_loc.equals( old_loc )){
												
												manager.setTorrentSaveDir( new_loc.getAbsolutePath());
											}
										}
									}
								}
							});
					}
				}
				
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
	
	public void 
	onCompletion(
		Download d )
	{
		final DownloadManager manager = PluginCoreUtils.unwrap( d );
	
		List<Tag> tags = getTagsForTaggable( manager );
		
		List<Tag> cc_tags = new ArrayList<Tag>();
		
		for ( Tag tag: tags ){
			
			if ( tag.getTagType().hasTagTypeFeature( TagFeature.TF_FILE_LOCATION )){
			
				TagFeatureFileLocation fl = (TagFeatureFileLocation)tag;

				if ( fl.supportsTagCopyOnComplete()){
					
					File save_loc = fl.getTagCopyOnCompleteFolder();
					
					if ( save_loc != null ){
						
						cc_tags.add( tag );
					}
				}
			}
		}
		
		if ( cc_tags.size() > 0 ){
			
			if ( cc_tags.size() > 1 ){
				
				Collections.sort(
						cc_tags,
					new Comparator<Tag>()
					{
						public int 
						compare(
							Tag o1, Tag o2) 
						{
							return( o1.getTagID() - o2.getTagID());
						}
					});
			}
			
			final File new_loc = ((TagFeatureFileLocation)cc_tags.get(0)).getTagCopyOnCompleteFolder();
			
			File old_loc = manager.getSaveLocation();
			
			if ( !new_loc.equals( old_loc )){
				
				boolean do_it;
				
				synchronized( active_copy_on_complete ){
					
					if ( active_copy_on_complete.contains( manager )){
						
						do_it = false;
						
					}else{
						
						active_copy_on_complete.add( manager );
						
						do_it = true;
					}
				}
				
				if ( do_it ){
					
					new AEThread2( "tm:copy")
					{
						public void
						run()
						{
							try{
								long stopped_and_incomplete_start 	= 0;
								long looks_good_start 				= 0;
								
								while( true ){
									
									if ( manager.isDestroyed()){
										
										throw( new Exception( "Download has been removed" ));
									}
									
									DiskManager dm = manager.getDiskManager();
								
									if ( dm == null ){
										
										looks_good_start = 0;
										
										if ( !manager.getAssumedComplete()){
											
											long	now = SystemTime.getMonotonousTime();
											
											if ( stopped_and_incomplete_start == 0 ){
											
												stopped_and_incomplete_start = now;
												
											}else if ( now - stopped_and_incomplete_start > 30*1000 ){
												
												throw( new Exception( "Download is stopped and incomplete" ));
											}
										}else{
											
											break;
										}
									}else{
										
										stopped_and_incomplete_start = 0;
										
										if ( manager.getAssumedComplete()){
											
											if ( dm.getMoveProgress() == -1 && dm.getCompleteRecheckStatus() == -1 ){
												
												long	now = SystemTime.getMonotonousTime();
												
												if ( looks_good_start == 0 ){
												
													looks_good_start = now;
													
												}else if ( now - looks_good_start > 5*1000 ){
													
													break;
												}
											}
										}else{
											
											looks_good_start = 0;
										}
									}
									
									//System.out.println( "Waiting" );
									
									Thread.sleep( 1000 );
								}
								
								manager.copyDataFiles( new_loc );
								
								Logger.logTextResource(
									new LogAlert(
										manager, 
										LogAlert.REPEATABLE,
										LogAlert.AT_INFORMATION, 
										"alert.copy.on.comp.done"),
									new String[]{ manager.getDisplayName(), new_loc.toString()});
								 
							}catch( Throwable e ){
								
								 Logger.logTextResource(
									new LogAlert(
										manager, 
										LogAlert.REPEATABLE,
										LogAlert.AT_ERROR, 
										"alert.copy.on.comp.fail"),
									new String[]{ manager.getDisplayName(), new_loc.toString(), Debug.getNestedExceptionMessage(e)});
								 
							}finally{
								
								synchronized( active_copy_on_complete ){
									
									active_copy_on_complete.remove( manager );
								}
								
							}
						}
					}.start();
				}
			}
		}
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
		if ( !enabled ){
		
			Debug.out( "Not enabled" );
			
			return;
		}
		
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
	
	public void
	taggableAdded(
		TagType		tag_type,
		Tag			tag,
		Taggable	tagged )
	{
			// hack to support initial-save-location logic when a user manually assigns a tag and the download
			// hasn't had files allocated yet (most common scenario is user has 'add-torrent-stopped' set up)
		
		int tt = tag_type.getTagType();
		
		try{
			if ( tt == TagType.TT_DOWNLOAD_MANUAL && tagged instanceof DownloadManager ){
				
				TagFeatureFileLocation fl = (TagFeatureFileLocation)tag;
	
				if ( fl.supportsTagInitialSaveFolder()){
					
					File save_loc = fl.getTagInitialSaveFolder();
					
					if ( save_loc != null ){
	
						DownloadManager dm = (DownloadManager)tagged;
						
						if ( dm.getState() == DownloadManager.STATE_STOPPED ){
						
							TOTorrent torrent = dm.getTorrent();
							
							if ( torrent != null ){
								
									// This test detects whether or not we are in the process of adding the download
									// If we are then initial save-location stuff will be applied by the init-adapter
									// code above - we're only dealing later assignments here 
								
								if ( dm.getGlobalManager().getDownloadManager( torrent.getHashWrapper()) != null ){
									
									File existing_save_loc = dm.getSaveLocation();
									
									if ( ! ( existing_save_loc.equals( save_loc ) || existing_save_loc.exists())){
										
										dm.setTorrentSaveDir( save_loc.getAbsolutePath());
									}
								}
							}
						}
					}
				}
			}
		}catch( Throwable e ){
		
			Debug.out(e );
		}
		
			// hack to limit tagged/untagged callbacks as the auto-dl-state ones generate a lot
			// of traffic and thusfar nobody's interested in it
		
		if ( tt == TagType.TT_DOWNLOAD_MANUAL ){
			
			synchronized( lifecycle_handlers ){
				
				long type = tagged.getTaggableType();
				
				LifecycleHandlerImpl handler = lifecycle_handlers.get( type );
				
				if ( handler == null ){
					
					handler = new LifecycleHandlerImpl();
					
					lifecycle_handlers.put( type, handler );
				}
				
				handler.taggableTagged( tag_type, tag, tagged );
			}
		}
	}
	
	public void
	taggableRemoved(
		TagType		tag_type,
		Tag			tag,
		Taggable	tagged )
	{
		int tt = tag_type.getTagType();

			// as above
		
		if ( tt == TagType.TT_DOWNLOAD_MANUAL ){

			synchronized( lifecycle_handlers ){
				
				long type = tagged.getTaggableType();
				
				LifecycleHandlerImpl handler = lifecycle_handlers.get( type );
				
				if ( handler == null ){
					
					handler = new LifecycleHandlerImpl();
					
					lifecycle_handlers.put( type, handler );
				}
				
				handler.taggableUntagged( tag_type, tag, tagged );
			}
		}
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
	
	public List<Tag>
	getTagsForTaggable(
		int			tts,
		Taggable	taggable )
	{
		Set<Tag>	result = new HashSet<Tag>();
		
		for ( TagType tt: tag_types ){
			
			if ( tt.getTagType() == tts ){
			
				result.addAll( tt.getTagsForTaggable( taggable ));
			}
		}
		
		return( new ArrayList<Tag>( result ));
	}
	
	public Tag
	lookupTagByUID(
		long	tag_uid )
	{
		int	tag_type_id = (int)((tag_uid>>32)&0xffffffffL);
		
		TagType tt;
		
		synchronized( tag_type_map ){
			
			tt = tag_type_map.get( tag_type_id );
		}
				
		if ( tt != null ){
				
			int	tag_id = (int)(tag_uid&0xffffffffL);
			
			return( tt.getTag( tag_id ));
		}
		
		return( null );
	}
	
	public TaggableLifecycleHandler
	registerTaggableResolver(
		TaggableResolver	resolver )
	{
		if ( !enabled ){
			
			return(
				new TaggableLifecycleHandler()
				{
					public void
					initialized(
						List<Taggable>	initial_taggables )
					{
					}
					
					public void
					taggableCreated(
						Taggable	taggable )
					{
					}
					
					public void
					taggableDestroyed(
						Taggable	taggable )
					{
					}
				});
		}
		
		LifecycleHandlerImpl handler;
		
		long type = resolver.getResolverTaggableType();
		
		synchronized( lifecycle_handlers ){
			
			handler = lifecycle_handlers.get( type );
			
			if ( handler == null ){
				
				handler = new LifecycleHandlerImpl();
				
				lifecycle_handlers.put( type, handler );
			}
			
			handler.setResolver( resolver );
		}
		
		return( handler );
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
	
	public void
	addTaggableLifecycleListener(
		long						taggable_type,
		TaggableLifecycleListener	listener )
	{
		synchronized( lifecycle_handlers ){
			
			LifecycleHandlerImpl handler = lifecycle_handlers.get( taggable_type );
			
			if ( handler == null ){
				
				handler = new LifecycleHandlerImpl();
				
				lifecycle_handlers.put( taggable_type, handler );
			}
			
			handler.addListener( listener );
		}
	}
	
	public void
	removeTaggableLifecycleListener(
		long						taggable_type,
		TaggableLifecycleListener	listener )
	{
		synchronized( lifecycle_handlers ){
			
			LifecycleHandlerImpl handler = lifecycle_handlers.get( taggable_type );
			
			if ( handler != null ){
				
				handler.removeListener( listener );
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
		if ( !enabled ){
			
			Debug.out( "TagManager is disabled" );;
			
			return( new HashMap());
		}
		
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
		if ( !enabled ){
			
			Debug.out( "TagManager is disabled" );;
		}
		
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
	
	protected boolean
	writeBooleanAttribute(
		TagTypeBase		tag_type,
		TagBase			tag,
		String			attr,
		boolean			value )
	{
		return( writeLongAttribute( tag_type, tag, attr, value?1:0 ));
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
	
	protected boolean
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
					
					return( false );
				}
				
				conf.put( attr, value );
				
				setDirty();
				
				return( true );
			}
		}catch( Throwable e ){
			
			Debug.out( e );
			
			return( false );
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
	
	protected String[]
	readStringListAttribute(
		TagTypeBase		tag_type,
		TagBase			tag,
		String			attr,
		String[]		def )
	{
		try{
			synchronized( this ){
				
				Map conf = getConf( tag_type, tag, false );
				
				if ( conf == null ){
					
					return( def );
				}
				
				List<String> vals = BDecoder.decodeStrings((List)conf.get( attr ));
				
				if ( vals == null ){
					
					return( def );
				}
				
				return( vals.toArray( new String[ vals.size()]));
			}
		}catch( Throwable e ){
			
			Debug.out( e );
			
			return( def );
		}
	}
	
	protected boolean
	writeStringListAttribute(
		TagTypeBase		tag_type,
		TagBase			tag,
		String			attr,
		String[]		value )
	{
		try{
			synchronized( this ){
				
				Map conf = getConf( tag_type, tag, true );
				
				List<String> old = BDecoder.decodeStrings((List)conf.get( attr ));
				
				if ( old == null && value == null ){
					
					return( false );
					
				}else if ( old != null && value != null ){
					
					if ( value.length == old.size()){
						
						boolean diff = false;
						
						for ( int i=0;i<value.length;i++){
							
							if ( !old.get(i).equals(value[i])){
								
								diff = true;
								
								break;
							}
						}
						
						if ( !diff ){
							
							return( false );
						}
					}
				}
				
				if ( value == null ){
				
					conf.remove( attr );
				}else{
					
					conf.put( attr, Arrays.asList( value ));
				}
				
				setDirty();
				
				return( true );
			}
		}catch( Throwable e ){
			
			Debug.out( e );
			
			return( false );
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
	
	private class
	LifecycleHandlerImpl
		implements TaggableLifecycleHandler
	{
		private TaggableResolver		resolver;
		private boolean					initialised;
		
		private CopyOnWriteList<TaggableLifecycleListener>	listeners = new CopyOnWriteList<TaggableLifecycleListener>();
		
		private
		LifecycleHandlerImpl()
		{
		}
		
		private void
		setResolver(
			TaggableResolver	_resolver )
		{
			resolver = _resolver;
		}
		
		private void
		addListener(
			final TaggableLifecycleListener	listener )
		{
			synchronized( this ){
				
				listeners.add( listener );
				
				if ( initialised ){
				
					final List<Taggable> taggables = resolver.getResolvedTaggables();
					
					if ( taggables.size() > 0 ){
												 
						async_dispatcher.dispatch(
							new AERunnable()
							{
								@Override
								public void 
								runSupport() 
								{										
									listener.initialised( taggables );
								}
							});
					}
				}
			}
		}
		
		private void
		removeListener(
			TaggableLifecycleListener	listener )
		{
			synchronized( this ){
				
				listeners.remove( listener );
			}
		}
		
		public void 
		initialized(
			final	List<Taggable>	initial_taggables )
		{				
			resolverInitialized( resolver );
			
			synchronized( this ){
				
				initialised = true;
				
				if ( listeners.size() > 0 ){
																
					final List<TaggableLifecycleListener> listeners_ref = listeners.getList();
					 
					async_dispatcher.dispatch(
						new AERunnable()
						{
							@Override
							public void 
							runSupport() 
							{
								for ( TaggableLifecycleListener listener: listeners_ref ){
									
									listener.initialised( initial_taggables );
								}
							}
						});
				}
			}
		}
		
		public void
		taggableCreated(
			final Taggable	t )
		{	
			synchronized( this ){
				
				if ( initialised ){
					
					final List<TaggableLifecycleListener> listeners_ref = listeners.getList();
					 
					async_dispatcher.dispatch(
						new AERunnable()
						{
							@Override
							public void 
							runSupport() 
							{
								for ( TaggableLifecycleListener listener: listeners_ref ){
									
									try{
										listener.taggableCreated( t );
										
									}catch( Throwable e ){
										
										Debug.out( e );
									}
								}
							}
						});
				}
			}
		}
		
		public void
		taggableDestroyed(
			final Taggable	t )
		{
			removeTaggable( resolver, t );
			
			synchronized( this ){
				
				if ( initialised ){
					
					final List<TaggableLifecycleListener> listeners_ref = listeners.getList();
					 
					async_dispatcher.dispatch(
						new AERunnable()
						{
							@Override
							public void 
							runSupport() 
							{
								for ( TaggableLifecycleListener listener: listeners_ref ){
									
									try{
										listener.taggableDestroyed( t );
										
									}catch( Throwable e ){
										
										Debug.out( e );
									}
								}
							}
						});
				}
			}
		}
		
		public void
		taggableTagged(
			final TagType	tag_type,
			final Tag		tag,
			final Taggable	taggable )
		{
			synchronized( this ){
				
				if ( initialised ){
					
					final List<TaggableLifecycleListener> listeners_ref = listeners.getList();
					 
					async_dispatcher.dispatch(
						new AERunnable()
						{
							@Override
							public void 
							runSupport() 
							{
								for ( TaggableLifecycleListener listener: listeners_ref ){
									
									try{
										listener.taggableTagged( tag_type, tag, taggable);
										
									}catch( Throwable e ){
										
										Debug.out( e );
									}
								}
							}
						});
				}
			}
		}
		
		public void
		taggableUntagged(
			final TagType	tag_type,
			final Tag		tag,
			final Taggable	taggable )
		{
			synchronized( this ){
				
				if ( initialised ){
					
					final List<TaggableLifecycleListener> listeners_ref = listeners.getList();
					 
					async_dispatcher.dispatch(
						new AERunnable()
						{
							@Override
							public void 
							runSupport() 
							{
								for ( TaggableLifecycleListener listener: listeners_ref ){
									
									try{
										listener.taggableUntagged( tag_type, tag, taggable );
										
									}catch( Throwable e ){
										
										Debug.out( e );
									}
								}
							}
						});
				}
			}
		}
	}
}
