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
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.internat.LocaleUtil;
import org.gudy.azureus2.core3.internat.LocaleUtilDecoder;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.Semaphore;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.ui.swt.FileDownloadWindow;
import org.gudy.azureus2.ui.swt.OpenTorrentWindow;
import org.gudy.azureus2.ui.swt.OpenUrlWindow;
import org.gudy.azureus2.ui.swt.URLTransfer;
import org.gudy.azureus2.ui.swt.sharing.ShareUtils;

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
  
  
  public static void openTorrent(final String fileName) {
    openTorrent(fileName, false, false);
  }

  public static void 
  openTorrent(
      final String  fileName, 
    final boolean   startInStoppedState,
    boolean     from_drag_and_drop ) {
    try {
      if (!FileUtil.isTorrentFile(fileName)){
        
        if ( from_drag_and_drop ){
          
          LGLogger.log( "MainWindow::openTorrent: file it not a torrent file, sharing" );

          ShareUtils.shareFile( fileName );
        
          return;
        }
      }
    } catch (Exception e) {
      
      LGLogger.log( "MainWindow::openTorrent: check fails", e );

      return;
    }

    if(display != null && ! display.isDisposed())
      display.asyncExec(new Runnable() {
        public void run() {
          if(!COConfigurationManager.getBooleanParameter("Add URL Silently", false))
            mainWindow.setActive();
          
          new Thread() {
            public void run() {
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
  
                globalManager.addDownloadManager(fileName, savePath, 
                                                 startInStoppedState ? DownloadManager.STATE_STOPPED 
                                                                     : DownloadManager.STATE_WAITING);
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
      
  final String[] savePath = {""};

    boolean useDefDataDir = COConfigurationManager.getBooleanParameter("Use default data dir");
    
    if (useDefDataDir) savePath[0] = COConfigurationManager.getStringParameter("Default save path", "");
    
    if (savePath[0].length() == 0 || ! useDefault) {

      boolean singleFile = false;
      
      String singleFileName = ""; //$NON-NLS-1$

      try {
        TOTorrent torrent = TorrentUtils.readFromFile(fileName);
        
        singleFile = torrent.isSimpleTorrent();
        
        LocaleUtilDecoder locale_decoder = LocaleUtil.getTorrentEncoding( torrent );
            
        singleFileName = locale_decoder.decodeString(torrent.getName());
        
        singleFileName = FileUtil.convertOSSpecificChars( singleFileName );
      }
      catch (Exception e) {
        e.printStackTrace();
      }

    
    final boolean f_singleFile    = singleFile;
    final boolean f_forSeeding = forSeeding;
    final String  f_singleFileName  = singleFileName;

    final Semaphore sem = new Semaphore();
    
    display.asyncExec(new Runnable() {
       public void run()
       {
          try{
            if (f_singleFile) {
              int style = (f_forSeeding) ? SWT.OPEN : SWT.SAVE;
              FileDialog fDialog = new FileDialog(mainWindow, SWT.SYSTEM_MODAL | style);
              String path = COConfigurationManager.getStringParameter("Default Path");
              if( path != null && path.length() > 0 ) {
                fDialog.setFilterPath( path );
              }
              else {
                fDialog.setFilterPath( COConfigurationManager.getStringParameter("Default save path", "") );
              }
              fDialog.setFileName(f_singleFileName);
              fDialog.setText(MessageText.getString("MainWindow.dialog.choose.savepath") + " (" + f_singleFileName + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
              savePath[0] = fDialog.open();
            }
            else {
              DirectoryDialog dDialog = new DirectoryDialog(mainWindow, SWT.SYSTEM_MODAL);
              String path = COConfigurationManager.getStringParameter("Default Path");
              if( path != null && path.length() > 0 ) {
                dDialog.setFilterPath( path );
              }
              else {
                dDialog.setFilterPath( COConfigurationManager.getStringParameter("Default save path", "") );
              }
              dDialog.setText(MessageText.getString("MainWindow.dialog.choose.savepath") + " (" + f_singleFileName + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
              savePath[0] = dDialog.open();
            }
            if (savePath[0] != null){
                          
              COConfigurationManager.setParameter("Default Path", savePath[0]); //$NON-NLS-1$
              
              COConfigurationManager.save();
            }
          }finally{
            sem.release();
          }
       }
    });
 
    
      sem.reserve();
    }
    
    return savePath[0];
  }

  public static void openTorrents(final String path, final String fileNames[]) {
    openTorrents(path,fileNames,true);
  }

  public static void openTorrentsForSeeding(final String path, final String fileNames[]) {
    openTorrents(path,fileNames,false,true);
  }
  
  public static void openTorrents(
    final String path, 
    final String fileNames[],
    final boolean useDefault )
  {
    openTorrents(path,fileNames,useDefault,false);
  }

  public static void openTorrents(
    final String path, 
    final String fileNames[],
    final boolean useDefault,
    final boolean forSeeding )
  {
  display.asyncExec(new Runnable() {
     public void run()
     {
      mainWindow.setActive();

      new Thread(){
          public void run() {
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
              globalManager.addDownloadManager(path + separator + fileNames[i], savePath);
            }
          }
        }.start();
     }
  });
  }

  public static void openTorrentsFromDirectory(String directoryName) {
    openTorrentsFromDirectory(directoryName, false);
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
    
    String path = COConfigurationManager.getStringParameter("Default Path");
    if( path != null && path.length() > 0 ) {
      dDialog.setFilterPath( path );
    }
    else {
      dDialog.setFilterPath( COConfigurationManager.getStringParameter("General_sDefaultTorrent_Directory", "") );
    }

    dDialog.setText(MessageText.getString("MainWindow.dialog.choose.savepath_forallfiles")); //$NON-NLS-1$
    final String savePath = dDialog.open();
    if (savePath == null)
      return;

    COConfigurationManager.setParameter("Default Path", savePath);
    COConfigurationManager.save();

    new Thread() {
      public void run() {
        for (int i = 0; i < files.length; i++)
          globalManager.addDownloadManager(files[i].getAbsolutePath(), savePath, 
                                           startInStoppedState ? DownloadManager.STATE_STOPPED 
                                                               : DownloadManager.STATE_QUEUED);
      }
    }
    .start();
  }


  public static void openDirectory() {
    DirectoryDialog fDialog = new DirectoryDialog(mainWindow, SWT.NULL);
    
    if ( COConfigurationManager.getBooleanParameter( "Save Torrent Files", true )){
      
      String	default_path = COConfigurationManager.getStringParameter( "General_sDefaultTorrent_Directory", "" );
      
      if ( default_path.length() > 0 ){
        
        fDialog.setFilterPath(default_path);
      }
    }
  
    fDialog.setText(MessageText.getString("MainWindow.dialog.choose.folder")); //$NON-NLS-1$
    String fileName = fDialog.open();
    if (fileName == null)
      return;
    COConfigurationManager.setParameter("Default Path", fileName);
    COConfigurationManager.save();
    TorrentOpener.openTorrentsFromDirectory(fileName);
  }


  public static void openTorrent() {
    FileDialog fDialog = new FileDialog(mainWindow, SWT.OPEN | SWT.MULTI);
    String path = COConfigurationManager.getStringParameter("Default Path");
    if( path != null && path.length() > 0 ) {
      fDialog.setFilterPath( path );
    }
    else {
      fDialog.setFilterPath( COConfigurationManager.getStringParameter("General_sDefaultTorrent_Directory", "") );
    }
    fDialog.setFilterExtensions(new String[] { "*.torrent", "*.tor" }); //$NON-NLS-1$
    fDialog.setFilterNames(new String[] { "*.torrent", "*.tor" }); //$NON-NLS-1$
    fDialog.setText(MessageText.getString("MainWindow.dialog.choose.file")); //$NON-NLS-1$
    String fileName = fDialog.open();
    if (fileName == null)
      return;
    COConfigurationManager.setParameter("Default Path", fileName);
    COConfigurationManager.save();
    TorrentOpener.openTorrents(fDialog.getFilterPath(), fDialog.getFileNames());
  }


  public static void openTorrentNoDefaultSave(boolean forSeeding) {
    FileDialog fDialog = new FileDialog(mainWindow, SWT.OPEN | SWT.MULTI);
    String path = COConfigurationManager.getStringParameter("Default Path");
    if( path != null && path.length() > 0 ) {
      fDialog.setFilterPath( path );
    }
    else {
      fDialog.setFilterPath( COConfigurationManager.getStringParameter("General_sDefaultTorrent_Directory", "") );
    }
    fDialog.setFilterExtensions(new String[] { "*.torrent", "*.tor" }); //$NON-NLS-1$
    fDialog.setFilterNames(new String[] { "*.torrent", "*.tor" }); //$NON-NLS-1$
    fDialog.setText(MessageText.getString("MainWindow.dialog.choose.file")); //$NON-NLS-1$
    String fileName = fDialog.open();
    if (fileName == null)
      return;
    COConfigurationManager.setParameter("Default Path", fileName);
    COConfigurationManager.save();
    TorrentOpener.openTorrents(fDialog.getFilterPath(), fDialog.getFileNames(),false,forSeeding);
  }
  
  public static void openTorrentWindow() {
    new OpenTorrentWindow(display, globalManager);
  }


  public static void openUrl() {
    openUrl(null);
  }


  public static void openUrl(String linkURL) {
    if(linkURL != null && linkURL.length() > 20 && COConfigurationManager.getBooleanParameter("Add URL Silently", false))
      new FileDownloadWindow(display, linkURL);
    else
      new OpenUrlWindow(display, linkURL);
  }
  
  public static void openDroppedTorrents(DropTargetEvent event) {
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
          TorrentOpener.openTorrent(source.getAbsolutePath(), startInStoppedState, true );
        else if (source.isDirectory()){
          
          String  dir_name = source.getAbsolutePath();
          
          String  drop_action = COConfigurationManager.getStringParameter("config.style.dropdiraction", "0");
        
          if ( drop_action.equals("1")){
            ShareUtils.shareDir(dir_name);
          }else if ( drop_action.equals("2")){
            ShareUtils.shareDirContents( dir_name, false );
          }else if ( drop_action.equals("3")){
            ShareUtils.shareDirContents( dir_name, true );
          }else{
            TorrentOpener.openTorrentsFromDirectory(dir_name, startInStoppedState);
          }
        }
      }
    } else {
      TorrentOpener.openUrl(((URLTransfer.URLType)event.data).linkURL);
    }
  }
}
