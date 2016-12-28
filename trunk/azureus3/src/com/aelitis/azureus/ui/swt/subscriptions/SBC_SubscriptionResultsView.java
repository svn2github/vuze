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

package com.aelitis.azureus.ui.swt.subscriptions;


import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableColumnCreationListener;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.ClipboardCopy;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewFactory;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.core.subs.SubscriptionListener;
import com.aelitis.azureus.core.subs.SubscriptionResult;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.ToolBarItem;
import com.aelitis.azureus.ui.common.table.*;
import com.aelitis.azureus.ui.common.table.impl.TableColumnManager;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.mdi.MdiEntry;
import com.aelitis.azureus.ui.selectedcontent.*;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.columns.searchsubs.ColumnSearchSubResultName;
import com.aelitis.azureus.ui.swt.columns.searchsubs.ColumnSearchSubResultActions;
import com.aelitis.azureus.ui.swt.columns.searchsubs.ColumnSearchSubResultAge;
import com.aelitis.azureus.ui.swt.columns.searchsubs.ColumnSearchSubResultCategory;
import com.aelitis.azureus.ui.swt.columns.searchsubs.ColumnSearchSubResultRank;
import com.aelitis.azureus.ui.swt.columns.searchsubs.ColumnSearchSubResultRatings;
import com.aelitis.azureus.ui.swt.columns.searchsubs.ColumnSearchSubResultSeedsPeers;
import com.aelitis.azureus.ui.swt.columns.searchsubs.ColumnSearchSubResultSize;
import com.aelitis.azureus.ui.swt.columns.searchsubs.ColumnSearchSubResultType;
import com.aelitis.azureus.ui.swt.columns.subscriptions.ColumnSubResultNew;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.ui.swt.mdi.MultipleDocumentInterfaceSWT;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.utils.SearchSubsUtils;
import com.aelitis.azureus.ui.swt.views.skin.SkinView;

public class 
SBC_SubscriptionResultsView
	extends SkinView
	implements UIUpdatable, UIPluginViewToolBarListener, TableViewFilterCheck<SBC_SubscriptionResult>, SubscriptionListener
{
	public static final String TABLE_SR = "SubscriptionResults";

	private static boolean columnsAdded = false;
	
	private TableViewSWT<SBC_SubscriptionResult> tv_subs_results;

	private MdiEntry 			mdi_entry;
	private Composite			table_parent;
	
	
	private Text txtFilter;

	private final Object filter_lock = new Object();

	private int minSize;
	private int maxSize;
	
	private String[]	with_keywords 		= {};
	private String[]	without_keywords 	= {};
	
	private FrequencyLimitedDispatcher	refilter_dispatcher =
			new FrequencyLimitedDispatcher( 
				new AERunnable() {
					
					@Override
					public void runSupport() 
					{
						refilter();
					}
				}, 250 );
	
	private Subscription	 ds;

	private List<SBC_SubscriptionResult>	last_selected_content = new ArrayList<SBC_SubscriptionResult>();
	
	public
	SBC_SubscriptionResultsView()
	{
	}
	
	public Object 
	skinObjectInitialShow(
		SWTSkinObject skinObject, Object params ) 
	{
		AzureusCoreFactory.addCoreRunningListener(
			new AzureusCoreRunningListener() 
			{
				public void 
				azureusCoreRunning(
					AzureusCore core )
				{
					initColumns( core );
				}
			});

		MultipleDocumentInterfaceSWT mdi = UIFunctionsManagerSWT.getUIFunctionsSWT().getMDISWT();
				
		if ( mdi != null && ds != null ){

			String mdi_key = "Subscription_" + ByteFormatter.encodeString(ds.getPublicKey());
			
			mdi_entry = mdi.getEntry( mdi_key );
			
			if ( mdi_entry != null ){
			
				mdi_entry.addToolbarEnabler(this);
			}
		}

		if ( ds != null ){
			
			SWTSkinObjectText title = (SWTSkinObjectText) getSkinObject("title");
			
			if ( title != null ){
				
				title.setText( MessageText.getString( "subs.results.view.title", new String[]{ ds.getName() }));
			}
		}
		
		SWTSkinObjectTextbox soFilterBox = (SWTSkinObjectTextbox) getSkinObject("filterbox");
		if (soFilterBox != null) {
			txtFilter = soFilterBox.getTextControl();
		}

		final SWTSkinObject soFilterArea = getSkinObject("filterarea");
		if (soFilterArea != null) {
			
			SWTSkinObjectToggle soFilterButton = (SWTSkinObjectToggle) getSkinObject("filter-button");
			if (soFilterButton != null) {
				boolean toggled = COConfigurationManager.getBooleanParameter( "Subscription View Filter Options Expanded", false );
				
				if ( toggled ){
					
					soFilterButton.setToggled( true );
					
					soFilterArea.setVisible( true );
				}
				
				soFilterButton.addSelectionListener(new SWTSkinToggleListener() {
					public void toggleChanged(SWTSkinObjectToggle so, boolean toggled) {
						
						COConfigurationManager.setParameter( "Subscription View Filter Options Expanded", toggled );
						
						soFilterArea.setVisible(toggled);
						Utils.relayout(soFilterArea.getControl().getParent());
					}
				});			}
			
			Composite parent = (Composite) soFilterArea.getControl();
	
			Label label;
			FormData fd;
			GridLayout layout;
			int sepHeight = 20;
			
			Composite cRow = new Composite(parent, SWT.NONE);
			fd = Utils.getFilledFormData();
			cRow.setLayoutData(fd);
			RowLayout rowLayout = new RowLayout(SWT.HORIZONTAL);
			rowLayout.spacing = 5;
			rowLayout.marginBottom = rowLayout.marginTop = rowLayout.marginLeft = rowLayout.marginRight = 0; 
			rowLayout.center = true;
			cRow.setLayout(rowLayout);
			
			
			// with/without keywords
			
			ImageLoader imageLoader = ImageLoader.getInstance();

			for ( int i=0;i<2;i++){
				
				final boolean with = i == 0;
		
				if ( !with ){
					
					label = new Label(cRow, SWT.VERTICAL | SWT.SEPARATOR);
					label.setLayoutData(new RowData(-1, sepHeight));
				}
				
				Composite cWithKW = new Composite(cRow, SWT.NONE);
				layout = new GridLayout(2, false);
				layout.marginWidth = 0;
				layout.marginBottom = layout.marginTop = layout.marginLeft = layout.marginRight = 0;
				cWithKW.setLayout(layout);
				//Label lblWithKW = new Label(cWithKW, SWT.NONE);
				//lblWithKW.setText(MessageText.getString(with?"SubscriptionResults.filter.with.words":"SubscriptionResults.filter.without.words"));
				Label lblWithKWImg = new Label(cWithKW, SWT.NONE);
				lblWithKWImg.setImage( imageLoader.getImage( with?"icon_filter_plus":"icon_filter_minus"));
				
				final Text textWithKW = new Text(cWithKW, SWT.BORDER);
				textWithKW.setMessage(MessageText.getString(with?"SubscriptionResults.filter.with.words":"SubscriptionResults.filter.without.words"));
				GridData gd = new GridData();
				gd.widthHint = Utils.adjustPXForDPI( 100 );
				textWithKW.setLayoutData( gd );
				textWithKW.addModifyListener(
					new ModifyListener() {
						
						public void modifyText(ModifyEvent e) {
							String text = textWithKW.getText().toLowerCase( Locale.US );
							String[] bits = text.split( "\\s+");
							
							Set<String>	temp = new HashSet<String>();
							
							for ( String bit: bits ){
							
								bit = bit.trim();
								if ( bit.length() > 0 ){
									temp.add( bit );
								}
							}
							
							String[] words = temp.toArray( new String[temp.size()] );
							synchronized( filter_lock ){
								if ( with ){
									with_keywords = words;
								}else{
									without_keywords = words;
								}
							}
							refilter_dispatcher.dispatch();
						}
					});
			}
			
					
				// min size
			
			label = new Label(cRow, SWT.VERTICAL | SWT.SEPARATOR);
			label.setLayoutData(new RowData(-1, sepHeight));

			Composite cMinSize = new Composite(cRow, SWT.NONE);
			layout = new GridLayout(2, false);
			layout.marginWidth = 0;
			layout.marginBottom = layout.marginTop = layout.marginLeft = layout.marginRight = 0;
			cMinSize.setLayout(layout);
			Label lblMinSize = new Label(cMinSize, SWT.NONE);
			lblMinSize.setText(MessageText.getString("SubscriptionResults.filter.min_size"));
			Spinner spinMinSize = new Spinner(cMinSize, SWT.BORDER);
			spinMinSize.setMinimum(0);
			spinMinSize.setMaximum(100*1024*1024);	// 100 TB should do...
			spinMinSize.setSelection(minSize);
			spinMinSize.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					minSize = ((Spinner) event.widget).getSelection();
					refilter();
				}
			});
			
			// max size
			
			label = new Label(cRow, SWT.VERTICAL | SWT.SEPARATOR);
			label.setLayoutData(new RowData(-1, sepHeight));

			Composite cMaxSize = new Composite(cRow, SWT.NONE);
			layout = new GridLayout(2, false);
			layout.marginWidth = 0;
			layout.marginBottom = layout.marginTop = layout.marginLeft = layout.marginRight = 0;
			cMaxSize.setLayout(layout);
			Label lblMaxSize = new Label(cMaxSize, SWT.NONE);
			lblMaxSize.setText(MessageText.getString("SubscriptionResults.filter.max_size"));
			Spinner spinMaxSize = new Spinner(cMaxSize, SWT.BORDER);
			spinMaxSize.setMinimum(0);
			spinMaxSize.setMaximum(100*1024*1024);	// 100 TB should do...
			spinMaxSize.setSelection(maxSize);
			spinMaxSize.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					maxSize = ((Spinner) event.widget).getSelection();
					refilter();
				}
			});
			
	

			parent.layout(true);
		}

		return null;
	}
	
	private boolean 
	isOurContent(
		SBC_SubscriptionResult result) 
	{
		long	size = result.getSize();
		
		boolean size_ok = 
			
			(size==-1||(size >= 1024L*1024*minSize)) &&
			(size==-1||(maxSize ==0 || size <= 1024L*1024*maxSize));
		
		if ( !size_ok ){
			
			return( false );
		}
		
		if ( with_keywords.length > 0 || without_keywords.length > 0 ){
			
			synchronized( filter_lock ){
				
				String name = result.getName().toLowerCase( Locale.US );
				
				for ( int i=0;i<with_keywords.length;i++){
					
					if ( !name.contains( with_keywords[i] )){
						
						return( false );
					}
				}
				
				for ( int i=0;i<without_keywords.length;i++){
					
					if ( name.contains( without_keywords[i] )){
						
						return( false );
					}
				}
			}
		}
		
		return( true );
	}


	protected void refilter() {
		if (tv_subs_results != null) {
			tv_subs_results.refilter();
		}
	}


	private void 
	initColumns(
		AzureusCore core ) 
	{
		synchronized( SBC_SubscriptionResultsView.class ){
			
			if ( columnsAdded ){
			
				return;
			}
		
			columnsAdded = true;
		}
		
		TableColumnManager tableManager = TableColumnManager.getInstance();
		
		tableManager.registerColumn(
			SBC_SubscriptionResult.class, 
			ColumnSubResultNew.COLUMN_ID,
				new TableColumnCreationListener() {
					
					public void tableColumnCreated(TableColumn column) {
						new ColumnSubResultNew(column);
					}
				});
		
		tableManager.registerColumn(
			SBC_SubscriptionResult.class, 
			ColumnSearchSubResultType.COLUMN_ID,
				new TableColumnCreationListener() {
					
					public void tableColumnCreated(TableColumn column) {
						new ColumnSearchSubResultType(column);
					}
				});	
		
		tableManager.registerColumn(
			SBC_SubscriptionResult.class, 
			ColumnSearchSubResultName.COLUMN_ID,
				new TableColumnCreationListener() {
					
					public void tableColumnCreated(TableColumn column) {
						new ColumnSearchSubResultName(column);
					}
				});	
		
		tableManager.registerColumn(
			SBC_SubscriptionResult.class, 
			ColumnSearchSubResultActions.COLUMN_ID,
				new TableColumnCreationListener() {
					
					public void tableColumnCreated(TableColumn column) {
						new ColumnSearchSubResultActions(column);
					}
				});			
		
		tableManager.registerColumn(
			SBC_SubscriptionResult.class, 
			ColumnSearchSubResultSize.COLUMN_ID,
				new TableColumnCreationListener() {
					
					public void tableColumnCreated(TableColumn column) {
						new ColumnSearchSubResultSize(column);
					}
				});			
			
		tableManager.registerColumn(
			SBC_SubscriptionResult.class, 
			ColumnSearchSubResultSeedsPeers.COLUMN_ID,
				new TableColumnCreationListener() {
					
					public void tableColumnCreated(TableColumn column) {
						new ColumnSearchSubResultSeedsPeers(column);
					}
				});		
	
		tableManager.registerColumn(
			SBC_SubscriptionResult.class, 
			ColumnSearchSubResultRatings.COLUMN_ID,
				new TableColumnCreationListener() {
					
					public void tableColumnCreated(TableColumn column) {
						new ColumnSearchSubResultRatings(column);
					}
				});		

		tableManager.registerColumn(
			SBC_SubscriptionResult.class, 
			ColumnSearchSubResultAge.COLUMN_ID,
				new TableColumnCreationListener() {
					
					public void tableColumnCreated(TableColumn column) {
						new ColumnSearchSubResultAge(column);
					}
				});

		tableManager.registerColumn(
			SBC_SubscriptionResult.class, 
			ColumnSearchSubResultRank.COLUMN_ID,
				new TableColumnCreationListener() {
					
					public void tableColumnCreated(TableColumn column) {
						new ColumnSearchSubResultRank(column);
					}
				});
		
		tableManager.registerColumn(
			SBC_SubscriptionResult.class, 
			ColumnSearchSubResultCategory.COLUMN_ID,
				new TableColumnCreationListener() {
					
					public void tableColumnCreated(TableColumn column) {
						new ColumnSearchSubResultCategory(column);
					}
				});
	}

	public Object 
	dataSourceChanged(
		SWTSkinObject skinObject, Object params) 
	{
		synchronized( this ){
			
			Subscription new_ds = null;
			
			if ( params instanceof Subscription ){
				
				new_ds = (Subscription)params;
				
			}else if ( params instanceof Object[] ){
				
				Object[] objs = (Object[])params;
				
				if ( objs.length == 1 && objs[0] instanceof Subscription ){
					
					new_ds = (Subscription)objs[0];
				}
			}
				
			if ( ds != null ){
					
				ds.removeListener( this );
			}
			
			ds = new_ds;
			
			if ( new_ds != null ){
								
				ds.addListener( this );
			}
		}
		
		return( super.dataSourceChanged(skinObject, params));
	}

	public void
	subscriptionChanged(
		Subscription		subs,
		int					reason )
	{	
		if ( reason == SubscriptionListener.CR_RESULTS ){
			
			reconcileResults( subs );
			
			tv_subs_results.runForAllRows(
				new TableGroupRowRunner() {
					@Override
					public void run(TableRowCore row) {
						row.invalidate( true );
					}
				});
		}
	}
	
	private void
	reconcileResults(
		Subscription		subs )
	{
		synchronized( this ){
			
			if ( subs != ds || ds == null || subs == null || tv_subs_results == null ){
				
				return;
			}
			
			tv_subs_results.processDataSourceQueueSync();
			
			List<SBC_SubscriptionResult> existing_results = tv_subs_results.getDataSources();
						
			Map<String,SBC_SubscriptionResult>	existing_map = new HashMap<String, SBC_SubscriptionResult>();
			
			for ( SBC_SubscriptionResult result: existing_results ){
				
				existing_map.put( result.getID(), result );
			}
			
			SubscriptionResult[] current_results = ds.getResults( false );
			
			List<SBC_SubscriptionResult> new_results	= new ArrayList<SBC_SubscriptionResult>(current_results.length);
			
			for ( SubscriptionResult result: current_results ){
				
				if ( existing_map.remove( result.getID()) == null ){
					
					new_results.add( new SBC_SubscriptionResult( ds, result));
				}
			}
		
			if ( new_results.size() > 0 ){
			
				tv_subs_results.addDataSources( new_results.toArray( new SBC_SubscriptionResult[ new_results.size()]));
			}
			
			if ( existing_map.size() > 0 ){
				
				Collection<SBC_SubscriptionResult> to_remove = existing_map.values();
				
				tv_subs_results.removeDataSources( to_remove.toArray( new SBC_SubscriptionResult[ to_remove.size()]));

			}
		}
	}
	
	public void
	subscriptionDownloaded(
		Subscription		subs,
		boolean				auto )
	{
	}
	
	private void
	showView()
	{
		SWTSkinObject so_list = getSkinObject("subs-results-list");

		if ( so_list != null ){
				
			so_list.setVisible(true);
			
			initTable((Composite) so_list.getControl());
		}
	}
	
	private void
	hideView()
	{
		Utils.disposeSWTObjects(new Object[] {
			table_parent,
		});
	}
	
	public Object 
	skinObjectShown(
		SWTSkinObject 	skinObject, 
		Object 			params ) 
	{
		super.skinObjectShown(skinObject, params);

		showView();
		
		synchronized( this ){
			
			if ( ds != null ){
				
				ds.addListener( this );
			}
		}
		
		return null;
	}

	public Object 
	skinObjectHidden(
		SWTSkinObject 	skinObject, 
		Object 			params ) 
	{
		hideView();
		
		synchronized( this ){
			
			if ( ds != null ){
				
				ds.removeListener( this );
			}
		}
		
		return( super.skinObjectHidden(skinObject, params));
	}

	public Object
	skinObjectDestroyed(
		SWTSkinObject 	skinObject, 
		Object 			params ) 
	{
		synchronized( this ){
			
			if ( tv_subs_results != null ){
				
				tv_subs_results.delete();
				
				tv_subs_results = null;
			}

			if ( ds != null ){
				
				ds.removeListener( this );
			}
		}
		
		Utils.disposeSWTObjects(new Object[] {
			table_parent,
		});
		
		return( super.skinObjectDestroyed(skinObject, params));
	}
	
	private void 
	initTable(
		Composite control ) 
	{
		tv_subs_results = TableViewFactory.createTableViewSWT(
				SBC_SubscriptionResult.class, 
				TABLE_SR,
				TABLE_SR, 
				new TableColumnCore[0], 
				ColumnSearchSubResultAge.COLUMN_ID,
				SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL );
		
		TableColumnManager tableManager = TableColumnManager.getInstance();

		tableManager.setDefaultColumnNames( TABLE_SR,
				new String[] {
					ColumnSubResultNew.COLUMN_ID,
					ColumnSearchSubResultType.COLUMN_ID,
					ColumnSearchSubResultName.COLUMN_ID,
					ColumnSearchSubResultActions.COLUMN_ID,
					ColumnSearchSubResultSize.COLUMN_ID,
					ColumnSearchSubResultSeedsPeers.COLUMN_ID,
					ColumnSearchSubResultRatings.COLUMN_ID,
					ColumnSearchSubResultAge.COLUMN_ID,
					ColumnSearchSubResultRank.COLUMN_ID,
					ColumnSearchSubResultCategory.COLUMN_ID,
				});
		
		tableManager.setDefaultSortColumnName(TABLE_SR, ColumnSearchSubResultAge.COLUMN_ID);
		
		TableColumnCore tcc = tableManager.getTableColumnCore( TABLE_SR, ColumnSearchSubResultAge.COLUMN_ID );
		
		if ( tcc != null ){
			
			tcc.setDefaultSortAscending( true );
		}
		
		if (txtFilter != null) {
			tv_subs_results.enableFilterCheck(txtFilter, this);
		}
		
		tv_subs_results.setRowDefaultHeight(COConfigurationManager.getIntParameter( "Search Subs Row Height" ));
		
		SWTSkinObject soSizeSlider = getSkinObject("table-size-slider");
		if (soSizeSlider instanceof SWTSkinObjectContainer) {
			SWTSkinObjectContainer so = (SWTSkinObjectContainer) soSizeSlider;
			if (!tv_subs_results.enableSizeSlider(so.getComposite(), 16, 100)) {
				so.setVisible(false);
			}
		}
		
		table_parent = new Composite(control, SWT.NONE);
		table_parent.setLayoutData(Utils.getFilledFormData());
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = layout.verticalSpacing = layout.horizontalSpacing = 0;
		table_parent.setLayout(layout);

		tv_subs_results.addSelectionListener(new TableSelectionListener() {

			public void 
			selected(
				TableRowCore[] _rows) 
			{
				updateSelectedContent();
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
				updateSelectedContent();
			}

			public void defaultSelected(TableRowCore[] rows, int stateMask) {
			}
			
			private void
			updateSelectedContent()
			{
				TableRowCore[] rows = tv_subs_results.getSelectedRows();
				
				ArrayList<ISelectedContent>	valid = new ArrayList<ISelectedContent>();

				last_selected_content.clear();
				
				for (int i=0;i<rows.length;i++){
					
					SBC_SubscriptionResult rc = (SBC_SubscriptionResult)rows[i].getDataSource();
					
					last_selected_content.add( rc );
					
					byte[] hash = rc.getHash();
					
					if ( hash != null && hash.length > 0 ){
						
						SelectedContent sc = new SelectedContent(Base32.encode(hash), rc.getName());
						
						sc.setDownloadInfo(new DownloadUrlInfo(	getDownloadURI( rc )));
						
						valid.add(sc);
					}
				}
				
				ISelectedContent[] sels = valid.toArray( new ISelectedContent[valid.size()] );
				
				SelectedContentManager.changeCurrentlySelectedContent("IconBarEnabler",
						sels, tv_subs_results);
				
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				
				if ( uiFunctions != null ){
					
					uiFunctions.refreshIconBar();
				}
			}
		}, false);

		tv_subs_results.addLifeCycleListener(
			new TableLifeCycleListener() 
			{
				public void 
				tableViewInitialized() 
				{
					reconcileResults( ds );
				}

				public void 
				tableViewDestroyed() 
				{
				}
			});


		tv_subs_results.addMenuFillListener(
			new TableViewSWTMenuFillListener()
			{
				public void 
				fillMenu(String sColumnName, Menu menu)
				{
					Object[] _related_content = tv_subs_results.getSelectedDataSources().toArray();

					final SBC_SubscriptionResult[] results = new SBC_SubscriptionResult[_related_content.length];

					System.arraycopy(_related_content, 0, results, 0, results.length);
					
					MenuItem item = new MenuItem(menu, SWT.PUSH);
					item.setText(MessageText.getString("label.copy.url.to.clip"));
					item.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							
							StringBuffer buffer = new StringBuffer(1024);
							
							for ( SBC_SubscriptionResult result: results ){
								
								if ( buffer.length() > 0 ){
									buffer.append( "\r\n" );
								}
								
								buffer.append( getDownloadURI( result ));
							}
							ClipboardCopy.copyToClipBoard( buffer.toString());
						};
					});
					
					item.setEnabled( results.length > 0 );
					
					SearchSubsUtils.addMenu( results, menu );
					
					new MenuItem(menu, SWT.SEPARATOR );

					if ( results.length == 1 ){
						
						if ( SearchSubsUtils.addMenu( results[0], menu )){
							
							new MenuItem(menu, SWT.SEPARATOR );
						}
					}
					
					final MenuItem remove_item = new MenuItem(menu, SWT.PUSH);

					remove_item.setText(MessageText.getString("azbuddy.ui.menu.remove"));

					Utils.setMenuItemImage( remove_item, "delete" );

					remove_item.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							userDelete(results);
						}

					});
					
					remove_item.setEnabled( results.length > 0 );
					
					new MenuItem(menu, SWT.SEPARATOR );
				}

				public void 
				addThisColumnSubMenu(
					String columnName, Menu menuThisColumn) 
				{
				}
			});

		tv_subs_results.addKeyListener(
				new KeyListener()
				{
					public void 
					keyPressed(
						KeyEvent e )
					{
						if ( e.stateMask == 0 && e.keyCode == SWT.DEL ){
							
							Object[] selected;
							
							synchronized (this) {
								
								if ( tv_subs_results == null ){
									
									selected = new Object[0];
									
								}else{
								
									selected = tv_subs_results.getSelectedDataSources().toArray();
								}
							}
							
							SBC_SubscriptionResult[] content = new SBC_SubscriptionResult[ selected.length ];
							
							for ( int i=0;i<content.length;i++){
								
								content[i] = (SBC_SubscriptionResult)selected[i];
							}
							
							userDelete( content );
							
							e.doit = false;
						}
					}
					
					public void 
					keyReleased(
						KeyEvent arg0 ) 
					{
					}
				});
		
		/*
		if (ds instanceof RCMItemSubView) {
	  		tv_related_content.addCountChangeListener(new TableCountChangeListener() {
	  			
	  			public void rowRemoved(TableRowCore row) {
	  				updateCount();
	  			}
	  			
	  			public void rowAdded(TableRowCore row) {
	  				updateCount();
	  			}
	
					private void updateCount() {
						int size = tv_related_content == null ? 0 : tv_related_content.size(false);
						((RCMItemSubView) ds).setCount(size);
					}
	  		});
	  		((RCMItemSubView) ds).setCount(0);
		}
		*/
		
		tv_subs_results.initialize( table_parent );

		control.layout(true);
	}
	
	private void 
	userDelete(
		SBC_SubscriptionResult[] results ) 
	{
		TableRowCore focusedRow = tv_subs_results.getFocusedRow();
		
		TableRowCore focusRow = null;
		
		if (focusedRow != null) {
			int i = tv_subs_results.indexOf(focusedRow);
			int size = tv_subs_results.size(false);
			if (i < size - 1) {
				focusRow = tv_subs_results.getRow(i + 1);
			} else if (i > 0) {
				focusRow = tv_subs_results.getRow(i - 1);
			}
		}
		
		for ( SBC_SubscriptionResult result: results ){
			
			result.delete();
		}
		
		if ( focusRow != null ){
  		
			tv_subs_results.setSelectedRows(new TableRowCore[]{focusRow });
		}
	};

	public String 
	getUpdateUIName() 
	{
		return( "SubscriptionResultsView" );
	}

	public void 
	updateUI() 
	{
		if ( tv_subs_results != null ){
			
			tv_subs_results.refreshTable( false );
		}
	}

	public boolean 
	filterCheck(
		SBC_SubscriptionResult 		ds, 
		String 						filter, 
		boolean 					regex )
	{	
		if (!isOurContent(ds)){
			
			return false;
		}

		if ( filter == null || filter.length() == 0 ){
			
			return( true );
		}

		try{
			boolean	hash_filter = filter.startsWith( "t:" );
			
			if ( hash_filter ){
				
				filter = filter.substring( 2 );
			}
						
			String s = regex ? filter : "\\Q" + filter.replaceAll("[|;]", "\\\\E|\\\\Q") + "\\E";
			
			boolean	match_result = true;
			
			if ( regex && s.startsWith( "!" )){
				
				s = s.substring(1);
				
				match_result = false;
			}
			
			Pattern pattern = Pattern.compile(s, Pattern.CASE_INSENSITIVE);
  
			if ( hash_filter ){
				
				byte[] hash = ds.getHash();
				
				if ( hash == null ){
					
					return( false );
				}
				
				String[] names = { ByteFormatter.encodeString( hash ), Base32.encode( hash )};
				
				for ( String name: names ){
					
					if ( pattern.matcher(name).find() == match_result ){
						
						return( true );
					}
				}
				
				return( false );
				
			}else{
				
				String name = ds.getName();

				return( pattern.matcher(name).find() == match_result );
			}
			
		}catch(Exception e ){
			
			return true;
		}
	}
	
	public void filterSet(String filter) {
	}

	public boolean 
	toolBarItemActivated(
		ToolBarItem item, 
		long activationType,
		Object datasource ) 
	{
		if ( tv_subs_results == null || !tv_subs_results.isVisible()){
			
			return( false );
		}
		
		if (item.getID().equals("remove")) {
			
			Object[] _related_content = tv_subs_results.getSelectedDataSources().toArray();
			
			if ( _related_content.length > 0 ){
				
				SBC_SubscriptionResult[] related_content = new SBC_SubscriptionResult[_related_content.length];
				
				System.arraycopy( _related_content, 0, related_content, 0, related_content.length );
				
				userDelete( related_content );
			
				return true;
			}
		}
		
		return false;
	}

	public void 
	refreshToolBarItems(
		Map<String, Long> list) 
	{
		if ( tv_subs_results == null || !tv_subs_results.isVisible()){
			
			return;
		}
		
			// make sure we're operating on a selection we understand...
		
		ISelectedContent[] content = SelectedContentManager.getCurrentlySelectedContent();
		
		for ( ISelectedContent c: content ){
			
			if ( c.getDownloadManager() != null ){
				
				return;
			}
		}
		
		list.put("remove", tv_subs_results.getSelectedDataSources().size() > 0 ? UIToolBarItem.STATE_ENABLED : 0);
	}

	public String
	getDownloadURI(
		SBC_SubscriptionResult	result )
	{
		String torrent_url = (String)result.getTorrentLink();
		
		if ( torrent_url != null && torrent_url.length() > 0 ){
			
			return( torrent_url );
		}
		
		String uri = UrlUtils.getMagnetURI( result.getHash(), result.getName(), ds.getHistory().getDownloadNetworks());
		
		return( uri );
	}

}
