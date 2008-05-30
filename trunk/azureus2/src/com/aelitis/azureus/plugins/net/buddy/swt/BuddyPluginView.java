/*
 * Created on Mar 19, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
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


package com.aelitis.azureus.plugins.net.buddy.swt;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTStatusEntry;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;

import com.aelitis.azureus.plugins.net.buddy.BuddyPlugin;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginAZ2;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginAZ2Listener;
import com.aelitis.azureus.plugins.net.buddy.tracker.BuddyPluginTracker;
import com.aelitis.azureus.plugins.net.buddy.tracker.BuddyPluginTrackerListener;


public class 
BuddyPluginView
	implements UISWTViewEventListener
{
	private BuddyPlugin		plugin;
	private UISWTInstance	ui_instance;
	
	private BuddyPluginViewInstance		current_instance;
	
	private UISWTStatusEntry		status;
	
	public
	BuddyPluginView(
		BuddyPlugin		_plugin,
		UISWTInstance	_ui_instance )
	{
		plugin			= _plugin;
		ui_instance		= _ui_instance;
		
		plugin.getAZ2Handler().addListener(
			new BuddyPluginAZ2Listener()
			{
				public void
				chatCreated(
					final BuddyPluginAZ2.chatInstance		chat )
				{
					final Display display = ui_instance.getDisplay();
					
					if ( !display.isDisposed()){
						
						display.asyncExec(
							new Runnable()
							{
								public void
								run()
								{
									if ( !display.isDisposed()){
									
										new BuddyPluginViewChat( plugin, display, chat );
									}
								}
							});
					}
				}
				
				public void
				chatDestroyed(
					BuddyPluginAZ2.chatInstance		chat )
				{
				}
			});
				
		status = ui_instance.createStatusEntry();
		
		status.setText( "BBB" );
		status.setTooltipText( "Idle" );
		status.setImage( UISWTStatusEntry.IMAGE_LED_GREY );
		status.setImageEnabled( true );
		
		status.setVisible( true );
		
		plugin.getTracker().addListener(
			new BuddyPluginTrackerListener()
			{
				public void
				networkStatusChanged(
					BuddyPluginTracker	tracker,
					int					new_status )
				{
					if ( new_status == BuddyPluginTracker.BUDDY_NETWORK_IDLE ){
						
						status.setImage( UISWTStatusEntry.IMAGE_LED_GREY );
						status.setTooltipText( "Idle" );
						
					}else if ( new_status == BuddyPluginTracker.BUDDY_NETWORK_INBOUND ){
						
						status.setImage( UISWTStatusEntry.IMAGE_LED_GREEN );
						status.setTooltipText( "Incoming" );
					}else{
						
						status.setImage( UISWTStatusEntry.IMAGE_LED_YELLOW );
						status.setTooltipText( "Outgoing" );
					}
				}
			});
	}
	
	public boolean 
	eventOccurred(
		UISWTViewEvent event )
	{
		switch( event.getType() ){

			case UISWTViewEvent.TYPE_CREATE:{
				
				if ( current_instance != null ){
					
					return( false );
				}
								
				break;
			}
			case UISWTViewEvent.TYPE_INITIALIZE:{
				
				current_instance = new BuddyPluginViewInstance(plugin, ui_instance, (Composite)event.getData());
				
				break;
			}
			case UISWTViewEvent.TYPE_CLOSE:
			case UISWTViewEvent.TYPE_DESTROY:{
				
				try{
					if ( current_instance != null ){
						
						current_instance.destroy();
					}
				}finally{
					
					current_instance = null;
				}
				
				break;
			}
		}
		
		return true;
	}
}
