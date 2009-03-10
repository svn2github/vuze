/**
 * Created on Mar 7, 2009
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

package com.aelitis.azureus.ui.swt.devices;

import java.util.*;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.devices.DeviceManager;
import com.aelitis.azureus.core.devices.DeviceManagerFactory;
import com.aelitis.azureus.ui.swt.browser.BrowserContext;
import com.aelitis.azureus.util.ConstantsVuze;

import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.installer.*;
import org.gudy.azureus2.plugins.update.UpdateCheckInstance;

/**
 * @author TuxPaper
 * @created Mar 7, 2009
 *
 */
public class DevicesFTUX
{
	public static DevicesFTUX instance;

	Shell shell;

	private Browser browser;

	private Button checkITunes;

	private Button btnInstall;

	private Button btnCancel;

	private Composite install_area;

	/**
	 * @return
	 *
	 * @since 4.1.0.5
	 */
	private boolean isDisposed() {
		return shell.isDisposed();
	}

	/**
	 * 
	 *
	 * @since 4.1.0.5
	 */
	private void setFocus() {
		shell.forceActive();
		shell.forceFocus();
	}

	private void open() {
		// This is a simple dialog box, so instead of using SkinnedDialog, we'll
		// just built it old school
		shell = ShellFactory.createShell(Utils.findAnyShell(), SWT.DIALOG_TRIM);
		shell.setText("Turn On Device Support");

		Utils.setShellIcon(shell);

		try {
			browser = new Browser(shell, Utils.getInitialBrowserStyle(SWT.BORDER));
			new BrowserContext("DevicesFTUX", browser, null, true);
		} catch (Throwable t) {
		}

		Label lblInfo = new Label(shell, SWT.BORDER);
		lblInfo.setText("To turn on this feature, installation of extra components is required.");

		checkITunes = new Button(shell, SWT.CHECK);
		checkITunes.setSelection(true);
		checkITunes.setText("Include support for iTunes");

		btnInstall = new Button(shell, SWT.NONE);
		btnInstall.setText("Turn On");
		btnInstall.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				doInstall(checkITunes.getSelection());
			}
		});

		shell.setDefaultButton(btnInstall);

		btnCancel = new Button(shell, SWT.NONE);
		Messages.setLanguageText(btnCancel, "Button.cancel");
		btnCancel.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				shell.dispose();
			}
		});

		shell.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_ESCAPE) {
					shell.dispose();
				}
			}
		});

		install_area = new Composite(shell, SWT.NONE);
		install_area.setVisible(false);

		FormLayout formLayout = new FormLayout();
		formLayout.marginWidth = formLayout.marginHeight = 5;
		shell.setLayout(formLayout);
		FormData fd;

		fd = Utils.getFilledFormData();
		fd.bottom = new FormAttachment(btnInstall, -10);
		lblInfo.setLayoutData(fd);

		fd = Utils.getFilledFormData();
		fd.bottom = new FormAttachment(btnInstall, -10);
		browser.setLayoutData(fd);

		fd = new FormData();
		fd.bottom = new FormAttachment(100, 0);
		fd.right = new FormAttachment(100, 0);
		btnCancel.setLayoutData(fd);

		fd = new FormData();
		fd.bottom = new FormAttachment(100, 0);
		fd.right = new FormAttachment(btnCancel, -10);
		btnInstall.setLayoutData(fd);

		fd = new FormData();
		fd.bottom = new FormAttachment(btnCancel, 0, SWT.CENTER);
		fd.left = new FormAttachment(0, 0);
		fd.right = new FormAttachment(btnInstall, -10);
		checkITunes.setLayoutData(fd);

		fd = new FormData();
		fd.top = new FormAttachment(btnCancel, 0, SWT.TOP);
		fd.bottom = new FormAttachment(100, 0);
		fd.left = new FormAttachment(0, 0);
		fd.right = new FormAttachment(100, -10);
		install_area.setLayoutData(fd);

		String url = ConstantsVuze.getDefaultContentNetwork().appendURLSuffix(
				"http://vuze.com/devices.start", false, true);
		browser.setUrl(url);
		shell.setSize(600, 400);
		Utils.centreWindow(shell);
		shell.open();
	}

	/**
	 * @param selection
	 *
	 * @since 4.1.0.5
	 */
	protected void doInstall(boolean itunes) {
		List<InstallablePlugin> plugins = new ArrayList<InstallablePlugin>(2);

		final PluginInstaller installer = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInstaller();

		StandardPlugin vuze_plugin = null;

		try {
			vuze_plugin = installer.getStandardPlugin("vuzexcode");

		} catch (Throwable e) {
		}

		if (vuze_plugin != null && !vuze_plugin.isAlreadyInstalled()) {
			plugins.add(vuze_plugin);
		}

		if (itunes) {
			StandardPlugin itunes_plugin = null;

			try {
				itunes_plugin = installer.getStandardPlugin("azitunes");

			} catch (Throwable e) {
			}

			if (itunes_plugin != null && !itunes_plugin.isAlreadyInstalled()) {
				plugins.add(itunes_plugin);
			}
		}

		if (plugins.size() == 0) {
			close();
			return;
		}
		InstallablePlugin[] installablePlugins = plugins.toArray(new InstallablePlugin[0]);

		try {
			install_area.setVisible(true);
			install_area.moveAbove(null);

			Map<Integer, Object> properties = new HashMap<Integer, Object>();

			properties.put(UpdateCheckInstance.PT_UI_STYLE,
					UpdateCheckInstance.PT_UI_STYLE_SIMPLE);

			properties.put(UpdateCheckInstance.PT_UI_PARENT_SWT_COMPOSITE,
					install_area);

			properties.put(UpdateCheckInstance.PT_UI_DISABLE_ON_SUCCESS_SLIDEY, true);

			installer.install(installablePlugins, false, properties,
					new PluginInstallationListener() {
						public void completed() {
							close();
						}

						public void 
						cancelled(){
							close();
						}
						
						public void failed(PluginException e) {
							
							Debug.out(e);
							//Utils.openMessageBox(Utils.findAnyShell(), SWT.OK, "Error",
							//		e.toString());
							close();
						}
					});

		} catch (Throwable e) {

			Debug.printStackTrace(e);
		}
	}

	/**
	 * 
	 *
	 * @since 4.1.0.5
	 */
	protected void close() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (shell != null && !shell.isDisposed()) {
					shell.dispose();
				}
			}
		});
	}

	/**
	 * @return
	 *
	 * @since 4.1.0.5
	 */
	public static boolean ensureInstalled() {
		DeviceManager device_manager = DeviceManagerFactory.getSingleton();

		if (device_manager.getTranscodeManager().getProviders().length == 0) {
			if (instance == null || instance.isDisposed()) {
				instance = new DevicesFTUX();
				instance.open();
			} else {
				instance.setFocus();
			}
			return false;
		}
		return true;
	}

}
