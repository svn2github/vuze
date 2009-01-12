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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;

import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
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

	private Shell shell;

	private SWTSkin skin;

	private List<SkinnedDialogClosedListener> closeListeners = new CopyOnWriteArrayList<SkinnedDialogClosedListener>();

	public SkinnedDialog(String skinFile, String shellSkinObjectID) {
		this.shellSkinObjectID = shellSkinObjectID;

		Shell mainShell = UIFunctionsManagerSWT.getUIFunctionsSWT().getMainShell();
		shell = ShellFactory.createShell(mainShell, SWT.DIALOG_TRIM | SWT.RESIZE);

		Utils.setShellIcon(shell);

		skin = SWTSkinFactory.getNonPersistentInstance(
				SkinnedDialog.class.getClassLoader(), "com/aelitis/azureus/ui/skin/",
				skinFile + ".properties");

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
				for (SkinnedDialogClosedListener l : closeListeners) {
					try {
						l.skinDialogClosed(SkinnedDialog.this);
					} catch (Exception e2) {
						Debug.out(e2);
					}
				}
			}
		});

		skin.layout();

		Utils.centerWindowRelativeTo(shell, mainShell);
	}

	public void open() {
		shell.open();
	}

	public SWTSkin getSkin() {
		return skin;
	}

	/**
	 * 
	 *
	 * @since 4.0.0.5
	 */
	public void close() {
		shell.close();
	}

	public void addCloseListener(SkinnedDialogClosedListener l) {
		closeListeners.add(l);
	}

	public interface SkinnedDialogClosedListener
	{
		public void skinDialogClosed(SkinnedDialog dialog);
	}

	/**
	 * @param string
	 *
	 * @since 4.0.0.5
	 */
	public void setTitle(String string) {
		if (shell != null && !shell.isDisposed()) {
			shell.setText(string);
		}
	}
}
