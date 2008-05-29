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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Control;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.util.MapUtils;

/**
 * @author TuxPaper
 * @created Sep 30, 2006
 *
 */
public class SearchResultsTabArea
	extends SkinView
{
	public static boolean PULL_TABS = false;

	private SWTSkinObjectBrowser browserSkinObject;

	private SWTSkin skin;

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.views.SkinView#showSupport(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	 */
	public Object showSupport(SWTSkinObject skinObject, Object params) {
		skin = skinObject.getSkin();
		browserSkinObject = (SWTSkinObjectBrowser) skin.getSkinObject(SkinConstants.VIEWID_BROWSER_SEARCHRESULTS);

		Browser browser = browserSkinObject.getBrowser();

		createBrowseArea(browserSkinObject);
		return null;
	}

	/**
	 * 
	 */
	private void createBrowseArea(SWTSkinObjectBrowser browserSkinObject) {
		this.browserSkinObject = browserSkinObject;
	}


	public void restart() {
		if (browserSkinObject != null) {
			browserSkinObject.restart();
		}
	}

	public static void openSearchResults(final Map params) {
  	SearchResultsTabArea view = ensureSearchTab();
  	if (view != null) {
  		view._openSearchResults(params);
  	}
	}

	private void _openSearchResults(final Map params) {
		Utils.execSWTThread(new AERunnable() {

			public void runSupport() {
				SWTSkinObject soSearchResults = skin.getSkinObject("searchresults-search-results");
				if (soSearchResults == null) {
					return;
				}

				Control browser = browserSkinObject.getControl();
				Browser search = (Browser) soSearchResults.getControl();
				search.setUrl(MapUtils.getMapString(params, "url",
						"http://google.com/?q=" + Math.random()));

				FormData gd = (FormData) search.getLayoutData();
				gd.top = new FormAttachment(browser, 0);
				gd.height = SWT.DEFAULT;
				search.setLayoutData(gd);
				soSearchResults.setVisible(true);

				gd = (FormData) browser.getLayoutData();
				gd.bottom = null;
				gd.height = MapUtils.getMapInt(params, "top-height", 120);
				browser.setLayoutData(gd);

				search.getParent().layout(true);
			}
		});
	}

	public static void closeSearchResults(final Map params) {
  	SearchResultsTabArea view = ensureSearchTab();
  	if (view != null) {
  		view._closeSearchResults(params);
  	}
	}
	
	private static SearchResultsTabArea ensureSearchTab() {
		SearchResultsTabArea view = (SearchResultsTabArea) SkinViewManager.get(SearchResultsTabArea.class);
		if (view == null) {
			// not avail yet!
			SWTSkin skin = SWTSkinFactory.getInstance();
			SWTSkinTabSet tabSetMain = skin.getTabSet(SkinConstants.TABSET_MAIN);
			if (tabSetMain != null) {
				SWTSkinObjectTab tab = tabSetMain.getTab(SkinConstants.VIEWID_SEARCHRESULTS_TAB);
				if (tab != null) {
					tabSetMain.setActiveTab(tab);
					view = (SearchResultsTabArea) SkinViewManager.get(SearchResultsTabArea.class);
					if (view == null) {
						return null;
					}

					SWTSkinObject[] children = tab.getChildren();
					for (int i = 0; i < children.length; i++) {
						SWTSkinObject child = children[i];
						
						if (child instanceof SWTSkinObjectText) {
							SWTSkinObjectText soTxt = (SWTSkinObjectText) child;
							soTxt.setText("Last\nSearch");
						}
					}
					tabSetMain.setActiveTab(tab);
				}
			}
		}
		return view;
	}
		
	private void _closeSearchResults(final Map params) {
		Utils.execSWTThread(new AERunnable() {

			public void runSupport() {
				SWTSkinObject soSearchResults = skin.getSkinObject("searchresults-search-results");
				if (soSearchResults == null) {
					return;
				}

				Control browser = browserSkinObject.getControl();
				Browser search = (Browser) soSearchResults.getControl();

				FormData gd = (FormData) search.getLayoutData();
				gd.top = null;
				gd.height = 0;
				search.setLayoutData(gd);
				soSearchResults.setVisible(false);

				gd = (FormData) browser.getLayoutData();
				gd.bottom = new FormAttachment(search, 0);
				gd.height = SWT.DEFAULT;
				browser.setLayoutData(gd);

				search.getParent().layout(true);
				search.setUrl("about:blank");
			}
		});
	}
}
