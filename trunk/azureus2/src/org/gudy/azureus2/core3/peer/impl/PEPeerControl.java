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


import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.disk.DiskManagerRequest;
import org.gudy.azureus2.core3.disk.DiskManagerDataQueueItem;

public interface
PEPeerControl
	extends PEPeerManager, PEPeerServerAdapter
{
  
  public static final int WAITING_SLEEP        = 75;
  public static final int DATA_EXPECTED_SLEEP  = 35;
  public static final int NO_SLEEP             = 10;
   	

	   
	public DiskManagerDataQueueItem
	createDiskManagerDataQueueItem(
	  DiskManagerRequest	req );
	
	public void
	enqueueReadRequest(
		DiskManagerDataQueueItem	item );
		
	public void
	freeRequest(
		DiskManagerDataQueueItem	item );
		

	public boolean 
	checkBlock(
		int pieceNumber, 
		int offset, 
		int length );


  /*
	public boolean 
	validateHandshaking( 
		PEPeer pc, 
		byte[] peerId );
  */

	public void 
	havePiece(
		int 		pieceNumber, 
		PEPeer 		pcOrigin );

	public void 
	haveNewPiece();

	public boolean 
	isOptimisticUnchoke(
		PEPeer 		pc);

	
	public void
	updateSuperSeedPiece(
	    PEPeer peer,
	    int pieceNumber);
	
	public void
	addListener(
		PEPeerControlListener	l );
	
	public void
	removeListener(
		PEPeerControlListener	l );
	
}