/*
 * Created on 3 mai 2004
 * Created by Olivier Chalouhi
 * 
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Alle Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.mainwindow;

import java.io.File;
import java.io.FileFilter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import com.aelitis.azureus.core.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.internat.LocaleUtil;
import org.gudy.azureus2.core3.internat.LocaleUtilDecoder;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.ui.swt.FileDownloadWindow;
import org.gudy.azureus2.ui.swt.OpenTorrentWindow;
import org.gudy.azureus2.ui.swt.OpenUrlWindow;
import org.gudy.azureus2.ui.swt.URLTransfer;
import org.gudy.azureus2.ui.swt.sharing.ShareUtils;
import org.gudy.azureus2.core3.util.AEThread;

/**
 * @author Olivier Chalouhi
 *
 */
public class TorrentOpener {
  
  private static Display display;
  private static Shell mainWindow;
  private static GlobalManager globalManager;
  
  public static void init(Shell _mainWindow,GlobalManager gm) {
    display = SWTThread.getInstance().getDisplay();
    mainWindow = _mainWindow;
    globalManager = gm;
  }
  
  
  public static void 
  openTorrent(
  	AzureusCore		azureus_core,
	String 			fileName) 
  {
  	boolean	default_start_stopped = COConfigurationManager.getBooleanParameter( "Default Start Torrents Stopped" );
  	
    openTorrent(azureus_core,fileName, default_start_stopped, false);
  }

  public static void 
  openTorrent(
  	  final AzureusCore	azureus_core,
      final String  fileName, 
	  final boolean   startInStoppedState,
	  boolean     from_drag_and_drop ) 
  {
    //catch a http url
    if( fileName.toUpperCase().startsWith( "HTTP://" ) ) {
      AERunnable r = new AERunnable() {
        public void runSupport() {
          openUrl( azureus_core, fileName );
        }
      };
      display.asyncExec( r );
      return;
    }
    
    try {
      if (!FileUtil.isTorrentFile(fileName)){
        
        if ( from_drag_and_drop ){
          
          LGLogger.log( "MainWindow::openTorrent: file it not a torrent file, sharing" );

          ShareUtils.shareFile( azureus_core, fileName );
        
          return;
        }
      }
    } catch (Exception e) {
      
      LGLogger.log( "MainWindow::openTorrent: check fails", e );

      return;
    }

    if(display != null && ! display.isDisposed())
      display.asyncExec(new AERunnable(){
        public void runSupport() {
          if(!COConfigurationManager.getBooleanParameter("Add URL Silently", false))
            mainWindow.setActive();
          
          new AEThread("TorrentOpener::openTorrent") {
            public void runSupport() {
              try{
                String savePath = getSavePath(fileName);
                if (savePath == null){
                  LGLogger.log( "MainWindow::openTorrent: save path not set, aborting" );
  
                  return;
                }
                // set to STATE_WAITING if we don't want to startInStoppedState
                // so that auto-open details will work (even if the torrent
                // immediately goes to queued)
                
                LGLogger.log( "MainWindow::openTorrent: adding download '" + fileName + "' --> '" + savePath + "'" );
 
                try{
	                globalManager.addDownloadManager(fileName, savePath, 
	                                                 startInStoppedState ? DownloadManager.STATE_STOPPED 
	                                                                     : DownloadManager.STATE_WAITING);
                }catch( Throwable e ){
    	          	
    	          	LGLogger.logAlert("Torrent open fails for '" + fileName + "'", e );
    	        }
	          }catch( Throwable e ){
                
               LGLogger.log( "MainWindow::openTorrent: torrent addition fails", e );

              }
            }
          }
          .start();
        }
      });
  }
  
  public static String getSavePath(String fileName) {
    return getSavePathSupport(fileName,true,false);
  }
  
  protected static String 
  getSavePathSupport(
    String fileName,
    boolean useDefault,
    boolean forSeeding) 
  {
      // This *musn't* run on the swt thread as the torrent decoder stuff can need to 
      // show a window...
    boolean has_default = COConfigurationManager.getBooleanParameter("Use default data dir");
    final String[] default_dir = { COConfigurationManager.getStringParameter( "Default save path" ) };
    if( default_dir[0] == null || default_dir[0].length() == 0 ) has_default = false;
      
    if ( !useDefault || !has_default ) {

      boolean singleFile = false;
      
      String singleFileName = ""; //$NON-NLS-1$

      try {
        TOTorrent torrent = TorrentUtils.readFromFile(fileName);
        
        singleFile = torrent.isSimpleTorrent();
        
        LocaleUtilDecoder locale_decoder = LocaleUtil.getSingleton().getTorrentEncoding( torrent );
            
        singleFileName = locale_decoder.decodeString(torrent.getName());
        
        singleFileName = FileUtil.convertOSSpecificChars( singleFileName );
      }
      catch (Exception e) {
      	Debug.printStackTrace( e );
      }

    
      final boolean f_singleFile    = singleFile;
      final boolean f_forSeeding = forSeeding;
      final String  f_singleFileName  = singleFileName;

      final AESemaphore sem = new AESemaphore("TorrentOpener");
    
      display.asyncExec(new AERunnable() {
        public void runSupport()
        {
          try{
            if (f_singleFile) {
              int style = (f_forSeeding) ? SWT.OPEN : SWT.SAVE;
              FileDialog fDialog = new FileDialog(mainWindow, SWT.SYSTEM_MODAL | style);             
              fDialog.setFilterPath( getFilterPathData() );
              fDialog.setFileName(f_singleFileName);
              fDialog.setText(MessageText.getString("MainWindow.dialog.choose.savepath") + " (" + f_singleFileName + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
              default_dir[0] = setFilterPathData( fDialog.open() );
            }
            else {
              DirectoryDialog dDialog = new DirectoryDialog(mainWindow, SWT.SYSTEM_MODAL);
              dDialog.setFilterPath( getFilterPathData() );
              dDialog.setText(MessageText.getString("MainWindow.dialog.choose.savepath") + " (" + f_singleFileName + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
              default_dir[0] = setFilterPathData( dDialog.open() );
            }
          }finally{
            sem.release();
          }
        }
      });
 
    
      sem.reserve();
    }
    
    return default_dir[0];
  }

  
  protected static void openTorrents(final String path, final String fileNames[]) {
    openTorrents(path,fileNames,true);
  }

  protected static void openTorrentsForSeeding(final String path, final String fileNames[]) {
    openTorrents(path,fileNames,false,true);
  }
  
  protected static void openTorrents(
    final String path, 
    final String fileNames[],
    final boolean useDefault )
  {
    openTorrents(path,fileNames,useDefault,false);
  }

  protected static void openTorrents(
    final String path, 
    final String fileNames[],
    final boolean useDefault,
    final boolean forSeeding )
  {
  display.asyncExec(new AERunnable(){
     public void runSupport()
     {
       mainWindow.setActive();

      new AEThread("TorrentOpener"){
          public void runSupport() {
            boolean	default_start_stopped = COConfigurationManager.getBooleanParameter( "Default Start Torrents Stopped" );

            String separator = System.getProperty("file.separator"); //$NON-NLS-1$
            for (int i = 0; i < fileNames.length; i++) {
              if (!FileUtil.getCanonicalFileName(fileNames[i]).endsWith(".torrent")) {
                if (!FileUtil.getCanonicalFileName(fileNames[i]).endsWith(".tor")) {
                  continue;
                }
              }
              String savePath = getSavePathSupport(path + separator + fileNames[i],useDefault,forSeeding);
              if (savePath == null)
                continue;
              
              try{
	              globalManager.addDownloadManager(
	              				path + separator + fileNames[i], 
								savePath,
								default_start_stopped ? DownloadManager.STATE_STOPPED 
	                                    : DownloadManager.STATE_QUEUED,
								true,
								forSeeding );
	          }catch( Throwable e ){
	          	
   	          	LGLogger.logAlert("Torrent open fails for '" + path + separator + fileNames[i] + "'", e );
	          }
            }
          }
        }.start();
     }
  });
  }

  public static void openTorrentsFromDirectory(String directoryName) {
    boolean	default_start_stopped = COConfigurationManager.getBooleanParameter( "Default Start Torrents Stopped" );
    
    openTorrentsFromDirectory(directoryName, default_start_stopped);
  }

  public static void openTorrentsFromDirectory(String directoryName, final boolean startInStoppedState) {
    File f = new File(directoryName);
    if (!f.isDirectory())
      return;
    final File[] files = f.listFiles(new FileFilter() {
      public boolean accept(File arg0) {
        if (FileUtil.getCanonicalFileName(arg0.getName()).endsWith(".torrent")) //$NON-NLS-1$
          return true;
        if (FileUtil.getCanonicalFileName(arg0.getName()).endsWith(".tor")) //$NON-NLS-1$
          return true;
        return false;
      }
    });
    if (files.length == 0)
      return;

    DirectoryDialog dDialog = new DirectoryDialog(mainWindow, SWT.NULL);
    dDialog.setFilterPath( getFilterPathData() );
    dDialog.setText(MessageText.getString("MainWindow.dialog.choose.savepath_forallfiles")); //$NON-NLS-1$
    final String path = setFilterPathData( dDialog.open() );
    if( path == null ) return;
    
    new AEThread("Torrent Opener") {
      public void runSupport() {
        for (int i = 0; i < files.length; i++)
        	try{
	          globalManager.addDownloadManager(files[i].getAbsolutePath(), path, 
	                                           startInStoppedState ? DownloadManager.STATE_STOPPED 
	                                                               : DownloadManager.STATE_QUEUED);
            }catch( Throwable e ){
	          	
   	          	LGLogger.logAlert("Torrent open fails for '" + files[i].getAbsolutePath() + "'", e );
	        }
      }
    }
    .start();
  }


  public static void openDirectory() {
    DirectoryDialog fDialog = new DirectoryDialog(mainWindow, SWT.NULL);
    fDialog.setFilterPath( getFilterPathTorrent() );
    fDialog.setText(MessageText.getString("MainWindow.dialog.choose.folder")); //$NON-NLS-1$
    String path = setFilterPathTorrent( fDialog.open() );
    if( path == null ) return;
    TorrentOpener.openTorrentsFromDirectory( path );
  }


  public static void openTorrent() {
    FileDialog fDialog = new FileDialog(mainWindow, SWT.OPEN | SWT.MULTI);
    fDialog.setFilterPath( getFilterPathTorrent() );
    fDialog.setFilterExtensions(new String[] { "*.torrent", "*.tor" }); //$NON-NLS-1$
    fDialog.setFilterNames(new String[] { "*.torrent", "*.tor" }); //$NON-NLS-1$
    fDialog.setText(MessageText.getString("MainWindow.dialog.choose.file")); //$NON-NLS-1$
    String path = setFilterPathTorrent( fDialog.open() );
    if( path == null ) return;
    TorrentOpener.openTorrents( path, fDialog.getFileNames() );
  }


  public static void openTorrentNoDefaultSave(boolean forSeeding) {
    FileDialog fDialog = new FileDialog(mainWindow, SWT.OPEN | SWT.MULTI);
    fDialog.setFilterPath( getFilterPathTorrent() );
    fDialog.setFilterExtensions(new String[] { "*.torrent", "*.tor" }); //$NON-NLS-1$
    fDialog.setFilterNames(new String[] { "*.torrent", "*.tor" }); //$NON-NLS-1$
    fDialog.setText(MessageText.getString("MainWindow.dialog.choose.file")); //$NON-NLS-1$
    String path = setFilterPathTorrent( fDialog.open() );
    if( path == null ) return;
    TorrentOpener.openTorrents( path, fDialog.getFileNames(), false, forSeeding );
  }
  
  public static void openTorrentWindow() {
    new OpenTorrentWindow(display, globalManager);
  }


  public static void 
  openUrl(
  	AzureusCore	azureus_core )
  {
    openUrl(azureus_core,null);
  }


  public static void 
  openUrl(
  	AzureusCore	azureus_core,
	String 		linkURL) 
  {
    if(linkURL != null && linkURL.length() > 12 && COConfigurationManager.getBooleanParameter("Add URL Silently", false))
      new FileDownloadWindow(azureus_core,display, linkURL, null );
    else
      new OpenUrlWindow(azureus_core, display, linkURL, null);
  }
  
  public static void 
  openDroppedTorrents(
  	AzureusCore		azureus_core,
  	DropTargetEvent event) 
  {
    if(event.data == null)
      return;
    if(event.data instanceof String[]) {
      final String[] sourceNames = (String[]) event.data;
      if (sourceNames == null)
        event.detail = DND.DROP_NONE;
      if (event.detail == DND.DROP_NONE)
        return;
      boolean startInStoppedState = event.detail == DND.DROP_COPY;
      for (int i = 0;(i < sourceNames.length); i++) {
        final File source = new File(sourceNames[i]);
        if (source.isFile())
          TorrentOpener.openTorrent(azureus_core, source.getAbsolutePath(), startInStoppedState, true );
        else if (source.isDirectory()){
          
          String  dir_name = source.getAbsolutePath();
          
          String  drop_action = COConfigurationManager.getStringParameter("config.style.dropdiraction", "0");
        
          if ( drop_action.equals("1")){
            ShareUtils.shareDir(azureus_core,dir_name);
          }else if ( drop_action.equals("2")){
            ShareUtils.shareDirContents( azureus_core,dir_name, false );
          }else if ( drop_action.equals("3")){
            ShareUtils.shareDirContents( azureus_core,dir_name, true );
          }else{
            TorrentOpener.openTorrentsFromDirectory(dir_name, startInStoppedState);
          }
        }
      }
    } else {
      TorrentOpener.openUrl(azureus_core,((URLTransfer.URLType)event.data).linkURL);
    }
  }
  
  
  public static String getFilterPathData() {
    String before = COConfigurationManager.getStringParameter("previous.filter.dir.data");
    if( before != null && before.length() > 0 ) {
      return before;
    }
    return COConfigurationManager.getStringParameter("Default save path");
  }
  
  public static String getFilterPathTorrent() {
    String before = COConfigurationManager.getStringParameter("previous.filter.dir.torrent");
    if( before != null && before.length() > 0 ) {
      return before;
    }
    return COConfigurationManager.getStringParameter("General_sDefaultTorrent_Directory");
  }
  
  public static String setFilterPathData( String path ) {
    if( path != null && path.length() > 0 ) {
      File test = new File( path );
      if( !test.isDirectory() ) test = test.getParentFile();
      String now = "";
      if( test != null ) now = test.getAbsolutePath();
      String before = COConfigurationManager.getStringParameter("previous.filter.dir.data");
      if( before == null || before.length() == 0 || !before.equals( now ) ) {
        COConfigurationManager.setParameter( "previous.filter.dir.data", now );
        COConfigurationManager.save();
      }
    }
    return path;
  }
  
  public static String setFilterPathTorrent( String path ) {
    if( path != null && path.length() > 0 ) {
      File test = new File( path );
      if( !test.isDirectory() ) test = test.getParentFile();
      String now = "";
      if( test != null ) now = test.getAbsolutePath();
      String before = COConfigurationManager.getStringParameter("previous.filter.dir.torrent");
      if( before == null || before.length() == 0 || !before.equals( now ) ) {
        COConfigurationManager.setParameter( "previous.filter.dir.torrent", now );
        COConfigurationManager.save();
      }
      return now;
    }
    return path;
  }
  
}
