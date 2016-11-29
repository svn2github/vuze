/*
 * Created on Jun 15, 2006 12:29:32 PM
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
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
 */
package com.aelitis.azureus.ui.swt.uiupdater;

import java.util.*;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.IMainStatusBar;

import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.common.updater.UIUpdatableAlways;
import com.aelitis.azureus.ui.common.updater.UIUpdater;
import com.aelitis.azureus.ui.common.updater.UIUpdater.UIUpdaterListener;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

/**
 * @author TuxPaper
 *
 */
public class UIUpdaterSWT
	extends AEThread2
	implements ParameterListener, UIUpdater
{
	private static final LogIDs LOGID = LogIDs.UI3;

	private static final String CFG_REFRESH_INTERVAL = "GUI Refresh";

	private static final String CFG_REFRESH_INACTIVE_FACTOR = "Refresh When Inactive";

	/** Calculate timer statistics for GUI update */
	private static final boolean DEBUG_TIMER = Constants.isCVSVersion();
	
	private static final boolean DEBUG_UPDATEABLES = Constants.IS_CVS_VERSION;

	private static UIUpdater updater = null;

	private int waitTimeMS;

	private boolean finished = false;

	private CopyOnWriteList<UIUpdatable> updateables = new CopyOnWriteList<UIUpdatable>();
	
	private Map<UIUpdatable, String> debug_Updateables; 

	private CopyOnWriteList<UIUpdatable> alwaysUpdateables = new CopyOnWriteList<UIUpdatable>();

	private AEMonitor updateables_mon = new AEMonitor("updateables");

	private int inactiveFactor;

	private int inactiveTicks;

	Map averageTimes = DEBUG_TIMER ? new HashMap() : null;

	private int	update_count = 0;
	private CopyOnWriteList<UIUpdaterListener>		listeners = new CopyOnWriteList<UIUpdaterListener>();
	
	public static UIUpdater getInstance() {
		synchronized( UIUpdaterSWT.class ){
			if (updater == null) {
				updater = new UIUpdaterSWT();
				updater.start();
			}
	
			return updater;
		}
	}

	public UIUpdaterSWT() {
		super("UI Updater", true);

		if (DEBUG_UPDATEABLES) {
			debug_Updateables = new HashMap<UIUpdatable, String>();
		}
		COConfigurationManager.addAndFireParameterListeners(new String[] {
			CFG_REFRESH_INTERVAL,
			CFG_REFRESH_INACTIVE_FACTOR
		}, this);
	}

	// @see org.gudy.azureus2.core3.util.AEThread2#run()
	public void run() {
		final AESemaphore	sem = new AESemaphore( "UI Updater" );
		
		while (!finished) {
			Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						try {
							Display display = Utils.getDisplay();
							if (display == null) {
								return;
							}

							if (display.getActiveShell() == null) {
								Shell[] shells = display.getShells();
								boolean noneVisible = true;
								for (int i = 0; i < shells.length; i++) {
									if (shells[i].isVisible() && !shells[i].getMinimized()) {
										noneVisible = false;
										break;
									}
								}
								if (noneVisible) {
									//System.out.println("nothing visible!");
									if (alwaysUpdateables.size() > 0) {
										update(alwaysUpdateables,false);
									}
									return;
								}

								// inactive used to mean "active shell is not mainwindow", but
								// now that this is more generic (can be used with any shell),
								// inactive means "active shell is not one of our app's"
								if (inactiveTicks++ % inactiveFactor != 0) {
									return;
								}
							}

							update(updateables,true);
						} catch (Exception e) {
							Logger.log(new LogEvent(LOGID,
									"Error while trying to update GUI", e));
						} finally {
							try{
								for ( UIUpdaterListener l: listeners ){
	
									try{
										l.updateComplete( ++update_count );
									}catch( Throwable e ){
										Debug.out( e );
									}
								}
							}finally{
								sem.release();
							}
						}
					}
				});
			
			long start = SystemTime.getHighPrecisionCounter();
			
			sem.reserve();
			
			long elapsed = SystemTime.getHighPrecisionCounter() - start;
			
			long	to_sleep = waitTimeMS - (elapsed/1000000);
			
			if ( to_sleep < 10 ){
				to_sleep = 10;
			}else if ( to_sleep > 25000 ){
				to_sleep = 25000;
			}
			try {
				//System.out.println( "sleep=" + to_sleep );
				Thread.sleep(to_sleep);
			} catch (Exception e) {
				Debug.printStackTrace(e);
			}
		}
	}

	// @see org.gudy.azureus2.core3.config.ParameterListener#parameterChanged(java.lang.String)
	public void parameterChanged(String parameterName) {
		waitTimeMS = COConfigurationManager.getIntParameter(CFG_REFRESH_INTERVAL);
		inactiveFactor = COConfigurationManager.getIntParameter(CFG_REFRESH_INACTIVE_FACTOR);
		if (inactiveFactor == 0) {
			inactiveFactor = 1;
		}
	}

	// @see com.aelitis.azureus.ui.swt.utils.UIUpdater#addUpdater(com.aelitis.azureus.ui.swt.utils.UIUpdatable)
	public void addUpdater(UIUpdatable updateable) {
		updateables_mon.enter();
		try {
			if (updateable instanceof UIUpdatableAlways) {
				if (!alwaysUpdateables.contains(updateable)) {
					alwaysUpdateables.add(updateable);
				}
			}

			if (!updateables.contains(updateable)) {
				updateables.add(updateable);
				if (DEBUG_UPDATEABLES) {
					debug_Updateables.put(updateable, Debug.getCompressedStackTrace() + "\n");
				}
			} else {
				if (DEBUG_UPDATEABLES) {
					System.out.println("WARNING: already added UIUpdatable " + updateable + "\n\t" + debug_Updateables.get(updateable) + "\t" + Debug.getCompressedStackTrace());
				} else {
					System.out.println("WARNING: already added UIUpdatable " + updateable);
				}
			}
		} finally {
			updateables_mon.exit();
		}
	}

	public boolean 
	isAdded(
		UIUpdatable updateable) 
	{
		updateables_mon.enter();
		try {
			return( updateables.contains(updateable));
		} finally {
			updateables_mon.exit();
		}
	}
	
	// @see com.aelitis.azureus.ui.swt.utils.UIUpdater#removeUpdater(com.aelitis.azureus.ui.swt.utils.UIUpdatable)
	public void removeUpdater(UIUpdatable updateable) {
		updateables_mon.enter();
		try {
			updateables.remove(updateable);
			if (updateable instanceof UIUpdatableAlways) {
				alwaysUpdateables.remove(updateable);
			}
		} finally {
			updateables_mon.exit();
		}
	}

	// @see com.aelitis.azureus.ui.swt.utils.UIUpdater#stopIt()
	public void stopIt() {
		finished = true;
		COConfigurationManager.removeParameterListener(CFG_REFRESH_INTERVAL, this);
	}

	private void update(CopyOnWriteList<UIUpdatable> updateables, boolean is_visible ) {
		long start = 0;
		Map mapTimeMap = DEBUG_TIMER ? new HashMap() : null;

		Display display = Utils.getDisplay();
		if (display == null || display.isDisposed()) {
			return;
		}

		for ( UIUpdatable updateable: updateables ){
			try {
				if (DEBUG_TIMER) {
					start = SystemTime.getCurrentTime();
				}
				if ( updateable instanceof UIUpdatableAlways ){
					((UIUpdatableAlways)updateable).updateUI(is_visible);
				}else{
					updateable.updateUI();
				}
				
				if (DEBUG_TIMER) {
					long diff = SystemTime.getCurrentTime() - start;
					if (diff > 0) {
						mapTimeMap.put(updateable, new Long(diff));
					}
				}
			} catch (Throwable t) {
				Logger.log(new LogEvent(LOGID,
						"Error while trying to update UI Element "
								+ updateable.getUpdateUIName(), t));
			}
		}
		if (DEBUG_TIMER) {
			makeDebugToolTip(mapTimeMap);
		}
	}

	private void makeDebugToolTip(Map timeMap) {
		final int IDX_AVG = 0;
		final int IDX_SIZE = 1;
		final int IDX_MAX = 2;
		final int IDX_LAST = 3;
		final int IDX_TIME = 4;

		long ttl = 0;
		for (Iterator iter = timeMap.keySet().iterator(); iter.hasNext();) {
			Object key = iter.next();

			if (!averageTimes.containsKey(key))
				averageTimes.put(key, new Object[] {
					new Long(0),
					new Long(0),
					new Long(0),
					new Long(0),
					new Long(System.currentTimeMillis())
				});

			Object[] average = (Object[]) averageTimes.get(key);

			long diff = ((Long) timeMap.get(key)).longValue();
			if (diff > 0) {
				long count = ((Long) average[IDX_SIZE]).longValue();
				// Limit to 20.  Gives slightly scewed averages, but doesn't
				// require storing all 20 values and averaging them each time
				if (count >= 20)
					count = 19;
				long lNewAverage = ((((Long) average[IDX_AVG]).longValue() * count) + diff)
						/ (count + 1);
				average[IDX_AVG] = new Long(lNewAverage);
				average[IDX_SIZE] = new Long(count + 1);
				if (diff > ((Long) average[IDX_MAX]).longValue())
					average[IDX_MAX] = new Long(diff);
				average[IDX_LAST] = new Long(diff);
				average[IDX_TIME] = new Long(System.currentTimeMillis());
			} else {
				average[IDX_LAST] = new Long(diff);
			}
			ttl += diff;
			averageTimes.put(key, average);
		}
		//System.out.println(SystemTime.getCurrentTime() + "] Refresh " + ttl + "ms");

		UIFunctionsSWT uiFunctionsSWT = UIFunctionsManagerSWT.getUIFunctionsSWT();
		IMainStatusBar mainStatusBar = uiFunctionsSWT == null ? null : uiFunctionsSWT.getMainStatusBar();
		if (mainStatusBar != null && mainStatusBar.isMouseOver()) {
			StringBuilder sb = new StringBuilder();
			for (Iterator iter = averageTimes.keySet().iterator(); iter.hasNext();) {
				Object key = iter.next();
				Object[] average = (Object[]) averageTimes.get(key);

				long lLastUpdated = ((Long) average[IDX_TIME]).longValue();
				if (System.currentTimeMillis() - lLastUpdated > 10000) {
					iter.remove();
					continue;
				}

				long lTime = ((Long) average[IDX_AVG]).longValue();
				if (lTime > 0) {
					if (sb.length() > 0)
						sb.append("\n");
					sb.append(lTime * 100 / waitTimeMS);
					sb.append("% ");
					sb.append(lTime).append("ms avg: ");
					sb.append("[").append(((UIUpdatable) key).getUpdateUIName()).append("]");
					sb.append(average[IDX_SIZE]).append(" samples");
					sb.append("; max:").append(average[IDX_MAX]);
					sb.append("; last:").append(average[IDX_LAST]);
				}
			}

			mainStatusBar.setDebugInfo(sb.toString());
		}
	}
	
	public void
	addListener(
		UIUpdaterListener		listener )
	{
		listeners.add( listener );
	}
	
	public void
	removeListener(
		UIUpdaterListener		listener )
	{
		listeners.remove( listener );
	}
}
