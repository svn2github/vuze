package com.aelitis.azureus.ui.swt.browser.listener;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.json.JSONException;
import org.json.JSONObject;

import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.swt.browser.msg.AbstractMessageListener;
import com.aelitis.azureus.ui.swt.browser.msg.BrowserMessage;
import com.aelitis.azureus.ui.swt.shells.BrowserWindow;
import com.aelitis.azureus.util.JSONUtils;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerListener;

public class DisplayListener
	extends AbstractMessageListener
{

	public static final String DEFAULT_LISTENER_ID = "display";

	public static final String OP_COPY_TO_CLIPBOARD = "copy-text";

	public static final String OP_OPEN_URL = "open-url";

	public static final String OP_RESET_URL = "reset-url";

	public static final String OP_SEND_EMAIL = "send-email";

	public static final String OP_IRC_SUPPORT = "irc-support";

	public static final String OP_BRING_TO_FRONT = "bring-to-front";

	private Browser browser;

	public DisplayListener(String id, Browser browser) {
		super(id);
		this.browser = browser;
	}

	/**
	 * 
	 */
	public DisplayListener(Browser browser) {
		this(DEFAULT_LISTENER_ID, browser);
	}

	public void handleMessage(BrowserMessage message) {
		String opid = message.getOperationId();

		if (OP_COPY_TO_CLIPBOARD.equals(opid)) {
			JSONObject decodedObject = message.getDecodedObject();
			copyToClipboard(decodedObject.getString("text"));
		} else if (OP_OPEN_URL.equals(opid)) {
			JSONObject decodedObject = message.getDecodedObject();
			String target = JSONUtils.getJSONString(decodedObject, "target", null);
			if (target == null && !decodedObject.has("width")) {
				launchUrl(JSONUtils.getJSONString(decodedObject, "url", null));
			} else {
				message.setCompleteDelayed(true);
				showBrowser(JSONUtils.getJSONString(decodedObject, "url", null),
						target, JSONUtils.getJSONInt(decodedObject, "width", 0),
						JSONUtils.getJSONInt(decodedObject, "height", 0),
						JSONUtils.getJSONBoolean(decodedObject, "resizable", false),
						message);
			}
		} else if (OP_RESET_URL.equals(opid)) {
			resetURL();
		} else if (OP_SEND_EMAIL.equals(opid)) {
			JSONObject decodedObject = message.getDecodedObject();

			String to = decodedObject.getString("to");
			String subject = decodedObject.getString("subject");

			String body = null;

			try {
				body = decodedObject.getString("body");
			} catch (JSONException e) {
				// Do nothing if its not found 
			}

			sendEmail(to, subject, body);

		} else if (OP_IRC_SUPPORT.equals(opid)) {
			JSONObject decodedObject = message.getDecodedObject();
			openIrc(null, decodedObject.getString("channel"),
					decodedObject.getString("user"));
		} else if (OP_BRING_TO_FRONT.equals(opid)) {
			bringToFront();
		} else {
			throw new IllegalArgumentException("Unknown operation: " + opid);
		}
	}

	/**
	 * 
	 */
	private void bringToFront() {
		final UIFunctions functions = UIFunctionsManager.getUIFunctions();
		if (functions != null) {
			functions.bringToFront();
		}
	}

	/**
	 * 
	 */
	private void resetURL() {
		if (browser == null || browser.isDisposed()) {
			return;
		}

		String sURL = (String) browser.getData("StartURL");
		System.out.println("reset " + sURL);
		if (sURL != null && sURL.length() > 0) {
			if (sURL.indexOf('?') > 0) {
				browser.setUrl(sURL + "&rnd=" + Math.random());
			} else {
				browser.setUrl(sURL + "?rnd=" + Math.random());
			}
		}
	}

	private void launchUrl(final String url) {
		if (url.startsWith("http://") || url.startsWith("https://")
				|| url.startsWith("mailto:")) {
			Utils.launch(url);
		}
	}

	private void sendEmail(final String to, final String subject,
			final String body) {
		String url = "mailto:" + to + "?subject=" + UrlUtils.encode(subject);

		if (body != null) {
			url = url + "&body=" + UrlUtils.encode(body);
		}
		Utils.launch(url);
	}

	private void copyToClipboard(final String text) {
		if (browser == null || browser.isDisposed()) {
			return;
		}
		final Clipboard cb = new Clipboard(browser.getDisplay());
		TextTransfer textTransfer = TextTransfer.getInstance();
		cb.setContents(new Object[] {
			text
		}, new Transfer[] {
			textTransfer
		});
		cb.dispose();
	}

	private void openIrc(final String server, final String channel,
			final String alias) {
		try {
			PluginManager pluginManager = PluginInitializer.getDefaultInterface().getPluginManager();
			PluginInterface piChat = pluginManager.getPluginInterfaceByID("azplugins");
			UIManager manager = piChat.getUIManager();
			manager.addUIListener(new UIManagerListener() {
				public void UIDetached(UIInstance instance) {
				}

				public void UIAttached(UIInstance instance) {
					if (instance instanceof UISWTInstance) {
						try {
							debug("Opening IRC channel " + channel + " on " + server
									+ " for user " + alias);
							UISWTInstance swtInstance = (UISWTInstance) instance;
							UISWTView[] openViews = swtInstance.getOpenViews(UISWTInstance.VIEW_MAIN);
							for (int i = 0; i < openViews.length; i++) {
								UISWTView view = openViews[i];
								// if only there was a way to tell if it was our IRC
								view.closeView();
							}

							swtInstance.openView(UISWTInstance.VIEW_MAIN, "IRC",
									new String[] {
										server,
										channel,
										alias
									});
						} catch (Exception e) {
							debug("Failure opening IRC channel " + channel + " on " + server,
									e);
						}
					}
				}
			});
		} catch (Exception e) {
			debug("Failure opening IRC channel " + channel + " on " + server, e);
		}
	}

	private void showBrowser(final String url, final String target, final int w,
			final int h, final boolean allowResize, final BrowserMessage message) {
		final UIFunctions functions = UIFunctionsManager.getUIFunctions();
		if (functions == null) {
			AEThread thread = new AEThread("show browser " + url) {
				public void runSupport() {
					final Display display = Display.getDefault();
					display.asyncExec(new AERunnable() {
						public void runSupport() {
							BrowserWindow window = new BrowserWindow(
									display.getActiveShell(), url, w, h, allowResize);
							window.waitUntilClosed();
							message.complete(false, true, null);
						}
					});
				}
			};
			thread.run();
			return;
		}

		AEThread thread = new AEThread("show browser " + url) {
			public void runSupport() {
				functions.viewURL(url, target, w, h, allowResize);
				message.complete(false, true, null);
			}
		};
		thread.run();
	}
}
