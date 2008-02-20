/**
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.ui.swt.views.skin;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;

import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.common.table.TableSelectionListener;
import com.aelitis.azureus.ui.swt.columns.vuzeactivity.ColumnVuzeActivity;
import com.aelitis.azureus.ui.swt.skin.SWTSkin;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.views.list.ListView;
import com.aelitis.azureus.util.VuzeActivitiesEntry;
import com.aelitis.azureus.util.VuzeActivitiesListener;
import com.aelitis.azureus.util.VuzeActivitiesManager;

/**
 * @author TuxPaper
 * @created Jan 28, 2008
 *
 */
public class VuzeActivitiesView
	extends SkinView
	implements VuzeActivitiesListener
{
	private static final long SHIFT_FREQUENCY = 1000L * 60 * 10;

	private static final long ONE_WEEK_MS = 7 * 3600 * 24 * 1000L;

	private static final boolean TEST_ENTRIES = false;

	private static final String PREFIX = "vuzeevents-";

	private static String TABLE_ID = "VuzeActivity";

	private ListView view;

	private ArrayList headerEntries = new ArrayList();

	private boolean skipShift;

	private SWTSkinButtonUtility btnAdd;

	private SWTSkinButtonUtility btnStop;

	private SWTSkinButtonUtility btnDelete;

	private SWTSkinButtonUtility btnDetails;

	private SWTSkinButtonUtility btnPlay;

	private SWTSkinButtonUtility btnShare;

	private SWTSkinButtonUtility btnComments;

	private SWTSkinButtonUtility btnColumnSetup;

	private long lastShiftedOn;

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#showSupport(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object showSupport(SWTSkinObject skinObject, Object params) {
		final SWTSkin skin = skinObject.getSkin();
		Composite cData = (Composite) skinObject.getControl();
		view = new ListView(TABLE_ID, skin.getSkinProperties(), cData, null,
				SWT.V_SCROLL);
		view.setRowMarginHeight(1);

		skipShift = true;

		view.addKeyListener(new KeyListener() {
			public void keyReleased(KeyEvent e) {
			}

			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.DEL) {
					removeSelected();
				} else if (e.keyCode == SWT.F5) {
					if ((e.stateMask & SWT.SHIFT) > 0) {
						VuzeActivitiesManager.resetRemovedEntries();
					}
					if ((e.stateMask & SWT.CONTROL) > 0) {
						System.out.println("pull all vuze news entries");
						VuzeActivitiesManager.pullActivitiesNow(
								VuzeActivitiesManager.MAX_LIFE_MS, 0);
					} else {
						System.out.println("pull latest vuze news entries");
						VuzeActivitiesManager.pullActivitiesNow(0);
					}
				}
			}
		});

		view.addSelectionListener(new TableSelectionListener() {

			public void selected(TableRowCore[] row) {
			}

			public void focusChanged(TableRowCore focus) {
			}

			public void deselected(TableRowCore[] rows) {
			}

			public void defaultSelected(TableRowCore[] rows) {
				if (rows.length == 1) {
					TorrentListViewsUtils.playOrStreamDataSource(rows[0].getDataSource(),
							btnPlay);
				}
			}
		}, false);

		cData.layout();

		final ColumnVuzeActivity columnVuzeActivity = new ColumnVuzeActivity(
				TABLE_ID);
		TableColumnCore[] columns = {
			columnVuzeActivity
		};
		view.setColumnList(columns, "name", false, false);

		VuzeActivitiesManager.addListener(this);

		view.addDataSources(VuzeActivitiesManager.getAllEntries());

		btnColumnSetup = TorrentListViewsUtils.addColumnSetupButton(skin, PREFIX,
				view);

		btnShare = TorrentListViewsUtils.addShareButton(skin, PREFIX, view);
		btnDetails = TorrentListViewsUtils.addDetailsButton(skin, PREFIX, view);
		btnComments = TorrentListViewsUtils.addCommentsButton(skin, PREFIX, view);
		btnPlay = TorrentListViewsUtils.addPlayButton(skin, PREFIX, view, false,
				true);

		skinObject = skin.getSkinObject(PREFIX + "delete");
		if (skinObject instanceof SWTSkinObject) {
			btnDelete = new SWTSkinButtonUtility(skinObject);

			btnDelete.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					removeSelected();
				}
			});
		}

		SWTSkinButtonUtility[] buttonsNeedingRow = {
			btnDelete,
			btnStop,
		};
		SWTSkinButtonUtility[] buttonsNeedingPlatform = {
			btnDetails,
			btnComments,
			btnShare,
		};
		SWTSkinButtonUtility[] buttonsNeedingSingleSelection = {
			btnDetails,
			btnComments,
			btnShare,
		};
		TorrentListViewsUtils.addButtonSelectionDisabler(view, buttonsNeedingRow,
				buttonsNeedingPlatform, buttonsNeedingSingleSelection, btnStop);

		VuzeActivitiesEntry headerEntry;
		headerEntry = new VuzeActivitiesEntry(0, 0, "This Week", null, null);
		headerEntries.add(headerEntry);

		headerEntry = new VuzeActivitiesEntry(0, 0, "Last Week", null, null);
		headerEntries.add(headerEntry);

		headerEntry = new VuzeActivitiesEntry(0, 0, "2 Weeks Ago", null, null);
		headerEntries.add(headerEntry);

		headerEntry = new VuzeActivitiesEntry(0, 0, "3 Weeks Ago", null, null);
		headerEntries.add(headerEntry);

		headerEntry = new VuzeActivitiesEntry(0, 0, "4 Weeks Ago", null, null);
		headerEntries.add(headerEntry);

		VuzeActivitiesManager.addEntries((VuzeActivitiesEntry[]) headerEntries.toArray(new VuzeActivitiesEntry[headerEntries.size()]));

		if (TEST_ENTRIES) {
			String[] testIDs = {
				"column.azproduct.product",
				"column.azproduct.globe",
				"icon.rate.wait"
			};

			for (int i = 0; i < 100; i++) {
				long timestamp = System.currentTimeMillis()
						- (int) (Math.random() * (40 * 3600 * 24 * 1000.0));
				VuzeActivitiesManager.addEntries(new VuzeActivitiesEntry[] {
					new VuzeActivitiesEntry(timestamp, 1, i + " blah blah\non "
							+ DisplayFormatters.formatTimeStamp(timestamp),
							testIDs[(int) (Math.random() * 3)], "" + timestamp)
				});
			}

			VuzeActivitiesManager.addEntries(new VuzeActivitiesEntry[] {
				new VuzeActivitiesEntry(SystemTime.getOffsetTime(-ONE_WEEK_MS + 1000),
						1, "Just under one week", testIDs[(int) (Math.random() * 3)], null)
			});

			VuzeActivitiesManager.addEntries(new VuzeActivitiesEntry[] {
				new VuzeActivitiesEntry(
						SystemTime.getOffsetTime(-3300),
						1,
						"This is an <A HREF=\"http://vuze.com/details/3833.html\">url test</a>. Good luck",
						null, null)
			});
		}

		SimpleTimer.addPeriodicEvent("ShiftVuzeNews", SHIFT_FREQUENCY,
				new TimerEventPerformer() {
					public void perform(TimerEvent event) {
						shiftVuzeNews();
					}
				});

		skipShift = false;
		shiftVuzeNews();

		return null;
	}

	/**
	 * 
	 *
	 * @since 3.0.4.3
	 */
	protected void removeSelected() {
		try {
			Object[] selectedDataSources = view.getSelectedDataSources();
			for (int i = 0; i < selectedDataSources.length; i++) {
				if (selectedDataSources[i] instanceof VuzeActivitiesEntry) {
					VuzeActivitiesEntry entry = (VuzeActivitiesEntry) selectedDataSources[i];
					MessageBoxShell mb = new MessageBoxShell(Utils.findAnyShell(),
							MessageText.getString("v3.activity.remove.title"),
							MessageText.getString("v3.activity.remove.text", new String[] {
								entry.text
							}), new String[] {
								MessageText.getString("Button.yes"),
								MessageText.getString("Button.no")
							}, 0, TABLE_ID + "-Remove",
							MessageText.getString("MessageBoxWindow.nomoreprompting"), false,
							0);
					mb.setRememberOnlyIfButton(0);
					if (mb.open() == 0) {
						VuzeActivitiesManager.removeEntries(new VuzeActivitiesEntry[] {
							entry
						});
					}
				}
			}
		} catch (Exception e) {
			Debug.out(e);
		}
	}

	/**
	 * 
	 *
	 * @since 3.0.4.3
	 */
	private void shiftVuzeNews() {
		if (skipShift) {
			return;
		}
		lastShiftedOn = System.currentTimeMillis();
		for (Iterator iter = headerEntries.iterator(); iter.hasNext();) {
			VuzeActivitiesEntry entry = (VuzeActivitiesEntry) iter.next();
			entry.setTimestamp(lastShiftedOn);
			lastShiftedOn -= ONE_WEEK_MS;
		}
		view.refreshTable(true);
	}

	// @see com.aelitis.azureus.util.VuzeNewsListener#vuzeNewsEntriesAdded(com.aelitis.azureus.util.VuzeNewsEntry[])
	public void vuzeNewsEntriesAdded(VuzeActivitiesEntry[] entries) {
		view.addDataSources(entries);
		long newest = 0;
		for (int i = 0; i < entries.length; i++) {
			VuzeActivitiesEntry entry = entries[i];
			if (entry.getTimestamp() > newest) {
				newest = entry.getTimestamp();
			}
		}
		if (newest > lastShiftedOn) {
			shiftVuzeNews();
		}
	}

	// @see com.aelitis.azureus.util.VuzeNewsListener#vuzeNewsEntriesRemoved(com.aelitis.azureus.util.VuzeNewsEntry[])
	public void vuzeNewsEntriesRemoved(VuzeActivitiesEntry[] entries) {
		view.removeDataSources(entries);
	}

	// @see com.aelitis.azureus.util.VuzeActivitiesListener#vuzeNewsEntryChanged(com.aelitis.azureus.util.VuzeActivitiesEntry)
	public void vuzeNewsEntryChanged(VuzeActivitiesEntry entry) {
		TableRowCore row = view.getRow(entry);
		if (row != null) {
			row.invalidate();
		}
	}
}
