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


import org.gudy.azureus2.core3.util.DelayedEvent;

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
	UpdateWindow window;
  
	UpdateCheckInstance		current_instance;
	
	public 
	UpdateMonitor() 
	{
		new DelayedEvent(
				2500,
				new Runnable()
				{
					public void
					run()
					{
						performCheck();
					}
				});
	}
  
	public void
	performCheck()
	{
	  	UpdateManager um = PluginInitializer.getDefaultInterface().getUpdateManager(); 
		
	  	current_instance = um.createUpdateCheckInstance();
		  	
	  	current_instance.addListener( this );
		  	
	  	UpdateChecker[]	checkers = current_instance.getCheckers();
	  	
	  	for (int i=0;i<checkers.length;i++){
	  		
	  		UpdateChecker	checker = checkers[i];
	  		
	  		System.out.println( "Checker:" + checker.getComponent().getName() + "/" + checker.getComponent().getMaximumCheckTime());
	  		
	  		checker.addListener(
	  			new UpdateCheckerListener()
				{
	  				public void
					completed(
						UpdateChecker	checker )
					{
	  					System.out.println( "    " + checker.getComponent().getName() + " completed" );
	  				}
										
					public void
					failed(
						UpdateChecker	checker )
					{
						System.out.println( "    " + checker.getComponent().getName() + " failed" );
					}
					
					public void
					cancelled(
						UpdateChecker	checker )
					{
	  					System.out.println( "    " + checker.getComponent().getName() + " cancelled" );
					}
	  			});
	  	}
	  	
	  	current_instance.start();		
	}
	
	public void
	complete(
		UpdateCheckInstance		instance )
	{
		if ( instance != current_instance ){
			
			return;
		}
		
	    Update[] us = instance.getUpdates();
	    
	    if ( us.length > 0 ){
	    	
			window = new UpdateWindow( instance );
				
			for(int i = 0 ;  i < us.length ; i++){
				
				window.addUpdate(us[i]);
			}	
	    }
	} 
	
	public void
	cancelled(
		UpdateCheckInstance		instance )
	{
	}
}
