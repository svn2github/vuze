/**
 * Created on Jul 2, 2008
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

package com.aelitis.azureus.ui.swt.views.skin;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;

/**
 * @author TuxPaper
 * @created Jul 2, 2008
 *
 */
public class SBC_LibraryView
	extends SkinView
{
	private final static String ID = "library-list";

	private final static int MODE_BIGTABLE = 0;

	private final static int MODE_SMALLTABLE = 1;

	private final static int MODE_OLDTABLE = 2;

	public static final int TORRENTS_ALL = 0;

	public static final int TORRENTS_COMPLETE = 1;

	public static final int TORRENTS_INCOMPLETE = 2;

	private final static String[] modeViewIDs = {
		SkinConstants.VIEWID_SIDEBAR_LIBRARY_BIG,
		SkinConstants.VIEWID_SIDEBAR_LIBRARY_SMALL,
		SkinConstants.VIEWID_SIDEBAR_LIBRARY_OLD,
	};

	private final static String[] modeIDs = {
		"library.table.big",
		"library.table.small",
		"library.table.old",
	};

	private int viewMode;

	private SWTSkinButtonUtility btnSmallTable;

	private SWTSkinButtonUtility btnBigTable;

	private SWTSkinObject soListArea;

	private SWTSkinButtonUtility btnOldTable;

	private int torrentFilterMode = TORRENTS_ALL;

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#showSupport(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		String torrentFilter = skinObject.getSkinObjectID();
		if (torrentFilter.equalsIgnoreCase("LibraryDL_SB")) {
			torrentFilterMode = TORRENTS_INCOMPLETE;
		} else if (torrentFilter.equalsIgnoreCase("LibraryCD_SB")) {
			torrentFilterMode = TORRENTS_COMPLETE;
		}

		soListArea = getSkinObject(ID + "-area");

		soListArea.getControl().setData("TorrentFilterMode",
				new Long(torrentFilterMode));

		setViewMode(COConfigurationManager.getIntParameter(ID + ".viewmode",
				MODE_BIGTABLE));

		SWTSkinObject so;
		so = getSkinObject(ID + "-button-smalltable");
		if (so != null) {
			btnSmallTable = new SWTSkinButtonUtility(so);
			btnSmallTable.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					setViewMode(MODE_SMALLTABLE);
				}
			});
		}

		so = getSkinObject(ID + "-button-bigtable");
		if (so != null) {
			btnBigTable = new SWTSkinButtonUtility(so);
			btnBigTable.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					setViewMode(MODE_BIGTABLE);
				}
			});
		}

		so = getSkinObject(ID + "-button-oldtable");
		if (so != null) {
			btnOldTable = new SWTSkinButtonUtility(so);
			btnOldTable.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					setViewMode(MODE_OLDTABLE);
				}
			});
		}

		return null;
	}

	public int getViewMode() {
		return viewMode;
	}

	public void setViewMode(int viewMode) {
		if (viewMode >= modeViewIDs.length || viewMode < 0) {
			return;
		}

		int oldViewMode = this.viewMode;

		this.viewMode = viewMode;

		SWTSkinObject soOldViewArea = getSkinObject(modeViewIDs[oldViewMode]);
		//SWTSkinObject soOldViewArea = skin.getSkinObjectByID(modeIDs[oldViewMode]);
		if (soOldViewArea != null) {
			soOldViewArea.setVisible(false);
		}

		SWTSkinObject soViewArea = getSkinObject(modeViewIDs[viewMode]);
		if (soViewArea == null) {
			soViewArea = skin.createSkinObject(modeIDs[viewMode] + torrentFilterMode,
					modeIDs[viewMode], soListArea);
			skin.layout();
			soViewArea.setVisible(true);
			soViewArea.getControl().setLayoutData(Utils.getFilledFormData());
		} else {
			soViewArea.setVisible(true);
		}

	}
}
