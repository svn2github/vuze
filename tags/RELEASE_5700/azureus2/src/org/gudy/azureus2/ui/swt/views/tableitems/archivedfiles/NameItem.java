/**
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
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

package org.gudy.azureus2.ui.swt.views.tableitems.archivedfiles;

import java.io.File;

import org.gudy.azureus2.ui.swt.debug.ObfusticateCellText;
import org.gudy.azureus2.ui.swt.views.ArchivedFilesView;
import org.gudy.azureus2.ui.swt.views.table.CoreTableColumnSWT;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.download.DownloadStub.DownloadStubFile;
import org.gudy.azureus2.plugins.ui.tables.*;



public class 
NameItem 
	extends CoreTableColumnSWT 
	implements TableCellLightRefreshListener, ObfusticateCellText
{
	public 
	NameItem(
		String tableID )
	{
		super(	"name", ALIGN_LEAD, POSITION_LAST, 400, tableID );
		
		setType(TableColumn.TYPE_TEXT);
	}

	public void 
	fillTableColumnInfo(
		TableColumnInfo info )
	{
		info.addCategories(new String[] {
			CAT_CONTENT,
		});
		
		info.setProficiency(TableColumnInfo.PROFICIENCY_BEGINNER);
	}
	
	public void 
	refresh(
		TableCell 	cell, 
		boolean 	sortOnlyRefresh)
	{
		DownloadStubFile fileInfo = (DownloadStubFile) cell.getDataSource();
		
		String name;
		
		if ( fileInfo == null ){
			
			name = "";
			
		}else{
			
			File f = fileInfo.getFile();
			
			if ( ArchivedFilesView.show_full_path ){
				
				name = f.getAbsolutePath();
			}else{
				
				name = f.getName();
			}
		}
		
		cell.setText(name);
	}

	public void 
	refresh(
		TableCell cell )
	{
		refresh(cell, false);
	}

	public String 
	getObfusticatedText(
		TableCell cell) 
	{
		DownloadStubFile fileInfo = (DownloadStubFile) cell.getDataSource();
		
		String name = (fileInfo == null) ? "" : Debug.secretFileName(fileInfo.getFile().getName());
		
		return( name );
	}
}
