/*
 * Created on 30 juin 2003
 *
 */
package org.gudy.azureus2.core;

import java.util.Vector;

import org.gudy.azureus2.ui.swt.IComponent;
import org.gudy.azureus2.ui.swt.IComponentListener;

/**
 * @author Olivier
 * 
 */
public class Component implements IComponent, IComponentListener {

  private Vector listeners;

  public Component() {
    listeners = new Vector();
  }

  public void addListener(IComponentListener listener) {
    synchronized (listeners) {
      listeners.add(listener);
    }
  }

  public void removeListener(IComponentListener listener) {
    synchronized (listeners) {
      listeners.remove(listener);
    }
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IComponentListener#objectAdded(java.lang.Object, java.lang.Object)
   */
  public void objectAdded(Object created) {
    //Notify all general listeners
    synchronized (listeners) {
      for (int i = 0; i < listeners.size(); i++) {
        IComponentListener listener = (IComponentListener) listeners.get(i);
        listener.objectAdded(created);
      }
    }
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IComponentListener#objectRemoved(java.lang.Object, java.lang.Object)
   */
  public void objectRemoved(Object removed) {
    //Notify all general listeners
    synchronized (listeners) {
      for (int i = 0; i < listeners.size(); i++) {
        IComponentListener listener = (IComponentListener) listeners.get(i);
        listener.objectRemoved(removed);
      }
    }
  }

}
