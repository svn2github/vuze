/*
 * Created on 16-Mar-2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.speedmanager.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Average;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.logging.LoggerChannel;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.clientmessageservice.ClientMessageService;
import com.aelitis.azureus.core.clientmessageservice.ClientMessageServiceClient;
import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.DHTFactory;
import com.aelitis.azureus.core.dht.DHTLogger;
import com.aelitis.azureus.core.dht.nat.DHTNATPuncherAdapter;
import com.aelitis.azureus.core.dht.transport.DHTTransportFactory;
import com.aelitis.azureus.core.dht.transport.udp.DHTTransportUDP;
import com.aelitis.azureus.core.speedmanager.SpeedManager;
import com.aelitis.azureus.core.speedmanager.SpeedManagerAdapter;

public class 
Test 
	implements SpeedManagerAdapter, DHTNATPuncherAdapter
{
	private Average upload_average = Average.getInstance( 1000, 10 );

	private int upload_limit;
	private int	upload_rate;
	
	protected 
	Test()
	{	
		try{
			AzureusCore	core = AzureusCoreFactory.create();
			
			final LoggerChannel logger = 
				core.getPluginManager().getDefaultPluginInterface().getLogger().getNullChannel("DHTChurner");

			DHTLogger	dht_logger = 
				new DHTLogger()
				{
					public void
					log(
						String	str )
					{
					}
				
					public void
					log(
						Throwable e )
					{
					}
					
					public void
					log(
						int		log_type,
						String	str )
					{
					}
				
					public boolean
					isEnabled(
						int	log_type )
					{
						return( true );
					}
						
					public PluginInterface
					getPluginInterface()
					{
						return( logger.getLogger().getPluginInterface());
					}
				};
				
			DHTTransportUDP	transport = 
				DHTTransportFactory.createUDP(
						DHTTransportUDP.PROTOCOL_VERSION_MAIN,
						DHT.NW_CVS,		// CVS network
						null,
						null,
						6881,
						4,
						2,
						30000, 
						50, 25,
						false,
						true,
						dht_logger );
		
			final DHT dht = DHTFactory.create( transport, new Properties(), null, this, dht_logger );
						
			transport.importContact(
					new InetSocketAddress( "dht.aelitis.com", 6881 ),
					DHTTransportUDP.PROTOCOL_VERSION_MAIN );
			
			new AEThread( "init", true )
			{
				public void
				runSupport()
				{
					dht.integrate(false);
				}
			}.start();
			
			SpeedManager	sm = core.getSpeedManager();
			
			sm.setSpeedTester( dht.getSpeedTester());
			
			sm.setEnabled( true );
			
			new AEThread( "init", true )
			{
				public void
				runSupport()
				{
					//upload();
				}
			}.start();
		
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}

	public int
	getCurrentProtocolUploadSpeed()
	{
		synchronized( upload_average ){
			
			return( 0 );
		}
	}	
	
	public int
	getCurrentDataUploadSpeed()
	{
		synchronized( upload_average ){
			
			return( (int)upload_average.getAverage());
		}
	}
	
	public void
	setCurrentUploadLimit(
		int		bytes_per_second )
	{
		upload_limit	= bytes_per_second;
	}

	public int
	getCurrentUploadLimit()
	{
		return( upload_limit );
	}
	
	protected void
	setManualUploadLimit(
		int		bytes_per_second )
	{
		upload_rate	= bytes_per_second;
	}
	
	protected void
	upload()
	{
		final int THREAD_NUM			= 10;
		final int SLEEP					= 500;
		final int OUTSTANDING_REPLIES	= 1;
				
		new Thread()
		{
			public void
			run()
			{
				while( true ){
					try{
						Thread.sleep(10000);
						
					}catch( Throwable e ){
						
						e.printStackTrace();
					}
					
					synchronized( upload_average ){
						
						System.out.println( "Upload: " + DisplayFormatters.formatByteCountToBase10KBEtcPerSec( upload_average.getAverage()));
					}
				}
			}
		}.start();
		
		for (int i=0;i<THREAD_NUM;i++){
			
			new Thread()
			{
				public void
				run()
				{
					while( true ){
						
						ClientMessageService	service = ClientMessageServiceClient.getServerService( "ae0.aelitis.com", 27021, 30, "DEVNULL" );
						
						int	rep_skip = OUTSTANDING_REPLIES;
						
						try{
							while( true ){
								
								int	my_upload = upload_rate/(THREAD_NUM*(1000/SLEEP));
								
								Map	request = new HashMap();
								
								byte[]	buffer = new byte[my_upload];
								
								synchronized( upload_average ){
									
									upload_average.addValue( my_upload );
								}
								
								request.put( "data", buffer );
								
								long	start = System.currentTimeMillis();
								
								if ( OUTSTANDING_REPLIES > 0 ){
									
									if ( rep_skip > 0 ){
										
										rep_skip--;
										
									}else{
										
										service.receiveMessage();
									}
								}
								
								service.sendMessage( request );
								
								if ( OUTSTANDING_REPLIES == 0 ){
									
									service.receiveMessage();
								}
								
								long	elapsed = System.currentTimeMillis() - start;
								
								// System.out.println( Thread.currentThread().getName() + ": sent " + my_upload + ", elapsed = " + elapsed );
								
								// service.close();
									
								long	sleep = SLEEP - elapsed;
								
								if ( sleep > 0 ){
									
									try{
										Thread.sleep(sleep);
										
									}catch( Throwable e ){
										
									}
								}
							}
						}catch( Throwable e ){
							
							e.printStackTrace();
						}
					}
				}
			}.start();
		}
	}
	
	public Map
	getClientData(
		InetSocketAddress	originator,
		Map					originator_data )
	{
		System.out.println( "getClientData - " + originator_data + "/" + originator );	

		Map	res = new HashMap();
		
		res.put( "udp_data_port", new Long( 1234 ));
		res.put( "tcp_data_port", new Long( 5678 ));
		
		return( res );
	}
	
	public static void
	main(
		String[]	args )
	{
		Test	test = new Test();
		
		BufferedReader in = new BufferedReader( new InputStreamReader( System.in ) );

		try{
			while( true ){
				
				String	line = in.readLine();
				
				if ( line != null ){
					
					line = line.trim().toLowerCase();
				}
				
				if ( line == null || line.equals("quit")){
					
					break;
				
				}else{

					try{
						test.setManualUploadLimit((int)(Float.parseFloat( line )*1024));
						
					}catch( Throwable e ){
					
						System.out.println( "Usage: quit, <uprate kb/sec>" );
					}
				}
			}
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
}
