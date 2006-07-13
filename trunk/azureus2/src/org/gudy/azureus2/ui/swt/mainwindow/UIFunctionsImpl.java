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

import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.ui.swt.plugins.UISWTPluginView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.views.ConfigView;

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
		mainwindow.setVisible(true);
	}

	// UIFunctions
	public void addPluginView(PluginView view) {
		if (mainwindow.getMenu() != null) {
			mainwindow.getMenu().addPluginView(view);
		}
	}

	public void openPluginView(PluginView view) {
		mainwindow.openPluginView(view, view.getPluginViewName());
	}

	public ConfigView showConfig() {
		return mainwindow.showConfig();
	}

	public void showStats() {
		mainwindow.showStats();
	}

	public void showStatsDHT() {
		mainwindow.showStatsDHT();
	}

	public void showStatsTransfers() {
		mainwindow.showStatsTransfers();
	}

	public Shell getMainShell() {
		return mainwindow.getShell();
	}

	public void addPluginView(UISWTPluginView view) {
		if (mainwindow.getMenu() != null) {
			mainwindow.getMenu().addPluginView(view);
		}
	}

	public void openPluginView(UISWTPluginView view) {
		mainwindow.openPluginView(view);
	}

	public void removePluginView(UISWTPluginView view) {
		if (mainwindow.getMenu() != null) {
			mainwindow.getMenu().removePluginView(view);
		}
	}

	public boolean showConfig(String string) {
		return mainwindow.showConfig(string);
	}

	public void addPluginView(String viewID, UISWTViewEventListener l) {
		if (mainwindow.getMenu() != null) {
			mainwindow.getMenu().addPluginView(viewID, l);
		}
	}
	
	public boolean requestShutdown() {
		return mainwindow.destroyRequest();
	}

	public void refreshLanguage() {
		if (mainwindow.getMenu() != null) {
			mainwindow.getMenu().refreshLanguage();
		}
	}
}
