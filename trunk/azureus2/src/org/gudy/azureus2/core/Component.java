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

  private Object listenerLock = new Object();

  public Component() {
    listeners = new Vector();
  }

  public void addListener(IComponentListener listener) {
    synchronized (listenerLock) {
      listeners.add(listener);
      listenerLock.notifyAll();
    }
  }

  public void removeListener(IComponentListener listener) {
    synchronized (listenerLock) {
      listeners.remove(listener);
      listenerLock.notifyAll();
    }
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IComponentListener#objectAdded(java.lang.Object, java.lang.Object)
   */
  public void objectAdded(Object created) {
    synchronized (listenerLock) {
      //Notify all general listeners
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
    synchronized (listenerLock) {
      //Notify all general listeners
      for (int i = 0; i < listeners.size(); i++) {
        IComponentListener listener = (IComponentListener) listeners.get(i);
        listener.objectRemoved(removed);
      }
    }
  }

}
