/*
 * File    : TRTrackerServerPeerImpl.java
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

package org.gudy.azureus2.core3.tracker.server.impl;

import org.gudy.azureus2.core3.tracker.server.*;

public class 
TRTrackerServerPeerImpl
	implements TRTrackerServerPeer
{
	protected byte[]		peer_id;
	protected byte[]		ip;
	protected int			port;
	
	protected long			last_contact_time;
	
	protected long			uploaded;
	protected long			downloaded;
	protected long			amount_left;
	protected int			num_peers;
	
	protected
	TRTrackerServerPeerImpl(
		byte[]		_peer_id,
		byte[]		_ip,
		int			_port )
	{
		peer_id		= _peer_id;
		ip			= _ip;
		port		= _port;
	}
	
	protected byte[]
	getPeerId()
	{
		return( peer_id );
	}
	
	protected byte[]
	getIP()
	{
		return( ip );
	}
	
	protected int
	getPort()
	{
		return( port );
	}
	
	protected void
	setLastContactTime(
		long		_t )
	{
		last_contact_time	= _t;
	}
	
	protected long
	getLastContactTime()
	{
		return( last_contact_time );
	}

	protected void
	setStats(
		long		_uploaded,
		long		_downloaded,
		long		_amount_left,
		int			_num_peers )
	{
		uploaded	= _uploaded;
		downloaded	= _downloaded;
		amount_left	= _amount_left;
		num_peers	= _num_peers;
	}
	
	public long
	getUploaded()
	{
		return( uploaded );
	}
	
	public long
	getDownloaded()
	{
		return( downloaded );
	}
	
	public long
	getAmountLeft()
	{
		return( amount_left );
	}
	
	public int
	getNumberOfPeers()
	{
		return( num_peers );
	}
	
	protected String
	getString()
	{
		return( new String(ip) + ":" + port + "(" + new String(peer_id) + ")" );
	}
}
