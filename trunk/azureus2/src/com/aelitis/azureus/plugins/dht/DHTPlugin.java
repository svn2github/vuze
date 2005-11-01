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
import org.gudy.azureus2.plugins.ui.components.UITextField;
import org.gudy.azureus2.plugins.ui.config.ActionParameter;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.IntParameter;
import org.gudy.azureus2.plugins.ui.config.LabelParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.config.StringParameter;
import org.gudy.azureus2.plugins.ui.model.*;
import org.gudy.azureus2.plugins.utils.UTTimerEvent;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;


import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.DHTLogger;

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
	
	private static final boolean	TRACE_NON_MAIN = false;
	private static final boolean	MAIN_DHT_ENABLE	= true;
	private static final boolean	CVS_DHT_ENABLE	= true;
	
	
	static{
		
		if ( TRACE_NON_MAIN ){
			
			System.out.println( "**** DHTPlugin - tracing non-main network actions ****" );
		}
	}
		
	private PluginInterface		plugin_interface;
	
	private int					status		= STATUS_INITALISING;
	private DHTPluginImpl[]		dhts;
	
	private ActionParameter		reseed;
		
	private boolean				enabled;
	private int					dht_data_port_default, dht_data_port;
	
	private boolean				got_extended_use;
	private boolean				extended_use;
	
	private AESemaphore			init_sem = new AESemaphore("DHTPlugin:init" );
	
	private BooleanParameter	ipfilter_logging;
	
	private LoggerChannel		log;
	private DHTLogger			dht_log;
	
	
	public void
	initialize(
		PluginInterface 	_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
				
		plugin_interface.getPluginProperties().setProperty( "plugin.version", 	PLUGIN_VERSION );
		plugin_interface.getPluginProperties().setProperty( "plugin.name", 		PLUGIN_NAME );

		dht_data_port_default = dht_data_port = plugin_interface.getPluginconfig().getIntParameter( "TCP.Listen.Port" );

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
		
	    dht_port_param.addListener( new ParameterListener() {
	        public void parameterChanged( Parameter p ) {
		        int val = dht_port_param.getValue();
		          
		        if( val == 6880 || val == 6881 ) {
		        	  dht_port_param.setValue( 6881 );
		        }
		        if( val > 65535 ) {
		        	dht_port_param.setValue(65535);
		    	}
		        if( val < 1 ) {
		        	dht_port_param.setValue(1);
		    	}
	        }
	      });
	    
	    use_default_port.addListener(new ParameterListener() {
	    	public void parameterChanged(Parameter p){
	    		boolean useDefault = use_default_port.getValue();
	    		
	    		if(useDefault){
	    			dht_port_param.setValue(dht_data_port_default);
	    		}
	    	}
	    });
		
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
		
		ipfilter_logging = config.addBooleanParameter2( "dht.ipfilter.log", "dht.ipfilter.log", true );

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
										
										dhts[i].logStats();
										
									}else if ( lc.equals( "testca" )){
																
										((DHTTransportUDPImpl)transport).testExternalAddressChange();
										
									}else if ( lc.equals( "testnd" )){
										
										((DHTTransportUDPImpl)transport).testNetworkAlive( false );
										
									}else if ( lc.equals( "testna" )){
										
										((DHTTransportUDPImpl)transport).testNetworkAlive( true );
				
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
	
											}else if ( lhs.equals( "punch" )){

												dht.getNATPuncher().punch( transport.getLocalContact());
												
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

		dht_log = 
			new DHTLogger()
			{
				public void
				log(
					String	str )
				{
					log.log( str );
				}
			
				public void
				log(
					Throwable e )
				{
					log.log( e );
				}
				
				public void
				log(
					int		log_type,
					String	str )
				{
					if ( isEnabled( log_type )){
						
						log.log( str );
					}
				}
			
				public boolean
				isEnabled(
					int	log_type )
				{
					if ( log_type == DHTLogger.LT_IP_FILTER ){
						
						return( ipfilter_logging.getValue());
					}
					
					return( true );
				}
					
				public PluginInterface
				getPluginInterface()
				{
					return( log.getLogger().getPluginInterface());
				}
			};
		
		
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

		setPluginInfo();
		
		plugin_interface.addListener(
			new PluginListener()
			{
				public void
				initializationComplete()
				{
					String	ip = null;
					
					if ( advanced.getValue()){
						
						ip = override_ip.getValue().trim();
						
						if ( ip.length() == 0 ){
							
							ip = null;
						}
					}
					
					initComplete( model.getStatus(), logging.getValue(), ip );
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
		
		final int sample_frequency		= 60*1000;
		final int sample_stats_ticks	= 15;	// every 15 mins

		plugin_interface.getUtilities().createTimer("DHTStats").addPeriodicEvent(
				sample_frequency,
				new UTTimerEventPerformer()
				{
					public void
					perform(
						UTTimerEvent		event )
					{
						if ( dhts != null ){
							
							for (int i=0;i<dhts.length;i++){
								
								dhts[i].updateStats( sample_stats_ticks );
							}
						}
						
						setPluginInfo();
					}
				});

	}
	
	protected void
	initComplete(
		final UITextField		status_area,
		final boolean			logging,
		final String			override_ip )
	{
		Thread t = 
			new AEThread( "DHDPlugin.init" )
			{
				public void
				runSupport()
				{
					try{							
						
						enabled = VersionCheckClient.getSingleton().DHTEnableAllowed();
						
						if ( enabled ){
							
							status_area.setText( "Initialising" );
							
							List	plugins = new ArrayList();
							
							if ( MAIN_DHT_ENABLE ){
								
								DHTPluginImpl plug = new DHTPluginImpl(
												plugin_interface,
												DHTTransportUDP.PROTOCOL_VERSION_MAIN,
												DHT.NW_MAIN,
												override_ip,
												dht_data_port,
												reseed,
												logging,
												log, dht_log );
								
								plugins.add( plug );
							}
							
							if ( Constants.isCVSVersion() && CVS_DHT_ENABLE ){
								
								plugins.add( new DHTPluginImpl(
										plugin_interface,
										DHTTransportUDP.PROTOCOL_VERSION_CVS,
										DHT.NW_CVS,
										override_ip,
										dht_data_port,
										reseed,
										logging,
										log, dht_log ));
							}
							
							DHTPluginImpl[]	_dhts = new DHTPluginImpl[plugins.size()];
							
							plugins.toArray( _dhts );
												
							dhts = _dhts;
							
							status = dhts[0].getStatus();
							
							status_area.setText( dhts[0].getStatusText());
						
						}else{
							
							status	= STATUS_DISABLED;

							status_area.setText( "Disabled administratively due to network problems" );
						}
					}finally{
						
						init_sem.releaseForever();
					}
				}
			};
			
		t.setDaemon(true);
		
		t.start();
	}
	
	protected void
	setPluginInfo()
	{
		boolean	reachable	= plugin_interface.getPluginconfig().getPluginBooleanParameter( "dht.reachable." + DHT.NW_MAIN, true );

		plugin_interface.getPluginconfig().setPluginParameter( 
				"plugin.info", 
				reachable?"1":"0" );
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
		
		if ( !got_extended_use){
		
			got_extended_use	= true;
			
			extended_use = VersionCheckClient.getSingleton().DHTExtendedUseAllowed();
		}
		
		return( extended_use );
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
		
		for (int i=1;i<dhts.length;i++){

			final int f_i	= i;
			
			new AEThread( "mutli-dht: put", true )
			{
				public void
				runSupport()
				{
					dhts[f_i].put( key, description, value, flags, 
							new DHTPluginOperationListener()
							{
								public void
								valueRead(
									DHTPluginContact	originator,
									DHTPluginValue		value )
								{
									if ( TRACE_NON_MAIN ){
										System.out.println( "DHT_" + f_i + ":put valueRead" );
									}
								}
								
								public void
								valueWritten(
									DHTPluginContact	target,
									DHTPluginValue		value )
								{
									if ( TRACE_NON_MAIN ){
										System.out.println( "DHT_" + f_i + ":put valueWritten" );
									}
								}
								
								public void
								complete(
									boolean	timeout_occurred )
								{
									if ( TRACE_NON_MAIN ){
										System.out.println( "DHT_" + f_i + ":put complete, timeout=" + timeout_occurred );
									}
								}
							});
				}
			}.start();
		}
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
		
		for (int i=1;i<dhts.length;i++){

			final int f_i	= i;
			
			new AEThread( "mutli-dht: get", true )
			{
				public void
				runSupport()
				{
					dhts[f_i].get( 
							key, description, flags, max_values, timeout, exhaustive, 
							new DHTPluginOperationListener()
							{
								public void
								valueRead(
									DHTPluginContact	originator,
									DHTPluginValue		value )
								{
									if ( TRACE_NON_MAIN ){
										System.out.println( "DHT_" + f_i + ":get valueRead" );
									}
								}
								
								public void
								valueWritten(
									DHTPluginContact	target,
									DHTPluginValue		value )
								{
									if ( TRACE_NON_MAIN ){
										System.out.println( "DHT_" + f_i + ":get valueWritten" );
									}
								}
								
								public void
								complete(
									boolean	timeout_occurred )
								{
									if ( TRACE_NON_MAIN ){
										System.out.println( "DHT_" + f_i + ":get complete, timeout=" + timeout_occurred );
									}
								}
							});
				}
			}.start();
		}
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
		
		for (int i=1;i<dhts.length;i++){

			final int f_i	= i;
			
			new AEThread( "mutli-dht: remove", true )
			{
				public void
				runSupport()
				{
					dhts[f_i].remove( 
							key, description, 
							new DHTPluginOperationListener()
							{
								public void
								valueRead(
									DHTPluginContact	originator,
									DHTPluginValue		value )
								{
									if ( TRACE_NON_MAIN ){
										System.out.println( "DHT_" + f_i + ":remove valueRead" );
									}
								}
								
								public void
								valueWritten(
									DHTPluginContact	target,
									DHTPluginValue		value )
								{
									if ( TRACE_NON_MAIN ){
										System.out.println( "DHT_" + f_i + ":remove valueWritten" );
									}
								}
								
								public void
								complete(
									boolean	timeout_occurred )
								{
									if ( TRACE_NON_MAIN ){
										System.out.println( "DHT_" + f_i + ":remove complete, timeout=" + timeout_occurred );
									}
								}
							});
				}
			}.start();
		}
	}
	
	public DHTPluginContact
	getLocalAddress()
	{
		if ( !isEnabled()){
			
			throw( new RuntimeException( "DHT isn't enabled" ));
		}

			// first DHT will do here
		
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
		
		for (int i=0;i<dhts.length;i++){
			
			dhts[i].registerHandler( handler_key, handler );
		}
	}
	
	public byte[]
	read(
		final DHTPluginProgressListener	listener,
		final DHTPluginContact			target,
		final byte[]					handler_key,
		final byte[]					key,
		final long						timeout )
	{
		if ( !isEnabled()){
			
			throw( new RuntimeException( "DHT isn't enabled" ));
		}
		
		for (int i=1;i<dhts.length;i++){

			final int f_i	= i;
			
			new AEThread( "mutli-dht: readXfer", true )
			{
				public void
				runSupport()
				{
					dhts[f_i].read( 
							new DHTPluginProgressListener()
							{
								public void
								reportSize(
									long	size )
								{
									if ( TRACE_NON_MAIN ){
										System.out.println( "DHT_" + f_i + ":readXfer: size = " + size );
									}
								}
								
								public void
								reportActivity(
									String	str )
								{
									if ( TRACE_NON_MAIN ){
										System.out.println( "DHT_" + f_i + ":readXfer: act = " + str );
									}
								}
								
								public void
								reportCompleteness(
									int		percent )
								{
									if ( TRACE_NON_MAIN ){
										System.out.println( "DHT_" + f_i + ":readXfer: % = " + percent );
									}
								}
							},
							target, handler_key, key, timeout );
				}
			}.start();
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
