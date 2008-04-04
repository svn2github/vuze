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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.swt.browser.BrowserContext;
import com.aelitis.azureus.ui.swt.browser.listener.*;
import com.aelitis.azureus.ui.swt.browser.listener.publish.LocalHoster;
import com.aelitis.azureus.ui.swt.browser.listener.publish.PublishListener;
import com.aelitis.azureus.ui.swt.utils.PublishUtils;
import com.aelitis.azureus.util.Constants;
import com.aelitis.azureus.util.LocalResourceHTTPServer;

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

	private Composite cArea;

	private String sStartURL;

	private LocalResourceHTTPServer local_publisher;

	private BrowserContext context;

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

		AzureusCore core = AzureusCoreFactory.getSingleton();

		cArea = parent == null ? skin.getShell() : (Composite) parent.getControl();

		try {
			browser = new Browser(cArea, Utils.getInitialBrowserStyle(SWT.NONE));
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

		context = new BrowserContext(browserID, browser, widgetIndicator,
				properties.getBooleanValue(sConfigID + ".forceVisibleAfterLoad", true));
		context.addMessageListener(new TorrentListener(core));
		context.addMessageListener(new DisplayListener(browser));
		context.addMessageListener(new ConfigListener(browser));
		context.addMessageListener(new PublishListener(skin.getShell(), this));
		context.addMessageListener(new LightBoxBrowserRequestListener());
		context.addMessageListener(new StatusListener());
		PublishUtils.setupContext(context);

		setControl(browser);
	}

	public Browser getBrowser() {
		return browser;
	}

	public void setURL(final String url) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (url == null) {
					browser.setText("");
				} else {
					String urlToUse = url;
					if (urlToUse.indexOf("azid=") < 0) {
						if (urlToUse.indexOf("?") >= 0) {
							urlToUse += "&" + Constants.URL_SUFFIX;
						} else {
							urlToUse += "?" + Constants.URL_SUFFIX;
						}
					}
					browser.setUrl(urlToUse);
				}
				if (sStartURL == null) {
					sStartURL = url;
					browser.setData("StartURL", url);
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
		cArea.layout();
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
}
