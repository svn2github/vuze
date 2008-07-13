/*
 * Created on Jul 11, 2008
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

import java.security.KeyPair;
import java.util.*;

import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.SHA1Simple;

import com.aelitis.azureus.core.security.CryptoECCUtils;
import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.core.subs.SubscriptionException;

public class 
SubscriptionImpl 
	implements Subscription 
{
	private byte[]			public_key;
	private byte[]			private_key;
	
	private byte[]			short_id;
	
	private int				version;
	
	protected
	SubscriptionImpl()
	
		throws SubscriptionException
	{
		try{
			KeyPair	kp = CryptoECCUtils.createKeys();
			
			public_key 	= CryptoECCUtils.keyToRawdata( kp.getPublic());
			private_key = CryptoECCUtils.keyToRawdata( kp.getPrivate());
			
			version		= 1;
			
			init();
			
		}catch( Throwable e ){
			
		}
	}
	
	protected
	SubscriptionImpl(
		Map		map )
	{
		fromMap( map );
		
		init();
	}

	protected Map
	toMap()
	{
		Map	map = new HashMap();
		
		map.put( "public_key", public_key );
		
		if ( private_key != null ){
			
			map.put( "private_key", private_key );
		}
		
		map.put( "version", new Long( version ));
		
		return( map );
	}
	
	protected void
	fromMap(
		Map		map )
	{
		public_key	= (byte[])map.get( "public_key" );
		private_key	= (byte[])map.get( "private_key" );
		version		= ((Long)map.get( "version" )).intValue();
	}
	
	protected void
	init()
	{
		byte[]	hash = new SHA1Simple().calculateHash( public_key );
		
		short_id = new byte[10];
		
		System.arraycopy( hash, 0, short_id, 0, 10 );
	}
	
	public byte[]
	getID()
	{
		return( public_key );
	}
	
	public byte[]
	getShortID()
	{
		return( short_id );
	}
	
	protected byte[]
	getPrivateKey()
	{
		return( private_key );
	}
	
	public int
	getVersion()
	{
		return( version );
	}
	
	public boolean
	isSubscribed()
	{
			// TODO:
		
		return( false );
	}
	
	public void
	addAssociation(
		byte[]		hash )
	{
		
	}
	
	protected String
	getString()
	{
		return( "sid=" + ByteFormatter.encodeString( short_id ) + ",ver=" + version + 
					",public=[" + public_key.length + "]" + 
					",private=[" + (private_key==null?"<none>":String.valueOf( private_key.length)) + "]" ); 
	}
}
