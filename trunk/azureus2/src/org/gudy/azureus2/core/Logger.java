/*
 * Created on 30 juin 2003
 *
 */
package org.gudy.azureus2.core;

/**
 * @author Olivier
 * 
 */
public class Logger {
  private static Logger logger = null;

  public static final int INFORMATION = 0;
  public static final int RECEIVED = 1;
  public static final int SENT = 2;
  public static final int ERROR = 3;

  public ILoggerListener listener;

  private Logger() {}

  public synchronized static final Logger getLogger() {
    if (logger == null)
      logger = new Logger();
    return logger;
  }

  public void log(int componentId, int event, int color, String text) {
    if(listener !=  null)
      listener.log(componentId,event,color,text);    
  }
  
  public void setListener(ILoggerListener listener) {
    this.listener = listener;
  }
  
  public void removeListener() {
    this.listener = null;
  }
}
