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
TRTrackerResponseImpl
	implements TRTrackerResponse 
{
	protected int		status;
	protected long		time_to_wait;
	protected String	failure_reason;
	
	protected TRTrackerResponsePeer[]	peers;
	
	protected
	TRTrackerResponseImpl(
		int		_status,
		long	_time_to_wait  )
	{
		status			= _status;	
		time_to_wait	= _time_to_wait;
	}
	
	protected
	TRTrackerResponseImpl(
		int		_status,
		long	_time_to_wait,
		String	_failure_reason )
	{
		status			= _status;	
		time_to_wait	= _time_to_wait;
		failure_reason	= _failure_reason;
	}
	
	protected
	TRTrackerResponseImpl(
		int						_status,
		long					_time_to_wait,
		TRTrackerResponsePeer[]	_peers )
	{
		status			= _status;	
		time_to_wait	= _time_to_wait;
		peers			= _peers;
	}
	
	public int
	getStatus()
	{
		return( status );
	}
	
	public long
	getTimeToWait()
	{
		return( time_to_wait );
	}
	
	public String
	getFailureReason()
	{
		return( failure_reason );
	}
	
	public TRTrackerResponsePeer[]
	getPeers()
	{
		return( peers );
	}
	
	public void
	print()
	{
		System.out.println( "TRTrackerResponse::print");
		System.out.println( "\tstatus = " + getStatus());
		System.out.println( "\tfail msg = " + getFailureReason());
		System.out.println( "\tpeers:" );
		
		if ( peers != null ){
					
			for (int i=0;i<peers.length;i++){
				
				TRTrackerResponsePeer	peer = peers[i];
				
				System.out.println( "\t\t" + peer.getIPAddress() + ":" + peer.getPort());
			}
		}
	}
}
