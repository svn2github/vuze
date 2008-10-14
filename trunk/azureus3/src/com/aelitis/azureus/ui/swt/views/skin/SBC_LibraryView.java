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
import org.gudy.azureus2.core3.global.GlobalManagerStats;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.speedmanager.SpeedManager;
import com.aelitis.azureus.core.torrent.HasBeenOpenedListener;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.toolbar.ToolBarItem;
import com.aelitis.azureus.ui.swt.toolbar.ToolBarItemListener;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarEntrySWT;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarVitalityImageSWT;

import org.gudy.azureus2.plugins.ui.sidebar.SideBarVitalityImage;
import org.gudy.azureus2.plugins.ui.tables.TableManager;

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

	public static final int TORRENTS_UNOPENED = 3;

	public static List allViews = new ArrayList(1);

	private final static String[] modeViewIDs = {
		SkinConstants.VIEWID_SIDEBAR_LIBRARY_BIG,
		SkinConstants.VIEWID_SIDEBAR_LIBRARY_SMALL
	};

	private final static String[] modeIDs = {
		"library.table.big",
		"library.table.small"
	};

	private static final String ID_VITALITY_ACTIVE = "image.sidebar.vitality.dl";

	private static final String ID_VITALITY_ALERT = "image.sidebar.vitality.alert";

	private static final long DL_VITALITY_REFRESH_RATE = 15000;

	private static int numSeeding = 0;

	private static int numDownloading = 0;

	private static int numComplete = 0;

	private static int numIncomplete = 0;

	private static int numErrorComplete = 0;

	private static String errorInCompleteTooltip;

	private static int numErrorInComplete = 0;

	private static String errorCompleteTooltip;

	private static int numUnOpened = 0;

	private int viewMode = -1;

	private SWTSkinButtonUtility btnSmallTable;

	private SWTSkinButtonUtility btnBigTable;

	private SWTSkinObject soListArea;

	private int torrentFilterMode = TORRENTS_ALL;

	private String torrentFilter;

	private ToolBarItem itemModeSmall;

	private ToolBarItem itemModeBig;

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#showSupport(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		torrentFilter = skinObject.getSkinObjectID();
		if (torrentFilter.equalsIgnoreCase(SideBar.SIDEBAR_SECTION_LIBRARY_DL)) {
			torrentFilterMode = TORRENTS_INCOMPLETE;
		} else if (torrentFilter.equalsIgnoreCase(SideBar.SIDEBAR_SECTION_LIBRARY_CD)) {
			torrentFilterMode = TORRENTS_COMPLETE;
		} else if (torrentFilter.equalsIgnoreCase(SideBar.SIDEBAR_SECTION_LIBRARY_UNOPENED)) {
			torrentFilterMode = TORRENTS_UNOPENED;
		}

		soListArea = getSkinObject(ID + "-area");

		soListArea.getControl().setData("TorrentFilterMode",
				new Long(torrentFilterMode));

		setViewMode(COConfigurationManager.getIntParameter(torrentFilter
				+ ".viewmode"), false);

		SWTSkinObject so;
		so = getSkinObject(ID + "-button-smalltable");
		if (so != null) {
			btnSmallTable = new SWTSkinButtonUtility(so);
			btnSmallTable.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility,
						SWTSkinObject skinObject, int stateMask) {
					setViewMode(MODE_SMALLTABLE, true);
				}
			});
		}

		so = getSkinObject(ID + "-button-bigtable");
		if (so != null) {
			btnBigTable = new SWTSkinButtonUtility(so);
			btnBigTable.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility,
						SWTSkinObject skinObject, int stateMask) {
					setViewMode(MODE_BIGTABLE, true);
				}
			});
		}
		
		SkinViewManager.addListener(new SkinViewManager.SkinViewManagerListener() {
			public void skinViewAdded(SkinView skinview) {
				if (skinview instanceof ToolBarView) {
					initToolBarView((ToolBarView) skinview);
				}
			}
		});

		ToolBarView tb = (ToolBarView) SkinViewManager.getByClass(ToolBarView.class);
		if (tb != null) {
			initToolBarView(tb);
		}

		return null;
	}

	protected void initToolBarView(ToolBarView tb) {
		itemModeSmall = tb.getToolBarItem("modeSmall");
		if (itemModeSmall != null) {
			itemModeSmall.addListener(new ToolBarItemListener() {
				public void pressed(ToolBarItem toolBarItem) {
					if (isVisible()) {
						setViewMode(MODE_SMALLTABLE, true);
					}
				}

				public boolean held(ToolBarItem toolBarItem) {
					return false;
				}
			});
		}
		itemModeBig = tb.getToolBarItem("modeBig");
		if (itemModeBig != null) {
			itemModeBig.addListener(new ToolBarItemListener() {
				public void pressed(ToolBarItem toolBarItem) {
					if (isVisible()) {
						setViewMode(MODE_BIGTABLE, true);
					}
				}

				public boolean held(ToolBarItem toolBarItem) {
					return false;
				}
			});
		}
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectShown(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectShown(SWTSkinObject skinObject, Object params) {
		ToolBarView tb = (ToolBarView) SkinViewManager.getByClass(ToolBarView.class);
		if (tb != null) {
			ToolBarItem itemModeSmall = tb.getToolBarItem("modeSmall");
			if (itemModeSmall != null) {
				itemModeSmall.setEnabled(true);
				itemModeSmall.getSkinButton().getSkinObject().switchSuffix(
						viewMode == MODE_BIGTABLE ? "" : "-down");
			}
			ToolBarItem itemModeBig = tb.getToolBarItem("modeBig");
			if (itemModeBig != null) {
				itemModeBig.setEnabled(true);
				itemModeBig.getSkinButton().getSkinObject().switchSuffix(
						viewMode == MODE_BIGTABLE ? "-down" : "");
			}
		}
		return super.skinObjectShown(skinObject, params);
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectAdapter#skinObjectHidden(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectHidden(SWTSkinObject skinObject, Object params) {
		return super.skinObjectHidden(skinObject, params);
	}

	public int getViewMode() {
		return viewMode;
	}

	public void setViewMode(int viewMode, boolean save) {
		if (viewMode >= modeViewIDs.length || viewMode < 0
				|| viewMode == this.viewMode) {
			return;
		}

		if (itemModeSmall != null) {
			itemModeSmall.getSkinButton().getSkinObject().switchSuffix(
					viewMode == MODE_BIGTABLE ? "" : "-down");
		}
		if (itemModeBig != null) {
			itemModeBig.getSkinButton().getSkinObject().switchSuffix(
					viewMode == MODE_BIGTABLE ? "-down" : "");
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
			COConfigurationManager.setParameter(torrentFilter + ".viewmode", viewMode);
		}
	}

	public static void setupViewTitle() {

		final ViewTitleInfo titleInfoDownloading = new ViewTitleInfo() {
			public Object getTitleInfoProperty(int propertyID) {
				if (propertyID == TITLE_LOGID) {
					String id = SideBar.SIDEBAR_SECTION_LIBRARY_DL;
					int viewMode = COConfigurationManager.getIntParameter(id
							+ ".viewmode");
					return id + "-" + viewMode;
				}

				if (propertyID == TITLE_INDICATOR_TEXT) {
					if (numIncomplete > 0)
						return numIncomplete + ""; // + " of " + numIncomplete;
				}

				if (propertyID == TITLE_INDICATOR_TEXT_TOOLTIP) {
					return "There are " + numIncomplete + " incomplete torrents, "
							+ numDownloading + " of which are currently downloading";
				}

				return null;
			}
		};
		SideBarEntrySWT infoDL = SideBar.getSideBarInfo(SideBar.SIDEBAR_SECTION_LIBRARY_DL);
		if (infoDL != null) {
			SideBarVitalityImage vitalityImage = infoDL.addVitalityImage(ID_VITALITY_ACTIVE);
			vitalityImage.setVisible(false);

			vitalityImage = infoDL.addVitalityImage(ID_VITALITY_ALERT);
			vitalityImage.setVisible(false);

			infoDL.setTitleInfo(titleInfoDownloading);

			SimpleTimer.addPeriodicEvent("DLVitalityRefresher",
					DL_VITALITY_REFRESH_RATE, new TimerEventPerformer() {
						public void perform(TimerEvent event) {
							SideBarEntrySWT entry = SideBar.getSideBarInfo(SideBar.SIDEBAR_SECTION_LIBRARY_DL);
							SideBarVitalityImage[] vitalityImages = entry.getVitalityImages();
							for (int i = 0; i < vitalityImages.length; i++) {
								SideBarVitalityImage vitalityImage = vitalityImages[i];
								if (vitalityImage.getImageID().equals(ID_VITALITY_ACTIVE)) {
									refreshDLSpinner((SideBarVitalityImageSWT) vitalityImage);
								}
							}
						}
					});
		}

		final ViewTitleInfo titleInfoSeeding = new ViewTitleInfo() {
			public Object getTitleInfoProperty(int propertyID) {
				if (propertyID == TITLE_LOGID) {
					String id = SideBar.SIDEBAR_SECTION_LIBRARY_CD;
					int viewMode = COConfigurationManager.getIntParameter(id
							+ ".viewmode");
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
		SideBarEntrySWT infoCD = SideBar.getSideBarInfo(SideBar.SIDEBAR_SECTION_LIBRARY_CD);
		if (infoCD != null) {
			SideBarVitalityImage vitalityImage = infoCD.addVitalityImage(ID_VITALITY_ALERT);
			vitalityImage.setVisible(false);

			infoCD.setTitleInfo(titleInfoSeeding);
		}

		SideBarEntrySWT infoLibrary = SideBar.getSideBarInfo(SideBar.SIDEBAR_SECTION_LIBRARY);
		if (infoLibrary != null) {
			infoLibrary.setTitleInfo(new ViewTitleInfo() {
				public Object getTitleInfoProperty(int propertyID) {
					if (propertyID == TITLE_LOGID) {
						String id = SideBar.SIDEBAR_SECTION_LIBRARY;
						int viewMode = COConfigurationManager.getIntParameter(id
								+ ".viewmode");
						return id + "-" + viewMode;
					}
					return null;
				}
			});
		}

		SideBarEntrySWT infoLibraryUn = SideBar.getSideBarInfo(SideBar.SIDEBAR_SECTION_LIBRARY_UNOPENED);
		if (infoLibraryUn != null) {
			infoLibraryUn.setTitleInfo(new ViewTitleInfo() {
				public Object getTitleInfoProperty(int propertyID) {
					if (propertyID == TITLE_LOGID) {
						String id = SideBar.SIDEBAR_SECTION_LIBRARY;
						int viewMode = COConfigurationManager.getIntParameter(id
								+ ".viewmode");
						return id + "-" + viewMode;
					} else if (propertyID == TITLE_INDICATOR_TEXT && numUnOpened > 0) {
						return "" + numUnOpened;
					}
					return null;
				}
			});
		}

		final GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
		final DownloadManagerListener dmListener = new DownloadManagerAdapter() {
			public void stateChanged(DownloadManager dm, int state) {
				if (PlatformTorrentUtils.getAdId(dm.getTorrent()) != null
						|| PlatformTorrentUtils.isUpdateDM(dm)) {
					return;
				}
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

				boolean complete = dm.getAssumedComplete();
				Boolean wasErrorStateB = (Boolean) dm.getUserData("wasErrorState");
				boolean wasErrorState = wasErrorStateB == null ? false
						: wasErrorStateB.booleanValue();
				boolean isErrorState = state == DownloadManager.STATE_ERROR;
				if (isErrorState != wasErrorState) {
					int rel = isErrorState ? 1 : -1;
					if (complete) {
						numErrorComplete += rel;
					} else {
						numErrorInComplete += rel;
					}
					updateErrorTooltip();
					dm.setUserData("wasErrorState", new Boolean(isErrorState));
				}
				refreshAllLibraries();
			}

			public void completionChanged(DownloadManager dm, boolean completed) {
				if (PlatformTorrentUtils.getAdId(dm.getTorrent()) != null
						|| PlatformTorrentUtils.isUpdateDM(dm)) {
					return;
				}
				if (completed) {
					numComplete++;
					numIncomplete--;
					if (dm.getState() == DownloadManager.STATE_ERROR) {
						numErrorComplete++;
						numErrorInComplete--;
					}
				} else {
					numIncomplete++;
					numComplete--;
					if (dm.getState() == DownloadManager.STATE_ERROR) {
						numErrorComplete--;
						numErrorInComplete++;
					}
				}
				recountUnopened();
				updateErrorTooltip();
				refreshAllLibraries();
			}

			protected void updateErrorTooltip() {
				if (numErrorComplete < 0) {
					numErrorComplete = 0;
				}
				if (numErrorInComplete < 0) {
					numErrorInComplete = 0;
				}

				if (numErrorComplete > 0 || numErrorInComplete > 0) {

					String comp_error = null;
					String incomp_error = null;

					List downloads = gm.getDownloadManagers();

					for (int i = 0; i < downloads.size(); i++) {

						DownloadManager download = (DownloadManager) downloads.get(i);

						if (download.getState() == DownloadManager.STATE_ERROR) {

							if (download.getAssumedComplete()) {

								if (comp_error == null) {

									comp_error = download.getDisplayName() + ": "
											+ download.getErrorDetails();
								} else {

									comp_error += "...";
								}
							} else {
								if (incomp_error == null) {

									incomp_error = download.getDisplayName() + ": "
											+ download.getErrorDetails();
								} else {

									incomp_error += "...";
								}
							}
						}
					}

					errorCompleteTooltip = comp_error;
					errorInCompleteTooltip = incomp_error;
				}
			}
		};

		gm.addListener(new GlobalManagerAdapter() {
			public void downloadManagerRemoved(DownloadManager dm) {
				if (PlatformTorrentUtils.getAdId(dm.getTorrent()) != null
						|| PlatformTorrentUtils.isUpdateDM(dm)) {
					return;
				}
				recountUnopened();
				if (dm.getAssumedComplete()) {
					numComplete--;
				} else {
					numIncomplete--;
				}
				refreshAllLibraries();
				dm.removeListener(dmListener);
			}

			public void downloadManagerAdded(DownloadManager dm) {
				if (PlatformTorrentUtils.getAdId(dm.getTorrent()) != null
						|| PlatformTorrentUtils.isUpdateDM(dm)) {
					return;
				}
				dm.addListener(dmListener, false);

				recountUnopened();
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
				refreshAllLibraries();
			}
		}, false);
		List downloadManagers = gm.getDownloadManagers();
		for (Iterator iter = downloadManagers.iterator(); iter.hasNext();) {
			DownloadManager dm = (DownloadManager) iter.next();
			if (PlatformTorrentUtils.getAdId(dm.getTorrent()) != null
					|| PlatformTorrentUtils.isUpdateDM(dm)) {
				continue;
			}
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
		recountUnopened();
		PlatformTorrentUtils.addHasBeenOpenedListener(new HasBeenOpenedListener() {
			public void hasBeenOpenedChanged(DownloadManager dm, boolean opened) {
				recountUnopened();
				refreshAllLibraries();
			}
		});
	}
	
	private static void recountUnopened() {
		GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
		List dms = gm.getDownloadManagers();
		numUnOpened = 0;
		for (Iterator iter = dms.iterator(); iter.hasNext();) {
			DownloadManager dm = (DownloadManager) iter.next();
			if (!PlatformTorrentUtils.getHasBeenOpened(dm) && dm.getAssumedComplete()) {
				numUnOpened++;
			}
		}
	}

	/**
	 * 
	 *
	 * @since 3.1.1.1
	 */
	protected static void refreshAllLibraries() {
		SideBarEntrySWT entry = SideBar.getSideBarInfo(SideBar.SIDEBAR_SECTION_LIBRARY_DL);
		SideBarVitalityImage[] vitalityImages = entry.getVitalityImages();
		for (int i = 0; i < vitalityImages.length; i++) {
			SideBarVitalityImage vitalityImage = vitalityImages[i];
			if (vitalityImage.getImageID().equals(ID_VITALITY_ACTIVE)) {
				vitalityImage.setVisible(numDownloading > 0);

				refreshDLSpinner((SideBarVitalityImageSWT) vitalityImage);

			} else if (vitalityImage.getImageID().equals(ID_VITALITY_ALERT)) {
				vitalityImage.setVisible(numErrorInComplete > 0);
				if (numErrorInComplete > 0) {
					vitalityImage.setToolTip(errorInCompleteTooltip);
				}
			}
		}
		ViewTitleInfoManager.refreshTitleInfo(entry.getTitleInfo());

		entry = SideBar.getSideBarInfo(SideBar.SIDEBAR_SECTION_LIBRARY_CD);
		vitalityImages = entry.getVitalityImages();
		for (int i = 0; i < vitalityImages.length; i++) {
			SideBarVitalityImage vitalityImage = vitalityImages[i];
			if (vitalityImage.getImageID().equals(ID_VITALITY_ALERT)) {
				vitalityImage.setVisible(numErrorComplete > 0);
				if (numErrorComplete > 0) {
					vitalityImage.setToolTip(errorCompleteTooltip);
				}
			}
		}

		entry = SideBar.getSideBarInfo(SideBar.SIDEBAR_SECTION_LIBRARY_UNOPENED);
		ViewTitleInfoManager.refreshTitleInfo(entry.getTitleInfo());
	}

	public static void refreshDLSpinner(SideBarVitalityImageSWT vitalityImage) {
		if (vitalityImage.getImageID().equals(ID_VITALITY_ACTIVE)) {
			if (!vitalityImage.isVisible()) {
				return;
			}
			SpeedManager sm = AzureusCoreFactory.getSingleton().getSpeedManager();
			if (sm != null) {
				GlobalManagerStats stats = AzureusCoreFactory.getSingleton().getGlobalManager().getStats();

				int delay = 100;
				int limit = NetworkManager.getMaxDownloadRateBPS();
				if (limit <= 0) {
					limit = sm.getEstimatedDownloadCapacityBytesPerSec().getBytesPerSec();
				}

				// smoothing
				int current = stats.getDataReceiveRate() / 10;
				limit /= 10;

				if (limit > 0) {
					if (current > limit) {
						delay = 25;
					} else {
						// 40 incrememnts of 5.. max 200
						current += 39;
						delay = (40 - (current * 40 / limit)) * 5;
						if (delay < 35) {
							delay = 35;
						} else if (delay > 200) {
							delay = 200;
						}
					}
					if (vitalityImage instanceof SideBarVitalityImageSWT) {
						SideBarVitalityImageSWT viSWT = (SideBarVitalityImageSWT) vitalityImage;
						if (viSWT.getDelayTime() != delay) {
							viSWT.setDelayTime(delay);
							//System.out.println("new delay: " + delay + "; via " + current + " / " + limit);
						}
					}
				}
			}
		}
	}

	public static String getTableIdFromFilterMode(int torrentFilterMode,
			boolean big) {
		if (torrentFilterMode == SBC_LibraryView.TORRENTS_COMPLETE) {
			return big ? TableManager.TABLE_MYTORRENTS_COMPLETE_BIG
					: TableManager.TABLE_MYTORRENTS_COMPLETE;
		} else if (torrentFilterMode == SBC_LibraryView.TORRENTS_INCOMPLETE) {
			return big ? TableManager.TABLE_MYTORRENTS_INCOMPLETE_BIG
					: TableManager.TABLE_MYTORRENTS_INCOMPLETE;
		} else if (torrentFilterMode == SBC_LibraryView.TORRENTS_UNOPENED) {
			return big ? TableManager.TABLE_MYTORRENTS_UNOPENED_BIG
					: TableManager.TABLE_MYTORRENTS_UNOPENED;
		} else if (torrentFilterMode == SBC_LibraryView.TORRENTS_ALL) {
			return TableManager.TABLE_MYTORRENTS_ALL_BIG;
		}
		return null;
	}
}
