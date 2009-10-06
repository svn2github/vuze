/*
 * Created on Oct 5, 2009
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


package com.aelitis.azureus.core.pairing.impl;

import java.util.*;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.pairing.*;

public class 
PairingManagerImpl
	implements PairingManager
{
	private static final PairingManagerImpl	singleton = new PairingManagerImpl();
	
	public static PairingManager
	getSingleton()
	{
		return( singleton );
	}
	
	private Map<String,PairedService>		services = new HashMap<String, PairedService>();
	
	protected
	PairingManagerImpl()
	{
		AzureusCoreFactory.addCoreRunningListener(
			new AzureusCoreRunningListener()
			{
				public void 
				azureusCoreRunning(
					AzureusCore core )
				{
					initialise( core );
				}
			});
	}
	
	protected void
	initialise(
		AzureusCore		_core )
	{
		
	}
	
	public String
	getAccessCode()
	
		throws PairingException
	{
		throw( new PairingException( "not imp" ));
	}
	
	public String
	getReplacementAccessCode()
	
		throws PairingException
	{
		throw( new PairingException( "not imp" ));
	}
	
	public PairedService
	addService(
		String		sid )
	{
		synchronized( services ){
			
			PairedService	result = services.get( sid );
			
			if ( result == null ){
				
				result = new PairedServiceImpl( sid );
				
				services.put( sid, result );
			}
			
			return( result );
		}
	}
	
	public PairedService
	getService(
		String		sid )
	{
		synchronized( services ){
			
			PairedService	result = services.get( sid );
			
			return( result );
		}
	}
	
	protected class
	PairedServiceImpl
		implements PairedService, PairingConnectionData
	{
		private String				sid;
		private Map<String,String>	attributes	= new HashMap<String, String>();
		
		protected
		PairedServiceImpl(
			String		_sid )
		{
			sid		= _sid;
		}
		
		public String
		getSID()
		{
			return( sid );
		}
		
		public PairingConnectionData
		getConnectionData()
		{
			return( this );
		}
		
		public void
		remove()
		{
			
		}
		
		public void
		setAttribute(
			String		name,
			String		value )
		{
			attributes.put( name, value );
		}
		
		public String
		getAttribute(
			String		name )
		{
			return( attributes.get( name ));
		}
		
		public void
		sync()
		{
			
		}
	}
}
