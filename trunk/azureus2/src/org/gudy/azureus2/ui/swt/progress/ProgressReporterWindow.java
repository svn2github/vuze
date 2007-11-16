package org.gudy.azureus2.ui.swt.progress;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.ui.swt.ITwistieListener;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;

public class ProgressReporterWindow
	implements IProgressReportConstants, ITwistieListener
{
	private Shell shell;

	private ScrolledComposite scrollable;

	private Composite scrollChild;

	private IProgressReporter[] pReporters;

	/**
	 * A registry to keep track of all reporters that are being displayed in all instances
	 * of this window.
	 * @see #isOpened(IProgressReporter)
	 */
	private static final ArrayList reportersRegistry = new ArrayList();

	/**
	 * The default width for the shell upon first opening
	 */
	private int defaultShellWidth = 400;

	private int style;

	/**
	 * Construct a <code>ProgressReporterWindow</code> for a single <code>ProgressReporter</code> 
	 * @param pReporter
	 */
	private ProgressReporterWindow(IProgressReporter pReporter, int style) {
		this.style = style;
		if (null != pReporter) {
			pReporters = new IProgressReporter[] {
				pReporter
			};

		} else {
			pReporters = new IProgressReporter[0];
		}

		createControls();
	}

	/**
	 * Construct a single <code>ProgressReporterWindow</code> showing all <code>ProgressReporter</code>'s in the given array
	 * @param pReporters
	 */
	private ProgressReporterWindow(IProgressReporter[] pReporters, int style) {
		this.style = style;
		if (null != pReporters) {
			this.pReporters = pReporters;

		} else {
			pReporters = new IProgressReporter[0];
		}

		createControls();
	}

	/**
	 * Opens the window and display the given <code>IProgressReporter</code>
	 * 
	 * @param pReporter
	 * @param closeOnFinished <code>true</code> to automatically close this window when the reporter is finished; otherwise leave it opened
	 */
	public static void open(IProgressReporter pReporter, int style) {
		new ProgressReporterWindow(pReporter, style).openWindow();
	}

	/**
	 * Opens the window and display the given array of <code>IProgressReporter</code>'s
	 * @param pReporters
	 */
	public static void open(IProgressReporter[] pReporters, int style) {
		new ProgressReporterWindow(pReporters, style).openWindow();
	}

	/**
	 * Returns whether the given <code>IProgressReporter</code> is opened in any instance of this window;
	 * processes can query this method before opening another window to prevent opening multiple
	 * windows for the same reporter.  This is implemented explicitly instead of having the window automatically
	 * recycle instances because there are times when it is desirable to open a reporter in more than one
	 * instances of this window.
	 * @param pReporter
	 * @return
	 */
	public static boolean isOpened(IProgressReporter pReporter) {
		return reportersRegistry.contains(pReporter);
	}

	private void createControls() {
		/*
		 * Sets up the shell
		 */

		int shellStyle = SWT.DIALOG_TRIM | SWT.RESIZE;
		if ((style & MODAL) != 0) {
			shellStyle |= SWT.APPLICATION_MODAL;
		}

		shell = ShellFactory.createMainShell(shellStyle);
		shell.setText(MessageText.getString("progress.window.title"));
		shell.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_WHITE)); //KN: must remove??

		if (!Constants.isOSX) {
			shell.setImage(ImageRepository.getImage("azureus"));
		}

		GridLayout gLayout = new GridLayout();
		gLayout.marginHeight = 0;
		gLayout.marginWidth = 0;
		shell.setLayout(gLayout);

		/*
		 * Using ScrolledComposite with only vertical scroll
		 */
		scrollable = new ScrolledComposite(shell, SWT.V_SCROLL);
		scrollable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		/*
		 * Main content composite where panels will be created
		 */
		scrollChild = new Composite(scrollable, SWT.NONE);

		scrollChild.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_WHITE));//KN: temp until finalized by UX
		
		GridLayout gLayoutChild = new GridLayout();
		gLayoutChild.marginHeight = 0;
		gLayoutChild.marginWidth = 0;
		gLayoutChild.verticalSpacing = 3;
		scrollChild.setLayout(gLayoutChild);
		scrollable.setContent(scrollChild);
		scrollable.setExpandVertical(true);
		scrollable.setExpandHorizontal(true);

		/*
		 * Re-adjust scrollbar setting when the window resizes
		 */
		scrollable.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				Rectangle r = scrollable.getClientArea();
				scrollable.setMinSize(scrollChild.computeSize(r.width, SWT.DEFAULT));
			}
		});

		/*
		 * On closing remove all reporters that was handled by this instance of the window from the registry 
		 */
		shell.addListener(SWT.Close, new Listener() {
			public void handleEvent(Event event) {
				for (int i = 0; i < pReporters.length; i++) {
					reportersRegistry.remove(pReporters[i]);
				}
			}
		});

		if (pReporters.length == 0) {
			createEmptyPanel();
		} else {
			createPanels();
		}
	}

	/**
	 * Creates just an empty panel with a message indicating there are no reports to display 
	 */
	private void createEmptyPanel() {
		scrollChild.setLayout(new GridLayout());

		Label nothingToDisplay = new Label(scrollChild, SWT.WRAP);
		GridData gData = new GridData(SWT.BEGINNING, SWT.TOP, true, true);
		gData.heightHint = 100;
		nothingToDisplay.setLayoutData(gData);
		nothingToDisplay.setText(MessageText.getString("Progress.reporting.no.reports.to.display"));

	}

	public void openWindow() {
		resizeToFit();
		shell.open();
	}

	private void resizeToFit() {
		Point p = shell.computeSize(defaultShellWidth, SWT.DEFAULT);
		if (false == shell.getSize().equals(p)) {
			shell.setSize(p);
		}
	}

	private void createPanels() {

		int size = pReporters.length;

		/*
		 * Add the style bit for standalone if there is zero or 1 reporters
		 */
		if (size < 2) {
			style |= STANDALONE;
		}

		for (int i = 0; i < size; i++) {
			if (null != pReporters[i]) {

				/*
				 * Add this reporter to the registry
				 */
				reportersRegistry.add(pReporters[i]);

				final ProgressReporterPanel panel = new ProgressReporterPanel(
						scrollChild, pReporters[i], style);
				/*
				 * For alternating panel background color use this code instead
				 * panel.setBackground(i % 2 == 0);
				 */
				panel.setBackground(true);

				panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				panel.addTwistieListener(this);

				/*
				 * When a panel is disposed resize and re-layout the window
				 */
				panel.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent e) {

						removeReporter(((ProgressReporterPanel) e.widget).pReporter);

						/*
						 * If it's the last reporter then close the shell itself since it will be just empty
						 */
						if (pReporters.length == 0) {
							shell.dispose();
						} else {

							panel.removeTwistieListener(ProgressReporterWindow.this);
							((GridData) panel.getLayoutData()).exclude = true;
							panel.setVisible(false);
							resizePanels(panel);
							shell.layout(true, true);
							resizeToFit();
						}
					}

				});
			}
		}

		resizePanels(null);
	}

	private void resizePanels(ProgressReporterPanel panelToIgnore) {
		Control[] controls = scrollChild.getChildren();

		for (int i = controls.length - 1; i >= 0; i--) {
			if (true != controls[i].equals(panelToIgnore)) {
				((GridData) controls[i].getLayoutData()).grabExcessVerticalSpace = true;
				break;
			}
		}
	}

	/**
	 * Remove the given <code>IProgressReporter</code> from the <code>pReporters</code> array; resize the array if required
	 * @param reporter
	 */
	private void removeReporter(IProgressReporter reporter) {
		/*
		 * The array is typically small so this is good enough for now
		 */

		int IDX = Arrays.binarySearch(pReporters, reporter);
		if (IDX >= 0) {
			IProgressReporter[] rps = new IProgressReporter[pReporters.length - 1];
			for (int i = 0; i < rps.length; i++) {
				rps[i] = pReporters[(i >= IDX ? i + 1 : i)];
			}
			pReporters = rps;
		}
	}

	/**
	 * When any <code>ProgressReporterPanel</code> in this window is expanded or collapsed;
	 * re-layout the controls and window appropriately 
	 */
	public void isCollapsed(boolean value) {
		scrollable.setRedraw(false);
		Rectangle r = scrollable.getClientArea();
		scrollable.setMinSize(scrollChild.computeSize(r.width, SWT.DEFAULT));
		scrollable.setRedraw(true);
		resizeToFit();
		shell.update();

	}

}
