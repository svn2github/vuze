/**
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
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
 * 
 */

package com.aelitis.azureus.ui.swt.shells.opentorrent;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.StringIterator;
import org.gudy.azureus2.core3.config.StringList;
import org.gudy.azureus2.core3.config.impl.ConfigurationDefaults;
import org.gudy.azureus2.core3.internat.LocaleTorrentUtil;
import org.gudy.azureus2.core3.internat.LocaleUtilDecoder;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.peer.PEPeerSource;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.torrent.impl.TorrentOpenFileOptions;
import org.gudy.azureus2.core3.torrent.impl.TorrentOpenOptions;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableColumnCreationListener;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewFactory;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.TrackerNameItem;
import org.gudy.azureus2.ui.swt.views.utils.TagUIUtils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.tag.Tag;
import com.aelitis.azureus.core.tag.TagFeatureFileLocation;
import com.aelitis.azureus.core.tag.TagManagerFactory;
import com.aelitis.azureus.core.tag.TagType;
import com.aelitis.azureus.core.tag.TagTypeListener;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.common.table.TableSelectionListener;
import com.aelitis.azureus.ui.common.table.impl.TableColumnManager;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.swt.shells.main.UIFunctionsImpl;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.uiupdater.UIUpdaterSWT;
import com.aelitis.azureus.ui.swt.views.skin.SkinnedDialog;
import com.aelitis.azureus.ui.swt.views.skin.SkinnedDialog.SkinnedDialogClosedListener;
import com.aelitis.azureus.ui.swt.views.skin.StandardButtonsArea;

@SuppressWarnings({
	"unchecked",
	"rawtypes"
})
public class OpenTorrentOptionsWindow
	implements UIUpdatable
{
	private static final Map<HashWrapper,OpenTorrentOptionsWindow>	active_windows = new HashMap<HashWrapper, OpenTorrentOptionsWindow>();
	
	private final static class FileStatsCacheItem
	{
		boolean exists;

		long freeSpace;

		public FileStatsCacheItem(final File f) {
			exists = f.exists();
			if (exists)
				freeSpace = FileUtil.getUsableSpace(f);
			else
				freeSpace = -1;
		}
	}

	private final static class Partition
	{
		long bytesToConsume = 0;

		long freeSpace = 0;

		final File root;

		public Partition(File root) {
			this.root = root;
		}
	}

	private final static String PARAM_DEFSAVEPATH = "Default save path";

	private final static String[] queueLocations = {
		"first",
		"last"
	};

	private final static String[] startModes = {
		"queued",
		"stopped",
		"forceStarted",
		"seeding"
	};

	public static final String TABLEID_FILES = "OpenTorrentFile";

	/* prevents loop of modifications */
	protected boolean bSkipDataDirModify = false;

	private Button btnSwarmIt;

	private StandardButtonsArea buttonsArea;

	private Combo cmbDataDir;

	private Combo cmbQueueLocation;

	private Combo cmbStartMode;

	private StringList dirList;

	private volatile boolean diskFreeInfoRefreshPending = false;

	private volatile boolean diskFreeInfoRefreshRunning = false;

	private Composite diskspaceComp;

	private SkinnedDialog dlg;

	private final Map fileStatCache = new WeakHashMap(20);

	private final Map parentToRootCache = new WeakHashMap(20);

	private SWTSkinObjectExpandItem soExpandItemFiles;

	private SWTSkinObjectExpandItem soExpandItemSaveTo;

	private SWTSkinObjectExpandItem soExpandItemTorrentInfo;

	private SWTSkinObjectText soFileAreaInfo;

	private TorrentOpenOptions torrentOptions;

	private TableViewSWT<Object> tvFiles;

	private SWTSkinObjectExpandItem soStartOptionsExpandItem;

	private SWTSkinObjectExpandItem soExpandItemPeer;

	private Button btnRename;

	private Button btnRetarget;

	public static void main(String[] args) {
		AzureusCore core = AzureusCoreFactory.create();
		core.start();

		UIConfigDefaultsSWT.initialize();

		//		try {
		//			SWTThread.createInstance(null);
		//		} catch (SWTThreadAlreadyInstanciatedException e) {
		//			e.printStackTrace();
		//		}
		Display display = Display.getDefault();

		Colors.getInstance();

		UIFunctionsImpl uiFunctions = new UIFunctionsImpl(null);
		UIFunctionsManager.setUIFunctions(uiFunctions);

		File file = new File("/Users/Vuze/samples.torrent");
		TOTorrent torrent = null;
		try {
			torrent = TOTorrentFactory.deserialiseFromBEncodedFile(file);
		} catch (TOTorrentException e) {
			e.printStackTrace();
		}
		;

		OpenTorrentOptionsWindow window = new OpenTorrentOptionsWindow(null,
				new TorrentOpenOptions(null, torrent, false));
		while (!window.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}

		core.stop();
	}

	public OpenTorrentOptionsWindow(Shell parent,
			TorrentOpenOptions torrentOptions) {
		this.torrentOptions = torrentOptions;

		torrentOptions.addListener(new TorrentOpenOptions.ToDownloadListener() {
			public void toDownloadChanged(TorrentOpenFileOptions fo, boolean toDownload) {
				TableRowCore row = tvFiles.getRow(fo);
				if (row != null) {
					row.invalidate(true);
					row.refresh(true);
				}
				updateSize();
			}
		});

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				swt_createWindow();
			}
		});
	}

	private void checkSeedingMode() {
		// Check for seeding
		boolean bTorrentValid = true;

		if (torrentOptions.iStartID == TorrentOpenOptions.STARTMODE_SEEDING) {
			// check if all selected files exist
			TorrentOpenFileOptions[] files = torrentOptions.getFiles();
			for (int j = 0; j < files.length; j++) {
				TorrentOpenFileOptions fileInfo = files[j];
				if (!fileInfo.isToDownload())
					continue;

				File file = fileInfo.getInitialLink();

				if (file == null) {

					file = fileInfo.getDestFileFullName();
				}

				if (!file.exists()) {
					fileInfo.isValid = false;
					bTorrentValid = false;
				} else if (!fileInfo.isValid) {
					fileInfo.isValid = true;
				}
			}
		}

		torrentOptions.isValid = bTorrentValid;
	}

	protected void cmbDataDirChanged() {

		if (bSkipDataDirModify || cmbDataDir == null) {
			return;
		}
		torrentOptions.setParentDir( cmbDataDir.getText());

		checkSeedingMode();

		if (!Utils.isCocoa || SWT.getVersion() > 3600) { // See Eclipse Bug 292449
			File file = new File(torrentOptions.getParentDir());
			if (!file.isDirectory()) {
				cmbDataDir.setBackground(Colors.colorErrorBG);
				// make the error state visible
				soExpandItemSaveTo.setExpanded(true);
			} else {
				cmbDataDir.setBackground(null);
			}
			cmbDataDir.redraw();
			cmbDataDir.update();
		}

		if (soExpandItemSaveTo != null) {
			String s = MessageText.getString("OpenTorrentOptions.header.saveto",
					new String[] {
						torrentOptions.getParentDir()
					});
			soExpandItemSaveTo.setText(s);
		}
		diskFreeInfoRefreshPending = true;
	}

	protected void dispose() {
		UIUpdaterSWT.getInstance().removeUpdater(this);
	}

	private long getCachedDirFreeSpace(File directory) {
		FileStatsCacheItem item = (FileStatsCacheItem) fileStatCache.get(directory);
		if (item == null)
			fileStatCache.put(directory, item = new FileStatsCacheItem(directory));
		return item.freeSpace;
	}

	private boolean getCachedExistsStat(File directory) {
		FileStatsCacheItem item = (FileStatsCacheItem) fileStatCache.get(directory);
		if (item == null)
			fileStatCache.put(directory, item = new FileStatsCacheItem(directory));
		return item.exists;
	}

	public String getUpdateUIName() {
		return null;
	}

	private boolean isDisposed() {
		if (dlg == null) {
			return false;
		}
		return dlg.isDisposed();
	}

	protected void setSelectedQueueLocation(int iLocation) {
		torrentOptions.iQueueLocation = iLocation;

		updateStartOptionsHeader();
	}

	private void updateStartOptionsHeader() {
		if (soStartOptionsExpandItem == null) {
			return;
		}

		String optionText = MessageText.getString("OpenTorrentWindow.startMode."
				+ startModes[torrentOptions.iStartID])
				+ ", "
				+ MessageText.getString("OpenTorrentWindow.addPosition."
						+ queueLocations[torrentOptions.iQueueLocation]);

		String s = MessageText.getString("OpenTorrentOptions.header.startoptions",
				new String[] {
					optionText
				});
		
		List<Tag> initialtags = torrentOptions.getInitialTags();
		
		String tag_str;
		
		if ( initialtags.size() == 0 ){
			
			tag_str = MessageText.getString( "label.none" );
			
		}else{
			
			tag_str = "";
			
			for ( Tag t: initialtags ){
				
				tag_str += (tag_str==""?"":", ") + t.getTagName( true );
			}
		}
		
		s += "        " + MessageText.getString( "OpenTorrentOptions.header.tags", new String[]{ tag_str });
		
		soStartOptionsExpandItem.setText(s);
	}

	protected void setSelectedStartMode(int iStartID) {
		torrentOptions.iStartID = iStartID;

		checkSeedingMode();
		updateStartOptionsHeader();
	}

	private void setupFileAreaButtons(SWTSkinObjectContainer so) {
		Composite cButtons = so.getComposite();
		RowLayout rowLayout = new RowLayout();
		rowLayout.marginBottom = rowLayout.marginTop = 0;
		cButtons.setLayout(rowLayout);

		btnRename = new Button(cButtons, SWT.PUSH);
		Messages.setLanguageText(btnRename, "Button.rename");
		btnRename.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				TorrentOpenFileOptions[] infos = tvFiles.getSelectedDataSources().toArray(
						new TorrentOpenFileOptions[0]);
				renameFilenames(infos);
			}
		});

		btnRetarget = new Button(cButtons, SWT.PUSH);
		Messages.setLanguageText(btnRetarget, "Button.retarget");
		btnRetarget.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				TorrentOpenFileOptions[] infos = tvFiles.getSelectedDataSources().toArray(
						new TorrentOpenFileOptions[0]);
				changeFileDestination(infos);
			}
		});
		
		try {
			if (COConfigurationManager.getBooleanParameter("rcm.overall.enabled",
					true) && AzureusCoreFactory.isCoreRunning()) {
				final PluginInterface pi = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByID(
						"aercm");

				if (pi != null && pi.getPluginState().isOperational()
						&& pi.getIPC().canInvoke("lookupBySize", new Object[] {
							new Long(0)
						})) {

					Label pad = new Label(cButtons, SWT.NONE);
					pad.setLayoutData(new RowData(50, 0));

					btnSwarmIt = new Button(cButtons, SWT.PUSH);
					Messages.setLanguageText(btnSwarmIt, "Button.swarmit");

					btnSwarmIt.addListener(SWT.Selection, new Listener() {
						public void handleEvent(Event event) {
							List<Object> selectedDataSources = tvFiles.getSelectedDataSources();
							for (Object ds : selectedDataSources) {
								TorrentOpenFileOptions file = (TorrentOpenFileOptions) ds;

								try {
									pi.getIPC().invoke("lookupBySize", new Object[] {
										new Long(file.lSize)
									});

								} catch (Throwable e) {

									Debug.out(e);
								}
								break;
							}
						}
					});

					btnSwarmIt.setEnabled(false);
				}
			}
		} catch (Throwable e) {

		}

		updateFileButtons();

	}

	private void setupFileAreaInfo(SWTSkinObjectText so) {
		soFileAreaInfo = so;
	}

	private void setupSaveLocation(SWTSkinObjectContainer soInputArea,
			SWTSkinObjectButton soBrowseButton) {
		cmbDataDir = new Combo(soInputArea.getComposite(), SWT.NONE);
		cmbDataDir.setLayoutData(Utils.getFilledFormData());

		cmbDataDir.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				cmbDataDirChanged();
			}
		});
		cmbDataDir.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				cmbDataDirChanged();
			}
		});

		updateDataDirCombo();
		dirList = COConfigurationManager.getStringListParameter("saveTo_list");
		StringIterator iter = dirList.iterator();
		while (iter.hasNext()) {
			String s = iter.next();
			if (!s.equals(torrentOptions.getParentDir())) {
				cmbDataDir.add(s);
			}
		}

		soBrowseButton.addSelectionListener(new ButtonListenerAdapter() {
			@Override
			public void pressed(SWTSkinButtonUtility buttonUtility,
					SWTSkinObject skinObject, int stateMask) {
				String sSavePath;
				String sDefPath = cmbDataDir.getText();

				File f = new File(sDefPath);
				if (sDefPath.length() > 0) {
					while (!f.exists()) {
						f = f.getParentFile();
						if (f == null) {
							f = new File(sDefPath);
							break;
						}
					}
				}

				DirectoryDialog dDialog = new DirectoryDialog(cmbDataDir.getShell(),
						SWT.SYSTEM_MODAL);
				dDialog.setFilterPath(f.getAbsolutePath());
				dDialog.setMessage(MessageText.getString("MainWindow.dialog.choose.savepath_forallfiles"));
				sSavePath = dDialog.open();

				if (sSavePath != null) {
					cmbDataDir.setText(sSavePath);
				}
			}
		});
	}

	private void setupStartOptions(SWTSkinObjectExpandItem so) {
		soStartOptionsExpandItem = so;
		Composite cTorrentOptions = so.getComposite();

		Composite cTorrentModes = new Composite(cTorrentOptions, SWT.NONE);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		cTorrentModes.setLayoutData(Utils.getFilledFormData());
		GridLayout layout = new GridLayout();
		layout.numColumns = 4;
		layout.marginWidth = 5;
		layout.marginHeight = 5;
		cTorrentModes.setLayout(layout);

		Label label = new Label(cTorrentModes, SWT.NONE);
		gridData = new GridData(GridData.VERTICAL_ALIGN_CENTER);
		label.setLayoutData(gridData);
		Messages.setLanguageText(label, "OpenTorrentWindow.startMode");

		cmbStartMode = new Combo(cTorrentModes, SWT.BORDER | SWT.READ_ONLY);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		cmbStartMode.setLayoutData(gridData);
		updateStartModeCombo();
		cmbStartMode.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setSelectedStartMode(cmbStartMode.getSelectionIndex());
			}
		});

		label = new Label(cTorrentModes, SWT.NONE);
		gridData = new GridData(GridData.VERTICAL_ALIGN_CENTER);
		label.setLayoutData(gridData);
		Messages.setLanguageText(label, "OpenTorrentWindow.addPosition");

		cmbQueueLocation = new Combo(cTorrentModes, SWT.BORDER | SWT.READ_ONLY);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		cmbQueueLocation.setLayoutData(gridData);
		updateQueueLocationCombo();
		cmbQueueLocation.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setSelectedQueueLocation(cmbQueueLocation.getSelectionIndex());
			}
		});

		if ( TagManagerFactory.getTagManager().isEnabled()){
			
				// tag area
			
			Composite tagLeft 	= new Composite( cTorrentModes, SWT.NULL);
			tagLeft.setLayoutData( new GridData(GridData.VERTICAL_ALIGN_CENTER ));
			Composite tagRight 	= new Composite( cTorrentModes, SWT.NULL);
			gridData = new GridData(GridData.FILL_HORIZONTAL );
			gridData.horizontalSpan=3;
			tagRight.setLayoutData(gridData);
			
			layout = new GridLayout();
			layout.numColumns = 1;
			layout.marginWidth  = 0;
			layout.marginHeight = 0;
			tagLeft.setLayout(layout);
			
			layout = new GridLayout();
			layout.numColumns = 2;
			layout.marginWidth  = 0;
			layout.marginHeight = 0;
			tagRight.setLayout(layout);
			
			label = new Label(tagLeft, SWT.NONE);
			gridData = new GridData(GridData.VERTICAL_ALIGN_CENTER);
			Messages.setLanguageText(label, "label.initial_tags");
	
			
			Composite tagButtons 	= new Composite( tagRight, SWT.NULL);
			gridData = new GridData(GridData.FILL_HORIZONTAL );
			tagButtons.setLayoutData(gridData);
	
			RowLayout tagLayout = new RowLayout();
			tagLayout.pack = false;
			tagButtons.setLayout( tagLayout);
			
			buildTagButtonPanel( tagButtons );
			
			Button addTag = new Button( tagRight, SWT.NULL );
			addTag.setLayoutData( new GridData(GridData.VERTICAL_ALIGN_CENTER ));
			Messages.setLanguageText( addTag, "label.add.tag" );
			
			addTag.addSelectionListener(
				new SelectionAdapter() {
					
					public void 
					widgetSelected(
						SelectionEvent e) 
					{
						TagUIUtils.createManualTag();
					}
				});
		}
	}
	
	private void
	buildTagButtonPanel(
		Composite	parent )
	{
		buildTagButtonPanel( parent, false );
	}
	
	private void
	buildTagButtonPanel(
		final 	Composite	parent,
		boolean	is_rebuild )
	{
		final String SP_KEY = "oto:tag:initsp";
				
		if ( is_rebuild ){
			
			Utils.disposeComposite( parent, false );
			
		}else{
			
			parent.setData( SP_KEY, getSavePath());
		}
		
		final TagType tt = TagManagerFactory.getTagManager().getTagType( TagType.TT_DOWNLOAD_MANUAL );
		
		List<Tag> initialTags = torrentOptions.getInitialTags();

		for ( final Tag tag: TagUIUtils.sortTags( tt.getTags())){
			
			if ( tag.isPublic()){
			
				final Button but = new Button( parent, SWT.TOGGLE );
			
				but.setText( tag.getTagName( true ));
				
				but.setToolTipText( TagUIUtils.getTagTooltip(tag));
				
				if ( initialTags.contains( tag )){
					
					but.setSelection( true );
				}
				
				but.addSelectionListener(
					new SelectionAdapter() {
						
						public void 
						widgetSelected(
							SelectionEvent e) 
						{
							List<Tag>  tags = torrentOptions.getInitialTags();
							
							if ( but.getSelection()){
								
								tags.add( tag );
								
								TagFeatureFileLocation fl = (TagFeatureFileLocation)tag;

								if ( fl.supportsTagInitialSaveFolder()){
									
									File save_loc = fl.getTagInitialSaveFolder();
									
									if ( save_loc != null ){
										
										setSavePath( save_loc.getAbsolutePath());
									}
								}
							}else{
								
								tags.remove( tag );
								
								TagFeatureFileLocation fl = (TagFeatureFileLocation)tag;

								if ( fl.supportsTagInitialSaveFolder()){
									
									File save_loc = fl.getTagInitialSaveFolder();
									
									if ( save_loc != null && getSavePath().equals( save_loc.getAbsolutePath())){
										
										String old = (String)parent.getData( SP_KEY );
										
										if ( old != null ){
										
											setSavePath( old );
										}
									}
								}
							}
							
							torrentOptions.setInitialTags( tags );
							
							updateStartOptionsHeader();
						}
					});
				
				Menu menu = new Menu( but );
				
				but.setMenu( menu );
				
				TagUIUtils.createSideBarMenuItems(menu, tag);
			}
		}
		
		if ( is_rebuild ){
		
			parent.getParent().layout( true,  true );
			
			return;
		}
		
		tt.addTagTypeListener(
			new TagTypeListener()
			{
				
				public void 
				tagTypeChanged(
					TagType tag_type) 
				{					
				}
				
				public void 
				tagRemoved(
					Tag tag ) 
				{
					rebuild();
				}
				
				public void 
				tagChanged(
					Tag tag) 
				{
					rebuild();
				}
				
				public void 
				tagAdded(
					Tag tag) 
				{
					rebuild();
				}
				
				private void
				rebuild()
				{
					if ( parent.isDisposed()){
						
						tt.removeTagTypeListener( this );
					}else{
						
						buildTagButtonPanel( parent, true );
					}
				}
			}, false );
	}

	private void setupTVFiles(SWTSkinObjectContainer soFilesTable) {
		TableColumnManager tcm = TableColumnManager.getInstance();
		if (tcm.getDefaultColumnNames(TABLEID_FILES) == null) {
			tcm.registerColumn(TorrentOpenFileOptions.class,
					TableColumnOTOF_Position.COLUMN_ID,
					new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new TableColumnOTOF_Position(column);
						}
					});
			tcm.registerColumn(TorrentOpenFileOptions.class,
					TableColumnOTOF_Download.COLUMN_ID,
					new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new TableColumnOTOF_Download(column);
						}
					});
			tcm.registerColumn(TorrentOpenFileOptions.class,
					TableColumnOTOF_Name.COLUMN_ID, new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new TableColumnOTOF_Name(column);
						}
					});
			tcm.registerColumn(TorrentOpenFileOptions.class,
					TableColumnOTOF_Size.COLUMN_ID, new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new TableColumnOTOF_Size(column);
						}
					});
			tcm.registerColumn(TorrentOpenFileOptions.class,
					TableColumnOTOF_Path.COLUMN_ID, new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new TableColumnOTOF_Path(column);
						}
					});
			tcm.registerColumn(TorrentOpenFileOptions.class,
					TableColumnOTOF_Ext.COLUMN_ID, new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new TableColumnOTOF_Ext(column);
						}
					});
			tcm.setDefaultColumnNames(TABLEID_FILES, new String[] {
				TableColumnOTOF_Position.COLUMN_ID,
				TableColumnOTOF_Download.COLUMN_ID,
				TableColumnOTOF_Name.COLUMN_ID,
				TableColumnOTOF_Size.COLUMN_ID,
				TableColumnOTOF_Path.COLUMN_ID
			});
			tcm.setDefaultSortColumnName(TABLEID_FILES, TableColumnOTOF_Position.COLUMN_ID);
		}

		tvFiles = TableViewFactory.createTableViewSWT(TorrentOpenFileOptions.class,
				TABLEID_FILES, TABLEID_FILES, null, "#", SWT.BORDER
						| SWT.FULL_SELECTION | SWT.MULTI);
		tvFiles.initialize(soFilesTable.getComposite());
		tvFiles.setRowDefaultHeight(20);

		tvFiles.addKeyListener(new KeyListener() {

			public void keyPressed(KeyEvent e) {
			}

			public void keyReleased(KeyEvent e) {
				if (e.keyCode == SWT.SPACE) {
					TableRowCore focusedRow = tvFiles.getFocusedRow();
					TorrentOpenFileOptions tfi_focus = ((TorrentOpenFileOptions) focusedRow.getDataSource());
					boolean download = !tfi_focus.isToDownload();

					TableRowCore[] rows = tvFiles.getSelectedRows();
					for (TableRowCore row : rows) {
						TorrentOpenFileOptions tfi = ((TorrentOpenFileOptions) row.getDataSource());
						tfi.setToDownload(download);
					}
				}
				if (e.keyCode == SWT.F2 && (e.stateMask & SWT.MODIFIER_MASK) == 0) {
					TorrentOpenFileOptions[] infos = tvFiles.getSelectedDataSources().toArray(
							new TorrentOpenFileOptions[0]);
					renameFilenames(infos);
					e.doit = false;
					return;
				}
			}
		});

		tvFiles.addMenuFillListener(new TableViewSWTMenuFillListener() {

			public void fillMenu(String sColumnName, Menu menu) {
				MenuItem item;
				TableRowCore focusedRow = tvFiles.getFocusedRow();
				final TorrentOpenFileOptions tfi_focus = ((TorrentOpenFileOptions) focusedRow.getDataSource());
				boolean download = tfi_focus.isToDownload();

				item = new MenuItem(menu, SWT.CHECK);
				Messages.setLanguageText(item, "label.download.file");
				item.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						TableRowCore focusedRow = tvFiles.getFocusedRow();
						TorrentOpenFileOptions tfi_focus = ((TorrentOpenFileOptions) focusedRow.getDataSource());
						boolean download = !tfi_focus.isToDownload();

						TorrentOpenFileOptions[] infos = tvFiles.getSelectedDataSources().toArray(
								new TorrentOpenFileOptions[0]);
						for (TorrentOpenFileOptions options : infos) {
							options.setToDownload(download);
						}
					}
				});
				item.setSelection(download);

				item = new MenuItem(menu, SWT.PUSH);
				Messages.setLanguageText(item, "FilesView.menu.rename_only");
				item.addSelectionListener(new SelectionAdapter() {

					public void widgetSelected(SelectionEvent e) {
						TorrentOpenFileOptions[] infos = tvFiles.getSelectedDataSources().toArray(
								new TorrentOpenFileOptions[0]);
						renameFilenames(infos);
					}

				});

				item = new MenuItem(menu, SWT.PUSH);
				Messages.setLanguageText(item,
						"OpenTorrentWindow.fileList.changeDestination");
				item.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						TorrentOpenFileOptions[] infos = tvFiles.getSelectedDataSources().toArray(
								new TorrentOpenFileOptions[0]);
						changeFileDestination(infos);
					}

				});

				new MenuItem(menu, SWT.SEPARATOR);

				item = new MenuItem(menu, SWT.PUSH);
				Messages.setLanguageText(item, "Button.selectAll");
				item.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						tvFiles.selectAll();
					}
				});

				
				String dest_path = tfi_focus.getDestPathName();
				String parentDir = tfi_focus.parent.getParentDir();
				
				List<String> folder_list = new ArrayList<String>();
				
				folder_list.add( dest_path );

				if ( dest_path.startsWith( parentDir ) && dest_path.length() > parentDir.length()){
										
					String relativePath = dest_path.substring( parentDir.length() + 1 );
					
					while ( relativePath.contains( File.separator )){
						
						int	pos = relativePath.lastIndexOf( File.separator );
						
						relativePath = relativePath.substring( 0,  pos );
						
						folder_list.add( parentDir + File.separator + relativePath );
					}
				}
				
				for ( int i=folder_list.size()-1;i>=0;i-- ){
					
					final String this_dest_path = folder_list.get(i);
							
					item = new MenuItem(menu, SWT.PUSH);
					Messages.setLanguageText(item, "menu.selectfilesinfolder", new String[] {
							this_dest_path
					});
					
					item.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {							
							TableRowCore[] rows = tvFiles.getRows();
							for (TableRowCore row : rows) {
								Object dataSource = row.getDataSource();
								if (dataSource instanceof TorrentOpenFileOptions) {
									TorrentOpenFileOptions fileOptions = (TorrentOpenFileOptions) dataSource;
									if ( fileOptions.getDestPathName().startsWith( this_dest_path )){
										row.setSelected(true);
									}
								}
							}
	
						}
					});
				}

			}

			public void addThisColumnSubMenu(String sColumnName, Menu menuThisColumn) {
			}
		});
		
		tvFiles.addSelectionListener(new TableSelectionListener() {
			
			public void selected(TableRowCore[] row) {
				updateFileButtons();
			}
			
			public void mouseExit(TableRowCore row) {
			}
			
			public void mouseEnter(TableRowCore row) {
			}
			
			public void focusChanged(TableRowCore focus) {
			}
			
			public void deselected(TableRowCore[] rows) {
				updateFileButtons();
			}
			
			public void defaultSelected(TableRowCore[] rows, int stateMask) {
			}
		}, false);
		

		tvFiles.addDataSources(torrentOptions.getFiles());
	}

	protected void updateFileButtons() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				boolean hasRowsSelected = tvFiles.getSelectedRowsSize() > 0;
				if (btnRename != null && !btnRename.isDisposed()) {
					btnRename.setEnabled(hasRowsSelected);
				}
				if (btnRetarget != null && !btnRetarget.isDisposed()) {
					btnRetarget.setEnabled(hasRowsSelected);
				}
				if (btnSwarmIt != null && !btnSwarmIt.isDisposed()){
					boolean	enable=false;
					TableRowCore[] rows = tvFiles.getSelectedRows();
					if ( rows.length == 1 ){
						TorrentOpenFileOptions tfi = ((TorrentOpenFileOptions) rows[0].getDataSource());	
						enable = tfi.lSize >= 50*1024*1024;
					}
					btnSwarmIt.setEnabled(enable);
				}
			}
		});
	}

	protected void renameFilenames(TorrentOpenFileOptions[] infos) {
		for (TorrentOpenFileOptions torrentFileInfo : infos) {
			String renameFilename = askForRenameFilename(torrentFileInfo);
			if (renameFilename == null) {
				break;
			}
			torrentFileInfo.setDestFileName(renameFilename);
			TableRowCore row = tvFiles.getRow(torrentFileInfo);
			if (row != null) {
				row.invalidate(true);
				row.refresh(true);
			}
		}
	}

	private static String askForRenameFilename(TorrentOpenFileOptions fileInfo) {
		SimpleTextEntryWindow dialog = new SimpleTextEntryWindow(
				"FilesView.rename.filename.title", "FilesView.rename.filename.text");
		dialog.setPreenteredText(fileInfo.orgFileName, false); // false -> it's not "suggested", it's a previous value
		dialog.allowEmptyInput(false);
		dialog.prompt();
		if (!dialog.hasSubmittedInput()) {
			return null;
		}
		return dialog.getSubmittedInput();
	}

	private void changeFileDestination(TorrentOpenFileOptions[] infos) {

		for (TorrentOpenFileOptions fileInfo : infos) {
			int style = (fileInfo.parent.iStartID == TorrentOpenOptions.STARTMODE_SEEDING)
					? SWT.OPEN : SWT.SAVE;
			FileDialog fDialog = new FileDialog(dlg.getShell(), SWT.SYSTEM_MODAL
					| style);

			String sFilterPath = fileInfo.getDestPathName();
			String sFileName = fileInfo.orgFileName;

			File f = new File(sFilterPath);
			if (!f.isDirectory()) {
				// Move up the tree until we have an existing path
				while (sFilterPath != null) {
					String parentPath = f.getParent();
					if (parentPath == null)
						break;

					sFilterPath = parentPath;
					f = new File(sFilterPath);
					if (f.isDirectory())
						break;
				}
			}

			if (sFilterPath != null)
				fDialog.setFilterPath(sFilterPath);
			fDialog.setFileName(sFileName);
			fDialog.setText(MessageText.getString("MainWindow.dialog.choose.savepath")
					+ " (" + fileInfo.orgFullName + ")");
			String sNewName = fDialog.open();

			if (sNewName == null)
				return;

			if (fileInfo.parent.iStartID == TorrentOpenOptions.STARTMODE_SEEDING) {
				File file = new File(sNewName);
				if (file.length() == fileInfo.lSize)
					fileInfo.setFullDestName(sNewName);
				else {
					MessageBoxShell mb = new MessageBoxShell(SWT.OK,
							"OpenTorrentWindow.mb.badSize", new String[] {
								file.getName(),
								fileInfo.orgFullName
							});
					mb.setParent(dlg.getShell());
					mb.open(null);
				}
			} else
				fileInfo.setFullDestName(sNewName);

		} // for i

		checkSeedingMode();
		updateDataDirCombo();
		diskFreeInfoRefreshPending = true;
	}

	/**
	 * 
	 * @since 4.6.0.5
	 */
	protected void swt_createWindow() {
		TOTorrent torrent = torrentOptions.getTorrent();
		final HashWrapper hash;
		if ( torrent == null ){
			Debug.out( "Hmm, no torren?" );
			hash = null;
		}else{
			HashWrapper hw = null;
			try{
				hw = torrent.getHashWrapper();
				
				OpenTorrentOptionsWindow existing = active_windows.get( hw );
					
				if ( existing != null ){
										
					existing.activate();
					
					return;
				}
				active_windows.put( hw,  this );
			}catch( Throwable e ){
				Debug.out( e );
			}
			hash = hw;
		}
		
		try{
			dlg = new SkinnedDialog("skin3_dlg_opentorrent_options", "shell",
					SWT.RESIZE | SWT.MAX | SWT.DIALOG_TRIM);
	
			dlg.setTitle(MessageText.getString("OpenTorrentOptions.title") + " [" + torrentOptions.getTorrentName() + "]");
			final SWTSkin skin = dlg.getSkin();
			
			SWTSkinObject so;
			
			if (COConfigurationManager.hasParameter(ConfigurationDefaults.CFG_TORRENTADD_OPENOPTIONS, true)) {
	  		so = skin.getSkinObject("showagain-area");
	  		if (so != null) {
	  			so.setVisible(false);
	  		}
			}
			
			SWTSkinObject soButtonArea = skin.getSkinObject("button-area");
			if (soButtonArea instanceof SWTSkinObjectContainer) {
				buttonsArea = new StandardButtonsArea() {
					protected void clicked(int intValue) {
						String dataDir = cmbDataDir.getText();
						if (intValue == SWT.OK) {
							if (okPressed(dataDir)) {
								if (dlg != null) {
									dlg.close();
								}
							}
						} else if (dlg != null) {
							dlg.close();
						}
					}
				};
				buttonsArea.setButtonIDs(new String[] {
					MessageText.getString("Button.ok"),
					MessageText.getString("Button.cancel")
				});
				buttonsArea.setButtonVals(new Integer[] {
					SWT.OK,
					SWT.CANCEL
				});
				buttonsArea.swt_createButtons(((SWTSkinObjectContainer) soButtonArea).getComposite());
			}
	
			so = skin.getSkinObject("filearea-table");
			if (so instanceof SWTSkinObjectContainer) {
				setupTVFiles((SWTSkinObjectContainer) so);
			}
	
			so = skin.getSkinObject("filearea-buttons");
			if (so instanceof SWTSkinObjectContainer) {
				setupFileAreaButtons((SWTSkinObjectContainer) so);
			}
	
			so = skin.getSkinObject("disk-space");
			if (so instanceof SWTSkinObjectContainer) {
				diskspaceComp = (Composite) so.getControl();
				GridLayout gl = new GridLayout(2, false);
				gl.marginHeight = gl.marginWidth = 0;
				diskspaceComp.setLayout(gl);
			}
	
			so = skin.getSkinObject("filearea-info");
			if (so instanceof SWTSkinObjectText) {
				setupFileAreaInfo((SWTSkinObjectText) so);
			}
	
			so = skin.getSkinObject("start-options");
			if (so instanceof SWTSkinObjectExpandItem) {
				setupStartOptions((SWTSkinObjectExpandItem) so);
			}
	
			so = skin.getSkinObject("peer-sources");
			if (so instanceof SWTSkinObjectContainer) {
				setupPeerSourcesOptions((SWTSkinObjectContainer) so);
			}
	
			so = skin.getSkinObject("ipfilter");
			if (so instanceof SWTSkinObjectContainer) {
				setupIPFilterOption((SWTSkinObjectContainer) so);
			}
	
			SWTSkinObject so1 = skin.getSkinObject("saveto-textarea");
			SWTSkinObject so2 = skin.getSkinObject("saveto-browse");
			if ((so1 instanceof SWTSkinObjectContainer)
					&& (so2 instanceof SWTSkinObjectButton)) {
				setupSaveLocation((SWTSkinObjectContainer) so1, (SWTSkinObjectButton) so2);
			}
	
			so = skin.getSkinObject("expanditem-saveto");
			if (so instanceof SWTSkinObjectExpandItem) {
				soExpandItemSaveTo = (SWTSkinObjectExpandItem) so;
			}
	
			so = skin.getSkinObject("expanditem-files");
			if (so instanceof SWTSkinObjectExpandItem) {
				soExpandItemFiles = (SWTSkinObjectExpandItem) so;
			}
	
			so = skin.getSkinObject("expanditem-peer");
			if (so instanceof SWTSkinObjectExpandItem) {
				soExpandItemPeer = (SWTSkinObjectExpandItem) so;
			}
	
			so = skin.getSkinObject("expanditem-torrentinfo");
			if (so instanceof SWTSkinObjectExpandItem) {
				soExpandItemTorrentInfo = (SWTSkinObjectExpandItem) so;
				soExpandItemTorrentInfo.setText(MessageText.getString("OpenTorrentOptions.header.torrentinfo")
						+ ": " + torrentOptions.getTorrentName());
			}
			
			setupShowAgainOptions(skin);
	
			setupInfoFields(skin);
	
			updateStartOptionsHeader();
			cmbDataDirChanged();
			updateSize();
	
			UIUpdaterSWT.getInstance().addUpdater(this);
			
				/* 
				 * The bring-to-front logic for torrent addition is controlled by other parts of the code so we don't
				 * want the dlg to override this behaviour (main example here is torrents passed from, say, a browser,
				 * and the user has disabled the 'show vuze on external torrent add' feature)
				 */
			
			dlg.open("otow",false);
	
			int	num_active_windows = active_windows.size();
			
			if ( num_active_windows > 1 ){
				
				int	max_x = 0;
				int max_y = 0;
				
				for ( OpenTorrentOptionsWindow window: active_windows.values()){
					
					if ( window == this ){
						
						continue;
					}
					
					Rectangle rect = window.getBounds();
					
					max_x = Math.max( max_x, rect.x );
					max_y = Math.max( max_y, rect.y );
				}
				
				Shell shell = dlg.getShell();
				
				Rectangle rect = shell.getBounds();
				
				rect.x = max_x + 16;
				rect.y = max_y + 16;
				
				dlg.getShell().setBounds( rect );
			}
			
			dlg.addCloseListener(new SkinnedDialogClosedListener() {
				public void skinDialogClosed(SkinnedDialog dialog) {
					try{
						dispose();
						
					}finally{
						
						if ( hash != null ){
							
							active_windows.remove( hash );
						}
					}
				}
			});
		}catch( Throwable e ){
			
			Debug.out( e );
			
			if ( hash != null ){
				
				active_windows.remove( hash );
			}
		}
	}

	private void
	activate()
	{
		Shell shell = dlg.getShell();
		
		if ( !shell.isDisposed()){
		
			shell.forceActive();
		}
	}
	
	private Rectangle
	getBounds()
	{
		return( dlg.getShell().getBounds());
	}
	
	private void setupShowAgainOptions(SWTSkin skin) {
		SWTSkinObjectCheckbox soNever = (SWTSkinObjectCheckbox) skin.getSkinObject("showagain-never");
		SWTSkinObjectCheckbox soAlways = (SWTSkinObjectCheckbox) skin.getSkinObject("showagain-always");
		SWTSkinObjectCheckbox soMany = (SWTSkinObjectCheckbox) skin.getSkinObject("showagain-manyfile");

		String showAgainMode = COConfigurationManager.getStringParameter(ConfigurationDefaults.CFG_TORRENTADD_OPENOPTIONS);
		boolean hasUserChosen = COConfigurationManager.hasParameter(ConfigurationDefaults.CFG_TORRENTADD_OPENOPTIONS, true);

		if (soNever != null) {
			soNever.addSelectionListener(new SWTSkinCheckboxListener() {
				public void checkboxChanged(SWTSkinObjectCheckbox so, boolean checked) {
					COConfigurationManager.setParameter(ConfigurationDefaults.CFG_TORRENTADD_OPENOPTIONS, ConfigurationDefaults.CFG_TORRENTADD_OPENOPTIONS_NEVER);
				}
			});
			if (hasUserChosen) {
				soNever.setChecked(ConfigurationDefaults.CFG_TORRENTADD_OPENOPTIONS_NEVER.equals(showAgainMode));
			}
		}

		if (soAlways != null) {
			soAlways.addSelectionListener(new SWTSkinCheckboxListener() {
				public void checkboxChanged(SWTSkinObjectCheckbox so, boolean checked) {
					COConfigurationManager.setParameter(ConfigurationDefaults.CFG_TORRENTADD_OPENOPTIONS, ConfigurationDefaults.CFG_TORRENTADD_OPENOPTIONS_ALWAYS);
				}
			});
			if (hasUserChosen) {
				soAlways.setChecked(ConfigurationDefaults.CFG_TORRENTADD_OPENOPTIONS_ALWAYS.equals(showAgainMode));
			}
		}

		if (soMany != null) {
			soMany.addSelectionListener(new SWTSkinCheckboxListener() {
				public void checkboxChanged(SWTSkinObjectCheckbox so, boolean checked) {
					COConfigurationManager.setParameter(ConfigurationDefaults.CFG_TORRENTADD_OPENOPTIONS, ConfigurationDefaults.CFG_TORRENTADD_OPENOPTIONS_MANY);
				}
			});
			if (hasUserChosen) {
				soMany.setChecked(ConfigurationDefaults.CFG_TORRENTADD_OPENOPTIONS_MANY.equals(showAgainMode));
			}
		}
}

	private void setupInfoFields(SWTSkin skin) {
		SWTSkinObject so;
		so = skin.getSkinObject("torrentinfo-name");
		if (so instanceof SWTSkinObjectText) {
			((SWTSkinObjectText) so).setText(torrentOptions.getTorrentName());
		}

		so = skin.getSkinObject("torrentinfo-trackername");
		if (so instanceof SWTSkinObjectText) {
			((SWTSkinObjectText) so).setText(TrackerNameItem.getTrackerName(torrentOptions.getTorrent()));
		}

		so = skin.getSkinObject("torrentinfo-comment");
		if (so instanceof SWTSkinObjectText) {

			try {
				LocaleUtilDecoder decoder = LocaleTorrentUtil.getTorrentEncoding(torrentOptions.getTorrent());
				String s = decoder.decodeString(torrentOptions.getTorrent().getComment());
				((SWTSkinObjectText) so).setText(s);
			} catch (UnsupportedEncodingException e) {
			} catch (TOTorrentException e) {
			}
		}

		so = skin.getSkinObject("torrentinfo-createdon");
		if (so instanceof SWTSkinObjectText) {
			String creation_date = DisplayFormatters.formatDate(torrentOptions.getTorrent().getCreationDate());
			((SWTSkinObjectText) so).setText(creation_date);
		}

	}

	private void setupIPFilterOption(SWTSkinObjectContainer so) {
		Composite parent = so.getComposite();

		Button button = new Button(parent, SWT.CHECK | SWT.WRAP);
		Messages.setLanguageText(button, "MyTorrentsView.menu.ipf_enable");

		button.setSelection(!torrentOptions.disableIPFilter);

		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				torrentOptions.disableIPFilter = !((Button) e.widget).getSelection();
			}
		});

	}

	private void setupPeerSourcesOptions(SWTSkinObjectContainer so) {
		Composite parent = so.getComposite();

		Group peer_sources_group = new Group(parent, SWT.NULL);
		Messages.setLanguageText(peer_sources_group,
				"ConfigView.section.connection.group.peersources");
		GridLayout peer_sources_layout = new GridLayout(3, true);
		peer_sources_group.setLayout(peer_sources_layout);

		peer_sources_group.setLayoutData(Utils.getFilledFormData());

		//		Label label = new Label(peer_sources_group, SWT.WRAP);
		//		Messages.setLanguageText(label,
		//				"ConfigView.section.connection.group.peersources.info");
		//		GridData gridData = new GridData();
		//		label.setLayoutData(gridData);

		for (int i = 0; i < PEPeerSource.PS_SOURCES.length; i++) {

			final String p = PEPeerSource.PS_SOURCES[i];

			String config_name = "Peer Source Selection Default." + p;
			String msg_text = "ConfigView.section.connection.peersource." + p;

			Button button = new Button(peer_sources_group, SWT.CHECK);
			Messages.setLanguageText(button, msg_text);

			button.setSelection(COConfigurationManager.getBooleanParameter(config_name));
			
			button.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					torrentOptions.peerSource.put(p, ((Button)e.widget).getSelection());
				}
			});

			GridData gridData = new GridData();
			button.setLayoutData(gridData);
		}

	}

	private void updateDataDirCombo() {

		if (cmbDataDir == null) {
			return;
		}

		try {
			bSkipDataDirModify = true;

			cmbDataDir.setText(torrentOptions.getParentDir());
		} finally {
			bSkipDataDirModify = false;
		}
	}

	private void
	setSavePath(
		String	path )
	{		
		cmbDataDir.setText( path );
	}
	
	private String
	getSavePath()
	{
		return( torrentOptions.getParentDir());
	}
	
	private void updateQueueLocationCombo() {
		if (cmbQueueLocation == null)
			return;

		String[] sItemsText = new String[queueLocations.length];
		for (int i = 0; i < queueLocations.length; i++) {
			String sText = MessageText.getString("OpenTorrentWindow.addPosition."
					+ queueLocations[i]);
			sItemsText[i] = sText;
		}
		cmbQueueLocation.setItems(sItemsText);
		cmbQueueLocation.select(torrentOptions.iQueueLocation);
	}

	private void updateSize() {

		if (soFileAreaInfo == null && soExpandItemFiles == null) {
			return;
		}

		/*
		 * determine info for selected torrents only
		 */
		long totalSize = 0;
		long checkedSize = 0;
		long numToDownload = 0;

		TorrentOpenFileOptions[] dataFiles = torrentOptions.getFiles();
		for (TorrentOpenFileOptions file : dataFiles) {
			totalSize += file.lSize;

			if (file.isToDownload()) {
				checkedSize += file.lSize;
				numToDownload++;
			}
		}

		String text;
		// build string and set label
		if (totalSize == 0) {
			text = "";
		} else if (checkedSize == totalSize) {
			text = DisplayFormatters.formatByteCountToKiBEtc(totalSize);
		} else {
			text = MessageText.getString("OpenTorrentWindow.filesInfo", new String[] {
				DisplayFormatters.formatByteCountToKiBEtc(checkedSize),
				DisplayFormatters.formatByteCountToKiBEtc(totalSize)
			});
		}

		if (soFileAreaInfo != null) {
			soFileAreaInfo.setText(text);
		}
		if (soExpandItemFiles != null) {
			String id = "OpenTorrentOptions.header.filesInfo."
					+ (numToDownload == dataFiles.length ? "all" : "some");
			soExpandItemFiles.setText(MessageText.getString(id, new String[] {
				String.valueOf(numToDownload),
				String.valueOf(dataFiles.length),
				text
			}));
		}

		diskFreeInfoRefreshPending = true;

	}

	private void updateStartModeCombo() {
		if (cmbStartMode == null)
			return;

		String[] sItemsText = new String[startModes.length];
		for (int i = 0; i < startModes.length; i++) {
			String sText = MessageText.getString("OpenTorrentWindow.startMode."
					+ startModes[i]);
			sItemsText[i] = sText;
		}
		cmbStartMode.setItems(sItemsText);
		cmbStartMode.select(torrentOptions.iStartID);
		cmbStartMode.layout(true);
	}

	public void updateUI() {
		tvFiles.refreshTable(false);

		if (diskFreeInfoRefreshPending && !diskFreeInfoRefreshRunning
				&& FileUtil.getUsableSpaceSupported()) {
			diskFreeInfoRefreshRunning = true;
			diskFreeInfoRefreshPending = false;

			final HashSet FSroots = new HashSet(Arrays.asList(File.listRoots()));
			final HashMap partitions = new HashMap();

			TorrentOpenFileOptions[] files = torrentOptions.getFiles();
			for (int j = 0; j < files.length; j++) {
				TorrentOpenFileOptions file = files[j];
				if (!file.isToDownload())
					continue;

				// reduce each file to its partition root
				File root = file.getDestFileFullName().getAbsoluteFile();

				Partition part = (Partition) partitions.get(parentToRootCache.get(root.getParentFile()));

				if (part == null) {
					File next;
					while (true) {
						root = root.getParentFile();
						next = root.getParentFile();
						if (next == null)
							break;

						// bubble up until we hit an existing directory
						if (!getCachedExistsStat(root) || !root.isDirectory())
							continue;

						// check for mount points (different free space) or simple loops in the directory structure
						if (FSroots.contains(root) || root.equals(next)
								|| getCachedDirFreeSpace(next) != getCachedDirFreeSpace(root))
							break;
					}

					parentToRootCache.put(
							file.getDestFileFullName().getAbsoluteFile().getParentFile(),
							root);

					part = (Partition) partitions.get(root);

					if (part == null) {
						part = new Partition(root);
						part.freeSpace = getCachedDirFreeSpace(root);
						partitions.put(root, part);
					}
				}

				part.bytesToConsume += file.lSize;
			}

			// clear child objects
			if (diskspaceComp != null && !diskspaceComp.isDisposed()) {
				Control[] labels = diskspaceComp.getChildren();
				for (int i = 0; i < labels.length; i++)
					labels[i].dispose();

				// build labels
				Iterator it = partitions.values().iterator();
				while (it.hasNext()) {
					Partition part = (Partition) it.next();

					boolean filesTooBig = part.bytesToConsume > part.freeSpace;
					
					String s = MessageText.getString("OpenTorrentWindow.diskUsage",
							new String[] {
						DisplayFormatters.formatByteCountToKiBEtc(part.bytesToConsume),
						DisplayFormatters.formatByteCountToKiBEtc(part.freeSpace)
					});
					
					Label l;
					l = new Label(diskspaceComp, SWT.NONE);
					l.setForeground(filesTooBig ? Colors.colorError : null);
					l.setText(part.root.getPath());
					l.setLayoutData(new GridData(SWT.END, SWT.TOP, false, false));

					l = new Label(diskspaceComp, SWT.NONE);
					l.setForeground(filesTooBig ? Colors.colorError : null);
					l.setLayoutData(new GridData(SWT.END, SWT.TOP, false, false));
					l.setText(s);
				}

				// hack to force resize :(
				diskspaceComp.layout(true);
				soExpandItemSaveTo.relayout();
			}

			diskFreeInfoRefreshRunning = false;
		}
	}

	protected boolean okPressed(String dataDir) {
		File file = new File(dataDir);

		File fileDefSavePath = new File(
				COConfigurationManager.getStringParameter(PARAM_DEFSAVEPATH));

		if (file.equals(fileDefSavePath) && !fileDefSavePath.isDirectory()) {
			FileUtil.mkdirs(fileDefSavePath);
		}

		boolean isPathInvalid = dataDir.length() == 0 || file.isFile();
		if (!isPathInvalid && !file.isDirectory()) {
			MessageBoxShell mb = new MessageBoxShell(SWT.YES | SWT.NO
					| SWT.ICON_QUESTION, "OpenTorrentWindow.mb.askCreateDir",
					new String[] {
						file.toString()
					});
			mb.setParent(dlg.getShell());
			mb.open(null);
			int doCreate = mb.waitUntilClosed();

			if (doCreate == SWT.YES)
				isPathInvalid = !FileUtil.mkdirs(file);
			else {
				cmbDataDir.setFocus();
				return false;
			}
		}

		if (isPathInvalid) {
			MessageBoxShell mb = new MessageBoxShell(SWT.OK | SWT.ICON_ERROR,
					"OpenTorrentWindow.mb.noGlobalDestDir", new String[] {
						file.toString()
					});
			mb.setParent(dlg.getShell());
			mb.open(null);
			cmbDataDir.setFocus();
			return false;
		}

		String sExistingFiles = "";
		int iNumExistingFiles = 0;

		file = new File(torrentOptions.getDataDir());

		// Need to make directory now, or single file torrent will take the 
		// "dest dir" as their filename.  ie:
		// 1) Add single file torrent with named "hi.exe"
		// 2) type a non-existant directory c:\test\moo
		// 3) unselect the torrent
		// 4) change the global def directory to a real one
		// 5) click ok.  "hi.exe" will be written as moo in c:\test			
		if (!file.isDirectory() && !FileUtil.mkdirs(file)) {
			MessageBoxShell mb = new MessageBoxShell(SWT.OK | SWT.ICON_ERROR,
					"OpenTorrentWindow.mb.noDestDir", new String[] {
						file.toString(),
						torrentOptions.getTorrentName()
					});
			mb.setParent(dlg.getShell());
			mb.open(null);
			return false;
		}

		if (!torrentOptions.isValid) {
			MessageBoxShell mb = new MessageBoxShell(SWT.OK | SWT.ICON_ERROR,
					"OpenTorrentWindow.mb.notValid", new String[] {
						torrentOptions.getTorrentName()
					});
			mb.setParent(dlg.getShell());
			mb.open(null);
			return false;
		}

		TorrentOpenFileOptions[] files = torrentOptions.getFiles();
		for (int j = 0; j < files.length; j++) {
			TorrentOpenFileOptions fileInfo = files[j];
			if (fileInfo.getDestFileFullName().exists()) {
				sExistingFiles += fileInfo.orgFullName + " - "
						+ torrentOptions.getTorrentName() + "\n";
				iNumExistingFiles++;
				if (iNumExistingFiles > 5) {
					// this has the potential effect of adding 5 files from the first 
					// torrent and then 1 file from each of the remaining torrents
					break;
				}
			}
		}

		if (sExistingFiles.length() > 0) {
			if (iNumExistingFiles > 5) {
				sExistingFiles += MessageText.getString(
						"OpenTorrentWindow.mb.existingFiles.partialList", new String[] {
							"" + iNumExistingFiles
						}) + "\n";
			}

			MessageBoxShell mb = new MessageBoxShell(SWT.OK | SWT.CANCEL
					| SWT.ICON_WARNING, "OpenTorrentWindow.mb.existingFiles",
					new String[] {
						sExistingFiles
					});
			mb.setParent(dlg.getShell());
			mb.open(null);
			if (mb.waitUntilClosed() != SWT.OK) {
				return false;
			}
		}

		String sDefaultPath = COConfigurationManager.getStringParameter(PARAM_DEFSAVEPATH);
		if (!torrentOptions.getParentDir().equals(sDefaultPath)) {
			// Move sDestDir to top of list

			// First, check to see if sDestDir is already in the list
			File fDestDir = new File(torrentOptions.getParentDir());
			int iDirPos = -1;
			for (int i = 0; i < dirList.size(); i++) {
				String sDirName = dirList.get(i);
				File dir = new File(sDirName);
				if (dir.equals(fDestDir)) {
					iDirPos = i;
					break;
				}
			}

			// If already in list, remove it
			if (iDirPos > 0 && iDirPos < dirList.size())
				dirList.remove(iDirPos);

			// and add it to the top
			dirList.add(0, torrentOptions.getParentDir());

			// Limit
			if (dirList.size() > 15)
				dirList.remove(dirList.size() - 1);

			// Temporary list cleanup
			try {
				for (int j = 0; j < dirList.size(); j++) {
					File dirJ = new File(dirList.get(j));
					for (int i = 0; i < dirList.size(); i++) {
						try {
							if (i == j)
								continue;

							File dirI = new File(dirList.get(i));

							if (dirI.equals(dirJ)) {
								dirList.remove(i);
								// dirList shifted up, fix indexes
								if (j > i)
									j--;
								i--;
							}
						} catch (Exception e) {
							// Ignore
						}
					}
				}
			} catch (Exception e) {
				// Ignore
			}

			COConfigurationManager.setParameter("saveTo_list", dirList);
			COConfigurationManager.save();
		}

		if (COConfigurationManager.getBooleanParameter("DefaultDir.AutoUpdate")) {
			COConfigurationManager.setParameter(PARAM_DEFSAVEPATH,
					torrentOptions.getParentDir());
		}

		openTorrents();

		return true;
	}

	/**
	 * Open the torrents already added based on user choices
	 * 
	 * @param sDataDir 
	 */
	private void openTorrents() {
		Utils.getOffOfSWTThread(new AERunnable() {
			public void runSupport() {
				TorrentOpener.addTorrent(torrentOptions);
			}
		});
	}

}
