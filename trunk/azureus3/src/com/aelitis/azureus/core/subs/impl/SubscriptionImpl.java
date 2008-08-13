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

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentCreator;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.LightHashMap;
import org.gudy.azureus2.core3.util.SHA1Simple;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.json.simple.JSONObject;

import com.aelitis.azureus.core.lws.LightWeightSeed;
import com.aelitis.azureus.core.lws.LightWeightSeedAdapter;
import com.aelitis.azureus.core.lws.LightWeightSeedManager;
import com.aelitis.azureus.core.metasearch.Engine;
import com.aelitis.azureus.core.metasearch.MetaSearchManagerFactory;
import com.aelitis.azureus.core.security.CryptoECCUtils;
import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.core.subs.SubscriptionException;
import com.aelitis.azureus.core.subs.SubscriptionHistory;
import com.aelitis.azureus.core.subs.SubscriptionListener;
import com.aelitis.azureus.core.subs.SubscriptionManager;
import com.aelitis.azureus.core.subs.SubscriptionPopularityListener;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.util.JSONUtils;

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
	
	private byte[]			public_key;
	private byte[]			private_key;
	
	private String			name;
	private int				version;
	private boolean			is_public;
	
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
	
	private boolean			server_published;
	private boolean			server_publication_outstanding;
	
	private LightWeightSeed	lws;
	
	private boolean			destroyed;
	
	private Map				history_map;
	
	private Map				user_data = new LightHashMap();
	
	private final 			SubscriptionHistoryImpl	history;
	
	private String			referer;
	
	private CopyOnWriteList	listeners = new CopyOnWriteList();
	
		// new subs constructor
	
	protected
	SubscriptionImpl(
		SubscriptionManagerImpl		_manager,
		String						_name,
		boolean						_public,
		String						_json_content )
	
		throws SubscriptionException
	{
		manager	= _manager;
		
		history_map	= new HashMap();

		history = new SubscriptionHistoryImpl( manager, this );
		
		name		= _name;
		is_public	= _public;
		version		= 1;
		
		try{
			KeyPair	kp = CryptoECCUtils.createKeys();
			
			public_key 	= CryptoECCUtils.keyToRawdata( kp.getPublic());
			private_key = CryptoECCUtils.keyToRawdata( kp.getPrivate());
			
				
			add_type			= ADD_TYPE_CREATE;
			add_time			= SystemTime.getCurrentTime();
			
			is_subscribed		= true;
			
			fixed_random	= new Random().nextInt();
			
			init();
			
			String json_content = embedEngines( _json_content );
			
			SubscriptionBodyImpl body = new SubscriptionBodyImpl( manager, name, is_public, json_content, public_key, version );
						
			syncToBody( body );
			
		}catch( Throwable e ){
			
			throw( new SubscriptionException( "Failed to create subscription", e ));
		}
	}
	
	protected static String
	getSkeletonJSON(
		Engine		engine )
	{
		JSONObject	map = new JSONObject();
		
		map.put( "engine_id", new Long( engine.getId()));
		
		map.put( "search_term", "" );

		map.put( "filters", new HashMap());
		
		map.put( "options", new HashMap());
		
		Map schedule = new HashMap();
		
		schedule.put( "interval", new Long( 120 ));
		
		List	days = new ArrayList();
		
		for (int i=1;i<=7;i++){
			
			days.add( String.valueOf(i));
		}
		schedule.put( "days", days );
		
		map.put( "schedule", schedule );
		
		embedEngines( map, engine );
		
		return( JSONUtils.encodeToJSON( map ));
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
		
		history = new SubscriptionHistoryImpl( manager, this );

		init();
	}

		// import constructor
	
	protected
	SubscriptionImpl(
		SubscriptionManagerImpl		_manager,
		SubscriptionBodyImpl		_body,
		int							_add_type,
		boolean						_is_subscribed )
	
		throws SubscriptionException
	{
		manager	= _manager;
			
		history_map	= new HashMap();
		
		history = new SubscriptionHistoryImpl( manager, this );
		
		syncFromBody( _body );
		
		add_type		= _add_type;
		add_time		= SystemTime.getCurrentTime();
		
		is_subscribed	= _is_subscribed;
		
		fixed_random	= new Random().nextInt();
		
		init();
				
		syncToBody( _body );
	}
	
	protected void
	syncFromBody(
		SubscriptionBodyImpl	body )
	{
		public_key		= body.getPublicKey();
		version			= body.getVersion();
		name			= body.getName();
		is_public		= body.isPublic();
	}
	
	protected void
	syncToBody(
		SubscriptionBodyImpl		body )
	
		throws SubscriptionException
	{
			// this picks up latest values of version, name + is_public from here
		
		body.writeVuzeFile( this );
		
		hash 			= body.getHash();
		sig				= body.getSig();
		sig_data_size	= body.getSigDataSize();
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
			
			map.put( "is_public", new Long( is_public?1:0 ));
			
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
			
			map.put( "sp", new Long( server_published?1:0 ));
			map.put( "spo", new Long( server_publication_outstanding?1:0 ));
			
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
			
			map.put( "history", history_map );
			
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
		is_public		= ((Long)map.get( "is_public")).intValue() == 1;
		
		hash			= (byte[])map.get( "hash" );
		sig				= (byte[])map.get( "sig" );
		sig_data_size	= ((Long)map.get( "sig_data_size" )).intValue();
		
		fixed_random	= ((Long)map.get( "rand" )).intValue();
		
		add_type		= ((Long)map.get( "add_type" )).intValue();		
		add_time		= ((Long)map.get( "add_time" )).longValue();
		
		is_subscribed	= ((Long)map.get( "subscribed" )).intValue()==1;
				
		popularity		= ((Long)map.get( "pop" )).longValue();
		
		highest_prompted_version = ((Long)map.get( "hupv" )).intValue();
		
		server_published = ((Long)map.get( "sp" )).intValue()==1;
		server_publication_outstanding = ((Long)map.get( "spo" )).intValue()==1;
		
		List	l_assoc = (List)map.get( "assoc" );
		
		if ( l_assoc != null ){
			
			for (int i=0;i<l_assoc.size();i++){
				
				Map	m = (Map)l_assoc.get(i);
				
				byte[]		hash 	= (byte[])m.get("h");
				long		when	= ((Long)m.get( "w" )).longValue();
				
				associations.add( new association( hash, when ));
			}
		}
		
		history_map = (Map)map.get( "history" );
		
		if ( history_map == null ){
			
			history_map = new HashMap();
		}
	}
	
	protected Map
	getHistoryConfig()
	{
		return( history_map );
	}
	
	protected void
	updateHistoryConfig(
		Map		_history_map )
	{
		history_map = _history_map;
		
		fireChanged();
	}
	
	protected void
	upgrade(
		SubscriptionBodyImpl		body )
	
		throws SubscriptionException
	{
			// pick up details from the body (excluding json that is maintained in body only)
		
		syncFromBody( body );
		
			// write to file
		
		syncToBody(body);
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
	
	public void
	setName(
		String		_name )
	
		throws SubscriptionException
	{
		if ( !name.equals( _name )){
			
			boolean	ok = false;
			
			String	old_name 	= name;
			int		old_version	= version;
			
			try{
				name	= _name;
				
				version++;
				
				SubscriptionBodyImpl body = new SubscriptionBodyImpl( manager, this );
					
				syncToBody( body );
				
				versionUpdated( body );
				
				ok	= true;
				
			}finally{
				
				if ( !ok ){
					
					name 	= old_name;
					version	= old_version;
				}
			}
			
			fireChanged();
		}
	}
	
	public boolean
	isPublic()
	{
		return( is_public );
	}
	
	public void
	setPublic(
		boolean		_is_public )
	
		throws SubscriptionException
	{
		if ( is_public != _is_public ){
				
			boolean	ok = false;
			
			boolean	old_public	= is_public;
			int		old_version	= version;
			
			try{
				is_public	= _is_public;
				
				version++;
								
				SubscriptionBodyImpl body = new SubscriptionBodyImpl( manager, this );
				
				syncToBody( body );
				
				versionUpdated( body );

				ok = true;
				
			}finally{
				
				if ( !ok ){
				
					version		= old_version;
					is_public	= old_public;
				}
			}
			
			fireChanged();
		}
	}
	
	protected boolean
	getServerPublicationOutstanding()
	{
		return( server_publication_outstanding );
	}
	
	protected void
	setServerPublicationOutstanding()	
	{
		if ( !server_publication_outstanding ){
			
			server_publication_outstanding = true;
		
			fireChanged();
		}
	}
	
	protected void
	setServerPublished()
	{
		if ( server_publication_outstanding || !server_published ){
			
			server_published 				= true;
			server_publication_outstanding	= false;
			
			fireChanged();
		}
	}
	
	protected boolean
	getServerPublished()
	{
		return( server_published );
	}
	
	public String
	getJSON()
	
		throws SubscriptionException
	{
		SubscriptionBodyImpl body = new SubscriptionBodyImpl( manager, this );

		return( body.getJSON());
	}
	
	public boolean
	setJSON(
		String		_json )
	
		throws SubscriptionException
	{
		String json = embedEngines( _json );
		
		SubscriptionBodyImpl body = new SubscriptionBodyImpl( manager, this );		
		
		String	old_json = body.getJSON();
		
		if ( !json.equals( old_json )){
			
			boolean	ok = false;
			
			int		old_version	= version;
			
			try{				
				version++;
													
				body.setJSON( json );
				
				syncToBody( body );
				
				versionUpdated( body );

				referer = null;
				
				ok	= true;
				
			}finally{
				
				if ( !ok ){
					
					version	= old_version;
				}
			}
			
			fireChanged();
			
			return( true );
		}
		
		return( false );
	}
	
	protected String
	embedEngines(
		String		json_in )
	{
			// see if we need to embed private search templates
		
		Map map = JSONUtils.decodeJSON( json_in );
		
		long 	engine_id 	= ((Long)map.get( "engine_id" )).longValue();

		String	json_out	= json_in;
		
		if ( engine_id >= Integer.MAX_VALUE || engine_id < 0 ){
			
			Engine engine = MetaSearchManagerFactory.getSingleton().getMetaSearch().getEngine( engine_id );

			if ( engine == null ){
				
				log( "Private search template with id '" + engine_id + "' not found!!!!" );
				
			}else{
				
				try{								
					embedEngines( map, engine );
					
					json_out = JSONUtils.encodeToJSON( map );
					

					log( "Embedded private search template '" + engine.getName() + "'" );
					
				}catch( Throwable e ){
					
					log( "Failed to embed private search template", e );
				}
			}
		}
		
		return( json_out );
	}
	
	protected static void
	embedEngines(
		Map			map,
		Engine		engine )
	{
		Map	engines = new HashMap();
		
		map.put( "engines", engines );
		
		Map	engine_map = new HashMap();
		
		try{
		
			String	engine_str = new String( Base64.encode( BEncoder.encode( engine.exportToBencodedMap())), "UTF-8" );
		
			engine_map.put( "content", engine_str );
		
			engines.put( String.valueOf( engine.getId()), engine_map );
			
		}catch( Throwable e ){
			
			Debug.out( e );
		}
	}

	protected Engine
	extractEngine(
		Map		json_map,
		long	id )
	{
		Map engines = (Map)json_map.get( "engines" );
		
		if ( engines != null ){
			
			Map	engine_map = (Map)engines.get( String.valueOf( id ));
			
			if ( engine_map != null ){
				
				String	engine_str = (String)engine_map.get( "content" );
				
				try{
				
					Map map = BDecoder.decode( Base64.decode( engine_str.getBytes( "UTF-8" )));
						
					return( MetaSearchManagerFactory.getSingleton().getMetaSearch().importFromBEncodedMap(map));
					
				}catch( Throwable e ){
					
					log( "failed to import engine", e );
				}
			}
		}
		
		return( null );
	}
	
	protected Engine
	getEngine(
		boolean		local_only )
	
		throws SubscriptionException
	{
		Map map = JSONUtils.decodeJSON( getJSON());
					
		return( manager.getEngine( this, map, local_only ));
	}
	
	protected void
	engineUpdated(
		Engine		engine )
	{
		try{
			String	json = getJSON();
			
			Map map = JSONUtils.decodeJSON( json );

			long	id = ((Long)map.get( "engine_id" )).longValue();
			
			if ( id == engine.getId()){
								
				if ( setJSON( json )){
					
					log( "Engine has been updated, saved" );
				}
			}
		}catch( Throwable e ){
			
			log( "Engine update failed", e );
		}
	}
	
	public boolean
	setDetails(
		String		_name,
		boolean		_is_public,
		String		_json )
	
		throws SubscriptionException
	{
		_json = embedEngines( _json );
		
		SubscriptionBodyImpl body = new SubscriptionBodyImpl( manager, this );		
		
		String	old_json = body.getJSON();
		
		if ( 	!_name.equals( name ) ||
				_is_public != is_public ||
				!_json.equals( old_json )){
			
			boolean	ok = false;
			
			String	old_name	= name;
			boolean	old_public	= is_public;
			int		old_version	= version;
			
			try{
				is_public	= _is_public;			
				name		= _name;

				body.setJSON( _json );
				
				version++;
												
				syncToBody( body );
				
				versionUpdated( body );

				ok = true;
				
			}finally{
				
				if ( !ok ){
				
					version		= old_version;
					is_public	= old_public;
					name		= old_name;
				}
			}
			
			fireChanged();
			
			return( true );
		}
		
		return( false );
	}
	
	protected void
	versionUpdated(
		SubscriptionBodyImpl		body )
	{
		if ( is_public ){
			
			manager.updatePublicSubscription( this, body.getJSON());
			
			setPublished( false );
			
			synchronized( this ){

				for (int i=0;i<associations.size();i++){
					
					((association)associations.get(i)).setPublished( false );
				}
			}
		}	
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
	
	public String
	getID()
	{
		return( Base32.encode(getShortID()));
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
			
			fireChanged();
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
		return( private_key != null );
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
			
			if ( is_subscribed ){
				
				manager.setSelected( this );
			}
			
			fireChanged();
		}
	}
	
	public void
	getPopularity(
		final SubscriptionPopularityListener	listener )
	
		throws SubscriptionException
	{
		new AEThread2( "subs:popwait", true )
		{
			public void
			run()
			{		
				try{
					manager.getPopularity( 
						SubscriptionImpl.this,
						new SubscriptionPopularityListener()
						{
							public void
							gotPopularity(
								long						pop )
							{
								if ( pop != popularity ){
									
									popularity = pop;
									
									fireChanged();
								}
								
								listener.gotPopularity( popularity );
							}
							
							public void
							failed(
								SubscriptionException		e )
							{
								if ( popularity == -1 ){
									
									listener.failed( new SubscriptionException( "Failed to read popularity", e ));
									
								}else{
									
									listener.gotPopularity( popularity );
								}
							}
						});
					
				}catch( Throwable e ){
					
					if ( popularity == -1 ){
					
						listener.failed( new SubscriptionException( "Failed to read popularity", e ));
						
					}else{
						
						listener.gotPopularity( popularity );
					}
				}
			}
		}.start();
	}
	
	public long 
	getCachedPopularity() 
	{
		return( popularity );
	}
	
	public String
	getReferer()
	{
		if ( referer == null ){
			
			try{
				Map map = JSONUtils.decodeJSON( getJSON());
						
				Engine engine = manager.getEngine( this, map, false );
				
				if ( engine != null ){
										
					referer = engine.getReferer();
				}
			}catch( Throwable e ){
				
				log( "Failed to get referer", e );
			}
			
			if ( referer == null ){
				
				referer = "";
			}
		}
		
		return( referer );
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
											log( " - generating torrent: " + Debug.getCompressedStackTrace());
											
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
						
						log( "Failed to create light-weight-seed", e );
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
		
		fireChanged();
		
		manager.associationAdded( this, hash);
	}
	
	public void
	addPotentialAssociation(
		String		result_id,
		String		key )
	{
		manager.addPotentialAssociation( this, result_id, key );
	}
	
	public int
	getAssociationCount()
	{
		synchronized( this ){
			
			return( associations.size());
		}
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
	fireChanged()
	{
		manager.configDirty( this );
		
		Iterator it = listeners.iterator();
		
		while( it.hasNext()){
			
			try{
				((SubscriptionListener)it.next()).subscriptionChanged( this );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}

	protected void
	fireDownloaded(
		boolean	was_auto )
	{

		Iterator it = listeners.iterator();
		
		while( it.hasNext()){
			
			try{
				((SubscriptionListener)it.next()).subscriptionDownloaded( this, was_auto );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
	public void
	addListener(
		SubscriptionListener	l )
	{
		listeners.add( l );
	}
	
	public void
	removeListener(
		SubscriptionListener	l )
	{
		listeners.remove( l );
	}
	
	public SubscriptionHistory 
	getHistory() 
	{
		return( history );
	}
	
	public SubscriptionManager
	getManager()
	{
		return( manager );
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
	
	public void
	remove()
	{
		destroy();
		
		manager.removeSubscription( this );
	}
	
	public void
	setUserData(
		Object		key,
		Object		data )
	{
		synchronized( user_data ){
			
			user_data.put( key, data );
		}
	}
	
	public Object
	getUserData(
		Object		key )
	{
		synchronized( user_data ){

			return( user_data.get( key ));
		}
	}
	
	protected void
	log(
		String		str )
	{
		manager.log( getString() + ": " + str );
	}
	
	protected void
	log(
		String		str,
		Throwable	e )
	{
		manager.log( getString() + ": " + str, e );
	}
	
	public String
	getString()
	{
		return( "name=" + name + 
					",sid=" + ByteFormatter.encodeString( short_id ) + 
					",ver=" + version + 
					",pub=" + is_public +
					",mine=" + isMine() +
					",sub=" + is_subscribed +
					",pop=" + popularity + 
					(server_publication_outstanding?",spo=true":""));
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
