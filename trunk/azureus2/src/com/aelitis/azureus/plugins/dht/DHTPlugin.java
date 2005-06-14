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



import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;


import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Constants;

import org.gudy.azureus2.core3.util.Debug;

import org.gudy.azureus2.plugins.*;

import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;

import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.config.ActionParameter;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.IntParameter;
import org.gudy.azureus2.plugins.ui.config.LabelParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.config.StringParameter;
import org.gudy.azureus2.plugins.ui.model.*;


import com.aelitis.azureus.core.dht.DHT;

import com.aelitis.azureus.core.dht.control.DHTControlActivity;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportFullStats;
import com.aelitis.azureus.core.dht.transport.udp.DHTTransportUDP;
import com.aelitis.azureus.core.dht.transport.udp.impl.DHTTransportUDPImpl;

import com.aelitis.azureus.core.versioncheck.VersionCheckClient;
import com.aelitis.azureus.plugins.dht.impl.DHTPluginImpl;

import com.aelitis.azureus.plugins.upnp.UPnPPlugin;

/**
 * @author parg
 *
 */

public class 
DHTPlugin
	implements Plugin
{
	public static final int			STATUS_DISABLED			= 1;
	public static final int			STATUS_INITALISING		= 2;
	public static final int			STATUS_RUNNING			= 3;
	public static final int			STATUS_FAILED			= 4;
	
	public static final byte		FLAG_SINGLE_VALUE	= DHT.FLAG_SINGLE_VALUE;
	public static final byte		FLAG_DOWNLOADING	= DHT.FLAG_DOWNLOADING;
	public static final byte		FLAG_SEEDING		= DHT.FLAG_SEEDING;
	public static final byte		FLAG_MULTI_VALUE	= DHT.FLAG_MULTI_VALUE;
	
	public static final int			MAX_VALUE_SIZE		= DHT.MAX_VALUE_SIZE;

	private static final String	PLUGIN_VERSION	= "1.0";
	private static final String	PLUGIN_NAME		= "Distributed DB";
	
		
	private PluginInterface		plugin_interface;
	
	private int					status		= STATUS_INITALISING;
	private DHTPluginImpl[]		dhts;
	
	private ActionParameter		reseed;
		
	private boolean				enabled;
	private int					dht_data_port;
	
	private AESemaphore			init_sem = new AESemaphore("DHTPlugin:init" );
	
	private LoggerChannel		log;
	
	public void
	initialize(
		PluginInterface 	_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
				
		plugin_interface.getPluginProperties().setProperty( "plugin.version", 	PLUGIN_VERSION );
		plugin_interface.getPluginProperties().setProperty( "plugin.name", 		PLUGIN_NAME );

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
				
		LabelParameter	reseed_label = config.addLabelParameter2( "dht.reseed.label" );
		
		final StringParameter	reseed_ip	= config.addStringParameter2( "dht.reseed.ip", "dht.reseed.ip", "" );
		final IntParameter		reseed_port	= config.addIntParameter2( "dht.reseed.port", "dht.reseed.port", 0 );
		
		reseed = config.addActionParameter2( "dht.reseed.info", "dht.reseed");

		reseed.setEnabled( false );
		
		config.createGroup( "dht.reseed.group",
				new Parameter[]{ reseed_label, reseed_ip, reseed_port, reseed });
		
		final BooleanParameter	advanced = config.addBooleanParameter2( "dht.advanced", "dht.advanced", false );

		LabelParameter	advanced_label = config.addLabelParameter2( "dht.advanced.label" );

		final StringParameter	override_ip	= config.addStringParameter2( "dht.override.ip", "dht.override.ip", "" );

		config.createGroup( "dht.advanced.group",
				new Parameter[]{ advanced_label, override_ip });

		advanced.addEnabledOnSelection( advanced_label );
		advanced.addEnabledOnSelection( override_ip );
		
		final StringParameter	command = config.addStringParameter2( "dht.execute.command", "dht.execute.command", "print" );
		
		ActionParameter	execute = config.addActionParameter2( "dht.execute.info", "dht.execute");
		
		final BooleanParameter	logging = config.addBooleanParameter2( "dht.logging", "dht.logging", false );

		config.createGroup( "dht.diagnostics.group",
				new Parameter[]{ command, execute, logging });

		logging.addListener(
			new ParameterListener()
			{
				public void
				parameterChanged(
					Parameter	param )
				{
					if ( dhts != null ){
						
						for (int i=0;i<dhts.length;i++){
							
							dhts[i].setLogging( logging.getValue());
						}
					}
				}
			});
		
		final DHTPluginOperationListener log_polistener =
			new DHTPluginOperationListener()
			{
				public void
				valueRead(
					DHTPluginContact	originator,
					DHTPluginValue		value )
				{
					log.log( "valueRead: " + new String(value.getValue()) + " from " + originator.getName());
				}
				
				public void
				valueWritten(
					DHTPluginContact	target,
					DHTPluginValue		value )
				{
					log.log( "valueWritten:" + new String( value.getValue()) + " to " + target.getName());
				}
				
				public void
				complete(
					boolean	timeout_occurred )
				{
					log.log( "complete: timeout = " + timeout_occurred );
				}
			};
			
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
								if ( dhts == null ){
									
									return;
								}
								
								for (int i=0;i<dhts.length;i++){

									DHT	dht = dhts[i].getDHT();
									
									DHTTransportUDP	transport = (DHTTransportUDP)dht.getTransport();
									
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
															"DHT Plugin: set",
															rhs.substring(pos+1).getBytes(),
															(byte)0,
															log_polistener );
												}
											}else if ( lhs.equals( "get" )){
												
												DHTPlugin.this.get(
													rhs.getBytes(), "DHT Plugin: get", (byte)0, 1, 10000, true, log_polistener );
	
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
																		transport.getProtocolVersion());
													}
													
													DHTTransportFullStats stats = contact.getStats();
														
													log.log( "Stats:" + (stats==null?"<null>":stats.getString()));
														
													DHTControlActivity[] activities = dht.getControl().getActivities();
														
													for (int j=0;j<activities.length;j++){
															
														log.log( "    act:" + activities[j].getString());
													}
											
												}catch( Throwable e ){
													
													Debug.printStackTrace(e);
												}
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
		
		reseed.addListener(
				new ParameterListener()
				{
					public void
					parameterChanged(
						Parameter	param )
					{
						reseed.setEnabled( false );						

						Thread t = 
							new AEThread( "DHT:reseeder" )
							{
								public void
								runSupport()
								{
									try{
										String	ip 	= reseed_ip.getValue().trim();

										if ( dhts == null ){
											
											return;
										}
									
										int		port = reseed_port.getValue();
									
										for (int i=0;i<dhts.length;i++){
											
											DHTPluginImpl	dht = dhts[i];
										
											if ( ip.length() == 0 || port == 0 ){
												
												dht.checkForReSeed( true );
												
											}else{
												
												if ( dht.importSeed( ip, port ) != null ){
													
													dht.integrateDHT( false, null );
												}
											}
										}
										
									}finally{
										
										reseed.setEnabled( true );
									}
								}
							};
							
						t.setDaemon( true );
						
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

			status	= STATUS_DISABLED;
			
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
				new AEThread( "DHDPlugin.init" )
				{
					public void
					runSupport()
					{
						try{							
								// we take the view that if the version check failed then we go ahead
								// and enable the DHT (i.e. we're being optimistic)
							
							enabled =
								(!VersionCheckClient.getSingleton().isVersionCheckDataValid()) ||
								VersionCheckClient.getSingleton().DHTEnableAllowed();
							
							if ( enabled ){
								
								model.getStatus().setText( "Initialising" );
								
								String	ip = null;
								
								if ( advanced.getValue()){
									
									ip = override_ip.getValue().trim();
									
									if ( ip.length() == 0 ){
										
										ip = null;
									}
								}
								
								List	plugins = new ArrayList();
								
								plugins.add( new DHTPluginImpl(
												plugin_interface,
												DHTTransportUDP.PROTOCOL_VERSION_MAIN,
												DHT.NW_MAIN,
												ip,
												dht_data_port,
												reseed,
												logging.getValue(),
												log ));
								
								if ( Constants.isCVSVersion()){
									
									plugins.add( new DHTPluginImpl(
											plugin_interface,
											DHTTransportUDP.PROTOCOL_VERSION_CVS,
											DHT.NW_CVS,
											ip,
											dht_data_port,
											reseed,
											logging.getValue(),
											log ));
								}
								
								dhts = new DHTPluginImpl[plugins.size()];
								
								plugins.toArray( dhts );
														
								status = dhts[0].getStatus();
								
								model.getStatus().setText( dhts[0].getStatusText());
							
							}else{
								
								status	= STATUS_DISABLED;

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
					if ( dhts != null ){
						
						for (int i=0;i<dhts.length;i++){
							
							dhts[i].closedownInitiated();
						}
					}
				}
				
				public void
				closedownComplete()
				{
				}
			});
	}
	
	

	public boolean
	isEnabled()
	{
		init_sem.reserve();
		
		return( enabled );
	}
	
	public boolean
	peekEnabled()
	{
		if ( init_sem.isReleasedForever()){
			
			return( enabled );
		}
		
		return( true );	// don't know yet
	}
	
	public boolean
	isExtendedUseAllowed()
	{
		if ( !isEnabled()){
			
			return( false );
		}
		
		return( VersionCheckClient.getSingleton().DHTExtendedUseAllowed());
	}
	
	public int
	getPort()
	{
		return( dht_data_port );
	}
	
	public void
	put(
		final byte[]						key,
		final String						description,
		final byte[]						value,
		final byte							flags,
		final DHTPluginOperationListener	listener)
	{
		if ( !isEnabled()){
			
			throw( new RuntimeException( "DHT isn't enabled" ));
		}
				
		dhts[0].put( key, description, value, flags, listener );
	}
	
	public DHTPluginValue
	getLocalValue(
		byte[]		key )
	{
		if ( !isEnabled()){
			
			throw( new RuntimeException( "DHT isn't enabled" ));
		}
		
		return( dhts[0].getLocalValue( key ));
	}
	
	public void
	get(
		final byte[]								key,
		final String								description,
		final byte									flags,
		final int									max_values,
		final long									timeout,
		final boolean								exhaustive,
		final DHTPluginOperationListener			listener )
	{
		if ( !isEnabled()){
			
			throw( new RuntimeException( "DHT isn't enabled" ));
		}
				
		dhts[0].get( key, description, flags, max_values, timeout, exhaustive, listener );
	}
	
	public void
	remove(
		final byte[]						key,
		final String						description,
		final DHTPluginOperationListener	listener )
	{
		if ( !isEnabled()){
			
			throw( new RuntimeException( "DHT isn't enabled" ));
		}
				
		dhts[0].remove( key, description, listener );
	}
	
	public DHTPluginContact
	getLocalAddress()
	{
		if ( !isEnabled()){
			
			throw( new RuntimeException( "DHT isn't enabled" ));
		}

		return( dhts[0].getLocalAddress());
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
		
		dhts[0].registerHandler( handler_key, handler );
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
		
		return( dhts[0].read( listener, target, handler_key, key, timeout ));
	}

	public int
	getStatus()
	{
		return( status );
	}
	
	public DHT[]
	getDHTs()
	{
		if ( dhts == null ){
			
			return( new DHT[0] );
		}
		
		DHT[]	res = new DHT[ dhts.length ];
		
		for (int i=0;i<res.length;i++){
			
			res[i] = dhts[i].getDHT();
		}
		
		return( res );
	}
	
	public void
	log(
		String	str )
	{
		log.log( str );
	}
}
