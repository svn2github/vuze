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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.ui.swt.IconBarEnabler;
import org.gudy.azureus2.ui.swt.TorrentUtil;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.selectedcontent.ISelectedContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentListener;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectText;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.toolbar.ToolBarItem;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarEntrySWT;
import com.aelitis.azureus.util.PlayUtils;

/**
 * @author TuxPaper
 * @created Jul 20, 2008
 */
public class ToolBarView
	extends SkinView
{
	private static toolbarButtonListener buttonListener;

	private Map items = new LinkedHashMap();

	private GlobalManager gm;

	//ToolBarItem lastItem = null;
	
	Control lastControl = null;
	
	private boolean showText = true;

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#showSupport(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectInitialShow(final SWTSkinObject skinObject, Object params) {
		buttonListener = new toolbarButtonListener();
		final GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
		final SWTSkinObject so2nd = skinObject.getSkin().getSkinObject("global-toolbar-2nd");

		
		final SWTSkinObject soGap = skinObject.getSkin().getSkinObject("toolbar-gap");
		if (soGap != null) {
			soGap.getControl().getParent().addListener(SWT.Resize, new Listener() {
				public void handleEvent(Event event) {
					Rectangle boundsLeft = skinObject.getControl().getBounds();
					Rectangle boundsRight = so2nd.getControl().getBounds();
					
					Rectangle clientArea = soGap.getControl().getParent().getClientArea();
					
					FormData fd = (FormData) soGap.getControl().getLayoutData();
					fd.width = clientArea.width - (boundsLeft.x + boundsLeft.width) - (boundsRight.width);
					if (fd.width < 0) {
						fd.width = 0;
					} else if (fd.width > 50) {
						fd.width -= 30;
					} else if (fd.width > 20) {
						fd.width = 20;
					}
					soGap.getControl().getParent().layout();
				}
			});
		}

		ToolBarItem item;
		
		// ==download
		item = new ToolBarItem("download", "image.button.download",
				"v3.MainWindow.button.download") {
			// @see com.aelitis.azureus.ui.swt.toolbar.ToolBarItem#triggerToolBarItem()
			public void triggerToolBarItem() {
				ToolBarView tb = (ToolBarView) SkinViewManager.getByClass(ToolBarView.class);
				if (tb != null) {
					ToolBarItem dlItem = tb.getToolBarItem("download");
					if (dlItem != null) {
						SWTSkinObject so = dlItem.getSkinButton().getSkinObject();
						String url = (String) so.getData("dlurl");
					}
				}

				
				// This is for our CDP pages
				ISelectedContent[] sc = SelectedContentManager.getCurrentlySelectedContent();
				if (sc != null && sc.length == 1
						&& (sc[0].getHash() != null || sc[0].getDownloadInfo() != null)) {
					TorrentListViewsUtils.downloadDataSource(sc[0], false, "ToolBar");
				}
			}
		};
		addToolBarItem(item);

		// ==play
		item = new ToolBarItem("play", "image.button.play", "iconBar.play") {
			// @see com.aelitis.azureus.ui.swt.toolbar.ToolBarItem#triggerToolBarItem()
			public void triggerToolBarItem() {
				ISelectedContent[] sc = SelectedContentManager.getCurrentlySelectedContent();
				if (sc != null) {
					TorrentListViewsUtils.playOrStreamDataSource(sc[0],
							this.getSkinButton());
				}
			}
		};
		addToolBarItem(item);
		
		addSeperator("toolbar.area.item.sep", soMain);

		lastControl = null;
		
		// ==OPEN
//		item = new ToolBarItem("open", "image.toolbar.open", "iconBar.open") {
//			public void triggerToolBarItem() {
//				TorrentOpener.openTorrentWindow();
//			}
//		};
//		addToolBarItem(item);
		
		addNonToolBar("toolbar.area.sitem.left", so2nd);

		// ==details
		//item = new ToolBarItem("details", "image.button.details", "iconBar.details") {
		//	// @see com.aelitis.azureus.ui.swt.toolbar.ToolBarItem#triggerToolBarItem()
		//	public void triggerToolBarItem() {
		//		DownloadManager[] dms = getDMSFromSelectedContent();
		//		if (dms != null) {
		//			TorrentListViewsUtils.viewDetails(dms[0], "Toolbar");
		//		}
		//	}
		//};
		//addToolBarItem(item, "toolbar.area.sitem");
		//addSeperator(so2nd);

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
		addToolBarItem(item, "toolbar.area.sitem", so2nd);
		addSeperator(so2nd);

		
		// ==run
		item = new ToolBarItem("run", "image.toolbar.run", "iconBar.run") {
			public void triggerToolBarItem() {
				DownloadManager[] dms = getDMSFromSelectedContent();
				if (dms != null) {
					TorrentUtil.runTorrents(dms);
					
					for (int i = 0; i < dms.length; i++) {
						DownloadManager dm = dms[i];
						PlatformTorrentUtils.setHasBeenOpened(dm.getTorrent(), true);
					}
				}
			}
		};
		addToolBarItem(item, "toolbar.area.sitem", so2nd);
		addSeperator(so2nd);

		
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
					for (int i = 0; i < dms.length; i++) {
						DownloadManager dm = (DownloadManager) dms[i];
						if (gm.isMoveableUp(dm)) {
							gm.moveUp(dm);
						}
					}
				}
			}
			
			// @see com.aelitis.azureus.ui.swt.toolbar.ToolBarItem#triggerToolBarItemHold()
			public boolean triggerToolBarItemHold() {
				DownloadManager[] dms = getDMSFromSelectedContent();
				if (dms != null) {
					gm.moveTop(dms);
				}
				return true;
			}
		};
		addToolBarItem(item, "toolbar.area.sitem", so2nd);
		addSeperator(so2nd);


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
					for (int i = 0; i < dms.length; i++) {
						DownloadManager dm = (DownloadManager) dms[i];
						if (gm.isMoveableDown(dm)) {
							gm.moveDown(dm);
						}
					}
				}
			}
			
			// @see com.aelitis.azureus.ui.swt.toolbar.ToolBarItem#triggerToolBarItemHold()
			public boolean triggerToolBarItemHold() {
				DownloadManager[] dms = getDMSFromSelectedContent();
				if (dms != null) {
					gm.moveEnd(dms);
				}
				return true;
			}
		};
		addToolBarItem(item, "toolbar.area.sitem", so2nd);
		addSeperator(so2nd);


		// ==start
		item = new ToolBarItem("start", "image.toolbar.start", "iconBar.queue") {
			public void triggerToolBarItem() {
				DownloadManager[] dms = getDMSFromSelectedContent();
				if (dms != null) {
					TorrentUtil.queueTorrents(dms, null);
				}
			}
		};
		addToolBarItem(item, "toolbar.area.sitem", so2nd);
		addSeperator(so2nd);


		// ==stop
		item = new ToolBarItem("stop", "image.toolbar.stop", "iconBar.stop") {
			public void triggerToolBarItem() {
				DownloadManager[] dms = getDMSFromSelectedContent();
				if (dms != null) {
					TorrentUtil.stopTorrents(dms, null);
				}
			}
		};
		addToolBarItem(item, "toolbar.area.sitem", so2nd);
		addSeperator(so2nd);


		// ==remove
		item = new ToolBarItem("remove", "image.toolbar.remove", "iconBar.remove") {
			public void triggerToolBarItem() {
				DownloadManager[] dms = getDMSFromSelectedContent();
				if (dms != null) {
					TorrentUtil.removeTorrents(dms, null);
				}
			}
		};
		addToolBarItem(item, "toolbar.area.sitem", so2nd);

		addNonToolBar("toolbar.area.sitem.right", so2nd);
		
		skinObject.getControl().getParent().layout();
		skinObject.getControl().getShell().layout(true, true);

//		// ==comment
//		item = new ToolBarItem("comment", "image.button.comment", "iconBar.comment") {
//			// @see com.aelitis.azureus.ui.swt.toolbar.ToolBarItem#triggerToolBarItem()
//			public void triggerToolBarItem() {
//				ISelectedContent[] sc = SelectedContentManager.getCurrentlySelectedContent();
//				if (sc.length > 0 && sc[0].getHash() != null) {
//					String url = Constants.URL_PREFIX + Constants.URL_COMMENTS
//							+ sc[0].getHash() + ".html?" + Constants.URL_SUFFIX + "&rnd="
//							+ Math.random();
//
//					UIFunctions functions = UIFunctionsManager.getUIFunctions();
//					functions.viewURL(url, SkinConstants.VIEWID_BROWSER_BROWSE, 0, 0,
//							false, false);
//				}
//			}
//		};
//		addToolBarItem(item);

		SelectedContentManager.addCurrentlySelectedContentListener(new SelectedContentListener() {
			public void currentlySelectedContentChanged(
					ISelectedContent[] currentContent, String viewID) {
				updateCoreItems(currentContent, viewID);
			}
		});

		return null;
	}

	/**
	 * 
	 *
	 * @since 3.1.1.1
	 */
	protected void updateCoreItems(ISelectedContent[] currentContent, String viewID) {
		String[] itemsNeedingSelection = {};

		String[] itemsNeedingDMSelection = {
			"remove",
			"run",
			"up",
			"down",
			"top",
			"bottom",
		};

		String[] itemsRequiring1Selection = {
			"share",
			"details",
			"comment",
		};

		String[] itemsRequiring1DMSelection = {
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
		
		DownloadManager[] dms = getDMSFromSelectedContent();
		boolean isDMSelection = dms != null && dms.length > 0;
		
		for (int i = 0; i < itemsNeedingDMSelection.length; i++) {
			String itemID = itemsNeedingDMSelection[i];
			item = getToolBarItem(itemID);

			if (item != null) {
				item.setEnabled(hasSelection && isDMSelection);
			}
		}
		for (int i = 0; i < itemsRequiring1Selection.length; i++) {
			String itemID = itemsRequiring1Selection[i];
			item = getToolBarItem(itemID);

			if (item != null) {
				item.setEnabled(has1Selection);
			}
		}
		for (int i = 0; i < itemsRequiring1DMSelection.length; i++) {
			String itemID = itemsRequiring1DMSelection[i];
			item = getToolBarItem(itemID);

			if (item != null) {
				item.setEnabled(has1Selection && isDMSelection);
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
		item = getToolBarItem("play");
		if (item != null) {
			item.setEnabled(has1Selection
					&& PlayUtils.canPlayDS(currentContent[0]));
		}
		item = getToolBarItem("download");
		if (item != null) {
			boolean enabled = has1Selection
					&& currentContent[0].getDM() == null
					&& (currentContent[0].getHash() != null || currentContent[0].getDownloadInfo() != null);
			item.setEnabled(enabled);
		}
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
			SideBarEntrySWT info = sidebar.getCurrentSideBarInfo();
			if (info.iview instanceof IconBarEnabler) {
				IconBarEnabler enabler = (IconBarEnabler) info.iview;
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
	public ToolBarItem getToolBarItem(String itemID) {
		return (ToolBarItem) items.get(itemID);
	}
	
	public ToolBarItem[] getAllToolBarItems() {
		return (ToolBarItem[]) items.values().toArray(new ToolBarItem[0]);
	}
	
	public void refreshCoreToolBarItems() {
		updateCoreItems(SelectedContentManager.getCurrentlySelectedContent(),
				SelectedContentManager.getCurrentySelectedViewID());
	}

	public void addToolBarItem(final ToolBarItem item) {
		addToolBarItem(item, "toolbar.area.item", soMain);
	}

	public void addToolBarItem(final ToolBarItem item, String templateID,
			SWTSkinObject soMain) {
		SWTSkinObject so = skin.createSkinObject("toolbar:" + item.getId(),
				templateID, soMain);
		if (so != null) {
			if (lastControl != null) {
				FormData fd = (FormData) so.getControl().getLayoutData();
				fd.left = new FormAttachment(lastControl);
			}

			so.setData("toolbaritem", item);
			SWTSkinButtonUtility btn = new SWTSkinButtonUtility(so,
					"toolbar-item-image");
			btn.setImage(item.getImageID());
			btn.addSelectionListener(buttonListener);
			item.setSkinButton(btn);

			SWTSkinObject soTitle = skin.getSkinObject("toolbar-item-title", so);
			if (soTitle instanceof SWTSkinObjectText) {
				((SWTSkinObjectText) soTitle).setTextID(item.getTextID());
			}

			Utils.relayout(so.getControl().getParent());

			lastControl = item.getSkinButton().getSkinObject().getControl();
			items.put(item.getId(), item);
		}
	}

	private void addSeperator(SWTSkinObject soMain) {
		addSeperator("toolbar.area.sitem.sep", soMain);
	}

	private void addSeperator(String id, SWTSkinObject soMain) {
		SWTSkinObject so = skin.createSkinObject("toolbar_sep" + Math.random(),
				id, soMain);
		if (so != null) {
			if (lastControl != null) {
				FormData fd = (FormData) so.getControl().getLayoutData();
				fd.left = new FormAttachment(lastControl, fd.left == null ? 0 : fd.left.offset);
			}
			
			lastControl = so.getControl();
		}
	}
	
	private void addNonToolBar(String skinid, SWTSkinObject soMain) {
		SWTSkinObject so = skin.createSkinObject("toolbar_d" + Math.random(),
				skinid, soMain);
		if (so != null) {
			if (lastControl != null) {
				FormData fd = (FormData) so.getControl().getLayoutData();
				fd.left = new FormAttachment(lastControl, fd.left == null ? 0 : fd.left.offset);
			}
			
			lastControl = so.getControl();
		}
	}

	/**
	 * @param showText the showText to set
	 */
	public void setShowText(boolean showText) {
		this.showText = showText;
		ToolBarItem[] allToolBarItems = getAllToolBarItems();
		for (int i = 0; i < allToolBarItems.length; i++) {
			ToolBarItem tbi = allToolBarItems[i];
			SWTSkinObject so = tbi.getSkinButton().getSkinObject();
			SWTSkinObject soTitle = skin.getSkinObject("toolbar-item-title", so);
			soTitle.setVisible(showText);
		}
	}

	/**
	 * @return the showText
	 */
	public boolean getShowText() {
		return showText;
	}

	private static class toolbarButtonListener
		extends ButtonListenerAdapter
	{
		public void pressed(SWTSkinButtonUtility buttonUtility) {
			ToolBarItem item = (ToolBarItem) buttonUtility.getSkinObject().getData(
					"toolbaritem");
			item.triggerToolBarItem();
		}
		
		public boolean held(SWTSkinButtonUtility buttonUtility) {
			ToolBarItem item = (ToolBarItem) buttonUtility.getSkinObject().getData(
			"toolbaritem");
			return item.triggerToolBarItemHold();
		}

		public void disabledStateChanged(SWTSkinButtonUtility buttonUtility,
				boolean disabled) {
			ToolBarItem item = (ToolBarItem) buttonUtility.getSkinObject().getData(
					"toolbaritem");
			item.setEnabled(!disabled);
		}
	}
}
