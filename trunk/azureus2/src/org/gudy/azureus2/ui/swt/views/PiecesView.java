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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerPeerListener;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.views.tableitems.pieces.*;
import org.gudy.azureus2.ui.swt.views.table.TableColumnCore;

/**
 * @author Olivier
 * @author TuxPaper
 *         2004/Apr/20: Remove need for tableItemToObject
 *         2004/Apr/21: extends TableView instead of IAbstractView
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
    new AvailabilityItem()
  };

  DownloadManager manager;
  
  public PiecesView(DownloadManager manager) {
    super(TableManager.TABLE_TORRENT_PIECES, "PiecesView",
          basicItems, basicItems[0].getName(), SWT.SINGLE | SWT.FULL_SELECTION);
    
    this.manager = manager;
  }

  public void initialize(Composite composite) {
    super.initialize(composite);
    
    super.createLegendComposite(
        	new Color[] {
        		Colors.blues[Colors.BLUES_DARKEST],    
    			Colors.blues[Colors.BLUES_MIDLIGHT],
    			Colors.red },
    		new String[] {
        			"pieceView.legend.written",
    				"pieceView.legend.requested",
    				"pieceView.legend.downloaded"}
        	);
    
    manager.addPeerListener(this);
  }

  public void tableStructureChanged() {
    //1. Unregister for item creation
    manager.removePeerListener(this);
    
    super.tableStructureChanged();

    //5. Re-add as a listener
    manager.addPeerListener(this);
  }
  
  public void delete() {
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
	public void peerManagerRemoved(PEPeerManager	manager) {	}
}
