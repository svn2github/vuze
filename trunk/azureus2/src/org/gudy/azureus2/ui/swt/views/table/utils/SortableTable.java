/*
 * File    : SortableTable.java
 * Created : 23 nov. 2003
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

import java.util.List;

import org.gudy.azureus2.ui.swt.views.table.TableColumnCore;

/** Functions that TableSorter needs in order to sort a column
 *
 * @author Olivier
 * @author TuxPaper
 *         2004/Apr/20: Remove need for tableItemToObject
 *         2004/May/14: Moved to table.utils
 */
public interface SortableTable {
  /** Retrieve a TableColumnCore object based on it's ID/name
   *
   * @param sColumnName name/ID of column
   * @return TableColumnCore object associated with the specified column
   */
  public TableColumnCore getTableColumnCore(String sColumnName);
  
  /** Retreive all the cells for a column
   *
   * @param sColumnName ID of column
   * @return list of TableCellCore object
   */
  public List getColumnCoreCells(String sColumnName);
}
