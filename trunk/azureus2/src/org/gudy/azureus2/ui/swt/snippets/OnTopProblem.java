/*
 * Created on Apr 9, 2004
 * Created by Alon Rohter
 * Copyright (C) 2004 Alon Rohter, All Rights Reserved.
 * 
 */
package org.gudy.azureus2.ui.swt.snippets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;

import org.gudy.azureus2.core3.util.AEThread;

/**
 * 
 */
public class OnTopProblem {
  private static int[] sizes = {100,120,140};
  
  Display display;
  Shell mainShell;
  Shell onTopShell;
  
  Label labelIter;
  
  int iter;
  
  
  public OnTopProblem() {
    display = new Display();
    mainShell = new Shell(display,SWT.SHELL_TRIM);        
    mainShell.setText("OnTopProblem");
    
    mainShell.setLayout(new FillLayout());
    
    Button btnClose = new Button(mainShell,SWT.PUSH);
    btnClose.setText("Close");
    btnClose.addListener(SWT.Selection,new Listener() {
    	public void handleEvent(Event arg0) {
    		mainShell.dispose();
    	} 
    });    
    
    mainShell.setSize(300,200);
    mainShell.open();
    
    onTopShell = new Shell(mainShell,SWT.ON_TOP);
    onTopShell.setSize(200,30);
    onTopShell.open();
    
    onTopShell.setLayout(new FillLayout());
    
    labelIter = new Label(onTopShell,SWT.NULL);
    
    Tray tray = display.getSystemTray();
    TrayItem trayItem = new TrayItem(tray,SWT.NULL);
    trayItem.addListener(SWT.DefaultSelection, new Listener() {
      public void handleEvent(Event e) {
       mainShell.setVisible(true); 
      }
    });
    
    mainShell.addListener(SWT.Close, new Listener(){
        public void handleEvent(Event e) {
        	e.doit = false;
          mainShell.setVisible(false);
          onTopShell.setVisible(true);
        }
    });
    
    Thread t = new AEThread("OnTopProblem") {
      public void run() {
       while(updateDisplay()) {
        try { Thread.sleep(100); } catch(Exception ignore) {}   
       }
      }
     };
     
     t.start();
    
    waitForDispose();
    display.dispose();
  }
  

  
  public void waitForDispose() {
    while(!mainShell.isDisposed()) {
      if(!display.readAndDispatch())
        display.sleep();
    }
  }
  
  public boolean updateDisplay() {
    if(display != null && ! display.isDisposed() ) {
      display.asyncExec(new Runnable() {
        public void run() {
          iter++;
          labelIter.setText("" + iter);
          onTopShell.setSize(sizes[iter % sizes.length],20);
        }
      });
      return true;
    }
    return false;
  }
  
  public static void main(String args[]) {
    new OnTopProblem();
  }
}
