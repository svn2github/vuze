/**
 * Created on Dec 23, 2008
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
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;

import com.aelitis.azureus.ui.swt.skin.SWTSkin;
import com.aelitis.azureus.ui.swt.skin.SWTSkinFactory;

/**
 * @author TuxPaper
 * @created Dec 23, 2008
 *
 */
public class SkinnedDialog
{
	private final String shellSkinObjectID;

	public SkinnedDialog(String shellSkinObjectID) {
		this.shellSkinObjectID = shellSkinObjectID;

		final Shell shell = ShellFactory.createShell(SWT.DIALOG_TRIM | SWT.RESIZE);
		
		Utils.setShellIcon(shell);

		SWTSkin skin = SWTSkinFactory.getNonPersistentInstance(
				SkinnedDialog.class.getClassLoader(), "com/aelitis/azureus/ui/skin/",
				"skin3_close_notification.properties");

		skin.initialize(shell, shellSkinObjectID);

		shell.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_ESCAPE) {
					shell.close();
				}
			}
		});

		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				//skin.destroy;
			}
		});
		
		skin.layout();

		shell.open();
	}
}
