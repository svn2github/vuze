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

  public HashData getHashData(String trackerUrl, Hash hash) {
    if (trackers.containsKey(trackerUrl)) {
      TrackerStatus ts = (TrackerStatus) trackers.get(trackerUrl);
      return ts.getHashData(hash);
    }
    final TrackerStatus ts = new TrackerStatus(trackerUrl);
    synchronized (trackers) {
      trackers.put(trackerUrl, ts);
    }
    Thread t = new Thread() {
      /* (non-Javadoc)
       * @see java.lang.Thread#run()
       */
      public void run() {
        ts.update();
      }
    };
    t.start();
    return null;
  }

  public void update() {
    synchronized (trackers) {
      Iterator iter = trackers.values().iterator();
      while (iter.hasNext()) {
        final TrackerStatus ts = (TrackerStatus) iter.next();
        Thread t = new Thread() {
          /* (non-Javadoc)
           * @see java.lang.Thread#run()
           */
          public void run() {
            ts.update();            
            }
        };
        t.start();
      }
    }
  }

}
