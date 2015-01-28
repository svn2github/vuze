/*
 * Created on Dec 2, 2013
 * Created by Paul Gardner
 * 
 * Copyright 2013 Azureus Software, Inc.  All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
 
package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;


import org.gudy.azureus2.ui.swt.SimpleTextEntryWindow;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnCreator;
import org.gudy.azureus2.ui.swt.views.tableitems.ColumnDateSizer;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.tables.*;



public class 
ShareRatioProgressItem
	extends ColumnDateSizer
{
	public static final Class DATASOURCE_TYPE = Download.class;

	public static final String COLUMN_ID = "sr_prog";

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] { CAT_TIME, CAT_SHARING, CAT_SWARM });
		info.setProficiency(TableColumnInfo.PROFICIENCY_INTERMEDIATE);
	}

	public ShareRatioProgressItem(String sTableID) {
		super(DATASOURCE_TYPE, COLUMN_ID, TableColumnCreator.DATE_COLUMN_WIDTH, sTableID);
		setRefreshInterval(INTERVAL_LIVE);
		setMultiline(false);
		
		final TableContextMenuItem menuSetInterval = addContextMenuItem(
				"TableColumn.menu.sr_prog.interval", MENU_STYLE_HEADER);
		menuSetInterval.setStyle(TableContextMenuItem.STYLE_PUSH);
		menuSetInterval.addListener(new MenuItemListener() {
			public void
			selected(
				MenuItem			menu,
				Object 				target )
			{
				SimpleTextEntryWindow entryWindow = new SimpleTextEntryWindow(
						"sr_prog.window.title", "sr_prog.window.message");
							
				int existing_sr = COConfigurationManager.getIntParameter( "Share Ratio Progress Interval" );
				
				String	sr_str 	= DisplayFormatters.formatDecimal((double) existing_sr / 1000, 3);
				
				entryWindow.setPreenteredText( sr_str, false );
				entryWindow.selectPreenteredText( true );
				entryWindow.setWidthHint( 400 );
				
				entryWindow.prompt();
				
				if ( entryWindow.hasSubmittedInput()){
					
					try{
						String text = entryWindow.getSubmittedInput().trim();
						
						if ( text.length() > 0 ){
						
							float f = Float.parseFloat( text );
							
							int sr = (int)(f * 1000 );
							
							COConfigurationManager.setParameter( "Share Ratio Progress Interval", sr );
						}
					}catch( Throwable e ){
						
					}
					
				}
			}
		});
	}

	
	public ShareRatioProgressItem(String tableID, boolean v) {
		this(tableID);
		setVisible(v);
	}

	public void refresh(TableCell cell, long timestamp) {
		DownloadManager dm = (DownloadManager) cell.getDataSource();
		
		long data = (dm == null) ? 0 : dm.getDownloadState().getLongAttribute( DownloadManagerState.AT_SHARE_RATIO_PROGRESS );
		
		if ( data == 0 ){
		
			super.refresh( cell, data ) ;
		
		}else{
			
			long		sr 		= (int)data;
			
			String		sr_str 	= DisplayFormatters.formatDecimal((double) sr / 1000, 3);
			
			timestamp = (data>>>32)*1000;

				// feed a bit of share ratio into sort order for fun
			
			timestamp += (sr&255);
			
			String prefix = sr_str + ": ";
					
			super.refresh( cell, timestamp, prefix );
		}
	}
}
