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

/**
 * @author parg
 *
 */

public class 
Test 
{
	public static void
	main(
		String[]		args )
	{
		DHTLog.setLoggingEnabled( false );
		
		try{
			int		K			= 5;
			int		B			= 1;
			int		ID_BYTES	= 4;
			
			DHT[]			dhts 		= new DHT[100];
			DHTTransport[]	transports 	= new DHTTransport[dhts.length];
			
			Map	check = new HashMap();
			
			for (int i=0;i<dhts.length;i++){
				
				DHT	dht = DHTFactory.create( K, B );
			
				dhts[i]	= dht;
				
				DHTTransport	transport = DHTTransportFactory.createLoopback(ID_BYTES);
			
				HashWrapper	id = new HashWrapper( transport.getLocalContact().getID());
				
				if ( check.get(id) != null ){
					
					System.out.println( "Duplicate ID - aborting" );
					
					return;
				}
				
				check.put(id,"");
				
				transports[i] = transport;
				
				dht.setTransport( transport );
			}

			for (int i=0;i<dhts.length-1;i++){
			
				transports[i+1].importContact( new ByteArrayInputStream( transports[i].getLocalContact().getID()));
				
				dhts[i].join();
			}
			
			transports[transports.length-1].importContact( new ByteArrayInputStream( transports[0].getLocalContact().getID()));
			
			dhts[transports.length-1].join();
			
			DHTLog.setLoggingEnabled( true );

			//dht1.print();
			
			//DHTTransportLoopbackImpl.setLatency( 500);
			
			/*
			System.out.println( "before put:" + transports[99].getStats().getString());
			dhts[99].put( "fred".getBytes(), new byte[2]);
			System.out.println( "after put:" + transports[99].getStats().getString());

			System.out.println( "get:"  + dhts[0].get( "fred".getBytes()));
			System.out.println( "get:"  + dhts[77].get( "fred".getBytes()));
			*/
			
			LineNumberReader	reader = new LineNumberReader( new InputStreamReader( System.in ));
			
			while( true ){
				
				System.out.print( "> " );
				
				String	str = reader.readLine().trim();
				
				if ( str == null ){
					
					break;
				}
				
				int	pos = str.indexOf(' ');
				
				if ( pos == -1 || pos == 0 ){
					
					System.out.println( "syntax: [p g] <key>[=<value>]" );
					
					continue;
				}
				
				int	dht_index = (int)(Math.random()*dhts.length);
				
				DHT	dht = dhts[dht_index];
				
				System.out.println( "Using dht " + dht_index );
				
				String	lhs = str.substring(0,pos);
				String	rhs = str.substring(pos+1);
				
				DHTTransportStats	stats_before = dht.getTransport().getStats().snapshot();
				
				if ( lhs.toLowerCase().startsWith("p")){
					
					pos = rhs.indexOf('=');
					
					if ( pos == -1 ){
						
						System.out.println( "syntax: [p g] <key>[=<value>]" );
						
					}else{
					
						String	key = rhs.substring(0,pos);
						String	val = rhs.substring(pos+1);
						
						dht.put( key.getBytes(), val.getBytes());
					}
				}else{
					
					byte[]	res = dht.get( rhs.getBytes());
					
					System.out.println( "-> " + (res==null?"null":new String(res)));
				}
				
				dht.print();
				
				DHTTransportStats	stats_after = dht.getTransport().getStats().snapshot();

				System.out.println( "before:" + stats_before.getString());
				System.out.println( "after:" + stats_after.getString());
				
			}
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
}
