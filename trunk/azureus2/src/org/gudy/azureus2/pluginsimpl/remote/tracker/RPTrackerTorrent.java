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
		Object[]	params	= request.getParams();			
		
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
		
		return( null );	}
	
	public int
	getStatus()
	{
		notSupported();
		
		return( 0 );	}
	
	public long
	getTotalUploaded()
	{
		notSupported();
		
		return( 0 );	
	}
	
	public long
	getTotalDownloaded()
	{
		notSupported();
		
		return( 0 );	
	}
	
	public long
	getAverageUploaded()
	{
		notSupported();
		
		return( 0 );
	}
	
	public long
	getAverageDownloaded()
	{
		notSupported();
		
		return( 0 );	
	}
	
	public long
	getTotalLeft()
	{
		notSupported();
		
		return( 0 );	
	}
	
	public long
	getCompletedCount()
	{
		notSupported();
		
		return( 0 );	
	}
	
	public Object
	getAdditionalProperty(
		String		name )
	{
		notSupported();
		
		return( null );
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
