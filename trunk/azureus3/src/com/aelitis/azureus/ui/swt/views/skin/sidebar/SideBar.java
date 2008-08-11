/**
 * Created on Jun 23, 2008
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

package com.aelitis.azureus.ui.swt.views.skin.sidebar;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.download.impl.DownloadManagerAdapter;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerAdapter;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.PluginsMenuHelper;
import org.gudy.azureus2.ui.swt.mainwindow.PluginsMenuHelper.IViewInfo;
import org.gudy.azureus2.ui.swt.mainwindow.PluginsMenuHelper.PluginAddedViewListener;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTInstanceImpl;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewEventCancelledException;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewImpl;
import org.gudy.azureus2.ui.swt.views.*;
import org.gudy.azureus2.ui.swt.views.stats.StatsView;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.activities.VuzeActivitiesListener;
import com.aelitis.azureus.activities.VuzeActivitiesManager;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoListener;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.views.skin.SkinView;
import com.aelitis.azureus.util.MapUtils;

import org.gudy.azureus2.plugins.ui.UIPluginView;

/**
 * @author TuxPaper
 * @created Jun 23, 2008
 *
 */
public class SideBar
	extends SkinView
	implements UIUpdatable, ViewTitleInfoListener
{
	public static final String SIDEBAR_SECTION_PLUGINS = "Plugins_SB";

	public static final String SIDEBAR_SECTION_LIBRARY = "Library_SB";

	public static final String SIDEBAR_SECTION_TOOLS = "Tools_SB";

	public static final String SIDEBAR_SECTION_BROWSE = "Browse_SB";

	public static final String SIDEBAR_SECTION_WELCOME = "Welcome_SB";

	public static final String SIDEBAR_SECTION_PUBLISH = "Publish_SB";

	public static final String SIDEBAR_SECTION_SUBSCRIPTIONS = "Subscriptions_SB";

	public static final String SIDEBAR_SECTION_ADVANCED = "Advanced_SB";

	public static final boolean SHOW_ALL_PLUGINS = false;

	public static final boolean SHOW_TOOLS = false;

	private SWTSkin skin;

	private SWTSkinObject soSideBarContents;

	private SWTSkinObject soSideBarList;

	private Tree tree;

	private Font fontHeader;

	private IView currentIView;

	private String currentIViewID;

	private static Map mapTitleInfoToTreeItem = new LightHashMap();

	private static Map mapIdToSideBarInfo = new LightHashMap();

	private static List listTreeItemsNoTitleInfo = new ArrayList();

	private static DisposeListener disposeTreeItemListener;

	private int numSeeding = 0;

	private int numDownloading = 0;

	private int numComplete = 0;

	private int numIncomplete = 0;

	private CopyOnWriteList listeners = new CopyOnWriteList();

	double lastPercent = 0.8;

	private static Map mapAutoOpen = new LightHashMap();

	static {
		disposeTreeItemListener = new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				TreeItem treeItem = (TreeItem) e.widget;
				String id = (String) treeItem.getData("Plugin.viewID");

				if (id != null) {
					mapAutoOpen.remove(id);
					mapIdToSideBarInfo.remove(id);
					return;
				}

				for (Iterator iter = mapIdToSideBarInfo.keySet().iterator(); iter.hasNext();) {
					id = (String) iter.next();
					SideBarInfo sideBarInfo = getSideBarInfo(id);
					if (sideBarInfo != null && sideBarInfo.treeItem == treeItem) {
						iter.remove();
					}
				}
			}
		};
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectAdapter#skinObjectCreated(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectCreated(SWTSkinObject skinObject, Object params) {
		skin = skinObject.getSkin();

		soSideBarContents = skin.getSkinObject("sidebar-contents");
		soSideBarList = skin.getSkinObject("sidebar-list");

		ViewTitleInfoManager.addListener(this);

		setupList();

		try {
			UIFunctionsManager.getUIFunctions().getUIUpdater().addUpdater(this);
		} catch (Exception e) {
			Debug.out(e);
		}

		Display.getDefault().addFilter(SWT.KeyDown, new Listener() {
			public void handleEvent(Event event) {
				if (event.keyCode == SWT.F9
						|| event.keyCode == SWT.F7
						|| (event.keyCode == 116 && event.stateMask == (SWT.COMMAND | SWT.ALT))) {
					event.doit = false;
					event.keyCode = 0;
					event.character = '\0';
					flipSideBarVisibility();
				}
			}
		});

		return null;
	}

	/**
	 * 
	 *
	 * @since 3.1.1.1
	 */
	public void flipSideBarVisibility() {
		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				SWTSkinObjectSash soSash = (SWTSkinObjectSash) skin.getSkinObject("sidebar-sash");
				if (soSash.getPercent() == 1) {
					if (lastPercent != 0) {
						soSash.setPercent(lastPercent);
					}
				} else {
					lastPercent = soSash.getPercent();
					soSash.setPercent(1);
				}
			}
		});
	}

	public boolean isVisible() {
		SWTSkinObjectSash soSash = (SWTSkinObjectSash) skin.getSkinObject("sidebar-sash");
		return soSash.getPercent() != 1.0;
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#showSupport(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		if (!isVisible()) {
			flipSideBarVisibility();
		}
		return null;
	}

	public Object skinObjectDestroyed(SWTSkinObject skinObject, Object params) {
		try {
			UIFunctionsManager.getUIFunctions().getUIUpdater().removeUpdater(this);
		} catch (Exception e) {
			Debug.out(e);
		}
		return null;
	}

	private void setupList() {
		Composite parent = (Composite) soSideBarList.getControl();

		tree = new Tree(parent, SWT.FULL_SELECTION | SWT.BORDER);
		tree.setHeaderVisible(false);

		tree.setLayoutData(Utils.getFilledFormData());

		SWTSkinProperties skinProperties = skin.getSkinProperties();
		final Color bg = skinProperties.getColor("color.sidebar.bg");
		final Color fg = skinProperties.getColor("color.sidebar.fg");
		final Color bgSel = skinProperties.getColor("color.sidebar.selected.bg");
		final Color fgSel = skinProperties.getColor("color.sidebar.selected.fg");
		final Color colorFocus = skinProperties.getColor("color.sidebar.focus");

		tree.setBackground(bg);
		tree.setForeground(fg);

		FontData[] fontData = tree.getFont().getFontData();
		//fontData[0].setHeight(fontData[0].getHeight() + 1);
		fontData[0].setStyle(SWT.BOLD);
		fontHeader = new Font(tree.getDisplay(), fontData);
		tree.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				fontHeader.dispose();
				saveCloseables();
			}
		});

		Listener paintListener = new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {
					case SWT.MeasureItem: {
						int clientWidth = tree.getClientArea().width;
						TreeItem item = (TreeItem) event.item;
						String text = item.getText(event.index);
						Point size = event.gc.textExtent(text);
						if (event.x + event.width < clientWidth) {
							event.width = size.x + event.x; // tree.getClientArea().width;
							event.x = 0;
						}
						event.height = Math.max(event.height, size.y + 12);
						break;
					}
					case SWT.PaintItem: {
						TreeItem treeItem = (TreeItem) event.item;
						String text = treeItem.getText(event.index);
						Point size = event.gc.textExtent(text);
						Rectangle treeBounds = tree.getBounds();

						event.gc.setClipping((Rectangle) null);

						boolean selected = tree.getSelectionCount() == 1
								&& tree.getSelection()[0].equals(treeItem);
						Color curBG;
						if (selected) {
							event.gc.setForeground(colorFocus);
							event.gc.drawRectangle(0, event.y, treeBounds.width - 1,
									event.height - 1);
							event.gc.setForeground(fgSel);
							event.gc.setBackground(bgSel);

							curBG = event.gc.getBackground();
							event.gc.fillRectangle(0, event.y, Math.max(treeBounds.width,
									event.width + event.x), event.height);
						} else {
							if (fg != null) {
								event.gc.setForeground(fg);
							}
							if (bg != null) {
								event.gc.setBackground(bg);
							}
							event.gc.fillRectangle(0, event.y, Math.max(treeBounds.width,
									event.width + event.x), event.height);
							curBG = event.gc.getBackground();
						}

						if (treeItem.getItemCount() > 0) {
							if (treeItem.getExpanded()) {
								event.gc.drawText("V", event.x - 16, event.y + 6, true);
							} else {
								event.gc.drawText(">", event.x - 16, event.y + 6, true);
							}
							event.gc.setFont(fontHeader);
						}

						Rectangle itemBounds = tree.getClientArea();

						event.gc.drawText(text, event.x, event.y + 6, true);

						event.gc.setFont(tree.getFont());

						String id = (String) treeItem.getData("Plugin.viewID");
						SideBarInfo sideBarInfo = getSideBarInfo(id);
						int xIndicatorOfs = 0;

						if (sideBarInfo.closeable) {
							Image imgClose = ImageRepository.getImage("smallx");
							Rectangle closeArea = imgClose.getBounds();
							closeArea.x = itemBounds.width - closeArea.width;
							closeArea.y = event.y + 4;
							xIndicatorOfs = closeArea.width;

							event.gc.fillRectangle(closeArea);

							event.gc.drawImage(imgClose, closeArea.x, closeArea.y);
							treeItem.setData("closeArea", closeArea);
						}

						if (sideBarInfo.titleInfo != null) {
							String textIndicator = sideBarInfo.titleInfo.getTitleInfoStringProperty(ViewTitleInfo.TITLE_INDICATOR_TEXT);
							if (textIndicator != null) {
								event.gc.setAntialias(SWT.ON);

								Point textSize = event.gc.textExtent(textIndicator);
								int x = itemBounds.width - textSize.x - 7 - xIndicatorOfs;
								int y = event.y + 6;

								event.gc.fillRectangle(x - 7, y - 2, textSize.x + 14,
										textSize.y + 4);

								event.gc.setForeground(Colors.blues[Colors.BLUES_LIGHTEST]);
								event.gc.setBackground(Colors.faded[Colors.BLUES_DARKEST]);
								event.gc.fillRoundRectangle(x - 5, y - 2, textSize.x + 10,
										textSize.y + 4, 10, textSize.y + 4);
								event.gc.drawText(textIndicator, x, y);
							}
						}

						break;
					}
					case SWT.EraseItem: {
						//event.detail &= ~SWT.FOREGROUND;
						event.detail &= ~(SWT.FOREGROUND | SWT.BACKGROUND);
						break;
					}

					case SWT.Resize: {
						tree.redraw();
					}
				}
			}
		};

		tree.addListener(SWT.MeasureItem, paintListener);
		if (bg != null) {
			tree.addListener(SWT.Resize, paintListener);
			tree.addListener(SWT.PaintItem, paintListener);
			tree.addListener(SWT.EraseItem, paintListener);
		}

		tree.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				TreeItem item = (TreeItem) event.item;
				itemSelected(item);
			}
		});

		tree.addListener(SWT.MouseUp, new Listener() {
			public void handleEvent(Event event) {
				if (tree.getItemCount() == 0) {
					return;
				}
				int indent = tree.getItem(0).getBounds().x;
				TreeItem treeItem = tree.getItem(new Point(indent, event.y));
				if (treeItem == null) {
					return;
				}
				Rectangle closeArea = (Rectangle) treeItem.getData("closeArea");
				if (closeArea != null && closeArea.contains(event.x, event.y)) {
					String id = (String) treeItem.getData("Plugin.viewID");
					SideBarInfo sideBarInfo = getSideBarInfo(id);
					if (sideBarInfo.iview != null) {
						sideBarInfo.iview.delete();
					}
					if (sideBarInfo.skinObject != null) {
						sideBarInfo.skinObject.getSkin().removeSkinObject(
								sideBarInfo.skinObject);
					}
					COConfigurationManager.removeParameter("SideBar.AutoOpen." + id);
					treeItem.dispose();
				}
			}
		});

		createTreeItems();

		TreeItem treeItem = tree.getItem(0);
		treeItem.getParent().select(treeItem);
		treeItem.getParent().showItem(treeItem);
		itemSelected(treeItem);

		parent.layout(true, true);
	}

	/**
	 * 
	 *
	 * @since 3.1.1.1
	 */
	private void createTreeItems() {
		TreeItem treeItem;

		createTreeItemFromSkinRef(null, SIDEBAR_SECTION_WELCOME,
				"main.area.welcome", "Welcome", null, null, false);

		// Put TitleInfo in another class
		final ViewTitleInfo titleInfoActivityView = new ViewTitleInfo() {
			public String getTitleInfoStringProperty(int propertyID) {
				if (propertyID == TITLE_INDICATOR_TEXT) {
					return "" + VuzeActivitiesManager.getNumEntries();
				}
				return null;
			}

			public Object getTitleInfoObjectProperty(int propertyID) {
				return null;
			}
		};
		VuzeActivitiesManager.addListener(new VuzeActivitiesListener() {
			public void vuzeNewsEntryChanged(VuzeActivitiesEntry entry) {
			}

			public void vuzeNewsEntriesRemoved(VuzeActivitiesEntry[] entries) {
				ViewTitleInfoManager.refreshTitleInfo(titleInfoActivityView);
			}

			public void vuzeNewsEntriesAdded(VuzeActivitiesEntry[] entries) {
				ViewTitleInfoManager.refreshTitleInfo(titleInfoActivityView);
			}
		});

		final TreeItem itemActivity = createTreeItemFromSkinRef(null,
				"Activity_SB", "main.area.events", "Activity", titleInfoActivityView,
				null, false);

		final GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
		final ViewTitleInfo titleInfoLibrary = new ViewTitleInfo() {
			public String getTitleInfoStringProperty(int propertyID) {
				if (propertyID == TITLE_INDICATOR_TEXT) {
					return "" + gm.getDownloadManagers().size();
				}
				return null;
			}

			public Object getTitleInfoObjectProperty(int propertyID) {
				return null;
			}
		};
		final ViewTitleInfo titleInfoDownloading = new ViewTitleInfo() {
			public String getTitleInfoStringProperty(int propertyID) {
				if (propertyID == TITLE_INDICATOR_TEXT) {
					return numDownloading + " of " + numIncomplete;
				}
				return null;
			}

			public Object getTitleInfoObjectProperty(int propertyID) {
				return null;
			}
		};
		final ViewTitleInfo titleInfoSeeding = new ViewTitleInfo() {
			public String getTitleInfoStringProperty(int propertyID) {
				if (propertyID == TITLE_INDICATOR_TEXT) {
					return numSeeding + " of " + numComplete;
				}
				return null;
			}

			public Object getTitleInfoObjectProperty(int propertyID) {
				return null;
			}
		};

		final DownloadManagerListener dmListener = new DownloadManagerAdapter() {
			public void stateChanged(DownloadManager dm, int state) {
				if (dm.getAssumedComplete()) {
					boolean isSeeding = dm.getState() == DownloadManager.STATE_SEEDING;
					Boolean wasSeedingB = (Boolean) dm.getUserData("wasSeeding");
					boolean wasSeeding = wasSeedingB == null ? false
							: wasSeedingB.booleanValue();
					if (isSeeding != wasSeeding) {
						if (isSeeding) {
							numSeeding++;
						} else {
							numSeeding--;
						}
						dm.setUserData("wasSeeding", new Boolean(isSeeding));
					}
				} else {
					boolean isDownloading = dm.getState() == DownloadManager.STATE_DOWNLOADING;
					Boolean wasDownloadingB = (Boolean) dm.getUserData("wasDownloading");
					boolean wasDownloading = wasDownloadingB == null ? false
							: wasDownloadingB.booleanValue();
					if (isDownloading != wasDownloading) {
						if (isDownloading) {
							numDownloading++;
						} else {
							numDownloading--;
						}
						dm.setUserData("wasDownloading", new Boolean(isDownloading));
					}
				}
				ViewTitleInfoManager.refreshTitleInfo(titleInfoDownloading);
				ViewTitleInfoManager.refreshTitleInfo(titleInfoSeeding);
			}

			public void completionChanged(DownloadManager dm, boolean completed) {
				if (dm.getAssumedComplete()) {
					numComplete++;
					numIncomplete--;
				} else {
					numIncomplete++;
					numComplete--;
				}
				ViewTitleInfoManager.refreshTitleInfo(titleInfoDownloading);
				ViewTitleInfoManager.refreshTitleInfo(titleInfoSeeding);
			}
		};
		gm.addListener(new GlobalManagerAdapter() {
			public void downloadManagerRemoved(DownloadManager dm) {
				if (dm.getAssumedComplete()) {
					numComplete--;
				} else {
					numIncomplete--;
				}
				ViewTitleInfoManager.refreshTitleInfo(titleInfoLibrary);
				ViewTitleInfoManager.refreshTitleInfo(titleInfoDownloading);
				ViewTitleInfoManager.refreshTitleInfo(titleInfoSeeding);
				dm.removeListener(dmListener);
			}

			public void downloadManagerAdded(DownloadManager dm) {
				ViewTitleInfoManager.refreshTitleInfo(titleInfoLibrary);
				ViewTitleInfoManager.refreshTitleInfo(titleInfoDownloading);
				ViewTitleInfoManager.refreshTitleInfo(titleInfoSeeding);
				dm.addListener(dmListener, false);

				if (dm.getAssumedComplete()) {
					numComplete++;
					if (dm.getState() == DownloadManager.STATE_SEEDING) {
						numSeeding++;
					}
				} else {
					numIncomplete++;
					if (dm.getState() == DownloadManager.STATE_DOWNLOADING) {
						dm.setUserData("wasDownloading", new Boolean(true));
						numSeeding++;
					} else {
						dm.setUserData("wasDownloading", new Boolean(false));
					}
				}
			}
		}, false);
		List downloadManagers = gm.getDownloadManagers();
		for (Iterator iter = downloadManagers.iterator(); iter.hasNext();) {
			DownloadManager dm = (DownloadManager) iter.next();
			dm.addListener(dmListener, false);
			if (dm.getAssumedComplete()) {
				numComplete++;
				if (dm.getState() == DownloadManager.STATE_SEEDING) {
					dm.setUserData("wasSeeding", new Boolean(true));
					numSeeding++;
				} else {
					dm.setUserData("wasSeeding", new Boolean(false));
				}
			} else {
				numIncomplete++;
				if (dm.getState() == DownloadManager.STATE_DOWNLOADING) {
					numSeeding++;
				}
			}
		}

		createTreeItemFromSkinRef(null, SIDEBAR_SECTION_LIBRARY, "library",
				"Library", titleInfoLibrary, null, false);

		createTreeItemFromSkinRef(SIDEBAR_SECTION_LIBRARY, "LibraryDL_SB",
				"library", "Downloading", titleInfoDownloading, null, false);

		createTreeItemFromSkinRef(SIDEBAR_SECTION_LIBRARY, "LibraryCD_SB",
				"library", "Seeding", titleInfoSeeding, null, false);

		createTreeItemFromSkinRef(null, SIDEBAR_SECTION_BROWSE,
				"main.area.browsetab", "On Vuze", null, null, false);

		createTreeItemFromSkinRef(SIDEBAR_SECTION_BROWSE, "Rec_SB",
				"main.area.rec", "Recommendations", null, null, false);

		createTreeItemFromSkinRef(SIDEBAR_SECTION_BROWSE, SIDEBAR_SECTION_PUBLISH,
				"publishtab.area", "Publish", null, null, false);

		createTreeItemFromSkinRef(null, SIDEBAR_SECTION_SUBSCRIPTIONS,
				"main.area.subscriptions", "Subscriptions", null, null, false);

		//new TreeItem(tree, SWT.NONE).setText("Search");

		if (SHOW_TOOLS) {
			createTreeItemFromSkinRef(null, SIDEBAR_SECTION_TOOLS, "main.area.hood",
					"Under The Hood", null, null, false);

			createTreeItemFromIViewClass(SIDEBAR_SECTION_TOOLS, "All Peers",
					PeerSuperView.class);
			createTreeItemFromIViewClass(SIDEBAR_SECTION_TOOLS, "Stats",
					StatsView.class);
			createTreeItemFromIViewClass(SIDEBAR_SECTION_TOOLS, "My Tracker",
					MyTrackerView.class);
			createTreeItemFromIViewClass(SIDEBAR_SECTION_TOOLS, "My Classic-Shares",
					MySharesView.class);
			createTreeItemFromIViewClass(SIDEBAR_SECTION_TOOLS, "Logger",
					LoggerView.class);
			createTreeItemFromIViewClass(SIDEBAR_SECTION_TOOLS, "Config",
					ConfigView.class);
		}

		if (SHOW_ALL_PLUGINS) {
			TreeItem itemPlugins = createTreeItemFromSkinRef(null,
					SIDEBAR_SECTION_PLUGINS, "main.area.plugins", "Plugins", null, null,
					false);

			IViewInfo[] pluginViewsInfo = PluginsMenuHelper.getInstance().getPluginViewsInfo();
			for (int i = 0; i < pluginViewsInfo.length; i++) {
				IViewInfo viewInfo = pluginViewsInfo[i];
				treeItem = new TreeItem(itemPlugins, SWT.NONE);
				treeItem.addDisposeListener(disposeTreeItemListener);

				treeItem.setText(viewInfo.name);
				treeItem.setData("Plugin.viewID", viewInfo.viewID);
				SideBarInfo sideBarInfo = getSideBarInfo(viewInfo.viewID);
				sideBarInfo.treeItem = treeItem;
				sideBarInfo.iview = viewInfo.view;
				sideBarInfo.eventListener = viewInfo.event_listener;
			}

			TreeItem itemPluginLogs = new TreeItem(itemPlugins, SWT.NONE);
			itemPluginLogs.setText("Log Views");
			IViewInfo[] pluginLogViewsInfo = PluginsMenuHelper.getInstance().getPluginLogViewsInfo();
			for (int i = 0; i < pluginLogViewsInfo.length; i++) {
				IViewInfo viewInfo = pluginLogViewsInfo[i];
				treeItem = new TreeItem(itemPluginLogs, SWT.NONE);
				treeItem.addDisposeListener(disposeTreeItemListener);

				treeItem.setText(viewInfo.name);
				treeItem.setData("Plugin.viewID", viewInfo.viewID);
				SideBarInfo sideBarInfo = getSideBarInfo(viewInfo.viewID);
				sideBarInfo.treeItem = treeItem;
				sideBarInfo.iview = viewInfo.view;
				sideBarInfo.eventListener = viewInfo.event_listener;
			}
		}

		UISWTInstanceImpl uiSWTInstance = (UISWTInstanceImpl) UIFunctionsManagerSWT.getUIFunctionsSWT().getUISWTInstance();
		if (uiSWTInstance != null) {
			Map allViews = uiSWTInstance.getAllViews();
			Object[] parentIDs = allViews.keySet().toArray();
			for (int i = 0; i < parentIDs.length; i++) {
				String parentID = (String) parentIDs[i];
				Map mapSubViews = (Map) allViews.get(parentID);
				if (mapSubViews != null) {
					Object[] viewIDs = mapSubViews.keySet().toArray();
					for (int j = 0; j < viewIDs.length; j++) {
						String viewID = (String) viewIDs[j];
						UISWTViewEventListener l = (UISWTViewEventListener) mapSubViews.get(viewID);
						if (l != null) {
							// TODO: Datasource
							// TODO: Multiple open

							boolean open = COConfigurationManager.getBooleanParameter(
									"SideBar.AutoOpen." + viewID, false);
							if (open) {
								createTreeItemFromEventListener(parentID, null, l, viewID,
										true, null);
							}
						}
					}
				}
			}
		}

		PluginsMenuHelper.getInstance().addPluginAddedViewListener(
				new PluginAddedViewListener() {
					// @see org.gudy.azureus2.ui.swt.mainwindow.PluginsMenuHelper.PluginAddedViewListener#pluginViewAdded(org.gudy.azureus2.ui.swt.mainwindow.PluginsMenuHelper.IViewInfo)
					public void pluginViewAdded(IViewInfo viewInfo) {
						System.out.println("PluginView Added: " + viewInfo.viewID);
						Object o = mapAutoOpen.get(viewInfo.viewID);
						if (o instanceof Map) {
							processAutoOpenMap(viewInfo.viewID, (Map) o, viewInfo);
						}
					}
				});

		loadCloseables();

		if (System.getProperty("v3.sidebar.advanced", "0").equals("1")) {
			createTreeItemFromSkinRef(null, SIDEBAR_SECTION_ADVANCED,
					"main.area.advancedtab", "Advanced", null, null, false);
		}
	}

	public TreeItem createTreeItemFromIView(String parentID, IView iview,
			String id, Object datasource, boolean closeable, boolean show) {
		if (id == null) {
			id = iview.getClass().getName();
			int i = id.lastIndexOf('.');
			if (i > 0) {
				id = id.substring(i + 1);
			}
		}
		TreeItem treeItem = null;

		SideBarInfo sideBarInfo = getSideBarInfo(id);

		if (sideBarInfo.treeItem != null) {
			treeItem = sideBarInfo.treeItem;
		} else {
			SideBarInfo sideBarInfoParent = getSideBarInfo(parentID);
			TreeItem parentTreeItem = sideBarInfoParent.treeItem;
			treeItem = createTreeItem(parentTreeItem, id, datasource, null,
					iview.getFullTitle(), closeable);

			setupTreeItem(null, treeItem, id, null, iview.getFullTitle(), null,
					datasource, closeable);

			sideBarInfo.parentID = parentID;

			createSideBarContentArea(id, iview, treeItem, datasource, closeable);

			iview.dataSourceChanged(datasource);
		}

		if (show) {
			showTreeItem(treeItem);
		}

		return treeItem;
	}

	public void showTreeItem(TreeItem treeItem) {
		if (treeItem != null) {
			treeItem.getParent().select(treeItem);
			treeItem.getParent().showItem(treeItem);
			itemSelected(treeItem);
		}
	}

	public TreeItem createTreeItemFromIViewClass(String parent, String title,
			Class iviewClass) {
		String id = iviewClass.getName();
		int i = id.lastIndexOf('.');
		if (i > 0) {
			id = id.substring(i + 1);
		}

		return createTreeItemFromIViewClass(parent, id, title, iviewClass, null,
				null, null, null, true);
	}

	public TreeItem createTreeItemFromIViewClass(String parent, String id,
			String title, Class iviewClass, Class[] iviewClassArgs,
			Object[] iviewClassVals, Object datasource, ViewTitleInfo titleInfo,
			boolean closeable) {

		SideBarInfo sideBarInfo = getSideBarInfo(id);
		if (sideBarInfo.treeItem != null) {
			return sideBarInfo.treeItem;
		}

		SideBarInfo sideBarInfoParent = getSideBarInfo(parent);
		TreeItem parentItem = sideBarInfoParent.treeItem;

		TreeItem treeItem;
		if (parentItem != null) {
			treeItem = new TreeItem(parentItem, SWT.NONE);
		} else {
			treeItem = new TreeItem(tree, SWT.NONE);
		}

		sideBarInfo.iviewClass = iviewClass;
		sideBarInfo.iviewClassArgs = iviewClassArgs;
		sideBarInfo.iviewClassVals = iviewClassVals;
		sideBarInfo.closeable = closeable;
		sideBarInfo.parentID = parent;
		setupTreeItem(null, treeItem, id, titleInfo, title, null, datasource,
				closeable);

		return treeItem;
	}

	private TreeItem createTreeItem(Object parentTreeItem, String id,
			Object datasource, ViewTitleInfo titleInfo, String title,
			boolean closeable) {
		TreeItem treeItem;

		if (parentTreeItem == null) {
			parentTreeItem = tree;
		}

		if (parentTreeItem instanceof Tree) {
			treeItem = new TreeItem((Tree) parentTreeItem, SWT.NONE);
		} else {
			treeItem = new TreeItem((TreeItem) parentTreeItem, SWT.NONE);
		}

		return treeItem;
	}

	private void setupTreeItem(IView iview, TreeItem treeItem, String id,
			ViewTitleInfo titleInfo, String title, Composite initializeView,
			Object datasource, boolean closeable) {
		SideBarInfo sideBarInfo = getSideBarInfo(id);

		boolean pull = true;
		if (treeItem.getParentItem() != null) {
			treeItem.getParentItem().setExpanded(true);
		}
		treeItem.removeDisposeListener(disposeTreeItemListener);
		treeItem.addDisposeListener(disposeTreeItemListener);
		treeItem.setData("Plugin.viewID", id);

		if (iview != null) {
			sideBarInfo.iview = iview;
		}

		if (title != null) {
			treeItem.setText(title);
		} else {
			if (sideBarInfo.iview != null) {
				treeItem.setText(sideBarInfo.iview.getFullTitle());
			}
		}

		if (titleInfo == null) {
			titleInfo = (sideBarInfo.iview instanceof ViewTitleInfo)
			? (ViewTitleInfo) sideBarInfo.iview : null;
		}

		if (titleInfo != null) {
			sideBarInfo.titleInfo = titleInfo;
		}

		
		if (sideBarInfo.titleInfo != null) {
			mapTitleInfoToTreeItem.put(sideBarInfo.titleInfo, treeItem);
			String newText = sideBarInfo.titleInfo.getTitleInfoStringProperty(ViewTitleInfo.TITLE_TEXT);
			if (newText != null) {
				pull = false;
				treeItem.setText(newText);
			}
			listTreeItemsNoTitleInfo.remove(treeItem);
		} else {
			if (!listTreeItemsNoTitleInfo.contains(treeItem)) {
				listTreeItemsNoTitleInfo.add(treeItem);
			}
		}

		if (treeItem != null) {
			sideBarInfo.treeItem = treeItem;
		}
		if (datasource != null) {
			sideBarInfo.datasource = datasource;
		}

		sideBarInfo.closeable = closeable;

		sideBarInfo.pullTitleFromIView = pull;

		if (closeable) {
			Map autoOpenInfo = new LightHashMap();
			if (sideBarInfo.parentID != null) {
				autoOpenInfo.put("parentID", sideBarInfo.parentID);
			}
			if (sideBarInfo.iviewClass != null) {
				autoOpenInfo.put("iviewClass", sideBarInfo.iviewClass.getName());
			}
			if (sideBarInfo.eventListener != null) {
				autoOpenInfo.put("eventlistenerid", id);
			}
			if (sideBarInfo.iview != null) {
				autoOpenInfo.put("title", sideBarInfo.iview.getFullTitle());
			}
			if (sideBarInfo.datasource instanceof DownloadManager) {
				try {
					autoOpenInfo.put(
							"dm",
							((DownloadManager) sideBarInfo.datasource).getTorrent().getHashWrapper().toBase32String());
				} catch (Throwable t) {
				}
			}

			mapAutoOpen.put(id, autoOpenInfo);
		}

		if (initializeView != null) {
			iview.initialize(initializeView);
			if (sideBarInfo.datasource != null) {
				iview.dataSourceChanged(sideBarInfo.datasource);
			}
		}
	}

	private static SideBarInfo getSideBarInfo(String id) {
		SideBarInfo sidebarInfo = (SideBarInfo) mapIdToSideBarInfo.get(id);
		if (sidebarInfo == null) {
			sidebarInfo = new SideBarInfo();
			mapIdToSideBarInfo.put(id, sidebarInfo);
		}
		return sidebarInfo;
	}

	//	private SideBarInfo getSideBarInfo(IView iview) {
	//		SideBarInfo sidebarInfo = (SideBarInfo) mapIViewToSideBarInfo.get(iview);
	//		if (sidebarInfo == null) {
	//			sidebarInfo = new SideBarInfo();
	//			mapIViewToSideBarInfo.put(iview, sidebarInfo);
	//		}
	//		return sidebarInfo;
	//	}

	public boolean showItemByID(String id) {
		SideBarInfo sideBarInfo = getSideBarInfo(id);
		if (sideBarInfo.treeItem != null) {
			itemSelected(sideBarInfo.treeItem);
			return true;
		}
		if (id.equals(SIDEBAR_SECTION_ADVANCED)) {
			TreeItem treeItem = createTreeItemFromSkinRef(null,
					SIDEBAR_SECTION_ADVANCED, "main.area.advancedtab", "Advanced", null,
					null, false);
			itemSelected(treeItem);
			return true;
		}
		return false;
	}

	/**
	 * @param treeItem
	 *
	 * @since 3.1.1.1
	 */
	private void itemSelected(TreeItem treeItem) {
		TreeItem[] selection = tree.getSelection();
		if (selection == null || selection.length == 0 || selection[0] != treeItem) {
			tree.showItem(treeItem);
			tree.select(treeItem);
		}
		final String id = (String) treeItem.getData("Plugin.viewID");
		SideBarInfo sideBarInfo = getSideBarInfo(id);

		// We'll have an iview if we've previously created one
		IView iview = sideBarInfo.iview;

		// Otherwise, we have two ways of creating an IView.. via IViewClass,
		// or UISWTViewEventListener

		if (iview == null) {
			if (sideBarInfo.iviewClass != null) {
				iview = createSideBarContentArea(id, sideBarInfo);

				if (iview == null) {
					return;
				}

				ViewTitleInfo titleInfo = (iview instanceof ViewTitleInfo)
						? (ViewTitleInfo) iview : null;
				setupTreeItem(iview, treeItem,
						(String) treeItem.getData("Plugin.viewID"), titleInfo,
						iview.getFullTitle(), null, null, sideBarInfo.closeable);
			}
		}

		if (iview == null && sideBarInfo.eventListener != null) {
			iview = createTreeItemFromEventListener(null, treeItem,
					sideBarInfo.eventListener, id, sideBarInfo.closeable, null);
		}

		if (iview != null) {
			IView oldView = currentIView;
			String oldID = currentIViewID;

			// hide old
			if (oldView != null) {
				SideBarInfo oldSideBarInfo = getSideBarInfo(oldID);
				if (oldSideBarInfo.skinObject != null) {
					SWTSkinObjectContainer container = (SWTSkinObjectContainer) oldSideBarInfo.skinObject;
					if (container != null) {
						container.setVisible(false);
					}
				}
				Composite oldComposite = oldView.getComposite();
				if (oldComposite != null && !oldComposite.isDisposed()) {
					oldView.getComposite().setVisible(false);
				}
			}

			// show new
			currentIView = iview;
			currentIViewID = (String) treeItem.getData("Plugin.viewID");
			SWTSkinObjectContainer container = (SWTSkinObjectContainer) sideBarInfo.skinObject;
			if (container != null) {
				Composite composite = container.getComposite();
				if (composite != null && !composite.isDisposed()) {
					composite.setVisible(true);
					composite.moveAbove(null);
					composite.setFocus();
				}
			}
			Composite c = currentIView.getComposite();
			if (c != null && !c.isDisposed()) {
				c.setVisible(true);
			}

			triggerListener(currentIView, currentIViewID, oldView, oldID);
		}
	}

	/**
	 * @param parentID 
	 * @param l
	 * @param datasource 
	 * @return
	 *
	 * @since 3.1.1.1
	 */
	public IView createTreeItemFromEventListener(String parentID,
			TreeItem treeItem, UISWTViewEventListener l, String id,
			boolean closeable, Object datasource) {

		final String originialID = id;
		SideBarInfo sideBarInfo = getSideBarInfo(id);

		if (sideBarInfo.treeItem != null && sideBarInfo.iview != null) {
			if (sideBarInfo.eventListener == l) {
				System.err.println("Already created view " + id);
			}
			id = id + "_" + SystemTime.getCurrentTime();
			sideBarInfo = getSideBarInfo(id);
		}

		String name = sideBarInfo.treeItem == null ? id
				: sideBarInfo.treeItem.getText();

		if (treeItem == null) {
			SideBarInfo sideBarInfoParent = getSideBarInfo(parentID);
			TreeItem parentTreeItem = sideBarInfoParent.treeItem;

			IViewInfo foundViewInfo = null;
			IViewInfo[] pluginViewsInfo = PluginsMenuHelper.getInstance().getPluginViewsInfo();
			for (int i = 0; i < pluginViewsInfo.length; i++) {
				IViewInfo viewInfo = pluginViewsInfo[i];
				if (viewInfo.event_listener == l) {
					foundViewInfo = viewInfo;
					break;
				}
			}
			if (foundViewInfo != null) {
				IViewInfo[] pluginLogViewsInfo = PluginsMenuHelper.getInstance().getPluginLogViewsInfo();
				for (int i = 0; i < pluginLogViewsInfo.length; i++) {
					IViewInfo viewInfo = pluginLogViewsInfo[i];
					if (viewInfo.event_listener == l) {
						foundViewInfo = viewInfo;
						break;
					}
				}
			}

			name = foundViewInfo == null ? id : foundViewInfo.name;

			sideBarInfo.eventListener = l;
			sideBarInfo.parentID = parentID;
			treeItem = createTreeItem(parentTreeItem, id, datasource, null, name,
					closeable);
		}

		IView iview = null;
		try {
			iview = new UISWTViewImpl("SideBar.Plugins", id, l, datasource);
			((UISWTViewImpl) iview).setTitle(name);
			iview.dataSourceChanged(datasource);

			if (l instanceof UISWTViewEventListenerFormLayout) {
				((UISWTViewImpl) iview).setUseCoreDataSource(true);
			}

			Composite parent = (Composite) soSideBarContents.getControl();
			parent.setBackgroundMode(SWT.INHERIT_NONE);

			Composite viewComposite = new Composite(parent, SWT.NONE);
			viewComposite.setBackgroundMode(SWT.INHERIT_NONE);
			viewComposite.setLayoutData(Utils.getFilledFormData());
			viewComposite.setBackground(parent.getDisplay().getSystemColor(
					SWT.COLOR_WIDGET_BACKGROUND));
			viewComposite.setForeground(parent.getDisplay().getSystemColor(
					SWT.COLOR_WIDGET_FOREGROUND));
			if (l instanceof UISWTViewEventListenerFormLayout) {
				viewComposite.setLayout(new FormLayout());
			} else {
				GridLayout gridLayout = new GridLayout();
				gridLayout.horizontalSpacing = gridLayout.verticalSpacing = gridLayout.marginHeight = gridLayout.marginWidth = 0;
				viewComposite.setLayout(gridLayout);
			}

			SWTSkinObjectContainer soContents = new SWTSkinObjectContainer(skin,
					skin.getSkinProperties(), viewComposite, "Contents"
							+ (mapIdToSideBarInfo.size() + 1), "", "container",
					soSideBarContents);

			sideBarInfo.skinObject = soContents;
			sideBarInfo.eventListener = l;
			sideBarInfo.parentID = parentID;

			ViewTitleInfo titleInfo = (iview instanceof ViewTitleInfo)
					? (ViewTitleInfo) iview : null;
			setupTreeItem(iview, treeItem, id, titleInfo, iview.getFullTitle(),
					viewComposite, datasource, closeable);

			parent.layout(true, true);

			if (closeable) {
				COConfigurationManager.setParameter("SideBar.AutoOpen." + id, true);
			} else {
				COConfigurationManager.removeParameter("SideBar.AutoOpen." + id);
			}

		} catch (UISWTViewEventCancelledException e) {
			if (treeItem != null && !treeItem.isDisposed()) {
				treeItem.dispose();
			}
			showItemByID(originialID);
		} catch (Exception e) {
			e.printStackTrace();
			try {
				final String id2 = id;
				l = new UISWTViewEventListener() {
					public boolean eventOccurred(UISWTViewEvent event) {
						if (event.getType() == UISWTViewEvent.TYPE_INITIALIZE) {
							Composite c = (Composite) event.getData();
							Label label = new Label(c, SWT.CENTER);
							label.setText("Plugin " + id2 + " did not want to initialize");
						}
						return true;
					}
				};
				iview = new UISWTViewImpl("SideBar.Plugins", id, l, datasource);
				((UISWTViewImpl) iview).setTitle(treeItem.getText());

				Composite parent = (Composite) soSideBarContents.getControl();
				Composite viewComposite = new Composite(parent, SWT.NONE);
				viewComposite.setLayoutData(Utils.getFilledFormData());
				viewComposite.setLayout(new FormLayout());
				viewComposite.setBackground(parent.getDisplay().getSystemColor(
						SWT.COLOR_WIDGET_BACKGROUND));
				viewComposite.setForeground(parent.getDisplay().getSystemColor(
						SWT.COLOR_WIDGET_FOREGROUND));
				SWTSkinObjectContainer soContents = new SWTSkinObjectContainer(skin,
						skin.getSkinProperties(), viewComposite, "Contents"
								+ (mapIdToSideBarInfo.size() + 1), "", "container",
						soSideBarContents);

				sideBarInfo.skinObject = soContents;
				sideBarInfo.eventListener = l;

				setupTreeItem(iview, treeItem, id, null, iview.getFullTitle(),
						viewComposite, datasource, closeable);

				parent.layout(true, true);
			} catch (Exception e1) {
				Debug.out(e1);
			}

			if (!(e instanceof UISWTViewEventCancelledException)) {
				Debug.out(e);
			}
		}
		return iview;
	}

	/**
	 * Creates an IView based on class.  Doesn't add it to the tree
	 * @param id 
	 * 
	 * @param iview
	 * @param iviewClass
	 * @param args
	 *
	 * @since 3.1.0.1
	 */
	private IView createSideBarContentArea(String id, SideBarInfo sideBarInfo) {
		if (id == null) {
			return null;
		}
		IView iview = null;
		try {
			if (sideBarInfo.iviewClassArgs == null) {
				iview = (IView) sideBarInfo.iviewClass.newInstance();
			} else {
				Constructor constructor = sideBarInfo.iviewClass.getConstructor(sideBarInfo.iviewClassArgs);
				iview = (IView) constructor.newInstance(sideBarInfo.iviewClassVals);
			}

			createSideBarContentArea(id, iview, sideBarInfo.treeItem,
					sideBarInfo.datasource, sideBarInfo.closeable);
		} catch (Exception e) {
			e.printStackTrace();
			if (iview != null) {
				iview.delete();
			}
			iview = null;
		}

		return iview;
	}

	/**
	 * Take an already created IView and put it into a new Container Skin Object.
	 * Doesn't add it to the tree
	 * @param id 
	 *  
	 * @param view
	 * @param closeable 
	 * @return
	 *
	 * @since 3.1.1.1
	 */
	private IView createSideBarContentArea(String id, IView view, TreeItem item,
			Object datasource, boolean closeable) {
		try {
			Composite parent = (Composite) soSideBarContents.getControl();

			SWTSkinObjectContainer soContents = new SWTSkinObjectContainer(skin,
					skin.getSkinProperties(), "Contents"
							+ (mapIdToSideBarInfo.size() + 1), "", soSideBarContents);

			Composite viewComposite = soContents.getComposite();
			viewComposite.setBackground(parent.getDisplay().getSystemColor(
					SWT.COLOR_WIDGET_BACKGROUND));
			viewComposite.setForeground(parent.getDisplay().getSystemColor(
					SWT.COLOR_WIDGET_FOREGROUND));
			viewComposite.setLayoutData(Utils.getFilledFormData());
			GridLayout gridLayout = new GridLayout();
			gridLayout.horizontalSpacing = gridLayout.verticalSpacing = gridLayout.marginHeight = gridLayout.marginWidth = 0;
			viewComposite.setLayout(gridLayout);

			SideBarInfo sideBarInfo = getSideBarInfo((String) item.getData("Plugin.viewID"));
			sideBarInfo.skinObject = soContents;
			
			setupTreeItem(view, item, id, null, null, viewComposite, datasource, closeable);

			Composite iviewComposite = view.getComposite();
			Object existingLayout = iviewComposite.getLayoutData();
			if (existingLayout == null || (existingLayout instanceof GridData)) {
				GridData gridData = new GridData(GridData.FILL_BOTH);
				iviewComposite.setLayoutData(gridData);
			}

			parent.layout(true, true);

		} catch (Exception e) {
			e.printStackTrace();
			if (view != null) {
				view.delete();
			}
			view = null;
		}
		return view;
	}

	public TreeItem createTreeItemFromSkinRef(String parentID, final String id,
			final String configID, String title, ViewTitleInfo titleInfo,
			final Object params, boolean closeable) {

		// temp until we start passing ds
		Object datasource = null;

		SideBarInfo sideBarInfo = getSideBarInfo(id);
		if (sideBarInfo.treeItem != null) {
			return sideBarInfo.treeItem;
		}

		TreeItem treeItem = null;

		UISWTViewEventListener l = new UISWTViewEventListenerFormLayout() {
			private SWTSkinObject skinObject;

			public boolean eventOccurred(UISWTViewEvent event) {
				switch (event.getType()) {
					case UISWTViewEvent.TYPE_INITIALIZE: {
						Composite parent = (Composite) event.getData();
						SWTSkinObject soParent = (SWTSkinObject) parent.getData("SkinObject");
						Shell shell = parent.getShell();
						Cursor cursor = shell.getCursor();
						try {
							shell.setCursor(shell.getDisplay().getSystemCursor(
									SWT.CURSOR_WAIT));

							skinObject = skin.createSkinObject(id, configID, soParent, params);
							skinObject.setVisible(true);
							skinObject.getControl().setLayoutData(Utils.getFilledFormData());
						} finally {
							shell.setCursor(cursor);
						}
					}
						break;
					case UISWTViewEvent.TYPE_REFRESH: {

					}
				}
				return true;
			}
		};
		sideBarInfo.eventListener = l;

		SideBarInfo sideBarInfoParent = getSideBarInfo(parentID);
		TreeItem parentTreeItem = sideBarInfoParent.treeItem;

		sideBarInfo.parentID = parentID;

		treeItem = createTreeItem(parentTreeItem == null ? (Object) tree
				: (Object) parentTreeItem, id, datasource, titleInfo, title, closeable);

		setupTreeItem(null, treeItem, id, titleInfo, title, null, datasource,
				closeable);

		return treeItem;
	}

	// @see com.aelitis.azureus.ui.swt.utils.UIUpdatable#getUpdateUIName()
	public String getUpdateUIName() {
		if (currentIView == null) {
			return "Sidebar";
		}
		if (currentIView instanceof UIPluginView) {
			UIPluginView uiPluginView = (UIPluginView) currentIView;
			return uiPluginView.getViewID();
		}

		return currentIView.getFullTitle();
	}

	// @see com.aelitis.azureus.ui.swt.utils.UIUpdatable#updateUI()
	public void updateUI() {
		if (currentIView == null) {
			return;
		}
		currentIView.refresh();
		SideBarInfo sidebarInfo = getSideBarInfo(currentIViewID);
		if (sidebarInfo.pullTitleFromIView && sidebarInfo.treeItem != null) {
			sidebarInfo.treeItem.setText(currentIView.getFullTitle());
		}
	}

	public static interface UISWTViewEventListenerFormLayout
		extends UISWTViewEventListener
	{
	}

	// @see com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoListener#viewTitleInfoRefresh(com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo)
	public void viewTitleInfoRefresh(final ViewTitleInfo titleIndicator) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (tree.isDisposed()) {
					return;
				}

				TreeItem treeItem = (TreeItem) mapTitleInfoToTreeItem.get(titleIndicator);
				if (treeItem == null) {
					Object o = titleIndicator.getTitleInfoObjectProperty(ViewTitleInfo.TITLE_SKINVIEW);
					if (o instanceof SkinView) {
						SkinView skinView = (SkinView) o;
						String id = skinView.getMainSkinObject().getSkinObjectID();
						if (id != null) {
							for (Iterator iter = listTreeItemsNoTitleInfo.iterator(); iter.hasNext();) {
								TreeItem searchTreeItem = (TreeItem) iter.next();
								String treeItemID = (String) searchTreeItem.getData("Plugin.viewID");
								if (treeItemID != null && treeItemID.equals(id)) {
									SideBarInfo sideBarInfo = getSideBarInfo(treeItemID);
									if (sideBarInfo.treeItem != null) {
										sideBarInfo.titleInfo = titleIndicator;
										treeItem = sideBarInfo.treeItem;
										mapTitleInfoToTreeItem.put(titleIndicator, treeItem);
									}
									break;
								}
							}
						}
					}

					if (treeItem == null) {
						return;
					}
				}

				if (treeItem.isDisposed()) {
					return;
				}

				String newText = titleIndicator.getTitleInfoStringProperty(ViewTitleInfo.TITLE_TEXT);
				if (newText != null) {
					String id = (String) treeItem.getData("Plugin.viewID");
					SideBarInfo sideBarInfo = getSideBarInfo(id);
					sideBarInfo.pullTitleFromIView = false;
					treeItem.setText(newText);
				}

				Rectangle bounds = treeItem.getBounds();
				Rectangle treeBounds = tree.getBounds();
				tree.redraw(0, bounds.y, treeBounds.width, bounds.height, true);
				tree.update();
			}
		});
	}

	public IView getCurrentIView() {
		return currentIView;
	}

	public String getCurrentViewID() {
		return currentIViewID == null ? "" : currentIViewID;
	}

	public void addListener(SideBarListener l) {
		if (listeners.contains(l)) {
			return;
		}
		listeners.add(l);
	}

	public void removeListener(SideBarListener l) {
		listeners.remove(l);
	}

	private void triggerListener(IView view, String id, IView oldView,
			String oldID) {
		for (Iterator iter = listeners.iterator(); iter.hasNext();) {
			SideBarListener l = (SideBarListener) iter.next();
			l.sidebarItemSelected(view, id, oldView, oldID);
		}
	}

	private static class SideBarInfo
	{
		public String parentID;

		public Object datasource;

		public ViewTitleInfo titleInfo;

		SWTSkinObject skinObject;

		TreeItem treeItem;

		boolean pullTitleFromIView;

		IView iview;

		boolean closeable;

		UISWTViewEventListener eventListener;

		Class iviewClass;

		Class[] iviewClassArgs;

		Object[] iviewClassVals;
	}

	public IView getIViewFromID(String id) {
		if (id == null) {
			return null;
		}
		return getSideBarInfo(id).iview;
	}

	public void saveCloseables() {
		// update title
		for (Iterator iter = mapAutoOpen.keySet().iterator(); iter.hasNext();) {
			String id = (String) iter.next();
			Object o = mapAutoOpen.get(id);

			if (o instanceof Map) {

				SideBarInfo sideBarInfo = getSideBarInfo(id);
				Map autoOpenInfo = (Map) o;

				if (sideBarInfo.treeItem != null) {
					autoOpenInfo.put("title", sideBarInfo.treeItem.getText());
				}

			}
		}

		FileUtil.writeResilientConfigFile("sidebarauto.config", mapAutoOpen);
	}

	public void loadCloseables() {
		mapAutoOpen = FileUtil.readResilientConfigFile("sidebarauto.config", true);
		BDecoder.decodeStrings(mapAutoOpen);
		for (Iterator iter = mapAutoOpen.keySet().iterator(); iter.hasNext();) {
			String id = (String) iter.next();
			Object o = mapAutoOpen.get(id);

			if (o instanceof Map) {
				processAutoOpenMap(id, (Map) o, null);
			}
		}
	}

	/**
	 * @param viewInfo 
	 * @param o
	 *
	 * @since 3.1.1.1
	 */
	private void processAutoOpenMap(String id, Map autoOpenInfo,
			IViewInfo viewInfo) {
		try {
			SideBarInfo sideBarInfo = getSideBarInfo(id);
			if (sideBarInfo.treeItem != null) {
				return;
			}

			String title = MapUtils.getMapString(autoOpenInfo, "title", id);
			String parentID = (String) autoOpenInfo.get("parentID");

			if (viewInfo != null) {
				if (viewInfo.view != null) {
					createTreeItemFromIView(parentID, viewInfo.view, id, null, true,
							false);
				} else if (viewInfo.event_listener != null) {
					createTreeItemFromEventListener(parentID, null,
							viewInfo.event_listener, id, true, null);
				}
				if (sideBarInfo.iview == null) {
					createSideBarContentArea(id, sideBarInfo);
				}
			}

			Class cla = Class.forName(MapUtils.getMapString(autoOpenInfo,
					"iviewClass", ""));
			if (cla != null) {
				String dmHash = MapUtils.getMapString(autoOpenInfo, "dm", null);
				Object ds = null;
				if (dmHash != null) {
					HashWrapper hw = new HashWrapper(Base32.decode(dmHash));
					GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
					ds = gm.getDownloadManager(hw);
				}
				createTreeItemFromIViewClass(parentID, id, title, cla, null, null, ds,
						null, true);
				
				if (sideBarInfo.iview == null) {
					createSideBarContentArea(id, sideBarInfo);
				}
			}
		} catch (ClassNotFoundException ce) {
			// ignore
		} catch (Throwable e) {
			Debug.out(e);
		}
	}
}
