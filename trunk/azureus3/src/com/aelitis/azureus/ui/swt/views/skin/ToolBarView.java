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
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FrequencyLimitedDispatcher;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.ui.swt.TorrentUtil;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.devices.DeviceManager;
import com.aelitis.azureus.core.devices.DeviceManagerFactory;
import com.aelitis.azureus.core.devices.TranscodeException;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.common.ToolBarEnabler;
import com.aelitis.azureus.ui.selectedcontent.*;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.devices.DeviceManagerUI;
import com.aelitis.azureus.ui.swt.devices.TranscodeChooser;
import com.aelitis.azureus.ui.swt.mdi.MdiEntrySWT;
import com.aelitis.azureus.ui.swt.mdi.MultipleDocumentInterfaceSWT;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.toolbar.ToolBarItem;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager.SkinViewManagerListener;
import com.aelitis.azureus.util.DLReferals;
import com.aelitis.azureus.util.PlayUtils;

/**
 * @author TuxPaper
 * @created Jul 20, 2008
 */
public class ToolBarView
	extends SkinView
{
	private static boolean DEBUG = false;

	private static toolbarButtonListener buttonListener;

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

		ToolBarItem item;

		if (!uiClassic) {
			// ==download
			item = new ToolBarItem("download", "image.button.download",
					"v3.MainWindow.button.download") {
				// @see com.aelitis.azureus.ui.swt.toolbar.ToolBarItem#triggerToolBarItem()
				public void triggerToolBarItem() {
					String viewID = SelectedContentManager.getCurrentySelectedViewID();
					if (viewID == null && triggerIViewToolBar(getId())) {
						return;
					}
					// This is for our CDP pages
					ISelectedContent[] sc = SelectedContentManager.getCurrentlySelectedContent();
					if (sc != null && sc.length == 1
							&& (sc[0].getHash() != null || sc[0].getDownloadInfo() != null)) {
						TorrentListViewsUtils.downloadDataSource(sc[0], false,
								DLReferals.DL_REFERAL_TOOLBAR);
					}
				}
			};
			addToolBarItem(item);

			// ==play
			item = new ToolBarItem("play", "image.button.play", "iconBar.play") {
				// @see com.aelitis.azureus.ui.swt.toolbar.ToolBarItem#triggerToolBarItem()
				public void triggerToolBarItem() {
					String viewID = SelectedContentManager.getCurrentySelectedViewID();
					if (viewID == null && triggerIViewToolBar(getId())) {
						return;
					}
					ISelectedContent[] sc = SelectedContentManager.getCurrentlySelectedContent();
					if (sc != null) {
						
						if ( PlayUtils.canStreamDS(sc[0], sc[0].getFileIndex())){
							TorrentListViewsUtils.playOrStreamDataSource(sc[0],
									DLReferals.DL_REFERAL_TOOLBAR, true, false);
						}else{
							TorrentListViewsUtils.playOrStreamDataSource(sc[0],
								DLReferals.DL_REFERAL_TOOLBAR, false, true);
						}
					}
				}
			};
			addToolBarItem(item);
			
			addSeperator((uiClassic ? "classic." : "") + "toolbar.area.item.sep",
					soMain);

			lastControl = null;

		} else {

			lastControl = null;

			// ==OPEN
			item = new ToolBarItem("open", "image.toolbar.open", "Button.add") {
				public void triggerToolBarItem() {
					TorrentOpener.openTorrentWindow();
				}
			};
			item.setAlwaysAvailable(true);
			addToolBarItem(item, "toolbar.area.sitem.left", so2nd);

			addSeperator(so2nd);

			// ==SEARCH
			item = new ToolBarItem("search", "search", "Button.search") {
				public void triggerToolBarItem() {
					UIFunctionsManagerSWT.getUIFunctionsSWT().promptForSearch();
				}
			};
			item.setAlwaysAvailable(true);
			addToolBarItem(item, "toolbar.area.sitem.right", so2nd);

			addSeperator((uiClassic ? "classic." : "") + "toolbar.area.item.sep3",
					so2nd);

			addNonToolBar("toolbar.area.sitem.left2", so2nd);
		}

		boolean first = true;

		// ==transcode
		if (!DeviceManagerUI.DISABLED) {
			item = new ToolBarItem("transcode", "image.button.transcode",
					"iconBar.transcode") {
				// @see com.aelitis.azureus.ui.swt.toolbar.ToolBarItem#triggerToolBarItem()
				public void triggerToolBarItem() {
					String viewID = SelectedContentManager.getCurrentySelectedViewID();
					if (viewID == null && triggerIViewToolBar(getId())) {
						return;
					}
					ISelectedContent[] contents = SelectedContentManager.getCurrentlySelectedContent();
					if (contents.length == 0) {
						return;
					}

					deviceSelected(contents, true);
				}
			};
			addToolBarItem(item, first ? "toolbar.area.sitem.left"
					: "toolbar.area.sitem", so2nd);
			first = false;
			addSeperator(so2nd);
		}

		// ==run
		item = new ToolBarItem("run", "image.toolbar.run", "iconBar.run") {
			public void triggerToolBarItem() {
				if (!triggerBasicToolBarItem(getId())) {
					DownloadManager[] dms = SelectedContentManager.getDMSFromSelectedContent();
					if (dms != null) {
						TorrentUtil.runDataSources(dms);

						for (int i = 0; i < dms.length; i++) {
							DownloadManager dm = dms[i];
							PlatformTorrentUtils.setHasBeenOpened(dm, true);
						}
					}
				}
			}
		};
		addToolBarItem(item, first ? "toolbar.area.sitem.left"
				: "toolbar.area.sitem", so2nd);
		first = false;
		//addToolBarItem(item, "toolbar.area.sitem", so2nd);
		addSeperator(so2nd);

		if (uiClassic) {
			// ==TOP
			item = new ToolBarItem("top", "image.toolbar.top", "iconBar.top") {
				public void triggerToolBarItem() {
					moveTop();
				}

				public boolean triggerToolBarItemHold() {
					return false;
				}
			};
			addToolBarItem(item, "toolbar.area.sitem", so2nd);
			addSeperator(so2nd);
		}

		// ==UP
		item = new ToolBarItem("up", "image.toolbar.up", "v3.iconBar.up") {
			public void triggerToolBarItem() {
				if (!AzureusCoreFactory.isCoreRunning()) {
					return;
				}
				if (!triggerBasicToolBarItem(getId())) {
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
				}
			}

			// @see com.aelitis.azureus.ui.swt.toolbar.ToolBarItem#triggerToolBarItemHold()
			public boolean triggerToolBarItemHold() {
				return moveTop();
			}
		};
		addToolBarItem(item, "toolbar.area.sitem", so2nd);
		addSeperator(so2nd);

		// ==down
		item = new ToolBarItem("down", "image.toolbar.down", "v3.iconBar.down") {
			public void triggerToolBarItem() {
				if (!AzureusCoreFactory.isCoreRunning()) {
					return;
				}

				if (!triggerBasicToolBarItem(getId())) {
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
					}
				}
			}

			// @see com.aelitis.azureus.ui.swt.toolbar.ToolBarItem#triggerToolBarItemHold()
			public boolean triggerToolBarItemHold() {
				return moveBottom();
			}
		};
		addToolBarItem(item, "toolbar.area.sitem", so2nd);
		addSeperator(so2nd);

		if (uiClassic) {
			// ==BOTTOM
			item = new ToolBarItem("bottom", "image.toolbar.bottom", "iconBar.bottom") {
				public void triggerToolBarItem() {
					moveBottom();
				}

				public boolean triggerToolBarItemHold() {
					return false;
				}
			};
			addToolBarItem(item, "toolbar.area.sitem", so2nd);
			addSeperator(so2nd);
		}

		// ==start
		item = new ToolBarItem("start", "image.toolbar.start", "iconBar.start") {
			public void triggerToolBarItem() {
				if (!triggerBasicToolBarItem(getId())) {
					DownloadManager[] dms = SelectedContentManager.getDMSFromSelectedContent();
					if (dms != null) {
						TorrentUtil.queueDataSources(dms, true);
					}
				}
			}
		};
		addToolBarItem(item, "toolbar.area.sitem", so2nd);
		//SWTSkinObjectContainer so = (SWTSkinObjectContainer) item.getSkinButton().getSkinObject();
		//so.setDebugAndChildren(true);
		addSeperator(so2nd);

		// ==stop
		item = new ToolBarItem("stop", "image.toolbar.stop", "iconBar.stop") {
			public void triggerToolBarItem() {
				if (!triggerBasicToolBarItem(getId())) {
  				ISelectedContent[] currentContent = SelectedContentManager.getCurrentlySelectedContent();
					TorrentUtil.stopDataSources(currentContent);
				}
			}
		};
		addToolBarItem(item, "toolbar.area.sitem", so2nd);
		addSeperator(so2nd);

		// ==remove
		item = new ToolBarItem("remove", "image.toolbar.remove", "iconBar.remove") {
			public void triggerToolBarItem() {
				if (!triggerBasicToolBarItem(getId())) {
					DownloadManager[] dms = SelectedContentManager.getDMSFromSelectedContent();
					TorrentUtil.removeDownloads(dms, null);
				}
			}
		};
		addToolBarItem(item, "toolbar.area.sitem.right", so2nd);

		///////////////////////

		addSeperator((uiClassic ? "classic." : "") + "toolbar.area.item.sep3",
				so2nd);

		addNonToolBar("toolbar.area.sitem.left2", so2nd);

		// == mode big
		item = new ToolBarItem("modeBig", "image.toolbar.table_large",
				"v3.iconBar.view.big") {
			public void triggerToolBarItem() {
				triggerBasicToolBarItem(getId());
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
		addToolBarItem(item, "toolbar.area.vitem.left", so2nd);

		SWTSkinObject soTitle = skin.getSkinObject("toolbar-item-title",
				item.getSkinButton().getSkinObject());
		if (soTitle instanceof SWTSkinObjectText) {
			((SWTSkinObjectText) soTitle).setStyle(SWT.RIGHT);
		}

		addSeperator(so2nd);

		// == mode small
		item = new ToolBarItem("modeSmall", "image.toolbar.table_normal",
				"v3.iconBar.view.small") {
			public void triggerToolBarItem() {
				triggerBasicToolBarItem(getId());
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
		addToolBarItem(item, "toolbar.area.vitem.right", so2nd);

		soTitle = skin.getSkinObject("toolbar-item-title",
				item.getSkinButton().getSkinObject());
		if (soTitle instanceof SWTSkinObjectText) {
			((SWTSkinObjectText) soTitle).setStyle(SWT.LEFT);
		}

		//addSeperator(so2nd);

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

	protected boolean triggerBasicToolBarItem(String itemKey) {
		if (triggerIViewToolBar(itemKey)) {
			return true;
		}

		if (DEBUG) {
			String viewID = SelectedContentManager.getCurrentySelectedViewID();
  		System.out.println("Warning: Fallback of toolbar button " + itemKey
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
						DiskManagerFileInfo[] files = dm.getDiskManagerFileInfo();
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


	protected void updateCoreItems(ISelectedContent[] currentContent,
			String viewID) {
		Map<String, Boolean> mapNewToolbarStates = TorrentUtil.calculateToolbarStates(
				currentContent, viewID);
		
		for (String key : mapNewToolbarStates.keySet()) {
			Boolean enable = mapNewToolbarStates.get(key);
			ToolBarItem item = getToolBarItem(key);
			if (item != null) {
				item.setEnabled(enable);
			}
		}
	}

	/**
	 * @param toolBarItem
	 *
	 * @since 3.1.1.1
	 */
	protected void activateViaSideBar(ToolBarItem toolBarItem) {
		MultipleDocumentInterfaceSWT mdi = UIFunctionsManagerSWT.getUIFunctionsSWT().getMDISWT();
		if (mdi != null) {
			MdiEntrySWT entry = mdi.getCurrentEntrySWT();
			if (entry.getIView() != null) {
				entry.getIView().itemActivated(toolBarItem.getId());
			}
		}
	}

	/**
	 * @param itemID
	 * @return
	 *
	 * @since 3.1.1.1
	 */
	public ToolBarItem getToolBarItem(String itemID) {
		return items.get(itemID);
	}

	public ToolBarItem[] getAllToolBarItems() {
		return items.values().toArray(new ToolBarItem[0]);
	}

	private FrequencyLimitedDispatcher refresh_limiter = 
		new FrequencyLimitedDispatcher(
			new AERunnable()
			{
				private AERunnable	lock = this;
				private boolean 	refresh_pending;
				
				public void
				runSupport()
				{
					synchronized( lock ){
						
						if ( refresh_pending ){
							
							return;
						}
						refresh_pending = true;
					}
					
					if ( DEBUG ){
						System.out.println("refreshCoreItems via " + Debug.getCompressedStackTrace());
					}
					
					Utils.execSWTThread(
						new AERunnable() 
						{
							public void 
							runSupport() {
						
								synchronized( lock ){
								
									refresh_pending = false;
								}
							
								_refreshCoreToolBarItems();
							}
						});
				}
			},
			250 );
	
	private Map<DownloadManager,DownloadManagerListener> dm_listener_map = new HashMap<DownloadManager, DownloadManagerListener>();
	
	public void refreshCoreToolBarItems() {
		refresh_limiter.dispatch();
	}

	public void _refreshCoreToolBarItems() {
		
		MultipleDocumentInterfaceSWT mdi = UIFunctionsManagerSWT.getUIFunctionsSWT().getMDISWT();

		if (mdi != null) {
			ToolBarItem[] allToolBarItems = getAllToolBarItems();
			MdiEntrySWT entry = mdi.getCurrentEntrySWT();
			Map<String, Boolean> mapStates = new HashMap<String, Boolean>();
			if (entry != null) {
  			ToolBarEnabler[] enablers = entry.getToolbarEnablers();
  			for (ToolBarEnabler enabler : enablers) {
  				enabler.refreshToolBar(mapStates);
  			}
			}
			
			ISelectedContent[] currentContent = SelectedContentManager.getCurrentlySelectedContent();
							
			synchronized( dm_listener_map ){
		
				Map<DownloadManager,DownloadManagerListener> copy = new HashMap<DownloadManager, DownloadManagerListener>( dm_listener_map );
				
				for ( ISelectedContent content : currentContent ){
					
					DownloadManager dm = content.getDownloadManager();
					
					if ( dm != null ){
						
						if ( copy.remove( dm ) == null ){
							
							DownloadManagerListener l = 
								new DownloadManagerListener()
								{
									public void
									stateChanged(
										DownloadManager manager,
										int		state )
									{
										refreshCoreToolBarItems();
									}
										
									public void
									downloadComplete(
										DownloadManager manager)
									{
										refreshCoreToolBarItems();
									}
		
									public void
									completionChanged(
										DownloadManager manager, 
										boolean bCompleted )
									{
										refreshCoreToolBarItems();
									}
		
									public void
									positionChanged(
										DownloadManager download, 
										int oldPosition, 
										int newPosition)
									{
										refreshCoreToolBarItems();
									}
								  
									public void
									filePriorityChanged( 
										DownloadManager download, DiskManagerFileInfo file )
									{
										refreshCoreToolBarItems();
									}
								};
																
							dm.addListener( l, false );
							
							dm_listener_map.put( dm, l );
							
							// System.out.println( "Added " + dm.getDisplayName() + " - size=" + dm_listener_map.size());
						}
					}
				}
				
				for ( Map.Entry<DownloadManager,DownloadManagerListener> e: copy.entrySet()){
				
					DownloadManager dm = e.getKey();
										
					dm.removeListener( e.getValue());
					
					dm_listener_map.remove( dm );
					
					// System.out.println( "Removed " + dm.getDisplayName() + " - size=" + dm_listener_map.size());
				}
			}
			
			if (!mapStates.containsKey("download")) {
				for (ISelectedContent content : currentContent) {
					if (content.getDownloadManager() == null
							&& content.getDownloadInfo() != null) {
						mapStates.put("download", true);
						break;
					}
				}
			}
			boolean has1Selection = currentContent.length == 1;
			
			boolean can_play	= false;
			boolean can_stream	= false;
			
			if ( has1Selection ){
				
				if ( !(currentContent[0] instanceof ISelectedVuzeFileContent)){
					
					can_play 	= PlayUtils.canPlayDS(currentContent[0], currentContent[0].getFileIndex());
					can_stream	= PlayUtils.canStreamDS(currentContent[0], currentContent[0].getFileIndex());
				}
			}
			
			mapStates.put( "play", can_play | can_stream );

			ToolBarItem pitem = getToolBarItem( "play" );
			
			if ( pitem != null ){
			
				pitem.setImageID( can_stream?"image.button.stream":"image.button.play" );
				pitem.setTextID( can_stream?"iconBar.stream":"iconBar.play" );
			}
			
			for (int i = 0; i < allToolBarItems.length; i++) {
				ToolBarItem toolBarItem = allToolBarItems[i];
				if (toolBarItem.isAlwaysAvailable()) {
					toolBarItem.setEnabled(true);
				} else {
					Boolean b = mapStates.get(toolBarItem.getId());
					if (b == null) {
						b = false;
					}
					toolBarItem.setEnabled(b);
				}
			}
			return;
		}
	}

	private boolean triggerIViewToolBar(String id) {
		MultipleDocumentInterfaceSWT mdi = UIFunctionsManagerSWT.getUIFunctionsSWT().getMDISWT();
		if (mdi != null) {
			MdiEntrySWT entry = mdi.getCurrentEntrySWT();
			ToolBarEnabler[] enablers = entry.getToolbarEnablers();
			for (ToolBarEnabler enabler : enablers) {
				if (enabler.toolBarItemActivated(id)) {
					return true;
				}
			}
		}
		return false;
	}

	public void addToolBarItem(final ToolBarItem item) {
		addToolBarItem(item, "toolbar.area.item", soMain);
	}

	public void addToolBarItem(final ToolBarItem item, String templateID,
			SWTSkinObject soMain) {
		SWTSkinObject so = skin.createSkinObject("toolbar:" + item.getId(),
				templateID, soMain);
		if (so != null) {
			so.setTooltipID(item.getTooltipID());

			if (lastControl != null) {
				FormData fd = (FormData) so.getControl().getLayoutData();
				fd.left = new FormAttachment(lastControl);
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
				item.setSkinTitle((SWTSkinObjectText)soTitle);
			}

			if (initComplete) {
				Utils.relayout(so.getControl().getParent());
			}

			lastControl = item.getSkinButton().getSkinObject().getControl();
			items.put(item.getId(), item);
		}
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
		ToolBarItem[] allToolBarItems = getAllToolBarItems();
		for (int i = 0; i < allToolBarItems.length; i++) {
			ToolBarItem tbi = allToolBarItems[i];
			SWTSkinObject so = tbi.getSkinButton().getSkinObject();
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
			item.triggerToolBarItem();
		}

		public boolean held(SWTSkinButtonUtility buttonUtility) {
			ToolBarItem item = (ToolBarItem) buttonUtility.getSkinObject().getData(
					"toolbaritem");
			buttonUtility.getSkinObject().switchSuffix("", 0, false, true);

			boolean triggerToolBarItemHold = item.triggerToolBarItemHold();
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

}
