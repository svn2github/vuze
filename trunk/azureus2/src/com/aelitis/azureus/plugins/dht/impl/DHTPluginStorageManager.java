/*
 * Created on 12-Mar-2005
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

package com.aelitis.azureus.plugins.dht.impl;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SHA1Simple;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.logging.LoggerChannel;

import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.DHTLogger;
import com.aelitis.azureus.core.dht.DHTStorageAdapter;
import com.aelitis.azureus.core.dht.DHTStorageKey;
import com.aelitis.azureus.core.dht.impl.DHTLog;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;
import com.aelitis.azureus.core.util.bloom.BloomFilter;
import com.aelitis.azureus.core.util.bloom.BloomFilterFactory;

/**
 * @author parg
 *
 */

public class 
DHTPluginStorageManager 
	implements DHTStorageAdapter
{
	private static final long		ADDRESS_EXPIRY			= 7*24*60*60*1000; 
	private static final int		DIV_WIDTH				= 10;
	private static final int		DIV_FRAG_GET_SIZE		= 2;
	private static final long		DIV_EXPIRY_MIN			= 2*24*60*60*1000;
	private static final long		DIV_EXPIRY_RAND			= 1*24*60*60*1000;
		
	
	public static final int			LOCAL_DIVERSIFICATION_SIZE_LIMIT			= 4096;
	public static final int			LOCAL_DIVERSIFICATION_ENTRIES_LIMIT			= 512;
	public static final int			LOCAL_DIVERSIFICATION_READS_PER_MIN_SAMPLES	= 3;
	public static final int			LOCAL_DIVERSIFICATION_READS_PER_MIN			= 30;
	
	public static final int			MAX_STORAGE_KEYS	= 65536;
	
	private DHTLogger		log;
	private File			data_dir;
	
	private AEMonitor	address_mon	= new AEMonitor( "DHTPluginStorageManager:address" );
	private AEMonitor	contact_mon	= new AEMonitor( "DHTPluginStorageManager:contact" );
	private AEMonitor	storage_mon	= new AEMonitor( "DHTPluginStorageManager:storage" );
	private AEMonitor	version_mon	= new AEMonitor( "DHTPluginStorageManager:version" );
	
	private Map					version_map			= new HashMap();
	private Map					recent_addresses	= new HashMap();
	
	private Map					remote_diversifications	= new HashMap();
	private Map					local_storage_keys		= new HashMap();
	

	public
	DHTPluginStorageManager(
		DHTLogger			_log,
		File				_data_dir )
	{
		log			= _log;
		data_dir	= _data_dir;
		
		data_dir.mkdirs();
		
		readRecentAddresses();
		
		readDiversifications();
		
		readVersionData();
	}
	
	protected void
	importContacts(
		DHT		dht )
	{
		try{
			contact_mon.enter();
						
			File	target = new File( data_dir, "contacts.dat" );

			if ( !target.exists()){
				
				target	= new File( data_dir, "contacts.saving" );
			}

			if ( target.exists()){
				
				DataInputStream	dis =  new DataInputStream( new FileInputStream( target ));
				
				try{
					
					dht.importState( dis );
					
				}finally{
											
					dis.close();
				}
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
			
		}finally{
			
			contact_mon.exit();
		}
	}
	
	protected void
	exportContacts(
		DHT		dht )
	{
		try{
			contact_mon.enter();
						
			File	saving = new File( data_dir, "contacts.saving" );
			File	target = new File( data_dir, "contacts.dat" );

			saving.delete();
			
			DataOutputStream	dos	= null;
			
			boolean	ok = false;
			
			try{
				dos = new DataOutputStream( new FileOutputStream( saving ));
					
				dht.exportState( dos, 32 );
			
				ok	= true;
				
			}finally{
				
				if ( dos != null ){
					
					dos.close();
					
					if ( ok ){
						
						target.delete();
						
						saving.renameTo( target );
					}
				}
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
			
		}finally{
			
			contact_mon.exit();
		}
		
			// this is a good point to save diversifications - useful when they've expired
			// as writing isn't triggered at expiry time 
		
		writeDiversifications();
	}
	
	protected void
	readRecentAddresses()
	{
		try{
			address_mon.enter();
			
			recent_addresses = readMapFromFile( "addresses" );
	
		}finally{
			
			address_mon.exit();
		}
	}
	
	protected void
	writeRecentAddresses()
	{
		try{
			address_mon.enter();
			
				// remove any old crud
			
			Iterator	it = recent_addresses.keySet().iterator();
			
			while( it.hasNext()){
				
				String	key = (String)it.next();
				
				if ( !key.equals( "most_recent" )){
					
					Long	time = (Long)recent_addresses.get(key);
					
					if ( SystemTime.getCurrentTime() - time.longValue() > ADDRESS_EXPIRY ){
						
						it.remove();
					}
				}
			}
			
			writeMapToFile( recent_addresses, "addresses" );
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
	
		}finally{
			
			address_mon.exit();
		}
	}
	
	protected void
	recordCurrentAddress(
		String		address )
	{
		try{
			address_mon.enter();

			recent_addresses.put( address, new Long( SystemTime.getCurrentTime()));
		
			recent_addresses.put( "most_recent", address.getBytes());
			
			writeRecentAddresses();
			
		}finally{
			
			address_mon.exit();
		}
	}
	
	protected String
	getMostRecentAddress()
	{
		byte[]	addr = (byte[])recent_addresses.get( "most_recent" );
		
		if ( addr == null ){
			
			return( null );
		}
		
		return( new String( addr ));
	}
	
	protected boolean
	isRecentAddress(
		String		address )
	{
		try{
			address_mon.enter();

			return( recent_addresses.containsKey( address ));
					
		}finally{
			
			address_mon.exit();
		}
	}
	
	
	protected Map
	readMapFromFile(
		String		file_prefix )
	{
		try{
			File target = new File( data_dir, file_prefix + ".dat" );
			
			if ( !target.exists()){
				
				target	= new File( data_dir, file_prefix + ".saving" );
			}
			
			if ( target.exists()){
				
				BufferedInputStream	is = new BufferedInputStream( new FileInputStream( target ));
				
				try{
					return( BDecoder.decode( is ));
					
				}finally{
					
					is.close();
				}
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );	
		}		
		
		return( new HashMap());
	}
	
	protected void
	writeMapToFile(
		Map			map,
		String		file_prefix )
	{
		try{
			File	saving = new File( data_dir, file_prefix + ".saving" );
			File	target = new File( data_dir, file_prefix + ".dat" );

			saving.delete();
			
			FileOutputStream os = null;
			
			boolean	ok = false;
			
			try{
				byte[]	data = BEncoder.encode( map );
				
				os = new FileOutputStream( saving );
					
				os.write( data );
			
				os.close();
			
				ok	= true;
				
			}finally{
				
				if ( os != null ){
					
					os.close();
					
					if ( ok ){
						
						target.delete();
						
						saving.renameTo( target );
					}
				}
			}
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	
	protected void
	readVersionData()
	{
		try{
			version_mon.enter();
			
			version_map = readMapFromFile( "version" );
	
		}finally{
			
			version_mon.exit();
		}
	}
	
	protected void
	writeVersionData()
	{
		try{
			version_mon.enter();
			
			writeMapToFile( version_map, "version" );
	
		}finally{
			
			version_mon.exit();
		}
	}
	public int
	getNextValueVersions(
		int		num )
	{
		try{
			version_mon.enter();

			Long	l_next = (Long)version_map.get( "next" );
			
			int	now = (int)(SystemTime.getCurrentTime()/1000);

			int	next;
			
			if ( l_next == null ){

				next = now;
				
			}else{
				
				next = l_next.intValue();
				
					// if "next" is in the future then we live with it to try and ensure increasing
					// values (system clock must have changed)
				
				if ( next < now ){
					
					next = now;
				}
			}
			
			version_map.put( "next", new Long( next+num ));
			
			writeVersionData();
			
			return( next );
			
		}finally{
			
			version_mon.exit();
		}
	}
	
		// key storage
	
	public DHTStorageKey
	keyCreated(
		HashWrapper		key,
		boolean			local )
	{
		//System.out.println( "DHT key created");
		
		try{
			storage_mon.enter();
		
			return(	getStorageKey( key ));
			
		}finally{
			
			storage_mon.exit();
		}
	}
	
	public void
	keyDeleted(
		DHTStorageKey		key )
	{
		//System.out.println( "DHT key deleted" );
		
		try{
			storage_mon.enter();
		
			deleteStorageKey((storageKey)key );
			
		}finally{
			
			storage_mon.exit();
		}
	}
	
	public void
	keyRead(
		DHTStorageKey			key,
		DHTTransportContact		contact )
	{
		//System.out.println( "DHT value read" );
		
		try{
			storage_mon.enter();
		
			((storageKey)key).read( contact );
			
		}finally{
			
			storage_mon.exit();
		}
	}
	
	public void
	valueAdded(
		DHTStorageKey		key,
		DHTTransportValue	value )
	{
		//System.out.println( "DHT value added" );
		
		try{
			storage_mon.enter();
		
			((storageKey)key).valueChanged( 1, value.getValue().length);
			
		}finally{
			
			storage_mon.exit();
		}
	}
	
	public void
	valueUpdated(
		DHTStorageKey		key,
		DHTTransportValue	old_value,
		DHTTransportValue	new_value )
	{
		//System.out.println( "DHT value updated" );
		
		try{
			storage_mon.enter();
			
			((storageKey)key).valueChanged( 0, new_value.getValue().length - old_value.getValue().length);
			
		}finally{
			
			storage_mon.exit();
		}
	}
	
	public void
	valueDeleted(
		DHTStorageKey		key,
		DHTTransportValue	value )
	{
		//System.out.println( "DHT value deleted" );
		
		try{
			storage_mon.enter();
		
			((storageKey)key).valueChanged( -1, -value.getValue().length);
			
		}finally{
			
			storage_mon.exit();
		}
	}
	
		// get diversifications for put operations must deterministically return the same end points
		// but gets for gets should be randomised to load balance
	
	public byte[][]
	getExistingDiversification(
		byte[]			key,
		boolean			put_operation,
		boolean			exhaustive )
	{
		//System.out.println( "DHT get existing diversification: put = " + put_operation  );
		
		HashWrapper	wrapper = new HashWrapper( key );
		
			// must always return a value - original if no diversification exists
		
		try{
			storage_mon.enter();
		
			byte[][]	res = followDivChain( wrapper, put_operation, exhaustive );
			
			if ( !Arrays.equals( res[0], key )){
				
				String	trace = "";
				
				for (int i=0;i<res.length;i++){
					trace += (i==0?"":",") + DHTLog.getString2( res[i] );
				}
				
				log.log( "SM: get div: " + DHTLog.getString2(key) + ", put = " + put_operation + ", exh = " + exhaustive + " -> " + trace );
			}
			
			return( res );
			
		}finally{
			
			storage_mon.exit();
		}
	}
	
	public byte[][]
	createNewDiversification(
		DHTTransportContact	cause,
		byte[]				key,
		boolean				put_operation,
		byte				diversification_type,
		boolean				exhaustive )
	{
		//System.out.println( "DHT create new diversification: put = " + put_operation +", type = " + diversification_type );
		
		HashWrapper	wrapper = new HashWrapper( key );
		
		try{
			storage_mon.enter();
		
			diversification	div = lookupDiversification( wrapper );
		
			boolean	created = false;
			
			if ( div == null ){
				
				div = createDiversification( wrapper, diversification_type );
				
				created	= true;			
			}
		
			byte[][] res = followDivChain( wrapper, put_operation, exhaustive );
		
			String	trace = "";
			
			for (int i=0;i<res.length;i++){
				
				trace += (i==0?"":",") + DHTLog.getString2( res[i] );
			}
			
			log.log( "SM: create div: " + DHTLog.getString2(key) + 
						", new = " + created + ", put = " + put_operation + 
						", exh = " + exhaustive + 
						", type = " + DHT.DT_STRINGS[diversification_type] + " -> " + trace +
						", cause = " + (cause==null?"<unknown>":cause.getString()));
			

			return( res );
			
		}finally{
			
			storage_mon.exit();
		}
	} 
	
	protected byte[][]
	followDivChain(
		HashWrapper	wrapper,
		boolean		put_operation,
		boolean		exhaustive )
	{
		List	list = new ArrayList();
		
		list.add( wrapper );
		
		list	= followDivChain( list, put_operation, 0, exhaustive, new ArrayList());
		
		byte[][]	res = new byte[list.size()][];
		
		for (int i=0;i<list.size();i++){
			
			res[i] = ((HashWrapper)list.get(i)).getBytes();
		}
		
		return( res );
	}
	
	protected List
	followDivChain(
		List		list_in,
		boolean		put_operation,
		int			depth,
		boolean		exhaustive,
		List		keys_done )
	{
		List	list_out = new ArrayList();
	
		/*
		String	indent = "";
		for(int i=0;i<depth;i++){
			indent+= "  ";
		}
		System.out.println( indent + "->" );
		*/
		
			// for each entry, if there are no diversifications then we just return the value
			// for those with divs we replace their entry with the diversified set (which can
			// include the entry itself under some circumstances )
		
		for (int i=0;i<list_in.size();i++){
			
			HashWrapper	wrapper = (HashWrapper)list_in.get(i);
		
			diversification	div = lookupDiversification( wrapper );

			if ( div == null ){
				
				if ( !list_out.contains( wrapper )){
					
					list_out.add(wrapper);
				}
				
			}else{
				
				if ( keys_done.contains( wrapper )){
					
						// we've recursed on the key, this means that a prior diversification wanted
						// the key included, so include it now
					
					if ( !list_out.contains( wrapper )){
						
						list_out.add(wrapper);
					}
					
					continue;
				}
				
				keys_done.add( wrapper );
				
					// replace this entry with the diversified keys 
				
				List	new_list = followDivChain( div.getKeys( put_operation, exhaustive ), put_operation, depth+1, exhaustive, keys_done );
				
				for (int j=0;j<new_list.size();j++){
					
					Object	entry =  new_list.get(j);
					
					if ( !list_out.contains( entry )){
						
						list_out.add(entry);
					}
				}
			}
		}
		// System.out.println( indent + "<-" );

		return( list_out );
	}
	
	protected storageKey
	getStorageKey(
		HashWrapper		key )
	{
		storageKey	res = (storageKey)local_storage_keys.get( key );
		
		if ( res == null ){
			
				// someout could be spamming us with crap, prevent things from getting
				// out of control
			
			if ( local_storage_keys.size() >= MAX_STORAGE_KEYS ){
				
				res = new storageKey( this, DHT.DT_SIZE, key ); 

				Debug.out( "DHTStorageManager: max key limit exceeded" );
				
				log.log( "SM: max storage key limit exceeded - " + DHTLog.getString2( key.getBytes()));
				
			}else{

				res = new storageKey( this, DHT.DT_NONE, key ); 
			
				local_storage_keys.put( key, res );
			}
		}
		
		return( res );
	}
	
	protected void
	deleteStorageKey(
		storageKey		key )
	{
		if ( local_storage_keys.remove( key ) != null ){
		
			if ( key.getDiversificationType() != DHT.DT_NONE ){
				
				writeDiversifications();
			}
		}
	}
	
	protected void
	readDiversifications()
	{
		try{
			storage_mon.enter();
			
			Map	map = readMapFromFile( "diverse" );
	
			List	keys = (List)map.get("local");
			
			if ( keys != null ){
				
				long	now = SystemTime.getCurrentTime();
				
				for (int i=0;i<keys.size();i++){
					
					storageKey d = storageKey.deserialise(this, (Map)keys.get(i));
						
					long	time_left = d.getExpiry() - now;
					
					if ( time_left > 0 ){
					
						local_storage_keys.put( d.getKey(), d );
						
					}else{
						
						log.log( "SM: serialised sk: " + DHTLog.getString2( d.getKey().getBytes()) + " expired" );
					}
				}
			}
			List	divs = (List)map.get("remote");
			
			if ( divs != null ){
				
				long	now = SystemTime.getCurrentTime();
				
				for (int i=0;i<divs.size();i++){
					
					diversification d = diversification.deserialise( this, (Map)divs.get(i));
						
					long	time_left = d.getExpiry() - now;

					if ( time_left > 0 ){
					
						remote_diversifications.put( d.getKey(), d );
						
					}else{
						
						log.log( "SM: serialised div: " + DHTLog.getString2( d.getKey().getBytes()) + " expired" );
					}
				}
			}
			
		}finally{
			
			storage_mon.exit();
		}
	}
	
	protected void
	writeDiversifications()
	{
		try{
			storage_mon.enter();
	
			Map	map = new HashMap();
			
			List	keys = new ArrayList();
			
			map.put( "local", keys );
			
			Iterator	it = local_storage_keys.values().iterator();
			
			while( it.hasNext()){
			
				storageKey	key = (storageKey)it.next();
				
				if ( key.getDiversificationType() != DHT.DT_NONE ){
					
					keys.add(key.serialise());
				}
			}
			
			List	divs = new ArrayList();
			
			map.put( "remote", divs );
			
			it = remote_diversifications.values().iterator();
			
			while( it.hasNext()){
			
				divs.add(((diversification)it.next()).serialise());
			}
			
			writeMapToFile( map, "diverse" );
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
	
		}finally{
			
			storage_mon.exit();
		}
	}
	
	protected diversification
	lookupDiversification(
		HashWrapper	wrapper )
	{
		diversification	div = (diversification)remote_diversifications.get(wrapper);
		
		if ( div != null ){
			
			if ( div.getExpiry() < SystemTime.getCurrentTime()){
				
				log.log( "SM: div: " + DHTLog.getString2( div.getKey().getBytes()) + " expired" );

				remote_diversifications.remove( wrapper );
				
				div = null;
			}
		}
		
		return( div );
	}
	
	protected diversification
	createDiversification(
		HashWrapper			wrapper,
		byte				type )
	{
		diversification	div = new diversification( this, wrapper, type );
			
		remote_diversifications.put( wrapper, div );
		
		writeDiversifications();
		
		return( div );
	}
	
	protected static String
	formatExpiry(
		long	l )
	{
		long	diff = l - SystemTime.getCurrentTime();
		
		return( (diff<0?"-":"") + DisplayFormatters.formatTime(Math.abs(diff)));
	}
	
	protected static class
	diversification
	{
		private DHTPluginStorageManager	manager;	

		private HashWrapper			key;
		private byte				type;
		
		private long				expiry;
		
		private int[]				fixed_put_offsets;
		
		protected
		diversification(
			DHTPluginStorageManager	_manager,
			HashWrapper				_key,
			byte					_type )
		
		{
			manager	= _manager;
			key		= _key;
			type	= _type;
			
			expiry	= SystemTime.getCurrentTime() + DIV_EXPIRY_MIN + (long)(Math.random() * DIV_EXPIRY_RAND );
			
			fixed_put_offsets	= new int[DIV_FRAG_GET_SIZE];
			
			int	pos = 0;
			
			while( pos < DIV_FRAG_GET_SIZE ){
				
				int i = (int)(Math.random()*DIV_WIDTH);
				
				boolean	found = false;
				
				for (int j=0;j<pos;j++){
					
					if( i == fixed_put_offsets[j] ){
						
						found	= true;
						
						break;
					}
				}
				
				if ( !found ){
					
					fixed_put_offsets[pos++] = i;
				}
			}
		}
		
		protected
		diversification(
			DHTPluginStorageManager	_manager,
			HashWrapper				_key,
			byte					_type,
			long					_expiry,
			int[]					_fixed_put_offsets )
		{
			manager				= _manager;
			key					= _key;
			type				= _type;
			expiry				= _expiry;
			fixed_put_offsets	= _fixed_put_offsets;
		}
		
		protected Map
		serialise()
		{
			Map	map = new HashMap();
			
			map.put( "key", key.getBytes());
			map.put( "type", new Long(type));
			map.put( "exp", new Long(expiry));
			
			List	offsets = new ArrayList();
			
			for (int i=0;i<fixed_put_offsets.length;i++){
				
				offsets.add( new Long( fixed_put_offsets[i]));
			}
			
			map.put( "fpo", offsets );
			
			manager.log.log( "SM: serialised div: " + DHTLog.getString2( key.getBytes()) + ", " + DHT.DT_STRINGS[type] + ", " + formatExpiry(expiry));

			return( map );
		}
		
		protected static diversification
		deserialise(
			DHTPluginStorageManager	_manager,
			Map						_map )
		{
			HashWrapper	key 	= new HashWrapper((byte[])_map.get("key"));
			int			type 	= ((Long)_map.get("type")).intValue(); 
			long		exp 	= ((Long)_map.get("exp")).longValue();
			
			List	offsets = (List)_map.get("fpo");
			
			int[]	fops = new int[offsets.size()];
			
			for (int i=0;i<fops.length;i++){
				
				fops[i] = ((Long)offsets.get(i)).intValue();
			}
			
			_manager.log.log( "SM: deserialised div: " + DHTLog.getString2( key.getBytes()) + ", " + DHT.DT_STRINGS[type] + ", " + formatExpiry(exp));

			return( new diversification( _manager, key, (byte)type, exp, fops ));
		}
		
		protected HashWrapper
		getKey()
		{
			return( key );
		}
		
		protected long
		getExpiry()
		{
			return( expiry );
		}
		
		protected List
		getKeys(
			boolean		put,
			boolean		exhaustive )
		{
			List	keys = new ArrayList();
			
			if ( put ){
				
				if ( type == DHT.DT_FREQUENCY ){
					
						// put to all keys
										
					for (int i=0;i<DIV_WIDTH;i++){
						
						keys.add( diversifyKey( key, i ));
					}
					
					if ( exhaustive ){
						
							// include original key
						
						// System.out.println( "followDivs:put:freq adding original" );
						
						keys.add( key );
					}				
				}else{
					
						// put to a fixed subset. has to be fixed else over time we'll put to
						// all the fragmented locations and nullify the point of this. gets are
						// randomised to we don't loose out by fixing the puts
															
					for (int i=0;i<fixed_put_offsets.length;i++){
						
						keys.add( diversifyKey( key, fixed_put_offsets[i]));
					}	
					
					if ( exhaustive ){
						
							// include original key
						
						// System.out.println( "followDivs:put:size adding original" );

						keys.add( key );
					}
				}
			}else{
				
					// get always returns a randomised selection
				
				if ( type == DHT.DT_FREQUENCY ){
					
						// diversification has lead to caching at all 'n' places
					
					keys.add( diversifyKey( key,(int)(Math.random()*DIV_WIDTH)));
					
				}else{
					
						// diversification has fragmented across 'n' places
						// select 2 to search or all if exhaustive
					
					if ( exhaustive ){
						
						for (int i=0;i<DIV_WIDTH;i++){
							
							keys.add( diversifyKey( key, i ));
						}

						// System.out.println( "followDivs:get:size adding all" );

					}else{
						
						List	randoms = new ArrayList();
						
						while( randoms.size() < DIV_FRAG_GET_SIZE ){
							
							Integer	i = new Integer((int)(Math.random()*DIV_WIDTH));
							
							if ( !randoms.contains(i)){
								
								randoms.add( i );
							}
						}
											
						for (int i=0;i<DIV_FRAG_GET_SIZE;i++){
							
							keys.add( diversifyKey( key, ((Integer) randoms.get(i)).intValue()));
						}
					}
				}
			}
			
			return( keys );
		}
	
		protected HashWrapper
		diversifyKey(
			HashWrapper		key_in,
			int				offset )
		{
			byte[]	old_bytes	= key_in.getBytes();
			
			byte[]	bytes = new byte[old_bytes.length+1];
			
			System.arraycopy( old_bytes, 0, bytes, 0, old_bytes.length );
			
			bytes[old_bytes.length] = (byte)offset;
			
			return( new HashWrapper( new SHA1Simple().calculateHash( bytes )));
		}
	}
	
	protected static class
	storageKey
		implements DHTStorageKey
	{
		private DHTPluginStorageManager	manager;
		
		private HashWrapper				key;	
		private byte					type;
		
		private int				size;
		private int				entries;
		
		private long			expiry;
		
		private long			read_count_start;
		//private int			read_count;
		//private int[]			read_history	= new int[8];
		
		private BloomFilter		ip_bloom_filter;
		
		protected
		storageKey(
			DHTPluginStorageManager	_manager,
			byte					_type,
			HashWrapper				_key )
		{
			manager		= _manager;
			type		= _type;
			key			= _key;
			
			expiry	= SystemTime.getCurrentTime() + DIV_EXPIRY_MIN + (long)(Math.random() * DIV_EXPIRY_RAND );
		}
		
		protected
		storageKey(
			DHTPluginStorageManager	_manager,
			byte					_type,
			HashWrapper				_key,
			long					_expiry )
		{
			manager		= _manager;
			type		= _type;
			key			= _key;
			expiry		= _expiry;
		}
		
		protected Map
		serialise()
		{
			Map	map = new HashMap();
			
			map.put( "key", key.getBytes());
			map.put( "type", new Long(type));
			map.put( "exp", new Long(expiry));
			
			manager.log.log( "SM: serialised sk: " + DHTLog.getString2( key.getBytes()) + ", " + DHT.DT_STRINGS[type] + ", " + formatExpiry(expiry) );
			
			return( map );
		}
		protected static storageKey
		deserialise(
			DHTPluginStorageManager	_manager,
			Map						map )
		{
			HashWrapper	key 	= new HashWrapper((byte[])map.get("key"));
			int			type 	= ((Long)map.get("type")).intValue(); 
			long		exp 	= ((Long)map.get("exp")).longValue();
			
			_manager.log.log( "SM: deserialised sk: " + DHTLog.getString2( key.getBytes()) + ", " + DHT.DT_STRINGS[type] + ", " + formatExpiry(exp));

			return( new storageKey( _manager, (byte)type, key, exp ));
		}
		
		protected HashWrapper
		getKey()
		{
			return( key );
		}
		
		protected long
		getExpiry()
		{
			return( expiry );
		}
		
		public byte
		getDiversificationType()
		{
			if ( type != DHT.DT_NONE ){
				
					// trigger timeouts here
				
				if ( expiry < SystemTime.getCurrentTime()){

					type	= DHT.DT_NONE;
					
					manager.log.log( "SM: sk: " + DHTLog.getString2( getKey().getBytes()) + " expired" );

					manager.writeDiversifications();
				}
			}
			
			return( type );
		}
		
		protected void
		read(
			DHTTransportContact	contact )
		{
			// System.out.println( "read: " + DHTLog.getString2( key.getBytes()));
						
			if ( type == DHT.DT_NONE ){				

				long	now = SystemTime.getCurrentTime();
				
				long	diff = now - read_count_start;

				if ( diff > LOCAL_DIVERSIFICATION_READS_PER_MIN_SAMPLES*60*1000 ){

					if ( ip_bloom_filter != null ){
						
						int	ip_entries = ip_bloom_filter.getEntryCount();
						
						if ( ip_entries > LOCAL_DIVERSIFICATION_READS_PER_MIN * LOCAL_DIVERSIFICATION_READS_PER_MIN_SAMPLES ){
						
							type = DHT.DT_FREQUENCY;
							
							manager.log.log( "SM: sk freq created (" + ip_entries + "reads ) - " + DHTLog.getString2( key.getBytes()));
							
							manager.writeDiversifications();
						}
					}
										
					read_count_start	= now;

					ip_bloom_filter	= null;	// just null it and drop this read, doesn't matter
											// and means that we don't bother creating a filter for
											// infrequently accessed data
					
				}else{
					
					if ( ip_bloom_filter == null ){
						
							// we want to hold enough IPs to detect a hit rate of reads_per_min*min
							// with a reasonable accuracy (sized to 10/3 to save space - this gives
							// an average of 100 adds required to detect 90 unique)
						
						ip_bloom_filter = BloomFilterFactory.createAddOnly(
								( LOCAL_DIVERSIFICATION_READS_PER_MIN * LOCAL_DIVERSIFICATION_READS_PER_MIN_SAMPLES *10 ) / 3 );
					}
					
					byte[]	address_bytes = contact.getAddress().getAddress().getAddress();
					
					ip_bloom_filter.add( address_bytes );
				}
			}
		}
		
		protected void
		valueChanged(
			int		entries_diff,
			int		size_diff )
		{
			entries += entries_diff;
			size	+= size_diff;
			
			if ( entries < 0 ){
				Debug.out( "entries negative" );
				entries	= 0;
			}
			
			if ( size < 0 ){
				Debug.out( "size negative" );
				size	= 0;
			}
			
			if ( type == DHT.DT_NONE ){
				
				if ( size > LOCAL_DIVERSIFICATION_SIZE_LIMIT ){
				
					type	= DHT.DT_SIZE;
					
					manager.log.log( "SM: sk size total created (size " + size + ") - " + DHTLog.getString2( key.getBytes()));

					manager.writeDiversifications();
					
				}else if ( entries > LOCAL_DIVERSIFICATION_ENTRIES_LIMIT ){
					
					type 	= DHT.DT_SIZE;
					
					manager.log.log( "SM: sk size entries created (" + entries + " entries) - " + DHTLog.getString2( key.getBytes()));

					manager.writeDiversifications();
				}
			}
			
			// System.out.println( "value changed: entries = " + entries + "(" + entries_diff + "), size = " +	size +  "(" + size_diff + ")");
		}
	}
}
