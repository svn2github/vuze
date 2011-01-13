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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectText;

/**
 * @author TuxPaper
 * @created Jul 22, 2008
 *
 */
public class ToolBarItem
{
	String imageID;

	String id;

	private SWTSkinButtonUtility 	skinButton;
	private SWTSkinObjectText		skinTitle;
	
	boolean enabled = true;
	
	private String textID;
	
	private String tooltipID;
	
	private List listeners = Collections.EMPTY_LIST;
	
	private boolean alwaysAvailable = false;

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
		this.tooltipID = textID + ".tooltip";
	}

	public void triggerToolBarItem() {
		Object[] array = listeners.toArray();
		for (int i = 0; i < array.length; i++) {
			ToolBarItemListener l = (ToolBarItemListener) array[i];
			l.pressed(this);
		}
	}


	public String getId() {
		return id;
	}

	public void setSkinButton(SWTSkinButtonUtility btn) {
		this.skinButton = btn;
	}

	public SWTSkinButtonUtility getSkinButton() {
		return skinButton;
	}

	public void setSkinTitle( SWTSkinObjectText s ){
		skinTitle	= s;
	}
	
	public boolean isEnabled() {
		if (skinButton != null) {
			return !skinButton.isDisabled();
		}
		return enabled;
	}

	public void setEnabled(boolean enabled) {
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

	/**
	 * @return
	 *
	 * @since 3.1.1.1
	 */
	public boolean triggerToolBarItemHold() {
		Object[] array = listeners.toArray();
		for (int i = 0; i < array.length; i++) {
			ToolBarItemListener l = (ToolBarItemListener) array[i];
			if (l.held(this)) {
				return true;
			}
		}
		return false;
	}

	public String getTooltipID() {
		return tooltipID;
	}

	public void setTooltipID(String tooltipID) {
		this.tooltipID = tooltipID;
	}
	
	public void addListener(ToolBarItemListener l) {
		synchronized (ToolBarItem.class) {
  		if (listeners == Collections.EMPTY_LIST) {
  			listeners = new ArrayList(1);
  		}
  		listeners.add(l);
		}
	}
	
	public void removeListener(ToolBarItemListener l) {
		synchronized (ToolBarItem.class) {
  		listeners.remove(l);
		}
	}

	public void setAlwaysAvailable(boolean alwaysAvailable) {
		this.alwaysAvailable = alwaysAvailable;
	}

	public boolean isAlwaysAvailable() {
		return alwaysAvailable;
	}
}
