/*
 * File    : CoreTableColumn.java
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

import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.impl.TableColumnImpl;

/** This class  provides constructors for setting most of
 * the common column attributes and sets the column as a 'core' column.<p>
 *
 * @author TuxPaper
 */
public class CoreTableColumn 
       extends TableColumnImpl 
{
  /** Construct a new CoreTableColumn
   *
   * @param sName See {@link #getName()}
   * @param iAlignment See {@link #getAlignment()}
   * @param iPosition See {@link #getPosition(int)}
   * @param iWidth See {@link #getWidth()}
   * @param sTableID See {@link #getTableID()}
   */
  public CoreTableColumn(String sName, int iAlignment,
                         int iPosition, int iWidth,
                         String sTableID) {
    super(sTableID, sName);
    super.initialize(iAlignment, iPosition, iWidth);
    setUseCoreDataSource(true);
    addListeners();
  }

  /** Construct a new CoreTableColumn.<p>
   * getAlignment() will be determined by iType.  TYPE_STRING will be ALIGN_LEAD, 
   * TYPE_LONG will be ALIGN_TRAIL, and TYPE_GRAPHIC will be ALIGN_CENTER.
   * <p>
   * getPosition(int) will be POSITION_INVISIBLE
   *
   * @param sName See {@link TableColumn#setName()}
   * @param iPosition See {@link TableColumn#setPosition(int)}
   * @param iWidth See {@link TableColumn#setWidth()}
   * @param sTableID See {@link TableColumn#setTableID()}
   */
  public CoreTableColumn(String sName, int iPosition, int iWidth, 
                         String sTableID) {
    super(sTableID, sName);
    setPosition(iPosition);
    setWidth(iWidth);
    setUseCoreDataSource(true);
    addListeners();
  }

  public CoreTableColumn(String sName, int iWidth, String sTableID) {
    super(sTableID, sName);
    setWidth(iWidth);
    setUseCoreDataSource(true);
    addListeners();
  }

  public CoreTableColumn(String sName, String sTableID) {
    super(sTableID, sName);
    setUseCoreDataSource(true);
    addListeners();
  }
  
  public void initializeAsGraphic(int iPosition, int iWidth) {
    setPosition(iPosition);
    setWidth(iWidth);
    setType(TYPE_GRAPHIC);
    setRefreshInterval(INTERVAL_GRAPHIC);
    addListeners();
  }
  
  private void addListeners() {
    if (this instanceof TableCellAddedListener)
      addCellAddedListener((TableCellAddedListener)this);
    if (this instanceof TableCellRefreshListener)
      addCellRefreshListener((TableCellRefreshListener)this);
  }
}
