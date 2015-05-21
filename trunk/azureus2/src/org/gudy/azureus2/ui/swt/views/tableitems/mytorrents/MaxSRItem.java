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

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumnInfo;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.ui.swt.SimpleTextEntryWindow;
import org.gudy.azureus2.ui.swt.views.table.CoreTableColumnSWT;

import com.aelitis.azureus.ui.common.table.TableRowCore;

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
		
	    TableContextMenuItem menuItem = addContextMenuItem("menu.max.share.ratio2");
	    menuItem.addMultiListener(new MenuItemListener() {
				public void selected(MenuItem menu, Object target) {
					if (target == null) {
						return;
					}
					
					Object[] o = (Object[]) target;
					
					int existing = -1;
					
					for (Object object : o) {
						if (object instanceof TableRowCore) {
							TableRowCore rowCore = (TableRowCore) object;
							object = rowCore.getDataSource(true);
						}
						if (object instanceof DownloadManager) {
							int x = ((DownloadManager)object).getDownloadState().getIntParameter( DownloadManagerState.PARAM_MAX_SHARE_RATIO );
							
							if ( existing == -1 ){
								existing = x;
							}else if ( existing != x ){
								existing = -1;
								break;
							}
						}
					}
					
					String existing_text;
					
					if ( existing == -1 ){
						existing_text = "";
					}else{
						existing_text = String.valueOf( existing/1000.0f);
					}
					
					SimpleTextEntryWindow entryWindow = new SimpleTextEntryWindow(
							"max.sr.window.title", "max.sr.window.message");
											
					entryWindow.setPreenteredText( existing_text, false );
					entryWindow.selectPreenteredText( true );
					
					entryWindow.prompt();
					
					if ( entryWindow.hasSubmittedInput()){
						
						try{
							String text = entryWindow.getSubmittedInput().trim();
							
							int	sr = 0;
							
							if ( text.length() > 0 ){
							
								try{
									float f = Float.parseFloat( text );
								
									sr = (int)(f * 1000 );
								
									if ( sr < 0 ){
										
										sr = 0;
										
									}else if ( sr == 0 && f > 0 ){
										
										sr = 1;
									}
								
								}catch( Throwable e ){
									
									Debug.out( e );
								}
								
								for (Object object : o) {
									if (object instanceof TableRowCore) {
										TableRowCore rowCore = (TableRowCore) object;
										object = rowCore.getDataSource(true);
									}
									if (object instanceof DownloadManager) {
										((DownloadManager)object).getDownloadState().setIntParameter( DownloadManagerState.PARAM_MAX_SHARE_RATIO, sr );
									}
								}								
							}			
						}catch( Throwable e ){
							
							Debug.out( e );
						}
					}
				}
	    });
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
