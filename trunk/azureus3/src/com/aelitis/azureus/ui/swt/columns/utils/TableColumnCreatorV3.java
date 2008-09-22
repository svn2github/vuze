package com.aelitis.azureus.ui.swt.columns.utils;

import java.lang.reflect.Constructor;
import java.util.*;

import org.gudy.azureus2.ui.swt.views.table.TableColumnCoreCreationListener;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnCreator;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnManager;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.*;

import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.swt.columns.torrent.*;

import org.gudy.azureus2.plugins.download.*;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;

/**
 * A utility class for creating some common column sets; this is a virtual clone of <code>TableColumnCreator</code>
 * with slight modifications
 * @author khai
 *
 */
public class TableColumnCreatorV3
{

	public static TableColumnCore[] createIncompleteDM(String tableID) {
		final String[] defaultVisibleOrder = {
				ColumnThumbnail.COLUMN_ID,
				RankItem.COLUMN_ID,
				NameItem.COLUMN_ID,
				ColumnQuality.COLUMN_ID,
				ColumnInfo.COLUMN_ID,
				SizeItem.COLUMN_ID,
				DoneItem.COLUMN_ID,
				StatusItem.COLUMN_ID,
				ETAItem.COLUMN_ID,
				ColumnRateUpDown.COLUMN_ID,
				DateAddedItem.COLUMN_ID,
		};

		TableColumnManager tcManager = TableColumnManager.getInstance();
		Map mapTCs = tcManager.getTableColumnsAsMap(DownloadTypeIncomplete.class, tableID);
		
		setVisibility(mapTCs, defaultVisibleOrder);

		// special changes
		StatusItem tcStatusItem = (StatusItem) mapTCs.get(StatusItem.COLUMN_ID);
		if (tcStatusItem != null) {
			tcStatusItem.setChangeRowFG(false);
		}
		NameItem tcNameItem = (NameItem) mapTCs.get(NameItem.COLUMN_ID);
		if (tcNameItem != null) {
			tcNameItem.setShowIcon(false);
		}
		
		return (TableColumnCore[]) mapTCs.values().toArray(new TableColumnCore[0]);
	}

	/**
	 * @param mapTCs
	 * @param defaultVisibleOrder
	 */
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
			}
		}
	}

	public static TableColumnCore[] createCompleteDM(String tableID) {
		final String[] defaultVisibleOrder = {
				ColumnThumbnail.COLUMN_ID,
				RankItem.COLUMN_ID,
				NameItem.COLUMN_ID,
				ColumnQuality.COLUMN_ID,
				ColumnInfo.COLUMN_ID,
				SizeItem.COLUMN_ID,
				DoneItem.COLUMN_ID,
				StatusItem.COLUMN_ID,
				ColumnRatingGlobal.COLUMN_ID,
				ColumnRateUpDown.COLUMN_ID,
				DateCompletedItem.COLUMN_ID,
		};

		TableColumnManager tcManager = TableColumnManager.getInstance();
		Map mapTCs = tcManager.getTableColumnsAsMap(DownloadTypeIncomplete.class, tableID);
		
		setVisibility(mapTCs, defaultVisibleOrder);

		// special changes
		StatusItem tcStatusItem = (StatusItem) mapTCs.get(StatusItem.COLUMN_ID);
		if (tcStatusItem != null) {
			tcStatusItem.setChangeRowFG(false);
		}
		NameItem tcNameItem = (NameItem) mapTCs.get(NameItem.COLUMN_ID);
		if (tcNameItem != null) {
			tcNameItem.setShowIcon(false);
		}
		
		return (TableColumnCore[]) mapTCs.values().toArray(new TableColumnCore[0]);
	}

	public static TableColumnCore[] createUnopenedDM(String tableID, boolean big) {
		final String[] defaultVisibleOrder = {
				ColumnUnopened.COLUMN_ID,
				ColumnThumbnail.COLUMN_ID,
				RankItem.COLUMN_ID,
				NameItem.COLUMN_ID,
				ColumnQuality.COLUMN_ID,
				ColumnInfo.COLUMN_ID,
				SizeItem.COLUMN_ID,
				DoneItem.COLUMN_ID,
				StatusItem.COLUMN_ID,
				ColumnRatingGlobal.COLUMN_ID,
				ColumnRateUpDown.COLUMN_ID,
				DateCompletedItem.COLUMN_ID,
		};

		TableColumnManager tcManager = TableColumnManager.getInstance();
		Map mapTCs = tcManager.getTableColumnsAsMap(DownloadTypeIncomplete.class, tableID);
		
		setVisibility(mapTCs, defaultVisibleOrder);

		// special changes
		StatusItem tcStatusItem = (StatusItem) mapTCs.get(StatusItem.COLUMN_ID);
		if (tcStatusItem != null) {
			tcStatusItem.setChangeRowFG(false);
		}
		NameItem tcNameItem = (NameItem) mapTCs.get(NameItem.COLUMN_ID);
		if (tcNameItem != null) {
			tcNameItem.setShowIcon(false);
		}
		
		return (TableColumnCore[]) mapTCs.values().toArray(new TableColumnCore[0]);
	}

	/**
	 * 
	 *
	 * @since 3.1.1.1
	 */
	public static void initCoreColumns() {
		TableColumnCreator.initCoreColumns();
		
		// short variable names to reduce wrapping
		final Map c = new HashMap();
		final Class all = Download.class;
		//final Class dl = DownloadTypeIncomplete.class;
		//final Class cd = DownloadTypeComplete.class;

		c.put(ColumnUnopened.COLUMN_ID, new cInfo(ColumnUnopened.class, all));
		c.put(ColumnThumbnail.COLUMN_ID, new cInfo(ColumnThumbnail.class, all));
		c.put(ColumnQuality.COLUMN_ID, new cInfo(ColumnQuality.class, all));
		c.put(ColumnInfo.COLUMN_ID, new cInfo(ColumnInfo.class, all));
		c.put(ColumnRateUpDown.COLUMN_ID, new cInfo(ColumnRateUpDown.class, all));
		c.put(ColumnRatingGlobal.COLUMN_ID, new cInfo(ColumnRatingGlobal.class, all));

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
