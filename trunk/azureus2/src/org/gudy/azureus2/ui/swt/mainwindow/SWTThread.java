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


import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.ui.swt.osx.CarbonUIEnhancer;

/**
 * The main SWT Thread, the only one that should run any GUI code.
 */
public class SWTThread {
  
  private static SWTThread instance;
  
  public static SWTThread getInstance() {
    return instance;
  }
  
  public static void createInstance(Application app) throws SWTThreadAlreadyInstanciatedException {
    if(instance != null) {
      throw new SWTThreadAlreadyInstanciatedException();
    }
    //Will only return on termination
    new SWTThread(app);

  }
  
  
  Display display;
  private boolean terminated;
  private Thread runner;
  
  private SWTThread(Application app) { 
    
    instance = this;
    
    display = new Display();
    Display.setAppName("Azureus");
    
    if(Constants.isOSX) {
      new CarbonUIEnhancer();
    }
    
    runner = new Thread(app,"Main Thread");
    runner.start();   
    
    while(!display.isDisposed() && !terminated) {
      try {
          if (!display.readAndDispatch())
            display.sleep();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
    
    //if it hasn been terminated, there's no way to stop the application
    //as the application is the one who has stopped it.
    if(!terminated) {
      app.stopIt();
    }
    
    display.dispose();
  }
  
  
  
  public void terminate() {
    terminated = true;
  }
  
  public Display getDisplay() {
    return display;
  }
}
