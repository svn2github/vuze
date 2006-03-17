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

import java.util.*;

import org.gudy.azureus2.core3.util.Average;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.dht.speed.DHTSpeedTester;
import com.aelitis.azureus.core.dht.speed.DHTSpeedTesterContact;
import com.aelitis.azureus.core.dht.speed.DHTSpeedTesterContactListener;
import com.aelitis.azureus.core.dht.speed.DHTSpeedTesterListener;
import com.aelitis.azureus.core.speedmanager.SpeedManager;
import com.aelitis.azureus.core.speedmanager.SpeedManagerAdapter;
import com.aelitis.azureus.core.util.CopyOnWriteList;

public class 
SpeedManagerImpl 
	implements SpeedManager
{
	private static final int CONTACT_NUMBER	= 3;
	private static final int PING_SECS		= 5;
	
	private AzureusCore			core;
	private DHTSpeedTester		speed_tester;
	private SpeedManagerAdapter	adapter;
	
	private int					min_up;
	private int					max_up;
	private boolean				enabled;
	
	private volatile CopyOnWriteList		ping_sources = new CopyOnWriteList();
	
	private Average ping_average = Average.getInstance( 1000, 10 );

	public
	SpeedManagerImpl(
		AzureusCore			_core,
		SpeedManagerAdapter	_adapter )
	{
		core			= _core;
		adapter			= _adapter;
	}
	
	public void
	setSpeedTester(
		DHTSpeedTester	_tester )
	{
		if ( speed_tester != null ){
			
			Debug.out( "speed tester already set!" );
			
			return;
		}
		
		speed_tester	= _tester;
		
			// TODO: who persists this stuff like enabled?
		
		setEnabled( true );
		
		speed_tester.addListener(
				new DHTSpeedTesterListener()
				{
					public void 
					contactAdded(
						DHTSpeedTesterContact contact)
					{
						if ( core.getInstanceManager().isLANAddress(contact.getContact().getAddress().getAddress())){
							
							contact.destroy();
							
						}else{
							
							new pingSource( contact );
						}
					}
				});
		
		SimpleTimer.addPeriodicEvent(
			1000,
			new TimerEventPerformer()
			{
				public void
				perform(
					TimerEvent	event )
				{
					if ( !enabled ){
						
						return;
					}
															
					List	sources = ping_sources.getList();
					
					if ( sources.size() > 0 ){
						
						calculate( sources);
					}
				}
			});
	}
	
	protected void
	calculate(
		List	sources )
	{
		int	current_speed = adapter.getCurrentUploadSpeed();
		
		int	speed_limit	= current_speed;

			// calculate...

		String	str  = "";
		
		Iterator	it = sources.iterator();
		
		int	ping_total = 0;
		
		while( it.hasNext()){
			
			pingSource	source = (pingSource)it.next();
			
			ping_total += source.getPing();
			
			str += (str.length()==0?"":",") + source.getPing() + "(" + source.getPingCount() + ")";
		}
		
		ping_average.addValue( ping_total / sources.size());
		
		System.out.println( "sources: " + str + " -> " + ping_average.getAverage());
		
		
		if ( min_up > 0 && speed_limit < min_up ){
			
			speed_limit = min_up;
			
		}else if ( max_up > 0 &&  speed_limit > max_up ){
			
			speed_limit = max_up;
		}
		
		adapter.setCurrentUploadLimit( speed_limit );
	}
	
	public boolean
	isAvailable()
	{
		return( ping_sources.getList().size() > 0 );
	}
	
	public void
	setMinumumUploadSpeed(
		int		speed )
	{
		min_up	= speed;
	}
	
	public int
	getMinumumUploadSpeed()
	{
		return( min_up );
	}
	
	public void
	setMaximumUploadSpeed(
		int		speed )
	{
		max_up	= speed;
	}
	
	public int
	getMaximumUploadSpeed()
	{
		return( max_up );
	}
	
	public void
	setEnabled(
		boolean		_enabled )
	{
		enabled	= _enabled;
		
		if ( speed_tester != null ){
			
			speed_tester.setContactNumber( enabled?CONTACT_NUMBER:0);
		}
	}
	
	public boolean
	isEnabled()
	{
		return( enabled );
	}
	
	public DHTSpeedTester
	getSpeedTester()
	{
		return( speed_tester );
	}
	
	protected class
	pingSource
	{
		private static final int PING_HISTORY	= 3;
		
		private DHTSpeedTesterContact		contact;
		
		private volatile int		ping_count;
		private volatile int		ping;
		
		protected
		pingSource(
			DHTSpeedTesterContact	_contact )
		{
			contact	= _contact;
			
			System.out.println( "Speed: contact added - " + contact.getContact().getString());
			
			contact.setPingPeriod( PING_SECS );
			
			contact.addListener(
				new DHTSpeedTesterContactListener()
				{
					int[]	pings		= new int[PING_HISTORY];
					
					public void
					ping(
						DHTSpeedTesterContact	contact,	
						int						round_trip_time )
					{
						// System.out.println( "    ping: " + round_trip_time );
						
						pings[ping_count++%PING_HISTORY] = round_trip_time;
						
						int	lim = ping_count > PING_HISTORY?PING_HISTORY:ping_count;
						
						int	res = 0;
						
						for (int i=0;i<lim;i++){
							
							res += pings[i];
						}
						
						ping = res/lim;
					}
					
					public void
					contactDied(
						DHTSpeedTesterContact	contact )
					{
						System.out.println( "Speed: contact died - " + contact.getContact().getString());
						
						ping_sources.remove( pingSource.this );
					}
				});
	
			ping_sources.add( this );
		}
		
		protected int
		getPing()
		{
			return( ping );
		}
		
		protected int
		getPingCount()
		{
			return( ping_count );
		}
	}
}
