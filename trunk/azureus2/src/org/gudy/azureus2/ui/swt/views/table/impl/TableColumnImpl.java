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
 
package org.gudy.azureus2.ui.swt.views.table.impl;

import java.util.ArrayList;

import org.eclipse.swt.SWT;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.plugins.ui.UIRuntimeException;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellDisposeListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellToolTipListener;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
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
  private String sTableID;
  private boolean bColumnAdded;
  private boolean bCoreDataSource;
	private ArrayList cellRefreshListeners;
	private ArrayList cellAddedListeners;
	private ArrayList cellDisposeListeners;
	private ArrayList cellToolTipListeners;
	private int iConsecutiveErrCount;
  private ArrayList menuItems;

  /** Create a column object for the specified table.
   *
   * @param tableID table in which the column belongs to
   * @param columnID name/id of the column
   */
  public TableColumnImpl(String tableID, String columnID) {
    sTableID = tableID;
    sName = columnID;
    iType = TYPE_TEXT;
    iPosition = POSITION_INVISIBLE;
    iWidth = 50;
    iAlignment = ALIGN_LEAD;
    bColumnAdded = false;
    bCoreDataSource = false;
    iInterval = INTERVAL_INVALID_ONLY;
    iConsecutiveErrCount = 0;
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
  
  public synchronized void addCellRefreshListener(TableCellRefreshListener listener) {
    if (cellRefreshListeners == null)
      cellRefreshListeners = new ArrayList();

		cellRefreshListeners.add(listener);
    //System.out.println(this + " :: addCellRefreshListener " + listener + ". " + cellRefreshListeners.size());
  }

  public synchronized void removeCellRefreshListener(TableCellRefreshListener listener) {
    if (cellRefreshListeners == null)
      return;

		cellRefreshListeners.remove(listener);
  }

  public void setRefreshInterval(int interval) {
    iInterval = interval;
  }

  public int getRefreshInterval() {
    return iInterval;
  }

  public synchronized void addCellAddedListener(TableCellAddedListener listener) {
    if (cellAddedListeners == null)
      cellAddedListeners = new ArrayList();

		cellAddedListeners.add(listener);
  }

  public synchronized void removeCellAddedListener(TableCellAddedListener listener) {
    if (cellAddedListeners == null)
      return;

		cellAddedListeners.remove(listener);
  }

  public synchronized void addCellDisposeListener(TableCellDisposeListener listener) {
    if (cellDisposeListeners == null)
      cellDisposeListeners = new ArrayList();

		cellDisposeListeners.add(listener);
  }

  public synchronized void removeCellDisposeListener(TableCellDisposeListener listener) {
    if (cellDisposeListeners == null)
      return;

		cellDisposeListeners.remove(listener);
  }

  public synchronized void addCellToolTipListener(TableCellToolTipListener listener) {
    if (cellToolTipListeners == null)
      cellToolTipListeners = new ArrayList();

		cellToolTipListeners.add(listener);
  }

  public synchronized void removeCellToolTipListener(TableCellToolTipListener listener) {
    if (cellToolTipListeners == null)
      return;

		cellToolTipListeners.remove(listener);
  }

  public void invalidateCells() {
    TableStructureEventDispatcher tsed = TableStructureEventDispatcher.getInstance(sTableID);
    tsed.columnInvalidate(this);
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
    for (int i = 0; i < cellRefreshListeners.size(); i++)
      ((TableCellRefreshListener)(cellRefreshListeners.get(i))).refresh(cell);
  }

  public void invokeCellAddedListeners(TableCellCore cell) {
    if (cellAddedListeners == null)
      return;
    for (int i = 0; i < cellAddedListeners.size(); i++)
      ((TableCellAddedListener)(cellAddedListeners.get(i))).cellAdded(cell);
  }

  public void invokeCellDisposeListeners(TableCellCore cell) {
    if (cellDisposeListeners == null)
      return;
    for (int i = 0; i < cellDisposeListeners.size(); i++)
      ((TableCellDisposeListener)(cellDisposeListeners.get(i))).dispose(cell);
  }

  public void invokeCellToolTipListeners(TableCellCore cell, int type) {
    if (cellToolTipListeners == null)
      return;
    if (type == TableCellCore.TOOLTIPLISTENER_HOVER) {
      for (int i = 0; i < cellToolTipListeners.size(); i++)
        ((TableCellToolTipListener)(cellToolTipListeners.get(i))).cellHover(cell);
    } else {
      for (int i = 0; i < cellToolTipListeners.size(); i++)
        ((TableCellToolTipListener)(cellToolTipListeners.get(i))).cellHoverComplete(cell);
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
  
  public synchronized String getTitleLanguageKey() {
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
}
