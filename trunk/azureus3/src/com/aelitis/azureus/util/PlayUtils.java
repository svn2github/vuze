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

import java.lang.reflect.Method;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.utils.FeatureManager;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.download.DownloadManagerEnhancer;
import com.aelitis.azureus.core.download.EnhancedDownloadManager;
import com.aelitis.azureus.core.download.StreamManager;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentV3;

/**
 * @author TuxPaper
 * @created Jun 1, 2008
 *
 */
public class PlayUtils
{
	public static final boolean COMPLETE_PLAY_ONLY = true;
	
	// deprecated but here for older versions of vzemp that still refer to it
	public static final int fileSizeThreshold = 90;
	
		/**
		 * Access to this static is deprecated - use get/setPlayableFileExtensions. For legacy EMP we need
		 * to keep it public for the moment...
		 */
	
	public static final String playableFileExtensions 	= ".avi .flv .flc .mp4 .divx .h264 .mkv .mov .mp2 .m4v .mp3 .aac, .mts, .m2ts";
	
	private static volatile String actualPlayableFileExtensions = playableFileExtensions;
	
	
	private static Boolean hasQuickTime;
	
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

	public static boolean canUseEMP(DiskManagerFileInfo file ){
		return( isExternallyPlayable( file ));
	}
	
	public static boolean canUseEMP(TOTorrent torrent, int file_index) {
		
		return( canUseEMP( torrent, file_index, COMPLETE_PLAY_ONLY ));
	}
	
	public static boolean canUseEMP(TOTorrent torrent, int file_index, boolean complete_only ) {
		if (torrent == null) { 
			return false;
		}

		if (canPlayViaExternalEMP(torrent, file_index, complete_only)) {
			return true;
		}
		
		return false;
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
		
		if ( !( Constants.isWindows || Constants.isOSX )){
			
			return( false );
		}
		
		if (ds == null) {
			return false;
		}
		
		if (ds instanceof org.gudy.azureus2.core3.disk.DiskManagerFileInfo) {
			org.gudy.azureus2.core3.disk.DiskManagerFileInfo fi = (org.gudy.azureus2.core3.disk.DiskManagerFileInfo) ds;
			return canPlayDS(fi.getDownloadManager(), fi.getIndex());
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
	
		// stream stuff
	
	public static boolean
	isStreamPermitted()
	{
		FeatureManager fm = PluginInitializer.getDefaultInterface().getUtilities().getFeatureManager();

		return( fm.isFeatureInstalled( "core" ));
	}
	
	private static boolean 
	canStream(
		DownloadManager 	dm, 
		int 				file_index ) 
	{
		if ( dm == null ){
			
			return( false );
		}
		
		org.gudy.azureus2.core3.disk.DiskManagerFileInfo	file;
		
		if ( file_index == -1 ){
			
			file = dm.getDownloadState().getPrimaryFile();
			if (file == null) {
				org.gudy.azureus2.core3.disk.DiskManagerFileInfo[] files = dm.getDiskManagerFileInfoSet().getFiles();
				if (files.length == 0) {
					return false;
				}
				file = files[0];
			}
			
			file_index = file.getIndex();
			
		}else{
			
			file = dm.getDiskManagerFileInfoSet().getFiles()[ file_index ];
		}
		
		if ( file.getDownloaded() == file.getLength()){
			
			return( false );
		}
		
		if ( !StreamManager.getSingleton().isStreamingUsable()){
			
			return( false );
		}
		
		TOTorrent torrent = dm.getTorrent();
				
		return( canUseEMP( torrent, file_index, false ));
	}
		
	public static boolean 
	canStreamDS(
		Object ds, 
		int file_index ) 
	{
		if ( !( Constants.isWindows || Constants.isOSX )){
			
			return( false );
		}
		
		if ( ds == null ){
			
			return( false );
		}

		if (ds instanceof org.gudy.azureus2.core3.disk.DiskManagerFileInfo) {
			org.gudy.azureus2.core3.disk.DiskManagerFileInfo fi = (org.gudy.azureus2.core3.disk.DiskManagerFileInfo) ds;
			return canStreamDS(fi.getDownloadManager(), fi.getIndex());
		}

		DownloadManager dm = DataSourceUtils.getDM(ds);
		
		if ( dm != null ){
			
			return( canStream( dm, file_index ));
		}
		
		return( false );
	}

	/**
	 * @param dmContent
	 * @return
	 *
	 * @since 3.0.4.3
	 */
	/*
	public static String getContentUrl(DownloadManager dmContent) {
		String contentPath;
		if (dmContent.isDownloadComplete(false)) {
			//use the file path if download is complete.
			org.gudy.azureus2.core3.disk.DiskManagerFileInfo primaryFile = dmContent.getDownloadState().getPrimaryFile();
			if (primaryFile == null) {
				return null;
			}
			File file = primaryFile.getFile(true);
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
	*/
	/**
	 * @param dl
	 *
	 * @since 3.0.2.3
	 */
	/*
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
	*/
	
	public static URL getMediaServerContentURL(DiskManagerFileInfo file) {
		
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
			if ( hasQuickTime == null ){
				
				UIFunctions uif = UIFunctionsManager.getUIFunctions();
				
				if ( uif != null ){
					
					hasQuickTime = uif.isProgramInstalled( ".qtl", "Quicktime" );
	
					try{
						pi.getIPC().invoke("setQuickTimeAvailable", new Object[] { hasQuickTime	});
						
					}catch( Throwable e ){
						
						Logger.log(new LogEvent(LogIDs.UI3, LogEvent.LT_WARNING,
								"IPC to media server plugin failed", e));
					}
				}
			}
	
			Object url = pi.getIPC().invoke("getContentURL", new Object[] { file });
			
			if (url instanceof String) {
				return new URL( (String) url);
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
	

	private static AtomicInteger dm_uid = new AtomicInteger();
	
	private static Map<String,Object[]>	ext_play_cache = 
		new LinkedHashMap<String,Object[]>(100,0.75f,true)
		{
			protected boolean 
			removeEldestEntry(
		   		Map.Entry<String,Object[]> eldest) 
			{
				return size() > 100;
			}
		};	
		
	public static boolean isExternallyPlayable(Download d, int file_index, boolean complete_only ) {
				
		if ( d == null ){
			
			return( false );
		}
		
		boolean use_cache = d.getState() != Download.ST_DOWNLOADING;
		
		String 	cache_key 	= null;
		long	now 		= 0;
		
		if ( use_cache ){
			
			Integer uid = (Integer)d.getUserData( PlayUtils.class );
			
			if ( uid == null ){
				
				uid = dm_uid.getAndIncrement();
				
				d.setUserData( PlayUtils.class, uid );
			}
			
			cache_key = uid+"/"+file_index+"/"+complete_only;
			
			Object[] cached;
			
			synchronized( ext_play_cache ){
				
				cached = ext_play_cache.get( cache_key );
			}
			
			now = SystemTime.getMonotonousTime();
			
			if ( cached != null ){
			
				if ( now - (Long)cached[0] < 60*1000 ){
					
					return((Boolean)cached[1]);
				}
			}
		}
		
		boolean result = isExternallyPlayableSupport(d, file_index, complete_only);
		
		if ( use_cache ){
		
			synchronized( ext_play_cache ){

				ext_play_cache.put( cache_key, new Object[]{ now, result });
			}
		}
		
		return( result );
	}
	
	private static boolean isExternallyPlayableSupport(Download d, int file_index, boolean complete_only ) {
		
		int primary_file_index = -1;

		if ( file_index == -1 ){
			

			DownloadManager dm = PluginCoreUtils.unwrap(d);
			
			if ( dm == null ) {
				
				return( false );
			}
			
			DiskManagerFileInfo file = null;
			try {
				file = PluginCoreUtils.wrap(dm.getDownloadState().getPrimaryFile());
			} catch (DownloadException e) {
				return false;
			}
			
			if ( file == null ){
				
				return( false );
			}
						
			if ( file.getDownloaded() != file.getLength()) {
				
				if ( complete_only || getMediaServerContentURL( file ) == null ){
					
					return( false );
				}
			}
			
			primary_file_index = file.getIndex();

		}else{
			
			DiskManagerFileInfo file = d.getDiskManagerFileInfo( file_index );
			
			if ( file.getDownloaded() != file.getLength()) {
				
				if ( complete_only || getMediaServerContentURL( file ) == null ){
					
					return( false );
				}
			}
			
			primary_file_index = file_index;
		}

		if ( primary_file_index == -1 ){
			
			return false;
		}
		
		return( isExternallyPlayable( d.getDiskManagerFileInfo()[primary_file_index] ));
	}
	
	private static boolean
	isExternallyPlayable(
		DiskManagerFileInfo	file )
	{		
		String	name = file.getFile( true ).getName();
		
		try{
			Download dl = file.getDownload();
			
			if ( dl != null ){
			
				String is = PluginCoreUtils.unwrap( dl ).getDownloadState().getAttribute( DownloadManagerState.AT_INCOMP_FILE_SUFFIX );
				
				if ( is != null && name.endsWith( is )){
					
					name = name.substring( 0, name.length() - is.length()); 
				}
			}
		}catch( Throwable e ){
		}
		
		int extIndex = name.lastIndexOf(".");
		
		if ( extIndex > -1 ){
			
			String ext = name.substring(extIndex);
			
			if ( ext == null ){
				
				return false;
			}
			
			ext = ext.toLowerCase();
			
			if ( getPlayableFileExtensions().indexOf(ext) > -1 ){
				
				return true;
			}
		}
		
		return false;
	}
	
	public static boolean isExternallyPlayable(TOTorrent torrent, int file_index, boolean complete_only ) {
		if (torrent == null) {
			return false;
		}
		try {
			Download download = AzureusCoreFactory.getSingleton().getPluginManager().getDefaultPluginInterface().getDownloadManager().getDownload(torrent.getHash());
			if (download != null) {
				return isExternallyPlayable(download, file_index, complete_only);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	private static final boolean canPlayViaExternalEMP(TOTorrent torrent, int file_index, boolean complete_only ) {
		if (torrent == null) {
			return false;
		}

		return isExternallyPlayable(torrent, file_index, complete_only );
	}

	public static String
	getPlayableFileExtensions()
	{
		return( actualPlayableFileExtensions );
	}
	
		/**
		 * This method available for player plugins to extend playable set if needed
		 * @param str
		 */
	
	public static void
	setPlayableFileExtensions(
		String	str )
	{
		actualPlayableFileExtensions = str;
	}
	

	/**
	 * @deprecated but still used by EMP
	 */
	public static int getPrimaryFileIndex(Download dl) {
		EnhancedDownloadManager edm = DownloadManagerEnhancer.getSingleton().getEnhancedDownload( PluginCoreUtils.unwrap(dl) );
		
		if ( edm == null ) {
			
			return -1;
		}

		return edm.getPrimaryFileIndex();
	}
	
	public static boolean
	isEMPAvailable()
	{
		PluginInterface pi = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByID( "azemp");
		
		if ( pi == null || pi.getPluginState().isDisabled()){
			
			return( false );
		}
		
		return( true );
	}

	
	public static boolean 
	playURL( 
		URL url, String name )
	{
		try{
			PluginInterface pi = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByID( "azemp");
			
			if ( pi == null || pi.getPluginState().isDisabled()){
				
				return( false );
			}
		
			Class<?> ewp_class = pi.getPlugin().getClass().getClassLoader().loadClass( "com.azureus.plugins.azemp.ui.swt.emp.EmbeddedPlayerWindowSWT" );
			
			if ( ewp_class != null ){
				
				Method ow = ewp_class.getMethod( "openWindow", URL.class, String.class );
				
				if ( ow != null ){
					
					ow.invoke( null, url, name );
					
					return( true );
				}
			}
			
			return( false );
			
		}catch( Throwable e ){
			
			Debug.out( e);
			
			return( false );
		}
	}
}
