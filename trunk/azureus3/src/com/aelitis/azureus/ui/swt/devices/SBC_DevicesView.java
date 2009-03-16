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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.ui.swt.IconBarEnabler;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewSWTImpl;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.devices.*;
import com.aelitis.azureus.core.messenger.config.PlatformDevicesMessenger;
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
	implements TranscodeQueueListener, IconBarEnabler, UIUpdatable,
	TranscodeTargetListener
{
	public static final String TABLE_DEVICES = "Devices";

	public static final String TABLE_TRANSCODE_QUEUE = "TranscodeQueue";

	public static final String TABLE_DEVICE_LIBRARY = "DeviceLibrary";

	private static boolean columnsAdded = false;

	private DeviceManager device_manager;

	private TranscodeManager transcode_manager;

	private TranscodeQueue transcode_queue;

	private TableViewSWTImpl tvDevices;

	private TableViewSWTImpl<TranscodeFile> tvFiles;

	private SideBarEntrySWT sidebarEntry;

	private Composite tableJobsParent;

	private Device device;

	private TranscodeTarget transTarget;

	private DropTarget dropTarget;

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectInitialShow(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		super.skinObjectInitialShow(skinObject, params);

		initColumns();

		device_manager = DeviceManagerFactory.getSingleton();

		transcode_manager = device_manager.getTranscodeManager();

		transcode_queue = transcode_manager.getQueue();

		SideBar sidebar = (SideBar) SkinViewManager.getByClass(SideBar.class);
		if (sidebar != null) {
			sidebarEntry = sidebar.getCurrentEntry();
			sidebarEntry.setIconBarEnabler(this);
			device = (Device) sidebarEntry.getDatasource();
		}

		if (device instanceof TranscodeTarget) {
			transTarget = (TranscodeTarget) device;
		}

		/*
		new InfoBarUtil(skinObject, true, "DeviceView.infobar",
				"v3.devicesview.infobar") {
			public boolean allowShow() {
				return true;
			}
		};
		*/

		SWTSkinObject soAdvInfo = getSkinObject("advinfo");
		if (soAdvInfo != null) {
			initAdvInfo(soAdvInfo);
		}

		if (device != null) {
			SWTSkinObject soTitle = getSkinObject("title");
			if (soTitle instanceof SWTSkinObjectText) {
				((SWTSkinObjectText) soTitle).setTextID("device.view.heading",
						new String[] {
							device.getName()
						});
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
		tableManager.registerColumn(TranscodeFile.class, ColumnTJ_Rank.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTJ_Rank(column);
					}
				});
		tableManager.registerColumn(TranscodeFile.class, ColumnAzProduct.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnAzProduct(column);
					}
				});
		tableManager.registerColumn(TranscodeFile.class, ColumnThumbnail.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnThumbnail(column);
					}
				});
		tableManager.registerColumn(TranscodeFile.class, ColumnTJ_Name.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTJ_Name(column);
					}
				});
		tableManager.registerColumn(TranscodeFile.class, ColumnTJ_Status.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTJ_Status(column);
					}
				});
		tableManager.registerColumn(TranscodeFile.class,
				ColumnTJ_Completion.COLUMN_ID, new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTJ_Completion(column);
					}
				});
		tableManager.registerColumn(TranscodeFile.class, ColumnTJ_Device.COLUMN_ID,
				new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTJ_Device(column);
						// Device column not needed for Device specific view.  Since
						// we can't remove it, just hide it
						if (!column.getTableID().equals(TABLE_TRANSCODE_QUEUE)) {
							column.setVisible(false);
						}
					}
				});
		tableManager.registerColumn(TranscodeFile.class,
				ColumnTJ_Profile.COLUMN_ID, new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTJ_Profile(column);
					}
				});

		tableManager.registerColumn(TranscodeFile.class,
				ColumnTJ_Duration.COLUMN_ID, new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTJ_Duration(column);
					}
				});

		tableManager.registerColumn(TranscodeFile.class,
				ColumnTJ_Resolution.COLUMN_ID, new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTJ_Resolution(column);
					}
				});

		tableManager.registerColumn(TranscodeFile.class,
				ColumnTJ_CopiedToDevice.COLUMN_ID, new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnTJ_CopiedToDevice(column);

						if (column.getTableID().endsWith(":type=1")) {
							column.setVisible(false);
						}
					}
				});
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectShown(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectShown(SWTSkinObject skinObject, Object params) {
		super.skinObjectShown(skinObject, params);

		transcode_queue.addListener(this);

		if (transTarget != null) {
			transTarget.addListener(this);
		}

		SWTSkinObject soDeviceList = getSkinObject("device-list");
		if (soDeviceList != null) {
			initDeviceListTable((Composite) soDeviceList.getControl());
		}

		SWTSkinObject soTranscodeQueue = getSkinObject("transcode-queue");
		if (soTranscodeQueue != null) {
			initTranscodeQueueTable((Composite) soTranscodeQueue.getControl());
		}

		Control control = skinObject.getControl();
		dropTarget = new DropTarget(control, 0xFF);

		setAdditionalInfoTitle(false);

		DevicesFTUX.ensureInstalled();

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
					setAdditionalInfoTitle(newVisibility);
				}
			}
		});
		setAdditionalInfoTitle(false);
	}

	/**
	 * @param newVisibility
	 *
	 * @since 4.1.0.5
	 */
	protected void setAdditionalInfoTitle(boolean newVisibility) {
		SWTSkinObject soArea = getSkinObject("advinfo-area");
		if (soArea != null) {
			soArea.setVisible(newVisibility);
		}
		SWTSkinObject soText = getSkinObject("advinfo-title");
		if (soText instanceof SWTSkinObjectText) {
			String s = (newVisibility ? "[-]" : "[+]");
			if (device != null) {
				s += "Additional Device Info and Settings";
			} else {
				s += "General Options";
			}
			((SWTSkinObjectText) soText).setText(s);
		}
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectHidden(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectHidden(SWTSkinObject skinObject, Object params) {
		transcode_queue.removeListener(this);

		if (transTarget != null) {
			transTarget.removeListener(this);
		}

		synchronized (this) {
			if (tvFiles != null) {
				tvFiles.delete();
				tvFiles = null;
			}
		}
		Utils.disposeSWTObjects(new Object[] {
			tableJobsParent,
			dropTarget
		});
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
		String tableID;

		if (device == null) {

			tableID = TABLE_TRANSCODE_QUEUE;

		} else {

			tableID = TABLE_DEVICE_LIBRARY;

			if (device instanceof DeviceMediaRenderer) {

				if (!((DeviceMediaRenderer) device).canCopyToDevice()) {

					tableID += ":type=1";
				}
			}
		}

		tvFiles = new TableViewSWTImpl<TranscodeFile>(tableID, tableID,
				new TableColumnCore[0], "rank", SWT.MULTI | SWT.FULL_SELECTION
						| SWT.VIRTUAL);
		tvFiles.setDataSourceType(TranscodeFile.class);
		tvFiles.setRowDefaultHeight(50);
		tvFiles.setHeaderVisible(true);
		tvFiles.setParentDataSource(device);

		tableJobsParent = new Composite(control, SWT.NONE);
		tableJobsParent.setLayoutData(Utils.getFilledFormData());
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = layout.verticalSpacing = layout.horizontalSpacing = 0;
		tableJobsParent.setLayout(layout);

		tvFiles.addSelectionListener(new TableSelectionListener() {

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

		tvFiles.addLifeCycleListener(new TableLifeCycleListener() {
			public void tableViewInitialized() {
				if (transTarget == null) {
					// just add all jobs' files
					TranscodeJob[] jobs = transcode_queue.getJobs();
					for (TranscodeJob job : jobs) {
						TranscodeFile file = job.getTranscodeFile();
						if (file != null) {
							tvFiles.addDataSource(file);
						}
					}
				} else {
					tvFiles.addDataSources(transTarget.getFiles());
				}
			}

			public void tableViewDestroyed() {
			}
		});

		tvFiles.addMenuFillListener(new TableViewSWTMenuFillListener() {
			public void fillMenu(Menu menu) {
				SBC_DevicesView.this.fillMenu(menu);
			}

			public void addThisColumnSubMenu(String columnName, Menu menuThisColumn) {
			}
		});

		tvFiles.initialize(tableJobsParent);

		control.layout(true);
	}

	/**
	 * @param menu
	 *
	 * @since 4.0.0.5
	 */
	protected void fillMenu(Menu menu) {

		Object[] _files = tvFiles.getSelectedDataSources().toArray();

		final TranscodeFile[] files = new TranscodeFile[_files.length];

		System.arraycopy(_files, 0, files, 0, files.length);

		// open file

		final MenuItem open_item = new MenuItem(menu, SWT.PUSH);

		Messages.setLanguageText(open_item, "MyTorrentsView.menu.open");

		Utils.setMenuItemImage(open_item, "run");

		File target_file = null;

		try {
			if (files.length == 1) {

				target_file = files[0].getTargetFile().getFile();

				if (!target_file.exists()) {

					target_file = null;
				}
			}
		} catch (Throwable e) {

			Debug.out(e);
		}

		final File f_target_file = target_file;

		open_item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent ev) {

				Utils.launch(f_target_file.getAbsolutePath());
			};
		});

		open_item.setEnabled(target_file != null && files[0].isComplete());

		// show in explorer

		final boolean use_open_containing_folder = COConfigurationManager.getBooleanParameter("MyTorrentsView.menu.show_parent_folder_enabled");

		final MenuItem show_item = new MenuItem(menu, SWT.PUSH);

		Messages.setLanguageText(show_item, "MyTorrentsView.menu."
				+ (use_open_containing_folder ? "open_parent_folder" : "explore"));

		show_item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {

				ManagerUtils.open(f_target_file, use_open_containing_folder);
			};
		});

		show_item.setEnabled(target_file != null);

		new MenuItem(menu, SWT.SEPARATOR);

		// pause

		final MenuItem pause_item = new MenuItem(menu, SWT.PUSH);

		pause_item.setText(MessageText.getString("v3.MainWindow.button.pause"));

		pause_item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {

				for (int i = 0; i < files.length; i++) {
					TranscodeJob job = files[i].getJob();

					if (job != null) {
						job.pause();
					}
				}
			};
		});

		// resume

		final MenuItem resume_item = new MenuItem(menu, SWT.PUSH);

		resume_item.setText(MessageText.getString("v3.MainWindow.button.resume"));

		resume_item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				for (int i = 0; i < files.length; i++) {
					TranscodeJob job = files[i].getJob();

					if (job != null) {
						job.resume();
					}
				}
			};
		});

		// separator

		new MenuItem(menu, SWT.SEPARATOR);

		if (device instanceof DeviceMediaRenderer) {

			DeviceMediaRenderer dmr = (DeviceMediaRenderer) device;

			if (dmr.canCopyToDevice()) {

				// retry

				final MenuItem retry_item = new MenuItem(menu, SWT.PUSH);

				retry_item.setText(MessageText.getString("device.retry.copy"));

				retry_item.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						for (int i = 0; i < files.length; i++) {
							TranscodeFile file = files[i];

							if (file.getCopyToDeviceFails() > 0) {

								file.retryCopyToDevice();
							}
						}
					};
				});

				retry_item.setEnabled(false);

				for (TranscodeFile file : files) {

					if (file.getCopyToDeviceFails() > 0) {

						retry_item.setEnabled(true);
					}
				}

				// separator

				new MenuItem(menu, SWT.SEPARATOR);
			}
		}

		// remove

		final MenuItem remove_item = new MenuItem(menu, SWT.PUSH);

		remove_item.setText(MessageText.getString("azbuddy.ui.menu.remove"));

		Utils.setMenuItemImage(remove_item, "delete");

		remove_item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				for (TranscodeFile file : files) {
					TranscodeJob job = file.getJob();

					if (job != null) {
						job.remove();
					}

					try {
						file.delete(true);

					} catch (Throwable f) {
					}
				}
			};
		});

		// separator

		new MenuItem(menu, SWT.SEPARATOR);

		// Logic to disable items 

		boolean has_selection = files.length > 0;

		remove_item.setEnabled(has_selection);

		boolean can_pause = has_selection;
		boolean can_resume = has_selection;

		int job_count = 0;

		for (int i = 0; i < files.length; i++) {
			TranscodeJob job = files[i].getJob();
			if (job == null) {
				continue;
			}

			job_count++;

			int state = job.getState();

			if (state != TranscodeJob.ST_RUNNING || !job.canPause()) {

				can_pause = false;
			}

			if (state != TranscodeJob.ST_PAUSED) {

				can_resume = false;
			}
		}

		pause_item.setEnabled(can_pause && job_count > 0);
		resume_item.setEnabled(can_resume && job_count > 0);
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
		synchronized (this) {
			if (tvFiles == null) {
				return;
			}

			if (transTarget == null) {
				TranscodeFile file = job.getTranscodeFile();
				if (file != null) {
					tvFiles.addDataSource(file);
				}
			}
		}
	}

	// @see com.aelitis.azureus.core.devices.TranscodeQueueListener#jobChanged(com.aelitis.azureus.core.devices.TranscodeJob)
	public void jobChanged(TranscodeJob job) {
		synchronized (this) {
			if (tvFiles == null) {
				return;
			}
			TableRowCore row = tvFiles.getRow(job.getTranscodeFile());
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
		synchronized (this) {
			if (tvFiles == null) {
				return;
			}
			if (transTarget == null) {
				TranscodeFile file = job.getTranscodeFile();
				if (file != null) {
					tvFiles.removeDataSource(file);
				}
			} else {
				TableRowCore row = tvFiles.getRow(job.getTranscodeFile());
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
	}

	// @see org.gudy.azureus2.ui.swt.IconBarEnabler#isEnabled(java.lang.String)
	public boolean isEnabled(String itemKey) {
		Object[] selectedDS;
		int size;
		synchronized (this) {
			if (tvFiles == null) {
				return false;
			}
			selectedDS = tvFiles.getSelectedDataSources().toArray();
			size = tvFiles.size(false);
		}
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
		boolean hasJob = false;

		for (Object ds : selectedDS) {
			TranscodeJob job = ((TranscodeFile) ds).getJob();

			if (job == null) {
				continue;
			}

			hasJob = true;

			int index = job.getIndex();

			if (index == 1) {

				can_move_up = false;

			}

			if (index == size) {

				can_move_down = false;
			}

			int state = job.getState();

			if (state != TranscodeJob.ST_PAUSED && state != TranscodeJob.ST_RUNNING
					&& state != TranscodeJob.ST_FAILED && state != TranscodeJob.ST_QUEUED) {

				can_stop = false;
			}

			if (state != TranscodeJob.ST_PAUSED && state != TranscodeJob.ST_STOPPED
					&& state != TranscodeJob.ST_FAILED) {

				can_queue = false;
			}
		}

		if (!hasJob) {
			can_stop = can_queue = can_move_down = can_move_up = false;
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
		// assumed to be on SWT thread, so it's safe to use tvFiles without a sync
		if (tvFiles == null) {
			return;
		}

		Object[] selectedDS = tvFiles.getSelectedDataSources().toArray();
		int size = tvFiles.size(false);
		if (selectedDS.length == 0) {
			return;
		}

		if (itemKey.equals("remove")) {
			for (Object ds : selectedDS) {
				try {
					((TranscodeFile) ds).delete(true);
				} catch (TranscodeException e) {
					Debug.out(e);
				}
			}
		}

		java.util.List<TranscodeJob> jobs = new ArrayList<TranscodeJob>(
				selectedDS.length);

		for (int i = 0; i < selectedDS.length; i++) {
			TranscodeFile file = (TranscodeFile) selectedDS[i];
			TranscodeJob job = file.getJob();
			if (job != null) {
				jobs.add(job);
			}
		}
		if (jobs.size() == 0) {
			return;
		}

		if (itemKey.equals("up") || itemKey.equals("down")) {

			Collections.sort(jobs, new Comparator<TranscodeJob>() {
				public int compare(TranscodeJob j1, TranscodeJob j2) {

					return ((itemKey.equals("up") ? 1 : -1) * (j1.getIndex() - j2.getIndex()));
				}
			});
		}

		boolean forceSort = false;
		for (TranscodeJob job : jobs) {

			if (itemKey.equals("remove")) {

				job.remove();

			} else if (itemKey.equals("stop")) {

				job.stop();

			} else if (itemKey.equals("start")) {

				job.queue();

			} else if (itemKey.equals("up")) {

				job.moveUp();

				TableColumnCore sortColumn = tvFiles.getSortColumn();
				forceSort = sortColumn != null
						&& sortColumn.getName().equals(ColumnTJ_Rank.COLUMN_ID);

			} else if (itemKey.equals("down")) {

				job.moveDown();

				TableColumnCore sortColumn = tvFiles.getSortColumn();
				forceSort = sortColumn != null
						&& sortColumn.getName().equals(ColumnTJ_Rank.COLUMN_ID);
			}
		}
		tvFiles.refreshTable(forceSort);

	}

	// @see com.aelitis.azureus.ui.common.updater.UIUpdatable#getUpdateUIName()
	public String getUpdateUIName() {
		return "DevicesView";
	}

	// @see com.aelitis.azureus.ui.common.updater.UIUpdatable#updateUI()
	public void updateUI() {
		if (tvFiles != null) {
			tvFiles.refreshTable(false);
		}
	}

	// @see com.aelitis.azureus.core.devices.TranscodeTargetListener#fileAdded(com.aelitis.azureus.core.devices.TranscodeFile)
	public void fileAdded(TranscodeFile file) {
		synchronized (this) {
			if (tvFiles != null) {
				tvFiles.addDataSource(file);
			}
		}
	}

	// @see com.aelitis.azureus.core.devices.TranscodeTargetListener#fileChanged(com.aelitis.azureus.core.devices.TranscodeFile, int, java.lang.Object)
	public void fileChanged(TranscodeFile file, int type, Object data) {
		synchronized (this) {
			if (tvFiles == null) {
				return;
			}
			TableRowCore row = tvFiles.getRow(file);
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

	// @see com.aelitis.azureus.core.devices.TranscodeTargetListener#fileRemoved(com.aelitis.azureus.core.devices.TranscodeFile)
	public void fileRemoved(TranscodeFile file) {
		synchronized (this) {
			if (tvFiles != null) {
				tvFiles.removeDataSource(file);
			}
		}
	}

}
