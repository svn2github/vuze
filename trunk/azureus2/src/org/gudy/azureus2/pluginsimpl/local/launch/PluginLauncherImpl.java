/*
 * Created on 25-Jul-2005
 * Created by Paul Gardner
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.pluginsimpl.local.launch;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.plugins.LaunchablePlugin;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;

public class 
PluginLauncherImpl 
{
	public static void
	launch(
		String[]		args )
	{
		final LoggerChannelListener	listener =
			new LoggerChannelListener()
			{
				public void
				messageLogged(
					int		type,
					String	content )
				{
					log(  content );

				}
				
				public void
				messageLogged(
					String		str,
					Throwable	error )
				{
					log(  str );
					
					StringWriter	sw = new StringWriter();
					
					PrintWriter		pw = new PrintWriter( sw );
					
					error.printStackTrace( pw );
					
					pw.flush();
					
					log( sw.toString());
				}
				
				protected synchronized void
				log(
					String	str )
				{
				    File	log_file	 = FileUtil.getApplicationFile("launch.log");

				    PrintWriter	pw = null;
				    
				    try{
						pw = new PrintWriter(new FileWriter( log_file, true ));

						if ( str.endsWith( "\n" )){
							
							System.err.print( "PluginLauncher: " + str );
							
							pw.print( str );
							
						}else{
							
							System.err.println( "PluginLauncher: " + str );
							
							pw.println( str );
						}
						
				    }catch( Throwable e ){
				    	
				    }finally{
				    	
				    	if ( pw != null ){
				    		
				    		pw.close();
				    	}
				    }
				}
			};
			
		LaunchablePlugin[]	launchables = PluginInitializer.findLaunchablePlugins(listener);
		
		if ( launchables.length == 0 ){
			
			listener.messageLogged( LoggerChannel.LT_ERROR, "No launchable plugins found" );
			
			return;
			
		}else if ( launchables.length > 1 ){
			
			listener.messageLogged( LoggerChannel.LT_ERROR, "Multiple launchable plugins found, running first" );
		}
		
		try{
				// set default details for restarter
			
			SystemProperties.setApplicationEntryPoint( "org.gudy.azureus2.plugins.PluginLauncher" );

			launchables[0].setDefaults( args );			

				// we have to run the core startup on a separate thread and then effectively pass "this thread"
				// through to the launchable "process" method
			
			Thread core_thread = 
				new Thread( "PluginLauncher" )
				{
					public void
					run()
					{
						try{
								// give 'process' call below some time to start up
							
							Thread.sleep(500);
							
							AzureusCore azureus_core = AzureusCoreFactory.create();

							azureus_core.start();
							
						}catch( Throwable e ){
							
							listener.messageLogged( "PluginLauncher: launch fails", e );
						}					
					}
				};
			
			core_thread.setDaemon( true );
			
			core_thread.start();
			
			boolean	restart = false;
			
			try{
				restart = launchables[0].process();
				
			}finally{
				
				if ( restart ){
					
					AzureusCoreFactory.getSingleton().restart();

				}else{
					
					AzureusCoreFactory.getSingleton().stop();
				}
			}
			
		}catch( Throwable e ){
			
			listener.messageLogged( "PluginLauncher: launch fails", e );
		}
	}
}
