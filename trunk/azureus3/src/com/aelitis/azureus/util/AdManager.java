/**
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.util;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.impl.DownloadManagerAdapter;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerAdapter;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.messenger.config.PlatformAdManager;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.net.magneturi.MagnetURIHandler;

/**
 * @author TuxPaper
 * @created May 31, 2007
 *
 */
public class AdManager
{
	private final static AdManager instance;

	static {
		instance = new AdManager();
	}

	public static AdManager getInstance() {
		return instance;
	}

	private AzureusCore core;

	private List adsDMList = new ArrayList();

	private Object lastSpyID;

	public void intialize(final AzureusCore core) {
		this.core = core;

		ExternalStimulusHandler.addListener(new ExternalStimulusListener() {
			public boolean receive(String name, Map values) {
				if (values == null) {
					return false;
				}

				if (name.equals("adtracker")) {
					spyOnYou(values);
					return true;
				}

				return false;
			}
		});

		GlobalManager gm = core.getGlobalManager();
		gm.addListener(new GlobalManagerAdapter() {
			public void downloadManagerRemoved(DownloadManager dm) {
				adsDMList.remove(dm);
			}

			public void downloadManagerAdded(final DownloadManager dm) {

				TOTorrent torrent = dm.getTorrent();
				if (PlatformTorrentUtils.getAdId(torrent) == null) {
					hookDM(new DownloadManager[] {
						dm
					});
				}
			}
		}, false);
		DownloadManager[] dms = (DownloadManager[]) gm.getDownloadManagers().toArray(
				new DownloadManager[0]);
		hookDM(dms);

		PlatformAdManager.loadUnsentImpressions();
		PlatformAdManager.sendUnsentImpressions(5000);
	}

	private void hookDM(final DownloadManager[] dms) {
		AEThread thread = new AEThread("hookDM", true) {
			public void runSupport() {
				// build list of ad supported content
				List list = new ArrayList();
				for (int i = 0; i < dms.length; i++) {
					DownloadManager dm = dms[i];

					TOTorrent torrent = dm.getTorrent();
					if (torrent == null) {
						return;
					}
					if (PlatformTorrentUtils.getAdId(torrent) != null) {
						// one of us!

						// TODO: Check expirey
						try {
							PlatformAdManager.debug("found ad " + dm + ": "
									+ PlatformTorrentUtils.getAdId(torrent) + ": "
									+ dm.getTorrent().getHashWrapper().toBase32String());
						} catch (TOTorrentException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						adsDMList.add(dm);
					}

					if (PlatformTorrentUtils.isContent(torrent)
							&& PlatformTorrentUtils.getContentHash(torrent) != null
							&& PlatformTorrentUtils.isContentAdEnabled(torrent)) {
						list.add(dm);
					}
				}

				if (list.size() == 0) {
					PlatformAdManager.debug("no ad enabled content.  skipping ad get.");
					return;
				}

				try {
					PlatformAdManager.debug("sending ad request for " + list.size()
							+ " pieces of content.  We already have " + adsDMList.size()
							+ " ads");
					DownloadManager[] dmAdable = (DownloadManager[]) list.toArray(new DownloadManager[0]);

					PlatformAdManager.getAds(dmAdable, 1000,
							new PlatformAdManager.GetAdsDataReplyListener() {
								public void replyReceived(String replyType, Map mapHashes) {
									PlatformAdManager.debug("bad reply. " + mapHashes.get("text"));
								}

								public void messageSent() {
								}

								public void adsReceived(List torrents) {
									PlatformAdManager.debug(torrents.size() + " Ads recieved");
									for (Iterator iter = torrents.iterator(); iter.hasNext();) {
										TOTorrent torrent = (TOTorrent) iter.next();
										try {
											PlatformAdManager.debug("Ad: "
													+ new String(torrent.getName()));
											File tempFile = File.createTempFile("AZ_", ".torrent");

											PlatformAdManager.debug("  Writing to " + tempFile);
											torrent.serialiseToBEncodedFile(tempFile);

											String sDefDir = null;
											try {
												sDefDir = COConfigurationManager.getDirectoryParameter("Default save path");
											} catch (IOException e) {
											}

											if (sDefDir == null) {
												sDefDir = tempFile.getParent();
											}

											DownloadManager adDM = core.getGlobalManager().addDownloadManager(
													tempFile.getAbsolutePath(), sDefDir);

											if (adDM != null) {
												adDM.setForceStart(true);
												if (adDM.getAssumedComplete()) {
													adsDMList.add(adDM);
												} else {
													adDM.addListener(new DownloadManagerAdapter() {
														public void downloadComplete(DownloadManager manager) {
															adsDMList.add(manager);
														}
													});
												}
												// TODO: Add Expiry date
												PlatformAdManager.debug("  ADDED ad "
														+ adDM.getDisplayName());
											}
											tempFile.deleteOnExit();
										} catch (Exception e) {
											Debug.out(e);
										}

									}

								}
							});

				} catch (Exception e) {
					Debug.out(e);
				}
			}

		};
		thread.run();
	}

	protected void spyOnYou(Map values) {
		try {
			String spyID = (String) values.get("impressionTracker");
			if (spyID == null || spyID.equals(lastSpyID)) {
				return;
			}
			lastSpyID = spyID;

			String adHash = (String) values.get("srcURL");
			if (adHash == null) {
				return;
			}

			String contentHash = (String) values.get("contentHash");
			if (contentHash == null) {
				PlatformAdManager.debug("No Content Hash!");
				return;
			}

			PlatformAdManager.debug("spy " + spyID + " commencing on " + contentHash);

			DownloadManager dm = core.getGlobalManager().getDownloadManager(
					new HashWrapper(Base32.decode(adHash)));
			if (dm == null) {
				System.err.println("DM for Ad not found. CHEATER!!");
			}

			PlatformAdManager.storeImpresssion(spyID, SystemTime.getCurrentTime(),
					contentHash, 5000);

		} catch (Exception e) {
			Debug.out(e);
		}
	}

	public DownloadManager[] getAds(boolean bIncludeIncomplete) {
		if (bIncludeIncomplete) {
			return (DownloadManager[]) adsDMList.toArray(new DownloadManager[0]);
		}

		ArrayList ads = new ArrayList(adsDMList);
		for (Iterator iter = ads.iterator(); iter.hasNext();) {
			DownloadManager dm = (DownloadManager) iter.next();
			if (!dm.getAssumedComplete()) {
				iter.remove();
			}
		}

		PlatformAdManager.debug("Get Ads"
				+ (bIncludeIncomplete ? " including incomplete" : "") + ads.size());
		return (DownloadManager[]) ads.toArray(new DownloadManager[0]);
	}

	public void createASX(final DownloadManager dm, final ASXCreatedListener l) {
		TOTorrent torrent = dm.getTorrent();
		if (torrent == null || !PlatformTorrentUtils.isContent(torrent)) {
			return;
		}
		String contentHash = PlatformTorrentUtils.getContentHash(torrent);

		PlatformAdManager.getPlayList(dm, "http://127.0.0.1:"
				+ MagnetURIHandler.getSingleton().getPort()
				+ "/setinfo?name=adtracker&contentHash=" + contentHash, 0,
				new PlatformAdManager.GetPlaylistReplyListener() {
					public void replyReceived(String replyType, String playlist) {
						if (playlist == null) {
							l.asxFailed();
							return;
						}
						File saveLocation = dm.getAbsoluteSaveLocation();
						File asxFile = new File(saveLocation.isFile()
								? saveLocation.getParentFile() : saveLocation, "play.asx");
						FileUtil.writeBytesAsFile(asxFile.getAbsolutePath(),
								playlist.getBytes());

						l.asxCreated(asxFile);
					}

					public void messageSent() {
					}
				});
	}

	public interface ASXCreatedListener
	{
		public void asxCreated(File asxFile);

		public void asxFailed();
	}
}
