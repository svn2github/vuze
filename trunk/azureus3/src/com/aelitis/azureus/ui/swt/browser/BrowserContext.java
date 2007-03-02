/*
 * Created on Jul 19, 2006 10:16:26 PM
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package com.aelitis.azureus.ui.swt.browser;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.*;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Utils;
import org.json.JSONString;

import com.aelitis.azureus.core.messenger.ClientMessageContextImpl;
import com.aelitis.azureus.core.messenger.config.PlatformConfigMessenger;
import com.aelitis.azureus.ui.swt.browser.msg.BrowserMessage;
import com.aelitis.azureus.ui.swt.browser.msg.MessageListener;

/**
 * Manages the context for a single SWT {@link Browser} component,
 * including transactions, listeners and messages.
 * 
 * @author dharkness
 * @created Jul 19, 2006
 */
public class BrowserContext
	extends ClientMessageContextImpl
	implements DisposeListener
{
	public static final String LISTENER_ID = "context";

	public static final String OP_PAGE_CHANGED = "page-changed";

	private static final String CONTEXT_KEY = "BrowserContext";

	private static final String KEY_ENABLE_MENU = "browser.menu.enable";

	private Browser browser;

	private Display display;

	private boolean pageLoading = false;

	/**
	 * Creates a context and registers the given browser.
	 * 
	 * @param id unique identifier of this context
	 * @param browser the browser to be registered
	 */
	public BrowserContext(String id, Browser browser,
			Control widgetWaitingIndicator) {
		this(id);
		registerBrowser(browser, widgetWaitingIndicator);
	}

	/**
	 * Creates a context without a registered browser.
	 * This method should rarely be used.
	 * 
	 * @param id unique identifier of this context
	 */
	public BrowserContext(String id) {
		super(id);
	}

	public void registerBrowser(final Browser browser,
			final Control widgetWaitIndicator) {
		if (this.browser != null) {
			throw new IllegalStateException("Context " + getID()
					+ " already has a registered browser");
		}

		final TimerEventPerformer showBrowersPerformer = new TimerEventPerformer() {
			public void perform(TimerEvent event) {
				if (browser != null && !browser.isDisposed()) {
					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {
							if (browser != null && !browser.isDisposed()
									&& !browser.isVisible()) {
								browser.setVisible(true);
							}
						}
					});
				}
			}
		};

		final TimerEventPerformer hideIndicatorPerformer = new TimerEventPerformer() {
			public void perform(TimerEvent event) {
				if (widgetWaitIndicator != null && !widgetWaitIndicator.isDisposed()) {
					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {
							if (widgetWaitIndicator != null
									&& !widgetWaitIndicator.isDisposed()) {
								widgetWaitIndicator.setVisible(false);
							}
						}
					});
				}
			}
		};

		browser.setVisible(false);
		if (widgetWaitIndicator != null && !widgetWaitIndicator.isDisposed()) {
			widgetWaitIndicator.setVisible(false);
		}
		browser.addTitleListener(new TitleListener() {
			public void changed(TitleEvent event) {
				if (!browser.isVisible()) {
					SimpleTimer.addEvent("Show Browser",
							System.currentTimeMillis() + 700, showBrowersPerformer);
				}
			}
		});

		browser.addProgressListener(new ProgressListener() {
			public void changed(ProgressEvent event) {
				//int pct = event.total == 0 ? 0 : 100 * event.current / event.total;
				//System.out.println(pct + "%/" + event.current + "/" + event.total);
			}

			public void completed(ProgressEvent event) {
				if (!browser.isVisible()) {
					browser.setVisible(true);
				}
				if (org.gudy.azureus2.core3.util.Constants.isOSX) {
					Shell shell = browser.getShell();
					Point size = shell.getSize();
					size.x -= 1;
					size.y -= 1;
					shell.setSize(size);
					size.x += 1;
					size.y += 1;
					shell.setSize(size);
				}
			}
		});

		browser.addLocationListener(new LocationListener() {
			private TimerEvent timerevent;

			public void changed(LocationEvent event) {
				if (timerevent != null) {
					timerevent.cancel();
				}
				if (widgetWaitIndicator != null && !widgetWaitIndicator.isDisposed()) {
					widgetWaitIndicator.setVisible(false);
				}
			}

			public void changing(LocationEvent event) {
				boolean isWebURL = event.location.startsWith("http://")
						|| event.location.startsWith("https://");
				if (!isWebURL) {
					// we don't get a changed state on non URLs (mailto, javascript, etc)
					return;
				}

				// Regex Test for https?://moo\.com:?[0-9]*/dr
				// http://moo.com/dr
				// httpd://moo.com:80/dr
				// https://moo.com:a0/dr
				// http://moo.com:80/dr
				// http://moo.com:8080/dr
				// https://moo.com/dr
				// https://moo.com:80/dr

				boolean blocked = PlatformConfigMessenger.isURLBlocked(event.location);

				if (blocked) {
					String[] whitelist = PlatformConfigMessenger.getURLWhitelist();
					debug("Canceling URL change to external: " + event.location
							+ " (does not match one of the " + whitelist.length
							+ " whitelist entries)");
					event.doit = false;
					browser.back();
				} else {
					if (widgetWaitIndicator != null && !widgetWaitIndicator.isDisposed()) {
						widgetWaitIndicator.setVisible(true);
					}

					// Backup in case changed(..) is never called
					timerevent = SimpleTimer.addEvent("Hide Indicator",
							System.currentTimeMillis() + 5000, hideIndicatorPerformer);
				}
			}
		});

		browser.setData(CONTEXT_KEY, this);
		browser.addDisposeListener(this);

		// enable right-click context menu only if system property is set
		final boolean enableMenu = System.getProperty(KEY_ENABLE_MENU, "0").equals(
				"1");
		browser.addListener(SWT.MenuDetect, new Listener() {
			// @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
			public void handleEvent(Event event) {
				event.doit = enableMenu;
			}
		});

		getMessageDispatcher().registerBrowser(browser);
		this.browser = browser;
		this.display = browser.getDisplay();
	}

	public void deregisterBrowser() {
		if (browser == null) {
			throw new IllegalStateException("Context " + getID()
					+ " doesn't have a registered browser");
		}

		browser.setData(CONTEXT_KEY, null);
		browser.removeDisposeListener(this);
		getMessageDispatcher().deregisterBrowser(browser);
		browser = null;
	}

	/**
	 * Accesses the context associated with the given browser.
	 * 
	 * @param browser holds the context in its application data map
	 * @return the browser's context or <code>null</code> if there is none
	 */
	public static BrowserContext getContext(Browser browser) {
		Object data = browser.getData(CONTEXT_KEY);
		if (data != null && !(data instanceof BrowserContext)) {
			Debug.out("Data in Browser with key " + CONTEXT_KEY
					+ " is not a BrowserContext");
			return null;
		}

		return (BrowserContext) data;
	}

	public void addMessageListener(MessageListener listener) {
		getMessageDispatcher().addListener(listener);
	}

	public Object getBrowserData(String key) {
		return browser.getData(key);
	}

	public void setBrowserData(String key, Object value) {
		browser.setData(key, value);
	}

	public boolean sendBrowserMessage(String key, String op) {
		return sendBrowserMessage(key, op, null);
	}

	public boolean sendBrowserMessage(String key, String op, JSONString params) {
		StringBuffer msg = new StringBuffer();
		msg.append("az.msg.dispatch('").append(key).append("', '").append(op).append(
				"'");
		if (params != null) {
			msg.append(", ").append(params.toJSONString());
		}
		msg.append(")");

		return executeInBrowser(msg.toString());
	}

	protected boolean maySend(String key, String op, JSONString params) {
		return !pageLoading;
	}

	public boolean executeInBrowser(final String javascript) {
		if (!mayExecute(javascript)) {
			debug("BLOCKED: browser.execute( " + getShortJavascript(javascript)
					+ " )");
			return false;
		}
		if (display == null || display.isDisposed()) {
			debug("CANNOT: browser.execute( " + getShortJavascript(javascript) + " )");
			return false;
		}

		// swallow errors silently
		final String reallyExecute = "try { " + javascript + " } catch ( e ) { }";
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (browser == null || browser.isDisposed()) {
					debug("CANNOT: browser.execute( " + getShortJavascript(javascript)
							+ " )");
				} else if (!browser.execute(reallyExecute)) {
					debug("FAILED: browser.execute( " + getShortJavascript(javascript)
							+ " )");
				} else {
					debug("SUCCESS: browser.execute( " + getShortJavascript(javascript)
							+ " )");
				}
			}
		});

		return true;
	}

	protected boolean mayExecute(String javascript) {
		return !pageLoading;
	}

	public void handleMessage(BrowserMessage message) {
		String operationId = message.getOperationId();
		if (OP_PAGE_CHANGED.equals(operationId)) {
			pageChanged(message);
		} else {
			throw new IllegalArgumentException("Unknown operation: " + operationId);
		}
	}

	/**
	 * Resets the internal page identifier when a page loads or the URL changes.
	 * 
	 * @param message contains information about the new page
	 */
	private void pageChanged(BrowserMessage message) {
		// TODO determine the page identifier
		debug("Page changed");
	}

	public void widgetDisposed(DisposeEvent event) {
		if (event.widget == browser) {
			deregisterBrowser();
		}
	}

	private String getShortJavascript(String javascript) {
		if (javascript.length() < (256 + 3 + 256)) {
			return javascript;
		}
		StringBuffer result = new StringBuffer();
		result.append(javascript.substring(0, 256));
		result.append("...");
		result.append(javascript.substring(javascript.length() - 256));
		return result.toString();
	}
}