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

import java.io.ByteArrayInputStream;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.table.TableRowCore;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.torrent.MetaDataUpdateListener;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.utils.PublishUtils;
import com.aelitis.azureus.ui.swt.views.TorrentListView;
import com.aelitis.azureus.ui.swt.views.TorrentListViewListener;
import com.aelitis.azureus.ui.swt.views.list.ListRow;
import com.aelitis.azureus.ui.swt.views.list.ListSelectionAdapter;

/**
 * @author TuxPaper
 * @created Oct 12, 2006
 *
 */
public class MediaList extends SkinView
{
	private SWTSkinObjectText lblCountAreaNotOurs;

	private SWTSkinObjectText lblCountAreaOurs;

	private TorrentListView view;

	private String PREFIX = "my-media-";

	private SWTSkinButtonUtility btnShare;

	private SWTSkinButtonUtility btnStop;

	private SWTSkinButtonUtility btnDelete;

	private SWTSkinButtonUtility btnDetails;

	private AzureusCore core;

	private SWTSkinButtonUtility btnComments;

	private SWTSkinButtonUtility btnPlay;

	private SWTSkinObjectImage skinImgThumb;

	private SWTSkinObjectText skinDetailInfo;

	private MetaDataUpdateListener listener;

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#showSupport(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object showSupport(SWTSkinObject skinObject, Object params) {
		final SWTSkin skin = skinObject.getSkin();
		core = AzureusCoreFactory.getSingleton();

		Composite cData = (Composite) skinObject.getControl();
		Composite cHeaders = null;

		skinObject = skin.getSkinObject(PREFIX + "list-headers");
		if (skinObject != null) {
			cHeaders = (Composite) skinObject.getControl();
		}

		view = new TorrentListView(core, skin, skin.getSkinProperties(), cHeaders,
				null, cData, TorrentListView.VIEW_MY_MEDIA, false, true) {
			public boolean isOurDownload(DownloadManager dm) {
				return true;
			}

			public void updateUI() {
				super.updateUI();

				if (!skinDetailInfo.getControl().isVisible()) {
					return;
				}
				
				if (view.getSelectedRows().length != 1) {
					updateDetailsInfo();
				}
			}
		};

		btnShare = TorrentListViewsUtils.addShareButton(skin, PREFIX, view);
		btnStop = TorrentListViewsUtils.addStopButton(skin, PREFIX, view);
		btnDetails = TorrentListViewsUtils.addDetailsButton(skin, PREFIX, view);
		btnComments = TorrentListViewsUtils.addCommentsButton(skin, PREFIX, view);
		btnPlay = TorrentListViewsUtils.addPlayButton(skin, PREFIX, view, false,
				true);

		view.addListener(new TorrentListViewListener() {
			boolean countChanging = false;

			// @see com.aelitis.azureus.ui.swt.views.TorrentListViewListener#stateChanged(org.gudy.azureus2.core3.download.DownloadManager)

			public void stateChanged(final DownloadManager manager) {
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						if (manager == null) {
							return;
						}
						ListRow row = view.getRow(manager);
						if (row == null) {
							return;
						}
						if (manager.isDownloadComplete(false)) {
							row.setForeground(null);
						} else {
							Color c = skin.getSkinProperties().getColor(
									"color.library.incomplete");
							row.setForeground(c);
						}

					}
				});
			}

			public void countChanged() {
				if (countChanging) {
					return;
				}

				countChanging = true;
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						countChanging = false;

						long totalOurs = 0;
						long totalNotOurs = 0;

						GlobalManager globalManager = core.getGlobalManager();
						Object[] dms = globalManager.getDownloadManagers().toArray();

						for (int i = 0; i < dms.length; i++) {
							DownloadManager dm = (DownloadManager) dms[i];
							if (dm.isDownloadComplete(false)) {
								if (PublishUtils.isPublished(dm)) {
									totalOurs++;
								} else {
									totalNotOurs++;
								}
							}
						}

						if (lblCountAreaOurs != null) {
							lblCountAreaOurs.setText(MessageText.getString("MainWindow.v3."
									+ PREFIX + "ours.count", new String[] { "" + totalOurs
							}));
						}
						if (lblCountAreaNotOurs != null) {
							lblCountAreaNotOurs.setText(MessageText.getString(
									"MainWindow.v3." + PREFIX + "notours.count",
									new String[] { "" + totalNotOurs
									}));
							lblCountAreaNotOurs.getControl().getParent().layout(true, true);
						}

					}
				});
			}
		});

		skinObject = skin.getSkinObject(PREFIX + "delete");
		if (skinObject instanceof SWTSkinObjectContainer) {
			btnDelete = new SWTSkinButtonUtility(skinObject);

			btnDelete.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					ListRow[] selectedRows = view.getSelectedRows();
					for (int i = 0; i < selectedRows.length; i++) {
						DownloadManager dm = (DownloadManager) selectedRows[i].getDataSource(true);
						ManagerUtils.remove(dm,
								btnDelete.getSkinObject().getControl().getShell(), true, true);
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

		view.addSelectionListener(new ListSelectionAdapter() {
			public void selected(ListRow row) {
				boolean bDisable = true;
				if (row != null) {
					DownloadManager dm = (DownloadManager) row.getDataSource(true);
					if (dm != null) {
						bDisable = !dm.isDownloadComplete(false);
					}
				}
				btnPlay.setDisabled(bDisable);
			}
		}, false);

		skinObject = skin.getSkinObject(PREFIX + "bigthumb");
		if (skinObject instanceof SWTSkinObjectImage) {
			listener = new MetaDataUpdateListener() {
				public void metaDataUpdated(TOTorrent torrent) {
					ListRow rowFocused = view.getRowFocused();
					if (rowFocused != null) {
						DownloadManager dm = (DownloadManager) rowFocused.getDataSource(true);
						if (dm.getTorrent().equals(torrent)) {
							update();
						}
					}
				}
			};
			PlatformTorrentUtils.addListener(listener);

			skinImgThumb = (SWTSkinObjectImage) skinObject;
			view.addSelectionListener(new ListSelectionAdapter() {
				public void deselected(ListRow row) {
					update();
				}

				public void selected(ListRow row) {
					update();
				}

				public void focusChanged(ListRow focusedRow) {
					update();
				}

			}, false);
		}

		skinObject = skin.getSkinObject(PREFIX + "detail-info");
		if (skinObject instanceof SWTSkinObjectText) {
			skinDetailInfo = (SWTSkinObjectText) skinObject;
			view.addSelectionListener(new ListSelectionAdapter() {
				public void deselected(ListRow row) {
					updateDetailsInfo();
				}

				public void selected(ListRow row) {
					updateDetailsInfo();
				}

				public void focusChanged(ListRow focusedRow) {
					updateDetailsInfo();
				}
			}, true);
		}

		return null;
	}

	private void updateDetailsInfo() {
		TableRowCore[] rows = view.getSelectedRows();
		if (rows.length == 0 || rows.length > 1) {
			int completed = 0;
			ListRow[] rowsUnsorted = view.getRowsUnsorted();
			int all = rowsUnsorted.length;
			for (int i = 0; i < all; i++) {
				ListRow row = rowsUnsorted[i];
				DownloadManager dm = (DownloadManager)row.getDataSource(true);
				if (dm != null) {
					if (dm.isDownloadComplete(false)) {
						completed++;
					}
				}
				
			}
			
			skinDetailInfo.setText(MessageText.getString(
					"MainWindow.v3.myMedia.noneSelected", new String[] {
						"" + all,
						"" + completed
					}));
			return;
		}
		String sText = "";
		DownloadManager dm = (DownloadManager) rows[0].getDataSource(true);
		if (dm != null) {
			TOTorrent torrent = dm.getTorrent();
			String s;
			s = PlatformTorrentUtils.getContentTitle(torrent);
			if (s != null) {
				sText += s + "\n\n";
			}

			s = PlatformTorrentUtils.getContentDescription(torrent);
			if (s != null) {
				sText += s + "\n";
			}
		}
		skinDetailInfo.setText(sText);
	}
	
	private void update() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				TableRowCore[] rows = view.getSelectedRows();
				if (rows.length == 0 || rows.length > 1) {
					skinImgThumb.setImage(null);
					return;
				}
				Image image = null;
				DownloadManager dm = (DownloadManager) rows[0].getDataSource(true);
				if (dm != null) {
					byte[] imageBytes = PlatformTorrentUtils.getContentThumbnail(dm.getTorrent());
					if (imageBytes != null) {
						ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
						image = new Image(skinImgThumb.getControl().getDisplay(), bais);
					}
				}
				Image oldImage = skinImgThumb.getImage();
				Utils.disposeSWTObjects(new Object[] { oldImage
				});
				skinImgThumb.setImage(image);
			}
		});
	}
	
}
