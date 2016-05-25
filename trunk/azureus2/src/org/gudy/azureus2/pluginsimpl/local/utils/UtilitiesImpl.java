/*
 * File    : UtilitiesImpl.java
 * Created : 24-Mar-2004
 * By      : parg
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.pluginsimpl.local.utils;

/**
 * @author parg
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.*;

import org.gudy.azureus2.platform.PlatformManager;
import org.gudy.azureus2.platform.PlatformManagerCapabilities;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.ddb.DistributedDatabase;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.network.RateLimiter;
import org.gudy.azureus2.plugins.tag.Tag;
import org.gudy.azureus2.plugins.tag.TagManager;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.utils.*;
import org.gudy.azureus2.plugins.utils.ScriptProvider.ScriptProviderListener;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;
import org.gudy.azureus2.plugins.utils.resourceuploader.ResourceUploaderFactory;
import org.gudy.azureus2.plugins.utils.search.SearchException;
import org.gudy.azureus2.plugins.utils.search.SearchInitiator;
import org.gudy.azureus2.plugins.utils.search.SearchProvider;
import org.gudy.azureus2.plugins.utils.security.SESecurityManager;
import org.gudy.azureus2.plugins.utils.subscriptions.Subscription;
import org.gudy.azureus2.plugins.utils.subscriptions.SubscriptionException;
import org.gudy.azureus2.plugins.utils.subscriptions.SubscriptionManager;
import org.gudy.azureus2.plugins.utils.subscriptions.SubscriptionResult;
import org.gudy.azureus2.plugins.utils.xml.rss.RSSFeed;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentException;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentFactory;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.pluginsimpl.local.ddb.DDBaseImpl;
import org.gudy.azureus2.pluginsimpl.local.network.ConnectionManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.update.UpdateManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader.*;
import org.gudy.azureus2.pluginsimpl.local.utils.resourceuploader.ResourceUploaderFactoryImpl;
import org.gudy.azureus2.pluginsimpl.local.utils.security.*;
import org.gudy.azureus2.pluginsimpl.local.utils.xml.rss.RSSFeedImpl;
import org.gudy.azureus2.pluginsimpl.local.utils.xml.simpleparser.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.ipchecker.extipchecker.ExternalIPChecker;
import org.gudy.azureus2.core3.ipchecker.extipchecker.ExternalIPCheckerFactory;
import org.gudy.azureus2.core3.ipchecker.extipchecker.ExternalIPCheckerService;
import org.gudy.azureus2.core3.ipchecker.extipchecker.ExternalIPCheckerServiceListener;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AENetworkClassifier;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.IPToHostNameResolver;
import org.gudy.azureus2.core3.util.IPToHostNameResolverListener;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.Timer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.networkmanager.LimitedRateGroup;
import com.aelitis.azureus.core.proxy.AEProxyFactory;
import com.aelitis.azureus.core.proxy.AEProxyFactory.PluginProxy;
import com.aelitis.azureus.core.tag.TagManagerFactory;
import com.aelitis.azureus.core.tag.TagType;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.core.versioncheck.VersionCheckClient;

public class 
UtilitiesImpl
	implements Utilities, FeatureManager
{
	private static InetAddress		last_public_ip_address;
	private static long				last_public_ip_address_time;
	
	private AzureusCore				core;
	private PluginInterface			pi;
	
	private static ThreadLocal<PluginInterface>		tls	= 
		new ThreadLocal<PluginInterface>()
		{
			public PluginInterface
			initialValue()
			{
				return( null );
			}
		};
		
	private static ThreadLocal<Object[]>		verified_enablers_tls	= 
		new ThreadLocal<Object[]>()
		{
			public Object[]
			initialValue()
			{
				return( new Object[]{ 0L, 0, null });
			}
		};
			
	private static List<searchManager>		search_managers 	= new ArrayList<searchManager>();
	private static List<Object[]>			search_providers	= new ArrayList<Object[]>();
	
	private static CopyOnWriteList<Object[]>				feature_enablers 	= new CopyOnWriteList<Object[]>();
	private static CopyOnWriteList<FeatureManagerListener>	feature_listeners	= new CopyOnWriteList<FeatureManagerListener>();

	private static FeatureManagerListener 
		feature_listener = new FeatureManagerListener()
		{
			public void
			licenceAdded(
				Licence	licence )
			{
				checkFeatureCache();
				
				for ( FeatureManagerListener listener: feature_listeners ){
					
					try{
						listener.licenceAdded(licence);
						
					}catch( Throwable e ){
						
						Debug.out( e );
					}
				}				
			}
			
			public void
			licenceChanged(
				Licence	licence )
			{
				checkFeatureCache();
				
				for ( FeatureManagerListener listener: feature_listeners ){
					
					try{
						listener.licenceChanged(licence);
						
					}catch( Throwable e ){
						
						Debug.out( e );
					}
				}	
			}
			
			public void
			licenceRemoved(
				Licence	licence )
			{
				checkFeatureCache();
				
				for ( FeatureManagerListener listener: feature_listeners ){
					
					try{
						listener.licenceRemoved(licence);
						
					}catch( Throwable e ){
						
						Debug.out( e );
					}
				}				
			}
		};
	
			// need to use a consistent wrapped group as its object identity drives byte allocs...
		
	private static WeakHashMap<RateLimiter,PluginLimitedRateGroup>	limiter_map = new WeakHashMap<RateLimiter,PluginLimitedRateGroup>();

	
	private static CopyOnWriteList<LocationProviderListener>	lp_listeners 		= new CopyOnWriteList<LocationProviderListener>();
	private static CopyOnWriteList<LocationProvider>			location_providers 	= new CopyOnWriteList<LocationProvider>();
	
	private static CopyOnWriteList<ScriptProviderListener>	sp_listeners 		= new CopyOnWriteList<ScriptProviderListener>();
	private static CopyOnWriteList<ScriptProvider>			script_providers 	= new CopyOnWriteList<ScriptProvider>();

	
	public static PluginLimitedRateGroup
	wrapLimiter(
		RateLimiter						limiter,
		boolean							disable_disable )
	{
		synchronized( limiter_map ){
		
			PluginLimitedRateGroup l = limiter_map.get( limiter );
			
			if ( l == null ){
				
				l = new PluginLimitedRateGroup( limiter, disable_disable );
				
				limiter_map.put( limiter, l );
			}else{
				
				if ( l.isDisableDisable() != disable_disable ){
					
					Debug.out( "Inconsistent setting for disable_disable" );
				}
			}
			
			return( l );
		}
	}
	
	public static RateLimiter
	unwrapLmiter(
		PluginLimitedRateGroup l )
	{
		return( l.limiter );
	}
	
	private static void
	checkFeatureCache()
	{
		Set<String> features = new TreeSet<String>();
		
		List<FeatureEnabler>	enablers = getVerifiedEnablers();
		
		for ( FeatureEnabler enabler: enablers ){
			
			try{
				Licence[] licences = enabler.getLicences();
					
				for ( Licence licence: licences ){
					
					int	licence_state = licence.getState();
					
					if ( licence_state != Licence.LS_AUTHENTICATED ){
						
						continue;
					}
					
					FeatureDetails[] details = licence.getFeatures();
					
					for ( FeatureDetails detail: details ){
						
						if ( !detail.hasExpired()){
							
							features.add( detail.getID());
						}
					}
				}
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
		
		if ( !getFeaturesInstalled().equals( features )){
		
			String str = "";
			
			for ( String f: features ){
				
				str += (str.length()==0?"":",") + f;
			}
			
			COConfigurationManager.setParameter( "featman.cache.features.installed", str );
		}
	}
	
	public static Set<String>
	getFeaturesInstalled()
	{
		String str = COConfigurationManager.getStringParameter( "featman.cache.features.installed", "" );
		
		Set<String>	result = new TreeSet<String>();
		
		if ( str.length() > 0 ){
		
			result.addAll( Arrays.asList( str.split( "," )));
		}
		
		return( result );
	}
	
	public
	UtilitiesImpl(
		AzureusCore			_core,
		PluginInterface		_pi )
	{
		core	= _core;
		pi		= _pi;
	}
	
	public String
	getAzureusUserDir()
	{
		String	res = SystemProperties.getUserPath();
		
		if ( res.endsWith(File.separator )){
			
			res = res.substring(0,res.length()-1);
		}
		
		return( res );
	}
	
	/**
	 * @note Exactly the same as {@link UpdateManagerImpl#getInstallDir()}
	 */
	public String
	getAzureusProgramDir()
	{
		String	res = SystemProperties.getApplicationPath();
		
		if ( res.endsWith(File.separator )){
			
			res = res.substring(0,res.length()-1);
		}
		
		return( res );	
	}
	
	public boolean
	isWindows()
	{
		return( Constants.isWindows );
	}
	
	public boolean
	isLinux()
	{
		return( Constants.isLinux );
	}
	
	public boolean
	isUnix()
	{
		return( Constants.isUnix );
	}

	public boolean
	isFreeBSD()
	{
		return( Constants.isFreeBSD );
	}

	public boolean
	isSolaris()
	{
		return( Constants.isSolaris );
	}
	
	public boolean
	isOSX()
	{
		return( Constants.isOSX );
	}
	
	public boolean
	isCVSVersion()
	{
		return( Constants.isCVSVersion());
	}
	
	public InputStream
	getImageAsStream(
		String	image_name )
	{
		return( UtilitiesImpl.class.getClassLoader().getResourceAsStream("org/gudy/azureus2/ui/icons/" + image_name));
	}
	
	public Semaphore
	getSemaphore()
	{
		return( new SemaphoreImpl( pi ));
	}
  
    public Monitor getMonitor(){
      return new MonitorImpl( pi );
    }
    
	
	public ByteBuffer
	allocateDirectByteBuffer(
		int		size )
	{
		return( DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_EXTERNAL,size ).getBuffer(DirectByteBuffer.SS_EXTERNAL));
	}
	
	public void
	freeDirectByteBuffer(
		ByteBuffer	buffer )
	{
    
		//DirectByteBufferPool.freeBuffer( buffer );
	}
	
	public PooledByteBuffer
	allocatePooledByteBuffer(
		int		length )
	{
		return( new PooledByteBufferImpl( length ));
	}
	
	public PooledByteBuffer
	allocatePooledByteBuffer(
		byte[]		data )
	{
		return( new PooledByteBufferImpl( data ));
	}
	
	public PooledByteBuffer
	allocatePooledByteBuffer(
		Map		map )
	
		throws IOException
	{
		return( new PooledByteBufferImpl( BEncoder.encode( map )));
	}
	
	public Formatters
	getFormatters()
	{
		return( new FormattersImpl());
	}
	
	public LocaleUtilities
	getLocaleUtilities()
	{
		return( new LocaleUtilitiesImpl( pi ));
	}
	
	public UTTimer
	createTimer(
		String		name )
	{
		return( new UTTimerImpl( pi, name, false ));
	}
	
	public UTTimer
	createTimer(
		String		name,
		boolean		lightweight )
	{
		return( new UTTimerImpl( pi, name, lightweight ));
	}

	public UTTimer
	createTimer(
		String		name,
		int priority )
	{
		return( new UTTimerImpl( pi, name, priority ));
	}

	public void
	createThread(
		String			name,
		final Runnable	target )
	{
		AEThread2 t = 
			new AEThread2( pi.getPluginName() + "::" + name, true )
			{
				public void
				run()
				{
					callWithPluginThreadContext( pi, target );
				}
			};
			
		t.start();
	}
	
	public void
	createProcess(
		String		command_line )
	
		throws PluginException
	{
	    try{
	    		// we need to spawn without inheriting handles
	    	
	    	PlatformManager pm = PlatformManagerFactory.getPlatformManager();
	    	
	    	if ( pm.hasCapability( PlatformManagerCapabilities.CreateCommandLineProcess )){
	    		
	    		pm.createProcess( command_line, false );
	    		
	    		return;
	    	}
	    }catch( Throwable e ){
	    	
	        Debug.printStackTrace(e);
	    }
	    
	    try{
	       	Runtime.getRuntime().exec( command_line );
	       	
	    }catch( Throwable f ){
	        	
	    	throw( new PluginException("Failed to create process", f ));
	    }
	}
	
	public ResourceDownloaderFactory
	getResourceDownloaderFactory()
	{
		return( ResourceDownloaderFactoryImpl.getSingleton());
	}
	
	public ResourceUploaderFactory
	getResourceUploaderFactory()
	{
		return( ResourceUploaderFactoryImpl.getSingleton());
	}
	
	public SESecurityManager
	getSecurityManager()
	{
		return( new SESecurityManagerImpl( core ));
	}
	
	public SimpleXMLParserDocumentFactory
	getSimpleXMLParserDocumentFactory()
	{
		return( new SimpleXMLParserDocumentFactoryImpl());
	}
	
	public RSSFeed
	getRSSFeed(
		InputStream		is )
	
		throws SimpleXMLParserDocumentException
	{
		return( getRSSFeed( null, is ));
	}
	
	public RSSFeed
	getRSSFeed(
		URL				source_url,
		InputStream		is )
	
		throws SimpleXMLParserDocumentException
	{
		try{
			return( new RSSFeedImpl( this, source_url, is ));
			
		}finally{
			
			try{
				is.close();
				
			}catch( Throwable e ){
			}
		}
	}
	
	public RSSFeed
	getRSSFeed(
		URL		feed_location )
	
		throws ResourceDownloaderException, SimpleXMLParserDocumentException
	{
		String	feed_str	= feed_location.toExternalForm();
	
		String	lc_feed_str = feed_str.toLowerCase( Locale.US );
	
		ResourceDownloader	rd;
		
		PluginProxy plugin_proxy	= null;
		
		try{
			if ( lc_feed_str.startsWith( "tor:" )){
			
				String target_resource = feed_str.substring( 4 );
	
				try{
					feed_location = new URL( target_resource );
					
				}catch( MalformedURLException e ){
					
					throw( new ResourceDownloaderException( e ));
				}
				
				Map<String,Object>	options = new HashMap<String,Object>();
			
				options.put( AEProxyFactory.PO_PEER_NETWORKS, new String[]{ AENetworkClassifier.AT_TOR });
			
				plugin_proxy = 
					AEProxyFactory.getPluginProxy( 
						"RSS Feed download of '" + target_resource + "'",
						feed_location,
						options,
						true );
	
				if ( plugin_proxy == null ){
					
					throw( new ResourceDownloaderException( "No Tor plugin proxy available for '" + feed_str + "'" ));
				}
		
			
				rd = getResourceDownloaderFactory().create( plugin_proxy.getURL(), plugin_proxy.getProxy());		
	
				rd.setProperty( "URL_HOST", plugin_proxy.getURLHostRewrite() + (feed_location.getPort()==-1?"":(":" + feed_location.getPort())));
	
			}else{
				
				if ( AENetworkClassifier.categoriseAddress( feed_location.getHost()) != AENetworkClassifier.AT_PUBLIC ){
					
					plugin_proxy = 
							AEProxyFactory.getPluginProxy( 
								"RSS Feed download of '" + feed_location + "'",
								feed_location,
								true );
			
					if ( plugin_proxy == null ){
							
						throw( new ResourceDownloaderException( "No Plugin proxy available for '" + feed_str + "'" ));
					}
				
					
					rd = getResourceDownloaderFactory().create( plugin_proxy.getURL(), plugin_proxy.getProxy());		
			
					rd.setProperty( "URL_HOST", plugin_proxy.getURLHostRewrite() + (feed_location.getPort()==-1?"":(":" + feed_location.getPort())));

				}else{
				
					rd = getResourceDownloaderFactory().create( feed_location );
				}
			}
			
			return( getRSSFeed( feed_location, rd));
			
		}finally{
			
			if ( plugin_proxy != null ){
				
				plugin_proxy.setOK( true );
			}
		}
	}
	
	public RSSFeed
	getRSSFeed(
		ResourceDownloader	feed_location )
	
		throws ResourceDownloaderException, SimpleXMLParserDocumentException
	{
		return( getRSSFeed( null, feed_location ));
	}

	
	public RSSFeed
	getRSSFeed(
		URL					source_url,
		ResourceDownloader	feed_location )
	
		throws ResourceDownloaderException, SimpleXMLParserDocumentException
	{
		return( new RSSFeedImpl( this, source_url, feed_location ));
	}
	
	public InetAddress
	getPublicAddress(
		boolean	v6 )
	{
		if ( v6 ){
			
			String	vc_ip = VersionCheckClient.getSingleton().getExternalIpAddress( false, true );
			
			if ( vc_ip != null && vc_ip.length() > 0 ){
				
				try{
					return( InetAddress.getByName( vc_ip ));
					
				}catch( Throwable e ){
					
					Debug.printStackTrace( e );
				}
			}
			
			return( null );
		}else{
			
			return( getPublicAddress());
		}
	}
	
	public InetAddress
	getPublicAddress()
	{
		long	now = SystemTime.getCurrentTime();
		
		if ( now < last_public_ip_address_time ){
			
			last_public_ip_address_time	 = now;
			
		}else{
		
			if ( last_public_ip_address != null && now - last_public_ip_address_time < 15*60*1000 ){
				
				return( last_public_ip_address );
			}
		}
		
		InetAddress res	= null;
		
		try{
			
			String	vc_ip = VersionCheckClient.getSingleton().getExternalIpAddress( false, false );
			
			if ( vc_ip != null && vc_ip.length() > 0 ){
								
				res = InetAddress.getByName( vc_ip );
				
			}else{
			
				ExternalIPChecker	checker = ExternalIPCheckerFactory.create();
				
				ExternalIPCheckerService[]	services = checker.getServices();
				
				final String[]	ip = new String[]{ null };
				
				for (int i=0;i<services.length && ip[0] == null;i++){
					
					final ExternalIPCheckerService	service = services[i];
					
					if ( service.supportsCheck()){
	
						final AESemaphore	sem = new AESemaphore("Utilities:getExtIP");
						
						ExternalIPCheckerServiceListener	listener = 
							new ExternalIPCheckerServiceListener()
							{
								public void
								checkComplete(
									ExternalIPCheckerService	_service,
									String						_ip )
								{
									ip[0]	= _ip;
									
									sem.release();
								}
									
								public void
								checkFailed(
									ExternalIPCheckerService	_service,
									String						_reason )
								{
									sem.release();
								}
									
								public void
								reportProgress(
									ExternalIPCheckerService	_service,
									String						_message )
								{
								}
							};
							
						services[i].addListener( listener );
						
						try{
							
							services[i].initiateCheck( 60000 );
							
							sem.reserve( 60000 );
							
						}finally{
							
							services[i].removeListener( listener );
						}
					}
					
						
					if ( ip[0] != null ){
							
						res = InetAddress.getByName( ip[0] );
						
						break;
					}
				}
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
		}
		
		if ( res == null ){
			
				// if we failed then use any prior value if we've got one
			
			res	= last_public_ip_address;
			
		}else{
			
			last_public_ip_address		= res;
			
			last_public_ip_address_time	= now;
		}
		
		return( res );
	}
	
	public String
	reverseDNSLookup(
		InetAddress		address )
	{
		final AESemaphore	sem = new AESemaphore("Utilities:reverseDNS");
		
		final String[]	res = { null };
		
		IPToHostNameResolver.addResolverRequest(
					address.getHostAddress(),
					new IPToHostNameResolverListener()
					{
						public void 
						IPResolutionComplete(
							String 		result,
							boolean 	succeeded )
						{
							if ( succeeded ){
								
								res[0] = result;
							}

							sem.release();
						}
					});
		
		sem.reserve( 60000 );
		
		return( res[0] );
	}
  
  
  public long getCurrentSystemTime() {
    return SystemTime.getCurrentTime();
  }
  
	public ByteArrayWrapper
	createWrapper(
		byte[]		data )
	{
		return( new HashWrapper( data ));
	}
	
	public AggregatedDispatcher
	createAggregatedDispatcher(
		final long	idle_dispatch_time,
		final long	max_queue_size )
	{
		return( 
			new AggregatedDispatcher()
			{
				private AggregatedList	list = 
					createAggregatedList(
						new AggregatedListAcceptor()
						{
							public void
							accept(
								List		l )
							{
								for (int i=0;i<l.size();i++){
									
									try{
										((Runnable)l.get(i)).run();
										
									}catch( Throwable e ){
										
										Debug.printStackTrace(e);
									}
								}
							}
						},
						idle_dispatch_time,
						max_queue_size );
				
				public void
				add(
					Runnable	runnable )
				{
					list.add( runnable );
				}
				
				public Runnable
				remove(
					Runnable	runnable )
				{
					return((Runnable)list.remove( runnable ));
				}
				
				public void
				destroy()
				{
					list.destroy();
				}
			});
	}
	
	public AggregatedList
	createAggregatedList(
		final AggregatedListAcceptor	acceptor,
		final long						idle_dispatch_time,
		final long						max_queue_size )
	{
		return( 
			new AggregatedList()
			{
				AEMonitor	timer_mon	= new AEMonitor( "aggregatedList" );
				
				Timer		timer = new Timer( "AggregatedList" );
				TimerEvent	event;
				
				List		list	= new ArrayList();
				
				public void
				add(
					Object	obj )
				{
					
					List	dispatch_now = null;
					
					try{
						timer_mon.enter();
						
							// if the list is full kick off a dispatch and reset the list
						
						if (	max_queue_size > 0 &&
								max_queue_size	== list.size()){
							
							dispatch_now = list;
							
							list	= new ArrayList();
							
						}
							
						list.add( obj );
						
							// set up a timer to wakeup in required time period 
						
						long	now = SystemTime.getCurrentTime();
					
						if ( event != null ){
							
							event.cancel();
						}
						
						event = 
							timer.addEvent( 
									now + idle_dispatch_time,
									new TimerEventPerformer()
									{
										public void
										perform(
											TimerEvent	event )
										{
											dispatch();
										}
									});
					}finally{
						
						timer_mon.exit();
					}
					
					if ( dispatch_now != null ){
						
						dispatch( dispatch_now );
					}
				}

				public Object
				remove(
					Object	obj )
				{
					Object	res = null;
					
					try{
						timer_mon.enter();
					
						res = list.remove( obj )?obj:null;
							
						if ( res != null ){
							
							long	now = SystemTime.getCurrentTime();
							
							if ( event != null ){
								
								event.cancel();
							}
								
							if ( list.size() == 0 ){
								
								event	= null;
								
							}else{
								
								event = 
									timer.addEvent( 
											now + idle_dispatch_time,
											new TimerEventPerformer()
											{
												public void
												perform(
													TimerEvent	event )
												{
													dispatch();
												}
											});
							}
						}
					}finally{
						
						timer_mon.exit();
					}
					
					return( res );
				}
				
				protected void
				dispatch()
				{
					List	dispatch_list;
					
					try{
						timer_mon.enter();
					
						dispatch_list	= list;
						
						list	= new ArrayList();
						
					}finally{
						
						timer_mon.exit();
					}
					
					dispatch( dispatch_list );
				}

				protected void
				dispatch(
					List		l )
				{
					if ( l.size() > 0 ){
						
						try{
							acceptor.accept( l );
							
						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
						}
					}
				}
				
				public void
				destroy()
				{
					dispatch();
					
					timer.destroy();
				}
			});
	}
	
	public static final void
	callWithPluginThreadContext(
		PluginInterface				pi,
		Runnable					target )
	{
		PluginInterface existing = tls.get();
		
		try{
			tls.set( pi );
			
			target.run();
			
		}finally{
			
			tls.set( existing );
		}
	}
	
	public static final <T extends Exception> void
	callWithPluginThreadContext(
		PluginInterface					pi,
		runnableWithException<T>		target )
	
		throws T
	{
		PluginInterface existing = tls.get();
		
		try{
			tls.set( pi );
			
			target.run();
			
		}finally{
			
			tls.set( existing );
		}
	}
	
	public static final <T> T
	callWithPluginThreadContext(
		PluginInterface				pi,
		runnableWithReturn<T>			target )
	{
		PluginInterface existing = tls.get();
		
		try{
			tls.set( pi );
			
			return( target.run());
			
		}finally{
			
			tls.set( existing );
		}
	}
	
	public static final <T,S extends Exception> T
	callWithPluginThreadContext(
		PluginInterface							pi,
		runnableWithReturnAndException<T,S>		target )
	
		throws S
	{
		PluginInterface existing = tls.get();
		
		try{
			tls.set( pi );
			
			return( target.run());
			
		}finally{
			
			tls.set( existing );
		}
	}
	
	public static PluginInterface
	getPluginThreadContext()
	{
		return((PluginInterface)tls.get());
	}
	
	public Map
 	readResilientBEncodedFile(
 		File	parent_dir,
 		String	file_name,
 		boolean	use_backup )
	{
		return( FileUtil.readResilientFile( parent_dir, file_name, use_backup ));
	}
 	
	public void
 	writeResilientBEncodedFile(
 		File	parent_dir,
 		String	file_name,
 		Map		data,
 		boolean	use_backup )
	{
		FileUtil.writeResilientFile( parent_dir, file_name, data, use_backup );
	}

	public void
 	deleteResilientBEncodedFile(
 		File	parent_dir,
 		String	file_name,
 		boolean	use_backup )
	{
		FileUtil.deleteResilientFile( new File( parent_dir, file_name ));
	}
	
	public int compareVersions(String v1, String v2) {
		return Constants.compareVersions( v1, v2 );
	}
	
	public String normaliseFileName(String f_name) {
		return FileUtil.convertOSSpecificChars(f_name,false);
	}
	
	public DelayedTask createDelayedTask(Runnable target) {
		return addDelayedTask(pi.getPluginName(), target); 
	}
	
	private static List			delayed_tasks = new ArrayList();
	private static AESemaphore	delayed_tasks_sem	= new AESemaphore( "Utilities:delayedTask" );
	private static AEThread2	delayed_task_thread;
	
	public static DelayedTask addDelayedTask(String	name, Runnable r) {
		DelayedTaskImpl res = new DelayedTaskImpl(name);
		res.setTask(r);
		return res;
	}
	
	private static void
	queueTask(
		DelayedTaskImpl		task,
		int pos)
	{
		synchronized( delayed_tasks ){
			
			delayed_tasks.add( pos == -1 ? delayed_tasks.size() : pos, task );
			
			delayed_tasks_sem.release();
			
			if ( delayed_task_thread == null ){
				
				delayed_task_thread = 
					new AEThread2( "Utilities:delayedTask", true )
					{
						public void
						run()
						{
							try{
								PluginInitializer.addInitThread();
							
								while( true ){
									
									if ( !delayed_tasks_sem.reserve( 5*1000 )){
										
										synchronized( delayed_tasks ){
											
											if ( delayed_tasks.isEmpty()){
												
												delayed_task_thread	= null;
												
												break;
											}
										}
									}else{
										
										DelayedTaskImpl	task;
										
										synchronized( delayed_tasks ){
											
											task = (DelayedTaskImpl)delayed_tasks.remove(0);
										}
										
										//System.out.println( TimeFormatter.milliStamp() + ": Running delayed task: " + task.getName());
										
										task.run();
									}
								}
							}finally{
								
								PluginInitializer.removeInitThread();
							}
						}
					};
					
				delayed_task_thread.setPriority( Thread.MIN_PRIORITY );
				
				delayed_task_thread.start();
			}
		}
	}
	
	public void 
	registerSearchProvider(
		SearchProvider 		provider )
	
		throws SearchException
	{
		List<searchManager>	managers;
		
		synchronized( UtilitiesImpl.class ){
			
			search_providers.add( new Object[]{ pi, provider  });
			
			managers = new ArrayList<searchManager>( search_managers );
		}
		
		for (int i=0;i<managers.size();i++){
				
			((searchManager)managers.get(i)).addProvider( pi, provider );
		}
	}
	
	public void 
	unregisterSearchProvider(
		SearchProvider 		provider )
	
		throws SearchException
	{
		List<searchManager>	managers;
		
		synchronized( UtilitiesImpl.class ){
			
			Iterator<Object[]> it = search_providers.iterator();
			
			while( it.hasNext()){
				
				Object[] entry = it.next();
				
				if ( entry[0] == pi && entry[1] == provider ){
					
					it.remove();
				}
			}
			
			managers = new ArrayList<searchManager>( search_managers );
		}
		
		for (int i=0;i<managers.size();i++){
				
			((searchManager)managers.get(i)).removeProvider( pi, provider );
		}
	}
	
	public SearchInitiator 
	getSearchInitiator() 
	
		throws SearchException 
	{
		List<searchManager>	managers;
		
		synchronized( UtilitiesImpl.class ){
						
			managers = new ArrayList<searchManager>( search_managers );
		}
		
		if ( managers.size() == 0 ){
			
			throw( new SearchException( "No search managers registered - try later" ));
		}
		
		return( managers.get(0));
	}
	
	public static void
	addSearchManager(
		searchManager		manager )
	{
		List	providers;
		
		synchronized( UtilitiesImpl.class ){
			
			search_managers.add( manager );
			
			providers = new ArrayList( search_providers );
		}
				
		for (int i=0;i<providers.size();i++){
			
			Object[]	entry = (Object[])providers.get(i);
			
			manager.addProvider((PluginInterface)entry[0],(SearchProvider)entry[1]);
		}
	}
	
	public FeatureManager 
	getFeatureManager()
	{
		return( this );
	}
	
	public Licence[]
	createLicences(
		String[]				feature_ids )
	
		throws PluginException
	{
		List<FeatureEnabler>	enablers = getVerifiedEnablers();
		
		Throwable last_error = null;
		
		for ( FeatureEnabler enabler: enablers ){
			
			try{
				Licence[] licences = enabler.createLicences( feature_ids );
				
				if ( licences != null ){
					
					return( licences );
				}
			}catch( Throwable e ){
				
				Debug.out( e );
				
				last_error = e;
			}
		}
		
		if ( last_error == null ){
			
			throw( getLicenceException( "Failed to create licence" ));
			
		}else{
			
			throw( new PluginException( "Licence handler failed to create licence", last_error ));
		}
	}
	
	public Licence 
	addLicence(
		String licence_key ) 
	
		throws PluginException
	{
		List<FeatureEnabler>	enablers = getVerifiedEnablers();
		
		Throwable last_error = null;
		
		for ( FeatureEnabler enabler: enablers ){
			
			try{
				Licence licence = enabler.addLicence( licence_key );
				
				if ( licence != null ){
					
					return( licence );
				}
			}catch( Throwable e ){
		
				last_error = e;
				
				Debug.out( e );
			}
		}
		
		if ( last_error == null ){
		
			throw( getLicenceException( "Licence addition failed" ));
			
		}else{
			
			throw( new PluginException( "Licence handler failed to add licence", last_error ));
		}
	}
	
	private PluginException
	getLicenceException(
		String		str )
	{		
		try{
			String extra = "";

			PluginInterface fm_pi = core.getPluginManager().getPluginInterfaceByID( "aefeatman_v", false );
		
			
			if ( fm_pi == null || ( fm_pi.getPluginVersion() != null && fm_pi.getPluginVersion().equals( "0.0" ))){
				
				Download[] downloads = pi.getDownloadManager().getDownloads();
				
				Download hit = null;
				
				for ( Download download: downloads ){
					
					Torrent torrent = download.getTorrent();
					
					if ( torrent != null && torrent.isSimpleTorrent()){
						
						String name = torrent.getFiles()[0].getName();
						
						if ( name.startsWith( "aefeatman_v_") && name.endsWith( ".zip" )){
							
							hit = download;
							
							break;
						}
					}
				}
				
				if ( hit == null ){
				
					extra = "The 'Vuze Feature Manager' plugin is required but isn't installed";
					
				}else{
					
					int	state = hit.getState();
					
					if (	(state == Download.ST_STOPPED && !hit.isComplete() ) || 
							state == Download.ST_ERROR ){
						
						extra = "The 'Vuze Feature Manager' plugin has failed to download - check your Library's detailed view for errors or stopped downloads";
						
					}else{
						
						extra = "The 'Vuze Feature Manager' plugin is currently downloading, please wait for it to complete and install";
					}
				}
			}else{
				
				PluginState ps = fm_pi.getPluginState();
				
				if ( !ps.isLoadedAtStartup()){
					
					extra = "You need to set the 'Vuze Feature Manager' plugin to 'load at startup' in the plugin options";
					
				}else if ( ps.isDisabled()){
					
					extra = "The 'Vuze Feature Manager' plugin needs to be enabled";

				}else if ( !ps.isOperational()){
					
					extra = "The 'Vuze Feature Manager' plugin isn't operational";
				}
			}
			
			return( new PluginException( str + ": " + extra ));
			
		}catch( Throwable e ){
			
			return( new PluginException( str, e ));
		}
	}
	
	public Licence[] 
	getLicences() 
	{
		List<Licence> all_licences = new ArrayList<Licence>();
		
		List<FeatureEnabler>	enablers = getVerifiedEnablers();
		
		for ( FeatureEnabler enabler: enablers ){
			
			try{
				Licence[] licence = enabler.getLicences();
				
				all_licences.addAll( Arrays.asList( licence ));
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
		
		return( all_licences.toArray( new Licence[ all_licences.size()]));
	}
	
	public void
	refreshLicences()
	{
		List<FeatureEnabler>	enablers = getVerifiedEnablers();
		
		for ( FeatureEnabler enabler: enablers ){
			
			try{
				enabler.refreshLicences();
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
	}
	
	public FeatureDetails[]
 	getFeatureDetails(
 		String 					feature_id )
 	{
		return( getFeatureDetailsSupport( feature_id ));
 	}
	
	public boolean
	isFeatureInstalled(
		String					feature_id )
	{
		return( getVerifiedEnablers().size() > 0 && getFeaturesInstalled().contains( feature_id ));
	}
	
	private static FeatureDetails[]
	getFeatureDetailsSupport(
		String 					feature_id )
	{
		List<FeatureDetails>	result = new ArrayList<FeatureDetails>();
	
		List<FeatureEnabler>	enablers = getVerifiedEnablers();
			
		for ( FeatureEnabler enabler: enablers ){
			
			try{
				Licence[] licences = enabler.getLicences();
					
				for ( Licence licence: licences ){
					
					FeatureDetails[] details = licence.getFeatures();
					
					for ( FeatureDetails detail: details ){
						
						if ( detail.getID().equals( feature_id )){
							
							result.add( detail );
						}
					}
				}
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
		
		return( result.toArray( new FeatureDetails[ result.size() ]));
	}
	
   	public void
	addListener(
		FeatureManagerListener		listener )
	{
   		synchronized( feature_enablers ){
   			
   			feature_listeners.add( listener );
   		}
	}
   	
	public void
	removeListener(
		FeatureManagerListener		listener )
	{
 		synchronized( feature_enablers ){
   			
 			feature_listeners.remove( listener );
   		}
	}
	
	private static final List<FeatureEnabler>
	getVerifiedEnablers()
	{		
		long	now = SystemTime.getMonotonousTime();
		int		mut = feature_enablers.getMutationCount();
		
		Object[] cache = verified_enablers_tls.get();
		
		long	last_time 	= (Long)cache[0];
		int		old_mut		= (Integer)cache[1];
		
		if ( 	last_time != 0 && now - last_time < 30*1000 &&
				mut == old_mut ){
			
			return((List<FeatureEnabler>)cache[2]);
		}
		
		List<FeatureEnabler>	enablers = new ArrayList<FeatureEnabler>();

		for ( Object[] entry: feature_enablers ){
			
			PluginInterface enabler_pi 		= (PluginInterface)entry[0];
			Plugin			enabler_plugin 	= (Plugin)entry[1];
			FeatureEnabler	enabler 		= (FeatureEnabler)entry[2];
			
			if ( PluginInitializer.isVerified( enabler_pi, enabler_plugin )){
				
				if ( PluginInitializer.DISABLE_PLUGIN_VERIFICATION ){
					
					enablers.add( enabler );
					
				}else{
					
					File f1 = FileUtil.getJarFileFromClass( enabler_plugin.getClass());
					File f2 = FileUtil.getJarFileFromClass( enabler.getClass());
					
					if ( f1 != null && f1.equals( f2 )){
				
						enablers.add( enabler );
					}
				}
			}
		}
		
		verified_enablers_tls.set( new Object[]{ now, mut, enablers } );
		
		return( enablers );
	}
	
	public void
	registerFeatureEnabler(
		FeatureEnabler	enabler )
	{
		Plugin plugin = pi.getPlugin();
		
		if ( !PluginInitializer.isVerified( pi, plugin )){
				
			Debug.out( "Feature enabler not registered as plugin unverified" );
			
			return;
		}
		
		synchronized( feature_enablers ){
			
			feature_enablers.add( new Object[]{ pi, plugin, enabler });
			
			enabler.addListener( feature_listener );
		}
		
		checkFeatureCache();
	}
	
	public void
	unregisterFeatureEnabler(
		FeatureEnabler	enabler )
	{
		synchronized( feature_enablers ){
			
			for ( Object[] entry: feature_enablers ){
				
				if ( entry[2] == enabler ){
					
					feature_enablers.remove( entry );
					
					return;
				}
			}
		}
		
		checkFeatureCache();
	}
	
	public interface
	searchManager
		extends SearchInitiator
	{
		public void
		addProvider( 
			PluginInterface		pi,
			SearchProvider		provider );
		
		public void
		removeProvider( 
			PluginInterface		pi,
			SearchProvider		provider );
	}
		
	
	public SubscriptionManager 
	getSubscriptionManager() 
	
		throws SubscriptionException
	{
		try{
			Method m = Class.forName( "com.aelitis.azureus.core.subs.SubscriptionManagerFactory" ).getMethod( "getSingleton" );
			
			final PluginSubscriptionManager sm = (PluginSubscriptionManager)m.invoke( null );
			
			return( 
				new SubscriptionManager()
				{
					public void
					requestSubscription(
						URL		url )
					{
						sm.requestSubscription( url, new HashMap<String,Object>());
					}
					
					public void 
					requestSubscription(
						URL 					url,
						Map<String, Object> 	options ) 
					{
						sm.requestSubscription( url, options );
					}
					
					public void 
					requestSubscription(
						SearchProvider 			sp,
						Map<String, Object> 	search_parameters ) 
						
						throws SubscriptionException
					{
						sm.requestSubscription( sp, search_parameters );
					}
					
					public Subscription[] 
					getSubscriptions() 
					{
						PluginSubscription[] p_subs = sm.getSubscriptions( true );
						
						Subscription[]	subs = new Subscription[ p_subs.length ];
						
						for ( int i=0;i<subs.length;i++ ){
							
							final PluginSubscription p_sub = p_subs[i];
							
							subs[i] = 
								new Subscription()
								{
									public String 
									getID() 
									{
										return( p_sub.getID());
									}
									
									public String 
									getName() 
									{
										return( p_sub.getName());
									}
									
									public boolean 
									isSearchTemplate() 
									{
										return( p_sub.isSearchTemplate());
									}
									
									public SubscriptionResult[] 
									getResults() 
									{
										PluginSubscriptionResult[] p_results = p_sub.getResults( false );
										
										SubscriptionResult[] results = new SubscriptionResult[p_results.length];
										
										for (int i=0;i<results.length;i++){
											
											final PluginSubscriptionResult p_res = p_results[i];
											
											results[i] = 
												new SubscriptionResult()
												{
													private Map<Integer,Object> map = p_res.toPropertyMap();
													
													public Object
													getProperty(
														int		property_name )
													{
														return( map.get( property_name ));
													}
													
													public boolean
													isRead()
													{
														return( p_res.getRead());
													}
													
													public void
													setRead(
														boolean	read )
													{
														p_res.setRead( read );
													}
												};
										}
										
										return( results );
									}
								};
						}
					
						return( subs );
					}
				});
			
		}catch( Throwable e ){
			
			throw( new SubscriptionException( "Subscriptions unavailable", e ));
		}
	}
	
	public boolean 
	supportsPowerStateControl(
		int state ) 
	{
		if ( state == PowerManagementListener.ST_SLEEP ){
			
			return( PlatformManagerFactory.getPlatformManager().hasCapability( PlatformManagerCapabilities.PreventComputerSleep ));
		}
		
		return( false );
	}
	
	public void
	addPowerManagementListener(
		PowerManagementListener	listener )
	{
		core.addPowerManagementListener( listener );
	}
		
	public void
	removePowerManagementListener(
		PowerManagementListener	listener )
	{
		core.removePowerManagementListener( listener );
	}
	
	public List<LocationProvider>	
	getLocationProviders()
	{
		return( location_providers.getList());
	}
	
	public void
	addLocationProvider(
		LocationProvider	provider )
	{
		location_providers.add( provider );
		
		for ( LocationProviderListener l: lp_listeners ){
			
			try{
				l.locationProviderAdded( provider );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
	}
	
	public void
	removeLocationProvider(
		LocationProvider	provider )
	{
		location_providers.remove( provider );
		
		for ( LocationProviderListener l: lp_listeners ){
			
			try{
				l.locationProviderRemoved( provider );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
	}
	
	public void
	addLocationProviderListener(
		LocationProviderListener		listener )
	{
		lp_listeners.add( listener );
		
		for ( LocationProvider lp: location_providers ){
			
			listener.locationProviderAdded( lp );
		}
	}
	
	public void
	removeLocationProviderListener(
		LocationProviderListener		listener )
	{
		lp_listeners.remove( listener );
	}
	
	
		// script providers
	
	public List<ScriptProvider>	
	getScriptProviders()
	{
		return( script_providers.getList());
	}
	
	public void
	registerScriptProvider(
		ScriptProvider	provider )
	{
		script_providers.add( provider );
		
		for ( ScriptProviderListener l: sp_listeners ){
			
			try{
				l.scriptProviderAdded( provider );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
	}
	
	public void
	unregisterScriptProvider(
		ScriptProvider	provider )
	{
		script_providers.remove( provider );
		
		for ( ScriptProviderListener l: sp_listeners ){
			
			try{
				l.scriptProviderRemoved( provider );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
	}
	
	public void
	addScriptProviderListener(
		ScriptProviderListener		listener )
	{
		sp_listeners.add( listener );
		
		for ( ScriptProvider lp: script_providers ){
			
			listener.scriptProviderAdded( lp );
		}
	}
	
	public void
	removeScriptProviderListener(
		ScriptProviderListener		listener )
	{
		sp_listeners.remove( listener );
	}
	
	public Tag
	lookupTag(
		String		name )
	{
		List<TagType> tts = TagManagerFactory.getTagManager().getTagTypes();
		
		for ( TagType tt: tts ){
			
			Tag t = tt.getTag( name, true );
			
			if ( t != null ){
				
				return( t );
			}
		}
		
		return( null );
	}
	
	public List<DistributedDatabase>
	getDistributedDatabases(
		String[]		networks )
	{
		return( DDBaseImpl.getDDBs( networks, null ));
	}
	   
	public List<DistributedDatabase>
	getDistributedDatabases(
		String[]				networks,
		Map<String,Object>		options )
	{
		return( DDBaseImpl.getDDBs( networks, options ));
	}
	
	public interface
	PluginSubscriptionManager
	{
		public void
		requestSubscription(
			URL					url,
			Map<String,Object>	options );
		
		public void 
		requestSubscription(
			SearchProvider 			sp,
			Map<String, Object> 	search_parameters ) 
			
			throws SubscriptionException;
		
		public PluginSubscription[]
		getSubscriptions(
			boolean	subscribed_only );
	}
	
	public interface
	PluginSubscription
	{
		public String
		getID();
		
		public String
		getName();
		
		public boolean 
		isSearchTemplate();
		
		public PluginSubscriptionResult[]
		getResults(
			boolean		include_deleted );
	}
	
	public interface
	PluginSubscriptionResult
	{
		public Map<Integer,Object>
		toPropertyMap();
		
		public void
		setRead(
			boolean		read );
		
		public boolean
		getRead();
	}
	
	public interface
	runnableWithReturn<T>
	{
		public T
		run();
	}
	
	public interface
	runnableWithException<T extends Exception>
	{
		public void
		run()
		
			throws T;
	}
	
	public interface
	runnableWithReturnAndException<T,S extends Exception>
	{
		public T
		run()
		
			throws S;
	}
	
	public interface
	PluginLimitedRateGroupListener
	{
		public void
		disabledChanged(
			PluginLimitedRateGroup		group,
			boolean						is_disabled );
		
			/**
			 * Periodically called to allow sanity checks - shouldn't really be required
			 * @param group
			 * @param is_disabled
			 */
		
		public void
		sync(
			PluginLimitedRateGroup		group,
			boolean						is_disabled );
	}
	
	public static class
	PluginLimitedRateGroup
		implements LimitedRateGroup
	{
		private RateLimiter	limiter;
		
		private ConnectionManagerImpl.PluginRateLimiter plimiter;
		
		private CopyOnWriteList<PluginLimitedRateGroupListener>	listeners;
	
		/*
		 * For peer connections throttling up/down speed to zero to try and block upload/download has the
		 * unwanted effect of blocking protocol message flow and stalls the connection. the 'disable_disable'
		 * flag causes the rate limiter to inform listeners (peer connections) when flow should be disabled 
		 * at the protocol (as opposed to byte) level and at the same time leaves the byte flow unlimited
		 * to ensure the connection doesn't stall
		 */
		
		private final boolean	disable_disable;
		
		private boolean	current_disabled = false;
		
		private long	last_sync;
		
		private
		PluginLimitedRateGroup(
			RateLimiter		_limiter,
			boolean			_disable_disable )
		{
			limiter	= _limiter;
			
			disable_disable	= _disable_disable;
			
			if ( limiter instanceof ConnectionManagerImpl.PluginRateLimiter ){
				
				plimiter = (ConnectionManagerImpl.PluginRateLimiter)limiter;
			}
		}
		
		public boolean
		isDisableDisable()
		{
			return( disable_disable );
		}
		
		public void
		addListener(
			PluginLimitedRateGroupListener		listener )
		{
			if ( disable_disable ){
				
					// in case things have changed but not been triggered yet...
				
				getRateLimitBytesPerSecond();
				
				synchronized( this ){
					
					if ( listeners == null ){
						
						listeners = new CopyOnWriteList<UtilitiesImpl.PluginLimitedRateGroupListener>();
					}
					
					listeners.add( listener );
					
					if ( current_disabled ){
						
						try{
							listener.disabledChanged( this, true );
							
						}catch( Throwable e ){
							
							Debug.out( e );
						}
					}
				}
			}
		}
		
		public void
		removeListener(
			PluginLimitedRateGroupListener		listener )
		{
			if ( disable_disable ){
				
				synchronized( this ){
					
					if ( listeners != null ){
						
						if ( listeners.remove( listener )){
					
							if ( current_disabled ){
								
								try{
									listener.disabledChanged( this, false );
									
								}catch( Throwable e ){
									
									Debug.out( e );
								}
							}
						}
					}
				}
			}
		}
		
		public String 
		getName()
		{
			String name = limiter.getName();
			
			if ( Constants.IS_CVS_VERSION ){
				
				if ( disable_disable ){
					
					String str = "";
					
					if ( current_disabled ){
						
						str += "Disabled";
					}
					
					synchronized( this ){
						
						if ( listeners != null ){
							
							str += (str.length()==0?"":"/") + listeners.size();
						}
					}
					
					if ( str.length() > 0 ){
						
						name += " (" + str + ")";
					}
				}
			}
			
			return( name );
		}
		  
		public int 
		getRateLimitBytesPerSecond()
		{
			int	value = limiter.getRateLimitBytesPerSecond();
		
			if ( disable_disable ){
			
				boolean is_disabled = value == -1;
				
				if ( is_disabled != current_disabled ){
					
					synchronized( this ){
						
						current_disabled = is_disabled;
						
						if ( listeners != null ){
							
							for ( PluginLimitedRateGroupListener l: listeners ){
								
								try{
									l.disabledChanged( this, is_disabled );
									
								}catch( Throwable e ){
									
									Debug.out( e );
								}
							}
						}
					}
				}else{
					long now = SystemTime.getMonotonousTime();
					
					if ( now - last_sync > 60*1000 ){
						
						last_sync = now;
						
						synchronized( this ){
														
							if ( listeners != null ){
								
								for ( PluginLimitedRateGroupListener l: listeners ){
									
									try{
										l.sync( this, current_disabled );
										
									}catch( Throwable e ){
										
										Debug.out( e );
									}
								}
							}
						}
					}
				}
				 
				return( is_disabled?0:value );
				
			}else{
			 
				return( value );
			}
		}
		
		public boolean 
		isDisabled() 
		{
			return( limiter.getRateLimitBytesPerSecond() < 0 );
		}
		
		public void
		updateBytesUsed(
			int	used )
		{  
			if ( plimiter != null ){
				
				plimiter.updateBytesUsed( used );
			}
		}
	}
	
	static class
	DelayedTaskImpl
		implements DelayedTask
	{
		private String 		name;
		private Runnable	target;
		
		private long	create_time = SystemTime.getCurrentTime();
		private long	run_time;
		
		private
		DelayedTaskImpl(
			String		_name )
		{
			name 	= _name;
		}
		
		public void
		setTask(
			Runnable		_target )
		{
			target	= _target;
		}
		
		public void
		queue()
		{
			if ( target == null ){
				
				throw( new RuntimeException( "Target must be set before queueing" ));
			}	
			
			queueTask( this, -1 );
		}

		public void
		queueFirst()
		{
			if ( target == null ){
				
				throw( new RuntimeException( "Target must be set before queueing" ));
			}	
			
			queueTask( this, 0 );
		}

		protected void
		run()
		{
			try{
				run_time = SystemTime.getCurrentTime();
				
				target.run();
				
				long now = SystemTime.getCurrentTime();
				
				if (Logger.isEnabled()) {
		     		Logger.log(	
		     			new LogEvent(
		     				LogIDs.PLUGIN, LogEvent.LT_INFORMATION,
		     				"Delayed task '" + getName() + 
		     					"': queue_time=" + ( run_time - create_time ) +
		     					", exec_time=" + ( now - run_time )));
				}
			}catch( Throwable e ){
				
				Debug.out( "Initialisation task " + getName() + " failed to complete", e );
			}
		}
		
		protected String
		getName()
		{
			return( name + " (" + target.getClass() + ")" );
		}

	}
		
	private static Map<String,JSONServer>	json_servers = new HashMap<String,JSONServer>();
	private static  Map<String,JSONClient>	json_clients = new HashMap<String,JSONClient>();
	
	public void
	registerJSONRPCServer(
		final JSONServer		server )
	{
		String key = (pi==null?"default":pi.getPluginID()) + ":" + server.getName();
		
		synchronized( json_servers ){

			JSONServer existing = json_servers.get( key );
			
			if ( existing != null ){
				
				for ( JSONClient client: json_clients.values()){
					
					client.serverUnregistered( existing );
				}
			}
			
			json_servers.put( key, server );
			
			for ( JSONClient client: json_clients.values()){
				
				client.serverRegistered( server );
			}
		}
	}	
	
	public void
	unregisterJSONRPCServer(
		JSONServer		server )
	{
		String key = (pi==null?"default":pi.getPluginID()) + ":" + server.getName();
		
		synchronized( json_servers ){

			JSONServer existing = json_servers.remove( key );
			
			if ( existing != null ){
				
				for ( JSONClient client: json_clients.values()){
					
					client.serverUnregistered( existing );
				}
			}
		}
	}

	public void
	registerJSONRPCClient(
		JSONClient		client )
	{
		String key = pi==null?"default":pi.getPluginID();

		synchronized( json_servers ){

			json_clients.put( key, client );
			
			for ( JSONServer server: json_servers.values()){
				
				client.serverRegistered( server );
			}
		}
	}
	
	public void
	unregisterJSONRPCClient(
		JSONClient		client )
	{
		String key = pi==null?"default":pi.getPluginID();

		synchronized( json_servers ){

			json_clients.remove( key );
		}
	}
	
	private TagManagerImpl	tag_manager = new TagManagerImpl();
	
	public TagManager 
	getTagManager() 
	{
		return( tag_manager );
	}
	
	
	private static class
	TagManagerImpl
		implements TagManager
	{
		public List<Tag>
		getTags()
		{
			List<com.aelitis.azureus.core.tag.Tag> tags = TagManagerFactory.getTagManager().getTagType( TagType.TT_DOWNLOAD_MANUAL ).getTags();
			
			return( new ArrayList<Tag>( tags ));
		}
		
		public Tag 
		lookupTag(
			String name) 
		{
			return( TagManagerFactory.getTagManager().getTagType( TagType.TT_DOWNLOAD_MANUAL ).getTag( name, true ));
		}
		
		public Tag
		createTag(
			String		name )
		{
			try{
				return( TagManagerFactory.getTagManager().getTagType( TagType.TT_DOWNLOAD_MANUAL ).createTag(name, true ));
				
			}catch( Throwable e ){
				
				Debug.out( e );
				
				return( null );
			}
		}
	}
}
