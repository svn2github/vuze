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
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.UIInputReceiver;
import org.gudy.azureus2.plugins.ui.UIInputReceiverListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.SimpleTextEntryWindow;
import org.gudy.azureus2.ui.swt.debug.ObfusticateCellText;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

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
	TableCellDisposeListener, TableCellSWTPaintListener
{
	public static final Class DATASOURCE_TYPE = Download.class;

	public static final String COLUMN_ID = "name";

	private boolean showIcon;

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

		COConfigurationManager.addAndFireParameterListener(
				"NameColumn.showProgramIcon", new ParameterListener() {
					public void parameterChanged(String parameterName) {
						setShowIcon(COConfigurationManager.getBooleanParameter("NameColumn.showProgramIcon"));
					}
				});
	}

	public void refresh(TableCell cell) {
		refresh(cell, false);
	}

	public void refresh(TableCell cell, boolean sortOnlyRefresh) {
		if (sortOnlyRefresh) {
			String name = null;
			DownloadManager dm = (DownloadManager) cell.getDataSource();
			if (dm != null) {
				name = dm.getDisplayName();
			}
			if (name == null) {
				name = "";
			}
			
			cell.setSortValue(name);
		}
	}

	public void cellPaint(GC gc, final TableCellSWT cell) {
		Object ds = cell.getDataSource();

		Rectangle cellBounds = cell.getBounds();

		Image[] imgThumbnail = TorrentUIUtilsV3.getContentImage(ds,
				cellBounds.width >= 20 && cellBounds.height >= 20,
				new ContentImageLoadedListener() {
					public void contentImageLoaded(Image image, boolean wasReturned) {
						if (!wasReturned) {
							// this may be triggered many times, so only invalidate and don't
							// force a refresh()
							cell.invalidate();
						}
					}
				});

		if (imgThumbnail == null) {
			// don't need to release a null image
			return;
		}

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

		try {
			gc.setAdvanced(true);
			gc.setInterpolation(SWT.HIGH);
		} catch (Exception e) {
		}
		int x = cellBounds.x;
		int textX = x + dstWidth + 3;
		if (dstHeight > 25) {
  		int minWidth = dstHeight * 7 / 4;
  		if (dstWidth < minWidth) {
  			x = cellBounds.x + ((minWidth - dstWidth + 1) / 2);
  			textX = cellBounds.x + minWidth + 3;
  		}
		}
		if (cellBounds.width - dstWidth < 100 && dstWidth > 32) {
			x = cellBounds.x;
			dstWidth = 32;
			dstHeight = imgBounds.height * dstWidth / imgBounds.width;
			textX = cellBounds.x + 35;
		}
		int y = cellBounds.y + ((cellBounds.height - dstHeight + 1) / 2);
		if (dstWidth > 0 && dstHeight > 0 && !imgBounds.isEmpty()) {
			Rectangle dst = new Rectangle(x, y, dstWidth, dstHeight);
			Rectangle lastClipping = gc.getClipping();
			try {
				gc.setClipping(cellBounds);

				for (int i = 0; i < imgThumbnail.length; i++) {
					Image image = imgThumbnail[i];
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

		String name = null;
		DownloadManager dm = (DownloadManager) cell.getDataSource();
		if (dm != null)
			name = dm.getDisplayName();
		if (name == null)
			name = "";

		GCStringPrinter.printString(gc, name, new Rectangle(textX,
				cellBounds.y, cellBounds.x + cellBounds.width - textX, cellBounds.height),
				false, true, SWT.WRAP);

		TorrentUIUtilsV3.releaseContentImage(ds);
	}

	public String getObfusticatedText(TableCell cell) {
		String name = null;
		DownloadManager dm = (DownloadManager) cell.getDataSource();
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
	}

	/**
	 * @return the showIcon
	 */
	public boolean isShowIcon() {
		return showIcon;
	}

}
