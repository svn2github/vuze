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
import java.net.URL;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.messenger.ClientMessageContext;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.browser.BrowserContext;
import com.aelitis.azureus.ui.swt.browser.listener.publish.DownloadStateAndRemoveListener;
import com.aelitis.azureus.ui.swt.browser.listener.publish.LocalHoster;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.utils.PublishUtils;
import com.aelitis.azureus.util.Constants;
import com.aelitis.azureus.util.LocalResourceHTTPServer;

import org.gudy.azureus2.plugins.*;

/**
 * @author TuxPaper
 * @created Oct 1, 2006
 *
 */
public class Publish extends SkinView implements LocalHoster
{
	private LocalResourceHTTPServer local_publisher;

	private Browser browser;

	public Object showSupport(final SWTSkinObject skinObject, Object params) {
		AzureusCore core = AzureusCoreFactory.getSingleton();

		// first, check if it's already there (evil!)
		PluginInterface pi = core.getPluginManager().getPluginInterfaceByID(
				"azdirector");
		if (pi == null) {
			PluginManager.registerPlugin(new Plugin() {
				public void initialize(PluginInterface pluginInterface)
						throws PluginException {
				}
			}, "azdirector");

			// initialization should be immediate, since the UI is running
			pi = core.getPluginManager().getPluginInterfaceByID("azdirector");
		}

		// bridge between pi and core
		UISWTInstance swtInstance = UIFunctionsManagerSWT.getUIFunctionsSWT().getUISWTInstance();

		// copied from DirectorPlugin.initialize
		// For sending the thumbnail to the platform
		try {
			local_publisher = new LocalResourceHTTPServer(pi, null);
		} catch (Throwable e) {
			Debug.out("Failed to create local resource publisher", e);
		}

		Composite cArea = (Composite) skinObject.getControl();
		browser = new Browser(cArea, SWT.NONE);
		browser.setLayoutData(Utils.getFilledFormData());

		// copied from DirectorPlugin.java
		// We are going to monitor Published Torrent to alert the User when he 
		// removes a published torrent from azureus
		DownloadStateAndRemoveListener downloadListener = new DownloadStateAndRemoveListener(
				pi, cArea.getDisplay(), swtInstance);
		pi.getDownloadManager().addListener(downloadListener);

		// copied from PublisherPanel.initUI
		ClientMessageContext context = new BrowserContext("publish", browser, null);
		PublishUtils.setupContext(context, browser, pi, this, downloadListener);

		restart();

		//browser.setUrl("google.com");

		cArea.layout(true);
		return null;
	}

	public URL hostFile(File f) {
		try {
			return local_publisher.publishResource(f);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 
	 */
	public void restart() {
		if (browser != null) {
			String sURL = Constants.URL_PREFIX + "PublishedContent.html?"
					+ Constants.URL_SUFFIX;
			System.out.println(sURL);
			browser.setUrl(sURL);
			browser.setData("StartURL", sURL);
		}
	}
}
