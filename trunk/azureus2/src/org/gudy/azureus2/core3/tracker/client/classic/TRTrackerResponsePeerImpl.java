/*
 * File    : TRTrackerResponsePeerImpl.java
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

package org.gudy.azureus2.core3.tracker.client.classic;


import org.gudy.azureus2.core3.tracker.client.*;

public class 
TRTrackerResponsePeerImpl
	implements TRTrackerResponsePeer 
{
	protected byte[]		peer_id;
	protected String		ip_address;
	protected int			port;
	
	protected
	TRTrackerResponsePeerImpl(
		byte[]		_peer_id,
		String		_ip_address,
		int			_port )
	{
		peer_id		= _peer_id;
		ip_address	= _ip_address;
		port		= _port;
	}
	
	public byte[]
	getPeerId()
	{
		return( peer_id );
	}
	
	public String
	getIPAddress()
	{
		return( ip_address );
	}
	
	public int
	getPort()
	{
		return( port );
	}
}
