/*
 * File    : DiskManagerImpl.java
 * Created : 22-Mar-2004
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

package org.gudy.azureus2.pluginsimpl.local.disk;

/**
 * @author parg
 *
 */

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.WeakHashMap;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.plugins.peers.*;
import org.gudy.azureus2.plugins.utils.PooledByteBuffer;
import org.gudy.azureus2.plugins.disk.*;

import org.gudy.azureus2.pluginsimpl.local.peers.*;
import org.gudy.azureus2.pluginsimpl.local.utils.PooledByteBufferImpl;

public class 
DiskManagerImpl
	implements DiskManager
{
	protected PeerManagerImpl		peer_manager;
	
	public
	DiskManagerImpl(
		PeerManagerImpl		_peer_manager )
	{
		peer_manager	= _peer_manager;
	}
	
	public void 
	writeBlock(
		PeerReadRequest		request,
		PooledByteBuffer 	data,
		Peer 				sender)
	{
		peer_manager.getDelegate().writeBlock( request.getPieceNumber(), request.getOffset(), ((PooledByteBufferImpl)data).getBuffer(), peer_manager.mapForeignPeer( sender ));
	}
}
