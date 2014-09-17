/*
 * Created on Sep 17, 2014
 * Created by Paul Gardner
 * 
 * Copyright 2014 Azureus Software, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or 
 * (at your option) any later version.
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


package org.gudy.azureus2.pluginsimpl.local.ui.config;

import org.gudy.azureus2.plugins.config.ConfigParameterListener;
import org.gudy.azureus2.plugins.ui.components.UIPropertyChangeListener;
import org.gudy.azureus2.plugins.ui.components.UITextArea;
import org.gudy.azureus2.plugins.ui.config.EnablerParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.pluginsimpl.local.PluginConfigImpl;


public class
UITextAreaImpl 
	extends 	ParameterImpl
	implements 	UITextArea
{
	private org.gudy.azureus2.pluginsimpl.local.ui.components.UITextAreaImpl	text_area;
	
	public
	UITextAreaImpl(
		PluginConfigImpl		config,
		String					resource_name )
	{ 
		super( config, resource_name, resource_name );
		
		text_area = new org.gudy.azureus2.pluginsimpl.local.ui.components.UITextAreaImpl();
	}

	public void
	setText(
		String		text )
	{
		text_area.setText( text );
	}
	
	public void
	appendText(
		String		text )
	{
		text_area.appendText(text);
	}
	
	public String
	getText()
	{
		return( text_area.getText());
	}
	
	public void
	setMaximumSize(
		int	max_size )
	{
		text_area.setMaximumSize(max_size);
	}
	
	public void
	setEnabled(
		boolean		enabled )
	{
		super.setEnabled( enabled );
		
		text_area.setEnabled(enabled);
	}
	
	public boolean
	getEnabled()
	{
		return( super.isEnabled());
	}
	
	public void
	setVisible(
		boolean		visible )
	{
		super.setEnabled( visible );
		
		text_area.setEnabled(visible );
	}
	
	public boolean
	getVisible()
	{
		return( super.isVisible());
	}
	
	public void
	setProperty(
		String	property_type,
		Object	property_value )
	{
		text_area.setProperty(property_type, property_value);
	}
	
	public Object
	getProperty(
		String		property_type )
	{
		return( text_area.getProperty(property_type));
	}
	
	public void
	addPropertyChangeListener(
		UIPropertyChangeListener	l )
	{
		text_area.addPropertyChangeListener(l);
	}
	
	public void
	removePropertyChangeListener(
		UIPropertyChangeListener	l )
	{	
		text_area.removePropertyChangeListener(l);
	}
}
