/*
 * File    : TableCellImpl.java
 * Created : 24 nov. 2003
 * By      : Olivier
 * Originally PluginItem.java, and changed to be more generic.
 *
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
 
package org.gudy.azureus2.ui.swt.views.table.impl;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.SWT.GraphicSWT;
import org.gudy.azureus2.plugins.ui.tables.TableCellDisposeListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellToolTipListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.pluginsimpl.local.ui.SWT.GraphicSWTImpl;
import org.gudy.azureus2.ui.swt.components.BufferedGraphicTableItem;
import org.gudy.azureus2.ui.swt.components.BufferedGraphicTableItem1;
import org.gudy.azureus2.ui.swt.components.BufferedGraphicTableItem2;
import org.gudy.azureus2.ui.swt.components.BufferedTableItem;
import org.gudy.azureus2.ui.swt.components.BufferedTableRow;
import org.gudy.azureus2.ui.swt.views.table.TableCellCore;
import org.gudy.azureus2.ui.swt.views.table.TableColumnCore;
import org.gudy.azureus2.ui.swt.views.table.TableRowCore;


/** TableCellImpl represents one cell in the table.  
 * Table access is provided by BufferedTableItem.  
 * TableCellImpl is stored in and accessed by TableRowCore.
 * Drawing control gets passed to listeners.
 *
 * For plugins, this object is the implementation to TableCell.
 *
 * This object is needed to split core code from plugin code.
 */
public class TableCellImpl 
       implements TableCellCore
{
  private TableRowCore tableRow;
  private Comparable sortValue;
  private BufferedTableItem bufferedTableItem;
  private ArrayList refreshListeners;
  private ArrayList disposeListeners;
  private ArrayList tooltipListeners;
  private TableColumnCore tableColumn;
  private boolean valid;
  private int refreshErrLoopCount;
  private int loopFactor;
  private Object oToolTip;
  
  public TableCellImpl(TableRowCore tableRow, TableColumnCore tableColumn) {
    this(tableRow, tableColumn, false);
  }

  /**
   * @param bSkipFirstColumn Add 1 to position because we make a non resizable 
   *                         0-sized 1st column to fix the 1st column gap 
   *                         problem (Eclipse Bug 43910)
   */
  public TableCellImpl(TableRowCore tableRow, TableColumnCore tableColumn,
                       boolean bSkipFirstColumn) {
    this.tableColumn = tableColumn;
    this.tableRow = tableRow;
    valid = false;
    refreshErrLoopCount = 0;
    loopFactor = 0;
    int position = tableColumn.getPosition();
    position = (position >= 0 && bSkipFirstColumn) ? position + 1 : position;
    if (tableColumn.getType() != TableColumnCore.TYPE_GRAPHIC) {
      bufferedTableItem = new BufferedTableItem((BufferedTableRow)tableRow, position) {
        public void refresh() {
          TableCellImpl.this.refresh();
        }
        public void invalidate() {
          TableCellImpl.this.setValid(false);
        }
      };
    } else if (COConfigurationManager.getBooleanParameter("GUI_SWT_bAlternateTablePainting")) {
      bufferedTableItem = new BufferedGraphicTableItem2((BufferedTableRow)tableRow, position) {
        public void refresh() {
          TableCellImpl.this.refresh();
        }
        public void invalidate() {
          TableCellImpl.this.setValid(false);
        }
      };
    } else {
      bufferedTableItem = new BufferedGraphicTableItem1((BufferedTableRow)tableRow, position) {
        public void refresh() {
          TableCellImpl.this.refresh();
        }
        public void invalidate() {
          TableCellImpl.this.setValid(false);
        }
      };
    }
    tableColumn.invokeCellAddedListeners(this);
  }

  private void pluginError(Throwable e) {
    String sPosition = (bufferedTableItem == null) 
      ? "null" 
      : "" + bufferedTableItem.getPosition() + 
        " (" + bufferedTableItem.getColumnName() + ")";
    LGLogger.log(LGLogger.ERROR, 
                 "Table Cell Plugin for Column #" + sPosition + 
                 " generated an exception: " + e);
    e.printStackTrace();
  }
  
  /* Public API */
  ////////////////
  
  public Object getDataSource() {
    return tableRow.getDataSource(tableColumn.getUseCoreDataSource());
  }
  
  public TableColumn getTableColumn() {
    return tableColumn;
  }

  public TableRow getTableRow() {
    return tableRow;
  }

  public String getTableID() {
    return tableRow.getTableID();
  }
  
  public boolean isValid() {
    return valid;
  }
  
  public boolean setForeground(Color color) {
    if (bufferedTableItem == null)
      return false;
    return bufferedTableItem.setItemForeground(color);
  }
  
  public boolean setForeground(int red, int green, int blue) {
    if (bufferedTableItem == null)
      return false;
    return bufferedTableItem.setItemForeground(red, green, blue);
  }

  public boolean setText(String text) {
    if (bufferedTableItem == null)
      return false;
    return bufferedTableItem.setText(text);
  }
  
  public String getText() {
    if (bufferedTableItem == null)
      return "";
    return bufferedTableItem.getText();
  }

  public boolean isShown() {
    if (bufferedTableItem == null)
      return false;
    return bufferedTableItem.isShown();
  }
  
  public boolean setSortValue(Comparable valueToSort) {
    if (sortValue == valueToSort)
      return false;
    sortValue = valueToSort;
    return true;
  }
  
  public boolean setSortValue(long valueToSort) {
    if ((sortValue instanceof Long) && 
        ((Long)sortValue).longValue() == valueToSort)
      return false;

    sortValue = new Long(valueToSort);
    return true;
  }

  public Comparable getSortValue() {
    if (sortValue == null) {
      if (bufferedTableItem != null)
        return bufferedTableItem.getText();
      return "";
    }
    return sortValue;
  }
    
  /* Start TYPE_GRAPHIC Functions */

  public Point getSize() {
    if (bufferedTableItem == null || 
        !(bufferedTableItem instanceof BufferedGraphicTableItem))
      return null;
    return ((BufferedGraphicTableItem)bufferedTableItem).getSize();
  }

  public int getWidth() {
    if (bufferedTableItem == null || 
        !(bufferedTableItem instanceof BufferedGraphicTableItem))
      return -1;
    Point pt = ((BufferedGraphicTableItem)bufferedTableItem).getSize();
    if (pt == null)
      return -1;
    return pt.x;
  }

  public int getHeight() {
    if (bufferedTableItem == null || 
        !(bufferedTableItem instanceof BufferedGraphicTableItem))
      return -1;
    Point pt = ((BufferedGraphicTableItem)bufferedTableItem).getSize();
    if (pt == null)
      return -1;
    return pt.y;
  }

  public boolean setGraphic(Image img) {
    if (bufferedTableItem == null || 
        !(bufferedTableItem instanceof BufferedGraphicTableItem))
      return false;
    return ((BufferedGraphicTableItem)bufferedTableItem).setGraphic(img);
  }

  public boolean setGraphic(Graphic img) {
    if (bufferedTableItem == null || 
        !(bufferedTableItem instanceof BufferedGraphicTableItem))
      return false;
    if (img == null)
      return ((BufferedGraphicTableItem)bufferedTableItem).setGraphic(null);

    if (!(img instanceof GraphicSWT))
      return false;
    Image imgSWT = ((GraphicSWT)img).getImage();
    return ((BufferedGraphicTableItem)bufferedTableItem).setGraphic(imgSWT);
  }

  public Graphic getGraphic() {
    if (bufferedTableItem == null || 
        !(bufferedTableItem instanceof BufferedGraphicTableItem))
      return null;
    Image img = ((BufferedGraphicTableItem)bufferedTableItem).getGraphic();
    return new GraphicSWTImpl(img);
  }

  public Image getGraphicSWT() {
    if (bufferedTableItem == null || 
        !(bufferedTableItem instanceof BufferedGraphicTableItem))
      return null;
    return ((BufferedGraphicTableItem)bufferedTableItem).getGraphic();
  }

  public void setFillCell(boolean bFillCell) {
    if (bufferedTableItem == null || 
        !(bufferedTableItem instanceof BufferedGraphicTableItem))
      return;
    ((BufferedGraphicTableItem)bufferedTableItem).fillCell = bFillCell;
  }

  public void setMarginHeight(int height) {
    if (bufferedTableItem == null || 
        !(bufferedTableItem instanceof BufferedGraphicTableItem))
      return;
    ((BufferedGraphicTableItem)bufferedTableItem).marginHeight = height;
  }

  public void setMarginWidth(int width) {
    if (bufferedTableItem == null || 
        !(bufferedTableItem instanceof BufferedGraphicTableItem))
      return;
    ((BufferedGraphicTableItem)bufferedTableItem).marginWidth = width;
  }

  /* End TYPE_GRAPHIC Functions */

  public synchronized void addRefreshListener(TableCellRefreshListener listener) {
    if (refreshListeners == null)
      refreshListeners = new ArrayList();

    refreshListeners.add(listener);
  }

  public synchronized void removeRefreshListener(TableCellRefreshListener listener) {
    if (refreshListeners == null)
      return;

    refreshListeners.remove(listener);
  }

  public synchronized void addDisposeListener(TableCellDisposeListener listener) {
    if (disposeListeners == null) {
      disposeListeners = new ArrayList();
    }
    disposeListeners.add(listener);
  }

  public synchronized void removeDisposeListener(TableCellDisposeListener listener) {
    if (disposeListeners == null)
      return;

    disposeListeners.remove(listener);
  }
  
  public synchronized void addToolTipListener(TableCellToolTipListener listener) {
    if (tooltipListeners == null) {
      tooltipListeners = new ArrayList();
    }
    tooltipListeners.add(listener);
  }

  public synchronized void removeToolTipListener(TableCellToolTipListener listener) {
    if (tooltipListeners == null)
      return;

    tooltipListeners.remove(listener);
  }
  
  /* Start of Core-Only function */
  //////////////////////////////////
  public void setValid(boolean valid) {
    this.valid = valid;
  }

  public void refresh() {
    refresh(true);
  }

  public void refresh(boolean bDoGraphics) {
    if (bufferedTableItem == null || refreshErrLoopCount > 2)
      return;
    int iErrCount = tableColumn.getConsecutiveErrCount();
    if (iErrCount > 10)
      return;

    try {
      int iInterval = tableColumn.getRefreshInterval();
      if ((iInterval == TableColumnCore.INTERVAL_LIVE ||
          (iInterval == TableColumnCore.INTERVAL_GRAPHIC && bDoGraphics) ||
          (iInterval > 0 && (loopFactor % iInterval) == 0) ||
          !valid) && bufferedTableItem.isShown()) 
      {
        tableColumn.invokeCellRefreshListeners(this);
        if (refreshListeners != null)
          for (int i = 0; i < refreshListeners.size(); i++)
            ((TableCellRefreshListener)(refreshListeners.get(i))).refresh(this);

        setValid(true);
      }
      loopFactor++;
      refreshErrLoopCount = 0;
      if (iErrCount > 0)
        tableColumn.setConsecutiveErrCount(0);
    } catch (Throwable e) {
      refreshErrLoopCount++;
      tableColumn.setConsecutiveErrCount(++iErrCount);
      pluginError(e);
      if (refreshErrLoopCount > 2)
        LGLogger.log(LGLogger.ERROR, 
                     "TableCell will not be refreshed anymore this session.");
    }
  }


  public void dispose() {
    if (disposeListeners != null) {
      try {
        tableColumn.invokeCellDisposeListeners(this);
        for (Iterator iter = disposeListeners.iterator(); iter.hasNext();) {
          TableCellDisposeListener listener = (TableCellDisposeListener)iter.next();
          listener.dispose(this);
        }
        disposeListeners = null;
      } catch (Throwable e) {
        pluginError(e);
      }
    }

    if (bufferedTableItem != null)
      bufferedTableItem.dispose();
    
    refreshListeners = null;
    bufferedTableItem = null;
    tableColumn = null;
    tableRow = null;
    sortValue = null;
  }
  
  public void setImage(Image img) {
    if (bufferedTableItem == null)
      return;
    bufferedTableItem.setImage(img);
  }

  public boolean needsPainting() {
    if (bufferedTableItem == null)
      return false;
    return bufferedTableItem.needsPainting();
  }
  
  public void doPaint(GC gc) {
    if (bufferedTableItem == null)
      return;
    bufferedTableItem.doPaint(gc);
  }

  public void locationChanged() {
    if (bufferedTableItem == null)
      return;
    bufferedTableItem.locationChanged();
  }

  public TableRowCore getTableRowCore() {
    return tableRow;
  }
  
  /* Comparable Implementation */
  
  /** Compare our sortValue to the specified object.  Assumes the object 
   * is TableCellImp (safe assumption)
   */
  public int compareTo(Object o) {
    try {
      Comparable ourSortValue = getSortValue();
      Comparable otherSortValue = ((TableCellImpl)o).getSortValue();
      if (ourSortValue instanceof String && otherSortValue instanceof String) {
        // Collator.getInstance cache's Collator object, so this is relatively
        // fast.  However, storing it as static somewhere might give a small
        // performance boost.  If such an approach is take, ensure that the static
        // variable is updated the user chooses an different language.
        Collator collator = Collator.getInstance(Locale.getDefault());
        return collator.compare(ourSortValue, otherSortValue);
      }
      try {
        return ourSortValue.compareTo(otherSortValue);
      } catch (ClassCastException e) {
        // It's possible that a row was created, but not refreshed yet.
        // In that case, one sortValue will be String, and the other will be
        // a comparable object that the plugin defined.  Those two sortValues 
        // may not be compatable (for good reason!), so just skip it.
      }
    } catch (Exception e) {
      System.out.println("Could not compare cells");
      e.printStackTrace();
    }
    return 0;
  }

  public void setToolTip(Object tooltip) {
    oToolTip = tooltip;
  }

  public Object getToolTip() {
    return oToolTip;
  }

  public void invokeToolTipListeners(int type) {
    tableColumn.invokeCellToolTipListeners(this, type);

    if (tooltipListeners == null)
      return;
    if (type == TOOLTIPLISTENER_HOVER) {
      for (int i = 0; i < tooltipListeners.size(); i++)
        ((TableCellToolTipListener)(tooltipListeners.get(i))).cellHover(this);
    } else {
      for (int i = 0; i < tooltipListeners.size(); i++)
        ((TableCellToolTipListener)(tooltipListeners.get(i))).cellHoverComplete(this);
    }
  }
}
