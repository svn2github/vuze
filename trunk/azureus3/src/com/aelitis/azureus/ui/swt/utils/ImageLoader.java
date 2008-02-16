/*
 * Created on Jun 7, 2006 2:31:26 PM
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package com.aelitis.azureus.ui.swt.utils;

import java.io.InputStream;
import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

import com.aelitis.azureus.ui.skin.SkinProperties;

/**
 * @author TuxPaper
 * @created Jun 7, 2006
 *
 */
public class ImageLoader
{

	private static final boolean DEBUG_UNLOAD = false;

	private Display display;

	public static Image noImage;

	private final Map mapImages;

	private final ArrayList notFound;

	private SkinProperties skinProperties;

	private final ClassLoader classLoader;

	public ImageLoader(ClassLoader classLoader, Display display,
			SkinProperties skinProperties) {
		this.classLoader = classLoader;
		mapImages = new HashMap();
		notFound = new ArrayList();
		this.display = display;
		this.skinProperties = skinProperties;
	}

	private Image loadImage(Display display, String key) {
		return loadImage(display, skinProperties.getStringValue(key), key);
	}

	private Image[] findResources(String sKey) {
		if (Collections.binarySearch(notFound, sKey) >= 0) {
			return null;
		}

		String[] sSuffixChecks = {
			"-over",
			"-down",
			"-disabled",
		};

		for (int i = 0; i < sSuffixChecks.length; i++) {
			String sSuffix = sSuffixChecks[i];

			if (sKey.endsWith(sSuffix)) {
				//System.out.println("YAY " + sSuffix + " for " + sKey);
				String sParentName = sKey.substring(0, sKey.length() - sSuffix.length());
				String[] sParentFiles = skinProperties.getStringArray(sParentName);
				if (sParentFiles != null) {
					boolean bFoundOne = false;
					Image[] images = new Image[sParentFiles.length];

					for (int j = 0; j < sParentFiles.length; j++) {
						int index = sParentFiles[j].lastIndexOf('.');
						if (index > 0) {
							String sTryFile = sParentFiles[j].substring(0, index) + sSuffix
									+ sParentFiles[j].substring(index);
							images[j] = loadImage(display, sTryFile, sKey);

							if (images[j] == null) {
								sTryFile = sParentFiles[j].substring(0, index)
										+ sSuffix.replace('-', '_')
										+ sParentFiles[j].substring(index);
								images[j] = loadImage(display, sTryFile, sKey);
							}

							if (!bFoundOne && images[j] != null) {
								bFoundOne = true;
							}
						}
					}

					if (bFoundOne) {
						return images;
					}
				}
			}
		}

		int i = Collections.binarySearch(notFound, sKey) * -1 - 1;
		notFound.add(i, sKey);
		return null;
	}

	private Image loadImage(Display display, String res, String sKey) {
		Image img = null;

		//System.out.println("LoadImage " + sKey + " - " + res);
		if (res == null) {
			String[] sSuffixChecks = {
				"-over",
				"-down",
				"-disabled",
			};

			for (int i = 0; i < sSuffixChecks.length; i++) {
				String sSuffix = sSuffixChecks[i];

				if (sKey.endsWith(sSuffix)) {
					//System.out.println("Yay " + sSuffix + " for " + sKey);
					String sParentName = sKey.substring(0, sKey.length()
							- sSuffix.length());
					String sParentFile = skinProperties.getStringValue(sParentName);
					if (sParentFile != null) {
						int index = sParentFile.lastIndexOf('.');
						if (index > 0) {
							String sTryFile = sParentFile.substring(0, index) + sSuffix
									+ sParentFile.substring(index);
							img = loadImage(display, sTryFile, sKey);

							if (img != null) {
								break;
							}

							sTryFile = sParentFile.substring(0, index)
									+ sSuffix.replace('-', '_') + sParentFile.substring(index);
							img = loadImage(display, sTryFile, sKey);

							if (img != null) {
								break;
							}
						}
					}
				}
			}
		}

		if (img == null) {
			try {
				InputStream is = classLoader.getResourceAsStream(res);
				if (is != null) {
					img = new Image(display, is);
				}

				if (img == null) {
					if (sKey.endsWith("-disabled") || sKey.endsWith("_disabled")) {
						Image imgToFade = getImage(sKey.substring(0, sKey.length() - 9));
						if (isRealImage(imgToFade)) {
							ImageData imageData = imgToFade.getImageData();
							// decrease alpha
							if (imageData.alphaData != null) {
								for (int i = 0; i < imageData.alphaData.length; i++) {
									imageData.alphaData[i] = (byte) ((imageData.alphaData[i] & 0xff) >> 3);
								}
								img = new Image(display, imageData);
							}
						}
					}
					//System.err.println("ImageRepository:loadImage:: Resource not found: " + res);
				}
			} catch (Throwable e) {
				System.err.println("ImageRepository:loadImage:: Resource not found: "
						+ res + "\n" + e);
			}
		}

		return img;
	}

	public void unLoadImages() {
		Iterator iter;
		if (DEBUG_UNLOAD) {
			iter = mapImages.keySet().iterator();
			while (iter.hasNext()) {
				Object key = iter.next();
				Image[] images = (Image[]) mapImages.get(key);
				if (images != null) {
					for (int i = 0; i < images.length; i++) {
						Image image = images[i];
						if (image != null && !image.isDisposed()) {
							System.out.println("dispose " + image + ";" + key);
							image.dispose();
						}
					}
				}
			}
		} else {
			iter = mapImages.values().iterator();
			while (iter.hasNext()) {
				Image[] images = (Image[]) iter.next();
				if (images != null) {
					for (int i = 0; i < images.length; i++) {
						Image image = images[i];
						if (image != null && !image.isDisposed()) {
							image.dispose();
						}
					}
				}
			}
		}
	}

	public Image[] getImages(String sKey) {
		if (sKey == null) {
			return new Image[] {
				getNoImage()
			};
		}

		Image[] images = (Image[]) mapImages.get(sKey);

		if (images != null) {
			return images;
		}
		String[] locations = skinProperties.getStringArray(sKey);
		//		System.out.println(sKey + "=" + properties.getStringValue(sKey)
		//				+ ";" + ((locations == null) ? "null" : "" + locations.length));
		if (locations == null || locations.length == 0) {
			images = findResources(sKey);

			if (images == null) {
				return new Image[] {
					getNoImage()
				};
			}

			for (int i = 0; i < images.length; i++) {
				if (images[i] == null) {
					images[i] = getNoImage();
				}
			}
		} else {
			images = new Image[locations.length];
			for (int i = 0; i < locations.length; i++) {
				images[i] = loadImage(display, locations[i], sKey);
				if (images[i] == null) {
					images[i] = getNoImage();
				}
			}
		}

		mapImages.put(sKey, images);

		return images;
	}

	public Image getImage(String sKey) {
		Image[] images = getImages(sKey);
		if (images == null || images.length == 0) {
			return null;
		}
		return images[0];
	}

	private static Image getNoImage() {
		if (noImage == null) {
			Display display = Display.getDefault();
			final int SIZE = 10;
			noImage = new Image(display, SIZE, SIZE);
			GC gc = new GC(noImage);
			gc.setBackground(display.getSystemColor(SWT.COLOR_YELLOW));
			gc.fillRectangle(0, 0, SIZE, SIZE);
			gc.setBackground(display.getSystemColor(SWT.COLOR_RED));
			gc.drawRectangle(0, 0, SIZE - 1, SIZE - 1);
			gc.dispose();
		}
		return noImage;
	}

	public boolean imageExists(String name) {
		return isRealImage(getImage(name));
	}

	public static boolean isRealImage(Image image) {
		return image != null && image != getNoImage() && !image.isDisposed();
	}
}
