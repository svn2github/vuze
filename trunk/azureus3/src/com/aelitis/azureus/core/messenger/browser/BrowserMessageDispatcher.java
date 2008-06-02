/**
 * Created on Jun 2, 2008
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

package com.aelitis.azureus.core.messenger.browser;

import com.aelitis.azureus.core.messenger.browser.listeners.BrowserMessageListener;

/**
 * @author TuxPaper
 * @created Jun 2, 2008
 *
 */
public interface BrowserMessageDispatcher
{

	/**
	 * Registers the given listener for the given ID.
	 * 
	 * @param id unique identifier used when dispatching messages
	 * @param listener receives messages targetted at the given ID
	 * 
	 * @throws IllegalStateException
	 *              if another listener is already registered under the same ID
	 */
	public abstract void addListener(BrowserMessageListener listener);

	/**
	 * Dispatches the given message to the appropriate listener.
	 * 
	 * @param message holds the listener ID, operation ID and parameters
	 * 
	 * @throws IllegalArgumentException
	 *              if no listener is registered with the given ID
	 */
	public abstract void dispatch(final BrowserMessage message);

	/**
	 * Returns the listener with the given ID.
	 * 
	 * @param id unique identifier of the listener to be returned
	 * @return the located listener
	 */
	public abstract BrowserMessageListener getListener(String id);

	/**
	 * Handles operations intended for the dispatcher.
	 * 
	 * @param message holds all message information
	 */
	public abstract void handleMessage(BrowserMessage message);

	/**
	 * Determines whether or not the given sequence number is still valid
	 * for the given {@link org.eclipse.swt.browser.Browser}. If the number is valid, it is stored
	 * as the last seen sequence number.
	 * 
	 * @param browser {@link org.eclipse.swt.browser.Browser} to test
	 * @param sequence the sequence number from an incoming message
	 * 
	 * @return <code>true</code> if the sequence number is valid
	 *          (greater than the last seen sequence number); <code>false</code> otherwise
	 */
	public abstract boolean isValidSequence(BrowserMessage message);

	/**
	 * Deregisters the listener with the given ID.
	 * 
	 * @param id unique identifier of the listener to be removed
	 */
	void removeListener(BrowserMessageListener listener);

	/**
	 * Deregisters the listener with the given ID.
	 * 
	 * @param id unique identifier of the listener to be removed
	 */
	void removeListener(String id);

	/**
	 * Resets the sequence number for the given {@link org.eclipse.swt.browser.Browser} to 0.
	 * 
	 * @param browser {@link org.eclipse.swt.browser.Browser} to reset
	 */
	public void resetSequence();
}