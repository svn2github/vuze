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

import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportException;
import com.aelitis.azureus.core.dht.transport.udp.impl.packethandler.DHTUDPPacketNetworkHandler;

public class 
DHTUDPPacketReplyError
	extends DHTUDPPacketReply
{
	public static final int	ET_UNKNOWN						= 0;
	public static final int	ET_ORIGINATOR_ADDRESS_WRONG		= 1;
	
	private int						error_type	= ET_UNKNOWN;
	
	private InetSocketAddress		originator_address;

	public
	DHTUDPPacketReplyError(
		DHTTransportUDPImpl		transport,
		int						trans_id,
		long					conn_id,
		DHTTransportContact		local_contact,
		DHTTransportContact		remote_contact )
	{
		super( transport, DHTUDPPacketHelper.ACT_REPLY_ERROR, trans_id, conn_id, local_contact, remote_contact );
	}
	
	protected
	DHTUDPPacketReplyError(
		DHTUDPPacketNetworkHandler		network_handler,		
		DataInputStream					is,
		int								trans_id )
	
		throws IOException
	{
		super( network_handler, is, DHTUDPPacketHelper.ACT_REPLY_ERROR, trans_id );
		
		error_type = is.readInt();
		
		if ( error_type == ET_ORIGINATOR_ADDRESS_WRONG ){
			
			originator_address	= DHTUDPUtils.deserialiseAddress( is );
		}
	}

	protected void
	setErrorType(
		int		error )
	{
		error_type	= error;
	}
	
	protected int
	getErrorType()
	{
		return( error_type );
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
	
		os.writeInt( error_type );
		
		if ( error_type == ET_ORIGINATOR_ADDRESS_WRONG ){

			try{
				DHTUDPUtils.serialiseAddress( os, originator_address );
				
			}catch( DHTTransportException e ){
				
				Debug.printStackTrace( e );
				
				throw( new IOException( e.getMessage()));
			}
		}
	}
}
