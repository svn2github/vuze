/*
 * Created on Mar 6, 2010 11:01:46 AM
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
package com.aelitis.azureus.ui.swt.feature;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.utils.FeatureManager.Licence;
import org.gudy.azureus2.plugins.utils.FeatureManager.Licence.LicenceInstallationListener;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.UserPrompterResultListener;
import com.aelitis.azureus.ui.skin.SkinPropertiesImpl;
import com.aelitis.azureus.ui.swt.skin.SWTSkin;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectContainer;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectText;
import com.aelitis.azureus.ui.swt.views.skin.VuzeMessageBox;
import com.aelitis.azureus.ui.swt.views.skin.VuzeMessageBoxListener;
import com.aelitis.azureus.util.FeatureUtils;

/**
 * @author TuxPaper
 * @created Mar 6, 2010
 *
 */
public class FeatureManagerInstallWindow
	implements LicenceInstallationListener
{
	private final static boolean FAKE_DELAY = Constants.IS_CVS_VERSION;

	private VuzeMessageBox box;

	private ProgressBar progressBar;

	private Licence licence;

	private SWTSkinObjectText soProgressText;

	private String progressText;

	private SWTSkinObjectText soInstallPct;
	
	public FeatureManagerInstallWindow(Licence licence) {
		if (!FeatureManagerUI.enabled) {
			return;
		}
		this.licence = licence;
		licence.addInstallationListener(this);
	}

	public void open() {
		if (!FeatureManagerUI.enabled) {
			return;
		}

		boolean isTrial = FeatureUtils.isTrialLicence(licence);
		box = new VuzeMessageBox(MessageText.getString("dlg.auth.title"),
				"", null, 0);
		box.setSubTitle(MessageText.getString(isTrial ? "dlg.auth.install.subtitle.trial"
				: "dlg.auth.install.subtitle.plus"));
		box.addResourceBundle(FeatureManagerUI.class,
				SkinPropertiesImpl.PATH_SKIN_DEFS, "skin3_dlg_register");
		box.setIconResource(isTrial ? "image.burn.dlg.header" : "image.vp");

		box.setListener(new VuzeMessageBoxListener() {

			public void shellReady(Shell shell, SWTSkinObjectContainer soExtra) {
				SWTSkin skin = soExtra.getSkin();
				skin.createSkinObject("dlg.register.install", "dlg.register.install",
						soExtra);

				SWTSkinObjectContainer soProgressBar = (SWTSkinObjectContainer) skin.getSkinObject("progress-bar");
				if (soProgressBar != null) {
					progressBar = new ProgressBar(soProgressBar.getComposite(),
							SWT.HORIZONTAL);
					progressBar.setMinimum(0);
					progressBar.setMaximum(100);
					progressBar.setLayoutData(Utils.getFilledFormData());
				}
				
				soInstallPct = (SWTSkinObjectText) skin.getSkinObject("install-pct");

				soProgressText = (SWTSkinObjectText) skin.getSkinObject("progress-text");
				if (soProgressText != null && progressText != null) {
					soProgressText.setText(progressText);
				}
			}
		});

		box.open(new UserPrompterResultListener() {
			public void prompterClosed(int result) {
				licence.removeInstallationListener(FeatureManagerInstallWindow.this);
			}
		});
	}

	public void reportActivity(String licence_key, String install, String activity) {
		if (FAKE_DELAY) {
			try {
				Thread.sleep(80);
			} catch (InterruptedException e) {
			}
		}

		if (soProgressText != null) {
			String[] split = install.split("/", 2);
			this.progressText = MessageText.getString("dlg.auth.install.progress",
					new String[] { split.length == 2 ? split[1] : split[0] });
			soProgressText.setText(this.progressText);
		}
	}

	public void reportProgress(String licence_key, String install,
			final int percent) {
		if (FAKE_DELAY) {
			try {
				Thread.sleep(80);
			} catch (InterruptedException e) {
			}
		}

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				int pct = percent == 100 ? 99 : percent;
				if (soInstallPct != null) {
					soInstallPct.setText(MessageText.getString("dlg.auth.install.pct",
							new String[] {
								"" + pct
							}));
				}
				if (progressBar != null && !progressBar.isDisposed()) {
					// never reach 100%!
					progressBar.setSelection(pct);
				}
			}
		});
	}

	public void complete(String licence_key) {
		if (box != null) {
			box.close(0);
		}
		licence.removeInstallationListener(this);
	}

	public static boolean alreadyFailing = false;
	public void failed(String licence_key, PluginException error) {
		if (alreadyFailing) {
			return;
		}
		alreadyFailing = true;
		UIFunctionsManager.getUIFunctions().promptUser(
				MessageText.getString( "dlg.auth.install.failed.title" ), 
				MessageText.getString( "dlg.auth.install.failed.text", new String[]{ licence_key, Debug.getNestedExceptionMessage( error )}),
				new String[] {
					MessageText.getString("Button.ok")
				}, 0, null, null, false, 0, new UserPrompterResultListener() {
					public void prompterClosed(int result) {
						alreadyFailing = false;
					}
				});
				
		
		Logger.log(new LogAlert(true, "Error while installing " + licence_key,
				error));
		box.close(0);
		licence.removeInstallationListener(this);
	}

	public void close() {
		
		box.close(0);
		licence.removeInstallationListener(this);
	}
	
	public void start(String licence_key) {
	}

}
