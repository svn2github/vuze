/**
 * Created on Jun 1, 2008
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */
 
package com.aelitis.azureus.util;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;

import org.eclipse.swt.program.Program;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadManagerImpl;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.download.DownloadManagerEnhancer;
import com.aelitis.azureus.core.download.EnhancedDownloadManager;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;

/**
 * @author TuxPaper
 * @created Jun 1, 2008
 *
 */
public class PlayUtils
{
	private static boolean triedLoadingEmpPluginClass = false;

	private static Method methodIsExternallyPlayable;

	public static boolean prepareForPlay(DownloadManager dm) {
		EnhancedDownloadManager edm = DownloadManagerEnhancer.getSingleton().getEnhancedDownload(
				dm);
	
		if (edm != null) {
	
			edm.setProgressiveMode(true);
	
			return (true);
		}
	
		return (false);
	}

	public static boolean canProgressiveOrIsComplete(TOTorrent torrent) {
		if (torrent == null) {
			return false;
		}
		try {
			DownloadManagerEnhancer enhancer = DownloadManagerEnhancer.getSingleton();
			EnhancedDownloadManager edm = DownloadManagerEnhancer.getSingleton().getEnhancedDownload(
					torrent.getHash());
	
			if (edm == null) {
				return enhancer.isProgressiveAvailable()
						&& PlatformTorrentUtils.isContentProgressive(torrent);
			}
	
			boolean complete = edm.getDownloadManager().isDownloadComplete(false);
			if (complete) {
				return true;
			}
	
			// not complete
			if (!edm.supportsProgressiveMode()) {
				return false;
			}
		} catch (TOTorrentException e) {
			return false;
		}
	
		return true;
	}

	public static boolean canUseEMP(TOTorrent torrent) {
		if (canPlayViaExternalEMP(torrent)) {
			return true;
		}
		
		if (!PlatformTorrentUtils.useEMP(torrent)
				|| !PlatformTorrentUtils.embeddedPlayerAvail()) {
			return false;
		}
	
		return canProgressiveOrIsComplete(torrent);
	}

	private static boolean canPlay(DownloadManager dm) {
		if (dm == null) {
			return false;
		}
		TOTorrent torrent = dm.getTorrent();
		if (!PlatformTorrentUtils.isContent(torrent, false)) {
			return false;
		}
	
		return dm.getAssumedComplete() || canUseEMP(torrent);
	}

	private static boolean canPlay(TOTorrent torrent) {
		if (!PlatformTorrentUtils.isContent(torrent, false)) {
			return false;
		}
	
		GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
		DownloadManager dm = gm.getDownloadManager(torrent);
	
	
		if (dm != null) {
			return dm.getAssumedComplete() || canUseEMP(torrent);
		}
		return canUseEMP(torrent);
	}

	public static boolean canPlayDS(Object ds) {
		if (ds == null) {
			return false;
		}
	
		DownloadManager dm = DataSourceUtils.getDM(ds);
		if (dm != null) {
			return canPlay(dm);
		}
		TOTorrent torrent = DataSourceUtils.getTorrent(ds);
		if (torrent != null) {
			return canPlay(torrent);
		}
		if (ds instanceof VuzeActivitiesEntry) {
			return ((VuzeActivitiesEntry) ds).isPlayable();
		}
		return false;
	}

	/**
	 * @param dmContent
	 * @return
	 *
	 * @since 3.0.4.3
	 */
	public static String getContentUrl(DownloadManager dmContent) {
		String contentPath;
		if (dmContent.isDownloadComplete(false)) {
			//use the file path if download is complete.
			EnhancedDownloadManager edm = DownloadManagerEnhancer.getSingleton().getEnhancedDownload(
					dmContent);
			File file;
			if (edm != null) {
				file = edm.getPrimaryFile().getFile(true);
			} else {
				file = new File(dmContent.getDownloadState().getPrimaryFile());
			}
			try {
				contentPath = file.toURL().toString();
			} catch (MalformedURLException e) {
				contentPath = file.getAbsolutePath();
			}
		} else {
			//use the stream path if download is not complete.
			contentPath = PlayUtils.getMediaServerContentURL(dmContent);
		}
		return contentPath;
	}

	public static String getMediaServerContentURL(DownloadManager dm) {
		try {
			return PlayUtils.getMediaServerContentURL(DownloadManagerImpl.getDownloadStatic(dm));
		} catch (DownloadException e) {
		}
		return null;
	}

	/**
	 * @param dl
	 *
	 * @since 3.0.2.3
	 */
	public static String getMediaServerContentURL(Download dl) {
	
		//TorrentListViewsUtils.debugDCAD("enter - getMediaServerContentURL");
	
		PluginManager pm = AzureusCoreFactory.getSingleton().getPluginManager();
		PluginInterface pi = pm.getPluginInterfaceByID("azupnpav");
	
		if (pi == null) {
			Logger.log(new LogEvent(LogIDs.UI3, "Media server plugin not found"));
			return null;
		}
	
		if (!pi.isOperational()) {
			Logger.log(new LogEvent(LogIDs.UI3, "Media server plugin not operational"));
			return null;
		}
	
		try {
			Program program = Program.findProgram(".qtl");
			boolean hasQuickTime = program == null ? false
					: (program.getName().toLowerCase().indexOf("quicktime") != -1);
	
			pi.getIPC().invoke("setQuickTimeAvailable", new Object[] {
				new Boolean(hasQuickTime)
			});
	
			Object url = pi.getIPC().invoke("getContentURL", new Object[] {
				dl
			});
			if (url instanceof String) {
				return (String) url;
			}
		} catch (Throwable e) {
			Logger.log(new LogEvent(LogIDs.UI3, LogEvent.LT_WARNING,
					"IPC to media server plugin failed", e));
		}
	
		return null;
	}
	
	private static final boolean canPlayViaExternalEMP(TOTorrent torrent) {
		if (!triedLoadingEmpPluginClass) {
			triedLoadingEmpPluginClass = true;

  		try {
  			PluginInterface pi = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByID(
  					"azemp");
  
  			if (pi == null) {
  
  				return (false);
  			}
  
  			Class empPluginClass = pi.getPlugin().getClass();

  			methodIsExternallyPlayable = empPluginClass.getMethod("isExternallyPlayabale", new Class[] {
  				TOTorrent.class
  			});
  			
  		} catch (Exception e1) {
  			return false;
  		}
		}

		if (methodIsExternallyPlayable == null) {
			return false;
		}
		
		//Data is passed to the openWindow via download manager.
		try {

			Object retObj = methodIsExternallyPlayable.invoke(null, new Object[] {
				torrent
			});
			
			if (retObj instanceof Boolean) {
				return (Boolean) retObj;
			}
		} catch (Throwable e) {
			e.printStackTrace();
			if (e.getMessage() == null
					|| !e.getMessage().toLowerCase().endsWith("only")) {
				Debug.out(e);
			}
		}

		return false;
	}
}
