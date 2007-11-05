package org.gudy.azureus2.ui.swt.progress;

/**
 * A simple listener that can be registered with the ProgressReportingManager to receive notification
 * when any ProgressReporter has a status change
 * @author knguyen
 *
 */
public interface IProgressReportingListener
	extends IProgressReportConstants
{
	/**
	 * Notify that some changes has happened
	 * @param reporter The <code>ProgressReporter</code> that reported the change;
	 * @return 
	 */
	public int reporting(IProgressReporter reporter);
}