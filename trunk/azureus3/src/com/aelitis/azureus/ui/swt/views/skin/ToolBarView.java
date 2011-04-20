/**
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

import java.util.*;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfoSet;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FrequencyLimitedDispatcher;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarActivationListener;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.ui.swt.TorrentUtil;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UIToolBarManagerCore;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.devices.DeviceManager;
import com.aelitis.azureus.core.devices.DeviceManagerFactory;
import com.aelitis.azureus.core.devices.TranscodeException;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.common.*;
import com.aelitis.azureus.ui.selectedcontent.*;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.devices.DeviceManagerUI;
import com.aelitis.azureus.ui.swt.devices.TranscodeChooser;
import com.aelitis.azureus.ui.swt.mdi.MdiEntrySWT;
import com.aelitis.azureus.ui.swt.mdi.MultipleDocumentInterfaceSWT;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.toolbar.ToolBarItemSO;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager.SkinViewManagerListener;
import com.aelitis.azureus.util.DLReferals;
import com.aelitis.azureus.util.PlayUtils;

/**
 * @author TuxPaper
 * @created Jul 20, 2008
 */
public class ToolBarView
	extends SkinView
	implements UIToolBarManagerCore
{
	private static boolean DEBUG = false;

	private static toolbarButtonListener buttonListener;

	private Map<String, List<String>> mapGroupToItemIDs = new HashMap<String, List<String>>();
	
	private Map<String, ToolBarItem> items = new LinkedHashMap<String, ToolBarItem>();

	//private GlobalManager gm;

	Control lastControl = null;

	private boolean showText = true;

	private SWTSkinObject skinObject;

	private SWTSkinObject so2nd;

	private SWTSkinObject soGap;

	private boolean initComplete = false;

	private ArrayList<ToolBarViewListener> listeners = new ArrayList<ToolBarViewListener>(
			1);

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#showSupport(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectInitialShow(final SWTSkinObject skinObject,
			Object params) {
		boolean uiClassic = COConfigurationManager.getStringParameter("ui").equals(
				"az2");

		this.skinObject = skinObject;
		buttonListener = new toolbarButtonListener();
		so2nd = skinObject.getSkin().getSkinObject("global-toolbar-2nd");

		soGap = skinObject.getSkin().getSkinObject("toolbar-gap");
		if (soGap != null) {
			Control cGap = soGap.getControl();
			FormData fd = (FormData) cGap.getLayoutData();
			if (fd.width == SWT.DEFAULT) {
				cGap.getParent().addListener(SWT.Resize, new Listener() {
					public void handleEvent(Event event) {
						resizeGap();
					}
				});
			} else {
				soGap = null;
			}
		}

		ToolBarItemSO item;

		if (!uiClassic) {
			// ==download
			item = new ToolBarItemSO(this, "download", "image.button.download",
					"v3.MainWindow.button.download");
			item.setGroupID(GROUP_BIG);
			item.setDefaultActivationListener(new UIToolBarActivationListener() {
				public boolean toolBarItemActivated(ToolBarItem item,
						long activationType, Object datasource) {
					if (activationType != ACTIVATIONTYPE_NORMAL) {
						return false;
					}
					// This is for our CDP pages
					ISelectedContent[] sc = SelectedContentManager.getCurrentlySelectedContent();
					if (sc != null && sc.length == 1
							&& (sc[0].getHash() != null || sc[0].getDownloadInfo() != null)) {
						TorrentListViewsUtils.downloadDataSource(sc[0], false,
								DLReferals.DL_REFERAL_TOOLBAR);
						return true;
					}
					return false;
				}
			});
			addToolBarItem(item, "toolbar.area.item", soMain);

			// ==play
			item = new ToolBarItemSO(this, "play", "image.button.play",
					"iconBar.play");
			item.setGroupID(GROUP_BIG);
			item.setDefaultActivationListener(new UIToolBarActivationListener() {
				public boolean toolBarItemActivated(ToolBarItem item,
						long activationType, Object datasource) {
					if (activationType != ACTIVATIONTYPE_NORMAL) {
						return false;
					}
					ISelectedContent[] sc = SelectedContentManager.getCurrentlySelectedContent();
					if (sc != null && sc.length > 0) {

						if (PlayUtils.canStreamDS(sc[0], sc[0].getFileIndex())) {
							TorrentListViewsUtils.playOrStreamDataSource(sc[0],
									DLReferals.DL_REFERAL_TOOLBAR, true, false);
						} else {
							TorrentListViewsUtils.playOrStreamDataSource(sc[0],
									DLReferals.DL_REFERAL_TOOLBAR, false, true);
						}
					}
					return false;
				}
			});
			addToolBarItem(item, "toolbar.area.item", soMain);

			addSeperator("toolbar.area.item.sep", soMain);

			lastControl = null;

		} else {

			lastControl = null;

			// ==OPEN
			item = new ToolBarItemSO(this, "open", "image.toolbar.open", "Button.add");
			item.setDefaultActivationListener(new UIToolBarActivationListener() {
				public boolean toolBarItemActivated(ToolBarItem item,
						long activationType, Object datasource) {
					if (activationType != ACTIVATIONTYPE_NORMAL) {
						return false;
					}
					TorrentOpener.openTorrentWindow();
					return true;
				}
			});
			item.setAlwaysAvailable(true);
			item.setGroupID("classic");
			addToolBarItemNoCreate(item);

			// ==SEARCH
			item = new ToolBarItemSO(this, "search", "search", "Button.search");
			item.setDefaultActivationListener(new UIToolBarActivationListener() {
				public boolean toolBarItemActivated(ToolBarItem item,
						long activationType, Object datasource) {
					if (activationType != ACTIVATIONTYPE_NORMAL) {
						return false;
					}
					UIFunctionsManagerSWT.getUIFunctionsSWT().promptForSearch();
					return true;
				}
			});
			item.setAlwaysAvailable(true);
			item.setGroupID("classic");
			addToolBarItemNoCreate(item);
		}

		// ==transcode
		if (!DeviceManagerUI.DISABLED) {
			item = new ToolBarItemSO(this, "transcode", "image.button.transcode",
					"iconBar.transcode");
			item.setDefaultActivationListener(new UIToolBarActivationListener() {
				public boolean toolBarItemActivated(ToolBarItem item,
						long activationType, Object datasource) {
					if (activationType != ACTIVATIONTYPE_NORMAL) {
						return false;
					}
					ISelectedContent[] contents = SelectedContentManager.getCurrentlySelectedContent();
					if (contents.length == 0) {
						return false;
					}

					deviceSelected(contents, true);
					return true;
				}
			});
			addToolBarItemNoCreate(item);
		}

		// ==run
		item = new ToolBarItemSO(this, "run", "image.toolbar.run", "iconBar.run");
		item.setDefaultActivationListener(new UIToolBarActivationListener() {
			public boolean toolBarItemActivated(ToolBarItem item,
					long activationType, Object datasource) {
				if (activationType != ACTIVATIONTYPE_NORMAL) {
					return false;
				}
				DownloadManager[] dms = SelectedContentManager.getDMSFromSelectedContent();
				if (dms != null) {
					TorrentUtil.runDataSources(dms);

					for (int i = 0; i < dms.length; i++) {
						DownloadManager dm = dms[i];
						PlatformTorrentUtils.setHasBeenOpened(dm, true);
					}
					return true;
				}
				return false;
			}
		});
		addToolBarItemNoCreate(item);
		//addToolBarItem(item, "toolbar.area.sitem", so2nd);

		if (uiClassic) {
			// ==TOP
			item = new ToolBarItemSO(this, "top", "image.toolbar.top", "iconBar.top");
			item.setDefaultActivationListener(new UIToolBarActivationListener() {
				public boolean toolBarItemActivated(ToolBarItem item,
						long activationType, Object datasource) {
					if (activationType == ACTIVATIONTYPE_NORMAL) {
						return moveTop();
					}

					return false;
				}
			});
			addToolBarItemNoCreate(item);
		}

		// ==UP
		item = new ToolBarItemSO(this, "up", "image.toolbar.up", "v3.iconBar.up");
		item.setDefaultActivationListener(new UIToolBarActivationListener() {
			public boolean toolBarItemActivated(ToolBarItem item,
					long activationType, Object datasource) {
				if (activationType == ACTIVATIONTYPE_NORMAL) {
					if (!AzureusCoreFactory.isCoreRunning()) {
						return false;
					}
					DownloadManager[] dms = SelectedContentManager.getDMSFromSelectedContent();
					if (dms != null) {
						Arrays.sort(dms, new Comparator<DownloadManager>() {
							public int compare(DownloadManager a, DownloadManager b) {
								return a.getPosition() - b.getPosition();
							}
						});
						GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
						for (int i = 0; i < dms.length; i++) {
							DownloadManager dm = dms[i];
							if (gm.isMoveableUp(dm)) {
								gm.moveUp(dm);
							}
						}
					}
				} else if (activationType == ACTIVATIONTYPE_HELD) {
					return moveTop();
				}
				return false;
			}
		});
		addToolBarItemNoCreate(item);

		// ==down
		item = new ToolBarItemSO(this, "down", "image.toolbar.down",
				"v3.iconBar.down");
		item.setDefaultActivationListener(new UIToolBarActivationListener() {
			public boolean toolBarItemActivated(ToolBarItem item,
					long activationType, Object datasource) {
				if (activationType == ACTIVATIONTYPE_NORMAL) {
					if (!AzureusCoreFactory.isCoreRunning()) {
						return false;
					}

					GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
					DownloadManager[] dms = SelectedContentManager.getDMSFromSelectedContent();
					if (dms != null) {
						Arrays.sort(dms, new Comparator<DownloadManager>() {
							public int compare(DownloadManager a, DownloadManager b) {
								return b.getPosition() - a.getPosition();
							}
						});
						for (int i = 0; i < dms.length; i++) {
							DownloadManager dm = dms[i];
							if (gm.isMoveableDown(dm)) {
								gm.moveDown(dm);
							}
						}
						return true;
					}
				} else if (activationType == ACTIVATIONTYPE_HELD) {
					return moveBottom();
				}
				return false;
			}
		});
		addToolBarItemNoCreate(item);

		if (uiClassic) {
			// ==BOTTOM
			item = new ToolBarItemSO(this, "bottom", "image.toolbar.bottom",
					"iconBar.bottom");
			item.setDefaultActivationListener(new UIToolBarActivationListener() {
				public boolean toolBarItemActivated(ToolBarItem item,
						long activationType, Object datasource) {
					if (activationType != ACTIVATIONTYPE_NORMAL) {
						return false;
					}
					return moveBottom();
				}
			});
			addToolBarItemNoCreate(item);
		}
		/*
				// ==start
				item = new ToolBarItemSO(this, "start", "image.toolbar.start", "iconBar.start");
				item.setDefaultActivation(new UIToolBarActivationListener() {
					public boolean toolBarItemActivated(ToolBarItem item, long activationType) {
						if (activationType != ACTIVATIONTYPE_NORMAL) {
							return false;
						}
						DownloadManager[] dms = SelectedContentManager.getDMSFromSelectedContent();
						if (dms != null) {
							TorrentUtil.queueDataSources(dms, true);
							return true;
						}
						return false;
					}
				});
				addToolBarItem(item, "toolbar.area.sitem", so2nd);
				//SWTSkinObjectContainer so = (SWTSkinObjectContainer) item.getSkinButton().getSkinObject();
				//so.setDebugAndChildren(true);
				addSeperator(so2nd);

				// ==stop
				item = new ToolBarItemSO(this, "stop", "image.toolbar.stop", "iconBar.stop");
				item.setDefaultActivation(new UIToolBarActivationListener() {
					public boolean toolBarItemActivated(ToolBarItem item, long activationType) {
						if (activationType != ACTIVATIONTYPE_NORMAL) {
							return false;
						}
		 				ISelectedContent[] currentContent = SelectedContentManager.getCurrentlySelectedContent();
						TorrentUtil.stopDataSources(currentContent);
						return true;
					}
				});
				addToolBarItem(item, "toolbar.area.sitem", so2nd);
				addSeperator(so2nd);
		*/
		// ==startstop
		item = new ToolBarItemSO(this, "startstop",
				"image.toolbar.startstop.start", "iconBar.startstop");
		item.setDefaultActivationListener(new UIToolBarActivationListener() {
			public boolean toolBarItemActivated(ToolBarItem item,
					long activationType, Object datasource) {
				if (activationType != ACTIVATIONTYPE_NORMAL) {
					return false;
				}
				ISelectedContent[] currentContent = SelectedContentManager.getCurrentlySelectedContent();
				TorrentUtil.stopOrStartDataSources(currentContent);
				return true;
			}
		});
		addToolBarItemNoCreate(item);

		// ==remove
		item = new ToolBarItemSO(this, "remove", "image.toolbar.remove",
				"iconBar.remove");
		item.setDefaultActivationListener(new UIToolBarActivationListener() {
			public boolean toolBarItemActivated(ToolBarItem item,
					long activationType, Object datasource) {
				if (activationType != ACTIVATIONTYPE_NORMAL) {
					return false;
				}
				DownloadManager[] dms = SelectedContentManager.getDMSFromSelectedContent();
				if (dms != null) {
					TorrentUtil.removeDownloads(dms, null);
					return true;
				}
				return false;
			}
		});
		addToolBarItemNoCreate(item);

		///////////////////////

		// == mode big
		item = new ToolBarItemSO(this, "modeBig", "image.toolbar.table_large",
				"v3.iconBar.view.big") {
			public void setSkinButton(SWTSkinButtonUtility btn) {
				super.setSkinButton(btn);
				SWTSkinObject soTitle = skin.getSkinObject("toolbar-item-title",
						btn.getSkinObject());
				if (soTitle instanceof SWTSkinObjectText) {
					((SWTSkinObjectText) soTitle).setStyle(SWT.RIGHT);
				}
			}

			public void setEnabled(boolean enabled) {
				if (!enabled) {
					SWTSkinObject so = getSkinButton().getSkinObject();
					if (so != null && so.getSuffix().contains("-down")) {
						so.switchSuffix("");
					}
				}
				super.setEnabled(enabled);
			}
		};
		item.setGroupID("views");
		addToolBarItemNoCreate(item);

		// == mode small
		item = new ToolBarItemSO(this, "modeSmall", "image.toolbar.table_normal",
				"v3.iconBar.view.small") {
			
			public void setSkinButton(SWTSkinButtonUtility btn) {
				super.setSkinButton(btn);
				SWTSkinObject soTitle = skin.getSkinObject("toolbar-item-title",
						btn.getSkinObject());
				if (soTitle instanceof SWTSkinObjectText) {
					((SWTSkinObjectText) soTitle).setStyle(SWT.LEFT);
				}
			}

			public void setEnabled(boolean enabled) {
				if (!enabled) {
					SWTSkinObject so = getSkinButton().getSkinObject();
					if (so != null && so.getSuffix().contains("-down")) {
						so.switchSuffix("");
					}
				}
				super.setEnabled(enabled);
			}
		};
		item.setGroupID("views");
		addToolBarItemNoCreate(item);

		
		//addSeperator(so2nd);

		if (uiClassic) {
			bulkSetupItems("classic", "toolbar.area.sitem", so2nd);
			addNonToolBar("toolbar.area.sitem.left2", so2nd);
		}
		bulkSetupItems(GROUP_MAIN, "toolbar.area.sitem", so2nd);
		addNonToolBar("toolbar.area.sitem.left2", so2nd);
		bulkSetupItems("views", "toolbar.area.vitem", so2nd);
		addNonToolBar("toolbar.area.sitem.left2", so2nd);

		resizeGap();

		SelectedContentManager.addCurrentlySelectedContentListener(new SelectedContentListener() {
			public void currentlySelectedContentChanged(
					ISelectedContent[] currentContent, String viewID) {
				refreshCoreToolBarItems();
				//updateCoreItems(currentContent, viewID);
				UIFunctionsManagerSWT.getUIFunctionsSWT().refreshTorrentMenu();
			}
		});

		try {
			if (!COConfigurationManager.getBooleanParameter("ToolBar.showText")) {
				flipShowText();
			}
		} catch (Throwable t) {
			Debug.out(t);
		}

		initComplete = true;

		synchronized (listeners) {
			for (ToolBarViewListener l : listeners) {
				try {
					l.toolbarViewInitialized(this);
				} catch (Exception e) {
					Debug.out(e);
				}
			}
		}

		return null;
	}

	public boolean triggerToolBarItem(ToolBarItem item, long activationType,
			Object datasource) {
		if (triggerViewToolBar(item, activationType, datasource)) {
			return true;
		}

		UIToolBarActivationListener defaultActivation = item.getDefaultActivationListener();
		if (defaultActivation != null) {
			return defaultActivation.toolBarItemActivated(item, activationType,
					datasource);
		}

		if (DEBUG) {
			String viewID = SelectedContentManager.getCurrentySelectedViewID();
			System.out.println("Warning: Fallback of toolbar button " + item.getID()
					+ " via " + viewID + " view");
		}

		return false;
	}

	protected boolean moveBottom() {
		if (!AzureusCoreFactory.isCoreRunning()) {
			return false;
		}

		GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
		DownloadManager[] dms = SelectedContentManager.getDMSFromSelectedContent();
		if (dms != null) {
			gm.moveEnd(dms);
		}
		return true;
	}

	protected void deviceSelected(final ISelectedContent[] contents,
			final boolean allow_retry) {
		TranscodeChooser deviceChooser = new TranscodeChooser() {
			public void closed() {
				DeviceManager deviceManager = DeviceManagerFactory.getSingleton();
				if (selectedTranscodeTarget != null && selectedProfile != null) {
					for (int i = 0; i < contents.length; i++) {
						ISelectedContent selectedContent = contents[i];

						DownloadManager dm = selectedContent.getDownloadManager();
						if (dm == null) {
							continue;
						}
						DiskManagerFileInfoSet fileSet = dm.getDiskManagerFileInfoSet();
						DiskManagerFileInfo[] files = fileSet.getFiles();
						for (DiskManagerFileInfo file : files) {
							try {
								deviceManager.getTranscodeManager().getQueue().add(
										selectedTranscodeTarget,
										selectedProfile,
										(org.gudy.azureus2.plugins.disk.DiskManagerFileInfo) PluginCoreUtils.convert(
												file, false), false);
							} catch (TranscodeException e) {
								Debug.out(e);
							}
						}
					}
				}
			}
		};

		deviceChooser.show(new Runnable() {
			public void run() {
				if (allow_retry) {

					deviceSelected(contents, false);
				}
			}
		});
	}

	protected boolean moveTop() {
		if (!AzureusCoreFactory.isCoreRunning()) {
			return false;
		}
		GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
		DownloadManager[] dms = SelectedContentManager.getDMSFromSelectedContent();
		if (dms != null) {
			gm.moveTop(dms);
		}
		return true;
	}

	/**
	 * 
	 *
	 * @since 3.1.1.1
	 */
	protected void resizeGap() {
		if (soGap == null) {
			skinObject.getControl().getParent().layout();
			return;
		}
		Rectangle boundsLeft = skinObject.getControl().getBounds();
		Rectangle boundsRight = so2nd.getControl().getBounds();

		Rectangle clientArea = soGap.getControl().getParent().getClientArea();

		FormData fd = (FormData) soGap.getControl().getLayoutData();
		fd.width = clientArea.width - (boundsLeft.x + boundsLeft.width)
				- (boundsRight.width);
		if (fd.width < 0) {
			fd.width = 0;
		} else if (fd.width > 50) {
			fd.width -= 30;
		} else if (fd.width > 20) {
			fd.width = 20;
		}
		soGap.getControl().getParent().layout();
	}

	public UIToolBarItem getToolBarItem(String itemID) {
		return items.get(itemID);
	}

	public ToolBarItemSO getToolBarItemSO(String itemID) {
		return (ToolBarItemSO) items.get(itemID);
	}

	public UIToolBarItem[] getAllToolBarItems() {
		return items.values().toArray(new ToolBarItem[0]);
	}

	public ToolBarItem[] getAllSWTToolBarItems() {
		return items.values().toArray(new ToolBarItem[0]);
	}

	private FrequencyLimitedDispatcher refresh_limiter = new FrequencyLimitedDispatcher(
			new AERunnable() {
				private AERunnable lock = this;

				private boolean refresh_pending;

				public void runSupport() {
					synchronized (lock) {

						if (refresh_pending) {

							return;
						}
						refresh_pending = true;
					}

					if (DEBUG) {
						System.out.println("refreshCoreItems via "
								+ Debug.getCompressedStackTrace());
					}

					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {

							synchronized (lock) {

								refresh_pending = false;
							}

							_refreshCoreToolBarItems();
						}
					});
				}
			}, 250);

	private Map<DownloadManager, DownloadManagerListener> dm_listener_map = new HashMap<DownloadManager, DownloadManagerListener>();

	public void refreshCoreToolBarItems() {
		refresh_limiter.dispatch();
	}

	public void _refreshCoreToolBarItems() {

		MultipleDocumentInterfaceSWT mdi = UIFunctionsManagerSWT.getUIFunctionsSWT().getMDISWT();

		if (mdi != null) {
			UIToolBarItem[] allToolBarItems = getAllToolBarItems();
			MdiEntrySWT entry = mdi.getCurrentEntrySWT();
			Map<String, Long> mapStates = new HashMap<String, Long>();
			if (entry != null) {
				ToolBarEnablerBase[] enablers = entry.getToolbarEnablers();
				for (ToolBarEnablerBase enabler : enablers) {
					if (enabler instanceof ToolBarEnabler2) {
						((ToolBarEnabler2) enabler).refreshToolBarItems(mapStates);
					} else if (enabler instanceof ToolBarEnabler) {
						Map<String, Boolean> oldMapStates = new HashMap<String, Boolean>();
						((ToolBarEnabler) enabler).refreshToolBar(oldMapStates);

						for (String key : oldMapStates.keySet()) {
							Boolean enable = oldMapStates.get(key);
							Long curState = mapStates.get(key);
							if (curState == null) {
								curState = 0L;
							}
							if (enable) {
								mapStates.put(key, curState | ToolBarEnabler2.STATE_ENABLED);
							} else {
								mapStates.put(key, curState & (~ToolBarEnabler2.STATE_ENABLED));
							}
						}
					}
				}
			}

			ISelectedContent[] currentContent = SelectedContentManager.getCurrentlySelectedContent();

			synchronized (dm_listener_map) {

				Map<DownloadManager, DownloadManagerListener> copy = new HashMap<DownloadManager, DownloadManagerListener>(
						dm_listener_map);

				for (ISelectedContent content : currentContent) {

					DownloadManager dm = content.getDownloadManager();

					if (dm != null) {

						if (copy.remove(dm) == null) {

							DownloadManagerListener l = new DownloadManagerListener() {
								public void stateChanged(DownloadManager manager, int state) {
									refreshCoreToolBarItems();
								}

								public void downloadComplete(DownloadManager manager) {
									refreshCoreToolBarItems();
								}

								public void completionChanged(DownloadManager manager,
										boolean bCompleted) {
									refreshCoreToolBarItems();
								}

								public void positionChanged(DownloadManager download,
										int oldPosition, int newPosition) {
									refreshCoreToolBarItems();
								}

								public void filePriorityChanged(DownloadManager download,
										DiskManagerFileInfo file) {
									refreshCoreToolBarItems();
								}
							};

							dm.addListener(l, false);

							dm_listener_map.put(dm, l);

							// System.out.println( "Added " + dm.getDisplayName() + " - size=" + dm_listener_map.size());
						}
					}
				}

				for (Map.Entry<DownloadManager, DownloadManagerListener> e : copy.entrySet()) {

					DownloadManager dm = e.getKey();

					dm.removeListener(e.getValue());

					dm_listener_map.remove(dm);

					// System.out.println( "Removed " + dm.getDisplayName() + " - size=" + dm_listener_map.size());
				}
			}

			if (!mapStates.containsKey("download")) {
				for (ISelectedContent content : currentContent) {
					if (content.getDownloadManager() == null
							&& content.getDownloadInfo() != null) {
						mapStates.put("download", ToolBarEnabler2.STATE_ENABLED);
						break;
					}
				}
			}
			boolean has1Selection = currentContent.length == 1;

			boolean can_play = false;
			boolean can_stream = false;

			boolean stream_permitted = false;

			if (has1Selection) {

				if (!(currentContent[0] instanceof ISelectedVuzeFileContent)) {

					can_play = PlayUtils.canPlayDS(currentContent[0],
							currentContent[0].getFileIndex());
					can_stream = PlayUtils.canStreamDS(currentContent[0],
							currentContent[0].getFileIndex());

					if (can_stream) {

						stream_permitted = PlayUtils.isStreamPermitted();
					}
				}
			}

			// allow a tool-bar enabler to manually handle play/stream events

			if (mapStates.containsKey("play")) {
				can_play |= (mapStates.get("play") & ToolBarEnabler2.STATE_ENABLED) > 0;
			}
			if (mapStates.containsKey("stream")) {
				can_stream |= (mapStates.get("stream") & ToolBarEnabler2.STATE_ENABLED) > 0;
			}

			mapStates.put("play", can_play | can_stream
					? ToolBarEnabler2.STATE_ENABLED : 0);

			UIToolBarItem pitem = getToolBarItem("play");

			if (pitem != null) {

				if (can_stream) {

					pitem.setImageID(stream_permitted ? "image.button.stream"
							: "image.button.pstream");
					pitem.setTextID(stream_permitted ? "iconBar.stream"
							: "iconBar.pstream");

				} else {

					pitem.setImageID("image.button.play");
					pitem.setTextID("iconBar.play");
				}
			}

			UIToolBarItem ssItem = getToolBarItem("startstop");
			if (ssItem != null) {
				boolean shouldStopGroup = TorrentUtil.shouldStopGroup(currentContent);
				ssItem.setTextID(shouldStopGroup ? "iconBar.stop" : "iconBar.start");
				ssItem.setImageID("image.toolbar.startstop."
						+ (shouldStopGroup ? "stop" : "start"));
			}

			for (int i = 0; i < allToolBarItems.length; i++) {
				UIToolBarItem toolBarItem = allToolBarItems[i];
				if (toolBarItem.isAlwaysAvailable()) {
					toolBarItem.setEnabled(true);
				} else {
					Long state = mapStates.get(toolBarItem.getID());
					if (state != null) {
						toolBarItem.setEnabled((state & ToolBarEnabler2.STATE_ENABLED) > 0);
					}
				}
			}
			return;
		}
	}

	private boolean triggerViewToolBar(ToolBarItem item, long activationType,
			Object datasource) {
		MultipleDocumentInterfaceSWT mdi = UIFunctionsManagerSWT.getUIFunctionsSWT().getMDISWT();
		if (mdi != null) {
			MdiEntrySWT entry = mdi.getCurrentEntrySWT();
			ToolBarEnablerBase[] enablers = entry.getToolbarEnablers();
			for (ToolBarEnablerBase enabler : enablers) {
				if (enabler instanceof ToolBarEnabler2) {
					if (((ToolBarEnabler2) enabler).toolBarItemActivated(item,
							activationType, datasource)) {
						return true;
					}
				} else if (enabler instanceof ToolBarEnabler) {
					if (activationType == UIToolBarActivationListener.ACTIVATIONTYPE_NORMAL
							&& ((ToolBarEnabler) enabler).toolBarItemActivated(item.getID())) {
						return true;
					}
				}
			}
		}

		return false;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.ui.toolbar.UIToolBarManager#createToolBarItem(java.lang.String)
	 */
	public UIToolBarItem createToolBarItem(String id) {
		return new ToolBarItemSO(this, id, true);
	}

	public UIToolBarItem createToolBarItem(PluginInterface pi, String id) {
		return new ToolBarItemSO(this, id, true);
	}

	public void addToolBarItem(UIToolBarItem item) {
		if (item instanceof ToolBarItemSO) {
			ToolBarItemSO itemSO = (ToolBarItemSO) item;
			itemSO.setGroupID("plugin");
			//addNonToolBar("toolbar.area.sitem.left2", so2nd);
			addToolBarItem(itemSO, "toolbar.area.sitem", so2nd);
		}
	}

	public void addToolBarItemNoCreate(final ToolBarItemSO item) {
		addToolBarItem(item, null, null);
	}

	public void addToolBarItem(final ToolBarItemSO item, String templatePrefix,
			SWTSkinObject soMain) {
		String groupID = item.getGroupID();
		
		int position = SWT.RIGHT;

		synchronized (mapGroupToItemIDs) {
			List<String> list = mapGroupToItemIDs.get(groupID);
			if (list == null) {
				list = new ArrayList<String>();
				mapGroupToItemIDs.put(groupID, list);
				position = SWT.LEFT;
			} else if (soMain != null && !groupID.equals(GROUP_BIG)) {
				// take the last item and change it to RIGHT
				int size = list.size();
				String lastID = list.get(size - 1);
				ToolBarItemSO lastItem = getToolBarItemSO(lastID);
				if (lastItem != null) {
					SWTSkinObject so = skin.getSkinObjectByID("toolbar:" + lastItem.getID());
					if (so != null) {
						String configID = so.getConfigID();
						if ((size == 1 && !configID.endsWith(".left")) || !configID.equals(templatePrefix)) {
    					setupToolBarItem(lastItem, templatePrefix, soMain, size == 1 ? SWT.LEFT
    							: SWT.CENTER);
						}
					}
				}

				addSeperator(soMain);
			}
			list.add(item.getID());
		}
		
		if (soMain != null) {
			setupToolBarItem(item, templatePrefix, soMain, groupID.equals(GROUP_BIG)
					? 0 : position);
		} else {
			items.put(item.getID(), item);
		}
	}
	
	private void bulkSetupItems(String groupID, String templatePrefix, SWTSkinObject soMain) {
		synchronized (mapGroupToItemIDs) {
			List<String> list = mapGroupToItemIDs.get(groupID);
			if (list == null) {
				return;
			}
			for (int i = 0; i < list.size(); i++) {
				String itemID = list.get(i);
				SWTSkinObject so = skin.getSkinObjectByID("toolbar:" + itemID);
				if (so != null) {
					so.dispose();
				}
				ToolBarItemSO item = getToolBarItemSO(itemID);
				if (item != null) {
					int position = 0;
					int size = list.size();
					if (size == 1) {
						position = SWT.SINGLE;
					} else if (i == 0) {
						position = SWT.LEFT;
					} else if (i == size - 1) {
						addSeperator(soMain);
						position = SWT.RIGHT;
					} else {
						addSeperator(soMain);
					}
					setupToolBarItem(item, templatePrefix, soMain, position);
				}
				
			}
		}
	}

	private void setupToolBarItem(final ToolBarItemSO item, String templatePrefix,
			SWTSkinObject soMain, int position) {
		String templateID = templatePrefix;
		if (position == SWT.RIGHT) {
			templateID += ".right";
		} else if (position == SWT.LEFT) {
			templateID += ".left";
		} else if (position == SWT.SINGLE) {
			templateID += ".lr";
		}

		Control attachToControl = this.lastControl;
		String id = "toolbar:" + item.getID();
		SWTSkinObject oldSO = skin.getSkinObjectByID(id);
		if (oldSO != null) {
			Object layoutData = oldSO.getControl().getLayoutData();
			if (layoutData instanceof FormData) {
				FormData fd = (FormData) layoutData;
				if (fd.left != null) {
					attachToControl = fd.left.control;
				}
			}
			oldSO.dispose();
		}
		SWTSkinObject so = skin.createSkinObject(id, templateID, soMain);
		if (so != null) {
			System.out.println("CREATE " + so.getSkinObjectID());
			so.setTooltipID(item.getTooltipID());

			if (lastControl != null) {
				FormData fd = (FormData) so.getControl().getLayoutData();
				fd.left = new FormAttachment(attachToControl);
			}

			so.setData("toolbaritem", item);
			SWTSkinButtonUtility btn = new SWTSkinButtonUtility(so,
					"toolbar-item-image");
			btn.setImage(item.getImageID());
			btn.addSelectionListener(buttonListener);
			item.setSkinButton(btn);

			SWTSkinObject soTitle = skin.getSkinObject("toolbar-item-title", so);
			if (soTitle instanceof SWTSkinObjectText) {
				((SWTSkinObjectText) soTitle).setTextID(item.getTextID());
				item.setSkinTitle((SWTSkinObjectText) soTitle);
			}

			if (initComplete) {
				Utils.relayout(so.getControl().getParent());
			}

			lastControl = item.getSkinButton().getSkinObject().getControl();
		}
		items.put(item.getID(), item);
	}

	private void addSeperator(SWTSkinObject soMain) {
		addSeperator("toolbar.area.sitem.sep", soMain);
	}

	private void addSeperator(String id, SWTSkinObject soMain) {
		SWTSkinObject so = skin.createSkinObject("toolbar_sep" + Math.random(), id,
				soMain);
		if (so != null) {
			if (lastControl != null) {
				FormData fd = (FormData) so.getControl().getLayoutData();
				fd.left = new FormAttachment(lastControl, fd.left == null ? 0
						: fd.left.offset);
			}

			lastControl = so.getControl();
		}
	}

	private void addNonToolBar(String skinid, SWTSkinObject soMain) {
		SWTSkinObject so = skin.createSkinObject("toolbar_d" + Math.random(),
				skinid, soMain);
		if (so != null) {
			if (lastControl != null) {
				FormData fd = (FormData) so.getControl().getLayoutData();
				fd.left = new FormAttachment(lastControl, fd.left == null ? 0
						: fd.left.offset);
			}

			lastControl = so.getControl();
		}
	}

	/**
	 * @param showText the showText to set
	 */
	public void setShowText(boolean showText) {
		this.showText = showText;
		UIToolBarItem[] allToolBarItems = getAllToolBarItems();
		for (int i = 0; i < allToolBarItems.length; i++) {
			UIToolBarItem tbi = allToolBarItems[i];
			SWTSkinObject so = ((ToolBarItemSO) tbi).getSkinButton().getSkinObject();
			SWTSkinObject soTitle = skin.getSkinObject("toolbar-item-title", so);
			soTitle.setVisible(showText);
		}
	}

	/**
	 * @return the showText
	 */
	public boolean getShowText() {
		return showText;
	}

	private static class toolbarButtonListener
		extends ButtonListenerAdapter
	{
		public void pressed(SWTSkinButtonUtility buttonUtility,
				SWTSkinObject skinObject, int stateMask) {
			ToolBarItem item = (ToolBarItem) buttonUtility.getSkinObject().getData(
					"toolbaritem");
			boolean rightClick = (stateMask & (SWT.BUTTON3 | SWT.MOD4)) > 0;
			item.triggerToolBarItem(rightClick
					? UIToolBarActivationListener.ACTIVATIONTYPE_RIGHTCLICK
					: UIToolBarActivationListener.ACTIVATIONTYPE_NORMAL, null);
		}

		public boolean held(SWTSkinButtonUtility buttonUtility) {
			ToolBarItem item = (ToolBarItem) buttonUtility.getSkinObject().getData(
					"toolbaritem");
			buttonUtility.getSkinObject().switchSuffix("", 0, false, true);

			boolean triggerToolBarItemHold = item.triggerToolBarItem(
					UIToolBarActivationListener.ACTIVATIONTYPE_HELD, null);
			return triggerToolBarItemHold;
		}

		public void disabledStateChanged(SWTSkinButtonUtility buttonUtility,
				boolean disabled) {
			ToolBarItem item = (ToolBarItem) buttonUtility.getSkinObject().getData(
					"toolbaritem");
			item.setEnabled(!disabled);
		}
	}

	public void flipShowText() {
		ToolBarView tb = (ToolBarView) SkinViewManager.getByClass(ToolBarView.class);
		if (tb == null) {
			SkinViewManager.addListener(new SkinViewManagerListener() {
				public void skinViewAdded(SkinView skinview) {
					if (skinview instanceof ToolBarView) {
						SkinViewManager.RemoveListener(this);
						flipShowText();
					}
				}
			});
			return;
		}

		try {
			boolean showText = !tb.getShowText();
			COConfigurationManager.setParameter("ToolBar.showText", showText);
			tb.setShowText(showText);

			SWTSkinObject skinObject;
			skinObject = skin.getSkinObject("search-text");
			if (skinObject != null) {
				Control control = skinObject.getControl();
				FormData fd = (FormData) control.getLayoutData();
				fd.top.offset = showText ? 6 : 5;
				fd.bottom.offset = showText ? -3 : -2;
			}
			skinObject = skin.getSkinObject("topgap");
			if (skinObject != null) {
				Control control = skinObject.getControl();
				FormData fd = (FormData) control.getLayoutData();
				fd.height = showText ? 6 : 2;
			}
			skinObject = skin.getSkinObject("tabbar");
			if (skinObject != null) {
				Control control = skinObject.getControl();
				FormData fd = (FormData) control.getLayoutData();
				fd.height = showText ? 50 : 32;
				//Utils.relayout(control);
				skinObject.switchSuffix(showText ? "" : "-small", 4, true);

				Shell shell = control.getShell();
				shell.layout(true, true);
				shell.redraw();
			}

		} catch (Exception e) {
			Debug.out(e);
		}
	}

	public void addListener(ToolBarViewListener l) {
		synchronized (listeners) {
			listeners.add(l);

			if (initComplete) {
				try {
					l.toolbarViewInitialized(this);
				} catch (Exception e) {
					Debug.out(e);
				}
			}
		}
	}

	public void removeListener(ToolBarViewListener l) {
		synchronized (listeners) {
			listeners.remove(l);
		}
	}

	public interface ToolBarViewListener
	{
		public void toolbarViewInitialized(ToolBarView tbv);
	}

	public void removeToolBarItem(String id) {
		UIToolBarItem toolBarItem = items.remove(id);
		if (toolBarItem instanceof ToolBarItemSO) {
			ToolBarItemSO item = (ToolBarItemSO) toolBarItem;
			item.dispose();
			SWTSkinObject so = skin.getSkinObjectByID("toolbar:" + item.getID());
			if (so != null) {
				so.dispose();
			}
		}
	}
}
