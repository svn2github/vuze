/*
 * File    : PRUDPPacketRequestScrape.java
 * Created : 21-Jan-2004
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

import org.gudy.azureus2.core3.util.*;

public class 
PRUDPPacketRequestScrape 
	extends PRUDPPacketRequest
{
	protected byte[]		hash;

	public
	PRUDPPacketRequestScrape(
		long			con_id,
		byte[]			_hash)
	{
		super( ACT_REQUEST_SCRAPE, con_id );
		
		hash	= _hash;
	}
	
	protected
	PRUDPPacketRequestScrape(
		DataInputStream		is,
		long				con_id,
		int					trans_id )
	
		throws IOException
	{
		super( ACT_REQUEST_SCRAPE, con_id, trans_id );
		
		hash 	= new byte[20];

		is.read( hash );
	}
	
	public byte[]
	getHash()
	{
		return( hash );
	}
	
	public void
	serialise(
		DataOutputStream	os )
	
	throws IOException
	{
		super.serialise(os);
		
		os.write( hash );
	}
	
	public String
	getString()
	{
		return( super.getString().concat("[").concat(
				"hash=").concat(ByteFormatter.nicePrint( hash, true )).concat(
				"]") );
	}
}

