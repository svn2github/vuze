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
 * @author MjrTom
 *			2005/Oct/08: Add _lastPiece
 *
 */

import java.util.*;

import org.gudy.azureus2.core3.disk.DiskManagerReadRequest;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.peer.impl.PEPeerControl;
import org.gudy.azureus2.core3.peer.impl.PEPeerTransport;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.network.Connection;
import org.gudy.azureus2.plugins.peers.Peer;
import org.gudy.azureus2.plugins.peers.PeerReadRequest;
import org.gudy.azureus2.pluginsimpl.local.messaging.MessageAdapter;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.peerdb.PeerItem;
import com.aelitis.azureus.core.peermanager.peerdb.PeerItemFactory;
import com.aelitis.azureus.core.peermanager.piecepicker.util.BitFlags;

public class 
PeerForeignDelegate
	implements 	PEPeerTransport
{
		// this implementation supports read-only peers (i.e. download only)

	protected volatile int		_lastPiece =-1;

	private PeerManagerImpl		manager;
	private Peer				foreign;
	
	private long	create_time		= SystemTime.getCurrentTime();
	private long	last_data_received_time =-1;
	private long	last_data_message_received_time =-1;
	private int		reserved_piece	= -1;
	
	private BitFlags	bit_flags;
	
	private Map			data;
	
	protected AEMonitor	this_mon	= new AEMonitor( "PeerForeignDelegate" );

	protected
	PeerForeignDelegate(
		PeerManagerImpl		_manager,
		Peer				_foreign )
	{
		manager		= _manager;
		foreign		= _foreign;
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

    
 
    public boolean
    transferAvailable() 
    {
    	return( foreign.isTransferAvailable());
    }
    
    public boolean isDownloadPossible()
    {
    	return foreign.isDownloadPossible();
    }
    
	public void
	sendCancel(
		DiskManagerReadRequest	request )
	{
		foreign.cancelRequest( request );
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
		return( foreign.addRequest( manager.getDelegate().getDiskManager().createReadRequest( pieceNumber, pieceOffset, pieceLength )));
	}
		
	protected  void
	dataReceived()
	{
		last_data_received_time	= SystemTime.getCurrentTime();
	}
  
	public void 
	closeConnection( 
		String reason ) 
	{
		foreign.close( reason, false, false ); 
	}
		
	public List
	getExpiredRequests()
	{
		return( foreign.getExpiredRequests());
	}
  		
	public int
	getNbRequests()
	{
		return( foreign.getNumberOfRequests());
	}
		
	public PEPeerControl
	getControl()
	{
		return((PEPeerControl)manager.getDelegate());
	}
  
	
	public void 
	updatePeerExchange()
	{
	}
  
  
	public PeerItem 
	getPeerItemIdentity() 
	{
		return PeerItemFactory.createPeerItem( foreign.getIp(), foreign.getPort(), PeerItemFactory.PEER_SOURCE_PLUGIN );
	}
  
  
	public int 
	getConnectionState() 
	{
		int	peer_state = getPeerState();
	  
		if ( peer_state == Peer.CONNECTING ){
		  
			return( CONNECTION_CONNECTING );
		  
		}else if ( peer_state == Peer.HANDSHAKING ){
		  
			return( CONNECTION_WAITING_FOR_HANDSHAKE );
			  
		}else if ( peer_state == Peer.TRANSFERING ){
		  
			return( CONNECTION_FULLY_ESTABLISHED );
		  
		}else{
		
			return( CONNECTION_FULLY_ESTABLISHED );
		}
	}  
  
  	public void 
  	doKeepAliveCheck() 
  	{
  	}
  
  	public boolean 
  	doTimeoutChecks() 
  	{
  		return false;
  	}
  
  	public void 
  	doPerformanceTuningCheck() 
  	{
  	}
  
  	public long 
  	getTimeSinceConnectionEstablished() 
  	{
  		long	now = SystemTime.getCurrentTime();
  		
  		if ( now > create_time ){
  			
  			return( now - create_time );
  		}
  		
  		return( 0 );
  	}
  
    public long getTimeSinceLastDataMessageReceived() {
        if (last_data_message_received_time ==-1)
          return -1;	//never received
        
        long now =SystemTime.getCurrentTime();
        long time_since =now -last_data_message_received_time;
        
        if (time_since <0)
        {	//time went backwards
        	last_data_message_received_time =now;
        	time_since =0;
        }
        return time_since;    
      }
  
	public long getTimeSinceGoodDataReceived()
	{
		if (last_data_received_time ==-1)
			return -1;	// never received
		long now =SystemTime.getCurrentTime();
		long time_since =now -last_data_received_time;

		if (time_since <0)
		{	// time went backwards
			last_data_received_time =now;
			time_since =0;
		}

		return time_since;
	}
  
  	public long 
  	getTimeSinceLastDataMessageSent() 
  	{
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
		int	peer_state = foreign.getState();
		
		return( peer_state );
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
  
  
  
  
  
	public BitFlags 
	getAvailable()
	{
		boolean[]	flags = foreign.getAvailable();
		
		if ( bit_flags == null || bit_flags.flags != flags ){
			
			bit_flags = new BitFlags( flags );
		}
		
		return( bit_flags );
	}

	public boolean isPieceAvailable(int pieceNumber)
	{
		return foreign.isPieceAvailable(pieceNumber);
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
	isInteresting()
	{
		return( foreign.isInteresting());
	}


	public boolean 
	isInterested()
	{
		return( foreign.isInterested());
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
	
	public long getSnubbedTime()
	{
		return foreign.getSnubbedTime();
	}

 
	public boolean isLANLocal() {
		if( AddressUtils.isLANLocalAddress( foreign.getIp() ) )  return true;
		return false;
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
		return foreign.getPercentDoneInThousandNotation();
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
  
	public Connection
	getConnection()
	{
		return( foreign.getConnection());
	}
  
	public int
	getPercentDoneOfCurrentIncomingRequest()
	{
		return( foreign.getPercentDoneOfCurrentIncomingRequest());
	}
	  
	public int
	getPercentDoneOfCurrentOutgoingRequest()
	{
		return( foreign.getPercentDoneOfCurrentOutgoingRequest());	
	}
  
	public boolean 
	supportsMessaging() 
	{
		return foreign.supportsMessaging();
	}

	public String
	getEncryption()
	{
		return( "" );
	}
	
	public Message[] 
	getSupportedMessages() 
	{
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
  
  
  
	public int 
	getReservedPieceNumber() 
	{
		return( reserved_piece );
	}
 
  	public void 
  	setReservedPieceNumber(int pieceNumber) 
  	{
  		reserved_piece	= pieceNumber;
  	}

	public int[] 
	getIncomingRequestedPieceNumbers() 
	{
		return( new int[0] );
	}

	public int[] 
	getOutgoingRequestedPieceNumbers() 
	{
		List	l = foreign.getRequests();
		
		int[]	res = new int[l.size()];
		
		for (int i=0;i<l.size();i++){
			
			res[i] = ((PeerReadRequest)l.get(i)).getPieceNumber();
		}
		
		return( res );
	}

	public int getLastPiece()
	{
		return _lastPiece;
	}

	public void setLastPiece(int pieceNumber)
	{
		_lastPiece =pieceNumber;
	}

    /**
     * Nothing to do if called
     */
    public void checkInterested() {}
    
    /**
     * Apaprently nothing significant to do if called
     */
	public boolean isAvailabilityAdded() {return false;}

    /**
     * Nothing to do if called
     */
	public void clearAvailabilityAdded() {};
}
