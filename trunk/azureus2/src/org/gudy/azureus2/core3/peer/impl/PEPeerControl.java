/*
 * File    : PEPeerControl.java
 * Created : 21-Oct-2003
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
 
package org.gudy.azureus2.core3.peer.impl;

/**
 * @author parg
 *
 */


import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.peer.*;


public interface
PEPeerControl
	extends PEPeerManager
{

  
  public boolean 
	checkBlock(
		int pieceNumber, 
		int offset, 
		int length );

	public void 
	havePiece(
		int pieceNumber,
		int pieceLength,
		PEPeer pcOrigin );

	public void
	updateSuperSeedPiece(
	    PEPeer peer,
	    int pieceNumber);
	
  /*
	public void
	addListener(
		PEPeerControlListener	l );
	
	public void
	removeListener(
		PEPeerControlListener	l );
  */
  
	public DiskManager getDiskManager();
	

  
  /**
   * Insert the given transport.
   * @param transport to control
   */
  public void addPeerTransport( PEPeerTransport transport );
}