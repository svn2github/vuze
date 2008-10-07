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
import java.nio.charset.Charset;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import java.io.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.gudy.azureus2.core3.security.SESecurityManager;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.ThreadPool;


public class 
HTTPSniffingProxy 
{
	public static final int MAX_PROCESSORS = 32;
		
	public static final int	CONNECT_TIMEOUT		= 30*1000;
	public static final int READ_TIMEOUT		= 30*1000;

	private HTTPSniffingProxy		parent;
	private Map						children	= new HashMap();
	
	private URL						delegate_to;
	private String					delegate_to_host;
	private int						delegate_to_port;
	private boolean					delegate_is_https;
	
	private int		port;
	
	private ServerSocket	server_socket;	
	
	private boolean			http_only_detected;
	
	private ThreadPool		thread_pool = new ThreadPool("HTTPSniffer", MAX_PROCESSORS, true );
	
	private List			processors = new ArrayList();
	
	private volatile boolean		destroyed;
		
	public
	HTTPSniffingProxy(
		URL			url )
	
		throws Exception
	{
		this( null, url );
	}
	
	protected
	HTTPSniffingProxy(
		HTTPSniffingProxy		_parent,
		URL						_delegate_to )
	
		throws Exception
	{
		parent				= _parent;
		delegate_to			= _delegate_to;
		
		delegate_to_host	= delegate_to.getHost();
		delegate_is_https	= delegate_to.getProtocol().toLowerCase().equals( "https" );
		delegate_to_port	= delegate_to.getPort()==-1?delegate_to.getDefaultPort():delegate_to.getPort();
					
		server_socket = new ServerSocket();
		
		server_socket.setReuseAddress( true ); 
             
		server_socket.bind( new InetSocketAddress( "127.0.0.1", 0 ));
        
        port = server_socket.getLocalPort();
        
        new AEThread2( 
        	"HTTPSniffingProxy: " + delegate_to_host + ":" + delegate_to_port + "/" + delegate_is_https + "/" + port, 
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
        							
        							proc.start();
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
	
	public boolean
	wasHTTPOnlyCookieDetected()
	{
		return( http_only_detected );
	}
	
	protected void
	setHTTPOnlyCookieDetected()
	{
		http_only_detected = true;
		
		if ( parent != null ){
			
			parent.setHTTPOnlyCookieDetected();
		}
	}
	
	protected String
	getKey(
		URL		url )
	{
		int child_port = url.getPort()==-1?url.getDefaultPort():url.getPort();
		
		String	key = url.getProtocol() + ":" + url.getHost() + ":" + child_port;

		return( key );
	}
	
	protected HTTPSniffingProxy
	getChild(
		String		url_str,
		boolean		existing_only )
	
		throws Exception
	{
		if ( parent != null ){
	
			return( parent.getChild( url_str,existing_only ));
		}
	
		String lc_url_str = url_str.toLowerCase();
		
		if ( lc_url_str.startsWith( "http://" ) || lc_url_str.startsWith( "https://")){
			
			URL child_url = new URL( url_str );
			
			String	child_key = getKey( child_url );
			
			if ( child_key.equals( getKey( delegate_to ))){
				
				return( this );
			}
			
			synchronized( this ){
	
				if ( destroyed ){
					
					throw( new Exception( "Destroyed" ));
				}
				
				HTTPSniffingProxy child = (HTTPSniffingProxy)children.get( child_key );
				
				if ( child == null && !existing_only ){
				
					child = new HTTPSniffingProxy( this, new URL( url_str ));
							
					children.put( child_key, child );
				}
				
				return( child );
			}
		}else{
				//relative
			
			return( this );
		}
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
		
		private transient boolean	destroyed;
		
		protected
		processor(
			Socket		_socket )
		{
			socket_in	= _socket;
		}
		
		protected void
		start()
		{
			thread_pool.run(
				new AERunnable()
				{
					public void
					runSupport()
					{
						try{
							sniff();
							
						}finally{
													
							synchronized( HTTPSniffingProxy.this ){

								processors.remove( processor.this );
							}
						}
					}
				});
		}
		
		protected void
		sniff()
		{
			try{
				InputStream is = socket_in.getInputStream();
				
				String request_header = readHeader( is );
								
				connectToDelegate();
				
				process( request_header );
				
			}catch( Throwable e ){
				
				e.printStackTrace();
				
				destroy();
			}
		}
		
		protected void
		connectToDelegate()
		
			throws IOException
		{
			try{
				if ( delegate_is_https ){
					
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
						
						socket_out.connect( new InetSocketAddress( delegate_to_host, delegate_to_port ), CONNECT_TIMEOUT );
					
					}catch( SSLException ssl_excep ){
												
						factory = SESecurityManager.installServerCertificates( "AZ-sniffer:" + delegate_to_host + ":" + port, delegate_to_host, delegate_to_port );
						
						socket_out = factory.createSocket();
						
						socket_out.connect( new InetSocketAddress( delegate_to_host, delegate_to_port ), 30*1000 );					
					}
				}else{
					
					socket_out = new Socket();
					
					socket_out.connect( new InetSocketAddress( delegate_to_host, delegate_to_port ), CONNECT_TIMEOUT );
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
		
			throws Exception
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
						
						String	port_str;
						
						if ( delegate_to_port == 80 || delegate_to_port == 443 ){
						
							port_str = "";
							
						}else{
							
							port_str = ":" + delegate_to_port;
						}
						
						line_out = "Host: " + delegate_to_host + port_str;
						
					}else if ( lhs.equals( "connection" )){
						
						line_out = "Connection: close";
						
					}else if ( lhs.equals( "referer" )){
						
						String page = line_out.substring( line_out.indexOf( ':' )+1).trim();
						
						page = page.substring( page.indexOf( "://") + 3);
						
						int pos = page.indexOf( '/' );
						
						if ( pos >= 0 ){
						
							page = page.substring( pos );
							
						}else{
							
							page = "/";
						}
						
						String	port_str;
						
						if ( delegate_to_port == 80 || delegate_to_port == 443 ){
						
							port_str = "";
							
						}else{
							
							port_str = ":" + delegate_to_port;
						}

						line_out = "Referer: http" + (delegate_is_https?"s":"") + "://" + delegate_to_host + port_str + page;
					}
				}
				
				if ( line_out != null ){
										
					// System.out.println( "-> " + line_out );
					
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
	
						while( !destroyed ){
												
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
				
			String	content_type	= null;
			String	content_charset	= "ISO-8859-1";
			
			for (int i=0;i<reply_lines.length;i++){
				
				String	line_in 	= reply_lines[i].trim().toLowerCase();
				
				String[] bits = line_in.split(":");
				
				if ( bits.length >= 2 ){

					String	lhs = bits[0].trim();
					
					if ( lhs.equals( "content-type" )){
						
						String rhs = reply_lines[i].substring( line_in.indexOf( ':' ) + 1 ).trim();
								
						String[] x = rhs.split( ";" );
						
						content_type = x[0];
						
						if ( x.length > 1 ){
							
							int	pos = rhs.toLowerCase().indexOf( "charset" );
						
							if ( pos >= 0 ){
							
								String cc = rhs.substring( pos+1 );
							
								pos = cc.indexOf('=');
							
								if ( pos != -1 ){
								
									cc = cc.substring( pos+1 ).trim();
								
									if ( Charset.isSupported( cc )){
																		
										content_charset = cc;
									}
								}
							}
						}
					}
				}
			}
			
			boolean	rewrite 			= false;
			boolean	chunked				= false;
			String	content_encoding	= null;
			
			if ( content_type == null ){
				
				rewrite = true;
				
			}else{
			
				content_type = content_type.toLowerCase();
				
				if ( content_type.indexOf( "text/" ) != -1 ){
					
					rewrite = true;
				}
			}
						
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
								
								setHTTPOnlyCookieDetected();
								
							}else if ( entry.equalsIgnoreCase( "secure" )){
								
							}else{
								
								modified_cookie += (modified_cookie.length()==0?"":"; ") + entry;
							}
						}
						
						line_out = "Set-Cookie: " + modified_cookie;
						
					}else if ( lhs.equals( "connection" )){
						
						line_out = "Connection: close";
						
					}else if ( lhs.equals( "location" )){
												
						String page = line_out.substring( line_out.indexOf( ':' )+1).trim();
						
						String child_url = page.trim();
						
						HTTPSniffingProxy child = getChild( child_url, false );

						int	pos = page.indexOf( "://");
						
						if ( pos >= 0 ){
							
							page = page.substring( pos + 3);
						
							pos = page.indexOf( '/' );
						
							if ( pos >= 0 ){
							
								page = page.substring( pos );
							
							}else{
							
								page = "/";
							}
						}else{
							
							if ( !page.startsWith( "/" )){
								
								page = "/" + page;
							}
						}
						
						line_out = "Location: http://127.0.0.1:" + child.getPort() + page;
						
					}else if ( lhs.equals( "content-encoding" )){
						 
						if ( rewrite ){
							
							String	encoding = bits[1].trim();
								 					
		 					if ( 	encoding.equalsIgnoreCase( "gzip"  ) || 
		 							encoding.equalsIgnoreCase( "deflate" )){
			 									 					
				 				content_encoding = encoding;
			 					
				 				line_out = null;
		 					}
						}
					}else if ( lhs.equals( "content-length" )){

						if ( rewrite ){
							
							line_out = null;
						}
					}else if ( lhs.equals( "transfer-encoding" )){

						if ( bits[1].indexOf( "chunked" ) != -1 ){
							
							chunked = true;
							
							if ( rewrite ){
								
								line_out = null;
							}
						}
	 				}
				}
				
				if ( line_out != null ){
					
					// System.out.println( "<- " + line_out );
					
					source_os.write((line_out+NL).getBytes());
				}
			}
			
			byte[]	buffer = new byte[32000];
			

			if ( rewrite ){
										
				StringBuffer	sb = new StringBuffer();
					
				if ( chunked ){
					
						// chunking uses ISO-8859-1
					
					while( true ){
						
						int	len = target_is.read( buffer );
						
						if ( len <= 0 ){
							
							break;
						}
						
						sb.append(new String( buffer, 0, len, "ISO-8859-1" ));
					}
					
					StringBuffer	sb_dechunked = new StringBuffer( sb.length());
					
					String chunk = "";
	
					int total_length = 0;
	
					int	sb_pos = 0;
										
					while( sb_pos < sb.length()){
	
						chunk += sb.charAt( sb_pos++ );
	
							// second time around the chunk will be prefixed with NL
							// from end of previous
							// so make sure we ignore this
	
						if ( chunk.endsWith( NL ) && chunk.length() > 2 ){
	
							int semi_pos = chunk.indexOf( ';' );
	
							if ( semi_pos != -1 ){
	
								chunk = chunk.substring( 0, semi_pos );
							}
	
							chunk = chunk.trim();
	
							int chunk_length = Integer.parseInt( chunk, 16 );
	
							if ( chunk_length <= 0 ){
	
								break;
							}
	
							total_length += chunk_length;
	
							if ( total_length > 2*1024*1024 ){
	
								throw (new IOException("Chunk size " + chunk_length
										+ " too large"));
							}
	
							char[] chunk_buffer = new char[chunk_length];
	
							sb.getChars( sb_pos, sb_pos + chunk_length, chunk_buffer, 0 );
	
							sb_dechunked.append( chunk_buffer );
	
							sb_pos += chunk_length;
							
							chunk = "";
						}
					}
					
						// dechunked ISO-8859-1 - unzip if required and then apply correct charset						

					target_is = new ByteArrayInputStream( sb_dechunked.toString().getBytes( "ISO-8859-1" ));
				}
				
				if ( content_encoding != null ){

					if ( content_encoding.equalsIgnoreCase( "gzip"  )){
		 					
						target_is = new GZIPInputStream( target_is );
		 							 				
	 				}else if ( content_encoding.equalsIgnoreCase( "deflate" )){
	 						
	 					target_is = new InflaterInputStream( target_is );
	 				}
	 			}
					
				sb.setLength(0);
					
				while( !destroyed ){
						
					int	len = target_is.read( buffer );
						
					if ( len <= 0 ){
							
						break;
					}
				
					sb.append(new String( buffer, 0, len, content_charset ));
				}
				
				String 	str 	= sb.toString();
				String	lc_str 	= str.toLowerCase();
				
				StringBuffer	result 	= null;
				int				str_pos	= 0;
				
				FileUtil.writeBytesAsFile( "C:\\temp\\xxx" + new Random().nextInt(100000) + ".txt", str.getBytes());
				
				while( true ){
					
						// http://a.b
					
					int	url_start = str.length() - str_pos >=10?lc_str.indexOf( "http", str_pos ):-1;
					
					if ( url_start == -1 ){
												
						break;
					}
					
					int	match_pos;
					
					if ( lc_str.charAt( url_start + 4 ) == 's' ){
						
						match_pos = url_start + 5;
						
					}else{
						
						match_pos = url_start + 4;
					}
					
					if ( lc_str.substring( match_pos, match_pos+3 ).equals( "://" )){
						
						int	url_end = -1;
						
						for (int i=match_pos+3;;i++){
							
							char c = lc_str.charAt(i);
							
							if ( c == '/' || i == lc_str.length()-1 ){
							
								url_end = i;
								
								break;
								
							}else if ( c == '.' || c == '-' || c == ':' ){
								
							}else if ( c >= '0' && c <= '9' ){
								
							}else if ( c >= 'a' && c <= 'z' ){
																
							}else{
								
								url_end = i;
								
								break;
							}
						}
						
						if ( url_end > url_start ){
							
							String 	url_str = str.substring( url_start, url_end+1 );
							
							boolean	appended = false;
							
							try{								
								HTTPSniffingProxy child = getChild( url_str, true );
								
								if ( child != null ){
									
									String replacement = "http://127.0.0.1:" + child.getPort();
									
									if ( url_str.endsWith( "/" )){
										
										replacement += "/";
									}
									
									if ( result == null ){
										
										result = new StringBuffer( str.length());
										
										if ( url_start > 0 ){
											
											result.append( str.subSequence( 0, url_start ));
										}
									}else if ( url_start > str_pos ){
										
										result.append( str.subSequence( str_pos, url_start ));
									}
									
									// System.out.println( "Replacing " + url_str + " with " + replacement );
									
									result.append( replacement );
									
									appended = true;
									
								}else{
									
									// System.out.println( "    Not child for " + url_str );
								}
							}catch( Throwable e ){
								
							}
							
							if ( result != null && !appended ){
								
								result.append( str.subSequence( str_pos, url_end+1 ));
							}
							
							str_pos = url_end+1;
							
						}else{
							
							break;
						}
					}else{
						
						if ( result != null ){
							
							result.append( str.subSequence( str_pos, match_pos ));
						}
						
						str_pos = match_pos;
					}
				}
				
				if ( result != null ){
							
					if ( str_pos < str.length() ){
						
						result.append( str.subSequence( str_pos, str.length()));
					}

					sb = result;
				}
				
				source_os.write( ( "Content-Length: " + sb.length() + NL ).getBytes());
				
				source_os.write( NL.getBytes());
				
				source_os.write( sb.toString().getBytes( content_charset ));
				
			}else{
				
				source_os.write( NL.getBytes());
							
				while( !destroyed  ){
									
					int	len = target_is.read( buffer );
					
					if ( len <= 0 ){
						
						break;
					}
					
					source_os.write( buffer, 0, len );
				}
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
			
			Iterator it = children.values().iterator();
			
			while ( it.hasNext()){
				
				((HTTPSniffingProxy)it.next()).destroy();
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
			}
		}
	}
	
	public static void
	main(
		String[]		args )
	{
		try{
			HTTPSniffingProxy proxy = new HTTPSniffingProxy( new URL( "http://www.sf.net/" ));
			
			System.out.println( "port=" + proxy.getPort());
			
			while( true ){
				
				Thread.sleep(1000);
			}
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
}
