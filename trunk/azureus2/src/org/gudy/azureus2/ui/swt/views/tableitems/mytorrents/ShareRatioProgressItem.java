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
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.download.DownloadManagerStats;
import org.gudy.azureus2.core3.util.Constants;
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

	private static int existing_sr;
	
	static{
		COConfigurationManager.addAndFireParameterListener(
			"Share Ratio Progress Interval",
			new ParameterListener() {
				
				public void 
				parameterChanged(
					String name )
				{
					existing_sr = COConfigurationManager.getIntParameter( name );
				}
			});
	}
	
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
		
		if ( dm == null || existing_sr <= 0 ){
			
			super.refresh( cell, 0 );
			
			return;
		}	
	
		int dm_state = dm.getState();
		
		long	next_eta = -1;
		
		if ( dm_state == DownloadManager.STATE_DOWNLOADING || dm_state == DownloadManager.STATE_SEEDING ){
							
			DownloadManagerStats stats = dm.getStats();
			
			long	downloaded 	= stats.getTotalGoodDataBytesReceived();
			long	uploaded 	= stats.getTotalDataBytesSent();

			if ( downloaded <= 0 ){
				
				next_eta = -2;
				
			}else{
				
				int current_sr = (int) ((1000 * uploaded) / downloaded);
			
				int	mult = current_sr / existing_sr;
				
				int	next_target_sr = (mult+1)*existing_sr;
				
				long	up_speed	= stats.getDataSendRate()==0?0:stats.getSmoothedDataSendRate();
					
				if ( up_speed <= 0 ){
					
					next_eta = -2;
					
				}else{
					
					if ( dm_state == DownloadManager.STATE_SEEDING ){
					
							// simple case
						
						long	target_upload = ( next_target_sr * downloaded ) / 1000;
						
						next_eta = ( target_upload - uploaded )/up_speed;
					
					}else{
							// more complex when downloading as we have to consider the fact that
							// at some point the download will complete and therefore download speed will
							// drop to 0
						
						DiskManager disk_man = dm.getDiskManager();
						
						if ( disk_man != null ){
			
							long	remaining = disk_man.getRemainingExcludingDND();
							
							long	down_speed	= (dm_state==DownloadManager.STATE_SEEDING||stats.getDataReceiveRate()==0)?0:stats.getSmoothedDataReceiveRate();
	
							if ( down_speed <= 0 || remaining <= 0 ){
								
									// same as if we are just seeding
								
								long	target_upload = ( next_target_sr * downloaded ) / 1000;
								
								next_eta = ( target_upload - uploaded )/up_speed;

							}else{
																
								/*
								 	time T until the target share ration is met is
								 	
								  		uploaded + ( T * upload_speed )
										------------------------------          = target_sr
										downloaded + ( T * download_speed )
								*/
								
								long time_to_sr = (( next_target_sr * downloaded )/1000 - uploaded ) / ( up_speed - ( down_speed * next_target_sr )/1000 );
																	
								long time_to_completion = remaining / down_speed;
									
								if ( time_to_sr > 0 &&  time_to_sr <= time_to_completion ){
										
									next_eta = time_to_sr;
									
								}else{
									
										// basic calculation shows eta is > download complete time so we need
										// to refactor things
									
									long uploaded_at_completion 	= uploaded + ( up_speed * time_to_completion );
									long downloaded_at_completion 	= downloaded + ( down_speed * time_to_completion );
									
										// usual seeding calculation for time after completion
									
									long	target_upload = ( next_target_sr * downloaded_at_completion ) / 1000;
									
									next_eta = time_to_completion + ( target_upload - uploaded_at_completion )/up_speed;
								}
							}
						}else{
							
							next_eta = -2;
						}
					}
				}
			}
		}
		
		long data = dm.getDownloadState().getLongAttribute( DownloadManagerState.AT_SHARE_RATIO_PROGRESS );

						
		long		sr 		= (int)data;
		
		String		sr_str 	= DisplayFormatters.formatDecimal((double) sr / 1000, 3);
		
		timestamp = (data>>>32)*1000;

			// feed a bit of share ratio/next eta into sort order for fun and to ensure refresh occurs when they change
		
		long	sort_order = timestamp;
		
		sort_order += (sr&0xff)<<8;
		
		sort_order += (next_eta&0xff );
		
		String next_eta_str;
		
		if ( next_eta == -1 ){
			next_eta_str = "";
		}else if ( next_eta == -2 ){
			next_eta_str = Constants.INFINITY_STRING + ": ";
		}else{
			next_eta_str = DisplayFormatters.formatETA(next_eta) + ": ";
		}
		
		String prefix = next_eta_str + sr_str + ( timestamp>0?": ":"" );
						
		super.refresh( cell, timestamp, sort_order, prefix );
	
	}
}
