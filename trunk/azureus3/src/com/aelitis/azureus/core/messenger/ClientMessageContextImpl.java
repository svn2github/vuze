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

import com.aelitis.azureus.ui.swt.browser.msg.MessageDispatcher;
import com.aelitis.azureus.ui.swt.browser.msg.MessageListener;
import com.aelitis.azureus.ui.swt.browser.txn.Transaction;
import com.aelitis.azureus.ui.swt.browser.txn.TransactionManager;
import com.aelitis.azureus.util.Constants;

/**
 * @author TuxPaper
 * @created Oct 9, 2006
 *
 */
public abstract class ClientMessageContextImpl implements ClientMessageContext
{
	private String id;

	private MessageDispatcher dispatcher;

	private TransactionManager txnManager;

	public ClientMessageContextImpl(String id) {
		this.id = id;
		this.dispatcher = new MessageDispatcher(this);
		this.txnManager = new TransactionManager(this);
	}

	public void addMessageListener(MessageListener listener) {
		dispatcher.addListener(listener);
	}

	public Transaction cancelTransaction(String type) {
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

	public Transaction getTransaction(String type) {
		return txnManager.getTransaction(type);
	}

	public TransactionManager getTransactionManager() {
		return txnManager;
	}

	public void registerTransactionType(String type, Class clazz) {
		txnManager.registerTransactionType(type, clazz);
	}

	public void removeMessageListener(String listenerId) {
		dispatcher.removeListener(listenerId);
	}

	public void removeMessageListener(MessageListener listener) {
		dispatcher.removeListener(listener);
	}

	public Transaction startTransaction(String type) {
		return txnManager.startTransaction(type);
	}
	
	public MessageDispatcher getMessageDispatcher() {
		return dispatcher;
	}

	public String getID() {
		return id;
	}
}
