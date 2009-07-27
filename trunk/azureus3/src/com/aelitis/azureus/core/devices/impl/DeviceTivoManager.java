/*
 * Created on Jul 24, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.core.devices.impl;

import java.util.*;
import java.io.IOException;
import java.net.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.RandomUtils;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.tracker.Tracker;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebContext;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageGenerator;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageRequest;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageResponse;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;

public class 
DeviceTivoManager 
{
	private static final String		LF				= "\n";
	private static final int		CONTROL_PORT	= 2190;
	
	private DeviceManagerImpl		device_manager;
	private PluginInterface			plugin_interface;
	
	private String	uid;
	private String	server_name	= "Vuze";
	private int		tcp_port;
	
	private TrackerWebContext twc;
	
	protected
	DeviceTivoManager(
		DeviceManagerImpl		_dm )
	{
		device_manager = _dm;

		AzureusCoreFactory.addCoreRunningListener(
			new AzureusCoreRunningListener() 
			{
				public void 
				azureusCoreRunning( 
					AzureusCore core )
				{
					init( core );
				}
			});
	}
	
	private void 
	init(
		AzureusCore azureus_core )
	{
		plugin_interface = azureus_core.getPluginManager().getDefaultPluginInterface();
		
		try{
			uid = COConfigurationManager.getStringParameter( "devices.tivo.uid", null );
			
			if ( uid == null ){
				
				byte[] bytes = new byte[8];
				
				RandomUtils.nextBytes( bytes );
				
				uid = Base32.encode( bytes );
				
				COConfigurationManager.setParameter( "devices.tivo.uid", uid );
			}
			
			try{
				String cn = PlatformManagerFactory.getPlatformManager().getComputerName();
				
				if ( cn != null && cn.length() > 0 ){
				
					server_name += " on " + cn;
				}
			}catch( Throwable e ){
			}
			
			twc = plugin_interface.getTracker().createWebContext( 0, Tracker.PR_HTTP );
			
			twc.addPageGenerator(
				new TrackerWebPageGenerator()
				{
					public boolean 
					generate(
						TrackerWebPageRequest 	request,
						TrackerWebPageResponse 	response )
					
						throws IOException 
					{
						String	id = (String)request.getHeaders().get( "tsn" );
						
						if ( id == null ){
							
							id = (String)request.getHeaders().get( "tivo_tcd_id" );	
						}
						
						if ( id != null ){
							
							DeviceTivo tivo = foundTiVo( request.getClientAddress2().getAddress(), id, "tivo.series3" );
							
							return( tivo.generate( request, response ));
						}
						
						return( false );
					}
				});
			
			tcp_port = twc.getURLs()[0].getPort();
			
			final DatagramSocket control_socket = new DatagramSocket( null );
				
			control_socket.setReuseAddress( true );
				
			try{
				control_socket.setSoTimeout( 60*1000 );
				
			}catch( Throwable e ){
				
				SimpleTimer.addPeriodicEvent(
					"Tivo:Beacon",
					60*1000,
					new TimerEventPerformer()
					{
						public void 
						perform(
							TimerEvent 	event )
						{
							sendBeacon( control_socket );
						}
					});
			}
			
			control_socket.bind( new InetSocketAddress((InetAddress)null, CONTROL_PORT ));
			
			sendBeacon( control_socket );
			
			new AEThread2( "TiVo:CtrlListener", true )
			{
				public void
				run()
				{
					long	successful_accepts 	= 0;
					long	failed_accepts		= 0;

					while(true){
						
						try{
							byte[] buf = new byte[8192];
							
							DatagramPacket packet = new DatagramPacket(buf, buf.length );
											
							control_socket.receive( packet );
								
							successful_accepts++;
							
							failed_accepts	 = 0;
							
							receiveBeacon( packet.getAddress(), packet.getData(), packet.getLength() );
							
						}catch( SocketTimeoutException e ){
							
							sendBeacon( control_socket );
							
						}catch( Throwable e ){
							
							failed_accepts++;
							
							log( "UDP receive on port " + CONTROL_PORT + " failed", e );

							if (( failed_accepts > 100 && successful_accepts == 0 ) || failed_accepts > 1000 ){
								
								log( "    too many failures, abandoning" );

								break;
							}
						}
					}				}
			}.start();
											
		}catch( Throwable e ){
		
			log( "Failed to establish listen on port " + CONTROL_PORT, e );
		}
	}
	
	protected void
	sendBeacon(
		DatagramSocket		socket )
	{
		String packet = 
			"tivoconnect=1" + LF +
			"swversion=1" + LF +	
			"method=broadcast" + LF +
			"identity=" + uid + LF +
			"machine=" + server_name + LF +
			"platform=pc" + LF +
			"services=TiVoMediaServer:" + tcp_port + "/http";
		
		try{
			byte[] 	bytes = packet.getBytes( "ISO-8859-1" );
			
			socket.send( new DatagramPacket( bytes, bytes.length, InetAddress.getByName( "255.255.255.255" ), CONTROL_PORT ));
			
		}catch( Throwable e ){
			
			log( "Failed to send beacon", e );
		}
	}
	
	protected void
	receiveBeacon(
		InetAddress	sender,
		byte[]		buffer,
		int			length )
	{
		try{
			String str = new String( buffer, 0, length, "ISO-8859-1" );
			
			String[]	lines = str.split( LF );
			
			Map<String,String>	map = new HashMap<String, String>();
			
			for (String line:lines ){
				
				int pos = line.indexOf( '=' );
				
				if ( pos > 0 ){
					
					map.put( line.substring( 0, pos ).trim().toLowerCase(), line.substring( pos+ 1 ).trim());
				}
			}
			
			String id = map.get( "identity" );
			
			if ( id == null || id.equals( uid )){
				
				return;
			}
			
			String platform = map.get( "platform" );
			
			if ( platform != null && platform.toLowerCase().startsWith( "tcd/")){
				
				String classification = "tivo." + platform.substring( 4 ).toLowerCase();
				
				foundTiVo( sender, id, classification );
			}
		}catch( Throwable e ){
			
			log( "Failed to decode beacon", e );
		}
	}
	
	protected DeviceTivo
	foundTiVo(
		InetAddress		address,
		String			uid,
		String			classification )
	{
		uid	= "tivo:" + uid;
		
		DeviceImpl[] devices = device_manager.getDevices();
				
		for ( DeviceImpl device: devices ){
			
			if ( device instanceof DeviceTivo ){
				
				DeviceTivo tivo = (DeviceTivo)device;
				
				if ( device.getID().equals( uid )){
				
					tivo.found( address, server_name );
					
					return( tivo );
				}
			}
		}
					
		DeviceTivo tivo = (DeviceTivo)device_manager.addDevice( new DeviceTivo( device_manager, uid, classification ));
		
		tivo.found( address, server_name );
		
		return( tivo );
	}
	
	protected void
	log(
		String		str )
	{
		if ( device_manager == null ){
			
			System.out.println( str );
			
		}else{
			
			device_manager.log( "TiVo: " + str );
		}
	}
	
	protected void
	log(
		String		str,
		Throwable 	e )
	{
		if ( device_manager == null ){
			
			System.out.println( str );
			
			e.printStackTrace();
			
		}else{
		
			device_manager.log( "TiVo: " + str, e );
		}
	}
}
