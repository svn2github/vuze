/* Written and copyright 2001-2003 Tobias Minich.
 * Distributed under the GNU General Public License; see the README file.
 * This code comes with NO WARRANTY.
 *
 *
 * WebLogAppender.java
 *
 * Created on 26. August 2003, 02:18
 */

package org.gudy.azureus2.ui.web;

import java.util.List;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;

import org.gudy.azureus2.core3.config.*;

/**
 *
 * @author  Tobias Minich
 */
public class WebLogAppender extends AppenderSkeleton implements Appender {
  
  private List log;
  
  /** Creates a new instance of WebLogAppender */
  public WebLogAppender(List _log) {
    log = _log;
  }
  
  public void append(org.apache.log4j.spi.LoggingEvent loggingEvent) {
    log.add(loggingEvent);
    while (log.size() > COConfigurationManager.getIntParameter("Server_iLogCount"))
      log.remove(0);
  }
 
  public boolean requiresLayout() {
    return false;
  }

  public void close() {
    closed = true;
  }
}
