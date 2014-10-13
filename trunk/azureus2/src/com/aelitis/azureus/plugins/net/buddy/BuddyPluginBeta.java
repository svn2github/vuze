/*
 * Created on Oct 13, 2014
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

import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.util.AENetworkClassifier;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginEvent;
import org.gudy.azureus2.plugins.PluginEventListener;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;

public class
BuddyPluginBeta 
{
	private PluginInterface		plugin_interface;
	private BooleanParameter	enabled;
	
	private AsyncDispatcher		dispatcher = new AsyncDispatcher( "BuddyPluginBeta" );
	
	protected
	BuddyPluginBeta(
		PluginInterface		_pi,
		BooleanParameter	_enabled )
	{
		plugin_interface 	= _pi;
		enabled				= _enabled;
	}
	
	protected void
	startup()
	{						
		plugin_interface.addEventListener(
			new PluginEventListener()
			{
				public void 
				handleEvent(
					PluginEvent ev )
				{
					int	type = ev.getType();
					
					if ( type == PluginEvent.PEV_PLUGIN_OPERATIONAL ){
						
						pluginAdded((PluginInterface)ev.getValue());
					}
					if ( type == PluginEvent.PEV_PLUGIN_NOT_OPERATIONAL ){
						
						pluginRemoved((PluginInterface)ev.getValue());
					}
				}
			});
		
		PluginInterface[] plugins = plugin_interface.getPluginManager().getPlugins( true );
		
		for ( PluginInterface pi: plugins ){
			
			if ( pi.getPluginState().isOperational()){
			
				pluginAdded( pi );
			}
		}
	}
	
	protected void
	closedown()
	{
		
	}
	
	private void
	pluginAdded(
		final PluginInterface	pi )
	{
		if ( pi.getPluginID().equals( "azmsgsync" )){
			
			dispatcher.dispatch(
				new AERunnable() {
					
					@Override
					public void 
					runSupport() 
					{
						try{
							Map<String,Object>		options = new HashMap<String, Object>();
							
							options.put( "network", AENetworkClassifier.AT_PUBLIC );
							options.put( "key", "test:beta:chat".getBytes( "UTF-8" ));
								
							Map<String,Object> reply = (Map<String,Object>)pi.getIPC().invoke(	"getMessageHandler", new Object[]{ options } );
							
							System.out.println( "reply: " + reply );
							
						}catch( Throwable e ){
							
							Debug.out( e );
						}
					}
				});

		}
	}
	
	private void
	pluginRemoved(
		PluginInterface	pi )
	{
		if ( pi.getPluginID().equals( "azmsgsync" )){
			
		}
	}
}
