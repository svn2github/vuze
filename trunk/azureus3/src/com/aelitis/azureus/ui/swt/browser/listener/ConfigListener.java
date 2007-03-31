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

import org.eclipse.swt.browser.Browser;

import org.gudy.azureus2.core3.util.AEDiagnostics;
import org.gudy.azureus2.core3.util.AEDiagnosticsLogger;
import org.gudy.azureus2.core3.util.Constants;
import org.json.JSONObject;

import com.aelitis.azureus.ui.swt.browser.msg.AbstractMessageListener;
import com.aelitis.azureus.ui.swt.browser.msg.BrowserMessage;
import com.aelitis.azureus.util.JSONUtils;

/**
 * @author TuxPaper
 * @created Mar 30, 2007
 *
 */
public class ConfigListener
	extends AbstractMessageListener
{
	private static final String DEFAULT_LISTENER_ID = "config";

	public static final String OP_GET_VERSION = "get-version";

	private Browser browser;

	public ConfigListener(String id, Browser browser) {
		super(id);
		this.browser = browser;
	}

	/**
	 * 
	 */
	public ConfigListener(Browser browser) {
		this(DEFAULT_LISTENER_ID, browser);
	}

	// @see com.aelitis.azureus.ui.swt.browser.msg.AbstractMessageListener#handleMessage(com.aelitis.azureus.ui.swt.browser.msg.BrowserMessage)
	public void handleMessage(BrowserMessage message) {
		String opid = message.getOperationId();
		
		if (OP_GET_VERSION.equals(opid)) {
			JSONObject decodedObject = message.getDecodedObject();
			String callback = JSONUtils.getJSONString(decodedObject, "callback", null);
			if (callback != null && browser != null) {
				browser.execute(callback + "('" + Constants.AZUREUS_VERSION + "')");
			} else {
				debug(message, "bad or no callback param");
			}
		}

	}

	public void debug(BrowserMessage bm, String message) {
		AEDiagnosticsLogger diag_logger = AEDiagnostics.getLogger("v3.CMsgr");
		String out = "[" + bm.getListenerId() + ":" + bm.getOperationId() + "] "
				+ message;
		diag_logger.log(out);
		if (com.aelitis.azureus.util.Constants.DIAG_TO_STDOUT) {
			System.out.println(out);
		}
	}
}
