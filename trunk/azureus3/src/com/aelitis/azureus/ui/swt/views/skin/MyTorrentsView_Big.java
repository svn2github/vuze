package com.aelitis.azureus.ui.swt.views.skin;

import org.eclipse.swt.SWT;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.ui.swt.views.MyTorrentsView;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewSWTImpl;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.util.Constants;

import org.gudy.azureus2.plugins.ui.tables.TableManager;

public class MyTorrentsView_Big
	extends MyTorrentsView
{
	private final int torrentFilterMode;

	public MyTorrentsView_Big(AzureusCore _azureus_core, int torrentFilterMode,
			TableColumnCore[] basicItems) {
		this.torrentFilterMode = torrentFilterMode;
		init(
				_azureus_core,
				SBC_LibraryView.getTableIdFromFilterMode(torrentFilterMode, true),
				torrentFilterMode == SBC_LibraryView.TORRENTS_INCOMPLETE ? false : true,
				basicItems);
		//setForceHeaderVisible(true);
	}
	

	public boolean isOurDownloadManager(DownloadManager dm) {
		if (PlatformTorrentUtils.getAdId(dm.getTorrent()) != null) {
			return false;
		}
		
		if (torrentFilterMode == SBC_LibraryView.TORRENTS_UNOPENED) {
			if (PlatformTorrentUtils.getHasBeenOpened(dm.getTorrent())) {
				return false;
			}
		} else if (torrentFilterMode == SBC_LibraryView.TORRENTS_ALL) {
			return true;
		}
		
		return super.isOurDownloadManager(dm);
	}

	protected TableViewSWT createTableView(TableColumnCore[] basicItems) {
		String tableID;
		switch (torrentFilterMode) {
			case SBC_LibraryView.TORRENTS_COMPLETE:
				tableID = TableManager.TABLE_MYTORRENTS_COMPLETE_BIG;
				break;

			case SBC_LibraryView.TORRENTS_INCOMPLETE:
				tableID = TableManager.TABLE_MYTORRENTS_INCOMPLETE_BIG;
				break;
				
			case SBC_LibraryView.TORRENTS_UNOPENED:
				tableID = TableManager.TABLE_MYTORRENTS_UNOPENED_BIG;
				break;
				
			case SBC_LibraryView.TORRENTS_ALL:
				tableID = TableManager.TABLE_MYTORRENTS_ALL_BIG;
				break;

			default:
				tableID = "bad";
				break;
		}
		TableViewSWTImpl tv = new TableViewSWTImpl(tableID, "MyTorrentsView_Big",
				basicItems, "#", SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL
						| SWT.BORDER);
		return tv;
	}
	
	// @see org.gudy.azureus2.ui.swt.views.MyTorrentsView#defaultSelected(com.aelitis.azureus.ui.common.table.TableRowCore[])
	public void defaultSelected(TableRowCore[] rows) {
		if (!isSeedingView) {
			super.defaultSelected(rows);
			return;
		}
		if (rows == null || rows.length > 1) {
			return;
		}
		Object ds = rows[0].getDataSource(true);
		TorrentListViewsUtils.playOrStreamDataSource(ds, null,
				Constants.DL_REFERAL_DBLCLICK);
	}

	protected int getRowDefaultHeight() {
		return 36;
	}

}
