/*
 * File    : TrackerWebPageReplyImpl.java
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

package org.gudy.azureus2.plugins.tracker.impl;

/**
 * @author parg
 *
 */

import java.util.*;
import java.io.*;
import java.net.*;

import org.gudy.azureus2.plugins.tracker.*;
import org.gudy.azureus2.plugins.tracker.web.*;
import org.gudy.azureus2.core3.tracker.host.*;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.util.*;

public class 
TrackerWebPageResponseImpl
	implements TrackerWebPageResponse
{
	protected static final String	NL			= "\r\n";
	
	protected OutputStream		os;
	
	protected ByteArrayOutputStream	baos = new ByteArrayOutputStream(2048);
	
	protected String				content_type = "text/html";
	
	protected int					reply_status	= 200;
	
	protected Vector	header_names 	= new Vector();
	protected Vector	header_values	= new Vector();
	
	protected
	TrackerWebPageResponseImpl(
		OutputStream		_os )
	{
		os	= _os;
	}
	
	public void
	setContentType(
		String		type )
	{
		content_type	= type;
	}	
	
	public void
	setReplyStatus(
		int		status )
	{
		reply_status 	= status;
	}
	
	public void
	setHeader(
			String		name,
			String		value )
	{
		header_names.add( name );
		header_values.add( value );
	}
	
	public OutputStream
	getOutputStream()
	{
		return( baos );
	}
	
	protected void
	complete()
	
		throws IOException
	{
		byte[]	reply_bytes = baos.toByteArray();
		
		String reply_header = "HTTP/1.1 " + reply_status + (reply_status == 200 || reply_status == 204?" OK":" BAD") + NL;
		
		for (int i=0;i<header_names.size();i++){
			
			String	name = (String)header_names.get(i);
			String	value = (String)header_values.get(i);
			
			reply_header += name + ": " + value + NL;
		}

		reply_header +=
			"Content-Type: " + content_type + NL +
			"Content-Length: " + reply_bytes.length + NL +
			NL;
		
		os.write( reply_header.getBytes());
		
		os.flush();
		
		os.write( reply_bytes );
		
		os.flush();
	}
	
	public void
	writeTorrent(
		TrackerTorrent	tracker_torrent )
	
		throws IOException
	{
		try{
			
			TRHostTorrent	host_torrent = ((TrackerTorrentImpl)tracker_torrent).getHostTorrent();
			
			TOTorrent	torrent = host_torrent.getTorrent();
			
			// make a copy of the torrent
			
			TOTorrent	torrent_to_send = TOTorrentFactory.deserialiseFromMap(torrent.serialiseToMap());
			
			// remove any non-standard stuff (e.g. resume data)
			
			torrent_to_send.removeAdditionalProperties();
			
			// override the announce url but not port (as this is already fixed)
			
			String 	tracker_ip 		= COConfigurationManager.getStringParameter("Tracker IP", "");
			
			// if tracker ip not set then assume they know what they're doing
			
			if ( host_torrent.getStatus() != TRHostTorrent.TS_PUBLISHED ){
				
				if ( tracker_ip.length() > 0 ){
					
					int	 	tracker_port 	= host_torrent.getPort();
					
					String protocol = torrent_to_send.getAnnounceURL().getProtocol();
					
					URL announce_url = new URL( protocol + "://" + tracker_ip + ":" + tracker_port + "/announce" );
					
					torrent_to_send.setAnnounceURL( announce_url );
					
					torrent_to_send.getAnnounceURLGroup().setAnnounceURLSets( new TOTorrentAnnounceURLSet[0]);
				}
			}
	
			baos.write( BEncoder.encode( torrent_to_send.serialiseToMap()));
			
			setContentType( "application/x-bittorrent" );
			
		}catch( TOTorrentException e ){
		
			e.printStackTrace();
		
			throw( new IOException( e.toString()));
		}
	}
}
