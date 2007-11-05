package org.gudy.azureus2.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ProgressBar;

/**
 * A ProgressBar implementation that allows the on-the-fly switching between determinate and indeterminate modes.
 * @author knguyen
 *
 */
public class AZProgressBar
	extends Composite
{

	private Composite progressPane = null;

	private ProgressBar incrementalProgressBar = null;

	private ProgressBar indeterminateProgressBar = null;

	private boolean isIndeterminate = false;

	private StackLayout stack = null;

	/**
	 * Construct a progress bar initialized as incremental and no input button
	 * @param parent
	 */
	public AZProgressBar(Composite parent) {
		this(parent, false);
	}

	/**
	 * 
	 * @param parent
	 * @param isIndeterminate
	 * @param useInputButton determines whether the <code>inputButton</code> is available or not
	 * @param image an <code>Image</code> to display; may be null
	 */
	public AZProgressBar(Composite parent, boolean isIndeterminate) {
		super(parent, SWT.NULL);
		setLayout(new GridLayout());

		/*
		 * Sub panel for the progress bars
		 */
		progressPane = new Composite(this, SWT.NONE);
		progressPane.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		incrementalProgressBar = new ProgressBar(progressPane, SWT.HORIZONTAL);
		indeterminateProgressBar = new ProgressBar(progressPane, SWT.HORIZONTAL
				| SWT.INDETERMINATE);

		stack = new StackLayout();
		progressPane.setLayout(stack);
		progressPane.pack();

		setIndeterminate(isIndeterminate);
	}

	public void setIndeterminate(boolean isIndeterminate) {
		if (this.isIndeterminate != isIndeterminate || null == stack.topControl) {
			this.isIndeterminate = isIndeterminate;
			if (true == isIndeterminate) {
				stack.topControl = indeterminateProgressBar;
			} else {
				incrementalProgressBar.setMinimum(0);
				incrementalProgressBar.setMaximum(100);
				incrementalProgressBar.setSelection(0);
				stack.topControl = incrementalProgressBar;
			}
			progressPane.layout();
		}
	}

	public void done() {
		incrementalProgressBar.setSelection(incrementalProgressBar.getMaximum());
		stack.topControl = null;
		progressPane.layout();
	}

	public void setSelection(int value) {
		if (incrementalProgressBar.getMaximum() < value) {
			done();
		} else {
			incrementalProgressBar.setSelection(value);
		}
	}

	public void setPercentage(final int percentage) {
		if (percentage > 0 && percentage < 101) {

			int range = incrementalProgressBar.getMaximum()
					- incrementalProgressBar.getMinimum();
			setSelection(incrementalProgressBar.getMinimum()
					+ (range * percentage / 100));

		}

	}

	public int getMaximum() {
		return incrementalProgressBar.getMaximum();
	}

	public int getMinimum() {
		return incrementalProgressBar.getMinimum();
	}

	public int getSelection() {
		return incrementalProgressBar.getSelection();
	}

	public void setMaximum(int value) {
		incrementalProgressBar.setMaximum(value);
	}

	public void setMinimum(int value) {
		incrementalProgressBar.setMinimum(value);
	}

	public boolean isIndeterminate() {
		return isIndeterminate;
	}

}
