package com.aelitis.azureus.ui.swt.shells;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.util.Constants;

import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.swt.shells.main.UIFunctionsImpl;

public class LightBoxShell
{

	private Shell shell = null;

	private Shell parentShell = null;

	private Rectangle fadedAreaExtent = null;

	private float lightPercentage = .6f;

	private int alpha = 255;

	private int top = 0;

	private int bottom = 0;

	private int left = 0;

	private int right = 0;

	public LightBoxShell() {
		UIFunctions uif = UIFunctionsManager.getUIFunctions();
		if (false == (uif instanceof UIFunctionsImpl)) {
			return;
		}

		parentShell = ((UIFunctionsImpl) uif).getMainShell();

		if (null == parentShell) {
			return;
		}
		createControls();
	}

	public LightBoxShell(Shell parentShell) {
		this.parentShell = parentShell;
		createControls();
	}

	public void setBrightness(float lightPercentage, int alpha) {
		this.lightPercentage = Math.min(Math.max(lightPercentage, 0f), 1f);
		this.alpha = Math.max(Math.min(alpha, 0), 255);
	}

	public void setInsets(int top, int bottom, int left, int right) {
		this.top = top;
		this.bottom = bottom;
		this.left = left;
		this.right = right;
	}

	private void createControls() {
		shell = new Shell(parentShell, SWT.NO_TRIM | SWT.APPLICATION_MODAL);
		parentShell.addPaintListener(new PaintListener() {
		
			public void paintControl(PaintEvent e) {
				System.out.println("Painting...");//KN: sysout
		
			}
		
		});
	}

	public void open() {
		if (null != shell) {
			shell.setBounds(getShadedArea());
			createFadedImage();
//			createBlurredImage();
			shell.open();
		}
	}

	public void close() {
		if (null != shell && false == shell.isDisposed()) {
			shell.close();
		}
	}

	private Rectangle getShadedArea() {
		if (null == fadedAreaExtent) {
			/*
			 * Not entirely sure why this has to be done this way but it seems
			 * the Windows' shell has a 4 pixel border whereas the OSX's shell has none;
			 * this offset is used to shift the image to fit the client area exactly
			 */

			int xyOffset = (true == Constants.isOSX) ? 0 : 4;

			fadedAreaExtent = parentShell.getClientArea();
			Point parentLocation = parentShell.getLocation();
			fadedAreaExtent.x = parentLocation.x + xyOffset + left;
			fadedAreaExtent.y = parentLocation.y + parentShell.getSize().y
					- fadedAreaExtent.height - xyOffset + top;
			fadedAreaExtent.width -= right + left;
			fadedAreaExtent.height -= top + bottom;
		}
		return fadedAreaExtent;
	}

	private void createBlurredImage() {
		Display display = parentShell.getDisplay();
		GC gc = new GC(display);

		Image fadedImage = new Image(display, getShadedArea());
		gc.copyArea(fadedImage, getShadedArea().x, getShadedArea().y);
		gc.dispose();

		ImageData imageData = fadedImage.getImageData();
		ImageData imageDataNew = fadedImage.getImageData();
		PaletteData palette = imageData.palette;
		imageData.alpha = alpha;
		if (null != palette) {

			int[] lookup = new int[256];
			for (int i = 0; i < lookup.length; i++) {
				lookup[i] = (int) (i * lightPercentage);
			}

			RGB rgbNew = new RGB(0, 0, 0);
			RGB rgbMinus = new RGB(0,0,0);
			RGB rgb = new RGB(0, 0, 0);
			RGB rgbPlus = new RGB(0,0,0);
			int[] lineData = new int[imageData.width];
			for (int y = 0; y < imageData.height; y++) {
				imageData.getPixels(0, y, imageData.width, lineData, 0);

				for (int x = 1; x < (lineData.length-1); x++) {

					rgbMinus = palette.getRGB(lineData[x-1]);
					rgb = palette.getRGB(lineData[x]);
					rgbPlus = palette.getRGB(lineData[x+1]);
					
					rgbNew.red = lookup[(rgbMinus.red + rgb.red + rgbPlus.red)/3];
					rgbNew.green = lookup[(rgbMinus.green + rgb.green + rgbPlus.green)/3];
					rgbNew.blue = lookup[(rgbMinus.blue + rgb.blue + rgbPlus.blue)/3];

					imageDataNew.setPixel(x, y, palette.getPixel(rgbNew));
				}
			}
		}
		shell.setBackgroundImage(new Image(display, imageDataNew));
		fadedImage.dispose();
	}

	private void createFadedImage() {
		Display display = parentShell.getDisplay();
		GC gc = new GC(display);

		Image fadedImage = new Image(display, getShadedArea());
		gc.copyArea(fadedImage, getShadedArea().x, getShadedArea().y);
		gc.dispose();
		
		ImageData imageData = fadedImage.getImageData();
		PaletteData palette = imageData.palette;
		imageData.alpha = alpha;
		if (null != palette) {

			int[] lookup = new int[256];
			for (int i = 0; i < lookup.length; i++) {
				lookup[i] = (int) (i * lightPercentage);
			}

			RGB rgb = new RGB(0, 0, 0);

			int[] lineData = new int[imageData.width];
			for (int y = 0; y < imageData.height; y++) {
				imageData.getPixels(0, y, imageData.width, lineData, 0);

				for (int x = 0; x < lineData.length; x++) {

					rgb = palette.getRGB(lineData[x]);
					rgb.red = lookup[rgb.red];
					rgb.green = lookup[rgb.green];
					rgb.blue = lookup[rgb.blue];

					imageData.setPixel(x, y, palette.getPixel(rgb));
				}
			}
		}
		shell.setBackgroundImage(new Image(display, imageData));
		fadedImage.dispose();
	}
}
