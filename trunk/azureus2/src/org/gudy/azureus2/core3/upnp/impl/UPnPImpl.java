/*
 * Created on 14-Jun-2004
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
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
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.core3.upnp.impl;

/**
 * @author parg
 *
 */

import java.util.*;
import java.net.*;
import java.io.*;

import org.gudy.azureus2.core3.util.*;

import org.gudy.azureus2.core3.upnp.*;
import org.gudy.azureus2.core3.upnp.services.UPnPWANConnectionPortMapping;
import org.gudy.azureus2.core3.upnp.services.UPnPWANIPConnection;
import org.gudy.azureus2.core3.xml.simpleparser.SimpleXMLParserDocument;
import org.gudy.azureus2.core3.xml.simpleparser.SimpleXMLParserDocumentException;
import org.gudy.azureus2.core3.xml.simpleparser.SimpleXMLParserDocumentFactory;

public class 
UPnPImpl
	implements UPnP, SSDPListener
{
	protected static UPnPImpl	singleton;
	
	public static synchronized UPnP
	getSingleton()
	
		throws UPnPException
	{
		if ( singleton == null ){
			
			singleton = new UPnPImpl();
		}
		
		return( singleton );
	}
	
	protected SSDP		ssdp;
	
	protected Map		root_locations	= new HashMap();
	
	protected List		log_listeners	= new ArrayList();
	protected List		log_history		= new ArrayList();
	
	protected List		rd_listeners	= new ArrayList();
	
	protected int		trace_index		= 0;
	
	
	protected
	UPnPImpl()
	
		throws UPnPException
	{
		ssdp = SSDPFactory.create( this );
		
		ssdp.addListener(this);
		
		ssdp.start();
	}
	
	public void
	rootDiscovered(
		InetAddress	local_address,
		String		location,
		String		usn,
		String		st )
	{
		if ( root_locations.get( location ) != null  ){
			
			return;
		}
		
		log( "UPnP: root = " + location + ", USN = " + usn +  ", local = " + local_address.toString() );
		
		try{
			UPnPRootDevice	root_device = UPnPDeviceFactory.createRootDevice( this, local_address, location, usn );
		
			List	listeners;
			
			synchronized( rd_listeners ){
				
				root_locations.put( location, root_device );

				listeners = new ArrayList( rd_listeners );
			}
			
			for (int i=0;i<listeners.size();i++){
				
				((UPnPRootDeviceListener)listeners.get(i)).rootDeviceFound( root_device.getDevice());
				
			}
		
		}catch( UPnPException e ){
			
			log( e.toString());
		}
	}
	
	public SimpleXMLParserDocument
	parseXML(
		InputStream		_is )
	
		throws SimpleXMLParserDocumentException, IOException
	{
			// ASSUME UTF-8
		
		ByteArrayOutputStream		baos = null;
		
		try{
			baos = new ByteArrayOutputStream(1024);
			
			byte[]	buffer = new byte[8192];
			
			while(true){
				
				int	len = _is.read( buffer );
				
				if ( len <= 0 ){
					
					break;
				}
				
				baos.write( buffer, 0, len );
			}
		}finally{
			
			baos.close();
		}
		
		byte[]	bytes_in = baos.toByteArray();
		
		InputStream	is = new ByteArrayInputStream( bytes_in );
		
		try{
			StringBuffer	data = new StringBuffer(1024);
			
			LineNumberReader	lnr = new LineNumberReader( new InputStreamReader( is, "UTF-8" ));
			
			while( true ){
				
				String	line = lnr.readLine();
				
				if ( line == null ){
					
					break;
				}
				
				data.append( line.trim() + "\n" );	
			}
					
			return( SimpleXMLParserDocumentFactory.create( data.toString()));
			
		}catch( Throwable e ){
			
			try{
				FileOutputStream	trace = new FileOutputStream( getTraceFile());
				
				trace.write( bytes_in );
				
				trace.close();
				
			}catch( Throwable f ){
				
				f.printStackTrace();
			}
			
			if ( e instanceof SimpleXMLParserDocumentException ){
				
				throw((SimpleXMLParserDocumentException)e);
			}
			
			throw( new SimpleXMLParserDocumentException(e ));
		}
	}
	
	protected synchronized File
	getTraceFile()
	{
		trace_index++;
		
		if ( trace_index == 6 ){
			
			trace_index = 1;
		}
		
		return( FileUtil.getUserFile( "upnp_trace" + trace_index + ".log" ));
	}
	
	public void
	log(
		Throwable e )
	{
		log( e.toString());
	}
	
	public void
	log(
		String	str )
	{
		List	old_listeners;
		
		synchronized( this ){

			old_listeners = new ArrayList(log_listeners);

			log_history.add( str );
			
			if ( log_history.size() > 32 ){
				
				log_history.remove(0);
			}
		}
		
		for (int i=0;i<old_listeners.size();i++){
	
			((UPnPLogListener)old_listeners.get(i)).log( str );
		}
	}
	
	public void
	addLogListener(
		UPnPLogListener	l )
	{
		List	old_logs;
		
		synchronized( this ){

			old_logs = new ArrayList(log_history);

			log_listeners.add( l );
		}
		
		for (int i=0;i<old_logs.size();i++){
			
			l.log((String)old_logs.get(i));
		}
	}
		
	public void
	removeLogListener(
		UPnPLogListener	l )
	{
		log_listeners.remove( l );
	}
	
	public void
	addRootDeviceListener(
		UPnPRootDeviceListener	l )
	{
		List	old_locations;
		
		synchronized( this ){

			old_locations = new ArrayList(root_locations.values());

			rd_listeners.add( l );
		}
		
		for (int i=0;i<old_locations.size();i++){
			
			l.rootDeviceFound(((UPnPRootDevice)old_locations.get(i)).getDevice());
		}
	}
		
	public void
	removeRootDeviceListener(
		UPnPRootDeviceListener	l )
	{
		rd_listeners.remove( l );
	}
	public static void
	main(
		String[]		args )
	{
		try{
			UPnP	upnp = UPnPFactory.getSingleton();
				
			upnp.addRootDeviceListener(
					new UPnPRootDeviceListener()
					{
						public void
						rootDeviceFound(
							UPnPDevice		device )
						{
							try{
								processDevice( device );
								
							}catch( Throwable e ){
								
								e.printStackTrace();
							}
						}						
					});
			
			upnp.addLogListener(
				new UPnPLogListener()
				{
					public void
					log(
						String	str )
					{
						System.out.println( str );
					}
				});
			
			Thread.sleep(20000);
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	protected static void
	processDevice(
		UPnPDevice	device )
	
		throws UPnPException
	{
		if ( device.getDeviceType().equalsIgnoreCase("urn:schemas-upnp-org:device:WANConnectionDevice:1")){
			
			System.out.println( "got device");
			
			UPnPService[] services = device.getServices();
			
			for (int i=0;i<services.length;i++){
				
				UPnPService	s = services[i];
				
				if ( s.getServiceType().equalsIgnoreCase( "urn:schemas-upnp-org:service:WANIPConnection:1")){
					
					System.out.println( "got service" );
					
					UPnPAction[]	actions = s.getActions();
					
					for (int j=0;j<actions.length;j++){
						
						System.out.println( actions[j].getName());
					}
					
					UPnPStateVariable[]	vars = s.getStateVariables();
					
					for (int j=0;j<vars.length;j++){
						
						System.out.println( vars[j].getName());
					}
					
					UPnPStateVariable noe = s.getStateVariable("PortMappingNumberOfEntries");
					
					System.out.println( "noe = " + noe.getValue());
					
					UPnPWANIPConnection wan_ip = (UPnPWANIPConnection)s.getSpecificService();
					
					UPnPWANConnectionPortMapping[] ports = wan_ip.getPortMappings();
					
					wan_ip.addPortMapping( true, 7007, "Moo!" );
	
					UPnPAction act	= s.getAction( "GetGenericPortMappingEntry" );
					
					UPnPActionInvocation inv = act.getInvocation();

					inv.addArgument( "NewPortMappingIndex", "0" );
					
					UPnPActionArgument[] outs = inv.invoke();
					
					for (int j=0;j<outs.length;j++){
						
						System.out.println( outs[j].getName() + " = " + outs[j].getValue());
					}
				}
			}
		}else{
			
			UPnPDevice[]	kids = device.getSubDevices();
			
			for (int i=0;i<kids.length;i++){
				
				processDevice( kids[i] );
			}
		}
	}
}
