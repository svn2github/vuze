/*
 * Created on Jul 16, 2008
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


package com.aelitis.azureus.core.lws;

import java.io.File;
import java.net.URL;
import java.util.*;

import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.HashWrapper;


public class 
LightWeightSeedManager 
{
	private static LightWeightSeedManager singleton = new LightWeightSeedManager();
	
	public static LightWeightSeedManager
	getSingleton()
	{
		return( singleton );
	}
	
	
	private Map lws_map = new HashMap();
	
	public LightWeightSeed
	add(
		String					name,
		HashWrapper				hash,
		URL						url,
		File					data_location,
		LightWeightSeedAdapter	adapter )
	
		throws Exception
	{
		LightWeightSeed lws;
		
		synchronized( this ){
			
			if ( lws_map.containsKey( hash )){
				
				throw( new Exception( "Seed for hash '" + ByteFormatter.encodeString( hash.getBytes()) + "' already added" ));
			}
			
			lws = new LightWeightSeed(  this, name, hash, url, data_location, adapter );
			
			lws_map.put( hash, lws );
		}
		
		lws.start();
		
		return( lws );
	}
	
	protected void
	remove(
		LightWeightSeed		lws )
	{
		lws.stop();
		
		synchronized( this ){

			lws_map.remove( lws.getHash());
		}
	}
}
