/*
 * Created on May 6, 2008
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


package com.aelitis.azureus.core.metasearch.impl;

import java.util.*;

import com.aelitis.azureus.core.metasearch.*;
import com.aelitis.azureus.core.util.CopyOnWriteList;

public class 
MetaSearchImpl
	implements MetaSearch
{
	private static final MetaSearch singleton = new MetaSearchImpl();
	
	private CopyOnWriteList 	engines = new CopyOnWriteList();
	
	public static MetaSearch
	getSingleton()
	{
		return( singleton );
	}
	
	protected 
	MetaSearchImpl() 
	{
		try{
			Class clazz = Class.forName( "com.aelitis.azureus.core.metasearch.impl.MetaSearchTestImpl" );
		
			clazz.getConstructor( new Class[]{ MetaSearch.class }).newInstance( new Object[]{ this });
			
		} catch(Exception e) {
			//Test implementation in progress, Test class not publicly available
			e.printStackTrace();
		}
	}
	
	public void 
	addEngine(
		Engine 	engine )
	{
		engines.add( engine );
	}
	
	public void 
	removeEngine(
		Engine 	engine )
	{
		engines.remove( engine );
	}
	
	public Engine[] 
	getEngines() 
	{
		List l = engines.getList();
				
		return( (Engine[])l.toArray( new Engine[ l.size() ]));
	}
	
	public void 
	search(
		ResultListener 		listener,
		SearchParameter[] 	searchParameters ) 
	{
		SearchExecuter se = new SearchExecuter(listener);
		
		Iterator it  = engines.iterator();
		
		while( it.hasNext()){
			
			se.search((Engine)it.next(), searchParameters);
		}
	}
}
