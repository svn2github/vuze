/*
 * File    : TrackersUtil.java
 * Created : 7 nov. 2003 12:09:56
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
 
package org.gudy.azureus2.core3.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Olivier
 * 
 */
public class TrackersUtil {
  
  private List trackers;
  private Map multiTrackers; 
  
  private static TrackersUtil instance;
  
  
  
  private TrackersUtil() {
    trackers = new ArrayList();
    multiTrackers = new HashMap();
    loadList();
  }
  
  
  public static synchronized TrackersUtil getInstance() {
    if(instance == null)
      instance = new TrackersUtil();
    return instance;
  }
  
  public List getTrackersList() {
    if(trackers != null)
      return new ArrayList(trackers);
    else
      return null;
  }
    
  public void addTracker(String trackerAnnounceUrl) {
    if(trackers.contains(trackerAnnounceUrl))
      return;
    trackers.add(0,trackerAnnounceUrl);
    saveList();
  }
  
  public void addMultiTracker(String configName, List groups) {
    multiTrackers.put(configName,groups);
    saveList();
  }
  
  public void removeMultiTracker(String configName) {
    multiTrackers.remove(configName);
    saveList();
  }
  
  public Map getMultiTrackers() {
    return new HashMap(multiTrackers);
  }
  
  private void loadList() {    
    File fTrackers = FileUtil.getApplicationFile("trackers.config");
    if(fTrackers.exists() && fTrackers.isFile()) {
      FileInputStream fin = null;
      BufferedInputStream bin = null;
      try {
        fin = new FileInputStream(fTrackers);
        bin = new BufferedInputStream(fin, 8192);
        Map map = BDecoder.decode(bin);
        List list = (List) map.get("trackers");
        if(list != null) {
	        Iterator iter = list.iterator();
	        while(iter.hasNext()) {
	          String tracker =  new String((byte[])iter.next());
	          trackers.add(tracker);
	        }
        }
        Map mapMT = (Map) map.get("multi-trackers");
        if(mapMT != null) {
          Iterator iter = mapMT.keySet().iterator();
          while(iter.hasNext()) {
            String configName =  (String) iter.next();            
            List groups = (List) mapMT.get(configName);
            List resGroups = new ArrayList(groups.size());
            Iterator iterGroups = groups.iterator();
            while(iterGroups.hasNext()) {
              List trackers = (List) iterGroups.next();
              List resTrackers = new ArrayList(trackers.size());
              Iterator iterTrackers = trackers.iterator();
              while(iterTrackers.hasNext()) {
                String tracker = new String((byte[]) iterTrackers.next());
                resTrackers.add(tracker);
              }
              resGroups.add(resTrackers);
            }
            this.multiTrackers.put(configName,resGroups);
          }
        }
        bin.close();                
        fin.close();
      } catch(Exception e) {
        e.printStackTrace();
      }
    }
  }
  
  private void saveList() {
    Map map = new HashMap();
    map.put("trackers",trackers);
    map.put("multi-trackers",multiTrackers);
    try {
      //  Open the file
      File fTrackers = FileUtil.getApplicationFile("trackers.config");
      FileOutputStream fos = new FileOutputStream(fTrackers);
      fos.write(BEncoder.encode(map));
      fos.close();     
    } catch (Exception e) {
      e.printStackTrace();
      // TODO: handle exception
    }    
  }

  

}
