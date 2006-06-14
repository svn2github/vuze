/*
 * Created on Apr 30, 2004
 * Created by Olivier Chalouhi
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
package org.gudy.azureus2.ui.swt.mainwindow;

import com.aelitis.azureus.core.*;
import com.aelitis.azureus.ui.IUIIntializer;

import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.common.util.UserAlerts;
import org.gudy.azureus2.ui.swt.Alerts;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.LocaleUtilSWT;
import org.gudy.azureus2.ui.swt.StartServer;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.auth.AuthenticatorWindow;
import org.gudy.azureus2.ui.swt.auth.CertificateTrustWindow;
import org.gudy.azureus2.ui.swt.networks.SWTNetworkSelection;
import org.gudy.azureus2.ui.swt.pluginsinstaller.InstallPluginWizard;
import org.gudy.azureus2.ui.swt.update.UpdateMonitor;
import org.gudy.azureus2.ui.swt.updater2.SWTUpdateChecker;

import java.util.ArrayList;
import java.util.Iterator;

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
	implements AzureusCoreListener, IUIIntializer
{
	private static final LogIDs LOGID = LogIDs.GUI;
  private AzureusCore		azureus_core;
  private GlobalManager 	gm;
  private StartServer 		startServer;
  
  private ArrayList listeners;
  private AEMonitor	listeners_mon	= new AEMonitor( "Initializer:l" );

  private String[] args;
  
  public 
  Initializer(
  		final AzureusCore		_azureus_core,
  		StartServer 			_server,
		String[] 				_args ) 
  {
    listeners = new ArrayList();
    
    azureus_core	= _azureus_core;
    startServer 	= _server;
    args 			= _args;
    
    	// these lifecycle actions are initially to handle plugin-initiated stops and restarts
    
    azureus_core.addLifecycleListener(
			new AzureusCoreLifecycleAdapter()
			{
				public boolean
				stopRequested(
					AzureusCore		_core )
				
					throws AzureusCoreException
				{
					return( handleStopRestart(false));
				}
				
				public boolean
				restartRequested(
					final AzureusCore		core )
				{
					return( handleStopRestart(true));
				}
			});
    
    try {
      SWTThread.createInstance(this);
    } catch(SWTThreadAlreadyInstanciatedException e) {
    	Debug.printStackTrace( e );
    }
  }  

  public boolean
  handleStopRestart(
  	final boolean	restart )
  {
  	if ( MainWindow.getWindow().getDisplay().getThread() == Thread.currentThread())
  		return( MainWindow.getWindow().dispose(restart,true));
		
		final AESemaphore			sem 	= new AESemaphore("SWTInit::stopRestartRequest");
		final boolean[]				ok	 	= {false};
		
		try{
			Utils.execSWTThread(
					new AERunnable()
					{
						public void
						runSupport()
						{
							try{
								ok[0] = MainWindow.getWindow().dispose(restart,true);
								
							}catch( Throwable e ){
								
								Debug.printStackTrace(e);
							
							}finally{
								
								sem.release();
							}
						}
					});
		
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
			
			sem.release();
		}
	
		sem.reserve();
	
		return( ok[0] );
  }
	

  public void 
  run() 
  {
  	try{
  		// initialise the SWT locale util
	  	
	    new LocaleUtilSWT( azureus_core );

			Display display = SWTThread.getInstance().getDisplay();
	    
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
	    
	    final ArrayList logEvents = new ArrayList();
	    ILogEventListener logListener = null;
	    if (COConfigurationManager.getBooleanParameter("Open Console", false)) {
	    	logListener = new ILogEventListener() {
					public void log(LogEvent event) {
						logEvents.add(event);
					}
	    	};
	    	Logger.addListener(logListener);
	    }
	    final ILogEventListener finalLogListener = logListener;

	    new SWTNetworkSelection();
	    
	    new AuthenticatorWindow();
	    
	    new CertificateTrustWindow();

	    InstallPluginWizard.register( azureus_core, display );
	    
	    nextTask(); 	    
	    reportCurrentTaskByKey("splash.loadingImages");
	    
	    ImageRepository.loadImages(display);
	    
	    nextTask();
	    reportCurrentTaskByKey("splash.initializeGM");
	    
	    azureus_core.addListener( this );
	    
	    azureus_core.addLifecycleListener(
	    	new AzureusCoreLifecycleAdapter()
			{
	    		public void
				componentCreated(
					AzureusCore					core,
					AzureusCoreComponent		comp )
	    		{
	    			if ( comp instanceof GlobalManager ){
	    				
	    				gm	= (GlobalManager)comp;
	    			}
	    		}
	    		
	    		public void
				started(
					AzureusCore		core )
	    		{	    		      	
	    			if (gm == null)
	    				return;

    		    new UserAlerts(gm);
    		    
    		    nextTask();	    
    		    reportCurrentTaskByKey("splash.initializeGui");
    		    
    		    new Colors();
    		    
    		    Cursors.init();
    		    
    		    MainWindow main_window = new MainWindow(core,Initializer.this,logEvents);
    		    
    		    if (finalLogListener != null)
    		    	Logger.removeListener(finalLogListener);

    		    nextTask();
    		    
	    		    reportCurrentTaskByKey( "splash.openViews");

	    		    SWTUpdateChecker.initialize();
	    		    
	    		    UpdateMonitor.getSingleton( core, main_window );	// setup the update monitor
	    			
	    		    //Tell listeners that all is initialized :
	    		    
	    		    Alerts.initComplete();
	    		    
	    		    nextTask();
	    		    
	    		    
	    		    //Finally, open torrents if any.
	    		    
	    		    for (int i=0;i<args.length;i++){
	    		    	
	    		    	try{
	    		    		TorrentOpener.openTorrent(args[i]);
	    		    		
	    	        	}catch( Throwable e ){
	    	        		
	    	        		Debug.printStackTrace(e);
	    	        	}	
	    		    }
	    		}
	    		
	    		public void
	    		stopping(
	    			AzureusCore		core )
	    		{
	    			Alerts.stopInitiated();
	    		}
	    		
	    		public void
	    		stopped(
	    			AzureusCore		core )
	    		{
	    			Alerts.stopCompleted();
	    		}
	    		
	    		public boolean
	    		syncInvokeRequired()
	    		{
	    			return( true );
	    		}
			});
	    
			nextTask();	    
			reportCurrentTask(MessageText.getString("splash.initializePlugins"));

	    azureus_core.start();

  	}catch( Throwable e ){
  		Logger.log(new LogEvent(LOGID, "Initialization fails:", e));
  	} 
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
  
  // AzureusCoreListener
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
  
  // AzureusCoreListener
  public void reportPercent(int percent) {
  	int overallPercent = overallPercent(percent);
    try{
    	listeners_mon.enter();
    
	    Iterator iter = listeners.iterator();
	    while(iter.hasNext()) {
	    	AzureusCoreListener listener = (AzureusCoreListener) iter.next();
	      listener.reportPercent(overallPercent);
	    }

	    if (overallPercent > 100) {
	    	listeners.clear();
	    }
    }finally{
    	
    	listeners_mon.exit();
    }
  }
  
  public void 
  stopIt(
  	boolean	for_restart,
	boolean	close_already_in_progress ) 
  
  	throws AzureusCoreException
  {
    if ( azureus_core != null && !close_already_in_progress ){

    	if ( for_restart ){

    		azureus_core.checkRestartSupported();
    	}
    }
    
  	try{
	    if ( startServer != null ){
	    
	    	startServer.stopIt();
	    }
	    
	    Cursors.dispose();
	    
	    SWTThread.getInstance().terminate();  
	    
	}finally{
	  	
	    if ( azureus_core != null && !close_already_in_progress ){

	    	try{
		    	if ( for_restart ){
		    			
		    		azureus_core.restart();
		    			
		    	}else{
		    			
		    		azureus_core.stop();
		    	}
	    	}catch( Throwable e ){
	    		
	    			// don't let any failure here cause the stop operation to fail
	    		
	    		Debug.out( e );
	    	}
	    }
	}
  }
  
  private int nbTasks = 1;
  private int currentTask = 0;
  private int currentPercent = 0;
  
  private void setNbTasks(int _nbTasks) {
    currentTask = 0;
    nbTasks = _nbTasks;
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
