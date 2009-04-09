/*
 * Created on Apr 8, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.ui.swt.subscriptions;

import org.gudy.azureus2.ui.swt.IconBarEnabler;

import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.ui.swt.toolbar.ToolBarEnablerSelectedContent;

public class 
SubscriptionSelectedContent 
	extends ToolBarEnablerSelectedContent
{
	private Subscription		subs;
	
	protected
	SubscriptionSelectedContent(
		IconBarEnabler		_enabler,
		Subscription		_subs )
	{
		super( _enabler );
		
		subs	= _subs;
	}
	
	public Subscription
	getSubscription()
	{
		return( subs );
	}
	
	public String
	getHash()
	{
		return( subs.getID());
	}
}
