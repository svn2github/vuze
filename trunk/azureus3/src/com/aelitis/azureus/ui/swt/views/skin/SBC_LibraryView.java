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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.download.impl.DownloadManagerAdapter;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerAdapter;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarEntrySWT;

/**
 * @author TuxPaper
 * @created Jul 2, 2008
 *
 */
public class SBC_LibraryView
	extends SkinView
{
	private final static String ID = "library-list";

	public final static int MODE_BIGTABLE = 0;

	public final static int MODE_SMALLTABLE = 1;

	public static final int TORRENTS_ALL = 0;

	public static final int TORRENTS_COMPLETE = 1;

	public static final int TORRENTS_INCOMPLETE = 2;
	
	public static List allViewTitles = new ArrayList(1);
	
	private final static String[] modeViewIDs = {
		SkinConstants.VIEWID_SIDEBAR_LIBRARY_BIG,
		SkinConstants.VIEWID_SIDEBAR_LIBRARY_SMALL
	};

	private final static String[] modeIDs = {
		"library.table.big",
		"library.table.small"
	};

	private static int numSeeding = 0;

	private static int numDownloading = 0;

	private static int numComplete = 0;

	private static int numIncomplete = 0;

	private int viewMode = -1;

	private SWTSkinButtonUtility btnSmallTable;

	private SWTSkinButtonUtility btnBigTable;

	private SWTSkinObject soListArea;

	private int torrentFilterMode = TORRENTS_ALL;

	private String torrentFilter;

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#showSupport(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		torrentFilter = skinObject.getSkinObjectID();
		if (torrentFilter.equalsIgnoreCase(SideBar.SIDEBAR_SECTION_LIBRARY_DL)) {
			torrentFilterMode = TORRENTS_INCOMPLETE;
		} else if (torrentFilter.equalsIgnoreCase(SideBar.SIDEBAR_SECTION_LIBRARY_CD)) {
			torrentFilterMode = TORRENTS_COMPLETE;
		}

		soListArea = getSkinObject(ID + "-area");

		soListArea.getControl().setData("TorrentFilterMode",
				new Long(torrentFilterMode));

		setViewMode(COConfigurationManager.getIntParameter(torrentFilter + ".viewmode",
				MODE_BIGTABLE), false);

		SWTSkinObject so;
		so = getSkinObject(ID + "-button-smalltable");
		if (so != null) {
			btnSmallTable = new SWTSkinButtonUtility(so);
			btnSmallTable.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					setViewMode(MODE_SMALLTABLE, true);
				}
			});
		}

		so = getSkinObject(ID + "-button-bigtable");
		if (so != null) {
			btnBigTable = new SWTSkinButtonUtility(so);
			btnBigTable.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					setViewMode(MODE_BIGTABLE, true);
				}
			});
		}
		
		return null;
	}
	
	public int getViewMode() {
		return viewMode;
	}

	public void setViewMode(int viewMode, boolean save) {
		if (viewMode >= modeViewIDs.length || viewMode < 0 || viewMode == this.viewMode) {
			return;
		}

		int oldViewMode = this.viewMode;

		this.viewMode = viewMode;

		if (oldViewMode >= 0 && oldViewMode < modeViewIDs.length) {
  		SWTSkinObject soOldViewArea = getSkinObject(modeViewIDs[oldViewMode]);
  		//SWTSkinObject soOldViewArea = skin.getSkinObjectByID(modeIDs[oldViewMode]);
  		if (soOldViewArea != null) {
  			soOldViewArea.setVisible(false);
  		}
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

		if (save) {
			COConfigurationManager.setParameter(torrentFilter + ".viewmode",
					viewMode);
		}
	}

	public static void setupViewTitle() {
		SideBar sidebar = (SideBar) SkinViewManager.getByClass(SideBar.class);
		
		final ViewTitleInfo titleInfoDownloading = new ViewTitleInfo() {
			public Object getTitleInfoProperty(int propertyID) {
				if (propertyID == TITLE_LOGID) {
					String id = SideBar.SIDEBAR_SECTION_LIBRARY_DL;
					int viewMode = COConfigurationManager.getIntParameter(id
							+ ".viewmode", MODE_BIGTABLE);
					return id + "-" + viewMode;
				}

				if (propertyID == TITLE_INDICATOR_TEXT) {
					if(numIncomplete > 0)
						return numIncomplete + ""; // + " of " + numIncomplete;
				} 
				
				if (propertyID == TITLE_INDICATOR_TEXT_TOOLTIP) {
					return "There are " + numIncomplete + " incomplete torrents, "
							+ numDownloading + " of which are currently downloading";
				}

				if(propertyID == TITLE_HAS_ACTIVITY) {
					if(numDownloading > 0) {
						return new Boolean(true);
					}
				}
				
				return null;
			}
		};
		allViewTitles.add(titleInfoDownloading);
		SideBarEntrySWT infoDL = SideBar.getSideBarInfo(SideBar.SIDEBAR_SECTION_LIBRARY_DL);
		if (infoDL != null) {
			infoDL.setTitleInfo(titleInfoDownloading);
		}
		
		final ViewTitleInfo titleInfoSeeding = new ViewTitleInfo() {
			public Object getTitleInfoProperty(int propertyID) {
				if (propertyID == TITLE_LOGID) {
					String id = SideBar.SIDEBAR_SECTION_LIBRARY_CD;
					int viewMode = COConfigurationManager.getIntParameter(id
							+ ".viewmode", MODE_BIGTABLE);
					return id + "-" + viewMode;
				}

				if (propertyID == TITLE_INDICATOR_TEXT) {
					return null; //numSeeding + " of " + numComplete;
				} 
				
				if (propertyID == TITLE_INDICATOR_TEXT_TOOLTIP) {
					return "There are " + numComplete + " complete torrents, "
					+ numSeeding + " of which are currently seeding";
				}
				return null;
			}
		};
		allViewTitles.add(titleInfoSeeding);
		SideBarEntrySWT infoCD = SideBar.getSideBarInfo(SideBar.SIDEBAR_SECTION_LIBRARY_CD);
		if (infoCD != null) {
			infoCD.setTitleInfo(titleInfoSeeding);
		}

		SideBarEntrySWT infoLibrary = SideBar.getSideBarInfo(SideBar.SIDEBAR_SECTION_LIBRARY);
		if (infoLibrary != null) {
			infoLibrary.setTitleInfo(new ViewTitleInfo() {
				public Object getTitleInfoProperty(int propertyID) {
					if (propertyID == TITLE_LOGID) {
						String id = SideBar.SIDEBAR_SECTION_LIBRARY;
						int viewMode = COConfigurationManager.getIntParameter(id
								+ ".viewmode", MODE_BIGTABLE);
						return id + "-" + viewMode;
					}
					return null;
				}
			});
		}
		allViewTitles.add(titleInfoSeeding);
		
		final GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
		final DownloadManagerListener dmListener = new DownloadManagerAdapter() {
			public void stateChanged(DownloadManager dm, int state) {
				if (dm.getAssumedComplete()) {
					boolean isSeeding = dm.getState() == DownloadManager.STATE_SEEDING;
					Boolean wasSeedingB = (Boolean) dm.getUserData("wasSeeding");
					boolean wasSeeding = wasSeedingB == null ? false
							: wasSeedingB.booleanValue();
					if (isSeeding != wasSeeding) {
						if (isSeeding) {
							numSeeding++;
						} else {
							numSeeding--;
						}
						dm.setUserData("wasSeeding", new Boolean(isSeeding));
					}
				} else {
					boolean isDownloading = dm.getState() == DownloadManager.STATE_DOWNLOADING;
					Boolean wasDownloadingB = (Boolean) dm.getUserData("wasDownloading");
					boolean wasDownloading = wasDownloadingB == null ? false
							: wasDownloadingB.booleanValue();
					if (isDownloading != wasDownloading) {
						if (isDownloading) {
							numDownloading++;
						} else {
							numDownloading--;
						}
						dm.setUserData("wasDownloading", new Boolean(isDownloading));
					}
				}
				refreshAllLibraries();
			}

			public void completionChanged(DownloadManager dm, boolean completed) {
				if (dm.getAssumedComplete()) {
					numComplete++;
					numIncomplete--;
				} else {
					numIncomplete++;
					numComplete--;
				}
				refreshAllLibraries();
			}
		};
		gm.addListener(new GlobalManagerAdapter() {
			public void downloadManagerRemoved(DownloadManager dm) {
				if (dm.getAssumedComplete()) {
					numComplete--;
				} else {
					numIncomplete--;
				}
				refreshAllLibraries();
				dm.removeListener(dmListener);
			}

			public void downloadManagerAdded(DownloadManager dm) {
				refreshAllLibraries();
				dm.addListener(dmListener, false);

				if (dm.getAssumedComplete()) {
					numComplete++;
					if (dm.getState() == DownloadManager.STATE_SEEDING) {
						numSeeding++;
					}
				} else {
					numIncomplete++;
					if (dm.getState() == DownloadManager.STATE_DOWNLOADING) {
						dm.setUserData("wasDownloading", new Boolean(true));
						numSeeding++;
					} else {
						dm.setUserData("wasDownloading", new Boolean(false));
					}
				}
			}
		}, false);
		List downloadManagers = gm.getDownloadManagers();
		for (Iterator iter = downloadManagers.iterator(); iter.hasNext();) {
			DownloadManager dm = (DownloadManager) iter.next();
			dm.addListener(dmListener, false);
			if (dm.getAssumedComplete()) {
				numComplete++;
				if (dm.getState() == DownloadManager.STATE_SEEDING) {
					dm.setUserData("wasSeeding", new Boolean(true));
					numSeeding++;
				} else {
					dm.setUserData("wasSeeding", new Boolean(false));
				}
			} else {
				numIncomplete++;
				if (dm.getState() == DownloadManager.STATE_DOWNLOADING) {
					numSeeding++;
				}
			}
		}

	}

	/**
	 * 
	 *
	 * @since 3.1.1.1
	 */
	protected static void refreshAllLibraries() {
		Object[] all = allViewTitles.toArray();
		for (int i = 0; i < all.length; i++) {
			ViewTitleInfo vti = (ViewTitleInfo) all[i];
			
			ViewTitleInfoManager.refreshTitleInfo(vti);
		}
	}
}
