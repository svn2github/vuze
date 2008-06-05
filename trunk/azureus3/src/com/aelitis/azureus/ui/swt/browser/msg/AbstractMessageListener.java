/*
 * Created on Jun 29, 2006 10:16:26 PM
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package com.aelitis.azureus.ui.swt.browser.msg;

import com.aelitis.azureus.core.messenger.ClientMessageContext;

/**
 * DO NOT USE. REMOVE after 3100
 */
public abstract class AbstractMessageListener
	implements MessageListener
{
	protected ClientMessageContext context = null;

	private String id;

	public AbstractMessageListener(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public ClientMessageContext getContext() {
		return context;
	}

	public void setContext(ClientMessageContext context) {
		if (this.context == null) {
			this.context = context;
		}
	}

	public abstract void handleMessage(BrowserMessage message);
	// TODO: Change to new BrowserMessage class and make abstract after 3100 release
	public void handleMessage(
			com.aelitis.azureus.core.messenger.browser.BrowserMessage message)
	{
		handleMessage((BrowserMessage) message);
	}
	

	protected void debug(String message) {
		context.debug("[" + id + "] " + message);
	}

	public void debug(String message, Throwable t) {
		context.debug("[" + id + "] " + message, t);
	}
}
