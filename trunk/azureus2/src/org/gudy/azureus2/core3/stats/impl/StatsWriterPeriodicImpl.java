/*
 * File    : StatsWriterPeriodicImpl.java
 * Created : 23-Oct-2003
 * By      : parg
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
 
package org.gudy.azureus2.core3.stats.impl;

import java.io.*;

import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.stats.*;
import org.gudy.azureus2.core3.global.*;
import org.gudy.azureus2.core3.config.*;

/**
 * @author parg
 */

public class 
StatsWriterPeriodicImpl 
	implements StatsWriterPeriodic, COConfigurationListener
{
	private static final int		DEFAULT_SLEEP_PERIOD	= 30*1000;
	
	private static StatsWriterPeriodicImpl	singleton;
	private static int				start_count;
	private static Thread			current_thread;
	
	private long			last_write_time	= 0;
	private GlobalManager	global_manager;
	
	private boolean			config_enabled;
	private int				config_period;	
	private String			config_dir;
	private String			config_file;
	
	public static synchronized StatsWriterPeriodic
	create(
		GlobalManager	manager )
	{
		if ( singleton == null ){
			
			singleton = new StatsWriterPeriodicImpl(manager);
		}
		
		return( singleton );	
	}
	
	protected
	StatsWriterPeriodicImpl(
		GlobalManager	manager )
	{
		global_manager	= manager;
		
		COConfigurationManager.addListener( this );	
	}
	
	protected void
	update()
	{
		readConfigValues();
		
		while( true ){
						
			synchronized( singleton ){
				
				if ( Thread.currentThread() != current_thread ){
					
					break;
				}
				
				writeStats();	
			}
			
			try{
				int	period;
				
				if ( !config_enabled ){
					
					period = DEFAULT_SLEEP_PERIOD;
								
				}else{
				
				 	period = config_period*1000;
				}
				
				if ( period > DEFAULT_SLEEP_PERIOD ){
					
					period = DEFAULT_SLEEP_PERIOD;
				}
				
				Thread.sleep( period );
				
			}catch( InterruptedException e ){
				
				e.printStackTrace();
			}
		}
	}
	
	protected void
	readConfigValues()
	{
		config_enabled 	= COConfigurationManager.getBooleanParameter( "Stats Enable", false );
		
		config_period	= COConfigurationManager.getIntParameter( "Stats Period", DEFAULT_SLEEP_PERIOD );
		
		config_dir		= COConfigurationManager.getStringParameter( "Stats Dir", "" );
		
		config_file		= COConfigurationManager.getStringParameter( "Stats File", DEFAULT_STATS_FILE_NAME );
	}
	
	protected void
	writeStats()
	{							
		if ( !config_enabled ){
			
			return;
		}

		int	period = config_period;
		
		long	now = System.currentTimeMillis()/1000;
		
		if ( now - last_write_time < period ){
			
			return;
		}
		
		last_write_time	= now;
		
		try{
			String	dir = config_dir;

			dir = dir.trim();
			
			if ( dir.length() == 0 ){
				
				dir = File.separator;			
			}
			
			String	file_name = dir;
			
			if ( !file_name.endsWith( File.separator )){
				
				file_name = file_name + File.separator;
			}
			
			String	file = config_file;

			if ( file.trim().length() == 0 ){
				
				file = DEFAULT_STATS_FILE_NAME;
			}
			
			file_name += file;
		
			LGLogger.log(0, 0, LGLogger.INFORMATION, "Stats Logged to '" + file_name + "'" );				
			
			new StatsWriterImpl( global_manager ).write( file_name );
			
		}catch( Throwable e ){
		
			e.printStackTrace();
			
			LGLogger.log(0, 0, "Stats Logging fails", e );
		}			
	}
	
	public void
	configurationSaved()
	{
			// only pick up configuration changes when saved
			
		readConfigValues();
		
		writeStats();
	}
	
	public void
	start()
	{
		synchronized( singleton ){
			
			start_count++;
			
			if ( start_count == 1 ){
							
				current_thread = 
					new Thread(){
						public void
						run()
						{
							update();
						}
					};
					
				current_thread.setDaemon( true );
				
				current_thread.start();
			}
		}
	}
	
	public void
	stop()
	{
		synchronized( singleton ){
			
			start_count--;
			
			if ( start_count == 0 ){
				
				current_thread = null;
			}
		}
	}
	

}
