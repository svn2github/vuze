package com.aelitis.azureus.ui.swt.browser.listener;

import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.ui.swt.browser.msg.BrowserMessage;
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

	public static final String LISTENER_ID = "lightbox-browser";

	private LightBoxBrowserWindow browserWindow;

	public LightBoxBrowserRequestListener(LightBoxBrowserWindow browserWindow) {
		super(LISTENER_ID);
		this.browserWindow = browserWindow;
	}

	public LightBoxBrowserRequestListener() {
		super(LISTENER_ID);
	}

	public void handleOpenURL() {
		if (null == browserWindow) {
			new LightBoxBrowserWindow(getURL(), getWidth(), getHeight());
		} else {
			Debug.out("Attempting to open another LightBoxBrowserWindow from a LightBoxBrowserWindow");
		}
	}

	public void handleResize() {
		browserWindow.setSize(getWidth(), getHeight());
	}

	public void handleClose() {
		browserWindow.close();

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
		} else if (null != getDisplayMessage()) {
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
		browserWindow.refresh();
	}
}
