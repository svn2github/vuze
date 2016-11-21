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

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadTypeComplete;
import org.gudy.azureus2.plugins.download.DownloadTypeIncomplete;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.MyTorrentsView;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableRowCore;

public class MyTorrentsView_Big
	extends MyTorrentsView
{
	private final int torrentFilterMode;
	private int defaultRowHeight;

	public MyTorrentsView_Big(AzureusCore _azureus_core, int torrentFilterMode,
			TableColumnCore[] basicItems, Text txtFilter, Composite cCatsTags) {
		super( true );
		defaultRowHeight = Utils.adjustPXForDPI(40);
		this.torrentFilterMode = torrentFilterMode;
		this.txtFilter = txtFilter;
		this.cCategoriesAndTags = cCatsTags;
		Class<?> forDataSourceType;
		switch (torrentFilterMode) {
			case SBC_LibraryView.TORRENTS_COMPLETE:
			case SBC_LibraryView.TORRENTS_UNOPENED:
				forDataSourceType = DownloadTypeComplete.class;
				break;

			case SBC_LibraryView.TORRENTS_INCOMPLETE:
				forDataSourceType = DownloadTypeIncomplete.class;
				break;
				
			default:
				forDataSourceType = Download.class;
				break;
		}
		init(
				_azureus_core,
				SB_Transfers.getTableIdFromFilterMode(torrentFilterMode, true),
				forDataSourceType, basicItems);
		//setForceHeaderVisible(true);
	}
	

	public boolean isOurDownloadManager(DownloadManager dm) {
		if (PlatformTorrentUtils.isAdvancedViewOnly(dm)) {
			return false;
		}
		
		if (torrentFilterMode == SBC_LibraryView.TORRENTS_UNOPENED) {
			if (PlatformTorrentUtils.getHasBeenOpened(dm)) {
				return false;
			}
		} else if (torrentFilterMode == SBC_LibraryView.TORRENTS_ALL) {
			if ( !isInCurrentTag(dm)){
				return(false );
			}
			return( isInCurrentTag(dm));
		}
		
		return super.isOurDownloadManager(dm);
	}

	// @see org.gudy.azureus2.ui.swt.views.MyTorrentsView#defaultSelected(com.aelitis.azureus.ui.common.table.TableRowCore[])
	public void defaultSelected(TableRowCore[] rows, int stateMask) {
		boolean neverPlay = DownloadTypeIncomplete.class.equals(getForDataSourceType());
		SBC_LibraryTableView.doDefaultClick(rows, stateMask, neverPlay);
	}

	protected int getRowDefaultHeight() {
		return defaultRowHeight;
	}
	
}
