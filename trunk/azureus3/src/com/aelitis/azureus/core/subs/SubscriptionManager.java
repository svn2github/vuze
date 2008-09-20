/*
 * Created on Jul 11, 2008
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


package com.aelitis.azureus.core.subs;

import java.net.URL;


public interface 
SubscriptionManager 
{
	public Subscription
	create(
		String		name,
		boolean		is_public,
		String		json )
		
		throws SubscriptionException;
	
	/*
	public Subscription
	createRSS(
		String		name,
		URL			url,
		int			check_interval_mins )
		
		throws SubscriptionException;
	*/
	
		// creates a subscription that will always have the same identity for the given parameters
		// and can't be updated
	
	public Subscription
	createSingletonRSS(
		String		name,
		URL			url,
		int			check_interval_mins,
		boolean		add_to_subscriptions )
		
		throws SubscriptionException;
	
	public Subscription[]
	getSubscriptions();
	
	public Subscription
	getSubscriptionByID(
		String			id );
	
		/**
		 * Full lookup
		 * @param hash
		 * @param listener
		 * @return
		 * @throws SubscriptionException
		 */
	
	public SubscriptionAssociationLookup
	lookupAssociations(
		byte[]						hash,
		SubscriptionLookupListener	listener )
	
		throws SubscriptionException;
	
		/**
		 * Cached view of hash's subs
		 * @param hash
		 * @return
		 */
	
	public Subscription[]
	getKnownSubscriptions(
		byte[]						hash );
	
	public SubscriptionScheduler
	getScheduler();
	
	public int
	getMaxNonDeletedResults();
	
	public void
	setMaxNonDeletedResults(
		int		max );
	
	public void
	addListener(
		SubscriptionManagerListener	listener );
	
	public void
	removeListener(
		SubscriptionManagerListener	listener );
}
