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

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.tracker.host.*;

public class 
TRHostConfigImpl 
{
	public static final String	LOG_FILE_NAME	= "tracker.log";
	
	protected TRHostImpl	host;
	
	protected Object		save_lock	= new Object(){};
	
	protected String		log_dir;
	
	protected boolean		loading	= false;
	
	protected
	TRHostConfigImpl(
		TRHostImpl	_host )
	{
		host	= _host;
		
		log_dir = FileUtil.getApplicationPath();
	}
	
	protected synchronized void
	loadConfig(
		TRHostTorrentFinder		finder ) 
	{
		FileInputStream 		fin = null;
		
		BufferedInputStream 	bin = null;
	   
	   	try{
	   		loading	= true;
	   		
			File configFile = FileUtil.getApplicationFile("tracker.config");
		 
			fin = new FileInputStream(configFile);
		 
			bin = new BufferedInputStream(fin, 8192);
		 
			Map map = BDecoder.decode(bin);
		 
			List torrents = (List) map.get("torrents");
		 
			if (torrents == null){
		
				return;
		 	}
		 
		 	Iterator  iter = torrents.iterator();
		 
		 	while (iter.hasNext()) {
		 	
		   		Map t_map = (Map) iter.next();
		   
 				byte[]	hash = (byte[])t_map.get("hash");
			 
			 	int	state = ((Long)t_map.get("status")).intValue();
			 	
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
			 	
			 	if ( state == TRHostTorrent.TS_FAILED ){
			 		
			 		state = TRHostTorrent.TS_STOPPED;
			 	}
			 	
			 	TOTorrent	torrent = finder.lookupTorrent( hash );
			 	
			 	if ( torrent != null ){
			 		
			 		TRHostTorrent	ht = host.addTorrent( torrent, state, true );
			 		
			 		if ( ht instanceof TRHostTorrentHostImpl ){
			 			
			 			TRHostTorrentHostImpl	hth = (TRHostTorrentHostImpl)ht;
			 			
			 			hth.setCompletedCount( (int)completed );
			 			hth.setAnnounceCount( (int)announces );
			 			hth.setScrapeCount( (int)scrapes );
			 			hth.setTotalUploaded( total_up );
			 			hth.setTotalDownloaded( total_down );
			 			hth.setTotalBytesIn( bytes_in );
			 			hth.setTotalBytesOut( bytes_out );
			 		}
			 	
			 	}else{
					if ( COConfigurationManager.getBooleanParameter( "Tracker Public Enable", false )){
		 		
			 			host.addExternalTorrent( hash, state );
					}
			 	}
		   	}
		}catch (FileNotFoundException e) {
			
		 	//Do nothing
		 	
	   	}catch (Exception e) {
		 
			e.printStackTrace();
			  
	   	}finally{
	   		
			try{
		   		if (bin != null)
					 bin.close();
		 	}catch (Exception e) {}
		 	
		 	try{
		   		if (fin != null)
			 		fin.close();
		 	}catch (Exception e) {}
		 	
		 	loading	= false;
	   	}
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
					
					if ( torrent.isPersistent()){
	
						StringBuffer	stats_entry = new StringBuffer(2048);
						
						Map t_map = new HashMap();
				 	
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
						
						TRHostPeer[]	peers = torrent.getPeers();
						
						int	seed_count 		= 0;
						int non_seed_count	= 0;
						
						for (int j=0;j<peers.length;j++){
							
							if ( peers[j].isSeed()){
								
								seed_count++;
								
							}else{
								
								non_seed_count++;
							}
						}
						
						
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
					}
				 	
		  	 	}catch( TOTorrentException e ){
		  	 		
		  	 		e.printStackTrace();
		  	 	}
		   	}
		   	
		   	map.put("torrents", list);
		   	
		   	synchronized( save_lock ){
		   		
			   		//open a file stream
			   		
			   	FileOutputStream fos = null;
			   	
			   	try{
			   			//encode the data
			   		
			   		byte[] torrentData = BEncoder.encode(map);
			   		
			   		fos = new FileOutputStream(FileUtil.getApplicationFile("tracker.config"));
				 	
				 		//write the data out
				 		
				 	fos.write(torrentData);
				 	
			   	}catch ( Throwable e){
			   		
				 	e.printStackTrace();
				 	
			   	}finally{
			   		
				 	try {
				   		if (fos != null)
					 		fos.close();
				 	}catch (Exception e) {}
			   	}
			   	
			   	if ( stats_entries.size() > 0 ){
			   		
				   	try{
				   		Calendar now = GregorianCalendar.getInstance();
				   		
				   		String timeStamp =
				   		"[".concat(String.valueOf(now.get(Calendar.HOUR_OF_DAY))).concat(":").concat(format(now.get(Calendar.MINUTE))).concat(":").concat(format(now.get(Calendar.SECOND))).concat("] ");    
				   		
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
				   			
				   			e.printStackTrace();
				   			
				   		}finally{
				   			
				   			if ( pw != null ){
				   				
				   				try{
				   					
				   					pw.close();
				   					
				   				}catch( Throwable e ){
				   				}
				   			}
				   		}
				   	}catch( Throwable e ){
				   		e.printStackTrace();
				   	}
			   	}
		   	}
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	private static String format(int n) {
		if(n < 10) return "0".concat(String.valueOf(n)); //$NON-NLS-1$
		return String.valueOf(n); //$NON-NLS-1$
	}  
	
}
