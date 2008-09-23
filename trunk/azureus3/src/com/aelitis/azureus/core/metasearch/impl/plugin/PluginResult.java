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

import java.util.Date;

import org.gudy.azureus2.plugins.utils.search.SearchResult;

import com.aelitis.azureus.core.metasearch.*;

public class 
PluginResult 
	extends Result
{
	private SearchResult			result;
	private String					search_term;
	
	protected
	PluginResult(
		PluginEngine		_engine,
		SearchResult		_result,
		String				_search_term )
	{
		super( _engine );
		
		result			= _result;
		search_term		= _search_term;
	}
	
	public Date
	getPublishedDate()
	{
		return((Date)result.getProperty( SearchResult.PR_PUB_DATE ));
	}
	
	public String 
	getCategory()
	{
		return(getStringProperty( SearchResult.PR_CATEGORY ));
	}
	
	public void 
	setCategory(
		String category )
	{	
	}
	
	public String 
	getContentType()
	{
		return(getStringProperty( SearchResult.PR_CONTENT_TYPE ));

	}
	
	public void 
	setContentType(
		String contentType )
	{	
	}
	
	public String 
	getName()
	{
		return(getStringProperty( SearchResult.PR_NAME ));
	}
	
	public long 
	getSize()
	{
		return(getLongProperty( SearchResult.PR_SIZE ));
	}
	
	public int 
	getNbPeers()
	{
		return(getIntProperty( SearchResult.PR_LEECHER_COUNT ));
	}
	
	public int 
	getNbSeeds()
	{
		return(getIntProperty( SearchResult.PR_SEED_COUNT ));
	}
	
	public int 
	getNbSuperSeeds()
	{
		return(getIntProperty( SearchResult.PR_SUPER_SEED_COUNT ));
	}
	
	public int 
	getComments()
	{
		return(getIntProperty( SearchResult.PR_COMMENTS ));
	}
	
	public int 
	getVotes()
	{
		return(getIntProperty( SearchResult.PR_VOTES ));
	}
	
	public boolean 
	isPrivate()
	{
		return( getBooleanProperty( SearchResult.PR_PRIVATE ));
	}
	
	
	public String 
	getDRMKey()
	{
		return(getStringProperty( SearchResult.PR_DRM_KEY ));
	}
	
	public String 
	getDownloadLink()
	{
		return(getStringProperty( SearchResult.PR_DOWNLOAD_LINK ));
	}
	
	public String 
	getDownloadButtonLink()
	{
		return(getStringProperty( SearchResult.PR_DOWNLOAD_BUTTON_LINK ));
	}
	
	public String 
	getCDPLink()
	{
		return( getStringProperty( SearchResult.PR_DETAILS_LINK ));

	}
	
	public String 
	getPlayLink()
	{
		return(getStringProperty( SearchResult.PR_PLAY_LINK ));
	}
	
	public float 
	getRank() 
	{
		Long	l_rank = (Long)result.getProperty( SearchResult.PR_RANK );
		
		if ( l_rank == null ){
			
			return( super.getRank());
		}
		
		float	rank = l_rank.longValue();
		
		if ( rank > 100 ){
			
			rank = 100;
			
		}else if ( rank < 0 ){
			
			rank = 0;
		}
		
		return( rank / 100 );
	}
	
	public String 
	getSearchQuery()
	{
		return( search_term );
	}
	
	protected int
	getIntProperty(
		int		name )
	{
		return((int)getLongProperty( name ));
	}
	
	protected int
	getLongProperty(
		int		name )
	{
		Long	l = (Long)result.getProperty( name );
		
		if ( l == null ){
			
			return( -1 );
		}
		
		return( l.intValue());
	}
	
	protected boolean
	getBooleanProperty(
		int		name )
	{
		Boolean	b = (Boolean)result.getProperty( name );
		
		if ( b == null ){
			
			return( false );
		}
		
		return( b.booleanValue());
	}
	
	protected String
	getStringProperty(
		int		name )
	{
		String	l = (String)result.getProperty( name );
		
		if ( l == null ){
			
			return( "" );
		}
		
		return( l );
	}
	
}
