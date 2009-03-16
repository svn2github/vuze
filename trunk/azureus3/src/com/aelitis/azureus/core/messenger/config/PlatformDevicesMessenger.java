/**
 * Created on Mar 12, 2009
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package com.aelitis.azureus.core.messenger.config;

import java.util.HashMap;

import org.gudy.azureus2.core3.config.COConfigurationManager;

import com.aelitis.azureus.core.devices.*;
import com.aelitis.azureus.core.messenger.PlatformMessage;
import com.aelitis.azureus.core.messenger.PlatformMessenger;

/**
 * @author TuxPaper
 * @created Mar 12, 2009
 *
 */
public class PlatformDevicesMessenger
{
	public static final String CFG_SEND_QOS = "devices.sendQOS";

	public static final String LISTENER_ID = "devices";

	private static final String OP_QOS_TURN_ON = "qos-turn-on";

	private static final String OP_QOS_FOUND_DEVICE = "qos-found-device";

	private static final String OP_QOS_TRANCODE = "qos-trancode";

	private static final String OP_QOS_PLAYBACK = "qos-playback";

	private static final String OP_GET_PROFILES = "get-profiles";

	private static final String OP_QOS_DROP = "get-profiles";

	public static void qosTurnOn(boolean withITunes) {
		if (!COConfigurationManager.getBooleanParameter(CFG_SEND_QOS, false)) {
			return;
		}

		PlatformMessage message = new PlatformMessage("AZMSG", LISTENER_ID,
				OP_QOS_TURN_ON, new Object[] {
					"itunes",
					Boolean.valueOf(withITunes),
				}, 5000);
		message.setSendAZID(false);
		PlatformMessenger.queueMessage(message, null);
	}

	public static void qosFoundDevice(Device device) {
		if (device == null
				|| !COConfigurationManager.getBooleanParameter(CFG_SEND_QOS, false)) {
			return;
		}

		PlatformMessage message = new PlatformMessage("AZMSG", LISTENER_ID,
				OP_QOS_FOUND_DEVICE, new Object[] {
					"device-name",
					device.getName(),
				}, 5000);
		message.setSendAZID(false);
		PlatformMessenger.queueMessage(message, null);
	}

	public static void qosYouJustDropped(DeviceMediaRenderer renderer) {
		if (!COConfigurationManager.getBooleanParameter(CFG_SEND_QOS, false)) {
			return;
		}

		HashMap<String,Object> map = new HashMap<String, Object>();
		if (renderer != null) {
			map.put("renderer-species", Integer.valueOf(renderer.getRendererSpecies()));
		}
		
		
		PlatformMessage message = new PlatformMessage("AZMSG", LISTENER_ID,
				OP_QOS_DROP, map, 5000);
		message.setSendAZID(false);
		PlatformMessenger.queueMessage(message, null);
	}

	public static void qosTranscode(TranscodeJob job, int state,
			DeviceMediaRenderer renderer, TranscodeProfile profile, String sourceExt,
			long filesize, long processTime) {
		if (!COConfigurationManager.getBooleanParameter(CFG_SEND_QOS, false)) {
			return;
		}

		HashMap<String,Object> map = new HashMap<String, Object>();

		map.put("job-state", Integer.valueOf(state));

		if (job != null) {
			if (state == TranscodeJob.ST_FAILED) {
				map.put("job-error", job.getError());
			}
		}
		map.put("profile-uid", profile.getUID());
		map.put("render-species", Integer.valueOf(renderer.getRendererSpecies()));
		map.put("process-time-ms", new Long(processTime));
		map.put("source-ext", sourceExt);
		if (filesize > 0) {
			map.put("file-size", new Long(filesize));
		}

		PlatformMessage message = new PlatformMessage("AZMSG", LISTENER_ID,
				OP_QOS_TRANCODE, map, 5000);
		message.setSendAZID(false);
		PlatformMessenger.queueMessage(message, null);
	}
}
