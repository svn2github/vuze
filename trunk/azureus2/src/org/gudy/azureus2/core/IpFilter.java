/*
 * File    : IpFilter.java
 * Created : 1 oct. 2003 12:27:26
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
 
package org.gudy.azureus2.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.gudy.azureus2.core3.util.*;

/**
 * @author Olivier
 * 
 */
public class IpFilter {

  private static IpFilter ipFilter;
  
  private List ipRanges;
  
  private class IpRange {
    
    private String description;
    
    private String startIp;
    private String endIp;
    
    private int[] start;
    private int[] end;
    
    private boolean valid;
    
    public IpRange(String description, String startIp, String endIp) {
      this.valid = false;
      this.description = description;  
      if(startIp == null || endIp == null) {        
        return;
      }
      this.startIp = startIp.trim();
      this.endIp = endIp.trim();
      checkValid(); 
    }
    
    private void checkValid() {   
      StringTokenizer stStart = new StringTokenizer(startIp,".");
      StringTokenizer stEnd = new StringTokenizer(endIp,".");
      if(stStart.countTokens() != 4 || stEnd.countTokens() != 4) {      
        return;
      }   
      start = new int[4];
      end = new int[4];
      int i = 0;
      while(stStart.hasMoreTokens() && i < start.length) {
        try {
          start[i++] = Integer.parseInt(stStart.nextToken());
        } catch(Exception e) {
          return;
        }
      }
      i = 0;
      while(stEnd.hasMoreTokens() && i < end.length) {
        try {
          end[i++] = Integer.parseInt(stEnd.nextToken());
        } catch(Exception e) {
          return;
        }
      }
      this.valid = true;
    }
    
    public boolean isValid() {
      return this.valid;
    }
    
    public boolean isInRange(String ipAddress) {
      StringTokenizer st = new StringTokenizer(ipAddress,".");
      if(st.countTokens() != 4)
        return false;    
      for(int i = 0 ; i < 4 ; i++) {
        int n = 0;
        try {
          n = Integer.parseInt(st.nextToken());
        } catch(Exception e) {
          return false;
        }
        if(n < start[i] || n > end[i])
          return false;
      }
      return true;
    }
    
    public String toString() {
      return description + " : " + startIp + " - " + endIp; 
    }
  }
  
  private IpFilter() {
    ipFilter = this;
    ipRanges = new ArrayList();
    loadFilters();
  }
  
  public static synchronized IpFilter getInstance() {
    if(ipFilter == null) {
      ipFilter = new IpFilter();
    }
    return ipFilter;
  }
  
  //TODO:: Move this to a FileManager class?
  public static String getApplicationPath() {
    return System.getProperty("user.dir");
  }

  public static File getApplicationFile(String filename) {
    return new File(getApplicationPath(), filename);
  }
  
  private void loadFilters() {
    FileInputStream fin = null;
    BufferedInputStream bin = null;
    try {
      //open the file
      File filtersFile = getApplicationFile("filters.config");
      if (filtersFile.exists()) {
	      fin = new FileInputStream(filtersFile);
	      bin = new BufferedInputStream(fin);
	      Map map = BDecoder.decode(bin);
	      List list = (List) map.get("ranges");
	      Iterator iter = list.listIterator();
	      while(iter.hasNext()) {
	        Map range = (Map) iter.next();
	        String description =  new String((byte[])range.get("description"));
	        String startIp =  new String((byte[])range.get("start"));
	        String endIp = new String((byte[])range.get("end"));
	        
	        IpRange ipRange = new IpRange(description,startIp,endIp);
	        if(ipRange.isValid())
	          this.ipRanges.add(ipRange);
	      }
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
  
  public boolean isInRange(String ipAddress) {   
    Iterator iter = ipRanges.iterator();
    while(iter.hasNext()) {
      IpRange ipRange = (IpRange) iter.next();
      if(ipRange.isInRange(ipAddress)) {
        Logger.getLogger().log(0,0,Logger.ERROR,"Ip Blocked : " + ipAddress + ", in range : " + ipRange);
        return true;
      }
    }
    
    return false;
  }
  
}
