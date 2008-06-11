package org.gudy.azureus2.ui.swt.components.widgets;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.gudy.azureus2.ui.swt.ImageRepository;

/**
 * TODO: This class will be refactored!!!!!
 * 
 * A button that uses the skin look-n-feel for a 'flat' button
 * 
 * This is intended to be used in a regular SWT container (outside of the skin instance) for when the look and feel
 * of the skin button is desired
 */
public class MiniCloseButton
	extends SkinButton
{

	private Image[] backgroundImages = new Image[3];

	private Image[] backgroundImages_over = new Image[3];

	public MiniCloseButton(Composite parent) {
		super(parent);

		if (null == ImageRepository.getImage("button_skin_close")) {
			ImageRepository.addPath(
					"com/aelitis/azureus/ui/images/button_skin_close.png",
					"button_skin_close");
			ImageRepository.addPath(
					"com/aelitis/azureus/ui/images/button_skin_close-over.png",
					"button_skin_close-over");
		}

		backgroundImages[0] = ImageRepository.getImage("button_skin_close");
		backgroundImages_over[0] = ImageRepository.getImage("button_skin_close-over");

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
		return null;
	}

}
