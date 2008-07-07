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

package com.aelitis.azureus.core.messenger;

import org.gudy.azureus2.core3.util.AEDiagnostics;
import org.gudy.azureus2.core3.util.AEDiagnosticsLogger;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.messenger.browser.BrowserMessageDispatcher;
import com.aelitis.azureus.core.messenger.browser.BrowserTransaction;
import com.aelitis.azureus.core.messenger.browser.BrowserTransactionManager;
import com.aelitis.azureus.core.messenger.browser.listeners.BrowserMessageListener;
import com.aelitis.azureus.util.Constants;

/**
 * @author TuxPaper
 * @created Oct 9, 2006
 *
 */
public abstract class ClientMessageContextImpl
	implements ClientMessageContext
{
	private String id;

	private BrowserMessageDispatcher dispatcher;

	private BrowserTransactionManager txnManager;

	public ClientMessageContextImpl(String id, BrowserMessageDispatcher dispatcher) {
		this.id = id;
		this.dispatcher = dispatcher;
		this.txnManager = new BrowserTransactionManager(this);
	}

	public void addMessageListener(BrowserMessageListener listener) {
		if (dispatcher != null) {
			dispatcher.addListener(listener);
		} else {
			debug("No dispatcher when trying to add MessageListener "
					+ listener.getId() + ";" + Debug.getCompressedStackTrace());
		}
	}

	public BrowserTransaction cancelTransaction(String type) {
		return txnManager.cancelTransaction(type);
	}

	public void debug(String message) {
		AEDiagnosticsLogger diag_logger = AEDiagnostics.getLogger("v3.CMsgr");
		diag_logger.log("[" + id + "] " + message);
		if (Constants.DIAG_TO_STDOUT) {
			System.out.println("[" + id + "] " + message);
		}
	}

	public void debug(String message, Throwable t) {
		AEDiagnosticsLogger diag_logger = AEDiagnostics.getLogger("v3.CMsgr");
		diag_logger.log("[" + id + "] " + message);
		diag_logger.log(t);
		if (Constants.DIAG_TO_STDOUT) {
			System.err.println("[" + id + "] " + message);
			t.printStackTrace();
		}
	}

	public BrowserTransaction getTransaction(String type) {
		return txnManager.getTransaction(type);
	}

	public BrowserTransactionManager getTransactionManager() {
		return txnManager;
	}

	public void registerTransactionType(String type, Class clazz) {
		txnManager.registerTransactionType(type, clazz);
	}

	public void removeMessageListener(String listenerId) {
		if (dispatcher != null) {
			dispatcher.removeListener(listenerId);
		} else {
			debug("No dispatcher when trying to remove MessageListener "
					+ listenerId + ";" + Debug.getCompressedStackTrace());
		}
	}

	public void removeMessageListener(BrowserMessageListener listener) {
		if (dispatcher != null) {
			dispatcher.removeListener(listener);
		} else {
			debug("No dispatcher when trying to remove MessageListener "
					+ listener.getId() + ";" + Debug.getCompressedStackTrace());
		}
	}

	public BrowserTransaction startTransaction(String type) {
		return txnManager.startTransaction(type);
	}

	public BrowserMessageDispatcher getDispatcher() {
		return dispatcher;
	}

	public String getID() {
		return id;
	}

	public void registerBrowser(Object browser, Object widgetWaitingIndicator) {
	}

	public void setMessageDispatcher(BrowserMessageDispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}
}
