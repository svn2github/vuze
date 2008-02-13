package com.aelitis.azureus.ui.swt.shells;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.*;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.LightBoxShell;
import org.gudy.azureus2.ui.swt.components.shell.LightBoxShell.StyledShell;
import org.gudy.azureus2.ui.swt.mainwindow.IMainWindow;

import com.aelitis.azureus.core.messenger.ClientMessageContext;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.browser.BrowserContext;
import com.aelitis.azureus.ui.swt.browser.listener.ConfigListener;
import com.aelitis.azureus.ui.swt.browser.listener.LightBoxBrowserRequestListener;

/**
 * A window with the lightbox effect hosting a browser widget in a stylized shell
 * 
 * @author knguyen
 *
 */
public class LightBoxBrowserWindow
{

	private String title = "";

	private String url = null;

	private String prefixVerifier = null;

	private StackLayout stack = new StackLayout();

	private Composite errorPanel;

	private Composite contentPanel;

	private Browser browser;

	private StyledShell styledShell;

	private LightBoxShell lightBoxShell;

	private int browserWidth = 400;

	private int browserHeight = 300;

	public LightBoxBrowserWindow(String url, String prefixVerifier, int width,
			int height) {
		this.url = url;
		this.prefixVerifier = prefixVerifier;
		browserWidth = Math.max(width, browserWidth);
		browserHeight = Math.max(height, browserHeight);
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				init();
			}
		});

	}

	public LightBoxBrowserWindow(String url, int width, int height) {
		this(url, null, width, height);
	}

	public LightBoxBrowserWindow(String url) {
		this(url, null, 0, 0);
	}

	public void init() {
		UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
		if (null == uiFunctions) {
			throw new IllegalStateException(
					"No instance of UIFunctionsSWT found; the UIFunctionsManager might not have been initialized properly");
		}

		lightBoxShell = new LightBoxShell(true);

		/*
		 * Specify the bottom inset to exclude the statusbar from the lightbox coverage area
		 */
		IMainWindow mainWindow = uiFunctions.getMainWindow();
		Rectangle r = mainWindow.getMetrics(IMainWindow.WINDOW_ELEMENT_STATUSBAR);
		lightBoxShell.setInsets(0, r.height, 0, 0);

		/*
		 * Create the StyledShell to host the browser
		 */
		styledShell = lightBoxShell.createStyledShell(10, true);

		/*
		 * Sets the cursor to busy since loading the light box and accompanying browser can take some time;
		 * we set it back to normal in the ProgressListener for the Browser below
		 */
		styledShell.setCursor(Display.getCurrent().getSystemCursor(SWT.CURSOR_WAIT));

		/*
		 * Use a StackLayout with an error panel in the background so we can switch it to the front
		 * when an error has occurred
		 */
		contentPanel = styledShell.getContent();
		contentPanel.setLayout(stack);
		errorPanel = new Composite(contentPanel, SWT.NONE);
		errorPanel.setBackground(new Color(null, 13, 13, 13));

		/*
		 * The Browser widget is very platform-dependent and can only support a limited set
		 * of native browsers; if a platform is not supported or no compatible native browser
		 * is found then the browser will throw an exception.
		 * 
		 * If we can not instantiate a browser we will still bring up the window but show an
		 * error panel instead of the browser
		 * 
		 */
		try {
			browser = new Browser(contentPanel, SWT.NONE);
		} catch (Throwable t) {
			//TODO:  show no browser error
		}

		if (browserWidth > 0 && browserHeight > 0) {
			styledShell.setSize(browserWidth, browserHeight);
		}

		if (null != browser) {
			hookListeners();
			setUrl(url);
			stack.topControl = browser;
		}

		contentPanel.layout();

	}

	private void hookListeners() {

		/*
		 * Capture the title of the loaded page so we can use it to verify the prefix
		 */
		browser.addTitleListener(new TitleListener() {
			public void changed(TitleEvent event) {
				title = event.title;
			}
		});

		/*
		 * If a java script 'window.close' message is detected from the loaded page
		 * then close the lightbox
		 */
		browser.addCloseWindowListener(new CloseWindowListener() {
			public void close(WindowEvent event) {
				lightBoxShell.close();
			}
		});

		/*
		 * When the page has finished loading verify its title prefix and show the error panel
		 * it the verification failed.
		 */
		browser.addProgressListener(new ProgressListener() {
			public void completed(ProgressEvent event) {

				stack.topControl = browser;

				/*
				 * If a prefixVerifier is specified then verify the loaded page
				 */
				if (null != prefixVerifier) {
					if (null == title || false == title.startsWith(prefixVerifier)) {
						stack.topControl = errorPanel;
					}
				}
				contentPanel.layout();
				lightBoxShell.open(styledShell);

			}

			public void changed(ProgressEvent event) {
				if (event.current == event.total) {
					styledShell.setCursor(Display.getCurrent().getSystemCursor(
							SWT.CURSOR_ARROW));
				}
			}
		});

		/*
		 * Add the appropriate messaging listeners
		 */
		final ClientMessageContext context = new BrowserContext(
				"lightbox-browser-window" + Math.random(), browser, null, true);

		/*
		 * This listener is for when the loaded page is requesting some info
		 */
		context.addMessageListener(new ConfigListener(browser));

		/*
		 * This listener will respond to actions that effects this window such as 'close', 'resize', etc...
		 */
		context.addMessageListener(new LightBoxBrowserRequestListener(this));

	}

	public void close() {
		lightBoxShell.close();
	}

	public void refresh() {
		if (null != browser) {
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					browser.refresh();
				}
			});
		}
	}

	public void setUrl(String url) {
		if (null != browser) {
			browser.setUrl(url);
			browser.setData("StartURL", url);
		}
	}

	public void setSize(int width, int height) {
		browserWidth = Math.max(width, 320);
		browserHeight = Math.max(height, 240);
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				styledShell.setSize(browserWidth, browserHeight);
			}
		});

	}
}
