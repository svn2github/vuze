/*
 * Created on 19-Apr-2004
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.pluginsimpl.local.ui.tables;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.plugins.ui.UIRuntimeException;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableListener;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.pluginsimpl.local.ui.tables.TableContextMenuItemImpl;
import org.gudy.azureus2.ui.swt.views.table.TableColumnCore;
import org.gudy.azureus2.ui.swt.views.table.impl.TableColumnImpl;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnManager;
import org.gudy.azureus2.ui.swt.views.table.utils.TableContextMenuManager;

/* Manage Tables
 *
 * @author TuxPaper
 * @since 2.0.8.5
 */
public class TableManagerImpl 
       implements TableManager
{	
	protected static TableManagerImpl	singleton;

	protected Map tableListeners = new HashMap();
	public static final int LISTENER_TABLEROW_BEFORE_REFRESH = 0;
	public static final int LISTENER_TABLEROW_AFTER_REFRESH = 1;
	public static final int LISTENER_TABLEROW_ADDED = 2;
	public static final int LISTENER_TABLEROW_REMOVED = 3;
	
	public synchronized static TableManagerImpl getSingleton() {
	  if (singleton == null)
	    singleton = new TableManagerImpl();
    return singleton;
	}

  public TableColumn createColumn(String tableID, String cellID) {
    return new TableColumnImpl(tableID, cellID);
  }

  public void addColumn(TableColumn tableColumn) {
    if (!(tableColumn instanceof TableColumnCore))
			throw(new UIRuntimeException("TableManager.addColumn(..) can only add columns created by createColumn(..)"));
    
    TableColumnManager.getInstance().addColumn((TableColumnCore)tableColumn);
  }
  
  public TableContextMenuItem addContextMenuItem(String tableID, String resourceKey) {
    TableContextMenuItemImpl item = new TableContextMenuItemImpl(tableID, resourceKey);
    TableContextMenuManager.getInstance().addContextMenuItem(item);
    return item;
  }


  public synchronized void addTableListener(String sTableID,
                                            TableListener listener) {
    ArrayList listeners = (ArrayList)tableListeners.get(sTableID);
    if (listeners == null) {
      listeners = new ArrayList();
  		tableListeners.put(sTableID, listeners);
    }
		listeners.add(listener);
  }

  public synchronized void removeTableListener(String sTableID,
                                               TableListener listener) {
    ArrayList listeners = (ArrayList)tableListeners.get(sTableID);
    if (listeners != null) {
  		listeners.remove(listener);
  	}
  }

	public void notifyRowRefresh(String sTableID, int iFunctionID, 
	                             TableRow rowInterface) {
    ArrayList listeners = (ArrayList)tableListeners.get(sTableID);
    if (listeners == null || listeners.size() == 0) {
      return;
    }

    if (iFunctionID == LISTENER_TABLEROW_BEFORE_REFRESH) {
  		for (int i = 0; i < listeners.size(); i++) {
  			((TableListener)listeners.get(i)).rowBeforeRefresh(rowInterface);
  		}
  	} else if (iFunctionID == LISTENER_TABLEROW_AFTER_REFRESH) {
  		for (int i = 0; i < listeners.size(); i++) {
  			((TableListener)listeners.get(i)).rowAfterRefresh(rowInterface);
  		}
  	} else if (iFunctionID == LISTENER_TABLEROW_ADDED) {
  		for (int i = 0; i < listeners.size(); i++) {
  			((TableListener)listeners.get(i)).rowAdded(rowInterface);
  		}
  	} else if (iFunctionID == LISTENER_TABLEROW_REMOVED) {
  		for (int i = 0; i < listeners.size(); i++) {
  			((TableListener)listeners.get(i)).rowRemoved(rowInterface);
  		}
  	}
	}
}
