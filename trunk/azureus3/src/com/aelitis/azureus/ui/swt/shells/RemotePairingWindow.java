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
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
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
import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.pairing.*;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.views.skin.SkinnedDialog;
import com.aelitis.azureus.ui.swt.views.skin.SkinnedDialog.SkinnedDialogClosedListener;
import com.aelitis.azureus.util.ConstantsVuze;

/**
 * @author TuxPaper
 * @created Jan 5, 2010
 *
 */
public class RemotePairingWindow implements PairingManagerListener
{
	private static final String PLUGINID_WEBUI = "xmwebui";

	static RemotePairingWindow instance = null;

	private SkinnedDialog skinnedDialog;

	private SWTSkin skin;

	private SWTSkinObjectButton soEnablePairing;

	private PairingManager pairingManager;

	private SWTSkinObject soCodeArea;

	private Font fontCode;

	private String accessCode;

	private Control control;

	private SWTSkinObjectText soStatus;

	private SWTSkinObject soFTUX;

	private SWTSkinObject soCode;

	private PluginInterface piWebUI;

	private SWTSkinObjectText soToClipboard;

	public static void open() {
		synchronized (RemotePairingWindow.class) {
			if (instance == null) {
				instance = new RemotePairingWindow();
			}
		}

		CoreWaiterSWT.waitForCore(TriggerInThread.SWT_THREAD, new AzureusCoreRunningListener() {
			public void azureusCoreRunning(AzureusCore core) {
				instance._open();
			}
		});
	}

	private void _open() {
		pairingManager = PairingManagerFactory.getSingleton();
		piWebUI = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByID(
				PLUGINID_WEBUI, true);
		boolean showFTUX = piWebUI == null || !pairingManager.isEnabled();
		if (skinnedDialog == null || skinnedDialog.isDisposed()) {
			skinnedDialog = new SkinnedDialog("skin3_dlg_remotepairing", "shell",
					SWT.DIALOG_TRIM);

			skin = skinnedDialog.getSkin();
			soEnablePairing = (SWTSkinObjectButton) skin.getSkinObject("enable-pairing");
			soEnablePairing.addSelectionListener(new ButtonListenerAdapter() {
				// @see com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter#pressed(com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility, com.aelitis.azureus.ui.swt.skin.SWTSkinObject, int)
				public void pressed(SWTSkinButtonUtility buttonUtility,
						SWTSkinObject skinObject, int stateMask) {
					skinObject.getControl().setEnabled(false);
					if (!pairingManager.isEnabled()) {
  					pairingManager.setEnabled(true);
  					try {
  						accessCode = pairingManager.getAccessCode();
  					} catch (PairingException e) {
  						// ignore.. if error, lastErrorUpdates will trigger
  					}
					}
					if (piWebUI == null) {
						installWebUI();
					}
					control.redraw();
				}
			});
			
			soStatus = (SWTSkinObjectText) skin.getSkinObject("status");
			updateStatusText();
			pairingManager.addListener(this);

			soCodeArea = skin.getSkinObject("code-area");
			control = soCodeArea.getControl();
			Font font = control.getFont();
			GC gc = new GC(control);
			fontCode = Utils.getFontWithHeight(font, gc, Constants.isWindows ? 20 : 18, SWT.BOLD);
			gc.dispose();
			control.setFont(fontCode);

			try {
				accessCode = pairingManager.getAccessCode();
			} catch (PairingException e) {
				// ignore.. if error, lastErrorUpdates will trigger
			}

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

					int numBoxes = accessCode == null ? 0 : accessCode.length();
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
						e.gc.setForeground(oldColor);
						GCStringPrinter.printString(e.gc, "" + accessCode.charAt(i), r, false, false, SWT.CENTER);
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
			SWTSkinButtonUtility btnToClipboard  = new SWTSkinButtonUtility(soToClipboard);
			btnToClipboard.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility,
						SWTSkinObject skinObject, int stateMask) {
				}
			});

			skinnedDialog.addCloseListener(new SkinnedDialogClosedListener() {
				public void skinDialogClosed(SkinnedDialog dialog) {
					pairingManager.removeListener(RemotePairingWindow.this);
					Utils.disposeSWTObjects(new Object[] { fontCode });
				}
			});
			
			soFTUX = skin.getSkinObject("pairing-ftux");
			soCode = skin.getSkinObject("pairing-code");
			
			
			if (showFTUX) {
				soFTUX.getControl().moveAbove(null);
			}
		}
		skinnedDialog.open();

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
		Utils.execSWTThread(new AERunnable() {
			
			public void runSupport() {

				if (skinnedDialog.isDisposed()) {
					return;
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

			installer.install(new InstallablePlugin[] { vuze_plugin }, false, properties,
					new PluginInstallationListener() {
						public void completed() {
							if (soLearnMore != null) {
								soLearnMore.setVisible(true);
							}
							switchToCode();
						}

						public void 
						cancelled(){
							skinnedDialog.close();
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

	private void updateStatusText() {
		if (soStatus != null) {
			String s =  "";
			if (pairingManager.hasActionOutstanding()) {
			 s += "Status: " + pairingManager.getStatus();
			}
			String lastServerError = pairingManager.getLastServerError();
			if (lastServerError != null && lastServerError.length() > 0) {
				if (s.length() > 0) {
					s += "\n";
				}
				s += "Last Error: " + lastServerError;
			}
			soStatus.setText(s);
		}
	}

	// @see com.aelitis.azureus.core.pairing.PairingManagerListener#somethingChanged(com.aelitis.azureus.core.pairing.PairingManager)
	public void somethingChanged(PairingManager pm) {
		updateStatusText();
		
		accessCode = pairingManager.peekAccessCode();
	}
}
