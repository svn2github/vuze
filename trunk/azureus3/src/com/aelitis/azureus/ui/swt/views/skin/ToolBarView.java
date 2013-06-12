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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Control;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FrequencyLimitedDispatcher;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarActivationListener;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarEnablerBase;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem;
import org.gudy.azureus2.ui.swt.TorrentUtil;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UIToolBarManagerCore;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.common.ToolBarEnabler;
import com.aelitis.azureus.ui.common.ToolBarItem;
import com.aelitis.azureus.ui.selectedcontent.*;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.mdi.MdiEntrySWT;
import com.aelitis.azureus.ui.swt.mdi.MultipleDocumentInterfaceSWT;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.toolbar.ToolBarItemSO;
import com.aelitis.azureus.util.DLReferals;
import com.aelitis.azureus.util.PlayUtils;
import com.aelitis.azureus.util.StringCompareUtils;

/**
 * @author TuxPaper
 * @created Jul 20, 2008
 */
public class ToolBarView
	extends SkinView
	implements UIToolBarManagerCore, SelectedContentListener
{
	private static boolean DEBUG = false;

	private static toolbarButtonListener buttonListener;

	private Map<String, List<String>> mapGroupToItemIDs = new HashMap<String, List<String>>();
	
	private Map<String, ToolBarItem> items = new LinkedHashMap<String, ToolBarItem>();

	//private GlobalManager gm;

	Control lastControl = null;

	private boolean showText = true;

	private SWTSkinObject so2nd;

	private boolean initComplete = false;
	
	private boolean showCalled = false;

	private ArrayList<ToolBarViewListener> listeners = new ArrayList<ToolBarViewListener>(
			1);

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#showSupport(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectInitialShow(SWTSkinObject skinObject,
			Object params) {
		boolean uiClassic = COConfigurationManager.getStringParameter("ui").equals(
				"az2");
		
		if (uiClassic && !"global-toolbar".equals(skinObject.getViewID())) {
			skinObject.setVisible(false);
			return null;
		}

		buttonListener = new toolbarButtonListener();
		//SWTSkin skin = skinObject.getSkin();
		so2nd = skin.getSkinObject("toolbar-2nd", skinObject);
		
		if (so2nd == null) {
			skinObject.setVisible(false);
			return null;
		}

		ToolBarItemSO item;

		if (uiClassic) {
			lastControl = null;

			// ==OPEN
			item = new ToolBarItemSO(this, "open", "image.toolbar.open", "Button.add");
			item.setDefaultActivationListener(new UIToolBarActivationListener() {
				public boolean toolBarItemActivated(ToolBarItem item,
						long activationType, Object datasource) {
					if (activationType != ACTIVATIONTYPE_NORMAL) {
						return false;
					}
					TorrentOpener.openTorrentWindow( false );
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

		if (!uiClassic) {
			// ==play
			item = new ToolBarItemSO(this, "play", "image.button.play",
					"iconBar.play");
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
		
		Utils.execSWTThreadLater(0, new Runnable() {
			public void run() {
				Utils.relayout(so2nd.getControl());
			}
		});

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

	String lastViewID = null;
	public void currentlySelectedContentChanged(
			ISelectedContent[] currentContent, String viewID) {
		//System.err.println("currentlySelectedContentChanged " + viewID + ";" + currentContent + ";" + getMainSkinObject() + this + " via " + Debug.getCompressedStackTrace());
		if (!StringCompareUtils.equals(lastViewID, viewID)) {
			lastViewID = viewID;
			ToolBarItem[] allToolBarItems = getAllSWTToolBarItems();
			for (int i = 0; i < allToolBarItems.length; i++) {
				UIToolBarItem toolBarItem = allToolBarItems[i];
				if (toolBarItem instanceof ToolBarItemSO) {
					toolBarItem.setState(((ToolBarItemSO) toolBarItem).getDefaultState());
				} else {
					toolBarItem.setState(0);
				}
			}
		}
		refreshCoreToolBarItems();
		//updateCoreItems(currentContent, viewID);
		UIFunctionsManagerSWT.getUIFunctionsSWT().refreshTorrentMenu();
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectShown(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectShown(SWTSkinObject skinObject, Object params) {
		
		if (showCalled) {
			return null;
		}
		showCalled = true;
		SelectedContentManager.addCurrentlySelectedContentListener(this);

		return super.skinObjectShown(skinObject, params);
	}
	
	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectHidden(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectHidden(SWTSkinObject skinObject, Object params) {
		showCalled = false;
		SelectedContentManager.removeCurrentlySelectedContentListener(this);

		return super.skinObjectHidden(skinObject, params);
	}

	public boolean triggerToolBarItem(ToolBarItem item, long activationType,
			Object datasource) {
		if (DEBUG && !isVisible()) {
			Debug.out("Trying to triggerToolBarItem when toolbar is not visible");
			return false;
		}
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

	private IdentityHashMap<DownloadManager, DownloadManagerListener> dm_listener_map = new IdentityHashMap<DownloadManager, DownloadManagerListener>();

	public void refreshCoreToolBarItems() {
		refresh_limiter.dispatch();
	}

	public void _refreshCoreToolBarItems() {
		if (DEBUG && !isVisible()) {
			Debug.out("Trying to refresh core toolbar items when toolbar is not visible " + this + getMainSkinObject());
		}

		MultipleDocumentInterfaceSWT mdi = UIFunctionsManagerSWT.getUIFunctionsSWT().getMDISWT();

		if (mdi != null) {
			UIToolBarItem[] allToolBarItems = getAllToolBarItems();
			MdiEntrySWT entry = mdi.getCurrentEntrySWT();
			Map<String, Long> mapStates = new HashMap<String, Long>();
			if (entry != null) {
				UIToolBarEnablerBase[] enablers = entry.getToolbarEnablers();
				for (UIToolBarEnablerBase enabler : enablers) {
					if (enabler instanceof UIPluginViewToolBarListener) {
						try{
							((UIPluginViewToolBarListener) enabler).refreshToolBarItems(mapStates);
						}catch( Throwable e ){
							Debug.out( e );	// don't trust them plugins
						}
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
								mapStates.put(key, curState | UIToolBarItem.STATE_ENABLED);
							} else {
								mapStates.put(key, curState & (~UIToolBarItem.STATE_ENABLED));
							}
						}
					}
				}
			}

			ISelectedContent[] currentContent = SelectedContentManager.getCurrentlySelectedContent();

			synchronized (dm_listener_map) {

				Map<DownloadManager, DownloadManagerListener> copy = new IdentityHashMap<DownloadManager, DownloadManagerListener>(
						dm_listener_map);

				for (ISelectedContent content : currentContent) {

					DownloadManager dm = content.getDownloadManager();

					if ( dm != null ){

						copy.remove( dm );
						
							// so in files view we can have multiple selections that map onto the SAME download manager
							// - ensure that we only add the listener once!
						
						if ( !dm_listener_map.containsKey( dm )) {

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

							//System.out.println( "Added " + dm.getDisplayName() + " - size=" + dm_listener_map.size());
						}
					}
				}

				for (Map.Entry<DownloadManager, DownloadManagerListener> e : copy.entrySet()) {

					DownloadManager dm = e.getKey();

					dm.removeListener(e.getValue());

					dm_listener_map.remove(dm);

					//System.out.println( "Removed " + dm.getDisplayName() + " - size=" + dm_listener_map.size());
				}
			}

			if (!mapStates.containsKey("download")) {
				for (ISelectedContent content : currentContent) {
					if (content.getDownloadManager() == null
							&& content.getDownloadInfo() != null) {
						mapStates.put("download", UIToolBarItem.STATE_ENABLED);
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
				can_play |= (mapStates.get("play") & UIToolBarItem.STATE_ENABLED) > 0;
			}
			if (mapStates.containsKey("stream")) {
				can_stream |= (mapStates.get("stream") & UIToolBarItem.STATE_ENABLED) > 0;
			}

			mapStates.put("play", can_play | can_stream
					? UIToolBarItem.STATE_ENABLED : 0);

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
				Long state = mapStates.get(toolBarItem.getID());
				if (state != null) {
					toolBarItem.setState(state);
				}
			}
			
			if ( ssItem != null ){
				
					// fallback to handle start/stop settings when no explicit selected content (e.g. for devices transcode view)
				
				if ( currentContent.length == 0 && !mapStates.containsKey( "startstop" )){
					
					boolean	can_stop 	= mapStates.containsKey("stop") && (mapStates.get("stop") & UIToolBarItem.STATE_ENABLED) > 0;
					boolean	can_start 	= mapStates.containsKey("start") && (mapStates.get("start") & UIToolBarItem.STATE_ENABLED) > 0;
					
					if ( can_start && can_stop ){
						
						can_stop = false;
					}
					
					if ( can_start | can_stop ){
						ssItem.setTextID(can_stop ? "iconBar.stop" : "iconBar.start");
						ssItem.setImageID("image.toolbar.startstop."
								+ (can_stop ? "stop" : "start"));
						
						ssItem.setState( 1 );
						
					}else{
						
						ssItem.setState( 0 );
					}
				}
			}
			
			return;
		}
	}

	private boolean triggerViewToolBar(ToolBarItem item, long activationType,
			Object datasource) {
		if (DEBUG && !isVisible()) {
			Debug.out("Trying to triggerViewToolBar when toolbar is not visible");
			return false;
		}
		MultipleDocumentInterfaceSWT mdi = UIFunctionsManagerSWT.getUIFunctionsSWT().getMDISWT();
		if (mdi != null) {
			MdiEntrySWT entry = mdi.getCurrentEntrySWT();
			UIToolBarEnablerBase[] enablers = entry.getToolbarEnablers();
			for (UIToolBarEnablerBase enabler : enablers) {
				if (enabler instanceof UIPluginViewToolBarListener) {
					if (((UIPluginViewToolBarListener) enabler).toolBarItemActivated(
							item, activationType, datasource)) {
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
		item.setDefaultState(item.getState());
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
				SWTSkinObject so = skin.getSkinObjectByID("toolbar:" + itemID, soMain);
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
		SWTSkinObject oldSO = skin.getSkinObjectByID(id, soMain);
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
			if (soTitle != null) {
				soTitle.setVisible(showText);
			}
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
					: UIToolBarActivationListener.ACTIVATIONTYPE_NORMAL,
					SelectedContentManager.convertSelectedContentToObject(null));
		}

		public boolean held(SWTSkinButtonUtility buttonUtility) {
			ToolBarItem item = (ToolBarItem) buttonUtility.getSkinObject().getData(
					"toolbaritem");
			buttonUtility.getSkinObject().switchSuffix("", 0, false, true);

			boolean triggerToolBarItemHold = item.triggerToolBarItem(
					UIToolBarActivationListener.ACTIVATIONTYPE_HELD,
					SelectedContentManager.convertSelectedContentToObject(null));
			return triggerToolBarItemHold;
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
			SWTSkinObject so = skin.getSkinObjectByID("toolbar:" + item.getID(), soMain);
			if (so != null) {
				so.dispose();
			}
		}
	}
}
