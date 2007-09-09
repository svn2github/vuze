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

package com.aelitis.azureus.ui.swt.skin;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.ui.swt.utils.ImageLoader;

/**
 * @author TuxPaper
 * @created Aug 28, 2007
 *
 */
public class SWTSkinObjectSlider
	extends SWTSkinObjectBasic
	implements PaintListener, MouseListener, MouseMoveListener
{
	private Image imageFG;

	private Object imageFGLeft;

	private Object imageFGRight;

	private Canvas canvas;

	private Image imageThumbRight;

	private Image imageThumb;

	private Image imageThumbLeft;

	private Image imageBGRight;

	private Image imageBG;

	private Image imageBGLeft;

	private double percent;

	private Rectangle imageFGbounds;

	private Rectangle imageBGbounds;

	private Rectangle imageThumbBounds;

	private Point maxSize = new Point(0, 0);

	private boolean mouseDown;
	
	private ArrayList listeners = new ArrayList();

	public SWTSkinObjectSlider(SWTSkin skin, SWTSkinProperties skinProperties,
			String sID, String sConfigID, String[] typeParams, SWTSkinObject parent) {
		super(skin, skinProperties, sID, sConfigID, "slider", parent);

		String sSuffix = ".complete";
		ImageLoader imageLoader = skin.getImageLoader(properties);
		Image[] images = imageLoader.getImages(sConfigID + sSuffix);
		if (images.length == 1 && ImageLoader.isRealImage(images[0])) {
			imageFG = images[0];
			imageFGLeft = imageLoader.getImage(sConfigID + sSuffix + "-left");
			imageFGRight = imageLoader.getImage(sConfigID + sSuffix + "-right");
		} else if (images.length == 3 && ImageLoader.isRealImage(images[2])) {
			imageFGLeft = images[0];
			imageFG = images[1];
			imageFGRight = images[2];
		}

		if (imageFG != null) {
			imageFGbounds = imageFG.getBounds();
		}

		sSuffix = ".incomplete";
		images = imageLoader.getImages(sConfigID + sSuffix);
		if (images.length == 1 && ImageLoader.isRealImage(images[0])) {
			imageBG = images[0];
			imageBGLeft = imageLoader.getImage(sConfigID + sSuffix + "-left");
			imageBGRight = imageLoader.getImage(sConfigID + sSuffix + "-right");
		} else if (images.length == 3 && ImageLoader.isRealImage(images[2])) {
			imageBGLeft = images[0];
			imageBG = images[1];
			imageBGRight = images[2];
		}

		if (imageBG != null) {
			imageBGbounds = imageBG.getBounds();
		}

		sSuffix = ".thumb";
		images = imageLoader.getImages(sConfigID + sSuffix);
		if (images.length == 1) {
			imageThumb = images[0];
			imageThumbLeft = imageLoader.getImage(sConfigID + sSuffix + "-left");
			imageThumbRight = imageLoader.getImage(sConfigID + sSuffix + "-right");
		} else if (images.length == 3 && ImageLoader.isRealImage(images[2])) {
			imageThumbLeft = images[0];
			imageThumb = images[1];
			imageThumbRight = images[2];
		}

		if (imageThumb != null) {
			imageThumbBounds = imageThumb.getBounds();
		}

		maxSize = buildMaxSize(new Rectangle[] {
			imageThumbBounds,
			imageBGbounds,
			imageFGbounds
		});

		if (skinProperties.getStringValue(sConfigID + ".width", "").equalsIgnoreCase(
				"auto")) {
			maxSize.x = 0;
		}

		int style = SWT.NONE;

		if (skinProperties.getIntValue(sConfigID + ".border", 0) == 1) {
			style |= SWT.BORDER;
		}

		Composite createOn;
		if (parent == null) {
			createOn = skin.getShell();
		} else {
			createOn = (Composite) parent.getControl();
		}

		canvas = new Canvas(createOn, style);
		canvas.setLayoutData(new FormData(maxSize.x == 0 ? SWT.DEFAULT
				: maxSize.x, maxSize.y));
		canvas.setSize(SWT.DEFAULT, maxSize.y);
		setControl(canvas);

		canvas.addPaintListener(this);
		canvas.addMouseListener(this);
		canvas.addMouseMoveListener(this);
	}

	private Point buildMaxSize(Rectangle[] bounds) {
		Point maxSize = new Point(0, 0);
		for (int i = 0; i < bounds.length; i++) {
			if (bounds[i] == null) {
				continue;
			}

			if (bounds[i].width > maxSize.x) {
				maxSize.x = bounds[i].width;
			}
			if (bounds[i].height > maxSize.y) {
				maxSize.y = bounds[i].height;
			}
		}
		return maxSize;
	}

	// @see org.eclipse.swt.events.PaintListener#paintControl(org.eclipse.swt.events.PaintEvent)
	public void paintControl(PaintEvent e) {
		int fullWidth = maxSize.x == 0 || imageFGbounds == null
				? canvas.getClientArea().width : imageFGbounds.width;

		if (percent > 0 && imageFG != null) {
			int xDrawTo = (int) (fullWidth * percent);
			int xDrawToSrc = xDrawTo > imageFGbounds.width ? imageFGbounds.width
					: xDrawTo;
			int y = (maxSize.y - imageFGbounds.height) / 2;
			e.gc.drawImage(imageFG, 0, 0, xDrawToSrc, imageFGbounds.height, 0, y,
					xDrawTo, imageFGbounds.height);
		}
		if (percent < 100 && imageBG != null) {
			int xDrawFrom = (int) (imageBGbounds.width * percent);
			int xDrawWidth = imageBGbounds.width - xDrawFrom;
			e.gc.drawImage(imageBG, xDrawFrom, 0, xDrawWidth, imageFGbounds.height,
					xDrawFrom, 0, xDrawWidth, imageFGbounds.height);
		}

		int drawWidth = fullWidth - imageThumbBounds.width;
		e.gc.drawImage(imageThumb, (int) (drawWidth * percent), 0);

	}

	public double getPercent() {
		return percent;
	}

	public void setPercent(double percent) {
		setPercent(percent, false);
	}

	private void setPercent(double percent, boolean triggerListeners) {
		if (this.percent == percent) {
			return;
		}
		if (triggerListeners) {
			Object[] listenersArray = listeners.toArray();
			for (int i = 0; i < listenersArray.length; i++) {
				SWTSkinListenerSliderSelection l = (SWTSkinListenerSliderSelection) listenersArray[i];
				if (!l.selectionChanging(this.percent, percent)) {
					return;
				}
			}
		}
		
		if (percent < 0) {
			percent = 0;
		} else if (percent > 1) {
			percent = 1;
		}
		this.percent = percent;

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (canvas != null && !canvas.isDisposed()) {
					canvas.redraw();
					canvas.update();
				}
			}
		});

		if (triggerListeners) {
			Object[] listenersArray = listeners.toArray();
			for (int i = 0; i < listenersArray.length; i++) {
				SWTSkinListenerSliderSelection l = (SWTSkinListenerSliderSelection) listenersArray[i];
				l.selectionChanged(percent);
			}
		}
	}

	// @see org.eclipse.swt.events.MouseListener#mouseDoubleClick(org.eclipse.swt.events.MouseEvent)
	public void mouseDoubleClick(MouseEvent e) {
	}

	// @see org.eclipse.swt.events.MouseListener#mouseDown(org.eclipse.swt.events.MouseEvent)
	public void mouseDown(MouseEvent e) {
		mouseDown = true;
		System.out.println(e.x);

		int offset = imageThumbBounds.width / 2;
		int sizeX = maxSize.x;
		if (maxSize.x == 0) {
			sizeX = canvas.getClientArea().width;
		}
		float newPercent = (e.x - offset)
				/ (float) (sizeX - imageThumbBounds.width);
		
		Object[] listenersArray = listeners.toArray();
		for (int i = 0; i < listenersArray.length; i++) {
			SWTSkinListenerSliderSelection l = (SWTSkinListenerSliderSelection) listenersArray[i];
			if (!l.selectionChanging(this.percent, newPercent)) {
				return;
			}
		}

		setPercent(newPercent, true);
	}

	// @see org.eclipse.swt.events.MouseListener#mouseUp(org.eclipse.swt.events.MouseEvent)
	public void mouseUp(MouseEvent e) {
		mouseDown = false;
	}

	// @see org.eclipse.swt.events.MouseMoveListener#mouseMove(org.eclipse.swt.events.MouseEvent)
	public void mouseMove(MouseEvent e) {
		if (mouseDown) {
			int offset = imageThumbBounds.width / 2;
			int sizeX = maxSize.x;
			if (maxSize.x == 0) {
				sizeX = canvas.getClientArea().width;
			}
			float newPercent = (e.x - offset)
					/ (float) (sizeX - imageThumbBounds.width);

			setPercent(newPercent, true);
		}
	}

	public void addListener(SWTSkinListenerSliderSelection listener) {
		listeners.add(listener);
	}

	public static class SWTSkinListenerSliderSelection
	{
		public boolean selectionChanging(double oldPercent, double newPercent) {
			return true;
		}

		public void selectionChanged(double percent) {
			
		}
	}
	
}
