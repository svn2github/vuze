/*
 * File    : PESharedPortSelector.java
 * Created : 24-Nov-2003
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

package org.gudy.azureus2.core3.peer.impl.transport.sharedport;

/**
 * @author parg
 *
 */

import java.io.*;
import java.nio.channels.*;

public class 
PESharedPortSelector 
{
	protected Selector	selector;
	
	protected
	PESharedPortSelector()
	
		throws IOException
	{
		selector = Selector.open();
		
		Thread t = new Thread("PESharedPortSelector")
			{
				public void
				run()
				{
					selectLoop();
				}
			};
			
		t.setDaemon(true);
		
		t.start();
	}
	
	protected void
	selectLoop()
	{
	}
	
	public void
	addSocket(
		SocketChannel		_socket )
	{
		System.out.println( "socket added");
	}
	
	public void
	addHash(
		PESharedPortServerImpl		_server,
		byte[]						_hash )
	{
		System.out.println( "hash added");
	}	
	
	public void
	removeHash(
		PESharedPortServerImpl		_server,
		byte[]						_hash )
	{
		System.out.println( "hash removed");
	}
}
