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

package org.gudy.azureus2.ui.swt.views.table.utils;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;

import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.views.table.ITableStructureModificationListener;
import org.gudy.azureus2.ui.swt.views.table.TableColumnCore;
import org.gudy.azureus2.ui.swt.views.utils.VerticalAligner;

public class TableColumnEditorWindow {
  
  private Display display;
  private Shell shell;
  private Color blue;
  private Table table;
  
  private TableColumnCore[] tableColumns;
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

  public TableColumnEditorWindow(Display _display,
                                TableColumnCore[] _tableColumns,
                                ITableStructureModificationListener _listener) {    
    RowData rd;
    this.display = _display;
    this.tableColumns = _tableColumns;
    this.listener = _listener;
    
    blue = new Color(display,0,0,128);
    
    shell = new Shell (display,SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
    shell.setImage(ImageRepository.getImage("azureus"));
    shell.setText(MessageText.getString("columnChooser.title"));
    
    GridLayout layout = new GridLayout();
    shell.setLayout (layout);
    
    GridData gridData;
    
    Label label = new Label(shell,SWT.NULL);
    label.setText(MessageText.getString("columnChooser.move"));
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    label.setLayoutData(gridData);
    
    table = new Table (shell, SWT.CHECK | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
    gridData = new GridData(GridData.FILL_BOTH);
    table.setLayoutData(gridData);
    table.setLinesVisible (true);    
    table.setHeaderVisible(true);
    Font f = table.getFont();
    FontData fd = f.getFontData()[0];
    fd.setHeight(9);
    final Font fontNew = new Font(display, fd); 
    table.setFont(fontNew);

    shell.addDisposeListener(new DisposeListener() {
      public void widgetDisposed(DisposeEvent de) {
        if (fontNew != null && !fontNew.isDisposed()) {
          fontNew.dispose();
        }
      }
    });
    
    Composite cButtonArea = new Composite(shell, SWT.NULL);
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
    cButtonArea.setLayoutData(gridData);
    RowLayout rLayout = new RowLayout(SWT.HORIZONTAL);
    rLayout.marginLeft = 0;
 		rLayout.marginTop = 0;
 		rLayout.marginRight = 0;
 		rLayout.marginBottom = 0;
 		rLayout.spacing = 5;
 		cButtonArea.setLayout (rLayout);
    
    Button bOk = new Button(cButtonArea,SWT.PUSH);
    bOk.setText(MessageText.getString("Button.ok"));
    rd = new RowData();
    rd.width = 70;
    bOk.setLayoutData(rd);
    bOk.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event e) {
        saveAndApply();
        close();
      }
    });
    
    Button bCancel = new Button(cButtonArea,SWT.PUSH);
    bCancel.setText(MessageText.getString("Button.cancel"));
    rd = new RowData();
    rd.width = 70;
    bCancel.setLayoutData(rd);
    bCancel.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event e) {
        close();
      }
    });
    
    Button bApply = new Button(cButtonArea,SWT.PUSH);
    bApply.setText(MessageText.getString("columnChooser.apply"));
    rd = new RowData();
    rd.width = 70;
    bApply.setLayoutData(rd);
    bApply.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event e) {
        saveAndApply();
      }
    });
    
    
    String[] columnsHeader = { "columnname", "columndescription" };
    for (int i=0; i< columnsHeader.length; i++) {
      TableColumn column = new TableColumn(table, SWT.NONE);    
      if (columnsHeader[i] != "")
        column.setText(MessageText.getString("columnChooser." + columnsHeader[i]));
    }
    Arrays.sort(tableColumns, new Comparator () {
      public final int compare (Object a, Object b) {
        int iPositionA = ((TableColumnCore)a).getPosition();
        if (iPositionA == -1)
          iPositionA = 0xFFFF;
        int iPositionB = ((TableColumnCore)b).getPosition();
        if (iPositionB == -1)
          iPositionB = 0xFFFF;

        return iPositionA - iPositionB;
      }
    });
    
    for (int i=0; i < tableColumns.length; i++) {
      createTableRow(-1, tableColumns[i], 
                     tableColumns[i].getPosition() >= 0);
    }
    for (int i = 0; i< columnsHeader.length; i++) {
      table.getColumn(i).pack();
    }
    
    
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
          TableColumnCore tableColumn = 
                           (TableColumnCore)selectedItem.getData("TableColumn");
          createTableRow(index, tableColumn, selectedItem.getChecked());
          selectedItem.dispose();        
        }
      }
    });
    
    table.addMouseMoveListener(new MouseMoveListener(){
      public void mouseMove(MouseEvent e) {
        if (!mousePressed || selectedItem == null)
          return;

        Point p = new Point(e.x,e.y);
        TableItem item = table.getItem(p);
        if (item == null)
          return;

        GC gc = new GC(table);
        Rectangle bounds = item.getBounds(0);
        int selectedPosition = table.indexOf(selectedItem);
        int newPosition = table.indexOf(item);

        //1. Restore old image
        if(oldPoint != null && oldImage != null) {
          gc.drawImage(oldImage,oldPoint.x,oldPoint.y);
          oldImage.dispose();
          oldImage = null;
          oldPoint = null;
        }            
        bounds.y += VerticalAligner.getTableAdjustVerticalBy(table);
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
    });
    table.redraw();
    shell.pack ();
    Point p = shell.getSize();
    if (p.x > 550) {
      p.x = 550;
      shell.setSize(p);
    }
    shell.open (); 
  }
  
  private void close() {
    if(blue != null && ! blue.isDisposed())
      blue.dispose();
    shell.dispose();
  }
  
  private void saveAndApply() {
    TableItem[] items = table.getItems();
    int position = 0;
    for(int i = 0 ; i < items.length ; i++) {
      TableColumnCore tableColumn = 
                      (TableColumnCore)items[i].getData("TableColumn");
      tableColumn.setPositionNoShift(items[i].getChecked() ? position++ : -1);
      tableColumn.saveSettings();
    }
    listener.tableStructureChanged();
  }
  
  private void createTableRow(int index, TableColumnCore tableColumn,
                              boolean selected) {
    String name = tableColumn.getName();
    TableItem item;
    
    if(index == -1)
      item = new TableItem (table, SWT.NONE);
    else
      item = new TableItem (table, SWT.NONE, index);
    
    String sTitleLanguageKey = tableColumn.getTitleLanguageKey();
    item.setText(0,MessageText.getString(sTitleLanguageKey));
    item.setText(1,MessageText.getString(sTitleLanguageKey + ".info", ""));
    item.setData("TableColumn", tableColumn);
    item.setChecked(selected);
  }
}

