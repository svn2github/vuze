package com.aelitis.azureus.ui.swt.mdi;

import java.util.*;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.ConfigurationDefaults;
import org.gudy.azureus2.core3.config.impl.ConfigurationParameterNotFoundException;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.LightHashMap;
import org.gudy.azureus2.plugins.ui.UIPluginView;
import org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarEnablerBase;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.*;

import com.aelitis.azureus.ui.common.ToolBarEnabler;
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
	implements MdiEntrySWT, ViewTitleInfoListener
{
	protected final MultipleDocumentInterface mdi;

	protected final String id;

	protected String logID;

	protected Object datasource;

	protected Class<? extends UIPluginView> viewClass;

	protected UISWTViewCore view;

	private String skinRef;

	private List<MdiCloseListener> listCloseListeners = null;

	private List<MdiChildCloseListener> listChildCloseListeners = null;

	private List<MdiEntryLogIdListener> listLogIDListeners = null;

	private List<MdiEntryOpenListener> listOpenListeners = null;

	private List<MdiEntryDropListener> listDropListeners = null;

	private List<MdiEntryDatasourceListener> listDatasourceListeners = null;

	protected ViewTitleInfo viewTitleInfo;

	private SWTSkinObject skinObject;

	private String title;

	private String titleID;

	private UISWTViewEventListener eventListener;

	private String parentID;

	private boolean pullTitleFromView;

	private boolean closeable;

	private Boolean isExpanded = null;

	private boolean disposed = false;

	private boolean added = false;

	private String imageLeftID;

	private Image imageLeft;

	private boolean collapseDisabled = false;

	private SWTSkinObject soMaster;

	private Set<UIToolBarEnablerBase> setToolBarEnablers = new HashSet<UIToolBarEnablerBase>(1);

	private String preferredAfterID;

	private Map<Object,Object>	user_data;
	
	@SuppressWarnings("unused")
	private BaseMdiEntry() {
		mdi = null;
		id = null;
		setDefaultExpanded(false);
	}

	public BaseMdiEntry(MultipleDocumentInterface mdi, String id) {
		this.mdi = mdi;
		this.id = id;
		this.pullTitleFromView = true;

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

	public String getId() {
		return id;
	}

	public MdiEntryVitalityImage addVitalityImage(String imageID) {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.mdi.MdiEntry#close()
	 */
	public boolean close(boolean forceClose) {
		if (!forceClose && view instanceof UISWTViewImpl) {
			if (!((UISWTViewImpl) view).requestClose()) {
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

	public String getExportableDatasource() {
		if (viewTitleInfo != null) {
			Object ds = viewTitleInfo.getTitleInfoProperty(ViewTitleInfo2.TITLE_EXPORTABLE_DATASOURCE);
			if (ds != null) {
				return ds.toString();
			}
		}
		return null;
	}
	
	public Object getDatasource() {
		return PluginCoreUtils.convert(datasource, false);
	}

	public void setDatasource(Object datasource) {
		this.datasource = datasource;

		triggerDatasourceListeners();

		if (view != null) {
			view.triggerEvent(UISWTViewEvent.TYPE_DATASOURCE_CHANGED, datasource);
		}

		if (isAdded()) {
			if (skinObject != null) {
				skinObject.triggerListeners(
						SWTSkinObjectListener.EVENT_DATASOURCE_CHANGED, datasource);
			}
		}
	}

	public UIPluginView getView() {
		return view;
	}

	public UISWTViewCore getCoreView() {
		return view;
	}

	public Class<? extends UIPluginView> getViewClass() {
		return viewClass;
	}

	public void setViewClass(Class<? extends UIPluginView> viewClass) {
		this.viewClass = viewClass;
	}

	public String getLogID() {
		return logID;
	}

	public MultipleDocumentInterface getMDI() {
		return mdi;
	}

	public String getParentID() {
		return parentID;
	}

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
		parentID = id;
		// ensure parent gets created if it isn't there already
		mdi.loadEntryByID(parentID, false);
	}

	public MdiEntryVitalityImage[] getVitalityImages() {
		return null;
	}

	public boolean isCloseable() {
		return closeable;
	}

	public boolean isCollapseDisabled() {
		return collapseDisabled;
	}

	public void setCollapseDisabled(boolean collapseDisabled) {
		this.collapseDisabled = collapseDisabled;
		setExpanded(true);
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

		MdiEntry parentEntry = mdi.getEntry(parentID);
		if (parentEntry instanceof BaseMdiEntry) {
			((BaseMdiEntry) parentEntry).triggerChildCloseListeners(this, user);
		}
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
		if (listChildCloseListeners == null) {
			return;
		}
		Object[] list;
		synchronized (this) {
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

		if (view != null) {
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

			String newTitle = (String) viewTitleInfo.getTitleInfoProperty(ViewTitleInfo.TITLE_TEXT);
			if (newTitle != null) {
				setPullTitleFromView(false);
				setTitle(newTitle);
			}

			String imageID = (String) viewTitleInfo.getTitleInfoProperty(ViewTitleInfo.TITLE_IMAGEID);
			if (imageID != null) {
				setImageLeftID(imageID.length() == 0 ? null : imageID);
			}

			ViewTitleInfoManager.addListener(this);
		}
	}

	/**
	 * @deprecated For azburn
	 */
	// @see com.aelitis.azureus.ui.mdi.MdiEntry#addToolbarEnabler(com.aelitis.azureus.ui.common.ToolBarEnabler)
	public void addToolbarEnabler(ToolBarEnabler enabler) {
		addToolbarEnabler((UIToolBarEnablerBase) enabler);
	}

	public void addToolbarEnabler(UIToolBarEnablerBase enabler) {
		setToolBarEnablers.add(enabler);
		setToolbarVisibility(setToolBarEnablers.size() > 0);
	}

	protected void setToolbarVisibility(boolean visible) {
	}

	public void removeToolbarEnabler(UIToolBarEnablerBase enabler) {
		setToolBarEnablers.remove(enabler);
		setToolbarVisibility(setToolBarEnablers.size() > 0);
	}

	public UIToolBarEnablerBase[] getToolbarEnablers() {
		if (view != null) {
			UIPluginViewToolBarListener listener = view.getToolBarListener();
			if (listener != null) {
				return new UIToolBarEnablerBase[] { listener };
			}
		}
		return setToolBarEnablers.toArray(new UIToolBarEnablerBase[0]);
	}

	public void setCoreView(UISWTViewCore view) {
		if (this.view != null && view == null) {
  		this.view.triggerEvent(UISWTViewEvent.TYPE_DESTROY, null);
		}
		this.view = view;
		if (view == null) {
			return;
		}

		UISWTViewEventListener eventListener = view.getEventListener();
		if (view instanceof ViewTitleInfo) {
			setViewTitleInfo((ViewTitleInfo) view);
		} else if (eventListener instanceof ViewTitleInfo) {
			setViewTitleInfo((ViewTitleInfo) eventListener);
		}

		if (view instanceof UIToolBarEnablerBase) {
			addToolbarEnabler((UIToolBarEnablerBase) view);
		} else if (eventListener instanceof UIToolBarEnablerBase) {
			addToolbarEnabler((UIToolBarEnablerBase) eventListener);
		}

		if (datasource != null) {
			view.triggerEvent(UISWTViewEvent.TYPE_DATASOURCE_CHANGED, datasource);
		}
	}

	public SWTSkinObject getSkinObject() {
		return skinObject;
	}

	public void setSkinObject(SWTSkinObject skinObject, SWTSkinObject soMaster) {
		this.skinObject = skinObject;
		this.soMaster = soMaster;
		if (datasource != null) {
			if (skinObject != null) {
				skinObject.triggerListeners(
						SWTSkinObjectListener.EVENT_DATASOURCE_CHANGED, datasource);
			}
			if (view != null) {
				view.triggerEvent(UISWTViewEvent.TYPE_DATASOURCE_CHANGED, datasource);
			}
		}
		if (skinObject != null) {
			setToolbarVisibility(setToolBarEnablers.size() > 0);
		}
	}

	public SWTSkinObject getSkinObjectMaster() {
		if (soMaster == null) {
			return skinObject;
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

	public String getTitle() {
		if (title != null && !isPullTitleFromView()) {
			return title;
		}
		if (view != null) {
			return view.getFullTitle();
		}
		return title;
	}

	public void setTitle(String title) {
		if (title == null) {
			return;
		}
		if (title.startsWith("{") && title.endsWith("}") && title.length() > 2) {
			setTitleID(title.substring(1, title.length() - 1));
			return;
		}
		if (title.equals(this.title)) {
			return;
		}
		this.title = title;
		this.titleID = null;
		redraw();
	}

	public void setTitleID(String titleID) {
		String title = MessageText.getString(titleID);
		setTitle(title.startsWith("{") ? title.substring(1) : title);
		this.titleID = titleID;
	}

	public void updateLanguage() {
		if (view != null) {
			view.triggerEvent(UISWTViewEvent.TYPE_LANGUAGEUPDATE, null);
		}
		if (titleID != null) {
			setTitleID(titleID);
		} else {
			if (viewTitleInfo != null) {
				viewTitleInfoRefresh(viewTitleInfo);
			}
			updateUI();
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
		if (view != null) {
			Composite c = view.getComposite();
			if (c != null && !c.isDisposed()) {
				c.setData("BaseMDIEntry", this);
				c.setVisible(true);
				c.getParent().layout();
			}

			try {
				view.triggerEvent(UISWTViewEvent.TYPE_FOCUSGAINED, null);
			} catch (Exception e) {
				Debug.out(e);
			}
		}
	}

	public void hide() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				swt_hide();
			}
		});
	}
	
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
		if (view != null) {
			Composite oldComposite = view.getComposite();
			if (oldComposite != null && !oldComposite.isDisposed()) {

				oldComposite.setVisible(false);
				oldComposite.getShell().update();
			}

			try {
				view.triggerEvent(UISWTViewEvent.TYPE_FOCUSLOST, null);
			} catch (Exception e) {
				Debug.out(e);
			}
		}
	}

	public UISWTViewEventListener getEventListener() {
		return eventListener;
	}

	public void setEventListener(UISWTViewEventListener _eventListener) {
		this.eventListener = _eventListener;

		if (view != null) {
			return;
		}
		try {
			setCoreView(new UISWTViewImpl(parentID, id, _eventListener,
					datasource));
		} catch (Exception e) {
			Debug.out(e);
		}

		if (_eventListener instanceof UISWTViewEventListenerHolder) {
			UISWTViewEventListenerHolder h = (UISWTViewEventListenerHolder) _eventListener;
			UISWTViewEventListener delegatedEventListener = h.getDelegatedEventListener(view);
			if (delegatedEventListener != null) {
				this.eventListener = delegatedEventListener;
			}
		}

		if (eventListener instanceof UIToolBarEnablerBase) {
			addToolbarEnabler((UIToolBarEnablerBase) eventListener);
		}
		if ((eventListener instanceof ViewTitleInfo) && viewTitleInfo == null) {
			setViewTitleInfo((ViewTitleInfo) eventListener);
		}


		if (eventListener instanceof BasicPluginViewImpl) {
			if ("image.sidebar.plugin".equals(getImageLeftID())) {
				setImageLeftID("image.sidebar.logview");
			}
		}
	}

	public boolean isPullTitleFromView() {
		return pullTitleFromView;
	}

	public void setPullTitleFromView(boolean pullTitleFromView) {
		this.pullTitleFromView = pullTitleFromView;
	}

	public void updateUI() {
		if (view == null) {
			return;
		}
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (view != null && !isDisposed()) {
					view.triggerEvent(UISWTViewEvent.TYPE_REFRESH, null);
				}
				if (isPullTitleFromView() && isAdded()) {
					setTitle(view.getFullTitle());
				}
			}
		});
	}

	public boolean isDisposed() {
		return disposed;
	}

	public Map<String, Object> getAutoOpenInfo() {
		Map<String, Object> autoOpenInfo = new LightHashMap<String, Object>();
		if (getParentID() != null) {
			autoOpenInfo.put("parentID", getParentID());
		}
		UISWTViewEventListener eventListener = getEventListener();
		if (eventListener != null) {
			autoOpenInfo.put("eventListenerClass",
					eventListener.getClass().getName());
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
		if (closeable) {

			mdi.informAutoOpenSet(this, getAutoOpenInfo());
			COConfigurationManager.setParameter("SideBar.AutoOpen." + id, true);
		} else {
			COConfigurationManager.removeParameter("SideBar.AutoOpen." + id);
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

	public boolean isAdded() {
		return added;
	}

	public void setDisposed(boolean b) {
		disposed = b;
		added = !b;
	}

	public void setImageLeftID(String id) {
		imageLeftID = id;
		imageLeft = null;
		redraw();
	}

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

	public void viewTitleInfoRefresh(ViewTitleInfo titleInfoToRefresh) {
		if (titleInfoToRefresh == null || this.viewTitleInfo != titleInfoToRefresh) {
			return;
		}
		if (isDisposed()) {
			return;
		}

		String newText = (String) viewTitleInfo.getTitleInfoProperty(ViewTitleInfo.TITLE_TEXT);
		if (newText != null) {
			setPullTitleFromView(false);
			setTitle(newText);
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

	public void setPreferredAfterID(String preferredAfterID) {
		this.preferredAfterID = preferredAfterID;
	}

	public String getPreferredAfterID() {
		return preferredAfterID;
	}
	
	public void
	setUserData(
		Object		key,
		Object		data )
	{
		synchronized( this ){
			
			if ( user_data == null ){
				
				user_data = new LightHashMap<Object,Object>();
			}
			
			user_data.put( key, data );
		}
	}
	
	public Object
	getUserData(
		Object 		key )
	{
		synchronized( this ){
			
			if ( user_data == null ){
				
				return( null );
			}
			
			return( user_data.get( key ));
		}
	}
}