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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerStats;
import org.gudy.azureus2.core3.global.impl.GlobalManagerAdpater;
import org.gudy.azureus2.core3.stats.transfer.OverallStats;
import org.gudy.azureus2.core3.stats.transfer.YearStatsList;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.Timer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;

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
  
	private void load(String filename) {
	  BufferedInputStream bin = null;
	  
	  try {
	    //open the file
	    File file = new File( FileUtil.getApplicationPath() + filename );
	    
	    //make sure the file exists and isn't zero-length
	    if ( file.length() <= 1L ) {
	      //if so, try using the backup file
	      file = new File( FileUtil.getApplicationPath() + filename + ".bak" );
	      if ( file.length() <= 1L ) {
	        throw new FileNotFoundException();
	      }
	    }
	    
	    bin = new BufferedInputStream( new FileInputStream(file), 8192 );
	      
	    try{
        statisticsMap = BDecoder.decode(bin);
	    }
	    catch( IOException e ){
	    	// Occurs when file is there but b0rked
        statisticsMap = new HashMap();
	    }
	
	  }
	  catch (FileNotFoundException e) {
      statisticsMap = new HashMap();
	  }
	  finally {
	  	try {
	  		if (bin != null)
	  			bin.close();
	  	} catch (Exception e) {
	  	}
	  }
	}
  
  private void load() {
	  load("azureus.statistics");
	}
  
  private synchronized void save(String filename) {  
	  //open a file stream
	  BufferedOutputStream bos = null;
	  try {
	  	//re-encode the data
	  	
	  	byte[] torrentData = BEncoder.encode(statisticsMap);
	    
	    File file = new File( FileUtil.getApplicationPath() + filename );
	    
	  	//backup
	    if ( file.length() > 1L ) {
	      File bakfile = new File( file + ".bak" );
	      if ( bakfile.exists() ) bakfile.delete();
	      file.renameTo( bakfile );
	    }
	  	
	    bos = new BufferedOutputStream( new FileOutputStream( file, false ), 8192 );
	  	
	  	//write the data out
	  	bos.write(torrentData);
	    bos.flush();
	    
	  } catch (Exception e) {
	    e.printStackTrace();
	  } finally {
	    try {
	      if (bos != null)
	        bos.close();
	    } catch (Exception e) {
	    }
	  }
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
    lastUptime = System.currentTimeMillis() / 1000;
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
    GlobalManagerStats stats = manager.getStats();
    
    long	current_total_received 	= stats.getTotalReceivedRaw();
    long	current_total_sent		= stats.getTotalSentRaw();
    
    totalDownloaded +=  current_total_received - lastDownloaded;
    lastDownloaded = current_total_received;
    
    totalUploaded +=  current_total_sent - lastUploaded;
    lastUploaded = current_total_sent;
    
    totalUptime += System.currentTimeMillis() / 1000 - lastUptime;
    lastUptime = System.currentTimeMillis() / 1000;
    
    overallMap.put("downloaded",new Long(totalDownloaded));
    overallMap.put("uploaded",new Long(totalUploaded));
    overallMap.put("uptime",new Long(totalUptime));
    
    save(); 
  }
}
