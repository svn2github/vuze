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
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LGLogger;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.ui.common.util.UserAlerts;
import org.gudy.azureus2.ui.swt.Alerts;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.StartServer;
import org.gudy.azureus2.ui.swt.associations.AssociationChecker;
import org.gudy.azureus2.ui.swt.auth.AuthenticatorWindow;
import org.gudy.azureus2.ui.swt.auth.CertificateTrustWindow;
import org.gudy.azureus2.ui.swt.update.Restarter;
import org.gudy.azureus2.ui.swt.update.UpdateMonitor;
import org.gudy.azureus2.ui.swt.updater2.SWTUpdateChecker;

import com.aelitis.azureus.core.*;

/**
 * this class initiatize all GUI and Core components which are :
 * 1. The SWT Thread
 * 2. Images
 * 3. The Splash Screen if needed
 * 4. The GlobalManager
 * 5. The Main Window in correct state or the Password window if needed.
 */
public class 
Initializer 
	implements AzureusCoreListener, Application 
{
  private AzureusCore		core;
  private GlobalManager 	gm;
  private StartServer 		startServer;
  
  private ArrayList listeners;
  private AEMonitor	listeners_mon	= new AEMonitor( "Initializer:l" );

  private String[] args;
  
  public 
  Initializer(
  		final AzureusCore		_core,
  		StartServer 			_server,
		String[] 				_args ) 
  {
    
    listeners = new ArrayList();
    
    core			= _core;
    startServer 	= _server;
    args 			= _args;
    
    	// these lifecycle actions are initially to handle plugin-initiated stops and restarts
    
    core.addLifecycleListener(
			new AzureusCoreLifecycleAdapter()
			{
				public boolean
				stopRequested(
					AzureusCore		core )
				
					throws AzureusCoreException
				{
					final AESemaphore				sem 	= new AESemaphore("SWTInit::stopReq");
					final AzureusCoreException[]	error 	= {null};
					
					try{
						MainWindow.getWindow().getDisplay().asyncExec(
							new Runnable()
							{
								public void
								run()
								{
									try{
								
										if ( !MainWindow.getWindow().dispose()){
											
											error[0] = new AzureusCoreException( "SWT Initializer: Azureus close action failed");
										}	
									}finally{
											
										sem.release();
									}
								}
							});
					}catch( Throwable e ){
						
						error[0]	= new AzureusCoreException( "SWT Initializer: closeAzureus fails", e );
						
						sem.release();
					}
					
					sem.reserve();
					
					if ( error[0] != null ){
			
						// removed reporting of error 
					}	
					
					return( true );
				}	
				
				public boolean
				restartRequested(
					AzureusCore		core )
				{
					final AESemaphore				sem 	= new AESemaphore("SWTInit:restart");
					final AzureusCoreException[]	error 	= {null};
						
					try{
						MainWindow.getWindow().getDisplay().asyncExec(
							new Runnable()
							{
								public void
								run()
								{
									try{				
										if ( !MainWindow.getWindow().dispose()){
												
											error[0] = new AzureusCoreException( "SWT Initializer: Azureus close action failed");
										}	
										
										Restarter.restartForUpgrade(_core);
									}finally{
												
										sem.release();
									}
								}
							});
					}catch( Throwable e ){
							
						error[0]	= new AzureusCoreException( "SWT Initializer: closeAzureus fails", e );
							
						sem.release();
					}
						
					sem.reserve();
						
					if ( error[0] != null ){

						// removed reporting of error 
					
					}
					
					return( true );
				}
			});
    
    try {
      SWTThread.createInstance(this);
    } catch(SWTThreadAlreadyInstanciatedException e) {
      e.printStackTrace();
    }
  }  
    
  public void 
  run() 
  {
  	try{
	    SWTThread swt = SWTThread.getInstance();
	    
	    Display display = swt.getDisplay();
	    
	    //The splash window, if displayed will need some images. 
	    ImageRepository.loadImagesForSplashWindow(display);
	    
	    //Splash Window is not always shown
	    if (COConfigurationManager.getBooleanParameter("Show Splash", true)) {
	      SplashWindow.create(display,this);
	    }
	    	    
	    setNbTasks(6);
	    
	    nextTask(); 	    
	    reportCurrentTaskByKey("splash.firstMessageNoI18N");
	    
	    Alerts.init();
	    
	    StartupUtils.setLocale();
	    
	    COConfigurationManager.checkConfiguration();
	    
	    new AuthenticatorWindow();
	    
	    new CertificateTrustWindow();

	    nextTask(); 	    
	    reportCurrentTaskByKey("splash.loadingImages");
	    
	    ImageRepository.loadImages(display);
	    
	    nextTask();
	    reportCurrentTaskByKey("splash.initializeGM");
	    
	    core.addListener( this );
	    
	    core.addLifecycleListener(
	    	new AzureusCoreLifecycleAdapter()
			{
	    		public void
				componentCreated(
					AzureusCore					core,
					AzureusCoreComponent		comp )
	    		{
	    			if ( comp instanceof GlobalManager ){
	    				
	    				gm	= (GlobalManager)comp;

		    		    new UserAlerts(gm);
		    		    
		    		    nextTask();	    
		    		    reportCurrentTaskByKey("splash.initializeGui");
		    		    
		    		    new Colors();
		    		    
		    		    Cursors.init();
		    		    
		    		    new MainWindow(core,Initializer.this);
		    		    
		    		    AssociationChecker.checkAssociations();

	    				nextTask();	    
	    				reportCurrentTask(MessageText.getString("splash.initializePlugins"));    				
	    			}
	    		}
	    		
	    		public void
				started(
					AzureusCore		core )
	    		{	    		      		    	    
	    		    nextTask();
	    		    reportCurrentTaskByKey( "splash.openViews");

	    		    SWTUpdateChecker.initialize();
	    		    
	    		    UpdateMonitor.getSingleton( core );	// setup the update monitor
	    			
	    		    //Tell listeners that all is initialized :
	    		    
	    		    Alerts.initComplete();
	    		    
	    		    nextTask();
	    		    
	    		    
	    		    //Finally, open torrents if any.
	    		    if (args.length != 0) {
	    		      TorrentOpener.openTorrent( core, args[0]);
	    		    }
	    		}
			});
	    
	    core.start();

  	}catch( Throwable e ){
  	
  		LGLogger.log( "Initialization fails:", e );
  		
  		e.printStackTrace();
  	} 
  }
  
  public GlobalManager getGlobalManager() {
    return gm;
  }
  
  public void openMainWindow() {
    
  }
  
  public void addListener(AzureusCoreListener listener){
    try{
    	listeners_mon.enter();
    	
    	listeners.add(listener);
    }finally{
    	
    	listeners_mon.exit();
    }
  }
  
  public void removeListener(AzureusCoreListener listener) {
    try{
    	listeners_mon.enter();
		
    	listeners.remove(listener);
    }finally{
    	
    	listeners_mon.exit();
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
  
  
  public void reportCurrentTask(String currentTaskString) {
     try{
     	listeners_mon.enter();
     
	    Iterator iter = listeners.iterator();
	    while(iter.hasNext()) {
	    	AzureusCoreListener listener = (AzureusCoreListener) iter.next();
	      listener.reportCurrentTask(currentTaskString);
	    }
    }finally{
    	
    	listeners_mon.exit();
    }
  }
  
  public void reportPercent(int percent) {
    try{
    	listeners_mon.enter();
    
	    Iterator iter = listeners.iterator();
	    while(iter.hasNext()) {
	    	AzureusCoreListener listener = (AzureusCoreListener) iter.next();
	      listener.reportPercent(overallPercent(percent));
	    }
    }finally{
    	
    	listeners_mon.exit();
    }
  }
  
  public void stopIt() {
    if(core != null){
    	try{
    		core.stop();
    		
    	}catch( AzureusCoreException e ){
    		
    		e.printStackTrace();
    	}
    }
    
    if (startServer != null)
      startServer.stopIt();
    Cursors.dispose();
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
    //System.out.println("ST percent " + currentPercent + " / " + taskPercent + " : " + (currentPercent + (taskPercent / nbTasks)));
    return currentPercent + taskPercent / nbTasks;
  }
  
  private void reportCurrentTaskByKey(String key) {
    reportCurrentTask(MessageText.getString(key));
  }
  
  
  public static void main(String args[]) 
  {
 	AzureusCore		core = AzureusCoreFactory.create();

 	new Initializer( core, null,args);
  }
 
}
