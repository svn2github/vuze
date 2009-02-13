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

package com.aelitis.azureus.ui.swt.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerAdapter;
import org.gudy.azureus2.core3.global.GlobalManagerListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloader;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderCallBackInterface;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.selectedcontent.DownloadUrlInfo;
import com.aelitis.azureus.ui.selectedcontent.DownloadUrlInfoContentNetwork;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.browser.listener.DownloadUrlInfoSWT;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader.ImageDownloaderListener;
import com.aelitis.azureus.ui.swt.views.skin.TorrentListViewsUtils;
import com.aelitis.azureus.util.*;

/**
 * @author TuxPaper
 * @created Sep 16, 2007
 *
 */
public class TorrentUIUtilsV3
{
	private final static String MSG_ALREADY_EXISTS = "OpenTorrentWindow.mb.alreadyExists";

	private final static String MSG_ALREADY_EXISTS_NAME = MSG_ALREADY_EXISTS
			+ ".default.name";

	//catches http://www.vuze.com/download/CHJW43PLS277RC7U3S5XRS2PZ4UUG7RS.torrent
	private static final Pattern hashPattern = Pattern.compile("download/([A-Z0-9]{32})\\.torrent");

	public static void loadTorrent(final AzureusCore core,
			final DownloadUrlInfo dlInfo, final boolean playNow, // open player
			final boolean playPrepare, // as for open player but don't actually open it
			final boolean bringToFront, final boolean forceDRMtoCDP) {
		if (dlInfo instanceof DownloadUrlInfoSWT) {
			DownloadUrlInfoSWT dlInfoSWT = (DownloadUrlInfoSWT) dlInfo;
			dlInfoSWT.invoke(playNow ? "play" : "download");
			return;
		}

		String url = dlInfo.getDownloadURL();
		try {
			Matcher m = hashPattern.matcher(url);
			if (m.find()) {
				String hash = m.group(1);
				GlobalManager gm = core.getGlobalManager();
				final DownloadManager dm = gm.getDownloadManager(new HashWrapper(
						Base32.decode(hash)));
				if (dm != null) {
					if (playNow || playPrepare) {
						new AEThread2("playExisting", true) {

							public void run() {
								if (playNow) {
									Debug.outNoStack("loadTorrent already exists.. playing",
											false);

									TorrentListViewsUtils.playOrStream(dm);
								} else {
									Debug.outNoStack("loadTorrent already exists.. preparing",
											false);

									PlayUtils.prepareForPlay(dm);
								}
							}

						}.start();
					} else {
						Utils.openMessageBox(Utils.findAnyShell(), SWT.OK,
								MSG_ALREADY_EXISTS, new String[] {
									" ",
									dm.getDisplayName(),
									MessageText.getString(MSG_ALREADY_EXISTS_NAME),
								});
					}
					return;
				}
			}

			// If it's going to our URLs, add some extra authenication
			if (UrlFilter.getInstance().urlCanRPC(url)) {
				ContentNetwork cn = null;
				if (dlInfo instanceof DownloadUrlInfoContentNetwork) {
					cn = ((DownloadUrlInfoContentNetwork) dlInfo).getContentNetwork();
				}
				if (cn == null) {
					cn = ConstantsVuze.getDefaultContentNetwork();
				}
				url = cn.appendURLSuffix(url, false, true);
			}

			UIFunctionsSWT uiFunctions = (UIFunctionsSWT) UIFunctionsManager.getUIFunctions();
			if (uiFunctions != null) {
				if (!COConfigurationManager.getBooleanParameter("add_torrents_silently")) {
					if (bringToFront) {
						uiFunctions.bringToFront();
					}
				}

				Shell shell = uiFunctions.getMainShell();
				if (shell != null) {
					new FileDownloadWindow(core, shell, url, dlInfo.getReferer(),
							dlInfo.getRequestProperties(),
							new TorrentDownloaderCallBackInterface() {

								public void TorrentDownloaderEvent(int state,
										TorrentDownloader inf) {
									if (state == TorrentDownloader.STATE_FINISHED) {

										File file = inf.getFile();
										file.deleteOnExit();

										// Do a quick check to see if it's a torrent
										if (!TorrentUtil.isFileTorrent(file, null, file.getName())) {
											Matcher m = hashPattern.matcher(inf.getURL());
											if (m.find()) {
												String hash = m.group(1);

												ContentNetwork cn = null;
												if (dlInfo instanceof DownloadUrlInfoContentNetwork) {
													cn = ((DownloadUrlInfoContentNetwork) dlInfo).getContentNetwork();
												}
												if (cn == null) {
													cn = ConstantsVuze.getDefaultContentNetwork();
												}
												TorrentListViewsUtils.viewDetails(cn, hash,
														"loadtorrent");
											} else {
												TorrentUtil.isFileTorrent(file, Utils.findAnyShell(),
														file.getName());
											}

											return;
										}

										TOTorrent torrent;
										try {
											torrent = TorrentUtils.readFromFile(file, false);
										} catch (TOTorrentException e) {
											Debug.out(e);
											return;
										}
										// Security: Only allow torrents from whitelisted trackers
										if (playNow
												&& !PlatformTorrentUtils.isPlatformTracker(torrent)) {
											Debug.out("stopped loading torrent because it's not in whitelist");
											return;
										}

										HashWrapper hw;
										try {
											hw = torrent.getHashWrapper();
										} catch (TOTorrentException e1) {
											Debug.out(e1);
											return;
										}

										if (forceDRMtoCDP
												&& (PlatformTorrentUtils.isContentDRM(torrent) || PlatformTorrentUtils.isContentPurchased(torrent))) {
											TorrentListViewsUtils.viewDetailsFromDS(torrent,
													"loadtorrent");
											return;
										}

										GlobalManager gm = core.getGlobalManager();

										if (playNow || playPrepare) {
											DownloadManager existingDM = gm.getDownloadManager(hw);
											if (existingDM != null) {
												if (playNow) {
													TorrentListViewsUtils.playOrStream(existingDM);
												} else {
													PlayUtils.prepareForPlay(existingDM);
												}
												return;
											}
										}

										final HashWrapper fhw = hw;

										GlobalManagerListener l = new GlobalManagerAdapter() {
											public void downloadManagerAdded(DownloadManager dm) {

												try {
													core.getGlobalManager().removeListener(this);

													handleDMAdded(dm, playNow, playPrepare, fhw);
												} catch (Exception e) {
													Debug.out(e);
												}
											}

										};
										gm.addListener(l, false);

										if (playNow || playPrepare) {
											PlayNowList.add(hw);
										}

										TorrentOpener.openTorrent(file.getAbsolutePath());
									}
								}
							});
				}
			}
		} catch (Exception e) {
			Debug.out(e);
		}
	}

	private static void handleDMAdded(final DownloadManager dm,
			final boolean playNow, final boolean playPrepare, final HashWrapper fhw) {
		new AEThread2("playDM", true) {
			public void run() {
				try {
					HashWrapper hw = dm.getTorrent().getHashWrapper();
					if (!hw.equals(fhw)) {
						return;
					}

					boolean showHomeHint = true;
					if (playNow || playPrepare) {
						if (playNow) {
							showHomeHint = !TorrentListViewsUtils.playOrStream(dm);
						} else {
							PlayUtils.prepareForPlay(dm);
							showHomeHint = false;
						}
					}
					if (showHomeHint) {
						TorrentListViewsUtils.showHomeHint(dm);
					}
				} catch (Exception e) {
					Debug.out(e);
				}
			}
		}.start();
	}

	/**
	 * No clue if we have a easy way to add a TOTorrent to the GM, so here it is
	 * @param torrent
	 * @return
	 *
	 * @since 3.0.5.3
	 */
	public static boolean addTorrentToGM(TOTorrent torrent) {
		File tempTorrentFile;
		try {
			tempTorrentFile = File.createTempFile("AZU", ".torrent");
			tempTorrentFile.deleteOnExit();
			String filename = tempTorrentFile.getAbsolutePath();
			torrent.serialiseToBEncodedFile(tempTorrentFile);

			String savePath = COConfigurationManager.getStringParameter("Default save path");
			if (savePath == null || savePath.length() == 0) {
				savePath = ".";
			}

			AzureusCore core = AzureusCoreFactory.getSingleton();
			core.getGlobalManager().addDownloadManager(filename, savePath);
		} catch (Throwable t) {
			Debug.out(t);
			return false;
		}
		return true;
	}

	/**
	 * Retrieves the thumbnail for the content, pulling it from the web if
	 * it can
	 * 
	 * @param datasource
	 * @param l When the thumbnail is available, this listener is triggered
	 * @return If the image is immediately available, the image will be returned
	 *         as well as the trigger being fired.  If the image isn't available
	 *         null will be returned and the listener will trigger when avail
	 *
	 * @since 4.0.0.5
	 */
	public static Image getContentImage(Object datasource, boolean big,
			final ContentImageLoadedListener l) {
		if (l == null) {
			return null;
		}
		TOTorrent torrent = DataSourceUtils.getTorrent(datasource);
		if (torrent == null) {
			l.contentImageLoaded(null, true);
			return null;
		}

		ImageLoader imageLoader = ImageLoader.getInstance();

		String thumbnailUrl = PlatformTorrentUtils.getContentThumbnailUrl(torrent);

		if (thumbnailUrl != null && imageLoader.imageExists(thumbnailUrl)) {
			l.contentImageLoaded(imageLoader.getImage(thumbnailUrl), true);
			return null;
		}

		String hash = null;
		try {
			hash = torrent.getHashWrapper().toBase32String();
		} catch (TOTorrentException e) {
		}
		if (hash == null) {
			l.contentImageLoaded(null, true);
			return null;
		}

		String id = "Thumbnail." + hash;

		Image image = imageLoader.imageAdded(id) ? imageLoader.getImage(id) : null;
		if (image != null && !image.isDisposed()) {
			l.contentImageLoaded(image, true);
			return image;
		}

		final byte[] imageBytes = PlatformTorrentUtils.getContentThumbnail(torrent);
		if (imageBytes != null) {
			image = (Image) Utils.execSWTThreadWithObject("thumbcreator",
					new AERunnableObject() {
						public Object runSupport() {

							ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
							Image image = new Image(Display.getDefault(), bis);

							return image;
						}
					}, 500);
		}
		if ((image == null || image.isDisposed()) && thumbnailUrl != null) {
			image = imageLoader.getUrlImage(thumbnailUrl,
					new ImageDownloaderListener() {
						public void imageDownloaded(Image image, boolean returnedImmediately) {
							l.contentImageLoaded(image, returnedImmediately);
						}
					});
			return image;
		}
		if (image == null || image.isDisposed()) {
			DownloadManager dm = DataSourceUtils.getDM(datasource);
			/*
			 * Try to get an image from the OS
			 */

			String path = null;
			if (dm == null) {
				if (torrent != null) {
					TOTorrentFile[] files = torrent.getFiles();
					if (files.length > 0) {
						path = files[0].getRelativePath();
					}
				}
			} else {
				path = dm.getDownloadState().getPrimaryFile();
			}
			if (path != null) {
				// Don't ever dispose of PathIcon, it's cached and may be used elsewhere
				Image icon = ImageRepository.getPathIcon(path, big, torrent != null
						&& !torrent.isSimpleTorrent());
				if (icon != null) {
					image = new Image(Display.getDefault(), icon, SWT.IMAGE_COPY);
				}
			}

			if (image == null) {
				imageLoader.addImage(id, ImageLoader.noImage);
			} else {
				imageLoader.addImage(id, image);
			}
		} else {
			imageLoader.addImage(id, image);
		}

		l.contentImageLoaded(image, true);
		return image;
	}

	public static void releaseContentImage(Object datasource) {
		TOTorrent torrent = DataSourceUtils.getTorrent(datasource);
		if (torrent == null) {
			return;
		}

		ImageLoader imageLoader = ImageLoader.getInstance();

		String thumbnailUrl = PlatformTorrentUtils.getContentThumbnailUrl(torrent);

		if (thumbnailUrl != null) {
			imageLoader.releaseImage(thumbnailUrl);
		} else {
			String hash = null;
			try {
				hash = torrent.getHashWrapper().toBase32String();
			} catch (TOTorrentException e) {
			}
			if (hash == null) {
				return;
			}

			String id = "Thumbnail." + hash;
			imageLoader.releaseImage(id);
		}
	}

	public static interface ContentImageLoadedListener
	{
		/**
		 * @param image
		 * @param wasReturned  Image was also returned from getContentImage 
		 *
		 * @since 4.0.0.5
		 */
		public void contentImageLoaded(Image image, boolean wasReturned);
	}
}
