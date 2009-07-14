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
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIPluginView;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.menus.MenuManager;
import org.gudy.azureus2.plugins.ui.sidebar.SideBarDropListener;
import org.gudy.azureus2.plugins.ui.sidebar.SideBarEntry;
import org.gudy.azureus2.plugins.ui.sidebar.SideBarVitalityImage;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.common.util.MenuItemManager;
import org.gudy.azureus2.ui.swt.MenuBuildUtils;
import org.gudy.azureus2.ui.swt.URLTransfer;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.MenuFactory;
import org.gudy.azureus2.ui.swt.mainwindow.PluginsMenuHelper;
import org.gudy.azureus2.ui.swt.mainwindow.PluginsMenuHelper.IViewInfo;
import org.gudy.azureus2.ui.swt.mainwindow.PluginsMenuHelper.PluginAddedViewListener;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTInstanceImpl;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewEventCancelledException;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewImpl;
import org.gudy.azureus2.ui.swt.shells.CoreWaiterSWT;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.shells.CoreWaiterSWT.TriggerInThread;
import org.gudy.azureus2.ui.swt.views.*;
import org.gudy.azureus2.ui.swt.views.stats.StatsView;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.activities.VuzeActivitiesListener;
import com.aelitis.azureus.activities.VuzeActivitiesManager;
import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.cnetwork.ContentNetworkManager;
import com.aelitis.azureus.core.cnetwork.ContentNetworkManagerFactory;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.table.TableView;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoListener;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.selectedcontent.ISelectedContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentV3;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.ui.swt.shells.AuthorizeWindow;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.toolbar.ToolBarEnabler;
import com.aelitis.azureus.ui.swt.toolbar.ToolBarEnablerSelectedContent;
import com.aelitis.azureus.ui.swt.toolbar.ToolBarItem;
import com.aelitis.azureus.ui.swt.utils.ColorCache;
import com.aelitis.azureus.ui.swt.utils.ContentNetworkUI;
import com.aelitis.azureus.ui.swt.utils.ContentNetworkUI.ContentNetworkImageLoadedListener;
import com.aelitis.azureus.ui.swt.views.skin.*;
import com.aelitis.azureus.util.ConstantsVuze;
import com.aelitis.azureus.util.ContentNetworkUtils;
import com.aelitis.azureus.util.MapUtils;

/**
 * @author TuxPaper
 * @created Jun 23, 2008
 *
 */
public class SideBar
	extends SkinView
	implements UIUpdatable, ViewTitleInfoListener
{
	private static final boolean END_INDENT = Constants.isLinux || Constants.isWindows2000 || Constants.isWindows9598ME;

	private static final int SIDEBAR_SPACING = 2;

	public static final String SIDEBAR_SECTION_PLUGINS = "Plugins";

	public static final String SIDEBAR_SECTION_LIBRARY = "Library";

	public static final String SIDEBAR_SECTION_LIBRARY_DL = "LibraryDL";

	public static final String SIDEBAR_SECTION_LIBRARY_CD = "LibraryCD";

	public static final String SIDEBAR_SECTION_LIBRARY_UNOPENED = "LibraryUnopened";

	public static final String SIDEBAR_SECTION_TOOLS = "Tools";

	public static String SIDEBAR_SECTION_BROWSE = "ContentNetwork.1";

	public static final String SIDEBAR_SECTION_WELCOME = "Welcome";

	public static final String SIDEBAR_SECTION_PUBLISH = "Publish";

	public static final String SIDEBAR_SECTION_SUBSCRIPTIONS = "Subscriptions";

	public static final String SIDEBAR_SECTION_DEVICES = "Devices";
	
	public static final String SIDEBAR_SECTION_RELATED_CONTENT = "RelatedContent";

	public static final String SIDEBAR_SECTION_ADVANCED = "Advanced";

	public static final boolean SHOW_ALL_PLUGINS = false;

	public static final boolean SHOW_TOOLS = false;
	
	public static final boolean SHOW_DEVICES = true;

	public static final String SIDEBAR_SECTION_ACTIVITIES = "Activity";

	private static final int IMAGELEFT_SIZE = 20;

	private static final int IMAGELEFT_GAP = 5;

	private static final boolean ALWAYS_IMAGE_GAP = false;

	private static final String[] default_indicator_colors = {
		"#000000",
		"#000000",
		"#166688",
		"#1c2056"
	};

	private SWTSkin skin;

	private SWTSkinObject soSideBarContents;

	private SWTSkinObject soSideBarList;

	private Tree tree;

	private Font fontHeader;

	private SideBarEntrySWT currentSideBarEntry;

	private static Map mapTitleInfoToEntry = new LightHashMap();

	private static Map mapIdToEntries = new LightHashMap();

	private static List listTreeItemsNoTitleInfo = new ArrayList();

	private static DisposeListener disposeTreeItemListener;

	private CopyOnWriteList listeners = new CopyOnWriteList();

	double lastPercent = 0.8;

	private Color bg;

	private Color fg;

	private Color bgSel;

	private Color fgSel;

	private Color colorFocus;

	private Image imgClose;

	private SWTSkinObject soSideBarPopout;

	private SelectionListener dropDownSelectionListener;

	private ImageLoader imageLoader;

	private int maxIndicatorWidth;

	private Image imgCloseSelected;

	private static Map mapAutoOpen = new LightHashMap();

	private Image treeImage;

	private Image lastImage;

	//private Image imgUntwist;

	//private Image imgTwist;

	private Shell shellFade;

	private DropTarget dropTarget;

	protected SideBarEntrySWT draggingOver;

	public static SideBar instance = null;

	static {
		SIDEBAR_SECTION_BROWSE = ContentNetworkUtils.getTarget(ConstantsVuze.getDefaultContentNetwork());

		disposeTreeItemListener = new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				final TreeItem treeItem = (TreeItem) e.widget;
				final Tree tree = treeItem.getParent();
				final int itemIndex = tree.indexOf(treeItem);
				final String id = (String) treeItem.getData("Plugin.viewID");
				Utils.execSWTThreadLater(0, new AERunnable() {
					public void runSupport() {
						// even though execThreadLater will not run on close off app because
						// the display is disposed, do a double chek of tree disposal just
						// in case.  We don't want to trigger close listeners or
						// remove autoopen parameters if the user is closing the app (as
						// opposed to closing  the sidebar)
						if (tree.isDisposed()) {
							return;
						}

						listTreeItemsNoTitleInfo.remove(treeItem);

						//TreeItem currentItem = treeItem.getParent().getSelection()[0];

						if (id != null) {
							try {
								SideBarEntrySWT entry = getEntry(id);
								entry.treeItem = null;

								entry.triggerCloseListeners();

								if (entry.iview != null) {
									IView iviewDelete = entry.iview;
									entry.iview = null;
									iviewDelete.delete();
								}
								if (entry.skinObject != null) {
									SWTSkinObject so = entry.skinObject;
									entry.skinObject = null;
									so.getSkin().removeSkinObject(so);
								}
								COConfigurationManager.removeParameter("SideBar.AutoOpen." + id);

								if (Constants.isOSX && !tree.isDisposed()
										&& tree.getSelectionCount() == 0) {

									if (entry.parentID != null) {
										entry.getSidebar().showEntryByID(entry.parentID);
									} else {
										int i = itemIndex;
										if (i >= tree.getItemCount() || i < 0) {
											i = tree.getItemCount() - 1;
										}
										TreeItem item = tree.getItem(i);
										entry.getSidebar().itemSelected(item);
									}
								}
							} catch (Exception e2) {
								Debug.out(e2);
							}

							mapAutoOpen.remove(id);
							mapIdToEntries.remove(id);

							return;
						}

						// find treeitem..
						for (Iterator iter = mapIdToEntries.keySet().iterator(); iter.hasNext();) {
							String id = (String) iter.next();
							SideBarEntrySWT entry = getEntry(id);
							if (entry != null && entry.treeItem == treeItem) {
								iter.remove();
							}
						}

					}
				});
			}
		};
	}

	public SideBar() {
		if (instance == null) {
			instance = this;
		}
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectAdapter#skinObjectCreated(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectCreated(SWTSkinObject skinObject, Object params) {
		skin = skinObject.getSkin();

		soSideBarContents = skin.getSkinObject("sidebar-contents");
		soSideBarList = skin.getSkinObject("sidebar-list");
		soSideBarPopout = skin.getSkinObject("sidebar-pop");

		imageLoader = skin.getImageLoader(skinObject.getProperties());
		imgClose = imageLoader.getImage("image.sidebar.closeitem");
		imgCloseSelected = imageLoader.getImage("image.sidebar.closeitem-selected");
		//imgTwist = imageLoader.getImage("image.sidebar.twist");
		//imgUntwist = imageLoader.getImage("image.sidebar.untwist");

		// addTestMenus();

		ViewTitleInfoManager.addListener(this);

		createSideBar();
		setupDefaultItems();

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
								List downloadManagers = gm.getDownloadManagers();
								for (Iterator iter = downloadManagers.iterator(); iter.hasNext();) {
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
		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				SWTSkinObjectSash soSash = (SWTSkinObjectSash) skin.getSkinObject("sidebar-sash");
				if (soSash.getPercent() == 0) {
					if (lastPercent != 0) {
						soSash.setPercent(lastPercent);
					}

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
					// invisible
					lastPercent = soSash.getPercent();
					soSash.setPercent(0);

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
		return soSash.getPercent() != 0.0;
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#showSupport(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		// force visible on first launch
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

		imageLoader = skin.getImageLoader(skinObject.getProperties());
		if (imageLoader != null) {
			imageLoader.releaseImage("image.sidebar.closeitem");
			imageLoader.releaseImage("image.sidebar.closeitem-selected");
		}

		return null;
	}

	private void createSideBar() {
		Composite parent = (Composite) soSideBarList.getControl();

		// there isn't a SWT.NO_SCROLL in pre 3.4
		final int NO_SCROLL = 1 << 4;
		tree = new Tree(parent, SWT.FULL_SELECTION | SWT.V_SCROLL
				| SWT.DOUBLE_BUFFERED | NO_SCROLL);
		tree.setHeaderVisible(false);

		new SideBarToolTips(this, tree);

		tree.setLayoutData(Utils.getFilledFormData());

		SWTSkinProperties skinProperties = skin.getSkinProperties();
		bg = skinProperties.getColor("color.sidebar.bg");
		fg = skinProperties.getColor("color.sidebar.fg");
		bgSel = skinProperties.getColor("color.sidebar.selected.bg");
		fgSel = skinProperties.getColor("color.sidebar.selected.fg");
		colorFocus = skinProperties.getColor("color.sidebar.focus");

		tree.setBackground(bg);
		tree.setForeground(fg);

		FontData[] fontData = tree.getFont().getFontData();
		//fontData[0].setHeight(fontData[0].getHeight() + 1);
		fontData[0].setStyle(SWT.BOLD);
		//fontData[0].setName("Helvetica");
		Utils.getFontHeightFromPX(tree.getDisplay(), fontData, null, 13);
		fontHeader = new Font(tree.getDisplay(), fontData);

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
						int padding = 4;
						//String id = (String) treeItem.getData("Plugin.viewID");
						//SideBarEntrySWT entry = getSideBarInfo(id);
						//if (entry.imageLeft != null) {
						//padding += 4;
						//}

						event.height = 24;// Math.max(event.height, size.y + padding);

						break;
					}
					case SWT.PaintItem: {
						//paintSideBar(event);
						break;
					}

					case SWT.Paint: {
						//System.out.println("Paint: " + event.getBounds() + ";" + event.detail + ";" + event.index);
						Rectangle bounds = event.getBounds();
						//if (tree.getItemCount() == 0) {
						//	return;
						//}
						int indent = END_INDENT ? tree.getClientArea().width - 1 : 0;
						int y = event.y + 1;
						treeItem = tree.getItem(new Point(indent, y));

						while (treeItem != null) {
							String id = (String) treeItem.getData("Plugin.viewID");
							SideBarEntrySWT entry = getEntry(id);
							Rectangle itemBounds = entry.getBounds();
							
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
  							//event.gc.setClipping(newClip);
  
  							paintSideBar(event, entry);

  							y = itemBounds.y + itemBounds.height + 1;
							} else {
								y += tree.getItemHeight();
							}

							if (y > bounds.y + bounds.height) {
								break;
							}
							treeItem = tree.getItem(new Point(indent, y));
						}

						if (tree.getTopItem() != lastTopItem) {
							lastTopItem = tree.getTopItem();
							SideBarEntrySWT[] sideBarEntries = (SideBarEntrySWT[]) mapIdToEntries.values().toArray(
									new SideBarEntrySWT[0]);
							updateSideBarHitAreasY(sideBarEntries);
						}

						break;
					}

					case SWT.EraseItem: {
						//event.detail &= ~SWT.FOREGROUND;
						//event.detail &= ~(SWT.FOREGROUND | SWT.BACKGROUND);
						event.doit = false;
						break;
					}

					case SWT.Resize: {
						tree.redraw();
						break;
					}

					case SWT.Selection: {
						itemSelected(treeItem);
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
						int indent = END_INDENT ? tree.getClientArea().width - 1
								: 0;
						treeItem = tree.getItem(new Point(indent, event.y));
						if (treeItem == null) {
							return;
						}
						String id = (String) treeItem.getData("Plugin.viewID");
						SideBarEntrySWT entry = getEntry(id);

						Rectangle closeArea = (Rectangle) treeItem.getData("closeArea");
						if (closeArea != null && closeArea.contains(event.x, event.y)) {
							treeItem.dispose();
						} else if (currentSideBarEntry != entry && Constants.isOSX) {
							itemSelected(entry.treeItem);
						}

						SideBarVitalityImage[] vitalityImages = entry.getVitalityImages();
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
						if (dropTarget != null && !dropTarget.isDisposed()) {
							dropTarget.dispose();
						}
						saveCloseables();

						break;
					}

					case SWT.Collapse: {
						String id = (String) treeItem.getData("Plugin.viewID");
						SideBarEntrySWT entry = getEntry(id);

						if (entry.disableCollapse) {
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
		//tree.addListener(SWT.PaintItem, treeListener);
		//tree.addListener(SWT.EraseItem, treeListener);

		tree.addListener(SWT.Selection, treeListener);
		tree.addListener(SWT.Dispose, treeListener);

		// For icons
		tree.addListener(SWT.MouseUp, treeListener);
		tree.addListener(SWT.MouseDown, treeListener);

		// to disable collapsing
		tree.addListener(SWT.Collapse, treeListener);

		dropTarget = new DropTarget(tree, DND.DROP_DEFAULT | DND.DROP_MOVE
				| DND.DROP_COPY | DND.DROP_LINK | DND.DROP_TARGET_MOVE);
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

			// @see org.eclipse.swt.dnd.DropTargetAdapter#dragOver(org.eclipse.swt.dnd.DropTargetEvent)
			public void dragOver(DropTargetEvent event) {
				if (!(event.item instanceof TreeItem)) {
					event.detail = DND.DROP_NONE;
					return;
				}
				TreeItem treeItem = (TreeItem) event.item;

				String id = (String) treeItem.getData("Plugin.viewID");
				SideBarEntrySWT entry = getEntry(id);

				if (entry.hasDropListeners()) {
					draggingOver = entry;
					if (Constants.isOSX) {
						tree.redraw();
					}
					if ((event.operations & DND.DROP_LINK) > 0)
						event.detail = DND.DROP_LINK;
					else if ((event.operations & DND.DROP_DEFAULT) > 0)
						event.detail = DND.DROP_DEFAULT;
					else if ((event.operations & DND.DROP_COPY) > 0)
						event.detail = DND.DROP_COPY;
				} else {
					event.detail = DND.DROP_NONE;
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
					return;
				}
				TreeItem treeItem = (TreeItem) event.item;

				String id = (String) treeItem.getData("Plugin.viewID");
				SideBarEntrySWT entry = getEntry(id);

				entry.triggerDropListeners(event.data);
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

				int indent = END_INDENT ? tree.getClientArea().width - 1
						: 0;
				TreeItem treeItem = tree.getItem(new Point(indent, ptMouse.y));
				if (treeItem == null) {
					return;
				}
				String id = (String) treeItem.getData("Plugin.viewID");
				SideBarEntrySWT entry = getEntry(id);

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
			String id = (String) treeItem.getData("Plugin.viewID");
			menuItem.setData("Plugin.viewID", id);
			SideBarEntrySWT entry = getEntry(id);
			ViewTitleInfo titleInfo = entry.getTitleInfo();
			String ind = "";
			if (titleInfo != null) {
				String o = (String) titleInfo.getTitleInfoProperty(ViewTitleInfo.TITLE_INDICATOR_TEXT);
				if (o != null) {
					ind = "  (" + o + ")";
					//ind = "\t" + o;
				}
			}
			menuItem.setText(s + treeItem.getData("text") + ind);
			menuItem.addSelectionListener(dropDownSelectionListener);
			if (currentSideBarEntry != null && currentSideBarEntry.id.equals(id)) {
				menuItem.setSelection(true);
			}

			TreeItem[] subItems = treeItem.getItems();
			if (subItems.length > 0) {
				fillDropDownMenu(menuDropDown, subItems, ++indent);
			}
		}
	}

	/**
	 * @param menuTree
	 *
	 * @since 3.1.0.1
	 */
	protected void fillMenu(Menu menuTree, final SideBarEntry entry) {
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

			if (currentSideBarEntry != null
					&& currentSideBarEntry.datasource instanceof DownloadManager) {

				DownloadManager[] downloads = new DownloadManager[] {
					(DownloadManager) currentSideBarEntry.datasource
				};

				org.eclipse.swt.widgets.MenuItem mi = MenuFactory.createTorrentMenuItem(menuTree);

				mi.setData("downloads", downloads);
				mi.setData("is_detailed_view", new Boolean(true));
			}
		}
	}

	/**
	 * @param event
	 * @param sideBarEntry
	 *
	 * @since 3.1.0.1
	 */
	protected void paintSideBar(Event event, SideBarEntrySWT sideBarEntry) {
		TreeItem treeItem = (TreeItem) event.item;
		Rectangle itemBounds = treeItem.getBounds();

		String text = (String) treeItem.getData("text");
		if (text == null)
			text = "";

		//Point size = event.gc.textExtent(text);
		//Rectangle treeBounds = tree.getBounds();
		GC gc = event.gc;

		gc.setAntialias(SWT.ON);
		gc.setAdvanced(true);
		//gc.setClipping((Rectangle) null);

		boolean selected = (event.detail & SWT.SELECTED) > 0;
		Color fgText = Colors.black;
		if (selected) {
			if (fgSel != null) {
				fgText = fgSel;
			}
			if (bgSel != null) {
				gc.setBackground(bgSel);
			}
			Color color1;
			Color color2;
			if (tree.isFocusControl()) {
				color1 = ColorCache.getColor(gc.getDevice(), "#166688");
				color2 = ColorCache.getColor(gc.getDevice(), "#1c2458");
			} else {
				color1 = ColorCache.getColor(gc.getDevice(), "#447281");
				color2 = ColorCache.getColor(gc.getDevice(), "#393e58");
			}

			gc.setBackground(color1);
			gc.fillRectangle(event.x, itemBounds.y, event.width, 3);

			gc.setForeground(color1);
			gc.setBackground(color2);
			gc.fillGradientRectangle(event.x, itemBounds.y + 3, event.width,
					itemBounds.height - 3, true);
		} else {

			if (fg != null) {
				fgText = fg;
			}
			if (bg != null) {
				gc.setBackground(bg);
			}

			if (sideBarEntry == draggingOver) {
				gc.setBackground(ColorCache.getColor(gc.getDevice(), "#2688aa"));
			}
			
			gc.fillRectangle(event.getBounds());
		}

		Rectangle treeArea = tree.getClientArea();

		gc.setFont(tree.getFont());

		if (sideBarEntry == null) {
			String id = (String) treeItem.getData("Plugin.viewID");
			sideBarEntry = getEntry(id);
		}
		int x1IndicatorOfs = SIDEBAR_SPACING;
		int x0IndicatorOfs = itemBounds.x;

		//System.out.println(System.currentTimeMillis() + "] refhres " + sideBarInfo.getId());
		if (sideBarEntry.titleInfo != null) {
			String textIndicator = (String) sideBarEntry.titleInfo.getTitleInfoProperty(ViewTitleInfo.TITLE_INDICATOR_TEXT);
			if (textIndicator != null) {

				Point textSize = gc.textExtent(textIndicator);
				Point minTextSize = gc.textExtent("99");
				if (textSize.x < minTextSize.x + 2) {
					textSize.x = minTextSize.x + 2;
				}

				int width = textSize.x + textSize.y / 2 + 2;
				x1IndicatorOfs += width + SIDEBAR_SPACING;
				int startX = treeArea.width - x1IndicatorOfs;

				int textOffsetY = 0;

				int height = textSize.y + 3;
				int startY = itemBounds.y + (itemBounds.height - height) / 2;

				//gc.fillRectangle(startX, startY, width, height);

				Pattern pattern;
				Color color1;
				Color color2;

				String[] colors = (String[]) sideBarEntry.titleInfo.getTitleInfoProperty(ViewTitleInfo.TITLE_INDICATOR_COLOR);

				if (colors == null || colors.length != 4) {
					colors = default_indicator_colors;
				}

				if (selected) {
					color1 = ColorCache.getColor(gc.getDevice(), colors[0]);
					color2 = ColorCache.getColor(gc.getDevice(), colors[1]);
					pattern = new Pattern(gc.getDevice(), 0, startY, 0, startY + height,
							color1, 127, color2, 4);
				} else {
					color1 = ColorCache.getColor(gc.getDevice(), colors[2]);
					color2 = ColorCache.getColor(gc.getDevice(), colors[3]);
					pattern = new Pattern(gc.getDevice(), 0, startY, 0, startY + height,
							color1, color2);
				}
				gc.setBackgroundPattern(pattern);
				gc.fillRoundRectangle(startX, startY, width, height, textSize.y + 1,
						height);
				gc.setBackgroundPattern(null);
				pattern.dispose();
				if (maxIndicatorWidth > width) {
					maxIndicatorWidth = width;
				}
				gc.setForeground(Colors.white);
				GCStringPrinter.printString(gc, textIndicator, new Rectangle(startX,
						startY + textOffsetY, width, height), true, false, SWT.CENTER);
			}
		}

		//if (x1IndicatorOfs < 30) {
		//	x1IndicatorOfs = 30;
		//}

		if (sideBarEntry.closeable) {
			Image img = selected ? imgCloseSelected : imgClose;
			Rectangle closeArea = img.getBounds();
			closeArea.x = treeArea.width - closeArea.width - SIDEBAR_SPACING
					- x1IndicatorOfs;
			closeArea.y = itemBounds.y + (itemBounds.height - closeArea.height) / 2;
			x1IndicatorOfs += closeArea.width + SIDEBAR_SPACING;

			//gc.setBackground(treeItem.getBackground());
			//gc.fillRectangle(closeArea);

			gc.drawImage(img, closeArea.x, closeArea.y);
			treeItem.setData("closeArea", closeArea);
		}

		SideBarVitalityImage[] vitalityImages = sideBarEntry.getVitalityImages();
		for (int i = 0; i < vitalityImages.length; i++) {
			SideBarVitalityImageSWT vitalityImage = (SideBarVitalityImageSWT) vitalityImages[i];
			if (!vitalityImage.isVisible() || vitalityImage.getAlignment() != SWT.RIGHT) {
				continue;
			}
			vitalityImage.switchSuffix(selected ? "-selected" : "");
			Image image = vitalityImage.getImage();
			if (image != null && !image.isDisposed()) {
				Rectangle bounds = image.getBounds();
				bounds.x = treeArea.width - bounds.width - SIDEBAR_SPACING
						- x1IndicatorOfs;
				bounds.y = itemBounds.y + (itemBounds.height - bounds.height) / 2;
				x1IndicatorOfs += bounds.width + SIDEBAR_SPACING;

				gc.drawImage(image, bounds.x, bounds.y);
				vitalityImage.setHitArea(bounds);
			}
		}

		String suffix = selected ? "-selected" : null;
		Image imageLeft = sideBarEntry.getImageLeft(suffix);
		if (imageLeft == null && selected) {
			sideBarEntry.releaseImageLeft(suffix);
			suffix = null;
			imageLeft = sideBarEntry.getImageLeft(null);
		}
		if (imageLeft != null) {
			Rectangle bounds = imageLeft.getBounds();
			int x = x0IndicatorOfs + ((IMAGELEFT_SIZE - bounds.width) / 2);
			int y = itemBounds.y + ((itemBounds.height - bounds.height) / 2);
			Rectangle clipping = gc.getClipping();
			gc.setClipping(x0IndicatorOfs, itemBounds.y, IMAGELEFT_SIZE,
					itemBounds.height);
			gc.drawImage(imageLeft, x, y);
			sideBarEntry.releaseImageLeft(suffix);
			gc.setClipping(clipping);
			//			0, 0, bounds.width, bounds.height,
			//					x0IndicatorOfs, itemBounds.y
			//							+ ((itemBounds.height - IMAGELEFT_SIZE) / 2), IMAGELEFT_SIZE,
			//					IMAGELEFT_SIZE);

			x0IndicatorOfs += IMAGELEFT_SIZE + IMAGELEFT_GAP;
		} else if (ALWAYS_IMAGE_GAP) {
			x0IndicatorOfs += IMAGELEFT_SIZE + IMAGELEFT_GAP;
		} else {
			if (treeItem.getParentItem() != null) {
				x0IndicatorOfs += 30 - 18;
			}
		}

		//		gc.setAdvanced(true);
		//		gc.setTextAntialias(SWT.ON);
		//		gc.setAntialias(SWT.ON);
		//		gc.setInterpolation(SWT.HIGH);

		if (treeItem.getParentItem() == null) {
			gc.setFont(fontHeader);
			gc.setForeground(ColorCache.getColor(gc.getDevice(), "#2B2D32"));
		}

		gc.setForeground(fgText);
		Rectangle clipping = new Rectangle(x0IndicatorOfs, itemBounds.y,
				treeArea.width - x1IndicatorOfs - SIDEBAR_SPACING - x0IndicatorOfs,
				itemBounds.height);
		if (event.getBounds().intersects(clipping)) {
			//gc.setClipping(clipping);

			if (text.startsWith(" ")) {
				text = text.substring(1);
				clipping.x += 30;
				clipping.width -= 30;
			}

			GCStringPrinter sp = new GCStringPrinter(gc, text, clipping, true, false, SWT.NONE);
			sp.printString();
			clipping.x += sp.getCalculatedSize().x + 5;
			//gc.setClipping((Rectangle) null);
		}
		
		for (int i = 0; i < vitalityImages.length; i++) {
			SideBarVitalityImageSWT vitalityImage = (SideBarVitalityImageSWT) vitalityImages[i];
			if (!vitalityImage.isVisible() || vitalityImage.getAlignment() != SWT.LEFT) {
				continue;
			}
			vitalityImage.switchSuffix(selected ? "-selected" : "");
			Image image = vitalityImage.getImage();
			if (image != null && !image.isDisposed()) {
				Rectangle bounds = image.getBounds();
				bounds.x = clipping.x;
				bounds.y = itemBounds.y + (itemBounds.height - bounds.height) / 2;
				clipping.x += bounds.width + SIDEBAR_SPACING;

				if (clipping.x > (treeArea.width - x1IndicatorOfs)) {
					vitalityImage.setHitArea(null);
					continue;
				}
				gc.drawImage(image, bounds.x, bounds.y);
				vitalityImage.setHitArea(bounds);
			}
		}


		// OSX overrides the twisty, and we can't use the default twisty
		// on Windows because it doesn't have transparency and looks ugly
		if (treeItem.getItemCount() > 0 && !sideBarEntry.disableCollapse) {
			gc.setAntialias(SWT.ON);
			Color oldBG = gc.getBackground();
			gc.setBackground(gc.getForeground());
			if (treeItem.getExpanded()) {
				int xStart = 15;
				int arrowSize = 8;
				int yStart = itemBounds.height - (itemBounds.height + arrowSize) / 2;
				gc.fillPolygon(new int[] {
					itemBounds.x - xStart,
					itemBounds.y + yStart,
					itemBounds.x - xStart + arrowSize,
					itemBounds.y + yStart,
					itemBounds.x - xStart + (arrowSize / 2),
					itemBounds.y + 16,
				});
			} else {
				int xStart = 15;
				int arrowSize = 8;
				int yStart = itemBounds.height - (itemBounds.height + arrowSize) / 2;
				gc.fillPolygon(new int[] {
					itemBounds.x - xStart,
					itemBounds.y + yStart,
					itemBounds.x - xStart + arrowSize,
					itemBounds.y + yStart + 4,
					itemBounds.x - xStart,
					itemBounds.y + yStart + 8,
				});
			}
			gc.setBackground(oldBG);
			gc.setFont(fontHeader);
		}
	}

	/**
	 * 
	 *
	 * @since 3.1.1.1
	 */
	private void updateSideBarHitAreasY(SideBarEntrySWT[] entries) {
		for (int x = 0; x < entries.length; x++) {
			SideBarEntrySWT entry = entries[x];
			TreeItem treeItem = entry.treeItem;
			if (treeItem == null || treeItem.isDisposed()) {
				continue;
			}
			Rectangle itemBounds = entry.getBounds();

			if (entry.closeable) {
				Rectangle closeArea = (Rectangle) treeItem.getData("closeArea");
				if (closeArea != null) {
					closeArea.y = itemBounds.y + (itemBounds.height - closeArea.height)
							/ 2;
				}
			}

			SideBarVitalityImage[] vitalityImages = entry.getVitalityImages();
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

		SideBarEntrySWT entry;
		ImageLoader imageLoader = ImageLoader.getInstance();

		entry = createEntryFromSkinRef(null, SIDEBAR_SECTION_LIBRARY, "library",
				MessageText.getString("sidebar." + SIDEBAR_SECTION_LIBRARY), null,
				null, false, -1);
		entry.setImageLeftID("image.sidebar.library");
		entry.disableCollapse = true;

		addDropTest(entry);

		createEntryFromSkinRef(SIDEBAR_SECTION_LIBRARY, SIDEBAR_SECTION_LIBRARY_DL,
				"library", MessageText.getString("sidebar.LibraryDL"), null, null,
				false, -1);

		createEntryFromSkinRef(SIDEBAR_SECTION_LIBRARY,
				SIDEBAR_SECTION_LIBRARY_UNOPENED, "library",
				MessageText.getString("sidebar.LibraryUnopened"), null, null, false, -1);
		addMenuUnwatched();

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

		SideBarEntrySWT entryActivity = createEntryFromSkinRef(null,
				SIDEBAR_SECTION_ACTIVITIES, "activity",
				MessageText.getString("sidebar." + SIDEBAR_SECTION_ACTIVITIES),
				titleInfoActivityView, null, false, -1);
		addMenuNotifications();

		//entry.setImageLeftID("image.sidebar.subscriptions");

		//new TreeItem(tree, SWT.NONE).setText("Search");

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
			SideBarEntrySWT pluginsEntry = createEntryFromSkinRef(null,
					SIDEBAR_SECTION_PLUGINS, "main.area.plugins", "Plugins", null, null,
					false, -1);

			IViewInfo[] pluginViewsInfo = PluginsMenuHelper.getInstance().getPluginViewsInfo();
			for (int i = 0; i < pluginViewsInfo.length; i++) {
				IViewInfo viewInfo = pluginViewsInfo[i];
				treeItem = new TreeItem(pluginsEntry.treeItem, SWT.NONE);
				treeItem.addDisposeListener(disposeTreeItemListener);

				treeItem.setData("text", viewInfo.name);
				treeItem.setData("Plugin.viewID", viewInfo.viewID);
				SideBarEntrySWT entryPlugin = getEntry(viewInfo.viewID);
				entryPlugin.treeItem = treeItem;
				entryPlugin.iview = viewInfo.view;
				entryPlugin.eventListener = viewInfo.event_listener;
			}

			TreeItem itemPluginLogs = new TreeItem(pluginsEntry.treeItem, SWT.NONE);
			itemPluginLogs.setText("Log Views");
			IViewInfo[] pluginLogViewsInfo = PluginsMenuHelper.getInstance().getPluginLogViewsInfo();
			for (int i = 0; i < pluginLogViewsInfo.length; i++) {
				IViewInfo viewInfo = pluginLogViewsInfo[i];
				treeItem = new TreeItem(itemPluginLogs, SWT.NONE);
				treeItem.addDisposeListener(disposeTreeItemListener);

				treeItem.setData("text", viewInfo.name);
				treeItem.setData("Plugin.viewID", viewInfo.viewID);
				SideBarEntrySWT entryPlugin = getEntry(viewInfo.viewID);
				entryPlugin.treeItem = treeItem;
				entryPlugin.iview = viewInfo.view;
				entryPlugin.eventListener = viewInfo.event_listener;
			}
		}

		SBC_LibraryView.setupViewTitle();

		// building plugin views needs UISWTInstance, which needs core.
		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener(){
			public void azureusCoreRunning(AzureusCore core) {
				Utils.execSWTThread(new AERunnable(){
					public void runSupport() {
						setupPluginViews();
					}
				});
			}
		});

		loadCloseables();

		if (System.getProperty("v3.sidebar.advanced", "0").equals("1")) {
			createEntryFromSkinRef(null, SIDEBAR_SECTION_ADVANCED,
					"main.area.advancedtab", "Advanced", null, null, false, -1);
		}

		Composite parent = tree.getParent();

		if (parent.isVisible()) {
			parent.layout(true, true);
		}
	}

	protected void setupPluginViews() {
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
						//System.out.println("PluginView Added: " + viewInfo.viewID);
						Object o = mapAutoOpen.get(viewInfo.viewID);
						if (o instanceof Map) {
							processAutoOpenMap(viewInfo.viewID, (Map) o, viewInfo);
						}
					}
				});
	}

	/**
		 * @param entry
		 *
		 * @since 4.1.0.3
		 */
	private void addDropTest(SideBarEntrySWT entry) {
		if (!Constants.isCVSVersion()) {
			return;
		}
		entry.addListener(new SideBarDropListener() {
			public void sideBarEntryDrop(SideBarEntry entry, Object droppedObject) {
				String s = "You just dropped " + droppedObject.getClass() + "\n"
						+ droppedObject + "\n\n";
				if (droppedObject.getClass().isArray()) {
					Object[] o = (Object[]) droppedObject;
					for (int i = 0; i < o.length; i++) {
						s += "" + i + ":  ";
						Object object = o[i];
						if (object == null) {
							s += "null";
						} else {
							s += object.getClass() + ";" + object;
						}
						s += "\n";
					}
				}
				Utils.openMessageBox(null, SWT.OK, "test", s);
			}
		});

	}

	/**
	 * 
	 *
	 * @return 
	 * @since 3.1.1.1
	 */
	private SideBarEntrySWT createWelcomeSection() {
		SideBarEntrySWT entry = createEntryFromSkinRef(null,
				SIDEBAR_SECTION_WELCOME, "main.area.welcome", MessageText.getString(
						"v3.MainWindow.menu.getting_started").replaceAll("&", ""), null,
				null, true, 0);
		entry.setImageLeftID("image.sidebar.welcome");
		return entry;
	}

	public TreeItem createTreeItemFromIView(String parentID, IView iview,
			String id, Object datasource, boolean closeable, boolean show) {

		return (createTreeItemFromIView(parentID, iview, id, datasource, closeable,
				show, true));
	}

	public TreeItem createTreeItemFromIView(String parentID, IView iview,
			String id, Object datasource, boolean closeable, boolean show,
			boolean expand) {
		if (id == null) {
			id = iview.getClass().getName();
			int i = id.lastIndexOf('.');
			if (i > 0) {
				id = id.substring(i + 1);
			}
		}
		TreeItem treeItem = null;

		SideBarEntrySWT entry = getEntry(id);

		if (entry.treeItem != null) {
			treeItem = entry.treeItem;
		} else {
			SideBarEntrySWT sideBarInfoParent = getEntry(parentID);
			TreeItem parentTreeItem = sideBarInfoParent.treeItem;
			treeItem = createTreeItem(parentTreeItem, id, datasource, null,
					iview.getFullTitle(), closeable, -1);

			setupTreeItem(null, treeItem, id, null, iview.getFullTitle(), null,
					datasource, closeable, expand);

			entry.parentID = parentID;

			createSideBarContentArea(id, iview, treeItem, datasource, closeable,
					expand);

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

	public TreeItem createTreeItemFromIViewClass(String parent, String id,
			String title, Class iviewClass, boolean closeable) {

		return createTreeItemFromIViewClass(parent, id, title, iviewClass, null,
				null, null, null, closeable);
	}

	public TreeItem createTreeItemFromIViewClass(String parent, String id,
			String title, Class iviewClass, Class[] iviewClassArgs,
			Object[] iviewClassVals, Object datasource, ViewTitleInfo titleInfo,
			boolean closeable) {

		SideBarEntrySWT entry = getEntry(id);
		if (entry.treeItem != null) {
			return entry.treeItem;
		}

		TreeItem parentItem = parent == null ? null : getEntry(parent).treeItem;

		TreeItem treeItem;
		if (parentItem != null) {
			treeItem = new TreeItem(parentItem, SWT.NONE);
		} else {
			treeItem = new TreeItem(tree, SWT.NONE);
		}

		entry.iviewClass = iviewClass;
		entry.iviewClassArgs = iviewClassArgs;
		entry.iviewClassVals = iviewClassVals;
		entry.closeable = closeable;
		entry.parentID = parent;
		setupTreeItem(null, treeItem, id, titleInfo, title, null, datasource,
				closeable, false );

		return treeItem;
	}

	private TreeItem createTreeItem(Object parentTreeItem, String id,
			Object datasource, ViewTitleInfo titleInfo, String title,
			boolean closeable, int index) {
		TreeItem treeItem;

		if (parentTreeItem == null) {
			parentTreeItem = tree;
		}

		if (parentTreeItem instanceof Tree) {
			if (index >= 0) {
				treeItem = new TreeItem((Tree) parentTreeItem, SWT.NONE, index);
			} else {
				treeItem = new TreeItem((Tree) parentTreeItem, SWT.NONE);
			}
		} else {
			if (index >= 0) {
				treeItem = new TreeItem((TreeItem) parentTreeItem, SWT.NONE, index);
			} else {
				treeItem = new TreeItem((TreeItem) parentTreeItem, SWT.NONE);
			}
		}

		return treeItem;
	}

	private void setupTreeItem(final IView iview, TreeItem treeItem, String id,
			ViewTitleInfo titleInfo, String title, Composite initializeView,
			Object datasource, boolean closeable, boolean expand) {
		final SideBarEntrySWT entry = getEntry(id);

		boolean pull = true;
		if (treeItem.getParentItem() != null && expand) {
			treeItem.getParentItem().setExpanded(true);
		}
		if (!expand) {
			treeItem.setExpanded(false);
		}
		treeItem.removeDisposeListener(disposeTreeItemListener);
		treeItem.addDisposeListener(disposeTreeItemListener);
		treeItem.setData("Plugin.viewID", id);

		if (iview != null) {
			entry.iview = iview;
		}

		if (title != null) {
			treeItem.setData("text", title);
		} else {
			if (entry.iview != null) {
				treeItem.setData("text", entry.iview.getFullTitle());
			}
		}

		if (titleInfo == null) {
			if (entry.iview instanceof ViewTitleInfo) {
				titleInfo = (ViewTitleInfo) entry.iview;
			} else if (entry.iview instanceof UISWTViewImpl) {
				UISWTViewEventListener eventListener = ((UISWTViewImpl) entry.iview).getEventListener();
				if (eventListener instanceof ViewTitleInfo) {
					titleInfo = (ViewTitleInfo) eventListener;
				}
			}

		}

		if (titleInfo != null) {
			entry.titleInfo = titleInfo;
		}

		if (entry.titleInfo != null) {
			mapTitleInfoToEntry.put(entry.titleInfo, entry);
			String newText = (String) entry.titleInfo.getTitleInfoProperty(ViewTitleInfo.TITLE_TEXT);
			if (newText != null) {
				pull = false;
				treeItem.setData("text", newText);
			}

			String imageID = (String) entry.titleInfo.getTitleInfoProperty(ViewTitleInfo.TITLE_IMAGEID);
			if (imageID != null) {
				entry.setImageLeftID(imageID.length() == 0 ? null : imageID);
			}
			listTreeItemsNoTitleInfo.remove(treeItem);
		} else {
			if (!listTreeItemsNoTitleInfo.contains(treeItem)) {
				listTreeItemsNoTitleInfo.add(treeItem);
			}
		}

		if (treeItem != null) {
			entry.treeItem = treeItem;
		}
		if (datasource != null) {
			entry.datasource = datasource;
		}

		entry.closeable = closeable;

		entry.pullTitleFromIView = pull;

		if (closeable) {
			Map autoOpenInfo = new LightHashMap();
			if (entry.parentID != null) {
				autoOpenInfo.put("parentID", entry.parentID);
			}
			if (entry.iviewClass != null) {
				autoOpenInfo.put("iviewClass", entry.iviewClass.getName());
			}
			if (entry.eventListener != null) {
				autoOpenInfo.put("eventlistenerid", id);
			}
			if (entry.iview != null) {
				autoOpenInfo.put("title", entry.iview.getFullTitle());
			}
			if (entry.datasource instanceof DownloadManager) {
				try {
					autoOpenInfo.put(
							"dm",
							((DownloadManager) entry.datasource).getTorrent().getHashWrapper().toBase32String());
				} catch (Throwable t) {
				}
			}

			mapAutoOpen.put(id, autoOpenInfo);
		}

		if (initializeView != null) {
			iview.initialize(initializeView);
			initializeView.setVisible(false);
			Composite composite = iview.getComposite();
			if (composite != null && !composite.isDisposed()) {
				composite.setVisible(false);
				composite.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent e) {
						if (entry.treeItem != null && !entry.treeItem.isDisposed()) {
							try {
								entry.treeItem.dispose();
							} catch (NullPointerException npe) {
								// ignore swt bug
							}
						}
					}
				});
			}
			if (entry.datasource != null) {
				iview.dataSourceChanged(entry.datasource);
			}
			initializeView.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					if (entry.iview != null) {
						try {
							entry.iview.delete();
						} catch (Throwable t) {
							Debug.out(t);
						}
						entry.iview = null;
					}
				}
			});
		}
	}

	public static SideBarEntrySWT getEntry(String id) {
		if ("Browse".equalsIgnoreCase(id)) {
			id = SIDEBAR_SECTION_BROWSE;
		}
		SideBarEntrySWT entry = (SideBarEntrySWT) mapIdToEntries.get(id);
		if (entry == null) {
			entry = new SideBarEntrySWT(instance, id);
			mapIdToEntries.put(id, entry);
		}
		return entry;
	}

	public static boolean entryExists(String id) {
		if ("Browse".equalsIgnoreCase(id)) {
			id = SIDEBAR_SECTION_BROWSE;
		}
		SideBarEntrySWT entry = (SideBarEntrySWT) mapIdToEntries.get(id);
		if (entry == null) {
			return false;
		}
		return entry.treeItem != null;
	}

	/**
	 * @param treeItem
	 *
	 * @since 3.1.1.1
	 */
	private void itemSelected(final TreeItem treeItem) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				_itemSelected(treeItem);
			}
		});
	}

	private void disabledViewModes() {
		ToolBarView tb = (ToolBarView) SkinViewManager.getByClass(ToolBarView.class);
		if (tb != null) {
			ToolBarItem itemModeSmall = tb.getToolBarItem("modeSmall");
			if (itemModeSmall != null) {
				itemModeSmall.getSkinButton().getSkinObject().switchSuffix("");
				itemModeSmall.setEnabled(false);
			}
			ToolBarItem itemModeBig = tb.getToolBarItem("modeBig");
			if (itemModeBig != null) {
				itemModeBig.getSkinButton().getSkinObject().switchSuffix("");
				itemModeBig.setEnabled(false);
			}
		}
	}

	private void _itemSelected(TreeItem treeItem) {
		TreeItem[] selection = tree.getSelection();
		if (selection == null || selection.length == 0 || selection[0] != treeItem) {
			tree.showItem(treeItem);
			tree.select(treeItem);
		}

		final String id = (String) treeItem.getData("Plugin.viewID");
		final SideBarEntrySWT newEntry = getEntry(id);

		if (currentSideBarEntry == newEntry) {
			triggerSelectionListener(newEntry, newEntry);
			return;
		}

		// We'll have an iview if we've previously created one
		IView newIView = newEntry.iview;

		// Otherwise, we have two ways of creating an IView.. via IViewClass,
		// or UISWTViewEventListener

		if (newIView == null) {
			if (newEntry.iviewClass != null) {
				newIView = createSideBarContentArea(id, newEntry);

				if (newIView == null) {
					return;
				}

				setupTreeItem(newIView, treeItem,
						(String) treeItem.getData("Plugin.viewID"), null,
						newIView.getFullTitle(), null, null, newEntry.closeable, true);
			}
		}

		if (newIView == null && newEntry.eventListener != null) {
			newIView = createTreeItemFromEventListener(null, treeItem,
					newEntry.eventListener, id, newEntry.closeable, null);
		}

		if (newIView != null) {

			if (newIView instanceof ToolBarEnabler) {

				ISelectedContent[] sels = new ISelectedContent[1];
				sels[0] = new ToolBarEnablerSelectedContent((ToolBarEnabler) newIView);
				TableView tv = null;
				if (newIView instanceof TableView) {
					tv = (TableView) newIView;
				}
				SelectedContentManager.changeCurrentlySelectedContent("IconBarEnabler",
						sels, tv);

			} else {

				SelectedContentManager.clearCurrentlySelectedContent();

			}

			disabledViewModes();

			Utils.execSWTThreadLater(0, new AERunnable() {

				public void runSupport() {
					flipVisibilityTo(newEntry);
				}
			});

		}
	}

	protected void flipVisibilityTo(SideBarEntrySWT newSideBarInfo) {
		if (tree.isDisposed()) {
			return;
		}

		final SideBarEntrySWT oldEntry = currentSideBarEntry;

		// show new
		currentSideBarEntry = newSideBarInfo;

		SWTSkinObjectContainer container;

		if (currentSideBarEntry.iview instanceof UISWTViewImpl) {
			Object ds = ((UISWTViewImpl) currentSideBarEntry.iview).getDataSource();
			DownloadManager dm = null;
			if (ds instanceof DownloadManager) {
				dm = (DownloadManager) ds;
			} else if (ds instanceof Download) {
				dm = PluginCoreUtils.unwrap((Download) ds);
			}
			if (dm != null) {
				try {
					TableView tv = null;
					if (currentSideBarEntry.iview instanceof TableView) {
						tv = (TableView) currentSideBarEntry.iview;
					}
					SelectedContentManager.changeCurrentlySelectedContent(
							currentSideBarEntry.id, new ISelectedContent[] {
								new SelectedContentV3(dm)
							}, tv);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		container = (SWTSkinObjectContainer) newSideBarInfo.skinObject;
		if (container != null) {
			//container.setVisible(true);
			Composite composite = container.getComposite();
			if (composite != null && !composite.isDisposed()) {
				composite.setVisible(true);
				composite.moveAbove(null);
				//composite.setFocus();
			}
		}
		Composite c = currentSideBarEntry.iview.getComposite();
		if (c != null && !c.isDisposed()) {
			c.setVisible(true);
		}

		// hide old
		if (oldEntry != null && oldEntry != newSideBarInfo) {
			if (lastImage != null && !lastImage.isDisposed()) {
				lastImage.dispose();
				lastImage = null;
			}
			if (oldEntry.skinObject != null) {
				container = (SWTSkinObjectContainer) oldEntry.skinObject;
				if (container != null) {
					Control oldComposite = container.getControl();
					doFade(oldComposite);

					container.setVisible(false);
					if (!oldComposite.isDisposed()) {
						oldComposite.getShell().update();
					}
				}
			}
			if (oldEntry.iview != null) {
				Composite oldComposite = oldEntry.iview.getComposite();
				if (oldComposite != null && !oldComposite.isDisposed()) {
					doFade(oldComposite);

					oldComposite.setVisible(false);
					oldComposite.getShell().update();
				}
			}
		}

		triggerSelectionListener(newSideBarInfo, oldEntry);
	}

	/**
	 * @param oldComposite
	 *
	 * @since 3.1.1.1
	 */
	private void doFade(final Control oldComposite) {
		if (true) {
			return;
		}
		if (oldComposite.isDisposed()
				|| (shellFade != null && !shellFade.isDisposed())) {
			return;
		}
		final Shell parentShell = oldComposite.getShell();
		if (parentShell != oldComposite.getDisplay().getActiveShell()) {
			return;
		}

		if (lastImage == null || lastImage.isDisposed()) {
			Rectangle bounds = oldComposite.getBounds();
			if (bounds.isEmpty()) {
				return;
			}
			lastImage = new Image(oldComposite.getDisplay(), bounds.width,
					bounds.height);
			GC gc = new GC(oldComposite);
			gc.copyArea(lastImage, 0, 0);
			gc.dispose();

			int style = SWT.NO_TRIM;
			if (Constants.isOSX) {
				style |= SWT.ON_TOP;
			}
			shellFade = new Shell(soSideBarContents.getControl().getShell(), style);
			Point pos = soSideBarContents.getControl().toDisplay(0, 0);
			shellFade.setBackgroundImage(lastImage);
			shellFade.setLocation(pos);
			shellFade.setSize(soSideBarContents.getControl().getSize());
			shellFade.setAlpha(255);
			shellFade.setVisible(true);
			shellFade.addListener(SWT.Dispose, new Listener() {
				public void handleEvent(Event event) {
					if (lastImage != null && !lastImage.isDisposed()) {
						lastImage.dispose();
					}
				}
			});
			Utils.execSWTThreadLater(15, new AERunnable() {
				long lastTime;

				public void runSupport() {
					if (shellFade.isDisposed()) {
						return;
					}
					if (lastImage == null || lastImage.isDisposed()
							|| parentShell != parentShell.getDisplay().getActiveShell()) {
						shellFade.dispose();
						return;
					}

					int alpha = shellFade.getAlpha();
					alpha -= 50;
					long now = SystemTime.getCurrentTime();

					if (alpha < 0 || lastImage == null || lastImage.isDisposed()) {
						shellFade.dispose();
						return;
					}
					shellFade.setAlpha(alpha);
					//System.out.println(alpha);

					if (lastTime > 0 && now - lastTime > 50) {
						Utils.execSWTThreadLater(0, this);
					} else {
						Utils.execSWTThreadLater(15, this);
					}
					lastTime = now;
				};
			});
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
		SideBarEntrySWT entry = getEntry(id);

		if (entry.treeItem != null && entry.iview != null) {
			if (entry.eventListener == l) {
				System.err.println("Already created view " + id);
			}
			id = id + "_" + SystemTime.getCurrentTime();
			entry = getEntry(id);
		}

		String name = entry.treeItem == null ? id
				: (String) entry.treeItem.getData("text");

		if (treeItem == null) {
			TreeItem parentTreeItem = parentID == null ? null
					: getEntry(parentID).treeItem;

			IViewInfo foundViewInfo = null;
			IViewInfo[] pluginViewsInfo = PluginsMenuHelper.getInstance().getPluginViewsInfo();
			for (int i = 0; i < pluginViewsInfo.length; i++) {
				IViewInfo viewInfo = pluginViewsInfo[i];
				if (viewInfo.event_listener == l) {
					foundViewInfo = viewInfo;
					break;
				}
			}
			if (foundViewInfo == null) {
				pluginViewsInfo = PluginsMenuHelper.getInstance().getPluginLogViewsInfo();
				for (int i = 0; i < pluginViewsInfo.length; i++) {
					IViewInfo viewInfo = pluginViewsInfo[i];
					if (viewInfo.event_listener == l) {
						foundViewInfo = viewInfo;
						break;
					}
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

			entry.eventListener = l;
			entry.parentID = parentID;
			treeItem = createTreeItem(parentTreeItem, id, datasource, null, name,
					closeable, -1);
		}

		IView iview = null;
		try {
			iview = new UISWTViewImpl(parentID, id, l, datasource);
			((UISWTViewImpl) iview).setTitle(name);
			iview.dataSourceChanged(datasource);

			if (l instanceof UISWTViewEventListenerSkinObject) {
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
			if (l instanceof UISWTViewEventListenerSkinObject) {
				viewComposite.setLayout(new FormLayout());
			} else {
				GridLayout gridLayout = new GridLayout();
				gridLayout.horizontalSpacing = gridLayout.verticalSpacing = gridLayout.marginHeight = gridLayout.marginWidth = 0;
				viewComposite.setLayout(gridLayout);
			}

			viewComposite.setVisible(false);

			SWTSkinObjectContainer soContents = new SWTSkinObjectContainer(skin,
					skin.getSkinProperties(), viewComposite, "Contents." + id + "."
							+ (mapIdToEntries.size() + 1), "", "container", soSideBarContents);

			entry.skinObject = soContents;
			entry.eventListener = l;
			entry.parentID = parentID;

			setupTreeItem(iview, treeItem, id, null, iview.getFullTitle(),
					viewComposite, datasource, closeable, true);

			if (parent.isVisible()) {
				parent.layout(true, true);
			}

			if (closeable) {
				COConfigurationManager.setParameter("SideBar.AutoOpen." + id, true);
			} else {
				COConfigurationManager.removeParameter("SideBar.AutoOpen." + id);
			}

		} catch (UISWTViewEventCancelledException e) {
			if (treeItem != null && !treeItem.isDisposed()) {
				treeItem.dispose();
			}
			showEntryByID(originialID);
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
				String text = (String) treeItem.getData("text");
				if (text != null) {
					((UISWTViewImpl) iview).setTitle(text);
				}

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
								+ (mapIdToEntries.size() + 1), "", "container",
						soSideBarContents);

				entry.skinObject = soContents;
				entry.eventListener = l;

				setupTreeItem(iview, treeItem, id, null, iview.getFullTitle(),
						viewComposite, datasource, closeable, true);

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
	private IView createSideBarContentArea(String id, SideBarEntrySWT sideBarInfo) {
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
					sideBarInfo.datasource, sideBarInfo.closeable, true);
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
			Object datasource, boolean closeable, boolean expand) {
		try {
			Composite parent = (Composite) soSideBarContents.getControl();

			SWTSkinObjectContainer soContents = new SWTSkinObjectContainer(skin,
					skin.getSkinProperties(), "Contents" + (mapIdToEntries.size() + 1),
					"", soSideBarContents);

			Composite viewComposite = soContents.getComposite();
			viewComposite.setBackground(parent.getDisplay().getSystemColor(
					SWT.COLOR_WIDGET_BACKGROUND));
			viewComposite.setForeground(parent.getDisplay().getSystemColor(
					SWT.COLOR_WIDGET_FOREGROUND));
			viewComposite.setLayoutData(Utils.getFilledFormData());
			GridLayout gridLayout = new GridLayout();
			gridLayout.horizontalSpacing = gridLayout.verticalSpacing = gridLayout.marginHeight = gridLayout.marginWidth = 0;
			viewComposite.setLayout(gridLayout);

			SideBarEntrySWT entry = getEntry((String) item.getData("Plugin.viewID"));
			entry.skinObject = soContents;

			setupTreeItem(view, item, id, null, null, viewComposite, datasource,
					closeable, expand);

			Composite iviewComposite = view.getComposite();
			Object existingLayout = iviewComposite.getLayoutData();
			if (existingLayout == null || (existingLayout instanceof GridData)) {
				GridData gridData = new GridData(GridData.FILL_BOTH);
				iviewComposite.setLayoutData(gridData);
			}

			if (iviewComposite.isVisible()) {
			parent.layout(true, true);
			}

		} catch (Exception e) {
			Debug.out("Error creating sidebar content area for " + id, e);
			if (view != null) {
				view.delete();
			}
			view = null;
			item.dispose();
		}
		return view;
	}

	public SideBarEntrySWT createEntryFromSkinRef(String parentID,
			final String id, final String configID, String title,
			ViewTitleInfo titleInfo, final Object params, boolean closeable, int index) {

		// temp until we start passing ds
		Object datasource = null;

		SideBarEntrySWT entry = getEntry(id);
		if (entry.treeItem != null) {
			return entry;
		}

		TreeItem treeItem = null;

		UISWTViewEventListener l = new UISWTViewEventListenerSkinObject() {
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
							skinObject.getControl().setLayoutData(Utils.getFilledFormData());
							skinObject.getControl().getParent().layout(true);
						} finally {
							shell.setCursor(cursor);
						}
					}
						break;
					case UISWTViewEvent.TYPE_REFRESH: {

						break;
					}

					case UISWTViewEvent.TYPE_DESTROY: {
						break;
					}
				}
				return true;
			}
		};
		entry.eventListener = l;

		TreeItem parentTreeItem = parentID == null ? null
				: getEntry(parentID).treeItem;

		entry.parentID = parentID;

		treeItem = createTreeItem(parentTreeItem == null ? (Object) tree
				: (Object) parentTreeItem, id, datasource, titleInfo, title, closeable,
				index);

		setupTreeItem(null, treeItem, id, titleInfo, title, null, datasource,
				closeable, true);

		return entry;
	}

	// @see com.aelitis.azureus.ui.swt.utils.UIUpdatable#getUpdateUIName()
	public String getUpdateUIName() {
		if (currentSideBarEntry == null || currentSideBarEntry.iview == null) {
			return "Sidebar";
		}
		if (currentSideBarEntry.iview instanceof UIPluginView) {
			UIPluginView uiPluginView = (UIPluginView) currentSideBarEntry.iview;
			return uiPluginView.getViewID();
		}

		return currentSideBarEntry.iview.getFullTitle();
	}

	// @see com.aelitis.azureus.ui.swt.utils.UIUpdatable#updateUI()
	public void updateUI() {
		if (currentSideBarEntry == null || currentSideBarEntry.iview == null
				|| tree.getSelectionCount() == 0) {
			return;
		}
		currentSideBarEntry.iview.refresh();

		SideBarEntrySWT entry = getEntry(currentSideBarEntry.id);
		if (entry.pullTitleFromIView && entry.treeItem != null
				&& !entry.treeItem.isDisposed()) {
			entry.treeItem.setData("text", currentSideBarEntry.iview.getFullTitle());
		}
	}

	public static abstract class UISWTViewEventListenerSkinObject
		implements UISWTViewEventListener
	{
		protected SWTSkinObject skinObject;

		public SWTSkinObject getSkinObject() {
			return skinObject;
		}

	}

	// @see com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoListener#viewTitleInfoRefresh(com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo)
	public void viewTitleInfoRefresh(final ViewTitleInfo titleIndicator) {
		if (titleIndicator == null) {
			return;
		}
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (tree == null || tree.isDisposed()) {
					return;
				}

				SideBarEntrySWT sideBarEntry = (SideBarEntrySWT) mapTitleInfoToEntry.get(titleIndicator);
				if (sideBarEntry == null) {
					Object o = titleIndicator.getTitleInfoProperty(ViewTitleInfo.TITLE_SKINVIEW);
					if (o instanceof SkinView) {
						SkinView skinView = (SkinView) o;
						String id = skinView.getMainSkinObject().getSkinObjectID();
						if (id != null) {
							for (Iterator iter = listTreeItemsNoTitleInfo.iterator(); iter.hasNext();) {
								TreeItem searchTreeItem = (TreeItem) iter.next();
								if (searchTreeItem.isDisposed()) {
									iter.remove();
									continue;
								}
								String treeItemID = (String) searchTreeItem.getData("Plugin.viewID");
								if (treeItemID != null && treeItemID.equals(id)) {
									sideBarEntry = getEntry(treeItemID);
									if (sideBarEntry.treeItem != null) {
										sideBarEntry.titleInfo = titleIndicator;
										mapTitleInfoToEntry.put(titleIndicator, sideBarEntry);
									}
									break;
								}
							}
						}
					}

					if (sideBarEntry == null || sideBarEntry.treeItem == null) {
						return;
					}
				}

				if (sideBarEntry.treeItem == null || sideBarEntry.treeItem.isDisposed()) {
					return;
				}

				String newText = (String) titleIndicator.getTitleInfoProperty(ViewTitleInfo.TITLE_TEXT);
				if (newText != null) {
					sideBarEntry.pullTitleFromIView = false;
					sideBarEntry.treeItem.setData("text", newText);
				}

				String imageID = (String) titleIndicator.getTitleInfoProperty(ViewTitleInfo.TITLE_IMAGEID);
				if (imageID != null) {
					sideBarEntry.setImageLeftID(imageID.length() == 0 ? null : imageID);
				}

				sideBarEntry.redraw();

				String logID = (String) titleIndicator.getTitleInfoProperty(ViewTitleInfo.TITLE_LOGID);
				if (logID != null) {
					sideBarEntry.setLogID(logID);
				}
			}
		});
	}

	public SideBarEntrySWT getCurrentEntry() {
		return currentSideBarEntry;
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

	private void triggerSelectionListener(SideBarEntrySWT newSideBarEntry,
			SideBarEntrySWT oldSideBarEntry) {
		for (Iterator iter = listeners.iterator(); iter.hasNext();) {
			SideBarListener l = (SideBarListener) iter.next();
			l.sidebarItemSelected(newSideBarEntry, oldSideBarEntry);
		}
	}

	public IView getIViewFromID(String id) {
		if (id == null) {
			return null;
		}
		return getEntry(id).iview;
	}

	public void saveCloseables() {
		// update title
		for (Iterator iter = mapAutoOpen.keySet().iterator(); iter.hasNext();) {
			String id = (String) iter.next();
			Object o = mapAutoOpen.get(id);

			if (o instanceof Map) {

				SideBarEntrySWT entry = getEntry(id);
				Map autoOpenInfo = (Map) o;

				if (entry.treeItem != null && !entry.treeItem.isDisposed()) {
					String s = (String) entry.treeItem.getData("text");
					if (s != null) {
						autoOpenInfo.put("title", s);
					}
				}

			}
		}

		FileUtil.writeResilientConfigFile("sidebarauto.config", mapAutoOpen);
	}

	public void loadCloseables() {
		mapAutoOpen = FileUtil.readResilientConfigFile("sidebarauto.config", true);
		if (mapAutoOpen.isEmpty()) {
			return;
		}
		BDecoder.decodeStrings(mapAutoOpen);
		for (Iterator iter = mapAutoOpen.keySet().iterator(); iter.hasNext();) {
			String id = (String) iter.next();
			Object o = mapAutoOpen.get(id);

			if (o instanceof Map) {
				if (!processAutoOpenMap(id, (Map) o, null)) {
					iter.remove();
				}
			}
		}
	}

	/**
	 * @param viewInfo 
	 * @param o
	 *
	 * @since 3.1.1.1
	 */
	private boolean processAutoOpenMap(String id, Map autoOpenInfo,
			IViewInfo viewInfo) {
		try {
			SideBarEntrySWT entry = getEntry(id);
			if (entry.treeItem != null) {
				return true;
			}

			if (id.equals(SIDEBAR_SECTION_WELCOME)) {
				createWelcomeSection();
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
				if (entry.iview == null) {
					createSideBarContentArea(id, entry);
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
					// XXX Skip auto open DM for now
					return false;
				}
				createTreeItemFromIViewClass(parentID, id, title, cla, null, null, ds,
						null, true);

				if (entry.iview == null) {
					IView view = createSideBarContentArea(id, entry);
					if (view == null) {
						return false;
					}
				}
			}
		} catch (ClassNotFoundException ce) {
			// ignore
		} catch (Throwable e) {
			Debug.out(e);
		}
		return true;
	}

	/**
	 * 
	 * TODO {@link #showEntryByID(String)} and {@link #showEntryByID(String)}
	 *      absolutely need to be combined since they do the same thing
	 *      but have different logic..
	 */
	public boolean showEntryByID(String id) {
		SideBarEntrySWT entry = getEntry(id);
		if (entry.treeItem != null) {
			itemSelected(entry.treeItem);
			return true;
		}
		if (id.equals(SIDEBAR_SECTION_ADVANCED)) {
			SideBarEntrySWT entryAdv = createEntryFromSkinRef(null,
					SIDEBAR_SECTION_ADVANCED, "main.area.advancedtab", "Advanced", null,
					null, false, -1);
			itemSelected(entryAdv.treeItem);
			return true;
		} else if (id.equals(SIDEBAR_SECTION_WELCOME)) {
			SideBarEntrySWT entryWelcome = createWelcomeSection();
			itemSelected(entryWelcome.treeItem);
			return true;
		} else if (id.equals(SIDEBAR_SECTION_PUBLISH)) {
			SideBarEntrySWT entryPublish = createEntryFromSkinRef(
					SIDEBAR_SECTION_BROWSE, SIDEBAR_SECTION_PUBLISH, "publishtab.area",
					"Publish", null, null, true, -1);
			itemSelected(entryPublish.treeItem);
			return true;
		} else if (id.startsWith("ContentNetwork.")) {
			long networkID = Long.parseLong(id.substring(15));
			handleContentNetworkSwitch(id, networkID);
			return true;
		}
		return false;
	}

	/**
	 * @param tabID
	 *
	 * @since 3.1.0.1
	 * 
	 * TODO {@link #showEntryByID(String)} and {@link #showEntryByID(String)}
	 *      absolutely need to be combined since they do the same thing
	 *      but have different logic..
	 */
	public String showEntryByTabID(String tabID) {
		if (tabID == null) {
			return null;
		}

		String id;
		SideBarEntrySWT entry = getEntry(tabID);
		if (entry.isInTree()) {
			id = tabID;
		} else if (tabID.equals("library") || tabID.equals("minilibrary")) {
			id = SIDEBAR_SECTION_LIBRARY;
		} else if (tabID.equals("publish")) {
			id = SIDEBAR_SECTION_PUBLISH;
		} else if (tabID.equals("activities")) {
			id = SIDEBAR_SECTION_ACTIVITIES;
		} else if (tabID.startsWith("ContentNetwork.")) {
			id = tabID;
		} else if (tabID.equals(SIDEBAR_SECTION_WELCOME)) {
			id = tabID;
		} else {
			// everything else can go to browse..
			id = SIDEBAR_SECTION_BROWSE;
		}

		final String itemID = id;

		entry = getEntry(itemID);
		if (entry.isInTree()) {
			itemSelected(entry.treeItem);
			return id;
		}

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (tree.isDisposed()) {
					return;
				}
				showEntryByID(itemID);
			}
		});
		return id;
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

	public void createContentNetworkSideBarEntry(ContentNetwork cn) {
		String entryID = ContentNetworkUtils.getTarget(cn);

		if (entryExists(entryID)) {
			return;
		}

		String name = cn.getName();
		SideBarEntrySWT entryBrowse = getEntry(SIDEBAR_SECTION_BROWSE);
		int position = entryBrowse == null ? 3
				: tree.indexOf(entryBrowse.getTreeItem()) + 1;

		Object prop = cn.getProperty(ContentNetwork.PROPERTY_REMOVEABLE);
		boolean closeable = (prop instanceof Boolean)
				? ((Boolean) prop).booleanValue() : false;
		final SideBarEntrySWT entry = createEntryFromSkinRef(null, entryID,
				"main.area.browsetab", name, null, cn, closeable, position);

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

	/**
	 * @param browse
	 * @return 
	 *
	 * @since 3.1.1.1
	 */
	public SideBarEntrySWT getEntryBySkinView(SkinView skinView) {
		SWTSkinObject so = skinView.getMainSkinObject();
		Object[] sideBarEntries = mapIdToEntries.values().toArray();
		for (int i = 0; i < sideBarEntries.length; i++) {
			SideBarEntrySWT entry = (SideBarEntrySWT) sideBarEntries[i];
			SWTSkinObject entrySO = entry.getSkinObject();
			SWTSkinObject entrySOParent = entrySO == null ? entrySO
					: entrySO.getParent();
			if (entrySO == so || entrySO == so.getParent() || entrySOParent == so) {
				return entry;
			}
		}
		return null;
	}

	public SideBarEntry[] getEntries() {
		return (SideBarEntry[]) mapIdToEntries.values().toArray(new SideBarEntry[0]);
	}

	public int
	getIndexOfEntryRelativeToParent(
		SideBarEntrySWT	entry )
	{
		TreeItem[] items = tree.getItems();
		
		for (int i=0;i<items.length;i++ ){
			
			if ( items[i] == entry.getTreeItem()){
				
				return( i );
			}
		}
		
		return( -1 );
	}
	
	protected void linkTitleInfoToEntry(ViewTitleInfo ti, SideBarEntry entry) {
		mapTitleInfoToEntry.put(ti, entry);
	}

	/**
	 * @param id
	 *
	 * @since 3.1.1.1
	 */
	public void closeEntry(final String id) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				SideBarEntrySWT entry = getEntry(id);
				if (entry != null && entry.treeItem != null
						&& !entry.treeItem.isDisposed()) {
					entry.treeItem.dispose();
				}
			}
		});
	}
}
