/*
 * File    : TRTrackerServerPeerImpl.java
 * Created : 5 Oct. 2003
 * By      : Parg 
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

package org.gudy.azureus2.core3.tracker.server.impl;

import java.net.*;
import java.util.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.tracker.server.*;

public class 
TRTrackerServerPeerImpl
	implements TRTrackerServerPeer, HostNameToIPResolverListener
{	
	protected byte[]		peer_id;
	protected String		key;
	
	protected byte[]		ip_when_created;
	protected byte[]		ip;
	protected int			port;
	protected String		ip_str;
	protected byte[]		ip_bytes;
	
	protected long			timeout;
	
	protected long			uploaded;
	protected long			downloaded;
	protected long			amount_left;
	
	protected long			last_contact_time;
	protected boolean		download_completed;
	
	protected
	TRTrackerServerPeerImpl(
		HashWrapper	_peer_id,
		String		_key,
		byte[]		_ip,
		int			_port,
		long		_last_contact_time,
		boolean		_download_completed )
	{
		peer_id				= _peer_id.getHash();
		key					= _key;
		ip_when_created		= _ip;
		ip					= _ip;
		port				= _port;
		last_contact_time	= _last_contact_time;
		download_completed	= _download_completed;
		
		resolve();
	}
	
	protected void
	checkForIPChange(
		byte[]		_ip )
	{
		if ( !Arrays.equals( _ip, ip )){
			
			ip			= _ip;
	
			resolve();
		}
	}
	
	protected void
	resolve()
	{
		// default values pending resolution
		
		ip_str 		= new String( ip );
		ip_bytes	= null;
		
		HostNameToIPResolver.addResolverRequest( ip_str, this );
	}
	
	public void
	completed(
		InetAddress	address )
	{
		if ( address != null ){
			
			ip_str 		= address.getHostAddress();
			
			ip_bytes	= address.getAddress();
		}
	}
	
	protected long
	getLastContactTime()
	{
		return( last_contact_time );
	}
	
	protected boolean
	getDownloadCompleted()
	{
		return( download_completed );
	}
	
	protected void
	setDownloadCompleted()
	{
		download_completed	= true;
	}
	
	protected HashWrapper
	getPeerId()
	{
		return( new HashWrapper(peer_id));
	}
	
	protected String
	getKey()
	{
		return( key );
	}
	
		// the original IP is used as a key so we need to keep track of it
	
	protected byte[]
	getIPWhenCreated()
	{
		return( ip_when_created );
	}
	
	protected byte[]
	getIPAsRead()
	{
		return( ip );
	}
	
	public String
	getIPRaw()
	{
		return( new String(ip));
	}
	
		/**
		 * If asynchronous resolution of the address is required, this will return
		 * the non-resolved address until the async process completes 
		 */
	
	public String
	getIP()
	{		
		return( ip_str );
	}
	
		/**
		 * This will return in resolution of the address is not complete or fails
		 * @return
		 */
	
	public byte[]
	getIPBytes()
	{
		return( ip_bytes );
	}
	
	protected int
	getPort()
	{
		return( port );
	}
	
	protected void
	setTimeout(
		long		_now,
		long		_timeout )
	{
		last_contact_time	= _now;
		
		timeout				= _timeout;
	}
	
	protected long
	getTimeout()
	{
		return( timeout );
	}

	protected void
	setStats(
		long		_uploaded,
		long		_downloaded,
		long		_amount_left )
	{
		uploaded	= _uploaded;
		downloaded	= _downloaded;
		amount_left	= _amount_left;
	}
	
	public long
	getUploaded()
	{
		return( uploaded );
	}
	
	public long
	getDownloaded()
	{
		return( downloaded );
	}
	
	public long
	getAmountLeft()
	{
		return( amount_left );
	}
	
	protected boolean
	isSeed()
	{
		return( amount_left == 0 );
	}
	
	protected String
	getString()
	{
		return( new String(ip) + ":" + port + "(" + new String(peer_id) + ")" );
	}
}
