/*
 * Created on 14-Feb-2005
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.core3.tracker.client.impl;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.peer.PEPeerSource;
import org.gudy.azureus2.core3.peer.util.PeerUtils;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncer;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerListener;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerResponse;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerResponsePeer;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.ListenerManager;
import org.gudy.azureus2.core3.util.ListenerManagerDispatcher;

/**
 * @author parg
 *
 */

public abstract class 
TRTrackerAnnouncerImpl
	implements TRTrackerAnnouncer
{
	public final static int componentID = 2;
	public final static int evtLifeCycle = 0;
	public final static int evtFullTrace = 1;
	public final static int evtErrors = 2;

	// 	listener
	
	protected static final int LDT_TRACKER_RESPONSE		= 1;
	protected static final int LDT_URL_CHANGED			= 2;
	protected static final int LDT_URL_REFRESH			= 3;
	
	protected ListenerManager	listeners 	= ListenerManager.createManager(
			"TrackerClient:ListenDispatcher",
			new ListenerManagerDispatcher()
			{
				public void
				dispatch(
					Object		_listener,
					int			type,
					Object		value )
				{
					TRTrackerAnnouncerListener	listener = (TRTrackerAnnouncerListener)_listener;
					
					if ( type == LDT_TRACKER_RESPONSE ){
						
						listener.receivedTrackerResponse((TRTrackerAnnouncerResponse)value);
						
					}else if ( type == LDT_URL_CHANGED ){
						
						Object[]	x = (Object[])value;
						
						String		url 	= (String)x[0];
						boolean	explicit	= ((Boolean)x[1]).booleanValue();
						
						listener.urlChanged(url, explicit );
						
					}else{
						
						listener.urlRefresh();
					}
				}
			});

	private Map	tracker_peer_cache		= new LinkedHashMap();	// insertion order - most recent at end
	private AEMonitor tracker_peer_cache_mon 	= new AEMonitor( "TRTrackerClientClassic:PC" );
	
 	
		// NOTE: tracker_cache is cleared out in DownloadManager when opening a torrent for the
		// first time as a DOS prevention measure

	public Map
	getTrackerResponseCache()
	{				
		return( exportTrackerCache());
	}
	
	
	public void
	setTrackerResponseCache(
		Map		map )
	{
		int	num = importTrackerCache( map );
		
		if( LGLogger.isEnabled() )  LGLogger.log(componentID, evtFullTrace, LGLogger.INFORMATION, 
	             "TRTrackerClient: imported " + num + " cached peers" );
	}

	protected Map
	exportTrackerCache()
	{
		Map	res = new HashMap();
		
		List	peers = new ArrayList();
		
		res.put( "tracker_peers", peers );
		
		try{
			tracker_peer_cache_mon.enter();
			
			Iterator it = tracker_peer_cache.values().iterator();
			
			while( it.hasNext()){
				
				TRTrackerAnnouncerResponsePeer	peer = (TRTrackerAnnouncerResponsePeer)it.next();		
	
				Map	entry = new HashMap();
				
				entry.put( "ip", peer.getAddress().getBytes());
				entry.put( "src", peer.getSource().getBytes());
				entry.put( "port", new Long(peer.getPort()));
				
				peers.add( entry );
			}
		
			if( LGLogger.isEnabled() )  LGLogger.log(componentID, evtFullTrace, LGLogger.INFORMATION, 
			             "TRTrackerClient: exported " + tracker_peer_cache.size() + " cached peers" );
		}finally{
			
			tracker_peer_cache_mon.exit();
		}
		
		return( res );
	}

 	protected byte[]
	getAnonymousPeerId(
		String	my_ip,
		int		my_port )
	{
  		byte[] anon_peer_id = new byte[20];
	
  		// unique initial two bytes to identify this as fake

  		anon_peer_id[0] = (byte)'[';
  		anon_peer_id[1] = (byte)']';

  		try{
	  		byte[]	ip_bytes 	= my_ip.getBytes( Constants.DEFAULT_ENCODING );
	  		int		ip_len		= ip_bytes.length;
	
	  		if ( ip_len > 18 ){
		
	  			ip_len = 18;
	  		}
	
	  		System.arraycopy( ip_bytes, 0, anon_peer_id, 2, ip_len );
									
	  		int	port_copy = my_port;
		
	  		for (int j=2+ip_len;j<20;j++){
			
	  			anon_peer_id[j] = (byte)(port_copy&0xff);
			
	  			port_copy >>= 8;
	  		}
  		}catch( UnsupportedEncodingException e ){
  			
  			Debug.printStackTrace( e );
  		}
  		
  		return( anon_peer_id );
   }
					
	protected int
	importTrackerCache(
		Map		map )
	{
		if ( !COConfigurationManager.getBooleanParameter("File.save.peers.enable")){
			
			return( 0 );
		}
		
		try{
			if ( map == null ){
				
				return( 0 );
			}
			
			List	peers = (List)map.get( "tracker_peers" );
	
			if ( peers == null ){
				
				return( 0 );
			}
			
			try{
				tracker_peer_cache_mon.enter();
				
				for (int i=0;i<peers.size();i++){
					
					Map	peer = (Map)peers.get(i);
					
					byte[]	src_bytes = (byte[])peer.get("src");
					String	peer_source = src_bytes==null?PEPeerSource.PS_BT_TRACKER:new String(src_bytes);
					String	peer_ip_address = new String((byte[])peer.get("ip"));
					int		peer_port		= ((Long)peer.get("port")).intValue();
					byte[]	peer_peer_id	= getAnonymousPeerId( peer_ip_address, peer_port );
						
					//System.out.println( "recovered " + ip_address + ":" + port );

          tracker_peer_cache.put( 
              peer_ip_address, 
              new TRTrackerAnnouncerResponsePeerImpl(peer_source, peer_peer_id, peer_ip_address, peer_port ));
					
				}
				
				return( tracker_peer_cache.size());
				
			}finally{
				
				tracker_peer_cache_mon.exit();
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
			
			return( tracker_peer_cache.size());
		}
	}

	protected void
	addToTrackerCache(
		TRTrackerAnnouncerResponsePeer[]		peers )
	{
		if ( !COConfigurationManager.getBooleanParameter("File.save.peers.enable")){
			
			return;
		}
		
		int	max = COConfigurationManager.getIntParameter( "File.save.peers.max", DEFAULT_PEERS_TO_CACHE );
		
		// System.out.println( "max peers= " + max );
		
		try{
			tracker_peer_cache_mon.enter();
			
			for (int i=0;i<peers.length;i++){
				
				TRTrackerAnnouncerResponsePeer	peer = peers[i];
				
					// remove and reinsert to maintain most recent last
				
				tracker_peer_cache.remove( peer.getAddress());
				
				tracker_peer_cache.put( peer.getAddress(), peer );
			}
			
			Iterator	it = tracker_peer_cache.keySet().iterator();
			
			if ( max > 0 ){
					
				while ( tracker_peer_cache.size() > max ){
						
					it.next();
					
					it.remove();
				}
			}
		}finally{
			
			tracker_peer_cache_mon.exit();
		}
	}

	public static Map
	mergeResponseCache(
		Map		map1,
		Map		map2 )
	{
		if ( map1 == null & map2 == null ){
			return( new HashMap());
		}else if ( map1 == null ){
			return( map2 );
		}else if ( map2 == null ){
			return( map1 );
		}
		
		Map	res = new HashMap();
				
		List	peers = (List)map1.get( "tracker_peers" );
		
		if ( peers == null ){
			
			peers = new ArrayList();
		}
		
		List	p2 = (List)map2.get( "tracker_peers" );
		
		if ( p2 != null ){
			
		  if( LGLogger.isEnabled() )  LGLogger.log(componentID, evtFullTrace, LGLogger.INFORMATION, 
			             "TRTrackerClient: merged peer sets: p1 = " + peers.size() + ", p2 = " + p2.size());
		
			for (int i=0;i<p2.size();i++){
				
				peers.add( p2.get( i ));
			}
		}
		
		res.put( "tracker_peers", peers );
		
		return( res );
	}
	
	protected TRTrackerAnnouncerResponsePeer[]
	getPeersFromCache(
		int	num_want )
	{
		try{
			tracker_peer_cache_mon.enter();
	
			if ( tracker_peer_cache.size() <= num_want ){
				
				TRTrackerAnnouncerResponsePeer[]	res = new TRTrackerAnnouncerResponsePeer[tracker_peer_cache.size()];
				
				tracker_peer_cache.values().toArray( res );
				
				if( LGLogger.isEnabled() )  LGLogger.log(componentID, evtFullTrace, LGLogger.INFORMATION, 
		                   "TRTrackerClient: returned " + res.length + " cached peers" );
			    
				return( res );
			}
			
			TRTrackerAnnouncerResponsePeer[]	res = new TRTrackerAnnouncerResponsePeer[num_want];
			
			Iterator	it = tracker_peer_cache.keySet().iterator();
			
				// take 'em out and put them back in so we cycle through the peers
				// over time
			
			for (int i=0;i<num_want;i++){
				
				String	key = (String)it.next();
				
				res[i] = (TRTrackerAnnouncerResponsePeer)tracker_peer_cache.get(key);
				
				it.remove();
			}
			
			for (int i=0;i<num_want;i++){
				
				tracker_peer_cache.put( res[i].getAddress(), res[i] );
			}
			
			if( LGLogger.isEnabled() )  LGLogger.log(componentID, evtFullTrace, LGLogger.INFORMATION, 
	                   "TRTrackerClient: returned " + res.length + " cached peers" );
		    
			return( res );
			
		}finally{
			
			tracker_peer_cache_mon.exit();
		}
	} 

 	public void
	addListener(
		TRTrackerAnnouncerListener	l )
	{
		listeners.addListener( l );
	}
		
	public void
	removeListener(
		TRTrackerAnnouncerListener	l )
	{
		listeners.removeListener(l);
	}
	
}
