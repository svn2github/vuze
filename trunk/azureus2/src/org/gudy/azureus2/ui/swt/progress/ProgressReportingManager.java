package org.gudy.azureus2.ui.swt.progress;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.gudy.azureus2.ui.swt.mainwindow.MainStatusBar;
import org.gudy.azureus2.ui.swt.progress.ProgressReporter.ProgressReport;

/**
 * A manager that aggregates and forward progress information for long running operations
 * <p> This is a non-intrusive implementation, such that, it does not directly manage any of the process; it simply receives and forwards information</p>
 * <p> The primary user of this class is the {@link MainStatusBar} where it is used to display progress information</p>
 * @author knguyen
 *
 */
public class ProgressReportingManager
{

	private static ProgressReportingManager INSTANCE = null;

	/**
	 * A custom stack to keep track of <code>ProgressReporter</code>
	 */
	private ProgressReporterStack progressReporters = new ProgressReporterStack();

	/**
	 * Keeps count of all <code>ProgressReporter</code> created since this session started;
	 * is used as unique ID and hashCode for each instance of <code>ProgressReporter</code>
	 * 
	 */
	private int reporterCounter = Integer.MIN_VALUE;

	public static final int COUNT_ALL = 0;

	public static final int COUNT_ACTIVE = 1;

	public static final int COUNT_ERROR = 2;

	/**
	 * A <code>List</code> of <code>IProgressReportingListener</code> 
	 */
	private List listeners = new ArrayList();

	private ProgressReportingManager() {
	}

	public static final synchronized ProgressReportingManager getInstance() {
		if (null == INSTANCE) {
			INSTANCE = new ProgressReportingManager();
		}
		return INSTANCE;
	}

	/**
	 * Returns the number of reporters that have sent any event to this manager and have not been removed
	 * <ul>
	 * <li><code>COUNT_ERROR</code> - count all reporters in error state</li>
	 * <li><code>COUNT_ACTIVE</code> - count all reporters that are still active</li>
	 * <li><code>COUNT_ALL</code> - count all reporters</li>
	 * </ul>
	 * @param whatToCount one of the above constants; will default to <code>COUNT_ALL</code> if the parameter is unrecognized
	 * @return
	 */
	public int getReporterCount(int whatToCount) {
		if (whatToCount == COUNT_ERROR) {
			return progressReporters.getErrorCount();
		}
		if (whatToCount == COUNT_ACTIVE) {
			return progressReporters.getActiveCount();
		}

		return progressReporters.size();
	}

	/**
	 * A convenience method for quickly determining whether more than one reporter is still active.
	 * This method can be much quicker than calling {@link #getReporterCount()} and inspecting the returned value
	 * if the number of reporters is high since we may not have to go through the entire list before getting the result
	 * 
	 * @return <code>true</code> if there are at least 2 active reporters; <code>false</code> otherwise
	 */
	public boolean hasMultipleActive() {
		return progressReporters.hasMultipleActive();
	}


	/**
	 * Returns the previous active reporter
	 * @return the previous reporter that is still active; <code>null</code> if none are active or no reporters are found
	 */
	public IProgressReporter getPreviousActiveReporter() {
		return progressReporters.getPreviousActiveReporter();
	}

	/**
	 * Returns the current reporter, in other word, the last reporter to have reported anything
	 * @return the last reporter; <code>null</code> if none are found
	 */
	public IProgressReporter getCurrentReporter() {
		return progressReporters.peek();
	}

	/**
	 * Returns a modifiable list of <code>ProgressReporter</code>s; manipulating this list has no
	 * effect on the internal list of reporters maintained by this manager
	 * 
	 * @param onlyActive <code>true</code> to filter the list to only include those reporters that are still active
	 * @return a sorted List of <code>ProgressReporter</code> where the oldest reporter would be at position 0
	 */
	public List getReporters(boolean onlyActive) {
		List reporters = progressReporters.getReporters(onlyActive);
		Collections.sort(reporters);
		return reporters;
	}

	/**
	 * 
	 * Returns a modifiable array of <code>ProgressReporter</code>s; manipulating this array has no
	 * effect on the internal list of reporters maintained by this manager
	 * @param onlyActive <code>true</code> to filter the array to only include those reporters that are still active
	 * @return a sorted array of <code>ProgressReporter</code> where the oldest reporter would be at position 0 
	 */
	public IProgressReporter[] getReportersArray(boolean onlyActive) {
		List rpList = progressReporters.getReporters(onlyActive);
		IProgressReporter[] array = (IProgressReporter[]) rpList.toArray(new IProgressReporter[rpList.size()]);
		Arrays.sort(array);
		return array;
	}

	/**
	 * Removes the given <code>ProgressReporter</code> from this manager.  This has the effect that
	 * any subsequent event reported by the same reporter will not be captured nor forwarded by this manager
	 * @param reporter
	 * @return
	 */
	protected boolean remove(IProgressReporter reporter) {
		boolean value = progressReporters.remove(reporter);
		notifyListeners(null);
		return value;
	}

	/**
	 * 
	 * @param listener
	 */
	public void addListener(IProgressReportingListener listener) {
		if (null != listener) {
			synchronized (listeners) {
				listeners.add(listener);
			}
		}
	}

	/**
	 * 
	 * @param listener
	 */
	public void removeListener(IProgressReportingListener listener) {
		if (null != listener) {
			synchronized (listeners) {
				listeners.remove(listener);
			}
		}
	}

	/**
	 * Notifies listeners that the given <code>ProgressReporter</code> has been modified
	 * @param reporter
	 */
	private void notifyListeners(IProgressReporter reporter) {
		synchronized (listeners) {
			for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
				IProgressReportingListener listener = (IProgressReportingListener) iterator.next();
				if (null != listener) {
					listener.reporting(reporter);
				}
			}
		}
	}

	/**
	 * Push this reporter on top of the stack, and notifies any listeners that a state change has occurred
	 * @param reporter
	 */
	protected synchronized void notifyManager(IProgressReporter reporter) {

		/*
		 * If this has been marked as disposed then we remove it 
		 */
		if (true == reporter.getProgressReport().isDisposed) {
			progressReporters.remove(reporter);
		} else {
			progressReporters.push(reporter);
		}

		/*
		 * Notify those listening to the manager itself
		 */
		notifyListeners(reporter);

	}

	/**
	 * A convenience <code>ArrayList</code> based stack for tracking <code>ProgressReporter</code>s
	 * <p>The <code>ArrayList</code> could be replaced if performance becomes an issue</p>
	 * <p>When a reporter is pushed onto the stack we remove any other occurrences of the same reporter so
	 * that there is at most one instance of a particular reporter in the stack at any time</p>
	 * <p>Most iteration through the internal ArrayList should be done in reverse order because active reporters
	 * tend to be closer to the end of the list than the beginning; additionally the {@link #trim(boolean)} method
	 * may be called occasionally to compact the list to increase lookup performance and memory footprint. </p>
	 * 
	 * @author knguyen
	 *
	 */
	private class ProgressReporterStack
	{
		private ArrayList reporterList;

		public ProgressReporterStack() {
			reporterList = new ArrayList();
		}

		public ProgressReporterStack(int stackInitialSize) {
			reporterList = new ArrayList(stackInitialSize);
		}

		/**
		 * Pushes the given reporter on top of the stack; additionally remove any previous occurrence of the reporter.
		 * @param reporter
		 */
		public void push(IProgressReporter reporter) {
			if (null == reporter) {
				return;
			}
			synchronized (reporterList) {

				/*
				 * Remove the reporter from the list if it's in there already
				 */
				if (true == reporterList.contains(reporter)) {
					reporterList.remove(reporter);
				}

				reporterList.add(reporter);
			}
		}

		/**
		 * Returns the reporter at the top of the stack
		 * @return
		 */
		public IProgressReporter peek() {
			synchronized (reporterList) {
				if (true == reporterList.isEmpty()) {
					return null;
				}

				return (IProgressReporter) reporterList.get(reporterList.size() - 1);
			}
		}

		/**
		 * Remove the given <code>ProgressReporter</code>;
		 * @return <code>true</code> if the given reporter is not found; otherwise <code>false</code>
		 */
		public boolean remove(IProgressReporter reporter) {
			synchronized (reporterList) {
				return reporterList.remove(reporter);
			}
		}

		/**
		 * Remove and return the reporter at the top of the stack
		 * @return
		 */
		public IProgressReporter pop() {
			synchronized (reporterList) {
				if (false == reporterList.isEmpty()) {
					return (IProgressReporter) reporterList.remove(reporterList.size() - 1);
				}
				return null;
			}
		}

		/**
		 * Trims the list by removing all inactive reporters
		 */
		public void trim() {
			/*
			 * Locking the list since we're iterating through it
			 */
			synchronized (reporterList) {
				for (Iterator iterator = reporterList.iterator(); iterator.hasNext();) {
					IProgressReporter reporter = ((IProgressReporter) iterator.next());
					ProgressReport report = reporter.getProgressReport();
					if (true == report.isActive) {
						reporter.dispose();
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
			/*
			 * Locking the list since we're iterating through it
			 */
			synchronized (reporterList) {
				List reporters = new ArrayList();
				for (ListIterator iterator = reporterList.listIterator(reporterList.size()); iterator.hasPrevious();) {
					IProgressReporter reporter = ((IProgressReporter) iterator.previous());
					ProgressReport report = reporter.getProgressReport();
					if (true == onlyActive) {
						if (true == report.isActive) {
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
			return reporterList.size();
		}

		/**
		 * Returns the number of reporters in the stack that are still active
		 * @return
		 */
		public int getActiveCount() {
			/*
			 * Locking the list since we're iterating through it
			 */
			synchronized (reporterList) {
				int activeReporters = 0;
				for (ListIterator iterator = reporterList.listIterator(reporterList.size()); iterator.hasPrevious();) {
					IProgressReporter reporter = ((IProgressReporter) iterator.previous());
					ProgressReport report = reporter.getProgressReport();
					if (true == report.isActive) {
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
			/*
			 * Locking the list since we're iterating through it
			 */
			synchronized (reporterList) {
				int reportersInErrorState = 0;
				for (ListIterator iterator = reporterList.listIterator(reporterList.size()); iterator.hasPrevious();) {
					IProgressReporter reporter = ((IProgressReporter) iterator.previous());
					ProgressReport report = reporter.getProgressReport();
					if (true == report.isInErrorState) {
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

			/*
			 * Locking the list since we're iterating through it
			 */
			synchronized (reporterList) {
				int activeReporters = 0;
				for (ListIterator iterator = reporterList.listIterator(reporterList.size()); iterator.hasPrevious();) {
					IProgressReporter reporter = (IProgressReporter) iterator.previous();
					ProgressReport report = reporter.getProgressReport();
					if (true == report.isActive) {
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
		 * Get the previous active reporter.
		 * <p><b>NOTE: </b> this is different from calling {@link #peek()} since the previous active reporter may not be at the top of the stack</p>
		 * @return ProgressReporter the previous reporter on the stack that is still active; <code>null</code> if none are active or none are found
		 */
		public IProgressReporter getPreviousActiveReporter() {
			/*
			 * Locking the list since we're iterating through it
			 */
			synchronized (reporterList) {
				for (ListIterator iterator = reporterList.listIterator(reporterList.size()); iterator.hasPrevious();) {
					IProgressReporter reporter = (IProgressReporter) iterator.previous();
					ProgressReport report = reporter.getProgressReport();
					if (true == report.isActive) {
						return reporter;
					}
				}
			}
			return null;
		}
	}

	/**
	 * Returns the next available ID that can be assigned to a {@link ProgressReporter}
	 * @return int the next available ID
	 */
	protected synchronized final int getNextAvailableID() {

		/*
		 * This is a simple brute forced way to generate unique ID's which can also be use as a unique hashcode
		 * without having to directly track all previously created/disposed ProgressReporters
		 * 
		 * This is synchronized to ensure that the incrementing of reporterCounter is consistent
		 * so that each ProgressReporter is guaranteed to have a unique ID (which is also used as its hashCode)
		 * 
		 * WARNING: This method is mainly intended to be used by the constructors of ProgressReporter and should not be called
		 * from anywhere else (unless you really know what you're doing); unintended repeated call to this method can exhaust
		 * the limit of the integer.  This counter starts from Integer.MIN_VALUE
		 */
		return reporterCounter++;
	}

}
