/*
 * File    : PRUDPPacketRequestAnnounce.java
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

import org.gudy.azureus2.core3.util.*;

public class 
PRUDPPacketRequestAnnounce 
	extends PRUDPPacketRequest
{
	/*
	char m_info_hash[20];
	char m_peer_id[20];
	__int64 m_downloaded;
	int m_event;
	int m_num_want;
	__int64 m_left;
	short m_port;
	__int64 m_uploaded;
	*/
	
	public static final int	EV_STARTED		= 1;
	public static final int	EV_STOPPED		= 2;
	public static final int	EV_COMPLETED	= 3;
	public static final int	EV_UPDATE		= 4;
	
	protected byte[]		hash;
	protected byte[]		peer_id;
	protected long			downloaded;
	protected int			event;
	protected int			num_want;
	protected long			left;
	protected short			port;
	protected long			uploaded;
	
	public
	PRUDPPacketRequestAnnounce(
		long				con_id )
	{
		super( ACT_REQUEST_ANNOUNCE, con_id );
	}
	
	protected
	PRUDPPacketRequestAnnounce(
		DataInputStream		is,
		long				con_id,
		int					trans_id )
	
		throws IOException
	{
		super( ACT_REQUEST_ANNOUNCE, con_id, trans_id );
		
		hash 	= new byte[20];
		peer_id	= new byte[20];
		
		is.read( hash );
		is.read( peer_id );
		
		downloaded 	= is.readLong();
		event 		= is.readInt();
		num_want	= is.readInt();
		left		= is.readLong();
		port		= is.readShort();
		uploaded	= is.readLong();
	}
	
	public void
	setDetails(
		byte[]		_hash,
		byte[]		_peer_id,
		long		_downloaded,
		int			_event,
		int			_num_want,
		long		_left,
		short		_port,
		long		_uploaded )
	{
		hash		= _hash;
		peer_id		= _peer_id;
		downloaded	= _downloaded;
		event		= _event;
		num_want	= _num_want;
		left		= _left;
		port		= _port;
		uploaded	= _uploaded;
	}
	
	public void
	serialise(
		DataOutputStream	os )
	
		throws IOException
	{
		super.serialise(os);
		
		os.write( hash );
		os.write( peer_id );
		os.writeLong( downloaded );
		os.writeInt( event );
		os.writeInt( num_want );
		os.writeLong( left );
		os.writeShort( port );
		os.writeLong( uploaded );
	}
	
	public String
	getString()
	{
		return( super.getString() + "[" +
					"hash=" + ByteFormatter.nicePrint( hash, true ) +
					"peer=" + ByteFormatter.nicePrint( peer_id, true ) +
					"dl=" + downloaded +
					"ev=" + event +
					"nw=" + num_want +
					"left="+left+
					"port=" + port +
					"ul=" + uploaded + "]" );
	}
}
