/*
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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.core3.util.SystemTime;

import org.gudy.azureus2.plugins.ui.UIRuntimeException;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.pluginsimpl.local.ui.tables.TableContextMenuItemImpl;
import org.gudy.azureus2.ui.swt.views.table.TableCellCore;
import org.gudy.azureus2.ui.swt.views.table.TableColumnCore;
import org.gudy.azureus2.ui.swt.views.table.utils.TableStructureEventDispatcher;


/** Table Column definition and modification routines.
 * Implements both the plugin API and the core API.
 **/
public class TableColumnImpl
       implements TableColumnCore
{
  private String sName;
  private String sTitleLanguageKey = null;
  private int iAlignment;
  private int iType;
  private int iPosition;
  private int iWidth;
  private int iInterval;
  private long lLastSortValueChange;
  
  private String sTableID;
  private boolean bColumnAdded;
  private boolean bCoreDataSource;
  
	private ArrayList cellRefreshListeners;
	private ArrayList cellAddedListeners;
	private ArrayList cellDisposeListeners;
	private ArrayList cellToolTipListeners;
	private ArrayList cellMouseListeners;
	private int iConsecutiveErrCount;
  private ArrayList menuItems;

  private boolean bObfusticateData;
  
  protected AEMonitor 		this_mon 	= new AEMonitor( "TableColumn" );
	private boolean bSortValueLive;

	private long lStatsRefreshTotalTime;
	private long lStatsRefreshCount = 0;
	private long lStatsRefreshZeroCount = 0;


  /** Create a column object for the specified table.
   *
   * @param tableID table in which the column belongs to
   * @param columnID name/id of the column
   */
  public TableColumnImpl(String tableID, String columnID) {
    sTableID = tableID;
    sName = columnID;
    iType = TYPE_TEXT_ONLY;
    iPosition = POSITION_INVISIBLE;
    iWidth = 50;
    iAlignment = ALIGN_LEAD;
    bColumnAdded = false;
    bCoreDataSource = false;
    iInterval = INTERVAL_INVALID_ONLY;
    iConsecutiveErrCount = 0;
    lLastSortValueChange = 0;
  }

  public void initialize(int iAlignment, int iPosition, 
                         int iWidth, int iInterval) {
    if (bColumnAdded)
			throw(new UIRuntimeException("Can't set properties. Column '" + sName + " already added"));

    this.iAlignment = iAlignment;
    this.iPosition = iPosition;
    this.iWidth = iWidth;
    this.iInterval = iInterval;
  }

  public void initialize(int iAlignment,
                         int iPosition,
                         int iWidth)
  {
    if (bColumnAdded)
			throw(new UIRuntimeException("Can't set properties. Column '" + sName + " already added"));

    this.iAlignment = iAlignment;
    this.iPosition = iPosition;
    this.iWidth = iWidth;
  }

  public String getName() {
    return sName;
  }

  public String getTableID() {
    return sTableID;
  }

  public void setType(int type) {
    if (bColumnAdded)
			throw(new UIRuntimeException("Can't set properties. Column '" + sName + " already added"));

    iType = type;
  }

  public int getType() {
    return iType;
  }

  public void setWidth(int width) {
    if (width == iWidth || width < 0)
      return;
    iWidth = width;

    if (bColumnAdded && iPosition != POSITION_INVISIBLE) {
      TableStructureEventDispatcher tsed = TableStructureEventDispatcher.getInstance(sTableID);
      tsed.columnSizeChanged(this);
      if (iType == TYPE_GRAPHIC)
        invalidateCells();
    }
  }

  public int getWidth() {
    return iWidth;
  }

  public void setPosition(int position) {
    if (bColumnAdded)
			throw(new UIRuntimeException("Can't set properties. Column '" + sName + " already added"));

    iPosition = position;
  }
  
  public int getPosition() {
    return iPosition;
  }

  public void setAlignment(int alignment) {
    if (bColumnAdded)
			throw(new UIRuntimeException("Can't set properties. Column '" + sName + " already added"));

    iAlignment = alignment;
  }

  public int getAlignment() {
    return iAlignment;
  }
  
  public void addCellRefreshListener(TableCellRefreshListener listener) {
  	try{
  		this_mon.enter();
  
  		if (cellRefreshListeners == null)
  			cellRefreshListeners = new ArrayList(1);

		cellRefreshListeners.add(listener);
		//System.out.println(this + " :: addCellRefreshListener " + listener + ". " + cellRefreshListeners.size());
		
  	}finally{
  		
  		this_mon.exit();
  	}
  }

  public void removeCellRefreshListener(TableCellRefreshListener listener) {
  	try{
  		this_mon.enter();
  	
  		if (cellRefreshListeners == null)
  			return;

		cellRefreshListeners.remove(listener);
  	}finally{
  		this_mon.exit();
  	}
  }
  
  public boolean hasCellRefreshListener() {
  	return cellRefreshListeners != null && cellRefreshListeners.size() > 0;
  }

  public void setRefreshInterval(int interval) {
    iInterval = interval;
  }

  public int getRefreshInterval() {
    return iInterval;
  }

  public void addCellAddedListener(TableCellAddedListener listener) {
  	try{
  		this_mon.enter();
  	
  		if (cellAddedListeners == null)
  			cellAddedListeners = new ArrayList(1);

		cellAddedListeners.add(listener);
		
  	}finally{
  		
  		this_mon.exit();
  	}
  }

  public void removeCellAddedListener(TableCellAddedListener listener) {
  	try{
  		this_mon.enter();
  	
  		if (cellAddedListeners == null)
  			return;

		cellAddedListeners.remove(listener);
		
  	}finally{
  		
  		this_mon.exit();
  	}
  }

  public void addCellDisposeListener(TableCellDisposeListener listener) {
  	try{
  		this_mon.enter();
  	
  		if (cellDisposeListeners == null)
  			cellDisposeListeners = new ArrayList(1);

		cellDisposeListeners.add(listener);
  	}finally{
  		
  		this_mon.exit();
  	}
  }

  public void removeCellDisposeListener(TableCellDisposeListener listener) {
  	try{
  		this_mon.enter();
  	
  		if (cellDisposeListeners == null)
  			return;

		cellDisposeListeners.remove(listener);
  	}finally{
  		
  		this_mon.exit();
  	}
  }

  public void addCellToolTipListener(TableCellToolTipListener listener) {
  	try{
  		this_mon.enter();
  	
  		if (cellToolTipListeners == null)
  			cellToolTipListeners = new ArrayList(1);

		cellToolTipListeners.add(listener);
		
  	}finally{
  		this_mon.exit();
  	}
  }

  public void removeCellToolTipListener(TableCellToolTipListener listener) {
  	try{
  		this_mon.enter();
  	
  		if (cellToolTipListeners == null)
  			return;

		cellToolTipListeners.remove(listener);
  	}finally{
  		this_mon.exit();
  	}
  }

	public void addCellMouseListener(TableCellMouseListener listener) {
		try {
			this_mon.enter();

			if (cellMouseListeners == null)
				cellMouseListeners = new ArrayList(1);

			cellMouseListeners.add(listener);

		} finally {
			this_mon.exit();
		}
	}

	public void removeCellMouseListener(TableCellMouseListener listener) {
		try {
			this_mon.enter();

			if (cellMouseListeners == null)
				return;

			cellMouseListeners.remove(listener);

		} finally {
			this_mon.exit();
		}
	}

  public void invalidateCells() {
    TableStructureEventDispatcher tsed = TableStructureEventDispatcher.getInstance(sTableID);
    tsed.columnInvalidate(this);
  }
  
	public void addListeners(Object listenerObject) {
		if (listenerObject instanceof TableCellDisposeListener)
			addCellDisposeListener((TableCellDisposeListener)listenerObject);

		if (listenerObject instanceof TableCellRefreshListener)
			addCellRefreshListener((TableCellRefreshListener)listenerObject);

		if (listenerObject instanceof TableCellToolTipListener)
			addCellToolTipListener((TableCellToolTipListener)listenerObject);

		if (listenerObject instanceof TableCellAddedListener)
			addCellAddedListener((TableCellAddedListener)listenerObject);

		if (listenerObject instanceof TableCellMouseListener)
			addCellMouseListener((TableCellMouseListener)listenerObject);
	}

  /* Start of not plugin public API functions */
  //////////////////////////////////////////////

  public void setColumnAdded(boolean bAdded) {
    bColumnAdded = bAdded;
  }
  
  public boolean getColumnAdded() {
    return bColumnAdded;
  }
  
  public void setUseCoreDataSource(boolean bCoreDataSource) {
    this.bCoreDataSource = bCoreDataSource;
  }

  public boolean getUseCoreDataSource() {
    return bCoreDataSource;
  }
  
  public void invokeCellRefreshListeners(TableCellCore cell) {
    //System.out.println(this + " :: invokeCellRefreshListeners" + cellRefreshListeners);
    if (cellRefreshListeners == null)
      return;

    //System.out.println(this + " :: invokeCellRefreshListeners" + cellRefreshListeners.size());
    for (int i = 0; i < cellRefreshListeners.size(); i++){
    	
    	try{
    		((TableCellRefreshListener)(cellRefreshListeners.get(i))).refresh(cell);
    		
    	}catch( Throwable e ){
    		
    		Debug.printStackTrace(e);
    	}
    }
  }

  public void invokeCellAddedListeners(TableCellCore cell) {
    if (cellAddedListeners == null)
      return;
    for (int i = 0; i < cellAddedListeners.size(); i++){
    	
    	try{
    		((TableCellAddedListener)(cellAddedListeners.get(i))).cellAdded(cell);
   	
    	}catch( Throwable e ){
    		
    		Debug.printStackTrace(e);
    	}
    }
  }

  public void invokeCellDisposeListeners(TableCellCore cell) {
    if (cellDisposeListeners == null)
      return;
    for (int i = 0; i < cellDisposeListeners.size(); i++){
    	try{
    		((TableCellDisposeListener)(cellDisposeListeners.get(i))).dispose(cell);
    		
    	}catch( Throwable e ){
    		
    		Debug.printStackTrace(e);
    	}
    }
  }

  public void invokeCellToolTipListeners(TableCellCore cell, int type) {
    if (cellToolTipListeners == null)
      return;
    if (type == TableCellCore.TOOLTIPLISTENER_HOVER) {
      for (int i = 0; i < cellToolTipListeners.size(); i++){
    	  try{
    		  ((TableCellToolTipListener)(cellToolTipListeners.get(i))).cellHover(cell);
    	  }catch( Throwable e ){
        		
        	Debug.printStackTrace(e);
    	  }
      }
    } else {
      for (int i = 0; i < cellToolTipListeners.size(); i++){
    	  
    	    try{
    		  ((TableCellToolTipListener)(cellToolTipListeners.get(i))).cellHoverComplete(cell);
    	  	}catch( Throwable e ){
        		
        		Debug.printStackTrace(e);
        	}
      }
    }
  }

  public void invokeCellMouseListeners(TableCellMouseEvent event) {
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

  public void setPositionNoShift(int position) {
    iPosition = position;
  }

  public void loadSettings() {
    String sItemPrefix = "Table." + sTableID + "." + sName;
    iWidth = COConfigurationManager.getIntParameter(sItemPrefix + ".width", iWidth);
    if (iWidth <= 0)
      iWidth = 100;
    iPosition = COConfigurationManager.getIntParameter(sItemPrefix + ".position",
                                                       iPosition);
  }
    
  public void saveSettings() {
    String sItemPrefix = "Table." + sTableID + "." + sName;
    COConfigurationManager.setParameter(sItemPrefix + ".position", iPosition);
    COConfigurationManager.setParameter(sItemPrefix + ".width", iWidth);
  }

  public int getSWTAlign() {
    return iAlignment == ALIGN_LEAD ? SWT.LEAD
                                    : (iAlignment == ALIGN_CENTER) ? SWT.CENTER
                                                                   : SWT.TRAIL;
  }
  
  public String getTitleLanguageKey() {
  	try{
  		this_mon.enter();
  
	    if (sTitleLanguageKey == null) {
	      sTitleLanguageKey = sTableID + ".column." + sName;
	      if (!MessageText.keyExists(sTitleLanguageKey)) {
	        // Support "Old Style" language keys, which have a prefix of TableID + "View."
	        // Also, "MySeeders" is actually stored in "MyTorrents"..
	        String sKeyPrefix = (sTableID.equals(TableManager.TABLE_MYTORRENTS_COMPLETE) 
	                             ? TableManager.TABLE_MYTORRENTS_INCOMPLETE
	                             : sTableID) + "View.";
	        if (MessageText.keyExists(sKeyPrefix + sName))
	          sTitleLanguageKey = sKeyPrefix + sName;
	      }
	    }
	    return sTitleLanguageKey;
  	}finally{
  		
  		this_mon.exit();
  	}
  }

  public int getConsecutiveErrCount() {
    return iConsecutiveErrCount;
  }

  public void setConsecutiveErrCount(int iCount) {
    iConsecutiveErrCount = iCount;
  }

  public void removeContextMenuItem(TableContextMenuItem menuItem) {
    if (menuItems == null)
      return;

    menuItems.remove(menuItem);
  }

  public TableContextMenuItem addContextMenuItem(String key) {
    if (menuItems == null)
      menuItems = new ArrayList();

    // Hack.. should be using our own implementation..
    TableContextMenuItemImpl item = new TableContextMenuItemImpl("", key);
    menuItems.add(item);
    return item;
  }

  public TableContextMenuItem[] getContextMenuItems() {
    if (menuItems == null)
      return new TableContextMenuItem[0];

    return (TableContextMenuItem[])menuItems.toArray(new TableContextMenuItem[0]);
  }

	public boolean isObfusticated() {
		return bObfusticateData;
	}

	public void setObfustication(boolean hideData) {
		bObfusticateData = hideData;
	}

	public long getLastSortValueChange() {
		if (bSortValueLive) {
			return SystemTime.getCurrentTime();
		}
		return lLastSortValueChange;
	}

	public void setLastSortValueChange(long lastSortValueChange) {
		lLastSortValueChange = lastSortValueChange;
	}

	public boolean isSortValueLive() {
		return bSortValueLive;
	}

	public void setSortValueLive(boolean live) {
//		if (live && !bSortValueLive) {
//			System.out.println("Setting " + sTableID + ": " + sName + " to live sort value");
//		}
		bSortValueLive = live;
	}

	public void addRefreshTime(long ms) {
		if (ms == 0) {
			lStatsRefreshZeroCount++;
		} else {
			lStatsRefreshTotalTime += ms;
			lStatsRefreshCount++;
		}
	}
	
  public void generateDiagnostics(IndentWriter writer) {
		writer.println("Column " + sTableID + ":" + sName
				+ (bSortValueLive ? " (Live Sort)" : ""));
		try {
			writer.indent();

			if (lStatsRefreshCount > 0) {
				writer.println("Avg refresh time (" + lStatsRefreshCount
						+ " samples): " + (lStatsRefreshTotalTime / lStatsRefreshCount)
						+ " (" + lStatsRefreshZeroCount
						+ " zero ms refreshes not included)");
			}
			writer.println("Listeners: refresh=" + getListCountString(cellRefreshListeners) 
					+ "; dispose=" + getListCountString(cellDisposeListeners)
					+ "; mouse=" + getListCountString(cellMouseListeners)
					+ "; added=" + getListCountString(cellAddedListeners)
					+ "; tooltip=" + getListCountString(cellToolTipListeners));
			
			writer.println("lLastSortValueChange=" + lLastSortValueChange);
		} catch (Exception e) {
		} finally {
			writer.exdent();
		}
	}
  
  private String getListCountString(List l) {
  	if (l == null) {
  		return "-0";
  	}
  	return "" + l.size();
  }
}
