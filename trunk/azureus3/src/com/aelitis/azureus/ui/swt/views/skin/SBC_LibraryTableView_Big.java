package com.aelitis.azureus.ui.swt.views.skin;

import org.gudy.azureus2.plugins.ui.tables.TableManager;

import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.swt.columns.utils.TableColumnCreatorV3;

public class SBC_LibraryTableView_Big
	extends SBC_LibraryTableView
{

	public String getUpdateUIName() {
		return "SBC_LibraryTableView_Big";
	}

	public int getTableMode() {
		return SBC_LibraryView.MODE_BIGTABLE;
	}

	public boolean useBigTable() {
		return true;
	}

	public TableColumnCore[] getColumns() {
		if (torrentFilterMode == SBC_LibraryView.TORRENTS_COMPLETE) {
			return TableColumnCreatorV3.createCompleteDM(TableManager.TABLE_MYTORRENTS_COMPLETE_BIG);

		} else if (torrentFilterMode == SBC_LibraryView.TORRENTS_INCOMPLETE) {
			return TableColumnCreatorV3.createIncompleteDM(TableManager.TABLE_MYTORRENTS_INCOMPLETE_BIG);
		}
		return null;
	}
}
