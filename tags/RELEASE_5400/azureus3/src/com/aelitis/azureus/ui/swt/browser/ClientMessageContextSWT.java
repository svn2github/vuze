/**
 * Created on Jun 1, 2008
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
 
package com.aelitis.azureus.ui.swt.browser;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.widgets.Control;

import com.aelitis.azureus.core.messenger.ClientMessageContext;

/**
 * @author TuxPaper
 * @created Jun 1, 2008
 *
 */
public interface ClientMessageContextSWT
	extends ClientMessageContext
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

	/**
	 * Deregisters the browser before it's disposed.
	 * 
	 * @param event used to verify it's the correct context
	 * 
	 * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
	 */
	public abstract void widgetDisposed(DisposeEvent event);
}
