/*
 * Created on May 25, 2004
 * Created by Alon Rohter
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
 
package com.aelitis.azureus.core.helpers;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.util.TorrentUtils;

/**
 * Watches a folder for new torrents and imports them.
 * NOTE: Folder-to-watch and other watching params are taken from a global
 *       config option right now, so starting multiple instances of
 *       TorrentFolderWatcher is useless as currently coded.
 */
public class TorrentFolderWatcher {
  private GlobalManager global_manager;
  private boolean running = false;
  private final ArrayList to_delete = new ArrayList();
  
  private FilenameFilter filename_filter = new FilenameFilter() {
    public boolean accept( File dir, String name ) {
      return name.toLowerCase().endsWith(".torrent");
    }
  };
  
  private ParameterListener param_listener = new ParameterListener() {
    public void parameterChanged( String parameterName ) {
      if( COConfigurationManager.getBooleanParameter("Watch Torrent Folder") ) {
        if( !running ) {
          running = true;
          watch_thread.setDaemon( true );
          watch_thread.setPriority( Thread.MIN_PRIORITY );
          watch_thread.start();
        }
      }
      else running = false;
    }
  };
  
  private final Thread watch_thread = new Thread( "FolderWatcher" ) {
    public void run() {
      while( running ) {
        try {
          Thread.sleep( COConfigurationManager.getIntParameter("Watch Torrent Folder Interval") * 60000 );
        }
        catch (Exception e) { e.printStackTrace(); }
        
        importAddedFiles();
      }
    }
  };
  
  
  /**
   * Start a folder watcher, which will auto-import torrents via the given manager.
   * @param global_manager
   */
  public TorrentFolderWatcher( GlobalManager global_manager ) {
    this.global_manager = global_manager;
    
    if( COConfigurationManager.getBooleanParameter("Watch Torrent Folder") ) {
      running = true;
      watch_thread.setDaemon( true );
      watch_thread.setPriority( Thread.MIN_PRIORITY );
      watch_thread.start();
    }
    
    COConfigurationManager.addParameterListener( "Watch Torrent Folder", param_listener );
  }
  
  
  /**
   * Stop and terminate this folder importer watcher.
   */
  public void destroy() {
    running = false;
    global_manager = null;
    COConfigurationManager.removeParameterListener( "Watch Torrent Folder", param_listener );
  }
  
  
  private synchronized void importAddedFiles() {
    
    if( !running ) return;
    
    boolean save_torrents = COConfigurationManager.getBooleanParameter("Save Torrent Files");
    String torrent_save_path = COConfigurationManager.getStringParameter("General_sDefaultTorrent_Directory");
    int start_state = COConfigurationManager.getBooleanParameter("Start Watched Torrents Stopped") ? DownloadManager.STATE_STOPPED : DownloadManager.STATE_QUEUED;
    String folder_path = COConfigurationManager.getStringParameter("Watch Torrent Folder Path");
    String data_save_path = COConfigurationManager.getStringParameter( "Default save path" );
    boolean default_data_dir_enabled = COConfigurationManager.getBooleanParameter("Use default data dir") && data_save_path.length() > 0;
    
    
    File folder = null;
    
    if( folder_path != null && folder_path.length() > 0 ) {
      folder = new File( folder_path );
      if( !folder.exists() ) folder.mkdirs();
      if( !folder.isDirectory() ) {
        LGLogger.log( LGLogger.ERROR, "ERROR: [Watch Torrent Folder Path] does not exist or is not a dir" );
        folder = null;
      }
    }
    
    if( folder == null ) {
      LGLogger.log( LGLogger.ERROR, "ERROR: [Watch Torrent Folder Path] not configured" );
      return;
    }
    
    if( !default_data_dir_enabled ) {
      LGLogger.log( LGLogger.ERROR, "ERROR: [Use default data dir] not enabled" );
      LGLogger.logAlert( LGLogger.ERROR, "'Save to default data dir' [Use default data dir] needs to be enabled for auto-.torrent-import to work." );
      return;
    }
    
    //delete torrents from the previous import run
    for( int i=0; i < to_delete.size(); i++ ) {
      ((File)to_delete.get( i )).delete();
    }
    to_delete.clear();
    
    String[] currentFileList = folder.list( filename_filter );

    for( int i = 0; i < currentFileList.length; i++ ) {
      File file = new File( folder, currentFileList[i] );
      
      boolean already_added = false;
      try { //make sure it hasn't already been added
        if( global_manager.getDownloadManager( TorrentUtils.readFromFile( file )) != null ) {
          already_added = true;
          save_torrents = false;
          LGLogger.log( LGLogger.INFORMATION, "INFO: " + file.getAbsolutePath()+ " is already being downloaded" );
        }
      } catch( Throwable t) {  t.printStackTrace();  }
      
      if( torrent_save_path.equals( folder ) ) save_torrents = false;
      
      if( !save_torrents || torrent_save_path.length() < 1 ) {
        File imported = new File( folder, file.getName() + ".imported" );
        file.renameTo( imported );
        if( !already_added ) global_manager.addDownloadManager( imported.getAbsolutePath(), data_save_path, start_state );
      }
      else {
        global_manager.addDownloadManager( file.getAbsolutePath(), data_save_path, start_state );
        to_delete.add( file );  //add file for deletion, since there will be a saved copy elsewhere
      }
      LGLogger.log( LGLogger.INFORMATION, "Auto-imported " + file.getAbsolutePath() );
    }
  }
  
}