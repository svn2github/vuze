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


import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;

import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;

import com.aelitis.azureus.core.devices.DeviceOfflineDownloader;
import com.aelitis.azureus.ui.swt.browser.BrowserContext;
import com.aelitis.azureus.util.ConstantsVuze;

/**
 * @author TuxPaper
 * @created Mar 7, 2009
 *
 */
public class DevicesODFTUX
{
	private DeviceOfflineDownloader		device;
	
	private Shell shell;

	private Button btnInstall;

	private Button btnCancel;

	protected
	DevicesODFTUX(
		DeviceOfflineDownloader		_device )
	{
		device	= _device;
		
		Utils.execSWTThread(
			new AERunnable() 
			{
				public void
				runSupport()
				{
					open();
				}
			});
	}


	private void 
	open() 
	{
		// This is a simple dialog box, so instead of using SkinnedDialog, we'll
		// just built it old school
		shell = ShellFactory.createShell(Utils.findAnyShell(), SWT.DIALOG_TRIM);
		shell.setText(MessageText.getString("device.od.turnon.title"));

		Utils.setShellIcon(shell);

		Composite stuff = new Composite( shell, SWT.NULL );
		stuff.setSize( 300, 200 );
		stuff.setBackground( Colors.white );
		
		Label lblInfo = new Label(shell, SWT.NULL);
		Messages.setLanguageText(lblInfo, "!Discovered device '" + device.getName() + "'!" );

		btnInstall = new Button(shell, SWT.NONE);
		Messages.setLanguageText(btnInstall, "Button.turnon");
		btnInstall.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				device.setEnabled( true );
				shell.dispose();
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
		
		FormLayout formLayout = new FormLayout();
		formLayout.marginWidth = formLayout.marginHeight = 0;
		shell.setLayout(formLayout);
		FormData fd;

		fd = Utils.getFilledFormData();
		fd.top = new FormAttachment(stuff, +10);
		fd.bottom = new FormAttachment(100, 0);
		fd.left = new FormAttachment(0, 8);
		fd.right = new FormAttachment(btnInstall, -10);
		lblInfo.setLayoutData(fd);
		
		fd = new FormData();
		fd.top = new FormAttachment(stuff, +10 );
		fd.bottom = new FormAttachment(100, -10);
		fd.right = new FormAttachment(100, -10);
		btnCancel.setLayoutData(fd);

		fd = new FormData();
		fd.top = new FormAttachment(stuff, +10 );
		fd.bottom = new FormAttachment(100, -10);
		fd.right = new FormAttachment(btnCancel, -12);
		btnInstall.setLayoutData(fd);
		
		fd = new FormData();
		fd.top = new FormAttachment(0, 0);
		fd.left = new FormAttachment(0, 0);
		fd.right = new FormAttachment(100, 0 );
		stuff.setLayoutData(fd);
				
		shell.pack();
		Utils.centreWindow(shell);
		
		btnInstall.setFocus();
		shell.open();
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
}
