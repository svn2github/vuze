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

import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerPiece;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.util.AEMonitor;

public class 
PEPieceImpl
	implements PEPiece
{  
  protected DiskManagerPiece	dm_piece;
	 
  public boolean[] downloaded;
  public boolean[] requested;
  
  private PEPeer[] 	writers;
  private List 		writes;

    
   public boolean isBeingChecked = false;

  //A Flag to indicate that this piece is for slow peers
  //Slow peers can only continue/create slow pieces
  //Fast peers can't continue/create slow pieces
  //In end game mode, this limitation isn't used
   
  private boolean slowPiece;
  
  public PEPeerManager manager;

  	// experimental class level lock
  
  protected static AEMonitor 	class_mon	= new AEMonitor( "PEPiece:class");

  public 
  PEPieceImpl(
  	PEPeerManager 		_manager, 
	DiskManagerPiece	_dm_piece,
	boolean				_slow_piece )
  {  
	manager 	= _manager;
	dm_piece	= _dm_piece;
	slowPiece	= _slow_piece;
	
	int	length 		= dm_piece.getLength();
	
	int	nbBlocs 	= (length + DiskManager.BLOCK_SIZE - 1) / DiskManager.BLOCK_SIZE;
	
	downloaded 	= new boolean[nbBlocs];
	requested 	= new boolean[nbBlocs];
	writers 	= new PEPeer[nbBlocs];
	writes 		= new ArrayList(0);
  }

  
  public int 
  getAvailability()
  {
  	if ( manager == null ){
  		
  		return( 0 );
  	}
  	
  	return( manager.getAvailability( dm_piece.getPieceNumber()));
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
	
		int	nbBlocs 	= dm_piece.getBlockCount();

		int blocNumber = -1;
		
		for (int i = 0; i < nbBlocs; i++) {
		  if (!requested[i] && !dm_piece.getWritten(i)) {
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

  public int 
  getBlockSize(
  	int blocNumber) 
  {
	if ( blocNumber == (dm_piece.getBlockCount() - 1)){
	
		int	length = dm_piece.getLength();
		
		if ((length % DiskManager.BLOCK_SIZE) != 0){
		
			return( length % DiskManager.BLOCK_SIZE );
		}
	}
	
	return DiskManager.BLOCK_SIZE;
  }

  public int getPieceNumber(){
  	return( dm_piece.getPieceNumber() );
  }
  public int getLength(){
  	return( dm_piece.getLength() );
  }
  public int getNbBlocs(){
  	return( dm_piece.getBlockCount() );  
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
  public void setManager(PEPeerManager _manager) {
	manager = _manager;
  }

  public void setWritten(PEPeer peer,int blocNumber) {
    writers[blocNumber] = peer;
  
    dm_piece.setWritten( blocNumber );
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
  	dm_piece.reset();
  	
  	int	nbBlocs = dm_piece.getBlockCount();
  	
    downloaded 	= new boolean[nbBlocs];
    requested 	= new boolean[nbBlocs];
    writers 	= new PEPeer[nbBlocs];
    
    isBeingChecked = false;
 
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
  
  public void setSlowPiece(boolean _slowPiece) {
    slowPiece = _slowPiece;
  }

  public boolean[] getWritten()
  {
  	return( dm_piece.getWritten());
  }
  
  public boolean
  isComplete()
  {
  	return( dm_piece.getCompleted());
  }
  
  public int
  getCompleted()
  {
  	return( dm_piece.getCompleteCount());
  }
  
  public boolean
  isWritten(
  	int		bn )
  {
  	return( dm_piece.getWritten( bn ));
  }

  public long
  getLastWriteTime()
  {
  	return( dm_piece.getLastWriteTime());
  }
  
  /**
   * @return Returns the manager.
   */
  public PEPeerManager getManager() {
    return manager;
  }
}