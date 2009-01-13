/**
 * Created on Dec 2, 2008
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

package com.aelitis.azureus.ui.swt.utils;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.cnetwork.ContentNetworkManagerFactory;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.ui.utils.ImageBytesDownloader;
import com.aelitis.azureus.ui.utils.ImageBytesDownloader.ImageDownloaderListener;
import com.aelitis.azureus.util.ContentNetworkUtils;

/**
 * @author TuxPaper
 * @created Dec 2, 2008
 *
 */
public class ContentNetworkUI
{
	// If we ever clear mapImages, don't forget to ImageLoderFactory.releaseImage if needed
	public static Map<Long, Image> mapImages = new HashMap();

	/**
	 * @param cn
	 * @param cnImageLoadedListener
	 *
	 * @since 4.0.0.5
	 */
	public static Image loadImage(final long contentNetworkID,
			final ContentNetworkImageLoadedListener cnImageLoadedListener) {
		Image image = mapImages.get(new Long(contentNetworkID));
		if (image != null && cnImageLoadedListener != null) {
			cnImageLoadedListener.contentNetworkImageLoaded(contentNetworkID, image, true);
			return image;
		}

		ContentNetwork cn = ContentNetworkManagerFactory.getSingleton().getContentNetwork(
				contentNetworkID);
		if (cn == null) {
			return image;
		}
		String imgURL = ContentNetworkUtils.getUrl(cn, ContentNetwork.SERVICE_GET_ICON);
		if (imgURL != null) {
			final File cache = new File(SystemProperties.getUserPath(), "cache"
					+ File.separator + imgURL.hashCode() + ".ico");
			boolean loadImage = true;
			if (cache.exists()) {
				try {
					FileInputStream fis = new FileInputStream(cache);

					try {
						byte[] imageBytes = FileUtil.readInputStreamAsByteArray(fis);
						InputStream is = new ByteArrayInputStream(imageBytes);
						image = new Image(Display.getCurrent(), is);
						try {
							is.close();
						} catch (IOException e) {
						}
						mapImages.put(new Long(contentNetworkID), image);
						if (cnImageLoadedListener != null) {
							cnImageLoadedListener.contentNetworkImageLoaded(contentNetworkID,
									image, true);
						}
					} finally {
						fis.close();
					}
					loadImage = false;
				} catch (Throwable e) {
					Debug.printStackTrace(e);
				}

			}
			if (loadImage) {
				ImageBytesDownloader.loadImage(imgURL,
						new ImageBytesDownloader.ImageDownloaderListener() {
							public void imageDownloaded(final byte[] imageBytes) {
								Utils.execSWTThread(new AERunnable() {
									public void runSupport() {
										FileUtil.writeBytesAsFile(cache.getAbsolutePath(),
												imageBytes);
										InputStream is = new ByteArrayInputStream(imageBytes);
										Image image = new Image(Display.getCurrent(), is);
										try {
											is.close();
										} catch (IOException e) {
										}
										mapImages.put(new Long(contentNetworkID), image);
										if (cnImageLoadedListener != null) {
											cnImageLoadedListener.contentNetworkImageLoaded(
													contentNetworkID, image, false);
										}
									}
								});
							}
						});
			}
		} else if (contentNetworkID == ContentNetwork.CONTENT_NETWORK_VUZE
				&& cnImageLoadedListener != null) {
			image = ImageLoader.getInstance().getImage("image.sidebar.vuze");
			mapImages.put(new Long(contentNetworkID), image);
			cnImageLoadedListener.contentNetworkImageLoaded(contentNetworkID, image, true);
		}
		return image;
	}
	
	

	public static interface ContentNetworkImageLoadedListener
	{
		public void contentNetworkImageLoaded(Long contentNetworkID, Image image, boolean wasReturned);
	}

}
