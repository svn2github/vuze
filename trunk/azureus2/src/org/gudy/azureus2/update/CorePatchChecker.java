/*
 * Created on 25-May-2004
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

package org.gudy.azureus2.update;

/**
 * @author parg
 *
 */

import java.io.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.logging.*;
	
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.update.*;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;


public class 
CorePatchChecker
	implements Plugin, UpdatableComponent, UpdateCheckInstanceListener
{
	public static final boolean	TESTING	= false;
	
	public void 
	initialize(
		PluginInterface pi )
	  
	  	throws PluginException
	{
		if ( TESTING || !Constants.isCVSVersion()){
		
			if ( TESTING ){
				
				System.out.println( "CorePatchChecker: TESTING !!!!" );
			}
			
			pi.getUpdateManager().registerUpdatableComponent( this, false );
		}
	}
	
	public String
	getName()
	{
		return( "Core Patch Checker");
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
			UpdateCheckInstance	inst = checker.getCheckInstance();
		
			inst.addListener( this );
		
			checker.addUpdate( "Core Patch Checker", new String[0], "",
								new ResourceDownloader[0],
								Update.RESTART_REQUIRED_MAYBE );
		}finally{
			
			checker.completed();
		}
	}
	
	public void
	cancelled(
		UpdateCheckInstance		instance )
	{
	}
	
	public void
	complete(
		final UpdateCheckInstance		instance )
	{
		Update[]	updates = instance.getUpdates();
		
		final PluginInterface updater_plugin = PluginManager.getPluginInterfaceByClass( UpdaterUpdateChecker.class );
		
		for (int i=0;i<updates.length;i++){
			
			final Update	update = updates[i];
						
			Object	user_object = update.getUserObject();
			
			if ( user_object != null && user_object == updater_plugin ){
				
				// OK, we have an updater update
				
				LGLogger.log( "Core Patcher: updater update found" );
				
				update.setRestartRequired( Update.RESTART_REQUIRED_MAYBE );
				
				update.addListener(
						new UpdateListener()
						{
							public void
							complete(
								Update	update )
							{
								LGLogger.log( "Core Patcher: updater update complete" );
								
								patch( instance, update, updater_plugin );
							}
						});
			}
		}
	}
	
	protected void
	patch(
		UpdateCheckInstance		instance,
		Update					updater_update,
		PluginInterface 		updater_plugin )
	{
		try{
				// use the update plugin to log stuff....
			
			ResourceDownloader rd_log = updater_update.getDownloaders()[0];

			File[]	files = new File(updater_plugin.getPluginDirectoryName()).listFiles();
			
			if ( files == null ){
			
				LGLogger.log( "Core Patcher: no files in plugin dir!!!" );
			
				return;
			}
		
			String	patch_prefix = "Azureus2_" + Constants.getBaseVersion() + "_P";
			
			int		highest_p		= -1;
			File	highest_p_file 	= null;
			
			for (int i=0;i<files.length;i++){
			
				String	name = files[i].getName();
				
				if ( name.startsWith( patch_prefix ) && name.endsWith( ".pat" )){
			
					LGLogger.log( "Core Patcher: found patch file '" + name + "'" );
					
					try{
						int	this_p = Integer.parseInt( name.substring( patch_prefix.length(), name.indexOf( ".pat" )));
						
						if ( this_p > highest_p ){
							
							highest_p = this_p;
							
							highest_p_file	= files[i];
						}
					}catch( Throwable e ){
						
						e.printStackTrace();
					}
				}
			}
			
			if ( CorePatchLevel.getCurrentPatchLevel() >= highest_p ){
				
				LGLogger.log( "Core Patcher: no applicable patch found (highest = " + highest_p + ")" );
				
			}else{
				
				rd_log.reportActivity( "Applying patch '" + highest_p_file.getName() + "'");
				
				LGLogger.log( "Core Patcher: applying patch '" + highest_p_file.toString() + "'" );
				
				UpdateInstaller	installer = instance.createInstaller();
				
				File	tmp = File.createTempFile("AZU", null );
								
				OutputStream	os = new FileOutputStream( tmp );
				
				String	az2_jar;

				if( Constants.isOSX ){
  
					az2_jar = installer.getInstallDir() + "/Azureus.app/Contents/Resources/Java/";
					
				}else{

					az2_jar = installer.getInstallDir() + File.separator;
				}
				
				az2_jar	+= "Azureus2.jar";				
				
				InputStream	is 	= new FileInputStream( az2_jar );
				
				InputStream pis = new FileInputStream( highest_p_file );
				
				new UpdateJarPatcher( is, pis, os );
				
				is.close();
				
				pis.close();
				
				os.close();
				
				String	resource_name = "Azureus2_P" + highest_p + ".jar";
				
				installer.addResource(  resource_name,
										new FileInputStream( tmp ));
				
				tmp.delete();
				
				installer.addMoveAction( resource_name, az2_jar );
				
				LGLogger.logAlert( 	LGLogger.AT_COMMENT,
									"Patch " + highest_p_file.getName() + " ready to be applied" );
				
				String done_file = highest_p_file.toString();
				
				done_file = done_file.substring(0,done_file.length()-1) + "x";
				
				highest_p_file.renameTo( new File( done_file ));
				
					// flip the original update over to 'restart required'
				
				updater_update.setRestartRequired( Update.RESTART_REQUIRED_YES );
			}
		}catch( Throwable e ){
			
			e.printStackTrace();
			
			LGLogger.logAlert( 	"Core Patcher failed", e );
		}
	}
}
