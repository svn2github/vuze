/*
 * Created on 8 juil. 2003
 *
 */
package org.gudy.azureus2.core3.logging;

/**
 * @author Olivier
 * 
 */
public interface ILoggerListener {

  public void log(int componentId,int event,int color,String text);
} 
