/*
 * File    : TRTrackerServer.java
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

package org.gudy.azureus2.core3.tracker.server;


public interface 
TRTrackerServer 
{
	public static final int DEFAULT_MIN_RETRY_DELAY 	= 120;
	public static final int DEFAULT_MAX_RETRY_DELAY 	= 0;
	public static final int DEFAULT_INC_BY				= 60;
	public static final int DEFAULT_INC_PER			 	= 10;
	
	public static final int DEFAULT_TRACKER_PORT 	= 6969;

	public int
	getRetryInterval();
	
	public void
	permit(
		byte[]		hash,
		boolean		explicit  )
		
		throws TRTrackerServerException;
		
	public void
	deny(
		byte[]		hash,
		boolean		explicit )
		
		throws TRTrackerServerException;
		
	public TRTrackerServerTorrentStats
	getStats(
		byte[]		hash );
		
	public TRTrackerServerPeer[]
	getPeers(
		byte[]		hash );

	public void
	addListener(
		TRTrackerServerListener	l );
		
	public void
	removeListener(
		TRTrackerServerListener	l );
}
