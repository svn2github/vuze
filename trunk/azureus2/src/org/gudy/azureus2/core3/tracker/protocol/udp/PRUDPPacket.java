/*
 * File    : PRUDPPacket.java
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

import java.net.*;
import java.io.*;
import java.util.*;

public abstract class 
PRUDPPacket 
{
	public static final int	MAX_PACKET_SIZE	= 8192;
	
	public static final int	ACT_REQUEST_CONNECT		= 0;
	public static final int	ACT_REQUEST_ANNOUNCE	= 1;
	public static final int	ACT_REQUEST_SCRAPE		= 2;
	
	public static final int	ACT_REPLY_CONNECT		= 0;
	public static final int	ACT_REPLY_ANNOUNCE		= 1;
	public static final int	ACT_REPLY_SCRAPE		= 2;
	public static final int	ACT_REPLY_ERROR			= 3;

	protected static int	next_id = new Random(System.currentTimeMillis()).nextInt();
	
	protected int		type;
	protected int		transaction_id;
	
	protected
	PRUDPPacket(
		int		_type,
		int		_transaction_id )
	{
		type			= _type;
		transaction_id	= _transaction_id;
	}
	
	protected
	PRUDPPacket(
		int		_type )
	{
		type			= _type;
		
		synchronized( PRUDPPacket.class ){
			
			transaction_id	= next_id++;
		}
	}
	
	public int
	getAction()
	{
		return( type );
	}
	
	public int
	getTransactionId()
	{
		return( transaction_id );
	}
	
	public abstract void
	serialise(
		DataOutputStream	os )
	
		throws IOException;
	
	public String
	getString()
	{
		return( "type=" + type);
	}
	
	public static int
	addressToInt(
		String		address )
	
		throws UnknownHostException
	{
		InetAddress i_address = InetAddress.getByName(address);
		
		byte[]	bytes = i_address.getAddress();
		
		int	resp = bytes[0]<<24 | bytes[1] << 16 | bytes[2] << 8 | bytes[3];
	
		// System.out.println( "addressToInt: " + address + " -> " + Integer.toHexString(resp));
		
		return( resp );
	}
	
	public static String
	intToAddress(
		int		value )
	
		throws UnknownHostException
	{
		byte[]	bytes = { (byte)(value>>24), (byte)(value>>16),(byte)(value>>8),(byte)value };
		
		return( InetAddress.getByAddress(bytes).getHostAddress());
	}
}
