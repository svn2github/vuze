package com.aelitis.azureus.ui.swt.subscriptions;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;

import org.gudy.azureus2.core3.category.Category;
import org.gudy.azureus2.core3.category.CategoryManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.UIInputReceiver;
import org.gudy.azureus2.plugins.ui.UIInputReceiverListener;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.menus.*;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;
import org.gudy.azureus2.ui.swt.plugins.UISWTInputReceiver;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.views.utils.TagUIUtils;

import com.aelitis.azureus.core.metasearch.Engine;
import com.aelitis.azureus.core.metasearch.impl.web.WebEngine;
import com.aelitis.azureus.core.subs.*;
import com.aelitis.azureus.core.tag.Tag;
import com.aelitis.azureus.core.tag.TagManager;
import com.aelitis.azureus.core.tag.TagManagerFactory;
import com.aelitis.azureus.core.tag.TagType;
import com.aelitis.azureus.core.vuzefile.VuzeFile;
import com.aelitis.azureus.ui.UserPrompterResultListener;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.mdi.MdiEntry;
import com.aelitis.azureus.ui.mdi.MdiEntryVitalityImage;
import com.aelitis.azureus.ui.swt.mdi.BaseMdiEntry;

public class SubscriptionMDIEntry implements SubscriptionListener, ViewTitleInfo
{
	private static final String ALERT_IMAGE_ID	= "image.sidebar.vitality.alert";
	private static final String AUTH_IMAGE_ID	= "image.sidebar.vitality.auth";

	private final MdiEntry mdiEntry;

	MdiEntryVitalityImage spinnerImage;

	private MdiEntryVitalityImage warningImage;
	private final Subscription subs;
	private String key;

	public SubscriptionMDIEntry(Subscription subs, MdiEntry entry) {
		this.subs = subs;
		this.mdiEntry = entry;
		key = "Subscription_" + ByteFormatter.encodeString(subs.getPublicKey());
		setupMdiEntry();
	}
	
	private void setupMdiEntry() {
		if (mdiEntry == null) {
			return;
		}
		
		mdiEntry.setViewTitleInfo(this);

		mdiEntry.setImageLeftID("image.sidebar.subscriptions");
		
		warningImage = mdiEntry.addVitalityImage( ALERT_IMAGE_ID );
		
		spinnerImage = mdiEntry.addVitalityImage("image.sidebar.vitality.dots");
		
		if (spinnerImage != null) {
			spinnerImage.setVisible(false);
		}
		
		setWarning();

		PluginInterface pi = PluginInitializer.getDefaultInterface();
		UIManager uim = pi.getUIManager();
		
		final MenuManager menuManager = uim.getMenuManager();
		
		MenuItem menuItem;
		
		menuItem = menuManager.addMenuItem("sidebar." + key,"Subscription.menu.forcecheck");
		menuItem.setText(MessageText.getString("Subscription.menu.forcecheck"));
		menuItem.addListener(new SubsMenuItemListener() {
			public void selected(MdiEntry info, Subscription subs) {
				try {
					subs.getManager().getScheduler().downloadAsync( subs, true );
				} catch (SubscriptionException e) {
					Debug.out(e);
				}
			}
		});
		
		menuItem = menuManager.addMenuItem("sidebar." + key,"Subscription.menu.clearall");
		menuItem.addListener(new SubsMenuItemListener() {
			public void selected(MdiEntry info, Subscription subs) {
				subs.getHistory().markAllResultsRead();
				refreshView();
			}
		});
		
		menuItem = menuManager.addMenuItem("sidebar." + key,"Subscription.menu.dirtyall");
		menuItem.addListener(new SubsMenuItemListener() {
			public void selected(MdiEntry info, Subscription subs) {
				subs.getHistory().markAllResultsUnread();
				refreshView();
			}
		});

		menuItem = menuManager.addMenuItem("sidebar." + key,"Subscription.menu.deleteall");
		menuItem.addListener(new SubsMenuItemListener() {
			public void selected(MdiEntry info, Subscription subs) {
				subs.getHistory().deleteAllResults();
				refreshView();
			}
		});
		
		menuItem = menuManager.addMenuItem("sidebar." + key,"Subscription.menu.reset");
		menuItem.addListener(new SubsMenuItemListener() {
			public void selected(MdiEntry info, Subscription subs) {
				subs.getHistory().reset();
				try{
					subs.getEngine().reset();
				}catch( Throwable e ){
					Debug.printStackTrace(e);
				}
				try{
					subs.getManager().getScheduler().downloadAsync(subs, true);
					
				}catch( Throwable e ){
					
					Debug.out(e);
				}
			}
		});

		try{
			Engine engine = subs.getEngine();
				
			if ( engine instanceof WebEngine ){
				
				if (((WebEngine)engine).isNeedsAuth()){
					
					menuItem = menuManager.addMenuItem("sidebar." + key,"Subscription.menu.resetauth");
					menuItem.addListener(new SubsMenuItemListener() {
						public void selected(MdiEntry info, Subscription subs) {
							try{
								Engine engine = subs.getEngine();
								
								if ( engine instanceof WebEngine ){
									
									((WebEngine)engine).setCookies( null );
								}
							}catch( Throwable e ){
								
								Debug.printStackTrace(e);
							}
							
							try{
								subs.getManager().getScheduler().downloadAsync(subs, true);
								
							}catch( Throwable e ){
								
								Debug.out(e);
							}
						}
					});
					
					menuItem = menuManager.addMenuItem("sidebar." + key,"Subscription.menu.setcookies");
					menuItem.addListener(new SubsMenuItemListener() {
						public void selected(MdiEntry info, final Subscription subs) {
							try{
								Engine engine = subs.getEngine();
								
								if ( engine instanceof WebEngine ){
									
									final WebEngine we = (WebEngine)engine;
									
									UISWTInputReceiver entry = new SimpleTextEntryWindow();
									
									String[] req = we.getRequiredCookies();
									
									String	req_str = "";
									
									for ( String r:req ){
										
										req_str += (req_str.length()==0?"":";") + r + "=?";
									}
									entry.setPreenteredText( req_str, true );
									entry.maintainWhitespace(false);
									entry.allowEmptyInput( false );
									entry.setTitle("general.enter.cookies");
									entry.prompt(new UIInputReceiverListener() {
										public void UIInputReceiverClosed(UIInputReceiver entry) {
											if (!entry.hasSubmittedInput()){
												
												return;
											}

											try {
		  									String input = entry.getSubmittedInput().trim();
		  									
		  									if ( input.length() > 0 ){
		  										
		  										we.setCookies( input );
		  										
		  										subs.getManager().getScheduler().downloadAsync(subs, true);
		  									}
											}catch( Throwable e ){
												
												Debug.printStackTrace(e);
											}
										}
									});
								}
							}catch( Throwable e ){
								
								Debug.printStackTrace(e);
							}
						}
					});
				}
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
		
			// sep
		
		menuManager.addMenuItem("sidebar." + key,"s1").setStyle( MenuItem.STYLE_SEPARATOR );

			// category
		
		menuItem = menuManager.addMenuItem("sidebar." + key, "MyTorrentsView.menu.setCategory");
		menuItem.setStyle( MenuItem.STYLE_MENU );
		
		menuItem.addFillListener(
			new MenuItemFillListener()
			{
				public void 
				menuWillBeShown(
					MenuItem 	menu, 
					Object 		data ) 
				{		
					addCategorySubMenu( menuManager, menu );
				}
			});
		
			// tag
		
		menuItem = menuManager.addMenuItem("sidebar." + key, "label.tag");
		menuItem.setStyle( MenuItem.STYLE_MENU );
		
		menuItem.addFillListener(
			new MenuItemFillListener()
			{
				public void 
				menuWillBeShown(
					MenuItem 	menu, 
					Object 		data ) 
				{		
					addTagSubMenu( menuManager, menu );
				}
			});
		
		if ( subs.isUpdateable()){
			
			menuItem = menuManager.addMenuItem("sidebar." + key,"MyTorrentsView.menu.rename");
			menuItem.addListener(new SubsMenuItemListener() {
				public void selected(MdiEntry info, final Subscription subs) {
					UISWTInputReceiver entry = new SimpleTextEntryWindow();
					entry.setPreenteredText(subs.getName(), false );
					entry.maintainWhitespace(false);
					entry.allowEmptyInput( false );
					entry.setLocalisedTitle(MessageText.getString("label.rename",
							new String[] {
								subs.getName()
							}));
					entry.prompt(new UIInputReceiverListener() {
						public void UIInputReceiverClosed(UIInputReceiver entry) {
							if (!entry.hasSubmittedInput()){
								
								return;
							}
							
							String input = entry.getSubmittedInput().trim();
							
							if ( input.length() > 0 ){
								
								try{
									subs.setName( input );
									
								}catch( Throwable e ){
									
									Debug.printStackTrace(e);
								}
							}
						}
					});
				}
			});
		}
		
		menuItem = menuManager.addMenuItem("sidebar." + key,"Subscription.menu.upgrade");
		menuItem.addListener(new SubsMenuItemListener() {
			public void selected(MdiEntry info, Subscription subs) {
				subs.resetHighestVersion();
			}
		});
			
		menuItem.addFillListener(
			new MenuItemFillListener()
			{
				public void 
				menuWillBeShown(
					MenuItem 	menu, 
					Object 		data ) 
				{									
					menu.setVisible( subs.getHighestVersion() > subs.getVersion());
				}
			});
		
		menuItem = menuManager.addMenuItem("sidebar." + key,"Subscription.menu.export");
		menuItem.addListener(new SubsMenuItemListener() {
			public void selected(MdiEntry info, Subscription subs) {
				export();
			}
		});
		
			// sep
		
		menuManager.addMenuItem("sidebar." + key,"s2").setStyle( MenuItem.STYLE_SEPARATOR );
		
		menuItem = menuManager.addMenuItem("sidebar." + key,"Subscription.menu.remove");
		menuItem.addListener(new SubsMenuItemListener() {
			public void selected(MdiEntry info, Subscription subs) {
				removeWithConfirm();
			}
		});
		
		menuManager.addMenuItem("sidebar." + key,"s3").setStyle( MenuItem.STYLE_SEPARATOR );

		menuItem = menuManager.addMenuItem("sidebar." + key,"Subscription.menu.properties");
		menuItem.addListener(new SubsMenuItemListener() {
			public void selected(MdiEntry info, Subscription subs) {
				showProperties();
			}
		});
		
		subs.addListener(this); 
	}

	public void subscriptionDownloaded(Subscription subs, boolean auto) {
	}
	
	public void subscriptionChanged(Subscription subs) {
		mdiEntry.redraw();
		ViewTitleInfoManager.refreshTitleInfo(mdiEntry.getViewTitleInfo());
	}

	protected void refreshView() {
		if (!(mdiEntry instanceof BaseMdiEntry)) {
			return;
		}
		UISWTViewEventListener eventListener = ((BaseMdiEntry)mdiEntry).getEventListener();
		if (eventListener instanceof SubscriptionView) {
			SubscriptionView subsView = (SubscriptionView) eventListener;
			subsView.refreshView();
		}
	}

	protected void
	setWarning()
	{
			// possible during initialisation, status will be shown again on complete
		
		if ( warningImage == null ){
			
			return;
		}
		
		SubscriptionHistory history = subs.getHistory();
		
		String	last_error = history.getLastError();

		boolean	auth_fail = history.isAuthFail();
		
			// don't report problem until its happened a few times, but not for auth fails as this is a perm error
		
		if ( history.getConsecFails() < 3 && !auth_fail ){
			
			last_error = null;
		}
		
		boolean	trouble = last_error != null;
		
		if ( trouble ){
		 
			warningImage.setToolTip( last_error );
			
			warningImage.setImageID( auth_fail?AUTH_IMAGE_ID:ALERT_IMAGE_ID );
			
			warningImage.setVisible( true );
			
		}else{
			
			warningImage.setVisible( false );
			
			warningImage.setToolTip( "" );
		}
	}

	private void 
	addCategorySubMenu(
		MenuManager				menu_manager,
		MenuItem				menu )
	{
		menu.removeAllChildItems();

		Category[] categories = CategoryManager.getCategories();
		
		Arrays.sort( categories );

		MenuItem m;

		if ( categories.length > 0 ){
			
			String	assigned_category = subs.getCategory();
			
			final Category uncat = CategoryManager.getCategory( Category.TYPE_UNCATEGORIZED );
						
			if ( uncat != null ){
				
				m = menu_manager.addMenuItem( menu, uncat.getName());
				
				m.setStyle( MenuItem.STYLE_RADIO );
								
				m.setData( new Boolean( assigned_category == null ));
				
				m.addListener(
					new MenuItemListener() 
					{
						public void
						selected(
							MenuItem			menu,
							Object 				target )
						{
							assignSelectedToCategory( uncat );
						}
					});
				

				m = menu_manager.addMenuItem( menu, "sep1" );
				
				m.setStyle( MenuItem.STYLE_SEPARATOR );
			}

			for ( int i=0; i<categories.length; i++ ){
				
				final Category cat = categories[i];
				
				if ( cat.getType() == Category.TYPE_USER) {
					
					m = menu_manager.addMenuItem( menu, "!" + cat.getName() + "!" );
					
					m.setStyle( MenuItem.STYLE_RADIO );
										
					m.setData( new Boolean( assigned_category != null && assigned_category.equals( cat.getName())));
					
					m.addListener(
						new MenuItemListener() 
						{
							public void
							selected(
								MenuItem			menu,
								Object 				target )
							{
								assignSelectedToCategory( cat );
							}
						});
				}
			}

			m = menu_manager.addMenuItem( menu, "sep2" );
			
			m.setStyle( MenuItem.STYLE_SEPARATOR );
		}

		m = menu_manager.addMenuItem( menu, "MyTorrentsView.menu.setCategory.add" );
		
		m.addListener(
				new MenuItemListener() 
				{
					public void
					selected(
						MenuItem			menu,
						Object 				target )
					{
						addCategory( );
					}
				});

	}

	private void 
	addCategory()	
	{
		CategoryAdderWindow adderWindow = new CategoryAdderWindow(Display.getDefault());
		
		Category newCategory = adderWindow.getNewCategory();
		
		if ( newCategory != null ){
		
			assignSelectedToCategory( newCategory );
		}
	}

	private void 
	assignSelectedToCategory(
		Category 			category )
	{
		if ( category.getType() == Category.TYPE_UNCATEGORIZED ){
		
			subs.setCategory( null );
			
		}else{
			
			subs.setCategory( category.getName());
		}
	}

	private void 
	addTagSubMenu(
		MenuManager				menu_manager,
		MenuItem				menu )
	{
		menu.removeAllChildItems();

		TagManager tm = TagManagerFactory.getTagManager();
		
		List<Tag> tags = tm.getTagType( TagType.TT_DOWNLOAD_MANUAL ).getTags();
		
		tags = TagUIUtils.sortTags( tags );
					
		long	tag_id = subs.getTagID();
			
		Tag assigned_tag = tm.lookupTagByUID( tag_id );
		
		MenuItem m = menu_manager.addMenuItem( menu, "label.no.tag" );
				
		m.setStyle( MenuItem.STYLE_RADIO );
							
		m.setData( new Boolean( assigned_tag == null ));
				
		m.addListener(
			new MenuItemListener() 
			{
				public void
				selected(
					MenuItem			menu,
					Object 				target )
				{
					subs.setTagID( -1 );
				}
			});
				

		m = menu_manager.addMenuItem( menu, "sep1" );
				
		m.setStyle( MenuItem.STYLE_SEPARATOR );
	
		for ( final Tag tag: tags ){
				
			m = menu_manager.addMenuItem( menu, tag.getTagName( false ));
					
			m.setStyle( MenuItem.STYLE_RADIO );
										
			m.setData( new Boolean( assigned_tag == tag ));
					
			m.addListener(
				new MenuItemListener() 
				{
					public void
					selected(
						MenuItem			menu,
						Object 				target )
					{
						subs.setTagID( tag.getTagUID());
					}
				});
		}
		
		m = menu_manager.addMenuItem( menu, "sep2" );
			
		m.setStyle( MenuItem.STYLE_SEPARATOR );

		m = menu_manager.addMenuItem( menu, "label.add.tag" );
		
		m.addListener(
			new MenuItemListener() 
			{
				public void
				selected(
					MenuItem			menu,
					Object 				target )
				{
					addTag();
				}
			});
	}

	private void 
	addTag()	
	{
		Tag new_tag = TagUIUtils.createManualTag();
		
		if ( new_tag != null ){
		
			subs.setTagID( new_tag.getTagUID());
		}
	}
	
	
	protected void export() {
		Utils.execSWTThread(
			new AERunnable() 
			{
				public void 
				runSupport()
				{
					FileDialog dialog = 
						new FileDialog( Utils.findAnyShell(), SWT.SYSTEM_MODAL | SWT.SAVE );
					
					dialog.setFilterPath( TorrentOpener.getFilterPathData() );
											
					dialog.setText(MessageText.getString("subscript.export.select.template.file"));
					
					dialog.setFilterExtensions(new String[] {
							"*.vuze",
							"*.vuz",
							Constants.FILE_WILDCARD
						});
					dialog.setFilterNames(new String[] {
							"*.vuze",
							"*.vuz",
							Constants.FILE_WILDCARD
						});
					
					String path = TorrentOpener.setFilterPathData( dialog.open());

					if ( path != null ){
						
						String lc = path.toLowerCase();
						
						if ( !lc.endsWith( ".vuze" ) && !lc.endsWith( ".vuz" )){
							
							path += ".vuze";
						}
						
						try{
							VuzeFile vf = subs.getVuzeFile();
							
							vf.write( new File( path ));
							

						}catch( Throwable e ){
							
							Debug.out( e );
						}
					}
				}
			});
	}

	protected void
	removeWithConfirm( )
	{
		MessageBoxShell mb = 
			new MessageBoxShell(
				MessageText.getString("message.confirm.delete.title"),
				MessageText.getString("message.confirm.delete.text",
						new String[] {
							subs.getName()
						}), 
				new String[] {
					MessageText.getString("Button.yes"),
					MessageText.getString("Button.no")
				},
				1 );
		
		mb.open(new UserPrompterResultListener() {
			public void prompterClosed(int result) {
				if (result == 0) {
					subs.setSubscribed( false );
				}
			}
		});
	}
	
	protected void
	showProperties()
	{
		SubscriptionHistory history = subs.getHistory();
		
		SimpleDateFormat df = new SimpleDateFormat();
		
		String last_error = history.getLastError();
		
		if ( last_error == null ){
			last_error = "";
		}
		
		String	engine_str;
		String	auth_str	= String.valueOf(false);
		
		try{
			Engine engine = subs.getEngine();
			
			engine_str = engine.getNameEx();
			
			if ( engine instanceof WebEngine ){
			
				WebEngine web_engine = (WebEngine)engine;
				
				if ( web_engine.isNeedsAuth()){
					
					auth_str = String.valueOf(true) + ": cookies=" + toString( web_engine.getRequiredCookies());
				}
			}
		}catch( Throwable e ){
			
			engine_str 	= "Unknown";
			auth_str	= "";
		}
		
		String[] keys = {
				"subs.prop.enabled",
				"subs.prop.is_public",
				"subs.prop.is_auto",
				"subs.prop.is_auto_ok",
				"subs.prop.update_period",
				"subs.prop.last_scan",
				"subs.prop.last_result",
				"subs.prop.next_scan",
				"subs.prop.last_error",
				"subs.prop.num_read",
				"subs.prop.num_unread",
				"subs.prop.assoc",
				"subs.prop.version",
				"subs.prop.high_version",
				"subscriptions.listwindow.popularity",
				"subs.prop.template",
				"subs.prop.auth",
				"TableColumn.header.category",
				"TableColumn.header.tag.name",
			};
		
		String	category_str;
		
		String category = subs.getCategory();
		
		if ( category == null ){
			
			category_str = MessageText.getString( "Categories.uncategorized" );
			
		}else{
			
			category_str = category;
		}
				
		Tag tag = TagManagerFactory.getTagManager().lookupTagByUID( subs.getTagID() );
		
		String tag_str = tag==null?"":tag.getTagName( true );
		
		int	 check_freq			= history.getCheckFrequencyMins();
		long last_new_result 	= history.getLastNewResultTime();
		long next_scan 			= history.getNextScanTime();
		
		String[] values = { 
				String.valueOf( history.isEnabled()),
				String.valueOf( subs.isPublic()),
				String.valueOf( history.isAutoDownload()),
				String.valueOf( subs.isAutoDownloadSupported()),
				(check_freq==Integer.MAX_VALUE?"":(String.valueOf( history.getCheckFrequencyMins() + " " + MessageText.getString( "ConfigView.text.minutes")))),
				df.format(new Date( history.getLastScanTime())),
				( last_new_result==0?"":df.format(new Date( last_new_result ))),
				( next_scan == Long.MAX_VALUE?"":df.format(new Date( next_scan ))),
				(last_error.length()==0?MessageText.getString("PeersView.uniquepiece.none"):last_error),
				String.valueOf( history.getNumRead()),
				String.valueOf( history.getNumUnread()),
				String.valueOf( subs.getAssociationCount()),
				String.valueOf( subs.getVersion()),
				subs.getHighestVersion() > subs.getVersion()?String.valueOf( subs.getHighestVersion()):null,
				subs.getCachedPopularity()<=1?null:String.valueOf( subs.getCachedPopularity()),
				engine_str,
				auth_str,
				category_str,
				tag_str,
			};
		
		new PropertiesWindow( subs.getName(), keys, values );
	}

	private String
	toString(
		String[]	strs )
	{
		String	res = "";
		
		for(int i=0;i<strs.length;i++){
			res += (i==0?"":",") + strs[i];
		}
		
		return( res );
	}
	

	public Object 
	getTitleInfoProperty(
		int propertyID ) 
	{
		// This should work, but since we have subs already in class, use that
		//if (mdiEntry == null) {
		//	return null;
		//}
		//Object ds = mdiEntry.getDatasource();
		//if (!(ds instanceof Subscription)) {
		//	return null;
		//}
		//Subscription subs = (Subscription) ds;

		switch( propertyID ){
		
			case ViewTitleInfo.TITLE_TEXT:{
				
				return( subs.getName());
			}
			case ViewTitleInfo.TITLE_INDICATOR_TEXT_TOOLTIP:{
			
				long	pop = subs.getCachedPopularity();
				
				String res = subs.getName();
				
				if ( pop > 1 ){
					
					res += " (" + MessageText.getString("subscriptions.listwindow.popularity").toLowerCase() + "=" + pop + ")";
				}
										
				return( res );
			}
			case ViewTitleInfo.TITLE_INDICATOR_TEXT :{
				
				SubscriptionMDIEntry mdi = (SubscriptionMDIEntry) subs.getUserData(SubscriptionManagerUI.SUB_ENTRYINFO_KEY);
				
				if ( mdi != null ){
					
					mdi.setWarning();
				}

				if( subs.getHistory().getNumUnread() > 0 ){
					
					return ( "" + subs.getHistory().getNumUnread());
				}
				
				return null;
			}
		}
		
		return( null );
	}

	public abstract static class SubsMenuItemListener implements MenuItemListener {
		public final void selected(MenuItem menu, Object target) {
			if (target instanceof MdiEntry) {
				MdiEntry info = (MdiEntry) target;
				Subscription subs = (Subscription) info.getDatasource();
				
				try {
					selected(info, subs);
				} catch (Throwable t) {
					Debug.out(t);
				}
			}
		}

		public abstract void selected(MdiEntry info, Subscription subs);
	}
}
