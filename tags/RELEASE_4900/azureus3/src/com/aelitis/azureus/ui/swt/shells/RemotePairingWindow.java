/**
 * Created on Jan 5, 2010
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

package com.aelitis.azureus.ui.swt.shells;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;

import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.installer.*;
import org.gudy.azureus2.plugins.update.UpdateCheckInstance;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.ClipboardCopy;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.shells.CoreWaiterSWT;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.shells.CoreWaiterSWT.TriggerInThread;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter.URLInfo;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.pairing.*;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.utils.ColorCache;
import com.aelitis.azureus.ui.swt.utils.FontUtils;
import com.aelitis.azureus.ui.swt.views.skin.SkinnedDialog;
import com.aelitis.azureus.ui.swt.views.skin.SkinnedDialog.SkinnedDialogClosedListener;
import com.aelitis.azureus.util.StringCompareUtils;

/**
 * @author TuxPaper
 * @created Jan 5, 2010
 *
 */
public class RemotePairingWindow
	implements PairingManagerListener
{
	private static final String PLUGINID_WEBUI = "xmwebui";

	private static final boolean SHOW_SPEW = false;

	private static final boolean DEBUG = false;

	static RemotePairingWindow instance = null;

	private SkinnedDialog skinnedDialog;

	private SWTSkin skin;

	private SWTSkinObjectButton soEnablePairing;

	private PairingManager pairingManager;

	private SWTSkinObject soCodeArea;

	private Font fontCode;

	private String accessCode;

	private Control control;

	private SWTSkinObjectText soStatusText;

	private SWTSkinObject soFTUX;

	private SWTSkinObject soCode;

	private SWTSkinObjectText soToClipboard;

	private boolean hideCode = true;

	private String fallBackStatusText = "";

	private static testPairingClass testPairingClass;

	private PairingTest pairingTest;

	private boolean alreadyTested;

	private String storedToClipboardText;

	private String lastPairingTestError;

	public static void open() {
		if (DEBUG) {
			if (testPairingClass == null) {
				testPairingClass = new testPairingClass();
			} else {
				testPairingClass.inc();
			}
		}

		synchronized (RemotePairingWindow.class) {
			if (instance == null) {
				instance = new RemotePairingWindow();
			}
		}

		CoreWaiterSWT.waitForCore(TriggerInThread.SWT_THREAD,
				new AzureusCoreRunningListener() {
					public void azureusCoreRunning(AzureusCore core) {
						instance._open();
					}
				});
	}

	private PluginInterface getWebUI() {
		return AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByID(
				PLUGINID_WEBUI, true);
	}

	private void _open() {
		alreadyTested = false;

		pairingManager = PairingManagerFactory.getSingleton();
		PluginInterface piWebUI = getWebUI();

		boolean showFTUX = piWebUI == null || !pairingManager.isEnabled();

		if (skinnedDialog == null || skinnedDialog.isDisposed()) {
			skinnedDialog = new SkinnedDialog("skin3_dlg_remotepairing", "shell",
					SWT.DIALOG_TRIM);

			skin = skinnedDialog.getSkin();

			soCodeArea = skin.getSkinObject("code-area");
			control = soCodeArea.getControl();

			soEnablePairing = (SWTSkinObjectButton) skin.getSkinObject("enable-pairing");
			soEnablePairing.addSelectionListener(new ButtonListenerAdapter() {
				// @see com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter#pressed(com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility, com.aelitis.azureus.ui.swt.skin.SWTSkinObject, int)
				public void pressed(SWTSkinButtonUtility buttonUtility,
						SWTSkinObject skinObject, int stateMask) {
					skinObject.getControl().setEnabled(false);

					if (!pairingManager.isEnabled()) {
						// enabling will automatically get access code and trigger
						// somethingChanged
						pairingManager.setEnabled(true);
						if (SHOW_SPEW) {
							System.out.println("PAIR] SetEnabled");
						}
					} else {
						// fire something changed ourselves, so that accesscode gets
						// picked up
						if (SHOW_SPEW) {
							System.out.println("PAIR] AlreadyEnabled");
						}
						somethingChanged(pairingManager);
					}

					if (getWebUI() == null) {
						installWebUI();
					} else {
						switchToCode();
					}
				}
			});

			soFTUX = skin.getSkinObject("pairing-ftux");
			soCode = skin.getSkinObject("pairing-code");

			soStatusText = (SWTSkinObjectText) skin.getSkinObject("status-text");
			soStatusText.addUrlClickedListener(new SWTSkinObjectText_UrlClickedListener() {
				public boolean urlClicked(URLInfo urlInfo) {
					if (urlInfo.url.equals("retry")) {
						if (DEBUG) {
							testPairingClass.inc();
						}
						alreadyTested = false;
						testPairing(false);
						return true;
					}
					return false;
				}
			});

			pairingManager.addListener(this);

			Font font = control.getFont();
			GC gc = new GC(control);
			fontCode = FontUtils.getFontWithHeight(font, gc, Constants.isWindows ? 20
					: 18, SWT.BOLD);
			gc.dispose();
			control.setFont(fontCode);

			control.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent e) {
					Color oldColor = e.gc.getForeground();

					Rectangle printArea = ((Composite) e.widget).getClientArea();
					int fullWidth = printArea.width;
					int fullHeight = printArea.height;
					GCStringPrinter sp = new GCStringPrinter(e.gc,
							MessageText.getString("remote.pairing.accesscode"), printArea,
							false, false, SWT.NONE);
					sp.calculateMetrics();
					Point sizeAccess = sp.getCalculatedSize();

					String drawAccessCode = accessCode == null ? "      " : accessCode;

					int numBoxes = drawAccessCode == null ? 0 : drawAccessCode.length();
					int boxSize = 25;
					int boxSizeAndPadding = 30;
					int allBoxesWidth = numBoxes * boxSizeAndPadding;
					int textPadding = 15;
					printArea.x = (fullWidth - (allBoxesWidth + sizeAccess.x + textPadding)) / 2;
					printArea.width = sizeAccess.x;

					sp.printString(e.gc, printArea, 0);
					e.gc.setBackground(Colors.white);
					e.gc.setForeground(Colors.blue);

					int xStart = printArea.x + sizeAccess.x + textPadding;
					int yStart = (fullHeight - boxSize) / 2;
					for (int i = 0; i < numBoxes; i++) {
						Rectangle r = new Rectangle(xStart + (i * boxSizeAndPadding),
								yStart, boxSize, boxSize);
						e.gc.fillRectangle(r);
						e.gc.setForeground(Colors.blues[Colors.BLUES_DARKEST]);
						e.gc.drawRectangle(r);
						if (!hideCode) {
							e.gc.setForeground(oldColor);
							GCStringPrinter.printString(e.gc, "" + drawAccessCode.charAt(i),
									r, false, false, SWT.CENTER);
						}
					}
				}
			});

			soToClipboard = (SWTSkinObjectText) skin.getSkinObject("pair-clipboard");

			soToClipboard.addUrlClickedListener(new SWTSkinObjectText_UrlClickedListener() {
				public boolean urlClicked(URLInfo urlInfo) {
					if (urlInfo.url.equals("new")) {
						try {
							accessCode = pairingManager.getReplacementAccessCode();
						} catch (PairingException e) {
							// ignore.. if error, lastErrorUpdates will trigger
						}
						control.redraw();
						String s = soToClipboard.getText();
						int i = s.indexOf("|");
						if (i > 0) {
							soToClipboard.setText(s.substring(0, i - 1));
						}
					} else if (urlInfo.url.equals("clip")) {
						ClipboardCopy.copyToClipBoard(accessCode);
					}
					return true;
				}
			});
			SWTSkinButtonUtility btnToClipboard = new SWTSkinButtonUtility(
					soToClipboard);
			btnToClipboard.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility,
						SWTSkinObject skinObject, int stateMask) {
				}
			});

			skinnedDialog.addCloseListener(new SkinnedDialogClosedListener() {
				public void skinDialogClosed(SkinnedDialog dialog) {
					skinnedDialog = null;
					pairingManager.removeListener(RemotePairingWindow.this);
					Utils.disposeSWTObjects(new Object[] {
						fontCode
					});
					if (pairingTest != null) {
						pairingTest.cancel();
					}
				}
			});

			if (showFTUX) {
				soFTUX.getControl().moveAbove(null);
			}
		}
		hideCode = true;
		skinnedDialog.open();
		hideCode = false;

		if (showFTUX) {
			switchToFTUX();
		} else {
			switchToCode();
		}
	}

	public void switchToFTUX() {
		SWTSkinObject soPairInstallArea = skin.getSkinObject("pair-install");
		if (soPairInstallArea != null) {
			soPairInstallArea.getControl().moveAbove(null);
		}
		soFTUX.setVisible(true);
		soCode.setVisible(false);
	}

	public void switchToCode() {
		// use somethingChanged to trigger testPairing if needed
		somethingChanged(pairingManager);

		Utils.execSWTThread(new AERunnable() {

			public void runSupport() {

				if (skinnedDialog == null || skinnedDialog.isDisposed()) {
					return;
				}

				SWTSkinObjectImage soImage = (SWTSkinObjectImage) skin.getSkinObject("status-image");
				if (soImage != null) {
					soImage.setImageByID("icon.spin", null);
				}

				SWTSkinObject soPairArea = skin.getSkinObject("reset-pair-area");
				if (soPairArea != null) {
					soPairArea.getControl().moveAbove(null);
				}
				soFTUX.setVisible(false);
				soCode.setVisible(true);
			}
		});
	}

	protected void testPairing(boolean delay) {
		if (SHOW_SPEW) {
			System.out.println("PAIR] Want testPairing; alreadyTested="
					+ alreadyTested + ";Delay?" + delay + ";"
					+ Debug.getCompressedStackTrace());
		}
		if (alreadyTested) {
			return;
		}

		lastPairingTestError = "";
		alreadyTested = true;

		storedToClipboardText = soToClipboard.getText();
		try {
			hideCode = true;
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					control.redraw();
					SWTSkinObjectImage soImage = (SWTSkinObjectImage) skin.getSkinObject("status-image");
					if (soImage != null) {
						soImage.setImageByID("icon.spin", null);
					}
				}
			});
			soStatusText.setTextID("remote.pairing.test.running");
			soStatusText.setTextColor(ColorCache.getColor(control.getDisplay(),
					"#000000"));
			soToClipboard.setText(" ");

			final PairingTestListener testListener = new PairingTestListener() {
				public void testStarted(PairingTest test) {
				}

				public void testComplete(PairingTest test) {
					if ( skinnedDialog == null || skinnedDialog.isDisposed() || control.isDisposed()) {
						return;
					}

					int outcome = test.getOutcome();
					String iconID = null;
					String colorID = "#000000";
					switch (outcome) {
						case PairingTest.OT_SUCCESS:
							fallBackStatusText = MessageText.getString("remote.pairing.test.success");
							iconID = "icon.success";
							colorID = "#007305";
							break;

						case PairingTest.OT_CANCELLED:
							fallBackStatusText = test.getErrorMessage();
							iconID = "icon.warning";
							colorID = "#A97000";
							break;

						case PairingTest.OT_SERVER_FAILED:
						case PairingTest.OT_SERVER_OVERLOADED:
						case PairingTest.OT_SERVER_UNAVAILABLE:
							fallBackStatusText = MessageText.getString(
									"remote.pairing.test.unavailable", new String[] {
										test.getErrorMessage()
									});
							iconID = "icon.warning";
							colorID = "#C98000";
							break;

						default:
							fallBackStatusText = MessageText.getString(
									"remote.pairing.test.fail", new String[] {
										test.getErrorMessage()
									});
							iconID = "icon.failure";
							colorID = "#c90000";
							break;
					}

					hideCode = false;
					final String fIconID = iconID;
					somethingChanged(pairingManager);
					lastPairingTestError = pairingTest.getErrorMessage();
					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {
							if ( !control.isDisposed()){
								control.redraw();
								SWTSkinObjectImage soImage = (SWTSkinObjectImage) skin.getSkinObject("status-image");
								if (soImage != null) {
									soImage.setImageByID(fIconID, null);
								}
							}
						}
					});
					updateToolTip();
					soStatusText.setText(fallBackStatusText);
					soStatusText.setTextColor(ColorCache.getColor(control.getDisplay(),
							colorID));
					soToClipboard.setText(storedToClipboardText);
				}
			};
			SimpleTimer.addEvent("testPairing", SystemTime.getOffsetTime(delay ? 5000
					: 0), new TimerEventPerformer() {
				public void perform(TimerEvent event) {
					try {
						pairingTest = pairingManager.testService(PLUGINID_WEBUI,
								testListener);
					} catch (PairingException e) {
						finishFailedTest();

						soStatusText.setText(Debug.getNestedExceptionMessage(e));
						Debug.out(e);
					}

					if (pairingTest == null) {
						finishFailedTest();
					}
				}
			});

			if (DEBUG) {
				testListener.testComplete(testPairingClass);
				return;
			}
		} catch (Exception e) {
			finishFailedTest();

			soStatusText.setText(Debug.getNestedExceptionMessage(e));
			Debug.out(e);
		}
	}

	/**
	 * 
	 *
	 * @since 4.1.0.5
	 */
	protected void updateToolTip() {
		SWTSkinObjectImage soImage = (SWTSkinObjectImage) skin.getSkinObject("status-image");
		if (soImage != null) {
			String s = lastPairingTestError;
			if (s == null) {
				s = "";
			}
			
			String status = pairingManager.getStatus();
			if (status != null && status.length() > 0) {
				if (s.length() > 0) {
					s += "\n";
				}
				s += "Pairing Status: " + status;
			}
			String lastPairingErr = pairingManager.getLastServerError();
			if (lastPairingErr != null && lastPairingErr.length() > 0) {
				if (s.length() > 0) {
					s += "\n";
				}
				s += "Pairing Error: " + lastPairingErr;
			}
			soImage.setTooltipID("!" + s + "!");
		}
	}

	private void finishFailedTest() {
		hideCode = false;
		somethingChanged(pairingManager);
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				control.redraw();
			}
		});
		if (storedToClipboardText != null && storedToClipboardText.length() > 0) {
			soToClipboard.setText(storedToClipboardText);
		}
	}

	protected void installWebUI() {
		final PluginInstaller installer = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInstaller();

		StandardPlugin vuze_plugin = null;

		try {
			vuze_plugin = installer.getStandardPlugin(PLUGINID_WEBUI);

		} catch (Throwable e) {
		}

		if (vuze_plugin == null) {
			return;
		}

		if (vuze_plugin.isAlreadyInstalled()) {
			PluginInterface plugin = vuze_plugin.getAlreadyInstalledPlugin();
			plugin.getPluginState().setDisabled(false);
			return;
		}

		try {
			switchToFTUX();

			final SWTSkinObject soInstall = skin.getSkinObject("pairing-install");
			final SWTSkinObject soLearnMore = skin.getSkinObject("learn-more");
			if (soLearnMore != null) {
				soLearnMore.setVisible(false);
			}

			Map<Integer, Object> properties = new HashMap<Integer, Object>();

			properties.put(UpdateCheckInstance.PT_UI_STYLE,
					UpdateCheckInstance.PT_UI_STYLE_SIMPLE);

			properties.put(UpdateCheckInstance.PT_UI_PARENT_SWT_COMPOSITE,
					soInstall.getControl());

			properties.put(UpdateCheckInstance.PT_UI_DISABLE_ON_SUCCESS_SLIDEY, true);

			installer.install(new InstallablePlugin[] {
				vuze_plugin
			}, false, properties, new PluginInstallationListener() {
				public void completed() {
					if (soLearnMore != null) {
						soLearnMore.setVisible(true);
					}
					switchToCode();
				}

				public void cancelled() {
					Utils.execSWTThread(new AERunnable() {

						public void runSupport() {

							if ( skinnedDialog != null && !skinnedDialog.isDisposed()){
							
								skinnedDialog.close();
								
								skinnedDialog = null;
							}
						}
					});
				}

				public void failed(PluginException e) {

					Debug.out(e);
					//Utils.openMessageBox(Utils.findAnyShell(), SWT.OK, "Error",
					//		e.toString());
				}
			});

		} catch (Throwable e) {

			Debug.printStackTrace(e);
		}
	}

	// @see com.aelitis.azureus.core.pairing.PairingManagerListener#somethingChanged(com.aelitis.azureus.core.pairing.PairingManager)
	public void somethingChanged(PairingManager pm) {
		if (skinnedDialog.isDisposed()) {
			return;
		}

		updateToolTip();

		String lastAccessCode = accessCode;

		accessCode = pairingManager.peekAccessCode();
		boolean newAccessCode = !StringCompareUtils.equals(lastAccessCode, accessCode);
		if (accessCode != null && getWebUI() != null && !alreadyTested
				&& !pm.hasActionOutstanding()) {
			if (newAccessCode) {
				// pause while registering..
				testPairing(true);
			} else {
				testPairing(false);
			}
		}else{
			String last_error = pm.getLastServerError();
		
			if ( last_error != null && last_error.length() > 0 ){
				soStatusText.setText(last_error);
				soStatusText.setTextColor(ColorCache.getColor(control.getDisplay(),
						"#c90000"));
			}
		}
		
		if (newAccessCode) {
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					control.redraw();
				}
			});
		}
	}

	public static class testPairingClass
		implements PairingTest
	{
		int curOutcome = 0;

		int[] testOutcomes = {
			OT_SUCCESS,
			OT_FAILED,
			OT_CANCELLED,
			OT_SERVER_FAILED,
			OT_SERVER_OVERLOADED,
			OT_SERVER_UNAVAILABLE
		};

		String[] testErrs = {
			"Success",
			"Could Not Connect blah blah technical stuff",
			"You Cancelled (unpossible!)",
			"Server Failed",
			"Server Overloaded",
			"Server Unavailable",
		};

		public void inc() {
			curOutcome++;
			if (curOutcome == testOutcomes.length) {
				curOutcome = 0;
			}
		}

		public int getOutcome() {
			return testOutcomes[curOutcome];
		}

		public String getErrorMessage() {
			return testErrs[curOutcome];
		}

		public void cancel() {
		}
	}
}
