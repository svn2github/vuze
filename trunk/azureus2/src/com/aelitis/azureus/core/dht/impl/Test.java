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
			
			DHT[]			dhts 		= new DHT[1000];
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
				
				dht.addTransport( transport );
			}
			
			for (int i=0;i<dhts.length-1;i++){
			
				transports[i].importContact( new ByteArrayInputStream( transports[i+1].getLocalContact().getID()));
			}
			
			transports[transports.length-1].importContact( new ByteArrayInputStream( transports[0].getLocalContact().getID()));
			
			//dht1.print();
			
			DHTLog.setLoggingEnabled( true );
			//DHTTransportLoopbackImpl.setLatency( 500);
			
			dhts[99].put( "fred".getBytes(), new byte[2]);
			
			System.out.println( "get:"  + dhts[0].get( "fred".getBytes()));
			System.out.println( "get:"  + dhts[777].get( "fred".getBytes()));
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
}
