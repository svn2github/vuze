/*
 * Created on 22 juil. 2003
 *
 */
package org.gudy.azureus2.core3.tracker.client.classic;

/**
 * @author Olivier
 * 
 */

import org.gudy.azureus2.core3.tracker.client.*;

public class 
TRTrackerScraperResponseImpl 
	implements TRTrackerScraperResponse
{
    protected int seeds;
    protected int peers;

    protected TRTrackerScraperResponseImpl(int seeds, int peers) {
      this.seeds = seeds;
      this.peers = peers;
    }

	public int
	getSeeds()
	{
		return( seeds );
	}
	
	public int
	getPeers()
	{
		return( peers);
	}
}
