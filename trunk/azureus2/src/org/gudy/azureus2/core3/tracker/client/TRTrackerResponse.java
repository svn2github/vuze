/*
 * Created on 06-Oct-2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.gudy.azureus2.core3.tracker.client;

/**
 * @author gardnerpar
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public interface 
TRTrackerResponse 
{
	public static final int	ST_OFFLINE			= 0;
	public static final int ST_REPORTED_ERROR	= 1;
	public static final int	ST_ONLINE			= 2;
	
	/**
	 * Returns the current status of the tracker
	 * @return	see above ST_ set
	 */
	
	public int
	getStatus();
	
	/**
	 * This value is always available
	 * @return time to wait before requerying tracker
	 */
	
	public long
	getTimeToWait();
	
	/**
	 * if the status is ST_REPORTED_ERROR then this method is of use
	 * @return	failure reason as reported by tracker.
	 */
	
	public String
	getFailureReason();
	
	/**
	 * 
	 * @return	peers reported by tracker. this will include the local peer as well
	 */
	
	public TRTrackerResponsePeer[]
	getPeers();
	
	public void
	print();
}
