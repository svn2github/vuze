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

import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.ConfigurationChecker;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.common.util.MenuItemManager;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.URLTransfer;
import org.gudy.azureus2.ui.swt.mainwindow.*;
import org.gudy.azureus2.ui.swt.mainwindow.PluginsMenuHelper.IViewInfo;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.shells.CoreWaiterSWT;
import org.gudy.azureus2.ui.swt.shells.CoreWaiterSWT.TriggerInThread;
import org.gudy.azureus2.ui.swt.views.*;
import org.gudy.azureus2.ui.swt.views.stats.StatsView;

import com.aelitis.azureus.activities.*;
import com.aelitis.azureus.core.*;
import com.aelitis.azureus.core.cnetwork.*;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.mdi.*;
import com.aelitis.azureus.ui.swt.feature.FeatureManagerUI;
import com.aelitis.azureus.ui.swt.mdi.*;
import com.aelitis.azureus.ui.swt.shells.AuthorizeWindow;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.utils.ContentNetworkUI;
import com.aelitis.azureus.ui.swt.utils.FontUtils;
import com.aelitis.azureus.ui.swt.utils.ContentNetworkUI.ContentNetworkImageLoadedListener;
import com.aelitis.azureus.ui.swt.views.skin.SBC_LibraryView;
import com.aelitis.azureus.util.ConstantsVuze;
import com.aelitis.azureus.util.ContentNetworkUtils;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.menus.*;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;

/**
 * @author TuxPaper
 * @created Jun 23, 2008
 *
 */
public class SideBar
	extends BaseMDI
{
	private static final boolean END_INDENT = Constants.isLinux
			|| Constants.isWindows2000 || Constants.isWindows9598ME;

	private static final boolean USE_PAINTITEM = Utils.isCocoa
			|| Constants.isWindows;

	// Need to use paint even on Cocoa, because there's cases where an area
	// will become invalidated and we don't get a paintitem :(
	private static final boolean USE_PAINT = !Constants.isWindows;

	protected static final boolean HIDE_NATIVE_EXPANDER = false;

	public static final boolean SHOW_ALL_PLUGINS = false;

	public static final boolean SHOW_TOOLS = false;

	public static final boolean SHOW_DEVICES = true;

	public static final String SIDEBAR_SECTION_ACTIVITIES = "Activity";

	private SWTSkin skin;

	private SWTSkinObjectContainer soSideBarContents;

	private SWTSkinObject soSideBarList;

	private Tree tree;

	private Font fontHeader;

	private Font font;

	private SWTSkinObject soSideBarPopout;

	private SelectionListener dropDownSelectionListener;

	private DropTarget dropTarget;

	protected SideBarEntrySWT draggingOver;

	private Color fg;

	private Color bg;

	public static SideBar instance = null;

	public SideBar() {
		super();
		if (instance == null) {
			instance = this;
		}
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectAdapter#skinObjectCreated(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectCreated(SWTSkinObject skinObject, Object params) {
		super.skinObjectCreated(skinObject, params);

		skin = skinObject.getSkin();

		soSideBarContents = (SWTSkinObjectContainer) skin.getSkinObject("sidebar-contents");
		soSideBarList = skin.getSkinObject("sidebar-list");
		soSideBarPopout = skin.getSkinObject("sidebar-pop");

		if (ConfigurationChecker.isNewVersion()
				&& Constants.compareVersions(Constants.AZUREUS_VERSION, "4.4.1.0") == 0) {
			final SWTSkinObjectSash soSash = (SWTSkinObjectSash) skin.getSkinObject("sidebar-sash");
			if (soSash != null) {
				soSash.resetWidth();
			}
		}

		// addTestMenus();

		createSideBar();
		try {
			setupDefaultItems();
		} catch (Exception e) {
			Debug.out(e);
		}

		try {
			UIFunctionsManager.getUIFunctions().getUIUpdater().addUpdater(this);
		} catch (Exception e) {
			Debug.out(e);
		}

		Display.getDefault().addFilter(SWT.KeyDown, new Listener() {
			public void handleEvent(Event event) {
				// F9 is standard Seamonkey, but doesn't work on OSX
				// Command Option T is standard on OSX
				// F7 works on both
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
	 * @since 3.1.0.1
	 */
	/*
	private void addTestMenus() {
		// Add some test menus
		PluginManager pm = AzureusCoreFactory.getSingleton().getPluginManager();
		PluginInterface pi = pm.getDefaultPluginInterface();
		UIManager uim = pi.getUIManager();
		MenuManager menuManager = uim.getMenuManager();
		MenuItem menuItem = menuManager.addMenuItem("sidebar", "test menu");
		menuItem.addListener(new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				ToolBarView tb = (ToolBarView) SkinViewManager.getByClass(ToolBarView.class);
				if (tb != null) {
					System.out.println("Found download Toolbar");
					ToolBarItem dlItem = tb.getToolBarItem("download");
					System.out.println("Download ToolBar Item is " + dlItem);
					if (dlItem != null) {
						System.out.println(dlItem.getSkinButton().getSkinObject());
					}
					dlItem.setEnabled(!dlItem.isEnabled());
				}

				if (target instanceof SideBarEntry) {
					SideBarEntry info = (SideBarEntry) target;
					System.err.println(info.getId() + " of " + info.getParentID()
							+ ";ds=" + info.getDatasource());
				}
			}
		});

		menuItem = menuManager.addMenuItem("sidebar." + SIDEBAR_SECTION_ACTIVITIES,
				"Activity Only Menu");
		menuItem.addListener(new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				if (target instanceof SideBarEntry) {
					SideBarEntry info = (SideBarEntry) target;
					System.err.println(info.getId() + " of " + info.getParentID()
							+ ";ds=" + info.getDatasource());
				}
			}
		});
	}
	*/

	private void addMenuNotifications() {
		PluginInterface pi = PluginInitializer.getDefaultInterface();
		UIManager uim = pi.getUIManager();
		MenuManager menuManager = uim.getMenuManager();

		MenuItem menuItem = menuManager.addMenuItem("sidebar."
				+ SIDEBAR_SECTION_ACTIVITIES, "v3.activity.button.readall");
		menuItem.addListener(new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				VuzeActivitiesEntry[] allEntries = VuzeActivitiesManager.getAllEntries();
				for (int i = 0; i < allEntries.length; i++) {
					VuzeActivitiesEntry entry = allEntries[i];
					entry.setRead(true);
				}
			}
		});
	}

	private void addMenuUnwatched() {
		PluginInterface pi = PluginInitializer.getDefaultInterface();
		UIManager uim = pi.getUIManager();
		MenuManager menuManager = uim.getMenuManager();

		MenuItem menuItem = menuManager.addMenuItem("sidebar."
				+ SIDEBAR_SECTION_LIBRARY_UNOPENED, "v3.activity.button.watchall");
		menuItem.addListener(new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				CoreWaiterSWT.waitForCore(TriggerInThread.ANY_THREAD,
						new AzureusCoreRunningListener() {
							public void azureusCoreRunning(AzureusCore core) {
								GlobalManager gm = core.getGlobalManager();
								List<?> downloadManagers = gm.getDownloadManagers();
								for (Iterator<?> iter = downloadManagers.iterator(); iter.hasNext();) {
									DownloadManager dm = (DownloadManager) iter.next();

									if (!PlatformTorrentUtils.getHasBeenOpened(dm)
											&& dm.getAssumedComplete()) {
										PlatformTorrentUtils.setHasBeenOpened(dm, true);
									}
								}
							}
						});
			}
		});
	}

	/**
	 * 
	 *
	 * @since 3.1.1.1
	 */
	public void flipSideBarVisibility() {
		final SWTSkinObjectSash soSash = (SWTSkinObjectSash) skin.getSkinObject("sidebar-sash");
		if (soSash == null) {
			return;
		}
		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				soSash.setAboveVisible(!soSash.isAboveVisible());
				updateSidebarVisibility();
			}
		});
	}
	
	private void updateSidebarVisibility() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				final SWTSkinObjectSash soSash = (SWTSkinObjectSash) skin.getSkinObject("sidebar-sash");
				if (soSash == null) {
					return;
				}
				if (soSash.isAboveVisible()) {
					if (soSideBarPopout != null) {
						Object ld = soSideBarPopout.getControl().getLayoutData();
						if (ld instanceof FormData) {
							FormData fd = (FormData) ld;
							fd.width = 0;
						}
						soSideBarPopout.setVisible(false);

						Utils.relayout(soSideBarPopout.getControl());
					}
				} else {
					if (soSideBarPopout != null) {
						Object ld = soSideBarPopout.getControl().getLayoutData();
						if (ld instanceof FormData) {
							FormData fd = (FormData) ld;
							fd.width = 24;
						}
						soSideBarPopout.setVisible(true);
						soSideBarPopout.getControl().moveAbove(null);
						Utils.relayout(soSideBarPopout.getControl());
					}
				}
			}
		});
	}

	public boolean isVisible() {
		SWTSkinObjectSash soSash = (SWTSkinObjectSash) skin.getSkinObject("sidebar-sash");
		if (soSash == null) {
			return false;
		}
		return soSash.isAboveVisible();
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#showSupport(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		updateSidebarVisibility();
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

	private void createSideBar() {
		if (soSideBarList == null) {
			return;
		}
		Composite parent = (Composite) soSideBarList.getControl();

		tree = new Tree(parent, SWT.FULL_SELECTION | SWT.V_SCROLL
				| SWT.DOUBLE_BUFFERED | SWT.NO_SCROLL);
		tree.setHeaderVisible(false);

		new SideBarToolTips(this, tree);

		tree.setLayoutData(Utils.getFilledFormData());

		SWTSkinProperties skinProperties = skin.getSkinProperties();
		bg = skinProperties.getColor("color.sidebar.bg");
		fg = skinProperties.getColor("color.sidebar.fg");

		tree.setBackground(bg);
		tree.setForeground(fg);
		FontData[] fontData = tree.getFont().getFontData();

		int fontHeight = 13 + (tree.getItemHeight() > 18
				? tree.getItemHeight() - 18 : 0);

		fontData[0].setStyle(SWT.BOLD);
		FontUtils.getFontHeightFromPX(tree.getDisplay(), fontData, null, fontHeight);
		//FontUtils.getFontHeightFromPX(tree.getDisplay(), fontData, null, fontHeight);
		fontHeader = new Font(tree.getDisplay(), fontData);
		
		fontData[0].setStyle(SWT.NORMAL);
		FontUtils.setFontDataHeight(fontData, FontUtils.getHeight(fontData) * 0.92f);
		//FontUtils.getFontHeightFromPX(tree.getDisplay(), fontData, null, fontHeight);
		font = new Font(tree.getDisplay(), fontData);

		tree.setFont(font);


		Listener treeListener = new Listener() {
			TreeItem lastTopItem = null;

			boolean mouseDowned = false;

			public void handleEvent(final Event event) {
				TreeItem treeItem = (TreeItem) event.item;

				switch (event.type) {
					case SWT.MeasureItem: {
						int clientWidth = tree.getClientArea().width;
						String text = treeItem.getText(event.index);
						Point size = event.gc.textExtent(text);
						if (event.x + event.width < clientWidth) {
							event.width = size.x + event.x; // tree.getClientArea().width;
							event.x = 0;
						}

						if (Constants.isWindows) {
							event.width = clientWidth - event.x;
						}
						//int padding = 4;
						//String id = (String) treeItem.getData("Plugin.viewID");
						//SideBarEntrySWT entry = getSideBarInfo(id);
						//if (entry.imageLeft != null) {
						//padding += 4;
						//}

						event.height = 24;// Math.max(event.height, size.y + padding);

						break;
					}
					case SWT.PaintItem: {
						if (USE_PAINTITEM) {
							SideBarEntrySWT entry = (SideBarEntrySWT) treeItem.getData("MdiEntry");
							//System.out.println("PaintItem: " + event.item + ";" + event.index + ";" + event.detail + ";" + id + ";" + event.getBounds() + ";" + event.gc.getClipping());
							if (entry != null) {
								TreeItem[] selection = tree.getSelection();
								if (selection == null || selection.length == 0 || selection[0] != treeItem) {
									event.detail &= ~SWT.SELECTED;
								}
								entry.swt_paintSideBar(event);
							}
						}
						break;
					}

					case SWT.Paint: {
						if (HIDE_NATIVE_EXPANDER) {
							boolean selected = (event.detail & SWT.SELECTED) > 0;
							Rectangle bounds = event.getBounds();
							int indent = END_INDENT ? tree.getClientArea().width - 1 : 0;
							int y = event.y + 1;
							treeItem = tree.getItem(new Point(indent, y));

							while (treeItem != null) {
								SideBarEntrySWT entry = (SideBarEntrySWT) treeItem.getData("MdiEntry");
								Rectangle itemBounds = entry.swt_getBounds();

								if (itemBounds != null && entry.isCollapseDisabled()) {
									Rectangle paintArea = treeItem.getBounds();
									paintArea.x = 0;
									paintArea.width = 17;
									selected = tree.getSelectionCount() == 1
											&& tree.getSelection()[0].equals(treeItem);
									int detail = 0;
									if (selected) {
										detail |= SWT.SELECTED;
									}
									entry.swt_paintEntryBG(detail, event.gc, paintArea);
									y = itemBounds.y + itemBounds.height + 1;
								} else {
									y += tree.getItemHeight();
								}

								if (y > bounds.y + bounds.height) {
									break;
								}
								treeItem = tree.getItem(new Point(indent, y));
							}
						}

						//System.out.println("Paint: " + event.getBounds() + ";" + event.detail + ";" + event.index + ";" + event.gc.getClipping() + "  " + Debug.getCompressedStackTrace());
						if (!USE_PAINT) {
							return;
						}
						Rectangle bounds = event.getBounds();
						int indent = END_INDENT ? tree.getClientArea().width - 1 : 0;
						int y = event.y + 1;
						treeItem = tree.getItem(new Point(indent, y));

						while (treeItem != null) {
							SideBarEntrySWT entry = (SideBarEntrySWT) treeItem.getData("MdiEntry");
							Rectangle itemBounds = entry.swt_getBounds();

							// null itemBounds is weird, the entry must be disposed. it 
							// happened once, so let's check..
							if (itemBounds != null) {
								event.item = treeItem;

								boolean selected = tree.getSelectionCount() == 1
										&& tree.getSelection()[0].equals(treeItem);
								event.detail = selected ? SWT.SELECTED : SWT.NONE;

								Rectangle newClip = bounds.intersection(itemBounds);
								//System.out.println("Paint " + id + " @ " + newClip);
								event.setBounds(newClip);
								event.gc.setClipping(newClip);

								entry.swt_paintSideBar(event);

								y = itemBounds.y + itemBounds.height + 1;
							} else {
								y += tree.getItemHeight();
							}

							if (y > bounds.y + bounds.height) {
								break;
							}
							TreeItem oldTreeItem = treeItem;
							treeItem = tree.getItem(new Point(indent, y));
							if (oldTreeItem == treeItem) {
								break;
							}
						}

						if (tree.getTopItem() != lastTopItem) {
							lastTopItem = tree.getTopItem();
							SideBarEntrySWT[] sideBarEntries = (SideBarEntrySWT[]) mapIdToEntry.values().toArray(
									new SideBarEntrySWT[0]);
							swt_updateSideBarHitAreasY(sideBarEntries);
						}

						break;
					}

					case SWT.EraseItem: {
						//event.detail &= ~SWT.FOREGROUND;
						//event.detail &= ~(SWT.FOREGROUND | SWT.BACKGROUND);
						event.doit = true;
						break;
					}

					case SWT.Resize: {
						tree.redraw();
						break;
					}

					case SWT.Selection: {
						if (treeItem == null) {
							return;
						}
						SideBarEntrySWT entry = (SideBarEntrySWT) treeItem.getData("MdiEntry");
						if (entry != null) {
							showEntry(entry);
						}
						break;
					}

					case SWT.MouseDown: {
						mouseDowned = true;
					}

					case SWT.MouseUp: {
						if (!mouseDowned) {
							return;
						}
						mouseDowned = false;
						if (tree.getItemCount() == 0 || event.button != 1) {
							return;
						}
						int indent = END_INDENT ? tree.getClientArea().width - 1 : 0;
						treeItem = tree.getItem(new Point(indent, event.y));
						if (treeItem == null) {
							return;
						}
						SideBarEntrySWT entry = (SideBarEntrySWT) treeItem.getData("MdiEntry");

						Rectangle closeArea = (Rectangle) treeItem.getData("closeArea");
						if (closeArea != null && closeArea.contains(event.x, event.y)) {
							treeItem.dispose();
						} else if (currentEntry != entry && Constants.isOSX) {
							showEntry(entry);
						}

						MdiEntryVitalityImage[] vitalityImages = entry.getVitalityImages();
						for (int i = 0; i < vitalityImages.length; i++) {
							SideBarVitalityImageSWT vitalityImage = (SideBarVitalityImageSWT) vitalityImages[i];
							if (!vitalityImage.isVisible()) {
								continue;
							}
							Rectangle hitArea = vitalityImage.getHitArea();
							if (hitArea == null) {
								continue;
							}
							if (hitArea.contains(event.x, event.y)) {
								vitalityImage.triggerClickedListeners(event.x, event.y);
								break;
							}
						}

						break;
					}

					case SWT.Dispose: {
						fontHeader.dispose();
						font.dispose();
						if (dropTarget != null && !dropTarget.isDisposed()) {
							dropTarget.dispose();
						}
						saveCloseables();

						break;
					}

					case SWT.Collapse: {
						SideBarEntrySWT entry = (SideBarEntrySWT) treeItem.getData("MdiEntry");

						if (entry.isCollapseDisabled()) {
							tree.setRedraw(false);
							Display.getDefault().asyncExec(new Runnable() {
								public void run() {
									((TreeItem) event.item).setExpanded(true);
									tree.setRedraw(true);
								}
							});
						}
						break;
					}

				}
			}
		};
		tree.addListener(SWT.MeasureItem, treeListener);
		tree.addListener(SWT.Resize, treeListener);
		tree.addListener(SWT.Paint, treeListener);
		if (USE_PAINTITEM) {
			tree.addListener(SWT.PaintItem, treeListener);
			tree.addListener(SWT.EraseItem, treeListener);
		}

		tree.addListener(SWT.Selection, treeListener);
		tree.addListener(SWT.Dispose, treeListener);

		// For icons
		tree.addListener(SWT.MouseUp, treeListener);
		tree.addListener(SWT.MouseDown, treeListener);

		// to disable collapsing
		tree.addListener(SWT.Collapse, treeListener);
		
		//DragSource dragSource = new DragSource(tree, DND.DROP_COPY | DND.DROP_MOVE);
		//dragSource.setTransfer(new Transfer[] { TextTransfer.getInstance() });

		dropTarget = new DropTarget(tree, DND.DROP_COPY);
		dropTarget.setTransfer(new Transfer[] {
			URLTransfer.getInstance(),
			FileTransfer.getInstance(),
			TextTransfer.getInstance(),
		});

		dropTarget.addDropListener(new DropTargetAdapter() {
			public void dropAccept(DropTargetEvent event) {
				event.currentDataType = URLTransfer.pickBestType(event.dataTypes,
						event.currentDataType);
			}

			public void dragEnter(DropTargetEvent event) {
			}
			
			public void dragOperationChanged(DropTargetEvent event) {
			}

			// @see org.eclipse.swt.dnd.DropTargetAdapter#dragOver(org.eclipse.swt.dnd.DropTargetEvent)
			public void dragOver(DropTargetEvent event) {
				TreeItem treeItem = (event.item instanceof TreeItem)
						? (TreeItem) event.item : null;

				if (treeItem != null) {
					SideBarEntrySWT entry = (SideBarEntrySWT) treeItem.getData("MdiEntry");

					draggingOver = entry;
				} else {
					draggingOver = null;
				}
				if (draggingOver == null || !draggingOver.hasDropListeners()) {
					event.detail = DND.DROP_NONE;
					draggingOver = null;
				} else if ((event.operations & DND.DROP_LINK) > 0)
					event.detail = DND.DROP_LINK;
				else if ((event.operations & DND.DROP_COPY) > 0)
					event.detail = DND.DROP_COPY;
				else if ((event.operations & DND.DROP_DEFAULT) > 0)
					event.detail = DND.DROP_COPY;

				if (Constants.isOSX) {
					tree.redraw();
				}
			}

			// @see org.eclipse.swt.dnd.DropTargetAdapter#dragLeave(org.eclipse.swt.dnd.DropTargetEvent)
			public void dragLeave(DropTargetEvent event) {
				draggingOver = null;
				tree.redraw();
			}

			public void drop(DropTargetEvent event) {
				draggingOver = null;
				tree.redraw();
				if (!(event.item instanceof TreeItem)) {
					defaultDrop(event);
					return;
				}
				TreeItem treeItem = (TreeItem) event.item;

				SideBarEntrySWT entry = (SideBarEntrySWT) treeItem.getData("MdiEntry");

				boolean handled = entry.triggerDropListeners(event.data);
				if (!handled) {
					defaultDrop(event);
				}
			}
		});

		final Menu menuTree = new Menu(tree);
		tree.setMenu(menuTree);

		menuTree.addMenuListener(new MenuListener() {
			boolean bShown = false;

			public void menuHidden(MenuEvent e) {
				bShown = false;

				if (Constants.isOSX) {
					return;
				}

				// Must dispose in an asyncExec, otherwise SWT.Selection doesn't
				// get fired (async workaround provided by Eclipse Bug #87678)
				Utils.execSWTThreadLater(0, new AERunnable() {
					public void runSupport() {
						if (bShown || menuTree.isDisposed()) {
							return;
						}
						Utils.disposeSWTObjects(menuTree.getItems());
					}
				});
			}

			public void menuShown(MenuEvent e) {
				Utils.disposeSWTObjects(menuTree.getItems());

				bShown = true;

				Point ptMouse = tree.toControl(e.display.getCursorLocation());

				int indent = END_INDENT ? tree.getClientArea().width - 1 : 0;
				TreeItem treeItem = tree.getItem(new Point(indent, ptMouse.y));
				if (treeItem == null) {
					return;
				}
				SideBarEntrySWT entry = (SideBarEntrySWT) treeItem.getData("MdiEntry");

				fillMenu(menuTree, entry);

				if (menuTree.getItemCount() == 0) {
					Utils.execSWTThreadLater(0, new AERunnable() {
						public void runSupport() {
							menuTree.setVisible(false);
						}
					});
				}
			}
		});

		if (soSideBarPopout != null) {
			SWTSkinObject soDropDown = skin.getSkinObject("sidebar-dropdown");
			if (soDropDown != null) {

				final Menu menuDropDown = new Menu(soDropDown.getControl());

				menuDropDown.addMenuListener(new MenuListener() {
					boolean bShown = false;

					public void menuHidden(MenuEvent e) {
						bShown = false;

						if (Constants.isOSX) {
							return;
						}

						// Must dispose in an asyncExec, otherwise SWT.Selection doesn't
						// get fired (async workaround provided by Eclipse Bug #87678)
						Utils.execSWTThreadLater(0, new AERunnable() {
							public void runSupport() {
								if (bShown || menuDropDown.isDisposed()) {
									return;
								}
								Utils.disposeSWTObjects(menuDropDown.getItems());
							}
						});
					}

					public void menuShown(MenuEvent e) {
						Utils.disposeSWTObjects(menuDropDown.getItems());

						bShown = true;

						fillDropDownMenu(menuDropDown, tree.getItems(), 0);
					}
				});

				dropDownSelectionListener = new SelectionListener() {
					public void widgetSelected(SelectionEvent e) {
						String id = (String) e.widget.getData("Plugin.viewID");
						showEntryByID(id);
					}

					public void widgetDefaultSelected(SelectionEvent e) {
					}
				};

				SWTSkinButtonUtility btnDropDown = new SWTSkinButtonUtility(soDropDown);
				btnDropDown.addSelectionListener(new ButtonListenerAdapter() {
					public void pressed(SWTSkinButtonUtility buttonUtility,
							SWTSkinObject skinObject, int stateMask) {
						Control c = buttonUtility.getSkinObject().getControl();
						menuDropDown.setLocation(c.getDisplay().getCursorLocation());
						menuDropDown.setVisible(!menuDropDown.getVisible());
					}
				});
			}

			SWTSkinObject soExpand = skin.getSkinObject("sidebar-expand");
			if (soExpand != null) {
				SWTSkinButtonUtility btnExpand = new SWTSkinButtonUtility(soExpand);
				btnExpand.addSelectionListener(new ButtonListenerAdapter() {
					public void pressed(SWTSkinButtonUtility buttonUtility,
							SWTSkinObject skinObject, int stateMask) {
						flipSideBarVisibility();
					}
				});
			}

		}
	}

	/**
	 * @param event
	 */
	protected void defaultDrop(DropTargetEvent event) {
		TorrentOpener.openDroppedTorrents(event, false);
	}

	/**
	 * @param menuDropDown
	 *
	 * @since 3.1.1.1
	 */
	protected void fillDropDownMenu(Menu menuDropDown, TreeItem[] items,
			int indent) {
		String s = "";
		for (int i = 0; i < indent; i++) {
			s += "   ";
		}
		for (int i = 0; i < items.length; i++) {
			TreeItem treeItem = items[i];

			org.eclipse.swt.widgets.MenuItem menuItem = new org.eclipse.swt.widgets.MenuItem(
					menuDropDown, SWT.RADIO);
			SideBarEntrySWT entry = (SideBarEntrySWT) treeItem.getData("MdiEntry");
			String id = entry.getId();
			menuItem.setData("Plugin.viewID", id);
			ViewTitleInfo titleInfo = entry.getViewTitleInfo();
			String ind = "";
			if (titleInfo != null) {
				String o = (String) titleInfo.getTitleInfoProperty(ViewTitleInfo.TITLE_INDICATOR_TEXT);
				if (o != null) {
					ind = "  (" + o + ")";
					//ind = "\t" + o;
				}
			}
			menuItem.setText(s + entry.getTitle() + ind);
			menuItem.addSelectionListener(dropDownSelectionListener);
			if (currentEntry != null && currentEntry.getId().equals(id)) {
				menuItem.setSelection(true);
			}

			TreeItem[] subItems = treeItem.getItems();
			if (subItems.length > 0) {
				fillDropDownMenu(menuDropDown, subItems, indent + 1);
			}
		}
	}

	/**
	 * @param menuTree
	 *
	 * @since 3.1.0.1
	 */
	protected void fillMenu(Menu menuTree, final MdiEntry entry) {
		org.gudy.azureus2.plugins.ui.menus.MenuItem[] menu_items;

		menu_items = MenuItemManager.getInstance().getAllAsArray("sidebar");

		MenuBuildUtils.addPluginMenuItems((Composite) soMain.getControl(),
				menu_items, menuTree, false, true,
				new MenuBuildUtils.MenuItemPluginMenuControllerImpl(new Object[] {
					entry
				}));

		if (entry != null) {
			menu_items = MenuItemManager.getInstance().getAllAsArray(
					"sidebar." + entry.getId());

			MenuBuildUtils.addPluginMenuItems((Composite) soMain.getControl(),
					menu_items, menuTree, false, true,
					new MenuBuildUtils.MenuItemPluginMenuControllerImpl(new Object[] {
						entry
					}));

			if ((currentEntry instanceof BaseMdiEntry)
					&& ((BaseMdiEntry) currentEntry).getDatasourceCore() instanceof DownloadManager) {

				DownloadManager[] downloads = new DownloadManager[] {
					(DownloadManager) ((BaseMdiEntry) currentEntry).getDatasourceCore()
				};

				org.eclipse.swt.widgets.MenuItem mi = MenuFactory.createTorrentMenuItem(menuTree);

				mi.setData("downloads", downloads);
				mi.setData("is_detailed_view", new Boolean(true));
			}
		}
	}

	/**
	 * 
	 *
	 * @since 3.1.1.1
	 */
	private void swt_updateSideBarHitAreasY(SideBarEntrySWT[] entries) {
		for (int x = 0; x < entries.length; x++) {
			SideBarEntrySWT entry = entries[x];
			TreeItem treeItem = entry.getTreeItem();
			if (treeItem == null || treeItem.isDisposed()) {
				continue;
			}
			Rectangle itemBounds = entry.swt_getBounds();

			if (entry.isCloseable()) {
				Rectangle closeArea = (Rectangle) treeItem.getData("closeArea");
				if (closeArea != null) {
					closeArea.y = itemBounds.y + (itemBounds.height - closeArea.height)
							/ 2;
				}
			}

			MdiEntryVitalityImage[] vitalityImages = entry.getVitalityImages();
			for (int i = 0; i < vitalityImages.length; i++) {
				SideBarVitalityImageSWT vitalityImage = (SideBarVitalityImageSWT) vitalityImages[i];
				if (!vitalityImage.isVisible()) {
					continue;
				}
				Image image = vitalityImage.getImage();
				if (image != null) {
					Rectangle bounds = vitalityImage.getHitArea();
					if (bounds == null) {
						continue;
					}
					bounds.y = itemBounds.y + (itemBounds.height - bounds.height) / 2;
				}
			}
		}
	}

	/**
	 * 
	 *
	 * @since 3.1.1.1
	 */
	private void setupDefaultItems() {
		TreeItem treeItem;

		// Put TitleInfo in another class
		final ViewTitleInfo titleInfoActivityView = new ViewTitleInfo() {
			public Object getTitleInfoProperty(int propertyID) {
				if (propertyID == TITLE_INDICATOR_TEXT) {
					int count = 0;
					VuzeActivitiesEntry[] allEntries = VuzeActivitiesManager.getAllEntries();
					for (int i = 0; i < allEntries.length; i++) {
						VuzeActivitiesEntry entry = allEntries[i];
						if (!entry.isRead()) {
							count++;
						}
					}
					if (count > 0) {
						return "" + count;
					} else {
						return null;
					}
				} else if (propertyID == TITLE_IMAGEID) {
					return "image.sidebar.activity";
				}
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

		MdiEntry entry;
		
		entry = createEntryFromSkinRef(null, SIDEBAR_SECTION_LIBRARY, "library",
				MessageText.getString("sidebar." + SIDEBAR_SECTION_LIBRARY), null,
				null, false, -1);
		entry.setImageLeftID("image.sidebar.library");
		entry.setCollapseDisabled(true);

		{
			createEntryFromSkinRef(SIDEBAR_SECTION_LIBRARY,
					SIDEBAR_SECTION_LIBRARY_DL, "library",
					MessageText.getString("sidebar.LibraryDL"), null, null, false, -1);

			createEntryFromSkinRef(SIDEBAR_SECTION_LIBRARY,
					SIDEBAR_SECTION_LIBRARY_UNOPENED, "library",
					MessageText.getString("sidebar.LibraryUnopened"), null, null, false,
					-1);
			addMenuUnwatched();
		}

		entry = createEntryFromSkinRef(null, SIDEBAR_SECTION_BROWSE,
				"main.area.browsetab", MessageText.getString("sidebar.VuzeHDNetwork"),
				null, null, false, -1);
		entry.setImageLeftID("image.sidebar.vuze");

		ContentNetworkManager cnm = ContentNetworkManagerFactory.getSingleton();
		if (cnm != null) {
			ContentNetwork[] contentNetworks = cnm.getContentNetworks();
			for (ContentNetwork cn : contentNetworks) {
				if (cn == null) {
					continue;
				}
				if (cn.getID() == ConstantsVuze.getDefaultContentNetwork().getID()) {
					cn.setPersistentProperty(ContentNetwork.PP_ACTIVE, Boolean.TRUE);
					continue;
				}

				Object oIsActive = cn.getPersistentProperty(ContentNetwork.PP_ACTIVE);
				boolean isActive = (oIsActive instanceof Boolean)
						? ((Boolean) oIsActive).booleanValue() : false;
				if (isActive) {
					createContentNetworkSideBarEntry(cn);
				}
			}
		}

		createEntryFromSkinRef(null, SIDEBAR_SECTION_ACTIVITIES, "activity",
				MessageText.getString("sidebar." + SIDEBAR_SECTION_ACTIVITIES),
				titleInfoActivityView, null, false, -1);
		addMenuNotifications();

		loadEntryByID(SIDEBAR_SECTION_SUBSCRIPTIONS, false);
		loadEntryByID(SIDEBAR_SECTION_DEVICES, false);
		if (Constants.IS_CVS_VERSION) {
			loadEntryByID(SIDEBAR_SECTION_RELATED_CONTENT, false);
		}

		if (Constants.IS_CVS_VERSION) {
			loadEntryByID(SIDEBAR_SECTION_RELATED_CONTENT, false);
		}
		
		if (SHOW_TOOLS) {
			createEntryFromSkinRef(null, SIDEBAR_SECTION_TOOLS, "main.area.hood",
					"Under The Hood", null, null, false, -1);

			createTreeItemFromIViewClass(SIDEBAR_SECTION_TOOLS,
					PeerSuperView.class.getSimpleName(), "All Peers",
					PeerSuperView.class, true);
			createTreeItemFromIViewClass(SIDEBAR_SECTION_TOOLS,
					StatsView.class.getSimpleName(), "Stats", StatsView.class, true);
			createTreeItemFromIViewClass(SIDEBAR_SECTION_TOOLS,
					MyTrackerView.class.getSimpleName(), "My Tracker",
					MyTrackerView.class, true);
			createTreeItemFromIViewClass(SIDEBAR_SECTION_TOOLS,
					MySharesView.class.getSimpleName(), "My Classic-Shares",
					MySharesView.class, true);
			createTreeItemFromIViewClass(SIDEBAR_SECTION_TOOLS,
					LoggerView.class.getSimpleName(), "Logger", LoggerView.class, true);
			createTreeItemFromIViewClass(SIDEBAR_SECTION_TOOLS,
					ConfigView.class.getSimpleName(), "Config", ConfigView.class, true);
		}

		if (SHOW_ALL_PLUGINS) {
			SideBarEntrySWT pluginsEntry = (SideBarEntrySWT) createEntryFromSkinRef(
					null, SIDEBAR_SECTION_PLUGINS, "main.area.plugins", "Plugins", null,
					null, false, -1);

			IViewInfo[] pluginViewsInfo = PluginsMenuHelper.getInstance().getPluginViewsInfo();
			for (int i = 0; i < pluginViewsInfo.length; i++) {
				IViewInfo viewInfo = pluginViewsInfo[i];
				treeItem = new TreeItem(pluginsEntry.getTreeItem(), SWT.NONE);

				treeItem.setData("text", viewInfo.name);
				SideBarEntrySWT entryPlugin = (SideBarEntrySWT) getEntry(viewInfo.viewID);
				treeItem.setData("MdiEntry", entryPlugin);
				entryPlugin.setTreeItem(treeItem);
				entryPlugin.setIView(viewInfo.view);
				entryPlugin.setEventListener(viewInfo.event_listener);
			}

			TreeItem itemPluginLogs = new TreeItem(pluginsEntry.getTreeItem(),
					SWT.NONE);
			itemPluginLogs.setText("Log Views");
			IViewInfo[] pluginLogViewsInfo = PluginsMenuHelper.getInstance().getPluginLogViewsInfo();
			for (int i = 0; i < pluginLogViewsInfo.length; i++) {
				IViewInfo viewInfo = pluginLogViewsInfo[i];
				treeItem = new TreeItem(itemPluginLogs, SWT.NONE);

				treeItem.setData("text", viewInfo.name);
				SideBarEntrySWT entryPlugin = (SideBarEntrySWT) getEntry(viewInfo.viewID);
				treeItem.setData("MdiEntry", entryPlugin);
				entryPlugin.setTreeItem(treeItem);
				entryPlugin.setIView(viewInfo.view);
				entryPlugin.setEventListener(viewInfo.event_listener);
			}
		}

		SBC_LibraryView.setupViewTitle();

		// building plugin views needs UISWTInstance, which needs core.
		final int burnInfoShown = COConfigurationManager.getIntParameter("burninfo.shown", 0);
		if (burnInfoShown == 0) {
  		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
  			public void azureusCoreRunning(AzureusCore core) {
  				Utils.execSWTThread(new AERunnable() {
  					public void runSupport() {
  						if (FeatureManagerUI.enabled) {
  							// blah, can't add until plugin initialization is done
  
  							
  							loadEntryByID(SIDEBAR_SECTION_PLUS, false);
  						
  							if (!FeatureManagerUI.hasFullBurn()) {
  								loadEntryByID(SIDEBAR_SECTION_BURN_INFO, false);
  							}
  							
								COConfigurationManager.setParameter("burninfo.shown",
										burnInfoShown + 1);
  						}
  
  						setupPluginViews();
  					}
  				});
  			}
  		});
		}

		try {
			loadCloseables();
		} catch (Throwable t) {
			Debug.out(t);
		}

		Composite parent = tree.getParent();

		if (parent.isVisible()) {
			parent.layout(true, true);
		}
	}

	/**
	 * 
	 *
	 * @return 
	 * @since 3.1.1.1
	 */
	public MdiEntry createEntryFromIView(String parentID, IView iview, String id,
			Object datasource, boolean closeable, boolean show, boolean expand) {
		if (id == null) {
			id = iview.getClass().getName();
			int i = id.lastIndexOf('.');
			if (i > 0) {
				id = id.substring(i + 1);
			}
		}

		MdiEntry oldEntry = getEntry(id);
		if (oldEntry != null) {
			if (show) {
				showEntry(oldEntry);
			}
			return oldEntry;
		}

		SideBarEntrySWT entry = new SideBarEntrySWT(this, skin, id);

		entry.setIView(iview);
		entry.setDatasource(datasource);
		entry.setParentID(parentID);

		setupNewEntry(entry, id, -1, expand, closeable);

		if (iview instanceof IViewAlwaysInitialize) {
			entry.build();
		}

		if (show) {
			showEntry(entry);
		}

		return entry;
	}

	private void setupNewEntry(final SideBarEntrySWT entry, final String id,
			final int index, final boolean expandParent, final boolean closeable) {
		//System.out.println("createItem " + id + ";" + Debug.getCompressedStackTrace());
		synchronized (mapIdToEntry) {
			mapIdToEntry.put(id, entry);
		}

		entry.setCloseable(closeable);
		entry.setParentSkinObject(soSideBarContents);

		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				_setupNewEntry(entry, id, index, expandParent, closeable);
			}
		});
	}

	protected void _setupNewEntry(SideBarEntrySWT entry, String id, int index,
			boolean expandParent, boolean closeable) {
		String parentID = entry.getParentID();
		MdiEntry parent = getEntry(parentID);
		TreeItem parentTreeItem = null;
		if (parent instanceof SideBarEntrySWT) {
			SideBarEntrySWT parentSWT = (SideBarEntrySWT) parent;
			parentTreeItem = parentSWT.getTreeItem();
			if (expandParent) {
				parentTreeItem.setExpanded(true);
			}
		}
		TreeItem treeItem = createTreeItem(parentTreeItem, index);
		if (treeItem != null) {
  		treeItem.setData("MdiEntry", entry);
  		entry.setTreeItem(treeItem);
		}
	}

	private MdiEntry createTreeItemFromIViewClass(String parent, String id,
			String title, Class<?> iviewClass, boolean closeable) {

		return createEntryFromIViewClass(parent, id, title, iviewClass, null, null,
				null, null, closeable);
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.mdi.BaseMDI#createEntryFromIViewClass(java.lang.String, java.lang.String, java.lang.String, java.lang.Class, java.lang.Class<?>[], java.lang.Object[], java.lang.Object, com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo, boolean)
	 */
	public MdiEntry createEntryFromIViewClass(String parent, String id,
			String title, Class<?> iviewClass, Class<?>[] iviewClassArgs,
			Object[] iviewClassVals, Object datasource, ViewTitleInfo titleInfo,
			boolean closeable) {

		MdiEntry oldEntry = getEntry(id);
		if (oldEntry != null) {
			return oldEntry;
		}

		SideBarEntrySWT entry = new SideBarEntrySWT(this, skin, id);
		entry.setTitle(title);

		entry.setIViewClass(iviewClass, iviewClassArgs, iviewClassVals);
		entry.setDatasource(datasource);
		entry.setViewTitleInfo(titleInfo);
		entry.setParentID(parent);

		setupNewEntry(entry, id, -1, false, closeable);
		
		if (IViewAlwaysInitialize.class.isAssignableFrom(iviewClass)) {
			entry.build();
		}
		
		return entry;
	}

	private TreeItem createTreeItem(Object parentSwtItem, int index) {
		TreeItem treeItem;

		if (parentSwtItem == null) {
			parentSwtItem = tree;
		}

		if (parentSwtItem instanceof Tree) {
			Tree tree = (Tree) parentSwtItem;
			if (tree.isDisposed()) {
				return null;
			}
			if (index >= 0 && index < tree.getItemCount()) {
				treeItem = new TreeItem(tree, SWT.NONE, index);
			} else {
				treeItem = new TreeItem(tree, SWT.NONE);
			}
		} else {
			if (((TreeItem) parentSwtItem).isDisposed()) {
				return null;
			}
			if (index >= 0) {
				treeItem = new TreeItem((TreeItem) parentSwtItem, SWT.NONE, index);
			} else {
				treeItem = new TreeItem((TreeItem) parentSwtItem, SWT.NONE);
			}
		}

		return treeItem;
	}

	public void showEntry(MdiEntry newEntry) {
		if (tree.isDisposed()) {
			return;
		}

		final SideBarEntrySWT oldEntry = (SideBarEntrySWT) currentEntry;

		if (currentEntry == newEntry) {
			triggerSelectionListener(newEntry, newEntry);
			return;
		}

		// show new
		currentEntry = (MdiEntrySWT) newEntry;

		((BaseMdiEntry) currentEntry).show();

		// hide old
		if (oldEntry != null && oldEntry != newEntry) {
			oldEntry.hide();
		}

		triggerSelectionListener(newEntry, oldEntry);
	}

	public MdiEntry createEntryFromEventListener(String parentID,
			UISWTViewEventListener l, String id, boolean closeable, Object datasource) {

		MdiEntry oldEntry = getEntry(id);
		if (oldEntry != null) {
			return oldEntry;
		}

		SideBarEntrySWT entry = new SideBarEntrySWT(this, skin, id);
		try {
			// hack: setEventListner will create the UISWTView.
			// We need to have the entry available for the view to use
			// if it wants
			synchronized (mapIdToEntry) {
				mapIdToEntry.put(id, entry);
			}

			entry.setParentID(parentID);
			entry.setDatasource(datasource);

			setupNewEntry(entry, id, -1, false, closeable);

			entry.setEventListener(l);
		} catch (Exception e) {
			Debug.out(e);
			entry.close(true);
		}

		return entry;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.mdi.BaseMDI#createEntryFromSkinRef(java.lang.String, java.lang.String, java.lang.String, java.lang.String, com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo, java.lang.Object, boolean, int)
	 */
	public MdiEntry createEntryFromSkinRef(String parentID, final String id,
			final String configID, String title, ViewTitleInfo titleInfo,
			final Object params, boolean closeable, int index) {

		MdiEntry oldEntry = getEntry(id);
		if (oldEntry != null) {
			return oldEntry;
		}

		SideBarEntrySWT entry = new SideBarEntrySWT(this, skin, id);

		entry.setTitle(title);
		entry.setSkinRef(configID, params);
		entry.setParentID(parentID);
		entry.setViewTitleInfo(titleInfo);

		setupNewEntry(entry, id, index, false, closeable);

		return entry;
	}

	// @see com.aelitis.azureus.ui.swt.utils.UIUpdatable#updateUI()
	public void updateUI() {
		if (currentEntry == null || currentEntry.getIView() == null
				|| tree.getSelectionCount() == 0) {
			return;
		}
		currentEntry.updateUI();
	}

	public boolean showEntryByID(String id) {
		return loadEntryByID(id, true);
	}

	public boolean loadEntryByID(String id, boolean activate) {
		if (id == null) {
			return false;
		}
		MdiEntry entry = getEntry(id);
		if (entry != null) {
			if (activate) {
				showEntry(entry);
			}
			return true;
		}
		if (id.equals(SIDEBAR_SECTION_WELCOME)) {
			SideBarEntrySWT entryWelcome = (SideBarEntrySWT) createWelcomeSection();
			if (activate) {
				showEntry(entryWelcome);
			}
			return true;
		} else if (id.startsWith("ContentNetwork.")) {
			long networkID = Long.parseLong(id.substring(15));
			handleContentNetworkSwitch(id, networkID);
			return true;
		} else if (id.equals("library") || id.equals("minilibrary")) {
			id = SIDEBAR_SECTION_LIBRARY;
			loadEntryByID(id, activate);
			return true;
		} else if (id.equals("activities")) {
			id = SIDEBAR_SECTION_ACTIVITIES;
			loadEntryByID(id, activate);
			return true;
		}

		MdiEntryCreationListener mdiEntryCreationListener = mapIdToCreationListener.get(id);
		if (mdiEntryCreationListener != null) {
			MdiEntry mdiEntry = mdiEntryCreationListener.createMDiEntry(id);
			if (mdiEntry instanceof SideBarEntrySWT) {
				if (activate) {
					showEntry(mdiEntry);
				}
				return true;
			}
		} else {
			setEntryAutoOpen(id, true);
		}

		return false;
	}

	/**
	 * @param tabID
	 *
	 * @since 4.0.0.3
	 */
	protected void handleContentNetworkSwitch(String tabID, long networkID) {
		try {
			ContentNetworkManager cnManager = ContentNetworkManagerFactory.getSingleton();
			if (cnManager == null) {
				showEntryByID(SIDEBAR_SECTION_BROWSE);
				return;
			}

			ContentNetwork cn = cnManager.getContentNetwork(networkID);
			if (cn == null) {
				showEntryByID(SIDEBAR_SECTION_BROWSE);
				return;
			}

			if (networkID == ContentNetwork.CONTENT_NETWORK_VUZE) {
				showEntryByID(SIDEBAR_SECTION_BROWSE);
				cn.setPersistentProperty(ContentNetwork.PP_ACTIVE, Boolean.TRUE);
				return;
			}

			boolean doneAuth = false;
			Object oDoneAuth = cn.getPersistentProperty(ContentNetwork.PP_AUTH_PAGE_SHOWN);
			if (oDoneAuth instanceof Boolean) {
				doneAuth = ((Boolean) oDoneAuth).booleanValue();
			}

			if (!doneAuth && cn.isServiceSupported(ContentNetwork.SERVICE_AUTHORIZE)) {
				if (!AuthorizeWindow.openAuthorizeWindow(cn)) {
					return;
				}
			}

			createContentNetworkSideBarEntry(cn);
			showEntryByID(tabID);
			return;
		} catch (Exception e) {
			Debug.out(e);
		}
		showEntryByID(SIDEBAR_SECTION_BROWSE);
	}

	private void createContentNetworkSideBarEntry(ContentNetwork cn) {
		String entryID = ContentNetworkUtils.getTarget(cn);

		if (entryExists(entryID)) {
			return;
		}

		String name = cn.getName();
		SideBarEntrySWT entryBrowse = (SideBarEntrySWT) getEntry(SIDEBAR_SECTION_BROWSE);
		int position = entryBrowse == null || entryBrowse.getTreeItem() == null ? 3
				: tree.indexOf(entryBrowse.getTreeItem()) + 1;

		Object prop = cn.getProperty(ContentNetwork.PROPERTY_REMOVEABLE);
		boolean closeable = (prop instanceof Boolean)
				? ((Boolean) prop).booleanValue() : false;
		final SideBarEntrySWT entry = (SideBarEntrySWT) createEntryFromSkinRef(
				null, entryID, "main.area.browsetab", name, null, cn, closeable,
				position);

		ContentNetworkUI.loadImage(cn.getID(),
				new ContentNetworkImageLoadedListener() {
					public void contentNetworkImageLoaded(Long contentNetworkID,
							Image image, boolean wasReturned) {
						entry.setImageLeft(image);
					}
				});
		cn.setPersistentProperty(ContentNetwork.PP_ACTIVE, Boolean.TRUE);
		cn.setPersistentProperty(ContentNetwork.PP_SHOW_IN_MENU, Boolean.TRUE);
	}

	public Font getHeaderFont() {
		return fontHeader;
	}

	protected Tree getTree() {
		return tree;
	}

}
