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
import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.config.*;

/**
 * @author parg
 */

public class 
StatsWriterImpl 
	implements StatsWriter, COConfigurationListener
{
	private static final int		DEFAULT_SLEEP_PERIOD	= 30*1000;
	
	private static StatsWriterImpl	singleton;
	private static int				start_count;
	private static Thread			current_thread;
	
	private long			last_write_time	= 0;
	private GlobalManager	global_manager;
	
	private boolean			config_enabled;
	private int				config_period;	
	private String			config_dir;
	private String			config_file;
	
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
			
			new statsWriter( global_manager, file_name ).write();
			
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
		protected String			file_name;
		
		protected
		statsWriter(
			GlobalManager		_global,
			String				_fn )
		{			
			global		= _global;
			file_name	= _fn;
		}
		
		protected void
		write()
		
			throws IOException
		{
			try{
				setOutputStream( new FileOutputStream( file_name ));
				
				writeLine( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" );

				writeLine( "<STATS>");
			
				try{
					indent();
				
					writeLine( "<GLOBAL>" );
					
					try{
						indent();
						
						GlobalManagerStats	gm_stats = global.getStats();
											
						writeRawCookedAverageTag( "DOWNLOAD_SPEED", gm_stats.getDownloadAverage() );
						writeRawCookedAverageTag( "UPLOAD_SPEED", 	gm_stats.getUploadAverage() );
						
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
								indent();
								
								writeLine( "<TORRENT>" );

								TOTorrent torrent = dm.getTorrent();
																	
								try{
									indent();
							
									writeTag( "NAME", dm.getName());
									
									writeTag( "TORRENT_FILE", dm.getTorrentFileName());
									
														if ( torrent != null ){
										
										writeTag( "PIECE_LENGTH", torrent.getPieceLength());
										
										writeTag( "PIECE_COUNT", torrent.getPieces().length );
										
										writeTag( "FILE_COUNT", torrent.getFiles().length );
										
										writeTag( "COMMENT", torrent.getComment());
										
										writeTag( "CREATED_BY", torrent.getCreatedBy());
										
										writeTag( "CREATION_DATE", torrent.getCreationDate());
									}
									
								}finally{
									
									exdent();
								}
								
								writeLine( "</TORRENT>");
								
								writeTag( "DOWNLOAD_STATUS", DisplayFormatters.formatDownloadStatusDefaultLocale( dm));
								
								writeTag( "DOWNLOAD_DIR", dm.getSavePath());
								
								DiskManager	disk_manager = dm.getDiskManager();
								
								if ( disk_manager != null ){
								
									if ( torrent.isSimpleTorrent()){
									
										writeTag( "TARGET_FILE", disk_manager.getFileName());
										
									}else{
										
										writeTag( "TARGET_DIR", disk_manager.getFileName());
									}
								}
								
								writeTag( "TRACKER_STATUS", dm.getTrackerStatus());
							
								writeTag( "COMPLETED", 		dm_stats.getCompleted());
								
								writeRawCookedTag( "DOWNLOADED", 		dm_stats.getDownloaded());
								writeRawCookedTag( "UPLOADED", 			dm_stats.getUploaded());
								writeRawCookedTag( "DISCARDED", 		dm_stats.getDiscarded());
								
								writeRawCookedAverageTag( "DOWNLOAD_SPEED", 	dm_stats.getDownloadAverage());
								writeRawCookedAverageTag( "UPLOAD_SPEED", 		dm_stats.getUploadAverage());
								writeRawCookedAverageTag( "TOTAL_SPEED", 		dm_stats.getTotalAverage());
																						
								writeTag( "ELAPSED", 		dm_stats.getElapsedTime());
								writeTag( "ETA", 			dm_stats.getETA());
								writeTag( "HASH_FAILS", 	dm_stats.getHashFails());
								writeTag( "SHARE_RATIO", 	dm_stats.getShareRatio());
					
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
				
			}finally{
				
				closeOutputStream();
			}
		}
		
		protected void
		writeRawCookedTag(
			String	tag,
			long	raw )
		{
			writeLine( "<" + tag + ">");
							
			try{
				indent();
								
				writeTag( "TEXT",	DisplayFormatters.formatByteCountToKBEtc( raw ));
				writeTag( "RAW",	raw);
								
			}finally{
								
				exdent();
			}
							
			writeLine( "</" + tag + ">");
		}
		
		protected void
		writeRawCookedAverageTag(
			String	tag,
			long	raw )
		{
			writeLine( "<" + tag + ">");
							
			try{
				indent();
								
				writeTag( "TEXT",	DisplayFormatters.formatByteCountToKBEtcPerSec( raw ));
				writeTag( "RAW",	raw);
								
			}finally{
								
				exdent();
			}
									
			writeLine( "</" + tag + ">");
		}
	}
}
