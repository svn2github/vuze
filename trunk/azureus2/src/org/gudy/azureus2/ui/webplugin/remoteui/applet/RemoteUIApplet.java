/*
 * File    : WebPluginApplet.java
 * Created : 27-Jan-2004
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

package org.gudy.azureus2.ui.webplugin.remoteui.applet;

/**
 * @author parg
 *
 */

import java.net.*;
import java.io.*;

import java.applet.*;
import javax.swing.*;
import java.awt.*;

import javax.net.ssl.*;

public class 
RemoteUIApplet
	extends Applet
{
	public
	RemoteUIApplet()
	{	
	}
	
	public void
	init()
	{
		setLayout(new BorderLayout());
		
		add(new JLabel("boing!"), BorderLayout.CENTER );
	}
	
	public void
	start()
	{
		sendRequest( "hello mum" );
	}
	
	protected void
	sendRequest(
		Object		request )
	{
		try{
			URL	url = this.getDocumentBase();
			
			url = new URL( url.toString() + "process.cgi" );
			
			System.out.println( "doc base = " + url );
			
			HttpURLConnection con;
			
			if ( url.getProtocol().equalsIgnoreCase("https")){
				
				// see ConfigurationChecker for SSL client defaults
				
				HttpsURLConnection ssl_con = (HttpsURLConnection)url.openConnection();
				
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
				
				con = (HttpURLConnection) url.openConnection();
			}

			con.setRequestProperty("Connection", "close" );
			
			con.setDoInput( true );
			
			con.setDoOutput( true );
			
			try{
				
				con.connect();
			
				ObjectOutputStream dos = null;
				
				try{
					dos = new ObjectOutputStream(con.getOutputStream());
				
					dos.writeObject( request );
					
					dos.flush();
					
				}finally{
				
					if ( dos != null ){
						
						dos.close();
					}
				}
				
				InputStream is = null;
				
				try{
					
					is = con.getInputStream();
					
					int content_length = con.getContentLength();
					
					byte[] data = new byte[1024];
					
					int	num_read = 0;
					
					while ( num_read < content_length ){
						
						try{
							int	len = is.read(data);
							
							if ( len > 0 ){
								
								//message.write(data, 0, len);
								
								System.out.println( new String(data,0,len));
								
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
				}finally{
					
					if ( is != null ){
						
						is.close();
					}
				}
			}catch( Exception e ){
				
				e.printStackTrace();
			}
		}catch( Throwable e ){		
			
			e.printStackTrace();
		}
	}
}