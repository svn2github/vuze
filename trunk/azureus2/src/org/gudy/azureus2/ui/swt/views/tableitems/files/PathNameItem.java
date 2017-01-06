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

package org.gudy.azureus2.ui.swt.views.tableitems.files;

import java.io.File;

import org.eclipse.swt.graphics.Image;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.debug.ObfusticateCellText;
import org.gudy.azureus2.ui.swt.views.table.CoreTableColumnSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.plugins.ui.tables.*;



public class PathNameItem extends CoreTableColumnSWT implements
		TableCellLightRefreshListener, ObfusticateCellText, TableCellDisposeListener
{
	private static boolean bShowIcon;

	static {
		COConfigurationManager.addAndFireParameterListener(
				"NameColumn.showProgramIcon", new ParameterListener() {
					public void parameterChanged(String parameterName) {
						bShowIcon = COConfigurationManager.getBooleanParameter("NameColumn.showProgramIcon");
					}
				});
	}
	
	/** Default Constructor */
	public PathNameItem() {
		super("pathname", ALIGN_LEAD, POSITION_INVISIBLE, 500,
				TableManager.TABLE_TORRENT_FILES);
		setObfustication(true);
		setType(TableColumn.TYPE_TEXT);
	}

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			CAT_CONTENT,
		});
		info.setProficiency(TableColumnInfo.PROFICIENCY_INTERMEDIATE);
	}
	
	public void refresh(TableCell cell, boolean sortOnlyRefresh)
	{
		final DiskManagerFileInfo fileInfo = (DiskManagerFileInfo) cell.getDataSource();
		String file_name = (fileInfo == null) ? "" : fileInfo.getFile(true).getName();
		if (file_name == null)
			file_name = "";
		String file_path = PathItem.determinePath(fileInfo);
		
		if ( !file_path.isEmpty()){
		
			if ( !file_path.endsWith( File.separator )){
				
				file_path += File.separator;
			}
			
			file_name = file_path + file_name;
		}
		//setText returns true only if the text is updated
		if (cell.setText(file_name) || !cell.isValid()) {
			if (bShowIcon && !sortOnlyRefresh) {
				Image icon = null;
				
				final TableCellSWT _cell = (TableCellSWT)cell;
				
				if (fileInfo == null) {
					icon = null;
				} else {
					
					// Don't ever dispose of PathIcon, it's cached and may be used elsewhere
					
					if ( Utils.isSWTThread()){
					
						icon = ImageRepository.getPathIcon(fileInfo.getFile(true).getPath(),
								cell.getHeight() > 32, false);
					}else{	
							// happens rarely (seen of filtering of file-view rows
							// when a new row is added )
												
						Utils.execSWTThread(
							new Runnable()
							{
								public void
								run()
								{
									Image icon = ImageRepository.getPathIcon(fileInfo.getFile(true).getPath(),
											_cell.getHeight() > 32, false);
									
									_cell.setIcon(icon);
									
									_cell.redraw();
								}
							});
					}
				}

				// cheat for core, since we really know it's a TabeCellImpl and want to use
				// those special functions not available to Plugins
				
				if ( icon != null ){
					_cell.setIcon(icon);
				}
			}
		}
	}

	public void refresh(TableCell cell)
	{
		refresh(cell, false);
	}

	public String getObfusticatedText(TableCell cell) {
		DiskManagerFileInfo fileInfo = (DiskManagerFileInfo) cell.getDataSource();
		String name = (fileInfo == null) ? "" : fileInfo.getIndex() + ": "
				+ Debug.secretFileName(fileInfo.getFile(true).getName());
		return name;
	}

	public void dispose(TableCell cell) {
	}
}
