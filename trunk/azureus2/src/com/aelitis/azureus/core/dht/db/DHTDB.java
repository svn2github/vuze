/*
 * Created on 28-Jan-2005
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

package com.aelitis.azureus.core.dht.db;

import java.util.Iterator;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.control.DHTControl;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

/**
 * @author parg
 *
 */

public interface 
DHTDB 
{
	public void
	setControl(
		DHTControl		control );
	
	public DHTDBValue
	store(
		HashWrapper		key,
		byte[]			value,
		byte			flags );
	
	public void
	store(
		DHTTransportContact 	sender, 
		HashWrapper				key,
		DHTTransportValue[]		values );
	
	public DHTDBValue[]
	get(
		HashWrapper		key,
		int				max_values );
		
	public DHTDBValue
	remove(	
		DHTTransportContact 	sender,
		HashWrapper				key );
	
	public boolean
	isEmpty();
	
	public long
	getSize();
	
		/**
		 * Returns an iterator over HashWrapper values denoting the snapshot of keys
		 * Thus by the time a key is used the entry may no longer exist
		 * @return
		 */
	
	public Iterator
	getKeys();
	
	public void
	print();
}
