/*
 * File    : DiskManagerRequestImpl.java
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

import org.gudy.azureus2.plugins.disk.*;
import org.gudy.azureus2.core3.peer.*;

public class 
DiskManagerRequestImpl
	implements DiskManagerRequest
{
	protected PEPeerManager		manager;

	protected org.gudy.azureus2.core3.disk.DiskManagerReadRequest	request;
	
	protected
	DiskManagerRequestImpl(
		PEPeerManager	_manager,
		int 			_pieceNumber,
		int 			_offset,
		int 			_length )
	{
		manager		= _manager;
		
		request		= manager.createDiskManagerRequest( _pieceNumber, _offset, _length );
	}
	
	public org.gudy.azureus2.core3.disk.DiskManagerReadRequest
	getDelegate()
	{
		return( request );
	}
	
	public int
	getPieceNumber()
	{
		return( request.getPieceNumber());
	}
	
	public int
	getOffset()
	{
		return( request.getOffset());
	}
	
	public int
	getLength()
	{
		return( request.getLength());
	}
	
	public void
	resetTime()
	{
		request.reSetTime();
	}
	
	public boolean
	isExpired()
	{
		return( request.isExpired());
	}
	
	public void
	cancel()
	{
		manager.requestCanceled( request );
	}
	
	public boolean
	equals(
		Object	other )
	{
		if ( other instanceof DiskManagerRequestImpl ){
			
			return( request.equals(((DiskManagerRequestImpl)other).request));
		}
		
		return( false );
	}
}
