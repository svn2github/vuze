/**
 * 
 */
package com.aelitis.azureus.ui.swt.columns.torrent;

import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.TimeFormatter;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableRowSWT;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.core.download.DownloadManagerEnhancer;
import com.aelitis.azureus.core.download.EnhancedDownloadManager;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.swt.skin.SWTSkinFactory;
import com.aelitis.azureus.ui.swt.skin.SWTSkinProperties;
import com.aelitis.azureus.ui.swt.utils.ColorCache;
import com.aelitis.azureus.ui.swt.utils.ImageLoader;
import com.aelitis.azureus.ui.swt.utils.ImageLoaderFactory;
import com.aelitis.azureus.ui.swt.views.skin.TorrentListViewsUtils;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Jun 13, 2006
 *
 */
public class ColumnProgressETA
	extends CoreTableColumn
	implements TableCellAddedListener
{
	public static String COLUMN_ID = "ProgressETA";

	private static final int borderWidth = 1;

	private static final int COLUMN_WIDTH = 120;

	private static Font fontText = null;

	Display display;

	/**
	 * 
	 */
	public ColumnProgressETA(String sTableID) {
		super(COLUMN_ID, sTableID);
		initializeAsGraphic(POSITION_LAST, COLUMN_WIDTH);
		setAlignment(ALIGN_LEAD);
		setMinWidth(COLUMN_WIDTH);

		display = SWTThread.getInstance().getDisplay();
	}

	public void cellAdded(TableCell cell) {
		new Cell(cell);
	}

	private class Cell
		implements TableCellRefreshListener, TableCellDisposeListener,
		TableCellMouseListener, TableCellVisibilityListener
	{
		int lastPercentDone = 0;

		long lastETA;

		private boolean bMouseDowned = false;

		Rectangle areaPlay = null;

		Rectangle areaStream = null;

		public Cell(TableCell cell) {
			cell.addListeners(this);
			cell.setMarginHeight(1);
			cell.setFillCell(true);
		}

		public void dispose(TableCell cell) {
			disposeExisting(cell);
		}

		public void refresh(TableCell cell) {
			refresh(cell, false);
		}

		private void refresh(TableCell cell, boolean bForce) {
			DownloadManager dm = (DownloadManager) cell.getDataSource();
			if (dm == null) {
				return;
			}

			TOTorrent torrent = dm.getTorrent();
			EnhancedDownloadManager edm = null;
			boolean bCanBeProgressive = PlatformTorrentUtils.isContentProgressive(torrent)
					&& !TorrentListViewsUtils.canUseEMP(torrent);
			if (bCanBeProgressive) {
				edm = getEDM(dm);
				if (edm == null || !edm.supportsProgressiveMode()) {
					bCanBeProgressive = false;
				}
			}

			int percentDone = getPercentDone(cell);
			long eta = getETA(cell);

			long sortValue = (percentDone << 49) + (eta << 4) + getState(cell)
					+ (bCanBeProgressive ? 1 : 0);

			if (!cell.setSortValue(sortValue) && !bForce && cell.isValid()
					&& lastPercentDone == percentDone && lastETA == eta) {
				return;
			}

			if (!bForce && !cell.isShown()) {
				return;
			}

			//Compute bounds ...
			int newWidth = cell.getWidth();
			if (newWidth <= 0) {
				return;
			}
			int newHeight = cell.getHeight();

			int x1 = newWidth - borderWidth - 1;
			int progressX1 = bCanBeProgressive ? x1 - 55 : x1;
			int progressY1 = newHeight - borderWidth - 1 - 12;
			if (x1 < 10 || progressX1 < 10 || progressY1 < 3) {
				return;
			}

			SWTSkinProperties skinProperties = SWTSkinFactory.getInstance().getSkinProperties();
			Color cBG = skinProperties.getColor("color.progress.bg");
			if (cBG == null) {
				cBG = Colors.blues[Colors.BLUES_LIGHTEST];
			}
			Color cFG = skinProperties.getColor("color.progress.fg");
			if (cFG == null) {
				cFG = Colors.blues[Colors.BLUES_MIDLIGHT];
			}
			Color cBorder = skinProperties.getColor("color.progress.border");
			if (cBorder == null) {
				cBorder = Colors.grey;
			}
			Color cText = skinProperties.getColor("color.progress.text");
			if (cBorder == null) {
				cBorder = Colors.black;
			}

			int etaY0 = progressY1 + 1;

			lastPercentDone = percentDone;
			lastETA = eta;

			boolean bDrawProgressBar = true;
			Image image = ((TableCellSWT) cell).getGraphicSWT();
			GC gcImage;
			boolean bImageSizeChanged;
			Rectangle imageBounds;
			if (image == null) {
				bImageSizeChanged = true;
			} else {
				imageBounds = image.getBounds();
				bImageSizeChanged = imageBounds.width != newWidth
						|| imageBounds.height != newHeight;
			}

			if (false) {
				log(cell, "building: " + cell.isValid() + ";"
						+ (lastPercentDone == percentDone) + ";" + (lastETA == eta)
						+ "; oldimg=" + (image != null));
			}

			image = new Image(display, newWidth, newHeight);
			imageBounds = image.getBounds();

			gcImage = new GC(image);
			Color background = ((TableRowSWT) cell.getTableRow()).getBackground();
			if (background != null) {
				gcImage.setBackground(background);
				gcImage.fillRectangle(imageBounds);
			}

			String sETALine = null;
			long lSpeed = getSpeed(dm);
			String sSpeed = lSpeed <= 0 ? "" : "("
					+ DisplayFormatters.formatByteCountToKiBEtcPerSec(lSpeed, true) + ")";

			areaPlay = null;
			areaStream = null;
			if (bCanBeProgressive) {
				String id = "";
				if (edm.getProgressiveMode()) {
					if (edm.getProgressivePlayETA() <= 0) {
						bDrawProgressBar = false;
						id = "image.stream.play";
						Image img = ImageLoaderFactory.getInstance().getImage(id);
						if (ImageLoader.isRealImage(img)) {
							gcImage.drawImage(img, 2, 0);
							Rectangle bounds = img.getBounds();
							areaPlay = new Rectangle(2, 0, bounds.width, bounds.height);
						}
					}

					id = "image.stream.enabled";
				} else {
					id = "image.stream";
				}
				Image img = ImageLoaderFactory.getInstance().getImage(id);
				if (ImageLoader.isRealImage(img)) {
					Rectangle bounds = img.getBounds();
					areaStream = new Rectangle(newWidth - img.getBounds().width - 2, 0,
							bounds.width, bounds.height);

					gcImage.drawImage(img, areaStream.x, areaStream.y);
				}
				if (edm.getProgressiveMode()) {
					if (isStopped(cell)) {
						sETALine = MessageText.getString(
								"MyTorrents.column.ColumnProgressETA.2ndLine",
								new String[] {
									DisplayFormatters.formatDownloadStatus((DownloadManager) cell.getDataSource())
								});
					} else {
						long newETA = edm.getProgressivePlayETA();
						if (newETA <= 0) {
							sETALine = MessageText.getString(
									"MyTorrents.column.ColumnProgressETA.StreamReady");
						} else {
							String sETA = TimeFormatter.format(newETA);
							sETALine = MessageText.getString(
									"MyTorrents.column.ColumnProgressETA.PlayableIn",
									new String[] {
										sETA
									});
						}
					}
				}
			}

			if (bDrawProgressBar) {
				if (bImageSizeChanged || true) {
					// draw border
					gcImage.setForeground(cBorder);
					gcImage.drawRectangle(0, 0, progressX1, progressY1);
				} else {
					gcImage = new GC(image);
				}

				int limit = (progressX1 * percentDone) / 1000;

				gcImage.setBackground(cBG);
				gcImage.fillRectangle(1, 1, limit, progressY1 - 1);
				if (limit < progressX1) {
					gcImage.setBackground(cFG);
					gcImage.fillRectangle(limit + 1, 1, progressX1 - limit,
							progressY1 - 1);
				}

			}

			if (sETALine == null) {
				if (isStopped(cell)) {
					sETALine = DisplayFormatters.formatDownloadStatus((DownloadManager) cell.getDataSource());
				} else {
					String sETA = TimeFormatter.format(eta);
					sETALine = MessageText.getString(
							"MyTorrents.column.ColumnProgressETA.2ndLine", new String[] {
								sETA
							});
				}
			}

			if (fontText == null) {
				fontText = Utils.getFontWithHeight(gcImage.getFont(), gcImage, 12);
			}

			gcImage.setFont(fontText);
			int[] fg = cell.getForeground();
			gcImage.setForeground(ColorCache.getColor(display, fg[0], fg[1], fg[2]));
			gcImage.drawText(sETALine, 0, etaY0, true);
			Point textExtent = gcImage.textExtent(sETALine);
			cell.setToolTip(textExtent.x > newWidth ? sETALine : null);

			if (bDrawProgressBar) {
				gcImage.setForeground(cText);
				String sPercent = DisplayFormatters.formatPercentFromThousands(percentDone);
				gcImage.drawText(sPercent, 2, 2, true);
				gcImage.drawText(sSpeed, 50, 2, true);
			}

			gcImage.setFont(null);

			gcImage.dispose();

			disposeExisting(cell);

			((TableCellSWT) cell).setGraphic(image);
		}

		private int getPercentDone(TableCell cell) {
			DownloadManager dm = (DownloadManager) cell.getDataSource();
			if (dm == null) {
				return 0;
			}
			return dm.getStats().getDownloadCompleted(true);
		}

		private long getETA(TableCell cell) {
			DownloadManager dm = (DownloadManager) cell.getDataSource();
			if (dm == null) {
				return Constants.INFINITY_AS_INT;
			}
			return dm.getStats().getETA();
		}

		private int getState(TableCell cell) {
			DownloadManager dm = (DownloadManager) cell.getDataSource();
			if (dm == null) {
				return DownloadManager.STATE_ERROR;
			}
			return dm.getState();
		}

		private boolean isStopped(TableCell cell) {
			int state = getState(cell);
			return state == DownloadManager.STATE_QUEUED
					|| state == DownloadManager.STATE_STOPPED
					|| state == DownloadManager.STATE_STOPPING
					|| state == DownloadManager.STATE_ERROR;
		}

		private long getSpeed(DownloadManager dm) {
			if (dm == null) {
				return 0;
			}

			return dm.getStats().getDataReceiveRate();
		}

		public EnhancedDownloadManager getEDM(DownloadManager dm) {
			DownloadManagerEnhancer dmEnhancer = DownloadManagerEnhancer.getSingleton();
			if (dmEnhancer == null) {
				return null;
			}
			return dmEnhancer.getEnhancedDownload(dm);
		}

		public void cellMouseTrigger(TableCellMouseEvent event) {
			// only first button
			if (event.button != 1) {
				return;
			}

			if (event.eventType == TableCellMouseEvent.EVENT_MOUSEDOWN) {
				bMouseDowned = true;
				return;
			}

			if (event.eventType == TableCellMouseEvent.EVENT_MOUSEUP && bMouseDowned) {
				// no rating if row isn't selected yet
				if (!event.cell.getTableRow().isSelected()) {
					log(event.cell, "not selected");
					return;
				}

				DownloadManager dm = (DownloadManager) event.cell.getDataSource();
				if (dm == null) {
					return;
				}

				EnhancedDownloadManager edm = getEDM(dm);
				if (edm == null) {
					return;
				}

				if (areaPlay != null && areaPlay.contains(event.x, event.y)) {
					if (edm.getProgressiveMode() && edm.getProgressivePlayETA() <= 0) {
						Download pDL = (Download) ((TableRowCore) event.cell.getTableRow()).getDataSource(false);
						TorrentListViewsUtils.playViaMediaServer(pDL);
					}
				} else if (areaStream != null && areaStream.contains(event.x, event.y)) {
					flipProgressiveMode(dm);
				}
				refresh(event.cell, true);
			}
			bMouseDowned = false;
		}

		private void flipProgressiveMode(DownloadManager dm) {
			EnhancedDownloadManager edm = getEDM(dm);
			if (edm == null) {
				return;
			}

			edm.setProgressiveMode(!edm.getProgressiveMode());
		}

		private void disposeExisting(TableCell cell) {
			Graphic oldGraphic = cell.getGraphic();
			//log(cell, oldGraphic);
			if (oldGraphic instanceof UISWTGraphic) {
				Image oldImage = ((UISWTGraphic) oldGraphic).getImage();
				if (oldImage != null && !oldImage.isDisposed()) {
					//log(cell, "dispose");
					cell.setGraphic(null);
					oldImage.dispose();
				}
			}
		}

		public void cellVisibilityChanged(TableCell cell, int visibility) {
			if (visibility == TableCellVisibilityListener.VISIBILITY_HIDDEN) {
				//log(cell, "whoo, save");
				disposeExisting(cell);
			} else if (visibility == TableCellVisibilityListener.VISIBILITY_SHOWN) {
				//log(cell, "whoo, draw");
				refresh(cell, true);
			}
		}

		private void log(TableCell cell, String s) {
			System.out.println(((TableRowCore) cell.getTableRow()).getIndex() + ":"
					+ System.currentTimeMillis() + ": " + s);
		}
	}
}
