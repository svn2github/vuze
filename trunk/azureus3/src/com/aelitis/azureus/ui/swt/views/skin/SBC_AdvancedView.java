/**
 * Created on Jul 8, 2008
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AERunnableObject;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.IMainMenu;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTInstanceImpl;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.shells.main.UIFunctionsImpl;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.utils.ColorCache;
import com.aelitis.azureus.util.ConstantsV3;

/**
 * @author TuxPaper
 * @created Jul 8, 2008
 *
 */
public class SBC_AdvancedView
	extends SkinView
	implements UIUpdatable
{
	private MainWindow oldMainWindow;


	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectInitialShow(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		createOldMainWindow();
		return null;
	}

	// @see com.aelitis.azureus.ui.swt.utils.UIUpdatable#getUpdateUIName()
	public String getUpdateUIName() {
		return "SB_AdvancedView";
	}

	// @see com.aelitis.azureus.ui.swt.utils.UIUpdatable#updateUI()
	public void updateUI() {
	}

	private org.gudy.azureus2.ui.swt.mainwindow.MainWindow createOldMainWindow() {
		final UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
		final UISWTInstance uiSWTInstance = uiFunctions.getUISWTInstance();
		if (uiSWTInstance == null) {
			System.out.println("This will end only in disaster! "
					+ Debug.getCompressedStackTrace());
		}

		return (org.gudy.azureus2.ui.swt.mainwindow.MainWindow) Utils.execSWTThreadWithObject(
				"createOldMainWindow", new AERunnableObject() {

					public Object runSupport() {

						oldMainWindow = null;
						final Composite cArea = (Composite) soMain.getControl();
						Display display = cArea.getDisplay();

						final Label lblWait = new Label(cArea, SWT.CENTER);
						FormData formData = new FormData();
						formData.left = new FormAttachment(0, 0);
						formData.right = new FormAttachment(100, 0);
						formData.top = new FormAttachment(0, 0);
						formData.bottom = new FormAttachment(100, 0);
						lblWait.setLayoutData(formData);
						lblWait.setForeground(soMain.getProperties().getColor(
								"color.row.fg"));
						Messages.setLanguageText(lblWait, "v3.MainWindow.view.wait");
						cArea.layout(true);
						lblWait.update();

						Color c = display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
						if (ConstantsV3.isUnix) {
							// Hack: For some reason, if we set the color of a Composite
							// to the widget background color, it will use the color
							// of the parent composite, even when backgroundmode is
							// INHERIT_NONE
							// The hack fix is to not use the exact color :(
							if (c.getRed() > 0) {
								c = ColorCache.getColor(display, c.getRed() - 1, c.getGreen(),
										c.getBlue());
							} else {
								c = ColorCache.getColor(display, c.getRed() + 1, c.getGreen(),
										c.getBlue());
							}
						}
						cArea.setBackground(c);

						oldMainWindow = new org.gudy.azureus2.ui.swt.mainwindow.MainWindow(
								AzureusCoreFactory.getSingleton(), null, cArea.getShell(),
								cArea, (UISWTInstanceImpl) uiSWTInstance);
						oldMainWindow.setShowMainWindow(false);
						oldMainWindow.runSupport();

						Utils.execSWTThreadLater(10, new AERunnable() {
							public void runSupport() {
								oldMainWindow.postPluginSetup(-1, 0);


								Object menu = cArea.getShell().getData("MainMenu");
								if (menu instanceof IMainMenu) {
									oldMainWindow.setMainMenu((IMainMenu) menu);
								}

								((UIFunctionsImpl) uiFunctions).oldMainWindowInitialized(oldMainWindow);

								lblWait.dispose();
								cArea.layout(true);
							}
						});

						return oldMainWindow;
					}

				}, 0);
	}

	public MainWindow getOldMainWindow() {
		return oldMainWindow;
	}
}
