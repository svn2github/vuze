/*
 * TorrentDownloaderFactory.java
 *
 * Created on 2. November 2003, 03:52
 */

package org.gudy.azureus2.core3.torrentdownloader;

import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.torrentdownloader.impl.TorrentDownloaderImpl;
import org.gudy.azureus2.core3.torrentdownloader.impl.TorrentDownloaderManager;

/**
 *
 * @author  Tobias Minich
 */
public class TorrentDownloaderFactory {
  
  private static TorrentDownloaderImpl getClass(boolean logged) {
    try {
      return (TorrentDownloaderImpl) Class.forName("org.gudy.azureus2.core3.torrentdownloader.impl.TorrentDownloader"+(logged?"Logged":"")+"Impl").newInstance();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
  
  public static TorrentDownloader download(TorrentDownloaderCallBackInterface callback, String url, String fileordir, boolean logged) {
    TorrentDownloaderImpl dl = getClass(logged);
    if (dl!=null)
      dl.init(callback, url, fileordir);
    return dl;
  }
  
  public static TorrentDownloader download(TorrentDownloaderCallBackInterface callback, String url, String fileordir) {
    return download(callback, url, fileordir, false);
  }
  
  public static TorrentDownloader download(TorrentDownloaderCallBackInterface callback, String url, boolean logged) {
    return download(callback, url, null, logged);
  }
  
  public static TorrentDownloader download(TorrentDownloaderCallBackInterface callback, String url) {
      return download(callback, url, null, false);
  }
  
  public static TorrentDownloader download(String url, String fileordir, boolean logged) {
    return download(null, url, fileordir, logged);
  }
  
  public static TorrentDownloader download(String url, String fileordir) {
    return download(null, url, fileordir, false);
  }
  
  public static TorrentDownloader download(String url, boolean logged) {
    return download(null, url, null, logged);
  }
  
  public static TorrentDownloader download(String url) {
    return download(null, url, null, false);
  }
  
  public static void initManager(GlobalManager gm, boolean logged, boolean autostart, String downloaddir) {
    TorrentDownloaderManager.getInstance().init(gm, logged, autostart, downloaddir);
  }
  
  public static void initManager(GlobalManager gm, boolean logged, boolean autostart) {
    TorrentDownloaderManager.getInstance().init(gm, logged, autostart, null);
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
