/*
 * Created on Nov 5, 2014
 * Created by Paul Gardner
 * 
 * Copyright 2014 Azureus Software, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.plugins.net.buddy;

import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;

import com.aelitis.azureus.core.AzureusCoreFactory;

public class 
BuddyPluginUtils 
{
	private static BuddyPlugin
	getPlugin()
	{
		PluginInterface pi = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByID( "azbuddy", true );
		
		if ( pi != null ){
			
			return((BuddyPlugin)pi.getPlugin());
		}
		
		return( null );
	}
	
	public static boolean
	isBetaChatAvailable()
	{
		BuddyPlugin bp = getPlugin();
		
		if ( bp != null && bp.isBetaEnabled()){
			
			return( bp.getBeta().isAvailable());
		}
		
		return( false );
	}
	
	public static boolean
	isBetaChatAnonAvailable()
	{
		BuddyPlugin bp = getPlugin();
		
		if ( bp != null && bp.isBetaEnabled()){
			
			return( bp.getBeta().isAvailable() && bp.getBeta().isI2PAvailable());
		}
		
		return( false );
	}
	
	public static void
	createBetaChat(
		final String		network,
		final String		key,
		final Runnable		callback )
	{
		new AEThread2( "Chat create async" )
		{
			public void
			run()
			{
				try{
					BuddyPlugin bp = getPlugin();
					
					bp.getBeta().getAndShowChat( network, key );
					
				}catch( Throwable e ){
					
					Debug.out( e );
					
				}finally{
					
					callback.run();
				}
			}
		}.start();
	}
}
