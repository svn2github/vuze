/*
 * Created on 15-Nov-2004
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

package org.gudy.azureus2.core3.download.impl;

import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.tracker.util.TRTrackerUtils;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.TorrentUtils;

/**
 * @author parg
 *
 */

public class 
DownloadManagerStateImpl
	implements DownloadManagerState
{
	private static final String			RESUME_KEY			= "resume";
	private static final String			TRACKER_CACHE_KEY	= "tracker_cache";
	
	private TOTorrent					torrent;
	
	private boolean						write_required;
	
	private Map							tracker_response_cache			= new HashMap();
  
	
	private AEMonitor	this_mon	= new AEMonitor( "DownloadManagerState" );

	public static DownloadManagerState
	getDownloadState(
		TOTorrent		torrent )
	{
		DownloadManagerStateImpl	res = new DownloadManagerStateImpl();
		
		res.setTorrent( torrent );
		
		return( res );
	}

	protected void
	setTorrent(
		TOTorrent		_torrent )
	{
		torrent	= _torrent;
		
		tracker_response_cache	= (Map)torrent.getAdditionalMapProperty( TRACKER_CACHE_KEY );
		
		if ( tracker_response_cache == null ){
			
			tracker_response_cache	= new HashMap();
		}
	}
	
	protected void
	clearTrackerResponseCache()
	{
		setTrackerResponseCache( new HashMap());
	}
	
	protected Map
	getTrackerResponseCache()
	{
		return( tracker_response_cache );
	}
	
	protected void
	setTrackerResponseCache(
		Map		value )
	{
		try{
			this_mon.enter();
		
			// System.out.println( "setting download state/tracker cache for '" + new String(torrent.getName()));

			boolean	changed = !BEncoder.mapsAreIdentical( value, tracker_response_cache );
		
			if ( changed ){
				
				write_required	= true;
				
				tracker_response_cache	 = value;
		
				torrent.setAdditionalMapProperty( TRACKER_CACHE_KEY, tracker_response_cache );
			}	
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected boolean
	mergeTrackerResponseCache(
		DownloadManagerStateImpl	other_state )
	{
		Map  other_cache	= other_state.getTrackerResponseCache();

		if ( other_cache != null && other_cache.size() > 0 ){
						
			Map merged_cache = TRTrackerUtils.mergeResponseCache( tracker_response_cache, other_cache );
		
			setTrackerResponseCache( merged_cache );
	  		
	  		return( true );
		}
		
		return( false );
	}
	
	public Map
	getResumeData()
	{
		return( torrent.getAdditionalMapProperty(RESUME_KEY));
	}
	
	protected void
	clearResumeData()
	{
		setResumeData( null );
	}
	
	public void
	setResumeData(
		Map	data )
	{
		try{
			this_mon.enter();
		
			// System.out.println( "setting download state/resume data for '" + new String(torrent.getName()));

			if ( data == null ){
				
				torrent.removeAdditionalProperty( RESUME_KEY );
				
			}else{
				
				torrent.setAdditionalMapProperty(RESUME_KEY, data);
			}
			
			write_required	= true;
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public TOTorrent
	getTorrent()
	{
		return( torrent );
	}
	
	public void
	saveNonTorrentData()
	{
		// TODO: when we migrate to separate file for resume/cache data then this method
		// must write that file. can't write the torrent here at the moment as the torrent
		// may not have its "filename" attribute set up...
	}
	
	public void
	save()
	{
	    if ( torrent == null ){
	    
	    	return;
	    }
	    
	  	if ( COConfigurationManager.getBooleanParameter("File.save.peers.enable", true )){
	  		
	  		boolean	do_write;
	  		
	  		try{
	  			this_mon.enter();
	  		
	  			do_write	= write_required;
	  			
	  			write_required	= false;
	  			
	  		}finally{
	  			
	  			this_mon.exit();
	  		}
	  		
		  	if ( do_write ){
		  				  	
		  		try{
		  			// System.out.println( "writing download state for '" + new String(torrent.getName()));
		  		
		  			TorrentUtils.writeToFile( torrent );
		  		
		  		}catch( Throwable e ){
		  		
		  			Debug.printStackTrace( e );
		  		}
		  	}else{
		  		
		  		// System.out.println( "not writing download state for '" + new String(torrent.getName()));
		  	}
	  	}
	}
}
