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


import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerDataProvider;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerException;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerResponse;
import org.gudy.azureus2.core3.tracker.client.impl.TRTrackerAnnouncerImpl;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.clientid.ClientIDException;
import org.gudy.azureus2.plugins.download.DownloadAnnounceResult;
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
		return( 0 );	//TODO:
	}
	
	public int
	getLastUpdateTime()
	{
		return( 0 );	// TODO:
	}
			
	public void
	update(
		boolean	force )
	{
	}	
	
	public void
	complete(
		boolean	already_reported )
	{
	}
	
	public void
	stop()
	{
	}
	
	public void
	destroy()
	{
	}
	
	public int
	getStatus()
	{
		return( TS_INITIALISED );	// TODO:
	}
	
	public String
	getStatusString()
	{
		return( "OK" );	// TODO:
	}
	
	public TRTrackerAnnouncerResponse
	getLastResponse()
	{
		return( null );	// TODO:
	}
	
	public void
	refreshListeners()
	{	
	}
	
	public void
	setAnnounceResult(
		DownloadAnnounceResult	result )
	{
		// TODO:
	}
}
