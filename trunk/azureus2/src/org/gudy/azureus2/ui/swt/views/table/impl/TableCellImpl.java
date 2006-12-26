/*
 * File    : TableCellImpl.java
 * Created : 24 nov. 2003
 * By      : Olivier
 * Originally PluginItem.java, and changed to be more generic.
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

import java.text.Collator;
import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.*;
import org.gudy.azureus2.ui.swt.debug.ObfusticateCellText;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTGraphicImpl;
import org.gudy.azureus2.ui.swt.views.table.TableCellCore;
import org.gudy.azureus2.ui.swt.views.table.TableColumnCore;
import org.gudy.azureus2.ui.swt.views.table.TableRowCore;

import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.UIRuntimeException;
import org.gudy.azureus2.plugins.ui.SWT.GraphicSWT;
import org.gudy.azureus2.plugins.ui.tables.*;


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
	private ArrayList cellMouseListeners;
	private ArrayList cellVisibilityListeners;
  private TableColumnCore tableColumn;
  private boolean valid;
  private int refreshErrLoopCount;
  private int tooltipErrLoopCount;
  private int loopFactor;
  private Object oToolTip;
	private int iCursorID = -1;
	private Graphic graphic = null;
  
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

  private static final String CFG_PAINT = "GUI_SWT_bAlternateTablePainting";
  private static boolean bAlternateTablePainting;

  static {
  	COConfigurationManager.addAndFireParameterListener(CFG_PAINT,
				new ParameterListener() {
					public void parameterChanged(String parameterName) {
						bAlternateTablePainting = COConfigurationManager
								.getBooleanParameter(CFG_PAINT);
					}
				});
  }

  public TableCellImpl(TableRowCore _tableRow, TableColumnCore _tableColumn,
      int position, BufferedTableItem item) {
    this.tableColumn = _tableColumn;
    this.tableRow = _tableRow;
    valid = false;
    refreshErrLoopCount = 0;
    tooltipErrLoopCount = 0;
    loopFactor = 0;

    bufferedTableItem = item;

    Utils.execSWTThread(new AERunnable() {
    	public void runSupport() {
        tableColumn.invokeCellAddedListeners(TableCellImpl.this);
    	}
    });
  }

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

    createBufferedTableItem(position);
    
    Utils.execSWTThread(new AERunnable() {
    	public void runSupport() {
        tableColumn.invokeCellAddedListeners(TableCellImpl.this);
    	}
    });
    //bDebug = (position == 1) && tableColumn.getTableID().equalsIgnoreCase("Peers");
  }
  
  private void createBufferedTableItem(int position) {
    BufferedTableRow bufRow = (BufferedTableRow)tableRow;
    if (tableColumn.getType() == TableColumnCore.TYPE_GRAPHIC) {
    	if (bAlternateTablePainting) {
	      bufferedTableItem = new BufferedGraphicTableItem2(bufRow, position) {
	        public void refresh() {
	          TableCellImpl.this.refresh();
	        }
	        public void invalidate() {
	          TableCellImpl.this.valid = false;
	        }
	      };
	    } else {
	      bufferedTableItem = new BufferedGraphicTableItem1(bufRow, position) {
	        public void refresh() {
	          TableCellImpl.this.refresh();
	        }
	        public void invalidate() {
	          TableCellImpl.this.valid = false;
	        }
	      };
	    }
    	setOrientationViaColumn();
    } else {
      bufferedTableItem = new BufferedTableItemImpl(bufRow, position) {
        public void refresh() {
          TableCellImpl.this.refresh();
        }
        public void invalidate() {
          TableCellImpl.this.valid = false;
        }
      };
    }
  }

  private void pluginError(Throwable e) {
    String sTitleLanguageKey = tableColumn.getTitleLanguageKey();

    String sPosition = (bufferedTableItem == null) 
      ? "null" 
      : "" + bufferedTableItem.getPosition() + 
        " (" + MessageText.getString(sTitleLanguageKey) + ")";
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
		// if we've been disposed then row/col are null
	  
	TableRowCore	row = tableRow;
	TableColumnCore	col	= tableColumn;
	
	if ( row == null || col == null){
		return( null );
	}
	
    return row.getDataSource(col.getUseCoreDataSource());
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
  
  public Color getForegroundSWT() {
  	checkCellForSetting();

    return bufferedTableItem.getForeground();
  }
  
  // @see org.gudy.azureus2.plugins.ui.tables.TableCell#getForeground()
  public int[] getForeground() {
		Color color = bufferedTableItem.getForeground();

		if (color == null) {
			return new int[3];
		}

		return new int[] { color.getRed(), color.getGreen(), color.getBlue()
		};
	}
  
  public boolean setForeground(Color color) {
  	checkCellForSetting();

  	// Don't need to set when not visible
  	if (isInvisibleAndCanRefresh())
  		return false;

    return bufferedTableItem.setForeground(color);
  }
  
  public boolean setForeground(int red, int green, int blue) {
  	checkCellForSetting();

  	// Don't need to set when not visible
  	if (isInvisibleAndCanRefresh())
  		return false;

    return bufferedTableItem.setForeground(red, green, blue);
  }

  public boolean setText(String text) {
  	checkCellForSetting();
  	if (text == null)
  		text = "";
  	boolean bChanged = false;

  	if (bSortValueIsText && !text.equals(sortValue)) {
  		bChanged = true;
  		sortValue = text;
    	tableColumn.setLastSortValueChange(SystemTime.getCurrentTime());
    	if (bDebug)
    		debug("Setting SortValue to text;");
  	}
  	

// Slower than setText(..)!
//  	if (isInvisibleAndCanRefresh()) {
//  		if (bDebug) {
//  			debug("setText ignored: invisible");
//  		}
//  		return false;
//  	}

    if (bufferedTableItem.setText(text) && !bSortValueIsText)
    	bChanged = true;

		if (bDebug) {
			debug("setText (" + bChanged + ") : " + text);
		}

  	return bChanged;
  }
  
  private boolean isInvisibleAndCanRefresh() {
  	return !isShown()
				&& (refreshListeners != null || tableColumn.hasCellRefreshListener());
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
		if (!tableColumn.isSortValueLive()) {
			// objects that can't change aren't live
			if (!(valueToSort instanceof Number) && !(valueToSort instanceof String)) {
				tableColumn.setSortValueLive(true);
			}
		}
		return _setSortValue(valueToSort);
	}

  private boolean _setSortValue(Comparable valueToSort) {
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
    
    if ((valueToSort instanceof String) && (sortValue instanceof String)
				&& sortValue.equals(valueToSort)) {
			return false;
		}

  	if (bDebug)
  		debug("Setting SortValue to "
					+ ((valueToSort == null) ? "null" : valueToSort.getClass().getName()));
  	
  	tableColumn.setLastSortValueChange(SystemTime.getCurrentTime());
    sortValue = valueToSort;

    return true;
  }
  
  public boolean setSortValue(long valueToSort) {
  	checkCellForSetting();

		if ((sortValue instanceof Long)
				&& ((Long) sortValue).longValue() == valueToSort)
			return false;

		return _setSortValue(new Long(valueToSort));
  }
  
  public boolean setSortValue( float valueToSort ) {
  	checkCellForSetting();

		if (sortValue instanceof Float
				&& ((Float) sortValue).floatValue() == valueToSort)
			return false;

		return _setSortValue(new Float(valueToSort));
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
  	Point pt = null;
  	
    if (bufferedTableItem instanceof BufferedGraphicTableItem) {
    	pt = ((BufferedGraphicTableItem)bufferedTableItem).getSize();
    } else {
    	Rectangle bounds = bufferedTableItem.getBounds();
    	if (bounds != null) {
    		pt = new Point(bounds.width, bounds.height);
    	}
    }
    if (pt == null)
      return -1;
    return pt.x;
  }

  public int getHeight() {
  	Point pt = null;
  	
    if (bufferedTableItem instanceof BufferedGraphicTableItem) {
    	pt = ((BufferedGraphicTableItem)bufferedTableItem).getSize();
    } else {
    	Rectangle bounds = bufferedTableItem.getBounds();
    	if (bounds != null) {
    		pt = new Point(bounds.width, bounds.height);
    	}
    }
    if (pt == null)
      return -1;
    return pt.y;
  }

  public boolean setGraphic(Image img) {
  	checkCellForSetting();

    if (!(bufferedTableItem instanceof BufferedGraphicTableItem))
      return false;

    graphic = null;
    return ((BufferedGraphicTableItem)bufferedTableItem).setGraphic(img);
  }

  public boolean setGraphic(Graphic img) {
  	if (img != null){
  		checkCellForSetting();
  	}

    if (!(bufferedTableItem instanceof BufferedGraphicTableItem))
      return false;

    graphic = img;

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
  	if (graphic != null) {
  		return graphic;
  	}

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
    
    if (bFillCell)
    	((BufferedGraphicTableItem)bufferedTableItem).setOrientation(SWT.FILL);
    else
    	setOrientationViaColumn();
  }

	public void setMarginHeight(int height) {
  	checkCellForSetting();

    if (!(bufferedTableItem instanceof BufferedGraphicTableItem))
      return;
    ((BufferedGraphicTableItem)bufferedTableItem).setMargin(-1, height);
  }

  public void setMarginWidth(int width) {
  	checkCellForSetting();

    if (!(bufferedTableItem instanceof BufferedGraphicTableItem))
      return;
    ((BufferedGraphicTableItem)bufferedTableItem).setMargin(width, -1);
  }

  /* End TYPE_GRAPHIC Functions */

  public void addRefreshListener(TableCellRefreshListener listener) {
  	try{
  		this_mon.enter();
  	
  		if (refreshListeners == null)
  			refreshListeners = new ArrayList(1);

  		if (bDebug) {
  			debug("addRefreshListener; count=" + refreshListeners.size());
  		}
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
	      disposeListeners = new ArrayList(1);
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
  			tooltipListeners = new ArrayList(1);
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
  
  
	public void addMouseListener(TableCellMouseListener listener) {
		try {
			this_mon.enter();

			if (cellMouseListeners == null)
				cellMouseListeners = new ArrayList(1);

			cellMouseListeners.add(listener);

		} finally {
			this_mon.exit();
		}
	}

	public void removeMouseListener(TableCellMouseListener listener) {
		try {
			this_mon.enter();

			if (cellMouseListeners == null)
				return;

			cellMouseListeners.remove(listener);

		} finally {
			this_mon.exit();
		}
	}


	public void addVisibilityListener(TableCellVisibilityListener listener) {
		try {
			this_mon.enter();

			if (cellVisibilityListeners == null)
				cellVisibilityListeners = new ArrayList(1);

			cellVisibilityListeners.add(listener);

		} finally {
			this_mon.exit();
		}
	}

	public void removeVisibilityListener(TableCellVisibilityListener listener) {
		try {
			this_mon.enter();

			if (cellVisibilityListeners == null)
				return;

			cellVisibilityListeners.remove(listener);

		} finally {
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

		if (listenerObject instanceof TableCellMouseListener)
			addMouseListener((TableCellMouseListener)listenerObject);

		if (listenerObject instanceof TableCellVisibilityListener)
			addVisibilityListener((TableCellVisibilityListener)listenerObject);
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
  		debug("Invalidate Cell;" + bMustRefresh);

  	if (bMustRefresh)
  		this.bMustRefresh = true;
  }

  public void refresh() {
    refresh(true);
  }
  
  public void refresh(boolean bDoGraphics) {
  	refresh(bDoGraphics, isShown());
  }

  public void refresh(boolean bDoGraphics, boolean bRowVisible) {
  	refresh(bDoGraphics, bRowVisible, isShown());
  }

  private boolean bInRefresh = false;
  public void refresh(boolean bDoGraphics, boolean bRowVisible,
			boolean bCellVisible)
	{
    if (refreshErrLoopCount > 2)
      return;
    int iErrCount = tableColumn.getConsecutiveErrCount();
    if (iErrCount > 10) {
    	refreshErrLoopCount = 3;
      return;
    }
    
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
    if (bCellVisible && !bIsUpToDate) {
    	if (bDebug)
    		debug("Setting Invalid because visible & not up to date");
    	valid = false;
    	bIsUpToDate = true;
    } else if (!bCellVisible && bIsUpToDate) {
    	bIsUpToDate = false;
    }

    try {
    	if (bDebug) {
    		debug("Cell Valid?" + valid + "; Visible?" + tableRow.isVisible() + "/" + bufferedTableItem.isShown());
    	}
      int iInterval = tableColumn.getRefreshInterval();
    	if (iInterval == TableColumnCore.INTERVAL_INVALID_ONLY && !valid
    			&& !bMustRefresh && bSortValueIsText && sortValue != null
					&& tableColumn.getType() == TableColumnCore.TYPE_TEXT_ONLY) {
    		if (bCellVisible) {
	      	if (bDebug)
	      		debug("fast refresh: setText");
	    		setText((String)sortValue);
	    		valid = true;
    		}
    	} else if ((iInterval == TableColumnCore.INTERVAL_LIVE ||
          (iInterval == TableColumnCore.INTERVAL_GRAPHIC && bDoGraphics) ||
          (iInterval > 0 && (loopFactor % iInterval) == 0) ||
          !valid || bMustRefresh)) 
      {
      	boolean bWasValid = isValid();

      	if (bDebug)
      		debug("invoke refresh");

      	long lTimeStart = SystemTime.getCurrentTime();
        tableColumn.invokeCellRefreshListeners(this);
        if (refreshListeners != null) {
          for (int i = 0; i < refreshListeners.size(); i++) {
            ((TableCellRefreshListener)(refreshListeners.get(i))).refresh(this);
          }
        }
      	long lTimeEnd = SystemTime.getCurrentTime();
      	tableColumn.addRefreshTime(lTimeEnd - lTimeStart);

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

    tableColumn.invokeCellDisposeListeners(this);

    if (disposeListeners != null) {
      try {
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
  
  public void setIcon(Image img) {
  	if (isInvisibleAndCanRefresh())
  		return;

    bufferedTableItem.setIcon(img);
    graphic = null;
  }
  
  public Image getIcon() {
  	return bufferedTableItem.getIcon();
  }

  public boolean needsPainting() {
    return bufferedTableItem.needsPainting();
  }
  
  public void doPaint(GC gc) {
  	if ((!bIsUpToDate || !valid)
				&& (refreshListeners != null || tableColumn.hasCellRefreshListener())) {
  		if (bDebug) {
  			debug("doPaint: invoke refresh");
  		}
  		refresh();
  	}

		if (bDebug) {
			debug("doPaint " + bIsUpToDate + ";" + valid + ";" + refreshListeners);
		}
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

  public void invokeMouseListeners(TableCellMouseEvent event) {
		if (cellMouseListeners == null)
			return;

		for (int i = 0; i < cellMouseListeners.size(); i++) {
			try {
				TableCellMouseListener l = (TableCellMouseListener) (cellMouseListeners
						.get(i));

				l.cellMouseTrigger(event);

			} catch (Throwable e) {
				Debug.printStackTrace(e);
			}
		}
	}

  public void invokeVisibilityListeners(int visibility) {
		if (cellVisibilityListeners == null)
			return;

		for (int i = 0; i < cellVisibilityListeners.size(); i++) {
			try {
				TableCellVisibilityListener l = (TableCellVisibilityListener) (cellVisibilityListeners.get(i));

				l.cellVisibilityChanged(this, visibility);

			} catch (Throwable e) {
				Debug.printStackTrace(e);
			}
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
	
	public boolean isUpToDate() {
		return bIsUpToDate;
	}
	
	private void debug(final String s) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				System.out.println(SystemTime.getCurrentTime() + ": r"
						+ tableRow.getIndex() + "c" + tableColumn.getPosition()
						+ "r.v?" + ((tableRow.isVisible() ? "Y":"N"))
						+ ";" + s);
			}
		}, true);
	}

	public Rectangle getBounds() {
		Rectangle bounds = bufferedTableItem.getBounds();
		if (bounds == null) {
      return new Rectangle(0,0,0,0);
		}
    return bounds;
	}

	private void setOrientationViaColumn() {
		if (!(bufferedTableItem instanceof BufferedGraphicTableItem))
			return;
		
		BufferedGraphicTableItem ti = (BufferedGraphicTableItem) bufferedTableItem;

		int align = tableColumn.getAlignment();
    if (align == TableColumn.ALIGN_CENTER)
    	ti.setOrientation(SWT.CENTER); 
    else if (align == TableColumn.ALIGN_LEAD)
    	ti.setOrientation(SWT.LEFT); 
    else if (align == TableColumn.ALIGN_TRAIL)
    	ti.setOrientation(SWT.RIGHT); 
	}

	public String getObfusticatedText() {
		if (tableColumn.isObfusticated()) {
			if (tableColumn instanceof ObfusticateCellText) {
				return ((ObfusticateCellText)tableColumn).getObfusticatedText(this);
			}
			
			return "";
		}
		return null;
	}

	public Image getBackgroundImage() {
  	return bufferedTableItem.getBackgroundImage();
	}
	
	public BufferedTableItem getBufferedTableItem() {
		return bufferedTableItem;
	}

	public int getCursorID() {
		return iCursorID;
	}
	
	public void setCursorID(int cursorID) {
		iCursorID = cursorID;
	}
}
