/**
 * Created on Apr 28, 2008
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
 
package com.aelitis.azureus.util;

import java.io.InputStream;
import java.net.URL;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader.ResourceDownloaderFactoryImpl;

import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderAdapter;

/**
 * @author TuxPaper
 * @created Apr 28, 2008
 *
 */
public class ImageDownloader
{
	public static void loadImage(String url, final ImageDownloaderListener l) {
		try {
			ResourceDownloader rd = ResourceDownloaderFactoryImpl.getSingleton().create(
					new URL(url));
			rd.addListener(new ResourceDownloaderAdapter() {
				public boolean completed(ResourceDownloader downloader, InputStream is) {
					try {
						if (is != null && is.available() > 0) {
							byte[] newImageBytes = new byte[is.available()];
							is.read(newImageBytes);
							if (l != null) {
								l.imageDownloaded(newImageBytes);
							}
						}
						return true;
					} catch (Exception e) {
						Debug.out(e);
					}
					return false;
				}
			});
			rd.asyncDownload();
		} catch (Exception e) {
			Debug.out(e);
		}
	}
	
	public static interface ImageDownloaderListener {
		public void imageDownloaded(byte[] image);
	}
}
