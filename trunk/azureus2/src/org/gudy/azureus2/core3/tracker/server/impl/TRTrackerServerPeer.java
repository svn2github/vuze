/*
 * Created on 03-Oct-2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.gudy.azureus2.core3.tracker.server.impl;

/**
 * @author gardnerpar
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class 
TRTrackerServerPeer
{
	protected byte[]		peer_id;
	protected byte[]		ip;
	protected int			port;
	
	protected long			last_contact_time;
	
	protected
	TRTrackerServerPeer(
		byte[]		_peer_id,
		byte[]		_ip,
		int			_port )
	{
		peer_id		= _peer_id;
		ip			= _ip;
		port		= _port;
	}
	
	protected byte[]
	getPeerId()
	{
		return( peer_id );
	}
	
	protected byte[]
	getIP()
	{
		return( ip );
	}
	
	protected int
	getPort()
	{
		return( port );
	}
	
	protected void
	setLastContactTime(
		long		_t )
	{
		last_contact_time	= _t;
	}
	
	protected long
	getLastContactTime()
	{
		return( last_contact_time );
	}
	
	protected String
	getString()
	{
		return( new String(ip) + ":" + port + "(" + new String(peer_id) + ")" );
	}
}
