/*
 * File    : PeerForeignDelegate.java
 * Created : 22-Mar-2004
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

package org.gudy.azureus2.pluginsimpl.peers;

/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.core3.disk.DiskManagerRequest;
import org.gudy.azureus2.core3.peer.PEPeerStats;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.impl.PEPeerTransport;
import org.gudy.azureus2.core3.peer.impl.PEPeerControl;
import org.gudy.azureus2.plugins.peers.*;

import org.gudy.azureus2.pluginsimpl.disk.*;

public class 
PeerForeignDelegate
	implements 	PEPeerTransport
{
		// this implementation supports read-only peers (i.e. download only)
	
	protected PeerManagerImpl		manager;
	protected Peer					foreign;
	
	protected Map		data;
	
	protected
	PeerForeignDelegate(
		PeerManagerImpl		_manager,
		Peer				_foreign )
	{
		manager		= _manager;
		foreign		= _foreign;
	}
	
	public PEPeerTransport
	getRealTransport()
	{
		foreign.initialize();
			    
		manager.peerAdded( foreign );	// add here so we see connect errors in the peers view

		return( this );
	}
	
    /**
     * Should never be called
     */
    public void sendChoke() {}

    /**
     * Nothing to do if called
     */
    public void sendHave(int piece) {}

    /**
     * Should never be called
     */
    public void sendUnChoke() {}

    
    /**
     * HTTP seeds never choke us
     */
    public boolean transferAvailable() {
      return true;
    }
    
	public void
	sendCancel(
		DiskManagerRequest	request )
	{
		foreign.cancelRequest(((DiskManagerImpl)manager.getDiskManager()).lookupRequest( request ));
	}
	
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
		int pieceLength )
	{
		return( foreign.addRequest( pieceNumber, pieceOffset, pieceLength ));
	}

	public void
	closeAll(
      String reason,
	  boolean closedOnError,
	  boolean attemptReconnect)
	{
		foreign.close( reason, closedOnError, attemptReconnect );
		
	  	manager.peerRemoved(foreign);
	}
			
	public boolean isReadyToRequest() {    
	    return true;
	 }
		
	public List
	getExpiredRequests()
	{
		DiskManagerImpl dm = (DiskManagerImpl)manager.getDiskManager();

		List	reqs = foreign.getExpiredRequests();
		
		if ( reqs == null ){
			
			return( null );
		}
		
		List	res = new ArrayList();
		
		for (int i=0;i<reqs.size();i++){
			
			DiskManagerRequestImpl	dmr = (DiskManagerRequestImpl)reqs.get(i);
			
			res.add( dmr.getDelegate());
		}
		
		return( res );
	}
  		
	public int
	getNbRequests()
	{
		return( foreign.getNumberOfRequests());
	}
		
	public PEPeerControl
	getControl()
	{
			// bit of a con this
		
		return((PEPeerControl)manager.getDelegate());
	}
  
	

	 //nothing to process
	public int processRead(){ return 0;}
	public int processWrite(){ return 0;}
	  
	  //used for process() timing...not needed
	public int getReadSleepTime(){ return 0; }
	public int getWriteSleepTime(){ return 0; }
	public long getLastReadTime(){ return 0; }
	public long getLastWriteTime(){ return 0; }
	public void setReadSleepTime(int time){}
	public void setWriteSleepTime(int time){}
	public void setLastReadTime(long time){}
	public void setLastWriteTime(long time){}
	
		// PEPeer stuff
	
	public PEPeerManager
	getManager()
	{
		return( manager.getDelegate());
	}
	
	public int 
	getState()
	{
		return( foreign.getState());
	}

	public byte[] 
	getId()
	{
		return( foreign.getId());
	}


	public String 
	getIp()
	{
		return( foreign.getIp());
	}

 
	public int 
	getPort()
	{
		return( foreign.getPort());
	}

	
	public boolean[] 
	getAvailable()
	{
		return( foreign.getAvailable());
	}

 
	public void 
	setSnubbed(boolean b)
	{
		foreign.setSnubbed( b );
	}

  
	public boolean 
	isChoked()
	{
		return( foreign.isChoked());
	}


	public boolean 
	isChoking()
	{
		return( foreign.isChoking());
	}


	public boolean 
	isInterested()
	{
		return( foreign.isInterested());
	}


	public boolean 
	isInteresting()
	{
		return( foreign.isInteresting());
	}


	public boolean 
	isSeed()
	{
		return( foreign.isSeed());
	}

 
	public boolean 
	isSnubbed()
	{
		return( foreign.isSnubbed());
	}

 
	public PEPeerStats 
	getStats()
	{
		return( ((PeerStatsImpl)foreign.getStats()).getDelegate());
	}

 	
	public boolean 
	isIncoming()
	{
		return( foreign.isIncoming());
	}

	public int 
	getDownloadPriority() 
	{
	    return( foreign.getDownloadPriority());
	}

	public int 
	getPercentDone()
	{
		return( foreign.getPercentDone());
	}


	public String 
	getClient()
	{
		return( foreign.getClient());
	}


	public boolean 
	isOptimisticUnchoke()
	{
		return( foreign.isOptimisticUnchoke());
	}

	
	public void 
	hasSentABadChunk()
	{
		foreign.hasSentABadChunk();
	}
	
	public int 
	getNbBadChunks()
	{
		return( foreign.getNumberOfBadChunks());
	}
	
	public int 
	getMaxUpload() 
	{  
	    return( foreign.getMaxUpload());
	}
	

	public int getUniqueAnnounce() 
	{
	    return -1;
	}

	public int getUploadHint() 
	{
	    return 0;
	}


	public void setUniqueAnnounce(int uniquePieceNumber) {}

	public void setUploadHint(int timeToSpread) {}  
	
	public int getAllowed() 
	{    
		return 0;
	}
  
	 // Seeds should never download, and so no limit implementation is needed
	public void 
	addLimitIfNotZero(
		int addToLimit) 
	{		
	}
	
	public int 
	getLimit() 
	{
		return 0;
	}

	public void 
	setLimit(
		int newLimit) 
	{	
	}
    
	 /** To retreive arbitrary objects against a peer. */
	  public Object getData (String key) {
	  	if (data == null) return null;
	    return data.get(key);
	  }

	  /** To store arbitrary objects against a peer. */
	  public synchronized void setData (String key, Object value) {
	  	if (data == null) {
	  	  data = new HashMap();
	  	}
	    if (value == null) {
	      if (data.containsKey(key))
	        data.remove(key);
	    } else {
	      data.put(key, value);
	    }
	  }
	  
	public boolean 
	equals(
		Object 	other )
	{
		if ( other instanceof PeerForeignDelegate ){
				
			return( foreign.equals(((PeerForeignDelegate)other).foreign ));
		}
			
		return( false );
	}
		
	public int
	hashCode()
	{
		return( foreign.hashCode());
	}
}
