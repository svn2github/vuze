/*
 * Created on 02-Jan-2005
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

package org.gudy.azureus2.core3.tracker.server.impl.tcp.blocking;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.security.SESecurityManager;
import org.gudy.azureus2.core3.tracker.server.TRTrackerServerException;
import org.gudy.azureus2.core3.tracker.server.impl.tcp.TRTrackerServerTCP;
import org.gudy.azureus2.core3.util.AEThread;


/**
 * @author parg
 *
 */

public class 
TRBlockingServer
	extends TRTrackerServerTCP
{
	public
	TRBlockingServer(
		String		_name,
		int			_port,
		boolean		_ssl,
		boolean		_apply_ip_filter )
		
		throws TRTrackerServerException
	{
		super( _name, _port, _ssl, _apply_ip_filter );
		
		boolean	ok = false;
		
		try{
			String bind_ip = COConfigurationManager.getStringParameter("Bind IP", "");
	
			if ( _ssl ){
				
				try { 	      
					SSLServerSocketFactory factory = SESecurityManager.getSSLServerSocketFactory();
	 
					if ( factory == null ){
											
						throw( new TRTrackerServerException( "TRTrackerServer: failed to get SSL factory" ));
						  
					}else{
						SSLServerSocket ssl_server_socket;
						
						if ( bind_ip.length() < 7 ){
							
							ssl_server_socket = (SSLServerSocket)factory.createServerSocket( getPort(), 128 );
							
						}else{
							
							ssl_server_socket = (SSLServerSocket)factory.createServerSocket(  getPort(), 128, InetAddress.getByName(bind_ip));
						}
		
						String cipherSuites[] = ssl_server_socket.getSupportedCipherSuites();
		  
						ssl_server_socket.setEnabledCipherSuites(cipherSuites);
		 
						ssl_server_socket.setNeedClientAuth(false);
						
						ssl_server_socket.setReuseAddress(true);
														
						final SSLServerSocket	f_ss = ssl_server_socket;
						
						Thread accept_thread = 
								new AEThread("TRTrackerServer:accept.loop(ssl)")
								{
									public void
									runSupport()
									{
										acceptLoop( f_ss );
									}
								};
					
						accept_thread.setDaemon( true );
					
						accept_thread.start();									
					
						LGLogger.log( "TRTrackerServer: SSL listener established on port " +  getPort() );
						
						ok	= true;
					}
					
				}catch( Throwable e){
									
					LGLogger.logUnrepeatableAlertUsingResource( 
							LGLogger.AT_ERROR,
							"Tracker.alert.listenfail",
							new String[]{ ""+ getPort() });
					
					LGLogger.log( "TRTrackerServer: SSL listener failed on port " +  getPort(), e ); 
					  
					if ( e instanceof TRTrackerServerException ){
						
						throw((TRTrackerServerException)e);
						
					}else{
						
						throw( new TRTrackerServerException( "TRTrackerServer: accept fails: " + e.toString()));
					}
				}
				
			}else{
				
				try{
					ServerSocket ss;
					
					if ( bind_ip.length() < 7 ){
						
						ss = new ServerSocket(  getPort(), 1024 );
						
					}else{
						
						ss = new ServerSocket(  getPort(), 1024, InetAddress.getByName(bind_ip));
					}
					
					ss.setReuseAddress(true);
					
					final ServerSocket	f_ss = ss;
					
					Thread accept_thread = 
							new AEThread("TRTrackerServer:accept.loop")
							{
								public void
								runSupport()
								{
									acceptLoop( f_ss );
								}
							};
				
					accept_thread.setDaemon( true );
				
					accept_thread.start();									
				
					LGLogger.log( "TRTrackerServer: listener established on port " +  getPort() ); 
					
					ok	= true;
					
				}catch( Throwable e){
				
					LGLogger.logUnrepeatableAlertUsingResource( 
							LGLogger.AT_ERROR,
							"Tracker.alert.listenfail",
							new String[]{ ""+ getPort() });
			
					LGLogger.log( "TRTrackerServer: listener failed on port " +  getPort(), e ); 
								
					throw( new TRTrackerServerException( "TRTrackerServer: accept fails: " + e.toString()));
				}			
			}
		}finally{
			
			if ( !ok ){
				
				destroy();
			}
		}
	}
		
	protected void
	acceptLoop(
		ServerSocket	ss )
	{		
		long	successfull_accepts = 0;
		long	failed_accepts		= 0;
		
		while(true){
			
			try{				
				Socket socket = ss.accept();
					
				successfull_accepts++;
				
				String	ip = socket.getInetAddress().getHostAddress();
								
				if ( (!isIPFilterEnabled()) || (!ip_filter.isInRange( ip, "Tracker" ))){
					
					runProcessor( new TRBlockingServerProcessor( this, socket ));
					
				}else{
					
					socket.close();
				}
				
			}catch( Throwable e ){
				
				failed_accepts++;
				
				LGLogger.log( "TRTrackerServer: listener failed on port " +  getPort(), e ); 
				
				if ( failed_accepts > 100 && successfull_accepts == 0 ){

						// looks like its not going to work...
						// some kind of socket problem
									
					LGLogger.logUnrepeatableAlertUsingResource( 
							LGLogger.AT_ERROR,
							"Network.alert.acceptfail",
							new String[]{ ""+ getPort(), "TCP" } );
			
					break;
				}
			}
		}
	}

}
