/*
 * File    : ConfigBasedItemEnumerator.java
 * Created : 25 nov. 2003
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
 
package org.gudy.azureus2.ui.swt.views.tableitems.utils;

import java.util.StringTokenizer;

import org.eclipse.swt.SWT;
import org.gudy.azureus2.core3.config.COConfigurationManager;

/**
 * @author Olivier
 *
 * @deprecated use TableColumnManager
 * XXX DeleteMe 
 */
public class ConfigBasedItemEnumerator extends ItemEnumerator {

  private String tableName;
  
  private ConfigBasedItemEnumerator(String tableName,ItemDescriptor[] items) {
    super(items);  
    this.tableName = tableName;
  }
  
  /**
   * Creates an ItemEnumerator, based on config.<br>
   * Default values are passed using the 'tableItems' Strings.<br>
   * A tableItem value must be composed of name;type(I|S);default width;default position<br>
   * Example : "status;I;45;5"
   * @param tableName
   * @param tableItems 
   */
  public static ConfigBasedItemEnumerator getInstance(String tableName,String[] tableItems) {
    ItemDescriptor[] items = new ItemDescriptor[tableItems.length];
    for(int i = 0 ; i < items.length ; i++) {
      StringTokenizer st = new StringTokenizer(tableItems[i],";");      
      String itemName = st.nextToken();
      String strAlign = st.nextToken();
      int align = SWT.LEFT;
      if(strAlign.equals("C")) align = SWT.CENTER; 
      if(strAlign.equals("R")) align = SWT.RIGHT;
        
      int type = st.nextToken().equals("I") ? ItemDescriptor.TYPE_INT : ItemDescriptor.TYPE_STRING;
      int dWidth = Integer.parseInt(st.nextToken());
      int dPosition = Integer.parseInt(st.nextToken());
      int width = COConfigurationManager.getIntParameter("Table.".concat(tableName).concat(".").concat(itemName).concat(".width"),dWidth);
      int position = COConfigurationManager.getIntParameter("Table.".concat(tableName).concat(".").concat(itemName).concat(".position"),dPosition);
      items[i] = new ItemDescriptor(itemName,align,type,position,width);
    }
    return new ConfigBasedItemEnumerator(tableName,items);
  }
  
  public void save() {
    ItemDescriptor[] items = getItems();
    for(int i = 0 ; i < items.length ; i++) {
      String name = items[i].getName();
      int position = items[i].getPosition();
      COConfigurationManager.setParameter("Table.".concat(tableName).concat(".").concat(name).concat(".position"),position);
    }
  }

}
