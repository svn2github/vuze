/*
 * File    : TableRowImpl.java
 * Originally TorrentRow.java, and changed to be more generic by TuxPaper
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableItem;

import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.core3.tracker.host.TRHostTorrent;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.pluginsimpl.local.disk.DiskManagerFileInfoImpl;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.peers.PeerImpl;
import org.gudy.azureus2.pluginsimpl.local.tracker.TrackerTorrentImpl;
import org.gudy.azureus2.ui.swt.components.BufferedTableRow;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;
import org.gudy.azureus2.ui.swt.views.TableView;
import org.gudy.azureus2.ui.swt.views.table.TableCellCore;
import org.gudy.azureus2.ui.swt.views.table.TableColumnCore;
import org.gudy.azureus2.ui.swt.views.table.TableRowCore;



/** Represents an entire row in a table.  Stores each cell belonging to the
 * row and handles refreshing them.
 *
 * @see TableCellImpl
 */
public class TableRowImpl 
       extends BufferedTableRow 
       implements TableRowCore
{
  /** List of cells in this column.  They are not stored in display order */
  private Map mTableCells;
  private String sTableID;
  
  private Object coreDataSource;
  private Object pluginDataSource;

  /** Must be initialized on the Display thread */
  public TableRowImpl(TableView tableView, Object dataSource,
                      boolean bSkipFirstColumn) {
    this(tableView, dataSource, bSkipFirstColumn, -1);
  }

  public TableRowImpl(TableView tableView, Object dataSource,
                      boolean bSkipFirstColumn, int index) {
    super(tableView.getTable(), index);
    this.sTableID = tableView.getTableID();
    coreDataSource = dataSource;
    mTableCells = new HashMap();

    // TableColumnCore objects contains a list of all column definitions for the table
    TableColumnCore[] tableColumns = tableView.getAllTableColumnCore();
    
    // create all the cells for the column
    for (int i = 0; i < tableColumns.length; i++) {
      //System.out.println(dataSource + ": " + tableColumns[i].getName() + ": " + tableColumns[i].getPosition());
      mTableCells.put(tableColumns[i].getName(), 
                      new TableCellImpl(TableRowImpl.this, tableColumns[i], 
                                        bSkipFirstColumn));
    }

    TableItem ti = getItem();
    if (ti != null) ti.setData("TableRow", this);
  }

  public boolean isValid() {
    boolean valid = true;
    Iterator iter = mTableCells.values().iterator();
    while (iter.hasNext()) {
      TableCellCore cell = (TableCellCore)iter.next();
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
    return (TableCell)mTableCells.get(field);
  }
  
  /* Start Core-Only functions */
  ///////////////////////////////

  public void setValid(boolean valid) {
    Iterator iter = mTableCells.values().iterator();
    while (iter.hasNext()) {
      TableCellCore cell = (TableCellCore)iter.next();
      if (cell != null)
        cell.setValid(valid);
    }
  }

  public void delete() {
    Display display = SWTThread.getInstance().getDisplay();
    if (display == null || display.isDisposed())
      return;
    display.asyncExec(new AERunnable() {
      public void runSupport() {
        // Manually dispose of TableCellImpl objects, since row does
        // not contain a list of them.
        Iterator iter = mTableCells.values().iterator();
        while(iter.hasNext()) {
          TableCellCore item = (TableCellCore)iter.next();
          item.dispose();
        }

        TableItem ti = getItem();
        if (ti != null && !ti.isDisposed()) {
          ti.setData("TableRow", null);
        }
        // Dispose of row
        dispose();
      }
    });
  }

  public void refresh(boolean bDoGraphics) {
    if (isDisposed())
      return;

    Iterator iter = mTableCells.values().iterator();
    while(iter.hasNext()) {
      TableCellCore item = (TableCellCore)iter.next();
      item.refresh();
    }
  }
  
  public void locationChanged(int iStartColumn) {
  	if (isDisposed())
  		return;

  	Iterator iter = mTableCells.values().iterator();
  	while(iter.hasNext()) {
  		TableCellCore item = (TableCellCore)iter.next();
  		if (item.getTableColumn().getPosition() > iStartColumn)
  		  item.locationChanged();
  	}
  }

  public void doPaint(GC gc) {
    if (isDisposed())
      return;

    Iterator iter = mTableCells.values().iterator();
    while(iter.hasNext()) {
      TableCellCore item = (TableCellCore) iter.next();
  		if (item.needsPainting()) {
  			item.doPaint(gc);
  		}
    }
  }

  public TableCellCore getTableCellCore(String field) {
    return (TableCellCore)mTableCells.get(field);
  }

  public void setHeight(int iHeight) {
    TableItem ti = getItem();
    if (ti == null)
      return;
    // set row height by setting image
    Image image = new Image(ti.getDisplay(), 1, iHeight);
    ti.setImage(0, image);
    ti.setImage(0, null);
    image.dispose();
  }

  public Object getDataSource(boolean bCoreObject) {
    TableItem ti = getItem();
    if (ti == null || ti.isDisposed())
      return null;

    if (bCoreObject)
      return coreDataSource;
      
    if (pluginDataSource != null)
      return pluginDataSource;

    if (sTableID.equals(TableManager.TABLE_MYTORRENTS_COMPLETE) ||
        sTableID.equals(TableManager.TABLE_MYTORRENTS_INCOMPLETE)) {
      DownloadManager dm = (DownloadManager)coreDataSource;
      if (dm != null) {
        try {
          pluginDataSource = DownloadManagerImpl.getDownloadStatic(dm);
        } catch (DownloadException e) { /* Ignore */ }
      }
    }
    if (sTableID.equals(TableManager.TABLE_TORRENT_PEERS)) {
      PEPeer peer = (PEPeer)coreDataSource;
      if (peer != null)
        pluginDataSource = new PeerImpl(peer);
    }

    if (sTableID.equals(TableManager.TABLE_TORRENT_PIECES)) {
      // XXX There is no Piece object for plugins yet
      PEPiece piece = (PEPiece)coreDataSource;
      if (piece != null)
        pluginDataSource = null;
    }

    if (sTableID.equals(TableManager.TABLE_TORRENT_FILES)) {
      DiskManagerFileInfo fileInfo = (DiskManagerFileInfo)coreDataSource;
      if (fileInfo != null)
        pluginDataSource = new DiskManagerFileInfoImpl(fileInfo);
    }

    if (sTableID.equals(TableManager.TABLE_MYSHARES)) {
      pluginDataSource = coreDataSource;
    }

    if (sTableID.equals(TableManager.TABLE_MYTRACKER)) {
      TRHostTorrent item = (TRHostTorrent)coreDataSource;
      if (item != null)
        pluginDataSource = new TrackerTorrentImpl(item);
    }
    
    return pluginDataSource;
  }
  
  // Flicker
  public boolean setIndex(int newIndex) {
    if (super.setIndex(newIndex)) {
      getItem().setData("TableRow", this);
      return true;
    }
    return false;
  }

  public boolean setDataSource(Object dataSource) {
    if (getDataSource(true) != dataSource) {
      //System.out.println(getDataSource(true) + " != " + dataSource);
      coreDataSource = dataSource;
      pluginDataSource = null;
      setValid(false);
      return true;
    }
    return false;
  }
}
