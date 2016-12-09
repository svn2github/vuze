/*
 * Created on Dec 7, 2016
 * Created by Paul Gardner
 * 
 * Copyright 2016 Azureus Software, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.ui.swt.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AENetworkClassifier;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.UrlUtils;
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
import com.aelitis.azureus.core.metasearch.Engine;
import com.aelitis.azureus.core.metasearch.MetaSearchManager;
import com.aelitis.azureus.core.metasearch.MetaSearchManagerFactory;
import com.aelitis.azureus.core.metasearch.Result;
import com.aelitis.azureus.core.metasearch.ResultListener;
import com.aelitis.azureus.core.metasearch.SearchParameter;
import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.core.subs.SubscriptionListener;
import com.aelitis.azureus.core.subs.SubscriptionResult;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.ToolBarItem;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableGroupRowRunner;
import com.aelitis.azureus.ui.common.table.TableLifeCycleListener;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.common.table.TableSelectionListener;
import com.aelitis.azureus.ui.common.table.TableViewFilterCheck;
import com.aelitis.azureus.ui.common.table.impl.TableColumnManager;
import com.aelitis.azureus.ui.mdi.MdiEntry;
import com.aelitis.azureus.ui.selectedcontent.DownloadUrlInfo;
import com.aelitis.azureus.ui.selectedcontent.ISelectedContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.columns.searchsubs.*;
import com.aelitis.azureus.ui.swt.mdi.MultipleDocumentInterfaceSWT;
import com.aelitis.azureus.ui.swt.search.SearchResultsTabArea.SearchQuery;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectContainer;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectText;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectTextbox;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectToggle;
import com.aelitis.azureus.ui.swt.skin.SWTSkinToggleListener;
import com.aelitis.azureus.ui.swt.subscriptions.SBC_SubscriptionResult;

public class 
SBC_SearchResultsView 
	implements SearchResultsTabAreaBase, TableViewFilterCheck<SBC_SearchResult>
{
	public static final String TABLE_SR = "SearchResults";

	private static boolean columnsAdded = false;
	
	private SearchResultsTabArea		parent;
	
	private TableViewSWT<SBC_SearchResult> tv_subs_results;

	private MdiEntry 			mdi_entry;
	private Composite			table_parent;
	
	
	private Text txtFilter;


	private int minSize;
	private int maxSize;
	
	
	private List<SBC_SearchResult>	last_selected_content = new ArrayList<SBC_SearchResult>();
	
	protected
	SBC_SearchResultsView(
		SearchResultsTabArea		_parent )
	{
		parent	= _parent;
	}
	
	private SWTSkinObject 
	getSkinObject(
		String viewID )
	{
		return( parent.getSkinObject(viewID));
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
		
		SWTSkinObjectTextbox soFilterBox = (SWTSkinObjectTextbox) getSkinObject("filterbox");
		if (soFilterBox != null) {
			txtFilter = soFilterBox.getTextControl();
		}

		final SWTSkinObject soFilterArea = getSkinObject("filterarea");
		if (soFilterArea != null) {
			
			SWTSkinObjectToggle soFilterButton = (SWTSkinObjectToggle) getSkinObject("filter-button");
			if (soFilterButton != null) {
				soFilterButton.addSelectionListener(new SWTSkinToggleListener() {
					public void toggleChanged(SWTSkinObjectToggle so, boolean toggled) {
						soFilterArea.setVisible(toggled);
						Utils.relayout(soFilterArea.getControl().getParent());
					}
				});
			}
			
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
			
			

			/////
			
		

				// min size
			
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
		SBC_SearchResult result) 
	{
		long	size = result.getSize();
		
		boolean show = 
			
			(size==-1||(size >= 1024L*1024*minSize)) &&
			(size==-1||(maxSize ==0 || size <= 1024L*1024*maxSize));
		
		return( show );
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
		synchronized( SBC_SearchResultsView.class ){
			
			if ( columnsAdded ){
			
				return;
			}
		
			columnsAdded = true;
		}
		
		TableColumnManager tableManager = TableColumnManager.getInstance();
				
		tableManager.registerColumn(
			SBC_SearchResult.class, 
			ColumnSearchSubResultType.COLUMN_ID,
				new TableColumnCreationListener() {
					
					public void tableColumnCreated(TableColumn column) {
						new ColumnSearchSubResultType(column);
					}
				});			
		tableManager.registerColumn(
			SBC_SearchResult.class, 
			ColumnSearchSubResultName.COLUMN_ID,
				new TableColumnCreationListener() {
					
					public void tableColumnCreated(TableColumn column) {
						new ColumnSearchSubResultName(column);
					}
				});	
		
		tableManager.registerColumn(
			SBC_SearchResult.class, 
			ColumnSearchSubResultActions.COLUMN_ID,
				new TableColumnCreationListener() {
					
					public void tableColumnCreated(TableColumn column) {
						new ColumnSearchSubResultActions(column);
					}
				});			
		
		tableManager.registerColumn(
			SBC_SearchResult.class, 
			ColumnSearchSubResultSize.COLUMN_ID,
				new TableColumnCreationListener() {
					
					public void tableColumnCreated(TableColumn column) {
						new ColumnSearchSubResultSize(column);
					}
				});			
			
		tableManager.registerColumn(
			SBC_SearchResult.class, 
			ColumnSearchSubResultSeedsPeers.COLUMN_ID,
				new TableColumnCreationListener() {
					
					public void tableColumnCreated(TableColumn column) {
						new ColumnSearchSubResultSeedsPeers(column);
					}
				});		
	
		tableManager.registerColumn(
			SBC_SearchResult.class, 
			ColumnSearchSubResultRatings.COLUMN_ID,
				new TableColumnCreationListener() {
					
					public void tableColumnCreated(TableColumn column) {
						new ColumnSearchSubResultRatings(column);
					}
				});		

		tableManager.registerColumn(
			SBC_SearchResult.class, 
			ColumnSearchSubResultAge.COLUMN_ID,
				new TableColumnCreationListener() {
					
					public void tableColumnCreated(TableColumn column) {
						new ColumnSearchSubResultAge(column);
					}
				});

		tableManager.registerColumn(
			SBC_SearchResult.class, 
			ColumnSearchSubResultRank.COLUMN_ID,
				new TableColumnCreationListener() {
					
					public void tableColumnCreated(TableColumn column) {
						new ColumnSearchSubResultRank(column);
					}
				});
		
		tableManager.registerColumn(
			SBC_SearchResult.class, 
			ColumnSearchSubResultCategory.COLUMN_ID,
				new TableColumnCreationListener() {
					
					public void tableColumnCreated(TableColumn column) {
						new ColumnSearchSubResultCategory(column);
					}
				});
	}
	
	public void
	showView()
	{
		SWTSkinObject so_list = getSkinObject("search-results-list");

		if ( so_list != null ){
				
			so_list.setVisible(true);
			
			initTable((Composite) so_list.getControl());
		}
	}
	
	public void
	hideView()
	{
		Utils.disposeSWTObjects(new Object[] {
			table_parent,
		});
	}
	
	private void 
	initTable(
		Composite control ) 
	{
		tv_subs_results = TableViewFactory.createTableViewSWT(
				SBC_SearchResult.class, 
				TABLE_SR,
				TABLE_SR, 
				new TableColumnCore[0], 
				ColumnSearchSubResultName.COLUMN_ID,
				SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL );
		
		TableColumnManager tableManager = TableColumnManager.getInstance();

		tableManager.setDefaultColumnNames( TABLE_SR,
				new String[] {
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
		
		tableManager.setDefaultSortColumnName(TABLE_SR, ColumnSearchSubResultRank.COLUMN_ID);
		
		
		if (txtFilter != null) {
			tv_subs_results.enableFilterCheck(txtFilter, this);
		}
		
		tv_subs_results.setRowDefaultHeight(16);
		
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
					
					SBC_SearchResult rc = (SBC_SearchResult)rows[i].getDataSource();
					
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

					final SBC_SearchResult[] results = new SBC_SearchResult[_related_content.length];

					System.arraycopy(_related_content, 0, results, 0, results.length);
					
					MenuItem item = new MenuItem(menu, SWT.PUSH);
					item.setText(MessageText.getString("label.copy.url.to.clip"));
					item.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							
							StringBuffer buffer = new StringBuffer(1024);
							
							for ( SBC_SearchResult result: results ){
								
								if ( buffer.length() > 0 ){
									buffer.append( "\r\n" );
								}
								
								buffer.append( getDownloadURI( result ));
							}
							ClipboardCopy.copyToClipBoard( buffer.toString());
						};
					});
					
					item.setEnabled( results.length > 0 );
					
					new MenuItem(menu, SWT.SEPARATOR );

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
							
							SBC_SearchResult[] content = new SBC_SearchResult[ selected.length ];
							
							for ( int i=0;i<content.length;i++){
								
								content[i] = (SBC_SearchResult)selected[i];
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
		SBC_SearchResult[] results ) 
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
		
		for ( SBC_SearchResult result: results ){
			
			result.delete();
		}
		
		if ( focusRow != null ){
  		
			tv_subs_results.setSelectedRows(new TableRowCore[]{focusRow });
		}
	};

	public String 
	getUpdateUIName() 
	{
		return( "SearchResultsView" );
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
		SBC_SearchResult ds, 
		String filter, 
		boolean regex)
	{	
		if (!isOurContent(ds)){
			
			return false;
		}

		if ( filter == null || filter.length() == 0 ){
			
			return( true );
		}

		try{
			String name = ds.getName();
			
			String s = regex ? filter : "\\Q" + filter.replaceAll("[|;]", "\\\\E|\\\\Q") + "\\E";
			
			boolean	match_result = true;
			
			if ( regex && s.startsWith( "!" )){
				
				s = s.substring(1);
				
				match_result = false;
			}
			
			Pattern pattern = Pattern.compile(s, Pattern.CASE_INSENSITIVE);
  
			return( pattern.matcher(name).find() == match_result );
			
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
				
				SBC_SearchResult[] related_content = new SBC_SearchResult[_related_content.length];
				
				System.arraycopy( _related_content, 0, related_content, 0, related_content.length );
				
				userDelete( related_content );
			
				return true;
			}
		}
		
		return false;
	}

	public void
	anotherSearch(
		SearchQuery	sq )
	{
		System.out.println( "Native search: " + sq.term );
		
		tv_subs_results.removeAllTableRows();
		
		SWTSkinObjectText title = (SWTSkinObjectText)parent.getSkinObject("title");
			
		if ( title != null ){
				
			title.setText( MessageText.getString( "search.results.view.title", new String[]{ sq.term }));
		}
		
		MetaSearchManager metaSearchManager = MetaSearchManagerFactory.getSingleton();

		ResultListener listener = new ResultListener() {
			
			public void 
			contentReceived(
				Engine engine, 
				String content ) 
			{
			}
			
			public void 
			matchFound(
				Engine 		engine, 
				String[] 	fields ) 
			{
			}
			
			public void 
			engineFailed(
				Engine 		engine, 
				Throwable 	e ) 
			{	
				Debug.out( e );
			}
			
			public void 
			engineRequiresLogin(
				Engine 		engine, 
				Throwable 	e ) 
			{
				Debug.out( e );
			}
			
			public void 
			resultsComplete(
				Engine engine ) 
			{
			
				System.out.println( "Got complete from " + engine.getName());
			}
			
			public void 
			resultsReceived(
				Engine 		engine,
				Result[] 	results) 
			{
				System.out.println( "Got " + results.length + " results from " + engine.getName());
				
				SBC_SearchResult[]	data_sources = new  SBC_SearchResult[ results.length ];
				
				for ( int i=0;i<results.length;i++){
					
					data_sources[i] = new SBC_SearchResult( engine, results[i] );
				}
				
				tv_subs_results.addDataSources( data_sources );
			}
		};
		
		List<SearchParameter>	sps = new ArrayList<SearchParameter>();
					
		sps.add( new SearchParameter( "s", sq.term ));
		
		SearchParameter[] parameters = sps.toArray(new SearchParameter[ sps.size()] );
		
		Map<String,String>	context = new HashMap<String, String>();
		
		context.put( Engine.SC_FORCE_FULL, "true" );
		
		context.put( Engine.SC_BATCH_PERIOD, "250" );
		
		context.put( Engine.SC_REMOVE_DUP_HASH, "true" );
		
		String headers = null;	// use defaults
		
		metaSearchManager.getMetaSearch().search( listener, parameters, headers, context, 500 );

	}
	
	public String
	getDownloadURI(
		SBC_SearchResult	result )
	{
		String torrent_url = (String)result.getTorrentLink();
		
		if ( torrent_url != null && torrent_url.length() > 0 ){
			
			return( torrent_url );
		}
		
		String uri = UrlUtils.getMagnetURI( result.getHash(), result.getName(), new String[]{ AENetworkClassifier.AT_PUBLIC });
		
		return( uri );
	}
	
}
