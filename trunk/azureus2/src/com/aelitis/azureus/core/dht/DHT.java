/*
 * Created on 11-Jan-2005
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

package com.aelitis.azureus.core.dht;

import java.io.*;

import com.aelitis.azureus.core.dht.control.DHTControl;
import com.aelitis.azureus.core.dht.db.DHTDB;
import com.aelitis.azureus.core.dht.router.DHTRouter;
import com.aelitis.azureus.core.dht.transport.DHTTransport;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

/**
 * @author parg
 *
 */

public interface 
DHT 
{
		// all property values are Integer values
	
	public static final String	PR_CONTACTS_PER_NODE					= "EntriesPerNode";
	public static final String	PR_NODE_SPLIT_FACTOR					= "NodeSplitFactor";
	public static final String	PR_SEARCH_CONCURRENCY					= "SearchConcurrency";
	public static final String	PR_LOOKUP_CONCURRENCY					= "LookupConcurrency";
	public static final String	PR_MAX_REPLACEMENTS_PER_NODE			= "ReplacementsPerNode";
	public static final String	PR_CACHE_AT_CLOSEST_N					= "CacheClosestN";
	public static final String	PR_ORIGINAL_REPUBLISH_INTERVAL			= "OriginalRepublishInterval";
	public static final String	PR_CACHE_REPUBLISH_INTERVAL				= "CacheRepublishInterval";
	public static final String	PR_MAX_VALUES_STORED					= "MaxValuesStored";

	public static final byte		FLAG_SINGLE_VALUE	= 0x00;
	public static final byte		FLAG_DOWNLOADING	= 0x01;
	public static final byte		FLAG_SEEDING		= 0x02;
	public static final byte		FLAG_MULTI_VALUE	= 0x04;

	public static final int 	MAX_VALUE_SIZE		= 256;

		// diversification types
	
	public static final byte	DT_NONE			= 1;
	public static final byte	DT_FREQUENCY	= 2;
	public static final byte	DT_SIZE			= 3;
	
	
	public void
	put(
		byte[]					key,
		byte[]					value,
		byte					flags,
		DHTOperationListener	listener );
	
		/**
		 * Returns value if originated from here for key
		 * @param key
		 * @return
		 */
	
	public DHTTransportValue
	getLocalValue(
		byte[]		key );
	
	
		/**
		 * @param key
		 * @param max_values
		 * @param timeout
		 * @param listener
		 */
	
	public void
	get(
		byte[]					key,
		byte					flags,
		int						max_values,
		long					timeout,
		DHTOperationListener	listener );
	
		
	public byte[]
	remove(
		byte[]			key,
		DHTOperationListener	listener );
	
	public int
	getIntProperty(
		String		name );
	
	public DHTTransport
	getTransport();
	
	public DHTRouter
	getRouter();
	
	public DHTControl
	getControl();
	
	public DHTDB
	getDataBase();
	
		/**
		 * externalises information that allows the DHT to be recreated at a later date
		 * and populated via the import method
		 * @param os
		 * @param max  maximum to export, 0 -> all
		 * @throws IOException
		 */
	
	public void
	exportState(
		DataOutputStream	os,
		int					max )
	
		throws IOException;
	
		/**
		 * populate the DHT with previously exported state 
		 * @param is
		 * @throws IOException
		 */
	
	public void
	importState(
		DataInputStream		is )
	
		throws IOException;
	
		/**
		 * Integrate the node into the DHT
		 * Can be invoked more than once if additional state is imported
		 */
	
	public void
	integrate();

	public void
	setLogging(
		boolean	on );

	public void
	print();
}
