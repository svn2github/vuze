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

import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility;

/**
 * @author TuxPaper
 * @created Jul 22, 2008
 *
 */
public abstract class ToolBarItem
{
	String imageID;

	String id;

	private SWTSkinButtonUtility skinButton;
	
	boolean enabled = true;
	
	private String textID;

	/**
	 * @param id
	 * @param image
	 */
	public ToolBarItem(String id, String imageid) {
		super();
		this.id = id;
		imageID = imageid;
	}

	public ToolBarItem(String id, String imageid, String textID) {
		super();
		this.id = id;
		imageID = imageid;
		this.textID = textID;
	}

	public abstract void triggerToolBarItem();


	public String getId() {
		return id;
	}

	public void setSkinButton(SWTSkinButtonUtility btn) {
		this.skinButton = btn;
	}

	public SWTSkinButtonUtility getSkinButton() {
		return skinButton;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		if (this.enabled == enabled) {
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
	}

	/**
	 * @param textID the textID to set
	 */
	public void setTextID(String textID) {
		this.textID = textID;
	}

	/**
	 * @return the textID
	 */
	public String getTextID() {
		return textID;
	}
}
