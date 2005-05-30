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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Label;

import com.aelitis.azureus.core.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloader;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderCallBackInterface;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderFactory;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.components.BufferedLabel;
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
  BufferedLabel status;
  Button retry;
  Button cancel;  
  TorrentDownloader downloader;
  
  public 
  FileDownloadWindow(
  		AzureusCore		_azureus_core,
  		Display 		display,
		final String 	url,
		final String	referrer ) 
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
    this.shell = org.gudy.azureus2.ui.swt.components.shell.ShellFactory.createShell(display,SWT.CLOSE | SWT.BORDER | SWT.TITLE);
    shell.setText(MessageText.getString("fileDownloadWindow.title"));
    if(! Constants.isOSX) {
      shell.setImage(ImageRepository.getImage("azureus"));
    }
    final FormLayout formLayout = new FormLayout();
    formLayout.marginHeight = 5;
    formLayout.marginWidth = 5;
    formLayout.spacing = 5;
    shell.setLayout(formLayout);    
    FormData data;
    
    
    Label lDownloading = new Label(shell, SWT.NONE);      
    lDownloading.setText(MessageText.getString("fileDownloadWindow.downloading"));
    
    
    Label lLocation = new Label(shell, SWT.NONE);
    data = new FormData();
    data.left = new FormAttachment(lDownloading);
    data.right = new FormAttachment(100,0);
    lLocation.setLayoutData(data);
        
    String shortUrl = url;
    if(url.length() > 70) {
      shortUrl = DisplayFormatters.truncateString(url,120);
    }
    lLocation.setText(shortUrl);
    lLocation.setToolTipText(url);
    
    
    progressBar = new ProgressBar(shell, SWT.NONE);
    progressBar.setMinimum(0);
    progressBar.setMaximum(100);
    progressBar.setSelection(0);
    
    data = new FormData();
    data.top = new FormAttachment(lDownloading);
    data.left = new FormAttachment(0,0);
    data.right = new FormAttachment(100,0);
    data.width = 400;    
    progressBar.setLayoutData(data);
    
    Label lStatus = new Label(shell, SWT.NONE);
    lStatus.setText(MessageText.getString("fileDownloadWindow.status"));
    
    data = new FormData();
    data.top = new FormAttachment(progressBar);
    lStatus.setLayoutData(data);
    
    status = new BufferedLabel(shell, SWT.NONE);    
    
    data = new FormData();
    data.top = new FormAttachment(progressBar);
    data.left = new FormAttachment(lStatus);
    data.right = new FormAttachment(100,0);
    status.setLayoutData(data);    
    
    retry = new Button(shell,SWT.PUSH);
        
    retry.setEnabled(false);
    retry.setText(MessageText.getString("fileDownloadWindow.retry"));
    final String _dirName = dirName;
    retry.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event e) {
        retry.setEnabled(false);
        status.setText("");
        downloader.cancel();       
        downloader = 
        	TorrentDownloaderFactory.create(
        			FileDownloadWindow.this,
					url,
					referrer,
					_dirName);
        downloader.start();
      }
    });        
    
    cancel = new Button(shell, SWT.PUSH);    
    cancel.setText(MessageText.getString("Button.cancel"));
    cancel.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event e) {
        downloader.cancel();
        shell.dispose();
      }
    });
    
    data = new FormData();
    data.top = new FormAttachment(lStatus);
    data.right = new FormAttachment(cancel);
    data.width = 100;        
    retry.setLayoutData(data);
    
    data = new FormData();
    data.top = new FormAttachment(lStatus);
    data.right = new FormAttachment(100,0);
    data.width = 100;
    cancel.setLayoutData(data);
        
    shell.setDefaultButton( retry );
    
	shell.addListener(SWT.Traverse, new Listener() {
 		
		public void handleEvent(Event e) {
			
			if ( e.character == SWT.ESC){
												
			    downloader.cancel();
			    
			    shell.dispose();
			}
		}
	});
	
	Point p = shell.computeSize( SWT.DEFAULT, SWT.DEFAULT );
	
	if ( p.x > 800 ){
		
		p.x = 800;
	}
	
    shell.setSize( p );
    
    Utils.centreWindow( shell );
    
    shell.open();
    
    downloader = TorrentDownloaderFactory.create(this,url,referrer,dirName);
    downloader.start();
  }    
    
  public void TorrentDownloaderEvent(int state, TorrentDownloader inf) {   
    update();    
  }
  
  
  
  private void update() {
    if(display != null && ! display.isDisposed()) {
      display.asyncExec(new AERunnable() {
        public void runSupport() {
          int percentDone = downloader.getPercentDone();
          if(progressBar != null && !progressBar.isDisposed()) {
            progressBar.setSelection(percentDone);
          }
          int state = downloader.getDownloadState();
          String stateText;
          switch(state) {
            case TorrentDownloader.STATE_DOWNLOADING :
              stateText = MessageText.getString("fileDownloadWindow.state_downloading") + ": " + downloader.getStatus();
              break;
            case TorrentDownloader.STATE_ERROR :
              stateText = MessageText.getString("fileDownloadWindow.state_error") + downloader.getError();
              break;
            default :
              stateText = "";
          }
          if(status != null && ! status.isDisposed()) {
            status.setText(DisplayFormatters.truncateString( stateText, 120 ));
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
          		if(! retry.isDisposed())
          			retry.setEnabled(true); 
          	}
                    	
        	Point p = shell.computeSize( SWT.DEFAULT, SWT.DEFAULT );
        	
        	if ( p.x > 800 ){
        		
        		p.x = 800;
        	}
        	
        	if ( !shell.getSize().equals(p)){
        		
	            shell.setSize( p );
	            
	            Utils.centreWindow( shell );
	            
	            shell.layout();
        	}
          }
        }
      });
    }
  }

}
