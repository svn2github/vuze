/*
 * Created on 12-Jan-2005
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

package com.aelitis.azureus.core.dht.impl;

import java.io.*;
import java.util.Properties;

import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.DHTLogger;
import com.aelitis.azureus.core.dht.DHTOperationListener;
import com.aelitis.azureus.core.dht.DHTStorageAdapter;
import com.aelitis.azureus.core.dht.control.*;
import com.aelitis.azureus.core.dht.db.DHTDB;
import com.aelitis.azureus.core.dht.nat.DHTNATPuncher;
import com.aelitis.azureus.core.dht.nat.DHTNATPuncherFactory;
import com.aelitis.azureus.core.dht.router.DHTRouter;
import com.aelitis.azureus.core.dht.transport.*;

/**
 * @author parg
 *
 */

public class 
DHTImpl 
	implements DHT
{
	private DHTStorageAdapter	storage_adapter;
	private DHTControl			control;
	private DHTNATPuncher		nat_puncher;
	private	Properties			properties;
	private DHTLogger			logger;
	
	public 
	DHTImpl(
		DHTTransport		_transport,
		Properties			_properties,
		DHTStorageAdapter	_storage_adapter,
		DHTLogger			_logger )
	{		
		properties		= _properties;
		storage_adapter	= _storage_adapter;
		logger			= _logger;
		
		DHTLog.setLogger( logger );
		
		int		K 		= getProp( PR_CONTACTS_PER_NODE, 			DHTControl.K_DEFAULT );
		int		B 		= getProp( PR_NODE_SPLIT_FACTOR, 			DHTControl.B_DEFAULT );
		int		max_r	= getProp( PR_MAX_REPLACEMENTS_PER_NODE, 	DHTControl.MAX_REP_PER_NODE_DEFAULT );
		int		s_conc 	= getProp( PR_SEARCH_CONCURRENCY, 			DHTControl.SEARCH_CONCURRENCY_DEFAULT );
		int		l_conc 	= getProp( PR_LOOKUP_CONCURRENCY, 			DHTControl.LOOKUP_CONCURRENCY_DEFAULT );
		int		o_rep 	= getProp( PR_ORIGINAL_REPUBLISH_INTERVAL, 	DHTControl.ORIGINAL_REPUBLISH_INTERVAL_DEFAULT );
		int		c_rep 	= getProp( PR_CACHE_REPUBLISH_INTERVAL, 	DHTControl.CACHE_REPUBLISH_INTERVAL_DEFAULT );
		int		c_n 	= getProp( PR_CACHE_AT_CLOSEST_N, 			DHTControl.CACHE_AT_CLOSEST_N_DEFAULT );
		
		control = DHTControlFactory.create( 
				new DHTControlAdapter()
				{
					public DHTStorageAdapter
					getStorageAdapter()
					{
						return( storage_adapter );
					}
					
					public byte[][]
					diversify(
						DHTTransportContact	cause,
						boolean				put_operation,
						boolean				existing,
						byte[]				key,
						byte				type,
						boolean				exhaustive )
					{
						boolean	valid;
						
						if ( existing ){
							
							valid =	 	type == DHT.DT_FREQUENCY ||
										type == DHT.DT_SIZE ||
										type == DHT.DT_NONE;
						}else{
							
							valid = 	type == DHT.DT_FREQUENCY ||
										type == DHT.DT_SIZE;
						}
						
						if ( storage_adapter != null && valid ){
							
							if ( existing ){
								
								return( storage_adapter.getExistingDiversification( key, put_operation, exhaustive ));
								
							}else{
								
								return( storage_adapter.createNewDiversification( cause, key, put_operation, type, exhaustive ));
							}
						}else{
							
							if ( !valid ){
								
								Debug.out( "Invalid diversification received: type = " + type );
							}
							
							if ( existing ){
								
								return( new byte[][]{ key });
								
							}else{
								
								return( new byte[0][] );
							}
						}
					}
				},
				_transport, 
				K, B, max_r,
				s_conc, l_conc, 
				o_rep, c_rep, c_n,
				logger );
		
		nat_puncher	= DHTNATPuncherFactory.create( this );
	}
	
	protected int
	getProp(
		String		name,
		int			def )
	{
		Integer	x = (Integer)properties.get(name);
		
		if ( x == null ){
			
			properties.put( name, new Integer( def ));
			
			return( def );
		}
		
		return( x.intValue());
	}
	
	public int
	getIntProperty(
		String		name )
	{
		return(((Integer)properties.get(name)).intValue());
	}
	
	public void
	put(
		byte[]					key,
		String					description,
		byte[]					value,
		byte					flags,
		DHTOperationListener	listener )
	{
		control.put( key, description, value, flags, listener );
	}
	
	public DHTTransportValue
	getLocalValue(
		byte[]		key )
	{
		return( control.getLocalValue( key ));
	}
		
	public void
	get(
		byte[]					key,
		String					description,
		byte					flags,
		int						max_values,
		long					timeout,
		boolean					exhaustive,
		DHTOperationListener	listener )
	{
		control.get( key, description, flags, max_values, timeout, exhaustive, listener );
	}
		
	public byte[]
	remove(
		byte[]					key,
		String					description,
		DHTOperationListener	listener )
	{
		return( control.remove( key, description, listener ));
	}
	
	public DHTTransport
	getTransport()
	{
		return( control.getTransport());
	}
	
	public DHTRouter
	getRouter()
	{
		return( control.getRouter());
	}
	
	public DHTControl
	getControl()
	{
		return( control );
	}
	
	public DHTDB
	getDataBase()
	{
		return( control.getDataBase());
	}
	
	public DHTNATPuncher
	getNATPuncher()
	{
		return( nat_puncher );
	}
	
	public void
	integrate(
		boolean		full_wait )
	{
		control.seed( full_wait );	
		
		nat_puncher.start();
	}
	
	public void
	exportState(
		DataOutputStream	os,
		int					max )
	
		throws IOException
	{	
		control.exportState( os, max );
	}
	
	public void
	importState(
		DataInputStream		is )
	
		throws IOException
	{	
		control.importState( is );
	}
	
	public void
	setLogging(
		boolean	on )
	{
		DHTLog.setLogging( on );
	}
	
	public DHTLogger
	getLogger()
	{
		return( logger );
	}
	
	public void
	print()
	{
		control.print();
	}
}
