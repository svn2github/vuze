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
import java.io.InputStream;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.buddy.impl.VuzeBuddyImpl;
import com.aelitis.azureus.ui.swt.buddy.VuzeBuddySWT;

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
		
		//temp.. give user an icon..
		setAvatarImage(ImageRepository.getImage("azureus128"));
	}

	public VuzeBuddySWTImpl() {
	}

	public void setAvatar(byte[] avatar) {
		super.setAvatar(avatar);

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
		avatarImage = new Image(display, is);
		ourAvatarImage = true;
	}

	public Image getAvatarImage() {
		return avatarImage;
	}

	public void setAvatarImage(Image avatarImage) {
		disposeOldAvatarImage();

		this.avatarImage = avatarImage;
		ourAvatarImage = false;
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
}
