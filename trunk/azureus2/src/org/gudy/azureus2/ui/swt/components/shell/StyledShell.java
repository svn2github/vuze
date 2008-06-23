package org.gudy.azureus2.ui.swt.components.shell;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
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
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.utils.ColorCache;

public class StyledShell
{

	public static final int HINT_ALIGN_CENTER = 1 << 1;

	public static final int HINT_ALIGN_FIT_IN_MONITOR = 1 << 2;

	public static final int HINT_ALIGN_NONE = 1 << 0;

	private Shell parentShell;

	private Shell styledShell;

	private Display display;

	private Composite borderedBackground;

	private Composite content;

	private int borderWidth;

	private boolean isAlreadyOpened = false;

	private int alpha = 255; //230; // Disabled because this doesn't work via VNC (and possibly other places)

	private boolean isAnimating = false;

	/**
	 * A reference to the screen monitor that the shell is in;
	 * this is used to ensure that subsequent resizing or repositioning operations
	 * are performed relative to this monitor.
	 */
	private Monitor monitor = null;

	private boolean useCustomTrim = true;

	private Region region;

	private boolean isAlphaSupported = true;

	private UIFunctionsSWT uiFunctions;

	/**
	 * 
	 * @param parentShell
	 * @param borderWidth
	 */
	public StyledShell(Shell parentShell, int borderWidth) {
		this(parentShell, borderWidth, true);
	}

	/**
	 * 
	 * @param parentShell
	 * @param borderWidth
	 * @param useCustomTrim
	 */
	public StyledShell(Shell parentShell, int borderWidth, boolean useCustomTrim) {
		this.parentShell = parentShell;
		this.borderWidth = borderWidth;
		this.useCustomTrim = useCustomTrim;

		if (null == parentShell) {
			throw new NullPointerException("parentShell can not be null");
		}

		/*
		 * Quick check to see if alpha is supported
		 */
		try {
			parentShell.setAlpha(parentShell.getAlpha());
		} catch (Throwable t) {
			isAlphaSupported = false;
		}

		display = parentShell.getDisplay();

		if (true == useCustomTrim) {

			createCustomShell();

			/*
			 * Must dispose of the Region explicitly when we're done; region is only used when custom trim is used
			 */
			if (null != styledShell) {
				styledShell.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent e) {
						if (null != region && false == region.isDisposed()) {
							region.dispose();
						}
					}
				});
			}

		} else {
			createStandardShell();
		}

	}

	/**
	 * Creates a pop-up shell with out custom style and trim
	 */
	private void createCustomShell() {
		styledShell = ShellFactory.createShell(parentShell,getShellStyle(SWT.NONE));
		FillLayout fillLayout = new FillLayout();
		fillLayout.marginHeight = borderWidth;
		fillLayout.marginWidth = borderWidth;
		styledShell.setLayout(fillLayout);

		if (true == Constants.isOSX) {
			getUIFunctions().createMainMenu(styledShell);
		}

		borderedBackground = new Composite(styledShell, SWT.NONE);

		fillLayout = new FillLayout();
		fillLayout.marginHeight = borderWidth;
		fillLayout.marginWidth = borderWidth;
		borderedBackground.setLayout(fillLayout);

		content = new Composite(borderedBackground, SWT.DOUBLE_BUFFERED);
		content.setBackgroundMode(SWT.INHERIT_DEFAULT);

		setBackground(ColorCache.getColor(styledShell.getDisplay(), 38, 38, 38));
		content.setBackground(ColorCache.getColor(styledShell.getDisplay(), 13, 13,
				13));
		content.setForeground(ColorCache.getColor(styledShell.getDisplay(), 206,
				206, 206));

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
				e.gc.fillRectangle(new Rectangle(r, bounds.height - r,
						bounds.width - d, r));

				/*
				 * Rectangle to fill the area between the 2 bars created above
				 */
				e.gc.fillRectangle(new Rectangle(0, r, bounds.width, bounds.height - d));
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
				if (e.type == SWT.Resize) {
					styledShell.setRegion(getRoundedRegion(styledShell.getBounds()));
				}
			}
		};
		styledShell.addListener(SWT.KeyDown, l);
		styledShell.addListener(SWT.MouseDown, l);
		styledShell.addListener(SWT.MouseMove, l);
		styledShell.addListener(SWT.Resize, l);
		styledShell.setCursor(display.getSystemCursor(SWT.CURSOR_HAND));
	}

	/**
	 * Creates a pop-up shell with standard dialog style and trim
	 */
	private void createStandardShell() {
		styledShell = ShellFactory.createShell(parentShell, getShellStyle(SWT.NONE));
		FillLayout fillLayout = new FillLayout();
		fillLayout.marginHeight = 0;
		fillLayout.marginWidth = 0;
		styledShell.setLayout(fillLayout);

		if (true == Constants.isOSX) {
			getUIFunctions().createMainMenu(styledShell);
		}
		Utils.setShellIcon(styledShell);

		content = new Composite(styledShell, SWT.DOUBLE_BUFFERED);
		content.setBackgroundMode(SWT.INHERIT_DEFAULT);

		alpha = 255;

	}

	/**
	 * Returns the bit mask for the proper shell style
	 * @param style
	 * @return
	 */
	private int getShellStyle(int style) {
		/*
		 * If there are any other shell on top that also has a title then bring this shell on top of that
		 * so it is not obscured by the other shell(s); conversely DO NOT bring this shell on top if the 
		 * above condition is false so that it will not obscure other windows like external browser, etc...
		 */
		//		if (true == Utils.anyShellHaveStyle(SWT.ON_TOP | SWT.TITLE)) {
		//			UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
		//			if (uiFunctions != null && uiFunctions.getMainShell() != null) {
		//				style |= SWT.ON_TOP;
		//			}
		//		}
		/*
		 * 
		 * On non-osx set the NO_TRIM flag and on OSX ONLY set the NO_TRIM flag if setAlpha()
		 * is also supported.  Versions of SWT on OSX that do not support setAlpha() also can not render
		 * the embedded web page properly if the NO_TRIM flag is set; the NO_TRIM flag allows us to draw
		 * a round-cornered shell.  Without this flag the shell corners would just be the normal square angle. 
		 */
		if (true == useCustomTrim) {
			if (true == Constants.isOSX) {
				if (true == isAlphaSupported) {
					style |= SWT.NO_TRIM;
				}
			} else {
				style |= SWT.NO_TRIM;
			}

			return style;
		}
		return SWT.DIALOG_TRIM | SWT.RESIZE;
	}

	private Region getRoundedRegion(Rectangle bounds) {

		int r = borderWidth;
		int d = r * 2;

		/*
		 * Must explicitly dispose of any previous reference to a Region before assigning a new one
		 */
		if (null != region && false == region.isDisposed()) {
			region.dispose();
		}

		region = new Region();

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

	public void open() {
		if (true == isAlive()) {
			/*
			 * Get the monitor that the mouse cursor is in
			 */
			Point cursorLocation = display.getCursorLocation();
			monitor = Utils.getMonitor(cursorLocation);

			styledShell.open();
			isAlreadyOpened = true;
		}
	}

	public void close() {
		if (true == isAlive()) {
			styledShell.close();
			isAlreadyOpened = false;
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
		setSize(width, height, HINT_ALIGN_FIT_IN_MONITOR
				| (false == isAlreadyOpened ? HINT_ALIGN_CENTER : 0));
	}

	public void setSize(int width, int height, int hint) {
		if (true == isAlive()) {
			Rectangle outerBounds = styledShell.getBounds();

			if (true == useCustomTrim) {
				/*
				 * Compensating since the 2 outer custom borders extends beyond the content area
				 */
				width += borderWidth * 4;
				height += borderWidth * 4;
			} else {
				/*
				 * Compensating since the dialog trim extends beyond the bounds of the content area
				 */
				width += styledShell.getBounds().width
						- styledShell.getClientArea().width;
				height += styledShell.getBounds().height
						- styledShell.getClientArea().height;
			}

			if (outerBounds.width != width || outerBounds.height != height) {

				outerBounds.width = width;
				outerBounds.height = height;

				/*
				 * Centers the the StyleShell relative to main application window
				 */
				if ((hint & HINT_ALIGN_CENTER) != 0) {
					Utils.centerRelativeTo(outerBounds,
							getUIFunctions().getMainShell().getBounds());
				}

				/*
				 * Adjust the new bounds if the shell does not fully fit on the screen.
				 * If a monitor is already present then adjust the bounds relative to the monitor;
				 * otherwise make it relative to the monitor the cursor resides in
				 */
				if ((hint & HINT_ALIGN_FIT_IN_MONITOR) != 0) {
					if (null != monitor) {
						Utils.makeVisibleOnMonitor(outerBounds, monitor);
					} else {
						Utils.makeVisibleOnCursor(outerBounds);
					}
				}

				styledShell.setBounds(outerBounds);

				/*
				 * Only custom trim needs custom region
				 */
				if (true == useCustomTrim) {
					styledShell.setRegion(getRoundedRegion(outerBounds));
				}

				styledShell.forceActive();
			}
		}
	}

	public void centersShell() {
		if (true == isAlive()) {
			Rectangle bounds = styledShell.getBounds();
			Utils.centerRelativeTo(bounds,
					getUIFunctions().getMainShell().getBounds());
			styledShell.setBounds(bounds);
		}
	}

	public void makeShellVisible() {
		if (true == isAlive()) {
			/*
			 * Adjust the new bounds if the shell does not fully fit on the screen.
			 * If a monitor is already present then adjust the bounds relative to the monitor;
			 * otherwise make it relative to the monitor the cursor resides in
			 */
			Rectangle bounds = styledShell.getBounds();
			if (null != monitor) {
				Utils.makeVisibleOnMonitor(bounds, monitor);
			} else {
				Utils.makeVisibleOnCursor(bounds);
			}
			styledShell.setBounds(bounds);
		}
	}

	/**
	 * Animates the visibility of the shell from 0 to the appropriate alpha level; this creates the effect
	 * of a pop-up fading into view
	 * @param milliSeconds
	 */
	public void animateFade(final int milliSeconds) {
		if (false == isAlive() || true == isAnimating || false == isAlphaSupported) {
			return;
		}
		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				if (!isAlive()) {
					return;
				}
				isAnimating = true;
				try {
					int seconds = milliSeconds;
					int currentAlpha = 0;
					int delay = 3;
					int sleepIncrement = milliSeconds / (10 + delay);
					if (true == isAlive()) {
						setAlpha(styledShell, currentAlpha);
						styledShell.setVisible(true);
					}
					while (seconds > 0) {
						Thread.sleep(sleepIncrement);
						seconds -= (sleepIncrement);
						if (true == isAlive()) {
							/*
							 * We don't update the alpha for a few cycles to allow the shell to initialize it's content
							 * while still remaining invisible
							 */
							if (delay <= 0) {
								setAlpha(styledShell, Math.min(currentAlpha, alpha));
								currentAlpha += 20;
							}
							delay--;
						} else {
							break;
						}
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					if (true == isAlive()) {
						setAlpha(styledShell, alpha);
					}
					isAnimating = false;
					styledShell.forceActive();
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

	public boolean isAlreadyOpened() {
		return isAlreadyOpened;
	}

	public void setBackground(Color color) {
		styledShell.setBackground(color);
	}

	public int getAlpha() {
		return alpha;
	}

	public void setAlpha(int alpha) {
		this.alpha = alpha;
	}

	public void hideShell(boolean value) {
		if (true == value) {
			setAlpha(styledShell, 0);
		} else {
			setAlpha(styledShell, alpha);
		}
	}

	public String getText() {

		return true == isAlive() ? styledShell.getText() : null;
	}

	public void setText(String string) {
		if (true == isAlive()) {
			styledShell.setText(string);
		}
	}

	public boolean isUseCustomTrim() {
		return useCustomTrim;
	}

	private UIFunctionsSWT getUIFunctions() {
		if (null == uiFunctions) {
			uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
			if (null == uiFunctions) {
				throw new NullPointerException(
						"An initialized instance of UIFunctionsSWT is required to create a LightBoxShell");
			}
		}
		return uiFunctions;
	}

	private void setAlpha(Shell shell, int alpha) {
		if (true == isAlphaSupported && null != shell) {
			shell.setAlpha(alpha);
		}
	}

	public Rectangle getBounds() {
		return styledShell.getBounds();
	}
}
