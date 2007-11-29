package org.gudy.azureus2.ui.swt.progress;

import java.util.*;

/**
 * A convenience Stack for tracking <code>ProgressReporter</code>s
 * <p>When a reporter is pushed onto the stack we remove any other occurrences of the same reporter so
 * that there is at most one instance of a particular reporter in the stack at any time</p>
 * 
 * @author knguyen
 *
 */
class ProgressReporterStack
{
	private Stack reporterStack = new Stack();

	/**
	 * Pushes the given reporter on top of the stack; additionally remove any next occurrence of the reporter.
	 * @param reporter
	 */
	public void push(IProgressReporter reporter) {
		if (null == reporter) {
			return;
		}
		synchronized (reporterStack) {

			/*
			 * Remove the reporter from the list if it's in there already
			 */
			if (true == reporterStack.contains(reporter)) {
				reporterStack.remove(reporter);
			}

			reporterStack.add(reporter);
		}
	}

	/**
	 * Returns the reporter at the top of the stack
	 * @return
	 */
	public IProgressReporter peek() {
		synchronized (reporterStack) {
			if (false == reporterStack.isEmpty()) {
				return (IProgressReporter) reporterStack.peek();
			}
			return null;

		}
	}

	/**
	 * Remove the given <code>ProgressReporter</code>;
	 * @return <code>true</code> if the given reporter is found; otherwise <code>false</code>
	 */
	public boolean remove(IProgressReporter reporter) {
		synchronized (reporterStack) {
			if (null != reporter && true == reporterStack.contains(reporter)) {
				return reporterStack.remove(reporter);
			}
			return false;
		}
	}

	/**
	 * Returns whether or not the given <code>IProgressReporter</code> is already in the stack
	 * @param reporter
	 * @return
	 */
	public boolean contains(IProgressReporter reporter) {
		return reporterStack.contains(reporter);
	}

	/**
	 * Remove and return the reporter at the top of the stack
	 * @return
	 */
	public IProgressReporter pop() {
		synchronized (reporterStack) {
			if (false == reporterStack.isEmpty()) {
				return (IProgressReporter) reporterStack.pop();
			}
			return null;
		}
	}

	/**
	 * Trim the list by removing all inactive reporters
	 */
	public void trim() {
		synchronized (reporterStack) {
			for (Iterator iterator = reporterStack.iterator(); iterator.hasNext();) {
				IProgressReporter reporter = ((IProgressReporter) iterator.next());
				if (false == reporter.getProgressReport().isActive()) {
					iterator.remove();
				}
			}
		}
	}

	/**
	 * Returns a list of reporters; this list can safely be manipulated because it is not directly referencing the internal list
	 * @param onlyActive <code>true</code> to return only reporters that are still active, <code>false</code> to return all reporters
	 * @return <code>List</code> 
	 */
	public List getReporters(boolean onlyActive) {
		synchronized (reporterStack) {
			List reporters = new ArrayList();
			for (Iterator iterator = reporterStack.iterator(); iterator.hasNext();) {
				IProgressReporter reporter = ((IProgressReporter) iterator.next());
				if (true == onlyActive) {
					if (true == reporter.getProgressReport().isActive()) {
						reporters.add(reporter);
					}
				} else {
					reporters.add(reporter);
				}
			}
			return reporters;
		}

	}

	public int size() {
		return reporterStack.size();
	}

	/**
	 * Returns the number of reporters in the stack that are still active
	 * @return
	 */
	public int getActiveCount() {
		synchronized (reporterStack) {
			int activeReporters = 0;
			for (Iterator iterator = reporterStack.iterator(); iterator.hasNext();) {
				IProgressReporter reporter = ((IProgressReporter) iterator.next());
				if (true == reporter.getProgressReport().isActive()) {
					activeReporters++;
				}
			}
			return activeReporters;
		}
	}

	/**
	 * Returns the number of reporters in the stack that are in error state  
	 * @return 
	 */
	public int getErrorCount() {
		synchronized (reporterStack) {
			int reportersInErrorState = 0;
			for (Iterator iterator = reporterStack.iterator(); iterator.hasNext();) {
				IProgressReporter reporter = ((IProgressReporter) iterator.next());
				if (true == reporter.getProgressReport().isInErrorState()) {
					reportersInErrorState++;
				}
			}
			return reportersInErrorState;
		}
	}

	/**
	 * A convenience method for quickly determining whether more than one reporter is still active.
	 * This method can be much quicker than calling {@link #getActiveCount()} and inspecting the returned value
	 * if the number of reporters is high since we may not have to go through the entire list before getting the result
	 * @return <code>true</code> if there are at least 2 active reporters; <code>false</code> otherwise
	 */
	public boolean hasMultipleActive() {
		synchronized (reporterStack) {
			int activeReporters = 0;
			for (Iterator iterator = reporterStack.iterator(); iterator.hasNext();) {
				IProgressReporter reporter = (IProgressReporter) iterator.next();
				if (true == reporter.getProgressReport().isActive()) {
					activeReporters++;
				}
				if (activeReporters > 1) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Get the next active reporter.
	 * <p><b>NOTE: </b> this is different from calling {@link #peek()} since the next active reporter may not be at the top of the stack</p>
	 * @return ProgressReporter the next reporter on the stack that is still active; <code>null</code> if none are active or none are found
	 */
	public IProgressReporter getNextActiveReporter() {
		synchronized (reporterStack) {
			for (Iterator iterator = reporterStack.iterator(); iterator.hasNext();) {
				IProgressReporter reporter = (IProgressReporter) iterator.next();
				if (true == reporter.getProgressReport().isActive()) {
					return reporter;
				}
			}
		}
		return null;
	}
}