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

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.common.util.MenuItemManager;
import org.gudy.azureus2.ui.swt.MenuBuildUtils;
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
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
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
import com.aelitis.azureus.ui.selectedcontent.ISelectedContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentV3;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.toolbar.ToolBarItem;
import com.aelitis.azureus.ui.swt.utils.ColorCache;
import com.aelitis.azureus.ui.swt.utils.ImageLoader;
import com.aelitis.azureus.ui.swt.utils.ImageLoaderFactory;
import com.aelitis.azureus.ui.swt.views.skin.*;
import com.aelitis.azureus.util.MapUtils;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIPluginView;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.menus.MenuManager;
import org.gudy.azureus2.plugins.ui.sidebar.SideBarEntry;
import org.gudy.azureus2.plugins.ui.sidebar.SideBarVitalityImage;

import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;

/**
 * @author TuxPaper
 * @created Jun 23, 2008
 *
 */
public class SideBar
	extends SkinView
	implements UIUpdatable, ViewTitleInfoListener
{
	private static final int SIDEBAR_SPACING = 2;

	public static final String SIDEBAR_SECTION_PLUGINS = "Plugins";

	public static final String SIDEBAR_SECTION_LIBRARY = "Library";

	public static final String SIDEBAR_SECTION_LIBRARY_DL = "LibraryDL";

	public static final String SIDEBAR_SECTION_LIBRARY_CD = "LibraryCD";

	public static final String SIDEBAR_SECTION_LIBRARY_UNOPENED = "LibraryUnopened";

	public static final String SIDEBAR_SECTION_TOOLS = "Tools";

	public static final String SIDEBAR_SECTION_BROWSE = "Browse";

	public static final String SIDEBAR_SECTION_WELCOME = "Welcome";

	public static final String SIDEBAR_SECTION_PUBLISH = "Publish";

	public static final String SIDEBAR_SECTION_SUBSCRIPTIONS = "Subscriptions";

	public static final String SIDEBAR_SECTION_ADVANCED = "Advanced";

	public static final boolean SHOW_ALL_PLUGINS = false;

	public static final boolean SHOW_TOOLS = false;

	public static final String SIDEBAR_SECTION_ACTIVITIES = "Activity";

	private static final int IMAGELEFT_SIZE = 20;

	private static final int IMAGELEFT_GAP = 5;

	private static final boolean ALWAYS_IMAGE_GAP = false;

	private SWTSkin skin;

	private SWTSkinObject soSideBarContents;

	private SWTSkinObject soSideBarList;

	private Tree tree;

	private Font fontHeader;

	private SideBarEntrySWT currentSideBarEntry;

	private static Map mapTitleInfoToEntry = new LightHashMap();

	private static Map mapIdToSideBarInfo = new LightHashMap();

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

	private Shell shellFade;

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
					SideBarEntrySWT sideBarInfo = getSideBarInfo(id);
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
		soSideBarPopout = skin.getSkinObject("sidebar-pop");

		imageLoader = skin.getImageLoader(skinObject.getProperties());
		imgClose = imageLoader.getImage("image.sidebar.closeitem");
		imgCloseSelected = imageLoader.getImage("image.sidebar.closeitem-selected");

		addTestMenus();

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

					if (soSideBarPopout != null) {
						Object ld = soSideBarPopout.getControl().getLayoutData();
						if (ld instanceof FormData) {
							FormData fd = (FormData) ld;
							fd.width = 0;
						}

						Utils.relayout(soSideBarPopout.getControl());
					}
				} else {
					// invisible
					lastPercent = soSash.getPercent();
					soSash.setPercent(1);

					if (soSideBarPopout != null) {
						Object ld = soSideBarPopout.getControl().getLayoutData();
						if (ld instanceof FormData) {
							FormData fd = (FormData) ld;
							fd.width = 22;
						}
						soSideBarPopout.getControl().moveAbove(null);
						Utils.relayout(soSideBarPopout.getControl());
					}
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
		return null;
	}

	private void createSideBar() {
		Composite parent = (Composite) soSideBarList.getControl();

		// there isn't a SWT.NO_SCROLL in pre 3.4
		final int NO_SCROLL = 1 << 4;
		tree = new Tree(parent, SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.DOUBLE_BUFFERED
				| NO_SCROLL);
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
		fontHeader = new Font(tree.getDisplay(), fontData);

		Listener treeListener = new Listener() {
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
						int padding = 12;
						//String id = (String) treeItem.getData("Plugin.viewID");
						//SideBarEntrySWT sideBarInfo = getSideBarInfo(id);
						//if (sideBarInfo.imageLeft != null) {
							//padding += 4;
						//}

						event.height = Math.max(event.height, size.y + padding);
						
						break;
					}
					case SWT.PaintItem: {
						//paintSideBar(event);
						break;
					}
					
					case SWT.Paint: {
						//System.out.println("Paint: " + event.getBounds());
						Rectangle bounds = event.getBounds();
						if (tree.getItemCount() == 0) {
							return;
						}
						int indent = tree.getItem(0).getBounds().x;
						int y = event.y;
						treeItem = tree.getItem(new Point(indent, y));
						
						while (treeItem != null) {
							String id = (String) treeItem.getData("Plugin.viewID");
							SideBarEntrySWT sideBarInfo = getSideBarInfo(id);
							Rectangle itemBounds = sideBarInfo.getBounds();

							event.item = treeItem;
							
							boolean selected = tree.getSelectionCount() == 1
									&& tree.getSelection()[0].equals(treeItem);
							event.detail = selected ? SWT.SELECTED : SWT.NONE;

							Rectangle newClip = bounds.intersection(itemBounds);
							//System.out.println("Paint " + id + " @ " + newClip);
							event.setBounds(newClip);
							event.gc.setClipping(newClip);
							
							paintSideBar(event, sideBarInfo);

							y = itemBounds.y + itemBounds.height + 1;
							if (y > bounds.y + bounds.height) {
								break;
							}
							treeItem = tree.getItem(new Point(indent, y));
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

					case SWT.MouseUp: {
						if (tree.getItemCount() == 0 || event.button != 1) {
							return;
						}
						int indent = tree.getItem(0).getBounds().x;
						treeItem = tree.getItem(new Point(indent, event.y));
						if (treeItem == null) {
							return;
						}
						String id = (String) treeItem.getData("Plugin.viewID");
						SideBarEntrySWT sideBarInfo = getSideBarInfo(id);

						Rectangle closeArea = (Rectangle) treeItem.getData("closeArea");
						if (closeArea != null && closeArea.contains(event.x, event.y)) {
							if (sideBarInfo.iview != null) {
								sideBarInfo.iview.delete();
							}
							if (sideBarInfo.skinObject != null) {
								sideBarInfo.skinObject.getSkin().removeSkinObject(
										sideBarInfo.skinObject);
							}
							COConfigurationManager.removeParameter("SideBar.AutoOpen." + id);
							treeItem.dispose();
						} else {
							itemSelected(sideBarInfo.treeItem);
						}
						
						SideBarVitalityImage[] vitalityImages = sideBarInfo.getVitalityImages();
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
						saveCloseables();

						break;
					}

					case SWT.Collapse: {
						String id = (String) treeItem.getData("Plugin.viewID");
						SideBarEntrySWT sideBarInfo = getSideBarInfo(id);
						
						if (sideBarInfo.disableCollapse) {
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

		// For close icon
		tree.addListener(SWT.MouseUp, treeListener);

		// to disable collapsing
		tree.addListener(SWT.Collapse, treeListener);
		
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

				fillMenu(menuTree);
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
						showItemByID(id);
					}

					public void widgetDefaultSelected(SelectionEvent e) {
					}
				};

				SWTSkinButtonUtility btnDropDown = new SWTSkinButtonUtility(soDropDown);
				btnDropDown.addSelectionListener(new ButtonListenerAdapter() {
					public void pressed(SWTSkinButtonUtility buttonUtility, SWTSkinObject skinObject) {
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
					public void pressed(SWTSkinButtonUtility buttonUtility, SWTSkinObject skinObject) {
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
			SideBarEntrySWT sideBarInfo = getSideBarInfo(id);
			ViewTitleInfo titleInfo = sideBarInfo.getTitleInfo();
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
	protected void fillMenu(Menu menuTree) {
		org.gudy.azureus2.plugins.ui.menus.MenuItem[] menu_items;

		menu_items = MenuItemManager.getInstance().getAllAsArray("sidebar");

		MenuBuildUtils.addPluginMenuItems((Composite) soMain.getControl(),
				menu_items, menuTree, false, true,
				new MenuBuildUtils.MenuItemPluginMenuControllerImpl(new Object[] {
					currentSideBarEntry
				}));

		if (currentSideBarEntry != null) {
			menu_items = MenuItemManager.getInstance().getAllAsArray(
					"sidebar." + currentSideBarEntry.id);

			MenuBuildUtils.addPluginMenuItems((Composite) soMain.getControl(),
					menu_items, menuTree, false, true,
					new MenuBuildUtils.MenuItemPluginMenuControllerImpl(new Object[] {
						currentSideBarEntry
					}));
		}
	}

	/**
	 * @param event
	 * @param sideBarInfo 
	 *
	 * @since 3.1.0.1
	 */
	protected void paintSideBar(Event event, SideBarEntrySWT sideBarInfo) {
		TreeItem treeItem = (TreeItem) event.item;
		String text = (String) treeItem.getData("text");
		if (text == null)
			text = "";

		//Point size = event.gc.textExtent(text);
		//Rectangle treeBounds = tree.getBounds();
		Rectangle itemBounds = treeItem.getBounds();

		GC gc = event.gc;

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
			if (tree.isFocusControl()) {
  			if (Constants.isOSX) {
  				gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_LIST_SELECTION));
  				gc.setBackground(Colors.faded[Colors.BLUES_DARKEST]);
  			} else {
  				gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_LIST_SELECTION));
  				gc.setForeground(Colors.faded[Colors.BLUES_DARKEST]);
  			}
			} else {
				if (Constants.isOSX) {
  				gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_LIST_SELECTION));
  				gc.setBackground(Colors.faded[Colors.BLUES_MIDLIGHT]);
				} else {
  				gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_LIST_SELECTION));
  				//gc.setForeground(Colors.faded[Colors.BLUES_MIDLIGHT]);
  				//gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
  				
  				gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
  				//gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
  				//gc.setForeground(Colors.faded[Colors.BLUES_MIDLIGHT]);
  				//fgText = fg;
				}
			}

			gc.fillGradientRectangle(event.x, itemBounds.y, event.width,
					itemBounds.height, true);
		} else {
			if (fg != null) {
				fgText = fg;
			}
			if (bg != null) {
				gc.setBackground(bg);
			}

			gc.fillRectangle(event.getBounds());
		}

		Rectangle treeArea = tree.getClientArea();

		gc.setFont(tree.getFont());

		if (sideBarInfo == null) {
			String id = (String) treeItem.getData("Plugin.viewID");
			sideBarInfo = getSideBarInfo(id);
		}
		int x1IndicatorOfs = SIDEBAR_SPACING;
		int x0IndicatorOfs = itemBounds.x;

		//System.out.println(System.currentTimeMillis() + "] refhres " + sideBarInfo.getId());
		if (sideBarInfo.titleInfo != null) {
			String textIndicator = (String) sideBarInfo.titleInfo.getTitleInfoProperty(ViewTitleInfo.TITLE_INDICATOR_TEXT);
			if (textIndicator != null) {
				gc.setAntialias(SWT.ON);

				Point textSize = gc.textExtent(textIndicator);
				Point minTextSize = gc.textExtent("99");
				int textOffsetX = 0;
				if (textSize.x < minTextSize.x) {
					textOffsetX = (minTextSize.x - textSize.x) / 2;
					textSize.x = minTextSize.x;
				}

				int width = textSize.x + textSize.y / 2 + 2;
				x1IndicatorOfs += width + SIDEBAR_SPACING;
				int startX = treeArea.width - x1IndicatorOfs;

				textOffsetX += textSize.y / 4 + 1;

				int textOffsetY = 0;

				int height = textSize.y + 3;
				int startY = itemBounds.y + (itemBounds.height - height) / 2;

				//gc.fillRectangle(startX, startY, width, height);

				Boolean b_vitality = (Boolean) sideBarInfo.titleInfo.getTitleInfoProperty(ViewTitleInfo.TITLE_HAS_VITALITY);

				boolean vitality = b_vitality != null && b_vitality.booleanValue();

				if (!selected) {
  				gc.setForeground(Colors.blues[Colors.BLUES_LIGHTEST]);
  				gc.setBackground(vitality ? Colors.fadedRed
  						: Colors.faded[Colors.BLUES_DARKEST]);
				} else {
  				gc.setBackground(ColorCache.getColor(gc.getDevice(), 210, 210, 230));
  				gc.setForeground(ColorCache.getColor(gc.getDevice(), 44, 44, 128));
  				//gc.setForeground(Colors.blue);
				}
				gc.fillRoundRectangle(startX, startY, width, height, textSize.y + 1,
						height);
				if (maxIndicatorWidth > width) {
					maxIndicatorWidth = width;
				}
				GCStringPrinter.printString(gc, textIndicator, new Rectangle(startX,
						startY + textOffsetY, width, height), true, false, SWT.CENTER);
			}
			}

		if (sideBarInfo.closeable) {
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
		
		SideBarVitalityImage[] vitalityImages = sideBarInfo.getVitalityImages();
		for (int i = 0; i < vitalityImages.length; i++) {
			SideBarVitalityImageSWT vitalityImage = (SideBarVitalityImageSWT) vitalityImages[i];
			if (!vitalityImage.isVisible()) {
				continue;
			}
			vitalityImage.switchSuffix(selected ? "-selected" : "");
			Image image = vitalityImage.getImage();
			if (image != null) {
  			Rectangle bounds = image.getBounds();
  			bounds.x = treeArea.width - bounds.width - SIDEBAR_SPACING
  					- x1IndicatorOfs;
  			bounds.y = itemBounds.y + (itemBounds.height - bounds.height) / 2;
  			x1IndicatorOfs += bounds.width + SIDEBAR_SPACING;
  			
  			gc.drawImage(image, bounds.x, bounds.y);
  			vitalityImage.setHitArea(bounds);
			}
		}

		Image imageLeft = sideBarInfo.getImageLeft(selected ? "-selected" : null);
		if (imageLeft != null) {
			Rectangle bounds = imageLeft.getBounds();
			int x = x0IndicatorOfs + ((IMAGELEFT_SIZE - bounds.width) / 2);
			int y = itemBounds.y + ((itemBounds.height - bounds.height) / 2);
			gc.drawImage(imageLeft, x, y); 
//			0, 0, bounds.width, bounds.height,
//					x0IndicatorOfs, itemBounds.y
//							+ ((itemBounds.height - IMAGELEFT_SIZE) / 2), IMAGELEFT_SIZE,
//					IMAGELEFT_SIZE);

			x0IndicatorOfs += IMAGELEFT_SIZE + IMAGELEFT_GAP;
			
			gc.setFont(fontHeader);
		} else if (ALWAYS_IMAGE_GAP) {
			x0IndicatorOfs += IMAGELEFT_SIZE + IMAGELEFT_GAP;
		} else {
			if (treeItem.getParentItem() != null) {
				x0IndicatorOfs += 30 - 18;
			}
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
  
  		GCStringPrinter.printString(gc, text, clipping, true, false, SWT.NONE);
  		//gc.setClipping((Rectangle) null);
		}

		// OSX overrides the twisty, and we can't use the default twisty
		// on Windows because it doesn't have transparency and looks ugly
		if (!Constants.isOSX && treeItem.getItemCount() > 0) {
			gc.setAntialias(SWT.ON);
			Color oldBG = gc.getBackground();
			gc.setBackground(gc.getForeground());
			if (treeItem.getExpanded()) {
				int xStart = 15;
				int arrowSize = 8;
				int yStart = itemBounds.height - (itemBounds.height + arrowSize) / 2;
				gc.fillPolygon(new int[] {
					event.x - xStart,
					event.y + yStart,
					event.x - xStart + arrowSize,
					event.y + yStart,
					event.x - xStart + (arrowSize / 2),
					event.y + 16,
				});
			} else {
				int xStart = 15;
				int arrowSize = 8;
				int yStart = itemBounds.height - (itemBounds.height + arrowSize) / 2;
				gc.fillPolygon(new int[] {
					event.x - xStart,
					event.y + yStart,
					event.x - xStart + arrowSize,
					event.y + yStart + 4,
					event.x - xStart,
					event.y + yStart + 8,
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
	private void setupDefaultItems() {
		TreeItem treeItem;

		File file = new File(SystemProperties.getUserPath(), "sidebarauto.config");
		if (!file.exists()) {
			createWelcomeSection();
		}

		// Put TitleInfo in another class
		final ViewTitleInfo titleInfoActivityView = new ViewTitleInfo() {
			public Object getTitleInfoProperty(int propertyID) {
				if (propertyID == TITLE_INDICATOR_TEXT) {
					return "" + VuzeActivitiesManager.getNumEntries();
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
		ImageLoader imageLoader = ImageLoaderFactory.getInstance();

		entry = createEntryFromSkinRef(null, SIDEBAR_SECTION_LIBRARY, "library",
				MessageText.getString("sidebar." + SIDEBAR_SECTION_LIBRARY), null,
				null, false, -1);
		entry.setImageLeftID("image.sidebar.library");
		entry.disableCollapse = true;

		createEntryFromSkinRef(SIDEBAR_SECTION_LIBRARY, SIDEBAR_SECTION_LIBRARY_DL,
				"library", MessageText.getString("sidebar.LibraryDL"), null, null,
				false, -1);

		createEntryFromSkinRef(SIDEBAR_SECTION_LIBRARY, SIDEBAR_SECTION_LIBRARY_CD,
				"library", MessageText.getString("sidebar.LibraryCD"), null, null,
				false, -1);

		createEntryFromSkinRef(SIDEBAR_SECTION_LIBRARY, SIDEBAR_SECTION_LIBRARY_UNOPENED,
				"library", MessageText.getString("sidebar.LibraryUnopened"), null, null,
				false, -1);

		entry = createEntryFromSkinRef(null, SIDEBAR_SECTION_BROWSE, "main.area.browsetab",
				"Vuze Network", null, null, false, -1);
		entry.setImageLeftID("image.sidebar.vuze");


		createEntryFromSkinRef(null, SIDEBAR_SECTION_ACTIVITIES,
				"main.area.events", "Activity", titleInfoActivityView, null, false, -1);

		entry = createEntryFromSkinRef(null, SIDEBAR_SECTION_SUBSCRIPTIONS,
				"main.area.subscriptions", "Subscriptions", null, null, false, -1);
		entry.setImageLeftID("image.sidebar.subscriptions");


		//new TreeItem(tree, SWT.NONE).setText("Search");

		if (SHOW_TOOLS) {
			createEntryFromSkinRef(null, SIDEBAR_SECTION_TOOLS, "main.area.hood",
					"Under The Hood", null, null, false, -1);

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
				SideBarEntrySWT sideBarInfo = getSideBarInfo(viewInfo.viewID);
				sideBarInfo.treeItem = treeItem;
				sideBarInfo.iview = viewInfo.view;
				sideBarInfo.eventListener = viewInfo.event_listener;
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
				SideBarEntrySWT sideBarInfo = getSideBarInfo(viewInfo.viewID);
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

		SBC_LibraryView.setupViewTitle();

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

		loadCloseables();

		if (System.getProperty("v3.sidebar.advanced", "0").equals("1")) {
			createEntryFromSkinRef(null, SIDEBAR_SECTION_ADVANCED,
					"main.area.advancedtab", "Advanced", null, null, false, -1);
		}

		Composite parent = tree.getParent();

		treeItem = tree.getItem(0);
		tree.select(treeItem);
		tree.showItem(treeItem);
		itemSelected(treeItem);

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
	private SideBarEntrySWT createWelcomeSection() {
		SideBarEntrySWT entry = createEntryFromSkinRef(null, SIDEBAR_SECTION_WELCOME,
				"main.area.welcome", "Getting Started", null, null, true, 0);
		entry.setImageLeftID("image.sidebar.welcome");
		return entry;
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

		SideBarEntrySWT sideBarInfo = getSideBarInfo(id);

		if (sideBarInfo.treeItem != null) {
			treeItem = sideBarInfo.treeItem;
		} else {
			SideBarEntrySWT sideBarInfoParent = getSideBarInfo(parentID);
			TreeItem parentTreeItem = sideBarInfoParent.treeItem;
			treeItem = createTreeItem(parentTreeItem, id, datasource, null,
					iview.getFullTitle(), closeable, -1);

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

		SideBarEntrySWT sideBarInfo = getSideBarInfo(id);
		if (sideBarInfo.treeItem != null) {
			return sideBarInfo.treeItem;
		}

		SideBarEntrySWT sideBarInfoParent = getSideBarInfo(parent);
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
			Object datasource, boolean closeable) {
		final SideBarEntrySWT sideBarInfo = getSideBarInfo(id);

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
			treeItem.setData("text", title);
		} else {
			if (sideBarInfo.iview != null) {
				treeItem.setData("text", sideBarInfo.iview.getFullTitle());
			}
		}

		if (titleInfo == null) {
			if (sideBarInfo.iview instanceof ViewTitleInfo) {
				titleInfo = (ViewTitleInfo) sideBarInfo.iview;
			} else if (sideBarInfo.iview instanceof UISWTViewImpl) {
				UISWTViewEventListener eventListener = ((UISWTViewImpl) sideBarInfo.iview).getEventListener();
				if (eventListener instanceof ViewTitleInfo) {
					titleInfo = (ViewTitleInfo) eventListener;
				}
			}

		}

		if (titleInfo != null) {
			sideBarInfo.titleInfo = titleInfo;
		}

		if (sideBarInfo.titleInfo != null) {
			mapTitleInfoToEntry.put(sideBarInfo.titleInfo, sideBarInfo);
			String newText = (String) sideBarInfo.titleInfo.getTitleInfoProperty(ViewTitleInfo.TITLE_TEXT);
			if (newText != null) {
				pull = false;
				treeItem.setData("text", newText);
			}

			String imageID = (String) sideBarInfo.titleInfo.getTitleInfoProperty(ViewTitleInfo.TITLE_IMAGEID);
			if (imageID != null) {
				sideBarInfo.setImageLeftID(imageID.length() == 0 ? null : imageID);
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
			initializeView.setVisible(false);
			Composite composite = iview.getComposite();
			if (composite != null && !composite.isDisposed()) {
				composite.setVisible(false);
			}
			if (sideBarInfo.datasource != null) {
				iview.dataSourceChanged(sideBarInfo.datasource);
			}
			initializeView.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					if (sideBarInfo.iview != null) {
						sideBarInfo.iview.delete();
						sideBarInfo.iview = null;
					}
				}
			});
		}
	}

	public static SideBarEntrySWT getSideBarInfo(String id) {
		SideBarEntrySWT sidebarInfo = (SideBarEntrySWT) mapIdToSideBarInfo.get(id);
		if (sidebarInfo == null) {
			sidebarInfo = new SideBarEntrySWT(id);
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
		SideBarEntrySWT sideBarInfo = getSideBarInfo(id);
		if (sideBarInfo.treeItem != null) {
			itemSelected(sideBarInfo.treeItem);
			return true;
		}
		if (id.equals(SIDEBAR_SECTION_ADVANCED)) {
			SideBarEntrySWT entry = createEntryFromSkinRef(null,
					SIDEBAR_SECTION_ADVANCED, "main.area.advancedtab", "Advanced", null,
					null, false, -1);
			itemSelected(entry.treeItem);
			return true;
		} else if (id.equals(SIDEBAR_SECTION_WELCOME)) {
			SideBarEntrySWT entry = createWelcomeSection();
			itemSelected(entry.treeItem);
			return true;
		} else if (id.equals(SIDEBAR_SECTION_PUBLISH)) {
			SideBarEntrySWT entry = createEntryFromSkinRef(SIDEBAR_SECTION_BROWSE,
					SIDEBAR_SECTION_PUBLISH, "publishtab.area", "Publish", null, null,
					true, -1);
			itemSelected(entry.treeItem);
			return true;
		}
		return false;
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

	private void _itemSelected(TreeItem treeItem) {
		TreeItem[] selection = tree.getSelection();
		if (selection == null || selection.length == 0 || selection[0] != treeItem) {
			tree.showItem(treeItem);
			tree.select(treeItem);
		}
		final String id = (String) treeItem.getData("Plugin.viewID");
		SideBarEntrySWT newSideBarInfo = getSideBarInfo(id);

		// We'll have an iview if we've previously created one
		IView newIView = newSideBarInfo.iview;

		// Otherwise, we have two ways of creating an IView.. via IViewClass,
		// or UISWTViewEventListener

		if (newIView == null) {
			if (newSideBarInfo.iviewClass != null) {
				newIView = createSideBarContentArea(id, newSideBarInfo);

				if (newIView == null) {
					return;
				}

				setupTreeItem(newIView, treeItem,
						(String) treeItem.getData("Plugin.viewID"), null,
						newIView.getFullTitle(), null, null, newSideBarInfo.closeable);
			}
		}

		if (newIView == null && newSideBarInfo.eventListener != null) {
			newIView = createTreeItemFromEventListener(null, treeItem,
					newSideBarInfo.eventListener, id, newSideBarInfo.closeable, null);
		}

		if (newIView != null) {
			SideBarEntrySWT oldSideBarInfo = currentSideBarEntry;

			// hide old
			if (oldSideBarInfo != null && oldSideBarInfo != newSideBarInfo) {
				if (lastImage != null && !lastImage.isDisposed()) {
					lastImage.dispose();
					lastImage = null;
				}
				if (oldSideBarInfo.skinObject != null) {
					SWTSkinObjectContainer container = (SWTSkinObjectContainer) oldSideBarInfo.skinObject;
					if (container != null) {
						Control oldComposite = container.getControl();
						doFade(oldComposite);
						
						container.setVisible(false);
						if (!oldComposite.isDisposed()) {
							oldComposite.getShell().update();
						}
					}
				}
				if (oldSideBarInfo.iview != null) {
					Composite oldComposite = oldSideBarInfo.iview.getComposite();
					if (oldComposite != null && !oldComposite.isDisposed()) {
						doFade(oldComposite);

						oldComposite.setVisible(false);
						oldComposite.getShell().update();
					}
				}
			}
				
			SelectedContentManager.changeCurrentlySelectedContent(null, null);

			// show new
			currentSideBarEntry = newSideBarInfo;

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
						SelectedContentManager.changeCurrentlySelectedContent(
								currentSideBarEntry.id, new ISelectedContent[] {
									new SelectedContentV3(dm)
								});
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

			SWTSkinObjectContainer container = (SWTSkinObjectContainer) newSideBarInfo.skinObject;
			if (container != null) {
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

			triggerListener(newSideBarInfo, oldSideBarInfo);
		}
	}

	/**
	 * @param oldComposite
	 *
	 * @since 3.1.1.1
	 */
	private void doFade(final Control oldComposite) {
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
					if (lastImage == null
							|| lastImage.isDisposed()
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
		SideBarEntrySWT sideBarInfo = getSideBarInfo(id);

		if (sideBarInfo.treeItem != null && sideBarInfo.iview != null) {
			if (sideBarInfo.eventListener == l) {
				System.err.println("Already created view " + id);
			}
			id = id + "_" + SystemTime.getCurrentTime();
			sideBarInfo = getSideBarInfo(id);
		}

		String name = sideBarInfo.treeItem == null ? id
				: (String) sideBarInfo.treeItem.getData("text");

		if (treeItem == null) {
			SideBarEntrySWT sideBarInfoParent = getSideBarInfo(parentID);
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

			sideBarInfo.eventListener = l;
			sideBarInfo.parentID = parentID;
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

			SWTSkinObjectContainer soContents = new SWTSkinObjectContainer(skin,
					skin.getSkinProperties(), viewComposite, "Contents"
							+ (mapIdToSideBarInfo.size() + 1), "", "container",
					soSideBarContents);

			sideBarInfo.skinObject = soContents;
			sideBarInfo.eventListener = l;
			sideBarInfo.parentID = parentID;

			setupTreeItem(iview, treeItem, id, null, iview.getFullTitle(),
					viewComposite, datasource, closeable);

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

			SideBarEntrySWT sideBarInfo = getSideBarInfo((String) item.getData("Plugin.viewID"));
			sideBarInfo.skinObject = soContents;

			setupTreeItem(view, item, id, null, null, viewComposite, datasource,
					closeable);

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

	public SideBarEntrySWT createEntryFromSkinRef(String parentID,
			final String id, final String configID, String title,
			ViewTitleInfo titleInfo, final Object params, boolean closeable, int index) {

		// temp until we start passing ds
		Object datasource = null;

		SideBarEntrySWT sideBarInfo = getSideBarInfo(id);
		if (sideBarInfo.treeItem != null) {
			return sideBarInfo;
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
		sideBarInfo.eventListener = l;

		SideBarEntrySWT sideBarInfoParent = getSideBarInfo(parentID);
		TreeItem parentTreeItem = sideBarInfoParent.treeItem;

		sideBarInfo.parentID = parentID;

		treeItem = createTreeItem(parentTreeItem == null ? (Object) tree
				: (Object) parentTreeItem, id, datasource, titleInfo, title, closeable,
				index);

		setupTreeItem(null, treeItem, id, titleInfo, title, null, datasource,
				closeable);

		return sideBarInfo;
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
		if (currentSideBarEntry == null || currentSideBarEntry.iview == null) {
			return;
		}
		currentSideBarEntry.iview.refresh();

		SideBarEntrySWT sidebarInfo = getSideBarInfo(currentSideBarEntry.id);
		if (sidebarInfo.pullTitleFromIView && sidebarInfo.treeItem != null) {
			sidebarInfo.treeItem.setData("text",
					currentSideBarEntry.iview.getFullTitle());
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
				if (tree.isDisposed()) {
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
								String treeItemID = (String) searchTreeItem.getData("Plugin.viewID");
								if (treeItemID != null && treeItemID.equals(id)) {
									sideBarEntry = getSideBarInfo(treeItemID);
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

				if (sideBarEntry.treeItem.isDisposed()) {
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
			}
		});
	}

	public SideBarEntrySWT getCurrentSideBarInfo() {
		return currentSideBarEntry;
	}

	public String getLogID(SideBarEntrySWT info) {
		if (info == null) {
			return "none";
		}
		String id = null;
		if (info.titleInfo != null) {
			id = (String) info.titleInfo.getTitleInfoProperty(ViewTitleInfo.TITLE_LOGID);
		}

		if (id == null) {
			id = info.id;
			int i = id.indexOf('_');
			if (i > 0) {
				id = id.substring(0, i);
			}
		}

		return id;
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

	private void triggerListener(SideBarEntrySWT newSideBarEntry,
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
		return getSideBarInfo(id).iview;
	}

	public void saveCloseables() {
		// update title
		for (Iterator iter = mapAutoOpen.keySet().iterator(); iter.hasNext();) {
			String id = (String) iter.next();
			Object o = mapAutoOpen.get(id);

			if (o instanceof Map) {

				SideBarEntrySWT sideBarInfo = getSideBarInfo(id);
				Map autoOpenInfo = (Map) o;

				if (sideBarInfo.treeItem != null) {
					String s = (String) sideBarInfo.treeItem.getData("text");
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
			SideBarEntrySWT sideBarInfo = getSideBarInfo(id);
			if (sideBarInfo.treeItem != null) {
				return;
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

	/**
	 * @param tabID
	 *
	 * @since 3.1.0.1
	 */
	public void showItemByTabID(String tabID) {
		if (tabID == null) {
			return;
		}
		// if it matches an existing sidebar item, just use that
		IView viewFromID = getIViewFromID(tabID);
		if (viewFromID != null) {
			showItemByID(tabID);
		}

		if (tabID.equals("library") || tabID.equals("minilibrary")) {
			showItemByID(SIDEBAR_SECTION_LIBRARY);
		} else if (tabID.equals("publish")) {
			showItemByID(SIDEBAR_SECTION_PUBLISH);
		} else if (tabID.equals("activities")) {
			showItemByID(SIDEBAR_SECTION_ACTIVITIES);
		} else {
			// everything else can go to browse..
			showItemByID(SIDEBAR_SECTION_BROWSE);
		}
	}

	/**
	 * @param browse
	 * @return 
	 *
	 * @since 3.1.1.1
	 */
	public SideBarEntrySWT getSideBarEntry(SkinView skinView) {
		SWTSkinObject so = skinView.getMainSkinObject();
		Object[] sideBarEntries = mapIdToSideBarInfo.values().toArray();
		for (int i = 0; i < sideBarEntries.length; i++) {
			SideBarEntrySWT entry = (SideBarEntrySWT) sideBarEntries[i];
			SWTSkinObject entrySO = entry.getSkinObject();
			SWTSkinObject entrySOParent = entrySO == null ? entrySO : entrySO.getParent();
			if (entrySO == so || entrySO == so.getParent() || entrySOParent == so) {
				return entry;
			}
		}
		return null;
	}
}
