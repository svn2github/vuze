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

public interface
PEPeerTransport
	extends PEPeer
{	

  public static final int CONNECTION_PENDING                = 0;
  public static final int CONNECTION_CONNECTING             = 1;
  public static final int CONNECTION_WAITING_FOR_HANDSHAKE  = 2;
  public static final int CONNECTION_WAITING_FOR_BITFIELD   = 3;
  public static final int CONNECTION_FULLY_ESTABLISHED      = 4;
  

	public int
	processRead();
  	
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

	public void
	closeAll(
      String reason,
	  boolean closedOnError,
	  boolean attemptReconnect);
			
	public boolean
	isReadyToRequest();
		
	public boolean
	transferAvailable();
	
	public List
	getExpiredRequests();
  		
	public int
	getNbRequests();
	
	public int
	getPercentDone();
	
	public PEPeerControl
	getControl();
  
	public int getReadSleepTime();
	public long getLastReadTime();
  
	public void setReadSleepTime(int time);
	public void setLastReadTime(long time);
  
  
	/**
	 * Check if we need to send a keep-alive message.
	 * A keep-alive is sent if no other message has been sent within the last 2min.
	 */
	public void doKeepAliveCheck();
  
  
  /**
   * Get the specific post-socket-establishment connection state.
   * @return connection state
   */
  public int getConnectionState();
  
}