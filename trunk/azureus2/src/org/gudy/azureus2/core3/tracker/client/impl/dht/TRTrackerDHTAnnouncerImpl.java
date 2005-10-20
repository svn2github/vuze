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

package org.gudy.azureus2.core3.tracker.client.impl.dht;

import java.net.URL;


import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerDataProvider;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerException;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerResponse;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerResponsePeer;
import org.gudy.azureus2.core3.tracker.client.impl.TRTrackerAnnouncerImpl;
import org.gudy.azureus2.core3.tracker.client.impl.TRTrackerAnnouncerResponseImpl;
import org.gudy.azureus2.core3.tracker.client.impl.TRTrackerAnnouncerResponsePeerImpl;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.clientid.ClientIDException;
import org.gudy.azureus2.plugins.download.DownloadAnnounceResult;
import org.gudy.azureus2.plugins.download.DownloadAnnounceResultPeer;
import org.gudy.azureus2.pluginsimpl.local.clientid.ClientIDManagerImpl;

/**
 * @author parg
 *
 */

public class 
TRTrackerDHTAnnouncerImpl
	extends TRTrackerAnnouncerImpl
{
	private TOTorrent		torrent;
	private String[]		networks;
	
	private byte[]			data_peer_id;
	
	private String						tracker_status_str;
	private long						last_update_time;
	
	private int							state = TS_INITIALISED;
	
	private TRTrackerAnnouncerResponseImpl	last_response;
		
	public
	TRTrackerDHTAnnouncerImpl(
		TOTorrent		_torrent,
		String[]		_networks )
	
		throws TRTrackerAnnouncerException
	{
		torrent		= _torrent;
		networks	= _networks;
		
		try{
			data_peer_id = ClientIDManagerImpl.getSingleton().generatePeerID( torrent, false );
			
		}catch( ClientIDException e ){

			 throw( new TRTrackerAnnouncerException( "TRTrackerAnnouncer: Peer ID generation fails", e ));
		}
		
		last_response = 
			new TRTrackerAnnouncerResponseImpl( 
				torrent.getAnnounceURL(),
				TRTrackerAnnouncerResponse.ST_OFFLINE, 0, "Initialising" );
		
		tracker_status_str = MessageText.getString("PeerManager.status.checking") + "...";
	}
	
	public void
	setAnnounceDataProvider(
		TRTrackerAnnouncerDataProvider		provider )
	{
		
	}
	
	public TOTorrent
	getTorrent()
	{
		return( torrent );
	}
	
	public URL
	getTrackerUrl()
	{
		return( torrent.getAnnounceURL());
	}
	
	public void
	setTrackerUrl(
		URL		url )
	{
		Debug.out( "setTrackerURL not supported for DHT" );
	}
		
	public void
	resetTrackerUrl(
		boolean	shuffle )
	{
	}
	
	public void
	setIPOverride(
		String		override )
	{
	}
	
	public void
	clearIPOverride()
	{
	}
		
	public byte[]
	getPeerId()
	{
		return( data_peer_id );
	}
	
	public void
	setRefreshDelayOverrides(
		int		percentage )
	{
	}
	
	public int
	getTimeUntilNextUpdate()
	{
		long elapsed = (SystemTime.getCurrentTime() - last_update_time)/1000;
		
		return( (int)(last_response.getTimeToWait()-elapsed));
	}
	
	public int
	getLastUpdateTime()
	{
		return( (int)(last_update_time/1000));
	}
			
	public void
	update(
		boolean	force )
	{
		state = TS_DOWNLOADING;
		
		checkCache();
	}	
	
	public void
	complete(
		boolean	already_reported )
	{
		state	= TS_COMPLETED;
	}
	
	public void
	stop()
	{
		state	= TS_STOPPED;
	}
	
	public void
	destroy()
	{
	}
	
	public int
	getStatus()
	{
		return( state );
	}
	
	public String
	getStatusString()
	{
		return( tracker_status_str );
	}
	
	public TRTrackerAnnouncerResponse
	getLastResponse()
	{
		return( last_response );
	}
	
	public void
	refreshListeners()
	{	
	}
	
	public void
	setAnnounceResult(
		DownloadAnnounceResult	result )
	{
		last_update_time	= SystemTime.getCurrentTime();
		
		TRTrackerAnnouncerResponseImpl response;
		
		if ( result.getResponseType() == DownloadAnnounceResult.RT_ERROR ){
			
			tracker_status_str = MessageText.getString("PeerManager.status.error"); 
		      
			String	reason = result.getError();
	
			if ( reason != null ){
		
				tracker_status_str += " (" + reason + ")";		
			}
			
	  		response = new TRTrackerAnnouncerResponseImpl(
				  				result.getURL(),
				  				TRTrackerAnnouncerResponse.ST_OFFLINE, 
								result.getTimeToWait(), 
								reason );
		}else{
			DownloadAnnounceResultPeer[]	ext_peers = result.getPeers();
			
			TRTrackerAnnouncerResponsePeer[] peers = new TRTrackerAnnouncerResponsePeer[ext_peers.length];
				
			for (int i=0;i<ext_peers.length;i++){
				
	    		LGLogger.log(componentID, evtFullTrace, LGLogger.INFORMATION, 
	    				"EXTERNAL PEER: ip=" +ext_peers[i].getAddress() + " port=" +ext_peers[i].getPort());

				peers[i] = new TRTrackerAnnouncerResponsePeerImpl( 
									ext_peers[i].getSource(),
									ext_peers[i].getPeerID(),
									ext_peers[i].getAddress(), 
									ext_peers[i].getPort());
			}
			
			addToTrackerCache( peers);
		
			tracker_status_str = MessageText.getString("PeerManager.status.ok");

			response = new TRTrackerAnnouncerResponseImpl( result.getURL(), TRTrackerAnnouncerResponse.ST_ONLINE, result.getTimeToWait(), peers );
		}
		
		last_response = response;
				
		listeners.dispatch( LDT_TRACKER_RESPONSE, response );
	}
	
	protected void
	checkCache()
	{
		if ( last_response.getStatus() != TRTrackerAnnouncerResponse.ST_ONLINE ){
			
		     TRTrackerAnnouncerResponsePeer[]	cached_peers = getPeersFromCache(100);

		     if ( cached_peers.length > 0 ){
		     	
		     	last_response.setPeers( cached_peers );
		     	
				listeners.dispatch( LDT_TRACKER_RESPONSE, last_response );
		     }
		}
	}
}
