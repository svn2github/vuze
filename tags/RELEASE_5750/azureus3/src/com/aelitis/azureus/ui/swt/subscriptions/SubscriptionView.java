/*
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.aelitis.azureus.ui.swt.subscriptions;

import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCoreEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCoreEventListenerEx;


public class
SubscriptionView
	implements SubscriptionsViewBase, UISWTViewCoreEventListenerEx
{
	private final SubscriptionsViewBase		impl;
	
	public
	SubscriptionView()
	{
		/*
		boolean	internal_subs = !COConfigurationManager.getBooleanParameter( "browser.external.subs" );
		
		if ( System.getProperty( "az.subs.native.results", "0" ).equals( "1" )){
			
			impl = new SubscriptionViewInternalNative();
			
		}else{
			
			impl = internal_subs?new SubscriptionViewInternalBrowser():new SubscriptionViewExternalBrowser();
		}
		*/
		
		impl = new SubscriptionViewInternalNative();
	}
	
	public boolean
	isCloneable()
	{
		return( true );
	}
	
	public UISWTViewCoreEventListener
	getClone()
	{
		return( new SubscriptionView());
	}
	
	public void 
	refreshView() 
	{
		impl.refreshView();
	}
	
	public boolean 
	eventOccurred(
		UISWTViewEvent event ) 
	{
		return( impl.eventOccurred( event ));
	}
}