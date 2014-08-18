/*
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.aelitis.azureus.ui.swt.player;

import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.plugins.PluginException;
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

public class PlayerInstaller {
	
	private PlayerInstallerListener listener;
	private PluginInstaller installer;
	private volatile UpdateCheckInstance instance;
	
	private boolean	cancelled;
	
	public PlayerInstaller() {
	}
	
	public void setListener(PlayerInstallerListener listener) {
		this.listener = listener;
	}
	
	public void 
	cancel() 
	{
		UpdateCheckInstance to_cancel = null;
		
		synchronized( this ){
			
			cancelled = true;
			
			to_cancel = instance;
		}
		
		if ( to_cancel != null ){
			
			to_cancel.cancel();
		}
	}
		
	public boolean install() {
		
		try{
			
			installer = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInstaller();
			
	 		StandardPlugin sp = installer.getStandardPlugin( "azemp" );
						
			Map<Integer, Object> properties = new HashMap<Integer, Object>();
	
			properties.put( UpdateCheckInstance.PT_UI_STYLE, UpdateCheckInstance.PT_UI_STYLE_NONE );
				
			properties.put(UpdateCheckInstance.PT_UI_DISABLE_ON_SUCCESS_SLIDEY, true);

			final AESemaphore sem = new AESemaphore("emp install");
			final boolean[] result = new boolean[1];
		
			instance = 
				installer.install(
					new InstallablePlugin[]{ sp },
					false,
					properties,
					new PluginInstallationListener() {

						public void
						completed()
						{
							result[0] = true;
							if(listener != null) {
								listener.finished();
							}
							sem.release();
						}
						
						public void
						cancelled()
						{
							result[0] = false;
							if(listener != null) {
								listener.finished();
							}
							sem.release();
						}
						
						public void
						failed(
							PluginException	e )
						{
							result[0] = false;
							if(listener != null) {
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
	 										if(listener != null) {
	 											listener.progress(percentage);
	 										}
	 									}
	 								});
	 						}
	 					}
					}
				});
					
			sem.reserve();
			
			return result[0];
			
		}catch( Throwable e ){
			

		}
		return false;
	}

}
