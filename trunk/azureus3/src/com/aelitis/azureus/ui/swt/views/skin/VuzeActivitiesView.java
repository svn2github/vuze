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

import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.shells.InputShell;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.activities.VuzeActivitiesListener;
import com.aelitis.azureus.activities.VuzeActivitiesManager;
import com.aelitis.azureus.buddy.VuzeBuddy;
import com.aelitis.azureus.buddy.impl.VuzeBuddyManager;
import com.aelitis.azureus.core.messenger.config.PlatformRelayMessenger;
import com.aelitis.azureus.ui.common.RememberedDecisionsManager;
import com.aelitis.azureus.ui.common.table.*;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.columns.vuzeactivity.ColumnVuzeActivity;
import com.aelitis.azureus.ui.swt.skin.SWTSkin;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.views.list.ListView;
import com.aelitis.azureus.util.*;

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

	private static final long ONE_DAY_MS = 3600 * 24 * 1000L;

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
				} else if (e.keyCode == SWT.F11) {
					PlatformRelayMessenger.fetch(0);
				} else if (e.keyCode == SWT.F12) {
					//VuzeActivitiesEntryBuddyRequest entry = new VuzeActivitiesEntryBuddyRequest(
					//		"ArronM", "TuxPaper");
					//VuzeActivitiesManager.addEntries(new VuzeActivitiesEntry[] {
					//	entry
					//});
					InputShell is = new InputShell("Moo", "Message:");
					String txt = is.open();
					if (txt != null) {
						VuzeActivitiesEntry entry = new VuzeActivitiesEntry(
								SystemTime.getCurrentTime(), txt, "Test");
						List buddies = VuzeBuddyManager.getAllVuzeBuddies();
						for (Iterator iter = buddies.iterator(); iter.hasNext();) {
							VuzeBuddy buddy = (VuzeBuddy) iter.next();
							if (buddy.getDisplayName().toLowerCase().indexOf("tux") >= 0) {
								System.out.println("sedning to " + buddy.getDisplayName());
								buddy.sendActivity(entry);
							}
						}
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

			view.addSelectionListener(new TableSelectionAdapter() {
				public void deselected(TableRowCore[] rows) {
					update();
				}

				public void selected(TableRowCore[] rows) {
					update();
				}

				public void focusChanged(TableRowCore focusedRow) {
					update();
				}

				private void update() {
					Object[] selectedDataSources = view.getSelectedDataSources();
					if (selectedDataSources.length > 0) {
						boolean disable = true;
						for (int i = 0; i < selectedDataSources.length; i++) {
							if (selectedDataSources[i] instanceof VuzeActivitiesEntry) {
								VuzeActivitiesEntry entry = (VuzeActivitiesEntry) selectedDataSources[i];
								boolean isHeader = VuzeActivitiesEntry.TYPEID_HEADER.equals(entry.getTypeID());
								if (!isHeader) {
									disable = false;
									break;
								}
							}
						}
						btnDelete.setDisabled(disable);
					}
				}
			}, false);
		}

		SWTSkinButtonUtility[] buttonsNeedingRow = {
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
		headerEntry = new VuzeActivitiesEntry(0,
				MessageText.getString("v3.activity.header.today"),
				VuzeActivitiesEntry.TYPEID_HEADER);
		headerEntries.add(headerEntry);

		headerEntry = new VuzeActivitiesEntry(0,
				MessageText.getString("v3.activity.header.yesterday"),
				VuzeActivitiesEntry.TYPEID_HEADER);
		headerEntries.add(headerEntry);

		for (int i = 2; i < 7; i++) {
			headerEntry = new VuzeActivitiesEntry(0, MessageText.getString(
					"v3.activity.header.xdaysago", new String[] {
						"" + i
					}), VuzeActivitiesEntry.TYPEID_HEADER);
			headerEntries.add(headerEntry);
		}

		headerEntry = new VuzeActivitiesEntry(0,
				MessageText.getString("v3.activity.header.1weekago"),
				VuzeActivitiesEntry.TYPEID_HEADER);
		headerEntries.add(headerEntry);

		for (int i = 2; i < 5; i++) {
			headerEntry = new VuzeActivitiesEntry(0, MessageText.getString(
					"v3.activity.header.xweeksago", new String[] {
						"" + i
					}), VuzeActivitiesEntry.TYPEID_HEADER);
			headerEntries.add(headerEntry);
		}

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
					new VuzeActivitiesEntry(timestamp, i + " blah blah\non "
							+ DisplayFormatters.formatTimeStamp(timestamp),
							testIDs[(int) (Math.random() * 3)], "" + timestamp, null, null)
				});
			}

			VuzeActivitiesManager.addEntries(new VuzeActivitiesEntry[] {
				new VuzeActivitiesEntry(SystemTime.getOffsetTime(-ONE_WEEK_MS + 1000),
						"Just under one week", testIDs[(int) (Math.random() * 3)], null,
						null, null)
			});

			VuzeActivitiesManager.addEntries(new VuzeActivitiesEntry[] {
				new VuzeActivitiesEntry(
						SystemTime.getOffsetTime(-3300),
						"This is an <A HREF=\"http://vuze.com/details/3833.html\">url test</a>. Good luck",
						null, null, null, null)
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
		shiftVuzeNews();
		Shell shell = view.getComposite().getShell();
		Cursor oldCursor = shell.getCursor();
		try {
			Object[] selectedDataSources = view.getSelectedDataSources();
			VuzeActivitiesEntry[] entriesToRemove = new VuzeActivitiesEntry[selectedDataSources.length];
			int entriesToRemovePos = 0;

			shell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));
			int rememberedDecision = RememberedDecisionsManager.getRememberedDecision(TABLE_ID
					+ "-Remove");
			if (rememberedDecision == 0) {
				try {
					for (int i = 0; i < selectedDataSources.length; i++) {
						if (selectedDataSources[i] instanceof VuzeActivitiesEntry) {
							VuzeActivitiesEntry entry = (VuzeActivitiesEntry) selectedDataSources[i];
							boolean isHeader = VuzeActivitiesEntry.TYPEID_HEADER.equals(entry.getTypeID());
							if (isHeader) {
								continue;
							}

							entriesToRemove[entriesToRemovePos++] = entry;
						}
					}
				} catch (Exception e) {
					Debug.out(e);
				}
			} else {
				try {
					for (int i = 0; i < selectedDataSources.length; i++) {
						if (selectedDataSources[i] instanceof VuzeActivitiesEntry) {
							VuzeActivitiesEntry entry = (VuzeActivitiesEntry) selectedDataSources[i];
							boolean isHeader = VuzeActivitiesEntry.TYPEID_HEADER.equals(entry.getTypeID());
							if (isHeader) {
								continue;
							}

							MessageBoxShell mb = new MessageBoxShell(Utils.findAnyShell(),
									MessageText.getString("v3.activity.remove.title"),
									MessageText.getString("v3.activity.remove.text",
											new String[] {
												entry.getText()
											}), new String[] {
										MessageText.getString("Button.yes"),
										MessageText.getString("Button.no")
									}, 0, TABLE_ID + "-Remove",
									MessageText.getString("MessageBoxWindow.nomoreprompting"),
									false, 0);
							mb.setRememberOnlyIfButton(0);
							int result = mb.open();
							if (result == 0) {
								entriesToRemove[entriesToRemovePos++] = entry;
							} else if (result == -1) {
								break;
							}
						}
					}
				} catch (Exception e) {
					Debug.out(e);
				}
			}

			if (entriesToRemovePos > 0) {
				VuzeActivitiesManager.removeEntries(entriesToRemove);
			}
		} catch (Exception e) {
			Debug.out(e);
		} finally {
			shell.setCursor(oldCursor);
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
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());
		cal.roll(Calendar.DATE, true);
		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);

		lastShiftedOn = cal.getTimeInMillis();
		int i = 0;
		for (Iterator iter = headerEntries.iterator(); iter.hasNext();) {
			VuzeActivitiesEntry entry = (VuzeActivitiesEntry) iter.next();
			entry.setTimestamp(lastShiftedOn);
			if (i < 7) {
				lastShiftedOn -= ONE_DAY_MS;
			} else {
				lastShiftedOn -= ONE_WEEK_MS;
			}
		}

		VuzeActivitiesEntry[] allEntries = VuzeActivitiesManager.getAllEntries();
		Arrays.sort(allEntries);
		boolean lastWasHeader = false;
		VuzeActivitiesEntry lastEntry = null;
		for (int j = allEntries.length - 1; j >= 0; j--) {
			VuzeActivitiesEntry entry = allEntries[j];
			boolean isHeader = VuzeActivitiesEntry.TYPEID_HEADER.equals(entry.getTypeID());
			if (lastWasHeader && lastEntry != null) {
				if (isHeader) {
					TableRowCore row = view.getRow(lastEntry);
					if (row != null) {
						view.removeDataSource(lastEntry);
						//System.out.println("hiding " + lastEntry.getTypeID() + "/" + lastEntry.text + "; cur = " + entry.getTypeID() + "/" + entry.text);
					}
				} else {
					TableRowCore row = view.getRow(lastEntry);
					if (row == null) {
						view.addDataSource(lastEntry);
						//System.out.println("showing " + lastEntry.getTypeID() + ";" + lastEntry.text);
					}
				}
			}

			lastWasHeader = isHeader;
			lastEntry = entry;
		}
	}

	// @see com.aelitis.azureus.util.VuzeNewsListener#vuzeNewsEntriesAdded(com.aelitis.azureus.util.VuzeNewsEntry[])
	public void vuzeNewsEntriesAdded(VuzeActivitiesEntry[] entries) {
		if (skipShift
				&& VuzeActivitiesEntry.TYPEID_HEADER.equals(entries[0].getTypeID())) {
			return;
		}
		view.addDataSources(entries);
		long newest = 0;
		for (int i = 0; i < entries.length; i++) {
			VuzeActivitiesEntry entry = entries[i];
			if (entry.getTimestamp() > newest) {
				newest = entry.getTimestamp();
			}
		}
		shiftVuzeNews();
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
