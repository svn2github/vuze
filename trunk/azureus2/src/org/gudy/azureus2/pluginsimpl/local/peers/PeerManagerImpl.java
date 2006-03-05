/*
 * File    : PeerManagerImpl.java
 * Created : 28-Dec-2003
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
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.plugins.disk.DiskManager;
import org.gudy.azureus2.plugins.download.*;
import org.gudy.azureus2.plugins.peers.*;
import org.gudy.azureus2.plugins.utils.PooledByteBuffer;
import org.gudy.azureus2.pluginsimpl.local.disk.DiskManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.utils.PooledByteBufferImpl;

public class 
PeerManagerImpl
	implements PeerManager
{
	protected PEPeerManager	manager;
	
	protected static AEMonitor	pm_map_mon	= new AEMonitor( "PeerManager:Map" );

	protected Map		foreign_map		= new HashMap();
	
	protected Map		listener_map 	= new HashMap();
	
	 protected AEMonitor	this_mon	= new AEMonitor( "PeerManager" );

	public static PeerManagerImpl
	getPeerManager(
		PEPeerManager	_manager )
	{
		try{
			pm_map_mon.enter();
			
			PeerManagerImpl	res = (PeerManagerImpl)_manager.getData( "PluginPeerManager" );
			
			if ( res == null ){
				
				res = new PeerManagerImpl( _manager );
				
				_manager.setData( "PluginPeerManager", res );
			}
			
			return( res );
		}finally{
			
			pm_map_mon.exit();
		}
	}
	
	protected
	PeerManagerImpl(
		PEPeerManager	_manager )
	{
		manager	= _manager;
	}

	public PEPeerManager
	getDelegate()
	{
		return( manager );
	}

	public DiskManager
	getDiskManager()
	{
		return( new DiskManagerImpl( this ));
	}
	
	public PeerManagerStats
	getStats()
	{
		return(new PeerManagerStatsImpl( manager));
	}
	
	public boolean
	isSeeding()
	{
		return( manager.getDiskManager().getRemaining() == 0 ); //yuck
	}
	
	public boolean
	isSuperSeeding()
	{
		return( manager.isSuperSeedMode());
	}
	
	public Download
	getDownload()
	
		throws DownloadException
	{
		return( DownloadManagerImpl.getDownloadStatic( manager.getDiskManager().getTorrent()));
	}
	
	
	public PeerStats
	createPeerStats(
		Peer	peer )
	{
		return( new PeerStatsImpl( this, peer, manager.createPeerStats()));
	}
	
	
	public void 
	requestComplete(
		PeerReadRequest		request,
		PooledByteBuffer 	data,
		Peer 				sender)
	{
		manager.writeBlock( 
			request.getPieceNumber(), 
			request.getOffset(), 
			((PooledByteBufferImpl)data).getBuffer(), 
			mapForeignPeer( sender ),
            false);
		
		PeerForeignDelegate	delegate = lookupForeignPeer( sender );
		
		if ( delegate != null ){
			
			delegate.dataReceived();
		}
	}
	
	public void
	requestCancelled(
		PeerReadRequest		request,
		Peer				sender )
	{
		manager.requestCanceled((DiskManagerReadRequest)request );
	}
	
	
	
		// these are foreign peers
	
	public void
	addPeer(
		Peer		peer )
	{
		manager.addPeer(mapForeignPeer( peer ));
	}
	
	public void
	removePeer(
		Peer		peer )
	{
		manager.removePeer(mapForeignPeer( peer ));
	}
  
	public void addPeer( 
		String 	ip_address, 
		int 	port ) 
	{
		manager.addPeer( ip_address, port, false );
	}
  
	
	public void addPeer( String ip_address, int port, boolean use_crypto ) {
		manager.addPeer( ip_address, port, use_crypto );
	}
  
	public Peer[]
	getPeers()
	{
		List	l = manager.getPeers();
		
		Peer[]	res= new Peer[l.size()];
		
			// this is all a bit shagged as we should maintain the PEPeer -> Peer link rather
			// than continually creating new PeerImpls...
		
		for (int i=0;i<res.length;i++){
			
			res[i] = new PeerImpl((PEPeer)l.get(i));
		}
		
		return( res );
	}
	
	public long
	getTimeSinceConnectionEstablished(
		Peer		peer )
	{
		if ( peer instanceof PeerImpl ){
			
			return(((PeerImpl)peer).getDelegate().getTimeSinceConnectionEstablished());
		}else{
			PeerForeignDelegate	delegate = lookupForeignPeer( peer );
			
			if ( delegate != null ){
				
				return( delegate.getTimeSinceConnectionEstablished());
				
			}else{
				
				return( 0 );
			}
		}
	}
	public PEPeer
	mapForeignPeer(
		Peer	_foreign )
	{
		PEPeer	local = (PEPeer)foreign_map.get( _foreign );
		
		if( local == null ){
			
			local 	= new PeerForeignDelegate( this, _foreign );
			
			_foreign.setUserData( PeerManagerImpl.class, local );
			
			foreign_map.put( _foreign, local );
		}
		
		return( local );
	}
	
	protected PeerForeignDelegate
	lookupForeignPeer(
		Peer	_foreign )
	{
		return((PeerForeignDelegate)_foreign.getUserData( PeerManagerImpl.class ));
	}
	
	public List
	mapForeignPeers(
		Peer[]	_foreigns )
	{
		List	res = new ArrayList();
		
		for (int i=0;i<_foreigns.length;i++){
		
			PEPeer	local = mapForeignPeer( _foreigns[i]);
			
				// could already be there if torrent contains two identical seeds (for whatever reason)
			
			if ( !res.contains( local )){
				
				res.add( local );
			}
		}
		
		return( res );
	}
	
	
	public void
	addListener(
		final PeerManagerListener	l )
	{
		try{
			this_mon.enter();
		
			final Map	peer_map = new HashMap();
			
      PEPeerManagerListener core_listener = new PEPeerManagerListener() {
        public void peerAdded( PEPeerManager manager, PEPeer peer ) {
          PeerImpl pi = new PeerImpl( peer );
          peer_map.put( peer, pi );
          l.peerAdded( PeerManagerImpl.this, pi );
        }

        public void peerRemoved( PEPeerManager manager, PEPeer peer ) {
          PeerImpl  pi = (PeerImpl)peer_map.remove( peer );
          
          if ( pi == null ){
            // somewhat inconsistently we get told here about the removal of
            // peers that never connected (and weren't added)
            // Debug.out( "PeerManager: peer not found");
          }
          else{         
            l.peerRemoved( PeerManagerImpl.this, pi );
          }
        }
      };
      
			listener_map.put( l, core_listener );
		
			manager.addListener( core_listener );
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	removeListener(
		PeerManagerListener	l )
	{
		try{
			this_mon.enter();
		
			PEPeerManagerListener core_listener	= (PEPeerManagerListener)listener_map.remove( l );
		
			if ( core_listener != null ){
				manager.removeListener( core_listener );
			}
      
		}finally{
			this_mon.exit();
		}
	}
}
