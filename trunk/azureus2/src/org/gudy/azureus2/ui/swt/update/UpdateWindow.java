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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.plugins.update.Update;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.mainwindow.Application;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;

/**
 * @author Olivier Chalouhi
 *
 */
public class UpdateWindow implements Runnable{
  
  Display display;  
  
  Shell updateWindow;
  Table table;
  StyledText stDescription;
  Button btnOk;
  Button btnCancel;
  
  boolean askingForShow;
  
  private static final int COL_NAME = 0;
  private static final int COL_VERSION = 1;
  private static final int COL_SIZE = 2;
  
  
  public UpdateWindow() {
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
    layout.marginHeight = 5;
    layout.marginWidth = 5;
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
    
    btnCancel = new Button(updateWindow,SWT.PUSH);
    Messages.setLanguageText(btnCancel,"swt.update.window.cancel");
    btnCancel.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event e) {
        updateWindow.setVisible(false);
      }
    });
    
    
    btnOk = new Button(updateWindow,SWT.PUSH);
    Messages.setLanguageText(btnOk,"swt.update.window.ok");
    
    formData = new FormData();
    formData.left = new FormAttachment(0,0);
    formData.right = new FormAttachment(100,0);
    formData.top = new FormAttachment(lHeaderText);
    formData.bottom = new FormAttachment(btnOk);
    sash.setLayoutData(formData);
    
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
        
        updateWindow.open();
      }
    });
  }
  
  public static void main(String args[]) throws Exception{
    Application app = new Application() {
      public void run() {
       new UpdateWindow();
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
}
