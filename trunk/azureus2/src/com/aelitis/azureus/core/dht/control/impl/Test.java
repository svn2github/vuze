/*
 * Created on 15-Jan-2005
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

package com.aelitis.azureus.core.dht.control.impl;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.gudy.azureus2.core3.util.ByteFormatter;


/**
 * @author parg
 *
 */

public class 
Test 
{
	public static void
	main(
		String[]	args )
	{
		final byte[]	key = { 0,1,0,0 };
		
		final Set			set = 
			new TreeSet(
				new Comparator()
				{
					public int
					compare(
						Object	o1,
						Object	o2 )
					{						
						byte[] d1 = DHTControlImpl.computeDistance( (byte[])o1, key );
						byte[] d2 = DHTControlImpl.computeDistance( (byte[])o2, key );
						
						return( -DHTControlImpl.compareDistances( d1, d2 ));
					}
				});
		
		set.add( new byte[]{ 0,0,0,0 });
		set.add( new byte[]{ 3,0,0,0 });
		set.add( new byte[]{ 2,0,0,0 });
		set.add( new byte[]{ 2,0,1,0 });
		set.add( new byte[]{ 0,1,1,0 });
		set.add( new byte[]{ 1,0,0,0 });
		
		Iterator it = set.iterator();
		
		while( it.hasNext()){
			
			byte[]	val = (byte[])it.next();
			
			System.out.println( ByteFormatter.nicePrint( val ));
		}
	}
}
