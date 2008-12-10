/**
 * Created on Dec 9, 2008
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package com.aelitis.azureus.core.utils;

import java.util.*;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.cnetwork.*;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.util.ConstantsV3;

/**
 * @author TuxPaper
 * @created Dec 9, 2008
 *
 */
public class UrlFilter
{
	private static UrlFilter instance = null;

	//private String RPC_WHITELIST = "AZMSG%3B[0-9]+%3B.*";

	private String default_site_host = (String) ConstantsV3.DEFAULT_CONTENT_NETWORK.getProperty(ContentNetwork.PROPERTY_SITE_HOST);

	private String DEFAULT_RPC_WHITELIST = "https?://"
			+ default_site_host.replaceAll("\\.", "\\\\.") + ":?[0-9]*/" + ".*";

	private List listUrlBlacklist = Collections.EMPTY_LIST;

	private List<String> listUrlWhitelist = new ArrayList<String>();

	private AEMonitor mon = new AEMonitor("UrlFilter");

	static {
	}

	public static UrlFilter getInstance() {
		synchronized (UrlFilter.class) {
			if (instance == null) {
				instance = new UrlFilter();
			}
			return instance;
		}
	}

	public UrlFilter() {
		listUrlWhitelist.add(DEFAULT_RPC_WHITELIST);

		ContentNetworkManager cmn = ContentNetworkManagerFactory.getSingleton();
		ContentNetwork[] contentNetworks = cmn.getContentNetworks();
		cmn.addListener(new ContentNetworkListener() {
			public void networkRemoved(ContentNetwork network) {
			}
		
			public void networkChanged(ContentNetwork network) {
			}
		
			public void networkAdded(ContentNetwork network) {
				addNetworkFilters(network);
			}
		});
		
		for (ContentNetwork cn : contentNetworks) {
			addNetworkFilters(cn);
		}
	}

	/**
	 * Ensure whitelist has important network urls
	 * 
	 * @param network
	 *
	 * @since 4.0.0.5
	 */
	protected void addNetworkFilters(ContentNetwork network) {
		if (network == null) {
			return;
		}
		
		int[] whitelist_services = {
				ContentNetwork.SERVICE_SITE,
				ContentNetwork.SERVICE_AUTH_RPC,
				ContentNetwork.SERVICE_RELAY_RPC,
				ContentNetwork.SERVICE_RPC,	
		};
		
		for ( int service: whitelist_services ){

			if ( network.isServiceSupported( service )){
	
				addUrlWhitelist( network.getServiceURL( service ) + ".*" );
			}
		}
	}

	public void addUrlWhitelist(String string) {
		mon.enter();
		try {
			if (!listUrlWhitelist.contains(string)) {
				PlatformMessenger.debug("add whitelist of " + string);
				listUrlWhitelist.add(string);
			} else {
				PlatformMessenger.debug("whitelist already exists: " + string);
			}
		} finally {
			mon.exit();
		}
	}

	public void addUrlBlacklist(String string) {
		mon.enter();
		try {
			if (!listUrlBlacklist.contains(string)) {
				PlatformMessenger.debug("add blacklist of " + string);
				listUrlBlacklist.add(string);
			}
		} finally {
			mon.exit();
		}
	}

	public String[] getUrlWhitelist() {
		return listUrlWhitelist.toArray(new String[0]);
	}

	public boolean urlCanRPC(String url) {
		return urlCanRPC(url, false);
	}

	public boolean urlCanRPC(String url,boolean showDebug) {
		if (url == null) {
			Debug.out("URL null and should be blocked");
			return false;
		}
		
		if (Constants.isCVSVersion() && url.startsWith("file://")) {
			return true;
		}

		String[] whitelist = getUrlWhitelist();
		for (int i = 0; i < whitelist.length; i++) {
			if (url.matches(whitelist[i])) {
				return true;
			}
		}
		if(showDebug) {
			Debug.out("urlCanRPC: URL '" + url + "' " + " does not match one of the "
					+ whitelist.length + " whitelist entries");
		}
		return false;
	}
	
	public boolean urlIsBlocked(String url) {
		if (url == null) {
			Debug.out("URL null and should be blocked");
			return true;
		}

		for (Iterator iter = listUrlBlacklist.iterator(); iter.hasNext();) {
			String blackListed = (String) iter.next();
			if (url.matches(blackListed)) {
				Debug.out("URL '" + url + "' " + " is blocked by " + blackListed);
				return true;
			}
		}
		return false;
	}

}
