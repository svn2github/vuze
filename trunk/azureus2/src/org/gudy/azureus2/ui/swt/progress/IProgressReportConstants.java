package org.gudy.azureus2.ui.swt.progress;

/**
 * These are all the constants used by the ProgressReporter and related classes
 * <p>
 * These constants are in this separate interface so that classes that need to reference these
 * constants can simply implement this interface and reference them directly like REPORT_TYPE_CANCEL == [some test]
 * instead of having to fully reference them like IProgressReportConstants.REPORT_TYPE_CANCEL == [some test]</p>
 * @author knguyen
 *
 */
public interface IProgressReportConstants
{

	/**
	 * Unless specified by the user for a particular reporter all reporters have this default type
	 */
	public static final String REPORTER_TYPE_DEFAULT = "default.reporter.type";

	//======= report types =============

	/**
	 * Default event type indicating no event 
	 */
	public static final int REPORT_TYPE_INIT = 0;

	/**
	 * When {@link ProgressReporter#cancel()} is detected
	 */
	public static final int REPORT_TYPE_CANCEL = 1;

	/**
	 * When {@link ProgressReporter#setDone()} is detected
	 */
	public static final int REPORT_TYPE_DONE = 2;

	/**
	 * When {@link ProgressReporter#setIndeterminate(boolean)} is detected
	 */
	public static final int REPORT_TYPE_MODE_CHANGE = 3;

	/**
	 * When {@link ProgressReporter#setErrorMessage(String)} is detected
	 */
	public static final int REPORT_TYPE_ERROR = 4;

	/**
	 * When {@link ProgressReporter#retry()} is detected
	 */
	public static final int REPORT_TYPE_RETRY = 5;

	/**
	 * When any other property is modified
	 */
	public static final int REPORT_TYPE_PROPERTY_CHANGED = 6;

	/**
	 * When {@link ProgressReporter#dispose()} is called
	 */
	public static final int REPORT_TYPE_DISPOSED = 7;

	//========== return values for report listeners ======

	/**
	 * Default return value from a listener indicating the event has been received and processed successfully
	 */
	public static final int RETVAL_OK = 0;

	/**
	 * A return value from a listener indicating that the listener is done and is no longer interested
	 * in any subsequent event; this is a hint to the notifier so that the notifier can perform clean up
	 * operation relating to that particular listener
	 */
	public static final int RETVAL_OK_TO_DISPOSE = 1;

}