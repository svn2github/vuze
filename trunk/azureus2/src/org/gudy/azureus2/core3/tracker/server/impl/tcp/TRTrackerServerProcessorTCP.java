/*
 * File    : TRTrackerServerProcessor.java
 * Created : 5 Oct. 2003
 * By      : Parg 
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

package org.gudy.azureus2.core3.tracker.server.impl.tcp;


import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;

import sun.misc.BASE64Decoder;

import org.gudy.azureus2.core3.tracker.server.*;
import org.gudy.azureus2.core3.tracker.server.impl.*;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.*;

public class 
TRTrackerServerProcessorTCP
	extends 	TRTrackerServerProcessor
{
	protected static final int SOCKET_TIMEOUT				= 5000;

	protected static final char		CR			= '\015';
	protected static final char		FF			= '\012';
	protected static final String	NL			= "\015\012";


	protected static final byte[]	HTTP_RESPONSE_START = (
		"HTTP/1.1 200 OK" + NL + 
		"Content-Type: text/html" + NL +
		"Server: " + Constants.AZUREUS_NAME + " " + Constants.AZUREUS_VERSION + NL +
		"Connection: close" + NL +
		"Content-Length: ").getBytes();
		
	protected static final byte[]	HTTP_RESPONSE_END_GZIP 		= (NL + "Content-Encoding: gzip" + NL + NL).getBytes();
	protected static final byte[]	HTTP_RESPONSE_END_NOGZIP 	= (NL + NL).getBytes();
	
	protected TRTrackerServerTCP	server;
	protected Socket				socket;
	

	protected
	TRTrackerServerProcessorTCP(
		TRTrackerServerTCP		_server,
		Socket					_socket )
	{
		server	= _server;
		socket	= _socket;
	}
	
	public void
	run()
	{
		try{
	
			setTaskState( "entry" );
			
			try{
												
				socket.setSoTimeout( SOCKET_TIMEOUT );
										
			}catch ( SocketException e ){
													
				// e.printStackTrace();
			}
										
			String	header_plus = "";
			
			setTaskState( "reading header" );

			try{
										
				InputStream	is = socket.getInputStream();
				
				byte[]	buffer = new byte[1024];
				
				while( header_plus.length()< 4096 ){
						
					int	len = is.read(buffer);
						
					if ( len == -1 ){
					
						break;
					}
									
					header_plus += new String( buffer, 0, len, Constants.BYTE_ENCODING );
									
					if ( 	header_plus.endsWith(NL+NL) ||
							header_plus.indexOf( NL+NL ) != -1 ){
						
						break;
					}
				}
		
				if ( LGLogger.isLoggingOn()){
					
					String	log_str = header_plus;
					
					int	pos = log_str.indexOf( NL );
					
					if ( pos != -1 ){
						
						log_str = log_str.substring(0,pos);
					}
					
					LGLogger.log(0, 0, LGLogger.INFORMATION, "Tracker Server: received header '" + log_str + "'" );
				}				
					
				// System.out.println( "got header:" + header_plus );
				
				InputStream	post_is 	= null;
				File		post_file	= null;
				
				String	actual_header;
				String	lowercase_header;
				
				boolean	head	= false;
				
				if ( header_plus.startsWith( "GET " )){
				
					actual_header		= header_plus;
					lowercase_header	= actual_header.toLowerCase();
	
				}else if ( header_plus.startsWith( "HEAD " )){
						
					actual_header		= header_plus;
					lowercase_header	= actual_header.toLowerCase();			

					head	= true;
					
				}else if ( header_plus.startsWith( "POST ")){
					
					setTaskState( "reading content" );

					int	header_end = header_plus.indexOf(NL+NL);
					
					if ( header_end == -1 ){
					
						throw( new TRTrackerServerException( "header truncated" ));
					}
					
					actual_header 		= header_plus.substring(0,header_end+4);
					lowercase_header	= actual_header.toLowerCase();
					
					int	cl_start = lowercase_header.indexOf("content-length:");
					
					if ( cl_start == -1 ){
						
						throw( new TRTrackerServerException( "header Content-Length start missing" ));
					}
					
					int	cl_end = actual_header.indexOf( NL, cl_start );
					
					if ( cl_end == -1 ){
						
						throw( new TRTrackerServerException( "header Content-Length end missing" ));
					}
					
					int	content_length = Integer.parseInt( actual_header.substring(cl_start+15,cl_end ).trim());
				
					ByteArrayOutputStream	baos		= null;
					FileOutputStream		fos			= null;
				
					OutputStream	data_os;
					
					if ( content_length <= 256*1024 ){
						
						baos = new ByteArrayOutputStream();
					
						data_os	= baos;
						
					}else{
						
						post_file	= File.createTempFile( "AZU", null );
						
						post_file.deleteOnExit();
						
						fos	= new FileOutputStream( post_file );
						
						data_os	= fos;
					}
					
						// if we have X<NL><NL>Y get Y
					
					int	rem = header_plus.length() - (header_end+4);
					
					if ( rem > 0 ){
						
						content_length	-= rem;
						
						data_os.write( header_plus.substring(header_plus.length()-rem).getBytes( Constants.BYTE_ENCODING ));
					}
					
					while( content_length > 0 ){
						
						int	len = is.read( buffer );
						
						if ( len < 0 ){
							
							throw( new TRTrackerServerException( "premature end of input stream" ));
						}
						
						data_os.write( buffer, 0, len );
						
						content_length -= len;
					}
					
					if ( baos != null ){
						
						post_is = new ByteArrayInputStream(baos.toByteArray());
						
					}else{
						
						fos.close();
						
						post_is = new BufferedInputStream( new FileInputStream( post_file ), 256*1024 );
					}
					
					// System.out.println( "TRTrackerServerProcessorTCP: request data = " + baos.size());
				}else{
					
					throw( new TRTrackerServerException( "header doesn't start with GET or POST ('" + (header_plus.length()>256?header_plus.substring(0,256):header_plus)+"')" ));
				}
				
				setTaskState( "processing request" );

				try{
					if ( post_is == null ){
						
							// set up a default input stream 
						
						post_is = new ByteArrayInputStream(new byte[0]);
					}
					
					String	url = actual_header.substring(4).trim();
					
					int	pos = url.indexOf( " " );
					
					if ( pos == -1 ){
						
						throw( new TRTrackerServerException( "header doesn't have space in right place" ));
					}
									
					url = url.substring(0,pos);
					
					if ( head ){
						
						ByteArrayOutputStream	head_response = new ByteArrayOutputStream(4096);
						
						processRequest( actual_header,
								lowercase_header,
								url, 
								socket.getInetAddress().getHostAddress(),
								post_is,
								head_response );
						
						byte[]	head_data = head_response.toByteArray();
						
						int	header_length = head_data.length;
						
						for (int i=3;i<head_data.length;i++){
							
							if ( 	head_data[i-3] 	== CR &&
									head_data[i-2] 	== FF &&
									head_data[i-1] 	== CR &&
									head_data[i]	== FF ){
								
								header_length = i+1;
						
								break;
							}
						}
												
						setTaskState( "writing head response" );

						socket.getOutputStream().write( head_data, 0, header_length );
						
						socket.getOutputStream().flush();
						
					}else{
					
						processRequest( actual_header,
										lowercase_header,
										url, 
										socket.getInetAddress().getHostAddress(),
										post_is,
										socket.getOutputStream() );
					}
				}finally{
					
					if ( post_is != null ){
						
						post_is.close();
					}
					
					if ( post_file != null ){
						
						post_file.delete();
					}
				}
			}catch( SocketTimeoutException e ){
				
				// System.out.println( "TRTrackerServerProcessor: timeout reading header, got '" + header + "'");
				// ignore it
							
			}catch( Throwable e ){
				
				 // e.printStackTrace();
			}
	
		}finally{
					
			setTaskState( "final socket close" );

			try{
				socket.close();
																							
			}catch( Throwable e ){
													
				// e.printStackTrace();
			}
		}
	}
	
	public void
	interruptTask()
	{
		try{
			socket.close();
																						
		}catch( Throwable e ){
												
			// e.printStackTrace();
		}
	}

	protected void
	processRequest(
		String			input_header,
		String			lowercase_input_header,
		String			url_path,
		String			client_ip_address,
		InputStream		is,
		OutputStream	os )
		
		throws IOException
	{
		String	str = url_path;
		
		try{
			Map	root = null;
				
			TRTrackerServerTorrentImpl	specific_torrent	= null;
			
			boolean	gzip_reply = false;
			
			try{
				int	request_type;
			
				if ( str.startsWith( "/announce?" )){
					
					request_type	= TRTrackerServerRequest.RT_ANNOUNCE;
					
					str = str.substring(10);
					
				}else if ( str.startsWith( "/scrape?" )){
					
					request_type	= TRTrackerServerRequest.RT_SCRAPE;
					
					str = str.substring(8);
				
				}else if ( str.equals( "/scrape" )){
					
					request_type	= TRTrackerServerRequest.RT_FULL_SCRAPE;
					
					str = "";
				
				}else{
					
					setTaskState( "external request" );

						// check non-tracker authentication
						
					if ( !doAuthentication( url_path, input_header, os, false )){
					
						return;
					}
					
					if ( handleExternalRequest( client_ip_address, str, input_header, is, os )){
					
						return;
					}
					
					os.write( ("HTTP/1.1 404 Not Found\r\n\r\n").getBytes() );
					
					os.flush();

					return; // throw( new Exception( "Unsupported Request Type"));
				}
				
					// OK, here its an announce, scrape or full scrape
				
					// check tracker authentication
					
				if ( !doAuthentication( url_path, input_header, os, true )){
					
					return;
				}
				
				
				int	enc_pos = lowercase_input_header.indexOf( "accept-encoding:");
				
				if ( enc_pos != -1 ){
					
					int	e_pos = input_header.indexOf( NL, enc_pos );
					
					if ( e_pos != -1 ){
						
						gzip_reply = input_header.substring(enc_pos+16,e_pos).toLowerCase().indexOf("gzip") != -1;
					}
				}
				
				setTaskState( "decoding announce/scrape" );

				int	pos = 0;
					
				String		hash_str	= null;
				HashWrapper	peer_id		= null;
				int			port		= 0;
				String		event		= null;
					
				long		uploaded		= 0;
				long		downloaded		= 0;
				long		left			= 0;
				int			num_peers		= 0;
				int			num_want		= -1;
				boolean		no_peer_id		= false;
				boolean		compact			= false;
				String		key				= null;
				
				while(pos < str.length()){
						
					int	p1 = str.indexOf( '&', pos );
						
					String	token;
						
					if ( p1 == -1 ){
							
						token = str.substring( pos );
							
					}else{
							
						token = str.substring( pos, p1 );
							
						pos = p1+1;
					}
					
					int	p2 = token.indexOf('=');
						
					if ( p2 == -1 ){
							
						throw( new Exception( "format invalid" ));
					}
						
					String	lhs = token.substring( 0, p2 ).toLowerCase();
					String	rhs = URLDecoder.decode(token.substring( p2+1 ), Constants.BYTE_ENCODING );
						
					// System.out.println( "param:" + lhs + " = " + rhs );
						
					if ( lhs.equals( "info_hash" )){
							
						hash_str	= rhs;
							
					}else if ( lhs.equals( "peer_id" )){
						
						peer_id	= new HashWrapper(rhs.getBytes(Constants.BYTE_ENCODING));
						
					}else if ( lhs.equals( "no_peer_id" )){
						
						no_peer_id = rhs.equals("1");
						
					}else if ( lhs.equals( "compact" )){
						
						if ( server.isCompactEnabled()){
							
							compact = rhs.equals("1");
						}
					}else if ( lhs.equals( "key" )){
						
						if ( server.isKeyEnabled()){
							
							key = rhs;
						}
						
					}else if ( lhs.equals( "port" )){
							
						port = Integer.parseInt( rhs );
						
					}else if ( lhs.equals( "event" )){
							
						event = rhs;
							
					}else if ( lhs.equals( "ip" )){
							
						client_ip_address = rhs;
						
					}else if ( lhs.equals( "uploaded" )){
							
						uploaded = Long.parseLong( rhs );
						
					}else if ( lhs.equals( "downloaded" )){
							
						downloaded = Long.parseLong( rhs );
						
					}else if ( lhs.equals( "left" )){
							
						left = Long.parseLong( rhs );
						
					}else if ( lhs.equals( "num_peers" )){
						
						num_peers = Integer.parseInt( rhs );
						
					}else if ( lhs.equals( "numwant" )){
						
						num_want = Integer.parseInt( rhs );
					}
						
					if ( p1 == -1 ){
							
						break;
					}
				}
				
				byte[]	hash_bytes = null;
				
				if ( hash_str != null ){
					
					hash_bytes = hash_str.getBytes(Constants.BYTE_ENCODING);
				}
				
				Map[]						root_out = new Map[1];
				TRTrackerServerPeerImpl[]	peer_out = new TRTrackerServerPeerImpl[1];
				
				specific_torrent = 
						processTrackerRequest( 
							server, root_out, peer_out,
							request_type,
							hash_bytes,
							peer_id, no_peer_id, compact, key, 
							event,
							port,
							client_ip_address,
							downloaded, uploaded, left,
							num_peers, num_want );
				
				root	= root_out[0];
				
					// only post-process if this isn't a cached entry
				
				if ( root.get( "_data" ) == null ){
	
					server.postProcess( peer_out[0], specific_torrent, request_type, root );
				}
				
			}catch( Exception e ){
				
				String	message = e.getMessage();
				
				// e.printStackTrace();
				
				if ( message == null || message.length() == 0 ){

					// e.printStackTrace();
								
					message = e.toString();
				}
					
				root	= new HashMap();
				
				root.put( "failure reason", message );
			}
		
			setTaskState( "writing response" );

				// cache both plain and gzip encoded data for possible reuse
			
			byte[] data 		= (byte[])root.get( "_data" );
					
			if ( data == null ){
				
				data = BEncoder.encode( root );
				
				root.put( "_data", data );
			}
						
			if ( gzip_reply ){

				byte[]	gzip_data = (byte[])root.get( "_gzipdata");
				
				if ( gzip_data == null ){
						
					ByteArrayOutputStream tos = new ByteArrayOutputStream(data.length);
					
					GZIPOutputStream gos = new GZIPOutputStream( tos );
					
					gos.write( data );
					
					gos.close();
					
					gzip_data = tos.toByteArray();
					
					root.put( "_gzipdata", gzip_data );
				}
				
				data	= gzip_data;
			}
						
				// System.out.println( "TRTrackerServerProcessor::reply: sending " + new String(data));
						
				// write the response
			
			setTaskState( "writing header" );

			os.write( HTTP_RESPONSE_START );
			
			byte[]	length_bytes = String.valueOf(data.length).getBytes();
			
			os.write( length_bytes );

			int	header_len = HTTP_RESPONSE_START.length + length_bytes.length;
			
			setTaskState( "writing content" );

			if ( gzip_reply ){
				
				os.write( HTTP_RESPONSE_END_GZIP );
			
				header_len += HTTP_RESPONSE_END_GZIP.length; 
			}else{
				
				os.write( HTTP_RESPONSE_END_NOGZIP );

				header_len += HTTP_RESPONSE_END_NOGZIP.length; 
			}
					
			os.write( data );
			
			server.updateStats( specific_torrent, input_header.length(), header_len+data.length );
							
		}finally{
			
			setTaskState( "final os flush" );

			os.flush();
		}
	}
	
	protected boolean
	doAuthentication(
		String			url_path,
		String			header,
		OutputStream	os,
		boolean			tracker )
		
		throws IOException
	{
		// System.out.println( "doAuth: " + server.isTrackerPasswordEnabled() + "/" + server.isWebPasswordEnabled());
		
		if (	( tracker && server.isTrackerPasswordEnabled()) ||
				( (!tracker) && server.isWebPasswordEnabled())){
			
			int	x = header.indexOf( "Authorization:" );
			
			if ( x == -1 ){
				
					// auth missing. however, if we have external auth defined
					// and external auth is happy with junk then allow it through
				
				if ( server.hasExternalAuthorisation()){
					
					try{
						String	resource_str = 
							( server.isSSL()?"https":"http" ) + "://" +
								server.getHost() + ":" + server.getPort() + url_path;
						
						URL	resource = new URL( resource_str );
					
						if ( server.performExternalAuthorisation( resource, "", "" )){
							
							return( true );
						}
					}catch( MalformedURLException e ){
						
						e.printStackTrace();
					}
				}
			}else{
															
					//			Authorization: Basic dG9tY2F0OnRvbWNhdA==
		
				int	p1 = header.indexOf(' ', x );
				int p2 = header.indexOf(' ', p1+1 );
				
				String	body = header.substring( p2, header.indexOf( '\r', p2 )).trim();
				
				String decoded=new String(new BASE64Decoder().decodeBuffer(body));

					// username:password
									
				int	cp = decoded.indexOf(':');
				
				String	user = decoded.substring(0,cp);
				String  pw	 = decoded.substring(cp+1);
				
				boolean	auth_failed	= false;
				
				if ( server.hasExternalAuthorisation()){
					
					try{
						String	resource_str = 
							( server.isSSL()?"https":"http" ) + "://" +
								server.getHost() + ":" + server.getPort() + url_path;
						
						URL	resource = new URL( resource_str );
					
						if ( server.performExternalAuthorisation( resource, user, pw )){
							
							return( true );
						}
					}catch( MalformedURLException e ){
						
						e.printStackTrace();
					}
					
					auth_failed	= true;
				}
				
				if ( server.hasInternalAuthorisation() && !auth_failed ){
					
					try{
				
						SHA1Hasher hasher = new SHA1Hasher();
						
						byte[] password = pw.getBytes();
						
						byte[] encoded;
						
						if( password.length > 0){
						
							encoded = hasher.calculateHash(password);
							
						}else{
							
							encoded = new byte[0];
						}
						
						if ( user.equals( "<internal>")){
							
							byte[] internal_pw = new BASE64Decoder().decodeBuffer(pw);
	
							if ( Arrays.equals( internal_pw, server.getPassword())){
								
								return( true );
							}
						}else if ( 	user.equalsIgnoreCase(server.getUsername()) &&
									Arrays.equals(encoded, server.getPassword())){
							 	
							 return( true );			 	
						}
					}catch( Exception e ){
						
						e.printStackTrace();
					}
				}
			}
			
			os.write( ("HTTP/1.1 401 BAD\r\nWWW-Authenticate: Basic realm=\"" + server.getName() + "\"\r\n\r\nAccess Denied\r\n").getBytes() );
			
			os.flush();
				
			return( false );

		}else{
		
			return( true );
		}
	}
		
	protected boolean
	handleExternalRequest(
		String			client_address,
		String			url,
		String			header,
		InputStream		is,
		OutputStream	os )
		
		throws IOException
	{
		return( server.handleExternalRequest(client_address,url,header, is, os));
	}
}
