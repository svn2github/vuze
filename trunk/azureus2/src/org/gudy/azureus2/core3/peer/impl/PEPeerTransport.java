/*
 * File    : PEPeerTransport
 * Created : 15-Oct-2003
 * By      : Olivier
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
 
  /*
 * Created on 4 juil. 2003
 *
 */
package org.gudy.azureus2.core3.peer.impl;

import java.util.List;

import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.disk.*;

import com.aelitis.azureus.core.peermanager.peerdb.PeerItem;

public interface
PEPeerTransport
	extends PEPeer
{	

  public static final int CONNECTION_PENDING                = 0;
  public static final int CONNECTION_CONNECTING             = 1;
  public static final int CONNECTION_WAITING_FOR_HANDSHAKE  = 2;
  public static final int CONNECTION_FULLY_ESTABLISHED      = 4;
  

  	
	public void
	sendChoke();
	
	public void
	sendUnChoke();
	
	public void
	sendHave(
		int		piece );
		
	public void
	sendCancel(
		DiskManagerReadRequest	request );
	
  /**
   * 
   * @param pieceNumber
   * @param pieceOffset
   * @param pieceLength
   * @return true if the piece is really requested
   */
	public boolean 
	request(
		int pieceNumber, 
		int pieceOffset, 
		int pieceLength );

  
  /**
   * Close the peer connection
   * @param reason for closure
   */
	public void closeConnection( String reason );
			
		
	public boolean
	transferAvailable();
	
	public List
	getExpiredRequests();
  		
	public int
	getNbRequests();
	
	public PEPeerControl
	getControl();
  
  
  
	/**
	 * Check if we need to send a keep-alive message.
	 * A keep-alive is sent if no other message has been sent within the last 2min.
	 */
	public void doKeepAliveCheck();
  
  /**
   * Check for possible connection timeouts.
   * @return true if the connection has been timed-out, false if not
   */
  public boolean doTimeoutChecks();

  
  /**
   * Perform checks related to performance optimizations,
   * i.e. tune buffering related to send/receive speed.
   */
  public void doPerformanceTuningCheck();
  
  
  /**
   * Get the specific peer connection state.
   * @return connection state
   */
  public int getConnectionState();
  
  /**
   * Get the time since this connection was first established.
   * NOTE: This method will always return 0 at any time before
   * the underlying transport is fully connected, i.e. before
   * handshaking begins.
   * @return time count in ms
   */
  public long getTimeSinceConnectionEstablished();
  
  /**
   * Get the time since the last (most-recent) data (payload) message was received.
   * @return time count in ms, or -1 if we've never received a data message from them
   */
  public long getTimeSinceLastDataMessageReceived();
  
  
  /**
   * Get the time since the last (most-recent) data (payload) message was sent.
   * @return time count in ms, or -1 if we've never sent them a data message
   */
  public long getTimeSinceLastDataMessageSent();
  
  
  /**
   * Do any peer exchange processing/updating.
   */
  public void updatePeerExchange();
  
  
  /**
   * Get the peer's address + port identification item.
   * @return id
   */
  public PeerItem getPeerItemIdentity();
  
}