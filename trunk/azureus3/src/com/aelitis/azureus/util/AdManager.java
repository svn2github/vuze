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
import java.net.MalformedURLException;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.impl.DownloadManagerAdapter;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerAdapter;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.download.DownloadManagerEnhancer;
import com.aelitis.azureus.core.download.EnhancedDownloadManager;
import com.aelitis.azureus.core.messenger.config.PlatformAdManager;
import com.aelitis.azureus.core.torrent.MetaDataUpdateListener;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.net.magneturi.MagnetURIHandler;

/**
 * @author TuxPaper
 * @created May 31, 2007
 *
 */
public class AdManager
{
	private final static long EXPIRE_ASX = 1000L * 60 * 10; // 10 min

	private final static AdManager instance;

	static {
		instance = new AdManager();
	}

	public static AdManager getInstance() {
		return instance;
	}

	private AzureusCore core;

	private List adsDMList = new ArrayList();

	private List adSupportedDMList = new ArrayList();

	// key = DM, value = Semaphore
	private Map asxInProgress = new HashMap();

	private AEMonitor asxInProgress_mon = new AEMonitor("asxInProgress");

	private Object lastImpressionID;

	protected boolean checkingForAds = false;

	public void intialize(final AzureusCore core) {
		this.core = core;

		ExternalStimulusHandler.addListener(new ExternalStimulusListener() {
			public boolean receive(String name, Map values) {
				if (values == null) {
					return false;
				}

				if (name.equals("adtracker")) {
					processImpression(values);
					return true;
				}

				return false;
			}
		});

		GlobalManager gm = core.getGlobalManager();
		gm.addListener(new GlobalManagerAdapter() {
			public void downloadManagerRemoved(DownloadManager dm) {
				adsDMList.remove(dm);
				File asxFile = (File) dm.getData("ASX");

				if (asxFile != null) {
					try {
						asxFile.delete();
					} catch (Exception e) {
					}
				}
			}

			public void downloadManagerAdded(final DownloadManager dm) {

				TOTorrent torrent = dm.getTorrent();
				if (PlatformTorrentUtils.isContentAdEnabled(torrent)) {
					dm.setData("ASX", buildASXFileLocation(dm));
				}
				hookDM(new DownloadManager[] {
					dm
				});
			}
		}, false);
		DownloadManager[] dms = (DownloadManager[]) gm.getDownloadManagers().toArray(
				new DownloadManager[0]);
		hookDM(dms);

		PlatformAdManager.loadUnsentImpressions();
		PlatformAdManager.sendUnsentImpressions(5000);

		PlatformTorrentUtils.addListener(new MetaDataUpdateListener() {
			public void metaDataUpdated(TOTorrent torrent) {
				GlobalManager gm = core.getGlobalManager();
				DownloadManager dm = gm.getDownloadManager(torrent);
				if (dm != null
						&& PlatformTorrentUtils.isContentAdEnabled(dm.getTorrent())) {
					hookDM(new DownloadManager[] {
						dm
					});
				}
			}
		});
	}

	private void hookDM(final DownloadManager[] dms) {
		AEThread thread = new AEThread("hookDM", true) {
			public void runSupport() {
				// build list of ad supported content
				List list = new ArrayList();
				for (int i = 0; i < dms.length; i++) {
					final DownloadManager dm = dms[i];

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
						if (!adsDMList.contains(dm)) {
							adsDMList.add(dm);
						}
						if (dm.getAssumedComplete()) {
							dm.setForceStart(false);
						} else {
							dm.addListener(new DownloadManagerAdapter() {
								public void downloadComplete(DownloadManager manager) {
									if (!adsDMList.contains(manager)) {
										adsDMList.add(manager);
									}
									manager.setForceStart(false);
									manager.removeListener(this);
								}
							});
						}
					}

					if (PlatformTorrentUtils.isContent(torrent, true)
							&& PlatformTorrentUtils.getContentHash(torrent) != null
							&& PlatformTorrentUtils.isContentAdEnabled(torrent)) {
						list.add(dm);
						if (!adSupportedDMList.contains(dm)) {
							adSupportedDMList.add(dm);
							if (!dm.getAssumedComplete()) {
								dm.addListener(new DownloadManagerAdapter() {
									public void downloadComplete(DownloadManager manager) {
										// good chance we still have internet here, so get/cache
										// the asx
										EnhancedDownloadManager edm = DownloadManagerEnhancer.getSingleton().getEnhancedDownload(
												dm);
										File file;
										if (edm != null) {
											file = edm.getPrimaryFile().getFile(true);
										} else {
											file = new File(dm.getDownloadState().getPrimaryFile());
										}
										String url;
										try {
											url = file.toURL().toString();
										} catch (MalformedURLException e) {
											url = file.getAbsolutePath();
										}
										createASX(dm, url, null);
									}
								});
							}
						}
					}
				}

				if (list.size() == 0) {
					PlatformAdManager.debug("none of the " + dms.length
							+ " new torrent(s) are ad enabled.  skipping ad get.");
					return;
				}

				try {
					checkingForAds = true;
					PlatformAdManager.debug("sending ad request for " + list.size()
							+ " pieces of content.  We already have " + adsDMList.size()
							+ " ads");
					DownloadManager[] dmAdable = (DownloadManager[]) list.toArray(new DownloadManager[0]);

					PlatformAdManager.getAds(dmAdable, 1000,
							new PlatformAdManager.GetAdsDataReplyListener() {
								public void replyReceived(String replyType, Map mapHashes) {
									checkingForAds = false;
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

											TorrentUtils.setFlag(torrent,
													TorrentUtils.TORRENT_FLAG_LOW_NOISE, true);

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
												if (adDM.getAssumedComplete()) {
													adsDMList.add(adDM);
													adDM.setForceStart(false);
												} else {
													adDM.setForceStart(true);
													PlatformAdManager.debug("Force Start " + adDM);
													adDM.addListener(new DownloadManagerAdapter() {
														public void downloadComplete(DownloadManager manager) {
															if (!adsDMList.contains(manager)) {
																adsDMList.add(manager);
															}
															manager.setForceStart(false);
															manager.removeListener(this);
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

									checkingForAds = false;
								}
							});

				} catch (Exception e) {
					checkingForAds = false;
					Debug.out(e);
				}
			}

		};
		thread.run();
	}

	protected void processImpression(Map values) {
		final String PREFIX = "Yum";
		try {
			String contentHash = (String) values.get("contentHash");
			if (contentHash == null) {
				PlatformAdManager.debug("No Content Hash!");
				return;
			}

			String impressionID = (String) values.get("impressionTracker");
			if (impressionID == null || impressionID.equals(lastImpressionID)) {
				return;
			}
			lastImpressionID = impressionID;

			String adHash = (String) values.get("srcURL");
			if (adHash == null) {
				return;
			}

			String torrentHash = (String) values.get("hash");

			DownloadManager dmContent = null;
			if (torrentHash != null) {
				dmContent = core.getGlobalManager().getDownloadManager(
						new HashWrapper(Base32.decode(torrentHash)));
			}

			if (dmContent == null) {
				dmContent = core.getGlobalManager().getDownloadManager(
						new HashWrapper(Base32.decode(contentHash)));
			}
			if (dmContent != null) {
				dmContent.setData("LastASX", null);
			}

			String adID = (String) values.get(PREFIX + "eURI");

			PlatformAdManager.debug("imp " + impressionID + " commencing on "
					+ contentHash);

			DownloadManager dm = core.getGlobalManager().getDownloadManager(
					new HashWrapper(Base32.decode(adHash)));
			if (dm == null) {
				System.err.println("DM for Ad not found. CHEATER!!");
			}

			PlatformAdManager.storeImpresssion(impressionID,
					SystemTime.getCurrentTime(), contentHash, torrentHash, adHash, adID,
					5000);

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

		PlatformAdManager.debug("There are"	+ ads.size() 
				+ (bIncludeIncomplete ? " including incomplete" : ""));
		return (DownloadManager[]) ads.toArray(new DownloadManager[0]);
	}
	
	public List getIncompleteAds() {
		ArrayList ads = new ArrayList(adsDMList);
		for (Iterator iter = ads.iterator(); iter.hasNext();) {
			DownloadManager dm = (DownloadManager) iter.next();
			if (dm.getAssumedComplete()) {
				iter.remove();
			}
		}

		PlatformAdManager.debug("Get Incomplete Ads: " + ads.size());
		return ads;
	}

	public void createASX(final DownloadManager dm, String URLToPlay,
			final ASXCreatedListener l) {
		try {
			TOTorrent torrent = dm.getTorrent();
			if (torrent == null || !PlatformTorrentUtils.isContent(torrent, true)) {
				return;
			}

			File asxFile = null;

			Object lastASXObject = dm.getData("LastASX");
			if (lastASXObject instanceof Long) {
				long lastASX = ((Long) lastASXObject).longValue();
				if (SystemTime.getCurrentTime() - lastASX < EXPIRE_ASX) {
					asxFile = buildASXFileLocation(dm);
					if (asxFile.isFile()) {
						PlatformAdManager.debug("playing using existing asx: " + asxFile
								+ "; expires in "
								+ (EXPIRE_ASX - SystemTime.getCurrentTime() - lastASX));
						if (l != null) {
							l.asxCreated(asxFile);
						}
						return;
					}
				}
			}

			final String contentHash = PlatformTorrentUtils.getContentHash(torrent);
			final String hash = dm.getTorrent().getHashWrapper().toBase32String();

			try {
				asxInProgress_mon.enter();

				AESemaphore sem = (AESemaphore) asxInProgress.get(hash);
				if (sem != null) {
					PlatformAdManager.debug("already getting asx.. waiting..");
					asxInProgress_mon.exit();

					sem.reserve();

					asxInProgress_mon.enter();

					asxFile = buildASXFileLocation(dm);
					if (asxFile.isFile()) {
						PlatformAdManager.debug("playing using existing asx: " + asxFile);
						if (l != null) {
							l.asxCreated(asxFile);
						}
						return;
					}
				} else {
					asxInProgress.put(hash, new AESemaphore(hash));
				}

			} finally {
				asxInProgress_mon.exit();
			}

			final File fasxFile = asxFile;
			PlatformAdManager.debug("getting asx");
			PlatformAdManager.getPlayList(dm, URLToPlay, "http://127.0.0.1:"
					+ MagnetURIHandler.getSingleton().getPort()
					+ "/setinfo?name=adtracker&contentHash=" + contentHash + "&hash="
					+ hash, 0, new PlatformAdManager.GetPlaylistReplyListener() {
				public void replyReceived(String replyType, String playlist) {
					try {
						if (playlist == null) {
							PlatformAdManager.debug("no asx in reply");
							if (l != null) {
								// we might be offline, ignore asx expirey date
								if (fasxFile != null && fasxFile.isFile()) {
									l.asxCreated(fasxFile);
								} else {
									l.asxFailed();
								}
							}
							return;
						}
						File asxFile = buildASXFileLocation(dm);
						PlatformAdManager.debug("got asx. Writing to " + asxFile);
						FileUtil.writeBytesAsFile(asxFile.getAbsolutePath(),
								playlist.getBytes());

						dm.setData("LastASX", new Long(SystemTime.getCurrentTime()));
						dm.setData("ASX", asxFile);

						if (l != null) {
							l.asxCreated(asxFile);
						}
					} catch (Exception e) {
						PlatformAdManager.debug("asx reply", e);
						if (l != null) {
							l.asxFailed();
						}
					} finally {
						try {
							asxInProgress_mon.enter();

							AESemaphore sem = (AESemaphore) asxInProgress.remove(hash);
							if (sem != null) {
								sem.releaseForever();
							} else {
							}

						} finally {
							asxInProgress_mon.exit();
						}
					}
				}

				public void messageSent() {
				}
			});
		} catch (Exception e) {
			if (l != null) {
				l.asxFailed();
			}
		}
	}

	private File buildASXFileLocation(DownloadManager dm) {
		EnhancedDownloadManager edm = DownloadManagerEnhancer.getSingleton().getEnhancedDownload(
				dm);
		File file;
		if (edm != null) {
			file = edm.getPrimaryFile().getFile(true);
		} else {
			file = new File(dm.getDownloadState().getPrimaryFile());
		}
		return new File(file.getAbsolutePath() + ".asx");
	}

	public interface ASXCreatedListener
	{
		public void asxCreated(File asxFile);

		public void asxFailed();
	}

	/**
	 * @param nowPlaying
	 *
	 * @since 3.0.2.3
	 */
	public boolean isAd(String nowPlaying) {
		if (nowPlaying == null) {
			return false;
		}
		File file = new File(nowPlaying);
		DownloadManager[] ads = getAds(true);
		for (int i = 0; i < ads.length; i++) {
			DownloadManager downloadManager = ads[i];
			DiskManagerFileInfo[] fileInfos = downloadManager.getDiskManagerFileInfo();
			for (int j = 0; j < fileInfos.length; j++) {
				DiskManagerFileInfo fileinfo = fileInfos[j];
				File adFile = fileinfo.getFile(true);
				if (adFile.equals(file)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean isCheckingForNewAds() {
		return checkingForAds;
	}
}
