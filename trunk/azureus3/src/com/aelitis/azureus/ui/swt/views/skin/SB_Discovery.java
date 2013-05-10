/**
 * Created on May 10, 2013
 *
 * Copyright 2011 Vuze, LLC.  All rights reserved.
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

import java.util.ArrayList;

import org.gudy.azureus2.core3.util.Constants;

import com.aelitis.azureus.core.util.FeatureAvailability;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.mdi.*;
import com.aelitis.azureus.ui.swt.mdi.BaseMdiEntry;
import com.aelitis.azureus.util.ConstantsVuze;
import com.aelitis.azureus.util.ContentNetworkUtils;

/**
 * @author TuxPaper
 * @created May 10, 2013
 *
 */
public class SB_Discovery
{
	private ArrayList<MdiEntry> children = new ArrayList<MdiEntry>();

	private ViewTitleInfo titleInfo;

	public SB_Discovery(MultipleDocumentInterface mdi) {
		setup(mdi);
	}

	private void setup(final MultipleDocumentInterface mdi) {
		MdiEntry entry = mdi.createEntryFromSkinRef(
				MultipleDocumentInterface.SIDEBAR_HEADER_DISCOVERY,
				ContentNetworkUtils.getTarget(ConstantsVuze.getDefaultContentNetwork()),
				"main.area.browsetab", "{sidebar.VuzeHDNetwork}",
				null, null, false, null);
		entry.setImageLeftID("image.sidebar.vuze");

		if (Constants.isWindows && FeatureAvailability.isGamesEnabled()) {
			mdi.registerEntry(MultipleDocumentInterface.SIDEBAR_SECTION_GAMES,
					new MdiEntryCreationListener() {
						public MdiEntry createMDiEntry(String id) {
							MdiEntry entry = mdi.createEntryFromSkinRef(
									MultipleDocumentInterface.SIDEBAR_HEADER_DISCOVERY,
									MultipleDocumentInterface.SIDEBAR_SECTION_GAMES,
									"main.generic.browse",
									"{mdi.entry.games}", null, null, true,
									null);
							((BaseMdiEntry) entry).setPreferredAfterID(ContentNetworkUtils.getTarget(ConstantsVuze.getDefaultContentNetwork()));
							String url = ConstantsVuze.getDefaultContentNetwork().getSiteRelativeURL(
									"starts/games.start", false);
							entry.setDatasource(url);
							entry.setImageLeftID("image.sidebar.games");
							return entry;
						}
					});
			mdi.loadEntryByID(MultipleDocumentInterface.SIDEBAR_SECTION_GAMES, false,
					true, null);
		}


		mdi.addListener(new MdiEntryLoadedListener() {
			public void mdiEntryLoaded(MdiEntry entry) {
				if (MultipleDocumentInterface.SIDEBAR_HEADER_DISCOVERY.equals(entry.getParentID())) {
					children.add(entry);
					entry.addListener(new MdiChildCloseListener() {
						public void mdiChildEntryClosed(MdiEntry parent, MdiEntry child,
								boolean user) {
							children.remove(child);
						}
					});
				}
				if (!entry.getId().equals(MultipleDocumentInterface.SIDEBAR_HEADER_DISCOVERY)) {
					return;
				}
				setupHeader(entry);
			}
		});
	}

	private void setupHeader(final MdiEntry entry) {

		titleInfo = new ViewTitleInfo() {
			public Object getTitleInfoProperty(int propertyID) {
				if (propertyID == ViewTitleInfo.TITLE_INDICATOR_TEXT) {
					if (entry.isExpanded()) {
						return null;
					}
					StringBuilder sb = new StringBuilder();
					MdiEntry[] entries = entry.getMDI().getEntries();
					for (MdiEntry subEntry : entries) {
						System.out.println(subEntry.getId());
						if (subEntry.getId().startsWith("Subscription_")) {
							continue;
						}
						if (entry.getId().equals(subEntry.getParentID())) {
							ViewTitleInfo titleInfo = subEntry.getViewTitleInfo();
							if (titleInfo != null) {
								Object text = titleInfo.getTitleInfoProperty(TITLE_INDICATOR_TEXT);
								if (text instanceof String) {
									if (sb.length() > 0) {
										sb.append(" | ");
									}
									sb.append(text);
								}
							}
						}
					}
					if (sb.length() > 0) {
						return sb.toString();
					}
				} else if (propertyID == ViewTitleInfo.TITLE_INDICATOR_TEXT_TOOLTIP) {
					if (entry.isExpanded()) {
						return null;
					}
					StringBuilder sb = new StringBuilder();
					MdiEntry[] entries = entry.getMDI().getEntries();
					for (MdiEntry subEntry : entries) {
						if (entry.getId().equals(subEntry.getParentID())) {
							ViewTitleInfo titleInfo = subEntry.getViewTitleInfo();
							if (titleInfo != null) {
								Object text = titleInfo.getTitleInfoProperty(TITLE_INDICATOR_TEXT);
								if (text instanceof String) {
									if (sb.length() > 0) {
										sb.append("\n");
									}
									sb.append(subEntry.getTitle() + ": " + text);
								}
							}
						}
					}
					if (sb.length() > 0) {
						return sb.toString();
					}
				}
				return null;
			}
		};
		entry.setViewTitleInfo(titleInfo);
	}

}
