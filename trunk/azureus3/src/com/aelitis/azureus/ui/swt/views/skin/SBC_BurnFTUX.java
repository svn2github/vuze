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

import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;

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
public class SBC_BurnFTUX
	extends SkinView
{
	private SWTSkinObjectBrowser browserSkinObject;

	private String url;

	private static String sRef;

	private String entryID;

	private MdiEntry entry;

	private static boolean DEBUG = Constants.IS_CVS_VERSION;

	public Object skinObjectInitialShow(final SWTSkinObject skinObject,
			Object params) {

		MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
		entry = mdi.getCurrentEntry();
		
		entryID = entry.getId();

		browserSkinObject = (SWTSkinObjectBrowser) skin.getSkinObject("browser",
				soMain);

		browserSkinObject.addListener(new loadingListener() {

			public void browserLoadingChanged(boolean loading, String url) {
				if (!loading) {
					skinObject.getControl().getParent().layout(true, true);
				}
			}
		});

		sRef = "user";
		if (DEBUG) {
			System.out.println("BurnFTUX sourceRef is now " + sRef);
		}

		return null;
	}
	
	public Object skinObjectShown(SWTSkinObject skinObject, Object params) {
		super.skinObjectShown(skinObject, params);
		buildURL();
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
		buildURL();
	}

	private void buildURL() {
		boolean isFull = FeatureManagerUI.hasFullLicence();
		boolean isTrial = FeatureManagerUI.hasFullBurn() && !isFull;
		url = "http://www2.vuze.com/client/plus/burn.php?view=" + entryID + "&mode="
				+ (isFull ? "plus" : isTrial ? "trial" : "free") + "&sourceRef=" + sRef;
		if (DEBUG) {
			System.out.println("URL is now " + url + " via " + Debug.getCompressedStackTrace());
		}

		MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
		MdiEntry currentEntry = mdi.getCurrentEntry();

		if (browserSkinObject != null && entry == currentEntry) {
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
				((SBC_BurnFTUX) bview).buildURL();
			}
		}
	}
}
