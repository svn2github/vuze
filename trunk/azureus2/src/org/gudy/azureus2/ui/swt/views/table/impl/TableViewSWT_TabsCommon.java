package org.gudy.azureus2.ui.swt.views.table.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.config.impl.ConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance.UISWTViewEventListenerWrapper;
import org.gudy.azureus2.ui.swt.pluginsimpl.*;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;

import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

public class TableViewSWT_TabsCommon
{
	TableViewSWT<?> tv;
	
		/** TabViews */
	private ArrayList<UISWTViewCore> 					tabViews 		= new ArrayList<UISWTViewCore>(1);

	private ArrayList<UISWTViewEventListenerWrapper>	removedViews 	= new ArrayList<UISWTViewEventListenerWrapper>();
	
	/** TabViews */
	private CTabFolder 			tabFolder;
	
	/** Composite that stores the table (sometimes the same as mainComposite) */
	public Composite tableComposite;

	private boolean minimized;
	private UISWTViewCore selectedView;


	public TableViewSWT_TabsCommon(TableViewSWT<?> tv) {
		this.tv = tv;
	}

	public void triggerTabViewsDataSourceChanged(boolean sendParent) {
		if (tabViews == null || tabViews.size() == 0) {
			return;
		}
		
		if (sendParent) {
			for (int i = 0; i < tabViews.size(); i++) {
				UISWTViewCore view = tabViews.get(i);
				if (view != null) {
					view.triggerEvent(UISWTViewEvent.TYPE_DATASOURCE_CHANGED,
							tv.getParentDataSource());
				}
			}
			return;
		}

		// Set Data Object for all tabs.  

		Object[] dataSourcesCore = tv.getSelectedDataSources(true);
		Object[] dataSourcesPlugin = null;

		for (int i = 0; i < tabViews.size(); i++) {
			UISWTViewCore view = tabViews.get(i);
			if (view != null) {
				if (view.useCoreDataSource()) {
					view.triggerEvent(UISWTViewEvent.TYPE_DATASOURCE_CHANGED,
							dataSourcesCore.length == 0 ? tv.getParentDataSource()
									: dataSourcesCore);
				} else {
					if (dataSourcesPlugin == null) {
						dataSourcesPlugin = tv.getSelectedDataSources(false);
					}

					view.triggerEvent(
							UISWTViewEvent.TYPE_DATASOURCE_CHANGED,
							dataSourcesPlugin.length == 0 ? PluginCoreUtils.convert(
									tv.getParentDataSource(), false) : dataSourcesPlugin);
				}
			}
		}
	}

	public void triggerTabViewsDataSourceChanged(TableViewSWT<?> tv) {
		if (tabViews == null || tabViews.size() == 0) {
			return;
		}

		// Set Data Object for all tabs.  

		Object[] dataSourcesCore = tv.getSelectedDataSources(true);
		Object[] dataSourcesPlugin = null;

		for (int i = 0; i < tabViews.size(); i++) {
			UISWTViewCore view = tabViews.get(i);
			if (view != null) {
				if (view.useCoreDataSource()) {
					view.triggerEvent(UISWTViewEvent.TYPE_DATASOURCE_CHANGED,
							dataSourcesCore.length == 0 ? tv.getParentDataSource()
									: dataSourcesCore);
				} else {
					if (dataSourcesPlugin == null) {
						dataSourcesPlugin = tv.getSelectedDataSources(false);
					}

					view.triggerEvent(
							UISWTViewEvent.TYPE_DATASOURCE_CHANGED,
							dataSourcesPlugin.length == 0 ? PluginCoreUtils.convert(
									tv.getParentDataSource(), false) : dataSourcesPlugin);
				}
			}
		}
	}
	
	public void triggerTabViewDataSourceChanged(UISWTViewCore view) {
		if (view != null) {
			view.triggerEvent(UISWTViewEvent.TYPE_DATASOURCE_CHANGED, tv.getParentDataSource());

			if (view.useCoreDataSource()) {
				Object[] dataSourcesCore = tv.getSelectedDataSources(true);
				if (dataSourcesCore.length > 0) {
					view.triggerEvent(UISWTViewEvent.TYPE_DATASOURCE_CHANGED,
							dataSourcesCore.length == 0 ? tv.getParentDataSource()
									: dataSourcesCore);
				}
			} else {
				Object[] dataSourcesPlugin = tv.getSelectedDataSources(false);
				if (dataSourcesPlugin.length > 0) {
					view.triggerEvent(
							UISWTViewEvent.TYPE_DATASOURCE_CHANGED,
							dataSourcesPlugin.length == 0 ? PluginCoreUtils.convert(
									tv.getParentDataSource(), false) : dataSourcesPlugin);
				}
			}
		}
		
	}

	public void delete() {
		if (tabViews != null && tabViews.size() > 0) {
			for (int i = 0; i < tabViews.size(); i++) {
				UISWTViewCore view = tabViews.get(i);
				if (view != null) {
					view.triggerEvent(UISWTViewEvent.TYPE_DESTROY, null);
				}
			}
		}
	}

	public void generate(IndentWriter writer) {
		writer.println("# of SubViews: " + tabViews.size());
		writer.indent();
		try {
			for (Iterator<UISWTViewCore> iter = tabViews.iterator(); iter.hasNext();) {
				UISWTViewCore view = iter.next();
				writer.println(view.getTitleID() + ": " + view.getFullTitle());
			}
		} finally {
			writer.exdent();
		}
	}

	public void localeChanged() {
		if (tabViews != null && tabViews.size() > 0) {
			for (int i = 0; i < tabViews.size(); i++) {
				UISWTViewCore view = tabViews.get(i);
				if (view != null) {
					view.triggerEvent(UISWTViewEvent.TYPE_LANGUAGEUPDATE, null);
				}
			}
		}
	}

	public UISWTViewCore getActiveSubView() {
		if (!tv.isTabViewsEnabled() || tabFolder == null || tabFolder.isDisposed()
				|| minimized) {
			return null;
		}

		return selectedView;
	}

	public void refreshSelectedSubView() {
		UISWTViewCore view = getActiveSubView();
		if (view != null && view.getComposite().isVisible()) {
			view.triggerEvent(UISWTViewEvent.TYPE_REFRESH, null);
		}
	}

	private String
	getViewTitleID(
		String	view_id )
	{
		String history_key = "swt.ui.table.tab.view.namecache." + view_id;

		String id = COConfigurationManager.getStringParameter( history_key, "" );

		if ( id.length() == 0 ){
			
			String test = view_id + ".title.full";
			
			if ( MessageText.keyExists( test )){
			
				return( test );
			}
			
			id = "!" + view_id + "!";
		}
		
		return( id );
	}

		// TabViews Functions
	
	private void 
	addTabView(
		UISWTViewEventListenerWrapper 	listener,
		boolean							start_of_day,
		boolean							start_minimized )
	{
		if ( tabFolder == null){
			
			return;
		}
				
		String view_id = listener.getViewID();
		
		try{
			UISWTViewImpl view = new UISWTViewImpl(tv.getTableID(), view_id, listener, null);
			
			triggerTabViewDataSourceChanged(view);
	
			int	insert_at = tabFolder.getItemCount();
			
			if ( !start_of_day && insert_at > 0 ){
				
				UIFunctionsSWT ui_func  = UIFunctionsManagerSWT.getUIFunctionsSWT();
				
				if ( ui_func != null ){
					
					UISWTInstance ui_swt = ui_func.getUISWTInstance();

					if ( ui_swt != null ){
						
						ArrayList<UISWTViewEventListenerWrapper> listeners = new ArrayList<UISWTViewEventListenerWrapper>( Arrays.asList( ui_swt.getViewListeners(tv.getTableID())));
					
						int	l_index = listeners.indexOf( listener );
						
						CTabItem[] items = tabFolder.getItems();
													
						for ( int j=0;j<items.length;j++){
									
							UISWTViewImpl v = (UISWTViewImpl)items[j].getData( "IView" );
									
							if ( v != null ){
										
								int v_index = listeners.indexOf( v.getEventListener());
							
								if ( v_index > l_index ){
									
									insert_at = j;
									
									break;
								}
							}
						}
					}
				}
			}
			
			CTabItem item = new CTabItem( tabFolder, SWT.NULL, insert_at );
			
			boolean	is_minimized;
			
			if ( start_of_day ){
				
				is_minimized = start_minimized;
				
			}else{
				
				is_minimized = tabFolder.getMinimized();
			}
						
			item.setToolTipText( MessageText.getString( is_minimized?"label.click.to.restore":"label.dblclick.to.min"));
			
			item.setData("IView", view);
			
			String title_id = view.getTitleID();
			
			String history_key = "swt.ui.table.tab.view.namecache." + view_id;
			
			String existing = COConfigurationManager.getStringParameter( history_key, "" );
			
			if ( !existing.equals( title_id )){
				
				COConfigurationManager.setParameter( history_key, title_id );
			}
			
			Messages.setLanguageText(item, title_id );
			
			view.initialize(tabFolder);
			
			item.setControl(view.getComposite());
			
			tabViews.add(view);
			
			if ( !start_of_day ){
				
				removedViews.remove( listener );
				
				tabFolder.setSelection(item);
				selectedView = (UISWTViewImpl)item.getData( "IView" );
			}

		}catch( Throwable e ){
			
			Debug.out( e );
		}
	}
	
	private void
	checkTabViews(
		Map		closed )
	{
		for ( UISWTViewEventListenerWrapper l: new ArrayList<UISWTViewEventListenerWrapper>( removedViews )){
			
			String view_id = l.getViewID();
			
			if ( !closed.containsKey( view_id )){
				
				addTabView( l, false, false );
			}
		}
		
		for ( CTabItem item: tabFolder.getItems()){
			
			UISWTViewImpl view = (UISWTViewImpl)item.getData( "IView" );
			
			if ( view != null ){
				
				String view_id = view.getViewID();
				
				if ( closed.containsKey( view_id )){
					
					removeTabView( item );
				}
			}
		}
	}
	
	private void
	removeTabView(
		CTabItem		item )
	{
		UISWTViewImpl view = (UISWTViewImpl)item.getData( "IView" );
		
		if ( view != null ){
						
			removedViews.add((UISWTViewEventListenerWrapper)view.getEventListener());
			
			tabViews.remove( view );
			
			view.triggerEvent(UISWTViewEvent.TYPE_DESTROY, null);
		
			item.dispose();
		}
	}

	public Composite createSashForm(final Composite composite) {
		if (!tv.isTabViewsEnabled()) {
			tableComposite = tv.createMainPanel(composite);
			return tableComposite;
		}

		ConfigurationManager configMan = ConfigurationManager.getInstance();
		
		int iNumViews = 0;

		UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
		UISWTViewEventListenerWrapper[] pluginViews = null;
		if (uiFunctions != null) {
			UISWTInstance pluginUI = uiFunctions.getUISWTInstance();

			if (pluginUI != null) {
				pluginViews = pluginUI.getViewListeners(tv.getTableID());
				iNumViews += pluginViews.length;
			}
		}

		if (iNumViews == 0) {
			tableComposite = tv.createMainPanel(composite);
			return tableComposite;
		}

		final String	props_prefix = tv.getTableID() + "." + tv.getPropertiesPrefix();
		
		FormData formData;

		final Composite form = new Composite(composite, SWT.NONE);
		FormLayout flayout = new FormLayout();
		flayout.marginHeight = 0;
		flayout.marginWidth = 0;
		form.setLayout(flayout);
		GridData gridData;
		gridData = new GridData(GridData.FILL_BOTH);
		form.setLayoutData(gridData);

		// Create them in reverse order, so we can have the table auto-grow, and
		// set the tabFolder's height manually

		final int TABHEIGHT = 22;
		tabFolder = new CTabFolder(form, SWT.TOP | SWT.BORDER);
		tabFolder.setMinimizeVisible(true);
		tabFolder.setTabHeight(TABHEIGHT);
		final int iFolderHeightAdj = tabFolder.computeSize(SWT.DEFAULT, 0).y;

		final Sash sash = new Sash(form, SWT.HORIZONTAL);

		tableComposite = tv.createMainPanel(form);
		Composite cFixLayout = tableComposite;
		while (cFixLayout != null && cFixLayout.getParent() != form) {
			cFixLayout = cFixLayout.getParent();
		}
		if (cFixLayout == null) {
			cFixLayout = tableComposite;
		}
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		cFixLayout.setLayout(layout);

		// FormData for Folder
		formData = new FormData();
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		formData.bottom = new FormAttachment(100, 0);
		int iSplitAt = configMan.getIntParameter(props_prefix + ".SplitAt",
				3000);
		// Was stored at whole
		if (iSplitAt < 100) {
			iSplitAt *= 100;
		}

		double pct = iSplitAt / 10000.0;
		if (pct < 0.03) {
			pct = 0.03;
		} else if (pct > 0.97) {
			pct = 0.97;
		}

		// height will be set on first resize call
		sash.setData("PCT", new Double(pct));
		tabFolder.setLayoutData(formData);
		final FormData tabFolderData = formData;

		// FormData for Sash
		formData = new FormData();
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		formData.bottom = new FormAttachment(tabFolder);
		formData.height = 5;
		sash.setLayoutData(formData);

		// FormData for table Composite
		formData = new FormData();
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		formData.top = new FormAttachment(0, 0);
		formData.bottom = new FormAttachment(sash);
		cFixLayout.setLayoutData(formData);

		// Listeners to size the folder
		sash.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				final boolean FASTDRAG = true;

				if (FASTDRAG && e.detail == SWT.DRAG) {
					return;
				}

				if (tabFolder.getMinimized()) {
					tabFolder.setMinimized(false);
					refreshSelectedSubView();
					ConfigurationManager configMan = ConfigurationManager.getInstance();
					configMan.setParameter(props_prefix + ".subViews.minimized",
							false);
				}

				Rectangle area = form.getClientArea();
				tabFolderData.height = area.height - e.y - e.height - iFolderHeightAdj;
				form.layout();

				Double l = new Double((double) tabFolder.getBounds().height
						/ form.getBounds().height);
				sash.setData("PCT", l);
				if (e.detail != SWT.DRAG) {
					ConfigurationManager configMan = ConfigurationManager.getInstance();
					configMan.setParameter(props_prefix + ".SplitAt",
							(int) (l.doubleValue() * 10000));
				}
			}
		});

		final CTabFolder2Adapter folderListener = new CTabFolder2Adapter() {
			public void minimize(CTabFolderEvent event) {
				minimized = true;
				
				tabFolder.setMinimized(true);
				tabFolderData.height = iFolderHeightAdj;
				CTabItem[] items = tabFolder.getItems();
				String tt = MessageText.getString( "label.click.to.restore" );
				for (int i = 0; i < items.length; i++) {
					CTabItem tabItem = items[i];
					tabItem.setToolTipText( tt );
					tabItem.getControl().setVisible(false);
				}
				form.layout();

				UISWTViewCore view = getActiveSubView();
				
				fireFocusLost( view );	// fire even if null so we pick up current...
				
				ConfigurationManager configMan = ConfigurationManager.getInstance();
				configMan.setParameter(props_prefix + ".subViews.minimized", true);
			}

			public void restore(CTabFolderEvent event) {
				minimized = false;
				tabFolder.setMinimized(false);
				CTabItem selection = tabFolder.getSelection();
				if (selection != null) {
					selection.getControl().setVisible(true);
				}
				
				CTabItem[] items = tabFolder.getItems();
				String tt = MessageText.getString( "label.dblclick.to.min"  );
				
				for (int i = 0; i < items.length; i++) {
					CTabItem tabItem = items[i];
					tabItem.setToolTipText( tt );
				}
				
				form.notifyListeners(SWT.Resize, null);

				UISWTViewCore view = getActiveSubView();
				if (view != null) {
					fireFocusGained( view );
				}
				refreshSelectedSubView();

				ConfigurationManager configMan = ConfigurationManager.getInstance();
				configMan.setParameter(props_prefix + ".subViews.minimized", false);
				
				tabFolder.setToolTipText( "max" );
			}

		};
		
		tabFolder.addCTabFolder2Listener(folderListener);

		tabFolder.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				selectedView = null;
				// make sure its above
				try {
					((CTabItem) e.item).getControl().setVisible(true);
					((CTabItem) e.item).getControl().moveAbove(null);

					selectedView = (UISWTViewImpl)e.item.getData( "IView" );
					// Call getActiveSubView and don't use selectedView. Function
					// may return null even if there's a selectedView
					UISWTViewCore view = getActiveSubView();
					if (view != null) {
						fireFocusGained( view );
					}
					
				} catch (Exception t) {
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		tabFolder.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				if (tabFolder.getMinimized()) {
					folderListener.restore(null);
					// If the user clicked down on the restore button, and we restore
					// before the CTabFolder does, CTabFolder will minimize us again
					// There's no way that I know of to determine if the mouse is 
					// on that button!

					// one of these will tell tabFolder to cancel
					e.button = 0;
					tabFolder.notifyListeners(SWT.MouseExit, null);
				}
			}
			public void mouseDoubleClick(MouseEvent e) {
				if (!tabFolder.getMinimized()) {
					folderListener.minimize( null );
				}
			}
		});

		final Menu menu = new Menu( tabFolder );
		
		tabFolder.addListener(SWT.MenuDetect, new Listener() {
			public void handleEvent(Event event) {
				
				for ( MenuItem item: menu.getItems()){
					
					item.dispose();
				}
												
				final CTabItem item = tabFolder.getItem( tabFolder.toControl( event.x, event.y ));
				
				boolean	need_sep = false;
				
				if ( item != null ){
					
					tabFolder.setSelection(item);
					selectedView = (UISWTViewImpl)item.getData( "IView" );
						
					if ( selectedView != null ){
						
						final String view_id = selectedView.getViewID();
						
						MenuItem mi = new MenuItem( menu, SWT.PUSH );
						
						mi.setText( MessageText.getString( "label.close.tab" ));
						
						mi.addListener(
							SWT.Selection,
							new Listener()
							{
								public void 
								handleEvent(
									Event event ) 
								{
									String key = props_prefix + ".closedtabs";
									
									Map closedtabs = COConfigurationManager.getMapParameter(key, new HashMap());
									
									if ( !closedtabs.containsKey( view_id )){
										
										closedtabs.put( view_id, "" );
										
										COConfigurationManager.setParameter( key, closedtabs );
									}
								}
							});
						
						need_sep = true;
					}
				}else{
					
					for ( final UISWTViewEventListenerWrapper l: removedViews ){
						
						need_sep = true;
						
						final String view_id = l.getViewID();
						
						MenuItem mi = new MenuItem( menu, SWT.PUSH );
						
						mi.setText( MessageText.getString( getViewTitleID( view_id )));
						
						mi.addListener(
							SWT.Selection,
							new Listener()
							{
								public void 
								handleEvent(
									Event event ) 
								{
									String key = props_prefix + ".closedtabs";
									
									Map closedtabs = COConfigurationManager.getMapParameter(key, new HashMap());
									
									if ( closedtabs.containsKey( view_id )){
									
										closedtabs.remove( view_id );
									
										COConfigurationManager.setParameter( key, closedtabs );
									}
								}
							});
					}
				}
				
				if ( need_sep ){
				
					new MenuItem( menu, SWT.SEPARATOR );
				}
				
				final MenuItem mi = new MenuItem( menu, SWT.CHECK );
				
				mi.setSelection( COConfigurationManager.getBooleanParameter( "Library.ShowTabsInTorrentView"));
				
				mi.setText( MessageText.getString( "ConfigView.section.style.ShowTabsInTorrentView" ));
				
				mi.addListener(
					SWT.Selection,
					new Listener()
					{
						public void 
						handleEvent(
							Event event ) 
						{
							COConfigurationManager.setParameter( "Library.ShowTabsInTorrentView", mi.getSelection());
						}
					});
				
				menu.setVisible(true);

			}
		});
		
		form.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event e) {
				if (tabFolder.getMinimized()) {
					return;
				}

				Double l = (Double) sash.getData("PCT");
				if (l != null) {
					tabFolderData.height = (int) (form.getBounds().height * l.doubleValue())
							- iFolderHeightAdj;
					form.layout();
				}
			}
		});

		String key = props_prefix + ".closedtabs";
		
		Map closed_tabs = COConfigurationManager.getMapParameter(key, new HashMap());

		COConfigurationManager.addParameterListener(
			key,
			new ParameterListener()
			{
				public void 
				parameterChanged(
					String	name )
				{
					if ( tabFolder.isDisposed()){
						
						COConfigurationManager.removeParameterListener( name, this );
						
					}else{
						
						checkTabViews( COConfigurationManager.getMapParameter(name, new HashMap()));
					}
				}
			});
		
		String[] restricted_to = tv.getTabViewsRestrictedTo();
		
		Set<String> rt_set = new HashSet<String>();
		
		if ( restricted_to != null ){
			
			rt_set.addAll( Arrays.asList( restricted_to ));
		}
		
		boolean folder_minimized = 
			configMan.getBooleanParameter( props_prefix + ".subViews.minimized", !tv.getTabViewsExpandedByDefault());
		
		// Call plugin listeners
		
		if (pluginViews != null) {
			for (UISWTViewEventListenerWrapper l : pluginViews) {
				if (l != null) {
					try {
						String view_id = l.getViewID();
						
						if ( restricted_to == null || rt_set.contains( view_id )){
						
							if ( closed_tabs.containsKey( view_id )){
								
								removedViews.add( l );
								
							}else{
								
								addTabView( l, true, folder_minimized );
							}
						}
					} catch (Exception e) {
						// skip, plugin probably specifically asked to not be added
					}
				}
			}
		}

		if ( folder_minimized ){
			
			tabFolder.setMinimized(true);
			
			tabFolderData.height = iFolderHeightAdj;
			
		}else{
			
			tabFolder.setMinimized(false);
		}

		if (tabFolder.getItemCount() > 0) {
			CTabItem item = tabFolder.getItem(0);
			tabFolder.setSelection(item);
			selectedView = (UISWTViewImpl)item.getData( "IView" );
		}

		return form;
	}

	private UISWTViewCore	focused_view = null;
	
	private void
	fireFocusGained(
		UISWTViewCore		view )
	{
		if ( focused_view != null ){
			
			focused_view.triggerEvent(UISWTViewEvent.TYPE_FOCUSLOST, null);
		}
		
		focused_view = view;
		
		view.triggerEvent(UISWTViewEvent.TYPE_FOCUSGAINED, null);
	}
	
	private void
	fireFocusLost(
		UISWTViewCore		view )
	{
		if ( focused_view != null && focused_view != view ){
			
			focused_view.triggerEvent(UISWTViewEvent.TYPE_FOCUSLOST, null);
		}
		
		focused_view = null;
		
		if ( view != null ){
		
			view.triggerEvent(UISWTViewEvent.TYPE_FOCUSLOST, null);
		}
	}
	
	public void swt_refresh() {
		if (tv.isTabViewsEnabled() && tabFolder != null && !tabFolder.isDisposed()
				&& !tabFolder.getMinimized()) {
			refreshSelectedSubView();
		}
	}
}
