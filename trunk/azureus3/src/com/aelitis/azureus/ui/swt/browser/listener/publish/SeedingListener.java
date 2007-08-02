package com.aelitis.azureus.ui.swt.browser.listener.publish;

import java.util.*;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.TimeFormatter;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.swt.browser.msg.AbstractMessageListener;
import com.aelitis.azureus.ui.swt.browser.msg.BrowserMessage;
import com.aelitis.azureus.ui.swt.utils.PublishUtils;
import com.aelitis.azureus.util.MapUtils;

public class SeedingListener
	extends AbstractMessageListener
{

	private static final long INFINITE_ETA = 31535999; //1 year

	private static final long STOPPED_ETA = -88;

	private static final long ERROR_ETA = -99;

	private static final String JS_UPLOAD_PROGRESS_MSG_KEY = "upload-progress";

	private static final String JS_INDIVI_UPDATE_MSG_OP = "torrents";

	private static final String JS_GLOBAL_UPDATE_MSG_OP = "global";

	public static final String DEFAULT_LISTENER_ID = "seeding";

	public static final String OP_SEND_UPDATE = "send-update";

	public static final String OP_REMOVE = "remove";

	public static final String OP_STOP = "stop";

	public static final String OP_START = "start";

	public SeedingListener() {
		this(DEFAULT_LISTENER_ID);
	}

	public SeedingListener(String id) {
		super(id);
	}

	public void handleMessage(BrowserMessage message) {
		if (OP_SEND_UPDATE.equals(message.getOperationId())) {
			sendUpdate();
		} else if (OP_REMOVE.equals(message.getOperationId())) {
			String id = MapUtils.getMapString(message.getDecodedMap(), "id", null);
			if (id != null) {
				removeTorrent(id);
			}
		} else if (OP_START.equals(message.getOperationId())) {
			String id = MapUtils.getMapString(message.getDecodedMap(), "id", null);
			if (id != null) {
				startTorrent(id);
			}
		} else if (OP_STOP.equals(message.getOperationId())) {
			String id = MapUtils.getMapString(message.getDecodedMap(), "id", null);
			if (id != null) {
				stopTorrent(id);
			}
		} else {
			throw new IllegalArgumentException("Unknown operation: "
					+ message.getOperationId());
		}
	}

	private DownloadManager getDM(String magnet) {
		AzureusCore core = AzureusCoreFactory.getSingleton();
		return core.getGlobalManager().getDownloadManager(
				new HashWrapper(Base32.decode(magnet)));
	}

	private void removeTorrent(String id) {
		DownloadManager dm = getDM(id);

		if (dm != null) {
			ManagerUtils.asyncStopDelete(dm, DownloadManager.STATE_STOPPED, false,
					false);
		}
	}

	private void startTorrent(String id) {
		DownloadManager dm = getDM(id);

		if (dm != null) {
			try {
				dm.setForceStart(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void stopTorrent(String id) {
		DownloadManager dm = getDM(id);
		stop(dm);
	}

	private void stop(DownloadManager dm) {
		try {
			ManagerUtils.stop(dm, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void sendUpdate() {
		try {
			GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
			if (gm == null) {
				return;
			}
			List dmsArray = gm.getDownloadManagers();
			if (dmsArray == null) {
				return;
			}

			Object[] dms = dmsArray.toArray();

			boolean hasIncompletePublish = false;
			long totSeedingBytes = 0;
			long totSeedingBytesRemaining = 0;
			long total_up_rate_bps = 0;

			int num_actually_uploading = 0;

			ArrayList indiv_torrents = new ArrayList();

			for (int i = dms.length - 1; i >= 0; i--) { //loop through all the running torrents

				DownloadManager d = (DownloadManager) dms[i];

				if (PublishUtils.isPublished(d)) { //this one we're uploading as published

					//TODO use something more explicit than swarm availability / uploaded bytes

					long download_size = d.getTorrent().getSize();

					int percent_done = (int) ((d.getStats().getTotalDataBytesSent() * 100) / download_size);

					if (percent_done > 99) { //100% uploaded
						indiv_torrents.add(new IndividualProgress(d.getTorrent().getHash(),
								d.getDisplayName(), 100, 0));
					} else { //upload still in progress
						hasIncompletePublish = true;

						long eta = INFINITE_ETA; //so it shows infinity						

						if (d.getState() == DownloadManager.STATE_STOPPED) {
							eta = STOPPED_ETA;
						} else if (d.getState() == DownloadManager.STATE_ERROR) {
							eta = ERROR_ETA;
						} else {
							num_actually_uploading++; //running and upload still needed

							totSeedingBytes += download_size;

							long remaining = (long) (download_size * ((float) (100 - percent_done) / 100)); //rough

							totSeedingBytesRemaining += remaining; //global stats

							long up_rate_bps = d.getStats().getDataSendRate();

							if (up_rate_bps > 0) {
								total_up_rate_bps += up_rate_bps; //global stats

								eta = remaining / up_rate_bps; //seconds remaining
							}
						}

						indiv_torrents.add(new IndividualProgress(d.getTorrent().getHash(),
								d.getDisplayName(), percent_done, eta));
					}
				}
			}

			if (!indiv_torrents.isEmpty()) { //there is something to update				

				long g_percent = 100;
				long g_eta = INFINITE_ETA;

				if (totSeedingBytes > 0) { //there is still torrent data to upload
					g_percent = ((totSeedingBytes - totSeedingBytesRemaining) * 100)
							/ totSeedingBytes;

					if (total_up_rate_bps > 0) {
						g_eta = totSeedingBytesRemaining / total_up_rate_bps; //seconds remaining					
					}

					if (num_actually_uploading < 1) { //all in stopped state
						g_eta = STOPPED_ETA;
						g_percent = 0;
					}
				} else if (!hasIncompletePublish) { //done uploading
					g_eta = 0;
				}

				List torrents = new ArrayList();

				for (Iterator it = indiv_torrents.iterator(); it.hasNext();) {
					IndividualProgress ind = (IndividualProgress) it.next();

					long mod_eta = ind.eta;

					if (g_eta > 0 && ind.eta > g_eta) { //for user display purposes, limit indiv eta to max what the global eta is showing
						mod_eta = g_eta;
					}

					torrents.add(constructJSTorrentProgress(ind.infohash, ind.name,
							ind.percent, mod_eta));
				}

				context.sendBrowserMessage(JS_UPLOAD_PROGRESS_MSG_KEY,
						JS_GLOBAL_UPDATE_MSG_OP, constructJSGlobalProgress((int) g_percent,
								g_eta));

				context.sendBrowserMessage(JS_UPLOAD_PROGRESS_MSG_KEY,
						JS_INDIVI_UPDATE_MSG_OP, torrents);
			}

		} catch (Throwable tt) {
			tt.printStackTrace();
		}
	}

	private Map constructJSTorrentProgress(byte[] infohash, String name,
			int percent, long _eta) {
		String hash = infohash == null ? "<null>" : Base32.encode(infohash);
		String eta = formatETA(_eta);

		Map torrent = new HashMap();
		torrent.put("hash", hash);
		torrent.put("name", name);
		torrent.put("percent", new Long(percent));
		torrent.put("eta", eta);

		return torrent;
	}

	private Map constructJSGlobalProgress(int percent, long _eta) {
		String eta = formatETA(_eta);

		Map global = new HashMap();
		global.put("percent", new Long(percent));
		global.put("eta", eta);

		return global;
	}

	private String formatETA(long eta) {
		if (eta == INFINITE_ETA) {
			return "";
		} else if (eta == STOPPED_ETA) {
			return "x";
		} else if (eta == ERROR_ETA) {
			return "e";
		}

		return TimeFormatter.format( eta );
	}

	private static class IndividualProgress
	{
		private final byte[] infohash;

		private final String name;

		private final int percent;

		private final long eta;

		private IndividualProgress(byte[] _infohash, String _name, int _percent,
				long _eta) {
			this.infohash = _infohash;
			this.name = _name;
			this.percent = _percent;
			this.eta = _eta;
		}
	}

}
