/*
 * Created on May 30, 2014
 * Created by Paul Gardner
 * 
 * Copyright 2014 Azureus Software, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.plugins.tracker.dht;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.*;

import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.ByteArrayHashMap;
import org.gudy.azureus2.core3.util.RandomUtils;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;


import com.aelitis.azureus.core.dht.transport.DHTTransportAlternativeContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportAlternativeNetwork;
import com.aelitis.azureus.core.dht.transport.udp.impl.DHTUDPUtils;

public class 
DHTTrackerPluginAlt 
{
	private static final int INITAL_DELAY	= 5*1000;
			
	private final byte[]	NID = new byte[20];
	
	private DatagramSocket	current_server;
	
	private ByteArrayHashMap<GetPeersTask>	tid_map = new ByteArrayHashMap<GetPeersTask>();

	
	protected
	DHTTrackerPluginAlt()
	{
		RandomUtils.nextBytes( NID );
	}
	
	private DatagramSocket
	getServer()
	{
		synchronized( this ){
			
			if ( current_server != null ){
				
				if ( current_server.isClosed()){
					
					current_server = null;
					
				}else{
					
					return( current_server );
				}
			}
	
			try{
				final DatagramSocket server = new DatagramSocket(null);
				
				server.setReuseAddress(true);
				
				//sock.bind(new InetSocketAddress(addr, port));
		
				server.bind( null );
					
				current_server = server;
				
				new AEThread2( "DHTPluginAlt:server" )
				{
					public void
					run()
					{
						try{
							while( true ){
								
								byte[] buffer = new byte[5120];
								
								DatagramPacket packet = new DatagramPacket( buffer, buffer.length );
					
								server.receive( packet );
								
								Map<String, Object> map = new BDecoder().decodeByteArray(packet.getData(), 0, packet.getLength() ,false);
								
								System.out.println( "got " + map );
								
								byte[]	tid = (byte[])map.get( "t" );
								
								if ( tid != null ){
									
									GetPeersTask task;
									
									synchronized( tid_map ){
					
										task = tid_map.remove( tid );
									}
									
									if ( task != null ){
										
										task.handleReply( map );
									}
								}
							}
						}catch( Throwable e ){
							
						}finally{
							
							try{
								server.close();
								
							}catch( Throwable f ){
							}
							
							synchronized( DHTTrackerPluginAlt.this ){
								
								if ( current_server == server ){
									
									current_server = null;
								}
							}
						}
					}
				}.start();
		
				return( server );
				
			}catch( Throwable e ){
				
				return( null );
			}
		}
	}
	
	protected void
	get(
		final byte[]				hash,
		final boolean				no_seeds,
		final LookupListener		listener )
	{	
		SimpleTimer.addEvent(
			"altlookup.delay",
			SystemTime.getCurrentTime() + INITAL_DELAY,
			new TimerEventPerformer() 
			{	
				public void 
				perform(
					TimerEvent event) 
				{
					if ( listener.isComplete()){
						
						return;
					}
					
					getSupport( hash, no_seeds, listener );
				}
			});
	}
	
	private void
	getSupport(
		final byte[]				hash,
		final boolean				no_seeds,
		final LookupListener		listener )
	{
		List<DHTTransportAlternativeContact> contacts = DHTUDPUtils.getAlternativeContacts( DHTTransportAlternativeNetwork.AT_MLDHT_IPV4, 16 );
		
		if ( contacts.size() == 0 ){
		
				// could try in a bit if start-of-day?
			
			return;
		}
		
		DatagramSocket	server = getServer();
		
		if ( server == null ){
			
			return;
		}
		
		new GetPeersTask( server, contacts, hash, no_seeds, listener );
	}
	
	private void
	send(
		GetPeersTask		task,
		DatagramSocket		server,
		InetSocketAddress	address,
		Map					map )
		
		throws IOException
	{
		while( true ){
			
			byte[]	tid = new byte[4];
			
			RandomUtils.nextBytes( tid );
	
			synchronized( tid_map ){
				
				if ( tid_map.containsKey( tid )){
					
					continue;
				}
				
				tid_map.put( tid, task );
			}
			
			map.put( "t", tid );
	
			byte[] 	data_out = BEncoder.encode( map );
			
			DatagramPacket packet = new DatagramPacket( data_out, data_out.length);
			
			packet.setSocketAddress( address );
	
			server.send( packet );
			
			break;
		}
	}
	
	private class
	GetPeersTask
	{
		private DatagramSocket		server;
		private byte[]				torrent_hash;
		private boolean				no_seeds;
		private LookupListener		listener;
		
		
		private Set<InetSocketAddress>	queried_nodes = new HashSet<InetSocketAddress>();
		
		private TreeMap<byte[],InetSocketAddress>	to_query =
			new TreeMap<byte[], InetSocketAddress>(
				new Comparator<byte[]>()
				{
					public int 
					compare(
						byte[] o1, 
						byte[] o2) 
					{
						for ( int i=0; i < o1.length;i++ ){
							
							byte b1 = o1[i];
							byte b2 = o2[i]; 

							if ( b1 == b2 ){
								
								continue;
							}
							
							byte t = torrent_hash[i];
							
							int d1 = (b1^t)&0xff;
							int d2 = (b2^t)&0xff; 
								
							if ( d1 == d2 ){
								
								continue;
							}

							if ( d1 < d2 ){
								
								return -1;
								
							}else{
								
								return 1;
							}
						}
						
						return( 0 );
					}
				});
		
		private
		GetPeersTask(
			DatagramSocket								_server,
			List<DHTTransportAlternativeContact>		_contacts,
			byte[]										_torrent_hash,
			boolean										_no_seeds,
			LookupListener								_listener )
		{
			server			= _server;
			torrent_hash	= _torrent_hash;
			no_seeds		= _no_seeds;
			listener		= _listener;
			
			Map<String,Object>	properties = _contacts.get(0).getProperties();
			
			byte[]	_a 	= (byte[])properties.get( "a" );
			Long	_p	= (Long)properties.get( "p" );
			
			if ( _a != null && _p != null ){
			
				try{
					InetSocketAddress address = new InetSocketAddress( InetAddress.getByAddress( _a ), _p.intValue());
				
					search( address );
					
				}catch( Throwable e ){
					
				}
			}
		}
		
		private void
		search(
			InetSocketAddress	address )
			
			throws IOException
		{
			synchronized( queried_nodes ){
			
				if ( queried_nodes.contains( address )){
					
					return;
				}
				
				queried_nodes.add( address );
			}
			
			Map<String,Object> map = new HashMap<String,Object>();
						
			map.put( "q", "get_peers" );
			map.put( "y", "q" );
			
			Map<String,Object> args = new HashMap<String,Object>();
			
			map.put( "a", args );
			
			args.put( "id", NID );
			
			args.put( "info_hash", torrent_hash );
			
			args.put( "noseed", new Long( no_seeds?1:0 ));
							
			send( this, server, address, map );
		}
		
		private void
		handleTimeout()
		{
			
		}
		
		private void
		handleReply(
			Map<String,Object>		map )
			
			throws IOException
		{
			Map<String,Object> reply = (Map<String,Object>)map.get( "r" );

			ArrayList<byte[]>	values = ( ArrayList<byte[]>)reply.get( "values" );
		
			if ( values != null ){
				
				for ( byte[] value: values ){
					
					try{
						ByteBuffer bb = ByteBuffer.wrap( value );
						
						byte[]	address = new byte[value.length-2];
						
						bb.get( address );
						
						int	port = bb.getShort() & 0xffff;
						
						InetSocketAddress addr = new InetSocketAddress( InetAddress.getByAddress(address), port );
						
						System.out.println( "Got value: " + addr );
						
					}catch( Throwable e ){
					}
				}
			}
			
			byte[]	nodes 	= (byte[])reply.get( "nodes" );
			byte[]	nodes6 	= (byte[])reply.get( "nodes6" );
		
			if ( nodes != null ){
			
				int	entry_size = 20+4+2;
				
				for ( int i=0;i<nodes.length;i+=entry_size ){
					
					ByteBuffer bb = ByteBuffer.wrap(nodes, i, entry_size );

					byte[] nid = new byte[20];
					
					bb.get(nid);

					byte[] address = new byte[ 4 ];
				
					bb.get( address );

					int port = bb.getShort()&0xffff;
					
					try{
						InetSocketAddress addr = new InetSocketAddress( InetAddress.getByAddress(address), port );
				
						to_query.put( nid, addr );
						
					}catch( Throwable e ){
						
					}
				}
				
				for ( Map.Entry<byte[],InetSocketAddress> entry: to_query.entrySet()){
					
					//System.out.println( ByteFormatter.encodeString( entry.getKey()) + " -> " + entry.getValue());
					
					search( entry.getValue());
				}
			}
		}
	}
	
	
	
	protected interface
	LookupListener
	{
		public void
		foundPeer(
			InetSocketAddress	address );
		
		public boolean
		isComplete();
	}
}
