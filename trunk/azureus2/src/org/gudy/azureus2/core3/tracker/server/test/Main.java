/*
 * File    : Main.java
 * Created : 5 Oct. 2003
 * By      : Parg 
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
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

package org.gudy.azureus2.core3.tracker.server.test;

//import java.net.*;

//import org.gudy.azureus2.core3.tracker.server.TRTrackerServerFactory;
//import org.gudy.azureus2.core3.tracker.protocol.udp.*;

import java.util.*;
import java.util.zip.*;
import java.net.*;
import java.io.*;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.plugins.tracker.*;
import org.gudy.azureus2.core3.util.*;

import org.gudy.azureus2.core3.security.SESecurityManager;
import org.gudy.azureus2.core3.tracker.server.impl.*;

public class 
Main
	implements Plugin, PluginListener
{
	static void
	usage()
	{
		System.err.println( "Usage:" );
		
		SESecurityManager.exitVM(1);
	}
	
	public static void
	main(
		String[]	args )
	{
		/*
		int	test_type= 0;
		
		if ( args.length != 0 ){
			
			usage();
		}
		
		
		try{
			int my_port 	= 6881;
			int their_port	= 6969;
			
			InetSocketAddress address = new InetSocketAddress("127.0.0.1",their_port);
			
			TRTrackerServerFactory.create( TRTrackerServerFactory.PR_UDP, their_port, false );
				
			PRUDPPacketHandler handler = PRUDPPacketHandlerFactory.getHandler( my_port );
			
			for (int i=0;i<100;i++){
				
				Thread.sleep(1000);
								
				PRUDPPacket request_packet = new PRUDPPacketRequestConnect();
				 
				PRUDPPacket reply_packet = handler.sendAndReceive( null, request_packet, address );
				
				System.out.println( reply_packet.getString());
			}
			
			Thread.sleep(100000);
			
		}catch( Throwable e ){
			
			e.printStackTrace();
			
		}
		*/
		
		
		// getSingleton();
		
		/*
		try{
			URL	tracker = new URL("http://192.168.0.2:6969/announce" );
			
		    String	info = "%9C%8B%B5%29%3B%26D8h%B1%BF%7C%E0%E9I%D9X%27%12c";
		    		
		    String  scrape = "http://127.0.0.1:6969/scrape?info_hash=%9C%8B%B5%29%3B%26D8h%B1%BF%7C%E0%E9I%D9X%27%12c";
		    
		    new Main().loadTest( tracker, info, scrape );
		    
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
		*/
		
		HashWrapper	hash = new HashWrapper("kjlklk".getBytes());
		
		TRTrackerServerTorrentImpl torrent = new TRTrackerServerTorrentImpl( hash );

		Runtime	rt= Runtime.getRuntime();
		
		System.out.println( "used = " + ( rt.totalMemory() - rt.freeMemory()));
		
		for (int x=0;x<1000;x++){
			for (int i=0;i<50000;i++){
				
				try{
					if ( i%1000 == 0 ){
					//	System.out.println(i);
					}
					torrent.peerContact("started", new HashWrapper( ("xxxxxxxxxxxxxxxx" + i).getBytes()), 1234, "xxxxxxx" + i, false,  "kkkkkkkk", 0, 0, 0, 100000 );
				}catch( Throwable e ){
					Debug.printStackTrace( e );
				}
			}
			
			rt.gc();
			
			System.out.println( "used = " + ( rt.totalMemory() - rt.freeMemory()));
		}
	}

	protected static AESemaphore			init_sem 	= new AESemaphore("TrackerServerTest");
	private static AEMonitor				class_mon	= new AEMonitor( "TrackerServerTest:class" );

	protected static Main		singleton;
			
	protected AEMonitor				this_mon	= new AEMonitor( "TrackerServerTest" );

	public static Main
	getSingleton()
	{
		try{
			class_mon.enter();
		
			if ( singleton == null ){
				
				new AEThread( "plugin initialiser ")
				{
					public void
					runSupport()
					{
						PluginManager.registerPlugin( Main.class );
		
						Properties props = new Properties();
						
						props.put( PluginManager.PR_MULTI_INSTANCE, "true" );
						
						PluginManager.startAzureus( PluginManager.UI_SWT, props );
					}
				}.start();
			
				init_sem.reserve();
			}
			
			return( singleton );
			
		}finally{
			
			class_mon.exit();
		}
	}	
	
	protected PluginInterface		plugin_interface;
	
	public void 
	initialize(
		PluginInterface _pi )
	{	
		plugin_interface = _pi;
		
		plugin_interface.getPluginProperties().setProperty( "plugin.name", "Tracker Server Tester" );
		
		singleton = this;
		
		init_sem.release();
		
		LoggerChannel log = plugin_interface.getLogger().getChannel("Plugin Test");
		
		log.log(LoggerChannel.LT_INFORMATION, "Plugin Initialised");
		
		plugin_interface.addListener( this );
	}
	
	public void
	initializationComplete()
	{
		new Thread()
		{
			public void
			run()
			{
				
				plugin_interface.getTracker().addListener(
					new TrackerListener()
					{
						public void
						torrentAdded(
							TrackerTorrent	torrent )
						{
							processTorrent( torrent );
						}
						
						public void
						torrentChanged(
							TrackerTorrent	torrent )
						{			
						}
						
						public void
						torrentRemoved(
							TrackerTorrent	torrent )
						{	
						}
					});
			}
		}.start();
	}

	long	count 			= 0;
	long	peer_id_next	= 2000000000; 
	long	ip_address_next	= 2000000000;
	
	long	now 	= System.currentTimeMillis();
	
	
	final int		num_want_base = 50;
	
	final String	event="started";
			
	final boolean	random_want	= true;
	
	final boolean	do_scrape	= false;
	final boolean	mix			= false;	

	protected void
	processTorrent(
		TrackerTorrent	torrent )
	{
		
		Tracker tracker = plugin_interface.getTracker();
		
		final URL	tracker_url = tracker.getURLs()[0];
		
		String	t_info_hash = null;
		
		try{
			t_info_hash = URLEncoder.encode( new String( torrent.getTorrent().getHash(), "ISO-8859-1"), "ISO-8859-1");
		
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
		}
		
		final String	info_hash	= t_info_hash;
		
		final String url_scrape 	= "http://127.0.0.1:" + tracker_url.getPort() + "/scrape?info_hash=" + info_hash;
	
		loadTest( tracker_url, info_hash, url_scrape );
	}
	
	protected void
	loadTest(
		final URL			tracker_url,
		final String		info_hash,
		final String		url_scrape )
	{
		System.out.println( "starting load test" );
		
		System.out.println( "    tracker = " + tracker_url.toString());
		System.out.println( "    info = " + info_hash );
		System.out.println( "    scrape = " + url_scrape );
		
		for (int i=0;i<6;i++){
			
			new Thread()
			{
				public void
				run()
				{
					for (int j=0;j<10000;j++){
						
						long	peer_id;
						long	address;
						
						int	num_want = (int)(num_want_base + (Math.random()*(num_want_base/2)));
							
						String url_start	= "http://127.0.0.1:" + tracker_url.getPort() + "/announce?info_hash=" + info_hash + "&peer_id=-AZ2103-aa";
						String url_end		= "&port=6881&uploaded=0&downloaded=0&left=10&event="+event+"&numwant=" +num_want + "&ip=IP";
												
						try{
							this_mon.enter();
							
							count++;
							
							if (count%100==0){
								
								System.out.println(count + ": " + (100000/(System.currentTimeMillis() - now)));
								
								now	= System.currentTimeMillis();
							}
							
							peer_id = peer_id_next++;
							address = ip_address_next++;
							
						}finally{
							
							this_mon.exit();
						}
							
						boolean	did_scrape;
						
						try{
							Thread.sleep(50);
							
							String	url_str;
							
							if (mix ){
								
								boolean b = count%2==1;
						
								did_scrape	= b;
								
								url_str= b?url_scrape :url_start + peer_id + url_end + address;
								
							}else{
								
								did_scrape = do_scrape;
								
								url_str= do_scrape?url_scrape :url_start + peer_id + url_end + address;
							}
							
							URL	url = new URL( url_str );
							
							HttpURLConnection con = (HttpURLConnection)url.openConnection();
							
							con.addRequestProperty("Accept-Encoding","gzip");
							
							con.setDoInput( true );
							
							con.connect();
							
							InputStream	is = new GZIPInputStream( con.getInputStream());		
							
							byte[] data = new byte[1024];
	
							int	total = 0;
							
							String	str = "";
							
							while ( true ){
								
								int	len = is.read(data );
								
								//Thread.sleep(100);
								
								if ( len > 0 ){
							
									str += new String(data,0,len);
									
									total += len;
									
								}else if ( len == 0 ){
									
									Thread.sleep(20);
									
								}else{
									
									break;
								}							
							}							
							
							
							if (did_scrape){
								
								if (str.indexOf("files") == -1){
									
									System.out.println("scrape_fail:" + str );
								}
							}else{
								
								if ( str.indexOf( "peer id" )== -1 ){
									System.out.println( "->" + total + "-" +str  );
								}
							}
							
							is.close();
							
						}catch( Throwable e ){
							
							Debug.printStackTrace( e );
						}
					}
				}
			}.start();
		}
	}
	
	public void
	closedownInitiated()
	{	
	}
	
	public void
	closedownComplete()
	{	
	}
}
