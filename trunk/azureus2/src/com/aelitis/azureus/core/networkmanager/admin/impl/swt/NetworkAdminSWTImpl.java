/*
 * Created on Aug 14, 2012
 * Created by Paul Gardner
 * 
 * Copyright 2012 Vuze, Inc.  All rights reserved.
 * 
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.core.networkmanager.admin.impl.swt;


import org.eclipse.swt.graphics.Image;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTStatusEntry;
import org.gudy.azureus2.ui.swt.plugins.UISWTStatusEntryListener;
import org.gudy.azureus2.ui.swt.views.stats.StatsView;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.networkmanager.admin.impl.NetworkAdminImpl;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.updater.UIUpdater;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

public class 
NetworkAdminSWTImpl 
{
	private AzureusCore				core;
	private NetworkAdminImpl		network_admin;
	
	private UISWTStatusEntry 	status;
		
	private Image	icon_grey;
	private Image	icon_green;
	private Image	icon_yellow;
	private Image	icon_red;
	
	private Image	last_icon;
	private String	last_tip;
	
	private volatile boolean	is_visible;
	
	public 
	NetworkAdminSWTImpl(
		AzureusCore				_core,
		NetworkAdminImpl		_network_admin )
	{
		core			= _core;
		network_admin	= _network_admin;
		
		final PluginInterface default_pi = PluginInitializer.getDefaultInterface();
	
		default_pi.getUIManager().addUIListener(
				new UIManagerListener()
				{
					public void
					UIAttached(
						final UIInstance		instance )
					{
						if ( instance instanceof UISWTInstance ){
							
							UIFunctions uif = UIFunctionsManager.getUIFunctions();
							
							if ( uif != null ){
								
								uif.getUIUpdater().addListener(
									new UIUpdater.UIUpdaterListener()
									{
										public void 
										updateComplete(
											int count )
										{											
											updateStatus();
										}
									});
							}
							
							Utils.execSWTThread(
								new AERunnable() 
								{
									public void 
									runSupport() 
									{
										ImageLoader imageLoader = ImageLoader.getInstance();
	
										icon_grey	 	= imageLoader.getImage( "st_net_grey" );
										icon_yellow 	= imageLoader.getImage( "st_net_yellow" );
										icon_green 		= imageLoader.getImage( "st_net_green" );
										icon_red		= imageLoader.getImage( "st_net_red" );
				
										final UISWTInstance	ui_instance = (UISWTInstance)instance;
										
										status	= ui_instance.createStatusEntry();
										
										status.setText( MessageText.getString( "label.routing" ));
																				
										status.setImageEnabled( true );
										
										status.setImage( icon_grey );
																				
										final String icon_param = "Show IP Bindings Icon";
										
										final MenuItem mi_show =
											default_pi.getUIManager().getMenuManager().addMenuItem(
																status.getMenuContext(),
																"pairing.ui.icon.show" );
										
										mi_show.setStyle( MenuItem.STYLE_CHECK );
										mi_show.setData( false );
										
										mi_show.addListener(
												new MenuItemListener()
												{
													public void
													selected(
														MenuItem			menu,
														Object 				target )
													{
														COConfigurationManager.setParameter( icon_param, false );
													}
												});
										
										COConfigurationManager.addAndFireParameterListeners(
											new String[]{ "Bind IP", icon_param },
											new ParameterListener()
											{
												public void 
												parameterChanged(
													String parameterName)
												{
													String 	bind_ip 	= COConfigurationManager.getStringParameter("Bind IP", "").trim();

													is_visible	 = 
														bind_ip.trim().length() > 0 &&
														COConfigurationManager.getBooleanParameter( icon_param );
													
													status.setVisible( is_visible );
													
													mi_show.setData( new Boolean( is_visible ));
													
													if ( is_visible ){
														
														updateStatus();
													}
												}
											});
											
										
										MenuItem mi_sep =
											default_pi.getUIManager().getMenuManager().addMenuItem(
																status.getMenuContext(),
																"" );
			
										mi_sep.setStyle( MenuItem.STYLE_SEPARATOR );
										
										MenuItem mi_options =
											default_pi.getUIManager().getMenuManager().addMenuItem(
																status.getMenuContext(),
																"MainWindow.menu.view.configuration" );
			
										mi_options.addListener(
											new MenuItemListener()
											{
												public void
												selected(
													MenuItem			menu,
													Object 				target )
												{
													UIFunctions uif = UIFunctionsManager.getUIFunctions();
			
													if ( uif != null ){
			
														uif.openView( UIFunctions.VIEW_CONFIG, "connection.advanced" );
													}
												}
											});
										
										
										UISWTStatusEntryListener click_listener = 
											new UISWTStatusEntryListener()
										{
												public void 
												entryClicked(
													UISWTStatusEntry entry )
												{
													UIFunctions uif = UIFunctionsManager.getUIFunctions();
													
													if ( uif != null ){
			
														uif.getMDI().loadEntryByID( StatsView.VIEW_ID, true, false, "TransferStatsView" );
													}
												}
											};
											
										status.setListener( click_listener );
									}
								});
						}
								
					}
					
					public void
					UIDetached(
						UIInstance		instance )
					{
						
					}
				});
	}
	
	private void
	updateStatus()
	{
		if ( !is_visible ){
			
			return;
		}
		
		Image	icon;
		String	tip;
				
	    Object[] bs_status = network_admin.getBindingStatus();
	    
	    int		bs_state 	= (Integer)bs_status[0];
	    tip					= (String)bs_status[1];
	    
		if ( bs_state == NetworkAdminImpl.BS_INACTIVE ){
			
			icon 	= icon_grey;
		
		}else if ( bs_state == NetworkAdminImpl.BS_OK){

			icon 	= icon_green;
			
		}else if ( bs_state == NetworkAdminImpl.BS_WARNING ){
			
			icon 	= icon_yellow;
			
		}else{
			
			icon 	= icon_red;
		}
		
		if ( last_icon != icon || !tip.equals( last_tip )){
			
			final Image 	f_icon 	= icon;
			final String	f_tip	= tip;
			
			Utils.execSWTThread(
				new AERunnable() 
				{
					public void 
					runSupport() 
					{
						last_icon 	= f_icon;
						last_tip	= f_tip;
						
						status.setImage( f_icon );
						
						status.setTooltipText( f_tip ); 
					}
				});
		}
	}
}
