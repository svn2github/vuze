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

import java.io.*;
import java.util.*;

import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.Timer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;

/**
 * @author parg
 *
 */

public class 
Test 
{
	static int		K			= 5;
	static int		B			= 1;
	static int		ID_BYTES	= 4;
	
	static Map	check = new HashMap();

	public static void
	main(
		String[]		args )
	{
		DHTLog.setLoggingEnabled( false );
		
		try{
			int	num_dhts	= 100;
			
			DHT[]			dhts 		= new DHT[num_dhts*2];
			DHTTransport[]	transports 	= new DHTTransport[num_dhts*2];
			
			
			for (int i=0;i<num_dhts;i++){
				
				createDHT( dhts, transports, i );
			}

			for (int i=0;i<num_dhts-1;i++){
			
				transports[i+1].importContact( new ByteArrayInputStream( transports[i].getLocalContact().getID()));
				
				dhts[i].join();
			}
			
			transports[num_dhts-1].importContact( new ByteArrayInputStream( transports[0].getLocalContact().getID()));
			
			dhts[num_dhts-1].join();
			
			DHTTransportLoopbackImpl.setFailPercentage(0);
			
			//dht1.print();
			
			//DHTTransportLoopbackImpl.setLatency( 500);
			
			/*
			System.out.println( "before put:" + transports[99].getStats().getString());
			dhts[99].put( "fred".getBytes(), new byte[2]);
			System.out.println( "after put:" + transports[99].getStats().getString());

			System.out.println( "get:"  + dhts[0].get( "fred".getBytes()));
			System.out.println( "get:"  + dhts[77].get( "fred".getBytes()));
			*/
			
			for (int i=0;i<1000;i++){
				
				int	dht_index = (int)(Math.random()*num_dhts);
				
				DHT	dht = dhts[dht_index];

				dht.put( (""+i).getBytes(), new byte[4] );
				
				if ( i != 0 && i %1000 == 0 ){
					
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
						DHTTransportStats stats = DHTTransportLoopbackImpl.getOverallStats();
						
						System.out.println( "Overall stats: " + stats.getString());
					}
				});
			
			LineNumberReader	reader = new LineNumberReader( new InputStreamReader( System.in ));
			
			DHTLog.setLoggingEnabled( true );

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
				}else if ( command == 'g' ){
					
					System.out.println( "Using dht " + dht_index );
					
					stats_before = dht.getTransport().getStats().snapshot();
					
					byte[]	res = dht.get( rhs.getBytes());
					
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
				}else if ( command == 'a' ){
					
					createDHT( dhts, transports, num_dhts++ );
					
					dht	= dhts[num_dhts-1];
					
					stats_before = transports[num_dhts-1].getStats().snapshot();
					
					transports[num_dhts-1].importContact( new ByteArrayInputStream( transports[(int)(Math.random()*(num_dhts-1))].getLocalContact().getID()));
					
					dht.join();

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
	{
		DHTTransport	transport = DHTTransportFactory.createLoopback(ID_BYTES);
		
		HashWrapper	id = new HashWrapper( transport.getLocalContact().getID());
		
		if ( check.get(id) != null ){
			
			System.out.println( "Duplicate ID - aborting" );
			
			return;
		}
		
		check.put(id,"");
		
		DHT	dht = DHTFactory.create( transport, K, B );
		
		dhts[i]	= dht;					

		transports[i] = transport;
	}
	
	protected static void
	usage()
	{
		System.out.println( "syntax: [p g] <key>[=<value>]" );
	}
}
