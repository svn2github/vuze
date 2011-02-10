/*
 * Created on Jul 13, 2006 6:15:55 PM
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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
 */
package com.aelitis.azureus.ui.swt.shells.main;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginView;
import org.gudy.azureus2.plugins.ui.UIInputReceiver;
import org.gudy.azureus2.plugins.ui.UIInputReceiverListener;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.SimpleTextEntryWindow;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.*;
import org.gudy.azureus2.ui.swt.minibar.AllTransfersBar;
import org.gudy.azureus2.ui.swt.minibar.MiniBarManager;
import org.gudy.azureus2.ui.swt.plugins.*;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTInstanceImpl;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewImpl;
import org.gudy.azureus2.ui.swt.shells.CoreWaiterSWT;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.shells.MessageSlideShell;
import org.gudy.azureus2.ui.swt.update.FullUpdateWindow;
import org.gudy.azureus2.ui.swt.views.*;
import org.gudy.azureus2.ui.swt.views.clientstats.ClientStatsView;
import org.gudy.azureus2.ui.swt.views.stats.StatsView;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.ui.*;
import com.aelitis.azureus.ui.common.table.TableView;
import com.aelitis.azureus.ui.common.updater.UIUpdater;
import com.aelitis.azureus.ui.mdi.MdiEntry;
import com.aelitis.azureus.ui.mdi.MultipleDocumentInterface;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.Initializer;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.mdi.BaseMdiEntry;
import com.aelitis.azureus.ui.swt.mdi.MultipleDocumentInterfaceSWT;
import com.aelitis.azureus.ui.swt.plugininstall.*;
import com.aelitis.azureus.ui.swt.shells.BrowserWindow;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.uiupdater.UIUpdaterSWT;
import com.aelitis.azureus.ui.swt.utils.ColorCache;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager;
import com.aelitis.azureus.ui.swt.views.skin.SkinnedDialog;
import com.aelitis.azureus.ui.swt.views.skin.ToolBarView;
import com.aelitis.azureus.ui.swt.views.skin.TorrentListViewsUtils;
import com.aelitis.azureus.ui.swt.views.skin.SkinnedDialog.SkinnedDialogClosedListener;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.util.ContentNetworkUtils;
import com.aelitis.azureus.util.UrlFilter;

/**
 * @author TuxPaper
 * @created Jul 13, 2006
 *
 */
public class UIFunctionsImpl
	implements UIFunctionsSWT
{
	private final static LogIDs LOGID = LogIDs.GUI;

	private final com.aelitis.azureus.ui.swt.shells.main.MainWindow mainWindow;

	/**
	 * Stores the current <code>SWTSkin</code> so it can be used by {@link #createMenu(Shell)}
	 */
	private SWTSkin skin = null;

	protected boolean isTorrentMenuVisible;

	/**
	 * @param window
	 */
	public UIFunctionsImpl(
			com.aelitis.azureus.ui.swt.shells.main.MainWindow window) {
		this.mainWindow = window;
		
		COConfigurationManager.addAndFireParameterListener(
				"show_torrents_menu", new ParameterListener() {
					public void parameterChanged(String parameterName) {
						isTorrentMenuVisible = COConfigurationManager.getBooleanParameter("show_torrents_menu");
					}
				});
	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#addPluginView(org.gudy.azureus2.plugins.PluginView)
	@SuppressWarnings("deprecation")
	public void addPluginView(PluginView view) {
		PluginsMenuHelper.getInstance().addPluginView(view,
				view.getPluginViewName());
	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#addPluginView(java.lang.String, org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener)
	public void addPluginView(final String viewID, final UISWTViewEventListener l) {
		try {

			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					PluginsMenuHelper.getInstance().addPluginView(viewID, l);
				}
			});

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "addPluginView", e));
		}

	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#addPluginView(org.gudy.azureus2.ui.swt.plugins.UISWTPluginView)
	@SuppressWarnings("deprecation")
	public void addPluginView(UISWTPluginView view) {
		PluginsMenuHelper.getInstance().addPluginView(view,
				view.getPluginViewName());
	}

	// @see com.aelitis.azureus.ui.UIFunctions#bringToFront()
	public void bringToFront() {
		bringToFront(true);
	}

	// @see com.aelitis.azureus.ui.UIFunctions#bringToFront(boolean)
	public void bringToFront(final boolean tryTricks) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				try {
					// this will force active and set !minimized after PW test
					mainWindow.setVisible(true, tryTricks);

				} catch (Exception e) {
					Logger.log(new LogEvent(LOGID, "bringToFront", e));
				}

			}
		});
	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#closeDownloadBars()
	public void closeDownloadBars() {
		try {
			Utils.execSWTThreadLater(0, new AERunnable() {
				public void runSupport() {
					MiniBarManager.getManager().closeAll();
				}
			});

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "closeDownloadBars", e));
		}

	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#closePluginView(org.gudy.azureus2.ui.swt.views.IView)
	public void closePluginView(IView view) {
		try {
			MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
			if (mdi == null) {
				return;
			}
			String id;
			if (view instanceof UISWTViewImpl) {
				id = ((UISWTViewImpl)view).getViewID();
			} else {
				id = view.getClass().getName();
				int i = id.lastIndexOf('.');
				if (i > 0) {
					id = id.substring(i + 1);
				}
			}
			mdi.closeEntry(id);

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "closePluginView", e));
		}

	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#closePluginViews(java.lang.String)
	public void closePluginViews(String sViewID) {
		try {
			MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
			if (mdi == null) {
				return;
			}
			mdi.closeEntry(sViewID);
			
		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "closePluginViews", e));
		}

	}

	// @see com.aelitis.azureus.ui.UIFunctions#dispose(boolean, boolean)
	public boolean dispose(boolean for_restart, boolean close_already_in_progress) {
		try {
			return mainWindow.dispose(for_restart, close_already_in_progress);
		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "Disposing MainWindow", e));
		}
		return false;
	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#getMainShell()
	public Shell getMainShell() {
		return mainWindow.shell;
	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#getPluginViews()
	public UISWTView[] getPluginViews() {
		try {
			return new UISWTView[0];
		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "getPluginViews", e));
		}

		return new UISWTView[0];
	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#getSWTPluginInstanceImpl()
	public UISWTInstanceImpl getSWTPluginInstanceImpl() {
		try {
			return (UISWTInstanceImpl) mainWindow.getUISWTInstanceImpl();
		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "getSWTPluginInstanceImpl", e));
		}

		return null;
	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#openPluginView(org.gudy.azureus2.ui.swt.views.AbstractIView, java.lang.String)
	public void openPluginView(AbstractIView view, String name) {
		try {
			MultipleDocumentInterfaceSWT mdi = getMDISWT();
			if (mdi == null) {
				return;
			}
			if (mdi.createEntryFromIView(
					MultipleDocumentInterface.SIDEBAR_HEADER_PLUGINS, view, name, null,
					true, true, true) != null) {
				return;
			}
		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "openPluginView", e));
		}

	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#openPluginView(org.gudy.azureus2.plugins.PluginView)
	@SuppressWarnings("deprecation")
	public void openPluginView(PluginView view) {
		openPluginView(view, view.getPluginViewName());
	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#openPluginView(java.lang.String, java.lang.String, org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener, java.lang.Object, boolean)
	public void openPluginView(String sParentID, String sViewID,
			UISWTViewEventListener l, Object dataSource, boolean bSetFocus) {
		try {
			MultipleDocumentInterfaceSWT mdi = getMDISWT();

			if (mdi != null) {
				
				String sidebarParentID = null;
				
				if (UISWTInstance.VIEW_MYTORRENTS.equals(sParentID)) {
					sidebarParentID = SideBar.SIDEBAR_HEADER_TRANSFERS;
				} else if (UISWTInstance.VIEW_MAIN.equals(sParentID)) {
					sidebarParentID = MultipleDocumentInterface.SIDEBAR_HEADER_PLUGINS;
				} else {
					System.err.println("Can't find parent " + sParentID + " for " + sViewID);
				}
				
				MdiEntry entry = mdi.createEntryFromEventListener(sidebarParentID, l, sViewID,
						true, dataSource);
				if (bSetFocus) {
					mdi.showEntryByID(sViewID);
				} else if (entry instanceof BaseMdiEntry) {
					// Some plugins (CVS Updater) want their view's composite initialized
					// on OpenPluginView, otherwise they won't do logic users expect
					// (like check for new snapshots).  So, enforce loading entry.
					((BaseMdiEntry) entry).build();
				}
			}
		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "openPluginView", e));
		}

	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#openPluginView(org.gudy.azureus2.ui.swt.plugins.UISWTPluginView)
	@SuppressWarnings("deprecation")
	public void openPluginView(UISWTPluginView view) {
		openPluginView(view, view.getPluginViewName());
	}

	// @see com.aelitis.azureus.ui.UIFunctions#refreshIconBar()
	public void refreshIconBar() {
		try {
			ToolBarView tb = (ToolBarView) SkinViewManager.getByClass(ToolBarView.class);
			if (tb != null) {
				tb.refreshCoreToolBarItems();
			}

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "refreshIconBar", e));
		}

	}

	// @see com.aelitis.azureus.ui.UIFunctions#refreshLanguage()
	public void refreshLanguage() {
		try {
			mainWindow.setSelectedLanguageItem();
			
		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "refreshLanguage", e));
		}

	}

	// @see com.aelitis.azureus.ui.UIFunctions#removeManagerView(org.gudy.azureus2.core3.download.DownloadManager)
	public void removeManagerView(DownloadManager dm) {
		// TODO: Remove me
		// old main window used to keep a list of manager views.  New one doesn't
	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#removePluginView(java.lang.String)
	public void removePluginView(String viewID) {
		try {

			PluginsMenuHelper.getInstance().removePluginViews(viewID);

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "removePluginView", e));
		}

	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#removePluginView(org.gudy.azureus2.ui.swt.plugins.UISWTPluginView)
	@SuppressWarnings("deprecation")
	public void removePluginView(UISWTPluginView view) {
		try {

			PluginsMenuHelper.getInstance().removePluginView(view, view.getPluginViewName());

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "removePluginView", e));
		}

	}

	// @see com.aelitis.azureus.ui.UIFunctions#setStatusText(java.lang.String)
	public void setStatusText(final String string) {
		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				getMainStatusBar().setStatusText(string);
			}
		});
	}

	// @see com.aelitis.azureus.ui.UIFunctions#setStatusText(int, java.lang.String, com.aelitis.azureus.ui.UIStatusTextClickListener)
	public void setStatusText(final int statustype, final String string,
			final UIStatusTextClickListener l) {
		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				getMainStatusBar().setStatusText(statustype, string, l);
			}
		});
	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#getMainStatusBar()
	public MainStatusBar getMainStatusBar() {
		return mainWindow.getMainStatusBar();
	}
	
	// @see com.aelitis.azureus.ui.UIFunctions#showConfig(java.lang.String)
	public boolean showConfig(String section) {
		try {
			boolean uiClassic = COConfigurationManager.getStringParameter("ui").equals("az2");
			if (uiClassic) {
				mainWindow.openView(SideBar.SIDEBAR_HEADER_PLUGINS, ConfigView.class, null, section, true);
			} else {
				ConfigShell.getInstance().open(section);
			}
			return true;

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "showConfig", e));
		}

		return false;
	}

	public void openView(final int viewID, final Object data) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				_openView(viewID, data);
			}
		});
	}
		
	private void _openView(int viewID, Object data) {
		switch (viewID) {
			case VIEW_CONSOLE:
				mainWindow.openView(SideBar.SIDEBAR_HEADER_PLUGINS, LoggerView.class,
						null, data, true);
				break;

			case VIEW_ALLPEERS:
				mainWindow.openView(SideBar.SIDEBAR_HEADER_TRANSFERS, PeerSuperView.class,
						null, data, true);
				break;

			case VIEW_PEERS_STATS:
				mainWindow.openView(SideBar.SIDEBAR_HEADER_PLUGINS, ClientStatsView.class,
						null, data, true);
				break;

			case VIEW_CONFIG:
				showConfig((data instanceof String) ? (String) data : null);
				break;

			case VIEW_DM_DETAILS:
				String id = "DMDetails_";
				if (data instanceof DownloadManager) {
					DownloadManager dm = (DownloadManager) data;
					TOTorrent torrent = dm.getTorrent();
					if (torrent != null) {
						try {
							id += torrent.getHashWrapper().toBase32String();
						} catch (TOTorrentException e) {
							e.printStackTrace();
						}
					}
				}

				mainWindow.openView(SideBar.SIDEBAR_HEADER_TRANSFERS, ManagerView.class,
						id, data, true);
				break;

			case VIEW_DM_MULTI_OPTIONS:
				mainWindow.openView(SideBar.SIDEBAR_HEADER_TRANSFERS,
						TorrentOptionsView.class, null, data, true);
				break;

			case VIEW_MYSHARES:
				mainWindow.openView(SideBar.SIDEBAR_HEADER_PLUGINS,
						MySharesView.class, null, data, true);
				break;

			case VIEW_MYTORRENTS: {
				MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
				if (mdi != null) {
					mdi.showEntryByID(SideBar.SIDEBAR_SECTION_LIBRARY);
				}
			}
				break;

			case VIEW_MYTRACKER:
				mainWindow.openView(SideBar.SIDEBAR_HEADER_PLUGINS, MyTrackerView.class,
						null, data, true);
				break;

			case VIEW_STATS:
				mainWindow.openView(SideBar.SIDEBAR_HEADER_PLUGINS, StatsView.class,
						null, data, true);
				break;
				
			case VIEW_RCM: {
				MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
				if (mdi != null) {
					mdi.showEntryByID(SideBar.SIDEBAR_SECTION_RELATED_CONTENT);
				}
				break;
			}

			default:
				break;
		}
	}


	public UISWTInstance getUISWTInstance() {
		return mainWindow.getUISWTInstanceImpl();
	}
	
	// @see com.aelitis.azureus.ui.UIFunctions#viewURL(java.lang.String, java.lang.String, java.lang.String)
	public void viewURL(String url, String target, String sourceRef) {
		ContentNetworkUtils.setSourceRef(target, sourceRef, false);
		viewURL(url, target, 0, 0, true, false);
	}

	public boolean viewURL(final String url, final String target, final int w,
			final int h, final boolean allowResize, final boolean isModal) {

		mainWindow.shell.getDisplay().syncExec(new AERunnable() {
			public void runSupport() {
				String realURL = url;
				ContentNetwork cn = ContentNetworkUtils.getContentNetworkFromTarget(target);
				if ( !realURL.startsWith( "http" )
					&& !realURL.startsWith("#")) {
					if ("_blank".equals(target)) {
						realURL = cn.getExternalSiteRelativeURL(realURL, false );
					} else {
						realURL = cn.getSiteRelativeURL(realURL, false );
					}
				}
				if (target == null) {
					if (UrlFilter.getInstance().urlCanRPC(realURL)) {
						realURL = cn.appendURLSuffix(realURL, false, true);
					}
					BrowserWindow window = new BrowserWindow(mainWindow.shell, realURL,
							w, h, allowResize, isModal);
					window.waitUntilClosed();
				} else {
					mainWindow.showURL(realURL, target);
				}
			}
		});
		return true;
	}

	public boolean viewURL(final String url, final String target, final double w,
			final double h, final boolean allowResize, final boolean isModal) {

		mainWindow.shell.getDisplay().syncExec(new AERunnable() {
			public void runSupport() {
				String realURL = url;
				ContentNetwork cn = ContentNetworkUtils.getContentNetworkFromTarget(target);
				if ( !realURL.startsWith( "http" )){
					if ("_blank".equals(target)) {
						realURL = cn.getExternalSiteRelativeURL(realURL, false );
					} else {
						realURL = cn.getSiteRelativeURL(realURL, false );
					}
				}
				if (target == null) {
					if (UrlFilter.getInstance().urlCanRPC(realURL)) {
						realURL = cn.appendURLSuffix(realURL, false, true);
					}
					BrowserWindow window = new BrowserWindow(mainWindow.shell, realURL,
							w, h, allowResize, isModal);
					window.waitUntilClosed();
				} else {
					mainWindow.showURL(realURL, target);
				}
			}
		});
		return true;
	}

	// @see com.aelitis.azureus.ui.UIFunctions#promptUser(java.lang.String, java.lang.String, java.lang.String[], int, java.lang.String, java.lang.String, boolean, int)
	public void promptUser(String title, String text, String[] buttons,
			int defaultOption, String rememberID, String rememberText,
			boolean rememberByDefault, int autoCloseInMS, UserPrompterResultListener l) {
		MessageBoxShell.open(getMainShell(), title, text, buttons,
				defaultOption, rememberID, rememberText, rememberByDefault,
				autoCloseInMS, l);
	}

	// @see com.aelitis.azureus.ui.UIFunctions#getUserPrompter(java.lang.String, java.lang.String, java.lang.String[], int)
	public UIFunctionsUserPrompter getUserPrompter(String title, String text,
			String[] buttons, int defaultOption) {

		MessageBoxShell mb = new MessageBoxShell(title, text, buttons,
				defaultOption);
		return mb;
	}

	public boolean isGlobalTransferBarShown() {
		if (!AzureusCoreFactory.isCoreRunning()) {
			return false;
		}
		return AllTransfersBar.getManager().isOpen(
				AzureusCoreFactory.getSingleton().getGlobalManager());
	}

	public void showGlobalTransferBar() {
		CoreWaiterSWT.waitForCoreRunning(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(AzureusCore core) {
				AllTransfersBar.open(core.getGlobalManager(), getMainShell());
			}
		});
	}

	public void closeGlobalTransferBar() {
		CoreWaiterSWT.waitForCoreRunning(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(AzureusCore core) {
				AllTransfersBar.close(core.getGlobalManager());
			}
		});
	}

	public void refreshTorrentMenu() {
		if (!isTorrentMenuVisible) {
			return;
		}
		try {
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					final MenuItem torrentItem = MenuFactory.findMenuItem(
							mainWindow.getMainMenu().getMenu(IMenuConstants.MENU_ID_MENU_BAR),
							MenuFactory.MENU_ID_TORRENT, false);

					if (null != torrentItem) {

						DownloadManager[] dms = SelectedContentManager.getDMSFromSelectedContent();

						final DownloadManager[] dm_final = dms;
						final boolean detailed_view_final = false;
						if (null == dm_final) {
							torrentItem.setEnabled(false);
						} else {
							TableView<?> tv = SelectedContentManager.getCurrentlySelectedTableView();

							torrentItem.getMenu().setData("TableView", tv);
							torrentItem.getMenu().setData("downloads", dm_final);
							torrentItem.getMenu().setData("is_detailed_view",
									Boolean.valueOf(detailed_view_final));
							torrentItem.setEnabled(true);
						}
					}
				}
			});

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "refreshTorrentMenu", e));
		}
	}

	public IMainMenu createMainMenu(Shell shell) {
		IMainMenu menu;
		boolean uiClassic = COConfigurationManager.getStringParameter("ui").equals("az2");
		if (uiClassic) {
			menu = new org.gudy.azureus2.ui.swt.mainwindow.MainMenu(shell);
		} else {
			menu = new MainMenu(skin, shell);
		}
		return menu;
	}

	public SWTSkin getSkin() {
		return skin;
	}

	public void setSkin(SWTSkin skin) {
		this.skin = skin;
	}

	public IMainWindow getMainWindow() {
		return mainWindow;
	}

	// @see com.aelitis.azureus.ui.UIFunctions#getUIUpdater()
	public UIUpdater getUIUpdater() {
		return UIUpdaterSWT.getInstance();
	}
	
	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#closeAllDetails()
	public void closeAllDetails() {
		MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
		if (mdi == null) {
			return;
		}
		MdiEntry[] sideBarEntries = mdi.getEntries();
		for (int i = 0; i < sideBarEntries.length; i++) {
			MdiEntry entry = sideBarEntries[i];
			String id = entry.getId();
			if (id != null && id.startsWith("DMDetails_")) {
				mdi.closeEntry(id);
			}
		}

	}
	
	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#hasDetailViews()
	public boolean hasDetailViews() {
		MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
		if (mdi == null) {
			return false;
		}

		MdiEntry[] sideBarEntries = mdi.getEntries();
		for (int i = 0; i < sideBarEntries.length; i++) {
			MdiEntry entry = sideBarEntries[i];
			String id = entry.getId();
			if (id != null && id.startsWith("DMDetails_")) {
				return true;
			}
		}

		return false;
	}
	
	public void 
	performAction(
		int 					action_id, 
		Object 					args, 
		final actionListener 	listener )
	{
		if ( action_id == ACTION_FULL_UPDATE ){
			
			FullUpdateWindow.handleUpdate((String)args, listener );
			
		}else if ( action_id == ACTION_UPDATE_RESTART_REQUEST ){
			
			String MSG_PREFIX = "UpdateMonitor.messagebox.";
			
			String title = MessageText.getString(MSG_PREFIX + "restart.title" );
			
			String text = MessageText.getString(MSG_PREFIX + "restart.text" );
			
			bringToFront();
			
			boolean no_timeout = args instanceof Boolean && ((Boolean)args).booleanValue();
			
			int timeout = 180000;
			
			if ( no_timeout || !PluginInitializer.getDefaultInterface().getPluginManager().isSilentRestartEnabled()){
				
				timeout = -1;
			}
			
			promptUser(
				title, 
				text, 
				new String[] {
					MessageText.getString("UpdateWindow.restart"),
					MessageText.getString("UpdateWindow.restartLater")
				}, 
				0, 
				null, 
				null, 
				false, 
				timeout, 
				new UserPrompterResultListener() 
				{
					public void 
					prompterClosed(
						int result ) 
					{
						listener.actionComplete( result == 0 );
					}
				});
		}else{
			
			Debug.out( "Unknown action " + action_id );
		}
	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#showCoreWaitDlg()
	public Shell showCoreWaitDlg() {
		final SkinnedDialog closeDialog = new SkinnedDialog(
				"skin3_dlg_coreloading", "coreloading.body", SWT.TITLE | SWT.BORDER
				| SWT.APPLICATION_MODAL);
		
		closeDialog.setTitle(MessageText.getString("dlg.corewait.title"));
		SWTSkin skin = closeDialog.getSkin();
		SWTSkinObjectButton soButton = (SWTSkinObjectButton) skin.getSkinObject("close");

		final SWTSkinObjectText soWaitTask = (SWTSkinObjectText) skin.getSkinObject("task");

		final SWTSkinObject soWaitProgress = skin.getSkinObject("progress");
		if (soWaitProgress != null) {
			soWaitProgress.getControl().addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent e) {
					Control c = (Control) e.widget;
					Point size = c.getSize();
					e.gc.setBackground(ColorCache.getColor(e.display, "#23a7df"));
					Object data = soWaitProgress.getData("progress");
					if (data instanceof Long) {
						int waitProgress = ((Long) data).intValue();
						int breakX = size.x * waitProgress / 100;
						e.gc.fillRectangle(0, 0, breakX, size.y);
						e.gc.setBackground(ColorCache.getColor(e.display, "#cccccc"));
						e.gc.fillRectangle(breakX, 0, size.x - breakX, size.y);
					}
				}
			});
		}
		
		if (!AzureusCoreFactory.isCoreRunning()) {
			final Initializer initializer = Initializer.getLastInitializer();
			if (initializer != null) {
				initializer.addListener(new InitializerListener() {
					public void reportPercent(final int percent) {
						Utils.execSWTThread(new AERunnable() {
							public void runSupport() {
								if (soWaitProgress != null && !soWaitProgress.isDisposed()) {
									soWaitProgress.setData("progress", new Long(percent));
									soWaitProgress.getControl().redraw();
									soWaitProgress.getControl().update();
								}
							}
						});
						if (percent > 100) {
							initializer.removeListener(this);
						}
					}
				
					public void reportCurrentTask(String currentTask) {
						if (soWaitTask != null && !soWaitTask.isDisposed()) {
							soWaitTask.setText(currentTask);
						}
					}
				});
			}
		}

		if (soButton != null) {
			soButton.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility,
						SWTSkinObject skinObject, int stateMask) {
					closeDialog.close();
				}
			});
		}

		closeDialog.addCloseListener(new SkinnedDialogClosedListener() {
			public void skinDialogClosed(SkinnedDialog dialog) {
			}
		});

		closeDialog.open();
		return closeDialog.getShell();
	}
	
	// @see com.aelitis.azureus.ui.UIFunctions#doSearch(java.lang.String)
	public void doSearch(String searchText) {
		MainWindow.doSearch(searchText);
	}
	
	public void promptForSearch() {
		SimpleTextEntryWindow entryWindow = new SimpleTextEntryWindow("Button.search", "search.dialog.text");
		entryWindow.prompt(new UIInputReceiverListener() {
			public void UIInputReceiverClosed(UIInputReceiver receiver) {
				if (receiver.hasSubmittedInput()) {
					doSearch(receiver.getSubmittedInput());
				}
			}
		});
	}

	public MultipleDocumentInterface getMDI() {
		return (MultipleDocumentInterface) SkinViewManager.getByViewID(SkinConstants.VIEWID_MDI);
	}

	public MultipleDocumentInterfaceSWT getMDISWT() {
		return (MultipleDocumentInterfaceSWT) SkinViewManager.getByViewID(SkinConstants.VIEWID_MDI);
	}

	public void forceNotify(final int iconID, final String title, final String text,
			final String details, final Object[] relatedObjects, final int timeoutSecs) {
		
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				int swtIconID = SWT.ICON_INFORMATION;
				switch (iconID) {
					case STATUSICON_WARNING:
						swtIconID = SWT.ICON_WARNING;
						break;
						
					case STATUSICON_ERROR:
						swtIconID = SWT.ICON_ERROR;
						break;
				}
				
				new MessageSlideShell(SWTThread.getInstance().getDisplay(), swtIconID,
						title, text, details, relatedObjects, timeoutSecs);
				
			}
		});
	}
	
	public void 
	installPlugin(
		String 				plugin_id,
		String				resource_prefix,
		actionListener		listener )
	{
		new SimplePluginInstaller( plugin_id, resource_prefix, listener );
	}
}
