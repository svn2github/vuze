/*
 * Created on 10 Jul 2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.nat;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.ThreadPool;
import org.gudy.azureus2.plugins.PluginInterface;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.nat.DHTNATPuncher;
import com.aelitis.azureus.core.dht.nat.DHTNATPuncherAdapter;
import com.aelitis.azureus.core.networkmanager.impl.tcp.TCPNetworkManager;
import com.aelitis.azureus.core.networkmanager.impl.udp.UDPNetworkManager;
import com.aelitis.azureus.plugins.dht.DHTPlugin;

public class 
NATTraverser 
	implements DHTNATPuncherAdapter
{
	public static final int	TRAVERSE_REASON_PEER_DATA	= 1;
	
	private static final int	MAX_QUEUE_SIZE	= 128;
	
	private AzureusCore		core;
	private DHTNATPuncher	puncher;
	
	private ThreadPool	thread_pool = new ThreadPool("NATTraverser", 16, true );
	
	private Map	handlers = new HashMap();
	
	public
	NATTraverser(
		AzureusCore		_core )
	{
		core	= _core;
	}
	
	public void
	registerHandler(
		int						type,
		NATTraversalHandler		handler )
	{
		synchronized( handlers ){
			
			handlers.put( new Integer(type), handler );
		}
	}
	
	public NATTraversal
	attemptTraversal(
		final int						type,
		final InetSocketAddress			target,
		final Map						request,
		boolean							sync,
		final NATTraversalObserver		listener )
	{
		final NATTraversal traversal = 
			new NATTraversal()
			{
				private boolean cancelled;
				
				public void 
				cancel() 
				{
					cancelled	= true;
				}
				
				public boolean
				isCancelled()
				{
					return( cancelled );
				}
			};
			
		if ( sync ){
			
			syncTraverse( type, target, request, listener );
			
		}else{
			
			if ( thread_pool.getQueueSize() >= MAX_QUEUE_SIZE ){
				
				Debug.out( "NATTraversal queue full" );
				
				listener.failed( new Exception( "queue full" ));
				
			}else{
				
				thread_pool.run(
					new AERunnable()
					{
						public void 
						runSupport() 
						{
							if ( traversal.isCancelled()){
								
								listener.failed(new Exception( "Cancelled" ));
								
							}else{
								
								syncTraverse( type, target, request, listener );
							}
						}
					});
			}
		}
		
		return( traversal );
	}
	
	protected void
	syncTraverse(
		int						type,
		InetSocketAddress		target,
		Map						request,
		NATTraversalObserver	listener )
	{
		try{
			
			synchronized( this ){
				
				if ( puncher == null ){
			
					PluginInterface dht_pi = 
						core.getPluginManager().getPluginInterfaceByClass( DHTPlugin.class );
					
					if ( dht_pi != null ){
						
						DHTPlugin dht_plugin = (DHTPlugin)dht_pi.getPlugin();
						
						if ( dht_plugin.isEnabled()){
							
							DHT[]	dhts = dht_plugin.getDHTs();

							puncher = dhts[dhts.length-1].getNATPuncher();
						}
					}
				}
				
				if ( puncher == null ){
					
					listener.disabled();
					
					return;
				}
			}

			if ( request == null ){
				
				request = new HashMap();
			}
			
			request.put( "_travreas", new Long( type ));
			
			InetSocketAddress[]	target_a = { target };
			
			Map	reply = puncher.punch( target_a, request );
			
			if ( reply == null ){
				
				listener.failed( new Exception( "NAT traversal failed" ));
				
			}else{
				
				listener.succeeded( target_a[0], reply );
			}
		}catch( Throwable e ){
			
			listener.failed( e );
		}
	}
	
	public Map
	getClientData(
		InetSocketAddress	originator,
		Map					originator_data )
	{
		System.out.println( "DHTPlugin:punch - " + originator_data );
		
		Long	type = (Long)originator_data.get( "_travreas" );
		
		if ( type != null ){
			
			NATTraversalHandler	handler;
			
			synchronized( handlers ){
				
				handler = (NATTraversalHandler)handlers.get( new Integer( type.intValue()));
			}
			
			
			if ( handler != null ){
				
				return( handler.process( originator, originator_data ));
			}
		}
		
		return( null );
	}
}
