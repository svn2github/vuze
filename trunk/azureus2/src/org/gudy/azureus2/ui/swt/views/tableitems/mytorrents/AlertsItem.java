/*
 * File    : CategoryItem.java
 * Created : 01 feb. 2004
 * By      : TuxPaper
 *
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

package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import java.util.Map;

import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTGraphicImpl;
import org.gudy.azureus2.ui.swt.views.table.CoreTableColumnSWT;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumnInfo;

import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

/** Display Category torrent belongs to.
 *
 * @author TuxPaper
 */
public class AlertsItem
	extends CoreTableColumnSWT 
	implements TableCellRefreshListener
{
	public static final Class DATASOURCE_TYPE = Download.class;

	private static final UISWTGraphic black_tick_icon;
	private static final UISWTGraphic gray_tick_icon;

	static {
		black_tick_icon = new UISWTGraphicImpl(ImageLoader.getInstance().getImage("blacktick"));
		gray_tick_icon 	= new UISWTGraphicImpl(ImageLoader.getInstance().getImage("graytick"));
	}

	public static final String COLUMN_ID = "alerts";

	public AlertsItem(String sTableID) {
		super(DATASOURCE_TYPE, COLUMN_ID, ALIGN_CENTER, 60, sTableID);
		addDataSourceType(DiskManagerFileInfo.class);
		setRefreshInterval(INTERVAL_LIVE);
		initializeAsGraphic(POSITION_INVISIBLE, 60);
		setMinWidth(20);
	}

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
				CAT_CONNECTION,
		});
		info.setProficiency(TableColumnInfo.PROFICIENCY_INTERMEDIATE );
	}

	public void 
	refresh(
		TableCell cell) 
	{
		UISWTGraphic	icon 	= null;
		int				sort	= 0;
		
		Object ds = cell.getDataSource();

		
		if ( ds instanceof DownloadManager ){
			
			DownloadManager dm =  (DownloadManager)ds;
			
			Map<String,String> map =  dm.getDownloadState().getMapAttribute( DownloadManagerState.AT_DL_FILE_ALERTS  );

			if ( map != null && map.size() > 0 ){
				
				for ( String k: map.keySet()){
					
					if ( k.length() > 0 ){
						
						if ( Character.isDigit( k.charAt(0))){
					
							icon 	= gray_tick_icon;
							sort	= 1;
							
						}else{
						
							icon 	= black_tick_icon;
							sort	= 2;
							
							break;
						}
					}
				}
			}
		}else if ( ds instanceof DiskManagerFileInfo ){
			
			DiskManagerFileInfo fi = (DiskManagerFileInfo)ds;
			
			DownloadManager dm = fi.getDownloadManager();
			
			Map<String,String> map =  dm.getDownloadState().getMapAttribute( DownloadManagerState.AT_DL_FILE_ALERTS  );

			if ( map != null && map.size() > 0 ){

				String prefix = fi.getIndex() + ".";
				
				for ( String k: map.keySet()){
					
					if ( k.startsWith( prefix )){
						
						icon 	= black_tick_icon;
						sort	= 2;
						
						break;
					}
				}
			}
		}

		cell.setSortValue( sort );
		
		if ( cell.getGraphic() != icon ){

			cell.setGraphic( icon );
		}
	}
}
