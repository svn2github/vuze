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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.PluginsMenuHelper;
import org.gudy.azureus2.ui.swt.mainwindow.PluginsMenuHelper.IViewInfo;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewImpl;
import org.gudy.azureus2.ui.swt.views.*;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnCreator;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.utils.ColorCache;

import org.gudy.azureus2.plugins.ui.tables.TableManager;

/**
 * @author TuxPaper
 * @created Jun 23, 2008
 *
 */
public class SideBar
	extends SkinView
{
	private SWTSkin skin;

	private SWTSkinObject soSideBarContents;

	private SWTSkinObject soSideBarList;

	private Tree tree;

	private IView currentIView;

	private static Map mapIViewToSkinObject = new HashMap();

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#showSupport(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		skin = skinObject.getSkin();

		soSideBarContents = skin.getSkinObject("sidebar-contents");
		soSideBarList = skin.getSkinObject("sidebar-list");

		setupList();

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

						if (tree.getSelectionCount() == 1
								&& tree.getSelection()[0].equals(item)) {
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

		TreeItem treeItem;

		createSkinned_UISWTViewEventListener(skin, tree, "Dashboard_SB",
				"main.area.events", "Dashboard");

		TreeItem itemLibrary = createSkinned_UISWTViewEventListener(skin, tree,
				"Library_SB", "library", "Library");

		treeItem = new TreeItem(itemLibrary, SWT.NONE);
		treeItem.setText("Seeding");
		treeItem.setData("IViewClass", MyTorrentsView.class);
		treeItem.setData("IViewClassArgs", new Class[] {
			AzureusCore.class,
			boolean.class,
			TableColumnCore[].class
		});
		treeItem.setData(
				"IViewClassVals",
				new Object[] {
					AzureusCoreFactory.getSingleton(),
					new Boolean(true),
					TableColumnCreator.createCompleteDM(TableManager.TABLE_MYTORRENTS_COMPLETE)
				});

		treeItem = new TreeItem(itemLibrary, SWT.NONE);
		treeItem.setText("Downloading");
		treeItem.setData("IViewClass", MyTorrentsView.class);
		treeItem.setData("IViewClassArgs", new Class[] {
			AzureusCore.class,
			boolean.class,
			TableColumnCore[].class
		});
		treeItem.setData("IViewClassVals", new Object[] {
			AzureusCoreFactory.getSingleton(),
			new Boolean(false),
			TableColumnCreator.createIncompleteDM("AA")
		});

		createSkinned_UISWTViewEventListener(skin, tree, "Browse_SB",
				"main.area.browsetab", "On Vuze");

		createSkinned_UISWTViewEventListener(skin, tree, "Publish_SB",
				"main.area.publishtab", "Publish");

		//new TreeItem(tree, SWT.NONE).setText("Search");

		treeItem = new TreeItem(tree, SWT.NONE);
		treeItem.setText("All Peers");
		treeItem.setData("IViewClass", PeerSuperView.class);

		TreeItem itemPlugins = new TreeItem(tree, SWT.NONE);
		itemPlugins.setText("Plugins");
		IViewInfo[] pluginViewsInfo = PluginsMenuHelper.getInstance().getPluginViewsInfo();
		for (int i = 0; i < pluginViewsInfo.length; i++) {
			IViewInfo viewInfo = pluginViewsInfo[i];
			treeItem = new TreeItem(itemPlugins, SWT.NONE);
			treeItem.setText(viewInfo.name);
			//treeItem.setData("IView", viewInfo.view);
			treeItem.setData("UISWTViewEventListener", viewInfo.event_listener);
			treeItem.setData("Plugin.viewID", viewInfo.viewID);
		}

		parent.getShell().layout(true, true);
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
					view = new UISWTViewImpl("MOO", sViewID, l);
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
			viewComposite.setLayout(new GridLayout());

			mapIViewToSkinObject.put(iview, soContents);

			iview.initialize(viewComposite);
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
			String title) {
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
							skin.layout();
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
			TreeItem treeItem = new TreeItem(tree, SWT.NONE);
			treeItem.setText(title);
			treeItem.setData("UISWTViewEventListener", l);
			treeItem.setData("Plugin.viewID", newID);
			return treeItem;
		}
		return null;
	}

	public static interface UISWTViewEventListenerFormLayout
		extends UISWTViewEventListener
	{
	}
}
