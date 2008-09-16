/**
 * Created on Sep 15, 2008
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

import org.eclipse.swt.graphics.Rectangle;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Utils;

import org.gudy.azureus2.plugins.ui.sidebar.SideBarEntry;
import org.gudy.azureus2.plugins.ui.sidebar.SideBarVitalityImage;
import org.gudy.azureus2.plugins.ui.sidebar.SideBarVitalityImageListener;

/**
 * @author TuxPaper
 * @created Sep 15, 2008
 *
 */
public class SideBarVitalityImageSWT
	implements SideBarVitalityImage
{
	private final String imageID;

	private final SideBarEntrySWT sideBarEntry;

	private List listeners = Collections.EMPTY_LIST;

	private String tooltip;

	private Rectangle hitArea;
	
	private boolean visible = true;

	/**
	 * @param imageID
	 */
	public SideBarVitalityImageSWT(SideBarEntrySWT entry, String imageID) {
		this.sideBarEntry = entry;
		this.imageID = imageID;
	}

	// @see org.gudy.azureus2.plugins.ui.sidebar.SideBarVitalityImage#getImageID()
	public String getImageID() {
		return imageID;
	}

	/**
	 * @return the sideBarEntry
	 */
	public SideBarEntry getSideBarEntry() {
		return sideBarEntry;
	}

	// @see org.gudy.azureus2.plugins.ui.sidebar.SideBarVitalityImage#addListener(org.gudy.azureus2.plugins.ui.sidebar.SideBarVitalityImageListener)
	public void addListener(SideBarVitalityImageListener l) {
		if (listeners == Collections.EMPTY_LIST) {
			listeners = new ArrayList(1);
		}
		listeners.add(l);
	}

	// @see org.gudy.azureus2.plugins.ui.sidebar.SideBarVitalityImage#setTooltip(java.lang.String)
	public void setToolTip(String tooltip) {
		this.tooltip = tooltip;
	}
	
	public String getToolTip() {
		return tooltip;
	}

	/**
	 * @param bounds
	 *
	 * @since 3.1.1.1
	 */
	public void setHitArea(Rectangle hitArea) {
		this.hitArea = hitArea;
	}

	public Rectangle getHitArea() {
		return hitArea;
	}

	// @see org.gudy.azureus2.plugins.ui.sidebar.SideBarVitalityImage#getVisible()
	public boolean isVisible() {
		return visible;
	}

	// @see org.gudy.azureus2.plugins.ui.sidebar.SideBarVitalityImage#setVisible(boolean)
	public void setVisible(boolean visible) {
		if (this.visible == visible) {
			return;
		}
		this.visible = visible;
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (sideBarEntry != null) {
					sideBarEntry.redraw();
				}
			}
		});
	}
}
