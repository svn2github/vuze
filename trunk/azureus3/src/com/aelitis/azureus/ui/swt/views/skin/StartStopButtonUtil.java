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
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility;
import com.aelitis.azureus.ui.swt.views.TorrentListView;
import com.aelitis.azureus.ui.swt.views.list.ListView;

/**
 * @author TuxPaper
 * @created Sep 30, 2006
 *
 */
public class StartStopButtonUtil
{
	public static void updateStopButton(ListView view,
			SWTSkinButtonUtility button) {
		if (button == null) {
			return;
		}

		try {
			TableRowCore[] selectedRows = view.getSelectedRows();

			if (selectedRows.length == 0) {
				button.setDisabled(true);
				return;
			}
			boolean bResume = true;
			boolean bDisabled = false;
			for (int i = 0; i < selectedRows.length; i++) {
				TableRowCore row = selectedRows[i];
				DownloadManager dm = (DownloadManager) row.getDataSource(true);
				if (dm != null) {
					if (bResume) {
  					int state = dm.getState();
  					boolean bNotRunning = state == DownloadManager.STATE_QUEUED
  							|| state == DownloadManager.STATE_STOPPED
  							|| state == DownloadManager.STATE_STOPPING
  							|| state == DownloadManager.STATE_ERROR;
  					if (!bNotRunning) {
  						bResume = false;
  					}
					}
					if (!bDisabled && dm.getAssumedComplete()) {
						bDisabled = true;
					}
				}
			}
			button.setDisabled(bDisabled);
			
			if (bResume) {
				button.setImage("image.button.unpause");
				button.setTooltipID("v3.MainWindow.button.resume");
			} else {
				button.setImage("image.button.pause");
				button.setTooltipID("v3.MainWindow.button.pause");
			}
		} catch (Exception e) {
			Debug.out(e);
		}
	}

}
