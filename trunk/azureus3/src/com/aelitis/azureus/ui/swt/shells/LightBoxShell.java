package com.aelitis.azureus.ui.swt.shells;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.swt.shells.main.UIFunctionsImpl;

public class LightBoxShell
{

	private Shell shell = null;

	private Shell parentShell = null;

	private Image fadedImage;

	private Rectangle fadedAreaExtent = null;

	public LightBoxShell() {
		UIFunctions uif = UIFunctionsManager.getUIFunctions();
		if (false == (uif instanceof UIFunctionsImpl)) {
			return;
		}

		parentShell = ((UIFunctionsImpl) uif).getMainShell();

		if (null == parentShell) {
			return;
		}

		shell = new Shell(parentShell, SWT.NO_TRIM | SWT.APPLICATION_MODAL);

		//		shell.addListener(SWT.Traverse, new Listener() {
		//			public void handleEvent(Event e) {
		//				if (e.detail == SWT.TRAVERSE_ESCAPE) {
		//					shell.close();
		//					e.detail = SWT.TRAVERSE_NONE;
		//					e.doit = false;
		//				}
		//			}
		//		});

		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				if (null != fadedImage && false == fadedImage.isDisposed()) {
					fadedImage.dispose();
				}
			}
		});

		shell.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				if (null != fadedImage) {
					e.gc.drawImage(fadedImage, 0, 0);
				}
			}
		});
	}

	public void open() {
		if (null != shell) {
			shell.setBounds(getShadedArea());
			createFadedImage();
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
			int borderWidth = parentShell.getBorderWidth();
			borderWidth += 3;
			fadedAreaExtent = parentShell.getClientArea();

			Point newLocation = parentShell.getLocation();
			fadedAreaExtent.x = newLocation.x + borderWidth;
			fadedAreaExtent.y = newLocation.y + parentShell.getSize().y
					- fadedAreaExtent.height - borderWidth;
		}
		return fadedAreaExtent;
	}

	private void createFadedImage() {
		Display display = parentShell.getDisplay();
		GC gc = new GC(display);

		fadedImage = new Image(display, getShadedArea());
		gc.copyArea(fadedImage, getShadedArea().x, getShadedArea().y);
		gc.dispose();

		ImageData imageData = fadedImage.getImageData();
		PaletteData palette = imageData.palette;

		if (null != palette) {
			float brightness = .5f;
			RGB rgb = new RGB(0, 0, 0);

			int[] lineData = new int[imageData.width];
			for (int y = 0; y < imageData.height; y++) {
				imageData.getPixels(0, y, imageData.width, lineData, 0);

				for (int x = 0; x < lineData.length; x++) {

					rgb = palette.getRGB(lineData[x]);
					rgb.red = (int) (rgb.red * brightness);
					rgb.green = (int) (rgb.green * brightness);
					rgb.blue = (int) (rgb.blue * brightness);

					imageData.setPixel(x, y, palette.getPixel(rgb));
				}
			}
		}
		fadedImage = new Image(display, imageData);
	}
}
