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
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.*;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemFillListener;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.menus.MenuManager;
import org.gudy.azureus2.ui.swt.PropertiesWindow;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.messenger.config.PlatformConfigMessenger;
import com.aelitis.azureus.core.metasearch.Engine;
import com.aelitis.azureus.core.metasearch.MetaSearchManagerFactory;
import com.aelitis.azureus.core.metasearch.impl.web.WebEngine;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.selectedcontent.ISelectedContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentV3;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.browser.OpenCloseSearchDetailsListener;
import com.aelitis.azureus.ui.swt.browser.listener.MetaSearchListener;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.util.Constants;
import com.aelitis.azureus.util.MapUtils;

/**
 * @author TuxPaper
 * @created Sep 30, 2006
 *
 */
public class SearchResultsTabArea
	extends SkinView
	implements ViewTitleInfo, OpenCloseSearchDetailsListener
{
	private SWTSkinObjectBrowser browserSkinObject;

	private SWTSkin skin;

	private String searchText;

	private boolean searchResultsInitialized = false;

	protected String title;

	private MenuItem menuItem;
	
	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.views.SkinView#showSupport(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	 */
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		skin = skinObject.getSkin();
		browserSkinObject = (SWTSkinObjectBrowser) skin.getSkinObject(
				SkinConstants.VIEWID_BROWSER_SEARCHRESULTS, skinObject);

		createBrowseArea(browserSkinObject);

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

		PluginManager pm = AzureusCoreFactory.getSingleton().getPluginManager();
		PluginInterface pi = pm.getDefaultPluginInterface();
		UIManager uim = pi.getUIManager();
		
		final MenuManager menuManager = uim.getMenuManager();

		if ( menuItem == null ){
		
			menuItem = menuManager.addMenuItem("sidebar.Search","Search.menu.engines");
		
			menuItem.setStyle( MenuItem.STYLE_MENU );
			
			menuItem.addFillListener(
				new MenuItemFillListener()
				{
					public void menuWillBeShown(MenuItem menu, Object data) {
				
						menuItem.removeAllChildItems();
						
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
							
							MenuItem engine_menu = menuManager.addMenuItem( menuItem, "!" + engine.getName() + "!" );
							
							engine_menu.setStyle( MenuItem.STYLE_MENU );

							if ( engine.getSource() != Engine.ENGINE_SOURCE_VUZE ){
								
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
							
							MenuItem mi = menuManager.addMenuItem( engine_menu, "Subscription.menu.properties" );

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
		}
		
		if (browserSkinObject != null) {
			Object o = skinObject.getData("CreationParams");

			anotherSearch((String) o);
		}

		return null;
	}

	/**
	 * 
	 */
	private void createBrowseArea(SWTSkinObjectBrowser browserSkinObject) {
		this.browserSkinObject = browserSkinObject;
		browserSkinObject.getContext().addMessageListener(
				new MetaSearchListener(this));
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

						browserSkinObject.addListener(new SWTSkinObjectListener() {
							public Object eventOccured(SWTSkinObject skinObject,
									final int eventType, Object params) {
								Utils.execSWTThread(new AERunnable() {
									public void runSupport() {
										if (eventType == SWTSkinObjectListener.EVENT_SHOW) {
											SelectedContentManager.changeCurrentlySelectedContent(
													"searchresults", getCurrentlySelectedContent());
										} else if (eventType == SWTSkinObjectListener.EVENT_HIDE) {
											SelectedContentManager.changeCurrentlySelectedContent(
													"searchresults", null);
										}
									}
								});
								return null;
							}
						});
						SelectedContentManager.changeCurrentlySelectedContent("searchresults",
								null);

						Browser browser = browserSkinObject.getBrowser();
						browser.addTitleListener(new TitleListener() {
							public void changed(TitleEvent event) {
								title = event.title;
								int i = title.toLowerCase().indexOf("details:");
								if (i > 0) {
									title = title.substring(i + 9);
								}
							}
						});

						browser.addLocationListener(new LocationListener() {
							public void changing(LocationEvent event) {
								//ViewTitleInfoManager.refreshTitleInfo(titleInfo);
							}

							public void changed(LocationEvent event) {
								SelectedContentManager.changeCurrentlySelectedContent("searchresults",
										getCurrentlySelectedContent());
								//ViewTitleInfoManager.refreshTitleInfo(titleInfo);
							}
						});
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
				Browser search = ((SWTSkinObjectBrowser) soSearchResults).getBrowser();
				String url = MapUtils.getMapString(params, "url",
						"http://google.com/search?q=" + Math.random());
				if (PlatformConfigMessenger.urlCanRPC(url)) {
					url = Constants.appendURLSuffix(url);
				}

				//Gudy, Not Tux, Listener Added
				String listenerAdded = (String) search.getData("g.nt.la");
				if (listenerAdded == null) {
					search.setData("g.nt.la", "");
					search.addProgressListener(new ProgressListener() {
						public void changed(ProgressEvent event) {
						}

						public void completed(ProgressEvent event) {
							Browser search = (Browser) event.widget;
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
				soSearchResults.setVisible(true);
				controlBottom.setVisible(true);
				search.setVisible(true);

				gd = (FormData) controlTop.getLayoutData();
				gd.bottom = null;
				gd.height = MapUtils.getMapInt(params, "top-height", 120);
				controlTop.setLayoutData(gd);

				controlTop.getParent().layout(true);
			}
		});
	}

	protected ISelectedContent[] getCurrentlySelectedContent() {
		SWTSkinObject soSearchResults = getSkinObject("searchresults-search-results");
		if (soSearchResults == null) {
			return null;
		}
		
		SWTSkinObjectBrowser browserSkinObject = (SWTSkinObjectBrowser) soSearchResults;
		Browser browser = browserSkinObject.getBrowser();
		if (browser == null) {
			return null;
		}
		String url = browser.getUrl();
		int i = url.indexOf(Constants.URL_DETAILS);
		if (i > 0) {
			i += Constants.URL_DETAILS.length();
			int end1 = url.indexOf("?", i);
			int end2 = url.indexOf(".", i);
			int end = end1 < 0 ? end2 : Math.min(end1, end2);

			String hash;
			if (end < 0 || end < i) {
				hash = url.substring(i);
			} else {
				hash = url.substring(i, end);
			}

			return new SelectedContentV3[] {
				new SelectedContentV3(hash, title, true, false)
			};
		}

		return null;
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
				Browser search = ((SWTSkinObjectBrowser) soSearchResults).getBrowser();

				FormData gd = (FormData) controlBottom.getLayoutData();
				if (gd == null) {
					return;
				}
				gd.top = null;
				gd.height = 0;
				controlBottom.setLayoutData(gd);
				soSearchResults.setVisible(false);

				gd = (FormData) controlTop.getLayoutData();
				gd.bottom = new FormAttachment(controlBottom, 0);
				gd.height = SWT.DEFAULT;
				controlTop.setLayoutData(gd);

				controlBottom.getParent().layout(true);
				search.setUrl("about:blank");
			}
		});
	}

	public void anotherSearch(String searchText) {
		this.searchText = searchText;
		String url = Constants.URL_PREFIX + Constants.URL_ADD_SEARCH
				+ UrlUtils.encode(searchText) + "&" + Constants.URL_SUFFIX + "&rand="
				+ SystemTime.getCurrentTime();

		if (System.getProperty("metasearch", "1").equals("1")) {
			url = Constants.URL_PREFIX + "xsearch?q=" + UrlUtils.encode(searchText)
					+ "&" + Constants.URL_SUFFIX + "&rand=" + SystemTime.getCurrentTime();
		}

		closeSearchResults(null);
		browserSkinObject.setURL(url);
		ViewTitleInfoManager.refreshTitleInfo(this);
	}

	// @see com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo#getTitleInfoProperty(int)
	public Object getTitleInfoProperty(int propertyID) {
		if (propertyID == TITLE_SKINVIEW) {
			return this;
		}
		if (propertyID == TITLE_TEXT) {
			return searchText;
		}
		return null;
	}
}
