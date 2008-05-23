package com.aelitis.azureus.ui.swt.views.skin.widgets;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.gudy.azureus2.ui.swt.ImageRepository;

import com.aelitis.azureus.ui.swt.skin.SWTSkin;
import com.aelitis.azureus.ui.swt.skin.SWTSkinFactory;

/**
 * A button that uses the skin look-n-feel for a 'flat' button
 * 
 * This is intended to be used in a regular SWT container (outside of the skin instance) for when the look and feel
 * of the skin button is desired
 */

public class BubbleButton
	extends SkinButton
{

	private Image[] backgroundImages = new Image[3];

	private Image[] backgroundImages_disabled = new Image[3];

	private Color[] foregroundColors = new Color[3];

	public BubbleButton(Composite parent) {
		super(parent);

		if (null == ImageRepository.getImage("button-left")) {
			ImageRepository.addPath(imagePath + "button-left.png", "button-left");
			ImageRepository.addPath(imagePath + "button-center.png", "button-center");
			ImageRepository.addPath(imagePath + "button-right.png", "button-right");
			ImageRepository.addPath(imagePath + "button_left-disabled.png",
					"button-left-disabled");
			ImageRepository.addPath(imagePath + "button-center-disabled.png",
					"button-center-disabled");
			ImageRepository.addPath(imagePath + "button-right-disabled.png",
					"button-right-disabled");
		}

		backgroundImages[0] = ImageRepository.getImage("button-left");
		backgroundImages[1] = ImageRepository.getImage("button-center");
		backgroundImages[2] = ImageRepository.getImage("button-right");

		backgroundImages_disabled[0] = ImageRepository.getImage("button_left-disabled");
		backgroundImages_disabled[1] = ImageRepository.getImage("button_center-disabled");
		backgroundImages_disabled[2] = ImageRepository.getImage("button_right-disabled");

		SWTSkin skin = SWTSkinFactory.getInstance();
		foregroundColors[0] = skin.getSkinProperties().getColor("color.text.fg");
		foregroundColors[1] = skin.getSkinProperties().getColor("color.text.over");
		foregroundColors[2] = skin.getSkinProperties().getColor(
				"color.text.disabled");
	}

	public Image[] getBackgroundImages() {
		return backgroundImages;
	}

	public Image[] getBackgroundImages_disabled() {
		return backgroundImages_disabled;
	}

	public Image[] getBackgroundImages_hover() {
		return null;
	}

	public Color[] getForegroundColors() {
		return foregroundColors;
	}

}
