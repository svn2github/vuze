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
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
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
import org.gudy.azureus2.plugins.ui.UIInputReceiver;
import org.gudy.azureus2.plugins.ui.UIInputReceiverListener;
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
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
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

	public static final String TABLEID_TORRENTS = "OpenTorrentTorrent";
	public static final String TABLEID_FILES 	= "OpenTorrentFile";



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

		File file1 = new File("C:\\temp\\test.torrent");
		File file2 = new File("C:\\temp\\test1.torrent");

		TOTorrent torrent1 = null;
		try {
			torrent1 = TOTorrentFactory.deserialiseFromBEncodedFile(file1);
		} catch (TOTorrentException e) {
			e.printStackTrace();
		};

		TOTorrent torrent2 = null;
		try {
			torrent2 = TOTorrentFactory.deserialiseFromBEncodedFile(file2);
		} catch (TOTorrentException e) {
			e.printStackTrace();
		};
		
		COConfigurationManager.setParameter( ConfigurationDefaults.CFG_TORRENTADD_OPENOPTIONS_SEP, false );
		
		OpenTorrentOptionsWindow window = addTorrent(
				new TorrentOpenOptions(null, torrent1, false));
		
		addTorrent(	new TorrentOpenOptions(null, torrent2, false));
		
		while (!window.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}

		core.stop();
	}

	
	private SkinnedDialog 			dlg;
	private ImageLoader 			image_loader;
	private SWTSkinObjectSash 		sash_object;
	private StackLayout				expand_stack;
	private	Composite 				expand_stack_area;
	private StandardButtonsArea 	buttonsArea;
	private boolean 				window_initialised;
	
	private Button	buttonTorrentUp;
	private Button	buttonTorrentDown;
	private Button	buttonTorrentRemove;
	
	private List<String>	images_to_dispose = new ArrayList<String>();
	
	
	private TableViewSWT<OpenTorrentInstance> 	tvTorrents;
	private Label								torrents_info_label;
	
	private OpenTorrentInstanceListener	optionListener;
	
	private List<OpenTorrentInstance>	open_instances = new ArrayList<OpenTorrentOptionsWindow.OpenTorrentInstance>();
	private OpenTorrentInstance			current_instance;
	
	public static OpenTorrentOptionsWindow
	addTorrent(
		final TorrentOpenOptions torrentOptions )
	{
		TOTorrent torrent = torrentOptions.getTorrent();
		
		try{
			final HashWrapper hw = torrent.getHashWrapper();
			
			synchronized( active_windows ){

				final OpenTorrentOptionsWindow existing = active_windows.get( hw );
				
				if ( existing != null ){
										
					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {
					
							existing.swt_activate();
						}
					});
					
					return( existing );
				}
				
				boolean	separate_dialogs = COConfigurationManager.getBooleanParameter( ConfigurationDefaults.CFG_TORRENTADD_OPENOPTIONS_SEP );
				
				if ( !separate_dialogs ){
					
					if ( active_windows.size() > 0 ){
						
						final OpenTorrentOptionsWindow reuse_window = active_windows.values().iterator().next();
													
						active_windows.put( hw,  reuse_window );
						
						Utils.execSWTThread(new AERunnable() {
							public void runSupport()
							{
								reuse_window.swt_addTorrent( hw, torrentOptions );
							}
						});
						
						
						return( reuse_window );
					}
				}
				
				final OpenTorrentOptionsWindow new_window = new OpenTorrentOptionsWindow();
				
				active_windows.put( hw,  new_window );
				
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
				
						new_window.swt_addTorrent( hw, torrentOptions );
					}
				});
				
				return( new_window );
			}
		}catch( Throwable e ){
			
			Debug.out( e );
			
			return( null );
		}

	}
	
	private
	OpenTorrentOptionsWindow()
	{
		image_loader = SWTSkinFactory.getInstance().getImageLoader(SWTSkinFactory.getInstance().getSkinProperties());

		optionListener = 
			new OpenTorrentInstanceListener()
		{
			public void 
			instanceChanged(
				OpenTorrentInstance instance )
			{
				updateInstanceInfo();
			}
		};
	}

	protected void 
	swt_addTorrent(
		HashWrapper				hash,
		TorrentOpenOptions		torrentOptions )
	{		
		try{
			if ( dlg == null ){
				
				dlg = new SkinnedDialog("skin3_dlg_opentorrent_options", "shell",
						SWT.RESIZE | SWT.MAX | SWT.DIALOG_TRIM);
		
				dlg.setTitle(MessageText.getString("OpenTorrentOptions.title") + " [" + torrentOptions.getTorrentName() + "]");
				
				final SWTSkin skin_outter = dlg.getSkin();
												
				SWTSkinObject so;
				
				if (COConfigurationManager.hasParameter(ConfigurationDefaults.CFG_TORRENTADD_OPENOPTIONS, true)) {
		  		
					so = skin_outter.getSkinObject("showagain-area");
					
					if (so != null) {
						so.setVisible(false);
					}
				}
					
				SWTSkinObject soButtonArea = skin_outter.getSkinObject("button-area");
				
				if (soButtonArea instanceof SWTSkinObjectContainer) {
					buttonsArea = new StandardButtonsArea() {
						protected void clicked(int intValue) {
							if (intValue == SWT.OK) {
								boolean	all_ok = true;
							
								AsyncDispatcher dispatcher = new AsyncDispatcher();
								
								for ( final OpenTorrentInstance instance: new ArrayList<OpenTorrentInstance>( open_instances )){
									
									String dataDir = instance.cmbDataDir.getText();
	
									if ( !instance.okPressed(dataDir)){
									
										all_ok = false;
										
									}else{
										
											// serialise additions in correct order
										
										dispatcher.dispatch(
											new AERunnable()
											{
												public void
												runSupport()
												{
													TorrentOpener.addTorrent( instance.getOptions());

												}
											});
										
										removeInstance( instance );
									}
								}
								
								if ( all_ok ){
									if (dlg != null){
										dlg.close();
									}
								}
							}else if (dlg != null){
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
					
				sash_object = (SWTSkinObjectSash)skin_outter.getSkinObject("multi-sash");
									
				SWTSkinObjectContainer select_area = (SWTSkinObjectContainer)skin_outter.getSkinObject( "torrents-table" );

				setupTVTorrents( select_area.getComposite());
				
				SWTSkinObjectContainer torrents_info = (SWTSkinObjectContainer)skin_outter.getSkinObject( "torrents-info" );

				Composite info_area = torrents_info.getComposite();
				
				info_area.setLayout( new GridLayout());
				
				torrents_info_label = new Label( info_area, SWT.NULL );
				torrents_info_label.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ));				
				
				sash_object.setVisible( false );
				sash_object.setAboveVisible( false );
				
				so = skin_outter.getSkinObject("expand-area");
		
				expand_stack_area = ((SWTSkinObjectContainer)so).getComposite();
		
				expand_stack	= new StackLayout();
				
				expand_stack_area.setLayout( expand_stack );
				
				Composite expand_area = new Composite( expand_stack_area, SWT.NULL );
				
				expand_area.setLayout( new FormLayout());
				
				expand_stack.topControl = expand_area;
				
				OpenTorrentInstance instance = new OpenTorrentInstance( hash, expand_area, torrentOptions, optionListener );

				addInstance( instance );
								
				current_instance = instance;
								
				UIUpdaterSWT.getInstance().addUpdater(this);
				
				setupShowAgainOptions(skin_outter);
	
					/* 
					 * The bring-to-front logic for torrent addition is controlled by other parts of the code so we don't
					 * want the dlg to override this behaviour (main example here is torrents passed from, say, a browser,
					 * and the user has disabled the 'show vuze on external torrent add' feature)
					 */
							
				dlg.open("otow",false);
				
				synchronized( active_windows ){
					
					int	num_active_windows = active_windows.size();
					
					if ( num_active_windows > 1 ){
						
						int	max_x = 0;
						int max_y = 0;
						
						for ( OpenTorrentOptionsWindow window: active_windows.values()){
							
							if ( window == this || !window.isInitialised()){
								
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
						
						shell.setBounds( rect );
												
						Utils.verifyShellRect( shell, true );
					}
				}
				
				dlg.addCloseListener(new SkinnedDialogClosedListener() {
					public void skinDialogClosed(SkinnedDialog dialog) {
						try{
							dispose();
							
						}finally{
							
							synchronized( active_windows ){
								
								Iterator<OpenTorrentOptionsWindow> it = active_windows.values().iterator();
								
								while( it.hasNext()){
									
									if ( it.next() == OpenTorrentOptionsWindow.this ){
										
										it.remove();
									}
								}
							}
						}
					}
				});
				
				window_initialised = true;
				
			}else{
				
				Composite expand_area = new Composite( expand_stack_area, SWT.NULL );
				
				expand_area.setLayout( new FormLayout());
								
				OpenTorrentInstance instance = new OpenTorrentInstance( hash, expand_area, torrentOptions, optionListener );

				addInstance( instance );
								
				dlg.setTitle(MessageText.getString("OpenTorrentOptions.title") + " [" + MessageText.getString("label.num.torrents",new String[]{ String.valueOf( open_instances.size())}) + "]");
				
				if ( !sash_object.isVisible()){
					
					sash_object.setVisible( true );
					
					sash_object.setAboveVisible( true );
					
					Utils.execSWTThreadLater(
						0,
						new Runnable()
						{
							public void
							run()
							{
								tvTorrents.processDataSourceQueueSync();
								
								TableRowCore row = tvTorrents.getRow( current_instance );
								
								if ( row != null ){
									
									tvTorrents.setSelectedRows( new TableRowCore[]{ row });
								}
							}
						});
						
				}
			}
		}catch( Throwable e ){
			
			Debug.out( e );
			
			synchronized( active_windows ){
				
				active_windows.remove( hash );
			}
		}
	}
	
	private boolean
	isInitialised()
	{
		return( window_initialised );
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
	
	private void 
	setupTVTorrents(
		Composite		parent )
	{
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		layout.horizontalSpacing = layout.verticalSpacing = 0;
		parent.setLayout( layout );
		GridData gd;
		
		// table
		
		Composite table_area = new Composite( parent, SWT.NULL );
		layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		layout.horizontalSpacing = layout.verticalSpacing = 0;
		table_area.setLayout( layout );
		gd = new GridData( GridData.FILL_BOTH );
		table_area.setLayoutData( gd );
		
			// toolbar area
		
		Composite button_area = new Composite( parent, SWT.NULL );
		layout = new GridLayout(5,false);
		layout.marginWidth = layout.marginHeight = 0;
		layout.horizontalSpacing = layout.verticalSpacing = 0;
		layout.marginTop = 5;
		button_area.setLayout( layout);
		gd = new GridData( GridData.FILL_HORIZONTAL );
		button_area.setLayoutData( gd );
		
		Label label = new Label( button_area, SWT.NULL );
		gd = new GridData( GridData.FILL_HORIZONTAL );
		label.setLayoutData( gd );
		
		buttonTorrentUp = new Button(button_area, SWT.PUSH);
		buttonTorrentUp.setImage( loadImage( "image.toolbar.up" ));
		buttonTorrentUp.setToolTipText(MessageText.getString("Button.moveUp"));
		buttonTorrentUp.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				List<Object> selected = tvTorrents.getSelectedDataSources();
				if ( selected.size() > 0 ){
					OpenTorrentInstance instance = (OpenTorrentInstance)selected.get(0);
					int index = instance.getIndex();
					if ( index > 0 ){
						open_instances.remove( instance );
						open_instances.add( index-1, instance );
						
						updateTVTorrentButtons();
						
						refreshTVTorrentIndexes();
					}
				}
			}});

		
		buttonTorrentDown = new Button(button_area, SWT.PUSH);
		buttonTorrentDown.setImage( loadImage( "image.toolbar.down" ));
		buttonTorrentDown.setToolTipText(MessageText.getString("Button.moveDown"));
		buttonTorrentDown.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				List<Object> selected = tvTorrents.getSelectedDataSources();
				if ( selected.size() > 0 ){
					OpenTorrentInstance instance = (OpenTorrentInstance)selected.get(0);
					int index = instance.getIndex();
					if ( index < open_instances.size() - 1 ){
						open_instances.remove( instance );
						open_instances.add( index+1, instance );
						
						updateTVTorrentButtons();
						
						refreshTVTorrentIndexes();
					}
				}
			}});
		
		buttonTorrentRemove = new Button(button_area, SWT.PUSH);
		buttonTorrentRemove.setToolTipText(MessageText.getString("OpenTorrentWindow.torrent.remove"));
		buttonTorrentRemove.setImage( loadImage( "image.toolbar.remove" ));
		buttonTorrentRemove.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				List<Object> selected = tvTorrents.getSelectedDataSources();
				for ( Object obj: selected ){
					
					OpenTorrentInstance	instance = (OpenTorrentInstance)obj;
					
					removeInstance( instance );
				}
			}});
		
		buttonTorrentUp.setEnabled( false );
		buttonTorrentDown.setEnabled( false );
		buttonTorrentRemove.setEnabled( false );
		
		label = new Label( button_area, SWT.NULL );
		gd = new GridData( GridData.FILL_HORIZONTAL );
		label.setLayoutData( gd );
		
	
		
		TableColumnManager tcm = TableColumnManager.getInstance();
		
		if (tcm.getDefaultColumnNames(TABLEID_TORRENTS) == null) {

			tcm.registerColumn(OpenTorrentInstance.class,
					TableColumnOTOT_Position.COLUMN_ID, new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new TableColumnOTOT_Position(column);
						}
					});

			tcm.registerColumn(OpenTorrentInstance.class,
					TableColumnOTOT_Name.COLUMN_ID, new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new TableColumnOTOT_Name(column);
						}
					});

			tcm.registerColumn(OpenTorrentInstance.class,
					TableColumnOTOT_Size.COLUMN_ID, new TableColumnCreationListener() {
						public void tableColumnCreated(TableColumn column) {
							new TableColumnOTOT_Size(column);
						}
					});
			
			tcm.setDefaultColumnNames(TABLEID_TORRENTS, new String[] {
				
				TableColumnOTOT_Position.COLUMN_ID,
				TableColumnOTOT_Name.COLUMN_ID,
				TableColumnOTOT_Size.COLUMN_ID,
			});
			
			tcm.setDefaultSortColumnName(TABLEID_TORRENTS, TableColumnOTOT_Position.COLUMN_ID);
		}

		tvTorrents = TableViewFactory.createTableViewSWT(OpenTorrentInstance.class,
				TABLEID_TORRENTS, TABLEID_TORRENTS, null, "#", SWT.BORDER
						| SWT.FULL_SELECTION | SWT.SINGLE);
		
		tvTorrents.initialize( table_area );
		
		tvTorrents.setRowDefaultHeight(20);


		tvTorrents.addMenuFillListener(
			new TableViewSWTMenuFillListener() 
			{
				public void 
				fillMenu(
					String 		sColumnName, 
					Menu 		menu )
				{
					final List<Object> selected = tvTorrents.getSelectedDataSources();
					
					if ( selected.size() > 0 ){
							
						final List<OpenTorrentInstance> instances = new ArrayList<OpenTorrentOptionsWindow.OpenTorrentInstance>( selected.size());
							
						for ( Object o: selected ){
							
							instances.add((OpenTorrentInstance)o );
						}
								
						MenuItem item = new MenuItem(menu, SWT.PUSH);
						
						Messages.setLanguageText(item, "OpenTorrentWindow.fileList.changeDestination");
						
						item.addSelectionListener(
							new SelectionAdapter() 
							{
								public void 
								widgetSelected(
									SelectionEvent e )
								{
									for ( Object obj: selected ){
										
										OpenTorrentInstance	instance = (OpenTorrentInstance)obj;
										
										instance.setSavePath();
									}
								}
							});
						
						new MenuItem(menu, SWT.SEPARATOR);

						item = new MenuItem(menu, SWT.PUSH);
						
						Messages.setLanguageText(item, "Button.remove");
						
						item.addSelectionListener(
							new SelectionAdapter() 
							{
								public void 
								widgetSelected(
									SelectionEvent e )
								{
									for ( Object obj: selected ){
										
										OpenTorrentInstance	instance = (OpenTorrentInstance)obj;
										
										removeInstance( instance );
									}
								}
							});
						
						new MenuItem(menu, SWT.SEPARATOR);
					}
				}
				
	
				public void 
				addThisColumnSubMenu(
					String 	sColumnName, 
					Menu 	menuThisColumn) 
				{
				}
			});
		
		
		tvTorrents.addSelectionListener(
			new TableSelectionListener() 
			{
				public void 
				selected(
					TableRowCore[] rows) 
				{
					TableRowCore row = rows[0];
					
					OpenTorrentInstance instance = (OpenTorrentInstance)row.getDataSource();
					
					selectInstance( instance );
					
					updateTVTorrentButtons();
				}
				
				public void mouseExit(TableRowCore row) {
				}
				
				public void mouseEnter(TableRowCore row) {
				}
				
				public void focusChanged(TableRowCore focus) {
				}
				
				public void 
				deselected(TableRowCore[] rows) 
				{
					updateTVTorrentButtons();
				}
			
				public void defaultSelected(TableRowCore[] rows, int stateMask) {
				}
				
			}, false);
	}
	
	private void
	selectInstance(
		OpenTorrentInstance		instance )
	{
		if ( instance == null ){
			
			current_instance = null;
			
			tvTorrents.setSelectedRows( new TableRowCore[0] );
			
		}else{
			
			expand_stack.topControl = instance.getComposite();
	
			expand_stack_area.layout(true);
			
			instance.layout();
			
			current_instance = instance;
			
			TableRowCore row = tvTorrents.getRow( current_instance );
			
			if ( row != null ){
				
				tvTorrents.setSelectedRows( new TableRowCore[]{ row });
			}
		}
	}
	
	private void
	addInstance(
		OpenTorrentInstance		instance )
	{
		open_instances.add( instance );
		
		instance.initialize();
			
		tvTorrents.addDataSources( new OpenTorrentInstance[]{ instance });

		updateInstanceInfo();

		updateTVTorrentButtons();
	}
	
	private void
	removeInstance(
		OpenTorrentInstance		instance )
	{
		synchronized( active_windows ){
			
			active_windows.remove( instance.getHash());
		}
		
		int index = open_instances.indexOf( instance );
		
		open_instances.remove( instance );
		
		tvTorrents.removeDataSource( instance );
				
		instance.getComposite().dispose();
		
		updateInstanceInfo();
		
		int	num_instances = open_instances.size();
		
		if ( num_instances > index ){
			
			selectInstance( open_instances.get( index ));
			
		}else if ( num_instances > 0 ){
			
			selectInstance( open_instances.get( num_instances-1 ));
			
		}else{
			
			selectInstance( null );
		}
		
		updateTVTorrentButtons();
		
		refreshTVTorrentIndexes();
		
		instance.dispose();
	}
	
	private void
	updateTVTorrentButtons()
	{
		List<Object> selected = tvTorrents.getSelectedDataSources();
		
		buttonTorrentRemove.setEnabled( selected.size() > 0 );

		if ( selected.size() > 0 ){
			
			OpenTorrentInstance instance = (OpenTorrentInstance)selected.get(0);
			
			buttonTorrentUp.setEnabled( instance.getIndex() > 0 );
			
			buttonTorrentDown.setEnabled( instance.getIndex() < open_instances.size()-1);
			
		}else{
			
			buttonTorrentUp.setEnabled( false );
			
			buttonTorrentDown.setEnabled( false );
		}
	}
	
	private void
	refreshTVTorrentIndexes()
	{
		Utils.execSWTThreadLater(
				0,
				new Runnable()
				{
					public void
					run()
					{
						tvTorrents.columnInvalidate( "#" );
						
						tvTorrents.refreshTable( true );
					}
				});
	}
	
	private void
	updateInstanceInfo()
	{	
		if ( torrents_info_label == null ){
			
			return;
		}
		
		long	total_size		= 0;
		long	selected_size 	= 0;
		
		for ( OpenTorrentInstance instance: open_instances ){
			
			total_size		+= instance.getOptions().getTorrent().getSize();
			selected_size 	+= instance.getSelectedDataSize();
		}
		
		String	sel_str = DisplayFormatters.formatByteCountToKiBEtc(selected_size);
		String	tot_str = DisplayFormatters.formatByteCountToKiBEtc(total_size);
		
		
		String text;
		
		if ( sel_str.equals( tot_str )){
			
			text = MessageText.getString("label.n.will.be.downloaded", new String[] { tot_str	});

		}else{
			
			text = MessageText.getString("OpenTorrentWindow.filesInfo", new String[] { sel_str,	tot_str	});
		}
		
		torrents_info_label.setText( text );
	}
	
	public void
	updateUI()
	{
		if ( tvTorrents != null ){
			
			tvTorrents.refreshTable( false );
		}
		
		for( OpenTorrentInstance instance: open_instances ){
		
			instance.updateUI();
		}
	}
	
	public String getUpdateUIName() {
		return null;
	}

	private void
	swt_activate()
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
	
	private Image
	loadImage(
		String		key )
	{
		 Image img = image_loader.getImage( key );
		 
		 if ( img != null ){
			 
			 images_to_dispose.add( key );
		 }
		 
		 return( img );
	}
	
	private void
	unloadImage(
		String	key )
	{
		image_loader.releaseImage( key );
	}
	
	protected void 
	dispose()
	{
		UIUpdaterSWT.getInstance().removeUpdater(this);
		
		for ( OpenTorrentInstance instance: open_instances ){
			
			instance.dispose();
		}
		
		for ( String key: images_to_dispose ){
			
			unloadImage( key );
		}
		
		images_to_dispose.clear();
		
		tvTorrents.delete();
	}

	private boolean 
	isDisposed() 
	{
		if ( dlg == null ){
			
			return false;
		}
		
		return dlg.isDisposed();
	}
	
	protected class
	OpenTorrentInstance
	{
		final private HashWrapper						hash;
		final private TorrentOpenOptions 				torrentOptions;
		final private OpenTorrentInstanceListener		changeListener;

		final private Composite	parent;
		final private Shell		shell;
		
		private SWTSkin skin;
		
		/* prevents loop of modifications */
		protected boolean bSkipDataDirModify = false;

		private Button btnSwarmIt;

		private Combo cmbDataDir;

		private Combo cmbQueueLocation;

		private Combo cmbStartMode;

		private StringList dirList;

		private volatile boolean diskFreeInfoRefreshPending = false;

		private volatile boolean diskFreeInfoRefreshRunning = false;

		private Composite diskspaceComp;

		private long	currentSelectedDataSize;

		private final Map fileStatCache = new WeakHashMap(20);

		private final Map parentToRootCache = new WeakHashMap(20);

		private SWTSkinObjectExpandItem soExpandItemFiles;

		private SWTSkinObjectExpandItem soExpandItemSaveTo;

		private SWTSkinObjectExpandItem soExpandItemTorrentInfo;

		private SWTSkinObjectText soFileAreaInfo;

		private TableViewSWT<Object> tvFiles;

		private SWTSkinObjectExpandItem soStartOptionsExpandItem;

		//private SWTSkinObjectExpandItem soExpandItemPeer;

		private AtomicInteger settingToDownload = new AtomicInteger(0);
		
		private Button btnSelectAll;
		private Button btnMarkSelected;
		private Button btnUnmarkSelected;
		private Button btnRename;
		private Button btnRetarget;
		
		private 
		OpenTorrentInstance(
			HashWrapper						_hash,
			Composite						_parent,	
			TorrentOpenOptions				_torrentOptions,
			OpenTorrentInstanceListener		_changeListener )
		{
			hash			= _hash;
			parent			= _parent;
			torrentOptions 	= _torrentOptions;
			changeListener	= _changeListener;

			shell = parent.getShell();

			torrentOptions.addListener(new TorrentOpenOptions.FileListener() {
				public void toDownloadChanged(TorrentOpenFileOptions fo, boolean toDownload) {
					TableRowCore row = tvFiles.getRow(fo);
					if (row != null) {
						row.invalidate(true);
						row.refresh(true);
					}
					if ( settingToDownload.get() == 0 ){
						updateFileButtons();
						updateSize();
					}
				}
				public void priorityChanged(TorrentOpenFileOptions fo, int priority) {
					TableRowCore row = tvFiles.getRow(fo);
					if (row != null) {
						row.invalidate(true);
						row.refresh(true);
					}
				}
			});
		}
		
		private HashWrapper
		getHash()
		{
			return( hash );
		}
		
		protected TorrentOpenOptions
		getOptions()
		{
			return( torrentOptions );
		}
		
		protected int
		getIndex()
		{
			return( open_instances.indexOf( this ));
		}
		
		protected Composite
		getComposite()
		{
			return( parent );
		}
		
		private void
		initialize()
		{						
			skin = SWTSkinFactory.getNonPersistentInstance(
						getClass().getClassLoader(), 
						"com/aelitis/azureus/ui/skin", "skin3_dlg_opentorrent_options_instance.properties" );			
		
			skin.initialize( parent, "expandview");

			SWTSkinObject so = skin.getSkinObject("filearea-table");
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
	
			/*
			so = skin.getSkinObject("expanditem-peer");
			if (so instanceof SWTSkinObjectExpandItem) {
				soExpandItemPeer = (SWTSkinObjectExpandItem) so;
			}
			*/
			
			so = skin.getSkinObject("expanditem-torrentinfo");
			if (so instanceof SWTSkinObjectExpandItem) {
				soExpandItemTorrentInfo = (SWTSkinObjectExpandItem) so;
				soExpandItemTorrentInfo.setText(MessageText.getString("OpenTorrentOptions.header.torrentinfo")
						+ ": " + torrentOptions.getTorrentName());
			}
				
			setupInfoFields(skin);
	
			updateStartOptionsHeader();
			cmbDataDirChanged();
			updateSize();
			
			skin.layout();
		}
		
		private void
		layout()
		{
			SWTSkinObjectExpandItem so = (SWTSkinObjectExpandItem)skin.getSkinObject("expanditem-files");

			SWTSkinObjectExpandBar bar = (SWTSkinObjectExpandBar)so.getParent();
			
			bar.relayout();
			
			for ( SWTSkinObjectExpandItem item: bar.getChildren()){
				
				item.relayout();
			}
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
			
			cButtons.setLayout(new GridLayout(7,false));
	
			List<Button>	buttons = new ArrayList<Button>();
			
			btnSelectAll = new Button(cButtons, SWT.PUSH);
			buttons.add( btnSelectAll );
			Messages.setLanguageText(btnSelectAll, "Button.selectAll");
			btnSelectAll.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					tvFiles.selectAll();
				}
			});
			
			btnMarkSelected = new Button(cButtons, SWT.PUSH);
			buttons.add( btnMarkSelected );
			Messages.setLanguageText(btnMarkSelected, "Button.mark");
			btnMarkSelected.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					TorrentOpenFileOptions[] infos = tvFiles.getSelectedDataSources().toArray(new TorrentOpenFileOptions[0]);
					setToDownload( infos, true );
				}
			});
			
			btnUnmarkSelected = new Button(cButtons, SWT.PUSH);
			buttons.add( btnUnmarkSelected );
			Messages.setLanguageText(btnUnmarkSelected, "Button.unmark");
			btnUnmarkSelected.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					TorrentOpenFileOptions[] infos = tvFiles.getSelectedDataSources().toArray(new TorrentOpenFileOptions[0]);
					setToDownload( infos, false );
	
				}
			});
			
			btnRename = new Button(cButtons, SWT.PUSH);
			buttons.add( btnRename );
			Messages.setLanguageText(btnRename, "Button.rename");
			btnRename.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					TorrentOpenFileOptions[] infos = tvFiles.getSelectedDataSources().toArray(
							new TorrentOpenFileOptions[0]);
					renameFilenames(infos);
				}
			});
	
			btnRetarget = new Button(cButtons, SWT.PUSH);
			buttons.add( btnRetarget );
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
						GridData gridData = new GridData( GridData.FILL_HORIZONTAL);
						pad.setLayoutData( gridData );
						
						btnSwarmIt = new Button(cButtons, SWT.PUSH);
						buttons.add( btnSwarmIt );
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
	
			Utils.makeButtonsEqualWidth( buttons );
			
			updateFileButtons();
	
		}
	
		private void
		setToDownload(
			TorrentOpenFileOptions[]	infos,
			boolean						download )	
		{
			boolean changed = false;
			try{
				settingToDownload.incrementAndGet();
			
				for (TorrentOpenFileOptions info: infos ){
					
					if ( info.isToDownload() != download ){
						
						info.setToDownload( download );
						
						changed = true;
					}
				}
			}finally{
				
				settingToDownload.decrementAndGet();
			}
			
			if ( changed ){
				updateFileButtons();
				updateSize();
			}
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
			if ( parent.isDisposed()){
				
				return;
			}
			
			final String SP_KEY = "oto:tag:initsp";
					
			if ( is_rebuild ){
				
				Utils.disposeComposite( parent, false );
				
			}else{
				
				parent.setData( SP_KEY, getSavePath());
			}
			
			final TagType tt = TagManagerFactory.getTagManager().getTagType( TagType.TT_DOWNLOAD_MANUAL );
			
			List<Tag> initialTags = torrentOptions.getInitialTags();
	
			for ( final Tag tag: TagUIUtils.sortTags( tt.getTags())){
				
				if ( tag.canBePublic() && !tag.isTagAuto()){
					
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
							
							Utils.execSWTThread(
								new Runnable()
								{
									public void
									run()
									{
										buildTagButtonPanel( parent, true );
									}
								});
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
				
				tcm.registerColumn(TorrentOpenFileOptions.class,
						TableColumnOTOF_Priority.COLUMN_ID, new TableColumnCreationListener() {
							public void tableColumnCreated(TableColumn column) {
								new TableColumnOTOF_Priority(column);
							}
						});
	
				tcm.setDefaultColumnNames(TABLEID_FILES, new String[] {
					TableColumnOTOF_Position.COLUMN_ID,
					TableColumnOTOF_Download.COLUMN_ID,
					TableColumnOTOF_Name.COLUMN_ID,
					TableColumnOTOF_Size.COLUMN_ID,
					TableColumnOTOF_Path.COLUMN_ID,
					TableColumnOTOF_Priority.COLUMN_ID
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
	
						TorrentOpenFileOptions[] infos = tvFiles.getSelectedDataSources().toArray(new TorrentOpenFileOptions[0]);
						setToDownload( infos, download );
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
					final Shell shell = menu.getShell();
					MenuItem item;
					TableRowCore focusedRow = tvFiles.getFocusedRow();
					final TorrentOpenFileOptions[] infos = tvFiles.getSelectedDataSources().toArray(new TorrentOpenFileOptions[0]);
					final TorrentOpenFileOptions tfi_focus = ((TorrentOpenFileOptions) focusedRow.getDataSource());
					boolean download = tfi_focus.isToDownload();
	
					item = new MenuItem(menu, SWT.CHECK);
					Messages.setLanguageText(item, "label.download.file");
					item.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							TableRowCore focusedRow = tvFiles.getFocusedRow();
							TorrentOpenFileOptions tfi_focus = ((TorrentOpenFileOptions) focusedRow.getDataSource());
							boolean download = !tfi_focus.isToDownload();
	
							setToDownload( infos, download );
						}
					});
					item.setSelection(download);
	
					
						// priority
					
					final MenuItem itemPriority = new MenuItem(menu, SWT.CASCADE);
					Messages.setLanguageText(itemPriority, "FilesView.menu.setpriority");
	
					final Menu menuPriority = new Menu(shell, SWT.DROP_DOWN);
					itemPriority.setMenu(menuPriority);
	
					final MenuItem itemHigh = new MenuItem(menuPriority, SWT.CASCADE);
	
					Messages.setLanguageText(itemHigh, "FilesView.menu.setpriority.high"); 
	
					final MenuItem itemNormal = new MenuItem(menuPriority, SWT.CASCADE);
		
					Messages.setLanguageText(itemNormal, "FilesView.menu.setpriority.normal");
					
					final MenuItem itemLow = new MenuItem(menuPriority, SWT.CASCADE);
		
					Messages.setLanguageText(itemLow, "FileItem.low");
	
					final MenuItem itemNumeric = new MenuItem(menuPriority, SWT.CASCADE);
		
					Messages.setLanguageText(itemNumeric, "FilesView.menu.setpriority.numeric"); 
	
					final MenuItem itemNumericAuto = new MenuItem(menuPriority, SWT.CASCADE);
		
					Messages.setLanguageText(itemNumericAuto, "FilesView.menu.setpriority.numeric.auto"); 
					
					Listener priorityListener = new Listener() {
						public void handleEvent(Event event) {
						
							Widget widget = event.widget;
							
							int	priority;
							
							if ( widget == itemHigh ){
								
								priority = 1;
								
							}else if ( widget == itemNormal ){
								
								priority = 0;
															
							}else if ( widget == itemLow ){
								
								priority = -1;
								
							}else if ( widget == itemNumeric ){
								
								SimpleTextEntryWindow entryWindow = new SimpleTextEntryWindow(
										"FilesView.dialog.priority.title",
										"FilesView.dialog.priority.text");
								
								entryWindow.prompt(
									new UIInputReceiverListener() {
										public void UIInputReceiverClosed(UIInputReceiver entryWindow) {
											if (!entryWindow.hasSubmittedInput()) {
												return;
											}
											String sReturn = entryWindow.getSubmittedInput();
											
											if (sReturn == null)
												return;
											
											int priority = 0;
											
											try {
												priority = Integer.valueOf(sReturn).intValue();
											} catch (NumberFormatException er) {
												
												Debug.out( "Invalid priority: " + sReturn );
												
												new MessageBoxShell(SWT.ICON_ERROR | SWT.OK,
														MessageText.getString("FilePriority.invalid.title"),
														MessageText.getString("FilePriority.invalid.text", new String[]{ sReturn })).open(null);
												
												return;
											}
											
											for (TorrentOpenFileOptions torrentFileInfo : infos) {
												
												torrentFileInfo.setPriority( priority );
											}
										}
									});
								
								return;
								
							}else if ( widget == itemNumericAuto ){
								
								int	next_priority = 0;
	
								TorrentOpenFileOptions[] all_files = torrentOptions.getFiles();
								
								if ( all_files.length != infos.length ){
									
									Set<Integer>	affected_indexes = new HashSet<Integer>();
									
									for ( TorrentOpenFileOptions file: infos ){
										
										affected_indexes.add( file.getIndex());
									}
										
									for ( TorrentOpenFileOptions file: all_files ){
									
										if ( !( affected_indexes.contains( file.getIndex()) || !file.isToDownload())){
											
											next_priority = Math.max( next_priority, file.getPriority()+1);
										}
									}
								}
								
								next_priority += infos.length;
								
								for ( TorrentOpenFileOptions file: infos ){
									
									file.setPriority( --next_priority );
								}
								
								return;
								
							}else{
								
								return;
							}
							
							for (TorrentOpenFileOptions torrentFileInfo : infos) {
								
								torrentFileInfo.setPriority( priority );
							}
						}
					};
	
					itemNumeric.addListener(SWT.Selection, priorityListener);
					itemNumericAuto.addListener(SWT.Selection, priorityListener);
					itemHigh.addListener(SWT.Selection, priorityListener);
					itemNormal.addListener(SWT.Selection, priorityListener);
					itemLow.addListener(SWT.Selection, priorityListener);
					
						// rename
					
					item = new MenuItem(menu, SWT.PUSH);
					Messages.setLanguageText(item, "FilesView.menu.rename_only");
					item.addSelectionListener(new SelectionAdapter() {
	
						public void widgetSelected(SelectionEvent e) {
	
							renameFilenames(infos);
						}
					});
	
					item = new MenuItem(menu, SWT.PUSH);
					Messages.setLanguageText(item,
							"OpenTorrentWindow.fileList.changeDestination");
					item.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
	
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
					
					if ( !torrentOptions.isSimpleTorrent()){
						
						 new MenuItem(menu, SWT.SEPARATOR );
						 
						 item = new MenuItem(menu, SWT.PUSH);
							Messages.setLanguageText(item, "OpenTorrentWindow.set.savepath");
							item.addSelectionListener(new SelectionAdapter() {
								public void widgetSelected(SelectionEvent e) {
									setSavePath();
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
					TableRowCore[] rows = tvFiles.getSelectedRows();
					
					boolean hasRowsSelected = rows.length > 0;
					if (btnRename != null && !btnRename.isDisposed()) {
						btnRename.setEnabled(hasRowsSelected);
					}
					if (btnRetarget != null && !btnRetarget.isDisposed()) {
						btnRetarget.setEnabled(hasRowsSelected);
					}
					
					boolean all_marked 		= true;
					boolean all_unmarked 	= true;
					
					for ( TableRowCore row: rows ){
						TorrentOpenFileOptions tfi = ((TorrentOpenFileOptions) row.getDataSource());
						if ( tfi.isToDownload()){
							all_unmarked 	= false;
						}else{
							all_marked 		= false;
						}
					}
					
					btnSelectAll.setEnabled(  rows.length < torrentOptions.getFiles().length );
					
					btnMarkSelected.setEnabled( hasRowsSelected && !all_marked );
					btnUnmarkSelected.setEnabled( hasRowsSelected && !all_unmarked );
					
					if (btnSwarmIt != null && !btnSwarmIt.isDisposed()){
						boolean	enable=false;
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
	
		private String askForRenameFilename(TorrentOpenFileOptions fileInfo) {
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
	
		private void
		setSavePath()
		{
			if ( torrentOptions.isSimpleTorrent()){
				
				changeFileDestination( torrentOptions.getFiles());
				
			}else{
				DirectoryDialog dDialog = new DirectoryDialog(shell,SWT.SYSTEM_MODAL);
				
				File filterPath = new File( torrentOptions.getDataDir());
				
				if ( !filterPath.exists()){
					filterPath = filterPath.getParentFile();
				}
				dDialog.setFilterPath( filterPath.getAbsolutePath());
				dDialog.setMessage(MessageText.getString("MainWindow.dialog.choose.savepath")
						+ " (" + torrentOptions.getTorrentName() + ")");
				String sNewDir = dDialog.open();
	
				if (sNewDir == null){
					return;
				}
	
				File newDir = new File(sNewDir).getAbsoluteFile();
				
				if ( !newDir.isDirectory()){
					
					if ( newDir.exists()){
						
						Debug.out( "new dir isn't a dir!" );
						
						return;
						
					}else if ( !newDir.mkdirs()){
						
						Debug.out( "Failed to create '" + newDir + "'" );
						
						return;
					}
				}
	
				File new_parent = newDir.getParentFile();
				
				if ( new_parent == null ){
					
					Debug.out( "Invalid save path, parent folder is null" );
					
					return;
				}
				
				torrentOptions.setParentDir( new_parent.getAbsolutePath());
				torrentOptions.setSubDir( newDir.getName());
				
				/* old window used to reset this - not sure why, if the user's
				 * made some per-file changes already then we should keep them
				for ( TorrentOpenFileOptions tfi: torrentOptions.getFiles()){
					
					tfi.setFullDestName( null );
				}
				*/
			}
		}
		
		private void changeFileDestination(TorrentOpenFileOptions[] infos) {
	
			for (TorrentOpenFileOptions fileInfo : infos) {
				int style = (fileInfo.parent.iStartID == TorrentOpenOptions.STARTMODE_SEEDING)
						? SWT.OPEN : SWT.SAVE;
				FileDialog fDialog = new FileDialog(shell, SWT.SYSTEM_MODAL
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
						mb.setParent(shell);
						mb.open(null);
					}
				} else
					fileInfo.setFullDestName(sNewName);
	
			} // for i
	
			checkSeedingMode();
			updateDataDirCombo();
			diskFreeInfoRefreshPending = true;
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
	
			boolean	changed = checkedSize != currentSelectedDataSize;
			
			currentSelectedDataSize = checkedSize;
			
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
	
			if ( changed ){
				
				changeListener.instanceChanged( this );
			}
		}
	
		protected long
		getSelectedDataSize()
		{
			return( currentSelectedDataSize );
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
	
		private boolean 
		okPressed(
			String dataDir) 
		{
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
				mb.setParent(shell);
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
				mb.setParent(shell);
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
				mb.setParent(shell);
				mb.open(null);
				return false;
			}
	
			if (!torrentOptions.isValid) {
				MessageBoxShell mb = new MessageBoxShell(SWT.OK | SWT.ICON_ERROR,
						"OpenTorrentWindow.mb.notValid", new String[] {
							torrentOptions.getTorrentName()
						});
				mb.setParent(shell);
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
				mb.setParent(shell);
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
		
			return true;
		}
		
		private void
		dispose()
		{
			tvFiles.delete();
		}
	}
	
	public interface
	OpenTorrentInstanceListener
	{
		public void
		instanceChanged(
			OpenTorrentInstance		instance );
	}
}
