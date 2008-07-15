/*
 * Created on Jul 15, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.core.subs.impl;

import java.io.File;
import java.io.IOException;
import java.util.*;

import com.aelitis.azureus.core.vuzefile.VuzeFile;
import com.aelitis.azureus.core.vuzefile.VuzeFileComponent;
import com.aelitis.azureus.core.vuzefile.VuzeFileHandler;

public class 
SubscriptionBodyImpl 
{
	private SubscriptionManagerImpl		manager;
	
	private String	name;
	private byte[]	public_key;
	private int		version;
	
	private Map		map;

		// import constructor
	
	protected 
	SubscriptionBodyImpl(
		SubscriptionManagerImpl	_manager,
		Map						_map )
	
		throws IOException
	{
		manager	= _manager;
		map		= _map;
		
		name		= new String((byte[])map.get( "name" ), "UTF-8" );
		
		public_key	= (byte[])_map.get( "public_key" );
		version		= ((Long)_map.get( "version" )).intValue();
	}

		// create constructor
	
	protected
	SubscriptionBodyImpl(
		SubscriptionManagerImpl	_manager,
		String					_name,
		byte[]					_public_key,
		int						_version )
	{
		manager		= _manager;
		
		name		= _name;
		public_key	= _public_key;
		version		= _version;
		
		map			= new HashMap();
	}
	
	protected String
	getName()
	{
		return( name );
	}
	
	protected byte[]
	getPublicKey()
	{
		return( public_key );
	}
	
	protected int
	getVersion()
	{
		return( version );
	}
	
	/*
	protected void
	sing()
	{
		version_key 	= new byte[20];
		
		RandomUtils.nextSecureBytes( version_key );
		
		Signature sig = CryptoECCUtils.getSignature( kp.getPrivate());
		
		sig.update( version_key );
		sig.update( getVersionBytes( version ));
		
		version_sig	= sig.sign();

	}
	*/
	
	protected void
	writeVuzeFile(
		SubscriptionImpl		subs )
	
		throws IOException
	{
		File file = manager.getVuzeFile( subs );

		if ( map == null ){
	
			readMap( file );
		}
		
		map.put( "name", name.getBytes( "UTF-8" ));
		map.put( "public_key", public_key );
		map.put( "version", new Long( version ));
		
		VuzeFile	vf = VuzeFileHandler.getSingleton().create();
		
		vf.addComponent( VuzeFileComponent.COMP_TYPE_SUBSCRIPTION, map );
		
		vf.write( file );
		
		map	= null;
	}
	
	protected Map
	readMap(
		File	vuze_file )
	
		throws IOException
	{
		VuzeFile	vf = VuzeFileHandler.getSingleton().loadVuzeFile( vuze_file.getAbsolutePath());

		if ( vf == null ){
			
			throw( new IOException( "Failed to load vuze file '" + vuze_file + "'" ));
		}
		
		return( vf.getComponents()[0].getContent());
	}
}
