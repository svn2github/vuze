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


import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.util.*;

import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;

import org.gudy.azureus2.update.CoreUpdateChecker;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.update.*;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;

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
	
	protected UpdateWindow 			current_window;
  
	protected UpdateCheckInstance		current_instance;
	
	public static synchronized UpdateMonitor
	getSingleton()
	{
		if ( singleton == null ){
			
			singleton = new UpdateMonitor();
		}
		
		return( singleton );
	}
	
	protected 
	UpdateMonitor() 
	{
		Timer version_check_timer = new Timer("Auto-update timer");

	    version_check_timer.addPeriodicEvent( 
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
				new Runnable()
				{
					public void
					run()
					{
						performAutoCheck(true);
					}
				});
	}
  
	protected void
	performAutoCheck(
		final boolean		start_of_day )
	{
		boolean check_at_start	= COConfigurationManager.getBooleanParameter( "update.start", true );
		boolean check_periodic	= COConfigurationManager.getBooleanParameter( "update.periodic", true );
		
			// periodic -> check at start as well
		
		check_at_start = check_at_start || check_periodic;
		
		if (	( check_at_start && start_of_day) ||
				( check_periodic && !start_of_day )){
			
			performCheck();	// this will implicitly do usage stats
			
		}else{

			new DelayedEvent(
					5000,
					new Runnable()
					{
						public void
						run()
						{
							if ( start_of_day ){
								
								MainWindow mainWindow = MainWindow.getWindow();
				
							    mainWindow.setStatusText( 
							    		Constants.AZUREUS_NAME + " " + Constants.AZUREUS_VERSION + 
										" / MainWindow.status.latestversionunchecked" );
							}
							
							CoreUpdateChecker.doUsageStats();
						}
					});
		}
	}
	
	public void
	performCheck()
	{
    if(current_window != null && ! current_window.isDisposed()) {
      SWTThread.getInstance().getDisplay().syncExec(new Runnable() {
        public void run() {               
          current_window.dispose();         
        }
      });
    }
		MainWindow mainWindow = MainWindow.getWindow();
		
	    mainWindow.setStatusText( Constants.AZUREUS_NAME + " " + Constants.AZUREUS_VERSION + " / MainWindow.status.checking ...");
	    
	  	UpdateManager um = PluginInitializer.getDefaultInterface().getUpdateManager(); 
		
	  	current_instance = um.createUpdateCheckInstance();
		  	
	  	current_instance.addListener( this );
		  	
	  	UpdateChecker[]	checkers = current_instance.getCheckers();
	  	
	  	current_instance.start();		
	}
	
	public void
	complete(
		UpdateCheckInstance		instance )
	{
		PluginInterface core_plugin = PluginManager.getPluginInterfaceByClass( CoreUpdateChecker.class );
		
		String latest_version = core_plugin.getPluginProperties().getProperty( CoreUpdateChecker.LATEST_VERSION_PROPERTY );
		
		MainWindow mainWindow = MainWindow.getWindow();
	
	    mainWindow.setStatusText( 
	    		Constants.AZUREUS_NAME + " " + Constants.AZUREUS_VERSION + 
				" / MainWindow.status.latestversion " + (latest_version==null?"Unknown":latest_version ));
	    
		if ( instance != current_instance ){
			
			return;
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
	   
	    //show_window = 	show_window && 
			//			COConfigurationManager.getBooleanParameter( "update.opendialog", true );
	    
	    
    	if ( show_window ){
    		
    			// don't show another if one's already there!
    		
    		if ( current_window == null || current_window.isDisposed()){
    			
	    		current_window = new UpdateWindow( instance );
				
	    		for( int i = 0 ;  i < us.length; i++ ){
				
	    			if ( us[i].getDownloaders().length > 0 ){
	    				
	    				current_window.addUpdate(us[i]);
	    			}
	    		}
    		}
    	}
	} 
	
	public void
	cancelled(
		UpdateCheckInstance		instance )
	{
	}
}
