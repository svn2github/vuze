/*
 * Created on 28-Nov-2004
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

package org.gudy.azureus2.pluginsimpl.local.installer;

/**
 * @author parg
 *
 */

import java.io.File;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.installer.*;
import org.gudy.azureus2.pluginsimpl.update.sf.SFPluginDetails;
import org.gudy.azureus2.pluginsimpl.update.sf.SFPluginDetailsException;
import org.gudy.azureus2.pluginsimpl.update.sf.SFPluginDetailsLoader;
import org.gudy.azureus2.pluginsimpl.update.sf.SFPluginDetailsLoaderFactory;

public class 
PluginInstallerImpl
	implements PluginInstaller
{
	protected static PluginInstallerImpl	singleton;
	
	public static PluginInstallerImpl
	getSingleton(
		PluginManager	_manager )
	{
		if ( singleton == null ){
			
			singleton	= new PluginInstallerImpl( _manager );
		}
		
		return( singleton );
	}
	
	protected PluginManager		manager;
	
	protected
	PluginInstallerImpl(
		PluginManager	_manager )
	{
		manager	= _manager;
	}
	
	protected PluginManager
	getPluginManager()
	{
		return( manager );
	}
	
	public StandardPlugin[]
	getStandardPlugins()
	
		throws PluginException
	{
		try{
			SFPluginDetailsLoader	loader = SFPluginDetailsLoaderFactory.getSingleton();
		
			SFPluginDetails[]	details = loader.getPluginDetails();

			StandardPlugin[]	res = new StandardPlugin[details.length];
			
			for (int i=0;i<res.length;i++){
				
				res[i] = new StandardPluginImpl( this, details[i] );
			}
			
			return( res );
		}catch( SFPluginDetailsException e ){
			
			throw( new PluginException("Failed to load standard plugin details", e ));
		}
	}
	
	public void
	install(
		StandardPlugin		standard_plugin )
	
		throws PluginException
	{
		PluginInterface	pi = standard_plugin.getAlreadyInstalledPlugin();

		if ( pi != null ){
			
			throw( new PluginException(" Plugin '" + standard_plugin.getId() + "' is already installed"));
		}
		
		System.out.println( "install '" + standard_plugin.getId() + "'" );
	}
	
	public boolean
	uninstall(
		StandardPlugin		standard_plugin )
	
		throws PluginException
	{
		PluginInterface	pi = standard_plugin.getAlreadyInstalledPlugin();
		
		if ( pi == null ){
			
			throw( new PluginException(" Plugin '" + standard_plugin.getId() + "' is not installed"));
		}
		
		return( pi.uninstall());
	}
	
	public boolean
	uninstall(
		PluginInterface		pi )
	
		throws PluginException
	{
		if ( pi.isMandatory()){
			
			throw( new PluginException( "Plugin '" + pi.getPluginID() + "' is mandatory, can't uninstall" ));
		}
		
		String	plugin_dir = pi.getPluginDirectoryName();
		
		if ( plugin_dir == null || !new File(plugin_dir).exists()){

			throw( new PluginException( "Plugin '" + pi.getPluginID() + "' is mandatory, can't uninstall" ));
		}
		
		try{
			if ( pi.isUnloadable()){
				
				pi.unload();
				
				FileUtil.recursiveDelete( new File( plugin_dir ));
				
				return( false );
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
		
			// TODO:
	
			// need to create an uninstall action to delete this
		
		throw( new PluginException( "not imp" ));	
	}
}
