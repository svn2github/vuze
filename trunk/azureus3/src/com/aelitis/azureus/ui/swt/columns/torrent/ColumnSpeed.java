/**
 * 
 */
package com.aelitis.azureus.ui.swt.columns.torrent;

import org.eclipse.swt.graphics.Image;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.TransferSpeedValidator;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerStats;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTGraphicImpl;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.ui.swt.utils.ImageLoaderFactory;

import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;

/**
 * @author TuxPaper
 * @created Jun 13, 2006
 *
 */
public class ColumnSpeed extends CoreTableColumn implements
		TableCellAddedListener
{
	private static boolean DEBUG = false;

	final static String[] ICON_NAMES = {
		"icon.speed.0",
		"icon.speed.1",
		"icon.speed.2",
		"icon.speed.3",
		"icon.speed.4"
	};

	static UISWTGraphic[] graphics;

	private static int width;

	static {
		graphics = new UISWTGraphic[ICON_NAMES.length];
		width = 0;
		for (int i = 0; i < graphics.length; i++) {
			Image img = ImageLoaderFactory.getInstance().getImage(ICON_NAMES[i]);
			graphics[i] = new UISWTGraphicImpl(img);
			width = Math.max(width, img.getBounds().width);
		}
	}

	/**
	 * 
	 */
	public ColumnSpeed(String sTableID) {
		super("SpeedGraphic", sTableID);
		// icon is 28, but column header needs more
		initializeAsGraphic(POSITION_LAST, width);
		setAlignment(ALIGN_CENTER);
	}

	public void cellAdded(TableCell cell) {
		new Cell(cell);
	}

	private class Cell implements TableCellRefreshListener
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

			int index = 0;

			int state = dm.getState();
			if (state == DownloadManager.STATE_DOWNLOADING
					|| state == DownloadManager.STATE_SEEDING) {

				boolean bComplete = dm.isDownloadComplete(false);
				long speed = bComplete ? dm.getStats().getDataSendRate()
						: dm.getStats().getDataReceiveRate();

				if (speed < 1024) {
					index = 0;
				} else {
					GlobalManager gm = dm.getGlobalManager();
					GlobalManagerStats stats = gm.getStats();
					int maxSpeed = COConfigurationManager.getIntParameter(bComplete
							? TransferSpeedValidator.getActiveUploadParameter(gm)
							: "Max Download Speed KBs", 0) * 1024;

					if (maxSpeed != 0) {
						if (speed >= maxSpeed) {
							index = 4;
						} else {
							index = (int) Math.round((double) ICON_NAMES.length * speed
									/ maxSpeed);
						}
						if (DEBUG) {
							System.out.println("speed=" + speed + ";ms=" + maxSpeed + ";"
									+ ((double) speed / maxSpeed) + ";" + index);
						}
					} else {
						int globalSpeed = bComplete ? stats.getDataSendRate()
								: stats.getDataReceiveRate();
						index = (int) Math.round((double) ICON_NAMES.length * speed
								/ globalSpeed);
						if (DEBUG) {
							System.out.println("speed=" + speed + ";gs=" + globalSpeed + ";"
									+ ((double) speed / globalSpeed) + ";" + index);
						}
					}
				}

				if (index >= ICON_NAMES.length) {
					index = ICON_NAMES.length - 1;
				}
			} else {
				// not active, use swarm
				if (dm.isDownloadComplete(false)) {
					index = -1;
				} else {
					TRTrackerScraperResponse scrape = dm.getTrackerScrapeResponse();
					if (scrape != null) {
						int seeds = scrape.getSeeds();
						int peers = scrape.getPeers();

						if (peers > 0) {
							index = (int) ((double) seeds / peers * ICON_NAMES.length);
						} else {
							index = seeds == 0 ? 0 : 4;
						}
					}
				}
				// Override
				index = -1;
			}

			if (!cell.setSortValue(index) && cell.isValid()) {
				return;
			}

			cell.setGraphic(index < 0 || index >= graphics.length ? null
					: graphics[index]);
		}
	}
}