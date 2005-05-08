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

package org.gudy.azureus2.plugins.ddb;

/**
 * @author parg
 *
 */

public interface 
DistributedDatabase 
{
	public static final int	OP_NONE				= 0;
	public static final int	OP_EXHAUSTIVE_READ	= 1;
	
	public boolean
	isAvailable();

	public boolean
	isExtendedUseAllowed();
	
	public DistributedDatabaseKey
	createKey(
		Object			key )
	
		throws DistributedDatabaseException;
	
	public DistributedDatabaseKey
	createKey(
		Object			key,
		String			description )
	
		throws DistributedDatabaseException;
	

	public DistributedDatabaseValue
	createValue(
		Object			value )
	
		throws DistributedDatabaseException;
		
	public void
	write(
		DistributedDatabaseListener		listener,
		DistributedDatabaseKey			key,
		DistributedDatabaseValue		value )
	
		throws DistributedDatabaseException;
	
	public void
	write(
		DistributedDatabaseListener		listener,
		DistributedDatabaseKey			key,
		DistributedDatabaseValue[]		values )
	
		throws DistributedDatabaseException;

	public void
	read(
		DistributedDatabaseListener		listener,
		DistributedDatabaseKey			key,
		long							timeout )
	
		throws DistributedDatabaseException;
	
	public void
	read(
		DistributedDatabaseListener		listener,
		DistributedDatabaseKey			key,
		long							timeout,
		int								options )
	
		throws DistributedDatabaseException;
	

	public void
	delete(
		DistributedDatabaseListener		listener,
		DistributedDatabaseKey			key )
	
		throws DistributedDatabaseException;
	
	public void
	addTransferHandler(
		DistributedDatabaseTransferType		type,
		DistributedDatabaseTransferHandler	handler )
	
		throws DistributedDatabaseException;
	
	public DistributedDatabaseTransferType
	getStandardTransferType(
		int		standard_type )
	
		throws DistributedDatabaseException;
}
