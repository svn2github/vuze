/*
 * File    : ManagerUtils.java
 * Created : 7 d�c. 2003}
 * By      : Olivier
 *
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.gudy.azureus2.ui.swt.views.utils;


import java.io.*;
import java.net.InetAddress;
import java.net.URL;
import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfoSet;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.download.impl.DownloadManagerAdapter;
import org.gudy.azureus2.core3.global.GlobalManagerDownloadRemovalVetoException;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.tracker.host.TRHostException;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.xml.util.XUXmlWriter;
import org.gudy.azureus2.platform.PlatformManager;
import org.gudy.azureus2.platform.PlatformManagerCapabilities;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.UnloadablePlugin;
import org.gudy.azureus2.plugins.disk.DiskManagerChannel;
import org.gudy.azureus2.plugins.disk.DiskManagerEvent;
import org.gudy.azureus2.plugins.disk.DiskManagerListener;
import org.gudy.azureus2.plugins.disk.DiskManagerRequest;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadStub;
import org.gudy.azureus2.plugins.platform.PlatformManagerException;
import org.gudy.azureus2.plugins.sharing.*;
import org.gudy.azureus2.plugins.tracker.Tracker;
import org.gudy.azureus2.plugins.tracker.TrackerTorrent;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageRequest;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageResponse;
import org.gudy.azureus2.plugins.utils.PooledByteBuffer;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.pluginsimpl.local.utils.FormattersImpl;
import org.gudy.azureus2.ui.swt.TorrentUtil;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.shells.CoreWaiterSWT;
import org.gudy.azureus2.ui.swt.shells.CoreWaiterSWT.TriggerInThread;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.webplugin.WebPlugin;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.tag.Tag;
import com.aelitis.azureus.core.tag.TagManager;
import com.aelitis.azureus.core.tag.TagManagerFactory;
import com.aelitis.azureus.core.tag.TagType;
import com.aelitis.azureus.core.util.AZ3Functions;
import com.aelitis.azureus.core.util.HTTPUtils;
import com.aelitis.azureus.core.util.LaunchManager;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;

/**
 * @author Olivier
 *
 */
public class ManagerUtils {
	
	private static RunDownloadManager run;

	public static interface RunDownloadManager {
		public void run(DownloadManager dm);
	}
	
	public static void setRunRunnable(RunDownloadManager run) {
		ManagerUtils.run = run;
	}
  
  public static void run( final DownloadManager dm) {
	if(dm != null) {
		LaunchManager	launch_manager = LaunchManager.getManager();
		
		LaunchManager.LaunchTarget target = launch_manager.createTarget( dm );
		
		launch_manager.launchRequest(
			target,
			new LaunchManager.LaunchAction()
			{
				public void
				actionAllowed()
				{
					AZ3Functions.provider prov = AZ3Functions.getProvider();
					
					if ( prov != null ){
						
						prov.setOpened( dm, true );
					}
					
					Utils.execSWTThread(
						new Runnable()
						{
							public void
							run()
							{
							   	if (run != null) {
						    		run.run(dm);
						    	} else {
						    		Utils.launch(dm.getSaveLocation().toString());
						    	}
							}
						});
				}
				
				public void
				actionDenied(
					Throwable			reason )
				{
					Debug.out( "Launch request denied", reason );
				}
			});
	}
  }
  
 /**
  * Opens the parent folder of dm's path
  * @param dm DownloadManager instance
  */
	public static void open(DownloadManager dm) {open(dm, false);}
	
	public static void open(final DownloadManager dm, final boolean open_containing_folder_mode) {

		if ( dm != null ){
			
			LaunchManager	launch_manager = LaunchManager.getManager();
			
			LaunchManager.LaunchTarget target = launch_manager.createTarget( dm );
			
			launch_manager.launchRequest(
				target,
				new LaunchManager.LaunchAction()
				{
					public void
					actionAllowed()
					{
						Utils.execSWTThread(
							new Runnable()
							{
								public void
								run()
								{
									open( dm.getSaveLocation(), open_containing_folder_mode );
								}
							});
					}
					
					public void
					actionDenied(
						Throwable			reason )
					{
						Debug.out( "Launch request denied", reason );
					}
				});
		}		
	}

	public static void 
	open(
		final DiskManagerFileInfo		file,
		final boolean					open_containing_folder_mode )
	{
		if ( file != null ){
			
			LaunchManager	launch_manager = LaunchManager.getManager();
			
			LaunchManager.LaunchTarget target = launch_manager.createTarget( file );
			
			launch_manager.launchRequest(
				target,
				new LaunchManager.LaunchAction()
				{
					public void
					actionAllowed()
					{
						Utils.execSWTThread(
							new Runnable()
							{
								public void
								run()
								{
									File this_file = file.getFile(true);
									
									File parent_file = (open_containing_folder_mode) ? this_file.getParentFile() : null;
									
									open((parent_file == null) ? this_file : parent_file);		
								}
							});
					}
					
					public void
					actionDenied(
						Throwable			reason )
					{
						Debug.out( "Launch request denied", reason );
					}
				});
		}
	}
	
	
	public static void open(File f, boolean open_containing_folder_mode) {
		if (open_containing_folder_mode) {
			Utils.launch(f.getParent());
		}
		else {
			open(f);
		}
	}
	
	public static void open(File f) {
		while (f != null && !f.exists())
			f = f.getParentFile();

		if (f == null)
			return;

		PlatformManager mgr = PlatformManagerFactory.getPlatformManager();

		if (mgr.hasCapability(PlatformManagerCapabilities.ShowFileInBrowser)) {
			try {
				PlatformManagerFactory.getPlatformManager().showFile(f.toString());
				return;
			} catch (PlatformManagerException e) {
				Debug.printStackTrace(e);
			}
		}

		if (f.isDirectory()) {
			Utils.launch(f.toString()); // default launcher
		} else {
			Utils.launch(f.getParent().toString());
		}
	}
  
	private static boolean
	getBrowseAnon(
		DownloadManager		dm )
	{
		boolean	anon = COConfigurationManager.getBooleanParameter( "Library.LaunchWebsiteInBrowserAnon" );
		
		if ( !anon ){
		
			boolean	found_pub = false;
			
			String[] nets = dm.getDownloadState().getNetworks();
			
			for ( String net: nets ){
				
				if ( net == AENetworkClassifier.AT_PUBLIC ){
					
					found_pub = true;
					
					break;
				}
			}
			
			if ( nets.length > 0 && !found_pub ){
				
				anon = true;
			}
		}
		
		return( anon );
	}
	
	private static DiskManagerFileInfo
	getBrowseHomePage(
		DownloadManager		dm )
	{
		try{
			DiskManagerFileInfo[] files = dm.getDiskManagerFileInfoSet().getFiles();
										
			for ( DiskManagerFileInfo file: files ){
				
				if ( file.getTorrentFile().getPathComponents().length == 1 ){
					
					String name = file.getTorrentFile().getRelativePath().toLowerCase( Locale.US );
					
					if ( name.equals( "index.html" ) || name.equals( "index.htm" )){
						
						return( file );
					}
				}
			}
		}catch( Throwable e ){
			
			Debug.out( e );
		}
		
		return( null );	
	}
	
	public static boolean
	browseWebsite(
		DiskManagerFileInfo		file )
	{
		try{
			String name = file.getTorrentFile().getRelativePath().toLowerCase( Locale.US );
			
			if ( name.equals( "index.html" ) || name.equals( "index.htm" )){
				
				ManagerUtils.browse( file );
				
				return( true );
			}
		}catch( Throwable e ){
			
			Debug.out( e );
		}
		
		return( false );
	}
	
	public static boolean
	browseWebsite(
		DownloadManager		dm )
	{
		DiskManagerFileInfo file = getBrowseHomePage( dm );
		
		if ( file != null ){
						
			ManagerUtils.browse( file );
			
			return( true );
		}
	
		return( false );
	}
	
	public static String 
	browse(
		DiskManagerFileInfo 	file )
	{
		boolean anon = getBrowseAnon( file.getDownloadManager());
		
		return( browse( file, anon, true ));
	}
	
	public static String 
	browse(
		DiskManagerFileInfo 	file,
		boolean					anon,
		boolean					launch )
	{
		return( browse( file.getDownloadManager(), file, anon, launch ));
	}
	
	public static String 
	browse(
		DownloadManager 	dm )
	{
		boolean anon = getBrowseAnon( dm );
		
		return( browse( dm, null, anon, true ));
	}
	
	public static String 
	browse(
		DownloadManager 	dm,
		boolean				anon,
		boolean				launch )
	{
		return( browse( dm, null, anon, launch ));
	}
	
	private static Map<DownloadManager,WebPlugin>	browse_plugins = new IdentityHashMap<DownloadManager, WebPlugin>();
	
	public static String 
	browse(
		final DownloadManager 			dm,
		DiskManagerFileInfo				_file,
		final boolean					anon,
		final boolean					launch )
	{
		Properties	props = new Properties();
		
		File	save_location = dm.getSaveLocation();
		
		final String	root_dir;
		
		if ( save_location.isFile()){
			
			root_dir = save_location.getParentFile().getAbsolutePath();
			
		}else{
		
			root_dir = save_location.getAbsolutePath();
		}
		
		final String	url_suffix;
		
		boolean	always_browse = COConfigurationManager.getBooleanParameter( "Library.LaunchWebsiteInBrowserDirList" );
		
		if ( !always_browse ){
			
			if ( _file == null ){
				
				_file = getBrowseHomePage( dm );
			}
		}
		
		final DiskManagerFileInfo file = _file;
		
		if ( file == null ){
				
				// asked to launch a download (note that the double-click on a download that has an index.html file will by default result in
				// us getting here with the file set, not null)
			
			url_suffix = "";
				
		}else{
				
			String relative_path = file.getTorrentFile().getRelativePath();
								
			String[] bits = relative_path.replace( File.separatorChar, '/' ).split( "/" );
				
			String _url_suffix = "";
				
			int	bits_to_use = always_browse?bits.length-1:bits.length;
			
			for ( int i=0;i<bits_to_use;i++){
				
				String bit = bits[i];
				
				if ( bit.length() == 0 ){
					
					continue;
				}
				
				_url_suffix += (_url_suffix==""?"":"/") + UrlUtils.encode( bit );
			}
			
			url_suffix = _url_suffix;
		}
		
		synchronized( browse_plugins ){
			
			WebPlugin	plugin = browse_plugins.get( dm );
		
			if ( plugin == null ){
			
				props.put( WebPlugin.PR_PORT, 0 );
				props.put( WebPlugin.PR_BIND_IP, "127.0.0.1" );
				props.put( WebPlugin.PR_HOME_PAGE, "" );
				props.put( WebPlugin.PR_ROOT_DIR, root_dir );
				props.put( WebPlugin.PR_ACCESS, "local" );
				props.put( WebPlugin.PR_HIDE_RESOURCE_CONFIG, true );
				
				props.put( WebPlugin.PR_ENABLE_KEEP_ALIVE, true );
				props.put( WebPlugin.PR_ENABLE_PAIRING, false );
				props.put( WebPlugin.PR_ENABLE_UPNP, false );
				props.put( WebPlugin.PR_ENABLE_I2P, false );
				props.put( WebPlugin.PR_ENABLE_TOR, false );
				
				final String plugin_id 		= "webserver:" + dm.getInternalName();
				final String plugin_name	= "Web Server for " + dm.getDisplayName();
				
				Properties messages = new Properties();
				
				messages.put( "plugins." + plugin_id, plugin_name );;
		
				PluginInitializer.getDefaultInterface().getUtilities().getLocaleUtilities().integrateLocalisedMessageBundle( messages );
				
				final AESemaphore 	waiter 		= new AESemaphore( "waiter" );
				final String[]		url_holder	= { null };
				
				plugin = 
					new UnloadableWebPlugin( props )
					{
						private Map<String,Object>	file_map = new HashMap<String,Object>();
						
						private String	host;
						
						@Override
						public void 
						initialize(
							PluginInterface plugin_interface )
								throws PluginException 
						{
							DiskManagerFileInfoSet file_set = dm.getDiskManagerFileInfoSet();
							
							DiskManagerFileInfo[] files = file_set.getFiles();
							
							Set<Object>	root_dir = new HashSet<Object>();
							
							file_map.put( "", root_dir );

							for ( DiskManagerFileInfo dm_file: files ){
												
								TOTorrentFile file = dm_file.getTorrentFile();
								
								String	path = file.getRelativePath();
								
								file_map.put( path, dm_file );
								
								if ( path.startsWith( File.separator )){
									
									path = path.substring( 1 );
								}
								
								Set<Object>	dir = root_dir;
								
								int	pos = 0;
																
								while( true ){
									
									int next_pos = path.indexOf( File.separatorChar, pos );
									
									if ( next_pos == -1 ){
																				
										dir.add( dm_file );
										
										break;
										
									}else{
										
										String bit = path.substring( pos, next_pos );
										
										dir.add( bit );
										
										String sub_path = path.substring( 0, next_pos );
										
										dir = (Set<Object>)file_map.get( sub_path );
										
										if ( dir == null ){
											
											dir = new HashSet<Object>();
											
											file_map.put( sub_path, dir );
										}
										
										pos = next_pos + 1;
									}
								}
							}
							
							Properties props = plugin_interface.getPluginProperties();
							
							props.put( "plugin.name", plugin_name );
							
							super.initialize( plugin_interface );
							
							InetAddress	bind_ip = getServerBindIP();
														
							if ( bind_ip.isAnyLocalAddress()){
								
								host = "127.0.0.1";
								
							}else{
								
								host = bind_ip.getHostAddress();
							}
							
							int port = getServerPort();
							
							log( "Assigned port: " + port );
							
							String protocol = getProtocol();

							String url = protocol + "://" + host + ":" + port + "/" + url_suffix;
							
							if ( launch ){
							
								Utils.launch( url, false, true, anon );
								
							}else{
								
								synchronized( url_holder ){
									
									url_holder[0] = url;
								}
								
								waiter.release();
							}
						}
						
						@Override
						public boolean
						generate(
							TrackerWebPageRequest		request,
							TrackerWebPageResponse		response )
						
							throws IOException
						{
							try{
								boolean res = super.generate(request, response);
								
								if ( !res ){
									
									response.setReplyStatus( 404 );
								}
							}catch( Throwable e ){
								
								response.setReplyStatus( 404 );
							}
							
							return( true );
						}
						
						@Override
						protected boolean
						useFile(
							TrackerWebPageRequest				request,
							final TrackerWebPageResponse		response,
							String								root,
							String								relative_url )
							
							throws IOException
						{
							URL absolute_url = request.getAbsoluteURL();
							
							String path = absolute_url.getPath();
							
							if ( path.equals( "/" )){
								
								if ( COConfigurationManager.getBooleanParameter( "Library.LaunchWebsiteInBrowserDirList" )){

									relative_url = "/";
								}
							}
							
							String download_name = XUXmlWriter.escapeXML( dm.getDisplayName());
							
							String relative_file = relative_url.replace( '/', File.separatorChar );
							
							String	node_key = relative_file.substring( 1 );
							
							Object	file_node = file_map.get( node_key );
							
							boolean	file_node_is_parent = false;
							
							if ( file_node == null ){
								
								int pos = node_key.lastIndexOf( File.separator );
								
								if ( pos == -1 ){
									
									node_key = "";
									
								}else{
									
									node_key = node_key.substring( 0, pos );
								}
								
								file_node = file_map.get( node_key );
								
								file_node_is_parent = true;
							}
									
							if ( file_node == null){
		
								return( false );
							}
									
							if ( file_node instanceof Set ){
									
								Set<Object>		kids = (Set<Object>)file_node;
								
								String request_url = request.getURL();
								
								if ( file_node_is_parent ){
									
									int	pos = request_url.lastIndexOf( "/" );
									
									if ( pos == -1 ){
										
										request_url = "";
										
									}else{
										
										request_url = request_url.substring( 0, pos );
									}
								}
																
								response.setContentType( "text/html" );
								
								OutputStream os = response.getOutputStream();
																
								String NL = "\r\n";
								
								String title = XUXmlWriter.escapeXML( UrlUtils.decode( request_url ));
								
								if ( title.length() == 0 ){
									
									title = "/";
								}
								
								os.write((
									"<html>" + NL +
									" <head>" + NL +
									" <meta charset=\"UTF-8\">" + NL + 
									"  <title>" + download_name + ": Index of " + title + "</title>" + NL +
									" </head>" + NL +
									" <body>" + NL +
									"  <p>" + download_name + "</p>" + NL + 
									"  <h1>Index of " + title + "</h1>" + NL +
									"  <pre><hr>" + NL ).getBytes( "UTF-8" ));
								
								String root_url = request_url;
								
								if ( !root_url.endsWith( "/" )){
									
									root_url += "/";
								}
								
								if ( request_url.length() > 1 ){
									
									int	pos = request_url.lastIndexOf( '/' );
									
									if ( pos == 0 ){
										
										pos++;
									}
									
									String parent = request_url.substring( 0, pos );
									
									os.write(( "<a href=\"" + parent + "\">..</a>" + NL).getBytes( "UTF-8" ));
								}
							
								List<String[]>	filenames		= new ArrayList<String[]>( kids.size());
								int				max_filename	= 0;
								
								int MAX_LEN = 120;
								
								for ( Object	entry: kids ){
									
									DiskManagerFileInfo		file;
									String					file_name;
									
									if ( entry instanceof String ){
										
										file = null;
										
										file_name	= (String)entry;
										
									}else{
										
										file = (DiskManagerFileInfo)entry;
										
										if ( file.isSkipped()){
											
											continue;
										}
										
										file_name 	= file.getTorrentFile().getRelativePath();
										
										int pos = file_name.lastIndexOf( File.separatorChar );
										
										if ( pos != -1 ){
											
											file_name = file_name.substring( pos+1 );
										}
									}
																		
									String url			= root_url + UrlUtils.encode( file_name );
											
									if ( file == null ){
										
										file_name += "/";
									}
									
									int len = file_name.length();
									
									if ( len > MAX_LEN ){
										
										file_name = file_name.substring( 0, MAX_LEN-3 ) + "...";
										
										len = file_name.length();
									}
									
									if ( len > max_filename ){
										
										max_filename = len;
									}
									
									filenames.add( new String[]{ url, file_name, file==null?"":DisplayFormatters.formatByteCountToKiBEtc( file.getLength())});
								}
								
								max_filename = ((max_filename + 15 )/8)*8;
								
								char[]	padding = new char[max_filename];
								
								Arrays.fill( padding, ' ' );
								
								Collections.sort(
									filenames,
									new Comparator<String[]>()
									{
										Comparator comp = new FormattersImpl().getAlphanumericComparator(true);
										
										public int 
										compare(
											String[] o1,
											String[] o2) 
										{
											return( comp.compare(o1[0], o2[0] ));
										}
									});
								
								for ( String[] entry: filenames ){
									
									String file_name = entry[1];
								
									int	len = file_name.length();
																		
									StringBuffer line = new StringBuffer( max_filename + 64 );
									
									line.append( "<a href=\"" + entry[0] + "\">" + XUXmlWriter.escapeXML( file_name ) + "</a>" );
									
									line.append( padding, 0, max_filename - len );
									
									line.append(  entry[2] );
									
									line.append( NL );
										
									os.write( line.toString().getBytes( "UTF-8" ));
								}
								
								os.write((
								"  <hr></pre>" + NL +
								"  <address>Vuze Web Server at " + host + " Port " + getServerPort() +"</address>" + NL +
								" </body>" + NL +
								"</html>" ).getBytes( "UTF-8" ));
								
								return( true );
								
							}else{
							
								DiskManagerFileInfo dm_file = (DiskManagerFileInfo)file_node;
								
								long	file_size = dm_file.getLength();
								
								File target_file = dm_file.getFile( true );
								
								boolean done = 	dm_file.getDownloaded() == file_size && 
												target_file.length() == file_size;
	
									// for big files see if we can hand off all processing to the
									// media server
								
								if ( file_size >= 512*1024 ){
									
									URL stream_url = getMediaServerContentURL( dm_file );

									if ( stream_url != null ){
										
										OutputStream os = response.getRawOutputStream();
										
										os.write((
											"HTTP/1.1 302 Found" + NL +
											"Location: " + stream_url.toExternalForm() + NL +
											NL ).getBytes( "UTF-8" ));
										
										return( true );
									}
								}
								
								String	file_type;
								
									// Use the original torrent file name when deducing file type to 
									// avoid incomplete suffix issues etc
								
								String relative_path = dm_file.getTorrentFile().getRelativePath();
								
								int	pos = relative_path.lastIndexOf( "." );
		
								if ( pos == -1 ){
		
									file_type = "";
									
								}else{
		
									file_type = relative_path.substring(pos+1);
								}
								
								if ( done ){
									
									if ( file_size < 512*1024 ){
									

										
										FileInputStream	fis = null;
				
										try{
											fis = new FileInputStream(target_file);
				
											response.useStream( file_type, fis );
				
											return( true );
				
										}finally{
				
											if ( fis != null ){
				
												fis.close();
											}
										}
									}else{
											
										
										OutputStream 	os 	= null;
										InputStream 	is	= null;
										
										try{
											os = response.getRawOutputStream();
											
											byte[] buffer = new byte[128*1024];
											
											is = new FileInputStream( target_file );
											
											while( true ){
												
												int len = is.read( buffer );
												
												if ( len <= 0 ){
													
													break;
												}
												
												os.write( buffer, 0, len );
											}
										}catch( Throwable e ){
											
											//e.printStackTrace();
											
										}finally{
											
											try{
												os.close();
												
											}catch( Throwable e ){
											}
											
											try{
												is.close();
												
											}catch( Throwable e ){
											}
											
				
										}
								
										return( true );
									}
							
								}else{
									
									dm_file.setPriority(10);
									
									try{
										final OutputStream os = response.getRawOutputStream();
										
										os.write((
												"HTTP/1.1 200 OK" + NL +
												"Content-Type:" + HTTPUtils.guessContentTypeFromFileType( file_type ) + NL +
												"Content-Length: " + file_size + NL +
												"Connection: close" + NL +
												"X-Vuze-Hack: X" ).getBytes( "UTF-8" ));
										
										DiskManagerChannel chan = PluginCoreUtils.wrap( dm_file ).createChannel();
										
										final DiskManagerRequest req = chan.createRequest();
										
										final boolean[] header_complete = { false };
										final long[]	last_write		= { 0 };
										
										req.setOffset( 0 );
										req.setLength( file_size );
																				
										req.addListener(
											new DiskManagerListener()
											{
												public void
												eventOccurred(
													DiskManagerEvent	event )
												{
													int	type = event.getType();
													
													if ( type ==  DiskManagerEvent.EVENT_TYPE_BLOCKED ){
													
														return;
														
													}else if ( type == DiskManagerEvent.EVENT_TYPE_FAILED ){
														
														throw( new RuntimeException( event.getFailure()));
													}
													
													PooledByteBuffer buffer = event.getBuffer();
													
													if ( buffer == null ){
														
														throw( new RuntimeException( "eh?" ));
													}

													try{

														boolean	do_header = false;
														
														synchronized( header_complete ){
															
															if ( !header_complete[0] ){
																
																do_header = true;
																
																header_complete[0] = true;
															}
															
															last_write[0] = SystemTime.getMonotonousTime();
														}
														
														if ( do_header ){
															
															os.write((NL+NL).getBytes( "UTF-8" ));
														}
																	
														byte[] data = buffer.toByteArray();
														
														os.write( data );
														
													}catch( IOException e ){
														
														throw( new RuntimeException( "Failed to write to " + file, e ));
														
													}finally{
														
														buffer.returnToPool();
													}
												}
											});
									
										final TimerEventPeriodic timer_event [] = {null};
										
										timer_event[0] =
											SimpleTimer.addPeriodicEvent(
												"KeepAlive",
												10*1000,
												new TimerEventPerformer() 
												{
													boolean	cancel_outstanding = false;
													
													public void 
													perform(
														TimerEvent event) 
													{
														if ( cancel_outstanding ){
															
															req.cancel();
															
														}else{
															
															synchronized( header_complete ){
																
																if ( header_complete[0] ){
																	
																	if ( SystemTime.getMonotonousTime() - last_write[0] >= 5*60*1000 ){
																		
																		req.cancel();
																	}
																}else{
																	
																	try{
																		os.write( "X".getBytes( "UTF-8" ));
																		
																		os.flush();
																		
																	}catch( Throwable e ){
																		
																		req.cancel();
																	}
																}
															}
															
															if ( !response.isActive()){
																
																cancel_outstanding = true;
															}
														}
													}
												});
										
										try{
											req.run();
										
										}finally{
											
											timer_event[0].cancel();
										}
										
										return( true );
										
									}catch( Throwable e ){
										
										return( false );
									}
								}
							}
						}
						
						public void 
						unload() 
							throws PluginException 
						{
							synchronized( browse_plugins ){
								
								 browse_plugins.remove( dm );
							}
							
							super.unload();
						}
					};
								
				PluginManager.registerPlugin(
					plugin,
					plugin_id,
					plugin_id );
				
				browse_plugins.put( dm, plugin );
				
				if ( launch ){
					
					return( null );
					
				}else{
					
					waiter.reserve( 10*1000 );
					
					synchronized( url_holder ){
						
						return( url_holder[0] );
					}
				}
			}else{
				
				String protocol = plugin.getProtocol();
				
				InetAddress	bind_ip = plugin.getServerBindIP();

				String	host;
				
				if ( bind_ip.isAnyLocalAddress()){
					
					host = "127.0.0.1";
					
				}else{
					
					host = bind_ip.getHostAddress();
				}
				
				String url = protocol + "://" + host+ ":" + plugin.getServerPort() + "/" + url_suffix;
				
				if ( launch ){
				
					Utils.launch( url, false, true, anon );
					
					return( null );
					
				}else{
					
					return( url );
				}
			}
		}
	}

	public static URL 
	getMediaServerContentURL(
		DiskManagerFileInfo file ) 
	{
		PluginManager pm = AzureusCoreFactory.getSingleton().getPluginManager();
		
		PluginInterface pi = pm.getPluginInterfaceByID( "azupnpav", false );
	
		if ( pi == null ){
			
			return( null );
		}
	
		if ( !pi.getPluginState().isOperational()){
			
			return( null );
		}
	
		try{						
			Object	url = pi.getIPC().invoke( "getContentURL", new Object[]{ PluginCoreUtils.wrap( file )});
			
			if ( url instanceof String ){
				
				return( new URL( (String) url));
			}
		}catch ( Throwable e ){
			
			e.printStackTrace();
		}
	
		return( null );
	}
	
	private static class
	UnloadableWebPlugin
		extends WebPlugin
		implements UnloadablePlugin
	{
		private
		UnloadableWebPlugin(
			Properties		props )
		{
			super( props );
		}
		
		public void 
		unload() 
			throws PluginException 
		{
			super.unloadPlugin();
		}
	}
	
  public static boolean isStartable(DownloadManager dm) {
    if(dm == null)
      return false;
    int state = dm.getState();
    if (state != DownloadManager.STATE_STOPPED) {
      return false;
    }
    return true;
  }
  
  public static boolean isStopable(DownloadManager dm) {
    if(dm == null)
      return false;
    int state = dm.getState();
    if (	state == DownloadManager.STATE_STOPPED ||
    		state == DownloadManager.STATE_STOPPING	) {
      return false;
    }
    return true;
  }
  
  public static boolean isPauseable(DownloadManager dm) {
	    if(dm == null)
	      return false;
	    int state = dm.getState();
	    if (	state == DownloadManager.STATE_STOPPED ||
	    		state == DownloadManager.STATE_STOPPING	||
	    		state == DownloadManager.STATE_ERROR ) {
	      return false;
	    }
	    return true;
	  }
  
  public static boolean isStopped(DownloadManager dm) {
	    if(dm == null)
	      return false;
	    int state = dm.getState();
	    if (	state == DownloadManager.STATE_STOPPED ||
	    		state == DownloadManager.STATE_ERROR	) {
	      return true;
	    }
	    return false;
	  }
  
  public static boolean
  isForceStartable(
  	DownloadManager	dm )
  {
    if(dm == null){
        return false;
  	}
    
    int state = dm.getState();
    
    if (	state != DownloadManager.STATE_STOPPED && state != DownloadManager.STATE_QUEUED &&
            state != DownloadManager.STATE_SEEDING && state != DownloadManager.STATE_DOWNLOADING){

    	return( false );
    }
    
    return( true );
  }
  
  /**
   * Host a DownloadManager on our Tracker.
   * <P>
   * Doesn't require SWT Thread
   */
  public static void 
  host(
  	AzureusCore		azureus_core,
	DownloadManager dm)
 {
		if (dm == null) {
			return;
		}

		TOTorrent torrent = dm.getTorrent();
		if (torrent == null) {
			return;
		}

		try {
			azureus_core.getTrackerHost().hostTorrent(torrent, true, false);
		} catch (TRHostException e) {
			MessageBoxShell mb = new MessageBoxShell(
					SWT.ICON_ERROR | SWT.OK,
					MessageText.getString("MyTorrentsView.menu.host.error.title"),
					MessageText.getString("MyTorrentsView.menu.host.error.message").concat(
							"\n").concat(e.toString()));
			mb.open(null);
		}
	}
  
  /**
   * Publish a DownloadManager on our Tracker.
   * <P>
   * Doesn't require SWT Thread
   */
  public static void 
  publish(
  		AzureusCore		azureus_core,
		DownloadManager dm)
 {
		if (dm == null) {
			return;
		}

		TOTorrent torrent = dm.getTorrent();
		if (torrent == null) {
			return;
		}

		try {
			azureus_core.getTrackerHost().publishTorrent(torrent);
		} catch (TRHostException e) {
			MessageBoxShell mb = new MessageBoxShell(
					SWT.ICON_ERROR | SWT.OK,
					MessageText.getString("MyTorrentsView.menu.host.error.title"),
					MessageText.getString("MyTorrentsView.menu.host.error.message").concat(
							"\n").concat(e.toString()));
			mb.open(null);
		}
	}
  
  
  public static void 
  start(
  		DownloadManager dm) 
  {
    if (dm != null && dm.getState() == DownloadManager.STATE_STOPPED) {
    	
      dm.setStateWaiting();
    }
  }

  public static void 
  queue(
  		DownloadManager dm,
		Composite panelNotUsed) 
  {
    if (dm != null) {
    	if (dm.getState() == DownloadManager.STATE_STOPPED){
    		
    		dm.setStateQueued();
    		
    		/* parg - removed this - why would we want to effectively stop + restart
    		 * torrents that are running? This is what happens if the code is left in.
    		 * e.g. select two torrents, one stopped and one downloading, then hit "queue"
    		 
    		 }else if (	dm.getState() == DownloadManager.STATE_DOWNLOADING || 
    				dm.getState() == DownloadManager.STATE_SEEDING) {
    		
    			stop(dm,panel,DownloadManager.STATE_QUEUED);
    		*/
      }
    }
  }
  
  public static void pause(DownloadManager dm, Shell shell) {
		if (dm == null) {
			return;
		}

		int state = dm.getState();

		if (state == DownloadManager.STATE_STOPPED
				|| state == DownloadManager.STATE_STOPPING ){
			return;
		}
		
		asyncPause(dm);
  }
  
  public static void stop(DownloadManager dm, Shell shell) {
  	stop(dm, shell, DownloadManager.STATE_STOPPED);
  }
  
	public static void stop(final DownloadManager dm, final Shell shell,
			final int stateAfterStopped) {
		if (dm == null) {
			return;
		}

		int state = dm.getState();

		if (state == DownloadManager.STATE_STOPPED
				|| state == DownloadManager.STATE_STOPPING
				|| state == stateAfterStopped) {
			return;
		}

		boolean stopme = true;
		if (state == DownloadManager.STATE_SEEDING) {

			if (dm.getStats().getShareRatio() >= 0
					&& dm.getStats().getShareRatio() < 1000
					&& COConfigurationManager.getBooleanParameter("Alert on close", false)) {
				if (!Utils.isThisThreadSWT()) {
					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {
							stop(dm, shell, stateAfterStopped);
						}
					});
					return;
				}
				Shell aShell = shell == null ? Utils.findAnyShell() : shell;
				MessageBox mb = new MessageBox(aShell, SWT.ICON_WARNING
						| SWT.YES | SWT.NO);
				mb.setText(MessageText.getString("seedmore.title"));
				mb.setMessage(MessageText.getString("seedmore.shareratio")
						+ (dm.getStats().getShareRatio() / 10) + "%.\n"
						+ MessageText.getString("seedmore.uploadmore"));
				int action = mb.open();
				stopme = action == SWT.YES;
			}
		}
		
		if (stopme) {
			asyncStop(dm, stateAfterStopped);
		}
	}

  /**
   * @deprecated Use {@link TorrentUtil#removeDownloads(DownloadManager[], AERunnable)}
   */
  public static void remove(final DownloadManager dm, Shell unused_shell,
			final boolean bDeleteTorrent, final boolean bDeleteData) {
  	remove(dm, unused_shell, bDeleteTorrent, bDeleteData, null);
	}
  
  /**
   * @deprecated Use {@link TorrentUtil#removeDownloads(DownloadManager[], AERunnable)}
   */
  public static void remove(final DownloadManager dm, Shell unused_shell,
			final boolean bDeleteTorrent, final boolean bDeleteData,
			final AERunnable deleteFailed) {
  	TorrentUtil.removeDownloads(new DownloadManager[] { dm }, null);
  	Debug.out("ManagerUtils.remove is Deprecated.  Use TorrentUtil.removeDownloads");
	}
  
  private static AsyncDispatcher async = new AsyncDispatcher(2000);
  
  public static void asyncStopDelete(final DownloadManager dm,
			final int stateAfterStopped, final boolean bDeleteTorrent,
			final boolean bDeleteData, final AERunnable deleteFailed) {

	  
	async.dispatch(new AERunnable() {
			public void runSupport() {

				try {
					// I would move the FLAG_DO_NOT_DELETE_DATA_ON_REMOVE even deeper
					// but I fear what could possibly go wrong.
					boolean reallyDeleteData = bDeleteData
							&& !dm.getDownloadState().getFlag(
									Download.FLAG_DO_NOT_DELETE_DATA_ON_REMOVE);

					dm.getGlobalManager().removeDownloadManager(dm, bDeleteTorrent,
							reallyDeleteData);
				} catch (GlobalManagerDownloadRemovalVetoException f) {
					
						// see if we can delete a corresponding share as users frequently share
						// stuff by mistake and then don't understand how to delete the share
						// properly
					
					try{
						PluginInterface pi = AzureusCoreFactory.getSingleton().getPluginManager().getDefaultPluginInterface();
						
						ShareManager sm = pi.getShareManager();
						
						Tracker	tracker = pi.getTracker();
						
						ShareResource[] shares = sm.getShares();
						
						TOTorrent torrent = dm.getTorrent();
						
						byte[] target_hash = torrent.getHash();
						
						for ( ShareResource share: shares ){
							
							int type = share.getType();
							
							byte[] hash;
							
							if ( type == ShareResource.ST_DIR ){
								
								hash = ((ShareResourceDir)share).getItem().getTorrent().getHash();
								
							}else if ( type == ShareResource.ST_FILE ){
								
								hash = ((ShareResourceFile)share).getItem().getTorrent().getHash();
								
							}else{
								
								hash = null;
							}
							
							if ( hash != null ){
								
								if ( Arrays.equals( target_hash, hash )){
									
									try{
										dm.stopIt( DownloadManager.STATE_STOPPED, false, false );
										
									}catch( Throwable e ){
									}
									
									
									try{
						        		TrackerTorrent	tracker_torrent = tracker.getTorrent( PluginCoreUtils.wrap( torrent ));

						        		if ( tracker_torrent != null ){
						        			
						        			tracker_torrent.stop();
						        		}
									}catch( Throwable e ){
									}
									
									share.delete();
									
									return;
								}
							}
						}
						
					}catch( Throwable e ){
						
					}
					
					if (!f.isSilent()) {
						UIFunctionsManager.getUIFunctions().forceNotify(
							UIFunctions.STATUSICON_WARNING, 
							MessageText.getString( "globalmanager.download.remove.veto" ), 
							f.getMessage(), null, null, -1 );
						
						//Logger.log(new LogAlert(dm, false,
						//		"{globalmanager.download.remove.veto}", f));
					}
					if (deleteFailed != null) {
						deleteFailed.runSupport();
					}
				} catch (Exception ex) {
					Debug.printStackTrace(ex);
					if (deleteFailed != null) {
						deleteFailed.runSupport();
					}
				}
			}
		});
	}
  
  	public static void
	asyncStop(
		final DownloadManager	dm,
		final int 				stateAfterStopped )
  	{
    	async.dispatch(new AERunnable() {
    		public void
			runSupport()
    		{
    			dm.stopIt( stateAfterStopped, false, false );
    		}
		});
  	}

 	public static void
	asyncPause(
		final DownloadManager	dm )
  	{
    	async.dispatch(new AERunnable() {
    		public void
			runSupport()
    		{
    			dm.pause();
    		}
		});
  	}
 	
	public static void asyncStartAll() {
		CoreWaiterSWT.waitForCore(TriggerInThread.NEW_THREAD,
				new AzureusCoreRunningListener() {
					public void azureusCoreRunning(AzureusCore core) {
						core.getGlobalManager().startAllDownloads();
					}
				});
	}

	public static void asyncStopAll() {
		CoreWaiterSWT.waitForCore(TriggerInThread.NEW_THREAD,
				new AzureusCoreRunningListener() {
					public void azureusCoreRunning(AzureusCore core) {
						core.getGlobalManager().stopAllDownloads();
					}
				});
	}

	public static void asyncPause() {
		CoreWaiterSWT.waitForCore(TriggerInThread.NEW_THREAD,
				new AzureusCoreRunningListener() {
					public void azureusCoreRunning(AzureusCore core) {
						core.getGlobalManager().pauseDownloads();
					}
				});
	}

	public static void asyncPauseForPeriod( final int seconds ) {
		CoreWaiterSWT.waitForCore(TriggerInThread.NEW_THREAD,
				new AzureusCoreRunningListener() {
					public void azureusCoreRunning(AzureusCore core) {
						core.getGlobalManager().pauseDownloadsForPeriod(seconds);
					}
				});
	}
	public static void asyncResume() {
		CoreWaiterSWT.waitForCore(TriggerInThread.NEW_THREAD,
				new AzureusCoreRunningListener() {
					public void azureusCoreRunning(AzureusCore core) {
						core.getGlobalManager().resumeDownloads();
					}
				});
	}
	
	public static void 
	asyncPauseForPeriod(
		final List<DownloadManager>		dms,
		final int 						seconds ) 
	{
		CoreWaiterSWT.waitForCore(TriggerInThread.NEW_THREAD,
				new AzureusCoreRunningListener() {
					public void azureusCoreRunning(AzureusCore core) 
					{
						
						final List<DownloadManager>		paused = new ArrayList<DownloadManager>();
						
						final DownloadManagerListener listener = 
							new DownloadManagerAdapter()
							{
								public void 
								stateChanged(
									DownloadManager 	manager, 
									int 				state )
								{
									synchronized( paused ){
									
										if ( !paused.remove( manager )){
											
											return;
										}
									}
									
									manager.removeListener( this );
								}
							};
						
						for ( DownloadManager dm: dms ){
							
							if ( !isPauseable( dm )){
								
								continue;
							}
							
							if ( dm.pause()){
							
								synchronized( paused ){
									
									paused.add( dm );
								}
								
								dm.addListener( listener, false );
							}
						}
						
						if ( paused.size() > 0 ){
							
							SimpleTimer.addEvent(
								"ManagerUtils.resumer",
								SystemTime.getOffsetTime( seconds*1000),
								new TimerEventPerformer()
								{	
									public void 
									perform(
										TimerEvent event ) 
									{
										List<DownloadManager>	to_resume = new ArrayList<DownloadManager>();
										
										synchronized( paused ){
											
											to_resume.addAll( paused );
											
											paused.clear();
										}
										
										for ( DownloadManager dm: to_resume ){
											
											dm.removeListener( listener );
											
											try{
												dm.resume();
												
											}catch( Throwable e ){
												
												Debug.out( e );
											}
										}
									}
								});
						}
					}
				});
	}
	
	public static class
	ArchiveCallback
	{
		public void
		success(
			DownloadStub		source,
			DownloadStub		target )
		{
		}
		
		public void
		failed(
			DownloadStub		original,
			Throwable			error )
		{
		}
		
		public void
		completed()
		{
		}
	}
	
	
	public static void
	moveToArchive(
		final List<Download>	downloads,
		ArchiveCallback			_run_when_complete )
	{
		final ArchiveCallback run_when_complete=_run_when_complete==null?new ArchiveCallback():_run_when_complete;

		Utils.getOffOfSWTThread(
			new AERunnable() {
				
				@Override
				public void 
				runSupport() 
				{
					try{
						String title 	= MessageText.getString( "archive.info.title" );
						String text 	= MessageText.getString( "archive.info.text" );
						
						MessageBoxShell prompter = 
							new MessageBoxShell(
								title, text, 
								new String[] { MessageText.getString("Button.ok") }, 0 );
					
						
						String remember_id = "managerutils.archive.info";
						
						prompter.setRemember( 
							remember_id, 
							true,
							MessageText.getString("MessageBoxWindow.nomoreprompting"));
										
						prompter.setAutoCloseInMS(0);
						
						prompter.open( null );
						
						prompter.waitUntilClosed();
						
						for ( Download dm: downloads ){
							
							try{
								DownloadStub stub = dm.stubbify();
								
								run_when_complete.success( dm, stub );
								
							}catch( Throwable e ){
								
								run_when_complete.failed( dm, e );
								
								Debug.out( e );
							}
						}
					}finally{
					
						run_when_complete.completed();
					}
				}
			});
	}
	
	public static void
	restoreFromArchive(
		final List<DownloadStub>		downloads,
		final boolean					start,
		ArchiveCallback					_run_when_complete )
	{
		final ArchiveCallback run_when_complete=_run_when_complete==null?new ArchiveCallback():_run_when_complete;
		
		Utils.getOffOfSWTThread(
			new AERunnable() {
				
				@Override
				public void 
				runSupport() 
				{
					try{
						Tag	tag = null;
						
						try{
							TagManager	tm = TagManagerFactory.getTagManager();
							
							TagType tt = tm.getTagType( TagType.TT_DOWNLOAD_MANUAL );
							
							String tag_name = MessageText.getString( "label.restored" );
							
							tag = tt.getTag( tag_name, true );
							
							if ( tag == null ){	
							
								tag = tt.createTag( tag_name, true );
							}
						}catch( Throwable e ){
									
							Debug.out( e );
						}
						
						for ( DownloadStub dm: downloads ){
							
							try{
								Download dl = dm.destubbify();
								
								if ( dl != null ){
																		
									run_when_complete.success( dm, dl );
									
									if ( tag != null ){
									
										tag.addTaggable(PluginCoreUtils.unwrap( dl ));
									}
									
									if ( start ){
										
										start( PluginCoreUtils.unwrap( dl ));
									}
								}else{
									
									run_when_complete.failed( dm, new Exception( "Unknown error" ));
								}
								
							}catch( Throwable e ){
								
								run_when_complete.failed( dm, e );
								
								Debug.out( e );
							}
						}
					}finally{
													
						run_when_complete.completed();
					}
				}
			});
	}
}
