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
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ddb.*;
import org.gudy.azureus2.plugins.download.Download;


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
	
	
	protected
	DDBaseImpl(
		final AzureusCore	azureus_core )
	{
		PluginInterface dht_pi = 
			azureus_core.getPluginManager().getPluginInterfaceByClass(
						DHTPlugin.class );
		
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
									
									return( createValue( dl.getTorrent().writeToBEncodedData()));
									
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
		throwIfNotAvailable();
		
		dht.put(	((DDBaseKeyImpl)key).getBytes(),
					((DDBaseValueImpl)value).getBytes(),
					(byte)0,
					new listenerMapper( listener, DistributedDatabaseEvent.ET_VALUE_WRITTEN, key ));
	}
		
	public void
	read(
		DistributedDatabaseListener		listener,
		DistributedDatabaseKey			key,
		long							timeout )
	
		throws DistributedDatabaseException
	{
			// TODO: max values?
		
		dht.get(	((DDBaseKeyImpl)key).getBytes(), (byte)0, 16, timeout, new listenerMapper( listener, DistributedDatabaseEvent.ET_VALUE_READ, key ));
	}
	
	public void
	delete(
		DistributedDatabaseListener		listener,
		DistributedDatabaseKey			key )
	
		throws DistributedDatabaseException
	{
		dht.remove( ((DDBaseKeyImpl)key).getBytes(), new listenerMapper( listener, DistributedDatabaseEvent.ET_VALUE_DELETED, key ));
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
		
		protected
		listenerMapper(
			DistributedDatabaseListener	_listener,
			int							_type,
			DistributedDatabaseKey		_key )
		{
			listener	= _listener;
			type		= _type;
			key			= _key;
		}
		
		public void
		valueRead(
			DHTPluginContact	originator,
			byte[]				value,
			byte				flags )
		{
			listener.event( new dbEvent( type, key, originator, value ));
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
			listener.event( 
				new dbEvent( 
					timeout_occurred?DistributedDatabaseEvent.ET_OPERATION_TIMEOUT:DistributedDatabaseEvent.ET_OPERATION_COMPLETE,
					key ));
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