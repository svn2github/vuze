/*
 * Created on Aug 7, 2008
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
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SHA1Simple;

import com.aelitis.azureus.core.metasearch.Result;
import com.aelitis.azureus.core.subs.SubscriptionResult;
import com.aelitis.azureus.util.JSONUtils;

public class 
SubscriptionResultImpl 
	implements SubscriptionResult
{
	final private SubscriptionHistoryImpl	history;
	
	private byte[]		key;
	private boolean		read;
	private boolean		deleted;
	
	private String		result_json;
	
	protected
	SubscriptionResultImpl(
		SubscriptionHistoryImpl		_history,
		Result						result )
	{
		history = _history;
		
		Map	map = result.toJSONMap();
		
		result_json 	= JSONUtils.encodeToJSON( map );
		read			= false;
		
		String	key_str =  result.getEngine().getId() + ":" + result.getName();
		
		try{
			byte[] sha1 = new SHA1Simple().calculateHash( key_str.getBytes( "UTF-8" ));
			
			key = new byte[10];
			
			System.arraycopy( sha1, 0, key, 0, 10 );
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	
	protected 
	SubscriptionResultImpl(
		SubscriptionHistoryImpl		_history,
		Map							map )
	{
		history = _history;
		
		key			= (byte[])map.get( "key" );
		read		= ((Long)map.get( "read")).intValue()==1;
		
		Long	l_deleted = (Long)map.get( "deleted" );
		
		if ( l_deleted != null ){
			
			deleted	= true;
			
		}else{
		
			try{
				result_json	= new String((byte[])map.get( "result_json" ), "UTF-8" );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
	protected boolean
	updateFrom(
		SubscriptionResultImpl	other )
	{
		if ( deleted ){
			
			return( false );
		}
		
		if ( getJSON().equals( other.getJSON())){
			
			return( false );
			
		}else{
			
			result_json = other.getJSON();
			
			return( true );
		}
	}
	
	public String
	getID()
	{
		return( Base32.encode( key ));
	}
	
	protected byte[]
	getKey()
	{
		return( key );
	}
	
	public boolean
	getRead()
	{
		return( read );
	}
	
	public void
	setRead(
		boolean	_read )
	{
		if ( read != _read ){
			
			read	= _read;
			
			history.updateResult( this );
		}
	}
	
	protected void
	setReadInternal(
		boolean	_read )
	{
		read	= _read;
	}
	
	public void
	delete()
	{
		if ( !deleted ){
			
			deleted	= true;
			
			history.updateResult( this );
		}
	}
	
	protected void
	deleteInternal()
	{
		deleted = true;
	}
	
	public boolean
	isDeleted()
	{
		return( deleted );
	}
	
	protected Map
	toBEncodedMap()
	{
		Map		map	= new HashMap();
		
		map.put( "key", key );
		map.put( "read", new Long(read?1:0));
		
		if ( deleted ){
			
			map.put( "deleted", new Long(1));
			
		}else{
		
			try{
				map.put( "result_json", result_json.getBytes( "UTF-8" ));
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
		
		return( map );
	}
	
	public Map
	toJSONMap()
	{
		Map	map = JSONUtils.decodeJSON( result_json );
		
		map.put( "subs_is_read", new Boolean( read ));
		map.put( "subs_id", getID());
		
		return( map );
	}
	
	protected String
	getJSON()
	{
		return( result_json );
	}
	
	public String 
	getDownloadLink() 
	{
		return((String)toJSONMap().get( "dl" ));
	}
	
	public String 
	getPlayLink() 
	{
		return((String)toJSONMap().get( "pl" ));
	}
}
