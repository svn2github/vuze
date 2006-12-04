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

package com.aelitis.azureus.core.messenger.config;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.aelitis.azureus.core.messenger.PlatformMessage;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.core.messenger.PlatformMessengerListener;

/**
 * @author TuxPaper
 * @created Oct 13, 2006
 *
 */
public class PlatformTorrentMessenger
{
	public static String LISTENER_ID = "torrent";
	
	public static String OP_GETMETADATA = "get-metadata";
	
	public static void getMetaData(String[] torrentHashes,
			long maxDelayMS, final GetMetaDataReplyListener replyListener) {
		
		PlatformMessage message = new PlatformMessage("AZMSG", LISTENER_ID,
				OP_GETMETADATA, new Object[] {
					"hashes",
					torrentHashes,
				}, maxDelayMS);

		PlatformMessengerListener listener = new PlatformMessengerListener() {
			public void messageSent(PlatformMessage message) {
				replyListener.messageSent();
			}

			public void replyReceived(PlatformMessage message, String replyType,
					Object JSONReply) {
				if (JSONReply instanceof JSONObject) {
					Map reply = ((JSONObject) JSONReply).toMap();
					replyListener.replyReceived(replyType, reply);
				} else {
					replyListener.replyReceived(replyType, new HashMap());
				}
			}
		};

		PlatformMessenger.queueMessage(message, listener);
	}
	
	public static interface GetMetaDataReplyListener {
		public void messageSent();
		
		public void replyReceived(String replyType, Map mapHashes);
	}
}
