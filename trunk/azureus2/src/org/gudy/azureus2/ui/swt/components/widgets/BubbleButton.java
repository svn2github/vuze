package org.gudy.azureus2.ui.swt.components.widgets;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.ui.swt.utils.ColorCache;

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

	private Image[] backgroundImages_hover = new Image[3];

	private Color[] foregroundColors = new Color[3];

	public BubbleButton(Composite parent) {
		super(parent);

		final ImageLoader imageLoader = ImageLoader.getInstance();
		
		backgroundImages[0] = imageLoader.getImage("button_dialog_left");
		backgroundImages[1] = imageLoader.getImage("button_dialog_center");
		backgroundImages[2] = imageLoader.getImage("button_dialog_right");

		backgroundImages_hover[0] = imageLoader.getImage("button_dialog_left-over");
		backgroundImages_hover[1] = imageLoader.getImage("button_dialog_center-over");
		backgroundImages_hover[2] = imageLoader.getImage("button_dialog_right-over");

		foregroundColors[0] = ColorCache.getColor(parent.getDisplay(), 194, 194,
				194);
		foregroundColors[1] = ColorCache.getColor(parent.getDisplay(), 194, 194,
				194);
		foregroundColors[2] = ColorCache.getColor(parent.getDisplay(), 85, 85, 85);

		setInset(new Inset(20, 20, 0, 0));

		/*
		 * Increase default font height by 1 so the text for this button is a little bit taller
		 */
		FontData[] fData = getFont().getFontData();
		for (int i = 0; i < fData.length; i++) {
			fData[i].height += 1;
		}
		final Font newFont = new Font(getDisplay(), fData);
		setFont(newFont);
		addDisposeListener(new DisposeListener() {

			public void widgetDisposed(DisposeEvent e) {
				if (null != newFont && false == newFont.isDisposed()) {
					newFont.dispose();
				}
				imageLoader.releaseImage("button_dialog_left");
				imageLoader.releaseImage("button_dialog_center");
				imageLoader.releaseImage("button_dialog_right");
				imageLoader.releaseImage("button_dialog_left-over");
				imageLoader.releaseImage("button_dialog_center-over");
				imageLoader.releaseImage("button_dialog_right-over");
			}
		});
	}

	public Image[] getBackgroundImages() {
		return backgroundImages;
	}

	public Image[] getBackgroundImages_disabled() {
		return null;
	}

	public Image[] getBackgroundImages_hover() {
		return backgroundImages_hover;
	}

	public Color[] getForegroundColors() {
		return foregroundColors;
	}

}
