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
import java.util.*;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.installer.*;
import org.gudy.azureus2.plugins.update.*;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.pluginsimpl.local.PluginInterfaceImpl;
import org.gudy.azureus2.pluginsimpl.update.sf.*;

import org.gudy.azureus2.pluginsimpl.update.PluginUpdatePlugin;

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

			List	res = new ArrayList();
				
			for (int i=0;i<details.length;i++){
				
				SFPluginDetails	detail = details[i];
				
				String	name 	= detail.getId();
				String	version	= detail.getVersion();
				
				if ( name.startsWith( "azplatform" ) || name.equals( "azupdater" )){
					
						// skip built in ones we don't want to let user install directly
						// not the cleanest of fixes, but it'll do for the moment
					
				}else if ( version == null || version.length() == 0 || !Character.isDigit(version.charAt(0))){
					
						// dodgy version
				}else{
					
					res.add( new StandardPluginImpl( this, details[i] ));
				}
			}
			
			StandardPlugin[]	res_a = new StandardPlugin[res.size()];

			res.toArray( res_a );
			
			return( res_a );
			
		}catch( SFPluginDetailsException e ){
			
			throw( new PluginException("Failed to load standard plugin details", e ));
		}
	}
	
	public void
	installFromFile(
		File				file,
		boolean				shared )
	
		throws PluginException
	{
		String	name = file.getName();
		
		boolean	bad_name	= true;
		
		int	pos = name.lastIndexOf( "." );
		
		if ( pos != -1 ){
			
			String	prefix = name.substring(0,pos);
			String	suffix = name.substring(pos+1);
			
			if ( 	suffix.toLowerCase().equals( "jar") ||
					suffix.toLowerCase().equals( "zip" )){
	
				pos = prefix.lastIndexOf("_");
		
				if ( pos != -1 ){
		
					String	plugin_id 	= prefix.substring(0,pos);
					String	version		= prefix.substring(pos+1);
					
					if ( manager.getPluginInterfaceByID( plugin_id ) != null ){
						
						throw( new PluginException( "Plugin '" + plugin_id + "' is already installed" ));
					}
					
					bad_name	= false;
					
					install( new String[]{ plugin_id }, shared, file, version, suffix.toLowerCase().equals( "jar"));
				}
			}
		}
		
		if ( bad_name ){
			
			throw( new PluginException( "Invalid plugin file name" ));
		}
	}
	
	public void
	install(
		StandardPlugin		standard_plugin,
		boolean				shared )
	
		throws PluginException
	{
		PluginInterface	pi = standard_plugin.getAlreadyInstalledPlugin();

		if ( pi != null ){
			
			throw( new PluginException(" Plugin '" + standard_plugin.getId() + "' is already installed"));
		}
		
		String	plugin_id	= standard_plugin.getId();
		
		install( new String[]{ plugin_id }, shared, null, null, false );
	}
	
	public void
	install(
		StandardPlugin[]	plugins,
		boolean				shared )
	
		throws PluginException
	{
		List	ids = new ArrayList();
		
		for (int i=0;i<plugins.length;i++){
			
			StandardPlugin	standard_plugin = plugins[i];
			
			PluginInterface	pi = standard_plugin.getAlreadyInstalledPlugin();

			if ( pi != null ){
				
				throw( new PluginException(" Plugin '" + standard_plugin.getId() + "' is already installed"));
			}
			
			ids.add( standard_plugin.getId());
		}
		
		String[]	ids_a = new String[ids.size()];
		
		ids.toArray( ids_a );
		
		install( ids_a, shared, null, null, false );
	}
	
	protected void
	install(
		final String[]				plugin_ids,
		final boolean				shared,
		final File					data_source,
		final String				data_source_version,
		final boolean				data_source_is_jar )
	
		throws PluginException
	{
		final PluginUpdatePlugin	pup = (PluginUpdatePlugin)manager.getPluginInterfaceByClass( PluginUpdatePlugin.class ).getPlugin();
		
		UpdateManager	uman = manager.getDefaultPluginInterface().getUpdateManager();
		
		UpdateCheckInstance	inst = 
			uman.createEmptyUpdateCheckInstance(UpdateCheckInstance.UCI_INSTALL);
		
		for (int i=0;i<plugin_ids.length;i++){
			
			String	plugin_id = plugin_ids[i];
			
			String	target_dir;
			
			if ( shared ){
			    	    
				target_dir 	= FileUtil.getApplicationFile( "plugins" ).toString();
				
			}else{
				
				target_dir 	= FileUtil.getUserFile( "plugins" ).toString(); 
			}
			
			target_dir += File.separator + plugin_id;
	
			new File( target_dir ).mkdir();
			
				// create a dummy plugin at version 0.0 to trigger the "upgrade" to the new
				// installed version
			
			final dummyPlugin	p = new dummyPlugin( plugin_id, target_dir );
			
			PluginManager.registerPlugin( p, plugin_id );
		
			final PluginInterface p_pi = manager.getPluginInterfaceByID( plugin_id );
			
				// null data source -> standard component from website, download it
			
			if ( data_source == null ){
				
				inst.addUpdatableComponent(
					pup.getCustomUpdateableComponent( plugin_id, false), false );
			
			}else{
			
					// here the data's coming from a local file
				
				inst.addUpdatableComponent(
					new UpdatableComponent()
					{
						public String
						getName()
						{
							return( data_source.getName());
						}
					
						public int
						getMaximumCheckTime()
						{
							return( 0 );
						}
						
						public void
						checkForUpdate(
							UpdateChecker	checker )
						{
							try{
								ResourceDownloader rd = 
									manager.getDefaultPluginInterface().getUtilities().getResourceDownloaderFactory().create( data_source );
								
	
								pup.addUpdate(
									p_pi,
									checker,
									getName(),
									new String[]{"Installation from file"},
									data_source_version,
									rd,
									data_source_is_jar,
									Update.RESTART_REQUIRED_NO );
									
							}finally{
								
								checker.completed();
							}
								
						}
					}, false );
				
			}
		
			inst.addListener(
				new UpdateCheckInstanceListener()
				{
					public void
					cancelled(
						UpdateCheckInstance		instance )
					{
						p.requestUnload();
					}
					
					public void
					complete(
						UpdateCheckInstance		instance )
					{
						p.requestUnload();
					}
				});
		}
		
		inst.start();
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
	
	protected class
	dummyPlugin
		implements UnloadablePlugin
	{
		protected String			plugin_name;
		protected String			plugin_dir;
		
		protected PluginInterfaceImpl	plugin_interface;
		
		protected
		dummyPlugin(
			String	_name,
			String	_target_dir )
		{
			plugin_name	= _name;
			plugin_dir	= _target_dir;
		}
		
		public void
		initialize(
			PluginInterface	_plugin_interface )
		{
			plugin_interface	= (PluginInterfaceImpl)_plugin_interface;
			
			plugin_interface.setPluginVersion( "0.0" );
			
			plugin_interface.setPluginName( plugin_name );
			
			plugin_interface.setPluginDirectoryName( plugin_dir );
		}
		
		public void
		unload()
		{	
		}
		
		protected void
		requestUnload()
		{
			try{
				plugin_interface.unload();
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
}
