/*
 * Created on Dec 7, 2016
 * Created by Paul Gardner
 * 
 * Copyright 2016 Azureus Software, Inc.  All rights reserved.
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


package com.aelitis.azureus.ui.swt.search;

import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.browser.TitleEvent;
import org.eclipse.swt.browser.TitleListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.ui.swt.BrowserWrapper;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.core.messenger.config.PlatformConfigMessenger;
import com.aelitis.azureus.core.proxy.AEProxyFactory;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.aelitis.azureus.ui.mdi.MdiEntry;
import com.aelitis.azureus.ui.mdi.MdiEntryVitalityImage;
import com.aelitis.azureus.ui.mdi.MultipleDocumentInterface;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.browser.BrowserContext;
import com.aelitis.azureus.ui.swt.browser.CookiesListener;
import com.aelitis.azureus.ui.swt.browser.OpenCloseSearchDetailsListener;
import com.aelitis.azureus.ui.swt.browser.BrowserContext.loadingListener;
import com.aelitis.azureus.ui.swt.browser.listener.ExternalLoginCookieListener;
import com.aelitis.azureus.ui.swt.browser.listener.MetaSearchListener;
import com.aelitis.azureus.ui.swt.mdi.MultipleDocumentInterfaceSWT;
import com.aelitis.azureus.ui.swt.search.SearchResultsTabArea.SearchQuery;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectBrowser;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectListener;
import com.aelitis.azureus.util.ConstantsVuze;
import com.aelitis.azureus.util.MapUtils;
import com.aelitis.azureus.util.UrlFilter;

public class 
SearchResultsTabAreaBrowser 
	implements SearchResultsTabAreaBase
{
	private static boolean							search_proxy_init_done;
	private static AEProxyFactory.PluginHTTPProxy	search_proxy;
	private static boolean							search_proxy_set;
	private static AESemaphore						search_proxy_sem = new AESemaphore( "sps" );
	
	private static List<SearchResultsTabAreaBrowser>	pending = new ArrayList<SearchResultsTabAreaBrowser>();
	
	private static void
	initProxy()
	{
		synchronized( SearchResultsTabArea.class ){
			
			if ( search_proxy_init_done ){
				
				return;
			}
			
			search_proxy_init_done = true;
		}
		
		new AEThread2( "ST_test" )
		{
			public void
			run()
			{	
				try{
			
					String test_url = PlatformConfigMessenger.getWebSearchUrl().replaceAll("%s", "derp");
					
					try{
						URL url = new URL( test_url );
						
						url = UrlUtils.setProtocol( url, "https" );
						
						url = UrlUtils.setPort( url, 443 );
						
						boolean use_proxy = !COConfigurationManager.getStringParameter( "browser.internal.proxy.id", "none" ).equals( "none" );
						
						if ( !use_proxy ){
						
							Boolean looks_ok = AEProxyFactory.testPluginHTTPProxy( url, true );
						
							use_proxy = looks_ok != null && !looks_ok;
						}
						
						if ( use_proxy ){
														
							search_proxy = AEProxyFactory.getPluginHTTPProxy( "search", url, true );
							
							if ( search_proxy != null ){
								
								UrlFilter.getInstance().addUrlWhitelist( "https?://" + ((InetSocketAddress)search_proxy.getProxy().address()).getAddress().getHostAddress() + ":?[0-9]*/.*" );
							}
						}
					}catch( Throwable e ){
					}
				}finally{
					
					List<SearchResultsTabAreaBrowser> to_redo = null;
					
					synchronized( SearchResultsTabArea.class ){
						
						search_proxy_set	= true;
														
						to_redo = new ArrayList<SearchResultsTabAreaBrowser>( pending );
						
						pending.clear();
					}
				
					search_proxy_sem.releaseForever();

					for ( SearchResultsTabAreaBrowser area: to_redo ){
							
						try{	
							try{
								area.browserSkinObject.setAutoReloadPending( false, search_proxy == null );
									
							}catch( Throwable e ){
							}
							
							if ( search_proxy != null ){
								
								SearchQuery sq = area.sq;
									
								if ( sq != null ){
									
									area.anotherSearch( sq );
								}
							}
						}catch( Throwable e ){	
						}
					}
				}
			}
		}.start();
	}
	
	static{
		COConfigurationManager.addParameterListener(
			"browser.internal.proxy.id",
			new ParameterListener()
			{	
				public void 
				parameterChanged(
					String parameterName ) 
				{
					synchronized( SearchResultsTabArea.class ){
						
						if ( !search_proxy_init_done ){
							
							return;
						}
						
						search_proxy_init_done = false;

						search_proxy_set	= false;
						
						if ( search_proxy != null ){
							
							search_proxy.destroy();
							
							search_proxy = null;
						}
					}
				}
			});
	}
	
	private static AEProxyFactory.PluginHTTPProxy
	getSearchProxy(
		SearchResultsTabAreaBrowser		area )
	{
		initProxy();
		
		boolean force_proxy = !COConfigurationManager.getStringParameter( "browser.internal.proxy.id", "none" ).equals( "none" );

		search_proxy_sem.reserve( force_proxy?60*1000:2500 );
		
		synchronized( SearchResultsTabArea.class ){
			
			if ( search_proxy_set ){
				
				return( search_proxy );
				
			}else{
				
				pending.add( area );
				
				try{
					area.browserSkinObject.setAutoReloadPending( true, false );
						
				}catch( Throwable e ){
				}
				
				return( null );
			}
		}
	}
	
	private final SearchResultsTabArea	parent;
	
	private SWTSkinObjectBrowser	browserSkinObject;
	
	private SearchQuery		sq;
	
	protected
	SearchResultsTabAreaBrowser(
		SearchResultsTabArea		_parent )
	{
		parent	= _parent;
	}
	
	protected void
	init(
		SWTSkinObjectBrowser		_browserSkinObject )
	{
		browserSkinObject = _browserSkinObject;
		
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
	}
	
	/**
	 * 
	 */
	private void createBrowseArea(SWTSkinObjectBrowser browserSkinObject) {
		this.browserSkinObject = browserSkinObject;
		

		browserSkinObject.addListener(new loadingListener() {
			public void browserLoadingChanged(boolean loading, String url) {
				parent.setBusy( loading );
			}
		});
	}


	public void 
	anotherSearch(
		SearchQuery sq ) 
	{
		this.sq	= sq;
		
		String url = PlatformConfigMessenger.getWebSearchUrl().replaceAll("%s", UrlUtils.encode(sq.term));

		AEProxyFactory.PluginHTTPProxy proxy = getSearchProxy( this );
			
		if ( proxy != null ){
			
			url = proxy.proxifyURL( url );
		}
		
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
  			if ( display != null ){
	  			long until = SystemTime.getCurrentTime() + 300;
	  			while (!done[0] && until > SystemTime.getCurrentTime()) {
	  				if (!display.readAndDispatch()) {
	  					display.sleep();
	  				}
	  			}
  			}
			} catch (Throwable t) {
				
			}
		}
		
		browserSkinObject.setURL(url);
	}

	// @see com.aelitis.azureus.ui.swt.search.SearchResultsTabAreaBase#getResultCount()
	public int getResultCount() {
		return 0;
	}

	// @see com.aelitis.azureus.ui.swt.search.SearchResultsTabAreaBase#showView()
	public void showView() {
	}

	// @see com.aelitis.azureus.ui.swt.search.SearchResultsTabAreaBase#refreshView()
	public void refreshView() {
	}

	// @see com.aelitis.azureus.ui.swt.search.SearchResultsTabAreaBase#hideView()
	public void hideView() {
	}
	
}
