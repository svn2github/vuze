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
	private static final int DONATIONS_ASK_EVERY = 168;

	private boolean pageLoadedOk = false;

	private Shell shell;

	private Browser browser;

	public static void checkForDonationPopup() {
		//Check if user has already donated first
		boolean alreadyDonated = COConfigurationManager.getBooleanParameter(
				"donations.donated", false);
		if (alreadyDonated)
			return;

		long upTime = StatsFactory.getStats().getTotalUpTime();
		int hours = (int) (upTime / (60 * 60)); //secs * mins

		//Ask every DONATIONS_ASK_AFTER hours.
		int nextAsk = COConfigurationManager.getIntParameter(
				"donations.nextAskHours", 0);

		if (nextAsk == 0) {
			COConfigurationManager.setParameter("donations.nextAskHours", hours
					+ DONATIONS_ASK_EVERY);
			COConfigurationManager.save();
			Utils.openMessageBox(null, SWT.OK, "Donations Test",
					"Newbie. You're active for " + hours + ".");
			return;
		}

		if (hours < nextAsk) {
			Utils.openMessageBox(null, SWT.OK, "Donations Test",
					"Wait " + (nextAsk - hours) + ".");
			return;
		}

		COConfigurationManager.setParameter("donations.nextAskHours", hours
				+ DONATIONS_ASK_EVERY);
		COConfigurationManager.save();

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				new DonationWindow().show();
			}
		});
	}

	public void show() {
		shell = ShellFactory.createShell(Utils.findAnyShell(), SWT.BORDER
				| SWT.APPLICATION_MODAL | SWT.TITLE);
		shell.setLayoutData(new FillLayout());

		try {
			browser = new Browser(shell, Utils.getInitialBrowserStyle(SWT.NONE));
		} catch (Throwable t) {
			shell.dispose();
			return;
		}

		browser.addTitleListener(new TitleListener() {
			public void changed(TitleEvent event) {
				System.out.println("AHAHA" + event.title);
				shell.setText(event.title);
			}
		});

		browser.addStatusTextListener(new StatusTextListener() {
			public void changed(StatusTextEvent event) {
				String text = event.text.toLowerCase();
				System.out.println(text);
				if (text.contains("page-loaded")) {
					pageLoadedOk = true;
					shell.open();
				} else if (text.contains("reset-ask-time")) {
					resetAskTime();
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

		SimpleTimer.addEvent("donation.pageload", SystemTime.getOffsetTime(10000),
				new TimerEventPerformer() {
					public void perform(TimerEvent event) {
						if (!pageLoadedOk) {
							Utils.execSWTThread(new AERunnable() {
								public void runSupport() {
									System.out.println("Page Didn't Load");
									MessageBoxShell.open(shell, "Beta Beta Beta",
											"Page Didn't Load Properly.  Donate?",
											new String[] {
												"$1 Million",
												"$1 Billion",
												"Beer",
												"Porn",
												"You Sellouts"
											}, 0);
									shell.dispose();
								}
							});
						}
					}
				});

		String url = "http://"
				+ System.getProperty("platform_address", "www.vuze.com") + ":"
				+ System.getProperty( "platform_port", "80" ) + "/"
				+ "donate.start?locale=" + Locale.getDefault().toString();
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
		long upTime = StatsFactory.getStats().getTotalUpTime();
		int hours = (int) (upTime / (60 * 60)); //secs * mins
		int nextAsk = hours + DONATIONS_ASK_EVERY;
		COConfigurationManager.setParameter("donations.nextAskHours", nextAsk);
		COConfigurationManager.setParameter("donations.lastVersion",
				Constants.AZUREUS_VERSION);
		COConfigurationManager.save();
	}

	public static void main(String[] args) {
		try {
			AzureusCoreFactory.create().start();
			//checkForDonationPopup();
			new DonationWindow().show();
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