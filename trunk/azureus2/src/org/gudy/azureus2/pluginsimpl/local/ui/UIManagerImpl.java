/*
 * Created on 19-Apr-2004
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

package org.gudy.azureus2.pluginsimpl.local.ui;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.ui.UIException;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.SWT.SWTManager;
import org.gudy.azureus2.plugins.ui.config.*;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.plugins.ui.model.PluginViewModel;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.pluginsimpl.local.ui.config.*;
import org.gudy.azureus2.pluginsimpl.local.ui.SWT.SWTManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.ui.model.BasicPluginConfigModelImpl;
import org.gudy.azureus2.pluginsimpl.local.ui.model.BasicPluginViewModelImpl;
import org.gudy.azureus2.pluginsimpl.local.ui.tables.TableManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.ui.view.BasicPluginViewImpl;
import org.gudy.azureus2.ui.swt.mainwindow.ClipboardCopy;


/**
 * @author parg
 *
 */

public class 
UIManagerImpl 
	implements UIManager
{	
	protected PluginInterface		pi;
	
	public
	UIManagerImpl(
		PluginInterface		_pi )
	{
		pi		=_pi;
	}
	
	protected boolean
	isSWTAvailable()
	{
		try{
			org.eclipse.swt.SWT.getVersion();
			
			return( true );
			
		}catch( Throwable e ){
			
			return( false );
		}
	}
	
	public BasicPluginViewModel
	getBasicPluginViewModel(
		String			name )
	{
		return( new BasicPluginViewModelImpl( name ));
	}
	
	public PluginView
	createPluginView(
		PluginViewModel	model )
	{
		if ( isSWTAvailable()){
			
		  if(model instanceof BasicPluginViewModel) {
		    return new BasicPluginViewImpl((BasicPluginViewModel)model);
		  } else {
		    //throw new Exception("Unsupported Model : " + model.getClass());
		    return null;
		  }
		}else{
			return( null );
		}
	}
	
	public BasicPluginViewModel
	createBasicPluginViewModel(
		String			name )
	{
		BasicPluginViewModel	model = getBasicPluginViewModel( name );
		
		PluginView	pv = createPluginView( model );
		
		if ( pv != null ){
			
			pi.getUIManager().getSWTManager().addView( pv );
		}
		
		return( model );
	}
	
	public BasicPluginConfigModel
	createBasicPluginConfigModel(
		String		section_name )
	{
		if ( isSWTAvailable()){
			
			return( new BasicPluginConfigModelImpl( pi, null, section_name ));
	
		}else{
			
			return( new dummyConfigModel());
		}
	}
	
	
	public BasicPluginConfigModel
	createBasicPluginConfigModel(
		String		parent_section,
		String		section_name )
	{
		if ( isSWTAvailable()){
			
			return( new BasicPluginConfigModelImpl( pi, parent_section, section_name ));
			
		}else{
			
			return( new dummyConfigModel());
		}
	}
	
	public void
	copyToClipBoard(
		String		data )
	
		throws UIException
	{
		if ( isSWTAvailable()){
			
			try{
				ClipboardCopy.copyToClipBoard( data );
				
			}catch( Throwable e ){
				
				throw( new UIException( "Failed to copy to clipboard", e ));
			}
		}
	}

  public TableManager getTableManager() {
    return TableManagerImpl.getSingleton();
  }

  public SWTManager getSWTManager() {
    return SWTManagerImpl.getSingleton();
  }
  
  protected class
  dummyConfigModel
  	implements BasicPluginConfigModel
  {
	public void
	addBooleanParameter(
		String 		key,
		String 		resource_name,
		boolean 	defaultValue )
	{	
	}
	
	public BooleanParameter
	addBooleanParameter2(
		final String 	key,
		String 			resource_name,
		final boolean 	defaultValue )
	{	
		return( new BooleanParameterImpl(pi.getPluginconfig(),key,resource_name,defaultValue));
	}
	
	
	public void
	addStringParameter(
		String 		key,
		String 		resource_name,
		String	 	defaultValue )
	{
	}
	
	public StringParameter
	addStringParameter2(
		final String 		key,
		String 				resource_name,
		final String	 	defaultValue )
	{
		return( new StringParameterImpl(pi.getPluginconfig(),key,resource_name,defaultValue));
	}
	
	public PasswordParameter
	addPasswordParameter2(
		String 		key,
		String 		resource_name,
		int			encoding_type,	
		byte[]	 	defaultValue )
	{
		return( new PasswordParameterImpl(pi.getPluginconfig(),key,resource_name,encoding_type, defaultValue));
	}
	
	public IntParameter
	addIntParameter2(
		final String 	key,
		String 			resource_name,
		final int	 	defaultValue )
	{
		return( new IntParameterImpl(pi.getPluginconfig(),key,resource_name,defaultValue));
	}
	
	public LabelParameter
	addLabelParameter2(
		String		resource )
	{
		return( new LabelParameter(){});
	}
	
	public DirectoryParameter
	addDirectoryParameter2(
		String 		key,
		String 		resource_name,
		String 		defaultValue )
	{
		return( new DirectoryParameterImpl(pi.getPluginconfig(),key, resource_name,defaultValue));
	}
  }
}
