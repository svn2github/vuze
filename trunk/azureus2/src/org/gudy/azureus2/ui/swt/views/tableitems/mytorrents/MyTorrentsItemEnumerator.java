/*
 * File    : MyTorrentsItemDescriptor.java
 * Created : 24 nov. 2003
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
 
package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import org.gudy.azureus2.ui.swt.views.tableitems.utils.ItemDescriptor;
import org.gudy.azureus2.ui.swt.views.tableitems.utils.ItemEnumerator;

/**
 * @author Olivier
 *
 */
public class MyTorrentsItemEnumerator extends ItemEnumerator {
  
  private MyTorrentsItemEnumerator(ItemDescriptor[] items) {
    super(items);
  }
  
  public static MyTorrentsItemEnumerator getInstance() {
    ItemDescriptor[] items = new ItemDescriptor[12];
    items[0] = new ItemDescriptor("#",ItemDescriptor.TYPE_INT,0,25);
    items[1] = new ItemDescriptor("name",ItemDescriptor.TYPE_STRING,1,250);
    items[2] = new ItemDescriptor("size",ItemDescriptor.TYPE_INT,2,70);
    items[3] = new ItemDescriptor("done",ItemDescriptor.TYPE_INT,3,55);
    items[4] = new ItemDescriptor("status",ItemDescriptor.TYPE_INT,4,80);
    items[5] = new ItemDescriptor("seeds",ItemDescriptor.TYPE_INT,5,45);
    items[6] = new ItemDescriptor("peers",ItemDescriptor.TYPE_INT,6,45);
    items[7] = new ItemDescriptor("downspeed",ItemDescriptor.TYPE_INT,7,70);
    items[8] = new ItemDescriptor("upspeed",ItemDescriptor.TYPE_INT,8,70);
    items[9] = new ItemDescriptor("eta",ItemDescriptor.TYPE_INT,9,70);
    items[10] = new ItemDescriptor("tracker",ItemDescriptor.TYPE_INT,10,70);
    items[11] = new ItemDescriptor("priority",ItemDescriptor.TYPE_INT,11,70);
    return new MyTorrentsItemEnumerator(items);
  }

}
