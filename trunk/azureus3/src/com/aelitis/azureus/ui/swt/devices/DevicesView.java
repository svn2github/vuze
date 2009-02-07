package com.aelitis.azureus.ui.swt.devices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.plugins.PluginEvent;
import org.gudy.azureus2.plugins.PluginEventListener;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.installer.PluginInstaller;
import org.gudy.azureus2.plugins.installer.StandardPlugin;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.IView;


import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.devices.DeviceManager;
import com.aelitis.azureus.core.devices.DeviceManagerFactory;
import com.aelitis.azureus.core.devices.TranscodeJob;
import com.aelitis.azureus.core.devices.TranscodeManager;
import com.aelitis.azureus.core.devices.TranscodeManagerListener;
import com.aelitis.azureus.core.devices.TranscodeProfile;
import com.aelitis.azureus.core.devices.TranscodeProvider;
import com.aelitis.azureus.core.devices.TranscodeQueue;
import com.aelitis.azureus.core.devices.TranscodeQueueListener;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.selectedcontent.ISelectedContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.swt.toolbar.ToolBarEnabler;
import com.aelitis.azureus.ui.swt.toolbar.ToolBarEnablerSelectedContent;

public class DevicesView
	implements UIUpdatable, IView, ToolBarEnabler, TranscodeManagerListener, TranscodeQueueListener
{	
	private DeviceManager			device_manager;
	private TranscodeManager		transcode_manager;
	private TranscodeQueue			transcode_queue;
	
	private Composite 				composite;
	private Table					job_table;
	private List<TranscodeJob>		transcode_jobs;
	private List<TranscodeJob>		selected_jobs		= new ArrayList<TranscodeJob>();
	
	public 
	DevicesView()
	{
		device_manager = DeviceManagerFactory.getSingleton();
		
		transcode_manager = device_manager.getTranscodeManager();
		
		transcode_manager.addListener( this );
		
		transcode_queue = transcode_manager.getQueue();
		
		transcode_jobs	= new ArrayList<TranscodeJob>( Arrays.asList( transcode_queue.getJobs()));
		
		transcode_queue.addListener( this );
	}
	
	public void 
	initialize(
		Composite parent ) 
	{
		composite = new Composite(parent,SWT.NONE);
		
		FormLayout layout = new FormLayout();
		
		layout.marginTop	= 4;
		layout.marginLeft	= 4;
		layout.marginRight	= 4;
		layout.marginBottom	= 4;
		
		composite.setLayout( layout );
		
		build();
		
		AzureusCoreFactory.getSingleton().getPluginManager().getDefaultPluginInterface().addEventListener(
				new PluginEventListener()
				{
					public void 
					handleEvent(
						PluginEvent ev )
					{
						int	type = ev.getType();
						
						if ( 	type == PluginEvent.PEV_PLUGIN_OPERATIONAL || 
								type == PluginEvent.PEV_PLUGIN_NOT_OPERATIONAL ){
							
							if (((PluginInterface)ev.getValue()).getPluginID().equals( "azitunes" )){
			
								rebuild();
							}
						}
					}
				});
	}
	
	protected void
	build()
	{
		FormData data = new FormData();
		data.left 	= new FormAttachment(0,0);
		data.right	= new FormAttachment(100,0);
		data.top	= new FormAttachment(composite,0);

		Label label = new Label( composite, SWT.NULL );
		
		label.setText( "Transcode Providers" );
		
		label.setLayoutData( data );
		
		Button vuze_button = new Button( composite, SWT.NULL );
		
		vuze_button.setText( "Install Vuze Transcoder" );
		
		PluginInstaller installer = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInstaller();
			
		StandardPlugin vuze_plugin = null;
		
		try{
			vuze_plugin = installer.getStandardPlugin( "vuzexcode" );

		}catch( Throwable e ){	
		}
		
		if ( vuze_plugin == null || vuze_plugin.isAlreadyInstalled()){
			
			vuze_button.setEnabled( false );
		}
		
		final StandardPlugin	f_vuze_plugin = vuze_plugin;
		
		vuze_button.addListener(
			SWT.Selection, 
			new Listener() 
			{
				public void 
				handleEvent(
					Event arg0 ) 
				{
					try{
						f_vuze_plugin.install( true );

					}catch( Throwable e ){
						
					}
				}
			});
		
		data = new FormData();
		data.left 	= new FormAttachment(0,0);
		data.top	= new FormAttachment(label,4);

		vuze_button.setLayoutData( data );
		
		TranscodeProvider[] providers = device_manager.getTranscodeManager().getProviders();
		
		Control top = vuze_button;
		
		for ( TranscodeProvider provider: providers ){
			
			data = new FormData();
			data.left 	= new FormAttachment(0,50);
			data.right	= new FormAttachment(100,0);
			data.top	= new FormAttachment(top,4);

			Label prov_lab = new Label( composite, SWT.NULL );
			
			prov_lab.setText( provider.getName());
			
			prov_lab.setLayoutData( data );
			
			top = prov_lab;
			
			TranscodeProfile[] profiles = provider.getProfiles();
			
			for ( TranscodeProfile profile: profiles ){
				
				data = new FormData();
				data.left 	= new FormAttachment(0,100);
				data.right	= new FormAttachment(100,0);
				data.top	= new FormAttachment(top,4);

				Label prof_lab = new Label( composite, SWT.NULL );
				
				prof_lab.setText( profile.getName());
				
				prof_lab.setLayoutData( data );
				
				top = prof_lab;
			}
		}
		
		Button itunes_button = new Button( composite, SWT.NULL );
		
		itunes_button.setText( "Install iTunes Integration" );
		

		StandardPlugin itunes_plugin = null;
		
		try{
			itunes_plugin = installer.getStandardPlugin( "azitunes" );

		}catch( Throwable e ){	
		}
		
		if ( itunes_plugin == null || itunes_plugin.isAlreadyInstalled()){
			
			itunes_button.setEnabled( false );
		}
		
		final StandardPlugin	f_itunes_plugin = itunes_plugin;
		
		itunes_button.addListener(
			SWT.Selection, 
			new Listener() 
			{
				public void 
				handleEvent(
					Event arg0 ) 
				{
					try{
						f_itunes_plugin.install( true );

					}catch( Throwable e ){
						
					}
				}
			});
		
		data = new FormData();
		data.left 	= new FormAttachment(0,0);
		data.top	= new FormAttachment(top,4);

		itunes_button.setLayoutData( data );
		
		data = new FormData();
		data.left 	= new FormAttachment(0,0);
		data.right	= new FormAttachment(100,0);
		data.top	= new FormAttachment(itunes_button,4);

		Label jobs_label = new Label( composite, SWT.NULL );
		
		jobs_label.setText( "Transcode Jobs" );
		
		jobs_label.setLayoutData( data );

	
		
		
		Composite table_composite = new Composite( composite, SWT.NULL );
		
		data = new FormData();
		data.left 	= new FormAttachment(0,0);
		data.right	= new FormAttachment(100,0);
		data.top	= new FormAttachment(jobs_label,4);
		data.bottom	= new FormAttachment(100,0);

		table_composite.setLayoutData( data );

		
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		table_composite.setLayout(layout);
		
		job_table = new Table(table_composite, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.VIRTUAL);

		final String[] headers = { 
				"azbuddy.ui.table.name", 
				"devices.device",  
				"devices.profile",
		};

		int[] sizes = { 300, 100, 150 };

		int[] aligns = { SWT.LEFT, SWT.CENTER, SWT.CENTER };

		for (int i = 0; i < headers.length; i++){

			TableColumn tc = new TableColumn(job_table, aligns[i]);
				
			tc.setWidth(sizes[i]);

			Messages.setLanguageText(tc, headers[i]);
		}	

		job_table.setHeaderVisible(true);

	    TableColumn[] columns = job_table.getColumns();
	    
	    columns[0].setData(new Integer(FilterComparator.FIELD_NAME));
	    columns[1].setData(new Integer(FilterComparator.FIELD_DEVICE));
	    columns[1].setData(new Integer(FilterComparator.FIELD_PROFILE));
	    
	    
	    final FilterComparator comparator = new FilterComparator();
	    
	    Listener sort_listener = 
	    	new Listener() 
	    	{
		    	public void 
		    	handleEvent(
		    		Event e ) 
		    	{
		    		TableColumn tc = (TableColumn) e.widget;
	
		    		int field = ((Integer) tc.getData()).intValue();
	
		    		comparator.setField( field );
	
		    		Collections.sort( transcode_jobs,comparator);
	
		    		updateTable();
		    	}
	    	};
	    
	    for (int i=0;i<columns.length;i++){
	    	
	    	columns[i].addListener(SWT.Selection,sort_listener);
	    }	    
	    
	    GridData gridData = new GridData(GridData.FILL_BOTH);
	    gridData.heightHint = job_table.getHeaderHeight() * 3;
	    job_table.setLayoutData(gridData);
		
		
	    job_table.addListener(
			SWT.SetData,
			new Listener()
			{
				public void 
				handleEvent(
					Event event) 
				{
					TableItem item = (TableItem)event.item;
					
					int index = job_table.indexOf(item);
	
					if ( index < 0 || index >= transcode_jobs.size()){
						
						return;
					}
					
					TranscodeJob job = transcode_jobs.get( index );
										
					item.setText(0, job.getName());
					item.setText(1, job.getTarget().getDevice().getName());
					item.setText(2, job.getProfile().getName());
					

					
					item.setData( job );
				}
			});
		
		final Listener tt_label_listener = 
			new Listener() 
			{
				public void handleEvent(Event event) {
					Label label = (Label) event.widget;
					Shell shell = label.getShell();
					switch (event.type) {
					case SWT.MouseDown:
						Event e = new Event();
						e.item = (TableItem) label.getData("_TABLEITEM");
						job_table.setSelection(new TableItem[] { (TableItem) e.item });
						job_table.notifyListeners(SWT.Selection, e);
						// fall through
					case SWT.MouseExit:
						shell.dispose();
						break;
					}
				}
			};


		Listener	tt_table_listener = 
			new Listener()
			{
				private Shell tip = null;

				private Label label = null;

				public void 
				handleEvent(
					Event event ) 
				{
					switch (event.type){
						case SWT.Dispose:
						case SWT.KeyDown:
						case SWT.MouseMove: {
							if (tip == null)
								break;
							tip.dispose();
							tip = null;
							label = null;
							break;
						}
						case SWT.MouseHover: 
						{
							Point mouse_position = new Point(event.x, event.y);
							
							TableItem item = job_table.getItem( mouse_position );
														
							if (item != null) {
								
								if (tip != null && !tip.isDisposed()){
									
									tip.dispose();
									
									tip = null;
								}
								
								int index = job_table.indexOf(item);
								
								if ( index < 0 || index >= transcode_jobs.size()){
									
									return;
								}

								TranscodeJob job = transcode_jobs.get( index );

								String tooltip = getToolTip( job );

								if ( tooltip == null ){
									
									return;
								}
								
								int	item_index = 0;
								
								for (int i=0;i<headers.length;i++){
									
									Rectangle bounds = item.getBounds(i);
									
									if ( bounds.contains( mouse_position )){
										
										item_index = i;
										
										break;
									}
								}
								
								if ( item_index != 0 ){
									
									return;
								}
								
								tip = new Shell(job_table.getShell(), SWT.ON_TOP | SWT.TOOL);
								tip.setLayout(new FillLayout());
								label = new Label(tip, SWT.NONE);
								label.setForeground(job_table.getDisplay()
										.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
								label.setBackground(job_table.getDisplay()
										.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
								label.setData("_TABLEITEM", item);
								
								label.setText( tooltip );
								
								label.addListener(SWT.MouseExit, tt_label_listener);
								label.addListener(SWT.MouseDown, tt_label_listener);
								Point size = tip.computeSize(SWT.DEFAULT, SWT.DEFAULT);
								Rectangle rect = item.getBounds(item_index);
								Point pt = job_table.toDisplay(rect.x, rect.y);
								tip.setBounds(pt.x, pt.y, size.x, size.y);
								tip.setVisible(true);
							}
						}
					}
				}
				
				protected String
				getToolTip(
						TranscodeJob	buddy )
				{
					return( null );
				}
			};
			
		job_table.addListener(SWT.Dispose, tt_table_listener);
		job_table.addListener(SWT.KeyDown, tt_table_listener);
		job_table.addListener(SWT.MouseMove, tt_table_listener);
		job_table.addListener(SWT.MouseHover, tt_table_listener);
		    			
		job_table.addSelectionListener(
			new SelectionAdapter() 
			{
				public void 
				widgetSelected(
					SelectionEvent e) 
				{
					setJobSelection();
				};
			});

		job_table.addListener(SWT.MouseUp, 
			new Listener()
			{
				public void 
				handleEvent(
					Event arg0 )
				{
					setJobSelection();
				}
			});

		final Menu menu = new Menu(job_table);
		
		final MenuItem remove_item = new MenuItem(menu, SWT.PUSH);
		
		remove_item.setText( MessageText.getString( "azbuddy.ui.menu.remove" ));

		remove_item.addSelectionListener(
			new SelectionAdapter() 
			{
				public void 
				widgetSelected(
					SelectionEvent e) 
				{
					TableItem[] selection = job_table.getSelection();
					
					for (int i=0;i<selection.length;i++){
						
						TranscodeJob job = (TranscodeJob)selection[i].getData();
						
						job.remove();
					}
				};
			});
		
		
		job_table.setMenu( menu );
				
		Collections.sort( transcode_jobs, comparator );

		updateTable();
	}
	
	protected void
	updateTable()
	{
		if ( job_table != null && !job_table.isDisposed()){
		
			job_table.setItemCount( transcode_jobs.size());
		
			job_table.clearAll();
		
			job_table.redraw();
			
			setJobSelection();
		}
	}
	
	protected void
	setJobSelection()
	{
		TableItem[] selection = job_table.getSelection();
		
		if ( selection.length == 0 ){
			
			selected_jobs.clear();
			
			SelectedContentManager.clearCurrentlySelectedContent();
			
		}else{
			
			List<TranscodeJob>	jobs = new ArrayList<TranscodeJob>();
			
			for ( TableItem row: selection ){
				
				TranscodeJob job = (TranscodeJob)row.getData();
				
				jobs.add( job );
			}
			
			if ( jobs.equals( selected_jobs )){
				
				return;
			}
			
			selected_jobs = jobs;
			
			ISelectedContent[] sels = new ISelectedContent[1];

			sels[0] = new ToolBarEnablerSelectedContent((ToolBarEnabler)DevicesView.this);

			SelectedContentManager.changeCurrentlySelectedContent( "IconBarEnabler", sels );
		}
	}
	
	public void
	jobAdded(
		TranscodeJob		job )
	{
		synchronized( transcode_jobs ){
			
			if ( transcode_jobs.contains( job )){
				
				return;
			}
			
			transcode_jobs.add( job );
		}
		
		if ( job_table != null && !job_table.isDisposed()){
			
			job_table.getDisplay().asyncExec(
					new Runnable()
					{
						public void
						run()
						{
							if ( !job_table.isDisposed()){
																	
								updateTable();
							}
						}
					});
		}
	}
	
	public void
	jobChanged(
		TranscodeJob		job )
	{
		
	}
	
	public void
	jobRemoved(
		TranscodeJob		job )
	{
		synchronized( transcode_jobs ){
			
			if ( !transcode_jobs.remove( job )){
				
				return;
			}
		}
		
		if ( job_table != null && !job_table.isDisposed()){
			
			job_table.getDisplay().asyncExec(
					new Runnable()
					{
						public void
						run()
						{
							if ( !job_table.isDisposed()){
																	
								updateTable();
							}
						}
					});
		}
	}
	
	protected void
	rebuild()
	{
		Utils.execSWTThread(
			new Runnable()
			{
				public void
				run()
				{
					for ( Control control: composite.getChildren()){
						
						control.dispose();
					}
					
					build();
					
					composite.layout();
				}
			});
	}
	
	public void
	providerAdded(
		TranscodeProvider	provider )
	{
		rebuild();
	}
	
	public void
	providerUpdated(
		TranscodeProvider	provider )
	{
		rebuild();
	}
	
	public void
	providerRemoved(
		TranscodeProvider	provider )
	{
		rebuild();
	}
	
	public void 
	delete() 
	{
		if ( composite != null && !composite.isDisposed()){
			
			composite.dispose();
			
			composite = null;
		}
	}
	
	public boolean 
	isEnabled(
		String item_key ) 
	{
		if ( selected_jobs.size() == 0 ){
			
			return( false );
		}
		
		if ( item_key.equals( "remove" )){
		
			return( true );
		}
		
		return( false );
	}
	
	public String 
	getUpdateUIName() 
	{

		return null;
	}
	
	public boolean 
	isSelected(
		String item_key ) 
	{
		return( false );
	}
	
	public void 
	itemActivated(
		String item_key )
	{
		if ( item_key.equals( "remove" )){
			
			List<TranscodeJob> jobs = new ArrayList<TranscodeJob>( selected_jobs );
			
			for ( TranscodeJob job: jobs ){
				
				job.remove();
			}
		}
	}
	
	public void 
	updateUI() 
	{
	}
	
	public void 
	dataSourceChanged(
		Object newDataSource) 
	{
	}
	

	
	public void 
	generateDiagnostics(
		IndentWriter writer) 
	{
	}
	
	public Composite 
	getComposite() 
	{
		return composite;
	}
	
	public String 
	getData() 
	{
		return( "devices.view.title" );
	}
	
	public String 
	getFullTitle() 
	{
		return MessageText.getString("devices.view.title");
	}
	
	public String 
	getShortTitle() 
	{
		return( getFullTitle());
	}
	

	
	public void 
	refresh() 
	{
	}
	
	public void 
	updateLanguage() 
	{
	}
	
	protected class 
	FilterComparator 
		implements Comparator<TranscodeJob>
	{
		boolean ascending = false;

		static final int FIELD_NAME			= 0;
		static final int FIELD_DEVICE 		= 1;
		static final int FIELD_PROFILE 		= 2;


		int field = FIELD_NAME;

		public int 
		compare(
			TranscodeJob j1,
			TranscodeJob j2 ) 
		{
			
			int	res = 0;
			
			if(field == FIELD_NAME){				
				 res = j1.getName().compareTo( j2.getName());
			}else if(field == FIELD_DEVICE){
				res = j1.getTarget().getDevice().getName().compareTo( j2.getTarget().getDevice().getName());
			}else if(field == FIELD_PROFILE){
				res = j1.getProfile().getName().compareTo( j2.getProfile().getName());
			}
			
			return(( ascending ? 1 : -1) * res );
		}
	
		public void 
		setField(
			int newField ) 
		{      
			if(field == newField) ascending = ! ascending;
			
			field = newField;
		}
	}
}
