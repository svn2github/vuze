/**
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.ui.swt.views.table.utils;

import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.Map;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.LightHashMap;
import org.gudy.azureus2.ui.swt.views.table.TableColumnCoreCreationListener;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.*;

import com.aelitis.azureus.ui.common.table.TableColumnCore;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadTypeComplete;
import org.gudy.azureus2.plugins.download.DownloadTypeIncomplete;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;

/**
 * @author TuxPaper
 * @created Dec 19, 2007
 *
 */
public class TableColumnCreator
{
	public static int DATE_COLUMN_WIDTH = 110;

	public static TableColumnCore[] createIncompleteDM(String tableID) {
		final String[] defaultVisibleOrder = {
			HealthItem.COLUMN_ID,
			RankItem.COLUMN_ID,
			NameItem.COLUMN_ID,
			"azsubs.ui.column.subs",
			"Info",
			CommentIconItem.COLUMN_ID,
			SizeItem.COLUMN_ID,
			DownItem.COLUMN_ID,
			DoneItem.COLUMN_ID,
			StatusItem.COLUMN_ID,
			SeedsItem.COLUMN_ID,
			PeersItem.COLUMN_ID,
			DownSpeedItem.COLUMN_ID,
			UpSpeedItem.COLUMN_ID,
			ETAItem.COLUMN_ID,
			ShareRatioItem.COLUMN_ID,
			TrackerStatusItem.COLUMN_ID,
		};

		TableColumnManager tcManager = TableColumnManager.getInstance();
		Map mapTCs = tcManager.getTableColumnsAsMap(DownloadTypeIncomplete.class, tableID);

		if (!tcManager.loadTableColumnSettings(DownloadTypeIncomplete.class,
				tableID)
				|| areNoneVisible(mapTCs)) {
			setVisibility(mapTCs, defaultVisibleOrder);
			RankItem tc = (RankItem) mapTCs.get(RankItem.COLUMN_ID);
			if (tc != null) {
				tcManager.setDefaultSortColumnName(tableID, RankItem.COLUMN_ID);
				tc.setSortAscending(true);
			}
		}

		return (TableColumnCore[]) mapTCs.values().toArray(new TableColumnCore[0]);
	}

	public static TableColumnCore[] createCompleteDM(String tableID) {
		final String[] defaultVisibleOrder = {
			HealthItem.COLUMN_ID,
			RankItem.COLUMN_ID,
			"SeedingRank",
			NameItem.COLUMN_ID,
			"azsubs.ui.column.subs",
			"Info",
			"RateIt",
			CommentIconItem.COLUMN_ID,
			SizeItem.COLUMN_ID,
			StatusItem.COLUMN_ID,
			SeedsItem.COLUMN_ID,
			PeersItem.COLUMN_ID,
			UpSpeedItem.COLUMN_ID,
			ShareRatioItem.COLUMN_ID,
			UpItem.COLUMN_ID,
			TrackerStatusItem.COLUMN_ID,
		};

		TableColumnManager tcManager = TableColumnManager.getInstance();
		Map mapTCs = tcManager.getTableColumnsAsMap(DownloadTypeComplete.class, tableID);

		if (!tcManager.loadTableColumnSettings(DownloadTypeIncomplete.class,
				tableID)
				|| areNoneVisible(mapTCs)) {
			setVisibility(mapTCs, defaultVisibleOrder);

			RankItem tc = (RankItem) mapTCs.get(RankItem.COLUMN_ID);
			if (tc != null) {
				tcManager.setDefaultSortColumnName(tableID, RankItem.COLUMN_ID);
				tc.setSortAscending(true);
			}
		}

		return (TableColumnCore[]) mapTCs.values().toArray(new TableColumnCore[0]);
	}

	private static boolean areNoneVisible(Map mapTCs) {
		boolean noneVisible = true;
		for (Iterator iter = mapTCs.values().iterator(); iter.hasNext();) {
			TableColumn tc = (TableColumn) iter.next();
			if (tc.isVisible()) {
				noneVisible = false;
				break;
			}
		}
		return noneVisible;
	}

	private static void setVisibility(Map mapTCs, String[] defaultVisibleOrder) {
		for (Iterator iter = mapTCs.values().iterator(); iter.hasNext();) {
			TableColumnCore tc = (TableColumnCore) iter.next();
			tc.setVisible(false);
		}

		for (int i = 0; i < defaultVisibleOrder.length; i++) {
			String id = defaultVisibleOrder[i];
			TableColumnCore tc = (TableColumnCore) mapTCs.get(id);
			if (tc != null) {
				tc.setVisible(true);
				tc.setPositionNoShift(i);
			}
		}
	}

	/**
	 * 
	 *
	 * @since 3.1.1.1
	 */
	public static void initCoreColumns() {
		// short variable names to reduce wrapping
		final Map c = new LightHashMap(50);
		final Class all = Download.class;
		final Class dl = DownloadTypeIncomplete.class;
		final Class cd = DownloadTypeComplete.class;

		c.put(RankItem.COLUMN_ID, new cInfo(RankItem.class, all));
		c.put(NameItem.COLUMN_ID, new cInfo(NameItem.class, all));
		c.put(SizeItem.COLUMN_ID, new cInfo(SizeItem.class, all));
		c.put(DoneItem.COLUMN_ID, new cInfo(DoneItem.class, all));
		c.put(StatusItem.COLUMN_ID, new cInfo(StatusItem.class, all));
		c.put(ETAItem.COLUMN_ID, new cInfo(ETAItem.class, dl));
		c.put(HealthItem.COLUMN_ID, new cInfo(HealthItem.class, all));
		c.put(CommentIconItem.COLUMN_ID, new cInfo(CommentIconItem.class, all));
		c.put(DownItem.COLUMN_ID, new cInfo(DownItem.class, dl));
		c.put(SeedsItem.COLUMN_ID, new cInfo(SeedsItem.class, all));
		c.put(PeersItem.COLUMN_ID, new cInfo(PeersItem.class, all));
		c.put(DownSpeedItem.COLUMN_ID, new cInfo(DownSpeedItem.class, dl));
		c.put(UpSpeedItem.COLUMN_ID, new cInfo(UpSpeedItem.class, all));
		c.put(UpSpeedLimitItem.COLUMN_ID, new cInfo(UpSpeedLimitItem.class, all));
		c.put(TrackerStatusItem.COLUMN_ID, new cInfo(TrackerStatusItem.class, all));
		c.put(CompletedItem.COLUMN_ID, new cInfo(CompletedItem.class, all));
		c.put(ShareRatioItem.COLUMN_ID, new cInfo(ShareRatioItem.class, all));
		c.put(UpItem.COLUMN_ID, new cInfo(UpItem.class, all));
		c.put(RemainingItem.COLUMN_ID, new cInfo(RemainingItem.class, dl));
		c.put(PiecesItem.COLUMN_ID, new cInfo(PiecesItem.class, dl));
		c.put(CompletionItem.COLUMN_ID, new cInfo(CompletionItem.class, dl));
		c.put(CommentItem.COLUMN_ID, new cInfo(CommentItem.class, all));
		c.put(MaxUploadsItem.COLUMN_ID, new cInfo(MaxUploadsItem.class, all));
		c.put(TotalSpeedItem.COLUMN_ID, new cInfo(TotalSpeedItem.class, all));
		c.put(FilesDoneItem.COLUMN_ID, new cInfo(FilesDoneItem.class, all));
		c.put(SavePathItem.COLUMN_ID, new cInfo(SavePathItem.class, all));
		c.put(TorrentPathItem.COLUMN_ID, new cInfo(TorrentPathItem.class, all));
		c.put(CategoryItem.COLUMN_ID, new cInfo(CategoryItem.class, all));
		c.put(NetworksItem.COLUMN_ID, new cInfo(NetworksItem.class, all));
		c.put(PeerSourcesItem.COLUMN_ID, new cInfo(PeerSourcesItem.class, all));
		c.put(AvailabilityItem.COLUMN_ID, new cInfo(AvailabilityItem.class, all));
		c.put(AvgAvailItem.COLUMN_ID, new cInfo(AvgAvailItem.class, all));
		c.put(SecondsSeedingItem.COLUMN_ID, new cInfo(SecondsSeedingItem.class, all));
		c.put(SecondsDownloadingItem.COLUMN_ID, new cInfo(SecondsDownloadingItem.class, all));
		c.put(TimeSinceDownloadItem.COLUMN_ID, new cInfo(TimeSinceDownloadItem.class, dl));
		c.put(TimeSinceUploadItem.COLUMN_ID, new cInfo(TimeSinceUploadItem.class, all));
		c.put(OnlyCDing4Item.COLUMN_ID, new cInfo(OnlyCDing4Item.class, all));
		c.put(TrackerNextAccessItem.COLUMN_ID, new cInfo(TrackerNextAccessItem.class, all));
		c.put(TrackerNameItem.COLUMN_ID, new cInfo(TrackerNameItem.class, all));
		c.put(SeedToPeerRatioItem.COLUMN_ID, new cInfo(SeedToPeerRatioItem.class, all));
		c.put(DownSpeedLimitItem.COLUMN_ID, new cInfo(DownSpeedLimitItem.class, dl));
		c.put(SwarmAverageSpeed.COLUMN_ID, new cInfo(SwarmAverageSpeed.class, all));
		c.put(SwarmAverageCompletion.COLUMN_ID, new cInfo(SwarmAverageCompletion.class, all));
		c.put(BadAvailTimeItem.COLUMN_ID, new cInfo(BadAvailTimeItem.class, all));

		c.put(DateCompletedItem.COLUMN_ID, new cInfo(DateCompletedItem.class, cd));
		c.put(DateAddedItem.COLUMN_ID, new cInfo(DateAddedItem.class, all));

		// Core columns are implementors of TableColumn to save one class creation
		// Otherwise, we'd have to create a generic TableColumnImpl class, pass it 
		// to another class so that it could manipulate it and act upon changes.

		TableColumnManager tcManager = TableColumnManager.getInstance();

		TableColumnCoreCreationListener tcCreator = new TableColumnCoreCreationListener() {
			// @see org.gudy.azureus2.ui.swt.views.table.TableColumnCoreCreationListener#createTableColumnCore()
			public TableColumnCore createTableColumnCore(String tableID,
					String columnID)
			{
				cInfo info = (cInfo) c.get(columnID);

				try {
					Constructor constructor = info.cla.getDeclaredConstructor(new Class[] { String.class });
					TableColumnCore column = (TableColumnCore) constructor.newInstance(new Object[] { tableID });
					return column;
				} catch (Exception e) {
					Debug.out(e);
				}

				return null;
			}

			public void tableColumnCreated(TableColumn column) {
			}
		};

		for (Iterator iter = c.keySet().iterator(); iter.hasNext();) {
			String id = (String) iter.next();
			cInfo info = (cInfo) c.get(id);

			tcManager.registerColumn(info.forDataSourceType, id, tcCreator);
		}

	}
	
	private static class cInfo {
		public Class cla;
		public Class forDataSourceType;
		
		public cInfo(Class cla, Class forDataSourceType) {
			this.cla = cla;
			this.forDataSourceType = forDataSourceType;
		}
	}
}
