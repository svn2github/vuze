/*
 * Copyright (c) 2000, 2003 IBM Corp.  All rights reserved.
 * This file is made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */
 
/*
 * Table example snippet: place arbitrary controls in a table
 *
 * For a list of all SWT example snippets see
 * http://dev.eclipse.org/viewcvs/index.cgi/%7Echeckout%7E/platform-swt-home/dev.html#snippets
 */

package org.gudy.azureus2.ui.swt.views.tableitems.utils;

import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.common.ImageRepository;

public class EnumeratorEditor {
  
  private Display display;
  private Shell shell;
  private Color blue;
  private Table table;
  
  private String propertiesName;
  private ConfigBasedItemEnumerator enumerator;
  private ITableStructureModificationListener listener;
  
  private boolean mousePressed;
  private TableItem selectedItem;
  Point oldPoint;
  Image oldImage;
  
  /**
   * @return Returns the shell.
   */
  public Shell getShell() {
    return shell;
  }

  public EnumeratorEditor(
    Display _display,
    ConfigBasedItemEnumerator _enumerator,
    ITableStructureModificationListener _listener,
    String _propertiesName
    ) {    
    this.display = _display;
    this.enumerator = _enumerator;
    this.listener = _listener;
    this.propertiesName = _propertiesName;
    
    blue = new Color(display,0,0,128);
    
    shell = new Shell (display,SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
    shell.setImage(ImageRepository.getImage("azureus"));
    shell.setText(MessageText.getString("columnChooser.title"));
    
    GridLayout layout = new GridLayout();
    layout.numColumns = 3;         
    shell.setLayout (layout);
    
    GridData gridData;
    
    Label label = new Label(shell,SWT.NULL);
    label.setText(MessageText.getString("columnChooser.move"));
    gridData = new GridData(GridData.FILL_BOTH);
    gridData.horizontalSpan = 3;
    label.setLayoutData(gridData);
    
    table = new Table (shell, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
    gridData = new GridData(GridData.FILL_BOTH);
    gridData.horizontalSpan = 3;
    table.setLayoutData(gridData);
    table.setLinesVisible (true);    
    Font f = table.getFont();
    FontData fd = f.getFontData()[0];
    fd.setHeight(9);
    table.setFont(new Font(display, fd));
    
    Button bOk = new Button(shell,SWT.PUSH);
    bOk.setText(MessageText.getString("columnChooser.ok"));
    gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END | GridData.HORIZONTAL_ALIGN_FILL);
    gridData.grabExcessHorizontalSpace = true;
    gridData.widthHint = 70;
    bOk.setLayoutData(gridData);
    bOk.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event e) {
        saveAndApply();
        close();
      }
    });
    
    Button bCancel = new Button(shell,SWT.PUSH);
    bCancel.setText(MessageText.getString("columnChooser.cancel"));
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
    gridData.grabExcessHorizontalSpace = false;
    gridData.widthHint = 70;
    bCancel.setLayoutData(gridData);    
    bCancel.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event e) {
        close();
      }
    });
    
    Button bApply = new Button(shell,SWT.PUSH);
    bApply.setText(MessageText.getString("columnChooser.apply"));
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
    gridData.grabExcessHorizontalSpace = false;
    gridData.widthHint = 70;
    bApply.setLayoutData(gridData);
    bApply.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event e) {
        saveAndApply();
      }
    });
    
    
    for (int i=0; i<2; i++) {
      TableColumn column = new TableColumn(table, SWT.NONE);    
    }
    ItemDescriptor[] items = enumerator.getItems();
    for (int i=0; i<items.length; i++) {
      for(int j=0;j<items.length;j++) {
        int position = items[j].getPosition();
        if(position == i)
          createTableRow(-1,items[j].getName(), (items[j].getPosition() != -1));
      }
    }
    for(int j=0;j<items.length;j++) {
      int position = items[j].getPosition();
      if(position == -1)
        createTableRow(-1,items[j].getName(), (items[j].getPosition() != -1));
    }
    //Hack to get a correct width
    table.getColumn(0).setWidth(20);
    table.getColumn(1).setWidth(200);
    
    
    table.addMouseListener(new MouseAdapter() {
      
      public void mouseDown(MouseEvent arg0) {
        mousePressed = true;
        selectedItem = table.getItem(new Point(arg0.x,arg0.y));
      }
      
      public void mouseUp(MouseEvent e) {
        mousePressed = false;
        //1. Restore old image
        if(oldPoint != null && oldImage != null) {
          GC gc = new GC(table);
          gc.drawImage(oldImage,oldPoint.x,oldPoint.y);
          oldImage.dispose();
          oldImage = null;
          oldPoint = null;
        }
        Point p = new Point(e.x,e.y);
        TableItem item = table.getItem(p);
        if(item != null && selectedItem != null) {
          int index = table.indexOf(item);
          int oldIndex = table.indexOf(selectedItem);
          if(index == oldIndex)
            return;
          if(index > oldIndex)
            index++;
          String name = (String) selectedItem.getData("name");
          Button oldBtn = (Button)selectedItem.getData("button");
          boolean selected = oldBtn.getSelection();
          oldBtn.dispose();
          createTableRow(index,name,selected);
          selectedItem.dispose();        
          Point size = shell.getSize();
          shell.setSize(size.x+1,size.y+1);
          shell.setSize(size);
        }
      }
    });
    
    table.addMouseMoveListener(new MouseMoveListener(){
      public void mouseMove(MouseEvent e) {
        if(mousePressed && selectedItem != null) {
          Point p = new Point(e.x,e.y);
          TableItem item = table.getItem(p);
          if(item != null) {
            GC gc = new GC(table);
            Rectangle bounds = item.getBounds(1);
            int selectedPosition = table.indexOf(selectedItem);
            int newPosition = table.indexOf(item);
            
            //1. Restore old image
            if(oldPoint != null && oldImage != null) {
              gc.drawImage(oldImage,oldPoint.x,oldPoint.y);
              oldImage.dispose();
              oldImage = null;
              oldPoint = null;
            }            
            if(newPosition <= selectedPosition)
              oldPoint = new Point(bounds.x,bounds.y);
            else
              oldPoint = new Point(bounds.x,bounds.y+bounds.height);
            //2. Store the image
            oldImage = new Image(display,bounds.width,2);
            gc.copyArea(oldImage,oldPoint.x,oldPoint.y);            
            //3. Draw a thick line
            gc.setBackground(blue);
            gc.fillRectangle(oldPoint.x,oldPoint.y,bounds.width,2);
          }
        }
      }
    });
    shell.pack ();
    shell.open ();   
  }
  
  private void close() {
    shell.dispose();
  }
  
  private void saveAndApply() {
    TableItem[] items = table.getItems();
    int position = 0;
    for(int i = 0 ; i < items.length ; i++) {
      Button btn = (Button)items[i].getData("button");
      String name = (String) items[i].getData("name");
      if(btn != null && btn.getSelection()) {
        enumerator.setPositionByName(name,position++);
      } else {
        enumerator.setPositionByName(name,-1);
      }
    }
    enumerator.save();
    listener.tableStructureChanged();
  }
  
  private void createTableRow(int index,String name,boolean selected) {
    TableItem item;
    
    if(index == -1)
      item = new TableItem (table, SWT.NONE);
    else
      item = new TableItem (table, SWT.NONE,index);
    
    item.setText(1,MessageText.getString(propertiesName + "." + name));
    item.setData("name",name);
    TableEditor editor = new TableEditor (table);
    Button button = new Button (table, SWT.CHECK);
    button.setSelection(selected);
    button.pack ();
    editor.minimumWidth = button.getSize ().x;    
    editor.horizontalAlignment = SWT.CENTER;
    editor.setEditor (button, item, 0);
    item.setData("button",button);      
  }
  
  public static void main(String[] args) {
    Display display = new Display();
    String[] tableItems = {
               "#;I;25;0"
              ,"name;S;250;1"
              ,"size;I;70;2"
              ,"done;I;55;3"
              ,"status;I;80;4"
              ,"seeds;I;45;5"
              ,"peers;I;45;6"
              ,"downspeed;I;70;7"
              ,"upspeed;I;70;8"    
              ,"eta;I;70;9"
              ,"tracker;I;70;10"
              ,"priority;I;70;11"
            };    
    ConfigBasedItemEnumerator itemEnumerator = ConfigBasedItemEnumerator.getInstance("MyTorrents",tableItems);
    ITableStructureModificationListener listener = new ITableStructureModificationListener() {
    public void tableStructureChanged() {
    }
    };
    Shell shell = new EnumeratorEditor(
        display,
        itemEnumerator,
        listener,
        "MyTorrentsView").getShell();
    while (!shell.isDisposed ()) {
      if (!display.readAndDispatch ()) display.sleep ();
    }
    display.dispose ();
  }
}

