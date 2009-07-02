package org.gudy.azureus2.ui.swt.shells;

import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;

public class CoreWaiterSWT
{
	public enum TriggerInThread {
		SWT_THREAD, ANY_THREAD, NEW_THREAD
	}

	private static Shell shell;

	public static void waitForCoreRunning(final AzureusCoreRunningListener l) {
		waitForCore(TriggerInThread.SWT_THREAD, l);
	}

	public static void waitForCore(final TriggerInThread triggerInThread,
			final AzureusCoreRunningListener l) {
		if (!AzureusCoreFactory.isCoreRunning()) {
			System.out.println("NOT AVAIL FOR " + Debug.getCompressedStackTrace());
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					showWaitWindow();
				}
			});
		} else {
			System.out.println("NO NEED TO WAIT.. CORE AVAIL! "
					+ Debug.getCompressedStackTrace());
		}

		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(final AzureusCore core) {
				if (triggerInThread == TriggerInThread.ANY_THREAD) {
					l.azureusCoreRunning(core);
				} else if (triggerInThread == TriggerInThread.NEW_THREAD) {
					new AEThread2("CoreWaiterInvoke", true) {
						public void run() {
							l.azureusCoreRunning(core);
						}
					}.start();
				}
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						if (shell != null && !shell.isDisposed()) {
							shell.dispose();
							shell = null;
						} else {
							// cancel (should do a cancel listener trigger?)
							return;
						}

						if (triggerInThread == TriggerInThread.SWT_THREAD) {
							l.azureusCoreRunning(core);
						}
					}
				});
			}
		});
	}

	protected static void showWaitWindow() {
		if (shell != null && !shell.isDisposed()) {
			shell.forceActive();
			return;
		}
		
		shell = UIFunctionsManagerSWT.getUIFunctionsSWT().showCoreWaitDlg();
	}
}
