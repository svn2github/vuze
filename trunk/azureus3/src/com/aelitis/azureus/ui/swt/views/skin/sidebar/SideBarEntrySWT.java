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
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.views.IView;

import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.utils.ImageLoader;
import com.aelitis.azureus.ui.swt.utils.ImageLoaderFactory;

import org.gudy.azureus2.plugins.ui.sidebar.SideBarEntry;
import org.gudy.azureus2.plugins.ui.sidebar.SideBarVitalityImage;

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
		
		if (treeItem != null && !treeItem.isDisposed()) {
  		String newText = (String) titleInfo.getTitleInfoProperty(ViewTitleInfo.TITLE_TEXT);
  		if (newText != null) {
  			pullTitleFromIView = false;
  			treeItem.setData("text", newText);
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

	public SideBarEntrySWT(String id) {
		this.id = id;
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
	}
	
	public Image getImageLeft(String suffix) {
		if (imageLeftID == null) {
			return null;
		}
		Image img = null;
		if (suffix == null) {
			img = ImageLoaderFactory.getInstance().getImage(imageLeftID);
		} else {
			img = ImageLoaderFactory.getInstance().getImage(imageLeftID + suffix);
		}
		if (ImageLoader.isRealImage(img)) {
			return img;
		}
		return null;
	}
	
	public void addListener(SideBarCloseListener l) {
		if (listCloseListeners == Collections.EMPTY_LIST) {
			listCloseListeners = new ArrayList(1);
		}
		listCloseListeners.add(l);
	}
	
	protected void triggerCloseListeners() {
		Object[] list = listCloseListeners.toArray();
		for (int i = 0; i < list.length; i++) {
			SideBarCloseListener l = (SideBarCloseListener) list[i];
			l.sidebarClosed(this);
		}
	}
}
