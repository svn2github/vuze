/*
 * File    : PeersViewEventDispacher.java
 * Created : 27 nov. 2003
 * By      : Olivier
 *
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
 
package org.gudy.azureus2.ui.swt.views.tableitems.peers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.gudy.azureus2.ui.swt.views.tableitems.utils.ITableStructureModificationListener;

/**
 * @author Olivier
 *
 */
public class PeersViewEventDispacher implements ITableStructureModificationListener {

  private static PeersViewEventDispacher instance;
  private List listeners;
  
  /**
   * 
   */
  private PeersViewEventDispacher() {
   listeners = new ArrayList();
  }
  
  public static synchronized PeersViewEventDispacher getInstance() {
    if(instance == null)
      instance = new PeersViewEventDispacher();
    return instance;
  }
  
  public void addListener(PeersViewListener listener) {
    synchronized(listeners) {
      this.listeners.add(listener);
    }
  }
  
  public void removeListener(PeersViewListener listener) {
    synchronized(listeners) {
      this.listeners.remove(listener);
    }
  }
  
  public void tableStructureChanged() {
   synchronized(listeners) {
     Iterator iter = listeners.iterator();
     while(iter.hasNext()) {
       PeersViewListener listener = (PeersViewListener) iter.next();
       listener.tableStructureChanged();
     }
   }
  }
  
  public void columnSizeChanged(int columnNumber,int newWidth) {
   synchronized(listeners) {
     Iterator iter = listeners.iterator();
     while(iter.hasNext()) {
       PeersViewListener listener = (PeersViewListener) iter.next();
       listener.columnSizeChanged(columnNumber,newWidth);
     }
   }
  }

}
