/*
 * Created on 20-Dec-2005
 * Created by Paul Gardner
 * Copyright (C) 2005, 2006 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.instancemanager.impl;

import java.net.InetAddress;

import org.gudy.azureus2.core3.config.COConfigurationListener;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SHA1Simple;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.PluginInterface;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.versioncheck.VersionCheckClient;
import com.aelitis.azureus.plugins.dht.DHTPlugin;
import com.aelitis.azureus.plugins.dht.DHTPluginContact;
import com.aelitis.azureus.plugins.dht.DHTPluginListener;

public class 
AZMyInstanceImpl
	extends AZInstanceImpl
{
	public static final long	FORCE_READ_EXT_MIN	= 8*60*60*1000;
	
	private AzureusCore				core;
	private AZInstanceManagerImpl	manager;
	
	private String				id;
	private InetAddress			internal_address;
	private int					tcp_port;
	
	private long				last_force_read_ext;
	private InetAddress			last_external_address;
	
	private boolean				dht_listener_added;
	
	protected
	AZMyInstanceImpl(
		AzureusCore				_core,
		AZInstanceManagerImpl	_manager )

	{
		core	= _core;
		manager	= _manager;
		
		id	= COConfigurationManager.getStringParameter( "ID", "" );
		
		if ( id.length() == 0 ){
			
			id	= "" + SystemTime.getCurrentTime();
		}
		
		id = ByteFormatter.encodeString( new SHA1Simple().calculateHash( id.getBytes()));
		
		COConfigurationManager.addListener( 
			new COConfigurationListener()
			{
				public void
				configurationSaved()
				{
					readConfig( false );
				}
			});
		
		readConfig( true );
	}
	
	protected void
	readConfig(
		boolean	first_time )
	{
		InetAddress	new_internal_address	= null;
		
		String internal_address_str = COConfigurationManager.getStringParameter("Bind IP");
		
		if ( internal_address_str.length() >= 7 ){
		
			try{
				new_internal_address = InetAddress.getByName( internal_address_str );
				
			}catch( Throwable e ){			
			}
		}
		
		if ( new_internal_address == null ){
			
			try{
				new_internal_address = InetAddress.getByName( "0.0.0.0" );
				
			}catch( Throwable e ){			
			}
		}
				
		int	new_tcp_port = COConfigurationManager.getIntParameter("TCP.Listen.Port");
		
		boolean	same = true;
		
		if ( !first_time ){
			
			same = internal_address.equals( new_internal_address) && tcp_port == new_tcp_port;
		}
		
		internal_address 	= new_internal_address;
		tcp_port			= new_tcp_port;
		
		
		if ( !same ){
			
			manager.informChanged( this );
		}
	}
	
	protected InetAddress
	readExternalAddress()
	{
	    PluginInterface dht_pi = core.getPluginManager().getPluginInterfaceByClass( DHTPlugin.class );
        
	    DHTPlugin dht = null;
	    
	    if ( dht_pi != null ){
    	
	    	dht = (DHTPlugin)dht_pi.getPlugin();
	    	
	    	if ( dht != null ){
	    		
	        	if ( !dht_listener_added ){
	        		
	        		dht_listener_added	= true;
	        		
		        	dht.addListener(
		        		new DHTPluginListener()
		        		{
		        			public void
		        			localAddressChanged(
		        				DHTPluginContact	local_contact )
		        			{
		        				manager.informChanged( AZMyInstanceImpl.this );
		        			}
		        		});
	        	}
	    	}
	    }
	    
		InetAddress	 external_address = null;
		
			// use cached version if available and the DHT isn't
		
		if ( dht == null || dht.getStatus() != DHTPlugin.STATUS_RUNNING ){
		
			String	str_address = VersionCheckClient.getSingleton().getExternalIpAddress( true );
		
			if ( str_address != null ){
				
				try{
					
					external_address	= InetAddress.getByName( str_address );
					
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
		}
		
		if ( external_address == null && dht != null  ){
			
				// no cache, use DHT (this will hang during initialisation, hence the use of cached
				// version above
			
			try{
				external_address = dht.getLocalAddress().getAddress().getAddress();
	        	
			}catch( Throwable e ){
			}
		}
		
		if ( external_address == null ){
			
				// force read it
			
			long	now = SystemTime.getCurrentTime();
			
			if ( last_force_read_ext > now ){
				
				last_force_read_ext	= now;
			}
			
			if ( now - last_force_read_ext > FORCE_READ_EXT_MIN ){
				
				last_force_read_ext	= now;
				
				external_address = core.getPluginManager().getDefaultPluginInterface().getUtilities().getPublicAddress();
			}
		}
		
			// no good address available		
		
		if ( external_address == null ){
				
			if ( last_external_address != null ){
				
				external_address = last_external_address;
				
			}else{
				try{
					external_address = InetAddress.getByName("127.0.0.1");
					
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
		}else{
			
			last_external_address	= external_address;
		}
		
		return( external_address );
	}
	
	public String
	getID()
	{
		return( id );
	}
	
	public InetAddress
	getInternalAddress()
	{
		return( internal_address );
	}
	
	public InetAddress
	getExternalAddress()
	{
		return( readExternalAddress());
	}
	
	public int
	getTrackerClientPort()
	{
		return( tcp_port );
	}
	
	public int
	getDHTPort()
	{
	    PluginInterface dht_pi = core.getPluginManager().getPluginInterfaceByClass( DHTPlugin.class );
        
	    	// may not be present
	    	
	    if ( dht_pi != null ){
	    	
	    	DHTPlugin dht = (DHTPlugin)dht_pi.getPlugin();
	         
	    	return( dht.getPort());
	    }
	    
	    return( 0 );
	}
}
