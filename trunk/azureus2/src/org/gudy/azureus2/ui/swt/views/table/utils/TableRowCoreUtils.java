/*
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

import java.util.Collection;
import java.util.Iterator;

import org.eclipse.swt.graphics.GC;

import org.gudy.azureus2.ui.swt.views.table.TableRowCore;


public class TableRowCoreUtils {
  public static void refresh(Collection colTableRows, boolean bDoGraphics) {
    Iterator iter = colTableRows.iterator();
    while (iter.hasNext()) {
      TableRowCore row = (TableRowCore)iter.next();
      if (row != null) row.refresh(bDoGraphics);
    }
  }

  public static void locationChanged(Collection colTableRows, int iStartColumn) {
    Iterator iter = colTableRows.iterator();
    while (iter.hasNext()) {
      TableRowCore row = (TableRowCore)iter.next();
      if (row != null) row.locationChanged(iStartColumn);
    }
  }

  public static void doPaint(Collection colTableRows, GC gc) {
    Iterator iter = colTableRows.iterator();
    while (iter.hasNext()) {
      TableRowCore row = (TableRowCore)iter.next();
      if (row != null) row.doPaint(gc);
    }
  }

  public static void delete(Collection colTableRows) {
    Iterator iter = colTableRows.iterator();
    while (iter.hasNext()) {
      TableRowCore row = (TableRowCore)iter.next();
      if (row != null) row.delete();
    }
  }
}