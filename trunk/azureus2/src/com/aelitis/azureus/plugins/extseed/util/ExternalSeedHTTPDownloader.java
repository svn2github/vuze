/*
 * Created on 16-Dec-2005
 * Created by Paul Gardner
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.plugins.extseed.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.StringTokenizer;

import com.aelitis.azureus.plugins.extseed.ExternalSeedException;

public class 
ExternalSeedHTTPDownloader 
{
	public static final String	NL = "\r\n";
	

	private URL			url;
	private String		user_agent;
	
	public
	ExternalSeedHTTPDownloader(
		URL		_url,
		String	_user_agent )
	{
		url			= _url;
		user_agent	= _user_agent;
	}
	
	public byte[]
	download(
		int				length )
	
		throws ExternalSeedException
	{
		return( download( new String[0], new String[0], length ));
	}
	
	public byte[]
	downloadRange(
		long		offset,
		int			length )
	
		throws ExternalSeedException
	{
		return( download( 
					new String[]{ "Range" }, new String[]{ "bytes=" + offset + "-" + (offset+length-1)},
					length ));
	}
	
	public byte[]
	download(
		String[]		prop_names,
		String[]		prop_values,
		int				length )
	
		throws ExternalSeedException
	{
		try{
			HttpURLConnection	connection = (HttpURLConnection)url.openConnection();
			
			connection.setRequestProperty( "Connection", "Keep-Alive" );
			connection.setRequestProperty( "User-Agent", user_agent );
			
			for (int i=0;i<prop_names.length;i++){
				
				connection.setRequestProperty( prop_names[i], prop_values[i] );
			}
			
			int	response = connection.getResponseCode();
			
			if ( 	response == HttpURLConnection.HTTP_ACCEPTED || 
					response == HttpURLConnection.HTTP_OK ||
					response == HttpURLConnection.HTTP_PARTIAL ){
				
				byte[]	data = new byte[length];
				
				InputStream	is = connection.getInputStream();
				
				try{
					int	pos = 0;
					
					while( pos < length ){
						
						int	len = is.read( data, pos, length-pos );
						
						if ( len < 0 ){
							
							break;
						}
						
						pos	+= len;
					}
					
					if ( pos != length ){
						
						String	data_str =  new String( data, 0, length );
						
						if ( data_str.length() > 64 ){
							
							data_str = data_str.substring( 0, 64 );
						}
						
						throw( new ExternalSeedException("Connection failed: data too short - " + length + "/" + pos + " [" + data_str + "]" ));
					}
				}finally{
					
					is.close();
				}
				
				return( data );
				
			}else{
				
				ExternalSeedException	error = new ExternalSeedException("Connection failed: " + connection.getResponseMessage());
				
				error.setPermanentFailure( true );
				
				throw( error );
			}
		}catch( Throwable e ){
			
			if ( e instanceof ExternalSeedException ){
				
				throw((ExternalSeedException)e);
			}
			
			throw( new ExternalSeedException("Connection failed", e ));
		}
	}
	
	public byte[]
	downloadSocket(
		int				length )
	        	
	    throws ExternalSeedException
	{
		return( downloadSocket( new String[0], new String[0], length ));
	}
	
	public byte[]
	downloadSocket(
		String[]		prop_names,
		String[]		prop_values,
		int				length )
	
		throws ExternalSeedException
	{
		Socket	socket	= null;
		
		try{
			ByteArrayOutputStream	baos = new ByteArrayOutputStream();
			
			PrintWriter	pw = new  PrintWriter( baos );
					
			pw.print( "GET " + url.getPath() + "?" + url.getQuery() + " HTTP/1.0" + NL );
			pw.print( "Host: " + url.getHost() + (url.getPort()==-1?"":":" + url.getPort()) + NL );	// TODO: HTTPS
			pw.print( "Accept: */*" + NL );
			pw.print( "Connection: Keep-Alive" + NL );
			pw.print( "User-Agent: " + user_agent + NL );
		
			for (int i=0;i<prop_names.length;i++){
				
				pw.print( prop_names[i] + ":" + prop_values[i] + NL );
			}
			
			pw.print( NL );
			pw.flush();
			
			socket = new Socket(  url.getHost(), url.getPort()==-1?url.getDefaultPort():url.getPort());
			
			OutputStream	os = socket.getOutputStream();
			
			os.write( baos.toByteArray());
			
			os.flush();
			
			InputStream is = new BufferedInputStream( socket.getInputStream());
			
			String	header = "";
			
			while( true ){
				
				byte[]	buffer = new byte[1];
				
				int	len = is.read( buffer );
				
				if ( len < 0 ){
					
					throw( new IOException( "input too short reading header" ));
				}
				
				header	+= (char)buffer[0];
				
				if ( header.endsWith(NL+NL)){
				
					break;
				}
			}
			
			System.out.println( "header = " + header );
			
			// HTTP/1.1 403 Forbidden
			
			int	line_end = header.indexOf(NL);
			
			if ( line_end == -1 ){
				
				throw( new IOException( "header too short" ));
			}
			
			String	first_line = header.substring(0,line_end);
			
			StringTokenizer	tok = new StringTokenizer(first_line, " " );
			
			tok.nextToken();
			
			int	response = Integer.parseInt( tok.nextToken());
			
			String	response_str	= tok.nextToken();
			
			if ( 	response == HttpURLConnection.HTTP_ACCEPTED || 
					response == HttpURLConnection.HTTP_OK ||
					response == HttpURLConnection.HTTP_PARTIAL ){
				
				byte[]	data = new byte[length];
								
				try{
					int	pos = 0;
					
					while( pos < length ){
						
						int	len = is.read( data, pos, length-pos );
						
						if ( len < 0 ){
							
							break;
						}
						
						pos	+= len;
					}
					
					if ( pos != length ){
						
						String	data_str =  new String( data, 0, length );
						
						if ( data_str.length() > 64 ){
							
							data_str = data_str.substring( 0, 64 );
						}
						
						throw( new ExternalSeedException("Connection failed: data too short - " + length + "/" + pos + " [" + data_str + "]" ));
					}
				}finally{
					
					is.close();
				}
				
				return( data );
				
			}else{
				
				ExternalSeedException	error = new ExternalSeedException("Connection failed: " + response_str );
				
				error.setPermanentFailure( true );
				
				throw( error );
			}
		}catch( Throwable e ){
			
			if ( e instanceof ExternalSeedException ){
				
				throw((ExternalSeedException)e);
			}
			
			throw( new ExternalSeedException("Connection failed", e ));
			
		}finally{
			
			if ( socket != null ){
				
				try{
					socket.close();
					
				}catch( Throwable e ){
				}
			}
		}
	}
}
