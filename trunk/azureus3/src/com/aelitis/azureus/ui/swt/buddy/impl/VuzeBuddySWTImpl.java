/**
 * Created on Apr 14, 2008
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

package com.aelitis.azureus.ui.swt.buddy.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.buddy.impl.VuzeBuddyImpl;
import com.aelitis.azureus.ui.swt.buddy.VuzeBuddySWT;
import com.aelitis.azureus.ui.swt.utils.ImageLoaderFactory;

/**
 * @author TuxPaper
 * @created Apr 14, 2008
 *
 */
public class VuzeBuddySWTImpl
	extends VuzeBuddyImpl
	implements VuzeBuddySWT
{
	private Image avatarImage;

	private boolean ourAvatarImage;

	/**
	 * 
	 */
	public VuzeBuddySWTImpl(String publicKey) {
		super(publicKey);
	}

	public VuzeBuddySWTImpl() {
	}

	public void setAvatar(byte[] avatar) {
		disposeOldAvatarImage();

		if (avatar == null) {
			avatarImage = null;
			return;
		}

		Display display = Utils.getDisplay();
		if (display == null) {
			return;
		}
		InputStream is = new ByteArrayInputStream(avatar);
		Image bigAvatarImage = new Image(display, is);
		avatarImage = new Image(display, 40, 40);
		GC gc = new GC(avatarImage);
		try {
			Rectangle bounds = bigAvatarImage.getBounds();
			try {
				gc.setInterpolation(SWT.HIGH);
			} catch (Exception e) {
			}
			gc.drawImage(bigAvatarImage, 0, 0, bounds.width, bounds.height, 0, 0, 40,
					40);
		} finally {
			gc.dispose();
		}
		bigAvatarImage.dispose();
		ourAvatarImage = true;

		// triggers listener
		super.setAvatar(avatar);
	}

	public Image getAvatarImage() {
		if (avatarImage == null) {
			try {
				avatarImage = ImageLoaderFactory.getInstance().getImage(
						"image.buddy.default.avatar");
			} catch (Exception e) {
				avatarImage = ImageRepository.getImage("azureus64");
			}
			ourAvatarImage = false;
		}

		return avatarImage;
	}

	public void setAvatarImage(final Image avatarImage) {
		disposeOldAvatarImage();

		this.avatarImage = avatarImage;
		ourAvatarImage = false;

		if (avatarImage != null) {
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					ImageLoader loader = new ImageLoader();
					ByteArrayOutputStream os = new ByteArrayOutputStream();
					loader.data = new ImageData[] {
						avatarImage.getImageData()
					};
					loader.save(os, SWT.IMAGE_PNG);
					VuzeBuddySWTImpl.super.setAvatar(os.toByteArray());
				}
			});
		}
	}

	private void disposeOldAvatarImage() {
		if (ourAvatarImage && avatarImage != null && !avatarImage.isDisposed()) {
			final Image avatarImageToDispose = avatarImage;
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					if (!avatarImageToDispose.isDisposed()) {
						avatarImageToDispose.dispose();
					}
				}
			});
		}
	}

	// @see com.aelitis.azureus.buddy.impl.VuzeBuddyImpl#toDebugString()
	public String toDebugString() {
		return "SWT" + super.toDebugString();
	}

	// @see java.lang.Object#finalize()
	protected void finalize() throws Throwable {
		super.finalize();

		if (avatarImage != null) {
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					if (avatarImage != null && !avatarImage.isDisposed()) {
						avatarImage.dispose();
					}
				}
			});
		}
	}
}
