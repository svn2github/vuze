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
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.PluginsMenuHelper;
import org.gudy.azureus2.ui.swt.mainwindow.PluginsMenuHelper.IViewInfo;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewImpl;
import org.gudy.azureus2.ui.swt.views.*;
import org.gudy.azureus2.ui.swt.views.stats.StatsView;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnCreator;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.activities.VuzeActivitiesListener;
import com.aelitis.azureus.activities.VuzeActivitiesManager;
import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.swt.ViewIndicator.*;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.utils.ColorCache;
import com.aelitis.azureus.ui.swt.utils.UIUpdatable;
import com.aelitis.azureus.ui.swt.utils.UIUpdaterFactory;

import org.gudy.azureus2.plugins.ui.UIPluginView;
import org.gudy.azureus2.plugins.ui.tables.TableManager;

/**
 * @author TuxPaper
 * @created Jun 23, 2008
 *
 */
public class SideBar
	extends SkinView
	implements UIUpdatable, ViewIndicatorListener
{
	private SWTSkin skin;

	private SWTSkinObject soSideBarContents;

	private SWTSkinObject soSideBarList;

	private Tree tree;

	private IView currentIView;

	private static Map mapIViewToSkinObject = new HashMap();

	private static Map mapViewIndicatorToTreeItem = new HashMap();
	
	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#showSupport(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		skin = skinObject.getSkin();

		soSideBarContents = skin.getSkinObject("sidebar-contents");
		soSideBarList = skin.getSkinObject("sidebar-list");

		setupList();

		UIUpdaterFactory.getInstance().addUpdater(this);
		
		ViewIndicatorManager.addListener(this);

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

		Listener paintListener = new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {
					case SWT.MeasureItem: {
						TreeItem item = (TreeItem) event.item;
						String text = item.getText(event.index);
						Point size = event.gc.textExtent(text);
						event.width = size.x + event.x; // tree.getClientArea().width;
						event.x = 0;
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
						if (selected) {
							event.gc.setForeground(colorFocus);
							event.gc.drawRectangle(0, event.y, treeBounds.width - 1,
									event.height - 1);
							event.gc.setForeground(fgSel);
							event.gc.setBackground(bgSel);
							event.gc.setBackground(ColorCache.getColor(event.gc.getDevice(),
									"#607080"));
							event.gc.fillRectangle(1, event.y + 1, treeBounds.width - 2,
									event.height - 2);
						} else {
							if (fg != null) {
								event.gc.setForeground(fg);
							}
							if (bg != null) {
								event.gc.setBackground(bg);
							}
							event.gc.fillRectangle(0, event.y, treeBounds.width, event.height);
						}

						event.gc.drawText(text, event.x, event.y + 6, true);

						if (item.getItemCount() > 0) {
							if (item.getExpanded()) {
								event.gc.drawText("V", 7, event.y + 6, true);
							} else {
								event.gc.drawText(">", 7, event.y + 6, true);
							}
						}

						ViewIndicator viewIndicator = (ViewIndicator) item.getData("ViewIndicator");
						if (viewIndicator != null) {
							String textIndicator = viewIndicator.getTextIndicator();
							if (textIndicator != null) {
								Point textSize = event.gc.textExtent(textIndicator);
								Rectangle itemBounds = tree.getClientArea();
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

						break;
					}
					case SWT.EraseItem: {
						//event.detail &= ~SWT.FOREGROUND;
						event.detail &= ~(SWT.FOREGROUND | SWT.BACKGROUND);
						break;
					}
				}
			}
		};

		tree.addListener(SWT.MeasureItem, paintListener);
		if (bg != null) {
			tree.addListener(SWT.PaintItem, paintListener);
			tree.addListener(SWT.EraseItem, paintListener);
		}

		tree.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				TreeItem item = (TreeItem) event.item;
				itemSelected(item);
			}
		});

		createTreeItems();

		parent.getShell().layout(true, true);
	}

	/**
	 * 
	 *
	 * @since 3.1.1.1
	 */
	private void createTreeItems() {
		TreeItem treeItem;

		final ViewIndicator viewIndicatorActivityView = new ViewIndicator() {
			public String getTextIndicator() {
				return "" + VuzeActivitiesManager.getNumEntries();
			}

			public String getImageIDIndicator() {
				return null;
			}
		};
		VuzeActivitiesManager.addListener(new VuzeActivitiesListener() {
			public void vuzeNewsEntryChanged(VuzeActivitiesEntry entry) {
			}
		
			public void vuzeNewsEntriesRemoved(VuzeActivitiesEntry[] entries) {
				ViewIndicatorManager.refreshViewIndicator(viewIndicatorActivityView);
			}
		
			public void vuzeNewsEntriesAdded(VuzeActivitiesEntry[] entries) {
				ViewIndicatorManager.refreshViewIndicator(viewIndicatorActivityView);
			}
		});

		final TreeItem itemActivity = createSkinned_UISWTViewEventListener(skin, tree, "Activity_SB",
				"main.area.events", "Activity", viewIndicatorActivityView);

		
		TreeItem itemLibrary = createSkinned_UISWTViewEventListener(skin, tree,
				"Library_SB", "library", "Library", null);

		createTreeItem(
				itemLibrary,
				"Seeding",
				MyTorrentsView.class,
				new Class[] {
					AzureusCore.class,
					boolean.class,
					TableColumnCore[].class
				},
				new Object[] {
					AzureusCoreFactory.getSingleton(),
					new Boolean(true),
					TableColumnCreator.createCompleteDM(TableManager.TABLE_MYTORRENTS_COMPLETE)
				});

		createTreeItem(itemLibrary, "Downloading", MyTorrentsView.class,
				new Class[] {
					AzureusCore.class,
					boolean.class,
					TableColumnCore[].class
				}, new Object[] {
					AzureusCoreFactory.getSingleton(),
					new Boolean(true),
					TableColumnCreator.createIncompleteDM("AA")
				});

		createSkinned_UISWTViewEventListener(skin, tree, "Browse_SB",
				"main.area.browsetab", "On Vuze", null);

		createSkinned_UISWTViewEventListener(skin, tree, "Publish_SB",
				"main.area.publishtab", "Publish", null);

		//new TreeItem(tree, SWT.NONE).setText("Search");

		TreeItem itemTools = new TreeItem(tree, SWT.NONE);
		itemTools.setText("Under The Hood");

		createTreeItem(itemTools, "All Peers", PeerSuperView.class, null, null);
		createTreeItem(itemTools, "Stats", StatsView.class, null, null);
		createTreeItem(itemTools, "My Tracker", MyTrackerView.class, null, null);
		createTreeItem(itemTools, "My Clasic-Shares", MySharesView.class, null,
				null);
		createTreeItem(itemTools, "Logger", LoggerView.class, null, null);
		createTreeItem(itemTools, "Config", ConfigView.class, null, null);

		TreeItem itemPlugins = new TreeItem(tree, SWT.NONE);
		itemPlugins.setText("Plugins");
		IViewInfo[] pluginViewsInfo = PluginsMenuHelper.getInstance().getPluginViewsInfo();
		for (int i = 0; i < pluginViewsInfo.length; i++) {
			IViewInfo viewInfo = pluginViewsInfo[i];
			treeItem = new TreeItem(itemPlugins, SWT.NONE);
			treeItem.setText(viewInfo.name);
			treeItem.setData("UISWTViewEventListener", viewInfo.event_listener);
			treeItem.setData("Plugin.viewID", viewInfo.viewID);
		}
	}

	/**
	 * @param title
	 * @param iviewClass
	 * @param iviewClassArgs
	 * @param iviewClassVals
	 * @return 
	 *
	 * @since 3.1.1.1
	 */
	private TreeItem createTreeItem(TreeItem parent, String title,
			Class iviewClass, Class[] iviewClassArgs, Object[] iviewClassVals) {
		TreeItem treeItem = new TreeItem(parent, SWT.NONE);
		return createTreeItem2(treeItem, title, iviewClass, iviewClassArgs,
				iviewClassVals);
	}

	private TreeItem createTreeItem(Tree parent, String title, Class iviewClass,
			Class[] iviewClassArgs, Object[] iviewClassVals) {
		TreeItem treeItem = new TreeItem(parent, SWT.NONE);
		return createTreeItem2(treeItem, title, iviewClass, iviewClassArgs,
				iviewClassVals);
	}

	private TreeItem createTreeItem2(TreeItem treeItem, String title,
			Class iviewClass, Class[] iviewClassArgs, Object[] iviewClassVals) {
		treeItem.setText(title);
		treeItem.setData("IViewClass", iviewClass);
		treeItem.setData("IViewClassArgs", iviewClassArgs);
		treeItem.setData("IViewClassVals", iviewClassVals);

		return treeItem;
	}

	/**
	 * @param item
	 *
	 * @since 3.1.0.1
	 */
	protected void itemSelected(TreeItem item) {
		IView iview = (IView) item.getData("IView");
		if (iview == null) {
			Class iviewClass = (Class) item.getData("IViewClass");
			if (iviewClass != null) {
				Class[] cArgs = (Class[]) item.getData("IViewClassArgs");
				Object[] cArgVals = (Object[]) item.getData("IViewClassVals");

				iview = createSideBarContentArea(soSideBarContents, iviewClass, cArgs,
						cArgVals);

				item.setData("IView", iview);
			}
		}

		if (iview == null) {
			UISWTViewEventListener l = (UISWTViewEventListener) item.getData("UISWTViewEventListener");
			if (l != null) {
				UISWTViewImpl view = null;
				try {
					String sViewID = (String) item.getData("Plugin.viewID");
					view = new UISWTViewImpl("SideBar.Plugins", sViewID, l);
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

					mapIViewToSkinObject.put(view, soContents);

					iview = view;
					item.setData("IView", iview);

					view.initialize(viewComposite);
					parent.layout(true, true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		if (iview != null) {
			if (currentIView != null) {
				SWTSkinObjectContainer container = (SWTSkinObjectContainer) mapIViewToSkinObject.get(currentIView);
				if (container != null) {
					Composite composite = container.getComposite();
					if (composite != null && !composite.isDisposed()) {
						composite.setVisible(false);
					}
				}
			}
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
	 * @param iview
	 * @param iviewClass
	 * @param args
	 *
	 * @since 3.1.0.1
	 */
	public static IView createSideBarContentArea(SWTSkinObject soSideBarContents,
			Class iviewClass, Class[] cArgs, Object[] cArgVals) {
		IView iview = null;
		try {
			if (cArgs == null) {
				iview = (IView) iviewClass.newInstance();
			} else {
				Constructor constructor = iviewClass.getConstructor(cArgs);
				iview = (IView) constructor.newInstance(cArgVals);
			}

			Composite parent = (Composite) soSideBarContents.getControl();

			SWTSkin skin = soSideBarContents.getSkin();

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

			mapIViewToSkinObject.put(iview, soContents);

			iview.initialize(viewComposite);

			Composite iviewComposite = iview.getComposite();
			if (iviewComposite.getLayoutData() == null) {
				GridData gridData = new GridData(GridData.FILL_BOTH);
				iviewComposite.setLayoutData(gridData);
			}

			parent.layout(true, true);
		} catch (Exception e) {
			e.printStackTrace();
			if (iview != null) {
				iview.delete();
			}
			iview = null;
		}
		return iview;
	}

	public static TreeItem createSkinned_UISWTViewEventListener(
			final SWTSkin skin, Tree tree, final String newID, final String configID,
			String title, ViewIndicator viewIndicator) {
		TreeItem treeItem = null;
		
		UISWTViewEventListener l = new UISWTViewEventListenerFormLayout() {
			private SWTSkinObject skinObject;

			public boolean eventOccurred(UISWTViewEvent event) {
				switch (event.getType()) {
					case UISWTViewEvent.TYPE_CREATE: {
					}
						break;

					case UISWTViewEvent.TYPE_INITIALIZE: {
						Composite parent = (Composite) event.getData();
						SWTSkinObject soParent = (SWTSkinObject) parent.getData("SkinObject");
						Shell shell = parent.getShell();
						Cursor cursor = shell.getCursor();
						try {
							shell.setCursor(shell.getDisplay().getSystemCursor(
									SWT.CURSOR_WAIT));

							skinObject = skin.createSkinObject(newID, configID, soParent);
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
		if (tree != null) {
			treeItem = new TreeItem(tree, SWT.NONE);
			treeItem.setText(title);
			treeItem.setData("UISWTViewEventListener", l);
			treeItem.setData("Plugin.viewID", newID);
			treeItem.setData("ViewIndicator", viewIndicator);
			
			if (viewIndicator != null) {
				mapViewIndicatorToTreeItem.put(viewIndicator, treeItem);
			}
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

	// @see com.aelitis.azureus.ui.swt.ViewInidicator.ViewIndicatorListener#viewIndicatorRefresh(com.aelitis.azureus.ui.swt.ViewInidicator.ViewIndicator)
	public void viewIndicatorRefresh(final ViewIndicator viewIndicator) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				TreeItem treeItem = (TreeItem) mapViewIndicatorToTreeItem.get(viewIndicator);
				if (treeItem == null) {
					return;
				}
				Rectangle bounds = treeItem.getBounds();
				Rectangle treeBounds = tree.getBounds();
				tree.redraw(0, bounds.y, treeBounds.width, bounds.height, true);
			}
		});
	}
}
