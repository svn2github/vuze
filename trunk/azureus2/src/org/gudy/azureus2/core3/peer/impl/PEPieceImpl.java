/*
 * File    : PEPieceImpl.java
 * Created : 15-Oct-2003
 * By      : stuff
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

public class 
PEPieceImpl
	implements PEPiece
{  
  public int length;
  public int nbBlocs;
  public int pieceNumber;

  public int lastBlocSize;

  public boolean[] downloaded;
  public boolean[] requested;
  public boolean[] written;
  public int completed;
  public boolean isBeingChecked = false;

  // Note by Moti:
  // TODO: find some way of removing this! The only place it's actually accessed is in 1 place in the UI (org.gudy.azureus2.ui.swt.PiecesView)  
  public PEPeerManager manager;

  public PEPieceImpl(PEPeerManager manager, int length) {
	this.manager = manager;
	//System.out.println("Creating Piece of Size " + length); 
	this.length = length;
	nbBlocs = (length + PEPeerManager.BLOCK_SIZE - 1) / PEPeerManager.BLOCK_SIZE;
	downloaded = new boolean[nbBlocs];
	requested = new boolean[nbBlocs];
	written = new boolean[nbBlocs];

	if ((length % PEPeerManager.BLOCK_SIZE) != 0)
	  lastBlocSize = length % PEPeerManager.BLOCK_SIZE;
	else
	  lastBlocSize = PEPeerManager.BLOCK_SIZE;

  }

  public PEPieceImpl(PEPeerManager manager, int length, int pieceNumber) {
	this(manager, length);
	this.pieceNumber = pieceNumber;
  }

  public PEPeerManager
  getManager()
  {
  	return( manager );
  }
  
  public void setWritten(int blocNumber) {
	written[blocNumber] = true;
	completed++;
  }

  public boolean isComplete() {
	boolean complete = true;
	for (int i = 0; i < nbBlocs; i++) {
	  complete = complete && written[i];
	}
	return complete;
  }

  public boolean isWritten(int blockNumber) {
	return written[blockNumber];
  }
  public boolean[] getWritten()
  {
  	return( written );
  }

  public boolean[] getRequested(){
  	return( requested );
  }
  public void setBloc(int blocNumber) {
	downloaded[blocNumber] = true;    
  }

  // This method is used to clear the requested information
  public void clearRequested(int blocNumber) {
	requested[blocNumber] = false;
  }

  // This method will return the first non requested bloc and
  // will mark it as requested
  public synchronized int getAndMarkBlock() {
	int blocNumber = -1;
	for (int i = 0; i < nbBlocs; i++) {
	  if (!requested[i] && !written[i]) {
		blocNumber = i;
		requested[i] = true;

		//To quit loop.
		i = nbBlocs;
	  }
	}
	return blocNumber;
  }

  public synchronized void unmarkBlock(int blocNumber) {
	if (!downloaded[blocNumber])
	  requested[blocNumber] = false;
  }

  public int getBlockSize(int blocNumber) {
	if (blocNumber == (nbBlocs - 1))
	  return lastBlocSize;
	return PEPeerManager.BLOCK_SIZE;
  }

  public void free() {
  }

  public int getCompleted() {
	return completed;
  }
  
  public int getPieceNumber(){
  	return( pieceNumber );
  }
  public int getLength(){
  	return( length );
  }
  public int getNbBlocs(){
  	return( nbBlocs );  
  }
  
  public void setBeingChecked() {
	this.isBeingChecked = true;
  }

  public boolean isBeingChecked() {
	return this.isBeingChecked;
  }

  /**
   * @param manager
   */
  public void setManager(PEPeerManager manager) {
	this.manager = manager;
  }
}