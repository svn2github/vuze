/*
 * File    : PEPieceImpl.java
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

package org.gudy.azureus2.core3.peer.impl;

/**
 * @author parg
 *
 */

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.SystemTime;

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
  
  private PEPeer[] 	writers;
  public List 		writes;
  private long		last_write_time;
    
  public int completed;
  public boolean isBeingChecked = false;

  //A Flag to indicate that this piece is for slow peers
  //Slow peers can only continue/create slow pieces
  //Fast peers can't continue/create slow pieces
  //In end game mode, this limitation isn't used
  private boolean slowPiece;
  
  public PEPeerManager manager;

  	// experimental class level lock
  
  protected static AEMonitor 	class_mon	= new AEMonitor( "PEPiece:class");

  protected PEPieceImpl(PEPeerManager manager, int length) {
    
    
	this.manager = manager;
	  
  
	this.length = length;
	nbBlocs = (length + PEPeerManager.BLOCK_SIZE - 1) / PEPeerManager.BLOCK_SIZE;
	this.writes = new ArrayList(0);
	downloaded = new boolean[nbBlocs];
	requested = new boolean[nbBlocs];
	written = new boolean[nbBlocs];
	writers = new PEPeer[nbBlocs];

	if ((length % PEPeerManager.BLOCK_SIZE) != 0)
	  lastBlocSize = length % PEPeerManager.BLOCK_SIZE;
	else
	  lastBlocSize = PEPeerManager.BLOCK_SIZE;
	
	last_write_time = SystemTime.getCurrentTime();

  }

  public PEPieceImpl(PEPeerManager manager, int length, int pieceNumber,boolean slowPiece) {
    this(manager, length);
	this.pieceNumber = pieceNumber;
	this.slowPiece = slowPiece;
  }
  
  public PEPieceImpl(PEPeerManager manager, int length, int pieceNumber) {
	this(manager, length,pieceNumber,true);	
  }
    
  public void setWritten(PEPeer peer,int blocNumber) {
    writers[blocNumber] = peer;
    written[blocNumber] = true;
    completed++;
    
    last_write_time	= SystemTime.getCurrentTime();
  }

  public long
  getLastWriteTime()
  {
  	return( last_write_time );
  }
  
  public int 
  getAvailability()
  {
  	if ( manager == null ){
  		
  		return( 0 );
  	}
  	
  	return( manager.getAvailability( pieceNumber ));
  }

  public boolean isComplete() {
  	boolean complete = true;
  	for (int i = 0; i < nbBlocs; i++) {
  		complete = complete && written[i];
  		if (!complete) return false;
  	}
	  return complete;
  }

  public boolean isWritten(int blockNumber) {
  	return written[blockNumber];
  }
  
  public boolean[] getWritten() {
  	return( written );
  }

  public boolean[] getRequested(){
  	return( requested );
  }
  
  public boolean[] getDownloaded(){
    return( downloaded );
  }
  
  
  public void setBlockWritten(int blocNumber) {
	downloaded[blocNumber] = true;    
  }

  // This method is used to clear the requested information
  public void clearRequested(int blocNumber) {
	requested[blocNumber] = false;
  }

  // This method will return the first non requested bloc and
  // will mark it as requested
  public int getAndMarkBlock() {
  	try{
	  	class_mon.enter();
	  	
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
  	}finally{
  		
  		class_mon.exit();
  	}
  }

  public void unmarkBlock(int blocNumber) {
  	try{
  		class_mon.enter();
  	
  		if (!downloaded[blocNumber])
  			requested[blocNumber] = false;
  	}finally{
  		
  		class_mon.exit();
  	}
  }
  
  public void markBlock(int blocNumber) {
  	try{
  		class_mon.enter();
  	
  		if (!downloaded[blocNumber])
  			requested[blocNumber] = true;
  	}finally{
  		
  		class_mon.exit();
  	}
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

  public List getPieceWrites() {
    List result;
    try{
    	class_mon.enter();
    
    	result = new ArrayList(writes);
    }finally{
    	
    	class_mon.exit();
    }
    return result;
  }


  public List getPieceWrites(int blockNumber) {
    List result;
    try{
    	class_mon.enter();
    
    	result = new ArrayList(writes);
      
    }finally{
    	
    	class_mon.exit();
    }
    Iterator iter = result.iterator();
    while(iter.hasNext()) {
      PEPieceWriteImpl write = (PEPieceWriteImpl) iter.next();
      if(write.getBlockNumber() != blockNumber)
        iter.remove();
    }
    return result;
  }


  public List getPieceWrites(PEPeer peer) {
    List result;
     try{
     	class_mon.enter();
     
     	result = new ArrayList(writes);
     }finally{
     	class_mon.exit();
     }
    Iterator iter = result.iterator();
    while(iter.hasNext()) {
      PEPieceWriteImpl write = (PEPieceWriteImpl) iter.next();
      if(peer == null || ! peer.equals(write.getSender()))
        iter.remove();
    }
     return result;
  }
  
  public void reset() {
    downloaded = new boolean[nbBlocs];
    requested = new boolean[nbBlocs];
    written = new boolean[nbBlocs];
    writers = new PEPeer[nbBlocs];
    isBeingChecked = false;
    completed = 0;
  }
  
  protected void addWrite(PEPieceWriteImpl write) {
  	try{
  		class_mon.enter();
  
  		writes.add(write);
  		
  	}finally{
  		
  		class_mon.exit();
  	}
  }
  
  public void 
  addWrite(
  		int blockNumber,
		PEPeer sender, 
		byte[] hash,
		boolean correct	)
  {
  	addWrite( new PEPieceWriteImpl( blockNumber, sender, hash, correct ));
  }
  public PEPeer[] getWriters() {
    return writers;
  }
  
  public boolean isSlowPiece() {
    return slowPiece;
  }
  
  public void setSlowPiece(boolean slowPiece) {
    this.slowPiece = slowPiece;
  }

  /**
   * @return Returns the manager.
   */
  public PEPeerManager getManager() {
    return manager;
  }
}