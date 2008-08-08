/*
 * Created on Jun 20, 2008
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


package com.aelitis.azureus.core.metasearch.impl.plugin;

import java.io.IOException;
import java.util.*;

import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.utils.search.SearchInstance;
import org.gudy.azureus2.plugins.utils.search.SearchObserver;
import org.gudy.azureus2.plugins.utils.search.SearchProvider;
import org.gudy.azureus2.plugins.utils.search.SearchResult;

import com.aelitis.azureus.core.metasearch.*;
import com.aelitis.azureus.core.metasearch.impl.*;

public class 
PluginEngine
	extends EngineImpl
{
	public static EngineImpl
	importFromBEncodedMap(
		MetaSearchImpl		meta_search,
		Map					map )
	
		throws IOException
	{
		return( new PluginEngine( meta_search, map ));
	}
	
	private SearchProvider			provider;
	
	public
	PluginEngine(
		MetaSearchImpl		_meta_search,
		long				_id,
		SearchProvider		_provider )
	{
		super( _meta_search, Engine.ENGINE_TYPE_PLUGIN, _id, 0, (String)_provider.getProperty( SearchProvider.PR_NAME ));
		
		provider	= _provider;
		
		setSource( ENGINE_SOURCE_LOCAL );
	}
	
	protected
	PluginEngine(
		MetaSearchImpl		_meta_search,
		Map					_map )
	
		throws IOException
	{
		super( _meta_search, _map );
		
		setSource( ENGINE_SOURCE_LOCAL );
	}
	
	public Map 
	exportToBencodedMap() 
	
		throws IOException 
	{
		Map	res = new HashMap();
				
		super.exportToBencodedMap( res );
		
		return( res );
	}
	
	public void
	setProvider(
		SearchProvider		_provider )
	{
		provider	= _provider;
	}
	
	public boolean
	isActive()
	{
		return( provider != null && super.isActive());
	}
	
	public String 
	getDownloadLinkCSS() 
	{
		if ( provider == null ){
			
			return( null );
		}
		
		return((String)provider.getProperty( SearchProvider.PR_DOWNLOAD_LINK_LOCATOR ));
	}
	
	public String 
	getIcon() 
	{
		if ( provider == null ){
			
			return( null );
		}
		
		return((String)provider.getProperty( SearchProvider.PR_ICON_URL ));
	}
	
	public String 
	getReferer() 
	{
		if ( provider == null ){
			
			return( null );
		}
		
		return((String)provider.getProperty( SearchProvider.PR_REFERER ));
	}
	
	protected Result[] 
	searchSupport(
		SearchParameter[] 	params, 
		int 				max_matches,
		String 				headers, 
		ResultListener 		listener )
	
		throws SearchException 
	{
		if ( provider == null ){
			
			return( new Result[0]  );
		}
		
		Map search_parameters = new HashMap();
		
		String	term = null;
		
		for (int i=0;i<params.length;i++){
			
			SearchParameter param = params[i];
			
			String pattern 	= param.getMatchPattern();
			String value	= param.getValue();
			
			if ( pattern.equals( "s" )){
				
				term = value;
				
				search_parameters.put( SearchProvider.SP_SEARCH_TERM, value );
				
			}else if ( pattern.equals( "m" )){
			
				search_parameters.put( SearchProvider.SP_MATURE, new Boolean(value ));

			}else{
				
				Debug.out( "Unrecognised search parameter '" + pattern + "=" + value + "' ignored" );
			}
		}
			
		final String f_term = term;
		
		try{
			final List	results = new ArrayList();
			
			final AESemaphore	sem = new AESemaphore( "waiter" );

			provider.search( 
				search_parameters,
				new SearchObserver()
				{
					public void 
					resultReceived(
						SearchInstance 		search,
						SearchResult 		result )
					{
						results.add( new PluginResult( PluginEngine.this, result, f_term ));
					}
					
					public void 
					cancelled() 
					{
						sem.release();
					}
					
					public void 
					complete() 
					{
						sem.release();
					}
				});
			
			sem.reserve();
			
			return((Result[])results.toArray(new Result[results.size()]));
			
		}catch( Throwable e ){
			
			throw( new SearchException( "Search failed", e ));
		}
	}
}
