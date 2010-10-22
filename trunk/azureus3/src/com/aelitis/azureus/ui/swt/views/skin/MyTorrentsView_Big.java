package com.aelitis.azureus.ui.swt.views.skin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.views.MyTorrentsView;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewSWTImpl;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.util.DLReferals;
import com.aelitis.azureus.util.DataSourceUtils;
import com.aelitis.azureus.util.PlayUtils;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadTypeComplete;
import org.gudy.azureus2.plugins.download.DownloadTypeIncomplete;
import org.gudy.azureus2.plugins.ui.tables.TableManager;

public class MyTorrentsView_Big
	extends MyTorrentsView
{
	private final int torrentFilterMode;

	public MyTorrentsView_Big(AzureusCore _azureus_core, int torrentFilterMode,
			TableColumnCore[] basicItems, Text txtFilter, Composite cCats) {
		this.torrentFilterMode = torrentFilterMode;
		this.txtFilter = txtFilter;
		this.cCategories = cCats;
		Class forDataSourceType;
		switch (torrentFilterMode) {
			case SBC_LibraryView.TORRENTS_COMPLETE:
				forDataSourceType = DownloadTypeComplete.class;
				break;

			case SBC_LibraryView.TORRENTS_INCOMPLETE:
				forDataSourceType = DownloadTypeIncomplete.class;
				break;
				
			case SBC_LibraryView.TORRENTS_ALL:
				forDataSourceType = Download.class;
				break;

			default:
				forDataSourceType = null;
				break;
		}
		init(
				_azureus_core,
				SB_Transfers.getTableIdFromFilterMode(torrentFilterMode, true),
				torrentFilterMode == SBC_LibraryView.TORRENTS_INCOMPLETE ? false : true,
				forDataSourceType, basicItems);
		//setForceHeaderVisible(true);
	}
	

	public boolean isOurDownloadManager(DownloadManager dm) {
		if (PlatformTorrentUtils.isAdvancedViewOnly(dm)) {
			return false;
		}
		
		if (torrentFilterMode == SBC_LibraryView.TORRENTS_ALL) {
			return isInCurrentCategory(dm);
		}
		
		return super.isOurDownloadManager(dm);
	}

	protected TableViewSWT createTableView(TableColumnCore[] basicItems) {
		String tableID;
		Class forDataSourceType;
		switch (torrentFilterMode) {
			case SBC_LibraryView.TORRENTS_COMPLETE:
				tableID = TableManager.TABLE_MYTORRENTS_COMPLETE_BIG;
				forDataSourceType = DownloadTypeComplete.class;
				break;

			case SBC_LibraryView.TORRENTS_INCOMPLETE:
				tableID = TableManager.TABLE_MYTORRENTS_INCOMPLETE_BIG;
				forDataSourceType = DownloadTypeIncomplete.class;
				break;
				
			case SBC_LibraryView.TORRENTS_ALL:
				tableID = TableManager.TABLE_MYTORRENTS_ALL_BIG;
				forDataSourceType = Download.class;
				break;

			default:
				tableID = "bad";
				forDataSourceType = null;
				break;
		}
		TableViewSWTImpl tv = new TableViewSWTImpl(forDataSourceType, tableID,
				"MyTorrentsView_Big", basicItems, "#", SWT.MULTI | SWT.FULL_SELECTION
						| SWT.VIRTUAL | SWT.BORDER);
		return tv;
	}
	
	// @see org.gudy.azureus2.ui.swt.views.MyTorrentsView#defaultSelected(com.aelitis.azureus.ui.common.table.TableRowCore[])
	public void defaultSelected(TableRowCore[] rows, int stateMask) {
		SBC_LibraryTableView.doDefaultClick(rows, stateMask, !isSeedingView);
	}

	protected int getRowDefaultHeight() {
		return 30;
	}

}
