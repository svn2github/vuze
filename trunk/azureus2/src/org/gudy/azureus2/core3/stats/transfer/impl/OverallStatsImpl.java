/*
 * File    : OverallStatsImpl.java
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
import java.util.Map;

import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerStats;
import org.gudy.azureus2.core3.global.impl.GlobalManagerAdpater;
import org.gudy.azureus2.core3.stats.transfer.OverallStats;
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
  
  AzureusCore	core;
   
  long totalDownloaded;
  long totalUploaded;
  long totalUptime;
  
  long lastDownloaded;
  long lastUploaded;
  long lastUptime; 
  
  long session_start_time = SystemTime.getCurrentTime();
  
  protected AEMonitor	this_mon	= new AEMonitor( "OverallStats" );

  private int 	tick_count;
  
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
    
	    HashMap	overallMap = new HashMap();
	    
	    overallMap.put("downloaded",new Long(totalDownloaded));
	    overallMap.put("uploaded",new Long(totalUploaded));
	    overallMap.put("uptime",new Long(totalUptime));

	    Map	map = new HashMap();
	    
	    map.put( "all", overallMap );
	    
	    save( map );
  	}finally{
  	
  		this_mon.exit();
  	}
  }
}
