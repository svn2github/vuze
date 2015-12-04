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

import java.util.*;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.ConfigurationDefaults;
import org.gudy.azureus2.core3.config.impl.ConfigurationParameterNotFoundException;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarEnablerBase;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.debug.ObfusticateImage;
import org.gudy.azureus2.ui.swt.plugins.PluginUISWTSkinObject;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.*;

import com.aelitis.azureus.ui.common.viewtitleinfo.*;
import com.aelitis.azureus.ui.mdi.*;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectContainer;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectListener;

public abstract class BaseMdiEntry
	extends UISWTViewImpl
	implements MdiEntrySWT, ViewTitleInfoListener, AEDiagnosticsEvidenceGenerator,
	ObfusticateImage
{
	protected final MultipleDocumentInterface mdi;

	protected String logID;

	private String skinRef;

	private List<MdiCloseListener> listCloseListeners = null;

	private List<MdiChildCloseListener> listChildCloseListeners = null;

	private List<MdiEntryLogIdListener> listLogIDListeners = null;

	private List<MdiEntryOpenListener> listOpenListeners = null;

	private List<MdiEntryDropListener> listDropListeners = null;

	private List<MdiEntryDatasourceListener> listDatasourceListeners = null;

	private List<MdiSWTMenuHackListener> listMenuHackListners;
	
	protected ViewTitleInfo viewTitleInfo;

	/** Parent MDIEntry.  Doesn't mean that this view is embedded inside the parentID */
	private String parentEntryID;

	private boolean closeable;

	private Boolean isExpanded = null;

	private boolean disposed = false;

	private boolean added = false;

	private String imageLeftID;

	private Image imageLeft;

	private boolean collapseDisabled = false;

	private SWTSkinObject soMaster;

	private String preferredAfterID;

	private boolean hasBeenOpened;

	private BaseMdiEntry() {
		super(null, null, false);
		mdi = null;
		setDefaultExpanded(false);
		AEDiagnostics.addEvidenceGenerator(this);
	}

	public BaseMdiEntry(MultipleDocumentInterface mdi, String id, String parentViewID) {
		super(id, parentViewID, true);
		this.mdi = mdi;
		AEDiagnostics.addEvidenceGenerator(this);

		if (id == null) {
			logID = "null";
		} else {
			int i = id.indexOf('_');
			if (i > 0) {
				logID = id.substring(0, i);
			} else {
				logID = id;
			}
		}
		setDefaultExpanded(false);
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.mdi.MdiEntry#getId()
	 */
	public String getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.mdi.MdiEntry#addVitalityImage(java.lang.String)
	 */
	public MdiEntryVitalityImage addVitalityImage(String imageID) {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.mdi.MdiEntry#close()
	 */
	public boolean close(boolean forceClose) {
		if (!forceClose) {
			if (!requestClose()) {
				return false;
			}
		}

		setCloseable(closeable);
		disposed = true;
		ViewTitleInfoManager.removeListener(this); 

		return true;
	}

	public Object getDatasourceCore() {
		return datasource;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.mdi.MdiEntry#getExportableDatasource()
	 */
	public String getExportableDatasource() {
		if (viewTitleInfo != null) {
			Object ds = viewTitleInfo.getTitleInfoProperty(ViewTitleInfo2.TITLE_EXPORTABLE_DATASOURCE);
			if (ds != null) {
				return ds.toString();
			}
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.mdi.MdiEntry#getDatasource()
	 */
	public Object getDatasource() {
		return PluginCoreUtils.convert(datasource, false);
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.mdi.MdiEntry#getLogID()
	 */
	public String getLogID() {
		return logID;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.mdi.MdiEntry#getMDI()
	 */
	public MultipleDocumentInterface getMDI() {
		return mdi;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.mdi.MdiEntry#getParentID()
	 */
	public String getParentID() {
		return parentEntryID;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.mdi.MdiEntry#setParentID(java.lang.String)
	 */
	public void setParentID(String id) {
		if (id == null || "Tools".equals(id)) {
			if (getId().equals(MultipleDocumentInterface.SIDEBAR_HEADER_DVD)
					&& id == null) {
				id = "";
			} else {
				id = MultipleDocumentInterface.SIDEBAR_HEADER_PLUGINS;
			}
		}
		if (id.equals(getId())) {
			Debug.out("Setting Parent to same ID as child! " + id);
			return;
		}
		parentEntryID = id;
		// ensure parent gets created if it isn't there already
		if (mdi != null) {
			mdi.loadEntryByID(parentEntryID, false);
		}
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.mdi.MdiEntry#getVitalityImages()
	 */
	public MdiEntryVitalityImage[] getVitalityImages() {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.mdi.MdiEntry#isCloseable()
	 */
	public boolean isCloseable() {
		return closeable;
	}

	public boolean isCollapseDisabled() {
		return collapseDisabled;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.mdi.MdiEntry#setCollapseDisabled(boolean)
	 */
	public void setCollapseDisabled(boolean collapseDisabled) {
		this.collapseDisabled = collapseDisabled;
		setExpanded(true);
	}
	
	// @see com.aelitis.azureus.ui.mdi.MdiEntry#addListeners(java.lang.Object)
	public void addListeners(Object objectWithListeners) {
		if (objectWithListeners instanceof MdiChildCloseListener) {
			addListener((MdiChildCloseListener) objectWithListeners);
		}
		if (objectWithListeners instanceof MdiCloseListener) {
			addListener((MdiCloseListener) objectWithListeners);
		}
		if (objectWithListeners instanceof MdiEntryDatasourceListener) {
			addListener((MdiEntryDatasourceListener) objectWithListeners);
		}
		if (objectWithListeners instanceof MdiEntryDropListener) {
			addListener((MdiEntryDropListener) objectWithListeners);
		}
		if (objectWithListeners instanceof MdiEntryLogIdListener) {
			addListener((MdiEntryLogIdListener) objectWithListeners);
		}
		if (objectWithListeners instanceof MdiEntryOpenListener) {
			addListener((MdiEntryOpenListener) objectWithListeners);
		}

		if (objectWithListeners instanceof MdiSWTMenuHackListener) {
			addListener((MdiSWTMenuHackListener) objectWithListeners);
		}
	}

	public void addListener(MdiCloseListener l) {
		synchronized (this) {
			if (listCloseListeners == null) {
				listCloseListeners = new ArrayList<MdiCloseListener>(1);
			}
			listCloseListeners.add(l);
		}
	}

	public void removeListener(MdiCloseListener l) {
		synchronized (this) {
			if (listCloseListeners != null) {
				listCloseListeners.remove(l);
			}
		}
	}

	public void triggerCloseListeners(boolean user) {
		Object[] list = {};
		synchronized (this) {
			if (listCloseListeners != null) {
				list = listCloseListeners.toArray();
			}
		}
		for (int i = 0; i < list.length; i++) {
			MdiCloseListener l = (MdiCloseListener) list[i];
			try {
				l.mdiEntryClosed(this, user);
			} catch (Exception e) {
				Debug.out(e);
			}
		}

		if (parentEntryID != null && mdi != null) {
  		MdiEntry parentEntry = mdi.getEntry(parentEntryID);
  		if (parentEntry instanceof BaseMdiEntry) {
  			((BaseMdiEntry) parentEntry).triggerChildCloseListeners(this, user);
  		}
		}

		triggerEvent(UISWTViewEvent.TYPE_DESTROY, null);
	}

	public void addListener(MdiChildCloseListener l) {
		synchronized (this) {
			if (listChildCloseListeners == null) {
				listChildCloseListeners = new ArrayList<MdiChildCloseListener>(1);
			}
			listChildCloseListeners.add(l);
		}
	}

	public void removeListener(MdiChildCloseListener l) {
		synchronized (this) {
			if (listChildCloseListeners != null) {
				listChildCloseListeners.remove(l);
			}
		}
	}

	public void triggerChildCloseListeners(MdiEntry child, boolean user) {
		Object[] list;
		synchronized (this) {
			if (listChildCloseListeners == null) {
				return;
			}
			list = listChildCloseListeners.toArray();
		}
		for (int i = 0; i < list.length; i++) {
			MdiChildCloseListener l = (MdiChildCloseListener) list[i];
			try {
				l.mdiChildEntryClosed(this, child, user);
			} catch (Exception e) {
				Debug.out(e);
			}
		}
	}

	public void addListener(MdiEntryLogIdListener l) {
		synchronized (this) {
			if (listLogIDListeners == null) {
				listLogIDListeners = new ArrayList<MdiEntryLogIdListener>(1);
			}
			listLogIDListeners.add(l);
		}
	}

	public void removeListener(MdiEntryLogIdListener sideBarLogIdListener) {
		synchronized (this) {
			if (listLogIDListeners != null) {
				listLogIDListeners.remove(sideBarLogIdListener);
			}
		}
	}

	protected void triggerLogIDListeners(String oldID) {
		Object[] list;
		synchronized (this) {
			if (listLogIDListeners == null) {
				return;
			}

			list = listLogIDListeners.toArray();
		}

		for (int i = 0; i < list.length; i++) {
			MdiEntryLogIdListener l = (MdiEntryLogIdListener) list[i];
			l.mdiEntryLogIdChanged(this, oldID, logID);
		}
	}

	public void addListener(MdiEntryOpenListener l) {
		synchronized (this) {
			if (listOpenListeners == null) {
				listOpenListeners = new ArrayList<MdiEntryOpenListener>(1);
			}
			listOpenListeners.add(l);
		}

		if (hasBeenOpened) {
			l.mdiEntryOpen(this);
		}
	}

	public void removeListener(MdiEntryOpenListener l) {
		synchronized (this) {
			if (listOpenListeners != null) {
				listOpenListeners.remove(l);
			}
		}
	}

	public void triggerOpenListeners() {
		Object[] list;
		hasBeenOpened = true;
		synchronized (this) {
			if (listOpenListeners == null) {
				return;
			}

			list = listOpenListeners.toArray();
		}
		for (int i = 0; i < list.length; i++) {
			MdiEntryOpenListener l = (MdiEntryOpenListener) list[i];
			try {
				l.mdiEntryOpen(this);
			} catch (Exception e) {
				Debug.out(e);
			}
		}
	}


	public void addListener(MdiEntryDatasourceListener l) {
		synchronized (this) {
			if (listDatasourceListeners == null) {
				listDatasourceListeners = new ArrayList<MdiEntryDatasourceListener>(1);
			}
			listDatasourceListeners.add(l);
		}

		l.mdiEntryDatasourceChanged(this);
	}

	public void removeListener(MdiEntryDatasourceListener l) {
		synchronized (this) {
			if (listDatasourceListeners != null) {
				listDatasourceListeners.remove(l);
			}
		}
	}

	public void triggerDatasourceListeners() {
		Object[] list;
		synchronized (this) {
			if (listDatasourceListeners == null) {
				return;
			}

			list = listDatasourceListeners.toArray();
		}
		for (int i = 0; i < list.length; i++) {
			MdiEntryDatasourceListener l = (MdiEntryDatasourceListener) list[i];
			try {
				l.mdiEntryDatasourceChanged(this);
			} catch (Exception e) {
				Debug.out(e);
			}
		}
	}

	public void addListener(MdiEntryDropListener l) {
		synchronized (this) {
			if (listDropListeners == null) {
				listDropListeners = new ArrayList<MdiEntryDropListener>(1);
			}
			listDropListeners.add(l);
		}
	}

	public void removeListener(MdiEntryDropListener l) {
		synchronized (this) {
			if (listDropListeners != null) {
				listDropListeners.remove(l);
			}
		}
	}

	public boolean hasDropListeners() {
		synchronized (this) {
			return listDropListeners != null && listDropListeners.size() > 0;
		}
	}

	/**
	 * 
	 * @param o
	 * @return true: handled; false: not handled
	 */
	public boolean triggerDropListeners(Object o) {
		boolean handled = false;
		Object[] list;
		synchronized (this) {
			if (listDropListeners == null) {
				return handled;
			}

			list = listDropListeners.toArray();
		}
		for (int i = 0; i < list.length; i++) {
			MdiEntryDropListener l = (MdiEntryDropListener) list[i];
			handled = l.mdiEntryDrop(this, o);
			if (handled) {
				break;
			}
		}
		return handled;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.mdi.MdiEntry#setLogID(java.lang.String)
	 */
	public void setLogID(String logID) {
		if (logID == null || logID.equals("" + this.logID)) {
			return;
		}
		String oldID = this.logID;
		this.logID = logID;
		triggerLogIDListeners(oldID);
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.mdi.MdiEntry#getViewTitleInfo()
	 */
	public ViewTitleInfo getViewTitleInfo() {
		return viewTitleInfo;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.mdi.MdiEntry#setViewTitleInfo(com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo)
	 */
	public void setViewTitleInfo(ViewTitleInfo viewTitleInfo) {
		if (this.viewTitleInfo == viewTitleInfo) {
			return;
		}
		this.viewTitleInfo = viewTitleInfo;
		// TODO: Need to listen for viewTitleInfo triggers so we can refresh items below
		if (viewTitleInfo != null) {
			if (viewTitleInfo instanceof ViewTitleInfo2) {
				ViewTitleInfo2 vti2 = (ViewTitleInfo2) viewTitleInfo;
				try {
					vti2.titleInfoLinked(mdi, this);
				} catch (Exception e) {
					Debug.out(e);
				}
			}

			String imageID = (String) viewTitleInfo.getTitleInfoProperty(ViewTitleInfo.TITLE_IMAGEID);
			if (imageID != null) {
				setImageLeftID(imageID.length() == 0 ? null : imageID);
			}

			ViewTitleInfoManager.addListener(this);
			
			if (getEventListener() == null && (viewTitleInfo instanceof UISWTViewEventListener)) {
				try {
					setEventListener((UISWTViewEventListener) viewTitleInfo, true);
				} catch (UISWTViewEventCancelledException e) {
				}
			}
		}
		redraw();
	}


	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewImpl2#setPluginSkinObject(org.gudy.azureus2.ui.swt.plugins.PluginUISWTSkinObject, org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void setPluginSkinObject(PluginUISWTSkinObject skinObject) {
		super.setPluginSkinObject(skinObject);
		Object initialDataSource = (datasource == null
				|| ((datasource instanceof Object[])
						&& ((Object[]) datasource).length == 0)) ? getInitialDataSource()
								: datasource;
		if (initialDataSource != null) {
			if (skinObject instanceof SWTSkinObject) {
				((SWTSkinObject) skinObject).triggerListeners(
						SWTSkinObjectListener.EVENT_DATASOURCE_CHANGED, initialDataSource);
			}
			triggerEvent(UISWTViewEvent.TYPE_DATASOURCE_CHANGED, initialDataSource);
		}
	}

	public void setSkinObjectMaster(SWTSkinObject soMaster) {
		this.soMaster = soMaster;
	}

	
	public SWTSkinObject getSkinObject() {
		return (SWTSkinObject) getPluginSkinObject();
	}
	
	public SWTSkinObject getSkinObjectMaster() {
		if (soMaster == null) {
			return getSkinObject();
		}
		return soMaster;
	}

	public void setSkinRef(String configID, Object params) {
		skinRef = configID;
		if (params != null) {
			setDatasource(params);
		}
	}

	public String getSkinRef() {
		return skinRef;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.mdi.MdiEntry#getTitle()
	 */
	public String getTitle() {
		if (viewTitleInfo != null) {
			String viewTitle = (String) viewTitleInfo.getTitleInfoProperty(ViewTitleInfo.TITLE_TEXT);
			if (viewTitle != null && viewTitle.length() > 0) {
				return viewTitle;
			}
		}
		return super.getFullTitle();
	}
	
	public void updateLanguage() {
		triggerEvent(UISWTViewEvent.TYPE_LANGUAGEUPDATE, null);
	}
	
	
	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewImpl2#triggerEvent(int, java.lang.Object)
	 */
	@Override
	public void triggerEvent(int eventType, Object data) {
		super.triggerEvent(eventType, data);
		
		if (eventType == UISWTViewEvent.TYPE_LANGUAGEUPDATE) {
			//if (getTitleID() != null) {
			//	setTitleID(getTitleID());
			//} else {
				if (viewTitleInfo != null) {
					viewTitleInfoRefresh(viewTitleInfo);
				}
				updateUI();
			//}

			SWTSkinObject skinObjectMaster = getSkinObjectMaster();
			if (skinObjectMaster != null) {
				skinObjectMaster.triggerListeners(SWTSkinObjectListener.EVENT_LANGUAGE_CHANGE);
			}
		}

	}


	public void show() {
		if (skinObject == null) {
			return;
		}

		SelectedContentManager.clearCurrentlySelectedContent();

		UIFunctionsSWT uif = UIFunctionsManagerSWT.getUIFunctionsSWT();
		if (uif != null) {
			//uif.refreshIconBar(); // needed?
			uif.refreshTorrentMenu();
		}
		
		

		SWTSkinObject skinObject = getSkinObjectMaster();
		skinObject.setVisible(true);
		if (skinObject instanceof SWTSkinObjectContainer) {
			SWTSkinObjectContainer container = (SWTSkinObjectContainer) skinObject;
			Composite composite = container.getComposite();
			if (composite != null && !composite.isDisposed()) {
				composite.setVisible(true);
				composite.moveAbove(null);
				//composite.setFocus();
				//container.getParent().relayout();
				composite.getParent().layout();
			}
			// This causes double show because createSkinObject already calls show
			//container.triggerListeners(SWTSkinObjectListener.EVENT_SHOW);
		}

		Composite c = getComposite();
		if (c != null && !c.isDisposed()) {
			c.setData("BaseMDIEntry", this);
			c.setVisible(true);
			c.getParent().layout();
		}

		try {
			// In theory, c.setVisible() will trigger TYPE_FOCUSGAINED, but let's
			// call it anyway (it will be ignored if focus is already gained)
			triggerEvent(UISWTViewEvent.TYPE_FOCUSGAINED, null);
		} catch (Exception e) {
			Debug.out(e);
		}
		setToolbarVisibility(hasToolbarEnableers());
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.mdi.MdiEntry#hide()
	 */
	public void hide() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				swt_hide();
			}
		});
		setToolbarVisibility(false);
	}
	
	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.mdi.MdiEntry#requestAttention()
	 */
	public void
	requestAttention()
	{
	}

	protected void swt_hide() {
		SWTSkinObject skinObjectMaster = getSkinObjectMaster();
		if (skinObjectMaster instanceof SWTSkinObjectContainer) {
			SWTSkinObjectContainer container = (SWTSkinObjectContainer) skinObjectMaster;
			Control oldComposite = container.getControl();

			container.setVisible(false);
			if (oldComposite != null && !oldComposite.isDisposed()) {
				oldComposite.getShell().update();
			}
		}

		Composite oldComposite = getComposite();
		if (oldComposite != null && !oldComposite.isDisposed()) {

			oldComposite.setVisible(false);
			oldComposite.getShell().update();
		}

		try {
			triggerEvent(UISWTViewEvent.TYPE_FOCUSLOST, null);
		} catch (Exception e) {
			Debug.out(e);
		}
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.mdi.MdiEntry#updateUI()
	 */
	public void 
	updateUI() 
	{
		Utils.execSWTThread(new AERunnable() {
			public void 
			runSupport() 
			{
				if (!isDisposed()) {
					if (getEventListener() != null) {
					
						triggerEvent(UISWTViewEvent.TYPE_REFRESH, null);
					}
					refreshTitle();
				}
			}
		});
	}

	public boolean isDisposed() {
		return disposed;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.mdi.MdiEntry#getAutoOpenInfo()
	 */
	public Map<String, Object> getAutoOpenInfo() {
		Map<String, Object> autoOpenInfo = new LightHashMap<String, Object>();
		if (getParentID() != null) {
			autoOpenInfo.put("parentID", getParentID());
		}
		autoOpenInfo.put("title", getTitle());
		Object datasource = getDatasourceCore();
		if (datasource instanceof DownloadManager) {
			try {
				autoOpenInfo.put(
						"dm",
						((DownloadManager) datasource).getTorrent().getHashWrapper().toBase32String());
			} catch (Throwable t) {
			}
		} else if (datasource instanceof DownloadManager[]) {
			DownloadManager[] dms = (DownloadManager[]) datasource;
			List<String> list = new ArrayList<String>();
			for (DownloadManager dm : dms) {
				try {
					list.add(dm.getTorrent().getHashWrapper().toBase32String());
				} catch (Throwable e) {
				}
			}
			autoOpenInfo.put("dms", list);
		}
		
		String eds = getExportableDatasource();
		if (eds != null) {
			autoOpenInfo.put("datasource", eds.toString());
		}
		return autoOpenInfo;
	}
	
	public void setCloseable(boolean closeable) {
		this.closeable = closeable;

		if (mdi != null) {
  		if (closeable) {
  			mdi.informAutoOpenSet(this, getAutoOpenInfo());
  		} else {
  			mdi.removeEntryAutoOpen(id);
  		}
		}
	}

	// @see com.aelitis.azureus.ui.mdi.MdiEntry#setDefaultExpanded(boolean)
	public void setDefaultExpanded(boolean defaultExpanded) {
		COConfigurationManager.setBooleanDefault("SideBar.Expanded." + id,
				defaultExpanded);
	}

	public boolean isExpanded() {
		return isExpanded == null
				? COConfigurationManager.getBooleanParameter("SideBar.Expanded." + id)
				: isExpanded;
			}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.mdi.MdiEntry#setExpanded(boolean)
	 */
	public void setExpanded(boolean expanded) {
		isExpanded = expanded;
		boolean defExpanded = true;
		try {
			defExpanded = ConfigurationDefaults.getInstance().getBooleanParameter(
					"SideBar.Expanded." + id);
		} catch (ConfigurationParameterNotFoundException e) {
		}
		if (isExpanded == defExpanded) {
			COConfigurationManager.removeParameter("SideBar.Expanded." + id);
		} else {
			COConfigurationManager.setParameter("SideBar.Expanded." + id, isExpanded);
		}
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.mdi.MdiEntry#isAdded()
	 */
	public boolean isAdded() {
		return added;
	}

	public void setDisposed(boolean b) {
		disposed = b;
		added = !b;
		
		if (added) {
			if (getSkinObject() != null) {
				getSkinObject().triggerListeners(
						SWTSkinObjectListener.EVENT_DATASOURCE_CHANGED, datasource);
			}
		}
	}

	public void setImageLeftID(String id) {
		imageLeftID = id;
		imageLeft = null;
		redraw();
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.mdi.MdiEntry#getImageLeftID()
	 */
	public String getImageLeftID() {
		return imageLeftID;
	}

	/**
	 * @param imageLeft the imageLeft to set
	 */
	public void setImageLeft(Image imageLeft) {
		this.imageLeft = imageLeft;
		imageLeftID = null;
		redraw();
	}

	public Image getImageLeft(String suffix) {
		if (imageLeft != null) {
			return imageLeft;
		}
		if (imageLeftID == null) {
			return null;
		}
		Image img = null;
		if (suffix == null) {
			img = ImageLoader.getInstance().getImage(imageLeftID);
		} else {
			img = ImageLoader.getInstance().getImage(imageLeftID + suffix);
		}
		if (ImageLoader.isRealImage(img)) {
//			System.out.println("real" + getTitle() + "/" + img.getBounds() + Debug.getCompressedStackTrace());
			return img;
		}
		return null;
	}

	public void releaseImageLeft(String suffix) {
		if (imageLeft != null) {
			ImageLoader.getInstance().releaseImage(
					imageLeftID + (suffix == null ? "" : suffix));
		}
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoListener#viewTitleInfoRefresh(com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo)
	 */
	public void viewTitleInfoRefresh(ViewTitleInfo titleInfoToRefresh) {
		if (titleInfoToRefresh == null || this.viewTitleInfo != titleInfoToRefresh) {
			return;
		}
		if (isDisposed()) {
			return;
		}

		String imageID = (String) viewTitleInfo.getTitleInfoProperty(ViewTitleInfo.TITLE_IMAGEID);
		if (imageID != null) {
			setImageLeftID(imageID.length() == 0 ? null : imageID);
		}

		redraw();

		String logID = (String) viewTitleInfo.getTitleInfoProperty(ViewTitleInfo.TITLE_LOGID);
		if (logID != null) {
			setLogID(logID);
		}
	}

	public void build() {
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.mdi.MdiEntry#setPreferredAfterID(java.lang.String)
	 */
	public void setPreferredAfterID(String preferredAfterID) {
		this.preferredAfterID = preferredAfterID;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.mdi.MdiEntry#getPreferredAfterID()
	 */
	public String getPreferredAfterID() {
		return preferredAfterID;
	}
	
	public boolean requestClose() {
		return triggerEventRaw(UISWTViewEvent.TYPE_CLOSE, null);
	}
	
	/* (non-Javadoc)
	 * @see org.gudy.azureus2.core3.util.AEDiagnosticsEvidenceGenerator#generate(org.gudy.azureus2.core3.util.IndentWriter)
	 */
	public void generate(IndentWriter writer) {
		writer.println("View: " + id + ": " + getTitle());

		try {
			writer.indent();

			writer.println("Parent: " + getParentID());
			//writer.println("Created: " + created);
			writer.println("Added: " + added);
			writer.println("closeable: " + closeable);
			writer.println("Disposed: " + disposed);
			writer.println("hasBeenOpened: " + hasBeenOpened);
			//writer.println("hasFocus: " + hasFocus);
			//writer.println("haveSentInitialize: " + haveSentInitialize);
			writer.println("control type: " + getControlType());
			writer.println("hasEventListener: " + (getEventListener() != null));
			writer.println("hasViewTitleInfo: " + (viewTitleInfo != null));
			writer.println("skinRef: " + skinRef);
		} catch (Exception e) {

		} finally {

			writer.exdent();
		}

		if (getEventListener() instanceof AEDiagnosticsEvidenceGenerator) {

			try {
				writer.indent();

				((AEDiagnosticsEvidenceGenerator) getEventListener()).generate(writer);
			} catch (Exception e) {

			} finally {

				writer.exdent();
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.mdi.UISWTViewImpl2#closeView()
	 */
	@Override
	public void closeView() {
		// This essentially calls mdi.closeEntry(id)
		//UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
		//if (uiFunctions != null) {
		//	uiFunctions.closePluginView(this);
		//}
		if (mdi != null) {
			mdi.closeEntry(id);
		}

		super.closeView();
	}
	
	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewImpl2#setEventListener(org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener, boolean)
	 */
	@Override
	public void setEventListener(UISWTViewEventListener _eventListener,
			boolean doCreate)
	throws UISWTViewEventCancelledException {
		UISWTViewEventListener eventListener = getEventListener();
		if (eventListener instanceof UIToolBarEnablerBase) {
			removeToolbarEnabler((UIToolBarEnablerBase) eventListener);
		}
		if ((eventListener instanceof ViewTitleInfo) && viewTitleInfo == eventListener) {
			setViewTitleInfo(null);
		}
		
		if (_eventListener instanceof UISWTViewEventListenerHolder) {
			UISWTViewEventListenerHolder h = (UISWTViewEventListenerHolder) _eventListener;
			UISWTViewEventListener delegatedEventListener = h.getDelegatedEventListener(this);
			if (delegatedEventListener != null) {
				_eventListener = delegatedEventListener;
			}
		}

		if (_eventListener instanceof UIToolBarEnablerBase) {
			addToolbarEnabler((UIToolBarEnablerBase) _eventListener);
		}
		if ((_eventListener instanceof ViewTitleInfo) && viewTitleInfo == null) {
			setViewTitleInfo((ViewTitleInfo) _eventListener);
		}
	
	
		if (_eventListener instanceof BasicPluginViewImpl) {
			String existing_id = getImageLeftID();
			
			if (existing_id==null||"image.sidebar.plugin".equals(existing_id)) {
				setImageLeftID("image.sidebar.logview");
			}
		}
	

		super.setEventListener(_eventListener, doCreate);
	}
	
	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.mdi.UISWTViewImpl2#setDatasource(java.lang.Object)
	 */
	@Override
	public void setDatasource(Object datasource) {
		super.setDatasource(datasource);

		triggerDatasourceListeners();
		if (isAdded()) {
			if (getSkinObject() != null) {
				getSkinObject().triggerListeners(
						SWTSkinObjectListener.EVENT_DATASOURCE_CHANGED, datasource);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.mdi.UISWTViewImpl2#setTitle(java.lang.String)
	 */
	@Override
	public void setTitle(String title) {
		super.setTitle(title);
		redraw();
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

}