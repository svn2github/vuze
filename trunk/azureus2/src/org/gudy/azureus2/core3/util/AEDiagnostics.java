/*
 * Created on 22-Sep-2004
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

package org.gudy.azureus2.core3.util;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.logging.*;

/**
 * @author parg
 *
 */

import java.io.*;

public class 
AEDiagnostics 
{
	private static final String	CONFIG_KEY	= "diagnostics.tidy_close";
	
	private static final File	debug_dir		= FileUtil.getUserFile( "debug" );

	private static final File	debug_save_dir	= new File( debug_dir, "save" );

	
	private static boolean	started_up;
	
	public static synchronized void
	startup()
	{
		if ( started_up ){
			
			return;
		}
		
		started_up	= true;
		
		boolean	was_tidy	= COConfigurationManager.getBooleanParameter( CONFIG_KEY );
		
		COConfigurationManager.setParameter( CONFIG_KEY, false );
		
		COConfigurationManager.save();
		
		if ( debug_dir.exists()){
			
			debug_save_dir.mkdir();
			
			File[]	files = debug_dir.listFiles();
			
			if ( files != null ){
				
				boolean	file_moved	= false;
				
				long	now = SystemTime.getCurrentTime();
				
				for (int i=0;i<files.length;i++){
					
					File	file = files[i];
					
					if ( file.isDirectory()){
						
						continue;
					}
					
					if ( !was_tidy ){
			
						file_moved	= true;
						
						FileUtil.renameFile( file, new File( debug_save_dir, now + "_" + file.getName()));
						
					}else{
						
						file.delete();
					}
				}
				
				if ( file_moved ){
					
					LGLogger.logAlertUsingResource(
						LGLogger.AT_WARNING,
						"diagnostics.log_found",
						new String[]{ debug_save_dir.toString() } );
				}
			}
		}else{
			
			debug_dir.mkdir();
		}
	}
	
	public static void
	shutdown()
	{
		COConfigurationManager.setParameter( CONFIG_KEY, true );
		
		COConfigurationManager.save();
	}
}
