/*
 * File    : XMLHTTPClient.java
 * Created : 13-Mar-2004
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

package org.gudy.azureus2.ui.webplugin.remoteui.xml.client;

/**
 * @author parg
 *
 */

import java.io.*;
import java.net.*;
import javax.net.ssl.*;

import org.gudy.azureus2.core3.util.*;

import org.gudy.azureus2.core3.xml.simpleparser.*;

public class 
XMLHTTPClient 
{
	protected
	XMLHTTPClient()
	{
		try{
			
			SimpleXMLParserDocument	res = sendRequest( "<REQUEST><OID>0</OID><METHOD>getSingleton</METHOD></REQUEST>");
			
			res.print();
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}

	protected SimpleXMLParserDocument
	sendRequest(
		String	request )
	
		throws SimpleXMLParserDocumentException
	{
		String	resp = sendRequestSupport( request );
		
		System.out.println( "got:" + resp );
		
		return( SimpleXMLParserDocumentFactory.create( resp ));
	}
	
	protected String
	sendRequestSupport(
		String		request )
	{
		try{
	
		    URL url = new URL( "http://localhost:6884/process.cgi" );
			
			// System.out.println( "doc base = " + url );
			
			HttpURLConnection con;
			
			if ( url.getProtocol().equalsIgnoreCase("https")){
				
				// see ConfigurationChecker for SSL client defaults
				
				URLConnection url_con = url.openConnection();
				
					// Opera doesn't return a javax class
				
				if ( url_con.getClass().getName().startsWith( "javax")){
									
					HttpsURLConnection ssl_con = (HttpsURLConnection)url_con;
					
					// allow for certs that contain IP addresses rather than dns names
					
					ssl_con.setHostnameVerifier(
							new HostnameVerifier()
							{
								public boolean
								verify(
										String		host,
										SSLSession	session )
								{
									return( true );
								}
							});	
				
					con = ssl_con;
				}else{
					
					con = (HttpURLConnection)url_con;
				}
			}else{
				
				con = (HttpURLConnection) url.openConnection();
			}
	
			con.setRequestProperty("Connection", "close" );
			
			con.setRequestMethod( "POST" );
			
			con.setAllowUserInteraction( true );
			
			con.setDoInput( true );
			
			con.setDoOutput( true );
						
			con.connect();
		
			PrintWriter os = null;
			
			try{
				os	= new PrintWriter( new OutputStreamWriter( con.getOutputStream() , Constants.DEFAULT_ENCODING ));
			
				os.print( request );
				
				os.flush();
				
			}finally{
			
				if ( os != null ){
					
					os.close();
				}
			}
			
			InputStream is = null;
			
			try{
				
				is = con.getInputStream();
				
				int content_length = con.getContentLength();
				
				byte[] data = new byte[1024];
				
				int	num_read = 0;
				
				ByteArrayOutputStream	baos = new ByteArrayOutputStream();
				
				while ( num_read < content_length ){
					
					try{
						int	len = is.read(data);
						
						if ( len > 0 ){
							
							baos.write(data, 0, len);
															
							num_read += len;
							
						}else if ( len == 0 ){
							
							Thread.sleep(20);
							
						}else{
							
							break;
						}
						
					}catch (Exception e){
						
						e.printStackTrace();
						
						break;
					}
				}
												
				return( new String( baos.toByteArray(), Constants.DEFAULT_ENCODING ));
				
			}finally{
				
				if ( is != null ){
					
					is.close();
				}
			}
		}catch( Throwable e ){		
	
			throw( new RuntimeException( "whoops", e ));
		}	
	}
	
	public static void
	main(
		String[]		args )
	{
		new XMLHTTPClient();
	}
}
