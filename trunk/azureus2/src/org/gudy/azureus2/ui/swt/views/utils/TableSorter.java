/*
 * File : TableSorter.java Created : 23 nov. 2003 By : Olivier
 * 
 * Azureus - a Java Bittorrent client
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
 */

package org.gudy.azureus2.ui.swt.views.utils;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core3.config.COConfigurationManager;

/**
 * @author Olivier
 *  
 */
public class TableSorter {

  private String lastField;
  private boolean lastFieldIsInt;
  private boolean ascending;
  private int loopFactor;
  
  private SortableTable sortableTable;
  private Table table;
  
  private Map objectToSortableItem;
  private Map tableItemToObject;

  public TableSorter(SortableTable sortableTable, String defaultField,boolean isDefaultInt) {
    loopFactor = 0;
    ascending = true;
    this.lastField = defaultField;
    this.lastFieldIsInt = isDefaultInt;
    this.sortableTable = sortableTable;
    this.objectToSortableItem = sortableTable.getObjectToSortableItemMap();
    this.tableItemToObject = sortableTable.getTableItemToObjectMap();
    this.table = sortableTable.getTable();
  }
  
  public void reOrder(boolean force) {
    int reOrderDelay = COConfigurationManager.getIntParameter("ReOrder Delay");
    
    if(!force && (reOrderDelay == 0 || loopFactor++ < reOrderDelay))
      return;
    loopFactor = 0;
    if(lastField != null) {
      ascending = !ascending;
      if(lastFieldIsInt)
        orderInt(lastField);
      else
        orderString(lastField);
    }
  }

  public void addIntColumnListener(TableColumn column, String field) {
    column.addListener(SWT.Selection, new IntColumnListener(field));
  }

  public void addStringColumnListener(TableColumn column, String field) {
    column.addListener(SWT.Selection, new StringColumnListener(field));
  }
  
  private void computeAscending(String field) {
    if (lastField.equals(field))
      ascending = !ascending;
    else {
      lastField = field;
      ascending = true;
    }
  }

  private class IntColumnListener implements Listener {

    private String field;

    public IntColumnListener(String field) {
      this.field = field;
    }

    public void handleEvent(Event e) {
      lastFieldIsInt = true;
      orderInt(field);      
    }
  }

  private class StringColumnListener implements Listener {

    private String field;

    public StringColumnListener(String field) {
      this.field = field;
    }

    public void handleEvent(Event e) {
      lastFieldIsInt = false;
      orderString(field);      
    }
  }

  private void orderInt(String field) {
    computeAscending(field);
    synchronized (objectToSortableItem) {
      List selected = getSelection();
      List ordered = new ArrayList(objectToSortableItem.size());
      SortableItem sortableItems[] = new SortableItem[objectToSortableItem.size()];
      Iterator iter = objectToSortableItem.keySet().iterator();
      while (iter.hasNext()) {
        Object dataSource = (Object) iter.next();
        SortableItem item = (SortableItem) objectToSortableItem.get(dataSource);
        int index = item.getIndex();
        if (index == -1)
          continue;
        sortableItems[index] = item;
        long value = item.getIntField(field);
        int i;
        for (i = 0; i < ordered.size(); i++) {
          Object dataSourcei = (Object) ordered.get(i);
          SortableItem itemi = (SortableItem) objectToSortableItem.get(dataSourcei);
          long valuei = itemi.getIntField(field);
          if (ascending) {
            if (valuei >= value)
              break;
          } else {
            if (valuei <= value)
              break;
          }
        }
        ordered.add(i, dataSource);
      }

      sort(sortableItems, ordered, selected);

    }
    sortableTable.refresh();
  }
  
  private void orderString(String field) {
      computeAscending(field);
      synchronized (objectToSortableItem) {
        List selected = getSelection();
        Collator collator = Collator.getInstance(Locale.getDefault());
        List ordered = new ArrayList(objectToSortableItem.size());
        SortableItem sortableItems[] = new SortableItem[objectToSortableItem.size()];
        Iterator iter = objectToSortableItem.keySet().iterator();
        while (iter.hasNext()) {
          Object dataSource = (Object) iter.next();
          SortableItem item = (SortableItem) objectToSortableItem.get(dataSource);
          int index = item.getIndex();
          if(index == -1)
            continue;
          sortableItems[index] = item;
          String value = item.getStringField(field);
          int i;
          for (i = 0; i < ordered.size(); i++) {
            Object dataSourcei = (Object) ordered.get(i);
            SortableItem itemi = (SortableItem) objectToSortableItem.get(dataSourcei);
            String valuei = itemi.getStringField(field);
            if (ascending) {
              if (collator.compare(valuei, value) <= 0)
                break;
            }
            else {
              if (collator.compare(valuei, value) >= 0)
                break;
            }
          }
          ordered.add(i, dataSource);
        }

        sort(sortableItems, ordered, selected);
      }
      sortableTable.refresh();
    }

  private void sort(SortableItem[] items, List ordered, List selected) {
    for (int i = 0; i < ordered.size(); i++) {
      Object dataSource = (Object) ordered.get(i);

      items[i].setDataSource(dataSource);

      objectToSortableItem.put(dataSource, items[i]);
      tableItemToObject.put(items[i].getTableItem(), dataSource);
      items[i].invalidate();
      if (selected.contains(dataSource)) {
        table.select(i);
      } else {
        table.deselect(i);
      }
    }
  }
  
  private List getSelection() {
    TableItem[] selection = table.getSelection();
    Map items = sortableTable.getTableItemToObjectMap();
    List selected = new ArrayList(selection.length);
    for(int i = 0 ; i < selection.length ; i++) {                
      Object piece = (Object) items.get(selection[i]);
      if(piece != null)
        selected.add(piece);
    }
    return selected;
  }

  /**
   * @return Returns the lastField.
   */
  public String getLastField() {
    return lastField;
  }

}
