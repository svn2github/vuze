/*
 * Created on Jan 30, 2008
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


package com.aelitis.azureus.plugins.net.netstatus;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.*;

import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.peer.impl.PEPeerTransport;
import org.gudy.azureus2.core3.peer.impl.transport.PEPeerTransportProtocol;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.ddb.DistributedDatabase;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseContact;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseException;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseKey;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseProgressListener;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseTransferHandler;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseTransferType;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseValue;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.config.ActionParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.config.StringParameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.networkmanager.ConnectionEndpoint;
import com.aelitis.azureus.core.networkmanager.IncomingMessageQueue;
import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.networkmanager.OutgoingMessageQueue;
import com.aelitis.azureus.core.networkmanager.ProtocolEndpoint;
import com.aelitis.azureus.core.networkmanager.Transport;
import com.aelitis.azureus.core.networkmanager.impl.TransportHelper;
import com.aelitis.azureus.core.networkmanager.impl.tcp.ProtocolEndpointTCP;
import com.aelitis.azureus.core.networkmanager.impl.tcp.TCPNetworkManager;
import com.aelitis.azureus.core.peermanager.PeerManager;
import com.aelitis.azureus.core.peermanager.PeerManagerRegistration;
import com.aelitis.azureus.core.peermanager.PeerManagerRegistrationAdapter;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamDecoder;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamEncoder;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamFactory;
import com.aelitis.azureus.core.peermanager.messaging.azureus.AZBadPiece;
import com.aelitis.azureus.core.peermanager.messaging.azureus.AZHandshake;
import com.aelitis.azureus.core.peermanager.messaging.azureus.AZHave;
import com.aelitis.azureus.core.peermanager.messaging.azureus.AZMessage;
import com.aelitis.azureus.core.peermanager.messaging.azureus.AZMessageDecoder;
import com.aelitis.azureus.core.peermanager.messaging.azureus.AZMessageEncoder;
import com.aelitis.azureus.core.peermanager.messaging.azureus.AZPeerExchange;
import com.aelitis.azureus.core.peermanager.messaging.azureus.AZRequestHint;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTBitfield;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTCancel;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTChoke;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTDHTPort;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTHandshake;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTHave;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTInterested;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTMessage;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTMessageDecoder;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTMessageEncoder;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTMessageFactory;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTPiece;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTRequest;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTUnchoke;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTUninterested;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.ltep.LTHandshake;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.ltep.LTMessage;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.ltep.UTPeerExchange;
import com.aelitis.azureus.plugins.dht.DHTPlugin;
import com.aelitis.azureus.plugins.net.netstatus.swt.NetStatusPluginView;

public class 
NetStatusPlugin
	implements Plugin, DistributedDatabaseTransferHandler
{
	public static final String VIEW_ID = "aznetstatus";
	
	private LoggerChannel	logger;
	
	private StringParameter ping_target;
	private StringParameter test_address;
	
	private DistributedDatabase	ddb;
	
	private testXferType		transfer_type;
	
	private DHTPlugin			dht_plugin;
	
	
	private Random	random = new SecureRandom();
	
	private PeerManagerRegistration		pm_reg;
	private byte[]						pm_hash;
	
	
	public void
	initialize(
		final PluginInterface		plugin_interface )
	{
		String name_res = "Views.plugins." + VIEW_ID + ".title";
		
		String name = 
			plugin_interface.getUtilities().getLocaleUtilities().getLocalisedMessageText( name_res );
		
		plugin_interface.getPluginProperties().setProperty( "plugin.version", 	"1.0" );
		plugin_interface.getPluginProperties().setProperty( "plugin.name", 		name );

		logger = plugin_interface.getLogger().getChannel( "NetStatus" );
		
		logger.setDiagnostic();
		
		transfer_type = new testXferType();
		
		BasicPluginConfigModel config = plugin_interface.getUIManager().createBasicPluginConfigModel( name_res );
		
		ping_target = config.addStringParameter2( "plugin.aznetstatus.pingtarget", "plugin.aznetstatus.pingtarget", "www.google.com" );
		
		if ( Constants.isCVSVersion()){
			
			test_address = config.addStringParameter2( "plugin.aznetstatus.test_address", "plugin.aznetstatus.test_address", "" );
	
			ActionParameter test = config.addActionParameter2( "test", "test " );
			
			test.addListener(
				new ParameterListener()
				{
					public void
					parameterChanged(
						Parameter	param )
					{
						runTest();
					}
				});
		}
		
		plugin_interface.getUIManager().addUIListener(
			new UIManagerListener()
			{
				public void
				UIAttached(
					UIInstance		instance )
				{
					if ( instance instanceof UISWTInstance ){
						
						UISWTInstance swt_ui = (UISWTInstance)instance;
						
						NetStatusPluginView view = new NetStatusPluginView( NetStatusPlugin.this );

						swt_ui.addView(	UISWTInstance.VIEW_MAIN, VIEW_ID, view );
						
						//swt_ui.openMainView( VIEW_ID, view, null );
					}
				}

				public void
				UIDetached(
					UIInstance		instance )
				{
				}
			});
		
		plugin_interface.addListener(
			new PluginListener()
			{
				public void
				initializationComplete()
				{
					new AEThread2( "NetstatusPlugin:init", true )
					{
						public void
						run()
						{
							try{
								PluginInterface dht_pi = plugin_interface.getPluginManager().getPluginInterfaceByClass( DHTPlugin.class );
								
								if ( dht_pi != null ){
									
									dht_plugin = (DHTPlugin)dht_pi.getPlugin();
								}

								ddb = plugin_interface.getDistributedDatabase();
								
								ddb.addTransferHandler(
									transfer_type,
									NetStatusPlugin.this );
								
								log( "DDB transfer type registered" );
								
							}catch( Throwable e ){
								
								log( "DDB transfer type registration failed", e );
							}
						}
					}.start();
				}
				
				public void
				closedownInitiated()
				{				
				}
				
				public void
				closedownComplete()
				{				
				}
			});
	}
	
	protected void
	runTest()
	{
		if ( ddb == null ){
			
			log( "DDB not initialised yet, please try later" );
			
			return;
		}
		
		try{
			String str = test_address.getValue().trim();
	
			if ( str.length() == 0 ){
				
				DHT[]	dhts = dht_plugin.getDHTs();
				
				DHT	cvs_dht = null;
				
				for (int i=0;i<dhts.length;i++){
					
					if ( dhts[i].getTransport().getNetwork() == DHT.NW_CVS ){
						
						cvs_dht = dhts[i];
					}
				}
				
				DHTTransportContact[] contacts = cvs_dht.getTransport().getReachableContacts();
				
				for (int i=0;i<contacts.length;i++){
					
					DHTTransportContact dht_contact = contacts[i];
					
					DistributedDatabaseContact contact = ddb.importContact( dht_contact.getAddress());
					
					Map	request = new HashMap();
					
					request.put( "v", new Long(1));
					
					Map	reply = sendRequest( contact, request );
					
					System.out.println( contact.getName() + " -> " + reply );
				}
			}else{
				
				String[]	bits = str.split( ":" );
				
				if ( bits.length != 2 ){
					
					log( "Invalid address - use <host>:<port> " );
					
					return;
				}
				
				InetSocketAddress address = new InetSocketAddress( bits[0].trim(), Integer.parseInt( bits[1].trim()));
				 
				DistributedDatabaseContact contact = ddb.importContact( address );
				
				Map	request = new HashMap();
				
				request.put( "v", new Long(1));
				
				Map	reply = sendRequest( contact, request );
				
				log( "Reply: " + reply );
				
				byte[]	server_hash = (byte[])reply.get( "h" );
				
				InetSocketAddress hack_address = new InetSocketAddress( "127.0.0.1", Integer.parseInt( bits[1].trim()));

				makeOutgoing( hack_address, server_hash );
			}
		}catch( Throwable e ){
			
			log( "Test failed", e );
		}
	}
	
	protected synchronized void
	setupIncoming()
	{
		if ( pm_reg != null ){
			
			return;
		}
		
		pm_hash = new byte[20];
		
		random.nextBytes( pm_hash );
		
		pm_reg = PeerManager.getSingleton().registerLegacyManager(
			new HashWrapper( pm_hash ),
			new PeerManagerRegistrationAdapter()
			{
				public byte[][]
	          	getSecrets()
				{
					return( new byte[][]{ pm_hash });
				}
	          	
	          	public boolean
	          	manualRoute(
	          		NetworkConnection		connection )
	          	{
	          		log( "Got incoming connection from " + connection.getEndpoint().getNotionalAddress());
	          		
	          		initialiseConnection( connection, false );
	          		
	          		return( true );
	          	}
	          	
	          	public boolean
	          	isPeerSourceEnabled(
	          		String					peer_source )
	          	{
	          		return( true );
	          	}
	          	
	          	public boolean
	          	activateRequest(
	          		InetSocketAddress		remote_address )
	          	{
	          		return( true );
	          	}
	          	
	          	public void
	          	deactivateRequest(
	          		InetSocketAddress		remote_address )
	          	{
	          	}
	          	
	          	public String
	          	getDescription()
	          	{
	          		return( "NetStatusPlugin - router" );
	          	}

			});
		
		log( "Incoming routing established for " + ByteFormatter.encodeString( pm_hash ));
	}
	
	protected void
	makeOutgoing(
		InetSocketAddress		address,
		byte[]					hash )
	{
		log( "Making outbound connection to " + address );
		
		boolean	use_crypto		= false;
		boolean	allow_fallback	= false;
		
		ProtocolEndpoint	pe = new ProtocolEndpointTCP( address );
		
		ConnectionEndpoint connection_endpoint	= new ConnectionEndpoint( address );

		connection_endpoint.addProtocol( pe );

		final NetworkConnection connection = 
			NetworkManager.getSingleton().createConnection(
					connection_endpoint, 
					new BTMessageEncoder(), 
					new BTMessageDecoder(), 
					use_crypto, 
					allow_fallback, 
					new byte[][]{ hash });
	
		connection.connect( 
				true,
				new NetworkConnection.ConnectionListener() 
				{
					public final void 
					connectStarted() 
					{
						log( "Outbound connect start" );
					}

					public final void 
					connectSuccess( 
						ByteBuffer remaining_initial_data ) 
					{
						log( "Outbound connect success" );
						
						initialiseConnection( connection, true );
					}

					public final void 
					connectFailure( 
						Throwable e ) 
					{
						log( "Outbound connect fail", e );
						
						connection.close();
					}

					public final void 
					exceptionThrown( 
						Throwable e ) 
					{
						log( "Outbound connect fail", e );
						
						connection.close();
					}
    			
					public String
					getDescription()
					{
						return( "NetStatusPlugin - outbound" );
					}
				});
	}
	
	protected void
	initialiseConnection(
		NetworkConnection	connection,
		boolean				outgoing )
	{
		connection.getIncomingMessageQueue().registerQueueListener(
			new IncomingMessageQueue.MessageQueueListener() 
			{
				public boolean 
				messageReceived( Message message ) 
				{               
					String	message_id = message.getID();

					log( "Incoming message received: " + message.getID());
					
			        if ( message_id.equals( BTMessage.ID_BT_HANDSHAKE )){
				
					}
			        
			        return( true );
				}
  

				public final void 
				protocolBytesReceived(
					int byte_count ) 
				
				{
				}

				public final void 
				dataBytesReceived( 
					int byte_count ) 
				{
				}
			});

		connection.getOutgoingMessageQueue().registerQueueListener( 
			new OutgoingMessageQueue.MessageQueueListener() 
			{
				public final boolean 
				messageAdded( 
					Message message )
				{
					return( true );
				}
	
				public final void 
				messageQueued( 
					Message message )
				{
				}
	
				public final void 
				messageRemoved( 
					Message message )
				{
					
				}
	
				public final void 
				messageSent( 
					Message message ) 
				{
					log( "Outgoing message sent: " + message.getID());
				}
	
				public final void 
				protocolBytesSent( 
					int byte_count ) 
				{
				}
	
				public final void 
				dataBytesSent( 
					int byte_count ) 
				{
				}
		});

		connection.startMessageProcessing();
		
		if ( outgoing ){
			
			byte[]	peer_id = new byte[20];
			
			random.nextBytes( peer_id );
			
			connection.getOutgoingMessageQueue().addMessage(
				new BTHandshake( pm_hash, peer_id, false, BTMessageFactory.MESSAGE_VERSION_INITIAL ),
				false );
		}
	}
	
	
	
	protected Map
	sendRequest(
		DistributedDatabaseContact	contact,
		Map							request )
	{
		try{
			DistributedDatabaseKey key = ddb.createKey( BEncoder.encode( request ));
			
			DistributedDatabaseValue value = 
				contact.read( 
					new DistributedDatabaseProgressListener()
					{
						public void
						reportSize(
							long	size )
						{	
						}
						
						public void
						reportActivity(
							String	str )
						{	
						}
						
						public void
						reportCompleteness(
							int		percent )
						{
						}
					},
					transfer_type,
					key,
					10000 );
			
			if ( value == null ){
				
				return( null );
			}
			
			return( BDecoder.decode((byte[])value.getValue( byte[].class )));
			
		}catch( Throwable e ){
			
			log( "sendRequest failed", e );
			
			return( null );
		}
	}
	
	protected Map
	receiveRequest(
		Map		request )
	{
		Map	reply = new HashMap();
		
		reply.put( "v", new Long(1));
		
		setupIncoming();
		
		reply.put( "h", pm_hash );
		
		return( reply );
	}
	
	public DistributedDatabaseValue
	read(
		DistributedDatabaseContact			contact,
		DistributedDatabaseTransferType		type,
		DistributedDatabaseKey				key )
	
		throws DistributedDatabaseException
	{
		Object	o_key = key.getKey();
		
		log( "Received ddb request from " + contact.getName() + ": " + o_key );

		try{
			Map	request = BDecoder.decode( (byte[])o_key);
			
			Map	result = receiveRequest( request );
			
			return( ddb.createValue( BEncoder.encode( result )));
			
		}catch( Throwable e ){
			
			log( "ddb read failed", e );
			
			return( null );
		}
	}
	
	public void
	write(
		DistributedDatabaseContact			contact,
		DistributedDatabaseTransferType		type,
		DistributedDatabaseKey				key,
		DistributedDatabaseValue			value )
	
		throws DistributedDatabaseException
	{
		throw( new DistributedDatabaseException( "not supported" ));
	}
	
	public String
	getPingTarget()
	{
		return( ping_target.getValue());
	}
	
	public void
	log(
		String		str )
	{
		System.out.println( str );
		
		logger.log( str );
	}
	
	public void
	log(
		String		str,
		Throwable	e )
	{
		System.out.println( str );
		e.printStackTrace();
		
		logger.log( str );
		logger.log( e );
	}
	
	protected class
	testXferType
		implements DistributedDatabaseTransferType
	{	
	}
}
