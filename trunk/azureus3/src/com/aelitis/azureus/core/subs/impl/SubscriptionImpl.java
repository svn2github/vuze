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
import java.net.URL;
import java.security.KeyPair;
import java.util.*;

import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentCreator;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SHA1Simple;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TorrentUtils;

import com.aelitis.azureus.core.lws.LightWeightSeed;
import com.aelitis.azureus.core.lws.LightWeightSeedAdapter;
import com.aelitis.azureus.core.lws.LightWeightSeedManager;
import com.aelitis.azureus.core.security.CryptoECCUtils;
import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.core.subs.SubscriptionException;

public class 
SubscriptionImpl 
	implements Subscription 
{
	public static final int	ADD_TYPE_CREATE		= 1;
	public static final int	ADD_TYPE_IMPORT		= 2;
	public static final int	ADD_TYPE_LOOKUP		= 3;
	
	protected static final int SIMPLE_ID_LENGTH				= 10;
	
	private static final int MAX_ASSOCIATIONS				= 256;
	private static final int MIN_RECENT_ASSOC_TO_RETAIN		= 16;
	
	protected static byte[]
	intToBytes(
		int		version )
	{
		return( new byte[]{ (byte)(version>>24), (byte)(version>>16),(byte)(version>>8),(byte)version } );
	}
	
	protected static int
	bytesToInt(
		byte[]		bytes )
	{
		return( (bytes[0]<<24)&0xff000000 | (bytes[1] << 16)&0x00ff0000 | (bytes[2] << 8)&0x0000ff00 | bytes[3]&0x000000ff );
	}
		
	private SubscriptionManagerImpl		manager;
	
	private String			name;
	private byte[]			public_key;
	private byte[]			private_key;
	
	private int				version;
	
	private byte[]			hash;
	private byte[]			sig;
	private int				sig_data_size;
	
	private int				add_type;
	private long			add_time;
	
	private boolean			is_subscribed;
	
	private int				highest_prompted_version;
	
	private byte[]			short_id;
	
	private List			associations = new ArrayList();
	
	private int				fixed_random;
	
	private long			popularity				= -1;
	
	private long			last_auto_upgrade_check	= -1;
	private boolean			published;
	
	private LightWeightSeed	lws;
	
	private boolean			destroyed;
	
	
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
				
			add_type			= ADD_TYPE_CREATE;
			add_time			= SystemTime.getCurrentTime();
			
			is_subscribed		= true;
			
			fixed_random	= new Random().nextInt();
			
			init();
			
			SubscriptionBodyImpl body = new SubscriptionBodyImpl( manager, name, public_key, version );
			
			body.writeVuzeFile( this );
			
			update( body );
			
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
		SubscriptionBodyImpl		_body,
		int							_add_type,
		boolean						_is_subscribed )
	
		throws IOException
	{
		manager	= _manager;
				
		public_key		= _body.getPublicKey();
		version			= _body.getVersion();
		name			= _body.getName();
		
		add_type		= _add_type;
		add_time		= SystemTime.getCurrentTime();
		
		is_subscribed	= _is_subscribed;
		
		fixed_random	= new Random().nextInt();
		
		init();
		
		_body.writeVuzeFile( this );
		
		update( _body );
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
			
				// body data
			
			map.put( "hash", hash );
			map.put( "sig", sig );
			map.put( "sig_data_size", new Long( sig_data_size ));
			
				// local data
			
			if ( private_key != null ){
				
				map.put( "private_key", private_key );
			}

			map.put( "add_type", new Long( add_type ));
			map.put( "add_time", new Long( add_time ));
			
			map.put( "subscribed", new Long( is_subscribed?1:0 ));
			
			map.put( "pop", new Long( popularity ));
			
			map.put( "rand", new Long( fixed_random ));
			
			map.put( "hupv", new Long( highest_prompted_version ));
			
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

		hash			= (byte[])map.get( "hash" );
		sig				= (byte[])map.get( "sig" );
		sig_data_size	= ((Long)map.get( "sig_data_size" )).intValue();
		
		fixed_random	= ((Long)map.get( "rand" )).intValue();

		Long	l_add_type 	= (Long)map.get( "add_type" );
		
		add_type		= l_add_type==null?ADD_TYPE_CREATE:l_add_type.intValue();

		Long	l_add_time 	= (Long)map.get( "add_time" );
		
		add_time		= l_add_time==null?SystemTime.getCurrentTime():l_add_time.longValue();

		Long	l_subs 	= (Long)map.get( "subscribed" );
		
		is_subscribed	= l_subs==null?true:l_subs.intValue()==1;
		
		Long	l_pop 	= (Long)map.get( "pop" );
		
		popularity		= l_pop==null?-1:l_pop.longValue();

		
		Long	l_hupv = (Long)map.get( "hupv" );
		
		highest_prompted_version = l_hupv==null?version:l_hupv.intValue();
		
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
		
		update( body );
	}
	
	protected void
	init()
	{
		byte[]	hash = new SHA1Simple().calculateHash( public_key );
		
		short_id = new byte[SIMPLE_ID_LENGTH];
		
		System.arraycopy( hash, 0, short_id, 0, SIMPLE_ID_LENGTH );
	}
	
	protected void
	update(
		SubscriptionBodyImpl		body )
	{
		hash 			= body.getHash();
		sig				= body.getSig();
		sig_data_size	= body.getSigDataSize();
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
	
	protected void
	setHighestUserPromptedVersion(
		int		v )
	{
		if ( v < version ){
			
			v  = version;
		}
		
		if ( highest_prompted_version != v ){
			
			highest_prompted_version = v;
			
			manager.configDirty();
		}
	}
	
	protected int
	getHighestUserPromptedVersion()
	{
		return( highest_prompted_version );
	}
	
	public boolean
	isMine()
	{
			// TODO:
		
		return( false );
	}
	
	public boolean
	isPublic()
	{
			// TODO:
		
		return( true );
	}
	
	public boolean
	isSubscribed()
	{
		return( is_subscribed );
	}
	
	public void
	setSubscribed(
		boolean			s )
	{
		if ( is_subscribed != s ){
			
			is_subscribed = s;
			
			manager.configDirty();
		}
	}
	
	public long
	getPopularity()
	
		throws SubscriptionException
	{
		try{
			long	pop = manager.getPopularity( this );
			
			if ( pop != popularity ){
				
				popularity = pop;
				
				manager.configDirty();
			}
		}catch( Throwable e ){
			
			if ( popularity == -1 ){
			
				throw( new SubscriptionException( "Failed to read popularity", e ));
			}
		}

		return( popularity );
	}
	
	protected void
	checkPublish()
	{
		synchronized( this ){
			
			if ( destroyed ){
				
				return;
			}
				
			if ( hash != null ){
				
				boolean	create = false;

				if ( lws == null ){
					
					create = true;
					
				}else{
					
					if ( !Arrays.equals( lws.getHash().getBytes(), hash )){
			
						lws.remove();
						
						create = true;
					}
				}
				
				if ( create ){
										
					try{
						File original_data_location = manager.getVuzeFile( this );

						if ( original_data_location.exists()){
							
								// make a version based filename to avoid issues regarding multiple
								// versions
							
							final File	versioned_data_location = new File( original_data_location.getParent(), original_data_location.getName() + "." + getVersion());
							
							if ( !versioned_data_location.exists()){
								
								if ( !FileUtil.copyFile( original_data_location, versioned_data_location )){
									
									throw( new Exception( "Failed to copy file to '" + versioned_data_location + "'" ));
								}
							}
							
							lws = LightWeightSeedManager.getSingleton().add(
									getName(),
									new HashWrapper( hash ),
									TorrentUtils.getDecentralisedEmptyURL(),
									versioned_data_location,
									new LightWeightSeedAdapter()
									{
										public TOTorrent 
										getTorrent(
											byte[] 		hash,
											URL 		announce_url, 
											File 		data_location) 
										
											throws Exception
										{
											manager.log( getString() + " - generating torrent: " + Debug.getCompressedStackTrace());
											
											TOTorrentCreator creator = 
												TOTorrentFactory.createFromFileOrDirWithFixedPieceLength( 
														data_location, 
														announce_url,
														256*1024 );
									
											TOTorrent t = creator.create();
											
											t.setHashOverride( hash );
											
											return( t );
										}
									});
						}
								
					}catch( Throwable e ){
						
						manager.log( "Failed to create light-weight-seed", e );
					}
				}
			}
		}
	}
	
	protected synchronized boolean
	canAutoUpgradeCheck()
	{
		long	now = SystemTime.getMonotonousTime();
		
		if ( last_auto_upgrade_check == -1 || now - last_auto_upgrade_check > 4*60*60*1000 ){
			
			last_auto_upgrade_check = now;
			
			return( true );
		}
		
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
		
		manager.associationAdded( this, hash);
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
	
	protected int
	getVerifiedPublicationVersion(
		Map		details )
	{
		if ( !verifyPublicationDetails( details )){
			
			return( -1 );
		}

		return( getPublicationVersion( details ));
	}
	
	protected static int
	getPublicationVersion(
		Map		details )
	{
		return(((Long)details.get("v")).intValue());
	}
	
	protected byte[]
	getPublicationHash()
	{
		return( hash );
	}
	
	protected static byte[]
	getPublicationHash(
		Map		details )
	{
		return((byte[])details.get( "h" ));
	}
	
	protected static int
	getPublicationSize(
		Map		details )
	{
		return(((Long)details.get("z")).intValue());
	}
	
	protected Map
	getPublicationDetails()
	{
		Map	result = new HashMap();
		
		result.put( "h", hash );
		result.put( "v", new Long( version ));
		result.put( "z", new Long( sig_data_size ));
		result.put( "s", sig );
				
		return( result );
	}
	
	protected boolean
	verifyPublicationDetails(
		Map		details )
	{
		byte[]	hash 	= (byte[])details.get( "h" );
		int		version	= ((Long)details.get( "v" )).intValue();
		int		size	= ((Long)details.get( "z" )).intValue();
		byte[]	sig		= (byte[])details.get( "s" );
		
		return( SubscriptionBodyImpl.verify( public_key, hash, version, size, sig ));
	}
	
	protected void
	destroy()
	{
		LightWeightSeed l;
		
		synchronized( this ){
			
			destroyed	= true;
			
			l = lws;
		}
		
		if ( l != null ){
			
			l.remove();
		}
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
