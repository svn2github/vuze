/**
 * Created on Feb 24, 2009
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

package com.aelitis.azureus.ui.swt.devices;

import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.IconBarEnabler;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewSWTImpl;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.devices.*;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.table.*;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.swt.columns.torrent.ColumnAzProduct;
import com.aelitis.azureus.ui.swt.columns.torrent.ColumnThumbnail;
import com.aelitis.azureus.ui.swt.devices.columns.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectText;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.views.skin.InfoBarUtil;
import com.aelitis.azureus.ui.swt.views.skin.SkinView;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarEntrySWT;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableColumnCreationListener;
import org.gudy.azureus2.plugins.ui.tables.TableManager;

/**
 * @author TuxPaper
 * @created Feb 24, 2009
 *
 */
public class SBC_DevicesView
	extends SkinView
	implements TranscodeQueueListener, IconBarEnabler, UIUpdatable
{
	public static final String TABLE_DEVICES = "Devices";

	public static final String TABLE_TRANSCODE_QUEUE = "TranscodeQueue";

	private static boolean columnsAdded = false;

	private DeviceManager device_manager;

	private TranscodeManager transcode_manager;

	private TranscodeQueue transcode_queue;

	private TableViewSWTImpl tvDevices;

	private TableViewSWTImpl tvJobs;

	private SideBarEntrySWT sidebarEntry;

	private Composite tableJobsParent;

	private Device device;

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectInitialShow(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		super.skinObjectInitialShow(skinObject, params);

		initColumns();

		device_manager = DeviceManagerFactory.getSingleton();

		transcode_manager = device_manager.getTranscodeManager();

		//transcode_manager.addListener( this );

		transcode_queue = transcode_manager.getQueue();

		transcode_queue.addListener(this);

		SideBar sidebar = (SideBar) SkinViewManager.getByClass(SideBar.class);
		if (sidebar != null) {
			sidebarEntry = sidebar.getCurrentEntry();
			sidebarEntry.setIconBarEnabler(this);
			device = (Device) sidebarEntry.getDatasource();
		}

		new InfoBarUtil(skinObject, true, "DeviceView.infobar",
				"v3.devicesview.infobar") {
			public boolean allowShow() {
				return true;
			}
		};

		SWTSkinObject soAdvInfo = getSkinObject("advinfo");
		if (soAdvInfo != null) {
			initAdvInfo(soAdvInfo);
		}

		if (device != null) {
			SWTSkinObject soTitle = getSkinObject("title");
			if (soTitle instanceof SWTSkinObjectText) {
				((SWTSkinObjectText) soTitle).setText(device.getName());
			}
		}

		return null;
	}

	/**
	 * 
	 *
	 * @since 4.1.0.5
	 */
	private void initColumns() {
		if (columnsAdded) {
			return;
		}
		columnsAdded = true;
		PluginInterface pi = AzureusCoreFactory.getSingleton().getPluginManager().getDefaultPluginInterface();
		UIManager uiManager = pi.getUIManager();
		TableManager tableManager = uiManager.getTableManager();
		tableManager.registerColumn(TranscodeJob.class, ColumnAzProduct.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnAzProduct(column);
					}
				});
		tableManager.registerColumn(TranscodeJob.class, ColumnTJ_Name.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTJ_Name(column);
					}
				});
		tableManager.registerColumn(TranscodeJob.class, ColumnTJ_Rank.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTJ_Rank(column);
					}
				});
		tableManager.registerColumn(TranscodeJob.class, ColumnThumbnail.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnThumbnail(column);
					}
				});
		tableManager.registerColumn(TranscodeJob.class, ColumnTJ_Device.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTJ_Device(column);
					}
				});
		tableManager.registerColumn(TranscodeJob.class, ColumnTJ_Profile.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTJ_Profile(column);
					}
				});
		tableManager.registerColumn(TranscodeJob.class, ColumnTJ_Status.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTJ_Status(column);
					}
				});
		tableManager.registerColumn(TranscodeJob.class,
				ColumnTJ_Completion.COLUMN_ID, new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTJ_Completion(column);
					}
				});
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectShown(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectShown(SWTSkinObject skinObject, Object params) {
		super.skinObjectShown(skinObject, params);

		SWTSkinObject soDeviceList = getSkinObject("device-list");
		if (soDeviceList != null) {
			initDeviceListTable((Composite) soDeviceList.getControl());
		}

		SWTSkinObject soTranscodeQueue = getSkinObject("transcode-queue");
		if (soTranscodeQueue != null) {
			initTranscodeQueueTable((Composite) soTranscodeQueue.getControl());
		}

		return null;
	}

	/**
	 * @param soAdvInfo
	 *
	 * @since 4.1.0.5
	 */
	private void initAdvInfo(SWTSkinObject soAdvInfo) {
		SWTSkinButtonUtility btnAdvInfo = new SWTSkinButtonUtility(soAdvInfo);
		btnAdvInfo.addSelectionListener(new ButtonListenerAdapter() {
			public void pressed(SWTSkinButtonUtility buttonUtility,
					SWTSkinObject skinObject, int stateMask) {
				SWTSkinObject soArea = getSkinObject("advinfo-area");
				if (soArea != null) {
					boolean newVisibility = !soArea.isVisible();
					soArea.setVisible(newVisibility);
					SWTSkinObject soText = getSkinObject("advinfo-title");
					if (soText instanceof SWTSkinObjectText) {
						((SWTSkinObjectText) soText).setText((newVisibility ? "[-]" : "[+]")
								+ " Additional Info");
					}
				}
			}
		});
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectHidden(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectHidden(SWTSkinObject skinObject, Object params) {
		if (tvJobs != null) {
			tvJobs.delete();
			tvJobs = null;
		}
		if (tableJobsParent != null && !tableJobsParent.isDisposed()) {
			tableJobsParent.dispose();
		}
		if (tvDevices != null) {
			tvDevices.delete();
			tvDevices = null;
		}
		return super.skinObjectHidden(skinObject, params);
	}

	/**
	 * @param control
	 *
	 * @since 4.1.0.5
	 */
	private void initTranscodeQueueTable(Composite control) {
		tvJobs = new TableViewSWTImpl(TABLE_TRANSCODE_QUEUE, TABLE_TRANSCODE_QUEUE,
				new TableColumnCore[0], "rank", SWT.MULTI | SWT.FULL_SELECTION
						| SWT.VIRTUAL);
		tvJobs.setDataSourceType(TranscodeJob.class);
		tvJobs.setRowDefaultHeight(50);
		tvJobs.setHeaderVisible(true);

		tableJobsParent = new Composite(control, SWT.NONE);
		tableJobsParent.setLayoutData(Utils.getFilledFormData());
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = layout.verticalSpacing = layout.horizontalSpacing = 0;
		tableJobsParent.setLayout(layout);

		tvJobs.addSelectionListener(new TableSelectionListener() {

			public void selected(TableRowCore[] row) {
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				if (uiFunctions != null) {
					uiFunctions.refreshIconBar();
				}
			}

			public void mouseExit(TableRowCore row) {
			}

			public void mouseEnter(TableRowCore row) {
			}

			public void focusChanged(TableRowCore focus) {
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				if (uiFunctions != null) {
					uiFunctions.refreshIconBar();
				}
			}

			public void deselected(TableRowCore[] rows) {
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				if (uiFunctions != null) {
					uiFunctions.refreshIconBar();
				}
			}

			public void defaultSelected(TableRowCore[] rows, int stateMask) {
			}
		}, false);

		tvJobs.addLifeCycleListener(new TableLifeCycleListener() {
			public void tableViewInitialized() {
				TranscodeJob[] jobs = transcode_queue.getJobs();
				if (device == null) {
					tvJobs.addDataSources(transcode_queue.getJobs());
				} else {
					for (TranscodeJob job : jobs) {
						if (isOurJob(job)) {
							tvJobs.addDataSource(job);
						}
					}
				}
			}

			public void tableViewDestroyed() {
			}
		});

		tvJobs.addMenuFillListener(new TableViewSWTMenuFillListener() {
			public void fillMenu(Menu menu) {
				SBC_DevicesView.this.fillMenu(menu);
			}

			public void addThisColumnSubMenu(String columnName, Menu menuThisColumn) {
			}
		});

		tvJobs.initialize(tableJobsParent);

		control.layout(true);
	}

	/**
	 * @param job
	 * @return
	 *
	 * @since 4.1.0.5
	 */
	protected boolean isOurJob(TranscodeJob job) {
		if (device == null) {
			return true;
		}
		return device.equals(job.getTarget().getDevice());
	}

	/**
	 * @param menu
	 *
	 * @since 4.0.0.5
	 */
	protected void fillMenu(Menu menu) {
		final MenuItem pause_item = new MenuItem(menu, SWT.PUSH);

		pause_item.setText(MessageText.getString("v3.MainWindow.button.pause"));

		pause_item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Object[] jobs = tvJobs.getSelectedDataSources();

				for (int i = 0; i < jobs.length; i++) {
					TranscodeJob job = (TranscodeJob) jobs[i];

					job.pause();
				}
			};
		});

		// resume

		final MenuItem resume_item = new MenuItem(menu, SWT.PUSH);

		resume_item.setText(MessageText.getString("v3.MainWindow.button.resume"));

		resume_item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Object[] jobs = tvJobs.getSelectedDataSources();

				for (int i = 0; i < jobs.length; i++) {
					TranscodeJob job = (TranscodeJob) jobs[i];

					job.resume();
				}
			};
		});

		// separator

		new MenuItem(menu, SWT.SEPARATOR);

		// remove

		final MenuItem remove_item = new MenuItem(menu, SWT.PUSH);

		remove_item.setText(MessageText.getString("azbuddy.ui.menu.remove"));

		remove_item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Object[] jobs = tvJobs.getSelectedDataSources();

				for (int i = 0; i < jobs.length; i++) {
					TranscodeJob job = (TranscodeJob) jobs[i];

					job.remove();
				}
			};
		});

		// separator

		new MenuItem(menu, SWT.SEPARATOR);

		// Login to disable items 
		Object[] jobs = tvJobs.getSelectedDataSources();

		boolean has_selection = jobs.length > 0;

		remove_item.setEnabled(has_selection);

		boolean can_pause = has_selection;
		boolean can_resume = has_selection;

		for (int i = 0; i < jobs.length; i++) {
			TranscodeJob job = (TranscodeJob) jobs[i];

			int state = job.getState();

			if (state != TranscodeJob.ST_RUNNING) {

				can_pause = false;
			}

			if (state != TranscodeJob.ST_PAUSED) {

				can_resume = false;
			}
		}

		pause_item.setEnabled(can_pause);
		resume_item.setEnabled(can_resume);
	}

	/**
	 * 
	 *
	 * @param parent 
	 * @since 4.1.0.5
	 */
	private void initDeviceListTable(Composite control) {
		tvDevices = new TableViewSWTImpl(TABLE_DEVICES, TABLE_DEVICES,
				new TableColumnCore[0], "name");
		tvDevices.setDataSourceType(TranscodeProvider.class);
		tvDevices.setRowDefaultHeight(50);
		tvDevices.setHeaderVisible(true);

		Composite parent = new Composite(control, SWT.NONE);
		parent.setLayoutData(Utils.getFilledFormData());
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = layout.verticalSpacing = layout.horizontalSpacing = 0;
		parent.setLayout(layout);

		tvDevices.initialize(parent);
	}

	// @see com.aelitis.azureus.core.devices.TranscodeQueueListener#jobAdded(com.aelitis.azureus.core.devices.TranscodeJob)
	public void jobAdded(TranscodeJob job) {
		if (tvJobs != null) {
			if (isOurJob(job)) {
				tvJobs.addDataSource(job);
			}
		}
	}

	// @see com.aelitis.azureus.core.devices.TranscodeQueueListener#jobChanged(com.aelitis.azureus.core.devices.TranscodeJob)
	public void jobChanged(TranscodeJob job) {
		if (tvJobs != null) {
			TableRowCore row = tvJobs.getRow(job);
			if (row != null) {
				row.invalidate();
				if (row.isVisible()) {
					UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
					if (uiFunctions != null) {
						uiFunctions.refreshIconBar();
					}
				}
			}
		}
	}

	// @see com.aelitis.azureus.core.devices.TranscodeQueueListener#jobRemoved(com.aelitis.azureus.core.devices.TranscodeJob)
	public void jobRemoved(TranscodeJob job) {
		if (tvJobs != null) {
			tvJobs.removeDataSource(job);
		}
	}

	// @see org.gudy.azureus2.ui.swt.IconBarEnabler#isEnabled(java.lang.String)
	public boolean isEnabled(String itemKey) {
		if (tvJobs == null) {
			return false;
		}
		Object[] selectedDS = tvJobs.getSelectedDataSources();
		int size = tvJobs.size(false);
		if (selectedDS.length == 0) {

			return (false);
		}

		if (itemKey.equals("remove")) {

			return (true);
		}

		boolean can_stop = true;
		boolean can_queue = true;
		boolean can_move_up = true;
		boolean can_move_down = true;

		for (Object ds : selectedDS) {
			TranscodeJob job = (TranscodeJob) ds;

			int index = job.getIndex();

			if (index == 1) {

				can_move_up = false;

			}

			if (index == size) {

				can_move_down = false;
			}

			int state = job.getState();

			if (state != TranscodeJob.ST_PAUSED && state != TranscodeJob.ST_RUNNING
					&& state != TranscodeJob.ST_FAILED) {

				can_stop = false;
			}

			if (state != TranscodeJob.ST_PAUSED && state != TranscodeJob.ST_STOPPED
					&& state != TranscodeJob.ST_FAILED) {

				can_queue = false;
			}
		}

		if (itemKey.equals("stop")) {

			return (can_stop);
		}

		if (itemKey.equals("start")) {

			return (can_queue);
		}

		if (itemKey.equals("up")) {

			return (can_move_up);
		}

		if (itemKey.equals("down")) {

			return (can_move_down);
		}

		return (false);
	}

	// @see org.gudy.azureus2.ui.swt.IconBarEnabler#isSelected(java.lang.String)
	public boolean isSelected(String itemKey) {
		return false;
	}

	// @see org.gudy.azureus2.ui.swt.IconBarEnabler#itemActivated(java.lang.String)
	public void itemActivated(final String itemKey) {
		Object[] selectedDS = tvJobs.getSelectedDataSources();
		int size = tvJobs.size(false);
		if (selectedDS.length == 0) {
			return;
		}

		TranscodeJob[] jobs = new TranscodeJob[selectedDS.length];
		for (int i = 0; i < jobs.length; i++) {
			jobs[i] = (TranscodeJob) selectedDS[i];
		}

		if (itemKey.equals("up") || itemKey.equals("down")) {

			Arrays.sort(jobs, new Comparator<TranscodeJob>() {
				public int compare(TranscodeJob j1, TranscodeJob j2) {

					return ((itemKey.equals("up") ? 1 : -1) * (j1.getIndex() - j2.getIndex()));
				}
			});
		}

		for (TranscodeJob job : jobs) {

			if (itemKey.equals("remove")) {

				job.remove();

			} else if (itemKey.equals("stop")) {

				job.stop();

			} else if (itemKey.equals("start")) {

				job.queue();

			} else if (itemKey.equals("up")) {

				job.moveUp();

			} else if (itemKey.equals("down")) {

				job.moveDown();
			}
		}
	}

	// @see com.aelitis.azureus.ui.common.updater.UIUpdatable#getUpdateUIName()
	public String getUpdateUIName() {
		return "DevicesView";
	}

	// @see com.aelitis.azureus.ui.common.updater.UIUpdatable#updateUI()
	public void updateUI() {
		if (tvJobs != null) {
			tvJobs.refreshTable(false);
		}
	}
}
