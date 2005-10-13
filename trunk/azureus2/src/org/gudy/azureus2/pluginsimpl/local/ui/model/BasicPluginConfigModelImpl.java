/*
 * Created on 28-Apr-2004
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

package org.gudy.azureus2.pluginsimpl.local.ui.model;

/**
 * @author parg
 *
 */

import java.util.*;



import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.plugins.ui.config.ActionParameter;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.LabelParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterGroup;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;

import org.gudy.azureus2.pluginsimpl.local.ui.UIManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.ui.config.*;

import org.gudy.azureus2.plugins.*;

import org.gudy.azureus2.plugins.ui.model.*;
import org.gudy.azureus2.plugins.ui.config.EnablerParameter;

public class 
BasicPluginConfigModelImpl
	implements BasicPluginConfigModel
{
	private UIManagerImpl		ui_manager;
	
	private String				parent_section;
	private String				section;
	private PluginInterface		pi;
	private ArrayList			parameters = new ArrayList();
	
	private String				key_prefix;
	
	public
	BasicPluginConfigModelImpl(
		UIManagerImpl		_ui_manager,
		String				_parent_section,
		String				_section )
	{
		ui_manager		= _ui_manager;
		parent_section	= _parent_section;
		section			= _section;
		
		pi				= ui_manager.getPluginInterface();

		key_prefix		= pi.getPluginconfig().getPluginConfigKeyPrefix();
	}

	public String
	getParentSection()
	{
		return( parent_section );
	}
	
	public String
	getSection()
	{
		return( section );
	}
	
	public PluginInterface
	getPluginInterface()
	{
		return( pi );
	}
	
	public Parameter[]
	getParameters()
	{
		Parameter[] res = new Parameter[parameters.size()];
		
		parameters.toArray( res );
		
		return( res );
	}
	
	public void
	addBooleanParameter(
		String 		key,
		String 		resource_name,
		boolean 	defaultValue )
	{
		addBooleanParameter2( key, resource_name, defaultValue );
	}
	
	public org.gudy.azureus2.plugins.ui.config.BooleanParameter
	addBooleanParameter2(
		String 		key,
		String 		resource_name,
		boolean 	defaultValue )
	{
		BooleanParameterImpl res = new BooleanParameterImpl( pi.getPluginconfig(), key_prefix + key, resource_name, defaultValue );
		
		parameters.add( res );
		
		return( res );
	}
	
	public void
	addStringParameter(
		String 		key,
		String 		resource_name,
		String  	defaultValue )
	{
		addStringParameter2( key, resource_name, defaultValue );
	}
	
	public org.gudy.azureus2.plugins.ui.config.StringParameter
	addStringParameter2(
		String 		key,
		String 		resource_name,
		String  	defaultValue )
	{
		StringParameterImpl res = new StringParameterImpl( pi.getPluginconfig(), key_prefix + key, resource_name, defaultValue );
	
		parameters.add( res );
		
		return( res );	
	}
	
	public org.gudy.azureus2.plugins.ui.config.StringListParameter
	addStringListParameter2(
		String 		key,
		String 		resource_name,
		String[]	values,
		String	 	defaultValue )
	{
		StringListParameterImpl res = new StringListParameterImpl( pi.getPluginconfig(), key_prefix + key, resource_name, defaultValue, values, values );
		
		parameters.add( res );
			
		return( res );			
	}
	
	public org.gudy.azureus2.plugins.ui.config.PasswordParameter
	addPasswordParameter2(
		String 		key,
		String 		resource_name,
		int			encoding_type,	
		byte[]	 	defaultValue )
	{
		PasswordParameterImpl res = new PasswordParameterImpl( pi.getPluginconfig(), key_prefix + key, resource_name, encoding_type, defaultValue );
		
		parameters.add( res );
			
		return( res );			
	}
	
	public org.gudy.azureus2.plugins.ui.config.IntParameter
	addIntParameter2(
		String 		key,
		String 		resource_name,
		int	 		defaultValue )
	{
		IntParameterImpl res = new IntParameterImpl( pi.getPluginconfig(), key_prefix + key, resource_name, defaultValue );
		
		parameters.add( res );
		
		return( res );	
	}	
	
	public org.gudy.azureus2.plugins.ui.config.DirectoryParameter
	addDirectoryParameter2(
		String 		key,
		String 		resource_name,
		String 		defaultValue )
	{
		DirectoryParameterImpl res = new DirectoryParameterImpl( pi.getPluginconfig(), key_prefix + key, resource_name, defaultValue );
		
		parameters.add( res );
		
		return( res );	
	}
	
	public LabelParameter
	addLabelParameter2(
		String		resource_name )
	{
		LabelParameterImpl res = new LabelParameterImpl( pi.getPluginconfig(), key_prefix, resource_name );
		
		parameters.add( res );
		
		return( res );		
	}

	
	public ActionParameter
	addActionParameter2(
		String 		label_resource_name,
		String		action_resource_name )	
	{
		ActionParameterImpl res = new ActionParameterImpl( pi.getPluginconfig(), label_resource_name, action_resource_name );
		
		parameters.add( res );
		
		return( res );			
	}
	
	public ParameterGroup
	createGroup(
		String											_resource_name,
		org.gudy.azureus2.plugins.ui.config.Parameter[]	_parameters )
	{
		ParameterGroupImpl	pg = new ParameterGroupImpl( _resource_name );
		
		for (int i=0;i<_parameters.length;i++){
			
			((ParameterImpl)_parameters[i]).setGroup( pg );
		}
		
		return( pg );
	}
	
	public void
	destroy()
	{
		ui_manager.destroy( this );
	}
}
