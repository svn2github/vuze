package com.aelitis.azureus.ui.swt.browser.listener;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.donations.DonationWindow;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;

import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.cnetwork.ContentNetworkManagerFactory;
import com.aelitis.azureus.core.messenger.browser.BrowserMessage;
import com.aelitis.azureus.core.messenger.browser.listeners.AbstractBrowserMessageListener;
import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.core.subs.SubscriptionManagerFactory;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.mdi.MultipleDocumentInterface;
import com.aelitis.azureus.ui.selectedcontent.*;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.browser.BrowserWrapper;
import com.aelitis.azureus.ui.swt.feature.FeatureManagerUI;
import com.aelitis.azureus.ui.swt.mdi.BaseMdiEntry;
import com.aelitis.azureus.ui.swt.mdi.MultipleDocumentInterfaceSWT;
import com.aelitis.azureus.ui.swt.shells.BrowserWindow;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.views.skin.SBC_BurnFTUX;
import com.aelitis.azureus.ui.swt.views.skin.SBC_PlusFTUX;
import com.aelitis.azureus.util.*;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.ui.*;

public class DisplayListener
	extends AbstractBrowserMessageListener
{

	public static final String DEFAULT_LISTENER_ID = "display";

	public static final String OP_COPY_TO_CLIPBOARD = "copy-text";

	public static final String OP_OPEN_URL = "open-url";

	public static final String OP_RESET_URL = "reset-url";

	public static final String OP_SEND_EMAIL = "send-email";

	public static final String OP_IRC_SUPPORT = "irc-support";

	public static final String OP_BRING_TO_FRONT = "bring-to-front";

	public static final String OP_SWITCH_TO_TAB = "switch-to-tab";

	public static final String OP_REFRESH_TAB = "refresh-browser";

	public static final String VZ_NON_ACTIVE = "vz-non-active";

	public static final String OP_SET_SELECTED_CONTENT = "set-selected-content";

	public static final String OP_GET_SELECTED_CONTENT = "get-selected-content";

	public static final String OP_SHOW_DONATION_WINDOW = "show-donation-window";

	public static final String OP_OPEN_SEARCH = "open-search";

	public static final String OP_REGISTER = "open-register";

	private BrowserWrapper browser;

	public DisplayListener(String id, BrowserWrapper browser) {
		super(id);
		this.browser = browser;
	}

	/**
	 * 
	 */
	public DisplayListener(BrowserWrapper browser) {
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
			if ((target == null || "_blank".equals(target))
					&& !decodedMap.containsKey("width")) {
				launchUrl(MapUtils.getMapString(decodedMap, "url", null),
						MapUtils.getMapBoolean(decodedMap, "append-suffix", false));
			} else {
				String ref = message.getReferer();
				if (target != null
						&& target.equals(SkinConstants.VIEWID_BROWSER_BROWSE)
						&& ref != null) {
					ContentNetwork cn = ContentNetworkManagerFactory.getSingleton().getContentNetworkForURL(
							ref);
					if (cn != null) {
						target = ContentNetworkUtils.getTarget(cn);
						System.err.println("TARGET REWRITTEN TO " + target);
					}
				}
				message.setCompleteDelayed(true);
				showBrowser(MapUtils.getMapString(decodedMap, "url", null), target,
						MapUtils.getMapInt(decodedMap, "width", 0), MapUtils.getMapInt(
								decodedMap, "height", 0), MapUtils.getMapBoolean(decodedMap,
								"resizable", false), message, MapUtils.getMapString(decodedMap,
								"source-ref", ref));
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
			switchToTab(MapUtils.getMapString(decodedMap, "target", ""),
					MapUtils.getMapString(decodedMap, "source-ref", message.getReferer()));
		} else if (OP_REFRESH_TAB.equals(opid)) {
			Map decodedMap = message.getDecodedMap();
			refreshTab(MapUtils.getMapString(decodedMap, "browser-id", ""));
		} else if (OP_SET_SELECTED_CONTENT.equals(opid)) {
			Map decodedMap = message.getDecodedMap();
			if (decodedMap != null) {
				setSelectedContent(message, decodedMap);
			}
		} else if (OP_GET_SELECTED_CONTENT.equals(opid)) {
			Map decodedMap = message.getDecodedMap();
			if (decodedMap != null) {
				getSelectedContent(message, decodedMap);
			}
		} else if (OP_SHOW_DONATION_WINDOW.equals(opid)) {
			Map decodedMap = message.getDecodedMap();
			DonationWindow.open(true, MapUtils.getMapString(decodedMap, "source-ref",
					"RPC"));
		} else if (OP_OPEN_SEARCH.equals(opid)) {
			Map decodedMap = message.getDecodedMap();
			UIFunctions uif = UIFunctionsManager.getUIFunctions();
			if (uif != null) {
				uif.doSearch(MapUtils.getMapString(decodedMap, "search-text", ""));
			}
		} else if (OP_REGISTER.equals(opid)) {
			FeatureManagerUI.openLicenceEntryWindow(false, null);
		} else {
			throw new IllegalArgumentException("Unknown operation: " + opid);
		}
	}

	private void getSelectedContent(BrowserMessage message, Map decodedMap) {
		String callback = MapUtils.getMapString(decodedMap, "callback", null);
		if (callback == null) {
			return;
		}
		
		List<Map> list = new ArrayList<Map>();
		
		DownloadManager[] dms = SelectedContentManager.getDMSFromSelectedContent();
		for (DownloadManager dm : dms) {
			if (dm != null) {
				Map<String, Object> mapDM = new HashMap<String, Object>();
				TOTorrent torrent = dm.getTorrent();
				if (torrent != null && !TorrentUtils.isReallyPrivate(torrent)) {
					try {
  					// make a copy of the torrent
  
  					Map torrent_map = torrent.serialiseToMap();
  					TOTorrent torrent_to_send = TOTorrentFactory.deserialiseFromMap( torrent_map );
  					Map	vuze_map = (Map)torrent_map.get( "vuze" );
  					// remove any non-standard stuff (e.g. resume data)
  					torrent_to_send.removeAdditionalProperties();
  					torrent_map = torrent_to_send.serialiseToMap();
  					if ( vuze_map != null ){
  						torrent_map.put( "vuze", vuze_map );
  					}
  					
  					byte[] encode = BEncoder.encode(torrent_map);
  					
  					mapDM.put("name", PlatformTorrentUtils.getContentTitle2(dm));
  					mapDM.put("torrent", Base32.encode(encode));
  					
  					list.add(mapDM);
					} catch (Throwable t) {
						Debug.out(t);
					}
				}
			}
		}
		
		if (list.size() > 0 && context != null) {
			context.executeInBrowser(callback + "(" + JSONUtils.encodeToJSON(list) + ")");
		}
	}

	/**
	 * @param message 
	 * @param decodedMap
	 *
	 * @since 3.1.1.1
	 */
	private void setSelectedContent(BrowserMessage message, Map decodedMap) {
		String hash = MapUtils.getMapString(decodedMap, "torrent-hash", null);
		String displayName = MapUtils.getMapString(decodedMap, "display-name", null);
		String dlURL = MapUtils.getMapString(decodedMap, "download-url", null);
		String referer = MapUtils.getMapString(decodedMap, "referer",
				"displaylistener");

		if (hash == null && dlURL == null) {
			SelectedContentManager.changeCurrentlySelectedContent(referer, null);
		}

		String callback = MapUtils.getMapString(decodedMap, "callback", null);
		if (callback != null && context != null) {
			DownloadUrlInfoSWT dlInfo = new DownloadUrlInfoSWT(context, callback,
					hash);
			boolean canPlay = MapUtils.getMapBoolean(decodedMap, "can-play", false);
			boolean isVuzeContent = MapUtils.getMapBoolean(decodedMap,
					"is-vuze-content", true);

			SelectedContentV3 content = new SelectedContentV3(hash, displayName,
					isVuzeContent, canPlay);
			content.setDownloadInfo(dlInfo);

			SelectedContentManager.changeCurrentlySelectedContent(referer,
					new ISelectedContent[] {
						content
					});
			return;
		}

		if (displayName != null) {
			String dlReferer = MapUtils.getMapString(decodedMap, "download-referer",
					null);
			String dlCookies = MapUtils.getMapString(decodedMap, "download-cookies",
					null);
			Map dlHeader = MapUtils.getMapMap(decodedMap, "download-header", null);

			boolean canPlay = MapUtils.getMapBoolean(decodedMap, "can-play", false);
			boolean isVuzeContent = MapUtils.getMapBoolean(decodedMap,
					"is-vuze-content", true);
			SelectedContentV3 content = new SelectedContentV3(hash, displayName,
					isVuzeContent, canPlay);
			content.setThumbURL(MapUtils.getMapString(decodedMap, "thumbnail.url",
					null));

			DownloadUrlInfo dlInfo = new DownloadUrlInfoContentNetwork(dlURL,
					ContentNetworkManagerFactory.getSingleton().getContentNetwork(
							context.getContentNetworkID()));
			dlInfo.setReferer(dlReferer);
			if (dlCookies != null) {
				if (dlHeader == null) {
					dlHeader = new HashMap();
				}
				dlHeader.put("Cookie", dlCookies);
			}
			dlInfo.setRequestProperties(dlHeader);

			String subID = MapUtils.getMapString(decodedMap, "subscription-id", null);
			String subresID = MapUtils.getMapString(decodedMap,
					"subscription-result-id", null);

			if (subID != null && subresID != null) {
				Subscription subs = SubscriptionManagerFactory.getSingleton().getSubscriptionByID(
						subID);
				if (subs != null) {
					subs.addPotentialAssociation(subresID, dlURL);
				}
			}

			// pass decodeMap down to TorrentUIUtilsV3.loadTorrent in case
			// is needs some other params
			dlInfo.setAdditionalProperties(decodedMap);

			content.setDownloadInfo(dlInfo);

			SelectedContentManager.changeCurrentlySelectedContent(referer,
					new ISelectedContent[] {
						content
					});
		} else {
			SelectedContentManager.changeCurrentlySelectedContent(referer, null);
		}
	}

	/**
	 * @param string
	 *
	 * @since 3.0.0.7
	 */
	public static void switchToTab(String tabID, String sourceRef) {
		MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
		if (mdi == null) {
			return;
		}
		if (sourceRef != null) {
			if (MultipleDocumentInterface.SIDEBAR_SECTION_PLUS.equals(tabID) ||
					MultipleDocumentInterface.SIDEBAR_SECTION_BURN_INFO.equals(tabID)) {
				Pattern pattern = Pattern.compile("http.*//[^/]+/([^.]+)");
				Matcher matcher = pattern.matcher(sourceRef);
				
				String sourceRef2;
				if (matcher.find()) {
					sourceRef2 = matcher.group(1);
				} else {
					sourceRef2 = sourceRef;
				}
				
				if (MultipleDocumentInterface.SIDEBAR_SECTION_PLUS.equals(tabID)) {
  				SBC_PlusFTUX.setSourceRef(sourceRef2);
				} else {
					SBC_BurnFTUX.setSourceRef(sourceRef2);
				}
			}
		}
		mdi.showEntryByID(tabID);
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

	/**
	 * @param string
	 *
	 * @since 3.0.5.0
	 */
	public static void refreshTab(String tabID) {
		if (null == tabID || tabID.length() < 1) {
			return;
		}

		SWTSkin skin = SWTSkinFactory.getInstance();

		/*
		 * Refreshes all except the currently active tab
		 */
		if (true == VZ_NON_ACTIVE.equals(tabID)) {
			// 3.2 TODO: Need to fix this up

			List browserViewIDs = new ArrayList();

			// Check if not active view and refresh (personally, sounds dangerous)

			for (Iterator iterator = browserViewIDs.iterator(); iterator.hasNext();) {
				refreshBrowser(iterator.next().toString());
			}

		} else {
			refreshBrowser(tabID);
		}
	}

	private static void refreshBrowser(final String browserID) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				MultipleDocumentInterfaceSWT mdi = UIFunctionsManagerSWT.getUIFunctionsSWT().getMDISWT();

				BaseMdiEntry entry = (BaseMdiEntry) mdi.getEntrySWT(browserID);
				//MdiEntrySWT entry = mdi.getEntrySWT(browserID);  // Use when UIs merged
				SWTSkinObjectBrowser soBrowser = SWTSkinUtils.findBrowserSO(entry.getSkinObject());

				if (soBrowser != null) {
					soBrowser.refresh();
					return;
				}

				SWTSkin skin = SWTSkinFactory.getInstance();
				SWTSkinObject skinObject = skin.getSkinObject(browserID);
				if (skinObject instanceof SWTSkinObjectBrowser) {
					final BrowserWrapper browser = ((SWTSkinObjectBrowser) skinObject).getBrowser();
					if (null != browser && false == browser.isDisposed()) {
						browser.refresh();
					}
				}
			}
		});
	}

	private void launchUrl(String url, boolean appendSuffix) {
		ContentNetwork cn = ContentNetworkUtils.getContentNetworkFromTarget(null);
		if (url.startsWith("/")){
			url = cn.getExternalSiteRelativeURL(url, appendSuffix);
		} else if (appendSuffix) {
			url = cn.appendURLSuffix(url, false, true);
		}
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
			final int h, final boolean allowResize, final BrowserMessage message,
			final String sourceRef) {
		final UIFunctions functions = UIFunctionsManager.getUIFunctions();
		if (functions == null) {
			AEThread2 thread = new AEThread2("show browser " + url, true) {
				public void run() {
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
			thread.start();
			return;
		}

		AEThread2 thread = new AEThread2("show browser " + url, true) {
			public void run() {
				if (w == 0 && target != null) {
					functions.viewURL(url, target, sourceRef);
				} else {
					functions.viewURL(url, target, w, h, allowResize, false);
				}
				message.complete(false, true, null);
			}
		};
		thread.start();
	}
}
