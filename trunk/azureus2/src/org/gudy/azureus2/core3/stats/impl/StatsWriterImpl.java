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

import java.util.*;
import java.io.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.xml.util.*;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.stats.*;
import org.gudy.azureus2.core3.global.*;
import org.gudy.azureus2.core3.download.*;
import org.gudy.azureus2.core3.config.*;

/**
 * @author parg
 */

public class 
StatsWriterImpl 
	implements StatsWriter, COConfigurationListener
{
	private static final int		DEFAULT_SLEEP_PERIOD	= 30*1000;
	
	private static final String		STATS_FILE_NAME	= Constants.AZUREUS_NAME + "_Stats.xml";
	
	private static StatsWriterImpl	singleton;
	private static int				start_count;
	private static Thread			current_thread;
	
	private long			last_write_time	= 0;
	private GlobalManager	global_manager;
	
	
	public static synchronized StatsWriter
	create(
		GlobalManager	manager )
	{
		if ( singleton == null ){
			
			singleton = new StatsWriterImpl(manager);
		}
		
		return( singleton );	
	}
	
	protected
	StatsWriterImpl(
		GlobalManager	manager )
	{
		global_manager	= manager;
		
		COConfigurationManager.addListener( this );	
	}
	
	protected void
	update()
	{
		while( true ){
						
			synchronized( singleton ){
				
				if ( Thread.currentThread() != current_thread ){
					
					break;
				}
				
				writeStats();	
			}
			
			try{
				int	period;
				
				if (!COConfigurationManager.getBooleanParameter( "Stats Enable", false )){
					
					period = DEFAULT_SLEEP_PERIOD;
								
				}else{
				
				 	period = COConfigurationManager.getIntParameter( "Stats Period", DEFAULT_SLEEP_PERIOD )*1000;
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
	writeStats()
	{							
		if ( !COConfigurationManager.getBooleanParameter( "Stats Enable", false )){
			
			return;
		}

		int	period = COConfigurationManager.getIntParameter( "Stats Period", DEFAULT_SLEEP_PERIOD );
		
		long	now = System.currentTimeMillis()/1000;
		
		if ( now - last_write_time < period ){
			
			return;
		}
		
		last_write_time	= now;
		
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
				
				new statsWriter( global_manager, writer ).write();
				
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
	configurationSaved()
	{
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
	
	protected static class
	statsWriter
		extends XUXmlWriter
	{
		protected GlobalManager		global;
		
		protected
		statsWriter(
			GlobalManager		_global,
			PrintWriter			_writer )
		{
			super( _writer );
			
			global		= _global;
		}
		
		protected void
		write()
		{
			writeLine( "<STATS>");
		
			try{
				indent();
			
				writeLine( "<GLOBAL>" );
				
				try{
					indent();
					
					writeTag( "DOWNLOAD_SPEED", global.getDownloadSpeed());
					writeTag( "UPLOAD_SPEED", global.getUploadSpeed());
					
				}finally{
					
					exdent();
				}
			
				writeLine( "</GLOBAL>" );
				
				writeLine( "<DOWNLOADS>");
				
				try{
					
					indent();
					
					List	dms = global.getDownloadManagers();
				
					for (int i=0;i<dms.size();i++){
						
						DownloadManager	dm = (DownloadManager)dms.get(i);
						
						DownloadManagerStats	dm_stats = dm.getStats();
						
						writeLine( "<DOWNLOAD>");
						
						try{
							
							writeTag( "NAME", dm.getName());
							writeTag( "TORRENT", dm.getTorrentFileName());
							writeTag( "TRACKER_STATUS", dm.getTrackerStatus());
						
							writeTag( "COMPLETED", 		dm_stats.getCompleted());
							writeTag( "DOWNLOADED",	 	dm_stats.getDownloaded());	
							writeTag( "UPLOADED", 		dm_stats.getUploaded());
							writeTag( "SHARE_RATIO", 	dm_stats.getShareRatio());
							writeTag( "DOWNLOAD_SPEED", dm_stats.getDownloadSpeed());
							writeTag( "UPLOAD_SPEED", 	dm_stats.getUploadSpeed());
							writeTag( "ELAPSED", 		dm_stats.getElapsed());
							writeTag( "ETA", 			dm_stats.getETA());
							writeTag( "TOTAL_SPEED", 	dm_stats.getTotalSpeed());
							writeTag( "HASH_FAILS", 	dm_stats.getHashFails());
				
							indent();
						}finally{
							
							exdent();
						}
						
						writeLine( "</DOWNLOAD>");
					}
					
				}finally{
					
					exdent();
				}
					
				writeLine( "</DOWNLOADS>" );
				
			}finally{
				
				exdent();
			}
			writeLine( "</STATS>");	
		}
	}
}
