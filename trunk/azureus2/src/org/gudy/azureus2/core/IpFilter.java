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
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.util.*;

/**
 * @author Olivier
 * 
 */
public class IpFilter {

  private static IpFilter ipFilter;
  
  private List ipRanges;
  
 
  
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
  
  public void save() {
      
    try {
      //Open the file
      File filtersFile = getApplicationFile("filters.config");
      FileOutputStream fos = new FileOutputStream(filtersFile);
      Map map = new HashMap();
      List filters = new ArrayList();
      map.put("ranges",filters);
      Iterator iter = this.ipRanges.iterator();
      while(iter.hasNext()) {
        IpRange range = (IpRange) iter.next();
        if(range.isValid()) {
          String description =  range.description;
          String startIp = range.startIp;
          String endIp = range.endIp;
          Map mapRange = new HashMap();
          mapRange.put("description",description);
          mapRange.put("start",startIp);
          mapRange.put("end",endIp);
          filters.add(mapRange);
        }
      }
      fos.write(BEncoder.encode(map));
      fos.close();     
    } catch (Exception e) {
      e.printStackTrace();
      // TODO: handle exception
    }
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
    synchronized(ipRanges) { 
      Iterator iter = ipRanges.iterator();
      while(iter.hasNext()) {
        IpRange ipRange = (IpRange) iter.next();
        if(ipRange.isInRange(ipAddress)) {
          Logger.getLogger().log(0,0,Logger.ERROR,"Ip Blocked : " + ipAddress + ", in range : " + ipRange);
          return true;
        }
      }
    }
    return false;
  }
  
  /**
   * @return
   */
  public List getIpRanges() {
    return ipRanges;
  }

}
