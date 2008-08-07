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

import com.aelitis.azureus.core.metasearch.Engine;
import com.aelitis.azureus.core.metasearch.MetaSearchManagerFactory;
import com.aelitis.azureus.core.metasearch.Result;
import com.aelitis.azureus.core.metasearch.SearchParameter;
import com.aelitis.azureus.core.subs.*;
import com.aelitis.azureus.util.JSONUtils;

public class 
SubscriptionDownloader 
{
	protected
	SubscriptionDownloader(
		SubscriptionImpl		subs )
	
		throws SubscriptionException
	{
		Map map = JSONUtils.decodeJSON( subs.getJSON());
		
		Long 	engine_id 	= (Long)map.get( "engine_id" );
		String	search_term	= (String)map.get( "search_term" );
		Map		filters		= (Map)map.get( "filters" );

		Engine engine = MetaSearchManagerFactory.getSingleton().getMetaSearch().getEngine( engine_id.intValue());
		
		List	sps = new ArrayList();
		
		sps.add( new SearchParameter( "s", search_term ));
		
		/*
		if ( mature != null ){
			
			sps.add( new SearchParameter( "m", mature.toString()));
		}
		*/
		
		SearchParameter[] parameters = (SearchParameter[])sps.toArray(new SearchParameter[ sps.size()] );

		try{
		
			Result[] results = engine.search( parameters );
			
		}catch( Throwable e ){
			
			throw( new SubscriptionException( "Search failed", e ));
		}
	}
}
