/*
 * Created on Jul 12, 2006 2:56:52 PM
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
package org.gudy.azureus2.ui.swt.mainwindow;

import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.MinimizedWindow;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTPluginView;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTInstanceImpl;
import org.gudy.azureus2.ui.swt.views.ConfigView;
import org.gudy.azureus2.ui.swt.views.IView;

import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

import org.gudy.azureus2.plugins.PluginView;

/**
 * @author TuxPaper
 * @created Jul 12, 2006
 *
 */
public class UIFunctionsImpl implements UIFunctionsSWT
{

	private final MainWindow mainwindow;

	/**
	 * @param window
	 */
	public UIFunctionsImpl(MainWindow mainwindow) {
		this.mainwindow = mainwindow;
	}

	// UIFunctions
	public void bringToFront() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				mainwindow.setVisible(true);
			}
		});
	}

	// UIFunctions
	public void addPluginView(final PluginView view) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (mainwindow.getMenu() != null) {
					mainwindow.getMenu().addPluginView(view, view.getPluginViewName());
				}
			}
		});
	}

	public void openPluginView(final PluginView view) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				mainwindow.openPluginView(view, view.getPluginViewName());
			}
		});
	}

	public void removePluginView(final PluginView view) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (mainwindow.getMenu() != null) {
					mainwindow.getMenu().removePluginView(view, view.getPluginViewName());
				}
			}
		});
	}

	public ConfigView showConfig() {
		return mainwindow.showConfig();
	}

	public void showStats() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				mainwindow.showStats();
			}
		});
	}

	public void showStatsDHT() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				mainwindow.showStatsDHT();
			}
		});
	}

	public void showStatsTransfers() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				mainwindow.showStatsTransfers();
			}
		});
	}

	public Shell getMainShell() {
		return mainwindow.getShell();
	}

	public void addPluginView(final UISWTPluginView view) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (mainwindow.getMenu() != null) {
					mainwindow.getMenu().addPluginView(view, view.getPluginViewName());
				}
			}
		});
	}

	public void openPluginView(final UISWTPluginView view) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				mainwindow.openPluginView(view, view.getPluginViewName());
			}
		});
	}

	public void removePluginView(final UISWTPluginView view) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (mainwindow.getMenu() != null) {
					mainwindow.getMenu().removePluginView(view, view.getPluginViewName());
				}
			}
		});
	}

	public boolean showConfig(String string) {
		return mainwindow.showConfig(string);
	}

	public void addPluginView(final String viewID, final UISWTViewEventListener l)
	{
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (mainwindow.getMenu() != null) {
					mainwindow.getMenu().addPluginView(viewID, l);
				}
			}
		});
	}

	public boolean requestShutdown() {
		return mainwindow.destroyRequest();
	}

	public void refreshLanguage() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (mainwindow.getMenu() != null) {
					mainwindow.getMenu().refreshLanguage();
				}
			}
		});
	}

	public void closeDownloadBars() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				MinimizedWindow.closeAll();
			}
		});
	}

	public UISWTInstanceImpl getSWTPluginInstanceImpl() {
		return mainwindow.getUISWTInstanceImpl();
	}

	public void openManagerView(final DownloadManager dm) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				mainwindow.openManagerView(dm);
			}
		});
	}

	public void refreshIconBar() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				mainwindow.refreshIconBar();
			}
		});
	}

	public void removeManagerView(final DownloadManager dm) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				mainwindow.removeManagerView(dm);
			}
		});
	}

	public void showMyTracker() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				mainwindow.showMyTracker();
			}
		});
	}

	public void closePluginView(final IView view) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				mainwindow.closePluginView(view);
			}
		});
	}

	public UISWTView[] getPluginViews() {
		return mainwindow.getPluginViews();
	}

	public void openPluginView(final String sParentID, final String sViewID,
			final UISWTViewEventListener l, final Object dataSource,
			final boolean bSetFocus)
	{
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				mainwindow.openPluginView(sParentID, sViewID, l, dataSource, bSetFocus);
			}
		});
	}

	public void removePluginView(final String viewID) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (mainwindow.getMenu() != null) {
					mainwindow.getMenu().removePluginViews(viewID);
				}
			}
		});
	}

	public void setStatusText(final String string) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				mainwindow.setStatusText(string);
			}
		});
	}

	public boolean dispose(boolean for_restart, boolean close_already_in_progress)
	{
		return mainwindow.dispose(for_restart, close_already_in_progress);
	}

	public Menu getMenu(int id) {
		if (mainwindow.getMenu() != null) {
			return mainwindow.getMenu().getMenu(id);
		}
		return null;
	}
}
