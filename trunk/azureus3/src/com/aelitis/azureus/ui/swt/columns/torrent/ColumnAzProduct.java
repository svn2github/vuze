/**
 * 
 */
package com.aelitis.azureus.ui.swt.columns.torrent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimeFormatter;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.ui.swt.utils.ContentNetworkUI;
import com.aelitis.azureus.ui.swt.utils.ContentNetworkUI.ContentNetworkImageLoadedListener;
import com.aelitis.azureus.util.DataSourceUtils;

import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Jun 13, 2006
 *
 */
public class ColumnAzProduct
	extends CoreTableColumn
	implements TableCellAddedListener, TableCellToolTipListener,
	TableCellRefreshListener, TableCellSWTPaintListener
{
	private static final String NAME_NOCN = "";

	public static String COLUMN_ID = "AzProduct";

	private static Image imgProductGlobe;

	static {
		imgProductGlobe = ImageLoader.getInstance().getImage("column.azproduct.globe");
	}

	/**
	 * 
	 */
	public ColumnAzProduct(String sTableID) {
		super(COLUMN_ID, sTableID);
		initializeAsGraphic(150);
		setMinWidth(30);
		setAlignment(ALIGN_CENTER);
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener#cellAdded(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellAdded(TableCell cell) {
		cell.setMarginWidth(0);
		cell.setMarginHeight(0);
	}

	public void cellPaint(GC gc, final TableCellSWT cell) {
		Object ds = cell.getDataSource();

		ContentNetwork cn = DataSourceUtils.getContentNetwork(ds);

		long cnID = cn == null ? -1 : cn.getID();

		Image img = imgProductGlobe; 
		if (cnID > 0) {
			img = ContentNetworkUI.loadImage(cnID, new ContentNetworkImageLoadedListener() {
				public void contentNetworkImageLoaded(Long contentNetworkID, Image image, boolean wasReturned) {
					if (!wasReturned) {
						cell.invalidate();
					}
				}
			});
		}
		
		if (img == null) {
			return;
		}

		Rectangle imgBounds = img.getBounds();
		Rectangle cellBounds = cell.getBounds();
		
		Rectangle dstBounds;
		if (imgBounds.height > cellBounds.height) {
			int w = cellBounds.height * imgBounds.width / imgBounds.height;
			dstBounds = new Rectangle(imgBounds.x, imgBounds.y, w,
					cellBounds.height);
			gc.setAdvanced(true);
		} else {
			dstBounds = new Rectangle(imgBounds.x, imgBounds.y, imgBounds.width,
					imgBounds.height);
		}
	  
		if (cellBounds.width < 60) {
  		gc.drawImage(img, imgBounds.x, imgBounds.y, imgBounds.width,
					imgBounds.height, cellBounds.x
							+ ((cellBounds.width - dstBounds.width) / 2), cellBounds.y
							+ ((cellBounds.height - dstBounds.height) / 2), dstBounds.width,
					dstBounds.height);
		} else {
			gc.drawImage(img, imgBounds.x, imgBounds.y, imgBounds.width,
					imgBounds.height, cellBounds.x + 1, cellBounds.y
							+ ((cellBounds.height - dstBounds.height) / 2), dstBounds.width,
					dstBounds.height);
			cellBounds.x += imgBounds.width + 4;
			cellBounds.width -= imgBounds.width + 4;
			GCStringPrinter.printString(gc, cn == null ? NAME_NOCN : cn.getName(),
					cellBounds, true, false, SWT.LEFT);
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener#refresh(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void refresh(final TableCell cell) {
		Object ds = cell.getDataSource();

		ContentNetwork cn = DataSourceUtils.getContentNetwork(ds);

		long cnID = cn == null ? -1 : cn.getID();
		long sortVal = cnID;

		cell.setSortValue(sortVal);
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellToolTipListener#cellHover(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellHover(TableCell cell) {
		Object ds = cell.getDataSource();

		ContentNetwork cn = DataSourceUtils.getContentNetwork(ds);
		
		cell.setToolTip(cn == null ? null : cn.getName());

		if (false && Constants.isCVSVersion()) {
			if (!(ds instanceof DownloadManager)) {
				return;
			}
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
