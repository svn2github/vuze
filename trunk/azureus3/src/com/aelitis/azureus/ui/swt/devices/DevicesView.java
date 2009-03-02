package com.aelitis.azureus.ui.swt.devices;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.views.AbstractIView;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.devices.*;

import org.gudy.azureus2.plugins.PluginEvent;
import org.gudy.azureus2.plugins.PluginEventListener;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.installer.PluginInstaller;
import org.gudy.azureus2.plugins.installer.StandardPlugin;

public class DevicesView
	extends AbstractIView
	implements TranscodeManagerListener
{	
	private DeviceManager			device_manager;
	private TranscodeManager		transcode_manager;
	
	private Composite 				composite;
	
	public 
	DevicesView()
	{
		device_manager = DeviceManagerFactory.getSingleton();
		
		transcode_manager = device_manager.getTranscodeManager();
		
		transcode_manager.addListener( this );
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
		return MessageText.getString("devices.view.title") + "(old)";
	}
}
