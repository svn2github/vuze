/*
 * Created on 07-May-2004
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

package org.gudy.azureus2.platform.win32;

/**
 * @author parg
 *
 */

import java.io.*;
import java.util.*;
import java.net.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.platform.*;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;
import org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader.*;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.update.*;
import org.gudy.azureus2.pluginsimpl.update.sf.*;

public class 
PlatformManagerUpdateChecker
	implements Plugin, UpdatableComponent
{
	public static final String UPDATE_NAME	= "Windows native support: " + PlatformManagerImpl.DLL_NAME + ".dll";
	
	public static final int	RD_SIZE_RETRIES	= 3;
	public static final int	RD_SIZE_TIMEOUT	= 10000;
	
	protected PluginInterface			plugin_interface;
	
	public void
	initialize(
		PluginInterface	_plugin_interface)
	{
		plugin_interface	= _plugin_interface;
		
		plugin_interface.getPluginProperties().setProperty( "plugin.name", "Native Support Updater" );

		String	version = "1.0";
		
		if (  PlatformManagerFactory.getPlatformType() == PlatformManager.PT_WINDOWS ){

			try{
				PlatformManagerImpl platform	= (PlatformManagerImpl)PlatformManagerFactory.getPlatformManager();
			
				if ( platform != null ){
					
					version = platform.getVersion();
				}
				
			}catch( Throwable e ){
			
				Debug.printStackTrace( e );
			}

			plugin_interface.getUpdateManager().registerUpdatableComponent( this, false );
			
		}else{
			
			plugin_interface.getPluginProperties().setProperty( "plugin.version.info", "Not required for this platform" );
			
		}
		
		plugin_interface.getPluginProperties().setProperty( "plugin.version", version );
	}
	
	public String
	getName()
	{
		return( UPDATE_NAME );
	}
	
	public int
	getMaximumCheckTime()
	{
		return(( RD_SIZE_RETRIES * RD_SIZE_TIMEOUT )/1000);
	}
	
	public void
	checkForUpdate(
		final UpdateChecker	checker )
	{
		try{			
			Properties	props = plugin_interface.getPluginProperties();
			
			SFPluginDetails	sf_details = SFPluginDetailsLoaderFactory.getSingleton().getPluginDetails( plugin_interface.getPluginID());
					
			String	current_dll_version = plugin_interface.getPluginVersion();
			
			LGLogger.log( "PlatformManager:Win32 update check starts: current = " + current_dll_version );
						
			boolean current_az_is_cvs	= Constants.isCVSVersion();
						
			String sf_plugin_version	= sf_details.getVersion();
			
			String sf_comp_version	 	= sf_plugin_version;
	
			if ( current_az_is_cvs ){
				
				String	sf_cvs_version = sf_details.getCVSVersion();
				
				if ( sf_cvs_version.length() > 0 ){
					
						// sf cvs version ALWAYS entry in _CVS
					
					sf_plugin_version	= sf_cvs_version;
					
					sf_comp_version = sf_plugin_version.substring(0,sf_plugin_version.length()-4);
				}
			}
			
			String	target_dll_version	= null;			

			if (	 sf_comp_version.length() == 0 ||
					!Character.isDigit(sf_comp_version.charAt(0))){
				
				LGLogger.log( "PlatformManager:Win32 no valid version to check against (" + sf_comp_version + ")" );

			}else if ( Constants.compareVersions( current_dll_version, sf_comp_version ) < 0 ){
				
				target_dll_version	= sf_comp_version;
			}
	
			LGLogger.log( "PlatformManager:Win32 update required = " + (target_dll_version!=null));
			
			if ( target_dll_version != null ){
					
				String target_download		= sf_details.getDownloadURL();
		
				if ( current_az_is_cvs ){
					
					String	sf_cvs_version = sf_details.getCVSVersion();
					
					if ( sf_cvs_version.length() > 0 ){
												
						target_download	= sf_details.getCVSDownloadURL();
					}
				}				

				ResourceDownloaderFactory rdf = ResourceDownloaderFactoryImpl.getSingleton();
				
				ResourceDownloader dll_rd = rdf.create( new URL( target_download ));
			
					// get size here so it is cached
				
				rdf.getTimeoutDownloader(rdf.getRetryDownloader(dll_rd,RD_SIZE_RETRIES),RD_SIZE_TIMEOUT).getSize();

				final String f_target_dll_version	= target_dll_version;
				
				dll_rd.addListener( 
						new ResourceDownloaderAdapter()
						{
							public boolean
							completed(
								final ResourceDownloader	downloader,
								InputStream					data )
							{	
								installUpdate( checker, downloader, f_target_dll_version, data );
									
								return( true );
							}							
						});

				checker.addUpdate(
						UPDATE_NAME,
						new String[]{"This DLL supports native operations such as file-associations" },
						target_dll_version,
						dll_rd,
						Update.RESTART_REQUIRED_YES );
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
			
			checker.failed();
			
		}finally{
			
			checker.completed();
		}
	}
	
	protected void
	installUpdate(
		UpdateChecker		checker,
		ResourceDownloader	rd,
		String				version,
		InputStream			data )
	{
		try{
			String	temp_dll_name 	= PlatformManagerImpl.DLL_NAME + "_" + version + ".dll";
			String	target_dll_name	= PlatformManagerImpl.DLL_NAME + ".dll";
			
			
			UpdateInstaller	installer = checker.createInstaller();
			
			installer.addResource( temp_dll_name, data );
			
			installer.addMoveAction( 
					temp_dll_name,
					installer.getInstallDir() + File.separator + target_dll_name );
			
		}catch( Throwable e ){
			
			rd.reportActivity("Update install failed:" + e.getMessage());
		}
	}
}
