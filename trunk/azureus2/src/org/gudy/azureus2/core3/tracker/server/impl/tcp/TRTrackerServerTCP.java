/*
 * File    : TRTrackerServerImpl.java
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


import java.net.*;
import java.io.*;
import java.util.*;

import javax.net.ssl.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.tracker.server.*;
import org.gudy.azureus2.core3.tracker.server.impl.*;
import org.gudy.azureus2.core3.security.*;

public class 
TRTrackerServerTCP 
	extends 	TRTrackerServerImpl
{
	protected static final int THREAD_POOL_SIZE				= 32;
	
	protected int	port;
	
	protected Vector	request_listeners 	= new Vector();
	
	protected ThreadPool	thread_pool;
	
	protected boolean	web_password_enabled;
	protected boolean	tracker_password_enabled;
	protected String	password_user;
	protected byte[]	password_pw;
	
	
	public
	TRTrackerServerTCP(
		int			_port,
		boolean		_ssl  )
		
		throws TRTrackerServerException
	{
		port					= _port;

		COConfigurationManager.addListener(
			new COConfigurationListener()
			{
				public void
				configurationSaved()
				{
					readPasswordSettings();
				}
			});
			
		readPasswordSettings();
				
		thread_pool = new ThreadPool( "TrackerServer:TCP:"+port, THREAD_POOL_SIZE );			
		current_announce_retry_interval	= COConfigurationManager.getIntParameter("Tracker Poll Interval Min", DEFAULT_MIN_RETRY_DELAY );
		
		if ( current_announce_retry_interval < RETRY_MINIMUM_SECS ){
			
			current_announce_retry_interval = RETRY_MINIMUM_SECS;
		}

		String bind_ip = COConfigurationManager.getStringParameter("Bind IP", "");

		if ( _ssl ){
			
			try {
				
 	      
				SSLServerSocketFactory factory = SESecurityManager.getSSLServerSocketFactory();
 
				SSLServerSocket ssl_server_socket;
				
				if ( bind_ip.length() < 7 ){
					
					ssl_server_socket = (SSLServerSocket)factory.createServerSocket( port, 128 );
					
				}else{
					
					ssl_server_socket = (SSLServerSocket)factory.createServerSocket( port, 128, InetAddress.getByName(bind_ip));
				}

				String cipherSuites[] = ssl_server_socket.getSupportedCipherSuites();
  
				ssl_server_socket.setEnabledCipherSuites(cipherSuites);
 
				ssl_server_socket.setNeedClientAuth(false);
				
				ssl_server_socket.setReuseAddress(true);
												
				final SSLServerSocket	f_ss = ssl_server_socket;
				
				Thread accept_thread = 
						new Thread("TRTrackerServer:accept.loop(ssl)")
						{
							public void
							run()
							{
								acceptLoop( f_ss );
							}
						};
			
				accept_thread.setDaemon( true );
			
				accept_thread.start();									
			
				LGLogger.log( "TRTrackerServer: SSL listener established on port " + port ); 
				
			}catch( Throwable e){
			
				LGLogger.log( "TRTrackerServer: SSL listener failed on port " + port, e ); 
				  
				throw( new TRTrackerServerException( "TRTrackerServer: accept fails: " + e.toString()));
   
			}
		}else{
			
			try{
				ServerSocket ss;
				
				if ( bind_ip.length() < 7 ){
					
					ss = new ServerSocket( port, 128 );
					
				}else{
					
					ss = new ServerSocket( port, 128, InetAddress.getByName(bind_ip));
				}
				
				ss.setReuseAddress(true);
				
				final ServerSocket	f_ss = ss;
				
				Thread accept_thread = 
						new Thread("TRTrackerServer:accept.loop")
						{
							public void
							run()
							{
								acceptLoop( f_ss );
							}
						};
			
				accept_thread.setDaemon( true );
			
				accept_thread.start();									
			
				LGLogger.log( "TRTrackerServer: listener established on port " + port ); 
				
			}catch( Throwable e){
			
				LGLogger.log( "TRTrackerServer: listener failed on port " + port, e ); 
							
				throw( new TRTrackerServerException( "TRTrackerServer: accept fails: " + e.toString()));
			}			
		}
	}
	

	int req_num;
	
	protected void
	acceptLoop(
		ServerSocket	ss )
	{		
		while(true){
			
			try{				
				final Socket socket = ss.accept();
				
				String	ip = socket.getInetAddress().getHostAddress();
				
				if ( !ip_filter.isInRange( ip )){
					
					thread_pool.run( new TRTrackerServerProcessorTCP( this, socket ));
					
				}else{
					
					socket.close();
				}
				
			}catch( Throwable e ){
				
				e.printStackTrace();		
			}
		}
	}
		

	protected void
	readPasswordSettings()
	{		
		web_password_enabled 		= COConfigurationManager.getBooleanParameter("Tracker Password Enable Web", false);
		tracker_password_enabled 	= COConfigurationManager.getBooleanParameter("Tracker Password Enable Torrent", false);

		if ( web_password_enabled || tracker_password_enabled ){
			
			password_user	= COConfigurationManager.getStringParameter("Tracker Username", "");
			password_pw		= COConfigurationManager.getByteParameter("Tracker Password", new byte[0]);
		}
	}
	
	public boolean
	isWebPasswordEnabled()
	{
		return( web_password_enabled );
	}
	
	public boolean
	isTrackerPasswordEnabled()
	{
		return( tracker_password_enabled );
	}
	
	public String
	getUsername()
	{
		return( password_user );
	}
	
	public byte[]
	getPassword()
	{
		return( password_pw );
	}
	

	
	protected boolean
	handleExternalRequest(
		String			url,
		String			header,
		InputStream		is,
		OutputStream	os )
		
		throws IOException
	{
		for (int i=0;i<listeners.size();i++){
			
			TRTrackerServerListener	listener;
			
			synchronized(this){
				
				if ( i >= listeners.size()){
					
					break;
				}
				
				listener = (TRTrackerServerListener)listeners.elementAt(i);
			}
			
			if (listener.handleExternalRequest( url, header, is, os )){
				
				return( true );
			}
		}
		
		return( false );
	}
	
	protected void
	postProcess(
		TRTrackerServerTorrentImpl	torrent,
		int							type,
		Map							response )
	{
		if ( request_listeners.size() > 0 ){
			
			TRTrackerServerRequestImpl	req = new TRTrackerServerRequestImpl( this, torrent, type, response );
			
			for (int i=0;i<request_listeners.size();i++){
				
				((TRTrackerServerRequestListener)request_listeners.elementAt(i)).postProcess( req );
			}
		}
	}
		
	public void
	addRequestListener(
		TRTrackerServerRequestListener	l )
	{
		request_listeners.addElement( l );
	}
	
	public void
	removeRequestListener(
		TRTrackerServerRequestListener	l )
	{
		request_listeners.removeElement(l);
	}
}
