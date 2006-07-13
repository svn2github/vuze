/*
 * Created on Jan 20, 2005
 * Created by Alon Rohter
 * Copyright (C) 2004-2005 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.peermanager;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.peer.PEPeerSource;
import org.gudy.azureus2.core3.peer.impl.*;
import org.gudy.azureus2.core3.peer.util.PeerIdentityManager;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.networkmanager.*;
import com.aelitis.azureus.core.networkmanager.impl.IncomingConnectionManager;
import com.aelitis.azureus.core.peermanager.download.TorrentDownload;
import com.aelitis.azureus.core.peermanager.download.TorrentDownloadFactory;
import com.aelitis.azureus.core.peermanager.messaging.*;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.*;

/**
 *
 */
public class PeerManager {
  private static final LogIDs LOGID = LogIDs.PEER;
	
  private static final PeerManager instance = new PeerManager();

  private static final int	PENDING_TIMEOUT	= 10*1000;
  
  private static final AEMonitor	timer_mon = new AEMonitor( "PeerManager:timeouts" );
  private static Thread	timer_thread;
  private static Set	timer_targets = new HashSet();
  
  protected static void
  registerForTimeouts(
	PeerManagerRegistrationImpl		reg )
  {
	  try{
		  timer_mon.enter();
		  
		  timer_targets.add( reg );
		  
		  if ( timer_thread == null ){
			  
			  	timer_thread = 
				  new AEThread( "PeerManager:timeouts", true )
				  {
					  public void
					  runSupport()
					  {
						  int	idle_time	= 0;
						  
						  while( true ){
							  
							  try{
								  Thread.sleep( PENDING_TIMEOUT / 2 );
								  
							  }catch( Throwable e ){  
							  }
							  
							  try{
								  timer_mon.enter();

								  if ( timer_targets.size() == 0 ){
									  
									  idle_time += PENDING_TIMEOUT / 2;
									  
									  if ( idle_time >= 30*1000 ){
										  										  
										  timer_thread = null;
										  
										  break;
									  }
								  }else{
									  
									  idle_time = 0;
									  
									  Iterator	it = timer_targets.iterator();
									  
									  while( it.hasNext()){
										  
										  PeerManagerRegistrationImpl	registration = (PeerManagerRegistrationImpl)it.next();
										  
										  if ( !registration.timeoutCheck()){
											  
											  it.remove();
										  }
									  }
								  }
							  }finally{
								  
								  timer_mon.exit();
							  }
						  }
					  }
				  };
				  
				timer_thread.start();
		  }
	  }finally{
		  
		  timer_mon.exit();
	  }
  }

  /**
   * Get the singleton instance of the peer manager.
   * @return the peer manager
   */
  public static PeerManager getSingleton() {  return instance;  }
  

   
  private final HashMap registered_legacy_managers 	= new HashMap();
   
  private final ByteBuffer legacy_handshake_header;
  
  private final AEMonitor	managers_mon = new AEMonitor( "PeerManager:managers" );
  

  private PeerManager() {
    legacy_handshake_header = ByteBuffer.allocate( 20 );
    legacy_handshake_header.put( (byte)BTHandshake.PROTOCOL.length() );
    legacy_handshake_header.put( BTHandshake.PROTOCOL.getBytes() );
    legacy_handshake_header.flip();
    
    MessageManager.getSingleton().initialize();  //ensure it gets initialized
    
    NetworkManager.ByteMatcher matcher =
    	new NetworkManager.ByteMatcher() 
    {
    	public int size() {  return 48;  }
    	public int minSize() { return 20; }

    	public Object
    	matches( 
    		InetSocketAddress	address,
    		ByteBuffer 			to_compare, 
    		int 				port ) 
    	{ 
    		int old_limit = to_compare.limit();
    		int old_position = to_compare.position();

    		to_compare.limit( old_position + 20 );

    		PeerManagerRegistrationImpl	routing_data = null;
    		
    		if( to_compare.equals( legacy_handshake_header ) ) {  //compare header 
    			to_compare.limit( old_position + 48 );
    			to_compare.position( old_position + 28 );

    			byte[]	hash = new byte[to_compare.remaining()];
    			
    			to_compare.get( hash );
    			
    			try{
    				managers_mon.enter();
    				  		
    				routing_data = (PeerManagerRegistrationImpl)registered_legacy_managers.get( new HashWrapper( hash ));
    				
    			}finally{
    				
    				managers_mon.exit();
    			}
    		}

    		//restore buffer structure
    		to_compare.limit( old_limit );
    		to_compare.position( old_position );

    		if ( routing_data != null ){
    			
    			if ( !routing_data.isActive()){
    			
    				if ( !routing_data.getAdapter().activateRequest( address )){
    				
    					routing_data = null;
    				}
    			}
    		}
    		return routing_data;
    	}
    	
    	public Object 
    	minMatches( 
    		InetSocketAddress	address,
    		ByteBuffer 			to_compare, 
    		int 				port ) 
    	{ 
     		boolean matches = false;

    		int old_limit = to_compare.limit();
    		int old_position = to_compare.position();

    		to_compare.limit( old_position + 20 );

    		if( to_compare.equals( legacy_handshake_header ) ) { 
    			matches = true;
    		}

    			//restore buffer structure
    		
    		to_compare.limit( old_limit );
    		to_compare.position( old_position );

    		return matches?"":null;
    	}

    	public byte[] 
    	getSharedSecret()
    	{
    		return( null );	// registered manually above
    	}
    };
    
    // register for incoming connection routing
    NetworkManager.getSingleton().requestIncomingConnectionRouting(
        matcher,
        new NetworkManager.RoutingListener() 
        {
        	public void 
        	connectionRouted( 
        		NetworkConnection 	connection, 
        		Object 				routing_data ) 
        	{
        		PeerManagerRegistrationImpl	registration = (PeerManagerRegistrationImpl)routing_data;
        		
        		registration.route( connection );
        	}
        	
        	public boolean
      	  	autoCryptoFallback()
        	{
        		return( false );
        	}
        	},
        new MessageStreamFactory() {
          public MessageStreamEncoder createEncoder() {  return new BTMessageEncoder();  }
          public MessageStreamDecoder createDecoder() {  return new BTMessageDecoder();  }
        });
  }
  
     
  public PeerManagerRegistration
  registerLegacyManager(
	HashWrapper						hash,
	PeerManagerRegistrationAdapter  adapter )
  {
	  try{
		  managers_mon.enter();
		  		
		  if ( registered_legacy_managers.get( hash ) != null ){
			  
			  Debug.out( "manager already registered" );
		  }
		  		  
		  IncomingConnectionManager.getSingleton().addSharedSecret( hash.getBytes());
		  
		  PeerManagerRegistration	registration = new PeerManagerRegistrationImpl( hash, adapter );
		  
		  registered_legacy_managers.put( hash, registration );
			  
		  return( registration );
	  }finally{
		  
		  managers_mon.exit();
	  }
  }
  
 
 
  private class
  PeerManagerRegistrationImpl
  	implements PeerManagerRegistration
  {
	private HashWrapper 					hash;
	private PeerManagerRegistrationAdapter	adapter;
	
	private TorrentDownload					download;
	
	private volatile PEPeerControl			active_control;
	
	private List	pending_connections;
	
	protected
	PeerManagerRegistrationImpl(
		HashWrapper						_hash,
		PeerManagerRegistrationAdapter	_adapter )
	{
		hash	= _hash;
		adapter	= _adapter;
	}
	
	protected HashWrapper
	getHash()
	{
		return( hash );
	}
	
	protected PeerManagerRegistrationAdapter
	getAdapter()
	{
		return( adapter );
	}
	
	public boolean
	isActive()
	{
		return( active_control != null );
	}
	
	public void
	activate(
		PEPeerControl	_active_control )
	{
		List	connections = null;
		
		try{
			  managers_mon.enter();

			  active_control = _active_control;
		
			  if ( download != null ){
				  
				  Debug.out( "Already activated" );
			  }
			  
			  download = TorrentDownloadFactory.getSingleton().createDownload( active_control );  //link legacy with new
			  
			  connections = pending_connections;
			  
			  pending_connections = null;
			  
		}finally{
		  
			managers_mon.exit();
		}
		
		if ( connections != null ){
			
			for (int i=0;i<connections.size();i++){
				
				Object[]	entry = (Object[])connections.get(i);
								
				NetworkConnection	nc = (NetworkConnection)entry[0];
				
				route( _active_control, nc );
			}
		}
	}
	
	public void
	deactivate()
	{
		  try{
			  managers_mon.enter();
	      
			  if ( download == null ){
				  
				  Debug.out( "Already deactivated" );
				  
			  }else{
				  
				  download.destroy();  //break legacy link
			  
				  download	= null;
			  }
			  
			  active_control = null;
			  
			  if ( pending_connections != null ){
				  
				  for (int i=0;i<pending_connections.size();i++){
					  
					  Object[]	entry = (Object[])pending_connections.get(i);
					  		
					  NetworkConnection	connection = (NetworkConnection)entry[0];
					                   	                                      
		              if (Logger.isEnabled())
							Logger.log(new LogEvent(LOGID, LogEvent.LT_WARNING,
											"Incoming connection from [" + connection
													+ "] closed due to deactivation" ));

					  connection.close();
				  }
			  
				  pending_connections = null;
			  }
			  
		  }finally{
			  
			  managers_mon.exit();
		  }
	}
	
	public void
	unregister()
	{
	  try{
		  managers_mon.enter();
		  		
		  if ( active_control != null ){
			  
			  Debug.out( "Not deactivated" );
			  
			  deactivate();
		  }
		  
		  IncomingConnectionManager.getSingleton().removeSharedSecret( hash.getBytes());
		  
		  if ( registered_legacy_managers.remove( hash ) == null ){
			  
			  Debug.out( "manager already deregistered" );
		  }
	  }finally{
		  
		  managers_mon.exit();
	  }
	}
	
	protected void
	route(
		NetworkConnection 	connection )
	{	
		PEPeerControl	control;
		
		boolean	register_for_timeouts = false;
		
		try{
			managers_mon.enter();

			control = active_control;
			
			if ( control == null ){
				
					// not yet activated, queue connection for use on activation
			
				if ( pending_connections != null && pending_connections.size() > 10 ){
					
	            	if (Logger.isEnabled())
						Logger.log(new LogEvent(LOGID, LogEvent.LT_WARNING,
										"Incoming connection from [" + connection
												+ "] dropped too many pending activations" ));
					connection.close();
					
				}else{
				
					if ( pending_connections == null ){
						
						pending_connections = new ArrayList();
					}
										
					pending_connections.add( new Object[]{ connection, new Long( SystemTime.getCurrentTime())});
					
					if ( pending_connections.size() == 1 ){
						
						register_for_timeouts	= true;
					}
				}
			}	
   		}finally{
   		
    		managers_mon.exit();
   		}
		
   			// do this outside the monitor as the timeout code calls us back holding the timeout monitor
   			// and we need to grab managers_mon inside this to run timeouts
   		
   		if ( register_for_timeouts ){
   			
			registerForTimeouts( this );
   		}
   		
		if ( control != null ){
    		
			route( control, connection );
   		}
	}
	
	protected boolean
	timeoutCheck()
	{
		try{
			managers_mon.enter();

			if ( pending_connections == null ){
				
				return( false );
			}
			
			Iterator it = pending_connections.iterator();
			
			long	now = SystemTime.getCurrentTime();
			
			while( it.hasNext()){
				
				Object[]	entry = (Object[])it.next();
				
				long	start_time = ((Long)entry[1]).longValue();
				
				if ( now < start_time ){
					
					entry[1] = new Long( now );
					
				}else if ( now - start_time > PENDING_TIMEOUT ){
					
					it.remove();
					
					NetworkConnection	connection = (NetworkConnection)entry[0];
                      
					if (Logger.isEnabled())
						Logger.log(new LogEvent(LOGID, LogEvent.LT_WARNING,
										"Incoming connection from [" + connection
												+ "] closed due to activation timeout" ));

					connection.close();		
				}
			}
			
			if ( pending_connections.size() == 0 ){
				
				pending_connections = null;
			}
			
			return( pending_connections != null );
			
		}finally{
			
			managers_mon.exit();
		}
	}
	
	protected void
	route(
		PEPeerControl		control,	
		NetworkConnection 	connection )
	{
	        // make sure not already connected to the same IP address; allow
	        // loopback connects for co-located proxy-based connections and
	        // testing
		
        String address = connection.getEndpoint().getNotionalAddress().getAddress().getHostAddress();
        
        boolean same_allowed = COConfigurationManager.getBooleanParameter( "Allow Same IP Peers" ) || address.equals( "127.0.0.1" );
        
        if( !same_allowed && PeerIdentityManager.containsIPAddress( control.getPeerIdentityDataID(), address ) ){
        	
        	if (Logger.isEnabled())
							Logger.log(new LogEvent(LOGID, LogEvent.LT_WARNING,
									"Incoming connection from [" + connection
											+ "] dropped as IP address already "
											+ "connected for ["
											+ control.getDisplayName() + "]"));
        	connection.close();
          
        	return;
        }
        
        if (Logger.isEnabled())
			Logger.log(new LogEvent(LOGID, "Incoming connection from ["
								+ connection + "] routed to legacy download ["
								+ control.getDisplayName() + "]"));
        
        control.addPeerTransport( PEPeerTransportFactory.createTransport( control, PEPeerSource.PS_INCOMING, connection ) );
	}
  }
}
