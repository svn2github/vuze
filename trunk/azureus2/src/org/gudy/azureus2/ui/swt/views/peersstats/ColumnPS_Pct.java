package org.gudy.azureus2.ui.swt.views.peersstats;

import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;

public class ColumnPS_Pct
	implements TableCellRefreshListener
{

	public static final String COLUMN_ID = "count";

	public ColumnPS_Pct(TableColumn column) {
		column.initialize(TableColumn.ALIGN_TRAIL, TableColumn.POSITION_LAST, 150);
		column.addListeners(this);
		column.setType(TableColumn.TYPE_TEXT_ONLY);
		column.setRefreshInterval(TableColumn.INTERVAL_LIVE);
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
