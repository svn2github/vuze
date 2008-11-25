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


package com.aelitis.azureus.core.cnetwork;

import com.aelitis.azureus.core.vuzefile.VuzeFile;

public interface 
ContentNetwork 
{
	public static final long	CONTENT_NETWORK_VUZE		= 1;
	public static final long	CONTENT_NETWORK_RFN			= 2;

	public static final int		SERVICE_SEARCH				= 1;	// String - query text
	public static final int		SERVICE_XSEARCH				= 2;	// String - query text; Boolean - toSubscribe
	public static final int		SERVICE_RPC					= 3;
	public static final int		SERVICE_RELAY_RPC			= 4;
	public static final int		SERVICE_AUTH_RPC			= 5;
	public static final int		SERVICE_BIG_BROWSE			= 6;
	public static final int		SERVICE_PUBLISH				= 7;
	public static final int		SERVICE_WELCOME				= 8;
	public static final int		SERVICE_PUBLISH_NEW			= 9;
	public static final int		SERVICE_PUBLISH_ABOUT		= 10;
	public static final int		SERVICE_CONTENT_DETAILS		= 11;	// String - hash; String (can be null) - client ref
	public static final int		SERVICE_COMMENT				= 12;	// String - hash
	public static final int		SERVICE_PROFILE				= 13;	// String - login_id; String - client ref
	public static final int		SERVICE_TORRENT_DOWNLOAD	= 14;	// String - hash; String (can be null) - client ref
	public static final int		SERVICE_SITE				= 15;
	public static final int		SERVICE_SUPPORT				= 16;
	public static final int		SERVICE_FAQ					= 17;
	public static final int		SERVICE_FAQ_TOPIC			= 18;	// String - topic entry
	public static final int		SERVICE_BLOG				= 19;
	public static final int		SERVICE_FORUMS				= 20;
	public static final int		SERVICE_WIKI				= 21;
	public static final int		SERVICE_LOGIN				= 22;	// String - message (can be null)
	public static final int		SERVICE_LOGOUT				= 23;
	public static final int		SERVICE_REGISTER			= 24;
	public static final int		SERVICE_MY_PROFILE			= 25;
	public static final int		SERVICE_MY_ACCOUNT			= 26;
	public static final int		SERVICE_SITE_RELATIVE		= 27;	// String - relative URL
	public static final int		SERVICE_ADD_FRIEND			= 28;	// String - bg colour
	public static final int		SERVICE_SUBSCRIPTION		= 29;	// String - subscription ID
	
	public static final int		PROPERTY_SITE_HOST			= 1;

	
		/**
		 * Returns one of the above CONTENT_NETWORK constants
		 * @return
		 */
	
	public long
	getID();
	
	public String
	getProperty(
		int			property );
	
		/**
		 * Test if the network supports a particular service
		 * @param service_type
		 * @return
		 */
	
	public boolean
	isServiceSupported(
		int			service_type );
	
		/**
		 * Returns the base URL of the service. If not parameterised then this is sufficient to
		 * invoke the service
		 * @param service_type
		 * @return
		 */
	
	public String
	getServiceURL(
		int			service_type );
	
		/**
		 * Generic parameterised service method
		 * @param service_type
		 * @param params
		 * @return
		 */
	
	public String
	getServiceURL(
		int			service_type,
		Object[]	params );
	
		/**
		 * search service helper method
		 * @param query
		 * @return
		 */
	
	public String
	getSearchService(
		String		query );
	
	
	public String
	getXSearchService(
		String		query,
		boolean		to_subscribe );
	
	public String
	getContentDetailsService(
		String		hash,
		String		client_ref );
	
	public String
	getCommentService(
		String		hash );
	
	public String
	getProfileService(
		String		login_id,
		String		client_ref );
	
	public String
	getTorrentDownloadService(
		String		hash,
		String		client_ref );
	
		/**
		 * @param topic The topic number or a pre-defined topic constant found in <code>FAQTopics</code>
		 */

	public String
	getFAQTopicService(
		String		topic );
	
	public String
	getLoginService(
		String		message );
	
	public String
	getSiteRelativeURL(
		String		relative_url,
		boolean		append_suffix );
	
	public String
	getAddFriendURL(
		String		bg_colour );
	
	public String
	getSubscriptionURL(
		String		subs_id );
	
	public String
	appendURLSuffix(
		String		url_in,
		boolean		include_azid );
	
		/**
		 * export to vuze file
		 * @return
		 */
	
	public VuzeFile
	getVuzeFile();
}
