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

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SHA1Simple;

import com.aelitis.azureus.core.metasearch.Result;
import com.aelitis.azureus.core.subs.SubscriptionResult;
import com.aelitis.azureus.util.JSONUtils;

public class 
SubscriptionResultImpl 
	implements SubscriptionResult
{
	private byte[]		key;
	private boolean		read;
	private String		result_json;
	
	protected
	SubscriptionResultImpl(
		Result		result )
	{
		Map	map = result.toJSONMap();
		
		result_json 	= JSONUtils.encodeToJSON( map );
		read			= false;
		
		String	key_str =  result.getName() + ":" + result.getSize() + ":" + result.getPublishedDate();
		
		try{
			key = new SHA1Simple().calculateHash( key_str.getBytes( "UTF-8" ));
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	
	protected 
	SubscriptionResultImpl(
		Map		map )
	{
		key			= (byte[])map.get( "key" );
		read		= ((Long)map.get( "read")).intValue()==1;
		
		try{
			result_json	= new String((byte[])map.get( "result_json" ), "UTF-8" );
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	
	protected Map
	toBEncodedMap()
	{
		Map		map	= new HashMap();
		
		map.put( "key", key );
		map.put( "read", new Long(read?1:0));
		
		try{
			map.put( "result_json", result_json.getBytes( "UTF-8" ));
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
		
		return( map );
	}
	
	public Map
	toJSONMap()
	{
		Map	map = JSONUtils.decodeJSON( result_json );
		
			// TODO: augment with extra
		
		return( map );
	}
}
