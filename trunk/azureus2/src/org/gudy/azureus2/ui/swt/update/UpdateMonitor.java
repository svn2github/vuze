/*
 * Created on 7 mai 2004
 * Created by Olivier Chalouhi
 * 
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.update;


import com.aelitis.azureus.core.*;

import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.logging.*;

import org.gudy.azureus2.ui.swt.components.StringListChooser;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;

import org.gudy.azureus2.update.CoreUpdateChecker;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.update.*;

/**
 * @author Olivier Chalouhi
 *
 */
public class 
UpdateMonitor 
	implements UpdateCheckInstanceListener 
{
	public static final long AUTO_UPDATE_CHECK_PERIOD = 23*60*60*1000;  // 23 hours

	protected static UpdateMonitor		singleton;
	
	protected static AEMonitor			class_mon 	= new AEMonitor( "UpdateMonitor:class" );
	
	
	public static UpdateMonitor
	getSingleton(
		AzureusCore		core )
	{
		try{
			class_mon.enter();
		
			if ( singleton == null ){
				
				singleton = new UpdateMonitor( core );
			}
			
			return( singleton );
			
		}finally{
			
			class_mon.exit();
		}
	}

	protected AzureusCore			azureus_core;
	protected UpdateWindow 			current_update_window;
	protected UpdateCheckInstance	current_update_instance;
	
	  		
	
	protected 
	UpdateMonitor(
		AzureusCore		_azureus_core ) 
	{
		azureus_core	= _azureus_core;
		
	  	UpdateManager um = azureus_core.getPluginManager().getDefaultPluginInterface().getUpdateManager(); 

	  	um.addListener(
	  		new UpdateManagerListener()
			{
	  			public void
				checkInstanceCreated(
					UpdateCheckInstance	instance )
	  			{
	  				instance.addListener( UpdateMonitor.this );
	  			}
			});
	  	
	    SimpleTimer.addPeriodicEvent( 
	            AUTO_UPDATE_CHECK_PERIOD,
	            new TimerEventPerformer()
	            {
	            	public void
					perform(
						TimerEvent  ev )
					{
	            		performAutoCheck(false);
					}
	            });
	      
	    	// wait a bit before starting check to give rest of AZ time to initialise 
	    
		new DelayedEvent(
				2500,
				new AERunnable()
				{
					public void
					runSupport()
					{
						performAutoCheck(true);
					}
				});
	}
  
	protected void
	performAutoCheck(
		final boolean		start_of_day )
	{
		boolean check_at_start	= false;
		boolean check_periodic	= false;
	
			// no update checks for java web start
		
		if ( !SystemProperties.isJavaWebStartInstance()){
					
			check_at_start	= COConfigurationManager.getBooleanParameter( "update.start", true );
			check_periodic	= COConfigurationManager.getBooleanParameter( "update.periodic", true );
		}
		
			// periodic -> check at start as well
		
		check_at_start = check_at_start || check_periodic;
		
		if (	( check_at_start && start_of_day) ||
				( check_periodic && !start_of_day )){
			
			performCheck();	// this will implicitly do usage stats
			
		}else{

			new DelayedEvent(
					5000,
					new AERunnable()
					{
						public void
						runSupport()
						{
							if ( start_of_day ){
                                MainWindow.getWindow().setStatusText("");
							}

							CoreUpdateChecker.doUsageStats();
						}
					});
		}
	}
	
	public void
	performCheck()
	{
		if ( SystemProperties.isJavaWebStartInstance()){
			
				// just in case we get here somehome!
			
			LGLogger.log( "skipping update check as java web start");
			
			return;
		}

			// kill any existing update window
		
	    if ( current_update_window != null && ! current_update_window.isDisposed()){
	    	
	      SWTThread.getInstance().getDisplay().syncExec(
	      		new AERunnable() 
				{
	      			public void 
					runSupport() 
	      			{               
	      				current_update_window.dispose();
	      			}
	      		});
	    }
	    
	    if ( current_update_instance != null ){
	    	
	    	current_update_instance.cancel();
	    }
	    
		MainWindow mainWindow = MainWindow.getWindow();
		
	    mainWindow.setStatusText("MainWindow.status.checking ...");
	    
	    	// take this off this GUI thread in case it blocks for a while
	    
	   AEThread t = 
		   	new AEThread( "UpdateMonitor:kickoff")
				{
			    	public void
					runSupport()
			    	{
			    		UpdateManager um = azureus_core.getPluginManager().getDefaultPluginInterface().getUpdateManager(); 
				
			    		current_update_instance = 
			    			um.createUpdateCheckInstance(
			    				UpdateCheckInstance.UCI_UPDATE,
			  					"update.instance.update" );
			  	
			    		current_update_instance.start();
			    	}
				};
			
		t.setDaemon( true );
		
		t.start();
	}
	
	public void
	complete(
		UpdateCheckInstance		instance )
	{
			// we can get here for either update actions (triggered above) or for plugin
			// install actions (triggered by the plugin installer)
		
		boolean	update_action = instance.getType() == UpdateCheckInstance.UCI_UPDATE;
		
		if ( update_action ){
		
			PluginInterface core_plugin = azureus_core.getPluginManager().getPluginInterfaceByClass( CoreUpdateChecker.class );
			
			String latest_version = core_plugin.getPluginProperties().getProperty( CoreUpdateChecker.LATEST_VERSION_PROPERTY );
			
			MainWindow mainWindow = MainWindow.getWindow();
		
		    mainWindow.setStatusText("");
		}
		
	    Update[] us = instance.getUpdates();
	   
	    boolean	show_window = false;
	    
	    	// updates with zero-length downloaders exist for admin purposes
	    	// and shoudn't cause the window to be shown if only they exist
	    
	    for (int i=0;i<us.length;i++){
	    	
	    	if (us[i].getDownloaders().length > 0 ){
	    		
	    		show_window	= true;
	    		
	    		break;
	    	}
	    }
	    
	    	// this controls whether or not the update window is displayed
	    	// note that we just don't show the window if this is set, we still do the
	    	// update check (as amongst other things we want ot know the latest
	    	// version of the core anyway	    
	    
    	if ( show_window ){
    		
    			// don't show another update if one's already there!
    		
    		UpdateWindow	this_window = null;
    		
    		if ( update_action ){
    			
    			if ( current_update_window == null || current_update_window.isDisposed()){
    			
    				this_window = current_update_window = new UpdateWindow( azureus_core, instance );
    			}
    		}else{

    				// always show an installer window
    			
    			this_window = new UpdateWindow( azureus_core, instance );
    		}
    		
    		if ( this_window != null ){
    			
	    		for( int i = 0 ;  i < us.length; i++ ){
				
	    			if ( us[i].getDownloaders().length > 0 ){
	    				
	    				this_window.addUpdate(us[i]);
	    			}
	    		}
	    		
	    		this_window.updateAdditionComplete();
	    		
    		}else{
    			
    			LGLogger.log( "UpdateMonitor: user dialog already in progress, updates skipped");

    		}
    	}else{
    		
			LGLogger.log( "UpdateMonitor: check instance resulted in no user-actionable updates");

    	}
	} 
	
	public void
	cancelled(
		UpdateCheckInstance		instance )
	{
		MainWindow mainWindow = MainWindow.getWindow();

		mainWindow.setStatusText("");

	}
}
