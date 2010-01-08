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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.shells.CoreWaiterSWT;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.shells.CoreWaiterSWT.TriggerInThread;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.pairing.*;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.views.skin.SkinnedDialog;
import com.aelitis.azureus.ui.swt.views.skin.SkinnedDialog.SkinnedDialogClosedListener;

/**
 * @author TuxPaper
 * @created Jan 5, 2010
 *
 */
public class RemotePairingWindow implements PairingManagerListener
{
	static RemotePairingWindow instance = null;

	private SkinnedDialog skinnedDialog;

	private SWTSkin skin;

	private SWTSkinObjectCheckbox soEnablePairing;

	private PairingManager pairingManager;

	private SWTSkinObject soCodeArea;

	private Font fontCode;

	private SWTSkinObject soResetPair;

	private String accessCode;

	private Control control;

	private SWTSkinObjectText soStatus;

	private PairingManagerListener pairingManagerListener;

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
		if (skinnedDialog == null || skinnedDialog.isDisposed()) {
			skinnedDialog = new SkinnedDialog("skin3_dlg_remotepairing", "shell",
					SWT.DIALOG_TRIM);

			skin = skinnedDialog.getSkin();
			soEnablePairing = (SWTSkinObjectCheckbox) skin.getSkinObject("enable-pairing");
			soEnablePairing.setChecked(pairingManager.isEnabled());
			soEnablePairing.addSelectionListener(new SWTSkinCheckboxListener() {
				public void checkboxChanged(SWTSkinObjectCheckbox so, boolean checked) {
					pairingManager.setEnabled(checked);
					try {
						accessCode = pairingManager.getAccessCode();
					} catch (PairingException e1) {
					}
					control.redraw();
				}
			});
			
			soStatus = (SWTSkinObjectText) skin.getSkinObject("status");
			setStatusText(pairingManager.getStatus());
			pairingManager.addListener(this);

			soCodeArea = skin.getSkinObject("code-area");
			control = soCodeArea.getControl();
			Font font = control.getFont();
			GC gc = new GC(control);
			fontCode = Utils.getFontWithHeight(font, gc, 18, SWT.BOLD);
			gc.dispose();
			control.setFont(fontCode);

			try {
				accessCode = pairingManager.getAccessCode();
			} catch (PairingException e) {
				setStatusText(e.getMessage());
			}

			control.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent e) {
					Rectangle printArea = ((Composite) e.widget).getClientArea();
					int fullWidth = printArea.width;
					int fullHeight = printArea.height;
					GCStringPrinter sp = new GCStringPrinter(e.gc, "Access Code:",
							printArea, false, false, SWT.NONE);
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
						e.gc.drawRectangle(r);
						GCStringPrinter.printString(e.gc, "" + accessCode.charAt(i), r, false, false, SWT.CENTER);
					}
				}
			});

			soResetPair = skin.getSkinObject("reset-pair");
			SWTSkinButtonUtility btnResetPair = new SWTSkinButtonUtility(soResetPair);
			btnResetPair.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility,
						SWTSkinObject skinObject, int stateMask) {
					try {
						accessCode = pairingManager.getReplacementAccessCode();
					} catch (PairingException e) {
						setStatusText(e.getMessage());
					}
					control.redraw();
				}
			});
		}
		skinnedDialog.addCloseListener(new SkinnedDialogClosedListener() {
			public void skinDialogClosed(SkinnedDialog dialog) {
				pairingManager.removeListener(RemotePairingWindow.this);
				Utils.disposeSWTObjects(new Object[] { fontCode });
			}
		});
		skinnedDialog.open();
	}

	/**
	 * @param status
	 *
	 * @since 4.1.0.5
	 */
	private void setStatusText(String status) {
		if (soStatus != null) {
			soStatus.setText("Status: " + status);
		}
	}

	// @see com.aelitis.azureus.core.pairing.PairingManagerListener#somethingChanged(com.aelitis.azureus.core.pairing.PairingManager)
	public void somethingChanged(PairingManager pm) {
		setStatusText(pairingManager.getStatus());
		try {
			accessCode = pairingManager.getAccessCode();
		} catch (PairingException e) {
			setStatusText(e.getMessage());
		}
	}
}
