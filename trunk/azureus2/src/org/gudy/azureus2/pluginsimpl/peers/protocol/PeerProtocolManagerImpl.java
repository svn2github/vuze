/*
 * File    : PeerProtocolManagerImpl.java
 * Created : 28-Dec-2003
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

package org.gudy.azureus2.pluginsimpl.peers.protocol;

/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.plugins.peers.protocol.*;
import org.gudy.azureus2.pluginsimpl.peers.*;

import org.gudy.azureus2.core3.peer.impl.*;

public class 
PeerProtocolManagerImpl
	implements PeerProtocolManager
{
	protected static PeerProtocolManager	singleton;
	
	public synchronized static PeerProtocolManager
	getSingleton()
	{
		if ( singleton == null ){
			
			singleton = new PeerProtocolManagerImpl();
		}
		
		return( singleton );
	}
	
	public void
	registerExtensionHandler(
		String								protocol_name,
		final PeerProtocolExtensionHandler	handler )
	{
		PEPeerTransportFactory.registerExtensionHandler(
				protocol_name,
				new PEPeerTransportExtensionHandler()
				{
					public List
					handleExtension(
						PEPeerControl	manager,
						Map				details )
					{
					
						List	res = handler.handleExtension(
										new PeerManagerImpl(manager),
										details );
						
						return( res );
					}
				});

	}
}
