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
import com.aelitis.azureus.core.dht.transport.DHTTransportException;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

public class 
DHTUDPPacketReplyFindValue
	extends DHTUDPPacketReply
{
	private DHTTransportContact[]		contacts;
	private DHTTransportValue[]			values;
	
	public
	DHTUDPPacketReplyFindValue(
		int					trans_id,
		long				conn_id,
		DHTTransportContact	contact)
	{
		super( DHTUDPPacket.ACT_REPLY_FIND_VALUE, trans_id, conn_id, contact );
	}
	
	protected
	DHTUDPPacketReplyFindValue(
		DHTTransportUDPImpl		transport,
		DataInputStream			is,
		int						trans_id )
	
		throws IOException
	{
		super( is, DHTUDPPacket.ACT_REPLY_FIND_VALUE, trans_id );
		
		boolean	is_value = is.readBoolean();
		
		if ( is_value ){
			
			values = DHTUDPUtils.deserialiseTransportValues( transport, is, 0 );
			
		}else{
			
			contacts = DHTUDPUtils.deserialiseContacts( transport, is );
		}
	}
	
	public void
	serialise(
		DataOutputStream	os )
	
		throws IOException
	{
		super.serialise(os);
		
		os.writeBoolean( values != null );
		
		if ( values == null ){
			
			DHTUDPUtils.serialiseContacts( os, contacts );
			
		}else{
			
			// values returned to a caller are adjusted by - skew
			
			try{
				DHTUDPUtils.serialiseTransportValues( os, values, -getClockSkew());
				
			}catch( DHTTransportException e ){
				
				throw( new IOException( e.getMessage()));
			}
		}
	}
	
	protected void
	setValue(
		DHTTransportValue	_value )
	{
		values	= new DHTTransportValue[]{ _value };
	}
	
	protected DHTTransportValue
	getValue()
	{
		if ( values == null || values.length == 0 ){
			
			return( null );
		}
		return( values[0] );
	}
	
	protected void
	setContacts(
		DHTTransportContact[]	_contacts )
	{
		contacts	= _contacts;
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
