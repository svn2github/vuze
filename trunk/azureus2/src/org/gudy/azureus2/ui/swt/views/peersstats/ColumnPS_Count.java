package org.gudy.azureus2.ui.swt.views.peersstats;

import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;

public class ColumnPS_Count
	implements TableCellRefreshListener
{

	public static final String COLUMN_ID = "count";

	public ColumnPS_Count(TableColumn column) {
		column.initialize(TableColumn.ALIGN_LEAD, TableColumn.POSITION_INVISIBLE,
				215);
		column.addListeners(this);
		column.setType(TableColumn.TYPE_TEXT_ONLY);
	}

	public void refresh(TableCell cell) {
		PeersStatsDataSource ds = (PeersStatsDataSource) cell.getDataSource();
		if (ds == null) {
			return;
		}
		cell.setSortValue(ds.count);
		cell.setText("" + ds.count);
	}
}
