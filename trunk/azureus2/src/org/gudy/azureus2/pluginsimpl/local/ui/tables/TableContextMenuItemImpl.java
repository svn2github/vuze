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
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.plugins.ui.menus.*;

public class TableContextMenuItemImpl 
       implements TableContextMenuItem
{
  private String sTableID;
  private String sName;
  protected List listeners  = new ArrayList();

  public TableContextMenuItemImpl(String tableID, String key) {
    sTableID = tableID;
    sName = key;
  }

  public String getTableID() {
    return sTableID;
  }

  public String getResourceKey() {
    return sName;
  }
  
  public void invokeListeners(TableRow row) {
    if (listeners == null)
      return;
    for (int i = 0; i < listeners.size(); i++)
      ((MenuItemListener)(listeners.get(i))).selected(this, row);
  }

  public void addListener(MenuItemListener l) {
    listeners.add(l);
  }
  
  public void removeListener(MenuItemListener l) {
    listeners.remove(l);
  }
}