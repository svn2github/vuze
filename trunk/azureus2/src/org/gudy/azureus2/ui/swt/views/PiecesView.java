/*
 * Created on 2 juil. 2003
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
package org.gudy.azureus2.ui.swt.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerPeerListener;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.ui.swt.components.Legend;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.views.tableitems.pieces.*;
import org.gudy.azureus2.ui.swt.views.table.TableColumnCore;

/**
 * @author Olivier
 * @author TuxPaper
 *         2004/Apr/20: Remove need for tableItemToObject
 *         2004/Apr/21: extends TableView instead of IAbstractView
 * @author MjrTom
 *			2005/Oct/08: Add PriorityItem, SpeedItem
*/

public class PiecesView 
       extends TableView 
       implements DownloadManagerPeerListener
{
  private final static TableColumnCore[] basicItems = {
    new PieceNumberItem(),
    new SizeItem(),
    new BlockCountItem(),
    new BlocksItem(),
    new CompletedItem(),
    new AvailabilityItem(),
    new TypeItem(),
    new ReservedByItem(),
    new WritersItem(),
    new PriorityItem(),
    new SpeedItem()
  };

  DownloadManager manager;
  
  /**
   * Initialize
   *
   */
	public PiecesView() {
		super(TableManager.TABLE_TORRENT_PIECES, "PiecesView", basicItems,
			basicItems[0].getName(), SWT.SINGLE | SWT.FULL_SELECTION | SWT.VIRTUAL);
		bEnableTabViews = true;
	}

	public void dataSourceChanged(Object newDataSource) {
  	if (manager != null)
  		manager.removePeerListener(this);

		if (newDataSource == null)
			manager = null;
		else if (newDataSource instanceof Object[])
			manager = (DownloadManager)((Object[])newDataSource)[0];
		else
			manager = (DownloadManager)newDataSource;
  	
  	if (manager != null && getTable() != null) {
    	manager.addPeerListener(this, false);
    	addExistingDatasources();
    }
	}

	public void initialize(Composite composite) {
    super.initialize(composite);
    
    Legend.createLegendComposite(
    		getTableComposite(),
    			BlocksItem.colors,
    		new String[] {
        			"PiecesView.legend.requested",
        			"PiecesView.legend.written",        			
    				"PiecesView.legend.downloaded",
    				"PiecesView.legend.incache"}
        	);
  }

  public void tableStructureChanged() {
    //1. Unregister for item creation
  	if (manager != null)
  		manager.removePeerListener(this);
    
    super.tableStructureChanged();

    //5. Re-add as a listener
    if (manager != null) {
    	manager.addPeerListener(this, false);
    	addExistingDatasources();
    }
  }
  
  public void delete() {
  	if (manager != null)
  		manager.removePeerListener(this);
    super.delete();
  }

  /* DownloadManagerPeerListener implementation */
  public void pieceAdded(PEPiece created) {
    addDataSource(created);
  }

  public void pieceRemoved(PEPiece removed) {    
    removeDataSource(removed);
  }
  
  public void peerAdded(PEPeer peer) {  }
  public void peerRemoved(PEPeer peer) {  }
	public void peerManagerAdded(PEPeerManager manager) {	}
	public void peerManagerRemoved(PEPeerManager	manager) {
		removeAllTableRows();
	}

	/**
	 * Add datasources already in existance before we called addListener.
	 * Faster than allowing addListener to call us one datasource at a time. 
	 */
	private void addExistingDatasources() {
		if (manager == null)
			return;
		Object[] dataSources = manager.getCurrentPieces();
		if (dataSources == null || dataSources.length == 0)
			return;
		
		addDataSources(dataSources);
	}

	public void initializeTable(Table table) {
		super.initializeTable(table);

		// Table is initialized, we can add datasources to it now
  	manager.addPeerListener(this, false);
  	addExistingDatasources();
	}
}
