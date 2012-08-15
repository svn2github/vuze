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


package com.aelitis.azureus.core.pairing.impl.swt;

import java.net.URL;

import org.eclipse.swt.graphics.Image;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTStatusEntry;
import org.gudy.azureus2.ui.swt.plugins.UISWTStatusEntryListener;

import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.updater.UIUpdater;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

public class 
PMSWTImpl 
{
	private UISWTStatusEntry 	status;
	
	private Image	icon_idle;
	private Image	icon_green;
	private Image	icon_red;
	
	public void
	initialise(
		final PluginInterface			pi,
		final BooleanParameter			icon_enable )
	{
		pi.getUIManager().addUIListener(
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
											System.out.println( "updated: " + count );
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
	
										icon_idle 	= imageLoader.getImage( "pair_sb_idle" );
										icon_green 	= imageLoader.getImage( "pair_sb_green" );
										icon_red	= imageLoader.getImage( "pair_sb_red" );
				
										UISWTInstance	ui_instance = (UISWTInstance)instance;
										
										status	= ui_instance.createStatusEntry();
											
										status.setTooltipText( "Remote Connection Status" );
										
										status.setImageEnabled( true );
										
										status.setImage( icon_idle );
										
										boolean	is_visible = icon_enable.getValue();
										
										status.setVisible( is_visible );
										
										final MenuItem mi_show =
											pi.getUIManager().getMenuManager().addMenuItem(
																status.getMenuContext(),
																"pairing.ui.icon.show" );
										
										mi_show.setStyle( MenuItem.STYLE_CHECK );
										mi_show.setData( new Boolean( is_visible ));
										
										mi_show.addListener(
												new MenuItemListener()
												{
													public void
													selected(
														MenuItem			menu,
														Object 				target )
													{
														icon_enable.setValue( false );
													}
												});
										
										icon_enable.addListener(
												new ParameterListener()
												{
													public void 
													parameterChanged(
														Parameter param )
													{
														boolean is_visible = icon_enable.getValue();
														
														status.setVisible( is_visible );
														
														mi_show.setData( new Boolean( is_visible ));
													}
												});
											
			
										
										MenuItem mi_sep =
											pi.getUIManager().getMenuManager().addMenuItem(
																status.getMenuContext(),
																"" );
			
										mi_sep.setStyle( MenuItem.STYLE_SEPARATOR );
										
										MenuItem mi_options =
											pi.getUIManager().getMenuManager().addMenuItem(
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
			
														uif.openView( UIFunctions.VIEW_CONFIG, "Pairing" );
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
			
														uif.openView( UIFunctions.VIEW_CONFIG, "Pairing" );
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
	
	private long	last_request;
	
	private TimerEvent	clear_event;
	
	public void
	recordRequest(
		String				name,
		String				ip,
		final boolean		good )
	{
		Utils.execSWTThread(
			new AERunnable() 
			{
				public void 
				runSupport() 
				{
					last_request = SystemTime.getMonotonousTime();
					
					Image	target = good?icon_green:icon_red;
					
					if ( target == null ){
						
						return;
					}
					
					status.setImage( target );
					
					if ( clear_event != null ){
						
						clear_event.cancel();
					}
					
					clear_event = 
						SimpleTimer.addEvent( 
							"", 
							SystemTime.getCurrentTime()+1000,
							new TimerEventPerformer()
							{
								public void 
								perform( final TimerEvent event)
								{
									Utils.execSWTThread(
										new AERunnable() 
										{
											public void 
											runSupport() 
											{
												if ( event == clear_event ){
													
													clear_event = null;
												}
												
												status.setImage( icon_idle );
											}
										});
								}
							});
				}
			});
	}
}
