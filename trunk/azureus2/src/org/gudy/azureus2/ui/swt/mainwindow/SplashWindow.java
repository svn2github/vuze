/*
 * Created on Apr 30, 2004
 * Created by Olivier Chalouhi
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
package org.gudy.azureus2.ui.swt.mainwindow;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

import com.aelitis.azureus.core.*;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Utils;

/**
 * The initial Splash Screen shown while azureus loads 
 */
public class SplashWindow implements AzureusCoreListener {
  
  Display display;
  Initializer initializer;
  
  Shell splash;
  Label currentTask;
  ProgressBar percentDone;
  Color white;
  
  
  private SplashWindow(Display display,Initializer initializer) {
    this.display = display;
    this.initializer = initializer;
    
    white = new Color(display,255,255,255);
    splash = new Shell(display, SWT.NULL);
    splash.setText("Azureus");
    splash.setImage(ImageRepository.getImage("azureus")); //$NON-NLS-1$
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    layout.horizontalSpacing = 0;
    layout.verticalSpacing = 0;
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    splash.setLayout(layout);
    Label label = new Label(splash, SWT.NONE);
    label.setImage(ImageRepository.getImage("azureus_splash"));

    currentTask = new Label(splash,SWT.BORDER);
    GridData gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
    currentTask.setLayoutData(gridData);
    currentTask.setBackground(white);
    currentTask.setText("(: Azureus :)");
    
    this.percentDone = new ProgressBar(splash,SWT.HORIZONTAL);
    this.percentDone.setMinimum(0);
    this.percentDone.setMaximum(100);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    this.percentDone.setLayoutData(gridData);

    splash.pack();
    splash.layout();
    Utils.centreWindow(splash);
    splash.open();
    
    initializer.addListener(this);
  }
  
  
  public static void create(final Display display,final Initializer initializer) {
    if(display != null && !display.isDisposed()) {
      display.asyncExec(new AERunnable() { public void runSupport() {new SplashWindow(display,initializer);}});
    }    
  }
  
  
  /*
   * Should be called by the GUI thread
   */
  private void closeSplash() {
    if(initializer != null)
      initializer.removeListener(this);
    if(splash != null && !splash.isDisposed())
      splash.dispose();
    if(white != null && ! white.isDisposed())
      white.dispose();
  }
  
  
  /*
   * STProgressListener implementation
   */
  
  public void reportCurrentTask(final String task) {
    //Ensure that display is set and not disposed
    if(display == null || display.isDisposed())
      return;
    
    //Post runnable to SWTThread
    display.asyncExec(new AERunnable(){
      public void runSupport() {
        //Ensure than the task Label is created and not disposed
        if(currentTask == null || currentTask.isDisposed())
          return;
        currentTask.setText(task);
      }
    });
  }
  
  public void reportPercent(final int percent) {
    //Ensure that display is set and not disposed
    if(display == null || display.isDisposed())
      return;
    
    //Post runnable to SWTThread
    display.asyncExec(new AERunnable(){
      public void runSupport() {
        //Ensure than the percentDone ProgressBar is created and not disposed
        if(percentDone == null || percentDone.isDisposed())
          return;
        percentDone.setSelection(percent);      
        
        //OK Tricky way to close the splash window BUT ... sending a percent > 100 means closing
        if(percent > 100) {
          closeSplash();
        }
      }
    });
  }
 
}
