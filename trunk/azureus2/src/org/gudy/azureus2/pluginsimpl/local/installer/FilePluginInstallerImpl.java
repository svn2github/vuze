/*
 * Created on 30-Nov-2004
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

import java.io.File;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.installer.FilePluginInstaller;
import org.gudy.azureus2.plugins.installer.PluginInstaller;
import org.gudy.azureus2.plugins.update.UpdatableComponent;
import org.gudy.azureus2.plugins.update.Update;
import org.gudy.azureus2.plugins.update.UpdateCheckInstance;
import org.gudy.azureus2.plugins.update.UpdateChecker;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.pluginsimpl.update.PluginUpdatePlugin;
import org.gudy.azureus2.pluginsimpl.update.sf.SFPluginDetails;
import org.gudy.azureus2.pluginsimpl.update.sf.SFPluginDetailsLoader;
import org.gudy.azureus2.pluginsimpl.update.sf.SFPluginDetailsLoaderFactory;

/**
 * @author parg
 *
 */

public class 
FilePluginInstallerImpl
	implements FilePluginInstaller, InstallablePluginImpl
{
	protected PluginInstallerImpl		installer;
	protected File						file;
	protected String					id;
	protected String					version;
	protected boolean					is_jar;
	
	protected
	FilePluginInstallerImpl(
		PluginInstallerImpl	_installer,
		File				_file )

		throws PluginException
	{
		installer	= _installer;
		file		= _file;

		String	name = file.getName();
		
		int	pos = name.lastIndexOf( "." );
		
		boolean	ok = false;
		
		if ( pos != -1 ){
			
			String	prefix = name.substring(0,pos);
			String	suffix = name.substring(pos+1);
			
			if ( 	suffix.toLowerCase().equals( "jar") ||
					suffix.toLowerCase().equals( "zip" )){
		
				pos = prefix.lastIndexOf("_");
		
				if ( pos != -1 ){
		
					id 			= prefix.substring(0,pos);

						// see if we can normalise the ID based on SF values
					
					try{
						SFPluginDetailsLoader	loader = SFPluginDetailsLoaderFactory.getSingleton();
					
						String[]	ids = loader.getPluginIDs();
						
						for (int i=0;i<ids.length;i++){
							
							if ( ids[i].equalsIgnoreCase(id)){
								
								id = ids[i];
								
								break;
							}
						}
					}catch( Throwable e ){
						
						Debug.printStackTrace(e);
					}

					version		= prefix.substring(pos+1);

					is_jar		= suffix.toLowerCase().equals( "jar");
						
					ok	= true;
				}
			}
		}
		
		if ( !ok ){
			
			throw( new PluginException( "Invalid plugin file name: must be of form <pluginid>_<version>.[jar|zip]" ));
		}
	}
	
	public File
	getFile()
	{
		return( file );
	}
	
	public String
	getId()
	{
		return( id );
	}
	
	public String
	getVersion()
	{
		return( version );
	}
	
	public String
	getName()
	{
		return( "" );
	}
	
	public String
	getDescription()
	{
		return( file.toString());
	}
		
	public PluginInterface
	getAlreadyInstalledPlugin()
	{
		return( installer.getAlreadyInstalledPlugin( getId()));
	}
	
	public void
	install(
		boolean		shared )
	
		throws PluginException
	{
		installer.install( this, shared );
	}	
	
	
	public void
	uninstall()
	
		throws PluginException
	{
		installer.uninstall( this );
	}	
	
	public PluginInstaller
	getInstaller()
	{
		return( installer );
	}
	
	public void
	addUpdate(
		UpdateCheckInstance	inst,
		final PluginUpdatePlugin	plugin_update_plugin,
		final Plugin				plugin,
		final PluginInterface		plugin_interface )
	{
		inst.addUpdatableComponent(
				new UpdatableComponent()
				{
					public String
					getName()
					{
						return( file.getName());
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
								plugin_interface.getUtilities().getResourceDownloaderFactory().create( file );
							
							plugin_update_plugin.addUpdate(
								plugin_interface,
								checker,
								getName(),
								new String[]{"Installation from file: " + file.toString()},
								version,
								rd,
								is_jar,
								Update.RESTART_REQUIRED_NO );
								
						}finally{
							
							checker.completed();
						}
							
					}
				}, false );
	}
}
