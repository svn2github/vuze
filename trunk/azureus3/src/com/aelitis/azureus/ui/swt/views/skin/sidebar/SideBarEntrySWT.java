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

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.TreeItem;

import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.views.IView;

import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;

import org.gudy.azureus2.plugins.ui.sidebar.SideBarEntry;

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
	
	protected Image imageLeft;

	public boolean disableCollapse;
	
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
}
