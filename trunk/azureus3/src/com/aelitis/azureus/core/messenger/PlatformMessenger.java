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

package com.aelitis.azureus.core.messenger;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.util.*;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.widgets.Control;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.util.Timer;
import org.json.*;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.swt.browser.listener.DisplayListener;
import com.aelitis.azureus.ui.swt.browser.listener.TorrentListener;
import com.aelitis.azureus.ui.swt.browser.msg.BrowserMessage;
import com.aelitis.azureus.ui.swt.browser.msg.MessageCompletionListener;
import com.aelitis.azureus.util.Constants;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderFactory;

/**
 * @author TuxPaper
 * @created Sep 25, 2006
 *
 */
public class PlatformMessenger
{
	private static boolean USE_HTTP_POST = true;

	public static String REPLY_EXCEPTION = "exception";

	public static String REPLY_ACTION = "action";

	public static String REPLY_RESULT = "response";

	static private Map mapQueue = new HashMap();

	static private AEMonitor queue_mon = new AEMonitor(
			"v3.PlatformMessenger.queue");

	static private Timer timerProcess = new Timer("v3.PlatformMessenger.queue");

	static private TimerEvent timerEvent = null;

	private static boolean initialized;

	private static fakeContext context;

	public static void init() {
		initialized = true;

		context = new fakeContext();
		context.addMessageListener(new TorrentListener());
		context.addMessageListener(new DisplayListener(null));
	}

	public static void queueMessage(PlatformMessage message,
			PlatformMessengerListener listener) {
		if (!initialized) {
			init();
		}

		debug("q msg " + message + " for " + new Date(message.getFireBefore()));
		queue_mon.enter();
		try {
			mapQueue.put(message, listener);

			if (timerEvent == null || timerEvent.hasRun()) {
				timerEvent = timerProcess.addEvent(message.getFireBefore(),
						new TimerEventPerformer() {
							public void perform(TimerEvent event) {
								timerEvent = null;
								processQueue();
							}
						});
			} else {
				// Move the time up if we have to
				if (message.getFireBefore() < timerEvent.getWhen()) {
					timerProcess.adjustAllBy(message.getFireBefore()
							- timerEvent.getWhen());
				}
			}
		} finally {
			queue_mon.exit();
		}
	}

	/**
	 * @param string
	 */
	private static void debug(String string) {
		AEDiagnosticsLogger diag_logger = AEDiagnostics.getLogger("v3.PMsgr");
		diag_logger.log(string);
		System.out.println(string);
	}

	/**
	 * 
	 */
	protected static void processQueue() {
		if (!initialized) {
			init();
		}

		final Map mapProcessing = new HashMap();

		queue_mon.enter();
		try {
			mapProcessing.putAll(mapQueue);
			mapQueue.clear();
		} finally {
			queue_mon.exit();
		}
		debug("about to process " + mapProcessing.size());

		String urlStem = "";
		long sequenceNo = 0;
		for (Iterator iter = mapProcessing.keySet().iterator(); iter.hasNext();) {
			PlatformMessage message = (PlatformMessage) iter.next();
			message.setSequenceNo(sequenceNo);

			if (sequenceNo > 0) {
				urlStem += "&";
			}
			try {
				urlStem += "cmd="
						+ URLEncoder.encode(BrowserMessage.MESSAGE_PREFIX
								+ BrowserMessage.MESSAGE_DELIM + sequenceNo
								+ BrowserMessage.MESSAGE_DELIM + message.getListenerID()
								+ BrowserMessage.MESSAGE_DELIM + message.getOperationID()
								+ BrowserMessage.MESSAGE_DELIM
								+ message.getParameters().toString(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
			}

			PlatformMessengerListener listener = (PlatformMessengerListener) mapProcessing.get(message);
			if (listener != null) {
				listener.messageSent(message);
			}
			sequenceNo++;
		}

		String sURL;
		String sPostData = null;
		if (USE_HTTP_POST) {
			sURL = Constants.URL_PREFIX + Constants.URL_POST_PLATFORM_MESSAGE;
			sPostData = Constants.URL_POST_PLATFORM_DATA + "&" + urlStem + "&"
					+ Constants.URL_SUFFIX + "\n";
			debug("POST: " + sURL + "?" + sPostData);
		} else {
			sURL = Constants.URL_PREFIX + Constants.URL_PLATFORM_MESSAGE + "&"
					+ urlStem + "&" + Constants.URL_SUFFIX;
			debug("GET: " + sURL);
		}

		final String fURL = sURL;
		final String fPostData = sPostData;

		AEThread thread = new AEThread("v3.PlatformMessenger") {
			public void runSupport() {
				try {
					processQueueAsync(fURL, fPostData, mapProcessing);
				} catch (Exception e) {
					Debug.out("Error while sending message(s) to Platform", e);
					for (Iterator iter = mapProcessing.keySet().iterator(); iter.hasNext();) {
						PlatformMessage message = (PlatformMessage) iter.next();
						PlatformMessengerListener l = (PlatformMessengerListener) mapProcessing.get(message);
						if (l != null) {
							l.replyReceived(message, REPLY_EXCEPTION, e.toString());
						}
					}
				}
			}
		};
		thread.start();
	}

	/**
	 * @param mapProcessing 
	 * @param surl
	 * @throws Exception 
	 */
	protected static void processQueueAsync(String sURL, String sData,
			Map mapProcessing) throws Exception {
		URL url;
		url = new URL(sURL);
		AzureusCore core = AzureusCoreFactory.getSingleton();
		final PluginInterface pi = core.getPluginManager().getDefaultPluginInterface();

		String s = new String(downloadURL(pi, url, sData));

		// Format: <sequence no> ; <classification> [; <results>] [ \n ]

		if (s == null || s.length() == 0 || !Character.isDigit(s.charAt(0))) {
			Debug.out("Error while sending message(s) to Platform: reply = " + s);
			for (Iterator iter = mapProcessing.keySet().iterator(); iter.hasNext();) {
				PlatformMessage message = (PlatformMessage) iter.next();
				PlatformMessengerListener l = (PlatformMessengerListener) mapProcessing.get(message);
				if (l != null) {
					l.replyReceived(message, REPLY_EXCEPTION, s);
				}
			}
			return;
		}

		Map mapSeqToBrowserMsg = new HashMap();

		String[] replies = s.split("\\n");
		for (int i = 0; i < replies.length; i++) {
			String reply = replies[i];

			final String[] replySections = reply.split(BrowserMessage.MESSAGE_DELIM,
					3);
			if (replySections.length < 2) {
				continue;
			}
			long sequenceNo = NumberFormat.getInstance().parse(replySections[0]).longValue();

			Object jsonReply = null;

			if (replySections.length == 3) {
				JSONTokener tokener = new JSONTokener(replySections[2]);
				jsonReply = tokener.nextValue();
			}

			// Find PlatformMessage associated with sequence
			// TODO: There's a better way to do this
			PlatformMessage message = null;
			PlatformMessengerListener listener = null;
			for (Iterator iter = mapProcessing.keySet().iterator(); iter.hasNext();) {
				PlatformMessage potentialMessage = (PlatformMessage) iter.next();
				if (potentialMessage.getSequenceNo() == sequenceNo) {
					message = potentialMessage;
					listener = (PlatformMessengerListener) mapProcessing.get(message);
				}
			}

			if (message == null) {
				Debug.out("No message with sequence number " + sequenceNo);
				continue;
			}

			debug("Got a reply! " + reply + "\n\t for " + message.toString());

			final PlatformMessage fMessage = message;
			final PlatformMessengerListener fListener = listener;
			final Object fJSONReply = jsonReply;

			// test
			if (i == 0 && false) {
				replySections[1] = "action";
				jsonReply = new JSONObject();
				((JSONObject) jsonReply).put("retry-client-message", true);
				JSONArray a = new JSONArray();
				a.put("[AZMSG;1;display;open-url;{\"url\":\"http://yahoo.com\",\"width\":500,\"height\":200}]");
				((JSONObject) jsonReply).put("messages", a);
			}

			// Todo check array [1] for reply type

			if (replySections[1].equals("action")) {
				if (jsonReply instanceof JSONObject) {
					JSONObject actionResults = (JSONObject) jsonReply;

					final boolean bRetry = actionResults.has("retry-client-message")
							&& actionResults.getBoolean("retry-client-message");

					if (actionResults.has("messages")) {
						JSONArray array = actionResults.getJSONArray("messages");
						for (int j = 0; j < array.size(); j++) {
							final String sMsg = array.getString(j);
							debug("handling (" + ((bRetry) ? " with retry" : " no retry")
									+ "): " + sMsg);

							final BrowserMessage browserMsg = new BrowserMessage(sMsg);
							int seq = browserMsg.getSequence();
							BrowserMessage existingBrowserMsg = (BrowserMessage) mapSeqToBrowserMsg.get(new Long(
									seq));
							if (existingBrowserMsg != null) {
								existingBrowserMsg.addCompletionListener(new MessageCompletionListener() {
									public void completed(boolean success, Object data) {
										debug("got complete for " + sMsg);
										if (success) {
											queueMessage(fMessage, fListener);
										} else {
											if (fListener != null) {
												fListener.replyReceived(fMessage, replySections[1],
														fJSONReply);
											}
										}
									}
								});
								continue;
							}

							if (bRetry) {
								mapSeqToBrowserMsg.put(new Long(seq), browserMsg);

								browserMsg.addCompletionListener(new MessageCompletionListener() {
									public void completed(boolean success, Object data) {
										debug("got complete for " + sMsg + ";" + success);
										if (success) {
											queueMessage(fMessage, fListener);
										} else {
											if (fListener != null) {
												fListener.replyReceived(fMessage, replySections[1],
														fJSONReply);
											}
										}
									}
								});
							}

							new AEThread("v3.Msg.Dispatch") {
								public void runSupport() {
									context.getMessageDispatcher().dispatch(browserMsg);
								}
							}.start();
						}
					}
					if (bRetry) {
						continue;
					}
				}
			}

			if (listener != null) {
				listener.replyReceived(message, replySections[1], jsonReply);
			}
		}
		context.getMessageDispatcher().resetSequence();
	}

	private static byte[] downloadURL(PluginInterface pi, URL url, String postData)
			throws Exception {
		ResourceDownloaderFactory rdf = pi.getUtilities().getResourceDownloaderFactory();

		ResourceDownloader rd = rdf.create(url, postData);

		rd = rdf.getRetryDownloader(rd, 3);
		// We could report percentage to listeners, but there's no need to atm
		//		rd.addListener(new ResourceDownloaderListener() {
		//		
		//			public void reportPercentComplete(ResourceDownloader downloader,
		//					int percentage) {
		//			}
		//		
		//			public void reportActivity(ResourceDownloader downloader, String activity) {
		//			}
		//		
		//			public void failed(ResourceDownloader downloader,
		//					ResourceDownloaderException e) {
		//			}
		//		
		//			public boolean completed(ResourceDownloader downloader, InputStream data) {
		//				return true;
		//			}
		//		});

		InputStream is = rd.download();

		int length = is.available();

		byte data[] = new byte[length];

		is.read(data);

		return (data);
	}

	private static class fakeContext extends ClientMessageContextImpl
	{

		public fakeContext() {
			super("fakeContext");
		}

		public void deregisterBrowser() {
			debug("deregisterBrowser");
		}

		public void displayBrowserMessage(String message) {
			debug("displayBrowserMessage");
		}

		public boolean executeInBrowser(String javascript) {
			debug("executeInBrowser");
			return false;
		}

		public Object getBrowserData(String key) {
			debug("getBrowserData");
			return null;
		}

		public void handleMessage(BrowserMessage message) {
			debug("handleMessage");
		}

		public void registerBrowser(Browser browser, Control widgetWaitingIndicator) {
			debug("registerBrowser");
		}

		public boolean sendBrowserMessage(String key, String op) {
			debug("sendBrowserMessage");
			return false;
		}

		public boolean sendBrowserMessage(String key, String op, JSONString params) {
			debug("sendBrowserMessage");
			return false;
		}

		public void setBrowserData(String key, Object value) {
			debug("setBrowserData");
		}

		public void widgetDisposed(DisposeEvent event) {
			debug("widgetDisposed");
		}
	}
}
