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

import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.udp.DHTTransportUDP;
import com.aelitis.azureus.core.dht.transport.udp.impl.packethandler.DHTUDPPacketNetworkHandler;

public class 
DHTUDPPacketReplyFindNode
	extends DHTUDPPacketReply
{	
	private DHTTransportContact[]	contacts;
	private int						random_id;
	
	public
	DHTUDPPacketReplyFindNode(
		int						trans_id,
		long					conn_id,
		DHTTransportContact		local_contact,
		DHTTransportContact		remote_contact )
	{
		super( DHTUDPPacketHelper.ACT_REPLY_FIND_NODE, trans_id, conn_id, local_contact, remote_contact );
	}
	
	protected
	DHTUDPPacketReplyFindNode(
		DHTUDPPacketNetworkHandler		network_handler,
		DataInputStream					is,
		int								trans_id )
	
		throws IOException
	{
		super( is, DHTUDPPacketHelper.ACT_REPLY_FIND_NODE, trans_id );
		
			// we can only get the correct transport after decoding the network...
		
		DHTTransportUDPImpl	transport = (DHTTransportUDPImpl)network_handler.getRequestHandler( this );

		if ( getProtocolVersion() >= DHTTransportUDP.PROTOCOL_VERSION_ANTI_SPOOF ){
			
			random_id	= is.readInt();
		}		
		
		contacts = DHTUDPUtils.deserialiseContacts( transport, is );
	}
	
	public void
	serialise(
		DataOutputStream	os )
	
		throws IOException
	{
		super.serialise(os);
		
		if ( getProtocolVersion() >= DHTTransportUDP.PROTOCOL_VERSION_ANTI_SPOOF ){
			
			os.writeInt( random_id );
		}

		DHTUDPUtils.serialiseContacts( os, contacts );
	}
	
	protected void
	setContacts(
		DHTTransportContact[]	_contacts )
	{
		contacts	= _contacts;
	}

	protected void
	setRandomID(
		int	_random_id )
	{
		random_id	= _random_id;
	}
	
	protected int
	getRandomID()
	{
		return( random_id );
	}
	
	protected DHTTransportContact[]
	getContacts()
	{
		return( contacts );
	}
	
	public String
	getString()
	{
		return( super.getString() + ",contacts=" + (contacts==null?"null":(""+contacts.length ))); 
	}
}
