/*
 * Created on 16-Jan-2005
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


/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.router.*;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;

public class 
DHTLog 
{
	private static ThreadLocal		tls	= 
		new ThreadLocal()
		{
			public Object
			initialValue()
			{
				Object[]	data = new Object[2];
				
				data[0] = new Stack();
				
				data[1] = "";
				
				return( data );
			}
		};
		
	
	public static void
	log(
		String	str )
	{
		Object[]	data = (Object[])tls.get();
		
		Stack	stack 	= (Stack)data[0];
		String	indent	= (String)data[1];
		
		System.out.println( indent + ":" + getString((byte[])stack.peek()) + ":" + str );
	}
	
	public static void
	indent(
		DHTRouter	router )
	{
		Object[]	data = (Object[])tls.get();
		
		Stack	stack = (Stack)data[0];
		
		stack.push( router.getLocalContact().getID());
		
		data[1] = (String)data[1] + "  ";
	
	}
	
	public static void
	exdent()
	{
		Object[]	data = (Object[])tls.get();
		
		Stack	stack = (Stack)data[0];
		
		stack.pop();
		
		data[1] = ((String)data[1]).substring( 0, ((String)data[1]).length()-2);
	}
	
	public static String
	getString(
		byte[]	b )
	{
		return( ByteFormatter.nicePrint(b));
	}
	
	public static String
	getString(
		HashWrapper	w )
	{
		return( getString( w.getHash()));
	}
	
	public static String
	getString(
		DHTTransportContact[]	contacts )
	{
		String	res = "{";
		
		for (int i=0;i<contacts.length;i++){
			
			res += (i==0?"":",") + getString(contacts[i].getID());
		}
		
		return( res + "}" );
	}
	
	public static String
	getString(
		DHTTransportContact	contact )
	{
		return( getString(contact.getID()));
	}
	
	public static String
	getString(
		List		l )
	{
		String	res = "{";
		
		for (int i=0;i<l.size();i++){
			
			res += (i==0?"":",") + getString((DHTTransportContact)l.get(i));
		}
		
		return( res + "}" );
	}
	
	public static String
	getString(
		Set			s )
	{
		String	res = "{";
		
		Iterator it = s.iterator();
		
		while( it.hasNext()){
			
			res += (res.length()==1?"":",") + getString((DHTTransportContact)it.next());
		}
		
		return( res + "}" );
	}
	
	public static String
	getString(
		Map			s )
	{
		String	res = "{";
		
		Iterator it = s.keySet().iterator();
		
		while( it.hasNext()){
			
			res += (res.length()==1?"":",") + getString((HashWrapper)it.next());
		}
		
		return( res + "}" );	
	}
}
