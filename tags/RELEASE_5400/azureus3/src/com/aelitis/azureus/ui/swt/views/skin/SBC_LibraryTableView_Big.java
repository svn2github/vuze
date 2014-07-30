package com.aelitis.azureus.ui.swt.views.skin;

import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.swt.columns.utils.TableColumnCreatorV3;

import org.gudy.azureus2.plugins.ui.tables.TableManager;

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
		TableColumnCore[] columns = null;
		if (torrentFilterMode == SBC_LibraryView.TORRENTS_COMPLETE) {
			columns = TableColumnCreatorV3.createCompleteDM(
					TableManager.TABLE_MYTORRENTS_COMPLETE_BIG, true);

		} else if (torrentFilterMode == SBC_LibraryView.TORRENTS_INCOMPLETE) {
			columns = TableColumnCreatorV3.createIncompleteDM(
					TableManager.TABLE_MYTORRENTS_INCOMPLETE_BIG, true);

		} else if (torrentFilterMode == SBC_LibraryView.TORRENTS_UNOPENED) {
			columns = TableColumnCreatorV3.createUnopenedDM(
					TableManager.TABLE_MYTORRENTS_UNOPENED_BIG, true);

		} else if (torrentFilterMode == SBC_LibraryView.TORRENTS_ALL) {
			columns = TableColumnCreatorV3.createAllDM(
					TableManager.TABLE_MYTORRENTS_ALL_BIG, true);
		}
		if (columns == null) {
			return null;
		}
		return columns;
	}
}
