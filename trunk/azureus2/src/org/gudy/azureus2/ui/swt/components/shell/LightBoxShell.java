package org.gudy.azureus2.ui.swt.components.shell;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.IMainWindow;

import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

public class LightBoxShell
{

	private Shell lbShell = null;

	private Shell parentShell = null;

	private Rectangle fadedAreaExtent = null;

	private int insetTop = 0;

	private int insetBottom = 0;

	private int insetLeft = 0;

	private int insetRight = 0;

	private boolean closeOnESC = false;

	private boolean isShellOpened = false;

	private Display display;

	private UIFunctionsSWT uiFunctions;

	public LightBoxShell() {
		this(false);
	}

	/**
	 * Creates a LightBoxShell without opening it
	 * @param closeOnESC if <code>true</code> then the ESC key can be used to dismiss the lightbox
	 */
	public LightBoxShell(boolean closeOnESC) {
		this.closeOnESC = closeOnESC;

		uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
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

	public void setInsets(int top, int bottom, int left, int right) {
		this.insetTop = top;
		this.insetBottom = bottom;
		this.insetLeft = left;
		this.insetRight = right;
	}

	private void createControls() {
		lbShell = new Shell(parentShell, SWT.NO_TRIM | SWT.APPLICATION_MODAL);

		try {
			/*
			 * For the ideal lightbox effect we set the mask (background color)
			 * and transparency (alpha value) differently for OSX vs. non-OSX
			 */
			if (true == Constants.isOSX) {
				/*
				 * Black mask with 50% transparency
				 */
				lbShell.setBackground(new Color(parentShell.getDisplay(), 0, 0, 0));
				lbShell.setAlpha(128);
			} else {
				/*
				 * Black mask with 30% transparency
				 */
				lbShell.setBackground(new Color(parentShell.getDisplay(), 0, 0, 0));
				lbShell.setAlpha(178);
			}

		} catch (Throwable t) {
			// Not supported on SWT older than 3.4M4
			t.printStackTrace();
		}

		display = parentShell.getDisplay();

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

	}

	public void open() {
		if (null != lbShell && false == lbShell.isDisposed()) {
			lbShell.setBounds(getTargetArea());
			isShellOpened = true;
			lbShell.open();
		}
	}

	public void close() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
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
			fadedAreaExtent.x = parentLocation.x + xyOffset + insetLeft;
			fadedAreaExtent.y = parentLocation.y + parentShell.getSize().y
					- fadedAreaExtent.height - xyOffset + insetTop;
			fadedAreaExtent.width -= insetRight + insetLeft;
			fadedAreaExtent.height -= insetTop + insetBottom;
		}
		return fadedAreaExtent;
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
	public void open(StyledShell shellToOpen) {
		if (null != shellToOpen && null != lbShell) {

			if (false == isShellOpened) {
				open();
			}

			if (false == shellToOpen.isOpenedAlready()) {
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

	public class StyledShell
	{
		private Shell styledShell;

		private Composite borderedBackground;

		private Composite content;

		private int borderWidth;

		private boolean isOpenedAlready = false;

		private int alpha = 230;

		private StyledShell(int borderWidth) {
			this.borderWidth = borderWidth;

			styledShell = new Shell(lbShell, SWT.NO_TRIM | SWT.ON_TOP);
			try {
				styledShell.setAlpha(0);
			} catch (Throwable t) {
				// Not supported on SWT older than 3.4M4
				t.printStackTrace();
			}

			if (true == Constants.isOSX) {
				uiFunctions.createMainMenu(styledShell);
			}

			FillLayout fillLayout = new FillLayout();
			fillLayout.marginHeight = borderWidth;
			fillLayout.marginWidth = borderWidth;
			styledShell.setLayout(fillLayout);

			borderedBackground = new Composite(styledShell, SWT.NONE);

			fillLayout = new FillLayout();
			fillLayout.marginHeight = borderWidth;
			fillLayout.marginWidth = borderWidth;
			borderedBackground.setLayout(fillLayout);

			content = new Composite(borderedBackground, SWT.NONE);

			borderedBackground.addPaintListener(new PaintListener() {

				public void paintControl(PaintEvent e) {

					Rectangle bounds = borderedBackground.getClientArea();
					int r = StyledShell.this.borderWidth;
					int d = r * 2;

					try {
						e.gc.setAntialias(SWT.ON);
					} catch (Throwable t) {
						//Do nothing if it's not supported
					}

					/*
					 * Fills the four corners with the StyleShell background color so it blends in with the shell
					 */
					e.gc.setBackground(styledShell.getBackground());
					e.gc.fillRectangle(0, 0, r, r);
					e.gc.fillRectangle(bounds.width - r, 0, r, r);
					e.gc.fillRectangle(bounds.width - r, bounds.height - r, r, r);
					e.gc.fillRectangle(0, bounds.height - r, r, r);

					/*
					 * Then paint in the rounded-corner rectangle
					 */
					e.gc.setBackground(content.getBackground());

					/*
					 * Paint the 4 circles for the rounded corners; these circles will partially overlap
					 * on top of the four corners drawn above to give the look of a rounded corner
					 */
					e.gc.fillPolygon(circle(r, r, r));
					e.gc.fillPolygon(circle(r, r, bounds.height - r));
					e.gc.fillPolygon(circle(r, bounds.width - r, r));
					e.gc.fillPolygon(circle(r, bounds.width - r, bounds.height - r));

					/*
					 * Rectangle connecting between the top-left and top-right circles
					 */
					e.gc.fillRectangle(new Rectangle(r, 0, bounds.width - d, r));

					/*
					 * Rectangle connecting between the bottom-left and bottom-right circles
					 */
					e.gc.fillRectangle(new Rectangle(r, bounds.height - r, bounds.width
							- d, r));

					/*
					 * Rectangle to fill the area between the 2 bars created above
					 */
					e.gc.fillRectangle(new Rectangle(0, r, bounds.width, bounds.height
							- d));
				}

			});

			Listener l = new Listener() {
				int startX, startY;

				public void handleEvent(Event e) {
					if (e.type == SWT.KeyDown && e.character == SWT.ESC) {
						styledShell.dispose();
					}
					if (e.type == SWT.MouseDown && e.button == 1) {
						startX = e.x;
						startY = e.y;
					}
					if (e.type == SWT.MouseMove && (e.stateMask & SWT.BUTTON1) != 0) {
						Point p = styledShell.toDisplay(e.x, e.y);
						p.x -= startX;
						p.y -= startY;
						styledShell.setLocation(p);
					}
				}
			};
			styledShell.addListener(SWT.KeyDown, l);
			styledShell.addListener(SWT.MouseDown, l);
			styledShell.addListener(SWT.MouseMove, l);
			styledShell.setCursor(display.getSystemCursor(SWT.CURSOR_HAND));

		}

		private Region getRoundedRegion(Rectangle bounds) {

			int r = borderWidth;
			int d = r * 2;
			Region region = new Region();

			/*
			 * Add the 4 circles for the rounded corners
			 */
			region.add(circle(r, r, r));
			region.add(circle(r, r, bounds.height - r));
			region.add(circle(r, bounds.width - r, r));
			region.add(circle(r, bounds.width - r, bounds.height - r));

			/*
			 * Rectangle connecting between the top-left and top-right circles
			 */
			region.add(new Rectangle(r, 0, bounds.width - d, r));

			/*
			 * Rectangle connecting between the bottom-left and bottom-right circles
			 */
			region.add(new Rectangle(r, bounds.height - r, bounds.width - d, r));

			/*
			 * Rectangle to fill the area between the 2 bars created above
			 */
			region.add(new Rectangle(0, r, bounds.width, bounds.height - d));

			return region;
		}

		private int[] circle(int r, int offsetX, int offsetY) {
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
			return polygon;
		}

		public void addListener(int eventType, Listener listener) {
			if (true == isAlive()) {
				styledShell.addListener(eventType, listener);
			}
		}

		private void open() {
			if (true == isAlive()) {
				styledShell.open();
				isOpenedAlready = true;
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
			/*
			 * If the shell is opened already then, by default, resizing should not try to center the shell
			 */
			setSize(width, height, false == isOpenedAlready);
		}

		public void setSize(int width, int height, boolean centersShell) {
			if (true == isAlive()) {
				Rectangle outerBounds = styledShell.getBounds();

				/*
				 * Compensating since the 2 outer borders extends beyond the content area
				 */

				width += borderWidth * 4;
				height += borderWidth * 4;

				if (outerBounds.width != width || outerBounds.height != height) {
					styledShell.setVisible(false);
					outerBounds.width = width;
					outerBounds.height = height;

					/*
					 * Centers the the StyleShell relative to the parent shell
					 */
					if (true == centersShell) {
						Utils.centerRelativeTo(outerBounds, parentShell.getBounds());
					}

					/*
					 * Adjust the new bounds if the shell does not fully fit on the screen
					 */
					Utils.makeVisibleOnCursor(outerBounds);

					styledShell.setRegion(getRoundedRegion(outerBounds));
					styledShell.setBounds(outerBounds);
					styledShell.setVisible(true);
					styledShell.forceActive();
				}
			}
		}

		public void animateCurtain(final int milliSeconds) {
			if (false == isAlive()) {
				return;
			}
			display.asyncExec(new Runnable() {
				public void run() {
					try {
						int seconds = milliSeconds;
						int currentAlpha = 0;
						styledShell.setAlpha(currentAlpha);
						while (seconds > 0) {
							Thread.sleep(milliSeconds / 10);
							seconds -= (milliSeconds / 10);
							currentAlpha += 20;
							styledShell.setAlpha(Math.min(currentAlpha, alpha));
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					} finally {
						styledShell.setAlpha(alpha);
					}
				}
			});

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

		public boolean isOpenedAlready() {
			return isOpenedAlready;
		}

		public void setBackground(Color color) {
			styledShell.setBackground(color);
		}
	}

	public Display getDisplay() {
		return display;
	}

}
