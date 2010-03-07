/**
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.ui.swt.views.skin;

import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.mdi.MdiEntry;
import com.aelitis.azureus.ui.mdi.MultipleDocumentInterface;
import com.aelitis.azureus.ui.swt.browser.BrowserContext.loadingListener;
import com.aelitis.azureus.ui.swt.feature.FeatureManagerUI;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectBrowser;


/**
 * @author TuxPaper
 * @created Oct 1, 2006
 *
 */
public class PlusFTUXView
	extends SkinView
{
	private SWTSkinObjectBrowser browserSkinObject;
	private boolean hasFullLicence;
	private String url;

	public Object skinObjectInitialShow(final SWTSkinObject skinObject,
			Object params) {
		
		MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
		MdiEntry entry = mdi.getEntry(MultipleDocumentInterface.SIDEBAR_SECTION_PLUS);
		
		browserSkinObject = (SWTSkinObjectBrowser) skin.getSkinObject(
				"plus-ftux", soMain);

		browserSkinObject.addListener(new loadingListener() {

			public void browserLoadingChanged(boolean loading, String url) {
				if (!loading) {
					skinObject.getControl().getParent().layout(true, true);
				}
			}
		});

		boolean b = FeatureManagerUI.hasFullLicence();
		hasFullLicence = !b; // so we get URL change
		setHasFullLicence(b);
		
		return null;
	}

	/**
	 * @param hasFullLicence
	 */
	public void setHasFullLicence(boolean hasFullLicence) {
		if (this.hasFullLicence == hasFullLicence) {
			return;
		}
		this.hasFullLicence = hasFullLicence;
		url = "http://www2.vuze.com/plus-ftux.start?full=" + hasFullLicence;
		if (browserSkinObject != null) {
			browserSkinObject.setURL(url);
		}
	}
}
