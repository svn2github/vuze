/*
 * Created on 06-Oct-2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.gudy.azureus2.core3.tracker.client.classic;

/**
 * @author gardnerpar
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

import org.gudy.azureus2.core3.tracker.client.*;

public class 
TRTrackerResponsePeerImpl
	implements TRTrackerResponsePeer 
{
	protected byte[]		peer_id;
	protected String		ip_address;
	protected int			port;
	
	protected
	TRTrackerResponsePeerImpl(
		byte[]		_peer_id,
		String		_ip_address,
		int			_port )
	{
		peer_id		= _peer_id;
		ip_address	= _ip_address;
		port		= _port;
	}
	
	public byte[]
	getPeerId()
	{
		return( peer_id );
	}
	
	public String
	getIPAddress()
	{
		return( ip_address );
	}
	
	public int
	getPort()
	{
		return( port );
	}
}
