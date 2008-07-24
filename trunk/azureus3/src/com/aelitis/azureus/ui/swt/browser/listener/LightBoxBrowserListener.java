package com.aelitis.azureus.ui.swt.browser.listener;

import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.messenger.browser.BrowserMessage;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.shells.LightBoxBrowserWindow;
import com.aelitis.azureus.ui.swt.shells.main.UIFunctionsImpl;
import com.aelitis.azureus.ui.swt.skin.SWTSkin;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectTab;
import com.aelitis.azureus.ui.swt.skin.SWTSkinTabSet;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;

/**
 * A listener to requests from a LightBox browser window
 * 
 * @author knguyen
 *
 */
public class LightBoxBrowserListener
	extends AbstractBrowserRequestListener
{

	private LightBoxBrowserWindow browserWindow;

	public LightBoxBrowserListener(LightBoxBrowserWindow browserWindow) {
		super();
		this.browserWindow = browserWindow;
	}

	public void handleResize() {
		if (null != browserWindow) {
			browserWindow.setSize(getWidth(), getHeight());
		}
	}

	public void handleClose() {

		if (null != browserWindow) {
			browserWindow.close();
		}

		/*
		 * If there is a status message attached then process it
		 */
		if (null != getStatusMessage()) {
			try {
				BrowserMessage statusMessage = new BrowserMessage(getStatusMessage());
				new StatusListener().handleMessage(statusMessage);
			} catch (Exception e) {
				Debug.out(e);
			}
		}

		if (true == isRedirect() && null != browserWindow.getRedirectURL()) {
			UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
			if (null == uiFunctions) {
				throw new IllegalStateException(
						"No instance of UIFunctionsSWT found; the UIFunctionsManager might not have been initialized properly");
			}

			/*
			 * Find the active browser (if any) and set it's URL to the RedirectURL
			 */
			if (uiFunctions instanceof UIFunctionsImpl) {
				SideBar sidebar = (SideBar)SkinViewManager.getByClass(SideBar.class);
				
				// 3.2 TODO: Get active content area, check if has a browser, and set url
				//           If that's still what we really want to do here..
			}
		}

		if (null != getDisplayMessage()) {
			try {
				BrowserMessage displayMessage = new BrowserMessage(getStatusMessage());

				/*
				 * Only the OPEN_URL operation really has any effect here since the rest of the operations
				 * for 'display' require a reference to a standard browser which would be incompatible
				 * with the new pop-up browser
				 */
				if (true == DisplayListener.OP_OPEN_URL.equals(displayMessage.getOperationId())) {
					new DisplayListener(null).handleMessage(displayMessage);
				}
			} catch (Exception e) {
				Debug.out(e);
			}

		}

	}

	public void handleRefresh() {
		if (null != browserWindow) {
			browserWindow.refresh();
		}
	}

}
