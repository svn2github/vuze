/*
 * Created on 25-May-2004
 * Created by Paul Gardner
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

package org.gudy.azureus2.update;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.logging.*;
	
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.update.*;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;


public class 
CorePatchChecker
	implements Plugin, UpdatableComponent, UpdateCheckInstanceListener
{
	public static final boolean	TESTING	= true;
	
	public void 
	initialize(
		PluginInterface pi )
	  
	  	throws PluginException
	{
		if ( TESTING || !Constants.isCVSVersion()){
		
			if ( TESTING ){
				
				System.out.println( "CorePatchChecker: TESTING !!!!" );
			}
			
			pi.getUpdateManager().registerUpdatableComponent( this, true );
		}
	}
	
	public String
	getName()
	{
		return( "Core Patch Checker");
	}
	
	
	public int
	getMaximumCheckTime()
	{
		return( 0 );
	}
	
	public void
	checkForUpdate(
		UpdateChecker	checker )
	{
		try{
			UpdateCheckInstance	inst = checker.getCheckInstance();
		
			inst.addListener( this );
		
			Update update = 
				checker.addUpdate( "Core Patch Checker", new String[0], "",
									new ResourceDownloader[0],
									Update.RESTART_REQUIRED_MAYBE );
		}finally{
			
			checker.completed();
		}
	}
	
	public void
	cancelled(
		UpdateCheckInstance		instance )
	{
	}
	
	public void
	complete(
		UpdateCheckInstance		instance )
	{
		Update[]	updates = instance.getUpdates();
		
		PluginInterface updater_plugin = PluginManager.getPluginInterfaceByClass( UpdaterUpdateChecker.class );
		
		for (int i=0;i<updates.length;i++){
			
			Update	update = updates[i];
			
			Object	user_object = update.getUserObject();
			
			if ( user_object != null && user_object == updater_plugin ){
				
				// OK, we have an updater update
				
				LGLogger.log( "Core Patcher: updater update found" );
				
				update.addListener(
						new UpdateListener()
						{
							public void
							complete(
								Update	update )
							{
								LGLogger.log( "Core Patcher: updater update complete" );
							}
						});
			}
		}
	}
}
