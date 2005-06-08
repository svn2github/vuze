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
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SHA1Simple;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ddb.*;


import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.plugins.dht.DHTPlugin;
import com.aelitis.azureus.plugins.dht.DHTPluginContact;
import com.aelitis.azureus.plugins.dht.DHTPluginOperationListener;
import com.aelitis.azureus.plugins.dht.DHTPluginProgressListener;
import com.aelitis.azureus.plugins.dht.DHTPluginTransferHandler;
import com.aelitis.azureus.plugins.dht.DHTPluginValue;

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
	
	private static DDBaseTTTorrent	torrent_transfer;
	
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
		
	protected
	DDBaseImpl(
		final AzureusCore	azureus_core )
	{
		torrent_transfer =  new DDBaseTTTorrent( azureus_core, this );
		
		PluginInterface dht_pi = 
			azureus_core.getPluginManager().getPluginInterfaceByClass(
						DHTPlugin.class );
				
		if ( dht_pi == null ){
			
			Debug.out( "DHTPlugin unavailable - if this is unexpected consider revising the plugin initialisation sequence" );
			
		}else{
			
			dht = (DHTPlugin)dht_pi.getPlugin();
			
			if ( dht.isEnabled()){
				
				try{
					addTransferHandler(	torrent_transfer, torrent_transfer );

				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
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
	
	public boolean
	isExtendedUseAllowed()
	{
		if ( dht == null ){
			
			return( false );
		}
		
		return( dht.isExtendedUseAllowed());	
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
	
	protected void
	log(
		String	str )
	{
		if ( dht != null ){
			
			dht.log( str );
		}
	}
	
	public DistributedDatabaseKey
	createKey(
		Object			key )
	
		throws DistributedDatabaseException
	{
		throwIfNotAvailable();
		
		return( new DDBaseKeyImpl( key ));
	}
	
	public DistributedDatabaseKey
	createKey(
		Object			key,
		String			description )
	
		throws DistributedDatabaseException
	{
		throwIfNotAvailable();
		
		return( new DDBaseKeyImpl( key, description ));
	}
	
	public DistributedDatabaseValue
	createValue(
		Object			value )
	
		throws DistributedDatabaseException
	{
		throwIfNotAvailable();
		
		return( new DDBaseValueImpl( new DDBaseContactImpl( this, dht.getLocalAddress()), value, SystemTime.getCurrentTime()));
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
			
			dht.put(	
					((DDBaseKeyImpl)key).getBytes(),
					key.getDescription(),
					((DDBaseValueImpl)values[0]).getBytes(),
					DHTPlugin.FLAG_SINGLE_VALUE,
					new listenerMapper( listener, DistributedDatabaseEvent.ET_VALUE_WRITTEN, key, 0, false ));
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
					
					dht.put(	
							f_current_key,
							key.getDescription(),
							copy,
							DHTPlugin.FLAG_MULTI_VALUE,
							new listenerMapper( listener, DistributedDatabaseEvent.ET_VALUE_WRITTEN, key, 0, false ));
					
					payload_length	= 1;
					
					current_key = new SHA1Simple().calculateHash( current_key );
				}
			}
			
			if ( payload_length > 1 ){
				
				payload[0]	= 0;
				
				final byte[]	copy = new byte[payload_length];
				
				System.arraycopy( payload, 0, copy, 0, copy.length );
				
				final byte[]					f_current_key	= current_key;
				
				dht.put(	
						f_current_key,
						key.getDescription(),
						copy,
						DHTPlugin.FLAG_MULTI_VALUE,
						new listenerMapper( listener, DistributedDatabaseEvent.ET_VALUE_WRITTEN, key, 0, false ));
			}
		}
	}
		
	public void
	read(
		DistributedDatabaseListener		listener,
		DistributedDatabaseKey			key,
		long							timeout )
	
		throws DistributedDatabaseException
	{
		read( listener, key, timeout, OP_NONE );
	}
	
	public void
	read(
		final DistributedDatabaseListener		listener,
		final DistributedDatabaseKey			key,
		final long								timeout,
		int										options )
	
		throws DistributedDatabaseException
	{
		throwIfNotAvailable();
		
		boolean	exhaustive  = (options&OP_EXHAUSTIVE_READ)==1;
		
			// TODO: max values?
		
		dht.get(	
			((DDBaseKeyImpl)key).getBytes(), 
			key.getDescription(),
			(byte)0, 
			256, 
			timeout, 
			exhaustive,
			new listenerMapper( listener, DistributedDatabaseEvent.ET_VALUE_READ, key, timeout, exhaustive ));
	}
	
	public void
	delete(
		final DistributedDatabaseListener		listener,
		final DistributedDatabaseKey			key )
	
		throws DistributedDatabaseException
	{
		throwIfNotAvailable();
		
		dht.remove( ((DDBaseKeyImpl)key).getBytes(),
					key.getDescription(),
					new listenerMapper( listener, DistributedDatabaseEvent.ET_VALUE_DELETED, key, 0, false ));
	}
	
	public void
	addTransferHandler(
		final DistributedDatabaseTransferType		type,
		final DistributedDatabaseTransferHandler	handler )
	
		throws DistributedDatabaseException
	{
		throwIfNotAvailable();
		
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
						
						if ( res == null ){
							
							return( null );
						}
						
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
							new DDBaseValueImpl( contact, value, SystemTime.getCurrentTime()));
						
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
	
	protected DistributedDatabaseValue
	read(
		DDBaseContactImpl							contact,
		final DistributedDatabaseProgressListener	listener,
		DistributedDatabaseTransferType				type,
		DistributedDatabaseKey						key,
		long										timeout )
	
		throws DistributedDatabaseException
	{
		if ( type == torrent_transfer ){
			
			return( torrent_transfer.read( contact, listener, type, key, timeout ));
			
		}else{
			
			byte[]	data = getDHT().read( 
								new DHTPluginProgressListener()
								{
									public void
									reportSize(
										long	size )
									{
										listener.reportSize( size );
									}
									
									public void
									reportActivity(
										String	str )
									{
										listener.reportActivity( str );
									}
									
									public void
									reportCompleteness(
										int		percent )
									{
										listener.reportCompleteness( percent );
									}
								},
								contact.getContact(),
								DDBaseHelpers.getKey(type.getClass()).getHash(),
								((DDBaseKeyImpl)key).getBytes(),
								timeout );
								
			if ( data == null ){
				
				return( null );
			}
			
			return( new DDBaseValueImpl( contact, data, SystemTime.getCurrentTime()));
		}
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
		private boolean						exhaustive;
		
		protected
		listenerMapper(
			DistributedDatabaseListener	_listener,
			int							_type,
			DistributedDatabaseKey		_key,
			long						_timeout,
			boolean						_exhaustive )
		{
			listener	= _listener;
			type		= _type;
			key			= _key;
			key_bytes	= ((DDBaseKeyImpl)key).getBytes();
			timeout		= _timeout;
			exhaustive	= _exhaustive;
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
			DHTPluginValue		_value )
		{
			byte[]	value = _value.getValue();
			
			if ( _value.getFlags() == DHTPlugin.FLAG_MULTI_VALUE ){

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
					
					listener.event( new dbEvent( type, key, originator, d, _value.getCreationTime()));
					
					pos += len;
				}				

				if ( value[0] == 1 ){
					
						// continuation exists
					
					final	byte[]	next_key_bytes = new SHA1Simple().calculateHash( key_bytes );
					
					complete_disabled	= true;
	
					dht.get(	
						next_key_bytes, 
						key.getDescription(),
						(byte)0, 
						256, 
						timeout, 
						exhaustive,
						new listenerMapper( listener, DistributedDatabaseEvent.ET_VALUE_READ, key, next_key_bytes, timeout ));
				}
			}else{
				
				listener.event( new dbEvent( type, key, originator, _value ));
			}
		}
		
		public void
		valueWritten(
			DHTPluginContact	target,
			DHTPluginValue		value )
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
			DHTPluginValue			_value )
		{
			type		= _type;
			key			= _key;
			
			contact	= new DDBaseContactImpl( DDBaseImpl.this, _contact );
			
			value	= new DDBaseValueImpl( contact, _value.getValue(), _value.getCreationTime()); 
		}
		
		protected
		dbEvent(
			int						_type,
			DistributedDatabaseKey	_key,
			DHTPluginContact		_contact,
			byte[]					_value,
			long					_ct )
		{
			type		= _type;
			key			= _key;
			
			contact	= new DDBaseContactImpl( DDBaseImpl.this, _contact );
			
			value	= new DDBaseValueImpl( contact, _value, _ct ); 
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