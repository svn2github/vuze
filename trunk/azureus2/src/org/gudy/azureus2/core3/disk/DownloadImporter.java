/*
 * Created on Apr 13, 2004
 * Created by Alon Rohter
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 * 
 */

package org.gudy.azureus2.core3.disk;

import org.gudy.azureus2.core3.torrent.TOTorrent;

import java.io.IOException;
import java.util.*;


/**
 * Imports torrents downloaded from other clients and converts
 * them to Azureus-friendly format.
 */
public class DownloadImporter {
  
  
  public void importDownload(TOTorrent torrent, List files,  ImportListener listener) throws IOException {
    
  }
  
  
  public interface ImportListener {
    public void update( float percent_complete );
  }
  
  
}
