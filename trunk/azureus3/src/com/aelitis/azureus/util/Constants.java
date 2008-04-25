/*
 * Created on Aug 30, 2006
 * Created by Alon Rohter
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
package com.aelitis.azureus.util;

import java.util.Locale;

import org.gudy.azureus2.core3.util.Base32;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.crypto.VuzeCryptoManager;

/**
 * 
 */
public class Constants
{
	// isOS* constants copied from AZ2 for ease of use/access
	public static boolean isOSX = org.gudy.azureus2.core3.util.Constants.isOSX;

	public static boolean isWindows = org.gudy.azureus2.core3.util.Constants.isWindows;

	public static boolean isUnix = org.gudy.azureus2.core3.util.Constants.isUnix;

	public static final String DEFAULT_ADDRESS = "www.vuze.com"; //DO NOT TOUCH !!!!  use the -Dplatform_address=ip override instead

	public static final String DEFAULT_PORT = "80";

	public static final String DEFAULT_RELAY_ADDRESS = "relay.vuze.com"; //DO NOT TOUCH !!!!  use the -Drelay_address=ip override instead

	public static final String DEFAULT_RELAY_PORT = "80";

	public static String URL_ADDRESS = System.getProperty("platform_address",
			DEFAULT_ADDRESS);

	public static String URL_PORT = System.getProperty("platform_port",
			DEFAULT_PORT);

	public static final String URL_PREFIX = "http://" + URL_ADDRESS + ":"
			+ URL_PORT + "/";

	public static final String DEFAULT_AUTHORIZED_RPC = "https://" + URL_ADDRESS
			+ ":443/app";

	public static String URL_SUFFIX;

	public static final String URL_ADD_SEARCH = "search?q=";

	public static final String URL_PLATFORM_MESSAGE = "?service=rpc";

	public static final String URL_POST_PLATFORM_DATA = "service=rpc";

	public static final String URL_BIG_BROWSE = "browse.start";

	public static final String URL_MINI_BROWSE = "dashboard.start";

	public static final String URL_PUBLISH = "publish.start";

	public static final String URL_DETAILS = "details/";

	public static final String URL_COMMENTS = "comment/";

	public static final String URL_SHARE = "share/";

	public static final String URL_PROFILE = "profile/";

	public static final String URL_BUDDY_ACCEPT = "buddy-accept/";

	public static final String URL_DOWNLOAD = "download/";

	public static final String URL_FAQ = URL_PREFIX + "Support.html";

	public static final String URL_PUBLISH_INFO = URL_PREFIX + "Publish.html";

	public static final String URL_RPC = "rpc/";

	public static String URL_RELAY_RPC = "http://"
			+ System.getProperty("relay_address", DEFAULT_RELAY_ADDRESS) + ":"
			+ System.getProperty("relay_port", DEFAULT_RELAY_PORT) + "/app";

	public static final String URL_AUTHORIZED_RPC = System.getProperty(
			"authorized_rpc", DEFAULT_AUTHORIZED_RPC);

	public static final String URL_POP_UP = "?popup";

	public static final String URL_LOGIN = "login.start";

	public static final String URL_LOGOUT = "logout.start";

	public static final String URL_REGISTRATION = "register.start";

	public static final String URL_MY_PROFILE = "profile.start";

	public static final String URL_ACCOUNT = "account.start";

	/**
	 * This verifier value is only used to validate that the page we're loading is
	 * in-fact a page from Vuze; mainly required by the LightBoxBrowserWindow
	 */
	public static final String URL_PAGE_VERIFIER_VALUE = "vuzePage";

	public static final boolean DIAG_TO_STDOUT = System.getProperty(
			"DIAG_TO_STDOUT", "0").equals("1");

	public static String AZID;

	public static void initialize(AzureusCore core) {
		AZID = Base32.encode(VuzeCryptoManager.getSingleton().getPlatformAZID());

		URL_SUFFIX = "azid=" + AZID + "&azv="
				+ org.gudy.azureus2.core3.util.Constants.AZUREUS_VERSION + "&locale="
				+ Locale.getDefault().toString();
	}
}
