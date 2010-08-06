/**
 * 
 */
package com.aelitis.azureus.ui.swt.columns.torrent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimeFormatter;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.TorrentUtil;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.FilesViewMenuUtil;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.core.download.DownloadManagerEnhancer;
import com.aelitis.azureus.core.download.EnhancedDownloadManager;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.ui.swt.skin.SWTSkinFactory;
import com.aelitis.azureus.ui.swt.skin.SWTSkinProperties;
import com.aelitis.azureus.ui.swt.utils.ColorCache;
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
	implements TableCellAddedListener, TableCellMouseListener,
	TableCellRefreshListener, TableCellSWTPaintListener
{
	public static final Class DATASOURCE_TYPE = DownloadTypeIncomplete.class;

	public static final String COLUMN_ID = "ProgressETA";

	private static final int borderWidth = 1;

	private static final int COLUMN_WIDTH = 200;

	public static final long SHOW_ETA_AFTER_MS = 30000;

	private final static Object CLICK_KEY = new Object();

	private static Font fontText = null;

	Display display;

	private Color cBG;

	private Color cBorder;

	private Color cText;

	Color textColor;

	private Image imgArrowButton;

	private Image imgPriHi;

	private Image imgPriNormal;

	private Image imgPriStopped;

	private Image imgBGTorrent;

	private Image imgBGfile;

	private Color cTextDrop;

	/**
	 * 
	 */
	public ColumnProgressETA(String sTableID) {
		super(DATASOURCE_TYPE, COLUMN_ID, ALIGN_CENTER, COLUMN_WIDTH, sTableID);
		addDataSourceType(DiskManagerFileInfo.class);
		initializeAsGraphic(COLUMN_WIDTH);
		setAlignment(ALIGN_LEAD);
		setMinWidth(COLUMN_WIDTH);

		display = SWTThread.getInstance().getDisplay();

		SWTSkinProperties skinProperties = SWTSkinFactory.getInstance().getSkinProperties();
		cBG = skinProperties.getColor("color.progress.bg");
		if (cBG == null) {
			cBG = Colors.blues[Colors.BLUES_DARKEST];
		}
		cBorder = skinProperties.getColor("color.progress.border");
		if (cBorder == null) {
			cBorder = Colors.grey;
		}
		cText = skinProperties.getColor("color.progress.text");
		if (cText == null) {
			cText = Colors.black;
		}
		cTextDrop = skinProperties.getColor("color.progress.text.drop");

		ImageLoader imageLoader = ImageLoader.getInstance();
		imgArrowButton = imageLoader.getImage("image.fileprogress.arrowbtn");
		imgPriHi = imageLoader.getImage("image.fileprogress.pri.hi");
		imgPriNormal = imageLoader.getImage("image.fileprogress.pri.normal");
		imgPriStopped = imageLoader.getImage("image.fileprogress.pri.stopped");
		imgBGTorrent = imageLoader.getImage("image.progress.bg.torrent");
		imgBGfile = imageLoader.getImage("image.progress.bg.file");
	}

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			CAT_CONTENT,
			CAT_ESSENTIAL,
			CAT_TIME,
		});
		info.setProficiency(TableColumnInfo.PROFICIENCY_BEGINNER);
	}

	public void cellAdded(TableCell cell) {
		cell.setMarginHeight(3);
		cell.setMarginWidth(8);
	}

	public void cellMouseTrigger(TableCellMouseEvent event) {

		Object ds = event.cell.getDataSource();
		if (ds instanceof DiskManagerFileInfo) {
			fileInfoMouseTrigger(event);
			return;
		}

		DownloadManager dm = (DownloadManager) ds;
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

	private static final int MAX_PROGRESS_FILL_HEIGHT = 19;

	public void refresh(TableCell cell) {
		Object ds = cell.getDataSource();
		
		int percentDone = getPercentDone(ds);

		long sortValue = 0;

		if (ds instanceof DownloadManager) {
			DownloadManager dm = (DownloadManager) cell.getDataSource();

			long completedTime = dm.getDownloadState().getLongParameter(
					DownloadManagerState.PARAM_DOWNLOAD_COMPLETED_TIME);
			if (completedTime <= 0 || !dm.isDownloadComplete(false)) {
				sortValue = Long.MAX_VALUE - 10000 + percentDone;
			} else {
				sortValue = completedTime;
			}
		} else if (ds instanceof DiskManagerFileInfo) {
			DiskManagerFileInfo fileInfo = (DiskManagerFileInfo) ds;
			int st = fileInfo.getStorageType();
			if ((st == DiskManagerFileInfo.ST_COMPACT || st == DiskManagerFileInfo.ST_REORDER_COMPACT)
					&& fileInfo.isSkipped()) {
				sortValue = 1;
			} else if (fileInfo.isSkipped()) {
				sortValue = 2;
			} else if (fileInfo.getPriority() > 0) {

				int pri = fileInfo.getPriority();
				sortValue = 4;

				if (pri > 1) {
					sortValue += pri;
				}
			} else {
				sortValue = 3;
			}
			sortValue = (fileInfo.getDownloadManager().getState() * 10000)
				+ percentDone + sortValue;
		}

		long eta = getETA(cell);
		long speed = getSpeed(ds);

		//System.out.println("REFRESH " + sortValue + ";" + ds);
		boolean sortChanged = cell.setSortValue(sortValue);
		
		if (sortChanged) {
			UIFunctionsManagerSWT.getUIFunctionsSWT().refreshIconBar();
		}

		long lastETA = 0;
		long lastSpeed = 0;
		TableRow row = cell.getTableRow();
		if (row != null) {
			Object data = row.getData("lastETA");
			if (data instanceof Number) {
				lastETA = ((Number) data).longValue();
			}
			row.setData("lastETA", new Long(eta));
			data = row.getData("lastSpeed");
			if (data instanceof Number) {
				lastSpeed = ((Number) data).longValue();
			}
			row.setData("lastSpeed", new Long(speed));
		}

		if (!sortChanged && (lastETA != eta || lastSpeed != speed)) {
			cell.invalidate();
		}
	}

	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener#cellPaint(org.eclipse.swt.graphics.GC, org.gudy.azureus2.ui.swt.views.table.TableCellSWT)
	public void cellPaint(GC gc, TableCellSWT cell) {
		Object ds = cell.getDataSource();
		if (ds instanceof DiskManagerFileInfo) {
			fillInfoProgressETA(cell.getTableRowCore(), gc, (DiskManagerFileInfo) ds,
					cell.getBounds());
			return;
		}

		if( !(ds instanceof DownloadManager )){
			return;
		}
		
		DownloadManager dm = (DownloadManager) cell.getDataSource();

		int percentDone = getPercentDone(ds);
		long eta = getETA(cell);

		//Compute bounds ...
		int newWidth = cell.getWidth();
		if (newWidth <= 0) {
			return;
		}
		int newHeight = cell.getHeight();

		Color fgFirst = gc.getForeground();

		final Color fgOriginal = fgFirst;
		
		Rectangle cellBounds = cell.getBounds();

		int xStart = cellBounds.x;
		int yStart = cellBounds.y;

		int xRelProgressFillStart = borderWidth;
		int yRelProgressFillStart = borderWidth;
		int xRelProgressFillEnd = newWidth - xRelProgressFillStart - borderWidth;
		int yRelProgressFillEnd = yRelProgressFillStart + 13;
		boolean showSecondLine = yRelProgressFillEnd + 10 < newHeight;

		if (xRelProgressFillEnd < 10 || xRelProgressFillEnd < 10) {
			return;
		}
		String sETALine = null;

		// Draw Progress bar
		if (percentDone < 1000) {
			ImageLoader imageLoader = ImageLoader.getInstance();

			Rectangle boundsImgBG;

			if (!ImageLoader.isRealImage(imgBGTorrent)) {
				boundsImgBG = new Rectangle(0, 0, 0, 13);
			} else {
				boundsImgBG = imgBGTorrent.getBounds();
			}

			if (fontText == null) {
				fontText = FontUtils.getFontWithHeight(gc.getFont(), gc,
						boundsImgBG.height - 3);
			}

			if (!showSecondLine) {
				yRelProgressFillStart = (cellBounds.height / 2)
						- ((boundsImgBG.height + 2) / 2);
			}

			yRelProgressFillEnd = yRelProgressFillStart + boundsImgBG.height;

			int progressWidth = newWidth - 2;
			gc.setForeground(cBorder);
			gc.drawRectangle(xStart + xRelProgressFillStart - 1, yStart
					+ yRelProgressFillStart - 1, progressWidth + 1,
					boundsImgBG.height + 1);

			int pctWidth = (int) (percentDone * (progressWidth - 1) / 1000);
			gc.setBackground(cBG);
			gc.fillRectangle(xStart + xRelProgressFillStart, yStart
					+ yRelProgressFillStart, pctWidth, boundsImgBG.height);
			gc.setBackground(Colors.white);
			gc.fillRectangle(xStart + xRelProgressFillStart + pctWidth, yStart
					+ yRelProgressFillStart, progressWidth - pctWidth, boundsImgBG.height);

			if (boundsImgBG.width > 0) {
				gc.drawImage(imgBGTorrent, 0, 0, boundsImgBG.width, boundsImgBG.height,
						xStart + xRelProgressFillStart, yStart + yRelProgressFillStart,
						progressWidth - 2, boundsImgBG.height);
			}
		}

		if (sETALine == null ) {
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
					sETALine = DisplayFormatters.formatDownloadStatus(dm).toUpperCase();
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
				
		if (percentDone == 1000 || dm.isDownloadComplete(false)) {
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

			if ( getTableID() == TableManager.TABLE_MYTORRENTS_ALL_BIG ){
			
				if ( percentDone == 1000 ){
				
					gc.setForeground( fgOriginal  );
					
				}else{
					
					gc.setForeground(Colors.black);
				}
			}else{
				
				gc.setForeground(cText);
			}
			
			String s = MessageText.getString( "MyTorrents.column.ColumnProgressETA.compon", new String[]{ DisplayFormatters.formatDateShort(value)});
			
			if ( percentDone < 1000 ){
				
				s = DisplayFormatters.formatPercentFromThousands((int)percentDone) + " " + s; // " " + Character.toLowerCase(s.charAt(0))+s.substring(1);
			}
			GCStringPrinter.printString(gc, s, new Rectangle(xStart + 2, yStart,
					newWidth - 4, newHeight), true, false, SWT.WRAP);
		} else {
			long lSpeed = getSpeed(ds);
			String sSpeed = lSpeed <= 0 ? "" : " ("
					+ DisplayFormatters.formatByteCountToKiBEtcPerSec(lSpeed, true) + ")";

			String sPercent = DisplayFormatters.formatPercentFromThousands(percentDone);

			Rectangle area = new Rectangle(xStart + xRelProgressFillStart + 3, yStart
					+ yRelProgressFillStart, xRelProgressFillEnd - xRelProgressFillStart
					- 6, yRelProgressFillEnd - yRelProgressFillStart);
			GCStringPrinter sp = new GCStringPrinter(gc, sPercent + sSpeed, area,
					true, false, SWT.LEFT);
			if (cTextDrop != null) {
  			area.x++;
  			area.y++;
  			gc.setForeground(cTextDrop);
  			sp.printString();
  			area.x--;
  			area.y--;
			}
			gc.setForeground(cText);
			sp.printString();
			Point pctExtent = sp.getCalculatedSize();

			area.width -= (pctExtent.x + 3);
			area.x += (pctExtent.x + 3);

			if (!showSecondLine && sETALine != null) {
				boolean fit = GCStringPrinter.printString(gc, sETALine, area.intersection(cellBounds), true,
						false, SWT.RIGHT);
				cell.setToolTip(fit ? null:sETALine);
			}
		}

		gc.setFont(null);
	}

	private int getPercentDone(Object ds) {
		if (ds instanceof DownloadManager) {
			return ((DownloadManager) ds).getStats().getDownloadCompleted(true);
		} else if (ds instanceof DiskManagerFileInfo) {
			DiskManagerFileInfo fileInfo = (DiskManagerFileInfo) ds;
			long length = fileInfo.getLength();
			if (length == 0) {
				return 1000;
			}
			return (int) (fileInfo.getDownloaded() * 1000 / length);
		}
		return 0;
	}

	private long getETA(TableCell cell) {
		Object ds = cell.getDataSource();
		if (ds instanceof DiskManagerFileInfo) {
			return 0;
		}
		DownloadManager dm = (DownloadManager) cell.getDataSource();

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

	private long getSpeed(Object ds) {
		if (!(ds instanceof DownloadManager)) {
			return 0;
		}

		return ((DownloadManager) ds).getStats().getDataReceiveRate();
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

	private Font progressFont;

	private void fillInfoProgressETA(TableRowCore row, GC gc,
			DiskManagerFileInfo fileInfo, Rectangle cellArea) {
		long percent = 0;
		long bytesDownloaded = fileInfo.getDownloaded();
		long length = fileInfo.getLength();

		if (bytesDownloaded < 0) {

			return;

		} else if (length == 0) {

			percent = 1000;

		} else if (fileInfo.getLength() != 0) {

			percent = (1000 * bytesDownloaded) / length;
		}

		gc.setAdvanced(true);
		gc.setTextAntialias(SWT.ON);

		final int BUTTON_WIDTH = imgArrowButton.getBounds().width;
		final int HILOW_WIDTH = imgPriHi.getBounds().width;
		final int BUTTON_HEIGHT = imgArrowButton.getBounds().height;
		final int HILOW_HEIGHT = imgPriHi.getBounds().height;
		final int PADDING_X = 12;
		final int PADDING_TEXT = 5;
		final int PROGRESS_HEIGHT = imgBGfile.getBounds().height;
		final int PROGRESS_TO_HILOW_GAP = 3;
		final int HILOW_TO_BUTTON_GAP = 3;

		cellArea.width -= 3;

		int ofsX = PADDING_X;
		int ofsY = (cellArea.height / 2) - (PROGRESS_HEIGHT / 2) - 1;
		int progressWidth = cellArea.width - (ofsX * 2) - PROGRESS_TO_HILOW_GAP
				- HILOW_WIDTH - HILOW_TO_BUTTON_GAP - BUTTON_WIDTH;
		
		if ( progressWidth > 0 ){
			if (progressFont == null) {
				progressFont = FontUtils.getFontWithHeight(gc.getFont(), gc,
						PROGRESS_HEIGHT - 2);
			}
			gc.setFont(progressFont);
			gc.setForeground(ColorCache.getSchemedColor(display, fileInfo.isSkipped()
					? "#95a6b2" : "#88acc1"));
			gc.drawRectangle(cellArea.x + ofsX, cellArea.y + ofsY - 1, progressWidth,
					PROGRESS_HEIGHT + 1);
	
			int pctWidth = (int) (percent * (progressWidth - 1) / 1000);
			gc.setBackground(ColorCache.getSchemedColor(display, fileInfo.isSkipped()
					? "#a6bdce" : "#8ccfff"));
			gc.fillRectangle(cellArea.x + ofsX + 1, cellArea.y + ofsY, pctWidth,
					PROGRESS_HEIGHT);
			gc.setBackground(Colors.white);
			gc.fillRectangle(cellArea.x + ofsX + pctWidth + 1, cellArea.y + ofsY,
					progressWidth - pctWidth - 1, PROGRESS_HEIGHT);
	
			Rectangle boundsImgBG = imgBGfile.getBounds();
			gc.drawImage(imgBGfile, boundsImgBG.x, boundsImgBG.y, boundsImgBG.width,
					boundsImgBG.height, cellArea.x + ofsX + 1,
					cellArea.y + ofsY, progressWidth - 1, PROGRESS_HEIGHT);
		}
		
		Color colorText = ColorCache.getSchemedColor(display, fileInfo.isSkipped()
				? "#556875" : "#2678b1");

		Rectangle printBounds = new Rectangle(
				cellArea.x + PADDING_X + PADDING_TEXT, cellArea.y, progressWidth
						- (PADDING_TEXT * 2), cellArea.height);
		ofsY = (cellArea.height / 2) - (BUTTON_HEIGHT / 2) - 1;

		Rectangle buttonBounds = new Rectangle(cellArea.x + cellArea.width
				- BUTTON_WIDTH - PADDING_X, cellArea.y + ofsY, BUTTON_WIDTH,
				BUTTON_HEIGHT);
		row.setData("buttonBounds", buttonBounds);

		ofsY = (cellArea.height / 2) - (HILOW_HEIGHT / 2) - 1;
		Rectangle hilowBounds = new Rectangle(buttonBounds.x - HILOW_TO_BUTTON_GAP
				- HILOW_WIDTH, cellArea.y + ofsY, HILOW_WIDTH, HILOW_HEIGHT);
		row.setData("hilowBounds", hilowBounds);

		gc.setForeground(colorText);

		String s = DisplayFormatters.formatPercentFromThousands((int) percent);
		GCStringPrinter.printString(gc, s, printBounds, true, false, SWT.LEFT);

		//gc.setForeground(ColorCache.getRandomColor());

		String tmp = null;
		if (fileInfo.getDownloadManager().getState() == DownloadManager.STATE_STOPPED) {
			tmp = MessageText.getString("FileProgress.stopped");
		} else {

			int st = fileInfo.getStorageType();
			if ((st == DiskManagerFileInfo.ST_COMPACT || st == DiskManagerFileInfo.ST_REORDER_COMPACT)
					&& fileInfo.isSkipped()) {
				tmp = MessageText.getString("FileProgress.deleted");
			} else if (fileInfo.isSkipped()) {
				tmp = MessageText.getString("FileProgress.stopped");
			} else if (fileInfo.getPriority() > 0) {

				int pri = fileInfo.getPriority();

				if (pri > 1) {
					tmp = MessageText.getString("FileItem.high");
					tmp += " (" + pri + ")";
				}
			} else {
				//tmp = MessageText.getString("FileItem.normal");
			}
		}

		if (tmp != null) {
			GCStringPrinter.printString(gc, tmp.toUpperCase(), printBounds, false,
					false, SWT.RIGHT);
		}

		gc.drawImage(imgArrowButton, buttonBounds.x, buttonBounds.y);
		Image imgPriority = fileInfo.isSkipped() ? imgPriStopped
				: fileInfo.getPriority() > 0 ? imgPriHi : imgPriNormal;
		gc.drawImage(imgPriority, hilowBounds.x, hilowBounds.y);

		//System.out.println(cellArea + s + ";" + Debug.getCompressedStackTrace());
		// make relative to row, because mouse events are
		hilowBounds.y -= cellArea.y;
		hilowBounds.x -= cellArea.x;
		buttonBounds.x -= cellArea.x;
		buttonBounds.y -= cellArea.y;
	}

	public void fileInfoMouseTrigger(TableCellMouseEvent event) {
		if (event.eventType != TableRowMouseEvent.EVENT_MOUSEDOWN) {
			return;
		}
		final Object dataSource = ((TableRowCore) event.row).getDataSource(true);
		if (dataSource instanceof DiskManagerFileInfo) {
			final DiskManagerFileInfo fileInfo = (DiskManagerFileInfo) dataSource;
			Rectangle hilowBounds = (Rectangle) event.row.getData("hilowBounds");
			if (event.button == 1 && hilowBounds != null
					&& hilowBounds.contains(event.x, event.y)) {
				if (fileInfo.getPriority() > 0) {
					fileInfo.setPriority(0);
				} else {
					fileInfo.setPriority(1);
				}
				((TableRowCore) event.row).redraw();
			}

			Rectangle buttonBounds = (Rectangle) event.row.getData("buttonBounds");

			if (buttonBounds != null && buttonBounds.contains(event.x, event.y)) {
				Menu menu = new Menu(Display.getDefault().getActiveShell(), SWT.POP_UP);

				MenuItem itemHigh = new MenuItem(menu, SWT.RADIO);
				Messages.setLanguageText(itemHigh, "priority.high");
				itemHigh.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event event) {
						FilesViewMenuUtil.changePriority(FilesViewMenuUtil.PRIORITY_HIGH,
								new Object[] {
									dataSource
								});
					}
				});
				itemHigh.setSelection(fileInfo.getPriority() != 0); 

				MenuItem itemNormal = new MenuItem(menu, SWT.RADIO);
				Messages.setLanguageText(itemNormal, "priority.normal");
				itemNormal.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event event) {
						FilesViewMenuUtil.changePriority(FilesViewMenuUtil.PRIORITY_NORMAL,
								new Object[] {
									dataSource
								});
					}
				});
				itemNormal.setSelection(fileInfo.getPriority() == 0);

				new MenuItem(menu, SWT.SEPARATOR);
				
				boolean canStart = fileInfo.isSkipped() || fileInfo.getDownloadManager().getState() == DownloadManager.STATE_STOPPED;

				MenuItem itemStop = new MenuItem(menu, SWT.PUSH);
				Messages.setLanguageText(itemStop, "v3.MainWindow.button.stop");
				itemStop.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event event) {
						FilesViewMenuUtil.changePriority(
								FilesViewMenuUtil.PRIORITY_SKIPPED, new Object[] {
									dataSource
								});
					}
				});
				itemStop.setEnabled(!canStart);

				MenuItem itemStart = new MenuItem(menu, SWT.PUSH);
				Messages.setLanguageText(itemStart, "v3.MainWindow.button.start");
				itemStart.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event event) {
						if (fileInfo.getDownloadManager().getState() == DownloadManager.STATE_STOPPED) {
							TorrentUtil.queueDataSources(new Object[] { dataSource }, true);
						}
						
						FilesViewMenuUtil.changePriority(FilesViewMenuUtil.PRIORITY_NORMAL,
								new Object[] {
									dataSource
								});
					}
				});
				itemStart.setEnabled(canStart);

				new MenuItem(menu, SWT.SEPARATOR);

				MenuItem itemDelete = new MenuItem(menu, SWT.PUSH);
				Messages.setLanguageText(itemDelete, "v3.MainWindow.button.delete");
				itemDelete.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event event) {
						FilesViewMenuUtil.changePriority(FilesViewMenuUtil.PRIORITY_DELETE,
								new Object[] {
									dataSource
								});
					}
				});

				menu.setVisible(true);
				event.skipCoreFunctionality = true;
			}
			/*
			if (buttonBounds != null && buttonBounds.contains(event.x, event.y)) {
				int st = fileInfo.getStorageType();
				if ((st == DiskManagerFileInfo.ST_COMPACT || st == DiskManagerFileInfo.ST_REORDER_COMPACT)
						&& fileInfo.isSkipped()) {
					// deleted: Move to normal
					fileInfo.setPriority(0);
					fileInfo.setSkipped(false);
				} else if (fileInfo.isSkipped()) {
					// skipped: move to normal
					fileInfo.setPriority(0);
					fileInfo.setSkipped(false);
				} else if (fileInfo.getPriority() > 0) {

					// high: move to skipped
					fileInfo.setSkipped(true);
				} else {
					// normal: move to high
					fileInfo.setPriority(1);
				}
				//((TableRowCore) event.row).invalidate();
				((TableRowCore) event.row).redraw();
			}
			*/
		}
	}
}
