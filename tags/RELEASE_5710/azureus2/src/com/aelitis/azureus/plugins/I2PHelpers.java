/*
 * Created on Mar 6, 2015
 * Created by Paul Gardner
 * 
 * Copyright 2015 Azureus Software, Inc.  All rights reserved.
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


package com.aelitis.azureus.plugins;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AENetworkClassifier;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginManager;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.UIFunctionsUserPrompter;

public class 
I2PHelpers 
{
	private static final Object i2p_install_lock = new Object();
	
	private static boolean i2p_installing = false;
	
	public static boolean
	isI2PInstalled()
	{
		if ( isInstallingI2PHelper()){
			
			return( true );
		}
	
		PluginManager pm = AzureusCoreFactory.getSingleton().getPluginManager();
					
		return( pm.getPluginInterfaceByID( "azneti2phelper" ) != null );
	}

	public static boolean
	isInstallingI2PHelper()
	{
		synchronized( i2p_install_lock ){
			
			return( i2p_installing );
		}
	}

	public static boolean
	installI2PHelper(
		String				remember_id,
		final boolean[]		install_outcome,
		final Runnable		callback )
	{
		return installI2PHelper(null, remember_id, install_outcome, callback);
	}

	public static boolean
	installI2PHelper(
		String extra_text,
		String				remember_id,
		final boolean[]		install_outcome,
		final Runnable		callback )
	{
		synchronized( i2p_install_lock ){
			
			if ( i2p_installing ){
				
				Debug.out( "I2P Helper already installing" );
				
				return( false );
			}
			
			i2p_installing = true;
		}
		
		boolean	installing = false;
		
		try{
			UIFunctions uif = UIFunctionsManager.getUIFunctions();
			
			if ( uif == null ){
				
				Debug.out( "UIFunctions unavailable - can't install plugin" );
				
				return( false );
			}
			
			String title = MessageText.getString("azneti2phelper.install");
			
			String text = "";
			if (extra_text != null) {
				text = extra_text + "\n\n";
			}
			text += MessageText.getString("azneti2phelper.install.text" );
			
			UIFunctionsUserPrompter prompter = uif.getUserPrompter(title, text, new String[] {
				MessageText.getString("Button.yes"),
				MessageText.getString("Button.no")
			}, 0);
			
			if ( remember_id != null ){

				prompter.setRemember( 
					remember_id, 
					false,
					MessageText.getString("MessageBoxWindow.nomoreprompting"));
			}
			
			prompter.setAutoCloseInMS(0);
			
			prompter.open(null);
			
			boolean	install = prompter.waitUntilClosed() == 0;
			
			if ( install ){
	
				installing = true;
				
				uif.installPlugin(
						"azneti2phelper",
						"azneti2phelper.install",
						new UIFunctions.actionListener()
						{
							public void
							actionComplete(
								Object		result )
							{
								try{
									if ( callback != null ){
										
										if ( result instanceof Boolean ){
											
											install_outcome[0] = (Boolean)result;
										}
										
										callback.run();
									}
								}finally{
																		
									synchronized( i2p_install_lock ){
											
										i2p_installing = false;
									}
								}	
							}
						});
			
			}else{
				
				Debug.out( "I2P Helper install declined (either user reply or auto-remembered)" );
			}
			
			return( install );
			
		}finally{
			
			if ( !installing ){
			
				synchronized( i2p_install_lock ){
					
					i2p_installing = false;
				}
			}
		}
	}
}
