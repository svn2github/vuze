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

import org.gudy.azureus2.plugins.utils.search.SearchResult;

import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.core.subs.SubscriptionResult;

public class 
SBC_SubscriptionResult 
{
	private final Subscription		subs;
	private final String			result_id;
	
	private final String			name;
	private final byte[]			hash;
	private final long				size;
	private final long				time;
	private final String			torrent_link;
	private final String			details_link;
	
	protected
	SBC_SubscriptionResult(
		Subscription		_subs,
		SubscriptionResult	_result )
	{
		subs		= _subs;
		result_id	= _result.getID();
		
		Map<Integer,Object>	properties = _result.toPropertyMap();
		
		name = (String)properties.get( SearchResult.PR_NAME );
		
		hash = (byte[])properties.get( SearchResult.PR_HASH );
		
		size = (Long)properties.get( SearchResult.PR_SIZE );
		
		Date pub_date = (Date)properties.get( SearchResult.PR_PUB_DATE );
		
		if ( pub_date == null ){
			
			time = _result.getTimeFound();
			
		}else{
			
			long pt = pub_date.getTime();
			
			if ( pt <= 0 ){
				
				time = _result.getTimeFound();
				
			}else{
			
				time = pt;
			};
		}
		
		torrent_link = (String)properties.get( SearchResult.PR_TORRENT_LINK );
		details_link = (String)properties.get( SearchResult.PR_DETAILS_LINK );

	}
	
	public Subscription
	getSubscription()
	{
		return( subs );
	}
	
	public String
	getID()
	{
		return( result_id );
	}
	
	public final String
	getName()
	{
		return( name );
	}
	
	public byte[]
	getHash()
	{
		return( hash );
	}
	
	public long
	getSize()
	{
		return( size );
	}
	
	public String
	getTorrentLink()
	{
		return( torrent_link );
	}
	
	public String
	getDetgailsLink()
	{
		return( details_link );
	}
	
	public long
	getTime()
	{
		return( time );
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
	
	public void
	delete()
	{
		SubscriptionResult result = subs.getHistory().getResult( result_id );
		
		if ( result != null ){
			
			result.delete();
		}
	}
}
