/**
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
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
 */

package com.aelitis.azureus.ui.swt.browser.listener;

import java.util.Map;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.AEDiagnostics;
import org.gudy.azureus2.core3.util.AEDiagnosticsLogger;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.ui.swt.BrowserWrapper;
import org.gudy.azureus2.ui.swt.update.UpdateMonitor;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.messenger.browser.BrowserMessage;
import com.aelitis.azureus.core.messenger.browser.listeners.AbstractBrowserMessageListener;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.util.ConstantsVuze;
import com.aelitis.azureus.util.MapUtils;
import com.aelitis.net.magneturi.MagnetURIHandler;

/**
 * @author TuxPaper
 * @created Mar 30, 2007
 *
 */
public class ConfigListener
	extends AbstractBrowserMessageListener
{
	public static final String DEFAULT_LISTENER_ID = "config";

	public static final String OP_GET_VERSION = "get-version";

	public static final String OP_NEW_INSTALL = "is-new-install";

	public static final String OP_CHECK_FOR_UPDATES = "check-for-updates";
	
	public static final String OP_GET_MAGNET_PORT = "get-magnet-port";
	
	public static final String OP_LOG_DIAGS = "log-diags";

	public static final String OP_LOG = "log";

	public ConfigListener(String id, BrowserWrapper browser) {
		super(id);
	}

	/**
	 * 
	 */
	public ConfigListener(BrowserWrapper browser) {
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
				
			} else if (OP_GET_MAGNET_PORT.equals(opid)) {
				
				Map decodedMap = message.getDecodedMap();

				String callback = MapUtils.getMapString(decodedMap, "callback", null);
				
				if (callback != null) {
					
					context.executeInBrowser(callback + "('" + MagnetURIHandler.getSingleton().getPort() + "')");
					
				} else {
					
					message.debug("bad or no callback param");
				}
			}else if ( OP_LOG_DIAGS.equals( opid )){
				
				logDiagnostics();
			} else if ( OP_LOG.equals(opid)) {
				Map decodedMap = message.getDecodedMap();
				String loggerName = MapUtils.getMapString(decodedMap, "log-name",
						"browser");
				String text = MapUtils.getMapString(decodedMap, "text", "");
				
				AEDiagnosticsLogger diag_logger = AEDiagnostics.getLogger(loggerName);
				diag_logger.log(text);
				if (ConstantsVuze.DIAG_TO_STDOUT) {
					System.out.println(Thread.currentThread().getName() + "|"
							+ System.currentTimeMillis() + "] " + text);
				}
			}
		} catch (Throwable t) {
			message.debug("handle Config message", t);
		}
	}

	public static void
	logDiagnostics()
	{
		AEDiagnostics.dumpThreads();
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
		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(AzureusCore core) {
				UpdateMonitor.getSingleton(core).performCheck(true, false, false, null);
			}
		});
	}
}
