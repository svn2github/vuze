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

import java.io.*;

import java.net.InetSocketAddress;
import java.util.Properties;

import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.config.ActionParameter;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.config.StringParameter;
import org.gudy.azureus2.plugins.ui.model.*;
import org.gudy.azureus2.plugins.utils.UTTimerEvent;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;

import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.DHTFactory;
import com.aelitis.azureus.core.dht.router.DHTRouterStats;
import com.aelitis.azureus.core.dht.transport.DHTTransportFactory;
import com.aelitis.azureus.core.dht.transport.udp.DHTTransportUDP;
import com.aelitis.azureus.core.dht.transport.udp.DHTTransportUDPStats;
import com.aelitis.azureus.core.dht.transport.udp.impl.DHTTransportUDPImpl;
import com.aelitis.azureus.core.versioncheck.VersionCheckClient;

/**
 * @author parg
 *
 */

public class 
DHTPlugin
	implements Plugin
{
	private PluginInterface		plugin_interface;
	
	private DHT					dht;
	private DHTTransportUDP		transport;
	
	private LoggerChannel		log;
	
	public void
	initialize(
		PluginInterface 	_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
				
		plugin_interface.getPluginProperties().setProperty( "plugin.name", "DHT" );

		log = plugin_interface.getLogger().getTimeStampedChannel("DHT");

		UIManager	ui_manager = plugin_interface.getUIManager();

		final BasicPluginViewModel model = 
			ui_manager.createBasicPluginViewModel( "DHT");
		
		BasicPluginConfigModel	config = ui_manager.createBasicPluginConfigModel( "Plugins", "DHT" );
			
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
												
												dht.put( 	rhs.substring(0,pos).getBytes(),
															rhs.substring(pos+1).getBytes());
											}
										}else{
											
											byte[] res = dht.get( rhs.getBytes(), 10000);
											
											log.log( "Get result:" + ( res==null?"<null>":new String( res )));
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
		
			// TODO: When DHT is known to work OK remove this feature!!!! 
		
		if ( VersionCheckClient.getSingleton().DHTEnableAllowed()){
			
			model.getStatus().setText( "Initialising" );
			
			Thread t = 
				new AEThread( "DTDPlugin.init" )
				{
					public void
					runSupport()
					{
						try{
							int	port = plugin_interface.getPluginconfig().getIntParameter( "TCP.Listen.Port" );
							
							transport = 
								DHTTransportFactory.createUDP( 
										port, 
										5,
										3,
										30000, 	// udp timeout - tried less but a significant number of 
												// premature timeouts occurred
										log );
							
							plugin_interface.getUtilities().createTimer("DHTStats").addPeriodicEvent(
									60000,
									new UTTimerEventPerformer()
									{
										public void
										perform(
											UTTimerEvent		event )
										{
											if ( dht != null ){
												
												DHTRouterStats	r_stats = dht.getRouter().getStats();
												
												long[]	rs = r_stats.getStats();
			
												log.log( "Router Stats    " +
															":no=" + rs[DHTRouterStats.ST_NODES] +
															",le=" + rs[DHTRouterStats.ST_LEAVES] +
															",co=" + rs[DHTRouterStats.ST_CONTACTS] +
															",re=" + rs[DHTRouterStats.ST_REPLACEMENTS] +
															",cl=" + rs[DHTRouterStats.ST_CONTACTS_LIVE] +
															",cu=" + rs[DHTRouterStats.ST_CONTACTS_UNKNOWN] +
															",cd=" + rs[DHTRouterStats.ST_CONTACTS_DEAD]);
																
												DHTTransportUDPStats t_stats = (DHTTransportUDPStats)transport.getStats();
												
												log.log( "Transport Stats" + 
															":ps=" + t_stats.getPacketsSent() +
															",pr=" + t_stats.getPacketsReceived() +
															",bs=" + t_stats.getBytesSent() +
															",br=" + t_stats.getBytesReceived() +
															",to=" + t_stats.getRequestsTimedOut());
											}
										}
									});
							
							Properties	props = new Properties();
							
							// props.put( DHT.PR_CACHE_REPUBLISH_INTERVAL, new Integer( 5*60*1000 ));
							
							long	start = SystemTime.getCurrentTime();
							
							dht = DHTFactory.create( transport, props, log );
							
							dht.setLogging( logging.getValue());
							
							transport.importContact(
									new InetSocketAddress( "213.186.46.164", 6881 ),
									DHTTransportUDP.PROTOCOL_VERSION );
							
							importContacts();
							
							plugin_interface.getUtilities().createTimer( "DHTExport" ).addPeriodicEvent(
									10*60*1000,
									new UTTimerEventPerformer()
									{
										public void
										perform(
											UTTimerEvent		event )
										{
											exportContacts();
										}
									});
							
							dht.integrate();
							
							long	end = SystemTime.getCurrentTime();
	
							log.log( "DHT integration complete: elapsed = " + (end-start));
							
							dht.print();
							
							model.getStatus().setText( "Running" );
							
								// test put
							
							byte[]	 key = transport.getLocalContact().getString().getBytes();
							
							dht.put( key, key );
							
							log.log( "Performed test put of '" + new String( key ) + "'" );
							
						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
							
							log.log( "DHT integrtion fails", e );
							
							model.getStatus().setText( "DHT Integration fails: " + Debug.getNestedExceptionMessage( e ));
						}
					}
				};
				
			t.setDaemon(true);
			
			t.start();
			
		}else{
			
			model.getStatus().setText( "Disabled administratively due to network problems" );
		}
	}
	
	protected File
	getDataDir()
	{
		File	dir = new File( plugin_interface.getUtilities().getAzureusUserDir(), "dht" );
		
		dir.mkdirs();
		
		return( dir );
	}
	
	protected synchronized void
	importContacts()
	{
		try{
			File	dir = getDataDir();
			
			File	target = new File( dir, "contacts.dat" );

			if ( target.exists()){
				
				DataInputStream	dis = null;
				
				try{
				
					dis = new DataInputStream( new FileInputStream( target ));
					
					dht.importState( dis );
					
				}finally{
					
					if ( dis != null ){
						
						dis.close();	
					}
				}
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
		}
	}
	
	protected synchronized void
	exportContacts()
	{
		try{
			File	dir = getDataDir();
			
			File	saving = new File( dir, "contacts.saving" );
			File	target = new File( dir, "contacts.dat" );

			saving.delete();
			
			DataOutputStream	dos	= null;
			
			boolean	ok = false;
			
			try{
				dos = new DataOutputStream( new FileOutputStream( saving ));
					
				dht.exportState( dos, 32 );
			
				ok	= true;
				
			}finally{
				
				if ( dos != null ){
					
					dos.close();
					
					if ( ok ){
						
						target.delete();
						
						saving.renameTo( target );
					}
				}
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
		}
	}
}
