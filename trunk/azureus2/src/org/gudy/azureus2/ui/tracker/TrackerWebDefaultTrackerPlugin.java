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
import org.gudy.azureus2.ui.common.UIImageRepository;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.torrent.*;
import org.gudy.azureus2.plugins.tracker.*;
import org.gudy.azureus2.plugins.tracker.web.*;

public class 
TrackerWebDefaultTrackerPlugin
	extends TrackerWeb
{
	public void 
	initialize(
			PluginInterface _plugin_interface )
	{	
		super.initialize( _plugin_interface );
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
		
		// System.out.println( "TrackerWebDefaultTrackerPlugin: " + url);
		
		try{
			if ( url.startsWith( "/torrents/")){
				
				String	str = url.substring(10);
				
				int	pos = str.indexOf ( "?" );
				
				String	hash_str = str.substring(pos+1);
				
				//byte[]	hash = URLDecoder.decode( hash_str, Constants.BYTE_ENCODING ).getBytes( Constants.BYTE_ENCODING );
				byte[]	hash = ByteFormatter.decodeString( hash_str );
				
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
				
				InputStream is = UIImageRepository.getImageAsStream( "favicon.ico" );
				
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
				
				if ( url.equals("/")){
					
					url = "/index.tmpl";
				}
				
				Hashtable	params = null;
				
				int	p_pos = url.indexOf( '?' );
				
				if ( p_pos != -1 ){
					
					params = decodeParams( url.substring( p_pos+1 ));
					
					url = url.substring(0,p_pos);
				}	
				
				InputStream is = TrackerWebDefaultTrackerPlugin.class.getClassLoader().getResourceAsStream("org/gudy/azureus2/ui/tracker/templates" + url );
				
				if ( is == null ){
					
					return( false );
				}
				
				try{
					int	pos = url.lastIndexOf( "." );
					
					if ( pos == -1 ){
						
						return( false );
					}
					String	file_type = url.substring(pos+1);
					
					if ( file_type.equals("php") || file_type.equals("tmpl")){
						
						Hashtable	args = new Hashtable();
						
						args.put( "filehandle", new InputStreamReader( is ));

						handleTemplate( url, params, args, os );
	
						return( true );
						
					}else{ 
													
						return( transferFile( file_type, is, response ));
					}	
				}finally{
													
					is.close();
				}			
			}
		}catch( Throwable e ){
						
			e.printStackTrace();
			
			os.write( e.toString().getBytes());
		}

		return( true );
	}
}
