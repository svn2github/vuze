/**
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
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
 */

package com.aelitis.azureus.ui.swt.skin;

import java.net.InetSocketAddress;
import java.net.URL;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.cnetwork.ContentNetworkManagerFactory;
import com.aelitis.azureus.core.proxy.AEProxyFactory;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.swt.browser.BrowserContext;
import org.gudy.azureus2.ui.swt.BrowserWrapper;
import com.aelitis.azureus.ui.swt.browser.BrowserContext.loadingListener;
import com.aelitis.azureus.ui.swt.browser.listener.*;
import com.aelitis.azureus.util.UrlFilter;

/**
 * @author TuxPaper
 * @created Oct 9, 2006
 *
 */
public class SWTSkinObjectBrowser
	extends SWTSkinObjectBasic
{
	private boolean							generic_proxy_init_done;
	private AEProxyFactory.PluginHTTPProxy	generic_proxy;
	private boolean							generic_proxy_set;
	private AESemaphore						generic_proxy_sem = new AESemaphore( "sps" );
	
	private void
	initProxy(
		final String		target_url,
		final String		proxy_reason )
	{
			// can't make this a static initializer as class is loaded whenever we have a subscription
			// in the sidebar, regardless of focus
		
		synchronized( SWTSkinObjectBrowser.class ){
			
			if ( generic_proxy_init_done ){
				
				return;
			}
			
			generic_proxy_init_done = true;
		}
		
		new AEThread2( "GB_test" )
		{
			public void
			run()
			{	
				try{
					try{
						URL url = new URL( target_url );
						
						url = UrlUtils.setProtocol( url, "https" );
						
						url = UrlUtils.setPort( url, 443 );

						boolean use_proxy = !COConfigurationManager.getStringParameter( "browser.internal.proxy.id", "none" ).equals( "none" );
						
						if ( !use_proxy ){
						
							Boolean looks_ok = AEProxyFactory.testPluginHTTPProxy( url, true );
						
							use_proxy = looks_ok != null && !looks_ok;
						}
						
						if ( use_proxy ){
							
							generic_proxy = AEProxyFactory.getPluginHTTPProxy( proxy_reason, url, true );
							
							if ( generic_proxy != null ){
								
								UrlFilter.getInstance().addUrlWhitelist( "https?://" + ((InetSocketAddress)generic_proxy.getProxy().address()).getAddress().getHostAddress() + ":?[0-9]*/.*" );
							}
						}
					}catch( Throwable e ){
					}
				}finally{
										
					synchronized( SWTSkinObjectBrowser.class ){
						
						generic_proxy_set	= true;
						
						generic_proxy_sem.releaseForever();

						if ( isDisposed()){
							
							if ( generic_proxy != null ){
								
								generic_proxy.destroy();
								
								generic_proxy	= null;
							}
							
							return;
						}
					}
					
											
					setAutoReloadPending( false, generic_proxy == null );
														
					if ( generic_proxy != null ){
							
						updateBrowserProxy( generic_proxy );			
					}
				}
			}
		}.start();
	}
	
	{
		COConfigurationManager.addParameterListener(
			"browser.internal.proxy.id",
			new ParameterListener()
			{	
				public void 
				parameterChanged(
					String parameterName ) 
				{
					synchronized( SWTSkinObjectBrowser.class ){
						
						if ( !generic_proxy_init_done ){
							
							return;
						}
						
						generic_proxy_init_done = false;

						generic_proxy_set	= false;
						
						if ( generic_proxy != null ){
							
							generic_proxy.destroy();
							
							generic_proxy = null;
						}
					}
				}
			});
	}
	
	private AEProxyFactory.PluginHTTPProxy
	getGenericProxy(
		String						target_url,
		String						reason )
	{
		initProxy( target_url, reason );
		
		boolean force_proxy = !COConfigurationManager.getStringParameter( "browser.internal.proxy.id", "none" ).equals( "none" );

		generic_proxy_sem.reserve( force_proxy?60*1000:2500 );
		
		synchronized( SWTSkinObjectBrowser.class ){
			
			if ( generic_proxy_set ){
				
				return( generic_proxy );
				
			}else{
								
				setAutoReloadPending( true, false );
				
				return( null );
			}
		}
	}
	
	private BrowserWrapper browser;

	private Composite cParent;

	private Composite cArea;

	private String sStartURL;

	private BrowserContext context;

	private String urlToUse;

	private boolean forceVisibleAfterLoad;
	
	private boolean	use_generic_proxy	= false;
	private String	proxy_reason		= null;
	
	private boolean	autoReloadPending = false;
	
	private static boolean doneTheUglySWTFocusHack = false;
	
	

	
	/**
	 * @param skin
	 * @param properties
	 * @param sID
	 * @param sConfigID
	 * @param type
	 * @param parent
	 */
	public SWTSkinObjectBrowser(SWTSkin skin, SWTSkinProperties properties,
			String sID, String sConfigID, SWTSkinObject parent) {
		super(skin, properties, sID, sConfigID, "browser", parent);

		cParent = parent == null ? skin.getShell()
				: (Composite) parent.getControl();

		cArea = cParent;
		cArea = new Canvas(cParent, SWT.NO_BACKGROUND);
		cArea.setLayout(new FormLayout());

		setControl(cArea);

		if (cParent.isVisible()) {
			init();
		} else {
			addListener(new SWTSkinObjectListener() {
				public Object eventOccured(SWTSkinObject skinObject, int eventType,
						Object params) {
					if (eventType == SWTSkinObjectListener.EVENT_SHOW) {
						removeListener(this);
						init();
					}
					return null;
				}
			});
		}
	}

	public void init() {
		if (browser != null && !browser.isDisposed()) {
			return;
		}

		try {
			browser = BrowserWrapper.createBrowser(cArea, Utils.getInitialBrowserStyle(SWT.NONE));

			browser.setLayoutData(Utils.getFilledFormData());
			browser.getParent().layout(true);
		} catch (SWTError e) {
			System.err.println("Browser: " + e.toString());
			return;
		}

		Control widgetIndicator = null;
		String sIndicatorWidgetID = properties.getStringValue(sConfigID
				+ ".indicator");
		if (sIndicatorWidgetID != null) {
			SWTSkinObject skinObjectIndicator = skin.getSkinObjectByID(sIndicatorWidgetID);
			if (skinObjectIndicator != null) {
				widgetIndicator = skinObjectIndicator.getControl();
			}
		}

		String browserID = properties.getStringValue(sConfigID + ".view");
		if (browserID == null) {
			browserID = sID;
		}

		forceVisibleAfterLoad = properties.getBooleanValue(sConfigID + ".forceVisibleAfterLoad", true);
		context = new BrowserContext(browserID, browser, widgetIndicator, forceVisibleAfterLoad);

		if ( autoReloadPending ){
			context.setAutoReloadPending( autoReloadPending, false );
		}
		boolean noListeners = properties.getBooleanValue(sConfigID + ".browser.nolisteners", false);
		
		if (!noListeners) {
  		context.addMessageListener(new TorrentListener());
  		context.addMessageListener(new VuzeListener());
  		context.addMessageListener(new DisplayListener(browser));
  		context.addMessageListener(new ConfigListener(browser));
		}

		boolean popouts = properties.getBooleanValue(sConfigID + ".browser.allowPopouts", true);
		context.setAllowPopups(popouts);
		
		context.addListener(new loadingListener() {
			public void browserLoadingChanged(boolean loading, String url) {
				if (loading && browser.isVisible()) {
					// hack so search results page doesn't clear cur selected
					if (UrlFilter.getInstance().urlCanRPC(url)) {
						SelectedContentManager.clearCurrentlySelectedContent();
					}
				}
				
			}
		});

		String url = urlToUse != null ? urlToUse : sStartURL != null ? sStartURL
				: properties.getStringValue(sConfigID + ".url", (String) null);
		if (url != null) {
			setURL(url);
		}
	}

	public BrowserWrapper getBrowser() {
		if (browser == null) {
			init();
		}
		return browser;
	}

	public void setURL(final String url) {
		urlToUse = url;
		if (browser == null) {
			return;
		}
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (browser.isDisposed()) {
					return;
				}
				if (url == null) {
					browser.setText("");
				} else {
					String urlToUse = url;
					if (UrlFilter.getInstance().urlCanRPC(url)){
						ContentNetwork contentNetwork = ContentNetworkManagerFactory.getSingleton().getContentNetwork(
								context.getContentNetworkID());
						if (contentNetwork != null) {
							urlToUse = contentNetwork.appendURLSuffix(urlToUse,
									false, true);
						}
					}
					if (browser != null) {
						setBrowserURL(urlToUse);
						if(browser.isVisible()) {
							browser.setFocus();
						}
					}
				}
				if (sStartURL == null) {
					sStartURL = url;
					if (browser != null) {
						browser.setData("StartURL", url);
					}
				}
				//System.out.println(SystemTime.getCurrentTime() + "] Set URL: " + url);
			}
		});
	}

	public void restart() {
		if (null != sStartURL) {
			String startURLUnique;
			String sRand = "rand=" + SystemTime.getCurrentTime();
			if (sStartURL.indexOf("rand=") > 0) {
				startURLUnique = sStartURL.replaceAll("rand=[0-9.]+", sRand);
			} else if (sStartURL.indexOf('?') > 0) {
				startURLUnique = sStartURL + "&" + sRand;
			} else {
				startURLUnique = sStartURL + "?" + sRand;
			}
			System.out.println(startURLUnique);
			setURL(startURLUnique);
		}
	}

	/**
	 * 
	 */
	public void layout() {
		cParent.layout();
	}
	
	public BrowserContext getContext() {
		return context;
	}

	public String getStartURL() {
		return sStartURL;
	}

	public void setStartURL(String url) {
		sStartURL = url;
		if (null != browser) {
			if (urlToUse == null) {
				setBrowserURL( url );
			}
			browser.setData("StartURL", url);
		}
	}

	private void
	setBrowserURL(
		String		url )
	{
		if ( use_generic_proxy ){
			
			browser.setData("CurrentURL", url);

			AEProxyFactory.PluginHTTPProxy proxy = getGenericProxy( url, proxy_reason );
				
			if ( proxy != null ){
				
				url = proxy.proxifyURL( url );
										
				browser.setData("StartURL", url);
			}
		}
		
		browser.setUrl( url );
	}
	
	private void
	updateBrowserProxy(
		final AEProxyFactory.PluginHTTPProxy	proxy )
	{			
		Utils.execSWTThread(
			new Runnable()
			{
				public void
				run()
				{
					if ( browser != null && !browser.isDisposed()){
					
						String url = (String)browser.getData( "CurrentURL" );
							
						if ( url != null ){
						
							url = proxy.proxifyURL( url );;
						
							browser.setData("StartURL", url);
							
							browser.setUrl( url );
						}
					}
				}
			});
	}
	
	public void
	enablePluginProxy(
		String		reason )
	{
		use_generic_proxy 	= true;
		proxy_reason		= reason;
	}
	
	public void
	setAutoReloadPending(
		boolean	is_pending,
		boolean	aborted )
	{
		autoReloadPending = is_pending;
		BrowserContext bc = context;
		if ( bc != null ){
			bc.setAutoReloadPending( is_pending, aborted );
		}
	}
	
	public boolean isPageLoading() {
		return context == null ? false : context.isPageLoading();
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectBasic#setVisible(boolean)
	public boolean setIsVisible(final boolean visible, boolean walkup) {
		boolean changed = super.setIsVisible(visible, walkup);

		if (changed) {
  		// notify browser after we've fully processed visibility 
  		Utils.execSWTThreadLater(0, new AERunnable() {
  			public void runSupport() {
  				if (!isDisposed() && context != null) {
  					context.sendBrowserMessage("browser", visible ? "shown" : "hidden");
  				}
  			}
  		});
		}
		return changed;
	}

	public void addListener(loadingListener l) {
		if (context != null) {
			context.addListener(l);
		}
	}

	public void refresh() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (browser != null && !browser.isDisposed()) {
					browser.refresh();
				}
			}
		});
		
	}

	public void dispose() {
		if ( generic_proxy != null ){
			generic_proxy.destroy();
			generic_proxy = null;
		}
		super.dispose();
	}
}
