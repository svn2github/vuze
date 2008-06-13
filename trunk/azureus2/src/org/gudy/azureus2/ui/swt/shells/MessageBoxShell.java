package org.gudy.azureus2.ui.swt.shells;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.OpenWindowListener;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.browser.WindowEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;
import org.gudy.azureus2.ui.swt.components.shell.StyledShell;
import org.gudy.azureus2.ui.swt.components.widgets.BubbleButton;
import org.gudy.azureus2.ui.swt.mainwindow.ClipboardCopy;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter.URLInfo;

import com.aelitis.azureus.ui.UIFunctionsUserPrompter;
import com.aelitis.azureus.ui.common.RememberedDecisionsManager;
import com.aelitis.azureus.ui.swt.UISkinnableManagerSWT;
import com.aelitis.azureus.ui.swt.UISkinnableSWTListener;
import com.aelitis.azureus.ui.swt.utils.ColorCache;

/**
 * A messagebox that allows you config the button
 * 
 * @todo When key is pressed, cancel auto close timer
 */
public class MessageBoxShell
	implements UIFunctionsUserPrompter
{
	private final static int MIN_SIZE_X = 300;

	private final static int MIN_SIZE_Y = 120;

	private final static int MAX_SIZE_X = 500;

	private static final int MIN_BUTTON_SIZE = 70;

	private static int numOpen = 0;

	private Shell parent;

	private final String title;

	private final String text;

	private final String[] buttons;

	private final int defaultOption;

	private String rememberID;

	private String rememberText;

	private boolean rememberByDefault;

	private int rememberOnlyIfButton = -1;

	private int autoCloseInMS;

	private String html;

	private String url;

	private boolean autoClosed = false;

	private Object[] relatedObjects;

	private Image imgLeft;

	protected Color urlColor;

	private boolean handleHTML = true;

	public static int open(final Shell parent, final String title,
			final String text, final String[] buttons, final int defaultOption) {
		return open(parent, title, text, buttons, defaultOption, null, false, -1);
	}

	public static int open(final Shell parent, final String title,
			final String text, final String[] buttons, final int defaultOption,
			final String rememberID, final boolean bRememberByDefault,
			final int autoCloseInMS) {
		return open(parent, title, text, buttons, defaultOption, rememberID,
				MessageText.getString("MessageBoxWindow.rememberdecision"),
				bRememberByDefault, autoCloseInMS);
	}

	public static int open(final Shell parent, final String title,
			final String text, final String[] buttons, final int defaultOption,
			final String rememberID, final String rememberText,
			final boolean bRememberByDefault, final int autoCloseInMS) {

		MessageBoxShell messageBoxShell = new MessageBoxShell(parent, title, text,
				buttons, defaultOption, rememberID, rememberText, bRememberByDefault,
				autoCloseInMS);
		return messageBoxShell.open();
	}

	public static boolean isOpen() {
		return numOpen > 0;
	}

	/**
	 * @param parent
	 * @param title
	 * @param text
	 * @param buttons
	 * @param defaultOption
	 * @param rememberID
	 * @param rememberText
	 * @param bRememberByDefault
	 * @param autoCloseInMS
	 */
	public MessageBoxShell(final Shell parent, final String title,
			final String text, final String[] buttons, final int defaultOption,
			final String rememberID, final String rememberText,
			final boolean bRememberByDefault, final int autoCloseInMS) {

		this.parent = parent;
		this.title = title;
		this.text = text;
		this.buttons = buttons;
		this.defaultOption = defaultOption;
		this.rememberID = rememberID;
		this.rememberText = rememberText;
		this.rememberByDefault = bRememberByDefault;
		this.autoCloseInMS = autoCloseInMS;
	}

	/**
	 * @param shellForChildren
	 * @param string
	 * @param string2
	 * @param strings
	 */
	public MessageBoxShell(final Shell parent, final String title,
			final String text, final String[] buttons, final int defaultOption) {
		this(parent, title, text, buttons, defaultOption, null, null, false, -1);
	}

	public int open() {
		return open(COConfigurationManager.getBooleanParameter("useNewStyleMessageBox"));
	}

	public int open(final boolean useCustomShell) {
		if (rememberID != null) {
			int rememberedDecision = RememberedDecisionsManager.getRememberedDecision(rememberID);
			if (rememberedDecision >= 0) {
				return rememberedDecision;
			}
		}

		numOpen++;

		final int[] result = new int[1];
		result[0] = -1;

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (true == useCustomShell) {
					result[0] = _open2();
				} else {
					result[0] = _open();
				}
			}
		}, false);

		numOpen--;
		return result[0];
	}

	private int _open() {
		final int[] result = {
			-1
		};

		if (parent == null || parent.isDisposed()) {
			parent = Utils.findAnyShell();
			if (parent == null || parent.isDisposed()) {
				return result[0];
			}
		}

		MouseTrackAdapter mouseAdapter = null;
		Display display = parent.getDisplay();

		final Shell shell = ShellFactory.createShell(parent, SWT.DIALOG_TRIM
				| SWT.RESIZE | SWT.APPLICATION_MODAL);
		shell.setText(title);
		shell.setBackgroundMode(SWT.INHERIT_DEFAULT);

		GridLayout gridLayout = new GridLayout();
		shell.setLayout(gridLayout);
		Utils.setShellIcon(shell);

		UISkinnableSWTListener[] listeners = UISkinnableManagerSWT.getInstance().getSkinnableListeners(
				MessageBoxShell.class.toString());
		for (int i = 0; i < listeners.length; i++) {
			listeners[i].skinBeforeComponents(shell, this, relatedObjects);
		}

		FormData formData;
		GridData gridData;

		Composite textComposite = shell;
		if (imgLeft != null) {
			textComposite = new Composite(shell, SWT.NONE);
			textComposite.setForeground(shell.getForeground());
			textComposite.setLayout(new GridLayout(2, false));
			textComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
			Label lblImage = new Label(textComposite, SWT.NONE);
			lblImage.setImage(imgLeft);
			lblImage.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		}

		Control linkControl;
		linkControl = createLinkLabel(textComposite, text);

		if ((html != null && html.length() > 0)
				|| (url != null && url.length() > 0)) {
			try {
				final Browser browser = new Browser(shell,
						Utils.getInitialBrowserStyle(SWT.NONE));
				if (url != null && url.length() > 0) {
					browser.setUrl(url);
				} else {
					browser.setText(html);
				}
				GridData gd = new GridData(GridData.FILL_BOTH);
				gd.heightHint = 200;
				browser.setLayoutData(gd);
				browser.addProgressListener(new ProgressListener() {
					public void completed(ProgressEvent event) {
						browser.addLocationListener(new LocationListener() {
							public void changing(LocationEvent event) {
								event.doit = false;
							}

							public void changed(LocationEvent event) {
							}
						});
						browser.addOpenWindowListener(new OpenWindowListener() {
							public void open(WindowEvent event) {
								event.required = true;
							}
						});
					}

					public void changed(ProgressEvent event) {
					}
				});
			} catch (Exception e) {
				Debug.out(e);
				if (html != null) {
					Text text = new Text(shell, SWT.BORDER | SWT.READ_ONLY | SWT.WRAP);
					text.setText(html);
					GridData gd = new GridData(GridData.FILL_BOTH);
					gd.heightHint = 200;
					text.setLayoutData(gd);
				}
			}

			gridData = new GridData(GridData.FILL_HORIZONTAL);
			linkControl.setLayoutData(gridData);
		} else {
			gridData = new GridData(GridData.FILL_BOTH);
			linkControl.setLayoutData(gridData);
		}

		Label lblPadding = new Label(shell, SWT.NONE);
		lblPadding.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// Closing in..
		if (autoCloseInMS > 0) {
			final Label lblCloseIn = new Label(shell, SWT.WRAP);
			lblCloseIn.setForeground(shell.getForeground());
			lblCloseIn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			long endOn = SystemTime.getCurrentTime() + autoCloseInMS;
			lblCloseIn.setData("CloseOn", new Long(endOn));
			SimpleTimer.addPeriodicEvent("autoclose", 500, new TimerEventPerformer() {
				public void perform(TimerEvent event) {
					if (shell.isDisposed()) {
						event.cancel();
						return;
					}
					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {
							if (!shell.isDisposed()) {
								boolean bDelayPaused = lblCloseIn.getData("DelayPaused") != null;
								if (bDelayPaused) {
									return;
								}

								long endOn = ((Long) lblCloseIn.getData("CloseOn")).longValue();
								if (SystemTime.getCurrentTime() > endOn) {
									result[0] = defaultOption;
									autoClosed = true;
									shell.dispose();
								} else {
									String sText = "";

									if (lblCloseIn.isDisposed())
										return;

									if (!bDelayPaused) {
										long delaySecs = (endOn - SystemTime.getCurrentTime()) / 1000;
										sText = MessageText.getString("popup.closing.in",
												new String[] {
													String.valueOf(delaySecs)
												});
									}

									lblCloseIn.setText(sText);
								}
							}
						};
					});
				}
			});

			SimpleTimer.addPeriodicEvent("OverPopup", 100, new TimerEventPerformer() {
				boolean wasOver = true;

				long lEnterOn = 0;

				public void perform(final TimerEvent event) {
					if (shell.isDisposed()) {
						event.cancel();
						return;
					}
					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {
							if (shell.isDisposed()) {
								event.cancel();
								return;
							}
							boolean isOver = shell.getBounds().contains(
									shell.getDisplay().getCursorLocation());
							if (isOver != wasOver) {
								wasOver = isOver;
								if (isOver) {
									lblCloseIn.setData("DelayPaused", "");
									lEnterOn = SystemTime.getCurrentTime();
									lblCloseIn.setText("");
								} else {
									lblCloseIn.setData("DelayPaused", null);
									if (lEnterOn > 0) {
										long diff = SystemTime.getCurrentTime() - lEnterOn;
										long endOn = ((Long) lblCloseIn.getData("CloseOn")).longValue()
												+ diff;
										lblCloseIn.setData("CloseOn", new Long(endOn));
									}
								}
							}
						}
					});
				}
			});
		}

		// Remember Me
		Button checkRemember = null;
		if (rememberID != null) {
			checkRemember = new Button(shell, SWT.CHECK);
			checkRemember.setText(rememberText);
			checkRemember.setSelection(rememberByDefault);

			checkRemember.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					Button checkRemember = (Button) e.widget;
					if (rememberID != null
							&& checkRemember != null
							&& checkRemember.getSelection()
							&& (rememberOnlyIfButton == -1 || rememberOnlyIfButton == result[0])) {
						RememberedDecisionsManager.setRemembered(rememberID, result[0]);
					}
				}
			});
		}

		// Buttons

		Composite cButtons = new Composite(shell, SWT.NONE);
		FormLayout layout = new FormLayout();

		cButtons.setLayout(layout);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
		cButtons.setLayoutData(gridData);

		Control lastButton = null;

		Listener buttonListener = new Listener() {

			public void handleEvent(Event event) {
				result[0] = ((Integer) event.widget.getData()).intValue();
				shell.dispose();
			}

		};

		int buttonWidth = 0;
		Button[] swtButtons = new Button[buttons.length];
		for (int i = 0; i < buttons.length; i++) {
			Button button = new Button(cButtons, SWT.PUSH);
			swtButtons[i] = button;
			button.setData(new Integer(i));
			button.setText(buttons[i]);
			button.addListener(SWT.Selection, buttonListener);

			formData = new FormData();
			if (lastButton != null) {
				formData.left = new FormAttachment(lastButton, 5);
			}

			button.setLayoutData(formData);

			Point size = button.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			if (size.x > buttonWidth) {
				buttonWidth = size.x;
			}

			if (i == defaultOption) {
				button.setFocus();
				shell.setDefaultButton(button);
			}

			lastButton = button;
		}

		if (buttonWidth > 0) {
			if (buttonWidth < MIN_BUTTON_SIZE) {
				buttonWidth = MIN_BUTTON_SIZE;
			}
			for (int i = 0; i < buttons.length; i++) {
				Point size = swtButtons[i].computeSize(buttonWidth, SWT.DEFAULT);
				swtButtons[i].setSize(size);
				formData = (FormData) swtButtons[i].getLayoutData();
				formData.width = buttonWidth;
			}
		}

		shell.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent event) {
				if (event.detail == SWT.TRAVERSE_ESCAPE) {
					shell.dispose();
				}
			}
		});

		Listener filterListener = new Listener() {
			public void handleEvent(Event event) {
				if (event.detail == SWT.TRAVERSE_ARROW_NEXT) {
					event.detail = SWT.TRAVERSE_TAB_NEXT;
					event.doit = true;
				} else if (event.detail == SWT.TRAVERSE_ARROW_PREVIOUS) {
					event.detail = SWT.TRAVERSE_TAB_PREVIOUS;
					event.doit = true;
				}
			}
		};
		display.addFilter(SWT.Traverse, filterListener);

		if (mouseAdapter != null) {
			addMouseTrackListener(shell, mouseAdapter);
		}

		shell.pack();
		Point size = shell.getSize();
		if (size.x < MIN_SIZE_X) {
			size.x = MIN_SIZE_X;
			shell.setSize(size);
		} else if (size.x > MAX_SIZE_X) {
			size = shell.computeSize(MAX_SIZE_X, SWT.DEFAULT);
			shell.setSize(size);
		}

		if (size.y < MIN_SIZE_Y) {
			size.y = MIN_SIZE_Y;
			shell.setSize(size);
		}

		Utils.centerWindowRelativeTo(shell, parent);

		for (int i = 0; i < listeners.length; i++) {
			listeners[i].skinAfterComponents(shell, this, relatedObjects);
		}

		shell.open();

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}

		if (display != null && !display.isDisposed()) {
			display.removeFilter(SWT.Traverse, filterListener);
		}

		return result[0];
	}

	private int _open2() {
		final int[] result = {
			-1
		};

		if (parent == null || parent.isDisposed()) {
			parent = Utils.findAnyShell();
			if (parent == null || parent.isDisposed()) {
				return result[0];
			}
		}

		MouseTrackAdapter mouseAdapter = null;
		Display display = parent.getDisplay();

		final StyledShell sShell = new StyledShell(parent, 6);
		final Shell shell = sShell.getShell();

		final Composite content = sShell.getContent();
		content.setBackgroundMode(SWT.INHERIT_DEFAULT);

		GridLayout gridLayout = new GridLayout();
		content.setLayout(gridLayout);

		Color foreground = content.getForeground();
		//TODO : Khai : fix this properly, quick hack to make things work for now
		if(foreground == null) {
			foreground = ColorCache.getColor(display,208,208,208);
			content.setForeground(foreground);
		}
		
		Label titleLabel = new Label(content, SWT.WRAP);
		titleLabel.setForeground(foreground);
		titleLabel.setText(title);
		titleLabel.setLayoutData(new GridData(GridData.FILL_BOTH));
		Utils.setFontHeight(titleLabel, 12, SWT.NORMAL);

		UISkinnableSWTListener[] listeners = UISkinnableManagerSWT.getInstance().getSkinnableListeners(
				MessageBoxShell.class.toString());
		for (int i = 0; i < listeners.length; i++) {
			listeners[i].skinBeforeComponents(shell, this, relatedObjects);
		}

		FormData formData;
		GridData gridData;

		Composite textComposite = content;
		if (imgLeft != null) {
			textComposite = new Composite(content, SWT.NONE);
			textComposite.setForeground(foreground);
			textComposite.setLayout(new GridLayout(2, false));
			textComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
			Label lblImage = new Label(textComposite, SWT.NONE);
			lblImage.setImage(imgLeft);
			lblImage.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		}

		Control linkControl;
		linkControl = createLinkLabel(textComposite, text);

		if ((html != null && html.length() > 0)
				|| (url != null && url.length() > 0)) {
			try {
				final Browser browser = new Browser(content,
						Utils.getInitialBrowserStyle(SWT.NONE));
				if (url != null && url.length() > 0) {
					browser.setUrl(url);
				} else {
					browser.setText(html);
				}
				GridData gd = new GridData(GridData.FILL_BOTH);
				gd.heightHint = 200;
				browser.setLayoutData(gd);
				browser.addProgressListener(new ProgressListener() {
					public void completed(ProgressEvent event) {
						browser.addLocationListener(new LocationListener() {
							public void changing(LocationEvent event) {
								event.doit = false;
							}

							public void changed(LocationEvent event) {
							}
						});
						browser.addOpenWindowListener(new OpenWindowListener() {
							public void open(WindowEvent event) {
								event.required = true;
							}
						});
					}

					public void changed(ProgressEvent event) {
					}
				});
			} catch (Exception e) {
				Debug.out(e);
				if (html != null) {
					Text text = new Text(content, SWT.BORDER | SWT.READ_ONLY | SWT.WRAP);
					text.setText(html);
					GridData gd = new GridData(GridData.FILL_BOTH);
					gd.heightHint = 200;
					text.setLayoutData(gd);
				}
			}

			gridData = new GridData(GridData.FILL_HORIZONTAL);
			linkControl.setLayoutData(gridData);
		} else {
			gridData = new GridData(GridData.FILL_BOTH);
			linkControl.setLayoutData(gridData);
		}

		Label lblPadding = new Label(content, SWT.NONE);
		lblPadding.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// Closing in..
		if (autoCloseInMS > 0) {
			final Label lblCloseIn = new Label(content, SWT.WRAP);
			lblCloseIn.setForeground(foreground);
			lblCloseIn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			long endOn = SystemTime.getCurrentTime() + autoCloseInMS;
			lblCloseIn.setData("CloseOn", new Long(endOn));
			SimpleTimer.addPeriodicEvent("autoclose", 500, new TimerEventPerformer() {
				public void perform(TimerEvent event) {
					if (content.isDisposed()) {
						event.cancel();
						return;
					}
					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {
							if (!content.isDisposed()) {
								boolean bDelayPaused = lblCloseIn.getData("DelayPaused") != null;
								if (bDelayPaused) {
									return;
								}

								long endOn = ((Long) lblCloseIn.getData("CloseOn")).longValue();
								if (SystemTime.getCurrentTime() > endOn) {
									result[0] = defaultOption;
									autoClosed = true;
									content.dispose();
								} else {
									String sText = "";

									if (lblCloseIn.isDisposed())
										return;

									if (!bDelayPaused) {
										long delaySecs = (endOn - SystemTime.getCurrentTime()) / 1000;
										sText = MessageText.getString("popup.closing.in",
												new String[] {
													String.valueOf(delaySecs)
												});
									}

									lblCloseIn.setText(sText);
								}
							}
						};
					});
				}
			});

			SimpleTimer.addPeriodicEvent("OverPopup", 100, new TimerEventPerformer() {
				boolean wasOver = true;

				long lEnterOn = 0;

				public void perform(final TimerEvent event) {
					if (content.isDisposed()) {
						event.cancel();
						return;
					}
					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {
							if (content.isDisposed()) {
								event.cancel();
								return;
							}
							boolean isOver = content.getBounds().contains(
									content.getDisplay().getCursorLocation());
							if (isOver != wasOver) {
								wasOver = isOver;
								if (isOver) {
									lblCloseIn.setData("DelayPaused", "");
									lEnterOn = SystemTime.getCurrentTime();
									lblCloseIn.setText("");
								} else {
									lblCloseIn.setData("DelayPaused", null);
									if (lEnterOn > 0) {
										long diff = SystemTime.getCurrentTime() - lEnterOn;
										long endOn = ((Long) lblCloseIn.getData("CloseOn")).longValue()
												+ diff;
										lblCloseIn.setData("CloseOn", new Long(endOn));
									}
								}
							}
						}
					});
				}
			});
		}

		// Remember Me
		Composite checkRememberPanel = null;
		if (rememberID != null) {
			checkRememberPanel = new Composite(content,SWT.NONE);
			FormLayout checklayout = new FormLayout();
			checkRememberPanel.setLayout(checklayout);
			final Button checkRemember = new Button(checkRememberPanel, SWT.CHECK);
			checkRemember.setSelection(rememberByDefault);
			
			Label checkRememberLabel = new Label(checkRememberPanel,SWT.NONE);
			
			checkRememberLabel.setForeground(foreground);
			checkRememberLabel.setText(rememberText);
			
			
			checkRememberLabel.addListener(SWT.MouseUp, new Listener() {
				public void handleEvent(Event arg0) {
					if(! checkRemember.isDisposed()) {
						checkRemember.setSelection(!checkRemember.getSelection());
					}
				}
			});
			FormData data = new FormData();
			data.left = new FormAttachment(checkRemember);
			data.bottom = new FormAttachment(checkRemember,-1,SWT.BOTTOM);
			checkRememberLabel.setLayoutData(data);
			
			checkRemember.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					Button checkRemember = (Button) e.widget;
					if (rememberID != null
							&& checkRemember != null
							&& checkRemember.getSelection()
							&& (rememberOnlyIfButton == -1 || rememberOnlyIfButton == result[0])) {
						RememberedDecisionsManager.setRemembered(rememberID, result[0]);
					}
				}
			});
		}

		// Buttons

		Composite cButtons = new Composite(content, SWT.NONE);
		FormLayout layout = new FormLayout();

		cButtons.setLayout(layout);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
		cButtons.setLayoutData(gridData);

		Control lastButton = null;

		Listener buttonListener = new Listener() {

			public void handleEvent(Event event) {
				result[0] = ((Integer) event.widget.getData()).intValue();
				sShell.close();
			}

		};

		int buttonWidth = 0;
		BubbleButton[] swtButtons = new BubbleButton[buttons.length];
		for (int i = 0; i < buttons.length; i++) {
			BubbleButton button = new BubbleButton(cButtons);
			swtButtons[i] = button;
			button.setData(new Integer(i));
			button.setText(buttons[i]);
			button.addListener(SWT.MouseUp, buttonListener);

			formData = new FormData();
			if (lastButton != null) {
				formData.left = new FormAttachment(lastButton, 20);
			}

			button.setLayoutData(formData);

			Point size = button.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			if (size.x > buttonWidth) {
				buttonWidth = size.x;
			}

			if (i == defaultOption) {
				button.setFocus();
				/*
				 * KN: TODO: Must implement default button behavior
				 */
				//				shell.setDefaultButton(button);
			}

			lastButton = button;
		}

		if (buttonWidth > 0) {
			if (buttonWidth < MIN_BUTTON_SIZE) {
				buttonWidth = MIN_BUTTON_SIZE;
			}
			for (int i = 0; i < buttons.length; i++) {
				Point size = swtButtons[i].computeSize(buttonWidth, SWT.DEFAULT);
				swtButtons[i].setSize(size);
				formData = (FormData) swtButtons[i].getLayoutData();
				formData.width = buttonWidth;
			}
		}

		shell.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent event) {
				if (event.detail == SWT.TRAVERSE_ESCAPE) {
					shell.dispose();
				}
			}
		});

		Listener filterListener = new Listener() {
			public void handleEvent(Event event) {
				if (event.detail == SWT.TRAVERSE_ARROW_NEXT) {
					event.detail = SWT.TRAVERSE_TAB_NEXT;
					event.doit = true;
				} else if (event.detail == SWT.TRAVERSE_ARROW_PREVIOUS) {
					event.detail = SWT.TRAVERSE_TAB_PREVIOUS;
					event.doit = true;
				}
			}
		};
		display.addFilter(SWT.Traverse, filterListener);

		if (mouseAdapter != null) {
			addMouseTrackListener(shell, mouseAdapter);
		}

		shell.pack();
		Point size = shell.getSize();
		if (size.x < MIN_SIZE_X) {
			size.x = MIN_SIZE_X;
			shell.setSize(size);
		} else if (size.x > MAX_SIZE_X) {
			size = shell.computeSize(MAX_SIZE_X, SWT.DEFAULT);
			shell.setSize(size);
		}

		if (size.y < MIN_SIZE_Y) {
			size.y = MIN_SIZE_Y;
			shell.setSize(size);
		}

		Utils.centerWindowRelativeTo(shell, parent);

		for (int i = 0; i < listeners.length; i++) {
			listeners[i].skinAfterComponents(content, this, relatedObjects);
		}

		sShell.open();

		while (!content.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}

		if (display != null && !display.isDisposed()) {
			display.removeFilter(SWT.Traverse, filterListener);
		}

		return result[0];
	}

	/**
	 * Adds mousetracklistener to composite and all it's children
	 * 
	 * @param parent Composite to start at
	 * @param listener Listener to add
	 */
	private void addMouseTrackListener(Composite parent,
			MouseTrackListener listener) {
		if (parent == null || listener == null || parent.isDisposed())
			return;

		parent.addMouseTrackListener(listener);
		Control[] children = parent.getChildren();
		for (int i = 0; i < children.length; i++) {
			Control control = children[i];
			if (control instanceof Composite)
				addMouseTrackListener((Composite) control, listener);
			else
				control.addMouseTrackListener(listener);
		}
	}

	private Canvas createLinkLabel(final Composite shell, final String text) {

		final Canvas canvas = new Canvas(shell, SWT.None) {
			public Point computeSize(int wHint, int hHint, boolean changed) {
				Rectangle area = new Rectangle(0, 0, wHint < 0 ? MAX_SIZE_X : wHint,
						5000);
				GC gc = new GC(this);
				GCStringPrinter sp = new GCStringPrinter(gc, text, area, true, false,
						SWT.WRAP | SWT.TOP);
				sp.calculateMetrics();
				gc.dispose();
				Point size = sp.getCalculatedSize();
				return size;
			}
		};

		Listener l = new Listener() {
			GCStringPrinter sp;

			public void handleEvent(Event e) {
				if (!handleHTML) {
					if (e.type == SWT.Paint) {
						Rectangle area = canvas.getClientArea();
						e.gc.setForeground(shell.getForeground());
						GCStringPrinter.printString(e.gc, text, area, true, false, SWT.WRAP
								| SWT.TOP);
					}
					return;
				}

				if (e.type == SWT.Paint) {
					Rectangle area = canvas.getClientArea();
					sp = new GCStringPrinter(e.gc, text, area, true, false, SWT.WRAP
							| SWT.TOP);
					sp.setUrlColor(ColorCache.getColor(e.gc.getDevice(), "#0000ff"));
					if (urlColor != null) {
						sp.setUrlColor(urlColor);
					}
					e.gc.setForeground(shell.getForeground());
					sp.printString();
				} else if (e.type == SWT.MouseMove) {
					if (sp != null) {
						URLInfo hitUrl = sp.getHitUrl(e.x, e.y);
						if (hitUrl != null) {
							canvas.setCursor(canvas.getDisplay().getSystemCursor(
									SWT.CURSOR_HAND));
							canvas.setToolTipText(hitUrl.url);
						} else {
							canvas.setCursor(canvas.getDisplay().getSystemCursor(
									SWT.CURSOR_ARROW));
							canvas.setToolTipText(null);
						}
					}
				} else if (e.type == SWT.MouseUp) {
					if (sp != null) {
						URLInfo hitUrl = sp.getHitUrl(e.x, e.y);
						if (hitUrl != null) {
							Utils.launch(hitUrl.url);
						}
					}
				}
			}
		};
		canvas.addListener(SWT.Paint, l);
		if (!handleHTML) {
			canvas.addListener(SWT.MouseMove, l);
			canvas.addListener(SWT.MouseUp, l);
		}

		ClipboardCopy.addCopyToClipMenu(canvas,
				new ClipboardCopy.copyToClipProvider() {
					public String getText() {
						return (text);
					}
				});

		return canvas;
	}

	public String getHtml() {
		return html;
	}

	public void setHtml(String html) {
		this.html = html;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * @return the rememberID
	 */
	public String getRememberID() {
		return rememberID;
	}

	/**
	 * @param rememberID the rememberID to set
	 */
	public void setRememberID(String rememberID, boolean rememberByDefault) {
		this.rememberID = rememberID;
		this.rememberByDefault = rememberByDefault;
	}

	/**
	 * @return the rememberText
	 */
	public String getRememberText() {
		return rememberText;
	}

	/**
	 * @param rememberText the rememberText to set
	 */
	public void setRememberText(String rememberText) {
		this.rememberText = rememberText;
	}

	/**
	 * @return the autoCloseInMS
	 */
	public int getAutoCloseInMS() {
		return autoCloseInMS;
	}

	/**
	 * @param autoCloseInMS the autoCloseInMS to set
	 */
	public void setAutoCloseInMS(int autoCloseInMS) {
		this.autoCloseInMS = autoCloseInMS;
	}

	/**
	 * @return the autoClosed
	 */
	public boolean isAutoClosed() {
		return autoClosed;
	}

	// @see com.aelitis.azureus.ui.UIFunctionsUserPrompter#setRelatedObject(java.lang.Object)
	public void setRelatedObject(Object relatedObject) {
		this.relatedObjects = new Object[] {
			relatedObject
		};
	}

	// @see com.aelitis.azureus.ui.UIFunctionsUserPrompter#setRelatedObjects(java.lang.Object[])
	public void setRelatedObjects(Object[] relatedObjects) {
		this.relatedObjects = relatedObjects;
	}

	public Object[] getRelatedObjects() {
		return relatedObjects;
	}

	public void setLeftImage(Image imgLeft) {
		this.imgLeft = imgLeft;
	}

	/**
	 * Replaces Image on left with icon
	 * 
	 * @param icon SWT.ICON_ERROR, ICON_INFORMATION, ICON_QUESTION, ICON_WARNING, ICON_WORKING
	 *
	 * @since 3.0.1.7
	 */
	public void setLeftImage(final int icon) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				setLeftImage(Display.getDefault().getSystemImage(icon));
			}
		});
	}

	public void setIconResource(String resource) {
		if (resource.equals("info")) {

			setLeftImage(SWT.ICON_INFORMATION);

		} else if (resource.equals("warning")) {

			setLeftImage(SWT.ICON_WARNING);

		} else if (resource.equals("error")) {

			setLeftImage(SWT.ICON_ERROR);

		} else {

			setLeftImage(ImageRepository.getImage(resource));
		}
	}

	public static void main(String[] args) {
		Display display = Display.getDefault();
		Shell shell = new Shell(display, SWT.SHELL_TRIM);
		shell.open();

		MessageBoxShell messageBoxShell = new MessageBoxShell(
				shell,
				"Title",
				"Test\n"
						+ "THis is a very long line that tests whether the box gets really wide which is something we don't want.\n"
						+ "A <A HREF=\"Link\">link</A> for <A HREF=\"http://moo.com\">you</a>",
				new String[] {
					"Okay",
					"Cancyyyyyy",
					"Maybe"
				}, 1, "test2",
				MessageText.getString("MessageBoxWindow.nomoreprompting"), false, 15000);

		messageBoxShell.setHtml("<b>Moo</b> goes the cow<p><hr>");
		System.out.println(messageBoxShell.open());
	}

	public int getRememberOnlyIfButton() {
		return rememberOnlyIfButton;
	}

	public void setRememberOnlyIfButton(int rememberOnlyIfButton) {
		this.rememberOnlyIfButton = rememberOnlyIfButton;
	}

	public Color getUrlColor() {
		return urlColor;
	}

	public void setUrlColor(Color colorURL) {
		this.urlColor = colorURL;
	}

	/**
	 * @param b
	 *
	 * @since 3.0.5.3
	 */
	public void setHandleHTML(boolean handleHTML) {
		this.handleHTML = handleHTML;
	}
}
