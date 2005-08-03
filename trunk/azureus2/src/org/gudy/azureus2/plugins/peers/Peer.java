/*
 * File    : Peer.java
 * Created : 01-Dec-2003
 * By      : parg
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

package org.gudy.azureus2.plugins.peers;

/**
 * @author parg
 *
 */

import java.util.List;

import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.plugins.disk.DiskManagerRequest;
import org.gudy.azureus2.plugins.messaging.Message;
import org.gudy.azureus2.plugins.network.Connection;

public interface 
Peer 
{
	public final static int CONNECTING 		= PEPeer.CONNECTING;
	public final static int HANDSHAKING 	= PEPeer.HANDSHAKING;
	public final static int TRANSFERING 	= PEPeer.TRANSFERING;
  public final static int DISCONNECTED  = PEPeer.DISCONNECTED;
  

  
	public PeerManager
	getManager();
	
	public int getState();	// from above set

	public byte[] getId();
  
  
  /**
   * Get the peer's local TCP connection port.
   * @return local port
   */
	public String getIp();
  
  
  /**
   * Get the TCP port this peer is listening for incoming connections on.
   * @return TCP port, or 0 if port is unknown
   */
  public int getTCPListenPort();
  
  /**
   * Get the UDP port this peer is listening for incoming connections on.
   * @return UDP port, or 0 if port is unknown
   */
  public int getUDPListenPort();
  
 
	public int getPort();
	
	public boolean[] getAvailable();
   
	public boolean isChoked();

	public boolean isChoking();

	public boolean isInterested();

	public boolean isInteresting();

	public boolean isSeed();
 
	public boolean isSnubbed();
 
	public void setSnubbed( boolean snubbed );
	
	public PeerStats getStats();
 	
	public boolean isIncoming();

		/**
		 * @deprecated This erroneously returns percent in 1000 (i.e. 100% = 1000 :) Therefore replaces
		 * with something more accurately named!
		 * @return
		 */
	
	public int getPercentDone();

	public int getPercentDoneInThousandNotation();
	
	public String getClient();

	public boolean isOptimisticUnchoke();
  
  public void setOptimisticUnchoke( boolean is_optimistic );
	
	public void
	initialize();
	
	public List
	getExpiredRequests();
  		
	public int
	getNumberOfRequests();

	public void
	cancelRequest(
		DiskManagerRequest	request );

 
	public boolean 
	addRequest(
		int pieceNumber, 
		int pieceOffset, 
		int pieceLength );


	public void
	close(
		String 		reason,
		boolean 	closedOnError,
		boolean 	attemptReconnect );
	

  
  /**
   * Add peer listener.
   * @param listener
   */
	public void	addListener( PeerListener	listener );
	

  /**
   * Remove peer listener.
   * @param listener
   */
	public void removeListener(	PeerListener listener );
  
  
  
  /**
   * Get the network connection that backs this peer.
   * @return connection
   */
  public Connection getConnection();
  
  
  /**
   * Whether or not this peer supports the advanced messaging API.
   * @return true if extended messaging is supported, false if not
   */
  public boolean supportsMessaging();
  
  
  /**
   * Get the list of messages that this peer and us mutually understand.
   * @return messages available for use, or null of supported is yet unknown
   */
  public Message[] getSupportedMessages();

}
