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
import org.gudy.azureus2.core3.download.DownloadManager;

public interface
PEPeerTransport
{
	public static final int HIGH_PRIORITY	= DownloadManager.HIGH_PRIORITY;
	
	public int
	getState();
	
	public void
	process();
	
	public void
	sendChoke();
	
	public void
	sendUnChoke();
	
	public void
	sendHave(
		int		piece );
		
	public void
	sendCancel(
		DiskManagerRequest	request );
	
  /**
   * 
   * @param pieceNumber
   * @param pieceOffset
   * @param pieceLength
   * @return true is the piece is really requested
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
		
	public void
	setSnubbed(
		boolean	snubbed );
		
	public boolean
	isReadyToRequest();
	
	public boolean
	isChoking();
	
	public boolean
	isSnubbed();
	
	public boolean
	isInteresting();
	
	public boolean
	isInterested();

	public boolean
	isSeed();
	
	public boolean
	transferAvailable();
	
	public List
	getExpiredRequests();
  	
	public boolean[]
	getAvailable();
	
	public int
	getNbRequests();
	
	public int
	getPercentDone();
	
	public PEPeerControl
	getManager();
	
	public String 
	getIp();

	public int
	getPort();
	
	public byte[]
	getId();
	
	public PEPeerStats
	getStats();
}