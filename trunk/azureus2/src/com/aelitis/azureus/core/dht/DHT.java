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

import com.aelitis.azureus.core.dht.router.DHTRouter;
import com.aelitis.azureus.core.dht.transport.DHTTransport;

/**
 * @author parg
 *
 */

public interface 
DHT 
{
	public void
	put(
		byte[]		key,
		byte[]		value );
	
	public byte[]
	get(
		byte[]		key );
	
	public DHTTransport
	getTransport();
	
	public DHTRouter
	getRouter();
	
		/**
		 * externalises information that allows the DHT to be recreated at a later date
		 * and populated via the import method
		 * @param os
		 * @throws IOException
		 */
	
	public void
	exportState(
		OutputStream	os )
	
		throws IOException;
	
		/**
		 * populate the DHT with previously exported state 
		 * @param is
		 * @throws IOException
		 */
	
	public void
	importState(
		InputStream		is )
	
		throws IOException;
	
		/**
		 * Initialise the DHT - invoke after all transports have been added and any state
		 * imported. Can be invoked more than once if additional state is imported
		 */
	
	public void
	join();
	

	public void
	print();
}
