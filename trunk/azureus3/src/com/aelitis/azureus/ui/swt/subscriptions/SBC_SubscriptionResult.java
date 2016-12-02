/*
 * Created on Dec 2, 2016
 * Created by Paul Gardner
 * 
 * Copyright 2016 Azureus Software, Inc.  All rights reserved.
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


package com.aelitis.azureus.ui.swt.subscriptions;

import java.util.*;

import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.core.subs.SubscriptionResult;

public class 
SBC_SubscriptionResult 
{
	private final Subscription		subs;
	private final String			result_id;
	
	protected
	SBC_SubscriptionResult(
		Subscription		_subs,
		SubscriptionResult	_result )
	{
		subs		= _subs;
		result_id	= _result.getID();
	}
	
	public long
	getTimeFound()
	{
		SubscriptionResult result = subs.getHistory().getResult( result_id );
		
		if ( result != null ){
			
			return( result.getTimeFound());
		}
		
		return( 0 );
	}
	
	public boolean
	getRead()
	{
		SubscriptionResult result = subs.getHistory().getResult( result_id );
		
		if ( result != null ){
			
			return( result.getRead());
		}
		
		return( true );
	}
	
	public void
	setRead(
		boolean		read )
	{
		SubscriptionResult result = subs.getHistory().getResult( result_id );
		
		if ( result != null ){
			
			result.setRead( read );
		}
	}
	
	public Map<Integer,Object>
	toPropertyMap()
	{
		SubscriptionResult result = subs.getHistory().getResult( result_id );
		
		if ( result != null ){
			
			return( result.toPropertyMap());
		}
		
		return( new HashMap<Integer,Object>());
	}
	
	public void
	delete()
	{
		SubscriptionResult result = subs.getHistory().getResult( result_id );
		
		if ( result != null ){
			
			result.delete();
		}
	}
}
