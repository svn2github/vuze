/*
 * Created on Jul 14, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.ui.swt.content;


import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.menus.MenuManager;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.sidebar.SideBarEntry;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.content.RelatedContentManager;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.swt.views.skin.SkinView;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager.SkinViewManagerListener;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarEntrySWT;

public class 
RelatedContentUI 
{
	private static final boolean	UI_ENABLED = System.getProperty( "vz.rcm.enable", "0" ).equals( "1" );
	
	private PluginInterface		plugin_interface;
	private UIManager			ui_manager;
	
	private RelatedContentManager	manager;
	
	private boolean			ui_setup;
	private SideBar			side_bar;
	private boolean			root_menus_added;
	
	public 
	RelatedContentUI()
	{
		plugin_interface = PluginInitializer.getDefaultInterface();
		
		ui_manager = plugin_interface.getUIManager();

		ui_manager.addUIListener(
				new UIManagerListener()
				{
					public void
					UIAttached(
							UIInstance		instance )
					{
						if ( instance instanceof UISWTInstance ){

							AzureusCoreFactory.addCoreRunningListener(
								new AzureusCoreRunningListener() 
								{
									public void 
									azureusCoreRunning(
										AzureusCore core ) 
									{
										uiAttachedAndCoreRunning(core);
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
	uiAttachedAndCoreRunning(
		AzureusCore core ) 
	{
		Utils.execSWTThread(
			new AERunnable() 
			{
				public void 
				runSupport() 
				{
					SideBar sideBar = (SideBar) SkinViewManager.getByClass(SideBar.class);
					
					if ( sideBar != null ){
						
						setupUI(sideBar);
						
					} else {
						
						SkinViewManager.addListener(
							new SkinViewManagerListener() 
							{
								public void 
								skinViewAdded(
									SkinView skinview ) 
								{
									if (skinview instanceof SideBar) {
									
										setupUI((SideBar) skinview);
										
										SkinViewManager.RemoveListener(this);
									}
								}
							});
					}
				}
			});
	}
	
	protected void
	setupUI(
		SideBar			_side_bar )	
	{
		synchronized( this ){
			
			if ( ui_setup ){
				
				return;
			}
			
			ui_setup = true;
		}
		
		if ( !UI_ENABLED ){
			
			return;
		}
		
		side_bar		= _side_bar;

		try{
			manager 	= RelatedContentManager.getSingleton();
			
			BasicPluginConfigModel config_model = 
				ui_manager.createBasicPluginConfigModel(
					ConfigSection.SECTION_ROOT, "Associations");
			
			final BooleanParameter enabled = 
				config_model.addBooleanParameter2( 
					"rcm.config.enabled", "rcm.config.enabled",
					manager.isEnabled());
			
			enabled.addListener(
					new ParameterListener()
					{
						public void 
						parameterChanged(
							Parameter param) 
						{
							manager.setEnabled( enabled.getValue());
							
							buildSideBar();
						}
					});
			
			buildSideBar();
			
		}catch( Throwable e ){
			
			Debug.out( e );
		}
	}
	
	protected void
	buildSideBar()
	{		
		final String parent_id = "sidebar." + SideBar.SIDEBAR_SECTION_RELATED_CONTENT;

		final SideBarEntrySWT main_sb_entry = SideBar.getEntry( SideBar.SIDEBAR_SECTION_RELATED_CONTENT );

		if ( main_sb_entry != null ){
				
			SideBarEntrySWT subs_entry = SideBar.getEntry( SideBar.SIDEBAR_SECTION_SUBSCRIPTIONS );

			int index = side_bar.getIndexOfEntryRelativeToParent( subs_entry );
			
			if ( index >= 0 ){
				
				index++;
			}
			
			if ( main_sb_entry.getTreeItem() == null ){
				
				if ( manager.isEnabled()){
					
					side_bar.createEntryFromSkinRef(
							null,
							SideBar.SIDEBAR_SECTION_RELATED_CONTENT, "rcmview",
							MessageText.getString("rcm.view.title"),
							null, null, false, index  );
				}else{
					
					return;
				}
			}else if ( !manager.isEnabled()){
				
				main_sb_entry.getTreeItem().dispose();
				
				return;
			}
			
			if ( !root_menus_added ){
				
				root_menus_added = true;
				
				MenuManager menu_manager = ui_manager.getMenuManager();
	
				MenuItem menu_item = menu_manager.addMenuItem( parent_id, "ConfigView.title.short" );
				
				menu_item.addListener( 
						new MenuItemListener() 
						{
							public void 
							selected(
								MenuItem menu, Object target ) 
							{
						      	 UIFunctions uif = UIFunctionsManager.getUIFunctions();
						      	 
						      	 if ( uif != null ){
						      		 
						      		 uif.openView( UIFunctions.VIEW_CONFIG, "Associations" );
						      	 }
							}
						});
			}
		}
	}
}
