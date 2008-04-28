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


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.gudy.azureus2.core3.util.AEMonitor;

import com.aelitis.azureus.buddy.QueuedVuzeShare;

/**
 * @author TuxPaper
 * @created Apr 22, 2008
 *
 */
public class VuzeQueuedShares
{
	private static List shares = new ArrayList();
	
	private static AEMonitor shares_mon = new AEMonitor("Qd Shares");

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
		for (Iterator iter = sharesByCode.iterator(); iter.hasNext();) {
			QueuedVuzeShare share = (QueuedVuzeShare) iter.next();

			share.setPk(pkSender);
		}
	}

	/**
	 * @param pk
	 * @return 
	 *
	 * @since 3.0.5.3
	 */
	public static List getSharesByPK(String pk) {
		List results = new ArrayList();
		
		shares_mon.enter();
		try {
			for (Iterator iter = shares.iterator(); iter.hasNext();) {
				QueuedVuzeShare share = (QueuedVuzeShare) iter.next();

				if (share.getPk().equals(pk)) {
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
		return share;
	}
}
