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

package org.gudy.azureus2.pluginsimpl.tracker;

/**
 * @author parg
 *
 */

import java.text.SimpleDateFormat;
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
		
		SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
		
		String	formatted_date_now		 = format.format(new Date());
		//String	formatted_date_bit_later = format.format(new Date( SystemTime.getCurrentTime() + 30000 ));
		
		setHeader( "Last-Modified",	formatted_date_now );
		
		setHeader( "Expires", formatted_date_now );
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
		
		// System.out.println( "TrackerWebPageResponse::complete: data = " + reply_bytes.length );
		
		String reply_header = "HTTP/1.1 " + reply_status + (reply_status == 200 || reply_status == 204?" OK":" BAD") + NL;
		
		for (int i=0;i<header_names.size();i++){
			
			String	name = (String)header_names.get(i);
			String	value = (String)header_values.get(i);
			
			reply_header += name + ": " + value + NL;
		}

		reply_header +=
			"Server: "+ Constants.AZUREUS_NAME + " " + Constants.AZUREUS_VERSION + NL +
			"Connection: close" + NL+
			"Content-Type: " + content_type + NL +
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
		String	response_type = null;
		
		if (file_type.equals("html") || file_type.equals("htm")){
			response_type = "text/html";
		}else if (file_type.equals("css")){
			response_type = "text/css";
		}else if (file_type.equals("xsl")){
			response_type = "text/xml";
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
		}else if ( file_type.equals( "zip")){
			response_type = "application/zip";
		}else if ( file_type.equals( "txt" )){
			response_type = "text/plain";
		}else if ( file_type.equals( "jar" )){
			response_type = "application/java-archive";
		}else if ( file_type.equals( "mp3" )){
			response_type = "audio/x-mpeg";
		}else{
			response_type = "application/octet-stream";
		}

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
