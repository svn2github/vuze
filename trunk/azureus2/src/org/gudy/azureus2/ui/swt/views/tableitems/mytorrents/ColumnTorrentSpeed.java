/**
 * 
 */
package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

/**
 * @author TuxPaper
 * @created Jul 11, 2010
 *
 */
public class ColumnTorrentSpeed
	extends CoreTableColumn
	implements TableCellRefreshListener
{
	public static final Class DATASOURCE_TYPE = Download.class;

	public static final String COLUMN_ID = "torrentspeed";

	public ColumnTorrentSpeed(String tableID) {
		super(COLUMN_ID, 80, tableID);
		setAlignment(ALIGN_TRAIL);
		setType(TableColumn.TYPE_TEXT);
    setRefreshInterval(INTERVAL_LIVE);
    setUseCoreDataSource(false);
	}
	
	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			CAT_ESSENTIAL,
			CAT_BYTES,
		});
	}

  public void refresh(TableCell cell) {
  	if (!(cell.getDataSource() instanceof Download)) {
  		return;
  	}
    Download dm = (Download)cell.getDataSource();
    long value;
    long sortValue;
    String prefix = "";
    if (dm == null) {
    	sortValue = value = 0;
    } else {
    	int iState;
      iState = dm.getState();
      if (iState == Download.ST_DOWNLOADING) {
      	value = dm.getStats().getDownloadAverage();
      	prefix = "\u21D3 ";
      } else if (iState == Download.ST_SEEDING) {
      	value = dm.getStats().getUploadAverage();
      	prefix = "\u21D1 ";
      } else {
      	value = 0;
      }
      sortValue = (value << 4) | iState;
    }
    
    if (cell.setSortValue(sortValue) || !cell.isValid()) {
    	cell.setText(value > 0 ? prefix + DisplayFormatters.formatByteCountToKiBEtcPerSec(value) : "");
    }
  }

}
