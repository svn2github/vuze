/*
 * Created on 01-Jul-2004
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

package org.gudy.azureus2.core3.tracker.server.test;

/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.core3.tracker.client.classic.*;
import org.gudy.azureus2.core3.tracker.server.TRTrackerServerRequestListener;
import org.gudy.azureus2.core3.tracker.server.impl.*;
import org.gudy.azureus2.core3.util.HashWrapper;

public class 
LoadTest 
{
	static int	ST_READY			= 0;
	static int	ST_STARTED			= 1;
	static int	ST_COMPLETED		= 2;
	static int	ST_STOPPED			= 3;
	
	int	address1	= 0;
	int	address2	= 0;
	int	address3	= 0;
	int	address4	= 0;
	
	int	num_clients	= 100000;
	loadTestClient[]	clients;
	
	
	TRTrackerServerTorrentImpl torrent;

	protected
	LoadTest()
	{
		int	timeout = 100;
		
		TRTrackerServerImpl.RETRY_MINIMUM_SECS		= timeout;
		TRTrackerServerImpl.RETRY_MINIMUM_MILLIS	= timeout*1000;
		
		TRTrackerServerImpl.TIMEOUT_CHECK 		    = timeout*1000;
		
		TRTrackerServerImpl	server = 
			new TRTrackerServerImpl( "test" )
			{
				public int
				getPort()
				{
					return( 1234 );
				}
				public String
				getHost()
				{
					return( "fred" );
				}
				
				public boolean
				isSSL()
				{
					return( false );
				}
				public void
				addRequestListener(
					TRTrackerServerRequestListener	l )
				{	
				}
				
				public void
				removeRequestListener(
					TRTrackerServerRequestListener	l )
				{	
				}
			};
			
		try{
			torrent = (TRTrackerServerTorrentImpl)server.permit("jkjkjkj".getBytes(), true);
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
		
		clients = new loadTestClient[num_clients];
		
		for (int i=0;i<clients.length;i++){
			clients[i] = new loadTestClient();
		}
		
		Runtime	rt= Runtime.getRuntime();
		rt.gc();
		
		long	initial_mem 	= rt.totalMemory() - rt.freeMemory();
		long	initial_time	= System.currentTimeMillis()-1;
		
		System.out.println( "used = " +initial_mem );
	
		int	loop = 0;
		
		while(true){
			
			loop++;
		
			if (loop%10000==0){
				
				System.out.println( loop +": peers = " + torrent.getPeerCount() + ",seeds = " + torrent.getSeedCount() + ", leechers = " + torrent.getLeecherCount());
		
				rt.gc();
				
				long	current_mem = rt.totalMemory() - rt.freeMemory();

				long	mem_per_peer = (current_mem-initial_mem)/torrent.getPeerCount();
				
				long	now	= System.currentTimeMillis();

				System.out.println( "used = " + ( rt.totalMemory() - rt.freeMemory()) + ": per peer = " + mem_per_peer  + ", per sec = " + ( loop*1000L/ (now-initial_time )));
			}
			
			loadTestClient	client = clients[(int)(Math.random()*clients.length)];
		
			int	state = client.getState();
			
			if ( state == ST_READY ){
				
				client.start();
				
			}else if ( state == ST_STARTED ){
				
				if( chance(2)){
					
					client.update();
					
				}else if (chance(10)){
					
					client.changeAddress();
					
					client.reset();
				}else{
					
					client.complete();
				}
				
			}else if ( state == ST_COMPLETED ){
				
				client.stop();
				
			}else if ( state == ST_STOPPED ){
				
				client.reset();
			}
		}
	}
	
	protected boolean
	chance(
		int		one_in )
	{
		return(((int)(Math.random()*one_in )) == 0 ); 
	}
	
	class
	loadTestClient
	{
		int			state		= ST_READY;
		String		peer_id;
		String		key;
		String		address;
		
		int			uploaded;
		int			downloaded;
		int			left		= 100;
		
		int			interval	= 10;
		int			num_want	= 100;
		
		loadTestClient()
		{
			state	= ST_READY;

			try{
				peer_id = new String(TRTrackerClientClassicImpl.createPeerID(), "ISO-8859-1" );
			}catch( Throwable e ){
				e.printStackTrace();
			}
			
			key		= TRTrackerClientClassicImpl.createKeyID();
			
			changeAddress();
		}
		
		int
		getState()
		{
			return( state );
		}
		
		void
		setState(
			int		_state )
		{
			state	= _state;
		}
		
		void
		changeAddress()
		{			
			address = address1+"."+address2+ "."+address3+"."+address4;

			if ( ++address4 == 256 ){
				address4 = 0;
				if ( ++address3 == 256 ){
					address3 = 0;
					if ( ++address2 == 256 ){
						address2=0;
						address1++;
					}
				}
			}
		}
		
		void 
		reset()
		{
			state	= ST_READY;
			
			left	= 100;
			
			try{
				peer_id = new String(TRTrackerClientClassicImpl.createPeerID(), "ISO-8859-1" );
			}catch( Throwable e ){
				e.printStackTrace();
			}		
		}
		
		void
		start()
		{
			try{
				torrent.peerContact("started", peer_id.toString(), 6881, address,  key, uploaded, downloaded, left, num_want, interval );
				
				Map m = torrent.exportAnnounceToMap( true, num_want, 4, true, true );
				
				/*
				byte[]	l = (byte[])m.get("peers");
				int	seeds = ((Long)m.get("complete")).intValue();
				int	leechers = ((Long)m.get("incomplete")).intValue();
				System.out.println( l.length / 6 + ","+seeds+","+leechers);
				*/
				
				state	= ST_STARTED;
				
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
		}
		
		void
		stop()
		{
			try{
				torrent.peerContact("stopped", peer_id.toString(), 6881, address,  key, uploaded, downloaded, left, num_want, interval );
				
				state	= ST_STOPPED;
				
			}catch( Throwable e ){
				
				e.printStackTrace();
			}		
		}
	
		void
		update()
		{
			try{
				left	= 0;
				
				torrent.peerContact(null, peer_id.toString(), 6881, address,  key, uploaded, downloaded, left, num_want, interval );
	
				Map m = torrent.exportAnnounceToMap( true, num_want, 4, true, true );
			
				state	= ST_COMPLETED;
				
			}catch( Throwable e ){
				
				e.printStackTrace();
			}			
		}
		void
		complete()
		{
			try{
				left	= 0;
				
				torrent.peerContact("complete", peer_id.toString(), 6881, address,  key, uploaded, downloaded, left, num_want, interval );
	
				Map m = torrent.exportAnnounceToMap( true, num_want, 4, true, true );
			
				state	= ST_COMPLETED;
				
			}catch( Throwable e ){
				
				e.printStackTrace();
			}			
		}
	}
	
	public static void
	main(
		String[]	args )
	{
		new LoadTest();
	}
}
