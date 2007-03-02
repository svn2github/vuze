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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManagerDownloadRemovalVetoException;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Alerts;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.views.TorrentListView;
import com.aelitis.azureus.ui.swt.views.list.ListView;

/**
 * @author TuxPaper
 * @created Sep 30, 2006
 *
 * TODO Code similaries between MiniRecentList, MiniDownloadList, ManageCdList, 
 *     and ManageDlList.  Need to combine
 */
public class MiniDownloadList
	extends SkinView
{
	private static String PREFIX = "minidownload-";

	private TorrentListView view;

	private SWTSkinButtonUtility btnAdd;

	private SWTSkinButtonUtility btnStop;

	private SWTSkinButtonUtility btnDelete;

	private SWTSkinButtonUtility btnDetails;

	private SWTSkinButtonUtility btnPlay;

	private SWTSkinButtonUtility btnShare;

	private SWTSkinButtonUtility btnComments;

	public Object showSupport(SWTSkinObject skinObject, Object params) {
		final SWTSkin skin = skinObject.getSkin();
		AzureusCore core = AzureusCoreFactory.getSingleton();

		Composite cData = (Composite) skinObject.getControl();
		Composite cHeaders = null;
		SWTSkinObjectText lblCountArea = null;

		skinObject = skin.getSkinObject(PREFIX + "list-headers");
		if (skinObject != null) {
			cHeaders = (Composite) skinObject.getControl();
		}

		skinObject = skin.getSkinObject(PREFIX + "titlextra");
		if (skinObject instanceof SWTSkinObjectText) {
			lblCountArea = (SWTSkinObjectText) skinObject;
		}

		view = new TorrentListView(core, skin, skin.getSkinProperties(), cHeaders,
				lblCountArea, cData, TorrentListView.VIEW_DOWNLOADING, true, true);

		skinObject = skin.getSkinObject(PREFIX + "add");
		if (skinObject instanceof SWTSkinObjectContainer) {
			btnAdd = new SWTSkinButtonUtility(skinObject);

			btnAdd.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					TorrentOpener.openTorrentWindow();
				}
			});
		}

		btnShare = TorrentListViewsUtils.addShareButton(skin, PREFIX, view);
		btnStop = TorrentListViewsUtils.addStopButton(skin, PREFIX, view);
		btnDetails = TorrentListViewsUtils.addDetailsButton(skin, PREFIX, view);
		btnComments = TorrentListViewsUtils.addCommentsButton(skin, PREFIX, view);
		btnPlay = TorrentListViewsUtils.addPlayButton(skin, PREFIX, view, true,
				false);

		SWTSkinObject soStream = skin.getSkinObject(PREFIX + "stream");
		if (soStream instanceof SWTSkinObjectContainer) {
			SWTSkinButtonUtility btn = new SWTSkinButtonUtility(soStream);
			btn.setDisabled(true);
			Composite c = (Composite) soStream.getControl();
			Control[] children = c.getChildren();
			c.setToolTipText("Coming Soon");

			for (int i = 0; i < children.length; i++) {
				Control control = children[i];
				control.setToolTipText("Coming Soon");
			}
		}

		skinObject = skin.getSkinObject(PREFIX + "delete");
		if (skinObject instanceof SWTSkinObjectContainer) {
			btnDelete = new SWTSkinButtonUtility(skinObject);

			btnDelete.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					TableRowCore[] selectedRows = view.getSelectedRows();
					for (int i = 0; i < selectedRows.length; i++) {
						DownloadManager dm = (DownloadManager) selectedRows[i].getDataSource(true);
						remove(dm, view, true, true);
					}
				}
			});
		}

		SWTSkinButtonUtility[] buttonsNeedingRow = {
			btnDelete,
			btnStop,
		};
		SWTSkinButtonUtility[] buttonsNeedingPlatform = {
			btnDetails,
			btnComments,
			btnShare,
		};
		SWTSkinButtonUtility[] buttonsNeedingSingleSelection = {
			btnDetails,
			btnComments,
			btnShare,
		};
		TorrentListViewsUtils.addButtonSelectionDisabler(view, buttonsNeedingRow,
				buttonsNeedingPlatform, buttonsNeedingSingleSelection, btnStop);

		return null;
	}

	public static void remove(final DownloadManager dm, final ListView view,
			final boolean bDeleteTorrent, final boolean bDeleteData) {
		// This is copied from ManagerUtils.java and modified so we
		// can remove the list row before stopping and removing

		Shell shell = view.getControl().getShell();
		if (COConfigurationManager.getBooleanParameter("confirm_torrent_removal")) {

			MessageBox mb = new MessageBox(shell, SWT.ICON_WARNING | SWT.YES | SWT.NO);

			mb.setText(MessageText.getString("deletedata.title"));

			mb.setMessage(MessageText.getString("deletetorrent.message1")
					+ dm.getDisplayName() + " :\n" + dm.getTorrentFileName()
					+ MessageText.getString("deletetorrent.message2"));

			if (mb.open() == SWT.NO) {
				return;
			}
		}

		boolean confirmDataDelete = COConfigurationManager.getBooleanParameter("Confirm Data Delete");

		int choice;
		if (confirmDataDelete && bDeleteData) {
			String path = dm.getSaveLocation().toString();

			MessageBox mb = new MessageBox(shell, SWT.ICON_WARNING | SWT.YES | SWT.NO);

			mb.setText(MessageText.getString("deletedata.title"));

			mb.setMessage(MessageText.getString("deletedata.message1")
					+ dm.getDisplayName() + " :\n" + path
					+ MessageText.getString("deletedata.message2"));

			choice = mb.open();
		} else {
			choice = SWT.YES;
		}

		if (choice == SWT.YES) {
			try {
				dm.getGlobalManager().canDownloadManagerBeRemoved(dm);
				view.removeDataSource(dm, true);
				new AEThread("asyncStop", true) {
					public void runSupport() {

						try {
							dm.stopIt(DownloadManager.STATE_STOPPED, bDeleteTorrent,
									bDeleteData);
							dm.getGlobalManager().removeDownloadManager(dm);
						} catch (GlobalManagerDownloadRemovalVetoException f) {
							if (!f.isSilent()) {
								Alerts.showErrorMessageBoxUsingResourceString(
										"globalmanager.download.remove.veto", f);
							}
							view.addDataSource(dm, true);
						} catch (Exception ex) {
							view.addDataSource(dm, true);
							Debug.printStackTrace(ex);
						}
					}
				}.start();
			} catch (GlobalManagerDownloadRemovalVetoException f) {
				if (!f.isSilent()) {
					Alerts.showErrorMessageBoxUsingResourceString(
							"globalmanager.download.remove.veto", f);
				}
			}
		}

	}

}
