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
	
	protected Hashtable
	decodeParams(
		String	str )
	{
		Hashtable	params = new Hashtable();
		
		int	pos = 0;
		
		while(true){
		
			int	p1 = str.indexOf( '&', pos );
			
			String	bit;
			
			if ( p1 == -1 ){
				
				bit = str.substring(pos);
				
			}else{
				
				bit = str.substring(pos,p1);
				
				pos = p1+1;
			}
			
			int	p2 = bit.indexOf('=');
			
			if ( p2 == -1 ){
			
				params.put(bit,"true");
				
			}else{
				
				params.put(bit.substring(0,p2), bit.substring(p2+1));
			}
			
			if ( p1 == -1 ){
				
				break;
			}
		}
		
		return( params );
	}
	
	protected void
	handleTemplate(
		Hashtable		params,
		Hashtable		args,
		OutputStream	os )
	
		throws IOException
	{	
		/*
		__FIRST__
		True for the first run of the loop, false otherwise 
		__LAST__ 
		True for the last run of the loop, false otherwise 
		__ODD__ 
		True for every other iteration of the loop - a loop starts at 1 
		__INNER__
		True if both __FIRST__ and __LAST__ are false 
		__COUNTER__
		Which iteration is currently on. Starts at 1.(new in 0.1.1) 
		*/
		
		args.put( "loop_context_vars", "true" );
		args.put( "global_vars", "true" );
		
		Template t = new Template( args );

		int	specific_torrent	= -1;
		
		if ( params != null ){
			
			String	specific_torrents = (String)params.get( "torrent_info" );
			
			
			if ( specific_torrents != null ){
				
				specific_torrent = Integer.parseInt( specific_torrents );
				
					// 1 based -> 0 based
				
				specific_torrent--;
			}
		}
		
			// set up the parameters
		
		Vector torrent_info = new Vector();
		
		TrackerTorrent[]	tracker_torrents = tracker.getTorrents();
		
		int	start;
		int	end;
		
		if ( specific_torrent != -1 ){
			
			start 	= specific_torrent;
			
			end		= specific_torrent+1;
		}else{
			
			start 	= 0;
			end		= tracker_torrents.length;
		}
		
		boolean	allow_details = plugin_interface.getPluginconfig().getBooleanParameter("Tracker Publish Enable Details", true );
		
		t.setParam( "torrent_details_allowed", allow_details?"1":"0");
		
		for (int i=start;i<end;i++){
			
			Hashtable t_row = new Hashtable();
			
			torrent_info.add( t_row );
			
			TrackerTorrent	tracker_torrent = tracker_torrents[i];
			
			Torrent	torrent = tracker_torrent.getTorrent();
			
			String	hash_str = URLEncoder.encode( new String( torrent.getHash(), Constants.BYTE_ENCODING ), Constants.BYTE_ENCODING );
			
			String	torrent_name = torrent.getName();
			
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
			
			t_row.put( "torrent_name", torrent_name );
			
			if ( torrent.getSize() > 0 ){
				
				t_row.put( "torrent_download_url", "/torrents/" + torrent_name.replace('?','_') + ".torrent?" + hash_str );

			}else{
				
				t_row.put( "torrent_download_url", "" );
			}
			
			t_row.put( "torrent_status", status_str );
			
			t_row.put( "torrent_size", (torrent.getSize()<=0?"N/A":DisplayFormatters.formatByteCountToKBEtc( torrent.getSize())));
			
			t_row.put( "torrent_seeds", "" + seed_count );
			
			t_row.put( "torrent_peers", "" + non_seed_count );

			t_row.put( "torrent_total_upload", DisplayFormatters.formatByteCountToKBEtc( tracker_torrent.getTotalUploaded())); 
			
			t_row.put( "torrent_total_download", DisplayFormatters.formatByteCountToKBEtc( tracker_torrent.getTotalDownloaded())); 
			
			t_row.put( "torrent_upload_speed", DisplayFormatters.formatByteCountToKBEtcPerSec( tracker_torrent.getAverageUploaded())); 
			
			t_row.put( "torrent_download_speed", DisplayFormatters.formatByteCountToKBEtcPerSec( tracker_torrent.getAverageDownloaded())); 
			
			t_row.put( "torrent_total_left", DisplayFormatters.formatByteCountToKBEtc( tracker_torrent.getTotalLeft())); 
			
			t_row.put( "torrent_completed",  "" + tracker_torrent.getCompletedCount());
		
			if ( allow_details ){
				
				if ( specific_torrent != -1 ){
				
					t_row.put( "torrent_hash", ByteFormatter.nicePrint(torrent.getHash(),true));
								
						// size is 0 for external torrents about which we know no more
					
					if ( torrent.getSize() > 0 ){
				
						t_row.put( "torrent_comment", torrent.getComment());
						
						t_row.put( "torrent_created_by", torrent.getCreatedBy());
						
						t_row.put( "torrent_piece_size", DisplayFormatters.formatByteCountToKBEtc(torrent.getPieceSize()));
						
						t_row.put( "torrent_piece_count", ""+torrent.getPieceCount());
						
						Vector	file_info = new Vector();
						
						TorrentFile[]	files = torrent.getFiles();
						
						for (int j=0;j<files.length;j++){
							
							Hashtable	f_row = new Hashtable();
							
							file_info.add( f_row );
							
							f_row.put( "file_name", files[j].getName());
							
							f_row.put( "file_size", DisplayFormatters.formatByteCountToKBEtc(files[j].getSize()));
						}
						
						t_row.put( "file_info", file_info );
						t_row.put( "file_info_count", ""+files.length );
					}
					
					Vector	peer_info = new Vector();
					
						// peers details for published torrents are dummy and not useful
					
					if ( status != TrackerTorrent.TS_PUBLISHED ){
						
						for (int j=0;j<peers.length;j++){
							
							Hashtable p_row = new Hashtable();
							
							peer_info.add( p_row );
							
							TrackerPeer	peer = peers[j];
							
							p_row.put( "peer_is_seed", peer.isSeed()?"1":"0" );
							p_row.put( "peer_uploaded", DisplayFormatters.formatByteCountToKBEtc(peer.getUploaded()));
							p_row.put( "peer_downloaded", DisplayFormatters.formatByteCountToKBEtc(peer.getDownloaded()));
							p_row.put( "peer_left", DisplayFormatters.formatByteCountToKBEtc(peer.getAmountLeft()));
							p_row.put( "peer_ip", hideLastIpBlock(peer.getIP()) );
							p_row.put( "peer_ip_full", peer.getIP());
						}
					}
					
					t_row.put( "peer_info", peer_info );
					t_row.put( "peer_info_count", ""+peer_info.size());
				}
			}
		}
		
		t.setParam("torrent_info", torrent_info);
		t.setParam("torrent_info_count", tracker_torrents.length);
		
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
		}else if (file_type.equals("css")){
			response_type = "text/css";
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
		}else if (file_type.equals("torrent") || file_type.equals( "tor" )) {
			response_type="application/x-bittorrent";
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
	
	private String hideLastIpBlock(String ip) {
	  if(ip == null)
	    return null;
	  StringTokenizer st = new StringTokenizer(ip,".");
	  if(st.countTokens() != 4)
	    return "*";
	  return st.nextToken() + "." + st.nextToken() + "." + st.nextToken() + ".*";
	}
}