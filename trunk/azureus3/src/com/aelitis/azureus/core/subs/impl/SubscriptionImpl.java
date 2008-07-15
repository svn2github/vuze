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

import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.security.Signature;
import java.util.*;

import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.RandomUtils;
import org.gudy.azureus2.core3.util.SHA1Simple;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.security.CryptoECCUtils;
import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.core.subs.SubscriptionException;
import com.aelitis.azureus.core.vuzefile.VuzeFile;
import com.aelitis.azureus.core.vuzefile.VuzeFileComponent;
import com.aelitis.azureus.core.vuzefile.VuzeFileHandler;

public class 
SubscriptionImpl 
	implements Subscription 
{
	protected static final int SIMPLE_ID_LENGTH				= 10;
	
	private static final int MAX_ASSOCIATIONS				= 256;
	private static final int MIN_RECENT_ASSOC_TO_RETAIN		= 16;
	
	protected static byte[]
	getVersionBytes(
		int		version )
	{
		return( new byte[]{ (byte)(version>>24), (byte)(version>>16),(byte)(version>>8),(byte)version } );
	}
	
	protected static int
	getVersion(
		byte[]		bytes )
	{
		return( (bytes[0]<<24)&0xff000000 | (bytes[1] << 16)&0x00ff0000 | (bytes[2] << 8)&0x0000ff00 | bytes[3]&0x000000ff );
	}
		
	private SubscriptionManagerImpl		manager;
	
	private String			name;
	private byte[]			public_key;
	private byte[]			private_key;
	
	private int				version;
	
	private byte[]			short_id;
	
	private List			associations = new ArrayList();
	
	private int				fixed_random;
	
	private boolean			published;
	
		// new subs constructor
	
	protected
	SubscriptionImpl(
		SubscriptionManagerImpl		_manager,
		String						_name )
	
		throws SubscriptionException
	{
		manager	= _manager;
		
		name	= _name;
		
		try{
			KeyPair	kp = CryptoECCUtils.createKeys();
			
			public_key 	= CryptoECCUtils.keyToRawdata( kp.getPublic());
			private_key = CryptoECCUtils.keyToRawdata( kp.getPrivate());
			
			version			= 1;
							
			fixed_random	= new Random().nextInt();
			
			init();
			
			new SubscriptionBodyImpl( manager, name, public_key, version ).writeVuzeFile( this );
			
		}catch( Throwable e ){
			
			throw( new SubscriptionException( "Failed to create subscription", e ));
		}
	}
	
		// cache detail constructor
	
	protected
	SubscriptionImpl(
		SubscriptionManagerImpl		_manager,
		Map							map )
	
		throws IOException
	{
		manager	= _manager;
		
		fromMap( map );
		
		init();
	}

		// import constructor
	
	protected
	SubscriptionImpl(
		SubscriptionManagerImpl		_manager,
		SubscriptionBodyImpl		_body )
	
		throws IOException
	{
		manager	= _manager;
				
		public_key		= _body.getPublicKey();
		version			= _body.getVersion();
		name			= _body.getName();
		
		fixed_random	= new Random().nextInt();
		
		init();
		
		_body.writeVuzeFile( this );
	}
	
	protected Map
	toMap()
	
		throws IOException
	{
		synchronized( this ){
			
			Map	map = new HashMap();
			
			map.put( "name", name.getBytes( "UTF-8" ));
			
			map.put( "public_key", public_key );
						
			map.put( "version", new Long( version ));
			
				// local data
			
			if ( private_key != null ){
				
				map.put( "private_key", private_key );
			}

			map.put( "rand", new Long( fixed_random ));
			
			if ( associations.size() > 0 ){
				
				List	l_assoc = new ArrayList();
				
				map.put( "assoc", l_assoc );
				
				for (int i=0;i<associations.size();i++){
					
					association assoc = (association)associations.get(i);
					
					Map m = new HashMap();
					
					l_assoc.add( m );
					
					m.put( "h", assoc.getHash());
					m.put( "w", new Long( assoc.getWhen()));
				}
			}
			
			return( map );
		}
	}
	
	protected void
	fromMap(
		Map		map )
	
		throws IOException
	{
		name			= new String((byte[])map.get( "name"), "UTF-8" );
		public_key		= (byte[])map.get( "public_key" );
		private_key		= (byte[])map.get( "private_key" );
		version			= ((Long)map.get( "version" )).intValue();

		fixed_random	= ((Long)map.get( "rand" )).intValue();

		List	l_assoc = (List)map.get( "assoc" );
		
		if ( l_assoc != null ){
			
			for (int i=0;i<l_assoc.size();i++){
				
				Map	m = (Map)l_assoc.get(i);
				
				byte[]		hash 	= (byte[])m.get("h");
				long		when	= ((Long)m.get( "w" )).longValue();
				
				associations.add( new association( hash, when ));
			}
		}
	}
	
	protected void
	upgrade(
		SubscriptionBodyImpl		body )
	
		throws IOException
	{
		version		= body.getVersion();
		
		body.writeVuzeFile( this );
	}
	
	protected void
	init()
	{
		byte[]	hash = new SHA1Simple().calculateHash( public_key );
		
		short_id = new byte[SIMPLE_ID_LENGTH];
		
		System.arraycopy( hash, 0, short_id, 0, SIMPLE_ID_LENGTH );
	}
	
	public String
	getName()
	{
		return( name );
	}
	
	public byte[]
	getPublicKey()
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
	
	protected int
	getFixedRandom()
	{
		return( fixed_random );
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
		synchronized( this ){
	
			for (int i=0;i<associations.size();i++){
				
				association assoc = (association)associations.get(i);
				
				if ( Arrays.equals( assoc.getHash(), hash )){
					
					return;
				}
			}
			
			associations.add( new association( hash, SystemTime.getCurrentTime()));
			
			if ( associations.size() > MAX_ASSOCIATIONS ){
				
				associations.remove( new Random().nextInt( MAX_ASSOCIATIONS - MIN_RECENT_ASSOC_TO_RETAIN ));
			}
		}
		
		manager.configDirty();
		
		manager.associationAdded();
	}
	
	protected association
	getAssociationForPublish()
	{
		synchronized( this ){
			
			int	num_assoc = associations.size();
			
				// first set in order of most recent
			
			for (int i=num_assoc-1;i>=Math.max( 0, num_assoc-MIN_RECENT_ASSOC_TO_RETAIN);i--){
				
				association assoc = (association)associations.get(i);
				
				if ( !assoc.getPublished()){
					
					assoc.setPublished( true );
					
					return( assoc );
				}
			}
			
				// remaining randomised
			
			int	rem = associations.size() - MIN_RECENT_ASSOC_TO_RETAIN;
			
			if ( rem > 0 ){
				
				List l = new ArrayList( associations.subList( 0, rem ));
				
				Collections.shuffle( l );
				
				for (int i=0;i<l.size();i++){
					
					association assoc = (association)l.get(i);

					if ( !assoc.getPublished()){
						
						assoc.setPublished( true );
						
						return( assoc );
					}
				}
			}
		}
		
		return( null );
	}
	
	protected boolean
	getPublished()
	{
		return( published );
	}
	
	protected void
	setPublished(
		boolean		b )
	{
		published = b;
	}
	
	public String
	getString()
	{
		return( "name=" + name + 
					",sid=" + ByteFormatter.encodeString( short_id ) + 
					",ver=" + version + 
					",public=[" + public_key.length + "]" + 
					",private=[" + (private_key==null?"<none>":String.valueOf( private_key.length)) + "]" ); 
	}
	
	protected static class
	association
	{
		private byte[]	hash;
		private long	when;
		private boolean	published;
		
		protected
		association(
			byte[]		_hash,
			long		_when )
		{
			hash		= _hash;
			when		= _when;
		}
		
		protected byte[]
		getHash()
		{
			return( hash );
		}
		
		protected long
		getWhen()
		{
			return( when );
		}
		
		protected boolean
		getPublished()
		{
			return( published );
		}
		
		protected void
		setPublished(
			boolean		b )
		{
			published = b;
		}
		
		protected String
		getString()
		{
			return( ByteFormatter.encodeString( hash ));
		}
	}
}
