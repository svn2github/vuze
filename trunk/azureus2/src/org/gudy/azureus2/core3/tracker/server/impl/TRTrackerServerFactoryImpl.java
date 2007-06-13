/*
 * File    : TRTrackerServerFactoryImpl.java
 * Created : 13-Dec-2003
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

package org.gudy.azureus2.core3.tracker.server.impl;

/**
 * @author parg
 *
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.tracker.server.*;
import org.gudy.azureus2.core3.tracker.server.impl.dht.TRTrackerServerDHT;
import org.gudy.azureus2.core3.tracker.server.impl.tcp.TRTrackerServerTCP;
import org.gudy.azureus2.core3.tracker.server.impl.tcp.blocking.TRBlockingServer;
import org.gudy.azureus2.core3.tracker.server.impl.tcp.nonblocking.TRNonBlockingServer;
import org.gudy.azureus2.core3.tracker.server.impl.tcp.nonblocking.TRNonBlockingServerProcessor;
import org.gudy.azureus2.core3.tracker.server.impl.tcp.nonblocking.TRNonBlockingServerProcessorFactory;
import org.gudy.azureus2.core3.tracker.server.impl.udp.*;
import org.gudy.azureus2.core3.util.AEMonitor;

public class 
TRTrackerServerFactoryImpl 
{
	protected static List		servers		= new ArrayList();
	protected static List		listeners 	= new ArrayList();
	protected static AEMonitor 	class_mon 	= new AEMonitor( "TRTrackerServerFactory" );

	public static TRTrackerServer
	create(
		String		name,
		int			protocol,
		int			port,
		InetAddress	bind_ip,
		boolean		ssl,
		boolean		apply_ip_filter,
		boolean		main_tracker,
		boolean		start_up_ready )
	
		throws TRTrackerServerException
	{
		try{
			class_mon.enter();
		
			TRTrackerServerImpl	server;
			
			if ( protocol == TRTrackerServerFactory.PR_TCP ){
				
				if ( COConfigurationManager.getBooleanParameter( "Tracker TCP NonBlocking" ) && main_tracker && !ssl ){
					
					server = 
						new TRNonBlockingServer( 
							name, 
							port, 
							bind_ip, 
							apply_ip_filter,
							start_up_ready,
							new TRNonBlockingServerProcessorFactory()
							{
								public TRNonBlockingServerProcessor
								create(	
									TRTrackerServerTCP		_server,
									SocketChannel			_socket )
								{
									return( new NonBlockingProcessor( _server, _socket ));

								}
							});
				}else{
					
					server = new TRBlockingServer( name, port, bind_ip, ssl, apply_ip_filter, start_up_ready );
				}
				
			}else if ( protocol == TRTrackerServerFactory.PR_UDP ){
				
				if ( ssl ){
					
					throw( new TRTrackerServerException( "TRTrackerServerFactory: UDP doesn't support SSL"));
				}
				
				server = new TRTrackerServerUDP( name, port, start_up_ready );
				
			}else{
				
				server = new TRTrackerServerDHT( name, start_up_ready );
			}
			
			servers.add( server );
			
			for (int i=0;i<listeners.size();i++){
				
				((TRTrackerServerFactoryListener)listeners.get(i)).serverCreated( server );
			}
			
			return( server );
			
		}finally{
			
			class_mon.exit();
		}
	}
	
	public static void
	addListener(
		TRTrackerServerFactoryListener	l )
	{
		try{
			class_mon.enter();
		
			listeners.add( l );
			
			for (int i=0;i<servers.size();i++){
				
				l.serverCreated((TRTrackerServer)servers.get(i));
			}
		}finally{
			
			class_mon.exit();
		}
	}	
	
	public static void
	removeListener(
		TRTrackerServerFactoryListener	l )
	{
		try{
			class_mon.enter();
		
			listeners.remove( l );
			
		}finally{
			
			class_mon.exit();
		}
	}
	
	protected static class
	NonBlockingProcessor
		extends TRNonBlockingServerProcessor
	{
		protected
		NonBlockingProcessor(
			TRTrackerServerTCP		_server,
			SocketChannel			_socket )
		{
			super( _server, _socket );
		}
		
		protected ByteArrayOutputStream 
		process(
			String 				input_header, 
			String 				lowercase_input_header, 
			String 				url_path, 
			InetSocketAddress 	client_address, 
			boolean 			announce_and_scrape_only, 
			InputStream 		is ) 
		
			throws IOException 
		{
			ByteArrayOutputStream	os = new ByteArrayOutputStream( 1024 );
			
			processRequest(input_header, lowercase_input_header, url_path, client_address, announce_and_scrape_only, is, os );
			
			return( os );
		}
	}
}
