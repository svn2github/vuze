/*
 * File    : TableCellCore.java
 * Created : 2004/May/14
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
 
package org.gudy.azureus2.ui.swt.views.table;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;

import org.gudy.azureus2.plugins.ui.tables.TableCell;


/** Core Table Cell functions are those available to plugins plus
 * some core-only functions.  The core-only functions are listed here.
 *
 * @see TableCellImpl
 *
 * @future split out SWT functions to TableCellSWTCore and move TableCellCore
 *         out of swt package. An abstract adapter for TableCell may have to 
 *         be created which implents any SWT functions (overriden by SWT 
 *         implementation)
 */
public interface TableCellCore
       extends TableCell, Comparable
{
  /** Change the cell's foreground color.
   *
   * NOTE: favor (R, G, B)
   *
   * @param color SWT Color object.
   * @return True - Color changed. <br>
   *         False - Color was already set.
   */
  boolean setForeground(Color color);

  /** Refresh the cell */
  public void refresh();
  
  /** dispose of the cell */
  public void dispose();
  
  /** Set the cell's image
   *
   * @param img Cell's new image
   */
  public void setImage(Image img);

  /** Retrieve whether the cell need any paint calls (graphic)
   *
   * @return whether the cell needs painting
   */
  public boolean needsPainting();
  
  /** Paint the cell (for graphics)
   *
   * @param gc GC object to be used for painting
   */
  public void doPaint(GC gc);

  /** Location of the cell has changed */
  public void locationChanged();

  /** Retrieve the row that this cell belongs to
   *
   * @return the row that this cell belongs to
   */
  public TableRowCore getTableRowCore();
}
