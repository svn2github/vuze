/*
 * Created on 2 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt.views;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core3.config.impl.ConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerPeerListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.views.tableitems.PieceTableItem;
import org.gudy.azureus2.ui.swt.views.utils.SortableTable;
import org.gudy.azureus2.ui.swt.views.utils.TableSorter;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;

/**
 * @author Olivier
 * 
 */
public class PiecesView extends AbstractIView implements DownloadManagerPeerListener,SortableTable {

  DownloadManager manager;
  Table table;
  HashMap pieceToPieceItem;
  HashMap tableItemToObject;
  TableSorter sorter;

  public PiecesView(DownloadManager manager) {
    this.manager = manager;
    pieceToPieceItem = new HashMap();
    tableItemToObject = new HashMap();    
  }

  public void initialize(Composite composite) {
    table = new Table(composite, SWT.SINGLE | SWT.FULL_SELECTION);
    table.setLinesVisible(false);
    table.setHeaderVisible(true);
    String[] titlesPieces =
      { "#", "size", "numberofblocks", "blocks", "completed", "availability" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
    int[] alignPieces =
      { SWT.LEFT, SWT.RIGHT, SWT.RIGHT, SWT.CENTER, SWT.RIGHT, SWT.RIGHT };
    int[] widths = 
      { 50, 60, 65, 300, 80, 80 };
    for (int i = 0; i < titlesPieces.length; i++) {
      TableColumn column = new TableColumn(table, alignPieces[i]);
      column.setWidth(widths[i]);
      Messages.setLanguageText(column, "PiecesView." + titlesPieces[i]); //$NON-NLS-1$
      column.addControlListener(new ControlAdapter() {
        public void controlResized(ControlEvent e) {
          refresh();
        }
      });
    }
    
    sorter = new TableSorter(this, "PiecesView", "#",true);
    
    sorter.addIntColumnListener(table.getColumn(0),"#");
    sorter.addIntColumnListener(table.getColumn(1),"size");
    sorter.addIntColumnListener(table.getColumn(2),"nbBlocs");
    sorter.addIntColumnListener(table.getColumn(3),"done");
    sorter.addIntColumnListener(table.getColumn(4),"done");
    sorter.addIntColumnListener(table.getColumn(5),"availability");
  }

  public Composite getComposite() {
    return table;
  }

  public void refresh() {
    if(getComposite() == null || getComposite().isDisposed())
      return;
      
    sorter.reOrder(false);

    synchronized (pieceToPieceItem) {
      Iterator iter = pieceToPieceItem.values().iterator();
      while (iter.hasNext()) {
        PieceTableItem pti = (PieceTableItem) iter.next();
        pti.updateDisplay();
      }
    }
  }


  public void delete() {
    manager.removePeerListener(this);
    Iterator iter = pieceToPieceItem.values().iterator();
    while (iter.hasNext()) {
      PieceTableItem item = (PieceTableItem) iter.next();
      item.remove();
    }
    if (table != null && !table.isDisposed())
      table.dispose();
    ConfigurationManager.getInstance().removeParameterListener("ReOrder Delay", sorter);
  }

  public String getData() {
    return "PiecesView.title.short"; //$NON-NLS-1$
  }

  public String getFullTitle() {
    return MessageText.getString("PiecesView.title.full"); //$NON-NLS-1$
  }

  public void pieceAdded(PEPiece created) {
    synchronized (pieceToPieceItem) {
      if (pieceToPieceItem.containsKey(created))
        return;
      PieceTableItem item = new PieceTableItem(this,table, (PEPiece) created);
      pieceToPieceItem.put(created, item);      
    }
  }

  public void pieceRemoved(PEPiece removed) {    
    PieceTableItem item;
    synchronized (pieceToPieceItem) {
      item = (PieceTableItem) pieceToPieceItem.remove(removed);
    }
    if (item == null)
      return;
    tableItemToObject.remove(item.getTableItem());
    item.remove();
  }
  
  public void peerAdded(PEPeer peer) {
  }

  public void peerRemoved(PEPeer peer) {
  }
  
  public void setItem(TableItem item, PEPiece piece) {
    tableItemToObject.put(item, piece);
  }
  
  /*
   * SortableTable implementation
   */
  
  public Table getTable() {
    return table;
  }
  
  public Map getObjectToSortableItemMap() {
    return pieceToPieceItem;
  }

  public Map getTableItemToObjectMap() {
    return tableItemToObject;
  }



}
