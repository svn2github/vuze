/*
 * Created on 11-Dec-2005
 * Created by Paul Gardner
 * Copyright (C) 2005, 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.core3.peer;

import org.gudy.azureus2.core3.logging.LogRelation;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;

import com.aelitis.azureus.core.peermanager.PeerManagerRegistration;

public interface 
PEPeerManagerAdapter 
{
	public String
	getDisplayName();
	
	public int
	getUploadRateLimitBytesPerSecond();
	
	public int
	getDownloadRateLimitBytesPerSecond();
	
	public int
	getMaxUploads();
	
	public int
	getMaxConnections();
	
	public boolean
	isAZMessagingEnabled();
	
	public boolean
	isPeerExchangeEnabled();
	
	public boolean
	isPeriodicRescanEnabled();
	
	public void
	setStateFinishing();
	
	public void
	setStateSeeding(
		boolean	never_downloaded );
	
	public void
	restartDownload();
	
	public TRTrackerScraperResponse
	getTrackerScrapeResponse();
	
	public String
	getTrackerClientExtensions();
	
	public void
	setTrackerRefreshDelayOverrides(
		int	percent );
	
	public boolean
	isNATHealthy();
	
	public void
	addPeer(
		PEPeer	peer );
	
	public void
	removePeer(
		PEPeer	peer );
	
	public void
	addPiece(
		PEPiece	piece );
	
	public void
	removePiece(
		PEPiece	piece );
	
	public void
	discarded(
		int	bytes );
	
	public void
	protocolBytesReceived(
		int	bytes );
	
	public void
	dataBytesReceived(
		int	bytes );
	
	public void
	protocolBytesSent(
		int	bytes, boolean LAN );
	
	public void
	dataBytesSent(
		int	bytes, boolean LAN );
	
	public PeerManagerRegistration
	getPeerManagerRegistration();
	
	public LogRelation
	getLogRelation();
}
