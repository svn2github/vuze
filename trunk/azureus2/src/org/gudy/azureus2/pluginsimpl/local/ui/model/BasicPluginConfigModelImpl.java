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
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.layout.*;


import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.plugins.ui.config.ActionParameter;
import org.gudy.azureus2.plugins.ui.config.ConfigSectionSWT;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.LabelParameter;

import org.gudy.azureus2.pluginsimpl.local.ui.config.*;

import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.plugins.*;

import org.gudy.azureus2.plugins.ui.model.*;
import org.gudy.azureus2.plugins.ui.config.EnablerParameter;

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
	
	public Composite 
	configSectionCreate(
		Composite parent ) 
	{
		
			// main tab set up
		
		Composite gMainTab = new Composite(parent, SWT.NULL);
		
		GridData main_gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
		
		gMainTab.setLayoutData(main_gridData);
		
		GridLayout layout = new GridLayout();
		
		layout.numColumns = 2;
		
		layout.marginHeight = 0;
		
		gMainTab.setLayout(layout);
		
		Map	comp_map	= new HashMap();
		
		for (int i=0;i<parameters.size();i++){
			
			ParameterImpl	param = 	(ParameterImpl)parameters.get(i);
		
			Label label = new Label(gMainTab, param instanceof LabelParameterImpl?SWT.WRAP:SWT.NULL);
			
			Messages.setLanguageText(label, param.getLabel());
						
			String	key = param.getKey();
						
			//System.out.println( "key = " + key );
			
			Parameter	swt_param;
			
			if ( param instanceof BooleanParameterImpl ){
				
				swt_param = new BooleanParameter(gMainTab, key, ((BooleanParameterImpl)param).getDefaultValue());
					
			}else if ( param instanceof IntParameterImpl ){
						
				swt_param = new IntParameter(gMainTab, key, ((IntParameterImpl)param).getDefaultValue());
				
				GridData gridData = new GridData();
				gridData.widthHint = 100;
				
				swt_param.setLayoutData( gridData );
							
			}else if ( param instanceof StringParameterImpl ){
				
				GridData gridData = new GridData();
				
				gridData.widthHint = 150;

				swt_param = new StringParameter(gMainTab, key, ((StringParameterImpl)param).getDefaultValue());
				
				swt_param.setLayoutData( gridData );
				
			}else if ( param instanceof StringListParameterImpl ){
				
				StringListParameterImpl	sl_param = (StringListParameterImpl)param;
				
				GridData gridData = new GridData();
				
				gridData.widthHint = 150;

				swt_param = new StringListParameter(gMainTab, key, sl_param.getDefaultValue(), sl_param.getValues(), sl_param.getValues());
				
				swt_param.setLayoutData( gridData );
				
			}else if ( param instanceof PasswordParameterImpl ){
				
				GridData gridData = new GridData();
				
				gridData.widthHint = 150;

				swt_param = new PasswordParameter(gMainTab, key, ((PasswordParameterImpl)param).getEncodingType() == PasswordParameterImpl.ET_SHA1 );
				
				swt_param.setLayoutData( gridData );
				
			}else if ( param instanceof DirectoryParameterImpl ){
				
				Composite area = new Composite(gMainTab, SWT.NULL);

				GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.FILL_HORIZONTAL );
				
				area.setLayoutData(gridData);
				
				layout = new GridLayout();
				
				layout.numColumns 	= 2;
				layout.marginHeight = 0;
				layout.marginWidth 	= 0;
				
				area.setLayout(layout);				
				
				swt_param = new DirectoryParameter(area, key, ((DirectoryParameterImpl)param).getDefaultValue());
		
			}else if ( param instanceof ActionParameterImpl ){
				
				swt_param = new ButtonParameter( gMainTab, MessageText.getString(((ActionParameterImpl)param).getActionResource()));
			}else{
				
					// label
				
				GridData gridData = new GridData();
				gridData.horizontalSpan	= 2;
				
				label.setLayoutData( gridData );
				
				swt_param	= null;
			}
			
			if ( swt_param != null ){
									
				Control[]	c = swt_param.getControls();
					
				Object[] moo = new Object[c.length+2];
					
				moo[0] = swt_param;
				moo[1] = label;
					
				for (int j=0;j<c.length;j++){
						
					moo[j+2] = c[j];
				}
					
				comp_map.put( param, moo );
			}
		}
		
		for (int i=0;i<parameters.size();i++){
			
			ParameterImpl	param = 	(ParameterImpl)parameters.get(i);
			
			if ( param instanceof EnablerParameter ){
				
				List controlsToEnable = new ArrayList();
				
				Iterator iter = param.getEnabledOnSelectionParameters().iterator();
				
				while(iter.hasNext()){
					
					ParameterImpl enable_param = (ParameterImpl) iter.next();
					
				    Object[] stuff = (Object[])comp_map.get( enable_param );
				    
				    if ( stuff != null ){
				    	
					    for(int k = 1 ; k < stuff.length ; k++) {
					    	
					    	controlsToEnable.add(stuff[k]);
					    }
				    }
				}
				
				List controlsToDisable = new ArrayList();

				iter = param.getDisabledOnSelectionParameters().iterator();
				
				while(iter.hasNext()){
					
					ParameterImpl disable_param = (ParameterImpl)iter.next();
					
				    Object[] stuff = (Object[])comp_map.get( disable_param );
				    
				    if ( stuff != null ){
				    	
					    for(int k = 1 ; k < stuff.length ; k++) {
					    	
					    	controlsToDisable.add(stuff[k]);
					    }
				    }
				}

				Control[] ce = new Control[controlsToEnable.size()];
				Control[] cd = new Control[controlsToDisable.size()];

				if ( ce.length + cd.length > 0 ){
				
				    IAdditionalActionPerformer ap = 
				    	new DualChangeSelectionActionPerformer(
				    			(Control[]) controlsToEnable.toArray(ce),
								(Control[]) controlsToDisable.toArray(cd));
				    
	
				    BooleanParameter	target = (BooleanParameter)((Object[])comp_map.get(param))[0];
				    
				    target.setAdditionalActionPerformer(ap);
				}
			}
		}
		
		return( gMainTab );
	}
}
