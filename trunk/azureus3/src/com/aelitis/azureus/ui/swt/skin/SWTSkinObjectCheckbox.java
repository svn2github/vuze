/**
 * Created on Sep 21, 2008
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

package com.aelitis.azureus.ui.swt.skin;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;

/**
 * Native checkbox
 * 
 * @author TuxPaper
 * @created Dec 24, 2008
 *
 */
public class SWTSkinObjectCheckbox
	extends SWTSkinObjectBasic
{
	private Button button;

	// stored so we can access it after button is disposed, and so we can
	// retrieve without being on SWT thread
	private boolean checked;

	private List<SWTSkinCheckboxListener> buttonListeners = new CopyOnWriteArrayList();

	public SWTSkinObjectCheckbox(SWTSkin skin, SWTSkinProperties properties,
			String id, String configID, SWTSkinObject parentSkinObject) {
		super(skin, properties, id, configID, "checkbox", parentSkinObject);

		Composite createOn;
		if (parent == null) {
			createOn = skin.getShell();
		} else {
			createOn = (Composite) parent.getControl();
		}
		
		// WinXP Classic Theme will not bring though parent's background image
		// without FORCEing the background mode
		createOn.setBackgroundMode(SWT.INHERIT_FORCE);

		button = new Button(createOn, SWT.CHECK | SWT.WRAP);
		checked = false;

		button.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				checked = button.getSelection();
				for (SWTSkinCheckboxListener l : buttonListeners) {
					try {
						l.checkboxChanged(SWTSkinObjectCheckbox.this, checked);
					} catch (Exception ex) {
						Debug.out(ex);
					}
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		setControl(button);
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectBasic#switchSuffix(java.lang.String, int, boolean)
	public String switchSuffix(String suffix, int level, boolean walkUp,
			boolean walkDown) {
		suffix = super.switchSuffix(suffix, level, walkUp, walkDown);

		if (suffix == null) {
			return null;
		}

		String sPrefix = sConfigID + ".text";
		String text = properties.getStringValue(sPrefix + suffix);
		if (text != null) {
			setText(text);
		}

		return suffix;
	}

	public void addSelectionListener(SWTSkinCheckboxListener listener) {
		if (buttonListeners.contains(listener)) {
			return;
		}
		buttonListeners.add(listener);
	}

	public void setText(final String text) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (button != null && !button.isDisposed()) {
					button.setText(text);
				}
			}
		});

	}

	public boolean isChecked() {
		return checked;
	}
	
	public void setChecked(boolean b) {
		checked = b;
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (button != null && !button.isDisposed()) {
					button.setSelection(checked);
				}
			}
		});
	}
}
