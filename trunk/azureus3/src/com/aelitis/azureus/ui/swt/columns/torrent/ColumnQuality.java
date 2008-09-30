/**
 * 
 */
package com.aelitis.azureus.ui.swt.columns.torrent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.swt.utils.ImageLoader;
import com.aelitis.azureus.ui.swt.utils.ImageLoaderFactory;
import com.aelitis.azureus.util.DataSourceUtils;

import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;

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
	public static final String COLUMN_ID = "Quality";

	public Font font;

	private final int COLUMN_WIDTH = 50;

	/**
	 * 
	 */
	public ColumnQuality(String sTableID) {
		super(COLUMN_ID, sTableID);
		initializeAsGraphic(COLUMN_WIDTH);
		setAlignment(ALIGN_CENTER);
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
		Image img = ImageLoaderFactory.getInstance().getImage(
				"icon.quality." + quality);
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
	}

	public void refresh(TableCell cell) {
		TOTorrent torrent = DataSourceUtils.getTorrent(cell.getDataSource());
		String quality = PlatformTorrentUtils.getContentQuality(torrent);
		cell.setSortValue(quality);
	}
}
