/*
 * Created on 22 juil. 2003
 *
 */
package org.gudy.azureus2.core;

import java.util.HashMap;
import java.util.Iterator;

/**
 * @author Olivier
 * 
 */
public class TrackerChecker {

  private HashMap trackers;

  public TrackerChecker() {
    trackers = new HashMap();
  }

  public HashData getHashData(String trackerUrl, byte[] hash) {
    return getHashData(trackerUrl,new Hash(hash));
  }

  public void removeHash(String trackerUrl,Hash hash) {
    TrackerStatus ts = (TrackerStatus) trackers.get(trackerUrl);
    if(ts != null) {
      ts.removeHash(hash);
    }
  }
  
  public HashData getHashData(String trackerUrl,final Hash hash) {
    if (trackers.containsKey(trackerUrl)) {
      final TrackerStatus ts = (TrackerStatus) trackers.get(trackerUrl);
      HashData data = ts.getHashData(hash);
      if(data != null)
        return data;
      else {
        ts.asyncUpdate(hash);
        return null;
      }        
    }
    final TrackerStatus ts = new TrackerStatus(trackerUrl);
    synchronized (trackers) {
      trackers.put(trackerUrl, ts);
    }
    ts.asyncUpdate(hash);
    return null;
  }

  public void update() {
    synchronized (trackers) {
      Iterator iter = trackers.values().iterator();
      while (iter.hasNext()) {
        TrackerStatus ts = (TrackerStatus) iter.next();
        Iterator iterHashes = ts.getHashesIterator();
        while(iterHashes.hasNext()) {              
          Hash hash = (Hash) iterHashes.next();
          ts.asyncUpdate(hash);
        }                      
      }
    }
  }

}
