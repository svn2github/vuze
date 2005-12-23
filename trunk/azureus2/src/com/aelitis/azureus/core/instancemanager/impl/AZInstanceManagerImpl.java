/*
 * Created on 20-Dec-2005
 * Created by Paul Gardner
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.instancemanager.impl;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.*;

import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.utils.UTTimer;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreLifecycleAdapter;
import com.aelitis.azureus.core.instancemanager.AZInstance;
import com.aelitis.azureus.core.instancemanager.AZInstanceManager;
import com.aelitis.azureus.core.instancemanager.AZInstanceManagerListener;
import com.aelitis.net.upnp.UPnPFactory;
import com.aelitis.net.upnp.UPnPSSDP;
import com.aelitis.net.upnp.UPnPSSDPAdapter;
import com.aelitis.net.upnp.UPnPSSDPListener;

public class 
AZInstanceManagerImpl 
	implements AZInstanceManager, UPnPSSDPListener
{
	private static final LogIDs LOGID = LogIDs.NET;
	
	private String				SSDP_GROUP_ADDRESS 	= "239.255.068.250";	// 239.255.000.000-239.255.255.255 
	private int					SSDP_GROUP_PORT		= 16680;				//
	private int					SSDP_CONTROL_PORT	= 16679;

	private static final long	ALIVE_PERIOD	= 30*60*1000;
	
	private static AZInstanceManagerImpl	singleton;
	
	private List	listeners	= new ArrayList();
	
	private static AEMonitor	class_mon = new AEMonitor( "AZInstanceManager:class" );
	
	public static AZInstanceManager
	getSingleton(
		AzureusCore	core )
	{
		try{
			class_mon.enter();
			
			if ( singleton == null ){
				
				singleton = new AZInstanceManagerImpl( core );
			}
		}finally{
			
			class_mon.exit();
		}
		
		return( singleton );
	}
	
	private AzureusCore	core;
	private UPnPSSDP 	ssdp;
	private long		search_id_next;
	private List		requests = new ArrayList();
	
	private AZMyInstanceImpl		my_instance;
	private Map						other_instances	= new HashMap();
	
	private AESemaphore	initial_search_sem	= new AESemaphore( "AZInstanceManager:initialSearch" );
	
	private AEMonitor	this_mon = new AEMonitor( "AZInstanceManager" );

	protected
	AZInstanceManagerImpl(
		AzureusCore	_core )
	{
		core			= _core;
		
		my_instance	= new AZMyInstanceImpl( core );
	}
	
	public void
	initialize()
	{
		final PluginInterface	pi = core.getPluginManager().getDefaultPluginInterface();
		
		try{
			ssdp = 
				UPnPFactory.getSSDP( 
					new UPnPSSDPAdapter()
					{
						public UTTimer
						createTimer(
							String	name )
						{
							return( pi.getUtilities().createTimer( name ));
						}
		
						public void
						createThread(
							String		name,
							AERunnable	runnable )
						{
							pi.getUtilities().createThread( name, runnable );
						}
						
						public void
						trace(
							Throwable	e )
						{
							Debug.printStackTrace( e );
							
							Logger.log(new LogEvent(LOGID, "SSDP: failed ", e)); 

						}
						
						public void
						trace(
							String	str )
						{
							if ( Logger.isEnabled()){
								
								Logger.log(new LogEvent( LOGID, str )); 
							}
						}
					},
					SSDP_GROUP_ADDRESS,
					SSDP_GROUP_PORT,
					SSDP_CONTROL_PORT );
			
			ssdp.addListener( this );
		
			core.addLifecycleListener(
				new AzureusCoreLifecycleAdapter()
				{
					public void
					stopping(
						AzureusCore		core )
					{
						if ( other_instances.size() > 0 ){
							
							ssdp.notify( my_instance.encode(), "ssdp:byebye" );
						}
					}
				});
			
			SimpleTimer.addPeriodicEvent(
				ALIVE_PERIOD,
				new TimerEventPerformer()
				{
					public void
					perform(
						TimerEvent	event )
					{
						checkTimeouts();
						
						if ( other_instances.size() > 0 ){
							
							ssdp.notify( my_instance.encode(), "ssdp:alive" );
						}				
					}
				});
		
		}catch( Throwable e ){
			
			initial_search_sem.releaseForever();
			
			Debug.printStackTrace(e);
		}
		
		new AEThread( "AZInstanceManager:initialSearch", true )
		{
			public void
			runSupport()
			{
				try{
					search();
					
				}finally{
					
					initial_search_sem.releaseForever();
				}
			}
		}.start();
	}
	
	public void
	receivedResult(
		NetworkInterface	network_interface,
		InetAddress			local_address,
		InetAddress			originator,
		URL					location,
		String				ST,
		String				AL )
	{
		// System.out.println( "received result: " + ST + "/" + AL );

		if ( ST.startsWith("azureus:") && AL != null ){
			
			StringTokenizer	tok = new StringTokenizer( ST, ":" );
			
			tok.nextToken();	// az
			
			tok.nextToken();	// command 

			String	az_id = tok.nextToken();	// az id
			
			if ( az_id.equals( my_instance.getID())){
			
				long search_id = Long.parseLong( tok.nextToken());	// search id
				
				try{
					this_mon.enter();
					
					for (int i=0;i<requests.size();i++){
						
						request	req = (request)requests.get(i);
						
						if ( req.getID() == search_id ){
							
							req.addReply( originator, AL );
						}
					}
				}finally{
					
					this_mon.exit();
				}
			}
		}
	}
	
	public String
	receivedSearch(
		NetworkInterface	network_interface,
		InetAddress			local_address,
		InetAddress			originator,
		String				user_agent,
		String				ST )
	{
		// System.out.println( "received search: " + user_agent + "/" + ST );
		
		if ( user_agent.startsWith("azureus:")){
			
			AZInstance	inst = AZOtherInstanceImpl.decode( originator, user_agent );
			
			if ( inst != null ){
				
				checkAdd( inst );
			}
		}
		
		if ( ST.startsWith("azureus:")){
			
			StringTokenizer	tok = new StringTokenizer( ST, ":" );
			
			tok.nextToken();	// az
			
			String	command = tok.nextToken();

			String az_id = tok.nextToken();	// az id
			
			if ( !az_id.equals( my_instance.getID())){
								
				String	reply = my_instance.encode();
	
				if ( command.equals("btih")){
					
					
				}else if ( command.equals( "instance" )){
					
					return( reply );
				}
			}
		}
		
		return( null );
	}
	
	public void
	receivedNotify(
		NetworkInterface	network_interface,
		InetAddress			local_address,
		InetAddress			originator,
		URL					location,
		String				NT,
		String				NTS )
	{
		// System.out.println( "received notify: " + NT + "/" + NTS );

		if ( NT.startsWith("azureus:")){
			
			AZInstanceImpl	inst = AZOtherInstanceImpl.decode( originator, NT );

			if ( inst != null ){
			
				if ( NTS.indexOf("alive") != -1 ){

					checkAdd( inst );
					
				}else if ( NTS.indexOf("byebye") != -1 ){

					checkRemove( inst );
				}
			}
		}
	}

	protected void
	checkAdd(
		AZInstance	inst )
	{
		if ( inst.getID().equals( my_instance.getID())){
			
			return;
		}
		
		boolean	added = false;
		
		try{
			this_mon.enter();
			
			added = other_instances.put( inst.getID(), inst ) == null;
			
		}finally{
			
			this_mon.exit();
		}
		
		if ( added ){
			
			informAdded( inst );
		}
	}
	
	protected void
	checkRemove(
		AZInstance	inst )
	{
		if ( inst.getID().equals( my_instance.getID())){
			
			return;
		}
		
		boolean	removed = false;
		
		try{
			this_mon.enter();
			
			removed = other_instances.remove( inst.getID()) != null;
			
		}finally{
			
			this_mon.exit();
		}
		
		if ( removed ){
			
			informRemoved( inst );
		}
	}
	
	public AZInstance
	getMyInstance()
	{
		return( my_instance );
	}
	
	protected void
	search()
	{
		request req = sendRequest( "instance" );
		
		req.getReply();
	}
	
	public AZInstance[]
	getOtherInstances()
	{
		initial_search_sem.reserve();
		
		try{
			this_mon.enter();

			return((AZInstance[])other_instances.values().toArray( new AZInstance[other_instances.size()]));
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public AZInstance[]
	getInstancesForTorrent(
		TOTorrent	torrent )
	{
		if ( ssdp == null ){
			
			return( new AZInstance[0]);
		}
		
		request req = sendRequest( "btih" );
		
		return( req.getReply());
	}
	
	protected void
	checkTimeouts()
	{
		long	now = SystemTime.getCurrentTime();
	
		List	removed = new ArrayList();
		
		try{
			this_mon.enter();

			Iterator	it = other_instances.values().iterator();
			
			while( it.hasNext()){
				
				AZInstanceImpl	inst = (AZInstanceImpl)it.next();
	
				if ( now - inst.getCreationTime() > ALIVE_PERIOD * 2.5 ){
					
					removed.add( inst );
					
					it.remove();
				}
			}
		}finally{
			
			this_mon.exit();
		}
		
		for (int i=0;i<removed.size();i++){
			
			AZInstance	inst = (AZInstance)removed.get(i);
			
			informRemoved( inst );
		}
	}
	
	protected void
	informRemoved(
		AZInstance	inst )
	{
		for (int i=0;i<listeners.size();i++){
			
			try{
				((AZInstanceManagerListener)listeners.get(i)).instanceLost( inst );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
	protected void
	informAdded(
		AZInstance	inst )
	{
		for (int i=0;i<listeners.size();i++){
			
			try{
				((AZInstanceManagerListener)listeners.get(i)).instanceFound( inst );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
	protected request
	sendRequest(
		String	type )
	{
		return( new request( type ));
	}
	
	protected class
	request
	{
		private long	id;
		
		private List	replies = new ArrayList();
		
		protected
		request(
			String		_type )
		{
			try{
				this_mon.enter();

				id	= search_id_next++;
						
				requests.add( this );
	
			}finally{
				
				this_mon.exit();
			}
			
			String	st = "azureus:" + _type + ":" + my_instance.getID() + ":" + id;
			
			ssdp.search( my_instance.encode(), st );
		}
		
		protected long
		getID()
		{
			return( id );
		}
		
		protected void
		addReply(
			InetAddress	internal_address,
			String		AL )
		{
			AZInstanceImpl	inst = AZOtherInstanceImpl.decode( internal_address, AL );
			
			if ( inst != null ){
				
				boolean	added = false;
				
				try{
					this_mon.enter();
				
					for (int i=0;i<replies.size();i++){
						
						AZInstance	rep = (AZInstance)replies.get(i);
						
						if ( rep.getID().equals( inst.getID())){
							
							return;
						}
					}
					
					added = other_instances.put( inst.getID(), inst ) == null;
					
					replies.add( inst );
										
				}finally{
					
					this_mon.exit();
				}
				
				if ( added ){
								
					informAdded( inst );
				}
			}
		}
		
		protected AZInstance[]
		getReply()
		{
			try{
				Thread.sleep( 2500 );
				
			}catch( Throwable e ){
				
			}
			
			try{
				this_mon.enter();

				requests.remove( this );
				
				return(( AZInstance[])replies.toArray(new AZInstance[replies.size()]));			
				
			}finally{
				
				this_mon.exit();
			}
		}
	}

	public void
	addListener(
		AZInstanceManagerListener	l )
	{
		listeners.add( l );
	}
	
	public void
	removeListener(
		AZInstanceManagerListener	l )
	{
		listeners.remove( l );
	}
}
