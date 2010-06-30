/*
 * File    : NameItem.java
 * Created : 24 nov. 2003
 * By      : Olivier
 *
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

package com.aelitis.azureus.ui.swt.columns.torrent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.UIInputReceiver;
import org.gudy.azureus2.plugins.ui.UIInputReceiverListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemFillListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.SimpleTextEntryWindow;
import org.gudy.azureus2.ui.swt.debug.ObfusticateCellText;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.ui.common.table.TableCellCore;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.ui.swt.utils.TorrentUIUtilsV3;
import com.aelitis.azureus.ui.swt.utils.TorrentUIUtilsV3.ContentImageLoadedListener;

/** Torrent name cell for My Torrents.
 *
 * @author Olivier
 * @author TuxPaper (2004/Apr/17: modified to TableCellAdapter)
 */
public class ColumnThumbAndName
	extends CoreTableColumn
	implements TableCellLightRefreshListener, ObfusticateCellText,
	TableCellDisposeListener, TableCellSWTPaintListener,
	TableCellClipboardListener, TableCellMouseMoveListener
{
	public static final Class DATASOURCE_TYPE = Download.class;

	public static final String COLUMN_ID = "name";

	private static final String ID_EXPANDOHITAREA = "expandoHitArea";

	private boolean showIcon;

	private boolean wrapText;

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			CAT_ESSENTIAL,
			CAT_CONTENT
		});
	}

	/**
	 * 
	 * @param sTableID
	 */
	public ColumnThumbAndName(String sTableID) {
		super(DATASOURCE_TYPE, COLUMN_ID, ALIGN_LEAD, 250, sTableID);
		addDataSourceType(org.gudy.azureus2.plugins.disk.DiskManagerFileInfo.class);
		setObfustication(true);
		setRefreshInterval(INTERVAL_LIVE);
		initializeAsGraphic(250);
		setMinWidth(100);

		TableContextMenuItem menuItem = addContextMenuItem("MyTorrentsView.menu.rename.displayed");
		menuItem.addMultiListener(new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				if (target == null) {
					return;
				}
				Object[] o = (Object[]) target;
				for (Object object : o) {
					if (object instanceof DownloadManager) {
						final DownloadManager dm = (DownloadManager) object;
						String msg_key_prefix = "MyTorrentsView.menu.rename.displayed.enter.";

						SimpleTextEntryWindow entryWindow = new SimpleTextEntryWindow(
								msg_key_prefix + "title", msg_key_prefix + "message");
						entryWindow.setPreenteredText(dm.getDisplayName(), false);
						entryWindow.prompt(new UIInputReceiverListener() {
							public void UIInputReceiverClosed(UIInputReceiver entryWindow) {
								if (!entryWindow.hasSubmittedInput()) {
									return;
								}
								String value = entryWindow.getSubmittedInput();
								if (value != null && value.length() > 0) {
									dm.getDownloadState().setDisplayName(value);
								}
							}
						});
					}
				}
			}
		});

		TableContextMenuItem menuShowIcon = addContextMenuItem(
				"ConfigView.section.style.showProgramIcon", MENU_STYLE_HEADER);
		menuShowIcon.setStyle(TableContextMenuItem.STYLE_CHECK);
		menuShowIcon.addFillListener(new MenuItemFillListener() {
			public void menuWillBeShown(MenuItem menu, Object data) {
				menu.setData(new Boolean(showIcon));
			}
		});
		final String CFG_SHOWPROGRAMICON = "NameColumn.showProgramIcon."
				+ getTableID();
		menuShowIcon.addMultiListener(new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				COConfigurationManager.setParameter(CFG_SHOWPROGRAMICON,
						((Boolean) menu.getData()).booleanValue());
			}
		});

		TableContextMenuItem menuWrapText = addContextMenuItem("label.wrap.text",
				MENU_STYLE_HEADER);
		menuWrapText.setStyle(TableContextMenuItem.STYLE_CHECK);
		menuWrapText.addFillListener(new MenuItemFillListener() {
			public void menuWillBeShown(MenuItem menu, Object data) {
				menu.setData(new Boolean(wrapText));
			}
		});
		final String CFG_WRAP_TEXT = "NameColumn.wrapText." + getTableID();
		menuWrapText.addMultiListener(new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				COConfigurationManager.setParameter(CFG_WRAP_TEXT,
						((Boolean) menu.getData()).booleanValue());
			}
		});

		COConfigurationManager.addAndFireParameterListener(CFG_WRAP_TEXT,
				new ParameterListener() {
					public void parameterChanged(String parameterName) {
						setWrapText(COConfigurationManager.getBooleanParameter(CFG_WRAP_TEXT));
					}
				});
		COConfigurationManager.addAndFireParameterListener(CFG_SHOWPROGRAMICON,
				new ParameterListener() {
					public void parameterChanged(String parameterName) {
						setShowIcon(COConfigurationManager.getBooleanParameter(
								CFG_SHOWPROGRAMICON,
								COConfigurationManager.getBooleanParameter("NameColumn.showProgramIcon")));
					}
				});
	}

	public void refresh(TableCell cell) {
		refresh(cell, false);
	}

	public void refresh(TableCell cell, boolean sortOnlyRefresh) {
		String name = null;
		Object ds = cell.getDataSource();
		if (ds instanceof DiskManagerFileInfo) {
			return;
		}
		DownloadManager dm = (DownloadManager) ds;
		if (dm != null) {
			name = dm.getDisplayName();
		}
		if (name == null) {
			name = "";
		}

		cell.setSortValue(name);
	}

	public void cellPaint(GC gc, final TableCellSWT cell) {
		Object ds = cell.getDataSource();
		if (ds instanceof DiskManagerFileInfo) {
			cellPaintFileInfo(gc, cell, (DiskManagerFileInfo) ds);
			return;
		}

		Rectangle cellBounds = cell.getBounds();

		int textX = cellBounds.x;

		TableRowCore rowCore = cell.getTableRowCore();
		if (rowCore != null) {
			int numSubItems = rowCore.getSubItemCount();
			int paddingX = 3;
			int width = 7;
			if (numSubItems > 1) {
				int middleY = cellBounds.y + (cellBounds.height / 2) - 1;
				int startX = cellBounds.x + paddingX;
				int halfHeight = 2;
				Color bg = gc.getBackground();
				gc.setBackground(gc.getForeground());
				gc.setAntialias(SWT.ON);
				gc.setAdvanced(true);
				if (rowCore.isExpanded()) {
					gc.fillPolygon(new int[] {
						startX,
						middleY - halfHeight,
						startX + width,
						middleY - halfHeight,
						startX + (width / 2),
						middleY + (halfHeight * 2) + 1
					});
				} else {
					gc.fillPolygon(new int[] {
						startX,
						middleY - halfHeight,
						startX + width,
						middleY + halfHeight,
						startX,
						middleY + (halfHeight * 2) + 1
					});
				}
				gc.setBackground(bg);
				Rectangle hitArea = new Rectangle(paddingX, middleY - halfHeight
						- cellBounds.y, width, (halfHeight * 4) + 1);
				rowCore.setData(ID_EXPANDOHITAREA, hitArea);

			}
			if (numSubItems > 0) {
				cellBounds.x += paddingX * 2 + width;
				cellBounds.width -= paddingX * 2 + width;
			}
		}

		if (!showIcon) {
			cellBounds.x += 2;
			cellBounds.width -= 4;
			cellPaintName(cell, gc, cellBounds, cellBounds.x);
			return;
		}

		Image[] imgThumbnail = TorrentUIUtilsV3.getContentImage(ds,
				cellBounds.height >= 20, new ContentImageLoadedListener() {
					public void contentImageLoaded(Image image, boolean wasReturned) {
						if (!wasReturned) {
							// this may be triggered many times, so only invalidate and don't
							// force a refresh()
							cell.invalidate();
						}
					}
				});

		if (imgThumbnail != null && ImageLoader.isRealImage(imgThumbnail[0])) {
			try {

				if (cellBounds.height > 30) {
					cellBounds.y += 1;
					cellBounds.height -= 3;
				}
				Rectangle imgBounds = imgThumbnail[0].getBounds();

				int dstWidth;
				int dstHeight;
				if (imgBounds.height > cellBounds.height) {
					dstHeight = cellBounds.height;
					dstWidth = imgBounds.width * cellBounds.height / imgBounds.height;
				} else if (imgBounds.width > cellBounds.width) {
					dstWidth = cellBounds.width - 4;
					dstHeight = imgBounds.height * cellBounds.width / imgBounds.width;
				} else {
					dstWidth = imgBounds.width;
					dstHeight = imgBounds.height;
				}

				if (cellBounds.height <= 18) {
					dstWidth = Math.min(dstWidth, cellBounds.height);
					dstHeight = Math.min(dstHeight, cellBounds.height);
					if (imgBounds.width > 16) {
						cellBounds.y++;
						dstHeight -= 2;
					}
				}

				try {
					gc.setAdvanced(true);
					gc.setInterpolation(SWT.HIGH);
				} catch (Exception e) {
				}
				int x = cellBounds.x;
				textX = x + dstWidth + 3;
				int minWidth = dstHeight * 7 / 4;
				int imgPad = 0;
				if (dstHeight > 25) {
					if (dstWidth < minWidth) {
						imgPad = ((minWidth - dstWidth + 1) / 2);
						x = cellBounds.x + imgPad;
						textX = cellBounds.x + minWidth + 3;
					}
				}
				if (cellBounds.width - dstWidth - (imgPad * 2) < 100 && dstHeight > 18) {
					dstWidth = Math.min(32, dstHeight);
					x = cellBounds.x + ((32 - dstWidth + 1) / 2);
					dstHeight = imgBounds.height * dstWidth / imgBounds.width;
					textX = cellBounds.x + dstWidth + 3;
				}
				int y = cellBounds.y + ((cellBounds.height - dstHeight + 1) / 2);
				if (dstWidth > 0 && dstHeight > 0 && !imgBounds.isEmpty()) {
					Rectangle dst = new Rectangle(x, y, dstWidth, dstHeight);
					Rectangle lastClipping = gc.getClipping();
					try {
						gc.setClipping(cellBounds);

						for (int i = 0; i < imgThumbnail.length; i++) {
							Image image = imgThumbnail[i];
							if (image == null || image.isDisposed()) {
								continue;
							}
							Rectangle srcBounds = image.getBounds();
							if (i == 0) {
								int w = dstWidth;
								int h = dstHeight;
								if (imgThumbnail.length > 1) {
									w = w * 9 / 10;
									h = h * 9 / 10;
								}
								gc.drawImage(image, srcBounds.x, srcBounds.y, srcBounds.width,
										srcBounds.height, x, y, w, h);
							} else {
								int w = dstWidth * 3 / 8;
								int h = dstHeight * 3 / 8;
								gc.drawImage(image, srcBounds.x, srcBounds.y, srcBounds.width,
										srcBounds.height, x + dstWidth - w, y + dstHeight - h, w, h);
							}
						}
					} catch (Exception e) {
						Debug.out(e);
					} finally {
						gc.setClipping(lastClipping);
					}
				}

				TorrentUIUtilsV3.releaseContentImage(ds);
			} catch (Throwable t) {
				Debug.out(t);
			}
		}

		cellPaintName(cell, gc, cellBounds, textX);
	}

	private void cellPaintFileInfo(GC gc, TableCellSWT cell,
			DiskManagerFileInfo fileInfo) {
		Rectangle cellArea = cell.getBounds();
		//System.out.println(cellArea);
		int padding = 10 + 20 + (showIcon ? cellArea.height : 0);
		cellArea.x += padding;
		cellArea.width -= padding;
		GCStringPrinter.printString(gc, fileInfo.getFile(true).getName(), cellArea,
				true, false, SWT.LEFT);
	}

	private void cellPaintName(TableCell cell, GC gc, Rectangle cellBounds,
			int textX) {
		String name = null;
		Object ds = cell.getDataSource();
		if (ds instanceof DiskManagerFileInfo) {
			return;
		}
		DownloadManager dm = (DownloadManager) ds;
		if (dm != null)
			name = dm.getDisplayName();
		if (name == null)
			name = "";

		GCStringPrinter.printString(gc, name, new Rectangle(textX, cellBounds.y,
				cellBounds.x + cellBounds.width - textX, cellBounds.height), true,
				true, wrapText ? SWT.WRAP : SWT.NONE);
	}

	public String getObfusticatedText(TableCell cell) {
		String name = null;
		Object ds = cell.getDataSource();
		if (ds instanceof DiskManagerFileInfo) {
			return null;
		}
		DownloadManager dm = (DownloadManager) ds;
		if (dm != null) {
			name = dm.toString();
			int i = name.indexOf('#');
			if (i > 0) {
				name = name.substring(i + 1);
			}
		}

		if (name == null)
			name = "";
		return name;
	}

	public void dispose(TableCell cell) {

	}

	/**
	 * @param showIcon the showIcon to set
	 */
	public void setShowIcon(boolean showIcon) {
		this.showIcon = showIcon;
		invalidateCells();
	}

	public void setWrapText(boolean wrap) {
		this.wrapText = wrap;
		invalidateCells();
	}

	/**
	 * @return the showIcon
	 */
	public boolean isShowIcon() {
		return showIcon;
	}

	public String getClipboardText(TableCell cell) {
		String name = null;
		Object ds = cell.getDataSource();
		if (ds instanceof DiskManagerFileInfo) {
			return null;
		}
		DownloadManager dm = (DownloadManager) ds;
		if (dm != null)
			name = dm.getDisplayName();
		if (name == null)
			name = "";
		return name;
	}

	public void cellMouseTrigger(TableCellMouseEvent event) {
		if (event.eventType == TableCellMouseEvent.EVENT_MOUSEMOVE
				|| event.eventType == TableRowMouseEvent.EVENT_MOUSEDOWN) {
			TableRow row = event.cell.getTableRow();
			if (row == null) {
				return;
			}
			Object data = row.getData(ID_EXPANDOHITAREA);
			if (data instanceof Rectangle) {
				Rectangle hitArea = (Rectangle) data;
				boolean inExpando = hitArea.contains(event.x, event.y);

				if (event.eventType == TableCellMouseEvent.EVENT_MOUSEMOVE) {
					((TableCellCore) event.cell).setCursorID(inExpando ? SWT.CURSOR_HAND
							: SWT.CURSOR_ARROW);
				} else if (inExpando) { // mousedown
					if (row instanceof TableRowCore) {
						TableRowCore rowCore = (TableRowCore) row;
						rowCore.setExpanded(!rowCore.isExpanded());
					}
				}
			}
		}
	}
}
