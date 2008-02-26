package com.aelitis.azureus.ui.swt.browser.listener;

import com.aelitis.azureus.ui.swt.shells.LightBoxBrowserWindow;

/**
 * A listener to browser requests that responds with a LightBox browser window
 * <p>
 * This class is a good reference implementation that can be readily copied
 * for when we want to extend support for other types of browsers.
 * 
 * @author knguyen
 *
 */
public class LightBoxBrowserRequestListener
	extends AbstractBrowserRequestListener
{

	public LightBoxBrowserRequestListener() {
		super();
	}

	public void handleOpenURL() {
		LightBoxBrowserWindow lbWindow = new LightBoxBrowserWindow(getURL(),
				getPrefixVerifier(), getWidth(), getHeight());

		if (null != getRedirectURL()) {
			lbWindow.setRedirectURL(getRedirectURL());
		}
	}

}
