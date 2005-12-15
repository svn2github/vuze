package org.gudy.azureus2.core3.disk;

import org.gudy.azureus2.plugins.peers.PeerReadRequest;

/**
 * 
 * This class represents a Bittorrent Request.
 * and a time stamp to know when it was created.
 * 
 * Request may expire after some time, which is used to determine who is snubbed.
 * 
 * @author Olivier
 *
 * 
 */
public interface 
DiskManagerReadRequest
	extends PeerReadRequest
{  
	public boolean isExpired();
  
	/**
	 * Allow some more time to the request.
	 * Typically used on peers that have just sent some data, we reset all
	 * other requests to give them extra time.
	 */
	public void reSetTime();
 
	public int getPieceNumber();
 
	public int getOffset();
 
	public int getLength();
	
	public long getTimeCreated();
	
		/**
		 * If flush is set then data held in memory will be flushed to disk during the read operation
		 * @param flush
		 */
	
	public void
	setFlush(
		boolean	flush );
	
	public boolean
	getFlush();
}