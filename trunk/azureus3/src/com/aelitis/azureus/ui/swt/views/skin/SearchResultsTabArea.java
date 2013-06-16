/**
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.ui.swt.views.skin;

import java.io.File;
import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.*;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.PropertiesWindow;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.ClipboardCopy;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;

import com.aelitis.azureus.core.*;
import com.aelitis.azureus.core.metasearch.Engine;
import com.aelitis.azureus.core.metasearch.MetaSearchManagerFactory;
import com.aelitis.azureus.core.metasearch.impl.web.WebEngine;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.mdi.*;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.browser.*;
import com.aelitis.azureus.ui.swt.browser.BrowserContext.loadingListener;
import com.aelitis.azureus.ui.swt.browser.listener.ExternalLoginCookieListener;
import com.aelitis.azureus.ui.swt.browser.listener.MetaSearchListener;
import com.aelitis.azureus.ui.swt.mdi.MultipleDocumentInterfaceSWT;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.util.*;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.menus.*;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;

/**
 * @author TuxPaper
 * @created Sep 30, 2006
 *
 */
public class SearchResultsTabArea
	extends SkinView
	implements OpenCloseSearchDetailsListener
{
	private SWTSkinObjectBrowser browserSkinObject;

	private SWTSkin skin;

	private boolean searchResultsInitialized = false;

	protected String title;
	
	private MdiEntryVitalityImage vitalityImage;

	private boolean menu_added;
	
	public SearchQuery sq;

	public static class SearchQuery {
		public SearchQuery(String term, boolean toSubscribe) {
			this.term = term;
			this.toSubscribe = toSubscribe;
		}

		public String term;
		public boolean toSubscribe;
	}
	
	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.views.SkinView#showSupport(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	 */
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		skin = skinObject.getSkin();
		browserSkinObject = (SWTSkinObjectBrowser) skin.getSkinObject(
				SkinConstants.VIEWID_BROWSER_SEARCHRESULTS, skinObject);

		browserSkinObject.addListener(new SWTSkinObjectListener() {
			
			public Object eventOccured(SWTSkinObject skinObject, int eventType,
					Object params) {
				if (eventType == EVENT_SHOW) {
					browserSkinObject.removeListener(this);

					createBrowseArea(browserSkinObject);
				}
				return null;
			}
		});

		/**
				final SWTSkinTabSet tabSetMain = skin.getTabSet(SkinConstants.TABSET_MAIN);
				if (tabSetMain != null) {
					final SWTSkinObjectTab tab = tabSetMain.getTab(SkinConstants.VIEWID_SEARCHRESULTS_TAB);
					if (tab != null) {
						SWTSkinObjectListener l = new SWTSkinObjectListener() {
							public Object eventOccured(SWTSkinObject skinObject, int eventType,
									Object params) {
								if (eventType == SWTSkinObjectListener.EVENT_SELECT) {
									tab.setVisible(tabSetMain.getActiveTab() == tab);
								}
								return null;
							}
						};
						tab.addListener(l);
					}
				}
		**/

		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(final AzureusCore core) {
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						initCoreStuff(core);
					}

				});
			}
		});
		
		if (sq != null) {
			anotherSearch(sq);
		}
		
		closeSearchResults(null);

		return null;
	}

	private void initCoreStuff(AzureusCore core) {
		PluginInterface pi = PluginInitializer.getDefaultInterface();
		UIManager uim = pi.getUIManager();
		
		final MenuManager menuManager = uim.getMenuManager();

		if ( !menu_added ){
			
			menu_added = true;
			
			final MenuItem template_menu = menuManager.addMenuItem("sidebar.Search","Search.menu.engines");
		
			template_menu.setStyle( MenuItem.STYLE_MENU );
			
			template_menu.addFillListener(
				new MenuItemFillListener()
				{
					public void menuWillBeShown(MenuItem menu, Object data) {
				
						template_menu.removeAllChildItems();
						
						Engine[] engines = MetaSearchManagerFactory.getSingleton().getMetaSearch().getEngines( true, false );
						
						Arrays.sort( 
							engines,
							new Comparator()
							{
								public int 
								compare(
									Object o1, 
									Object o2)
								{
									return(((Engine)o1).getName().compareToIgnoreCase(((Engine)o2).getName()));
								}
							});
							
						for (int i=0;i<engines.length;i++){
							
							final Engine engine = engines[i];
							
							MenuItem engine_menu = menuManager.addMenuItem( template_menu, "!" + engine.getName() + "!" );
							
							engine_menu.setStyle( MenuItem.STYLE_MENU );

							if ( true || engine.getSource() != Engine.ENGINE_SOURCE_VUZE ){
								
								MenuItem mi = menuManager.addMenuItem( engine_menu, "MyTorrentsView.menu.exportmenu" );

								mi.addListener(
									new MenuItemListener()
									{
										public void 
										selected(
											MenuItem menu, 
											Object target) 
										{
											final Shell shell = Utils.findAnyShell();
											
											shell.getDisplay().asyncExec(
												new AERunnable() 
												{
													public void 
													runSupport()
													{
														FileDialog dialog = 
															new FileDialog( shell, SWT.SYSTEM_MODAL | SWT.SAVE );
														
														dialog.setFilterPath( TorrentOpener.getFilterPathData() );
																				
														dialog.setText(MessageText.getString("metasearch.export.select.template.file"));
														
														dialog.setFilterExtensions(new String[] {
																"*.vuze",
																"*.vuz",
																org.gudy.azureus2.core3.util.Constants.FILE_WILDCARD
															});
														dialog.setFilterNames(new String[] {
																"*.vuze",
																"*.vuz",
																org.gudy.azureus2.core3.util.Constants.FILE_WILDCARD
															});
														
														String path = TorrentOpener.setFilterPathData( dialog.open());
									
														if ( path != null ){
															
															String lc = path.toLowerCase();
															
															if ( !lc.endsWith( ".vuze" ) && !lc.endsWith( ".vuz" )){
																
																path += ".vuze";
															}
															
															try{
																engine.exportToVuzeFile( new File( path ));
																
															}catch( Throwable e ){
																
																Debug.out( e );
															}
														}
													}
												});						
										}
									});
							}
							
							if ( Constants.IS_CVS_VERSION ){
								
								MenuItem copy_mi = menuManager.addMenuItem( engine_menu, "ConfigView.copy.to.clipboard.tooltip" );
	
								copy_mi.addListener(
									new MenuItemListener()
									{
										public void 
										selected(
											MenuItem menu, 
											Object target) 
										{
											final Shell shell = Utils.findAnyShell();
											
											shell.getDisplay().asyncExec(
												new AERunnable() 
												{
													public void 
													runSupport()
													{
														try{
															ClipboardCopy.copyToClipBoard( engine.exportToVuzeFile().exportToJSON());
															
														}catch( Throwable e ){
															
															Debug.out( e );
														}
													}
												});
										}
									});
							}
							
							if ( engine instanceof WebEngine ){
								
								final WebEngine we = (WebEngine)engine;
								
								if ( we.isNeedsAuth()){
									
									String cookies = we.getCookies();
									
									if ( cookies != null && cookies.length() > 0 ){
										
										MenuItem mi = menuManager.addMenuItem( engine_menu, "Subscription.menu.resetauth" );

										mi.addListener(
											new MenuItemListener()
											{
												public void 
												selected(
													MenuItem menu, 
													Object target) 
												{
													we.setCookies( null );
												}
											});
									}
								}
							}
							
							if ( engine_menu.getItems().length > 0 ){
								
								MenuItem mi = menuManager.addMenuItem( engine_menu, "Subscription.menu.sep" );

								mi.setStyle( MenuItem.STYLE_SEPARATOR );
							}		
															
							MenuItem mi = menuManager.addMenuItem( engine_menu, "Button.remove" );
	
							mi.addListener(
								new MenuItemListener()
								{
									public void 
									selected(
										MenuItem menu, 
										Object target) 
									{
										engine.setSelectionState( Engine.SEL_STATE_DESELECTED );
									}
								});
							
							mi = menuManager.addMenuItem( engine_menu, "Subscription.menu.sep2" );
	
							mi.setStyle( MenuItem.STYLE_SEPARATOR );
							
							mi = menuManager.addMenuItem( engine_menu, "Subscription.menu.properties" );

							mi.addListener(
								new MenuItemListener()
								{
									public void 
									selected(
										MenuItem menu, 
										Object target) 
									{
										String	engine_str;
										String	auth_str	= String.valueOf(false);
										
										engine_str = engine.getNameEx();
										
										if ( engine instanceof WebEngine ){
										
											WebEngine web_engine = (WebEngine)engine;
											
											if ( web_engine.isNeedsAuth()){
												
												auth_str = String.valueOf(true) + ": cookies=" + toString( web_engine.getRequiredCookies());
											}
										}
										
										String[] keys = {
												"subs.prop.template",
												"subs.prop.auth",
											};
										
										String[] values = { 
												engine_str,
												auth_str,
											};
										
										new PropertiesWindow( engine.getName(), keys, values );
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
								});
							
							if ( engine_menu.getItems().length == 0 ){
								
								engine_menu.setEnabled( false );
							}
						}
					}
				});
			
			final MenuItem export_menu = menuManager.addMenuItem("sidebar.Search","search.export.all");
			
			
			export_menu.setStyle( MenuItem.STYLE_PUSH );
			
			export_menu.addListener(
				new MenuItemListener()
				{
					public void 
					selected(
						MenuItem menu, 
						Object target) 
					{
						final Shell shell = Utils.findAnyShell();
						
						shell.getDisplay().asyncExec(
							new AERunnable() 
							{
								public void 
								runSupport()
								{
									FileDialog dialog = 
										new FileDialog( shell, SWT.SYSTEM_MODAL | SWT.SAVE );
									
									dialog.setFilterPath( TorrentOpener.getFilterPathData() );
															
									dialog.setText(MessageText.getString("metasearch.export.select.template.file"));
									
									dialog.setFilterExtensions(new String[] {
											"*.vuze",
											"*.vuz",
											org.gudy.azureus2.core3.util.Constants.FILE_WILDCARD
										});
									dialog.setFilterNames(new String[] {
											"*.vuze",
											"*.vuz",
											org.gudy.azureus2.core3.util.Constants.FILE_WILDCARD
										});
									
									String path = TorrentOpener.setFilterPathData( dialog.open());
				
									if ( path != null ){
										
										String lc = path.toLowerCase();
										
										if ( !lc.endsWith( ".vuze" ) && !lc.endsWith( ".vuz" )){
											
											path += ".vuze";
										}
										
										try{
											MetaSearchManagerFactory.getSingleton().getMetaSearch().exportEngines(  new File( path ));
											
										}catch( Throwable e ){
											
											Debug.out( e );
										}
									}
								}
							});							}
				});
		}
	}

	/**
	 * 
	 */
	private void createBrowseArea(SWTSkinObjectBrowser browserSkinObject) {
		this.browserSkinObject = browserSkinObject;
		browserSkinObject.getContext().addMessageListener(
				new MetaSearchListener(this));
		
		MultipleDocumentInterfaceSWT mdi = UIFunctionsManagerSWT.getUIFunctionsSWT().getMDISWT();
		if (mdi != null) {
			final MdiEntry entry = mdi.getEntryBySkinView(this);
			if (entry != null) {
				vitalityImage = entry.addVitalityImage("image.sidebar.vitality.dots");
				if ( vitalityImage != null ){
					vitalityImage.setVisible(false);
				}
			}
		}
		browserSkinObject.addListener(new loadingListener() {
			public void browserLoadingChanged(boolean loading, String url) {
				if (vitalityImage != null) {
					vitalityImage.setVisible(loading);
				}
			}
		});

		SWTSkinObject soSearchResults = getSkinObject("searchresults-search-results");
		if (soSearchResults instanceof SWTSkinObjectBrowser) {
			SWTSkinObjectBrowser browserSearchResults = (SWTSkinObjectBrowser) soSearchResults;
			browserSearchResults.addListener(new loadingListener() {
				public void browserLoadingChanged(boolean loading, String url) {
					if (vitalityImage != null) {
						vitalityImage.setVisible(loading);
					}
				}
			});
		}
	}

	public void restart() {
		if (browserSkinObject != null) {
			browserSkinObject.restart();
		}
	}

	public void openSearchResults(final Map params) {
		if (!searchResultsInitialized) {
			searchResultsInitialized = true;
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					SWTSkinObject soSearchResults = getSkinObject("searchresults-search-results");
					if (soSearchResults != null) {
						SWTSkinObjectBrowser browserSkinObject = (SWTSkinObjectBrowser) soSearchResults;

						final BrowserWrapper browser = browserSkinObject.getBrowser();
						
						browser.addTitleListener(new TitleListener() {
							public void changed(TitleEvent event) {
								if (event.widget.isDisposed()
										|| browser.getShell().isDisposed()) {
									return;
								}
								title = event.title;
								int i = title.toLowerCase().indexOf("details:");
								if (i > 0) {
									title = title.substring(i + 9);
								}
							}
						});

						final ExternalLoginCookieListener cookieListener = new ExternalLoginCookieListener(new CookiesListener() {
							public void cookiesFound(String cookies) {
								browser.setData("current-cookies", cookies);
							}
						},browser);
						
						cookieListener.hook();
					}
				}
			});

		}
		Utils.execSWTThread(new AERunnable() {

			public void runSupport() {
				SWTSkinObject soSearchResults = getSkinObject("searchresults-search-results");
				if (soSearchResults == null) {
					return;
				}

				Control controlTop = browserSkinObject.getControl();
				Control controlBottom = soSearchResults.getControl();
				final BrowserWrapper search = ((SWTSkinObjectBrowser) soSearchResults).getBrowser();
				String url = MapUtils.getMapString(params, "url",
						"http://google.com/search?q=" + Math.random());
				if (UrlFilter.getInstance().urlCanRPC(url)) {
					url = ConstantsVuze.getDefaultContentNetwork().appendURLSuffix(url, false, true);
				}

				//Gudy, Not Tux, Listener Added
				String listenerAdded = (String) search.getData("g.nt.la");
				if (listenerAdded == null) {
					search.setData("g.nt.la", "");
					search.addProgressListener(new ProgressListener() {
						public void changed(ProgressEvent event) {
						}

						public void completed(ProgressEvent event) {
							
							String execAfterLoad = (String) search.getData("execAfterLoad");
							//Erase it, so that it's only used once after the page loads
							search.setData("execAfterLoad", null);
							if (execAfterLoad != null && !execAfterLoad.equals("")) {
								//String execAfterLoadDisplay = execAfterLoad.replaceAll("'","\\\\'");
								//search.execute("alert('injecting script : " + execAfterLoadDisplay + "');");
								boolean result = search.execute(execAfterLoad);
								//System.out.println("Injection : " + execAfterLoad + " (" + result + ")");
							}

						}
					});
				}

				//Store the "css" match string in the search cdp browser object
				String execAfterLoad = MapUtils.getMapString(params, "execAfterLoad",
						null);
				search.setData("execAfterLoad", execAfterLoad);

				search.setUrl(url);

				FormData gd = (FormData) controlBottom.getLayoutData();
				gd.top = new FormAttachment(controlTop, 0);
				gd.height = SWT.DEFAULT;
				controlBottom.setLayoutData(gd);

				gd = (FormData) controlTop.getLayoutData();
				gd.bottom = null;
				gd.height = MapUtils.getMapInt(params, "top-height", 120);
				controlTop.setLayoutData(gd);

				soSearchResults.setVisible(true);
				controlBottom.setVisible(true);
				search.setVisible(true);

				controlTop.getParent().layout(true);
			}
		});
	}


	public void closeSearchResults(final Map params) {
		Utils.execSWTThread(new AERunnable() {

			public void runSupport() {
				SWTSkinObject soSearchResults = skin.getSkinObject("searchresults-search-results");
				if (soSearchResults == null) {
					return;
				}

				Control controlTop = browserSkinObject.getControl();
				Control controlBottom = soSearchResults.getControl();
				BrowserWrapper search = ((SWTSkinObjectBrowser) soSearchResults).getBrowser();

				soSearchResults.setVisible(false);

				FormData gd = (FormData) controlBottom.getLayoutData();
				if (gd == null) {
					return;
				}
				gd.top = null;
				gd.height = 0;
				controlBottom.setLayoutData(gd);

				gd = (FormData) controlTop.getLayoutData();
				gd.bottom = new FormAttachment(controlBottom, 0);
				gd.height = SWT.DEFAULT;
				controlTop.setLayoutData(gd);

				controlBottom.getParent().layout(true);
				if (search != null) {
					search.setUrl("about:blank");
				}
				
				BrowserContext context = browserSkinObject.getContext();
				if (context != null) {
					context.executeInBrowser("searchResultsClosed()");
				}
			}
		});
	}
	
	public void resizeMainBrowser() {
		// TODO Auto-generated method stub
		
	}
	
	public void resizeSecondaryBrowser() {
		// TODO Auto-generated method stub
		
	}

	public Object dataSourceChanged(SWTSkinObject skinObject, Object params) {
		if (params instanceof SearchQuery) {
			sq = (SearchQuery) params;
			if (browserSkinObject != null) {
				anotherSearch(sq.term, sq.toSubscribe);
			}
		}

		return null;
	}

	public void anotherSearch(String searchText,boolean toSubscribe) {
		anotherSearch(new SearchQuery(searchText, toSubscribe));
	}
	
	public void anotherSearch(SearchQuery sq) {
		this.sq = sq;
		String url = 
			ConstantsVuze.getDefaultContentNetwork().getSearchService( sq.term );

		if (System.getProperty("metasearch", "1").equals("1")) {
			
			url = ConstantsVuze.getDefaultContentNetwork().getXSearchService( sq.term, sq.toSubscribe );
		}

		closeSearchResults(null);
		if (Utils.isThisThreadSWT()) {
			try {
  			browserSkinObject.getBrowser().setText("");
  			final BrowserWrapper browser = browserSkinObject.getBrowser();
  			final boolean[] done = {false};
  			browser.addLocationListener(new LocationListener() {
  				public void changing(LocationEvent event) {
  				}
  				
  				public void changed(LocationEvent event) {
  					done[0] = true;
  					browser.removeLocationListener(this);
  				}
  			});
  			browserSkinObject.getBrowser().setUrl("about:blank");
  			browserSkinObject.getBrowser().refresh();
  			browserSkinObject.getBrowser().update();
  			Display display = Utils.getDisplay();
  			long until = SystemTime.getCurrentTime() + 300;
  			while (!done[0] && until > SystemTime.getCurrentTime()) {
  				if (!display.readAndDispatch()) {
  					display.sleep();
  				}
  			}
			} catch (Throwable t) {
				
			}
		}
		browserSkinObject.setURL(url);

		MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
		String id = MultipleDocumentInterface.SIDEBAR_SECTION_SEARCH;
		MdiEntry entry = mdi.getEntry(id);
		if (entry != null) {
			ViewTitleInfoManager.refreshTitleInfo(entry.getViewTitleInfo());
		}
	}
}
