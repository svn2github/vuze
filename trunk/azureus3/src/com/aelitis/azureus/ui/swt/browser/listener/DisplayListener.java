package com.aelitis.azureus.ui.swt.browser.listener;

import java.util.Map;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;

import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.browser.msg.AbstractMessageListener;
import com.aelitis.azureus.ui.swt.browser.msg.BrowserMessage;
import com.aelitis.azureus.ui.swt.shells.BrowserWindow;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.util.MapUtils;

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

	public static final String OP_SWITCH_TO_TAB = "switch-to-tab";

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
			Map decodedMap = message.getDecodedMap();
			copyToClipboard(MapUtils.getMapString(decodedMap, "text", ""));
		} else if (OP_OPEN_URL.equals(opid)) {
			Map decodedMap = message.getDecodedMap();
			String target = MapUtils.getMapString(decodedMap, "target", null);
			if (target == null && !decodedMap.containsKey("width")) {
				launchUrl(MapUtils.getMapString(decodedMap, "url", null));
			} else {
				message.setCompleteDelayed(true);
				showBrowser(MapUtils.getMapString(decodedMap, "url", null),
						target, MapUtils.getMapInt(decodedMap, "width", 0),
						MapUtils.getMapInt(decodedMap, "height", 0),
						MapUtils.getMapBoolean(decodedMap, "resizable", false),
						message);
			}
		} else if (OP_RESET_URL.equals(opid)) {
			resetURL();
		} else if (OP_SEND_EMAIL.equals(opid)) {
			Map decodedMap = message.getDecodedMap();

			String to = MapUtils.getMapString(decodedMap, "to", "");
			String subject = MapUtils.getMapString(decodedMap, "subject", "");

			String body = MapUtils.getMapString(decodedMap, "body", null);

			sendEmail(to, subject, body);

		} else if (OP_IRC_SUPPORT.equals(opid)) {
			Map decodedMap = message.getDecodedMap();
			openIrc(null, MapUtils.getMapString(decodedMap, "channel", ""),
					MapUtils.getMapString(decodedMap, "user", ""));
		} else if (OP_BRING_TO_FRONT.equals(opid)) {
			bringToFront();
		} else if (OP_SWITCH_TO_TAB.equals(opid)) {
			Map decodedMap = message.getDecodedMap();
			switchToTab(MapUtils.getMapString(decodedMap, "target", ""));
		} else {
			throw new IllegalArgumentException("Unknown operation: " + opid);
		}
	}

	/**
	 * @param string
	 *
	 * @since 3.0.0.7
	 */
	private void switchToTab(String tabID) {
		SWTSkin skin = SWTSkinFactory.getInstance();
		SWTSkinObject skinObject = skin.getSkinObject("tab-" + tabID);
		if (skinObject != null) {
			skin.activateTab(skinObject);
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
		
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (browser == null || browser.isDisposed()) {
					return;
				}

				String sURL = (String) browser.getData("StartURL");
				context.debug("reset " + sURL);
				if (sURL != null && sURL.length() > 0) {
					String startURLUnique;
					String sRand = "rand=" + SystemTime.getCurrentTime();
					if (sURL.indexOf("rand=") > 0) {
						startURLUnique = sURL.replaceAll("rand=[0-9.]+", sRand);
					} else if (sURL.indexOf('?') > 0) {
						startURLUnique = sURL + "&" + sRand;
					} else {
						startURLUnique = sURL + "?" + sRand;
					}
					browser.setUrl(startURLUnique);
				}
			}
		});
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

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
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
		});
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
									display.getActiveShell(), url, w, h, allowResize, false);
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
				functions.viewURL(url, target, w, h, allowResize, false);
				message.complete(false, true, null);
			}
		};
		thread.run();
	}
}
