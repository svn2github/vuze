/*
 * File    : PRHelpers.java
 * Created : 10-Mar-2004
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

package org.gudy.azureus2.core3.tracker.protocol;

/**
 * @author parg
 *
 */

import java.net.*;

public class 
PRHelpers 
{
	public static int
	addressToInt(
		String		address )
	
		throws UnknownHostException
	{
		InetAddress i_address = InetAddress.getByName(address);
		
		byte[]	bytes = i_address.getAddress();
		
		int	resp = (bytes[0]<<24)&0xff000000 | (bytes[1] << 16)&0x00ff0000 | (bytes[2] << 8)&0x0000ff00 | bytes[3]&0x000000ff;
	
		// System.out.println( "addressToInt: " + address + " -> " + Integer.toHexString(resp));
		
		return( resp );
	}
	
	public static String
	intToAddress(
		int		value )
	
		throws UnknownHostException
	{
		byte[]	bytes = { (byte)(value>>24), (byte)(value>>16),(byte)(value>>8),(byte)value };
		
		String	res = InetAddress.getByAddress(bytes).getHostAddress();
		
		// System.out.println( "intToAddress: " + Integer.toHexString(value) + " -> " + res );
		
		return( res );
	}
	
	public static void
	addressTo4ByteArray(
		String		address,
		byte[]		buffer,
		int			offset )
	
		throws UnknownHostException
	{
		InetAddress i_address = InetAddress.getByName(address);
		
		byte[]	bytes = i_address.getAddress();
	
		System.arraycopy( bytes, 0, buffer, offset, 4 );
	}
	
	public static String
	DNSToIPAddress(
		String		dns_name )
	
		throws UnknownHostException
	{
		return( InetAddress.getByName(dns_name).getHostAddress());
	}
}
