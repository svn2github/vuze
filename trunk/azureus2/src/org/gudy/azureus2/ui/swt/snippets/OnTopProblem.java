/*
 * Created on Apr 9, 2004
 * Created by Alon Rohter
 * Copyright (C) 2004 Alon Rohter, All Rights Reserved.
 * 
 */
package org.gudy.azureus2.ui.swt.snippets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

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
    mainShell.setSize(300,200);
    mainShell.open();
    
    onTopShell = new Shell(mainShell,SWT.ON_TOP);
    onTopShell.setSize(200,30);
    onTopShell.open();
    
    mainShell.addListener(SWT.Iconify, new Listener(){
        public void handleEvent(Event e) {
        	onTopShell.setVisible(true);
        }
    });
    
    
    waitForDispose();
    display.dispose();
  }
  
  public void waitForDispose() {
    while(!mainShell.isDisposed()) {
      if(!display.readAndDispatch())
        display.sleep();
    }
  }
  
  public void updateDisplay() {
    if(display != null && ! display.isDisposed() ) {
      display.asyncExec(new Runnable() {
        public void run() {
          labelIter.setText("" + iter);
          onTopShell.setSize(sizes[iter % sizes.length],20);
        }
      });
    }
  }
  
  public static void main(String args[]) {
    new OnTopProblem();
  }
}
