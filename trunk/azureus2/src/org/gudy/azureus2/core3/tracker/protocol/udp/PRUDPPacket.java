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

import java.io.*;

public abstract class 
PRUDPPacket 
{
	public static final int	ACT_REQUEST_CONNECT		= 0;
	public static final int	ACT_REQUEST_ANNOUNCE	= 1;
	public static final int	ACT_REQUEST_SCRAPE		= 2;
	
	public static final int	ACT_REPLY_CONNECT		= 10;
	public static final int	ACT_REPLY_ANNOUNCE		= 11;
	public static final int	ACT_REPLY_SCRAPE		= 12;
	
	protected int		type;
	
	protected
	PRUDPPacket(
		int		_type )
	{
		type	= _type;
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
}
