/*
 * TorrentDownloaderInfo.java
 *
 * Created on 2. November 2003, 01:48
 */

package org.gudy.azureus2.core3.torrentdownloader;

/**
 *
 * @author  Tobias Minich
 */
public interface TorrentDownloader {
    public static final int STATE_NON_INIT = -1;
    public static final int STATE_INIT = 0;
    public static final int STATE_START = 1;
    public static final int STATE_DOWNLOADING = 2;
    public static final int STATE_FINISHED = 3;
    public static final int STATE_ERROR = 4;
    public static final int STATE_DUPLICATE = 5;
    
    //public void init(TorrentDownloaderCallBackInterface _iface, String _url, String _file);
    public void start();
    public int getState();
    public java.io.File getFile();
    public int getPercentDone();
    public String getError();
    public String getURL();
}
