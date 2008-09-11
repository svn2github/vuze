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

package com.aelitis.azureus.util;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.selectedcontent.ISelectedContent;

/**
 * @author TuxPaper
 * @created Jun 1, 2008
 *
 */
public class DataSourceUtils
{

	public static DownloadManager getDM(Object ds) {
		try {
			if (ds instanceof DownloadManager) {
				return (DownloadManager) ds;
			} else if (ds instanceof VuzeActivitiesEntry) {
				VuzeActivitiesEntry entry = (VuzeActivitiesEntry) ds;
				DownloadManager dm = entry.getDownloadManger();
				if (dm == null) {
					String assetHash = entry.getAssetHash();
					if (assetHash != null) {
						GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
						dm = gm.getDownloadManager(new HashWrapper(Base32.decode(assetHash)));
						entry.setDownloadManager(dm);
					}
				}
				return dm;
			} else if (ds instanceof TOTorrent) {
				GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
				return gm.getDownloadManager((TOTorrent) ds);
			} else if (ds instanceof ISelectedContent) {
				return getDM(((ISelectedContent)ds).getDM()); 
			}
		} catch (Exception e) {
			Debug.printStackTrace(e);
		}
		return null;
	}

	public static TOTorrent getTorrent(Object ds) {
		if (ds instanceof TOTorrent) {
			return (TOTorrent) ds;
		}

		if (ds instanceof DownloadManager) {
			TOTorrent torrent = ((DownloadManager) ds).getTorrent();
			if (torrent != null) {
				return torrent;
			}
		}
		if (ds instanceof VuzeActivitiesEntry) {
			TOTorrent torrent = ((VuzeActivitiesEntry) ds).getTorrent();
			if (torrent == null) {
				// getDM will check hash as well
				DownloadManager dm = getDM(ds);
				if (dm != null) {
					torrent = dm.getTorrent();
				}
			}
			return torrent;
		}

		if (ds instanceof ISelectedContent) {
			return getTorrent(((ISelectedContent)ds).getDM());
		}

		return null;
	}

	/**
	 * @return
	 *
	 * @since 3.0.5.3
	 */
	public static boolean isPlatformContent(Object ds) {
		TOTorrent torrent = getTorrent(ds);
		if (torrent != null) {
			return PlatformTorrentUtils.isContent(torrent, true);
		}
		if ((ds instanceof VuzeActivitiesEntry)
				&& ((VuzeActivitiesEntry) ds).isPlatformContent()) {
			return true;
		}

		return false;
	}

	public static String getHash(Object ds) {
		try {
			if (ds instanceof DownloadManager) {
				return ((DownloadManager) ds).getTorrent().getHashWrapper().toBase32String();
			} else if (ds instanceof TOTorrent) {
				return ((TOTorrent) ds).getHashWrapper().toBase32String();
			} else if (ds instanceof VuzeActivitiesEntry) {
				VuzeActivitiesEntry entry = (VuzeActivitiesEntry) ds;
				return entry.getAssetHash();
			} else if (ds instanceof ISelectedContent) {
				return ((ISelectedContent)ds).getHash();
			}
		} catch (Exception e) {
			Debug.printStackTrace(e);
		}
		return null;
	}

	/**
	 * @param ds
	 *
	 * @since 3.1.1.1
	 */
	public static String getDownloadURL(Object ds) {
		if (ds instanceof ISelectedContent) {
			return ((ISelectedContent)ds).getDownloadURL();
		}
		return null;
	}

}
