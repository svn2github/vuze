/*
 * File    : TableStructureEventDispatcher.java
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
 
package org.gudy.azureus2.ui.swt.views.table.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.gudy.azureus2.ui.swt.views.table.ITableStructureModificationListener;
import org.gudy.azureus2.ui.swt.views.table.TableColumnCore;

/**
 * @author Olivier
 *
 */
public class TableStructureEventDispatcher implements ITableStructureModificationListener {

  private static Map instances = new HashMap();
  private List listeners;
  
  /**
   * 
   */
  private TableStructureEventDispatcher() {
    listeners = new ArrayList();
  }
  
  public static synchronized TableStructureEventDispatcher getInstance(String sTableID) {
    TableStructureEventDispatcher instance = (TableStructureEventDispatcher)instances.get(sTableID);
    if (instance == null) {
      instance = new TableStructureEventDispatcher();
      instances.put(sTableID, instance);
    }
    return instance;
  }
  
  public void addListener(ITableStructureModificationListener listener) {
    synchronized(listeners) {
      this.listeners.add(listener);
    }
  }
  
  public void removeListener(ITableStructureModificationListener listener) {
    synchronized(listeners) {
      this.listeners.remove(listener);
    }
  }
  
  public void tableStructureChanged() {
   synchronized(listeners) {
     Iterator iter = listeners.iterator();
     while(iter.hasNext()) {
       ITableStructureModificationListener listener = (ITableStructureModificationListener) iter.next();
       listener.tableStructureChanged();
     }
   }
  }
  
  public void columnSizeChanged(TableColumnCore tableColumn) {
   synchronized(listeners) {
     Iterator iter = listeners.iterator();
     while(iter.hasNext()) {
       ITableStructureModificationListener listener = (ITableStructureModificationListener) iter.next();
       listener.columnSizeChanged(tableColumn);
     }
   }
  }

  public void columnInvalidate(TableColumnCore tableColumn) {
    synchronized (listeners) {
      Iterator iter = listeners.iterator();
      while (iter.hasNext()) {
        ITableStructureModificationListener listener = 
                              (ITableStructureModificationListener)iter.next();
        listener.columnInvalidate(tableColumn);
      }
    }
  }
}
