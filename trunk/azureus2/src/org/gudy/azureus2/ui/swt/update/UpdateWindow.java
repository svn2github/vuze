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
 * 8 Alle Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.update;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.plugins.update.Update;
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
    
    updateWindow = new Shell(display,SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
    
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
       
    table = new Table(sash,SWT.BORDER);
    String[] names = {"name" , "version" , "size"};
    int[] sizes = {220,80,80};
    for(int i = 0 ; i < names.length ; i++) {
      TableColumn column = new TableColumn(table,SWT.LEFT);
      Messages.setLanguageText(column,"swt.update.window.columns." + names[i]);
      column.setWidth(sizes[i]);
    }
    table.setHeaderVisible(true);
    
    stDescription = new StyledText(sash,SWT.BORDER | SWT.READ_ONLY);
    
    btnCancel = new Button(updateWindow,SWT.PUSH);
    Messages.setLanguageText(btnCancel,"swt.update.window.cancel");
    
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
        
        TableItem item = new TableItem(table,SWT.NULL);
        item.setData(update);
        item.setText(0,update.getName());       
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
}
