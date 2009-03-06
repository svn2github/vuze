/**
 * Created on Aug 13, 2008
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
 
package com.aelitis.azureus.ui.swt.views.skin.sidebar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.ui.swt.IconBarEnabler;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.IconBar.IconBarListener;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.views.IView;

import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;

import org.gudy.azureus2.plugins.ui.sidebar.*;

/**
 * @author TuxPaper
 * @created Aug 13, 2008
 *
 */
public class SideBarEntrySWT implements SideBarEntry
{
	final public String id;
	
	public String parentID;

	public Object datasource;

	public ViewTitleInfo titleInfo;

	SWTSkinObject skinObject;

	TreeItem treeItem;

	boolean pullTitleFromIView;

	public IView iview;

	public boolean closeable;

	public UISWTViewEventListener eventListener;

	public Class iviewClass;

	public Class[] iviewClassArgs;

	public Object[] iviewClassVals;
	
	public boolean disableCollapse;
	
	private List listVitalityImages = Collections.EMPTY_LIST;

	private String imageLeftID;
	
	private List listCloseListeners = Collections.EMPTY_LIST;

	private List listLogIDListeners = Collections.EMPTY_LIST;

	private List listOpenListeners = Collections.EMPTY_LIST;

	private List listDropListeners = Collections.EMPTY_LIST;

	private IconBarEnabler iconBarEnabler;

	private final SideBar sidebar;
	
	private String logID;

	private Image imageLeft;
	
	public SideBarEntrySWT(SideBar sidebar, String id) {
		this.id = id;
		
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

		this.sidebar = sidebar;
	}
	
	public String getParentID() {
		return parentID;
	}

	public void setParentID(String parentID) {
		this.parentID = parentID;
	}

	public Object getDatasourceCore() {
		return datasource;
	}

	public Object getDatasource() {
		return PluginCoreUtils.convert(datasource, false);
	}

	public void setDatasource(Object datasource) {
		this.datasource = datasource;
	}

	public ViewTitleInfo getTitleInfo() {
		return titleInfo;
	}

	public void setTitleInfo(ViewTitleInfo titleInfo) {
		this.titleInfo = titleInfo;
		
		sidebar.linkTitleInfoToEntry(titleInfo, this);
		
		if (treeItem != null && !treeItem.isDisposed()) {
  		String newText = (String) titleInfo.getTitleInfoProperty(ViewTitleInfo.TITLE_TEXT);
  		if (newText != null) {
  			pullTitleFromIView = false;
  			treeItem.setData("text", newText);
  			redraw();
  		}
		}
	}

	public SWTSkinObject getSkinObject() {
		return skinObject;
	}

	public void setSkinObject(SWTSkinObject skinObject) {
		this.skinObject = skinObject;
	}

	public TreeItem getTreeItem() {
		return treeItem;
	}

	public void setTreeItem(TreeItem treeItem) {
		this.treeItem = treeItem;
	}

	public boolean isPullTitleFromIView() {
		return pullTitleFromIView;
	}

	public void setPullTitleFromIView(boolean pullTitleFromIView) {
		this.pullTitleFromIView = pullTitleFromIView;
	}

	public IView getIView() {
		return iview;
	}

	public void setIView(IView iview) {
		this.iview = iview;
	}

	public boolean isCloseable() {
		return closeable;
	}

	public void setCloseable(boolean closeable) {
		this.closeable = closeable;
	}

	public UISWTViewEventListener getEventListener() {
		return eventListener;
	}

	public void setEventListener(UISWTViewEventListener eventListener) {
		this.eventListener = eventListener;
	}

	public Class getIViewClass() {
		return iviewClass;
	}

	public void setIViewClass(Class iviewClass) {
		this.iviewClass = iviewClass;
	}

	public Class[] getIViewClassArgs() {
		return iviewClassArgs;
	}

	public void setIViewClassArgs(Class[] iviewClassArgs) {
		this.iviewClassArgs = iviewClassArgs;
	}

	public Object[] getIViewClassVals() {
		return iviewClassVals;
	}

	public void setIViewClassVals(Object[] iviewClassVals) {
		this.iviewClassVals = iviewClassVals;
	}

	public String getId() {
		return id;
	}

	// @see org.gudy.azureus2.plugins.ui.sidebar.SideBarEntry#addVitalityImage(java.lang.String)
	public SideBarVitalityImage addVitalityImage(String imageID) {
		SideBarVitalityImageSWT vitalityImage = new SideBarVitalityImageSWT(this, imageID);
		if (listVitalityImages == Collections.EMPTY_LIST) {
			listVitalityImages = new ArrayList(1);
		}
		listVitalityImages.add(vitalityImage);
		return vitalityImage;
	}
	
	public SideBarVitalityImage[] getVitalityImages() {
		return (SideBarVitalityImage[]) listVitalityImages.toArray(new SideBarVitalityImage[0]);
	}
	
	public SideBarVitalityImage getVitalityImage(int hitX, int hitY) {
		SideBarVitalityImage[] vitalityImages = getVitalityImages();
		for (int i = 0; i < vitalityImages.length; i++) {
			SideBarVitalityImageSWT vitalityImage = (SideBarVitalityImageSWT) vitalityImages[i];
			if (!vitalityImage.isVisible()) {
				continue;
			}
			Rectangle hitArea = vitalityImage.getHitArea();
			if (hitArea != null && hitArea.contains(hitX, hitY)) {
				return vitalityImage;
			}
		}
		return null;
	}
	
	public void redraw() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (treeItem == null || treeItem.isDisposed()) {
					return;
				}
				Tree tree = treeItem.getParent();
				Rectangle bounds = treeItem.getBounds();
				Rectangle treeBounds = tree.getBounds();
				tree.redraw(0, bounds.y, treeBounds.width, bounds.height, true);
				tree.update();
			}
		});
	}
	
	public Rectangle getBounds() {
		if (treeItem == null || treeItem.isDisposed()) {
			return null;
		}
		Tree tree = treeItem.getParent();
		Rectangle bounds = treeItem.getBounds();
		Rectangle treeBounds = tree.getBounds();
		return new Rectangle(0, bounds.y, treeBounds.width, bounds.height);
	}
	
	public void setImageLeftID(String id) {
		imageLeftID = id;
		imageLeft = null;
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
	
	/**
	 * @param l
	 *
	 * @since 4.0.0.5
	 */
	public void addListener(SideBarCloseListener l) {
		if (listCloseListeners == Collections.EMPTY_LIST) {
			listCloseListeners = new ArrayList(1);
		}
		listCloseListeners.add(l);
	}
	
	public void removeListener(SideBarCloseListener l) {
		listCloseListeners.remove(l);
	}

	protected void triggerCloseListeners() {
		Object[] list = listCloseListeners.toArray();
		for (int i = 0; i < list.length; i++) {
			SideBarCloseListener l = (SideBarCloseListener) list[i];
			try {
				l.sidebarClosed(this);
			} catch (Exception e) {
				Debug.out(e);
			}
		}
	}

	public void addListener(SideBarLogIdListener l) {
		if (listLogIDListeners == Collections.EMPTY_LIST) {
			listLogIDListeners = new ArrayList(1);
		}
		listLogIDListeners.add(l);
	}
	
	public void removeListener(SideBarLogIdListener sideBarLogIdListener) {
		listLogIDListeners.remove(sideBarLogIdListener);
	}

	protected void triggerLogIDListeners(String oldID) {
		Object[] list = listLogIDListeners.toArray();
		for (int i = 0; i < list.length; i++) {
			SideBarLogIdListener l = (SideBarLogIdListener) list[i];
			l.sidebarLogIdChanged(this, oldID, logID);
		}
	}

	public void addListener(SideBarOpenListener l) {
		if (listOpenListeners == Collections.EMPTY_LIST) {
			listOpenListeners = new ArrayList(1);
		}
		listOpenListeners.add(l);
		if (treeItem != null) {
			l.sideBarEntryOpen(this);
		}
	}
	
	public void removeListener(SideBarOpenListener l) {
		listOpenListeners.remove(l);
	}

	protected void triggerOpenListeners() {
		Object[] list = listOpenListeners.toArray();
		for (int i = 0; i < list.length; i++) {
			SideBarOpenListener l = (SideBarOpenListener) list[i];
			l.sideBarEntryOpen(this);
		}
	}

	public void addListener(SideBarDropListener l) {
		if (listDropListeners == Collections.EMPTY_LIST) {
			listDropListeners = new ArrayList(1);
		}
		listDropListeners.add(l);
	}
	
	public void removeListener(SideBarDropListener l) {
		listDropListeners.remove(l);
	}
	
	protected boolean hasDropListeners() {
		return listDropListeners != null && listDropListeners.size() > 0;
	}

	protected void triggerDropListeners(Object o) {
		Object[] list = listDropListeners.toArray();
		for (int i = 0; i < list.length; i++) {
			SideBarDropListener l = (SideBarDropListener) list[i];
			l.sideBarEntryDrop(this, o);
		}
	}

	public String getLogID() {
		return logID;
	}

	public void setLogID(String logID) {
		if (logID == null || logID.equals("" + this.logID)) {
			return;
		}
		String oldID = this.logID;
		this.logID = logID;
		triggerLogIDListeners(oldID);
	}

	public SideBar getSidebar() {
		return sidebar;
	}

	/**
	 * @return
	 */
	public boolean isInTree() {
		return treeItem != null && !treeItem.isDisposed();
	}

	public IconBarEnabler getIconBarEnabler() {
		return iconBarEnabler;
	}

	public void setIconBarEnabler(IconBarEnabler iconBarEnabler) {
		this.iconBarEnabler = iconBarEnabler;
	}
}
