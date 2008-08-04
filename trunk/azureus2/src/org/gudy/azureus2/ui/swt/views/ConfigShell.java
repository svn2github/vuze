package org.gudy.azureus2.ui.swt.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

/**
 * A shell containing the <code>ConfigView</code>
 * This is used to pop-up the configs in a Shell as opposed to hosting it in the application
 * This class is used to ensure that only one shell is opened at any time.
 * @author khai
 *
 */
public class ConfigShell
{

	private static ConfigShell instance;

	private Shell shell;

	public static ConfigShell getInstance() {
		if (null == instance) {
			instance = new ConfigShell();
		}
		return instance;
	}

	private ConfigShell() {
	}

	/**
	 * Opens the <code>ConfigView</code> inside a pop-up <code>Shell</code>.
	 * If the Shell is opened already then just force it active
	 * @param width
	 * @param height
	 */
	public void open() {
		if (null != shell && false == shell.isDisposed()) {
			if (true == shell.getMinimized()) {
				shell.setMinimized(false);
			}
			shell.forceActive();
			shell.forceFocus();
		} else {
			shell = ShellFactory.createShell(SWT.SHELL_TRIM);
			shell.setLayout(new GridLayout());
			shell.setText(MessageText.getString(MessageText.resolveLocalizationKey("ConfigView.title.full")));
			Utils.setShellIcon(shell);
			final ConfigView cView = new ConfigView(AzureusCoreFactory.getSingleton());
			cView.initialize(shell);

			/*
			 * Set default size and centers the shell if it's configuration does not exist yet
			 */
			if (null == COConfigurationManager.getStringParameter(
					"options.rectangle", null)) {
				Rectangle shellBounds = shell.getMonitor().getBounds();
				Point size = new Point(shellBounds.width * 10 / 11,
						shellBounds.height * 10 / 11);
				if (size.x > 1400) {
					size.x = 1400;
				}
				if (size.y > 700) {
					size.y = 700;
				}
				shell.setSize(size);
				Utils.centerWindowRelativeTo(shell, getMainShell());
			}

			Utils.linkShellMetricsToConfig(shell, "options");

			/*
			 * Auto-save when the shell closes
			 */
			shell.addListener(SWT.Close, new Listener() {
				public void handleEvent(Event event) {
					cView.save();
					event.doit = true;
				}
			});
			
			shell.addTraverseListener(new TraverseListener() {
				public void keyTraversed(TraverseEvent e) {
					if (e.detail == SWT.TRAVERSE_ESCAPE) {
						shell.dispose();
					}
				}
			});

			shell.open();
		}
	}

	public void close() {
		if (null != shell && false == shell.isDisposed()) {
			shell.close();
		}
	}

	private Shell getMainShell() {
		UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
		if (null != uiFunctions) {
			return uiFunctions.getMainShell();
		}

		throw new IllegalStateException(
				"No instance of UIFunctionsSWT found; the UIFunctionsManager might not have been initialized properly");

	}
}
