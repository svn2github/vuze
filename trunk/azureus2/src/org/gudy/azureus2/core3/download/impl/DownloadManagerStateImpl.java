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
import java.io.*;

import org.gudy.azureus2.core3.download.*;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.tracker.client.TRTrackerClient;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.TorrentUtils;

/**
 * @author parg
 * Overall aim of this is to stop updating the torrent file itself and update something
 * Azureus owns. To this end a file based on torrent hash is created in user-dir/active
 * It is actually just a copy of the torrent file
 */

public class 
DownloadManagerStateImpl
	implements DownloadManagerState
{
	private static final String			RESUME_KEY			= "resume";
	private static final String			TRACKER_CACHE_KEY	= "tracker_cache";
	
	private static final File			ACTIVE_DIR;
	
	static{
	
		ACTIVE_DIR = FileUtil.getUserFile( "active" );
		
		if ( !ACTIVE_DIR.exists()){
			
			ACTIVE_DIR.mkdirs();
		}
	}
	
	private static AEMonitor	class_mon	= new AEMonitor( "DownloadManagerState:class" );
	
	private static Map					state_map = new HashMap();
	
	private DownloadManagerImpl			download_manager;
	
	private TOTorrent					torrent;
	
	private boolean						write_required;
	
	private Map							tracker_response_cache			= new HashMap();
  
	
	private AEMonitor	this_mon	= new AEMonitor( "DownloadManagerState" );


	private static DownloadManagerState
	getDownloadState(
		DownloadManagerImpl	download_manager,
		TOTorrent			original_torrent,
		TOTorrent			target_torrent )
	
		throws TOTorrentException
	{
		byte[]	hash	= target_torrent.getHash();
		
		DownloadManagerStateImpl	res	= null;
		
		try{
			class_mon.enter();
		
			HashWrapper	hash_wrapper = new HashWrapper( hash );
			
			res = (DownloadManagerStateImpl)state_map.get(hash_wrapper); 
			
			if ( res == null ){
			
				res = new DownloadManagerStateImpl( download_manager, target_torrent );
									
				state_map.put( hash_wrapper, res );
				
			}else{
				
					// if original state was created without a download manager, 
					// bind it to this one
				
				if ( res.getDownloadManager() == null && download_manager != null ){
					
					res.setDownloadManager( download_manager );
				}
				
				if ( original_torrent != null ){
						
					res.mergeTorrentDetails( original_torrent );
				}
			}
		}finally{
			
			class_mon.exit();
		}
				
		return( res );
	}

	
	public static DownloadManagerState
	getDownloadState(
		TOTorrent		original_torrent )
	
		throws TOTorrentException
	{
		byte[]	torrent_hash = original_torrent.getHash();
		
		// System.out.println( "getDownloadState: hash = " + ByteFormatter.encodeString(torrent_hash));
		
		TOTorrent saved_state	= null;
				
		File	saved_file = getStateFile( torrent_hash ); 
		
		if ( saved_file.exists()){
			
			try{
				saved_state = TorrentUtils.readFromFile( saved_file, true );
				
			}catch( Throwable e ){
				
				Debug.out( "Failed to load download state for " + saved_file );
			}
		}
		
			// if saved state not found then recreate from original torrent 
		
		if ( saved_state == null ){
		
			TorrentUtils.copyToFile( original_torrent, saved_file );
			
			saved_state = TorrentUtils.readFromFile( saved_file, true );
		}

		return( getDownloadState( null, original_torrent, saved_state ));
	}
	
	protected static DownloadManagerState
	getDownloadState(
		DownloadManagerImpl	download_manager,
		String				torrent_file,
		byte[]				torrent_hash )
	
		throws TOTorrentException
	{
		// System.out.println( "getDownloadState: hash = " + (torrent_hash==null?"null":ByteFormatter.encodeString(torrent_hash) + ", file = " + torrent_file ));

		TOTorrent	original_torrent	= null;
		TOTorrent 	saved_state			= null;
		
			// first, if we already have the hash then see if we can load the saved state
		
		if ( torrent_hash != null ){
			
			File	saved_file = getStateFile( torrent_hash ); 
		
			if ( saved_file.exists()){
				
				try{
					saved_state = TorrentUtils.readFromFile( saved_file, true );
					
				}catch( Throwable e ){
					
					Debug.out( "Failed to load download state for " + saved_file );
				}
			}
		}
		
			// if saved state not found then recreate from original torrent if required
		
		if ( saved_state == null ){
		
			original_torrent = TorrentUtils.readFromFile( new File(torrent_file), true );
			
			torrent_hash = original_torrent.getHash();
			
			File	saved_file = getStateFile( torrent_hash ); 
			
			if ( saved_file.exists()){
				
				try{
					saved_state = TorrentUtils.readFromFile( saved_file, true );
					
				}catch( Throwable e ){
					
					Debug.out( "Failed to load download state for " + saved_file );
				}
			}
			
			if ( saved_state == null ){
						
					// we must copy the torrent as we want one independent from the
					// original (someone might still have references to the original
					// and do stuff like write it somewhere else which would screw us
					// up)
				
				TorrentUtils.copyToFile( original_torrent, saved_file );
				
				saved_state = TorrentUtils.readFromFile( saved_file, true );
			}
		}

		return( getDownloadState( download_manager, original_torrent, saved_state ));
	}
	
	protected static File
	getStateFile(
		byte[]		torrent_hash )
	{
		return( new File( ACTIVE_DIR, ByteFormatter.encodeString( torrent_hash ) + ".dat" ));
	}
	
	protected
	DownloadManagerStateImpl(
		DownloadManagerImpl	_download_manager,
		TOTorrent			_torrent )
	{
		download_manager	= _download_manager;
		torrent				= _torrent;
		
		tracker_response_cache	= (Map)torrent.getAdditionalMapProperty( TRACKER_CACHE_KEY );
		
		if ( tracker_response_cache == null ){
			
			tracker_response_cache	= new HashMap();
		}
	}
	
	protected DownloadManagerImpl
	getDownloadManager()
	{
		return( download_manager );
	}
	
	protected void
	setDownloadManager(
		DownloadManagerImpl		dm )
	{
		download_manager	= dm;
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
	
	/*
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
	*/
	
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
	save()
	{
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
	  		
	  			TorrentUtils.writeToFile( torrent, true );
	  		
	  		}catch( Throwable e ){
	  		
	  			Debug.printStackTrace( e );
	  		}
	  	}else{
	  		
	  		// System.out.println( "not writing download state for '" + new String(torrent.getName()));
	  	}
	}
	
	public void
	delete()
	{
		try{
			class_mon.enter();

			state_map.remove( torrent.getHashWrapper());
			
	        TorrentUtils.delete( torrent );
	        
	    }catch( TOTorrentException e ){
	    	
	    	Debug.printStackTrace( e );
	   
		}finally{
			
			class_mon.exit();
		}
	}
	
	protected void
	mergeTorrentDetails(
		TOTorrent	other_torrent )
	{
		try{		
			boolean	write = TorrentUtils.mergeAnnounceURLs( other_torrent, torrent );
					
			// System.out.println( "DownloadManagerState:mergeTorrentDetails -> " + write );
			
			if ( write ){
				
				save();
				
				if ( download_manager != null ){
					
					TRTrackerClient	client = download_manager.getTrackerClient();

					if ( client != null ){
										
						// pick up any URL changes
					
						client.resetTrackerUrl( false );
					}
				}
			}
		}catch( Throwable e ){
				
			Debug.printStackTrace( e );
		}
	}
}
