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


import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TreeItem;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.ByteArrayHashMap;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemFillListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.menus.MenuManager;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.sidebar.SideBarCloseListener;
import org.gudy.azureus2.plugins.ui.sidebar.SideBarEntry;
import org.gudy.azureus2.plugins.ui.sidebar.SideBarVitalityImage;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.views.AbstractIView;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.content.RelatedContent;
import com.aelitis.azureus.core.content.RelatedContentManager;
import com.aelitis.azureus.core.content.RelatedContentManagerListener;
import com.aelitis.azureus.core.devices.Device;
import com.aelitis.azureus.core.devices.DeviceMediaRenderer;
import com.aelitis.azureus.core.devices.TranscodeFile;
import com.aelitis.azureus.core.devices.TranscodeTargetListener;
import com.aelitis.azureus.core.metasearch.Engine;
import com.aelitis.azureus.core.metasearch.impl.web.WebEngine;
import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.core.subs.SubscriptionHistory;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
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
	
	private ByteArrayHashMap<RCMItem>	rcm_item_map = new ByteArrayHashMap<RCMItem>();
	
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
			
			hookMenus();
			
			buildSideBar();
			
		}catch( Throwable e ){
			
			Debug.out( e );
		}
	}
	
	protected void
	hookMenus()
	{
		TableManager	table_manager = plugin_interface.getUIManager().getTableManager();

		String[]	table_ids = {
				TableManager.TABLE_MYTORRENTS_INCOMPLETE,
				TableManager.TABLE_MYTORRENTS_COMPLETE,
				TableManager.TABLE_MYTORRENTS_ALL_BIG,
				TableManager.TABLE_MYTORRENTS_COMPLETE_BIG,
				TableManager.TABLE_MYTORRENTS_INCOMPLETE_BIG,
		};
		
		for ( String table_id: table_ids ){
			
			TableContextMenuItem menu_item = table_manager.addContextMenuItem( table_id, "rcm.contextmenu.lookupassoc");
		
			menu_item.setStyle( TableContextMenuItem.STYLE_PUSH );

			MenuItemListener listener = 
				new MenuItemListener()
				{
					public void 
					selected(
						MenuItem 	menu, 
						Object 		target) 
					{
						TableRow[]	rows = (TableRow[])target;
						
						if ( rows.length > 0 ){
							
							Download download = (Download)rows[0].getDataSource();
							
							explicitSearch( download );
						}
					}
				};
				
				menu_item.addMultiListener( listener );
		}
	}
	
	protected void
	explicitSearch(
		Download		download )
	{
		addSearch( download );
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
					
					main_sb_entry.setDatasource(
						new RelatedContentEnumerator()
						{
							public void
							enumerate(
								RelatedContentManagerListener	listener )
							{
								
							}
						});
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
	
	protected void
	addSearch(
		final Download		download )
	{
		synchronized( this ){
				
			Torrent	torrent = download.getTorrent();
			
			if ( torrent == null ){
				
				return;
			}
			
			final byte[] hash = torrent.getHash();
			
			final RCMItem existing_si = rcm_item_map.get( hash );
			
			if (  existing_si == null ){
	
				final RCMItem new_si = new RCMItem( download, hash );
				
				rcm_item_map.put( hash, new_si );
				
				Utils.execSWTThread(
					new Runnable()
					{
						public void
						run()
						{
							synchronized( RelatedContentUI.this ){

								if ( new_si.isDestroyed()){
									
									return;
								}
								
								RCMView view = new RCMView( SideBar.SIDEBAR_SECTION_RELATED_CONTENT, download );
								
								new_si.setView( view );
								
								String key = "RCM_" + ByteFormatter.encodeString( hash );
								
								SideBarEntrySWT	entry = side_bar.createEntryFromSkinRef(
										SideBar.SIDEBAR_SECTION_RELATED_CONTENT,
										key, "rcmview",
										view.getTitle(),
										view, null, true, -1 );
								
								new_si.setTreeItem( entry.getTreeItem(), entry );
								
								/*
								TreeItem  tree_item = 
									side_bar.createTreeItemFromIView(
										SideBar.SIDEBAR_SECTION_RELATED_CONTENT, 
										view,
										key, 
										null, 
										true, 
										true,
										false );
										
									SideBarEntrySWT	entry = SideBar.getEntry( key );
																
									new_si.setTreeItem( tree_item, entry );
								*/
																
									
								/*
								PluginInterface pi = PluginInitializer.getDefaultInterface();
								UIManager uim = pi.getUIManager();
								MenuManager menuManager = uim.getMenuManager();
								
								MenuItem menuItem;
								*/
								
								new_si.activate();
							}
						}
					});
			}else{
				
				Utils.execSWTThread(
						new Runnable()
						{
							public void
							run()
							{
								ViewTitleInfoManager.refreshTitleInfo( existing_si.getView());
								
								SideBarEntrySWT mainSBEntry = SideBar.getEntry(SideBar.SIDEBAR_SECTION_RELATED_CONTENT );
								
								if ( mainSBEntry != null ){
									
									ViewTitleInfoManager.refreshTitleInfo( mainSBEntry.getTitleInfo());
								}
								
								existing_si.activate();
							}
						});
			}
		}
	}
	
	protected class
	RCMView
		implements 	ViewTitleInfo
	{
		private String			parent_key;
		private Download		download;
		
		private Composite		parent_composite;
		private Composite		composite;
		
		private int last_indicator;

		protected
		RCMView(
			String			_parent_key,
			Download		_download )
		{
			parent_key	= _parent_key;
			download	= _download;
		}
		
		public Object 
		getTitleInfoProperty(
			int propertyID ) 
		{		
			if ( propertyID == TITLE_TEXT ){
				
				return( getTitle());
				
			}else if ( propertyID == TITLE_INDICATOR_TEXT ){
				

			}else if ( propertyID == TITLE_INDICATOR_COLOR ){
	
			}
			
			return null;
		}
		
		public String
		getTitle()
		{
			return( download.getName());
		}
		
		protected void
		refreshTitles()
		{
			ViewTitleInfoManager.refreshTitleInfo( this );

			String	key = parent_key;
			
			while( key != null ){
			
				SideBarEntrySWT parent = SideBar.getEntry( key );
			
				if ( parent != null ){
				
					ViewTitleInfoManager.refreshTitleInfo(parent.getTitleInfo());
					
					key = parent.getParentID();
				}
			}
		}
	}
	
	private static final String SPINNER_IMAGE_ID 	= "image.sidebar.vitality.dl";

	protected static void
	hideIcon(
		SideBarVitalityImage	x )
	{
		if ( x == null ){
			return;
		}
		
		x.setVisible( false );
		x.setToolTip( "" );
	}
	
	protected static void
	showIcon(
		SideBarVitalityImage	x ,
		String					t )
	{
		if ( x == null ){
			return;
		}
		
		x.setToolTip( t );
		x.setVisible( true );
	}
	
	public class
	RCMItem
		implements RelatedContentEnumerator, SideBarCloseListener
	{	
		private Download			download;
		private byte[]				hash;
		
		private RCMView				view;
		private SideBarEntrySWT		sb_entry;
		private TreeItem			tree_item;
		private boolean				destroyed;
		
		private SideBarVitalityImage	warning;
		private SideBarVitalityImage	spinner;
		private SideBarVitalityImage	info;
		
		protected
		RCMItem(
			Download	_download,
			byte[]		_hash )
		{
			download	= _download;
			hash		= _hash;
		}
		
		protected void
		setTreeItem(
			TreeItem		_tree_item,
			SideBarEntrySWT	_sb_entry )
		{
			tree_item	= _tree_item;
			sb_entry	= _sb_entry;
			
			sb_entry.setDatasource( this );
			
			sb_entry.addListener( this );
			
			spinner = sb_entry.addVitalityImage( SPINNER_IMAGE_ID );
		}
		
		public void
		enumerate(
			final RelatedContentManagerListener	listener )
		{
			try{
				showIcon( spinner, null );
				
				try{
					listener.lookupStarted( download );
					
				}catch( Throwable e ){
					
					Debug.out( e );
				}
				
				manager.lookupContent(
					download,
					new RelatedContentManagerListener()
					{
						public void
						lookupStarted(
							Download		for_download )
						{
							
						}
						
						public void
						foundContent(
							Download		for_download,
							RelatedContent	content )
						{
							try{
								listener.foundContent( download, content );
								
							}catch( Throwable e ){
								
								Debug.out( e );
							}
						}
						
						public void
						lookupCompleted(
							Download		for_download )
						{
							try{
								listener.lookupStarted( download );
								
							}catch( Throwable e ){
								
								Debug.out( e );
							}
							
							hideIcon( spinner );
						}
					});
			}catch( Throwable e ){
				
				Debug.out( e );
				
				try{
					listener.lookupCompleted( download );
					
				}catch( Throwable f ){
					
					Debug.out( f );
				}
				
				hideIcon( spinner );
			}
		}
		
		protected TreeItem
		getTreeItem()
		{
			return( tree_item );
		}
		
		protected SideBarEntrySWT
		getSideBarEntry()
		{
			return( sb_entry );
		}
		
		protected void
		setView(
			RCMView		_view )
		{
			view	= _view;
		}
		
		protected RCMView
		getView()
		{
			return( view );
		}
		
		protected void
		setStatus(
			Device	device )
		{
				// possible during initialisation, status will be shown again on complete
			
			if ( spinner != null ){
			

			}
			
			if ( view != null ){
				
				view.refreshTitles();
			}
		}
		
		protected boolean
		isDestroyed()
		{
			return( destroyed );
		}
		
		public void 
		sidebarClosed(
			SideBarEntry entry )
		{
			destroy();
		}
		
		protected void
		destroy()
		{
			destroyed = true;
			
			synchronized( RelatedContentUI.this ){
					
				rcm_item_map.remove( hash );
			}
		}
		
		public void 
		activate() 
		{
			SideBar sideBar = (SideBar)SkinViewManager.getByClass(SideBar.class);
			
			if ( sideBar != null && sb_entry != null ){
				
				sideBar.showEntryByID(sb_entry.getId());
			}
		}
	}
}
