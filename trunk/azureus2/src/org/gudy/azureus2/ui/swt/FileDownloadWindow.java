/*
 * File    : FileDownloadWindow.java
 * Created : 3 nov. 2003 12:51:53
 * By      : Olivier 
 * 
 * Azureus - a Java Bittorrent client
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
 */
 
package org.gudy.azureus2.ui.swt;

import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Label;

import com.aelitis.azureus.core.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloader;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderCallBackInterface;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderFactory;
import org.gudy.azureus2.ui.swt.mainwindow.*;
import org.eclipse.swt.widgets.Button;

/**
 * @author Olivier
 * 
 */
public class FileDownloadWindow implements TorrentDownloaderCallBackInterface{
  AzureusCore	azureus_core;
  Display display;
  Shell shell;
  ProgressBar progressBar;
  Label status;
  Button retry;
  Button cancel;  
  TorrentDownloader downloader;
  
  public 
  FileDownloadWindow(
  		AzureusCore	_azureus_core,
  		Display 	display,
		final String url) 
  {
  	azureus_core	= _azureus_core;
  	
    String dirName = null;
    if(COConfigurationManager.getBooleanParameter("Save Torrent Files",true)) {
      try {
        dirName = COConfigurationManager.getDirectoryParameter("General_sDefaultTorrent_Directory");
      } catch(Exception egnore) {}
    }
    if(dirName == null) {
      DirectoryDialog dd = new DirectoryDialog(MainWindow.getWindow().getShell(),SWT.NULL);
      dd.setText(MessageText.getString("fileDownloadWindow.saveTorrentIn"));
      dirName = dd.open();
    }
    if(dirName == null) return;
    
    this.display = display;
    this.shell = new Shell(display,SWT.CLOSE | SWT.BORDER | SWT.TITLE);
    final GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 4;
    shell.setLayout(gridLayout);
    shell.setText(MessageText.getString("fileDownloadWindow.title"));
    shell.setImage(ImageRepository.getImage("azureus"));
    
    Label label = new Label(shell, SWT.NONE);      
    label.setText(MessageText.getString("fileDownloadWindow.downloading"));
    GridData gridData = new GridData();
    gridData.horizontalSpan = 2;
    label.setLayoutData(gridData);
    
    label = new Label(shell, SWT.NONE);    
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    label.setLayoutData(gridData);
    String shortUrl = url;
    if(url.length() > 70) {
      shortUrl = url.substring(0,70) + "...";
    }
    label.setText(shortUrl);
    label.setToolTipText(url);
    
    progressBar = new ProgressBar(shell, SWT.NONE);
    progressBar.setMinimum(0);
    progressBar.setMaximum(100);
    progressBar.setSelection(0);
    
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 4;
    gridData.widthHint = 300;
    progressBar.setLayoutData(gridData);
    
    label = new Label(shell, SWT.NONE);
    label.setText(MessageText.getString("fileDownloadWindow.status"));
    
    status = new Label(shell, SWT.NONE);    
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
    gridData.horizontalSpan = 3;
    status.setLayoutData(gridData);
    
    new Label(shell,SWT.NONE);
    new Label(shell,SWT.NONE);
    
    retry = new Button(shell,SWT.NONE);
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.FILL_HORIZONTAL);
    gridData.widthHint = 100;
    retry.setLayoutData(gridData);
    retry.setEnabled(false);
    retry.setText(MessageText.getString("fileDownloadWindow.retry"));
    final String _dirName = dirName;
    retry.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event e) {
        retry.setEnabled(false);
        status.setText("");
        downloader.cancel();       
        downloader = TorrentDownloaderFactory.download(FileDownloadWindow.this,url,_dirName);
        downloader.start();
      }
    });        
    
    cancel = new Button(shell, SWT.NONE);    
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
    gridData.widthHint = 100;
    cancel.setLayoutData(gridData);
    cancel.setText(MessageText.getString("Button.cancel"));
    cancel.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event e) {
        downloader.cancel();
        shell.dispose();
      }
    });
    shell.pack();
    shell.open();
    downloader = TorrentDownloaderFactory.download(this,url,dirName);
    downloader.start();
  }    
    
  public void TorrentDownloaderEvent(int state, TorrentDownloader inf) {   
    update();    
  }
  
  
  
  private void update() {
    if(display != null && ! display.isDisposed()) {
      display.asyncExec(new Runnable() {
        public void run() {
          int percentDone = downloader.getPercentDone();
          if(progressBar != null && !progressBar.isDisposed()) {
            progressBar.setSelection(percentDone);
          }
          int state = downloader.getDownloadState();
          String stateText;
          switch(state) {
            case TorrentDownloader.STATE_DOWNLOADING :
              stateText = MessageText.getString("fileDownloadWindow.state_downloading");
              break;
            case TorrentDownloader.STATE_ERROR :
              stateText = MessageText.getString("fileDownloadWindow.state_error") + downloader.getError();
              break;
            default :
              stateText = "";
          }
          if(status != null && ! status.isDisposed()) {
            status.setText(stateText);
            status.setToolTipText(stateText);
          }
          
          if(state == TorrentDownloader.STATE_FINISHED) {
            //If the Shell has been disposed, then don't process the torrent.
            if(shell != null && ! shell.isDisposed()) {
              shell.dispose();
              TorrentOpener.openTorrent(azureus_core, downloader.getFile().getAbsolutePath());
            }
          }
   
          if ( !shell.isDisposed()){
          	
          	if(state == TorrentDownloader.STATE_ERROR) {
          		retry.setEnabled(true); 
          	}
          
          	shell.pack();
          }
        }
      });
    }
  }

}
