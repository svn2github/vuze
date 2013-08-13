package com.aelitis.azureus.ui.swt.mdi;

import java.util.*;
import java.util.regex.Pattern;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.mainwindow.PluginsMenuHelper;
import org.gudy.azureus2.ui.swt.mainwindow.PluginsMenuHelper.IViewInfo;
import org.gudy.azureus2.ui.swt.mainwindow.PluginsMenuHelper.PluginAddedViewListener;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.mdi.*;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.views.skin.SkinView;
import com.aelitis.azureus.util.ConstantsVuze;
import com.aelitis.azureus.util.ContentNetworkUtils;
import com.aelitis.azureus.util.MapUtils;

public abstract class BaseMDI
	extends SkinView
	implements MultipleDocumentInterfaceSWT, UIUpdatable
{
	protected MdiEntrySWT currentEntry;

	private Map<String, MdiEntryCreationListener> mapIdToCreationListener = new LightHashMap<String, MdiEntryCreationListener>();
	private Map<String, MdiEntryCreationListener2> mapIdToCreationListener2 = new LightHashMap<String, MdiEntryCreationListener2>();

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
		} else {
			removeEntryAutoOpen(id);
		}
	}

	public abstract MdiEntry createEntryFromEventListener(String parentID,
			UISWTViewEventListener l, String id, boolean closeable, Object datasource, String preferedAfterID);

	public abstract MdiEntry createEntryFromView(String parentID, UISWTViewCore iview,
			String id, Object datasource, boolean closeable, boolean show,
			boolean expand);

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

	public UISWTViewCore getCoreViewFromID(String id) {
		if (id == null) {
			return null;
		}
		MdiEntrySWT entry = getEntrySWT(id);
		if (entry == null) {
			return null;
		}
		return entry.getCoreView();
	}

	public String getUpdateUIName() {
		if (currentEntry == null || currentEntry.getView() == null) {
			return "MDI";
		}
		return currentEntry.getView().getViewID();
	}

	public void registerEntry(String id, MdiEntryCreationListener2 l) {
		mapIdToCreationListener2.put(id, l);
		
		createIfAutoOpen(id);
	}

	private boolean createIfAutoOpen(String id) {
		Object o = mapAutoOpen.get(id);
		if (o instanceof Map<?, ?>) {
			Map<?, ?> autoOpenMap = (Map<?, ?>) o;

			return createEntryByCreationListener(id, autoOpenMap.get("datasource"), autoOpenMap) != null;
		}
		return false;
	}

	private MdiEntry createEntryByCreationListener(String id, Object ds, Map<?, ?> autoOpenMap) {
		MdiEntryCreationListener mdiEntryCreationListener = null;
		for (String key : mapIdToCreationListener.keySet()) {
			if (Pattern.matches(key, id)) {
				mdiEntryCreationListener = mapIdToCreationListener.get(key);
				break;
			}
		}
		if (mdiEntryCreationListener != null) {
			try {
				MdiEntry mdiEntry = mdiEntryCreationListener.createMDiEntry(id);
				
				if (mdiEntry != null && ds != null) {
					mdiEntry.setDatasource(ds);
				}
				return mdiEntry;
			} catch (Exception e) {
				Debug.out(e);
			}
		}

		MdiEntryCreationListener2 mdiEntryCreationListener2 = null;
		for (String key : mapIdToCreationListener2.keySet()) {
			if (Pattern.matches(key, id)) {
				mdiEntryCreationListener2 = mapIdToCreationListener2.get(key);
				break;
			}
		}
		if (mdiEntryCreationListener2 != null) {
			try {
				MdiEntry mdiEntry = mdiEntryCreationListener2.createMDiEntry(this, id, ds, autoOpenMap);
				if (mdiEntry == null) {
					removeEntryAutoOpen(id);
				}
				return mdiEntry;
			} catch (Exception e) {
				Debug.out(e);
			}
		}
		
		setEntryAutoOpen(id, ds);

		return null;
	}

	public void registerEntry(String id, MdiEntryCreationListener l) {
		mapIdToCreationListener.put(id, l);
		
		createIfAutoOpen(id);
	}

	public boolean showEntryByID(String id) {
		return loadEntryByID(id, true);
	}

	@Override
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		return null;
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectDestroyed(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectDestroyed(SWTSkinObject skinObject, Object params) {
		MdiEntry entry = getCurrentEntry();
		if (entry != null) {
  		COConfigurationManager.setParameter("v3.StartTab",
  				entry.getId());
  		String ds = entry.getExportableDatasource();
  		COConfigurationManager.setParameter("v3.StartTab.ds", ds == null ? null : ds.toString());
		}

		return super.skinObjectDestroyed(skinObject, params);
	}
	
	public void updateUI() {
		MdiEntry currentEntry = getCurrentEntry();
		if (currentEntry != null) {
			currentEntry.updateUI();
		}
	}

	// @see com.aelitis.azureus.ui.mdi.MultipleDocumentInterface#loadEntryByID(java.lang.String, boolean)
	public boolean loadEntryByID(String id, boolean activate) {
		return loadEntryByID(id, activate, false, null);
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.mdi.MultipleDocumentInterface#loadEntryByID(java.lang.String, boolean, boolean)
	 */
	public boolean loadEntryByID(String id, boolean activate,
			boolean onlyLoadOnce, Object datasource) {
		if (id == null) {
			return false;
		}
		MdiEntry entry = getEntry(id);
		if (entry != null) {
			if (datasource != null) {
				entry.setDatasource(datasource);
			}
			if (activate) {
				showEntry(entry);
			}
			return true;
		}

		@SuppressWarnings("deprecation")
		boolean loadedOnce = wasEntryLoadedOnce(id);
		if (loadedOnce && onlyLoadOnce) {
			return false;
		}

		MdiEntry mdiEntry = createEntryByCreationListener(id, datasource, null);
		if (mdiEntry != null) {
			if (onlyLoadOnce) {
				setEntryLoadedOnce(id);
			}
			if (activate) {
				showEntry(mdiEntry);
			}
			return true;
		}

		return false;
	}


	protected abstract void setEntryLoadedOnce(String id);

	protected abstract boolean wasEntryLoadedOnce(String id);

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

	// @see com.aelitis.azureus.ui.mdi.MultipleDocumentInterface#setEntryAutoOpen(java.lang.String, java.lang.Object)
	public void setEntryAutoOpen(String id, Object datasource) {
		Map<String, Object> map = (Map<String, Object>) mapAutoOpen.get(id);
		if (map == null) {
			map = new LightHashMap<String, Object>(1);
		}
		map.put("datasource", datasource);
		mapAutoOpen.put(id, map);
	}
	
	// @see com.aelitis.azureus.ui.mdi.MultipleDocumentInterface#removeEntryAutoOpen(java.lang.String)
	public void removeEntryAutoOpen(String id) {
		mapAutoOpen.remove(id);
	}

	protected void setupPluginViews() {

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

	@SuppressWarnings({
		"unchecked",
		"rawtypes"
	})
	public void saveCloseables() {
		// update auto open info
		for (Iterator<?> iter = mapAutoOpen.keySet().iterator(); iter.hasNext();) {
			String id = (String) iter.next();

			MdiEntry entry = getEntry(id);
			
			if (entry != null && entry.isAdded()) {
				mapAutoOpen.put(id, entry.getAutoOpenInfo());
			} else {
				mapAutoOpen.remove(id);
			}
		}

		FileUtil.writeResilientConfigFile("sidebarauto.config", mapAutoOpen);
	}

	private boolean processAutoOpenMap(String id, Map<?, ?> autoOpenInfo,
			IViewInfo viewInfo) {
		try {
			MdiEntry entry = getEntry(id);
			if (entry != null) {
				return true;
			}

			Object datasource = autoOpenInfo.get("datasource");
			String title = MapUtils.getMapString(autoOpenInfo, "title", id);

			MdiEntry mdiEntry = createEntryByCreationListener(id, datasource, autoOpenInfo);
			if (mdiEntry != null) {
				if (mdiEntry.getTitle().equals("")) {
					mdiEntry.setTitle(title);
				}
				return true;
			}

			String parentID = MapUtils.getMapString(autoOpenInfo, "parentID", SIDEBAR_HEADER_PLUGINS);

			if (viewInfo != null) {
				if (viewInfo.view != null) {
					entry = createEntryFromView(parentID, viewInfo.view, id, datasource,
							true, false, true);
				} else if (viewInfo.event_listener != null) {
					entry = createEntryFromEventListener(parentID,
							viewInfo.event_listener, id, true, datasource,null);
  				entry.setTitle(title);
				}
			}

			if (entry != null && datasource == null) {
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
					final List<?> listHashes = MapUtils.getMapList(autoOpenInfo, "dms",
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

			return entry != null;
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
		MdiEntry[] entries = getEntries();
		
		for (MdiEntry entry : entries) {
			if (entry instanceof BaseMdiEntry) {
				BaseMdiEntry baseEntry = (BaseMdiEntry) entry;
				baseEntry.updateLanguage();
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
