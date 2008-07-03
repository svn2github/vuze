/**
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.ui.swt.views.skin;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.util.DisplayFormatters;

import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.common.table.TableSelectionAdapter;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectText;
import com.aelitis.azureus.ui.swt.views.TorrentListView;
import com.aelitis.azureus.ui.swt.views.TorrentListViewListener;

/**
 * @author TuxPaper
 * @created Sep 30, 2006
 *
 * TODO Code similaries between MiniRecentList, MiniDownloadList, ManageCdList, 
 *     and ManageDlList.  Need to combine
 */
public class ManageDlList
	extends SkinView
{
	private String PREFIX = "manage-dl-";

	private TorrentListView view;

	private SWTSkinObjectText statusObject;

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#showSupport(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {

		view = new TorrentListView(this, PREFIX, TorrentListView.VIEW_DOWNLOADING,
				false, true);


		skinObject = skin.getSkinObject(PREFIX + "status");
		if (skinObject instanceof SWTSkinObjectText) {
			statusObject = (SWTSkinObjectText) skinObject;

			view.addListener(new TorrentListViewListener() {
				public void stateChanged(DownloadManager manager) {
					TableRowCore[] selectedRows = view.getSelectedRows();
					updateStatusText(selectedRows.length == 1 ? selectedRows[0] : null);
				}
			});
		}

		view.addSelectionListener(new TableSelectionAdapter() {
			public void deselected(TableRowCore[] rows) {
				update();
			}

			public void selected(TableRowCore[] rows) {
				update();
			}

			public void focusChanged(TableRowCore focusedRow) {
				update();
			}

			private void update() {
				updateStatusText(view.getFocusedRow());
			}
		}, true);

		return null;
	}

	/**
	 * @param row
	 */
	protected void updateStatusText(TableRowCore row) {
		if (row != null) {
			DownloadManager dm = (DownloadManager) row.getDataSource(true);
			statusObject.setText(DisplayFormatters.formatDownloadStatus(dm));
		} else {
			statusObject.setTextID("v3.MainWindow." + PREFIX + "status.noselection");
		}
	}
}
