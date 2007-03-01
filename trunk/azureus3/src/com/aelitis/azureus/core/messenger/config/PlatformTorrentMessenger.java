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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.json.JSONArray;
import org.json.JSONObject;

import com.aelitis.azureus.core.messenger.PlatformMessage;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.core.messenger.PlatformMessengerListener;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;

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

	/**
	 * @param torrent
	 * @param maxDelayMS
	 * @param replyListener
	 *
	 * @since 3.0.0.7
	 */
	public static void getMetaData(TOTorrent[] torrents, long maxDelayMS,
			final GetMetaDataReplyListener replyListener) {
		if (PlatformConfigMessenger.getRPCVersion() > 0) {
			// We can use the better function
			
			JSONObject jsonObject = new JSONObject();
			JSONArray jsonArray = new JSONArray();
			jsonObject.put("hashes", jsonArray);
			
			for (int i = 0; i < torrents.length; i++) {
				TOTorrent torrent = torrents[i];
				String hash = null;
				try {
					hash = torrent.getHashWrapper().toBase32String();
				} catch (TOTorrentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				if (hash != null) {
					JSONObject jsonSubObject = new JSONObject();
					jsonArray.put(jsonObject);
					jsonSubObject.put("hash", hash);
					jsonSubObject.put("last-revision", new Long(PlatformTorrentUtils.getContentLastUpdated(torrent)));
				}
			}
			
			PlatformMessage message = new PlatformMessage("AZMSG", LISTENER_ID,
					OP_GETMETADATA, jsonObject, maxDelayMS);

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
		} else {
			// use the old one
			ArrayList hashes = new ArrayList();
			for (int i = 0; i < torrents.length; i++) {
				TOTorrent torrent = torrents[i];
				try {
					String hash = torrent.getHashWrapper().toBase32String();
					if (hash != null) {
						hashes.add(hash);
					}
				} catch (TOTorrentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			PlatformTorrentMessenger.getMetaData(
					(String[]) hashes.toArray(new String[hashes.size()]), maxDelayMS,
					replyListener);
		}
	}
}
