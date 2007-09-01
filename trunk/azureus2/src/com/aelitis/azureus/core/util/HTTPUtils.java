/*
 * Created on Feb 15, 2007
 * Created by Paul Gardner
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package com.aelitis.azureus.core.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class 
HTTPUtils 
{
	public static final String	NL	= "\r\n";

	private static final String[][]	type_map = {
			{ "html",		"text/html" },
			{ "htm",		"text/html" },
			{ "css",		"text/css" },
			{ "xml",		"text/xml" },
			{ "xsl",		"text/xml" },
			{ "jpg",		"image/jpeg" },
			{ "jpeg",		"image/jpeg" },
			{ "gif",		"image/gif" },
			{ "tiff",		"image/tiff" },
			{ "bmp",		"image/bmp" },
			{ "png",		"image/png" },
			{ "torrent",	"application/x-bittorrent" },
			{ "tor",		"application/x-bittorrent" },
			{ "zip",		"application/zip" },
			{ "txt",		"text/plain" },
			{ "jar",		"application/java-archive" },
			{ "jnlp",		"application/x-java-jnlp-file" },
			{ "mp3",		"audio/x-mpeg" },
			{ "flv",		"video/x-flv" },
	};
	
	private static final String default_type = "application/octet-stream";
	
	private static final Map	map = new HashMap();
	
	static{
		
		for (int i=0;i<type_map.length;i++){
			
			map.put( type_map[i][0], type_map[i][1] );
		}
	}
	
	public static String
	guessContentTypeFromFileType(
		String	file_type )
	{
		if ( file_type != null ){
			
			String	type = (String)map.get( file_type.toLowerCase());

			if ( type != null ){
				
				return( type );
			}
		}
		
		return( default_type );
	}
	
	public static InputStream
	decodeChunkedEncoding(
		InputStream	is )
	
		throws IOException
	{
		String	reply_header = "";
		
		while(true){
			
			byte[]	buffer = new byte[1];
			
			if ( is.read( buffer ) <= 0 ){
				
				throw( new IOException( "Premature end of input stream" ));
			}
			
			reply_header += (char)buffer[0];
			
			if ( reply_header.endsWith( NL+NL )){
				
				break;
			}
		}
		
		int p1 = reply_header.indexOf( NL );
		
		String	first_line = reply_header.substring( 0, p1 ).trim();
		
		if ( first_line.indexOf( "200" ) == -1 ){
			
			throw( new IOException( "HTTP request failed:" + first_line ));
		}
		
		String	lc_reply_header = reply_header.toLowerCase();
		
		int	te_pos = lc_reply_header.indexOf( "transfer-encoding" );
		
		if ( te_pos != -1 ){
			
			String	property = lc_reply_header.substring( te_pos );
			
			property = property.substring( property.indexOf(':') + 1, property.indexOf( NL )).trim();
			
			if ( property.equals( "chunked" )){
		
				ByteArrayOutputStream	baos = new ByteArrayOutputStream();
				
				String	chunk = "";
				
				int	total_length = 0;
				
				while( true ){
					
					int	x = is.read();
					
					if ( x == -1 ){
						
						break;
					}
					
					chunk += (char)x;
					
						// second time around the chunk will be prefixed with NL from end of previous
						// so make sure we ignore this
					
					if ( chunk.endsWith( NL ) && chunk.length() > 2 ){
						
						int	semi_pos = chunk.indexOf(';');
						
						if ( semi_pos != -1 ){
							
							chunk = chunk.substring(0,semi_pos);
						}
						
						chunk = chunk.trim();
						
						int	chunk_length = Integer.parseInt( chunk, 16 );
						
						if ( chunk_length <= 0 ){
							
							break;
						}
						
						total_length += chunk_length;
						
						if ( total_length > 1024*1024 ){
							
							throw( new IOException( "Chunk size " + chunk_length + " too large" ));
						}
												
						byte[] buffer = new byte[chunk_length];
						
						int	buffer_pos 	= 0;
						int	rem			= chunk_length;
						
						while( rem > 0 ){
							
							int	len = is.read( buffer, buffer_pos, rem );
							
							if ( len <= 0 ){
								
								throw( new IOException( "Premature end of stream" ));
							}
							
							buffer_pos 	+= len;
							rem			-= len;
						}
							
						baos.write( buffer );

						chunk	= "";
					}
				}
				
				return( new ByteArrayInputStream( baos.toByteArray()));
			}
		}
		
		return( is );
	}
}
