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

import java.nio.ByteBuffer;

import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.disk.DiskManagerRequest;
import org.gudy.azureus2.core3.disk.DiskManagerDataQueueItem;

public interface
PEPeerControl
	extends PEPeerManager
{
	public PEPeerStatsImpl
	createPeerStats();
	
	public void 
	peerAdded(PEPeer pc);

	public void 
	peerRemoved(PEPeer pc);

	public void
	addPeerTransport(
		PEPeerTransport	transport );
		
	public DiskManagerRequest
	createDiskManagerRequest(
	   int pieceNumber,
	   int offset,
	   int length );
	   
	public DiskManagerDataQueueItem
	createDiskManagerDataQueueItem(
	  DiskManagerRequest	req );
	
	public void
	requestCanceled(
		DiskManagerRequest	item );
		
	public void
	enqueueReadRequest(
		DiskManagerDataQueueItem	item );
		
	public void
	freeRequest(
		DiskManagerDataQueueItem	item );
		
	public void 
	writeBlock(
		int 		pieceNumber, 
		int 		offset, 
		ByteBuffer 	data,
    PEPeer sender);
 
	public boolean 
	checkBlock(
		int pieceNumber, 
		int offset, 
		int length );

	public boolean 
	checkBlock(
		int 		pieceNumber, 
		int 		offset, 
		ByteBuffer 	data );
		
	public boolean 
	validateHandshaking( 
		PEPeer pc, 
		byte[] peerId );

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
	received(
		int		l );	
	
	public void
	sent(
		int		l );	
		
	public void
	discarded(
		int		l );
}