package org.gudy.azureus2.ui.swt.progress;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;

/**
 * A implementation of <code>IProgressReporter</code>
 * <p>Any process wishing to participate in providing global progress indication can instantiate
 * this class and simply use the available methods so set values or issue a command
 * 
 * <p>
 * When ever any state changes in this reporter a notification will be sent to the global
 * reporting manager {@link ProgressReportingManager} followed by a notification to all
 * registered listeners of this reporter</p>
 * 
 * The listeners will be passed an immutable progress report {@link ProgressReporter.ProgressReport} which
 * represents a snapshot of all the public properties contained in this reporter; inspecting the
 * ProgressReport is the only way a listener can query the properties of this reporter.  This pattern
 * allows the ProgressReporter to continue and process requests by not having to thread lock all public
 * methods.  Additionally, the listeners are guaranteed a consistent snapshot of the reporter.
 * 
 * <p>
 * This reporter also has the capability to receive loop back commands from a listener for actions such like
 * {@link #cancel()} and {@link #retry()}.  These commands are enabled by calling 
 * 		{@link ProgressReporter#setCancelAllowed(boolean)} 
 * or {@link ProgressReporter#setRetryAllowed(boolean)}.
 * 
 * The listener only initiates these actions by sending a notification back to the owner of the reporter;
 * it is up the to owner to perform the actual act of canceling or retrying.
 * </p><p>
 * 
 * A typical life cycle of the ProgresReporter as seen from an owner is as follows 
 * (an owner is defined as the process that created the reporter):
 * <ul>
 * <li>Create ProgressReporter</li>
 * <li>Set initial properties</li>
 * <li>Register a listener to the reporter to respond to loopback notifications (optional)</li>
 * <li>Update the reporter</li>
 * <ul>
 * <li>Set selection or percentage [{@link ProgressReporter#setSelection(int, String)}, {@link ProgressReporter#setPercentage(int, String)}]</li>
 * <li>Set message [{@link ProgressReporter#setMessage(String)}]</li>
 * <li>...</li>
 * <li>Repeat until done</li>
 * <li>Set done [{@link ProgressReporter#setDone()}]</li>
 * </ul>
 * 
 * <li>Then optionally Dispose of the reporter [{@link ProgressReporter#dispose(Object)}]</li>.<p>
 * In addition to internal clean-ups, calling dispose(Object) will effectively remove the reporter from the history stack of the
 * reporting manager and no more messages from this reporter will be processed.</P>
 * </ul></p><p>
 * 
 * Once a reporter is created and any property in the reporter is set the global reporting manager is
 * notified; at which point any listener listening to the manager is forwarded this reporter.
 * The manager listener may decide to display this reporter in a UI element, may register specific
 * listeners to this reporter, may query its properties and take action, or can simply monitor it
 * for such functions as logging.</p>
 * 
 * This implementation is non-intrusive to the owner process and so provides existing processes the
 * ability to participate in global progress indication without significant modification to the underlying
 * processes.
 * 
 * 
 * 
 * @author knguyen
 *
 */
public class ProgressReporter
	implements IProgressReporter, IProgressReportConstants
{


	private ProgressReportingManager manager = null;

	/**
	 * An instance id for this reporter that is guaranteed to be unique within this same session 
	 * It is declared as transient to help ensure no accidental reliance on the persistence of this ID
	 * between sessionS (just in case this class is declared serializable in a future modification)
	 */
	private transient int ID;

	private int minimum, maximum, selection, percentage;

	private int latestReportType = REPORT_TYPE_INIT;

	private boolean isIndeterminate, isDone, isPercentageInUse, isCancelAllowed,
			isCanceled, isRetryAllowed, isInErrorState, isDisposed;

	private String title = "";

	private String message = "";

	/**
	 * Accumulates the detail messages in a List
	 * <p>This is for when a listener starts listening to this reporter after it has started running;
	 * upon initialization the listener may query this list to get all messages sent up to that point.</p>
	 * 
	 */
	private List detailMessageHistory = new ArrayList();

	private String detailMessage = "";

	private String errorMessage = "";

	private String name = "";

	private Image image = null;

	private String reporterType = REPORTER_TYPE_DEFAULT;

	private List reporterListeners = null; //KN: Lazy init since not all reporters will have direct listeners

	/**
	 * An arbitrary object reference that can be used by the owner of the <code>ProgressReporter</code> and its
	 * listeners to share additional information should the current implementation be insufficient
	 */
	private Object objectData = null;

	/**
	 * Construct a <code>ProgressReporter</code>; the returned instance is initialized with the proper ID
	 */
	public ProgressReporter() {
		this(null);
	}


	/**
	 * Construct a <code>ProgressReporter</code> with the given <code>name</code>; the returned
	 * instance would have been initialized with the proper ID
	 * @param name
	 */
	public ProgressReporter(String name) {
		manager = ProgressReportingManager.getInstance();
		this.name = name;
		this.ID = manager.getNextAvailableID();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#setReporterType(String)
	 */
	public void setReporterType(String reporterType) {
		this.reporterType = reporterType;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#dispose()
	 */
	public void dispose() {

		/*
		 * Disposed already so no need to do it again
		 */
		if (true == isDisposed) {
			return;
		}

		isDisposed = true;
		latestReportType = REPORT_TYPE_DISPOSED;

		/*
		 * Notifies listeners
		 */
		notifyListeners();

		/*
		 * Then dispose all listeners to let GC do it's magic.
		 * This is just to do final clearing since at this point most listeners should have been disposed
		 * already.
		 */
		if (null != reporterListeners) {
			synchronized (reporterListeners) {
				reporterListeners.clear();
			}
		}

		/*
		 * Finally notifies the manager
		 */
		manager.notifyManager(this);
	}


	/**
	 * Resets this reporter to its initial states such that values are reset to default
	 * <p>An appropriate use for this is when a process is restarting or retrying; this allows an owning process
	 * to keep on using the same instance of this reporter without having to create and dispatch a new one</p>
	 */
	private void reInit() {
		isCanceled = false;
		isDone = false;
		isInErrorState = false;
		errorMessage = "";
		message = "";
		detailMessage = "";
		detailMessageHistory.clear();
	}

	/**
	 * Notifies registered listener that an event has occurred.
	 * Subsequently a listener may be removed if it returns the value of  <code>RETVAL_OK_TO_DISPOSE</code>;
	 * this optimization is designed to prevent dangling/orphaned listeners, and also reduces the
	 * number of listeners to notify upon the next event 
	 */
	private void notifyListeners() {
		if (null == reporterListeners || true == reporterListeners.isEmpty()) {
			return;
		}

		/*
		 * Take a snap shot of the reporter
		 */
		IProgressReport pReport = getProgressReport();

		synchronized (reporterListeners) {

			for (Iterator iterator = reporterListeners.iterator(); iterator.hasNext();) {
				IProgressReporterListener listener = ((IProgressReporterListener) iterator.next());

				/*
				 * If the listener returned RETVAL_OK_TO_DISPOSE then it has indicated that it is no longer needed so we release it
				 */
				if (RETVAL_OK_TO_DISPOSE == listener.report(pReport)) {
					iterator.remove();
				}
			}
		}
	}

	/**
	 * Updates and notifies listeners
	 * @param REPORT_TYPE
	 */
	private void updateAndNotify(int eventType) {
		latestReportType = eventType;
		/*
		 * We directly bubble up this event to the manager for efficiency;
		 * as opposed to having the manager register as a listener to each and every ProgressReporter.
		 * Effectively this allows the manager to receive all events from all reporters 
		 */
		manager.notifyManager(this);

		/*
		 * If a property has changed but the reporter has been canceled then don't notify the listener
		 * since they are no longer expecting a REPORT_TYPE_PROPERTY_CHANGED event
		 */
		if (eventType == REPORT_TYPE_PROPERTY_CHANGED && true == isCanceled) {
			return;
		}

		/*
		 * Now we can notify listeners for this specific reporter
		 */
		notifyListeners();
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#setSelection(int, java.lang.String)
	 */
	public void setSelection(int selection, String message) {
		if (true == shouldIgnore()) {
			return;
		}
		if (selection >= maximum) {
			setDone();
			return;
		}
		if (selection < minimum) {
			percentage = 0;
			selection = minimum;
			isIndeterminate = true;
			return;
		}
		this.selection = selection;
		this.message = message;
		percentage = (selection * 100) / (maximum - minimum);
		isDone = false;
		isPercentageInUse = false;
		isIndeterminate = false;
		updateAndNotify(REPORT_TYPE_PROPERTY_CHANGED);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#setPercentage(int, java.lang.String)
	 */
	public void setPercentage(int percentage, String message) {
		if (true == shouldIgnore()) {
			return;
		}

		if (percentage >= 100) {
			setDone();
			return;
		}

		if (percentage < 0) {
			percentage = 0;
			selection = minimum;
			isIndeterminate = true;
			return;
		}
		minimum = 0;
		maximum = 100;
		this.percentage = percentage;
		this.message = message;
		this.selection = percentage;
		isDone = false;
		isPercentageInUse = true;
		isIndeterminate = false;
		updateAndNotify(REPORT_TYPE_PROPERTY_CHANGED);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#setIndeterminate(boolean)
	 */
	public void setIndeterminate(boolean isIndeterminate) {
		if (true == shouldIgnore()) {
			return;
		}

		this.isIndeterminate = isIndeterminate;
		if (true == isIndeterminate) {
			minimum = 0;
			maximum = 0;
		}
		updateAndNotify(REPORT_TYPE_MODE_CHANGE);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#setDone()
	 */
	public void setDone() {
		if (true == shouldIgnore()) {
			return;
		}

		isDone = true;
		selection = maximum;
		percentage = 100;
		message = MessageText.getString("Progress.reporting.status.finished");
		updateAndNotify(REPORT_TYPE_DONE);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#setMessage(java.lang.String)
	 */
	public void setMessage(String message) {
		if (true == shouldIgnore()) {
			return;
		}
		this.message = message;
		updateAndNotify(REPORT_TYPE_PROPERTY_CHANGED);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#setDetailMessage(java.lang.String)
	 */
	public void appendDetailMessage(String detailMessage) {
		if (true == shouldIgnore()) {
			return;
		}
		this.detailMessage = detailMessage;

		/*
		 * Limiting the history list to prevent runaway processes from taking up too much resources
		 * KN: TODO implement something better than just this arbitrary limit
		 */
		if (detailMessageHistory.size() < 300) {
			detailMessageHistory.add(detailMessage);
		} else if (detailMessageHistory.size() == 300) {
			Debug.out(new Exception(
					MessageText.getString("Progress.reporting.detail.history.limit")));
		}

		updateAndNotify(REPORT_TYPE_PROPERTY_CHANGED);

		/*
		 * The detail message operates in append mode so after we have notified all the listeners
		 * we reset it.  It is up to the listeners to accumulate the messages.
		 * 
		 * Lazy implementor can also simply use the detailMessageHistory
		 */
		this.detailMessage = "";
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#setMinimum(int)
	 */
	public void setMinimum(int min) {
		if (true == shouldIgnore()) {
			return;
		}
		this.minimum = min;
		updateAndNotify(REPORT_TYPE_PROPERTY_CHANGED);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#setMaximum(int)
	 */
	public void setMaximum(int max) {
		if (true == shouldIgnore()) {
			return;
		}
		this.maximum = max;
		updateAndNotify(REPORT_TYPE_PROPERTY_CHANGED);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#cancel()
	 */
	public synchronized void cancel() {
		if (true == isCanceled || true == shouldIgnore()) {
			return;
		}

		isCanceled = true;
		message = MessageText.getString("Progress.reporting.status.canceled");
		updateAndNotify(REPORT_TYPE_CANCEL);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#retry()
	 */
	public synchronized void retry() {
		if (true == shouldIgnore()) {
			return;
		}
		reInit();
		message = MessageText.getString("Progress.reporting.status.retrying");
		updateAndNotify(REPORT_TYPE_RETRY);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#setCancelAllowed(boolean)
	 */
	public void setCancelAllowed(boolean cancelAllowed) {
		if (true == shouldIgnore()) {
			return;
		}

		this.isCancelAllowed = cancelAllowed;
		updateAndNotify(REPORT_TYPE_PROPERTY_CHANGED);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#setName(java.lang.String)
	 */
	public void setName(String name) {
		if (true == shouldIgnore()) {
			return;
		}
		this.name = name + ""; //KN: Just a quick way to ensure the name is not null
		updateAndNotify(REPORT_TYPE_PROPERTY_CHANGED);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#setTitle(java.lang.String)
	 */
	public void setTitle(String title) {
		if (true == shouldIgnore()) {
			return;
		}
		this.title = title;
		updateAndNotify(REPORT_TYPE_PROPERTY_CHANGED);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#setImage(org.eclipse.swt.graphics.Image)
	 */
	public void setImage(Image image) {
		if (true == shouldIgnore()) {
			return;
		}
		this.image = image;
		updateAndNotify(REPORT_TYPE_PROPERTY_CHANGED);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#setErrorMessage(java.lang.String)
	 */
	public void setErrorMessage(String errorMessage) {
		if (true == shouldIgnore()) {
			return;
		}
		if (null == errorMessage || errorMessage.length() < 1) {
			this.errorMessage = MessageText.getString("Progress.reporting.default.error");
		} else {
			this.errorMessage = errorMessage;
		}
		isInErrorState = true;
		updateAndNotify(REPORT_TYPE_ERROR);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#setRetryAllowed(boolean)
	 */
	public void setRetryAllowed(boolean retryOnError) {
		if (true == shouldIgnore()) {
			return;
		}
		this.isRetryAllowed = retryOnError;
	}

	/**
	 * A convenience method to return whether this reporter should ignore subsequent calls to its accessor methods.
	 * When a reporter has reached this state listeners usually make the assumption that there will be no
	 * more changes to any properties in this reporter.  The use of this method is intended to provide a more consistent
	 * state for the reporter; without this check a listener may have reference to a reporter that is marked as <code>done</code>
	 * but then a subsequent message says it's at 50% completion.  By cutting of the notification here we prevent such 
	 * ambiguity from occurring for the listeners.
	 * <p>
	 * If any of the following states have been reached then the reporter can ignore subsequent calls to its accessor methods:
	 * <ul>
	 * <li><code>isDisposed</code> 			== 	<code>true</code></li>
	 * <li><code>isDone</code> 					== 	<code>true</code></li>
	 * </ul>
	 * </p>
	 * @return
	 */
	private boolean shouldIgnore() {
		return (true == isDisposed || true == isDone);
	}

	/**
	 * A convenience method to return whether this reporter has reached a state considered inactive.
	 * <p>
	 * If any of the following states have been reached then the reporter is considered inactive:
	 * <ul>
	 * <li><code>isDisposed</code> 			== 	<code>true</code></li>
	 * <li><code>isDone</code> 					== 	<code>true</code></li>
	 * <li><code>isInErrorState</code>	==	<code>true</code></li>
	 * <li><code>isCanceled</code> 			==	<code>true</code></li>
	 * </ul>
	 * </p>
	 * @return <code>true</code> if any of the above is met; <code>false</code> otherwise
	 */
	private boolean isActive() {
		return false == (true == isDisposed || true == isDone
				|| true == isInErrorState || true == isCanceled);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#setObjectData(java.lang.Object)
	 */
	public void setObjectData(Object objectData) {
		if (true == shouldIgnore()) {
			return;
		}
		this.objectData = objectData;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#addListener(org.gudy.azureus2.ui.swt.mainwindow.IProgressReporterListener)
	 */
	public synchronized void addListener(IProgressReporterListener listener) {
		if (true == shouldIgnore()) {
			return;
		}
		if (null == reporterListeners) {
			reporterListeners = new ArrayList();
		}
		synchronized (reporterListeners) {
			reporterListeners.add(listener);
		}
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#removeListener(org.gudy.azureus2.ui.swt.mainwindow.IProgressReporterListener)
	 */
	public void removeListener(IProgressReporterListener listener) {
		if (null == reporterListeners) {
			return;
		}
		synchronized (reporterListeners) {
			reporterListeners.remove(listener);
		}
	}

	/**
	 * Numerically compare by reporter ID's
	 */
	public int compareTo(Object obj) {
		if (obj instanceof IProgressReporter) {
			int targetID = obj.hashCode(); //KN: Will this always work as expected as opposed to using ((IProgressReporter)obj).getID()?
			return (ID < targetID ? -1 : (ID == targetID ? 0 : 1));
		}
		return 0;
	}

	/**
	 * Reporters are equal iif they have the same ID
	 */
	public boolean equals(Object obj) {
		if (obj instanceof IProgressReporter) {
			return ID == obj.hashCode();//KN: Will this always work as expected as opposed to using ((IProgressReporter)obj).getID()?
		}
		return false;
	}

	/**
	 * Hashcode and ID are the same
	 */
	public int hashCode() {
		return ID;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.mainwindow.IProgressReporter#getProgressReport()
	 */
	public synchronized IProgressReport getProgressReport() {
		return new ProgressReport();
	}

	/**
	 * An immutable object containing all interesting values in a <code>ProgressReporter</code>.
	 * <p>This represents a snapshot of all values at a single moment so instantiation of this class
	 * should be guarded against multi-threaded modification of the source <code>ProgressReporter</code> </p>
	 * 
	 * <p>This class is the only way an observer can query the properties of a <code>ProgressReporter</code>;
	 * though they do not have to be, all variables are declared <code>final</code> to help remind the user of this class
	 * that modification to any of its properties would have no effect on the reporter itself.
	 * <p>
	 * An exception to this insulation is the <code>objectData</code> variable; both the reporter
	 * and the ProgressReport consumer have full access to it.  This is to facilitate advanced
	 * 2-way communication between the 2 parties.</p> 
	 * 
	 * 
	 * @author knguyen
	 * @see ProgressReporter#getProgressReport()
	 */
	public class ProgressReport implements IProgressReport
	{
		private String reporterType = ProgressReporter.this.reporterType;

		private int reporterID = ProgressReporter.this.ID;

		private int minimum = ProgressReporter.this.minimum;

		private int maximum = ProgressReporter.this.maximum;

		private int selection = ProgressReporter.this.selection;

		private int percentage = ProgressReporter.this.percentage;

		private boolean isActive = ProgressReporter.this.isActive();

		private boolean isIndeterminate = ProgressReporter.this.isIndeterminate;

		private boolean isDone = ProgressReporter.this.isDone;

		private boolean isPercentageInUse = ProgressReporter.this.isPercentageInUse;

		private boolean isCancelAllowed = ProgressReporter.this.isCancelAllowed;

		public final boolean isCanceled = ProgressReporter.this.isCanceled;

		private boolean isRetryAllowed = ProgressReporter.this.isRetryAllowed;

		private boolean isInErrorState = ProgressReporter.this.isInErrorState;

		private boolean isDisposed = ProgressReporter.this.isDisposed;

		private String title = ProgressReporter.this.title;

		private String message = ProgressReporter.this.message;

		private String detailMessage = ProgressReporter.this.detailMessage;

		/*
		 * Converting to array so the original list is insulated from modification
		 * KN: this might be too costly if the list is large so might need to implement something better
		 */
		private String[] detailMessageHistory = (String[]) ProgressReporter.this.detailMessageHistory.toArray(new String[ProgressReporter.this.detailMessageHistory.size()]);

		private String errorMessage = ProgressReporter.this.errorMessage;

		private String name = ProgressReporter.this.name;

		private Image image = ProgressReporter.this.image;

		private Object objectData = ProgressReporter.this.objectData;

		private int REPORT_TYPE = ProgressReporter.this.latestReportType;

		/**
		 * Construct a ProgressReport
		 */
		private ProgressReport() {
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#getReporterType()
		 */
		public String getReporterType() {
			return reporterType;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#getReporterID()
		 */
		public int getReporterID() {
			return reporterID;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#getMinimum()
		 */
		public int getMinimum() {
			return minimum;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#getMaximum()
		 */
		public int getMaximum() {
			return maximum;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#getSelection()
		 */
		public int getSelection() {
			return selection;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#getPercentage()
		 */
		public int getPercentage() {
			return percentage;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#isActive()
		 */
		public boolean isActive() {
			return isActive;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#isIndeterminate()
		 */
		public boolean isIndeterminate() {
			return isIndeterminate;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#isDone()
		 */
		public boolean isDone() {
			return isDone;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#isPercentageInUse()
		 */
		public boolean isPercentageInUse() {
			return isPercentageInUse;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#isCancelAllowed()
		 */
		public boolean isCancelAllowed() {
			return isCancelAllowed;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#isCanceled()
		 */
		public boolean isCanceled() {
			return isCanceled;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#isRetryAllowed()
		 */
		public boolean isRetryAllowed() {
			return isRetryAllowed;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#isInErrorState()
		 */
		public boolean isInErrorState() {
			return isInErrorState;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#isDisposed()
		 */
		public boolean isDisposed() {
			return isDisposed;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#getTitle()
		 */
		public String getTitle() {
			return title;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#getMessage()
		 */
		public String getMessage() {
			return message;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#getDetailMessage()
		 */
		public String getDetailMessage() {
			return detailMessage;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#getDetailMessageHistory()
		 */
		public String[] getDetailMessageHistory() {
			return detailMessageHistory;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#getErrorMessage()
		 */
		public String getErrorMessage() {
			return errorMessage;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#getName()
		 */
		public String getName() {
			return name;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#getImage()
		 */
		public Image getImage() {
			return image;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#getObjectData()
		 */
		public Object getObjectData() {
			return objectData;
		}

		/* (non-Javadoc)
		 * @see org.gudy.azureus2.ui.swt.progress.IProgressReport#getREPORT_TYPE()
		 */
		public int getReportType() {
			return REPORT_TYPE;
		}

		
		
	}

}