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

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.disk.DiskManagerChannel;
import org.gudy.azureus2.plugins.disk.DiskManagerEvent;
import org.gudy.azureus2.plugins.disk.DiskManagerListener;
import org.gudy.azureus2.plugins.disk.DiskManagerRequest;

public class 
DiskManagerChannelImpl 
	implements DiskManagerChannel
{
	private DiskManagerFileInfoImpl		file;
	private RandomAccessFile			raf;
	
	protected
	DiskManagerChannelImpl(
		DiskManagerFileInfoImpl		_file )
	{
		file		= _file;
	}
	
	public DiskManagerRequest
	createRequest()
	{
		return( new request());
	}
	
	public void
	destroy()
	{
	
		if ( raf != null ){
			
			try{
				raf.close();
				
				raf = null;
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
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
			if ( raf == null ){
				
				try{
					
					raf = new RandomAccessFile( file.getFile(), "r" );
					
				}catch( Throwable e ){
										
					inform( e );
				}
			}
			
			byte[]	buffer = new byte[65536];
			
			long	rem = request_length;
			
			long	pos = request_offset;
			
			try{

				raf.seek( request_offset );

				while( rem > 0 && !cancelled ){
					
					int	len = raf.read( buffer, 0, (int)( rem<buffer.length?rem:buffer.length ));

					if ( len <= 0 ){
						
						inform( new IOException( "file too short" ));
					}
				
					inform( new event( buffer, pos, len ));
					
					pos += len;
					rem -= len;
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
			private int			event_type;
			private Throwable	error;
			private byte[]		bytes;
			private long		event_offset;
			private int			event_length;
			
			protected
			event(
				Throwable		_error )
			{
				event_type	= DiskManagerEvent.EVENT_TYPE_FAILED;
				error		= _error;
			}
			
			protected
			event(
				byte[]		_bytes,
				long		_offset,
				int			_length )
			{
				event_type		= DiskManagerEvent.EVENT_TYPE_SUCCESS;
				bytes			= _bytes;
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
			
			public byte[]
			getBytes()
			{
				return( bytes );
			}
			
			public Throwable
			getFailure()
			{
				return( error );
			}
		}
	}
}
