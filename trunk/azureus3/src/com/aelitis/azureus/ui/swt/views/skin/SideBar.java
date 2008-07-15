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

package com.aelitis.azureus.ui.swt.views.skin;

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

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.download.impl.DownloadManagerAdapter;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerAdapter;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.PluginsMenuHelper;
import org.gudy.azureus2.ui.swt.mainwindow.PluginsMenuHelper.IViewInfo;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewImpl;
import org.gudy.azureus2.ui.swt.views.*;
import org.gudy.azureus2.ui.swt.views.stats.StatsView;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.activities.VuzeActivitiesListener;
import com.aelitis.azureus.activities.VuzeActivitiesManager;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoListener;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.utils.ColorCache;

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

	private SWTSkin skin;

	private SWTSkinObject soSideBarContents;

	private SWTSkinObject soSideBarList;

	private Tree tree;

	private Font fontHeader;

	private IView currentIView;

	private static Map mapIViewToSkinObject = new HashMap();

	private static Map mapTitleInfoToTreeItem = new HashMap();

	private static Map mapIdToTreeItem = new HashMap();

	private static List listTreeItemsNoTitleInfo = new ArrayList();

	private static DisposeListener disposeTreeItemListener;

	private int numSeeding = 0;

	private int numDownloading = 0;

	private int numComplete = 0;

	private int numIncomplete = 0;

	static {
		disposeTreeItemListener = new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				TreeItem treeItem = (TreeItem) e.widget;
				String id = (String) treeItem.getData("Plugin.viewID");

				if (id != null) {
					mapIdToTreeItem.remove(id);
					return;
				}

				IView iview = (IView) treeItem.getData("IView");
				if (iview != null) {
					mapIdToTreeItem.remove(iview.getClass().getName());
					return;
				}

				for (Iterator iter = mapIdToTreeItem.keySet().iterator(); iter.hasNext();) {
					id = (String) iter.next();
					TreeItem item = (TreeItem) mapIdToTreeItem.get(id);
					if (item == treeItem) {
						iter.remove();
					}
				}

			}
		};
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#showSupport(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		skin = skinObject.getSkin();

		soSideBarContents = skin.getSkinObject("sidebar-contents");
		soSideBarList = skin.getSkinObject("sidebar-list");

		setupList();

		try {
			UIFunctionsManager.getUIFunctions().getUIUpdater().addUpdater(this);
		} catch (Exception e) {
			Debug.out(e);
		}

		ViewTitleInfoManager.addListener(this);

		soSideBarList.getControl().getDisplay().addFilter(SWT.KeyDown,
				new Listener() {
					double lastPercent;

					public void handleEvent(Event event) {
						if (event.keyCode == SWT.F9 && event.stateMask == 0) {
							event.doit = false;
							Utils.execSWTThreadLater(0, new AERunnable() {
								public void runSupport() {
									SWTSkinObjectSash soSash = (SWTSkinObjectSash) skin.getSkinObject("sidebar-sash");
									if (soSash.getPercent() == 100) {
										if (lastPercent != 0) {
											soSash.setPercent(lastPercent);
										}
									} else {
										lastPercent = soSash.getPercent();
										soSash.setPercent(100);
									}
								}
							});
						}
					}
				});

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

		tree = new Tree(parent, SWT.FULL_SELECTION);
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
		fontData[0].setHeight(fontData[0].getHeight() + 1);
		fontData[0].setStyle(SWT.BOLD);
		fontHeader = new Font(tree.getDisplay(), fontData);
		tree.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				fontHeader.dispose();
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
						TreeItem item = (TreeItem) event.item;
						String text = item.getText(event.index);
						Point size = event.gc.textExtent(text);
						Rectangle treeBounds = tree.getBounds();

						event.gc.setClipping((Rectangle) null);

						boolean selected = tree.getSelectionCount() == 1
								&& tree.getSelection()[0].equals(item);
						Color curBG;
						if (selected) {
							event.gc.setForeground(colorFocus);
							event.gc.drawRectangle(0, event.y, treeBounds.width - 1,
									event.height - 1);
							event.gc.setForeground(fgSel);
							event.gc.setBackground(bgSel);
							event.gc.setBackground(ColorCache.getColor(event.gc.getDevice(),
									"#607080"));
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

						if (item.getItemCount() > 0) {
							if (item.getExpanded()) {
								event.gc.drawText("V", event.x - 16, event.y + 6, true);
							} else {
								event.gc.drawText(">", event.x - 16, event.y + 6, true);
							}
							event.gc.setFont(fontHeader);
						}

						Rectangle itemBounds = tree.getClientArea();

						event.gc.drawText(text, event.x, event.y + 6, true);

						event.gc.setFont(tree.getFont());

						ViewTitleInfo titleInfo = (ViewTitleInfo) item.getData("TitleInfo");
						if (titleInfo != null) {
							String textIndicator = titleInfo.getTitleInfoStringProperty(ViewTitleInfo.TITLE_INDICATOR_TEXT);
							if (textIndicator != null) {
								Point textSize = event.gc.textExtent(textIndicator);
								int x = itemBounds.width - textSize.x - 10;
								int y = event.y + 6;
								event.gc.setForeground(Colors.blues[Colors.BLUES_LIGHTEST]);
								event.gc.setBackground(Colors.faded[Colors.BLUES_DARKEST]);
								event.gc.setAntialias(SWT.ON);
								event.gc.fillRoundRectangle(x - 5, y - 2, textSize.x + 10,
										textSize.y + 4, 10, textSize.y + 4);
								event.gc.drawText(textIndicator, x, y);
							}
						}

						Boolean closeableObj = (Boolean) item.getData("closeable");
						if (closeableObj != null && closeableObj.booleanValue()) {
							Image imgClose = ImageRepository.getImage("smallx");
							Rectangle closeArea = imgClose.getBounds();
							closeArea.x = itemBounds.width - closeArea.width - 2;
							closeArea.y = event.y + 4;
							event.gc.drawImage(imgClose, closeArea.x, closeArea.y);
							item.setData("closeArea", closeArea);
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
				TreeItem item = tree.getItem(new Point(event.x, event.y));
				if (item == null) {
					return;
				}
				Rectangle closeArea = (Rectangle) item.getData("closeArea");
				if (closeArea != null && closeArea.contains(event.x, event.y)) {
					IView iview = (IView) item.getData("IView");
					if (iview != null) {
						iview.delete();
					}
					item.dispose();
				}
			}
		});

		createTreeItems();

		TreeItem treeItem = tree.getItem(0);
		treeItem.getParent().select(treeItem);
		treeItem.getParent().showItem(treeItem);
		itemSelected(treeItem);

		parent.getShell().layout(true, true);
	}

	/**
	 * 
	 *
	 * @since 3.1.1.1
	 */
	private void createTreeItems() {
		TreeItem treeItem;

		createTreeItemFromSkinRef(tree, "Welcome_SB", "main.area.welcome",
				"Welcome", null, null);

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

		final TreeItem itemActivity = createTreeItemFromSkinRef(tree,
				"Activity_SB", "main.area.events", "Activity", titleInfoActivityView,
				null);

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

		TreeItem itemLibrary = createTreeItemFromSkinRef(tree,
				SIDEBAR_SECTION_LIBRARY, "library", "Library", titleInfoLibrary, null);

		createTreeItemFromSkinRef(itemLibrary, "LibraryDL_SB", "library",
				"Downloading", titleInfoDownloading, null);

		createTreeItemFromSkinRef(itemLibrary, "LibraryCD_SB", "library",
				"Seeding", titleInfoSeeding, null);

		TreeItem itemOnVuze = createTreeItemFromSkinRef(tree, "Browse_SB",
				"main.area.browsetab", "On Vuze", null, null);

		createTreeItemFromSkinRef(itemOnVuze, "Rec_SB", "main.area.rec",
				"Recommendations", null, null);

		createTreeItemFromSkinRef(itemOnVuze, "Publish_SB", "main.area.publishtab",
				"Publish", null, null);

		//new TreeItem(tree, SWT.NONE).setText("Search");

		createTreeItemFromSkinRef(tree, SIDEBAR_SECTION_TOOLS, "main.area.hood",
				"Under The Hood", null, null);

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

		TreeItem itemPlugins = createTreeItemFromSkinRef(tree,
				SIDEBAR_SECTION_PLUGINS, "main.area.plugins", "Plugins", null, null);

		IViewInfo[] pluginViewsInfo = PluginsMenuHelper.getInstance().getPluginViewsInfo();
		for (int i = 0; i < pluginViewsInfo.length; i++) {
			IViewInfo viewInfo = pluginViewsInfo[i];
			treeItem = new TreeItem(itemPlugins, SWT.NONE);
			treeItem.addDisposeListener(disposeTreeItemListener);

			treeItem.setText(viewInfo.name);
			treeItem.setData("UISWTViewEventListener", viewInfo.event_listener);
			treeItem.setData("Plugin.viewID", viewInfo.viewID);
			mapIdToTreeItem.put(viewInfo.viewID, treeItem);
		}

		TreeItem itemPluginLogs = new TreeItem(itemPlugins, SWT.NONE);
		itemPluginLogs.setText("Log Views");
		IViewInfo[] pluginLogViewsInfo = PluginsMenuHelper.getInstance().getPluginLogViewsInfo();
		for (int i = 0; i < pluginLogViewsInfo.length; i++) {
			IViewInfo viewInfo = pluginLogViewsInfo[i];
			treeItem = new TreeItem(itemPluginLogs, SWT.NONE);
			treeItem.addDisposeListener(disposeTreeItemListener);

			treeItem.setText(viewInfo.name);
			treeItem.setData("UISWTViewEventListener", viewInfo.event_listener);
			treeItem.setData("Plugin.viewID", viewInfo.viewID);
			mapIdToTreeItem.put(viewInfo.viewID, treeItem);
		}

		createTreeItemFromSkinRef(tree, "Advanced_SB", "main.area.advancedtab",
				"Advanced", null, null);
	}

	public TreeItem createAndShowTreeItem(IView view) {
		TreeItem treeItem = null;
		if (mapIdToTreeItem.containsKey(view.getClass().getName())) {
			treeItem = (TreeItem) mapIdToTreeItem.get(view.getClass().getName());
		} else {
			treeItem = new TreeItem(tree, SWT.NONE);
			mapIdToTreeItem.put(view.getClass().getName(), treeItem);
			createSideBarContentArea(view);

			if (view instanceof ViewTitleInfo) {
				treeItem.setData("TitleInfo", view);
				String newText = ((ViewTitleInfo) view).getTitleInfoStringProperty(ViewTitleInfo.TITLE_TEXT);
				if (newText != null) {
					treeItem.setText(newText);
				}
				listTreeItemsNoTitleInfo.remove(treeItem);
				mapTitleInfoToTreeItem.put(view, treeItem);
			}
		}

		if (treeItem != null) {
			treeItem.getParent().select(treeItem);
			treeItem.getParent().showItem(treeItem);
			itemSelected(treeItem);
		}

		return treeItem;
	}

	public TreeItem createTreeItemFromIViewClass(String parent, String title,
			Class iviewClass) {
		return createTreeItemFromIViewClass(parent, iviewClass.getName(), title,
				iviewClass, null, null, null, false);
	}

	public TreeItem createTreeItemFromIViewClass(String parent, String id,
			String title, Class iviewClass, Class[] iviewClassArgs,
			Object[] iviewClassVals, ViewTitleInfo titleInfo, boolean closeable) {

		if (mapIdToTreeItem.containsKey(id)) {
			return (TreeItem) mapIdToTreeItem.get(id);
		}

		TreeItem parentItem = (TreeItem) mapIdToTreeItem.get(parent);

		TreeItem treeItem;
		if (parentItem != null) {
			treeItem = new TreeItem(parentItem, SWT.NONE);
		} else {
			treeItem = new TreeItem(tree, SWT.NONE);
		}

		treeItem.setData("closeable", new Boolean(closeable));

		setupTreeItem(treeItem, id, title, iviewClass, iviewClassArgs,
				iviewClassVals);
		setupTreeItem(treeItem, id, titleInfo, title);

		return treeItem;
	}

	private TreeItem setupTreeItem(TreeItem treeItem, String id, String title,
			Class iviewClass, Class[] iviewClassArgs, Object[] iviewClassVals) {
		treeItem.setText(title);
		treeItem.setData("IViewClass", iviewClass);
		treeItem.setData("IViewClassArgs", iviewClassArgs);
		treeItem.setData("IViewClassVals", iviewClassVals);
		treeItem.setData("Plugin.viewID", id);

		return treeItem;
	}

	private TreeItem creatTreeItem(Object parentTreeItem, String id,
			ViewTitleInfo titleInfo, String title) {
		TreeItem treeItem;

		if (parentTreeItem instanceof Tree) {
			treeItem = new TreeItem((Tree) parentTreeItem, SWT.NONE);
		} else {
			treeItem = new TreeItem((TreeItem) parentTreeItem, SWT.NONE);
		}

		setupTreeItem(treeItem, id, titleInfo, title);

		return treeItem;
	}

	private void setupTreeItem(TreeItem treeItem, String id,
			ViewTitleInfo titleInfo, String title) {
		treeItem.addDisposeListener(disposeTreeItemListener);
		treeItem.setData("Plugin.viewID", id);
		if (titleInfo != null) {
			treeItem.setData("TitleInfo", titleInfo);
		}
		treeItem.setText(title);
		if (titleInfo != null) {
			mapTitleInfoToTreeItem.put(titleInfo, treeItem);
			String newText = titleInfo.getTitleInfoStringProperty(ViewTitleInfo.TITLE_TEXT);
			if (newText != null) {
				treeItem.setText(newText);
			}
			listTreeItemsNoTitleInfo.remove(treeItem);
		} else {
			listTreeItemsNoTitleInfo.add(treeItem);
		}

		mapIdToTreeItem.put(id, treeItem);
	}

	public boolean showItemByID(String id) {
		TreeItem treeItem = (TreeItem) mapIdToTreeItem.get(id);
		if (treeItem != null) {
			itemSelected(treeItem);
			return true;
		}
		return false;
	}

	/**
	 * @param treeItem
	 *
	 * @since 3.1.0.1
	 */
	private void itemSelected(TreeItem treeItem) {
		TreeItem[] selection = tree.getSelection();
		if (selection == null || selection.length == 0 || selection[0] != treeItem) {
			tree.showItem(treeItem);
			tree.select(treeItem);
		}

		// We'll have an "IView" if we've previously created one
		IView iview = (IView) treeItem.getData("IView");

		// Otherwise, we have two ways of creating an IView.. via IViewClass,
		// or UISWTViewEventListener

		if (iview == null) {
			Class iviewClass = (Class) treeItem.getData("IViewClass");
			if (iviewClass != null) {
				Class[] cArgs = (Class[]) treeItem.getData("IViewClassArgs");
				Object[] cArgVals = (Object[]) treeItem.getData("IViewClassVals");

				iview = createSideBarContentArea(iviewClass, cArgs, cArgVals);

				treeItem.setData("IView", iview);

				ViewTitleInfo titleInfo = (iview instanceof ViewTitleInfo)
						? (ViewTitleInfo) iview : null;
				setupTreeItem(treeItem, (String) treeItem.getData("Plugin.viewID"),
						titleInfo, iview.getFullTitle());
			}
		}

		if (iview == null) {
			UISWTViewEventListener l = (UISWTViewEventListener) treeItem.getData("UISWTViewEventListener");
			if (l != null) {
				try {
					String id = (String) treeItem.getData("Plugin.viewID");
					iview = new UISWTViewImpl("SideBar.Plugins", id, l);
					((UISWTViewImpl)iview).setTitle(treeItem.getText());
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
						viewComposite.setLayout(new GridLayout());
					}

					SWTSkinObjectContainer soContents = new SWTSkinObjectContainer(skin,
							skin.getSkinProperties(), viewComposite, "Contents"
									+ (mapIViewToSkinObject.size() + 1), "", "container",
							soSideBarContents);

					mapIViewToSkinObject.put(iview, soContents);

					treeItem.setData("IView", iview);

					ViewTitleInfo titleInfo = (iview instanceof ViewTitleInfo)
							? (ViewTitleInfo) iview : null;
					setupTreeItem(treeItem, id, titleInfo, iview.getFullTitle());

					iview.initialize(viewComposite);
					parent.layout(true, true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		if (iview != null) {
			// hide old
			if (currentIView != null) {
				SWTSkinObjectContainer container = (SWTSkinObjectContainer) mapIViewToSkinObject.get(currentIView);
				if (container != null) {
					Composite composite = container.getComposite();
					if (composite != null && !composite.isDisposed()) {
						composite.setVisible(false);
					}
				}
			}

			// show new
			currentIView = iview;
			SWTSkinObjectContainer container = (SWTSkinObjectContainer) mapIViewToSkinObject.get(currentIView);
			if (container != null) {
				Composite composite = container.getComposite();
				if (composite != null && !composite.isDisposed()) {
					composite.setVisible(true);
					composite.moveAbove(null);
				}
			}
		}
	}

	/**
	 * Creates an IView based on class.  Doesn't add it to the tree
	 * 
	 * @param iview
	 * @param iviewClass
	 * @param args
	 *
	 * @since 3.1.0.1
	 */
	private IView createSideBarContentArea(Class iviewClass, Class[] cArgs,
			Object[] cArgVals) {
		IView iview = null;
		try {
			if (cArgs == null) {
				iview = (IView) iviewClass.newInstance();
			} else {
				Constructor constructor = iviewClass.getConstructor(cArgs);
				iview = (IView) constructor.newInstance(cArgVals);
			}

			createSideBarContentArea(iview);
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
	 *  
	 * @param view
	 * @return
	 *
	 * @since 3.1.1.1
	 */
	private IView createSideBarContentArea(IView view) {
		try {
			Composite parent = (Composite) soSideBarContents.getControl();

			SWTSkinObjectContainer soContents = new SWTSkinObjectContainer(skin,
					skin.getSkinProperties(), "Contents"
							+ (mapIViewToSkinObject.size() + 1), "", soSideBarContents);

			Composite viewComposite = soContents.getComposite();
			viewComposite.setBackground(parent.getDisplay().getSystemColor(
					SWT.COLOR_WIDGET_BACKGROUND));
			viewComposite.setForeground(parent.getDisplay().getSystemColor(
					SWT.COLOR_WIDGET_FOREGROUND));
			viewComposite.setLayoutData(Utils.getFilledFormData());
			GridLayout gridLayout = new GridLayout();
			gridLayout.horizontalSpacing = 0;
			gridLayout.verticalSpacing = 0;
			viewComposite.setLayout(gridLayout);

			mapIViewToSkinObject.put(view, soContents);

			view.initialize(viewComposite);

			Composite iviewComposite = view.getComposite();
			if (iviewComposite.getLayoutData() == null) {
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

	private TreeItem createTreeItemFromSkinRef(Object parentTreeItem,
			final String id, final String configID, String title,
			ViewTitleInfo titleInfo, final Object params) {

		if (mapIdToTreeItem.containsKey(id)) {
			return (TreeItem) mapIdToTreeItem.get(id);
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
				}
				return true;
			}
		};
		if (parentTreeItem != null) {
			treeItem = creatTreeItem(parentTreeItem, id, titleInfo, title);
			treeItem.setData("UISWTViewEventListener", l);
		}
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
									treeItem = searchTreeItem;
									treeItem.setData("TitleInfo", titleIndicator);
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
					treeItem.setText(newText);
				}

				Rectangle bounds = treeItem.getBounds();
				Rectangle treeBounds = tree.getBounds();
				tree.redraw(0, bounds.y, treeBounds.width, bounds.height, true);
			}
		});
	}
}
