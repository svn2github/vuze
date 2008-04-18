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

import java.io.UnsupportedEncodingException;
import java.util.*;

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.buddy.VuzeBuddy;
import com.aelitis.azureus.buddy.VuzeBuddyCreator;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.messenger.config.PlatformRelayMessenger;
import com.aelitis.azureus.core.messenger.config.VuzeRelayListener;
import com.aelitis.azureus.plugins.net.buddy.*;
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
	protected static final boolean ALLOW_ONLY_AZ3 = false;

	private static BuddyPlugin buddyPlugin = null;

	private static List buddyList = new ArrayList();

	private static AEMonitor buddyList_mon = new AEMonitor("buddyList");

	private static Map pkList = new HashMap();

	private static VuzeBuddyCreator vuzeBuddyCreator;

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
							if (!canHandleBuddy(buddy)) {
								return;
							}
							try {
								buddyList_mon.enter();

								String pk = buddy.getPublicKey();

								VuzeBuddy vuzeBuddy = (VuzeBuddy) pkList.remove(pk);
								if (vuzeBuddy != null) {
									vuzeBuddy.removePublicKey(pk);
									if (vuzeBuddy.getPublicKeys().length == 0) {
										buddyList.remove(buddy);
									}
								}
							} finally {
								buddyList_mon.exit();
							}
						}

						public void buddyChanged(BuddyPluginBuddy buddy) {
							if (!canHandleBuddy(buddy)) {
								return;
							}

							try {
								buddyList_mon.enter();

								String pk = buddy.getPublicKey();

								VuzeBuddy vuzeBuddy = (VuzeBuddy) pkList.get(pk);
								if (vuzeBuddy != null) {
									vuzeBuddy.setDisplayName(buddy.getNickName());
								}
							} finally {
								buddyList_mon.exit();
							}
						}

						public void buddyAdded(BuddyPluginBuddy buddy) {
							if (!canHandleBuddy(buddy)) {
								return;
							}

							VuzeBuddy newBuddy;
							if (vuzeBuddyCreator == null) {
								newBuddy = new VuzeBuddyImpl(buddy.getPublicKey());
							} else {
								newBuddy = vuzeBuddyCreator.createBuddy(buddy.getPublicKey());
							}
							if (newBuddy != null) {
								newBuddy.setDisplayName(buddy.getName());
								addBuddy(newBuddy);
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

								VuzeBuddy vuzeBuddy = (VuzeBuddy) pkList.get(pk);
								if (vuzeBuddy != null) {
									String mt = MapUtils.getMapString(request, "VuzeMessageType",
											"");
									if (mt.equals("ActivityEntry")) {
										Map mapEntry = (Map) MapUtils.getMapObject(request,
												"ActivityEntry", new HashMap(), Map.class);
										VuzeActivitiesEntry entry = VuzeActivitiesManager.createEntryFromMap(mapEntry);
										if (entry != null) {
											VuzeActivitiesManager.addEntries(new VuzeActivitiesEntry[] {
												entry
											});
										}
									}
								}
							} catch (Exception e) {
								Debug.out(e);
							}

							return null;
						}

						public void pendingMessages(BuddyPluginBuddy[] from_buddies) {
							for (int i = 0; i < from_buddies.length; i++) {
								BuddyPluginBuddy pluginBuddy = from_buddies[i];
								
								String pk = pluginBuddy.getPublicKey();
								VuzeBuddy vuzeBuddy = getBuddyByPK(pk);
								if (vuzeBuddy != null) {
									PlatformRelayMessenger.fetch(0);
								}
							}
						}

					};

					// TODO create an addListener that triggers for existing buddies
					buddyPlugin.addListener(listener);
					buddyPlugin.addRequestListener(requestListener);
					List buddies = buddyPlugin.getBuddies();
					for (int i = 0; i < buddies.size(); i++) {
						BuddyPluginBuddy buddy = (BuddyPluginBuddy) buddies.get(i);
						if (canHandleBuddy(buddy)) {
							listener.buddyAdded(buddy);
						}
					}
				}
			}

			VuzeRelayListener vuzeRelayListener = new VuzeRelayListener() {
				public void newRelayServerPayLoad(VuzeBuddy sender, byte[] payload) {
					try {
						String s = new String(payload, "utf-8");
						Map mapPayLoad = JSONUtils.decodeJSON(s);

						String mt = MapUtils.getMapString(mapPayLoad, "VuzeMessageType", "");

						if (mt.equals("ActivityEntry")) {
							Map mapEntry = (Map) MapUtils.getMapObject(mapPayLoad,
									"ActivityEntry", new HashMap(), Map.class);
							VuzeActivitiesEntry entry = VuzeActivitiesManager.createEntryFromMap(mapEntry);
							if (entry != null) {
								VuzeActivitiesManager.addEntries(new VuzeActivitiesEntry[] {
									entry
								});
							}
						} else if (mt.equals("BuddySync")) {
							// TODO buddy sync
						}
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			};

			PlatformRelayMessenger.addRelayServerListener(vuzeRelayListener);
		} catch (Throwable t) {
			Debug.out(t);
		}
	}

	/**
	 * @param buddy
	 * @return
	 *
	 * @since 3.0.5.3
	 */
	protected static boolean canHandleBuddy(BuddyPluginBuddy buddy) {
		if (buddy == null) {
			return false;
		}
		if (ALLOW_ONLY_AZ3) {
			int subsystem = buddy.getSubsystem();
			return subsystem == BuddyPlugin.SUBSYSTEM_AZ3;
		}
		
		return true;
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

	public static VuzeBuddy getBuddyByPK(String pk) {
		try {
			buddyList_mon.enter();

			return (VuzeBuddy) pkList.get(pk);
		} finally {
			buddyList_mon.exit();
		}
	}

	public static VuzeBuddy getBuddyByLoginID(String loginID) {
		if (loginID == null) {
			return null;
		}
		loginID = loginID.toLowerCase();
		
		try {
			buddyList_mon.enter();

			for (Iterator iter = buddyList.iterator(); iter.hasNext();) {
				VuzeBuddy buddy = (VuzeBuddy) iter.next();
				
				String loginID2 = buddy.getLoginID();
				
				if (loginID2 != null && loginID.equals(loginID2.toLowerCase())) {
					return buddy;
				}
			}
		} finally {
			buddyList_mon.exit();
		}
		return null;
	}

	public static void setVuzeBuddyCreatorClass(VuzeBuddyCreator vuzeBuddyCreator) {
		VuzeBuddyManager.vuzeBuddyCreator = vuzeBuddyCreator;
	}

	public static void log(String s) {
		AEDiagnosticsLogger diag_logger = AEDiagnostics.getLogger("v3.Buddy");
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

			if (!buddyList.contains(buddy)) {
				buddyList.add(buddy);
			}
			
			String[] publicKeys = buddy.getPublicKeys();
			for (int i = 0; i < publicKeys.length; i++) {
				pkList.put(publicKeys[i], buddy);
			}

		} finally {
			buddyList_mon.exit();
		}
	}

	public static VuzeBuddy createNewBuddy(String pk) {
		if (buddyPlugin == null) {
			return null;
		}
		VuzeBuddy newBuddy;
		if (vuzeBuddyCreator == null) {
			newBuddy = new VuzeBuddyImpl(pk);
		} else {
			newBuddy = vuzeBuddyCreator.createBuddy(pk);
		}
		
		if (newBuddy == null) {
			return null;
		}
		
		BuddyPluginBuddy pluginBuddy = buddyPlugin.getBuddyFromPublicKey(pk);
		if (pluginBuddy == null) {
			pluginBuddy = buddyPlugin.addBuddy(pk, BuddyPlugin.SUBSYSTEM_AZ3);
		} else {
			pluginBuddy.setSubsystem(BuddyPlugin.SUBSYSTEM_AZ3);
		}
		
		addBuddy(newBuddy);
		
		return newBuddy;
	}

	/**
	 * @param mapNewBuddy
	 * @return 
	 *
	 * @since 3.0.5.3
	 */
	public static VuzeBuddy createNewBuddy(Map mapNewBuddy) {
		if (buddyPlugin == null) {
			return null;
		}
		VuzeBuddy newBuddy;
		if (vuzeBuddyCreator == null) {
			newBuddy = new VuzeBuddyImpl();
		} else {
			newBuddy = vuzeBuddyCreator.createBuddy();
		}
		
		if (newBuddy == null) {
			return null;
		}
		
		newBuddy.loadFromMap(mapNewBuddy);
		
		String[] publicKeys = newBuddy.getPublicKeys();
		for (int i = 0; i < publicKeys.length; i++) {
			String pk = publicKeys[i];

			BuddyPluginBuddy pluginBuddy = buddyPlugin.getBuddyFromPublicKey(pk);
			if (pluginBuddy == null) {
				pluginBuddy = buddyPlugin.addBuddy(pk, BuddyPlugin.SUBSYSTEM_AZ3);
			} else {
				pluginBuddy.setSubsystem(BuddyPlugin.SUBSYSTEM_AZ3);
			}
		}

		addBuddy(newBuddy);
		
		return newBuddy;
	}

	/**
	 * @param loginID
	 *
	 * @since 3.0.5.3
	 */
	public static void removeBuddy(VuzeBuddy buddy) {
		if (buddy == null) {
			return;
		}

		try {
			buddyList_mon.enter();

			buddyList.remove(buddy);
			
		} finally {
			buddyList_mon.exit();
		}
	}

	/**
	 * @param updateTime
	 *
	 * @since 3.0.5.3
	 */
	public static void removeBuddiesOlderThan(long updateTime) {
		try {
			buddyList_mon.enter();

			for (Iterator iter = buddyList.iterator(); iter.hasNext();) {
				VuzeBuddy buddy = (VuzeBuddy) iter.next();
				
				if (buddy.getLastUpdated() < updateTime) {
					iter.remove();
				}
			}
			
		} finally {
			buddyList_mon.exit();
		}
	}
}
