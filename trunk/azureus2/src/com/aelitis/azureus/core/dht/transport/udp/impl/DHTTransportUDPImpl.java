/*
 * Created on 21-Jan-2005
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

package com.aelitis.azureus.core.dht.transport.udp.impl;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;

import org.gudy.azureus2.core3.ipchecker.extipchecker.ExternalIPChecker;
import org.gudy.azureus2.core3.ipchecker.extipchecker.ExternalIPCheckerFactory;
import org.gudy.azureus2.core3.ipchecker.extipchecker.ExternalIPCheckerService;
import org.gudy.azureus2.core3.ipchecker.extipchecker.ExternalIPCheckerServiceListener;
import org.gudy.azureus2.core3.ipfilter.IpFilter;
import org.gudy.azureus2.core3.ipfilter.IpFilterManagerFactory;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.logging.LoggerChannel;

import com.aelitis.azureus.core.dht.transport.*;
import com.aelitis.azureus.core.dht.transport.udp.*;
import com.aelitis.azureus.core.dht.transport.util.DHTTransportRequestCounter;
import com.aelitis.azureus.core.versioncheck.VersionCheckClient;
import com.aelitis.net.udp.*;

/**
 * @author parg
 *
 */

public class 
DHTTransportUDPImpl 
	implements DHTTransportUDP, PRUDPRequestHandler
{
	public static boolean TEST_EXTERNAL_IP	= false;
		
	private static String	external_address;
	
	
	private int					port;
	private int					max_fails_for_live;
	private int					max_fails_for_unknown;
	private long				request_timeout;
	private long				store_timeout;
	private LoggerChannel		logger;
	
	private PRUDPPacketHandler			packet_handler;
	
	private DHTTransportRequestHandler	request_handler;
	
	private DHTTransportUDPContactImpl		local_contact;
	
	private long last_address_change;
	
	private List listeners	= new ArrayList();
	
	private IpFilter	ip_filter	= IpFilterManagerFactory.getSingleton().getIPFilter();

	
	private DHTTransportUDPStatsImpl	stats;

	private static final int CONTACT_HISTORY_MAX = 32;
	
	private Map	contact_history = 
		new LinkedHashMap(CONTACT_HISTORY_MAX,0.75f,true)
		{
		   protected boolean 
		   removeEldestEntry(
		   		Map.Entry eldest) 
		   {
		   	return size() > CONTACT_HISTORY_MAX;
		   }
		};
		
		// TODO: secure enough?
	
	private	Random		random = new Random( SystemTime.getCurrentTime());
	
	
	private static AEMonitor	class_mon	= new AEMonitor( "DHTTransportUDP:class" );
	
	private AEMonitor	this_mon	= new AEMonitor( "DHTTransportUDP" );

	public
	DHTTransportUDPImpl(
		int				_port,
		int				_max_fails_for_live,
		int				_max_fails_for_unknown,
		long			_timeout,
		LoggerChannel	_logger )
	
		throws DHTTransportException
	{
		port					= _port;
		max_fails_for_live		= _max_fails_for_live;
		max_fails_for_unknown	= _max_fails_for_unknown;
		request_timeout			= _timeout;
		logger					= _logger;
				
		store_timeout			= request_timeout * 2;
		
		DHTUDPPacket.registerCodecs( logger );

			// DHTPRUDPPacket relies on the request-handler being an instanceof THIS so watch out
			// if you change it :)
		
		packet_handler = PRUDPPacketHandlerFactory.getHandler( _port, this );		

			// limit send and receive rates. Receive rate is lower as we want a stricter limit
			// on the max speed we generate packets than those we're willing to process.
		
		packet_handler.setDelays( 100, 50 );
		
		stats =  new DHTTransportUDPStatsImpl( packet_handler.getStats());
		
		InetSocketAddress	address = 
			new InetSocketAddress( getExternalAddress(false,"127.0.0.1", logger), port );

		logger.log( "Initial external address: " + address );
		
		local_contact = new DHTTransportUDPContactImpl( this, address, address, DHTUDPPacket.VERSION, random.nextInt(), 0);
	}
	
	public void
	testInstanceIDChange()
	
		throws DHTTransportException
	{
		local_contact = new DHTTransportUDPContactImpl( this, local_contact.getTransportAddress(), local_contact.getExternalAddress(), DHTUDPPacket.VERSION, random.nextInt(), 0);		
	}
	
	public void
	testTransportIDChange()
	
		throws DHTTransportException
	{
		if ( external_address.equals("127.0.0.1")){
			
			external_address = "192.168.0.2";
		}else{
			
			external_address = "127.0.0.1";
		}
		
		InetSocketAddress	address = new InetSocketAddress( external_address, port );
		
		local_contact = new DHTTransportUDPContactImpl( this, address, address, DHTUDPPacket.VERSION, local_contact.getInstanceID(), 0 );		

		for (int i=0;i<listeners.size();i++){
			
			try{
				((DHTTransportListener)listeners.get(i)).localContactChanged( local_contact );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
	public void
	testExternalAddressChange()
	{
		try{
			DHTTransportUDPContactImpl c = (DHTTransportUDPContactImpl)contact_history.values().iterator().next();
			
			externalAddressChange( c, c.getExternalAddress());
			//externalAddressChange( c, new InetSocketAddress( "192.168.0.7", 6881 ));
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	
	protected String
	getExternalAddress(
		boolean				force,
		String				default_address,
		final LoggerChannel	log )
	{
			// class level synchronisation is for testing purposes when running multiple UDP instances
			// in the same VM
		
		try{
			class_mon.enter();
			
			if ( force || external_address == null ){
				
				String new_external_address = null;
				
				try{
					
					log.log( "Obtaining external address" );
					
					if ( TEST_EXTERNAL_IP ){
						
						new_external_address	= "192.168.0.2";
						
						log.log( "    External IP address obtained from test data: " + new_external_address );
					}
										
					if ( new_external_address == null ){

							// First attempt is via other contacts we know about. Select three
						
						List	contacts;
						
						try{
							this_mon.enter();
							
							contacts = new ArrayList( contact_history.values());
							
						}finally{
							
							this_mon.exit();
						}
						
							// randomly select up to 10 entries to ping until we 
							// get three replies
						
						String	returned_address 	= null;
						int		returned_matches	= 0;
						
						int		search_lim = Math.min(10, contacts.size());
						
						log.log( "    Contacts to search = " + search_lim );
						
						for (int i=0;i<search_lim;i++){
							
							DHTTransportUDPContactImpl	contact = (DHTTransportUDPContactImpl)contacts.remove((int)(contacts.size()*Math.random()));
														
							InetSocketAddress a = askContactForExternalAddress( contact );
							
							if ( a != null && a.getAddress() != null ){
								
								String	ip = a.getAddress().getHostAddress();
								
								if ( returned_address == null ){
									
									returned_address = ip;
									
									log.log( "    : contact " + contact.getString() + " reported external address as '" + ip + "'" );
									
									returned_matches++;
									
								}else if ( returned_address.equals( ip )){
									
									returned_matches++;
									
									log.log( "    : contact " + contact.getString() + " also reported external address as '" + ip + "'" );
									
									if ( returned_matches == 3 ){
										
										new_external_address	= returned_address;
										
										log.log( "    External IP address obtained from contacts: "  + returned_address );
										
										break;
									}
								}else{
									
									log.log( "    : contact " + contact.getString() + " reported external address as '" + ip + "', abandoning due to mismatch" );
									
										// mismatch - give up
									
									break;
								}
							}else{
								
								log.log( "    : contact " + contact.getString() + " didn't reply" );
							}
						}

					}
					
					if ( new_external_address == null ){
				
						String	vc_ip = VersionCheckClient.getSingleton().getExternalIpAddress();
						
						if ( vc_ip != null && vc_ip.length() > 0 ){
							
							log.log( "    External IP address obtained from version-check: " + vc_ip );
							
							new_external_address	= vc_ip;
						}
					}
					
					if ( new_external_address == null ){
						
						ExternalIPChecker	checker = ExternalIPCheckerFactory.create();
						
						ExternalIPCheckerService[]	services = checker.getServices();
						
						final String[]	ip = new String[]{ null };
						
						for (int i=0;i<services.length && ip[0] == null;i++){
							
							final ExternalIPCheckerService	service = services[i];
							
							if ( service.supportsCheck()){
			
								final AESemaphore	sem = new AESemaphore("DHTUDP:getExtIP");
								
								ExternalIPCheckerServiceListener	listener = 
									new ExternalIPCheckerServiceListener()
									{
										public void
										checkComplete(
											ExternalIPCheckerService	_service,
											String						_ip )
										{
											log.log( "    External IP address obtained from " + service.getName() + ": " + _ip );
	
											ip[0]	= _ip;
											
											sem.release();
										}
											
										public void
										checkFailed(
											ExternalIPCheckerService	_service,
											String						_reason )
										{
											sem.release();
										}
											
										public void
										reportProgress(
											ExternalIPCheckerService	_service,
											String						_message )
										{
										}
									};
									
								services[i].addListener( listener );
								
								try{
									
									services[i].initiateCheck( 60000 );
									
									sem.reserve( 60000 );
									
								}finally{
									
									services[i].removeListener( listener );
								}
							}
						}
						
						if ( ip[0] != null ){
							
							new_external_address	= ip[0];
						}
					}
					
				}catch( Throwable e ){
					
					Debug.printStackTrace( e );
				}
			
				if ( new_external_address == null ){
				
					new_external_address =	default_address;
				
					log.log( "    External IP address defaulted:  " + new_external_address );
				}
				
				external_address = new_external_address;
			}
			
			return( external_address );
			
		}finally{
			
			class_mon.exit();
		}
	}
	
	protected boolean
	externalAddressChange(
		DHTTransportUDPContactImpl	reporter,
		InetSocketAddress			new_address )
	
		throws DHTTransportException
	{
			/*
			 * A node has reported that our external address and the one he's seen a 
			 * message coming from differ. Natural explanations are along the lines of
			 * 1) my address is dynamically allocated by my ISP and it has changed
			 * 2) I have multiple network interfaces 
			 * 3) there's some kind of proxy going on
			 * 4) this is a DOS attempting to stuff me up
			 * 
			 * We assume that our address won't change more frequently than once every
			 * 5 minutes
			 * We assume that if we can successfully obtain an external address by
			 * using the above explicit check then this is accurate
			 * Only in the case where the above check fails do we believe the address
			 * that we've been told about
			 */
			
		InetAddress	ia = new_address.getAddress();
		
		if ( ia == null ){
			
			Debug.out( "reported new external address '" + new_address + "' is unresolved" );
			
			throw( new DHTTransportException( "Address '" + new_address + "' is unresolved" ));
		}
		
		String	new_ip = ia.getHostAddress();
		
		if ( new_ip.equals( external_address )){
			
				// probably just be a second notification of an address change, return
				// "ok to retry" as it should now work
							
			return( true );
		}
		
		try{
			this_mon.enter();
	
			long	now = SystemTime.getCurrentTime();
	
			if ( now - last_address_change < 5*60*1000 ){
				
				return( false );
			}
			
			logger.log( "Node " + reporter.getString() + " has reported that the external IP address is '" + new_address + "'" );
	
				// check for dodgy addresses that shouldn't appear as an external address!
			
			if ( 	ia.isLinkLocalAddress() ||
					ia.isLoopbackAddress() ||
					new_ip.startsWith( "192.168." )){
				
				logger.log( "     This is invalid as it is a private address." );
	
				return( false );
			}
	
				// another situation to ignore is where the reported address is the same as
				// the reporter (they must be seeing it via, say, socks connection on a local
				// interface
			
			if ( reporter.getExternalAddress().getAddress().getHostAddress().equals( new_ip )){
				
				logger.log( "     This is invalid as it is the same as the reporter's address." );
	
				return( false );		
			}
			
			last_address_change	= now;
			
		}finally{
			
			this_mon.exit();
		}
		
		String	a = getExternalAddress( true, new_ip, logger );
		
		if ( a.equals( external_address )){
			
				// address hasn't changed, notifier must be perceiving different address
				// due to proxy or something
							
			return( true );
		}
		
		InetSocketAddress	s_address = new InetSocketAddress( a, port );
		
		logger.log( "External address changed: " + s_address );
		
		local_contact = new DHTTransportUDPContactImpl( this, s_address, s_address, DHTUDPPacket.VERSION, random.nextInt(), 0);

		for (int i=0;i<listeners.size();i++){
			
			try{
				((DHTTransportListener)listeners.get(i)).localContactChanged( local_contact );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
		
		return( true );
	}
	
	protected void
	contactAlive(
		DHTTransportUDPContactImpl	contact )
	{
		try{
			this_mon.enter();
			
			contact_history.put( contact.getTransportAddress(), contact );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected int
	getMaxFailForLiveCount()
	{
		return( max_fails_for_live );
	}
	
	protected int
	getMaxFailForUnknownCount()
	{
		return( max_fails_for_unknown );
	}
	
	public DHTTransportContact
	getLocalContact()
	{
		return( local_contact );
	}
	
	public DHTTransportContact
	importContact(
		DataInputStream		is )
	
		throws IOException, DHTTransportException
	{
		DHTTransportContact	contact = DHTUDPUtils.deserialiseContact( this, is );
		
		request_handler.contactImported( contact );
				
		logger.log( "Imported contact " + contact.getString());
		
		return( contact );
	}
	
	public DHTTransportContact
	importContact(
		InetSocketAddress	address,
		byte				protocol_version )
	
		throws DHTTransportException
	{
			// instance id of 0 means "unknown"
		
		DHTTransportContact	contact = new DHTTransportUDPContactImpl( this, address, address, protocol_version, 0, 0 );
		
		request_handler.contactImported( contact );
		
		logger.log( "Imported contact " + contact.getString());

		return( contact );
	}
	
	public void
	exportContact(
		DHTTransportContact	contact,
		DataOutputStream	os )
	
		throws IOException, DHTTransportException
	{
		DHTUDPUtils.serialiseContact( os, contact );
	}
	
	public void
	setRequestHandler(
		DHTTransportRequestHandler	_request_handler )
	{
		request_handler = new DHTTransportRequestCounter( _request_handler, stats );
	}
	
	public DHTTransportStats
	getStats()
	{
		return( stats );
	}
	
	protected void
	checkAddress(
		DHTTransportUDPContactImpl		contact )
	
		throws PRUDPPacketHandlerException
	{
		if ( ip_filter.isInRange( contact.getTransportAddress().getAddress().getHostAddress(), "DHT" )){
			
			throw( new PRUDPPacketHandlerException( "IPFilter check fails" ));
		}
	}
	
	protected void
	sendPing(
		final DHTTransportUDPContactImpl	contact,
		final DHTTransportReplyHandler		handler )
	{
		stats.pingSent();

		final long	connection_id = getConnectionID();
		
		final DHTUDPPacketRequestPing	request = 
			new DHTUDPPacketRequestPing( connection_id, local_contact );
			
		try{
			checkAddress( contact );
			
			packet_handler.sendAndReceive(
				request,
				contact.getTransportAddress(),
				new PRUDPPacketReceiver()
				{
					private int	retry_count	= 0;
					
					public void
					packetReceived(
						PRUDPPacket			_packet,
						InetSocketAddress	from_address )
					{
						try{
							DHTUDPPacketReply	packet = (DHTUDPPacketReply)_packet;
							
							if ( packet.getConnectionId() != connection_id ){
								
								throw( new Exception( "connection id mismatch" ));
							}
							
							contact.setInstanceID( packet.getTargetInstanceID());
							
							if ( handleErrorReply( contact, packet )){
								
								retry_count++;
								
								if ( retry_count > 1 ){
									
									error( new PRUDPPacketHandlerException("retry limit exceeded"));
									
								}else{
									
									request.setOriginatorAddress(local_contact.getExternalAddress());
									
									packet_handler.sendAndReceive(
											request,
											contact.getTransportAddress(),
											this,
											request_timeout, false );
								}
							}else{
								
								stats.pingOK();
							
								handler.pingReply( contact );
							}
						}catch( PRUDPPacketHandlerException e ){
							
							error( e );
							
						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
							
							error( new PRUDPPacketHandlerException( "ping failed", e ));
						}
					}
					
					public void
					error(
						PRUDPPacketHandlerException	e )
					{
						stats.pingFailed();
						
						handler.failed( contact,e );
					}
				},
				request_timeout, false );
			
		}catch( Throwable e ){
			
			stats.pingFailed();
			
			handler.failed( contact,e );
		}
	}
	
		// stats
	
	protected void
	sendStats(
		final DHTTransportUDPContactImpl	contact,
		final DHTTransportReplyHandler		handler )
	{
		stats.statsSent();

		final long	connection_id = getConnectionID();
		
		final DHTUDPPacketRequestStats	request = 
			new DHTUDPPacketRequestStats( connection_id, local_contact );
			
		try{
			checkAddress( contact );
			
			packet_handler.sendAndReceive(
				request,
				contact.getTransportAddress(),
				new PRUDPPacketReceiver()
				{
					private int	retry_count	= 0;
					
					public void
					packetReceived(
						PRUDPPacket			_packet,
						InetSocketAddress	from_address )
					{
						try{
							DHTUDPPacketReply	packet = (DHTUDPPacketReply)_packet;
							
							if ( packet.getConnectionId() != connection_id ){
								
								throw( new Exception( "connection id mismatch" ));
							}
							
							contact.setInstanceID( packet.getTargetInstanceID());
							
							if ( handleErrorReply( contact, packet )){
								
								retry_count++;
								
								if ( retry_count > 1 ){
									
									error( new PRUDPPacketHandlerException("retry limit exceeded"));
									
								}else{
									
									request.setOriginatorAddress(local_contact.getExternalAddress());
									
									packet_handler.sendAndReceive(
											request,
											contact.getTransportAddress(),
											this,
											request_timeout, true );
								}
							}else{
								
								DHTUDPPacketReplyStats	reply = (DHTUDPPacketReplyStats)packet;

								stats.statsOK();
							
								handler.statsReply( contact, reply.getStats());
							}
						}catch( PRUDPPacketHandlerException e ){
							
							error( e );
							
						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
							
							error( new PRUDPPacketHandlerException( "stats failed", e ));
						}
					}
					
					public void
					error(
						PRUDPPacketHandlerException	e )
					{
						stats.statsFailed();
						
						handler.failed( contact, e );
					}
				},
				request_timeout, true );
			
		}catch( Throwable e ){
			
			stats.statsFailed();
			
			handler.failed( contact, e );
		}
	}
	
		// PING for deducing external IP address
	
	protected InetSocketAddress
	askContactForExternalAddress(
		DHTTransportUDPContactImpl	contact )
	{
		stats.pingSent();

		final long	connection_id = getConnectionID();
	
		final DHTUDPPacketRequestPing	request = 
			new DHTUDPPacketRequestPing( connection_id, local_contact );
		
		try{
			checkAddress( contact );
		
			final AESemaphore	sem = new AESemaphore( "DHTTransUDP:extping" );

			final InetSocketAddress[]	result = new InetSocketAddress[1];
			
			packet_handler.sendAndReceive(
				request,
				contact.getTransportAddress(),
				new PRUDPPacketReceiver()
				{
					public void
					packetReceived(
						PRUDPPacket			_packet,
						InetSocketAddress	from_address )
					{
						try{
							
							if ( _packet instanceof DHTUDPPacketReplyPing ){
								
								// ping was OK so current address is OK
								
								result[0] = local_contact.getExternalAddress();
								
							}else if ( _packet instanceof DHTUDPPacketReplyError ){
								
								DHTUDPPacketReplyError	packet = (DHTUDPPacketReplyError)_packet;
								
								if ( packet.getErrorType() == DHTUDPPacketReplyError.ET_ORIGINATOR_ADDRESS_WRONG ){
									
									result[0] = packet.getOriginatingAddress();
								}
							}
						}finally{
							
							sem.release();
						}
					}
					
					public void
					error(
						PRUDPPacketHandlerException	e )
					{
						try{
							stats.pingFailed();
							
						}finally{
						
							sem.release();
						}
					}
				},
				5000, false );
			
			sem.reserve( 5000 );
			
			return( result[0] );
			
		}catch( Throwable e ){
			
			stats.pingFailed();

			return( null );
		}
	}
	
		// STORE
	
	public void
	sendStore(
		final DHTTransportUDPContactImpl	contact,
		final DHTTransportReplyHandler		handler,
		byte[][]							keys,
		DHTTransportValue[][]				value_sets )
	{
		stats.storeSent();
		
		final long	connection_id = getConnectionID();
		
		try{
			checkAddress( contact );
			
			final DHTUDPPacketRequestStore	request = 
				new DHTUDPPacketRequestStore( connection_id, local_contact );
			
			request.setKeys( keys );
			
			request.setValueSets( value_sets );
			
			packet_handler.sendAndReceive(
				request,
				contact.getTransportAddress(),
				new PRUDPPacketReceiver()
				{
					private int	retry_count	= 0;
					
					public void
					packetReceived(
						PRUDPPacket			_packet,
						InetSocketAddress	from_address )
					{
						try{
							DHTUDPPacketReply	packet = (DHTUDPPacketReply)_packet;
							
							if ( packet.getConnectionId() != connection_id ){
								
								throw( new Exception( "connection id mismatch" ));
							}
							
							contact.setInstanceID( packet.getTargetInstanceID());
							
							if ( handleErrorReply( contact, packet )){
								
								retry_count++;
								
								if ( retry_count > 1 ){
									
									error( new PRUDPPacketHandlerException("retry limit exceeded"));
									
								}else{
									
									request.setOriginatorAddress(local_contact.getExternalAddress());
									
									packet_handler.sendAndReceive(
											request,
											contact.getTransportAddress(),
											this,
											store_timeout, true );
								}
							}else{
								
								stats.storeOK();
															
								handler.storeReply( contact );
							}
							
						}catch( PRUDPPacketHandlerException e ){
							
							error( e );
							
						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
							
							error( new PRUDPPacketHandlerException( "store failed", e ));
						}
					}
					
					public void
					error(
						PRUDPPacketHandlerException	e )
					{
						stats.storeFailed();
												
						handler.failed( contact, e );
					}
				},
				store_timeout,
				true );	// low priority
			
		}catch( Throwable e ){
						
			stats.storeFailed();
						
			handler.failed( contact, e );
		}
	}
	
		// FIND NODE
	
	public void
	sendFindNode(
		final DHTTransportUDPContactImpl	contact,
		final DHTTransportReplyHandler		handler,
		byte[]								nid )
	{
		stats.findNodeSent();
		
		final long	connection_id = getConnectionID();
		
		try{
			checkAddress( contact );
			
			final DHTUDPPacketRequestFindNode	request = 
				new DHTUDPPacketRequestFindNode( connection_id, local_contact );
			
			request.setID( nid );
			
			packet_handler.sendAndReceive(
				request,
				contact.getTransportAddress(),
				new PRUDPPacketReceiver()
				{
					private int	retry_count	= 0;
					
					public void
					packetReceived(
						PRUDPPacket			_packet,
						InetSocketAddress	from_address )
					{
						try{
							DHTUDPPacketReply	packet = (DHTUDPPacketReply)_packet;
														
							if ( packet.getConnectionId() != connection_id ){
								
								throw( new Exception( "connection id mismatch" ));
							}

							contact.setInstanceID( packet.getTargetInstanceID());
							
							if ( handleErrorReply( contact, packet )){
								
								retry_count++;
								
								if ( retry_count > 1 ){
									
									error( new PRUDPPacketHandlerException("retry limit exceeded"));
									
								}else{
									
									request.setOriginatorAddress(local_contact.getExternalAddress());
									
									packet_handler.sendAndReceive(
											request,
											contact.getTransportAddress(),
											this,
											request_timeout, false );
								}
							}else{
								
								DHTUDPPacketReplyFindNode	reply = (DHTUDPPacketReplyFindNode)packet;
							
								stats.findNodeOK();
								
								handler.findNodeReply( contact, reply.getContacts());
							}
						}catch( PRUDPPacketHandlerException e ){
							
							error( e );
							
						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
							
							error( new PRUDPPacketHandlerException( "findNode failed", e ));
						}
					}
					
					public void
					error(
						PRUDPPacketHandlerException	e )
					{
						stats.findNodeFailed();
						
						handler.failed( contact, e );
					}
				},
				request_timeout, false );
			
		}catch( Throwable e ){
			
			stats.findNodeFailed();
			
			handler.failed( contact, e );
		}
	}
	
		// FIND VALUE
	
	public void
	sendFindValue(
		final DHTTransportUDPContactImpl	contact,
		final DHTTransportReplyHandler		handler,
		byte[]								key,
		int									max_values,
		byte								flags )
	{
		stats.findValueSent();

		final long	connection_id = getConnectionID();
		
		try{
			checkAddress( contact );
			
			final DHTUDPPacketRequestFindValue	request = 
				new DHTUDPPacketRequestFindValue( connection_id, local_contact );
			
			request.setID( key );
			
			request.setMaximumValues( max_values );
			
			request.setFlags( flags );
			
			packet_handler.sendAndReceive(
				request,
				contact.getTransportAddress(),
				new PRUDPPacketReceiver()
				{
					private int	retry_count	= 0;
					
					public void
					packetReceived(
						PRUDPPacket			_packet,
						InetSocketAddress	from_address )
					{
						try{
							DHTUDPPacketReply	packet = (DHTUDPPacketReply)_packet;
							
							if ( packet.getConnectionId() != connection_id ){
								
								throw( new Exception( "connection id mismatch" ));
							}
							
							contact.setInstanceID( packet.getTargetInstanceID());
							
							if ( handleErrorReply( contact, packet )){
								
								retry_count++;
								
								if ( retry_count > 1 ){
									
									error( new PRUDPPacketHandlerException("retry limit exceeded"));
									
								}else{
									
									request.setOriginatorAddress(local_contact.getExternalAddress());
									
									packet_handler.sendAndReceive(
											request,
											contact.getTransportAddress(),
											this,
											request_timeout, false );
								}
							}else{
								
								DHTUDPPacketReplyFindValue	reply = (DHTUDPPacketReplyFindValue)packet;
								
								stats.findValueOK();
								
								DHTTransportValue[]	res = reply.getValues();
								
								if ( res != null ){
									
									handler.findValueReply( contact, res );
									
								}else{
									
									handler.findValueReply( contact, reply.getContacts());
								}
							}
						}catch( PRUDPPacketHandlerException e ){
							
							error( e );
							
						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
							
							error( new PRUDPPacketHandlerException( "findValue failed", e ));
						}
					}
					
					public void
					error(
						PRUDPPacketHandlerException	e )
					{
						stats.findValueFailed();
						
						handler.failed( contact, e );
					}
				},
				request_timeout, false );
			
		}catch( Throwable e ){
			
			if ( !(e instanceof PRUDPPacketHandlerException )){
				
				stats.findValueFailed();
			
				handler.failed( contact, e );
			}
		}
	}
	
	protected DHTTransportFullStats
	getFullStats(
		DHTTransportUDPContactImpl	contact )
	{
		if ( contact == local_contact ){
			
			return( request_handler.statsRequest( contact ));
		}
		
		final DHTTransportFullStats[] res = { null };
		
		final AESemaphore	sem = new AESemaphore( "DHTTransportUDP:getFullStats");
		
		sendStats(	contact,
					new DHTTransportReplyHandlerAdapter()
					{
						public void
						statsReply(
							DHTTransportContact 	_contact,
							DHTTransportFullStats	_stats )
						{
							res[0]	= _stats;
							
							sem.release();
						}
						
						public void
						failed(
							DHTTransportContact 	_contact,
							Throwable				_error )
						{
							sem.release();
						}
				
					});
		
		sem.reserve();

		return( res[0] );
	}
	
	public void
	process(
		PRUDPPacketRequest	_request )
	{
		if ( request_handler == null ){
			
			logger.log( "Ignoring packet as not yet ready to process" );
			
			return;
		}
		
		try{
			stats.incomingRequestReceived();
			
			DHTUDPPacketRequest	request = (DHTUDPPacketRequest)_request;
			
			InetSocketAddress	transport_address = request.getAddress();
			
			DHTTransportUDPContactImpl	originating_contact = 
				new DHTTransportUDPContactImpl( 
						this, 
						transport_address, 
						request.getOriginatorAddress(), 
						request.getVersion(),
						request.getOriginatorInstanceID(),
						request.getClockSkew());
			
			try{
				checkAddress( originating_contact );
					
			}catch( PRUDPPacketHandlerException e ){
				
				return;
			}

			if ( !originating_contact.isValid()){
				
				logger.log( "Node " + originating_contact.getString() + " has incorrect ID, reporting it to them" );
				
				DHTUDPPacketReplyError	reply = 
					new DHTUDPPacketReplyError(
							request.getTransactionId(),
							request.getConnectionId(),
							local_contact );
				
				reply.setErrorType( DHTUDPPacketReplyError.ET_ORIGINATOR_ADDRESS_WRONG );
				
				reply.setOriginatingAddress( originating_contact.getTransportAddress());
				
				packet_handler.send( reply, request.getAddress());

			}else{
				
				contactAlive( originating_contact );
				
				if ( request instanceof DHTUDPPacketRequestPing ){
					
					request_handler.pingRequest( originating_contact );
					
					DHTUDPPacketReplyPing	reply = 
						new DHTUDPPacketReplyPing(
								request.getTransactionId(),
								request.getConnectionId(),
								local_contact );
					
					packet_handler.send( reply, request.getAddress());
					
				}else if ( request instanceof DHTUDPPacketRequestStats ){
					
					DHTTransportFullStats	full_stats = request_handler.statsRequest( originating_contact );
					
					DHTUDPPacketReplyStats	reply = 
						new DHTUDPPacketReplyStats(
								request.getTransactionId(),
								request.getConnectionId(),
								local_contact );
					
					reply.setStats( full_stats );
					
					packet_handler.send( reply, request.getAddress());

				}else if ( request instanceof DHTUDPPacketRequestStore ){
					
					DHTUDPPacketRequestStore	store_request = (DHTUDPPacketRequestStore)request;
					
					request_handler.storeRequest(
							originating_contact, 
							store_request.getKeys(), 
							store_request.getValueSets());
					
					DHTUDPPacketReplyStore	reply = 
						new DHTUDPPacketReplyStore(
								request.getTransactionId(),
								request.getConnectionId(),
								local_contact );
					
					packet_handler.send( reply, request.getAddress());
					
				}else if ( request instanceof DHTUDPPacketRequestFindNode ){
					
					DHTUDPPacketRequestFindNode	find_request = (DHTUDPPacketRequestFindNode)request;
					
					DHTTransportContact[] res = 
						request_handler.findNodeRequest(
									originating_contact,
									find_request.getID());
					
					DHTUDPPacketReplyFindNode	reply = 
						new DHTUDPPacketReplyFindNode(
								request.getTransactionId(),
								request.getConnectionId(),
								local_contact );
								
					reply.setContacts( res );
					
					packet_handler.send( reply, request.getAddress());
					
				}else if ( request instanceof DHTUDPPacketRequestFindValue ){
					
					DHTUDPPacketRequestFindValue	find_request = (DHTUDPPacketRequestFindValue)request;
				
					Object res = 
						request_handler.findValueRequest(
									originating_contact,
									find_request.getID(),
									find_request.getMaximumValues(),
									find_request.getFlags());
					
					DHTUDPPacketReplyFindValue	reply = 
						new DHTUDPPacketReplyFindValue(
							request.getTransactionId(),
							request.getConnectionId(),
							local_contact );
					
					if ( res instanceof DHTTransportValue[] ){
						
						reply.setValues((DHTTransportValue[])res);
	
					}else{
						
						reply.setContacts((DHTTransportContact[])res );
					}
					
					packet_handler.send( reply, request.getAddress());
					
				}else{
					
					Debug.out( "Unexpected packet:" + request.toString());
				}
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	
		/**
		 * Returns false if this isn't an error reply, true if it is and a retry can be
		 * performed, throws an exception otherwise
		 * @param reply
		 * @return
		 * @throws PRUDPPacketHandlerException
		 */
	
	protected boolean
	handleErrorReply(
		DHTTransportUDPContactImpl	contact,
		DHTUDPPacketReply			reply )
	
		throws PRUDPPacketHandlerException
	{
		if ( reply.getAction() == DHTUDPPacket.ACT_REPLY_ERROR ){
			
			DHTUDPPacketReplyError	error = (DHTUDPPacketReplyError)reply;
			
			boolean	ok_to_retry = false;
			
			switch( error.getErrorType()){
			
				case DHTUDPPacketReplyError.ET_ORIGINATOR_ADDRESS_WRONG:
				{
					try{
						ok_to_retry = externalAddressChange( contact, error.getOriginatingAddress());
						
					}catch( DHTTransportException e ){
						
						Debug.printStackTrace(e);
					}
					
					break;
				}
				default:
				{
					Debug.out( "Unknown error type received" );
					
					break;
				}
			}
			
			if ( ok_to_retry ){
				
				return( true );
				
			}else{
				
				throw( new PRUDPPacketHandlerException( "retry no permitted" ));
			}
			
		}else{
			
			contactAlive( contact );
			
			return( false );
		}
	}
	
	protected long
	getConnectionID()
	{
			// unfortunately, to reuse the UDP port with the tracker protocol we 
			// have to distinguish our connection ids by setting the MSB. This allows
			// the decode to work as there is no common header format for the request
			// and reply packets
		
			// note that tracker usage of UDP via this handler is only for outbound
			// messages, hence for that use a request will never be received by the
			// handler
		
		return( 0x8000000000000000L | random.nextLong());
	}
	
	public void
	addListener(
		DHTTransportListener	l )
	{
		listeners.add(l);
	}
	
	public void
	removeListener(
		DHTTransportListener	l )
	{
		listeners.remove(l);
	}
}
