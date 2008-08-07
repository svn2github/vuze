/*
 * Created on Aug 6, 2008
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


package com.aelitis.azureus.core.subs.impl;

import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.core.subs.SubscriptionException;
import com.aelitis.azureus.core.subs.SubscriptionManagerListener;
import com.aelitis.azureus.core.subs.SubscriptionScheduler;

public class 
SubscriptionSchedulerImpl 
	implements SubscriptionScheduler, SubscriptionManagerListener
{
	private SubscriptionManagerImpl		manager;
	
	protected
	SubscriptionSchedulerImpl(
		SubscriptionManagerImpl		_manager )
	{
		manager	= _manager;
		
		manager.addListener( this );
	}
	
	public void 
	download(
		Subscription subs )
	
		throws SubscriptionException 
	{
		new SubscriptionDownloader((SubscriptionImpl)subs );
	}
	
	public void
	subscriptionAdded(
		Subscription		subscription )
	{
		
	}
	
	public void
	subscriptionChanged(
		Subscription		subscription )
	{
		
	}
	
	public void
	subscriptionRemoved(
		Subscription		subscription )
	{
		
	}
	
	public void
	associationsChanged(
		byte[]				association_hash )
	{
		
	}
}
