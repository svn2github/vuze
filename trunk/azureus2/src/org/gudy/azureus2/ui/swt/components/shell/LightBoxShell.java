package org.gudy.azureus2.ui.swt.components.shell;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.*;
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

	public void setInsets(int top, int bottom, int left, int right) {
		this.top = top;
		this.bottom = bottom;
		this.left = left;
		this.right = right;
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
				 * Light gray mask with 43% transparency
				 */
				lbShell.setBackground(new Color(parentShell.getDisplay(), 28, 28, 28));
				lbShell.setAlpha(110);
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
		if (null != lbShell) {
			lbShell.setBounds(getTargetArea());
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

	public class StyledShell
	{
		private Shell styledShell;

		private Composite content;

		private StyledShell(int borderWidth) {
			styledShell = new Shell(lbShell, SWT.NO_TRIM);
			styledShell.setBackground(new Color(parentShell.getDisplay(), 38, 38, 38));
			try {
				styledShell.setAlpha(230);
			} catch (Throwable t) {
				// Not supported on SWT older than 3.4M4
				t.printStackTrace();
			}

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

			styledShell.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					if (null != processedImage && false == processedImage.isDisposed()) {
						processedImage.dispose();
					}
				}
			});

		}

		private void setRegion() {
			Rectangle bounds = styledShell.getBounds();
			int r = 10;
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
