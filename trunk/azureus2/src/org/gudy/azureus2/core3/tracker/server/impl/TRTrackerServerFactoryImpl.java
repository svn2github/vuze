/*
 * File    : TRTrackerServerFactoryImpl.java
 * Created : 13-Dec-2003
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

package org.gudy.azureus2.core3.tracker.server.impl;

/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.core3.tracker.server.*;

public class 
TRTrackerServerFactoryImpl 
{
	protected static List	servers		= new ArrayList();
	protected static List	listeners 	= new ArrayList();
	
	public static synchronized TRTrackerServer
	create(
		int			port,
		boolean		ssl)
	
		throws TRTrackerServerException
	{
		TRTrackerServerImpl	server = new TRTrackerServerImpl( port, ssl );
		
		servers.add( server );
		
		for (int i=0;i<listeners.size();i++){
			
			((TRTrackerServerFactoryListener)listeners.get(i)).serverCreated( server );
		}
		
		return( server );
	}
	
	public static synchronized void
	addListener(
		TRTrackerServerFactoryListener	l )
	{
		listeners.add( l );
		
		for (int i=0;i<servers.size();i++){
			
			l.serverCreated((TRTrackerServer)servers.get(i));
		}
	}	
	
	public static synchronized void
	removeListener(
		TRTrackerServerFactoryListener	l )
	{
		TRTrackerServerFactoryImpl.removeListener( l );
	}
}
