/*
 * File    : TableRowImpl.java
 * Originally TorrentRow.java, and changed to be more generic by TuxPaper
 *
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
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
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.views.table.impl;

import java.util.*;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Table;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.BufferedTableRow;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.views.table.*;

import com.aelitis.azureus.ui.common.table.*;

import org.gudy.azureus2.plugins.ui.tables.*;

import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;



/** Represents an entire row in a table.  Stores each cell belonging to the
 * row and handles refreshing them.
 *
 * @see TableCellImpl
 * 
 * @author TuxPaper
 *            2005/Oct/07: Moved TableItem.SetData("TableRow", ..) to 
 *                         BufferedTableRow
 *            2005/Oct/07: Removed all calls to BufferedTableRoe.getItem()
 */
public class TableRowImpl 
       extends BufferedTableRow 
       implements TableRowSWT
{
  /** List of cells in this column.  They are not stored in display order */
  private Map mTableCells;
  private String sTableID;
  
  private Object coreDataSource;
  private Object pluginDataSource;
  private boolean bDisposed;
  private boolean bSetNotUpToDateLastRefresh = false;
	private TableView tableView;

  private static AEMonitor this_mon = new AEMonitor( "TableRowImpl" );
	private ArrayList mouseListeners;
	private boolean wasShown = false;
	private Map<String,Object> dataList;
	
	private int lastIndex = -1;
	private int fontStyle;
	private int alpha = 255;

  // XXX add rowVisuallyupdated bool like in ListRow

  /**
   * Default constructor
   * 
   * @param table
   * @param sTableID
   * @param columnsSorted
   * @param dataSource
   * @param bSkipFirstColumn
   */
  public TableRowImpl(TableView tv, TableOrTreeSWT table, String sTableID,
			TableColumnCore[] columnsSorted, Object dataSource,
			boolean bSkipFirstColumn) {
		super(table);
		this.tableView = tv;
    this.sTableID = sTableID;
    coreDataSource = dataSource;
    mTableCells = new LightHashMap();
    bDisposed = false;

    // create all the cells for the column
    for (int i = 0; i < columnsSorted.length; i++) {
    	if (columnsSorted[i] == null)
    		continue;
      //System.out.println(dataSource + ": " + tableColumns[i].getName() + ": " + tableColumns[i].getPosition());
    	TableCellImpl cell = new TableCellImpl(TableRowImpl.this, columnsSorted[i], 
          bSkipFirstColumn ? i+1 : i);
      mTableCells.put(columnsSorted[i].getName(), cell);
      //if (i == 10) cell.bDebug = true;
    }
  }

  public boolean isValid() {
  	if (bDisposed)
  		return true;

    boolean valid = true;
    Iterator iter = mTableCells.values().iterator();
    while (iter.hasNext()) {
    	TableCellSWT cell = (TableCellSWT)iter.next();
      if (cell != null)
        valid &= cell.isValid();
    }
    return valid;
  }

  /** TableRow Implementation which returns the 
   * associated plugin object for the row.  Core Column Object who wish to get 
   * core data source must re-class TableRow as TableRowCore and use
   * getDataSource(boolean)
   *
   * @see TableRowCore.getDataSource()
   */
  public Object getDataSource() {
    return getDataSource(false);
  }

  public String getTableID() {
    return sTableID;
  }
  
  public TableCell getTableCell(String field) {
  	if (bDisposed)
  		return null;
    return (TableCell)mTableCells.get(field);
  }

	public void addMouseListener(TableRowMouseListener listener) {
		try {
			this_mon.enter();

			if (mouseListeners == null)
				mouseListeners = new ArrayList(1);

			mouseListeners.add(listener);

		} finally {
			this_mon.exit();
		}
	}

	public void removeMouseListener(TableRowMouseListener listener) {
		try {
			this_mon.enter();

			if (mouseListeners == null)
				return;

			mouseListeners.remove(listener);

		} finally {
			this_mon.exit();
		}
	}

  public void invokeMouseListeners(TableRowMouseEvent event) {
		ArrayList listeners = mouseListeners;
		if (listeners == null)
			return;
		
		for (int i = 0; i < listeners.size(); i++) {
			try {
				TableRowMouseListener l = (TableRowMouseListener) (listeners.get(i));

				l.rowMouseTrigger(event);

			} catch (Throwable e) {
				Debug.printStackTrace(e);
			}
		}
	}

  /* Start Core-Only functions */
  ///////////////////////////////

  public void delete() {
		this_mon.enter();

		try {
			if (bDisposed)
				return;

			if (TableViewSWT.DEBUGADDREMOVE)
				System.out.println((table.isDisposed() ? "" : table.getData("Name"))
						+ " row delete; index=" + getIndex());

			Iterator iter = mTableCells.values().iterator();
			while (iter.hasNext()) {
				TableCellSWT item = (TableCellSWT) iter.next();
				item.dispose();
			}
			
			setForeground((Color) null);

			bDisposed = true;
		} finally {
			this_mon.exit();
		}
	}
  
  public List refresh(boolean bDoGraphics) {
    if (bDisposed) {
      return Collections.EMPTY_LIST;
    }
    
    boolean bVisible = isVisible();

    return refresh(bDoGraphics, bVisible);
  }

  public List refresh(boolean bDoGraphics, boolean bVisible) {
    // If this were called from a plugin, we'd have to refresh the sorted column
    // even if we weren't visible
    List list = Collections.EMPTY_LIST;

  	if (bDisposed) {
  		return list;
  	}

    if (!bVisible) {
    	if (!bSetNotUpToDateLastRefresh) {
    		setUpToDate(false);
    		bSetNotUpToDateLastRefresh = true;
    	}
  		return list;
  	}
    
		bSetNotUpToDateLastRefresh = false;
		
		//System.out.println(SystemTime.getCurrentTime() + "refresh " + getIndex() + ";vis=" + bVisible);
		
		((TableViewSWTImpl)tableView).invokeRefreshListeners(this);

    Iterator iter = mTableCells.values().iterator();
    while (iter.hasNext())
		{
			TableCellSWT item = (TableCellSWT) iter.next();
			TableColumn column = item.getTableColumn();
			//System.out.println(column);
			if (column != tableView.getSortColumn() && !tableView.isColumnVisible(column)) {
				//System.out.println("skip " + column);
				continue;
			}
			boolean changed = item.refresh(bDoGraphics, bVisible);
			if (changed)
			{
				if(list == Collections.EMPTY_LIST)
					list = new ArrayList(mTableCells.size());
				list.add(item);
			}
				
		}
    //System.out.println();
    return list;
  }

  public void setAlternatingBGColor(boolean bEvenIfNotVisible) {
  	super.setAlternatingBGColor(bEvenIfNotVisible);
  }

  public void locationChanged(int iStartColumn) {
    if (bDisposed || !isVisible())
      return;

  	Iterator iter = mTableCells.values().iterator();
  	while(iter.hasNext()) {
  		TableCellSWT item = (TableCellSWT)iter.next();
  		if (item.getTableColumn().getPosition() > iStartColumn)
  		  item.locationChanged();
  	}
  }

  public void doPaint(GC gc) {
  	doPaint(gc, isVisible());
  }

  // @see org.gudy.azureus2.ui.swt.views.table.TableRowSWT#doPaint(org.eclipse.swt.graphics.GC, boolean, boolean)
  public void doPaint(GC gc, boolean bVisible) {
    if (bDisposed || !bVisible)
      return;

    Iterator iter = mTableCells.values().iterator();
    while(iter.hasNext()) {
    	TableCellSWT cell = (TableCellSWT) iter.next();
    	if (cell == null) {
    		continue;
    	}
//    	if (bOnlyIfChanged && !cell.getVisuallyChangedSinceRefresh()) {
//    		continue;
//    	}
  		if (cell.needsPainting()) {
  			cell.doPaint(gc);
  		}
    }
  }

  public TableCellCore getTableCellCore(String field) {
  	if (bDisposed)
  		return null;

    return (TableCellCore)mTableCells.get(field);
  }

	/**
	 * @param name
	 * @return
	 */
	public TableCellSWT getTableCellSWT(String name) {
  	if (bDisposed)
  		return null;

    return (TableCellSWT)mTableCells.get(name);
	}
  
  public Object getDataSource(boolean bCoreObject) {
		if (bDisposed)
			return null;

		if (bCoreObject)
			return coreDataSource;

		if (pluginDataSource != null)
			return pluginDataSource;
		
		pluginDataSource = PluginCoreUtils.convert(coreDataSource, bCoreObject);

		return pluginDataSource;
	}
  
	public boolean isRowDisposed() {
		return bDisposed;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.components.BufferedTableRow#getIndex()
	 */
	public int getIndex() {
		if (bDisposed)
			return -1;

		if (lastIndex >= 0) {
			TableRowCore row = ((TableViewSWTImpl) tableView).getRowQuick(lastIndex);
			if (row == this) {
				return lastIndex;
			}
		}
		
		// don't set directly to lastIndex, so setTableItem will eventually do
		// its job
		return tableView.indexOf(this);

		//return super.getIndex();
	}
	
	public int getRealIndex() {
		return super.getIndex();
	}
	
	public boolean setTableItem(int newIndex, boolean isVisible)
	{
		if (bDisposed) {
			System.out.println("XXX setTI: bDisposed from " + Debug.getCompressedStackTrace());
			return false;
		}
		
		//if (getRealIndex() != newIndex) {
		//	((TableViewSWTImpl)tableView).debug("sTI " + newIndex + "; via " + Debug.getCompressedStackTrace(4));
		//}
		boolean changedSWTRow = super.setTableItem(newIndex, false, isVisible);
		boolean changedIndex = lastIndex != newIndex;
		if (changedIndex) {
			//System.out.println("row " + newIndex + " from " + lastIndex + ";" + tableView.isRowVisible(this) + ";" + changedSWTRow);
			lastIndex = newIndex;
		}
		boolean rowVisible = tableView.isRowVisible(this);
		setShown(rowVisible, changedSWTRow);
		if (changedSWTRow && rowVisible) {
			//invalidate();
			//refresh(true, true);
			setUpToDate(false);
		}
		return changedSWTRow; 
	}

	public boolean setTableItem(int newIndex) {
		return setTableItem(newIndex,true);
	}
	
	private static final boolean DEBUG_SET_FOREGROUND = System.getProperty("debug.setforeground") != null;
	private static void setForegroundDebug(String method_sig, Color c) {
		if (DEBUG_SET_FOREGROUND && c != null) {
			Debug.out("BufferedTableRow " + method_sig + " -> " + c);
		}
	}
	private static void setForegroundDebug(String method_sig, int r, int g, int b) {
		if (DEBUG_SET_FOREGROUND && (!(r == 0 && g == 0 && b == 0))) {
			Debug.out("BufferedTableRow " + method_sig + " -> " + r + "," + g + "," + b);
		}
	}
	
	// @see org.gudy.azureus2.ui.swt.components.BufferedTableRow#setForeground(int, int, int)
	public void setForeground(int r, int g, int b) {
		setForegroundDebug("setForeground(r, g, b)", r, g, b);
		// Don't need to set when not visible
		if (!isVisible()) {
			return;
		}

		super.setForeground(r, g, b);
	}

	// @see org.gudy.azureus2.ui.swt.components.BufferedTableRow#setForeground(org.eclipse.swt.graphics.Color)
	public void setForeground(final Color c) {
		setForegroundDebug("setForeground(Color)", c);
		// Don't need to set when not visible
		if (!isVisible())
			return;

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				TableRowImpl.this.setForegroundInSWTThread(c);
			}
		});
	}
	
	private void setForegroundInSWTThread(Color c) {
		setForegroundDebug("setForegroundInSWTThread(Color)", c);
		if (!isVisible())
			return;

		super.setForeground(c);
	}


	// @see org.gudy.azureus2.plugins.ui.tables.TableRow#setForeground(int[])
	public void setForeground(int[] rgb) {
		if (rgb == null || rgb.length < 3) {
			setForeground((Color) null);
			return;
		}
		setForeground(rgb[0], rgb[1], rgb[2]);
	}

	public void setForegroundToErrorColor() {
		this.setForeground(Colors.colorError);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.components.BufferedTableRow#invalidate()
	 */
	public void invalidate() {
		super.invalidate();
		
  	if (bDisposed)
  		return;

    Iterator iter = mTableCells.values().iterator();
    while (iter.hasNext()) {
    	TableCellSWT cell = (TableCellSWT)iter.next();
      if (cell != null)
        cell.invalidate(false);
    }
	}
	
	public void setUpToDate(boolean upToDate) {
  	if (bDisposed)
  		return;

    Iterator iter = mTableCells.values().iterator();
    while (iter.hasNext()) {
    	TableCellSWT cell = (TableCellSWT)iter.next();
      if (cell != null)
        cell.setUpToDate(upToDate);
    }
	}
	
	// @see com.aelitis.azureus.ui.common.table.TableRowCore#redraw()
	public void redraw() {
		// this will call paintItem which may call refresh
		Rectangle bounds = getBounds();
		table.redraw(bounds.x, bounds.y, bounds.width, bounds.height, false);
	}

	public String toString() {
		String result = "TableRowImpl@" + Integer.toHexString(hashCode()) + "/#"
				+ lastIndex;
		return result;
	}

	// @see com.aelitis.azureus.ui.common.table.TableRowCore#getView()
	public TableView getView() {
		return tableView;
	}

	/**
	 * @param b
	 *
	 * @since 3.0.4.3
	 */
	public void setShown(boolean b, boolean force) {
  	if (bDisposed)
  		return;
  	
  	if (b == wasShown && !force) {
  		return;
  	}
  	wasShown  = b;
  	
    Iterator iter = mTableCells.values().iterator();
    while (iter.hasNext()) {
    	TableCellSWT cell = (TableCellSWT)iter.next();
      if (cell != null) {
        cell.invokeVisibilityListeners(b
						? TableCellVisibilityListener.VISIBILITY_SHOWN
						: TableCellVisibilityListener.VISIBILITY_HIDDEN, true);
      }
    }

    /* Don't need to refresh; paintItem will trigger a refresh on
     * !cell.isUpToDate()
     *
  	if (b) {
  		refresh(b, true);
  	}
  	/**/
	}

	public boolean isMouseOver() {
		return tableView.getTableRowWithCursor() == this;
	}

	public void setData(String id, Object data) {
		synchronized (this) {
			if (dataList == null) {
				dataList = new HashMap<String, Object>(1);
			}
			if (data == null) {
				dataList.remove(id);
			} else {
				dataList.put(id, data);
			}
		}
	}
	
	public Object getData(String id) {
		synchronized (this) {
			return dataList == null ? null : dataList.get(id);
		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableRowCore#setDrawableHeight(int)
	public boolean setDrawableHeight(int height) {
		return setHeight(height);
	}
	
	// @see org.gudy.azureus2.ui.swt.views.table.TableRowSWT#getBounds()
	public Rectangle getBounds() {
		Rectangle bounds = getBounds(1);
		if (bounds == null) {
			return new Rectangle(0, 0, 0, 0);
		}
		bounds.x = 0;
		bounds.width = table.getSize().x;
		return bounds;
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableRowSWT#setFontStyle(int)
	public boolean setFontStyle(int style) {
		if (fontStyle == style) {
			return false;
		}
		
		fontStyle = style;
		invalidate();
		
		return true;
	}

	// @see com.aelitis.azureus.ui.common.table.TableRowCore#setAlpha(int)
	public boolean setAlpha(int alpha) {
		if (this.alpha == alpha) {
			return false;
		}
		
		this.alpha = alpha;
		invalidate();
		
		return true;
	}
	
	// @see org.gudy.azureus2.ui.swt.views.table.TableRowSWT#getAlpha()
	public int getAlpha() {
		return alpha;
	}
	
	// @see org.gudy.azureus2.ui.swt.views.table.TableRowSWT#getFontStyle()
	public int getFontStyle() {
		return fontStyle;
	}
	
	// @see org.gudy.azureus2.ui.swt.components.BufferedTableRow#isVisible()
	public boolean isVisible() {
		return Utils.execSWTThreadWithBool("isVisible", new AERunnableBoolean() {
			public boolean runSupport() {
				return TableRowImpl.super.isVisible();
			}
		}, 1000);
	}

	// @see org.gudy.azureus2.ui.swt.components.BufferedTableRow#setSelected(boolean)
	public void setSelected(boolean selected) {
		super.setSelected(selected);

		if (tableView instanceof TableViewSWTImpl) {
			((TableViewSWTImpl)tableView).updateSelectedRowIndexes();
		}
	}	
	
	// @see org.gudy.azureus2.ui.swt.components.BufferedTableRow#isSelected()
	public boolean isSelected() {
		return tableView.isSelected(this);
		/*
		return Utils.execSWTThreadWithBool("isSelected", new AERunnableBoolean() {
			public boolean runSupport() {
				return TableRowImpl.super.isSelected();
			}
		}, 1000);
		*/
	}
	
	// @see org.gudy.azureus2.ui.swt.components.BufferedTableRow#setSubItemCount(int)
	public void setSubItemCount(int i) {
		super.setSubItemCount(i);
	}
	
	// @see com.aelitis.azureus.ui.common.table.TableRowCore#isInPaintItem()
	public boolean isInPaintItem() {
		return super.inPaintItem();
	}
}
