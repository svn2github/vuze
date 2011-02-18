package com.aelitis.azureus.ui.swt.mdi;

import java.util.*;

import org.eclipse.swt.SWT;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.ui.UIPluginView;
import org.gudy.azureus2.ui.swt.mainwindow.PluginsMenuHelper;
import org.gudy.azureus2.ui.swt.mainwindow.PluginsMenuHelper.IViewInfo;
import org.gudy.azureus2.ui.swt.mainwindow.PluginsMenuHelper.PluginAddedViewListener;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTInstanceImpl;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewEventListenerHolder;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.views.IView;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.mdi.*;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.views.skin.SkinView;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.util.ConstantsVuze;
import com.aelitis.azureus.util.ContentNetworkUtils;
import com.aelitis.azureus.util.MapUtils;

public abstract class BaseMDI
	extends SkinView
	implements MultipleDocumentInterfaceSWT, UIUpdatable
{
	protected MdiEntrySWT currentEntry;

	protected Map<String, MdiEntryCreationListener> mapIdToCreationListener = new LightHashMap<String, MdiEntryCreationListener>();

	// Sync changes to entry maps on mapIdEntry
	protected Map<String, MdiEntrySWT> mapIdToEntry = new LightHashMap<String, MdiEntrySWT>();

	private List<MdiListener> listeners = new ArrayList<MdiListener>();

	private List<MdiEntryLoadedListener> listLoadListeners = new ArrayList<MdiEntryLoadedListener>();

	private static Map<String, Object> mapAutoOpen = new LightHashMap<String, Object>();

	private String[] preferredOrder;

	public void addListener(MdiListener l) {
		synchronized (listeners) {
			if (listeners.contains(l)) {
				return;
			}
			listeners.add(l);
		}
	}

	public void removeListener(MdiListener l) {
		synchronized (listeners) {
			listeners.remove(l);
		}
	}

	public void addListener(MdiEntryLoadedListener l) {
		synchronized (listLoadListeners) {
			if (listLoadListeners.contains(l)) {
				return;
			}
			listLoadListeners.add(l);
		}
		// might be a very rare thread issue here if entry gets loaded while
		// we are walking through entries
		MdiEntry[] entries = getEntries();
		for (MdiEntry entry : entries) {
			if (entry.isAdded()) {
				l.mdiEntryLoaded(entry);
			}
		}
	}

	public void removeListener(MdiEntryLoadedListener l) {
		synchronized (listLoadListeners) {
			listLoadListeners.remove(l);
		}
	}
	
	protected void triggerSelectionListener(MdiEntry newEntry, MdiEntry oldEntry) {
		MdiListener[] array = listeners.toArray(new MdiListener[0]);
		for (MdiListener l : array) {
			try {
				l.mdiEntrySelected(newEntry, oldEntry);
			} catch (Exception e) {
				Debug.out(e);
			}
		}
	}

	public void triggerEntryLoadedListeners(MdiEntry entry) {
		MdiEntryLoadedListener[] array = listLoadListeners.toArray(new MdiEntryLoadedListener[0]);
		for (MdiEntryLoadedListener l : array) {
			try {
				l.mdiEntryLoaded(entry);
			} catch (Exception e) {
				Debug.out(e);
			}
		}
	}

	public void closeEntry(final String id) {
		MdiEntry entry = getEntry(id);
		if (entry != null) {
			entry.close(false);
		}
	}

	public abstract MdiEntry createEntryFromEventListener(String parentID,
			UISWTViewEventListener l, String id, boolean closeable, Object datasource);

	public abstract MdiEntry createEntryFromIView(String parentID, IView iview,
			String id, Object datasource, boolean closeable, boolean show,
			boolean expand);

	public abstract MdiEntry createEntryFromIViewClass(String parent, String id,
			String title, Class<?> iviewClass, Class<?>[] iviewClassArgs,
			Object[] iviewClassVals, Object datasource, ViewTitleInfo titleInfo,
			boolean closeable);

	/**
	 * @deprecated
	 */
	public abstract MdiEntry createEntryFromSkinRef(String parentID, String id,
			String configID, String title, ViewTitleInfo titleInfo, Object params,
			boolean closeable, int index);

	public abstract MdiEntry createEntryFromSkinRef(String parentID, String id,
			String configID, String title, ViewTitleInfo titleInfo, Object params,
			boolean closeable, String preferedAfterID);

	public MdiEntry getCurrentEntry() {
		return currentEntry;
	}

	public MdiEntrySWT getCurrentEntrySWT() {
		return currentEntry;
	}

	public MdiEntry[] getEntries() {
		return mapIdToEntry.values().toArray(new MdiEntry[0]);
	}

	public MdiEntrySWT[] getEntriesSWT() {
		return mapIdToEntry.values().toArray(new MdiEntrySWT[0]);
	}

	public MdiEntry getEntry(String id) {
		if (SkinConstants.VIEWID_BROWSER_BROWSE.equalsIgnoreCase(id)) {
			id = ContentNetworkUtils.getTarget(ConstantsVuze.getDefaultContentNetwork());
		}
		MdiEntry entry = mapIdToEntry.get(id);
		return entry;
	}

	public MdiEntrySWT getEntrySWT(String id) {
		if (SkinConstants.VIEWID_BROWSER_BROWSE.equalsIgnoreCase(id)) {
			id = ContentNetworkUtils.getTarget(ConstantsVuze.getDefaultContentNetwork());
		}
		MdiEntrySWT entry = mapIdToEntry.get(id);
		return entry;
	}

	/**
	 * @param skinView
	 * @return 
	 *
	 * @since 3.1.1.1
	 */
	public MdiEntry getEntryBySkinView(Object skinView) {
		SWTSkinObject so = ((SkinView)skinView).getMainSkinObject();
		Object[] sideBarEntries = mapIdToEntry.values().toArray();
		for (int i = 0; i < sideBarEntries.length; i++) {
			//MdiEntrySWT entry = (MdiEntrySWT) sideBarEntries[i];
			BaseMdiEntry entry = (BaseMdiEntry) sideBarEntries[i];
			SWTSkinObject entrySO = entry.getSkinObject();
			SWTSkinObject entrySOParent = entrySO == null ? entrySO
					: entrySO.getParent();
			if (entrySO == so || entrySO == so.getParent() || entrySOParent == so) {
				return entry;
			}
		}
		return null;
	}

	public IView getIViewFromID(String id) {
		if (id == null) {
			return null;
		}
		MdiEntrySWT entry = getEntrySWT(id);
		if (entry == null) {
			return null;
		}
		return entry.getIView();
	}

	public String getUpdateUIName() {
		if (currentEntry == null || currentEntry.getIView() == null) {
			return "MDI";
		}
		if (currentEntry.getIView() instanceof UIPluginView) {
			UIPluginView uiPluginView = (UIPluginView) currentEntry.getIView();
			return uiPluginView.getViewID();
		}

		return currentEntry.getIView().getFullTitle();
	}

	public void registerEntry(String id, MdiEntryCreationListener l) {
		mapIdToCreationListener.put(id, l);

		Object o = mapAutoOpen.get(id);
		if (o instanceof Map<?, ?>) {
			MdiEntryCreationListener mdiEntryCreationListener = mapIdToCreationListener.get(id);
			if (mdiEntryCreationListener != null) {
				try {
					mdiEntryCreationListener.createMDiEntry(id);
				} catch (Exception e) {
					Debug.out(e);
				}
			}
		}
	}

	public abstract boolean showEntryByID(String id);

	@Override
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		return null;
	}

	public void updateUI() {
		MdiEntry currentEntry = getCurrentEntry();
		if (currentEntry != null) {
			currentEntry.updateUI();
		}
	}

	public boolean entryExists(String id) {
		if (SkinConstants.VIEWID_BROWSER_BROWSE.equalsIgnoreCase(id)) {
			id = ContentNetworkUtils.getTarget(ConstantsVuze.getDefaultContentNetwork());
		}
		MdiEntry entry = mapIdToEntry.get(id);
		if (entry == null) {
			return false;
		}
		return entry.isAdded();
	}

	protected MdiEntry createWelcomeSection() {
		MdiEntry entry = createEntryFromSkinRef(
				SIDEBAR_HEADER_VUZE,
				SIDEBAR_SECTION_WELCOME,
				"main.area.welcome",
				MessageText.getString("v3.MainWindow.menu.getting_started").replaceAll(
						"&", ""), null, null, true, 0);
		entry.setImageLeftID("image.sidebar.welcome");
		addDropTest(entry);
		return entry;
	}

	protected void addDropTest(MdiEntry entry) {
		if (!Constants.isCVSVersion()) {
			return;
		}
		entry.addListener(new MdiEntryDropListener() {
			public boolean mdiEntryDrop(MdiEntry entry, Object droppedObject) {
				String s = "You just dropped " + droppedObject.getClass() + "\n"
						+ droppedObject + "\n\n";
				if (droppedObject.getClass().isArray()) {
					Object[] o = (Object[]) droppedObject;
					for (int i = 0; i < o.length; i++) {
						s += "" + i + ":  ";
						Object object = o[i];
						if (object == null) {
							s += "null";
						} else {
							s += object.getClass() + ";" + object;
						}
						s += "\n";
					}
				}
				new MessageBoxShell(SWT.OK, "test", s).open(null);
				return true;
			}
		});
	}

	public void setEntryAutoOpen(String id, boolean autoOpen) {
		if (!autoOpen) {
			mapAutoOpen.remove(id);
		} else {
			mapAutoOpen.put(id, new LightHashMap(0));
		}
	}

	protected void setupPluginViews() {
		UISWTInstanceImpl uiSWTInstance = (UISWTInstanceImpl) UIFunctionsManagerSWT.getUIFunctionsSWT().getUISWTInstance();
		if (uiSWTInstance != null) {
			Map<String, Map<String, UISWTViewEventListenerHolder>> allViews = uiSWTInstance.getAllViews();
			Object[] parentIDs = allViews.keySet().toArray();
			for (int i = 0; i < parentIDs.length; i++) {
				String parentID = (String) parentIDs[i];
				String sidebarParentID = null;
				if (UISWTInstance.VIEW_MYTORRENTS.equals(parentID)) {
					sidebarParentID = SideBar.SIDEBAR_HEADER_TRANSFERS;
				} else if (UISWTInstance.VIEW_MAIN.equals(parentID)) {
					sidebarParentID = MultipleDocumentInterface.SIDEBAR_HEADER_PLUGINS;
				}
				Map<String, UISWTViewEventListenerHolder> mapSubViews = allViews.get(parentID);
				if (mapSubViews != null) {
					Object[] viewIDs = mapSubViews.keySet().toArray();
					for (int j = 0; j < viewIDs.length; j++) {
						String viewID = (String) viewIDs[j];
						UISWTViewEventListener l = (UISWTViewEventListener) mapSubViews.get(viewID);
						if (l != null) {
							// TODO: Datasource
							// TODO: Multiple open

							boolean open = COConfigurationManager.getBooleanParameter(
									"SideBar.AutoOpen." + viewID, false);
							if (open) {
								createEntryFromEventListener(sidebarParentID, l, viewID, true, null);
							}
						}
					}
				}
			}
		}

		// When a new Plugin View is added, check out auto-open list to see if
		// the user had it open
		PluginsMenuHelper.getInstance().addPluginAddedViewListener(
				new PluginAddedViewListener() {
					// @see org.gudy.azureus2.ui.swt.mainwindow.PluginsMenuHelper.PluginAddedViewListener#pluginViewAdded(org.gudy.azureus2.ui.swt.mainwindow.PluginsMenuHelper.IViewInfo)
					public void pluginViewAdded(IViewInfo viewInfo) {
						//System.out.println("PluginView Added: " + viewInfo.viewID);
						Object o = mapAutoOpen.get(viewInfo.viewID);
						if (o instanceof Map<?, ?>) {
							processAutoOpenMap(viewInfo.viewID, (Map<?, ?>) o, viewInfo);
						}
					}
				});
	}

	public void informAutoOpenSet(MdiEntry entry, Map<String, Object> autoOpenInfo) {
		mapAutoOpen.put(entry.getId(), autoOpenInfo);
	}

	public void loadCloseables() {
		Map<?,?> loadedMap = FileUtil.readResilientConfigFile("sidebarauto.config", true);
		if (loadedMap.isEmpty()) {
			return;
		}
		BDecoder.decodeStrings(loadedMap);
		for (Iterator<?> iter = loadedMap.keySet().iterator(); iter.hasNext();) {
			String id = (String) iter.next();
			Object o = loadedMap.get(id);

			if (o instanceof Map<?, ?>) {
				if (!processAutoOpenMap(id, (Map<?, ?>) o, null)) {
					mapAutoOpen.put(id, o);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void saveCloseables() {
		// update title
		for (Iterator<?> iter = mapAutoOpen.keySet().iterator(); iter.hasNext();) {
			String id = (String) iter.next();
			Object o = mapAutoOpen.get(id);

			MdiEntry entry = getEntry(id);
			if (entry != null && entry.isAdded() && (o instanceof Map)) {
				Map autoOpenInfo = (Map) o;

				String s = entry.getTitle();
				if (s != null) {
					autoOpenInfo.put("title", s);
				}
			}
		}

		FileUtil.writeResilientConfigFile("sidebarauto.config", mapAutoOpen);
	}

	private boolean processAutoOpenMap(String id, Map<?, ?> autoOpenInfo,
			IViewInfo viewInfo) {
		//System.out.println("processAutoOpenMap " + id + " via " + Debug.getCompressedStackTrace());
		try {
			MdiEntry entry = getEntry(id);
			if (entry != null) {
				return true;
			}

			if (id.equals(SIDEBAR_SECTION_WELCOME)) {
				createWelcomeSection();
			}
			
			MdiEntryCreationListener mdiEntryCreationListener = mapIdToCreationListener.get(id);
			if (mdiEntryCreationListener != null) {
				try {
					mdiEntryCreationListener.createMDiEntry(id);
					return true;
				} catch (Exception e) {
					Debug.out(e);
				}
			}


			String title = MapUtils.getMapString(autoOpenInfo, "title", id);
			String parentID = MapUtils.getMapString(autoOpenInfo, "parentID", SIDEBAR_HEADER_PLUGINS);
			Object datasource = autoOpenInfo.get("datasource");

			if (viewInfo != null) {
				if (viewInfo.view != null) {
					entry = createEntryFromIView(parentID, viewInfo.view, id, datasource,
							true, false, true);
					return true;
				} else if (viewInfo.event_listener != null) {
					entry = createEntryFromEventListener(parentID,
							viewInfo.event_listener, id, true, datasource);
					return true;
				}
			}

			Class<?> cla = Class.forName(MapUtils.getMapString(autoOpenInfo,
					"iviewClass", ""));
			if (cla != null) {
				entry = createEntryFromIViewClass(parentID, id, title, cla, null, null,
						datasource, null, true);

				if (datasource == null) {
					final MdiEntry fEntry = entry;
					final String dmHash = MapUtils.getMapString(autoOpenInfo, "dm", null);
					if (dmHash != null) {
						AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
							public void azureusCoreRunning(AzureusCore core) {
								GlobalManager gm = core.getGlobalManager();
								HashWrapper hw = new HashWrapper(Base32.decode(dmHash));
								DownloadManager dm = gm.getDownloadManager(hw);
								if (dm != null) {
									fEntry.setDatasource(dm);
								}
							}
						});
					} else {
						final List listHashes = MapUtils.getMapList(autoOpenInfo, "dms",
								null);
						if (listHashes != null) {
							AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
								public void azureusCoreRunning(AzureusCore core) {
									List<DownloadManager> listDMS = new ArrayList<DownloadManager>(
											1);
									GlobalManager gm = core.getGlobalManager();
									for (Object oDM : listHashes) {
										if (oDM instanceof String) {
											String hash = (String) oDM;
											DownloadManager dm = gm.getDownloadManager(new HashWrapper(
													Base32.decode(hash)));
											if (dm != null) {
												listDMS.add(dm);
											}
										}
										fEntry.setDatasource(listDMS.toArray(new DownloadManager[0]));
									}
								}
							});
						}
					}
				}
				return true;
			}
		} catch (ClassNotFoundException ce) {
			// ignore
		} catch (Throwable e) {
			Debug.out(e);
		}
		return false;
	}

	public void removeItem(MdiEntry entry) {
		String id = entry.getId();
		synchronized (mapIdToEntry) {
			mapIdToEntry.remove(id);
			
			removeChildrenOf(id);
		}
	}

	private void removeChildrenOf(String id) {
		if (id == null) {
			return;
		}
		synchronized (mapIdToEntry) {
			MdiEntrySWT[] entriesSWT = getEntriesSWT();
			for (MdiEntrySWT entry : entriesSWT) {
				if (id.equals(entry.getParentID())) {
					mapIdToEntry.remove(entry);
					removeChildrenOf(entry.getId());
				}
			}
		}
	}
	
	public List<MdiEntry> getChildrenOf(String id) {
		if (id == null) {
			return Collections.emptyList();
		}
		List<MdiEntry> list = new ArrayList<MdiEntry>(1);
		synchronized (mapIdToEntry) {
			MdiEntrySWT[] entriesSWT = getEntriesSWT();
			for (MdiEntrySWT entry : entriesSWT) {
				if (id.equals(entry.getParentID())) {
					list.add(entry);
				}
			}
		}
		return list;
	}

	public Object updateLanguage(SWTSkinObject skinObject, Object params) {
  	MdiEntrySWT[] entries = getEntriesSWT();
  	for (MdiEntrySWT entry : entries) {
			if (entry == null) {
				continue;
			}
			IView view = entry.getIView();
			if (view != null) {
			  try {
          view.updateLanguage();
          view.refresh();
        }
        catch (Exception e) {
        	Debug.printStackTrace(e);
        }
			}
		}
    
		return null;
	}

	public void setPreferredOrder(String[] preferredOrder) {
		this.preferredOrder = preferredOrder;
	}

	public String[] getPreferredOrder() {
		return preferredOrder == null ? new String[0] : preferredOrder;
	}
}
