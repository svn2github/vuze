/*
 * Created on 29 juin 2003
 *
 */
package org.gudy.azureus2.core;

/**
 * @author Olivier
 * 
 */
public interface IComponentListener {
  
  public void objectAdded(Object created);  
  public void objectRemoved(Object removed);
  
}
