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

import org.eclipse.swt.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;


import org.gudy.azureus2.plugins.ui.config.ConfigSectionSWT;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;

import org.gudy.azureus2.pluginsimpl.local.ui.config.*;

import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.plugins.*;

import org.gudy.azureus2.plugins.ui.model.*;

public class 
BasicPluginConfigModelImpl
	implements BasicPluginConfigModel, ConfigSectionSWT
{
	protected PluginInterface		pi;
	
	protected String				parent_section;
	protected String				section;
	
	protected ArrayList				parameters = new ArrayList();
	
	protected String				key_prefix;
	
	public
	BasicPluginConfigModelImpl(
		PluginInterface		_pi,
		String				_parent_section,
		String				_section )
	{
		pi				= _pi;
		parent_section	= _parent_section;
		section			= _section;
		
		key_prefix		= pi.getPluginconfig().getPluginConfigKeyPrefix();
		
		pi.addConfigSection( this );
	}

	public String 
	configSectionGetParentSection()
	{
		if ( parent_section == null || parent_section.length() == 0 ){
			
			return( ConfigSection.SECTION_ROOT );
		}

		return( parent_section );
	}
	
	public String 
	configSectionGetName()
	{
		return( section );
	}


	public void 
	configSectionSave()
	{
		
	}


	public void 
	configSectionDelete()
	{
		
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
	
	public Composite 
	configSectionCreate(
		Composite parent ) 
	{
		GridData gridData;
		GridLayout layout;
		Label label;
		
			// main tab set up
		
		Composite gMainTab = new Composite(parent, SWT.NULL);
		
		gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
		gMainTab.setLayoutData(gridData);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		gMainTab.setLayout(layout);
		
		for (int i=0;i<parameters.size();i++){
			
			ParameterImpl	param = 	(ParameterImpl)parameters.get(i);
		
			label = new Label(gMainTab, SWT.NULL);
			
			Messages.setLanguageText(label, param.getLabel());
			
			gridData = new GridData();
			//gridData.widthHint = 40;
			
			String	key = param.getKey();
						
			//System.out.println( "key = " + key );
			
			if ( param instanceof BooleanParameterImpl ){
				
				new BooleanParameter(gMainTab, key, ((BooleanParameterImpl)param).getDefaultValue());
					
			}else if ( param instanceof IntParameterImpl ){
						
				IntParameter intp = new IntParameter(gMainTab, key, ((IntParameterImpl)param).getDefaultValue());
				
				gridData = new GridData();
				gridData.widthHint = 100;
				
				intp.setLayoutData( gridData );
							
			}else{
				gridData = new GridData();
				
				gridData.widthHint = 150;

				new StringParameter(gMainTab, key, ((StringParameterImpl)param).getDefaultValue() ).setLayoutData( gridData );
			}
		}
		
		return( gMainTab );
	}
}
