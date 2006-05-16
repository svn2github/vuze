/*
 * Created on 03-Mar-2005
 * Created by Paul Gardner
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
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

package com.aelitis.net.magneturi.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.*;
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
  private static final LogIDs LOGID = LogIDs.NET;
		// see http://magnet-uri.sourceforge.net/magnet-draft-overview.txt
	
	private static MagnetURIHandlerImpl		singleton;
	
	private static AEMonitor				class_mon = new AEMonitor( "MagnetURLHandler:class" );
	
	private static final int				DOWNLOAD_TIMEOUT	= 3*60*1000;
	
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
			if (Logger.isEnabled())
				Logger.log(new LogEvent(LOGID, LogEvent.LT_ERROR,
						"MagnetURI: no free sockets, giving up"));
			
		}else{
			if (Logger.isEnabled())
				Logger.log(new LogEvent(LOGID, "MagnetURI: bound on "
						+ socket.getLocalPort()));
			
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
											        	
											        		Logger.log(new LogEvent(LOGID,
											        					"MagnetURIHandler: processing '" + line + "'"));
			
											        		line = line.substring(4);
											        		
											        		int	pos = line.lastIndexOf(' ');
											        		
											        		line = line.substring( 0, pos );
											        		
											        		close_socket = process( line, sck.getOutputStream() );
											        		
											        	}else{
											        		
															Logger.log(new LogEvent(LOGID, LogEvent.LT_WARNING,
																		"MagnetURIHandler: invalid command - '" + line
																			+ "'"));
											        	}
										        	}else{
										        		
											       		Logger.log(new LogEvent(LOGID, LogEvent.LT_WARNING,
											       				"MagnetURIHandler: connect from "
											       				+ "'" + address + "': no data read"));
									        		
										        	}
											   }else{
												   
											      	Logger.log(new LogEvent(LOGID, LogEvent.LT_WARNING,
											      				"MagnetURIHandler: connect from "
											       				+ "invalid address '" + address + "'"));
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
									if (Logger.isEnabled())
										Logger.log(new LogEvent(LOGID,
										"MagnetURIHandler: bailing out, too many socket errors"));
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
		
		Map		params 			= new HashMap();
		List 	source_params	= new ArrayList();
		
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
						String	lhs = arg.substring( 0, pos ).trim();
						
						String	rhs = URLDecoder.decode( arg.substring( pos+1 ).trim(), Constants.DEFAULT_ENCODING);

						params.put( lhs, rhs );
						
						if ( lhs.equalsIgnoreCase( "xsource" )){
							
							source_params.add( rhs );
						}
					}catch( UnsupportedEncodingException e ){
						
						Debug.printStackTrace( e );
					}
				}
			}
		}
		

		if ( get.startsWith( "/magnet10/badge.img" )){
			
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

			String	fail_reason = "";
			
			boolean	ok = false;
			
			String urn = (String)params.get( "xt" );

			if ( urn == null ){
				
				fail_reason	= "xt missing";
				
			}else{
					
				try{
				
					URL	magnet = new URL( "magnet:?xt=" + urn );
				
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
			}
			
			if ( ok ){
				
				if ( "image".equalsIgnoreCase((String)params.get( "result" ))){
					
					for (int i=0;i<listeners.size();i++){

						byte[]	data = ((MagnetURIHandlerListener)listeners.get(i)).badge();
					
						if ( data != null ){
							
							writeReply( os, "image/gif", data );
							
							return( true );
						}
					}
				}
					
				writeReply( os, "text/plain", "Download initiated" );
				
			}else{
				
				writeReply( os, "text/plain", "Download initiation failed: " + fail_reason );
			}
			
		}else if ( get.startsWith( "/download/" )){
			
			String urn = (String)params.get( "xt" );
			
			if ( urn == null || !( urn.startsWith( "urn:sha1:") || urn.startsWith( "urn:btih:"))){
				if (Logger.isEnabled())
					Logger.log(new LogEvent(LOGID, LogEvent.LT_WARNING,
							"MagnetURIHandler: " + "invalid command - '" + get + "'"));
				
				return( true );
			}
			
			final PrintWriter	pw = new PrintWriter( new OutputStreamWriter( os, "UTF-8" ));

			try{			
				pw.print( "HTTP/1.0 200 OK" + NL ); 

				pw.flush();
								
				String	base_32 = urn.substring(9);
				
				List	sources = new ArrayList();
				
				for (int i=0;i<source_params.size();i++){
					
					String	source = (String)source_params.get(i);
					
					int	p = source.indexOf(':');
					
					if ( p != -1 ){
						
						try{
							InetSocketAddress	sa = new InetSocketAddress( source.substring(0,p), Integer.parseInt( source.substring(p+1)));
							
							sources.add( sa );
							
						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
						}
					}
				}
					
				InetSocketAddress[]	s = new InetSocketAddress[ sources.size()];
				
				sources.toArray( s );
				
				if (Logger.isEnabled())
					Logger.log(new LogEvent(LOGID, "MagnetURIHandler: download of '"
							+ base_32 + "' starts (initial sources=" + s.length + ")"));

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
									pw.print( "X-Report: " + getMessageText( "torrent_size", String.valueOf( size )) + NL );
									
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
									pw.print( "X-Report: " + getMessageText( "percent", String.valueOf(percent)) + NL );
									
									pw.flush();
								}
							},
							sha1, 
							s,
							DOWNLOAD_TIMEOUT );
					
					if ( data != null ){
						
						break;
					}
				}
				
				if (Logger.isEnabled())
					Logger.log(new LogEvent(LOGID, "MagnetURIHandler: download of '"
							+ base_32
							+ "' completes, data "
							+ (data == null ? "not found"
									: ("found, length = " + data.length))));

				if ( data != null ){
					
					pw.print( "Content-Length: " + data.length + NL + NL );
					
					pw.flush();
					
					os.write( data );
					
					os.flush();
					
				}else{
					
						// HACK: don't change the "error:" message below, it is used by TorrentDownloader to detect this
						// condition
					
					pw.print( "X-Report: error: " + getMessageText( "no_sources" ) + NL );
					
					pw.flush();
					
						// pause on error
					
					return( !params.containsKey( "pause_on_error" ));
				}
			}catch( Throwable e ){
				
					// don't remove the "error:" (see above)
				
				pw.print( "X-Report: error: " + getMessageText( "error", Debug.getNestedExceptionMessage(e)) + NL );
				
				pw.flush();
				
				// Debug.printStackTrace(e);
				
					// pause on error
				
				return( !params.containsKey( "pause_on_error" ));
			}
		}
		
		return( true );
	}
	
	protected String
	getMessageText(
		String	resource )
	{
		return( MessageText.getString( "MagnetURLHandler.report." + resource ));
	}
	
	protected String
	getMessageText(
		String	resource,
		String	param )
	{
		return( MessageText.getString( "MagnetURLHandler.report." + resource, new String[]{ param } ));
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

		pw.print( "HTTP/1.1 200 OK" + NL );
		pw.print( "Cache-Control: no-cache" + NL );
		pw.print( "Pragma: no-cache" + NL );
		pw.print( "Content-Type: " + content_type + NL );
		pw.print( "Content-Length: " + content.length + NL + NL );
		
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
