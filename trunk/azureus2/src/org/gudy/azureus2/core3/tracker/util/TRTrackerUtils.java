/*
 * File    : TRTrackerClientUtils.java
 * Created : 31-Mar-2004
 * By      : parg
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.core3.tracker.util;

/**
 * @author parg
 *
 */

import java.util.Map;
import java.net.URL;
import java.io.IOException;

import org.gudy.azureus2.core3.tracker.util.impl.*;

public class 
TRTrackerUtils 
{
	public static void
	checkForBlacklistedURLs(
		URL		url )
	
		throws IOException
	{
		TRTrackerUtilsImpl.checkForBlacklistedURLs( url );
	}

	public static String
	getTrackerIP()
	{
		return( TRTrackerUtilsImpl.getTrackerIP());
	}
	
	public static URL
	adjustURLForHosting(
		URL		url_in )
	{
		return( TRTrackerUtilsImpl.adjustURLForHosting(url_in ));
	}

	public static String
	adjustHostFromHosting(
		String	host_in )
	{
		return( TRTrackerUtilsImpl.adjustHostFromHosting( host_in ));
		
	}
	
	public static Map
	mergeResponseCache(
		Map		map1,
		Map		map2 )
	{
		return( TRTrackerUtilsImpl.mergeResponseCache( map1, map2 ));
	}
}
