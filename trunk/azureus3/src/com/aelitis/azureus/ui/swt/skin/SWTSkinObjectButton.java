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

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;

/**
 * Native button.  For non-native, use SWTSkinButtonUtility on any SWTSkinObject
 * 
 * @author TuxPaper
 * @created Sep 21, 2008
 *
 */
public class SWTSkinObjectButton
	extends SWTSkinObjectBasic
{
	private Button button;
	private ArrayList buttonListeners = new ArrayList(1);

	public SWTSkinObjectButton(SWTSkin skin, final SWTSkinProperties properties,
			String id, String configID, SWTSkinObject parentSkinObject) {
		super(skin, properties, id, configID, "button", parentSkinObject);

		Composite createOn;
		if (parent == null) {
			createOn = skin.getShell();
		} else {
			createOn = (Composite) parent.getControl();
		}


		button = new Button(createOn, SWT.PUSH);
		
		button.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				Object[] listeners = buttonListeners.toArray();
				for (int i = 0; i < listeners.length; i++) {
					ButtonListenerAdapter l = (ButtonListenerAdapter) listeners[i];
					l.pressed(null);
					l.pressed(null, SWTSkinObjectButton.this, e.stateMask);
				}
			}
		
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		setControl(button);
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectBasic#switchSuffix(java.lang.String, int, boolean)
	public String switchSuffix(String suffix, int level, boolean walkUp, boolean walkDown) {
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

	public void addSelectionListener(ButtonListenerAdapter listener) {
		if (buttonListeners.contains(listener)) {
			return;
		}
		buttonListeners.add(listener);
	}

	/**
	 * @param text
	 *
	 * @since 3.1.1.1
	 */
	public void setText(final String text) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (button != null && !button.isDisposed()) {
					button.setText(text);
					int width = properties.getIntValue(sConfigID + ".width", -1);
					if (width == -1) {
  					int minWidth = properties.getIntValue(sConfigID + ".minwidth", -1);
  					if (minWidth >= 0) {
    					FormData fd = (FormData) button.getLayoutData();
    					if (fd == null) {
    						fd = new FormData();
    					}
    					Point size = button.computeSize(SWT.DEFAULT, SWT.DEFAULT);
    					if (size.x < minWidth) {
    						fd.width = minWidth;
    					} else {
    						fd.width = -1;
    					}
    					button.setLayoutData(fd);
    					Utils.relayout(control);
  					}
					}
				}
			}
		});
		
	}
}
