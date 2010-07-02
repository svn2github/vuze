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


import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.plugins.disk.*;
import org.gudy.azureus2.pluginsimpl.local.utils.PooledByteBufferImpl;

public class 
DiskManagerImpl
	implements DiskManager
{
	private org.gudy.azureus2.core3.disk.DiskManager		disk_manager;
	
	public
	DiskManagerImpl(
		org.gudy.azureus2.core3.disk.DiskManager		_disk_manager )
	{
		disk_manager	= _disk_manager;
	}
	
	public org.gudy.azureus2.core3.disk.DiskManager
	getDiskmanager()
	{
		return( disk_manager );	
	}
	
	public DiskManagerReadRequest 
	read(
		int 									piece_number, 
		int 									offset, 
		int 									length,
		final DiskManagerReadRequestListener	listener )
	
		throws DiskManagerException 
	{
		if ( !disk_manager.checkBlockConsistencyForRead( "plugin", false, piece_number, offset, length )){
			
			throw( new DiskManagerException( "read invalid - parameters incorrect or piece incomplete" ));
		}
		
		final DMRR request = new DMRR( disk_manager.createReadRequest( piece_number, offset, length ));
		
		disk_manager.enqueueReadRequest( 
			request.getDelegate(),
			new org.gudy.azureus2.core3.disk.DiskManagerReadRequestListener()
			{
				public void 
				readCompleted( 
					org.gudy.azureus2.core3.disk.DiskManagerReadRequest 	_request, 
					DirectByteBuffer 										_data )
				{
					listener.complete( request, new PooledByteBufferImpl( _data ));
				}

				public void 
				readFailed( 
					org.gudy.azureus2.core3.disk.DiskManagerReadRequest 	_request, 
					Throwable		 										_cause )
				{
					listener.failed( request, new DiskManagerException( "read failed", _cause ));
				}

				public int
				getPriority()
				{
					return( 0 );
				}
				
				public void 
				requestExecuted(
					long 	bytes )
				{					
				}
			});
		
		return( request );
	}
	
	private class
	DMRR
		implements org.gudy.azureus2.plugins.disk.DiskManagerReadRequest
	{
		private org.gudy.azureus2.core3.disk.DiskManagerReadRequest		request;
		
		private
		DMRR(
			org.gudy.azureus2.core3.disk.DiskManagerReadRequest	_request )
		{
			request = _request;
		}
		
		private org.gudy.azureus2.core3.disk.DiskManagerReadRequest
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
	}
}
