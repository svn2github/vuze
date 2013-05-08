/**
 * Created on Sep 13, 2010
 *
 * Copyright 2010 Vuze, Inc.  All rights reserved.
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
 
package com.aelitis.azureus.ui.swt.views.skin;

import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.ui.mdi.MdiEntry;
import com.aelitis.azureus.ui.mdi.MdiEntryVitalityImage;
import com.aelitis.azureus.ui.mdi.MdiListener;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.browser.BrowserContext;
import com.aelitis.azureus.ui.swt.mdi.MultipleDocumentInterfaceSWT;
import com.aelitis.azureus.ui.swt.skin.*;

/**
 * @author TuxPaper
 * @created Sep 13, 2010
 *
 */
public class SBC_GenericBrowsePage
extends SkinView
{
	private SWTSkinObjectBrowser browserSkinObject;
	private MdiEntryVitalityImage vitalityImage;
	private MdiEntry entry;

	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		Object creationParams = skinObject.getData("CreationParams");

		browserSkinObject = SWTSkinUtils.findBrowserSO(soMain);

		final MultipleDocumentInterfaceSWT mdi = UIFunctionsManagerSWT.getUIFunctionsSWT().getMDISWT();
		if (mdi != null) {
			entry = mdi.getEntryBySkinView(this);
			if (entry != null) {
				vitalityImage = entry.addVitalityImage("image.sidebar.vitality.dots");
				vitalityImage.setVisible(false);

				mdi.addListener(new MdiListener() {
					long lastSelect = 0;

					public void mdiEntrySelected(MdiEntry newEntry,
							MdiEntry oldEntry) {
						if (entry == newEntry) {
							if (entry == oldEntry) {
								if (lastSelect < SystemTime.getOffsetTime(-1000)) {
									if (browserSkinObject != null) {
										browserSkinObject.restart();
									}
								}
							} else {
								lastSelect = SystemTime.getCurrentTime();
							}
						}
					}
				});
			}
		}

		browserSkinObject.addListener(new SWTSkinObjectListener() {
		
			public Object eventOccured(SWTSkinObject skinObject, int eventType,
					Object params) {
				if (eventType == EVENT_SHOW) {
					browserSkinObject.removeListener(this);

					browserSkinObject.addListener(new BrowserContext.loadingListener() {
						public void browserLoadingChanged(boolean loading, String url) {
							if (vitalityImage != null) {
								vitalityImage.setVisible(loading);
							}
						}
					});
				}
				return null;
			}
		});

		openURL();

		return null;
	}

	private void openURL() {
		
		Object o = entry.getDatasource();
		if (o instanceof String) {
			browserSkinObject.setURL((String) o);
		}
	}
}
