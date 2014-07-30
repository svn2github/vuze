package com.aelitis.azureus.ui.swt.plugininstall;

import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.installer.InstallablePlugin;
import org.gudy.azureus2.plugins.installer.PluginInstallationListener;
import org.gudy.azureus2.plugins.installer.PluginInstaller;
import org.gudy.azureus2.plugins.installer.StandardPlugin;
import org.gudy.azureus2.plugins.update.Update;
import org.gudy.azureus2.plugins.update.UpdateCheckInstance;
import org.gudy.azureus2.plugins.update.UpdateCheckInstanceListener;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderAdapter;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.UIFunctions;

public class 
SimplePluginInstaller 
{
	private String						plugin_id;
	private UIFunctions.actionListener	action_listener;
	
	private SimplePluginInstallerListener listener;
	private PluginInstaller installer;
	private volatile UpdateCheckInstance instance;
	
	private boolean	completed;
	private boolean	cancelled;
	
	public 
	SimplePluginInstaller(
		String								_plugin_id,
		String								_resource_prefix,
		final UIFunctions.actionListener	_action_listener )
	{
		plugin_id 		= _plugin_id;
		
		PluginInterface existing = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByID( plugin_id );
		
		if ( existing != null ){
			
			if ( existing.getPluginState().isOperational()){
				
				_action_listener.actionComplete( true );
				
			}else{
				
				_action_listener.actionComplete( new Exception( "Plugin is installed but not operational" ));
			}
			
			return;
		}
		
		action_listener	= 
			new UIFunctions.actionListener()
			{
				private boolean informed = false;
			
				public void 
				actionComplete(
					Object result )
				{
					synchronized( this ){
						
						if ( informed ){
							
							return;
						}
						
						informed = true;
					}
					
					_action_listener.actionComplete( result );
				}
			};
			
		SimplePluginInstallWindow window = new SimplePluginInstallWindow( this, _resource_prefix );
			
		window.open();
		
		AEThread2 installerThread = new AEThread2("plugin installer",true) {
			public void 
			run() 
			{
				install();
			}
		};
		
		installerThread.start();	
	}
	
	public void 
	setListener(
		SimplePluginInstallerListener listener) 
	{
		this.listener = listener;
	}
	
	public void 
	cancel() 
	{
		UpdateCheckInstance to_cancel = null;
		
		synchronized( this ){
			
			if ( completed ){
				
				return;
			}
			
			cancelled = true;
			
			to_cancel = instance;
		}
		
		if ( to_cancel != null ){
			
			to_cancel.cancel();
		}
		
		action_listener.actionComplete( new Exception( "Cancelled" ));
	}
		
	public boolean 
	install() 
	{	
		try{
			
			installer = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInstaller();
			
	 		StandardPlugin sp = installer.getStandardPlugin( plugin_id );
						
	 		if ( sp == null ){
	 			
	 			throw( new Exception( "Unknown plugin" ));
	 		}
	 		
			Map<Integer, Object> properties = new HashMap<Integer, Object>();
	
			properties.put( UpdateCheckInstance.PT_UI_STYLE, UpdateCheckInstance.PT_UI_STYLE_NONE );
				
			properties.put(UpdateCheckInstance.PT_UI_DISABLE_ON_SUCCESS_SLIDEY, true);

			final AESemaphore sem = new AESemaphore( "plugin-install" );
			
			final Object[] result = new Object[]{ null };
		
			instance = 
				installer.install(
					new InstallablePlugin[]{ sp },
					false,
					properties,
					new PluginInstallationListener() {

						public void
						completed()
						{
							synchronized( SimplePluginInstaller.this ){
								
								completed = true;
							}
							
							result[0] = true;
							
							if ( listener != null ){
								
								listener.finished();
							}
							
							sem.release();
						}
						
						public void
						cancelled()
						{
							result[0] =  new Exception( "Cancelled" );
							
							if ( listener != null ){
								
								listener.finished();
							}
														
							sem.release();
						}
						
						public void
						failed(
							PluginException	e )
						{
							result[0] = e;
							
							if ( listener != null ){
								
								listener.finished();
							}
														
							sem.release();
						}
					});
		
			boolean kill_it;
			
			synchronized( this ){
				
				kill_it = cancelled;
			}

			if ( kill_it ){
				
				instance.cancel();
				
				action_listener.actionComplete( new Exception( "Cancelled" ));
				
				return( false );
			}
			
			instance.addListener(
				new UpdateCheckInstanceListener() {

					public void
					cancelled(
						UpdateCheckInstance		instance )
					{
					}
					
					public void
					complete(
						UpdateCheckInstance		instance )
					{
	  					Update[] updates = instance.getUpdates();
	 					
	 					for ( final Update update: updates ){
	 						
	 						ResourceDownloader[] rds = update.getDownloaders();
	 					
	 						for ( ResourceDownloader rd: rds ){
	 							
	 							rd.addListener(
	 								new ResourceDownloaderAdapter()
	 								{
	 									public void
	 									reportActivity(
	 										ResourceDownloader	downloader,
	 										String				activity )
	 									{										
	 									}
	 									
	 									public void
	 									reportPercentComplete(
	 										ResourceDownloader	downloader,
	 										int					percentage )
	 									{
	 										if ( listener != null ){
	 											
	 											listener.progress(percentage);
	 										}
	 									}
	 								});
	 						}
	 					}
					}
				});
					
			sem.reserve();
			
			action_listener.actionComplete( result[0] );

			return( result[0] instanceof Boolean );
			
		}catch( Throwable e ){
			
			if ( listener != null ){
				
				listener.finished();
			}

			action_listener.actionComplete( e );
		}
		
		return false;
	}
}
