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
	protected byte[]	hash;
    protected int 		seeds;
    protected int 		peers;
    protected boolean 	valid;

    protected 
    TRTrackerScraperResponseImpl(
    	byte[]	_hash,
		int 	_seeds, 
		int 	_peers) 
    {
    	hash		= _hash;
    	seeds 		= _seeds;
    	peers 	= _peers;
      
    	if( seeds == -1 && peers == -1 ){
    		
    		valid = false;
    		
    	}else{
    		
    		valid = true;
    	}
    }

    public byte[]
    getHash()
    {
    	return( hash );
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
  
  public boolean
  isValid()
  {
    return( valid);
  }
}
