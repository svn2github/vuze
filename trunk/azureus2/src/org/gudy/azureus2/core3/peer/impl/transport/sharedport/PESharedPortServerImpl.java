/*
 * File    : PESharedPortServerImpl.java
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

import java.io.IOException;
import java.nio.channels.SocketChannel;

import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.peer.impl.*;

import org.gudy.azureus2.core3.peer.impl.transport.base.*;

public class 
PESharedPortServerImpl
	implements PEPeerServerHelper
{
	protected static PEPeerServerHelper		server_delegate;
	protected static PESharedPortSelector	selector;
	
	protected PEPeerServerAdapter	adapter;
	
	public
	PESharedPortServerImpl()
	{
		synchronized( PESharedPortServerImpl.class ){
			
			if ( server_delegate == null ){
				
				try{
				
					selector = new PESharedPortSelector();
										
					server_delegate = (PEPeerServerHelper)PEPeerServerImpl.create();
					
					if ( server_delegate == null || server_delegate.getPort() == 0 ){
						
							// no ports available
							
						server_delegate = null;
						
					}else{
					
						server_delegate.setServerAdapter( 
							new PEPeerServerAdapter()
							{
								public void
								addPeerTransport(
									Object		param )
								{
										// new incoming connection
                  
									selector.addSocket((SocketChannel)param);
								}
									
								public PEPeerControl
								getControl()
								{
									System.out.println( "PESharedPortServer::getControl - should never be called!!!!");
				
									throw( new RuntimeException( "whoops!"));
								}
							});
							
						server_delegate.startServer();
					}
				}catch( IOException e ){
					
					String	message = e.getMessage();
					
					LGLogger.log(0, 0, LGLogger.INFORMATION, "PESharedPortServer: failed to establish selector" + e.toString());
					
					LGLogger.logAlertUsingResource(	LGLogger.AT_ERROR, "SharedPortServer.alert.selectorfailed" ); 
										
					e.printStackTrace();
				}
			}
		}
	}
	
	public int
	getPort()
	{
		return( server_delegate==null?0:server_delegate.getPort());
	}
	
	public void
	startServer()
	{		
		selector.addHash( this, adapter.getControl().getHash() );
	}
	
	public void
	stopServer()
	{
		selector.removeHash( this, adapter.getControl().getHash() );
	}
	
	public void
	setServerAdapter(
		PEPeerServerAdapter	_adapter )
	{
		adapter	= _adapter;
	}
		
	protected void
	connectionReceived(
		SocketChannel		socket,
		byte[]				data_read )
	{		
		adapter.addPeerTransport( new Object[]{ socket, data_read });
	}
	
	public PEPeerTransport
	createPeerTransport(
		Object		param )
	{
		Object[]	temp = (Object[])param;
		
		SocketChannel	channel = (SocketChannel)temp[0];
		byte[]			data	= (byte[])temp[1];
				
		return( new PEPeerTransportImpl( adapter.getControl(), channel, data ));
	}
}
