package org.gudy.azureus2.ui.swt.components.widgets;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.gudy.azureus2.ui.swt.ImageRepository;

import com.aelitis.azureus.ui.swt.utils.ColorCache;

/**
 * A button that uses the skin look-n-feel for a 'flat' button
 * 
 * This is intended to be used in a regular SWT container (outside of the skin instance) for when the look and feel
 * of the skin button is desired
 */
public class FlatButton
	extends SkinButton
{

	private Image[] backgroundImages = new Image[3];

	private Image[] backgroundImages_over = new Image[3];

	private Color[] foregroundColors = new Color[3];

	public FlatButton(Composite parent) {
		super(parent);

		if (null == ImageRepository.getImage("button_left")) {
			ImageRepository.addPath(imagePath + "button_left.png", "button_left");
			ImageRepository.addPath(imagePath + "button_mid.png", "button_mid");
			ImageRepository.addPath(imagePath + "button_right.png", "button_right");
			ImageRepository.addPath(imagePath + "button_left-over.png",
					"button_left-over");
			ImageRepository.addPath(imagePath + "button_mid-over.png",
					"button_mid-over");
			ImageRepository.addPath(imagePath + "button_right-over.png",
					"button_right-over");
		}

		backgroundImages[0] = ImageRepository.getImage("button_left");
		backgroundImages[1] = ImageRepository.getImage("button_mid");
		backgroundImages[2] = ImageRepository.getImage("button_right");

		backgroundImages_over[0] = ImageRepository.getImage("button_left-over");
		backgroundImages_over[1] = ImageRepository.getImage("button_mid-over");
		backgroundImages_over[2] = ImageRepository.getImage("button_right-over");

		foregroundColors[0] = ColorCache.getColor(parent.getDisplay(), 194, 194,
				194);
		foregroundColors[1] = ColorCache.getColor(parent.getDisplay(), 179, 208,
				255);
		foregroundColors[2] = ColorCache.getColor(parent.getDisplay(), 85, 85, 85);

		setInset(new Inset(10, 10, 0, 0));

	}

	public Image[] getBackgroundImages() {
		return backgroundImages;
	}

	public Image[] getBackgroundImages_disabled() {
		return null;
	}

	public Image[] getBackgroundImages_hover() {
		return backgroundImages_over;
	}

	public Color[] getForegroundColors() {
		return foregroundColors;
	}

}
