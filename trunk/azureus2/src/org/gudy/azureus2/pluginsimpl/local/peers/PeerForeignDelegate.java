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

package org.gudy.azureus2.pluginsimpl.local.peers;

/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.core3.disk.DiskManagerReadRequest;
import org.gudy.azureus2.core3.peer.PEPeerListener;
import org.gudy.azureus2.core3.peer.PEPeerSource;
import org.gudy.azureus2.core3.peer.PEPeerStats;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.impl.PEPeerTransport;
import org.gudy.azureus2.core3.peer.impl.PEPeerControl;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.plugins.peers.*;

import org.gudy.azureus2.pluginsimpl.local.disk.*;
import org.gudy.azureus2.pluginsimpl.local.messaging.MessageAdapter;
import org.gudy.azureus2.pluginsimpl.local.network.ConnectionImpl;

import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.peerdb.PeerItem;
import com.aelitis.azureus.core.peermanager.peerdb.PeerItemFactory;

public class 
PeerForeignDelegate
	implements 	PEPeerTransport
{
		// this implementation supports read-only peers (i.e. download only)
	
	protected PeerManagerImpl		manager;
	protected Peer					foreign;
	
	protected Map		data;
	
	protected AEMonitor	this_mon	= new AEMonitor( "PeerForeignDelegate" );

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
		DiskManagerReadRequest	request )
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
		
  
  public void closeConnection( String reason ) {
    foreign.close( reason, false, false );  //TODO implement reconnect?
    manager.peerRemoved(foreign);
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
  
	
  public void updatePeerExchange(){}
  
  
  public PeerItem getPeerItemIdentity() {
    return PeerItemFactory.createPeerItem( foreign.getIp(), foreign.getPort(), PeerItemFactory.PEER_SOURCE_PLUGIN );
  }
  
  
  public int getConnectionState() {  return 0;  }
	  
  
  public void doKeepAliveCheck() {}
  
  public boolean doTimeoutChecks() {
    return false;
  }
  
  public void doPerformanceTuningCheck() { }
  
  public long getTimeSinceConnectionEstablished() {
    return 0;
  }
  
  public long getTimeSinceLastDataMessageReceived() {
    return 0;
  }
  
  public long getTimeSinceLastDataMessageSent() {
    return 0;
  }

		// PEPeer stuff
	
	public PEPeerManager
	getManager()
	{
		return( manager.getDelegate());
	}
	
	public String
	getPeerSource()
	{
		return( PEPeerSource.PS_PLUGIN );
	}
	
	public int 
	getPeerState()
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
	
	public String 
	getIPHostName()
	{
		return( foreign.getIp());
	}
 
	public int 
	getPort()
	{
		return( foreign.getPort());
	}

	
  public int getTCPListenPort() {  return foreign.getTCPListenPort();  }
  public int getUDPListenPort() {  return foreign.getUDPListenPort();  }
  
  
  
  
  
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
	isChokingMe()
	{
		return( foreign.isChoked());
	}


	public boolean 
	isChokedByMe()
	{
		return( foreign.isChoking());
	}


	public boolean 
	isInterestingToMe()
	{
		return( foreign.isInterested());
	}


	public boolean 
	isInterestedInMe()
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
	getPercentDoneInThousandNotation()
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
  
  public void setOptimisticUnchoke( boolean is_optimistic ) {
    foreign.setOptimisticUnchoke( is_optimistic );
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
	

  public void addListener( PEPeerListener listener ) { /* nothing */ }

  public void removeListener( PEPeerListener listener ) { /* nothing */ }
  
  
  
  public NetworkConnection getConnection() {
    return ((ConnectionImpl)foreign.getConnection()).getCoreConnection();
  }
  
  
  public boolean supportsMessaging() {
    return foreign.supportsMessaging();
  }

  public Message[] getSupportedMessages() {
    org.gudy.azureus2.plugins.messaging.Message[] plug_msgs = foreign.getSupportedMessages();
    
    Message[] core_msgs = new Message[ plug_msgs.length ];
    
    for( int i=0; i < plug_msgs.length; i++ ) {
      core_msgs[i] = new MessageAdapter( plug_msgs[i] );
    }
    
    return core_msgs;
  }
    
  
  
  
	 /** To retreive arbitrary objects against a peer. */
	  public Object getData (String key) {
	  	if (data == null) return null;
	    return data.get(key);
	  }

	  /** To store arbitrary objects against a peer. */
	  public void setData (String key, Object value) {
	  	try{
	  		this_mon.enter();
	  	
	  		if (data == null) {
		  	  data = new HashMap();
		  	}
		    if (value == null) {
		      if (data.containsKey(key))
		        data.remove(key);
		    } else {
		      data.put(key, value);
		    }
	  	}finally{
	  		
	  		this_mon.exit();
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
  
  
  
  public int getReservedPieceNumber() {
    //TODO : Really implement it (Gudy)
    return -1;
 }
 
  public void setReservedPieceNumber(int pieceNumber) {
    //TODO : Really implement it (Gudy)
  }
	
}
