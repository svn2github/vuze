/*
 * Created on 7 mai 2004
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
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.update;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.plugins.update.Update;
import org.gudy.azureus2.plugins.update.UpdateCheckInstance;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderException;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderListener;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.mainwindow.Application;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;

/**
 * @author Olivier Chalouhi
 *
 */
public class UpdateWindow implements Runnable, ResourceDownloaderListener{
  
  private UpdateCheckInstance	check_instance;
	
  Display display;  
  
  Shell updateWindow;
  Table table;
  StyledText stDescription;
  ProgressBar progress;
  Label status;
  Button btnOk;
  Listener lOk;
  Button btnCancel;
  
  
  
  boolean askingForShow;
  
  boolean restartRequired;
  
  private long totalDownloadSize;
  private List downloaders;
  private Iterator iterDownloaders;
  
  private static final int COL_NAME = 0;
  private static final int COL_VERSION = 1;
  private static final int COL_SIZE = 2;
  
  
  public UpdateWindow(
  		UpdateCheckInstance	_check_instance ) 
  {
  	check_instance = _check_instance;
  	
    this.display = SWTThread.getInstance().getDisplay();
    this.updateWindow = null;
    this.askingForShow =false;
    if(display != null && !display.isDisposed())
      display.asyncExec(this);
  }
  
  //The Shell creation process
  public void run() {
    if(display == null || display.isDisposed())
      return;
    
    updateWindow = new Shell(display,(SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL) & ~SWT.CLOSE );
    
    updateWindow.setImage(ImageRepository.getImage("azureus"));
    Messages.setLanguageText(updateWindow,"swt.update.window.title");
    
    FormLayout layout = new FormLayout();
    layout.spacing = 5;
    layout.marginHeight = 10;
    layout.marginWidth = 10;
    FormData formData;
    updateWindow.setLayout(layout);
    
    Label lHeaderText = new Label(updateWindow,SWT.WRAP);
    Messages.setLanguageText(lHeaderText,"swt.update.window.header");
    formData = new FormData();
    formData.left = new FormAttachment(0,0);
    formData.right = new FormAttachment(100,0);
    formData.top = new FormAttachment(0,0);
    lHeaderText.setLayoutData(formData);
    
    SashForm sash = new SashForm(updateWindow,SWT.VERTICAL);
       
    table = new Table(sash,SWT.CHECK | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
    String[] names = {"name" , "version" , "size"};
    int[] sizes = {220,80,80};
    for(int i = 0 ; i < names.length ; i++) {
      TableColumn column = new TableColumn(table,SWT.LEFT);
      Messages.setLanguageText(column,"swt.update.window.columns." + names[i]);
      column.setWidth(sizes[i]);
    }
    table.setHeaderVisible(true);
    
    table.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event e) {
        checkMandatory();
        checkRestartNeeded();
        TableItem[] items = table.getSelection();
        if(items.length == 0) return;
        Update update = (Update) items[0].getData();        
        String[] descriptions = update.getDescription();
        stDescription.setText("");
        for(int i = 0 ; i < descriptions.length ; i++) {
          stDescription.append(descriptions[i] + "\n");
        }
      }
    });
    
    stDescription = new StyledText(sash,SWT.BORDER | SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL);
    
    progress = new ProgressBar(updateWindow,SWT.NULL);
    progress.setMinimum(0);
    progress.setMaximum(100);
    progress.setSelection(0);
    
    status = new Label(updateWindow,SWT.NULL);
    
    btnCancel = new Button(updateWindow,SWT.PUSH);
    Messages.setLanguageText(btnCancel,"swt.update.window.quit");
    btnCancel.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event e) {
        updateWindow.dispose();
        
       	check_instance.cancel();
      }
    });
    
    
    btnOk = new Button(updateWindow,SWT.PUSH);
    Messages.setLanguageText(btnOk,"swt.update.window.ok");
    
    lOk = new Listener() {
      public void handleEvent(Event e) {
        update();
      }
    };
    
    btnOk.addListener(SWT.Selection, lOk);
    
    formData = new FormData();
    formData.left = new FormAttachment(0,0);
    formData.right = new FormAttachment(100,0);
    formData.top = new FormAttachment(lHeaderText);
    formData.bottom = new FormAttachment(progress);
    sash.setLayoutData(formData);
    
    formData = new FormData();
    formData.left = new FormAttachment(0,0);
    formData.right = new FormAttachment(100,0);
    formData.bottom = new FormAttachment(status);
    progress.setLayoutData(formData);
    
    formData = new FormData();
    formData.left = new FormAttachment(0,0);
    formData.right = new FormAttachment(100,0);
    formData.bottom = new FormAttachment(btnOk);
    status.setLayoutData(formData);
    
    formData = new FormData();
    formData.width = 100;
    formData.right = new FormAttachment(100,0);
    formData.bottom = new FormAttachment(100,0);
    btnOk.setLayoutData(formData);
    
    formData = new FormData();
    formData.width = 100;
    formData.right = new FormAttachment(btnOk);
    formData.bottom = new FormAttachment(100,0);
    btnCancel.setLayoutData(formData);
    
    updateWindow.setSize(400,400);
    //updateWindow.open();
  }
  
  public void addUpdate(final Update update) {
    if(display == null || display.isDisposed())
      return;
  
    display.asyncExec(new Runnable() {
      public void run() {
        if(table == null || table.isDisposed())
          return;
        
        final TableItem item = new TableItem(table,SWT.NULL);
        item.setData(update);
        item.setText(COL_NAME,update.getName());  
        item.setText(COL_VERSION,update.getNewVersion());
        ResourceDownloader[] rds = update.getDownloaders();
        long totalLength = 0;
        for(int i = 0 ; i < rds.length ; i++) {
          try {
            totalLength += rds[i].getSize();
          } catch(Exception e) {
          }
        }                
        
        item.setText(COL_SIZE,DisplayFormatters.formatByteCountToBase10KBEtc(totalLength));                
        
        item.setChecked(true);
        
        checkRestartNeeded();
        
        updateWindow.open();
      }
    });
  }
  
  public static void main(String args[]) throws Exception{
    Application app = new Application() {
      public void run() {
       new UpdateWindow(null);
      }
      
      public void stopIt() {
       
      }
    };
    SWTThread.createInstance(app);    
  }
  
  private void checkMandatory() {
    TableItem[] items = table.getItems();
    for(int i = 0 ; i < items.length ; i++) {
      Update update = (Update) items[i].getData();
      if(update.isMandatory()) items[i].setChecked(true);
    }
  }
  
  private void checkRestartNeeded() {  
    restartRequired = false;
    TableItem[] items = table.getItems();
    for(int i = 0 ; i < items.length ; i++) {
      if(! items[i].getChecked()) continue;
      Update update = (Update) items[i].getData();
      int required = update.getRestartRequired();
      if((required == Update.RESTART_REQUIRED_MAYBE) ||
         (required == Update.RESTART_REQUIRED_YES)     ) restartRequired = true;
    }
    if(restartRequired) {
      status.setText(MessageText.getString("swt.update.window.status.restartNeeded"));
    } else {
      status.setText("");
    }
  }
  
  private void update() {
    btnOk.setEnabled(false);    
    Messages.setLanguageText(btnCancel,"swt.update.window.cancel");
    table.setEnabled(false);
    stDescription.setText("");
    TableItem[] items = table.getItems();
    
    totalDownloadSize = 0;   
    downloaders = new ArrayList();
    
    for(int i = 0 ; i < items.length ; i++) {
      if(! items[i].getChecked()) continue;
      
      Update update = (Update) items[i].getData();
      ResourceDownloader[] rds = update.getDownloaders();
      for(int j = 0 ; j < rds.length ; j++) {
        downloaders.add(rds[j]);        
        try {
          totalDownloadSize += rds[j].getSize();
        } catch (Exception e) {
          stDescription.append(MessageText.getString("swt.update.window.no_size") + rds[j].getName() +"\n");
        }        
      }
    }
    downloadersToData = new HashMap();
    iterDownloaders = downloaders.iterator();
    nextUpdate();
  }
  
  private void nextUpdate() {
    if(iterDownloaders.hasNext()) {
      ResourceDownloader downloader = (ResourceDownloader) iterDownloaders.next();
      downloader.addListener(this);
      downloader.asyncDownload();
    } else {
      switchToRestart();      
    }
  }
  
  private void switchToRestart() {
    if(display == null || display.isDisposed())
      return;
    
    display.asyncExec(new Runnable() {
      public void run() {
        progress.setSelection(100);
        status.setText(MessageText.getString("swt.update.window.status.done"));
        btnOk.removeListener(SWT.Selection,lOk);
        btnOk.setEnabled(true);
        btnOk.addListener(SWT.Selection,new Listener() {
          public void handleEvent(Event e) {
            finishUpdate();
          }
        });
        if(restartRequired) {
          Messages.setLanguageText(btnOk,"swt.update.window.restart");
        } else {
          Messages.setLanguageText(btnOk,"swt.update.window.close");
        }
        btnCancel.setEnabled(false);
          }
    });
  }
  
  public void reportPercentComplete(ResourceDownloader downloader,
      int percentage) {
    setProgressSelection(percentage);   
  }
  
  private void setProgressSelection(final int percent) {
    if(display == null || display.isDisposed())
      return;
    
    display.asyncExec(new Runnable() {
      public void run() {
        if(progress != null && !progress.isDisposed())
        progress.setSelection(percent);
      }
    });
  }
  
  private Map downloadersToData;
  
  public boolean completed(ResourceDownloader downloader, InputStream data) {
    downloadersToData.put(downloader,data);
    downloader.removeListener(this);
    setProgressSelection(0);
    nextUpdate();
    return true;
  }
  
  public void failed(ResourceDownloader downloader,
      ResourceDownloaderException e) {
    downloader.removeListener(this);
    setStatusText(MessageText.getString("swt.update.window.status.failed"));
    appendDetails(downloader.getName() + " : " + e);
  }
  
  public void reportActivity(ResourceDownloader downloader, final String activity) {
    setStatusText(activity);
    appendDetails(activity);
  }
  
  private void setStatusText(final String text) {
    if(display == null || display.isDisposed())
      return;
    
    display.asyncExec(new Runnable() {
      public void run() {
        if(status != null && !status.isDisposed())
          status.setText(text);
      }
    });  
  }
  
  private void appendDetails(final String text) {
    if(display == null || display.isDisposed())
      return;
    
    display.asyncExec(new Runnable() {
      public void run() {
        if(stDescription != null && !stDescription.isDisposed())
          stDescription.append(text + "\n");
      }
    });  
  }
  
  
  private void finishUpdate() {
    //If restart is required, then restart
    if(restartRequired) {
    	// this HAS to be done this way around else the restart inherits
    	// the 6880 port listen. However, this is a general problem....
      MainWindow.getWindow().dispose();
      Restarter.restartForUpgrade();
    } else {
      updateWindow.dispose();      
    }
  }
  
  protected boolean
  isDisposed()
  {
  	return( display == null || display.isDisposed());
  }
}
