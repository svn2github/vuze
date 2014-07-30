/**
 * Created on Jul 22, 2008
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

package com.aelitis.azureus.ui.swt.toolbar;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarActivationListener;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarManager;

import com.aelitis.azureus.ui.common.ToolBarItem;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectText;
import com.aelitis.azureus.ui.swt.views.skin.ToolBarView;

/**
 * @author TuxPaper
 * @created Jul 22, 2008
 *
 */
public class ToolBarItemSO
	implements ToolBarItem
{
	String imageID = "image.toolbar.run";

	String id;

	private SWTSkinButtonUtility skinButton;

	private SWTSkinObjectText skinTitle;

	boolean enabled = false;

	private String textID;

	private String tooltipID;

	private boolean alwaysAvailable = false;

	private final ToolBarView tbView;

	private UIToolBarActivationListener defaultActivation;

	private final boolean isPluginItem;

	private String groupID = UIToolBarManager.GROUP_MAIN;

	private long defaultState;

	private boolean isDown;

	/**
	 * @param id
	 * @param image
	 */
	public ToolBarItemSO(ToolBarView tbView, String id, String imageid) {
		super();
		this.tbView = tbView;
		this.id = id;
		imageID = imageid;
		isPluginItem = false;
	}

	public ToolBarItemSO(ToolBarView tbView, String id, String imageid,
			String textID) {
		super();
		this.tbView = tbView;
		this.id = id;
		imageID = imageid;
		this.textID = textID;
		this.tooltipID = textID + ".tooltip";
		isPluginItem = false;
	}

	public ToolBarItemSO(ToolBarView tbView, String id, boolean isPluginItem) {
		this.tbView = tbView;
		this.id = id;
		this.isPluginItem = isPluginItem;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.common.ToolBarItem#triggerToolBarItem(long, java.lang.Object)
	 */
	public boolean triggerToolBarItem(long activationType, Object datasource) {
		return tbView.triggerToolBarItem(this, activationType, datasource);
	}

	public String getID() {
		return id;
	}

	public void setSkinButton(SWTSkinButtonUtility btn) {
		this.skinButton = btn;
		skinButton.setDisabled(!enabled);
	}

	public SWTSkinButtonUtility getSkinButton() {
		return skinButton;
	}

	public void setSkinTitle(SWTSkinObjectText s) {
		skinTitle = s;
	}

	public long getState() {
		long state = (isEnabled() ? STATE_ENABLED : 0) | (isDown ? STATE_DOWN : 0);

		return state;
	}

	public void setState(long state) {
		// TODO: This gets called a lot for the same toolbar item -- need to look
		// into why.
		setEnabled((state & STATE_ENABLED) > 0);
		isDown = (state & STATE_DOWN) > 0;
		if (skinButton != null) {
			skinButton.getSkinObject().switchSuffix(isDown ? "-selected" : "", 4, false);
		}
	}

	private boolean isEnabled() {
		if (skinButton != null) {
			return !skinButton.isDisabled();
		}
		return enabled;
	}

	private void setEnabled(boolean enabled) {
		if (alwaysAvailable && !enabled) {
			return;
		}
		this.enabled = enabled;
		if (skinButton != null) {
			skinButton.setDisabled(!enabled);
		}
	}

	public String getImageID() {
		return imageID;
	}

	public void setImageID(String imageID) {
		this.imageID = imageID;
		if (skinButton != null) {
			skinButton.setImage(imageID);
		}
	}

	/**
	 * @param textID the textID to set
	 */
	public void setTextID(String textID) {
		this.textID = textID;
		if (skinTitle != null) {
			skinTitle.setTextID(textID);
		}
	}

	/**
	 * @return the textID
	 */
	public String getTextID() {
		return textID;
	}

	public String getTooltipID() {
		return tooltipID;
	}

	public void setTooltipID(String tooltipID) {
		this.tooltipID = tooltipID;
	}

	public void setAlwaysAvailable(boolean alwaysAvailable) {
		this.alwaysAvailable = alwaysAvailable;
		if (alwaysAvailable) {
			setEnabled(true);
		}
	}

	public boolean isAlwaysAvailable() {
		return alwaysAvailable;
	}

	public void setDefaultActivationListener(
			UIToolBarActivationListener defaultActivation) {
		this.defaultActivation = defaultActivation;
	}

	public UIToolBarActivationListener getDefaultActivationListener() {
		return defaultActivation;
	}

	public void dispose() {
		// ToolBarView will dispose of skinobjects
		skinButton = null;
		skinTitle = null;
	}

	public String getGroupID() {
		return groupID;
	}

	public void setGroupID(String groupID) {
		this.groupID = groupID;
	}

	public void setDefaultState(long state) {
		this.defaultState = state;
	}

	public long getDefaultState() {
		return defaultState;
	}
}
