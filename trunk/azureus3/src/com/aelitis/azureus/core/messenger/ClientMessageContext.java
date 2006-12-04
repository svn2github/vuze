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

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.widgets.Control;

import org.json.JSONString;

import com.aelitis.azureus.ui.swt.browser.msg.BrowserMessage;
import com.aelitis.azureus.ui.swt.browser.msg.MessageListener;
import com.aelitis.azureus.ui.swt.browser.txn.Transaction;
import com.aelitis.azureus.ui.swt.browser.txn.TransactionManager;

/**
 * @author TuxPaper
 * @created Oct 9, 2006
 *
 */
public interface ClientMessageContext
{

	/**
	 * Attaches this context and its message dispatcher to the browser.
	 * 
	 * @param browser the browser to be attached
	 * @param widgetWaitingIndicator Widget to be shown when browser is loading
	 */
	public abstract void registerBrowser(final Browser browser,
			Control widgetWaitingIndicator);

	/**
	 * Detaches everything from this context's browser.
	 */
	public abstract void deregisterBrowser();

	public abstract void addMessageListener(MessageListener listener);

	public abstract void removeMessageListener(String listenerId);

	public abstract void removeMessageListener(MessageListener listener);

	public abstract TransactionManager getTransactionManager();

	public abstract void registerTransactionType(String type, Class clazz);

	public abstract Transaction getTransaction(String type);

	public abstract Transaction startTransaction(String type);

	public abstract Transaction cancelTransaction(String type);

	public abstract Object getBrowserData(String key);

	public abstract void setBrowserData(String key, Object value);

	/**
	 * Sends a message to the JavaScript message dispatcher in the page.
	 * 
	 * @param key identifies the listener to receive the message
	 * @param op identifies the operation to perform
	 */
	public abstract boolean sendBrowserMessage(String key, String op);

	/**
	 * Sends a message to the JavaScript message dispatcher in the page.
	 * 
	 * @param key identifies the listener to receive the message
	 * @param op identifies the operation to perform
	 * @param params optional message parameters
	 */
	public abstract boolean sendBrowserMessage(String key, String op,
			JSONString params);

	public abstract boolean executeInBrowser(final String javascript);

	/**
	 * Handles operations intended for the context.
	 * 
	 * @param message holds all message information
	 */
	public abstract void handleMessage(BrowserMessage message);

	/**
	 * Deregisters the browser before it's disposed.
	 * 
	 * @param event used to verify it's the correct context
	 * 
	 * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
	 */
	public abstract void widgetDisposed(DisposeEvent event);

	/**
	 * Displays a debug message tagged with the context ID.
	 * 
	 * @param message sent to the debug log
	 */
	public abstract void debug(String message);

	/**
	 * Displays a debug message and exception tagged with the context ID.
	 * 
	 * @param message sent to the debug log
	 * @param t exception to log with message
	 */
	public abstract void debug(String message, Throwable t);

}