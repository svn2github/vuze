/**
 * Created on Apr 22, 2008
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

import java.io.File;
import java.util.*;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;

import com.aelitis.azureus.buddy.QueuedVuzeShare;
import com.aelitis.azureus.util.MapUtils;

/**
 * @author TuxPaper
 * @created Apr 22, 2008
 *
 */
public class VuzeQueuedShares
{
	private static List shares = new ArrayList();

	private static AEMonitor shares_mon = new AEMonitor("Qd Shares");

	private static String SAVE_FILENAME = "v3.QdShares.dat";

	private static File configDir;

	/**
	 * @param code
	 * @return
	 *
	 * @since 3.0.5.3
	 */
	public static List getSharesByCode(String code) {
		List results = new ArrayList();

		shares_mon.enter();
		try {
			for (Iterator iter = shares.iterator(); iter.hasNext();) {
				QueuedVuzeShare share = (QueuedVuzeShare) iter.next();

				if (share.getCode().equals(code)) {
					results.add(share);
				}
			}
		} finally {
			shares_mon.exit();
		}
		return results;
	}

	/**
	 * @param pkSender
	 *
	 * @since 3.0.5.3
	 */
	public static void updateSharePK(String code, String pkSender) {
		List sharesByCode = VuzeQueuedShares.getSharesByCode(code);

		log("Updating " + sharesByCode.size() + " shares with code " + code
				+ " to pk " + pkSender);

		for (Iterator iter = sharesByCode.iterator(); iter.hasNext();) {
			QueuedVuzeShare share = (QueuedVuzeShare) iter.next();

			share.setPk(pkSender);
		}
		save();
	}

	/**
	 * @param pk
	 * @return 
	 *
	 * @since 3.0.5.3
	 */
	public static List getSharesByPK(String pk) {
		List results = new ArrayList();
		
		if (pk == null) {
			return results;
		}

		shares_mon.enter();
		try {
			for (Iterator iter = shares.iterator(); iter.hasNext();) {
				QueuedVuzeShare share = (QueuedVuzeShare) iter.next();

				if (pk.equals(share.getPk())) {
					results.add(share);
				}
			}
		} finally {
			shares_mon.exit();
		}
		return results;
	}

	/**
	 * @param share
	 *
	 * @since 3.0.5.3
	 */
	public static void remove(QueuedVuzeShare share) {
		shares_mon.enter();
		try {
			shares.remove(share);
		} finally {
			shares_mon.exit();
		}
		log("Remove");
	}

	/**
	 * @param code
	 * @return 
	 *
	 * @since 3.0.5.3
	 */
	public static QueuedVuzeShare add(String code) {
		QueuedVuzeShare share = new QueuedVuzeShare();
		share.setCode(code);

		shares_mon.enter();
		try {
			shares.add(share);
		} finally {
			shares_mon.exit();
		}

		log("Share Added for code " + code);
		return share;
	}

	public static void save() {
		log("Qd Shares Save");

		Map mapSave = new HashMap();
		List storedShareList = new ArrayList();
		mapSave.put("shares", storedShareList);

		try {
			shares_mon.enter();

			for (Iterator iter = shares.iterator(); iter.hasNext();) {
				QueuedVuzeShare share = (QueuedVuzeShare) iter.next();

				if (share != null) {
					storedShareList.add(share.toMap());
				}
			}

			FileUtil.writeResilientFile(configDir, SAVE_FILENAME, mapSave, true);
		} finally {
			shares_mon.exit();
		}
	}

	private static void load() {
		Map map = FileUtil.readResilientFile(configDir, SAVE_FILENAME, true);

		List storedBuddyList = MapUtils.getMapList(map, "shares",
				Collections.EMPTY_LIST);

		shares_mon.enter();
		try {
			shares.clear();

			for (Iterator iter = storedBuddyList.iterator(); iter.hasNext();) {
				Map mapBuddy = (Map) iter.next();

				QueuedVuzeShare share = new QueuedVuzeShare(mapBuddy);

				shares.add(share);
			}
		} finally {
			shares_mon.exit();
		}

		log("Qd Shares Load.  Size=" + shares.size());
	}

	/**
	 * 
	 *
	 * @param configDir 
	 * @since 3.0.5.3
	 */
	public static void init(File configDir) {
		VuzeQueuedShares.configDir = configDir;
		try {
			load();
		} catch (Exception e) {
			Debug.out(e);
		}
	}

	private static void log(String s) {
		VuzeBuddyManager.log("[Qd Shares] " + s);
	}
}
