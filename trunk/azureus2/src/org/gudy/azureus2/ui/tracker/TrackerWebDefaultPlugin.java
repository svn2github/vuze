/*
 * File    : TrackerWebDefaultPlugin.java
 * Created : 08-Dec-2003
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

import java.net.*;
import java.io.*;
import java.util.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.common.*;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.torrent.*;
import org.gudy.azureus2.plugins.tracker.*;
import org.gudy.azureus2.plugins.tracker.web.*;

public class 
TrackerWebDefaultPlugin
	implements Plugin, TrackerWebPageGenerator
{
	protected static final String	NL			= "\r\n";
	
	protected PluginInterface		plugin_interface;
	protected Tracker				tracker;
	
	public void 
	initialize(
		PluginInterface _plugin_interface )
	{	
		plugin_interface	= _plugin_interface;
		
		tracker = plugin_interface.getTracker();
		
		tracker.addPageGenerator( this );
	}
	
	public boolean
	generate(
		TrackerWebPageRequest		request,
		TrackerWebPageResponse		response )
	
		throws IOException
	{
		String	url = request.getURL();
				
		TrackerTorrent[]	torrents = tracker.getTorrents();
		
		OutputStream	os = response.getOutputStream();
		
		try{
			if ( url.equals( "/" )){
				
				String[]	widths = { "30%", "10%", "10%", "8%", "6%", "6%", "6%", "6%", "6%", "6%", "6%" };
				
				String	reply_string = 
				"<html>" +
				"<head>" +
				"<title> Azureus : Java BitTorrent Client Tracker</title>" + 
				"<META HTTP-EQUIV=\"Pragma\" CONTENT=\"no-cache\">" +
				"<META HTTP-EQUIV=\"Expires\" CONTENT=\"-1\">" +
				"<meta name=\"keywords\" content=\"BitTorrent, bt, java, client, azureus\">" +
				"<link rel=\"stylesheet\" href=\"http://azureus.sourceforge.net/style.css\" type=\"text/css\">" +
				"</head>" +
				"<body>" +
				//"<table align=\"center\" class=\"body\" cellpadding=\"0\" cellspacing=\"0\">" + 
				//"<tr><td>" +
				"<table align=\"center\" class=\"main\" cellpadding=\"0\" cellspacing=\"0\">" +
				"<tr><td>" +
				"<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" bordercolor=\"#111111\" width=\"100%\">" +
				"  <tr>" +
				"	<td><a href=\"http://azureus.sourceforge.net/\"><img src=\"http://azureus.sourceforge.net/img/Azureus_banner.gif\" border=\"0\" alt=\"Azureus\" hspace=\"0\" width=\"100\" height=\"40\" /></a></td>" +
				"	<td><p align=\"center\"><font size=\"5\">Azureus: BitTorrent Client Tracker</font></td>" +
				"  </tr>" +
				"</table>" +
				"<table align=\"center\" class=\"main1\" bgcolor=\"#526ED6\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">" +
				"<tr><td valign=\"top\" height=\"20\"></td></tr>" +
				"<tr>" +
				"  <td valign=\"top\">"+
				"   <table align=\"center\" border=\"1\" cellpadding=\"2\" cellspacing=\"1\" bordercolor=\"#111111\" width=\"96%\" bgcolor=\"#D7E0FF\">" +
				"   <thead>" +
				"     <tr>" +
				"	    <td width=\""+widths[0]+"\" bgcolor=\"#FFDEAD\">Torrent</td>" +
				"	    <td width=\""+widths[1]+"\" bgcolor=\"#FFDEAD\">Status</td>" +
				"	    <td width=\""+widths[2]+"\" bgcolor=\"#FFDEAD\">Size</td>" +
				"	    <td width=\""+widths[3]+"\" bgcolor=\"#FFDEAD\">Seeds</td>" +
				"	    <td width=\""+widths[4]+"\" bgcolor=\"#FFDEAD\">Peers</td>" +
				"	    <td width=\""+widths[5]+"\" bgcolor=\"#FFDEAD\">Tot Up</td>" +
				"	    <td width=\""+widths[6]+"\" bgcolor=\"#FFDEAD\">Tot Down</td>" +
				"	    <td width=\""+widths[7]+"\" bgcolor=\"#FFDEAD\">Ave Up</td>" +
				"	    <td width=\""+widths[8]+"\" bgcolor=\"#FFDEAD\">Ave Down</td>" +
				"	    <td width=\""+widths[9]+"\" bgcolor=\"#FFDEAD\">Left</td>" +
				"	    <td width=\""+widths[10]+"\" bgcolor=\"#FFDEAD\">Comp</td>" +
				"	  </tr>" +
				"    </thread>";
				
				StringBuffer	table_bit = new StringBuffer(1024);
				
				synchronized( this ){
					
					for (int i=0;i<torrents.length;i++){
						
						TrackerTorrent	tracker_torrent = torrents[i];
						
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
						
						table_bit.append( "<tr>" );
						
						if ( torrent.getSize() > 0 ){
							
							table_bit.append( "<td>"+
									"<a href=\"/torrents/" + torrent_name.replace('?','_') + ".torrent?" + hash_str + "\">" + torrent_name + "</a></td>" );
						}else{			  
							
							table_bit.append( "<td>" + torrent_name + "</td>" );
						}
						
						table_bit.append( "<td>" + status_str + "</td>" );
						
						table_bit.append( "<td>" + 
								(torrent.getSize()<=0?"N/A":DisplayFormatters.formatByteCountToKBEtc( torrent.getSize())) + "</td>" );
						
						table_bit.append( "<td><b><font color=\"" + (seed_count==0?"#FF0000":"#00CC00")+"\">" +
								seed_count + "</font></b></td>" );
						
						table_bit.append( "<td>" + 
								non_seed_count + "</td>" );
						
						table_bit.append( "<td>" + 
								DisplayFormatters.formatByteCountToKBEtc( tracker_torrent.getTotalUploaded()) + 
						"</td>" );
						
						table_bit.append( "<td>" + 
								DisplayFormatters.formatByteCountToKBEtc( tracker_torrent.getTotalDownloaded()) + 
						"</td>" );
						
						table_bit.append( "<td>" + 
								DisplayFormatters.formatByteCountToKBEtcPerSec( tracker_torrent.getAverageUploaded()) + 
						"</td>" );
						
						table_bit.append( "<td>" + 
								DisplayFormatters.formatByteCountToKBEtcPerSec( tracker_torrent.getAverageDownloaded()) + 
						"</td>" );
						
						table_bit.append( "<td>" + 
								DisplayFormatters.formatByteCountToKBEtc( tracker_torrent.getTotalLeft()) + 
						"</td>" );
						
						table_bit.append( "<td>" + tracker_torrent.getCompletedCount() + 
						"</td>" );
						
						table_bit.append( "</tr>" );
					}	
				}
				
				reply_string += table_bit;
				
				reply_string +=
				"    </table>" +
				"    <tr><td>&nbsp;</tr></td>" +
				"  </td>" +
				"</tr>" +
				"</table>" +
				"</td></tr>" +
				"</table>" +
				//"</td></tr>" +
				//"</table>" +
				"</body>" +
				"</html>";
				
				os.write( reply_string.getBytes());
				
			}else if ( url.startsWith( "/torrents/")){
				
				String	str = url.substring(10);
				
				int	pos = str.indexOf ( "?" );
				
				String	hash_str = str.substring(pos+1);
				
				byte[]	hash = URLDecoder.decode( hash_str, Constants.BYTE_ENCODING ).getBytes( Constants.BYTE_ENCODING );
				
				synchronized( this ){
					
					for (int i=0;i<torrents.length;i++){
						
						TrackerTorrent	tracker_torrent = torrents[i];
						
						Torrent	torrent = tracker_torrent.getTorrent();
						
						if ( Arrays.equals( hash, torrent.getHash())){
							
							response.writeTorrent( tracker_torrent );
							
							return( true );
						}
					}
				}
				
				System.out.println( "Torrent not found at '" + url + "'" );
										
				response.setReplyStatus( 404 );
			
			}else if ( url.equalsIgnoreCase("/favicon.ico" )){
								
				response.setContentType( "image/x-icon" );
				
				response.setHeader( "Last Modified",
									"Fri,05 Sep 2003 01:01:01 GMT" );
				
				response.setHeader( "Expires",
									"Sun, 17 Jan 2038 01:01:01 GMT" );
				
				InputStream is = ImageRepository.getImageAsStream( "favicon.ico" );
				
				if ( is == null ){
										
					response.setReplyStatus( 404 );
					
				}else{
					
					byte[] data = new byte[4096];
										
					while(true){
						
						int len = is.read(data, 0, 4096 );
						
						if ( len <= 0 ){
							
							break;
						}
						
						os.write( data, 0, len );
					}	
				}
			}else{

				return( false );
			}
		}catch( Throwable e ){
						
			os.write( e.toString().getBytes());
		}

		return( true );
	}
}
