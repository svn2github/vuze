/*
 * Created on 18-Jan-2005
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

package com.aelitis.azureus.core.dht.transport.util;

import com.aelitis.azureus.core.dht.transport.DHTTransportStats;

/**
 * @author parg
 *
 */

public abstract class 
DHTTransportStatsImpl
	implements DHTTransportStats
{
	private long[]	pings		= new long[4];
	private long[]	find_nodes	= new long[4];
	private long[]	find_values	= new long[4];
	private long[]	stores		= new long[4];
	private long[]	stats		= new long[4];
	
	public void
	add(
		DHTTransportStatsImpl	other )
	{
		add( pings, other.pings );
		add( find_nodes, other.find_nodes );
		add( find_values, other.find_values );
		add( stores, other.stores );
		add( stats, other.stats );
	}
	
	protected void
	add(
		long[]	a,
		long[] 	b )
	{
		for (int i=0;i<a.length;i++){
			a[i]	+= b[i];
		}
	}
	
	protected void
	snapshotSupport(
		DHTTransportStatsImpl	clone )
	{
		clone.pings		= (long[])pings.clone();
		clone.find_nodes	= (long[])find_nodes.clone();
		clone.find_values	= (long[])find_values.clone();
		clone.stores		= (long[])stores.clone();
	}
		// ping
	
	public void
	pingSent()
	{
		pings[STAT_SENT]++;
	}
	public void
	pingOK()
	{
		pings[STAT_OK]++;
	}
	public void
	pingFailed()
	{
		pings[STAT_FAILED]++;
	}
	public void
	pingReceived()
	{
		pings[STAT_RECEIVED]++;
	}
	
	public long[]
	getPings()
	{
		return( pings );
	}
	
		// find node
	
	public void
	findNodeSent()
	{
		find_nodes[STAT_SENT]++;
	}
	public void
	findNodeOK()
	{
		find_nodes[STAT_OK]++;
	}
	public void
	findNodeFailed()
	{
		find_nodes[STAT_FAILED]++;
	}
	public void
	findNodeReceived()
	{
		find_nodes[STAT_RECEIVED]++;
	}
	public long[]
	getFindNodes()
	{
		return( find_nodes );
	}
	
		// find value
	
	public void
	findValueSent()
	{
		find_values[STAT_SENT]++;
	}
	public void
	findValueOK()
	{
		find_values[STAT_OK]++;
	}
	public void
	findValueFailed()
	{
		find_values[STAT_FAILED]++;
	}
	public void
	findValueReceived()
	{
		find_values[STAT_RECEIVED]++;
	}
	public long[]
	getFindValues()
	{
		return( find_values );
	}
	
		// store
	
	public void
	storeSent()
	{
		stores[STAT_SENT]++;
	}
	public void
	storeOK()
	{
		stores[STAT_OK]++;
	}
	public void
	storeFailed()
	{
		stores[STAT_FAILED]++;
	}
	public void
	storeReceived()
	{
		stores[STAT_RECEIVED]++;
	}
	public long[]
	getStores()
	{
		return( stores );
	}
		//stats
	
	public void
	statsSent()
	{
		stats[STAT_SENT]++;
	}
	public void
	statsOK()
	{
		stats[STAT_OK]++;
	}
	public void
	statsFailed()
	{
		stats[STAT_FAILED]++;
	}
	public void
	statsReceived()
	{
		stats[STAT_RECEIVED]++;
	}
	
	public String
	getString()
	{
		return( "ping:" + getString( pings ) + "," +
				"store:" + getString( stores ) + "," +
				"node:" + getString( find_nodes ) + "," +
				"value:" + getString( find_values ) + "," +
				"stats:" + getString( stats ));
	}
	
	protected String
	getString(
		long[]	x )
	{
		String	str = "";
		
		for (int i=0;i<x.length;i++){
			
			str += (i==0?"":",") + x[i];
		}
				
		return( str );
	}
}
