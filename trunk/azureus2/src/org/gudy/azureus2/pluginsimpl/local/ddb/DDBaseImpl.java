/*
 * Created on 18-Feb-2005
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
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
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.pluginsimpl.local.ddb;

import java.util.*;


import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SHA1Hasher;
import org.gudy.azureus2.core3.util.ThreadPool;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ddb.*;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.torrent.Torrent;


import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.plugins.dht.DHTPlugin;
import com.aelitis.azureus.plugins.dht.DHTPluginContact;
import com.aelitis.azureus.plugins.dht.DHTPluginOperationListener;
import com.aelitis.azureus.plugins.dht.DHTPluginTransferHandler;

/**
 * @author parg
 *
 */

public class 
DDBaseImpl 
	implements DistributedDatabase
{
	private static DDBaseImpl	singleton;
	
	protected static AEMonitor		class_mon	= new AEMonitor( "DDBaseImpl:class");

	protected static Map			transfer_map = new HashMap();
	
	private static final DistributedDatabaseTransferType	torrent_transfer = new DDBaseTTTorrent();
	
	public static DistributedDatabase
	getSingleton(
		AzureusCore	azureus_core )
	{
		try{
			class_mon.enter();
	
			if ( singleton == null ){
				
				singleton = new DDBaseImpl( azureus_core );
			}
		}finally{
			
			class_mon.exit();
		}
		
		return( singleton );
	}
	
	
	private DHTPlugin		dht;
	
	private static final int	THREAD_POOL_SIZE	= 8;
	private AEMonitor			task_mon			= new AEMonitor( "DBase:TaskMon" );
	private int					tasks_active		= 0;
	private List				task_queue			= new ArrayList();
	private ThreadPool			thread_pool;
	
	protected
	DDBaseImpl(
		final AzureusCore	azureus_core )
	{
		PluginInterface dht_pi = 
			azureus_core.getPluginManager().getPluginInterfaceByClass(
						DHTPlugin.class );
		
		thread_pool = new ThreadPool("DDBase:queue",8);
		
		if ( dht_pi != null ){
			
			dht = (DHTPlugin)dht_pi.getPlugin();
			
			try{
				addTransferHandler(
						torrent_transfer,
						new DistributedDatabaseTransferHandler()
						{
							public DistributedDatabaseValue
							read(
								DistributedDatabaseContact			contact,
								DistributedDatabaseTransferType		type,
								DistributedDatabaseKey				key )
							
								throws DistributedDatabaseException
							{
								try{
									byte[]	hash = ((DDBaseKeyImpl)key).getBytes();
									
									Download dl = azureus_core.getPluginManager().getDefaultPluginInterface().getShortCuts().getDownload( hash );
									
									Torrent	torrent = dl.getTorrent();
									
									torrent = torrent.removeAdditionalProperties();
									
									return( createValue( torrent.writeToBEncodedData()));
									
								}catch( Throwable e ){
									
									throw( new DistributedDatabaseException("Torrent write fails", e ));
								}
							}
							
							public void
							write(
								DistributedDatabaseContact			contact,
								DistributedDatabaseTransferType		type,
								DistributedDatabaseKey				key,
								DistributedDatabaseValue			value )
							
								throws DistributedDatabaseException
							{
								throw( new DistributedDatabaseException( "not supported" ));
							}
						});
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
	public boolean
	isAvailable()
	{
		if ( dht == null ){
			
			return( false );
		}
		
		return( dht.isEnabled());
	}
	
	protected void
	throwIfNotAvailable()
	
		throws DistributedDatabaseException
	{
		if ( !isAvailable()){
			
			throw( new DistributedDatabaseException( "DHT not available" ));
		}
	}
	
	protected DHTPlugin
	getDHT()
	
		throws DistributedDatabaseException
	{
		throwIfNotAvailable();
		
		return( dht );
	}
	
	public DistributedDatabaseKey
	createKey(
		Object			key )
	
		throws DistributedDatabaseException
	{
		throwIfNotAvailable();
		
		return( new DDBaseKeyImpl( key ));
	}
	
	public DistributedDatabaseValue
	createValue(
		Object			value )
	
		throws DistributedDatabaseException
	{
		throwIfNotAvailable();
		
		return( new DDBaseValueImpl( new DDBaseContactImpl( this, dht.getLocalAddress()), value ));
	}
	
	public void
	write(
		DistributedDatabaseListener		listener,
		DistributedDatabaseKey			key,
		DistributedDatabaseValue		value )
	
		throws DistributedDatabaseException
	{
		write( listener, key, new DistributedDatabaseValue[]{ value } );
	}
	
	public void
	write(
		final DistributedDatabaseListener	listener,
		final DistributedDatabaseKey		key,
		final DistributedDatabaseValue		values[] )
	
		throws DistributedDatabaseException
	{
		throwIfNotAvailable();
		
		for (int i=0;i<values.length;i++){
			
			if (((DDBaseValueImpl)values[i]).getBytes().length > DDBaseValueImpl.MAX_VALUE_SIZE ){
				
				throw( new DistributedDatabaseException("Value size limited to " + DDBaseValueImpl.MAX_VALUE_SIZE + " bytes" ));		
			}
		}
		
		if ( values.length == 0 ){
			
			delete( listener, key );
			
		}else if ( values.length == 1 ){
			
			queue(
				new AERunnable()
				{
					public void
					runSupport()
					{
						dht.put(	
								((DDBaseKeyImpl)key).getBytes(),
								((DDBaseValueImpl)values[0]).getBytes(),
								DHTPlugin.FLAG_SINGLE_VALUE,
								new listenerMapper( listener, DistributedDatabaseEvent.ET_VALUE_WRITTEN, key, 0 ));
					}
				});
		}else{
			
				
			// TODO: optimise re-publishing to avoid republishing everything each time
			/*
			DHTPluginValue	old_value = dht.getLocalValue( ((DDBaseKeyImpl)key).getBytes());
			
			List	old_values = new ArrayList();
			
			if ( old_value != null ){
				
				if (( old_value.getFlags() & DHTPlugin.FLAG_MULTI_VALUE ) == 0 ){
			
					old_values.add( old_value.getValue());
					
				}else{
					
					byte[]	encoded = old_value.getValue();
					
					
				}
			}
			*/
		
			byte[]	current_key = ((DDBaseKeyImpl)key).getBytes();
			
				// format is: <continuation> <len><len><data>
			
			byte[]	payload			= new byte[DHTPlugin.MAX_VALUE_SIZE];
			int		payload_length	= 1;
					
			int	pos = 0;
			
			while( pos < values.length ){
				
				DDBaseValueImpl	value = (DDBaseValueImpl)values[pos];
				
				byte[]	bytes = value.getBytes();
			
				int		len = bytes.length;
				
				if ( payload_length + len < payload.length - 2 ){
					
					payload[payload_length++] = (byte)(( len & 0x0000ff00 ) >> 8);
					payload[payload_length++] = (byte) ( len & 0x000000ff );
					
					System.arraycopy( bytes, 0, payload, payload_length, len );
					
					payload_length	+= len;
					
					pos++;
					
				}else{
					
					payload[0]	= 1;
					
					final byte[]	copy = new byte[payload_length];
					
					System.arraycopy( payload, 0, copy, 0, copy.length );
					
					final byte[]					f_current_key	= current_key;
					
					queue(
							new AERunnable()
							{
								public void
								runSupport()
								{
									dht.put(	
											f_current_key,
											copy,
											DHTPlugin.FLAG_MULTI_VALUE,
											new listenerMapper( listener, DistributedDatabaseEvent.ET_VALUE_WRITTEN, key, 0 ));
								}
							});	
					
					payload_length	= 1;
					
					current_key = new SHA1Hasher().calculateHash( current_key );
				}
			}
			
			if ( payload_length > 1 ){
				
				payload[0]	= 0;
				
				final byte[]	copy = new byte[payload_length];
				
				System.arraycopy( payload, 0, copy, 0, copy.length );
				
				final byte[]					f_current_key	= current_key;
				
				queue(
						new AERunnable()
						{
							public void
							runSupport()
							{
								dht.put(	
										f_current_key,
										copy,
										DHTPlugin.FLAG_MULTI_VALUE,
										new listenerMapper( listener, DistributedDatabaseEvent.ET_VALUE_WRITTEN, key, 0 ));
							}
						});					
			}
		}
	}
		
	public void
	read(
		final DistributedDatabaseListener		listener,
		final DistributedDatabaseKey			key,
		final long								timeout )
	
		throws DistributedDatabaseException
	{
			// TODO: max values?
		
		queue(
			new AERunnable()
			{
				public void
				runSupport()
				{
					dht.get(	
						((DDBaseKeyImpl)key).getBytes(), 
						(byte)0, 
						256, 
						timeout, 
						new listenerMapper( listener, DistributedDatabaseEvent.ET_VALUE_READ, key, timeout ));
				}
			});
	}
	
	public void
	delete(
		final DistributedDatabaseListener		listener,
		final DistributedDatabaseKey			key )
	
		throws DistributedDatabaseException
	{
		queue(
			new AERunnable()
			{
				public void
				runSupport()
				{
					dht.remove( ((DDBaseKeyImpl)key).getBytes(), new listenerMapper( listener, DistributedDatabaseEvent.ET_VALUE_DELETED, key, 0 ));
				}
			});
	}
	
	protected void
	queue(
		final AERunnable	task )
	{
		boolean	run_it;
		
		try{
			task_mon.enter();
			
			if ( tasks_active >= THREAD_POOL_SIZE ){
				
				task_queue.add( task );
				
				run_it	= false;
				
			}else{
				
				tasks_active++;
				
				run_it	= true;
			}
		}finally{
			
			task_mon.exit();
		}
		
			// note thread pool will block if > thread_pool_size added, however should never block
			// here due to above logic regarding active tasks
				
		if ( run_it ){
			
			thread_pool.run( 
				new AERunnable()
				{
					public void
					runSupport()
					{
						AERunnable	current_task	= task;

						while( current_task != null ){
							
							try{
								current_task.runSupport();
								
							}catch( Throwable e ){
								
								Debug.printStackTrace(e);
								
							}finally{
								
								try{
									task_mon.enter();
								
									if ( task_queue.size() > 0 ){
										
										current_task = (AERunnable)task_queue.remove(0);
										
									}else{
										
										current_task	= null;
										
										tasks_active--;
									}
								}finally{
									
									task_mon.exit();
								}
							}
						}
					}
				});
		}
	}
	
	public void
	addTransferHandler(
		final DistributedDatabaseTransferType		type,
		final DistributedDatabaseTransferHandler	handler )
	
		throws DistributedDatabaseException
	{
		final HashWrapper	type_key = DDBaseHelpers.getKey( type.getClass());
		
		if ( transfer_map.get( type_key ) != null ){
			
			throw( new DistributedDatabaseException( "Handler for class '" + type.getClass().getName() + "' already defined" ));
		}
		
		transfer_map.put( type_key, handler );
		
		dht.registerHandler(
			type_key.getHash(),
			new DHTPluginTransferHandler()
			{
				public byte[]
				handleRead(
					DHTPluginContact	originator,
					byte[]				xfer_key )
				{
					try{
						DDBaseValueImpl	res = (DDBaseValueImpl)
							handler.read(
									new DDBaseContactImpl( DDBaseImpl.this, originator ),
									type,
									new DDBaseKeyImpl( xfer_key ));
						
						return( res.getBytes());
						
					}catch( Throwable e ){
						
						Debug.printStackTrace(e);
						
						return( null );
					}
				}
				
				public void
				handleWrite(
					DHTPluginContact	originator,
					byte[]				xfer_key,
					byte[]				value )
				{
					try{
						DDBaseContactImpl	contact = new DDBaseContactImpl( DDBaseImpl.this, originator );
						
						handler.write( 
							contact,
							type,
							new DDBaseKeyImpl( xfer_key ),
							new DDBaseValueImpl( contact, value ));
						
					}catch( Throwable e ){
						
						Debug.printStackTrace(e);
					}
				}
			});
	}
	
	public DistributedDatabaseTransferType
	getStandardTransferType(
		int		standard_type )
	
		throws DistributedDatabaseException
	{
		if ( standard_type == DistributedDatabaseTransferType.ST_TORRENT ){
			
			return( torrent_transfer );
		}
		
		throw( new DistributedDatabaseException( "unknown type" ));
	}
	
	protected class
	listenerMapper
		implements DHTPluginOperationListener
	{
		private DistributedDatabaseListener	listener;
		private int							type;
		private DistributedDatabaseKey		key;
		private byte[]						key_bytes;
		private long						timeout;
		private boolean						complete_disabled;
		
		protected
		listenerMapper(
			DistributedDatabaseListener	_listener,
			int							_type,
			DistributedDatabaseKey		_key,
			long						_timeout )
		{
			listener	= _listener;
			type		= _type;
			key			= _key;
			key_bytes	= ((DDBaseKeyImpl)key).getBytes();
			timeout		= _timeout;
		}
		
		private
		listenerMapper(
			DistributedDatabaseListener	_listener,
			int							_type,
			DistributedDatabaseKey		_key,
			byte[]						_key_bytes,
			long						_timeout )
		{
			listener	= _listener;
			type		= _type;
			key			= _key;
			key_bytes	= _key_bytes;
			timeout		= _timeout;
		}
		
		public void
		valueRead(
			DHTPluginContact	originator,
			byte[]				value,
			byte				flags )
		{
			if ( flags == DHTPlugin.FLAG_MULTI_VALUE ){

				int	pos = 1;
				
				while( pos < value.length ){
					
					int	len = (	( value[pos++]<<8 ) & 0x0000ff00 )+
							 	( value[pos++] & 0x000000ff );
					
					if ( len > value.length - pos ){
						
						Debug.out( "Invalid length: len = " + len + ", remaining = " + (value.length - pos ));
						
						break;
					}
					
					byte[]	d = new byte[len];
					
					System.arraycopy( value, pos, d, 0, len );
					
					listener.event( new dbEvent( type, key, originator, d ));
					
					pos += len;
				}				

				if ( value[0] == 1 ){
					
						// continuation exists
					
					final	byte[]	next_key_bytes = new SHA1Hasher().calculateHash( key_bytes );
					
					complete_disabled	= true;
					
					queue(
							new AERunnable()
							{
								public void
								runSupport()
								{
									dht.get(	
										next_key_bytes, 
										(byte)0, 
										16, 
										timeout, 
										new listenerMapper( listener, DistributedDatabaseEvent.ET_VALUE_READ, key, next_key_bytes, timeout ));
								}
							});
				}
			}else{
				
				listener.event( new dbEvent( type, key, originator, value ));
			}
		}
		
		public void
		valueWritten(
			DHTPluginContact	target,
			byte[]				value )
		{
			listener.event( new dbEvent( type, key, target, value ));
		}

		public void
		complete(
			boolean	timeout_occurred )
		{
			if ( !complete_disabled ){
				
				listener.event( 
					new dbEvent( 
						timeout_occurred?DistributedDatabaseEvent.ET_OPERATION_TIMEOUT:DistributedDatabaseEvent.ET_OPERATION_COMPLETE,
						key ));
			}
		}
	}
	
	protected class
	dbEvent
		implements DistributedDatabaseEvent
	{
		private int							type;
		private DistributedDatabaseKey		key;
		private DistributedDatabaseValue	value;
		private DDBaseContactImpl			contact;
		
		protected 
		dbEvent(
			int						_type,
			DistributedDatabaseKey	_key )
		{
			type	= _type;
			key		= _key;
		}
		
		protected
		dbEvent(
			int						_type,
			DistributedDatabaseKey	_key,
			DHTPluginContact		_contact,
			byte[]					_value )
		{
			type		= _type;
			key			= _key;
			
			contact	= new DDBaseContactImpl( DDBaseImpl.this, _contact );
			
			value	= new DDBaseValueImpl( contact, _value ); 
		}
		
		public int
		getType()
		{
			return( type );
		}
		
		public DistributedDatabaseKey
		getKey()
		{
			return( key );
		}
		
		public DistributedDatabaseValue
		getValue()
		{
			return( value );
		}
		
		public DistributedDatabaseContact
		getContact()
		{
			return( contact );
		}
	}
}