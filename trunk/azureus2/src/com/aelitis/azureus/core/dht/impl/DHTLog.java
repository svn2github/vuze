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
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

public class 
DHTLog 
{
	public static final boolean	ADD_TRACE	= true;
	
	private static boolean	LOGGING_DEFAULT	= false;
	
	private static ThreadLocal		tls;
	
	static{
		if (ADD_TRACE ){ 
	
			tls = 
				new ThreadLocal()
				{
					public Object
					initialValue()
					{
						Object[]	data = new Object[3];
						
						data[0] = new Stack();
						
						data[1] = "";
						
						data[2] = new Boolean(LOGGING_DEFAULT);
						
						return( data );
					}
				};
		}
	}

	public static void
	log(
		String	str )
	{
		if ( ADD_TRACE ){
			Object[]	data = (Object[])tls.get();
			
			Stack	stack 			= (Stack)data[0];
			String	indent			= (String)data[1];
			boolean	logging_enabled = ((Boolean)data[2]).booleanValue();
			
			if ( logging_enabled ){
				
				if ( stack.isEmpty()){
					
					System.out.println( str );
					
				}else{
					
					System.out.println( indent + ":" + getString((byte[])stack.peek()) + ":" + str );
				}
			}
		}
	}
	
	public static void
	setLoggingEnabled(
		boolean	b )
	{
		if ( ADD_TRACE ){
			
			Object[]	data = (Object[])tls.get();
	
			data[2] = new Boolean(b);
		}
	}
	
	public static void
	indent(
		DHTRouter	router )
	{
		if ( ADD_TRACE ){
			Object[]	data = (Object[])tls.get();
				
			Stack	stack = (Stack)data[0];
				
			stack.push( router.getID());
				
			data[1] = (String)data[1] + "  ";
		}
	}
	
	public static void
	exdent()
	{
		if ( ADD_TRACE ){
			Object[]	data = (Object[])tls.get();
				
			Stack	stack = (Stack)data[0];
				
			stack.pop();
				
			data[1] = ((String)data[1]).substring( 0, ((String)data[1]).length()-2);
		}
	}
	
	public static String
	getString(
		byte[]	b )
	{
		if ( ADD_TRACE ){
			
			return( ByteFormatter.nicePrint(b));
			
		}else{
			
			return( "" );
		}
	}
	
	public static String
	getString(
		HashWrapper	w )
	{
		if ( ADD_TRACE ){
			
			return( getString( w.getHash()));
			
		}else{
			return( "" );
		}
	}
	
	public static String
	getString(
		DHTTransportContact[]	contacts )
	{
		if ( ADD_TRACE ){
			
			String	res = "{";
			
			for (int i=0;i<contacts.length;i++){
				
				res += (i==0?"":",") + getString(contacts[i].getID());
			}
			
			return( res + "}" );
		}else{
			return( "" );
		}
	}
	
	public static String
	getString(
		DHTTransportContact	contact )
	{
		if ( ADD_TRACE ){
			return( getString(contact.getID()));
		}else{
			return( "" );
		}
	}
	
	public static String
	getString(
		List		l )
	{
		if ( ADD_TRACE ){
			String	res = "{";
			
			for (int i=0;i<l.size();i++){
				
				res += (i==0?"":",") + getString((DHTTransportContact)l.get(i));
			}
			
			return( res + "}" );
		}else{
			return( "" );
		}
	}
	
	public static String
	getString(
		Set			s )
	{
		if ( ADD_TRACE ){
			String	res = "{";
			
			Iterator it = s.iterator();
			
			while( it.hasNext()){
				
				res += (res.length()==1?"":",") + getString((DHTTransportContact)it.next());
			}
			
			return( res + "}" );
		}else{
			return( "" );
		}
	}
	
	public static String
	getString(
		Map			s )
	{
		if ( ADD_TRACE ){
			String	res = "{";
			
			Iterator it = s.keySet().iterator();
			
			while( it.hasNext()){
				
				res += (res.length()==1?"":",") + getString((HashWrapper)it.next());
			}
			
			return( res + "}" );	
		}else{
			return( "" );
		}
	}
	
	public static String
	getString(
		DHTTransportValue	value )
	{
		if ( ADD_TRACE ){
			
			if ( value == null ){
				
				return( "<null>");
			}
			
			return( getString(value.getValue()) + "<" + value.getCacheDistance() + ">" );
		}else{
			return( "" );
		}
	}
}
