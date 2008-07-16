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
import java.util.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AEDiagnostics;
import org.gudy.azureus2.core3.util.AEDiagnosticsLogger;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DelayedEvent;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerEvent;
import org.gudy.azureus2.plugins.utils.StaticUtilities;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreLifecycleAdapter;
import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.core.subs.SubscriptionException;
import com.aelitis.azureus.core.subs.SubscriptionLookupListener;
import com.aelitis.azureus.core.subs.SubscriptionManager;
import com.aelitis.azureus.core.vuzefile.VuzeFile;
import com.aelitis.azureus.core.vuzefile.VuzeFileComponent;
import com.aelitis.azureus.core.vuzefile.VuzeFileHandler;
import com.aelitis.azureus.core.vuzefile.VuzeFileProcessor;
import com.aelitis.azureus.plugins.dht.DHTPlugin;
import com.aelitis.azureus.plugins.dht.DHTPluginContact;
import com.aelitis.azureus.plugins.dht.DHTPluginOperationListener;
import com.aelitis.azureus.plugins.dht.DHTPluginValue;


public class 
SubscriptionManagerImpl 
	implements SubscriptionManager
{
	private static final String	CONFIG_FILE = "subscriptions.config";
	private static final String	LOGGER_NAME = "Subscriptions";

	private static SubscriptionManagerImpl		singleton;
	
	public static void
	preInitialise()
	{
		VuzeFileHandler.getSingleton().addProcessor(
			new VuzeFileProcessor()
			{
				public void
				process(
					VuzeFile[]		files,
					int				expected_types )
				{
					for (int i=0;i<files.length;i++){
						
						VuzeFile	vf = files[i];
						
						VuzeFileComponent[] comps = vf.getComponents();
						
						for (int j=0;j<comps.length;j++){
							
							VuzeFileComponent comp = comps[j];
							
							if ( comp.getType() == VuzeFileComponent.COMP_TYPE_SUBSCRIPTION ){
								
								try{
									((SubscriptionManagerImpl)getSingleton()).importSubscription( comp.getContent());
									
									comp.setProcessed();
									
								}catch( Throwable e ){
									
									Debug.printStackTrace(e);
								}
							}
						}
					}
				}
			});		
	}
		
	public static synchronized SubscriptionManager
	getSingleton()
	{
		if ( singleton == null ){
			
			singleton = new SubscriptionManagerImpl();
		}
		
		return( singleton );
	}
	
	
	private boolean		started;
	
	private volatile DHTPlugin	dht_plugin;
	
	private List		subscriptions	= new ArrayList();
	
	private boolean	config_dirty;
	
	private boolean	publish_associations_active;
	private boolean publish_subscription_active;
	
	private AEDiagnosticsLogger		logger;
	
	
	protected
	SubscriptionManagerImpl()
	{
		loadConfig();

		/*
		if ( subscriptions.size() == 0 ){
			
			try{
				create( "Paul's test subs" );
				
			}catch( Throwable e ){
				
				e.printStackTrace();
			}

		
			for (int i=0;i<subscriptions.size();i++){
			
				((Subscription)subscriptions.get(i)).addAssociation( ByteFormatter.decodeString( "E02E5E117A5A9080D552A11FA675DE868A05FE71" ));
			}
		}
		*/
		
		AzureusCore	core = AzureusCoreFactory.getSingleton();
		
		core.addLifecycleListener(
			new AzureusCoreLifecycleAdapter()
			{
				public void
				started(
					AzureusCore		core )
				{
					core.removeLifecycleListener( this );
					
					startUp();
				}
			});
		
		if ( core.isStarted()){
		
			startUp();
		}
	}

	protected void
	startUp()
	{
		synchronized( this ){
			
			if ( started ){
				
				return;
			}
			
			started	= true;
		}
		
		PluginInterface  pi  = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByClass( DHTPlugin.class );

		if ( pi != null ){
			
			dht_plugin = (DHTPlugin)pi.getPlugin();
			
			if ( subscriptions.size() > 0 ){
				
				publishAssociations();
			
				publishSubscriptions();
			}
		}
	}
	
	public Subscription 
	create(
		String			name )
	
		throws SubscriptionException 
	{
		SubscriptionImpl result = new SubscriptionImpl( this, name );
		
		log( "Created new subscription: " + result.getString());
		
		synchronized( this ){
			
			subscriptions.add( result );
			
			saveConfig();
		}
		
		subscriptionAdded();
		
		return( result );
	}
	
	public Subscription
	importSubscription(
		Map		map )
	
		throws SubscriptionException
	{
		try{
			SubscriptionBodyImpl body = new SubscriptionBodyImpl( this, map );
					
			SubscriptionImpl existing = getSubscription( body.getPublicKey());
			
			if ( existing != null ){
			
				if ( existing.getVersion() <= body.getVersion()){
					
					log( "Not upgrading subscription: " + existing.getString() + " as supplied is not more recent");
					
					UIManager ui_manager = StaticUtilities.getUIManager( 120*1000 );
					
					String details = MessageText.getString(
							"subscript.add.dup.desc",
							new String[]{ existing.getName()});
					
					ui_manager.showMessageBox(
							"subscript.add.dup.title",
							"!" + details + "!",
							UIManagerEvent.MT_OK );
					
						// we have a newer one, ignore
					
					return( existing );
					
				}else{
					
					UIManager ui_manager = StaticUtilities.getUIManager( 120*1000 );
	
					String details = MessageText.getString(
							"subscript.add.upgrade.desc",
							new String[]{ existing.getName()});
					
					long res = ui_manager.showMessageBox(
							"subscript.add.upgrade.title",
							"!" + details + "!",
							UIManagerEvent.MT_YES | UIManagerEvent.MT_NO );
					
					if ( res != UIManagerEvent.MT_YES ){	
					
						throw( new SubscriptionException( "User declined upgrade" ));
					}
					
					log( "Upgrading subscription: " + existing.getString());
	
					existing.upgrade( body );
					
					saveConfig();
					
					subscriptionUpdated();
					
					return( existing );
				}
			}else{
				
				SubscriptionImpl new_subs = new SubscriptionImpl( this, body );
	
				UIManager ui_manager = StaticUtilities.getUIManager( 120*1000 );
	
				String details = MessageText.getString(
						"subscript.add.desc",
						new String[]{ new_subs.getName()});
				
				long res = ui_manager.showMessageBox(
						"subscript.add.title",
						"!" + details + "!",
						UIManagerEvent.MT_YES | UIManagerEvent.MT_NO );
				
				if ( res != UIManagerEvent.MT_YES ){	
				
					throw( new SubscriptionException( "User declined addition" ));
				}
				
				log( "Imported new subscription: " + new_subs.getString());
				
				synchronized( this ){
					
					subscriptions.add( new_subs );
					
					saveConfig();
				}
				
				subscriptionAdded();
				
				return( new_subs );
			}
		}catch( SubscriptionException e ){
				
			throw( e );
				
		}catch( Throwable e ){
			
			throw( new SubscriptionException( "Subscription import failed", e ));
		}
	}
	
	public SubscriptionImpl
	getSubscription(
		byte[]		public_key )
	{
		synchronized( this ){
			
			for (int i=0;i<subscriptions.size();i++){
				
				SubscriptionImpl s = (SubscriptionImpl)subscriptions.get(i);
				
				if ( Arrays.equals( s.getPublicKey(), public_key )){
					
					return( s );
				}
			}
		}
		
		return( null );
	}
	
	protected File
	getVuzeFile(
		SubscriptionImpl 		subs )
	
		throws IOException
	{
 		File target = new File(SystemProperties.getUserPath());

 		target = new File( target, "subs" );
 		
 		if ( !target.exists()){
 			
 			if ( !target.mkdirs()){
 				
 				throw( new IOException( "Failed to create '" + target + "'" ));
 			}
 		}
 		
 		return( new File( target, ByteFormatter.encodeString( subs.getShortID()) + ".vuze" ));
	}
	
	public void
	lookupAssociations(
		byte[] 							hash,
		SubscriptionLookupListener		listener )
	
		throws SubscriptionException 
	{
		
	}
	
	protected void
	associationAdded()
	{
		if ( dht_plugin != null ){
			
			publishAssociations();
		}
	}
	
	protected void
	publishAssociations()
	{
		List	 shuffled_subs;

		synchronized( this ){
			
			if ( publish_associations_active ){
				
				return;
			}			
			
			shuffled_subs = new ArrayList( subscriptions );

			publish_associations_active = true;
		}
		
		boolean	publish_initiated = false;
		
		try{
			Collections.shuffle( shuffled_subs );
						
			for (int i=0;i<shuffled_subs.size();i++){
				
				SubscriptionImpl sub = (SubscriptionImpl)shuffled_subs.get( i );
				
				SubscriptionImpl.association  assoc = sub.getAssociationForPublish();
				
				if ( assoc != null ){
					
					publishAssociation( sub, assoc );
					
					publish_initiated = true;
					
					break;
				}
			}
		}finally{
			
			if ( !publish_initiated ){
				
				log( "Publishing Associations Complete" );
				
				synchronized( this ){

					publish_associations_active = false;
				}
			}
		}
	}
	
	protected void
	publishAssociation(
		final SubscriptionImpl					subs,
		final SubscriptionImpl.association		assoc )
	{
		log( "Checking association '" + subs.getString() + "' -> '" + assoc.getString() + "'" );
		
		byte[]	sub_id 		= subs.getShortID();
		int		sub_version	= subs.getVersion();
		
		byte[]	assoc_hash	= assoc.getHash();
		
		final String	key = "subscription:assoc:" + ByteFormatter.encodeString( assoc_hash ); 
				
		final byte[]	put_value = new byte[sub_id.length + 4];
		
		System.arraycopy( sub_id, 0, put_value, 4, sub_id.length );
		
		put_value[0]	= (byte)(sub_version>>16);
		put_value[1]	= (byte)(sub_version>>8);
		put_value[2]	= (byte)sub_version;
		put_value[3]	= (byte)subs.getFixedRandom();
		
		dht_plugin.get(
			key.getBytes(),
			"Subscription association read: " + ByteFormatter.encodeString( assoc_hash ),
			DHTPlugin.FLAG_SINGLE_VALUE,
			30,
			60*1000,
			false,
			false,
			new DHTPluginOperationListener()
			{
				private int	hits	= 0;
				private int	max_ver = 0;
				
				public void
				diversified()
				{
				}
				
				public void
				valueRead(
					DHTPluginContact	originator,
					DHTPluginValue		value )
				{
					byte[]	val = value.getValue();
					
					if ( val.length == put_value.length ){
						
						boolean	diff = false;
						
						for (int i=4;i<val.length;i++){
							
							if ( val[i] != put_value[i] ){
								
								diff = true;
								
								break;
							}
						}
						
						if ( !diff ){
							
							hits++;
							
							int	ver = ((val[0]<<16)&0xff0000) | ((val[1]<<8)&0xff00) | (val[2]&0xff);
							
							if ( ver > max_ver ){
								
								max_ver = ver;
							}
						}
					}
				}
				
				public void
				valueWritten(
					DHTPluginContact	target,
					DHTPluginValue		value )
				{
				}
				
				public void
				complete(
					byte[]				original_key,
					boolean				timeout_occurred )
				{
					if ( max_ver >= subs.getVersion()){
						
						updateSubscription( subs, max_ver );
					}
					
					if ( hits < 6 ){			
			
						log( "    Publishing association '" + subs.getString() + "' -> '" + assoc.getString() + "', existing=" + hits );

						dht_plugin.put(
							key.getBytes(),
							"Subscription association write: " + ByteFormatter.encodeString( assoc.getHash()) + " -> " + ByteFormatter.encodeString( subs.getShortID() ) + ":" + subs.getVersion(),
							put_value,
							DHTPlugin.FLAG_ANON,
							new DHTPluginOperationListener()
							{
								public void
								diversified()
								{
								}
								
								public void
								valueRead(
									DHTPluginContact	originator,
									DHTPluginValue		value )
								{
								}
								
								public void
								valueWritten(
									DHTPluginContact	target,
									DHTPluginValue		value )
								{
								}
								
								public void
								complete(
									byte[]				key,
									boolean				timeout_occurred )
								{
									log( "        completed '" + subs.getString() + "' -> '" + assoc.getString() + "'" );
				
									publishNext();
								}
							});
					}else{
						
						log( "    Not publishing association '" + subs.getString() + "' -> '" + assoc.getString() + "', existing =" + hits );

						publishNext();
					}
				}
				
				protected void
				publishNext()
				{
					synchronized( this ){
						
						publish_associations_active = false;
					}
					
					publishAssociations();
				}
			});
	}
	
	protected void
	subscriptionUpdated()
	{
		if ( dht_plugin != null ){
			
			publishSubscriptions();
		}
	}
	
	protected void
	subscriptionAdded()
	{
		if ( dht_plugin != null ){
			
			publishSubscriptions();
		}
	}
	
	protected void
	publishSubscriptions()
	{
		List	 shuffled_subs;

		synchronized( this ){
			
			if ( publish_subscription_active ){
				
				return;
			}			
			
			shuffled_subs = new ArrayList( subscriptions );

			publish_subscription_active = true;
		}
	
		boolean	publish_initiated = false;
		
		try{
			Collections.shuffle( shuffled_subs );
						
			for (int i=0;i<shuffled_subs.size();i++){
				
				SubscriptionImpl sub = (SubscriptionImpl)shuffled_subs.get( i );
				
				if ( !sub.getPublished()){
									
					sub.setPublished( true );
					
					publishSubscription( sub );
					
					publish_initiated = true;
					
					break;
				}
			}
		}finally{
			
			if ( !publish_initiated ){
				
				log( "Publishing Subscriptions Complete" );
				
				synchronized( this ){
	
					publish_subscription_active = false;
				}
			}
		}
	}
	
	protected void
	publishSubscription(
		final SubscriptionImpl					subs )
	{
		log( "Checking subscription '" + subs.getString() + "'" );
		
		byte[]	sub_id 		= subs.getShortID();
		int		sub_version	= subs.getVersion();
				
		final String	key = "subscription:pub:" + ByteFormatter.encodeString( sub_id ) + ":" + sub_version; 
						
		dht_plugin.get(
			key.getBytes(),
			"Subscription presence read: " + ByteFormatter.encodeString( sub_id ) + ":" + sub_version,
			DHTPlugin.FLAG_SINGLE_VALUE,
			12,
			60*1000,
			false,
			false,
			new DHTPluginOperationListener()
			{
				private int	hits	= 0;
				
				public void
				diversified()
				{
				}
				
				public void
				valueRead(
					DHTPluginContact	originator,
					DHTPluginValue		value )
				{
						// if we change this format we simply need to update the publish KEY rather than
						// worrying about versioning the VALUE
					
					byte[]	val = value.getValue();
										
					if ( subs.getVerifiedPublicationVersion( val ) == subs.getVersion()){
						
						hits++;
					}
				}
				
				public void
				valueWritten(
					DHTPluginContact	target,
					DHTPluginValue		value )
				{
				}
				
				public void
				complete(
					byte[]				original_key,
					boolean				timeout_occurred )
				{
					if ( hits < 6 ){			
			
						log( "    Publishing subscription '" + subs.getString() + ", existing=" + hits );

						byte[]	put_value = subs.getPublicationDetails();
						
						dht_plugin.put(
							key.getBytes(),
							"Subscription presence write: " + ByteFormatter.encodeString( subs.getShortID() ) + ":" + subs.getVersion(),
							put_value,
							DHTPlugin.FLAG_SINGLE_VALUE,
							new DHTPluginOperationListener()
							{
								public void
								diversified()
								{
								}
								
								public void
								valueRead(
									DHTPluginContact	originator,
									DHTPluginValue		value )
								{
								}
								
								public void
								valueWritten(
									DHTPluginContact	target,
									DHTPluginValue		value )
								{
								}
								
								public void
								complete(
									byte[]				key,
									boolean				timeout_occurred )
								{
									log( "        completed '" + subs.getString() + "'" );
				
									publishNext();
								}
							});
						
					}else{
						
						log( "    Not publishing subscription '" + subs.getString() + "', existing =" + hits );

						publishNext();
					}
				}
				
				protected void
				publishNext()
				{
					synchronized( this ){
						
						publish_subscription_active = false;
					}
					
					publishSubscriptions();
				}
			});
	}
	
	protected void
	updateSubscription(
		final SubscriptionImpl		subs,
		final int					new_version )
	{
		log( "Subscription " + subs.getString() + " - higher version found: " + new_version );
		
		if ( !subs.canAutoUpgradeCheck()){
			
			log( "    Checked too recently, ignoring" );
			
			return;
		}
		
		log( "Checking subscription '" + subs.getString() + "' upgrade to version " + new_version );
			
		byte[]	sub_id 		= subs.getShortID();
				
		final String	key = "subscription:pub:" + ByteFormatter.encodeString( sub_id ) + ":" + new_version; 
						
		dht_plugin.get(
			key.getBytes(),
			"Subscription presence read: " + ByteFormatter.encodeString( sub_id ) + ":" + new_version,
			DHTPlugin.FLAG_SINGLE_VALUE,
			12,
			60*1000,
			false,
			false,
			new DHTPluginOperationListener()
			{
				private byte[]	verified_hash;
				private int		verified_size;
				
				public void
				diversified()
				{
				}
				
				public void
				valueRead(
					DHTPluginContact	originator,
					DHTPluginValue		value )
				{
						// if we change this format we simply need to update the publish KEY rather than
						// worrying about versioning the VALUE
					
					byte[]	val = value.getValue();
											
					if ( 	verified_hash == null && 
							subs.getVerifiedPublicationVersion( val ) == new_version ){
						
						verified_hash 	= subs.getPublicationHash( val );
						verified_size	= subs.getPublicationSize( val );
					}
				}
				
				public void
				valueWritten(
					DHTPluginContact	target,
					DHTPluginValue		value )
				{
				}
				
				public void
				complete(
					byte[]				original_key,
					boolean				timeout_occurred )
				{
					if ( verified_hash != null ){			
			
						log( "    Subscription '" + subs.getString() + " upgrade verified as authentic" );

						updateSubscription( subs, verified_hash, verified_size );
						
					}else{
						
						log( "    Subscription '" + subs.getString() + " upgrade not verified" );
					}
				}
			});
	}
	
	protected void
	updateSubscription(
		SubscriptionImpl			subs,
		byte[]						update_hash,
		int							update_size )
	{
		log( "Subscription " + subs.getString() + " - update hash=" + ByteFormatter.encodeString( update_hash ) + ", size=" + update_size );

	}
	
	protected void
	loadConfig()
	{
		if ( !FileUtil.resilientConfigFileExists( CONFIG_FILE )){
			
			return;
		}
		
		log( "Loading configuration" );
		
		synchronized( this ){
			
			Map map = FileUtil.readResilientConfigFile( CONFIG_FILE );
			
			List	l_subs = (List)map.get( "subs" );
			
			if ( l_subs != null ){
				
				for (int i=0;i<l_subs.size();i++){
					
					Map	m = (Map)l_subs.get(i);
					
					try{
						SubscriptionImpl sub = new SubscriptionImpl( this, m );
						
						subscriptions.add( sub );
						
						log( "    loaded " + sub.getString());
						
					}catch( Throwable e ){
						
						log( "Failed to import subscription from " + m, e );
					}
				}
			}
		}
	}
	
	protected void
	configDirty()
	{
		synchronized( this ){
			
			if ( config_dirty ){
				
				return;
			}
			
			config_dirty = true;
		
			new DelayedEvent( 
				"Subscriptions:save", 5000,
				new AERunnable()
				{
					public void 
					runSupport() 
					{
						synchronized( this ){
							
							if ( !config_dirty ){

								return;
							}
							
							saveConfig();
						}	
					}
				});
		}
	}
	
	protected void
	saveConfig()
	{
		log( "Saving configuration" );
		
		synchronized( this ){
			
			config_dirty = false;
			
			if ( subscriptions.size() == 0 ){
				
				FileUtil.deleteResilientConfigFile( CONFIG_FILE );
				
			}else{
				
				Map map = new HashMap();
				
				List	l_subs = new ArrayList();
				
				map.put( "subs", l_subs );
				
				Iterator	it = subscriptions.iterator();
				
				while( it.hasNext()){
					
					SubscriptionImpl sub = (SubscriptionImpl)it.next();
						
					try{
						l_subs.add( sub.toMap());
						
					}catch( Throwable e ){
						
						log( "Failed to save subscription " + sub.getString(), e );
					}
				}
				
				FileUtil.writeResilientConfigFile( CONFIG_FILE, map );
			}
		}
	}
	
	protected synchronized AEDiagnosticsLogger
	getLogger()
	{
		if ( logger == null ){
			
			logger = AEDiagnostics.getLogger( LOGGER_NAME );
		}
		
		return( logger );
	}
	
	public void 
	log(
		String 		s,
		Throwable 	e )
	{
		AEDiagnosticsLogger diag_logger = getLogger();
		
		diag_logger.log( s );
		diag_logger.log( e );
	}
	
	public void 
	log(
		String 	s )
	{
		AEDiagnosticsLogger diag_logger = getLogger();
		
		diag_logger.log( s );
	}
}
