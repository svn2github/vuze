/*
 * TorrentDownloaderCallBackInterface.java
 *
 * Created on 2. November 2003, 01:32
 */

package org.gudy.azureus2.core3.torrentdownloader;

/**
 *
 * @author  Tobias Minich
 */
public interface TorrentDownloaderCallBackInterface {
    public void TorrentDownloaderEvent(int state, TorrentDownloader inf);
}
