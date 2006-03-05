/*
 * File    : StatsStorage.java
 * Created : 2 mars 2004
 * By      : Olivier
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
package org.gudy.azureus2.core3.stats.transfer.impl;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerStats;
import org.gudy.azureus2.core3.global.impl.GlobalManagerAdpater;
import org.gudy.azureus2.core3.stats.transfer.OverallStats;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.util.*;


/**
 * @author Olivier
 * 
 */
public class 
OverallStatsImpl 
	extends GlobalManagerAdpater 
	implements OverallStats, TimerEventPerformer
{
  private static final String[]	exts = { "mp3;ogg;wav;wma;flac", "avi;mpg;mpeg;wmv;vob;mp4;divx;mov", "zip;rar;iso;bin;tar;sit" };
  private static Set[]	ext_sets;
  
  	// sizes in MB
  
  private long[]	file_sizes = { 400, 800, 1600 };
  
  static{
	ext_sets = new Set[exts.length];
	  
	for (int i=0;i<exts.length;i++){
		
		StringTokenizer	tok = new StringTokenizer( exts[i], ";" );
		
		Set	set = ext_sets[i] = new HashSet();
		
		while( tok.hasMoreTokens()){
			
			set.add( tok.nextToken());
		}
	}
  }
  
  GlobalManager manager;
   
  long totalDownloaded;
  long totalUploaded;
  long totalUptime;
  
  long lastDownloaded;
  long lastUploaded;
  long lastUptime; 
  
  long session_start_time = SystemTime.getCurrentTime();
  
  long	downloadCount;
  Map	downloadTypes	= new HashMap();
  
  protected AEMonitor	this_mon	= new AEMonitor( "OverallStats" );

  private Map 
  load(String filename) 
  {
    return( FileUtil.readResilientConfigFile( filename ));
  }
  
  private Map load() {
	  return( load("azureus.statistics"));
	}
  
  private void 
  save(String filename,
		Map	map ) 
  {  	  
  	try{
  		this_mon.enter();
  	  		
  		FileUtil.writeResilientConfigFile( filename, map );
  		
  	}finally{
  		
  		this_mon.exit();
  	}
  }
  
  private void save( Map map ) {
	  save("azureus.statistics", map);
	}
  
  private void validateAndLoadValues(
	Map	statisticsMap ) {
	  
    lastUptime = SystemTime.getCurrentTime() / 1000;

    Map overallMap = (Map) statisticsMap.get("all");

    totalDownloaded = getLong( overallMap, "downloaded" );
	totalUploaded = getLong( overallMap, "uploaded" );
	totalUptime = getLong( overallMap, "uptime" );	    
	    
	downloadCount = getLong( overallMap, "download_count" );
	downloadTypes = getMap( overallMap,  "download_types" );
  }
  
  protected long
  getLong(
	Map		map,
	String	name )
  {
	  if ( map == null ){
		  return( 0 );
	  }
	
	  Object	obj = map.get(name);
	
	  if (!(obj instanceof Long )){
		return(0);
	  }
	
	  return(((Long)obj).longValue());
  }
  
  protected Map
  getMap(
	Map		map,
	String	name )
  {
	  if ( map == null ){
		  return( new HashMap());
	  }
	
	  Object	obj = map.get(name);
	
	  if (!(obj instanceof Map )){
		return(new HashMap());
	  }
	
	  return((Map)obj);
  }
  
  public OverallStatsImpl(GlobalManager _manager) {
    manager = _manager;
    manager.addListener(this);
    Map 	stats = load();
    validateAndLoadValues(stats);

    SimpleTimer.addPeriodicEvent(1000 * 60,this);
  }
  
	public int getAverageDownloadSpeed() {
		if(totalUptime > 1) {
      return (int)(totalDownloaded / totalUptime);
    }
    return 0;
	}

	public int getAverageUploadSpeed() {
    if(totalUptime > 1) {
      return (int)(totalUploaded / totalUptime);
    }
    return 0;
	}

	public long getDownloadedBytes() {
		return totalDownloaded;
	}

	public long getUploadedBytes() {
		return totalUploaded;
	}

	public long getTotalUpTime() {
		return totalUptime;
  }

  public long getSessionUpTime() {
    return (SystemTime.getCurrentTime() - session_start_time) / 1000;
  }
  
  public Map getDownloadStats(){
	  Map	res = new HashMap();
	  
	  res.put( "tot", new Long(downloadCount));
	  
	  res.put( "type", downloadTypes );
	  
	  return( res );
  }
	public void perform(TimerEvent event) {
    updateStats();
	}
  
	public void 
	downloadManagerAdded(
		DownloadManager dm) 
	{
		if ( !dm.isPersistent()){
				// don't count shares
			return;
		}
		
		if ( dm.getStats().getDownloadCompleted(false) > 0 ){
				// don't count downloads added as seeding
			return;
		}
		
		if ( !dm.getDownloadState().getBooleanParameter( DownloadManagerState.PARAM_STATS_COUNTED )){
			
			downloadCount++;
			
			TOTorrent	t = dm.getTorrent();
			
			if ( t == null ){
				
				return;
			}
			
			TOTorrentFile[]	files = t.getFiles();
			
			Map	ext_sizes = new HashMap();
			
			long	largest		= 0;
			String	largest_ext	= null;
			
			for (int i=0;i<files.length;i++){
				
				TOTorrentFile	file = files[i];
				
				String	path = file.getRelativePath();
				
				int	pos = path.lastIndexOf(File.separator);
				
				if ( pos != -1 ){
					
					path = path.substring(pos+1);
				}
				
				pos	= path.lastIndexOf('.');
				
				String	ext;
				
				if ( pos == -1 ){
				
					ext	= "?";
				}else{
					
					ext = path.substring(pos+1).toLowerCase();
				}
				
				Long	old_tot = (Long)ext_sizes.get( ext );
				
				long	new_tot	= 0;
				
				if ( old_tot != null ){
					
					new_tot = ((Long)old_tot).longValue();
				}
				
				new_tot += file.getLength();
				
				if ( new_tot > largest ){
					
					largest		= new_tot;
					largest_ext	= ext;
				}
				
				ext_sizes.put( ext, new Long( new_tot ));
			}
			
			int	size_id = 0;
			int	type_id	= 0;
			
			long	size_mb = t.getSize()/(1024*1024);
			
			for (int i=0;i<file_sizes.length;i++){
				if ( size_mb <= file_sizes[i] ){
					
					size_id = i+1;
					break;
				}
			}
			
			for (int i=0;i<ext_sets.length;i++){
				if ( ext_sets[i].contains(largest_ext) ){
					
					type_id = i+1;
					break;
				}
			}
			
			String	key = String.valueOf( size_id*1024+type_id );
			
			Object	old_val = downloadTypes.get(key);
			long	val		= 0;
			
			if ( old_val instanceof Long ){
				
				val = ((Long)old_val).longValue();
			}
			
			val++;
			
			downloadTypes.put( key, new Long( val ));
			
			dm.getDownloadState().setBooleanParameter( DownloadManagerState.PARAM_STATS_COUNTED, true );
		}
	}
	
  public void destroyInitiated() {
    updateStats();
  }

  private void updateStats() 
  {
  	try{
  		this_mon.enter();
  	
	    long current_time = SystemTime.getCurrentTime() / 1000;
	    
	    if ( current_time < lastUptime ) {  //time went backwards
	      lastUptime = current_time;
	      return;
	    }
	    
	    GlobalManagerStats stats = manager.getStats();
	    
	    long	current_total_received 	= stats.getTotalDataBytesReceived() + stats.getTotalProtocolBytesReceived();
	    long	current_total_sent		= stats.getTotalDataBytesSent() + stats.getTotalProtocolBytesSent();
	    
	    totalDownloaded +=  current_total_received - lastDownloaded;
	    lastDownloaded = current_total_received;
	    
	    if( totalDownloaded < 0 )  totalDownloaded = 0;
	    if( totalDownloaded > 1024*1024*1024*1024*1024 )  totalDownloaded = 0;  //petabyte
	    
	    totalUploaded +=  current_total_sent - lastUploaded;
	    lastUploaded = current_total_sent;
	    
	    if( totalUploaded < 0 )  totalUploaded = 0;
	    if( totalUploaded > 1024*1024*1024*1024*1024 )  totalUploaded = 0;  //petabyte
	    
	    long delta = current_time - lastUptime;
	    
	    if( delta > 100 || delta < 0 ) { //make sure the time diff isn't borked
	      lastUptime = current_time;
	      return;
	    }
	    
	    if( totalUptime > 60*60*24*365*10 ) {  //total uptime > 10years is an error, reset
	      totalUptime = 0;
	    }
	    
	    if( totalUptime < 0 )  totalUptime = 0;
	    
	    totalUptime += delta;
	    lastUptime = current_time;
	    
	    HashMap	overallMap = new HashMap();
	    
	    overallMap.put("downloaded",new Long(totalDownloaded));
	    overallMap.put("uploaded",new Long(totalUploaded));
	    overallMap.put("uptime",new Long(totalUptime));
	    overallMap.put("download_count",new Long(downloadCount));
	    overallMap.put("download_types", downloadTypes);
	    
	    Map	map = new HashMap();
	    
	    map.put( "all", overallMap );
	    
	    save( map );
  	}finally{
  	
  		this_mon.exit();
  	}
  }
}
