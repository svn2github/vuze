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

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerFactory;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.startup.STProgressListener;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.common.util.UserAlerts;
import org.gudy.azureus2.ui.swt.Alerts;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.StartServer;
import org.gudy.azureus2.ui.swt.associations.AssociationChecker;
import org.gudy.azureus2.ui.swt.auth.AuthenticatorWindow;
import org.gudy.azureus2.ui.swt.auth.CertificateTrustWindow;

/**
 * this class initiatize all GUI and Core components which are :
 * 1. The SWT Thread
 * 2. Images
 * 3. The Splash Screen if needed
 * 4. The GlobalManager
 * 5. The Main Window in correct state or the Password window if needed.
 */
public class Initializer implements STProgressListener, Application {
  
  private GlobalManager gm;
  private StartServer startServer;
  
  private ArrayList listeners;
  
  public Initializer(StartServer server) {
    
    this.listeners = new ArrayList();
    this.startServer = server;
    
    try {
      SWTThread.createInstance(this);
    } catch(SWTThreadAlreadyInstanciatedException e) {
      e.printStackTrace();
    }
  }  
    
  public void run() {  
    SWTThread swt = SWTThread.getInstance();
    
    Display display = swt.getDisplay();
    
    //The splash window, if displayed will need some images. 
    ImageRepository.loadImagesForSplashWindow(display);
    
    //Splash Window is not always shown
    if (COConfigurationManager.getBooleanParameter("Show Splash", true)) {
      SplashWindow.create(display,this);
    }
    
    setNbTasks(6);
    
    reportCurrentTaskByKey("splash.firstMessageNoI18N");
    nextTask();   
    Alerts.init();
    StartupUtils.setLocale();
    COConfigurationManager.checkConfiguration();
    new AuthenticatorWindow();
    new CertificateTrustWindow();
    AssociationChecker.checkAssociations();
    
    nextTask();
    reportCurrentTaskByKey("splash.loadingImages");
    ImageRepository.loadImages(display);
    
    reportCurrentTaskByKey("splash.initializeGM");   
    nextTask();
    this.gm = GlobalManagerFactory.create(false, this);
    new UserAlerts(gm);
    
    
    
    
    nextTask();
    reportCurrentTaskByKey("splash.initializeGui");
    new Colors();
    new MainWindow(gm,this);
    
    nextTask();  
    reportCurrentTaskByKey( "splash.initializePlugins");
    PluginInitializer.getSingleton(gm,this).initializePlugins( PluginManager.UI_SWT );        
    LGLogger.log("Initializing Plugins complete");
    
    nextTask();
    reportCurrentTaskByKey( "splash.openViews");
    
    
    //Tell listeners that all is initialized :
    Alerts.initComplete();
    
    nextTask();
     
  }
  
  public GlobalManager getGlobalManager() {
    return gm;
  }
  
  public void openMainWindow() {
    
  }
  
  public void addListener(STProgressListener listener){
    synchronized(listeners) {
      listeners.add(listener);
    }
  }
  
  public void removeListener(STProgressListener listener) {
    synchronized(listeners) {
      listeners.remove(listener);
    }
  }
  public void disposeAllGUIElements() {
    
    ImageRepository.unLoadImages();
  }
  
  public void stopApplication() {
    disposeAllGUIElements();
    if(startServer != null)
      	startServer.stopIt();
    SWTThread.getInstance().terminate();
  }
  
  public static void main(String args[]) {
    new Initializer(null);
  }
  
  public void reportCurrentTask(String currentTask) {
    synchronized(listeners) {
	    Iterator iter = listeners.iterator();
	    while(iter.hasNext()) {
	      STProgressListener listener = (STProgressListener) iter.next();
	      listener.reportCurrentTask(currentTask);
	    }
    }
  }
  
  public void reportPercent(int percent) {
    synchronized(listeners) {
	    Iterator iter = listeners.iterator();
	    while(iter.hasNext()) {
	      STProgressListener listener = (STProgressListener) iter.next();
	      listener.reportPercent(overallPercent(percent));
	    }
    }
  }
  
  public void stopIt() {
    if(gm != null)
      gm.stopAll();
    if (startServer != null)
      startServer.stopIt();
    SWTThread.getInstance().terminate();  
  }
  
  private int nbTasks = 1;
  private int currentTask = 0;
  private int currentPercent = 0;
  
  private void setNbTasks(int nbTasks) {
    this.currentTask = 0;
    this.nbTasks = nbTasks;
  }
  
  private void nextTask() {
    currentTask++;
    currentPercent = 100 * currentTask / (nbTasks) ;
    //0% done of current task
    reportPercent(0);
  }
  
  private int overallPercent(int taskPercent) {
    return currentPercent + (100 * taskPercent) / (100 * nbTasks);
  }
  
  private void reportCurrentTaskByKey(String key) {
    reportCurrentTask(MessageText.getString(key));
  }
}
