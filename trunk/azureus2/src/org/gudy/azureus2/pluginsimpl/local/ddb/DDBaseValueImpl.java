/*
 * Created on 18-Feb-2005
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

package org.gudy.azureus2.pluginsimpl.local.ddb;

import org.gudy.azureus2.plugins.ddb.*;

import com.aelitis.azureus.plugins.dht.DHTPlugin;

/**
 * @author parg
 *
 */

public class 
DDBaseValueImpl 
	implements DistributedDatabaseValue
{
	private DDBaseContactImpl	contact;
	
	private Object			value;
	private byte[]			value_bytes;
	
	private long			creation_time;
	
		// we reserve 3 bytes for overflow marker and length encoding for multi-value values

	protected static int MAX_VALUE_SIZE 	= DHTPlugin.MAX_VALUE_SIZE -3;
	
	protected 
	DDBaseValueImpl(
		DDBaseContactImpl	_contact,
		Object				_value,
		long				_creation_time )
	
		throws DistributedDatabaseException
	{
		contact			= _contact;
		value			= _value;
		creation_time	= _creation_time;
		
		value_bytes	= DDBaseHelpers.encode( value );
		
		// don't police value size limit here as temporary large objects can be 
		// created when handling transfers
	}
	
	protected 
	DDBaseValueImpl(
		DDBaseContactImpl	_contact,
		byte[]				_value_bytes,
		long				_creation_time )
	{
		contact			= _contact;
		value_bytes		= _value_bytes;
		creation_time	= _creation_time;
	}
	
	public Object
	getValue(
		Class		c )
	
		throws DistributedDatabaseException
	{
		if ( value == null ){
			
			value = DDBaseHelpers.decode( c, value_bytes );
		}
		
		return( value );
	}
	
	protected byte[]
	getBytes()
	{
		return( value_bytes );
	}
	
	public long
	getCreationTime()
	{
		return( creation_time );
	}

	public DistributedDatabaseContact
	getContact()
	{		
		return( contact );
	}
}
