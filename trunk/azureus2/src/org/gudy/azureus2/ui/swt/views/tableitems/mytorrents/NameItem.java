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

package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.debug.ObfusticateCellText;
import org.gudy.azureus2.ui.swt.views.table.TableCellCore;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import org.gudy.azureus2.plugins.ui.tables.*;

/** Torrent name cell for My Torrents.
 *
 * @author Olivier
 * @author TuxPaper (2004/Apr/17: modified to TableCellAdapter)
 */
public class NameItem extends CoreTableColumn implements
		TableCellRefreshListener, ObfusticateCellText, TableCellDisposeListener
{
	private static boolean bShowIcon;

	static {
		COConfigurationManager.addAndFireParameterListener(
				"NameColumn.showProgramIcon", new ParameterListener() {
					public void parameterChanged(String parameterName) {
						bShowIcon = COConfigurationManager.getBooleanParameter("NameColumn.showProgramIcon");
					}
				});
	}

	/** Default Constructor */
	public NameItem(String sTableID) {
		super("name", POSITION_LAST, 250, sTableID);
		setObfustication(true);
		setRefreshInterval(INTERVAL_LIVE);
		setType(TableColumn.TYPE_TEXT);
	}

	public void refresh(TableCell cell) {
		String name = null;
		DownloadManager dm = (DownloadManager) cell.getDataSource();
		if (dm != null)
			name = dm.getDisplayName();
		if (name == null)
			name = "";

		//setText returns true only if the text is updated
		if (cell.setText(name) || !cell.isValid()) {
			if (dm != null && bShowIcon) {
				DiskManagerFileInfo[] fileInfo = dm.getDiskManagerFileInfo();
				if (fileInfo.length > 0) {
					int idxBiggest = 0;
					long lBiggest = fileInfo[0].getLength();
					for (int i = 1; i < fileInfo.length; i++) {
						if (fileInfo[i].getLength() > lBiggest) {
							lBiggest = fileInfo[i].getLength();
							idxBiggest = i;
						}
					}
					String path = fileInfo[idxBiggest].getFile(true).getPath();
					// Don't ever dispose of PathIcon, it's cached and may be used elsewhere
					Image icon = ImageRepository.getPathIcon(path);

					if (Constants.isWindows) {
						disposeCellIcon(cell);

						Rectangle iconBounds = icon.getBounds();
						// recomposite to avoid artifacts - transparency mask does not work
						final Image dstImage = new Image(Display.getCurrent(),
								iconBounds.width, iconBounds.height);
						GC gc = new GC(dstImage);
						try {
							gc.drawImage(icon, 0, 0);
							if (fileInfo.length > 1) {
								Image imgFolder = ImageRepository.getImage("foldersmall");
								Rectangle folderBounds = imgFolder.getBounds();
								gc.drawImage(imgFolder, folderBounds.x, folderBounds.y,
										folderBounds.width, folderBounds.height, iconBounds.width
												- folderBounds.width, iconBounds.height
												- folderBounds.height, folderBounds.width,
										folderBounds.height);
							}
						} finally {
							gc.dispose();
						}
						icon = dstImage;
					}

					// cheat for core, since we really know it's a TabeCellImpl and want to
					// use those special functions not available to Plugins
					((TableCellCore) cell).setIcon(icon);
				} else {
					if (Constants.isWindows) {
						disposeCellIcon(cell);
					}
					((TableCellCore) cell).setIcon(null);
				}
			}
		}
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
		if (bShowIcon && Constants.isWindows) {
			disposeCellIcon(cell);
		}
	}

	private void disposeCellIcon(TableCell cell) {
		final Image img = ((TableCellCore) cell).getIcon();
		if (img != null && !img.isDisposed()) {
			img.dispose();
		}
	}
}
