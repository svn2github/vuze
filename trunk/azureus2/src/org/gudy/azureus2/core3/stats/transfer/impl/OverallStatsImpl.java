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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerStats;
import org.gudy.azureus2.core3.global.impl.GlobalManagerAdpater;
import org.gudy.azureus2.core3.stats.transfer.OverallStats;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreComponent;
import com.aelitis.azureus.core.AzureusCoreLifecycleAdapter;


/**
 * @author Olivier
 * 
 */
public class 
OverallStatsImpl 
	extends GlobalManagerAdpater 
	implements OverallStats
{
  
  	// sizes in MB
  
  private static final long TEN_YEARS 		= 60*60*24*365*10L;
  
  private static final long	STATS_PERIOD	= 60*1000;	// 1 min
  private static final long DL_STATE_TICKS	= 15;		// 15 min
  private static final int DL_AVERAGE_CELLS = (int)( 12*60*60*1000 / ( STATS_PERIOD * DL_STATE_TICKS ));
 
  
  AzureusCore	core;
   
  long totalDownloaded;
  long totalUploaded;
  long totalUptime;
  
  long lastDownloaded;
  long lastUploaded;
  long lastUptime; 
  
  long session_start_time = SystemTime.getCurrentTime();
  
  long	downloadCount;
  
  protected AEMonitor	this_mon	= new AEMonitor( "OverallStats" );

  private int 	tick_count;
  private int	dl_cell_pos;
  private int[]	dl_average_cells 	= new int[DL_AVERAGE_CELLS];
  private int[]	seed_average_cells	= new int[DL_AVERAGE_CELLS];
  private int	dl_average;
  private int	seed_average;
  private int	running_count;
  private int	public_count;
  
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
	
	dl_average 		= (int)getLong( overallMap, "download_average" );
	seed_average 	= (int)getLong( overallMap, "seed_average" );
	
	if ( dl_average < 0 || dl_average > 32000 ){
		dl_average = 0;
	}
	
	if ( seed_average < 0 || seed_average > 32000 ){
		seed_average = 0;
	}
	
	dl_average_cells[0]		= dl_average;
	seed_average_cells[0]	= seed_average;
	
	running_count 	= (int)getLong( overallMap, "running" );
	public_count 	= (int)getLong( overallMap, "public" );

	dl_cell_pos	= 1;
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
  
  public 
  OverallStatsImpl(
	AzureusCore _core) 
  {
	core	= _core;
	
    Map 	stats = load();
    validateAndLoadValues(stats);

    core.addLifecycleListener(
    	new AzureusCoreLifecycleAdapter()
    	{
    		public void
    		componentCreated(
    			AzureusCore				core,
    			AzureusCoreComponent	component )
    		{
    			if ( component instanceof GlobalManager ){
    				
    				GlobalManager	gm = (GlobalManager)component;
    				
    				gm.addListener( OverallStatsImpl.this, false );
    				   
    			    SimpleTimer.addPeriodicEvent(
    			    	"OverallStats", 
    			    	STATS_PERIOD, 
    			    	new TimerEventPerformer()
    			    	{
    			    		public void 
    			    		perform(TimerEvent event) 
    			    		{
    			    			updateStats( false );
    			    		}
    			    	});
    			}
    		}
    	});

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
			
			dm.getDownloadState().setBooleanParameter( DownloadManagerState.PARAM_STATS_COUNTED, true );
		}
	}
	
  public void destroyInitiated() {
    updateStats( true );
  }

  private void updateStats( boolean force ) 
  {
  	try{
  		this_mon.enter();
  	
	    long current_time = SystemTime.getCurrentTime() / 1000;
	    
	    if ( current_time < lastUptime ) {  //time went backwards
	      lastUptime = current_time;
	      return;
	    }
	    
	    GlobalManagerStats stats = core.getGlobalManager().getStats();
	    
	    long	current_total_received 	= stats.getTotalDataBytesReceived() + stats.getTotalProtocolBytesReceived();
	    long	current_total_sent		= stats.getTotalDataBytesSent() + stats.getTotalProtocolBytesSent();
	    
	    totalDownloaded +=  current_total_received - lastDownloaded;
	    lastDownloaded = current_total_received;
	    
	    if( totalDownloaded < 0 )  totalDownloaded = 0;

	    totalUploaded +=  current_total_sent - lastUploaded;
	    lastUploaded = current_total_sent;

	    if( totalUploaded < 0 )  totalUploaded = 0;
	    
	    long delta = current_time - lastUptime;
	    
	    if( delta > 100 || delta < 0 ) { //make sure the time diff isn't borked
	      lastUptime = current_time;
	      return;
	    }
	    
	    if( totalUptime > TEN_YEARS ) {  //total uptime > 10years is an error, reset
	      totalUptime = 0;
	    }
	    
	    if( totalUptime < 0 )  totalUptime = 0;
	    
	    totalUptime += delta;
	    lastUptime = current_time;
	    
	    tick_count++;
	    
	    if ( force || tick_count % DL_STATE_TICKS == 0 ){
	    	
	      try{
		  	  List	managers = core.getGlobalManager().getDownloadManagers();
			  		  
			  int	dl		= 0;
			  int	seed	= 0;
			  int	run		= 0;
			  int	pub		= 0;
			  
			  for (int i=0;i<managers.size();i++){
				  
				  DownloadManager	dm = (DownloadManager)managers.get(i);
				  			  
				  int	state = dm.getState();
				  
				  if ( state == DownloadManager.STATE_DOWNLOADING ){
						  
					  dl++;
						  
				  }else if ( state == DownloadManager.STATE_SEEDING ){
						  
					  seed++;
				  }
				  
				  TOTorrent	torrent = dm.getTorrent();
				  
				  if ( torrent != null && !torrent.getPrivate()){
					  
					  pub++;
				  }
				  				  
				  if (	 state != DownloadManager.STATE_ERROR &&
						 state != DownloadManager.STATE_STOPPED ){
					  
					  run++;
				  }
			  }
		    
			  dl_average_cells[dl_cell_pos%DL_AVERAGE_CELLS]	= dl;
		    	
			  seed_average_cells[dl_cell_pos%DL_AVERAGE_CELLS]	= seed;
		    	
			  dl_cell_pos++;
			  
			  int	cells = Math.min( dl_cell_pos, DL_AVERAGE_CELLS );
			  
			  dl_average	= 0;
			  seed_average	= 0;
			  
			  for (int i=0;i<cells;i++){
				  dl_average += dl_average_cells[i];
				  seed_average += seed_average_cells[i];
			  }
			  
			  dl_average 	= dl_average/cells;
			  seed_average 	= seed_average/cells;
			 		
			  running_count	= run;
			  public_count	= pub;
			  
	      }catch( Throwable e ){
	    	  
	    	  Debug.printStackTrace(e);
	      }
	    }
	    
	    HashMap	overallMap = new HashMap();
	    
	    overallMap.put("downloaded",new Long(totalDownloaded));
	    overallMap.put("uploaded",new Long(totalUploaded));
	    overallMap.put("uptime",new Long(totalUptime));
	    overallMap.put("download_count",new Long(downloadCount));
	    overallMap.put("download_average", new Long(dl_average));
	    overallMap.put("seed_average", new Long(seed_average));
	    overallMap.put("public", new Long(public_count));
	    overallMap.put("running", new Long(running_count));

	    Map	map = new HashMap();
	    
	    map.put( "all", overallMap );
	    
	    save( map );
  	}finally{
  	
  		this_mon.exit();
  	}
  }
}
