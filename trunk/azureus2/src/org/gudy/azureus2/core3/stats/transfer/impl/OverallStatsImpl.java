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
import java.util.Map;

import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerStats;
import org.gudy.azureus2.core3.global.impl.GlobalManagerAdpater;
import org.gudy.azureus2.core3.stats.transfer.OverallStats;
import org.gudy.azureus2.core3.stats.transfer.YearStatsList;
import org.gudy.azureus2.core3.util.*;


/**
 * @author Olivier
 * 
 */
public class OverallStatsImpl extends GlobalManagerAdpater implements OverallStats, TimerEventPerformer{

  GlobalManager manager;
  Map statisticsMap;
  Map overallMap;
  
  Timer timer;
  
  long totalDownloaded;
  long totalUploaded;
  long totalUptime;
  
  long lastDownloaded;
  long lastUploaded;
  long lastUptime; 
  
  private void 
  load(String filename) 
  {
    statisticsMap = FileUtil.readResilientConfigFile( filename );
  }
  
  private void load() {
	  load("azureus.statistics");
	}
  
  private synchronized void 
  save(String filename) 
  {  	  	
	 File file = new File( SystemProperties.getUserPath() + filename );
	    
	 FileUtil.writeResilientConfigFile( filename, statisticsMap );
  }
  
  private void save() {
	  save("azureus.statistics");
	}
  
  private void validateAndLoadValues() {
    overallMap = (Map) statisticsMap.get("all");
    if(overallMap == null) {
      overallMap = new HashMap();
      overallMap.put("downloaded",new Long(0));
      overallMap.put("uploaded",new Long(0));
      overallMap.put("uptime",new Long(0));
      statisticsMap.put("all",overallMap);
    }
    totalDownloaded = ((Long)overallMap.get("downloaded")).longValue();
    totalUploaded = ((Long)overallMap.get("uploaded")).longValue();
    totalUptime = ((Long)overallMap.get("uptime")).longValue();
    lastUptime = SystemTime.getCurrentTime() / 1000;
  }
  
  public OverallStatsImpl(GlobalManager manager) {
    this.manager = manager;
    manager.addListener(this);
    load();
    validateAndLoadValues();
    timer = new Timer("Stats Recorder");
    timer.addPeriodicEvent(1000 * 60,this);
  }
  
	public String getXMLExport() {
		// TODO Implement the XML export thing
		return null;
	}
	
	public YearStatsList getYearStats() {
		// TODO Implement granularity
		return null;
	}

	public void setLogLevel(int logLevel) {
		// TODO Auto-generated method stub
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

	public long getUpTime() {
		return totalUptime;
  }

	public void perform(TimerEvent event) {
    updateStats();
	}
  
  public void destroyInitiated() {
    updateStats();
  }

  private synchronized void updateStats() {
    
    if ( SystemTime.isErrorLast5min() ) {
      lastUptime = SystemTime.getCurrentTime() / 1000;
      return;
    }
    
    GlobalManagerStats stats = manager.getStats();
    
    long	current_total_received 	= stats.getTotalReceivedRaw();
    long	current_total_sent		= stats.getTotalSentRaw();
    
    totalDownloaded +=  current_total_received - lastDownloaded;
    lastDownloaded = current_total_received;
    
    totalUploaded +=  current_total_sent - lastUploaded;
    lastUploaded = current_total_sent;
    
    totalUptime += SystemTime.getCurrentTime() / 1000 - lastUptime;
    lastUptime = SystemTime.getCurrentTime() / 1000;
    
    overallMap.put("downloaded",new Long(totalDownloaded));
    overallMap.put("uploaded",new Long(totalUploaded));
    overallMap.put("uptime",new Long(totalUptime));
    
    save(); 
  }
}
