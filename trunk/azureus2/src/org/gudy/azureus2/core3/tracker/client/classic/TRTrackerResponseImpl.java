/*
 * File    : TRTrackerResponseImpl.java
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

package org.gudy.azureus2.core3.tracker.client.classic;

import java.util.Map;

import org.gudy.azureus2.core3.tracker.client.*;

public class 
TRTrackerResponseImpl
	implements TRTrackerResponse 
{
	protected int		status;
	protected long		time_to_wait;
	protected String	failure_reason;
	
	protected TRTrackerResponsePeer[]	peers;
	
	protected Map						extensions;
	
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
	
	protected void
	setPeers(
		TRTrackerResponsePeer[]		_peers )
	{
		peers	= _peers;
	}
	
	public TRTrackerResponsePeer[]
	getPeers()
	{
		return( peers );
	}
	
	protected void
	setExtensions(
		Map		_extensions )
	{
		extensions = _extensions;
	}
	
	public Map
	getExtensions()
	{
		return( extensions );
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
