package org.gudy.azureus2.ui.swt.shells;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;

/**
 * A messagebox that allows you config the button
 * 
 */
public class MessageBoxShell
{
	private final static String REGEX_URLHTML = "<A HREF=\"(.+?)\">(.+?)</A>";

	private final static int MIN_SIZE_X = 300;

	private final static int MIN_SIZE_Y = 200;

	private final static int MAX_SIZE_X = 500;

	private static int numOpen = 0;

	public static int open(final Shell parent, final String title,
			final String text, final String[] buttons, final int defaultOption) {
		return open(parent, title, text, buttons, defaultOption, null, false, -1);
	}

	public static int getRememberedDecision(String id) {
		if (id == null) {
			return -1;
		}
		Map remembered_decisions = COConfigurationManager.getMapParameter(
				"MessageBoxWindow.decisions", new HashMap());

		Long l = (Long) remembered_decisions.get(id);
		System.out.println("getR " + id + " -> " + l);
		if (l != null) {

			return l.intValue();
		}

		return -1;
	}

	protected static void setRemembered(String id, int value) {
		if (id == null) {
			return;
		}

		Map remembered_decisions = COConfigurationManager.getMapParameter(
				"MessageBoxWindow.decisions", new HashMap());

		if (value == -1) {
			remembered_decisions.remove(id);
		} else {
			remembered_decisions.put(id, new Long(value));
		}

		System.out.println("setR " + id + " -> " + value);
		COConfigurationManager.setParameter("MessageBoxWindow.decisions",
				remembered_decisions);
		COConfigurationManager.save();
	}

	public static int open(final Shell parent, final String title,
			final String text, final String[] buttons, final int defaultOption,
			final String rememberID, final boolean bRememberByDefault,
			final int autoCloseInMS) {

		if (rememberID != null) {
			int rememberedDecision = getRememberedDecision(rememberID);
			if (rememberedDecision >= 0) {
				return rememberedDecision;
			}
		}

		numOpen++;

		final int[] result = new int[1];
		result[0] = -1;

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				MessageBoxShell messageBoxShell = new MessageBoxShell();
				result[0] = messageBoxShell._open(parent, title, text, buttons,
						defaultOption, rememberID, bRememberByDefault, autoCloseInMS);
			}
		}, false);

		numOpen--;
		return result[0];
	}

	public static boolean isOpen() {
		return numOpen > 0;
	}

	private int _open(Shell parent, String title, String text, String[] buttons,
			final int defaultOption, final String rememberID,
			boolean bRememberByDefault, int autoCloseInMS) {
		MouseTrackAdapter mouseAdapter = null;
		Display display = parent.getDisplay();
		final int[] result = { -1
		};

		final Shell shell = new Shell(parent, SWT.DIALOG_TRIM
				| SWT.APPLICATION_MODAL);
		shell.setText(title);
		GridLayout gridLayout = new GridLayout();
		shell.setLayout(gridLayout);

		FormData formData;
		GridData gridData;

		Control linkControl;
		try {
			Link linkLabel = new Link(shell, SWT.WRAP);

			linkControl = linkLabel;

			linkLabel.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
			linkLabel.setText(text);
			linkLabel.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					if (e.text.endsWith(".torrent"))
						TorrentOpener.openTorrent(e.text);
					else
						Utils.launch(e.text);
				}
			});

			Matcher matcher = Pattern.compile(REGEX_URLHTML, Pattern.CASE_INSENSITIVE).matcher(
					text);
			String tooltip = null;
			while (matcher.find()) {
				if (tooltip == null)
					tooltip = "";
				else
					tooltip += "\n";
				tooltip += matcher.group(2) + ": " + matcher.group(1);
			}
			linkLabel.setToolTipText(tooltip);
		} catch (Throwable t) {
			// 3.0
			Label linkLabel = new Label(shell, SWT.WRAP);
			linkControl = linkLabel;

			text = Pattern.compile(REGEX_URLHTML, Pattern.CASE_INSENSITIVE).matcher(
					text).replaceAll("$2 ($1)");

			linkLabel.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
			linkLabel.setText(text);
		}

		gridData = new GridData(GridData.FILL_BOTH);
		linkControl.setLayoutData(gridData);

		// Closing in..
		if (autoCloseInMS > 0) {
			final Label lblCloseIn = new Label(shell, SWT.WRAP);
			lblCloseIn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			long endOn = SystemTime.getCurrentTime() + autoCloseInMS;
			lblCloseIn.setData("CloseOn", new Long(endOn));
			SimpleTimer.addPeriodicEvent("autoclose", 500, new TimerEventPerformer() {
				public void perform(TimerEvent event) {
					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {
							if (!shell.isDisposed()) {
								long endOn = ((Long) lblCloseIn.getData("CloseOn")).longValue();
								if (SystemTime.getCurrentTime() > endOn) {
									result[0] = defaultOption;
									shell.dispose();
								} else {
									String sText = "";

									if (lblCloseIn == null || lblCloseIn.isDisposed())
										return;

									boolean bDelayPaused = lblCloseIn.getData("DelayPaused") != null;
									if (!bDelayPaused) {
										long delaySecs = (endOn - SystemTime.getCurrentTime()) / 1000;
										sText = MessageText.getString("popup.closing.in",
												new String[] { String.valueOf(delaySecs)
												});
									}

									lblCloseIn.setText(sText);
								}
							}
						};
					});
				}
			});

			mouseAdapter = new MouseTrackAdapter() {
				long lEnterOn = 0;

				public void mouseEnter(MouseEvent e) {
					lblCloseIn.setData("DelayPaused", "");
					lEnterOn = SystemTime.getCurrentTime();
				}

				public void mouseExit(MouseEvent e) {
					lblCloseIn.setData("DelayPaused", null);
					if (lEnterOn > 0) {
						long diff = SystemTime.getCurrentTime() - lEnterOn;
						long endOn = ((Long) lblCloseIn.getData("CloseOn")).longValue()
								+ diff;
						lblCloseIn.setData("CloseOn", new Long(endOn));
					}
				}
			};
		}

		// Remember Me
		Button checkRemember = null;
		if (rememberID != null) {
			checkRemember = new Button(shell, SWT.CHECK);
			Messages.setLanguageText(checkRemember,
					"MessageBoxWindow.rememberdecision");
			checkRemember.setSelection(bRememberByDefault);

			checkRemember.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					Button checkRemember = (Button) e.widget;
					if (rememberID != null && checkRemember != null
							&& checkRemember.getSelection()) {
						setRemembered(rememberID, result[0]);
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
				shell.setDefaultButton(button);
			}

			lastButton = button;
		}

		if (buttonWidth > 0) {
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
		shell.open();

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
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

	public static void main(String[] args) {
		Display display = Display.getDefault();
		Shell shell = new Shell(display, SWT.SHELL_TRIM);
		shell.open();

		System.out.println(open(
				shell,
				"Title",
				"Test\n"
						+ "THis is a very long line that tests whether the box gets really wide which is something we don't want.\n"
						+ "A <A HREF=\"Link\">link</A> for <A HREF=\"http://moo.com\">you</a>",
				new String[] {
					"Okay",
					"Cancyyyyyy",
					"Maybe"
				}, 1, "test2", false, 15000));
	}
}
