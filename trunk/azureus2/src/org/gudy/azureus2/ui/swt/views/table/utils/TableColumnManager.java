/*
 * File    : TableColumnManager.java
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

import java.util.*;
import org.gudy.azureus2.plugins.ui.tables.TableManager;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.plugins.ui.tables.mytorrents.PluginMyTorrentsItemFactory;
import org.gudy.azureus2.plugins.ui.tables.peers.PluginPeerItemFactory;
import org.gudy.azureus2.ui.swt.views.table.TableColumnCore;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.OldMyTorrentsPluginItem;
import org.gudy.azureus2.ui.swt.views.tableitems.peers.OldPeerPluginItem;


/** Holds a list of column definitions (TableColumnCore) for 
 * all the tables in Azureus.
 *
 * Colum definitions are added via 
 * PluginInterface.addColumn(TableColumn)
 * See Use javadoc section for more uses.
 *
 * @author Oliver (Original Code)
 * @author TuxPaper (Modifications to make generic & comments)
 */
public class TableColumnManager {

  private static TableColumnManager instance;
  /* Holds all the TableColumnCore objects.
   * key   = TABLE_* type (see TableColumnCore)
   * value = Map:
   *           key = column name
   *           value = TableColumnCore object
   */
  private Map items;
  
  private TableColumnManager() {
   items = new HashMap();
  }
  
  /** Retrieve the static TableColumnManager instance
   * @return the static TableColumnManager instance
   */
  public static synchronized TableColumnManager getInstance() {
    if(instance == null)
      instance = new TableColumnManager();
    return instance;
  }
  
  /** Adds a column definition to the list
   * @param item The column definition object
   */
  public void addColumn(TableColumnCore item) {
    try {
      String name = item.getName();
      String sTableID = item.getTableID();
      synchronized(items) {
        Map mTypes = (Map)items.get(sTableID);
        if (mTypes == null) {
          // LinkedHashMap to preserve order
          mTypes = new LinkedHashMap();
          items.put(sTableID, mTypes);
        }
        if (!mTypes.containsKey(name)) {
          mTypes.put(name, item);
          ((TableColumnCore)item).loadSettings();
        }
      }
      if (!item.getColumnAdded()) {
        item.setColumnAdded(true);
      }
    } catch (Exception e) {
      System.out.println("Error while adding Table Column Extension");
      e.printStackTrace();
    }
  }

  /** Add an extension from the deprecated PluginMyTorrentsItemFactory */
  public void addExtension(String name, PluginMyTorrentsItemFactory item) {
    String sAlign = item.getOrientation();
    int iAlign;
    if (sAlign.equals(PluginMyTorrentsItemFactory.ORIENT_RIGHT))
      iAlign = TableColumnCore.ALIGN_TRAIL;
    else
      iAlign = TableColumnCore.ALIGN_LEAD;

    int iVisibleIn = item.getTablesVisibleIn();
    if ((iVisibleIn & PluginMyTorrentsItemFactory.TABLE_COMPLETE) != 0) {
      TableColumnCore tci = 
        new OldMyTorrentsPluginItem(TableManager.TABLE_MYTORRENTS_COMPLETE, 
                                    name, item);
      tci.initialize(iAlign, item.getDefaultPosition(), item.getDefaultSize());
      addColumn(tci);
    }
    if ((iVisibleIn & PluginMyTorrentsItemFactory.TABLE_INCOMPLETE) != 0) {
      TableColumnCore tci = 
        new OldMyTorrentsPluginItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE, 
                                    name, item);
      tci.initialize(iAlign, item.getDefaultPosition(), item.getDefaultSize());
      addColumn(tci);
    }
  }

  /** Add an extension from the deprecated PluginPeerItemFactory */
  public void addExtension(String name, PluginPeerItemFactory item) {
    TableColumnCore tci = new OldPeerPluginItem(TableManager.TABLE_TORRENT_PEERS,
                                            name, item);
    tci.initialize(TableColumnCore.ALIGN_LEAD, 
                   TableColumnCore.POSITION_INVISIBLE, item.getDefaultSize());
    addColumn(tci);
  }

  /** Retrieves TableColumnCore objects of a particular type.
   * @param sTableID TABLE_* constant.  See {@link TableColumn} for list 
   * of constants
   *
   * @return Map of column definition objects matching the supplied criteria.
   *         key = name
   *         value = TableColumnCore object
   */
  public Map getTableColumnsAsMap(String sTableID) {
    //System.out.println("getTableColumnsAsMap(" + sTableID + ")");
    synchronized(items) {
      Map mReturn = new LinkedHashMap();
      Map mTypes = (Map)items.get(sTableID);
      if (mTypes != null) {
        mReturn.putAll(mTypes);
      }
      //System.out.println("getTableColumnsAsMap(" + sTableID + ") returnsize: " + mReturn.size());
      return mReturn;
    }
  }
  
  public TableColumnCore[] getAllTableColumnCoreAsArray(String sTableID) {
    Map mTypes = (Map)items.get(sTableID);
    if (mTypes != null) {
      return (TableColumnCore[])mTypes.values().toArray(new TableColumnCore[0]);
    }
    return new TableColumnCore[0];
  }
  
  public TableColumnCore getTableColumnCore(String sTableID,
                                            String sColumnName) {
    Map mTypes = (Map)items.get(sTableID);
    if (mTypes == null)
      return null;
    return (TableColumnCore)mTypes.get(sColumnName);
  }
  
  public void ensureIntegrety(String sTableID) {
    Map mTypes = (Map)items.get(sTableID);
    if (mTypes == null)
      return;

    TableColumnCore[] tableColumns = 
      (TableColumnCore[])mTypes.values().toArray(new TableColumnCore[0]);

    Arrays.sort(tableColumns, new Comparator () {
      public final int compare (Object a, Object b) {
        int iPositionA = ((TableColumnCore)a).getPosition();
        if (iPositionA == TableColumnCore.POSITION_LAST)
          iPositionA = 0xFFFF;
        int iPositionB = ((TableColumnCore)b).getPosition();
        if (iPositionB == TableColumnCore.POSITION_LAST)
          iPositionB = 0xFFFF;

        return iPositionA - iPositionB;
      }
    });
    int iPos = 0;
    for (int i = 0; i < tableColumns.length; i++) {
      int iCurPos = tableColumns[i].getPosition();
      if (iCurPos >= 0 || iCurPos == TableColumnCore.POSITION_LAST) {
        tableColumns[i].setPositionNoShift(iPos++);
      }
    }
  }

  /** Saves all the user configurable Table Column settings at once, complete
   * with a COConfigurationManager.save().
   *
   * @param sTableID Table to save settings for
   */
  public void saveTableColumns(String sTableID) {
    TableColumnCore[] tcs = getAllTableColumnCoreAsArray(sTableID);
    for (int i = 0; i < tcs.length; i++) {
      if (tcs[i] != null)
        tcs[i].saveSettings();
    }
    COConfigurationManager.save();
  }
}
