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

package com.aelitis.azureus.plugins.dht;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.PluginInterface;

import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.DHTStorageAdapter;
import com.aelitis.azureus.core.dht.DHTStorageKey;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

/**
 * @author parg
 *
 */

public class 
DHTPluginStorageManager 
	implements DHTStorageAdapter
{
	private PluginInterface	plugin_interface;
	
	private AEMonitor	this_mon	= new AEMonitor( "DHTPlugin" );
	
	private Map					recent_addresses	= new HashMap();
	


	protected
	DHTPluginStorageManager(
		PluginInterface		_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
	}

	protected File
	getDataDir()
	{
		File	dir = new File( plugin_interface.getUtilities().getAzureusUserDir(), "dht" );
		
		dir.mkdirs();
		
		return( dir );
	}
	
	protected void
	importContacts(
		DHT		dht )
	{
		try{
			this_mon.enter();
			
			File	dir = getDataDir();
			
			File	target = new File( dir, "contacts.dat" );

			if ( target.exists()){
				
				DataInputStream	dis = null;
				
				try{
				
					dis = new DataInputStream( new FileInputStream( target ));
					
					dht.importState( dis );
					
				}finally{
					
					if ( dis != null ){
						
						dis.close();	
					}
				}
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected void
	exportContacts(
		DHT		dht )
	{
		try{
			this_mon.enter();
			
			File	dir = getDataDir();
			
			File	saving = new File( dir, "contacts.saving" );
			File	target = new File( dir, "contacts.dat" );

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
			
			this_mon.exit();
		}
	}
	
	protected void
	readRecentAddresses()
	{
		try{
			this_mon.enter();
			
			File f = new File( getDataDir(), "addresses.dat" );
			
			if ( f.exists()){
				
				BufferedInputStream	is = new BufferedInputStream( new FileInputStream( f ));
				
				recent_addresses = BDecoder.decode( is );
				
				is.close();
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected void
	writeRecentAddresses()
	{
		try{
			this_mon.enter();
		
			File f = new File( getDataDir(), "addresses.dat" );

				// remove any old crud
			
			Iterator	it = recent_addresses.keySet().iterator();
			
			while( it.hasNext()){
				
				String	key = (String)it.next();
				
				Long	time = (Long)recent_addresses.get(key);
				
				if ( SystemTime.getCurrentTime() - time.longValue() > 7*24*60*60*1000 ){
					
					it.remove();
				}
			}
			
			byte[]	data = BEncoder.encode( recent_addresses );
			
			FileOutputStream os = new FileOutputStream( f );
			
			os.write( data );
			
			os.close();
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected void
	recordCurrentAddress(
		String		address )
	{
		try{
			this_mon.enter();

			recent_addresses.put( address, new Long( SystemTime.getCurrentTime()));
		
			writeRecentAddresses();
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected boolean
	isRecentAddress(
		String		address )
	{
		try{
			this_mon.enter();

			return( recent_addresses.containsKey( address ));
					
		}finally{
			
			this_mon.exit();
		}
	}
	
	public DHTStorageKey
	keyCreated(
		HashWrapper		key )
	{
		System.out.println( "DHT key created");
		
		return(
			new DHTStorageKey()
			{
				public byte
				getDiversificationType()
				{
					return( DHT.DT_NONE );
				}
			});
	}
	
	public void
	keyDeleted(
		DHTStorageKey		key )
	{
		System.out.println( "DHT key deleted" );
	}
	
	public void
	keyRead(
		DHTStorageKey			key,
		DHTTransportContact		contact )
	{
		System.out.println( "DHT value read" );
	}
	
	public void
	valueAdded(
		DHTStorageKey		key,
		DHTTransportValue	value )
	{
		System.out.println( "DHT value added" );
	}
	
	public void
	valueUpdated(
		DHTStorageKey		key,
		DHTTransportValue	value )
	{
		System.out.println( "DHT value updated" );
	}
	
	public void
	valueDeleted(
		DHTStorageKey		key,
		DHTTransportValue	value )
	{
		System.out.println( "DHT value deleted" );
	}
	
	public byte[][]
	getExistingDiversification(
		byte[]			key,
		boolean			put_operation )
	{
		System.out.println( "DHT get existing diversification: put = " + put_operation  );
		
		return( new byte[][]{ key });
	}
	
	public byte[][]
	createNewDiversification(
		byte[]			key,
		boolean			put_operation,
		int				diversification_type )
	{
		System.out.println( "DHT create new diversification: put = " + put_operation +", type = " + diversification_type );
											
		return( new byte[0][] );
	} 
}
