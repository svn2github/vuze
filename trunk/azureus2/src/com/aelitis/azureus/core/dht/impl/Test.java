/*
 * Created on 12-Jan-2005
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

package com.aelitis.azureus.core.dht.impl;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.dht.*;
import com.aelitis.azureus.core.dht.transport.*;
import com.aelitis.azureus.core.dht.transport.loopback.DHTTransportLoopbackImpl;
import com.aelitis.azureus.core.dht.transport.udp.DHTTransportUDP;
import com.aelitis.azureus.core.dht.transport.udp.impl.DHTTransportUDPImpl;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;

import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.Timer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.plugins.logging.Logger;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;

/**
 * @author parg
 *
 */

public class 
Test 
{
	static boolean	AELITIS_TEST	= false;
	static InetSocketAddress	AELITIS_ADDRESS = new InetSocketAddress("213.186.46.164", 6881);
	
	static{
		
		DHTTransportUDPImpl.TEST_EXTERNAL_IP	= true;
	}
	
	static int num_dhts		= 2;
	static int num_stores	= 2;
	static int MAX_VALUES	= 10000;
	
	static boolean	udp_protocol	= true;
	static int		udp_timeout		= 1000;
	

	static int		K			= 20;
	static int		B			= 5;
	static int		ID_BYTES	= 20;
	
	static int		fail_percentage	= 00;
	
	static Properties	dht_props = new Properties();

	static{
		dht_props.put( DHT.PR_CONTACTS_PER_NODE, new Integer(K));
		dht_props.put( DHT.PR_NODE_SPLIT_FACTOR, new Integer(B));
		dht_props.put( DHT.PR_MAX_VALUES_STORED, new Integer(MAX_VALUES));
		dht_props.put( DHT.PR_CACHE_REPUBLISH_INTERVAL, new Integer(30000));
		dht_props.put( DHT.PR_ORIGINAL_REPUBLISH_INTERVAL, new Integer(60000));
	}

	static byte[]	th_key	= new byte[]{ 1,1,1,1 };
	
	static Map	check = new HashMap();


	static LoggerChannel	logger;
	
	static{
		
		logger = AzureusCoreFactory.create().getPluginManager().getDefaultPluginInterface().getLogger().getNullChannel("test");
		
		logger.addListener(
			new LoggerChannelListener()
			{
				public void
				messageLogged(
					int		type,
					String	content )
				{
					System.out.println( content );
				}
				
				public void
				messageLogged(
					String		str,
					Throwable	error )
				{
					System.out.println( str );
					
					error.printStackTrace();
				}
			});
	}

	public static LoggerChannel
	getLogger()
	{
		return( logger );
	}
	
	public static void
	main(
		String[]		args )
	{
		
		try{
	
			
			DHT[]			dhts 		= new DHT[num_dhts*2+30];
			DHTTransport[]	transports 	= new DHTTransport[num_dhts*2+30];
			
			
			for (int i=0;i<num_dhts;i++){
				
				createDHT( dhts, transports, i );
			}

			for (int i=0;i<num_dhts-1;i++){
			
				if ( AELITIS_TEST ){
					
					((DHTTransportUDP)transports[i]).importContact( AELITIS_ADDRESS, DHTTransportUDP.PROTOCOL_VERSION );
					
				}else{
					ByteArrayOutputStream	baos = new ByteArrayOutputStream();
					
					DataOutputStream	daos = new DataOutputStream( baos );
					
					transports[i].getLocalContact().exportContact( daos );
					
					daos.close();
					
					transports[i+1].importContact( new DataInputStream( new ByteArrayInputStream( baos.toByteArray())));
				}
					
				dhts[i].integrate();
					
				if ( i > 0 && i%10 == 0 ){
						
					System.out.println( "Integrated " + i + " DHTs" );
				}
			}
			
			if ( AELITIS_TEST ){
				
				((DHTTransportUDP)transports[num_dhts-1]).importContact( AELITIS_ADDRESS, DHTTransportUDP.PROTOCOL_VERSION );

			}else{
				
				ByteArrayOutputStream	baos = new ByteArrayOutputStream();
				
				DataOutputStream	daos = new DataOutputStream( baos );
				
				transports[0].getLocalContact().exportContact( daos );
				
				daos.close();
				
				transports[num_dhts-1].importContact( new DataInputStream( new ByteArrayInputStream( baos.toByteArray())));
			}
			
			
			dhts[num_dhts-1].integrate();
			
			DHTTransportLoopbackImpl.setFailPercentage(fail_percentage);
			
			//dht1.print();
			
			//DHTTransportLoopbackImpl.setLatency( 500);
			
			/*
			System.out.println( "before put:" + transports[99].getStats().getString());
			dhts[99].put( "fred".getBytes(), new byte[2]);
			System.out.println( "after put:" + transports[99].getStats().getString());

			System.out.println( "get:"  + dhts[0].get( "fred".getBytes()));
			System.out.println( "get:"  + dhts[77].get( "fred".getBytes()));
			*/
			
			Map	store_index = new HashMap();
			
			for (int i=0;i<num_stores;i++){
				
				int	dht_index = (int)(Math.random()*num_dhts);
				
				DHT	dht = dhts[dht_index];

				dht.put( (""+i).getBytes(), new byte[4] );
			
				store_index.put( ""+i, dht );
				
				if ( i != 0 && i %100 == 0 ){
					
					System.out.println( "Stored " + i + " values" );
				}
			}
			
			Timer	timer = new Timer("");
			
			timer.addPeriodicEvent(
				10000,
				new TimerEventPerformer()
				{
					public void 
					perform(
						TimerEvent event) 
					{
						if ( !udp_protocol ){
							
							DHTTransportStats stats = DHTTransportLoopbackImpl.getOverallStats();
						
							System.out.println( "Overall stats: " + stats.getString());
						}
					}
				});
			
			LineNumberReader	reader = new LineNumberReader( new InputStreamReader( System.in ));
			
			while( true ){
				
				System.out.print( "> " );
				
				try{
					String	str = reader.readLine().trim();
					
					if ( str == null ){
						
						break;
					}
					
					int	pos = str.indexOf(' ');
					
					if ( pos == -1 || pos == 0 ){
						
						usage();
						
						continue;
					}
					
					int	dht_index = (int)(Math.random()*num_dhts);
					
					DHT	dht = dhts[dht_index];
					
					String	lhs = str.substring(0,pos);
					String	rhs = str.substring(pos+1);
					
					DHTTransportStats	stats_before 	= null;
					
					char command = lhs.toLowerCase().charAt(0);
					
					if ( command == 'p' ){
						
						pos = rhs.indexOf('=');
						
						if ( pos == -1 ){
							
							usage();
							
						}else{
						
							System.out.println( "Using dht " + dht_index );
							
							stats_before = dht.getTransport().getStats().snapshot();
	
							String	key = rhs.substring(0,pos);
							String	val = rhs.substring(pos+1);
							
							dht.put( key.getBytes(), val.getBytes(), (byte)(Math.random()*255), null );
						}
					}else if ( command == 'x' ){
						
						dht = (DHT)store_index.get( rhs );
						
						if ( dht == null ){
							
							System.out.println( "DHT not found" );
							
						}else{
							
							stats_before = dht.getTransport().getStats().snapshot();
							
							byte[]	res = dht.remove( rhs.getBytes());
							
							if ( res != null ){
								
								store_index.remove( rhs );
							}
							
							System.out.println( "-> " + (res==null?"null":new String(res)));
						}
					}else if ( command == 'e' ){
						
						dht = (DHT)store_index.get( rhs );
						
						if ( dht == null ){
							
							System.out.println( "DHT not found" );
							
						}else{
							
							DataOutputStream	daos = new DataOutputStream( new FileOutputStream( "C:\\temp\\dht.state"));
							
							dht.exportState( daos, 0 );
							
							daos.close();
						}
					}else if ( command == 'g' ){
						
						System.out.println( "Using dht " + dht_index );
						
						stats_before = dht.getTransport().getStats().snapshot();
					
						dht.get( 
								rhs.getBytes(), (byte)0, 32, 0,
								new DHTOperationAdapter()
								{
									public void
									read(
										DHTTransportContact	contact,
										DHTTransportValue	value )
									{
										System.out.println( "-> " + new String(value.getValue()));
									}
																	
									public void
									complete(
										boolean				timeout )
									{
										System.out.println( "-> complete" );
									}		
								});
						
						
						
					}else if ( command == 'd' ){
						
						System.out.println( "Using dht " + dht_index );
						
						stats_before = dht.getTransport().getStats().snapshot();
						
						byte[]	res = dht.remove( rhs.getBytes());
						
						System.out.println( "-> " + (res==null?"null":new String(res)));
						
					}else if ( command == 'z' ){
						
						System.out.println( "Using dht " + dht_index );
						
						stats_before = dht.getTransport().getStats().snapshot();
						
						dht.get( rhs.getBytes(), (byte)0, 10, 0,
								new DHTOperationListener()
								{
									public void
									searching(
										DHTTransportContact	contact,
										int					level,
										int					active_searches )
									{
										
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
										System.out.println( "-> " + value.getString());
	
										new AEThread("blah")
										{
											public void
											runSupport()
											{
												DHTTransportFullStats stats = contact.getStats();
										
												System.out.println( "    stats = " + stats.getString() );
											}
										}.start();
									}
									public void
									wrote(
										final DHTTransportContact	contact,
										DHTTransportValue	value )
									{
									}
									
						
									public void
									complete(
										boolean				timeout )
									{
										System.out.println( "complete");
									}
								});
						
						
					}else if ( command == 'v' ){
				
						try{
							int	index = Integer.parseInt( rhs );
					
							dht = dhts[index];
	
							stats_before = dht.getTransport().getStats().snapshot();
							
							dht.print();
	
						}catch( Throwable e ){
							
							e.printStackTrace();
						}
					}else if ( command == 't' ){
						
						try{
							int	index = Integer.parseInt( rhs );
					
							dht = dhts[index];
	
							stats_before = dht.getTransport().getStats().snapshot();
							
							((DHTTransportUDPImpl)transports[index]).testInstanceIDChange();
							
							dht.integrate();
	
						}catch( Throwable e ){
							
							e.printStackTrace();
						}
					}else if ( command == 's' ){
						
						try{
							int	index = Integer.parseInt( rhs );
					
							dht = dhts[index];
	
							stats_before = dht.getTransport().getStats().snapshot();
							
							((DHTTransportUDPImpl)transports[index]).testTransportIDChange();
							
						}catch( Throwable e ){
							
							e.printStackTrace();
						}
					}else if ( command == 'a' ){
						
						createDHT( dhts, transports, num_dhts++ );
						
						dht	= dhts[num_dhts-1];
						
						stats_before = transports[num_dhts-1].getStats().snapshot();
						
						ByteArrayOutputStream	baos = new ByteArrayOutputStream();
						
						DataOutputStream	daos = new DataOutputStream( baos );
						
						transports[(int)(Math.random()*(num_dhts-1))].getLocalContact().exportContact( daos );
						
						daos.close();
						
						transports[num_dhts-1].importContact( new DataInputStream( new ByteArrayInputStream( baos.toByteArray())));
						
						dht.integrate();
	
						dht.print();
						
					}else if ( command == 'r' ){
						
						System.out.println( "read - dht0 -> dht1" );
											
						byte[]	res = 
							dhts[0].getTransport().readTransfer(
									new DHTTransportProgressListener()
									{
										public void
										reportSize(
											long	size )
										{
											System.out.println( "   size: " + size );
										}
										
										public void
										reportActivity(
											String	str )
										{
											System.out.println( "   act: " + str );
										}
										
										public void
										reportCompleteness(
											int		percent )
										{
											System.out.println( "   %: " + percent );
										}
									},
									dhts[1].getTransport().getLocalContact(),
									th_key,
									new byte[]{1,2,3,4},
									30000 );
		
						System.out.println( "res = " + res );
						
					}else if ( command == 'w' ){
						
						System.out.println( "write - dht0 -> dht1" );
											
						dhts[0].getTransport().writeTransfer(
									dhts[1].getTransport().getLocalContact(),
									th_key,
									new byte[]{1,2,3,4},
									new byte[]{4,3,2,1},
									30000 );
		
					}else{
						
						usage();
					}
									
					if ( stats_before != null ){
						
						DHTTransportStats	stats_after = dht.getTransport().getStats().snapshot();
	
						System.out.println( "before:" + stats_before.getString());
						System.out.println( "after:" + stats_after.getString());
					}
				}catch( Throwable e ){
					
					e.printStackTrace();
				}
			}
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	protected static void
	createDHT(
		DHT[]			dhts,
		DHTTransport[]	transports,
		int				i )
	
		throws DHTTransportException
	{
		DHTTransport	transport;
		
		if ( udp_protocol ){
			
			transport = DHTTransportFactory.createUDP( 6890 + i, 5, 3, udp_timeout, logger );
			
		}else{
			
			transport = DHTTransportFactory.createLoopback(ID_BYTES);
		}
		
		transport.registerTransferHandler(
			th_key,
			new DHTTransportTransferHandler()
			{
				public byte[]
				handleRead(
					DHTTransportContact	originator,
					byte[]				key )
				{
					System.out.println("handle read");
					
					return( new byte[10000]);
				}
				
				public void
				handleWrite(
					DHTTransportContact	originator,
					byte[]				key,
					byte[]				value )
				{
					System.out.println("handle write");
					
				}
			});
		
		HashWrapper	id = new HashWrapper( transport.getLocalContact().getID());
		
		if ( check.get(id) != null ){
			
			System.out.println( "Duplicate ID - aborting" );
			
			return;
		}
		
		check.put(id,"");
		
		DHT	dht = DHTFactory.create( transport, dht_props, logger );
		
		dhts[i]	= dht;					

		transports[i] = transport;
	}
	
	protected static void
	usage()
	{
		System.out.println( "syntax: [p g] <key>[=<value>]" );
	}
}
