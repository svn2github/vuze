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

import org.gudy.azureus2.plugins.logging.LoggerChannel;

import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.DHTOperationListener;
import com.aelitis.azureus.core.dht.control.*;
import com.aelitis.azureus.core.dht.db.DHTDB;
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
	private DHTControl		control;
	private	Properties		properties;
	private LoggerChannel	logger;
	
	public 
	DHTImpl(
		DHTTransport	_transport,
		Properties		_properties,
		LoggerChannel	_logger )
	{		
		properties	= _properties;
		logger		= _logger;
		
		DHTLog.setLogger( logger );
		
		int		K 		= getProp( PR_CONTACTS_PER_NODE, 			DHTControl.K_DEFAULT );
		int		B 		= getProp( PR_NODE_SPLIT_FACTOR, 			DHTControl.B_DEFAULT );
		int		max_r	= getProp( PR_MAX_REPLACEMENTS_PER_NODE, 	DHTControl.MAX_REP_PER_NODE_DEFAULT );
		int		s_conc 	= getProp( PR_SEARCH_CONCURRENCY, 			DHTControl.SEARCH_CONCURRENCY_DEFAULT );
		int		l_conc 	= getProp( PR_LOOKUP_CONCURRENCY, 			DHTControl.LOOKUP_CONCURRENCY_DEFAULT );
		int		o_rep 	= getProp( PR_ORIGINAL_REPUBLISH_INTERVAL, 	DHTControl.ORIGINAL_REPUBLISH_INTERVAL_DEFAULT );
		int		c_rep 	= getProp( PR_CACHE_REPUBLISH_INTERVAL, 	DHTControl.CACHE_REPUBLISH_INTERVAL_DEFAULT );
		int		c_n 	= getProp( PR_CACHE_AT_CLOSEST_N, 			DHTControl.CACHE_AT_CLOSEST_N_DEFAULT );
		int		max_v 	= getProp( PR_MAX_VALUES_STORED, 			DHTControl.MAX_VALUES_STORED_DEFAULT );
		
		control = DHTControlFactory.create( 
				_transport, 
				K, B, max_r,
				s_conc, l_conc, 
				o_rep, c_rep, c_n, max_v,
				logger );
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
		byte[]		key,
		byte[]		value )
	{
		control.put( key, value );
	}
	
	public void
	put(
		byte[]			key,
		byte[]			value,
		DHTOperationListener	listener )
	{
		control.put( key, value, listener );
	}
	
	public byte[]
	get(
		byte[]		key,
		long		timeout )
	{
		return( control.get( key, timeout ));
	}
	
	public void
	get(
		byte[]			key,
		int				max_values,
		long			timeout,
		DHTOperationListener	listener )
	{
		control.get( key, max_values, timeout, listener );
	}
	
	public byte[]
	remove(
		byte[]		key )
	{
		return( control.remove( key ));
	}
	
	public byte[]
	remove(
		byte[]			key,
		DHTOperationListener	listener )
	{
		return( control.remove( key, listener ));
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
	
	public DHTDB
	getDataBase()
	{
		return( control.getDataBase());
	}
	
	public void
	integrate()
	{
		control.seed();	
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
	
	public void
	print()
	{
		control.print();
	}
}
