/*
 * File    : PeersItem.java
 * Created : 24 nov. 2003
 * By      : Olivier
 *
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
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
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

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
public class PeersItem extends CoreTableColumn implements
		TableCellAddedListener
{
	/** Default Constructor */
	public PeersItem(String sTableID) {
		super("peers", ALIGN_CENTER, POSITION_LAST, 60, sTableID);
		setRefreshInterval(INTERVAL_LIVE);
	}

	public void cellAdded(TableCell cell) {
		new Cell(cell);
	}

	private class Cell extends AbstractTrackerCell {
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

			long lConnectedPeers = 0;
			if (dm != null) {
				lConnectedPeers = dm.getNbPeers();

				if (lTotalPeers == -1)
					scrapeResult(dm.getTrackerScrapeResponse());
			}

			long value = lConnectedPeers * 10000000;
			if (lTotalPeers > 0)
				value = value + lTotalPeers;
			if (!cell.setSortValue(value) && cell.isValid())
				return;

			String tmp = String.valueOf(lConnectedPeers);
			if (lTotalPeers != -1)
				tmp += " (" + lTotalPeers + ")";

			cell.setText(tmp);
		}

		public void cellHover(TableCell cell) {
			super.cellHover(cell);

			long lConnectedPeers = 0;
			DownloadManager dm = (DownloadManager) cell.getDataSource();
			if (dm != null) {
				lConnectedPeers = dm.getNbPeers();
			}

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
			cell.setToolTip(sToolTip);
		}
	}
}
