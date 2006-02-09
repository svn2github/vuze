package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import java.net.URL;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerTrackerListener;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerResponse;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellDisposeListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellToolTipListener;

/**
 * Base cell class for cells listening to the tracker listener
 */
abstract class AbstractTrackerCell implements TableCellRefreshListener,
		TableCellToolTipListener, TableCellDisposeListener,
		DownloadManagerTrackerListener {

	TableCell cell;

	DownloadManager dm;

	/**
	 * Initialize
	 * 
	 * @param cell
	 */
	public AbstractTrackerCell(TableCell cell) {
		this.cell = cell;
		cell.addListeners(this);

		dm = (DownloadManager) cell.getDataSource();
		if (dm == null)
			return;
		dm.addTrackerListener(this);
	}

	public void announceResult(TRTrackerAnnouncerResponse response) {
		// Don't care about announce
	}

	public boolean checkScrapeResult(TRTrackerScraperResponse response) {
		if (response != null && response.isValid()) {
			// Exit if this scrape result is not from the tracker currently being used.
			DownloadManager dm = (DownloadManager) cell.getDataSource();
			if (dm == null || dm != this.dm)
				return false;

			TOTorrent	torrent = dm.getTorrent();
			
			if ( torrent == null ){
				return( false );
			}
			URL announceURL = torrent.getAnnounceURL();
			URL responseURL = response.getURL();
			if (announceURL != responseURL && announceURL != null
					&& responseURL != null
					&& !announceURL.toString().equals(responseURL.toString()))
				return false;
			

			if (cell != null)
				cell.invalidate();
			
			return true;
		}

		return false;
	}

	public void refresh(TableCell cell) {
		DownloadManager oldDM = dm;
		dm = (DownloadManager) cell.getDataSource();

		// datasource changed, change listener
		if (dm != oldDM) {
			if (oldDM != null)
				oldDM.removeTrackerListener(this);

			if (dm != null)
				dm.addTrackerListener(this);
		}
	}

	public void cellHover(TableCell cell) {
	}

	public void cellHoverComplete(TableCell cell) {
		cell.setToolTip(null);
	}

	public void dispose(TableCell cell) {
		if (dm != null)
			dm.removeTrackerListener(this);
		DownloadManager dm = (DownloadManager) cell.getDataSource();
		if (dm != null && dm != this.dm)
			dm.removeTrackerListener(this);
		dm = null;
		cell = null;
	}
}
