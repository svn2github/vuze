/*
 * Created on Jan 28, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.core.devices.impl;

import java.text.SimpleDateFormat;
import java.util.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.LightHashMap;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.devices.Device;

public abstract class 
DeviceImpl
	implements Device
{
	private int				type;
	private String			uid;
	private String 			name;
	
	
	private long			last_seen;
	private boolean			online;
	
	
	private Map<Object,Object>	transient_properties = new LightHashMap<Object, Object>(1);
	
	protected
	DeviceImpl(
		int			_type,
		String		_uid,
		String		_name )
	{
		type		= _type;
		uid			= _uid;
		name		= _name;
	}
	
	public int
	getType()
	{
		return( type );
	}
	
	public String
	getID()
	{
		return( uid );
	}
	
	public String
	getName()
	{
		return( name );
	}
	
	protected void
	alive()
	{
		last_seen	= SystemTime.getCurrentTime();
		
		online	= true;
		
		setDirty();
	}
	
	protected void
	dead()
	{
		online	= false;
		
		setDirty();
	}
	
	protected boolean
	updateFrom(
		DeviceImpl		other )
	{
		if ( type != other.type || !uid.equals( other.uid )){
			
			Debug.out( "Inconsistent update operation" );
			
			return( false );
			
		}else{
			
			if ( name.equals( other.name )){
				
				name	= other.name;
				
				setDirty();
			}
			
			alive();
			
			return( true );
		}
	}
	
	protected void
	setDirty()
	{
		
	}
	
	protected void
	updateStatus()
	{	
	}
	
	public String[][] 
	getDisplayProperties() 
	{
		List<String[]> dp = new ArrayList<String[]>();
		
	    getDisplayProperties( dp );
	    	    
	    String[][] res = new String[2][dp.size()];
	   
	    int	pos = 0;
	    
	    for ( String[] entry: dp ){
	    
	    	res[0][pos] = entry[0];
	    	res[1][pos] = entry[1];
	    	
	    	pos++;
	    }
	    
	    return( res );
	}
	
	protected void
	getDisplayProperties(
		List<String[]>	dp )
	{
		addDP( dp, "TableColumn.header.name", name );
		
		addDP( dp, "azbuddy.ui.table.online",  online );
		
		addDP( dp, "azbuddy.ui.table.lastseen", last_seen==0?"":new SimpleDateFormat().format(new Date( last_seen )));
	}
	
	protected void
	addDP(
		List<String[]>	dp,
		String			name,
		String			value )
	{
		dp.add( new String[]{ name, value });
	}
	
	protected void
	addDP(
		List<String[]>	dp,
		String			name,
		boolean			value )
	{
		dp.add( new String[]{ name, MessageText.getString( value?"GeneralView.yes":"GeneralView.no" ) });
	}
	
	public void
	setTransientProperty(
		Object		key,
		Object		value )
	{
		synchronized( transient_properties ){
			
			transient_properties.put( key, value );
		}
	}
	
	public Object
	getTransientProperty(
		Object		key )
	{
		synchronized( transient_properties ){
			
			return( transient_properties.get( key ));
		}
	}
	
	public String
	getString()
	{
		return( "type=" + type + ",uid=" + uid + ",name=" + name );
	}
}
