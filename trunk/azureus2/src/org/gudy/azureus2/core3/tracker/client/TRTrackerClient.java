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

import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.peer.*;

public interface 
TRTrackerClient 
{
	public static final int TS_INITIALISED		= 1;
	public static final int TS_DOWNLOADING		= 2;
	public static final int TS_COMPLETED		= 3;
	public static final int TS_STOPPED			= 4;

	
	public void
	setManager(
		PEPeerManager		manager );
	
	public TOTorrent
	getTorrent();
	
	public String
	getTrackerUrl();
	
	public void
	addTrackerUrl(
		String		url );
		
	public void
	setTrackerUrl(
		String		url );
		
	public void
	resetTrackerUrl();
	
	public void
	setIPOverride(
		String		override );
	
	public void
	clearIPOverride();
		
	public byte[]
	getPeerId();
		
	public void
	update();
	
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
