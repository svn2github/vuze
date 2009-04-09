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
import java.util.Map;

import org.eclipse.swt.program.Program;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.download.ForceRecheckListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadImpl;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadManagerImpl;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.download.DownloadManagerEnhancer;
import com.aelitis.azureus.core.download.EnhancedDownloadManager;
import com.aelitis.azureus.core.messenger.config.PlatformDCAdManager;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.core.vuzefile.VuzeFile;
import com.aelitis.azureus.core.vuzefile.VuzeFileComponent;
import com.aelitis.azureus.core.vuzefile.VuzeFileHandler;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.common.table.TableView;
import com.aelitis.azureus.ui.selectedcontent.DownloadUrlInfo;
import com.aelitis.azureus.ui.selectedcontent.DownloadUrlInfoContentNetwork;
import com.aelitis.azureus.ui.selectedcontent.ISelectedContent;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.browser.listener.DownloadUrlInfoSWT;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility;
import com.aelitis.azureus.ui.swt.utils.TorrentUIUtilsV3;
import com.aelitis.azureus.util.*;
import com.aelitis.azureus.util.win32.Win32Utils;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;

/**
 * @author TuxPaper
 * @created Oct 12, 2006
 *
 */
public class TorrentListViewsUtils
{

	public static final boolean ENABLE_ON_HOVER = false;

	/**
	 * @param dm
	 *
	 * @since 3.0.1.5
	 */
	public static void stop(DownloadManager dm) {
		int state = dm.getState();
		if (state == DownloadManager.STATE_ERROR) {
			dm.stopIt(DownloadManager.STATE_QUEUED, false, false);
		} else if (state == DownloadManager.STATE_STOPPED) {
			ManagerUtils.queue(dm, null);
		} else {
			ManagerUtils.stop(dm, null);
		}
	}

	public static void viewDetails(TableRowCore row, String ref) {
		Object ds = row.getDataSource(true);
		viewDetails(DataSourceUtils.getContentNetwork(ds),
				DataSourceUtils.getHash(ds), ref);
	}

	public static boolean canViewDetails(DownloadManager dm) {
		if (dm == null) {
			return( false );
		}
		if (!PlatformTorrentUtils.isContent(dm.getTorrent(), true)) {
			return( false );
		}

		try{
			return(	canViewDetails(DataSourceUtils.getContentNetwork(dm.getTorrent()),
						dm.getTorrent().getHashWrapper().toBase32String()));
		} catch (Throwable e) {
			Debug.out(e);
			
			return( false );
		}
	}
	
	public static void viewDetails(DownloadManager dm, String ref) {
		if (dm == null) {
			return;
		}
		if (!PlatformTorrentUtils.isContent(dm.getTorrent(), true)) {
			return;
		}

		try {
			viewDetails(DataSourceUtils.getContentNetwork(dm.getTorrent()),
					dm.getTorrent().getHashWrapper().toBase32String(), ref);
		} catch (Throwable e) {
			Debug.out(e);
		}
	}

	public static void viewDetails(ContentNetwork cn, String hash, String ref) {
		if (hash == null || cn == null) {
			return;
		}

		String url = cn.getContentDetailsService( hash, ref );

		UIFunctions functions = UIFunctionsManager.getUIFunctions();
		if (functions != null) {
			functions.viewURL(url, ContentNetworkUtils.getTarget(cn), ref);
		}
	}

	public static boolean canViewDetails(ContentNetwork cn, String hash) {
		if (hash == null || cn == null ) {
			return( false );
		}

		String url = cn.getContentDetailsService( hash, "" );

		UIFunctions functions = UIFunctionsManager.getUIFunctions();
		
		return( functions != null && url != null );
	}
	
	public static String
	getDetailsURL(
		DownloadManager		dm )
	{
		try {
			ContentNetwork cn = DataSourceUtils.getContentNetwork(dm.getTorrent());
			
			if ( cn == null ){
				
				return( null );
			}
			
			TOTorrent torrent = dm.getTorrent();
			
			if ( torrent == null ){
				
				return( null );
			}

			String hash = torrent.getHashWrapper().toBase32String();
			
			String url = cn.getContentDetailsService( hash, "" );

			return( url );
			
		}catch(Throwable  e ){
			
			Debug.out(e);
			
			return( null );
		}
	}

	/**
	 * @param ds
	 * @param ref
	 *
	 * @since 4.0.0.5
	 */
	public static void viewDetailsFromDS(Object ds, String ref) {
		String hash = DataSourceUtils.getHash(ds);
		if (hash == null) {
			return;
		}
		viewDetails(DataSourceUtils.getContentNetwork(ds), hash, ref);
	}


	public static void playOrStreamDataSource(Object ds, SWTSkinButtonUtility btn) {
		String referal = DLReferals.DL_REFERAL_UNKNOWN;
		if (ds instanceof VuzeActivitiesEntry) {
			referal = DLReferals.DL_REFERAL_PLAYDASHACTIVITY;
		} else if (ds instanceof DownloadManager) {
			referal = DLReferals.DL_REFERAL_PLAYDM;
		} else if (ds instanceof ISelectedContent) {
			referal = DLReferals.DL_REFERAL_SELCONTENT;
		}
		playOrStreamDataSource(ds, btn, referal);
	}

	public static void playOrStreamDataSource(Object ds,
			SWTSkinButtonUtility btn, String referal) {

		debugDCAD("enter - playOrStreamDataSource");

		DownloadManager dm = DataSourceUtils.getDM(ds);
		if (dm == null) {
			downloadDataSource(ds, true, referal);
		} else {
			playOrStream(dm, btn);
		}

		debugDCAD("exit - playOrStreamDataSource");

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
			AzureusCore core = AzureusCoreFactory.getSingleton();
			DownloadUrlInfo dlInfo = DataSourceUtils.getDownloadInfo(ds);
			if (dlInfo instanceof DownloadUrlInfoSWT) {
				TorrentUIUtilsV3.loadTorrent(core, dlInfo, playNow, false,
						true, true);
				return;
			}

			String hash = DataSourceUtils.getHash(ds);
			if (hash != null) {
				ContentNetwork cn = DataSourceUtils.getContentNetwork(ds);
				if (cn == null) {
					return;
				}
				if (ds instanceof VuzeActivitiesEntry) {
					if (((VuzeActivitiesEntry) ds).isDRM()) {
						TorrentListViewsUtils.viewDetails(cn, hash, "drm-play");
						return;
					}
				}

				String url = cn.getTorrentDownloadService(hash, referal);
				dlInfo = new DownloadUrlInfoContentNetwork(url, cn);
				TorrentUIUtilsV3.loadTorrent(core, dlInfo, playNow, false, true, true);
			} else if (dlInfo != null) {
				TorrentUIUtilsV3.loadTorrent(core, dlInfo, playNow, false,
						true, true);
			}
		}
	}

	public static boolean playOrStream(final DownloadManager dm,
			final SWTSkinButtonUtility btn) {
		boolean played = _playOrStream(dm, btn);
		if (played) {
			PlatformTorrentUtils.setHasBeenOpened(dm, true);
		}
		return played;
	}

	private static boolean _playOrStream(final DownloadManager dm,
			final SWTSkinButtonUtility btn) {

		debugDCAD("enter - playOrStream");

		if (dm == null) {
			return false;
		}

		//		if (!canPlay(dm)) {
		//			return false;
		//		}

		TOTorrent torrent = dm.getTorrent();
		if (PlayUtils.canUseEMP(torrent)) {
			debug("Can use EMP");

			if (openInEMP(dm)) {
				return true;
			} else {
				debug("Open EMP Failed");
			}
			// fallback to normal
		} else {
			debug("Can't use EMP. torrent says "
					+ PlatformTorrentUtils.useEMP(torrent));
		}

		if (btn != null) {
			btn.setDisabled(true);
		}

		boolean reenableButton = false;
		try {
			if (!PlayUtils.canProgressiveOrIsComplete(torrent)) {
				return false;
			}

			File file;
			String sFile = null;

			EnhancedDownloadManager edm = DownloadManagerEnhancer.getSingleton().getEnhancedDownload(
					dm);
			if (edm != null) {
				boolean doProgressive = edm.getProgressiveMode();
				if (doProgressive && edm.getProgressivePlayETA() > 0) {
					return false;
				}

				if (!doProgressive && dm.getDiskManagerFileInfo().length > 1
						&& PlatformTorrentUtils.getContentPrimaryFileIndex(torrent) == -1) {
					// multi-file torrent that we aren't progressive playing or useEMPing
					Utils.launch(dm.getSaveLocation().getAbsolutePath());
					return true;
				}

				file = edm.getPrimaryFile().getFile(true);
				sFile = file.getAbsolutePath();
			} else {
				sFile = dm.getDownloadState().getPrimaryFile();
				file = new File(sFile);
			}

			String ext = FileUtil.getExtension(sFile);
			
			try {
  			if (ext.equalsIgnoreCase(".exe")
  					&& DataSourceUtils.isPlatformContent(dm)
  					&& "Game".equalsIgnoreCase(PlatformTorrentUtils.getContentType(dm.getTorrent()))) {
  				Utils.launch(sFile);
  				return true;
  			}
			} catch (Exception e) {
				Debug.out(e);
			}

			String sPrefix = "v3.mb.openFile.";
			

			UIFunctionsSWT functionsSWT = UIFunctionsManagerSWT.getUIFunctionsSWT();
			if (functionsSWT == null) {
				return false;
			}
			
			Program program = Program.findProgram(ext);
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
				mb = new MessageBoxShell(functionsSWT.getMainShell(),
						MessageText.getString(sPrefix + "title"), MessageText.getString(
								sTextID, new String[] {
									dm.getDisplayName(),
									sFileType,
									ext
								}), buttons, 0, sPrefix + ".remember_id", MessageText.getString(sPrefix
								+ "remember"), false, 0);
				mb.setRememberOnlyIfButton(1);
				mb.setRelatedObject(dm);
			} else {
				mb = new MessageBoxShell(functionsSWT.getMainShell(),
						MessageText.getString(sPrefix + "title"), MessageText.getString(
								sTextID, new String[] {
									dm.getDisplayName(),
									sFileType,
									ext
								}), buttons, 0);
				mb.setRelatedObject(dm);
			}

			int i = mb.open();
			
			if(i == 0) {
				String url = MessageText.getString(sPrefix + "guideurl");
				if(UrlUtils.isURL(url)) {
					Utils.launch(url);
				}
			}
			
			if (i != 1 || program == null) {
				return false;
			}
			//}

			boolean bComplete = dm.isDownloadComplete(false);

			if (bComplete) {
				if (PlatformTorrentUtils.isContentAdEnabled(torrent)) {
					final String sfFile = sFile;
					debug("calling createASX from ...Tor.Utils.playOrStream, in is complete block.");
					DCAdManager.getInstance().createASX(dm,
							new DCAdManager.ASXCreatedListener() {
								public void asxCreated(File asxFile) {
									if (btn != null) {
										btn.setDisabled(false);
									}
									runFile(dm.getTorrent(), asxFile.getAbsolutePath());
								}

								public void asxFailed() {
									if (btn != null) {
										btn.setDisabled(false);
									}
									runFile(dm.getTorrent(), sfFile);
								}
							});
				} else {
					reenableButton = true;
					runFile(dm.getTorrent(), sFile);
				}
			} else {
				reenableButton = true;
				try {
					playViaMediaServer(DownloadManagerImpl.getDownloadStatic(dm));
				} catch (DownloadException e) {
					Debug.out(e);
				}
			}
		} finally {
			if (btn != null && reenableButton) {
				btn.setDisabled(false);
			}
		}

		debugDCAD("enter - playOrStream");

		return true;
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

	private static void runFile(TOTorrent torrent, String runFile) {
		runFile(torrent, runFile, false);
	}

	private static void runFile(final TOTorrent torrent, final String runFile,
			final boolean forceWMP) {

		AEThread2 thread = new AEThread2("runFile", true) {
			public void run() {

				Utils.execSWTThread(new AERunnable() {

					public void runSupport() {
						debugDCAD("enter - runFile - runSupport");

						if (PlayUtils.canUseEMP(torrent)) {
							Debug.out("Shouldn't call runFile with EMP torrent.");
						}

						if (PlatformTorrentUtils.isContentDRM(torrent) || forceWMP) {
							if (!runInMediaPlayer(runFile)) {
								Utils.launch(runFile);
							}
						} else {
							Utils.launch(runFile);
						}

						debugDCAD("exit - runFile - runSupport");
					}

				});

			}

		};
		thread.start();
	}

	/**
	 * New version accepts map with ASX parameters. If the params are null then is uses the
	 * old version to start the player. If the
	 *
	 *
	 * @param dm - DownloadManager
	 * @return - boolean
	 * @since 3.0.4.4 -
	 */
	private static boolean openInEMP(DownloadManager dm) {

		debugDCAD("enter - openInEMP");

		Class epwClass = null;
		try {
			PluginInterface pi = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByID(
					"azemp");

			if (pi == null) {

				return (false);
			}

			epwClass = pi.getPlugin().getClass().getClassLoader().loadClass(
					"com.azureus.plugins.azemp.ui.swt.emp.EmbeddedPlayerWindowSWT");

		} catch (ClassNotFoundException e1) {
			return false;
		}

		//Data is passed to the openWindow via download manager.
		try {
			debug("EmbeddedPlayerWindowSWT - openWindow");
			Method method = epwClass.getMethod("openWindow", new Class[] {
				DownloadManager.class
			});

			method.invoke(null, new Object[] {
				dm
			});

			return true;
		} catch (Throwable e) {
			e.printStackTrace();
			if (e.getMessage() == null
					|| !e.getMessage().toLowerCase().endsWith("only")) {
				Debug.out(e);
			}
		}

		return false;
	}//openInEMP

	/**
	* @param dm
	*
	* @since 3.0.0.7
	*/
	private static void handleNoFileExists(DownloadManager dm) {
		UIFunctionsSWT functionsSWT = UIFunctionsManagerSWT.getUIFunctionsSWT();
		if (functionsSWT == null) {
			return;
		}
		ManagerUtils.start(dm);

		String sPrefix = "v3.mb.PlayFileNotFound.";
		MessageBoxShell mb = new MessageBoxShell(functionsSWT.getMainShell(),
				MessageText.getString(sPrefix + "title"), MessageText.getString(sPrefix
						+ "text", new String[] {
					dm.getDisplayName(),
				}), new String[] {
					MessageText.getString(sPrefix + "button.remove"),
					MessageText.getString(sPrefix + "button.redownload"),
					MessageText.getString("Button.cancel"),
				}, 2);
		mb.setRelatedObject(dm);
		int i = mb.open();

		if (i == 0) {
			ManagerUtils.remove(dm, functionsSWT.getMainShell(), true, false);
		} else if (i == 1) {
			dm.forceRecheck(new ForceRecheckListener() {
				public void forceRecheckComplete(DownloadManager dm) {
					ManagerUtils.start(dm);
				}
			});
		}
	}

	/**
	 * @param string
	 */
	private static boolean runInMediaPlayer(String mediaFile) {

		debugDCAD("enter - runInMediaPlayer");

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
	public static void playViaMediaServer(Download download) {

		debugDCAD("enter - playViaMediaServer");

		try {
			final DownloadManager dm = ((DownloadImpl) download).getDownload();

			TOTorrent torrent = dm.getTorrent();
			if (PlatformTorrentUtils.isContentAdEnabled(torrent)) {
				debug("calling createASX from ...Tor.Utils.playViaMediaServer, in is complete block. dm="
						+ dm);
				DCAdManager.getInstance().createASX(dm,
						new DCAdManager.ASXCreatedListener() {
							public void asxCreated(File asxFile) {
								runFile(dm.getTorrent(), asxFile.getAbsolutePath(), true);
							}

							public void asxFailed() {
								runFile(dm.getTorrent(), PlayUtils.getContentUrl(dm), true);
							}
						});
			} else {
				// force to WMP if we aren't using EMP
				runFile(torrent, PlayUtils.getContentUrl(dm), true);
			}
		} catch (Throwable e) {
			Logger.log(new LogEvent(LogIDs.UI3, "IPC to media server plugin failed",
					e));
		}
	}

	public static void removeDownloads(final DownloadManager[] dms) {
		if (dms == null) {
			return;
		}
		
		int doAllAs = -1;

		for (int i = 0; i < dms.length; i++) {
			DownloadManager dm = dms[i];
			if (dm != null) {
				if (PublishUtils.isPublished(dm) || dm.getDownloadState().getFlag(
						Download.FLAG_DO_NOT_DELETE_DATA_ON_REMOVE)) {
					ManagerUtils.remove(dm, null, true, false, null);
					continue;
				}

				boolean deleteTorrent = true;
				boolean deleteData = true;


				if (!dm.getDownloadState().getFlag(DownloadManagerState.FLAG_LOW_NOISE)) {
					String path = dm.getSaveLocation().toString();

					String title = MessageText.getString("deletedata.title");
					String text = MessageText.getString("v3.deleteContent.message",
							new String[] {
								dm.getDisplayName()
							});
						
					int result;
					if (doAllAs < 0) {
  					MessageBoxShell mb = new MessageBoxShell(Utils.findAnyShell(), title,
  							text, new String[] {
  								MessageText.getString("Button.cancel"),
  								MessageText.getString("Button.deleteContent.fromComputer"),
  								MessageText.getString("Button.deleteContent.fromLibrary"),
  							}, 2, null, null, false, 0);
  					int numLeft = (dms.length - i);
  					if (numLeft > 1) {
    					mb.setRememberText(MessageText.getString(
									"v3.deleteContent.applyToAll", new String[] {
										"" + numLeft
									}));
    					mb.setRememberID("na", false);
    					mb.setRememberOnlyIfButton(-3);
  					}
  					mb.setRelatedObject(dm);
  					mb.setLeftImage(ImageLoader.getInstance().getImage("image.trash"));
  
  					result = mb.open();

  					ImageLoader.getInstance().releaseImage("image.trash");
						if (numLeft > 1 && mb.isRemembered()) {
							doAllAs = result;
						}
					} else {
						result = doAllAs;
					}

					if (result == 1 || result == 2) {
						if (result == 2) {
							deleteData = false;
						}
					
						ManagerUtils.asyncStopDelete(dm, DownloadManager.STATE_STOPPED,
								deleteTorrent, deleteData, null);
					} else {
						if (doAllAs >= 0) {
							return;
						}
						continue;
					}
				} else {
					ManagerUtils.asyncStopDelete(dm, DownloadManager.STATE_STOPPED,
							deleteTorrent, deleteData, null);
				}
			}
		}
	}

	public static void removeDownload(final DownloadManager dm,
				final TableView tableView) {

		debug("removeDownload");

		AERunnable failure = null;
		if (tableView != null) {
			tableView.removeDataSource(dm, true);

			failure = new AERunnable() {
				public void runSupport() {
					tableView.addDataSource(dm, true);
				}
			};
		}

		if (PublishUtils.isPublished(dm) || dm.getDownloadState().getFlag(
				Download.FLAG_DO_NOT_DELETE_DATA_ON_REMOVE)) {
			ManagerUtils.remove(dm, null, true, false, failure);
			return;
		}

		boolean deleteTorrent = true;
		boolean deleteData = true;


		if (!dm.getDownloadState().getFlag(DownloadManagerState.FLAG_LOW_NOISE)) {
			String path = dm.getSaveLocation().toString();

			String title = MessageText.getString("deletedata.title");
			String text = MessageText.getString("v3.deleteContent.message",
					new String[] {
						dm.getDisplayName()
					});
					
			MessageBoxShell mb = new MessageBoxShell(Utils.findAnyShell(), title,
					text, new String[] {
						MessageText.getString("Button.cancel"),
						MessageText.getString("Button.deleteContent.fromComputer"),
						MessageText.getString("Button.deleteContent.fromLibrary"),
					}, 2, null, null, false, 0);
			mb.setRelatedObject(dm);
			mb.setLeftImage(ImageLoader.getInstance().getImage("image.trash"));

			int result = mb.open();
			ImageLoader.getInstance().releaseImage("image.trash");

			if (result == 1 || result == 2) {
				if (result == 2) {
					deleteData = false;
				}
			
				ManagerUtils.asyncStopDelete(dm, DownloadManager.STATE_STOPPED,
						deleteTorrent, deleteData, failure);
			} else {
				if (failure != null) {
					failure.runSupport();
				}
				return;
			}
		} else {
			ManagerUtils.asyncStopDelete(dm, DownloadManager.STATE_STOPPED,
					deleteTorrent, deleteData, failure);
		}
	}

	/**
	 * @param dm
	 *
	 * @since 3.0.2.3
	 */
	public static void showHomeHint(final DownloadManager dm) {
	}

	public static void debugDCAD(String s) {
		PlatformDCAdManager.debug("TorrentListViewsUtils: " + s);
	}//debugDCAD

	public static boolean playOrStream(final DownloadManager dm) {
		return playOrStream(dm, null);
	}
}
