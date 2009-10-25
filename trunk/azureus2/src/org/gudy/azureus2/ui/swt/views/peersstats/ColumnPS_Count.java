package org.gudy.azureus2.ui.swt.views.peersstats;

import org.gudy.azureus2.plugins.ui.tables.*;

public class ColumnPS_Count
	implements TableCellRefreshListener
{

	public static final String COLUMN_ID = "count";

	public ColumnPS_Count(TableColumn column) {
		column.initialize(TableColumn.ALIGN_TRAIL, TableColumn.POSITION_LAST, 150);
		column.addListeners(this);
		column.setType(TableColumn.TYPE_TEXT_ONLY);
	}

	public void refresh(TableCell cell) {
		PeersStatsDataSource ds = (PeersStatsDataSource) cell.getDataSource();
		if (ds == null) {
			return;
		}
		long val = ds.count;
		if (cell.setSortValue(val) || !cell.isValid()) {
			cell.setText(Long.toString(val));
		}
	}
}
