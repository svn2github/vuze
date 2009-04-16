package org.gudy.azureus2.ui.swt.shells;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.AzureusCoreFactory;

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

		Shell activeShell = Display.getDefault().getActiveShell();
		if (activeShell == null) {
			activeShell = Utils.findAnyShell();
		}
		shell = new Shell(activeShell, SWT.TITLE | SWT.BORDER
				| SWT.APPLICATION_MODAL);
		shell.setText("Please Wait");
		FillLayout fillLayout = new FillLayout();
		fillLayout.marginHeight = 5;
		fillLayout.marginWidth = 5;
		shell.setLayout(fillLayout);

		Label label = new Label(shell, SWT.NONE);
		label.setText("Your operation will run momentarily\nOne day I'll add the task name that required the core, and a cancel button,\nand i18n and an animated frog hopping to the beat of a PoppinS song");

		shell.pack();
		//shell.setSize(300,50);
		Utils.centreWindow(shell);
		shell.open();
	}
}
