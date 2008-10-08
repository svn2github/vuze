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

import com.aelitis.azureus.core.metasearch.Engine;
import com.aelitis.azureus.core.vuzefile.VuzeFile;

public interface 
Subscription 
{
	public String
	getName();
	
	public void
	setName(
		String		str )
	
		throws SubscriptionException;
	
	public String
	getID();
	
	public byte[]
	getPublicKey();
	
	public int
	getVersion();
	
	public boolean
	isMine();
	
	public boolean
	isPublic();
	
	public boolean
	isUpdateable();
	
	public boolean
	isShareable();
	
	public String
	getJSON()
	
		throws SubscriptionException;
	
	public boolean
	setJSON(
		String		json )
	
		throws SubscriptionException;
	
	public boolean
	isSubscribed();
	
	public void
	setSubscribed(
		boolean		subscribed );
	
	public void
	getPopularity(
		SubscriptionPopularityListener	listener )
	
		throws SubscriptionException;
	
	public boolean
	setDetails(
		String		name,
		boolean		is_public,
		String		json )
	
		throws SubscriptionException;
	
	public String
	getReferer();
	
	public long
	getCachedPopularity();
	
	public void
	addAssociation(
		byte[]		hash );
	
	public void
	addPotentialAssociation(
		String		result_id,
		String		key );

	public int
	getAssociationCount();
	
	public Engine
	getEngine()
	
		throws SubscriptionException;
	
	public VuzeFile
	getVuzeFile()
	
		throws SubscriptionException;
		
	public void
	remove();
	
	public SubscriptionManager
	getManager();
	
	public SubscriptionHistory
	getHistory();
	
	public void
	addListener(
		SubscriptionListener		l );
	
	public void
	removeListener(
		SubscriptionListener		l );
	
	public void
	setUserData(
		Object		key,
		Object		data );
	
	public Object
	getUserData(
		Object		key );
	
	public String
	getString();
}
