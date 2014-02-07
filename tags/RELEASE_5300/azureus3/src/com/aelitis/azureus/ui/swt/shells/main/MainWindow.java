/*
 * Created on Sep 13, 2012
 * Created by Paul Gardner
 * 
 * Copyright 2012 Vuze, Inc.  All rights reserved.
 * 
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */

package com.aelitis.azureus.ui.swt.shells.main;

import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.ui.swt.mainwindow.IMainMenu;
import org.gudy.azureus2.ui.swt.mainwindow.IMainStatusBar;
import org.gudy.azureus2.ui.swt.mainwindow.IMainWindow;

import com.aelitis.azureus.core.AzureusCore;

import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTInstanceImpl;


public interface
MainWindow
	extends IMainWindow
{
	public void
	init(
		AzureusCore		core );
	
	
	public Shell
	getShell();
	
	public IMainMenu 
	getMainMenu();
	
	public IMainStatusBar 
	getMainStatusBar();
	
	public boolean
	isReady();
	
	public void 
	setVisible(
		boolean visible, 
		boolean tryTricks );
	
	public UISWTInstanceImpl
	getUISWTInstanceImpl();

	public void 
	setSelectedLanguageItem();
	
	public void
	setHideAll(
		boolean	hide );
	
	public boolean 
	dispose(
		boolean for_restart,
		boolean close_already_in_progress );
}