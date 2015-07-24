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

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Constants;

import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.mdi.*;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.browser.BrowserContext.loadingListener;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectBrowser;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.util.ConstantsVuze;
import com.aelitis.azureus.util.ContentNetworkUtils;


/**
 * @author TuxPaper
 * @created Oct 1, 2006
 *
 */
public class WelcomeView
	extends SkinView
{
	private static boolean waitLoadingURL = true;
	
	private static WelcomeView instance;

	private SWTSkinObjectBrowser browserSkinObject;

	private SWTSkinObject skinObject;

	public Object skinObjectDestroyed(SWTSkinObject skinObject, Object params) {
		instance = null;
		return super.skinObjectDestroyed(skinObject, params);
	}

	public Object skinObjectInitialShow(final SWTSkinObject skinObject,
			Object params) {
		
		this.skinObject = skinObject;
		instance = this;
		browserSkinObject = (SWTSkinObjectBrowser) skin.getSkinObject(
				SkinConstants.VIEWID_BROWSER_WELCOME, soMain);

		browserSkinObject.addListener(new loadingListener() {

			public void browserLoadingChanged(boolean loading, String url) {
				if (!loading) {
					skinObject.getControl().getParent().layout(true, true);
				}
			}
		});

		COConfigurationManager.setParameter("v3.Show Welcome", false);
		
		openURL();

		MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
		
		if ( mdi != null ){
			
			MdiEntry entry = mdi.getEntry(SideBar.SIDEBAR_SECTION_WELCOME);
			
			if ( entry != null ){
				
				entry.addListener(new MdiCloseListener() {
					public void mdiEntryClosed(MdiEntry entry, boolean userClosed) {
						MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
						if (mdi != null) {
							mdi.showEntryByID(SideBar.SIDEBAR_SECTION_LIBRARY);
						}
					}
				});
			}
		}
		
		return null;
	}

	private void openURL() {
		if (waitLoadingURL) {
			return;
		}
		String sURL;
		Object o = skinObject.getData("CreationParams");
		if (o instanceof String) {
			sURL = (String)o;
		} else {
			sURL = ContentNetworkUtils.getUrl( ConstantsVuze.getDefaultContentNetwork(), ContentNetwork.SERVICE_WELCOME);
		}
		
		browserSkinObject.enablePluginProxy( "welcome" );
		
		browserSkinObject.setURL(sURL);
	}

	public static void setWaitLoadingURL(boolean waitLoadingURL) {
		WelcomeView.waitLoadingURL = waitLoadingURL;
		if (!waitLoadingURL && instance != null) {
			instance.openURL();
		}
	}

	public static void setupSidebarEntry(final MultipleDocumentInterface mdi) {
		mdi.registerEntry(MultipleDocumentInterface.SIDEBAR_SECTION_WELCOME,
				new MdiEntryCreationListener() {
					public MdiEntry createMDiEntry(String id) {
						MdiEntry entry = mdi.createEntryFromSkinRef(
								MultipleDocumentInterface.SIDEBAR_HEADER_VUZE,
								MultipleDocumentInterface.SIDEBAR_SECTION_WELCOME,
								"main.area.welcome",
								MessageText.getString(
										"v3.MainWindow.menu.getting_started").replaceAll("&", ""),
								null, null, true, "");
						entry.setImageLeftID("image.sidebar.welcome");
						addDropTest(entry);
						return entry;
					}
				});
	}

	private static void addDropTest(MdiEntry entry) {
		if (!Constants.isCVSVersion()) {
			return;
		}
		entry.addListener(new MdiEntryDropListener() {
			public boolean mdiEntryDrop(MdiEntry entry, Object droppedObject) {
				String s = "You just dropped " + droppedObject.getClass() + "\n"
						+ droppedObject + "\n\n";
				if (droppedObject.getClass().isArray()) {
					Object[] o = (Object[]) droppedObject;
					for (int i = 0; i < o.length; i++) {
						s += "" + i + ":  ";
						Object object = o[i];
						if (object == null) {
							s += "null";
						} else {
							s += object.getClass() + ";" + object;
						}
						s += "\n";
					}
				}
				UIFunctionsManager.getUIFunctions().promptUser("test", s, null, 0, null,
						null, false, 0, null);
				return true;
			}
		});
	}
}
