package com.vuze.tests.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.*;

public class testTreeBrowser
{
	public static void main(String[] args) {
		Display display = new Display();
		System.out.println(SWT.getVersion());
		Shell shell = new Shell(display, SWT.SHELL_TRIM);
		shell.setLayout(new FillLayout(SWT.HORIZONTAL));

		Tree tree = new Tree(shell, SWT.BORDER);
		tree.addListener(SWT.PaintItem, new Listener() {

			public void handleEvent(Event event) {
			}
		});
		TreeItem item1 = new TreeItem(tree, SWT.NONE);
		item1.setText("Google");
		item1.setData(
				"url",
				"http://google.com/about");
//				"http://client.vuze.com:80/xsearch/index.php?subscription=5UNUJ6QJE522MYOZ&azid=P3WFILH5KAAKGEUXXTKV33SCJYRMZPPW&azv=4.3.0.5_CVS&locale=&os.name=Mac OS X");
		TreeItem item2 = new TreeItem(tree, SWT.NONE);
		item2.setText("Yahoo");
		item2.setData(
				"url",
				"http://yahoo.com/about.html");
//				"http://client.vuze.com:80/xsearch/index.php?subscription=SWZUYGQ7ICJR2CLS&azid=P3WFILH5KAAKGEUXXTKV33SCJYRMZPPW&azv=4.3.0.5_CVS&locale=&os.name=Mac OS X");

		final Composite c = new Composite(shell, SWT.NONE);
		c.setLayout(new FillLayout());

		tree.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				String url = (String) e.item.getData("url");
				Control[] children = c.getChildren();
				for (Control control : children) {
					control.dispose();
				}
				final Browser browser = new Browser(c, SWT.NONE);
				final BrowserFunction browserFunction = new BrowserFunction(browser,
						"sendMessageToAZ") {
					public Object function(Object[] args) {
						return null;
					}
				};
				browser.addProgressListener(new ProgressListener() {

					public void completed(ProgressEvent event) {
					}

					public void changed(ProgressEvent event) {
					}
				});

				browser.addLocationListener(new LocationListener() {

					public void changing(LocationEvent event) {
						System.out.println("changing " + event.location);
					}

					public void changed(LocationEvent event) {
						System.out.println("changed " + event.location);
					}
				});

				c.layout(true);
				browser.setUrl(url);

				browser.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent e) {
						browserFunction.dispose();
					}
				});
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		shell.open();
		while (!shell.isDisposed())
			if (!display.readAndDispatch())
				display.sleep();
	}
}
