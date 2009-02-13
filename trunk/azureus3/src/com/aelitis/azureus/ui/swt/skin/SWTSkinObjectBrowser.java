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

package com.aelitis.azureus.ui.swt.skin;

import java.io.File;
import java.net.URL;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.cnetwork.ContentNetworkManagerFactory;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.swt.browser.BrowserContext;
import com.aelitis.azureus.ui.swt.browser.BrowserContext.loadingListener;
import com.aelitis.azureus.ui.swt.browser.listener.*;
import com.aelitis.azureus.ui.swt.browser.listener.publish.LocalHoster;
import com.aelitis.azureus.ui.swt.browser.listener.publish.PublishListener;
import com.aelitis.azureus.ui.swt.utils.PublishUtils;
import com.aelitis.azureus.util.LocalResourceHTTPServer;
import com.aelitis.azureus.util.UrlFilter;

import org.gudy.azureus2.plugins.PluginInterface;

/**
 * @author TuxPaper
 * @created Oct 9, 2006
 *
 */
public class SWTSkinObjectBrowser
	extends SWTSkinObjectBasic
	implements LocalHoster
{

	private Browser browser;

	private Composite cParent;

	private Composite cArea;

	private String sStartURL;

	private LocalResourceHTTPServer local_publisher;

	private BrowserContext context;

	private String urlToUse;

	private boolean forceVisibleAfterLoad;
	
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
		AzureusCore core = AzureusCoreFactory.getSingleton();

		try {
			browser = new Browser(cArea, Utils.getInitialBrowserStyle(SWT.NONE));

			browser.setLayoutData(Utils.getFilledFormData());
		} catch (SWTError e) {
			System.err.println("Browser: " + e.toString());
			return;
		}

		//TODO [SWT] : Remove this stupid code as soon as we update SWT
		if(Constants.isOSX && ! doneTheUglySWTFocusHack) {
			doneTheUglySWTFocusHack = true;
			Shell shell = new Shell(browser.getDisplay(),SWT.NONE);
			shell.setSize(1,1);
			shell.setLocation(-2, -2);
			shell.open();
			shell.close();
			browser.setFocus();
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

		context.addMessageListener(new TorrentListener(core));
		context.addMessageListener(new VuzeListener());
		context.addMessageListener(new DisplayListener(browser));
		context.addMessageListener(new ConfigListener(browser));
		context.addMessageListener(new PublishListener(skin.getShell(), this));
		context.addMessageListener(new LightBoxBrowserRequestListener());
		context.addMessageListener(new StatusListener());
		context.addMessageListener(new BrowserRpcBuddyListener());

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

		PublishUtils.setupContext(context);

		String url = urlToUse != null ? urlToUse : sStartURL != null ? sStartURL
				: properties.getStringValue(sConfigID + ".url", (String) null);
		if (url != null) {
			setURL(url);
		}
	}

	public Browser getBrowser() {
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
						browser.setUrl(urlToUse);
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
	
	public void dispose() {
		if (browser != null && !browser.isDisposed()) {
			browser.setVisible(false);
			browser.setUrl("about:blank");
		}
		super.dispose();
	}

	// @see com.aelitis.azureus.ui.swt.browser.listener.publish.LocalHoster#hostFile(java.io.File)
	public URL hostFile(File f) {
		if (local_publisher == null) {
			try {
				PluginInterface pi = AzureusCoreFactory.getSingleton().getPluginManager().getDefaultPluginInterface();
				local_publisher = new LocalResourceHTTPServer(pi, null);
			} catch (Throwable e) {
				Debug.out("Failed to create local resource publisher", e);
				return null;
			}
		}
		try {
			return local_publisher.publishResource(f);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
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
			browser.setData("StartURL", url);
		}
	}

	public boolean isPageLoading() {
		return context == null ? false : context.isPageLoading();
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectBasic#setVisible(boolean)
	public void setIsVisible(final boolean visible, boolean walkup) {
		super.setIsVisible(visible, walkup);

		// notify browser after we've fully processed visibility 
		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				if (!isDisposed() && context != null) {
					context.sendBrowserMessage("browser", visible ? "shown" : "hidden");
				}
			}
		});
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

}
