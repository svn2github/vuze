/*
 * Created on Jan 25, 2004
 * Created by Alon Rohter
 *
 */
package org.gudy.azureus2.core3.peer.impl.transport.sharedport;


import java.util.*;
import java.nio.channels.*;

import org.gudy.azureus2.core3.util.*;


/**
 * Temp class designed to help detect Selector anomalies and cleanly re-open if necessary.
 * 
 * NOTE:
 * As of JVM 1.4.2_03, after network connection disconnect/reconnect, usually-blocking
 * select() and select(long) calls no longer block, and will instead return immediately.
 * This can cause selector spinning and 100% cpu usage.
 * See:
 *   http://forum.java.sun.com/thread.jsp?forum=4&thread=293213
 *   http://developer.java.sun.com/developer/bugParade/bugs/4850373.html
 *   http://developer.java.sun.com/developer/bugParade/bugs/4881228.html
 */
public class SelectorGuard {
  
  private long timeThreshold;
  private int countThreshold;
  
  private int consecutiveZeroSelects = 0;
  private long lastZeroSelectTime = 0;
  
  
  public SelectorGuard( long time_threshold, int count_threshold ) {
    this.timeThreshold = time_threshold;
    this.countThreshold = count_threshold;    
  }
  
  
  /**
   * Checks whether selector is still OK, and not spinning.
   */
  public boolean isSelectorOK( int num_keys_ready ) {
    if (num_keys_ready > 0) {
      //non-zero select, so OK
      consecutiveZeroSelects = 0;
      return true;
    }
    
    long currentTime = System.currentTimeMillis();
    long elapsedTime = currentTime - lastZeroSelectTime;
    
    lastZeroSelectTime = currentTime;
    
    if (elapsedTime > timeThreshold) {
      //zero-select, but over the time threshold, so OK
      consecutiveZeroSelects = 0;
      return true;
    }
    
    //if we've gotten here, then we have a potential selector anomalie
    consecutiveZeroSelects++;
    
    if (consecutiveZeroSelects > countThreshold) {
      //we're over the threshold: reset stats and report error
      consecutiveZeroSelects = 0;
      lastZeroSelectTime = 0;
      return false;
    }
    
    //not yet over the count threshold
    return true;
  }
  
  
  /**
   * Cleanup bad selector and return a fresh new one.
   */
  public static Selector repairSelector( Selector bad_selector ) {
    Debug.out("Likely network disconnect/reconnect: Repairing 1 selector, " +bad_selector.keys().size()+ " keys");
    
    try {
      //sleep a bit to allow underlying network recovery
      Thread.sleep(3000);
        
    	//open new
    	Selector newSelector = Selector.open();
      
    	//register old selector's keyset with new selector
    	Iterator it = bad_selector.keys().iterator();
    	while (it.hasNext()) {
    		SelectionKey key = (SelectionKey)it.next();
    		it.remove();
            
    		SocketChannel channel = (SocketChannel)key.channel();
    		channel.register(newSelector, key.interestOps(), key.attachment());
    	}
        
    	//close old
    	bad_selector.close();
        
      Thread.sleep(2000);
        
    	//return new
    	return newSelector;
        
    } catch (Exception e) {e.printStackTrace();}
      
    Debug.out("Unable to repair bad selector; returning original as still-bad");
    return bad_selector;
  }
  
  
  
}
