/*
 * Created on Sep 22, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.core.custom.impl;

import java.io.File;
import java.io.InputStream;

import com.aelitis.azureus.core.custom.Customization;
import com.aelitis.azureus.core.custom.CustomizationException;

public class 
CustomizationImpl 	
	implements Customization
{
	private CustomizationManagerImpl		manager;
	
	private String		name;
	private String		version;
	private File		contents;
	
	protected
	CustomizationImpl(
		CustomizationManagerImpl	_manager,
		String						_name,
		String						_version,
		File						_contents )
	
		throws CustomizationException
	{	
		manager		= _manager;
		name		= _name;
		version		= _version;
		contents	= _contents;
		
		if ( !contents.exists()){
			
			throw( new CustomizationException( "Content file '" + contents + " not found" ));
		}
	}
	
	public String
	getName()
	{
		return( name );
	}
	
	public String
	getVersion()
	{
		return( version );
	}
	
	protected File
	getContents()
	{
		return( contents );
	}
	
	public Object
	getProperty(
		String		name )
	{
		return( null );
	}
	
	public boolean
	isActive()
	{
		return( true );
	}
	
	public void
	setActive(
		boolean		active )
	{
		// TODO:
	}
	
	public InputStream
	getResource(
		String		resource_name )
	{
		return( null );
	}
	
	public void 
	exportToVuzeFile(	
		File 		file )
	
		throws CustomizationException 
	{
		manager.exportCustomization( this, file );
	}
}
