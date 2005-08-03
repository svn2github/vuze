/*
 * File    : PluginPEPeerWrapper.java
 * Created : 01-Dec-2003
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

import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.util.AEMonitor;

import org.gudy.azureus2.plugins.messaging.Message;
import org.gudy.azureus2.plugins.network.Connection;
import org.gudy.azureus2.plugins.peers.*;
import org.gudy.azureus2.plugins.disk.*;
import org.gudy.azureus2.pluginsimpl.local.messaging.MessageAdapter;
import org.gudy.azureus2.pluginsimpl.local.network.ConnectionImpl;

public class 
PeerImpl 
	implements Peer
{
	protected PeerManager	manager;
	protected PEPeer		delegate;
	protected AEMonitor	this_mon	= new AEMonitor( "Peer" );
  
  private final Connection connection;
  
  private HashMap peer_listeners;
  

	public
	PeerImpl(
		PEPeer		_delegate )
	{
		delegate	= _delegate;
		
		manager = PeerManagerImpl.getPeerManager( delegate.getManager());
		
		connection = new ConnectionImpl( delegate.getConnection() );
	}

	public PeerManager
	getManager()
	{
		return( manager );
	}
	
  
  public Connection getConnection() {
    return connection;
  }
  
  
  public boolean supportsMessaging() {
    return delegate.supportsMessaging();
  }
  
  
  public Message[] getSupportedMessages() {
    com.aelitis.azureus.core.peermanager.messaging.Message[] core_msgs = delegate.getSupportedMessages();
    
    Message[] plug_msgs = new Message[ core_msgs.length ];
    
    for( int i=0; i < core_msgs.length; i++ ) {
      plug_msgs[i] = new MessageAdapter( core_msgs[i] );
    }
    
    return plug_msgs;
  }
  
  
  
	public int 
	getState()
	{
		int	state = delegate.getPeerState();
		
		switch( state ){
			
			case PEPeer.CONNECTING:
			{
				return( Peer.CONNECTING );
			}
			case PEPeer.DISCONNECTED:
			{
				return( Peer.DISCONNECTED );
			}
			case PEPeer.HANDSHAKING:
			{
				return( Peer.HANDSHAKING );
			}
			case PEPeer.TRANSFERING:
			{
				return( Peer.TRANSFERING );
			}
		}
		
		return( -1 );
	}

	public byte[] getId()
	{
		return( delegate.getId());
	}

	public String getIp()
	{
		return( delegate.getIp());
	}
 
	public int getPort()
	{
		return( delegate.getPort());
	}
  
  public int getTCPListenPort() {  return delegate.getTCPListenPort();  }
  public int getUDPListenPort() {  return delegate.getUDPListenPort();  }
  
	
	public boolean[] getAvailable()
	{
		return( delegate.getAvailable());
	}
   
	public boolean isChoked()
	{
		return( delegate.isChokingMe());
	}

	public boolean isChoking()
	{
		return( delegate.isChokedByMe());
	}

	public boolean isInterested()
	{
		return( delegate.isInterestingToMe());
	}

	public boolean isInteresting()
	{
		return( delegate.isInterestedInMe());
	}

	public boolean isSeed()
	{
		return( delegate.isSeed());
	}
 
	public boolean isSnubbed()
	{
		return( delegate.isSnubbed());
	}
 
	public void
	setSnubbed(
		boolean	b )
	{
		delegate.setSnubbed(b);
	}
	
	public PeerStats getStats()
	{
		return( new PeerStatsImpl(((PeerManagerImpl)manager).getDelegate(), delegate.getStats()));
	}
 	

	public boolean isIncoming()
	{
		return( delegate.isIncoming());
	}

	public int getPercentDone()
	{
		return( delegate.getPercentDoneInThousandNotation());
	}

	public int getPercentDoneInThousandNotation()
	{
		return( delegate.getPercentDoneInThousandNotation());
	}
	
	public String getClient()
	{
		return( delegate.getClient());
	}

	public boolean isOptimisticUnchoke()
	{
		return( delegate.isOptimisticUnchoke());
	}
	
  public void setOptimisticUnchoke( boolean is_optimistic ) {
    delegate.setOptimisticUnchoke( is_optimistic );
  }
	
	public void
	initialize()
	{
		throw( new RuntimeException( "not supported"));
	}
	
	public List
	getExpiredRequests()
	{
		throw( new RuntimeException( "not supported"));
	}	
  		
	public int
	getNumberOfRequests()
	{
		throw( new RuntimeException( "not supported"));
	}

	public void
	cancelRequest(
		DiskManagerRequest	request )
	{
		throw( new RuntimeException( "not supported"));
	}

 
	public boolean 
	addRequest(
		int pieceNumber, 
		int pieceOffset, 
		int pieceLength )
	{
		throw( new RuntimeException( "not supported"));
	}


	public void
	close(
		String 		reason,
		boolean 	closedOnError,
		boolean 	attemptReconnect )
	{
		throw( new RuntimeException( "not supported"));
	}
	


	public void 
	addListener( 
		final PeerListener	l ) 
	{
		PEPeerListener core_listener = 
			new PEPeerListener() 
			{
				public void 
				stateChanged( 
					int new_state ) 
				{
					l.stateChanged( new_state );
				}
      
				public void 
				sentBadChunk( 
					int piece_num, 
					int total_bad_chunks )
				{
					l.sentBadChunk( piece_num, total_bad_chunks );
				}
			};
    
		delegate.addListener( core_listener );
    
		if( peer_listeners == null ){
			
			peer_listeners = new HashMap();
		}
		
		peer_listeners.put( l, core_listener );
	}
	

	public void	
	removeListener( 
		PeerListener	l ) 
	{
		if ( peer_listeners != null ){
			
			PEPeerListener core_listener = (PEPeerListener)peer_listeners.remove( l );
    
			if( core_listener != null ) {
      
				delegate.removeListener( core_listener );
			}
		}
	}
	
		// as we don't maintain a 1-1 mapping between these and delegates make sure
		// that "equals" etc works sensibly
	
	public boolean
	equals(
		Object	other )
	{
		if ( other instanceof PeerImpl ){
			
			return( delegate == ((PeerImpl)other).delegate );
		}
		
		return( false );
	}
	
	public int
	hashCode()
	{
		return( delegate.hashCode());
	}
}
