/*
 * File    : TrackerWebUtil.java
 * Created : 10-Dec-2003
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

package org.gudy.azureus2.ui.tracker;

/**
 * @author parg
 *
 */

import java.io.*;
import java.util.*;
import java.net.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.torrent.*;
import org.gudy.azureus2.plugins.tracker.*;
import org.gudy.azureus2.plugins.tracker.web.*;

import HTML.Template;

public abstract class 
TrackerWeb
	implements Plugin, TrackerWebPageGenerator
{
	protected static final String	NL			= "\r\n";
	
	protected static final String[]		welcome_pages = {"index.html", "index.htm", "index.tmpl" };
	protected static File[]				welcome_files;
	
	protected PluginInterface		plugin_interface;
	protected Tracker				tracker;
	
	protected String				file_root;
	
	public void 
	initialize(
		PluginInterface _plugin_interface )
	{	
		plugin_interface	= _plugin_interface;
		
		tracker = plugin_interface.getTracker();
		
		file_root = FileUtil.getApplicationPath() + "web";

		welcome_files = new File[welcome_pages.length];
		
		for (int i=0;i<welcome_pages.length;i++){
			
			welcome_files[i] = new File( file_root + File.separator + welcome_pages[i] );
		}
		
		tracker.addPageGenerator( this );
	}
	
	protected void
	handleTemplate(
		Hashtable		args,
		OutputStream	os )
	
		throws IOException
	{		
		Template t = new Template( args );

		// set up the paramters
		
		Vector torrent_info = new Vector();
		
		TrackerTorrent[]	tracker_torrents = tracker.getTorrents();
		
		for (int i=0;i<tracker_torrents.length;i++){
			
			Hashtable row = new Hashtable();
			
			torrent_info.add( row );
			
			TrackerTorrent	tracker_torrent = tracker_torrents[i];
			
			Torrent	torrent = tracker_torrent.getTorrent();
			
			String	hash_str = URLEncoder.encode( new String( torrent.getHash(), Constants.BYTE_ENCODING ), Constants.BYTE_ENCODING );
			
			String	torrent_name = new String(torrent.getName());
			
			TrackerPeer[]	peers = tracker_torrent.getPeers();
			
			int	seed_count 		= 0;
			int non_seed_count	= 0;
			
			for (int j=0;j<peers.length;j++){
				
				if ( peers[j].isSeed()){
					
					seed_count++;
					
				}else{
					
					non_seed_count++;
				}
			}
			
			int	status = tracker_torrent.getStatus();
			
			String	status_str;
			
			if ( status == TrackerTorrent.TS_STARTED ){

				status_str = "Running";
				
			}else if ( status == TrackerTorrent.TS_STOPPED ){
				
				status_str = "Stopped";
				
			}else if ( status == TrackerTorrent.TS_PUBLISHED ){
				
				status_str = "Published";
				
			}else{
				
				status_str = "Failed";
			}
			
			row.put( "name", torrent_name );
			
			if ( torrent.getSize() > 0 ){
				
				row.put( "download_url", "/torrents/" + torrent_name.replace('?','_') + ".torrent?" + hash_str );

			}
			
			row.put( "status", status_str );
			
			row.put( "size", (torrent.getSize()<=0?"N/A":DisplayFormatters.formatByteCountToKBEtc( torrent.getSize())));
			
			row.put( "seeds", "" + seed_count );
			
			row.put( "peers", "" + non_seed_count );

			row.put( "total_upload", DisplayFormatters.formatByteCountToKBEtc( tracker_torrent.getTotalUploaded())); 
			
			row.put( "total_download", DisplayFormatters.formatByteCountToKBEtc( tracker_torrent.getTotalDownloaded())); 
			
			row.put( "upload_speed", DisplayFormatters.formatByteCountToKBEtcPerSec( tracker_torrent.getAverageUploaded())); 
			
			row.put( "download_speed", DisplayFormatters.formatByteCountToKBEtcPerSec( tracker_torrent.getAverageDownloaded())); 
			
			row.put( "total_left", DisplayFormatters.formatByteCountToKBEtc( tracker_torrent.getTotalLeft())); 
			
			row.put( "completed",  "" + tracker_torrent.getCompletedCount()); 
		}
		
		t.setParam("torrent_info", torrent_info);

		String	data = t.output();
		
		os.write( data.getBytes());
	}
	
	protected boolean
	transferFile(
		String					file_type,
		InputStream				is,
		TrackerWebPageResponse	response )
	
		throws IOException
	{
		String	response_type = null;
		
		if (file_type.equals("html") || file_type.equals("htm")){
			response_type = "text/html";
		}else if (file_type.equals("jpg") || file_type.equals("jpeg")) {
			response_type="image/jpeg";
		}else if (file_type.equals("gif")) {
			response_type="image/gif";
		}else if (file_type.equals("tiff")) {
			response_type="image/tiff";
		}else if (file_type.equals("bmp")) {
			response_type="image/bmp";
		}else if (file_type.equals("png")) {
			response_type="image/png";
		}
								
		if ( response_type == null ){
				
			return( false );
		}
		
		OutputStream	os = response.getOutputStream();
		
		response.setContentType( response_type );
				
		byte[]	buffer = new byte[4096];
				
		while(true){
						
			int	len = is.read(buffer);
						
			if ( len <= 0 ){
									
				break;
			}
						
			os.write( buffer, 0, len );
		}

		return( true );				
	}
}