/*
 * File    : TRTrackerClientFactoryImpl.java
 * Created : 04-Nov-2003
 * By      : parg
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.core3.tracker.client.classic;

/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.core3.peer.PEPeerServer;

import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.tracker.client.*;
import org.gudy.azureus2.core3.util.*;

public class 
TRTrackerClientFactoryImpl 
{
	protected static List	listeners 	= new ArrayList();
	protected static List	clients		= new ArrayList();
	
	protected static AEMonitor 		class_mon 	= new AEMonitor( "TRTrackerClientFactory" );

	public static TRTrackerClient
	create(
		TOTorrent		torrent,
		PEPeerServer	peer_server )
		
		throws TRTrackerClientException
	{
		TRTrackerClient	client = new TRTrackerClientClassicImpl( torrent, peer_server );
		
		List	listeners_copy	= new ArrayList();
		
		try{
			class_mon.enter();
			
			clients.add( client );
		
			listeners_copy = new ArrayList( listeners );
	
		}finally{
			
			class_mon.exit();
		}
		
		for (int i=0;i<listeners_copy.size();i++){
			
			try{
				((TRTrackerClientFactoryListener)listeners_copy.get(i)).clientCreated( client );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
		
		return( client );
	}
	
		/*
		 * At least once semantics for this one
		 */
	
	public static void
	addListener(
		 TRTrackerClientFactoryListener	l )
	{
		List	clients_copy;
		
		try{
			class_mon.enter();
		
			listeners.add(l);
			
			clients_copy = new ArrayList( clients );
	
		}finally{
			
			class_mon.exit();
		}
		
		for (int i=0;i<clients_copy.size();i++){
			
			try{
				l.clientCreated((TRTrackerClient)clients_copy.get(i));
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
	public static void
	removeListener(
		 TRTrackerClientFactoryListener	l )
	{
		try{
			class_mon.enter();
		
			listeners.remove(l);
			
		}finally{
			
			class_mon.exit();
		}
	}

	public static void
	destroy(
		TRTrackerClient	client )
	{
		List	listeners_copy	= new ArrayList();
		
		try{
			class_mon.enter();
		
			clients.remove( client );
			
			listeners_copy	= new ArrayList( listeners );

		}finally{
			
			class_mon.exit();
		}
		
		for (int i=0;i<listeners_copy.size();i++){
			
			try{
				((TRTrackerClientFactoryListener)listeners_copy.get(i)).clientDestroyed( client );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
}
