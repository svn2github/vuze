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
import org.gudy.azureus2.core3.util.AEMonitor;

public class 
TRTrackerClientFactoryImpl 
{
	protected static Vector	listeners 	= new Vector();
	protected static Vector	clients		= new Vector();
	protected static AEMonitor 		class_mon 	= new AEMonitor( "TRTrackerClientFactory" );

	public static TRTrackerClient
	create(
		TOTorrent		torrent,
		PEPeerServer	peer_server )
		
		throws TRTrackerClientException
	{
		TRTrackerClient	client = new TRTrackerClientClassicImpl( torrent, peer_server );
		
		try{
			class_mon.enter();
			
			clients.addElement( client );
			
			for (int i=0;i<listeners.size();i++){
			
				((TRTrackerClientFactoryListener)listeners.elementAt(i)).clientCreated( client );	
			}
		}finally{
			
			class_mon.exit();
		}
		
		return( client );
	}
	
	public static void
	addListener(
		 TRTrackerClientFactoryListener	l )
	{
		try{
			class_mon.enter();
		
			listeners.addElement(l);
			
			for (int i=0;i<clients.size();i++){
				
				l.clientCreated((TRTrackerClient)clients.elementAt(i));	
			}
		}finally{
			
			class_mon.exit();
		}
	}
	
	public static void
	removeListener(
		 TRTrackerClientFactoryListener	l )
	{
		try{
			class_mon.enter();
		
			listeners.removeElement(l);
			
		}finally{
			
			class_mon.exit();
		}
	}

	public static void
	destroy(
		TRTrackerClient	client )
	{
		try{
			class_mon.enter();
		
			clients.removeElement( client );
			
			for (int i=0;i<listeners.size();i++){
				
				((TRTrackerClientFactoryListener)listeners.elementAt(i)).clientDestroyed( client );	
			}
		}finally{
			
			class_mon.exit();
		}
	}
}
