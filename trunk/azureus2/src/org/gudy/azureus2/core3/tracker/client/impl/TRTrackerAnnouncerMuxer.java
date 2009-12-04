/*
 * Created on Dec 4, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package org.gudy.azureus2.core3.tracker.client.impl;

import java.net.URL;
import java.util.Map;

import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentAnnounceURLSet;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncer;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerDataProvider;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerException;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerListener;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerResponse;
import org.gudy.azureus2.core3.tracker.client.impl.bt.TRTrackerBTAnnouncerImpl;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.plugins.download.DownloadAnnounceResult;

public class 
TRTrackerAnnouncerMuxer
	implements TRTrackerAnnouncer
{
	private TRTrackerAnnouncer	main_announcer;
	
	protected
	TRTrackerAnnouncerMuxer(
		TOTorrent		torrent,
		String[]		networks,
		boolean			manual )
	
		throws TRTrackerAnnouncerException
	{
		TOTorrentAnnounceURLSet[]	sets = torrent.getAnnounceURLGroup().getAnnounceURLSets();
		
		main_announcer = new TRTrackerBTAnnouncerImpl( torrent, sets, networks, manual );
	}
	
	public void
	setAnnounceDataProvider(
		TRTrackerAnnouncerDataProvider		provider )
	{
		main_announcer.setAnnounceDataProvider( provider );
	}
	
	public TOTorrent
	getTorrent()
	{
		return( main_announcer.getTorrent());
	}
	
	public URL
	getTrackerUrl()
	{
		return( main_announcer.getTrackerUrl());
	}
	
	public void
	setTrackerUrl(
		URL		url )
	{
		main_announcer.setTrackerUrl( url );
	}
		
	public void
	resetTrackerUrl(
		boolean	shuffle )
	{
		main_announcer.resetTrackerUrl( shuffle );
	}
	
	public void
	setIPOverride(
		String		override )
	{
		main_announcer.setIPOverride( override );
	}
	
	public void
	cloneFrom(
		TRTrackerAnnouncer	other )
	{
		main_announcer.cloneFrom( other );
	}
	
	public void
	clearIPOverride()
	{
		main_announcer.clearIPOverride();
	}
	
	public byte[]
	getPeerId()
	{
		return( main_announcer.getPeerId());
	}
	
	public void
	setRefreshDelayOverrides(
		int		percentage )
	{
		main_announcer.setRefreshDelayOverrides( percentage );
	}
	
	public int
	getTimeUntilNextUpdate()
	{
		return( main_announcer.getTimeUntilNextUpdate());
	}
	
	public int
	getLastUpdateTime()
	{
		return( main_announcer.getLastUpdateTime());
	}
			
	public void
	update(
		boolean	force )
	{
		main_announcer.update(force);
	}
	
	public void
	complete(
		boolean	already_reported )
	{
		main_announcer.complete(already_reported);
	}
	
	public void
	stop(
		boolean	for_queue )
	{
		main_announcer.stop( for_queue );
	}
	
	public void
	destroy()
	{
		main_announcer.destroy();
	}
	
	public int
	getStatus()
	{
		return( main_announcer.getStatus());
	}
	
	public boolean
	isManual()
	{
		return( main_announcer.isManual());
	}
	
	public String
	getStatusString()
	{
		return( main_announcer.getStatusString());
	}
	
	public TRTrackerAnnouncerResponse
	getLastResponse()
	{
		return( main_announcer.getLastResponse());
	}
	
	
	public Map
	getTrackerResponseCache()
	{
		return( main_announcer.getTrackerResponseCache());
	}
	
	public void
	setTrackerResponseCache(
		Map		map )
	{
		main_announcer.setTrackerResponseCache(map);
	}
	
	
	public void
	removeFromTrackerResponseCache(
		String		ip,
		int			tcp_port )
	{
		main_announcer.removeFromTrackerResponseCache(ip, tcp_port);
	}
	
	
	public void
	refreshListeners()
	{
		main_announcer.refreshListeners();	
	}
	
	public void
	setAnnounceResult(
		DownloadAnnounceResult	result )
	{
		main_announcer.setAnnounceResult(result);
	}
	
	public void
	addListener(
		TRTrackerAnnouncerListener	l )
	{
		main_announcer.addListener(l);
	}
		
	public void
	removeListener(
		TRTrackerAnnouncerListener	l )
	{
		main_announcer.removeListener(l);
	}
	
	public void 
	generateEvidence(
		IndentWriter writer )
	{
		main_announcer.generateEvidence(writer);
	}
}
