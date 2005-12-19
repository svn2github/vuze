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
import java.util.Comparator;
import java.util.Iterator;
import java.util.Locale;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.UIRuntimeException;
import org.gudy.azureus2.plugins.ui.SWT.GraphicSWT;
import org.gudy.azureus2.plugins.ui.tables.TableCellDisposeListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellToolTipListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.BufferedGraphicTableItem;
import org.gudy.azureus2.ui.swt.components.BufferedGraphicTableItem1;
import org.gudy.azureus2.ui.swt.components.BufferedGraphicTableItem2;
import org.gudy.azureus2.ui.swt.components.BufferedTableItem;
import org.gudy.azureus2.ui.swt.components.BufferedTableRow;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTGraphicImpl;
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
	private static final LogIDs LOGID = LogIDs.GUI;
  private TableRowCore tableRow;
  private Comparable sortValue;
  private boolean bSortValueIsText = true;
  private BufferedTableItem bufferedTableItem;
  private ArrayList refreshListeners;
  private ArrayList disposeListeners;
  private ArrayList tooltipListeners;
  private TableColumnCore tableColumn;
  private boolean valid;
  private int refreshErrLoopCount;
  private int tooltipErrLoopCount;
  private int loopFactor;
  private Object oToolTip;
  /**
   * For refreshing, this flag manages whether the row is actually up to date.
   * 
   * We don't update any visuals while the row isn't visible.  But, validility
   * does get set to true so that the cell isn't forced to refresh every
   * cycle when not visible.  (We can't just never call refresh when the row
   * is not visible, as refresh also sets the sort value)
   *  
   * When the row does become visible, we have to invalidate the row so
   * that the row will set its visuals again (this time, actually
   * updating a viewable object).
   */
	private boolean bIsUpToDate = true;
	
	private boolean bDisposed = false;
	
	private boolean bMustRefresh = false;
	
	public boolean bDebug = false;
  
  private AEMonitor 	this_mon 	= new AEMonitor( "TableCell" );

  /**
   * Initialize
   *  
   * @param _tableRow
   * @param _tableColumn
   * @param position
   */
  public TableCellImpl(TableRowCore _tableRow, TableColumnCore _tableColumn,
                       int position) {
    this.tableColumn = _tableColumn;
    this.tableRow = _tableRow;
    valid = false;
    refreshErrLoopCount = 0;
    tooltipErrLoopCount = 0;
    loopFactor = 0;
    if (tableColumn.getType() != TableColumnCore.TYPE_GRAPHIC) {
      bufferedTableItem = new BufferedTableItem((BufferedTableRow)tableRow, position) {
        public void refresh() {
          TableCellImpl.this.refresh();
        }
        public void invalidate() {
          TableCellImpl.this.valid = false;
        }
      };
    } else if (COConfigurationManager.getBooleanParameter("GUI_SWT_bAlternateTablePainting")) {
      bufferedTableItem = new BufferedGraphicTableItem2((BufferedTableRow)tableRow, position) {
        public void refresh() {
          TableCellImpl.this.refresh();
        }
        public void invalidate() {
          TableCellImpl.this.valid = false;
        }
      };
    } else {
      bufferedTableItem = new BufferedGraphicTableItem1((BufferedTableRow)tableRow, position) {
        public void refresh() {
          TableCellImpl.this.refresh();
        }
        public void invalidate() {
          TableCellImpl.this.valid = false;
        }
      };
    }
    tableColumn.invokeCellAddedListeners(this);
    
    //bDebug = (position == 1) && tableColumn.getTableID().equalsIgnoreCase("Peers");
  }

  private void pluginError(Throwable e) {
    String sPosition = (bufferedTableItem == null) 
      ? "null" 
      : "" + bufferedTableItem.getPosition() + 
        " (" + bufferedTableItem.getColumnName() + ")";
    Logger.log(new LogEvent(LOGID, "Table Cell Plugin for Column #" + sPosition
				+ " generated an exception ", e));
  }
  
  private void checkCellForSetting() {
  	if (isDisposed())
  		throw new UIRuntimeException("Table Cell is disposed.");
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
  	checkCellForSetting();

  	// Don't need to set when not visible
  	if (!tableRow.isVisible())
  		return false;

    return bufferedTableItem.setItemForeground(color);
  }
  
  public boolean setForeground(int red, int green, int blue) {
  	checkCellForSetting();

  	// Don't need to set when not visible
  	if (!tableRow.isVisible())
  		return false;

    return bufferedTableItem.setItemForeground(red, green, blue);
  }

  public boolean setText(String text) {
  	checkCellForSetting();
  	if (text == null)
  		text = "";
  	boolean bChanged = false;

  	if (bSortValueIsText && !text.equals(sortValue)) {
  		bChanged = true;
  		sortValue = text;
    	if (bDebug)
    		debug("Setting SortValue to text;");
  	}
  	
  	if (!tableRow.isVisible())
  		return false;
  	
    if (bufferedTableItem.setText(text) && !bSortValueIsText)
    	bChanged = true;

  	return bChanged;
  }
  
  public String getText() {
  	if (bSortValueIsText && sortValue instanceof String)
  		return (String)sortValue;
    return bufferedTableItem.getText();
  }

  public boolean isShown() {
    return bufferedTableItem.isShown();
  }
  
  public boolean setSortValue(Comparable valueToSort) {
  	checkCellForSetting();

    if (sortValue == valueToSort)
      return false;

    if (bSortValueIsText) {
      bSortValueIsText = false;
    	if (sortValue instanceof String)
	    	// Make sure text is actually in the cell (it may not have been if
	      // cell wasn't created at the time of setting)
	      setText((String)sortValue);
    }

  	if (bDebug)
  		debug("Setting SortValue to "
					+ ((valueToSort == null) ? "null" : valueToSort.getClass().getName()));
    sortValue = valueToSort;

    return true;
  }
  
  public boolean setSortValue(long valueToSort) {
  	checkCellForSetting();

		if ((sortValue instanceof Long)
				&& ((Long) sortValue).longValue() == valueToSort)
			return false;

		return setSortValue(new Long(valueToSort));
  }
  
  public boolean setSortValue( float valueToSort ) {
  	checkCellForSetting();

		if (sortValue instanceof Float
				&& ((Float) sortValue).floatValue() == valueToSort)
			return false;

		return setSortValue(new Float(valueToSort));
  }

  public Comparable getSortValue() {
  	if (bDebug)
			debug("GetSortValue;"
					+ (sortValue == null ? "null" : sortValue.getClass().getName() + ";"
							+ sortValue.toString()));

    if (sortValue == null) {
      if (bufferedTableItem != null)
        return bufferedTableItem.getText();
      return "";
    }
    return sortValue;
  }
  
  public void setToolTip(Object tooltip) {
    oToolTip = tooltip;
  }

  public Object getToolTip() {
    return oToolTip;
  }

	public boolean isDisposed() {
		return bDisposed;
	}
  
  /* Start TYPE_GRAPHIC Functions */

	public Point getSize() {
    if (!(bufferedTableItem instanceof BufferedGraphicTableItem))
      return null;
    return ((BufferedGraphicTableItem)bufferedTableItem).getSize();
  }

  public int getWidth() {
    if (!(bufferedTableItem instanceof BufferedGraphicTableItem))
      return -1;
    Point pt = ((BufferedGraphicTableItem)bufferedTableItem).getSize();
    if (pt == null)
      return -1;
    return pt.x;
  }

  public int getHeight() {
    if (!(bufferedTableItem instanceof BufferedGraphicTableItem))
      return -1;
    Point pt = ((BufferedGraphicTableItem)bufferedTableItem).getSize();
    if (pt == null)
      return -1;
    return pt.y;
  }

  public boolean setGraphic(Image img) {
  	checkCellForSetting();

    if (!(bufferedTableItem instanceof BufferedGraphicTableItem))
      return false;

    return ((BufferedGraphicTableItem)bufferedTableItem).setGraphic(img);
  }

  public boolean setGraphic(Graphic img) {
  	checkCellForSetting();

    if (!(bufferedTableItem instanceof BufferedGraphicTableItem))
      return false;

    if (img == null)
      return ((BufferedGraphicTableItem)bufferedTableItem).setGraphic(null);

    if (img instanceof GraphicSWT){
    	Image imgSWT = ((GraphicSWT)img).getImage();
    	return ((BufferedGraphicTableItem)bufferedTableItem).setGraphic(imgSWT);
    }
    
    if (img instanceof UISWTGraphic){
    	Image imgSWT = ((UISWTGraphic)img).getImage();
    	return ((BufferedGraphicTableItem)bufferedTableItem).setGraphic(imgSWT);
    }
    
    return( false );
  }

  public Graphic getGraphic() {
    if (!(bufferedTableItem instanceof BufferedGraphicTableItem))
      return null;
    Image img = ((BufferedGraphicTableItem)bufferedTableItem).getGraphic();
    return new UISWTGraphicImpl(img);
  }

  public Image getGraphicSWT() {
    if (!(bufferedTableItem instanceof BufferedGraphicTableItem))
      return null;
    return ((BufferedGraphicTableItem)bufferedTableItem).getGraphic();
  }

  public void setFillCell(boolean bFillCell) {
  	checkCellForSetting();

    if (!(bufferedTableItem instanceof BufferedGraphicTableItem))
      return;
    ((BufferedGraphicTableItem)bufferedTableItem).fillCell = bFillCell;
  }

  public void setMarginHeight(int height) {
  	checkCellForSetting();

    if (!(bufferedTableItem instanceof BufferedGraphicTableItem))
      return;
    ((BufferedGraphicTableItem)bufferedTableItem).marginHeight = height;
  }

  public void setMarginWidth(int width) {
  	checkCellForSetting();

    if (!(bufferedTableItem instanceof BufferedGraphicTableItem))
      return;
    ((BufferedGraphicTableItem)bufferedTableItem).marginWidth = width;
  }

  /* End TYPE_GRAPHIC Functions */

  public void addRefreshListener(TableCellRefreshListener listener) {
  	try{
  		this_mon.enter();
  	
  		if (refreshListeners == null)
  			refreshListeners = new ArrayList();

  		refreshListeners.add(listener);
  		
  	}finally{
  		this_mon.exit();
  	}
  }

  public void removeRefreshListener(TableCellRefreshListener listener) {
  	try{
  		this_mon.enter();
  
	    if (refreshListeners == null)
	      return;
	
	    refreshListeners.remove(listener);
  	}finally{
  		
  		this_mon.exit();
  	}
  }

  public void addDisposeListener(TableCellDisposeListener listener) {
  	try{
  		this_mon.enter();
  
	    if (disposeListeners == null) {
	      disposeListeners = new ArrayList();
	    }
	    disposeListeners.add(listener);
  	}finally{
  		
  		this_mon.exit();
  	}
  }

  public void removeDisposeListener(TableCellDisposeListener listener) {
  	try{
  		this_mon.enter();
  
  		if (disposeListeners == null)
  			return;

  		disposeListeners.remove(listener);
  		
  	}finally{
  		
  		this_mon.exit();
  	}
  }
  
  public void addToolTipListener(TableCellToolTipListener listener) {
  	try{
  		this_mon.enter();
  
  		if (tooltipListeners == null) {
  			tooltipListeners = new ArrayList();
  		}
  		tooltipListeners.add(listener);
  		
  	}finally{
  		this_mon.exit();
  	}
  }

  public void removeToolTipListener(TableCellToolTipListener listener) {
  	try{
  		this_mon.enter();
  	
  		if (tooltipListeners == null)
  			return;

  		tooltipListeners.remove(listener);
  	}finally{
  		
  		this_mon.exit();
  	}
  }
  
  
	public void addListeners(Object listenerObject) {
		if (listenerObject instanceof TableCellDisposeListener)
			addDisposeListener((TableCellDisposeListener)listenerObject);

		if (listenerObject instanceof TableCellRefreshListener)
			addRefreshListener((TableCellRefreshListener)listenerObject);

		if (listenerObject instanceof TableCellToolTipListener)
			addToolTipListener((TableCellToolTipListener)listenerObject);
	}

	/**
	 * If a plugin in trying to invalidate a cell, then clear the sort value
	 * too.
	 */
	public void invalidate() {
  	checkCellForSetting();

  	invalidate(true);
	}

	/* Start of Core-Only function */
  //////////////////////////////////
  public void invalidate(final boolean bMustRefresh) {
  	valid = false;

  	if (bDebug)
  		debug("Invalidate Cell;" + bMustRefresh + "; Visible?" + tableRow.isVisible());

  	if (bMustRefresh)
  		this.bMustRefresh = true;
  }

  public void refresh() {
    refresh(true);
  }
  
  public void refresh(boolean bDoGraphics) {
  	refresh(bDoGraphics, tableRow.isVisible());
  }

  private boolean bInRefresh = false;
  public void refresh(boolean bDoGraphics, boolean bRowVisible) {
    if (refreshErrLoopCount > 2)
      return;
    int iErrCount = tableColumn.getConsecutiveErrCount();
    if (iErrCount > 10)
      return;
    
    if (bInRefresh) {
    	// Skip a Refresh call when being called from within refresh.
    	// This could happen on virtual tables where SetData calls us again, or
    	// if we ever introduce plugins to refresh.
    	if (bDebug)
    		debug("Calling Refresh from Refresh :) Skipping.");
    	return;
    }
  	bInRefresh = true;

    // See bIsUpToDate variable comments
    if (bRowVisible && !bIsUpToDate) {
    	if (bDebug)
    		debug("Setting Invalid because visible & not up to date");
    	valid = false;
    	bIsUpToDate = true;
    }

    try {
    	if (bDebug)
    		debug("Cell Valid?" + valid + "; Visible?" + tableRow.isVisible());
      int iInterval = tableColumn.getRefreshInterval();
    	if (iInterval == TableColumnCore.INTERVAL_INVALID_ONLY && !valid
    			&& !bMustRefresh && bSortValueIsText && sortValue != null
					&& tableColumn.getType() == TableColumnCore.TYPE_TEXT_ONLY) {
    		setText((String)sortValue);
    		valid = true;
    	} else if ((iInterval == TableColumnCore.INTERVAL_LIVE ||
          (iInterval == TableColumnCore.INTERVAL_GRAPHIC && bDoGraphics) ||
          (iInterval > 0 && (loopFactor % iInterval) == 0) ||
          !valid || bMustRefresh) && bufferedTableItem.isShown()) 
      {
      	boolean bWasValid = isValid();

        tableColumn.invokeCellRefreshListeners(this);
        if (refreshListeners != null)
          for (int i = 0; i < refreshListeners.size(); i++)
            ((TableCellRefreshListener)(refreshListeners.get(i))).refresh(this);

        // Change to valid only if we weren't valid before the listener calls
        // This is in case the listeners set valid to false when it was true
        if (!bWasValid) 
        	valid = true;
        
        if (bMustRefresh)
        	bMustRefresh = false;
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
      	Logger.log(new LogEvent(LOGID, LogEvent.LT_ERROR,
						"TableCell will not be refreshed anymore this session."));
    } finally {
    	bInRefresh = false;
    }
  }


  public void dispose() {
  	bDisposed = true;

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
  	if (!tableRow.isVisible())
  		return;

    bufferedTableItem.setImage(img);
  }

  public boolean needsPainting() {
    return bufferedTableItem.needsPainting();
  }
  
  public void doPaint(GC gc) {
    bufferedTableItem.doPaint(gc);
  }

  public void locationChanged() {
    bufferedTableItem.locationChanged();
  }

  public TableRowCore getTableRowCore() {
    return tableRow;
  }
  
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getText();
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
      Debug.printStackTrace( e );
    }
    return 0;
  }

  public void invokeToolTipListeners(int type) {
  	if (tableColumn == null)
  		return;

    tableColumn.invokeCellToolTipListeners(this, type);

    if (tooltipListeners == null || tooltipErrLoopCount > 2)
      return;

    int iErrCount = tableColumn.getConsecutiveErrCount();
    if (iErrCount > 10)
      return;

    try {
	    if (type == TOOLTIPLISTENER_HOVER) {
	      for (int i = 0; i < tooltipListeners.size(); i++)
	        ((TableCellToolTipListener)(tooltipListeners.get(i))).cellHover(this);
	    } else {
	      for (int i = 0; i < tooltipListeners.size(); i++)
	        ((TableCellToolTipListener)(tooltipListeners.get(i))).cellHoverComplete(this);
	    }
	    tooltipErrLoopCount = 0;
    } catch (Throwable e) {
      tooltipErrLoopCount++;
      tableColumn.setConsecutiveErrCount(++iErrCount);
      pluginError(e);
      if (tooltipErrLoopCount > 2)
      	Logger.log(new LogEvent(LOGID, LogEvent.LT_ERROR,
						"TableCell's tooltip will not be refreshed anymore this session."));
    }
  }
  
  public static final Comparator TEXT_COMPARATOR = new TextComparator();
  private static class TextComparator implements Comparator {
		public int compare(Object arg0, Object arg1) {
			return arg0.toString().compareToIgnoreCase(arg1.toString());
		}
  }
  
	public void setUpToDate(boolean upToDate) {
		bIsUpToDate = upToDate;
	}
	
	private void debug(final String s) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				System.out.println("r" + tableRow.getIndex() + "; " + s);
			}
		}, true);
	}
}
