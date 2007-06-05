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

import com.aelitis.azureus.core.dht.netcoords.DHTNetworkPosition;

public class 
TRTrackerServerPeerImpl
	implements TRTrackerServerPeer, HostNameToIPResolverListener, TRTrackerServerNatCheckerListener
{	
	private HashWrapper	peer_id;
	private int			key_hash_code;
	
	private byte[]		ip;
	private boolean		ip_override;
	private short		tcp_port;
	private short		udp_port;
	private short		http_port;
	private byte		crypto_level;
	private byte		az_ver;
	private String		ip_str;
	private byte[]		ip_bytes;
	private byte		NAT_status	= NAT_CHECK_UNKNOWN;
	
	private long		timeout;
	
	private long		uploaded;
	private long		downloaded;
	private long		amount_left;
	
	private long		last_contact_time;
	private boolean		download_completed;
	private boolean		biased;
	
	private short				up_speed;
	private DHTNetworkPosition	network_position;
	private Object				user_data;
	
	protected
	TRTrackerServerPeerImpl(
		HashWrapper			_peer_id,
		int					_key_hash_code,
		byte[]				_ip,
		boolean				_ip_override,
		int					_tcp_port,
		int					_udp_port,
		int					_http_port,
		byte				_crypto_level,
		byte				_az_ver,
		long				_last_contact_time,
		boolean				_download_completed,
		byte				_last_nat_status,
		int					_up_speed,
		DHTNetworkPosition	_network_position )
	{
		peer_id				= _peer_id;
		key_hash_code		= _key_hash_code;
		ip					= _ip;
		ip_override			= _ip_override;
		tcp_port			= (short)_tcp_port;
		udp_port			= (short)_udp_port;
		http_port			= (short)_http_port;
		crypto_level		= _crypto_level;
		az_ver				= _az_ver;
		last_contact_time	= _last_contact_time;
		download_completed	= _download_completed;
		NAT_status			= _last_nat_status;	
		up_speed			= _up_speed>Short.MAX_VALUE?Short.MAX_VALUE:(short)_up_speed;
		network_position	= _network_position;
			
		resolveAndCheckNAT();
	}
	
	protected boolean
	update(
		byte[]				_ip,
		int					_port,
		int					_udp_port,
		int					_http_port,
		byte				_crypto_level,
		byte				_az_ver,
		int					_up_speed,
		DHTNetworkPosition	_network_position )
	{
		udp_port			= (short)_udp_port;
		http_port			= (short)_http_port;
		crypto_level		= _crypto_level;
		az_ver				= _az_ver;
		up_speed			= _up_speed>Short.MAX_VALUE?Short.MAX_VALUE:(short)_up_speed;
		network_position	= _network_position;
		
		boolean	res	= false;
		
		if ( _port != getTCPPort() ){
			
			tcp_port	= (short)_port;
			
			res		= true;
		}
		
		if ( !Arrays.equals( _ip, ip )){
			
			ip			= _ip;
	
			res	= true;	
		}
		
		if ( res ){
			
			resolveAndCheckNAT();
		}
		
		return( res );
	}
	
	public void
	NATCheckComplete(
		boolean		ok )
	{
		if ( ok ){
			
			NAT_status = NAT_CHECK_OK;
			
		}else{
			
			NAT_status	= NAT_CHECK_FAILED;
		}
	}
	
	protected void
	setNATStatus(
		byte		status )
	{
		NAT_status	= status;
	}
	
	public byte
	getNATStatus()
	{
		return( NAT_status );
	}
	
	protected boolean
	isNATStatusBad()
	{
		return( NAT_status == NAT_CHECK_FAILED || NAT_status == NAT_CHECK_FAILED_AND_REPORTED );
	}
	
	protected void
	resolveAndCheckNAT()
	{
		// default values pending resolution
		
		ip_str 		= new String( ip );
		ip_bytes	= null;
		
		HostNameToIPResolver.addResolverRequest( ip_str, this );
		
			// a port of 0 is taken to mean that the client can't/won't receive incoming
			// connections - tr

		if ( tcp_port == 0 ){
			
			NAT_status = NAT_CHECK_FAILED_AND_REPORTED;
			
		}else{
			
				// only recheck if we haven't already ascertained the state
			
			if ( NAT_status == NAT_CHECK_UNKNOWN ){
				
				NAT_status	= NAT_CHECK_INITIATED;
				
				if ( !TRTrackerServerNATChecker.getSingleton().addNATCheckRequest( ip_str, getTCPPort(), this )){
					
					NAT_status = NAT_CHECK_DISABLED;
				}
			}	
		}
	}
	
	public void
	hostNameResolutionComplete(
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
	
	public boolean
	isBiased()
	{
		return( biased );
	}
	
	public void
	setBiased(
		boolean	_biased )
	{
		biased	= _biased;
	}
	
	protected HashWrapper
	getPeerId()
	{
		return( peer_id );
	}
	
	public byte[]
	getPeerID()
	{
		return( peer_id.getBytes());
	}
	
	protected int
	getKeyHashCode()
	{
		return( key_hash_code );
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
	
	protected boolean
	isIPOverride()
	{
		return( ip_override );
	}
	
		/**
		 * This will return in resolution of the address is not complete or fails
		 * @return
		 */
	
	protected byte[]
	getIPAddressBytes()
	{
		return( ip_bytes );
	}
	
	public int
	getTCPPort()
	{
		return( tcp_port&0xffff );
	}
	
	protected int
	getUDPPort()
	{
		return( udp_port&0xffff );
	}
	
	protected int
	getHTTPPort()
	{
		return( http_port&0xffff );
	}
	
	protected byte
	getCryptoLevel()
	{
		return( crypto_level );
	}
	
	protected byte
	getAZVer()
	{
		return( az_ver );
	}
	
	protected int
	getUpSpeed()
	{
		return( up_speed&0xffff );
	}
	
	protected DHTNetworkPosition
	getNetworkPosition()
	{
		return( network_position );
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
	
	public void
	setUserData(
		Object		key,
		Object		data )
	{
		if ( user_data == null ){
			
			user_data = new Object[]{ key, data };
			
		}else if ( user_data instanceof Object[]){
			
			Object[]	x = (Object[])user_data;
			
			if ( x[0] == key ){
				
				x[1] = data;
				
			}else{
				
				HashMap	map = new HashMap();
				
				user_data = map;
				
				map.put( x[0], x[1] );
				
				map.put( key, data );
			}
		}else{
			
			((Map)user_data).put( key, data );
		}
	}
	
	public Object
	getUserData(
		Object		key )
	{
		if ( user_data == null ){
			
			return( null );
			
		}else if( user_data instanceof Object[]){
			
			Object[]	x = (Object[])user_data;
			
			if ( x[0] == key ){
				
				return( x[1] );
				
			}else{
				
				return( null );
			}
		}else{
			
			return(((Map)user_data).get(key));
		}
	}
	
	protected String
	getString()
	{
		return( new String(ip) + ":" + getTCPPort() + "(" + new String(peer_id.getHash()) + ")" );
	}
}
