/**
 * 
 */
package com.aelitis.azureus.ui.swt.columns.torrent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimeFormatter;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.core.download.DownloadManagerEnhancer;
import com.aelitis.azureus.core.download.EnhancedDownloadManager;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.ui.swt.skin.SWTSkinFactory;
import com.aelitis.azureus.ui.swt.skin.SWTSkinProperties;
import com.aelitis.azureus.ui.swt.utils.FontUtils;

import org.gudy.azureus2.plugins.download.DownloadTypeIncomplete;
import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Jun 13, 2006
 *
 */
public class ColumnProgressETA
	extends CoreTableColumn
	implements TableCellAddedListener, TableCellMouseListener
{
	public static final Class DATASOURCE_TYPE = DownloadTypeIncomplete.class;

	public static final String COLUMN_ID = "ProgressETA";

	private static final int borderWidth = 1;

	private static final int COLUMN_WIDTH = 150;

	public static final long SHOW_ETA_AFTER_MS = 30000;

	private final static Object CLICK_KEY = new Object();

	private static Font fontText = null;

	Display display;

	private Color cBG;

	private Color cFG;

	private Color cBorder;

	private Color cText;

	Color textColor;

	/**
	 * 
	 */
	public ColumnProgressETA(String sTableID) {
		super(DATASOURCE_TYPE, COLUMN_ID, ALIGN_LEAD, COLUMN_WIDTH, sTableID);
		initializeAsGraphic(COLUMN_WIDTH);
		setAlignment(ALIGN_LEAD);
		setMinWidth(COLUMN_WIDTH);

		display = SWTThread.getInstance().getDisplay();

		SWTSkinProperties skinProperties = SWTSkinFactory.getInstance().getSkinProperties();
		cBG = skinProperties.getColor("color.progress.bg");
		if (cBG == null) {
			cBG = Colors.blues[Colors.BLUES_DARKEST];
		}
		cFG = skinProperties.getColor("color.progress.fg");
		if (cFG == null) {
			cFG = Colors.blues[Colors.BLUES_LIGHTEST];
		}
		cBorder = skinProperties.getColor("color.progress.border");
		if (cBorder == null) {
			cBorder = Colors.grey;
		}
		cText = skinProperties.getColor("color.progress.text");
		if (cText == null) {
			cText = Colors.black;
		}
	}

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			CAT_CONTENT,
			CAT_ESSENTIAL,
			CAT_TIME,
		});
	}

	public void cellAdded(TableCell cell) {
		new Cell(cell);
	}

	public void cellMouseTrigger(TableCellMouseEvent event) {

		DownloadManager dm = (DownloadManager) event.cell.getDataSource();
		if (dm == null) {
			return;
		}

		String clickable = (String) dm.getUserData(CLICK_KEY);

		if (clickable == null) {

			return;
		}

		event.skipCoreFunctionality = true;

		if (event.eventType == TableCellMouseEvent.EVENT_MOUSEUP) {

			String url = UrlUtils.getURL(clickable);

			if (url != null) {

				Utils.launch(url);
			}
		}
	}

	private class Cell
		implements TableCellRefreshListener, TableCellSWTPaintListener
	{
		private static final int MAX_PROGRESS_FILL_HEIGHT = 19;

		int lastPercentDone = 0;

		long lastETA;

		public Cell(TableCell cell) {
			cell.addListeners(this);
			cell.setMarginHeight(3);
		}

		public void refresh(TableCell cell) {
			DownloadManager dm = (DownloadManager) cell.getDataSource();
			if (dm == null) {
				return;
			}

			int percentDone = getPercentDone(cell);

			long sortValue = 0;

			long completedTime = dm.getDownloadState().getLongParameter(
					DownloadManagerState.PARAM_DOWNLOAD_COMPLETED_TIME);
			if (completedTime <= 0 || !dm.isDownloadComplete(false)) {
				sortValue = Long.MAX_VALUE - 10000 + percentDone;
			} else {
				sortValue = completedTime;
			}

			long eta = getETA(cell);

			boolean sortChanged = cell.setSortValue(sortValue);
			if (!sortChanged && cell.isValid()
					&& lastPercentDone == percentDone && lastETA == eta) {
				return;
			}

			lastPercentDone = percentDone;
			lastETA = eta;

			if (sortChanged) {
				cell.invalidate();
			}
		}

		// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener#cellPaint(org.eclipse.swt.graphics.GC, org.gudy.azureus2.ui.swt.views.table.TableCellSWT)
		public void cellPaint(GC gc, TableCellSWT cell) {
			DownloadManager dm = (DownloadManager) cell.getDataSource();
			if (dm == null) {
				return;
			}
			int percentDone = getPercentDone(cell);
			long eta = getETA(cell);

			//Compute bounds ...
			int newWidth = cell.getWidth();
			if (newWidth <= 0) {
				return;
			}
			int newHeight = cell.getHeight();

			Color fgFirst = gc.getForeground();

			Rectangle cellBounds = cell.getBounds();

			int xStart = cellBounds.x + cell.getMarginWidth();
			int yStart = cellBounds.y + cell.getMarginHeight();

			int xRelProgressFillStart = borderWidth;
			int yRelProgressFillStart = borderWidth;
			int xRelProgressFillEnd = newWidth - xRelProgressFillStart - borderWidth;
			int yRelProgressFillEnd = yRelProgressFillStart + 13;
			boolean showSecondLine = yRelProgressFillEnd + 10 < newHeight;

			if (xRelProgressFillEnd < 10 || xRelProgressFillEnd < 10) {
				return;
			}

			boolean bDrawProgressBar = true;

			String sETALine = null;

			// Draw Progress bar
			if (bDrawProgressBar && percentDone < 1000) {
				ImageLoader imageLoader = ImageLoader.getInstance();
				Image imgEnd = imageLoader.getImage("dl_bar_end");
				Image img0 = imageLoader.getImage("dl_bar_0");
				Image img1 = imageLoader.getImage("dl_bar_1");

				gc.drawImage(imgEnd, xStart, yStart + yRelProgressFillStart);
				gc.drawImage(imgEnd, xStart + xRelProgressFillEnd
						- xRelProgressFillStart + 1, yStart + yRelProgressFillStart);

				int limit = ((xRelProgressFillEnd - xRelProgressFillStart) * percentDone) / 1000;

				gc.drawImage(img1, 0, 0, 1, 13, xStart + xRelProgressFillStart, yStart
						+ yRelProgressFillStart, limit, 13);

				if (limit < xRelProgressFillEnd) {
					gc.drawImage(img0, 0, 0, 1, 13, xStart + limit + 1, yStart
							+ yRelProgressFillStart, xRelProgressFillEnd - limit - 1, 13);
				}

				imageLoader.releaseImage("dl_bar_end");
				imageLoader.releaseImage("dl_bar_0");
				imageLoader.releaseImage("dl_bar_1");
			}

			if (sETALine == null) {
				if (dm.isUnauthorisedOnTracker()) {
					sETALine = dm.getTrackerStatus();
					// fgFirst = Colors.colorError;	pftt, no colours allowed apparently
				} else {
					if (dm.isDownloadComplete(true)) {
						//sETALine = DisplayFormatters.formatByteCountToKiBEtc(dm.getSize());
					} else if (eta > 0) {
						String sETA = TimeFormatter.format(eta);
						sETALine = MessageText.getString(
								"MyTorrents.column.ColumnProgressETA.2ndLine", new String[] {
									sETA
								});
					} else {
						sETALine = DisplayFormatters.formatDownloadStatus(dm);
					}
				}

				int cursor_id;

				if (sETALine != null && sETALine.indexOf("http://") == -1) {

					dm.setUserData(CLICK_KEY, null);

					cursor_id = SWT.CURSOR_ARROW;

				} else {

					dm.setUserData(CLICK_KEY, sETALine);

					cursor_id = SWT.CURSOR_HAND;

					if (!cell.getTableRow().isSelected()) {

						fgFirst = Colors.blue;
					}
				}

				((TableCellSWT) cell).setCursorID(cursor_id);
			}

			if (fontText == null) {
				fontText = FontUtils.getFontWithHeight(gc.getFont(), gc, 11);
			}

			gc.setTextAntialias(SWT.ON);
			gc.setFont(fontText);
			if (showSecondLine && sETALine != null) {
				gc.setForeground(fgFirst);
				boolean over = GCStringPrinter.printString(gc, sETALine, new Rectangle(
						xStart + 2, yStart + yRelProgressFillEnd, xRelProgressFillEnd,
						newHeight - yRelProgressFillEnd), true, false, SWT.CENTER);
				cell.setToolTip(over ? sETALine : null);
			}
			int middleY = (yRelProgressFillEnd - 12) / 2;
			if (percentDone == 1000) {
				gc.setForeground(fgFirst);
				long value;
				long completedTime = dm.getDownloadState().getLongParameter(
						DownloadManagerState.PARAM_DOWNLOAD_COMPLETED_TIME);
				if (completedTime <= 0) {
					value = dm.getDownloadState().getLongParameter(
							DownloadManagerState.PARAM_DOWNLOAD_ADDED_TIME);
				} else {
					value = completedTime;
				}

				String s = "Completed on " + DisplayFormatters.formatDateShort(value);
				GCStringPrinter.printString(gc, s, new Rectangle(xStart + 2, yStart,
						newWidth - 4, newHeight), true, false, SWT.WRAP);
			} else if (bDrawProgressBar) {
				long lSpeed = getSpeed(dm);
				String sSpeed = lSpeed <= 0 ? "" : "("
						+ DisplayFormatters.formatByteCountToKiBEtcPerSec(lSpeed, true)
						+ ")";

				gc.setForeground(cText);
				String sPercent = DisplayFormatters.formatPercentFromThousands(percentDone);
				Point pctExtent = gc.textExtent(sPercent, SWT.DRAW_TRANSPARENT);
				Point spdExtent = gc.textExtent(sSpeed, SWT.DRAW_TRANSPARENT);
				gc.drawText(sSpeed, xStart + pctExtent.x + 4, yStart + yRelProgressFillStart + 1,
						true);
				gc.drawText(sPercent, xStart + 2, yStart + yRelProgressFillStart + 1,
						true);
				
				if (!showSecondLine && sETALine != null) {
					boolean over = GCStringPrinter.printString(gc, sETALine, new Rectangle(
							xStart + pctExtent.x + spdExtent.x + 10, yStart + yRelProgressFillStart + 1, 
							xRelProgressFillEnd - (pctExtent.x + spdExtent.x + 12),
							newHeight - yRelProgressFillEnd), true, false, SWT.RIGHT);
					cell.setToolTip(over ? sETALine : null);
				}
			}

			gc.setFont(null);
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
				return 0;
			}
			long diff = SystemTime.getCurrentTime() - dm.getStats().getTimeStarted();
			if (diff > SHOW_ETA_AFTER_MS) {
				return dm.getStats().getETA();
			}
			return 0;
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

		private void log(TableCell cell, String s) {
			System.out.println(((TableRowCore) cell.getTableRow()).getIndex() + ":"
					+ System.currentTimeMillis() + ": " + s);
		}
	}
}
