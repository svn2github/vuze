/*
 * File    : TRTrackerResponse.java
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

public interface 
TRTrackerResponse 
{
	public static final int	ST_OFFLINE			= 0;
	public static final int ST_REPORTED_ERROR	= 1;
	public static final int	ST_ONLINE			= 2;
	
	/**
	 * Returns the current status of the tracker
	 * @return	see above ST_ set
	 */
	
	public int
	getStatus();
	
	/**
	 * This value is always available
	 * @return time to wait before requerying tracker
	 */
	
	public long
	getTimeToWait();
	
	/**
	 * if the status is ST_REPORTED_ERROR then this method is of use
	 * Also populated when ST_OFFLINE - in this case it gives a reason where possible
	 * @return	failure reason as reported by tracker.
	 */
	
	public String
	getFailureReason();
	
	/**
	 * 
	 * @return	peers reported by tracker. this will include the local peer as well
	 */
	
	public TRTrackerResponsePeer[]
	getPeers();
	
	public void
	print();
}
