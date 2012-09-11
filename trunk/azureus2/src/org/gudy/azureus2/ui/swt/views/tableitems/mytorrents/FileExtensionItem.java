/*
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

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.tables.*;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

public class FileExtensionItem
	extends CoreTableColumn 
	implements TableCellRefreshListener
{
	public static final Class DATASOURCE_TYPE = Download.class;

	public static final String COLUMN_ID = "fileext";

	public 
	FileExtensionItem(String sTableID) 
	{
		super( DATASOURCE_TYPE, COLUMN_ID, ALIGN_CENTER, 50, sTableID);
		
		setMinWidthAuto(true);
	}

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			CAT_CONTENT,
		});
		info.setProficiency(TableColumnInfo.PROFICIENCY_INTERMEDIATE);
	}

	public void refresh(TableCell cell) {
		DownloadManager dm = (DownloadManager)cell.getDataSource();

		String	text = "";

		if ( dm != null ){
			String name = dm.getDownloadState().getPrimaryFile().getFile( true ).getName();
			
			int	pos = name.lastIndexOf( "." );
			
			if ( pos >= 0 ){
				
				text = name.substring( pos+1 );
			}
		}

		cell.setText( text );
	}
}
