/**
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.ui.swt.browser.listener;

import java.util.Map;

import org.eclipse.swt.browser.Browser;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.ui.swt.update.UpdateMonitor;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.browser.msg.AbstractMessageListener;
import com.aelitis.azureus.ui.swt.browser.msg.BrowserMessage;
import com.aelitis.azureus.util.MapUtils;

/**
 * @author TuxPaper
 * @created Mar 30, 2007
 *
 */
public class ConfigListener
	extends AbstractMessageListener
{
	public static final String DEFAULT_LISTENER_ID = "config";

	public static final String OP_GET_VERSION = "get-version";

	public static final String OP_NEW_INSTALL = "is-new-install";

	public static final String OP_CHECK_FOR_UPDATES = "check-for-updates";

	public ConfigListener(String id, Browser browser) {
		super(id);
	}

	/**
	 * 
	 */
	public ConfigListener(Browser browser) {
		this(DEFAULT_LISTENER_ID, browser);
	}

	// @see com.aelitis.azureus.ui.swt.browser.msg.AbstractMessageListener#handleMessage(com.aelitis.azureus.ui.swt.browser.msg.BrowserMessage)
	public void handleMessage(BrowserMessage message) {
		try {
			String opid = message.getOperationId();

			if (OP_GET_VERSION.equals(opid)) {
				Map decodedMap = message.getDecodedMap();
				String callback = MapUtils.getMapString(decodedMap, "callback", null);
				if (callback != null) {
					context.executeInBrowser(callback + "('" + Constants.AZUREUS_VERSION + "')");
				} else {
					message.debug("bad or no callback param");
				}
			} else if (OP_NEW_INSTALL.equals(opid)) {
				Map decodedMap = message.getDecodedMap();
				String callback = MapUtils.getMapString(decodedMap, "callback", null);
				if (callback != null) {
					context.executeInBrowser(callback + "(" + COConfigurationManager.isNewInstall() + ")");
				} else {
					message.debug("bad or no callback param");
				}
			} else if (OP_CHECK_FOR_UPDATES.equals(opid)) {
				checkForUpdates();
			}
		} catch (Throwable t) {
			message.debug("handle Config message", t);
		}
	}

	/**
	 * 
	 *
	 * @since 3.0.5.3
	 */
	public static void checkForUpdates() {
		UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
		if (uiFunctions != null) {
			uiFunctions.bringToFront();
		}
		UpdateMonitor.getSingleton(AzureusCoreFactory.getSingleton()).performCheck(
				true, false, false, null);
	}
}
