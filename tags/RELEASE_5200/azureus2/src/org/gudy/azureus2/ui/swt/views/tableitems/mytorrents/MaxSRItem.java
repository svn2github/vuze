/*
 * File    : CategoryItem.java
 * Created : 01 feb. 2004
 * By      : TuxPaper
 *
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumnInfo;
import org.gudy.azureus2.ui.swt.views.table.CoreTableColumnSWT;

/** Display Category torrent belongs to.
 *
 * @author TuxPaper
 */
public class MaxSRItem
       extends CoreTableColumnSWT 
       implements TableCellRefreshListener
{
	public static final Class DATASOURCE_TYPE = Download.class;

	public static final String COLUMN_ID = "max_sr";

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] { CAT_SHARING });
		info.setProficiency(TableColumnInfo.PROFICIENCY_INTERMEDIATE);
	}

	/** Default Constructor */
	public MaxSRItem(String sTableID) {
		super(DATASOURCE_TYPE, COLUMN_ID, ALIGN_TRAIL, 70, sTableID);
		setRefreshInterval(INTERVAL_LIVE);
	}

	public void refresh(TableCell cell) {
		DownloadManager dm = (DownloadManager)cell.getDataSource();
		int	value = 0;
		if (dm != null) {
			value = dm.getDownloadState().getIntParameter( DownloadManagerState.PARAM_MAX_SHARE_RATIO );
		}
		
		if (!cell.setSortValue(value) && cell.isValid()){
		      return;
		}
		
		cell.setText( value==0?"": String.valueOf( value/1000.0f));
	}
}
