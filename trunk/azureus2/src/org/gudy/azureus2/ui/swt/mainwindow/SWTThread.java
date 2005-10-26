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
import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.platform.PlatformManagerFactory;

import java.lang.reflect.Constructor;

/**
 * The main SWT Thread, the only one that should run any GUI code.
 */
public class SWTThread {
  
  private static SWTThread instance;
  
  public static SWTThread getInstance() {
    return instance;
  }
  
  public static void createInstance(Initializer app) throws SWTThreadAlreadyInstanciatedException {
    if(instance != null) {
      throw new SWTThreadAlreadyInstanciatedException();
    }
    
    // set SWT specific config parameter defaults
    
    boolean bGTKTableBug_default = Constants.isLinux && SWT.getPlatform().equals("gtk");
  
    COConfigurationManager.setBooleanDefault( "SWT_bGTKTableBug", bGTKTableBug_default );
    
    	//Will only return on termination
    
    new SWTThread(app);

  }
  
  
  Display display;
  private boolean sleak;
  private boolean terminated;
  private Thread runner;
  
  private 
  SWTThread(
  	final Initializer app ) 
  { 
    
    instance = this;
    try {
      display = Display.getCurrent();
	  if ( display == null ){
		  display = new Display();
	      sleak = false;
	  }else{
		  sleak = true;
	  }
    } catch(Exception e) { 
      display = new Display();
      sleak = false;
    }
    
    Display.setAppName("Azureus");
    
    if ( Constants.isOSX ){
    	
    		// use reflection here so we decouple generic SWT from OSX specific stuff to an extent
    	
    	 try{
    	 	
            Class ehancerClass = Class.forName("org.gudy.azureus2.ui.swt.osx.CarbonUIEnhancer");
            
            Constructor constructor = ehancerClass.getConstructor(new Class[]{});
            
            constructor.newInstance(new Object[] {});

        } catch (Exception e) {
        	
            Debug.printStackTrace(e);
        }
    }
    
    runner = new Thread( new AERunnable(){ public void runSupport(){app.run();}},"Main Thread");
    runner.start();   
    
    
    if(!sleak) {
      while(!display.isDisposed() && !terminated) {
        try {
            if (!display.readAndDispatch())
              display.sleep();
        }
        catch (Exception e) {
        	// Must use printStackTrace() (no params) in order to get 
        	// "cause of"'s stack trace in SWT < 3119
        	if (SWT.getVersion() < 3119)
        		e.printStackTrace();
        	else
        		Debug.printStackTrace(e);
        }
      }
      
     
      if(!terminated) {
        
        // if we've falled out of the loop without being explicitly terminated then
        // this appears to have been caused by a non-specific exit/restart request (as the
        // specific ones should terminate us before disposing of the window...)
        
        app.stopIt( false, false );
      }

      try {
      	if (!display.isDisposed())
      		display.dispose();
      } catch (Throwable t) {
      	// Must use printStackTrace() (no params) in order to get 
      	// "cause of"'s stack trace in SWT < 3119
      	if (SWT.getVersion() < 3119)
      		t.printStackTrace();
      	else
      		Debug.printStackTrace(t);
      }

       // dispose platform manager here
      PlatformManagerFactory.getPlatformManager().dispose();
    }
    
  }
  
  
  
  public void terminate() {
    terminated = true;
  }
  
  public Display getDisplay() {
    return display;
  }
}
