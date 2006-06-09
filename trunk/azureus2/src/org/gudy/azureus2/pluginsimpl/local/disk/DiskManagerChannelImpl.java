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

import java.util.*;

import org.gudy.azureus2.core3.disk.DiskManagerFileInfoListener;
import org.gudy.azureus2.core3.download.DownloadManagerPeerListener;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.Average;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.plugins.disk.DiskManagerChannel;
import org.gudy.azureus2.plugins.disk.DiskManagerEvent;
import org.gudy.azureus2.plugins.disk.DiskManagerListener;
import org.gudy.azureus2.plugins.disk.DiskManagerRequest;
import org.gudy.azureus2.plugins.utils.PooledByteBuffer;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadImpl;
import org.gudy.azureus2.pluginsimpl.local.utils.PooledByteBufferImpl;

import com.aelitis.azureus.core.peermanager.piecepicker.PiecePicker;
import com.aelitis.azureus.core.peermanager.piecepicker.PiecePiecerPriorityShaper;

public class 
DiskManagerChannelImpl 
	implements DiskManagerChannel, DiskManagerFileInfoListener, DownloadManagerPeerListener, PiecePiecerPriorityShaper
{
	private static final boolean	TRACE = false;
	
	private static final int COMPACT_DELAY	= 32;
	
	private static final Comparator comparator = new
		Comparator()
		{
			public int 
		   	compare(
		   		Object _o1, 
				Object _o2)
			{
				dataEntry	o1 = (dataEntry)_o1;
				dataEntry	o2 = (dataEntry)_o2;
				
				long	offset1 = o1.getOffset();
				long	length1	= o1.getLength();
				
				long	offset2 = o2.getOffset();
				long	length2	= o2.getLength();
			
		   	
				long	res;
				
				if ( offset1 == offset2 ){
					
					res = length1 - length2;
					
				}else{
					
					res = offset1 - offset2;
				}
				
				if ( res == 0 ){
					return(0);
				}else if ( res < 0 ){
					return(-1);
				}else{
					return(1);
				}
			}
		};
		
	private static final String	channel_key = "DiskManagerChannel";
	private static int	channel_id_next;
	
	private DownloadImpl	download;
	
	private org.gudy.azureus2.pluginsimpl.local.disk.DiskManagerFileInfoImpl	plugin_file;
	private org.gudy.azureus2.core3.disk.DiskManagerFileInfo					core_file;
	
	private Set	data_written = new TreeSet( comparator );
	
	private int compact_delay	= COMPACT_DELAY;
	
	private List	waiters	= new ArrayList();

	private long	file_offset_in_torrent;
	private long	piece_size;
	
	private static final int	PIECES_TO_BUFFER_MIN	= 10;
	
	private Average	byte_rate = Average.getInstance( 1000, 20 );
	
	private long	current_position;
	private int		pieces_to_buffer	= PIECES_TO_BUFFER_MIN;
	
	private PEPeerManager	peer_manager;
	private int[]	priority_offsets;
	
	private int		channel_id;
	
	protected
	DiskManagerChannelImpl(
		DownloadImpl															_download,
		org.gudy.azureus2.pluginsimpl.local.disk.DiskManagerFileInfoImpl		_plugin_file )
	{
		download		= _download;
		plugin_file		= _plugin_file;
		
		core_file		= plugin_file.getCore();
		
		synchronized( DiskManagerChannelImpl.class ){
			
			channel_id = channel_id_next++;
		}
				
		TOTorrentFile	tf = core_file.getTorrentFile();
		
		TOTorrent 	torrent = tf.getTorrent();
		
		TOTorrentFile[]	tfs = torrent.getFiles();

		priority_offsets	= new int[torrent.getNumberOfPieces()];
		
		core_file.getDownloadManager().addPeerListener(this);
			
		for (int i=0;i<core_file.getIndex();i++){
				
			file_offset_in_torrent += tfs[i].getLength();
		}
			
		piece_size	= tf.getTorrent().getPieceLength();
		
		core_file.addListener( this );
	}
	
	public DiskManagerRequest
	createRequest()
	{
		if ( core_file.getDownloaded() != core_file.getLength()){
			
			if ( core_file.isSkipped()){
				
				core_file.setSkipped( false );
			}
			
			boolean	force_start = download.isForceStart();
			
			if ( !force_start ){
				
				synchronized( download ){
					
					Map	dl_state = (Map)download.getDownload().getData( channel_key );
					
					if ( dl_state == null ){
						
						dl_state = new HashMap();
						
						download.getDownload().setData( channel_key, dl_state );
					}
					
					dl_state.put( ""+channel_id, "" );
				}
				
				download.setForceStart( true );
			}
		}
		
		return( new request());
	}
	
	public void
	dataWritten(
		long	offset,
		long	length )
	{
		if ( TRACE ){
			System.out.println( "data written:" + offset + "/" + length );
		}
		
		dataEntry	entry = new dataEntry( offset, length );
		
		synchronized( data_written ){
			
			data_written.add( entry );
			
			compact_delay--;
			
			if ( compact_delay == 0 ){
				
				compact_delay	= COMPACT_DELAY;
				
				Iterator	it = data_written.iterator();
				
				dataEntry	prev_e	= null;
				
				while( it.hasNext()){
					
					dataEntry	this_e = (dataEntry)it.next();
					
					if ( prev_e == null ){
						
						prev_e = this_e;
						
					}else{
						
						long	prev_offset = prev_e.getOffset();
						long	prev_length	= prev_e.getLength();
						long	this_offset = this_e.getOffset();
						long	this_length	= this_e.getLength();
						
						if ( this_offset <= prev_offset + prev_length ){
							
							if ( TRACE ){	
								System.out.println( "merging: " + prev_e.getString()  + "/" + this_e.getString());
							}
							
							it.remove();
							
							prev_e.setLength( Math.max( prev_offset + prev_length, this_offset + this_length ) - prev_offset );
						
							if ( TRACE ){	
								System.out.println( "    -> " + prev_e.getString());
							}

						}else{
							
							prev_e = this_e;
						}
					}
				}
			}
		
			for (int i=0;i<waiters.size();i++){
					
				((AESemaphore)waiters.get(i)).release();
			}
		}
	}
	
	public void
	dataChecked(
		long	offset,
		long	length )
	{
		// System.out.println( "data checked:" + offset + "/" + length );
	}
	
	public void
	peerManagerAdded(
		PEPeerManager	manager )
	{
		peer_manager = manager;
		
		manager.getPiecePicker().addPriorityShaper( this );
	}
	
	public void
	peerManagerRemoved(
		PEPeerManager	manager )
	{
		peer_manager = null;
		
		manager.getPiecePicker().removePriorityShaper( this );
	}
	
	public void
	peerAdded(
		PEPeer 	peer )
	{
	}
		
	public void
	peerRemoved(
		PEPeer	peer )
	{
	}
		
	public void
	pieceAdded(
		PEPiece 	piece )
	{
	}
		
	public void
	pieceRemoved(
		PEPiece		piece )
	{
	}

	       	
   	public int[]
   	updatePriorityOffsets(
   		PiecePicker		picker )
   	{
   		long	overall_pos = current_position + file_offset_in_torrent;
   		
   		int	first_piece = (int)( overall_pos / piece_size );
   		
   		Arrays.fill( priority_offsets, 0 );
   		   		
   		for (int i=first_piece;i<first_piece+pieces_to_buffer&&i<priority_offsets.length;i++){
   			
   			priority_offsets[i]	= 1000000 - ((i-first_piece)*10000);
   		}
   		
   		return( priority_offsets );
   	}
   	
	public void
	destroy()
	{
		core_file.removeListener( this );
		
		core_file.getDownloadManager().removePeerListener(this);
		
		if ( peer_manager != null ){
			
			peer_manager.getPiecePicker().removePriorityShaper( this );
		}
		
		boolean	stop_force_start = false;
		
		synchronized( download ){
			
			Map	dl_state = (Map)download.getDownload().getData( channel_key );
			
			if ( dl_state != null ){
				
				dl_state.remove( "" + channel_id );
				
				if ( dl_state.size() == 0 ){
					
					stop_force_start	= true;
				}
			}
		}
		
		if ( stop_force_start ){
			
			download.setForceStart( false );
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
		
		private volatile boolean	cancelled;
		
		AESemaphore	wait_sem = new AESemaphore( "DiskManagerChannelImpl:wait" );
		
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
		run()
		{
			int	max_chunk = 65536;
			
			long	rem = request_length;
			
			long	pos = request_offset;
			
			try{

				while( rem > 0 && !cancelled ){
					
					int	len = 0;
					
					synchronized( data_written ){
						
						current_position = pos;
						
						Iterator	it = data_written.iterator();
						
						while( it.hasNext()){
							
							dataEntry	entry = (dataEntry)it.next();
							
							long	entry_offset = entry.getOffset();
							
							if ( entry_offset > pos ){
																
								break;
							}
							
							long	entry_length = entry.getLength();
							
							long	available = entry_offset + entry_length - pos;
							
							if ( available > 0 ){
								
								len = (int)( available<max_chunk?available:max_chunk);
								
								break;
							}
						}
					}				
					
					if ( len > 0 ){
						
						DirectByteBuffer buffer = core_file.read( pos, len );
	
						inform( new event( new PooledByteBufferImpl( buffer ), pos, len ));
						
						pos += len;
						
						rem -= len;
						
						synchronized( data_written ){
							
							byte_rate.addValue( len );
							
							current_position = pos;
						}
					}else{
						
						inform( new event( pos ));
						
						synchronized( data_written ){
							
							waiters.add( wait_sem );
						}
						
						try{
							wait_sem.reserve();
							
						}finally{
							
							synchronized( data_written ){
								
								waiters.remove( wait_sem );
							}
						}
					}
				}
			}catch( Throwable e ){
				
				inform( e );
			}
		}
		
		public void
		cancel()
		{
			cancelled	= true;
						
			inform( new Throwable( "Request cancelled" ));

			wait_sem.release();
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
				long				_offset )
			{
				event_type		= DiskManagerEvent.EVENT_TYPE_BLOCKED;

				event_offset	= _offset;	
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
	
	protected static class
	dataEntry
	{
		private long	offset;
		private long	length;
	
		protected
		dataEntry(
			long		_offset,
			long		_length )
		{
			offset	= _offset;
			length	= _length;
		}
		
		protected long
		getOffset()
		{
			return( offset );
		}
		
		protected long
		getLength()
		{
			return( length );
		}
		
		protected void
		setLength(
			long	_length )
		{
			length	= _length;
		}
		
		protected String
		getString()
		{
			return( "offset=" + offset + ",length=" + length );
		}
	}
}
