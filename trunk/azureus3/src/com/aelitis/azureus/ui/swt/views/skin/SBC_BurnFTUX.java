/**
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
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
 */

package com.aelitis.azureus.ui.swt.views.skin;

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.mdi.MdiEntry;
import com.aelitis.azureus.ui.mdi.MultipleDocumentInterface;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.browser.BrowserContext.loadingListener;
import com.aelitis.azureus.ui.swt.feature.FeatureManagerUI;
import com.aelitis.azureus.ui.swt.mdi.MultipleDocumentInterfaceSWT;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectBrowser;
import com.aelitis.azureus.util.ConstantsVuze;

/**
 * @author TuxPaper
 * @created Oct 1, 2006
 *
 */
public class SBC_BurnFTUX
	extends SkinView
{
	private SWTSkinObjectBrowser browserSkinObject;

	private String url;

	private static String sRef = "user";

	private String entryID;

	private MdiEntry entry;

	private static boolean DEBUG = Constants.IS_CVS_VERSION;

	public Object skinObjectInitialShow(final SWTSkinObject skinObject,
			Object params) {

		MultipleDocumentInterfaceSWT mdi = UIFunctionsManagerSWT.getUIFunctionsSWT().getMDISWT();
		if (mdi != null) {
			entry = mdi.getEntryFromSkinObject(skinObject);
			if (entry != null) {
				entryID = entry.getId();
			}
		}

		browserSkinObject = (SWTSkinObjectBrowser) skin.getSkinObject("browser",
				soMain);

		browserSkinObject.addListener(new loadingListener() {

			public void browserLoadingChanged(boolean loading, String url) {
				if (!loading) {
					skinObject.getControl().getParent().layout(true, true);
				}
			}
		});

		if (DEBUG) {
			System.out.println("BurnFTUX sourceRef is now " + sRef);
		}

		return null;
	}
	
	public Object skinObjectShown(SWTSkinObject skinObject, Object params) {
		super.skinObjectShown(skinObject, params);
		buildURL(true);
		return null;
	}
	
	public Object skinObjectHidden(SWTSkinObject skinObject, Object params) {
		if (browserSkinObject != null) {
			browserSkinObject.setURL("about:blank");
		}
		sRef = "user";
		if (DEBUG) {
			System.out.println("BurnFTUX sourceRef is now " + sRef);
		}
		return super.skinObjectHidden(skinObject, params);
	}

	/**
	 * @param hasFullLicence
	 */
	public void updateLicenceInfo() {
		buildURL(false);
	}

	private void buildURL(boolean forceSet) {
		String suffix = ("?view=" + entryID)
				+ ("&sourceRef=" + UrlUtils.encode(sRef + "-/plus/ftux/dvd"));
		String newUrl = ConstantsVuze.getDefaultContentNetwork().getSiteRelativeURL(
				"burn_ftux.start" + suffix, false);
		newUrl = FeatureManagerUI.appendFeatureManagerURLParams(newUrl);
		if (!forceSet && newUrl.equals(url)) {
			return;
		}
		
		url = newUrl;

		if (DEBUG) {
			System.out.println("URL is now " + url + " via " + Debug.getCompressedStackTrace());
		}

		MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
		MdiEntry currentEntry = mdi.getCurrentEntry();

		if (browserSkinObject != null && (forceSet || entry == currentEntry)) {
			browserSkinObject.setURL(url);
		}
	}

	public static void setSourceRef(String _sRef) {
		sRef = _sRef;
		if (DEBUG) {
			System.out.println("BurnFTUX sourceRef is now " + sRef);
		}

		SkinView[] views = SkinViewManager.getMultiByClass(SBC_BurnFTUX.class);
		if (views != null) {
			for (SkinView bview : views) {
				((SBC_BurnFTUX) bview).buildURL(false);
			}
		}
	}
}
