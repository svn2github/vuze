/*
 * Created on 23-Dec-2005
 * Created by Paul Gardner
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.instancemanager.impl;

import java.util.*;

import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.instancemanager.AZInstance;

public abstract class 
AZInstanceImpl 
	implements AZInstance
{
	private long	create_time;
	private Map		properties	= new HashMap();
	
	protected
	AZInstanceImpl()
	{
		create_time	= SystemTime.getCurrentTime();
	}
	
	protected long
	getCreationTime()
	{
		long	now = SystemTime.getCurrentTime();
		
		if ( now < create_time ){
			
			create_time	= now;
		}
		
		return( create_time );
	}
	
	protected static String
	mapAddress(
		String	str )
	{
		return( str.replace(':','$'));
	}
	
	protected static String
	unmapAddress(
		String	str )
	{
		return( str.replace('$',':'));
	}
	
	protected String
	encode()
	{
		String	reply = "azureus:" + getID();				

		reply += ":" + mapAddress(getInternalAddress().getHostAddress());
		
		reply += ":" + mapAddress(getExternalAddress().getHostAddress());
		
		reply += ":" + getTCPPort();
		
        reply += ":" + getUDPPort();
        
        return( reply );
	}
	
	public Object
	getProperty(
		String	name )
	{
		return(properties.get(name));
	}
	
	protected void
	setProperty(
		String	name,
		Object	value )
	{
		properties.put( name, value );
	}
	
	public String
	getString()
	{
		return( "int=" + getInternalAddress().getHostAddress() + ",ext=" + 
				getExternalAddress().getHostAddress() +	",tcp=" + getTCPPort() + ",udp=" + getUDPPort() );
	}
}
