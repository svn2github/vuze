/*
 * TorrentDownloaderLoggedImpl.java
 *
 * Created on 2. November 2003, 03:26
 */

package org.gudy.azureus2.core3.torrentdownloader.impl;

import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloader;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderCallBackInterface;

/**
 *
 * @author  Tobias Minich
 */
public class TorrentDownloaderLoggedImpl extends TorrentDownloaderImpl implements TorrentDownloader {

    public void notifyListener() {
      super.notifyListener();
      switch(this.getState()) {
          case STATE_INIT:
            org.apache.log4j.Logger.getLogger("azureus2.torrentdownloader").info("Download of '"+this.getFile().getName()+"' queued.");
            break;
          case STATE_START:
            org.apache.log4j.Logger.getLogger("azureus2.torrentdownloader").info("Download of '"+this.getFile().getName()+"' started.");
            break;
          case STATE_FINISHED:
            org.apache.log4j.Logger.getLogger("azureus2.torrentdownloader").info("Download of '"+this.getFile().getName()+"' finished.");
            break;
          case STATE_ERROR:
            org.apache.log4j.Logger.getLogger("azureus2.torrentdownloader").error(this.getError());
          case STATE_DUPLICATE:
            org.apache.log4j.Logger.getLogger("azureus2.torrentdownloader").error("Download of '"+this.getFile().getName()+"' cancelled. File is already queued or downloading.");
      }
  }
    
}
