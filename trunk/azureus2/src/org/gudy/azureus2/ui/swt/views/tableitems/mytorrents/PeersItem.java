/*
 * File    : PeersItem.java
 * Created : 24 nov. 2003
 * By      : Olivier
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

import java.util.Locale;

import org.eclipse.swt.graphics.Image;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.internat.MessageText.MessageTextListener;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemFillListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellMouseEvent;
import org.gudy.azureus2.plugins.ui.tables.TableCellMouseListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumnInfo;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.table.CoreTableColumnSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;

import com.aelitis.azureus.plugins.tracker.dht.DHTTrackerPlugin;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

/** # of Peers
 * 
 * A new object is created for each cell, so that we can listen to the
 * scrapes and update individually (and only when needed).
 * 
 * Total connected peers are left to update on INTERVAL_LIVE, as they aren't
 * very expensive.  It would probably be more expensive to hook the
 * peer listener and only refresh on peer added/removed, because that happens
 * frequently.
 *
 * @author Olivier
 * @author TuxPaper
 * 		2004/Apr/17: modified to TableCellAdapter
 * 		2005/Oct/13: Use listener to update total from scrape
 */
public class PeersItem extends CoreTableColumnSWT implements
		TableCellAddedListener, ParameterListener
{
	public static final Class DATASOURCE_TYPE = Download.class;

	public static final String COLUMN_ID = "peers";

	private static final String CFG_SHOW_ICON 		= "PeersColumn.showNetworkIcon";

	private static String textStarted;
	private static String textStartedOver;
	private static String textNotStarted;
	private static String textStartedNoScrape;
	private static String textNotStartedNoScrape;

	private static Image i2p_img;
	private static Image none_img;

	static {
		MessageText.addAndFireListener(new MessageTextListener() {
			public void localeChanged(Locale old_locale, Locale new_locale) {
				textStarted = MessageText.getString("Column.seedspeers.started");
				textStartedOver = MessageText.getString("Column.seedspeers.started.over");
				textNotStarted = MessageText.getString("Column.seedspeers.notstarted");
				textStartedNoScrape = MessageText.getString("Column.seedspeers.started.noscrape");
				textNotStartedNoScrape = MessageText.getString("Column.seedspeers.notstarted.noscrape");
			}
		});
		
		ImageLoader imageLoader = ImageLoader.getInstance();
		
		i2p_img 	= imageLoader.getImage("net_I2P_x");
		none_img 	= imageLoader.getImage("net_None_x");
	}

	private boolean showIcon;

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] { CAT_SWARM });
	}

	/** Default Constructor */
	public PeersItem(String sTableID) {
		super(DATASOURCE_TYPE, COLUMN_ID, ALIGN_CENTER, 60, sTableID);
		setRefreshInterval(INTERVAL_LIVE);
		setMinWidthAuto(true);
		
		showIcon	 = COConfigurationManager.getBooleanParameter(CFG_SHOW_ICON);
		
		COConfigurationManager.addParameterListener(CFG_SHOW_ICON, this);
		// show icon menu
		
		TableContextMenuItem menuShowIcon = addContextMenuItem(
				"ConfigView.section.style.showNetworksIcon", MENU_STYLE_HEADER);
		menuShowIcon.setStyle(TableContextMenuItem.STYLE_CHECK);
		menuShowIcon.addFillListener(new MenuItemFillListener() {
			public void menuWillBeShown(MenuItem menu, Object data) {
				menu.setData(Boolean.valueOf(showIcon));
			}
		});

		menuShowIcon.addMultiListener(new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				COConfigurationManager.setParameter(CFG_SHOW_ICON,
						((Boolean) menu.getData()).booleanValue());
			}
		});
	}

	public void reset() {
		super.reset();
		
		COConfigurationManager.removeParameter( CFG_SHOW_ICON );
	}
	
	protected void finalize() throws Throwable {
		super.finalize();
		COConfigurationManager.removeParameterListener(CFG_SHOW_ICON, this);
	}
	public void cellAdded(TableCell cell) {
		new Cell(cell);
	}

	public void parameterChanged(String parameterName) {
		setShowIcon( COConfigurationManager.getBooleanParameter(CFG_SHOW_ICON));
	}
	
	public void setShowIcon(boolean b) {
		showIcon = b;
		invalidateCells();
	}
	
	private class Cell 
		extends AbstractTrackerCell
		implements TableCellMouseListener
	{
		long lTotalPeers = -1;
		
		/**
		 * Initialize
		 * 
		 * @param cell
		 */
		public Cell(TableCell cell) {
			super(cell);
		}

		public void scrapeResult(TRTrackerScraperResponse response) {
			if (checkScrapeResult(response)) {
				lTotalPeers = response.getPeers();
			}
		}

		public void refresh(TableCell cell) {
			super.refresh(cell);
			DownloadManager dm = (DownloadManager) cell.getDataSource();
			
			long lConnectedPeers = 0;
			if (dm != null) {
				lConnectedPeers = dm.getNbPeers();

				if (lTotalPeers == -1) {
					TRTrackerScraperResponse response = dm.getTrackerScrapeResponse();
					if (response != null && response.isValid()) {
						lTotalPeers = response.getPeers();
					}
				}
				
				if (cell instanceof TableCellSWT) {

					int[] i2p_info = (int[])dm.getUserData( DHTTrackerPlugin.DOWNLOAD_USER_DATA_I2P_SCRAPE_KEY );
				
					Image icon = none_img;
				
					if ( showIcon && i2p_info != null ){
					
						int totalI2PLeechers = i2p_info[1];
						
						if ( totalI2PLeechers > 0 ){
							
							icon = i2p_img;
						}
					}
					
					((TableCellSWT)cell).setIcon( icon );
				}
			}
			
			long totalPeers = lTotalPeers;
			if (totalPeers <= 0) {
				if (dm != null) {
					totalPeers = dm.getActivationCount();
				}
			}

			long value = lConnectedPeers * 10000000;
			if (totalPeers > 0)
				value = value + totalPeers;
			
			String text;
			
			if ( dm != null ){
				int state = dm.getState();
				boolean started = state == DownloadManager.STATE_SEEDING
						|| state == DownloadManager.STATE_DOWNLOADING;
				boolean hasScrape = lTotalPeers >= 0;
	
				if (started) {
					text = hasScrape ? (lConnectedPeers > lTotalPeers ? textStartedOver
							: textStarted) : textStartedNoScrape;
				} else {
					text = hasScrape ? textNotStarted : textNotStartedNoScrape;
				}
				
				if ( text.length() == 0 ){
					if ( text.length() == 0 ){
						
						value = Integer.MIN_VALUE;
						
						long cache = dm.getDownloadState().getLongAttribute( DownloadManagerState.AT_SCRAPE_CACHE );
						
						if ( cache != -1 ){
							
							int leechers 	= (int)(cache&0x00ffffff);

							value += leechers+1;
						}
					}				}
				if (!cell.setSortValue(value) && cell.isValid()){
					// we have an accurate value now, bail if no change
					return;
				}
				
				text = text.replaceAll("%1", String.valueOf(lConnectedPeers));
				text = text.replaceAll("%2", String.valueOf(totalPeers));
				
			}else{
				text	= "";
				value	= Integer.MIN_VALUE;
				
				if (!cell.setSortValue(value) && cell.isValid()){
					return;
				}
			}
			
			cell.setText( text );
		}

		public void cellHover(TableCell cell) {
			super.cellHover(cell);

			long lConnectedPeers = 0;
			DownloadManager dm = (DownloadManager) cell.getDataSource();
			if (dm != null) {
				lConnectedPeers = dm.getNbPeers();
		
				String sToolTip = lConnectedPeers + " "
						+ MessageText.getString("GeneralView.label.connected") + "\n";
				if (lTotalPeers != -1) {
					sToolTip += lTotalPeers + " "
							+ MessageText.getString("GeneralView.label.in_swarm");
				} else {
					TRTrackerScraperResponse response = dm.getTrackerScrapeResponse();
					sToolTip += "?? " + MessageText.getString("GeneralView.label.in_swarm");
					if (response != null)
						sToolTip += "(" + response.getStatusString() + ")";
				}
				
				int activationCount = dm.getActivationCount();
				if (activationCount > 0) {
					sToolTip += "\n"
							+ MessageText.getString("PeerColumn.activationCount",
									new String[] { "" + activationCount });
				}
				
				long cache = dm.getDownloadState().getLongAttribute( DownloadManagerState.AT_SCRAPE_CACHE );
				
				if ( cache != -1 ){
					
					int leechers 	= (int)(cache&0x00ffffff);
				
					if ( leechers != lTotalPeers ){
						sToolTip += "\n" + leechers + " " + MessageText.getString( "Scrape.status.cached" ).toLowerCase( Locale.US );
					}
				}
				
				int[] i2p_info = (int[])dm.getUserData( DHTTrackerPlugin.DOWNLOAD_USER_DATA_I2P_SCRAPE_KEY );
				
				if ( i2p_info != null ){
				
					int totalI2PPeers = i2p_info[1];
					
					if ( totalI2PPeers > 0 ){
						
						sToolTip += "\n" + 
								MessageText.getString(
									"TableColumn.header.peers.i2p",
									new String[]{ String.valueOf( totalI2PPeers )});
					}
				}
				cell.setToolTip(sToolTip);
			}else{
				cell.setToolTip("");
			}
		}
		
		  public void cellMouseTrigger(TableCellMouseEvent event) {
				DownloadManager dm = (DownloadManager) event.cell.getDataSource();
				if (dm == null) {return;}
				
				if (event.eventType != TableCellMouseEvent.EVENT_MOUSEDOUBLECLICK) {return;}
				
				event.skipCoreFunctionality = true;
				
				
				int[] i2p_info = (int[])dm.getUserData( DHTTrackerPlugin.DOWNLOAD_USER_DATA_I2P_SCRAPE_KEY );
				
				if ( i2p_info != null && i2p_info[1] > 0 ){
					Utils.launch(MessageText.getString( "privacy.view.wiki.url" ));
				}
		  }
	}
}
