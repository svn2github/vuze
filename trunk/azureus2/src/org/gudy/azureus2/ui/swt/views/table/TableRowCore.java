/*
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
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
 *
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.views.table;


import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Color;

import org.gudy.azureus2.plugins.ui.tables.TableRow;


/** Core Table Row functions are those available to plugins plus
 * some core-only functions.  The core-only functions are listed here.
 *
 * @author TuxPaper
 * @since 2.0.8.5 2004/May/14
 */
public interface TableRowCore
       extends TableRow
{
  /** Sets the Validitiliy of the row
   *
   * @param valid The valid to set.
   */
  public void setValid(boolean valid);

  /** Delete the row */
  public void delete();

  /** Refresh all the cells in the row 
   *
   * @param bDoGraphics Refresh graphic cells to
   */
  public void refresh(boolean bDoGraphics);

  /** re-paint an area of the row
   *
   * @param gc Area needing repainting, and GC object one can use to repaint it
   */
  public void doPaint(GC gc);

  /** Location of a column has changed
   *
   * @param iStartColumn Cells starting at this value may need repainting
   *
   * XXX Rename to cellLocationChanged?
   */
  public void locationChanged(int iStartColumn);

  /** Retrieve the Data Source related to this row
   *
   * @param bCoreObject true - return a core object<br>
   *                    false - return a plugin object
   * @return the Data Source Object related to the row
   */
  public Object getDataSource(boolean bCoreObject);
  
  /** Move the row to a new location
   *
   * @param newIndex where to move the row to
   * @return true - changed<br>
   *         false - not changed or already at that index
   */
  public boolean setIndex(int newIndex);

  /** Set the data source that's related to this row
   *
   * @param dataSource Data Source related to this row
   *
   * @return true - dataSource changed.  false - already set
   */
  public boolean setDataSource(Object dataSource);

  /** Adjust cell height.  Don't use if any other column/cell uses setImage()
   *
   * @param iHeight new Row Height.  Will not reduce row's height (SWT)
   */
  public void setHeight(int iHeight);

  /** Retrieve a cell based on the supplied value
   *
   * @param field Column name of the cell to be returned
   * @return TableCellCore object related to this row and the specified column 
   */
  public TableCellCore getTableCellCore(String field);

  /** Retreive the color of the row
   *
   * @return color of the row
   */
	public Color getForeground();
  /** Set the color of the row
   *
   * @param c new color
   */
	public void setForeground(Color	c);
}
