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

import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.devices.*;
import com.aelitis.azureus.core.messenger.PlatformMessage;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.torrent.Torrent;

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

	private static final String OP_QOS_TRANSCODE = "qos-transcode";

	private static final String OP_QOS_PLAYBACK = "qos-playback";

	private static final String OP_GET_PROFILES = "get-profiles";

	private static final String OP_REPORT_DEVICES = "report-devices";

	private static final String OP_QOS_TRANSCODE_REQUEST = "qos-transcode-request";
	
	private static String plugin_xcode_version = null;
	
	private static String plugin_itunes_version = null;

	public static void qosTurnOn(boolean withITunes) {
		if (!COConfigurationManager.getBooleanParameter(CFG_SEND_QOS, false)) {
			return;
		}

		PlatformMessage message = new PlatformMessage("AZMSG", LISTENER_ID,
				OP_QOS_TURN_ON, new Object[] {
					"itunes",
					Boolean.valueOf(withITunes),
					"os-name",
					Constants.OSName
				}, 5000);
		message.setSendAZID(false);
		PlatformMessenger.queueMessage(message, null);
	}

	public static void qosFoundDevice(Device device) {
		if (device == null
				|| !COConfigurationManager.getBooleanParameter(CFG_SEND_QOS, false)) {
			return;
		}
		
		HashMap<String, Object> map = new HashMap<String, Object>();

		addPluginVersionsToMap(map);

		map.put("device-name", getDeviceName(device));
		map.put("device-type", new Integer(device.getType()));
		if (device instanceof DeviceMediaRenderer) {
			DeviceMediaRenderer renderer = (DeviceMediaRenderer) device;
			map.put("renderer-species",
					Integer.valueOf(renderer.getRendererSpecies()));
		}

		PlatformMessage message = new PlatformMessage("AZMSG", LISTENER_ID,
				OP_QOS_FOUND_DEVICE, map, 5000);
		message.setSendAZID(false);
		PlatformMessenger.queueMessage(message, null);
	}

	private static void addPluginVersionsToMap(Map map) {
		if (AzureusCoreFactory.isCoreRunning()) {
  		PluginManager pm = AzureusCoreFactory.getSingleton().getPluginManager();
  		PluginInterface pi;
  		pi = pm.getPluginInterfaceByID("vuzexcode");
  		if (pi != null) {
  			map.put("xcode-plugin-version", pi.getPluginVersion());
  		}
  		pi = pm.getPluginInterfaceByID("azitunes");
  		if (pi != null) {
  			map.put("itunes-plugin-version", pi.getPluginVersion());
  		}
		}
		map.put("os-name", Constants.OSName);
	}

	public static void qosTranscodeRequest(TranscodeTarget transcodeTarget, String sourceRef) {
		if (!COConfigurationManager.getBooleanParameter(CFG_SEND_QOS, false)
				|| transcodeTarget == null) {
			return;
		}

		HashMap<String, Object> map = new HashMap<String, Object>();

		addPluginVersionsToMap(map);

		Device device = transcodeTarget.getDevice();
		if (device != null) { // should never be null..
			map.put("device-name", getDeviceName(device));
			map.put("device-type", new Integer(device.getType()));
		}
		if (transcodeTarget instanceof DeviceMediaRenderer) {
			DeviceMediaRenderer renderer = (DeviceMediaRenderer) transcodeTarget;
			map.put("renderer-species",
					Integer.valueOf(renderer.getRendererSpecies()));
		}
		map.put("source-ref", sourceRef);

		PlatformMessage message = new PlatformMessage("AZMSG", LISTENER_ID,
				OP_QOS_TRANSCODE_REQUEST, map, 5000);
		message.setSendAZID(false);
		PlatformMessenger.queueMessage(message, null);
	}

	private static Object getDeviceName(Device device) {
		String name = device.getName();
		String classification = device.getClassification();
		String deviceName = name.equals(classification) ? name : name + "/" + classification;
		return deviceName;
	}

	public static void qosTranscode(TranscodeJob job, int stateOveride) {
		if (!COConfigurationManager.getBooleanParameter(CFG_SEND_QOS, false)
				|| job == null) {
			return;
		}

		TranscodeFile transcodeFile = job.getTranscodeFile();
		DiskManagerFileInfo sourceFileInfo = null;
		try {
			sourceFileInfo = transcodeFile.getSourceFile();
		} catch (Throwable t) {
		}
		DiskManagerFileInfo targetFileInfo = null;
		try {
			targetFileInfo = transcodeFile.getTargetFile();
		} catch (Throwable t) {
		}
		TranscodeProfile profile = job.getProfile();
		TranscodeTarget target = job.getTarget();
		Device device = target.getDevice();
		
		HashMap<String, Object> map = new HashMap<String, Object>();

		addPluginVersionsToMap(map);

		map.put("job-state", Integer.valueOf(stateOveride));

		if ((stateOveride & 0xff) == TranscodeJob.ST_FAILED) {
			String error = job.getError();
			if (error != null) {
				if (error.endsWith("\r\n")) {
					error.substring(0, error.length() - 2);
				} else if (error.endsWith("\r") || error.endsWith("\n")) {
					error.substring(0, error.length() - 1);
				}
			}
			map.put("job-error", error);
		}
		
		try {
			Torrent torrent = job.getFile().getDownload().getTorrent();
			if (PlatformTorrentUtils.isContent(torrent, true)) {
				map.put("asset-hash", new HashWrapper(torrent.getHash()).toBase32String());
			}
		} catch (Throwable t) {
		}

		map.put("transcode-mode", new Integer(job.getTranscodeRequirement()));
		map.put("transcode-required", new Boolean(transcodeFile.getTranscodeRequired()));

		// These help us determine if the transcode is taking too long
		map.put("transcode-video-width", new Long(transcodeFile.getVideoWidth()));
		map.put("transcode-video-height", new Long(transcodeFile.getVideoHeight()));
		map.put("transcode-video-duration-ms", new Long(transcodeFile.getDurationMillis()));
		map.put("process-time-ms", new Long(job.getProcessTime()));

		// Gotta know which device/profile/renderer we are transcoding to so we
		// know what's should be worked on more
		map.put("device-name", getDeviceName(device));
		map.put("device-type", new Integer(device.getType()));
		if (profile != null) {
			map.put("profile-name", profile.getName());
		}
		
		if (target instanceof DeviceMediaRenderer) {
			DeviceMediaRenderer renderer = (DeviceMediaRenderer) target;
			map.put("renderer-species", Integer.valueOf(renderer.getRendererSpecies()));
		}

		// Don't worry, we don't send the filename, just the extension.  This
		// helps us figure out which file types are failing/succeeding the most
		if (sourceFileInfo != null) {
			map.put("source-file-ext", FileUtil.getExtension(sourceFileInfo.getFile().getName()));
			map.put("source-file-size", new Long(sourceFileInfo.getLength()));
		}
		if (targetFileInfo != null) {
			map.put("target-file-ext", FileUtil.getExtension(targetFileInfo.getFile().getName()));
			map.put("target-file-size", new Long(targetFileInfo.getLength()));
		}
		

		PlatformMessage message = new PlatformMessage("AZMSG", LISTENER_ID,
				OP_QOS_TRANSCODE, map, 5000);
		message.setSendAZID(false);
		PlatformMessenger.queueMessage(message, null);
	}
	
	public static void setupDeviceSender() {
		if ( !COConfigurationManager.getStringParameter("ui").equals("az2")){

			final DeviceManager deviceManager = DeviceManagerFactory.getSingleton();
			Device[] devices = deviceManager.getDevices();
			if (devices == null || devices.length == 0) {
	  		deviceManager.addListener(new DeviceManagerListener() {
	  		
	  			public void deviceRemoved(Device device) {
	  			}
	  		
	  			public void deviceChanged(Device device) {
	  			}
	  		
	  			public void deviceAttentionRequest(Device device) {
	  			}
	  			
	  			public void deviceAdded(Device device) {
	  			}
	  
	  			public void deviceManagerLoaded() {
	  				deviceManager.removeListener(this);
	  				Device[] devices = deviceManager.getDevices();
	  				if (devices != null && devices.length > 0) {
	  					sendDeviceList(devices);
	  				}
	  			}
	  		});
			} else {
				sendDeviceList(devices);
			}
		}
	}

	private static void sendDeviceList(Device[] devices) {
		if (!PlatformConfigMessenger.allowSendDeviceList()) {
			return;
		}
		List<String> listRenderers = new ArrayList<String>(devices.length);
		for (Device dev : devices) {
			if (dev.getType() == Device.DT_MEDIA_RENDERER) {
				listRenderers.add(dev.getClassification());
			}
		}

		if (listRenderers.size() == 0) {
			return;
		}
		PlatformMessage message = new PlatformMessage("AZMSG", LISTENER_ID,
				OP_REPORT_DEVICES, new Object[] {
					"renderers",
					listRenderers
				}, 500);
		PlatformMessenger.queueMessage(message, null);
	}
}
