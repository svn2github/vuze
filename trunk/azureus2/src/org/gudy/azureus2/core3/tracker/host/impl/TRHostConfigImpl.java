/*
 * File    : TRHostConfigImpl.java
 * Created : 06-Nov-2003
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

package org.gudy.azureus2.core3.tracker.host.impl;

/**
 * @author parg
 *
 */

import java.util.*;
import java.io.*;
import java.text.*;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.tracker.host.*;

public class 
TRHostConfigImpl 
{
	public static final String	LOG_FILE_NAME	= "tracker.log";
	
	protected TRHostImpl	host;
	
	protected AEMonitor 	save_lock_mon 	= new AEMonitor( "TRHostConfig:SL" );
	
	protected String		log_dir;
	
	protected boolean		loading	= false;

	protected Map			saved_stats		= new HashMap();
	
	protected AEMonitor this_mon 	= new AEMonitor( "TRHostConfig" );

	protected
	TRHostConfigImpl(
		TRHostImpl	_host )
	{
		host	= _host;
		
		log_dir = SystemProperties.getUserPath();
	}
	
	protected void
	loadConfig(
		TRHostTorrentFinder		finder ) 
	{
	   	try{
	   		this_mon.enter();
	   		
	   		loading	= true;
	   		
	   		Map	map = FileUtil.readResilientConfigFile("tracker.config");
		 
			List torrents = (List) map.get("torrents");
		 
			if (torrents == null){
		
				return;
		 	}
		 
		 	Iterator  iter = torrents.iterator();
		 
		 	while (iter.hasNext()) {
		 	
		   		Map t_map = (Map) iter.next();
		   
		   		Long	persistent_l = (Long)t_map.get("persistent");
		   		
		   		boolean	persistent =  persistent_l==null || persistent_l.longValue() == 1;
	
				byte[]	hash = (byte[])t_map.get("hash");

		   		if ( persistent ){	
			 
				 	int	state = ((Long)t_map.get("status")).intValue();				 	
				 	
				 	if ( state == TRHostTorrent.TS_FAILED ){
				 		
				 		state = TRHostTorrent.TS_STOPPED;
				 	}
				 	
				 	TOTorrent	torrent = finder.lookupTorrent( hash );
				 	
				 	if ( torrent != null ){
				 		
				 		TRHostTorrent	ht = host.addTorrent( torrent, state, true );
				 		
				 		if ( ht instanceof TRHostTorrentHostImpl ){
				 			
				 			TRHostTorrentHostImpl	hth = (TRHostTorrentHostImpl)ht;
				 			
				 			recoverStats( hth, t_map );
				 		}
				 	
				 	}else{
						if ( COConfigurationManager.getBooleanParameter( "Tracker Public Enable", false )){
			 		
				 			host.addExternalTorrent( hash, state );
						}
				 	}
		   		}else{
		   			
		   				// store stats for later
		   			
		   			saved_stats.put( new HashWrapper( hash ), t_map );
		   		}
		   	}
		 	
	   	}catch (Exception e) {
		 
	   		Debug.printStackTrace( e );
	   	}finally{
		 	
	   		loading	= false;
	   		
	   		this_mon.exit();
	   	}
	}

	protected void
	recoverStats(
		TRHostTorrentHostImpl	host_torrent )
	{
		try{
			HashWrapper	hash = host_torrent.getTorrent().getHashWrapper();
		
			Map	t_map = (Map)saved_stats.get( hash );
			
			if ( t_map != null ){
				
				saved_stats.remove( hash );
				
				recoverStats( host_torrent, t_map );
			}
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	
	protected void
	recoverStats(
		TRHostTorrentHostImpl	host_torrent,
		Map						t_map )
	{
	 	long	completed	= 0;
	 	long	announces	= 0;
	 	long	scrapes		= 0;
	 	long	total_up	= 0;
	 	long	total_down	= 0;
	 	long	bytes_in	= 0;
	 	long	bytes_out	= 0;
	 	
	 	Map	s_map	= (Map)t_map.get( "stats" );
	 	
	 	if ( s_map != null ){
	 	
	 		completed 	= ((Long)s_map.get( "completed")).longValue();
	 		announces	= ((Long)s_map.get( "announces")).longValue();
	 		total_up	= ((Long)s_map.get( "uploaded")).longValue();
	 		total_down	= ((Long)s_map.get( "downloaded")).longValue();
	 		
	 		Long	scrapes_l = (Long)s_map.get( "scrapes" );
	 		if ( scrapes_l != null ){		 			
	 			scrapes	= scrapes_l.longValue();
	 		}
	 		Long	bytes_in_l = (Long)s_map.get( "bytesin" );
	 		if ( bytes_in_l != null ){		 			
	 			bytes_in	= bytes_in_l.longValue();
	 		}
	 		Long	bytes_out_l = (Long)s_map.get( "bytesout" );
	 		if ( bytes_out_l != null ){		 			
	 			bytes_out	= bytes_out_l.longValue();
	 		}
	 	}
	 	
	 	host_torrent.setStartOfDayValues( completed, announces, scrapes, total_up, total_down, bytes_in, bytes_out );
	}
	
	protected void
	saveConfig()
	{
		if( loading ){
			return;
		}
				
		try{
		   	Map map = new HashMap();
		   
		   	List list = new ArrayList();
		   
		   	TRHostTorrent[]	torrents = host.getTorrents();
		   
		   	List	stats_entries = new ArrayList();
		   	
		   	for (int i = 0; i < torrents.length; i++){
		   	
		  	 	try{
		  
					TRHostTorrent torrent = (TRHostTorrent)torrents[i];
					
					StringBuffer	stats_entry = new StringBuffer(2048);
					
					byte[]	hash 		= torrent.getTorrent().getHash();
					byte[]	name		= torrent.getTorrent().getName();
					int		status 		= torrent.getStatus();
					long	completed	= torrent.getCompletedCount();
					long	announces	= torrent.getAnnounceCount();
					long	scrapes		= torrent.getScrapeCount();
					long	uploaded	= torrent.getTotalUploaded();
					long	downloaded	= torrent.getTotalDownloaded();
					long	bytes_in	= torrent.getTotalBytesIn();
					long	bytes_out	= torrent.getTotalBytesOut();
										
					int	seed_count 		= torrent.getSeedCount();
					int non_seed_count	= torrent.getLeecherCount();
										
				
					Map t_map = new HashMap();
				
					t_map.put("persistent",new Long(torrent.isPersistent()?1:0));
					
					t_map.put("hash", hash );
				
					t_map.put("status", new Long(status ));
	
					list.add(t_map);
				
					Map	s_map = new HashMap();
				
					t_map.put( "stats", s_map );
				
					s_map.put( "completed", new Long(completed));
					s_map.put( "announces", new Long(announces));
					s_map.put( "scrapes", new Long(scrapes));
					s_map.put( "uploaded", new Long(uploaded));
					s_map.put( "downloaded", new Long(downloaded));
					s_map.put( "bytesin", new Long(bytes_in));
					s_map.put( "bytesout", new Long(bytes_out));

					
					stats_entry.append( new String(name, Constants.DEFAULT_ENCODING ));
					stats_entry.append(",");
					stats_entry.append( ByteFormatter.nicePrint(hash,true));
					stats_entry.append(",");
					stats_entry.append(status);
					stats_entry.append(",");
					stats_entry.append(seed_count);
					stats_entry.append(",");
					stats_entry.append(non_seed_count);
					stats_entry.append(",");
					stats_entry.append(completed);
					stats_entry.append(",");
					stats_entry.append(announces);
					stats_entry.append(",");
					stats_entry.append(scrapes);
					stats_entry.append(",");
					stats_entry.append(DisplayFormatters.formatByteCountToKiBEtc(uploaded));
					stats_entry.append(",");
					stats_entry.append(DisplayFormatters.formatByteCountToKiBEtc(downloaded));
					stats_entry.append(",");
					stats_entry.append(DisplayFormatters.formatByteCountToKiBEtcPerSec(torrent.getAverageUploaded()));
					stats_entry.append(",");
					stats_entry.append(DisplayFormatters.formatByteCountToKiBEtcPerSec(torrent.getAverageDownloaded()));
					stats_entry.append(",");
					stats_entry.append(DisplayFormatters.formatByteCountToKiBEtc( torrent.getTotalLeft()));
					stats_entry.append(",");
					stats_entry.append(DisplayFormatters.formatByteCountToKiBEtc( bytes_in ));
					stats_entry.append(",");
					stats_entry.append(DisplayFormatters.formatByteCountToKiBEtc( bytes_out ));
					stats_entry.append(",");
					stats_entry.append(DisplayFormatters.formatByteCountToKiBEtcPerSec(torrent.getAverageBytesIn()));
					stats_entry.append(",");
					stats_entry.append(DisplayFormatters.formatByteCountToKiBEtcPerSec(torrent.getAverageBytesOut()));
					
					stats_entry.append( "\r\n");
					
					stats_entries.add( stats_entry );
				 	
		  	 	}catch( TOTorrentException e ){
		  	 		
		  	 		Debug.printStackTrace( e );
		  	 	}
		   	}
		   	
		   	map.put("torrents", list);
		   	
		   try{
		   		save_lock_mon.enter();
		   		
		   		FileUtil.writeResilientConfigFile( "tracker.config", map );
			   	
				if ( 	COConfigurationManager.getBooleanParameter( "Tracker Log Enable", false ) &&
						stats_entries.size() > 0 ){
			   		
				   	try{
				   		String timeStamp = "["+new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").format(new Date())+"] ";
				   		
				   		PrintWriter	pw = null;
				   		
				   		File	file_name = new File( log_dir.concat(File.separator).concat(LOG_FILE_NAME) );
				   		
				   		try{		
				   			
				   			pw = new PrintWriter(new FileWriter( file_name, true ));
				   
				   			for (int i=0;i<stats_entries.size();i++){
				   				
				   				StringBuffer	stats_entry = (StringBuffer)stats_entries.get(i);
				   				
				   				String str = timeStamp + stats_entry.toString();
				   					
				   				pw.print( str );
				   			}
				   			
				   		}catch( Throwable e ){
				   			
				   			Debug.printStackTrace( e );
				   			
				   		}finally{
				   			
				   			if ( pw != null ){
				   				
				   				try{
				   					
				   					pw.close();
				   					
				   				}catch( Throwable e ){
				   				}
				   			}
				   		}
				   	}catch( Throwable e ){
				   		Debug.printStackTrace( e );
				   	}
			   	}
		   	}finally{
		   		
		   		save_lock_mon.exit();
		   	}
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
		}
	}
	
	private static String format(int n) {
		if(n < 10) return "0".concat(String.valueOf(n)); //$NON-NLS-1$
		return String.valueOf(n); //$NON-NLS-1$
	}  
	
}
