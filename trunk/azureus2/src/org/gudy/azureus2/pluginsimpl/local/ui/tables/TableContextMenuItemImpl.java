/*
 * Azureus - a Java Bittorrent client
 * 2004/May/16 TuxPaper
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
 
package org.gudy.azureus2.pluginsimpl.local.ui.tables;

import java.util.ArrayList;
import java.util.List;

import org.gudy.azureus2.plugins.ui.UIManagerEvent;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.plugins.ui.menus.*;
import org.gudy.azureus2.pluginsimpl.local.ui.menus.MenuItemImpl;

public class TableContextMenuItemImpl extends MenuItemImpl implements TableContextMenuItem {

  private String sTableID;
  private List	m_listeners	= new ArrayList();
  
  public TableContextMenuItemImpl(String tableID, String key) {
	  super(MenuManager.MENU_TABLE, key);
	  sTableID = tableID;
  }
  
  public TableContextMenuItemImpl(TableContextMenuItemImpl ti, String key) {
	  super(ti, key);
	  this.sTableID = ti.getTableID();
  }

  public String getTableID() {
    return sTableID;
  }

  // Currently used by TableView.
  public void invokeListeners(TableRow[] rows) {
	  // We invoke the multi listeners first...
	  invokeListenersOnList(this.m_listeners, rows);
	  for (int i=0; i<rows.length; i++) {
		  invokeListeners(rows[i]);
	  }
  }
 
  public void addMultiListener(MenuItemListener l) {
	  m_listeners.add(l);
  }
  
  public void removeMultiListener(MenuItemListener l) {
	  m_listeners.remove(l);
  }
  
  public void remove() {
		removeWithEvents(UIManagerEvent.ET_REMOVE_TABLE_CONTEXT_MENU_ITEM,
				UIManagerEvent.ET_REMOVE_TABLE_CONTEXT_SUBMENU_ITEM);
		this.m_listeners.clear();
	}
  
}