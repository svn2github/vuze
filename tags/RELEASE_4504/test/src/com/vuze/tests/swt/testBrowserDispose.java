package com.vuze.tests.swt;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.*;

public class testBrowserDispose
{
	
	static Browser b2;
	
	public static void main(String[] args) {
		Display display = new Display();
		final Shell shell = new Shell(display, SWT.SHELL_TRIM);
		shell.setLayout(new FillLayout());

		final Browser b1 = new Browser(shell, SWT.NONE);
		b1.setUrl("http://vuze.com/welcome.start");

		Button btn = new Button(shell, SWT.PUSH);
		btn.setText("add/remove");

		b2 = new Browser(shell, SWT.NONE);
		b2.setUrl("http://vuze.com/browse.start");

		btn.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				if (b2.isDisposed()) {
					b2 = new Browser(shell, SWT.NONE);
					b2.setUrl("http://vuze.com/browse.start");
					shell.layout();
				} else {
  				//b2.setUrl("about:blank");
  				//b2.setVisible(false);
  				b2.dispose();
				}
			}
		});

		shell.open();

		while (!shell.isDisposed ()) {
			if (!display.readAndDispatch ())
				display.sleep ();
		}
		display.dispose ();
	}
}
