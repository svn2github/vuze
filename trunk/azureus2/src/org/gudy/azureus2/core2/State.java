/*
 * Created on 4 juil. 2003
 *
 */
package org.gudy.azureus2.core2;

/**
 * @author Olivier
 * 
 */
public interface State {
  
  public void process();
  
  public int getState();

}
