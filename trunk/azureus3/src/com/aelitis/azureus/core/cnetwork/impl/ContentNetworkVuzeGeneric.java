/*
 * Created on Nov 25, 2008
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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.UrlUtils;

import com.aelitis.azureus.core.crypto.VuzeCryptoManager;
import com.aelitis.azureus.util.ImportExportUtils;

public class 
ContentNetworkVuzeGeneric 
	extends ContentNetworkImpl
{
	private static String URL_SUFFIX;

	static{
		COConfigurationManager.addAndFireParameterListener(
			"locale",
			new ParameterListener(){
				public void 
				parameterChanged(
					String parameterName )
				{	
						// Don't change the order of the params as there's some code somewhere
						// that depends on them (I think its code that removes the azid so
						// we can fix this up when that code's migrated here I guess
					
					URL_SUFFIX = 	"azid=" 	+ Base32.encode(VuzeCryptoManager.getSingleton().getPlatformAZID()) +
									"&azv=" 	+ Constants.AZUREUS_VERSION +
									"&locale=" 	+ Locale.getDefault().toString();
				}
			});
	}

	private Map<Integer, String>		service_map = new HashMap<Integer, String>();


	private String	SITE_HOST;
	private String	URL_PREFIX;
	private String	URL_ICON;
	private String	URL_RELAY_RPC;
	private String	URL_AUTHORIZED_RPC;
	private String	URL_FAQ;
	private String	URL_BLOG;
	private String	URL_FORUMS;
	private String	URL_WIKI;

	protected
	ContentNetworkVuzeGeneric(
		ContentNetworkManagerImpl	_manager,
		long						_content_network,
		long						_version,
		String						_name,
		String						_site_host,
		String						_url_prefix,
		String						_url_icon,
		String						_url_relay_rpc,
		String						_url_authorised_rpc,
		String						_url_faq,
		String						_url_blog,
		String						_url_forums,
		String						_url_wiki )
	{
		super( _manager, TYPE_VUZE_GENERIC, _content_network, _version, _name );
		 
		SITE_HOST				= _site_host;
		URL_PREFIX 				= _url_prefix;
		URL_ICON				= _url_icon;
		URL_RELAY_RPC			= _url_relay_rpc;
		URL_AUTHORIZED_RPC		= _url_authorised_rpc;
		URL_FAQ					= _url_faq;
		URL_BLOG				= _url_blog;
		URL_FORUMS				= _url_forums;
		URL_WIKI				= _url_wiki;
		 
		init();
	}
	
	protected
	ContentNetworkVuzeGeneric(
		ContentNetworkManagerImpl	_manager,
		Map							_map )
	
		throws IOException
	{
		super( _manager );
		
		importFromBEncodedMap( _map );
	}
	
	protected void
	importFromBEncodedMap(
		Map			map )
	
		throws IOException
	{		
		super.importFromBencodedMap( map );
		
		SITE_HOST				= ImportExportUtils.importString(map, "vg_site" );
		URL_PREFIX 				= ImportExportUtils.importString(map, "vg_prefix" );
		URL_ICON 				= ImportExportUtils.importString(map, "vg_icon" );
		URL_RELAY_RPC			= ImportExportUtils.importString(map, "vg_relay_rpc" );
		URL_AUTHORIZED_RPC		= ImportExportUtils.importString(map, "vg_auth_rpc" );
		URL_FAQ					= ImportExportUtils.importString(map, "vg_faq" );
		URL_BLOG				= ImportExportUtils.importString(map, "vg_blog" );
		URL_FORUMS				= ImportExportUtils.importString(map, "vg_forums" );
		URL_WIKI				= ImportExportUtils.importString(map, "vg_wiki" );
		 
		init();
	}
	
	protected void
	exportToBencodedMap(
		Map			map )
	
		throws IOException
	{
		super.exportToBencodedMap( map );
		
		ImportExportUtils.exportString(map, "vg_site", 		SITE_HOST );
		ImportExportUtils.exportString(map, "vg_prefix", 	URL_PREFIX );
		ImportExportUtils.exportString(map, "vg_icon", 		URL_ICON );
		ImportExportUtils.exportString(map, "vg_relay_rpc", URL_RELAY_RPC );
		ImportExportUtils.exportString(map, "vg_auth_rpc", 	URL_AUTHORIZED_RPC );
		ImportExportUtils.exportString(map, "vg_faq", 		URL_FAQ );
		ImportExportUtils.exportString(map, "vg_blog", 		URL_BLOG );
		ImportExportUtils.exportString(map, "vg_forums",	URL_FORUMS );
		ImportExportUtils.exportString(map, "vg_wiki",		URL_WIKI );
	}
	
	protected void
	init()
	{
		service_map.clear();
		
		addService( SERVICE_SEARCH, 			URL_PREFIX + "search?q=" );
		addService( SERVICE_XSEARCH, 			URL_PREFIX + "xsearch?q=" );
		addService( SERVICE_RPC, 				URL_PREFIX + "rpc/" );
		addService( SERVICE_BIG_BROWSE, 		URL_PREFIX + "browse.start?" );
		addService( SERVICE_PUBLISH, 			URL_PREFIX + "publish.start?" );
		addService( SERVICE_WELCOME, 			URL_PREFIX + "welcome.start?" );
		addService( SERVICE_PUBLISH_NEW, 		URL_PREFIX + "publishnew.start?" );
		addService( SERVICE_PUBLISH_ABOUT, 		URL_PREFIX + "publishinfo.start" );
		addService( SERVICE_CONTENT_DETAILS, 	URL_PREFIX + "details/" );
		addService( SERVICE_COMMENT,			URL_PREFIX + "comment/" );
		addService( SERVICE_PROFILE,			URL_PREFIX + "profile/" );
		addService( SERVICE_TORRENT_DOWNLOAD,	URL_PREFIX + "download/" );
		addService( SERVICE_SITE,				URL_PREFIX );
		addService( SERVICE_SUPPORT,			URL_PREFIX + "support/" );
		addService( SERVICE_LOGIN,				URL_PREFIX + "login.start?" );
		addService( SERVICE_LOGOUT,				URL_PREFIX + "logout.start?" );
		addService( SERVICE_REGISTER,			URL_PREFIX + "register.start?" );
		addService( SERVICE_MY_PROFILE,			URL_PREFIX + "profile.start?" );
		addService( SERVICE_MY_ACCOUNT,			URL_PREFIX + "account.start?" );
		addService( SERVICE_SITE_RELATIVE,		URL_PREFIX );
		addService( SERVICE_ADD_FRIEND,			URL_PREFIX + "user/AddFriend.html?" );
		addService( SERVICE_SUBSCRIPTION,		URL_PREFIX + "xsearch?" );
		 		
		addService( SERVICE_AUTHORIZE,			URL_PREFIX + "ip.start?" );
		addService( SERVICE_GET_ICON,			URL_ICON );

		if ( URL_RELAY_RPC != null ){
			 
			addService( SERVICE_RELAY_RPC, 		URL_RELAY_RPC );
		}
		 
		if ( URL_AUTHORIZED_RPC != null ){
			 
			addService( SERVICE_AUTH_RPC, 		URL_AUTHORIZED_RPC );
		}
		 
		if ( URL_FAQ != null ){
		 
			addService( SERVICE_FAQ,			URL_FAQ );
			addService( SERVICE_FAQ_TOPIC,		URL_FAQ + "?View=entry&EntryID=" );
		}
		 
		if ( URL_BLOG != null ){
		 
			addService( SERVICE_BLOG,			URL_BLOG );
		}
		 
		if ( URL_FORUMS != null ){
		 
			addService( SERVICE_FORUMS,			URL_FORUMS );
		}
		 
		if ( URL_WIKI != null ){
			 
			addService( SERVICE_WIKI,			URL_WIKI );
		} 
	}
	
	protected void
	addService(
		int		type,
		String	url_str )
	{
		 service_map.put( type, url_str );
	}
	
	public Object 
	getProperty(
		int property ) 
	{
		if ( property == PROPERTY_SITE_HOST ){
			
			return( SITE_HOST );
			
		}else if ( property == PROPERTY_REMOVEABLE ){

			return( getID() != CONTENT_NETWORK_VUZE );
			
		}else if ( property == PROPERTY_ORDER ){
			
			return( String.valueOf( getID()));
			
		}else{
			
			debug( "Unknown property" );
			
			return( null );
		}
	}
	public boolean 
	isServiceSupported(
		int service_type )
	{
		return( service_map.get( service_type ) != null );
	}
	
	public String
	getServiceURL(
		int			service_type )
	{
		return( getServiceURL( service_type, new Object[0]));
	}
	
	
	public String	
	getServiceURL(
		int			service_type,
		Object[]	params )
	{
		String	base = service_map.get( service_type );
		
		if ( base == null ){
			
			debug( "Unknown service type '" + service_type + "'" );
			
			return( null );
		}
		
		switch( service_type ){
		
			case SERVICE_SEARCH:{
				
				String	query = (String)params[0];
				
				return(	base +
						UrlUtils.encode(query) + 
						"&" + URL_SUFFIX + 
						"&rand=" + SystemTime.getCurrentTime());
			}
			
			case SERVICE_XSEARCH:{
				
				String	query 			= (String)params[0];
				boolean	to_subscribe	= (Boolean)params[1];
				
				String url_str = 
							base +
							UrlUtils.encode(query) + 
							"&" + URL_SUFFIX + 
							"&rand=" + SystemTime.getCurrentTime();
				
				if ( to_subscribe ){
					
					url_str += "&createSubscription=1";
				}
				
				return( url_str );
			}
			case SERVICE_CONTENT_DETAILS:{
				
				String	hash 		= (String)params[0];
				String	client_ref 	= (String)params[1];
				
				String url_str = base + hash + ".html?" + URL_SUFFIX;
				
				if ( client_ref != null ){
					
					url_str += "&client_ref=" +  UrlUtils.encode( client_ref );
				}
				
				return( url_str );
			}
			case SERVICE_COMMENT:{
				
				String	hash 		= (String)params[0];
				
				return( base + hash + ".html?" + URL_SUFFIX	+ "&rnd=" + Math.random());
			}
			case SERVICE_PROFILE:{
			
				String	login_id 	= (String)params[0];
				String	client_ref 	= (String)params[1];
				
				return( base + UrlUtils.encode( login_id ) + "?" + URL_SUFFIX + "&client_ref=" +  UrlUtils.encode( client_ref ));
			}
			case SERVICE_TORRENT_DOWNLOAD:{
				
				String	hash 		= (String)params[0];
				String	client_ref 	= (String)params[1];

				String url_str = base + hash + ".torrent";
				
				if ( client_ref != null ){
					
					url_str += "?referal=" +  UrlUtils.encode( client_ref );
				}
				
				url_str = appendURLSuffix(url_str, false, true);
				
				return( url_str );
			}
			case SERVICE_FAQ_TOPIC:{
				
				String	topic 		= (String)params[0];
				
				return( base + topic );
			}
			case SERVICE_LOGIN:{
				
				String	message 		= (String)params[0];
				
				if ( message == null || message.length() == 0 ){
					
					base += URL_SUFFIX;
					
				}else{
					
					base += "msg=" + UrlUtils.encode( message );
					
					base += "&" + URL_SUFFIX;
				}
				
				return( base );
			}
			case SERVICE_MY_PROFILE:
			case SERVICE_MY_ACCOUNT:{
				
				base += URL_SUFFIX + "&rand=" + SystemTime.getCurrentTime();
				
				return( base );
			}
			case SERVICE_SITE_RELATIVE:{
				
				String	relative_url 	= (String)params[0];
				boolean	append_suffix	= (Boolean)params[1];
				
				base += relative_url.startsWith("/")?relative_url.substring(1):relative_url;
				
				if ( append_suffix ){

					base = appendURLSuffix( base, false, true );
				}
				
				return( base );
			}
			case SERVICE_ADD_FRIEND:{
				
				String	colour 	= (String)params[0];
				
				base += "ts=" + Math.random() + "&bg_color=" + colour;

				return( base );
			}
			case SERVICE_SUBSCRIPTION:{
				
				String	subs_id 	= (String)params[0];
				
				base += "subscription=" + subs_id + "&" + URL_SUFFIX;

				return( base );
			}
			case SERVICE_BIG_BROWSE:
			case SERVICE_PUBLISH:
			case SERVICE_WELCOME:
			case SERVICE_LOGOUT:
			case SERVICE_REGISTER:
			case SERVICE_AUTHORIZE:{
				
				 return( base + URL_SUFFIX );
			}
			default:{
				
				return( base );
			}
		}
	}
	
	public String 
	appendURLSuffix(
		String 		url_in, 
		boolean		for_post,
		boolean 	include_azid ) 
	{
		if ( url_in.indexOf( "azid=" ) != -1 ){
	
				// already present
			
			return( url_in );
		}
	
		String suffix = URL_SUFFIX;
		
		if ( !include_azid ){
			
			suffix = suffix.replaceAll( "azid=.*&", "" );
		}
		
		if ( for_post ){
			
			if ( url_in.length() == 0 ){
				
				return( suffix );
				
			}else{
				
				return( url_in + "&" + suffix  );
			}
		}else{
			
			if ( url_in.indexOf("?") >= 0 ){
	
				return( url_in + "&" + suffix );
				
			}else{
				
				return( url_in + "?" + suffix );
			}
		}
	}
}
