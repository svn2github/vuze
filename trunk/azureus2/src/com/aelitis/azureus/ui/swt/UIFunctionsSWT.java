/*
 * Created on Jul 12, 2006 3:11:00 PM
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
package com.aelitis.azureus.ui.swt;

import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.ui.swt.plugins.UISWTPluginView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;

import com.aelitis.azureus.ui.UIFunctions;

import org.gudy.azureus2.plugins.PluginView;

/**
 * @author TuxPaper
 * @created Jul 12, 2006
 *
 */
public interface UIFunctionsSWT extends UIFunctions
{
	public Shell getMainShell();

	/**
	 * @param view
	 */
	void addPluginView(PluginView view);

	/**
	 * @param view
	 */
	void openPluginView(PluginView view);

	/**
	 * @param view
	 */
	public void openPluginView(UISWTPluginView view);

	/**
	 * @param view
	 */
	void addPluginView(UISWTPluginView view);

	/**
	 * @param view
	 */
	public void removePluginView(UISWTPluginView view);


	/**
	 * @param viewID
	 * @param l
	 */
	void addPluginView(String viewID, UISWTViewEventListener l);
}
