/*
 * Created on 04-Oct-2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.gudy.azureus2.core3.tracker.client;

/**
 * @author stuff
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.tracker.client.classic.*;

public class 
TRTrackerClientFactory 
{
	public static TRTrackerClient
	create(
		TOTorrent		torrent,
		int				port )
		
		throws TRTrackerClientException

	{
		return( new TRTrackerClientClassicImpl( torrent, port ));
	}
}
