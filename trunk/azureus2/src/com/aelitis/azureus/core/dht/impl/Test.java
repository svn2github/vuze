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

import com.aelitis.azureus.core.dht.*;
import com.aelitis.azureus.core.dht.transport.*;
import com.aelitis.azureus.core.dht.transport.loopback.DHTTransportLoopbackImpl;
import com.aelitis.azureus.core.dht.transport.udp.DHTTransportUDP;
import com.aelitis.azureus.core.dht.transport.udp.impl.DHTTransportUDPImpl;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;

import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.Timer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
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
	}
	
	static Map	check = new HashMap();

	public static LoggerChannel	logger = 
		new LoggerChannel()
		{
			public String
			getName()
			{
				return( "" );
			}
			
			public void
			log(
				int		log_type,
				String	data )
			{
				log( data );
			}
			
			public void
			log(
				String	data )
			{
				System.out.println( data );
			}
			
			public void
			log(
				Throwable 	error )
			{
				error.printStackTrace();
			}
			
			public void
			log(
				String		data,
				Throwable 	error )
			{
				log( data );
				log( error );
			}
			
			public void
			logAlert(
				int			alert_type,
				String		message )
			{
				log( message );
			}
			
			public void
			logAlert(
				String		message,
				Throwable 	e )
			{
				log( message );
				log( e );
			}
		
			public void
			logAlertRepeatable(
				int			alert_type,
				String		message )
			{
				log( message );
			}
			
			public void
			logAlertRepeatable(
				String		message,
				Throwable 	e )
			{
				log( message );
				log( e );
			}
			
			public void
			addListener(
				LoggerChannelListener	l )
			{
			}
			
			public void
			removeListener(
				LoggerChannelListener	l )
			{
			}
		};
		
	public static void
	main(
		String[]		args )
	{
		
		try{
			
			DHT[]			dhts 		= new DHT[num_dhts*2];
			DHTTransport[]	transports 	= new DHTTransport[num_dhts*2];
			
			
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
						
						dht.put( key.getBytes(), val.getBytes());
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
					
					byte[]	res = dht.get( rhs.getBytes(), 0);
					
					System.out.println( "-> " + (res==null?"null":new String(res)));
					
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
					
				}else{
					
					usage();
				}
								
				if ( stats_before != null ){
					
					DHTTransportStats	stats_after = dht.getTransport().getStats().snapshot();

					System.out.println( "before:" + stats_before.getString());
					System.out.println( "after:" + stats_after.getString());
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
