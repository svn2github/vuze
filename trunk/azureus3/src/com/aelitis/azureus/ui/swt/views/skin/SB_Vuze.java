/*
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.aelitis.azureus.ui.swt.views.skin;

import java.util.ArrayList;

import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoListener;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.mdi.*;
import com.aelitis.azureus.ui.swt.views.ViewTitleInfoBetaP;

public class SB_Vuze
{
	private ArrayList<MdiEntry> children = new ArrayList<MdiEntry>(4);

	private ViewTitleInfo titleInfo;

	public SB_Vuze(MultipleDocumentInterface mdi) {
		setup(mdi);
	}

	private void setup(final MultipleDocumentInterface mdi) {

		ViewTitleInfoBetaP.setupSidebarEntry(mdi);

		WelcomeView.setupSidebarEntry(mdi);

		SBC_ActivityTableView.setupSidebarEntry(mdi);

		// Refresh the Vuze header when one of the children's title properties change
		ViewTitleInfoManager.addListener(new ViewTitleInfoListener() {
			public void viewTitleInfoRefresh(ViewTitleInfo titleInfo) {
				if (SB_Vuze.this.titleInfo == null) {
					return;
				}
				MdiEntry childrenArray[] = children.toArray(new MdiEntry[0]);
				for (MdiEntry entry : childrenArray) {
					if (entry.getViewTitleInfo() == titleInfo) {
						ViewTitleInfoManager.refreshTitleInfo(SB_Vuze.this.titleInfo);
						break;
					}
				}
			}
		});

		// Maintain a list of children entries; Open header on load
		mdi.addListener(new MdiEntryLoadedListener() {
			public void mdiEntryLoaded(MdiEntry entry) {
				if (MultipleDocumentInterface.SIDEBAR_HEADER_VUZE.equals(
						entry.getParentID())) {
					children.add(entry);
					entry.addListener(new MdiChildCloseListener() {
						public void mdiChildEntryClosed(MdiEntry parent, MdiEntry child,
								boolean user) {
							children.remove(child);
						}
					});
				}
				if (!entry.getId().equals(
						MultipleDocumentInterface.SIDEBAR_HEADER_VUZE)) {
					return;
				}
				titleInfo = new ViewTitleInfo_Vuze(entry);
				entry.setViewTitleInfo(titleInfo);
			}
		});
	}

	private static class ViewTitleInfo_Vuze
		implements ViewTitleInfo
	{
		private MdiEntry entry;

		public ViewTitleInfo_Vuze(MdiEntry entry) {
			this.entry = entry;
		}

		public Object getTitleInfoProperty(int propertyID) {
			if (propertyID == ViewTitleInfo.TITLE_INDICATOR_TEXT) {
				if (entry.isExpanded()) {
					return null;
				}
				StringBuilder sb = new StringBuilder();
				MdiEntry[] entries = entry.getMDI().getEntries();
				for (MdiEntry subEntry : entries) {
					if (entry.getId().equals(subEntry.getParentID())) {
						ViewTitleInfo titleInfo = subEntry.getViewTitleInfo();
						if (titleInfo != null) {
							Object text = titleInfo.getTitleInfoProperty(
									TITLE_INDICATOR_TEXT);
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
							Object text = titleInfo.getTitleInfoProperty(
									TITLE_INDICATOR_TEXT);
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
	}

}
