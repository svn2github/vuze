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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.json.JSONObject;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.messenger.config.PlatformRatingMessenger;
import com.aelitis.azureus.core.messenger.config.PlatformRatingMessenger.GetRatingReplyListener;
import com.aelitis.azureus.util.Constants;

import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;

/**
 * @author TuxPaper
 * @created Sep 26, 2006
 *
 */
public class Test
{
	public void initialize(PluginInterface pi) throws PluginException {
		Constants.initialize(AzureusCoreFactory.getSingleton());
		System.out.println(Constants.URL_SUFFIX);

		PlatformMessenger.init();
		Map parameters = new HashMap();
		parameters.put("section-type", "browse");
		parameters.put("locale", Locale.getDefault().toString());
		System.out.println(SystemTime.getCurrentTime() + ": queueMessage 0");
		PlatformMessenger.queueMessage(new PlatformMessage("AZMSG", "config",
				"get-browse-sections", new JSONObject(parameters), 150),
				new PlatformMessengerListener() {

					public void replyReceived(PlatformMessage message, String replyType,
							Object JSONReply) {
						System.out.println(SystemTime.getCurrentTime() + ": replyRecieved "
								+ message + ";" + replyType + ";" + JSONReply);
					}

					public void messageSent(PlatformMessage message) {
						System.out.println(SystemTime.getCurrentTime() + ": messageSent"
								+ message);
					}

				});

		parameters = new HashMap();
		parameters.put("section-type", "minibrowse");
		parameters.put("locale", Locale.getDefault().toString());
		System.out.println(SystemTime.getCurrentTime() + ": queueMessage 1");
		PlatformMessenger.queueMessage(new PlatformMessage("AZMSG", "config",
				"get-browse-sections", new JSONObject(parameters), 550),
				new PlatformMessengerListener() {

					public void replyReceived(PlatformMessage message, String replyType,
							Object JSONReply) {
						System.out.println(SystemTime.getCurrentTime() + ": replyRecieved "
								+ message + ";" + replyType + ";" + JSONReply);
					}

					public void messageSent(PlatformMessage message) {
						System.out.println(SystemTime.getCurrentTime() + ": messageSent"
								+ message);
					}

				});

		System.out.println(SystemTime.getCurrentTime() + ": queueMessage gr");
		PlatformRatingMessenger.getUserRating(
				new String[] { PlatformRatingMessenger.RATE_TYPE_CONTENT
				}, new String[] { "11"
				}, 500, new GetRatingReplyListener() {

					public void messageSent() {
						System.out.println(SystemTime.getCurrentTime() + ": r messageSent");
					}

					public void replyReceived(String replyType, PlatformRatingMessenger.GetRatingReply reply) {
						System.out.println(SystemTime.getCurrentTime() + ": replyRecieved "
								+ ";" + replyType + ";" + reply.getMap().size());
						dumpMap(reply.getMap(), "");
					}

				});

		System.out.println(SystemTime.getCurrentTime() + ": queueMessage 3");
		PlatformRatingMessenger.setUserRating(
				"11", 1, 500, null);
	}

	public static void dumpMap(Map map, String indent) {
		for (Iterator iterator = map.keySet().iterator(); iterator.hasNext();) {
			Object key = (Object) iterator.next();
			Object value = map.get(key);
			if (value instanceof Map) {
				System.out.println(key + " - " + ((Map) value).size());
				dumpMap((Map) value, indent + "  ");
			}
			System.out.println(indent + key + ": " + value);
		}
	}

	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display, SWT.DIALOG_TRIM);
		shell.open();

		int count = 0;
		try {
			AzureusCore core = AzureusCoreFactory.create();
			Test test = new Test();
			test.initialize(core.getPluginManager().getDefaultPluginInterface());

			while (!shell.isDisposed()) {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}

		} catch (Throwable e) {

			Debug.printStackTrace(e);
		}
	}

}
