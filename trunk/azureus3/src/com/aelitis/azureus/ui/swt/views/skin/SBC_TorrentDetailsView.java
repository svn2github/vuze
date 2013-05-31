/*
 * Created on 2 juil. 2003
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
package com.aelitis.azureus.ui.swt.views.skin;

import java.util.ArrayList;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerAdapter;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadImpl;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadManagerImpl;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.debug.ObfusticateTab;
import org.gudy.azureus2.ui.swt.mainwindow.MenuFactory;
import org.gudy.azureus2.ui.swt.plugins.*;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance.UISWTViewEventListenerWrapper;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewEventListenerHolder;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewImpl;
import org.gudy.azureus2.ui.swt.views.MyTorrentsView;
import org.gudy.azureus2.ui.swt.views.PeersView;
import org.gudy.azureus2.ui.swt.views.piece.PieceInfoView;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewTab;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.ToolBarEnabler;
import com.aelitis.azureus.ui.common.ToolBarItem;
import com.aelitis.azureus.ui.common.table.TableView;
import com.aelitis.azureus.ui.common.table.TableViewFilterCheck;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.selectedcontent.ISelectedContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentListener;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.mdi.MdiEntrySWT;
import com.aelitis.azureus.ui.swt.mdi.MultipleDocumentInterfaceSWT;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectText;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectTextbox;
import com.aelitis.azureus.ui.swt.utils.ColorCache;
import com.aelitis.azureus.util.DataSourceUtils;

/**
 * Torrent download view, consisting of several information tabs
 * 
 * @author Olivier
 * 
 */
public class SBC_TorrentDetailsView
	extends SkinView
	implements DownloadManagerListener, ObfusticateTab,
	UIUpdatable, UIPluginViewToolBarListener, SelectedContentListener
{

	private DownloadManager manager;

	private CTabFolder folder;

	private ArrayList<UISWTViewCore> tabViews = new ArrayList<UISWTViewCore>();

	int lastCompleted = -1;

	private GlobalManagerAdapter gmListener;

	private Composite parent;

	protected UISWTViewCore activeView;

	private FilterCheckHandler filter_check_handler;

	private int selection_count = 0;

	private SWTSkinObjectTextbox soFilterTextBox;

	private SWTSkinObjectText soInfoArea;

	private MdiEntrySWT mdi_entry;

	/**
	 * 
	 */
	public SBC_TorrentDetailsView() {
		// assumed if we are opening a Download Manager View that we
		// have a DownloadManager and thus an AzureusCore
		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(AzureusCore core) {
				GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
				gmListener = new GlobalManagerAdapter() {
					public void downloadManagerRemoved(DownloadManager dm) {
						if (dm.equals(manager)) {
							Utils.execSWTThread(new AERunnable() {
								public void runSupport() {
									getMainSkinObject().dispose();
								}
							});
						}
					}
				};
				gm.addListener(gmListener, false);
			}
		});
	}

	public static DownloadManager dataSourceToDownloadManager(Object ds) {
		DownloadManager dm = null;
		if (ds instanceof DownloadImpl) {
			DownloadImpl dataSourcePlugin = (DownloadImpl) ds;
			dm = dataSourcePlugin.getDownload();
		} else if (ds instanceof DownloadManager) {
			dm = (DownloadManager) ds;
		} else if (ds instanceof Object[]
				&& ((Object[]) ds)[0] instanceof DownloadManager) {
			Object[] o = (Object[]) ds;
			dm = (DownloadManager) o[0];
		} else if (ds instanceof String) {
			final String s = (String) ds;
			dm = DataSourceUtils.getDM(s);
		} else {
			dm = null;
		}
		return dm;
	}
	
	private void dataSourceChanged(Object newDataSource) {
		if (manager != null) {
			manager.removeListener(this);
		}

		manager = dataSourceToDownloadManager(newDataSource);
		
		if (newDataSource instanceof Object[]
				&& ((Object[]) newDataSource)[1] instanceof PEPeer) {
			Object[] o = (Object[]) newDataSource;

			PeersView pv = (PeersView) showView(PeersView.class);

			if (pv != null) {

				pv.selectPeer((PEPeer) o[1]);
			}
		}

		if (manager != null) {
			manager.addListener(this);
		}

		for (int i = 0; i < tabViews.size(); i++) {
			UISWTViewCore view = tabViews.get(i);
			if (view != null) {
				view.triggerEvent(UISWTViewEvent.TYPE_DATASOURCE_CHANGED, newDataSource);
			}
		}

		refreshTitle();
	}

	private void delete() {
		UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
		if (uiFunctions != null) {
			uiFunctions.getUIUpdater().removeUpdater(this);
		}
		if (manager != null) {
			manager.removeListener(this);
		}

		try {
			GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
			gm.removeListener(gmListener);
		} catch (Exception e) {
			Debug.out(e);
		}

		SelectedContentManager.removeCurrentlySelectedContentListener(this);

		if (folder != null && !folder.isDisposed()) {
			folder.setSelection(0);
		}

		//Don't ask me why, but without this an exception is thrown further (in folder.dispose() )
		//TODO : Investigate to see if it's a platform (OSX-Carbon) BUG, and report to SWT team.
		if (Utils.isCarbon) {
			if (folder != null && !folder.isDisposed()) {
				Utils.disposeSWTObjects(folder.getItems());
			}
		}

		for (int i = 0; i < tabViews.size(); i++) {
			UISWTViewCore view = tabViews.get(i);
			try {
				if (view != null) {
					view.triggerEvent(UISWTViewEvent.TYPE_DESTROY, null);
				}
			} catch (Throwable t) {
				Debug.out(t);
			}
		}
		tabViews.clear();

		Utils.disposeSWTObjects(new Object[] {
			folder,
			parent
		});
	}

	private void initialize(Composite composite) {

		Composite main_area = new Composite(composite, SWT.NULL);
		main_area.setLayout(new FormLayout());

		boolean az2 = Utils.isAZ2UI();

		Color bg_color = ColorCache.getColor(composite.getDisplay(), "#c0cbd4");

		FormData formData;

		this.parent = composite;
		if (folder == null) {
			folder = new CTabFolder(main_area, SWT.LEFT);
			folder.setBorderVisible(true);
		} else {
			System.out.println("ManagerView::initialize : folder isn't null !!!");
		}

		formData = Utils.getFilledFormData();

		folder.setLayoutData(formData);

		if (composite.getLayout() instanceof FormLayout) {
			main_area.setLayoutData(Utils.getFilledFormData());
		} else if (composite.getLayout() instanceof GridLayout) {
			main_area.setLayoutData(new GridData(GridData.FILL_BOTH));
		}

		folder.setTabHeight(20);

		// Call plugin listeners
		UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
		if (uiFunctions != null) {
			UISWTInstance pluginUI = uiFunctions.getUISWTInstance();

			MyTorrentsView.registerPluginViews(pluginUI);

			// unfortunately views for the manager view are currently registered
			// against 'MyTorrents'...

			for (String id : new String[] {
				UISWTInstance.VIEW_MYTORRENTS,
				UISWTInstance.VIEW_TORRENT_DETAILS
			}) {

				UISWTViewEventListenerWrapper[] pluginViews = pluginUI.getViewListeners(id);

				for (UISWTViewEventListenerWrapper l : pluginViews) {

					if (id == UISWTInstance.VIEW_MYTORRENTS
							&& l.getViewID() == PieceInfoView.MSGID_PREFIX) {
						// Simple hack to exlude PieceInfoView tab as it's already within Pieces View
						continue;
					}

					if (l != null) {

						try {
							UISWTViewImpl view = new UISWTViewImpl(
									UISWTInstance.VIEW_TORRENT_DETAILS, l.getViewID(), l, null);

							addSection(view);

						} catch (Throwable e) {

							Debug.out(e);
						}
					}
				}
			}
		}

		SelectedContentManager.addCurrentlySelectedContentListener(this);

		Menu menu = new Menu(folder);

		menu.setData("downloads", new DownloadManager[] {
			manager
		});
		menu.setData("is_detailed_view", true);

		MenuFactory.buildTorrentMenu(menu);

		folder.setMenu(menu);

		// Initialize view when user selects it
		folder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				CTabItem item = (CTabItem) e.item;
				selectView(item);
			}
		});

		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				if (!folder.isDisposed() && folder.getItemCount() > 0) {
					selectView(folder.getItem(0));
				}
			}
		});

		Utils.relayout(folder);
	}

	private void selectView(CTabItem item) {
		if (item == null) {
			return;
		}
		if (folder.getSelection() != item) {
			folder.setSelection(item);
		}
		folder.getShell().setCursor(
				folder.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));
		try {
			// Send one last refresh to previous tab, just in case it
			// wants to do something when view goes invisible
			refresh();

			if (activeView != null) {
				activeView.triggerEvent(UISWTViewEvent.TYPE_FOCUSLOST, null);

				UISWTViewEventListener listener = activeView.getEventListener();

				if (listener instanceof UISWTViewEventListenerHolder) {

					listener = ((UISWTViewEventListenerHolder) listener).getDelegatedEventListener(activeView);
				}

				// unhook filtering

				if (listener instanceof TableViewTab<?>
						&& listener instanceof TableViewFilterCheck<?>) {

					TableViewTab<?> tvt = (TableViewTab<?>) listener;

					TableViewSWT tv = tvt.getTableView();

					tv.disableFilterCheck();
				}
			}

			UISWTViewCore view = (UISWTViewCore) item.getData("IView");
			if (view == null) {
				Class<?> cla = (Class<?>) item.getData("claEventListener");
				UISWTViewEventListener l = (UISWTViewEventListener) cla.newInstance();
				view = new UISWTViewImpl(UISWTInstance.VIEW_MAIN, cla.getSimpleName(),
						l, manager);
				item.setData("IView", view);
			}

			if (mdi_entry != null) {
				String id = "";
				if (activeView instanceof UISWTViewImpl) {
					id = "" + ((UISWTViewImpl) activeView).getViewID();
					id = id.substring(id.lastIndexOf(".") + 1);
				} else if (activeView != null) {
					String simpleName = activeView.getClass().getName();
					id = simpleName.substring(simpleName.lastIndexOf(".") + 1);
				} else {
					id = "??";
				}
				mdi_entry.setLogID("DMDetails-" + id);
			}
			activeView = view;

			if (item.getControl() == null) {
				view.triggerEvent(UISWTViewEvent.TYPE_DATASOURCE_CHANGED, manager);
				view.initialize(folder);
				item.setControl(view.getComposite());
			}

			UISWTViewEventListener listener = view.getEventListener();

			if (listener instanceof UISWTViewEventListenerHolder) {

				listener = ((UISWTViewEventListenerHolder) listener).getDelegatedEventListener(view);
			}

			// hook in filtering

			if (listener instanceof TableViewTab<?>
					&& listener instanceof TableViewFilterCheck<?>) {

				TableViewTab<Object> tvt = (TableViewTab<Object>) listener;

				TableViewFilterCheck delegate = (TableViewFilterCheck) tvt;

				soFilterTextBox.setVisible(true);

				filter_check_handler = new FilterCheckHandler(tvt, delegate);

				tvt.getTableView().enableFilterCheck(soFilterTextBox.getTextControl(),
						filter_check_handler);

			} else {
				filter_check_handler = null;

				soFilterTextBox.setVisible(false);
			}

			item.getControl().setFocus();
			SelectedContentManager.clearCurrentlySelectedContent();

			view.triggerEvent(UISWTViewEvent.TYPE_FOCUSGAINED, null);

			UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
			if (uiFunctions != null) {
				uiFunctions.refreshIconBar(); // For edit columns view
			}

			refresh();
			mdi_entry.redraw();
			ViewTitleInfoManager.refreshTitleInfo(mdi_entry.getViewTitleInfo());
		} catch (Exception e) {
			Debug.out(e);
		} finally {
			folder.getShell().setCursor(null);
		}
	}

	public void currentlySelectedContentChanged(
			ISelectedContent[] currentContent, String viewId) {
		selection_count = currentContent.length;

		if (filter_check_handler != null) {

			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					filter_check_handler.updateHeader();
				}
			});
		} else if (soInfoArea != null) {
			TableView view = SelectedContentManager.getCurrentlySelectedTableView();
			String s = "";
			if (view != null) {
				int total = view.size(false);

				s = MessageText.getString("library.unopened.header"
						+ (total > 1 ? ".p" : ""), new String[] {
					String.valueOf(total)
				});

				if (selection_count > 1) {

					s += ", " + MessageText.getString("label.num_selected", new String[] {
						String.valueOf(selection_count)
					});
				}
			}

			soInfoArea.setText(s);
		}
	}

	protected Object showView(Class view_class) {
		CTabItem[] items = folder.getItems();

		for (int i = 0; i < items.length; i++) {

			CTabItem item = items[i];

			UISWTViewCore view = (UISWTViewCore) item.getData("IView");

			UISWTViewEventListener listener = view.getEventListener();

			if (listener instanceof UISWTViewEventListenerHolder) {

				UISWTViewEventListenerHolder lh = (UISWTViewEventListenerHolder) listener;

				UISWTViewEventListener delegated_listener = lh.getDelegatedEventListener(view);

				if (view_class.isInstance(delegated_listener)) {

					selectView(item);

					return (delegated_listener);
				}
			}
		}

		return (null);
	}

	private UISWTViewCore getActiveView() {
		return activeView;
	}

	/**
	 * Called when view is visible
	 */
	private void refresh() {
		if (folder == null || folder.isDisposed())
			return;

		try {
			UISWTViewCore view = getActiveView();
			if (view != null) {
				view.triggerEvent(UISWTViewEvent.TYPE_REFRESH, null);
			}

			CTabItem[] items = folder.getItems();

			for (int i = 0; i < items.length; i++) {
				CTabItem item = items[i];
				view = (UISWTViewCore) item.getData("IView");
				try {
					if (item.isDisposed() || view == null) {
						continue;
					}
					String lastTitle = item.getText();
					String newTitle = view.getFullTitle();
					if (lastTitle == null || !lastTitle.equals(newTitle)) {
						item.setText(escapeAccelerators(newTitle));
					}
					String lastToolTip = item.getToolTipText();
					String newToolTip = view.getFullTitle();
					if (lastToolTip == null || !lastToolTip.equals(newToolTip)) {
						item.setToolTipText(newToolTip);
					}
				} catch (Exception e) {
					Debug.printStackTrace(e);
				}
			}

		} catch (Exception e) {
			Debug.printStackTrace(e);
		}
	}

	/**
	 * 
	 *
	 * @since 3.1.0.1
	 */
	private void refreshTitle() {
		int completed = manager == null ? -1 : manager.getStats().getCompleted();
		if (lastCompleted != completed) {
			if (mdi_entry != null) {
				ViewTitleInfoManager.refreshTitleInfo(mdi_entry.getViewTitleInfo());
			}
			lastCompleted = completed;
		}
	}

	protected static String escapeAccelerators(String str) {
		if (str == null) {

			return (str);
		}

		return (str.replaceAll("&", "&&"));
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener#refreshToolBarItems(java.util.Map)
	 */
	public void refreshToolBarItems(Map<String, Long> list) {
		UISWTViewCore active_view = getActiveView();
		if (active_view != null) {
			UIPluginViewToolBarListener l = active_view.getToolBarListener();
			if (l != null) {
				l.refreshToolBarItems(list);
				return;
			}
		}

	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.ui.toolbar.UIToolBarActivationListener#toolBarItemActivated(com.aelitis.azureus.ui.common.ToolBarItem, long, java.lang.Object)
	 */
	public boolean toolBarItemActivated(ToolBarItem item, long activationType,
			Object datasource) {
		UISWTViewCore active_view = getActiveView();
		if (active_view != null) {
			UIPluginViewToolBarListener l = active_view.getToolBarListener();
			if (l != null && l.toolBarItemActivated(item, activationType, datasource)) {
				return true;
			}
		}

		String itemKey = item.getID();

		if (itemKey.equals("editcolumns")) {
			if (active_view instanceof ToolBarEnabler) {
				return ((ToolBarEnabler) active_view).toolBarItemActivated(itemKey);
			}
		}

		return false;
	}

	public void downloadComplete(DownloadManager manager) {
	}

	public void completionChanged(DownloadManager manager, boolean bCompleted) {
	}

	public void filePriorityChanged(DownloadManager download,
			org.gudy.azureus2.core3.disk.DiskManagerFileInfo file) {
	}

	public void stateChanged(DownloadManager manager, int state) {
		if (folder == null || folder.isDisposed())
			return;
		Display display = folder.getDisplay();
		if (display == null || display.isDisposed())
			return;
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				if (uiFunctions != null) {
					uiFunctions.refreshIconBar();
				}
			}
		});
	}

	public void positionChanged(DownloadManager download, int oldPosition,
			int newPosition) {
	}

	public void addSection(UISWTViewImpl view) {
		Object pluginDataSource = null;
		try {
			pluginDataSource = DownloadManagerImpl.getDownloadStatic(manager);
		} catch (DownloadException e) {
			/* Ignore */
		}
		addSection(view, pluginDataSource);
	}

	private void addSection(UISWTViewCore view, Object dataSource) {
		if (view == null)
			return;

		view.triggerEvent(UISWTViewEvent.TYPE_DATASOURCE_CHANGED, dataSource);

		CTabItem item = new CTabItem(folder, SWT.NULL);
		Messages.setLanguageText(item, view.getTitleID());
		item.setData("IView", view);
		tabViews.add(view);
	}

	public String getObfusticatedHeader() {
		int completed = manager.getStats().getCompleted();
		return DisplayFormatters.formatPercentFromThousands(completed) + " : "
				+ manager;
	}

	public DownloadManager getDownload() {
		return manager;
	}

	// @see org.gudy.azureus2.ui.swt.IconBarEnabler#isSelected(java.lang.String)
	public boolean isSelected(String itemKey) {
		return false;
	}

	// @see com.aelitis.azureus.ui.common.updater.UIUpdatable#getUpdateUIName()
	public String getUpdateUIName() {
		return "DMDetails";
	}

	// @see com.aelitis.azureus.ui.common.updater.UIUpdatable#updateUI()
	public void updateUI() {
		refreshTitle();
		refresh();
	}

	private class FilterCheckHandler
		implements TableViewFilterCheck.TableViewFilterCheckEx<Object>
	{
		private TableViewTab<Object> tvt;

		private TableViewFilterCheck delegate;

		boolean enabled;

		int value;

		private FilterCheckHandler(TableViewTab<Object> _tvt,
				TableViewFilterCheck _delegate)

		{
			tvt = _tvt;
			delegate = _delegate;

			updateHeader();
		}

		public boolean filterCheck(Object ds, String filter, boolean regex) {
			return (delegate.filterCheck(ds, filter, regex));
		};

		public void filterSet(String filter) {
			boolean was_enabled = enabled;

			enabled = filter != null && filter.length() > 0;

			delegate.filterSet(filter);

			if (enabled != was_enabled) {

				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						updateHeader();
					}
				});
			}
		}

		public void viewChanged(TableView<Object> view) {
			value = view.size(false);

			if (enabled) {

				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						updateHeader();
					}
				});
			}
		}

		private void updateHeader() {
			int total = manager.getNumFileInfos();

			String s = MessageText.getString("library.unopened.header"
					+ (total > 1 ? ".p" : ""), new String[] {
				String.valueOf(total)
			});

			if (enabled) {

				String extra = MessageText.getString("filter.header.matches1",
						new String[] {
							String.valueOf(value)
						});

				s += " " + extra;
			}

			if (selection_count > 1) {

				s += ", " + MessageText.getString("label.num_selected", new String[] {
					String.valueOf(selection_count)
				});
			}

			if (soInfoArea != null) {
				soInfoArea.setText(s);
			}
		}
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectInitialShow(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		SWTSkinObject soListArea = getSkinObject("torrentdetails-list-area");
		soFilterTextBox = (SWTSkinObjectTextbox) getSkinObject("torrentdetails-filter");
		soInfoArea = (SWTSkinObjectText) getSkinObject("torrentdetails-info");

		MultipleDocumentInterfaceSWT mdi = UIFunctionsManagerSWT.getUIFunctionsSWT().getMDISWT();

		if (mdi != null) {

			mdi_entry = mdi.getEntryFromSkinObject(skinObject);

			if ( mdi_entry == null ){
				
					// We *really* need to not use 'current' here as it is inaccurate (try opening multiple torrent details view
					// at once to see this)
				
				Debug.out( "Failed to get MDI entry from skin object, reverting to using 'current'" );
				
				mdi_entry = mdi.getCurrentEntrySWT();
			}
			
			mdi_entry.addToolbarEnabler(this);
		}

		initialize((Composite) soListArea.getControl());
		return null;
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectShown(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectShown(SWTSkinObject skinObject, Object params) {
		UISWTViewCore view = getActiveView();
		if (view != null) {
			view.triggerEvent(UISWTViewEvent.TYPE_FOCUSGAINED, null);
		}
		refresh();
		return super.skinObjectShown(skinObject, params);
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectHidden(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectHidden(SWTSkinObject skinObject, Object params) {
		UISWTViewCore view = getActiveView();
		if (view != null) {
			view.triggerEvent(UISWTViewEvent.TYPE_FOCUSLOST, null);
		}
		return super.skinObjectHidden(skinObject, params);
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectDestroyed(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectDestroyed(SWTSkinObject skinObject, Object params) {
		delete();
		return super.skinObjectDestroyed(skinObject, params);
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectAdapter#updateLanguage(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object updateLanguage(SWTSkinObject skinObject, Object params) {
		Messages.updateLanguageForControl(folder);
		return super.updateLanguage(skinObject, params);
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectAdapter#dataSourceChanged(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object dataSourceChanged(SWTSkinObject skinObject, Object params) {
		dataSourceChanged(params);
		return super.dataSourceChanged(skinObject, params);
	};
}
