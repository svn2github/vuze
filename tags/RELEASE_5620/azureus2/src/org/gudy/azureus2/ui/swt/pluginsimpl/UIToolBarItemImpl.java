/**
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

package org.gudy.azureus2.ui.swt.pluginsimpl;

import java.util.ArrayList;
import java.util.List;

import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarActivationListener;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarManager;

import com.aelitis.azureus.ui.common.ToolBarItem;

/**
 * 
 * A Toolbar item implementation, independent of UI (SWT)
 * 
 * @author TuxPaper
 * @created Feb 19, 2015
 *
 */
public class UIToolBarItemImpl
	implements ToolBarItem
{
	private String id;

	private String imageID = "image.toolbar.run";

	private String textID;

	private boolean alwaysAvailable = false;

	private long state;

	private UIToolBarActivationListener defaultActivation;

	private String tooltipID;

	private String groupID = UIToolBarManager.GROUP_MAIN;

	private List<ToolBarItemListener> toolBarItemListeners = new ArrayList<ToolBarItemListener>();

	private String toolTip;

	public UIToolBarItemImpl(String id) {
		this.id = id;
	}

	// @see com.aelitis.azureus.ui.common.ToolBarItem#addToolBarItemListener(com.aelitis.azureus.ui.common.ToolBarItem.ToolBarItemListener)
	public void addToolBarItemListener(ToolBarItemListener l) {
		if (!toolBarItemListeners.contains(l)) {
			toolBarItemListeners.add(l);
		}
	}
	
	// @see com.aelitis.azureus.ui.common.ToolBarItem#removeToolBarItemListener(com.aelitis.azureus.ui.common.ToolBarItem.ToolBarItemListener)
	public void removeToolBarItemListener(ToolBarItemListener l) {
		toolBarItemListeners.remove(l);
	}

	private void triggerFieldChange() {
		ToolBarItemListener[] array = toolBarItemListeners.toArray(new ToolBarItemListener[0]);
		for (ToolBarItemListener l : array) {
			l.uiFieldChanged(this);
		}
	}

	// @see org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem#getID()
	public String getID() {
		return id;
	}

	// @see org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem#getTextID()
	public String getTextID() {
		return textID;
	}

	// @see org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem#setTextID(java.lang.String)
	public void setTextID(String id) {
		textID = id;
		triggerFieldChange();
	}

	// @see org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem#getImageID()
	public String getImageID() {
		return imageID;
	}

	// @see org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem#setImageID(java.lang.String)
	public void setImageID(String id) {
		imageID = id;
		triggerFieldChange();
	}

	// @see org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem#isAlwaysAvailable()
	public boolean isAlwaysAvailable() {
		return alwaysAvailable;
	}

	public void setAlwaysAvailable(boolean alwaysAvailable) {
		this.alwaysAvailable = alwaysAvailable;
		triggerFieldChange();
	}

	// @see org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem#getState()
	public long getState() {
		return state;
	}

	// @see org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem#setState(long)
	public void setState(long state) {
		this.state = state;
		triggerFieldChange();
	}

	// @see org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem#triggerToolBarItem(long, java.lang.Object)
	public boolean triggerToolBarItem(long activationType, Object datasource) {
		ToolBarItemListener[] array = toolBarItemListeners.toArray(new ToolBarItemListener[0]);
		for (ToolBarItemListener l : array) {
			if (l.triggerToolBarItem(this, activationType, datasource)) {
				return true;
			}
		}
		return false;
	}

	// @see org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem#setDefaultActivationListener(org.gudy.azureus2.plugins.ui.toolbar.UIToolBarActivationListener)
	public void setDefaultActivationListener(
			UIToolBarActivationListener defaultActivation) {
		this.defaultActivation = defaultActivation;
	}

	// @see com.aelitis.azureus.ui.common.ToolBarItem#getDefaultActivationListener()
	public UIToolBarActivationListener getDefaultActivationListener() {
		return defaultActivation;
	}

	// @see com.aelitis.azureus.ui.common.ToolBarItem#getTooltipID()
	public String getTooltipID() {
		return tooltipID;
	}

	public void setTooltipID(String tooltipID) {
		this.tooltipID = tooltipID;
	}

	// @see org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem#getGroupID()
	public String getGroupID() {
		return groupID;
	}

	// @see org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem#setGroupID(java.lang.String)
	public void setGroupID(String groupID) {
		this.groupID = groupID;
	}

	// @see org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem#setToolTip(java.lang.String)
	public void setToolTip(String text) {
		toolTip = text;
	}

	// @see org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem#getToolTip()
	public String getToolTip() {
		return toolTip;
	}
}
