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
import java.text.SimpleDateFormat;
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

	protected String	name;
	protected boolean	ssl;
	protected int		port;
	protected boolean	apply_ip_filter;
	
	protected Vector	request_listeners 	= new Vector();
	
	protected ThreadPool	thread_pool;
	
	public
	TRTrackerServerTCP(
		String		_name,
		int			_port,
		boolean		_ssl,
		boolean		_apply_ip_filter )
		
		throws TRTrackerServerException
	{
		super( _name );
		
		port					= _port;
		ssl						= _ssl;
		apply_ip_filter			= _apply_ip_filter;

		thread_pool = new ThreadPool( "TrackerServer:TCP:"+port, THREAD_POOL_SIZE );			
		current_announce_retry_interval	= COConfigurationManager.getIntParameter("Tracker Poll Interval Min", DEFAULT_MIN_RETRY_DELAY );
		
		if ( current_announce_retry_interval < RETRY_MINIMUM_SECS ){
			
			current_announce_retry_interval = RETRY_MINIMUM_SECS;
		}

		String bind_ip = COConfigurationManager.getStringParameter("Bind IP", "");

		if ( _ssl ){
			
			try { 	      
				SSLServerSocketFactory factory = SESecurityManager.getSSLServerSocketFactory();
 
				if ( factory == null ){
										
					throw( new TRTrackerServerException( "TRTrackerServer: failed to get SSL factory" ));
					  
				}else{
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
							new AEThread("TRTrackerServer:accept.loop(ssl)")
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
				}
				
			}catch( Throwable e){
								
				LGLogger.logAlertUsingResource( 
						LGLogger.AT_ERROR,
						"Tracker.alert.listenfail",
						new String[]{ ""+port });
				
				LGLogger.log( "TRTrackerServer: SSL listener failed on port " + port, e ); 
				  
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
					
					ss = new ServerSocket( port, 128 );
					
				}else{
					
					ss = new ServerSocket( port, 128, InetAddress.getByName(bind_ip));
				}
				
				ss.setReuseAddress(true);
				
				final ServerSocket	f_ss = ss;
				
				Thread accept_thread = 
						new AEThread("TRTrackerServer:accept.loop")
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
			
				LGLogger.logAlertUsingResource( 
						LGLogger.AT_ERROR,
						"Tracker.alert.listenfail",
						new String[]{ ""+port });
		
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
				
				//if ( checkDOS( ip )){
					
				//	socket.close();
					
				//}else{
					
					if ( (!apply_ip_filter) || (!ip_filter.isInRange( ip, "Tracker" ))){
						
						thread_pool.run( new TRTrackerServerProcessorTCP( this, socket ));
						
					}else{
						
						socket.close();
					}
				//}
				
			}catch( Throwable e ){
				
				// e.printStackTrace();		
			}
		}
	}
	
	static boolean	LOG_DOS_TO_FILE	= false;
	
	static{
		
		LOG_DOS_TO_FILE = System.getProperty("azureus.log.dos") != null;
	}
	
	protected static File		dos_log_file;
	
	Map	DOS_map = 
		new LinkedHashMap( 1000, (float)0.75, true )
		{
			protected boolean 
			removeEldestEntry(
				Map.Entry eldest) 
			{
				return( checkDOSRemove( eldest ));
			}
		};
	
	List	dos_list	= new ArrayList(128);
	
	long	last_dos_check				= 0;
	long	MAX_DOS_ENTRIES				= 10000;
	long	MAX_DOS_RETENTION			= 10000;
	int		DOS_CHECK_DEAD_WOOD_COUNT	= 512;
	int		DOS_MIN_INTERVAL			= 1000;
	int		dos_check_count				= 0;
	
	protected boolean
	checkDOS(
		String		ip )
	
		throws UnknownHostException
	{
		InetAddress	inet_address = InetAddress.getByName(ip);
		
		if ( inet_address.isLoopbackAddress() || InetAddress.getLocalHost().equals( inet_address )){
			
			return( false);
		}
		
		boolean	res;
		
		last_dos_check = SystemTime.getCurrentTime();
		
		DOSEntry	entry = (DOSEntry)DOS_map.get(ip);
		
		if ( entry == null ){
						
			entry = new DOSEntry(ip);
			
			DOS_map.put( ip, entry );
			
			res	= false;
			
		}else{
	
			res = last_dos_check - entry.last_time < DOS_MIN_INTERVAL;
			
			if ( res && LOG_DOS_TO_FILE ){
				
				dos_list.add( entry );
			}
			
			entry.last_time = last_dos_check;
		}
		
			// remove dead wood
		
		dos_check_count++;
		
		if ( dos_check_count == DOS_CHECK_DEAD_WOOD_COUNT ){
			
			dos_check_count = 0;
			
			Iterator	it = DOS_map.values().iterator();
			
			while( it.hasNext()){
				
				DOSEntry	this_entry = (DOSEntry)it.next();
				
				if ( last_dos_check - this_entry.last_time > MAX_DOS_RETENTION ){
					
					it.remove();
										
				}else{
					
					break;
				}
			}
			
			if ( dos_list.size() > 0 ){
				
				synchronized( TRTrackerServerTCP.class ){
					
					if ( dos_log_file == null ){
											
						dos_log_file = new File( System.getProperty("user.dir" ) + File.separator + "dos.log" );
					}
					
					PrintWriter pw = null;
					
					try{
						
						pw = new PrintWriter( new FileWriter( dos_log_file, true ));
						
						for (int i=0;i<dos_list.size();i++){
							
							DOSEntry	this_entry = (DOSEntry)dos_list.get(i);
							
							String ts = new SimpleDateFormat("hh:mm:ss - ").format( new Date(this_entry.last_time ));
						
							pw.println( ts + this_entry.ip );
						}
						
					}catch( Throwable e ){
						
					}finally{
						
						dos_list.clear();
						
						if ( pw != null ){
							
							try{
								
								pw.close();
								
							}catch( Throwable e ){
							}
						}
					}
				}
			}
		}
		
		return( res );
	}
	
	protected boolean
	checkDOSRemove(
		Map.Entry		eldest )
	{
		boolean res = 	DOS_map.size() > MAX_DOS_ENTRIES || 
						last_dos_check - ((DOSEntry)eldest.getValue()).last_time > 	MAX_DOS_RETENTION;
				
		return( res );
	}
	
	protected class
	DOSEntry
	{
		String		ip;
		long		last_time;
		
		protected
		DOSEntry(
			String		_ip )
		{
			ip			= _ip;
			last_time	= last_dos_check;
		}
	}
	
	public int
	getPort()
	{
		return( port );
	}
	
	public String
	getHost()
	{
		return( COConfigurationManager.getStringParameter( "Tracker IP", "" ));
	}
	
	public boolean
	isSSL()
	{
		return( ssl );
	}
	
	
	protected boolean
	handleExternalRequest(
		String			client_address,
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
			
			if (listener.handleExternalRequest( client_address, url, header, is, os )){
				
				return( true );
			}
		}
		
		return( false );
	}
	
	protected void
	postProcess(
		TRTrackerServerPeerImpl		peer,
		TRTrackerServerTorrentImpl	torrent,
		int							type,
		Map							response )
	
		throws TRTrackerServerException
	{
		if ( request_listeners.size() > 0 ){
			
			TRTrackerServerRequestImpl	req = new TRTrackerServerRequestImpl( this, peer, torrent, type, response );
			
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
