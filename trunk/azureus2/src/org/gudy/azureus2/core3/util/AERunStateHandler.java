/*
 * Created on Sep 18, 2012
 * Created by Paul Gardner
 * 
 * Copyright 2012 Vuze, Inc.  All rights reserved.
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


package org.gudy.azureus2.core3.util;

import java.util.Iterator;

import org.gudy.azureus2.core3.config.COConfigurationManager;

import com.aelitis.azureus.core.util.CopyOnWriteList;

public class 
AERunStateHandler 
{
	private static boolean	delayed_start = COConfigurationManager.getBooleanParameter( "Start In Low Resource Mode" );
	
	private static AsyncDispatcher	dispatcher = new AsyncDispatcher(2500);
	
	private static CopyOnWriteList<ActivationListener>	listeners = new CopyOnWriteList<ActivationListener>();
	
	public static boolean
	isDelayedUI()
	{
		return( delayed_start );
	}
	
	public static boolean
	isUDPNetworkOnly()
	{
		return( delayed_start );
	}
	
	public static boolean
	isDHTSleeping()
	{
		return( delayed_start );
	}
	
	public static void
	setActivated()
	{
		synchronized( dispatcher ){
			
			if ( !delayed_start ){
				
				return;
			}
		
			delayed_start = false;
			
			final Iterator<ActivationListener> it = listeners.iterator();
			
			dispatcher.dispatch(
				new AERunnable()
				{
					public void
					runSupport()
					{
						while( it.hasNext()){
							
							try{								
								it.next().activated();
								
							}catch( Throwable e ){
								
								Debug.out( e );
							}
						}
						
					}
				});
		}
	}
	
	public static void
	addListener(
		final ActivationListener	l )
	{
		synchronized( dispatcher ){

			listeners.add( l );
			
			if ( !delayed_start ){
				
				dispatcher.dispatch(
					new AERunnable()
					{
						public void
						runSupport()
						{
							try{
								l.activated();

							}catch( Throwable e ){
								
								Debug.out( e );
							}
						}
					});
			}
		}
	}
	
	public interface
	ActivationListener
	{
		public void
		activated();
	}
}
