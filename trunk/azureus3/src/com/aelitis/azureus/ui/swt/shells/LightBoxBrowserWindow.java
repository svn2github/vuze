package com.aelitis.azureus.ui.swt.shells;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.*;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.LightBoxShell;
import org.gudy.azureus2.ui.swt.components.shell.StyledShell;
import org.gudy.azureus2.ui.swt.components.widgets.BubbleButton;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;

import com.aelitis.azureus.core.messenger.ClientMessageContext;
import com.aelitis.azureus.core.messenger.browser.BrowserMessage;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.browser.BrowserContext;
import com.aelitis.azureus.ui.swt.browser.listener.ConfigListener;
import com.aelitis.azureus.ui.swt.browser.listener.DisplayListener;
import com.aelitis.azureus.ui.swt.browser.listener.LightBoxBrowserListener;
import com.aelitis.azureus.util.ConstantsVuze;

/**
 * A window with the lightbox effect hosting a browser widget in a stylized shell
 * 
 * @author knguyen
 *
 */
public class LightBoxBrowserWindow
{
	private static boolean CUSTOM_TRIM = true;

	private static boolean DIM = false;

	private String url = null;

	private String pageVerifierValue = ConstantsVuze.URL_PAGE_VERIFIER_VALUE;

	private StackLayout stack = new StackLayout();

	private Composite errorPanel;

	private Composite contentPanel;

	private Browser browser;

	private StyledShell styledShell;

	private LightBoxShell lightBoxShell;

	private int browserWidth = 0;

	private int browserHeight = 0;

	private Label errorMessageLabel;

	private String redirectURL = null;

	private Color borderColor = null;

	private Color contentBackgroundColor = null;

	private UIFunctionsSWT uiFunctions;

	private closeListener closeListener;

	private String callback;

	private ClientMessageContext callBackContext;

	private Composite waitPanel;

	private Label waitMessageLabel;

	public LightBoxBrowserWindow(String url, String prefixVerifier, int width,
			int height) {
		this.url = url;
		this.pageVerifierValue = prefixVerifier;
		browserWidth = width;
		browserHeight = height;

		Utils.execSWTThread(new Runnable() {
			public void run() {
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
		uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
		if (null == uiFunctions) {
			throw new IllegalStateException(
					"No instance of UIFunctionsSWT found; the UIFunctionsManager might not have been initialized properly");
		}

		lightBoxShell = new LightBoxShell(true);
		//Shell mainShell = uiFunctions.getMainShell();
		
		borderColor = new Color(lightBoxShell.getDisplay(), 38, 38, 38);
		contentBackgroundColor = new Color(lightBoxShell.getDisplay(), 13, 13, 13);

		/*
		 * Create the StyledShell to host the browser
		 */
		styledShell = lightBoxShell.createPopUpShell(6, true, CUSTOM_TRIM);

		styledShell.setBackground(borderColor);

		styledShell.addListener(SWT.Traverse, new Listener() {
			public void handleEvent(Event e) {
				if (e.detail == SWT.TRAVERSE_ESCAPE) {
					e.doit = false;
					close();
				}
			}
		});
		
		final Listener escListener = new Listener() {
			public void handleEvent(Event event) {
				if (event.keyCode == 27) {
					close();
				}
			}
		};
		styledShell.getShell().getDisplay().addFilter(SWT.KeyDown, escListener);
		styledShell.addListener(SWT.Dispose, new Listener() {
			public void handleEvent(Event event) {
				event.display.removeFilter(SWT.KeyDown, escListener);
			}
		});
		
		
		/*
		 * Use a StackLayout with an error panel in the background so we can switch it to the front
		 * when an error has occurred
		 */
		contentPanel = styledShell.getContent();
		stack.marginHeight = 0;
		stack.marginWidth = 0;
		contentPanel.setLayout(stack);
		contentPanel.setBackground(contentBackgroundColor);

		if (CUSTOM_TRIM) {
  		waitPanel = new Composite(contentPanel, SWT.NONE);
  		waitPanel.setBackground(contentBackgroundColor);
  		waitPanel.setLayout(new FormLayout());
  		waitPanel.setBackgroundMode(SWT.INHERIT_DEFAULT);
  		waitMessageLabel = new Label(waitPanel, SWT.CENTER);
  		waitMessageLabel.setBackground(waitPanel.getBackground());
  		waitMessageLabel.setForeground(Colors.grey);
  		waitMessageLabel.setLayoutData(Utils.getFilledFormData());
  		Messages.setLanguageText(waitMessageLabel, "v3.MainWindow.Loading");
		} else {
			contentPanel.getShell().setText(MessageText.getString("v3.MainWindow.Loading"));
		}

		
		errorPanel = new Composite(contentPanel, SWT.NONE);
		errorPanel.setBackground(contentBackgroundColor);
		errorPanel.setLayout(new FormLayout());
		errorPanel.setBackgroundMode(SWT.INHERIT_DEFAULT);
		errorMessageLabel = new Label(errorPanel, SWT.WRAP);
		errorMessageLabel.setBackground(errorPanel.getBackground());
		errorMessageLabel.setForeground(Colors.grey);

		BubbleButton closeButton = new BubbleButton(errorPanel);
		closeButton.setText(MessageText.getString("wizard.close"));
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

		/*
		 * This close button is only on the error panel
		 */
		closeButton.addMouseListener(new MouseAdapter() {

			public void mouseDown(MouseEvent e) {
				close();
			}

		});
		//		closeButton.addSelectionListener(new SelectionListener() {
		//			public void widgetSelected(SelectionEvent e) {
		//				widgetDefaultSelected(e);
		//			}
		//
		//			public void widgetDefaultSelected(SelectionEvent e) {
		//				close();
		//			}
		//
		//		});

		/*
		 * Disposing resources when the LightBoxShell is disposed
		 */
		lightBoxShell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				if (null != borderColor && false == borderColor.isDisposed()) {
					borderColor.dispose();
				}
				if (null != contentBackgroundColor
						&& false == contentBackgroundColor.isDisposed()) {
					contentBackgroundColor.dispose();
				}
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
			browser = new Browser(contentPanel,
					Utils.getInitialBrowserStyle(SWT.NONE));
		} catch (Throwable t) {
			// Be silent if no browser
		}

		if (browserWidth > 0 && browserHeight > 0) {
			styledShell.setSize(browserWidth, browserHeight,
					StyledShell.HINT_ALIGN_CENTER | StyledShell.HINT_ALIGN_CENTER);
		}

		contentPanel.layout();

		/*
		 * Make sure the main shell is not minimized when the lightbox is opened
		 */
		if (true == uiFunctions.getMainShell().getMinimized()) {
			uiFunctions.getMainShell().setMinimized(false);
		}
		
		if (CUSTOM_TRIM) {
  		stack.topControl = waitPanel;
  		Point computeSize = waitPanel.computeSize(browserWidth > 0 ? browserWidth : 400, 0);
  		Point size = styledShell.getShell().computeSize(computeSize.x, computeSize.y);
  		styledShell.setSize(size.x, size.y + 15);
  		styledShell.centersShell();
		} else {
  		styledShell.setSize(browserWidth > 0 ? browserWidth : 400, 0);
  		styledShell.centersShell();
		}


		styledShell.hideShell(true);
		Utils.execSWTThreadLater(1000, new AERunnable() {
			public void runSupport() {
				if (!styledShell.getShell().isDisposed()) {
					styledShell.hideShell(false);
				}
			}
		});
		lightBoxShell.open(styledShell, DIM);

		if (null != browser) {
			hookListeners();
			setUrl(url);
			stack.topControl = browser;
		} else {
			stack.topControl = errorPanel;
		}
	}

	private void hookListeners() {

		/*
		 * If a java script 'window.close' message is detected from the loaded page
		 * then close the lightbox
		 */
		browser.addCloseWindowListener(new CloseWindowListener() {
			public void close(WindowEvent event) {
				LightBoxBrowserWindow.this.close();
			}
		});
		
		browser.addLocationListener(new LocationListener() {
		
			public void changing(LocationEvent event) {
				lightBoxShell.showBusy(true, 500);
			}
		
			public void changed(LocationEvent event) {
				// TODO Auto-generated method stub
		
			}
		});

		/*
		 * Once a page has finished loading make sure again that the main shell is not minimized
		 * then fade the styledShell back into being visible
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

						String errorMessage = MessageText.getString("Browser.popup.error.no.access");
						errorMessageLabel.setText(errorMessage);
						if (false == stack.topControl.equals(errorPanel)) {
							stack.topControl = errorPanel;
							setSize(300, 200);
							contentPanel.layout();
						}
					}
				}

				/*
				 * Once a page has finished loading make sure the main shell is not minimized
				 * then fade the styledShell back into being visible
				 */
				if (true == uiFunctions.getMainShell().getMinimized()) {
					uiFunctions.getMainShell().setMinimized(false);
				}

				lightBoxShell.showBusy(false, 0);
				//styledShell.animateFade(100);
				if (browserWidth > 0 && browserHeight > 0) {
					styledShell.setSize(browserWidth, browserHeight,
							StyledShell.HINT_ALIGN_CENTER | StyledShell.HINT_ALIGN_CENTER);
				}
				styledShell.hideShell(false);
			}

			public void changed(ProgressEvent event) {
				/*
				 * When ever the URL changes hide the shell to eliminate flickering then
				 * show it again once the page has finished loading
				 */
				if (event.current == 0 && event.total != 0) {
					//styledShell.hideShell(true);
					lightBoxShell.showBusy(true, 500);
				}
			}
		});

		/*
		 * If not using custom trim for the shell then we must display the title since the standard
		 * dialog trim has the title bar
		 */
		if (false == styledShell.isUseCustomTrim()) {
			browser.addTitleListener(new TitleListener() {

				public void changed(TitleEvent event) {
					styledShell.setText(event.title);

				}

			});
		}
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
		context.addMessageListener(new LightBoxBrowserListener(this) {
			public void handleMessage(BrowserMessage message) {
				super.handleMessage(message);
				String opID = message.getOperationId();
				decodedMap = message.getDecodedMap();
				if (true == OP_INVOKE_CALLBACK.equals(opID) && callBackContext != null && callback != null) { 
					callBackContext.executeInBrowser(callback);
				}
			}
		});

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
		if (closeListener != null) {
			closeListener.close();
		}
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
		browserWidth = width;
		browserHeight = height;
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				styledShell.getShell().setRedraw(false);
				styledShell.setSize(browserWidth, browserHeight);
				styledShell.centersShell();
				styledShell.getShell().setRedraw(true);
			}
		});

	}

	public String getRedirectURL() {
		return redirectURL;
	}

	public void setRedirectURL(String redirectURL) {
		this.redirectURL = redirectURL;
	}

	public void setCloseListener(closeListener l) {
		closeListener = l;
	}

	public interface closeListener
	{
		public void close();
	}

	/**
	 * @param callback
	 * @param clientMessageContext 
	 *
	 * @since 4.0.0.5
	 */
	public void setCallback(String callback, ClientMessageContext clientMessageContext) {
		this.callback = callback;
		this.callBackContext = clientMessageContext;
	}
}
