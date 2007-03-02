/*
 * Created on Jun 15, 2006 12:29:32 PM
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package com.aelitis.azureus.ui.swt.utils;

import java.util.ArrayList;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;

/**
 * @author TuxPaper
 *
 */
public class UIUpdaterImpl
	extends AEThread
	implements ParameterListener, UIUpdater
{
	private static final LogIDs LOGID = LogIDs.UI3;

	private static String CFG_REFRESH_INTERVAL = "GUI Refresh";

	private int iWaitTime;

	private boolean finished = false;

	private boolean refreshed = true;

	private ArrayList updateables = new ArrayList();

	private AEMonitor updateables_mon = new AEMonitor("updateables");

	public UIUpdaterImpl() {
		super("UI Updater");
		setDaemon(true);

		COConfigurationManager.addAndFireParameterListener(CFG_REFRESH_INTERVAL,
				this);
	}

	// @see org.gudy.azureus2.core3.util.AEThread#runSupport()
	public void runSupport() {
		while (!finished) {
			if (refreshed) {
				refreshed = false;
				if (!Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						try {
							update();
						} catch (Exception e) {
							Logger.log(new LogEvent(LOGID,
									"Error while trying to update GUI", e));
						}

						refreshed = true;
					}
				})) {
					refreshed = true;
				}
			}

			try {
				Thread.sleep(iWaitTime);
			} catch (Exception e) {
				Debug.printStackTrace(e);
			}
		}
	}

	// @see org.gudy.azureus2.core3.config.ParameterListener#parameterChanged(java.lang.String)
	public void parameterChanged(String parameterName) {
		iWaitTime = COConfigurationManager.getIntParameter(CFG_REFRESH_INTERVAL);
	}

	// @see com.aelitis.azureus.ui.swt.utils.UIUpdater#addUpdater(com.aelitis.azureus.ui.swt.utils.UIUpdatable)
	public void addUpdater(UIUpdatable updateable) {
		updateables_mon.enter();
		try {
			if (!updateables.contains(updateable)) {
				updateables.add(updateable);
			}
		} finally {
			updateables_mon.exit();
		}
	}

	// @see com.aelitis.azureus.ui.swt.utils.UIUpdater#removeUpdater(com.aelitis.azureus.ui.swt.utils.UIUpdatable)
	public void removeUpdater(UIUpdatable updateable) {
		updateables_mon.enter();
		try {
			updateables.remove(updateable);
		} finally {
			updateables_mon.exit();
		}
	}

	// @see com.aelitis.azureus.ui.swt.utils.UIUpdater#stopIt()
	public void stopIt() {
		finished = true;
		COConfigurationManager.removeParameterListener(CFG_REFRESH_INTERVAL, this);
	}

	private void update() {
		Object[] updateablesArray = updateables.toArray();
		for (int i = 0; i < updateablesArray.length; i++) {
			if (!(updateablesArray[i] instanceof UIUpdatable)) {
				continue;
			}

			if (SWTThread.getInstance().getDisplay().isDisposed()) {
				return;
			}

			UIUpdatable updateable = (UIUpdatable) updateablesArray[i];
			try {
				updateable.updateUI();
			} catch (Exception e) {
				Logger.log(new LogEvent(LOGID, "Error while trying to update GUI "
						+ updateable.getUpdateUIName(), e));
			}
		}
	}
}
