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
    public static final int STATE_CANCELLED = 6;
    
    //public void init(TorrentDownloaderCallBackInterface _iface, String _url, String _file);
    /**
     * Starts the download.
     */
    public void start();
    /**
     * Cancels the download.
     */
    public void cancel();
    /**
     * Changes the path and filename to download to.
     * You can give <code>null</code> for either to leave it as is.
     * (These are initialized to either the path/filename given via
     * <code>TorrentDownloaderFactory.download(Managed)</code> or to
     * the default torrent save directory (path) and the filename the 
     * server proposes (file).
     * This function does nothing after the download has been started.
     *
     * @param path The path for download.
     * @param file The file name for download.
     */
    public void setDownloadPath(String path, String file);
    /**
     * Gets the state of the TorrentDownloader.
     */
    public int getState();
    /**
     * Returns the <code>File</code> the TorrentDownloader downloads to.
     */
    public java.io.File getFile();
    /**
     * Returns the amount downloaded in per cent. Gives -1 if total size is not available.
     */
    public int getPercentDone();
	/**
	 * Returns the amount downloaded in bytes.
	 */
	public int getTotalRead();
    /**
     * Returns the error string if one occured, "Ok" otherwise.
     */
    public String getError();
    /**
     * Returns the URL downloaded from.
     */
    public String getURL();
}
