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

package org.gudy.azureus2.core3.tracker.client.impl;


import org.gudy.azureus2.core3.tracker.client.*;

public class 
TRTrackerAnnouncerResponsePeerImpl
	implements TRTrackerAnnouncerResponsePeer
{
	private String		source;
	private byte[]		peer_id;
	private String		address;
	private int			tcp_port;
	private int			udp_port;
	private short		crypto;
	
	public
	TRTrackerAnnouncerResponsePeerImpl(
		String		_source,
		byte[]		_peer_id,
		String		_address,
		int			_tcp_port,
		int			_udp_port,
		short		_crypto )
	{
		source		= _source;
		peer_id		= _peer_id;
		address		= _address;
		tcp_port	= _tcp_port;
		udp_port	= _udp_port;
		crypto		= _crypto;
	}
	
	public String
	getSource()
	{
		return( source );
	}
	
	public byte[]
	getPeerID()
	{
		return( peer_id );
	}
	
	public String
	getAddress()
	{
		return( address );
	}
	
	public int
	getPort()
	{
		return( tcp_port );
	}
	
	public int
	getUDPPort()
	{
		return( udp_port );
	}
	
	public short
	getProtocol()
	{
		return( crypto );
	}
}
