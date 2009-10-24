package org.gudy.azureus2.ui.swt.views.peersstats;

import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;

public class ColumnPS_Sent
	implements TableCellRefreshListener
{

	public static final String COLUMN_ID = "sent";

	public ColumnPS_Sent(TableColumn column) {
		column.initialize(TableColumn.ALIGN_LEAD, TableColumn.POSITION_LAST, 150);
		column.addListeners(this);
		column.setType(TableColumn.TYPE_TEXT_ONLY);
	}

	public void refresh(TableCell cell) {
		PeersStatsDataSource ds = (PeersStatsDataSource) cell.getDataSource();
		if (ds == null) {
			return;
		}
		cell.setSortValue(ds.bytesSent);
		cell.setText(DisplayFormatters.formatByteCountToKiBEtc(ds.bytesSent));
	}
}
