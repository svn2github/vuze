/*
 * Created on 24-Jan-2005
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

package com.aelitis.azureus.plugins.dht;


import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Properties;

import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Average;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.config.ActionParameter;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.IntParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.config.StringParameter;
import org.gudy.azureus2.plugins.ui.model.*;
import org.gudy.azureus2.plugins.utils.UTTimerEvent;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;

import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.DHTFactory;
import com.aelitis.azureus.core.dht.DHTOperationListener;
import com.aelitis.azureus.core.dht.control.DHTControlActivity;
import com.aelitis.azureus.core.dht.router.DHTRouterStats;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportException;
import com.aelitis.azureus.core.dht.transport.DHTTransportFactory;
import com.aelitis.azureus.core.dht.transport.DHTTransportFullStats;
import com.aelitis.azureus.core.dht.transport.DHTTransportListener;
import com.aelitis.azureus.core.dht.transport.DHTTransportProgressListener;
import com.aelitis.azureus.core.dht.transport.DHTTransportStats;
import com.aelitis.azureus.core.dht.transport.DHTTransportTransferHandler;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;
import com.aelitis.azureus.core.dht.transport.udp.DHTTransportUDP;
import com.aelitis.azureus.core.dht.transport.udp.impl.DHTTransportUDPImpl;

import com.aelitis.azureus.core.versioncheck.VersionCheckClient;
import com.aelitis.azureus.plugins.upnp.UPnPPlugin;

/**
 * @author parg
 *
 */

public class 
DHTPlugin
	implements Plugin
{
	public static final byte		FLAG_SINGLE_VALUE	= DHT.FLAG_SINGLE_VALUE;
	public static final byte		FLAG_DOWNLOADING	= DHT.FLAG_DOWNLOADING;
	public static final byte		FLAG_SEEDING		= DHT.FLAG_SEEDING;
	public static final byte		FLAG_MULTI_VALUE	= DHT.FLAG_MULTI_VALUE;
	
	public static final int			MAX_VALUE_SIZE		= DHT.MAX_VALUE_SIZE;

	private static final String	PLUGIN_NAME		= "Distributed DB";
	private static final String	SEED_ADDRESS	= "aelitis.com";
	private static final int	SEED_PORT		= 6881;
		
	private PluginInterface		plugin_interface;
	
	private DHT					dht;
	private DHTTransportUDP		transport;
	private long				integrated_time;
	
	private DHTPluginStorageManager storage_manager;

	private boolean				enabled;
	private int					dht_data_port;
	
	private AESemaphore			init_sem = new AESemaphore("DHTPlugin:init" );
	
	private LoggerChannel		log;
	
	public void
	initialize(
		PluginInterface 	_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
				
		plugin_interface.getPluginProperties().setProperty( "plugin.name", PLUGIN_NAME );

		dht_data_port = plugin_interface.getPluginconfig().getIntParameter( "TCP.Listen.Port" );

		log = plugin_interface.getLogger().getTimeStampedChannel(PLUGIN_NAME);

		UIManager	ui_manager = plugin_interface.getUIManager();

		final BasicPluginViewModel model = 
			ui_manager.createBasicPluginViewModel( PLUGIN_NAME);
		
		BasicPluginConfigModel	config = ui_manager.createBasicPluginConfigModel( "Plugins", "plugins.dht" );
			
		config.addLabelParameter2( "dht.info" );
		
		final BooleanParameter	enabled_param = config.addBooleanParameter2( "dht.enabled", "dht.enabled", true );

		final BooleanParameter	use_default_port = config.addBooleanParameter2( "dht.portdefault", "dht.portdefault", true );

		final IntParameter		dht_port_param	= config.addIntParameter2( "dht.port", "dht.port", dht_data_port );
				
		use_default_port.addDisabledOnSelection( dht_port_param );
		
		if ( !use_default_port.getValue()){
		
			dht_data_port	= dht_port_param.getValue();
		}
		
		final int f_dht_data_port	= dht_data_port;
		
		final StringParameter	command = config.addStringParameter2( "dht.execute.command", "dht.execute.command", "print" );
		
		ActionParameter	execute = config.addActionParameter2( "dht.execute.info", "dht.execute");
		
		final BooleanParameter	logging = config.addBooleanParameter2( "dht.logging", "dht.logging", false );

		logging.addListener(
			new ParameterListener()
			{
				public void
				parameterChanged(
					Parameter	param )
				{
					if ( dht != null ){
						
						dht.setLogging( logging.getValue());
					}
				}
			});
		
		execute.addListener(
			new ParameterListener()
			{
				public void
				parameterChanged(
					Parameter	param )
				{
					Thread t = 
						new AEThread( "DHT:commandrunner" )
						{
							public void
							runSupport()
							{
								if ( dht == null ){
									
									return;
								}
								
								String	c = command.getValue().trim();
								
								String	lc = c.toLowerCase();
								
								if ( lc.equals("print")){
									
									dht.print();
									
								}else if ( lc.equals( "test" )){
									
									((DHTTransportUDPImpl)transport).testExternalAddressChange();
									
								}else{
									
									int pos = c.indexOf( ' ' );
									
									if ( pos != -1 ){
										
										String	lhs = lc.substring(0,pos);
										String	rhs = c.substring(pos+1);
										
										if ( lhs.equals( "set" )){
											
											pos	= rhs.indexOf( '=' );
											
											if ( pos != -1 ){
												
												DHTPlugin.this.put( 
														rhs.substring(0,pos).getBytes(),
														rhs.substring(pos+1).getBytes(),
														(byte)0,
														null );
											}
										}else if ( lhs.equals( "get" )){
											
											DHTPlugin.this.get(
												rhs.getBytes(), (byte)0, 1, 10000, null );

										}else if ( lhs.equals( "stats" )){
											
											try{
												pos = rhs.indexOf( ":" );
												
												DHTTransportContact	contact;
												
												if ( pos == -1 ){
												
													contact = transport.getLocalContact();
													
												}else{
													
													String	host = rhs.substring(0,pos);
													int		port = Integer.parseInt( rhs.substring(pos+1));
													
													contact = 
															transport.importContact(
																	new InetSocketAddress( host, port ),
																	DHTTransportUDP.PROTOCOL_VERSION );
												}
												
												DHTTransportFullStats stats = contact.getStats();
													
												log.log( "Stats:" + (stats==null?"<null>":stats.getString()));
													
												DHTControlActivity[] activities = dht.getControl().getActivities();
													
												for (int i=0;i<activities.length;i++){
														
													log.log( "    act:" + activities[i].getString());
												}
										
											}catch( Throwable e ){
												
												Debug.printStackTrace(e);
											}
										}
									}
								}
							}
						};
						
					t.setDaemon(true);
					
					t.start();
				}
			});
		
		model.getActivity().setVisible( false );
		model.getProgress().setVisible( false );
		
		log.addListener(
				new LoggerChannelListener()
				{
					public void
					messageLogged(
						int		type,
						String	message )
					{
						model.getLogArea().appendText( message+"\n");
					}
					
					public void
					messageLogged(
						String		str,
						Throwable	error )
					{
						model.getLogArea().appendText( error.toString()+"\n");
					}
				});

		if (!enabled_param.getValue()){
			
			model.getStatus().setText( "Disabled" );

			init_sem.releaseForever();
			
			return;
		}
		
		PluginInterface pi_upnp = plugin_interface.getPluginManager().getPluginInterfaceByClass( UPnPPlugin.class );
		
		if ( pi_upnp == null ){

			log.log( "UPnP plugin not found, can't map port" );
			
		}else{
			
			((UPnPPlugin)pi_upnp.getPlugin()).addMapping( 
							plugin_interface.getPluginName(), 
							false, 
							dht_data_port, 
							true );
		}

		
		Thread t = 
				new AEThread( "DTDPlugin.init" )
				{
					public void
					runSupport()
					{
						try{
							// TODO: When DHT is known to work OK remove this feature!!!! 
							
								// we take the view that if the version check failed then we go ahead
								// and enable the DHT (i.e. we're being optimistic)
							
							enabled =
								(!VersionCheckClient.getSingleton().isVersionCheckDataValid()) ||
								VersionCheckClient.getSingleton().DHTEnableAllowed();
							
							if ( enabled ){
								
								model.getStatus().setText( "Initialising" );
								
								try{
									storage_manager = new DHTPluginStorageManager( log, getDataDir());
									
									transport = 
										DHTTransportFactory.createUDP( 
												f_dht_data_port, 
												4,
												2,
												20000, 	// udp timeout - tried less but a significant number of 
														// premature timeouts occurred
												log );
									
									transport.addListener(
										new DHTTransportListener()
										{
											public void
											localContactChanged(
												DHTTransportContact	local_contact )
											{
											}
											
											public void
											currentAddress(
												String		address )
											{
												storage_manager.recordCurrentAddress( address );
											}
										});
										
									final int sample_frequency	= 60000;
									final int sample_duration	= 10*60;
									
				
									plugin_interface.getUtilities().createTimer("DHTStats").addPeriodicEvent(
											sample_frequency,
											new UTTimerEventPerformer()
											{
												Average	incoming_packet_average = Average.getInstance(sample_frequency,sample_duration);
												
												long	last_incoming;
												
												public void
												perform(
													UTTimerEvent		event )
												{
													if ( dht != null ){
														
														DHTTransportStats t_stats = transport.getStats();
																			
														long	current_incoming = t_stats.getIncomingRequests();
														
														incoming_packet_average.addValue( (current_incoming-last_incoming)*sample_frequency/1000);
														
														last_incoming	= current_incoming;
														
														long	incoming_average = incoming_packet_average.getAverage();
														
														// System.out.println( "incoming average = " + incoming_average );
														
														long	now = SystemTime.getCurrentTime();
														
															// give some time for thing to generate reasonable stats
														
														if ( 	integrated_time > 0 &&
																now - integrated_time >= 5*60*1000 ){
														
																// 1 every 30 seconds indicates problems
															
															if ( incoming_average <= 2 ){
																
																log.logAlert(
																	LoggerChannel.LT_WARNING,
																	"If you have a router/firewall, please check that you have port " + f_dht_data_port + 
																	" UDP open.\nDecentralised tracking requires this." );
															}
														}
														
														DHTRouterStats	r_stats = dht.getRouter().getStats();
														
														long[]	rs = r_stats.getStats();
					

														log.log( "Router" +
																	":nodes=" + rs[DHTRouterStats.ST_NODES] +
																	",leaves=" + rs[DHTRouterStats.ST_LEAVES] +
																	",contacts=" + rs[DHTRouterStats.ST_CONTACTS] +
																	",replacement=" + rs[DHTRouterStats.ST_REPLACEMENTS] +
																	",live=" + rs[DHTRouterStats.ST_CONTACTS_LIVE] +
																	",unknown=" + rs[DHTRouterStats.ST_CONTACTS_UNKNOWN] +
																	",failing=" + rs[DHTRouterStats.ST_CONTACTS_DEAD]);
											
														log.log( 	"Transport" + 
																	":" + t_stats.getString()); 
																	
														log.log( 	"Database" +
																	":values=" + dht.getDataBase().getSize());
													}
												}
											});
									
									Properties	props = new Properties();
									
									/*
									System.out.println( "FRIGGED REFRESH PERIOD" );
									
									props.put( DHT.PR_CACHE_REPUBLISH_INTERVAL, new Integer( 5*60*1000 ));
									*/
									
									long	start = SystemTime.getCurrentTime();
									
									dht = DHTFactory.create( 
												transport, 
												props,
												storage_manager,
												log );
									
									dht.setLogging( logging.getValue());
									
									importSeed();
									
									storage_manager.importContacts( dht );
									
									plugin_interface.getUtilities().createTimer( "DHTExport" ).addPeriodicEvent(
											10*60*1000,
											new UTTimerEventPerformer()
											{
												public void
												perform(
													UTTimerEvent		event )
												{
													checkForReSeed();
													
													storage_manager.exportContacts( dht );
												}
											});
									
									dht.integrate();
									
									long	end = SystemTime.getCurrentTime();
			
									integrated_time	= end;
									
									log.log( "DHT integration complete: elapsed = " + (end-start));
									
									dht.print();
									
									model.getStatus().setText( "Running" );
																		
								}catch( Throwable e ){
									
									Debug.printStackTrace(e);
									
									log.log( "DHT integrtion fails", e );
									
									model.getStatus().setText( "DHT Integration fails: " + Debug.getNestedExceptionMessage( e ));
								}
							}else{
								
								model.getStatus().setText( "Disabled administratively due to network problems" );
							}
						}finally{
							
							init_sem.releaseForever();
						}
					}
				};
				
		t.setDaemon(true);
			
		t.start();
		
		plugin_interface.addListener(
			new PluginListener()
			{
				public void
				initializationComplete()
				{
				}
				
				public void
				closedownInitiated()
				{
					if ( dht != null ){
						
						storage_manager.exportContacts( dht );
					}
				}
				
				public void
				closedownComplete()
				{
				}
			});
	}
	
	protected File
	getDataDir()
	{
		File	dir = new File( plugin_interface.getUtilities().getAzureusUserDir(), "dht" );
		
		dir.mkdirs();
		
		return( dir );
	}
	
	protected void
	checkForReSeed()
	{
		try{
			
			long[]	router_stats = dht.getRouter().getStats().getStats();
		
			if ( router_stats[ DHTRouterStats.ST_CONTACTS_LIVE] < 10 ){
				
				log.log( "Less the 10 live contacts, reseeding" );
				
				importSeed();
				
				dht.integrate();
			}
			
		}catch( Throwable e ){
			
			log.log(e);
		}
	}
		
	protected void
	importSeed()
	{
		try{
			transport.importContact(
					new InetSocketAddress( getSeedAddress(), SEED_PORT ),
					DHTTransportUDP.PROTOCOL_VERSION );
			
		}catch( Throwable e ){
			
			log.log(e);
		}
	}
	
	protected InetAddress
	getSeedAddress()
	{
		try{
			return( InetAddress.getByName( SEED_ADDRESS ));
			
		}catch( Throwable e ){
			
			try{
				return( InetAddress.getByName("213.186.46.164"));
				
			}catch( Throwable f ){
				
				log.log(f);
				
				return( null );
			}
		}
	}
	

	public boolean
	isEnabled()
	{
		init_sem.reserve();
		
		return( enabled );
	}
	
	public int
	getPort()
	{
		return( dht_data_port );
	}
	
	public void
	put(
		final byte[]						key,
		final byte[]						value,
		final byte							flags,
		final DHTPluginOperationListener	listener)
	{
		if ( !isEnabled()){
			
			throw( new RuntimeException( "DHT isn't enabled" ));
		}
				
		dht.put( 	key, 
					value,
					flags,
					new DHTOperationListener()
					{
						public void
						searching(
							DHTTransportContact	contact,
							int					level,
							int					active_searches )
						{
							String	indent = "";
							
							for (int i=0;i<level;i++){
								
								indent += "  ";
							}
							
							// log.log( indent + "Put: level = " + level + ", active = " + active_searches + ", contact = " + contact.getString());
						}
						
						public void
						found(
							DHTTransportContact	contact )
						{
						}

						public void
						read(
							DHTTransportContact	_contact,
							DHTTransportValue	_value )
						{
							Debug.out( "read operation not supported for puts" );
						}
						
						public void
						wrote(
							DHTTransportContact	_contact,
							DHTTransportValue	_value )
						{
							// log.log( "Put: wrote " + _value.getString() + " to " + _contact.getString());
							
							if ( listener != null ){
								
								listener.valueWritten( new DHTPluginContactImpl(_contact ), _value.getValue());
							}

						}
						
						public void
						complete(
							boolean				timeout )
						{
							// log.log( "Put: complete, timeout = " + timeout );
						
							if ( listener != null ){
								
								listener.complete( timeout );
							}
						}
					});
	}
	
	public DHTPluginValue
	getLocalValue(
		byte[]		key )
	{
		final DHTTransportValue	val = dht.getLocalValue( key );
		
		if ( val == null ){
			
			return( null );
		}
		return( 
				new DHTPluginValue()
				{
					public byte[]
					getValue()
					{
						return( val.getValue());
					}
					
					public int
					getFlags()
					{
						return( val.getFlags()&0xff);
					}
				});
	}
	
	public void
	get(
		final byte[]								key,
		final byte									flags,
		final int									max_values,
		final long									timeout,
		final DHTPluginOperationListener			listener )
	{
		if ( !isEnabled()){
			
			throw( new RuntimeException( "DHT isn't enabled" ));
		}
				
		dht.get( 	key, flags, max_values, timeout,
					new DHTOperationListener()
					{
						public void
						searching(
							DHTTransportContact	contact,
							int					level,
							int					active_searches )
						{
							String	indent = "";
							
							for (int i=0;i<level;i++){
								
								indent += "  ";
							}
							
							// log.log( indent + "Get: level = " + level + ", active = " + active_searches + ", contact = " + contact.getString());
						}
						
						public void
						found(
							DHTTransportContact	contact )
						{
						}

						public void
						read(
							final DHTTransportContact	contact,
							final DHTTransportValue		value )
						{
							// log.log( "Get: read " + value.getString() + " from " + contact.getString() + ", originator = " + value.getOriginator().getString());
							
							if ( listener != null ){
								
								listener.valueRead( new DHTPluginContactImpl( value.getOriginator()), value.getValue(), (byte)value.getFlags());
							}
						}
						
						public void
						wrote(
							final DHTTransportContact	contact,
							final DHTTransportValue		value )
						{
							// log.log( "Get: wrote " + value.getString() + " to " + contact.getString());
						}
						
						public void
						complete(
							boolean				_timeout )
						{
							// log.log( "Get: complete, timeout = " + _timeout );
							
							if ( listener != null ){
								
								listener.complete( _timeout );
							}
						}
					});
	}
	
	public void
	remove(
		final byte[]						key,
		final DHTPluginOperationListener	listener )
	{
		if ( !isEnabled()){
			
			throw( new RuntimeException( "DHT isn't enabled" ));
		}
				
		dht.remove( 	key,
						new DHTOperationListener()
						{
							public void
							searching(
								DHTTransportContact	contact,
								int					level,
								int					active_searches )
							{
								String	indent = "";
								
								for (int i=0;i<level;i++){
									
									indent += "  ";
								}
								
								// log.log( indent + "Remove: level = " + level + ", active = " + active_searches + ", contact = " + contact.getString());
							}
							
							public void
							found(
								DHTTransportContact	contact )
							{
							}

							public void
							read(
								DHTTransportContact	contact,
								DHTTransportValue	value )
							{
								// log.log( "Remove: read " + value.getString() + " from " + contact.getString());
							}
							
							public void
							wrote(
								DHTTransportContact	contact,
								DHTTransportValue	value )
							{
								// log.log( "Remove: wrote " + value.getString() + " to " + contact.getString());
								if ( listener != null ){
									
									listener.valueWritten( new DHTPluginContactImpl( contact ), value.getValue());
								}
							}
							
							public void
							complete(
								boolean				timeout )
							{
								// log.log( "Remove: complete, timeout = " + timeout );
							
								if ( listener != null ){
								
									listener.complete( timeout );
								}
							}			
						});
	}
	
	public DHTPluginContact
	getLocalAddress()
	{
		if ( !isEnabled()){
			
			throw( new RuntimeException( "DHT isn't enabled" ));
		}
		
		return( new DHTPluginContactImpl( dht.getTransport().getLocalContact()));
	}
	
		// direct read/write support
	
	public void
	registerHandler(
		byte[]							handler_key,
		final DHTPluginTransferHandler	handler )
	{
		if ( !isEnabled()){
			
			throw( new RuntimeException( "DHT isn't enabled" ));
		}
		
		dht.getTransport().registerTransferHandler( 
				handler_key,
				new DHTTransportTransferHandler()
				{
					public byte[]
					handleRead(
						DHTTransportContact	originator,
						byte[]				key )
					{
						return( handler.handleRead( new DHTPluginContactImpl( originator ), key ));
					}
					
					public void
					handleWrite(
							DHTTransportContact	originator,
						byte[]				key,
						byte[]				value )
					{
						handler.handleWrite( new DHTPluginContactImpl( originator ), key, value );
					}
				});
	}
	
	public byte[]
	read(
		final DHTPluginProgressListener	listener,
		DHTPluginContact				target,
		byte[]							handler_key,
		byte[]							key,
		long							timeout )
	{
		if ( !isEnabled()){
			
			throw( new RuntimeException( "DHT isn't enabled" ));
		}
		
		try{
			return( dht.getTransport().readTransfer(
						new DHTTransportProgressListener()
						{
							public void
							reportSize(
								long	size )
							{
								listener.reportSize( size );
							}
							
							public void
							reportActivity(
								String	str )
							{
								listener.reportActivity( str );
							}
							
							public void
							reportCompleteness(
								int		percent )
							{
								listener.reportCompleteness( percent );
							}
						},
						((DHTPluginContactImpl)target).getContact(), 
						handler_key, 
						key, 
						timeout ));
			
		}catch( DHTTransportException e ){
			
			throw( new RuntimeException( e ));
		}
	}

	public DHT
	getDHT()
	{
		return( dht );
	}
	
	protected class
	DHTPluginContactImpl
		implements DHTPluginContact
	{
		protected DHTTransportContact	contact;
		
		protected
		DHTPluginContactImpl(
			DHTTransportContact	_contact )
		{
			contact	= _contact;
		}
		
		protected DHTTransportContact
		getContact()
		{
			return( contact );
		}
		
		public String
		getName()
		{
			return( contact.getName());
		}
		
		public InetSocketAddress
		getAddress()
		{
			return( contact.getAddress());
		}
		
		public boolean
		isAlive(
			long		timeout )
		{
			return( contact.isAlive( timeout ));
		}
		
		public boolean
		isOrHasBeenLocal()
		{
			return( storage_manager.isRecentAddress( contact.getAddress().getAddress().getHostAddress()));
		}
	}
}
