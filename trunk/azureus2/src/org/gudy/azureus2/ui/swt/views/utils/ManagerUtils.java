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
import java.net.URLEncoder;
import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.download.impl.DownloadManagerAdapter;
import org.gudy.azureus2.core3.global.GlobalManagerDownloadRemovalVetoException;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.tracker.host.TRHostException;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.xml.util.XUXmlWriter;
import org.gudy.azureus2.platform.PlatformManager;
import org.gudy.azureus2.platform.PlatformManagerCapabilities;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.platform.PlatformManagerException;
import org.gudy.azureus2.plugins.sharing.*;
import org.gudy.azureus2.plugins.tracker.Tracker;
import org.gudy.azureus2.plugins.tracker.TrackerTorrent;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageRequest;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageResponse;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.TorrentUtil;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.shells.CoreWaiterSWT;
import org.gudy.azureus2.ui.swt.shells.CoreWaiterSWT.TriggerInThread;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.webplugin.WebPlugin;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.util.AZ3Functions;
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
  
	public static void 
	browse(
		final DiskManagerFileInfo file )
	{
		browse( file.getDownloadManager(), file );
	}
	
	public static void 
	browse(
		final DownloadManager dm )
	{
		browse( dm, null );
	}
	
	private static Map<DownloadManager,WebPlugin>	browse_plugins = new HashMap<DownloadManager, WebPlugin>();
	
	public static void 
	browse(
		final DownloadManager 			dm,
		final DiskManagerFileInfo		file )
	{
		Properties	props = new Properties();
		
		File	save_location = dm.getSaveLocation();
		
		final String	root_dir;
		final String	url_suffix;
		
		if ( save_location.isFile()){
			
			url_suffix = UrlUtils.encode( save_location.getName());
			
			root_dir = save_location.getParentFile().getAbsolutePath();
			
		}else{
			
			root_dir = save_location.getAbsolutePath();
		
			if ( file == null ){
				
				url_suffix = "";
				
			}else{
				
				File f = file.getFile( true );
				
				String str = f.getAbsolutePath();
				
				if ( str.startsWith( root_dir )){
					
					String[] bits = str.substring( root_dir.length()).replace( File.separatorChar, '/' ).split( "/" );
					
					String _url_suffix = "";
					
					for ( String bit: bits ){
						
						if ( bit.length() == 0 ){
							
							continue;
						}
						
						_url_suffix += (_url_suffix==""?"":"/") + UrlUtils.encode( bit );
					}
					
					url_suffix = _url_suffix;
					
				}else{
					
					url_suffix = "";
				}
			}
		}
		
		synchronized( browse_plugins ){
			
			WebPlugin	plugin = browse_plugins.get( dm );
		
			if ( plugin == null ){
			
				props.put( WebPlugin.PR_PORT, 0 );
				props.put( WebPlugin.PR_ROOT_DIR, root_dir );
				
				props.put( WebPlugin.PR_ENABLE_KEEP_ALIVE, true );
				props.put( WebPlugin.PR_ENABLE_PAIRING, false );
				props.put( WebPlugin.PR_ENABLE_I2P, false );
				
				final String plugin_id 		= "webserver:" + dm.getInternalName();
				final String plugin_name	= "Web Server for " + dm.getDisplayName();
				
				Properties messages = new Properties();
				
				messages.put( "plugins." + plugin_id, plugin_name );;
		
				PluginInitializer.getDefaultInterface().getUtilities().getLocaleUtilities().integrateLocalisedMessageBundle( messages );
				
				plugin = 
					new WebPlugin( props )
					{
						@Override
						public void 
						initialize(
							PluginInterface plugin_interface )
								throws PluginException 
						{
							Properties props = plugin_interface.getPluginProperties();
							
							props.put( "plugin.name", plugin_name );
							
							super.initialize( plugin_interface );
							
							int port = getServerPort();
							
							log( "Assigned port: " + port );
							
							String url = "http://127.0.0.1:" + port + "/" + url_suffix;
							
							Utils.launch( url, false, true );
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
							String	target = root_dir + relative_url.replace( '/', File.separatorChar );
		
							final File canonical_file = new File(target).getCanonicalFile();
		
								// make sure some fool isn't trying to use ../../ to escape from web dir
		
							if ( !canonical_file.toString().toLowerCase().startsWith( root_dir.toLowerCase())){
		
								return( false );
							}
									
							if ( canonical_file.isDirectory() || !canonical_file.exists()){
		
								File dir;
								
								String request_url = request.getURL();
								
								if ( canonical_file.isDirectory()){
									
									dir = canonical_file;
											
								}else{
									
									dir = canonical_file.getParentFile();
									
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
								
								String title = XUXmlWriter.escapeXML( request_url );
								
								if ( title.length() == 0 ){
									
									title = "/";
								}
								
								os.write((
								"<html>" + NL +
								" <head>" + NL +
								" <meta charset=\"UTF-8\">" + NL + 
								"  <title>Index of " + title + "</title>" + NL +
								" </head>" + NL +
								" <body>" + NL +
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

								File[] files = dir.listFiles();

								if ( files == null ){
									
									files = new File[0];
								}
								
								List<String[]>	filenames		= new ArrayList<String[]>( files.length );
								int				max_filename	= 0;
								
								int MAX_LEN = 120;
								
								for ( File file: files ){
									
									String file_name 	= file.getName();
									String url			= root_url + UrlUtils.encode( file_name );
											
									if ( file.isDirectory()){
										
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
									
									filenames.add( new String[]{ url, file_name, file.isDirectory()?"":DisplayFormatters.formatByteCountToKiBEtc( file.length())});
								}
								
								max_filename = ((max_filename + 15 )/8)*8;
								
								char[]	padding = new char[max_filename];
								
								Arrays.fill( padding, ' ' );
								
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
								"  <address>Vuze Web Server for Download '" + XUXmlWriter.escapeXML( dm.getDisplayName()) + "' at 127.0.0.1 Port " + getServerPort() +"</address>" + NL +
								" </body>" + NL +
								"</html>" ).getBytes( "UTF-8" ));
								
								return( true );
							}
							
							if ( !canonical_file.canRead()){
								
								return( false );
							}

							if ( canonical_file.length() < 512*1024 ){
							
								String	file_type;
								
								String file_name = canonical_file.getName();
								
								int	pos = file_name.lastIndexOf( "." );
		
								if ( pos == -1 ){
		
									file_type = "";
									
								}else{
		
									file_type = file_name.substring(pos+1);
								}
								
								FileInputStream	fis = null;
		
								try{
									fis = new FileInputStream(canonical_file);
		
									response.useStream( file_type, fis );
		
									return( true );
		
								}finally{
		
									if ( fis != null ){
		
										fis.close();
									}
								}
							}
							
							OutputStream 	os 	= null;
							InputStream 	is	= null;
							
							try{
								os = response.getRawOutputStream();
								
								byte[] buffer = new byte[128*1024];
								
								is = new FileInputStream( canonical_file );
								
								while( true ){
									
									int len = is.read( buffer );
									
									if ( len <= 0 ){
										
										break;
									}
									
									os.write( buffer, 0, len );
								}
							}catch( Throwable e ){
								
								e.printStackTrace();
								
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
					};
								
				PluginManager.registerPlugin(
					plugin,
					plugin_id );
				
				browse_plugins.put( dm, plugin );
				
			}else{
				
				String url = "http://127.0.0.1:" + plugin.getServerPort() + "/" + url_suffix;
				
				Utils.launch( url, false, true );
			}
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
}
