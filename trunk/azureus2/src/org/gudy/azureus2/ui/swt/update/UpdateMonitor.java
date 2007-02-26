/*
 * Created on 7 mai 2004
 * Created by Olivier Chalouhi
 * 
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.update;

import org.eclipse.swt.SWT;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.update.CoreUpdateChecker;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.update.*;

/**
 * @author Olivier Chalouhi
 *
 */
public class UpdateMonitor
	implements UpdateCheckInstanceListener
{
	private static final LogIDs LOGID = LogIDs.GUI;

	public static final long AUTO_UPDATE_CHECK_PERIOD = 23 * 60 * 60 * 1000; // 23 hours

	private static final String MSG_PREFIX = "UpdateMonitor.messagebox.";

	protected static UpdateMonitor singleton;

	protected static AEMonitor class_mon = new AEMonitor("UpdateMonitor:class");

	public static UpdateMonitor getSingleton(AzureusCore core) {
		try {
			class_mon.enter();

			if (singleton == null) {

				singleton = new UpdateMonitor(core);
			}

			return (singleton);

		} finally {

			class_mon.exit();
		}
	}

	protected AzureusCore azCore;

	protected UpdateWindow current_update_window;

	protected UpdateCheckInstance current_update_instance;

	protected UpdateMonitor(AzureusCore _azureus_core) {
		azCore = _azureus_core;

		PluginInterface defPI = azCore.getPluginManager().getDefaultPluginInterface();
		UpdateManager um = defPI.getUpdateManager();

		um.addListener(new UpdateManagerListener() {
			public void checkInstanceCreated(UpdateCheckInstance instance) {
				instance.addListener(UpdateMonitor.this);
			}
		});

		um.addVerificationListener(new UpdateManagerVerificationListener() {
			public boolean acceptUnVerifiedUpdate(final Update update) {
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				if (uiFunctions != null) {
					String title = MessageText.getString(MSG_PREFIX
							+ "accept.unverified.title");
					String text = MessageText.getString(MSG_PREFIX
							+ "accept.unverified.text", new String[] {
						update.getName()
					});
					return uiFunctions.promptUser(title, text, new String[] {
						MessageText.getString("Button.yes"),
						MessageText.getString("Button.no")
					}, 1, MSG_PREFIX + "accept.unverified",
							MessageText.getString("MessageBoxWindow.nomoreprompting"), false,
							0) == 0;
				}

				return false;
			}

			public void verificationFailed(final Update update, final Throwable cause) {
				final String cause_str = Debug.getNestedExceptionMessage(cause);
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				if (uiFunctions != null) {
					String title = MessageText.getString(MSG_PREFIX
							+ "verification.failed.title");
					String text = MessageText.getString(MSG_PREFIX
							+ "verification.failed.text", new String[] {
						update.getName(),
						cause_str
					});
					uiFunctions.promptUser(title, text, new String[] {
						MessageText.getString("Button.ok")
					}, 0, null, null, false, 0);
				}
			}
		});

		SimpleTimer.addPeriodicEvent("UpdateMon:autocheck",
				AUTO_UPDATE_CHECK_PERIOD, new TimerEventPerformer() {
					public void perform(TimerEvent ev) {
						performAutoCheck(false);
					}
				});

		// wait a bit before starting check to give rest of AZ time to initialise 
		new DelayedEvent("UpdateMon:wait", 2500, new AERunnable() {
			public void runSupport() {
				performAutoCheck(true);
			}
		});
	}

	protected void performAutoCheck(final boolean start_of_day) {
		boolean check_at_start = false;
		boolean check_periodic = false;
		boolean bOldSWT = SWT.getVersion() < 3139;

		// no update checks for java web start

		if (!SystemProperties.isJavaWebStartInstance()) {

			// force check when SWT is really old
			check_at_start = COConfigurationManager.getBooleanParameter("update.start")
					|| bOldSWT;
			check_periodic = COConfigurationManager.getBooleanParameter("update.periodic");
		}

		// periodic -> check at start as well

		check_at_start = check_at_start || check_periodic;

		if ((check_at_start && start_of_day) || (check_periodic && !start_of_day)) {

			performCheck(bOldSWT, true, null); // this will implicitly do usage stats

		} else {

			new DelayedEvent("UpdateMon:wait2", 5000, new AERunnable() {
				public void runSupport() {
					if (start_of_day) {
						UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
						if (uiFunctions != null) {
							uiFunctions.setStatusText("");
						}
					}

					CoreUpdateChecker.doUsageStats();
				}
			});
		}
	}

	public void performCheck(final boolean bForce, final boolean automatic,
			final UpdateCheckInstanceListener l) {
		if (SystemProperties.isJavaWebStartInstance()) {

			// just in case we get here somehome!
			if (Logger.isEnabled())
				Logger.log(new LogEvent(LOGID,
						"skipping update check as java web start"));

			return;
		}

		// kill any existing update window

		if (current_update_window != null && !current_update_window.isDisposed()) {
			current_update_window.dispose();
		}

		if (current_update_instance != null) {

			current_update_instance.cancel();
		}

		UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
		if (uiFunctions != null) {
			// XXX What kind of format is this!?
			uiFunctions.setStatusText("MainWindow.status.checking ...");
		}

		// take this off this GUI thread in case it blocks for a while

		AEThread t = new AEThread("UpdateMonitor:kickoff") {
			public void runSupport() {
				UpdateManager um = azCore.getPluginManager().getDefaultPluginInterface().getUpdateManager();

				current_update_instance = um.createUpdateCheckInstance(bForce
						? UpdateCheckInstance.UCI_INSTALL : UpdateCheckInstance.UCI_UPDATE,
						"update.instance.update");

				if (!automatic) {

					current_update_instance.setAutomatic(false);
				}

				if (l != null) {
					current_update_instance.addListener(l);
				}
				current_update_instance.start();
			}
		};

		t.setDaemon(true);

		t.start();
	}

	public void complete(UpdateCheckInstance instance) {
		// we can get here for either update actions (triggered above) or for plugin
		// install actions (triggered by the plugin installer)

		boolean update_action = instance.getType() == UpdateCheckInstance.UCI_UPDATE;

		UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
		if (uiFunctions != null) {
			uiFunctions.setStatusText("");
		}

		Update[] us = instance.getUpdates();

		boolean hasDownloads = false;

		// updates with zero-length downloaders exist for admin purposes
		// and shoudn't cause the window to be shown if only they exist

		for (int i = 0; i < us.length; i++) {

			if (us[i].getDownloaders().length > 0) {

				hasDownloads = true;

				break;
			}
		}

		// this controls whether or not the update window is displayed
		// note that we just don't show the window if this is set, we still do the
		// update check (as amongst other things we want ot know the latest
		// version of the core anyway	    

		if (hasDownloads) {

			// don't show another update if one's already there!

			UpdateWindow this_window = null;
			boolean autoDownload = COConfigurationManager.getBooleanParameter("update.autodownload");

			if (update_action) {
				if (!autoDownload
						&& (current_update_window == null || current_update_window.isDisposed())) {

					this_window = current_update_window = new UpdateWindow(azCore,
							instance);
				}
			} else {

				// always show an installer window

				this_window = new UpdateWindow(azCore, instance);
			}

			if (this_window != null) {

				for (int i = 0; i < us.length; i++) {

					if (us[i].getDownloaders().length > 0) {

						this_window.addUpdate(us[i]);
					}
				}

				this_window.updateAdditionComplete();

			} else {
				if (autoDownload) {
					new UpdateAutoDownloader(us, new UpdateAutoDownloader.cbCompletion() {
						public void allUpdatesComplete(boolean requiresRestart) {
							if (requiresRestart) {
								UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
								if (uiFunctions != null) {
									String title = MessageText.getString(MSG_PREFIX
											+ "restart.title");
									String text = MessageText.getString(MSG_PREFIX
											+ "restart.text");
									uiFunctions.bringToFront();
									if (uiFunctions.promptUser(title, text, new String[] {
										MessageText.getString("UpdateWindow.restart"),
										MessageText.getString("UpdateWindow.restartLater")
									}, 0, null, null, false, 180000) == 0) {
										uiFunctions.dispose(true, false);
									}
								}
							}
						}
					});
				} else {
					if (Logger.isEnabled())
						Logger.log(new LogEvent(LOGID, LogEvent.LT_WARNING,
								"UpdateMonitor: user dialog already "
										+ "in progress, updates skipped"));
				}

			}
		} else {
			if (Logger.isEnabled())
				Logger.log(new LogEvent(LOGID, "UpdateMonitor: check instance "
						+ "resulted in no user-actionable updates"));

		}
	}

	public void cancelled(UpdateCheckInstance instance) {
		UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
		if (uiFunctions != null) {
			uiFunctions.setStatusText("");
		}
	}
}
