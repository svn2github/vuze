/*
 * File    : PRUDPPacketRequest.java
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

package org.gudy.azureus2.core3.tracker.protocol.udp;

/**
 * @author parg
 *
 */

import java.io.*;

public abstract class 
PRUDPPacketRequest
	extends PRUDPPacket
{	
	protected long		connection_id;
	
	public 
	PRUDPPacketRequest(
		int		_action,
		long	_con_id )
	{
		super( _action );
		
		connection_id	= _con_id;
	}
	
	protected 
	PRUDPPacketRequest(
		int		_action,
		long	_con_id,
		int		_trans_id )
	{
		super( _action, _trans_id );
		
		connection_id	= _con_id;
	}
	
	public void
	serialise(
		DataOutputStream	os )
	
		throws IOException
	{
		os.writeLong( connection_id );
		os.writeInt( type );
		os.writeInt( transaction_id );
	}
	
	public static PRUDPPacketRequest
	deserialiseRequest(
		DataInputStream		is )
	
		throws IOException
	{
		long		connection_id 	= is.readLong();
		int			action			= is.readInt();
		int			transaction_id	= is.readInt();
		
		switch( action ){
			case ACT_REQUEST_CONNECT:
			{
				return( new PRUDPPacketRequestConnect(is, connection_id,transaction_id));
			}
		}
		
		
		throw( new IOException( "unsupported request type"));
	}
	
	public String
	getString()
	{
		return( super.getString() + ":request[con=" + connection_id + ",trans=" + transaction_id + "]" );
	}
}
