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

import java.util.List;

import org.gudy.azureus2.core3.peer.*;

import org.gudy.azureus2.plugins.peers.*;
import org.gudy.azureus2.plugins.disk.*;

public class 
PeerImpl 
	implements Peer
{
	protected PeerManager	manager;
	protected PEPeer		delegate;
	
	public
	PeerImpl(
		PEPeer		_delegate )
	{
		delegate	= _delegate;
		
		manager = PeerManagerImpl.getPeerManager( delegate.getManager());
		
	}

	public PeerManager
	getManager()
	{
		return( manager );
	}
	
	public int 
	getState()
	{
		int	state = delegate.getState();
		
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
	
	public boolean[] getAvailable()
	{
		return( delegate.getAvailable());
	}
   
	public boolean isChoked()
	{
		return( delegate.isChoked());
	}

	public boolean isChoking()
	{
		return( delegate.isChoking());
	}

	public boolean isInterested()
	{
		return( delegate.isInterested());
	}

	public boolean isInteresting()
	{
		return( delegate.isInteresting());
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
 	
	public int getMaxUpload()
	{
		return( delegate.getMaxUpload());
	}

	public boolean isIncoming()
	{
		return( delegate.isIncoming());
	}

	public int getDownloadPriority()
	{
		return( delegate.getDownloadPriority());
	}

	public int getPercentDone()
	{
		return( delegate.getPercentDone());
	}

	public String getClient()
	{
		return( delegate.getClient());
	}

	public boolean isOptimisticUnchoke()
	{
		return( delegate.isOptimisticUnchoke());
	}
	
	public int getNumberOfBadChunks()
	{
	  return( delegate.getNbBadChunks());
	}
	
	public void
	hasSentABadChunk()
	{
		delegate.hasSentABadChunk();
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
}
