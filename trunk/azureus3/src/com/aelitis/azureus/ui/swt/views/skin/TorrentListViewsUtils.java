/**
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.ui.swt.views.skin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Map;

import org.eclipse.swt.program.Program;

import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.ForceRecheckListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadImpl;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadManagerImpl;
import org.gudy.azureus2.ui.swt.TextViewerWindow;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.download.*;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.core.util.LaunchManager;
import com.aelitis.azureus.core.vuzefile.VuzeFile;
import com.aelitis.azureus.core.vuzefile.VuzeFileComponent;
import com.aelitis.azureus.core.vuzefile.VuzeFileHandler;
import com.aelitis.azureus.ui.UserPrompterResultListener;
import com.aelitis.azureus.ui.selectedcontent.DownloadUrlInfo;
import com.aelitis.azureus.ui.selectedcontent.DownloadUrlInfoContentNetwork;
import com.aelitis.azureus.ui.selectedcontent.ISelectedContent;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.player.PlayerInstallWindow;
import com.aelitis.azureus.ui.swt.player.PlayerInstaller;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility;
import com.aelitis.azureus.ui.swt.toolbar.ToolBarItem;
import com.aelitis.azureus.ui.swt.utils.TorrentUIUtilsV3;
import com.aelitis.azureus.util.DLReferals;
import com.aelitis.azureus.util.DataSourceUtils;
import com.aelitis.azureus.util.PlayUtils;
import com.aelitis.azureus.util.win32.Win32Utils;

/**
 * @author TuxPaper
 * @created Oct 12, 2006
 *
 */
public class TorrentListViewsUtils
{
	private static StreamManagerDownload	current_stream;
	private static TextViewerWindow			stream_viewer;
	
	public static void playOrStreamDataSource(Object ds,
			boolean launch_already_checked) {
		String referal = DLReferals.DL_REFERAL_UNKNOWN;
		if (ds instanceof VuzeActivitiesEntry) {
			referal = DLReferals.DL_REFERAL_PLAYDASHACTIVITY;
		} else if (ds instanceof DownloadManager) {
			referal = DLReferals.DL_REFERAL_PLAYDM;
		} else if (ds instanceof ISelectedContent) {
			referal = DLReferals.DL_REFERAL_SELCONTENT;
		}
		playOrStreamDataSource(ds, referal, launch_already_checked, true );
	}

	public static void playOrStreamDataSource(Object ds, String referal,
			boolean launch_already_checked, boolean complete_only) {

		DiskManagerFileInfo fileInfo = DataSourceUtils.getFileInfo(ds);
		if (fileInfo != null) {
			playOrStream(fileInfo.getDownloadManager(), fileInfo.getIndex(),
					complete_only, launch_already_checked);
		}
		
		DownloadManager dm = DataSourceUtils.getDM(ds);
		if (dm == null) {
			downloadDataSource(ds, true, referal);
		} else {
			playOrStream(dm, -1, complete_only, launch_already_checked);
		}

	}

	public static void downloadDataSource(Object ds, boolean playNow,
			String referal) {
		TOTorrent torrent = DataSourceUtils.getTorrent(ds);
		
			// handle encapsulated vuze file
		try{
			Map torrent_map = torrent.serialiseToMap();
			
			torrent_map.remove( "info" );
			
			VuzeFile vf = VuzeFileHandler.getSingleton().loadVuzeFile( torrent_map );
		
			if ( vf != null ){
				
				VuzeFileHandler.getSingleton().handleFiles( new VuzeFile[]{ vf }, VuzeFileComponent.COMP_TYPE_NONE );
				
				return;
			}
		}catch( Throwable e ){
			
		}
		// we want to re-download the torrent if it's ours, since the existing
		// one is likely stale
		if (torrent != null && !DataSourceUtils.isPlatformContent(ds)) {
			TorrentUIUtilsV3.addTorrentToGM(torrent);
		} else {
			DownloadUrlInfo dlInfo = DataSourceUtils.getDownloadInfo(ds);
			if (dlInfo != null) {
				TorrentUIUtilsV3.loadTorrent(dlInfo, playNow, false, true);
				return;
			}

			String hash = DataSourceUtils.getHash(ds);
			if (hash != null) {
				ContentNetwork cn = DataSourceUtils.getContentNetwork(ds);
				if (cn == null) {
					dlInfo = new DownloadUrlInfo(UrlUtils.parseTextForMagnets(hash));
					dlInfo.setReferer(referal);
					TorrentUIUtilsV3.loadTorrent(dlInfo, playNow, false, true);
					return;
				}

				String url = cn.getTorrentDownloadService(hash, referal);
				dlInfo = new DownloadUrlInfoContentNetwork(url, cn);
				TorrentUIUtilsV3.loadTorrent(dlInfo, playNow, false, true);
			}
		}
	}

	public static void playOrStream(final DownloadManager dm,
			final int file_index, final boolean complete_only,
			boolean launch_already_checked) {
			
		if (dm == null) {
			return;
		}
		
		if ( launch_already_checked ){
		
			_playOrStream(dm, file_index, complete_only);
			
		}else{
			
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
									_playOrStream(dm, file_index, complete_only);
								}
							});
					}
					
					public void
					actionDenied(
						Throwable		reason )
					{
						Debug.out( "Launch request denied", reason );
					}
				});
		}
	}

	private static void _playOrStream(final DownloadManager dm,
			final int file_index, boolean complete_only) {

		if (dm == null) {
			return;
		}

		//		if (!canPlay(dm)) {
		//			return false;
		//		}

		final TOTorrent torrent = dm.getTorrent();
		if (torrent == null) {
			return;
		}
		if (PlayUtils.canUseEMP(torrent, file_index,complete_only)) {
			debug("Can use EMP");

			int open_result = openInEMP(dm,file_index,complete_only);
			
			if ( open_result == 0 ){
				PlatformTorrentUtils.setHasBeenOpened(dm, true);
				return;
			}else if ( open_result == 2 ){
				debug( "Open in EMP abandoned" );
				return;
			} else {
				debug("Open EMP Failed");
			}
			// fallback to normal
		} else {
			debug("Can't use EMP. torrent says "
					+ PlatformTorrentUtils.useEMP(torrent));
		}

		ToolBarItem btnPlay = null;
		ToolBarView tb = (ToolBarView) SkinViewManager.getByClass(ToolBarView.class);
		if (tb != null) {
			btnPlay = tb.getToolBarItem("play");
		}
		final ToolBarItem btn = btnPlay;
		if (btn != null) {
			btn.setEnabled(false);
		}

		boolean reenableButton = false;
		try {
			if (!PlayUtils.canProgressiveOrIsComplete(torrent)) {
				return;
			}

			File file;
			String sFile = null;

			EnhancedDownloadManager edm = DownloadManagerEnhancer.getSingleton().getEnhancedDownload(
					dm);
			if (edm != null) {
				boolean doProgressive = edm.getProgressiveMode();
				if (doProgressive && edm.getProgressivePlayETA() > 0) {
					return;
				}

				if (!doProgressive && dm.getNumFileInfos() > 1
						&& PlatformTorrentUtils.getContentPrimaryFileIndex(torrent) == -1) {
					// multi-file torrent that we aren't progressive playing or useEMPing
					Utils.launch(dm.getSaveLocation().getAbsolutePath());
  				reenableButton = true;
  				PlatformTorrentUtils.setHasBeenOpened(dm, true);
  				return;
				}

				file = edm.getPrimaryFile().getFile(true);
				sFile = file.getAbsolutePath();
			} else {
				sFile = dm.getDownloadState().getPrimaryFile();
				file = new File(sFile);
			}
			
			final String sfFile = sFile;

			String ext = FileUtil.getExtension(sFile);
			
			try {
  			if (ext.equalsIgnoreCase(".exe")
  					&& DataSourceUtils.isPlatformContent(dm)
  					&& "Game".equalsIgnoreCase(PlatformTorrentUtils.getContentType(dm.getTorrent()))) {
  				reenableButton = true;
  				Utils.launch(sFile);
  				PlatformTorrentUtils.setHasBeenOpened(dm, true);
  				return;
  			}
			} catch (Exception e) {
				Debug.out(e);
			}

			final String sPrefix = "v3.mb.openFile.";
			

			UIFunctionsSWT functionsSWT = UIFunctionsManagerSWT.getUIFunctionsSWT();
			if (functionsSWT == null) {
				return;
			}
			
			final Program program = Program.findProgram(ext);
			String sTextID;
			String sFileType;
			if (program == null) {
				sTextID = sPrefix + "text.unknown";
				sFileType = ext;
			} else {
				sTextID = sPrefix + "text.known";
				sFileType = program.getName();
			}
			
			String[] buttons = new String[(program == null ? 2 : 3)];
			buttons[0] = MessageText.getString(sPrefix + "button.guide");
			buttons[buttons.length-1] = MessageText.getString(sPrefix + "button.cancel");
			
			MessageBoxShell mb = null;
			if(program != null) {
				buttons[1] = MessageText.getString(sPrefix + "button.play");
				mb = new MessageBoxShell(MessageText.getString(sPrefix + "title"),
						MessageText.getString(sTextID, new String[] {
									dm.getDisplayName(),
									sFileType,
									ext
								}), buttons, 0);
				mb.setRemember(sPrefix + ".remember_id", false, MessageText.getString(sPrefix
						+ "remember"));
				mb.setRememberOnlyIfButton(1);
				mb.setRelatedObject(dm);
			} else {
				mb = new MessageBoxShell(MessageText.getString(sPrefix + "title"),
						MessageText.getString(sTextID, new String[] {
							dm.getDisplayName(),
							sFileType,
							ext
						}), buttons, 0);
				mb.setRelatedObject(dm);
			}

			reenableButton = false;
			mb.open(new UserPrompterResultListener() {
				public void prompterClosed(int i) {
					if(i == 0) {
						String url = MessageText.getString(sPrefix + "guideurl");
						if(UrlUtils.isURL(url)) {
							Utils.launch(url);
							return;
						}
					}
					
					if (i != 1 || program == null) {
						return;
					}
					
    			boolean bComplete = dm.isDownloadComplete(false);
    
    			if (bComplete) {
    				if (btn != null) {
    					btn.setEnabled(true);
    				}
    				runFile(dm.getTorrent(), file_index, sfFile);
    			} else {
    				if (btn != null) {
    					btn.setEnabled(true);
    				}
    				try {
    					playViaMediaServer(DownloadManagerImpl.getDownloadStatic(dm), file_index );
    				} catch (DownloadException e) {
    					Debug.out(e);
    				}
    			}
				}
			});
			
		} finally {
			if (btn != null && reenableButton) {
				btn.setEnabled(true);
			}
		}
	}


	/**
	 * @param string
	 *
	 * @since 3.0.3.3
	 */
	private static void debug(String string) {
		if (org.gudy.azureus2.core3.util.Constants.isCVSVersion()) {
			System.out.println(string);
		}
	}

	private static void runFile(TOTorrent torrent, int file_index, String runFile) {
		runFile(torrent, file_index, runFile, false);
	}

	private static void runFile(final TOTorrent torrent, final int file_index, final String runFile,
			final boolean forceWMP) {

		AEThread2 thread = new AEThread2("runFile", true) {
			public void run() {

				Utils.execSWTThread(new AERunnable() {

					public void runSupport() {
						if (PlayUtils.canUseEMP(torrent, file_index)) {
							Debug.out("Shouldn't call runFile with EMP torrent.");
						}

						if (PlatformTorrentUtils.isContentDRM(torrent) || forceWMP) {
							if (!runInMediaPlayer(runFile)) {
								Utils.launch(runFile);
							}
						} else {
							Utils.launch(runFile);
						}

					}

				});

			}

		};
		thread.start();
	}
	
	private static boolean	emp_installing;
	
	/**
	 * 
	 * @param dm
	 * @return 0=good, 1 = fail, 2 = abandon
	 */
	private static int 
	installEMP(
		final DownloadManager 	dm,
		final int 				file_index,
		final boolean			complete_only ) 
	{
		synchronized( TorrentListViewsUtils.class ){
			
			if ( emp_installing ){
				
				Debug.out( "EMP is already being installed, secondary launch for " + dm.getDisplayName() + " ignored" );
				
				return( 2 );
			}
			
			emp_installing = true;
		}
		
		boolean	running = false;
		
		try{
			final PlayerInstaller installer = new PlayerInstaller();
			
			final PlayerInstallWindow window = new PlayerInstallWindow(installer);
			
			window.open();
			
			AEThread2 installerThread = new AEThread2("player installer",true) {
				public void 
				run() 
				{
					try{
						if(installer.install()) {
							Utils.execSWTThread(new AERunnable() {
								
								public void runSupport() {
									openInEMP(dm,file_index,complete_only);
									
								}
							});
						}
					}finally{
						
						synchronized( TorrentListViewsUtils.class ){
							
							emp_installing = false;
						}
					}
				}
			};
			
			installerThread.start();
			
			running = true;
			
			return( 0 );
			
		}catch( Throwable e ){
			
			Debug.out( e );
			
			return( 1 );
			
		}finally{
			
			if ( !running ){
				
				synchronized( TorrentListViewsUtils.class ){
					
					emp_installing = false;
				}
			}
		}
		
	}

	/**
	 * New version accepts map with ASX parameters. If the params are null then is uses the
	 * old version to start the player. If the
	 *
	 *
	 * @param dm - DownloadManager
	 * @return - int: 0 = ok, 1 = fail, 2 = abandon, installation in progress
	 * @since 3.0.4.4 -
	 */
	private static int 
	openInEMP(
		final DownloadManager dm, int _file_index, boolean complete_only ) 
	{
		try {
			final int file_index;
			
			if ( _file_index == -1 ){
				
				file_index = PlayUtils.getPrimaryFileIndex( dm );
				
			}else{
				
				file_index = _file_index;
			}
			
			if ( file_index == -1 ){
				
				return( 1 );
			}
						
			org.gudy.azureus2.plugins.disk.DiskManagerFileInfo file = PluginCoreUtils.wrap( dm ).getDiskManagerFileInfo()[ file_index ];
			
			final URL url;
			
			if ((! complete_only ) && file.getDownloaded() != file.getLength()){
					
				url = PlayUtils.getMediaServerContentURL( file );
				
			}else{
				
				url = null;
			}
				
			if ( url != null ){
								
				new AEThread2( "stream:async" )
				{
					public void
					run()
					{
						StreamManager	sm = StreamManager.getSingleton();

						synchronized( TorrentListViewsUtils.class ){
							
							if ( current_stream != null ){
								
								if ( current_stream.getURL().equals( url )){
									
									current_stream.setPreviewMode( !current_stream.getPreviewMode());
									
									return;
								}
								
								current_stream.cancel();
								
								current_stream = null;
							}
			
							if ( stream_viewer == null || stream_viewer.isDisposed()){
								
								Utils.execSWTThread(
									new Runnable()
									{
										public void
										run()
										{
											if ( stream_viewer != null ){
												
												stream_viewer.close();
											}
											
											stream_viewer = new 
												TextViewerWindow( "Stream Status", "Debug information for stream process", "", false );
											
											stream_viewer.addListener(
												new TextViewerWindow.TextViewerWindowListener()
												{
													public void 
													closed() 
													{
														synchronized( TorrentListViewsUtils.class ){
															
															if ( current_stream != null ){
																
																current_stream.cancel();
																
																current_stream = null;
															}
														}
													}
												});
										}
									});
							}
							
							current_stream = 
								sm.stream( 
									dm, file_index, url, false,
									new StreamManagerDownloadListener()
									{
										public void
										updateActivity(
											String		str )
										{
											append( "Activity: " + str );
										}
										
										public void
										updateStats(
											int			secs_until_playable,
											int			buffer_secs,
											long		buffer_bytes,
											int			target_secs )
										{
											append( "stats: play in " + secs_until_playable + " sec, buffer=" + DisplayFormatters.formatByteCountToKiBEtc( buffer_bytes ) + "/" + buffer_secs + " sec - target=" + target_secs + " sec" );
										}
										
										public void
										ready()
										{
											append( "ready" );
										}
										
										public void
										failed(
											Throwable 	error )
										{
											append( "failed: " + Debug.getNestedExceptionMessage(error));
											
											Debug.out( error );
										}
										
										private void
										append(
											final String	str )
										{
											Utils.execSWTThread(
												new Runnable()
												{
													public void
													run()
													{
														if ( stream_viewer != null && !stream_viewer.isDisposed()){
															
															stream_viewer.append( str + "\r\n" );
														}
													}
												});
										}
									});
							}
						}
					}.start();
	
					
				return( 0 );
						
			}
			
			Class epwClass = null;
			
			try {
				// Assumed we have a core, since we are passed a
				// DownloadManager
				PluginInterface pi = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByID(
						"azemp", false );

				if (pi == null) {

					return (installEMP(dm, file_index,complete_only ));
					
				}else if ( !pi.getPluginState().isOperational()){
					
					return( 1 );
				}

				epwClass = pi.getPlugin().getClass().getClassLoader().loadClass(
						"com.azureus.plugins.azemp.ui.swt.emp.EmbeddedPlayerWindowSWT");

			} catch (ClassNotFoundException e1) {
				return 1;
			}
	
										
			try{
				Method method = epwClass.getMethod("openWindow", new Class[] {
						File.class, String.class
					});

				File f = file.getFile( true );
				
				method.invoke(null, new Object[] {
						f, f.getName()
					});
					
				return( 0 );
					
			}catch( Throwable e ){
				debug( "file/name open method missing" );
			}

			
				// fall through here if old emp
			
			Method method = epwClass.getMethod("openWindow", new Class[] {
				DownloadManager.class
			});

			method.invoke(null, new Object[] {
				dm
			});

			return 0;
		} catch (Throwable e) {
			e.printStackTrace();
			if (e.getMessage() == null
					|| !e.getMessage().toLowerCase().endsWith("only")) {
				Debug.out(e);
			}
		}

		return 1;
	}//openInEMP

	/**
	* @param dm
	*
	* @since 3.0.0.7
	*/
	private static void handleNoFileExists(final DownloadManager dm) {
		final UIFunctionsSWT functionsSWT = UIFunctionsManagerSWT.getUIFunctionsSWT();
		if (functionsSWT == null) {
			return;
		}
		ManagerUtils.start(dm);

		String sPrefix = "v3.mb.PlayFileNotFound.";
		MessageBoxShell mb = new MessageBoxShell(
				MessageText.getString(sPrefix + "title"), MessageText.getString(sPrefix
						+ "text", new String[] {
					dm.getDisplayName(),
				}), new String[] {
					MessageText.getString(sPrefix + "button.remove"),
					MessageText.getString(sPrefix + "button.redownload"),
					MessageText.getString("Button.cancel"),
				}, 2);
		mb.setRelatedObject(dm);
		mb.open(new UserPrompterResultListener() {
			public void prompterClosed(int i) {
				if (i == 0) {
					ManagerUtils.asyncStopDelete(dm, DownloadManager.STATE_STOPPED,
							true, false, null);
				} else if (i == 1) {
					dm.forceRecheck(new ForceRecheckListener() {
						public void forceRecheckComplete(DownloadManager dm) {
							ManagerUtils.start(dm);
						}
					});
				}
			}
		});

	}

	/**
	 * @param string
	 */
	private static boolean runInMediaPlayer(String mediaFile) {

		if (Constants.isWindows) {
			String wmpEXE = Win32Utils.getWMP();
			if (new File(wmpEXE).exists()) {
				try {
					Runtime.getRuntime().exec(wmpEXE + " \"" + mediaFile + "\"");
					return true;
				} catch (IOException e) {
					Debug.out("error playing " + mediaFile + " via WMP " + mediaFile, e);
				}
			}
		}
		return false;
	}

	/**
	 * XXX DO NOT USE.  Only for EMP <= 2.0.14 support
	 * @param dm
	 * @return
	 */
	public static String getMediaServerContentURL(DownloadManager dm) {
		try {
			return PlayUtils.getMediaServerContentURL(DownloadManagerImpl.getDownloadStatic(dm));
		} catch (DownloadException e) {
		}
		return null;
	}

	/**
	 * 
	 */
	public static void playViaMediaServer(Download download, int file_index ) {

		try {
			final DownloadManager dm = ((DownloadImpl) download).getDownload();

			TOTorrent torrent = dm.getTorrent();
			runFile(torrent, file_index, PlayUtils.getContentUrl(dm), true);
		} catch (Throwable e) {
			Logger.log(new LogEvent(LogIDs.UI3, "IPC to media server plugin failed",
					e));
		}
	}

	public static void playOrStream(final DownloadManager dm, int file_index ) {
		playOrStream(dm, file_index, PlayUtils.COMPLETE_PLAY_ONLY, false );
	}
}
