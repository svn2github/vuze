package org.gudy.azureus2.core3.disk;

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
}