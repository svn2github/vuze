/*
 * Created on Apr 9, 2004
 * Created by Alon Rohter
 * Copyright (C) 2004 Alon Rohter, All Rights Reserved.
 * 
 */
package org.gudy.azureus2.ui.swt.snippets;


import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import org.gudy.azureus2.core3.util.AEThread;

/**
 * 
 */
public class TableWith0sizedColumn {
  public static void main(String[] args) {
    final Display display = new Display();
    final Shell shell = new Shell(display,SWT.SHELL_TRIM);
    shell.setLayout(new FillLayout());
    
    final Table table = new Table(shell,SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER);
    table.setHeaderVisible(true);
    
    TableColumn column0sized = new TableColumn(table,SWT.NULL);
    TableColumn column1 = new TableColumn(table,SWT.NULL);
    TableColumn column2 = new TableColumn(table,SWT.NULL);
    column0sized.setWidth(0);
    column0sized.setResizable(false);
    
    column1.setWidth(200);
    column1.setText("Column 1");
    column2.setWidth(200);
    column2.setText("Column 2");
    
    for(int i = 0 ; i < 5 ; i++) {
      TableItem item = new TableItem(table,0);
      item.setText(new String[] {null,"col 1 row " + i , "col 2 row " + i});
    }
    
    Thread tUpdateValues = new AEThread("TableWith0sizedColumn") {
      public void run() {
        final int[] t = new int[1];
        t[0] = 0;
        while(!display.isDisposed()) {
          t[0]++;
          display.asyncExec(new Runnable() {
            public void run() {
              if(table.isDisposed())
                return;
              TableItem[] items = table.getItems();
              for(int i = 0 ; i < items.length ; i++) {
                TableItem item = items[i];
                item.setText(new String[] {null,"col 1 row " + i + " / " + t[0] , "col 2 row " + i + " / " + t[0]});
              }
            }
          });
          try { Thread.sleep(1000); } catch(Exception ignore) {}
        }
      }
    };
    
    tUpdateValues.start();
    
    shell.setSize(300,300);
    shell.open();
    
    while(!shell.isDisposed()) {
      if(!display.readAndDispatch())
        display.sleep();
    }
    
    display.dispose();
  }
}
