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
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;
import org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader.*;
import org.gudy.azureus2.plugins.update.*;
import org.gudy.azureus2.pluginsimpl.local.update.*;

public class 
PlatformManagerUpdateChecker 
{
	
	public static final String	UPDATE_DIR				= Constants.SF_WEB_SITE + "update2/";
	public static final String	UPDATE_PROPERTIES_FILE	= UPDATE_DIR + PlatformManagerImpl.DLL_NAME + ".dll.properties";
	
	protected
	PlatformManagerUpdateChecker(
		final PlatformManagerImpl		platform )
	{
		Thread	t = 
			new Thread("Win32PlatformManager:updateChecker")
			{
				public void
				run()
				{
					check( platform );
				}
			};
		
		t.setDaemon( true );
		
		t.start();
	}
	
	protected void
	check(
		PlatformManagerImpl		platform )
	{
		String	current_dll_version = platform==null?"1.0":platform.getVersion();
		
		LGLogger.log( "PlatformManager:Win32 update check starts: current = " + current_dll_version );
		
		try{
			ResourceDownloaderFactory rf = ResourceDownloaderFactoryImpl.getSingleton();
			
			ResourceDownloader	rd = rf.create( new URL(UPDATE_PROPERTIES_FILE));

			rd	= rf.getRetryDownloader( rd, 5 );
			
			String  current_az_version 	= Constants.getBaseVersion();
			boolean current_az_is_cvs	= Constants.isCVSVersion();
			
			InputStream is = rd.download();
			
			Properties	props = new Properties();
			
			props.load( is );
			
			String	target_dll_version	= null;
			
			Iterator it = props.keySet().iterator();
			
				// find the most recent version
			
			while( it.hasNext()){
				
				String	this_az_version 	= (String)it.next();
				String	this_dll_version	= (String)props.get(this_az_version);
				
				String	this_base 	= Constants.getBaseVersion( this_az_version );
				boolean	this_cvs	= Constants.isCVSVersion( this_az_version );
				
				if ( current_az_is_cvs != this_cvs ){
					
					continue;
				}
				
				if ( Constants.compareVersions( this_base, current_az_version ) == 0 ){
					
					if ( Constants.compareVersions( this_dll_version, current_dll_version ) > 0 ){
						
						target_dll_version	= this_dll_version;
						
						break;
					}
				}
			}
		
			LGLogger.log( "PlatformManager:Win32 update required = " + (target_dll_version!=null));
			
			if ( target_dll_version != null ){
				
				String	target = UPDATE_DIR + PlatformManagerImpl.DLL_NAME + "_" + target_dll_version + ".dll";
				
				ResourceDownloader dll_rd = rf.create( new URL( target ));
							
				LGLogger.log( "PlatformManager:Win32 downloaded" );
				
				UpdateManagerImpl.getSingleton().addUpdate(
						PlatformManagerImpl.DLL_NAME + ".dll :" + target_dll_version,
						dll_rd,
						Update.RESTART_REQUIRED_YES );
			}
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
}
