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
  private static TrackersUtil instance;
  
  private TrackersUtil() {
    trackers = new ArrayList();
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
  
  private void loadList() {    
    File fTrackers = FileUtil.getApplicationFile("trackers.config");
    if(fTrackers.exists() && fTrackers.isFile()) {
      FileInputStream fin = null;
      BufferedInputStream bin = null;
      try {
        fin = new FileInputStream(fTrackers);
        bin = new BufferedInputStream(fin);
        Map map = BDecoder.decode(bin);
        List list = (List) map.get("trackers");
        Iterator iter = list.iterator();
        while(iter.hasNext()) {
          String tracker =  new String((byte[])iter.next());
          trackers.add(tracker);
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
