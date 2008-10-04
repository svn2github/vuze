/*
 * Created on Oct 2, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.core.util.http;

import java.net.*;
import java.util.*;
import java.io.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.gudy.azureus2.core3.security.SESecurityManager;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;


public class 
HTTPSniffingProxy 
{
	public static final int MAX_PROCESSORS = 32;
	
	public static final int	HTTP_PORT		= 80;
	public static final int	HTTPS_PORT		= 443;
	
	public static final int	CONNECT_TIMEOUT		= 30*1000;
	public static final int READ_TIMEOUT		= 30*1000;

	private String	delegate_to;
	private boolean	delegate_is_https;
	
	private int		port;
	
	private ServerSocket	server_socket;	
	
	private List			processors = new ArrayList();
	
	private volatile boolean		destroyed;
	
	public
	HTTPSniffingProxy(
		String		_delegate_to,
		boolean		_delegate_is_https )
	
		throws Exception
	{
		delegate_to			= _delegate_to;
		delegate_is_https	= _delegate_is_https;
		
		server_socket = new ServerSocket();
		
		server_socket.setReuseAddress( true ); 
             
		server_socket.bind( new InetSocketAddress( "127.0.0.1", 0 ));
        
        port = server_socket.getLocalPort();
        
        new AEThread2( 
        	"HTTPSniffingProxy:" + delegate_to + "/" + delegate_is_https + "/" + port, 
        	true )
        	{
        		public void 
        		run() 
        		{
        			try{
        				while( !destroyed ){
    
        					Socket	socket = server_socket.accept();
        					   
        					socket.setSoTimeout( READ_TIMEOUT );
        					
        					synchronized( HTTPSniffingProxy.this ){
        						
        						if ( processors.size() >= MAX_PROCESSORS ){
        							
        							try{
        								Debug.out( "Too many processors" );
        								
        								socket.close();
        								
        							}catch( Throwable e ){
        							}
        						}else{
        							
        							processor proc = new processor( socket );
        							
        							processors.add( proc );
        						}
        					}
        				}
        			}catch( Throwable e ){
        				
        				if ( !destroyed ){
        					
        					Debug.printStackTrace( e );
        				}
        			}
        		}
        	}.start();	
	}
	
	public int
	getPort()
	{
		return( port );
	}
	
	public void
	destroy()
	{
		List	to_destroy;
		
		synchronized( this ){
			
			if ( destroyed ){
				
				return;
			}
			
			destroyed = true;
				
			to_destroy = new ArrayList( processors );
			
			processors.clear();
			
			try{				
				server_socket.close();

			}catch( Throwable e ){	
			}
		}
		
		for (int i=0;i<to_destroy.size();i++){
			
			((processor)to_destroy.get(i)).destroy();
		}
	}
	
	protected class
	processor
	{
		private static final String	NL = "\r\n";
		
		private Socket		socket_in;
		private Socket		socket_out;
		
		private boolean	destroyed;
		
		protected
		processor(
			Socket		_socket )
		{
			socket_in	= _socket;
			
			new AEThread2( "HTTPSniffingProxy:proc:1", true )
			{
				public void 
				run() 
				{
					sniff();
				}
			}.start();
		}
		
		protected void
		sniff()
		{
			try{
				InputStream is = socket_in.getInputStream();
				
				String request_header = readHeader( is );
								
				connectToDelegate( delegate_to, delegate_is_https );
				
				process( request_header );
				
			}catch( Throwable e ){
				
				e.printStackTrace();
				
				destroy();
			}
		}
		
		protected void
		connectToDelegate(
			String	target_ip,
			boolean	target_ssl )
		
			throws IOException
		{
			try{
				if ( target_ssl ){
					
					TrustManager[] trustAllCerts = new TrustManager[]{
							new X509TrustManager() {
								public java.security.cert.X509Certificate[] getAcceptedIssuers() {
									return null;
								}
								public void checkClientTrusted(
										java.security.cert.X509Certificate[] certs, String authType) {
								}
								public void checkServerTrusted(
										java.security.cert.X509Certificate[] certs, String authType) {
								}
							}
						};
				
					SSLContext sc = SSLContext.getInstance("SSL");
				
					sc.init(null, trustAllCerts, new java.security.SecureRandom());
				
					SSLSocketFactory factory = sc.getSocketFactory();

					try{
						socket_out = factory.createSocket();
						
						socket_out.connect( new InetSocketAddress( target_ip, HTTPS_PORT ), CONNECT_TIMEOUT );
					
					}catch( SSLException ssl_excep ){
												
						factory = SESecurityManager.installServerCertificates( "AZ-sniffer:" + target_ip + ":" + port, target_ip, HTTPS_PORT );
						
						socket_out = factory.createSocket();
						
						socket_out.connect( new InetSocketAddress( target_ip, HTTPS_PORT ), 30*1000 );					
					}
				}else{
					
					socket_out = new Socket();
					
					socket_out.connect( new InetSocketAddress( target_ip, HTTP_PORT ), CONNECT_TIMEOUT );
				}
			}catch( Throwable e ){
				
				if ( e instanceof IOException ){
					
					throw((IOException)e );
				}
				
				throw( new IOException( e.toString()));
				
			}finally{
				
				if ( socket_out != null ){
										
					synchronized( this ){
						
						if ( destroyed ){

							try{
								socket_out.close();
								
							}catch( Throwable e ){
								
							}finally{
								
								socket_out = null;
							}
							
							throw( new IOException( "destroyed" ));
						}
					}
				}
			}
		}
		
		protected void
		process(
			String		request_header )
		
			throws IOException
		{
			final OutputStream target_os = socket_out.getOutputStream();
			
			String[]	request_lines = splitHeader( request_header );
			
			for (int i=0;i<request_lines.length;i++){
				
				String	line_out	= request_lines[i];

				String	line_in 	= line_out.trim().toLowerCase();
				
				String[] bits = line_in.split(":");
				
				if ( bits.length >= 2 ){
					
					String	lhs = bits[0].trim();
					
					if ( lhs.equals( "host" )){
						
						line_out = "Host: " + delegate_to;
						
					}else if ( lhs.equals( "connection" )){
						
						line_out = "Connection: close";
						
					}else if ( lhs.equals( "referer" )){
						
						String page = line_out.substring( line_out.indexOf( ':' )+1).trim();
						
						page = page.substring( page.indexOf( "://") + 3);
						
						page = page.substring( page.indexOf( '/' ));
						
						line_out = "Referer: http" + (delegate_is_https?"s":"") + "://" + delegate_to + page;
					}
				}
				
				if ( line_out != null ){
										
					System.out.println( "-> " + line_out );
					
					target_os.write((line_out+NL).getBytes());
				}
			}
			
			target_os.write( NL.getBytes());
			
			target_os.flush();
			
			new AEThread2( "HTTPSniffingProxy:proc:2", true )
			{
				public void 
				run() 
				{
					try{
						InputStream	source_is = socket_in.getInputStream();
						
						byte[]	buffer = new byte[32000];
	
						while( true ){
												
							int	len = source_is.read( buffer );
								
							if ( len <= 0 ){
									
								break;
							}
							
							target_os.write( buffer, 0, len );
						}
					}catch( Throwable e ){
					}
				}
			}.start();
			
			InputStream	target_is = socket_out.getInputStream();
			
			OutputStream	source_os = socket_in.getOutputStream();
			
			String	reply_header = readHeader( target_is );
			
			String[]	reply_lines = splitHeader( reply_header );
			
				// TODO: handled various 'moved' replies
			
			for (int i=0;i<reply_lines.length;i++){
				
				String	line_out	= reply_lines[i];

				String	line_in 	= line_out.trim().toLowerCase();
				
				String[] bits = line_in.split(":");
				
				if ( bits.length >= 2 ){
					
					String	lhs = bits[0].trim();
					
					if ( lhs.equals( "set-cookie" )){
						
						String	cookie = line_out.substring( line_out.indexOf( ':' )+1 );
						
						String[]	x = cookie.split( ";" );
						
						String	modified_cookie = "";
						
						for (int j=0;j<x.length;j++){
							
							String entry = x[j];
							
							if ( entry.equalsIgnoreCase( "httponly" )){
								
							}else if ( entry.equalsIgnoreCase( "secure" )){
								
							}else{
								
								modified_cookie += (modified_cookie.length()==0?"":"; ") + entry;
							}
						}
						
						line_out = "Set-Cookie: " + modified_cookie;
						
					}else if ( lhs.equals( "connection" )){
						
						line_out = "Connection: close";
					}
				}
				
				if ( line_out != null ){
					
					System.out.println( "<- " + line_out );
					
					source_os.write((line_out+NL).getBytes());
				}
			}
			
			source_os.write( NL.getBytes());
			
			source_os.flush();
			
			byte[]	buffer = new byte[32000];

				// TODO: url rewriting - have to gunzip + rezip if zipped and update content-length...
			
			while( true ){
								
				int	len = target_is.read( buffer );
				
				if ( len <= 0 ){
					
					break;
				}
				
				source_os.write( buffer, 0, len );
			}
		}
		
		protected String
		readHeader(
			InputStream		is )
		
			throws IOException
		{
			String	header = "";
			
			byte[]	buffer = new byte[1];
			
			while( true ){
			
				if ( is.read( buffer ) != 1 ){
					
					break;
				}
				
				header += (char)buffer[0];
				
				if ( header.endsWith( NL + NL )){
					
					break;
				}
			}
			
			return( header );
		}
		
		protected String[]
		splitHeader(
			String		str )
		{
			String[] bits = str.split( NL );
			
			return( bits );
		}
		
		protected void
		destroy()
		{
			synchronized( this ){
				
				if ( destroyed ){
					
					return;
				}
				
				destroyed = true;
			}
			
			if ( socket_out != null ){
				
				try{
					socket_out.close();
					
				}catch( Throwable e ){
					
				}
			}
			try{
				socket_in.close();
				
			}catch( Throwable e ){
				
			}finally{
			
				synchronized( HTTPSniffingProxy.this ){

					processors.remove( this );
				}
			}
		}
	}
	
	public static void
	main(
		String[]		args )
	{
		try{
			HTTPSniffingProxy proxy = new HTTPSniffingProxy( "www.google.com", true );
			
			System.out.println( "port=" + proxy.getPort());
			
			while( true ){
				
				Thread.sleep(1000);
			}
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
}
