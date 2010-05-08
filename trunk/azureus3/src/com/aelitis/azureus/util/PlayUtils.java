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
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadManagerImpl;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.download.DownloadManagerEnhancer;
import com.aelitis.azureus.core.download.EnhancedDownloadManager;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentV3;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;

/**
 * @author TuxPaper
 * @created Jun 1, 2008
 *
 */
public class PlayUtils
{
	public static final boolean DISABLE_INCOMPLETE_PLAY = true;
	
	public static final int fileSizeThreshold = 90;
	public static final String playableFileExtensions = ".mpg .avi .flv .flc .mp4 .mpeg .divx .h264 .mkv .wmv .mov .mp2 .m2v .m4v .mp3 .ts .mts .aac";
	
	
	private static boolean triedLoadingEmpPluginClass = false;

	private static Method methodIsExternallyPlayable;
	private static Method methodIsExternallyPlayable2;

	private static PluginInterface piEmp;
	
	//private static Method methodIsExternalPlayerInstalled;

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

	public static boolean canUseEMP(TOTorrent torrent, int file_index) {
		if (canPlayViaExternalEMP(torrent, file_index)) {
			return true;
		}
		
		if (!PlatformTorrentUtils.useEMP(torrent)
				|| !PlatformTorrentUtils.embeddedPlayerAvail()) {
			return false;
		}
	
		return canProgressiveOrIsComplete(torrent);
	}

	private static boolean canPlay(DownloadManager dm, int file_index) {
		if (dm == null) {
			return false;
		}
		TOTorrent torrent = dm.getTorrent();
		return canUseEMP(torrent,file_index);
	}

	private static boolean canPlay(TOTorrent torrent, int file_index) {
		if (!PlatformTorrentUtils.isContent(torrent, false)) {
			return false;
		}
	
		if (!AzureusCoreFactory.isCoreRunning()) {
			return false;
		}

		GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
		DownloadManager dm = gm.getDownloadManager(torrent);
	
	
		if (dm != null) {
			return dm.getAssumedComplete() || canUseEMP(torrent, file_index);
		}
		return canUseEMP(torrent, file_index);
	}

	public static boolean canPlayDS(Object ds, int file_index ) {
		if (ds == null) {
			return false;
		}
		
	
		DownloadManager dm = DataSourceUtils.getDM(ds);
		if (dm != null) {
			return canPlay(dm, file_index);
		}
		TOTorrent torrent = DataSourceUtils.getTorrent(ds);
		if (torrent != null) {
			return canPlay(torrent, file_index);
		}
		if (ds instanceof VuzeActivitiesEntry) {
			return ((VuzeActivitiesEntry) ds).isPlayable();
		}
		
		if (ds instanceof SelectedContentV3) {
			SelectedContentV3 sel = (SelectedContentV3) ds;
			return sel.canPlay();
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
		PluginInterface pi = pm.getPluginInterfaceByID("azupnpav", false);
	
		if (pi == null) {
			Logger.log(new LogEvent(LogIDs.UI3, "Media server plugin not found"));
			return null;
		}
	
		if (!pi.getPluginState().isOperational()) {
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
	
	public static String getMediaServerContentURL(DiskManagerFileInfo file) {
		
		//TorrentListViewsUtils.debugDCAD("enter - getMediaServerContentURL");
	
		PluginManager pm = AzureusCoreFactory.getSingleton().getPluginManager();
		PluginInterface pi = pm.getPluginInterfaceByID("azupnpav", false);
	
		if (pi == null) {
			Logger.log(new LogEvent(LogIDs.UI3, "Media server plugin not found"));
			return null;
		}
	
		if (!pi.getPluginState().isOperational()) {
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
					file
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
	
	/*
	private static final boolean isExternalEMPInstalled() {
		if(!loadEmpPluginClass()) {
			return false;
		}
		
		if (methodIsExternalPlayerInstalled == null) {
			return false;
		}
		
		try {

			Object retObj = methodIsExternalPlayerInstalled.invoke(null, new Object[] {});
			
			if (retObj instanceof Boolean) {
				return ((Boolean) retObj).booleanValue();
			}
		} catch (Throwable e) {
			e.printStackTrace();
			if (e.getMessage() == null
					|| !e.getMessage().toLowerCase().endsWith("only")) {
				Debug.out(e);
			}
		}

		return false;
		
	}*/
	
	private static synchronized final boolean loadEmpPluginClass() {
		if (piEmp != null && piEmp.getPluginState().isUnloaded()) {
			piEmp = null;
			triedLoadingEmpPluginClass = false;
			methodIsExternallyPlayable = null;
			methodIsExternallyPlayable2 = null;
		}

		if (!triedLoadingEmpPluginClass) {
			triedLoadingEmpPluginClass = true;

  		try {
  			piEmp = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByID(
  					"azemp");
  
  			if (piEmp == null) {
  
  				return (false);
  			}
  
  			Class empPluginClass = piEmp.getPlugin().getClass();

  			methodIsExternallyPlayable = empPluginClass.getMethod("isExternallyPlayable", new Class[] {
  				TOTorrent.class
  			});
  			
  			try{
  				methodIsExternallyPlayable2 = empPluginClass.getMethod("isExternallyPlayable", new Class[] {
  						TOTorrent.class, int.class
 	  				});
  			}catch( Throwable e ){
  				Logger.log(new LogEvent(LogIDs.UI3, "isExternallyPlayable with file_index not found"));
  			}
 			
  			//methodIsExternalPlayerInstalled = empPluginClass.getMethod("isExternalPlayerInstalled", new Class[] {});
  			
  		} catch (Exception e1) {
  			return false;
  		}
		}
		return true;
	}
	
	public static File getPrimaryFile(Download d) {
		DiskManagerFileInfo info = getPrimaryFileInfo(d);

		if ( info == null ){
			return( null );
		}else{
			return( info.getFile( true ));
		}
	}
	
	public static int getPrimaryFileIndex(DownloadManager dm ){
		return( getPrimaryFileIndex( PluginCoreUtils.wrap( dm )));
	}
	
	public static int getPrimaryFileIndex(Download d) {
		DiskManagerFileInfo info = getPrimaryFileInfo(d);
		
		if ( info == null ){
			return( -1 );
		}else{
			return( info.getIndex());
		}
	}
	
	public static DiskManagerFileInfo getPrimaryFileInfo(Download d) {
		long size = d.getTorrent().getSize();
		DiskManagerFileInfo[] infos = d.getDiskManagerFileInfo();
		for(int i = 0; i < infos.length ; i++) {
			DiskManagerFileInfo info = infos[i];
			if ( info.isSkipped() || info.isDeleted()){
				continue;
			}
			if( info.getLength() > (long)fileSizeThreshold * size / 100l) {
				return info;
			}
		}
		return null;
	}
	
	public static boolean isExternallyPlayable(Download d, int file_index ) {
		
		File primaryFile;

		if ( file_index == -1 ){
			
			DiskManagerFileInfo file = getPrimaryFileInfo( d );
			
			if ( file == null ){
				
				return( false );
			}
						
			if ( file.getDownloaded() != file.getLength()) {
				
				if ( DISABLE_INCOMPLETE_PLAY || getMediaServerContentURL( file ) == null ){
					
					return( false );
				}
			}
			
			primaryFile = getPrimaryFile(d);

		}else{
			
			DiskManagerFileInfo file = d.getDiskManagerFileInfo( file_index );
			
			if ( file.getDownloaded() != file.getLength()) {
				
				if ( DISABLE_INCOMPLETE_PLAY || getMediaServerContentURL( file ) == null ){
					
					return( false );
				}
			}
			
			primaryFile = file.getFile( true );
		}

		if ( primaryFile == null ){
			
			return false;
		}
		
		String name = primaryFile.getName();
		
		if ( name == null ){
			
			return false;
		}
		
		int extIndex = name.lastIndexOf(".");
		
		if ( extIndex > -1 ){
			
			String ext = name.substring(extIndex);
			
			if ( ext == null ){
				
				return false;
			}
			
			ext = ext.toLowerCase();
			
			if ( playableFileExtensions.indexOf(ext) > -1 ){
				
				return true;
			}
		}
		
		return false;
	}
	
	public static boolean isExternallyPlayable(TOTorrent torrent, int file_index ) {
		if (torrent == null) {
			return false;
		}
		try {
			Download download = AzureusCoreFactory.getSingleton().getPluginManager().getDefaultPluginInterface().getDownloadManager().getDownload(torrent.getHash());
			if (download != null) {
				return isExternallyPlayable(download, file_index);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	private static final boolean canPlayViaExternalEMP(TOTorrent torrent, int file_index ) {
		if(!loadEmpPluginClass() || methodIsExternallyPlayable == null) {
			return isExternallyPlayable(torrent, file_index );
		}
		
		//Data is passed to the openWindow via download manager.
		try {

			if ( file_index != -1 && methodIsExternallyPlayable2 != null ){
				
				try{
					Object retObj = methodIsExternallyPlayable2.invoke(null, new Object[] {
							torrent, file_index
						});
				}catch( Throwable e ){
					Logger.log(new LogEvent(LogIDs.UI3, "isExternallyPlayable with file_index failed", e ));
				}
			}
			
			Object retObj = methodIsExternallyPlayable.invoke(null, new Object[] {
				torrent
			});
			
			if (retObj instanceof Boolean) {
				return ((Boolean) retObj).booleanValue();
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
