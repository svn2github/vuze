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

import java.util.*;
import java.net.*;
import java.io.*;

import java.applet.*;
import java.awt.*;

import javax.net.ssl.*;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.ui.webplugin.remoteui.plugins.*;
import org.gudy.azureus2.ui.webplugin.remoteui.plugins.download.*;

import org.gudy.azureus2.plugins.*;

public class 
RemoteUIApplet
	extends 	Applet
	implements 	RPRequestDispatcher
{
	protected PluginInterface		plugin_interface;
	
	public
	RemoteUIApplet()
	{	
	}
	
	public void
	init()
	{
			// set up dummy configuration parameters as file-based default read will fail as 
			// we are an applet!
		
		COConfigurationManager.initialiseFromMap( new HashMap());
	}
	
	public void
	start()
	{
		try{
			final PluginInterface pi = RPFactory.getPlugin( this );
			
			System.out.println( "got pi:" + pi );
			
			Properties props = pi.getPluginProperties();
			
			System.out.println( "props = " + props );
			
			RemoteUIMainPanel	panel = new RemoteUIMainPanel( pi );
			
			panel.addListener(
				new RemoteUIMainPanelListener()
				{
					public void
					refresh()
					{
						((RPDownloadManager)pi.getDownloadManager())._refresh();
					}
				});
			
			setLayout(new BorderLayout());
			
			add(panel, BorderLayout.CENTER );
			
			validate();
			
		}catch( RPException e ){
			
			e.printStackTrace();
		}
	}
	
	public RPReply
	dispatch(
		RPRequest	request )
	
		throws RPException
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
				
				ObjectInputStream	ois = new ObjectInputStream(new ByteArrayInputStream( baos.toByteArray()));
				
				try{
					return((RPReply)ois.readObject());
					
				}finally{
					
					ois.close();
				}
			}finally{
				
				if ( is != null ){
					
					is.close();
				}
			}
		}catch( Throwable e ){		
		
			throw( new RPException( "RequestDispatch fails", e ));
		}
	}
}