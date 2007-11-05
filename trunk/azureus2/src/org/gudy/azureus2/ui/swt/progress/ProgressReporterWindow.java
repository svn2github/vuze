package org.gudy.azureus2.ui.swt.progress;

import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.ui.swt.AZProgressBar;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;
import org.gudy.azureus2.ui.swt.mainwindow.Cursors;
import org.gudy.azureus2.ui.swt.progress.ProgressReporter.ProgressReport;

public class ProgressReporterWindow
	implements IProgressReportConstants
{
	private Shell shell;

	private ScrolledComposite scrollable;

	private Composite scrollChild;

	private IProgressReporter[] pReporters;

	private int inset = 4;

	private Color normalColor = null;

	private Color errorColor = null;

	private Color activeLinkColor = null;

	private Color disabledLinkColor = null;

	private Color alternatingBackground = null;

	private String actionLabelText_cancel = null;

	private String actionLabelText_remove = null;

	private String actionLabelText_retry = null;

	private String actionLabelText_detail = null;

	private final String NO_HISTORY_TO_DISPLAY = MessageText.getString("Progress.reporting.no.history.to.display");

	/**
	 * Maximum width that any actionLabel_multi_function can be; this is calculated in {@link #createControls()}
	 */
	private int actionLabelMaxWidth = -1;

	/**
	 * Minimum height that any text controls must be; this is so the layout manager will still allocate
	 * space for an empty text control and is calculated in {@link #createControls()}
	 */
	private int labelMinHeight = -1;

	/**
	 * Default image to use if a reporter does not provide its own
	 */
	private Image defaultImage = null;

	/**
	 * The default width for the shell upon first opening
	 */
	private int defaultShellWidth = 400;

	/**
	 * If <code>true</code> then this window is hosting only 1 <code>ProgressReporter</code> 
	 */
	private boolean isStandalone = false;

	public static final int NONE = 0;

	/**
	 * Automatically closes the window when the reporter is finished;
	 * this only takes effect when there is only 1 reporter in the window
	 */
	public static final int AUTO_CLOSE = 1 << 1;

	/**
	 * Open the window as MODAL
	 */
	public static final int MODAL = 1 << 2;

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

		isStandalone = true;
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

		isStandalone = this.pReporters.length == 1;
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

		if (!Constants.isOSX) {
			shell.setImage(ImageRepository.getImage("azureus"));
		}

		GridLayout gLayout = new GridLayout();
		gLayout.marginHeight = 0;
		gLayout.marginWidth = 0;
		shell.setLayout(gLayout);

		/*
		 * Gets some resources
		 */
		actionLabelText_cancel = MessageText.getString("Progress.reporting.prompt.label.cancel");
		actionLabelText_remove = MessageText.getString("Progress.reporting.prompt.label.remove");
		actionLabelText_retry = MessageText.getString("Progress.reporting.prompt.label.retry");
		actionLabelText_detail = MessageText.getString("Progress.reporting.prompt.label.detail");

		/*
		 * Determine the max width of all the possible action label values;
		 * actionLabelMaxWidth is used in resizeContent()
		 * 
		 * Additionally grab the height to be used as the labelMinHeight
		 */
		GC gc = new GC(shell);
		Point p = gc.textExtent(actionLabelText_cancel);
		actionLabelMaxWidth = gc.textExtent(actionLabelText_cancel).x;
		labelMinHeight = p.y;
		if (actionLabelMaxWidth < gc.textExtent(actionLabelText_remove).x) {
			actionLabelMaxWidth = gc.textExtent(actionLabelText_remove).x;
		}
		if (actionLabelMaxWidth < gc.textExtent(actionLabelText_retry).x) {
			actionLabelMaxWidth = gc.textExtent(actionLabelText_retry).x;
		}
		if (actionLabelMaxWidth < gc.textExtent(actionLabelText_detail).x) {
			actionLabelMaxWidth = gc.textExtent(actionLabelText_detail).x;
		}

		actionLabelMaxWidth += inset * 2;
		gc.dispose();

		/*
		 * Sets up default colors
		 */
		normalColor = shell.getDisplay().getSystemColor(SWT.COLOR_BLUE);
		errorColor = shell.getDisplay().getSystemColor(SWT.COLOR_RED);
		alternatingBackground = shell.getDisplay().getSystemColor(SWT.COLOR_WHITE);
		activeLinkColor = shell.getDisplay().getSystemColor(SWT.COLOR_BLUE);
		disabledLinkColor = shell.getDisplay().getSystemColor(SWT.COLOR_GRAY);

		/*
		 * Default image
		 */
		defaultImage = shell.getDisplay().getSystemImage(SWT.ICON_INFORMATION);
		/*
		 * Using ScrolledComposite with only vertical scroll
		 */
		scrollable = new ScrolledComposite(shell, SWT.V_SCROLL);
		scrollable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		/*
		 * Main content composite where panels will be created
		 */
		scrollChild = new Composite(scrollable, SWT.NONE);
		shell.setBackground(shell.getDisplay().getSystemColor(
				SWT.COLOR_WIDGET_BACKGROUND));

		scrollChild.setBackground(shell.getBackground());
		GridLayout gLayoutChild = new GridLayout();
		gLayoutChild.marginHeight = 0;
		gLayoutChild.marginWidth = 0;
		gLayoutChild.verticalSpacing = 0;
		gLayoutChild.horizontalSpacing = 0;
		scrollChild.setLayout(gLayoutChild);
		scrollable.setContent(scrollChild);
		scrollable.setExpandVertical(true);
		scrollable.setExpandHorizontal(true);

		scrollable.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				Rectangle r = scrollable.getClientArea();
				scrollable.setMinSize(scrollChild.computeSize(r.width, SWT.DEFAULT));
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
		Label nothingToDisplay = new Label(scrollChild, SWT.WRAP);
		GridData gData = new GridData(SWT.BEGINNING, SWT.TOP, true, true);
		gData.heightHint = 100;
		nothingToDisplay.setLayoutData(gData);
		nothingToDisplay.setText(MessageText.getString("Progress.reporting.no.reports.to.display"));
		nothingToDisplay.setBackground(alternatingBackground);

		GridLayout gLayoutChild = new GridLayout();
		gLayoutChild.marginHeight = inset;
		gLayoutChild.marginWidth = inset;
		scrollChild.setLayout(gLayoutChild);
		scrollChild.setBackground(alternatingBackground);

	}

	public void openWindow() {
		fixupSize();
		shell.open();
	}

	private void fixupSize() {
		shell.layout(true, true);
		Point p = shell.computeSize(defaultShellWidth, SWT.DEFAULT);
		//		p.y += 15;
		shell.setSize(p);
		Utils.centreWindow(shell);
	}

	private void createPanels() {

		int size = pReporters.length;
		for (int i = 0; i < size; i++) {
			if (null != pReporters[i]) {
				ReporterPanel panel = null;

				/*
				 * Show separator only when there is more than 1
				 */
				if (size > 1 && i < (size - 1)) {
					panel = new ReporterPanel(scrollChild, pReporters[i], true);
				} else {
					panel = new ReporterPanel(scrollChild, pReporters[i], false);
				}
				/*
				 * For alternating panel background color use this code instead
				 * panel.setBackground(i % 2 == 0);
				 */
				panel.setBackground(true);

				/*
				 * When a panel is disposed resize and re-layout the window
				 */
				panel.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent e) {

						removeReporter(((ReporterPanel) e.widget).pReporter);

						/*
						 * If there's only 1 left then set it into standalone mode; so we know to close the window
						 * if the style flag AUTO_CLOSE is set
						 */
						isStandalone = pReporters.length == 1;

						/*
						 * If it's the last reporter then might as well close the shell itself
						 */
						if (pReporters.length == 0) {
							shell.dispose();
						} else {
							System.out.println("resize shell please!!");//KN: sysout 
						}
					}

				});
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
	 * 
	 * @author knguyen
	 *
	 */
	private class ReporterPanel
		extends Composite
	{
		public IProgressReporter pReporter = null;

		private IProgressReporterListener reporterListener = null;

		private int iconMargin = 0;

		private Label imageLabel = null;

		private Label nameLabel = null;

		private Label messageLabel = null;

		private List detailListWidget = null;

		private AZProgressBar pBar = null;

		private StyledText actionLabel_multi_function = null;

		private StyledText actionLabel_remove = null;

		private StyledText actionLabel_detail = null;

		private GridData nameData = null;

		private GridData pbarData = null;

		private GridData messageData = null;

		private Composite leftPanel = null;

		private Composite middlePanel = null;

		private Composite rightPanel = null;

		public ReporterPanel(Composite parent, IProgressReporter reporter,
				boolean showSeparator) {
			super(parent, SWT.NONE);

			if (null == reporter) {
				throw new NullPointerException("ProgressReporter can not be null");//KN: should use resource
			}

			this.pReporter = reporter;

			ProgressReport pReport = pReporter.getProgressReport();

			setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

			GridLayout gLayout = new GridLayout(3, false);
			gLayout.horizontalSpacing = 0;
			setLayout(gLayout);

			/* ========================
			 * Creates the 3 main panels
			 */
			leftPanel = new Composite(this, SWT.NONE);
			GridData leftPanelData = new GridData(SWT.FILL, SWT.FILL, true, false);
			leftPanel.setLayoutData(leftPanelData);
			GridLayout leftLayout = new GridLayout();
			leftLayout.marginHeight = 0;
			leftLayout.marginWidth = 0;
			leftPanel.setLayout(leftLayout);

			middlePanel = new Composite(this, SWT.NONE);
			middlePanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			GridLayout middleLayout = new GridLayout();
			middleLayout.marginHeight = 0;
			middleLayout.marginWidth = 0;
			middlePanel.setLayout(middleLayout);

			rightPanel = new Composite(this, SWT.NONE);
			GridData rightPanelData = new GridData(SWT.FILL, SWT.FILL, true, false);
			rightPanel.setLayoutData(rightPanelData);
			rightPanel.setLayout(new GridLayout());

			/* =====================
			 * Creates the controls
			 */

			if (true == isStandalone) {

				/*
				 * Add the detail section
				 */
				detailListWidget = new List(this, SWT.BORDER | SWT.V_SCROLL);
				GridData detailGroupData = new GridData(SWT.FILL, SWT.FILL, true, true);
				detailGroupData.horizontalSpan = 3;
				detailGroupData.heightHint = 50;
				detailListWidget.setLayoutData(detailGroupData);

				/*
				 * And set the layout of the panel to fill all
				 */
				setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

				/*
				 * Sets the shell title to the title of the reporter if it's not null
				 */

				if (null != pReport.title && pReport.title.length() > 0) {
					shell.setText(pReport.title);
				}
			}

			if (true == showSeparator) {
				Label topSeparator = new Label(this, SWT.SEPARATOR | SWT.HORIZONTAL);
				GridData gData = new GridData(SWT.FILL, SWT.TOP, true, false);
				gData.horizontalSpan = 3;
				topSeparator.setLayoutData(gData);
			}

			/*
			 * in the left panel
			 */
			imageLabel = new Label(leftPanel, SWT.NONE);
			imageLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.TOP, true, false));

			/*
			 * in the middle panel
			 */
			nameLabel = new Label(middlePanel, SWT.WRAP);
			nameData = new GridData(SWT.FILL, SWT.TOP, true, false);
			nameLabel.setLayoutData(nameData);

			pBar = new AZProgressBar(middlePanel, pReport.isIndeterminate);
			pbarData = new GridData(SWT.CENTER, SWT.TOP, true, false);
			pBar.setLayoutData(pbarData);

			Label separator = new Label(middlePanel, SWT.SEPARATOR | SWT.HORIZONTAL);
			separator.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

			messageLabel = new Label(middlePanel, SWT.WRAP);
			messageData = new GridData(SWT.FILL, SWT.TOP, true, false);
			messageLabel.setLayoutData(messageData);

			/*
			 * in the right panel
			 */

			actionLabel_multi_function = new StyledText(rightPanel, SWT.READ_ONLY);
			GridData gData = new GridData(SWT.END, SWT.TOP, false, false);
			gData.widthHint = actionLabelMaxWidth;
			actionLabel_multi_function.setLayoutData(gData);

			actionLabel_remove = new StyledText(rightPanel, SWT.READ_ONLY);
			gData = new GridData(SWT.END, SWT.TOP, false, false);
			gData.widthHint = actionLabelMaxWidth;
			actionLabel_remove.setLayoutData(gData);

			/*
			 * Standalone window already shows the detail panel so no need to display the "Detail" label 
			 */
			if (false == isStandalone) {

				actionLabel_detail = new StyledText(rightPanel, SWT.READ_ONLY);
				gData = new GridData(SWT.END, SWT.TOP, false, false);
				gData.widthHint = actionLabelMaxWidth;
				actionLabel_detail.setLayoutData(gData);
			}

			/* ======================
			 * Init the controls
			 */

			if (null != pReport.image) {
				imageLabel.setImage(pReport.image);
			} else {
				imageLabel.setImage(defaultImage);
			}

			nameLabel.setText(pReport.name);

			if (true == pReport.isInErrorState) {
				showInErrorColor(messageLabel, nullToEmptyString(pReport.errorMessage),
						true);
			} else {
				showInErrorColor(messageLabel, nullToEmptyString(pReport.message),
						false);
			}

			showAsLink(actionLabel_remove, actionLabelText_remove, true);
			showAsLink(actionLabel_detail, actionLabelText_detail, true);

			/*
			 * Configures the action label appropriately
			 */
			configureActionLabel(pReport);

			/*
			 * Catch up on any detail messages we might have missed
			 */
			if (null != detailListWidget) {

				/*
				 * Add a default message instead of an empty box if there is no history;
				 * remove this later when a real detail message arrive
				 */
				if (pReport.detailMessageHistory.length < 1) {
					detailListWidget.add(NO_HISTORY_TO_DISPLAY);

					/*
					 * Make is disabled to turn the text gray so the user does not confuse the default
					 * message with an actual message from the reporter; we'll enable it once we have a
					 * valid detail message from the reporter
					 */
					detailListWidget.setEnabled(false);

				} else {
					for (int i = 0; i < pReport.detailMessageHistory.length; i++) {
						detailListWidget.add(pReport.detailMessageHistory[i]);
					}
				}
			}
			/*
			 * Layout the controls
			 * NOTE: These layout must be done AFTER the controls are initialized since we
			 * are calculating their sizes based on their content
			 */

			iconMargin = imageLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
			iconMargin += (inset * 2);
			leftPanelData.widthHint = leftPanel.computeSize(iconMargin, SWT.DEFAULT).x;
			rightPanelData.widthHint = actionLabel_multi_function.computeSize(
					actionLabelMaxWidth, labelMinHeight).x;

			synchProgressBar(pReport);

			/*
			 * Resize content when this panel changes size
			 */
			addListener(SWT.Resize, new Listener() {
				public void handleEvent(Event e) {
					resizeContent();
				}
			});

			/*
			 * Listener to mouse click on the action label
			 */
			actionLabel_multi_function.addMouseListener(new MouseAdapter() {
				public void mouseDown(MouseEvent e) {
					/*
					 * KN: Not exactly good programming but this works and it's simpler than
					 * inspecting the reporter directly
					 */
					if (actionLabelText_cancel.equals(actionLabel_multi_function.getText())) {
						pReporter.cancel();
					} else if (actionLabelText_retry.equals(actionLabel_multi_function.getText())) {
						pReporter.retry();
					} else if (actionLabelText_remove.equals(actionLabel_multi_function.getText())) {
						dispose();
					}
				}
			});

			/*
			 * Listener to mouse click on the extra action label
			 */
			actionLabel_remove.addMouseListener(new MouseAdapter() {
				public void mouseDown(MouseEvent e) {
					/*
					 * When active (is visible) this label only perform 1 action which is disposing this
					 */
					dispose();
				}
			});

			/*
			 * Listener to mouse click on the detail action label
			 */
			if (null != actionLabel_detail) {
				actionLabel_detail.addMouseListener(new MouseAdapter() {
					public void mouseDown(MouseEvent e) {
						/*
						 * This label only perform 1 action which is showing the current reporter in the detail window
						 */

						shell.getDisplay().asyncExec(new Runnable() {
							public void run() {
								ProgressReporterWindow.open(pReporter, NONE);
							}
						});

						/*
						 * Close the original shell since it was opened as MODAL; if left opened it will intercept UI input
						 * and interfere with the new window
						 */
						shell.getDisplay().asyncExec(new Runnable() {
							public void run() {
								shell.close();
							}
						});
					}
				});
			}

			/*
			 * Listens to events from the reporter and take appropriate action(s) to update the UI
			 */
			reporterListener = new IProgressReporterListener() {
				public int report(ProgressReport pReport) {
					return updateControls(pReport);
				}
			};
			pReporter.addListener(reporterListener);
		}

		/**
		 * Sets the background of all controls 
		 * @param useAlternateColor <code>true</code> to use <code>alternatingBackground</code>; otherwise use default shell color
		 */
		public void setBackground(boolean useAlternateColor) {

			//KN: TODO --  clean up this list... are all these required?
			if (true == useAlternateColor) {
				scrollChild.setBackground(alternatingBackground);
				setBackground(alternatingBackground);
				imageLabel.setBackground(alternatingBackground);
				nameLabel.setBackground(alternatingBackground);
				pBar.setBackground(alternatingBackground);
				messageLabel.setBackground(alternatingBackground);
				leftPanel.setBackground(alternatingBackground);
				rightPanel.setBackground(alternatingBackground);
				middlePanel.setBackground(alternatingBackground);

			} else {

				Color shellBackground = shell.getBackground();
				imageLabel.setBackground(shellBackground);
				nameLabel.setBackground(shellBackground);
				pBar.setBackground(shellBackground);
				messageLabel.setBackground(shellBackground);
				leftPanel.setBackground(shellBackground);
				rightPanel.setBackground(shellBackground);
				middlePanel.setBackground(shellBackground);

			}
		}

		/**
		 * Updates UI elements based on the given <code>ProgressReport</code>
		 * 
		 * @param pReport
		 */
		private int updateControls(final ProgressReport pReport) {
			if (null == pReport || null == shell || true == shell.isDisposed()
					|| null == shell.getDisplay()) {
				return RETVAL_OK;
			}

			/* Note each 'case' statement of the 'switch' block performs its UI update encapsulated in an .asyncExec()
			 * so that the UI does not freeze or flicker.  It may be tempting to encapsulate the whole 'switch' block
			 * within one .asyncExec() but then a separate loop would be required to wait for the Runnable to finish
			 * before attaining the correct return code.  The alternative would be to encapsulate the 'switch' block
			 * in a .syncExec() but that would cause freezing and flickering
			 */

			switch (pReport.REPORT_TYPE) {
				case REPORT_TYPE_PROPERTY_CHANGED:

					shell.getDisplay().asyncExec(new Runnable() {
						public void run() {
							if (null != nameLabel && false == nameLabel.isDisposed()) {
								nameLabel.setText(nullToEmptyString(pReport.name));
							}
							showInErrorColor(messageLabel,
									nullToEmptyString(pReport.message), false);
							synchProgressBar(pReport);
							updateDetailWidget(pReport);
							configureActionLabel(pReport);
							resizeContent();
						}
					});
					break;
				case REPORT_TYPE_CANCEL:
					shell.getDisplay().asyncExec(new Runnable() {
						public void run() {
							synchProgressBar(pReport);
							showInErrorColor(messageLabel,
									nullToEmptyString(pReport.message), false);
							configureActionLabel(pReport);
							resizeContent();
						}
					});
					return RETVAL_OK;
				case REPORT_TYPE_DONE:
					shell.getDisplay().asyncExec(new Runnable() {
						public void run() {

							if (true == isStandalone && ((style & AUTO_CLOSE) != 0)) {
								shell.close();
							} else {

								synchProgressBar(pReport);
								showInErrorColor(messageLabel,
										nullToEmptyString(pReport.message), false);
								configureActionLabel(pReport);
								resizeContent();
							}
						}
					});

					return RETVAL_OK_TO_DISPOSE;
				case REPORT_TYPE_MODE_CHANGE:
					shell.getDisplay().asyncExec(new Runnable() {
						public void run() {
							if (null != pBar && false == pBar.isDisposed()) {
								pBar.setIndeterminate(pReport.isIndeterminate);
							}
						}
					});
					break;
				case REPORT_TYPE_ERROR:
					shell.getDisplay().asyncExec(new Runnable() {
						public void run() {
							showInErrorColor(messageLabel,
									nullToEmptyString(pReport.errorMessage), true);
							configureActionLabel(pReport);
							synchProgressBar(pReport);
							resizeContent();
						}
					});
					break;

				case REPORT_TYPE_RETRY:
					shell.getDisplay().asyncExec(new Runnable() {
						public void run() {
							showInErrorColor(messageLabel,
									nullToEmptyString(pReport.message), false);
							configureActionLabel(pReport);
							synchProgressBar(pReport);
							resizeContent();
						}
					});

					break;
				default:
					break;
			}

			return RETVAL_OK;
		}

		/**
		 * Convenience method to return an empty string if the given string is null.
		 * <p>SWT text controls do not allow a null argument as a value and will throw an exception if a <code>null</code> is encountered</p>
		 * @param string
		 * @return
		 */
		private String nullToEmptyString(String string) {
			return null == string ? "" : string;
		}

		/**
		 * Synchronize the progress bar with the given <code>ProgressReport</code>
		 * @param pReport
		 */
		private void synchProgressBar(ProgressReport pReport) {
			if (null == pBar || pBar.isDisposed() || null == pReport) {
				return;
			}

			if (false == pReport.isActive) {
				showProgressBar(false);
			} else {
				pBar.setIndeterminate(pReport.isIndeterminate);
				if (false == pReport.isIndeterminate) {
					pBar.setMinimum(pReport.minimum);
					pBar.setMaximum(pReport.maximum);
				}
				pBar.setSelection(pReport.selection);
				showProgressBar(true);
			}

		}

		/**
		 * 
		 * Show or hide the progress bar
		 * @param showProgressBar
		 */
		private void showProgressBar(boolean showProgressBar) {
			if (null == pBar || pBar.isDisposed()) {
				return;
			}
			if (true == showProgressBar) {
				pbarData.heightHint = 25;
			} else {
				pbarData.heightHint = 0;
			}
			layout();
		}

		/**
		 * Sets the defined color to the given <code>label</code>
		 * @param label
		 * @param text
		 * @param showError <code>true</code> to show as error; <code>false</code> otherwise
		 */
		private void showInErrorColor(Label label, String text, boolean showError) {
			if (null == label || label.isDisposed()) {
				return;
			}
			label.setText(text + "");
			if (false == showError) {
				label.setForeground(normalColor);
			} else {
				label.setForeground(errorColor);
			}
			label.update();

		}

		/**
		 * Configure the given <code>StyledText</code> to be displayed as a link
		 * <p>Note: we are creating a custom link behavior instead of using {@link org.eclipse.swt.widgets.Link}
		 * because that widget automatically draw a rectangle around the link which is not what we want; we only
		 * want to show an underlined text that is clickable</p>
		 * 
		 * @param actionLabel_multi_function
		 * @param text the text to display; note that should the text value changes for the given label
		 *  this method must be called again to re-apply the link behavior
		 */
		private void showAsLink(StyledText actionLabel, String text, boolean enabled) {
			if (null == actionLabel || true == actionLabel.isDisposed()) {
				return;
			}
			actionLabel.setText(text);
			actionLabel.setCursor(Cursors.handCursor);

			if (true == enabled) {
				actionLabel.setForeground(activeLinkColor);
			} else {
				actionLabel.setForeground(disabledLinkColor);
			}

			actionLabel.setEnabled(enabled);
			StyleRange style1 = new StyleRange();
			style1.start = 0;
			style1.length = actionLabel.getCharCount();
			style1.underline = true;
			actionLabel.setStyleRange(style1);
			actionLabel.getCaret().setVisible(false);
			actionLabel.update();

		}

		/**
		 * Display the appropriate text for the action labels
		 * based on what action can be taken
		 */
		private void configureActionLabel(ProgressReport pReport) {
			if (null == actionLabel_remove || null == actionLabel_multi_function
					|| true == actionLabel_remove.isDisposed()
					|| true == actionLabel_multi_function.isDisposed()) {
				return;
			}

			/*
			 * There are 2 labels that can be clicked on; base on the state of the reporter itself.
			 * The basic rules are these:
			 * 	If it's in error
			 * 		and retry is allowed
			 * 		then show "retry" and "remove"
			 * 		else just show "remove"
			 * 
			 * 	If it's been canceled
			 * 		and retry is allowed
			 * 		then show "retry" and "remove"
			 * 		else just show "remove"
			 * 
			 * 	If it's done
			 * 		then show just "remove"
			 * 
			 * 	If it's none of the above
			 * 		then show just the "cancel" label
			 * 			enable the label if cancel is allowed
			 * 			else disable the label
			 * 
			 */

			actionLabel_remove.setVisible(false);

			if (true == pReport.isInErrorState) {
				if (true == pReport.isRetryAllowed) {
					showAsLink(actionLabel_multi_function, actionLabelText_retry, true);
					actionLabel_remove.setVisible(true);
				} else {
					showAsLink(actionLabel_multi_function, actionLabelText_remove, true);
				}

			} else if (true == pReport.isCanceled) {
				if (true == pReport.isRetryAllowed) {
					showAsLink(actionLabel_multi_function, actionLabelText_retry, true);
					actionLabel_remove.setVisible(true);
				} else {
					showAsLink(actionLabel_multi_function, actionLabelText_remove, true);
				}

			} else if (true == pReport.isDone) {
				showAsLink(actionLabel_multi_function, actionLabelText_remove, true);
			} else {
				showAsLink(actionLabel_multi_function, actionLabelText_cancel,
						pReport.isCancelAllowed);
			}

		}

		private void updateDetailWidget(ProgressReport pReport) {
			if (null == detailListWidget || detailListWidget.isDisposed()) {
				return;
			}

			if (null != pReport.detailMessage && pReport.detailMessage.length() > 0) {
				detailListWidget.add(pReport.detailMessage);

				/*
				 * We added a default message at init so if it's still there then remove it
				 */
				if (detailListWidget.getItemCount() == 2) {
					if (true == NO_HISTORY_TO_DISPLAY.equals(detailListWidget.getItem(0))) {
						detailListWidget.remove(0);
						detailListWidget.setEnabled(true);
					}
				}
			}
		}

		/**
		 * Resizes the content of this panel to fit within the shell and to layout children control appropriately
		 */
		public void resizeContent() {
			int maxWidth = getClientArea().width;
			maxWidth -= iconMargin;
			maxWidth -= actionLabelMaxWidth;
			nameData.widthHint = maxWidth;
			pbarData.widthHint = maxWidth;
			messageData.widthHint = maxWidth;
			shell.layout(true, true);

		}

		public void dispose() {
			imageLabel.dispose();
			nameLabel.dispose();
			pBar.dispose();
			actionLabel_multi_function.dispose();
			actionLabel_remove.dispose();
			messageLabel.dispose();
			pReporter.removeListener(reporterListener);
			pReporter.dispose();
			super.dispose();
		}
	}

}
