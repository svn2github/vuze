/*
 * File    : PRUDPPacketReply.java
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
PRUDPPacketReply
	extends PRUDPPacket
{	
	protected int		return_value;
	protected int		transaction_id;
	
	public
	PRUDPPacketReply(
		int		_type,
		int		_return_value,
		int		_tran_id )
	{
		super( _type );
		
		return_value	= _return_value;
		transaction_id	= _tran_id;
	}
	
	public int
	getType()
	{
		return( type );
	}
	
	public void
	serialise(
		DataOutputStream	os )
	
	throws IOException
	{
		os.writeInt( return_value );
		os.writeInt( transaction_id );
	}
	
	public static PRUDPPacketReply
	deserialiseReply(
		DataInputStream		is,
		int					type )
	
		throws IOException
	{
		int		return_value	= is.readInt();
		int		transaction_id	= is.readInt();
		
		switch( type ){
			case ACT_REPLY_CONNECT:
			{
				return( new PRUDPPacketReplyConnect(is, return_value,transaction_id));
			}
		}
		
		
		throw( new IOException( "unsupported reply type"));
	}
	
	public String
	getString()
	{
		return( super.getString() + ":reply" );
	}
}