package com.aelitis.azureus.ui.swt.views.skin;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadTypeComplete;
import org.gudy.azureus2.plugins.download.DownloadTypeIncomplete;
import org.gudy.azureus2.ui.swt.views.MyTorrentsView;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableRowCore;

public class MyTorrentsView_Big
	extends MyTorrentsView
{
	private final int torrentFilterMode;

	public MyTorrentsView_Big(AzureusCore _azureus_core, int torrentFilterMode,
			TableColumnCore[] basicItems, Text txtFilter, Composite cCats) {
		super( true );
		this.torrentFilterMode = torrentFilterMode;
		this.txtFilter = txtFilter;
		this.cCategories = cCats;
		Class<?> forDataSourceType;
		switch (torrentFilterMode) {
			case SBC_LibraryView.TORRENTS_COMPLETE:
				forDataSourceType = DownloadTypeComplete.class;
				break;

			case SBC_LibraryView.TORRENTS_INCOMPLETE:
				forDataSourceType = DownloadTypeIncomplete.class;
				break;
				
			case SBC_LibraryView.TORRENTS_UNOPENED:
				forDataSourceType = Download.class;
				break;
				
			case SBC_LibraryView.TORRENTS_ALL:
				forDataSourceType = Download.class;
				break;

			default:
				forDataSourceType = null;
				break;
		}
		init(
				_azureus_core,
				SB_Transfers.getTableIdFromFilterMode(torrentFilterMode, true),
				torrentFilterMode == SBC_LibraryView.TORRENTS_INCOMPLETE ? false : true,
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
			if ( !isInCurrentCategory(dm)){
				return(false );
			}
			return( isInCurrentTag(dm));
		}
		
		return super.isOurDownloadManager(dm);
	}

	// @see org.gudy.azureus2.ui.swt.views.MyTorrentsView#defaultSelected(com.aelitis.azureus.ui.common.table.TableRowCore[])
	public void defaultSelected(TableRowCore[] rows, int stateMask) {
		SBC_LibraryTableView.doDefaultClick(rows, stateMask, !isSeedingView);
	}

	protected int getRowDefaultHeight() {
		return 40;
	}
	
}
