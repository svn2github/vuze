package com.aelitis.azureus.ui.swt.mdi;

import java.util.*;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.LightHashMap;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.PluginsMenuHelper;
import org.gudy.azureus2.ui.swt.mainwindow.PluginsMenuHelper.IViewInfo;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.BasicPluginViewImpl;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewImpl;
import org.gudy.azureus2.ui.swt.views.IView;
import org.gudy.azureus2.ui.swt.views.IViewExtension;

import com.aelitis.azureus.ui.common.ToolBarEnabler;
import com.aelitis.azureus.ui.common.table.TableView;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoListener;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.mdi.*;
import com.aelitis.azureus.ui.selectedcontent.ISelectedContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentV3;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectContainer;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectListener;
import com.aelitis.azureus.ui.swt.toolbar.ToolBarItem;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager;
import com.aelitis.azureus.ui.swt.views.skin.ToolBarView;

public abstract class BaseMdiEntry
	implements MdiEntrySWT, ViewTitleInfoListener
{
	protected final MultipleDocumentInterface mdi;

	protected final String id;

	protected String logID;

	protected Object datasource;

	protected Class<?> iviewClass;

	protected Class<?>[] iviewClassArgs;

	protected Object[] iviewClassVals;

	protected IView iview;

	private String skinRef;

	private List<MdiCloseListener> listCloseListeners = null;

	private List<MdiEntryLogIdListener> listLogIDListeners = null;

	private List<MdiEntryOpenListener> listOpenListeners = null;

	private List<MdiEntryDropListener> listDropListeners = null;

	protected ViewTitleInfo viewTitleInfo;

	private SWTSkinObject skinObject;

	private String title;

	private UISWTViewEventListener eventListener;

	private String parentID;

	private boolean pullTitleFromIView;

	private boolean closeable;

	private boolean isExpanded = false;

	private boolean disposed;

	private String imageLeftID;

	private Image imageLeft;

	private boolean collapseDisabled;

	private SWTSkinObject soMaster;
	
	private Set<ToolBarEnabler> setToolBarEnablers = new HashSet<ToolBarEnabler>(1);

	@SuppressWarnings("unused")
	private BaseMdiEntry() {
		mdi = null;
		id = null;
	}

	public BaseMdiEntry(MultipleDocumentInterface mdi, String id) {
		this.mdi = mdi;
		this.id = id;
		this.pullTitleFromIView = true;

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
		if (!forceClose && iview instanceof UISWTViewImpl) {
			if (!((UISWTViewImpl) iview).requestClose()) {
				return false;
			}
		}

		disposed = true;
		ViewTitleInfoManager.removeListener(this);

		return true;
	}

	public Object getDatasourceCore() {
		return datasource;
	}

	public Object getDatasource() {
		return PluginCoreUtils.convert(datasource, false);
	}

	public void setDatasource(Object datasource) {
		this.datasource = datasource;

		if (isAdded()) {
			if (iview != null) {
				iview.dataSourceChanged(datasource);
			}
			if (skinObject != null) {
				skinObject.triggerListeners(
						SWTSkinObjectListener.EVENT_DATASOURCE_CHANGED, datasource);
			}
		}
	}

	public IView getIView() {
		return iview;
	}

	public Class<?> getIViewClass() {
		return iviewClass;
	}

	public void setIViewClass(Class<?> iviewClass, Class<?>[] iviewClassArgs,
			Object[] iviewClassVals) {
		this.iviewClass = iviewClass;
		this.iviewClassArgs = iviewClassArgs;
		this.iviewClassVals = iviewClassVals;
	}

	public Class<?>[] getIViewClassArgs() {
		return iviewClassArgs;
	}

	public Object[] getIViewClassVals() {
		return iviewClassVals;
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
		parentID = id;
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
		if (listCloseListeners == null) {
			return;
		}
		Object[] list;
		synchronized (this) {
			list = listCloseListeners.toArray();
		}
		for (int i = 0; i < list.length; i++) {
			MdiCloseListener l = (MdiCloseListener) list[i];
			try {
				l.mdiEntryClosed(this, user);
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

		if (iview != null) {
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
		this.viewTitleInfo = viewTitleInfo;
		// TODO: Need to listen for viewTitleInfo triggers so we can refresh items below
		if (viewTitleInfo != null) {
			String newTitle = (String) viewTitleInfo.getTitleInfoProperty(ViewTitleInfo.TITLE_TEXT);
			if (newTitle != null) {
				setPullTitleFromIView(false);
				setTitle(newTitle);
			}

			String imageID = (String) viewTitleInfo.getTitleInfoProperty(ViewTitleInfo.TITLE_IMAGEID);
			if (imageID != null) {
				setImageLeftID(imageID.length() == 0 ? null : imageID);
			}

			ViewTitleInfoManager.addListener(this);
		}
	}

	public void addToolbarEnabler(ToolBarEnabler enabler) {
		setToolBarEnablers.add(enabler);
	}
	
	public void removeToolbarEnabler(ToolBarEnabler enabler) {
		setToolBarEnablers.remove(enabler);
	}
	
	public ToolBarEnabler[] getToolbarEnablers() {
		return setToolBarEnablers.toArray(new ToolBarEnabler[0]);
	}
	
	public void setIView(IView iview) {
		this.iview = iview;
		if (iview instanceof ViewTitleInfo) {
			setViewTitleInfo((ViewTitleInfo) iview);
		} else if (iview instanceof UISWTViewImpl) {
			UISWTViewEventListener eventListener = ((UISWTViewImpl) iview).getEventListener();
			if (eventListener instanceof ViewTitleInfo) {
				setViewTitleInfo((ViewTitleInfo) eventListener);
			}
		}
		
		if (iview instanceof ToolBarEnabler) {
			addToolbarEnabler((ToolBarEnabler) iview);
		}

		if (iview != null) {
			if (title == null) {
				setTitle(iview.getFullTitle());
			}
			if (datasource != null) {
				try {
					iview.dataSourceChanged(datasource);
				} catch (Exception e) {
					Debug.out(e);
				}
			}
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
			if (iview != null) {
				iview.dataSourceChanged(datasource);
			}
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
		return title;
	}

	public void setTitle(String title) {
		if (title == null) {
			return;
		}
		if (title != null && title.equals(this.title)) {
			return;
		}
		this.title = title;
		redraw();
	}

	public void show() {
		if (skinObject == null) {
			return;
		}

		SelectedContentManager.clearCurrentlySelectedContent();

		disableViewModes();

		UIFunctionsSWT uif = UIFunctionsManagerSWT.getUIFunctionsSWT();
		if (uif != null) {
			//uif.refreshIconBar(); // needed?
			uif.refreshTorrentMenu();
		}

		SWTSkinObject skinObject = getSkinObjectMaster();
		if (skinObject instanceof SWTSkinObjectContainer) {
			SWTSkinObjectContainer container = (SWTSkinObjectContainer) skinObject;
			container.setVisible(true);
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
		if (iview != null) {
			Composite c = iview.getComposite();
			if (c != null && !c.isDisposed()) {
				c.setVisible(true);
				c.getParent().layout();
			}
		}

		try {
			if (iview instanceof IViewExtension) {
				((IViewExtension) iview).viewActivated();
			} else if (iview instanceof UISWTView) {
				((UISWTView) iview).triggerEvent(UISWTViewEvent.TYPE_FOCUSGAINED, null);
			}
		} catch (Exception e) {
			Debug.out(e);
		}
	}

	public void hide() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				swt_hide();
			}
		});
	}

	private void swt_hide() {
		SWTSkinObject skinObjectMaster = getSkinObjectMaster();
		if (skinObjectMaster instanceof SWTSkinObjectContainer) {
			SWTSkinObjectContainer container = (SWTSkinObjectContainer) skinObjectMaster;
			if (container != null) {
				Control oldComposite = container.getControl();

				container.setVisible(false);
				if (!oldComposite.isDisposed()) {
					oldComposite.getShell().update();
				}
			}
		}
		if (iview != null) {
			Composite oldComposite = iview.getComposite();
			if (oldComposite != null && !oldComposite.isDisposed()) {

				oldComposite.setVisible(false);
				oldComposite.getShell().update();
			}
		}

		try {
			if (iview instanceof IViewExtension) {
				((IViewExtension) iview).viewDeactivated();
			} else if (iview instanceof UISWTView) {
				((UISWTView) iview).triggerEvent(UISWTViewEvent.TYPE_FOCUSLOST, null);
			}
		} catch (Exception e) {
			Debug.out(e);
		}
	}

	public UISWTViewEventListener getEventListener() {
		return eventListener;
	}

	public void setEventListener(UISWTViewEventListener _eventListener) {
		this.eventListener = _eventListener;
		
		if (eventListener instanceof ToolBarEnabler) {
			addToolbarEnabler((ToolBarEnabler) eventListener);
		}
		if ((eventListener instanceof ViewTitleInfo) && viewTitleInfo == null) {
			setViewTitleInfo((ViewTitleInfo) eventListener);
		}

		UISWTViewEventListener eventListenerDelegate = _eventListener;
		/*
		UISWTViewEventListener eventListenerDelegate = new UISWTViewEventListener() {
			public boolean eventOccurred(UISWTViewEvent event) {
				switch (event.getType()) {
					case UISWTViewEvent.TYPE_CREATE:
						break;

					case UISWTViewEvent.TYPE_DATASOURCE_CHANGED:
						if (skinObject != null) {
							skinObject.triggerListeners(
									SWTSkinObjectListener.EVENT_DATASOURCE_CHANGED,
									event.getData());
						}
						break;

					default:
						break;
				}
				return eventListener.eventOccurred(event);
			}
		};
		*/
		if (iview != null) {
			return;
		}
		try {
			IView iview = new UISWTViewImpl(parentID, id, eventListenerDelegate,
					datasource);
			setIView(iview);

			IViewInfo foundViewInfo = PluginsMenuHelper.getInstance().findIViewInfo(
					eventListener);

			String title;
			if (foundViewInfo != null) {
				title = foundViewInfo.name;
			} else {
				title = iview.getFullTitle();
			}
			((UISWTViewImpl) iview).setTitle(title);
			setTitle(title);
		} catch (Exception e) {
			Debug.out(e);
		}
		
		if ((_eventListener instanceof BasicPluginViewImpl)
				&& getImageLeftID() == null) {
			setImageLeftID("image.sidebar.logview");
		}
	}

	protected void disableViewModes() {
		ToolBarView tb = (ToolBarView) SkinViewManager.getByClass(ToolBarView.class);
		if (tb != null) {
			ToolBarItem itemModeSmall = tb.getToolBarItem("modeSmall");
			if (itemModeSmall != null) {
				itemModeSmall.getSkinButton().getSkinObject().switchSuffix("");
				itemModeSmall.setEnabled(false);
			}
			ToolBarItem itemModeBig = tb.getToolBarItem("modeBig");
			if (itemModeBig != null) {
				itemModeBig.getSkinButton().getSkinObject().switchSuffix("");
				itemModeBig.setEnabled(false);
			}
		}
	}

	public boolean isPullTitleFromIView() {
		return pullTitleFromIView;
	}

	public void setPullTitleFromIView(boolean pullTitleFromIView) {
		this.pullTitleFromIView = pullTitleFromIView;
	}

	public void updateUI() {
		if (iview == null) {
			return;
		}
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (iview != null && !isDisposed()) {
					iview.refresh();
				}
				if (isPullTitleFromIView() && isAdded()) {
					setTitle(iview.getFullTitle());
				}
			}
		});
	}

	public boolean isDisposed() {
		return disposed;
	}

	public void setCloseable(boolean closeable) {
		this.closeable = closeable;
		if (closeable) {
			Map<String, Object> autoOpenInfo = new LightHashMap<String, Object>();
			if (getParentID() != null) {
				autoOpenInfo.put("parentID", getParentID());
			}
			if (getIViewClass() != null) {
				autoOpenInfo.put("iviewClass", getIViewClass().getName());
			}
			if (getEventListener() != null) {
				autoOpenInfo.put("eventlistenerid", id);
			}
			if (getIView() != null) {
				autoOpenInfo.put("title", getIView().getFullTitle());
			}
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
			} else if (datasource != null) {
				autoOpenInfo.put("datasource", datasource.toString());
			}

			mdi.informAutoOpenSet(this, autoOpenInfo);
			COConfigurationManager.setParameter("SideBar.AutoOpen." + id, true);
		} else {
			COConfigurationManager.removeParameter("SideBar.AutoOpen." + id);
		}
	}

	public boolean isExpanded() {
		return isExpanded;
	}

	public void setExpanded(boolean expanded) {
		isExpanded = expanded;
	}

	public boolean isAdded() {
		return !isDisposed();
	}

	public void setDisposed(boolean b) {
		disposed = b;
	}

	public void setImageLeftID(String id) {
		imageLeftID = id;
		imageLeft = null;
		redraw();
	}

	public String getImageLeftID() {
		return (imageLeftID);
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
			return img;
		}
		return null;
	}

	public void releaseImageLeft(String suffix) {
		if (imageLeft != null) {
			ImageLoader.getInstance().releaseImage(imageLeftID + suffix);
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
			setPullTitleFromIView(false);
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
}
