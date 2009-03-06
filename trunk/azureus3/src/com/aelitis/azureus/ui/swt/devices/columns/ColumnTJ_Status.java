/**
 * Created on Feb 26, 2009
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
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

package com.aelitis.azureus.ui.swt.devices.columns;

import java.util.Locale;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.internat.MessageText.MessageTextListener;

import com.aelitis.azureus.core.devices.TranscodeFile;
import com.aelitis.azureus.core.devices.TranscodeJob;

import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Feb 26, 2009
 *
 */
public class ColumnTJ_Status
	implements TableCellRefreshListener
{
	public static final String COLUMN_ID = "trascode_status";

	private static final String[] js_resource_keys = {
		"ManagerItem.queued",
		"DHTView.activity.status.false",
		"ManagerItem.paused",
		"sidebar.LibraryCD",
		"Progress.reporting.status.canceled",
		"ManagerItem.error",
		"ManagerItem.stopped",
		"devices.copy.fail",		// 7
		"devices.on.demand",		// 8 
		"devices.ready"				// 9
	};

	private static String[] js_resources;

	public ColumnTJ_Status(final TableColumn column) {
		column.initialize(TableColumn.ALIGN_LEAD, TableColumn.POSITION_LAST, 120);
		column.addListeners(this);
		column.setRefreshInterval(TableColumn.INTERVAL_INVALID_ONLY);
		column.setType(TableColumn.TYPE_TEXT_ONLY);
		column.setMinWidth(100);

		MessageText.addAndFireListener(new MessageTextListener() {
			public void localeChanged(Locale old_locale, Locale new_locale) {
				js_resources = new String[js_resource_keys.length];

				for (int i = 0; i < js_resources.length; i++) {
					js_resources[i] = MessageText.getString(js_resource_keys[i]);
				}
				column.invalidateCells();
			}
		});
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener#refresh(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void refresh(TableCell cell) {
		TranscodeFile tf = (TranscodeFile) cell.getDataSource();
		if (tf == null) {
			return;
		}
		TranscodeJob job = tf.getJob();
		if (job == null) {
			if ( tf.getCopyToDeviceFails() > 0 ){
					// should be red but whatever
				cell.setText( js_resources[7] );
			}else if ( tf.isTemplate() && !tf.isComplete()){
				
				cell.setText( js_resources[8] );
			}else{
				
				cell.setText( js_resources[9] );
			}
			return;
		}
		cell.setText(getJobStatus(job));
	}

	protected String getJobStatus(TranscodeJob job) {
		int state = job.getState();

		String res = js_resources[state];

		if (state == TranscodeJob.ST_FAILED) {

			// should be red but whatever
			
			res += ": " + job.getError();
		}

		return (res);
	}
}
