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

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.download.*;

import org.gudy.azureus2.plugins.peers.*;
import org.gudy.azureus2.plugins.download.*;
import org.gudy.azureus2.plugins.disk.*;
import org.gudy.azureus2.pluginsimpl.local.disk.*;
import org.gudy.azureus2.pluginsimpl.local.download.*;


import org.gudy.azureus2.core3.peer.*;

public class 
PeerManagerImpl
	implements PeerManager
{
	protected PEPeerManager	manager;
	
	protected static Map	pm_map	= new WeakHashMap();
	
	protected Map		foreign_map	= new WeakHashMap();
	
	protected Map		listener_map = new HashMap();
	
	public static PeerManagerImpl
	getPeerManager(
		PEPeerManager	_manager )
	{
		synchronized( pm_map ){
			
			PeerManagerImpl	res = (PeerManagerImpl)pm_map.get( _manager );
			
			if ( res == null ){
				
				res = new PeerManagerImpl( _manager );
				
				pm_map.put( _manager, res );
			}
			
			return( res );
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
		return( manager.getState() == PEPeerManager.PS_SEEDING );
	}
	
	public Download
	getDownload()
	
		throws DownloadException
	{
		return( DownloadManagerImpl.getDownloadStatic( manager.getDownloadManager()));
	}
	
	
	public PeerStats
	createPeerStats()
	{
		return( new PeerStatsImpl( manager, manager.createPeerStats()));
	}
	
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
	
	protected  void
	peerAdded(
		Peer		peer )
	{
		manager.peerAdded( mapForeignPeer( peer ));
	}
	
	protected void
	peerRemoved(
		Peer		peer )
	{
		manager.peerRemoved( mapForeignPeer( peer ));
	}
		
	public PEPeer
	mapForeignPeer(
		Peer	_foreign )
	{
		PEPeer	local = (PEPeer)foreign_map.get( _foreign );
		
		if( local == null ){
			
			local 	= new PeerForeignDelegate( this, _foreign );
			
			foreign_map.put( _foreign, local );
		}
		
		return( local );
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
	
	
	public synchronized void
	addListener(
		final PeerManagerListener	l )
	{
		final Map	peer_map = new HashMap();
		
		DownloadManagerPeerListener	pml = 
			new DownloadManagerPeerListener()
			{
				public void
				peerManagerAdded(
					PEPeerManager	manager )
				{
				}
				
				public void
				peerManagerRemoved(
					PEPeerManager	manager )
				{
				}
								
				public void
				peerAdded(
					PEPeer 	peer )
				{
					PeerImpl pi = new PeerImpl( peer );
										
					peer_map.put( peer, pi );
					
					l.peerAdded( PeerManagerImpl.this, pi );
				}
					
				public void
				peerRemoved(
					PEPeer	peer )
				{	
					PeerImpl	pi = (PeerImpl)peer_map.remove( peer );
					
					if ( pi == null ){
						
						// somewhat inconsistently we get told here about the removal of
						// peers that never connected (and weren't added)
						// Debug.out( "PeerManager: peer not found");
						
					}else{
												
						l.peerRemoved( PeerManagerImpl.this, pi );
					}
				}
					
				public void
				pieceAdded(
					PEPiece 	piece )
				{	
				}
					
				public void
				pieceRemoved(
					PEPiece		piece )
				{
				}
			};

		listener_map.put( l, pml );
	
		manager.getDownloadManager().addPeerListener(pml);
	}
	
	public synchronized void
	removeListener(
		PeerManagerListener	l )
	{
		DownloadManagerPeerListener	pml = (DownloadManagerPeerListener)listener_map.get( l );
		
		if ( pml != null ){
		
			manager.getDownloadManager().removePeerListener( pml );
			
			listener_map.remove( l );
		}
	}
}
