package com.aelitis.azureus.ui.swt.devices;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.plugins.installer.PluginInstaller;
import org.gudy.azureus2.plugins.installer.StandardPlugin;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.IView;


import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.devices.DeviceManager;
import com.aelitis.azureus.core.devices.DeviceManagerFactory;
import com.aelitis.azureus.core.devices.TranscodeManagerListener;
import com.aelitis.azureus.core.devices.TranscodeProfile;
import com.aelitis.azureus.core.devices.TranscodeProvider;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.swt.toolbar.ToolBarEnabler;

public class DevicesView
	implements UIUpdatable, IView, ToolBarEnabler, TranscodeManagerListener
{	
	private DeviceManager		device_manager;
	
	private Composite composite;
	
	public 
	DevicesView()
	{
		device_manager = DeviceManagerFactory.getSingleton();
		
		device_manager.getTranscodeManager().addListener( this );
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
		data.top	= new FormAttachment(label,0);

		vuze_button.setLayoutData( data );
		
		TranscodeProvider[] providers = device_manager.getTranscodeManager().getProviders();
		
		Control top = vuze_button;
		
		for ( TranscodeProvider provider: providers ){
			
			data = new FormData();
			data.left 	= new FormAttachment(0,50);
			data.right	= new FormAttachment(100,0);
			data.top	= new FormAttachment(top,0);

			Label prov_lab = new Label( composite, SWT.NULL );
			
			prov_lab.setText( provider.getName());
			
			prov_lab.setLayoutData( data );
			
			top = prov_lab;
			
			TranscodeProfile[] profiles = provider.getProfiles();
			
			for ( TranscodeProfile profile: profiles ){
				
				data = new FormData();
				data.left 	= new FormAttachment(0,100);
				data.right	= new FormAttachment(100,0);
				data.top	= new FormAttachment(top,0);

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
		data.top	= new FormAttachment(top,0);

		itunes_button.setLayoutData( data );
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
		String itemKey ) 
	{
		return false;
	}
	
	public String 
	getUpdateUIName() 
	{

		return null;
	}
	
	public boolean 
	isSelected(
		String itemKey ) 
	{
		return false;
	}
	
	public void 
	itemActivated(
		String itemKey )
	{
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
}
