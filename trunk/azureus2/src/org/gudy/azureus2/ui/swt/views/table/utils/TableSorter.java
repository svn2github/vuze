/*
 * File : TableSorter.java 
 * Created : 23 nov. 2003 By : Olivier
 *
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

package org.gudy.azureus2.ui.swt.views.table.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.swt.widgets.TableItem;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.ui.swt.views.table.TableRowCore;
import org.gudy.azureus2.ui.swt.views.table.TableCellCore;
import org.gudy.azureus2.ui.swt.views.table.TableColumnCore;
import org.gudy.azureus2.ui.swt.components.BufferedTableRow;

/**
 * @author Olivier
 *
 * @author TuxPaper
 *         2004/Apr/20: Remove need for tableItemToObject (store object in tableItem.setData)
 *         2004/May/11: Use Comparable instead of SortableItem
 *         2004/May/14: moved from org.gudy.azureus2.ui.swt.utils
 */
public class TableSorter implements ParameterListener {
  private static int reOrderDelay = COConfigurationManager.getIntParameter("ReOrder Delay");

  private TableColumnCore lastSortedTableColumn;
  private boolean bLastAscending;
  private int loopFactor;

  private SortableTable sortableTable;
  private String configTableName;


  public TableSorter(SortableTable sortableTable,
                     String configTableName,
                     String defaultField) {
  	this(sortableTable, configTableName, defaultField, true);
  }

  public TableSorter(SortableTable sortableTable, String configTableName,
                     String defaultField, boolean isDefaultAscending) {
    loopFactor = 0;
    String sSortColumn = COConfigurationManager.getStringParameter(configTableName + ".sortColumn",
                                                                   defaultField);
    lastSortedTableColumn = sortableTable.getTableColumnCore(sSortColumn);
    bLastAscending = COConfigurationManager.getBooleanParameter(configTableName + ".sortAsc",
                                                                isDefaultAscending);
    this.sortableTable = sortableTable;
    this.configTableName = configTableName;
    COConfigurationManager.addParameterListener("ReOrder Delay", this);
  }

  public void reOrder(boolean force) {
    if (!force && (reOrderDelay == 0 || loopFactor++ < reOrderDelay))
      return;
    loopFactor = 0;
    sortColumn();
  }

  public void parameterChanged(String parameterName) {
    reOrderDelay = COConfigurationManager.getIntParameter("ReOrder Delay");
  }

  public String getLastField() {
    if (lastSortedTableColumn == null)
      return "";
    return lastSortedTableColumn.getName();
  }
  
  public boolean isAscending() {
    return bLastAscending;
  }

  public void sortColumn(boolean bForce) {
    sortColumn(lastSortedTableColumn, bLastAscending, bForce);
  }

  public void sortColumn() {
    sortColumn(lastSortedTableColumn, bLastAscending, false);
  }

  public void sortColumnReverse(TableColumnCore tableColumn) {
    sortColumn(tableColumn, 
               (lastSortedTableColumn == tableColumn) ? !bLastAscending : true, 
               false);
  }

  public void sortColumn(TableColumnCore tableColumn, boolean bAscending) {
    sortColumn(tableColumn, bAscending, false);
  }

  public void sortColumn(TableColumnCore tableColumn, boolean bAscending, boolean bForce) {
    if (tableColumn == null)
      return;

    String sColumnName = tableColumn.getName();

    // Store any changes
    if (lastSortedTableColumn != tableColumn) {
      COConfigurationManager.setParameter(configTableName + ".sortColumn",
                                          sColumnName);
      lastSortedTableColumn = tableColumn;
    }
    if (bLastAscending != bAscending) {
      COConfigurationManager.setParameter(configTableName + ".sortAsc",
                                          bAscending);
      this.bLastAscending = bAscending;
    }

    List columnCellList = sortableTable.getColumnCoreCells(sColumnName);
    TableCellCore[] cells = (TableCellCore[])columnCellList.toArray(new TableCellCore[0]);

    TableCellCore[] cellsOriginal = (TableCellCore[])cells.clone();
    TableItem[] tableItems = new TableItem[cells.length];
    boolean[] selected = new boolean[cells.length];
    for (int i = 0; i < cells.length; i++) {
      BufferedTableRow row = (BufferedTableRow)cells[i].getTableRowCore();
      tableItems[i] = row.getItem();
      selected[i] = row.getSelected();
    }
    
    if (bForce) {
      for (int i = 0; i < cells.length; i++)
        cells[i].refresh();
    }

    if (bLastAscending)
      Arrays.sort(cells);
    else
      Arrays.sort(cells, Collections.reverseOrder());

/* Flicker
    for (int i = 0; i < cells.length; i++)
      cells[i].getTableRowCore().setIndex(i);
*/

    for (int i = 0; i < cells.length; i++) {
      if (cells[i] != cellsOriginal[i]) {
        TableRowCore row = cells[i].getTableRowCore();
        boolean bWasSelected = selected[row.getIndex()];
        ((BufferedTableRow)row).setTableItem(tableItems[i], false);
        ((BufferedTableRow)row).setSelected(bWasSelected);
        tableItems[i].setData("TableRow", row);
        row.setValid(false);
        row.refresh(true);
      }
    }
  }
}
