/**
 * Created on Apr 14, 2008
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

package com.aelitis.azureus.buddy.impl;

import java.util.*;

import org.gudy.azureus2.core3.util.AEDiagnostics;
import org.gudy.azureus2.core3.util.AEDiagnosticsLogger;
import org.gudy.azureus2.core3.util.AEMonitor;

import com.aelitis.azureus.buddy.VuzeBuddy;
import com.aelitis.azureus.buddy.VuzeBuddyCreator;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.plugins.net.buddy.BuddyPlugin;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginBuddy;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginListener;
import com.aelitis.azureus.ui.swt.buddy.impl.VuzeBuddySWTImpl;
import com.aelitis.azureus.util.Constants;

import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginInterface;

/**
 * @author TuxPaper
 * @created Apr 14, 2008
 *
 */
public class VuzeBuddyManager
{
	private static BuddyPlugin buddyPlugin = null;

	private static List buddyList = new ArrayList();

	private static AEMonitor buddyList_mon = new AEMonitor("buddyList");

	private static VuzeBuddyCreator vuzeBuddyCreator;

	public static void init(final VuzeBuddyCreator vuzeBuddyCreator) {
		VuzeBuddyManager.vuzeBuddyCreator = vuzeBuddyCreator;

		PluginInterface pi = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByID(
				"azbuddy");

		if (pi != null) {
			Plugin plugin = pi.getPlugin();
			if (plugin instanceof BuddyPlugin) {
				buddyPlugin = (BuddyPlugin) plugin;
				BuddyPluginListener listener = new BuddyPluginListener() {
					public void messageLogged(String str) {
					}

					public void initialised(boolean available) {
					}

					public void buddyRemoved(BuddyPluginBuddy buddy) {
						try {
							buddyList_mon.enter();

							Comparator c = new Comparator() {

								public int compare(Object arg0, Object arg1) {
									VuzeBuddy v0 = (VuzeBuddy) arg0;
									VuzeBuddy v1 = (VuzeBuddy) arg1;
									return v0.getPublicKey().compareTo(v1.getPublicKey());
								}

							};
							Collections.sort(buddyList, c);
							int i = Collections.binarySearch(buddyList, buddy.getPublicKey(),
									c);
							if (i >= 0) {
								buddyList.remove(i);
							}
						} finally {
							buddyList_mon.exit();
						}
					}

					public void buddyChanged(BuddyPluginBuddy buddy) {
					}

					public void buddyAdded(BuddyPluginBuddy buddy) {
						VuzeBuddy newBuddy;
						if (vuzeBuddyCreator == null) {
							newBuddy = new VuzeBuddyImpl(buddy.getPublicKey());
						} else {
							newBuddy = vuzeBuddyCreator.createBuddy(buddy.getPublicKey());
						}
						if (newBuddy != null) {
							newBuddy.setDisplayName(buddy.getName());
							try {
								buddyList_mon.enter();

								buddyList.add(newBuddy);
							} finally {
								buddyList_mon.exit();
							}
						}
					}
				};

				// TODO create an addListener that triggers for existing buddies
				buddyPlugin.addListener(listener);
				List buddies = buddyPlugin.getBuddies();
				for (int i = 0; i < buddies.size(); i++) {
					BuddyPluginBuddy buddy = (BuddyPluginBuddy) buddies.get(i);
					listener.buddyAdded(buddy);
				}
			}
		}
	}

	public static BuddyPlugin getBuddyPlugin() {
		return buddyPlugin;
	}

	public static List getAllVuzeBuddies() {
		try {
			buddyList_mon.enter();

			return new ArrayList(buddyList);
		} finally {
			buddyList_mon.exit();
		}
	}

	public static void setVuzeBuddyCreatorClass(VuzeBuddyCreator vuzeBuddyCreator) {
		VuzeBuddyManager.vuzeBuddyCreator = vuzeBuddyCreator;
	}

	public static void log(String s) {
		AEDiagnosticsLogger diag_logger = AEDiagnostics.getLogger("v3.PMsgr");
		diag_logger.log(s);
		if (Constants.DIAG_TO_STDOUT) {
			System.out.println(Thread.currentThread().getName() + "|"
					+ System.currentTimeMillis() + "] " + s);
		}
	}

	/**
	 * @param buddy
	 *
	 * @since 3.0.5.3
	 */
	public static void addBuddy(VuzeBuddy buddy) {
		try {
			buddyList_mon.enter();

			buddyList.add(buddy);
		} finally {
			buddyList_mon.exit();
		}
	}
}
