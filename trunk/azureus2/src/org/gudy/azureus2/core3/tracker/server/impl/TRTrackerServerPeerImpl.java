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

import java.net.*;

import org.gudy.azureus2.core3.tracker.server.*;

public class 
TRTrackerServerPeerImpl
	implements TRTrackerServerPeer
{
	protected byte[]		peer_id;
	protected byte[]		ip;
	protected int			port;
	protected String		ip_str;
	protected byte[]		ip_bytes;
	
	protected long			timeout;
	
	protected long			uploaded;
	protected long			downloaded;
	protected long			amount_left;
	protected int			numwant;
	
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
	getIPAsRead()
	{
		return( ip );
	}
	
	public String
	getIPRaw()
	{
		return( new String(ip));
	}
	
	public String
	getIP()
	{
		if ( ip_str == null ){
			
			ip_str = new String(ip);
		
			try{
				InetAddress addr = InetAddress.getByName( ip_str );
				
				ip_str 		= addr.getHostAddress();
				ip_bytes	= addr.getAddress();
				
			}catch( UnknownHostException e ){
			
				ip_str 		= new String( ip );
				ip_bytes	= null;
			}
		}
		
		return( ip_str );
	}
	
	public byte[]
	getIPBytes()
	{
		if ( ip_str == null ){
			
			getIP();
		}
		
		return( ip_bytes );
	}
	
	protected int
	getPort()
	{
		return( port );
	}
	
	protected void
	setTimeout(
		long		_t )
	{
		timeout	= _t;
	}
	
	protected long
	getTimeout()
	{
		return( timeout );
	}

	protected void
	setStats(
		long		_uploaded,
		long		_downloaded,
		long		_amount_left,
		int			_numwant )
	{
		uploaded	= _uploaded;
		downloaded	= _downloaded;
		amount_left	= _amount_left;
		numwant	= _numwant;
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
	
	protected boolean
	isSeed()
	{
		return( amount_left == 0 );
	}
	
	public int
	getNumberOfPeers()
	{
		return( numwant );
	}
	
	protected String
	getString()
	{
		return( new String(ip) + ":" + port + "(" + new String(peer_id) + ")" );
	}
}
