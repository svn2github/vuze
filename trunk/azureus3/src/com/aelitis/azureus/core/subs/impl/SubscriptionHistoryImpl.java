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

import java.util.*;

import org.gudy.azureus2.core3.util.ByteArrayHashMap;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.subs.SubscriptionHistory;
import com.aelitis.azureus.core.subs.SubscriptionResult;

public class 
SubscriptionHistoryImpl
	implements SubscriptionHistory
{
	private SubscriptionManagerImpl		manager;
	private SubscriptionImpl			subs;
	
	private long		last_scan;
	private int			num_unread;
	private int			num_read;
	
	protected
	SubscriptionHistoryImpl(
		SubscriptionManagerImpl		_manager,
		SubscriptionImpl			_subs )
	{
		manager		= _manager;
		subs		= _subs;
		
		loadConfig();
	}
	
	protected void
	reconcileResults(
		SubscriptionResultImpl[]		latest_results )
	{
		boolean	changed;
		
		int	new_unread 	= 0;
		int new_read	= 0;
		
		synchronized( this ){
			
			SubscriptionResultImpl[] existing_results = manager.loadResults( subs );
		
			changed = latest_results.length != existing_results.length;
			
			ByteArrayHashMap	map = new ByteArrayHashMap();
			
			for (int i=0;i<existing_results.length;i++){
				
				SubscriptionResultImpl r = existing_results[i];
				
				map.put( r.getKey(), r );
			}
			
			for (int i=0;i<latest_results.length;i++){

				SubscriptionResultImpl r = existing_results[i];

				SubscriptionResultImpl e = (SubscriptionResultImpl)map.get( r.getKey());
				
				if ( e == null ){
					
					changed = true;
					
				}else{
					
					latest_results[i] = e;
				}
				
				if ( latest_results[i].getRead()){
					
					new_read++;
					
				}else{
					
					new_unread++;
				}
			}
			
			if ( changed ){
				
				manager.saveResults( subs, latest_results );
			}
		}
		
		last_scan 	= SystemTime.getCurrentTime();
		num_unread	= new_unread;
		num_read	= new_read;
		
			// always save config as we have a new scan time
		
		saveConfig();
	}
	

	public long
	getLastScanTime()
	{
		return( last_scan );
	}
	
	public int
	getNumUnread()
	{
		return( num_unread );
	}
	
	public int
	getNumRead()
	{
		return( num_read );
	}
	
	public SubscriptionResult[]
	getResults()
	{
		return( manager.loadResults( subs ));
	}
	
	protected void
	loadConfig()
	{
		Map	map = subs.getHistoryConfig();
		
		Long	l_last_scan = (Long)map.get( "last_scan" );		
		last_scan			= l_last_scan==null?0:l_last_scan.longValue();
		
		Long	l_num_unread 	= (Long)map.get( "num_unread" );		
		num_unread				= l_num_unread==null?0:l_num_unread.intValue();

		Long	l_num_read 	= (Long)map.get( "num_read" );		
		num_read			= l_num_read==null?0:l_num_read.intValue();
	}
	
	protected void
	saveConfig()
	{
		Map	map = new HashMap();
		
		map.put( "last_scan", new Long( last_scan ));
		map.put( "num_unread", new Long( num_unread ));
		map.put( "num_read", new Long( num_read ));
		
		subs.updateHistoryConfig( map );
	}
}
