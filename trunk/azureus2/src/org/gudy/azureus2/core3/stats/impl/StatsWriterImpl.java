/*
 * File    : StatsWriterImpl.java
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

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.stats.*;
import org.gudy.azureus2.core3.global.*;
import org.gudy.azureus2.core3.config.*;

/**
 * @author parg
 */

public class 
StatsWriterImpl 
	implements StatsWriter
{
	private static final int		SLEEP_PERIOD	= 60*1000;
	private static final String		STATS_FILE_NAME	= Constants.AZUREUS_NAME + "_Stats.xml";
	
	private static StatsWriterImpl	singleton;
	private static int				start_count;
	private static Thread			current_thread;
	
	public static synchronized StatsWriter
	create(
		GlobalManager	manager )
	{
		if ( singleton == null ){
			
			singleton = new StatsWriterImpl();
		}
		
		return( singleton );	
	}
	
	protected void
	update()
	{
		while( true ){
			
			synchronized( singleton ){
				
				if ( Thread.currentThread() != current_thread ){
					
					break;
				}
				
				if ( COConfigurationManager.getBooleanParameter( "Stats Enable", false )){
					
					writeStats();
				}	
			}
			
			try{
				Thread.sleep( SLEEP_PERIOD );
				
			}catch( InterruptedException e ){
				
				e.printStackTrace();
			}
		}
	}
	
	protected void
	writeStats()
	{
		try{
			PrintWriter	writer = null;
			
			try{
				String	dir = COConfigurationManager.getStringParameter( "Stats Dir", "" );
	
				dir = dir.trim();
				
				if ( dir.length() == 0 ){
					
					dir = File.separator;			
				}
				
				String	file_name = dir;
				
				if ( !file_name.endsWith( File.separator )){
					
					file_name = file_name + File.separator;
				}
				
				file_name += STATS_FILE_NAME;
			
				LGLogger.log(0, 0, LGLogger.INFORMATION, "Stats Logged to '" + file_name + "'" );
				
				writer = new PrintWriter( new FileOutputStream( file_name ));
				
				writer.println( "<STATS>");
				
				writer.println( "</STATS>");
				
			}catch( IOException e ){
			
				LGLogger.log(0, 0, "Stats Logging fails", e );
				
			}finally{
				
				if ( writer != null ){
					
					writer.close();
				}
			}
		}catch( Throwable e ){
		
			LGLogger.log(0, 0, "Stats Logging fails", e );		
		}
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
