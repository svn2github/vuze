/*
 * TorrentDownloaderFactory.java
 *
 * Created on 2. November 2003, 03:52
 */

package org.gudy.azureus2.core3.torrentdownloader;

import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.torrentdownloader.impl.TorrentDownloaderImpl;
import org.gudy.azureus2.core3.torrentdownloader.impl.TorrentDownloaderManager;
import org.gudy.azureus2.core3.util.Debug;

/**
 *
 * @author  Tobias Minich
 */
public class TorrentDownloaderFactory {
  
  private static TorrentDownloaderImpl getClass(boolean logged) {
    try {
      return (TorrentDownloaderImpl) Class.forName("org.gudy.azureus2.core3.torrentdownloader.impl.TorrentDownloader"+(logged?"Logged":"")+"Impl").newInstance();
    } catch (Exception e) {
    	Debug.printStackTrace( e );
      return null;
    }
  }
  
  /**
   * creates and initializes a TorrentDownloader object with the specified parameters.
   * NOTE: this does not actually start the TorrentDownloader object
   * @param callback object to notify about torrent download status
   * @param url url of torrent file to download
   * @param referrer url of referrer to set as HTTP_REFERER header when requesting torrent
   * @param fileordir path to a file or directory that the actual 
   *        torrent file should be saved to. if a default save directory is not specified, this will be used instead.
   * 		even if a default save directory is specified, if this parameter path refers to a file, the filename will
   * 		be used when saving the torrent  
   * @param whether or not logging is enabled for the torrent download. this is performed through the TorrentDownloaderLoggedImpl class which is only available in the uis project
   * @return
   */
  public static TorrentDownloader 
  create(
  	TorrentDownloaderCallBackInterface 	callback, 
	String 								url,
	String								referrer,
	String 								fileordir, 
	boolean 							logged) 
  {
    TorrentDownloaderImpl dl = getClass(logged);
    if (dl!=null)
      dl.init(callback, url, referrer, fileordir);
    return dl;
  }
  
  public static TorrentDownloader 
  create(
  		TorrentDownloaderCallBackInterface 	callback, 
		String 								url,
		String								referrer,
		String 								fileordir) 
  {
    return create(callback, url, referrer, fileordir, false);
  }
  
  public static TorrentDownloader create(TorrentDownloaderCallBackInterface callback, String url, boolean logged) {
    return create(callback, url, null, null, logged);
  }
  
  public static TorrentDownloader create(TorrentDownloaderCallBackInterface callback, String url) {
      return create(callback, url, null, null, false);
  }
  
  public static TorrentDownloader create(String url, String fileordir, boolean logged) {
    return create(null, url, null, fileordir, logged);
  }
  
  public static TorrentDownloader create(String url, String fileordir) {
    return create(null, url, null, fileordir, false);
  }
  
  public static TorrentDownloader create(String url, boolean logged) {
    return create(null, url, null, null, logged);
  }
  
  public static TorrentDownloader create(String url) {
    return create(null, url, null, null, false);
  }
  
  public static void initManager(GlobalManager gm, boolean logged, boolean autostart, String downloaddir) {
    TorrentDownloaderManager.getInstance().init(gm, logged, autostart, downloaddir);
  }
    
  public static TorrentDownloader downloadManaged(String url, String fileordir, boolean logged) {
    return TorrentDownloaderManager.getInstance().download(url, fileordir, logged);
  }
  
  public static TorrentDownloader downloadManaged(String url, String fileordir) {
    return TorrentDownloaderManager.getInstance().download(url, fileordir);
  }
  
  public static TorrentDownloader downloadManaged(String url, boolean logged) {
    return TorrentDownloaderManager.getInstance().download(url, logged);
  }
  
  public static TorrentDownloader downloadManaged(String url) {
    return TorrentDownloaderManager.getInstance().download(url);
  }
}
