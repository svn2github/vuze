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

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AESemaphore;
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
import com.aelitis.azureus.core.dht.DHTOperationListener;
import com.aelitis.azureus.core.dht.router.DHTRouterStats;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportFactory;
import com.aelitis.azureus.core.dht.transport.DHTTransportFullStats;
import com.aelitis.azureus.core.dht.transport.DHTTransportStats;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;
import com.aelitis.azureus.core.dht.transport.udp.DHTTransportUDP;
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
	
	private boolean				enabled;
	
	private AESemaphore			init_sem = new AESemaphore("DHTPlugin:init" );
	
	private LoggerChannel		log;
	
	private AEMonitor	this_mon	= new AEMonitor( "DHTPlugin" );

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
												
												DHTPlugin.this.put( 
														rhs.substring(0,pos).getBytes(),
														rhs.substring(pos+1).getBytes(),
														null );
											}
										}else if ( lhs.equals( "get" )){
											
											DHTPlugin.this.get(
												rhs.getBytes(), 1, 10000, null );

										}else if ( lhs.equals( "stats" )){
											
											pos = rhs.indexOf( ":" );
											
											String	host = rhs.substring(0,pos);
											int		port = Integer.parseInt( rhs.substring(pos+1));
											
											try{
												DHTTransportContact	contact = 
													transport.importContact(
															new InetSocketAddress( host, port ),
															DHTTransportUDP.PROTOCOL_VERSION );
												
												DHTTransportFullStats stats = contact.getStats();
												
												log.log( "Stats:" + (stats==null?"<null>":stats.getString()));
												
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
		
		Thread t = 
				new AEThread( "DTDPlugin.init" )
				{
					public void
					runSupport()
					{
						try{
							// TODO: When DHT is known to work OK remove this feature!!!! 
							
							enabled = VersionCheckClient.getSingleton().DHTEnableAllowed();
							
							if ( enabled ){
								
								model.getStatus().setText( "Initialising" );
								
								try{
									int	port = plugin_interface.getPluginconfig().getIntParameter( "TCP.Listen.Port" );
									
									transport = 
										DHTTransportFactory.createUDP( 
												port, 
												4,
												2,
												20000, 	// udp timeout - tried less but a significant number of 
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
					
														DHTTransportStats t_stats = transport.getStats();
														

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
	}
	
	protected File
	getDataDir()
	{
		File	dir = new File( plugin_interface.getUtilities().getAzureusUserDir(), "dht" );
		
		dir.mkdirs();
		
		return( dir );
	}
	
	protected void
	importContacts()
	{
		try{
			this_mon.enter();
			
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
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected void
	exportContacts()
	{
		try{
			this_mon.enter();
			
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
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public boolean
	isEnabled()
	{
		init_sem.reserve();
		
		return( enabled );
	}
		
	public void
	put(
		final byte[]						key,
		final byte[]						value,
		final DHTPluginOperationListener	listener)
	{
		if ( !isEnabled()){
			
			throw( new RuntimeException( "DHT isn't enabled" ));
		}
				
		dht.put( 	key, 
					value,
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
							
							log.log( indent + "Put: level = " + level + ", active = " + active_searches + ", contact = " + contact.getString());
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
							DHTTransportContact	contact,
							DHTTransportValue	value )
						{
							log.log( "Put: wrote " + value.getString() + " to " + contact.getString());
						}
						
						public void
						complete(
							boolean				timeout )
						{
							log.log( "Put: complete, timeout = " + timeout );
						
							if ( listener != null ){
								
								listener.complete( timeout );
							}
						}
					});
	}
	
	public void
	get(
		final byte[]								key,
		final int									max_values,
		final long									timeout,
		final DHTPluginOperationListener			listener )
	{
		if ( !isEnabled()){
			
			throw( new RuntimeException( "DHT isn't enabled" ));
		}
				
		dht.get( 	key, max_values, timeout,
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
							
							log.log( indent + "Get: level = " + level + ", active = " + active_searches + ", contact = " + contact.getString());
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
							log.log( "Get: read " + value.getString() + " from " + contact.getString() + ", originator = " + value.getOriginator().getString());
							
							if ( listener != null ){
								
								listener.valueFound( value.getOriginator().getAddress(), value.getValue());
							}
						}
						
						public void
						wrote(
							final DHTTransportContact	contact,
							final DHTTransportValue		value )
						{
							log.log( "Get: wrote " + value.getString() + " to " + contact.getString());
						}
						
						public void
						complete(
							boolean				timeout )
						{
							log.log( "Get: complete, timeout = " + timeout );
							
							if ( listener != null ){
								
								listener.complete( timeout );
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
								
								log.log( indent + "Remove: level = " + level + ", active = " + active_searches + ", contact = " + contact.getString());
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
								log.log( "Remove: read " + value.getString() + " from " + contact.getString());
							}
							
							public void
							wrote(
								DHTTransportContact	contact,
								DHTTransportValue	value )
							{
								log.log( "Remove: wrote " + value.getString() + " to " + contact.getString());
							}
							
							public void
							complete(
								boolean				timeout )
							{
								log.log( "Remove: complete, timeout = " + timeout );
							
								if ( listener != null ){
								
									listener.complete( timeout );
								}
							}			
						});
	}
}
