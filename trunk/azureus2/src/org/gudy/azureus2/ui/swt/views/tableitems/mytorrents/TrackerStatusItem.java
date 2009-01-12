/*
 * File    : TrackerStatusItem.java
 * Created : 24 nov. 2003
 * By      : Olivier
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

import org.eclipse.swt.SWT;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncer;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerResponse;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellAddedListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellMouseEvent;
import org.gudy.azureus2.plugins.ui.tables.TableCellMouseMoveListener;
import org.gudy.azureus2.plugins.ui.tables.TableCellToolTipListener;

import com.aelitis.azureus.core.util.AZ3Functions;

/**
 * @author Olivier
 *
 */
public class TrackerStatusItem extends CoreTableColumn implements
		TableCellAddedListener, TableCellToolTipListener, TableCellMouseMoveListener
{
	public static final String COLUMN_ID = "tracker";

	public TrackerStatusItem(String sTableID) {
		super(COLUMN_ID, POSITION_LAST, 90, sTableID);
		setRefreshInterval(15); // Slow update while no responses from tracker
	}

	public void cellAdded(TableCell cell) {
		new Cell(cell);
	}

	public void 
	cellMouseTrigger(
		TableCellMouseEvent event )
	{
		Object ds = event.cell.getDataSource();
		
		if ( !( ds instanceof DownloadManager )){
			
			return;
		}
		
		AZ3Functions.provider az3 = AZ3Functions.getProvider();
		
		if ( az3 == null ){
			
			return;
		}
		
		boolean invalidateAndRefresh = false;
		
		DownloadManager dm = (DownloadManager)ds;
		
		TRTrackerAnnouncer announcer = dm.getTrackerClient();

		String	status_str = null;
		
		if ( announcer != null ){
			
			TRTrackerAnnouncerResponse resp = announcer.getLastResponse();
			
			if ( resp != null ){
				
				if ( resp.getStatus() == TRTrackerAnnouncerResponse.ST_REPORTED_ERROR ){
					
					status_str = resp.getStatusString();
				}
			}
		}else{
			
			TRTrackerScraperResponse resp = dm.getTrackerScrapeResponse();
			
			if ( resp != null ){
				
				if ( resp.getStatus() == TRTrackerScraperResponse.ST_ERROR ){
					
					status_str = resp.getStatusString();
				}
			}
		}
		
		int newCursor = SWT.CURSOR_ARROW;
		
		if ( status_str != null ){
			
			status_str = status_str.toLowerCase();
			
			if ( 	status_str.contains( "not authorised" ) ||
					status_str.contains( "not authorized" )){
				
				newCursor = SWT.CURSOR_HAND;
			}
		}

		int oldCursor = ((TableCellSWT) event.cell).getCursorID();
		if (oldCursor != newCursor) {
			invalidateAndRefresh = true;
			((TableCellSWT) event.cell).setCursorID(newCursor);
		}
		
		if ( event.eventType == TableCellMouseEvent.EVENT_MOUSEUP ){
			
			if ( newCursor == SWT.CURSOR_HAND ){
				
				az3.showCDP( dm, "tracker.unauth" );
			}
		}
		
		if (invalidateAndRefresh) {
			event.cell.invalidate();
			((TableCellSWT)event.cell).redraw();
		}
	}
	private class Cell extends AbstractTrackerCell {
		public Cell(TableCell cell) {
			super(cell);
		}

		public void refresh(TableCell cell) {
			super.refresh(cell);
			
			String status = dm == null ? "" : dm.getTrackerStatus();

			// status sometimes contains multiline text (e.g. HTML) on failure
			// - trim to end of first line break if present (see bug 1337563)

			int nl_pos = status.indexOf('\n');
			if (nl_pos >= 0)
				status = status.substring(0, nl_pos);

	    if (cell.setText(status) || !cell.isValid()) {
	    	TrackerCellUtils.updateColor(cell, dm);
	    }
		}

		public void scrapeResult(TRTrackerScraperResponse response) {
			checkScrapeResult(response);
		}

		public void announceResult(TRTrackerAnnouncerResponse response) {
			cell.invalidate();
		}
	}

	public void cellHover(TableCell cell) {
		DownloadManager dm = (DownloadManager) cell.getDataSource();
		cell.setToolTip(TrackerCellUtils.getTooltipText(cell, dm));
	}

	public void cellHoverComplete(TableCell cell) {
		cell.setToolTip(null);
	}
}
