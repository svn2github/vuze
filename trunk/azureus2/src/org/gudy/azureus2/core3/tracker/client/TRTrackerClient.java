/*
 * File    : TRTrackerClient.java
 * Created : 5 Oct. 2003
 * By      : Parg 
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

package org.gudy.azureus2.core3.tracker.client;

import java.util.Map;

import org.gudy.azureus2.core3.torrent.*;

public interface 
TRTrackerClient 
{
	public static final int REFRESH_MINIMUM_SECS		= 60;

	public static final int TS_INITIALISED		= 1;
	public static final int TS_DOWNLOADING		= 2;
	public static final int TS_COMPLETED		= 3;
	public static final int TS_STOPPED			= 4;

	
	public void
	setAnnounceDataProvider(
		TrackerClientAnnounceDataProvider		provider );
	
	public TOTorrent
	getTorrent();
	
	public String
	getTrackerUrl();
	
	public void
	setTrackerUrl(
		String		url );
		
	public void
	resetTrackerUrl(
		boolean	shuffle );
	
	public void
	setIPOverride(
		String		override );
	
	public void
	clearIPOverride();
		
	public byte[]
	getPeerId();
	
	public void
	setRefreshDelayOverrides(
		boolean	use_minimum,
		int		percentage );
	
	public int
	getTimeUntilNextUpdate();
	
	public int
	getLastUpdateTime();
			
	public void
	update(
		boolean	force );
	
	public void
	complete(
		boolean	already_reported );
	
	public void
	stop();
	
	public void
	destroy();
	
	public int
	getStatus();
	
	public String
	getStatusString();
	
	public TRTrackerResponse
	getLastResponse();
	
		/**
		 * returns a Map containing "bencoded" entries representing a cache of tracker
		 * responses.
		 * @return
		 */
	
	public Map
	getTrackerResponseCache();
	
		/**
		 * sets the response cache. This may be used by the tracker client to return peer
		 * details when the tracker is offline 
		 * @param map
		 */
	
	public void
	setTrackerResponseCache(
		Map		map );
	
	/**
	 * This method forces all listeners to get an explicit "urlChanged" event to get them
	 * to re-examine the tracker
	 */
	
	public void
	refreshListeners();
	
	public void
	addListener(
		TRTrackerClientListener	l );
		
	public void
	removeListener(
		TRTrackerClientListener	l );
}
