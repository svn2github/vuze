/*
 * WebLogAppender.java
 *
 * Created on 26. August 2003, 02:18
 */

package org.gudy.azureus2.server;

import java.util.List;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.Filter;

import org.gudy.azureus2.core.ConfigurationManager;

/**
 *
 * @author  tobi
 */
public class WebLogAppender extends AppenderSkeleton implements Appender {
  
  private List log;
  
  /** Creates a new instance of WebLogAppender */
  public WebLogAppender(List _log) {
    log = _log;
  }
  
  public void append(org.apache.log4j.spi.LoggingEvent loggingEvent) {
    log.add(loggingEvent);
    while (log.size() > ConfigurationManager.getInstance().getIntParameter("Server_iLogCount"))
      log.remove(0);
  }
 
  public boolean requiresLayout() {
    return false;
  }

  public void close() {
    closed = true;
  }
}
