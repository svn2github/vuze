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

package org.gudy.azureus2.ui.swt.views.tableitems.files;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
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
	public NameItem() {
		super("name", ALIGN_LEAD, POSITION_LAST, 300,
				TableManager.TABLE_TORRENT_FILES);
		setType(TableColumn.TYPE_TEXT);
	}

	public void refresh(TableCell cell) {
		DiskManagerFileInfo fileInfo = (DiskManagerFileInfo) cell.getDataSource();
		String name = (fileInfo == null) ? "" : fileInfo.getFile(true).getName();
		if (name == null)
			name = "";
		//setText returns true only if the text is updated
		if (cell.setText(name) || !cell.isValid()) {
			if (bShowIcon) {
				Image icon;
				if (fileInfo == null) {
					icon = null;
				} else {
					// Don't ever dispose of PathIcon, it's cached and may be used elsewhere
					icon = ImageRepository.getPathIcon(fileInfo.getFile(true).getPath());

					if (Constants.isWindows) {
						// recomposite to avoid artifacts - transparency mask does not work
						final Image dstImage = new Image(Display.getCurrent(),
								icon.getBounds().width, icon.getBounds().height);
						GC gc = new GC(dstImage);
						gc.drawImage(icon, 0, 0);
						gc.dispose();
						icon = dstImage;
					}
				}

				// cheat for core, since we really know it's a TabeCellImpl and want to use
				// those special functions not available to Plugins
				((TableCellCore) cell).setIcon(icon);
			}
		}
	}

	public String getObfusticatedText(TableCell cell) {
		DiskManagerFileInfo fileInfo = (DiskManagerFileInfo) cell.getDataSource();
		String name = (fileInfo == null) ? ""
				: Debug.secretFileName(fileInfo.getFile(true).getName());
		if (name == null)
			name = "";
		return name;
	}

	public void dispose(TableCell cell) {
		if (bShowIcon && Constants.isWindows) {
			final Image img = ((TableCellCore) cell).getIcon();
			if (img != null) {
				((TableCellCore) cell).setIcon(null);
				if (!img.isDisposed()) {
					img.dispose();
				}
			}
		}
	}
}
