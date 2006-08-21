package org.gudy.azureus2.ui.swt.shells;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;

public class MessageBoxShell {
	private final static String REGEX_URLHTML = "<A HREF=\"(.+?)\">(.+?)</A>";

	private final static int MIN_SIZE_X = 300;

	private final static int MIN_SIZE_Y = 200;

	private final static int MAX_SIZE_X = 500;

	public static int open(final Shell parent, final String title,
			final String text, final String[] buttons, final int defaultOption) {
		final int[] result = new int[1];
		result[0] = -1;

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				MessageBoxShell messageBoxShell = new MessageBoxShell();
				result[0] = messageBoxShell._open(parent, title, text, buttons,
						defaultOption);
			}
		}, false);

		return result[0];
	}

	private int _open(Shell parent, String title, String text, String[] buttons,
			int defaultOption) {
		Display display = parent.getDisplay();
		final int[] result = { -1 };

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
						Program.launch(e.text);
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
				formData = (FormData)swtButtons[i].getLayoutData();
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
				new String[] { "Okay", "Cancyyyyyy", "Maybe" }, 1));
	}
}
