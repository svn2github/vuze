package org.gudy.azureus2.ui.swt.components.shell;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.IMainWindow;

import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

public class LightBoxShell
{

	private Shell lbShell = null;

	private Shell parentShell = null;

	private int insetTop = 0;

	private int insetBottom = 0;

	private int insetLeft = 0;

	private int insetRight = 0;

	private boolean closeOnESC = false;

	private boolean isAlreadyOpened = false;

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

		parentShell = getUIFunctions().getMainShell();

		if (null == parentShell) {
			return;
		}
		IMainWindow mainWindow = getUIFunctions().getMainWindow();
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
		if (null == parentShell) {
			return;
		}

		lbShell = new Shell(parentShell, SWT.NO_TRIM | SWT.APPLICATION_MODAL);

		if (true == Constants.isOSX) {
			getUIFunctions().createMainMenu(lbShell);
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

		/*
		 * For OSX add this listener to make sure that the parent shell and
		 * the lighbox shell behave like they are sandwiched together; without this
		 * then external applications can slide in between the parent shell and the
		 * lightbox which creates a strange visual effect 
		 */
		if (true == Constants.isOSX) {
			lbShell.addShellListener(new ShellAdapter() {
				public void shellActivated(ShellEvent e) {
					if (null != parentShell && false == parentShell.isDisposed()) {
						parentShell.forceActive();
					}
				}
			});
		}

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

	public void open() {
		if (null != lbShell && false == lbShell.isDisposed()) {
			lbShell.setBounds(getBounds());
			isAlreadyOpened = true;

			/*
			 * Black mask with 30% transparency
			 */
			lbShell.setBackground(Colors.black);

			try {
				lbShell.setAlpha(178);
			} catch (Throwable t) {
				//Do nothing if alpha is not supported
			}

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
	public Rectangle getBounds() {
		/*
		 * Not entirely sure why this has to be done this way but it seems
		 * the Windows' shell has a 4 pixel border whereas the OSX's shell has none;
		 * this offset is used to shift the image to fit the client area exactly
		 */

		int xyOffset = (true == Constants.isOSX) ? 0 : 4;

		Rectangle fadedAreaExtent = parentShell.getClientArea();
		Point parentLocation = parentShell.getLocation();
		fadedAreaExtent.x = parentLocation.x + xyOffset + insetLeft;
		fadedAreaExtent.y = parentLocation.y + parentShell.getSize().y
				- fadedAreaExtent.height - xyOffset + insetTop;
		fadedAreaExtent.width -= insetRight + insetLeft;
		fadedAreaExtent.height -= insetTop + insetBottom;
		return fadedAreaExtent;
	}

	/**
	 * 
	 * Creates a stylized shell with pre-defined look and feel
	 * @param borderWidth
	 * @param closeLightboxOnExit if <code>true</code> then close the parent lightbox when this pop-up is closed; otherwise leave it opened
	 * @return
	 */
	public StyledShell createPopUpShell(int borderWidth,
			boolean closeLightboxOnExit) {
		return createPopUpShell(borderWidth, closeLightboxOnExit, true);
	}

	/**
	 * Creates a stylized shell with pre-defined look and feel
	 * @param borderWidth is used for the width of the custom dialog trim; is not in effect if standard trim is specified
	 * @param closeLightboxOnExit if <code>true</code> then close the parent lightbox when this pop-up is closed; otherwise leave it opened
	 * @param useCustomTrim if <code>true</code> use our custom dialog trim; otherwise use default OS dialog trims 
	 * @return
	 */
	public StyledShell createPopUpShell(int borderWidth,
			boolean closeLightboxOnExit, boolean useCustomTrim) {
		StyledShell newShell = new StyledShell(lbShell, borderWidth, useCustomTrim);

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

			if (false == isAlreadyOpened) {
				open();
			}

			if (false == shellToOpen.isAlreadyOpened()) {
				shellToOpen.open();
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

	public Display getDisplay() {
		return display;
	}

	public boolean isAlreadyOpened() {
		return isAlreadyOpened;
	}

	public void addDisposeListener(DisposeListener listener) {
		lbShell.addDisposeListener(listener);
	}

}
