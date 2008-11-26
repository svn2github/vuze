/*
 * Created on Nov 20, 2008
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


package com.aelitis.azureus.core.cnetwork.impl;

import java.io.IOException;
import java.util.*;

import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.cnetwork.*;
import com.aelitis.azureus.core.vuzefile.VuzeFile;
import com.aelitis.azureus.core.vuzefile.VuzeFileComponent;
import com.aelitis.azureus.core.vuzefile.VuzeFileHandler;
import com.aelitis.azureus.util.ImportExportUtils;

public abstract class 
ContentNetworkImpl
	implements ContentNetwork
{
	protected static final long	TYPE_VUZE_GENERIC		= 1;
	
	protected static ContentNetworkImpl
	importFromBencodedMap(
		Map		map )
	
		throws IOException
	{
		long type	= ImportExportUtils.importLong( map, "type" );
		
		if ( type == TYPE_VUZE_GENERIC ){
			
			return( new ContentNetworkVuzeGeneric( map ));
			
		}else{
		
			throw( new IOException( "Unsupported network type: " + type ));
		}
	}
	
	private long		type;
	private long		version;
	private long		id;
	private String		name;
	
	protected
	ContentNetworkImpl(
		long			_type,
		long			_id,
		long			_version,
		String			_name )
	{
		type		= _type;
		version		= _version;
		id			= _id;
		name		= _name;
	}
	
	protected
	ContentNetworkImpl(
		Map		map )
	
		throws IOException
	{
		type	= ImportExportUtils.importLong( map, "type" );
		id		= ImportExportUtils.importLong( map, "id" );
		version	= ImportExportUtils.importLong( map, "version" );
		name 	= ImportExportUtils.importString( map, "name" );
	}
	
	protected void
	exportToBencodedMap(
		Map			map )
	
		throws IOException
	{
		ImportExportUtils.exportLong( map, "type", type );
		ImportExportUtils.exportLong( map, "id", id );
		ImportExportUtils.exportLong( map, "version", version );
		ImportExportUtils.exportString( map, "name", name );
	}
	
	public long 
	getID() 
	{
		return( id );
	}
	
	protected long
	getVersion()
	{
		return( version );
	}
	
	public String
	getName()
	{
		return( name );
	}
	
	protected boolean
	isSameAs(
		ContentNetworkImpl		other )
	{
		try{
			Map	map1 = new HashMap();
			Map map2 = new HashMap();
			
			exportToBencodedMap( map1 );
			
			other.exportToBencodedMap( map2 );
			
			return( BEncoder.mapsAreIdentical( map1, map2 ));
			
		}catch( Throwable e ){
			
			Debug.out( e );
			
			return( false );
		}
	}
	
	public boolean 
	isServiceSupported(
		int service_type )
	{
		return( getServiceURL( service_type ) != null );
	}
		
	public String
	getSearchService(
		String		query )
	{
		return( getServiceURL( SERVICE_SEARCH, new Object[]{ query } ));
	}
	
	public String
	getXSearchService(
		String		query,
		boolean		to_subscribe )
	{
		return( getServiceURL( SERVICE_XSEARCH, new Object[]{ query, to_subscribe } ));
	}
	
	public String 
	getContentDetailsService(
		String 		hash, 
		String 		client_ref ) 
	{
		return( getServiceURL( SERVICE_CONTENT_DETAILS, new Object[]{ hash, client_ref }));
	}
	
	public String 
	getCommentService(
		String hash )
	{
		return( getServiceURL( SERVICE_COMMENT, new Object[]{ hash }));
	}
	
	public String 
	getProfileService(
		String 		login_id, 
		String 		client_ref ) 
	{
		return( getServiceURL( SERVICE_PROFILE, new Object[]{ login_id, client_ref }));
	}
	
	public String 
	getTorrentDownloadService(
		String 		hash, 
		String 		client_ref ) 
	{
		return( getServiceURL( SERVICE_TORRENT_DOWNLOAD, new Object[]{ hash, client_ref }));
	}
	
	public String 
	getFAQTopicService(
		String topic )
	{
		return( getServiceURL( SERVICE_FAQ_TOPIC, new Object[]{ topic }));
	}
	
	public String 
	getLoginService(
		String 	message )
	{
		return( getServiceURL( SERVICE_LOGIN, new Object[]{ message }));
	}
	
	public String 
	getSiteRelativeURL(
		String 		relative_url,
		boolean		append_suffix )
	{
		return( getServiceURL( SERVICE_SITE_RELATIVE, new Object[]{ relative_url, append_suffix }));
	}
	
	public String 
	getAddFriendURL(
		String 	colour )
	{
		return( getServiceURL( SERVICE_ADD_FRIEND, new Object[]{ colour }));
	}
	
	public String 
	getSubscriptionURL(
		String 	subs_id )
	{
		return( getServiceURL( SERVICE_SUBSCRIPTION, new Object[]{ subs_id }));
	}
	
	public VuzeFile
	getVuzeFile()
	{
		VuzeFile	vf = VuzeFileHandler.getSingleton().create();
		
		Map	map = new HashMap();
		
		try{
			exportToBencodedMap( map );
		
			vf.addComponent( VuzeFileComponent.COMP_TYPE_CONTENT_NETWORK, map );
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
		}
		
		return( vf );
	}
	
	protected String
	getString()
	{
		return( getName() + ": version=" + getVersion() + ", site=" + getProperty( PROPERTY_SITE_HOST ));
	}
}
