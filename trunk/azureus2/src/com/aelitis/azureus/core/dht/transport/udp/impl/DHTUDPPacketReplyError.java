/*
 * File    : PRUDPPacketReplyConnect.java
 * Created : 20-Jan-2004
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

package com.aelitis.azureus.core.dht.transport.udp.impl;

/**
 * @author parg
 *
 */

import java.io.*;
import java.net.InetSocketAddress;

public class 
DHTUDPPacketReplyError
	extends DHTUDPPacketReply
{
	private InetSocketAddress		originator_address;
	
	public
	DHTUDPPacketReplyError(
		int			trans_id,
		long		conn_id )
	{
		super( DHTUDPPacket.ACT_REPLY_ERROR, trans_id, conn_id );
	}
	
	protected
	DHTUDPPacketReplyError(
		DataInputStream		is,
		int					trans_id )
	
		throws IOException
	{
		super( is, DHTUDPPacket.ACT_REPLY_ERROR, trans_id );
		
		originator_address	= DHTUDPUtils.deserialiseAddress( is );
	}
	
	protected void
	setOriginatingAddress(
			InetSocketAddress	a )
	{
		originator_address = a;
	}
	
	protected InetSocketAddress
	getOriginatingAddress()
	{
		return( originator_address );
	}
	
	public void
	serialise(
		DataOutputStream	os )
	
		throws IOException
	{
		super.serialise(os);
		
		DHTUDPUtils.serialiseAddress( os, originator_address );
	}
}
