/*
 * Created on 21-Jun-2004
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

package org.gudy.azureus2.pluginsimpl.remote.tracker;

import org.gudy.azureus2.plugins.tracker.*;
import org.gudy.azureus2.plugins.torrent.*;
import org.gudy.azureus2.pluginsimpl.remote.*;


/**
 * @author parg
 *
 */

public class 
RPTrackerTorrent
	extends		RPObject
	implements 	TrackerTorrent
{
	protected transient TrackerTorrent		delegate;
	
		// don't change the names of these, they appear in XML serialisation
	
	public int		status;
	public long		total_uploaded;
	public long		total_downloaded;
	public long		average_uploaded;
	public long		average_downloaded;
	public long		total_left;
	public long 	completed_count;
	public long		total_bytes_in;
	public long		average_bytes_in;
	public long		total_bytes_out;
	public long 	average_bytes_out;
	public long		scrape_count;
	public long		announce_count;
	public int		seed_count;
	public int		leecher_count;
	public int		bad_NAT_count;
	
	
	public static RPTrackerTorrent
	create(
		TrackerTorrent		_delegate )
	{
		RPTrackerTorrent	res =(RPTrackerTorrent)_lookupLocal( _delegate );
		
		if ( res == null ){
			
			res = new RPTrackerTorrent( _delegate );
		}
		
		return( res );
	}
	
	protected
	RPTrackerTorrent(
		TrackerTorrent		_delegate )
	{
		super( _delegate );
	}
	
	protected void
	_setDelegate(
		Object		_delegate )
	{
		delegate = (TrackerTorrent)_delegate;
		
		status				= delegate.getStatus();
		total_uploaded		= delegate.getTotalUploaded();
		total_downloaded	= delegate.getTotalDownloaded();
		average_uploaded	= delegate.getAverageUploaded();
		average_downloaded	= delegate.getAverageDownloaded();
		total_left			= delegate.getTotalLeft();
		completed_count		= delegate.getCompletedCount();
		total_bytes_in		= delegate.getTotalBytesIn();
		average_bytes_in	= delegate.getAverageBytesIn();
		total_bytes_out		= delegate.getTotalBytesOut();
		average_bytes_out	= delegate.getAverageBytesOut();
		scrape_count		= delegate.getScrapeCount();
		announce_count		= delegate.getAnnounceCount();
		seed_count			= delegate.getSeedCount();
		leecher_count		= delegate.getLeecherCount();
		bad_NAT_count		= delegate.getBadNATCount();
	}
	
	public Object
	_setLocal()
	
		throws RPException
	{
		return( _fixupLocal());
	}
	
	
	public RPReply
	_process(
		RPRequest	request	)
	{
		String		method 	= request.getMethod();
		// Object[]	params	= request.getParams();			
		
		throw( new RPException( "Unknown method: " + method ));
	}

		//***************************************************************************8
	
	public void
	remove()
	
		throws TrackerTorrentRemovalVetoException
	{
		notSupported();
		
	}
	
	public boolean
	canBeRemoved()
	
		throws TrackerTorrentRemovalVetoException
	{
		notSupported();
		
		return( false );	
	}
	
	
	public Torrent
	getTorrent()
	{
		notSupported();
		
		return( null );	}
	
	public TrackerPeer[]
	getPeers()
	{
		notSupported();
		
		return( null );	
	}
	
	public int
	getStatus()
	{
		notSupported();
		
		return( status );	
	}
	
	public long
	getTotalUploaded()
	{
		notSupported();
		
		return( total_uploaded );	
	}
	
	public long
	getTotalDownloaded()
	{
		notSupported();
		
		return( total_downloaded );	
	}
	
	public long
	getAverageUploaded()
	{
		notSupported();
		
		return( average_uploaded );
	}
	
	public long
	getAverageDownloaded()
	{
		notSupported();
		
		return( average_downloaded );	
	}
	
	public long
	getTotalLeft()
	{
		notSupported();
		
		return( total_left );	
	}
	
	public long
	getCompletedCount()
	{
		notSupported();
		
		return( completed_count );	
	}

	public long
	getTotalBytesIn()
	{
		notSupported();
		
		return( total_bytes_in );	
	}	
	
	public long
	getAverageBytesIn()
	{
		notSupported();
		
		return( average_bytes_in );	
	}
	
	public long
	getTotalBytesOut()
	{
		notSupported();
		
		return( total_bytes_out );	
	}
	
	public long
	getAverageBytesOut()
	{
		notSupported();
		
		return( average_bytes_out );	
	}	

	public long
	getScrapeCount()
	{
		notSupported();
		
		return( scrape_count );	
	}
	
	public long
	getAnnounceCount()
	{
		notSupported();
		
		return( announce_count );	
	}
	
	public int
	getSeedCount()
	{
		notSupported();
		
		return( seed_count );	
	}	
	
	public int
	getLeecherCount()
	{
		notSupported();
		
		return( leecher_count);	
	}
	
	public int
	getBadNATCount()
	{
		notSupported();
		
		return( bad_NAT_count );
	}
	
	public void
	disableReplyCaching()
	{
		notSupported();
	}
	
	public void
	addListener(
		TrackerTorrentListener	listener )
	{
		
	}
	
	public void
	removeListener(
		TrackerTorrentListener	listener )
	{
		
	}
	
	public void
	addRemovalListener(
		TrackerTorrentWillBeRemovedListener	listener )
	{
	}
	
	
	public void
	removeRemovalListener(
		TrackerTorrentWillBeRemovedListener	listener )
	{
		
	}
}
