/*
 * Created on 03-Mar-2005
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
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
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.net.magneturi.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;

import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.net.magneturi.MagnetURIHandler;
import com.aelitis.net.magneturi.MagnetURIHandlerListener;
import com.aelitis.net.magneturi.MagnetURIHandlerProgressListener;

/**
 * @author parg
 *
 */

public class 
MagnetURIHandlerImpl 
	extends MagnetURIHandler
{
		// see http://magnet-uri.sourceforge.net/magnet-draft-overview.txt
	
	private static MagnetURIHandlerImpl		singleton;
	
	private static AEMonitor				class_mon = new AEMonitor( "MagnetURLHandler:class" );
	
	private static final int				DOWNLOAD_TIMEOUT	= 120000;
	
	protected static final String	NL			= "\015\012";

	public static MagnetURIHandler
	getSingleton()
	{
		try{
			class_mon.enter();
		
			if ( singleton == null ){
			
				singleton	= new MagnetURIHandlerImpl();
			}
		
			return( singleton );
			
		}finally{
			
			class_mon.exit();
		}
	}
	
	private int		port;
	
	private List	listeners	= new ArrayList();
	
	protected
	MagnetURIHandlerImpl()
	{
		ServerSocket	socket	= null;
		
		for (int i=45100;i<=45199;i++){
			
			try{
				
			   socket = new ServerSocket(i, 50, InetAddress.getByName("127.0.0.1"));

			   port	= i;
			   
			   break;
			   
			}catch( Throwable e ){
				
			}
		}
		
		if ( socket == null ){
			
			// no free sockets, not much we can do
			
			LGLogger.log( "MagnetURI: no free sockets, giving up");
			
		}else{
			
			LGLogger.log( "MagnetURI: bound on " + socket.getLocalPort());
			
			final ServerSocket	f_socket = socket;
			
			Thread t = 
				new AEThread("MagnetURIHandler")
				{
					public void
					runSupport()
					{
						int	errors 	= 0;
						int	ok		= 0;
						
						while(true){
							
							try{
						
								final Socket sck = f_socket.accept();
								
								ok++;
								
								errors	= 0;
								
								Thread t = 
									new AEThread( "MagnetURIHandler:processor" )
									{
										public void
										runSupport()
										{
											boolean	close_socket	= true;
											
											try{
											        String address = sck.getInetAddress().getHostAddress();
										        
										        if ( address.equals("localhost") || address.equals("127.0.0.1")) {
										        	
										        	BufferedReader br = new BufferedReader(new InputStreamReader(sck.getInputStream(),Constants.DEFAULT_ENCODING));
										        	
										        	String line = br.readLine();
					
										        	if ( line != null ){
										        		
											        	if ( line.toUpperCase().startsWith( "GET " )){
											        	
												        	LGLogger.log("MagentURIHandler: processing '" + line + "'" );
			
											        		line = line.substring(4);
											        		
											        		int	pos = line.lastIndexOf(' ');
											        		
											        		line = line.substring( 0, pos );
											        		
											        		close_socket = process( line, sck.getOutputStream() );
											        		
											        	}else{
											        								        	
												        	LGLogger.log("MagentURIHandler: invalid command - '" + line + "'" );
											        	}
											        }else{
											        	
											        	LGLogger.log("MagentURIHandler: connect from invalid address '" + address + "'" );
											        	
											        }
										        }
											}catch( Throwable e ){
												
												if ( !(e instanceof IOException || e instanceof SocketException )){
													
													Debug.printStackTrace(e);
												}
											}finally{
												
												try{
														// leave client to close socket if not requested
													
													if ( close_socket ){
														
														sck.close();
													}
													
												}catch( Throwable e ){
												}
											}
										}
									};
								
								t.setDaemon( true );
								
								t.start();
								
							}catch( Throwable e ){
								
								Debug.printStackTrace(e);
								
								errors++;
								
								if ( errors > 100 ){
									
									LGLogger.log("MagentURIHandler: bailing out, too many socket errors" );
								}
							}
						}
					}
				};
				
			t.setDaemon( true );
			
			t.start();
		}
	}
	
	protected boolean
	process(
		String			get,
		OutputStream	os )
	
		throws IOException
	{
		//System.out.println( "get = " + get );
		
			// magnet:?xt=urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO5C
		
		Map	params = new HashMap();
	
		int	pos	= get.indexOf( '?' );
		
		if ( pos != -1 ){
					
			StringTokenizer	tok = new StringTokenizer( get.substring( pos+1 ), "&" );
			
			while( tok.hasMoreTokens()){
				
				String	arg = tok.nextToken();
				
				pos	= arg.indexOf( '=' );
				
				if ( pos == -1 ){
					
					params.put( arg.trim(), "" );
					
				}else{
					
					try{
						params.put( arg.substring( 0, pos ).trim(), URLDecoder.decode( arg.substring( pos+1 ).trim(), Constants.DEFAULT_ENCODING));
						
					}catch( UnsupportedEncodingException e ){
						
						Debug.printStackTrace( e );
					}
				}
			}
		}
		

		if ( get.equals( "/magnet10/badge.img" )){
			
			for (int i=0;i<listeners.size();i++){
				
				byte[]	data = ((MagnetURIHandlerListener)listeners.get(i)).badge();
					
				if ( data != null ){
					
					writeReply( os, "image/gif", data );
					
					return( true );
				}
			}
			
			writeNotFound( os );
			
			return( true );
			
		}else if ( get.startsWith( "/magnet10/canHandle.img?" )){

			String urn = (String)params.get( "xt" );

			if ( urn != null && urn.startsWith( "urn:btih:")){
			
				for (int i=0;i<listeners.size();i++){
					
					byte[]	data = ((MagnetURIHandlerListener)listeners.get(i)).badge();
						
					if ( data != null ){
						
						writeReply( os, "image/gif", data );
						
						return( true );
					}
				}
			}
			
			writeNotFound( os );
			
			return( true );
			
		}else if ( 	get.startsWith( "/magnet10/options.js?" ) ||
					get.startsWith( "/magnet10/default.js?" )){
		
			String	resp = "";
			
			resp += getJS( "magnetOptionsPreamble" );
			
			resp += getJSS( "<a href=\\\"http://127.0.0.1:\"+(45100+magnetCurrentSlot)+\"/select/?\"+magnetQueryString+\"\\\" target=\\\"_blank\\\">" );
			resp += getJSS( "<img src=\\\"http://127.0.0.1:\"+(45100+magnetCurrentSlot)+\"/magnet10/badge.img\\\">" );
			resp += getJSS( "Download with Azureus" );
			resp += getJSS( "</a>" );

			resp += getJS( "magnetOptionsPostamble" );
			
			resp += "magnetOptionsPollSuccesses++";
			
			writeReply( os, "application/x-javascript", resp );
		
			return( true );
			
		}else if ( get.startsWith( "/magnet10/pause" )){
			
			try{
				Thread.sleep( 250 );
				
			}catch( Throwable e ){
				
			}
			writeNotFound( os );
			
			return( true );
			
		}else if ( get.startsWith( "/select/" )){

			int	query	= get.indexOf( '?' );

			boolean	ok = false;
			
			String	fail_reason = "";
			
			try{
			
				URL	magnet = new URL( "magnet:" + get.substring( query ));;
			
				for (int i=0;i<listeners.size();i++){
					
					if (((MagnetURIHandlerListener)listeners.get(i)).download( magnet )){
						
						ok = true;
						
						break;
					}
				}
				
				if ( !ok ){
					
					fail_reason = "No listeners accepted the operation";
				}
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
				
				fail_reason	= Debug.getNestedExceptionMessage(e);
			}
			
			if ( ok ){
				
				writeReply( os, "text/plain", "Download initiated" );
				
			}else{
				
				writeReply( os, "text/plain", "Download initiation failed: " + fail_reason );
			}
			
		}else if ( get.startsWith( "/download/" )){
			
			String urn = (String)params.get( "xt" );
			
			if ( urn == null || !( urn.startsWith( "urn:sha1:") || urn.startsWith( "urn:btih:"))){
				
				LGLogger.log("MagentURIHandler: invalid command - '" + get + "'" );
				
				return( true );
			}
			
			final PrintWriter	pw = new PrintWriter( new OutputStreamWriter( os ));

			try{
								
				pw.print( "HTTP/1.0 200 OK" + NL ); 

				pw.flush();
				
				String	base_32 = urn.substring(9);
					
	        	LGLogger.log("MagentURIHandler: download of '" + base_32 + "' starts" );

				byte[] sha1 = Base32.decode( base_32 );
				
				byte[]	data = null;
				
				
				for (int i=0;i<listeners.size();i++){
				
					data = ((MagnetURIHandlerListener)listeners.get(i)).download(
							new MagnetURIHandlerProgressListener()
							{
								public void
								reportSize(
									long	size )
								{
									pw.print( "X-Report: torrent size: " + size + NL );
									
									pw.flush();
								}
								
								public void
								reportActivity(
									String	str )
								{
									pw.print( "X-Report: " + str + NL );
									
									pw.flush();
								}
								
								public void
								reportCompleteness(
									int		percent )
								{
									pw.print( "X-Report: completed: " + percent + "%" + NL );
									
									pw.flush();
								}
							},
							sha1, 
							DOWNLOAD_TIMEOUT );
					
					if ( data != null ){
						
						break;
					}
				}
				
	        	LGLogger.log("MagentURIHandler: download of '" + base_32 + "' completes, data " + (data==null?"not found":("found, length = "+data.length )));

				if ( data != null ){
					
					pw.print( "Content-Length: " + data.length + NL + NL );
					
					pw.flush();
					
					os.write( data );
					
					os.flush();
					
				}else{
					
						// HACK: don't change this message below, it is used by TorrentDownloader to detect this
						// condition
					
					pw.print( "X-Report: no sources found for torrent" + NL );
					
					pw.flush();
					
						// pause on error
					
					return( !params.containsKey( "pause_on_error" ));
				}
			}catch( Throwable e ){
				
				pw.print( "X-Report: Error " + Debug.getNestedExceptionMessage(e) + NL );
				
				pw.flush();
				
				Debug.printStackTrace(e);
				
					// pause on error
				
				return( !params.containsKey( "pause_on_error" ));
			}
		}
		
		return( true );
	}
	
	protected String
	getJS(
		String	s )
	{
		return( "document.write(" + s + ");" + NL );
	}
	
	protected String
	getJSS(
		String	s )
	{
		return( "document.write(\"" + s + "\");" + NL );
	}
	
	protected void
	writeReply(
		OutputStream		os,
		String				content_type,
		String				content )
	
		throws IOException
	{
		writeReply( os, content_type, content.getBytes());
	}
	
	protected void
	writeReply(
		OutputStream		os,
		String				content_type,
		byte[]				content )
	
		throws IOException
	{
		PrintWriter	pw = new PrintWriter( new OutputStreamWriter( os ));

		pw.print( "HTTP/1.0 200 OK" + NL );
		pw.print( "Content-type: " + content_type + NL );
		pw.print( "Content-length: " + content.length + NL );
		
		pw.print( NL );

		pw.flush();
		
		os.write( content );

	}
	
	protected void
	writeNotFound(
		OutputStream		os )
	
		throws IOException
	{
		PrintWriter	pw = new PrintWriter( new OutputStreamWriter( os ));

		pw.print( "HTTP/1.0 404 Not Found" + NL + NL );

		pw.flush();
	}
	
	public int
	getPort()
	{
		return( port );
	}
	
	public void
	addListener(
		MagnetURIHandlerListener	l )
	{
		listeners.add( l );
	}
	
	public void
	removeListener(
		MagnetURIHandlerListener	l )
	{
		listeners.remove( l );
	}
	
	public static void
	main(
		String[]	args )
	{
		new MagnetURIHandlerImpl();
		
		try{
			Thread.sleep(1000000);
		}catch( Throwable e ){
			
		}
	}
}
