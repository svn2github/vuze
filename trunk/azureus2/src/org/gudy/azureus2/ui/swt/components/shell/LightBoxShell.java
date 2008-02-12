package org.gudy.azureus2.ui.swt.components.shell;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.IMainWindow;

import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

public class LightBoxShell
{

	private Shell lbShell = null;

	private Shell parentShell = null;

	private Rectangle fadedAreaExtent = null;

	private float lightPercentage = .5f;

	private int alpha = 255;

	private int top = 0;

	private int bottom = 0;

	private int left = 0;

	private int right = 0;

	private boolean closeOnESC = false;

	private boolean isShellOpened = false;

	private Display display;

	private Image processedImage;

	public LightBoxShell() {
		this(false);
	}

	public LightBoxShell(boolean closeOnESC) {
		this.closeOnESC = closeOnESC;

		UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
		if (null == uiFunctions) {
			throw new NullPointerException(
					"An initialized instance of UIFunctionsSWT is required to create a LightBoxShell");
		}

		parentShell = uiFunctions.getMainShell();

		if (null == parentShell) {
			return;
		}
		IMainWindow mainWindow = uiFunctions.getMainWindow();
		Rectangle r = mainWindow.getMetrics(IMainWindow.WINDOW_ELEMENT_STATUSBAR);
		setInsets(0, r.height, 0, 0);
		createControls();
	}

	public LightBoxShell(Shell parentShell) {
		this.parentShell = parentShell;
		createControls();
	}

	public void setBrightness(float lightPercentage, int alpha) {
		this.lightPercentage = Math.min(Math.max(lightPercentage, 0f), 1f);
		this.alpha = Math.max(Math.min(alpha, 255), 0);
	}

	public void setInsets(int top, int bottom, int left, int right) {
		this.top = top;
		this.bottom = bottom;
		this.left = left;
		this.right = right;
	}

	private void createControls() {
		lbShell = new Shell(parentShell, SWT.NO_TRIM | SWT.APPLICATION_MODAL);

		/*
		 * Trap and prevent the ESC key from closing the shell
		 */
		if (false == closeOnESC) {
			lbShell.addListener(SWT.Traverse, new Listener() {
				public void handleEvent(Event e) {
					if (e.detail == SWT.TRAVERSE_ESCAPE) {
						e.doit = false;
					}
				}
			});
		}

		lbShell.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				if (null != processedImage && false == processedImage.isDisposed()) {
					Rectangle clipping = e.gc.getClipping();
					e.gc.drawImage(processedImage, clipping.x, clipping.y,
							clipping.width, clipping.height, clipping.x, clipping.y,
							clipping.width, clipping.height);
				}
			}
		});

		//		createFadedImage();
	}

	public void open() {
		if (null != lbShell) {
			lbShell.setBounds(getTargetArea());
			createFadedImage();
			//			createBlurredImage();
			isShellOpened = true;
			lbShell.open();
		}
	}

	public void close() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (null != processedImage && false == processedImage.isDisposed()) {
					processedImage.dispose();
				}

				if (null != lbShell && false == lbShell.isDisposed()) {
					lbShell.close();
				}
			}
		});
	}

	/**
	 * Returns the effective area for the lightbox
	 * @return
	 */
	private Rectangle getTargetArea() {
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

	/**
	 * Creates a darken, faded, and blurred image
	 */
	private void createBlurredImage() {
		display = parentShell.getDisplay();
		GC gc = new GC(display);
		Image originalBackground = new Image(display, getTargetArea());
		gc.copyArea(originalBackground, getTargetArea().x, getTargetArea().y);
		gc.dispose();

		ImageData imageData = originalBackground.getImageData();
		ImageData imageDataNew = originalBackground.getImageData();
		PaletteData palette = imageData.palette;
		imageData.alpha = alpha;
		if (null != palette) {

			int[] lookup = new int[256];
			for (int i = 0; i < lookup.length; i++) {
				lookup[i] = (int) (i * lightPercentage);
			}

			RGB rgbNew = new RGB(0, 0, 0);
			RGB rgbMinus = new RGB(0, 0, 0);
			RGB rgb = new RGB(0, 0, 0);
			RGB rgbPlus = new RGB(0, 0, 0);
			int[] lineData = new int[imageData.width];
			for (int y = 0; y < imageData.height; y++) {
				imageData.getPixels(0, y, imageData.width, lineData, 0);

				for (int x = 1; x < (lineData.length - 1); x++) {

					rgbMinus = palette.getRGB(lineData[x - 1]);
					rgb = palette.getRGB(lineData[x]);
					rgbPlus = palette.getRGB(lineData[x + 1]);

					rgbNew.red = lookup[(rgbMinus.red + rgb.red + rgbPlus.red) / 3];
					rgbNew.green = lookup[(rgbMinus.green + rgb.green + rgbPlus.green) / 3];
					rgbNew.blue = lookup[(rgbMinus.blue + rgb.blue + rgbPlus.blue) / 3];

					imageDataNew.setPixel(x, y, palette.getPixel(rgbNew));
				}
			}
		}

		processedImage = new Image(display, getTargetArea());
		gc = new GC(processedImage);
		gc.setBackground(display.getSystemColor(SWT.COLOR_GRAY));
		gc.fillRectangle(0, 0, getTargetArea().width, getTargetArea().height);
		gc.drawImage(new Image(display, imageData), 0, 0);

		lbShell.setBackgroundImage(processedImage);

		gc.dispose();
		originalBackground.dispose();

	}

	/**
	 * Creates a darken and faded image
	 */
	private void createFadedImage() {
		display = parentShell.getDisplay();
		GC gc = new GC(display);

		Image originalBackground = new Image(display, getTargetArea());
		gc.copyArea(originalBackground, getTargetArea().x, getTargetArea().y);
		gc.dispose();

		ImageData imageData = originalBackground.getImageData();
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

		processedImage = new Image(display, getTargetArea());
		gc = new GC(processedImage);
		gc.setBackground(display.getSystemColor(SWT.COLOR_GRAY));
		gc.fillRectangle(0, 0, getTargetArea().width, getTargetArea().height);
		gc.drawImage(new Image(display, imageData), 0, 0);

		lbShell.setBackgroundImage(processedImage);

		gc.dispose();
		originalBackground.dispose();
	}

	/**
	 * Creates a stylized shell with pre-defined look and feel
	 * @param closeLightboxOnExit
	 * @return
	 */
	public StyledShell createStyledShell(int borderWidth,
			boolean closeLightboxOnExit) {
		StyledShell newShell = new StyledShell(borderWidth);

		if (true == closeLightboxOnExit) {
			newShell.addListener(SWT.Close, new Listener() {
				public void handleEvent(Event event) {
					close();
				}
			});

		}
		return newShell;
	}

	/**
	 * Centers and opens the given shell and closes the light box when the given shell is closed
	 * @param shellToOpen
	 */
	public void open(Shell shellToOpen) {
		if (null != shellToOpen && null != lbShell) {

			if (false == isShellOpened) {
				open();
			}

			Utils.centerWindowRelativeTo(shellToOpen, lbShell);

			shellToOpen.open();

			/*
			 * Block the return from this method until the given shell is closed
			 */
			//			Display display = shellToOpen.getDisplay();
			//			while (!shellToOpen.isDisposed()) {
			//				if (!display.readAndDispatch()) {
			//					display.sleep();
			//				}
			//			}
			close();
		}
	}

	/**
	 * Centers and opens the given shell and closes the light box when the given shell is closed
	 * @param shellToOpen
	 */
	public void open(StyledShell shellToOpen) {
		if (null != shellToOpen && null != lbShell) {

			if (false == isShellOpened) {
				open();
			}

			shellToOpen.open();

			/*
			 * Block the return from this method until the given shell is closed;
			 * without this the shell will simply open and close immediately
			 */

			while (true == shellToOpen.isAlive()) {
				if (false == display.readAndDispatch()) {
					display.sleep();
				}
			}
		}
	}

	public void setCursor(Cursor cursor) {
		if (null != lbShell && false == lbShell.isDisposed()) {
			lbShell.setCursor(cursor);
		}
	}

	public void setData(String key, Object value) {
		if (null != lbShell && false == lbShell.isDisposed()) {
			lbShell.setData(key, value);
		}
	}

	private void fadeDisplay(int delayInSeconds) {
		GC gc = new GC(display);
		Rectangle bounds = lbShell.getBounds();
		ImageData imageData = processedImage.getImageData();
		imageData.alpha = 255;
		Image image = new Image(display, imageData);
		for (int i = 0; i < delayInSeconds; i++) {
			gc.drawImage(image, bounds.x, bounds.y);
			imageData.alpha -= 30;
			image = new Image(display, imageData);

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		lbShell.redraw();
		image.dispose();
		gc.dispose();
	}

	public class StyledShell
	{
		private Shell styledShell;

		private Image trimImage;

		private Composite content;

		private StyledShell(int borderWidth) {
			styledShell = new Shell(lbShell, SWT.NO_TRIM );

			FillLayout fillLayout = new FillLayout();
			fillLayout.marginHeight = borderWidth;
			fillLayout.marginWidth = borderWidth;
			styledShell.setLayout(fillLayout);

			content = new Composite(styledShell, SWT.NONE);

			styledShell.addListener(SWT.Traverse, new Listener() {
				public void handleEvent(Event e) {
					switch (e.detail) {
						case SWT.TRAVERSE_ESCAPE:
							styledShell.close();
							e.detail = SWT.TRAVERSE_NONE;
							e.doit = false;
							break;
					}
				}
			});

			styledShell.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent e) {
					try{
					if (null != trimImage && false == trimImage.isDisposed()) {
						if (null != styledShell && false == styledShell.isDisposed()) {
							if (null != lbShell && false == lbShell.isDisposed()) {
								Rectangle bounds = styledShell.getBounds();
								e.gc.drawImage(trimImage, bounds.x - lbShell.getBounds().x,
										bounds.y - lbShell.getBounds().y, bounds.width,
										bounds.height, 0, 0, bounds.width, bounds.height);
							}
						}
					}}
					catch (Exception ex){
						Debug.out(ex);
					}
				}
			});

			styledShell.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					if (null != processedImage && false == processedImage.isDisposed()) {
						processedImage.dispose();
					}
				}
			});

		}

		private void createBackGround() {
			GC gc = new GC(processedImage);
			Image originalBackground = new Image(display, processedImage.getBounds());
			gc.copyArea(originalBackground, 0, 0);
			gc.dispose();

			ImageData imageData = originalBackground.getImageData();
			PaletteData palette = imageData.palette;
			//			imageData.alpha = 255;
			if (null != palette) {

				int[] lookup = new int[256];
				for (int i = 0; i < lookup.length; i++) {
					lookup[i] = (int) (i * .5);
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
			trimImage = new Image(display, imageData);
			styledShell.setBackgroundImage(originalBackground);
			//			originalBackground.dispose();
		}

		private void setRegion() {
			Rectangle bounds = styledShell.getBounds();
			int r = 10;
			int d = r * 2;
			Region region = new Region();
			region.add(circle(r, r, r));
			region.add(circle(r, r, bounds.height - r));
			region.add(circle(r, bounds.width - r, r));
			region.add(circle(r, bounds.width - r, bounds.height - r));

			region.add(new Rectangle(r - 1, 0, bounds.width - d + 2, r));

			region.add(new Rectangle(r - 1, bounds.height - r, bounds.width - d + 2,
					r));

			region.add(new Rectangle(0, r, bounds.width, bounds.height - d));
			styledShell.setRegion(region);
		}

		private Region circle(int r, int offsetX, int offsetY) {
			Region region = new Region();
			int[] polygon = new int[8 * r + 4];
			//x^2 + y^2 = r^2
			for (int i = 0; i < 2 * r + 1; i++) {
				int x = i - r;
				int y = (int) Math.sqrt(r * r - x * x);
				polygon[2 * i] = offsetX + x;
				polygon[2 * i + 1] = offsetY + y;
				polygon[8 * r - 2 * i - 2] = offsetX + x;
				polygon[8 * r - 2 * i - 1] = offsetY - y;
			}
			region.add(polygon);
			return region;
		}

		public void addListener(int eventType, Listener listener) {
			if (true == isAlive()) {
				styledShell.addListener(eventType, listener);
			}
		}

		private void open() {
			if (true == isAlive()) {
				Utils.centerWindowRelativeTo(styledShell, lbShell);
				setRegion();
				createBackGround();
				styledShell.open();
			}
		}

		public void forceActive() {
			if (true == isAlive()) {
				styledShell.setVisible(true);
				styledShell.forceActive();
			}
		}

		public void pack() {
			if (true == isAlive()) {
				styledShell.pack();
			}
		}

		public void pack(boolean changed) {
			if (true == isAlive()) {
				styledShell.pack(changed);
			}
		}

		public void setSize(int width, int height) {
			if (true == isAlive()) {
				/*
				 * Prevent the shell from being made bigger than the lightbox
				 */
				Rectangle lbBounds = lbShell.getBounds();
				width = Math.min(width, lbBounds.width);
				height = Math.min(height, lbBounds.height);

				Rectangle bounds = styledShell.getBounds();
				if (bounds.width != width || bounds.height != height) {
					styledShell.setSize(width, height);
					Utils.centerWindowRelativeTo(styledShell, lbShell);
					setRegion();
				}
			}
		}

		public void setSize(Point size) {
			setSize(size.x, size.y);
		}

		public void setVisible(boolean visible) {
			if (true == isAlive()) {
				styledShell.setVisible(visible);
			}
		}

		public void removeListener(int eventType, Listener listener) {
			if (true == isAlive()) {
				styledShell.removeListener(eventType, listener);
			}
		}

		public void setCursor(Cursor cursor) {
			if (true == isAlive()) {
				styledShell.setCursor(cursor);
			}

			if (null != lbShell && false == lbShell.isDisposed()) {
				lbShell.setCursor(cursor);
			}
		}

		public void setData(String key, Object value) {
			if (true == isAlive()) {
				styledShell.setData(key, value);
			}
		}

		public boolean isAlive() {
			if (null == styledShell || true == styledShell.isDisposed()) {
				return false;
			}
			return true;
		}

		public Composite getContent() {
			return content;
		}

		public Shell getShell() {
			return styledShell;
		}
	}

}
