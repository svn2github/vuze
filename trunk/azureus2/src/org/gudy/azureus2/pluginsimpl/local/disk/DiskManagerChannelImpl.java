/*
 * Created on 29-Mar-2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.pluginsimpl.local.disk;

import java.io.*;
import java.util.*;

import org.gudy.azureus2.core3.disk.DiskManagerFileInfoListener;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.plugins.disk.DiskManagerChannel;
import org.gudy.azureus2.plugins.disk.DiskManagerEvent;
import org.gudy.azureus2.plugins.disk.DiskManagerListener;
import org.gudy.azureus2.plugins.disk.DiskManagerRequest;
import org.gudy.azureus2.plugins.utils.PooledByteBuffer;
import org.gudy.azureus2.pluginsimpl.local.utils.PooledByteBufferImpl;

public class 
DiskManagerChannelImpl 
	implements DiskManagerChannel, DiskManagerFileInfoListener
{
	private DiskManagerFileInfoImpl		file;
	
	protected
	DiskManagerChannelImpl(
		DiskManagerFileInfoImpl		_file )
	{
		file		= _file;
		
		file.getCore().addListener( this );
	}
	
	public DiskManagerRequest
	createRequest()
	{
		return( new request());
	}
	public void
	dataWritten(
		long	offset,
		long	length )
	{
		System.out.println( "data written:" + offset + "/" + length );
	}
	
	public void
	dataChecked(
		long	offset,
		long	length )
	{
		System.out.println( "data checked:" + offset + "/" + length );
	}
	
	public void
	destroy()
	{
		file.getCore().removeListener( this );
	}
	
	protected class
	request 
		implements DiskManagerRequest
	{
		private int		request_type;
		private long	request_offset;
		private long	request_length;
		private List	listeners	= new ArrayList();
		
		private boolean	cancelled;
		
		public void
		setType(
			int			_type )
		{
			request_type		= _type;
		}
		
		public void
		setOffset(
			long		_offset )
		{
			request_offset	= _offset;
		}
		
		public void
		setLength(
			long		_length )
		{
			request_length	= _length;
		}
		
		public void
		queue()
		{
			int	max_chunk = 65536;
			
			long	rem = request_length;
			
			long	pos = request_offset;
			
			try{

				while( rem > 0 && !cancelled ){
					
					int	len = (int)( rem<max_chunk?rem:max_chunk);
					
					DirectByteBuffer buffer = file.getCore().read( pos, len );

					inform( new event( new PooledByteBufferImpl( buffer ), pos, len ));
					
					pos += len;
					rem -= len;
					
					Thread.sleep(60*1000);
				}
			}catch( Throwable e ){
				
				inform( e );
			}
		}
		
		public void
		run()
		{
			queue();
		}
		
		public void
		cancel()
		{
			cancelled	= true;
		}
		
		protected void
		inform(
			Throwable e )
		{
			inform( new event( e ));
		}
		
		protected void
		inform(
			event		ev )
		{
			for (int i=0;i<listeners.size();i++){
				
				try{
					((DiskManagerListener)listeners.get(i)).eventOccurred( ev );
					
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
		}
		
		public void
		addListener(
			DiskManagerListener	listener )
		{
			listeners.add( listener );
		}
	
		public void
		removeListener(
			DiskManagerListener	listener )
		{
			listeners.remove( listener );
		}
		
		protected class
		event
			implements DiskManagerEvent
		{
			private int					event_type;
			private Throwable			error;
			private PooledByteBuffer	buffer;
			private long				event_offset;
			private int					event_length;
			
			protected
			event(
				Throwable		_error )
			{
				event_type	= DiskManagerEvent.EVENT_TYPE_FAILED;
				error		= _error;
			}
			
			protected
			event(
				PooledByteBuffer	_buffer,
				long				_offset,
				int					_length )
			{
				event_type		= DiskManagerEvent.EVENT_TYPE_SUCCESS;
				buffer			= _buffer;
				event_offset	= _offset;
				event_length	= _length;
			}
			
			public int
			getType()
			{
				return( event_type );
			}
			
			public DiskManagerRequest
			getRequest()
			{
				return( request.this );
			}
			
			public long
			getOffset()
			{
				return( event_offset );
			}
			
			public int
			getLength()
			{
				return( event_length );
			}
			
			public PooledByteBuffer
			getBuffer()
			{
				return( buffer );
			}
			
			public Throwable
			getFailure()
			{
				return( error );
			}
		}
	}
}
