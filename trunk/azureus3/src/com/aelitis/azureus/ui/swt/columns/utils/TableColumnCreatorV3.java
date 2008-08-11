package com.aelitis.azureus.ui.swt.columns.utils;

import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.AvailabilityItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.AvgAvailItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.BadAvailTimeItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.CategoryItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.CommentIconItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.CommentItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.CompletedItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.CompletionItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.DateAddedItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.DateCompletedItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.DoneItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.DownItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.DownSpeedItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.DownSpeedLimitItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.ETAItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.FilesDoneItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.HealthItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.MaxUploadsItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.NameItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.NetworksItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.OnlyCDing4Item;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.PeerSourcesItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.PeersItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.PiecesItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.RankItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.RemainingItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.SavePathItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.SecondsDownloadingItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.SecondsSeedingItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.SeedToPeerRatioItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.SeedsItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.ShareRatioItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.SizeItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.StatusItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.SwarmAverageCompletion;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.SwarmAverageSpeed;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.TimeSinceDownloadItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.TimeSinceUploadItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.TorrentPathItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.TotalSpeedItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.TrackerNameItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.TrackerNextAccessItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.TrackerStatusItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.UpItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.UpSpeedItem;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.UpSpeedLimitItem;

import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.swt.columns.torrent.ColumnThumbnail;

/**
 * A utility class for creating some common column sets; this is a virtual clone of <code>TableColumnCreator</code>
 * with slight modifications
 * @author khai
 *
 */
public class TableColumnCreatorV3
{

	public static TableColumnCore[] createIncompleteDM(String tableID) {

		return new TableColumnCore[] {
			/*
			 * Initially visible
			 */
			show(new ColumnThumbnail(tableID, 73, 5)),
			show(new RankItem(tableID)),
			show(new NameItem(tableID, true, false)),
			show(new SizeItem(tableID)),
			show(new DoneItem(tableID)),
			show(new StatusItem(tableID)),
			show(new ETAItem(tableID)),

			/*
			 * Initially hidden
			 */
			hide(new HealthItem(tableID)),
			hide(new CommentIconItem(tableID)),
			hide(new DownItem(tableID)),
			hide(new SeedsItem(tableID)),
			hide(new PeersItem(tableID)),
			hide(new DownSpeedItem(tableID)),
			hide(new UpSpeedItem(tableID)),
			hide(new UpSpeedLimitItem(tableID)),
			hide(new TrackerStatusItem(tableID)),
			hide(new CompletedItem(tableID)),
			hide(new ShareRatioItem(tableID, false)),
			hide(new UpItem(tableID, false)),
			hide(new RemainingItem(tableID)),
			hide(new PiecesItem(tableID, 16)),
			hide(new CompletionItem(tableID, 16)),
			hide(new CommentItem(tableID)),
			hide(new MaxUploadsItem(tableID)),
			hide(new TotalSpeedItem(tableID)),
			hide(new FilesDoneItem(tableID)),
			hide(new SavePathItem(tableID)),
			hide(new TorrentPathItem(tableID)),
			hide(new CategoryItem(tableID)),
			hide(new NetworksItem(tableID)),
			hide(new PeerSourcesItem(tableID)),
			hide(new AvailabilityItem(tableID)),
			hide(new AvgAvailItem(tableID)),
			hide(new SecondsSeedingItem(tableID)),
			hide(new SecondsDownloadingItem(tableID)),
			hide(new TimeSinceDownloadItem(tableID)),
			hide(new TimeSinceUploadItem(tableID)),
			hide(new OnlyCDing4Item(tableID)),
			hide(new TrackerNextAccessItem(tableID)),
			hide(new TrackerNameItem(tableID)),
			hide(new SeedToPeerRatioItem(tableID)),
			hide(new DownSpeedLimitItem(tableID)),
			hide(new SwarmAverageSpeed(tableID)),
			hide(new SwarmAverageCompletion(tableID)),
			hide(new DateAddedItem(tableID)),
			hide(new BadAvailTimeItem(tableID)),
		};
	}

	public static TableColumnCore[] createCompleteDM(String tableID) {

		return new TableColumnCore[] {
			/*
			 * Initially visible
			 */
			show(new ColumnThumbnail(tableID, 73, 5)),
			show(new RankItem(tableID)),
			show(new NameItem(tableID, true, false)),
			show(new SizeItem(tableID)),
			show(new DoneItem(tableID)),
			show(new StatusItem(tableID)),

			/*
			 * Initially hidden
			 */
			hide(new CompletedItem(tableID)),
			hide(new CommentItem(tableID)),
			hide(new MaxUploadsItem(tableID)),
			hide(new TotalSpeedItem(tableID)),
			hide(new FilesDoneItem(tableID)),
			hide(new SavePathItem(tableID)),
			hide(new TorrentPathItem(tableID)),
			hide(new CategoryItem(tableID)),
			hide(new NetworksItem(tableID)),
			hide(new PeerSourcesItem(tableID)),
			hide(new AvailabilityItem(tableID)),
			hide(new AvgAvailItem(tableID)),
			hide(new SecondsSeedingItem(tableID)),
			hide(new SecondsDownloadingItem(tableID)),
			hide(new TimeSinceUploadItem(tableID)),
			hide(new OnlyCDing4Item(tableID)),
			hide(new TrackerStatusItem(tableID)),
			hide(new TrackerNextAccessItem(tableID)),
			hide(new TrackerNameItem(tableID)),
			hide(new SeedToPeerRatioItem(tableID)),
			hide(new SwarmAverageSpeed(tableID)),
			hide(new SwarmAverageCompletion(tableID)),
			hide(new DateAddedItem(tableID)),
			hide(new DateCompletedItem(tableID))
		};
	}

	private static TableColumnCore show(TableColumnCore column) {
		if (null != column) {
			column.setVisible(true);
		}
		return column;
	}

	private static TableColumnCore hide(TableColumnCore column) {
		if (null != column) {
			column.setVisible(false);
		}
		return column;
	}
}
