/*
 * Created on Jan 1, 2009 3:17:06 PM
 * Copyright (C) 2009 Aelitis, All Rights Reserved.
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
package com.aelitis.azureus.ui.swt.imageloader;

import org.eclipse.swt.graphics.Image;

/**
 * @author TuxPaper
 * @created Jan 1, 2009
 *
 */
public class ImageLoaderRefInfo
{
	private Image[] images;

	// -2: non-disposable; -1: someone over unref'd; 0: no refs (dispose!)
	private long refcount;

	public ImageLoaderRefInfo(Image[] images) {
		this.images = images;
		refcount = 1;
	}

	public ImageLoaderRefInfo(Image image) {
		this.images = new Image[] {
			image
		};
		refcount = 1;
	}
	
	public void setNonDisposable() {
		refcount = -2;
	}
	
	public boolean isNonDisposable() {
		return refcount == -2;
	}
	
	public void addref() {
		if (refcount >= 0) {
			refcount++;
		}
	}
	
	public void unref() {
		if (refcount >= 0) {
			refcount--;
		}
	}
	
	public boolean canDispose() {
		return refcount == 0 || refcount == -1;
	}
	
	public long getRefCount() {
		return refcount;
	}
	
	public Image[] getImages() {
		return images;
	}
	
	public void setImages(Image[] images) {
		this.images = images;
	}
}
