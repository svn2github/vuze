package com.aelitis.azureus.ui.swt.shells;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.*;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.LightBoxShell;
import org.gudy.azureus2.ui.swt.components.shell.LightBoxShell.StyledShell;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.IMainWindow;

import com.aelitis.azureus.core.messenger.ClientMessageContext;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.browser.BrowserContext;
import com.aelitis.azureus.ui.swt.browser.listener.ConfigListener;
import com.aelitis.azureus.ui.swt.browser.listener.DisplayListener;
import com.aelitis.azureus.ui.swt.browser.listener.LightBoxBrowserRequestListener;
import com.aelitis.azureus.util.Constants;

/**
 * A window with the lightbox effect hosting a browser widget in a stylized shell
 * 
 * @author knguyen
 *
 */
public class LightBoxBrowserWindow
{

	private String url = null;

	private String pageVerifierValue = Constants.URL_PAGE_VERIFIER_VALUE;

	private StackLayout stack = new StackLayout();

	private Composite errorPanel;

	private Composite contentPanel;

	private Browser browser;

	private StyledShell styledShell;

	private LightBoxShell lightBoxShell;

	private int browserWidth = 0;

	private int browserHeight = 0;

	private Label errorMessageLabel;

	public LightBoxBrowserWindow(String url, String prefixVerifier, int width,
			int height) {
		this.url = url;
		this.pageVerifierValue = prefixVerifier;
		browserWidth = width;
		browserHeight = height;

		init();
		
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
		styledShell = lightBoxShell.createStyledShell(6, true);

		styledShell.setBackground(new Color(lightBoxShell.getDisplay(), 38, 38, 38));
		/*
		 * Use a StackLayout with an error panel in the background so we can switch it to the front
		 * when an error has occurred
		 */
		contentPanel = styledShell.getContent();
		stack.marginHeight = 0;
		stack.marginWidth = 0;
		contentPanel.setLayout(stack);
		contentPanel.setBackground(new Color(null, 13, 13, 13));
		errorPanel = new Composite(contentPanel, SWT.NONE);
		errorPanel.setBackground(new Color(null, 13, 13, 13));
		errorPanel.setLayout(new FormLayout());

		errorMessageLabel = new Label(errorPanel, SWT.WRAP);
		errorMessageLabel.setBackground(errorPanel.getBackground());
		errorMessageLabel.setForeground(Colors.grey);

		Button closeButton = new Button(errorPanel, SWT.NONE);
		closeButton.setText("Close");

		FormData fData = new FormData();
		fData.width = 100;
		fData.bottom = new FormAttachment(100, -20);
		fData.right = new FormAttachment(100, -20);
		closeButton.setLayoutData(fData);

		fData = new FormData();
		fData.top = new FormAttachment(0, 0);
		fData.left = new FormAttachment(0, 0);
		fData.bottom = new FormAttachment(closeButton, 0);
		fData.right = new FormAttachment(100, -20);

		errorMessageLabel.setLayoutData(fData);

		closeButton.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				widgetDefaultSelected(e);
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				close();
			}

		});

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
			// Be silent if no browser
		}

		if (browserWidth > 0 && browserHeight > 0) {
			styledShell.setSize(browserWidth, browserHeight, true);
		}

		if (null != browser) {
			hookListeners();
			setUrl(url);
			stack.topControl = browser;
		} else {
			stack.topControl = errorPanel;
		}

		contentPanel.layout();
		lightBoxShell.open();
	}

	private void hookListeners() {

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
		 * if the verification failed.
		 */
		browser.addProgressListener(new ProgressListener() {
			public void completed(ProgressEvent event) {

				/*
				 * If a prefixVerifier is specified then verify the loaded page
				 */
				if (null != pageVerifierValue) {

					String browserText = null;

					try {
						browserText = browser.getText();
					} catch (Throwable t) {
						/*
						 * KN: Do nothing for verification if Browser.getText() is not found;
						 * this could be due the the SWT jar being of an older version
						 */
					}
					if (null != browserText && false == isPageVerified(browser.getText())) {

						String errorMessage = "An error has occured while attempting to access:\n";
						errorMessage += browser.getUrl() + "\n\n";
						errorMessage += "Please try again at a later time.\n";
						errorMessageLabel.setText(errorMessage);
						if (false == stack.topControl.equals(errorPanel)) {
							stack.topControl = errorPanel;
							contentPanel.layout();
						}
					}
				}
				styledShell.animateFade(100);
				if (false == styledShell.isAlreadyOpened()) {
					lightBoxShell.open(styledShell);
				}
			}

			public void changed(ProgressEvent event) {
				if(event.current == 0 && event.total != 0){
					styledShell.setVisible(false);
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

		/*
		 * This listener handles a number of tasks involving opening external browser,
		 * popping up traditional browser dialogs, and navigating to embedded browser
		 * pages in the client application
		 */
		context.addMessageListener(new DisplayListener(null));

	}

	/**
	 * Returns whether the given block of html contains the predefined '<INPUT...' block
	 * This is used to detect whether the url given was resolved and loaded properly, or
	 * whether an error may have prevented the page from loading fully.
	 * 
	 * NOTE: This is not a security implementation and no reliance should be made that
	 * this check is enough to prevent spoofing, etc...
	 * 
	 * @param html
	 * @return
	 */
	private boolean isPageVerified(String html) {
		if (null == html || html.length() < 1) {
			return false;
		}

		//		String fullSearchString = "<INPUT type=hidden value=" + pageVerifierValue
		//				+ " name=pageVerifier>";

		//TODO: WARNING!!!!! this has been temporarily hardcoded
		if (html.indexOf("vuzePage") != -1) {
			return true;
		}
		return false;
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
		} else {
			System.out.println("Browser is null????");//KN: sysout
		}
	}

	public void setSize(int width, int height) {
		browserWidth = width;
		browserHeight = height;
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
//				styledShell.animateFade(500);
				styledShell.setSize(browserWidth, browserHeight);
			}
		});

	}
}
