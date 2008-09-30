package com.aelitis.azureus.ui.swt.views.skin;

import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.ui.swt.views.MyTorrentsSuperView;
import org.gudy.azureus2.ui.swt.views.MyTorrentsView;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.swt.columns.utils.TableColumnCreatorV3;

public class MyTorrentsSuperView_Big
	extends MyTorrentsSuperView
{
	protected TableColumnCore[] getIncompleteColumns() {
		return TableColumnCreatorV3.createIncompleteDM(
				TableManager.TABLE_MYTORRENTS_INCOMPLETE_BIG, true);
	}

	protected TableColumnCore[] getCompleteColumns() {
		return TableColumnCreatorV3.createCompleteDM(
				TableManager.TABLE_MYTORRENTS_COMPLETE_BIG, true);
	}

	// @see org.gudy.azureus2.ui.swt.views.MyTorrentsSuperView#createTorrentView(com.aelitis.azureus.core.AzureusCore, java.lang.String, boolean, com.aelitis.azureus.ui.common.table.TableColumnCore[])
	protected MyTorrentsView createTorrentView(AzureusCore _azureus_core,
			String tableID, boolean isSeedingView, TableColumnCore[] columns) {
		return new MyTorrentsView_Big(_azureus_core, isSeedingView
				? SBC_LibraryView.TORRENTS_COMPLETE
				: SBC_LibraryView.TORRENTS_INCOMPLETE, columns);
	}

}
