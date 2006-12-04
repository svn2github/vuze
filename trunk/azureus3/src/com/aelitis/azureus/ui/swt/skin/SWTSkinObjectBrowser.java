/**
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.ui.swt.skin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.messenger.ClientMessageContext;
import com.aelitis.azureus.ui.swt.browser.BrowserContext;
import com.aelitis.azureus.ui.swt.browser.listener.DisplayListener;
import com.aelitis.azureus.ui.swt.browser.listener.TorrentListener;

/**
 * @author TuxPaper
 * @created Oct 9, 2006
 *
 */
public class SWTSkinObjectBrowser extends SWTSkinObjectBasic
{

	private Browser browser;

	private Composite cArea;
	
	private String sStartURL;

	/**
	 * @param skin
	 * @param properties
	 * @param sID
	 * @param sConfigID
	 * @param type
	 * @param parent
	 */
	public SWTSkinObjectBrowser(SWTSkin skin, SWTSkinProperties properties,
			String sID, String sConfigID, SWTSkinObject parent) {
		super(skin, properties, sID, sConfigID, "browser", parent);

		AzureusCore core = AzureusCoreFactory.getSingleton();

		cArea = (Composite) parent.getControl();

		browser = new Browser(cArea, SWT.NONE);

		Control widgetIndicator = null;
		String sIndicatorWidgetID = properties.getStringValue(sConfigID
				+ ".indicator");
		if (sIndicatorWidgetID != null) {
			SWTSkinObject skinObjectIndicator = skin.getSkinObjectByID(sIndicatorWidgetID);
			if (skinObjectIndicator != null) {
				widgetIndicator = skinObjectIndicator.getControl();
			}
		}

		final ClientMessageContext context = new BrowserContext(sID, browser,
				widgetIndicator);
		context.addMessageListener(new TorrentListener(core));
		context.addMessageListener(new DisplayListener(browser));

		setControl(browser);
	}

	public Browser getBrowser() {
		return browser;
	}

	public void setURL(String url) {
		if (url == null) {
			browser.setText("");
		} else {
			browser.setUrl(url);
		}
		if (sStartURL == null) {
			sStartURL = url;
			browser.setData("StartURL", url);
		}
		System.out.println("Set URL: " + url);
	}

	public void restart() {
		setURL(sStartURL);
	}

	/**
	 * 
	 */
	public void layout() {
		cArea.layout();
	}
}
