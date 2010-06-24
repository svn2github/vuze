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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

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
public class SWTSkinObjectTextbox
	extends SWTSkinObjectBasic
{
	private Text textWidget;
	
	private String text = "";

	public SWTSkinObjectTextbox(SWTSkin skin, SWTSkinProperties properties,
			String id, String configID, SWTSkinObject parentSkinObject) {
		super(skin, properties, id, configID, "textbox", parentSkinObject);

		Composite createOn;
		if (parent == null) {
			createOn = skin.getShell();
		} else {
			createOn = (Composite) parent.getControl();
		}
		
		int style = SWT.BORDER;
		
		String styleString = properties.getStringValue(sConfigID + ".style");
		if (styleString != null) {
			String[] styles = styleString.toLowerCase().split(",");
			Arrays.sort(styles);
			if (Arrays.binarySearch(styles, "readonly") >= 0) {
				style |= SWT.READ_ONLY;
			}
			if (Arrays.binarySearch(styles, "wrap") >= 0) {
				style |= SWT.WRAP;
			}
			if (Arrays.binarySearch(styles, "multiline") >= 0) {
				style |= SWT.MULTI | SWT.V_SCROLL;
			} else {
				style |= SWT.SINGLE;
			}
			if (Arrays.binarySearch(styles, "search") >= 0) {
				style |= SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL;
			}
		}
		
		if ((style & SWT.WRAP) == 0 && (style & SWT.MULTI) > 0) {
			style |= SWT.H_SCROLL;
		}


		textWidget = new Text(createOn, style);
		
		textWidget.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				text = textWidget.getText();
			}
		});
		
		String message = properties.getStringValue(configID + ".message", (String) null);
		if (message != null && message.length() > 0) {
			textWidget.setMessage(message);
		}

		setControl(textWidget);
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

	public void setText(final String val) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (textWidget != null && !textWidget.isDisposed()) {
					textWidget.setText(val == null ? "" : val);
					text = val;
				}
			}
		});

	}

	public String getText() {
		return text;
	}

}
