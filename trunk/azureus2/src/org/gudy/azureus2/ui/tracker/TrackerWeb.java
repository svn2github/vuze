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
//import java.net.*;

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
	
	protected static final String[]		welcome_pages = {"index.html", "index.htm", "index.php", "index.tmpl" };
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
	
	protected boolean
	handleTemplate(
		String			page_url,
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
		
		TrackerTorrent[]	tracker_torrents = tracker.getTorrents();
		
		args.put( "loop_context_vars", "true" );
		args.put( "global_vars", "true" );
		
		Template t = new Template( args );

		int	specific_torrent	= -1;
		int tracker_page = -1;
		int tracker_last_page = 1;
		//Todo: get tracker config tab options to set//enable this.
		//int tracker_skip = 0;
		int tracker_skip = plugin_interface.getPluginconfig().getIntParameter("Tracker Skip", 0); // 0 = disabled, range: >0
		
		// parse get parms.
		if ( params != null ){
			
			String	specific_torrents 		= (String)params.get( "torrent_info" );
			String	specific_torrents_hash 	= (String)params.get( "torrent_info_hash" );
			
			String	page = (String)params.get( "page" );
			String	skip = (String)params.get( "skip" );
			
			if ( specific_torrents != null ){
				
				specific_torrent = Integer.parseInt( specific_torrents );
				
					// 1 based -> 0 based
				
				specific_torrent--;
				
			}else if ( specific_torrents_hash != null ){
				
				byte[] hash = ByteFormatter.decodeString( specific_torrents_hash );
				
				for (int i=0;i<tracker_torrents.length;i++){
					
					if ( Arrays.equals( hash, tracker_torrents[i].getTorrent().getHash())){
						
						specific_torrent = i;
						
						break;
					}
				}
				
				if ( specific_torrent == -1 ){
					
					return( torrentNotFound(os) );
				}
			}
			
			if ( page != null ){
				
				//make sure our values are in range
				if (Integer.parseInt( page ) > -1 )
				{
					tracker_page = Integer.parseInt( page );
						// 1 based -> 0 based
					tracker_page--;
				}

			}
			if ( skip != null ){
				
				if (Integer.parseInt( skip ) > 0)
				{
					tracker_skip = Integer.parseInt( skip );
				}
			}
			
		}
		
			// set up the parameters
		
		Vector torrent_info = new Vector();
		
		
		int	start;
		int	end;
		//use "pagenation" page links ?
		boolean pagenation = (tracker_skip > 0);
		
		// if we're using pagenation we need a last page, and NOW!!
		if(pagenation)
		{
			int remainder = tracker_torrents.length % tracker_skip;
			tracker_last_page = (tracker_torrents.length-remainder)/tracker_skip;
			if(remainder >0 )
			{
				tracker_last_page++;
			}
		}
		
		if ( specific_torrent != -1 ){
			
			start 	= specific_torrent;
			
			end		= specific_torrent+1;
		}
		else{
			int tracker_start = 0;
			int tracker_end = tracker_torrents.length;
			//are we using pages ?
			if (pagenation)
			{
				//make sure we start on the first page.
				if(tracker_page == -1)
				{
					tracker_page = 0;
				}
				
				if (tracker_page < tracker_last_page)
				{
					//where to start..
					tracker_start = (tracker_page * tracker_skip);
				}
				else
				{
					//default to page 0
					tracker_page = 0;
					tracker_start = (tracker_page * tracker_skip);
				}
				if ((tracker_page+1) * tracker_skip <= tracker_end)
				{
					//where to end..
					tracker_end = ((tracker_page+1) * tracker_skip);
				}

			}
			start 	= tracker_start;
			end		= tracker_end;
			//debug
			//System.out.println("start: "+start+"\n");
			//System.out.println("end: "+end+"\n");

		}

		t.setParam( "page_url", page_url );
		//Pagenation
		if (pagenation)
		{
			if ( specific_torrent == -1 )
			{
				t.setParam( "show_pagenation", "1");

				t.setParam( "show_first_link", (tracker_page>1)?"1":"0");
				t.setParam( "first_link", page_url+"?skip="+tracker_skip+"&page=1" );
				t.setParam( "show_previous_link", (tracker_page>0)?"1":"0");
				t.setParam( "previous_link", page_url+"?skip="+tracker_skip+"&page="+(tracker_page) );
				t.setParam( "show_last_link", (tracker_page<tracker_last_page-2)?"1":"0");
				t.setParam( "last_link", page_url+"?skip="+tracker_skip+"&page="+tracker_last_page );
				t.setParam( "show_next_link", (tracker_page<tracker_last_page-1)?"1":"0");
				t.setParam( "next_link", page_url+"?skip="+tracker_skip+"&page="+(tracker_page+2) );

				t.setParam("current_page", tracker_page+1);
				String pagenation_text = "";
				
				for(int i=1; i<=tracker_last_page; i++)
				{
					if(i==tracker_page+1)
					{
						pagenation_text = pagenation_text+"<span class=\"pagenation\">"+i+"</span> ";
					}
					else
					{	
						pagenation_text = pagenation_text+"<a href=\""+page_url+"?skip="+tracker_skip+"&page="+i+"\" class=\"pagenation\">"+i+"</a> ";
					}
				}
				
				t.setParam( "pagenation", pagenation_text);
				
				
			}
		}
		else 
		{
			t.setParam( "show_pagenation", "0");
		}

		boolean	allow_details = plugin_interface.getPluginconfig().getBooleanParameter("Tracker Publish Enable Details", true );
		
		t.setParam( "torrent_details_allowed", allow_details?"1":"0");
	
		for (int i=start;i<end;i++){
			
			Hashtable t_row = new Hashtable();
			
			torrent_info.add( t_row );
			
			TrackerTorrent	tracker_torrent = tracker_torrents[i];
			
			Torrent	torrent = tracker_torrent.getTorrent();
			
			//String	hash_str = URLEncoder.encode( new String( torrent.getHash(), Constants.BYTE_ENCODING ), Constants.BYTE_ENCODING );
			String	hash_str = ByteFormatter.encodeString( torrent.getHash());
			
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

				t_row.put( "torrent_details_url", "details.tmpl?torrent_info_hash=" + hash_str );
				
				t_row.put( "torrent_details_params", "torrent_info_hash=" + hash_str );
			}else{
				
				t_row.put( "torrent_download_url", "#" );
				t_row.put( "torrent_details_url", "#" );
				t_row.put( "torrent_details_params", "");
			}
			
			t_row.put( "torrent_status", status_str );
			
			t_row.put( "torrent_size", (torrent.getSize()<=0?"N/A":DisplayFormatters.formatByteCountToKiBEtc( torrent.getSize())));
			
			t_row.put( "torrent_seeds", "" + seed_count );
			
			t_row.put( "torrent_peers", "" + non_seed_count );

			t_row.put( "torrent_total_upload", DisplayFormatters.formatByteCountToKiBEtc( tracker_torrent.getTotalUploaded())); 
			
			t_row.put( "torrent_total_download", DisplayFormatters.formatByteCountToKiBEtc( tracker_torrent.getTotalDownloaded())); 
			
			t_row.put( "torrent_upload_speed", DisplayFormatters.formatByteCountToKiBEtcPerSec( tracker_torrent.getAverageUploaded())); 
			
			t_row.put( "torrent_download_speed", DisplayFormatters.formatByteCountToKiBEtcPerSec( tracker_torrent.getAverageDownloaded())); 
			
			t_row.put( "torrent_total_left", DisplayFormatters.formatByteCountToKiBEtc( tracker_torrent.getTotalLeft())); 
			
			t_row.put( "torrent_completed",  "" + tracker_torrent.getCompletedCount());
			

			if ( allow_details ){
				
				if ( specific_torrent != -1 ){
				
					t_row.put( "torrent_hash", ByteFormatter.nicePrint(torrent.getHash(),true));
								
						// size is 0 for external torrents about which we know no more
					
					if ( torrent.getSize() > 0 ){
				
							// if we put out a normal space for optional bits then the table doesn't draw properly
						
						t_row.put( "torrent_comment", torrent.getComment().length()==0?"&nbsp;":torrent.getComment());
						
						t_row.put( "torrent_created_by", torrent.getCreatedBy().length()==0?"&nbsp;":torrent.getCreatedBy());
						
						String	date = DisplayFormatters.formatDate(torrent.getCreationDate() * 1000 );
						
						t_row.put( "torrent_created_on", date.length()==0?"&nbsp;":date );
						
						t_row.put( "torrent_piece_size", DisplayFormatters.formatByteCountToKiBEtc(torrent.getPieceSize()));
						
						t_row.put( "torrent_piece_count", ""+torrent.getPieceCount());
						
						Vector	file_info = new Vector();
						
						TorrentFile[]	files = torrent.getFiles();
						
						for (int j=0;j<files.length;j++){
							
							Hashtable	f_row = new Hashtable();
							
							file_info.add( f_row );
							
							f_row.put( "file_name", files[j].getName());
							
							f_row.put( "file_size", DisplayFormatters.formatByteCountToKiBEtc(files[j].getSize()));
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
							
							long	uploaded 	= peer.getUploaded();
							long	downloaded	= peer.getDownloaded();
							
							float	share_ratio;
							
							if ( downloaded == 0 ){
								
								share_ratio = uploaded==0?1:-1;
								
							}else{
								share_ratio	= (float)((uploaded*1000)/downloaded)/1000;
							}
							
							int	share_health = (int)share_ratio*5;
							
							if ( share_health > 5 ){
								
								share_health = 5;
							}
														
							p_row.put( "peer_is_seed", peer.isSeed()?"1":"0" );
							p_row.put( "peer_uploaded", DisplayFormatters.formatByteCountToKiBEtc(uploaded));
							p_row.put( "peer_downloaded", DisplayFormatters.formatByteCountToKiBEtc(downloaded));
							p_row.put( "peer_left", DisplayFormatters.formatByteCountToKiBEtc(peer.getAmountLeft()));
							p_row.put( "peer_ip", hideLastIpBlock(peer.getIP()) );
							p_row.put( "peer_ip_full", peer.getIP());
							p_row.put( "peer_share_ratio", (share_ratio==-1?"&#8734;":""+share_ratio));
							p_row.put( "peer_share_health", ""+share_health );
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
		
		return( true );
	}
	
	protected boolean
	torrentNotFound(
		OutputStream	os )
	
		throws IOException
	{
		os.write( "Torrent not found".getBytes());
		
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
	
	// Map the Homepage URL.
	protected String mapHomePage( String url_in )
	{
	  if ( url_in.equals("/")){
		for (int i=0;i<welcome_files.length;i++){
		  if ( welcome_files[i].exists()){
			url_in = "/" + welcome_pages[i];
			return (url_in);
		  }
		}
	  }
	  return (url_in);
	}
}