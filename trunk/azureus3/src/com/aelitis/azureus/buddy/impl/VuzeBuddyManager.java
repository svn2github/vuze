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

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.buddy.VuzeBuddy;
import com.aelitis.azureus.buddy.VuzeBuddyCreator;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.plugins.net.buddy.*;
import com.aelitis.azureus.plugins.net.buddy.BuddyPlugin.cryptoResult;
import com.aelitis.azureus.util.*;
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

	static Comparator buddyComparePK;

	static {
		buddyComparePK = new Comparator() {
			public int compare(Object arg0, Object arg1) {
				try {
					String v0 = (arg0 instanceof VuzeBuddy)
							? ((VuzeBuddy) arg0).getPublicKey() : (String) arg0;
					String v1 = (arg1 instanceof VuzeBuddy)
							? ((VuzeBuddy) arg1).getPublicKey() : (String) arg1;
					if (v0 == null) {
						v0 = "";
					}
					if (v1 == null) {
						v1 = null;
					}
					return v0.compareTo(v1);
				} catch (Exception e) {
					Debug.out(e);
				}
				return 0;
			}

		};
	}

	public static void init(final VuzeBuddyCreator vuzeBuddyCreator) {
		VuzeBuddyManager.vuzeBuddyCreator = vuzeBuddyCreator;

		try {
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

								String pk = buddy.getPublicKey();

								Collections.sort(buddyList, buddyComparePK);
								int i = Collections.binarySearch(buddyList, pk, buddyComparePK);
								if (i >= 0) {
									buddyList.remove(i);
								}
							} finally {
								buddyList_mon.exit();
							}
						}

						public void buddyChanged(BuddyPluginBuddy buddy) {
							try {
								buddyList_mon.enter();

								String pk = buddy.getPublicKey();

								Collections.sort(buddyList, buddyComparePK);
								int i = Collections.binarySearch(buddyList, pk, buddyComparePK);
								if (i >= 0) {
									try {
										VuzeBuddy vuzeBuddy = (VuzeBuddy) buddyList.get(i);
										vuzeBuddy.setDisplayName(buddy.getNickName());
									} catch (Exception e) {
										Debug.out(e);
									}
								}
							} finally {
								buddyList_mon.exit();
							}
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

					BuddyPluginBuddyRequestListener requestListener = new BuddyPluginBuddyRequestListener() {

						public Map requestReceived(BuddyPluginBuddy from_buddy,
								int subsystem, Map request) throws BuddyPluginException {
							if (subsystem != BuddyPlugin.SUBSYSTEM_AZ3) {
								return null;
							}

							try {
								String pk = from_buddy.getPublicKey();
								Collections.sort(buddyList, buddyComparePK);
								int i = Collections.binarySearch(buddyList, pk, buddyComparePK);
								if (i < 0) {
									return null; // we aren't a buddy
								}

								String mt = MapUtils.getMapString(request, "VuzeMessageType",
										"");
								if (mt.equals("ActivityEntry")) {
									byte[] encPayload = MapUtils.getMapByteArray(request,
											"ActivityEntry", null);
									if (encPayload == null) {
										return null;
									}
									cryptoResult result = from_buddy.decrypt(encPayload);
									byte[] payload = result.getPayload();
									Map mapEntry = BDecoder.decode(payload);
									VuzeActivitiesEntry entry = VuzeActivitiesManager.createEntryFromMap(mapEntry);
									if (entry != null) {
										VuzeActivitiesManager.addEntries(new VuzeActivitiesEntry[] {
											entry
										});
									}
								}
							} catch (Exception e) {
								Debug.out(e);
							}

							return null;
						}

						public void pendingMessages(BuddyPluginBuddy[] from_buddies) {
						}

					};

					// TODO create an addListener that triggers for existing buddies
					buddyPlugin.addListener(listener);
					buddyPlugin.addRequestListener(requestListener);
					List buddies = buddyPlugin.getBuddies();
					for (int i = 0; i < buddies.size(); i++) {
						BuddyPluginBuddy buddy = (BuddyPluginBuddy) buddies.get(i);
						listener.buddyAdded(buddy);
					}
				}
			}
		} catch (Throwable t) {
			Debug.out(t);
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
