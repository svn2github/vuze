/**
 * Created on Feb 9, 2009
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package org.gudy.azureus2.ui.swt.donations;

import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.*;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.stats.transfer.StatsFactory;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;

import com.aelitis.azureus.core.AzureusCoreFactory;

/**
 * @author TuxPaper
 * @created Feb 9, 2009
 *
 */
public class DonationWindow
{
	public static boolean DEBUG = System.getProperty("donations.debug", "0").equals(
			"1");

	private static int reAskEveryHours = 96;

	private static int initialAskHours = 48;

	private boolean pageLoadedOk = false;

	private Shell shell;

	private Browser browser;

	public static void checkForDonationPopup() {
		//Check if user has already donated first
		boolean alreadyDonated = COConfigurationManager.getBooleanParameter(
				"donations.donated", false);
		if (alreadyDonated) {
			if (DEBUG) {
				Utils.openMessageBox(null, SWT.OK, "Donations Test",
						"Already Donated! I like you.");
			}
			return;
		}

		long upTime = StatsFactory.getStats().getTotalUpTime();
		int hours = (int) (upTime / (60 * 60)); //secs * mins

		//Ask every DONATIONS_ASK_AFTER hours.
		int nextAsk = COConfigurationManager.getIntParameter(
				"donations.nextAskHours", 0);

		if (nextAsk == 0) {
			// First Time
			COConfigurationManager.setParameter("donations.nextAskHours", hours
					+ initialAskHours);
			COConfigurationManager.save();
			if (DEBUG) {
				Utils.openMessageBox(null, SWT.OK, "Donations Test",
						"Newbie. You're active for " + hours + ".");
			}
			return;
		}

		if (hours < nextAsk) {
			if (DEBUG) {
				Utils.openMessageBox(null, SWT.OK, "Donations Test", "Wait "
						+ (nextAsk - hours) + ".");
			}
			return;
		}

		long minDate = COConfigurationManager.getLongParameter("donations.minDate",
				0);
		if (minDate > 0 && minDate > SystemTime.getCurrentTime()) {
			if (DEBUG) {
				Utils.openMessageBox(null, SWT.OK, "Donation Test", "Wait "
						+ ((SystemTime.getCurrentTime() - minDate) / 1000 / 3600 / 24)
						+ " days");
			}
			return;
		}

		COConfigurationManager.setParameter("donations.nextAskHours", hours
				+ reAskEveryHours);
		COConfigurationManager.save();

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				new DonationWindow().show(false);
			}
		});
	}

	public void show(final boolean showNoLoad) {
		shell = ShellFactory.createShell(Utils.findAnyShell(), SWT.BORDER
				| SWT.APPLICATION_MODAL | SWT.TITLE);
		shell.setLayout(new FillLayout());

		try {
			browser = new Browser(shell, Utils.getInitialBrowserStyle(SWT.NONE));
		} catch (Throwable t) {
			shell.dispose();
			return;
		}

		browser.addTitleListener(new TitleListener() {
			public void changed(TitleEvent event) {
				shell.setText(event.title);
			}
		});

		browser.addStatusTextListener(new StatusTextListener() {
			String last = null;

			public void changed(StatusTextEvent event) {
				String text = event.text.toLowerCase();
				if (last != null && last.equals(text)) {
					return;
				}
				last = text;
				if (text.contains("page-loaded")) {
					pageLoadedOk = true;
					COConfigurationManager.setParameter("donations.count",
							COConfigurationManager.getLongParameter("donations.count", 1) + 1);
					Utils.centreWindow(shell);
					shell.open();
				} else if (text.contains("reset-ask-time")) {
					int time = reAskEveryHours;
					String[] strings = text.split(" ");
					if (strings.length > 1) {
						try {
							time = Integer.parseInt(strings[1]);
						} catch (Throwable t) {
						}
					}
					resetAskTime(time);
				} else if (text.contains("never-ask-again")) {
					neverAskAgain();
				} else if (text.contains("close")) {
					shell.dispose();
				} else if (text.startsWith("open-url")) {
					String url = event.text.substring(9);
					Utils.launch(url);
				} else if (text.startsWith("set-size")) {
					String[] strings = text.split(" ");
					if (strings.length > 2) {
						try {
							int w = Integer.parseInt(strings[1]);
							int h = Integer.parseInt(strings[2]);

							Rectangle computeTrim = shell.computeTrim(0, 0, w, h);
							shell.setSize(computeTrim.width, computeTrim.height);
						} catch (Exception e) {
						}
					}
				}
			}
		});

		browser.addLocationListener(new LocationListener() {
			public void changing(LocationEvent event) {
			}

			public void changed(LocationEvent event) {
			}
		});

		long upTime = StatsFactory.getStats().getTotalUpTime();
		int upHours = (int) (upTime / (60 * 60)); //secs * mins
		final String url = "http://"
				+ System.getProperty("platform_address", "www.vuze.com") + ":"
				+ System.getProperty("platform_port", "80") + "/"
				+ "donate.start?locale=" + Locale.getDefault().toString() + "&azv="
				+ Constants.AZUREUS_VERSION + "&count="
				+ COConfigurationManager.getLongParameter("donations.count", 1)
				+ "&uphours=" + upHours;

		SimpleTimer.addEvent("donation.pageload", SystemTime.getOffsetTime(10000),
				new TimerEventPerformer() {
					public void perform(TimerEvent event) {
						if (!pageLoadedOk) {
							Utils.execSWTThread(new AERunnable() {
								public void runSupport() {
									Debug.out("Page Didn't Load:" + url);
									shell.dispose();
									if (showNoLoad) {
  									Utils.openMessageBox(shell, SWT.OK,
  											MessageText.getString("DonationWindow.noload.title"),
  											MessageText.getString("DonationWindow.noload.text"));
									}
								}
							});
						}
					}
				});

		browser.setUrl(url);
	}

	/**
	 * 
	 *
	 * @since 4.0.0.5
	 */
	protected void neverAskAgain() {
		COConfigurationManager.setParameter("donations.donated", true);
		COConfigurationManager.save();
	}

	/**
	 * 
	 *
	 * @since 4.0.0.5
	 */
	public static void resetAskTime() {
		resetAskTime(reAskEveryHours);
	}

	public static void resetAskTime(int askEveryHours) {
		long upTime = StatsFactory.getStats().getTotalUpTime();
		int hours = (int) (upTime / (60 * 60)); //secs * mins
		int nextAsk = hours + askEveryHours;
		COConfigurationManager.setParameter("donations.nextAskHours", nextAsk);
		COConfigurationManager.setParameter("donations.lastVersion",
				Constants.AZUREUS_VERSION);
		COConfigurationManager.save();
	}

	public static void updateMinDate() {
		COConfigurationManager.setParameter("donations.minDate",
				SystemTime.getOffsetTime(1000l * 3600 * 24 * 30));
		COConfigurationManager.save();
	}

	public static void setMinDate(long timestamp) {
		COConfigurationManager.setParameter("donations.minDate", timestamp);
		COConfigurationManager.save();
	}

	public static int getInitialAskHours() {
		return initialAskHours;
	}

	public static void setInitialAskHours(int i) {
		initialAskHours = i;
	}

	public static void main(String[] args) {
		try {
			AzureusCoreFactory.create().start();
			//checkForDonationPopup();
			new DonationWindow().show(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		Display d = Display.getDefault();
		while (true) {
			if (!d.readAndDispatch()) {
				d.sleep();
			}
		}
	}

}