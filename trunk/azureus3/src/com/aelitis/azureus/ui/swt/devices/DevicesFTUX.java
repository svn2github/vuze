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
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.devices.Device;
import com.aelitis.azureus.core.devices.DeviceManager;
import com.aelitis.azureus.core.devices.DeviceManagerFactory;
import com.aelitis.azureus.core.messenger.config.PlatformDevicesMessenger;
import com.aelitis.azureus.ui.swt.browser.BrowserContext;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarEntrySWT;
import com.aelitis.azureus.util.ConstantsVuze;

import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.installer.*;
import org.gudy.azureus2.plugins.ui.sidebar.SideBarVitalityImage;
import org.gudy.azureus2.plugins.update.UpdateCheckInstance;

/**
 * @author TuxPaper
 * @created Mar 7, 2009
 *
 */
public class DevicesFTUX
{
	private static final String URL_LEARN_MORE = "";

	private static final String URL_DEVICES_INFO = "http://www.vuze.com/devices.start";

	public static DevicesFTUX instance;

	Shell shell;

	private Browser browser;

	private Button checkITunes;

	private Button btnInstall;

	private Button btnCancel;

	private Composite install_area;

	private Button checkQOS;

	private Composite install_area_parent;

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
		shell.setText(MessageText.getString("devices.turnon.title"));

		Utils.setShellIcon(shell);

		try {
			browser = new Browser(shell, Utils.getInitialBrowserStyle(SWT.NONE));
			new BrowserContext("DevicesFTUX", browser, null, true);
		} catch (Throwable t) {
		}

		Label lblInfo = new Label(shell, SWT.WRAP);
		Messages.setLanguageText(lblInfo, "devices.turnon.prepageload");

		checkITunes = new Button(shell, SWT.CHECK);
		checkITunes.setSelection(true);
		Messages.setLanguageText(checkITunes, "devices.turnon.itunes");

		checkQOS = new Button(shell, SWT.CHECK);
		checkQOS.setSelection(true);
		Messages.setLanguageText(checkQOS, "devices.turnon.qos");
		
		Link lblLearnMore = new Link(shell, SWT.NONE);
		lblLearnMore.setText("<A HREF=\"" + URL_LEARN_MORE + "\">"
				+ MessageText.getString("label.learnmore") + "</A>");
		lblLearnMore.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Utils.launch(e.text);
			}
		});

		btnInstall = new Button(shell, SWT.NONE);
		Messages.setLanguageText(btnInstall, "Button.turnon");
		btnInstall.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				boolean sendQOS = checkQOS.getSelection();
				doInstall(checkITunes.getSelection(), sendQOS);
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

		install_area_parent = new Composite(shell, SWT.NONE);
		install_area_parent.setLayout(new FormLayout());
		install_area_parent.setVisible(false);

		install_area = new Composite(install_area_parent, SWT.NONE);

		FormLayout formLayout = new FormLayout();
		formLayout.marginWidth = formLayout.marginHeight = 0;
		shell.setLayout(formLayout);
		FormData fd;

		fd = Utils.getFilledFormData();
		fd.bottom = new FormAttachment(checkITunes, -5);
		fd.top = new FormAttachment(0, 8);
		fd.left = new FormAttachment(0, 8);
		fd.right = new FormAttachment(100, -8);
		lblInfo.setLayoutData(fd);

		fd = Utils.getFilledFormData();
		fd.bottom = new FormAttachment(checkITunes, -5);
		fd.width = 550;
		fd.height = 475;
		browser.setLayoutData(fd);
		
		fd = new FormData();
		fd.bottom = new FormAttachment(100, -10);
		fd.right = new FormAttachment(100, -10);
		btnCancel.setLayoutData(fd);

		fd = new FormData();
		fd.bottom = new FormAttachment(100, -10);
		fd.right = new FormAttachment(btnCancel, -12);
		btnInstall.setLayoutData(fd);

		fd = new FormData();
		fd.bottom = new FormAttachment(checkQOS, -3);
		fd.left = new FormAttachment(0, 10);
		fd.right = new FormAttachment(btnInstall, -12);
		checkITunes.setLayoutData(fd);

		fd = new FormData();
		fd.bottom = new FormAttachment(100, -5);
		fd.left = new FormAttachment(0, 10);
		checkQOS.setLayoutData(fd);
		
		fd = new FormData();
		fd.top = new FormAttachment(checkQOS, 0, SWT.CENTER);
		fd.left = new FormAttachment(checkQOS, 5);
		lblLearnMore.setLayoutData(fd);
		
		fd = new FormData();
		fd.top = new FormAttachment(browser, 0);
		fd.bottom = new FormAttachment(100, 0);
		fd.left = new FormAttachment(0, 0);
		fd.right = new FormAttachment(100, 0);
		install_area_parent.setLayoutData(fd);

		fd = new FormData();
		fd.height = btnInstall.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		fd.bottom = new FormAttachment(100, -5);
		fd.left = new FormAttachment(0, 5);
		fd.right = new FormAttachment(100, -12);
		install_area.setLayoutData(fd);

		String url = ConstantsVuze.getDefaultContentNetwork().appendURLSuffix(
				URL_DEVICES_INFO, false, true);
		browser.setUrl(url);
		
		shell.pack();
		Utils.centreWindow(shell);
		
		btnInstall.setFocus();
		shell.open();
	}

	/**
	 * @param sendQos 
	 * @param selection
	 *
	 * @since 4.1.0.5
	 */
	protected void doInstall(boolean itunes, boolean sendQOS) {
		COConfigurationManager.setParameter(PlatformDevicesMessenger.CFG_SEND_QOS,
				sendQOS);

		if (sendQOS) {
  		try {
  			PlatformDevicesMessenger.qosTurnOn(itunes);
  		} catch (Throwable ignore) {
  		}
  		try {
  			// catch any devices we found before installing additional plugins
  			DeviceManager device_manager = DeviceManagerFactory.getSingleton();
  			Device[] devices = device_manager.getDevices();
  			for (Device device : devices) {
  	  		try {
  	  			PlatformDevicesMessenger.qosFoundDevice(device);
  	  		} catch (Throwable ignore) {
  	  		}
				}
  		} catch (Throwable ignore) {
  		}
		}
		
		
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
			install_area_parent.setVisible(true);
			install_area_parent.moveAbove(null);

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

							SideBarEntrySWT sb = SideBar.getEntry( SideBar.SIDEBAR_SECTION_DEVICES );
							SideBarVitalityImage[] vitalityImages = sb.getVitalityImages();
							for (SideBarVitalityImage vi : vitalityImages) {
								if (vi.getImageID().contains("turnon")) {
									vi.setVisible(false);
								}
							}
							
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
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					if (instance == null || instance.isDisposed()) {
						instance = new DevicesFTUX();
						instance.open();
					} else {
						instance.setFocus();
					}
				}
			});
			return false;
		}
		return true;
	}

	public static void showForDebug() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (instance == null || instance.isDisposed()) {
					instance = new DevicesFTUX();
					instance.open();
				} else {
					instance.setFocus();
				}
			}
		});
	}

}
