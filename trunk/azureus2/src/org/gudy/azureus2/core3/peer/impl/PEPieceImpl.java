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
 * @author MjrTom
 *			2005/Oct/08: numerous changes for new piece-picking
 *
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerPiece;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.SystemTime;

public class 
PEPieceImpl
	implements PEPiece
{  
  private DiskManagerPiece	dm_piece;
	  
  private boolean[] downloaded;
  private boolean[] requested;
  
  private PEPeer[] 	writers;
  private List 		writes;

  private PEPeer reservedBy;
    
	public boolean		isBeingChecked = false;
	private long		creation_time;

	//In end game mode, this limitation isn't used
	private int			pieceSpeed;			//slower peers dont slow down fast pieces too much

	public PEPeerManager	manager;

  	// experimental class level lock
  
  protected static AEMonitor 	class_mon	= new AEMonitor( "PEPiece:class");

	public 
	PEPieceImpl(
		PEPeerManager 		_manager, 
		DiskManagerPiece	_dm_piece,
		int					_pieceSpeed,
		boolean				_recovered )
	{
		manager 	= _manager;
		dm_piece	= _dm_piece;
		pieceSpeed	= _pieceSpeed;

		int	nbBlocs 	= dm_piece.getBlockCount();

		downloaded 	= new boolean[nbBlocs];
		requested 	= new boolean[nbBlocs];
		writers 	= new PEPeer[nbBlocs];
		writes 		= new ArrayList(0);

		creation_time =SystemTime.getCurrentTime();
		if (!_recovered)
		{
			dm_piece.setInitialWriteTime();
		} else
		{
			pieceSpeed =0;
		}
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

	// This method will return the first non requested block and
	// will mark it as requested, unless it's been too long since
	// anything was written to the piece, in which case it'll return
	// the first non-written block so it can be re-requested
	/**
	 * Assumption - single threaded access to this
	 */
	public int 
	getAndMarkBlock() 
	{
		for (int i =0; i <requested.length; i++)
		{
			if (!requested[i] &&!downloaded[i] &&!dm_piece.getWritten(i))
			{
				requested[i] =true;
				return i;
			}
		}
		return -1;
	}

	// find a block w/o requesting it
	public int getBlock() 
	{
		for (int i =0; i <requested.length; i++)
		{
			if (!requested[i] &&!dm_piece.getWritten(i) &&!downloaded[i])
			{
				return i;
			}
		}
		return -1;
	}

	/**
	 * This method is safe in a multi-threaded situation as the worst that it can
	 * do is mark a block as not requested even though its downloaded which may lead
	 * to it being downloaded again
	 */

	public void unmarkBlock(int blocNumber)
	{
		requested[blocNumber] =downloaded[blocNumber];
	}
  
  public void
  reDownloadBlocks(
	String	writer )
  {
	  for (int i=0;i<writers.length;i++){
	  
		  PEPeer peer = writers[i];
		  
		  if ( peer != null && peer.getIp().equals( writer )){
			  		  
			  reDownloadBlock( i );
		  }
	  }
  }
  
	/**
	 * Assumption - single threaded with getAndMarkBlock
	 */
	public boolean 
	markBlock(int blockNumber) 
	{
		if (!downloaded[blockNumber])
		{
			return requested[blockNumber] =true;
		}
		return false;
	}

  public int 
  getBlockSize(
  	int blocNumber) 
  {
	if ( blocNumber == (downloaded.length - 1)){
	
		int	length = dm_piece.getLength();
		
		if ((length % DiskManager.BLOCK_SIZE) != 0){
		
			return( length % DiskManager.BLOCK_SIZE );
		}
	}
	
	return DiskManager.BLOCK_SIZE;
  }

  public int 
  getPieceNumber()
  {
  	return( dm_piece.getPieceNumber() );
  }
  public int getLength(){
  	return( dm_piece.getLength() );
  }
  public int getNbBlocs(){
  	return( downloaded.length );  
  }
  
  public void setBeingChecked() {
	this.isBeingChecked = true;
  }
  
  public void setBeingChecked(boolean checking) {
  this.isBeingChecked = checking;
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
  
  public List 
  getPieceWrites( 
	String	ip ) 
  {
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
	      
	      if ( !write.getSender().getIp().equals( ip )){
	    	 
	        iter.remove();
	      }
	   }
	   
	  return result;
  }
  
  public void reset() {
  	dm_piece.reset();
  	
  	int	nbBlocs = downloaded.length;
  	
    downloaded 	= new boolean[nbBlocs];
    requested 	= new boolean[nbBlocs];
    writers 	= new PEPeer[nbBlocs];
    
    isBeingChecked = false;
    reservedBy = null;
 
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
  
	public int getSpeed() {
		return pieceSpeed;
	}

	public void setSpeed(int speed) {
		pieceSpeed =speed;
	}

  	// written can be null, in which case if the piece is complete, all blocks are complete
  	// otherwise no blocks are complete
  
  public boolean[] 
  getWritten()
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
  
  public boolean
  isNeeded()
  {
	  return( dm_piece.isNeeded());
  }
  
  /**
   * @return Returns the manager.
   */
  public PEPeerManager getManager() {
    return manager;
  }
  
  public void setReservedBy(PEPeer peer) {
   this.reservedBy = peer;   
  }
  
  public PEPeer getReservedBy() {
    return this.reservedBy;
  }
  
  public void reDownloadBlock(int blockNumber) {
    downloaded[blockNumber] = false;
    requested[blockNumber] = false;
    dm_piece.reDownloadBlock(blockNumber);
  }

	public long getCreationTime()
	{
		long now =SystemTime.getCurrentTime();
		if (now >=creation_time)
		{
			return creation_time;
		}
		creation_time =now;
		return creation_time;
	}

	public int getNbRequests()
	{
		int	result =0;
		for (int i =dm_piece.getBlockCount() -1; i >=0 ;i--)
		{
			if (!downloaded[i] &&requested[i])
				result++;
		}
		return result;
	}

	public DiskManagerPiece getDMPiece()
	{
		return dm_piece;
	}

	public long getPriority(){
		return dm_piece.getPriority();
	}

	public void setResumePriority(long p)
	{
		dm_piece.setResumePriority(p);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.core3.peer.PEPiece#getResumePriority()
	 */
	public long getResumePriority()
	{
		return dm_piece.getResumePriority();
	}

	public int getNbUnrequested()
	{
		int	result =0;
		for (int i =dm_piece.getBlockCount() -1; i >=0 ;i--)
		{
			if (!downloaded[i] &&!requested[i])
				result++;
		}
		return result;
	}

}