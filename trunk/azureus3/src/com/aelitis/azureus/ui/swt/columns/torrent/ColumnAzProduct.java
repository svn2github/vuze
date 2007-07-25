/**
 * 
 */
package com.aelitis.azureus.ui.swt.columns.torrent;

import org.eclipse.swt.graphics.Image;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimeFormatter;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTGraphicImpl;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.swt.utils.ImageLoaderFactory;

import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Jun 13, 2006
 *
 */
public class ColumnAzProduct
	extends CoreTableColumn
	implements TableCellAddedListener, TableCellToolTipListener
{
	public static String COLUMN_ID = "AzProduct";

	private static UISWTGraphicImpl graphicProductAzureus;

	private static UISWTGraphicImpl graphicProductGlobe;

	private static int width;

	static {
		Image img = ImageLoaderFactory.getInstance().getImage(
				"column.azproduct.product");
		width = img.getBounds().width;
		graphicProductAzureus = new UISWTGraphicImpl(img);

		img = ImageLoaderFactory.getInstance().getImage("column.azproduct.globe");
		width = Math.max(width, img.getBounds().width);
		graphicProductGlobe = new UISWTGraphicImpl(img);
	}

	/**
	 * 
	 */
	public ColumnAzProduct(String sTableID) {
		super(COLUMN_ID, sTableID);
		initializeAsGraphic(POSITION_LAST, width);
		setWidthLimits(width, width);
		setAlignment(ALIGN_CENTER);
	}

	public void cellAdded(TableCell cell) {
		new Cell(cell);
	}

	private class Cell
		implements TableCellRefreshListener
	{

		public Cell(TableCell cell) {
			cell.addListeners(this);
			cell.setMarginWidth(0);
			cell.setMarginHeight(0);
		}

		public void refresh(TableCell cell) {
			DownloadManager dm = (DownloadManager) cell.getDataSource();
			if (dm == null) {
				return;
			}
			TOTorrent torrent = dm.getTorrent();
			boolean isContent = PlatformTorrentUtils.isContent(torrent, true);

			long sortVal = (isContent) ? 1 : 0;

			if (!cell.setSortValue(sortVal) && cell.isValid()) {
				return;
			}

			if (!cell.isShown()) {
				return;
			}

			cell.setGraphic(isContent ? graphicProductAzureus : graphicProductGlobe);
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellToolTipListener#cellHover(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellHover(TableCell cell) {
		if (Constants.isCVSVersion()) {
			DownloadManager dm = (DownloadManager) cell.getDataSource();
			if (dm == null) {
				return;
			}

			TOTorrent torrent = dm.getTorrent();
			long refreshOn = PlatformTorrentUtils.getMetaDataRefreshOn(torrent);
			long diff = (refreshOn - SystemTime.getCurrentTime()) / 1000;
			cell.setToolTip("Meta data auto refreshes in "
					+ TimeFormatter.format(diff));
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellToolTipListener#cellHoverComplete(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellHoverComplete(TableCell cell) {
	}
}
