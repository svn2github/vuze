/*
 * File    : TRTrackerServerImpl.java
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

import org.gudy.azureus2.core3.tracker.server.*;

public class 
TRTrackerServerImpl 
	implements TRTrackerServer
{
	protected int	retry_interval;
	
	public
	TRTrackerServerImpl(
		int		port,
		int		_retry_interval )
		
		throws TRTrackerServerException
	{
		retry_interval	= _retry_interval;
		
		try{
			ServerSocket ss = new ServerSocket( port );
			
			while(true){
				
				Socket socket = ss.accept();
				
				new TRTrackerServerProcessor( this, socket );
			}
		}catch( Throwable e ){
			
			throw( new TRTrackerServerException( "TRTrackerServer: accept fails: " + e.toString()  ));
		}	
	}
	
	public int
	getRetryInterval()
	{
		return( retry_interval );
	}
}
