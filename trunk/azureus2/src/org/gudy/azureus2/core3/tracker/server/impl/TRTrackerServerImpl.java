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
