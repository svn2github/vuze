/**
 * Created on Jul 20, 2008
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package com.aelitis.azureus.ui.swt.views.skin;

import java.util.*;

import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Control;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.ui.swt.IconBarEnabler;
import org.gudy.azureus2.ui.swt.TorrentUtil;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;
import org.gudy.azureus2.ui.swt.views.IView;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.selectedcontent.ISelectedContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentListener;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectText;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.toolbar.ToolBarItem;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.util.Constants;
import com.aelitis.azureus.util.DataSourceUtils;

/**
 * @author TuxPaper
 * @created Jul 20, 2008
 *
 * 3.2 TODO: Link in az3 buttons
 * 3.2 TODO: Implement az2 button actions
 * 3.2 TODO: Implement disabling
 */
public class ToolBarView
	extends SkinView
{
	private static toolbarButtonListener buttonListener;

	private Map items = new LinkedHashMap();

	ToolBarItem lastItem = null;

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#showSupport(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		buttonListener = new toolbarButtonListener();

		// ==OPEN
		ToolBarItem item;
		item = new ToolBarItem("open", "image.toolbar.open", "iconBar.open") {
			public void triggerToolBarItem() {
				TorrentOpener.openTorrentWindow();
			}
		};
		addToolBarItem(item);

		// ==TOP
		item = new ToolBarItem("top", "image.toolbar.top", "iconBar.top") {
			public void triggerToolBarItem() {
				DownloadManager[] dms = getDMSFromSelectedContent();
				if (dms != null) {
					GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
					gm.moveTop(dms);
				}
			}
		};
		addToolBarItem(item);

		// ==UP
		item = new ToolBarItem("up", "image.toolbar.up", "iconBar.up") {
			public void triggerToolBarItem() {
				DownloadManager[] dms = getDMSFromSelectedContent();
				if (dms != null) {
					Arrays.sort(dms, new Comparator() {
						public int compare(Object a, Object b) {
							return ((DownloadManager) a).getPosition()
									- ((DownloadManager) b).getPosition();
						}
					});
					GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
					for (int i = 0; i < dms.length; i++) {
						DownloadManager dm = (DownloadManager) dms[i];
						if (gm.isMoveableUp(dm)) {
							gm.moveUp(dm);
						}
					}
				}
			}
		};
		addToolBarItem(item);

		// ==down
		item = new ToolBarItem("down", "image.toolbar.down", "iconBar.down") {
			public void triggerToolBarItem() {
				DownloadManager[] dms = getDMSFromSelectedContent();
				if (dms != null) {
					Arrays.sort(dms, new Comparator() {
						public int compare(Object a, Object b) {
							return ((DownloadManager) b).getPosition()
									- ((DownloadManager) a).getPosition();
						}
					});
					GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
					for (int i = 0; i < dms.length; i++) {
						DownloadManager dm = (DownloadManager) dms[i];
						if (gm.isMoveableDown(dm)) {
							gm.moveDown(dm);
						}
					}
				}
			}
		};
		addToolBarItem(item);

		// ==bottom
		item = new ToolBarItem("bottom", "image.toolbar.bottom", "iconBar.bottom") {
			public void triggerToolBarItem() {
				DownloadManager[] dms = getDMSFromSelectedContent();
				if (dms != null) {
					GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
					gm.moveEnd(dms);
				}
			}
		};
		addToolBarItem(item);

		// ==run
		item = new ToolBarItem("run", "image.toolbar.run", "iconBar.run") {
			public void triggerToolBarItem() {
				DownloadManager[] dms = getDMSFromSelectedContent();
				if (dms != null) {
					TorrentUtil.runTorrents(dms);
				}
			}
		};
		addToolBarItem(item);

		// ==start
		item = new ToolBarItem("start", "image.toolbar.start", "iconBar.queue") {
			public void triggerToolBarItem() {
				DownloadManager[] dms = getDMSFromSelectedContent();
				if (dms != null) {
					TorrentUtil.queueTorrents(dms, null);
				}
			}
		};
		addToolBarItem(item);

		// ==stop
		item = new ToolBarItem("stop", "image.toolbar.stop", "iconBar.stop") {
			public void triggerToolBarItem() {
				DownloadManager[] dms = getDMSFromSelectedContent();
				if (dms != null) {
					TorrentUtil.stopTorrents(dms, null);
				}
			}
		};
		addToolBarItem(item);

		// ==remove
		item = new ToolBarItem("remove", "image.toolbar.remove", "iconBar.remove") {
			public void triggerToolBarItem() {
				DownloadManager[] dms = getDMSFromSelectedContent();
				if (dms != null) {
					TorrentUtil.removeTorrents(dms, null);
				}
			}
		};
		addToolBarItem(item);
		
		
		// ==share
		item = new ToolBarItem("share", "image.button.share", "iconBar.share") {
			// @see com.aelitis.azureus.ui.swt.toolbar.ToolBarItem#triggerToolBarItem()
			public void triggerToolBarItem() {
				ISelectedContent[] contents = SelectedContentManager.getCurrentlySelectedContent();
				if (contents.length > 0) {
					VuzeShareUtils.getInstance().shareTorrent(contents[0], "ToolBar");
				}
			}
		};
		addToolBarItem(item);

		// ==details
		item = new ToolBarItem("details", "image.button.details", "iconBar.details") {
			// @see com.aelitis.azureus.ui.swt.toolbar.ToolBarItem#triggerToolBarItem()
			public void triggerToolBarItem() {
				DownloadManager[] dms = getDMSFromSelectedContent();
				if (dms != null) {
					TorrentListViewsUtils.viewDetails(dms[0], "Toolbar");
				}
			}
		};
		addToolBarItem(item);

		// ==comment
		item = new ToolBarItem("comment", "image.button.comment", "iconBar.comment") {
			// @see com.aelitis.azureus.ui.swt.toolbar.ToolBarItem#triggerToolBarItem()
			public void triggerToolBarItem() {
				DownloadManager[] dms = getDMSFromSelectedContent();
				if (dms != null) {
					String hash = DataSourceUtils.getHash(dms[0]);

					String url = Constants.URL_PREFIX + Constants.URL_COMMENTS + hash
							+ ".html?" + Constants.URL_SUFFIX + "&rnd=" + Math.random();

					UIFunctions functions = UIFunctionsManager.getUIFunctions();
					functions.viewURL(url, SkinConstants.VIEWID_BROWSER_BROWSE, 0, 0,
							false, false);
				}
			}
		};
		addToolBarItem(item);
		
		SelectedContentManager.addCurrentlySelectedContentListener(new SelectedContentListener() {
			public void currentlySectedContentChanged(
					ISelectedContent[] currentContent) {
				String[] itemsNeedingSelection = {
					"up",
					"down",
					"top",
					"bottom",
					"run",
					"remove",
				};

				String[] itemsRequiring1Selection = {
					"share",
					"details",
					"comment",
				};
				
				int numSelection = currentContent.length;
				boolean hasSelection = numSelection > 0;
				boolean has1Selection = numSelection == 1;
				ToolBarItem item;
				for (int i = 0; i < itemsNeedingSelection.length; i++) {
					String itemID = itemsNeedingSelection[i];
					item = getToolBarItem(itemID);

					if (item != null) {
						item.setEnabled(hasSelection);
					}
				}
				for (int i = 0; i < itemsRequiring1Selection.length; i++) {
					String itemID = itemsRequiring1Selection[i];
					item = getToolBarItem(itemID);

					if (item != null) {
						item.setEnabled(has1Selection);
					}
				}

				boolean canStart = false;
				boolean canStop = false;
				for (int i = 0; i < currentContent.length; i++) {
					ISelectedContent content = currentContent[i];
					DownloadManager dm = content.getDM();
					if (!canStart && ManagerUtils.isStartable(dm)) {
						canStart = true;
					}
					if (!canStop && ManagerUtils.isStopable(dm)) {
						canStop = true;
					}
				}
				item = getToolBarItem("start");
				if (item != null) {
					item.setEnabled(canStart);
				}
				item = getToolBarItem("stop");
				if (item != null) {
					item.setEnabled(canStop);
				}

			}
		});

		return null;
	}

	/**
	 * @return
	 *
	 * @since 3.1.1.1
	 */
	protected DownloadManager[] getDMSFromSelectedContent() {
		ISelectedContent[] sc = SelectedContentManager.getCurrentlySelectedContent();
		if (sc.length > 0) {
			int x = 0;
			DownloadManager[] dms = new DownloadManager[sc.length];
			for (int i = 0; i < sc.length; i++) {
				ISelectedContent selectedContent = sc[i];
				dms[x] = selectedContent.getDM();
				if (dms[x] != null) {
					x++;
				}
			}
			if (x > 0) {
				System.arraycopy(dms, 0, dms, 0, x);
				return dms;
			}
		}
		return null;
	}

	/**
	 * @param toolBarItem
	 *
	 * @since 3.1.1.1
	 */
	protected void activateViaSideBar(ToolBarItem toolBarItem) {
		SideBar sidebar = (SideBar) SkinViewManager.getByClass(SideBar.class);
		if (sidebar != null) {
			IView view = sidebar.getCurrentIView();
			if (view instanceof IconBarEnabler) {
				IconBarEnabler enabler = (IconBarEnabler) view;
				enabler.itemActivated(toolBarItem.getId());
			}
		}
	}

	/**
	 * @param itemID
	 * @return
	 *
	 * @since 3.1.1.1
	 */
	protected ToolBarItem getToolBarItem(String itemID) {
		return (ToolBarItem) items.get(itemID);
	}

	public void addToolBarItem(final ToolBarItem item) {
		SWTSkinObject so = skin.createSkinObject("toolbar:" + item.getId(),
				"toolbar.area.item", soMain);
		if (so != null) {
			if (lastItem != null) {
				Control lastControl = lastItem.getSkinButton().getSkinObject().getControl();
				FormData fd = (FormData) so.getControl().getLayoutData();
				fd.left = new FormAttachment(lastControl);
			}

			so.setData("toolbaritem", item);
			SWTSkinButtonUtility btn = new SWTSkinButtonUtility(so);
			btn.setImage(item.getImageID());
			btn.addSelectionListener(buttonListener);
			item.setSkinButton(btn);

			SWTSkinObject soTitle = skin.getSkinObject("toolbar-item-title", so);
			if (soTitle instanceof SWTSkinObjectText) {
				((SWTSkinObjectText) soTitle).setTextID(item.getTextID());
			}

			Utils.relayout(so.getControl().getParent());

			lastItem = item;
			items.put(item.getId(), item);
		}
	}

	private static class toolbarButtonListener
		extends ButtonListenerAdapter
	{
		public void pressed(SWTSkinButtonUtility buttonUtility) {
			ToolBarItem item = (ToolBarItem) buttonUtility.getSkinObject().getData(
					"toolbaritem");
			item.triggerToolBarItem();
		}

		public void disabledStateChanged(SWTSkinButtonUtility buttonUtility,
				boolean disabled) {
			ToolBarItem item = (ToolBarItem) buttonUtility.getSkinObject().getData(
					"toolbaritem");
			item.setEnabled(!disabled);
		}
	}
}
