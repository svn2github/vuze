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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;
import org.gudy.azureus2.ui.swt.minibar.AllTransfersBar;
import org.gudy.azureus2.ui.swt.plugins.*;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTInstanceImpl;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.views.AbstractIView;
import org.gudy.azureus2.ui.swt.views.IView;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.UIFunctionsUserPrompter;
import com.aelitis.azureus.ui.UIStatusTextClickListener;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.shells.BrowserWindow;

import org.gudy.azureus2.plugins.PluginView;

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
	 * This isn't presently populated.
	 * mapPluginViews stores the plugin views that need to be added once the
	 * oldMainWindow is created.  Currently, we create the oldMainWindow
	 * at startup.  Once we swtich to delayed oldMainWindow creation, in theory
	 * the code will work.
	 */
	private final Map mapPluginViews = new HashMap();

	private final AEMonitor pluginViews_mon = new AEMonitor("v3.uif.pluginViews");

	/**
	 * @param window
	 */
	public UIFunctionsImpl(
			com.aelitis.azureus.ui.swt.shells.main.MainWindow window) {
		this.mainWindow = window;
	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#addPluginView(org.gudy.azureus2.plugins.PluginView)
	public void addPluginView(PluginView view) {
		try {
			UIFunctionsSWT uiFunctions = mainWindow.getOldUIFunctions(true);
			if (uiFunctions == null) {
				pluginViews_mon.enter();
				try {
					mapPluginViews.put(view, null);
				} finally {
					pluginViews_mon.exit();
				}
				return;
			}

			uiFunctions.addPluginView(view);

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "addPluginView", e));
		}

	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#addPluginView(java.lang.String, org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener)
	public void addPluginView(String viewID, UISWTViewEventListener l) {
		try {
			UIFunctionsSWT uiFunctions = mainWindow.getOldUIFunctions(true);
			if (uiFunctions == null) {
				pluginViews_mon.enter();
				try {
					mapPluginViews.put(viewID, l);
				} finally {
					pluginViews_mon.exit();
				}
				return;
			}

			uiFunctions.addPluginView(viewID, l);

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "addPluginView", e));
		}

	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#addPluginView(org.gudy.azureus2.ui.swt.plugins.UISWTPluginView)
	public void addPluginView(UISWTPluginView view) {
		try {
			UIFunctionsSWT uiFunctions = mainWindow.getOldUIFunctions(true);
			if (uiFunctions == null) {
				pluginViews_mon.enter();
				try {
					mapPluginViews.put(view, null);
				} finally {
					pluginViews_mon.exit();
				}
				return;
			}

			uiFunctions.addPluginView(view);

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "addPluginView", e));
		}

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
			UIFunctionsSWT uiFunctions = mainWindow.getOldUIFunctions(false);
			if (uiFunctions == null) {
				return;
			}

			uiFunctions.closeDownloadBars();

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "closeDownloadBars", e));
		}

	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#closePluginView(org.gudy.azureus2.ui.swt.views.IView)
	public void closePluginView(IView view) {
		try {
			UIFunctionsSWT uiFunctions = mainWindow.getOldUIFunctions(false);
			if (uiFunctions == null) {
				return;
			}

			uiFunctions.closePluginView(view);

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "closePluginView", e));
		}

	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#closePluginViews(java.lang.String)
	public void closePluginViews(String sViewID) {
		try {
			UIFunctionsSWT uiFunctions = mainWindow.getOldUIFunctions(false);
			if (uiFunctions == null) {
				return;
			}

			uiFunctions.closePluginViews(sViewID);

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

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#getMenu(int)
	public Menu getMenu(int id) {
		// TODO Auto-generated method stub
		// XXX Don't use oldMainWindow, menu is global and oldMainWindow
		//     shouldn't need to be initialized
		return null;
	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#getPluginViews()
	public UISWTView[] getPluginViews() {
		try {
			UIFunctionsSWT uiFunctions = mainWindow.getOldUIFunctions(true);
			if (uiFunctions == null) {
				return new UISWTView[0];
			}

			return uiFunctions.getPluginViews();

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

	// @see com.aelitis.azureus.ui.UIFunctions#openManagerView(org.gudy.azureus2.core3.download.DownloadManager)
	public void openManagerView(DownloadManager dm) {
		try {
			UIFunctionsSWT uiFunctions = mainWindow.getOldUIFunctions(true);
			if (uiFunctions == null) {
				return;
			}

			mainWindow.switchToAdvancedTab();
			uiFunctions.openManagerView(dm);

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "openManagerView", e));
		}

	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#openPluginView(org.gudy.azureus2.ui.swt.views.AbstractIView, java.lang.String)
	public void openPluginView(AbstractIView view, String name) {
		try {
			UIFunctionsSWT uiFunctions = mainWindow.getOldUIFunctions(true);
			if (uiFunctions == null) {
				return;
			}

			mainWindow.switchToAdvancedTab();
			uiFunctions.openPluginView(view, name);

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "openPluginView", e));
		}

	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#openPluginView(org.gudy.azureus2.plugins.PluginView)
	public void openPluginView(PluginView view) {
		try {
			UIFunctionsSWT uiFunctions = mainWindow.getOldUIFunctions(true);
			if (uiFunctions == null) {
				return;
			}

			mainWindow.switchToAdvancedTab();
			uiFunctions.openPluginView(view);

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "openPluginView", e));
		}

	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#openPluginView(java.lang.String, java.lang.String, org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener, java.lang.Object, boolean)
	public void openPluginView(String sParentID, String sViewID,
			UISWTViewEventListener l, Object dataSource, boolean bSetFocus) {
		try {
			UIFunctionsSWT uiFunctions = mainWindow.getOldUIFunctions(true);
			if (uiFunctions == null) {
				return;
			}

			mainWindow.switchToAdvancedTab();
			uiFunctions.openPluginView(sParentID, sViewID, l, dataSource, bSetFocus);

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "openPluginView", e));
		}

	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#openPluginView(org.gudy.azureus2.ui.swt.plugins.UISWTPluginView)
	public void openPluginView(UISWTPluginView view) {
		try {
			UIFunctionsSWT uiFunctions = mainWindow.getOldUIFunctions(true);
			if (uiFunctions == null) {
				return;
			}

			mainWindow.switchToAdvancedTab();
			uiFunctions.openPluginView(view);

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "openPluginView", e));
		}

	}

	// @see com.aelitis.azureus.ui.UIFunctions#refreshIconBar()
	public void refreshIconBar() {
		try {
			UIFunctionsSWT uiFunctions = mainWindow.getOldUIFunctions(true);
			if (uiFunctions == null) {
				return;
			}

			uiFunctions.refreshIconBar();

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "refreshIconBar", e));
		}

	}

	// @see com.aelitis.azureus.ui.UIFunctions#refreshLanguage()
	public void refreshLanguage() {
		try {
			UIFunctionsSWT uiFunctions = mainWindow.getOldUIFunctions(true);
			if (uiFunctions == null) {
				return;
			}

			uiFunctions.refreshLanguage();

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "refreshLanguage", e));
		}

	}

	// @see com.aelitis.azureus.ui.UIFunctions#removeManagerView(org.gudy.azureus2.core3.download.DownloadManager)
	public void removeManagerView(DownloadManager dm) {
		try {
			UIFunctionsSWT uiFunctions = mainWindow.getOldUIFunctions(true);
			if (uiFunctions == null) {
				return;
			}

			uiFunctions.removeManagerView(dm);

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "removeManagerView", e));
		}

	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#removePluginView(java.lang.String)
	public void removePluginView(String viewID) {
		try {
			UIFunctionsSWT uiFunctions = mainWindow.getOldUIFunctions(true);
			if (uiFunctions == null) {
				pluginViews_mon.enter();
				try {
					mapPluginViews.remove(viewID);
				} finally {
					pluginViews_mon.exit();
				}
				return;
			}

			uiFunctions.removePluginView(viewID);

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "removePluginView", e));
		}

	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#removePluginView(org.gudy.azureus2.ui.swt.plugins.UISWTPluginView)
	public void removePluginView(UISWTPluginView view) {
		try {
			UIFunctionsSWT uiFunctions = mainWindow.getOldUIFunctions(true);
			if (uiFunctions == null) {
				pluginViews_mon.enter();
				try {
					mapPluginViews.remove(view);
				} finally {
					pluginViews_mon.exit();
				}
				return;
			}

			uiFunctions.removePluginView(view);

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "removePluginView", e));
		}

	}

	// @see com.aelitis.azureus.ui.UIFunctions#requestShutdown()
	public boolean requestShutdown() {
		try {
			UIFunctionsSWT uiFunctions = mainWindow.getOldUIFunctions(true);
			if (uiFunctions == null) {
				return true;
			}

			return uiFunctions.requestShutdown();

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "requestShutdown", e));
		}

		return false;
	}

	// @see com.aelitis.azureus.ui.UIFunctions#setStatusText(java.lang.String)
	public void setStatusText(String string) {
		// TODO Auto-generated method stub

		// XXX Don't use oldMainWindow, status bar is global and oldMainWindow
		//     shouldn't need to be initialized
	}

	// @see com.aelitis.azureus.ui.UIFunctions#setStatusText(int, java.lang.String, com.aelitis.azureus.ui.UIStatusTextClickListener)
	public void setStatusText(int statustype, String string,
			UIStatusTextClickListener l) {
		// TODO Auto-generated method stub

	}

	// @see com.aelitis.azureus.ui.UIFunctions#showConfig(java.lang.String)
	public boolean showConfig(String string) {
		try {
			UIFunctionsSWT uiFunctions = mainWindow.getOldUIFunctions(true);
			if (uiFunctions == null) {
				return false;
			}

			mainWindow.switchToAdvancedTab();
			return uiFunctions.showConfig(string);

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "showConfig", e));
		}

		return false;
	}

	// @see com.aelitis.azureus.ui.UIFunctions#showMyShares()
	public void showMyShares() {
		try {
			UIFunctionsSWT uiFunctions = mainWindow.getOldUIFunctions(true);
			if (uiFunctions == null) {
				return;
			}

			mainWindow.switchToAdvancedTab();
			uiFunctions.showMyShares();

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "showMyShares", e));
		}

	}

	// @see com.aelitis.azureus.ui.UIFunctions#showMyTorrents()
	public void showMyTorrents() {
		try {
			UIFunctionsSWT uiFunctions = mainWindow.getOldUIFunctions(true);
			if (uiFunctions == null) {
				return;
			}

			mainWindow.switchToAdvancedTab();
			uiFunctions.showMyTorrents();

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "showMyTorrents", e));
		}

	}

	// @see com.aelitis.azureus.ui.UIFunctions#showMyTracker()
	public void showMyTracker() {
		try {
			UIFunctionsSWT uiFunctions = mainWindow.getOldUIFunctions(true);
			if (uiFunctions == null) {
				return;
			}

			mainWindow.switchToAdvancedTab();
			uiFunctions.showMyTracker();

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "showMyTracker", e));
		}

	}

	// @see com.aelitis.azureus.ui.UIFunctions#showStats()
	public void showStats() {
		try {
			UIFunctionsSWT uiFunctions = mainWindow.getOldUIFunctions(true);
			if (uiFunctions == null) {
				return;
			}

			mainWindow.switchToAdvancedTab();
			uiFunctions.showStats();

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "showStats", e));
		}

	}

	// @see com.aelitis.azureus.ui.UIFunctions#showStatsDHT()
	public void showStatsDHT() {
		try {
			UIFunctionsSWT uiFunctions = mainWindow.getOldUIFunctions(true);
			if (uiFunctions == null) {
				return;
			}

			mainWindow.switchToAdvancedTab();
			uiFunctions.showStatsDHT();

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "showStatsDHT", e));
		}
	}

	// @see com.aelitis.azureus.ui.UIFunctions#showStatsTransfers()
	public void showStatsTransfers() {
		try {
			UIFunctionsSWT uiFunctions = mainWindow.getOldUIFunctions(true);
			if (uiFunctions == null) {
				return;
			}

			mainWindow.switchToAdvancedTab();
			uiFunctions.showStatsTransfers();
		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "", e));
		}
	}

	// @see com.aelitis.azureus.ui.UIFunctions#showConsole()
	public void showConsole() {
		try {
			UIFunctionsSWT uiFunctions = mainWindow.getOldUIFunctions(true);
			if (uiFunctions == null) {
				return;
			}

			mainWindow.switchToAdvancedTab();
			uiFunctions.showConsole();
		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "ShowConsole", e));
		}
	}

	public UISWTInstance getUISWTInstance() {
		return mainWindow.getUISWTInstanceImpl();
	}

	public boolean viewURL(final String url, final String target, final int w,
			final int h, final boolean allowResize, final boolean isModal) {

		mainWindow.shell.getDisplay().syncExec(new AERunnable() {
			public void runSupport() {
				if (target == null) {
					BrowserWindow window = new BrowserWindow(mainWindow.shell, url, w, h,
							allowResize, isModal);
					window.waitUntilClosed();
				} else {
					mainWindow.showURL(url, target);
				}
			}
		});
		return true;
	}

	public boolean viewURL(final String url, final String target, final double w,
			final double h, final boolean allowResize, final boolean isModal) {

		mainWindow.shell.getDisplay().syncExec(new AERunnable() {
			public void runSupport() {
				if (target == null) {
					BrowserWindow window = new BrowserWindow(mainWindow.shell, url, w, h,
							allowResize, isModal);
					window.waitUntilClosed();
				} else {
					mainWindow.showURL(url, target);
				}
			}
		});
		return true;
	}

	protected void oldMainWindowInitialized(MainWindow oldMainWindow) {
		UIFunctionsSWT uiFunctions = mainWindow.getOldUIFunctions(true);
		if (uiFunctions == null) {
			return;
		}

		pluginViews_mon.enter();
		try {
			for (Iterator iterator = mapPluginViews.keySet().iterator(); iterator.hasNext();) {
				Object key = iterator.next();
				if (key instanceof PluginView) {
					uiFunctions.addPluginView((PluginView) key);
				} else if (key instanceof UISWTPluginView) {
					uiFunctions.addPluginView((UISWTPluginView) key);
				} else if (key instanceof String) {
					UISWTViewEventListener value = (UISWTViewEventListener) mapPluginViews.get(key);
					uiFunctions.addPluginView((String) key, value);
				}
			}
			mapPluginViews.clear();
		} finally {
			pluginViews_mon.exit();
		}
	}

	// @see com.aelitis.azureus.ui.UIFunctions#promptUser(java.lang.String, java.lang.String, java.lang.String[], int, java.lang.String, java.lang.String, boolean, int)
	public int promptUser(String title, String text, String[] buttons,
			int defaultOption, String rememberID, String rememberText,
			boolean rememberByDefault, int autoCloseInMS) {
		return MessageBoxShell.open(getMainShell(), title, text, buttons,
				defaultOption, rememberID, rememberText, rememberByDefault,
				autoCloseInMS);
	}

	// @see com.aelitis.azureus.ui.UIFunctions#getUserPrompter(java.lang.String, java.lang.String, java.lang.String[], int)
	public UIFunctionsUserPrompter getUserPrompter(String title, String text,
			String[] buttons, int defaultOption) {

		MessageBoxShell mb = new MessageBoxShell(getMainShell(), title, text,
				buttons, defaultOption);
		return mb;
	}

	public boolean isGlobalTransferBarShown() {
		return AllTransfersBar.getManager().isOpen(
				AzureusCoreFactory.getSingleton().getGlobalManager());
	}

	public void showGlobalTransferBar() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				AllTransfersBar.open(
						AzureusCoreFactory.getSingleton().getGlobalManager(),
						getMainShell());
			}
		});
	}
	
	public void closeGlobalTransferBar() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				AllTransfersBar.close(
						AzureusCoreFactory.getSingleton().getGlobalManager());
			}
		});
	} 
	
	public void refreshTorrentMenu() {
		try {
			UIFunctionsSWT uiFunctions = mainWindow.getOldUIFunctions(true);
			if (uiFunctions == null) {
				return;
			}

			uiFunctions.refreshTorrentMenu();

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "refreshIconBar", e));
		}
	}
}
