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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
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
	public static final String UPDATE_NAME	= "Platform-specific support";
	
	public static final int	RD_SIZE_RETRIES	= 3;
	public static final int	RD_SIZE_TIMEOUT	= 10000;
	
	protected PluginInterface			plugin_interface;
	
	public void
	initialize(
		PluginInterface	_plugin_interface)
	{
		plugin_interface	= _plugin_interface;
		
		plugin_interface.getPluginProperties().setProperty( "plugin.name", "Platform-Specific Support" );

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
			SFPluginDetails	sf_details = SFPluginDetailsLoaderFactory.getSingleton().getPluginDetails( plugin_interface.getPluginID());
					
			String	current_version = plugin_interface.getPluginVersion();
			
			LGLogger.log( "PlatformManager:Win32 update check starts: current = " + current_version );
						
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
			
			String	target_version	= null;			

			if (	 sf_comp_version.length() == 0 ||
					!Character.isDigit(sf_comp_version.charAt(0))){
				
				LGLogger.log( "PlatformManager:Win32 no valid version to check against (" + sf_comp_version + ")" );

			}else if ( Constants.compareVersions( current_version, sf_comp_version ) < 0 ){
				
				target_version	= sf_comp_version;
			}
	
			LGLogger.log( "PlatformManager:Win32 update required = " + (target_version!=null));
			
			if ( target_version != null ){
					
				String target_download		= sf_details.getDownloadURL();
		
				if ( current_az_is_cvs ){
					
					String	sf_cvs_version = sf_details.getCVSVersion();
					
					if ( sf_cvs_version.length() > 0 ){
												
						target_download	= sf_details.getCVSDownloadURL();
					}
				}				

				ResourceDownloaderFactory rdf = ResourceDownloaderFactoryImpl.getSingleton();
				
				ResourceDownloader update_rd = rdf.create( new URL( target_download ));
			
					// get size here so it is cached
				
				rdf.getTimeoutDownloader(rdf.getRetryDownloader(update_rd,RD_SIZE_RETRIES),RD_SIZE_TIMEOUT).getSize();
				
				update_rd.addListener( 
						new ResourceDownloaderAdapter()
						{
							public boolean
							completed(
								final ResourceDownloader	downloader,
								InputStream					data )
							{	
								installUpdate( checker, downloader, data );
									
								return( true );
							}							
						});

				
				List	update_desc = new ArrayList();
				
				List	desc_lines = splitMultiLine( "", sf_details.getDescription());
								
				update_desc.addAll( desc_lines );
								
				List	comment_lines = splitMultiLine( "    ", sf_details.getComment());
				
				update_desc.addAll( comment_lines );

				String[]	update_d = new String[update_desc.size()];
				
				update_desc.toArray( update_d );

				checker.addUpdate(
						UPDATE_NAME,
						update_d,
						target_version,
						update_rd,
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
		InputStream			data )
	{
		try{
			UpdateInstaller installer = checker.createInstaller();
			
		    ZipInputStream zip = new ZipInputStream( data );
		    
		    ZipEntry entry = null;
		    
		    while((entry = zip.getNextEntry()) != null){
		    	
		        String name = entry.getName();
		        
		        if ( name.toLowerCase().startsWith( "windows/" )){
		        	
		        	// win32 only files
		        
		        	name = name.substring( 8 );
		        
		        		// skip the director entry
		        	
		        	if ( name.length() > 0 ){
		        		
		    			LGLogger.log( "PlatformManager:Win32 adding action for '" + name + "'" );

		        		installer.addResource( name, zip, false );

		        		installer.addMoveAction( name, installer.getInstallDir() + File.separator + name );
		        	}
		        }
		    }
			
		    zip.close();
		    
		}catch( Throwable e ){
			
			rd.reportActivity("Update install failed:" + e.getMessage());
		}
	}
	
	protected List
	splitMultiLine(
		String		indent,
		String		text )
	{
		int		pos = 0;
		
		String	lc_text = text.toLowerCase();
		
		List	lines = new ArrayList();
		
		while( true ){
			
			String	line;
			
			int	p1 = lc_text.indexOf( "<br>", pos );
			
			if ( p1 == -1 ){
				
				line = text.substring(pos);
				
			}else{
				
				line = text.substring(pos,p1);
				
				pos = p1+4;
			}
			
			lines.add( indent + line );
			
			if ( p1 == -1 ){
				
				break;
			}
		}
		
		return( lines );
	}
}
