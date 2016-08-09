/*
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.aelitis.azureus.ui.swt.mdi;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.regex.Pattern;

import org.eclipse.swt.widgets.Menu;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.pluginsimpl.local.ui.config.ConfigSectionHolder;
import org.gudy.azureus2.pluginsimpl.local.ui.config.ConfigSectionRepository;
import org.gudy.azureus2.ui.common.util.MenuItemManager;
import org.gudy.azureus2.ui.swt.MenuBuildUtils;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.PluginsMenuHelper;
import org.gudy.azureus2.ui.swt.mainwindow.PluginsMenuHelper.IViewInfo;
import org.gudy.azureus2.ui.swt.mainwindow.PluginsMenuHelper.PluginAddedViewListener;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.mdi.*;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.views.skin.SkinView;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarEntrySWT;
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
	private Map<String, MdiEntry> mapIdToEntry = new LinkedHashMap<String, MdiEntry>(8);

	private List<MdiListener> listeners = new ArrayList<MdiListener>();

	private List<MdiEntryLoadedListener> listLoadListeners = new ArrayList<MdiEntryLoadedListener>();

	private List<MdiSWTMenuHackListener> listMenuHackListners;

	private LinkedHashMap<String, Object> mapAutoOpen = new LinkedHashMap<String, Object>();

	private String[] preferredOrder;

	private boolean mapAutoOpenLoaded = false;

	private String closeableConfigFile = "sidebarauto.config";

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
		MdiListener[] array;
		synchronized (listeners) {
			array = listeners.toArray(new MdiListener[0]);
		}
		for (MdiListener l : array) {
			try {
				l.mdiEntrySelected(newEntry, oldEntry);
			} catch (Exception e) {
				Debug.out(e);
			}
		}
		
		itemSelected( newEntry );
	}

	public void triggerEntryLoadedListeners(MdiEntry entry) {
		MdiEntryLoadedListener[] array;
		synchronized (listLoadListeners) {
			array = listLoadListeners.toArray(new MdiEntryLoadedListener[0]);
		}
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

	// @see com.aelitis.azureus.ui.swt.mdi.MultipleDocumentInterfaceSWT#createEntryFromEventListener(java.lang.String, org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener, java.lang.String, boolean, java.lang.Object, java.lang.String)
	public final MdiEntry createEntryFromEventListener(String parentID,
			UISWTViewEventListener l, String id, boolean closeable, Object datasource, String preferedAfterID) {
		return createEntryFromEventListener(parentID, null, l, id, closeable, datasource, preferedAfterID);
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.mdi.MultipleDocumentInterfaceSWT#createEntryFromEventListener(java.lang.String, java.lang.String, org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener, java.lang.String, boolean, java.lang.Object, java.lang.String)
	 */
	public abstract MdiEntry createEntryFromEventListener(String parentEntryID,
			String parentViewID, UISWTViewEventListener l, String id,
			boolean closeable, Object datasource, String preferredAfterID);
	
	// @see com.aelitis.azureus.ui.mdi.MultipleDocumentInterface#createEntryFromSkinRef(java.lang.String, java.lang.String, java.lang.String, java.lang.String, com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo, java.lang.Object, boolean, java.lang.String)
	public abstract MdiEntry createEntryFromSkinRef(String parentID, String id,
			String configID, String title, ViewTitleInfo titleInfo, Object params,
			boolean closeable, String preferedAfterID);

	// @see com.aelitis.azureus.ui.swt.mdi.MultipleDocumentInterfaceSWT#createEntryFromEventListener(java.lang.String, java.lang.Class, java.lang.String, boolean, java.lang.Object, java.lang.String)
	public MdiEntry createEntryFromEventListener(final String parentID,
			Class<? extends UISWTViewEventListener> cla, String id, boolean closeable,
			Object data, String preferedAfterID) {
		final MultipleDocumentInterfaceSWT mdi = UIFunctionsManagerSWT.getUIFunctionsSWT().getMDISWT();
		if (mdi == null) {
			return null;
		}

		if (id == null) {
			id = cla.getName();
			int i = id.lastIndexOf('.');
			if (i > 0) {
				id = id.substring(i + 1);
			}
		}

		MdiEntry entry = mdi.getEntry(id);
		if (entry != null) {
			if (data != null) {
				entry.setDatasource(data);
			}
			return entry;
		}
		UISWTViewEventListener l = null;
		if (data != null) {
			try {
				Constructor<?> constructor = cla.getConstructor(new Class[] {
					data.getClass()
				});
				l = (UISWTViewEventListener) constructor.newInstance(new Object[] {
					data
				});
			} catch (Exception e) {
			}
		}

		try {
			if (l == null) {
				l = cla.newInstance();
			}
			return mdi.createEntryFromEventListener(parentID, l, id, closeable, data,
					preferedAfterID);
		} catch (Exception e) {
			Debug.out(e);
		}

		return null;
	}
	
	public MdiEntry getCurrentEntry() {
		return currentEntry;
	}

	public MdiEntrySWT getCurrentEntrySWT() {
		return currentEntry;
	}

	public MdiEntry[] getEntries() {
		return getEntries( new MdiEntry[0]);
	}

	public MdiEntrySWT[] getEntriesSWT() {
		return getEntries( new MdiEntrySWT[0]);
	}

	public <T extends MdiEntry> T[] getEntries( T[] array ) {
		synchronized(mapIdToEntry){
			return mapIdToEntry.values().toArray( array );
		}
	}

	public MdiEntry getEntry(String id) {
		if (SkinConstants.VIEWID_BROWSER_BROWSE.equalsIgnoreCase(id)) {
			id = ContentNetworkUtils.getTarget(ConstantsVuze.getDefaultContentNetwork());
		}
		synchronized(mapIdToEntry){
			MdiEntry entry = mapIdToEntry.get(id);
			return entry;
		}
	}

	public MdiEntrySWT getEntrySWT(String id) {
		if (SkinConstants.VIEWID_BROWSER_BROWSE.equalsIgnoreCase(id)) {
			id = ContentNetworkUtils.getTarget(ConstantsVuze.getDefaultContentNetwork());
		}
		synchronized(mapIdToEntry){
			MdiEntrySWT entry = (MdiEntrySWT)mapIdToEntry.get(id);
			return entry;
		}
	}

	/**
	 * @param skinView
	 * @return 
	 *
	 * @since 3.1.1.1
	 */
	public MdiEntry getEntryBySkinView(Object skinView) {
		SWTSkinObject so = ((SkinView)skinView).getMainSkinObject();
		BaseMdiEntry[] sideBarEntries = getEntries( new BaseMdiEntry[0] );
		for (int i = 0; i < sideBarEntries.length; i++) {
			BaseMdiEntry entry = sideBarEntries[i];
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
		if (entry instanceof UISWTViewCore) {
			return (UISWTViewCore) entry;
		}
		return null;
	}

	public String getUpdateUIName() {
		if (currentEntry == null) {
			return "MDI";
		}
		return currentEntry.getId();
	}

	// @see com.aelitis.azureus.ui.mdi.MultipleDocumentInterface#registerEntry(java.lang.String, com.aelitis.azureus.ui.mdi.MdiEntryCreationListener2)
	public void registerEntry(String id, MdiEntryCreationListener2 l) {
		if (mapIdToCreationListener.containsKey(id)) {
			System.err.println("Warning: MDIEntry " + id
					+ " Creation Listener being registered twice. "
					+ Debug.getCompressedStackTrace());
		}
		mapIdToCreationListener2.put(id, l);
		
		createIfAutoOpen(id);
	}
	
	// @see com.aelitis.azureus.ui.mdi.MultipleDocumentInterface#deregisterEntry(java.lang.String, com.aelitis.azureus.ui.mdi.MdiEntryCreationListener2)
	public void deregisterEntry(String id, MdiEntryCreationListener2 l) {
		MdiEntryCreationListener2 l2 = mapIdToCreationListener2.get(id);
		if (l == l2) {
			mapIdToCreationListener2.remove(id);
		}
	}

	private boolean createIfAutoOpen(String id) {
		Object o = mapAutoOpen.get(id);
		if (o instanceof Map<?, ?>) {
			Map<?, ?> autoOpenMap = (Map<?, ?>) o;

			return createEntryByCreationListener(id, autoOpenMap.get("datasource"),
					autoOpenMap) != null;
		}

		boolean created = false;
		String[] autoOpenIDs = mapAutoOpen.keySet().toArray(new String[0]);
		for (String autoOpenID : autoOpenIDs) {
			if (Pattern.matches(id, autoOpenID)) {
				Map<?, ?> autoOpenMap = (Map<?, ?>) mapAutoOpen.get(autoOpenID);
				created |= createEntryByCreationListener(autoOpenID,
						autoOpenMap.get("datasource"), autoOpenMap) != null;
			}
		}
		return created;
	}

	protected MdiEntry 
	createEntryByCreationListener(String id, Object ds, Map<?, ?> autoOpenMap)
	{
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

	// @see com.aelitis.azureus.ui.mdi.MultipleDocumentInterface#registerEntry(java.lang.String, com.aelitis.azureus.ui.mdi.MdiEntryCreationListener)
	public void registerEntry(String id, MdiEntryCreationListener l) {
		if (mapIdToCreationListener.containsKey(id)
				|| mapIdToCreationListener2.containsKey(id)) {
			System.err.println("Warning: MDIEntry " + id
					+ " Creation Listener being registered twice. "
					+ Debug.getCompressedStackTrace());
		}
		mapIdToCreationListener.put(id, l);
		
		createIfAutoOpen(id);
	}

	// @see com.aelitis.azureus.ui.mdi.MultipleDocumentInterface#deregisterEntry(java.lang.String, com.aelitis.azureus.ui.mdi.MdiEntryCreationListener)
	public void deregisterEntry(String id, MdiEntryCreationListener l) {
		MdiEntryCreationListener l2 = mapIdToCreationListener.get(id);
		if (l == l2) {
			mapIdToCreationListener.remove(id);
		}
	}
	
	public boolean showEntryByID(String id) {
		return loadEntryByID(id, true);
	}

	public boolean showEntryByID(String id, Object datasource) {
		return loadEntryByID(id, true, false, datasource);
	}

	@Override
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		UIManager ui_manager = PluginInitializer.getDefaultInterface().getUIManager();
		ui_manager.addUIListener(new UIManagerListener() {
			public void UIDetached(UIInstance instance) {
			}
			
			public void UIAttached(UIInstance instance) {
				if (instance instanceof UISWTInstance) {
					final AESemaphore wait_sem = new AESemaphore( "SideBar:wait" );
					
					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {
							try{
								try {
									loadCloseables();
								} catch (Throwable t) {
									Debug.out(t);
								}
	
								setupPluginViews();
								
							}finally{
								
								wait_sem.release();
							}
						}
					});
					
						// we need to wait for the loadCloseables to complete as there is code in MainMDISetup that runs on the 'UIAttachedComplete'
						// callback that needs the closables to be loaded (when setting 'start tab') otherwise the order gets broken
					
					if ( !wait_sem.reserve(10*1000)){
						
						Debug.out( "eh?");
					}
				}
			}
		});

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

		@SuppressWarnings("deprecation")
		boolean loadedOnce = wasEntryLoadedOnce(id);
		if (loadedOnce && onlyLoadOnce) {
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
		synchronized(mapIdToEntry){
			MdiEntry entry = mapIdToEntry.get(id);
			if (entry == null) {
				return false;
			}
			return entry.isAdded();
		}
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
		if (closeableConfigFile == null) {
			return;
		}
		try{
			Map<?,?> loadedMap = FileUtil.readResilientConfigFile(closeableConfigFile , true);
			if (loadedMap.isEmpty()) {
				return;
			}
			BDecoder.decodeStrings(loadedMap);
	
			List<Map> orderedEntries = (List<Map>)loadedMap.get( "_entries_" );
			
			if ( orderedEntries == null ){
					// migrate old format
				for (Iterator<?> iter = loadedMap.keySet().iterator(); iter.hasNext();) {
					String id = (String) iter.next();
					Object o = loadedMap.get(id);
		
					if (o instanceof Map<?, ?>) {
						if (!processAutoOpenMap(id, (Map<?, ?>) o, null)) {
							mapAutoOpen.put(id, o);
						}
					}
				}
			}else{
				for (Map map: orderedEntries){
					String id = (String)map.get( "id" );
					
					//System.out.println( "loaded " + id );
					Object o = map.get( "value" );
					if (o instanceof Map<?, ?>) {
						if (!processAutoOpenMap(id, (Map<?, ?>) o, null)) {
							mapAutoOpen.put(id, o);
						}
					}
				}
			}
		}catch( Throwable e ){
			
			Debug.out( e );
			
		}finally{
		
			mapAutoOpenLoaded  = true;
		}
	}

	@SuppressWarnings({
		"unchecked",
		"rawtypes"
	})
	public void saveCloseables() {
		if (!mapAutoOpenLoaded) {
			return;
		}
		if (closeableConfigFile == null) {
			return;
		}

		try{
			// update auto open info
			for (Iterator<String> iter = new ArrayList<String>(mapAutoOpen.keySet()).iterator(); iter.hasNext();) {
				String id = (String) iter.next();
	
				MdiEntry entry = getEntry(id);
				
				if ( entry != null && entry.isAdded()){
					
					mapAutoOpen.put(id, entry.getAutoOpenInfo());
					
				}else{
					
					mapAutoOpen.remove(id);
				}
			}
	
			Map map = new HashMap();
			
			List<Map> list = new ArrayList<Map>( mapAutoOpen.size());
			
			map.put( "_entries_", list );
			
			for ( Map.Entry<String,Object> entry: mapAutoOpen.entrySet()){
				
				Map m = new HashMap();
				
				list.add( m );
				
				String id = entry.getKey();
				
				m.put( "id", id );
				m.put( "value", entry.getValue());
				
				//System.out.println( "saved " + id );
			}
			
			FileUtil.writeResilientConfigFile(closeableConfigFile, map );
			
		}catch( Throwable e ){
			
			Debug.out( e );
		}	
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
				if (viewInfo.event_listener != null) {
					entry = createEntryFromEventListener(parentID,
							viewInfo.event_listener, id, true, datasource,null);
					if (entry != null) {
						entry.setTitle(title);
					}
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

	public void addItem(MdiEntry entry) {
		String id = entry.getId();
		synchronized (mapIdToEntry) {
			mapIdToEntry.put(id,entry);
		}
	}
	
	protected void
	itemSelected(MdiEntry entry ){
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
					String kid_id = entry.getId();
					mapIdToEntry.remove(kid_id);
					removeChildrenOf(kid_id);
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
	
	public int getEntriesCount() {
		synchronized( mapIdToEntry ){
			return mapIdToEntry.size();
		}
	}

	public void setCloseableConfigFile(String closeableConfigFile) {
		this.closeableConfigFile = closeableConfigFile;
	}
	
	public void addListener(MdiSWTMenuHackListener l) {
		synchronized (this) {
			if (listMenuHackListners == null) {
				listMenuHackListners = new ArrayList<MdiSWTMenuHackListener>(1);
			}
			if (!listMenuHackListners.contains(l)) {
				listMenuHackListners.add(l);
			}
		}
	}

	public void removeListener(MdiSWTMenuHackListener l) {
		synchronized (this) {
			if (listMenuHackListners == null) {
				listMenuHackListners = new ArrayList<MdiSWTMenuHackListener>(1);
			}
			listMenuHackListners.remove(l);
		}
	}

	public MdiSWTMenuHackListener[] getMenuHackListeners() {
		synchronized (this) {
			if (listMenuHackListners == null) {
				return new MdiSWTMenuHackListener[0];
			}
			return listMenuHackListners.toArray(new MdiSWTMenuHackListener[0]);
		}
	}


	public void fillMenu(Menu menu, final MdiEntry entry, String menuID) {
		org.gudy.azureus2.plugins.ui.menus.MenuItem[] menu_items;
		
		menu_items = MenuItemManager.getInstance().getAllAsArray(menuID);

		MenuBuildUtils.addPluginMenuItems(menu_items, menu, false, true,
				new MenuBuildUtils.MenuItemPluginMenuControllerImpl(new Object[] {
					entry
				}));

		if (entry != null) {

			menu_items = MenuItemManager.getInstance().getAllAsArray(
					"sidebar." + entry.getId());

			if (menu_items.length == 0) {

				if (entry instanceof UISWTView) {

					PluginInterface pi = ((UISWTView) entry).getPluginInterface();

					if (pi != null) {

						final List<String> relevant_sections = new ArrayList<String>();

						List<ConfigSectionHolder> sections = ConfigSectionRepository.getInstance().getHolderList();

						for (ConfigSectionHolder cs : sections) {

							if (pi == cs.getPluginInterface()) {

								relevant_sections.add(cs.configSectionGetName());
							}
						}

						if (relevant_sections.size() > 0) {

							MenuItem mi = pi.getUIManager().getMenuManager().addMenuItem(
									"sidebar." + entry.getId(),
									"MainWindow.menu.view.configuration");

							mi.addListener(new MenuItemListener() {
								public void selected(MenuItem menu, Object target) {
									UIFunctions uif = UIFunctionsManager.getUIFunctions();

									if (uif != null) {

										for (String s : relevant_sections) {

											uif.getMDI().showEntryByID(
													MultipleDocumentInterface.SIDEBAR_SECTION_CONFIG, s);
										}
									}
								}
							});

							menu_items = MenuItemManager.getInstance().getAllAsArray(
									"sidebar." + entry.getId());
						}
					}
				}
			}

			MenuBuildUtils.addPluginMenuItems(menu_items, menu, false, true,
					new MenuBuildUtils.MenuItemPluginMenuControllerImpl(new Object[] {
						entry
					}));

			MdiSWTMenuHackListener[] menuHackListeners = getMenuHackListeners();
			for (MdiSWTMenuHackListener l : menuHackListeners) {
				try {
					l.menuWillBeShown(entry, menu);
				} catch (Exception e) {
					Debug.out(e);
				}
			}
			if (currentEntry instanceof SideBarEntrySWT) {
				menuHackListeners = ((SideBarEntrySWT) entry).getMenuHackListeners();
				for (MdiSWTMenuHackListener l : menuHackListeners) {
					try {
						l.menuWillBeShown(entry, menu);
					} catch (Exception e) {
						Debug.out(e);
					}
				}
			}
		}
			
		menu_items = MenuItemManager.getInstance().getAllAsArray(menuID + "._end_");

		if ( menu_items.length > 0 ){
			
			MenuBuildUtils.addPluginMenuItems(menu_items, menu, false, true,
					new MenuBuildUtils.MenuItemPluginMenuControllerImpl(new Object[] {
						entry
					}));
		}
	}


}
