/*
 * Created on 2 juil. 2003
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
package org.gudy.azureus2.ui.swt.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerPeerListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.ipfilter.IpFilterManagerFactory;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.peer.PeerInfoView;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewSWTImpl;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewTab;
import org.gudy.azureus2.ui.swt.views.tableitems.peers.*;

import com.aelitis.azureus.ui.common.table.*;

/**
 * @author Olivier
 * @author TuxPaper
 *         2004/Apr/20: Use TableRowImpl instead of PeerRow
 *         2004/Apr/20: Remove need for tableItemToObject
 *         2004/Apr/21: extends TableView instead of IAbstractView
 * @author MjrTom
 *			2005/Oct/08: Add PieceItem
 */

public class PeersView
	extends TableViewTab
	implements DownloadManagerPeerListener, TableDataSourceChangedListener,
	TableLifeCycleListener, TableViewSWTMenuFillListener
{
  private static final TableColumnCore[] basicItems = {
    new IpItem(),
    new ClientItem(),
    new TypeItem(),
    new MessagingItem(),
    new EncryptionItem(),
    new PiecesItem(),
    new PercentItem(),
    new DownSpeedItem(),
    new UpSpeedItem(),
    new PeerSourceItem(),
    new HostNameItem(),
    new PortItem(),
    new InterestedItem(),
    new ChokedItem(),
    new DownItem(),
    new InterestingItem(),
    new ChokingItem(),
    new OptimisticUnchokeItem(),
    new UpItem(),
    new UpDownRatioItem(),
    new GainItem(),
    new StatUpItem(),
    new SnubbedItem(),
    new TotalDownSpeedItem(),
    new DiscardedItem(),
    new UniquePieceItem(),
    new TimeToSendPieceItem(),
    new DLedFromOthersItem(),
    new UpRatioItem(),
    new StateItem(),
    new ConnectedTimeItem(),
    new PieceItem(),
    new IncomingRequestCountItem(),
    new OutgoingRequestCountItem(),
    new UpSpeedLimitItem(),
    new DownSpeedLimitItem(),
    new LANItem(),
    new PeerIDItem(),
    new PeerByteIDItem(),
    new HandshakeReservedBytesItem(),
  };
  
  private DownloadManager manager;
	private TableViewSWT tv;
	private Shell shell;


  /**
   * Initialize
   *
   */
  public PeersView() {
		tv = new TableViewSWTImpl(TableManager.TABLE_TORRENT_PEERS, "PeersView",
				basicItems, "pieces", SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL);
		setTableView(tv);
		tv.setRowDefaultHeight(16);
		tv.setEnableTabViews(true);
		tv.setCoreTabViews(new IView[] {
			new PeerInfoView(),
			new LoggerView()
		});
		tv.addTableDataSourceChangedListener(this, true);
		tv.addLifeCycleListener(this);
		tv.addMenuFillListener(this);
	}
  
	public void tableDataSourceChanged(Object newDataSource) {
  	if (manager != null)
  		manager.removePeerListener(this);

		if (newDataSource == null)
			manager = null;
		else if (newDataSource instanceof Object[])
			manager = (DownloadManager)((Object[])newDataSource)[0];
		else
			manager = (DownloadManager)newDataSource;

  	if (manager != null && !tv.isDisposed()) {
    	manager.addPeerListener(this, false);
    	addExistingDatasources();
    }
	}

  
	// @see com.aelitis.azureus.ui.common.table.TableLifeCycleListener#tableViewInitialized()
	public void tableViewInitialized() {
		if (tv instanceof TableViewSWT) {
			shell = ((TableViewSWT)tv).getComposite().getShell();
		} else {
			shell = Utils.findAnyShell();
		}

		if (manager != null) {
  		manager.removePeerListener(this);
			manager.addPeerListener(this, false);
		}
  	addExistingDatasources();
	}
	
	public void tableViewDestroyed() {
  	if (manager != null) {
  		manager.removePeerListener(this);
  	}
	}

	public void fillMenu(final Menu menu) {

		final MenuItem block_item = new MenuItem(menu, SWT.CHECK);

		Object[] peers = tv.getSelectedDataSources();
		
		boolean hasSelection = (peers.length > 0);

		boolean downSpeedDisabled	= false;
		boolean	downSpeedUnlimited	= false;
		long	totalDownSpeed		= 0;
		long	downSpeedSetMax		= 0;
		long	maxDown				= 0;
		boolean upSpeedDisabled		= false;
		boolean upSpeedUnlimited	= false;
		long	totalUpSpeed		= 0;
		long	upSpeedSetMax		= 0;
		long	maxUp				= 0;
		
		if (hasSelection){
			for (int i = 0; i < peers.length; i++) {
				PEPeer peer = (PEPeer)peers[i];

				try {
					int maxul = peer.getStats().getUploadRateLimitBytesPerSecond();
					
					maxUp += maxul * 4;
					
					if (maxul == 0) {
						upSpeedUnlimited = true;
					}else{
						if ( maxul > upSpeedSetMax ){
							upSpeedSetMax	= maxul;
						}
					}
					if (maxul == -1) {
						maxul = 0;
						upSpeedDisabled = true;
					}
					totalUpSpeed += maxul;

					int maxdl = peer.getStats().getDownloadRateLimitBytesPerSecond();
					
					maxDown += maxdl * 4;
					
					if (maxdl == 0) {
						downSpeedUnlimited = true;
					}else{
						if ( maxdl > downSpeedSetMax ){
							downSpeedSetMax	= maxdl;
						}
					}
					if (maxdl == -1) {
						maxdl = 0;
						downSpeedDisabled = true;
					}
					totalDownSpeed += maxdl;

				} catch (Exception ex) {
					Debug.printStackTrace(ex);
				}
			}
		}
		
		PEPeer peer = (PEPeer) tv.getFirstSelectedDataSource();


		if ( peer == null || peer.getManager().getDiskManager().getRemainingExcludingDND() > 0 ){
			// disallow peer upload blocking when downloading
			block_item.setSelection(false);
			block_item.setEnabled(false);
		}
		else {
			block_item.setEnabled(true);
			block_item.setSelection(peer.isSnubbed());
		}

		Messages.setLanguageText(block_item, "PeersView.menu.blockupload");
		block_item.addListener(SWT.Selection, new TableSelectedRowsListener(tv) {
			public void run(TableRowCore row) {
				((PEPeer) row.getDataSource(true))
						.setSnubbed(block_item.getSelection());
			}
		});

		final MenuItem ban_item = new MenuItem(menu, SWT.PUSH);

		Messages.setLanguageText(ban_item, "PeersView.menu.kickandban");
		ban_item.addListener(SWT.Selection, new TableSelectedRowsListener(tv) {
			public void run(TableRowCore row) {
				PEPeer peer = (PEPeer) row.getDataSource(true);
				String msg = MessageText.getString("PeersView.menu.kickandban.reason");
				IpFilterManagerFactory.getSingleton().getIPFilter().ban(peer.getIp(),
						msg, true );
				peer.getManager().removePeer(peer);
			}
		});

		// === advanced menu ===

		final MenuItem itemAdvanced = new MenuItem(menu, SWT.CASCADE);
		Messages.setLanguageText(itemAdvanced, "MyTorrentsView.menu.advancedmenu"); //$NON-NLS-1$
		itemAdvanced.setEnabled(hasSelection);

		final Menu menuAdvanced = new Menu(shell, SWT.DROP_DOWN);
		itemAdvanced.setMenu(menuAdvanced);

		// advanced > Download Speed Menu //

		ViewUtils.addSpeedMenu(
			shell,
			menuAdvanced,
			hasSelection,
			downSpeedDisabled,
			downSpeedUnlimited,
			totalDownSpeed,
			downSpeedSetMax,
			maxDown,
			upSpeedDisabled,
			upSpeedUnlimited,
			totalUpSpeed,
			upSpeedSetMax,
			maxUp,
			peers.length,
			new ViewUtils.SpeedAdapter()
			{
				public void 
				setDownSpeed(
					int speed ) 
				{
					setSelectedPeersDownSpeed( speed );	
				}
				
				public void 
				setUpSpeed(
					int speed ) 
				{
					setSelectedPeersUpSpeed( speed );
				}
			});
		new MenuItem(menu, SWT.SEPARATOR);
	}

	public void addThisColumnSubMenu(String columnName, Menu menuThisColumn) {
	}

	private void setSelectedPeersUpSpeed(int speed) {      
		Object[] peers = tv.getSelectedDataSources();
		if(peers.length > 0) {            
			for (int i = 0; i < peers.length; i++) {
				try {
					PEPeer peer = (PEPeer)peers[i];
					peer.getStats().setUploadRateLimitBytesPerSecond(speed);
				} catch (Exception e) {
					Debug.printStackTrace( e );
				}
			}
		}
	}

	private void setSelectedPeersDownSpeed(int speed) {      
		Object[] peers = tv.getSelectedDataSources();
		if(peers.length > 0) {            
			for (int i = 0; i < peers.length; i++) {
				try {
					PEPeer peer = (PEPeer)peers[i];
					peer.getStats().setDownloadRateLimitBytesPerSecond(speed);
				} catch (Exception e) {
					Debug.printStackTrace( e );
				}
			}
		}
	}
  
  /* DownloadManagerPeerListener implementation */
  public void peerAdded(PEPeer created) {
    tv.addDataSource(created);
  }

  public void peerRemoved(PEPeer removed) {
    tv.removeDataSource(removed);
  }

  public void pieceAdded(PEPiece piece) {  }
  public void pieceRemoved(PEPiece piece) {  }
  public void peerManagerWillBeAdded( PEPeerManager	peer_manager ){}
  public void peerManagerAdded(PEPeerManager manager) {	}
  public void peerManagerRemoved(PEPeerManager manager) {
  	tv.removeAllTableRows();
  }

	/**
	 * Add datasources already in existance before we called addListener.
	 * Faster than allowing addListener to call us one datasource at a time. 
	 */
	private void addExistingDatasources() {
		if (manager == null || tv.isDisposed()) {
			return;
		}

		Object[] dataSources = manager.getCurrentPeers();
		if (dataSources == null || dataSources.length == 0) {
			return;
		}
		
		tv.addDataSources(dataSources);
		tv.processDataSourceQueue();
	}
}
