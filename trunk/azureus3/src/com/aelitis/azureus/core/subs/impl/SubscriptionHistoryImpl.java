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

import org.gudy.azureus2.core3.util.Base32;
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
	
	private boolean		enabled;
	private boolean		auto_dl;
	
	private long		last_scan;
	private long		last_new_result;
	private int			num_unread;
	private int			num_read;
	
	private String			last_error;
	private boolean			auth_failed;
	

	protected
	SubscriptionHistoryImpl(
		SubscriptionManagerImpl		_manager,
		SubscriptionImpl			_subs )
	{
		manager		= _manager;
		subs		= _subs;
		
		loadConfig();
	}
	
	protected SubscriptionResultImpl[]
	reconcileResults(
		SubscriptionResultImpl[]		latest_results )
	{
		int	new_unread 	= 0;
		int new_read	= 0;
		
		if ( last_scan == 0 ){
			
				// first download of someone else's feed -> mark all existing as read
			
			if ( !subs.isMine()){
				
				for (int i=0;i<latest_results.length;i++){
					
					latest_results[i].setReadInternal(true);
				}
			}
		}
		
		long	now = SystemTime.getCurrentTime();
		
		SubscriptionResultImpl[] result;
		
		int	max_results = manager.getMaxNonDeletedResults();
		
		synchronized( this ){
			
			boolean	got_new_or_changed_result	= false;
			
			SubscriptionResultImpl[] existing_results = manager.loadResults( subs );
					
			ByteArrayHashMap	map = new ByteArrayHashMap();
			
			List	new_results = new ArrayList();

			for (int i=0;i<existing_results.length;i++){
				
				SubscriptionResultImpl r = existing_results[i];
				
				map.put( r.getKey(), r );
				
				new_results.add( r );
				
				if ( !r.isDeleted()){
					
					if ( r.getRead()){
						
						new_read++;
						
					}else{
						
						new_unread++;
					}
				}
			}
						
			for (int i=0;i<latest_results.length;i++){

				SubscriptionResultImpl r = latest_results[i];
				
				SubscriptionResultImpl existing = (SubscriptionResultImpl)map.get( r.getKey());
				
				if ( existing == null ){
					
					last_new_result = now;
					
					new_results.add( r );
					
					got_new_or_changed_result = true;
				
					if ( r.getRead()){
						
						new_read++;
						
					}else{
					
						new_unread++;
					}
				}else{
					
					if ( existing.updateFrom( r )){
						
						got_new_or_changed_result = true;
					}
				}
			}
			
				// see if we need to delete any old ones
			
			if ( max_results > 0 && (new_unread + new_read ) > max_results ){
				
				for (int i=0;i<new_results.size();i++){
					
					SubscriptionResultImpl r = (SubscriptionResultImpl)new_results.get(i);
					
					if ( !r.isDeleted()){
						
						if ( r.getRead()){
							
							new_read--;
							
						}else{
							
							new_unread--;
						}
						
						r.deleteInternal();
						
						got_new_or_changed_result = true;
						
						if (( new_unread + new_read ) <= max_results ){
							
							break;
						}
					}
				}
			}
			
			if ( got_new_or_changed_result ){
				
				result = (SubscriptionResultImpl[])new_results.toArray( new SubscriptionResultImpl[new_results.size()]);
				
				manager.saveResults( subs, result );
				
			}else{
				
				result = existing_results;
			}
		
			last_scan 	= now;
			num_unread	= new_unread;
			num_read	= new_read;
		}
		
			// always save config as we have a new scan time
		
		saveConfig();
		
		return( result );
	}
	
	public boolean
	isEnabled()
	{
		return( enabled );
	}
	
	public void
	setEnabled(
		boolean		_enabled )
	{
		if ( _enabled != enabled ){
			
			enabled	= _enabled;
		
			saveConfig();
		}
	}
	
	public boolean
	isAutoDownload()
	{
		return( auto_dl );
	}
	
	public void
	setAutoDownload(
		boolean		_auto_dl )
	{
		if ( _auto_dl != auto_dl ){
			
			auto_dl	= _auto_dl;
		
			saveConfig();
		}
	}
	
	public void 
	setDetails(
		boolean 	_enabled, 
		boolean 	_auto_dl ) 
	{
		if ( enabled != _enabled || auto_dl != _auto_dl ){
			
			enabled	= _enabled;
			auto_dl	= _auto_dl;
			
			saveConfig();
		}
	}
	
	public long
	getLastScanTime()
	{
		return( last_scan );
	}
	
	public long 
	getLastNewResultTime() 
	{
		return( last_new_result );
	}
	
	public long
	getNextScanTime()
	{
		Map	schedule = subs.getScheduleConfig();
		
		if ( schedule.size() == 0  ){
			
			log( "Schedule is empty!");
			
			return( Long.MAX_VALUE );
			
		}else{
			
			try{
			
				long	interval_min = ((Long)schedule.get( "interval" )).longValue();
				
				return( last_scan + interval_min*60*1000 );
				
			}catch( Throwable e ){
				
				log( "Failed to decode schedule " + schedule, e );
				
				return( Long.MAX_VALUE );
			}
		}
	}
	
	public int
	getCheckFrequencyMins()
	{
		Map	schedule = subs.getScheduleConfig();
		
		if ( schedule.size() == 0  ){
			
			return( DEFAULT_CHECK_INTERVAL_MINS );
			
		}else{
			
			try{		
				int	interval_min = ((Long)schedule.get( "interval" )).intValue();
				
				return( interval_min );
				
			}catch( Throwable e ){
								
				return( DEFAULT_CHECK_INTERVAL_MINS );
			}
		}
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
	getResults(
		boolean		include_deleted )
	{
		SubscriptionResult[] results;
		
		synchronized( this ){
			
			results = manager.loadResults( subs );
		}
		
		if ( include_deleted ){
			
			return( results );
			
		}else{
			
			List	l = new ArrayList( results.length );
			
			for (int i=0;i<results.length;i++){
				
				if ( !results[i].isDeleted()){
					
					l.add( results[i] );
				}
			}
			
			return((SubscriptionResult[])l.toArray( new SubscriptionResult[l.size()]));
		}
	}
	
	public SubscriptionResult
	getResult(
		String		result_id )
	{
		SubscriptionResult[] results = getResults( true );
		
		for (int i=0;i<results.length;i++){
			
			if ( results[i].getID().equals( result_id )){
				
				return( results[i] );
			}
		}
		
		return( null );
	}
	
	protected void
	updateResult(
		SubscriptionResultImpl 	result )
	{
		byte[]	key = result.getKey();
		
		boolean	changed = false;

		synchronized( this ){
			
			SubscriptionResultImpl[] results = manager.loadResults( subs );
						
			for (int i=0;i<results.length;i++){
				
				if ( Arrays.equals( results[i].getKey(), key )){
					
					results[i] = result;
					
					changed	= true;
				}
			}
			
			if ( changed ){
				
				updateReadUnread( results );
				
				manager.saveResults( subs, results );
			}
		}
		
		if ( changed ){
			
			saveConfig();
		}
	}
	

	public void 
	deleteResults(
		String[] result_ids )
	{
		ByteArrayHashMap rids = new ByteArrayHashMap();
		
		for (int i=0;i<result_ids.length;i++){
			
			rids.put( Base32.decode( result_ids[i]), "" );
		}
	
		boolean	changed = false;

		synchronized( this ){
				
			SubscriptionResultImpl[] results = manager.loadResults( subs );

			for (int i=0;i<results.length;i++){
				
				SubscriptionResultImpl result = results[i];
				
				if ( !result.isDeleted() && rids.containsKey( result.getKey())){
					
					changed = true;
					
					result.deleteInternal();
				}
			}
			
			if ( changed ){
				
				updateReadUnread( results );
				
				manager.saveResults( subs, results );
			}
		}
		
		if ( changed ){
			
			saveConfig();
		}
	}
	
	public void
	deleteAllResults()
	{
		boolean	changed = false;
		
		synchronized( this ){
						
			SubscriptionResultImpl[] results = manager.loadResults( subs );

			for (int i=0;i<results.length;i++){
				
				SubscriptionResultImpl result = results[i];
				
				if ( !result.isDeleted()){
					
					changed = true;
				
					result.deleteInternal();
				}
			}
			
			if ( changed ){
				
				updateReadUnread( results );
				
				manager.saveResults( subs, results );
			}
		}
		
		if ( changed ){
			
			saveConfig();
		}
	}
	
	public void
	markAllResultsRead()
	{
		boolean	changed = false;
		
		synchronized( this ){
						
			SubscriptionResultImpl[] results = manager.loadResults( subs );

			for (int i=0;i<results.length;i++){
				
				SubscriptionResultImpl result = results[i];
				
				if ( !result.getRead()){
					
					changed = true;
				
					result.setReadInternal( true );
				}
			}
			
			if ( changed ){
				
				updateReadUnread( results );
				
				manager.saveResults( subs, results );
			}
		}
		
		if ( changed ){
			
			saveConfig();
		}
	}
	
	public void 
	markResults(
		String[] 		result_ids,
		boolean[]		reads )
	{
		ByteArrayHashMap rid_map = new ByteArrayHashMap();
		
		for (int i=0;i<result_ids.length;i++){
			
			rid_map.put( Base32.decode( result_ids[i]), new Boolean( reads[i] ));
		}
	
		boolean	changed = false;

		synchronized( this ){
						
			SubscriptionResultImpl[] results = manager.loadResults( subs );

			for (int i=0;i<results.length;i++){
				
				SubscriptionResultImpl result = results[i];
				
				Boolean	read = (Boolean)rid_map.get( result.getKey());
				
				if ( read != null ){
					
					if ( result.getRead() != read.booleanValue()){
						
						changed = true;
					
						result.setReadInternal( read.booleanValue());
					}
				}
			}
			
			if ( changed ){
				
				updateReadUnread( results );
				
				manager.saveResults( subs, results );
			}
		}
		
		if ( changed ){
			
			saveConfig();
		}
	}
	
	public void 
	reset() 
	{
		synchronized( this ){
			
			SubscriptionResultImpl[] results = manager.loadResults( subs );
			
			if ( results.length > 0 ){
				
				results = new SubscriptionResultImpl[0];
								
				manager.saveResults( subs, results );
			}
			
			updateReadUnread( results );
		}
		
		last_error		= "";
		last_new_result	= 0;
		last_scan		= 0;
					
		saveConfig();
	}
	
	protected void
	checkMaxResults(
		int		max_results )
	{
		if ( max_results <= 0 ){
			
			return;
		}
		
		boolean	changed = false;

		synchronized( this ){

			if ((num_unread + num_read ) > max_results ){

				SubscriptionResultImpl[] results = manager.loadResults( subs );
				
				for (int i=0;i<results.length;i++){
					
					SubscriptionResultImpl r = results[i];
					
					if ( !r.isDeleted()){
						
						if ( r.getRead()){
							
							num_read--;
							
						}else{
							
							num_unread--;
						}
						
						r.deleteInternal();
						
						changed = true;
						
						if (( num_unread + num_read ) <= max_results ){
							
							break;
						}
					}
				}
				
				if ( changed ){
					
					manager.saveResults( subs, results );
				}
			}
		}
		
		if ( changed ){
			
			saveConfig();
		}
	}
	
	protected void
	updateReadUnread(
		SubscriptionResultImpl[]	results )
	{
		int	new_unread	= 0;
		int	new_read	= 0;
		
		for (int i=0;i<results.length;i++){
			
			SubscriptionResultImpl result = results[i];
			
			if ( !result.isDeleted()){
				
				if ( result.getRead()){
					
					new_read++;
					
				}else{
					
					new_unread++;
				}
			}
		}
		
		num_read	= new_read;
		num_unread	= new_unread;
	}
	
	protected void
	setLastError(
		String		_last_error,
		boolean		_auth_failed )
	{
		last_error 	= _last_error;
		auth_failed	= _auth_failed;
		
		subs.fireChanged();
	}
	
	public String
	getLastError()
	{
		return( last_error );
	}
	
	public boolean
	isAuthFail()
	{
		return( auth_failed );
	}
	
	protected void
	loadConfig()
	{
		Map	map = subs.getHistoryConfig();
		
		Long	l_enabled	= (Long)map.get( "enabled" );		
		enabled				= l_enabled==null?true:l_enabled.longValue()==1;
		
		Long	l_auto_dl	= (Long)map.get( "auto_dl" );		
		auto_dl				= l_auto_dl==null?false:l_auto_dl.longValue()==1;

		Long	l_last_scan = (Long)map.get( "last_scan" );		
		last_scan			= l_last_scan==null?0:l_last_scan.longValue();
		
		Long	l_last_new 	= (Long)map.get( "last_new" );		
		last_new_result		= l_last_new==null?0:l_last_new.longValue();
		
		Long	l_num_unread 	= (Long)map.get( "num_unread" );		
		num_unread				= l_num_unread==null?0:l_num_unread.intValue();

		Long	l_num_read 	= (Long)map.get( "num_read" );		
		num_read			= l_num_read==null?0:l_num_read.intValue();
	}
	
	protected void
	saveConfig()
	{
		Map	map = new HashMap();
		
		map.put( "enabled", new Long( enabled?1:0 ));
		map.put( "auto_dl", new Long( auto_dl?1:0 ));
		map.put( "last_scan", new Long( last_scan ));
		map.put( "last_new", new Long( last_new_result ));
		map.put( "num_unread", new Long( num_unread ));
		map.put( "num_read", new Long( num_read ));
		
		subs.updateHistoryConfig( map );
	}
	
	protected void
	log(
		String		str )
	{
		subs.log( "History: " + str );
	}
	
	protected void
	log(
		String		str,
		Throwable	e )
	{
		subs.log( "History: " + str, e );
	}
}
