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

import org.gudy.azureus2.core3.upnp.*;
import org.gudy.azureus2.core3.upnp.impl.ssdp.*;

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
	
	protected SSDPImpl	ssdp;
	
	protected Map		root_locations	= new HashMap();
	
	protected List		log_listeners	= new ArrayList();
	protected List		log_history		= new ArrayList();
	
	protected
	UPnPImpl()
	
		throws UPnPException
	{
		ssdp = new SSDPImpl( this );
		
		ssdp.addListener(this);
		
		ssdp.start();
	}
	
	public void
	rootDiscovered(
		String		location,
		String		usn,
		String		st )
	{
		if ( root_locations.get( location ) != null  ){
			
			return;
		}
		
		log( "UPnP: root = " + location + ", USN = " + usn );
		
		try{
			UPnPRootDevice	device = new UPnPRootDevice( this, location, usn );
		
			root_locations.put( location, device );
			
		}catch( UPnPException e ){
			
			log( e.toString());
		}
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
	
	public static void
	main(
		String[]		args )
	{
		try{
			UPnP	upnp = UPnPFactory.getSingleton();
				
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
			
			Thread.sleep(2000);
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
}
