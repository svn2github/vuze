/**
 * 
 */
package com.aelitis.azureus.ui.swt.columns.torrent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;

import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.util.DataSourceUtils;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Jun 13, 2006
 *
 */
public class ColumnQuality
	extends CoreTableColumn
	implements TableCellAddedListener, TableCellRefreshListener,
	TableCellSWTPaintListener
{
	public static final Class DATASOURCE_TYPE = Download.class;

	public static final String COLUMN_ID = "Quality";

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] { CAT_CONTENT });
	}

	public Font font;

	private final static int COLUMN_WIDTH = 50;

	/**
	 * 
	 */
	public ColumnQuality(String sTableID) {
		super(DATASOURCE_TYPE, COLUMN_ID, ALIGN_CENTER, COLUMN_WIDTH, sTableID);
		initializeAsGraphic(COLUMN_WIDTH);
	}

	public void cellAdded(TableCell cell) {
		cell.setMarginWidth(0);
		cell.setMarginHeight(0);
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener#cellPaint(org.eclipse.swt.graphics.GC, org.gudy.azureus2.ui.swt.views.table.TableCellSWT)
	public void cellPaint(GC gc, TableCellSWT cell) {
		Rectangle cellBounds = cell.getBounds();
		TOTorrent torrent = DataSourceUtils.getTorrent(cell.getDataSource());

		String quality = PlatformTorrentUtils.getContentQuality(torrent);
		String imageID = "icon.quality." + quality;
		Image img = ImageLoader.getInstance().getImage(imageID);
		try {
  		if (ImageLoader.isRealImage(img)) {
  			Rectangle imgBounds = img.getBounds();
  
  			if (imgBounds.height <= cellBounds.height) {
  				gc.drawImage(img, cellBounds.x
  						+ ((cellBounds.width - imgBounds.width) / 2), cellBounds.y
  						+ ((cellBounds.height - imgBounds.height) / 2));
  				return;
  			}
  		}
  		GCStringPrinter.printString(gc, quality, cellBounds, true, false,
  				SWT.CENTER);
		} finally {
			ImageLoader.getInstance().releaseImage(imageID);
		}
	}

	public void refresh(TableCell cell) {
		TOTorrent torrent = DataSourceUtils.getTorrent(cell.getDataSource());
		String quality = PlatformTorrentUtils.getContentQuality(torrent);
		cell.setSortValue(quality);
	}
}
