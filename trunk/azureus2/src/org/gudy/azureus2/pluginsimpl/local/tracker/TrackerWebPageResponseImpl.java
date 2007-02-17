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

package org.gudy.azureus2.pluginsimpl.local.tracker;

/**
 * @author parg
 *
 */

import java.util.*;
import java.io.*;
import java.net.*;

import org.gudy.azureus2.plugins.tracker.*;
import org.gudy.azureus2.plugins.tracker.web.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.tracker.host.*;
import org.gudy.azureus2.core3.tracker.util.TRTrackerUtils;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.util.HTTPUtils;

public class 
TrackerWebPageResponseImpl
	implements TrackerWebPageResponse
{
	protected static final String	NL			= "\r\n";
	
	protected OutputStream		os;
	
	protected ByteArrayOutputStream	baos = new ByteArrayOutputStream(2048);
	
	protected String				content_type = "text/html";
	
	protected int					reply_status	= 200;
	
	protected Map		header_map 	= new LinkedHashMap();
	
	protected
	TrackerWebPageResponseImpl(
		OutputStream		_os )
	{
		os	= _os;
				
		String	formatted_date_now		 = TimeFormatter.getHTTPDate( SystemTime.getCurrentTime());
		
		setHeader( "Last-Modified",	formatted_date_now );
		
		setHeader( "Expires", formatted_date_now );
	}
	
	public void
	setLastModified(
		long		time )
	{		
		String	formatted_date		 = TimeFormatter.getHTTPDate( time );

		setHeader( "Last-Modified",	formatted_date );
	}
	
	public void
	setExpires(
		long		time )
	{		
		String	formatted_date		 = TimeFormatter.getHTTPDate( time );

		setHeader( "Expires",	formatted_date );	
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
		addHeader( name, value, true );
	}
	
	protected void
	addHeader(
		String		name,
		String		value,
		boolean		replace )
	{
		Iterator	it = header_map.keySet().iterator();
		
		while( it.hasNext()){
			
			String	key = (String)it.next();
			
			if ( key.equalsIgnoreCase( name )){
				
				if ( replace ){
					
					it.remove();
					
				}else{
					
					return;
				}
			}
		}
		
		header_map.put( name, value );
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
		
		// System.out.println( "TrackerWebPageResponse::complete: data = " + reply_bytes.length );
		
		String	status_string = "BAD";
		
			// random collection
		
		if ( reply_status == 200 ){
			
			status_string = "OK";
			
		}else if ( reply_status == 204 ){
			
			status_string = "No Content";
			
		}else if ( reply_status == 206 ){
			
			status_string = "Partial Content";
			
		}else if ( reply_status == 401 ){
			
			status_string = "Unauthorized";
			
		}else if ( reply_status == 404 ){
			
			status_string = "Not Found";
			
		}else if ( reply_status == 501 ){
				
			status_string = "Not Implemented";
		}
		
		String reply_header = "HTTP/1.1 " + reply_status + " " + status_string + NL;
		
			// add header fields if not already present
		
		addHeader( "Server", Constants.AZUREUS_NAME + " " + Constants.AZUREUS_VERSION, false );
		addHeader( "Connection", "close", false );
		addHeader( "Content-Type", content_type, false );
		
		Iterator	it = header_map.keySet().iterator();
		
		while( it.hasNext()){
			
			String	name 	= (String)it.next();
			String	value 	= (String)header_map.get(name);
			
			reply_header += name + ": " + value + NL;
		}

		reply_header +=
			"Content-Length: " + reply_bytes.length + NL +
			NL;
		
		// System.out.println( "writing reply:" + reply_header );
		
		os.write( reply_header.getBytes());
		
		os.flush();
		
		os.write( reply_bytes );
		
		os.flush();
	}
	
	public boolean
	useFile(
		String		root_dir,
		String		relative_url )
	
		throws IOException
	{
		String	target = root_dir + relative_url.replace('/',File.separatorChar);
		
		File canonical_file = new File(target).getCanonicalFile();
		
			// make sure some fool isn't trying to use ../../ to escape from web dir
		
		if ( !canonical_file.toString().startsWith( root_dir )){
			
			return( false );
		}
		
		if ( canonical_file.isDirectory()){
			
			return( false );
		}

		if ( canonical_file.canRead()){
			
			String str = canonical_file.toString().toLowerCase();
			
			int	pos = str.lastIndexOf( "." );
			
			if ( pos == -1 ){
				
				return( false );
			}
			
			String	file_type = str.substring(pos+1);
				
			FileInputStream	fis = null;
				
			try{
				fis = new FileInputStream(canonical_file);
					
				useStream( file_type, fis );
					
				return( true );
				
			}finally{
				
				if ( fis != null ){
					
					fis.close();
				}
			}
		}
		
		return( false );
	}
	
	public void
	useStream(
		String		file_type,
		InputStream	input_stream )
		
		throws IOException
	{
		String	response_type = HTTPUtils.guessContentTypeFromFileType( file_type );

		OutputStream	os = getOutputStream();
			
		setContentType( response_type );
			
		byte[]	buffer = new byte[4096];
			
		while(true){
				
			int	len = input_stream.read(buffer);
				
			if ( len <= 0 ){
					
				break;
			}
				
			os.write( buffer, 0, len );
		}
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
			
			if ( !TorrentUtils.isDecentralised( torrent_to_send )){
								
				URL[][]	url_sets = TRTrackerUtils.getAnnounceURLs();
									
					// if tracker ip not set then assume they know what they're doing

				if ( host_torrent.getStatus() != TRHostTorrent.TS_PUBLISHED && url_sets.length > 0 ){
				
						// if the user has disabled the mangling of urls when hosting then don't do it here
						// either
					
					if ( COConfigurationManager.getBooleanParameter("Tracker Host Add Our Announce URLs")){
						
						String protocol = torrent_to_send.getAnnounceURL().getProtocol();
	
						for (int i=0;i<url_sets.length;i++){
												
							URL[]	urls = url_sets[i];
							
							if ( urls[0].getProtocol().equalsIgnoreCase( protocol )){
								
								torrent_to_send.setAnnounceURL( urls[0] );
							
								torrent_to_send.getAnnounceURLGroup().setAnnounceURLSets( new TOTorrentAnnounceURLSet[0]);
								
								for (int j=1;j<urls.length;j++){
									
									TorrentUtils.announceGroupsInsertLast( torrent_to_send, new URL[]{ urls[j] });
								}
								
								break;
							}
						}
					}
				}
			}
	
			baos.write( BEncoder.encode( torrent_to_send.serialiseToMap()));
			
			setContentType( "application/x-bittorrent" );
			
		}catch( TOTorrentException e ){
		
			Debug.printStackTrace( e );
		
			throw( new IOException( e.toString()));
		}
	}
}
