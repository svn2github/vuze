/*
 * Created on 03-Oct-2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.gudy.azureus2.core3.tracker.server;

/**
 * @author gardnerpar
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

import org.gudy.azureus2.core3.tracker.server.impl.*;

public class 
TRTrackerServerFactory 
{
	public static TRTrackerServer
	create(
		int		port,
		int		retry_delay )
		
		throws TRTrackerServerException
	{
		return( new TRTrackerServerImpl( port, retry_delay ));
	}
}
