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
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerPeerListener;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.views.table.TableColumnCore;
import org.gudy.azureus2.ui.swt.views.table.TableRowCore;
import org.gudy.azureus2.ui.swt.views.tableitems.peers.*;


/**
 * @author Olivier
 * @author TuxPaper
 *         2004/Apr/20: Use TableRowImpl instead of PeerRow
 *         2004/Apr/20: Remove need for tableItemToObject
 *         2004/Apr/21: extends TableView instead of IAbstractView
 */
public class PeersView 
       extends TableView
       implements DownloadManagerPeerListener
{
  private static final TableColumnCore[] basicItems = {
    new IpItem(),
    new ClientItem(),
    new TypeItem(),
    new PiecesItem(),
    new PercentItem(),
    new DownSpeedItem(),
    new UpSpeedItem(),

    new PortItem(),
    new InterestedItem(),
    new ChokedItem(),
    new DownItem(),
    new InterestingItem(),
    new ChokingItem(),
    new OptimisticUnchokeItem(),
    new UpItem(),
    new StatUpItem(),
    new SnubbedItem(),
    new TotalDownSpeedItem(),
    new DiscardedItem(),
    new UniquePieceItem(),
    new TimeToSendPieceItem(),
    new AllowedUpItem()
  };
  private DownloadManager manager;

  public PeersView(DownloadManager manager) {
    super(TableManager.TABLE_TORRENT_PEERS, "PeersView", 
          basicItems, "pieces");
    this.manager = manager;
    iCellHeight = 16;
  }
  
  public void initialize(Composite composite) {
    super.initialize(composite);
    manager.addPeerListener(this);
  }
  
  public void tableStructureChanged() {
    //1. Unregister for item creation
    manager.removePeerListener(this);
    
    super.tableStructureChanged();

    //5. Re-add as a listener
    manager.addPeerListener(this);
  }

  public void fillMenu(final Menu menu) {
    final MenuItem item = new MenuItem(menu, SWT.CHECK);

    menu.addListener(SWT.Show, new Listener() {
      public void handleEvent(Event e) {
        PEPeer peer = (PEPeer)getFirstSelectedDataSource();
        if (peer == null) {
          item.setEnabled(false);
          return;
        }
        item.setEnabled(true);
        item.setSelection(peer.isSnubbed());
      }
    });

    Messages.setLanguageText(item, "PeersView.menu.snubbed"); //$NON-NLS-1$
    item.addListener(SWT.Selection,
                     new SelectedTableRowsListener() {
      public void run(TableRowCore row) {
        ((PEPeer)row.getDataSource(true)).setSnubbed(item.getSelection());
      }
    });
    
    new MenuItem(menu, SWT.SEPARATOR);

    super.fillMenu(menu);
  }


  public void delete() {
    manager.removePeerListener(this);
    super.delete();
  }

  /* DownloadManagerPeerListener implementation */
  public void peerAdded(PEPeer created) {
    addDataSource(created);
  }

  public void peerRemoved(PEPeer removed) {
    removeDataSource(removed);
  }

  public void pieceAdded(PEPiece piece) {  }
  public void pieceRemoved(PEPiece piece) {  }
  public void peerManagerAdded(PEPeerManager manager) {	}
  public void peerManagerRemoved(PEPeerManager manager) { }
}
