/*
 * Created on Jul 11, 2006 8:57:55 PM
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
package com.aelitis.azureus.ui.swt.search.network;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.browser.Browser;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ipc.IPCInterface;
import org.gudy.azureus2.pluginsimpl.local.ipc.IPCInterfaceImpl;

import com.aelitis.azureus.core.AzureusCore;

/**
 * @author PARG!
 * @created Jul 11, 2006
 *
 */
public class NetworkSearch
{
	private static final LogIDs LOGID = LogIDs.GUI;

	public static void search(AzureusCore core, String searchText, Browser browser) {
		new NetworkSearch(core, searchText, browser);
	}

	protected NetworkSearch(AzureusCore core, String searchText, Browser browser) {
		PluginInterface pi = core.getPluginManager().getPluginInterfaceByID(
				"azsearch");

		if (pi == null) {

			Logger.log(new LogEvent(LOGID, "Search plugin not found"));

			return;
		}

		if (!pi.isOperational()) {

			Logger.log(new LogEvent(LOGID, "Search plugin not operational"));

			return;
		}

		Map params = new HashMap();

		params.put("expression", searchText);

		params.put("swtbrowser", browser);

		try {
			IPCInterface my_ipc = new IPCInterfaceImpl(this);

			pi.getIPC().invoke("search", new Object[] { my_ipc, params
			});

		} catch (Throwable e) {

			Logger.log(new LogEvent(LOGID, "IPC to search plugin failed", e));
		}
	}

	public void searchCallback(Map params) {
		System.out.println("NetworkSearch::callback - " + params);
	}
}
